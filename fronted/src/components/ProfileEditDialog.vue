<template>
    <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="编辑个人资料"
        width="480px" :close-on-click-modal="false" destroy-on-close class="profile-edit-dialog">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="edit-form">
            <el-form-item label="头像">
                <div class="avatar-edit-row">
                    <el-avatar :size="64" class="edit-avatar">
                        {{ (form.nickname || form.username || 'U').charAt(0) }}
                    </el-avatar>
                    <el-input v-model="form.avatar" placeholder="请输入头像图片链接（可选）" class="avatar-input" />
                </div>
            </el-form-item>

            <el-form-item label="用户名">
                <el-input v-model="form.username" disabled />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
                <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="真实姓名" v-if="form.realName !== undefined">
                <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
        </el-form>

        <template #footer>
            <div class="dialog-footer">
                <el-button @click="$emit('update:modelValue', false)">取消</el-button>
                <el-button type="primary" @click="handleSave" :loading="saving">
                    保存修改
                </el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const props = defineProps({
    modelValue: { type: Boolean, default: false },
    userInfo: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const userStore = useUserStore()
const formRef = ref(null)
const saving = ref(false)

const form = reactive({
    username: '',
    nickname: '',
    phone: '',
    email: '',
    avatar: '',
    realName: undefined
})

const rules = {
    nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 1, max: 20, message: '昵称长度在 1 到 20 个字符', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    email: [
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ]
}

// 当弹窗打开时，同步外部传入的用户数据到表单
watch(() => props.modelValue, (visible) => {
    if (visible) {
        form.username = props.userInfo.username || ''
        form.nickname = props.userInfo.nickname || ''
        form.phone = props.userInfo.phone || ''
        form.email = props.userInfo.email || ''
        form.avatar = props.userInfo.avatar || ''
        form.realName = props.userInfo.realName
        // 重置表单校验状态
        formRef.value?.resetFields()
    }
})

const handleSave = async () => {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    saving.value = true
    try {
        // 调用后端更新接口
        await authApi.updateProfile({
            nickname: form.nickname,
            phone: form.phone,
            email: form.email,
            avatar: form.avatar
        })
        // 更新本地 store
        userStore.nickname = form.nickname
        localStorage.setItem('nickname', form.nickname)
        if (form.avatar) {
            userStore.avatar = form.avatar
            localStorage.setItem('avatar', form.avatar)
        }
        ElMessage.success('个人信息已更新')
        emit('update:modelValue', false)
        emit('saved', { ...form })
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '保存失败')
    } finally {
        saving.value = false
    }
}
</script>

<style scoped>
.edit-form {
    padding: 8px 0;
}

.avatar-edit-row {
    display: flex;
    align-items: center;
    gap: 16px;
}

.edit-avatar {
    background: linear-gradient(135deg, #20c997, #0d9488);
    color: #fff;
    font-size: 24px;
    font-weight: 600;
    flex-shrink: 0;
}

.avatar-input {
    flex: 1;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}
</style>
