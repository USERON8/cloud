<template>
  <el-form
    ref="formRef"
    :model="model"
    :rules="rules"
    :label-width="labelWidth"
    :inline="inline"
    v-bind="$attrs"
    @submit.prevent="handleSubmit"
    class="custom-form"
  >
    <el-row :gutter="20">
      <template v-for="(field, index) in fields" :key="index">
        <el-col 
          v-if="!field.hidden" 
          :span="field.span || 24"
          :md="field.md || field.span || 24"
          :sm="field.sm || 24"
          :xs="24"
        >
          <el-form-item :label="field.label" :prop="field.prop">
            <!-- 输入框 -->
            <el-input
              v-if="field.type === 'input'"
              v-model="model[field.prop]"
              :type="field.inputType || 'text'"
              :placeholder="field.placeholder"
              :clearable="field.clearable !== false"
              :show-password="field.showPassword"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            />
            
            <!-- 文本域 -->
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="model[field.prop]"
              type="textarea"
              :placeholder="field.placeholder"
              :rows="field.rows || 3"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            />
            
            <!-- 数字输入框 -->
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="model[field.prop]"
              :min="field.min"
              :max="field.max"
              :step="field.step"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            />
            
            <!-- 选择框 -->
            <el-select
              v-else-if="field.type === 'select'"
              v-model="model[field.prop]"
              :placeholder="field.placeholder"
              :clearable="field.clearable !== false"
              :multiple="field.multiple"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            >
              <el-option
                v-for="option in field.options"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            
            <!-- 日期选择器 -->
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="model[field.prop]"
              type="date"
              :placeholder="field.placeholder"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            />
            
            <!-- 自定义插槽 -->
            <slot 
              v-else-if="field.type === 'slot'" 
              :name="field.slotName || field.prop" 
              :model="model"
              :field="field"
            />
            
            <!-- 默认输入框 -->
            <el-input
              v-else
              v-model="model[field.prop]"
              :placeholder="field.placeholder"
              v-bind="field.attrs"
              :size="field.size || 'default'"
            />
          </el-form-item>
        </el-col>
      </template>
    </el-row>
    
    <el-form-item v-if="showActions">
      <slot name="actions">
        <div class="form-actions">
          <el-button 
            type="primary" 
            @click="handleSubmit" 
            :loading="loading"
            :size="actionSize"
          >
            {{ submitText }}
          </el-button>
          <el-button 
            @click="handleReset" 
            v-if="showReset"
            :size="actionSize"
          >
            {{ resetText }}
          </el-button>
          <slot name="extra-actions"></slot>
        </div>
      </slot>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

interface FieldOption {
  label: string
  value: any
}

interface Field {
  prop: string
  label?: string
  type?: 'input' | 'textarea' | 'number' | 'select' | 'date' | 'slot'
  placeholder?: string
  inputType?: string // text, password, email, etc.
  showPassword?: boolean
  clearable?: boolean
  rows?: number
  min?: number
  max?: number
  step?: number
  multiple?: boolean
  options?: FieldOption[]
  slotName?: string
  hidden?: boolean
  attrs?: Record<string, any>
  size?: 'large' | 'default' | 'small'
  span?: number
  md?: number
  sm?: number
}

interface Props {
  model: Record<string, any>
  fields: Field[]
  rules?: FormRules
  labelWidth?: string
  inline?: boolean
  loading?: boolean
  showActions?: boolean
  showReset?: boolean
  submitText?: string
  resetText?: string
  actionSize?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<Props>(), {
  labelWidth: '100px',
  inline: false,
  loading: false,
  showActions: true,
  showReset: true,
  submitText: '提交',
  resetText: '重置',
  actionSize: 'default'
})

const emit = defineEmits<{
  (e: 'submit', model: Record<string, any>): void
  (e: 'reset'): void
}>()

const formRef = ref<FormInstance>()

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('submit', props.model)
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
  emit('reset')
}

defineExpose({
  formRef
})
</script>

<style scoped>
.custom-form {
  background-color: var(--card-background);
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 var(--shadow-color);
  border: 1px solid var(--border-color);
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  color: var(--regular-text-color);
  font-weight: 500;
}

.form-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .custom-form {
    padding: 20px;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

@media (max-width: 480px) {
  .custom-form {
    padding: 16px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
  
  :deep(.el-form-item__label) {
    padding-bottom: 6px;
  }
}
</style>