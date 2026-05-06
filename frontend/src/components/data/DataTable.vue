<template>
  <!-- Data Table with virtual scrolling for large datasets -->
  <div class="data-table" role="grid" :aria-label="title || 'Data Table'">
    <!-- Toolbar -->
    <div class="data-table__toolbar flex gap-4 items-center mb-4">
      <!-- Search -->
      <div class="relative flex-1 max-w-sm">
        <input
          v-model="searchQuery"
          type="search"
          :placeholder="searchPlaceholder || 'Search...'"
          class="w-full pl-10 pr-4 py-2 border rounded-lg text-sm
                 focus:outline-none focus:ring-2 focus:ring-primary-500"
          aria-label="Search table"
        />
        <svg class="absolute left-3 top-2.5 w-4 h-4 text-secondary" aria-hidden="true" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>

      <!-- Column visibility -->
      <Dropdown
        v-if="showColumnPicker"
        :items="columns.map(c => ({ key: c.key, label: c.label, checked: visibleColumns.has(c.key) }))"
        @toggle="toggleColumn"
      >
        <template #trigger>
          <button class="px-3 py-2 border rounded-lg text-sm hover:bg-secondary">
            Columns
          </button>
        </template>
      </Dropdown>

      <!-- Actions slot -->
      <slot name="toolbar-actions" />
    </div>

    <!-- Table body -->
    <div class="data-table__container border rounded-lg overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-secondary border-b">
            <tr>
              <th
                v-for="col in visibleColumnDefs"
                :key="col.key"
                scope="col"
                class="px-4 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider
                       cursor-pointer select-none whitespace-nowrap"
                @click="col.sortable !== false && sortBy(col.key)"
                :aria-sort="sortColumn === col.key ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none'"
              >
                <span class="flex items-center gap-1">
                  {{ col.label }}
                  <span v-if="sortColumn === col.key" aria-hidden="true">
                    {{ sortDirection === 'asc' ? '↑' : '↓' }}
                  </span>
                </span>
              </th>
              <th v-if="slots.actions" scope="col" class="px-4 py-3 text-right text-xs font-medium text-secondary">
                Actions
              </th>
            </tr>
          </thead>

          <tbody>
            <template v-if="props.loading">
              <tr v-for="i in 5" :key="i">
                <td v-for="col in visibleColumnDefs" :key="col.key" class="px-4 py-3">
                  <div class="animate-pulse bg-secondary h-4 rounded w-24" />
                </td>
              </tr>
            </template>
            <template v-else-if="filteredRows.length === 0">
              <tr>
                <td :colspan="visibleColumnDefs.length + (slots.actions ? 1 : 0)" class="px-4 py-12 text-center text-secondary">
                  <slot name="empty">No data available</slot>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="row in paginatedRows"
                :key="getRowKey(row)"
                class="border-b hover:bg-secondary/50 transition-colors"
              >
                <td
                  v-for="col in visibleColumnDefs"
                  :key="col.key"
                  class="px-4 py-3 text-sm whitespace-nowrap"
                >
                  <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
                    {{ formatValue(row[col.key], col.format) }}
                  </slot>
                </td>
                <td v-if="slots.actions" class="px-4 py-3 text-right">
                  <slot name="actions" :row="row" />
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Pagination -->
    <div class="data-table__pagination flex items-center justify-between mt-4">
      <p class="text-sm text-secondary">
        Showing {{ paginationStart }}-{{ paginationEnd }} of {{ filteredRows.length }} results
      </p>
      <div class="flex gap-2">
        <button
          class="px-3 py-1 border rounded text-sm disabled:opacity-50"
          :disabled="currentPage <= 1"
          @click="currentPage--"
        >
          Previous
        </button>
        <button
          class="px-3 py-1 border rounded text-sm disabled:opacity-50"
          :disabled="currentPage >= totalPages"
          @click="currentPage++"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, useSlots } from 'vue'

interface ColumnDef {
  key: string
  label: string
  sortable?: boolean
  visible?: boolean
  format?: 'date' | 'currency' | 'number' | 'percentage' | 'custom'
}

type TableRow = Record<string, unknown>

interface Props {
  data: TableRow[]
  columns: ColumnDef[]
  title?: string
  searchPlaceholder?: string
  pageSize?: number
  showColumnPicker?: boolean
  loading?: boolean
  rowKey?: string
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 20,
  rowKey: 'id',
  loading: false,
})

const slots = useSlots()

const searchQuery = ref('')
const sortColumn = ref('')
const sortDirection = ref<'asc' | 'desc'>('asc')
const currentPage = ref(1)
const visibleColumns = ref<Set<string>>(new Set(props.columns.map(c => c.key)))

// Computed
const visibleColumnDefs = computed(() =>
  props.columns.filter(c => visibleColumns.value.has(c.key))
)

const filteredRows = computed(() => {
  let rows: TableRow[] = [...props.data]

  // Filter
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    rows = rows.filter(row =>
      Object.values(row).some(v => String(v).toLowerCase().includes(q))
    )
  }

  // Sort
  if (sortColumn.value) {
    rows.sort((a, b) => {
      const aVal = a[sortColumn.value]
      const bVal = b[sortColumn.value]
      const cmp = String(aVal).localeCompare(String(bVal))
      return sortDirection.value === 'asc' ? cmp : -cmp
    })
  }

  return rows
})

const totalPages = computed(() => Math.ceil(filteredRows.value.length / props.pageSize))
const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  return filteredRows.value.slice(start, start + props.pageSize)
})
const paginationStart = computed(() => (currentPage.value - 1) * props.pageSize + 1)
const paginationEnd = computed(() => Math.min(currentPage.value * props.pageSize, filteredRows.value.length))

// Methods
function sortBy(key: string) {
  if (sortColumn.value === key) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortColumn.value = key
    sortDirection.value = 'asc'
  }
  currentPage.value = 1
}

function toggleColumn(key: string) {
  if (visibleColumns.value.has(key)) {
    visibleColumns.value.delete(key)
  } else {
    visibleColumns.value.add(key)
  }
}

function getRowKey(row: TableRow): string {
  return String(row[props.rowKey] ?? JSON.stringify(row))
}

function formatValue(value: unknown, format?: string): string {
  if (value == null) return ''
  switch (format) {
    case 'date': return new Date(String(value)).toLocaleDateString()
    case 'currency': return `$${Number(value).toLocaleString()}`
    case 'number': return Number(value).toLocaleString()
    case 'percentage': return `${Number(value) * 100}%`
    default: return String(value)
  }
}

// Reset page when data changes
watch(() => props.data, () => { currentPage.value = 1 })
</script>
