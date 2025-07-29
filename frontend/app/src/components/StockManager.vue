<template>
  <div class="stock-manager">
    <!-- 搜索区域 -->
    <div class="search-section">
      <el-card class="search-card">
        <el-form :model="searchForm" inline class="search-form">
          <el-form-item label="商品ID">
            <el-input
                v-model="searchForm.productId"
                placeholder="请输入商品ID"
                clearable
                style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="商品名称">
            <el-input
                v-model="searchForm.productName"
                placeholder="请输入商品名称"
                clearable
                style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="库存状态">
            <el-select v-model="searchForm.stockStatus" placeholder="请选择" clearable>
              <el-option label="缺货" :value="0"/>
              <el-option label="不足" :value="1"/>
              <el-option label="充足" :value="2"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading">
              <el-icon>
                <Search/>
              </el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-value">{{ statistics.totalProducts || 0 }}</div>
              <div class="stats-label">总商品数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-value low-stock">{{ statistics.lowStockCount || 0 }}</div>
              <div class="stats-label">库存不足</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-value out-stock">{{ statistics.outStockCount || 0 }}</div>
              <div class="stats-label">缺货商品</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-value normal-stock">{{ statistics.normalStockCount || 0 }}</div>
              <div class="stats-label">库存充足</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 库存列表 -->
    <div class="table-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>库存列表</span>
            <el-button type="primary" @click="handleRefresh" :loading="loading">
              <el-icon>
                <Refresh/>
              </el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <el-table
            :data="tableData"
            v-loading="loading"
            stripe
            border
            style="width: 100%"
        >
          <el-table-column prop="productId" label="商品ID" width="120"/>
          <el-table-column prop="productName" label="商品名称" min-width="200"/>
          <el-table-column prop="totalCount" label="总库存" width="100" align="center"/>
          <el-table-column prop="availableCount" label="可用库存" width="100" align="center"/>
          <el-table-column prop="reservedCount" label="预留库存" width="100" align="center"/>
          <el-table-column label="库存状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                  :type="getStockStatusType(row.stockStatus)"
                  size="small"
              >
                {{ getStockStatusText(row.stockStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180"/>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button
                  type="primary"
                  size="small"
                  @click="handleViewDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="库存详情" width="600px">
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
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {Refresh, Search} from '@element-plus/icons-vue'
import {stockApi} from '../api/stock'

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentStock = ref(null)

// 搜索表单
const searchForm = reactive({
  productId: '',
  productName: '',
  stockStatus: null
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
  const types = {0: 'danger', 1: 'warning', 2: 'success'}
  return types[status] || 'info'
}

// 获取库存状态文本
const getStockStatusText = (status) => {
  const texts = {0: '缺货', 1: '不足', 2: '充足'}
  return texts[status] || '未知'
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
    console.error('获取统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    productId: '',
    productName: '',
    stockStatus: null
  })
  pagination.current = 1
  loadData()
}

// 刷新
const handleRefresh = () => {
  loadData()
  loadStatistics()
}

// 查看详情
const handleViewDetail = (row) => {
  currentStock.value = row
  detailVisible.value = true
}

// 分页变化
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadData()
}

// 初始化
onMounted(() => {
  loadData()
  loadStatistics()
})
</script>

<style scoped>
.stock-manager {
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.search-card {
  border-radius: 8px;
}

.search-form {
  margin: 0;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-card {
  border-radius: 8px;
  text-align: center;
}

.stats-item {
  padding: 10px 0;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stats-value.low-stock {
  color: #e6a23c;
}

.stats-value.out-stock {
  color: #f56c6c;
}

.stats-value.normal-stock {
  color: #67c23a;
}

.stats-label {
  font-size: 14px;
  color: #666;
}

.table-section {
  background: #fff;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: right;
}

.detail-content {
  padding: 20px 0;
}
</style>
