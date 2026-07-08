/** 必填规则 */
export const required = (message, trigger = 'blur') => ({
  required: true,
  message,
  trigger
})

/** 邮箱规则 */
export const email = (trigger = 'blur') => [
  required('请输入邮箱', trigger),
  { type: 'email', message: '邮箱格式不正确', trigger }
]

/** 邮箱规则（非必填） */
export const emailOptional = (trigger = 'blur') => ({
  type: 'email',
  message: '请输入正确的邮箱地址',
  trigger
})

/** 密码规则 */
export const password = (minLen = 6, message = '请设置密码', trigger = 'blur') => [
  required(message, trigger),
  { min: minLen, message: `密码至少${minLen}位`, trigger }
]

/** 手机号规则（非必填） */
export const phone = (trigger = 'blur') => ({
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入正确的手机号',
  trigger
})

/** 长度限制规则 */
export const length = (min, max, trigger = 'blur') => ({
  min,
  max,
  message: `长度在 ${min} 到 ${max} 个字符`,
  trigger
})

/** 长度限制规则 + 必填 */
export const requiredLength = (min, max, message = '请输入', trigger = 'blur') => [
  required(message, trigger),
  length(min, max, trigger)
]

/** 确认密码规则 */
export const confirmPassword = (getPasswordFn, trigger = 'blur') => ({
  required: true,
  validator: (_rule, value, callback) => {
    if (!value) {
      callback(new Error('请再次输入密码'))
    } else if (value !== getPasswordFn()) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  },
  trigger
})

// ==================== 表单提交辅助函数 ====================

/**
 * 校验 el-form 表单，校验通过返回 true，失败返回 false
 * @param {import('element-plus').FormInstance} formRef - 表单 ref 实例
 * @returns {Promise<boolean>}
 */
export const validateForm = async (formRef) => {
  if (!formRef) return false
  try {
    await formRef.validate()
    return true
  } catch {
    return false
  }
}

// ==================== 常用规则集 ====================

/** 登录表单规则 */
export const loginRules = {
  username: [required('请输入用户名')],
  password: [required('请输入密码')]
}

/** 注册表单规则（基础字段） */
export const registerBaseRules = (getPassword) => ({
  email: email(),
  realName: [required('请输入真实姓名')],
  password: password(),
  confirmPassword: confirmPassword(getPassword)
})

/** 个人资料编辑规则 */
export const profileRules = {
  nickname: requiredLength(1, 20, '请输入昵称'),
  phone: [phone()],
  email: [emailOptional()]
}

// ==================== ElMessageBox.prompt 校验器 ====================

/** 密码强度校验器（用于 ElMessageBox.prompt） */
export const passwordInputValidator = (minLen = 6) => (value) => {
  if (!value || value.length < minLen) {
    return `密码至少${minLen}位`
  }
  return true
}

// ==================== 快捷手动校验（辅助函数，简化手写 if 判断） ====================

/**
 * 判断值是否为空（null / undefined / 空字符串 / 纯空白）
 * @param {*} value
 * @returns {boolean}
 */
export const isEmpty = (value) => {
  if (value === null || value === undefined) return true
  return String(value).trim() === ''
}
