<template>
    <section class="panel course-panel">
        <div class="panel-header">
            <h3 class="panel-title">课程</h3>
            <div class="panel-filters">
                <el-select v-model="semester" placeholder="学年学期" size="small" class="filter-select">
                    <el-option label="2025-2026 第二学期" value="2025-2026-2" />
                    <el-option label="2025-2026 第一学期" value="2025-2026-1" />
                </el-select>
                <span class="sort-hint" @click="toggleSort" style="cursor:pointer">{{ sortLabel }}</span>
                <el-icon :size="14" color="#909399" style="cursor:pointer" @click="toggleSort">
                    <Sort />
                </el-icon>
            </div>
        </div>
        <div v-if="loading" class="loading-state">
            <el-icon class="is-loading" :size="32">
                <Loading />
            </el-icon>
            <p>加载课程中...</p>
        </div>
        <div v-else-if="courses.length === 0" class="empty-state" style="padding:24px 0">
            <p class="empty-text">{{ emptyText }}</p>
        </div>
        <div v-else class="course-scroll">
            <div class="course-card" v-for="course in courses" :key="course.id">
                <div class="card-cover" :style="{ background: getCoverGradient(course.id) }">
                    <CourseCoverPattern extended />
                </div>
                <div class="card-body">
                    <h4 class="card-title" :title="course.name">{{ course.name }}</h4>
                    <p class="card-author">教师 ：{{ course.teacherName || course.createByName || '未知教师' }}</p>
                    <p class="card-student-id">{{ course.courseCode || '' }}</p>
                </div>
                <div class="card-footer">
                    <button class="btn-enter" @click="$emit('enterCourse', course)">
                        {{ isTeacher ? '管理课程' : '进入课程' }}
                    </button>
                    <el-dropdown trigger="click" @command="(cmd) => $emit('courseAction', cmd, course)">
                        <button class="btn-more" title="更多操作" @click.stop>
                            <el-icon :size="16">
                                <MoreFilled />
                            </el-icon>
                        </button>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="enter">{{ isTeacher ? '管理课程' : '进入课程' }}</el-dropdown-item>
                                <el-dropdown-item command="detail">课程详情</el-dropdown-item>
                                <el-dropdown-item v-if="!isTeacher" command="enroll" :disabled="course.enrolled">
                                    {{ course.enrolled ? '已选课' : '选课' }}
                                </el-dropdown-item>
                                <el-dropdown-item v-if="isTeacher" command="edit">编辑课程</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup>
import { ref, computed } from 'vue'
import CourseCoverPattern from '@/components/CourseCoverPattern.vue'

const props = defineProps({
    courses: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    isTeacher: { type: Boolean, default: false },
    emptyText: { type: String, default: '暂无课程' }
})

defineEmits(['enterCourse', 'courseAction'])

const semester = ref('2025-2026-2')
const courseSortAsc = ref(false)

const sortLabel = computed(() => courseSortAsc.value ? '按名称排序' : '最近访问排序')

const toggleSort = () => {
    courseSortAsc.value = !courseSortAsc.value
    // Sorting is handled locally via emitted event or just local reorder
}

const gradientPalette = [
    'linear-gradient(135deg, #5a7040 0%, #6b8e4e 50%, #8cb369 100%)',
    'linear-gradient(135deg, #4a5c38 0%, #606c38 50%, #7f8c52 100%)',
    'linear-gradient(135deg, #3b5e4a 0%, #4c7a5e 50%, #6b9e7a 100%)',
    'linear-gradient(135deg, #5c4a3a 0%, #7a6048 50%, #9c7a5c 100%)',
    'linear-gradient(135deg, #4a5a6e 0%, #5c7a8a 50%, #7a9fb5 100%)',
    'linear-gradient(135deg, #6b5e4a 0%, #8a7a5c 50%, #a8986e 100%)',
    'linear-gradient(135deg, #4e5a3a 0%, #667a48 50%, #8c9e6a 100%)',
    'linear-gradient(135deg, #5a4a3e 0%, #7a6250 50%, #a08068 100%)'
]
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]
</script>

<style scoped>
.course-panel {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    max-height: 60%;
}

.course-scroll {
    display: flex;
    gap: 16px;
    overflow-x: auto;
    padding-bottom: 8px;
    flex: 1;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.course-scroll::-webkit-scrollbar {
    display: none;
}

.course-card {
    min-width: 200px;
    max-width: 200px;
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    border: 1px solid rgba(163, 177, 138, 0.1);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    transition: box-shadow var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-normal) var(--bio-ease-out);
}

.course-card:hover {
    box-shadow: var(--bio-shadow-lg);
    transform: translateY(-3px);
}

.course-card:active {
    transform: translateY(0px);
}

.card-cover {
    height: 100px;
    position: relative;
    overflow: hidden;
}

.card-body {
    padding: 12px 14px 8px;
    flex: 1;
}

.card-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.card-author {
    font-size: 12px;
    color: var(--bio-ink-soft);
    margin-bottom: 2px;
}

.card-student-id {
    font-size: 11px;
    color: var(--bio-ink-faint);
}

.card-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px 14px;
}

.btn-enter {
    flex: 1;
    height: 34px;
    border: none;
    border-radius: var(--bio-radius-sm);
    background: linear-gradient(135deg, rgba(163, 177, 138, 0.15), rgba(163, 177, 138, 0.08));
    color: var(--bio-moss);
    font-size: 13px;
    cursor: pointer;
    transition: background var(--bio-dur-fast) var(--bio-ease-out),
        filter var(--bio-dur-fast) var(--bio-ease-out);
}

.btn-enter:hover {
    filter: brightness(1.1);
}

.btn-enter:active {
    transform: translateY(1px);
}

.btn-more {
    width: 34px;
    height: 34px;
    border: none;
    border-radius: var(--bio-radius-sm);
    background: transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--bio-ink-muted);
}

.btn-more:hover {
    background: rgba(163, 177, 138, 0.10);
}

.panel {
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    box-shadow: var(--bio-shadow-sm);
    padding: 20px 24px;
    border: 1px solid rgba(163, 177, 138, 0.08);
    transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.panel:hover {
    box-shadow: var(--bio-shadow-md);
}

.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.panel-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--bio-ink);
}

.panel-filters {
    display: flex;
    align-items: center;
    gap: 12px;
}

.filter-select {
    width: 130px;
}

.sort-hint {
    font-size: 12px;
    color: var(--bio-ink-muted);
    white-space: nowrap;
}

.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 0;
    color: var(--bio-ink-muted);
    font-size: 13px;
    gap: 12px;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0 32px;
}

.empty-text {
    color: var(--bio-ink-faint);
    font-size: 14px;
}
</style>
