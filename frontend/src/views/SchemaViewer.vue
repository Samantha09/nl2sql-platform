<template>
  <div class="schema-page">
    <!-- 顶部工具栏 -->
    <div class="schema-toolbar">
      <div class="toolbar-title">
        <span class="title-icon">⊞</span>
        <div>
          <h2 class="page-title">Schema 查看器</h2>
          <p class="page-desc">浏览多数据源的表结构与字段信息</p>
        </div>
      </div>

      <div class="toolbar-actions">
        <select v-model="selectedDataSource" class="ds-select">
          <option v-for="ds in dataSources" :key="ds.id" :value="ds.id">
            {{ ds.name }} ({{ ds.type }})
          </option>
        </select>
        <button class="refresh-btn" :disabled="refreshing" @click="refreshSchema">
          <span class="refresh-icon" :class="{ spinning: refreshing }">🔄</span>
          <span>{{ refreshing ? '刷新中...' : '刷新 Schema' }}</span>
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="schema-body">
      <!-- 左侧表列表 -->
      <aside class="schema-sidebar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            v-model="tableSearch"
            type="text"
            class="search-input"
            placeholder="搜索表名..."
          />
        </div>

        <div class="table-list">
          <div
            v-for="table in filteredTables"
            :key="table.name"
            class="table-item"
            :class="{ active: selectedTable?.name === table.name }"
            @click="selectTable(table)"
          >
            <span class="table-icon">▦</span>
            <div class="table-info">
              <div class="table-name">{{ table.name }}</div>
              <div v-if="table.comment" class="table-comment">{{ table.comment }}</div>
            </div>
          </div>

          <div v-if="filteredTables.length === 0" class="list-empty">
            未找到匹配的表
          </div>
        </div>
      </aside>

      <!-- 右侧表详情 -->
      <main class="schema-detail">
        <div v-if="selectedTable" class="detail-content">
          <div class="detail-header">
            <div class="detail-title">
              <h3>{{ selectedTable.name }}</h3>
              <span v-if="selectedTable.comment" class="detail-comment">{{ selectedTable.comment }}</span>
            </div>
            <div v-if="schemaDetail?.primaryKeys?.length" class="pk-tags">
              <span class="pk-label">主键：</span>
              <span
                v-for="pk in schemaDetail.primaryKeys"
                :key="pk"
                class="pk-tag"
              >{{ pk }}</span>
            </div>
          </div>

          <!-- 字段信息 -->
          <section class="detail-section">
            <div class="section-title">
              <span class="section-icon">📋</span>
              <span>字段信息</span>
            </div>

            <div class="data-table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>字段名</th>
                    <th>类型</th>
                    <th>可空</th>
                    <th>默认值</th>
                    <th>注释</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="col in schemaDetail?.columns"
                    :key="col.name"
                    :class="{ 'pk-row': col.isPrimaryKey }"
                  >
                    <td>
                      <span class="col-name">{{ col.name }}</span>
                      <span v-if="col.isPrimaryKey" class="pk-badge">PK</span>
                    </td>
                    <td><code class="type-code">{{ col.type }}</code></td>
                    <td>
                      <span :class="['nullable-badge', col.nullable ? 'yes' : 'no']">
                        {{ col.nullable ? '是' : '否' }}
                      </span>
                    </td>
                    <td>{{ col.defaultValue || '-' }}</td>
                    <td>{{ col.comment || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- 索引信息 -->
          <section v-if="schemaDetail?.indexes?.length" class="detail-section">
            <div class="section-title">
              <span class="section-icon">🔑</span>
              <span>索引信息</span>
            </div>

            <div class="index-list">
              <div
                v-for="idx in schemaDetail.indexes"
                :key="idx.name"
                class="index-card"
              >
                <div class="index-name">{{ idx.name }}</div>
                <div class="index-meta">
                  <span class="index-columns">{{ idx.columns.join(', ') }}</span>
                  <span v-if="idx.unique" class="unique-badge">唯一</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div v-else class="detail-empty">
          <div class="empty-icon">⊞</div>
          <div class="empty-title">选择一张表</div>
          <div class="empty-desc">从左侧列表选择表，查看字段、索引与主键信息</div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface DataSource {
  id: number
  name: string
  type: 'mysql' | 'clickhouse' | 'postgresql'
  host: string
  port: number
  databaseName: string
}

interface TableInfo {
  name: string
  comment?: string
}

interface ColumnInfo {
  name: string
  type: string
  comment?: string
  nullable: boolean
  defaultValue?: string
  isPrimaryKey: boolean
}

interface IndexInfo {
  name: string
  columns: string[]
  unique: boolean
}

interface TableSchema {
  tableName: string
  tableComment?: string
  columns: ColumnInfo[]
  primaryKeys: string[]
  indexes: IndexInfo[]
}

// 演示数据
const dataSources = ref<DataSource[]>([
  { id: 1, name: 'mysql_prod', type: 'mysql', host: 'localhost', port: 3306, databaseName: 'shop' },
  { id: 2, name: 'clickhouse_log', type: 'clickhouse', host: 'localhost', port: 8123, databaseName: 'logs' }
])

const selectedDataSource = ref(1)
const refreshing = ref(false)
const tableSearch = ref('')
const selectedTable = ref<TableInfo | null>(null)

const tables = ref<TableInfo[]>([
  { name: 'users', comment: '用户表' },
  { name: 'orders', comment: '订单表' },
  { name: 'products', comment: '商品表' },
  { name: 'order_items', comment: '订单明细表' }
])

const schemaDetail = ref<TableSchema | null>(null)

const filteredTables = computed(() => {
  const keyword = tableSearch.value.trim().toLowerCase()
  if (!keyword) return tables.value
  return tables.value.filter(t => t.name.toLowerCase().includes(keyword))
})

const selectTable = (table: TableInfo) => {
  selectedTable.value = table
  // 演示：根据表名生成不同的 Mock 详情
  schemaDetail.value = generateMockSchema(table.name)
}

const refreshSchema = () => {
  refreshing.value = true
  setTimeout(() => {
    refreshing.value = false
  }, 800)
}

const generateMockSchema = (tableName: string): TableSchema => {
  if (tableName === 'users') {
    return {
      tableName: 'users',
      tableComment: '用户表',
      primaryKeys: ['id'],
      columns: [
        { name: 'id', type: 'BIGINT', nullable: false, defaultValue: 'AUTO_INCREMENT', isPrimaryKey: true, comment: '主键' },
        { name: 'username', type: 'VARCHAR(50)', nullable: false, isPrimaryKey: false, comment: '用户名' },
        { name: 'email', type: 'VARCHAR(100)', nullable: true, isPrimaryKey: false, comment: '邮箱' },
        { name: 'created_at', type: 'DATETIME', nullable: false, defaultValue: 'CURRENT_TIMESTAMP', isPrimaryKey: false, comment: '创建时间' }
      ],
      indexes: [
        { name: 'idx_username', columns: ['username'], unique: true },
        { name: 'idx_email', columns: ['email'], unique: false }
      ]
    }
  }

  if (tableName === 'orders') {
    return {
      tableName: 'orders',
      tableComment: '订单表',
      primaryKeys: ['id'],
      columns: [
        { name: 'id', type: 'BIGINT', nullable: false, defaultValue: 'AUTO_INCREMENT', isPrimaryKey: true, comment: '主键' },
        { name: 'user_id', type: 'BIGINT', nullable: false, isPrimaryKey: false, comment: '用户 ID' },
        { name: 'total_amount', type: 'DECIMAL(18,2)', nullable: false, isPrimaryKey: false, comment: '订单总金额' },
        { name: 'status', type: 'VARCHAR(20)', nullable: false, defaultValue: 'pending', isPrimaryKey: false, comment: '订单状态' },
        { name: 'created_at', type: 'DATETIME', nullable: false, defaultValue: 'CURRENT_TIMESTAMP', isPrimaryKey: false, comment: '创建时间' }
      ],
      indexes: [
        { name: 'idx_user_id', columns: ['user_id'], unique: false },
        { name: 'idx_created_at', columns: ['created_at'], unique: false }
      ]
    }
  }

  return {
    tableName,
    columns: [
      { name: 'id', type: 'BIGINT', nullable: false, isPrimaryKey: true, comment: '主键' },
      { name: 'name', type: 'VARCHAR(100)', nullable: false, isPrimaryKey: false, comment: '名称' }
    ],
    primaryKeys: ['id'],
    indexes: []
  }
}
</script>

<style scoped>
.schema-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 48px);
  min-height: 600px;
}

.schema-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-5) var(--spacing-6);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.title-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 22px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--spacing-1);
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.ds-select {
  padding: var(--spacing-2) var(--spacing-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: white;
  font-size: 14px;
  color: var(--color-text);
  min-width: 180px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--color-primary-light);
}

.refresh-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.refresh-icon {
  display: inline-block;
}

.refresh-icon.spinning {
  animation: spin 1s linear infinite;
}

.schema-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.schema-sidebar {
  width: 280px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}

.search-box {
  position: relative;
  padding: var(--spacing-3) var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.search-icon {
  position: absolute;
  left: calc(var(--spacing-4) + var(--spacing-2));
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  font-size: 13px;
}

.search-input {
  width: 100%;
  padding: var(--spacing-2) var(--spacing-3) var(--spacing-2) calc(var(--spacing-3) + 20px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  outline: none;
}

.search-input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.08);
}

.table-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-2);
}

.table-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.table-item:hover {
  background: #f8fafc;
}

.table-item.active {
  background: #eff6ff;
}

.table-icon {
  color: var(--color-text-muted);
  font-size: 16px;
  margin-top: 2px;
}

.table-info {
  flex: 1;
  min-width: 0;
}

.table-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.table-comment {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-empty {
  padding: var(--spacing-8) var(--spacing-4);
  text-align: center;
  color: var(--color-text-muted);
  font-size: 13px;
}

.schema-detail {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-6);
  background: var(--color-bg);
}

.detail-content {
  max-width: 960px;
}

.detail-header {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  margin-bottom: var(--spacing-5);
  box-shadow: var(--shadow-sm);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-3);
}

.detail-title h3 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.detail-comment {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.pk-tags {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: 14px;
}

.pk-label {
  color: var(--color-text-secondary);
}

.pk-tag {
  padding: var(--spacing-1) var(--spacing-2);
  background: #fef3c7;
  color: #b45309;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.detail-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  margin-bottom: var(--spacing-5);
  box-shadow: var(--shadow-sm);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: var(--spacing-4);
}

.section-icon {
  font-size: 18px;
}

.data-table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: var(--spacing-3) var(--spacing-4);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.data-table th {
  background: #f8fafc;
  font-weight: 600;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background: #f8fafc;
}

.pk-row {
  background: #fffbeb;
}

.col-name {
  font-weight: 600;
  color: var(--color-text);
}

.pk-badge {
  margin-left: var(--spacing-2);
  padding: 1px var(--spacing-2);
  background: #f59e0b;
  color: white;
  border-radius: var(--radius-sm);
  font-size: 10px;
  font-weight: 700;
}

.type-code {
  background: #f1f5f9;
  color: var(--color-accent);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-family: 'Fira Code', monospace;
}

.nullable-badge {
  font-size: 12px;
  padding: 2px var(--spacing-2);
  border-radius: var(--radius-sm);
}

.nullable-badge.yes {
  background: #f1f5f9;
  color: var(--color-text-secondary);
}

.nullable-badge.no {
  background: #fef2f2;
  color: var(--color-error);
}

.index-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-3);
}

.index-card {
  padding: var(--spacing-3) var(--spacing-4);
  background: #f8fafc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.index-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-1);
}

.index-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: 13px;
}

.index-columns {
  color: var(--color-text-secondary);
  font-family: 'Fira Code', monospace;
}

.unique-badge {
  padding: 1px var(--spacing-2);
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 600;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.empty-icon {
  font-size: 56px;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-4);
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
}

.empty-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .schema-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-3);
  }

  .toolbar-actions {
    width: 100%;
  }

  .ds-select {
    flex: 1;
  }

  .schema-sidebar {
    width: 220px;
  }
}
</style>
