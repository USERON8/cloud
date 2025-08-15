<template>
  <div class="page-header">
    <div class="page-header-content">
      <div class="page-header-main">
        <el-breadcrumb v-if="breadcrumb && breadcrumb.length > 0" class="page-breadcrumb">
          <el-breadcrumb-item 
            v-for="(item, index) in breadcrumb" 
            :key="index"
            :to="item.to"
          >
            {{ item.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <div class="page-title-wrapper">
          <h1 class="page-title">{{ title }}</h1>
          <p v-if="description" class="page-description">{{ description }}</p>
        </div>
      </div>
      
      <div class="page-header-extra">
        <slot name="extra"></slot>
      </div>
    </div>
    
    <div v-if="$slots.default" class="page-header-sub">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface BreadcrumbItem {
  title: string
  to?: string | Record<string, any>
}

interface Props {
  title: string
  description?: string
  breadcrumb?: BreadcrumbItem[]
}

withDefaults(defineProps<Props>(), {
  breadcrumb: () => []
})
</script>

<style scoped>
.page-header {
  background-color: var(--card-background);
  padding: 20px 24px;
  margin-bottom: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 var(--shadow-color);
  border: 1px solid var(--border-color);
}

.page-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.page-breadcrumb {
  margin-bottom: 12px;
}

.page-title-wrapper {
  flex: 1;
  min-width: 0;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--primary-text-color);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-description {
  margin: 0;
  font-size: 14px;
  color: var(--secondary-text-color);
  line-height: 1.5;
}

.page-header-extra {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header-sub {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 16px 20px;
    margin-bottom: 20px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
  .page-header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .page-header-extra {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 480px) {
  .page-header {
    padding: 12px 16px;
    margin-bottom: 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
}
</style>