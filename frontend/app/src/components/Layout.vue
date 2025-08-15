<template>
  <div class="layout-container">
    <slot name="header">
      <header class="layout-header" v-if="showHeader">
        <slot name="header-content"></slot>
      </header>
    </slot>
    
    <div class="layout-main">
      <slot name="sidebar">
        <aside class="layout-sidebar" v-if="showSidebar">
          <slot name="sidebar-content"></slot>
        </aside>
      </slot>
      
      <main class="layout-content">
        <slot></slot>
      </main>
    </div>
    
    <slot name="footer">
      <footer class="layout-footer" v-if="showFooter">
        <slot name="footer-content"></slot>
      </footer>
    </slot>
  </div>
</template>

<script setup lang="ts">
interface Props {
  showHeader?: boolean
  showSidebar?: boolean
  showFooter?: boolean
}

withDefaults(defineProps<Props>(), {
  showHeader: true,
  showSidebar: true,
  showFooter: true
})
</script>

<style scoped>
.layout-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--background-color);
}

.layout-header {
  flex-shrink: 0;
  background-color: var(--card-background);
  box-shadow: 0 2px 8px var(--shadow-color);
  z-index: 100;
  border-bottom: 1px solid var(--border-color);
}

.layout-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.layout-sidebar {
  flex-shrink: 0;
  width: 220px;
  background-color: var(--card-background);
  overflow-y: auto;
  border-right: 1px solid var(--border-color);
  padding: 20px 0;
  box-shadow: 2px 0 8px var(--shadow-color);
  transition: all 0.3s ease;
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background-color: var(--background-color);
  transition: all 0.3s ease;
}

.layout-footer {
  flex-shrink: 0;
  background-color: var(--card-background);
  padding: 20px;
  text-align: center;
  border-top: 1px solid var(--border-color);
  box-shadow: 0 -2px 8px var(--shadow-color);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .layout-sidebar {
    width: 60px;
  }
  
  .layout-content {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .layout-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  .layout-sidebar.open {
    transform: translateX(0);
  }
  
  .layout-main {
    position: relative;
    z-index: 1;
  }
  
  .layout-content {
    padding: 16px;
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .layout-content {
    padding: 12px;
  }
}
</style>