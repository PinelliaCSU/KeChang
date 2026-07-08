<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">学习笔记</h2>
      <el-button type="primary" size="default" @click="showCreate = true">+ 新建笔记</el-button>
    </div>

    <div class="panel" v-if="notes.length > 0">
      <div class="note-grid">
        <div class="note-card" v-for="n in notes" :key="n.id">
          <h4 class="note-title">{{ n.title }}</h4>
          <p class="note-preview">{{ (n.content || '').substring(0, 120) || '暂无内容' }}</p>
          <div class="note-footer">
            <span class="note-course">{{ n.courseName || '' }}</span>
            <span class="note-time">{{ n.createTime || '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration text="暂无笔记，去课程学习中记录吧" />
    </div>

    <!-- 新建笔记弹窗 -->
    <el-dialog v-model="showCreate" title="新建笔记" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题" required><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="createForm.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api'
import { ElMessage } from 'element-plus'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const notes = ref([])
const showCreate = ref(false)
const createForm = ref({ title: '', content: '' })
const submitting = ref(false)

const fetchNotes = async () => {
  try {
    const res = await knowledgeApi.listNotes({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      notes.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
}

const doCreate = async () => {
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  submitting.value = true
  try {
    await knowledgeApi.createNote(createForm.value)
    ElMessage.success('笔记创建成功')
    showCreate.value = false
    createForm.value = { title: '', content: '' }
    fetchNotes()
  } catch (e) { ElMessage.error('创建失败') }
  submitting.value = false
}

onMounted(fetchNotes)
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

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.note-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.note-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.note-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.note-preview {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 12px;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #c0c4cc;
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

.empty-illustration {
  opacity: 0.6;
}

.empty-text {
  color: #c0c4cc;
  font-size: 14px;
}
</style>
