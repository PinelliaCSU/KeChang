<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div class="title-left">
        <el-button text @click="$router.push('/student/knowledge')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 class="page-title">我的资料</h2>
      </div>
      <div class="title-actions">
        <el-input v-model="keyword" placeholder="搜索资料..." size="default" style="width:220px" clearable
          @clear="searchMaterials" />
        <el-select v-model="filterCourse" placeholder="全部课程" size="default" style="width:160px" clearable
          @change="searchMaterials">
          <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-button type="primary" size="default" @click="showUpload = true">+ 上传资料</el-button>
      </div>
    </div>

    <div class="panel" v-loading="loading" v-if="materials.length > 0">
      <div class="material-grid">
        <div class="material-card" v-for="item in materials" :key="item.id" @click="previewItem(item)">
          <div class="m-icon">
            <el-icon :size="32" v-if="item.fileType === 'PDF' || item.fileType === 'pdf'" color="#ef476f">
              <Document />
            </el-icon>
            <el-icon :size="32" v-else-if="item.fileType === 'IMAGE' || isImage(item.fileType)" color="#06d6a0">
              <PictureFilled />
            </el-icon>
            <el-icon :size="32" v-else-if="item.fileType === 'PPT' || item.fileType === 'pptx'" color="#ffd166">
              <DataAnalysis />
            </el-icon>
            <el-icon :size="32" v-else-if="item.fileType === 'DOCX' || item.fileType === 'doc'" color="#118ab2">
              <Reading />
            </el-icon>
            <el-icon :size="32" v-else color="#8a94a6">
              <Folder />
            </el-icon>
          </div>
          <h4 class="m-title">{{ item.title }}</h4>
          <p class="m-desc">{{ item.description?.substring(0, 80) || '暂无描述' }}</p>
          <div class="m-tags" v-if="parseTags(item.tags).length > 0">
            <el-tag v-for="tag in parseTags(item.tags)" :key="tag" size="small" type="info">{{ tag }}</el-tag>
          </div>
          <div class="m-footer">
            <el-tag size="small" type="info">{{ item.fileType || '文件' }}</el-tag>
            <span class="m-course">{{ item.courseName || '未分类' }}</span>
          </div>
          <div class="m-time">{{ item.createTime || '' }}</div>
          <div class="m-actions" @click.stop>
            <el-button size="small" text @click="editItem(item)"><el-icon><Edit /></el-icon></el-button>
            <el-button size="small" text type="danger" @click="deleteItem(item)"><el-icon><Delete /></el-icon></el-button>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="total > size">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
          v-model:current-page="page" @current-change="fetchMaterials" />
      </div>
    </div>

    <div class="panel empty-panel" v-else-if="!loading">
      <EmptyIllustration text="还没有上传学习资料" />
    </div>

    <!-- 上传弹窗 -->
    <el-dialog v-model="showUpload" title="上传资料" width="520px" destroy-on-close @closed="resetUploadForm">
      <el-form label-width="80px">
        <el-form-item label="选择文件" required>
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="handleFileChange" drag
            accept=".pdf,.ppt,.pptx,.doc,.docx,.jpg,.jpeg,.png,.gif,.bmp,.webp,.txt,.md" :file-list="fileList">
            <el-icon :size="40"><UploadFilled /></el-icon>
            <div class="upload-text">将文件拖到此处，或<em>点击选择</em></div>
            <template #tip><div class="upload-tip">支持 PDF、PPT、Word、图片、文本等格式</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="uploadForm.title" placeholder="资料标题（默认使用文件名）" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="添加简要描述..." /></el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="uploadForm.courseId" placeholder="选择课程（可选）" style="width:100%" clearable>
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="uploadForm.tags" placeholder="多个标签用英文逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEdit" title="编辑资料" width="480px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="editForm.tags" placeholder="多个标签用英文逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="showPreview" :title="previewItemData?.title || '文件预览'" width="780px" destroy-on-close>
      <FilePreview v-if="previewItemData" :url="previewItemData.fileUrl" :name="previewItemData.fileName || previewItemData.title"
        :type="previewItemData.fileType" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { knowledgeApi, courseApi, uploadApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import FilePreview from '@/components/FilePreview.vue'

const materials = ref([])
const courses = ref([])
const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const keyword = ref('')
const filterCourse = ref(null)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const showUpload = ref(false)
const showEdit = ref(false)
const showPreview = ref(false)
const previewItemData = ref(null)
const uploadRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)

const uploadForm = ref({ title: '', description: '', courseId: null, tags: '' })
const editForm = ref({ title: '', description: '', tags: '' })
const editId = ref(null)

const parseTags = (tags) => {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

const isImage = (type) => {
  if (!type) return false
  return ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'svg'].includes(type.toLowerCase())
}

let searchTimer = null
const searchMaterials = () => {
  page.value = 1
  fetchMaterials()
}

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => searchMaterials(), 300)
})

const fetchMaterials = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (filterCourse.value) params.courseId = filterCourse.value
    const res = await knowledgeApi.myMaterials(params)
    if (res.data.code === 200) {
      const d = res.data.data
      materials.value = d?.records || []
      total.value = d?.total || 0
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) {
      courses.value = res.data.data?.records || []
    }
  } catch (e) { /* ignore */ }
}

const handleFileChange = (file) => {
  selectedFile.value = file?.raw || file
  if (!uploadForm.value.title) {
    uploadForm.value.title = (file?.name || '').replace(/\.[^.]+$/, '')
  }
}

const resetUploadForm = () => {
  uploadForm.value = { title: '', description: '', courseId: null, tags: '' }
  selectedFile.value = null
  fileList.value = []
}

const doUpload = async () => {
  if (!selectedFile.value) { ElMessage.warning('请选择文件'); return }
  if (!uploadForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const upRes = await uploadApi.uploadFile(formData)
    if (upRes.data.code !== 200) { ElMessage.error('文件上传失败'); uploading.value = false; return }

    const { url, name, size } = upRes.data.data
    const ext = (name || '').split('.').pop()?.toUpperCase() || 'OTHER'
    let fileType = ['PNG', 'JPG', 'JPEG', 'GIF', 'BMP', 'WEBP', 'SVG'].includes(ext) ? 'IMAGE' : ext

    const course = courses.value.find(c => c.id === uploadForm.value.courseId)
    await knowledgeApi.createMaterial({
      title: uploadForm.value.title,
      description: uploadForm.value.description,
      fileUrl: url, fileName: name, fileType, fileSize: size,
      courseId: uploadForm.value.courseId,
      courseName: course?.courseName || null,
      tags: uploadForm.value.tags
    })
    ElMessage.success('资料上传成功')
    showUpload.value = false
    resetUploadForm()
    fetchMaterials()
  } catch (e) { ElMessage.error('上传失败') }
  uploading.value = false
}

const previewItem = (item) => {
  previewItemData.value = item
  showPreview.value = true
}

const editItem = (item) => {
  editId.value = item.id
  editForm.value = { title: item.title, description: item.description || '', tags: item.tags || '' }
  showEdit.value = true
}

const doEdit = async () => {
  if (!editForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  saving.value = true
  try {
    await knowledgeApi.updateMaterial(editId.value, editForm.value)
    ElMessage.success('修改成功')
    showEdit.value = false
    fetchMaterials()
  } catch (e) { ElMessage.error('修改失败') }
  saving.value = false
}

const deleteItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该资料？', '确认删除', { type: 'warning' })
    await knowledgeApi.deleteMaterial(item.id)
    ElMessage.success('已删除')
    fetchMaterials()
  } catch (e) { /* cancelled */ }
}

onMounted(() => { fetchCourses(); fetchMaterials() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.title-left { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.title-actions { display: flex; gap: 10px; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 24px; }
.material-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.material-card { background: #fafbfc; border: 1px solid #ebeef5; border-radius: 10px; padding: 20px 16px; cursor: pointer; transition: all 0.3s; position: relative; }
.material-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); transform: translateY(-2px); }
.m-icon { margin-bottom: 10px; }
.m-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0 0 8px; }
.m-desc { font-size: 12px; color: #909399; line-height: 1.5; margin: 0 0 8px; min-height: 36px; }
.m-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 10px; }
.m-footer { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.m-course { font-size: 12px; color: #c0c4cc; }
.m-time { font-size: 11px; color: #c0c4cc; margin-bottom: 8px; }
.m-actions { display: flex; justify-content: flex-end; gap: 4px; opacity: 0; transition: opacity 0.2s; }
.material-card:hover .m-actions { opacity: 1; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.empty-panel { display: flex; align-items: center; justify-content: center; padding: 60px 24px; }
.upload-text { color: #909399; font-size: 14px; margin-top: 8px; }
.upload-tip { color: #c0c4cc; font-size: 12px; margin-top: 4px; }
</style>
