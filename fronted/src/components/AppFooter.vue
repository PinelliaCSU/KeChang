<template>
  <footer class="app-footer">
    <p>© 2024 课畅 KeChang — 智能化在线教学支持服务平台 | 技术支持：华迪实训</p>
  </footer>
</template>

<style scoped>
.app-footer {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #fdfbf7 0%, #f7f5ed 100%);
  border-top: 1px solid rgba(163, 177, 138, 0.15);
  color: var(--bio-ink-muted);
  font-size: 13px;
  position: relative;
}

.app-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(163, 177, 138, 0.2), transparent);
}
</style>
