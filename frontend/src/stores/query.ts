import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface QueryRecord {
  id: string
  naturalLanguage: string
  sql?: string
  data?: Record<string, any>[]
  totalCount?: number
  executeTimeMs?: number
  createdAt: Date
}

export const useQueryStore = defineStore('query', () => {
  const result = ref<any>(null)
  const loading = ref(false)
  const error = ref('')
  const history = ref<QueryRecord[]>([])

  const hasResult = computed(() => !!result.value)

  const setResult = (data: any) => {
    result.value = data
  }

  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const setError = (message: string) => {
    error.value = message
  }

  const addToHistory = (record: QueryRecord) => {
    history.value.unshift(record)
    if (history.value.length > 50) {
      history.value = history.value.slice(0, 50)
    }
  }

  const clearResult = () => {
    result.value = null
    error.value = ''
  }

  return {
    result,
    loading,
    error,
    history,
    hasResult,
    setResult,
    setLoading,
    setError,
    addToHistory,
    clearResult
  }
})
