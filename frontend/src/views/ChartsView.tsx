import { useState } from 'react';
import { Series, ChartType } from '../types';
import { DATA } from '../data/mock';
import ChartRenderer from '../components/charts/ChartRenderer';

interface CardDef {
  title: string;
  tag: string;
  type: ChartType;
  data: Series[];
  opts: ChartType[];
}

const DEFS: CardDef[] = [
  { title: '月度销售额趋势', tag: 'orders · 折线', type: 'line', data: DATA.revenue, opts: ['line', 'bar'] },
  { title: '各品类订单占比', tag: 'orders×products · 饼图', type: 'donut', data: DATA.category, opts: ['donut', 'bar'] },
  { title: '销量 Top 10 商品', tag: 'orders×products · 条形', type: 'bar', data: DATA.topProducts, opts: ['bar', 'donut'] },
  { title: '各城市客户数', tag: 'customers · 条形', type: 'bar', data: DATA.city, opts: ['bar', 'donut'] },
];

const NAMES: Record<ChartType, string> = { line: '折线', bar: '条形', donut: '饼' };

function ChartCard({ def }: { def: CardDef }) {
  const [type, setType] = useState<ChartType>(def.type);
  return (
    <div className="card chartcard">
      <h3>
        {def.title}<span className="tag">{def.tag}</span>
        <span className="switch">
          {def.opts.map(o => (
            <button key={o} className={o === type ? 'on' : ''} onClick={() => setType(o)}>{NAMES[o]}</button>
          ))}
        </span>
      </h3>
      <ChartRenderer type={type} data={def.data} />
    </div>
  );
}

export default function ChartsView() {
  return <div className="charts-grid">{DEFS.map(d => <ChartCard key={d.title} def={d} />)}</div>;
}
