<template>
  <div class="page-container">
    <h2>📊 我的成绩</h2>
    <el-row :gutter="20">
      <el-col :span="16">
        <el-table :data="grades" border stripe>
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="examAvgScore" label="考试均分" width="100" />
          <el-table-column prop="assignmentAvgScore" label="作业均分" width="100" />
          <el-table-column prop="totalScore" label="总评" width="100" />
          <el-table-column prop="gradeLevel" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="levelType(row.gradeLevel)">{{ row.gradeLevel }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>🕸 能力雷达图</template>
          <div ref="radarRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { gradeApi } from '@/api'
import * as echarts from 'echarts'

const grades = ref([])
const radarRef = ref(null)

const levelType = (level) => {
  const map = { '优秀': 'success', '良好': '', '中等': 'warning', '及格': '', '不及格': 'danger' }
  return map[level] || 'info'
}

onMounted(async () => {
  try {
    const res = await gradeApi.myGrades()
    if (res.data.code === 200) grades.value = res.data.data || []
  } catch (e) { /* ignore */ }

  await nextTick()
  if (radarRef.value) {
    const chart = echarts.init(radarRef.value)
    chart.setOption({
      radar: { indicator: [
        { name: '知识点A', max: 100 }, { name: '知识点B', max: 100 },
        { name: '知识点C', max: 100 }, { name: '知识点D', max: 100 }
      ]},
      series: [{ type: 'radar', data: [{ value: [85, 72, 90, 68], name: '能力值' }],
        areaStyle: { color: 'rgba(24,144,255,0.2)' } }]
    })
  }
})
</script>

<style scoped>
.page-container h2 { margin-bottom: 20px; color: #303133; }
</style>
