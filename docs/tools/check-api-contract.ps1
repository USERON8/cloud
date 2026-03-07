param(
    [string]$Root = "."
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Normalize-PathJoin {
    param(
        [string]$BasePath,
        [string]$MethodPath
    )
    $left = if ($null -eq $BasePath) { "" } else { [string]$BasePath }
    $right = if ($null -eq $MethodPath) { "" } else { [string]$MethodPath }
    $left = $left.Trim()
    $right = $right.Trim()
    if ([string]::IsNullOrWhiteSpace($left)) { $left = "/" }
    if ([string]::IsNullOrWhiteSpace($right)) { return $left }
    if (-not $left.StartsWith("/")) { $left = "/$left" }
    if ($left.EndsWith("/")) { $left = $left.TrimEnd("/") }
    if (-not $right.StartsWith("/")) { $right = "/$right" }
    return "$left$right"
}

function Select-FirstQuotedValue {
    param([string]$InputText)
    if ([string]::IsNullOrWhiteSpace($InputText)) { return "" }
    $m = [regex]::Match($InputText, '"([^"]+)"')
    if ($m.Success) { return $m.Groups[1].Value }
    return ""
}

function Get-QuotedValues {
    param([string]$InputText)
    if ([string]::IsNullOrWhiteSpace($InputText)) { return @() }
    return @([regex]::Matches($InputText, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
}

function Parse-ClassBasePaths {
    param([string]$Content)
    $m = [regex]::Match($Content, '@RequestMapping\(([^)]*)\)(?:(?!\bclass\b).)*\bclass\b', 'Singleline')
    if (-not $m.Success) { return @("/") }
    $basePaths = @(Get-QuotedValues -InputText $m.Groups[1].Value)
    if ($basePaths.Count -eq 0) {
        $basePath = Select-FirstQuotedValue -InputText $m.Groups[1].Value
        if (-not [string]::IsNullOrWhiteSpace($basePath)) {
            $basePaths = @($basePath)
        }
    }
    if ($basePaths.Count -eq 0) { return @("/") }
    return $basePaths
}

function Get-AnnotationBlock {
    param(
        [string[]]$Lines,
        [int]$StartIndex
    )
    $block = [System.Collections.Generic.List[string]]::new()
    for ($i = $StartIndex - 1; $i -ge 0; $i--) {
        $line = $Lines[$i]
        if ([string]::IsNullOrWhiteSpace($line)) {
            break
        }
        $block.Insert(0, $line)
    }
    return ($block -join "`n")
}

$controllerFiles = Get-ChildItem -Path $Root -Recurse -File -Filter *Controller.java |
    Where-Object { $_.FullName -notmatch '\\target\\' }

$issues = [System.Collections.Generic.List[pscustomobject]]::new()
$checkedMethods = 0

$methodPattern = 'public\s+(?<return>.+?)\s+(?<name>[A-Za-z_]\w*)\s*\((?<params>.*?)\)\s*\{'

foreach ($file in $controllerFiles) {
    $content = Get-Content -Raw -Path $file.FullName
    $lines = Get-Content -Path $file.FullName
    $classBasePaths = @(Parse-ClassBasePaths -Content $content)
    $isApiAdapterController = $content -match 'implements\s+\w+Api\b'

    for ($lineIndex = 0; $lineIndex -lt $lines.Count; $lineIndex++) {
        if ($lines[$lineIndex] -notmatch '^\s*public\b') {
            continue
        }

        $annotationBlock = Get-AnnotationBlock -Lines $lines -StartIndex $lineIndex
        if ($annotationBlock -notmatch '@(?:Get|Post|Put|Delete|Patch|Request)Mapping') {
            continue
        }

        $signatureLines = [System.Collections.Generic.List[string]]::new()
        $braceFound = $false
        for ($sigIndex = $lineIndex; $sigIndex -lt $lines.Count; $sigIndex++) {
            $signatureLines.Add($lines[$sigIndex])
            if ($lines[$sigIndex] -match '\{') {
                $braceFound = $true
                break
            }
        }
        if (-not $braceFound) {
            continue
        }

        $signature = ($signatureLines -join ' ')
        $match = [regex]::Match($signature, $methodPattern, 'Singleline')
        if (-not $match.Success) {
            continue
        }

        $checkedMethods++
        $ann = $annotationBlock
        $params = $match.Groups['params'].Value
        $returnType = if ($null -eq $match.Groups['return'].Value) { "" } else { [string]$match.Groups['return'].Value }
        $returnType = $returnType.Trim()
        $methodName = $match.Groups['name'].Value

        $httpMethod = "REQUEST"
        if ($ann -match '@GetMapping') { $httpMethod = "GET" }
        elseif ($ann -match '@PostMapping') { $httpMethod = "POST" }
        elseif ($ann -match '@PutMapping') { $httpMethod = "PUT" }
        elseif ($ann -match '@DeleteMapping') { $httpMethod = "DELETE" }
        elseif ($ann -match '@PatchMapping') { $httpMethod = "PATCH" }

        $mappingArgs = [regex]::Match($ann, '@(?:Get|Post|Put|Delete|Patch|Request)Mapping\(([^)]*)\)')
        $methodPath = ""
        if ($mappingArgs.Success) {
            $methodPath = Select-FirstQuotedValue -InputText $mappingArgs.Groups[1].Value
        }
        $fullPaths = @($classBasePaths | ForEach-Object { Normalize-PathJoin -BasePath $_ -MethodPath $methodPath })
        $fullPath = $fullPaths[0]

        $placeholderMatches = [regex]::Matches($fullPath, '\{([^}/]+)\}')
        $placeholders = @()
        foreach ($pm in $placeholderMatches) { $placeholders += $pm.Groups[1].Value }

        $pathVarMatches = [regex]::Matches($params, '@PathVariable(?:\((?:\s*(?:value|name)\s*=\s*)?"?([^",)]+)"?[^)]*\))?(?:\s+@\w+(?:\([^)]*\))?)*\s+[^\s,<>]+(?:<[^>]+>)?\s+([A-Za-z_]\w*)', 'Singleline')
        $pathVars = @()
        foreach ($vm in $pathVarMatches) {
            $explicitName = if ($null -eq $vm.Groups[1].Value) { "" } else { [string]$vm.Groups[1].Value }
            $paramName = if ($null -eq $vm.Groups[2].Value) { "" } else { [string]$vm.Groups[2].Value }
            $explicitName = $explicitName.Trim()
            $paramName = $paramName.Trim()
            if ([string]::IsNullOrWhiteSpace($explicitName)) { $pathVars += $paramName } else { $pathVars += $explicitName }
        }

        $missingPathVars = @($placeholders | Where-Object { $_ -notin $pathVars })
        $unusedPathVars = @($pathVars | Where-Object { $_ -notin $placeholders })

        if ($missingPathVars.Count -gt 0 -or $unusedPathVars.Count -gt 0) {
            $issues.Add([pscustomobject]@{
                    File   = $file.FullName
                    Method = $methodName
                    Type   = "path-variable-mismatch"
                    Detail = "path=$fullPath placeholders=[$($placeholders -join ',')] pathVars=[$($pathVars -join ',')]"
                })
        }

        $requestBodyCount = [regex]::Matches($params, '@RequestBody').Count
        if ($requestBodyCount -gt 1) {
            $issues.Add([pscustomobject]@{
                    File   = $file.FullName
                    Method = $methodName
                    Type   = "multiple-request-body"
                    Detail = "requestBodyCount=$requestBodyCount"
                })
        }

        if ($httpMethod -eq "GET" -and $requestBodyCount -gt 0) {
            $issues.Add([pscustomobject]@{
                    File   = $file.FullName
                    Method = $methodName
                    Type   = "get-with-request-body"
                    Detail = "GET endpoint should not declare @RequestBody"
                })
        }

        $isApiEndpoint = @($fullPaths | Where-Object { $_.StartsWith("/api/") }).Count -gt 0
        $isInternalApi = @($fullPaths | Where-Object { $_.StartsWith("/internal/") }).Count -gt 0
        $isGatewayFallback = @($fullPaths | Where-Object { $_.StartsWith("/gateway/") }).Count -gt 0
        if ($isApiEndpoint -and -not $isInternalApi -and -not $isGatewayFallback -and -not $isApiAdapterController -and $returnType -notmatch '(^|[<\s])Result<') {
            $issues.Add([pscustomobject]@{
                    File   = $file.FullName
                    Method = $methodName
                    Type   = "non-standard-result-wrapper"
                    Detail = "returnType=$returnType paths=[$($fullPaths -join ', ')]"
                })
        }
    }
}

Write-Host ("[api-contract] checked_controllers={0} checked_methods={1}" -f $controllerFiles.Count, $checkedMethods)

if ($issues.Count -gt 0) {
    Write-Host ("[api-contract] issues={0}" -f $issues.Count)
    foreach ($issue in $issues) {
        Write-Host ("[ERROR] {0}::{1} [{2}] {3}" -f $issue.File, $issue.Method, $issue.Type, $issue.Detail)
    }
    exit 1
}

Write-Host "[api-contract] no issues found"
