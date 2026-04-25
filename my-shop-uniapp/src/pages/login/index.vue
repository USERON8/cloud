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
              brand: "My Shop Cloud",
              title: "Sign in to My Shop Cloud",
              subtitle: "Continue with your platform account.",
              accessMode: "Access mode",
              targetRoute: "Return path",
              auth: "OAuth 2.1",
              heading: "Sign in",
              body: "Authorization is handled by the cloud identity service.",
              recommended: "Account entry",
              recommendedBody: "Your role is resolved after sign-in.",
              action: "Continue with account",
              storefront: "Storefront access",
              back: "Back to market",
              error: "Failed to start sign-in",
          }
        : {
              brand: "云端商城工作台",
              title: "登录 My Shop Cloud",
              subtitle: "使用平台账号继续访问。",
              accessMode: "访问模式",
              targetRoute: "返回路径",
              auth: "OAuth 2.1",
              heading: "登录",
              body: "授权由云端身份服务完成。",
              recommended: "账号入口",
              recommendedBody: "登录成功后自动识别当前角色。",
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
        <view class="page-container auth-shell">
            <view class="auth-panel display-panel fade-in-up">
                <view class="auth-topbar">
                    <view class="brand-line">
                        <text class="brand-mark">MS</text>
                        <text class="brand-name">{{ copy.brand }}</text>
                    </view>
                    <LocaleSwitch />
                </view>

                <view class="auth-main">
                    <view class="auth-copy">
                        <text class="hero-eyebrow">{{ copy.auth }}</text>
                        <text class="hero-title auth-title">{{ copy.title }}</text>
                        <text class="hero-subtitle auth-subtitle">
                            {{ copy.subtitle }}
                        </text>

                        <view class="context-grid">
                            <view class="context-item">
                                <text class="context-label">{{ copy.accessMode }}</text>
                                <text class="context-value">{{ entryLabel }}</text>
                            </view>
                            <view class="context-item">
                                <text class="context-label">{{ copy.targetRoute }}</text>
                                <text class="context-value">{{ redirectPath }}</text>
                            </view>
                        </view>
                    </view>

                    <view class="signin-panel">
                        <view class="signin-header">
                            <text class="signin-title">{{ copy.heading }}</text>
                            <text class="signin-copy">{{ copy.body }}</text>
                        </view>

                        <view class="signin-hint">
                            <text class="hint-title">{{ copy.recommended }}</text>
                            <text class="hint-copy">{{ copy.recommendedBody }}</text>
                        </view>

                        <button
                            class="btn-primary action-button"
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

                        <button class="btn-outline action-button" @click="backToMarket">
                            {{ copy.back }}
                        </button>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<style scoped>
.page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    padding: 24px 0;
}

.auth-shell {
    display: flex;
    align-items: center;
    min-height: calc(100vh - 48px);
}

.auth-panel {
    width: 100%;
    padding: 28px;
    border-radius: var(--radius-xl);
    display: flex;
    flex-direction: column;
    gap: 30px;
}

.auth-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
}

.brand-line {
    display: inline-flex;
    align-items: center;
    gap: 12px;
}

.brand-mark {
    width: 42px;
    height: 42px;
    border-radius: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--accent), var(--highlight));
    color: #04111c;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.12em;
}

.brand-name {
    font-size: 12px;
    color: var(--text-muted);
    font-weight: 800;
    letter-spacing: 0.16em;
    text-transform: uppercase;
}

.auth-main {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(340px, 420px);
    gap: 28px;
    align-items: stretch;
}

.auth-copy {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 18px;
    min-height: 430px;
}

.auth-title {
    max-width: 640px;
}

.auth-subtitle {
    max-width: 520px;
}

.context-grid {
    width: min(560px, 100%);
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    padding-top: 8px;
}

.context-item {
    min-width: 0;
    padding: 16px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--panel-border);
}

.context-label {
    font-size: 12px;
    color: var(--text-soft);
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.context-value {
    margin-top: 10px;
    display: block;
    font-size: 14px;
    font-weight: 700;
    color: var(--text-main);
    line-height: 1.55;
    overflow-wrap: anywhere;
}

.signin-panel {
    padding: 24px;
    border-radius: var(--radius-lg);
    background: rgba(5, 14, 23, 0.62);
    border: 1px solid var(--panel-border-strong);
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 16px;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.signin-header {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.signin-title {
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.04em;
    color: var(--text-main);
}

.signin-copy {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.7;
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

.action-button {
    width: 100%;
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
    .auth-main {
        grid-template-columns: minmax(0, 1fr);
    }

    .auth-copy {
        min-height: auto;
    }
}

@media (max-width: 768px) {
    .page {
        align-items: flex-start;
        padding: 12px 0 28px;
    }

    .auth-panel {
        padding: 18px;
        gap: 22px;
        border-radius: 28px;
    }

    .auth-main {
        gap: 20px;
    }

    .context-grid {
        grid-template-columns: minmax(0, 1fr);
    }

    .signin-panel {
        padding: 18px;
    }

    .signin-title {
        font-size: 26px;
    }
}
</style>
