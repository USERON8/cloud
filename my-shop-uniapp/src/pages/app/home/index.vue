<script setup lang="ts">
import { computed } from "vue";
import { sessionState } from "../../../auth/session";
import AppShell from "../../../components/AppShell.vue";
import { useLocale } from "../../../i18n/locale";
import { navigateTo } from "../../../router/navigation";
import { Routes } from "../../../router/routes";

const { locale } = useLocale();

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
                  {
                      eyebrow: "Checkout prep",
                      title: "Move cart and address work into one lane.",
                      body:
                          "Review items, verify shipping data, and push through checkout with fewer context switches.",
                      action: "Go to cart",
                      route: Routes.appCart,
                  },
              ],
              panelTitle: "Quick launch",
              panelSubtitle:
                  "Use the entry points below when you want to move directly into a focused task.",
              quickLinks: [
                  {
                      title: "Payments",
                      body: "Track payment status and reopen checkout when needed.",
                      action: "Open payments",
                      route: Routes.appPayments,
                  },
                  {
                      title: "Address book",
                      body: "Keep delivery details ready for a faster checkout path.",
                      action: "Open addresses",
                      route: Routes.appAddresses,
                  },
                  {
                      title: "Profile",
                      body: "Review account data and your current session identity.",
                      action: "Open profile",
                      route: Routes.appProfile,
                  },
              ],
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
                  "围绕经营信号而不是堆叠信息重新组织首页，让你从发现商品到跟进订单始终停留在一条更短的动作链路里。",
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
                      detail: "商品、支付、订单与个人入口现在使用统一视觉层次。",
                  },
                  {
                      label: "操作节奏",
                      value: "更短经营闭环",
                      detail: "把浏览、决策和执行放在更顺手的路径上。",
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
                  {
                      eyebrow: "结算准备",
                      title: "购物车和地址维护收敛到同一条工作线。",
                      body:
                          "更快核对商品、收货信息与结算前置条件，减少上下文切换。",
                      action: "进入购物车",
                      route: Routes.appCart,
                  },
              ],
              panelTitle: "快速入口",
              panelSubtitle:
                  "需要直接进入某项动作时，可以从下面这些入口开始。",
              quickLinks: [
                  {
                      title: "支付",
                      body: "查看支付状态，并在需要时重新拉起收银台。",
                      action: "打开支付页",
                      route: Routes.appPayments,
                  },
                  {
                      title: "地址簿",
                      body: "提前维护收货信息，让下单链路更顺畅。",
                      action: "打开地址簿",
                      route: Routes.appAddresses,
                  },
                  {
                      title: "我的",
                      body: "查看账号信息与当前登录会话状态。",
                      action: "打开个人页",
                      route: Routes.appProfile,
                  },
              ],
          },
);
</script>

<template>
    <AppShell :title="copy.pageTitle">
        <view class="dashboard-layout">
            <view class="hero-grid display-panel fade-in-up">
                <view class="hero-copy">
                    <text class="hero-eyebrow">{{ copy.eyebrow }}</text>
                    <text class="hero-title">{{ copy.title }}</text>
                    <text class="hero-subtitle">{{ copy.subtitle }}</text>

                    <view class="hero-actions">
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

                <view class="hero-stack">
                    <view
                        v-for="card in copy.heroCards"
                        :key="card.label"
                        class="hero-card info-card"
                    >
                        <text class="info-label">{{ card.label }}</text>
                        <text class="info-value">{{ card.value }}</text>
                        <text class="hero-card-copy">{{ card.detail }}</text>
                    </view>
                </view>
            </view>

            <view class="feature-grid fade-in-up">
                <view
                    v-for="section in copy.sections"
                    :key="section.title"
                    class="feature-card surface-card"
                >
                    <text class="feature-kicker">{{ section.eyebrow }}</text>
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

            <view class="section-block editorial-section fade-in-up">
                <view class="section-head">
                    <text class="section-title">{{ copy.panelTitle }}</text>
                    <text class="section-subtitle">{{ copy.panelSubtitle }}</text>
                </view>

                <view class="quick-grid">
                    <view
                        v-for="link in copy.quickLinks"
                        :key="link.title"
                        class="quick-card surface-card"
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
    </AppShell>
</template>

<style scoped>
.dashboard-layout {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.hero-grid {
    padding: 34px;
    display: grid;
    grid-template-columns: minmax(0, 1.3fr) minmax(320px, 360px);
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

.hero-stack {
    display: grid;
    grid-template-columns: 1fr;
    gap: 14px;
}

.hero-card {
    min-height: 138px;
}

.hero-card-copy {
    margin-top: 12px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--text-muted);
}

.feature-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
}

.feature-card {
    padding: 26px;
    display: flex;
    flex-direction: column;
    gap: 14px;
    min-height: 280px;
    justify-content: flex-end;
    transition:
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease;
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
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 220px;
    justify-content: flex-end;
    transition:
        transform 0.22s ease,
        box-shadow 0.22s ease,
        border-color 0.22s ease;
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

@media (hover: hover) {
    .feature-card:hover,
    .quick-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 18px 36px rgba(1, 7, 14, 0.34);
        border-color: var(--panel-border-strong);
    }
}

@media (max-width: 980px) {
    .hero-grid,
    .feature-grid,
    .quick-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .hero-grid {
        padding: 24px;
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
