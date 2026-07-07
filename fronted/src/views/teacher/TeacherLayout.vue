<template>
  <div class="teacher-layout">
    <DashboardHeader />
    <div class="layout-body">
      <AppSidebar />
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import DashboardHeader from '@/components/DashboardHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
</script>

<style scoped>
.teacher-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bio-cream);
  font-family: var(--bio-font);
}

.layout-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background:
    radial-gradient(ellipse 60% 40% at 30% 20%, rgba(163, 177, 138, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse 50% 50% at 80% 70%, rgba(165, 196, 212, 0.04) 0%, transparent 60%);
}
</style>
