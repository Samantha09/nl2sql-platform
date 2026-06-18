# React 18 核心概念 · 学习文档

这组文档用 **ai-db-frontend 本项目的真实代码**讲解 React 18 的核心概念，每篇都包含：概念 → 最小示例 → 本项目真实示例（带文件路径）→ 实际使用场景 → 常见坑。

## 阅读顺序

1. [**01 · 组件与 JSX**](./01-组件与JSX.md) — React 的基本积木
2. [**02 · Props**](./02-Props.md) — 父子组件之间怎么传数据
3. [**03 · State 与 useState**](./03-State-useState.md) — 组件内部的可变状态
4. [**04 · Hooks 进阶**](./04-Hooks进阶.md) — useContext / useEffect / useId / 自定义 Hook

## 先建立心智模型

React 的一句话本质：

```
界面 = 组件(状态)        // UI 是状态的函数：状态变了，界面自动重算
       ↑          ↑
   可复用 UI 块   组件内部可变数据
```

你只管**改状态**，React 负责**重新渲染界面**。不需要手动操作 DOM。

## React 在本项目里的位置

| 你写的 | React 干的事 |
|--------|-------------|
| `src/views/QueryView.tsx`（一个函数返回 JSX） | React 把它渲染成界面上看到的「智能查询」页 |
| `const [input,setInput]=useState('')` | 你改 input → React 自动重渲染输入框 |
| `useStore()` 里的 `view` 变化 | React 自动切换显示哪个视图 |
| `<LineChart data={...}/>` | React 把 props 传给子组件并渲染图表 |

入口在 `src/main.tsx`：

```tsx
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <StoreProvider><App /></StoreProvider>
  </React.StrictMode>
);
```

## React 18 的关键新特性（本项目用到的）

- **`createRoot`**：React 18 的新渲染入口（替代旧版 `ReactDOM.render`），启用并发渲染能力。
- **`<StrictMode>`**：开发模式下会**故意多渲染一次**组件、帮助发现副作用问题——不是 bug，是特性。生产构建自动去掉。
- **自动批处理（Automatic Batching）**：多个 `setState` 调用会被合并成一次重渲染，性能更好。React 18 把它扩展到了异步代码（定时器、Promise）里。
- **并发渲染（Concurrent Rendering）**：React 可以中断/恢复渲染，保持界面响应。

---

下一篇：[01 · 组件与 JSX →](./01-组件与JSX.md)
