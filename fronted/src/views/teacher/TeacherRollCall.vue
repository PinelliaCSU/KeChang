<template>
  <div class="roll-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">课堂互动</p>
        <h2>随机点名</h2>
      </div>
      <div class="head-actions">
        <el-button :disabled="!form.courseId || !scoreboard.length" @click="exportScoreboard">导出积分表</el-button>
        <el-button type="primary" size="large" :loading="rolling" @click="rollCall">开始点名</el-button>
      </div>
    </div>

    <div class="layout">
      <section class="panel control-panel">
        <h3>点名设置</h3>
        <el-form label-position="top">
          <el-form-item label="课程">
            <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width:100%" @change="onCourseChange">
              <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="抽取人数">
            <el-input-number v-model="form.count" :min="1" :max="20" style="width:100%" />
          </el-form-item>
          <el-form-item label="给分说明">
            <el-input v-model="form.reason" placeholder="如：课堂回答、主动分享、未到扣分" />
          </el-form-item>
        </el-form>
        <div class="hint-box">
          <strong>操作方式</strong>
          <span>抽取后右击学生头像，可按回答情况选择 +1、+2、+3 或 -1。也可以用头像下方的快捷按钮。</span>
        </div>
      </section>

      <section class="stage-panel">
        <div v-if="selectedStudents.length" class="student-wall">
          <div v-for="student in selectedStudents" :key="student.studentId" class="student-card">
            <el-dropdown trigger="contextmenu" @command="score => awardPoint(student, score)">
              <div class="avatar-wrap">
                <el-avatar :size="84" :src="student.avatar || ''" class="student-avatar">
                  {{ avatarText(student) }}
                </el-avatar>
                <span class="course-point">{{ student.coursePoints ?? 0 }}分</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="1">+1 回答正确</el-dropdown-item>
                  <el-dropdown-item :command="2">+2 表现较好</el-dropdown-item>
                  <el-dropdown-item :command="3">+3 表现优秀</el-dropdown-item>
                  <el-dropdown-item divided :command="-1">-1 未到/未参与</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <h4>{{ student.studentName }}</h4>
            <p>{{ student.studentNo || '暂无学号' }} · {{ student.className || '班级未填' }}</p>
            <div class="quick-score">
              <el-button size="small" plain type="success" @click="awardPoint(student, 1)">+1</el-button>
              <el-button size="small" plain type="success" @click="awardPoint(student, 2)">+2</el-button>
              <el-button size="small" plain type="success" @click="awardPoint(student, 3)">+3</el-button>
              <el-button size="small" plain type="danger" @click="awardPoint(student, -1)">-1</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="选择课程后开始随机点名" :image-size="120" />
      </section>

      <section class="panel result-panel">
        <div class="panel-head">
          <h3>课程积分榜</h3>
          <el-tag v-if="scoreboard.length" type="success">{{ scoreboard.length }} 人</el-tag>
        </div>
        <div v-if="scoreboard.length" class="rank-list">
          <div v-for="(student, index) in scoreboard.slice(0, 12)" :key="student.studentId" class="rank-item">
            <span class="rank-no">{{ index + 1 }}</span>
            <div class="rank-name">
              <strong>{{ student.studentName }}</strong>
              <span>{{ student.studentNo || '暂无学号' }}</span>
            </div>
            <b>{{ student.coursePoints || 0 }}</b>
          </div>
        </div>
        <el-empty v-else description="暂无积分数据" :image-size="90" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { courseApi, participationApi } from '@/api'

const courses = ref([])
const rolling = ref(false)
const selectedStudents = ref([])
const scoreboard = ref([])
const form = reactive({
  courseId: null,
  count: 1,
  reason: '课堂互动'
})

const currentCourseName = () => courses.value.find(item => item.id === form.courseId)?.courseName || '课程'

const loadCourses = async () => {
  const res = await courseApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) {
    courses.value = res.data.data?.records || []
    if (!form.courseId && courses.value.length) {
      form.courseId = courses.value[0].id
      await loadScoreboard()
    }
  }
}

const loadScoreboard = async () => {
  if (!form.courseId) return
  const res = await participationApi.scoreboard(form.courseId)
  if (res.data.code === 200) {
    scoreboard.value = res.data.data || []
  }
}

const onCourseChange = async () => {
  selectedStudents.value = []
  await loadScoreboard()
}

const rollCall = async () => {
  if (!form.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  rolling.value = true
  try {
    const res = await participationApi.rollCall({
      courseId: form.courseId,
      count: form.count
    })
    if (res.data.code === 200) {
      selectedStudents.value = res.data.data?.students || []
      ElMessage.success(`已抽取 ${selectedStudents.value.length} 名学生`)
    }
  } finally {
    rolling.value = false
  }
}

const awardPoint = async (student, points) => {
  const res = await participationApi.award({
    courseId: form.courseId,
    studentId: student.studentId,
    points,
    reason: form.reason || (points > 0 ? '课堂回答加分' : '课堂未到/未参与扣分')
  })
  if (res.data.code === 200) {
    student.coursePoints = res.data.data.coursePoints
    ElMessage.success(`${student.studentName} ${points > 0 ? '+' : ''}${points} 分`)
    await loadScoreboard()
  }
}

const avatarText = (student) => {
  const name = student.studentName || student.studentNo || '学'
  return name.slice(-2)
}

const exportScoreboard = () => {
  const headers = ['排名', '学号', '姓名', '专业', '班级', '课程积分']
  const rows = scoreboard.value.map((student, index) => [
    index + 1,
    student.studentNo || '',
    student.studentName || '',
    student.major || '',
    student.className || '',
    student.coursePoints || 0
  ])
  const escapeCell = value => `"${String(value).replace(/"/g, '""')}"`
  const csv = [headers, ...rows].map(row => row.map(escapeCell).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentCourseName()}-课堂积分表.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(loadCourses)
</script>

<style scoped>
.roll-page { display: flex; flex-direction: column; gap: 18px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.head-actions { display: flex; gap: 10px; }
.eyebrow { margin: 0 0 4px; color: #7c8a63; font-size: 13px; font-weight: 700; }
.page-head h2 { margin: 0; color: var(--bio-ink); font-size: 25px; }
.layout { display: grid; grid-template-columns: 320px minmax(0, 1fr) 320px; gap: 18px; align-items: stretch; }
.panel, .stage-panel { background: #fff; border: 1px solid #edf0e8; border-radius: 8px; box-shadow: 0 10px 28px rgba(42, 54, 34, .06); padding: 20px; }
.panel h3 { margin: 0 0 16px; color: #2d3327; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.panel-head h3 { margin: 0; }
.hint-box { display: flex; flex-direction: column; gap: 8px; padding: 12px; border-radius: 8px; background: var(--bio-primary-soft); color: var(--bio-ink-soft); line-height: 1.6; }
.stage-panel { min-height: 560px; background: #fff; }
.student-wall { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; align-content: start; }
.student-card { min-height: 220px; padding: 18px 14px; border-radius: 8px; background: #fff; border: 1px solid var(--bio-border); text-align: center; transition: .2s; }
.student-card:hover { transform: none; box-shadow: none; border-color: var(--bio-primary); }
.avatar-wrap { display: inline-flex; flex-direction: column; align-items: center; gap: 8px; cursor: context-menu; }
.student-avatar { background: var(--bio-primary); color: #fff; font-size: 22px; font-weight: 700; }
.course-point { padding: 3px 10px; border-radius: 999px; background: var(--bio-primary-soft); color: var(--bio-primary); font-weight: 700; font-size: 12px; }
.student-card h4 { margin: 10px 0 6px; color: var(--bio-ink); font-size: 19px; }
.student-card p { margin: 0; color: var(--bio-ink-muted); font-size: 13px; }
.quick-score { display: flex; justify-content: center; gap: 6px; margin-top: 14px; flex-wrap: wrap; }
.rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank-item { display: grid; grid-template-columns: 34px 1fr auto; align-items: center; gap: 10px; padding: 10px; border-radius: 8px; background: #f8faf5; }
.rank-no { width: 28px; height: 28px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; background: #e7efdd; color: #557341; font-weight: 800; }
.rank-name { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.rank-name strong { color: #2d3327; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-name span { color: #8a927f; font-size: 12px; }
.rank-item b { color: var(--bio-primary); font-size: 18px; }
@media (max-width: 1180px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
