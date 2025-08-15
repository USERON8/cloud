<template>
  <div class="table-container">
    <div class="table-toolbar" v-if="showToolbar">
      <slot name="toolbar">
        <div class="toolbar-actions">
          <el-button 
            v-for="(button, index) in toolbarButtons" 
            :key="index"
            :type="button.type"
            :icon="button.icon"
            @click="handleToolbarClick(button.action)"
            size="small"
          >
            {{ button.text }}
          </el-button>
        </div>
      </slot>
    </div>
    
    <el-table
      ref="tableRef"
      :data="data"
      :border="border"
      :stripe="stripe"
      :loading="loading"
      :height="height"
      :max-height="maxHeight"
      @selection-change="handleSelectionChange"
      v-bind="$attrs"
      class="custom-table"
    >
      <el-table-column 
        v-if="showSelection" 
        type="selection" 
        width="55"
      />
      
      <el-table-column 
        v-if="showIndex" 
        type="index" 
        :label="indexLabel" 
        width="60" 
      />
      
      <slot>
        <el-table-column
          v-for="column in columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
          :fixed="column.fixed"
          :align="column.align || 'left'"
          :header-align="column.headerAlign"
          :sortable="column.sortable"
          v-bind="column.attrs"
        >
          <template #default="scope" v-if="column.slotName">
            <slot 
              :name="column.slotName" 
              :row="scope.row"
              :column="scope.column"
              :index="scope.$index"
            />
          </template>
        </el-table-column>
      </slot>
    </el-table>
    
    <div class="table-pagination" v-if="showPagination && pagination">
      <slot name="pagination">
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="pagination.pageSizes || [10, 20, 50, 100]"
            :total="pagination.total"
            :layout="pagination.layout || 'total, sizes, prev, pager, next, jumper'"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            background
          />
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { TableInstance } from 'element-plus'

interface Column {
  prop: string
  label?: string
  width?: string | number
  minWidth?: string | number
  fixed?: boolean | 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  headerAlign?: 'left' | 'center' | 'right'
  sortable?: boolean
  slotName?: string
  attrs?: Record<string, any>
}

interface Pagination {
  currentPage: number
  pageSize: number
  total: number
  pageSizes?: number[]
  layout?: string
}

interface ToolbarButton {
  text: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  icon?: any
  action: string
}

interface Props {
  data: any[]
  columns?: Column[]
  loading?: boolean
  border?: boolean
  stripe?: boolean
  height?: string | number
  maxHeight?: string | number
  showSelection?: boolean
  showIndex?: boolean
  indexLabel?: string
  showToolbar?: boolean
  toolbarButtons?: ToolbarButton[]
  showPagination?: boolean
  pagination?: Pagination
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  columns: () => [],
  loading: false,
  border: true,
  stripe: true,
  showSelection: false,
  showIndex: false,
  indexLabel: '序号',
  showToolbar: false,
  toolbarButtons: () => [],
  showPagination: false
})

const emit = defineEmits<{
  (e: 'toolbar-click', action: string): void
  (e: 'selection-change', selection: any[]): void
  (e: 'size-change', size: number): void
  (e: 'current-change', current: number): void
}>()

const tableRef = ref<TableInstance>()

const handleToolbarClick = (action: string) => {
  emit('toolbar-click', action)
}

const handleSelectionChange = (selection: any[]) => {
  emit('selection-change', selection)
}

const handleSizeChange = (size: number) => {
  emit('size-change', size)
}

const handleCurrentChange = (current: number) => {
  emit('current-change', current)
}

defineExpose({
  tableRef
})
</script>

<style scoped>
.table-container {
  background-color: var(--card-background);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 var(--shadow-color);
  border: 1px solid var(--border-color);
}

.table-toolbar {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background-color: rgba(0, 0, 0, 0.02);
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.custom-table {
  width: 100%;
}

.table-pagination {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background-color: rgba(0, 0, 0, 0.02);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .table-toolbar {
    padding: 12px 16px;
  }
  
  .toolbar-actions {
    gap: 8px;
  }
  
  .table-pagination {
    padding: 12px 16px;
  }
}

@media (max-width: 480px) {
  .pagination-wrapper {
    justify-content: center;
  }
  
  :deep(.el-pagination) .el-pagination__sizes {
    display: none;
  }
  
  :deep(.el-pagination) .el-pagination__jump {
    display: none;
  }
}
</style>