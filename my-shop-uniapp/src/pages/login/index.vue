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
const credentialsMode = ref(false);
const username = ref("");
const password = ref("");
const feedback = ref("");
const submitting = ref(false);

const { locale } = useLocale();

const entryLabel = computed(() => {
    if (locale.value === "en-US") {
        if (entryType.value === "admin") return "Admin entry";
        if (entryType.value === "merchant") return "Merchant entry";
        return "User entry";
    }

    if (entryType.value === "admin") return "管理入口";
    if (entryType.value === "merchant") return "商家入口";
    return "用户入口";
});

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              brand: "ShopMall",
              title: "Welcome to ShopMall",
              subtitle: "Sign in to continue shopping, checkout, and manage your account.",
              accessMode: "Access mode",
              targetRoute: "Return path",
              auth: "Account",
              heading: "Sign in",
              body: "Authorization is handled by the cloud identity service.",
              recommended: "Unified account",
              recommendedBody: "Your role and permissions are resolved after sign-in.",
              action: "Continue with account",
              username: "Username",
              usernamePlaceholder: "Enter your username",
              password: "Password",
              passwordPlaceholder: "Enter your password",
              submit: "Sign in securely",
              github: "Continue with GitHub",
              invalidCredentials: "The username or password is incorrect.",
              signedOut: "You have signed out.",
              required: "Enter both your username and password.",
              connecting: "Connecting to the secure sign-in service…",
              storefront: "Storefront",
              back: "Back to market",
              error: "Failed to start sign-in",
          }
        : {
              brand: "ShopMall",
              title: "欢迎来到 ShopMall",
              subtitle: "登录后继续购物、结算订单，并管理你的账号信息。",
              accessMode: "访问模式",
              targetRoute: "返回路径",
              auth: "账号登录",
              heading: "登录",
              body: "授权由云端身份服务完成，登录后会自动识别当前角色。",
              recommended: "统一账号入口",
              recommendedBody: "用户、商家和管理员权限会在登录后自动解析。",
              action: "使用账号继续登录",
              username: "用户名",
              usernamePlaceholder: "请输入用户名",
              password: "密码",
              passwordPlaceholder: "请输入密码",
              submit: "安全登录",
              github: "使用 GitHub 继续",
              invalidCredentials: "用户名或密码不正确，请重新输入。",
              signedOut: "你已安全退出登录。",
              required: "请输入用户名和密码。",
              connecting: "正在连接安全登录服务…",
              storefront: "商城访问",
              back: "返回商城",
              error: "发起登录失败",
          },
);

onLoad((query) => {
    credentialsMode.value = query.mode === "credentials";
    if (query.error === "invalid_credentials") {
        feedback.value = copy.value.invalidCredentials;
    } else if (query.logout === "true") {
        feedback.value = copy.value.signedOut;
    }
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
    if (!credentialsMode.value) {
        setTimeout(() => void handleAuthorizationStart("password"), 0);
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

function submitCredentials(): void {
    if (!username.value.trim() || !password.value) {
        feedback.value = copy.value.required;
        return;
    }
    if (typeof document === "undefined" || typeof window === "undefined") {
        toast(copy.value.error);
        return;
    }

    submitting.value = true;
    feedback.value = "";
    const form = document.createElement("form");
    form.method = "post";
    form.action = `${window.location.origin}/login/process`;
    form.style.display = "none";
    for (const [name, value] of [
        ["username", username.value.trim()],
        ["password", password.value],
    ]) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
}

function continueWithGitHub(): void {
    if (typeof window !== "undefined") {
        window.location.assign(`${window.location.origin}/oauth2/authorization/github`);
    }
}

function backToMarket(): void {
    navigateTo(Routes.market);
}
</script>

<template>
    <view class="login-page">
        <view class="login-shell">
            <view class="login-header">
                <text class="brand-name">{{ copy.brand }}</text>
                <view class="header-right">
                    <LocaleSwitch class="locale-compact" />
                    <button class="back-link" @click="backToMarket">
                        {{ copy.back }}
                    </button>
                </view>
            </view>

            <view class="login-card">
                <view class="welcome-panel">
                    <view class="welcome-copy">
                        <text class="eyebrow">{{ copy.auth }}</text>
                        <text class="welcome-title">{{ copy.title }}</text>
                        <text class="welcome-subtitle">{{ copy.subtitle }}</text>
                    </view>
                    <view class="shopping-visual">
                        <view class="bag bag-main" />
                        <view class="bag bag-side" />
                        <view class="box box-one" />
                        <view class="box box-two" />
                    </view>
                    <view class="service-mini">
                        <view class="service-mini-item">
                            <text class="service-dot">✓</text>
                            <text>正品保障</text>
                        </view>
                        <view class="service-mini-item">
                            <text class="service-dot">▣</text>
                            <text>极速配送</text>
                        </view>
                        <view class="service-mini-item">
                            <text class="service-dot">♡</text>
                            <text>无忧售后</text>
                        </view>
                    </view>
                </view>

                <view class="signin-panel">
                    <view class="signin-head">
                        <text class="signin-title">{{ copy.heading }}</text>
                        <text class="signin-copy">{{ copy.body }}</text>
                    </view>

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

                    <view v-if="feedback" class="signin-feedback">
                        {{ feedback }}
                    </view>

                    <view v-if="!credentialsMode" class="signin-hint">
                        <text class="hint-title">{{ copy.recommended }}</text>
                        <text class="hint-copy">{{ copy.connecting }}</text>
                    </view>

                    <button
                        v-if="!credentialsMode"
                        class="primary-action"
                        :loading="startingProvider === 'password'"
                        @click="handleAuthorizationStart('password')"
                    >
                        {{ copy.action }}
                    </button>

                    <form v-else class="credential-form" @submit="submitCredentials">
                        <label class="field-group">
                            <text class="field-label">{{ copy.username }}</text>
                            <input
                                v-model="username"
                                class="field-input"
                                type="text"
                                name="username"
                                autocomplete="username"
                                :placeholder="copy.usernamePlaceholder"
                                @confirm="submitCredentials"
                            />
                        </label>
                        <label class="field-group">
                            <text class="field-label">{{ copy.password }}</text>
                            <input
                                v-model="password"
                                class="field-input"
                                type="text"
                                password
                                name="password"
                                autocomplete="current-password"
                                :placeholder="copy.passwordPlaceholder"
                                confirm-type="done"
                                @confirm="submitCredentials"
                            />
                        </label>
                        <button
                            class="primary-action"
                            :loading="submitting"
                            form-type="submit"
                        >
                            {{ copy.submit }}
                        </button>
                        <button class="github-action" @click="continueWithGitHub">
                            {{ copy.github }}
                        </button>
                    </form>

                    <view class="divider">
                        <view class="divider-line" />
                        <text class="divider-text">{{ copy.storefront }}</text>
                        <view class="divider-line" />
                    </view>

                    <button class="secondary-action" @click="backToMarket">
                        {{ copy.back }}
                    </button>
                </view>
            </view>
        </view>
    </view>
</template>

<style scoped>
.login-page {
    min-height: 100vh;
    padding: 24px 0 44px;
    color: #15171a;
    background:
        linear-gradient(180deg, rgba(248, 249, 251, 0.96), #ffffff 44%),
        #ffffff;
}

.login-shell {
    width: min(1280px, calc(100% - 32px));
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 22px;
}

.login-header {
    min-height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 0 20px;
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 18px 60px rgba(20, 25, 35, 0.06);
}

.brand-name {
    font-size: 20px;
    font-weight: 800;
    color: #111316;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 12px;
}

.login-header :deep(.locale-compact) {
    gap: 0;
}

.login-header :deep(.switch-copy),
.login-header :deep(.switch-long) {
    display: none;
}

.login-header :deep(.switch-segment) {
    gap: 2px;
    padding: 2px;
    border-color: #e8ebef;
    border-radius: 4px;
    background: #fbfcfd;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
}

.login-header :deep(.switch-option) {
    min-width: 34px;
    min-height: 28px;
    padding: 0 8px;
    border-radius: 3px;
    color: #6f7781;
    box-shadow: none;
}

.login-header :deep(.switch-option.active) {
    border-color: #111316;
    background: #111316;
    color: #ffffff;
}

.login-header :deep(.switch-short) {
    font-size: 11px;
    letter-spacing: 0;
}

.back-link,
.primary-action,
.secondary-action {
    margin: 0;
    border-radius: 4px;
    font-weight: 700;
}

.back-link {
    min-height: 32px;
    padding: 8px 12px;
    border: 1px solid #d9dee6;
    background: #ffffff;
    color: #1d2025;
    font-size: 12px;
}

.login-card {
    min-height: 620px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 460px;
    gap: 18px;
}

.welcome-panel,
.signin-panel {
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 18px 58px rgba(18, 24, 35, 0.05);
}

.welcome-panel {
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 46px 58px;
    background:
        radial-gradient(circle at 70% 52%, rgba(213, 222, 236, 0.62), transparent 34%),
        linear-gradient(135deg, #f7f9fd 0%, #eef3fa 48%, #f9fbfe 100%);
}

.welcome-copy {
    display: flex;
    flex-direction: column;
    gap: 14px;
    position: relative;
    z-index: 2;
}

.eyebrow {
    color: #69727d;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.welcome-title {
    max-width: 620px;
    color: #101216;
    font-size: 42px;
    font-weight: 900;
    line-height: 1.18;
}

.welcome-subtitle {
    max-width: 560px;
    color: #5f6874;
    font-size: 15px;
    line-height: 1.75;
}

.shopping-visual {
    position: absolute;
    right: 72px;
    bottom: 122px;
    width: 330px;
    height: 260px;
}

.bag {
    position: absolute;
    border: 1px solid #eef1f5;
    background: #ffffff;
    box-shadow: 0 28px 60px rgba(31, 38, 49, 0.1);
}

.bag::before {
    content: "";
    position: absolute;
    left: 50%;
    top: -34px;
    width: 74px;
    height: 54px;
    border: 8px solid #dfe7f1;
    border-bottom: 0;
    border-radius: 999px 999px 0 0;
    transform: translateX(-50%);
}

.bag-main {
    right: 54px;
    bottom: 30px;
    width: 170px;
    height: 150px;
    border-radius: 6px;
}

.bag-side {
    right: 190px;
    bottom: 18px;
    width: 92px;
    height: 92px;
    border-radius: 5px;
    opacity: 0.9;
}

.box {
    position: absolute;
    border-radius: 5px;
    background: #f7f2e9;
    border: 1px solid #ece7de;
}

.box-one {
    right: 18px;
    bottom: 20px;
    width: 88px;
    height: 88px;
}

.box-two {
    right: 8px;
    bottom: 118px;
    width: 54px;
    height: 54px;
}

.service-mini {
    display: flex;
    align-items: center;
    gap: 22px;
    color: #4d5661;
    font-size: 12px;
    position: relative;
    z-index: 2;
}

.service-mini-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.service-dot {
    width: 24px;
    height: 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 999px;
    border: 1px solid #d9dee6;
    color: #20242a;
    font-weight: 800;
}

.signin-panel {
    padding: 44px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 18px;
}

.signin-head {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.signin-title {
    color: #111316;
    font-size: 30px;
    font-weight: 900;
}

.signin-copy,
.hint-copy {
    color: #69727d;
    font-size: 13px;
    line-height: 1.7;
}

.context-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.context-item,
.signin-hint {
    min-width: 0;
    padding: 14px;
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: #fbfcfd;
}

.signin-feedback {
    padding: 12px 14px;
    border: 1px solid rgba(217, 45, 32, 0.18);
    border-radius: 6px;
    background: rgba(255, 59, 48, 0.06);
    color: #b42318;
    font-size: 12px;
    line-height: 1.6;
}

.context-label {
    color: #8b939e;
    font-size: 11px;
    font-weight: 800;
}

.context-value {
    display: block;
    margin-top: 8px;
    color: #171a1f;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.5;
    overflow-wrap: anywhere;
}

.signin-hint {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.hint-title {
    color: #111316;
    font-size: 12px;
    font-weight: 900;
}

.primary-action,
.secondary-action,
.github-action {
    width: 100%;
    min-height: 46px;
    font-size: 13px;
}

.primary-action {
    border: 1px solid #111316;
    background: #111316;
    color: #ffffff;
}

.secondary-action {
    border: 1px solid #d9dee6;
    background: #ffffff;
    color: #1d2025;
}

.credential-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.field-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.field-label {
    color: #4f5965;
    font-size: 12px;
    font-weight: 800;
}

.field-input {
    width: 100%;
    min-height: 46px;
    padding: 0 14px;
    border: 1px solid #dfe4ea;
    border-radius: 4px;
    background: #fbfcfd;
    color: #15181d;
    font-size: 14px;
}

.field-input:focus {
    border-color: #111316;
    box-shadow: 0 0 0 3px rgba(17, 19, 22, 0.08);
}

.github-action {
    width: 100%;
    min-height: 46px;
    border: 1px solid #d9dee6;
    background: #ffffff;
    color: #1d2025;
    font-size: 13px;
}

.divider {
    display: flex;
    align-items: center;
    gap: 10px;
}

.divider-line {
    flex: 1;
    height: 1px;
    background: #edf0f4;
}

.divider-text {
    color: #9aa2ad;
    font-size: 11px;
    font-weight: 800;
}

button::after {
    border: none;
}

@media (max-width: 980px) {
    .login-card {
        grid-template-columns: 1fr;
    }

    .shopping-visual {
        position: relative;
        right: auto;
        bottom: auto;
        align-self: center;
        margin: 24px 0;
    }
}

@media (max-width: 640px) {
    .login-page {
        padding: 12px 0 32px;
    }

    .login-shell {
        width: min(100% - 20px, 720px);
        gap: 16px;
    }

    .login-header {
        align-items: flex-start;
        flex-direction: column;
        padding: 14px;
    }

    .header-right {
        width: 100%;
        justify-content: space-between;
    }

    .welcome-panel,
    .signin-panel {
        padding: 24px;
    }

    .welcome-title {
        font-size: 32px;
    }

    .context-grid,
    .service-mini {
        grid-template-columns: 1fr;
        flex-direction: column;
        align-items: flex-start;
    }
}
</style>
