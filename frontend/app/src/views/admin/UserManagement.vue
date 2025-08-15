<template>
  <div class="user-management">
    <PageHeader 
      title="用户管理" 
      description="管理系统中的所有用户"
      :breadcrumb="[
        { title: '首页', to: '/' },
        { title: '管理员', to: '/admin/dashboard' },
        { title: '用户管理' }
      ]"
    >
      <template #extra>
        <el-button type="primary" @click="handleAddUser">新增用户</el-button>
        <el-button @click="handleRefresh">刷新</el-button>
      </template>
    </PageHeader>
    
    <Card>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="searchForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <Table
        :data="userData"
        :columns="userColumns"
        :loading="loading"
        show-selection
        show-index
        show-pagination
        :pagination="pagination"
        @selection-change="handleSelectionChange"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
        
        <template #actions="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </Table>
    </Card>
    
    <!-- 新增/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userFormRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="userForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitUser" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="deleteDialogVisible"
      title="删除确认"
      message="您确定要删除该用户吗？此操作不可恢复。"
      type="warning"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import Table from '@/components/Table.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

// 搜索表单
const searchForm = reactive({
  username: '',
  email: ''
})

// 表格相关
const loading = ref(false)
const userData = ref([
  { id: 1, username: 'user1', email: 'user1@example.com', phone: '13800138001', nickname: '用户1', status: 1, createTime: '2023-01-01' },
  { id: 2, username: 'user2', email: 'user2@example.com', phone: '13800138002', nickname: '用户2', status: 0, createTime: '2023-01-02' },
  { id: 3, username: 'user3', email: 'user3@example.com', phone: '13800138003', nickname: '用户3', status: 1, createTime: '2023-01-03' }
])

const userColumns = [
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'email', label: '邮箱' },
  { prop: 'phone', label: '手机号', width: 120 },
  { prop: 'nickname', label: '昵称', width: 120 },
  { prop: 'status', label: '状态', slotName: 'status', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 120 },
  { prop: 'actions', label: '操作', slotName: 'actions', width: 120, fixed: 'right' }
]

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 3
})

// 表单相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const isEdit = ref(false)
const currentUserId = ref(0)

const userForm = reactive({
  username: '',
  email: '',
  phone: '',
  nickname: '',
  status: 1
})

const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ]
}

// 删除相关
const deleteDialogVisible = ref(false)
const deleteUser = ref<any>(null)

// 表格事件处理
const handleSelectionChange = (selection: any[]) => {
  console.log('选中的用户:', selection)
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  console.log('页面大小改变:', size)
}

const handleCurrentChange = (current: number) => {
  pagination.currentPage = current
  console.log('当前页改变:', current)
}

// 搜索相关
const handleSearch = () => {
  ElMessage.info('执行搜索操作')
  console.log('搜索条件:', searchForm)
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.email = ''
  ElMessage.info('重置搜索条件')
}

// 操作相关
const handleAddUser = () => {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  dialogVisible.value = true
  resetUserForm()
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  currentUserId.value = row.id
  // 填充表单数据
  userForm.username = row.username
  userForm.email = row.email
  userForm.phone = row.phone
  userForm.nickname = row.nickname
  userForm.status = row.status
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  deleteUser.value = row
  deleteDialogVisible.value = true
}

const handleRefresh = () => {
  ElMessage.info('刷新数据')
}

// 对话框相关
const handleDialogClose = () => {
  resetUserForm()
}

const resetUserForm = () => {
  userForm.username = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.nickname = ''
  userForm.status = 1
}

// 提交用户表单
const handleSubmitUser = () => {
  ElMessage.success(`${isEdit.value ? '编辑' : '新增'}用户成功`)
  dialogVisible.value = false
}

// 删除确认
const confirmDelete = () => {
  ElMessage.success('删除用户成功')
  deleteDialogVisible.value = false
  deleteUser.value = null
}

const cancelDelete = () => {
  deleteUser.value = null
}

// 页面加载时获取数据
onMounted(() => {
  console.log('用户管理页面已加载')
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>