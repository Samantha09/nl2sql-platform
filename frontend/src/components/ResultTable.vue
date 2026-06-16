<template>
  <div v-if="data?.length" class="result-section">
    <div class="result-header">
      <div class="result-title">
        <span class="title-icon">📊</span>
        <span>查询结果</span>
      </div>
      <div class="result-meta">共 {{ data.length }} 条记录</div>
    </div>

    <div class="table-wrapper">
      <table class="result-table">
        <thead>
          <tr>
            <th v-for="key in columnKeys" :key="key">{{ key }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in data" :key="index">
            <td v-for="key in columnKeys" :key="key">{{ formatValue(row[key]) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div v-else-if="hasQuery" class="empty-state">
    <div class="empty-icon">📭</div>
    <div class="empty-title">暂无结果</div>
    <div class="empty-desc">该查询没有返回任何数据</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data?: Record<string, any>[]
  hasQuery?: boolean
}>()

const columnKeys = computed(() => {
  if (!props.data?.length) return []
  return Object.keys(props.data[0])
})

const formatValue = (value: any) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return value
}
</script>

<style scoped>
.result-section {
  margin-top: var(--spacing-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3) var(--spacing-4);
  background: #f8fafc;
  border-bottom: 1px solid var(--color-border);
}

.result-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.title-icon {
  font-size: 15px;
}

.result-meta {
  font-size: 13px;
  color: var(--color-text-muted);
}

.table-wrapper {
  overflow-x: auto;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.result-table th,
.result-table td {
  padding: var(--spacing-3) var(--spacing-4);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.result-table th {
  background: #f8fafc;
  font-weight: 600;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.result-table td {
  color: var(--color-text);
}

.result-table tbody tr:hover {
  background: #f8fafc;
}

.result-table tbody tr:last-child td {
  border-bottom: none;
}

.empty-state {
  margin-top: var(--spacing-6);
  padding: var(--spacing-10) var(--spacing-6);
  text-align: center;
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: var(--spacing-3);
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-1);
}

.empty-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
