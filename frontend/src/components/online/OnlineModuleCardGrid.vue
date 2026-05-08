<template>
  <div class="module-grid">
    <article v-for="item in items" :key="item.title" class="module-card card">
      <div class="module-card__body">
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
      </div>
      <div class="module-card__footer">
        <router-link :to="item.to" class="module-link">
          {{ item.title }}
        </router-link>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    title: string
    description: string
    to: string
  }>
}>()
</script>

<style scoped>
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-md);
}

.module-card {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  justify-content: space-between;
}

.module-card__body h3 {
  margin: 0 0 var(--space-sm);
  font-size: var(--text-base);
}

.module-card__body p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.module-card__footer {
  margin-top: var(--space-lg);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
}

.module-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: none;
}

.module-link:hover {
  text-decoration: underline;
}
</style>
