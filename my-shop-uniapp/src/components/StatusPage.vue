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
}

.error-card {
    width: min(520px, 100%);
    padding: 28px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
}

.status-mark {
    width: 64px;
    height: 64px;
    border-radius: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--highlight), var(--accent));
    color: #07131f;
    box-shadow: 0 18px 34px rgba(1, 7, 14, 0.28);
}

.status-code {
    font-size: 18px;
    font-weight: 800;
}

.title {
    font-size: 30px;
    line-height: 1.12;
    font-weight: 800;
    color: var(--text-main);
}

.description,
.detail {
    font-size: 14px;
    line-height: 1.7;
    color: var(--text-muted);
}

.detail {
    width: 100%;
    padding: 12px 14px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--panel-border);
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
