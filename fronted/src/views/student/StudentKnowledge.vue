<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">反思</h2>
    </div>

    <div class="panel">
      <div class="quick-links">
        <div class="quick-card" @click="$router.push('/student/notes')">
          <el-icon :size="36" color="#20c4cc"><Notebook /></el-icon>
          <span>学习笔记</span>
          <p>课堂笔记、学习心得随时记录</p>
        </div>
        <div class="quick-card" @click="$router.push('/student/materials')">
          <el-icon :size="36" color="#06d6a0"><Folder /></el-icon>
          <span>我的资料</span>
          <p>上传和管理学习资料文件</p>
        </div>
        <div class="quick-card" @click="$router.push('/student/wrong-questions')">
          <el-icon :size="36" color="#ef476f"><WarningFilled /></el-icon>
          <span>我的错题</span>
          <p>考试和随堂测验的错题自动收录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
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

.quick-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 28px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafbfc;
}

.quick-card:hover {
  border-color: #20c4cc;
  box-shadow: 0 4px 12px rgba(32, 196, 204, 0.12);
  transform: translateY(-2px);
}

.quick-card span {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-top: 12px;
}

.quick-card p {
  font-size: 12px;
  color: #909399;
  margin: 6px 0 0;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .quick-links {
    grid-template-columns: 1fr;
  }
}
</style>
