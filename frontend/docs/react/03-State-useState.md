# 03 · State 与 useState

## 什么是 State

**State 是组件内部的可变数据。** 和 props 的区别：

| | props | state |
|--|-------|-------|
| 谁拥有 | 父组件 | 组件自己 |
| 可变吗 | 只读 | 可改 |
| 改了会 | 父重传 | **组件自动重新渲染** |

**state 一变，React 就自动重算界面**——这是 React 响应式的核心。

## useState 的用法

```tsx
import { useState } from 'react';

function Counter() {
  //       ↓ 当前值    ↓ 修改函数     ↓ 初始值
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>点击了 {count} 次</p>
      <button onClick={() => setCount(count + 1)}>+1</button>
    </div>
  );
}
```

- `useState(0)` 返回一个**二元组**：当前值 + 一个 setter 函数。
- 调 `setCount(新值)` → React 把新值存起来 → 重新渲染这个组件 → 界面更新。

## 不可变更新（最重要的一条规则）

**永远不要直接改 state 变量**，要用 setter 传一个**新值/新对象**：

```tsx
// ❌ 错：直接改（React 察觉不到变化，界面不更新）
const [user, setUser] = useState({ name: '李伟', age: 30 });
user.age = 31;

// ✅ 对：传一个全新对象
setUser({ ...user, age: 31 });     // 展开旧对象，覆盖要改的字段

// 数组同理
const [list, setList] = useState([1, 2, 3]);
// ❌ list.push(4)
// ✅
setList([...list, 4]);             // 加一项
setList(list.filter(x => x !== 2));// 删一项
```

原因：React 用"引用是否变化"判断要不要重渲染。直接改原对象，引用没变，React 以为没动。

## 本项目真实示例

### 例 1：`QueryView.tsx` —— 一个组件用三个 state

智能查询页 `src/views/QueryView.tsx` 用 state 管理输入框、查询阶段、命中结果：

```tsx
export default function QueryView() {
  const [input, setInput] = useState('');                // 输入框文字
  const [phase, setPhase] = useState<Phase>('idle');     // idle/thinking/empty/done
  const [hit, setHit] = useState<KBItem | null>(null);   // 命中的知识库条目

  const run = (q?: string) => {
    const text = (q ?? input).trim();
    if (!text) { setPhase('idle'); return; }
    setInput(text);
    setPhase('thinking');                                 // ① 进入"思考中"
    const found = matchQuestion(text);
    window.setTimeout(() => {
      if (found) { setHit(found); setPhase('done'); }     // ② 思考完 → done
      else { setHit(null); setPhase('empty'); }           // ③ 没命中 → empty
    }, 650);
  };

  return (
    <>
      <input value={input} onChange={e => setInput(e.target.value)} />
      <button disabled={phase === 'thinking'} onClick={() => run()}>
        {phase === 'thinking' ? '思考中' : '提问'}
      </button>
      {phase === 'done' && hit && <ResultCard hit={hit} />}
    </>
  );
}
```

可以看到一个清晰的**状态机**：`idle → thinking → done/empty`，每个 state 对应不同的界面。改 `phase` 后，界面自动切换显示什么。

### 例 2：`ChartsView.tsx` —— 每张图卡独立的 state

```tsx
function ChartCard({ def }: { def: CardDef }) {
  const [type, setType] = useState<ChartType>(def.type);  // 当前图表类型
  return (
    <div className="card chartcard">
      <span className="switch">
        {def.opts.map(o => (
          <button key={o} className={o === type ? 'on' : ''}
                  onClick={() => setType(o)}>{NAMES[o]}</button>
        ))}
      </span>
      <ChartRenderer type={type} data={def.data} />
    </div>
  );
}
```

点"折线/条形/饼"按钮 → `setType(o)` → 卡片重渲染 → 图表切换。**每张卡片有自己的 type state，互不干扰**（因为 state 定义在组件内部，组件每渲染一次是独立实例）。

## 实际使用场景

| 场景 | state 示例 |
|------|-----------|
| 表单输入 | `const [value, setValue] = useState('')` |
| 开关/展开 | `const [open, setOpen] = useState(false)` |
| 加载状态 | `const [loading, setLoading] = useState(false)` |
| 当前选中 | `const [active, setActive] = useState('orders')` |
| 数据结果 | `const [data, setData] = useState(null)` |

## 常见坑

### 1. 直接改 state（界面不更新）
见上面"不可变更新"。

### 2. 用旧值算新值（闭包陷阱）
```tsx
// ❌ 连点 3 次只 +1：每次 onClick 看到的都是旧的 count
<button onClick={() => setCount(count + 1)} />

// ✅ 用函数式更新，React 保证拿到最新值
<button onClick={() => setCount(c => c + 1)} />
```
当新值依赖旧值时，**传函数** `setX(prev => newValue)`。

### 3. 多次 setState，React 18 自动批处理
```tsx
const run = () => {
  setPhase('thinking');   // 这三句不会触发三次渲染
  setHit(null);
  setInput(text);          // React 18 会合并成一次重渲染 ✅
};
```
React 18 的**自动批处理**让这种情况性能更好（旧版本只在事件处理里批处理，18 扩展到了定时器/Promise）。

### 4. State 放错层级
如果多个兄弟组件要共享同一个状态，不要各自 `useState`，应该**提升到共同父组件**，或用 Context（下一篇）。本项目的 `view`/`curTable` 就是因为 Rail、ObjectTree、Topbar 都要用，所以提升到了全局 `store`。

---

← [02 Props](./02-Props.md) ｜ 下一篇：[04 · Hooks 进阶 →](./04-Hooks进阶.md)
