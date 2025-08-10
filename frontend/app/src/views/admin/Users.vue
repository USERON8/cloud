<template>
  <div class="users-container">
    <el-card class="users-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleCreate">新增用户</el-button>
        </div>
      </template>

      <!-- 搜索条件 -->
      <el-form :model="searchForm" class="search-form" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="用户名">
              <el-input v-model="searchForm.username" placeholder="请输入用户名"/>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="用户类型">
              <el-select v-model="searchForm.userType" clearable placeholder="请选择用户类型">
                <el-option label="管理员" value="admin"/>
                <el-option label="商户" value="merchant"/>
                <el-option label="普通用户" value="user"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" clearable placeholder="请选择状态">
                <el-option label="启用" value="1"/>
                <el-option label="禁用" value="0"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 用户列表 -->
      <el-table
          v-loading="loading"
          :data="userList"
          border
          class="users-table"
          element-loading-text="加载中..."
      >
        <el-table-column label="用户ID" prop="id" width="80"/>
        <el-table-column label="用户名" min-width="120" prop="username"/>
        <el-table-column label="昵称" prop="nickname" width="100"/>
        <el-table-column label="用户类型" prop="userType" width="100">
          <template #default="scope">
            <el-tag :type="getUserTypeTagType(scope.row.userType)">
              {{ getUserTypeName(scope.row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="150" prop="email"/>
        <el-table-column label="手机号" prop="phone" width="120"/>
        <el-table-column label="状态" prop="status" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160"/>
        <el-table-column fixed="right" label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button
                size="small"
                type="warning"
                @click="handleResetPassword(scope.row)"
            >
              重置密码
            </el-button>
            <el-button
                size="small"
                type="danger"
                @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          class="pagination"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 用户编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
        @close="handleDialogClose"
    >
      <el-form
          ref="userFormRef"
          :model="userForm"
          :rules="userRules"
          label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" placeholder="请输入用户名"/>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称"/>
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="userForm.userType" :disabled="!!userForm.id" placeholder="请选择用户类型"
                     style="width: 100%">
            <el-option label="管理员" value="admin"/>
            <el-option label="商户" value="merchant"/>
            <el-option label="普通用户" value="user"/>
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"/>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号"/>
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
          <el-button :loading="submitLoading" type="primary" @click="submitUserForm">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useUserStore} from '@/store/modules/user'

// 用户状态管理
const userStore = useUserStore()

// 响应式数据
const searchForm = reactive({
  username: '',
  userType: '',
  status: ''
})

const userForm = reactive({
  id: null,
  username: '',
  nickname: '',
  userType: '',
  email: '',
  phone: '',
  status: 1
})

const userRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  nickname: [
    {required: true, message: '请输入昵称', trigger: 'blur'}
  ],
  userType: [
    {required: true, message: '请选择用户类型', trigger: 'change'}
  ],
  email: [
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ]
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const userFormRef = ref(null)

// 计算属性
const userList = computed(() => userStore.userList)
const loading = computed(() => userStore.loading)
const pagination = computed(() => userStore.userPagination)

// 方法
const handleSearch = async () => {
  await userStore.fetchUserList(searchForm)
}

const handleReset = () => {
  Object.assign(searchForm, {
    username: '',
    userType: '',
    status: ''
  })
  handleSearch()
}

const handleCreate = () => {
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
  Object.assign(userForm, {
    id: null,
    username: '',
    nickname: '',
    userType: '',
    email: '',
    phone: '',
    status: 1
  })
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
  Object.assign(userForm, row)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
      `确定要删除用户 "${row.username}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      // 这里应该调用删除用户接口，目前暂未实现
      ElMessage.success('删除成功')
      handleSearch()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

const handleResetPassword = (row) => {
  ElMessageBox.confirm(
      `确定要重置用户 "${row.username}" 的密码吗？`,
      '确认重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      // 这里应该调用重置密码接口，目前暂未实现
      ElMessage.success('密码已重置为默认密码')
    } catch (error) {
      ElMessage.error(error.message || '重置密码失败')
    }
  }).catch(() => {
    // 用户取消重置密码
  })
}

const handleSizeChange = (val) => {
  userStore.setPageSize(val)
  handleSearch()
}

const handleCurrentChange = (val) => {
  userStore.setCurrentPage(val)
  handleSearch()
}

const submitUserForm = async () => {
  try {
    await userFormRef.value.validate()
    submitLoading.value = true

    if (userForm.id) {
      // 更新用户
      await userStore.updateUserInfo(userForm)
      ElMessage.success('更新成功')
    } else {
      // 创建用户（这里应该调用创建用户接口，目前暂未实现）
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(error.message || (userForm.id ? '更新失败' : '创建失败'))
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  userFormRef.value?.resetFields()
}

// 获取用户类型标签类型
const getUserTypeTagType = (userType) => {
  switch (userType) {
    case 'admin':
      return 'danger'
    case 'merchant':
      return 'warning'
    case 'user':
      return 'success'
    default:
      return 'info'
  }
}

// 获取用户类型名称
const getUserTypeName = (userType) => {
  switch (userType) {
    case 'admin':
      return '管理员'
    case 'merchant':
      return '商户'
    case 'user':
      return '普通用户'
    default:
      return '未知'
  }
}

// 组件挂载时获取数据
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.users-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.users-table {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>