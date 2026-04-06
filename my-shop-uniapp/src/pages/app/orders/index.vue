<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed, reactive, ref } from "vue";
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
import { useLocale } from "../../../i18n/locale";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";
import type { AfterSaleInfo, OrderItem } from "../../../types/domain";
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

const { locale } = useLocale();

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              pageTitle: "Orders",
              eyebrow: "Orders",
              heroTitle: "Track payment, delivery, and after-sale flow with less drift.",
              heroSubtitle:
                  "Stay inside one order board to reopen payment, confirm receipt, and start after-sale work without losing context.",
              totalOrders: "Total orders",
              pendingPay: "Pending payment",
              recordsTitle: "Order records",
              recordsSubtitle:
                  "Review recent activity and the actions available for each order.",
              refresh: "Refresh",
              empty: "No orders yet",
              orderLabel: "Order",
              createdAt: "Created",
              amount: "Amount",
              age: "Age",
              subOrder: "Sub-order",
              refundNo: "Refund no.",
              afterSaleStatus: "After-sale",
              openingCheckout: "Opening checkout...",
              payNow: "Pay now",
              complete: "Confirm receipt",
              viewRefund: "View refund",
              applyAfterSale: "Apply after-sale",
              cancelAfterSale: "Cancel after-sale",
              cancelOrder: "Cancel order",
              afterSaleTitle: "After-sale request",
              afterSaleSubtitle:
                  "Create a refund or return-and-refund request for the selected order.",
              close: "Close",
              afterSaleType: "After-sale type",
              reasonPlaceholder: "Enter the after-sale reason",
              descriptionPlaceholder: "Add more detail",
              amountPlaceholder: "Enter the requested amount",
              submitAfterSale: "Submit request",
              orderMetaMissing: "The order is missing after-sale metadata.",
              amountInvalid: "Requested amount must be greater than 0.",
              reasonRequired: "Please provide the after-sale reason.",
              loadFailed: "Failed to load orders",
              payInfoMissing: "This order is missing payment metadata.",
              amountUnavailable: "The order amount is invalid.",
              openCheckoutFailed: "Failed to open checkout",
              afterSaleUnsupported:
                  "This order cannot create a new after-sale request.",
              refundUnavailable: "Refund tracking is unavailable for this order.",
              selectOrderFirst: "Select an order first.",
              afterSaleCreatedPrefix: "After-sale request created:",
              afterSaleCreateFailed: "Failed to create after-sale request",
              afterSaleCancelFailed: "Failed to cancel the after-sale request",
              afterSaleCancelled: "After-sale request cancelled",
              afterSaleCannotCancel:
                  "The current after-sale request cannot be cancelled.",
              cancelConfirm: (orderNo: string) => `Cancel order ${orderNo}?`,
              cancelSuccess: "Order cancelled",
              cancelFailed: "Failed to cancel order",
              completeConfirm: (orderNo: string) =>
                  `Confirm receipt for order ${orderNo}?`,
              completeSuccess: "Order completed",
              completeFailed: "Failed to confirm receipt",
              completeUnavailable: "This order cannot be completed right now.",
              afterSaleCancelConfirm: (value: string) =>
                  `Cancel after-sale request ${value}?`,
          }
        : {
              pageTitle: "订单",
              eyebrow: "订单",
              heroTitle: "更清晰地跟进支付、履约和售后节奏。",
              heroSubtitle:
                  "在一个订单面板里继续支付、确认收货和处理售后动作，避免在流程里丢失上下文。",
              totalOrders: "订单总数",
              pendingPay: "待支付",
              recordsTitle: "订单记录",
              recordsSubtitle: "查看最近订单动态以及每笔订单可执行的动作。",
              refresh: "刷新",
              empty: "暂无订单",
              orderLabel: "订单",
              createdAt: "创建时间",
              amount: "金额",
              age: "下单时长",
              subOrder: "子订单号",
              refundNo: "退款单号",
              afterSaleStatus: "售后状态",
              openingCheckout: "正在打开收银台...",
              payNow: "立即支付",
              complete: "确认收货",
              viewRefund: "查看退款",
              applyAfterSale: "申请售后",
              cancelAfterSale: "取消售后",
              cancelOrder: "取消订单",
              afterSaleTitle: "售后申请",
              afterSaleSubtitle: "为当前选中订单提交退款或退货退款申请。",
              close: "关闭",
              afterSaleType: "售后类型",
              reasonPlaceholder: "请输入售后原因",
              descriptionPlaceholder: "请输入补充说明",
              amountPlaceholder: "请输入申请金额",
              submitAfterSale: "提交售后申请",
              orderMetaMissing: "订单缺少售后所需的元数据。",
              amountInvalid: "申请金额必须大于 0。",
              reasonRequired: "请填写售后原因。",
              loadFailed: "加载订单失败",
              payInfoMissing: "订单缺少支付信息。",
              amountUnavailable: "订单金额无效，无法支付。",
              openCheckoutFailed: "拉起支付失败",
              afterSaleUnsupported: "当前订单暂不支持新的售后申请。",
              refundUnavailable: "当前订单暂无退款跟踪信息。",
              selectOrderFirst: "请先选择订单。",
              afterSaleCreatedPrefix: "售后申请已创建：",
              afterSaleCreateFailed: "创建售后申请失败",
              afterSaleCancelFailed: "取消售后申请失败",
              afterSaleCancelled: "售后申请已取消",
              afterSaleCannotCancel: "当前售后申请无法取消。",
              cancelConfirm: (orderNo: string) => `确认取消订单 ${orderNo} 吗？`,
              cancelSuccess: "订单已取消",
              cancelFailed: "取消订单失败",
              completeConfirm: (orderNo: string) =>
                  `确认签收订单 ${orderNo} 吗？`,
              completeSuccess: "订单已完成",
              completeFailed: "确认收货失败",
              completeUnavailable: "当前订单无法确认收货。",
              afterSaleCancelConfirm: (value: string) =>
                  `确认取消售后申请 ${value} 吗？`,
          },
);

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
        toast(copy.value.afterSaleUnsupported);
        return;
    }
    afterSaleDraft.orderId = order.id;
    afterSaleDraft.afterSaleType = "REFUND";
    afterSaleDraft.reason = "";
    afterSaleDraft.description = "";
    afterSaleDraft.applyAmount = String(order.payAmount ?? order.totalAmount ?? "");
}

function selectedOrder(): OrderItem | null {
    return rows.value.find((item) => item.id === afterSaleDraft.orderId) ?? null;
}

function buildAfterSalePayload(order: OrderItem): AfterSaleInfo | null {
    if (
        typeof order.id !== "number" ||
        typeof order.subOrderId !== "number" ||
        typeof order.merchantId !== "number"
    ) {
        toast(copy.value.orderMetaMissing);
        return null;
    }
    const amount = Number(afterSaleDraft.applyAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
        toast(copy.value.amountInvalid);
        return null;
    }
    if (!afterSaleDraft.reason.trim()) {
        toast(copy.value.reasonRequired);
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
    if (loading.value) {
        return;
    }
    loading.value = true;
    try {
        const result = await listOrders({ page: 1, size: 30 });
        rows.value = result.records;
    } catch (error) {
        toast(error instanceof Error ? error.message : copy.value.loadFailed);
    } finally {
        loading.value = false;
    }
}

async function onPay(order: OrderItem): Promise<void> {
    if (!canPay(order) || typeof order.userId !== "number" || !order.subOrderNo) {
        toast(copy.value.payInfoMissing);
        return;
    }
    const amount = Number(order.payAmount ?? order.totalAmount ?? NaN);
    if (!Number.isFinite(amount) || amount <= 0) {
        toast(copy.value.amountUnavailable);
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
        toast(error instanceof Error ? error.message : copy.value.openCheckoutFailed);
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
    if (typeof order.id !== "number") {
        return;
    }
    const ok = await confirm(copy.value.cancelConfirm(order.orderNo));
    if (!ok) {
        return;
    }
    try {
        await cancelOrder(order.id);
        toast(copy.value.cancelSuccess, "success");
        await loadOrders();
    } catch (error) {
        toast(error instanceof Error ? error.message : copy.value.cancelFailed);
    }
}

async function onComplete(order: OrderItem): Promise<void> {
    if (!canComplete(order) || typeof order.id !== "number") {
        toast(copy.value.completeUnavailable);
        return;
    }
    const ok = await confirm(copy.value.completeConfirm(order.orderNo));
    if (!ok) {
        return;
    }
    completingOrderId.value = order.id;
    try {
        await completeOrder(order.id);
        toast(copy.value.completeSuccess, "success");
        await loadOrders();
    } catch (error) {
        toast(error instanceof Error ? error.message : copy.value.completeFailed);
    } finally {
        completingOrderId.value = null;
    }
}

function onViewRefund(order: OrderItem): void {
    if (!canViewRefund(order)) {
        toast(copy.value.refundUnavailable);
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
        toast(copy.value.selectOrderFirst);
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
            `${copy.value.afterSaleCreatedPrefix} ${result.afterSaleNo ?? "--"}`,
            "success",
        );
        resetAfterSaleDraft();
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : copy.value.afterSaleCreateFailed,
        );
    } finally {
        refundingOrderId.value = null;
    }
}

async function cancelAfterSale(order: OrderItem): Promise<void> {
    if (!canCancelAfterSale(order) || typeof order.afterSaleId !== "number") {
        toast(copy.value.afterSaleCannotCancel);
        return;
    }
    const ok = await confirm(
        copy.value.afterSaleCancelConfirm(
            order.afterSaleNo ?? String(order.afterSaleId),
        ),
    );
    if (!ok) {
        return;
    }
    refundingOrderId.value = order.id;
    try {
        await advanceAfterSaleStatus(order.afterSaleId, "CANCEL");
        toast(copy.value.afterSaleCancelled, "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : copy.value.afterSaleCancelFailed,
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
    <AppShell :title="copy.pageTitle">
        <view class="orders-layout">
            <view class="hero-card display-panel fade-in-up">
                <view class="hero-copy">
                    <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
                    <text class="hero-title">{{ copy.heroTitle }}</text>
                    <text class="hero-subtitle">{{ copy.heroSubtitle }}</text>
                </view>

                <view class="hero-stats">
                    <view class="info-card">
                        <text class="info-label">{{ copy.totalOrders }}</text>
                        <text class="info-value">{{ rows.length }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">{{ copy.pendingPay }}</text>
                        <text class="info-value">
                            {{ rows.filter((item) => item.status === 0).length }}
                        </text>
                    </view>
                </view>
            </view>

            <view class="surface-card panel fade-in-up">
                <view class="header">
                    <view class="section-block compact-block">
                        <text class="section-title">{{ copy.recordsTitle }}</text>
                        <text class="section-subtitle">
                            {{ copy.recordsSubtitle }}
                        </text>
                    </view>
                    <button class="btn-outline" @click="loadOrders">
                        {{ copy.refresh }}
                    </button>
                </view>

                <view v-if="rows.length === 0" class="empty-state">
                    {{ copy.empty }}
                </view>

                <view v-else class="order-grid">
                    <view
                        v-for="item in rows"
                        :key="item.id"
                        class="order-card surface-card"
                    >
                        <view class="order-head">
                            <view class="order-main">
                                <text class="order-name">
                                    {{ copy.orderLabel }} {{ item.orderNo }}
                                </text>
                                <text class="order-sub">
                                    {{ copy.createdAt }} {{ formatDate(item.createdAt) }}
                                </text>
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
                                <text class="metric-label">{{ copy.amount }}</text>
                                <text class="metric-value">
                                    {{ formatPrice(item.payAmount ?? item.totalAmount) }}
                                </text>
                            </view>
                            <view class="metric-item">
                                <text class="metric-label">{{ copy.age }}</text>
                                <text class="metric-value">
                                    {{ formatRelativeDate(item.createdAt) }}
                                </text>
                            </view>
                        </view>

                        <view class="meta-list">
                            <text class="meta" v-if="item.subOrderNo">
                                {{ copy.subOrder }}: {{ item.subOrderNo }}
                            </text>
                            <text class="meta" v-if="item.refundNo">
                                {{ copy.refundNo }}: {{ item.refundNo }}
                            </text>
                            <text
                                v-if="
                                    item.afterSaleStatus &&
                                    item.afterSaleStatus !== 'NONE'
                                "
                                class="meta"
                            >
                                {{ copy.afterSaleStatus }}: {{ item.afterSaleStatus
                                }}{{
                                    item.afterSaleNo ? ` (${item.afterSaleNo})` : ""
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
                                        ? copy.openingCheckout
                                        : copy.payNow
                                }}
                            </button>
                            <button
                                v-if="canComplete(item)"
                                class="btn-outline action-button"
                                :loading="completingOrderId === item.id"
                                @click="onComplete(item)"
                            >
                                {{ copy.complete }}
                            </button>
                            <button
                                v-if="canViewRefund(item)"
                                class="btn-outline action-button"
                                @click="onViewRefund(item)"
                            >
                                {{ copy.viewRefund }}
                            </button>
                            <button
                                v-if="canApplyAfterSale(item)"
                                class="btn-outline action-button"
                                @click="openAfterSale(item)"
                            >
                                {{ copy.applyAfterSale }}
                            </button>
                            <button
                                v-if="canCancelAfterSale(item)"
                                class="btn-outline action-button"
                                @click="cancelAfterSale(item)"
                            >
                                {{ copy.cancelAfterSale }}
                            </button>
                            <button
                                class="btn-secondary action-button"
                                @click="onCancel(item)"
                            >
                                {{ copy.cancelOrder }}
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
                        <text class="section-title">{{ copy.afterSaleTitle }}</text>
                        <text class="section-subtitle">
                            {{ copy.afterSaleSubtitle }}
                        </text>
                    </view>
                    <button class="btn-outline" @click="resetAfterSaleDraft">
                        {{ copy.close }}
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
                    <view class="picker-field">
                        {{ copy.afterSaleType }}: {{ afterSaleDraft.afterSaleType }}
                    </view>
                </picker>

                <input
                    v-model="afterSaleDraft.reason"
                    class="input"
                    :placeholder="copy.reasonPlaceholder"
                />
                <textarea
                    v-model="afterSaleDraft.description"
                    class="textarea"
                    :placeholder="copy.descriptionPlaceholder"
                />
                <input
                    v-model="afterSaleDraft.applyAmount"
                    class="input"
                    type="digit"
                    :placeholder="copy.amountPlaceholder"
                />

                <button
                    class="btn-primary submit-button"
                    :loading="refundingOrderId === afterSaleDraft.orderId"
                    @click="submitAfterSale"
                >
                    {{ copy.submitAfterSale }}
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
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease;
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
    font-weight: 800;
    letter-spacing: -0.03em;
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
    background: rgba(255, 255, 255, 0.08);
    color: var(--text-main);
    white-space: nowrap;
}

.status-0 {
    background: var(--highlight-soft);
    color: #d9a44b;
}

.status-1 {
    background: rgba(95, 209, 194, 0.16);
    color: var(--accent);
}

.status-2 {
    background: rgba(67, 176, 255, 0.16);
    color: #8bc5ff;
}

.status-3 {
    background: rgba(64, 201, 135, 0.14);
    color: #58d78f;
}

.status-4 {
    background: rgba(255, 107, 107, 0.16);
    color: #ff9a9a;
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
    border: 1px solid var(--panel-border);
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.metric-label {
    font-size: 12px;
    color: var(--text-muted);
    letter-spacing: 0.05em;
    text-transform: uppercase;
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
    background: rgba(255, 255, 255, 0.04);
    color: var(--text-main);
    border-radius: 16px;
    padding: 13px 16px;
    font-size: 14px;
    border: 1px solid var(--panel-border);
}

.picker-field:focus,
.input:focus,
.textarea:focus {
    border-color: rgba(95, 209, 194, 0.4);
    box-shadow: 0 0 0 3px rgba(95, 209, 194, 0.12);
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
        box-shadow: 0 18px 34px rgba(1, 7, 14, 0.34);
        border-color: var(--panel-border-strong);
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
