<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { patchSessionUser, sessionState } from "../../../auth/session";
import {
    changeCurrentPassword,
    getCurrentProfile,
    updateCurrentProfile,
    uploadCurrentAvatarByPath,
} from "../../../api/user";
import type { UserProfileUpdatePayload } from "../../../types/domain";
import { toast } from "../../../utils/ui";

const loading = ref(false);
const savingProfile = ref(false);
const savingPassword = ref(false);
const uploadingAvatar = ref(false);

const profileForm = reactive<UserProfileUpdatePayload>({
    nickname: "",
    email: "",
    phone: "",
});

const passwordForm = reactive({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
});

const user = computed(() => sessionState.user);
const primaryRole = computed(() => (user.value?.roles || [])[0] || "USER");
const displayName = computed(
    () => user.value?.nickname || user.value?.username || "Current User",
);
const avatarLabel = computed(() =>
    displayName.value.slice(0, 1).toUpperCase() || "U",
);

function syncProfileForm(): void {
    profileForm.nickname = user.value?.nickname || "";
    profileForm.email = user.value?.email || "";
    profileForm.phone = user.value?.phone || "";
}

async function loadProfile(showError = true): Promise<void> {
    if (loading.value) {
        return;
    }
    loading.value = true;
    try {
        const profile = await getCurrentProfile();
        patchSessionUser(profile);
        syncProfileForm();
    } catch (error) {
        if (showError) {
            toast(
                error instanceof Error
                    ? error.message
                    : "Failed to load profile",
            );
        }
    } finally {
        loading.value = false;
    }
}

function buildProfilePayload(): UserProfileUpdatePayload | null {
    const payload: UserProfileUpdatePayload = {};
    const nickname = profileForm.nickname?.trim();
    const email = profileForm.email?.trim();
    const phone = profileForm.phone?.trim();

    if (nickname && nickname !== (user.value?.nickname || "")) {
        payload.nickname = nickname;
    }
    if (email && email !== (user.value?.email || "")) {
        payload.email = email;
    }
    if (phone && phone !== (user.value?.phone || "")) {
        payload.phone = phone;
    }

    return Object.keys(payload).length > 0 ? payload : null;
}

async function saveProfile(): Promise<void> {
    if (savingProfile.value) {
        return;
    }
    const payload = buildProfilePayload();
    if (!payload) {
        toast("No profile changes detected");
        return;
    }

    savingProfile.value = true;
    try {
        await updateCurrentProfile(payload);
        patchSessionUser(payload);
        toast("Profile updated", "success");
        await loadProfile(false);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Profile update failed");
    } finally {
        savingProfile.value = false;
    }
}

async function updateAvatar(): Promise<void> {
    if (uploadingAvatar.value) {
        return;
    }
    uploadingAvatar.value = true;
    try {
        const chooseResult = await uni.chooseImage({
            count: 1,
            sizeType: ["compressed"],
            sourceType: ["album", "camera"],
        });
        const filePath = chooseResult.tempFilePaths?.[0];
        if (!filePath) {
            toast("No image selected");
            return;
        }

        const avatarUrl = await uploadCurrentAvatarByPath(filePath);
        patchSessionUser({ avatarUrl });
        toast("Avatar updated", "success");
        await loadProfile(false);
    } catch (error) {
        toast(error instanceof Error ? error.message : "Avatar upload failed");
    } finally {
        uploadingAvatar.value = false;
    }
}

async function savePassword(): Promise<void> {
    if (savingPassword.value) {
        return;
    }
    if (!passwordForm.oldPassword.trim() || !passwordForm.newPassword.trim()) {
        toast("Both password fields are required");
        return;
    }
    if (passwordForm.newPassword.trim().length < 6) {
        toast("New password must be at least 6 characters");
        return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        toast("Password confirmation does not match");
        return;
    }

    savingPassword.value = true;
    try {
        await changeCurrentPassword({
            oldPassword: passwordForm.oldPassword,
            newPassword: passwordForm.newPassword,
        });
        passwordForm.oldPassword = "";
        passwordForm.newPassword = "";
        passwordForm.confirmPassword = "";
        toast("Password updated", "success");
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Password update failed",
        );
    } finally {
        savingPassword.value = false;
    }
}

onMounted(() => {
    syncProfileForm();
    void loadProfile(false);
});
</script>

<template>
    <AppShell title="Profile">
        <view class="profile-layout">
            <view class="hero-card dashboard-hero display-panel fade-in-up">
                <view class="hero-main">
                    <image
                        v-if="user?.avatarUrl"
                        :src="user.avatarUrl"
                        class="avatar-image"
                        mode="aspectFill"
                    />
                    <view v-else class="avatar">{{ avatarLabel }}</view>

                    <view class="hero-copy">
                        <text class="eyebrow">Account center</text>
                        <text class="hero-title">{{ displayName }}</text>
                        <text class="hero-subtitle">
                            Review and update your account profile, avatar, and
                            password with the current backend profile APIs.
                        </text>
                        <view class="action-wrap">
                            <button
                                class="btn-primary"
                                :loading="uploadingAvatar"
                                @click="updateAvatar"
                            >
                                Update avatar
                            </button>
                            <button
                                class="btn-outline"
                                :loading="loading"
                                @click="loadProfile"
                            >
                                Refresh profile
                            </button>
                        </view>
                    </view>
                </view>

                <view class="hero-side dashboard-hero-stats">
                    <view class="info-card">
                        <text class="info-label">Primary role</text>
                        <text class="info-value">{{ primaryRole }}</text>
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
                        <text class="summary-title">Profile source</text>
                        <text class="summary-copy">
                            This page now refreshes data from
                            <text class="inline-code"
                                >/api/user/profile/current</text
                            >
                            instead of relying on JWT claims only.
                        </text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-2 fade-in-up">
                <view class="surface-card details-card panel-hover">
                    <text class="section-title">Update profile</text>
                    <view class="form-grid">
                        <view class="field">
                            <text class="label">Display name</text>
                            <input
                                v-model="profileForm.nickname"
                                class="field-control"
                                placeholder="Nickname"
                            />
                        </view>
                        <view class="field">
                            <text class="label">Email</text>
                            <input
                                v-model="profileForm.email"
                                class="field-control"
                                placeholder="Email"
                            />
                        </view>
                        <view class="field field-span">
                            <text class="label">Phone</text>
                            <input
                                v-model="profileForm.phone"
                                class="field-control"
                                placeholder="Phone"
                            />
                        </view>
                    </view>
                    <button
                        class="btn-primary submit-button"
                        :loading="savingProfile"
                        @click="saveProfile"
                    >
                        Save profile
                    </button>
                </view>

                <view class="surface-card details-card panel-hover">
                    <text class="section-title">Change password</text>
                    <view class="form-grid">
                        <view class="field field-span">
                            <text class="label">Current password</text>
                            <input
                                v-model="passwordForm.oldPassword"
                                class="field-control"
                                password
                                placeholder="Current password"
                            />
                        </view>
                        <view class="field">
                            <text class="label">New password</text>
                            <input
                                v-model="passwordForm.newPassword"
                                class="field-control"
                                password
                                placeholder="New password"
                            />
                        </view>
                        <view class="field">
                            <text class="label">Confirm password</text>
                            <input
                                v-model="passwordForm.confirmPassword"
                                class="field-control"
                                password
                                placeholder="Confirm password"
                            />
                        </view>
                    </view>
                    <button
                        class="btn-primary submit-button"
                        :loading="savingPassword"
                        @click="savePassword"
                    >
                        Update password
                    </button>
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

.avatar,
.avatar-image {
    width: 72px;
    height: 72px;
    border-radius: 24px;
    flex-shrink: 0;
}

.avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--accent), var(--accent-strong));
    color: #fff;
    font-size: 28px;
    font-weight: 700;
    box-shadow: 0 14px 30px rgba(95, 209, 194, 0.24);
}

.avatar-image {
    border: 1px solid var(--panel-border);
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

.hero-subtitle,
.summary-copy {
    font-size: 14px;
    color: var(--text-muted);
    line-height: 1.7;
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

.inline-code {
    font-family: "JetBrains Mono", monospace;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.field-span {
    grid-column: span 2;
}

.submit-button {
    width: 100%;
}

@media (max-width: 900px) {
    .hero-main {
        align-items: flex-start;
        flex-direction: column;
    }

    .form-grid {
        grid-template-columns: 1fr;
    }

    .field-span {
        grid-column: span 1;
    }
}
</style>
