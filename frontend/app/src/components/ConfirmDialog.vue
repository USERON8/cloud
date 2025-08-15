<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    v-bind="$attrs"
    @close="handleClose"
  >
    <div class="confirm-content">
      <el-alert
        v-if="type === 'warning'"
        :title="message"
        type="warning"
        show-icon
      />
      <el-alert
        v-else-if="type === 'error'"
        :title="message"
        type="error"
        show-icon
      />
      <el-alert
        v-else-if="type === 'success'"
        :title="message"
        type="success"
        show-icon
      />
      <el-alert
        v-else
        :title="message"
        type="info"
        show-icon
      />
      
      <div class="confirm-message" v-if="!type">
        {{ message }}
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">{{ cancelText }}</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirm" 
          :loading="loading"
          :type="confirmButtonType"
        >
          {{ confirmText }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  message: string
  type?: 'info' | 'success' | 'warning' | 'error'
  width?: string
  loading?: boolean
  confirmText?: string
  cancelText?: string
  confirmButtonType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

const props = withDefaults(defineProps<Props>(), {
  title: '提示',
  type: 'info',
  width: '30%',
  loading: false,
  confirmText: '确定',
  cancelText: '取消',
  confirmButtonType: 'primary'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const visible = ref(false)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  },
  { immediate: true }
)

watch(
  () => visible.value,
  (val) => {
    emit('update:modelValue', val)
  }
)

const handleClose = () => {
  visible.value = false
}

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
  visible.value = false
}
</script>

<style scoped>
.confirm-content {
  padding: 20px 0;
}

.confirm-message {
  font-size: 14px;
  color: #606266;
  line-height: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>