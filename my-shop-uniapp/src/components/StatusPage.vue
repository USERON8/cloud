<script setup lang="ts">
withDefaults(
    defineProps<{
        code?: string;
        eyebrow: string;
        title: string;
        description: string;
        detail?: string;
    }>(),
    {
        code: "!",
        detail: "",
    },
);
</script>

<template>
    <view class="page">
        <view class="error-card glass-card">
            <view class="status-mark">
                <text class="status-code">{{ code }}</text>
            </view>
            <text class="hero-eyebrow">{{ eyebrow }}</text>
            <text class="title">{{ title }}</text>
            <text class="description">{{ description }}</text>
            <text v-if="detail" class="detail">{{ detail }}</text>
            <view v-if="$slots.actions" class="actions">
                <slot name="actions" />
            </view>
        </view>
    </view>
</template>

<style scoped>
.page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px 16px;
    color: #15171a;
    background:
        linear-gradient(180deg, rgba(248, 249, 251, 0.96), #ffffff 44%),
        #ffffff;
}

.error-card {
    width: min(520px, 100%);
    padding: 28px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    border: 1px solid #edf0f4;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 18px 58px rgba(18, 24, 35, 0.05);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
}

.status-mark {
    width: 64px;
    height: 64px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #111316;
    color: #ffffff;
    box-shadow: none;
}

.status-code {
    font-size: 18px;
    font-weight: 800;
}

.title {
    font-size: 30px;
    line-height: 1.12;
    font-weight: 900;
    color: #101216;
}

.description,
.detail {
    font-size: 14px;
    line-height: 1.7;
    color: #69727d;
}

.detail {
    width: 100%;
    padding: 12px 14px;
    border-radius: 8px;
    background: #fbfcfd;
    border: 1px solid #edf0f4;
    overflow-wrap: anywhere;
}

.actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    width: 100%;
    padding-top: 4px;
}

.actions :deep(button) {
    flex: 1;
    min-width: 150px;
    border-radius: 4px;
}

.actions :deep(.btn-primary) {
    background: #111316;
    border: 1px solid #111316;
    color: #ffffff;
    box-shadow: none;
}

.actions :deep(.btn-outline),
.actions :deep(.btn-secondary) {
    background: #ffffff;
    border: 1px solid #d9dee6;
    color: #1d2025;
    box-shadow: none;
}

.hero-eyebrow {
    color: #69727d;
}

@media (max-width: 520px) {
    .error-card {
        padding: 22px;
    }

    .title {
        font-size: 24px;
    }

    .actions :deep(button) {
        min-width: 100%;
    }
}
</style>
