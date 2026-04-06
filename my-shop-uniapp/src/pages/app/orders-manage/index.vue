<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import AppShell from "../../../components/AppShell.vue";
import {
    advanceAfterSaleStatus,
    completeOrder,
    listOrders,
    shipOrder,
} from "../../../api/order";
import type { OrderItem } from "../../../types/domain";
import {
    formatDate,
    formatOrderStatus,
    formatPrice,
    formatRelativeDate,
} from "../../../utils/format";
import { confirm, toast } from "../../../utils/ui";

const rows = ref<OrderItem[]>([]);
const loading = ref(false);
const afterSaleActingOrderId = ref<number | null>(null);
const expandedRows = reactive<Record<string, boolean>>({});
const shippingForm = reactive({
    shippingCompany: "",
    trackingNumber: "",
});
const afterSaleForm = reactive({
    remark: "",
});

const activeAfterSaleCount = computed(
    () =>
        rows.value.filter(
            (row) => row.afterSaleStatus && row.afterSaleStatus !== "NONE",
        ).length,
);

type AfterSaleAction =
    | "AUDIT"
    | "APPROVE"
    | "REJECT"
    | "WAIT_RETURN"
    | "RETURN"
    | "RECEIVE"
    | "PROCESS";

type ActionSpec = {
    key: string;
    label: string;
    kind: "ship" | "complete" | "after_sale";
    action?: AfterSaleAction;
};

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

function requireShippingField(value: string, label: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) {
        toast(`${label} is required`);
        return null;
    }
    return trimmed;
}

function canAuditAfterSale(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "APPLIED"
    );
}

function canApproveAfterSale(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "AUDITING"
    );
}

function canRejectAfterSale(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        ["APPLIED", "AUDITING"].includes(order.afterSaleStatus ?? "")
    );
}

function canWaitReturn(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "APPROVED" &&
        order.afterSaleType === "RETURN_REFUND"
    );
}

function canProcessRefund(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        ((order.afterSaleStatus === "APPROVED" &&
            order.afterSaleType === "REFUND") ||
            order.afterSaleStatus === "RECEIVED")
    );
}

function canMarkReturned(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "WAIT_RETURN"
    );
}

function canMarkReceived(order: OrderItem): boolean {
    return (
        typeof order.afterSaleId === "number" &&
        order.afterSaleStatus === "RETURNED"
    );
}

function toggleExpandedRow(key: string | number | undefined): void {
    if (key === undefined || key === null) return;
    const nextKey = String(key);
    expandedRows[nextKey] = !expandedRows[nextKey];
}

function isExpandedRow(key: string | number | undefined): boolean {
    if (key === undefined || key === null) return false;
    return Boolean(expandedRows[String(key)]);
}

async function onShip(order: OrderItem): Promise<void> {
    if (typeof order.id !== "number") return;
    const shippingCompany = requireShippingField(
        shippingForm.shippingCompany,
        "Shipping company",
    );
    if (!shippingCompany) return;
    const trackingNumber = requireShippingField(
        shippingForm.trackingNumber,
        "Tracking number",
    );
    if (!trackingNumber) return;
    const ok = await confirm(`Confirm shipment for order ${order.orderNo}?`);
    if (!ok) return;
    try {
        await shipOrder(order.id, shippingCompany, trackingNumber);
        toast("Order shipped", "success");
        await loadOrders();
    } catch (error) {
        toast(error instanceof Error ? error.message : "Failed to ship order");
    }
}

async function onComplete(order: OrderItem): Promise<void> {
    if (typeof order.id !== "number") return;
    const ok = await confirm(`Confirm completion for order ${order.orderNo}?`);
    if (!ok) return;
    try {
        await completeOrder(order.id);
        toast("Order completed", "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error ? error.message : "Failed to complete order",
        );
    }
}

async function onAdvanceAfterSale(
    order: OrderItem,
    action: AfterSaleAction,
): Promise<void> {
    if (typeof order.afterSaleId !== "number" || typeof order.id !== "number") {
        toast("This order is missing after-sale metadata");
        return;
    }
    const labels: Record<typeof action, string> = {
        AUDIT: "start review",
        APPROVE: "approve",
        REJECT: "reject",
        WAIT_RETURN: "wait for return",
        RETURN: "mark returned",
        RECEIVE: "mark received",
        PROCESS: "start refund",
    };
    const ok = await confirm(
        `Confirm ${labels[action]} for ${order.afterSaleNo ?? order.afterSaleId}?`,
    );
    if (!ok) {
        return;
    }
    afterSaleActingOrderId.value = order.id;
    try {
        const remark = afterSaleForm.remark.trim() || undefined;
        await advanceAfterSaleStatus(order.afterSaleId, action, remark);
        toast(`After-sale ${labels[action]} completed`, "success");
        await loadOrders();
    } catch (error) {
        toast(
            error instanceof Error
                ? error.message
                : "Failed to update the after-sale request",
        );
    } finally {
        afterSaleActingOrderId.value = null;
    }
}

function buildActionSpecs(order: OrderItem): ActionSpec[] {
    const actions: ActionSpec[] = [
        { key: "ship", label: "Ship", kind: "ship" },
        { key: "complete", label: "Complete", kind: "complete" },
    ];

    if (canAuditAfterSale(order)) {
        actions.push({
            key: "audit",
            label: "Start review",
            kind: "after_sale",
            action: "AUDIT",
        });
    }
    if (canApproveAfterSale(order)) {
        actions.push({
            key: "approve",
            label: "Approve",
            kind: "after_sale",
            action: "APPROVE",
        });
    }
    if (canRejectAfterSale(order)) {
        actions.push({
            key: "reject",
            label: "Reject",
            kind: "after_sale",
            action: "REJECT",
        });
    }
    if (canWaitReturn(order)) {
        actions.push({
            key: "wait-return",
            label: "Wait return",
            kind: "after_sale",
            action: "WAIT_RETURN",
        });
    }
    if (canMarkReturned(order)) {
        actions.push({
            key: "mark-returned",
            label: "Mark returned",
            kind: "after_sale",
            action: "RETURN",
        });
    }
    if (canMarkReceived(order)) {
        actions.push({
            key: "mark-received",
            label: "Mark received",
            kind: "after_sale",
            action: "RECEIVE",
        });
    }
    if (canProcessRefund(order)) {
        actions.push({
            key: "process",
            label: "Start refund",
            kind: "after_sale",
            action: "PROCESS",
        });
    }

    return actions;
}

function visibleActions(order: OrderItem): ActionSpec[] {
    return buildActionSpecs(order).slice(0, 3);
}

function hiddenActions(order: OrderItem): ActionSpec[] {
    return buildActionSpecs(order).slice(3);
}

function hasMoreActions(order: OrderItem): boolean {
    return hiddenActions(order).length > 0;
}

async function triggerAction(order: OrderItem, spec: ActionSpec): Promise<void> {
    if (spec.kind === "ship") {
        await onShip(order);
        return;
    }
    if (spec.kind === "complete") {
        await onComplete(order);
        return;
    }
    if (spec.action) {
        await onAdvanceAfterSale(order, spec.action);
    }
}

onMounted(() => {
    void loadOrders();
});
</script>

<template>
    <AppShell title="Order Admin">
        <view class="orders-page">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">Merchant operations</text>
                    <text class="hero-title">Order control room</text>
                    <text class="hero-subtitle">
                        Ship orders, complete delivery flow, and process
                        after-sale requests from one queue.
                    </text>

                    <view class="action-wrap">
                        <button
                            class="btn-primary"
                            :loading="loading"
                            @click="loadOrders"
                        >
                            Refresh orders
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view class="metric-card">
                        <text class="metric-label">Orders</text>
                        <text class="metric-value">{{ rows.length }}</text>
                    </view>
                    <view class="metric-card">
                        <text class="metric-label">After-sale</text>
                        <text class="metric-value">
                            {{ activeAfterSaleCount }}
                        </text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-main fade-in-up">
                <view class="surface-card panel-block">
                    <view class="section-head">
                        <text class="section-title">Shared dispatch tools</text>
                        <text class="section-subtitle">
                            Shipping fields and after-sale remarks are reused
                            across the current order list.
                        </text>
                    </view>

                    <view class="form-grid">
                        <view class="field">
                            <text class="field-label">Shipping company</text>
                            <input
                                v-model="shippingForm.shippingCompany"
                                class="field-control"
                                placeholder="e.g. FedEx"
                            />
                        </view>
                        <view class="field">
                            <text class="field-label">Tracking number</text>
                            <input
                                v-model="shippingForm.trackingNumber"
                                class="field-control"
                                placeholder="Enter tracking no."
                            />
                        </view>
                        <view class="field field-span">
                            <text class="field-label">After-sale remark</text>
                            <input
                                v-model="afterSaleForm.remark"
                                class="field-control"
                                placeholder="Optional remark"
                            />
                        </view>
                    </view>
                </view>

                <view class="surface-card panel-block sticky-side">
                    <view class="section-head">
                        <text class="section-title">Queue focus</text>
                        <text class="section-subtitle">
                            Each row surfaces order value, status, creation
                            time, and available actions before the extra
                            after-sale detail.
                        </text>
                    </view>
                </view>
            </view>

            <view v-if="rows.length === 0" class="empty-state fade-in-up">
                <text>No orders found</text>
            </view>

            <view v-else class="orders-list fade-in-up">
                <view
                    v-for="item in rows"
                    :key="item.id"
                    class="surface-card panel-block panel-hover order-card"
                >
                    <view class="order-main">
                        <text class="row-title">{{ item.orderNo }}</text>

                        <view class="meta-inline">
                            <text class="meta-chip">
                                {{ formatOrderStatus(item.status) }}
                            </text>
                            <text class="meta-chip">
                                {{
                                    item.afterSaleStatus &&
                                    item.afterSaleStatus !== "NONE"
                                        ? `After-sale: ${item.afterSaleStatus}`
                                        : "No after-sale"
                                }}
                            </text>
                        </view>

                        <view class="summary-grid">
                            <view class="summary-item">
                                <text class="summary-label">Amount</text>
                                <text class="summary-value">{{
                                    formatPrice(
                                        item.payAmount ?? item.totalAmount,
                                    )
                                }}</text>
                            </view>
                            <view class="summary-item">
                                <text class="summary-label">Created</text>
                                <text class="summary-value">
                                    {{ formatDate(item.createdAt) }}
                                </text>
                                <text class="summary-subvalue">
                                    {{ formatRelativeDate(item.createdAt) }}
                                </text>
                            </view>
                        </view>

                        <view
                            v-if="isExpandedRow(item.id)"
                            class="detail-grid"
                        >
                            <view class="detail-item">
                                <text class="summary-label">Order ID</text>
                                <text class="summary-value">{{
                                    item.id ?? "--"
                                }}</text>
                            </view>
                            <view class="detail-item">
                                <text class="summary-label">After-sale no</text>
                                <text class="summary-value">{{
                                    item.afterSaleNo || "--"
                                }}</text>
                            </view>
                            <view class="detail-item">
                                <text class="summary-label">After-sale type</text>
                                <text class="summary-value">{{
                                    item.afterSaleType || "--"
                                }}</text>
                            </view>
                            <view class="detail-item">
                                <text class="summary-label"
                                    >After-sale status</text
                                >
                                <text class="summary-value">{{
                                    item.afterSaleStatus || "--"
                                }}</text>
                            </view>
                        </view>
                    </view>

                    <view class="action-wrap row-actions">
                        <button
                            class="btn-secondary"
                            @click="toggleExpandedRow(item.id)"
                        >
                            {{
                                isExpandedRow(item.id)
                                    ? "Hide details"
                                    : "Show details"
                            }}
                        </button>
                        <button
                            v-for="spec in visibleActions(item)"
                            :key="spec.key"
                            class="btn-outline"
                            :loading="
                                spec.kind === 'after_sale' &&
                                afterSaleActingOrderId === item.id
                            "
                            @click="triggerAction(item, spec)"
                        >
                            {{ spec.label }}
                        </button>
                        <text
                            v-if="hasMoreActions(item) && !isExpandedRow(item.id)"
                            class="more-hint"
                        >
                            More actions inside details
                        </text>
                    </view>

                    <view
                        v-if="isExpandedRow(item.id) && hasMoreActions(item)"
                        class="surface-muted panel-block secondary-actions"
                    >
                        <text class="summary-label">More actions</text>
                        <view class="action-wrap">
                            <button
                                v-for="spec in hiddenActions(item)"
                                :key="spec.key"
                                class="btn-secondary"
                                :loading="
                                    spec.kind === 'after_sale' &&
                                    afterSaleActingOrderId === item.id
                                "
                                @click="triggerAction(item, spec)"
                            >
                                {{ spec.label }}
                            </button>
                        </view>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.orders-page {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.field-span {
    grid-column: span 2;
}

.field-label {
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--text-soft);
}

.orders-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.order-card {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
}

.order-main {
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex: 1;
    min-width: 260px;
}

.row-title {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.03em;
    color: var(--text-main);
}

.summary-subvalue {
    font-size: 12px;
    color: var(--text-muted);
}

.row-actions {
    align-items: center;
}

.secondary-actions {
    gap: 12px;
}

.more-hint {
    font-size: 12px;
    color: var(--text-soft);
}

@media (max-width: 768px) {
    .form-grid {
        grid-template-columns: 1fr;
    }

    .field-span {
        grid-column: span 1;
    }

    .row-actions {
        width: 100%;
    }
}
</style>
