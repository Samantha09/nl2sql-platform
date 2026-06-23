import { Fragment, useState } from 'react';
import { useStore } from '../lib/store';
import DataSourceModal from './DataSourceModal';

const Chev = () => (<svg className="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.5}><path d="M9 6l6 6-6 6" /></svg>);

export default function ObjectTree() {
  const { view, curTable, curMode, curDatabase, tree, selectTable, selectMode, toggleNode, toggleDatabase,
    dsName, dbName, tableNames, details, schemaSource, schemaLoading,
    dataSources, dsId, selectDataSource, removeDataSource } = useStore();

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');

  const handleSelectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const id = Number(e.target.value);
    if (id && id !== dsId) {
      selectDataSource(id);
    }
  };

  const handleDelete = () => {
    if (dsId == null) return;
    const ds = dataSources.find(d => d.id === dsId);
    if (!window.confirm(`确定删除数据源「${ds?.name || dsId}」？`)) return;
    removeDataSource(dsId);
  };

  const openCreate = () => {
    setModalMode('create');
    setModalOpen(true);
  };

  const openEdit = () => {
    setModalMode('edit');
    setModalOpen(true);
  };

  const renderDatabase = (db: string) => {
    const names = tableNames[db] || [];
    const open = !!tree.openDatabases[db];
    return (
      <Fragment key={db}>
        <div className={'tnode' + (open ? ' open' : '')} onClick={() => toggleDatabase(db)}>
          <Chev />
          <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="#4f7cff" strokeWidth={2}><path d="M3 7h18v10H3z" /><path d="M3 7l3-3h12l3 3" /></svg>
          {db}<span className="cnt">{names.length}</span>
        </div>
        <div className={'tchildren' + (open ? ' open' : '')}>
          {!names.length && (
            <div className="leaf" style={{ color: 'var(--ink-3)', fontSize: '12px' }}>
              {schemaLoading ? '加载中…' : '该库暂无表'}
            </div>
          )}
          {names.map(name => {
            const active = name === curTable && db === curDatabase && view === 'schema';
            const openTable = !!tree.openTables[name];
            const meta = details[db]?.[name];
            const colCount = meta ? meta.cols.length : 0;
            return (
              <Fragment key={name}>
                <div className={'tnode' + (active ? ' active' : '') + (openTable ? ' open' : '')} onClick={() => { selectTable(name); }}>
                  <Chev />
                  <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M9 4v16" /></svg>
                  {name}<span className="cnt">{colCount || ''}</span>
                </div>
                <div className={'tchildren' + (openTable ? ' open' : '')}>
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
      </Fragment>
    );
  };

  return (
    <>
      <aside className="objtree">
        <div className="objtree-head">
          <select
            className="ds-select"
            value={dsId ?? ''}
            onChange={handleSelectChange}
            disabled={schemaLoading || dataSources.length === 0}
            title="切换数据源"
          >
            {dataSources.length === 0 && <option value="">无数据源</option>}
            {dataSources.map(ds => (
              <option key={ds.id} value={ds.id}>{ds.name}</option>
            ))}
          </select>
          <div className="ds-actions">
            <button className="add" onClick={openCreate} title="新增数据源">＋</button>
            <button className="edit" onClick={openEdit} disabled={dsId == null} title="编辑当前数据源">✎</button>
            <button className="del" onClick={handleDelete} disabled={dsId == null} title="删除当前数据源">×</button>
          </div>
        </div>
        <div className="tree">
          <div className={'tnode' + (tree.openDb ? ' open' : '')} onClick={() => toggleNode('db')}>
            <Chev />
            <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="var(--emerald)" strokeWidth={2}><ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M3 5v14c0 1.7 4 3 9 3s9-1.3 9-3V5" /></svg>
            <b style={{ color: 'var(--ink)' }}>{dsName || '加载中…'}</b><span className="cnt">database</span>
          </div>
          <div className={'tchildren' + (tree.openDb ? ' open' : '')}>
            <div className={'tnode' + (tree.openSchema ? ' open' : '')} onClick={() => toggleNode('schema')}>
              <Chev />
              <svg className="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><path d="M4 6h16M4 12h16M4 18h16" /></svg>
              schemas<span className="cnt">{Object.keys(tableNames).length}</span>
            </div>
            <div className={'tchildren' + (tree.openSchema ? ' open' : '')}>
              {!Object.keys(tableNames).length && (
                <div className="leaf" style={{ color: 'var(--ink-3)', fontSize: '12px' }}>
                  {schemaLoading ? '加载中…' : '暂无库/表，点上方刷新扫描'}
                </div>
              )}
              {Object.keys(tableNames).map(db => renderDatabase(db))}
            </div>
          </div>
        </div>
        <div className="objtree-foot">
          <div className="row"><span>连接</span><span className="pill"><span className="dot" />{schemaSource === 'real' ? '已连接' : '演示模式'}</span></div>
          <div className="row"><span>数据库</span><span>{dbName || dsName || '—'}</span></div>
          <div className="row"><span>表 / 模式</span><span>{Object.values(tableNames).flat().length} / {Object.keys(tableNames).length}</span></div>
        </div>
      </aside>
      <DataSourceModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        mode={modalMode}
        initialData={modalMode === 'edit' ? dataSources.find(d => d.id === dsId) || null : null}
      />
    </>
  );
}
