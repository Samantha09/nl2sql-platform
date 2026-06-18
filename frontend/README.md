# AI 数据库助手 · 前端

一个「AI 管理数据库」系统的前端原型，演示三大能力：**库表结构扫描**、**自然语言转 SQL 并执行**、**数据可视化统计**。

本仓库默认对接 nl2sql-platform 后端网关（开发期经 Vite proxy 转发到 `localhost:8080`）：**智能查询优先调用真实后端，后端不可用时自动回退本地 mock**，因此无需启动后端也能开箱演示。视觉采用 Supabase Studio 风格的暗色仪表盘。

> 设计原型来自同级的 `awesome-design-md` 仓库（Supabase 的 `DESIGN.md`），本工程是其 React 工程化落地。

## 📚 学习文档（React 18 核心概念）

用本项目真实代码讲解，见 [`docs/react/`](./docs/react/README.md)：

- [01 · 组件与 JSX](./docs/react/01-组件与JSX.md)
- [02 · Props](./docs/react/02-Props.md)
- [03 · State 与 useState](./docs/react/03-State-useState.md)
- [04 · Hooks 进阶（useContext/useEffect/useId/自定义 Hook）](./docs/react/04-Hooks进阶.md)

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [架构与数据流](#架构与数据流)
- [视觉设计系统](#视觉设计系统)
- [mock 数据与 NL→SQL 引擎](#mock-数据与-nlsql-引擎)
- [图表实现](#图表实现)
- [快速开始](#快速开始)
- [环境要求](#环境要求)
- [后续扩展](#后续扩展)

---

## 功能特性

| 视图 | 能力 |
|------|------|
| **概览** | KPI 卡片（表数/总记录/月订单/月销售额）+ 近 6 月销售额折线图 + 订单状态饼图 + 表清单（可跳转结构） |
| **表结构** | 三级对象树：库 `demo.db` → 模式 `main` → 表 → 字段/数据；字段含类型/PK/FK/约束，数据页只读行预览，外键关系可视化 |
| **智能查询** | 自然语言输入 → mock AI 分步输出：意图理解 → 生成的 SQL（语法高亮+复制）→ 执行结果表 → 推荐图表（一键跳可视化） |
| **数据可视化** | 4 张图表卡片网格（折线/饼图/条形），每张可切换图表类型 |

**外壳**：图标导航栏 + 常驻对象树 + 主工作区（专业 DB 工具式三栏布局），对象树在所有视图始终可见。

---

## 技术栈

### 运行时与工具链

| 工具 | 版本 | 用途 |
|------|------|------|
| **Node.js** | ≥ 18（开发机 v24） | JS 运行时，Vite 与构建依赖它 |
| **npm** | ≥ 9（开发机 v11） | 包管理器 |

### 核心框架与库

| 依赖 | 版本 | 类型 | 说明 |
|------|------|------|------|
| **React** | ^18.3.1 | 运行时 | UI 视图库。全部使用**函数组件 + Hooks**（`useState` / `useContext` / `useId`），无 class 组件 |
| **react-dom** | ^18.3.1 | 运行时 | React 的 DOM 渲染器，使用 `createRoot` API（React 18 并发渲染入口） |
| **TypeScript** | ^5.6.3 | 语言 | 静态类型系统，开启 `strict` 模式。所有数据结构在 `src/types.ts` 统一定义 |
| **Vite** | ^5.4（实装 5.4.21） | 构建/开发服务器 | 下一代前端构建工具。**选型理由**：① 开发服务器基于原生 ESM + esbuild 预构建，冷启动 < 1s；② HMR 极快；③ 生产构建用 Rollup，产物小；④ 零配置开箱即用，适合纯前端原型 |

### 开发依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| **@vitejs/plugin-react** | ^4.3.4 | Vite 官方 React 插件：提供 JSX 自动转换（无需 `import React`）与 Fast Refresh（组件热更新保留状态） |
| **@types/react** | ^18.3.12 | React 类型定义 |
| **@types/react-dom** | ^18.3.1 | React DOM 类型定义 |

### 刻意未引入的依赖（及原因）

本项目是有意保持**零运行时第三方依赖**（`dependencies` 仅 React + React DOM），以保持轻量与对设计 token 的完全控制：

| 未使用 | 原因 | 何时可引入 |
|--------|------|-----------|
| 路由库（react-router 等） | 原型仅单页，用 Context 状态切换视图足够 | 需多页/深链接/浏览器后退时 |
| 状态管理库（Redux/Zustand/Jotai） | 全局状态简单，`Context + useState` 已覆盖 | 状态变复杂、跨多层共享时 |
| UI 组件库（antd/MUI/Chakra） | 需 1:1 还原 Supabase DESIGN.md，手写 CSS 更可控 | 想快速堆标准后台组件、不追求品牌还原时 |
| 图表库（Recharts/ECharts/Chart.js） | 手写 SVG 图表，零依赖、断网可用、样式完全可控 | 图表类型变多（堆叠/散点/地图）或要交互（tooltip/缩放）时 |
| CSS 框架（Tailwind/UnoCSS） | 用 CSS 变量 + 单文件样式直接承载设计 token | 团队偏好原子化 CSS 时 |
| HTTP 库（axios 等） | 纯前端 mock，无网络请求 | 接真实后端 API 时 |

---

## 项目结构

```
ai-db-frontend/
├── index.html                 # Vite 入口 HTML（挂载 #root）
├── package.json               # 依赖与脚本（dev/build/preview）
├── vite.config.ts             # Vite 配置（React 插件 + 端口 5173）
├── tsconfig.json              # TypeScript 配置（strict）
├── tsconfig.node.json         # Node 端 TS 配置（vite.config.ts 用）
├── .gitignore
└── src/
    ├── main.tsx               # 应用入口：createRoot + StoreProvider 包裹
    ├── App.tsx                # 三栏外壳 + 视图路由（按 view 条件渲染）
    ├── styles.css             # 全局样式：Supabase 暗色 token + 所有组件类
    ├── types.ts               # 全局类型定义
    │
    ├── data/
    │   └── mock.ts            # 全部 mock 数据（DATA / SCHEMA / DBTREE / KB）
    │
    ├── api/                   # 后端 API 层（fetch 封装，经 Vite proxy 转发到网关 8080）
    │   ├── http.ts            # 统一请求封装：解包 R<T>、ApiError、超时/异常处理
    │   ├── types.ts           # 后端接口契约类型（对齐 common.R 与各服务 DTO）
    │   ├── query.ts           # 查询服务端点（/query/nl、/query/sql、/query/history）
    │   ├── ai.ts              # AI 服务端点（/ai/convert、/ai/validate）
    │   ├── schema.ts          # Schema 服务端点（数据源 CRUD、表结构）
    │   └── index.ts           # 统一导出（queryApi / aiApi / schemaApi）
    │
    ├── lib/
    │   ├── store.tsx          # 全局状态（Context）：view/curTable/curMode/tree + 操作
    │   ├── nlsql.ts           # mock NL→SQL 引擎：关键词匹配 + SQL 高亮（后端失败时兜底）
    │   ├── adapt.ts           # 结果适配层：后端 QueryResult / mock KBItem → 统一视图模型
    │   ├── toast.ts           # 轻量 toast 提示工具
    │   └── theme.ts           # 图表配色常量（供 SVG 直接使用）
    │
    ├── components/
    │   ├── Rail.tsx           # 左侧窄导航栏（4 个图标 + tooltip）
    │   ├── ObjectTree.tsx     # 常驻对象树（库→模式→表→字段/数据）
    │   ├── Topbar.tsx         # 顶部标题栏 + 连接状态 + 刷新
    │   └── charts/
    │       ├── LineChart.tsx     # 折线图（SVG）
    │       ├── BarChart.tsx      # 条形图（SVG）
    │       ├── DonutChart.tsx    # 环形饼图（SVG）
    │       └── ChartRenderer.tsx # 按 type 分发到具体图表组件
    │
    └── views/
        ├── Overview.tsx       # 概览视图
        ├── SchemaView.tsx     # 表结构视图（字段 / 数据 两个 tab）
        ├── QueryView.tsx      # 智能查询视图（含本地状态机：idle/thinking/done）
        └── ChartsView.tsx     # 数据可视化视图
```

---

## 架构与数据流

### 状态管理：React Context

全局状态集中在 `src/lib/store.tsx` 的 `StoreProvider` 中，通过 `useStore()` 在任意组件消费。状态包括：

| 状态 | 类型 | 作用 |
|------|------|------|
| `view` | `'overview' \| 'schema' \| 'query' \| 'charts'` | 当前激活视图 |
| `curTable` | `string` | 当前选中的表（默认 `orders`） |
| `curMode` | `'struct' \| 'data'` | 表结构的字段/数据 tab |
| `tree` | `TreeState` | 对象树展开状态（库/模式/各表） |

暴露的操作：`go(view, table?)`、`selectTable(name)`、`selectMode(name, mode)`、`toggleNode('db'|'schema')`。

### 视图切换

`App.tsx` 按 `view` 条件渲染对应视图组件（非 CSS 显隐切换），保证切走时视图状态干净、切回时重新初始化。`QueryView` 内部用本地 `useState` 维护查询阶段机（`idle → thinking → done/empty`）。

### 数据流

```
mock.ts (静态数据)
   │
   ├──► 视图/组件 直接读取（概览 KPI、图表、表清单）
   │
   └──► lib/nlsql.ts (matchQuestion 关键词匹配)
            │
            └──► QueryView 输入问题 → 命中 KBItem → 渲染 理解/SQL/结果/图表
```

所有数据为前端内存常量，无任何异步请求。`matchQuestion` 返回命中条目后，模拟 650ms「思考」延迟以贴近真实 AI 体验。

---

## 视觉设计系统

配色与设计 token 取自 `awesome-design-md` 仓库的 **Supabase `DESIGN.md`**，并适配为暗色仪表盘底（对应真实 Supabase Studio）。所有 token 以 CSS 变量定义在 `styles.css` 的 `:root`：

| 用途 | 变量 | 值 |
|------|------|----|
| 画布 / 表面 | `--bg` `--bg-soft` `--bg-2` `--bg-side` `--bg-rail` | `#1c1c1c` / `#202020` / `#262626` / `#181818` / `#141414` |
| 翡翠主色 / 深 / 亮 | `--emerald` `--emerald-deep` `--emerald-soft` | `#3ecf8e` / `#24b47e` / `#4ade80` |
| 文字 主/次/弱 | `--ink` `--ink-2` `--ink-3` | `#ededed` / `#a0a0a0` / `#707070` |
| 分割线 | `--line` `--line-2` | `#2e2e2e` / `#353535` |
| 圆角 | `--radius` `--radius-lg` | `8px` / `12px` |
| 字体 | `--sans` / `--mono` | Circular → Inter 回退栈 / 系统等宽 |

字体回退栈包含中文（PingFang SC / Microsoft YaHei），界面文案为中文。

---

## mock 数据与 NL→SQL 引擎

`src/data/mock.ts` 导出四组数据：

| 导出 | 内容 |
|------|------|
| `DATA` | 图表聚合数据：`revenue`（月度销售额）、`status`（订单状态）、`topProducts`、`category`、`city` |
| `SCHEMA` | 三张表（`customers` / `products` / `orders`）的字段定义、外键关系、8 行示例数据 |
| `DBTREE` | 对象树结构：`demo.db` → `main` 模式 → 三张表 |
| `KB` | mock 自然语言知识库：5 条「关键词 → 意图 + SQL + 结果 + 图表」映射 |

**NL→SQL 引擎**（`src/lib/nlsql.ts`）：
- `matchQuestion(q)`：用关键词命中 `KB`，返回匹配条目（如输入含「每月/月度」+「销售」→ 月度销售额 SQL）。
- `highlightSql(sql)`：对 SQL 做关键字/函数/字符串/注释的语法着色，返回 HTML。
- 未命中时 QueryView 显示友好提示并列出示例问题。

**安全边界**（原型约定）：mock 的预置 SQL 全部为 `SELECT`，仅演示读流程；后续接真实后端时会做只读校验。

---

## 图表实现

三个图表组件（`LineChart` / `BarChart` / `DonutChart`）均为**纯手写 SVG**，无任何图表库依赖：

- **折线图**：计算坐标点 → 渐变面积 + 折线 + 数据点圆圈 + 数值标签 + Y 轴网格。
- **条形图**：按最大值等比计算柱高，每柱用调色板取色，含数值标签。
- **环形饼图**：用极坐标计算每段圆弧的 SVG path（`donutSlice`），中心显示合计，右侧图例。
- `ChartRenderer` 按 `type` 字段统一分发。
- 图表配色常量在 `src/lib/theme.ts`（与 CSS 变量色值一致，供 SVG 属性直接引用）。

优势：零依赖、断网可用、尺寸/颜色完全可控；代价是图表类型有限（无 tooltip/动画/复杂交互），后续如需可平滑替换为 Recharts 等。

---

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 生产构建（tsc 类型检查 + vite 打包，产物在 dist/）
npm run build

# 本地预览生产构建
npm run preview
```

---

## 环境要求

- **Node.js ≥ 18**（在 Node 24 上开发验证通过）
- **npm ≥ 9**

无其它系统级依赖，无需数据库、无需 API Key。

---

## 后续扩展

本前端为「全 mock」原型，预留了清晰的扩展点：

| 方向 | 做法 |
|------|------|
| **接真实 SQLite 后端** | 在 `lib/` 增加 API 层（fetch），把 `data/mock.ts` 的直接引用改为请求；或整体迁移到 Next.js（API Routes + better-sqlite3） |
| **接真实 LLM（NL→SQL）** | 替换 `lib/nlsql.ts` 的 `matchQuestion` 为后端 AI 接口调用；保留「思考中」动效 |
| **图表增强** | 用 Recharts/ECharts 替换手写 SVG，获得 tooltip/动画/缩放 |
| **多数据库/多模式** | 扩展 `DBTREE` 结构与对象树渲染，支持多 schema、视图、索引节点 |
| **路由** | 引入 react-router，支持深链接与浏览器前进后退 |

---

## 许可

原型代码可自由使用。视觉设计 token 源自公开的 Supabase `DESIGN.md`，仅用于 AI 生成一致 UI 的演示目的。
