<template>
  <div class="layout">
    <header class="layout-header">
      <div class="header-brand">Aurora LowCode</div>
      <nav class="header-nav">
        <router-link to="/dashboard">Dashboard</router-link>
        <router-link to="/forms">Forms</router-link>
        <router-link to="/reports">Reports</router-link>
        <router-link to="/workflows">Workflows</router-link>
      </nav>
      <div class="header-actions">
        <span class="user-info">{{ authStore.username }}</span>
        <button @click="authStore.logout()" class="logout-btn">Logout</button>
      </div>
    </header>
    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  padding: 0 1.5rem;
  height: 56px;
  background: #1f2937;
  color: white;
}

.header-brand {
  font-weight: 700;
  font-size: 1.125rem;
}

.header-nav {
  display: flex;
  gap: 1.5rem;
  margin-left: 2rem;
}

.header-nav a {
  color: #9ca3af;
  text-decoration: none;
  font-size: 0.875rem;
}

.header-nav a:hover,
.header-nav a.router-link-active {
  color: white;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  font-size: 0.875rem;
  color: #9ca3af;
}

.logout-btn {
  padding: 0.375rem 0.75rem;
  background: transparent;
  color: #9ca3af;
  border: 1px solid #4b5563;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.75rem;
}

.logout-btn:hover {
  color: white;
  border-color: #6b7280;
}

.layout-main {
  flex: 1;
  padding: 1.5rem;
  background: #f9fafb;
}
</style>
