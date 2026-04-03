<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { register } from "../../api/auth";
import { navigateTo, redirectTo } from "../../router/navigation";
import { Routes } from "../../router/routes";
import { toast } from "../../utils/ui";

const entryType = ref("");
const submitting = ref(false);
const redirectPath = ref<string>(Routes.appHome);

const form = reactive({
    username: "",
    nickname: "",
    phone: "",
    password: "",
    confirmPassword: "",
});

const entryLabel = computed(() => {
    if (entryType.value === "admin") return "admin";
    if (entryType.value === "merchant") return "merchant";
    return "customer";
});

const titleText = computed(() => {
    if (entryLabel.value === "merchant") return "Create merchant-ready account";
    if (entryLabel.value === "admin") return "Create platform account";
    return "Create your shopping account";
});

const subtitleText = computed(() => {
    if (entryLabel.value === "merchant") {
        return "Register a basic account first, then sign in and complete merchant authentication materials.";
    }
    if (entryLabel.value === "admin") {
        return "Administrator accounts are usually provisioned by the platform. This page is kept available when manual bootstrap is needed.";
    }
    return "Create a customer account to start browsing, ordering, and managing your profile.";
});

onLoad((query) => {
    if (typeof query.entry === "string") {
        entryType.value = query.entry.toLowerCase();
    }
    if (typeof query.redirect === "string") {
        try {
            redirectPath.value = decodeURIComponent(query.redirect);
        } catch {
            redirectPath.value = query.redirect;
        }
    }
});

function normalizePhone(value: string): string {
    return value.replace(/\s+/g, "").trim();
}

function validateForm(): boolean {
    if (!form.username.trim()) {
        toast("Please enter username");
        return false;
    }
    if (!form.nickname.trim()) {
        toast("Please enter nickname");
        return false;
    }
    if (!normalizePhone(form.phone)) {
        toast("Please enter phone");
        return false;
    }
    if (!form.password) {
        toast("Please enter password");
        return false;
    }
    if (form.password.length < 6) {
        toast("Password must be at least 6 characters");
        return false;
    }
    if (form.password !== form.confirmPassword) {
        toast("Passwords do not match");
        return false;
    }
    return true;
}

async function submitRegister(): Promise<void> {
    if (!validateForm()) return;
    submitting.value = true;
    try {
        await register({
            username: form.username.trim(),
            nickname: form.nickname.trim(),
            phone: normalizePhone(form.phone),
            password: form.password,
        });
        toast("Registration successful", "success");
        redirectTo(
            `${Routes.login}?entry=${encodeURIComponent(entryType.value || "customer")}&redirect=${encodeURIComponent(redirectPath.value)}`,
        );
    } catch (error) {
        toast(error instanceof Error ? error.message : "Registration failed");
    } finally {
        submitting.value = false;
    }
}

function goToLogin(): void {
    navigateTo(
        `${Routes.login}?entry=${encodeURIComponent(entryType.value || "customer")}&redirect=${encodeURIComponent(redirectPath.value)}`,
    );
}

function backToMarket(): void {
    navigateTo(Routes.market);
}
</script>

<template>
    <view class="page">
        <view class="page-container auth-layout">
            <view class="hero-panel">
                <view class="hero-badge">My Shop Cloud</view>
                <text class="hero-title">{{ titleText }}</text>
                <text class="hero-copy">
                    {{ subtitleText }}
                </text>

                <view class="hero-points">
                    <view class="point surface-card">
                        <text class="point-label">Access mode</text>
                        <text class="point-value">{{
                            entryLabel === "admin"
                                ? "Administrator onboarding"
                                : entryLabel === "merchant"
                                  ? "Merchant onboarding"
                                  : "Customer onboarding"
                        }}</text>
                    </view>
                    <view class="point surface-card">
                        <text class="point-label">Next step</text>
                        <text class="point-value">{{
                            entryLabel === "merchant"
                                ? "Submit merchant verification after sign-in"
                                : entryLabel === "admin"
                                  ? "Use platform authorization after bootstrap"
                                  : "Sign in and start shopping"
                        }}</text>
                    </view>
                </view>
            </view>

            <view class="signin-card glass-card">
                <view class="header">
                    <text class="eyebrow">Account registration</text>
                    <text class="title">Register</text>
                    <text class="muted">
                        Create your account with basic profile information.
                        After registration, you will be redirected to the
                        sign-in page.
                    </text>
                </view>

                <view class="signin-content">
                    <input
                        v-model="form.username"
                        class="input"
                        placeholder="Username"
                    />
                    <input
                        v-model="form.nickname"
                        class="input"
                        placeholder="Nickname"
                    />
                    <input
                        v-model="form.phone"
                        class="input"
                        type="number"
                        placeholder="Phone number"
                    />
                    <input
                        v-model="form.password"
                        class="input"
                        password
                        placeholder="Password"
                    />
                    <input
                        v-model="form.confirmPassword"
                        class="input"
                        password
                        placeholder="Confirm password"
                    />

                    <button
                        class="btn-primary full-width action-button"
                        :loading="submitting"
                        @click="submitRegister"
                    >
                        Create account
                    </button>

                    <button
                        class="btn-outline full-width action-button"
                        @click="goToLogin"
                    >
                        Already have an account? Sign in
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
    grid-template-columns: minmax(0, 1.15fr) minmax(360px, 440px);
    gap: 18px;
    align-items: stretch;
}

.hero-panel {
    padding: 28px;
    border-radius: var(--radius-xl);
    background: linear-gradient(
        160deg,
        rgba(15, 23, 42, 0.94),
        rgba(37, 99, 235, 0.88)
    );
    color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 24px;
    box-shadow: 0 24px 56px rgba(15, 23, 42, 0.22);
}

.hero-badge {
    align-self: flex-start;
    padding: 8px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.14);
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
    max-width: 540px;
}

.hero-points {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.point {
    padding: 16px;
    background: rgba(255, 255, 255, 0.14);
    border-color: rgba(255, 255, 255, 0.12);
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

.input {
    width: 100%;
    min-height: 46px;
    padding: 0 14px;
    border-radius: 16px;
    border: 1px solid rgba(27, 44, 74, 0.08);
    background: rgba(255, 255, 255, 0.86);
    font-size: 14px;
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
