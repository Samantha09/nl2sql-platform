import { Series, TableMeta, KBItem } from '../types';

/* 聚合数据（图表用） */
export const DATA: Record<string, Series[]> = {
  revenue: [
    { label: '2025-01', value: 62100 }, { label: '2025-02', value: 58400 },
    { label: '2025-03', value: 71800 }, { label: '2025-04', value: 67900 },
    { label: '2025-05', value: 79200 }, { label: '2025-06', value: 84320 },
  ],
  status: [
    { label: '已支付', value: 620 }, { label: '已发货', value: 210 },
    { label: '已退款', value: 90 }, { label: '待处理', value: 80 },
  ],
  topProducts: [
    { label: '无线降噪耳机', value: 312 }, { label: '智能手表 Pro', value: 268 },
    { label: '机械键盘', value: 241 }, { label: '4K 显示器', value: 198 },
    { label: '便携充电宝', value: 176 }, { label: '蓝牙音箱', value: 154 },
    { label: '人体工学椅', value: 132 }, { label: '游戏鼠标', value: 121 },
    { label: 'USB-C 扩展坞', value: 109 }, { label: '高清摄像头', value: 93 },
  ],
  category: [
    { label: '数码', value: 480 }, { label: '影音', value: 260 },
    { label: '外设', value: 150 }, { label: '家具', value: 70 }, { label: '配件', value: 40 },
  ],
  city: [
    { label: '上海', value: 38 }, { label: '北京', value: 34 }, { label: '深圳', value: 29 },
    { label: '广州', value: 24 }, { label: '杭州', value: 21 }, { label: '成都', value: 18 },
    { label: '武汉', value: 15 }, { label: '南京', value: 13 }, { label: '其他', value: 8 },
  ],
};

/* 表结构 + 数据预览 */
export const SCHEMA: Record<string, TableMeta> = {
  customers: {
    rows: 200,
    cols: [
      { name: 'id', type: 'INTEGER', pk: true, nullable: false },
      { name: 'name', type: 'TEXT', nullable: false },
      { name: 'email', type: 'TEXT', nullable: false },
      { name: 'city', type: 'TEXT', nullable: true },
      { name: 'created_at', type: 'TEXT', nullable: false },
    ],
    rels: [],
    data: [
      [1, '李伟', 'liwei@example.com', '上海', '2024-08-12'],
      [2, '王芳', 'wangfang@example.com', '北京', '2024-09-03'],
      [3, '张敏', 'zhangmin@example.com', '深圳', '2024-10-21'],
      [4, '刘洋', 'liuyang@example.com', '广州', '2024-11-15'],
      [5, '陈静', 'chenjing@example.com', '杭州', '2024-12-08'],
      [6, '杨帆', 'yangfan@example.com', '成都', '2025-01-19'],
      [7, '赵磊', 'zhaolei@example.com', '武汉', '2025-02-27'],
      [8, '孙琳', 'sunlin@example.com', '南京', '2025-03-14'],
    ],
  },
  products: {
    rows: 40,
    cols: [
      { name: 'id', type: 'INTEGER', pk: true, nullable: false },
      { name: 'name', type: 'TEXT', nullable: false },
      { name: 'category', type: 'TEXT', nullable: false },
      { name: 'price', type: 'REAL', nullable: false },
      { name: 'stock', type: 'INTEGER', nullable: false },
    ],
    rels: [],
    data: [
      [1, '无线降噪耳机', '数码', 1299, 45],
      [2, '智能手表 Pro', '数码', 2399, 30],
      [3, '机械键盘', '外设', 599, 80],
      [4, '4K 显示器', '影音', 2899, 18],
      [5, '便携充电宝', '配件', 199, 200],
      [6, '蓝牙音箱', '影音', 449, 60],
      [7, '人体工学椅', '家具', 1599, 12],
      [8, '游戏鼠标', '外设', 299, 95],
    ],
  },
  orders: {
    rows: 1000,
    cols: [
      { name: 'id', type: 'INTEGER', pk: true, nullable: false },
      { name: 'customer_id', type: 'INTEGER', fk: 'customers.id', nullable: false },
      { name: 'product_id', type: 'INTEGER', fk: 'products.id', nullable: false },
      { name: 'quantity', type: 'INTEGER', nullable: false },
      { name: 'amount', type: 'REAL', nullable: false },
      { name: 'status', type: 'TEXT', nullable: false },
      { name: 'created_at', type: 'TEXT', nullable: false },
    ],
    rels: ['orders.customer_id → customers.id', 'orders.product_id → products.id'],
    data: [
      [1001, 1, 1, 1, 1299, '已支付', '2025-06-10'],
      [1002, 2, 3, 2, 1198, '已发货', '2025-06-11'],
      [1003, 3, 5, 1, 199, '已支付', '2025-06-12'],
      [1004, 4, 4, 1, 2899, '已退款', '2025-06-12'],
      [1005, 5, 2, 1, 2399, '已支付', '2025-06-13'],
      [1006, 6, 8, 3, 897, '待处理', '2025-06-14'],
      [1007, 7, 6, 2, 898, '已发货', '2025-06-15'],
      [1008, 8, 7, 1, 1599, '已支付', '2025-06-16'],
    ],
  },
};

/* 树结构：库 → 模式 → 表 */
export const DBTREE = {
  name: 'demo.db',
  schemas: [{ name: 'main', tables: ['customers', 'products', 'orders'] }],
};

/* mock 自然语言 → SQL 知识库 */
export const KB: KBItem[] = [
  {
    keys: ['每月', '月度', '月份'], label: '每月销售额趋势', intent: '月度销售额趋势', tables: ['orders'],
    sql: `SELECT strftime('%Y-%m', created_at) AS month,\n       SUM(amount) AS total\nFROM orders\nGROUP BY month\nORDER BY month;`,
    cols: ['month', 'total'],
    rows: [['2025-01', '¥62,100'], ['2025-02', '¥58,400'], ['2025-03', '¥71,800'], ['2025-04', '¥67,900'], ['2025-05', '¥79,200'], ['2025-06', '¥84,320']],
    chart: 'line', data: DATA.revenue,
  },
  {
    keys: ['前十', 'top', '排名'], label: '销量前十的商品', intent: '商品销量 Top 10', tables: ['orders', 'products'],
    sql: `SELECT p.name, SUM(o.quantity) AS sold\nFROM orders o\nJOIN products p ON p.id = o.product_id\nGROUP BY p.id\nORDER BY sold DESC\nLIMIT 10;`,
    cols: ['name', 'sold'], rows: DATA.topProducts.map(d => [d.label, d.value]),
    chart: 'bar', data: DATA.topProducts,
  },
  {
    keys: ['品类', '分类', '类别'], label: '各品类订单占比', intent: '品类订单占比', tables: ['orders', 'products'],
    sql: `SELECT p.category, COUNT(*) AS orders\nFROM orders o\nJOIN products p ON p.id = o.product_id\nGROUP BY p.category;`,
    cols: ['category', 'orders'], rows: DATA.category.map(d => [d.label, d.value]),
    chart: 'donut', data: DATA.category,
  },
  {
    keys: ['城市', '地区'], label: '各城市客户数', intent: '城市客户分布', tables: ['customers'],
    sql: `SELECT city, COUNT(*) AS customers\nFROM customers\nGROUP BY city\nORDER BY customers DESC;`,
    cols: ['city', 'customers'], rows: DATA.city.map(d => [d.label, d.value]),
    chart: 'bar', data: DATA.city,
  },
  {
    keys: ['状态'], label: '订单状态分布', intent: '订单状态分布', tables: ['orders'],
    sql: `SELECT status, COUNT(*) AS cnt\nFROM orders\nGROUP BY status;`,
    cols: ['status', 'cnt'], rows: DATA.status.map(d => [d.label, d.value]),
    chart: 'donut', data: DATA.status,
  },
];
