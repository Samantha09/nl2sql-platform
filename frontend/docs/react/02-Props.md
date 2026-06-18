# 02 · Props（属性）

## 什么是 Props

**Props 是父组件传给子组件的输入**，类似函数的参数。父组件在用子组件时通过"属性"传值：

```tsx
// 子组件：声明接收一个 name 属性
function Greeting({ name }: { name: string }) {
  return <h1>你好，{name}</h1>;
}

// 父组件：传入 name="李伟"
<Greeting name="李伟" />
// 渲染出：<h1>你好，李伟</h1>
```

**核心特点：props 是只读的、单向的（父 → 子）。** 子组件不能改自己的 props；要"改"，得让父组件改了再传新的进来（这就引出后面的 state）。

## 解构、类型、children

```tsx
// 1. 解构 + TypeScript 类型
function Button({ label, onClick, disabled = false }: {
  label: string;
  onClick: () => void;
  disabled?: boolean;          // ? 表示可选，给默认值
}) {
  return <button onClick={onClick} disabled={disabled}>{label}</button>;
}

// 2. children：标签之间的内容
function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      {children}             {/* ← 父组件放在标签之间的东西 */}
    </div>
  );
}

// 用法：<Card> 之间的内容会作为 children 传入
<Card title="概览">
  <KpiCards />        {/* 这部分就是 children */}
  <LineChart />
</Card>
```

## 本项目真实示例

### 例 1：图表组件接收数据 —— `LineChart`

`src/components/charts/LineChart.tsx`：

```tsx
export default function LineChart({ data }: { data: Series[] }) {
  //   ↑ 把 props 解构成 data                ↑ 类型：Series 数组
  const max = Math.max(...data.map(d => d.value)) * 1.12;
  return (
    <svg className="chart" viewBox={`0 0 ${520} ${220}`}>
      {/* 用 data 渲染折线点 */}
      {data.map((d, i) => (
        <circle key={'d' + i} cx={x(i)} cy={y(d.value)} r={3.5} fill={EMERALD} />
      ))}
    </svg>
  );
}
```

父组件 `Overview.tsx` 这样传：

```tsx
<LineChart data={DATA.revenue} />      {/* 把 DATA.revenue 作为 data 传进去 */}
```

同一个 `<LineChart/>` 组件，传不同的 `data` 就能画不同的折线——**这就是复用**。

### 例 2：`ChartRenderer` —— 用 props 决定渲染哪种图

`src/components/charts/ChartRenderer.tsx`：

```tsx
export default function ChartRenderer({ type, data }: { type: ChartType; data: Series[] }) {
  if (type === 'line')  return <LineChart data={data} />;
  if (type === 'donut') return <DonutChart data={data} />;
  return <BarChart data={data} />;
}
```

接收 `type` 和 `data` 两个 props，根据 `type` 分发到对应图表。

### 例 3：事件回调也是 props

`Overview.tsx` 里把"跳转"函数传给按钮：

```tsx
<button className="iconbtn" onClick={() => go('schema', t.name)}>查看 →</button>
```

`onClick` 就是一个 props——把父组件的函数传下去，子组件（button）触发时回调它。这是**子 → 父通信**的标准方式。

## 实际使用场景

| 场景 | props 的角色 |
|------|-------------|
| 通用组件复用 | `<LineChart data={...}/>` 传不同数据画不同图 |
| 配置组件行为 | `<ChartRenderer type="bar"/>` 传 type 切换样式 |
| 子组件通知父组件 | `onClick={handleClick}` 传回调函数 |
| 内容分发 | `<Card>{children}</Card>` 把任意内容塞进卡片 |
| 展示数据 | `<td>{row.name}</td>` 把数据渲染成界面 |

## 常见坑

1. **直接改 props**：`props.name = '新名'` ❌。props 只读，要变就用 state（下一篇）。
2. **数据流搞混**：父传子用 props；子要影响父，**不能直接改父的数据**，只能调用父传下来的回调。
3. **props 钻取（prop drilling）**：数据要从爷爷传到孙子的孙子，层层传很烦——这时该用 Context（见 [04 Hooks 进阶](./04-Hooks进阶.md)）。本项目正是用 `useStore()` 解决了 view/table 状态的全局共享。
4. **`children` 没声明就用**：要在参数里解构出 `children` 才能用。

---

← [01 组件与 JSX](./01-组件与JSX.md) ｜ 下一篇：[03 · State 与 useState →](./03-State-useState.md)
