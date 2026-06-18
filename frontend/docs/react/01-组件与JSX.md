# 01 · 组件与 JSX

## 什么是组件

**组件 = 一个返回界面（JSX）的函数。** 它是 React 的基本积木：把界面拆成一个个可复用的小块。

```tsx
// 最简单的组件：一个函数，返回一段 JSX
function Welcome() {
  return <h1>你好，AI 数据库助手</h1>;
}
```

用的时候像写 HTML 标签一样：

```tsx
<Welcome />
```

## JSX：在 JS 里写"类 HTML"

JSX 是 JavaScript 的语法扩展，让你在 JS 里直接写界面结构。它会被编译成普通的 JS 函数调用。

```tsx
function Card() {
  const title = '订单状态分布';
  return (
    <div className="card">           {/* class 要写成 className */}
      <h3>{title}</h3>               {/* 用 {} 嵌入 JS 变量 */}
      <p>来源：orders</p>
    </div>
  );
}
```

### JSX 五条规则

| 规则 | 说明 | 示例 |
|------|------|------|
| 1. `class` → `className` | 因为 `class` 是 JS 保留字 | `<div className="card">` |
| 2. `{}` 嵌入任意 JS | 变量、表达式、三元、map | `<h3>{item.name}</h3>` |
| 3. 标签必须闭合 | 包括自闭合 | `<input />`、`<img />` |
| 4. 只能有一个根元素 | 多个元素要用 `<>...</>` 包裹 | `<> <h1/> <p/> </>` |
| 5. 注释写 `{/* */}` | 不是 HTML 的 `<!-- -->` | `{/* 这是注释 */}` |

## 本项目真实示例

### 例 1：`Rail.tsx` —— 一个完整的函数组件

`src/components/Rail.tsx` 里的左侧导航栏就是一个组件：

```tsx
export default function Rail() {
  const { view, go } = useStore();              // 取全局状态
  return (
    <aside className="rail">
      <div className="rail-logo">◆</div>
      {ORDER.map(v => (                          // {} 里用 map 渲染列表
        <button key={v} className={'rail-item' + (view === v ? ' active' : '')}
                data-tip={TIPS[v]} onClick={() => go(v)}>
          {ICONS[v]}
        </button>
      ))}
    </aside>
  );
}
```

注意几个点：
- `view === v ? ' active' : ''` —— JSX 里用三元表达式动态拼 className。
- `{ORDER.map(...)}` —— 用 JS 的 `map` 把数组渲染成一组按钮。
- 每个 button 有 `key` —— 列表渲染时 React 要求唯一 key。

### 例 2：`App.tsx` —— 组件的组合

组件可以像搭积木一样嵌套组合：

```tsx
export default function App() {
  const { view } = useStore();
  return (
    <div className="app">
      <Rail />              {/* ← 子组件 */}
      <ObjectTree />        {/* ← 子组件 */}
      <main className="main">
        <Topbar />
        <section className="view">
          {view === 'overview' && <Overview />}
          {view === 'schema'   && <SchemaView />}
          {view === 'query'    && <QueryView />}
          {view === 'charts'   && <ChartsView />}
        </section>
      </main>
    </div>
  );
}
```

`App` 不自己画界面，而是**组合**了一堆子组件。`view === 'overview' && <Overview/>` 是"条件渲染"的惯用法。

## 实际使用场景

- **复用**：写一次 `<DonutChart/>`，概览页、查询结果页、可视化页都能用。
- **拆分大文件**：一个 500 行的组件看不懂？按区域拆成 `<Sidebar/>`、`<Main/>`、`<Chart/>`。
- **列表渲染**：对象树里的表、KPI 卡片、示例问题 chips，都是 `array.map(项 => <组件/>)`。

## 常见坑

1. **忘记 `return`**：组件函数必须 `return` JSX，否则渲染 `undefined`。
2. **多个根元素**：`return <div/><div/>` ❌，要用 `<>` 包裹或用一个父 `<div>`。
3. **`map` 忘写 `key`**：React 会报警告，列表更新可能出 bug。
4. **把对象直接当子节点**：`{someObject}` ❌，React 不知道怎么显示对象；要先取字段 `{someObject.name}`。

---

← [导读](./README.md) ｜ 下一篇：[02 · Props →](./02-Props.md)
