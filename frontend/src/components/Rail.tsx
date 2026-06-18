import { ReactNode } from 'react';
import { useStore } from '../lib/store';
import { ViewKey } from '../types';

const ICONS: Record<ViewKey, ReactNode> = {
  overview: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" /><rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" /></svg>),
  schema: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M3 5v6c0 1.7 4 3 9 3s9-1.3 9-3V5" /><path d="M3 11v6c0 1.7 4 3 9 3s9-1.3 9-3v-6" /></svg>),
  query: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>),
  charts: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d="M3 3v18h18" /><path d="M7 14l3-3 3 3 5-6" /></svg>),
};
const TIPS: Record<ViewKey, string> = { overview: '概览', schema: '表结构', query: '智能查询', charts: '数据可视化' };
const ORDER: ViewKey[] = ['overview', 'schema', 'query', 'charts'];

export default function Rail() {
  const { view, go, railOpen, toggleRail } = useStore();
  return (
    <aside className={'rail' + (railOpen ? ' open' : '')}>
      <div className="rail-top">
        <div className="rail-logo">◆</div>
        {railOpen && <span className="rail-brand">AI 数据库助手</span>}
        <button className="rail-toggle" onClick={toggleRail} title={railOpen ? '收起菜单' : '展开菜单'}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
            {railOpen ? <path d="M15 18l-6-6 6-6" /> : <path d="M9 6l6 6-6 6" />}
          </svg>
        </button>
      </div>
      {ORDER.map(v => (
        <button key={v} className={'rail-item' + (view === v ? ' active' : '')} data-tip={TIPS[v]} onClick={() => go(v)}>
          {ICONS[v]}
          {railOpen && <span className="rail-label">{TIPS[v]}</span>}
        </button>
      ))}
      <div className="rail-spacer" />
      <div className="rail-foot">
        <span className="rail-dot" title="mock 模式" />
        {railOpen && <span className="rail-status">mock 模式</span>}
      </div>
    </aside>
  );
}
