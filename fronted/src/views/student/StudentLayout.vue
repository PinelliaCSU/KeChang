<template>
  <div class="student-layout">
    <AppHeader />
    <div class="layout-body">
      <AppSidebar />
      <main class="main-content">
        <router-view />
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<style scoped>
.student-layout { min-height: 100vh; display: flex; flex-direction: column; background: #f0f5ff; }
.layout-body { display: flex; flex: 1; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; background: #f0f5ff; }
</style>
