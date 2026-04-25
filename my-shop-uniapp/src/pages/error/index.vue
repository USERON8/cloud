<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import StatusPage from "../../components/StatusPage.vue";
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
    <StatusPage
        :code="status || '!'"
        :eyebrow="copy.eyebrow"
        :title="copy.title"
        :description="copy.description"
        :detail="detailText"
    >
        <template #actions>
            <button class="btn-primary" @click="retry">
                {{ copy.primaryAction }}
            </button>
            <button class="btn-outline" @click="backToMarket">
                {{ copy.secondaryAction }}
            </button>
        </template>
    </StatusPage>
</template>
