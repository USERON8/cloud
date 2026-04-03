<script setup lang="ts">
import { computed } from "vue";
import { logout } from "../api/auth";
import { useRole, type UserRole } from "../auth/permission";
import { clearSession, sessionState } from "../auth/session";
import { cartCount } from "../store/cart";
import { currentRoutePath, navigateTo, redirectTo } from "../router/navigation";
import { Routes, type RoutePath } from "../router/routes";

defineProps<{ title?: string }>();

interface NavItem {
    label: string;
    path: RoutePath;
    roles: UserRole[];
    public?: boolean;
    showBadge?: boolean;
}

const { role } = useRole();

const navItems: NavItem[] = [
    {
        label: "Dashboard",
        path: Routes.appHome,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    {
        label: "Marketplace",
        path: Routes.market,
        roles: ["USER", "MERCHANT", "ADMIN"],
        public: true,
    },
    {
        label: "Products",
        path: Routes.appCatalog,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    {
        label: "Product Admin",
        path: Routes.appCatalogManage,
        roles: ["MERCHANT", "ADMIN"],
    },
    {
        label: "Orders",
        path: Routes.appOrders,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    {
        label: "Order Admin",
        path: Routes.appOrdersManage,
        roles: ["MERCHANT", "ADMIN"],
    },
    {
        label: "Payments",
        path: Routes.appPayments,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    { label: "Stock Ledger", path: Routes.appStock, roles: ["ADMIN"] },
    {
        label: "Cart",
        path: Routes.appCart,
        roles: ["USER", "MERCHANT", "ADMIN"],
        showBadge: true,
    },
    {
        label: "Addresses",
        path: Routes.appAddresses,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    { label: "Merchant Center", path: Routes.appMerchant, roles: ["MERCHANT"] },
    { label: "Admin Center", path: Routes.appAdmin, roles: ["ADMIN"] },
    { label: "Ops Center", path: Routes.appOps, roles: ["MERCHANT", "ADMIN"] },
    {
        label: "Profile",
        path: Routes.appProfile,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
];

const visibleNavItems = computed(() =>
    navItems.filter((item) => item.public || item.roles.includes(role.value)),
);

const displayName = computed(
    () =>
        sessionState.user?.nickname ||
        sessionState.user?.username ||
        "Current User",
);
const roleLabel = computed(() => role.value);

function isActive(path: string): boolean {
    const current = currentRoutePath();
    if (!current) {
        return false;
    }
    return current === path.replace(/^\//, "") || current === path;
}

function handleNav(item: NavItem): void {
    const guard = item.public
        ? undefined
        : { requiresAuth: true, roles: item.roles };
    navigateTo(item.path, undefined, guard);
}

async function handleLogout(): Promise<void> {
    try {
        await logout();
    } catch {
        // ignore
    } finally {
        clearSession();
        uni.showToast({ title: "Signed out", icon: "success" });
        redirectTo(Routes.login);
    }
}
</script>

<template>
    <view class="app-shell">
        <view class="page-container shell-inner">
            <view class="top-bar">
                <view class="top-main">
                    <text class="brand">My Shop</text>
                    <text class="title">{{ title || "Dashboard" }}</text>
                    <text class="subtitle"
                        >A refined commerce experience inspired by premium
                        product storytelling.</text
                    >
                </view>
                <view class="user-meta">
                    <view class="role-chip">{{ roleLabel }}</view>
                    <text class="user-name">{{ displayName }}</text>
                    <button
                        class="btn-secondary logout-btn"
                        @click="handleLogout"
                    >
                        Sign out
                    </button>
                </view>
            </view>

            <scroll-view class="nav-row" scroll-x>
                <view class="nav-items">
                    <view
                        v-for="item in visibleNavItems"
                        :key="item.path"
                        class="nav-item"
                        :class="{ active: isActive(item.path) }"
                        @click="handleNav(item)"
                    >
                        <text>{{ item.label }}</text>
                        <text
                            v-if="item.showBadge && cartCount > 0"
                            class="badge"
                            >{{ cartCount }}</text
                        >
                    </view>
                </view>
            </scroll-view>

            <view class="content">
                <slot />
            </view>
        </view>
    </view>
</template>

<style scoped>
.app-shell {
    min-height: 100vh;
    padding: 20px 0 36px;
}

.shell-inner {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.top-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    padding: 14px 4px 2px;
}

.top-main {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.brand {
    display: inline-flex;
    align-self: flex-start;
    font-size: 12px;
    color: var(--accent);
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.title {
    display: block;
    font-size: clamp(28px, 4vw, 44px);
    font-weight: 700;
    letter-spacing: -0.04em;
    line-height: 1.02;
}

.subtitle {
    color: var(--text-muted);
    font-size: 15px;
    line-height: 1.65;
    max-width: 680px;
}

.user-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: flex-end;
    padding-top: 4px;
}

.role-chip {
    font-size: 12px;
    border: 1px solid rgba(29, 29, 31, 0.08);
    border-radius: 999px;
    color: var(--text-main);
    padding: 7px 12px;
    background: rgba(255, 255, 255, 0.72);
    font-weight: 600;
}

.user-name {
    color: var(--text-muted);
    font-size: 13px;
}

.logout-btn {
    min-width: 100px;
}

.nav-row {
    padding: 0 2px 4px;
}

.nav-items {
    display: flex;
    gap: 10px;
    padding: 6px 0;
}

.nav-item {
    padding: 11px 16px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.64);
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border: 1px solid rgba(15, 23, 42, 0.05);
    color: var(--text-main);
    font-weight: 600;
    transition:
        background-color 0.2s ease,
        border-color 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease,
        color 0.2s ease;
}

.nav-item.active {
    background: rgba(255, 255, 255, 0.92);
    border-color: rgba(15, 23, 42, 0.08);
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
    color: var(--text-main);
}

.badge {
    background: var(--text-main);
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 999px;
}

.content {
    padding-bottom: 20px;
}

@media (hover: hover) {
    .nav-item:hover {
        transform: translateY(-1px);
        box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
    }
}

button {
    margin: 0;
    line-height: 1;
}

@media (max-width: 768px) {
    .app-shell {
        padding-top: 16px;
    }

    .top-bar {
        flex-direction: column;
        padding-top: 6px;
    }

    .title {
        font-size: 30px;
    }

    .subtitle {
        font-size: 14px;
    }

    .user-meta {
        width: 100%;
        justify-content: flex-start;
    }
}
</style>
