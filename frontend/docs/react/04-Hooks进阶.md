# 04 · Hooks 进阶

**Hook 是以 `use` 开头的特殊函数**，让你在函数组件里"挂载"React 能力（状态、副作用、上下文……）。前面 `useState` 就是最常用的 Hook。本篇讲其余几个核心 Hook。

> 📌 **Hooks 两条铁律**：
> 1. 只能在**组件顶层**或**自定义 Hook 里**调用，不能放进 `if`/循环/嵌套函数里。
> 2. 必须按顺序调用——React 靠调用顺序对应每次渲染的 state。

---

## 1. useContext —— 全局状态共享（本项目核心）

### 解决的问题：prop drilling

如果 `view`（当前视图）只放在 `App` 里，但 `Rail`、`ObjectTree`、`Topbar` 都要用，又不能改 props——层层传下去很烦。Context 让你**一处分发，任意组件读取**。

### 用法：创建 → 提供 → 消费

```tsx
import { createContext, useContext, useState } from 'react';

// ① 创建 Context
const Ctx = createContext<类型>(默认值);

// ② Provider：在上层提供值
function StoreProvider({ children }) {
  const [view, setView] = useState('overview');
  return <Ctx.Provider value={{ view, setView }}>{children}</Ctx.Provider>;
}

// ③ 消费：任意深层子组件读取
function SomeDeepChild() {
  const { view, setView } = useContext(Ctx);   // 直接拿到，不用 props
}
```

### 本项目真实示例：`src/lib/store.tsx`

本项目正是用 Context 管理全局状态（视图、当前表、对象树展开）。`store.tsx` 提供：

```tsx
const Ctx = createContext<StoreCtx>(null as unknown as StoreCtx);

export function StoreProvider({ children }) {
  const [view, setView] = useState<ViewKey>('overview');
  const [curTable, setCurTable] = useState('orders');
  // ...其他 state 和操作
  return <Ctx.Provider value={{ view, go, curTable, selectTable, ... }}>{children}</Ctx.Provider>;
}

export const useStore = () => useContext(Ctx);   // 封装成 useStore() 方便用
```

消费方（`Rail.tsx`）只需一行：

```tsx
const { view, go } = useStore();   // 直接拿全局的 view 和 go 函数
```

`main.tsx` 在最外层包了 `<StoreProvider>`，所以整个应用任何组件都能 `useStore()`。

**适用场景**：主题、当前用户、全局视图状态、i18n 语言——"全应用共享、低频变化"的数据。

⚠️ Context 值变化时，所有消费它的组件都会重渲染，**别塞高频变化的大数据**（那种用状态管理库更合适）。

---

## 2. useEffect —— 副作用（本项目暂未用，接后端时必用）

### 什么是副作用

"渲染"应该是纯的（同样的 props/state → 同样的界面）。但有时你需要做"渲染之外"的事：发请求、订阅、操作 DOM、设定时器——这些叫**副作用**，放进 `useEffect`。

### 用法

```tsx
useEffect(() => {
  // 这里执行副作用
  return () => {
    // 可选：清理函数（组件卸载或下次 effect 前执行）
  };
}, [依赖数组]);   // 依赖变化时才重新执行
```

依赖数组的含义：
| 写法 | 行为 |
|------|------|
| `useEffect(fn, [])` | **只在挂载时执行一次** |
| `useEffect(fn, [a, b])` | `a` 或 `b` 变化时执行 |
| `useEffect(fn)`（不传） | 每次渲染都执行（少用） |

### 接后端时的真实写法（示例）

本项目现在全 mock，**没有 `useEffect`**。等接真实 SQLite 后端，"进入表结构页就加载字段"会这样写：

```tsx
function SchemaView({ tableName }: { tableName: string }) {
  const [cols, setCols] = useState<Column[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetch(`/api/schema/${tableName}`)
      .then(r => r.json())
      .then(data => { if (!cancelled) { setCols(data.cols); setLoading(false); } });
    return () => { cancelled = true; };   // 清理：避免组件卸载后还 setState
  }, [tableName]);                          // tableName 变了就重新加载

  if (loading) return <Spinner />;
  return <Table cols={cols} />;
}
```

**适用场景**：数据请求、事件订阅、定时器、操作 DOM、写入 localStorage。

⚠️ 常见坑：
- **忘写依赖数组** → 每次渲染都跑，可能死循环（请求→setState→重渲染→再请求）。
- **依赖数组漏项** → 用到闭包里的旧值。
- **清理函数漏写** → 卸载后还在 setState → 报警告 / 内存泄漏。

---

## 3. useId —— 生成唯一 ID（本项目用了）

某些场景需要唯一 id（比如 SVG 的 `<defs>` 引用、`<label htmlFor>`）。`useId` 生成稳定的唯一 id，**服务端/客户端一致**（避免水合不匹配）。

### 本项目真实示例：`src/components/charts/LineChart.tsx`

折线图用渐变填充，渐变靠 `id` 引用。如果页面上有两个折线图，id 重复会冲突，所以用 `useId` 保证唯一：

```tsx
import { useId } from 'react';

export default function LineChart({ data }: { data: Series[] }) {
  const uid = useId().replace(/[:]/g, '');   // 生成唯一 id（去掉冒号，SVG id 不能有冒号）
  const gid = `lg-${uid}`;
  return (
    <svg className="chart" viewBox="0 0 520 220">
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={EMERALD} />
          <stop offset="1" stopColor={EMERALD} stopOpacity="0" />
        </linearGradient>
      </defs>
      {/* 用 url(#gid) 引用这个渐变 */}
      <polygon points={...} fill={`url(#${gid})`} opacity={0.25} />
    </svg>
  );
}
```

---

## 4. 自定义 Hook —— 复用逻辑

当你发现同一段状态逻辑在多个组件重复，就把它抽成**自定义 Hook**（一个 `use` 开头的函数）。

### 示例：把 QueryView 的查询逻辑抽出来

`QueryView` 里的 `input`/`phase`/`hit` + `run` 逻辑，可以抽成一个 `useQuestion` Hook：

```tsx
// src/lib/useQuestion.ts
import { useState } from 'react';
import { matchQuestion } from './nlsql';
import { KBItem } from '../types';

type Phase = 'idle' | 'thinking' | 'empty' | 'done';

export function useQuestion() {
  const [input, setInput] = useState('');
  const [phase, setPhase] = useState<Phase>('idle');
  const [hit, setHit] = useState<KBItem | null>(null);

  const run = (q?: string) => {
    const text = (q ?? input).trim();
    if (!text) { setPhase('idle'); return; }
    setInput(text);
    setPhase('thinking');
    const found = matchQuestion(text);
    window.setTimeout(() => {
      if (found) { setHit(found); setPhase('done'); }
      else { setHit(null); setPhase('empty'); }
    }, 650);
  };

  return { input, setInput, phase, hit, run };   // 把状态和操作一起返回
}
```

组件里就变得很干净：

```tsx
function QueryView() {
  const { input, setInput, phase, hit, run } = useQuestion();   // 一行搞定
  return <>...</>;
}
```

**自定义 Hook 的价值**：逻辑复用，且不增加组件嵌套（比高阶组件/render props 简洁）。React 内置的 Hook 本身就是这样实现的。

---

## Hook 速查表

| Hook | 作用 | 本项目 |
|------|------|--------|
| `useState` | 组件内部状态 | ✅ QueryView、ChartsView |
| `useContext` | 读取全局 Context | ✅ store.tsx + 各组件 useStore() |
| `useId` | 生成唯一 id | ✅ LineChart 渐变 id |
| `useEffect` | 副作用（请求/订阅/定时器） | ⏳ 暂未用，接后端时用 |
| `useRef` | 存可变值不触发渲染 / 引用 DOM | — |
| `useMemo` | 缓存计算结果 | — |
| `useCallback` | 缓存函数 | — |

> 经验：`useMemo`/`useCallback` 不要无脑加，多数情况是过早优化。先写清楚，遇到性能问题再上。

---

← [03 State 与 useState](./03-State-useState.md) ｜ [回到导读](./README.md)
