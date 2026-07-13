<template>
    <div class="checkin-page">
        <div class="page-head">
            <div>
                <p class="eyebrow">课堂互动</p>
                <h2>课堂签到</h2>
            </div>
            <div class="head-actions">
                <el-button type="primary" size="large" @click="creatorRef?.open()">
                    <el-icon>
                        <Plus />
                    </el-icon>发起签到
                </el-button>
            </div>
        </div>

        <!-- 全屏展示当前签到（教师发起后自动全屏） -->
        <div v-if="activeSession" class="active-section">
            <div class="active-header">
                <h3>{{ activeSession.courseName }} — {{ activeSession.title }}</h3>
                <div class="active-actions">
                    <el-button @click="refreshActive" :loading="refreshing" circle>
                        <el-icon>
                            <Refresh />
                        </el-icon>
                    </el-button>
                    <el-button type="warning" @click="enterFullscreen" plain>
                        <el-icon>
                            <FullScreen />
                        </el-icon>大屏展示
                    </el-button>
                    <el-button type="danger" @click="handleCloseCheckin" :loading="closing">
                        <el-icon>
                            <SwitchButton />
                        </el-icon>结束签到
                    </el-button>
                </div>
            </div>

            <div class="active-body">
                <!-- 签到码展示 -->
                <div class="code-panel">
                    <CheckinCodeDisplay :session="activeSession" :stats="activeStats" />
                </div>

                <!-- 实时统计 -->
                <div class="stats-panel">
                    <CheckinResultPanel :stats="activeStats" />
                </div>
            </div>
        </div>

        <!-- 历史签到列表 -->
        <div class="history-section" v-else>
            <div class="panel-toolbar">
                <el-select v-model="filterCourseId" placeholder="筛选课程" clearable @change="resetPage(); loadSessions()"
                    style="width:200px">
                    <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
                </el-select>
                <el-input v-model="searchKeyword" placeholder="搜索签到标题" clearable style="width:200px;margin-left:12px"
                    @change="onKeywordChange" @clear="onKeywordChange" />
            </div>

            <template v-if="sessions.length">
                <el-table :data="sessions" class="kechang-table is-borderless" @row-click="selectSession"
                    style="cursor:pointer">
                    <el-table-column prop="courseName" label="课程" width="160" />
                    <el-table-column prop="title" label="签到标题" min-width="180" />
                    <el-table-column label="方式" width="120">
                        <template #default="{ row }">
                            <el-tag :type="row.checkinType === 'LOCATION' ? 'warning' : ''" size="small" effect="plain">
                                {{ typeLabel(row.checkinType) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="签到码" width="100">
                        <template #default="{ row }">{{ row.code }}</template>
                    </el-table-column>
                    <el-table-column label="时间" width="180">
                        <template #default="{ row }">
                            <span>{{ formatShort(row.startTime) }} - {{ formatShort(row.endTime) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="签到情况" width="160">
                        <template #default="{ row }">
                            <div class="cell-stats">
                                <span class="cs-item success">{{ row.successCount || 0 }}</span>
                                <span class="cs-sep">/</span>
                                <span class="cs-item late">{{ row.lateCount || 0 }}</span>
                                <span class="cs-sep">/</span>
                                <span class="cs-item total">{{ row.totalStudents || 0 }}</span>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.status === 'ACTIVE' ? 'danger' : 'info'" size="small">
                                {{ row.status === 'ACTIVE' ? '签到中' : '已结束' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="100">
                        <template #default="{ row }">
                            <el-button text type="primary" size="small" @click.stop="selectSession(row)">详情</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" @change="loadSessions" />
            </template>
            <el-empty v-else description="暂无签到记录" :image-size="120" />
        </div>

        <!-- 选中签到的详情弹窗 -->
        <el-drawer v-model="detailVisible" title="签到详情" size="600px">
            <template v-if="selectedSession">
                <CheckinCodeDisplay :session="selectedSession" />
                <el-divider />
                <CheckinResultPanel :stats="detailStats" />
                <el-divider />
                <div style="text-align:center">
                    <el-button v-if="selectedSession.status === 'ACTIVE'" type="danger" @click="handleCloseCheckin"
                        :loading="closing">
                        结束签到
                    </el-button>
                </div>
            </template>
        </el-drawer>

        <!-- 全屏展示对话框 -->
        <el-dialog v-model="fullscreenVisible" fullscreen :show-close="false" destroy-on-close>
            <CheckinCodeDisplay v-if="activeSession" :session="activeSession" :stats="activeStats" :fullscreen="true"
                :closing="closing" @close="fullscreenVisible = false" @closeCheckin="handleCloseCheckin" />
        </el-dialog>

        <!-- 创建签到对话框 -->
        <CheckinCreator ref="creatorRef" @created="onCreated" />
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, FullScreen, SwitchButton } from '@element-plus/icons-vue'
import { checkinApi, courseApi } from '@/api'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import CheckinCreator from '@/components/checkin/CheckinCreator.vue'
import CheckinCodeDisplay from '@/components/checkin/CheckinCodeDisplay.vue'
import CheckinResultPanel from '@/components/checkin/CheckinResultPanel.vue'

const route = useRoute()
const creatorRef = ref(null)
const sessions = ref([])
const courses = ref([])
const filterCourseId = ref(null)
const searchKeyword = ref('')
const refreshing = ref(false)
const closing = ref(false)
const detailVisible = ref(false)
const fullscreenVisible = ref(false)
const selectedSession = ref(null)
const activeSession = ref(null)
const activeStats = ref({})
const detailStats = ref({})
let pollTimer = null
const { page, size, total, resetPage } = usePagination({ defaultSize: 10 })

const onKeywordChange = () => {
    resetPage()
    loadSessions()
}

const typeLabel = (t) => ({ QR_CODE: '二维码', NUMERIC_CODE: '数字码', LOCATION: '位置签到' })[t] || t
const formatShort = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadCourses = async () => {
    try {
        const res = await courseApi.list({})
        courses.value = (res.data.data?.records || res.data.data || [])
    } catch { /* ignore */ }
}

const loadSessions = async () => {
    try {
        const res = await checkinApi.teacherList({ page: page.value, size: size.value, courseId: filterCourseId.value || undefined, keyword: searchKeyword.value || undefined })
        sessions.value = res.data.data?.records || []
        total.value = res.data.data?.total || 0
        // 检查是否有活跃签到（仅当前页）
        const active = sessions.value.find(s => s.status === 'ACTIVE')
        if (active && !activeSession.value) {
            activeSession.value = active
            startPolling()
        }
        // 仅在第1页且当前页无活跃签到时才清除，避免分页误判
        if (!active && page.value === 1) {
            activeSession.value = null
            stopPolling()
        }
    } catch { /* ignore */ }
}

const loadStats = async (sessionId) => {
    try {
        const res = await checkinApi.stats(sessionId)
        return res.data.data || {}
    } catch { return {} }
}

const refreshActive = async () => {
    refreshing.value = true
    await loadSessions()
    if (activeSession.value) {
        activeStats.value = await loadStats(activeSession.value.id)
    }
    refreshing.value = false
}

const selectSession = async (session) => {
    selectedSession.value = session
    detailStats.value = await loadStats(session.id)
    detailVisible.value = true
}

const handleCloseCheckin = async () => {
    const target = activeSession.value || selectedSession.value
    if (!target) return
    try {
        await ElMessageBox.confirm('确定要结束本次签到吗？结束后将自动标记未签到学生为缺勤。', '结束签到', { type: 'warning' })
    } catch { return }

    closing.value = true
    try {
        await checkinApi.close(target.id)
        ElMessage.success('签到已结束')
        activeSession.value = null
        fullscreenVisible.value = false
        detailVisible.value = false
        selectedSession.value = null
        stopPolling()
        await loadSessions()
    } catch (e) {
        ElMessage.error('操作失败')
    } finally {
        closing.value = false
    }
}

const onCreated = async (session) => {
    // 直接使用创建接口返回的 session，不依赖分页列表查找
    if (session && session.status === 'ACTIVE') {
        activeSession.value = session
        activeStats.value = await loadStats(session.id)
        startPolling()
    }
    // 重置到第1页并刷新历史列表（后台执行，不影响已设置的 activeSession）
    resetPage()
    await loadSessions()
}

const enterFullscreen = () => { fullscreenVisible.value = true }

const startPolling = () => {
    stopPolling()
    pollTimer = setInterval(async () => {
        if (activeSession.value) {
            activeStats.value = await loadStats(activeSession.value.id)
        }
    }, 10000)
}

const stopPolling = () => {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

onMounted(async () => {
    await loadCourses()
    await loadSessions()
    // header "发起签到" 带 ?action=create 时自动打开创建弹窗
    if (route.query.action === 'create') {
        setTimeout(() => creatorRef.value?.open(), 300)
    }
})

onUnmounted(() => { stopPolling() })
</script>

<style scoped>
.checkin-page {
    padding: 24px 32px;
    max-width: 1400px;
    margin: 0 auto;
}

.page-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 24px;
}

.eyebrow {
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 2px;
    color: var(--bio-primary);
    margin: 0 0 4px;
}

.page-head h2 {
    font-size: 24px;
    font-weight: 700;
    color: var(--bio-ink, #2c3630);
    margin: 0;
}

.active-section {
    background: var(--bio-paper, #fff);
    border-radius: var(--bio-radius-lg, 20px);
    padding: 24px;
    box-shadow: var(--bio-shadow-md);
}

.active-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.active-header h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    margin: 0;
}

.active-actions {
    display: flex;
    gap: 10px;
}

.active-body {
    display: grid;
    grid-template-columns: 380px 1fr;
    gap: 24px;
}

.code-panel {
    background: #f7f8fa;
    border-radius: var(--bio-radius-md, 14px);
    padding: 20px;
    display: flex;
    justify-content: center;
}

.history-section {
    background: var(--bio-paper, #fff);
    border-radius: var(--bio-radius-lg, 20px);
    padding: 24px;
    box-shadow: var(--bio-shadow-sm);
}

.panel-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
}

.cell-stats {
    display: flex;
    align-items: center;
    gap: 2px;
    font-size: 13px;
}

.cs-item.success {
    color: var(--bio-primary);
    font-weight: 600;
}

.cs-item.late {
    color: var(--bio-warning, #c4956a);
    font-weight: 600;
}

.cs-item.total {
    color: var(--bio-ink-muted);
}

.cs-sep {
    color: #ccc;
}
</style>
