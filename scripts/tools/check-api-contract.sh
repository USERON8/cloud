#!/usr/bin/env bash
set -euo pipefail

ROOT="."
for arg in "$@"; do
  case "$arg" in
    --root=*) ROOT="${arg#*=}" ;;
    --help|-h)
      cat <<'EOF'
Usage: bash scripts/tools/check-api-contract.sh [--root=PATH]

Static controller contract check for:
- API path and @PathVariable mismatches
- Multiple @RequestBody declarations
- GET endpoints declaring @RequestBody
- /api/** endpoints not returning Result<T>
EOF
      exit 0
      ;;
    *)
      ROOT="$arg"
      ;;
  esac
done

PYTHON_BIN="${PYTHON_BIN:-}"
if [ -z "$PYTHON_BIN" ]; then
  if command -v python3 >/dev/null 2>&1; then
    PYTHON_BIN="python3"
  elif command -v python >/dev/null 2>&1; then
    PYTHON_BIN="python"
  else
    echo "python3 or python is required to run scripts/tools/check-api-contract.sh" >&2
    exit 1
  fi
fi

"$PYTHON_BIN" - "$ROOT" <<'PY'
from __future__ import annotations

import pathlib
import re
import sys


def normalize_path_join(base_path: str | None, method_path: str | None) -> str:
    left = (base_path or "").strip()
    right = (method_path or "").strip()
    if not left:
        left = "/"
    if not right:
        return left
    if not left.startswith("/"):
        left = "/" + left
    if left.endswith("/"):
        left = left.rstrip("/")
    if not right.startswith("/"):
        right = "/" + right
    return left + right


def select_first_quoted_value(text: str) -> str:
    if not text or text.isspace():
        return ""
    match = re.search(r'"([^"]+)"', text)
    return match.group(1) if match else ""


def get_quoted_values(text: str) -> list[str]:
    if not text or text.isspace():
        return []
    return re.findall(r'"([^"]+)"', text)


def parse_class_base_paths(content: str) -> list[str]:
    match = re.search(
        r"@RequestMapping\(([^)]*)\)(?:(?!\bclass\b).)*\bclass\b",
        content,
        re.S,
    )
    if not match:
        return ["/"]

    base_paths = get_quoted_values(match.group(1))
    if not base_paths:
        base_path = select_first_quoted_value(match.group(1))
        if base_path:
            base_paths = [base_path]
    return base_paths or ["/"]


def get_annotation_block(lines: list[str], start_index: int) -> str:
    block: list[str] = []
    for index in range(start_index - 1, -1, -1):
        line = lines[index]
        if not line.strip():
            break
        block.insert(0, line)
    return "\n".join(block)


def main() -> int:
    root = pathlib.Path(sys.argv[1]).resolve()
    controller_files = sorted(
        path
        for path in root.rglob("*Controller.java")
        if "target" not in path.parts
    )

    method_pattern = re.compile(
        r"public\s+(?P<return>.+?)\s+(?P<name>[A-Za-z_]\w*)\s*\((?P<params>.*?)\)\s*\{",
        re.S,
    )
    mapping_pattern = re.compile(r"@(?:Get|Post|Put|Delete|Patch|Request)Mapping")
    mapping_args_pattern = re.compile(
        r"@(?:Get|Post|Put|Delete|Patch|Request)Mapping\(([^)]*)\)"
    )
    path_var_pattern = re.compile(
        r'@PathVariable(?:\((?:\s*(?:value|name)\s*=\s*)?"?([^",)]+)"?[^)]*\))?'
        r'(?:\s+@\w+(?:\([^)]*\))?)*\s+[^\s,<>]+(?:<[^>]+>)?\s+([A-Za-z_]\w*)',
        re.S,
    )

    issues: list[dict[str, str]] = []
    checked_methods = 0

    for file_path in controller_files:
        content = file_path.read_text(encoding="utf-8", errors="ignore")
        lines = content.splitlines()
        class_base_paths = parse_class_base_paths(content)
        is_api_adapter_controller = re.search(r"implements\s+\w+Api\b", content) is not None

        for line_index, line in enumerate(lines):
            if not re.match(r"^\s*public\b", line):
                continue

            annotation_block = get_annotation_block(lines, line_index)
            if not mapping_pattern.search(annotation_block):
                continue

            signature_lines: list[str] = []
            brace_found = False
            for sig_index in range(line_index, len(lines)):
                signature_lines.append(lines[sig_index])
                if "{" in lines[sig_index]:
                    brace_found = True
                    break
            if not brace_found:
                continue

            signature = " ".join(signature_lines)
            match = method_pattern.search(signature)
            if not match:
                continue

            checked_methods += 1
            params = match.group("params")
            return_type = (match.group("return") or "").strip()
            method_name = match.group("name")

            http_method = "REQUEST"
            if "@GetMapping" in annotation_block:
                http_method = "GET"
            elif "@PostMapping" in annotation_block:
                http_method = "POST"
            elif "@PutMapping" in annotation_block:
                http_method = "PUT"
            elif "@DeleteMapping" in annotation_block:
                http_method = "DELETE"
            elif "@PatchMapping" in annotation_block:
                http_method = "PATCH"

            mapping_args = mapping_args_pattern.search(annotation_block)
            method_path = select_first_quoted_value(mapping_args.group(1)) if mapping_args else ""
            full_paths = [normalize_path_join(base_path, method_path) for base_path in class_base_paths]
            full_path = full_paths[0]

            placeholders = re.findall(r"\{([^}/]+)\}", full_path)
            path_vars: list[str] = []
            for path_match in path_var_pattern.finditer(params):
                explicit_name = (path_match.group(1) or "").strip()
                param_name = (path_match.group(2) or "").strip()
                path_vars.append(explicit_name or param_name)

            missing_path_vars = [placeholder for placeholder in placeholders if placeholder not in path_vars]
            unused_path_vars = [path_var for path_var in path_vars if path_var not in placeholders]
            if missing_path_vars or unused_path_vars:
                issues.append(
                    {
                        "File": str(file_path.resolve()),
                        "Method": method_name,
                        "Type": "path-variable-mismatch",
                        "Detail": (
                            f"path={full_path} placeholders=[{','.join(placeholders)}] "
                            f"pathVars=[{','.join(path_vars)}]"
                        ),
                    }
                )

            request_body_count = len(re.findall(r"@RequestBody", params))
            if request_body_count > 1:
                issues.append(
                    {
                        "File": str(file_path.resolve()),
                        "Method": method_name,
                        "Type": "multiple-request-body",
                        "Detail": f"requestBodyCount={request_body_count}",
                    }
                )

            if http_method == "GET" and request_body_count > 0:
                issues.append(
                    {
                        "File": str(file_path.resolve()),
                        "Method": method_name,
                        "Type": "get-with-request-body",
                        "Detail": "GET endpoint should not declare @RequestBody",
                    }
                )

            is_api_endpoint = any(path.startswith("/api/") for path in full_paths)
            is_internal_api = any(path.startswith("/internal/") for path in full_paths)
            is_gateway_fallback = any(path.startswith("/gateway/") for path in full_paths)
            if (
                is_api_endpoint
                and not is_internal_api
                and not is_gateway_fallback
                and not is_api_adapter_controller
                and re.search(r"(^|[<\s])Result<", return_type) is None
            ):
                issues.append(
                    {
                        "File": str(file_path.resolve()),
                        "Method": method_name,
                        "Type": "non-standard-result-wrapper",
                        "Detail": f"returnType={return_type} paths=[{', '.join(full_paths)}]",
                    }
                )

    print(
        f"[api-contract] checked_controllers={len(controller_files)} "
        f"checked_methods={checked_methods}"
    )

    if issues:
        print(f"[api-contract] issues={len(issues)}")
        for issue in issues:
            print(
                f"[ERROR] {issue['File']}::{issue['Method']} "
                f"[{issue['Type']}] {issue['Detail']}"
            )
        return 1

    print("[api-contract] no issues found")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
PY
