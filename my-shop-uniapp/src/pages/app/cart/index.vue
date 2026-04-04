<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import AppShell from "../../../components/AppShell.vue";
import {
    cartItems,
    cartTotal,
    clearCart,
    removeFromCart,
    setCartItemQuantity,
} from "../../../store/cart";
import type { CartEntry } from "../../../store/cart";
import { getDefaultAddress } from "../../../api/address";
import { createOrder } from "../../../api/order";
import { sessionState } from "../../../auth/session";
import type { UserAddress } from "../../../types/domain";
import { formatPrice } from "../../../utils/format";
import { confirm, toast } from "../../../utils/ui";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";

interface ShopGroup {
    shopId: number;
    items: CartEntry[];
    subtotal: number;
}

const placing = ref(false);
const loadingAddress = ref(false);
const selectedAddress = ref<UserAddress | null>(null);
const userId = computed(() => sessionState.user?.id);

const shopGroups = computed<ShopGroup[]>(() => {
    const map = new Map<number, CartEntry[]>();
    for (const item of cartItems.value) {
        const list = map.get(item.shopId);
        if (list) {
            list.push(item);
        } else {
            map.set(item.shopId, [item]);
        }
    }
    return [...map.entries()].map(([shopId, items]) => ({
        shopId,
        items,
        subtotal: items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    }));
});

function formatAddress(address: UserAddress | null): string {
    if (!address) {
        return "";
    }
    return [
        address.province,
        address.city,
        address.district,
        address.street,
        address.detailAddress,
    ]
        .map((part) => part?.trim())
        .filter((part): part is string => Boolean(part))
        .join(" ");
}

async function loadDefaultAddress(): Promise<void> {
    if (loadingAddress.value) {
        return;
    }
    if (typeof userId.value !== "number") {
        selectedAddress.value = null;
        return;
    }

    loadingAddress.value = true;
    try {
        selectedAddress.value = await getDefaultAddress(userId.value);
    } catch (error) {
        selectedAddress.value = null;
        toast(
            error instanceof Error
                ? error.message
                : "Failed to load the default address",
        );
    } finally {
        loadingAddress.value = false;
    }
}

function openAddressBook(): void {
    navigateTo(Routes.appAddresses, undefined, { requiresAuth: true });
}

function changeQuantity(item: CartEntry, delta: number): void {
    const next = item.quantity + delta;
    if (next <= 0) {
        void onRemove(item);
        return;
    }
    setCartItemQuantity(item.productId, item.skuId, next);
}

async function onRemove(item: CartEntry): Promise<void> {
    const ok = await confirm(`Remove ${item.productName} from the cart?`);
    if (!ok) return;
    removeFromCart(item.productId, item.skuId);
    toast("Item removed", "success");
}

async function onClearCart(): Promise<void> {
    const ok = await confirm("Clear the cart?");
    if (!ok) return;
    clearCart();
    toast("Cart cleared", "success");
}

async function onPlaceOrder(): Promise<void> {
    if (shopGroups.value.length === 0) {
        toast("Cart is empty");
        return;
    }
    if (!selectedAddress.value) {
        toast("Add a default address before checkout");
        openAddressBook();
        return;
    }

    const receiverAddress = formatAddress(selectedAddress.value);
    const receiverName = selectedAddress.value.receiverName.trim();
    const receiverPhone = selectedAddress.value.receiverPhone.trim();

    if (!receiverName || !receiverPhone || !receiverAddress) {
        toast("The default address is incomplete");
        openAddressBook();
        return;
    }

    const ok = await confirm(
        `Submit ${shopGroups.value.length} shop order(s)?`,
    );
    if (!ok) return;

    placing.value = true;
    const orderGroups = shopGroups.value.map((group) => ({
        shopId: group.shopId,
        items: group.items.map((item) => ({ ...item })),
    }));
    const failedShops = new Set<number>();
    const successfulItems: CartEntry[] = [];

    try {
        for (const group of orderGroups) {
            for (const item of group.items) {
                if (typeof item.skuId !== "number") {
                    failedShops.add(group.shopId);
                    toast(`Shop ${group.shopId} item is missing a SKU`);
                    continue;
                }
                try {
                    await createOrder({
                        shopId: group.shopId,
                        spuId: item.productId,
                        skuId: item.skuId,
                        quantity: item.quantity,
                        price: item.price,
                        receiverName,
                        receiverPhone,
                        receiverAddress,
                    });
                    successfulItems.push(item);
                } catch (error) {
                    failedShops.add(group.shopId);
                    const msg =
                        error instanceof Error
                            ? error.message
                            : "Unknown error";
                    toast(`Shop ${group.shopId}: ${msg}`);
                }
            }
        }
    } finally {
        placing.value = false;
    }

    successfulItems.forEach((item) => {
        removeFromCart(item.productId, item.skuId);
    });

    if (failedShops.size === 0) {
        toast(`Submitted ${successfulItems.length} item(s)`, "success");
        navigateTo(Routes.appOrders, undefined, { requiresAuth: true });
        return;
    }

    if (successfulItems.length > 0) {
        toast(
            "Some items were submitted. Only failed items remain in the cart.",
        );
        return;
    }

    toast(`Failed shops: ${[...failedShops].join(", ")}`);
}

onShow(() => {
    void loadDefaultAddress();
});
</script>

<template>
    <AppShell title="Cart">
        <view class="cart-layout">
            <view class="hero-card display-panel fade-in-up">
                <view class="hero-copy">
                    <text class="hero-eyebrow">Cart</text>
                    <text class="hero-title"
                        >Review items, confirm delivery details, and move to
                        checkout with less friction.</text
                    >
                    <text class="hero-subtitle">
                        Items are grouped by shop so merchant orders can be
                        created cleanly while keeping the checkout flow simple.
                    </text>
                </view>

                <view class="hero-stats">
                    <view class="info-card">
                        <text class="info-label">Items</text>
                        <text class="info-value">{{ cartItems.length }}</text>
                    </view>
                    <view class="info-card">
                        <text class="info-label">Shops</text>
                        <text class="info-value">{{ shopGroups.length }}</text>
                    </view>
                </view>
            </view>

            <view class="content-grid">
                <view class="main-column">
                    <view class="surface-card panel fade-in-up">
                        <view class="header">
                            <view class="section-block compact-block">
                                <text class="section-title"
                                    >Delivery address</text
                                >
                                <text class="section-subtitle"
                                    >Use the default address for this checkout
                                    session.</text
                                >
                            </view>
                            <button
                                class="btn-outline"
                                @click="openAddressBook"
                            >
                                {{
                                    selectedAddress
                                        ? "Manage addresses"
                                        : "Add address"
                                }}
                            </button>
                        </view>

                        <view class="address-card">
                            <template v-if="selectedAddress">
                                <text class="address-name"
                                    >{{ selectedAddress.receiverName }} |
                                    {{ selectedAddress.receiverPhone }}</text
                                >
                                <text class="address-text">{{
                                    formatAddress(selectedAddress)
                                }}</text>
                            </template>
                            <text
                                v-else-if="loadingAddress"
                                class="address-text"
                                >Loading default address...</text
                            >
                            <text v-else class="address-text"
                                >No default address is available. Add one before
                                checkout.</text
                            >
                        </view>
                    </view>

                    <view class="surface-card panel fade-in-up">
                        <view class="header">
                            <view class="section-block compact-block">
                                <text class="section-title">Cart items</text>
                                <text class="section-subtitle"
                                    >Grouped by shop for clean order
                                    submission.</text
                                >
                            </view>
                            <view class="header-actions">
                                <button
                                    class="btn-outline"
                                    @click="
                                        navigateTo(
                                            Routes.appCatalog,
                                            undefined,
                                            { requiresAuth: true },
                                        )
                                    "
                                >
                                    Continue shopping
                                </button>
                                <button
                                    v-if="cartItems.length > 0"
                                    class="btn-outline"
                                    @click="onClearCart"
                                >
                                    Clear
                                </button>
                            </view>
                        </view>

                        <view v-if="cartItems.length === 0" class="empty-state"
                            >Your cart is empty</view
                        >

                        <view v-else class="group-list">
                            <view
                                v-for="group in shopGroups"
                                :key="group.shopId"
                                class="shop-group"
                            >
                                <view class="shop-head">
                                    <text class="shop-name"
                                        >Shop {{ group.shopId }}</text
                                    >
                                    <text class="shop-subtotal"
                                        >Subtotal
                                        {{ formatPrice(group.subtotal) }}</text
                                    >
                                </view>

                                <view
                                    v-for="item in group.items"
                                    :key="`${item.productId}-${item.skuId}`"
                                    class="item-row"
                                >
                                    <view class="item-info">
                                        <text class="item-name">{{
                                            item.productName
                                        }}</text>
                                        <text class="item-meta"
                                            >{{
                                                formatPrice(item.price)
                                            }}
                                            each</text
                                        >
                                    </view>

                                    <view class="item-actions">
                                        <button
                                            class="btn-outline mini-button"
                                            @click="changeQuantity(item, -1)"
                                        >
                                            -                                        </button>
                                        <text class="qty">{{
                                            item.quantity
                                        }}</text>
                                        <button
                                            class="btn-outline mini-button"
                                            @click="changeQuantity(item, 1)"
                                        >
                                            +
                                        </button>
                                        <button
                                            class="btn-secondary remove-button"
                                            @click="onRemove(item)"
                                        >
                                            Remove
                                        </button>
                                    </view>
                                </view>
                            </view>
                        </view>
                    </view>
                </view>

                <view class="side-column">
                    <view class="surface-card panel summary-card fade-in-up">
                        <view class="section-block compact-block">
                            <text class="section-title">Order summary</text>
                            <text class="section-subtitle"
                                >Final review before creating orders.</text
                            >
                        </view>

                        <view class="summary-metrics">
                            <view class="metric-item">
                                <text class="metric-label">Selected items</text>
                                <text class="metric-value">{{
                                    cartItems.length
                                }}</text>
                            </view>
                            <view class="metric-item">
                                <text class="metric-label"
                                    >Merchant groups</text
                                >
                                <text class="metric-value">{{
                                    shopGroups.length
                                }}</text>
                            </view>
                        </view>

                        <view class="summary-total">
                            <text class="summary-label">Total payable</text>
                            <text class="summary-price">{{
                                formatPrice(cartTotal)
                            }}</text>
                        </view>

                        <text class="summary-note">
                            Orders are created per shop. Successful items will
                            be removed from the cart automatically.
                        </text>

                        <button
                            class="btn-primary submit-button"
                            :loading="placing"
                            :disabled="placing || !selectedAddress"
                            @click="onPlaceOrder"
                        >
                            Submit orders
                        </button>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.cart-layout {
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

.content-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.5fr) 340px;
    gap: 18px;
}

.main-column,
.side-column {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.panel {
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

.header-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.address-card {
    padding: 18px;
    border-radius: var(--radius-lg);
    background: var(--panel-muted);
    border: 1px solid rgba(20, 20, 20, 0.08);
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.address-name {
    font-size: 17px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.address-text {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.7;
}

.group-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.shop-group {
    padding: 18px;
    border-radius: var(--radius-lg);
    background: var(--panel-muted);
    border: 1px solid rgba(20, 20, 20, 0.08);
    display: flex;
    flex-direction: column;
    gap: 14px;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.shop-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    flex-wrap: wrap;
}

.shop-name {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.shop-subtotal {
    font-size: 13px;
    color: var(--text-muted);
}

.item-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-top: 14px;
    border-top: 1px solid rgba(20, 20, 20, 0.08);
    flex-wrap: wrap;
}

.item-info {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.item-name {
    font-size: 15px;
    font-weight: 700;
}

.item-meta {
    font-size: 13px;
    color: var(--text-muted);
}

.item-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.mini-button {
    min-width: 42px;
    padding-left: 0;
    padding-right: 0;
}

.remove-button {
    min-width: 96px;
}

.qty {
    min-width: 28px;
    text-align: center;
    font-size: 15px;
    font-weight: 700;
}

.summary-card {
    position: sticky;
    top: 0;
}

.summary-metrics {
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

.summary-total {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    padding: 16px 0;
    border-top: 1px solid rgba(20, 20, 20, 0.08);
    border-bottom: 1px solid rgba(20, 20, 20, 0.08);
}

.summary-label {
    font-size: 13px;
    color: var(--text-muted);
}

.summary-price {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-main);
    letter-spacing: -0.03em;
}

.summary-note {
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.7;
}

.submit-button {
    width: 100%;
}

.panel {
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

@media (hover: hover) {
    .panel:hover,
    .shop-group:hover {
        transform: translateY(-2px);
        box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
        border-color: rgba(20, 20, 20, 0.12);
    }
}

.empty-state {
    padding: 28px 0;
    text-align: center;
    color: var(--text-muted);
    font-size: 13px;
}

@media (max-width: 900px) {
    .hero-card,
    .content-grid {
        grid-template-columns: 1fr;
    }

    .hero-card {
        padding: 26px;
    }

    .hero-copy {
        min-height: auto;
    }

    .summary-card {
        position: static;
    }
}
</style>
