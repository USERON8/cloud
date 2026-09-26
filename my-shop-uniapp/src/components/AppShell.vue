<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { logout } from "../api/auth";
import { useRole, type UserRole } from "../auth/permission";
import { clearSession, isAuthenticated, sessionState } from "../auth/session";
import { type Locale, useLocale } from "../i18n/locale";
import {
    currentRoutePath,
    navigateTo,
    redirectTo,
    resolveRouteGuard,
} from "../router/navigation";
import { Routes, type RoutePath } from "../router/routes";
import { cartCount } from "../store/cart";
import LocaleSwitch from "./LocaleSwitch.vue";

const props = defineProps<{
    title?: string;
    routePath?: RoutePath;
    requiresAuth?: boolean;
    roles?: UserRole[];
}>();

interface NavItem {
    key: string;
    path: RoutePath;
    roles: UserRole[];
    public?: boolean;
    showBadge?: boolean;
}

const { role } = useRole();
const { locale } = useLocale();
const accessGranted = ref(false);

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
        public: true,
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
            brand: "ShopMall",
            defaultTitle: "Control Center",
            currentUser: "Current user",
            guestRole: "Guest",
            guestName: "Guest visitor",
            guestAction: "Sign in",
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
        brand: "ShopMall",
        defaultTitle: "控制中心",
        currentUser: "当前用户",
        guestRole: "访客",
        guestName: "未登录用户",
        guestAction: "登录",
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
        .filter(
            (item) =>
                item.public ||
                (isAuthenticated() && item.roles.includes(role.value)),
        )
        .map((item) => ({
            ...item,
            label: copy.value.nav[item.key as keyof typeof copy.value.nav],
        })),
);

const displayName = computed(
    () =>
        (!isAuthenticated() && copy.value.guestName) ||
        sessionState.user?.nickname ||
        sessionState.user?.username ||
        copy.value.currentUser,
);

const roleLabel = computed(() =>
    !isAuthenticated()
        ? copy.value.guestRole
        : roleTextMap[role.value]?.[locale.value] ?? role.value,
);

const publicPaths = new Set(
    navItems.filter((item) => item.public).map((item) => item.path),
);

function isActive(path: string): boolean {
    const current = currentRoutePath();
    if (!current) {
        return false;
    }
    return current === path.replace(/^\//, "") || current === path;
}

function ensureCurrentRouteAccess(): boolean {
    const currentPath = props.routePath || currentRoutePath();
    if (!currentPath) {
        return false;
    }

    const matchedItem = navItems.find((item) => item.path === currentPath);
    const routeGuard = resolveRouteGuard(currentPath);
    const requiresAuth =
        props.requiresAuth ??
        routeGuard?.requiresAuth ??
        (!matchedItem?.public && !publicPaths.has(currentPath as RoutePath));
    const requiredRoles = props.roles?.length
        ? props.roles
        : routeGuard?.roles?.length
          ? routeGuard.roles
          : matchedItem?.public
            ? []
            : (matchedItem?.roles ?? []);

    if (!requiresAuth) {
        return true;
    }

    if (!isAuthenticated()) {
        redirectTo(Routes.login, { redirect: currentPath });
        return false;
    }

    if (requiredRoles.length > 0 && !requiredRoles.includes(role.value)) {
        redirectTo(Routes.forbidden);
        return false;
    }
    return true;
}

function handleNav(item: NavItem): void {
    const guard = item.public
        ? undefined
        : { requiresAuth: true, roles: item.roles };
    navigateTo(item.path, undefined, guard);
}

function handleLogin(): void {
    redirectTo(Routes.login, { redirect: currentRoutePath() || Routes.appCatalog });
}

async function handleLogout(): Promise<void> {
    try {
        await logout();
    } catch {
        // keep local logout available even if the remote session has expired
    } finally {
        clearSession();
        uni.showToast({ title: copy.value.logoutSuccess, icon: "success" });
        redirectTo(Routes.login);
    }
}

function refreshCurrentRouteAccess(): void {
    accessGranted.value = ensureCurrentRouteAccess();
}

onMounted(() => {
    refreshCurrentRouteAccess();
    if (typeof window !== "undefined") {
        window.addEventListener("hashchange", refreshCurrentRouteAccess);
        window.setTimeout(refreshCurrentRouteAccess, 0);
    }
});

onBeforeUnmount(() => {
    if (typeof window !== "undefined") {
        window.removeEventListener("hashchange", refreshCurrentRouteAccess);
    }
});
</script>

<template>
    <view v-if="accessGranted" class="app-shell">
        <view class="page-container shell-inner">
            <view class="masthead">
                <view class="masthead-main">
                    <view class="brand-line">
                        <text class="brand-name">{{ copy.brand }}</text>
                    </view>
                    <text class="title">{{ props.title || copy.defaultTitle }}</text>
                </view>

                <view class="masthead-side">
                    <LocaleSwitch />
                    <view class="profile-panel">
                        <view class="profile-meta">
                            <text class="role-chip">{{ roleLabel }}</text>
                            <text class="user-name">{{ displayName }}</text>
                        </view>
                        <button
                            v-if="isAuthenticated()"
                            class="btn-secondary logout-btn"
                            @click="handleLogout"
                        >
                            {{ copy.logout }}
                        </button>
                        <button
                            v-else
                            class="btn-secondary logout-btn"
                            @click="handleLogin"
                        >
                            {{ copy.guestAction }}
                        </button>
                    </view>
                </view>
            </view>

            <scroll-view class="nav-row" scroll-x>
                <view class="nav-items">
                    <button
                        v-for="item in visibleNavItems"
                        :key="item.path"
                        class="nav-item"
                        :class="{ active: isActive(item.path) }"
                        :aria-current="isActive(item.path) ? 'page' : undefined"
                        @click="handleNav(item)"
                    >
                        <text class="nav-label">{{ item.label }}</text>
                        <text v-if="item.showBadge && cartCount > 0" class="badge">
                            {{ cartCount }}
                        </text>
                    </button>
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
    padding: 24px 0 44px;
    color: #15171a;
    background:
        linear-gradient(180deg, rgba(248, 249, 251, 0.96), #ffffff 44%),
        #ffffff;
    --bg-elevated: #ffffff;
    --panel-bg: #ffffff;
    --panel-solid: #ffffff;
    --panel-muted: #f8fafc;
    --panel-border: #edf0f4;
    --panel-border-strong: #dfe5ec;
    --text-main: #15181d;
    --text-muted: #69727d;
    --text-soft: #9aa2ad;
    --accent: #111316;
    --accent-strong: #111316;
    --accent-soft: #f5f7fa;
    --highlight: #ff3b30;
    --highlight-soft: rgba(255, 59, 48, 0.08);
    --success-soft: rgba(27, 127, 84, 0.1);
    --warning-soft: rgba(177, 108, 11, 0.11);
    --danger-soft: rgba(255, 59, 48, 0.1);
    --shadow-soft: 0 18px 60px rgba(20, 25, 35, 0.06);
    --shadow-card: 0 18px 58px rgba(18, 24, 35, 0.05);
    --shadow-float: 0 24px 70px rgba(18, 24, 35, 0.08);
    --radius-xl: 8px;
    --radius-lg: 8px;
    --radius-md: 6px;
    --radius-sm: 4px;
}

.shell-inner {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.masthead {
    min-height: 64px;
    padding: 0 20px;
    display: grid;
    grid-template-columns: minmax(220px, 1fr) auto;
    gap: 18px;
    align-items: center;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.96);
    border: 1px solid #edf0f4;
    box-shadow: 0 18px 60px rgba(20, 25, 35, 0.06);
}

.masthead-main {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 18px;
    min-width: 0;
}

.brand-name {
    font-size: 20px;
    font-weight: 800;
    letter-spacing: 0;
    color: #111316;
    white-space: nowrap;
}

.title {
    min-width: 0;
    color: #15181d;
    font-size: 16px;
    font-weight: 800;
    line-height: 1.2;
    overflow-wrap: anywhere;
}

.masthead-side {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-end;
    gap: 14px;
}

.masthead-side :deep(.locale-switch) {
    gap: 0;
}

.masthead-side :deep(.switch-copy) {
    display: none;
}

.masthead-side :deep(.switch-segment) {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 2px;
    border-color: #e8ebef;
    border-radius: 4px;
    background: #fbfcfd;
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
}

.masthead-side :deep(.switch-option) {
    min-width: 34px;
    min-height: 28px;
    padding: 0 8px;
    border-radius: 3px;
    color: #6f7781;
    box-shadow: none;
}

.masthead-side :deep(.switch-option.active) {
    border-color: #111316;
    background: #111316;
    color: #ffffff;
}

.masthead-side :deep(.switch-long) {
    display: none;
}

.masthead-side :deep(.switch-short) {
    font-size: 11px;
    letter-spacing: 0;
}

.profile-panel {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    padding: 0;
}

.profile-meta {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 8px;
}

.role-chip {
    min-height: 26px;
    display: inline-flex;
    align-items: center;
    font-size: 11px;
    border: 1px solid #e7ebf0;
    border-radius: 4px;
    color: #69727d;
    padding: 0 8px;
    background: #fbfcfd;
    font-weight: 700;
}

.user-name {
    color: #25282d;
    font-size: 12px;
    font-weight: 700;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.logout-btn {
    min-width: 76px;
    min-height: 32px;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 12px;
}

.nav-row {
    padding: 0;
    border-radius: 8px;
    background: #ffffff;
    border: 1px solid #edf0f4;
    box-shadow: 0 18px 58px rgba(18, 24, 35, 0.04);
}

.nav-items {
    display: flex;
    gap: 0;
    padding: 0 10px;
}

.nav-item {
    appearance: none;
    position: relative;
    padding: 0 16px;
    border-radius: 0;
    background: transparent;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 48px;
    border: 0;
    color: #4c535d;
    font-weight: 700;
    white-space: nowrap;
    flex-shrink: 0;
}

.nav-item.active {
    color: #111316;
}

.nav-item.active::after {
    content: "";
    position: absolute;
    left: 16px;
    right: 16px;
    bottom: 8px;
    height: 2px;
    border-radius: 999px;
    background: #111316;
}

.badge {
    background: #ff3b30;
    color: #ffffff;
    font-size: 10px;
    font-weight: 800;
    padding: 3px 7px;
    border-radius: 999px;
}

.content {
    padding-bottom: 20px;
}

.app-shell :deep(.glass-card),
.app-shell :deep(.surface-card),
.app-shell :deep(.surface-muted),
.app-shell :deep(.display-panel),
.app-shell :deep(.metric-card),
.app-shell :deep(.info-card),
.app-shell :deep(.summary-item),
.app-shell :deep(.detail-item),
.app-shell :deep(.readiness-card) {
    background: #ffffff;
    border-color: #edf0f4;
    border-radius: 8px;
    color: #15181d;
    box-shadow: 0 18px 58px rgba(18, 24, 35, 0.05);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
}

.app-shell :deep(.display-panel) {
    background:
        radial-gradient(
            circle at 84% 48%,
            rgba(213, 222, 236, 0.52),
            transparent 32%
        ),
        linear-gradient(135deg, #f7f9fd 0%, #eef3fa 48%, #f9fbfe 100%);
}

.app-shell :deep(.hero-eyebrow) {
    color: #69727d;
    letter-spacing: 0.08em;
}

.app-shell :deep(.hero-title) {
    color: #101216;
    letter-spacing: 0;
}

.app-shell :deep(.hero-subtitle),
.app-shell :deep(.section-subtitle),
.app-shell :deep(.text-muted),
.app-shell :deep(.summary-subvalue),
.app-shell :deep(.meta-chip),
.app-shell :deep(.metric-label),
.app-shell :deep(.info-label) {
    color: #69727d;
}

.app-shell :deep(.section-title),
.app-shell :deep(.info-value),
.app-shell :deep(.metric-value),
.app-shell :deep(.summary-value),
.app-shell :deep(.readiness-value) {
    color: #15181d;
}

.app-shell :deep(.field-control),
.app-shell :deep(input),
.app-shell :deep(textarea) {
    background: #fbfcfd;
    border-color: #e6e9ee;
    color: #16191d;
}

.app-shell :deep(.btn-primary) {
    border-radius: 4px;
    background: #111316;
    color: #ffffff;
    border: 1px solid #111316;
    box-shadow: none;
}

.app-shell :deep(.btn-outline),
.app-shell :deep(.btn-secondary) {
    border-radius: 4px;
    background: #ffffff;
    border: 1px solid #d9dee6;
    color: #1d2025;
    box-shadow: none;
}

.app-shell :deep(.meta-chip.status-success) {
    background: rgba(27, 127, 84, 0.09);
    border-color: rgba(27, 127, 84, 0.18);
    color: #1b7f54;
}

.app-shell :deep(.meta-chip.status-warning) {
    background: rgba(177, 108, 11, 0.1);
    border-color: rgba(177, 108, 11, 0.18);
    color: #9a5f0b;
}

.app-shell :deep(.meta-chip.status-danger) {
    background: rgba(255, 59, 48, 0.1);
    border-color: rgba(255, 59, 48, 0.18);
    color: #d92d20;
}

.app-shell :deep(.meta-chip.status-accent) {
    background: #f5f7fa;
    border-color: #e7ebf0;
    color: #15181d;
}

button {
    margin: 0;
    line-height: 1;
}

button::after {
    border: none;
}

@media (hover: hover) {
    .nav-item:hover {
        color: #ff3b30;
    }
}

@media (max-width: 960px) {
    .masthead {
        grid-template-columns: 1fr;
        padding: 14px;
        align-items: stretch;
    }

    .masthead-side {
        justify-content: flex-start;
        flex-wrap: wrap;
    }
}

@media (max-width: 768px) {
    .app-shell {
        padding-top: 12px;
    }

    .masthead {
        gap: 12px;
    }

    .title {
        font-size: 14px;
    }

    .profile-panel {
        align-items: center;
        flex-wrap: wrap;
    }

    .nav-row {
        overflow: visible;
    }

    .nav-items {
        flex-wrap: wrap;
        padding: 6px;
    }

    .nav-item {
        min-height: 38px;
        padding: 0 10px;
        flex: 1 1 calc(25% - 1px);
        font-size: 12px;
        white-space: normal;
    }

    .nav-item.active::after {
        left: 10px;
        right: 10px;
        bottom: 4px;
    }

    .logout-btn {
        width: 100%;
    }
}
</style>
