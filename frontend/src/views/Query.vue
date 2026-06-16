<template>
  <div>
    <h3>自然语言查询</h3>
    <query-input @submit="onSubmit" />
    <s-q-l-preview :sql="result?.sql" />
    <el-table v-if="result?.data" :data="result.data" style="margin-top: 16px">
      <el-table-column v-for="key in columnKeys" :key="key" :prop="key" :label="key" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import QueryInput from '../components/QueryInput.vue'
import SQLPreview from '../components/SQLPreview.vue'
import { nlQuery } from '../api/query'

const result = ref<any>(null)

const columnKeys = computed(() => {
  if (!result.value?.data?.length) return []
  return Object.keys(result.value.data[0])
})

const onSubmit = async (text: string) => {
  const res: any = await nlQuery({
    userId: 1,
    dataSourceId: 1,
    naturalLanguage: text,
    conversationId: 'demo'
  })
  if (res.code === 200) {
    result.value = res.data
  }
}
</script>
