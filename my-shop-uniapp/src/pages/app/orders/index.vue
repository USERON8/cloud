<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { reactive, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import {
    advanceAfterSaleStatus,
    applyAfterSale,
    cancelOrder,
    completeOrder,
    listOrders,
} from "../../../api/order";
import { resolveApiUrl } from "../../../api/http";
import {
    createPaymentCheckoutSession,
    createPaymentOrder,
    getPaymentOrderByOrderNo,
} from "../../../api/payment";
import type { AfterSaleInfo, OrderItem } from "../../../types/domain";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import {
    formatDate,
    formatOrderStatus,
    formatPrice,
    formatRelativeDate,
} from "../../../utils/format";
import { confirm, toast } from "../../../utils/ui";

const rows = ref<OrderItem[]>([]);
const loading = ref(false);
const refundingOrderId = ref<number | null>(null);
const payingOrderId = ref<number | null>(null);
const completingOrderId = ref<number | null>(null);

const afterSaleDraft = reactive({
    orderId: null as number | null,
    afterSaleType: "REFUND",
    reason: "",
    description: "",
    applyAmount: "",
});

function canApplyAfterSale(order: OrderItem): boolean {
    return (
        typeof order.id === "number" &&
        typeof order.subOrderId === "number" &&
        typeof order.merchantId === "number" &&
        [1, 2, 3].includes(order.status ?? -1) &&
        (!order.afterSaleStatus || order.afterSaleStatus === "NONE")
    );
}

function canCancelAfterSale(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "APPLIED"
    );
}

function canPay(order: OrderItem): boolean {
    return (
        order.status === 0 &&
        typeof order.userId === "number" &&
        !!order.orderNo &&
        !!order.subOrderNo
    );
}

function canComplete(order: OrderItem): boolean {
    return typeof order.id === "number" && order.status === 2;
}

function canViewRefund(order: OrderItem): boolean {
    return (
        !!order.refundNo &&
        ["REFUNDING", "REFUNDED"].includes(order.afterSaleStatus ?? "")
    );
}

function buildPaymentIdempotencyKey(order: OrderItem): string {
    return `payment:${order.orderNo}:${order.subOrderNo ?? order.id}`;
}

function buildPaymentNo(order: OrderItem): string {
    const subOrderNo =
        order.subOrderNo?.replace(/[^A-Za-z0-9_-]/g, "") || String(order.id);
    return `PAY-${subOrderNo}`;
}

function openCheckout(url: string, paymentNo: string): void {
    navigateTo(
        Routes.webview,
        { url, paymentNo },
        {
            requiresAuth: true,
            roles: ["USER", "MERCHANT", "ADMIN"],
        },
    );
}

function resetAfterSaleDraft(): void {
    afterSaleDraft.orderId = null;
    afterSaleDraft.afterSaleType = "REFUND";
    afterSaleDraft.reason = "";
    afterSaleDraft.description = "";
    afterSaleDraft.applyAmount = "";
}

function openAfterSale(order: OrderItem): void {
    if (!canApplyAfterSale(order)) {
        toast("This order is not eligible for a new after-sale request");
        return;
    }
    afterSaleDraft.orderId = order.id;
    afterSaleDraft.afterSaleType = "REFUND";
    afterSaleDraft.reason = "";
    afterSaleDraft.description = "";
    afterSaleDraft.applyAmount = String(
        order.payAmount ?? order.totalAmount ?? "",
    );
}

function selectedOrder(): OrderItem | null {
    return (
        rows.value.find((item) => item.id === afterSaleDraft.orderId) ?? null
    );
}

function buildAfterSalePayload(order: OrderItem): AfterSaleInfo | null {
    if (
        typeof order.id !== "number" ||
        typeof order.subOrderId !== "number" ||
        typeof order.merchantId !== "number"
    ) {
        toast("The order is missing after-sale metadata");
        return null;
    }
    const amount = Number(afterSaleDraft.applyAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
        toast("Apply amount must be greater than 0");
        return null;
    }
    if (!afterSaleDraft.reason.trim()) {
        toast("Reason is required");
        return null;
    }

    return {
        mainOrderId: order.id,
        subOrderId: order.subOrderId,
        merchantId: order.merchantId,
        afterSaleType: afterSaleDraft.afterSaleType,
        reason: afterSaleDraft.reason.trim(),
        description: afterSaleDraft.description.trim() || undefined,
        applyAmount: Number(amount.toFixed(2)),
    };
}

async function loadOrders(): Promise<void> {
    if (loading.value) return;
    loading.value = true;
    try {
        const result = await listOrders({ page: 1, size: 30 });
        rows.value = result.records;
    } catch (error) {
        toast(error instanceof Error ? error.message : "Failed to load orders");
    } finally {
        loading.value = false;
    }
}

async function onPay(order: OrderItem): Promise<void> {
    if (
        !canPay(order) ||
        typeof order.userId !== "number" ||
        !order.subOrderNo
    ) {
        toast("This order is missing payment metadata");
        return;
    }
    const amount = Number(order.payAmount ?? order.totalAmount ?? NaN);
    if (!Number.isFinite(amount) || amount <= 0) {
        toast("This order does not have a valid payable amount");
        return;
    }

    payingOrderId.value = order.id;
    let paymentNo = "";
    try {
        const existingOrder = await getPaymentOrderByOrderNo(
            order.orderNo,
            order.subOrderNo,
        );
        paymentNo = existingOrder?.paymentNo ?? buildPaymentNo(order);
        if (!existingOrder) {
            await createPaymentOrder({
                paymentNo,
                mainOrderNo: order.orderNo,
                subOrderNo: order.subOrderNo,
                userId: order.userId,
                amount: Number(amount.toFixed(2)),
                channel: "ALIPAY",
                idempotencyKey: buildPaymentIdempotencyKey(order),
            });
        }
        const session = await createPaymentCheckoutSession(paymentNo);
        if (!session.checkoutPath) {
            throw new Error("Checkout session is missing checkoutPath");
        }
        openCheckout(resolveApiUrl(session.checkoutPath), paymentNo);
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to prepare payment",
        );
        if (paymentNo) {
            navigateTo(
                Routes.appPayments,
                { paymentNo, autoPoll: 1 },
                {
                    requiresAuth: true,
                    roles: ["USER", "MERCHANT", "ADMIN"],
                },
            );
        }
    } finally {
        payingOrderId.value = null;
    }
}

async function onCancel(order: OrderItem): Promise<void> {
    if (typeof order.id !== "number") return;
    const ok = await confirm(`Cancel order ${order.orderNo}?`);
    if (!ok) return;
    try {
        await cancelOrder(order.id);
        toast("Order cancelled", "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to cancel order",
        );
    }
}

async function onComplete(order: OrderItem): Promise<void> {
    if (!canComplete(order) || typeof order.id !== "number") {
        toast("This order cannot be completed");
        return;
    }
    const ok = await confirm(`Confirm receipt for order ${order.orderNo}?`);
    if (!ok) {
        return;
    }
    completingOrderId.value = order.id;
    try {
        await completeOrder(order.id);
        toast("Order completed", "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to complete the order",
        );
    } finally {
        completingOrderId.value = null;
    }
}

function onViewRefund(order: OrderItem): void {
    if (!canViewRefund(order)) {
        toast("Refund tracking is not available for this order");
        return;
    }
    navigateTo(
        Routes.appPayments,
        { refundNo: order.refundNo },
        {
            requiresAuth: true,
            roles: ["USER", "MERCHANT", "ADMIN"],
        },
    );
}

async function submitAfterSale(): Promise<void> {
    const order = selectedOrder();
    if (!order || typeof order.id !== "number") {
        toast("Select an order first");
        return;
    }
    const payload = buildAfterSalePayload(order);
    if (!payload) {
        return;
    }

    refundingOrderId.value = order.id;
    try {
        const result = await applyAfterSale(payload);
        toast(
            `After-sale request created: ${result.afterSaleNo ?? "pending number"}`,
            "success",
        );
        resetAfterSaleDraft();
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to create the after-sale request",
        );
    } finally {
        refundingOrderId.value = null;
    }
}

async function cancelAfterSale(order: OrderItem): Promise<void> {
    if (!canCancelAfterSale(order) || typeof order.afterSaleId !== "number") {
        toast("This after-sale request cannot be cancelled");
        return;
    }
    const ok = await confirm(
        `Cancel after-sale request ${order.afterSaleNo ?? order.afterSaleId}?`,
    );
    if (!ok) {
        return;
    }
    refundingOrderId.value = order.id;
    try {
        await advanceAfterSaleStatus(order.afterSaleId, "CANCEL");
        toast("After-sale request cancelled", "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to cancel the after-sale request",
        );
    } finally {
        refundingOrderId.value = null;
    }
}

onShow(() => {
    void loadOrders();
});
</script>

<template>
    <AppShell title="Orders">
        <view class="orders-layout">
            <view class="hero-card display-panel fade-in-up">
                <view class="hero-copy">
                    <text class="hero-eyebrow">Orders</text>
                    <text class="hero-title"
                        >Track payments, delivery progress, and after-sale
                        actions with more clarity.</text
                    >
                    <text class="hero-subtitle">
                        Review order history, recover pending payments, confirm
                        receipt, and open refund workflows from a cleaner,
                        calmer workspace.
                    </text>
                </view>

                <view class="hero-stats">
                    <view class="info-card">
                        <text class="info-label">Total orders</text>
                        <text class="info-value">{{ rows.length }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Pending payment</text>
                        <text class="info-value">{{
                            rows.filter((item) => item.status === 0).length
                        }}</text>
                    </view>
                </view>
            </view>

            <view class="surface-card panel fade-in-up">
                <view class="header">
                    <view class="section-block compact-block">
                        <text class="section-title">Order history</text>
                        <text class="section-subtitle"
                            >Recent activity and actions available for each
                            order.</text
                        >
                    </view>
                    <button class="btn-outline" @click="loadOrders">
                        Refresh
                    </button>
                </view>

                <view v-if="rows.length === 0" class="empty-state">
                    No orders yet
                </view>

                <view v-else class="order-grid">
                    <view
                        v-for="item in rows"
                        :key="item.id"
                        class="order-card surface-card"
                    >
                        <view class="order-head">
                            <view class="order-main">
                                <text class="order-name"
                                    >Order {{ item.orderNo }}</text
                                >
                                <text class="order-sub"
                                    >Created
                                    {{ formatDate(item.createdAt) }}</text
                                >
                            </view>
                            <text
                                class="status-chip"
                                :class="`status-${item.status ?? 'unknown'}`"
                            >
                                {{ formatOrderStatus(item.status) }}
                            </text>
                        </view>

                        <view class="order-metrics">
                            <view class="metric-item">
                                <text class="metric-label">Amount</text>
                                <text class="metric-value">{{
                                    formatPrice(
                                        item.payAmount ?? item.totalAmount,
                                    )
                                }}</text>
                            </view>
                            <view class="metric-item">
                                <text class="metric-label">Age</text>
                                <text class="metric-value">{{
                                    formatRelativeDate(item.createdAt)
                                }}</text>
                            </view>
                        </view>

                        <view class="meta-list">
                            <text class="meta" v-if="item.subOrderNo"
                                >Sub-order: {{ item.subOrderNo }}</text
                            >
                            <text class="meta" v-if="item.refundNo"
                                >Refund no: {{ item.refundNo }}</text
                            >
                            <text
                                v-if="
                                    item.afterSaleStatus &&
                                    item.afterSaleStatus !== 'NONE'
                                "
                                class="meta"
                            >
                                After-sale: {{ item.afterSaleStatus
                                }}{{
                                    item.afterSaleNo
                                        ? ` (${item.afterSaleNo})`
                                        : ""
                                }}
                            </text>
                        </view>

                        <view class="actions">
                            <button
                                v-if="item.status === 0"
                                class="btn-primary action-button"
                                :loading="payingOrderId === item.id"
                                @click="onPay(item)"
                            >
                                {{
                                    payingOrderId === item.id
                                        ? "Opening checkout..."
                                        : "Pay now"
                                }}
                            </button>
                            <button
                                v-if="canComplete(item)"
                                class="btn-outline action-button"
                                :loading="completingOrderId === item.id"
                                @click="onComplete(item)"
                            >
                                Confirm receipt
                            </button>
                            <button
                                v-if="canViewRefund(item)"
                                class="btn-outline action-button"
                                @click="onViewRefund(item)"
                            >
                                View refund
                            </button>
                            <button
                                v-if="canApplyAfterSale(item)"
                                class="btn-outline action-button"
                                @click="openAfterSale(item)"
                            >
                                Apply after-sale
                            </button>
                            <button
                                v-if="canCancelAfterSale(item)"
                                class="btn-outline action-button"
                                @click="cancelAfterSale(item)"
                            >
                                Cancel after-sale
                            </button>
                            <button
                                class="btn-secondary action-button"
                                @click="onCancel(item)"
                            >
                                Cancel order
                            </button>
                        </view>
                    </view>
                </view>
            </view>

            <view
                v-if="afterSaleDraft.orderId"
                class="surface-card aftersale-panel fade-in-up"
            >
                <view class="header">
                    <view class="section-block compact-block">
                        <text class="section-title">After-sale request</text>
                        <text class="section-subtitle"
                            >Submit a refund or a return + refund request for
                            the selected order.</text
                        >
                    </view>
                    <button class="btn-outline" @click="resetAfterSaleDraft">
                        Close
                    </button>
                </view>

                <picker
                    mode="selector"
                    :range="['REFUND', 'RETURN_REFUND']"
                    :value="
                        afterSaleDraft.afterSaleType === 'RETURN_REFUND' ? 1 : 0
                    "
                    @change="
                        afterSaleDraft.afterSaleType =
                            $event.detail.value === 1
                                ? 'RETURN_REFUND'
                                : 'REFUND'
                    "
                >
                    <view class="picker-field"
                        >Type: {{ afterSaleDraft.afterSaleType }}</view
                    >
                </picker>

                <input
                    v-model="afterSaleDraft.reason"
                    class="input"
                    placeholder="Reason"
                />
                <textarea
                    v-model="afterSaleDraft.description"
                    class="textarea"
                    placeholder="Description"
                />
                <input
                    v-model="afterSaleDraft.applyAmount"
                    class="input"
                    type="digit"
                    placeholder="Apply amount"
                />

                <button
                    class="btn-primary submit-button"
                    :loading="refundingOrderId === afterSaleDraft.orderId"
                    @click="submitAfterSale"
                >
                    Submit after-sale request
                </button>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.orders-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.hero-card {
    padding: 36px;
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) 300px;
    gap: 24px;
    align-items: stretch;
}

.hero-copy {
    display: flex;
    flex-direction: column;
    gap: 18px;
    justify-content: center;
    min-height: 320px;
}

.hero-stats {
    display: flex;
    flex-direction: column;
    gap: 14px;
    justify-content: flex-end;
}

.panel,
.aftersale-panel {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
}

.compact-block {
    gap: 6px;
}

.order-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.order-card {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.order-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
}

.order-main {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.order-name {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.order-sub {
    font-size: 13px;
    color: var(--text-muted);
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

.status-0 {
    background: var(--highlight-soft);
    color: #8a6a1d;
}

.status-1 {
    background: rgba(11, 107, 95, 0.16);
    color: var(--accent);
}

.status-2 {
    background: rgba(15, 118, 110, 0.18);
    color: #0b5d54;
}

.status-3 {
    background: rgba(52, 199, 89, 0.14);
    color: #167c3a;
}

.status-4 {
    background: rgba(255, 69, 58, 0.12);
    color: #b42318;
}

.order-metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.metric-item {
    padding: 14px;
    border-radius: var(--radius-md);
    background: var(--panel-muted);
    border: 1px solid rgba(20, 20, 20, 0.08);
    display: flex;
    flex-direction: column;
    gap: 8px;
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

.meta-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.meta {
    font-size: 13px;
    color: var(--text-muted);
    word-break: break-all;
    line-height: 1.7;
}

.actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.action-button {
    flex: 1 1 180px;
}

.picker-field,
.input,
.textarea {
    width: 100%;
    background: rgba(255, 255, 255, 0.96);
    border-radius: 16px;
    padding: 13px 16px;
    font-size: 14px;
    border: 1px solid rgba(20, 20, 20, 0.12);
}

.picker-field:focus,
.input:focus,
.textarea:focus {
    border-color: rgba(11, 107, 95, 0.4);
    box-shadow: 0 0 0 3px rgba(11, 107, 95, 0.12);
}

.textarea {
    min-height: 120px;
}

.submit-button {
    width: 100%;
}

@media (hover: hover) {
    .order-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
        border-color: rgba(20, 20, 20, 0.12);
    }
}

@media (max-width: 900px) {
    .hero-card,
    .order-grid {
        grid-template-columns: 1fr;
    }

    .hero-card {
        padding: 26px;
    }

    .hero-copy {
        min-height: auto;
    }

    .header {
        flex-direction: column;
    }
}
</style>
