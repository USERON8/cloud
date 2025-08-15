<template>
  <div class="card" :class="{ 'card-shadow': shadow }">
    <div class="card-header" v-if="showHeader">
      <slot name="header">
        <div class="card-header-content">
          <div class="card-header-main">
            <h3 v-if="title" class="card-title">{{ title }}</h3>
            <p v-if="description" class="card-description">{{ description }}</p>
          </div>
          <div class="card-extra">
            <slot name="extra"></slot>
          </div>
        </div>
      </slot>
    </div>
    
    <div class="card-body">
      <slot></slot>
    </div>
    
    <div class="card-footer" v-if="showFooter">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
  showHeader?: boolean
  showFooter?: boolean
  shadow?: boolean
}

withDefaults(defineProps<Props>(), {
  showHeader: true,
  showFooter: false,
  shadow: true
})
</script>

<style scoped>
.card {
  background-color: var(--card-background);
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 var(--shadow-color);
}

.card-shadow {
  box-shadow: 0 2px 12px 0 var(--shadow-color);
}

.card-header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-color);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.card-header-main {
  flex: 1;
  min-width: 0;
}

.card-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-text-color);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-description {
  margin: 0;
  font-size: 13px;
  color: var(--secondary-text-color);
  line-height: 1.5;
}

.card-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-body {
  padding: 24px;
}

.card-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background-color: rgba(0, 0, 0, 0.02);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-header {
    padding: 16px 20px 12px;
  }
  
  .card-body {
    padding: 20px;
  }
  
  .card-footer {
    padding: 12px 20px;
  }
  
  .card-title {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .card-header {
    padding: 12px 16px 8px;
  }
  
  .card-body {
    padding: 16px;
  }
  
  .card-footer {
    padding: 12px 16px;
  }
  
  .card-title {
    font-size: 15px;
  }
}
</style>