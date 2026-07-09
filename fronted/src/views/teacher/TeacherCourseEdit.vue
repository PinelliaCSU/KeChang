<template>
    <div class="page-shell">
        <div class="page-title-bar">
            <div>
                <h2 class="page-title">{{ isNew ? '创建课程' : '编辑课程' }}</h2>
                <p class="page-subtitle">{{ isNew ? '填写课程基本信息' : '修改「' + (course.courseName || '') + '」的基本信息' }}</p>
            </div>
            <el-button @click="goBack">返回</el-button>
        </div>

        <div class="panel" v-loading="loading">
            <el-form :model="form" label-width="100px" class="edit-form">
                <el-form-item label="课程名称" required>
                    <el-input v-model="form.courseName" placeholder="请输入课程名称" maxlength="100" show-word-limit />
                </el-form-item>

                <el-form-item label="课程简介">
                    <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入课程简介" maxlength="500"
                        show-word-limit />
                </el-form-item>

                <el-form-item label="封面图片">
                    <div class="cover-upload">
                        <div v-if="form.coverImg" class="cover-preview"
                            :style="{ backgroundImage: `url(${resolveFileUrl(form.coverImg)})` }">
                            <el-button class="cover-remove" size="small" circle
                                @click="form.coverImg = ''">×</el-button>
                        </div>
                        <div v-else class="cover-placeholder" @click="triggerUpload">
                            <el-icon :size="24">
                                <Plus />
                            </el-icon>
                            <span>点击上传封面</span>
                        </div>
                        <input ref="fileInput" type="file" accept="image/*" style="display:none"
                            @change="handleFileChange" />
                        <p class="cover-hint">建议尺寸 1200×600px，支持 JPG/PNG</p>
                    </div>
                </el-form-item>

                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="开始日期" required>
                            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD"
                                placeholder="选择开始日期" style="width:100%" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="结束日期" required>
                            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD"
                                placeholder="选择结束日期" style="width:100%" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="课程状态">
                    <el-select v-model="form.status" style="width:100%">
                        <el-option label="进行中" value="进行中" />
                        <el-option label="已结束" value="已结束" />
                    </el-select>
                </el-form-item>

                <el-divider />

                <div class="form-actions">
                    <el-button @click="goBack">取消</el-button>
                    <el-button type="primary" :loading="saving" @click="saveCourse">
                        {{ isNew ? '创建课程' : '保存修改' }}
                    </el-button>
                </div>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { resolveFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const fileInput = ref(null)
const course = ref({})

const isNew = computed(() => !route.params.id || route.params.id === 'new')

const form = reactive({
    courseName: '',
    coverImg: '',
    description: '',
    startDate: '',
    endDate: '',
    status: '进行中'
})

const goBack = () => {
    if (isNew.value) {
        router.push('/teacher/courses')
    } else {
        router.push(`/teacher/courses/${route.params.id}`)
    }
}

const fetchCourse = async () => {
    if (isNew.value) return
    loading.value = true
    try {
        const res = await courseApi.detail(route.params.id)
        if (res.data.code === 200) {
            const data = res.data.data || {}
            course.value = data
            form.courseName = data.courseName || ''
            form.coverImg = data.coverImg || ''
            form.description = data.description || ''
            form.startDate = data.startDate || ''
            form.endDate = data.endDate || ''
            form.status = data.status || '进行中'
        }
    } catch (e) {
        ElMessage.error('加载课程信息失败')
    } finally {
        loading.value = false
    }
}

const triggerUpload = () => {
    fileInput.value?.click()
}

const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    try {
        const res = await uploadApi.uploadFile(formData)
        if (res.data.code === 200) {
            form.coverImg = res.data.data.url
        }
    } catch (e) {
        ElMessage.error('封面上传失败')
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
        if (isNew.value) {
            await courseApi.create({ ...form })
            ElMessage.success('课程创建成功')
            router.push('/teacher/courses')
        } else {
            await courseApi.update(route.params.id, { ...form })
            ElMessage.success('课程已更新')
            router.push(`/teacher/courses/${route.params.id}`)
        }
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '保存失败')
    } finally {
        saving.value = false
    }
}

onMounted(fetchCourse)
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
}

.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, .05);
    padding: 32px 40px;
    max-width: 720px;
}

.edit-form {
    max-width: 600px;
}

.cover-upload {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.cover-preview {
    width: 100%;
    height: 160px;
    border-radius: 8px;
    background-size: cover;
    background-position: center;
    position: relative;
    border: 1px solid #ebeef5;
}

.cover-remove {
    position: absolute;
    top: 8px;
    right: 8px;
}

.cover-placeholder {
    width: 100%;
    height: 160px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #909399;
    cursor: pointer;
    transition: border-color 0.2s;
    background: #fafafa;
}

.cover-placeholder:hover {
    border-color: #409eff;
    color: #409eff;
}

.cover-hint {
    color: #c0c4cc;
    font-size: 12px;
    margin: 0;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}
</style>
