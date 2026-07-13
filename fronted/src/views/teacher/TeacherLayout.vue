<template>
  <div class="teacher-layout">
    <DashboardHeader />
    <div class="layout-body">
      <AppSidebar />
      <main class="main-content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide">
            <keep-alive :max="6">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import DashboardHeader from '@/components/DashBoard/DashboardHeader.vue'
import AppSidebar from '@/components/Layout/AppSidebar.vue'
</script>

<style scoped>
.teacher-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bio-page);
  font-family: var(--bio-font);
}

.layout-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  min-width: 0;
  padding: 24px 28px;
  overflow-y: auto;
  background: var(--bio-page);
}
</style>
