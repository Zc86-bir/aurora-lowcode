<template>
  <div class="card table-card">
    <div class="table-header">
      <h3>{{ title }}</h3>
      <div v-if="$slots.actions" class="table-actions">
        <slot name="actions" />
      </div>
    </div>

    <table class="data-table">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column">{{ column }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="rows.length === 0">
          <td :colspan="columns.length" class="empty-state-cell">No records found.</td>
        </tr>
        <slot v-else />
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  columns: string[]
  rows: unknown[]
}>()
</script>

<style scoped>
.table-card { padding: 0; overflow: hidden; }
.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-bottom: 1px solid var(--color-border);
}
.table-header h3 {
  margin: 0;
  font-size: var(--text-base);
}
.table-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; padding: 0.75rem var(--space-lg); font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); background: #f8fafc; border-bottom: 1px solid var(--color-border); }
.data-table td { padding: 0.75rem var(--space-lg); font-size: var(--text-sm); border-bottom: 1px solid var(--color-border); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #f8fafc; }
.empty-state-cell {
  color: var(--color-text-muted);
  text-align: center;
}
</style>
