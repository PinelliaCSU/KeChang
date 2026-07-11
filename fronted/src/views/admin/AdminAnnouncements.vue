<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">平台公告</h2>
        <p class="page-subtitle">向全平台、所有教师或所有学生发布公告通知，公告会同步推送到用户通知中心</p>
      </div>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <DataPanel max-width="1400px">
      <template #toolbar>
        <div class="search-bar">
          <el-button type="primary" @click="openCreate">发布公告</el-button>
        </div>
      </template>

      <el-table :data="announcements" stripe class="is-borderless" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisherName" label="发布人" width="120" />
        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="!row.courseId ? 'success' : ''">
              {{ row.scope || '全平台' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmCount" label="确认数" width="80" />
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </DataPanel>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="createVisible" title="发布公告" width="560px" destroy-on-close @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px" label-position="top">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="5" placeholder="请输入公告内容" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="发送范围">
          <el-radio-group v-model="createForm.scope">
            <el-radio value="all">全平台（所有教师和学生）</el-radio>
            <el-radio value="teachers">所有教师</el-radio>
            <el-radio value="students">所有学生</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="submitCreate">发布</el-button>
      </template>
    </el-dialog>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="detailVisible" title="公告详情" width="560px">
      <div v-if="detail" class="detail-body">
        <h3 class="detail-title">{{ detail.title }}</h3>
        <div class="detail-meta">
          <span>发布人：{{ detail.publisherName }}</span>
          <span>时间：{{ detail.publishTime }}</span>
          <span>范围：{{ detail.scope || '全平台' }}</span>
          <span>已确认：{{ detail.confirmCount }} 人</span>
        </div>
        <div class="detail-content">{{ detail.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { announcementApi } from '@/api'
import { required } from '@/utils/validators'
import DataPanel from '@/components/DataPanel.vue'

const announcements = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

// 发布公告
const createVisible = ref(false)
const publishing = ref(false)
const createFormRef = ref(null)

const createForm = reactive({
  title: '',
  content: '',
  scope: 'all'
})

const createRules = {
  title: [required('请输入公告标题')],
  content: [required('请输入公告内容')]
}

// 详情
const detailVisible = ref(false)
const detail = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await announcementApi.adminList({ page: query.page, size: query.size })
    if (res.data.code === 200) {
      announcements.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
  } catch {
    ElMessage.error('公告列表加载失败')
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  query.page = 1
  fetchList()
}

// ---- 发布 ----
const openCreate = () => {
  createForm.title = ''
  createForm.content = ''
  createForm.scope = 'all'
  createVisible.value = true
}

const resetCreateForm = () => {
  if (createFormRef.value) createFormRef.value.resetFields()
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  publishing.value = true
  try {
    await announcementApi.adminCreate({
      title: createForm.title,
      content: createForm.content,
      scope: createForm.scope
    })
    ElMessage.success('公告已发布')
    createVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

// ---- 详情 ----
const openDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

// ---- 删除 ----
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告"${row.title}"吗？`,
      '删除公告',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await announcementApi.delete(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; }
.search-bar { display: flex; gap: 12px; }
.pager { display: flex; justify-content: center; margin-top: 18px; }
.detail-body { padding: 0; }
.detail-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0 0 12px; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 16px; }
.detail-content { background: #f5f7fa; padding: 16px; border-radius: 6px; font-size: 14px; color: #303133; line-height: 1.7; white-space: pre-wrap; }
</style>
