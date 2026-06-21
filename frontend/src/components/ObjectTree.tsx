import { Fragment } from 'react';
import { useStore } from '../lib/store';

const Chev = () => (<svg className="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d="M9 6l6 6-6 6" /></svg>);

export default function ObjectTree() {
  const { view, curTable, curMode, tree, selectTable, selectMode, toggleNode,
    dsName, dbName, tableNames, details, schemaSource, schemaLoading } = useStore();
  return (
    <aside className="objtree">
      <div className="objtree-head"><span>对象浏览器</span><span className="add">＋</span></div>
      <div className="tree">
        <div className={'tnode' + (tree.openDb ? ' open' : '')} onClick={() => toggleNode('db')}>
          <Chev />
          <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="var(--emerald)" strokeWidth={2}><ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5" /></svg>
          <b style={{ color: 'var(--ink)' }}>{dsName || '加载中…'}</b><span className="cnt">database</span>
        </div>
        <div className={'tchildren' + (tree.openDb ? ' open' : '')}>
          <div className={'tnode' + (tree.openSchema ? ' open' : '')} onClick={() => toggleNode('schema')}>
            <Chev />
            <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="#4f7cff" strokeWidth={2}><path d="M3 7h18v10H3z" /><path d="M3 7l3-3h12l3 3" /></svg>
            {dbName || 'main'}<span className="cnt">schema</span>
          </div>
          <div className={'tchildren' + (tree.openSchema ? ' open' : '')}>
            {!tableNames.length && (
              <div className="leaf" style={{ color: 'var(--ink-3)', fontSize: '12px' }}>
                {schemaLoading ? '加载中…' : '暂无表，点上方刷新扫描'}
              </div>
            )}
            {tableNames.map(name => {
              const active = name === curTable && view === 'schema';
              const open = !!tree.openTables[name];
              const meta = details[name];
              const colCount = meta ? meta.cols.length : 0;
              return (
                <Fragment key={name}>
                  <div className={'tnode' + (active ? ' active' : '') + (open ? ' open' : '')} onClick={() => selectTable(name)}>
                    <Chev />
                    <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M9 4v16" /></svg>
                    {name}<span className="cnt">{colCount || ''}</span>
                  </div>
                  <div className={'tchildren' + (open ? ' open' : '')}>
                    <div className={'leaf' + (active && curMode === 'struct' ? ' active' : '')} onClick={e => { e.stopPropagation(); selectMode(name, 'struct'); }}>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d="M4 6h16M4 12h16M4 18h16" /></svg>
                      字段 ({colCount})
                    </div>
                    <div className={'leaf' + (active && curMode === 'data' ? ' active' : '')} onClick={e => { e.stopPropagation(); selectMode(name, 'data'); }}>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><ellipse cx="12" cy="5" rx="8" ry="3" /><path d="M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5" /></svg>
                      数据
                    </div>
                  </div>
                </Fragment>
              );
            })}
          </div>
        </div>
      </div>
      <div className="objtree-foot">
        <div className="row"><span>连接</span><span className="pill"><span className="dot" />{schemaSource === 'real' ? '已连接' : '演示模式'}</span></div>
        <div className="row"><span>数据库</span><span>{dbName || dsName || '—'}</span></div>
        <div className="row"><span>表 / 模式</span><span>{tableNames.length} / {dbName || 'main'}</span></div>
      </div>
    </aside>
  );
}
