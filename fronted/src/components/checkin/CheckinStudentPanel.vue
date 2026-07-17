<template>
    <div class="student-panel">
        <!-- 倒计时 + 迟到提示 -->
        <div class="time-banner" :class="{ late: isLateWindow }">
            <el-icon>
                <Timer />
            </el-icon>
            <span v-if="isLateWindow" class="late-warn">迟到窗口 — 剩余 {{ countdownText }}</span>
            <span v-else>剩余 {{ countdownText }}</span>
        </div>

        <!-- 已签到状态 -->
        <div v-if="session?.checkedIn" class="done-state">
            <el-icon :size="48" color="#20c4cc">
                <CircleCheckFilled />
            </el-icon>
            <p>签到成功</p>
            <span class="done-badge">{{ statusLabel(session.myStatus) }}</span>
        </div>

        <!-- 签到表单 -->
        <template v-else-if="!session?.checkedIn">
            <!-- 数字码签到：手动输入 -->
            <div v-if="session?.checkinType === 'NUMERIC_CODE'" class="code-section">
                <label>请输入6位签到码</label>
                <div class="code-input-row">
                    <el-input v-model="code" placeholder="输入6位签到码" maxlength="6" class="code-input"
                        :disabled="submitting" @keyup.enter="handleCheckin">
                        <template #prefix><el-icon>
                                <Iphone />
                            </el-icon></template>
                    </el-input>
                    <el-button type="primary" :loading="submitting" :disabled="code.length !== 6"
                        @click="handleCheckin">
                        签到
                    </el-button>
                </div>
            </div>

            <!-- 地理位置签到 -->
            <div v-if="session?.checkinType === 'LOCATION'" class="loc-section">
                <div class="loc-status" :class="{ located: studentLat != null }">
                    <el-icon :size="36">
                        <Location />
                    </el-icon>
                    <span v-if="studentLat != null">已获取位置</span>
                    <span v-else>请先获取位置信息</span>
                </div>
                <el-button type="primary" :loading="locating" @click="getLocation" :disabled="submitting" plain>
                    <el-icon>
                        <Location />
                    </el-icon>
                    {{ studentLat != null ? '重新获取位置' : '获取我的位置' }}
                </el-button>
                <el-button type="primary" :loading="submitting" :disabled="studentLat == null" @click="handleCheckin"
                    style="margin-top: 12px;">
                    确认签到
                </el-button>
            </div>
        </template>

        <!-- 错误提示 -->
        <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="true" @close="errorMsg = ''"
            style="margin-top:12px" />
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Timer, CircleCheckFilled, CircleCheck, Iphone, Location } from '@element-plus/icons-vue'
import { checkinApi } from '@/api'
import { useRoute } from 'vue-router'

const props = defineProps({
    session: { type: Object, required: true }
})

const emit = defineEmits(['checkedIn'])

const route = useRoute()
const code = ref('')
const submitting = ref(false)
const errorMsg = ref('')
const locating = ref(false)
const studentLat = ref(null)
const studentLng = ref(null)
const now = ref(new Date())
let timer = null

// 签到结束时定格此刻的时间，防止倒计时继续走
const frozenNow = ref(null)

// 检查URL参数中是否有预填code（扫码进入）
const urlCode = computed(() => route.query.code || '')

watch(() => urlCode.value, (val) => {
    if (val && props.session?.checkinType !== 'LOCATION') {
        code.value = val
    }
}, { immediate: true })

const isLateWindow = computed(() => props.session?.isLateWindow || false)

const countdownText = computed(() => {
    if (!props.session?.endTime) return ''
    const end = new Date(props.session.endTime)
    // 签到已结束则使用定格时间，否则使用实时时间
    const effectiveNow = props.session?.status !== 'ACTIVE' && frozenNow.value
        ? frozenNow.value
        : now.value
    const remaining = Math.max(0, end - effectiveNow)
    const m = Math.floor(remaining / 60000)
    const s = Math.floor((remaining % 60000) / 1000)
    if (m > 0) return `${m}分${s}秒`
    return `${s}秒`
})

// 监听签到状态：一旦变为非 ACTIVE（教师结束签到），立刻停止倒计时
watch(() => props.session?.status, (status, oldStatus) => {
    if (oldStatus === 'ACTIVE' && status !== 'ACTIVE') {
        frozenNow.value = new Date()
        if (timer) {
            clearInterval(timer)
            timer = null
        }
    }
    // 如果重新变为 ACTIVE，恢复计时
    if (status === 'ACTIVE') {
        frozenNow.value = null
        if (!timer) {
            timer = setInterval(() => { now.value = new Date() }, 1000)
        }
    }
})

const statusLabel = (s) => ({ SUCCESS: '正常', LATE: '迟到', ABSENT: '缺勤' })[s] || s

const getLocation = () => {
    if (!navigator.geolocation) { ElMessage.warning('浏览器不支持定位'); return }
    locating.value = true
    navigator.geolocation.getCurrentPosition(
        (pos) => {
            studentLat.value = pos.coords.latitude
            studentLng.value = pos.coords.longitude
            locating.value = false
        },
        () => {
            locating.value = false
            errorMsg.value = '定位失败，请检查浏览器定位权限'
        },
        { enableHighAccuracy: true, timeout: 10000 }
    )
}

const handleCheckin = async () => {
    if (submitting.value) return
    errorMsg.value = ''

    if (props.session?.checkinType === 'NUMERIC_CODE' && code.value.length !== 6) {
        errorMsg.value = '请输入6位签到码'
        return
    }

    submitting.value = true
    try {
        const data = {}
        if (props.session?.checkinType === 'NUMERIC_CODE') {
            data.code = code.value
        } else if (props.session?.checkinType === 'LOCATION') {
            data.latitude = studentLat.value
            data.longitude = studentLng.value
        }

        const res = await checkinApi.checkin(props.session.id, data)
        if (res.data.code === 200) {
            ElMessage.success(res.data.data?.message || '签到成功')
            emit('checkedIn', res.data.data)
        }
    } catch (e) {
        const msg = e?.response?.data?.message || e?.message || '签到失败'
        errorMsg.value = msg
    } finally {
        submitting.value = false
    }
}

onMounted(() => {
    // 仅当签到仍活跃时才启动倒计时
    if (props.session?.status === 'ACTIVE') {
        timer = setInterval(() => { now.value = new Date() }, 1000)
    } else {
        frozenNow.value = new Date()
    }
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.student-panel {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.time-banner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    border-radius: var(--bio-radius-sm, 8px);
    background: #e8f5e9;
    color: #2e7d32;
    font-size: 14px;
    font-weight: 500;
}

.time-banner.late {
    background: #fff3e0;
    color: #e65100;
}

.late-warn {
    font-weight: 600;
}

.done-state {
    text-align: center;
    padding: 32px 0;
}

.done-state p {
    font-size: 16px;
    color: var(--bio-ink, #2c3630);
    margin: 8px 0 4px;
}

.done-badge {
    display: inline-block;
    padding: 2px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
    background: #e8f5e9;
    color: #2e7d32;
}

.code-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.code-section label {
    font-size: 14px;
    color: var(--bio-ink-muted);
}

.code-input-row {
    display: flex;
    gap: 10px;
}

.code-input {
    flex: 1;
}

.code-input :deep(.el-input__inner) {
    font-size: 20px;
    letter-spacing: 8px;
    text-align: center;
    font-family: 'JetBrains Mono', monospace;
}

.prefill-hint {
    font-size: 12px;
    color: var(--bio-primary);
    margin: 0;
}

.qr-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 16px 0;
}

.qr-scanned {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.qr-code-display {
    font-size: 32px;
    font-weight: 800;
    letter-spacing: 8px;
    color: var(--bio-primary);
    font-family: 'JetBrains Mono', monospace;
    margin: 0;
}

.qr-hint {
    font-size: 14px;
    color: var(--bio-ink-muted);
    margin: 0;
}

.qr-hint-sub {
    font-size: 12px;
    color: var(--bio-ink-muted);
    opacity: .7;
    margin: 0;
}

.qr-waiting {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 24px 0;
}

.qr-waiting p {
    margin: 0;
    font-size: 15px;
    font-weight: 500;
    color: var(--bio-ink, #2c3630);
}

.loc-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 24px 0;
}

.loc-status {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: var(--bio-ink-muted);
}

.loc-status.located {
    color: var(--bio-primary);
}
</style>
