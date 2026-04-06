<script setup lang="ts">
import { computed } from "vue";
import { type Locale, useLocale } from "../i18n/locale";

withDefaults(
    defineProps<{
        align?: "start" | "end";
    }>(),
    {
        align: "end",
    },
);

const { locale, setLocale } = useLocale();

const copy = computed(() =>
    locale.value === "en-US"
        ? {
              label: "Language",
              caption: "Switch UI copy",
          }
        : {
              label: "语言",
              caption: "切换界面文案",
          },
);

const options: Array<{ value: Locale; short: string; long: string }> = [
    {
        value: "zh-CN",
        short: "中",
        long: "中文",
    },
    {
        value: "en-US",
        short: "EN",
        long: "English",
    },
];

function handleSelect(value: Locale): void {
    if (locale.value === value) {
        return;
    }
    setLocale(value);
}
</script>

<template>
    <view class="locale-switch" :class="`align-${align}`">
        <view class="switch-copy">
            <text class="switch-label">{{ copy.label }}</text>
            <text class="switch-caption">{{ copy.caption }}</text>
        </view>
        <view class="switch-segment">
            <button
                v-for="option in options"
                :key="option.value"
                class="switch-option"
                :class="{ active: locale === option.value }"
                @click="handleSelect(option.value)"
            >
                <text class="switch-short">{{ option.short }}</text>
                <text class="switch-long">{{ option.long }}</text>
            </button>
        </view>
    </view>
</template>

<style scoped>
.locale-switch {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-wrap: wrap;
}

.align-end {
    justify-content: flex-end;
}

.align-start {
    justify-content: flex-start;
}

.switch-copy {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.switch-label {
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--text-main);
}

.switch-caption {
    font-size: 12px;
    color: var(--text-soft);
}

.switch-segment {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px;
    border-radius: 999px;
    border: 1px solid var(--panel-border-strong);
    background: rgba(6, 15, 25, 0.66);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
}

.switch-option {
    min-width: 94px;
    min-height: 46px;
    padding: 10px 14px;
    border-radius: 999px;
    border: 1px solid transparent;
    background: transparent;
    color: var(--text-soft);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition:
        background-color 0.22s ease,
        border-color 0.22s ease,
        color 0.22s ease,
        transform 0.22s ease,
        box-shadow 0.22s ease;
}

.switch-option.active {
    background: linear-gradient(
        135deg,
        rgba(95, 209, 194, 0.22),
        rgba(240, 182, 90, 0.2)
    );
    color: var(--text-main);
    border-color: rgba(95, 209, 194, 0.28);
    box-shadow: 0 12px 26px rgba(4, 10, 18, 0.28);
}

.switch-short {
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.04em;
}

.switch-long {
    font-size: 12px;
    font-weight: 600;
}

button {
    margin: 0;
    line-height: 1;
}

button::after {
    border: none;
}

@media (hover: hover) {
    .switch-option:hover {
        transform: translateY(-1px);
        color: var(--text-main);
    }
}

@media (max-width: 640px) {
    .locale-switch {
        width: 100%;
    }

    .switch-segment {
        width: 100%;
    }

    .switch-option {
        flex: 1 1 0;
        min-width: 0;
    }
}
</style>
