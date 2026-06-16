<template>
  <div class="query-page">
    <div class="page-header">
      <h2 class="page-title">自然语言查询</h2>
      <p class="page-desc">描述你的问题，AI 将自动转换为 SQL 并返回结果</p>
    </div>

    <query-input :loading="loading" @submit="onSubmit" />

    <div v-if="error" class="alert alert-error">
      <span class="alert-icon">⚠️</span>
      <span>{{ error }}</span>
    </div>

    <s-q-l-preview :sql="result?.sql" />

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <div class="loading-text">正在生成 SQL 并执行查询...</div>
    </div>

    <result-table :data="result?.data" :has-query="hasQuery" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import QueryInput from '../components/QueryInput.vue'
import SQLPreview from '../components/SQLPreview.vue'
import ResultTable from '../components/ResultTable.vue'
import { nlQuery } from '../api/query'

const result = ref<any>(null)
const loading = ref(false)
const error = ref('')
const hasQuery = ref(false)

const onSubmit = async (text: string) => {
  loading.value = true
  error.value = ''
  hasQuery.value = true
  result.value = null

  try {
    const res: any = await nlQuery({
      userId: 1,
      dataSourceId: 1,
      naturalLanguage: text,
      conversationId: 'demo'
    })

    if (res.code === 200) {
      result.value = res.data
    } else {
      error.value = res.message || '查询失败'
    }
  } catch (err: any) {
    error.value = err.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.query-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-8);
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--spacing-2);
}

.page-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0;
}

.alert {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.alert-error {
  background: #fef2f2;
  color: var(--color-error);
  border: 1px solid #fecaca;
}

.alert-icon {
  font-size: 16px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-4);
  margin-top: var(--spacing-8);
  padding: var(--spacing-8);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
