<template>
  <div class="query-input-wrapper">
    <div class="input-card">
      <input
        v-model="text"
        type="text"
        class="query-input"
        placeholder="输入自然语言查询，例如：查上月销售额前10的产品"
        @keyup.enter="submit"
      />
      <button
        class="submit-btn"
        :disabled="!text.trim() || loading"
        @click="submit"
      >
        <span v-if="loading" class="btn-spinner">⏳</span>
        <span>{{ loading ? '生成中...' : '查询' }}</span>
      </button>
    </div>
    <p class="input-hint">按 Enter 快速提交，当前为演示模式返回 Mock 数据</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [text: string]
}>()

const text = ref('')

const submit = () => {
  if (!text.value.trim() || props.loading) return
  emit('submit', text.value)
}
</script>

<style scoped>
.query-input-wrapper {
  width: 100%;
}

.input-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-2);
  background: var(--color-surface);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
}

.input-card:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 4px rgba(8, 145, 178, 0.08);
}

.query-input {
  flex: 1;
  border: none;
  outline: none;
  padding: var(--spacing-3) var(--spacing-4);
  font-size: 16px;
  color: var(--color-text);
  background: transparent;
}

.query-input::placeholder {
  color: var(--color-text-muted);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-6);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-hint {
  margin: var(--spacing-3) 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
}

@media (max-width: 640px) {
  .input-card {
    flex-direction: column;
  }

  .submit-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
