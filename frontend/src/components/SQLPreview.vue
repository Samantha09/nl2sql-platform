<template>
  <el-card v-if="sql" header="生成的 SQL" style="margin-top: 16px">
    <pre>{{ sql }}</pre>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ sql?: string }>()
</script>
