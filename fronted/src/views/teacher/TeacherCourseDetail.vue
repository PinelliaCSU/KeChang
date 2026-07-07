<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">{{ course.courseName || '课程详情' }}</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="课程信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程名称">{{ course.courseName }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ course.teacherName }}</el-descriptions-item>
            <el-descriptions-item label="开始日期">{{ course.startDate }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ course.endDate }}</el-descriptions-item>
            <el-descriptions-item label="选课人数">{{ course.studentCount }}人</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="course.status === '进行中' ? 'success' : 'info'">{{ course.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="课程简介" :span="2">{{ course.description }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="章节管理" name="chapters">
          <el-button type="primary" size="small" @click="showChapterDialog = true" style="margin-bottom:12px">+
            添加章节</el-button>
          <el-table :data="chapters" border stripe>
            <el-table-column prop="sortOrder" label="序号" width="60" />
            <el-table-column prop="chapterName" label="章节名称" />
            <el-table-column prop="resourceCount" label="资源数" width="80" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" @click="editChapter(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteChapter(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="资源管理" name="resources">
          <el-upload action="#" :http-request="uploadResource" :show-file-list="false">
            <el-button type="primary">上传资源</el-button>
          </el-upload>
          <p style="color:#909399;margin-top:12px">支持视频、PDF、PPT等格式</p>
        </el-tab-pane>

        <el-tab-pane label="学生管理" name="students">
          <el-table :data="students" border stripe>
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="className" label="班级" width="100" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="作业" name="assignments">
          <div v-if="loadingAssignments" v-loading="loadingAssignments" style="min-height:100px"></div>
          <el-table v-else-if="courseAssignments.length > 0" :data="courseAssignments" border stripe>
            <el-table-column prop="title" label="作业标题" min-width="180" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="deadline" label="截止时间" width="170" />
            <el-table-column label="提交情况" width="110">
              <template #default="{ row }">{{ row.submitCount || 0 }}/{{ row.totalStudents || 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">{{ row.status || '进行中'
                  }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="$router.push('/teacher/assignments')">管理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无作业" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="考试" name="exams">
          <div v-if="loadingExams" v-loading="loadingExams" style="min-height:100px"></div>
          <el-table v-else-if="courseExams.length > 0" :data="courseExams" border stripe>
            <el-table-column prop="examName" label="考试名称" min-width="180" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column prop="endTime" label="结束时间" width="170" />
            <el-table-column prop="duration" label="时长(分)" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '进行中' ? 'success' : row.status === '未开始' ? 'warning' : 'info'"
                  size="small">{{ row.status || '未开始' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="$router.push('/teacher/exams')">管理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无考试安排" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="成绩" name="grades">
          <div v-if="loadingGrades" v-loading="loadingGrades" style="min-height:100px"></div>
          <el-table v-else-if="courseGrades.length > 0" :data="courseGrades" border stripe>
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="examAvgScore" label="考试均分" width="100" />
            <el-table-column prop="assignmentAvgScore" label="作业均分" width="100" />
            <el-table-column prop="totalScore" label="总评" width="80">
              <template #default="{ row }">
                <b :style="{ color: row.totalScore >= 90 ? '#67c23a' : row.totalScore >= 60 ? '#409eff' : '#f56c6c' }">{{
                  row.totalScore }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="gradeLevel" label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="levelType(row.gradeLevel)" size="small">{{ row.gradeLevel || '--' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无成绩数据" :image-size="80" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加章节弹窗 -->
    <el-dialog v-model="showChapterDialog" title="添加章节" width="400px">
      <el-form :model="chapterForm" label-width="80px">
        <el-form-item label="章节名称"><el-input v-model="chapterForm.chapterName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="chapterForm.sortOrder" :min="1" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="chapterForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChapterDialog = false">取消</el-button>
        <el-button type="primary" @click="addChapter">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi, assignmentApi, examApi, gradeApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute(); const router = useRouter()
const activeTab = ref('info')
const course = ref({})
const chapters = ref([])
const students = ref([])
const showChapterDialog = ref(false)
const chapterForm = reactive({ chapterName: '', sortOrder: 1, description: '' })

// 课程维度的作业/考试/成绩
const courseAssignments = ref([])
const courseExams = ref([])
const courseGrades = ref([])
const loadingAssignments = ref(false)
const loadingExams = ref(false)
const loadingGrades = ref(false)

const levelType = (level) => {
  const m = { '优秀': 'success', '良好': '', '中等': 'warning', '及格': '', '不及格': 'danger' }
  return m[level] || 'info'
}

const fetchTabData = async (tabName) => {
  const courseId = route.params.id
  if (tabName === 'assignments') {
    loadingAssignments.value = true
    try {
      const res = await assignmentApi.list({ courseId, size: 50 })
      if (res.data.code === 200) {
        const d = res.data.data
        courseAssignments.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
      }
    } catch (e) { /* ignore */ }
    loadingAssignments.value = false
  } else if (tabName === 'exams') {
    loadingExams.value = true
    try {
      const res = await examApi.listExams({ courseId, size: 50 })
      if (res.data.code === 200) {
        const d = res.data.data
        courseExams.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
      }
    } catch (e) { /* ignore */ }
    loadingExams.value = false
  } else if (tabName === 'grades') {
    loadingGrades.value = true
    try {
      const res = await gradeApi.courseGrades(courseId)
      if (res.data.code === 200) {
        courseGrades.value = res.data.data || []
      }
    } catch (e) { /* ignore */ }
    loadingGrades.value = false
  }
}

onMounted(async () => {
  try {
    const res = await courseApi.detail(route.params.id)
    if (res.data.code === 200) {
      course.value = res.data.data
      chapters.value = res.data.data.chapters || []
    }
    const sRes = await courseApi.getStudents(route.params.id, { page: 1, size: 50 })
    if (sRes.data.code === 200) students.value = sRes.data.data.records || []
  } catch (e) { /* ignore */ }
})

// 切换tab时加载数据
import { watch } from 'vue'
watch(activeTab, (tab) => {
  if (tab === 'assignments' || tab === 'exams' || tab === 'grades') {
    fetchTabData(tab)
  }
})

const addChapter = async () => {
  try {
    await courseApi.addChapter(route.params.id, chapterForm)
    ElMessage.success('章节添加成功')
    showChapterDialog.value = false
    const res = await courseApi.detail(route.params.id)
    if (res.data.code === 200) chapters.value = res.data.data.chapters || []
  } catch (e) { /* ignore */ }
}

const deleteChapter = async (chapterId) => {
  try {
    await courseApi.deleteChapter(route.params.id, chapterId)
    ElMessage.success('已删除')
    chapters.value = chapters.value.filter(c => c.id !== chapterId)
  } catch (e) { /* ignore */ }
}

const uploadResource = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    await courseApi.uploadResource(route.params.id, formData)
    ElMessage.success('上传成功')
  } catch (e) { /* ignore */ }
}
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
</style>
