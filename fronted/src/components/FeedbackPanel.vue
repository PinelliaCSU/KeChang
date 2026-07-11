<template>
  <div class="feedback-panel">
    <div class="feedback-toolbar">
      <span class="toolbar-title">批注反馈</span>
      <div class="toolbar-buttons">
        <el-button size="small" type="success" plain @click="addMark('check')">对钩</el-button>
        <el-button size="small" type="danger" plain @click="addMark('wrong')">错号</el-button>
        <el-button size="small" type="primary" plain @click="addMark('text')">文字</el-button>
      </div>
    </div>

    <div class="mark-list" v-if="marks.length">
      <div v-for="(mark, index) in marks" :key="mark.id || index" class="mark-card" :class="mark.type">
        <div class="mark-symbol">{{ symbol(mark.type) }}</div>
        <el-input v-model="mark.text" type="textarea" :rows="2" :placeholder="placeholder(mark.type)" :disabled="readonly" />
        <el-button v-if="!readonly" type="danger" link @click="removeMark(index)">删除</el-button>
      </div>
    </div>
    <el-empty v-else description="暂无批注" :image-size="70" />
  </div>
</template>

<script setup>
const props = defineProps({
  readonly: { type: Boolean, default: false }
})
const marks = defineModel({ type: Array, default: () => [] })

const addMark = (type) => {
  if (props.readonly) return
  marks.value.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    type,
    text: ''
  })
}

const removeMark = (index) => {
  marks.value.splice(index, 1)
}

const symbol = (type) => ({ check: '✓', wrong: '×', text: '批' }[type] || '批')
const placeholder = (type) => {
  if (type === 'check') return '写下做得好的地方'
  if (type === 'wrong') return '写下错误原因或修改建议'
  return '输入文字反馈'
}
</script>

<style scoped>
.feedback-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.feedback-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-title {
  font-weight: 600;
  color: #303133;
}

.toolbar-buttons {
  display: flex;
  gap: 8px;
}

.mark-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mark-card {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #ebeef5;
}

.mark-symbol {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
}

.mark-card.check .mark-symbol {
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
}

.mark-card.wrong .mark-symbol {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
