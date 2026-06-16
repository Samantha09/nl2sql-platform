<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-badge">✨ 自然语言 → SQL → 可视化结果</div>
      <h2 class="hero-title">
        让数据查询像聊天一样简单
      </h2>
      <p class="hero-desc">
        连接你的数据库，用中文描述问题，平台自动分析 Schema、生成 SQL 并返回清晰的结果图表。
      </p>
      <div class="hero-actions">
        <button class="btn btn-primary" @click="$router.push('/query')">
          开始查询
        </button>
        <button class="btn btn-secondary" @click="$router.push('/datasource')">
          配置数据源
        </button>
      </div>
    </section>

    <section class="features">
      <div class="feature-card">
        <div class="feature-icon feature-icon-blue">💬</div>
        <h3 class="feature-title">自然语言提问</h3>
        <p class="feature-desc">无需掌握 SQL，用日常中文描述你想知道的数据问题。</p>
      </div>

      <div class="feature-card">
        <div class="feature-icon feature-icon-cyan">⚡</div>
        <h3 class="feature-title">自动 SQL 生成</h3>
        <p class="feature-desc">结合数据库 Schema 上下文，由 AI 生成可执行的查询语句。</p>
      </div>

      <div class="feature-card">
        <div class="feature-icon feature-icon-green">📊</div>
        <h3 class="feature-title">可视化结果</h3>
        <p class="feature-desc">查询结果自动以表格、图表形式展示，支持历史会话回溯。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 960px;
  margin: 0 auto;
}

.hero {
  text-align: center;
  padding: var(--spacing-12) var(--spacing-6);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  background: #ecfeff;
  color: var(--color-accent);
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: var(--spacing-6);
}

.hero-title {
  font-size: 36px;
  font-weight: 800;
  color: var(--color-text);
  margin: 0 0 var(--spacing-4);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  max-width: 560px;
  margin: 0 auto var(--spacing-6);
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: var(--spacing-4);
}

.btn {
  padding: var(--spacing-3) var(--spacing-6);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  box-shadow: var(--shadow-sm);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.btn-secondary {
  background: white;
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: #f8fafc;
}

.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-6);
  margin-top: var(--spacing-8);
}

.feature-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: var(--spacing-4);
}

.feature-icon-blue {
  background: #eff6ff;
}

.feature-icon-cyan {
  background: #ecfeff;
}

.feature-icon-green {
  background: #f0fdf4;
}

.feature-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 var(--spacing-2);
  color: var(--color-text);
}

.feature-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0;
}

@media (max-width: 768px) {
  .features {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-actions {
    flex-direction: column;
  }
}
</style>
