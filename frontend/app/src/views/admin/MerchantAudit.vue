<template>
  <div class="merchant-audit">
    <PageHeader 
      title="商家审核" 
      description="审核商家认证申请"
      :breadcrumb="[
        { title: '首页', to: '/' },
        { title: '管理员', to: '/admin/dashboard' },
        { title: '商家审核' }
      ]"
    >
      <template #extra>
        <el-button @click="handleRefresh">刷新</el-button>
      </template>
    </PageHeader>
    
    <Card>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.shopName" placeholder="请输入商家名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已拒绝" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <Table
        :data="merchantData"
        :columns="merchantColumns"
        :loading="loading"
        show-index
        show-pagination
        :pagination="pagination"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      >
        <template #status="{ row }">
          <el-tag v-if="row.status === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已拒绝</el-tag>
        </template>
        
        <template #actions="{ row }">
          <el-button 
            v-if="row.status === 0" 
            link 
            type="primary" 
            @click="handleAudit(row)"
          >
            审核
          </el-button>
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
        </template>
      </Table>
    </Card>
    
    <!-- 审核对话框 -->
    <el-dialog
      v-model="auditDialogVisible"
      title="商家审核"
      width="600px"
      @close="handleAuditDialogClose"
    >
      <el-form
        ref="auditFormRef"
        :model="auditForm"
        :rules="auditFormRules"
        label-width="120px"
      >
        <el-form-item label="商家名称">
          <span>{{ currentMerchant.shopName }}</span>
        </el-form-item>
        <el-form-item label="营业执照号">
          <span>{{ currentMerchant.businessLicenseNumber }}</span>
        </el-form-item>
        <el-form-item label="联系人手机">
          <span>{{ currentMerchant.contactPhone }}</span>
        </el-form-item>
        <el-form-item label="联系地址">
          <span>{{ currentMerchant.contactAddress }}</span>
        </el-form-item>
        <el-form-item label="营业执照">
          <el-image
            v-if="currentMerchant.businessLicenseUrl"
            :src="currentMerchant.businessLicenseUrl"
            :preview-src-list="[currentMerchant.businessLicenseUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="身份证正面">
          <el-image
            v-if="currentMerchant.idCardFrontUrl"
            :src="currentMerchant.idCardFrontUrl"
            :preview-src-list="[currentMerchant.idCardFrontUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="身份证反面">
          <el-image
            v-if="currentMerchant.idCardBackUrl"
            :src="currentMerchant.idCardBackUrl"
            :preview-src-list="[currentMerchant.idCardBackUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="审核状态" prop="status">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="remark">
          <el-input 
            v-model="auditForm.remark" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入审核备注（选填）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="auditDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="商家详情"
      width="600px"
    >
      <el-form
        label-width="120px"
        :model="currentMerchant"
      >
        <el-form-item label="商家名称">
          <span>{{ currentMerchant.shopName }}</span>
        </el-form-item>
        <el-form-item label="营业执照号">
          <span>{{ currentMerchant.businessLicenseNumber }}</span>
        </el-form-item>
        <el-form-item label="联系人手机">
          <span>{{ currentMerchant.contactPhone }}</span>
        </el-form-item>
        <el-form-item label="联系地址">
          <span>{{ currentMerchant.contactAddress }}</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag v-if="currentMerchant.status === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="currentMerchant.status === 1" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已拒绝</el-tag>
        </el-form-item>
        <el-form-item label="审核备注" v-if="currentMerchant.auditRemark">
          <span>{{ currentMerchant.auditRemark }}</span>
        </el-form-item>
        <el-form-item label="营业执照">
          <el-image
            v-if="currentMerchant.businessLicenseUrl"
            :src="currentMerchant.businessLicenseUrl"
            :preview-src-list="[currentMerchant.businessLicenseUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="身份证正面">
          <el-image
            v-if="currentMerchant.idCardFrontUrl"
            :src="currentMerchant.idCardFrontUrl"
            :preview-src-list="[currentMerchant.idCardFrontUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="身份证反面">
          <el-image
            v-if="currentMerchant.idCardBackUrl"
            :src="currentMerchant.idCardBackUrl"
            :preview-src-list="[currentMerchant.idCardBackUrl]"
            style="width: 100px; height: 100px"
            fit="cover"
          />
          <span v-else>无</span>
        </el-form-item>
        <el-form-item label="创建时间">
          <span>{{ currentMerchant.createTime }}</span>
        </el-form-item>
        <el-form-item label="更新时间">
          <span>{{ currentMerchant.updateTime }}</span>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import Card from '@/components/Card.vue'
import Table from '@/components/Table.vue'

// 搜索表单
const searchForm = reactive({
  shopName: '',
  status: ''
})

// 表格相关
const loading = ref(false)
const merchantData = ref([
  {
    id: 1,
    userId: 1001,
    shopName: '示例商家1',
    businessLicenseNumber: '123456789012345678',
    businessLicenseUrl: '',
    idCardFrontUrl: '',
    idCardBackUrl: '',
    contactPhone: '13800138001',
    contactAddress: '北京市朝阳区示例街道1号',
    status: 0,
    auditRemark: '',
    createTime: '2023-01-01 10:00:00',
    updateTime: '2023-01-01 10:00:00'
  },
  {
    id: 2,
    userId: 1002,
    shopName: '示例商家2',
    businessLicenseNumber: '876543210987654321',
    businessLicenseUrl: '',
    idCardFrontUrl: '',
    idCardBackUrl: '',
    contactPhone: '13800138002',
    contactAddress: '上海市浦东新区示例街道2号',
    status: 1,
    auditRemark: '资料齐全，审核通过',
    createTime: '2023-01-02 11:00:00',
    updateTime: '2023-01-03 14:00:00'
  },
  {
    id: 3,
    userId: 1003,
    shopName: '示例商家3',
    businessLicenseNumber: '112233445566778899',
    businessLicenseUrl: '',
    idCardFrontUrl: '',
    idCardBackUrl: '',
    contactPhone: '13800138003',
    contactAddress: '广州市天河区示例街道3号',
    status: 2,
    auditRemark: '资料不全，审核拒绝',
    createTime: '2023-01-03 12:00:00',
    updateTime: '2023-01-04 15:00:00'
  }
])

const merchantColumns = [
  { prop: 'shopName', label: '商家名称', width: 150 },
  { prop: 'businessLicenseNumber', label: '营业执照号', width: 200 },
  { prop: 'contactPhone', label: '联系人手机', width: 120 },
  { prop: 'contactAddress', label: '联系地址' },
  { prop: 'status', label: '状态', slotName: 'status', width: 100 },
  { prop: 'createTime', label: '申请时间', width: 150 },
  { prop: 'actions', label: '操作', slotName: 'actions', width: 120, fixed: 'right' }
]

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 3
})

// 审核对话框相关
const auditDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const submitLoading = ref(false)
const currentMerchant = ref<any>({})

const auditForm = reactive({
  status: 1,
  remark: ''
})

const auditFormRules = {
  status: [
    { required: true, message: '请选择审核状态', trigger: 'change' }
  ]
}

// 表格事件处理
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
  searchForm.shopName = ''
  searchForm.status = ''
  ElMessage.info('重置搜索条件')
}

// 操作相关
const handleAudit = (row: any) => {
  currentMerchant.value = row
  auditForm.status = 1
  auditForm.remark = ''
  auditDialogVisible.value = true
}

const handleView = (row: any) => {
  currentMerchant.value = row
  detailDialogVisible.value = true
}

const handleRefresh = () => {
  ElMessage.info('刷新数据')
}

// 审核对话框相关
const handleAuditDialogClose = () => {
  auditForm.status = 1
  auditForm.remark = ''
}

const submitAudit = () => {
  ElMessage.success('审核操作成功')
  auditDialogVisible.value = false
}

// 页面加载时获取数据
onMounted(() => {
  console.log('商家审核页面已加载')
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