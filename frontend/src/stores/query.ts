import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useQueryStore = defineStore('query', () => {
  const result = ref<any>(null)
  const loading = ref(false)

  const setResult = (data: any) => {
    result.value = data
  }

  return { result, loading, setResult }
})
