declare module 'vue-lazyload' {
  const plugin: {
    install: (app: import('vue').App, options?: Record<string, unknown>) => void
  }
  export default plugin
}
