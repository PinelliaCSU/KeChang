<template>
    <div class="checkin-page">
        <div class="page-head">
            <div>
                <p class="eyebrow">学习中心</p>
                <h2>课堂签到</h2>
            </div>
        </div>

        <!-- 扫码预填提示 -->
        <el-alert v-if="urlCode && !selectedSession" type="info" :closable="false" show-icon style="margin-bottom:20px">
            已识别签到码 <b>{{ urlCode }}</b>，请选择下方对应课程完成签到
        </el-alert>

        <!-- 活跃签到：卡片列表 -->
        <div v-if="sessions.length" class="session-grid">
            <CheckinSessionCard v-for="s in sessions" :key="s.id" :session="s" :show-stats="false"
                @click="selectSession(s)" />
        </div>
        <el-empty v-else description="当前没有可签到的场次" :image-size="120">
            <el-button type="primary" @click="loadSessions">刷新</el-button>
        </el-empty>

        <!-- 选中签到：签到面板 -->
        <el-dialog v-model="dialogVisible"
            :title="selectedSession?.courseName + ' — ' + (selectedSession?.title || '课堂签到')" width="480px"
            :close-on-click-modal="false" destroy-on-close @closed="selectedSession = null">
            <CheckinStudentPanel v-if="selectedSession" :key="selectedSession.id" :session="selectedSession"
                @checked-in="onCheckedIn" />
        </el-dialog>

        <!-- 历史签到记录 -->
        <div class="history-section" v-if="historySessions.length">
            <h3 class="section-title">签到记录</h3>
            <el-table :data="historySessions" class="kechang-table is-borderless">
                <el-table-column prop="courseName" label="课程" width="160" />
                <el-table-column prop="title" label="签到标题" min-width="160" />
                <el-table-column label="时间" width="200">
                    <template #default="{ row }">
                        <span>{{ formatDate(row.startTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.myStatus)" size="small">
                            {{ statusLabel(row.myStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
            </el-table>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkinApi } from '@/api'
import CheckinSessionCard from '@/components/checkin/CheckinSessionCard.vue'
import CheckinStudentPanel from '@/components/checkin/CheckinStudentPanel.vue'

const route = useRoute()
const router = useRouter()
const sessions = ref([])
const historySessions = ref([])
const selectedSession = ref(null)
const dialogVisible = ref(false)

const urlCode = computed(() => route.query.code || '')
const urlSessionId = computed(() => route.query.sessionId || '')

const selectSession = async (session) => {
    selectedSession.value = session
    dialogVisible.value = true
}

const onCheckedIn = async () => {
    if (selectedSession.value) {
        selectedSession.value.checkedIn = true
        selectedSession.value.myStatus = 'SUCCESS'
        // 更新活跃列表中的状态
        const idx = sessions.value.findIndex(s => s.id === selectedSession.value.id)
        if (idx >= 0) {
            sessions.value[idx].checkedIn = true
            sessions.value[idx].myStatus = 'SUCCESS'
        }
    }
    dialogVisible.value = false
    await loadSessions()
}

const loadSessions = async () => {
    try {
        const res = await checkinApi.studentActiveList()
        sessions.value = res.data.data || []

        // 如果URL中有sessionId，自动选中
        if (urlSessionId.value) {
            const found = sessions.value.find(s => String(s.id) === String(urlSessionId.value))
            if (found) selectSession(found)
        }
        // 如果只有一个活跃签到且URL有code，自动打开
        if (urlCode.value && sessions.value.length === 1) {
            selectSession(sessions.value[0])
        }
    } catch { sessions.value = [] }

    // 加载最近的签到记录（所有课程）
    try {
        const courseRes = await import('@/api').then(m => m.courseApi.list({}))
        const courses = (courseRes.data.data?.records || courseRes.data.data || [])
        const allRecords = []
        for (const c of courses.slice(0, 5)) {
            try {
                const recRes = await checkinApi.courseRecords(c.id)
                const recs = (recRes.data.data || []).filter(r => r.myStatus)
                allRecords.push(...recs)
            } catch { /* skip */ }
        }
        historySessions.value = allRecords.sort((a, b) => new Date(b.startTime || 0) - new Date(a.startTime || 0)).slice(0, 20)
    } catch { /* ignore */ }
}

const formatDate = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const statusLabel = (s) => ({ SUCCESS: '正常', LATE: '迟到', ABSENT: '缺勤' })[s] || '未签到'
const statusTag = (s) => ({ SUCCESS: 'success', LATE: 'warning', ABSENT: 'danger' })[s] || 'info'

onMounted(() => {
    loadSessions()
})
</script>

<style scoped>
.checkin-page {
    padding: 24px 32px;
    max-width: 1000px;
    margin: 0 auto;
}

.page-head {
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

.session-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
}

.history-section {
    margin-top: 40px;
}

.section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    margin: 0 0 12px;
}
</style>
