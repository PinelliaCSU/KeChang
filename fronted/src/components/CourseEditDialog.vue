<template>
  <el-dialog
    v-model="visibleProxy"
    :title="isEdit ? '编辑课程' : '创建课程'"
    width="640px"
    class="course-edit-dialog"
    destroy-on-close
  >
    <div v-loading="loading" class="course-dialog-body">
      <el-form :model="form" label-width="92px" class="course-form">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.courseName" placeholder="请输入课程名称" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="课程简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入课程简介"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="封面图片">
          <div class="cover-upload">
            <div
              v-if="form.coverImg"
              class="cover-preview"
              :style="{ backgroundImage: `url(${resolveFileUrl(form.coverImg)})` }"
            >
              <el-button class="cover-remove" size="small" circle @click="form.coverImg = ''">×</el-button>
            </div>
            <div v-else class="cover-placeholder" @click="triggerUpload">
              <el-icon :size="24"><Plus /></el-icon>
              <span>点击上传封面</span>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-input" @change="handleFileChange" />
            <p class="cover-hint">建议尺寸 1200×600px，支持 JPG/PNG</p>
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期" required>
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" required>
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="课程状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visibleProxy = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveCourse">
        {{ isEdit ? '保存修改' : '确定创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { courseApi, uploadApi } from '@/api'
import { resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  course: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const saving = ref(false)
const fileInput = ref(null)

const emptyForm = () => ({
  courseName: '',
  coverImg: '',
  description: '',
  startDate: '',
  endDate: '',
  status: '进行中'
})

const form = reactive(emptyForm())
const isEdit = computed(() => Boolean(props.course?.id))

const visibleProxy = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const fillForm = () => {
  const course = props.course || {}
  Object.assign(form, {
    courseName: course.courseName || '',
    coverImg: course.coverImg || '',
    description: course.description || '',
    startDate: course.startDate || '',
    endDate: course.endDate || '',
    status: course.status || '进行中'
  })
}

watch(
  () => props.modelValue,
  visible => {
    if (visible) fillForm()
  }
)

watch(
  () => props.course,
  () => {
    if (props.modelValue) fillForm()
  }
)

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async event => {
  const file = event.target.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)
  loading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      form.coverImg = res.data.data?.url || res.data.data
      ElMessage.success('封面上传成功')
    } else {
      ElMessage.error(res.data.message || '封面上传失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '封面上传失败')
  } finally {
    loading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const saveCourse = async () => {
  if (!form.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择课程起止日期')
    return
  }

  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await courseApi.update(props.course.id, payload)
      ElMessage.success('课程已更新')
    } else {
      await courseApi.create(payload)
      ElMessage.success('课程创建成功')
    }
    visibleProxy.value = false
    emit('saved')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存课程失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.course-dialog-body {
  min-height: 240px;
}

.course-form {
  max-width: 100%;
}

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.cover-preview,
.cover-placeholder {
  width: 100%;
  height: 180px;
  border: 1px solid var(--bio-border);
  border-radius: 8px;
}

.cover-preview {
  position: relative;
  background-size: cover;
  background-position: center;
}

.cover-remove {
  position: absolute;
  top: 10px;
  right: 10px;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #f7f8fa;
  color: var(--bio-ink-muted);
  cursor: pointer;
  transition: border-color 160ms ease, color 160ms ease, background-color 160ms ease;
}

.cover-placeholder:hover {
  border-color: var(--bio-primary);
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
}

.cover-hint {
  margin: 0;
  color: var(--bio-ink-muted);
  font-size: 13px;
}

.file-input {
  display: none;
}

@media (max-width: 700px) {
  .course-edit-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
  }

  .course-form :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
