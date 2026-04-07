<script setup lang="ts">
import { computed } from "vue";
import { useRole } from "../../../auth/permission";
import { sessionState } from "../../../auth/session";
import AppShell from "../../../components/AppShell.vue";
import { useLocale } from "../../../i18n/locale";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";

const { locale } = useLocale();
const { isMerchant } = useRole();

const quickLinks = computed(() => {
    const baseLinks = [
        {
            title: locale.value === "en-US" ? "Cart" : "购物车",
            body:
                locale.value === "en-US"
                    ? "Review current items and move into checkout without leaving the workspace flow."
                    : "查看当前待结算商品，让下单动作保持在连续链路里。",
            action: locale.value === "en-US" ? "Open cart" : "打开购物车",
            route: Routes.appCart,
        },
        {
            title: locale.value === "en-US" ? "Address book" : "地址簿",
            body:
                locale.value === "en-US"
                    ? "Keep delivery details ready for a faster checkout path."
                    : "提前维护收货信息，让下单链路更顺畅。",
            action:
                locale.value === "en-US" ? "Open addresses" : "打开地址簿",
            route: Routes.appAddresses,
        },
        {
            title: locale.value === "en-US" ? "Profile" : "我的",
            body:
                locale.value === "en-US"
                    ? "Review account data and your current session identity."
                    : "查看账号信息与当前登录会话状态。",
            action: locale.value === "en-US" ? "Open profile" : "打开个人页",
            route: Routes.appProfile,
        },
    ];

    if (isMerchant.value) {
        return baseLinks;
    }

    return [
        baseLinks[0],
        {
            title: locale.value === "en-US" ? "Payments" : "支付",
            body:
                locale.value === "en-US"
                    ? "Track payment status and reopen checkout when needed."
                    : "查看支付状态，并在需要时重新拉起收银台。",
            action:
                locale.value === "en-US" ? "Open payments" : "打开支付页",
            route: Routes.appPayments,
        },
        baseLinks[1],
        baseLinks[2],
    ];
});

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              pageTitle: "Home",
              eyebrow: "Overview",
              title: `Welcome back, ${
                  sessionState.user?.nickname ||
                  sessionState.user?.username ||
                  "operator"
              }`,
              subtitle:
                  "A cloud commerce workspace arranged around signal, not clutter. Move from discovery to order follow-up in one continuous flow.",
              primaryAction: "Browse catalog",
              secondaryAction: "Open orders",
              heroCards: [
                  {
                      label: "Identity",
                      value:
                          sessionState.user?.nickname ||
                          sessionState.user?.username ||
                          "Current user",
                      detail: "Your active account for this session.",
                  },
                  {
                      label: "Workspace",
                      value: "Unified cloud console",
                      detail:
                          "Product, payment, order, and profile access are now visually aligned.",
                  },
                  {
                      label: "Rhythm",
                      value: "Fast operational loop",
                      detail:
                          "Focus on a shorter path from browsing to execution.",
                  },
              ],
              sections: [
                  {
                      eyebrow: "Catalog flow",
                      title: "Reach products without visual noise.",
                      body:
                          "The new composition puts search, offer review, and quick actions closer together.",
                      action: "Go to catalog",
                      route: Routes.appCatalog,
                  },
                  {
                      eyebrow: "Order tracking",
                      title: "Keep payment and delivery signals in view.",
                      body:
                          "Use the order board to stay close to payment status, shipment state, and after-sale actions.",
                      action: "Go to orders",
                      route: Routes.appOrders,
                  },
              ],
              panelTitle: "Quick launch",
              panelSubtitle:
                  "Use the entry points below when you want to move directly into a focused task.",
              flowTitle: "Core flow",
              quickSubtitle: "Move straight into a focused task.",
              focusTitle: "Workspace tone",
          }
        : {
              pageTitle: "首页",
              eyebrow: "总览",
              title: `欢迎回来，${
                  sessionState.user?.nickname ||
                  sessionState.user?.username ||
                  "当前用户"
              }`,
              subtitle:
                  "围绕经营信号而不是信息堆叠重新组织首页，让你从发现商品到跟进订单始终停留在一条更短的操作链路里。",
              primaryAction: "浏览商品",
              secondaryAction: "查看订单",
              heroCards: [
                  {
                      label: "当前身份",
                      value:
                          sessionState.user?.nickname ||
                          sessionState.user?.username ||
                          "当前用户",
                      detail: "当前会话使用中的账号身份。",
                  },
                  {
                      label: "工作区域",
                      value: "统一云端控制台",
                      detail:
                          "商品、支付、订单与个人入口现在使用统一视觉层次。",
                  },
                  {
                      label: "操作节奏",
                      value: "更短经营闭环",
                      detail: "把浏览、决策和执行放在更顺手的路径中。",
                  },
              ],
              sections: [
                  {
                      eyebrow: "商品流转",
                      title: "先看商品，再做动作，不被界面噪音打断。",
                      body:
                          "新的结构把搜索、选品和常用操作收得更近，减少重复跳转。",
                      action: "进入商品页",
                      route: Routes.appCatalog,
                  },
                  {
                      eyebrow: "订单跟进",
                      title: "把支付、履约和售后状态始终放在视野内。",
                      body:
                          "订单面板更适合连续跟进付款状态、配送进度和售后动作。",
                      action: "进入订单页",
                      route: Routes.appOrders,
                  },
              ],
              panelTitle: "快速入口",
              panelSubtitle:
                  "需要直接进入某项动作时，可以从下面这些入口开始。",
              flowTitle: "核心流程",
              quickSubtitle: "直接进入单一任务，不被多余信息打断。",
              focusTitle: "当前工作节奏",
          },
);
</script>

<template>
    <AppShell :title="copy.pageTitle">
        <view class="dashboard-layout">
            <view class="display-panel dashboard-hero fade-in-up">
                <view class="dashboard-hero-copy">
                    <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
                    <text class="hero-title">{{ copy.title }}</text>
                    <text class="hero-subtitle">{{ copy.subtitle }}</text>

                    <view class="action-wrap hero-actions">
                        <button
                            class="btn-primary"
                            @click="
                                navigateTo(Routes.appCatalog, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            {{ copy.primaryAction }}
                        </button>
                        <button
                            class="btn-outline"
                            @click="
                                navigateTo(Routes.appOrders, undefined, {
                                    requiresAuth: true,
                                })
                            "
                        >
                            {{ copy.secondaryAction }}
                        </button>
                    </view>
                </view>

                <view class="dashboard-hero-stats">
                    <view
                        v-for="card in copy.heroCards"
                        :key="card.label"
                        class="info-card hero-card"
                    >
                        <text class="info-label">{{ card.label }}</text>
                        <text class="info-value">{{ card.value }}</text>
                        <text class="hero-card-copy">{{ card.detail }}</text>
                    </view>
                </view>
            </view>

            <view class="dashboard-grid-main fade-in-up">
                <view class="surface-card panel-block workflow-panel">
                    <view class="section-head">
                        <text class="section-title">{{ copy.flowTitle }}</text>
                        <text class="section-subtitle">
                            {{ copy.panelSubtitle }}
                        </text>
                    </view>

                    <view class="workflow-grid">
                        <view
                            v-for="section in copy.sections"
                            :key="section.title"
                            class="surface-muted panel-block panel-hover workflow-card"
                        >
                            <text class="feature-kicker">{{
                                section.eyebrow
                            }}</text>
                            <text class="feature-title">{{ section.title }}</text>
                            <text class="feature-copy">{{ section.body }}</text>
                            <button
                                class="btn-outline feature-button"
                                @click="
                                    navigateTo(section.route, undefined, {
                                        requiresAuth: true,
                                    })
                                "
                            >
                                {{ section.action }}
                            </button>
                        </view>
                    </view>
                </view>

                <view class="surface-card panel-block quick-panel">
                    <view class="section-head">
                        <text class="section-title">{{ copy.panelTitle }}</text>
                        <text class="section-subtitle">
                            {{ copy.quickSubtitle }}
                        </text>
                    </view>

                    <view class="quick-grid">
                        <view
                            v-for="link in quickLinks"
                            :key="link.title"
                            class="surface-muted panel-block panel-hover quick-card"
                        >
                            <text class="quick-title">{{ link.title }}</text>
                            <text class="quick-copy">{{ link.body }}</text>
                            <button
                                class="btn-secondary quick-button"
                                @click="
                                    navigateTo(link.route, undefined, {
                                        requiresAuth: true,
                                    })
                                "
                            >
                                {{ link.action }}
                            </button>
                        </view>
                    </view>
                </view>
            </view>

            <view class="surface-card panel-block focus-strip fade-in-up">
                <view class="section-head">
                    <text class="section-title">{{ copy.focusTitle }}</text>
                    <text class="section-subtitle">
                        {{ copy.panelSubtitle }}
                    </text>
                </view>

                <view class="focus-grid">
                    <view
                        v-for="card in copy.heroCards"
                        :key="card.label"
                        class="surface-muted focus-card"
                    >
                        <text class="focus-label">{{ card.label }}</text>
                        <text class="focus-detail">{{ card.detail }}</text>
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

.hero-actions {
    padding-top: 8px;
}

.hero-card {
    min-height: 120px;
}

.hero-card-copy {
    margin-top: 12px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--text-muted);
}

.workflow-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.workflow-card {
    min-height: 260px;
    justify-content: flex-end;
}

.feature-kicker {
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    color: var(--accent);
    font-weight: 800;
}

.feature-title {
    font-size: 28px;
    line-height: 1.12;
    font-weight: 800;
    letter-spacing: -0.04em;
}

.feature-copy {
    font-size: 14px;
    line-height: 1.8;
    color: var(--text-muted);
}

.feature-button {
    align-self: flex-start;
}

.section-head {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.quick-grid {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.quick-card {
    gap: 10px;
}

.quick-title {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: -0.03em;
}

.quick-copy {
    color: var(--text-muted);
    font-size: 14px;
    line-height: 1.8;
}

.quick-button {
    align-self: flex-start;
}

.focus-strip {
    gap: 16px;
}

.focus-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
}

.focus-card {
    min-height: 120px;
    padding: 18px;
    border-radius: var(--radius-md);
}

.focus-label {
    font-size: 13px;
    font-weight: 700;
    color: var(--text-main);
    letter-spacing: -0.02em;
}

.focus-detail {
    margin-top: 10px;
    font-size: 13px;
    line-height: 1.75;
    color: var(--text-muted);
}

@media (max-width: 980px) {
    .workflow-grid,
    .focus-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .workflow-card,
    .focus-card {
        min-height: auto;
    }
}
</style>
