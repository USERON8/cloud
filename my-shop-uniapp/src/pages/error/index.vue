<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { navigateTo, reLaunch } from "../../router/navigation";
import { Routes, type RoutePath } from "../../router/routes";

type ErrorKind = "network" | "timeout" | "server" | "not-found";

interface ErrorCopy {
    eyebrow: string;
    title: string;
    description: string;
    primaryAction: string;
    secondaryAction: string;
}

const kind = ref<ErrorKind>("network");
const status = ref("");
const message = ref("");
const redirect = ref<RoutePath | "">("");

const copyMap: Record<ErrorKind, ErrorCopy> = {
    network: {
        eyebrow: "Network issue",
        title: "The service cannot be reached right now.",
        description:
            "Check the connection or wait for the backend gateway to recover, then try again.",
        primaryAction: "Try again",
        secondaryAction: "Back to market",
    },
    timeout: {
        eyebrow: "Request timeout",
        title: "The request took too long to finish.",
        description:
            "The backend may be starting up or under load. Retry the current page after a moment.",
        primaryAction: "Retry",
        secondaryAction: "Back to market",
    },
    server: {
        eyebrow: "Server error",
        title: "The backend returned an unexpected error.",
        description:
            "This usually needs the service logs checked. You can retry or return to the storefront.",
        primaryAction: "Retry",
        secondaryAction: "Back to market",
    },
    "not-found": {
        eyebrow: "Not found",
        title: "The requested resource was not found.",
        description:
            "The link may be stale or the resource may have been removed.",
        primaryAction: "Retry",
        secondaryAction: "Back to market",
    },
};

const copy = computed(() => copyMap[kind.value]);
const detailText = computed(() => {
    const parts = [status.value ? `Status ${status.value}` : "", message.value]
        .map((part) => part.trim())
        .filter(Boolean);
    return parts.join(" · ");
});

function normalizeKind(value?: string): ErrorKind {
    if (
        value === "network" ||
        value === "timeout" ||
        value === "server" ||
        value === "not-found"
    ) {
        return value;
    }
    return "network";
}

function normalizeRoute(value?: string): RoutePath | "" {
    if (!value) {
        return "";
    }
    return Object.values(Routes).includes(value as RoutePath)
        ? (value as RoutePath)
        : "";
}

onLoad((query) => {
    kind.value = normalizeKind(String(query.kind || ""));
    status.value = String(query.status || "");
    message.value = String(query.message || "");
    redirect.value = normalizeRoute(String(query.redirect || ""));
});

function retry(): void {
    if (redirect.value) {
        reLaunch(redirect.value);
        return;
    }
    reLaunch(Routes.market);
}

function backToMarket(): void {
    navigateTo(Routes.market);
}
</script>

<template>
    <view class="page">
        <view class="error-card glass-card">
            <view class="status-mark">
                <text class="status-code">{{ status || "!" }}</text>
            </view>
            <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
            <text class="title">{{ copy.title }}</text>
            <text class="description">{{ copy.description }}</text>
            <text v-if="detailText" class="detail">{{ detailText }}</text>
            <view class="actions">
                <button class="btn-primary" @click="retry">
                    {{ copy.primaryAction }}
                </button>
                <button class="btn-outline" @click="backToMarket">
                    {{ copy.secondaryAction }}
                </button>
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

.actions button {
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

    .actions button {
        min-width: 100%;
    }
}
</style>
