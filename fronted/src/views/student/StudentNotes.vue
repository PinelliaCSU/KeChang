<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div class="title-left">
        <el-button text @click="$router.push('/student/knowledge')">
          <el-icon>
            <ArrowLeft />
          </el-icon> 返回
        </el-button>
        <h2 class="page-title">学习笔记</h2>
      </div>
      <el-button type="primary" size="default" @click="showCreate = true">+ 新建笔记</el-button>
    </div>

    <div class="panel" v-if="notes.length > 0">
      <div class="note-grid">
        <div class="note-card" v-for="n in notes" :key="n.id">
          <h4 class="note-title">{{ n.title }}</h4>
          <p class="note-preview" v-html="notePreviewHtml(n.content)"></p>
          <div class="note-footer">
            <span class="note-course">{{ n.courseName || '' }}</span>
            <span class="note-time">{{ n.createTime || '' }}</span>
          </div>
          <div class="note-actions" @click.stop>
            <el-button size="small" text @click="editNote(n)"><el-icon>
                <Edit />
              </el-icon></el-button>
            <el-button size="small" text type="danger" @click="deleteNote(n)"><el-icon>
                <Delete />
              </el-icon></el-button>
          </div>
        </div>
      </div>
      <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" :page-sizes="[12, 24, 48]"
        @change="fetchNotes" />
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration text="暂无笔记，去课程学习中记录吧" />
    </div>

    <!-- 新建笔记弹窗 -->
    <el-dialog v-model="showCreate" title="新建笔记" width="750px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题" required><el-input v-model="createForm.title" /></el-form-item>
      </el-form>
      <div class="editor-section">
        <label class="editor-label">内容</label>
        <RichTextEditor ref="createEditorRef" :modelValue="createForm.content" placeholder="在此编写笔记内容..." />
      </div>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑笔记弹窗 -->
    <el-dialog v-model="showEdit" title="编辑笔记" width="750px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题" required><el-input v-model="editForm.title" /></el-form-item>
      </el-form>
      <div class="editor-section">
        <label class="editor-label">内容</label>
        <RichTextEditor ref="editEditorRef" :modelValue="editForm.content" placeholder="在此编写笔记内容..." />
      </div>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const notes = ref([])
const showCreate = ref(false)
const showEdit = ref(false)
const createForm = ref({ title: '', content: '' })
const editForm = ref({ title: '', content: '' })
const editId = ref(null)
const submitting = ref(false)
const saving = ref(false)
const createEditorRef = ref(null)
const editEditorRef = ref(null)

const { page, size, total, resetPage } = usePagination({ defaultSize: 12 })

const fetchNotes = async () => {
  try {
    const res = await knowledgeApi.listNotes({ page: page.value, size: size.value })
    if (res.data.code === 200) {
      const d = res.data.data
      notes.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
      total.value = d?.total || 0
    }
  } catch (e) { /* ignore */ }
}

const doCreate = async () => {
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  submitting.value = true
  try {
    const content = createEditorRef.value?.getHTML() || ''
    await knowledgeApi.createNote({ title: createForm.value.title, content })
    ElMessage.success('笔记创建成功')
    showCreate.value = false
    createForm.value = { title: '', content: '' }
    fetchNotes()
  } catch (e) { ElMessage.error('创建失败') }
  submitting.value = false
}

const editNote = (item) => {
  editId.value = item.id
  editForm.value = { title: item.title, content: item.content || '' }
  showEdit.value = true
}

const doEdit = async () => {
  if (!editForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  saving.value = true
  try {
    const content = editEditorRef.value?.getHTML() || ''
    await knowledgeApi.updateNote(editId.value, { title: editForm.value.title, content })
    ElMessage.success('笔记修改成功')
    showEdit.value = false
    fetchNotes()
  } catch (e) { ElMessage.error('修改失败') }
  saving.value = false
}

const notePreviewHtml = (content) => {
  if (!content) return '<span style="color:#b7bfcc">暂无内容</span>'
  const text = content.replace(/<[^>]*>/g, '')
  return text.substring(0, 120) || '<span style="color:#b7bfcc">暂无内容</span>'
}

const deleteNote = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该笔记？', '确认删除', { type: 'warning' })
    await knowledgeApi.deleteNote(item.id)
    ElMessage.success('已删除')
    fetchNotes()
  } catch (e) { /* cancelled */ }
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

.title-left {
  display: flex;
  align-items: center;
  gap: 8px;
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
  position: relative;
}

.note-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.note-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  margin-top: 4px;
}

.note-card:hover .note-actions {
  opacity: 1;
}

.editor-section {
  margin-top: 16px;
}

.editor-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
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
