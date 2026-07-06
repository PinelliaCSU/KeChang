<template>
  <div class="course-detail">
    <div class="page-header">
      <h2>📚 {{ course.courseName || '课程详情' }}</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
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
        <el-button type="primary" size="small" @click="showChapterDialog = true" style="margin-bottom:12px">+ 添加章节</el-button>
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
        <el-empty description="请在作业管理页面查看" />
      </el-tab-pane>
      <el-tab-pane label="考试" name="exams">
        <el-empty description="请在考试管理页面查看" />
      </el-tab-pane>
      <el-tab-pane label="成绩" name="grades">
        <el-empty description="请在成绩管理页面查看" />
      </el-tab-pane>
    </el-tabs>

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
import { courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute(); const router = useRouter()
const activeTab = ref('info')
const course = ref({})
const chapters = ref([])
const students = ref([])
const showChapterDialog = ref(false)
const chapterForm = reactive({ chapterName: '', sortOrder: 1, description: '' })

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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { color: #303133; }
</style>
