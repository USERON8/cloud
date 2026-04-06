<script setup lang="ts">
import { computed } from "vue";
import { logout } from "../api/auth";
import { useRole, type UserRole } from "../auth/permission";
import { clearSession, sessionState } from "../auth/session";
import { type Locale, useLocale } from "../i18n/locale";
import { currentRoutePath, navigateTo, redirectTo } from "../router/navigation";
import { Routes, type RoutePath } from "../router/routes";
import { cartCount } from "../store/cart";
import LocaleSwitch from "./LocaleSwitch.vue";

const props = defineProps<{ title?: string }>();

interface NavItem {
    key: string;
    path: RoutePath;
    roles: UserRole[];
    public?: boolean;
    showBadge?: boolean;
}

const { role } = useRole();
const { locale } = useLocale();

const navItems: NavItem[] = [
    { key: "home", path: Routes.appHome, roles: ["USER", "MERCHANT", "ADMIN"] },
    {
        key: "market",
        path: Routes.market,
        roles: ["USER", "MERCHANT", "ADMIN"],
        public: true,
    },
    {
        key: "catalog",
        path: Routes.appCatalog,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    {
        key: "catalogManage",
        path: Routes.appCatalogManage,
        roles: ["MERCHANT", "ADMIN"],
    },
    {
        key: "orders",
        path: Routes.appOrders,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    {
        key: "ordersManage",
        path: Routes.appOrdersManage,
        roles: ["MERCHANT", "ADMIN"],
    },
    {
        key: "payments",
        path: Routes.appPayments,
        roles: ["USER", "ADMIN"],
    },
    { key: "stock", path: Routes.appStock, roles: ["ADMIN"] },
    {
        key: "cart",
        path: Routes.appCart,
        roles: ["USER", "MERCHANT", "ADMIN"],
        showBadge: true,
    },
    {
        key: "addresses",
        path: Routes.appAddresses,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
    { key: "merchant", path: Routes.appMerchant, roles: ["MERCHANT"] },
    { key: "admin", path: Routes.appAdmin, roles: ["ADMIN"] },
    { key: "ops", path: Routes.appOps, roles: ["ADMIN"] },
    {
        key: "profile",
        path: Routes.appProfile,
        roles: ["USER", "MERCHANT", "ADMIN"],
    },
];

const copy = computed(() => {
    if (locale.value === "en-US") {
        return {
            brand: "My Shop Cloud",
            defaultTitle: "Control Center",
            subtitle:
                "A sharper cloud console for inventory, orders, payment flow, and storefront operations.",
            currentUser: "Current user",
            logout: "Sign out",
            logoutSuccess: "Signed out",
            nav: {
                home: "Home",
                market: "Market",
                catalog: "Catalog",
                catalogManage: "Catalog Ops",
                orders: "Orders",
                ordersManage: "Order Ops",
                payments: "Payments",
                stock: "Stock",
                cart: "Cart",
                addresses: "Addresses",
                merchant: "Merchant",
                admin: "Admin",
                ops: "Ops",
                profile: "Profile",
            },
        };
    }

    return {
        brand: "云端商城工作台",
        defaultTitle: "控制中心",
        subtitle:
            "围绕商品、订单、支付与经营动作重组界面层次，让日常操作更聚焦、更稳定。",
        currentUser: "当前用户",
        logout: "退出登录",
        logoutSuccess: "已退出登录",
        nav: {
            home: "首页",
            market: "商城",
            catalog: "商品",
            catalogManage: "商品管理",
            orders: "订单",
            ordersManage: "订单管理",
            payments: "支付",
            stock: "库存台账",
            cart: "购物车",
            addresses: "地址簿",
            merchant: "商家中心",
            admin: "管理中心",
            ops: "运维中心",
            profile: "我的",
        },
    };
});

const roleTextMap: Record<UserRole, Record<Locale, string>> = {
    USER: {
        "zh-CN": "用户",
        "en-US": "User",
    },
    MERCHANT: {
        "zh-CN": "商家",
        "en-US": "Merchant",
    },
    ADMIN: {
        "zh-CN": "管理员",
        "en-US": "Admin",
    },
};

const visibleNavItems = computed(() =>
    navItems
        .filter((item) => item.public || item.roles.includes(role.value))
        .map((item) => ({
            ...item,
            label: copy.value.nav[item.key as keyof typeof copy.value.nav],
        })),
);

const displayName = computed(
    () =>
        sessionState.user?.nickname ||
        sessionState.user?.username ||
        copy.value.currentUser,
);

const roleLabel = computed(
    () => roleTextMap[role.value]?.[locale.value] ?? role.value,
);

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
        uni.showToast({ title: copy.value.logoutSuccess, icon: "success" });
        redirectTo(Routes.login);
    }
}
</script>

<template>
    <view class="app-shell">
        <view class="page-container shell-inner">
            <view class="masthead glass-card fade-in-up">
                <view class="masthead-main">
                    <view class="brand-line">
                        <text class="brand-mark">MS</text>
                        <text class="brand-name">{{ copy.brand }}</text>
                    </view>
                    <text class="title">{{ props.title || copy.defaultTitle }}</text>
                    <text class="subtitle">{{ copy.subtitle }}</text>
                </view>

                <view class="masthead-side">
                    <LocaleSwitch />
                    <view class="profile-panel">
                        <view class="profile-meta">
                            <text class="role-chip">{{ roleLabel }}</text>
                            <text class="user-name">{{ displayName }}</text>
                        </view>
                        <button class="btn-secondary logout-btn" @click="handleLogout">
                            {{ copy.logout }}
                        </button>
                    </view>
                </view>
            </view>

            <scroll-view class="nav-row fade-in-up" scroll-x>
                <view class="nav-items">
                    <view
                        v-for="item in visibleNavItems"
                        :key="item.path"
                        class="nav-item"
                        :class="{ active: isActive(item.path) }"
                        @click="handleNav(item)"
                    >
                        <text class="nav-label">{{ item.label }}</text>
                        <text v-if="item.showBadge && cartCount > 0" class="badge">
                            {{ cartCount }}
                        </text>
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
    gap: 20px;
}

.masthead {
    padding: 24px 28px;
    display: grid;
    grid-template-columns: minmax(0, 1.25fr) minmax(320px, 420px);
    gap: 24px;
    align-items: stretch;
}

.masthead-main {
    display: flex;
    flex-direction: column;
    gap: 16px;
    justify-content: center;
}

.brand-line {
    display: inline-flex;
    align-items: center;
    gap: 12px;
}

.brand-mark {
    width: 40px;
    height: 40px;
    border-radius: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.12em;
    color: #04111c;
    background: linear-gradient(135deg, var(--accent), var(--highlight));
    box-shadow: 0 12px 28px rgba(95, 209, 194, 0.2);
}

.brand-name {
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.18em;
    text-transform: uppercase;
    color: var(--text-muted);
}

.title {
    display: block;
    font-size: clamp(32px, 4vw, 50px);
    font-weight: 800;
    letter-spacing: -0.05em;
    line-height: 1;
}

.subtitle {
    color: var(--text-muted);
    font-size: 15px;
    line-height: 1.75;
    max-width: 620px;
}

.masthead-side {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 18px;
}

.profile-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 18px;
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--panel-border);
}

.profile-meta {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.role-chip {
    align-self: flex-start;
    font-size: 12px;
    border: 1px solid rgba(95, 209, 194, 0.24);
    border-radius: 999px;
    color: var(--text-main);
    padding: 7px 12px;
    background: var(--accent-soft);
    font-weight: 700;
}

.user-name {
    color: var(--text-main);
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.02em;
}

.logout-btn {
    min-width: 112px;
}

.nav-row {
    padding: 8px;
    border-radius: 999px;
    background: rgba(5, 14, 23, 0.82);
    border: 1px solid var(--panel-border);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
}

.nav-items {
    display: flex;
    gap: 10px;
    padding: 4px 0;
}

.nav-item {
    padding: 11px 16px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.03);
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    border: 1px solid transparent;
    color: var(--text-muted);
    font-weight: 700;
    transition:
        background-color 0.22s ease,
        border-color 0.22s ease,
        box-shadow 0.22s ease,
        transform 0.22s ease,
        color 0.22s ease;
}

.nav-item.active {
    background: linear-gradient(
        135deg,
        rgba(95, 209, 194, 0.18),
        rgba(240, 182, 90, 0.16)
    );
    border-color: rgba(95, 209, 194, 0.24);
    box-shadow: 0 14px 28px rgba(1, 7, 14, 0.34);
    color: var(--text-main);
}

.nav-label {
    white-space: nowrap;
}

.badge {
    background: linear-gradient(135deg, var(--highlight), #ffd480);
    color: #101923;
    font-size: 10px;
    font-weight: 800;
    padding: 3px 7px;
    border-radius: 999px;
}

.content {
    padding-bottom: 20px;
}

@media (hover: hover) {
    .nav-item:hover {
        transform: translateY(-1px);
        color: var(--text-main);
        border-color: var(--panel-border);
        box-shadow: 0 12px 24px rgba(2, 8, 16, 0.24);
    }
}

button {
    margin: 0;
    line-height: 1;
}

button::after {
    border: none;
}

@media (max-width: 960px) {
    .masthead {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .app-shell {
        padding-top: 12px;
    }

    .masthead {
        padding: 18px;
        gap: 18px;
    }

    .title {
        font-size: 30px;
    }

    .subtitle {
        font-size: 13px;
        line-height: 1.7;
    }

    .profile-panel {
        flex-direction: column;
        align-items: flex-start;
        padding: 14px;
    }

    .nav-row {
        padding: 6px;
    }

    .nav-item {
        padding: 10px 14px;
        font-size: 12px;
    }

    .logout-btn {
        width: 100%;
    }
}
</style>
