<template>
    <el-dialog v-model="visible" :title="isCreating ? '发起签到' : '签到详情'" width="480px" :close-on-click-modal="false"
        destroy-on-close>
        <el-form v-if="isCreating" ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="选择课程" prop="courseId">
                <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width:100%">
                    <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
                </el-select>
            </el-form-item>

            <el-form-item label="签到标题" prop="title">
                <el-input v-model="form.title" placeholder="如：第3周周一签到" maxlength="50" show-word-limit />
            </el-form-item>

            <el-form-item label="签到方式" prop="checkinType">
                <div class="type-selector">
                    <div v-for="opt in typeOptions" :key="opt.value" class="type-card"
                        :class="{ selected: form.checkinType === opt.value }" @click="form.checkinType = opt.value">
                        <el-icon :size="24">
                            <component :is="opt.icon" />
                        </el-icon>
                        <span>{{ opt.label }}</span>
                    </div>
                </div>
            </el-form-item>

            <!-- LOCATION 方式的额外配置 -->
            <template v-if="form.checkinType === 'LOCATION'">
                <el-form-item label="签到位置">
                    <div class="location-setup">
                        <el-button type="primary" :loading="locatingTeacher" @click="getTeacherLocation" plain>
                            <el-icon>
                                <Location />
                            </el-icon>
                            {{ teacherLat != null ? '已获取位置 ✓' : '获取我的位置' }}
                        </el-button>
                        <span v-if="teacherLat != null" class="loc-coords">
                            {{ teacherLat.toFixed(6) }}, {{ teacherLng.toFixed(6) }}
                        </span>
                    </div>
                </el-form-item>
                <el-form-item label="有效半径（米）">
                    <el-slider v-model="form.locationRadius" :min="20" :max="500" :step="10" show-input
                        style="width:100%" />
                </el-form-item>
            </template>

            <el-form-item label="签到持续时长（分钟）" prop="durationMinutes">
                <el-radio-group v-model="form.durationMinutes">
                    <el-radio-button :value="1">1分钟</el-radio-button>
                    <el-radio-button :value="3">3分钟</el-radio-button>
                    <el-radio-button :value="5">5分钟</el-radio-button>
                    <el-radio-button :value="10">10分钟</el-radio-button>
                    <el-radio-button :value="15">15分钟</el-radio-button>
                </el-radio-group>
            </el-form-item>
        </el-form>

        <template #footer>
            <el-button @click="visible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
                {{ isCreating ? '发起签到' : '确定' }}
            </el-button>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { PictureFilled, Iphone, Location } from '@element-plus/icons-vue'
import { checkinApi, courseApi } from '@/api'

const emit = defineEmits(['created'])

const visible = ref(false)
const isCreating = ref(true)
const submitting = ref(false)
const formRef = ref(null)
const courses = ref([])

const teacherLat = ref(null)
const teacherLng = ref(null)
const locatingTeacher = ref(false)

const form = reactive({
    courseId: null,
    title: '',
    checkinType: 'NUMERIC_CODE',
    durationMinutes: 5,
    locationRadius: 100
})

const rules = {
    courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
    durationMinutes: [{ required: true, message: '请选择时长', trigger: 'change' }]
}

const typeOptions = [
    { value: 'NUMERIC_CODE', label: '数字码签到', icon: Iphone },
    { value: 'QR_CODE', label: '二维码签到', icon: PictureFilled },
    { value: 'LOCATION', label: '位置签到', icon: Location }
]

watch(visible, async (val) => {
    if (val && isCreating.value) {
        await fetchCourses()
    }
})

const fetchCourses = async () => {
    try {
        const res = await courseApi.list({})
        courses.value = (res.data.data?.records || res.data.data || []).filter(c => c.status !== '已结束')
    } catch { /* ignore */ }
}

const getTeacherLocation = () => {
    if (!navigator.geolocation) {
        ElMessage.warning('浏览器不支持定位功能')
        return
    }
    locatingTeacher.value = true
    navigator.geolocation.getCurrentPosition(
        (pos) => {
            teacherLat.value = pos.coords.latitude
            teacherLng.value = pos.coords.longitude
            form.courseId = form.courseId // trigger reactivity
            locatingTeacher.value = false
            ElMessage.success('位置获取成功')
        },
        () => {
            locatingTeacher.value = false
            ElMessage.error('定位失败，请检查浏览器定位权限')
        },
        { enableHighAccuracy: true, timeout: 10000 }
    )
}

const handleSubmit = async () => {
    if (!isCreating.value) { visible.value = false; return }
    if (!formRef.value) return
    try {
        await formRef.value.validate()
    } catch { return }

    submitting.value = true
    try {
        const data = {
            courseId: form.courseId,
            title: form.title || '课堂签到',
            checkinType: form.checkinType,
            durationMinutes: form.durationMinutes
        }
        if (form.checkinType === 'LOCATION') {
            if (teacherLat.value == null) {
                ElMessage.warning('请先获取签到位置')
                submitting.value = false
                return
            }
            data.latitude = teacherLat.value
            data.longitude = teacherLng.value
            data.locationRadius = form.locationRadius
        }
        const res = await checkinApi.create(data)
        if (res.data.code === 200) {
            ElMessage.success('签到已发起')
            visible.value = false
            emit('created', res.data.data)
            resetForm()
        }
    } catch (e) {
        ElMessage.error('发起失败')
    } finally {
        submitting.value = false
    }
}

const resetForm = () => {
    form.courseId = null
    form.title = ''
    form.checkinType = 'NUMERIC_CODE'
    form.durationMinutes = 5
    form.locationRadius = 100
    teacherLat.value = null
    teacherLng.value = null
    formRef.value?.resetFields()
}

const open = () => {
    isCreating.value = true
    resetForm()
    visible.value = true
}

defineExpose({ open })
</script>

<style scoped>
.type-selector {
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: center;
}

.type-card {
    width: 100%;
    max-width: 340px;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 16px;
    padding: 14px 20px;
    border-radius: var(--bio-radius-md, 14px);
    border: 2px solid #e8ecdf;
    cursor: pointer;
    transition: all .15s ease;
    font-size: 14px;
    color: var(--bio-ink-muted, #7a8278);
}

.type-card:hover {
    border-color: var(--bio-primary);
    background: #fff;
}

.type-card.selected {
    border-color: var(--bio-primary);
    background: var(--bio-primary-soft);
    color: var(--bio-primary);
    font-weight: 600;
}

.type-card .el-icon {
    flex-shrink: 0;
}

.location-setup {
    display: flex;
    align-items: center;
    gap: 12px;
}

.loc-coords {
    font-size: 12px;
    color: var(--bio-ink-muted, #7a8278);
    font-family: monospace;
}
</style>
