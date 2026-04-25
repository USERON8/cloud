import assert from "node:assert/strict";
import test from "node:test";
import { openExternalPage } from "./external-navigation.ts";

test("openExternalPage prefers browser redirect when location.assign is available", () => {
    let assignedUrl = "";

    const mode = openExternalPage("https://example.com/pay", {
        browserLocation: {
            href: "",
            assign(url) {
                assignedUrl = url;
            },
        },
    });

    assert.equal(mode, "browser");
    assert.equal(assignedUrl, "https://example.com/pay");
});

test("openExternalPage falls back to browser href when assign is unavailable", () => {
    const browserLocation = { href: "" };

    const mode = openExternalPage("https://example.com/pay", {
        browserLocation,
    });

    assert.equal(mode, "browser");
    assert.equal(browserLocation.href, "https://example.com/pay");
});

test("openExternalPage opens webview when browser location is unavailable", () => {
    let openedTarget;

    const mode = openExternalPage("/api/payment-checkouts/ticket", {
        query: { paymentNo: "PAY-123" },
        guard: {
            requiresAuth: true,
            roles: ["USER", "MERCHANT", "ADMIN"],
        },
        openWebview(target) {
            openedTarget = target;
        },
    });

    assert.equal(mode, "webview");
    assert.deepEqual(openedTarget, {
        url: "/api/payment-checkouts/ticket",
        query: { paymentNo: "PAY-123" },
        guard: {
            requiresAuth: true,
            roles: ["USER", "MERCHANT", "ADMIN"],
        },
    });
});
