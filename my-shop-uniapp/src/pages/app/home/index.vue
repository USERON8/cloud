<script setup lang="ts">
import { computed } from "vue";
import AppShell from "../../../components/AppShell.vue";
import { sessionState } from "../../../auth/session";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";

const displayName = computed(
    () =>
        sessionState.user?.nickname ||
        sessionState.user?.username ||
        "Current User",
);
const roleLabel = computed(
    () => (sessionState.user?.roles || []).join(", ") || "USER",
);
</script>

<template>
    <AppShell title="Dashboard">
        <view class="dashboard-layout">
            <view class="hero-card display-panel fade-in-up">
                <view class="hero-copy">
                    <text class="hero-eyebrow">Dashboard</text>
                    <text class="hero-title"
                        >Welcome back, {{ displayName }}.</text
                    >
                    <text class="hero-subtitle">
                        A quieter, more premium commerce workspace for browsing
                        products, following orders, and moving from discovery to
                        checkout with clarity.
                    </text>

                    <view class="hero-actions">
                        <button
                            class="btn-primary"
                            @click="
                                navigateTo(Routes.appCatalog, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            Explore products
                        </button>
                        <button
                            class="btn-outline"
                            @click="
                                navigateTo(Routes.appOrders, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            View orders
                        </button>
                    </view>
                </view>

                <view class="hero-side">
                    <view class="info-card spotlight-card">
                        <text class="info-label">Current role</text>
                        <text class="info-value">{{ roleLabel }}</text>
                        <text class="spotlight-copy"
                            >Your workspace adapts to the permissions available
                            in this session.</text
                        >
                    </view>
                </view>
            </view>

            <view class="feature-grid fade-in-up">
                <view class="feature-card surface-card primary-card">
                    <text class="feature-kicker">Start shopping</text>
                    <text class="feature-title"
                        >Browse the full catalog with a cleaner product-first
                        experience.</text
                    >
                    <text class="feature-copy"
                        >Search indexed products, review pricing, and add
                        eligible items to your cart with fewer
                        distractions.</text
                    >
                    <button
                        class="btn-primary feature-button"
                        @click="
                            navigateTo(Routes.appCatalog, undefined, {
                                requiresAuth: true,
                            })
                        "
                    >
                        Open catalog
                    </button>
                </view>

                <view class="feature-card surface-card">
                    <text class="feature-kicker">Orders</text>
                    <text class="feature-title"
                        >Track purchases and monitor the next fulfillment
                        step.</text
                    >
                    <text class="feature-copy"
                        >Review statuses, payment progress, and order history in
                        one place.</text
                    >
                    <button
                        class="btn-outline feature-button"
                        @click="
                            navigateTo(Routes.appOrders, undefined, {
                                requiresAuth: true,
                            })
                        "
                    >
                        Open orders
                    </button>
                </view>

                <view class="feature-card surface-card">
                    <text class="feature-kicker">Cart</text>
                    <text class="feature-title"
                        >Move from selection to checkout with less
                        friction.</text
                    >
                    <text class="feature-copy"
                        >Review grouped items, confirm addresses, and submit
                        merchant orders smoothly.</text
                    >
                    <button
                        class="btn-outline feature-button"
                        @click="
                            navigateTo(Routes.appCart, undefined, {
                                requiresAuth: true,
                            })
                        "
                    >
                        Open cart
                    </button>
                </view>
            </view>

            <view class="section-block editorial-section fade-in-up">
                <view class="section-head">
                    <text class="section-title">Your essentials</text>
                    <text class="section-subtitle"
                        >A simplified set of actions for the areas you visit
                        most often.</text
                    >
                </view>

                <view class="quick-grid">
                    <view class="quick-card surface-card">
                        <text class="quick-title">Payments</text>
                        <text class="quick-copy"
                            >Follow payment state and reopen checkout when
                            needed.</text
                        >
                        <button
                            class="btn-secondary quick-button"
                            @click="
                                navigateTo(Routes.appPayments, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            Open payments
                        </button>
                    </view>

                    <view class="quick-card surface-card">
                        <text class="quick-title">Addresses</text>
                        <text class="quick-copy"
                            >Maintain default delivery information for fast
                            checkout.</text
                        >
                        <button
                            class="btn-secondary quick-button"
                            @click="
                                navigateTo(Routes.appAddresses, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            Open addresses
                        </button>
                    </view>

                    <view class="quick-card surface-card">
                        <text class="quick-title">Profile</text>
                        <text class="quick-copy"
                            >Review account identity and current session
                            details.</text
                        >
                        <button
                            class="btn-secondary quick-button"
                            @click="
                                navigateTo(Routes.appProfile, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            Open profile
                        </button>
                    </view>
                </view>
            </view>
        </view>
    </AppShell>
</template>

<style scoped>
.dashboard-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.hero-card {
    padding: 36px;
    display: grid;
    grid-template-columns: minmax(0, 1.5fr) 320px;
    gap: 24px;
    align-items: stretch;
}

.hero-copy {
    display: flex;
    flex-direction: column;
    gap: 18px;
    justify-content: center;
    min-height: 420px;
}

.hero-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    padding-top: 8px;
}

.hero-side {
    display: flex;
    align-items: flex-end;
}

.spotlight-card {
    min-height: 220px;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
}

.spotlight-copy {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--text-muted);
}

.feature-grid {
    display: grid;
    grid-template-columns: 1.2fr repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.feature-card {
    padding: 26px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    min-height: 300px;
    justify-content: flex-end;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.primary-card {
    background: linear-gradient(180deg, #ffffff 0%, #f6f1e8 100%);
    border-color: rgba(11, 107, 95, 0.16);
}

.feature-kicker {
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--accent);
    font-weight: 700;
}

.feature-title {
    font-size: 28px;
    line-height: 1.15;
    font-weight: 700;
    letter-spacing: -0.03em;
}

.feature-copy {
    font-size: 14px;
    line-height: 1.7;
    color: var(--text-muted);
}

.feature-button {
    align-self: flex-start;
}

.editorial-section {
    gap: 16px;
}

.section-head {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.quick-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
}

.quick-card {
    padding: 22px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 220px;
    justify-content: flex-end;
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.quick-title {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.quick-copy {
    color: var(--text-muted);
    font-size: 14px;
    line-height: 1.7;
}

.quick-button {
    align-self: flex-start;
}

@media (hover: hover) {
    .feature-card:hover,
    .quick-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
        border-color: rgba(20, 20, 20, 0.12);
    }
}

@media (max-width: 900px) {
    .hero-card,
    .feature-grid,
    .quick-grid {
        grid-template-columns: 1fr;
    }

    .hero-card {
        padding: 26px;
    }

    .hero-copy {
        min-height: auto;
    }

    .feature-card,
    .quick-card {
        min-height: auto;
    }
}
</style>
