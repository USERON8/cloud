<template>
  <div class="statistics">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>统计报表</span>
          <el-button :loading="loading" type="primary" @click="loadData">
            <el-icon>
              <Refresh/>
            </el-icon>
            刷新数据
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="chart-card">
            <h3>库存状态分布</h3>
            <div class="chart-placeholder">
              <p>图表区域 - 可集成ECharts</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card">
            <h3>库存趋势</h3>
            <div class="chart-placeholder">
              <p>图表区域 - 可集成ECharts</p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {Refresh} from '@element-plus/icons-vue'

const loading = ref(false)

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 1000)
}
</script>

<style scoped>
.statistics {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>