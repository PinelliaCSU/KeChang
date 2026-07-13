<template>
    <div class="page-shell">
        <div class="page-title-bar">
            <h2 class="page-title">问卷调查</h2>
        </div>

        <div class="panel" v-if="surveys.length > 0">
            <div class="survey-list">
                <div class="survey-card" v-for="s in surveys" :key="s.id">
                    <div class="survey-card-header">
                        <h3>{{ s.title }}</h3>
                        <el-tag :type="s.submitted ? 'success' : 'warning'" size="small">
                            {{ s.submitted ? '已提交' : '待填写' }}
                        </el-tag>
                    </div>
                    <p class="survey-desc">{{ s.description || '暂无说明' }}</p>
                    <div class="survey-meta">
                        <span><el-icon>
                                <School />
                            </el-icon> {{ s.courseName || '未关联课程' }}</span>
                        <span><el-icon>
                                <Calendar />
                            </el-icon> {{ s.startDate || '-' }} ~ {{ s.endDate || '-' }}</span>
                        <span><el-icon>
                                <Document />
                            </el-icon> {{ s.questionCount || 0 }} 题</span>
                        <span><el-icon>
                                <User />
                            </el-icon> {{ s.responseCount || 0 }} 人已填</span>
                    </div>
                    <div class="survey-actions">
                        <el-tag :type="statusTagType(computeStatus(s.startDate, s.endDate))" size="small">{{
                            computeStatus(s.startDate, s.endDate) }}</el-tag>
                        <el-button v-if="!s.submitted && computeStatus(s.startDate, s.endDate) === '进行中'" type="primary"
                            size="small" @click="openSurvey(s)">
                            开始填写
                        </el-button>
                        <el-button v-if="s.submitted" type="info" size="small" disabled>已提交</el-button>
                        <span v-if="computeStatus(s.startDate, s.endDate) !== '进行中' && !s.submitted"
                            style="color:#909399;font-size:12px">
                            {{ computeStatus(s.startDate, s.endDate) === '未开始' ? '暂未开放' : '已结束' }}
                        </span>
                    </div>
                </div>
            </div>
            <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" @change="fetchSurveys" />
        </div>

        <div class="panel empty-panel" v-else-if="!loading">
            <EmptyIllustration variant="star" text="暂无待填写的问卷" />
        </div>

        <!-- 填写问卷弹窗 -->
        <el-dialog v-model="fillDialogVisible" :title="'填写问卷: ' + currentSurvey.title" width="700px" destroy-on-close
            :close-on-click-modal="false">
            <div v-if="questions.length > 0" class="fill-form">
                <div v-for="(q, idx) in questions" :key="q.id" class="fill-question-item">
                    <div class="fill-q-header">
                        <span class="fill-q-num">{{ idx + 1 }}.</span>
                        <span class="fill-q-content">{{ q.questionContent }}</span>
                        <el-tag v-if="q.isRequired === 1" size="small" type="danger">必填</el-tag>
                        <el-tag v-else size="small" type="info">选填</el-tag>
                    </div>
                    <div class="fill-q-body">
                        <!-- 单选题 -->
                        <el-radio-group v-if="q.questionType === 'SINGLE'" v-model="answers[idx]"
                            class="fill-option-group">
                            <el-radio v-for="(opt, oi) in parseOptions(q.options)" :key="oi" :value="opt">{{ opt
                            }}</el-radio>
                        </el-radio-group>
                        <!-- 多选题 -->
                        <el-checkbox-group v-if="q.questionType === 'MULTI'" v-model="multiAnswers[idx]"
                            class="fill-option-group">
                            <el-checkbox v-for="(opt, oi) in parseOptions(q.options)" :key="oi" :value="opt"
                                :label="opt">{{ opt }}</el-checkbox>
                        </el-checkbox-group>
                        <!-- 问答题 -->
                        <el-input v-if="q.questionType === 'TEXT'" v-model="answers[idx]" type="textarea" :rows="3"
                            placeholder="请输入您的回答" />
                    </div>
                </div>
            </div>
            <el-empty v-else description="该问卷暂无题目" />
            <template #footer>
                <el-button @click="fillDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submittingFill" @click="handleSubmitSurvey">提交问卷</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { surveyApi } from '@/api'
import { ElMessage } from 'element-plus'
import { School, Calendar, Document, User } from '@element-plus/icons-vue'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import { computeStatus, statusTagType } from '@/utils/status'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const surveys = ref([]); const loading = ref(false)
const { page, size, total, resetPage } = usePagination({ defaultSize: 10 })

// 填写问卷
const fillDialogVisible = ref(false); const currentSurvey = ref({})
const questions = ref([]); const answers = ref([]); const multiAnswers = ref([])
const submittingFill = ref(false)

const statusTag = (s) => statusTagType(s)

const parseOptions = (opts) => {
    try { return JSON.parse(opts || '[]') } catch (e) { return [] }
}

const fetchSurveys = async () => {
    loading.value = true
    try {
        const res = await surveyApi.list({ page: page.value, size: size.value })
        if (res.data.code === 200) { surveys.value = res.data.data.records; total.value = res.data.data.total }
    } catch (e) { /* ignore */ }
    finally { loading.value = false }
}

const openSurvey = async (survey) => {
    currentSurvey.value = survey
    answers.value = []
    multiAnswers.value = []
    try {
        const res = await surveyApi.getQuestions(survey.id)
        if (res.data.code === 200) {
            questions.value = res.data.data || []
            // 初始化答案数组
            questions.value.forEach((q, idx) => {
                if (q.questionType === 'MULTI') {
                    multiAnswers.value[idx] = []
                    answers.value[idx] = ''
                } else {
                    answers.value[idx] = ''
                }
            })
        }
        fillDialogVisible.value = true
    } catch (e) { ElMessage.error('获取题目失败') }
}

const handleSubmitSurvey = async () => {
    // 验证必填题
    for (let i = 0; i < questions.value.length; i++) {
        const q = questions.value[i]
        if (q.isRequired === 1) {
            const ans = q.questionType === 'MULTI'
                ? (multiAnswers.value[i] || []).join(',')
                : (answers.value[i] || '').trim()
            if (!ans) {
                ElMessage.warning(`请回答第 ${i + 1} 题: ${q.questionContent}`)
                return
            }
        }
    }

    submittingFill.value = true
    try {
        const payload = {
            surveyId: currentSurvey.value.id,
            answers: questions.value.map((q, idx) => ({
                questionId: q.id,
                answerContent: q.questionType === 'MULTI'
                    ? (multiAnswers.value[idx] || []).join(',')
                    : (answers.value[idx] || '')
            }))
        }
        await surveyApi.submit(currentSurvey.value.id, payload)
        ElMessage.success('问卷提交成功！')
        fillDialogVisible.value = false
        fetchSurveys()
    } catch (e) { /* ignore */ }
    finally { submittingFill.value = false }
}

onMounted(() => { fetchSurveys() })
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

.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
}

.survey-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.survey-card {
    padding: 20px;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    transition: box-shadow 0.2s;
}

.survey-card:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.survey-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}

.survey-card-header h3 {
    margin: 0;
    font-size: 16px;
    color: #303133;
}

.survey-desc {
    font-size: 13px;
    color: #909399;
    margin-bottom: 12px;
}

.survey-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 12px;
    color: #909399;
    margin-bottom: 12px;
}

.survey-meta span {
    display: flex;
    align-items: center;
    gap: 4px;
}

.survey-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.fill-form {
    max-height: 55vh;
    overflow-y: auto;
    padding-right: 8px;
}

.fill-question-item {
    padding: 16px;
    margin-bottom: 12px;
    background: #f9fafb;
    border-radius: 8px;
    border-left: 3px solid var(--bio-primary);
}

.fill-q-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    flex-wrap: wrap;
}

.fill-q-num {
    font-weight: 700;
    color: var(--bio-primary);
    font-size: 15px;
}

.fill-q-content {
    font-size: 14px;
    color: #303133;
    flex: 1;
}

.fill-q-body {
    padding-left: 20px;
}

.fill-option-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.empty-panel {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
}

.empty-text {
    font-size: 14px;
    color: #909399;
}
</style>
