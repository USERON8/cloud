<template>
  <div class="products-container">
    <el-card class="products-card">
      <template #header>
        <div class="card-header">
          <span>产品管理</span>
          <el-button type="primary" @click="handleCreate">新增产品</el-button>
        </div>
      </template>

      <!-- 搜索条件 -->
      <el-form :model="searchForm" class="search-form" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="产品名称">
              <el-input v-model="searchForm.productName" placeholder="请输入产品名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="产品状态">
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

      <!-- 产品列表 -->
      <el-table
          v-loading="loading"
          :data="productList"
          border
          class="products-table"
          element-loading-text="加载中..."
      >
        <el-table-column label="产品ID" prop="id" width="80"/>
        <el-table-column label="产品名称" min-width="150" prop="productName"/>
        <el-table-column label="产品编码" prop="productCode" width="120"/>
        <el-table-column label="产品分类" prop="category" width="100"/>
        <el-table-column label="价格" prop="price" width="100">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column label="库存" prop="stock" width="80"/>
        <el-table-column label="状态" prop="status" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160"/>
        <el-table-column fixed="right" label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
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

    <!-- 产品编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
        @close="handleDialogClose"
    >
      <el-form
          ref="productFormRef"
          :model="productForm"
          :rules="productRules"
          label-width="80px"
      >
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="productForm.productName" placeholder="请输入产品名称"/>
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="productForm.productCode" placeholder="请输入产品编码"/>
        </el-form-item>
        <el-form-item label="产品分类" prop="category">
          <el-select v-model="productForm.category" placeholder="请选择产品分类" style="width: 100%">
            <el-option label="电子产品" value="electronics"/>
            <el-option label="服装" value="clothing"/>
            <el-option label="食品" value="food"/>
            <el-option label="家居" value="home"/>
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number
              v-model="productForm.price"
              :min="0"
              :precision="2"
              :step="0.1"
              controls-position="right"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number
              v-model="productForm.stock"
              :min="0"
              controls-position="right"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
              v-model="productForm.status"
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
          <el-button :loading="submitLoading" type="primary" @click="submitProductForm">
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
import {useProductStore} from '@/store/modules/product'

// 产品状态管理
const productStore = useProductStore()

// 响应式数据
const searchForm = reactive({
  productName: '',
  status: ''
})

const productForm = reactive({
  id: null,
  productName: '',
  productCode: '',
  category: '',
  price: 0,
  stock: 0,
  status: 1
})

const productRules = {
  productName: [
    {required: true, message: '请输入产品名称', trigger: 'blur'}
  ],
  productCode: [
    {required: true, message: '请输入产品编码', trigger: 'blur'}
  ],
  category: [
    {required: true, message: '请选择产品分类', trigger: 'change'}
  ],
  price: [
    {required: true, message: '请输入价格', trigger: 'blur'}
  ]
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const productFormRef = ref(null)

// 计算属性
const productList = computed(() => productStore.productList)
const loading = computed(() => productStore.loading)
const pagination = computed(() => productStore.productPagination)

// 方法
const handleSearch = async () => {
  await productStore.fetchProductList(searchForm)
}

const handleReset = () => {
  Object.assign(searchForm, {
    productName: '',
    status: ''
  })
  handleSearch()
}

const handleCreate = () => {
  dialogTitle.value = '新增产品'
  dialogVisible.value = true
  Object.assign(productForm, {
    id: null,
    productName: '',
    productCode: '',
    category: '',
    price: 0,
    stock: 0,
    status: 1
  })
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑产品'
  dialogVisible.value = true
  Object.assign(productForm, row)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
      `确定要删除产品 "${row.productName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await productStore.deleteProduct(row.id)
      ElMessage.success('删除成功')
      handleSearch()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

const handleSizeChange = (val) => {
  productStore.setPageSize(val)
  handleSearch()
}

const handleCurrentChange = (val) => {
  productStore.setCurrentPage(val)
  handleSearch()
}

const submitProductForm = async () => {
  try {
    await productFormRef.value.validate()
    submitLoading.value = true

    if (productForm.id) {
      // 更新产品
      await productStore.updateProduct(productForm.id, productForm)
      ElMessage.success('更新成功')
    } else {
      // 创建产品
      await productStore.createProduct(productForm)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(error.message || (productForm.id ? '更新失败' : '创建失败'))
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  productFormRef.value?.resetFields()
}

// 组件挂载时获取数据
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.products-container {
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

.products-table {
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