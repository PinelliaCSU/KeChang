<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">知识库</h2>
      <el-input v-model="keyword" placeholder="搜索知识..." size="default" style="width:260px" clearable />
    </div>

    <div class="panel" v-if="items.length > 0">
      <div class="knowledge-grid">
        <div class="knowledge-card" v-for="item in items" :key="item.id">
          <div class="k-icon">
            <el-icon :size="28" color="#20c4cc" v-if="item.type === 'doc'">
              <Document />
            </el-icon>
            <el-icon :size="28" color="#5c7a8a" v-else-if="item.type === 'video'">
              <VideoCamera />
            </el-icon>
            <el-icon :size="28" color="#9c6644" v-else>
              <Folder />
            </el-icon>
          </div>
          <h4 class="k-title">{{ item.title }}</h4>
          <p class="k-desc">{{ item.description?.substring(0, 80) || '暂无描述' }}</p>
          <div class="k-footer">
            <span>{{ item.courseName || '' }}</span>
            <span>{{ item.createTime || '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration text="知识库建设中，敬请期待" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { knowledgeApi } from '@/api'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const items = ref([])
const keyword = ref('')

const fetchArticles = async () => {
  try {
    const params = { page: 1, size: 50 }
    if (keyword.value) params.keyword = keyword.value
    const res = await knowledgeApi.listArticles(params)
    if (res.data.code === 200) {
      const d = res.data.data
      items.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
}

onMounted(fetchArticles)
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

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.knowledge-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.knowledge-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.k-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.k-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.k-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 10px;
}

.k-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #c0c4cc;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-illustration {
  opacity: 0.6;
}

.empty-text {
  color: #c0c4cc;
  font-size: 14px;
}
</style>
