import { useStore } from '../lib/store';
import { SCHEMA } from '../data/mock';

export default function SchemaView() {
  const { curTable, curMode, selectMode } = useStore();
  const t = SCHEMA[curTable];

  return (
    <div className="card">
      <h3>{curTable} <span className="tag">{t.rows.toLocaleString()} 行 · 模式 main</span></h3>
      <div className="tabs">
        <button className={curMode === 'struct' ? 'on' : ''} onClick={() => selectMode(curTable, 'struct')}>字段</button>
        <button className={curMode === 'data' ? 'on' : ''} onClick={() => selectMode(curTable, 'data')}>数据</button>
      </div>

      {curMode === 'struct' ? (
        <>
          <table className="data">
            <thead><tr><th>列名</th><th>类型</th><th>约束</th><th>键</th></tr></thead>
            <tbody>
              {t.cols.map(c => {
                const key = c.pk ? <span className="badge b-pk">PK</span> : c.fk ? <span className="badge b-fk">FK</span> : null;
                const nullable = <span className="badge b-nn">{c.nullable ? 'NULL' : 'NOT NULL'}</span>;
                const fkref = c.fk ? <span style={{ color: '#4f7cff', fontFamily: 'var(--mono)', fontSize: '11.5px' }}> → {c.fk}</span> : null;
                return (
                  <tr key={c.name}>
                    <td><b>{c.name}</b>{fkref}</td>
                    <td style={{ color: 'var(--ink-2)', fontFamily: 'var(--mono)' }}>{c.type}</td>
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
        </>
      ) : (
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
      )}
    </div>
  );
}
