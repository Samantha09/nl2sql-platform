import { useState } from 'react';
import { KB } from '../data/mock';
import { matchQuestion, highlightSql } from '../lib/nlsql';
import { fromKB, fromQueryResult, ResultVM } from '../lib/adapt';
import { useStore } from '../lib/store';
import { ChartType } from '../types';
import { queryApi, ApiError } from '../api';
import ChartRenderer from '../components/charts/ChartRenderer';

type Phase = 'idle' | 'thinking' | 'empty' | 'done';
const CHART_NAME: Record<ChartType, string> = { line: '折线图', bar: '条形图', donut: '饼图' };

export default function QueryView() {
  const { go } = useStore();
  const [input, setInput] = useState('');
  const [phase, setPhase] = useState<Phase>('idle');
  const [vm, setVm] = useState<ResultVM | null>(null);
  const [notice, setNotice] = useState('');

  const run = async (q?: string) => {
    const text = (q ?? input).trim();
    if (!text) { setPhase('idle'); return; }
    setInput(text);
    setPhase('thinking');
    setNotice('');

    try {
      const res = await queryApi.nlQuery({ naturalLanguage: text });
      setVm(fromQueryResult(res));
      setPhase('done');
    } catch (e) {
      // 后端不可用或返回异常：回退到本地 mock，保证演示链路可用
      const found = matchQuestion(text);
      if (found) {
        setVm(fromKB(found));
        setPhase('done');
        const reason = e instanceof ApiError ? e.message : '后端未响应';
        setNotice(`后端调用失败（${reason}），已回退本地示例数据`);
      } else {
        setVm(null);
        setPhase('empty');
      }
    }
  };

  const copySql = (sql: string, btn: HTMLButtonElement) => {
    navigator.clipboard?.writeText(sql);
    btn.textContent = '已复制';
    window.setTimeout(() => (btn.textContent = '复制'), 1500);
  };

  return (
    <>
      <div className="card" style={{ marginBottom: 16 }}>
        <h3>用自然语言提问 <span className="tag">实时调用后端 · 失败回退示例</span></h3>
        <div className="askbar">
          <input
            value={input}
            placeholder="例如：每个月的销售额趋势"
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') run(); }}
          />
          <button className="btn-primary" disabled={phase === 'thinking'} onClick={() => run()}>
            {phase === 'thinking' ? <><span className="spinner" /> 思考中</> : <>▶ 提问</>}
          </button>
        </div>
        <div className="chips">
          {KB.map((k, i) => <button key={i} className="chip-q" onClick={() => run(k.label)}>{k.label}</button>)}
        </div>
      </div>

      {phase === 'idle' && <Empty msg="输入问题或点击示例，AI 会生成 SQL、执行并推荐图表" />}
      {phase === 'thinking' && (
        <div className="step">
          <div className="step-head"><div className="ai-avatar">✦</div>AI 正在分析问题…</div>
          <div className="step-card" style={{ color: 'var(--ink-3)' }}>解析自然语言、匹配库表结构、生成 SQL…</div>
        </div>
      )}
      {phase === 'empty' && (
        <>
          <Empty msg="暂未识别该问题，请试试下方示例：" />
          <div className="chips">
            {KB.map((k, i) => <button key={i} className="chip-q" onClick={() => run(k.label)}>{k.label}</button>)}
          </div>
        </>
      )}
      {phase === 'done' && vm && (
        <>
          {notice && <div className="notice-fallback">{notice}</div>}
          {vm.tables.length > 0 && (
            <div className="step">
              <div className="step-head"><div className="ai-avatar">✦</div>AI 理解</div>
              <div className="step-card">
                <div className="intent-row">
                  意图：<b>{vm.intent}</b> · 命中表：
                  {vm.tables.map(t => (
                    <span key={t} style={{ display: 'inline-flex', alignItems: 'center', background: 'var(--bg-2)', border: '1px solid var(--line-2)', padding: '3px 9px', borderRadius: 6, fontFamily: 'var(--mono)', fontSize: '11.5px' }}>{t}</span>
                  ))}
                </div>
              </div>
            </div>
          )}
          <div className="step">
            <div className="step-head"><div className="ai-avatar" style={{ background: 'linear-gradient(135deg,#ffcc4d,#ff7a59)' }}>{'{ }'}</div>生成的 SQL</div>
            <div className="sql-block">
              <button className="copybtn" onClick={e => copySql(vm.sql, e.currentTarget)}>复制</button>
              <pre dangerouslySetInnerHTML={{ __html: highlightSql(vm.sql) }} />
            </div>
          </div>
          <div className="step">
            <div className="step-head">
              <div className="ai-avatar" style={{ background: 'linear-gradient(135deg,var(--emerald-deep),var(--emerald))' }}>▶</div>
              执行结果 <span style={{ marginLeft: 'auto', textTransform: 'none' }}>{vm.rows.length} 行</span>
            </div>
            <div className="step-card">
              <div className="result-head">
                <div className="left">建议图表：<b style={{ color: 'var(--emerald)' }}>{CHART_NAME[vm.chart]}</b></div>
                {vm.data.length > 0 && (
                  <button className="btn-primary" style={{ padding: '7px 14px', fontSize: '12.5px' }} onClick={() => go('charts')}>📊 在可视化查看</button>
                )}
              </div>
              <div className="result-grid">
                <table className="data" style={{ border: '1px solid var(--line)', borderRadius: 8, overflow: 'hidden' }}>
                  <thead><tr>{vm.cols.map(c => <th key={c}>{c}</th>)}</tr></thead>
                  <tbody>
                    {vm.rows.map((r, i) => (
                      <tr key={i}>{r.map((c, j) => <td key={j} className={j ? 'num' : ''}>{String(c)}</td>)}</tr>
                    ))}
                  </tbody>
                </table>
                {vm.data.length > 0 && (
                  <div className="chart-mini"><ChartRenderer type={vm.chart} data={vm.data} /></div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}

function Empty({ msg }: { msg: string }) {
  return (
    <div className="empty">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
      <div>{msg}</div>
    </div>
  );
}
