<script setup lang="ts">
import { computed, ref } from "vue";
import { onHide, onLoad, onShow, onUnload } from "@dcloudio/uni-app";
import { useTimeoutPoll } from "@vueuse/core";
import AppShell from "../../../components/AppShell.vue";
import { resolveApiUrl } from "../../../api/http";
import {
    createPaymentCheckoutSession,
    getPaymentOrderByNo,
    getPaymentStatus,
    getRefundByNo,
} from "../../../api/payment";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import type {
    PaymentOrderInfo,
    PaymentRefundInfo,
    PaymentStatusInfo,
} from "../../../types/domain";
import {
    formatDate,
    formatPrice,
    formatRelativeDate,
} from "../../../utils/format";
import { toast } from "../../../utils/ui";

const paymentNo = ref("");
const refundNo = ref("");
const paymentInfo = ref<PaymentOrderInfo | null>(null);
const refundInfo = ref<PaymentRefundInfo | null>(null);
const checkoutLoading = ref(false);
const pollAttempts = ref(0);
const isPolling = ref(false);

const FINAL_PAYMENT_STATUSES = new Set(["PAID", "FAILED"]);
const MAX_POLL_ATTEMPTS = 15;
const POLL_INTERVAL_MS = 2000;

function canOpenCheckout(): boolean {
    return (
        paymentInfo.value?.status === "CREATED" &&
        !!paymentInfo.value?.paymentNo
    );
}

const paymentStatusHint = computed(() => {
    if (isPolling.value) {
        return "Checking the latest payment status...";
    }
    if (paymentInfo.value?.status === "CREATED") {
        return "Payment is still pending.";
    }
    return "";
});

function openCheckout(url: string): void {
    navigateTo(
        Routes.webview,
        { url, paymentNo: paymentNo.value.trim() },
        {
            requiresAuth: true,
            roles: ["USER", "MERCHANT", "ADMIN"],
        },
    );
}

function shouldKeepPolling(status?: string): boolean {
    return !!status && !FINAL_PAYMENT_STATUSES.has(status);
}

function applyPaymentStatus(statusPayload: PaymentStatusInfo | null): void {
    if (!statusPayload?.status) {
        return;
    }
    paymentInfo.value = {
        ...(paymentInfo.value ?? {}),
        paymentNo:
            statusPayload.paymentNo ??
            paymentInfo.value?.paymentNo ??
            paymentNo.value.trim(),
        status: statusPayload.status,
    };
}

async function queryPayment(showError = true): Promise<void> {
    const targetPaymentNo = paymentNo.value.trim();
    if (!targetPaymentNo) {
        if (showError) {
            toast("Enter a payment number");
        }
        return;
    }
    try {
        paymentInfo.value = await getPaymentOrderByNo(targetPaymentNo);
    } catch (error) {
        if (showError) {
            toast(
                error instanceof Error
                    ? error.message
                    : "Failed to query the payment order",
            );
        }
    }
}

async function pollPaymentStatus(): Promise<void> {
    const targetPaymentNo = paymentNo.value.trim();
    if (!targetPaymentNo || pollAttempts.value >= MAX_POLL_ATTEMPTS) {
        isPolling.value = false;
        paymentPoller.pause();
        return;
    }
    isPolling.value = true;
    pollAttempts.value += 1;
    try {
        const statusPayload = await getPaymentStatus(targetPaymentNo);
        applyPaymentStatus(statusPayload);
        if (!shouldKeepPolling(statusPayload.status)) {
            isPolling.value = false;
            paymentPoller.pause();
            await queryPayment(false);
        }
    } catch (error) {
        isPolling.value = false;
        paymentPoller.pause();
        toast(
            error instanceof Error
                ? error.message
                : "Failed to refresh payment status",
        );
    }
}

const paymentPoller = useTimeoutPoll(
    () => {
        void pollPaymentStatus();
    },
    POLL_INTERVAL_MS,
    { immediate: false },
);

async function startPaymentPolling(): Promise<void> {
    paymentPoller.pause();
    pollAttempts.value = 0;
    isPolling.value = false;
    await queryPayment(false);
    if (!shouldKeepPolling(paymentInfo.value?.status)) {
        return;
    }
    paymentPoller.resume();
}

async function queryRefund(): Promise<void> {
    if (!refundNo.value.trim()) {
        toast("Enter a refund number");
        return;
    }
    try {
        refundInfo.value = await getRefundByNo(refundNo.value.trim());
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to query the refund order",
        );
    }
}

async function openPaymentCheckout(): Promise<void> {
    const targetPaymentNo =
        paymentInfo.value?.paymentNo || paymentNo.value.trim();
    if (!targetPaymentNo) {
        toast("Enter a payment number");
        return;
    }
    checkoutLoading.value = true;
    try {
        const session = await createPaymentCheckoutSession(targetPaymentNo);
        if (!session.checkoutPath) {
            throw new Error("Checkout session is missing checkoutPath");
        }
        openCheckout(resolveApiUrl(session.checkoutPath));
        void startPaymentPolling();
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to open checkout",
        );
    } finally {
        checkoutLoading.value = false;
    }
}

onLoad((query) => {
    const queryPaymentNo =
        typeof query.paymentNo === "string" && query.paymentNo.trim()
            ? query.paymentNo.trim()
            : typeof query.out_trade_no === "string" &&
                query.out_trade_no.trim()
              ? query.out_trade_no.trim()
              : "";
    const shouldAutoPoll =
        query.autoPoll === "1" || query.payment_return === "1";

    if (queryPaymentNo) {
        paymentNo.value = queryPaymentNo;
        if (shouldAutoPoll) {
            void startPaymentPolling();
        } else {
            void queryPayment(false);
        }
    }
    if (typeof query.refundNo === "string" && query.refundNo.trim()) {
        refundNo.value = query.refundNo.trim();
        void queryRefund();
    }
});

onShow(() => {
    if (
        paymentNo.value.trim() &&
        shouldKeepPolling(paymentInfo.value?.status)
    ) {
        void startPaymentPolling();
    }
});

onHide(() => {
    paymentPoller.pause();
    isPolling.value = false;
});

onUnload(() => {
    paymentPoller.pause();
});
</script>

<template>
    <AppShell title="Payments">
        <view class="payments-layout">
            <view class="hero-card dashboard-hero display-panel fade-in-up">
                <view class="hero-copy dashboard-hero-copy">
                    <text class="hero-eyebrow">Payments</text>
                    <text class="hero-title"
                        >Follow payment and refund progress from one cleaner
                        workspace.</text
                    >
                    <text class="hero-subtitle">
                        Query payment numbers, reopen checkout when required,
                        and monitor refund completion without leaving the app.
                    </text>
                </view>

                <view class="hero-stats dashboard-hero-stats">
                    <view class="info-card">
                        <text class="info-label">Polling state</text>
                        <text class="info-value">{{
                            isPolling ? "Running" : "Idle"
                        }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Attempts</text>
                        <text class="info-value"
                            >{{ pollAttempts }} / {{ MAX_POLL_ATTEMPTS }}</text
                        >
                    </view>
                </view>
            </view>

            <view class="content-grid dashboard-grid-2">
                <view class="surface-card panel panel-block panel-hover fade-in-up">
                    <view class="section-block compact-block">
                        <text class="section-title">Payment lookup</text>
                        <text class="section-subtitle"
                            >Search a payment record and continue checkout if it
                            is still awaiting completion.</text
                        >
                    </view>

                    <view class="search-row">
                        <input
                            v-model="paymentNo"
                            class="search-input field-control field-control-pill"
                            placeholder="Payment number"
                        />
                        <button class="btn-primary" @click="queryPayment">
                            Search
                        </button>
                    </view>

                    <view v-if="paymentInfo" class="result-card surface-muted panel-hover">
                        <view class="result-head">
                            <view>
                                <text class="name"
                                    >Payment {{ paymentInfo.paymentNo }}</text
                                >
                                <text class="meta"
                                    >Main order
                                    {{ paymentInfo.mainOrderNo || "--" }}</text
                                >
                            </view>
                            <text
                                class="status-chip"
                                :class="`status-${(paymentInfo.status || 'unknown').toLowerCase()}`"
                            >
                                {{ paymentInfo.status || "--" }}
                            </text>
                        </view>

                        <view class="metric-grid">
                            <view class="metric-item metric-card">
                                <text class="metric-label">Amount</text>
                                <text class="metric-value">{{
                                    formatPrice(paymentInfo.amount)
                                }}</text>
                            </view>
                            <view class="metric-item metric-card">
                                <text class="metric-label">Channel</text>
                                <text class="metric-value">{{
                                    paymentInfo.channel || "--"
                                }}</text>
                            </view>
                        </view>

                        <view class="meta-list meta-stack">
                            <text class="meta"
                                >Sub order:
                                {{ paymentInfo.subOrderNo || "--" }}</text
                            >
                            <text class="meta"
                                >Paid at:
                                {{ formatDate(paymentInfo.paidAt) }}</text
                            >
                            <text v-if="paymentInfo.paidAt" class="meta"
                                >Paid:
                                {{
                                    formatRelativeDate(paymentInfo.paidAt)
                                }}</text
                            >
                            <text
                                v-if="paymentStatusHint"
                                class="meta status-hint"
                                >{{ paymentStatusHint }}</text
                            >
                        </view>

                        <view class="actions action-wrap">
                            <button
                                v-if="paymentInfo.status === 'CREATED'"
                                class="btn-outline action-button"
                                :loading="isPolling"
                                @click="startPaymentPolling"
                            >
                                {{
                                    isPolling
                                        ? "Checking status..."
                                        : "Refresh status"
                                }}
                            </button>
                            <button
                                v-if="canOpenCheckout()"
                                class="btn-primary action-button"
                                :loading="checkoutLoading"
                                @click="openPaymentCheckout"
                            >
                                Open checkout
                            </button>
                        </view>
                    </view>

                    <view v-else class="empty-state"
                        >No payment record selected yet.</view
                    >
                </view>

                <view class="surface-card panel panel-block panel-hover fade-in-up">
                    <view class="section-block compact-block">
                        <text class="section-title">Refund lookup</text>
                        <text class="section-subtitle"
                            >Review refund records linked to payment and
                            after-sale workflows.</text
                        >
                    </view>

                    <view class="search-row">
                        <input
                            v-model="refundNo"
                            class="search-input field-control field-control-pill"
                            placeholder="Refund number"
                        />
                        <button class="btn-primary" @click="queryRefund">
                            Search
                        </button>
                    </view>

                    <view v-if="refundInfo" class="result-card surface-muted panel-hover">
                        <view class="result-head">
                            <view>
                                <text class="name"
                                    >Refund {{ refundInfo.refundNo }}</text
                                >
                                <text class="meta"
                                    >Payment
                                    {{ refundInfo.paymentNo || "--" }}</text
                                >
                            </view>
                            <text
                                class="status-chip"
                                :class="`status-${(refundInfo.status || 'unknown').toLowerCase()}`"
                            >
                                {{ refundInfo.status || "--" }}
                            </text>
                        </view>

                        <view class="metric-grid">
                            <view class="metric-item metric-card">
                                <text class="metric-label">Amount</text>
                                <text class="metric-value">{{
                                    formatPrice(refundInfo.refundAmount)
                                }}</text>
                            </view>
                            <view class="metric-item metric-card">
                                <text class="metric-label">After-sale</text>
                                <text class="metric-value">{{
                                    refundInfo.afterSaleNo || "--"
                                }}</text>
                            </view>
                        </view>

                        <view class="meta-list meta-stack">
                            <text class="meta"
                                >Refunded at:
                                {{ formatDate(refundInfo.refundedAt) }}</text
                            >
                            <text v-if="refundInfo.refundedAt" class="meta"
                                >Refunded:
                                {{
                                    formatRelativeDate(refundInfo.refundedAt)
                                }}</text
                            >
                        </view>
                    </view>

                    <view v-else class="empty-state"
                        >No refund record selected yet.</view
                    >
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.payments-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.compact-block {
    gap: 6px;
}

.search-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.search-input {
    flex: 1;
}

.search-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.result-card {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 18px;
}

.result-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
}

.name {
    display: block;
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 6px;
    letter-spacing: -0.02em;
}

.status-chip {
    padding: 8px 13px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    background: rgba(20, 20, 20, 0.08);
    color: var(--text-main);
    white-space: nowrap;
}

.status-created {
    background: var(--highlight-soft);
    color: #8a6a1d;
}

.status-paid,
.status-success {
    background: rgba(52, 199, 89, 0.14);
    color: #167c3a;
}

.status-failed,
.status-closed {
    background: rgba(255, 69, 58, 0.12);
    color: #b42318;
}

.status-refunding {
    background: rgba(11, 107, 95, 0.16);
    color: var(--accent);
}

.status-refunded {
    background: rgba(15, 118, 110, 0.18);
    color: #0b5d54;
}

.metric-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.metric-label {
    font-size: 12px;
    color: var(--text-muted);
    letter-spacing: 0.01em;
}

.metric-value {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.meta {
    font-size: 13px;
    color: var(--text-muted);
    word-break: break-all;
    line-height: 1.7;
}

.status-hint {
    color: var(--accent);
}

.action-button {
    flex: 1 1 180px;
}

.empty-state {
    padding: 28px 0;
    text-align: center;
    color: var(--text-muted);
    font-size: 13px;
}

@media (max-width: 900px) {
    .search-row {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
