import { useStore } from '../lib/store';

export default function SchemaView() {
  const { curTable, curMode, selectMode, details, schemaSource, schemaLoading } = useStore();
  const t = details[curTable];

  if (!t) {
    return (
      <div className="card">
        <h3>{schemaLoading ? '加载结构中…' : '未选择表'}</h3>
        <div style={{ color: 'var(--ink-3)', fontSize: '12.5px' }}>
          {schemaLoading ? '正在从后端读取扫描结果' : '请在左侧对象浏览器选择一张表，或点击右上角「刷新」扫描数据源'}
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>
        {curTable}
        <span className="tag">{t.cols.length} 字段 · 模式 {schemaSource === 'real' ? '真实扫描' : 'main'}</span>
        {t.comment ? <span className="tag" style={{ marginLeft: 8 }}>{t.comment}</span> : null}
      </h3>
      <div className="tabs">
        <button className={curMode === 'struct' ? 'on' : ''} onClick={() => selectMode(curTable, 'struct')}>字段</button>
        <button className={curMode === 'data' ? 'on' : ''} onClick={() => selectMode(curTable, 'data')}>数据</button>
      </div>

      {curMode === 'struct' ? (
        <>
          <table className="data">
            <thead><tr><th>列名</th><th>类型</th><th>注释</th><th>约束</th><th>键</th></tr></thead>
            <tbody>
              {t.cols.map(c => {
                const key = c.pk ? <span className="badge b-pk">PK</span> : c.fk ? <span className="badge b-fk">FK</span> : null;
                const nullable = <span className="badge b-nn">{c.nullable ? 'NULL' : 'NOT NULL'}</span>;
                const fkref = c.fk ? <span style={{ color: '#4f7cff', fontFamily: 'var(--mono)', fontSize: '11.5px' }}> → {c.fk}</span> : null;
                return (
                  <tr key={c.name}>
                    <td><b>{c.name}</b>{fkref}</td>
                    <td style={{ color: 'var(--ink-2)', fontFamily: 'var(--mono)' }}>{c.type}</td>
                    <td style={{ color: 'var(--ink-3)', fontSize: '12px' }}>{c.comment || ''}</td>
                    <td>{nullable}</td>
                    <td>{key}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          <div className="card" style={{ marginTop: 16, padding: 0, border: 'none', background: 'transparent' }}>
            <h3 style={{ padding: 0 }}>外键关系</h3>
            {t.rels.length
              ? <div className="rel">{t.rels.map(r => <span className="chip" key={r}>{r}</span>)}</div>
              : <div style={{ color: 'var(--ink-3)', fontSize: '12.5px' }}>该表无外键引用</div>}
          </div>

          {t.indexes && t.indexes.length > 0 && (
            <div className="card" style={{ marginTop: 16, padding: 0, border: 'none', background: 'transparent' }}>
              <h3 style={{ padding: 0 }}>索引</h3>
              <div className="rel">
                {t.indexes.map(idx => (
                  <span className="chip" key={idx.name}>
                    {idx.name}{idx.unique ? ' · 唯一' : ''} ({idx.columns.join(', ')})
                  </span>
                ))}
              </div>
            </div>
          )}
        </>
      ) : t.data.length ? (
        <>
          <div className="data-note">
            显示前 {t.data.length} 行 · 共 <b style={{ color: 'var(--ink-2)' }}>{t.rows.toLocaleString()}</b> 行
            <span className="pill" style={{ marginLeft: 'auto' }}><span className="dot" />只读预览</span>
          </div>
          <table className="data">
            <thead><tr>{t.cols.map(c => <th key={c.name}>{c.name}</th>)}</tr></thead>
            <tbody>
              {t.data.map((row, i) => (
                <tr key={i}>
                  {row.map((c, j) => <td key={j} className={j === 0 || typeof c === 'number' ? 'num' : ''}>{String(c)}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </>
      ) : (
        <div className="data-note" style={{ color: 'var(--ink-3)' }}>
          结构扫描暂不包含数据预览（仅采集表结构）。如需行数据采样，可后续在 schema-service 增加采样接口。
        </div>
      )}
    </div>
  );
}
