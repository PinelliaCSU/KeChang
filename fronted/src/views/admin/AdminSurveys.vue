<template>
    <div class="page-shell">
        <div class="page-title-bar">
            <h2 class="page-title">问卷统计总览</h2>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-cards">
            <div class="stat-card">
                <div class="stat-value">{{ overview.totalSurveys || 0 }}</div>
                <div class="stat-label">总问卷数</div>
            </div>
            <div class="stat-card active">
                <div class="stat-value">{{ overview.activeSurveys || 0 }}</div>
                <div class="stat-label">进行中</div>
            </div>
            <div class="stat-card">
                <div class="stat-value">{{ overview.totalResponses || 0 }}</div>
                <div class="stat-label">总回收数</div>
            </div>
        </div>

        <!-- 按课程统计 -->
        <DataPanel v-if="overview.courseStats && overview.courseStats.length > 0" title="按课程统计" max-width="1400px">
            <el-table :data="overview.courseStats" stripe class="is-borderless" style="width:100%">
                <el-table-column prop="courseName" label="课程名称" min-width="180" />
                <el-table-column prop="surveyCount" label="问卷数" width="100" align="center" />
                <el-table-column prop="responseCount" label="回收数" width="100" align="center" />
            </el-table>
        </DataPanel>

        <!-- 最近问卷 -->
        <DataPanel v-if="overview.recentSurveys && overview.recentSurveys.length > 0" title="最近问卷" max-width="1400px">
            <el-table :data="overview.recentSurveys" stripe class="is-borderless" style="width:100%">
                <el-table-column prop="title" label="问卷标题" min-width="180" />
                <el-table-column prop="courseName" label="关联课程" min-width="120" />
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="statusTagType(computeStatus(row.startDate, row.endDate))" size="small">{{
                            computeStatus(row.startDate, row.endDate) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="questionCount" label="题目数" width="80" align="center" />
                <el-table-column prop="responseCount" label="回收数" width="80" align="center" />
                <el-table-column label="操作" width="120">
                    <template #default="{ row }">
                        <el-button size="small" type="primary" @click="viewStats(row)">查看统计</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </DataPanel>

        <!-- 问卷统计详情弹窗 -->
        <el-dialog v-model="statsDialogVisible" :title="statsTitle" width="800px" destroy-on-close>
            <div v-if="statsData" class="stats-content">
                <div class="stats-summary">
                    <el-statistic title="参与人数" :value="statsData.totalRespondents || 0" />
                    <el-statistic title="题目数" :value="statsData.questionCount || 0" />
                </div>
                <el-divider />
                <div v-for="(qs, qi) in (statsData.questionStats || [])" :key="qi" class="stats-question-item">
                    <h4>{{ qi + 1 }}. [{{ typeLabel(qs.questionType) }}] {{ qs.questionContent }}</h4>
                    <p class="stats-meta">共 {{ qs.totalAnswers }} 人回答</p>
                    <div v-if="qs.optionCounts && Object.keys(qs.optionCounts).length > 0" class="option-chart">
                        <div v-for="(count, opt) in qs.optionCounts" :key="opt" class="option-bar-row">
                            <span class="option-label">{{ opt }}</span>
                            <div class="option-bar-track">
                                <div class="option-bar-fill" :style="{ width: barWidth(count, qs.totalAnswers) }"></div>
                            </div>
                            <span class="option-count">{{ count }}票 ({{ percentStr(count, qs.totalAnswers) }})</span>
                        </div>
                    </div>
                    <div v-if="qs.textAnswers && qs.textAnswers.length > 0" class="text-answers">
                        <el-tag size="small" v-for="(ta, tai) in qs.textAnswers" :key="tai" style="margin:4px">{{ ta ||
                            '(空)'
                            }}</el-tag>
                    </div>
                </div>
            </div>
            <el-empty v-else description="暂无统计数据" />
            <template #footer><el-button @click="statsDialogVisible = false">关闭</el-button></template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { surveyApi } from '@/api'
import { computeStatus, statusTagType } from '@/utils/status'
import DataPanel from '@/components/DataPanel.vue'

const overview = reactive({
    totalSurveys: 0,
    activeSurveys: 0,
    totalResponses: 0,
    courseStats: [],
    recentSurveys: []
})

const statsDialogVisible = ref(false); const statsTitle = ref(''); const statsData = ref(null)

const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', TEXT: '问答' }[t] || t)
const barWidth = (count, total) => total > 0 ? (count / total * 100).toFixed(1) + '%' : '0%'
const percentStr = (count, total) => total > 0 ? (count / total * 100).toFixed(1) + '%' : '0%'

const fetchOverview = async () => {
    try {
        const res = await surveyApi.getAdminStats()
        if (res.data.code === 200) Object.assign(overview, res.data.data)
    } catch (e) { /* ignore */ }
}

const viewStats = async (row) => {
    statsTitle.value = '问卷统计 - ' + row.title
    statsData.value = null
    try {
        const res = await surveyApi.getStats(row.id)
        if (res.data.code === 200) statsData.value = res.data.data
        statsDialogVisible.value = true
    } catch (e) { /* ignore */ }
}

onMounted(() => { fetchOverview() })
</script>

<style scoped>
.page-shell {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.page-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.stats-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
}

.stat-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
    text-align: center;
}

.stat-card.active {
    border: 2px solid #67c23a;
}

.stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #303133;
}

.stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
}

.stats-content {
    max-height: 60vh;
    overflow-y: auto;
}

.stats-summary {
    display: flex;
    gap: 40px;
    padding: 8px 0;
}

.stats-question-item {
    margin-bottom: 20px;
    padding: 12px;
    background: #f9fafb;
    border-radius: 8px;
}

.stats-question-item h4 {
    margin: 0 0 6px 0;
    font-size: 15px;
    color: #303133;
}

.stats-meta {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
}

.option-chart {
    padding-left: 12px;
}

.option-bar-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
}

.option-label {
    font-size: 13px;
    min-width: 80px;
    color: #606266;
}

.option-bar-track {
    flex: 1;
    height: 22px;
    background: #ebeef5;
    border-radius: 4px;
    overflow: hidden;
}

.option-bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #409eff, #67c23a);
    border-radius: 4px;
    transition: width 0.4s ease;
    min-width: 2px;
}

.option-count {
    font-size: 12px;
    color: #909399;
    min-width: 100px;
    text-align: right;
}

.text-answers {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    padding-left: 12px;
}
</style>
