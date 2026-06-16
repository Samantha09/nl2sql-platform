<template>
  <div class="placeholder-page">
    <div class="placeholder-card">
      <div class="placeholder-icon">📋</div>
      <h2 class="placeholder-title">结果详情</h2>
      <p class="placeholder-desc">展示单次查询的完整结果，支持表格、图表等多视图切换。</p>
      <div class="placeholder-status">页面开发中，敬请期待</div>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.placeholder-card {
  text-align: center;
  padding: var(--spacing-10) var(--spacing-8);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  max-width: 420px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-4);
}

.placeholder-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--spacing-2);
}

.placeholder-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 var(--spacing-4);
}

.placeholder-status {
  display: inline-block;
  padding: var(--spacing-2) var(--spacing-4);
  background: #f1f5f9;
  color: var(--color-text-muted);
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 500;
}
</style>
