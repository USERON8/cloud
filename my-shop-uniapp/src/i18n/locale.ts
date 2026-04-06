import { computed, readonly, ref } from "vue";
import { getStorage, setStorage } from "../utils/storage";

export type Locale = "zh-CN" | "en-US";

const STORAGE_KEY = "app.locale";
const DEFAULT_LOCALE: Locale = "zh-CN";
const SUPPORTED_LOCALES: Locale[] = ["zh-CN", "en-US"];

const localeState = ref<Locale>(DEFAULT_LOCALE);

function isLocale(value: unknown): value is Locale {
    return SUPPORTED_LOCALES.includes(value as Locale);
}

export function hydrateLocaleFromStorage(): void {
    const storedLocale = getStorage<Locale>(STORAGE_KEY);
    if (isLocale(storedLocale)) {
        localeState.value = storedLocale;
    }
}

export function setLocale(locale: Locale): void {
    localeState.value = locale;
    setStorage(STORAGE_KEY, locale);
}

export function toggleLocale(): void {
    setLocale(localeState.value === "zh-CN" ? "en-US" : "zh-CN");
}

export function useLocale() {
    return {
        locale: readonly(localeState),
        isChinese: computed(() => localeState.value === "zh-CN"),
        isEnglish: computed(() => localeState.value === "en-US"),
        setLocale,
        toggleLocale,
    };
}
