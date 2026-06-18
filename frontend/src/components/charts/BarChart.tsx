import { Series } from '../../types';
import { PALETTE } from '../../lib/theme';

export default function BarChart({ data }: { data: Series[] }) {
  const w = 520, h = 220, pad = { l: 46, r: 14, t: 10, b: 42 };
  const max = Math.max(...data.map(d => d.value)) * 1.1;
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const bw = (iw / data.length) * 0.55, gap = iw / data.length;
  const fmtAxis = (v: number) => (v >= 1000 ? Math.round(v / 1000) + 'k' : String(v));

  const grid = [];
  for (let g = 0; g <= 4; g++) {
    const yy = pad.t + (ih * g) / 4;
    const val = Math.round(max * (1 - g / 4));
    grid.push(
      <g key={g}>
        <line x1={pad.l} y1={yy} x2={w - pad.r} y2={yy} stroke="#2a2a2a" />
        <text className="axis" x={pad.l - 8} y={yy + 3} textAnchor="end">{fmtAxis(val)}</text>
      </g>
    );
  }

  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="xMidYMid meet">
      {grid}
      {data.map((d, i) => {
        const bh = (ih * d.value) / max;
        const bx = pad.l + gap * i + (gap - bw) / 2;
        const by = pad.t + ih - bh;
        const c = PALETTE[i % PALETTE.length];
        return (
          <g key={i}>
            <rect x={bx} y={by} width={bw} height={bh} rx={3} fill={c} />
            <text className="axis" x={bx + bw / 2} y={by - 5} textAnchor="middle" fill={c}>{d.value}</text>
            <text className="axis" x={bx + bw / 2} y={h - 22} textAnchor="middle">{String(d.label).slice(0, 5)}</text>
          </g>
        );
      })}
    </svg>
  );
}
