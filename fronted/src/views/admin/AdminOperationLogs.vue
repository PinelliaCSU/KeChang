<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-subtitle">审计教师和平台关键业务操作，便于管理端追踪平台运行情况</p>
      </div>
      <el-button @click="fetchLogs">刷新</el-button>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-num">{{ overview.total || 0 }}</div>
        <div class="stat-label">累计操作</div>
      </div>
      <div class="stat-card">
        <div class="stat-num success">{{ overview.today || 0 }}</div>
        <div class="stat-label">今日操作</div>
      </div>
    </div>

    <DataPanel max-width="1400px">
      <template #toolbar>
        <div class="search-bar">
          <el-select v-model="query.module" placeholder="模块筛选" clearable style="width:180px" @change="fetchLogs">
            <el-option label="课程管理" value="课程管理" />
            <el-option label="作业管理" value="作业管理" />
            <el-option label="考试管理" value="考试管理" />
            <el-option label="实训管理" value="实训管理" />
          </el-select>
          <el-select v-model="query.action" placeholder="动作筛选" clearable style="width:180px" @change="fetchLogs">
            <el-option label="创建课程" value="创建课程" />
            <el-option label="编辑课程" value="编辑课程" />
            <el-option label="上传资源" value="上传资源" />
            <el-option label="发布作业" value="发布作业" />
            <el-option label="批改作业" value="批改作业" />
            <el-option label="发布考试" value="发布考试" />
            <el-option label="批改主观题" value="批改主观题" />
            <el-option label="发布实训项目" value="发布实训项目" />
            <el-option label="评分实训报告" value="评分实训报告" />
          </el-select>
        </div>
      </template>

      <el-table :data="logs" stripe class="is-borderless" v-loading="loading" style="width:100%">
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="operatorName" label="操作人" width="130" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="动作" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="对象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchLogs"
          @size-change="fetchLogs"
        />
      </div>
    </DataPanel>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { operationLogApi } from '@/api'
import DataPanel from '@/components/DataPanel.vue'

const logs = ref([])
const total = ref(0)
const overview = ref({})
const loading = ref(false)
const query = reactive({
  page: 1,
  size: 10,
  module: '',
  action: ''
})

const fetchOverview = async () => {
  const res = await operationLogApi.overview()
  if (res.data.code === 200) overview.value = res.data.data || {}
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await operationLogApi.list(query)
    if (res.data.code === 200) {
      logs.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
  } catch {
    ElMessage.error('操作日志加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOverview()
  fetchLogs()
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; }
.stat-row { display: flex; gap: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 8px; padding: 20px 24px; box-shadow: 0 4px 12px rgba(0,0,0,.05); border: 1px solid #ebeef5; }
.stat-num { color: #303133; font-size: 28px; font-weight: 700; }
.stat-num.success { color: var(--bio-primary); }
.stat-label { color: #606266; font-size: 13px; margin-top: 6px; }
.search-bar { display: flex; gap: 12px; }
.pager { display: flex; justify-content: center; margin-top: 18px; }
</style>
