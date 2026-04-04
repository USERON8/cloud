<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { startAuthorization } from "../../api/auth";
import { navigateTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { toast } from "../../utils/ui";

const redirectPath = ref<string>(Routes.appHome);
const entryType = ref("");
const startingProvider = ref<"password" | "">("");

const entryLabel = computed(() => {
    if (entryType.value === "admin") return "admin";
    if (entryType.value === "merchant") return "merchant";
    return "customer";
});

onLoad((query) => {
    if (typeof query.redirect === "string") {
        try {
            redirectPath.value = decodeURIComponent(query.redirect);
        } catch {
            redirectPath.value = query.redirect;
        }
    }
    if (typeof query.entry === "string") {
        entryType.value = query.entry.toLowerCase();
    }
});

async function handleAuthorizationStart(provider: "password"): Promise<void> {
    startingProvider.value = provider;
    try {
        await startAuthorization(redirectPath.value);
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to start sign-in",
        );
        startingProvider.value = "";
    }
}

function backToMarket(): void {
    navigateTo(Routes.market);
}
</script>

<template>
    <view class="page">
        <view class="page-container auth-layout">
            <view class="hero-panel fade-in-up">
                <view class="hero-badge">My Shop Cloud</view>
                <text class="hero-title"
                    >Fast sign-in for your storefront workspace</text
                >
                <text class="hero-copy">
                    One unified H5 entry for customer, merchant, and admin
                    access. Continue with the authorization server to enter the
                    storefront workspace.
                </text>

                <view class="hero-points">
                    <view class="point surface-card">
                        <text class="point-label">Access mode</text>
                        <text class="point-value">{{
                            entryLabel === "admin"
                                ? "Administrator workspace"
                                : entryLabel === "merchant"
                                  ? "Merchant workspace"
                                  : "Customer workspace"
                        }}</text>
                    </view>
                    <view class="point surface-card">
                        <text class="point-label">Redirect target</text>
                        <text class="point-value">{{ redirectPath }}</text>
                    </view>
                </view>
            </view>

            <view class="signin-card glass-card fade-in-up">
                <view class="header">
                    <text class="eyebrow">OAuth 2.1</text>
                    <text class="title">Sign in</text>
                    <text class="muted"
                        >Use your platform account to continue. The same login
                        page supports customer, merchant, and administrator
                        access.</text
                    >
                </view>

                <view class="signin-content">
                    <view class="signin-hint">
                        <text class="hint-title">Recommended</text>
                        <text class="hint-copy">
                            The authorization server returns your effective
                            roles after successful sign-in, so the same page can
                            serve customers, merchants, and administrators. The
                            current entry is optimized for
                            {{
                                entryLabel === "admin"
                                    ? " administrator operations"
                                    : entryLabel === "merchant"
                                      ? " merchant operations"
                                      : " customer browsing"
                            }}.
                        </text>
                    </view>

                    <button
                        class="btn-primary full-width action-button"
                        :loading="startingProvider === 'password'"
                        @click="handleAuthorizationStart('password')"
                    >
                        Continue with account sign-in
                    </button>

                    <view class="divider">
                        <view class="divider-line" />
                        <text class="divider-text">Marketplace access</text>
                        <view class="divider-line" />
                    </view>

                    <button
                        class="btn-secondary full-width action-button"
                        @click="backToMarket"
                    >
                        Back to the market
                    </button>
                </view>
            </view>
        </view>
    </view>
</template>

<style scoped>
.page {
    min-height: 100vh;
    padding: 24px 16px;
}

.auth-layout {
    min-height: calc(100vh - 48px);
    display: grid;
    grid-template-columns: minmax(0, 1.15fr) minmax(360px, 420px);
    gap: 18px;
    align-items: stretch;
}

.hero-panel {
    padding: 28px;
    border-radius: var(--radius-xl);
    background: linear-gradient(
        160deg,
        rgba(14, 27, 36, 0.96),
        rgba(11, 107, 95, 0.9)
    );
    color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 24px;
    box-shadow: 0 28px 60px rgba(13, 24, 32, 0.28);
}

.hero-badge {
    align-self: flex-start;
    padding: 8px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.16);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.04em;
}

.hero-title {
    font-size: 34px;
    line-height: 1.15;
    font-weight: 700;
    letter-spacing: -0.03em;
}

.hero-copy {
    font-size: 14px;
    line-height: 1.7;
    color: rgba(255, 255, 255, 0.82);
    max-width: 520px;
}

.hero-points {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.point {
    padding: 16px;
    background: rgba(255, 255, 255, 0.18);
    border-color: rgba(255, 255, 255, 0.16);
    box-shadow: none;
}

.point-label {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.7);
}

.point-value {
    margin-top: 8px;
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: #fff;
    word-break: break-all;
}

.signin-card {
    width: 100%;
    padding: 24px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 18px;
}

.header {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.eyebrow {
    font-size: 12px;
    color: var(--accent);
    font-weight: 700;
}

.title {
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.muted {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.6;
}

.signin-content {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.signin-hint {
    border: 1px solid rgba(27, 44, 74, 0.08);
    background: rgba(255, 255, 255, 0.72);
    border-radius: 18px;
    padding: 14px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.hint-title {
    font-size: 12px;
    font-weight: 700;
    color: var(--accent);
}

.hint-copy {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.6;
}

.full-width {
    width: 100%;
}

.action-button {
    min-height: 46px;
}

.divider {
    display: flex;
    align-items: center;
    gap: 10px;
}

.divider-line {
    flex: 1;
    height: 1px;
    background: rgba(148, 163, 184, 0.3);
}

.divider-text {
    color: var(--text-soft);
    font-size: 12px;
}

@media (max-width: 900px) {
    .auth-layout {
        grid-template-columns: 1fr;
    }

    .hero-title {
        font-size: 28px;
    }

    .hero-points {
        grid-template-columns: 1fr;
    }
}
</style>
