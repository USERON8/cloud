<script setup lang="ts">
import { computed } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { sessionState } from "../../../auth/session";

const user = computed(() => sessionState.user);
</script>

<template>
    <AppShell title="Profile">
        <view class="profile-layout">
            <view class="hero-card dashboard-hero display-panel fade-in-up">
                <view class="hero-main">
                    <view class="avatar">{{
                        (user?.nickname || user?.username || "U")
                            .slice(0, 1)
                            .toUpperCase()
                    }}</view>
                    <view class="hero-copy">
                        <text class="eyebrow">Account center</text>
                        <text class="hero-title">{{
                            user?.nickname || user?.username || "Current User"
                        }}</text>
                        <text class="hero-subtitle">
                            Review your identity, account attributes, and role
                            assignments for this workspace.
                        </text>
                    </view>
                </view>

                <view class="hero-side dashboard-hero-stats">
                    <view class="info-card">
                        <text class="info-label">Primary role</text>
                        <text class="info-value">{{
                            (user?.roles || [])[0] || "USER"
                        }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Account status</text>
                        <text class="info-value">{{
                            user?.status === 1 ? "Active" : "Ready"
                        }}</text>
                    </view>
                </view>
            </view>

            <view class="details-grid dashboard-grid-2 fade-in-up">
                <view class="surface-card details-card panel-hover">
                    <text class="section-title">Profile details</text>
                    <view class="detail-list">
                        <view class="detail-row">
                            <text class="label">Username</text>
                            <text class="value">{{
                                user?.username || "--"
                            }}</text>
                        </view>
                        <view class="detail-row">
                            <text class="label">Display name</text>
                            <text class="value">{{
                                user?.nickname || "--"
                            }}</text>
                        </view>
                        <view class="detail-row">
                            <text class="label">Email</text>
                            <text class="value">{{ user?.email || "--" }}</text>
                        </view>
                        <view class="detail-row">
                            <text class="label">Phone</text>
                            <text class="value">{{ user?.phone || "--" }}</text>
                        </view>
                    </view>
                </view>

                <view class="surface-card details-card panel-hover">
                    <text class="section-title">Role assignments</text>
                    <view class="roles-wrap">
                        <text
                            v-for="role in user?.roles || ['USER']"
                            :key="role"
                            class="chip role-chip"
                            >{{ role }}</text
                        >
                    </view>

                    <view class="summary-box">
                        <text class="summary-title">Workspace summary</text>
                        <text class="summary-copy">
                            Your account is already connected to the unified
                            OAuth session, so the visible navigation and
                            available actions update automatically based on your
                            current role set.
                        </text>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.profile-layout {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.hero-main {
    display: flex;
    align-items: center;
    gap: 18px;
}

.avatar {
    width: 72px;
    height: 72px;
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--accent), var(--accent-strong));
    color: #fff;
    font-size: 28px;
    font-weight: 700;
    box-shadow: 0 14px 30px rgba(95, 209, 194, 0.24);
    flex-shrink: 0;
}

.hero-copy {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.eyebrow {
    font-size: 12px;
    color: var(--accent);
    font-weight: 800;
}

.hero-title {
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.03em;
}

.hero-subtitle {
    font-size: 14px;
    color: var(--text-muted);
    line-height: 1.7;
    max-width: 620px;
}

.details-card {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.detail-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.detail-row {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid var(--panel-border);
}

.detail-row:first-child {
    padding-top: 0;
}

.detail-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.label {
    color: var(--text-muted);
    font-size: 12px;
}

.value {
    font-size: 13px;
    font-weight: 700;
    text-align: right;
}

.roles-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.role-chip {
    background: var(--accent-soft);
    color: var(--accent-strong);
    border-color: rgba(95, 209, 194, 0.16);
}

.summary-box {
    padding: 16px;
    border-radius: var(--radius-md);
    background: rgba(95, 209, 194, 0.08);
    border: 1px solid rgba(95, 209, 194, 0.16);
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.summary-title {
    font-size: 13px;
    font-weight: 700;
}

.summary-copy {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.7;
}

@media (max-width: 900px) {
    .hero-main {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
