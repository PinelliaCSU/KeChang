<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">互动问答</h2>
      <el-button type="primary" size="default" @click="openCreate">+ 发布问题</el-button>
    </div>

    <div class="panel">
      <!-- 筛选与排序 -->
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="resetPage(); fetchPosts()">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="orderBy" style="width:140px" @change="resetPage(); fetchPosts()">
          <el-option label="最新发布" value="time" />
          <el-option label="最多回复" value="reply" />
          <el-option label="最多浏览" value="view" />
        </el-select>
      </div>

      <!-- 类型筛选 Tab -->
      <el-tabs v-model="postTypeFilter" @tab-change="resetPage(); fetchPosts()" style="margin-bottom:8px">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="提问" name="提问" />
        <el-tab-pane label="讨论" name="讨论" />
      </el-tabs>

      <!-- 帖子列表 -->
      <div class="post-list" v-if="posts.length > 0">
        <div class="post-item" v-for="p in posts" :key="p.id" @click="viewPost(p)">
          <div class="post-main">
            <div class="post-title">
              <el-tag size="small" :type="p.postType === '提问' ? 'warning' : 'info'" style="margin-right:8px">{{
                p.postType || '提问' }}</el-tag>
              {{ p.title }}
            </div>
            <div class="post-meta">
              <span><el-icon>
                  <User />
                </el-icon> {{ p.createByName || '匿名' }}</span>
              <span v-if="p.courseName"><el-icon>
                  <Collection />
                </el-icon> {{ p.courseName }}</span>
              <span><el-icon>
                  <View />
                </el-icon> {{ p.viewCount || 0 }}</span>
              <span><el-icon>
                  <ChatDotSquare />
                </el-icon> {{ p.replyCount || 0 }}</span>
              <span><el-icon>
                  <Clock />
                </el-icon> {{ p.createTime }}</span>
            </div>
          </div>
          <div class="post-badge" v-if="p.replyCount === 0">
            <el-tag type="danger" size="small">待回复</el-tag>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无问答帖子" :image-size="100" />

      <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" @change="fetchPosts" />
    </div>

    <!-- 发布问题弹窗 -->
    <el-dialog v-model="showCreate" title="发布问题" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="所属课程" required>
          <el-select v-model="createForm.courseId" placeholder="请选择课程" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="帖子类型" required>
          <el-radio-group v-model="createForm.postType">
            <el-radio value="提问">提问</el-radio>
            <el-radio value="讨论">讨论</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入问题标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createForm.content" type="textarea" :rows="5" placeholder="描述问题内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createPost">发布</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { qaApi, courseApi } from '@/api'
import { ElMessage } from 'element-plus'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import { User, Clock, View, ChatDotSquare, Collection } from '@element-plus/icons-vue'

const router = useRouter()

// ========== 列表 ==========
const posts = ref([])
const { page, size, total, resetPage } = usePagination({ defaultSize: 10 })
const courseFilter = ref(null); const orderBy = ref('time'); const postTypeFilter = ref('')
const courseList = ref([])

const fetchPosts = async () => {
  try {
    const res = await qaApi.listPosts({ page: page.value, size: size.value, courseId: courseFilter.value || undefined, orderBy: orderBy.value })
    if (res.data.code === 200) {
      let records = res.data.data.records
      if (postTypeFilter.value) records = records.filter(p => p.postType === postTypeFilter.value)
      posts.value = records
      total.value = postTypeFilter.value ? records.length : res.data.data.total
    }
  } catch (e) { /* ignore */ }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

// ========== 发布 ==========
const showCreate = ref(false); const submitting = ref(false)
const createForm = ref({ title: '', content: '', courseId: null, postType: '提问' })

const openCreate = () => { showCreate.value = true }

const createPost = async () => {
  if (!createForm.value.courseId) { ElMessage.warning('请选择所属课程'); return }
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  submitting.value = true
  try {
    await qaApi.createPost(createForm.value)
    ElMessage.success('发布成功')
    showCreate.value = false
    createForm.value = { title: '', content: '', courseId: null, postType: '提问' }
    fetchPosts()
  } catch (e) { /* ignore */ }
  submitting.value = false
}

// ========== 导航到详情页 ==========
const viewPost = (p) => {
  router.push(`/teacher/posts/${p.id}`)
}

onMounted(() => { fetchCourses(); fetchPosts() })
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

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.post-list {
  display: flex;
  flex-direction: column;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.post-item:hover {
  background: #f5f7fa;
  margin: 0 -24px;
  padding: 16px 24px;
}

.post-main {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  flex-wrap: wrap;
}
</style>
