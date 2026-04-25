export interface BrowserLocationLike {
    href: string;
    assign?: (url: string) => void;
}

export interface WebviewOpenTarget<TGuard = unknown> {
    url: string;
    query: Record<string, unknown>;
    guard?: TGuard;
}

export interface OpenExternalPageOptions<TGuard = unknown> {
    browserLocation?: BrowserLocationLike | null;
    query?: Record<string, unknown>;
    guard?: TGuard;
    openWebview?: (target: WebviewOpenTarget<TGuard>) => void;
}

function resolveBrowserLocation(): BrowserLocationLike | null {
    return typeof window !== "undefined" && window.location
        ? window.location
        : null;
}

export function openExternalPage<TGuard = unknown>(
    url: string,
    options: OpenExternalPageOptions<TGuard> = {},
): "browser" | "webview" {
    const browserLocation = options.browserLocation ?? resolveBrowserLocation();
    if (browserLocation) {
        if (typeof browserLocation.assign === "function") {
            browserLocation.assign(url);
        } else {
            browserLocation.href = url;
        }
        return "browser";
    }

    const target: WebviewOpenTarget<TGuard> = {
        url,
        query: options.query ?? {},
        guard: options.guard,
    };
    if (!options.openWebview) {
        throw new Error("openWebview is required when browser location is unavailable");
    }
    options.openWebview(target);
    return "webview";
}
