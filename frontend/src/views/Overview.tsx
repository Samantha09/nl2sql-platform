import { useStore } from '../lib/store';
import { DATA } from '../data/mock';
import LineChart from '../components/charts/LineChart';
import DonutChart from '../components/charts/DonutChart';

const KPIS = [
  { label: '数据表', value: '3', sub: 'customers · products · orders' },
  { label: '总记录数', value: '1,240', sub: '较上周 +42' },
  { label: '本月订单', value: '342', sub: '较上月 +12.4%' },
  { label: '本月销售额', value: '¥84,320', sub: '较上月 +8.1%' },
];
const TABLES = [
  { name: 'customers', rows: 200, cols: 'id, name, email, city' },
  { name: 'products', rows: 40, cols: 'id, name, category, price' },
  { name: 'orders', rows: 1000, cols: 'id, customer_id, product_id, amount' },
];

export default function Overview() {
  const { go } = useStore();
  return (
    <>
      <div className="kpi-grid">
        {KPIS.map(k => (
          <div className="kpi" key={k.label}>
            <div className="label">{k.label}</div>
            <div className="value">{k.value}</div>
            <div className="sub">{k.sub}</div>
          </div>
        ))}
      </div>
      <div className="grid-2">
        <div className="card">
          <h3>近 6 个月销售额趋势 <span className="tag">来源：orders</span></h3>
          <LineChart data={DATA.revenue} />
        </div>
        <div className="card">
          <h3>订单状态分布 <span className="tag">来源：orders</span></h3>
          <DonutChart data={DATA.status} />
        </div>
      </div>
      <div className="card grid-1">
        <h3>表清单 <span className="tag">点击查看结构</span></h3>
        <table className="data">
          <thead><tr><th>表名</th><th>记录数</th><th>主要列</th><th></th></tr></thead>
          <tbody>
            {TABLES.map(t => (
              <tr key={t.name}>
                <td><b>{t.name}</b></td>
                <td className="num">{t.rows}</td>
                <td>{t.cols}</td>
                <td className="right"><button className="iconbtn" onClick={() => go('schema', t.name)}>查看 →</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
