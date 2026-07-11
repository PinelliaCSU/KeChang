<template>
    <div class="page-shell">
        <!-- 顶部标题栏 -->
        <div class="page-title-bar">
            <div>
                <h2 class="page-title">{{ course.courseName || '课程详情' }}</h2>
                <p class="page-subtitle">
                    {{ course.teacherName || '未知教师' }} ·
                    {{ course.studentCount || 0 }} 人选课 ·
                    <el-tag :type="statusTagType(courseStatus)" size="small">{{ courseStatus }}</el-tag>
                </p>
            </div>
            <el-button @click="router.back()">返回</el-button>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar" v-if="course.id">
            <div class="action-info">
                <span v-if="course.enrolled" class="enrolled-badge">✓ 已加入该课程</span>
                <span v-else class="unenrolled-badge">尚未加入该课程</span>
            </div>
            <div class="action-buttons">
                <el-button v-if="course.enrolled" type="primary" size="large" @click="enterLearn">
                    进入学习
                </el-button>
                <el-button v-else type="success" size="large" :loading="enrolling" :disabled="!canEnroll"
                    @click="enrollCourse">
                    {{ canEnroll ? '加入课程' : '暂不可选课' }}
                </el-button>
                <el-button v-if="course.enrolled" type="danger" plain size="large" :loading="unenrolling"
                    @click="unenrollCourse">
                    退出课程
                </el-button>
            </div>
        </div>

        <!-- 主内容面板 -->
        <div class="panel">
            <el-tabs v-model="activeTab">
                <!-- 课程信息 -->
                <el-tab-pane label="课程信息" name="info">
                    <el-descriptions :column="2" border>
                        <el-descriptions-item label="课程名称">{{ course.courseName }}</el-descriptions-item>
                        <el-descriptions-item label="授课教师">{{ course.teacherName }}</el-descriptions-item>
                        <el-descriptions-item label="开始日期">{{ course.startDate || '--' }}</el-descriptions-item>
                        <el-descriptions-item label="结束日期">{{ course.endDate || '--' }}</el-descriptions-item>
                        <el-descriptions-item label="选课人数">{{ course.studentCount || 0 }} 人</el-descriptions-item>
                        <el-descriptions-item label="状态">
                            <el-tag :type="statusTagType(courseStatus)">{{ courseStatus }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="课程简介" :span="2">{{ course.description || '暂无课程简介'
                        }}</el-descriptions-item>
                    </el-descriptions>
                </el-tab-pane>

                <!-- 同班同学 -->
                <el-tab-pane label="同班同学" name="students">
                    <div class="students-header">
                        <span class="students-count">共 {{ students.length }} 位同学选课</span>
                    </div>
                    <div v-if="students.length > 0" class="student-grid">
                        <div v-for="s in students" :key="s.id" class="student-card">
                            <div class="student-avatar">
                                {{ (s.realName || s.studentNo || '?').charAt(0) }}
                            </div>
                            <div class="student-info">
                                <span class="student-name">{{ s.realName || '未知姓名' }}</span>
                                <span class="student-no">{{ s.studentNo || '' }}</span>
                            </div>
                            <div class="student-tags">
                                <el-tag v-if="s.major" size="small" effect="plain">{{ s.major }}</el-tag>
                                <el-tag v-if="s.className" size="small" type="info" effect="plain">{{ s.className
                                }}</el-tag>
                                <el-tag v-if="s.grade" size="small" type="warning" effect="plain">{{ s.grade }}</el-tag>
                            </div>
                        </div>
                    </div>
                    <el-empty v-else description="暂无同学选课" :image-size="90" />
                </el-tab-pane>

                <!-- 课程公告 -->
                <el-tab-pane label="课程公告" name="announcements">
                    <div v-if="announcements.length > 0" class="announcement-list">
                        <div v-for="a in announcements" :key="a.id" class="announcement-item"
                            @click="viewAnnouncement(a)">
                            <div class="announcement-item-header">
                                <span class="announcement-item-dot" :class="{ unread: !a.confirmed }"></span>
                                <span class="announcement-item-title">{{ a.title }}</span>
                                <el-tag v-if="a.confirmed" size="small" type="success">已确认</el-tag>
                                <el-tag v-else size="small" type="warning">待确认</el-tag>
                            </div>
                            <p class="announcement-item-desc">{{ truncateContent(a.content) }}</p>
                            <div class="announcement-item-meta">
                                <span><el-icon>
                                        <Clock />
                                    </el-icon> {{ formatDate(a.publishTime) }}</span>
                                <span><el-icon>
                                        <User />
                                    </el-icon> {{ a.publisherName || '教师' }}</span>
                            </div>
                        </div>
                    </div>
                    <el-empty v-else description="暂无课程公告" :image-size="90" />

                    <!-- 公告详情弹窗 -->
                    <el-dialog v-model="detailDialogVisible" :title="currentAnnouncement.title" width="650px"
                        destroy-on-close>
                        <div class="detail-meta" style="margin-bottom:12px">
                            <span><strong>发布时间：</strong>{{ formatDate(currentAnnouncement.publishTime) }}</span>
                            <span><strong>发布人：</strong>{{ currentAnnouncement.publisherName || '-' }}</span>
                            <span><strong>确认人数：</strong>{{ currentAnnouncement.confirmCount || 0 }}</span>
                        </div>
                        <el-divider />
                        <div class="detail-content" v-html="formatAnnounceContent(currentAnnouncement.content)"></div>
                        <template #footer>
                            <el-button @click="detailDialogVisible = false">关闭</el-button>
                            <el-button v-if="!currentAnnouncement.confirmed" type="primary"
                                :loading="confirmingAnnouncement"
                                @click="confirmAnnouncement(currentAnnouncement)">确认公告</el-button>
                            <el-button v-else type="success" disabled>已确认</el-button>
                        </template>
                    </el-dialog>
                </el-tab-pane>

                <!-- 章节内容（只读） -->
                <el-tab-pane label="章节内容" name="chapters">
                    <el-collapse v-model="openChapterNames" class="chapter-collapse">
                        <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :name="String(chapter.id)">
                            <template #title>
                                <div class="chapter-title">
                                    <span>{{ chapter.sortOrder }}. {{ chapter.chapterName }}</span>
                                    <span class="chapter-stat">
                                        {{ resourcesByChapter(chapter.id).length }} 个资源 ·
                                        {{ assignmentsByChapter(chapter.id).length }} 个作业
                                    </span>
                                </div>
                            </template>

                            <!-- 资源列表 -->
                            <div class="content-group">
                                <h4 class="group-title">📎 资源</h4>
                                <div v-if="resourcesByChapter(chapter.id).length" class="item-list">
                                    <div v-for="resource in resourcesByChapter(chapter.id)" :key="resource.id"
                                        class="content-item">
                                        <span class="item-kind">{{ resource.resourceType || '课件' }}</span>
                                        <span class="item-title">{{ resource.resourceName }}</span>
                                        <span class="item-meta">{{ formatFileSize(resource.fileSize) }}</span>
                                        <div class="item-actions">
                                            <el-button v-if="resource.fileUrl" size="small" type="primary" link
                                                @click="previewFile(resource)">预览</el-button>
                                        </div>
                                    </div>
                                </div>
                                <el-empty v-else description="本章节暂无资源" :image-size="70" />
                            </div>

                            <!-- 作业列表 -->
                            <div class="content-group">
                                <h4 class="group-title">📝 作业</h4>
                                <div v-if="assignmentsByChapter(chapter.id).length" class="item-list">
                                    <div v-for="assignment in assignmentsByChapter(chapter.id)" :key="assignment.id"
                                        class="content-item">
                                        <span class="item-kind assignment">作业</span>
                                        <span class="item-title">{{ assignment.title }}</span>
                                        <span class="item-meta">截止 {{ formatDate(assignment.deadline) }}</span>
                                        <el-button v-if="course.enrolled" size="small" type="primary"
                                            @click="router.push('/student/assignments')">去完成</el-button>
                                        <el-button v-else size="small" disabled>请先加入课程</el-button>
                                    </div>
                                </div>
                                <el-empty v-else description="本章节暂无作业" :image-size="70" />
                            </div>
                        </el-collapse-item>
                    </el-collapse>

                    <el-empty v-if="chapters.length === 0" description="暂无章节内容" :image-size="100" />
                </el-tab-pane>
            </el-tabs>
        </div>

        <!-- 资源预览弹窗 -->
        <el-dialog v-model="previewVisible" title="资源预览" width="80%" top="5vh" destroy-on-close>
            <FilePreview v-if="previewResource" :url="previewResource.fileUrl" :name="previewResource.resourceName"
                :type="previewResource.resourceType" />
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { announcementApi, assignmentApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, User } from '@element-plus/icons-vue'
import FilePreview from '@/components/FilePreview.vue'
import { computeStatus, statusTagType } from '@/utils/status'

const route = useRoute()
const router = useRouter()

const activeTab = ref(route.query.tab || 'info')
const course = ref({})
const chapters = ref([])
const resources = ref([])
const assignments = ref([])
const openChapterNames = ref([])
const previewVisible = ref(false)
const previewResource = ref(null)
const enrolling = ref(false)
const unenrolling = ref(false)
const students = ref([])

const courseId = () => route.params.id

const courseStatus = computed(() => computeStatus(course.value.startDate, course.value.endDate))

const canEnroll = computed(() => courseStatus.value === '进行中')

// 公告相关
const announcements = ref([])
const detailDialogVisible = ref(false)
const currentAnnouncement = ref({})
const confirmingAnnouncement = ref(false)

const fetchAnnouncements = async () => {
    try {
        const res = await announcementApi.list({ page: 1, size: 50, courseId: courseId() })
        if (res.data.code === 200) {
            announcements.value = res.data.data?.records || []
        }
    } catch (e) { /* ignore */ }
}

const viewAnnouncement = (a) => {
    currentAnnouncement.value = { ...a }
    detailDialogVisible.value = true
}

const confirmAnnouncement = async (a) => {
    confirmingAnnouncement.value = true
    try {
        await announcementApi.confirm(a.id)
        a.confirmed = true
        a.confirmCount = (a.confirmCount || 0) + 1
        currentAnnouncement.value.confirmed = true
        currentAnnouncement.value.confirmCount = (currentAnnouncement.value.confirmCount || 0) + 1
    } catch (e) { /* ignore */ }
    finally { confirmingAnnouncement.value = false }
}

const truncateContent = (content) => {
    if (!content) return '暂无内容'
    const text = content.replace(/<[^>]*>/g, '')
    return text.length > 120 ? text.substring(0, 120) + '...' : text
}

const formatAnnounceContent = (content) => {
    if (!content) return '<p style="color:#909399">暂无内容</p>'
    return content.replace(/\n/g, '<br>')
}

const resourcesByChapter = (chapterId) =>
    resources.value.filter((r) => r.chapterId === chapterId)

const assignmentsByChapter = (chapterId) =>
    assignments.value.filter((a) => a.chapterId === chapterId)

/** 获取课程详情 */
const fetchCourse = async () => {
    const res = await courseApi.detail(courseId())
    if (res.data.code === 200) {
        course.value = res.data.data || {}
        chapters.value = course.value.chapters || []
        openChapterNames.value = chapters.value.slice(0, 1).map((ch) => String(ch.id))
        // 如果接口返回了 enrolled 字段则直接使用
        // 否则在 loadAll 中通过 checkEnrolled 补充
    }
}

/** 检测当前学生是否已加入该课程 */
const checkEnrolled = async () => {
    // 优先使用课程详情中的 enrolled 字段
    if (course.value.enrolled !== undefined) return
    try {
        // 仅获取最近一页（size=20），已加入的课程通常排在最前
        const res = await courseApi.list({ page: 1, size: 20 })
        if (res.data.code === 200) {
            const data = res.data.data
            const list = Array.isArray(data?.records)
                ? data.records
                : Array.isArray(data?.list)
                    ? data.list
                    : Array.isArray(data)
                        ? data
                        : []
            const match = list.find((c) => c.id === course.value.id || c.id === Number(courseId()))
            if (match) {
                course.value.enrolled = !!match.enrolled
            }
        }
    } catch {
        // 静默处理，不影响主流程
    }
}

/** 获取课程资源 */
const fetchResources = async () => {
    const res = await courseApi.getResources(courseId())
    if (res.data.code === 200) resources.value = res.data.data || []
}

/** 获取课程作业 */
const fetchAssignments = async () => {
    const res = await assignmentApi.list({ page: 1, size: 200, courseId: courseId() })
    if (res.data.code === 200) assignments.value = res.data.data?.records || []
}

/** 获取选课同学 */
const fetchStudents = async () => {
    const res = await courseApi.getStudents(courseId(), { page: 1, size: 100 })
    if (res.data.code === 200) students.value = res.data.data?.records || []
}

const loadAll = async () => {
    try {
        await Promise.all([fetchCourse(), fetchResources(), fetchAssignments(), fetchStudents(), fetchAnnouncements()])
        // 课程详情接口可能不返回 enrolled，通过课程列表补充检测
        await checkEnrolled()
    } catch (e) {
        ElMessage.error('课程详情加载失败')
    }
}

/** 加入课程 */
const enrollCourse = async () => {
    if (course.value.enrolled) {
        ElMessage.info('已加入该课程')
        return
    }
    enrolling.value = true
    try {
        await courseApi.enroll(courseId())
        course.value.enrolled = true
        course.value.studentCount = Number(course.value.studentCount || 0) + 1
        ElMessage.success('选课成功，现在可以开始学习该课程')
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '选课失败')
    } finally {
        enrolling.value = false
    }
}

/** 退出课程 */
const unenrollCourse = async () => {
    try {
        await ElMessageBox.confirm('确定退出该课程吗？退出后将无法查看课程资源和提交作业。', '退出课程', {
            type: 'warning',
            confirmButtonText: '确定退出',
            cancelButtonText: '取消'
        })
        unenrolling.value = true
        await courseApi.unenroll(courseId())
        course.value.enrolled = false
        course.value.studentCount = Math.max(0, Number(course.value.studentCount || 0) - 1)
        ElMessage.success('已退出课程')
    } catch (e) {
        if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '退课失败')
    } finally {
        unenrolling.value = false
    }
}

/** 进入学习 */
const enterLearn = () => {
    if (!course.value.enrolled) {
        ElMessage.warning('请先加入课程')
        return
    }
    router.push(`/student/courses/${courseId()}/learn`)
}

/** 预览资源 */
const previewFile = (resource) => {
    previewResource.value = resource
    previewVisible.value = true
}

const formatDate = (value) =>
    value ? String(value).replace('T', ' ').slice(0, 16) : '--'

const formatFileSize = (size) => {
    const n = Number(size || 0)
    if (n < 1024) return `${n} B`
    if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
    return `${(n / 1024 / 1024).toFixed(1)} MB`
}

onMounted(loadAll)
</script>

<style scoped>
.page-shell {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

/* ---------- 标题栏 ---------- */
.page-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.page-subtitle {
    color: #909399;
    font-size: 13px;
    margin: 6px 0 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

/* ---------- 操作栏 ---------- */
.action-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 16px 24px;
}

.action-info {
    font-size: 15px;
}

.enrolled-badge {
    color: var(--bio-primary);
    font-weight: 500;
}

.unenrolled-badge {
    color: #909399;
}

.action-buttons {
    display: flex;
    gap: 12px;
}

/* ---------- 主面板 ---------- */
.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
}

.chapter-collapse :deep(.el-collapse-item__header) {
    height: 58px;
    font-size: 16px;
}

.chapter-title {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-right: 20px;
}

.chapter-stat {
    color: #909399;
    font-size: 13px;
    font-weight: 400;
}

.content-group {
    border-top: 1px solid #ebeef5;
    padding-top: 14px;
    margin-top: 14px;
}

.group-title {
    margin: 0 0 10px;
    color: #303133;
    font-size: 14px;
}

.item-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.content-item {
    display: grid;
    grid-template-columns: 56px minmax(160px, 1fr) minmax(180px, auto) auto;
    gap: 12px;
    align-items: center;
    padding: 12px 14px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 8px;
}

.item-kind {
    color: var(--bio-primary);
    background: var(--bio-primary-soft);
    border-radius: 4px;
    padding: 3px 8px;
    text-align: center;
    font-size: 12px;
}

/* ---------- 同班同学 ---------- */
.students-header {
    margin-bottom: 16px;
}

.students-count {
    color: #909399;
    font-size: 14px;
}

.student-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 12px;
}

.student-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    transition: all 0.25s;
}

.student-card:hover {
    background: #f0f5ff;
    border-color: #b3d8ff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
    transform: translateY(-2px);
}

.student-avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--bio-primary), var(--bio-primary-dark));
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 600;
    flex-shrink: 0;
}

.student-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
}

.student-name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.student-no {
    font-size: 12px;
    color: #909399;
}

.student-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    justify-content: flex-end;
}

.item-kind.assignment {
    color: #e6a23c;
    background: #fdf6ec;
}

.item-title {
    color: #303133;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.item-meta {
    color: #909399;
    font-size: 13px;
}

.item-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

/* ---------- 课程公告 ---------- */
.announcement-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.announcement-item {
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    cursor: pointer;
    transition: box-shadow 0.2s, border-color 0.2s;
}

.announcement-item:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    border-color: #b3d8ff;
}

.announcement-item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
}

.announcement-item-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #d4d8c8;
    flex-shrink: 0;
}

.announcement-item-dot.unread {
    background: var(--bio-primary);
}

.announcement-item-title {
    flex: 1;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.announcement-item-desc {
    font-size: 13px;
    color: #909399;
    margin: 0 0 8px;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.announcement-item-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #909399;
}

.announcement-item-meta span {
    display: flex;
    align-items: center;
    gap: 4px;
}

.detail-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    font-size: 13px;
    color: #606266;
}

.detail-content {
    font-size: 14px;
    line-height: 1.8;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 50vh;
    overflow-y: auto;
    padding: 8px 0;
}
</style>
