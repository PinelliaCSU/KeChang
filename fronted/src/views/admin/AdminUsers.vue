<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">账号管理</h2>
        <p class="page-subtitle">管理平台教师和学生账号，支持创建、编辑、启用/禁用、重置密码和删除操作</p>
      </div>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-num teacher">{{ stats.teacherCount || 0 }}</div>
        <div class="stat-label">教师数量</div>
      </div>
      <div class="stat-card">
        <div class="stat-num student">{{ stats.studentCount || 0 }}</div>
        <div class="stat-label">学生数量</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.totalUsers || 0 }}</div>
        <div class="stat-label">总用户数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num new">{{ stats.newThisMonth || 0 }}</div>
        <div class="stat-label">本月新增</div>
      </div>
    </div>

    <DataPanel max-width="1400px">
      <template #toolbar>
        <div class="search-bar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名/姓名"
            clearable
            style="width: 240px"
            @change="fetchUsers"
            @clear="fetchUsers"
          />
          <el-select v-model="query.roleId" placeholder="角色筛选" clearable style="width: 140px" @change="fetchUsers">
            <el-option label="教师" :value="teacherRoleId" />
            <el-option label="学生" :value="studentRoleId" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 130px" @change="fetchUsers">
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
          <el-button type="primary" @click="openCreate">添加用户</el-button>
        </div>
      </template>

      <el-table :data="users" stripe class="is-borderless" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名（邮箱）" min-width="180" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="roleName" label="角色" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.roleCode === 'ROLE_TEACHER' ? '' : 'success'">
              {{ row.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 0 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="info" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchUsers"
          @size-change="fetchUsers"
        />
      </div>
    </DataPanel>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名（邮箱）" prop="username">
              <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" :disabled="isEdit" placeholder="请选择角色" style="width: 100%" @change="onRoleChange">
            <el-option label="教师" :value="teacherRoleId" />
            <el-option label="学生" :value="studentRoleId" />
          </el-select>
        </el-form-item>

        <!-- 教师扩展字段 -->
        <template v-if="selectedRoleCode === 'ROLE_TEACHER'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职称" prop="title">
                <el-input v-model="form.title" placeholder="如：教授、副教授" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="院系" prop="department">
            <el-input v-model="form.department" placeholder="请输入所属院系" />
          </el-form-item>
        </template>

        <!-- 学生扩展字段 -->
        <template v-if="selectedRoleCode === 'ROLE_STUDENT'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学号" prop="studentNo">
                <el-input v-model="form.studentNo" placeholder="留空则自动生成" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="年级" prop="grade">
                <el-input v-model="form.grade" placeholder="如：2024级" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业" prop="major">
                <el-input v-model="form.major" placeholder="请输入专业" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="班级" prop="className">
            <el-input v-model="form.className" placeholder="请输入班级" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserApi } from '@/api'
import { required, password, phone } from '@/utils/validators'
import DataPanel from '@/components/DataPanel.vue'

// ---- 角色ID缓存 ----
const teacherRoleId = ref(null)
const studentRoleId = ref(null)

// ---- 列表数据 ----
const users = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  roleId: '',
  status: ''
})
const stats = ref({})

// ---- 对话框 ----
const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const isEdit = ref(false)
const saving = ref(false)
const editingUserId = ref(null)
const formRef = ref(null)

const emptyForm = () => ({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  roleId: null,
  realName: '',
  title: '',
  department: '',
  studentNo: '',
  grade: '',
  major: '',
  className: ''
})
const form = reactive(emptyForm())

// 当前选中的角色编码
const selectedRoleCode = computed(() => {
  if (form.roleId === teacherRoleId.value) return 'ROLE_TEACHER'
  if (form.roleId === studentRoleId.value) return 'ROLE_STUDENT'
  return ''
})

// 表单校验规则
const formRules = computed(() => {
  const base = {
    username: [required('请输入邮箱'), { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
    nickname: [],
    phone: [phone()],
    roleId: [required('请选择角色')]
  }
  if (!isEdit.value) {
    base.password = password(6, '请设置密码')
  }
  return base
})

// ---- 数据加载 ----
const fetchStats = async () => {
  try {
    const res = await adminUserApi.stats()
    if (res.data.code === 200) {
      const data = res.data.data || {}
      stats.value = data
      // 从 stats 接口直接获取角色ID（后端已返回）
      if (data.teacherRoleId) teacherRoleId.value = data.teacherRoleId
      if (data.studentRoleId) studentRoleId.value = data.studentRoleId
    }
  } catch { /* ignore */ }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.roleId) params.roleId = query.roleId
    if (query.status !== '') params.status = query.status
    const res = await adminUserApi.list(params)
    if (res.data.code === 200) {
      users.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
  } catch {
    ElMessage.error('用户列表加载失败')
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  query.page = 1
  fetchStats()
  fetchUsers()
}

// ---- 创建/编辑 ----
const openCreate = () => {
  isEdit.value = false
  dialogTitle.value = '添加用户'
  editingUserId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  editingUserId.value = row.id
  form.username = row.username
  form.nickname = row.nickname || ''
  form.phone = row.phone || ''
  form.roleId = row.roleId
  form.realName = row.realName || ''
  form.studentNo = row.studentNo || ''
  form.grade = row.grade || ''
  form.major = row.major || ''
  form.className = row.className || ''
  form.title = row.title || ''
  form.department = row.department || ''
  form.password = ''
  dialogVisible.value = true
}

const onRoleChange = () => {
  // 切换角色时清空扩展字段
  form.realName = ''
  form.title = ''
  form.department = ''
  form.studentNo = ''
  form.grade = ''
  form.major = ''
  form.className = ''
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  if (formRef.value) formRef.value.resetFields()
}

const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      const data = {
        nickname: form.nickname || null,
        phone: form.phone || null,
        realName: form.realName || null,
        title: form.title || null,
        department: form.department || null,
        studentNo: form.studentNo || null,
        grade: form.grade || null,
        major: form.major || null,
        className: form.className || null
      }
      await adminUserApi.update(editingUserId.value, data)
      ElMessage.success('用户信息已更新')
    } else {
      await adminUserApi.create({ ...form })
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
    fetchStats()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// ---- 状态切换 ----
const handleToggleStatus = async (row) => {
  const action = row.status === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户"${row.username}"吗？`,
      `${action}账号`,
      { type: 'warning' }
    )
    await adminUserApi.toggleStatus(row.id)
    ElMessage.success(`已${action}`)
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// ---- 重置密码 ----
const handleResetPassword = async (row) => {
  try {
    const { value: newPwd } = await ElMessageBox.prompt(
      `请输入用户"${row.username}"的新密码`,
      '重置密码',
      {
        inputType: 'password',
        inputValidator: (val) => {
          if (!val || val.length < 6) return '密码至少6位'
          return true
        }
      }
    )
    await adminUserApi.resetPassword(row.id, { newPassword: newPwd })
    ElMessage.success('密码已重置')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// ---- 删除 ----
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户"${row.username}"吗？此操作不可恢复。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await adminUserApi.delete(row.id)
    ElMessage.success('已删除')
    fetchUsers()
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchUsers()
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; }
.stat-row { display: flex; gap: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 8px; padding: 20px 24px; box-shadow: 0 4px 12px rgba(0,0,0,.05); border: 1px solid #ebeef5; }
.stat-num { color: #303133; font-size: 28px; font-weight: 700; }
.stat-num.teacher { color: #409eff; }
.stat-num.student { color: #67c23a; }
.stat-num.new { color: var(--bio-primary); }
.stat-label { color: #606266; font-size: 13px; margin-top: 6px; }
.search-bar { display: flex; gap: 12px; align-items: center; }
.pager { display: flex; justify-content: center; margin-top: 18px; }
</style>
