<template>
  <div class="stock-manager">
    <!-- 页面标题 -->
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>库存管理</el-breadcrumb-item>
        <el-breadcrumb-item>库存明细</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 class="page-title">库存明细</h2>
      <p class="page-description">查看和管理商品库存信息</p>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <el-card class="search-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>搜索条件</span>
            <el-button link type="primary" @click="toggleAdvancedSearch">
              {{ showAdvancedSearch ? '收起' : '展开' }}高级搜索
              <el-icon :class="{ 'is-rotate': showAdvancedSearch }">
                <ArrowDown />
              </el-icon>
            </el-button>
          </div>
        </template>
        
        <el-form :model="searchForm" class="search-form" label-width="80px" label-position="left">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="商品ID">
                <el-input
                  v-model="searchForm.productId"
                  clearable
                  placeholder="请输入商品ID"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="商品名称">
                <el-input
                  v-model="searchForm.productName"
                  clearable
                  placeholder="请输入商品名称"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="库存状态">
                <el-select v-model="searchForm.stockStatus" clearable placeholder="请选择">
                  <el-option :value="0" label="缺货"/>
                  <el-option :value="1" label="不足"/>
                  <el-option :value="2" label="充足"/>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-collapse-transition>
            <div v-show="showAdvancedSearch">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="最低库存">
                    <el-input-number
                      v-model="searchForm.minStock"
                      :min="0"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="最高库存">
                    <el-input-number
                      v-model="searchForm.maxStock"
                      :min="0"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </el-collapse-transition>
          
          <div class="form-actions">
            <el-button :loading="loading" type="primary" @click="handleSearch">
              <el-icon>
                <Search />
              </el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon>
                <RefreshLeft />
              </el-icon>
              重置
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-item">
              <div class="stats-icon primary">
                <el-icon size="20">
                  <Goods />
                </el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ statistics.totalProducts || 0 }}</div>
                <div class="stats-label">总商品数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-item">
              <div class="stats-icon warning">
                <el-icon size="20">
                  <Warning />
                </el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ statistics.lowStockCount || 0 }}</div>
                <div class="stats-label">库存不足</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-item">
              <div class="stats-icon danger">
                <el-icon size="20">
                  <CircleClose />
                </el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ statistics.outStockCount || 0 }}</div>
                <div class="stats-label">缺货商品</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card" shadow="hover">
            <div class="stats-item">
              <div class="stats-icon success">
                <el-icon size="20">
                  <SuccessFilled />
                </el-icon>
              </div>
              <div class="stats-info">
                <div class="stats-value">{{ statistics.normalStockCount || 0 }}</div>
                <div class="stats-label">库存充足</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 库存列表 -->
    <div class="table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <span>库存列表</span>
              <el-tag type="info">共 {{ pagination.total }} 条记录</el-tag>
            </div>
            <div class="header-right">
              <el-button-group>
                <el-button :loading="loading" type="primary" @click="handleRefresh">
                  <el-icon>
                    <Refresh />
                  </el-icon>
                  刷新
                </el-button>
                <el-button type="success" @click="handleExport">
                  <el-icon>
                    <Download />
                  </el-icon>
                  导出
                </el-button>
              </el-button-group>
            </div>
          </div>
        </template>

        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="productId" label="商品ID" width="100" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="totalCount" label="总库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockCountType(row.totalCount)">
                {{ row.totalCount }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="availableCount" label="可用库存" width="100" align="center" />
          <el-table-column prop="reservedCount" label="预留库存" width="100" align="center" />
          <el-table-column prop="stockStatus" label="库存状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStockStatusType(row.stockStatus)">
                {{ getStockStatusText(row.stockStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" align="center" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDetail(row)">
                查看详情
              </el-button>
              <el-button link type="danger" size="small" @click="deleteStock(row.id)">
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
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </el-card>
    </div>

    <!-- 库存详情对话框 -->
    <el-dialog v-model="detailVisible" title="库存详情" width="600px" draggable>
      <div v-if="currentStock" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品ID">{{ currentStock.productId }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ currentStock.productName }}</el-descriptions-item>
          <el-descriptions-item label="总库存">{{ currentStock.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="可用库存">{{ currentStock.availableCount }}</el-descriptions-item>
          <el-descriptions-item label="预留库存">{{ currentStock.reservedCount }}</el-descriptions-item>
          <el-descriptions-item label="库存状态">
            <el-tag :type="getStockStatusType(currentStock.stockStatus)">
              {{ getStockStatusText(currentStock.stockStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentStock.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentStock.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="detailVisible = false">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  Search, 
  ArrowDown, 
  RefreshLeft, 
  Download,
  Goods,
  Warning,
  CircleClose,
  SuccessFilled
} from '@element-plus/icons-vue'
import { stockApi } from '../api/stock'

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentStock = ref(null)
const showAdvancedSearch = ref(false)
const selectedItems = ref([])

// 搜索表单
const searchForm = reactive({
  productId: '',
  productName: '',
  stockStatus: null,
  minStock: null,
  maxStock: null
})

// 分页数据
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 统计数据
const statistics = ref({
  totalProducts: 0,
  lowStockCount: 0,
  outStockCount: 0,
  normalStockCount: 0
})

// 获取库存状态类型
const getStockStatusType = (status) => {
  const types = { 0: 'danger', 1: 'warning', 2: 'success' }
  return types[status] || 'info'
}

// 获取库存状态文本
const getStockStatusText = (status) => {
  const texts = { 0: '缺货', 1: '不足', 2: '充足' }
  return texts[status] || '未知'
}

// 获取库存数量类型
const getStockCountType = (count) => {
  if (count === 0) return 'danger'
  if (count < 10) return 'warning'
  return 'success'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }

    const response = await stockApi.pageQuery(params)
    if (response.code === 200) {
      tableData.value = response.data.records
      pagination.total = response.data.total
    } else {
      ElMessage.error(response.message || '查询失败')
    }
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await stockApi.getStatistics()
    if (response.code === 200) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置搜索条件
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = null
  })
  pagination.current = 1
  loadData()
}

// 刷新
const handleRefresh = () => {
  loadData()
  loadStatistics()
}

// 导出数据
const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

// 切换高级搜索
const toggleAdvancedSearch = () => {
  showAdvancedSearch.value = !showAdvancedSearch.value
}

// 查看详情
const viewDetail = (row) => {
  currentStock.value = row
  detailVisible.value = true
}

// 删除库存
const deleteStock = (id) => {
  ElMessageBox.confirm('确定要删除该库存记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await stockApi.deleteStock(id)
      if (response.code === 200) {
        ElMessage.success('删除成功')
        loadData()
      } else {
        ElMessage.error(response.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pagination.size = val
  loadData()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  pagination.current = val
  loadData()
}

// 处理选择变化
const handleSelectionChange = (val) => {
  selectedItems.value = val
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.stock-manager {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header :deep(.el-breadcrumb) {
  margin-bottom: 10px;
}

.page-title {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: 500;
  color: var(--text-color);
}

.page-description {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 14px;
}

.search-section {
  margin-bottom: 20px;
}

.search-card :deep(.el-card__header) {
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .el-button {
  font-size: 12px;
}

.card-header .el-button :deep(.el-icon) {
  transition: transform 0.3s;
}

.card-header .el-button :deep(.el-icon.is-rotate) {
  transform: rotate(180deg);
}

.search-form {
  padding: 10px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-card {
  border: none;
}

.stats-item {
  display: flex;
  align-items: center;
}

.stats-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: white;
}

.stats-icon.primary {
  background-color: var(--primary-color);
}

.stats-icon.success {
  background-color: var(--success-color);
}

.stats-icon.warning {
  background-color: var(--warning-color);
}

.stats-icon.danger {
  background-color: var(--danger-color);
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 5px;
}

.stats-label {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.table-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
}

.detail-content {
  padding: 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-section :deep(.el-col) {
    margin-bottom: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>