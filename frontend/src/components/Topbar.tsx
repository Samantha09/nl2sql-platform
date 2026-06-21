import { useStore } from '../lib/store';
import { ViewKey } from '../types';

const TITLES: Record<ViewKey, [string, string]> = {
  overview: ['概览', '数据库整体情况'],
  schema: ['表结构', '库表结构与外键关系'],
  query: ['智能查询', '自然语言转 SQL 并执行'],
  charts: ['数据可视化', '数据统计图表'],
};

export default function Topbar() {
  const { view, dsName, tableNames, schemaSource, schemaLoading, rescan } = useStore();
  const [title, sub] = TITLES[view];
  return (
    <div className="topbar">
      <div className="title">{title}<small>{sub}</small></div>
      <div className="meta">
        <span className="pill"><span className="dot" />{schemaSource === 'real' ? '已连接' : '演示模式'} · {dsName || '—'}</span>
        <span className="sep">|</span>
        <span>表 {tableNames.length}</span>
        <button className="iconbtn" disabled={schemaLoading} onClick={() => rescan()}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M3.5 9a9 9 0 0 1 14.8-3.4L23 10M1 14l4.7 4.4A9 9 0 0 0 20.5 15" /></svg>
          {schemaLoading ? '扫描中…' : '刷新'}
        </button>
      </div>
    </div>
  );
}
