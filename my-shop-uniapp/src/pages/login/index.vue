<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { startAuthorization } from "../../api/auth";
import LocaleSwitch from "../../components/LocaleSwitch.vue";
import { useLocale } from "../../i18n/locale";
import { navigateTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { toast } from "../../utils/ui";

const redirectPath = ref<string>(Routes.appHome);
const entryType = ref("");
const startingProvider = ref<"password" | "">("");

const { locale } = useLocale();

const entryLabel = computed(() => {
    if (locale.value === "en-US") {
        if (entryType.value === "admin") {
            return "Admin entry";
        }
        if (entryType.value === "merchant") {
            return "Merchant entry";
        }
        return "User entry";
    }

    if (entryType.value === "admin") {
        return "管理入口";
    }
    if (entryType.value === "merchant") {
        return "商家入口";
    }
    return "用户入口";
});

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              title: "Step into the cloud console without the usual friction.",
              subtitle:
                  "One sign-in surface now supports user, merchant, and admin workflows while keeping the handoff cleaner and more consistent.",
              accessMode: "Access mode",
              targetRoute: "Target route",
              auth: "OAuth 2.1",
              heading: "Sign in",
              body:
                  "Continue with your platform account. The same surface now adapts to user, merchant, and admin access.",
              recommended: "Recommended path",
              recommendedBody:
                  "Authorization returns the effective role after sign-in, so one flow is enough for multiple identities. The current entry has already been tuned for your context.",
              action: "Continue with account",
              storefront: "Storefront access",
              back: "Back to market",
              error: "Failed to start sign-in",
          }
        : {
              title: "更顺手地进入云端经营控制台。",
              subtitle:
                  "一个登录入口同时承接用户、商家和管理员工作流，并把授权切换过程做得更干净、更统一。",
              accessMode: "访问模式",
              targetRoute: "跳转目标",
              auth: "OAuth 2.1",
              heading: "登录",
              body:
                  "使用平台账号继续访问。当前页面已经统一承接用户、商家与管理员登录流程。",
              recommended: "推荐方式",
              recommendedBody:
                  "授权服务会在登录成功后返回当前有效角色，因此一个入口就足以适配多种身份。当前入口已按你的上下文做了默认优化。",
              action: "使用账号继续登录",
              storefront: "商城访问",
              back: "返回商城",
              error: "发起登录失败",
          },
);

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
        toast(error instanceof Error ? error.message : copy.value.error);
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
                <view class="hero-topbar">
                    <view class="hero-brand">
                        <text class="hero-badge">My Shop Cloud</text>
                    </view>
                    <LocaleSwitch />
                </view>

                <view class="hero-main">
                    <text class="hero-title">{{ copy.title }}</text>
                    <text class="hero-copy">{{ copy.subtitle }}</text>
                </view>

                <view class="hero-points">
                    <view class="point surface-card">
                        <text class="point-label">{{ copy.accessMode }}</text>
                        <text class="point-value">{{ entryLabel }}</text>
                    </view>
                    <view class="point surface-card">
                        <text class="point-label">{{ copy.targetRoute }}</text>
                        <text class="point-value">{{ redirectPath }}</text>
                    </view>
                </view>
            </view>

            <view class="signin-card glass-card fade-in-up">
                <view class="header">
                    <text class="eyebrow">{{ copy.auth }}</text>
                    <text class="title">{{ copy.heading }}</text>
                    <text class="muted">{{ copy.body }}</text>
                </view>

                <view class="signin-content">
                    <view class="signin-hint">
                        <text class="hint-title">{{ copy.recommended }}</text>
                        <text class="hint-copy">{{ copy.recommendedBody }}</text>
                    </view>

                    <button
                        class="btn-primary full-width action-button"
                        :loading="startingProvider === 'password'"
                        @click="handleAuthorizationStart('password')"
                    >
                        {{ copy.action }}
                    </button>

                    <view class="divider">
                        <view class="divider-line" />
                        <text class="divider-text">{{ copy.storefront }}</text>
                        <view class="divider-line" />
                    </view>

                    <button
                        class="btn-secondary full-width action-button"
                        @click="backToMarket"
                    >
                        {{ copy.back }}
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
    background:
        radial-gradient(
            circle at 100% 0%,
            rgba(95, 209, 194, 0.18),
            transparent 26%
        ),
        linear-gradient(160deg, rgba(9, 18, 29, 0.98), rgba(8, 28, 43, 0.92));
    color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 28px;
    box-shadow: 0 32px 70px rgba(1, 7, 14, 0.4);
    border: 1px solid var(--panel-border-strong);
}

.hero-topbar {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
}

.hero-badge {
    align-self: flex-start;
    padding: 9px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.08);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.hero-main {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.hero-title {
    font-size: 38px;
    line-height: 1.08;
    font-weight: 800;
    letter-spacing: -0.05em;
}

.hero-copy {
    font-size: 15px;
    line-height: 1.8;
    color: rgba(242, 247, 251, 0.8);
    max-width: 560px;
}

.hero-points {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.point {
    padding: 16px;
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow: none;
}

.point-label {
    font-size: 12px;
    color: rgba(242, 247, 251, 0.62);
    text-transform: uppercase;
    letter-spacing: 0.08em;
}

.point-value {
    margin-top: 10px;
    display: block;
    font-size: 15px;
    font-weight: 700;
    color: #fff;
    word-break: break-all;
    line-height: 1.6;
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
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.title {
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.04em;
    color: var(--text-main);
}

.muted {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.7;
}

.signin-content {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.signin-hint {
    border: 1px solid var(--panel-border);
    background: rgba(255, 255, 255, 0.04);
    border-radius: 20px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.hint-title {
    font-size: 12px;
    font-weight: 800;
    color: var(--accent);
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.hint-copy {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.7;
}

.full-width {
    width: 100%;
}

.action-button {
    min-height: 48px;
}

.divider {
    display: flex;
    align-items: center;
    gap: 10px;
}

.divider-line {
    flex: 1;
    height: 1px;
    background: rgba(148, 163, 184, 0.16);
}

.divider-text {
    color: var(--text-soft);
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

@media (max-width: 960px) {
    .auth-layout {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .hero-title {
        font-size: 30px;
    }

    .hero-points {
        grid-template-columns: 1fr;
    }
}
</style>
