#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

function trimEnvValue(value) {
    return String(value ?? "").trim();
}

function readEnvFile(filePath) {
    const state = {
        keys: [],
        values: {},
    };
    if (!fs.existsSync(filePath)) {
        return state;
    }

    const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
    for (const rawLine of lines) {
        const trimmedLine = trimEnvValue(rawLine).replace(/^\uFEFF/, "");
        if (!trimmedLine || trimmedLine.startsWith("#")) {
            continue;
        }
        const separatorIndex = trimmedLine.indexOf("=");
        if (separatorIndex < 0) {
            continue;
        }
        const key = trimEnvValue(trimmedLine.slice(0, separatorIndex));
        const value = trimmedLine.slice(separatorIndex + 1);
        if (!key) {
            continue;
        }
        if (!(key in state.values)) {
            state.keys.push(key);
        }
        state.values[key] = value;
    }
    return state;
}

function writeEnvFile(filePath, state) {
    const content = state.keys.map((key) => `${key}=${state.values[key] ?? ""}`).join("\n");
    fs.writeFileSync(filePath, `${content}\n`, "utf8");
}

function setEnvValue(state, key, value = "") {
    if (!(key in state.values)) {
        state.keys.push(key);
    }
    state.values[key] = value;
}

function firstNonEmptyValue(...candidates) {
    for (const candidate of candidates) {
        const trimmed = trimEnvValue(candidate);
        if (trimmed) {
            return trimmed;
        }
    }
    return "";
}

function resolvePublicBaseUrl(url) {
    const trimmed = trimEnvValue(url);
    if (!trimmed) {
        return "";
    }
    try {
        return new URL(trimmed).origin.replace(/\/+$/, "");
    } catch {
        return trimmed.replace(/\/+$/, "");
    }
}

function parseCpolarDomainMap(rawValue) {
    const values = {};
    const normalized = trimEnvValue(rawValue);
    if (!normalized) {
        return values;
    }
    for (const entry of normalized.split(",")) {
        const trimmedEntry = trimEnvValue(entry);
        if (!trimmedEntry) {
            continue;
        }
        const separatorIndex = trimmedEntry.indexOf("=");
        if (separatorIndex < 0) {
            continue;
        }
        const key = trimEnvValue(trimmedEntry.slice(0, separatorIndex)).toLowerCase();
        const value = resolvePublicBaseUrl(trimmedEntry.slice(separatorIndex + 1));
        if (key && value) {
            values[key] = value;
        }
    }
    return values;
}

function buildCpolarDomainMap(publicBaseUrl, frontendBaseUrl) {
    const parts = [];
    if (publicBaseUrl) {
        parts.push(`public=${publicBaseUrl}`);
    }
    if (frontendBaseUrl) {
        parts.push(`frontend=${frontendBaseUrl}`);
    }
    return parts.join(",");
}

function addUniqueValue(values, value) {
    if (value && !values.includes(value)) {
        values.push(value);
    }
}

function addOriginVariants(origins, baseUrl) {
    const resolvedBaseUrl = resolvePublicBaseUrl(baseUrl);
    if (!resolvedBaseUrl) {
        return;
    }
    addUniqueValue(origins, resolvedBaseUrl);
    if (resolvedBaseUrl.startsWith("http://")) {
        addUniqueValue(origins, `https://${resolvedBaseUrl.slice("http://".length)}`);
    }
}

function parseArguments(argv) {
    const args = {
        root: "",
        preferredLocalIpv4: "",
    };
    for (const entry of argv) {
        if (entry.startsWith("--preferred-local-ipv4=")) {
            args.preferredLocalIpv4 = trimEnvValue(entry.slice("--preferred-local-ipv4=".length));
            continue;
        }
        if (!args.root) {
            args.root = entry;
        }
    }
    if (!args.root) {
        throw new Error("repository root path is required");
    }
    return args;
}

function syncEnvironmentFiles(rootPath, preferredLocalIpv4) {
    const rootEnvPath = path.join(rootPath, ".env");
    const dockerEnvPath = path.join(rootPath, "docker", ".env");
    const frontendDevEnvPath = path.join(rootPath, "my-shop-uniapp", ".env.development");
    const frontendProdEnvPath = path.join(rootPath, "my-shop-uniapp", ".env.production");

    const rootEnv = readEnvFile(rootEnvPath);
    const dockerEnv = readEnvFile(dockerEnvPath);

    for (const key of rootEnv.keys) {
        setEnvValue(dockerEnv, key, rootEnv.values[key]);
    }

    const nginxHttpPort = dockerEnv.values.PORT_NGINX_HTTP || "18080";
    const localGatewayBaseUrl = `http://127.0.0.1:${nginxHttpPort}`;
    const gatewayUpstream = preferredLocalIpv4
        ? `${preferredLocalIpv4}:8080`
        : "host.docker.internal:8080";
    const authUpstream = preferredLocalIpv4
        ? `${preferredLocalIpv4}:8081`
        : "host.docker.internal:8081";

    const cpolarDomainMap = parseCpolarDomainMap(
        firstNonEmptyValue(rootEnv.values.CPOLAR_DOMAIN_MAP, dockerEnv.values.CPOLAR_DOMAIN_MAP),
    );
    const publicBaseUrl = firstNonEmptyValue(
        cpolarDomainMap.public,
        resolvePublicBaseUrl(rootEnv.values.CPOLAR_PUBLIC_BASE_URL),
        resolvePublicBaseUrl(rootEnv.values.CPOLAR_DOMAIN),
        resolvePublicBaseUrl(dockerEnv.values.CPOLAR_PUBLIC_BASE_URL),
        resolvePublicBaseUrl(dockerEnv.values.CPOLAR_DOMAIN),
        localGatewayBaseUrl,
    );
    const frontendBaseUrl = firstNonEmptyValue(
        cpolarDomainMap.frontend,
        resolvePublicBaseUrl(rootEnv.values.CPOLAR_FRONTEND_BASE_URL),
        resolvePublicBaseUrl(dockerEnv.values.CPOLAR_FRONTEND_BASE_URL),
        publicBaseUrl,
    );
    const cpolarDomainMapValue = buildCpolarDomainMap(publicBaseUrl, frontendBaseUrl);

    setEnvValue(dockerEnv, "NGINX_GATEWAY_UPSTREAM", gatewayUpstream);
    setEnvValue(dockerEnv, "NGINX_AUTH_UPSTREAM", authUpstream);
    setEnvValue(rootEnv, "CPOLAR_DOMAIN_MAP", cpolarDomainMapValue);
    setEnvValue(dockerEnv, "CPOLAR_DOMAIN_MAP", cpolarDomainMapValue);
    setEnvValue(rootEnv, "CPOLAR_DOMAIN", publicBaseUrl);
    setEnvValue(rootEnv, "CPOLAR_PUBLIC_BASE_URL", publicBaseUrl);
    setEnvValue(rootEnv, "CPOLAR_FRONTEND_BASE_URL", frontendBaseUrl);
    setEnvValue(dockerEnv, "CPOLAR_DOMAIN", publicBaseUrl);
    setEnvValue(dockerEnv, "CPOLAR_PUBLIC_BASE_URL", publicBaseUrl);
    setEnvValue(dockerEnv, "CPOLAR_FRONTEND_BASE_URL", frontendBaseUrl);

    const oauthRedirectUris = [];
    addUniqueValue(oauthRedirectUris, `${frontendBaseUrl}/callback`);
    addUniqueValue(oauthRedirectUris, `${publicBaseUrl}/callback`);
    addUniqueValue(oauthRedirectUris, `http://127.0.0.1:${nginxHttpPort}/callback`);
    addUniqueValue(oauthRedirectUris, "http://127.0.0.1:3000/callback");
    addUniqueValue(oauthRedirectUris, "http://127.0.0.1:5173/callback");
    addUniqueValue(oauthRedirectUris, "http://localhost:5173/callback");

    const originPatterns = [];
    addUniqueValue(originPatterns, "http://127.0.0.1:*");
    addUniqueValue(originPatterns, "https://127.0.0.1:*");
    addUniqueValue(originPatterns, "http://localhost:*");
    addUniqueValue(originPatterns, "https://localhost:*");
    addOriginVariants(originPatterns, publicBaseUrl);
    addOriginVariants(originPatterns, frontendBaseUrl);

    for (const envState of [rootEnv, dockerEnv]) {
        setEnvValue(
            envState,
            "ALIPAY_NOTIFY_URL",
            `${publicBaseUrl}/api/v1/payment/alipay/notify`,
        );
        setEnvValue(
            envState,
            "ALIPAY_RETURN_URL",
            `${frontendBaseUrl}/#/pages/app/payments/index`,
        );
        setEnvValue(
            envState,
            "GITHUB_REDIRECT_URI",
            `${publicBaseUrl}/login/oauth2/code/github`,
        );
        setEnvValue(
            envState,
            "APP_OAUTH2_GITHUB_ERROR_URL",
            `${frontendBaseUrl}/auth/error`,
        );
        setEnvValue(
            envState,
            "APP_OAUTH2_WEB_REDIRECT_URIS",
            oauthRedirectUris.join(","),
        );
        setEnvValue(
            envState,
            "APP_SECURITY_CORS_ALLOWED_ORIGIN_PATTERNS",
            originPatterns.join(","),
        );
    }

    writeEnvFile(rootEnvPath, rootEnv);
    writeEnvFile(dockerEnvPath, dockerEnv);

    const frontendEnv = {
        keys: [
            "VITE_API_BASE_URL",
            "VITE_DEV_PROXY_TARGET",
            "VITE_CPOLAR_DOMAIN",
            "VITE_OAUTH_CLIENT_ID",
            "VITE_OAUTH_REDIRECT_URI",
            "VITE_SEARCH_FALLBACK_TIMEOUT",
        ],
        values: {
            VITE_API_BASE_URL: publicBaseUrl,
            VITE_DEV_PROXY_TARGET: localGatewayBaseUrl,
            VITE_CPOLAR_DOMAIN: frontendBaseUrl,
            VITE_OAUTH_CLIENT_ID: "web-client",
            VITE_OAUTH_REDIRECT_URI: `${frontendBaseUrl}/callback`,
            VITE_SEARCH_FALLBACK_TIMEOUT: "5000",
        },
    };

    writeEnvFile(frontendDevEnvPath, frontendEnv);
    writeEnvFile(frontendProdEnvPath, frontendEnv);

    return {
        rootEnvPath,
        dockerEnvPath,
        frontendDevEnvPath,
        frontendProdEnvPath,
        rootEnv: rootEnv.values,
        dockerEnv: dockerEnv.values,
        frontendEnv: frontendEnv.values,
    };
}

try {
    const { root, preferredLocalIpv4 } = parseArguments(process.argv.slice(2));
    const result = syncEnvironmentFiles(path.resolve(root), preferredLocalIpv4);
    process.stdout.write(JSON.stringify(result));
} catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    process.stderr.write(`${message}\n`);
    process.exit(1);
}
