import { Series } from '../../types';
import { PALETTE } from '../../lib/theme';

function polar(cx: number, cy: number, r: number, deg: number): [number, number] {
  const a = (deg * Math.PI) / 180;
  return [cx + r * Math.sin(a), cy - r * Math.cos(a)];
}

function slice(cx: number, cy: number, ro: number, ri: number, a0: number, a1: number) {
  const [x1, y1] = polar(cx, cy, ro, a1);
  const [x2, y2] = polar(cx, cy, ro, a0);
  const [x3, y3] = polar(cx, cy, ri, a0);
  const [x4, y4] = polar(cx, cy, ri, a1);
  const large = a1 - a0 <= 180 ? 0 : 1;
  return `M ${x1} ${y1} A ${ro} ${ro} 0 ${large} 0 ${x2} ${y2} L ${x3} ${y3} A ${ri} ${ri} 0 ${large} 1 ${x4} ${y4} Z`;
}

export default function DonutChart({ data }: { data: Series[] }) {
  const cx = 100, cy = 110, ro = 80, ri = 50;
  const total = data.reduce((s, d) => s + d.value, 0);
  let acc = 0;
  const slices = data.map((d, i) => {
    const a0 = (acc / total) * 360;
    const a1 = ((acc + d.value) / total) * 360;
    acc = a1;
    return { path: slice(cx, cy, ro, ri, a0, a1), c: PALETTE[i % PALETTE.length], label: d.label, pct: Math.round((d.value / total) * 100), key: i };
  });

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <svg width={200} height={220} viewBox="0 0 200 220">
        <text x={100} y={105} textAnchor="middle" fill="var(--ink-3)" fontSize={11}>合计</text>
        <text x={100} y={128} textAnchor="middle" fill="var(--emerald)" fontSize={20} fontWeight={600}>{total}</text>
        {slices.map(s => <path key={s.key} d={s.path} fill={s.c} />)}
      </svg>
      <div className="legend" style={{ flexDirection: 'column', gap: 9, margin: 0 }}>
        {slices.map(s => (
          <span key={s.key}><i style={{ background: s.c }} />{s.label} <b style={{ color: 'var(--ink)' }}>{s.pct}%</b></span>
        ))}
      </div>
    </div>
  );
}
