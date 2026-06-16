<template>
  <div v-if="sql" class="sql-preview">
    <div class="preview-header">
      <div class="preview-title">
        <span class="title-icon">⚡</span>
        <span>生成的 SQL</span>
      </div>
      <button class="copy-btn" @click="copySql">{{ copied ? '已复制' : '复制' }}</button>
    </div>
    <pre class="preview-body"><code>{{ sql }}</code></pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  sql?: string
}>()

const copied = ref(false)

const copySql = async () => {
  if (!props.sql) return
  try {
    await navigator.clipboard.writeText(props.sql)
    copied.value = true
    setTimeout(() => copied.value = false, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}
</script>

<style scoped>
.sql-preview {
  margin-top: var(--spacing-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3) var(--spacing-4);
  background: #f8fafc;
  border-bottom: 1px solid var(--color-border);
}

.preview-title {
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

.copy-btn {
  padding: var(--spacing-1) var(--spacing-3);
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.preview-body {
  margin: 0;
  padding: var(--spacing-4);
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.6;
  overflow-x: auto;
}

.preview-body code {
  background: transparent;
  color: inherit;
}
</style>
