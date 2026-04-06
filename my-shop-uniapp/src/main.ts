import { createSSRApp } from "vue";
import App from "./App.vue";
import { hydrateSessionFromStorage } from "./auth/session";
import { hydrateLocaleFromStorage } from "./i18n/locale";
import { hydrateCartFromStorage } from "./store/cart";
import { pinia } from "./stores/pinia";

export function createApp() {
    const app = createSSRApp(App);
    app.use(pinia);
    hydrateSessionFromStorage();
    hydrateCartFromStorage();
    hydrateLocaleFromStorage();
    return { app };
}
