<template>
  <div class="component-demo">
    <PageHeader 
      title="公共组件演示" 
      description="展示如何使用项目中的公共组件"
      :breadcrumb="[
        { title: '首页', to: '/' },
        { title: '组件演示' }
      ]"
    >
      <template #extra>
        <el-button type="primary">主要操作</el-button>
        <el-button>次要操作</el-button>
      </template>
    </PageHeader>
    
    <el-row :gutter="20">
      <el-col :span="12">
        <Card title="卡片组件示例">
          <p>这是一个卡片组件的示例内容。</p>
          <p>卡片组件可以包含头部、主体和底部区域。</p>
        </Card>
      </el-col>
      
      <el-col :span="12">
        <Card title="表单组件示例">
          <Form
            :model="formModel"
            :fields="formFields"
            :rules="formRules"
            @submit="handleFormSubmit"
          >
            <template #extra-actions>
              <el-button @click="resetForm">重置</el-button>
            </template>
          </Form>
        </Card>
      </el-col>
    </el-row>
    
    <Card title="表格组件示例" class="table-card">
      <Table
        :data="tableData"
        :columns="tableColumns"
        :loading="tableLoading"
        show-selection
        show-index
        show-toolbar
        :toolbar-buttons="toolbarButtons"
        show-pagination
        :pagination="pagination"
        @toolbar-click="handleToolbarClick"
        @selection-change="handleSelectionChange"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </Table>
    </Card>
    
    <ConfirmDialog
      v-model="dialogVisible"
      title="确认操作"
      message="您确定要执行此操作吗？"
      type="warning"
      @confirm="handleConfirm"
      @cancel="handleCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import Form from '@/components/Form.vue'
import Table from '@/components/Table.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

// 表单相关
const formModel = reactive({
  name: '',
  email: '',
  status: ''
})

const formFields = [
  {
    prop: 'name',
    label: '姓名',
    type: 'input',
    placeholder: '请输入姓名'
  },
  {
    prop: 'email',
    label: '邮箱',
    type: 'input',
    inputType: 'email',
    placeholder: '请输入邮箱'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: '1' },
      { label: '禁用', value: '0' }
    ]
  }
]

const formRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const handleFormSubmit = (data: any) => {
  ElMessage.success('表单提交成功')
  console.log('表单数据:', data)
}

const resetForm = () => {
  formModel.name = ''
  formModel.email = ''
  formModel.status = ''
}

// 表格相关
const tableLoading = ref(false)
const tableData = ref([
  { id: 1, name: '张三', email: 'zhangsan@example.com', status: 1 },
  { id: 2, name: '李四', email: 'lisi@example.com', status: 0 },
  { id: 3, name: '王五', email: 'wangwu@example.com', status: 1 }
])

const tableColumns = [
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'email', label: '邮箱' },
  { prop: 'status', label: '状态', slotName: 'status', width: 100 }
]

const toolbarButtons = [
  { text: '新增', type: 'primary', action: 'add' },
  { text: '编辑', type: 'success', action: 'edit' },
  { text: '删除', type: 'danger', action: 'delete' }
]

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 3
})

const handleToolbarClick = (action: string) => {
  switch (action) {
    case 'add':
      ElMessage.info('新增操作')
      break
    case 'edit':
      ElMessage.info('编辑操作')
      break
    case 'delete':
      dialogVisible.value = true
      break
  }
}

const selectedRows = ref<any[]>([])

const handleSelectionChange = (selection: any[]) => {
  selectedRows.value = selection
}

// 确认对话框相关
const dialogVisible = ref(false)

const handleConfirm = () => {
  ElMessage.success('确认操作')
  dialogVisible.value = false
}

const handleCancel = () => {
  ElMessage.info('取消操作')
}
</script>

<style scoped>
.component-demo {
  padding: 20px;
}

.table-card {
  margin-top: 20px;
}
</style>