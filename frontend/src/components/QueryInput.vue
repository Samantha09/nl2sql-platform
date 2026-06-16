<template>
  <el-input
    v-model="text"
    placeholder="输入自然语言查询，例如：查上月销售额前10的产品"
    @keyup.enter="submit"
  >
    <template #append>
      <el-button type="primary" @click="submit">查询</el-button>
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ submit: [text: string] }>()
const text = ref('')

const submit = () => {
  if (text.value.trim()) {
    emit('submit', text.value)
  }
}
</script>
