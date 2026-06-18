import { useId } from 'react';
import { Series } from '../../types';
import { EMERALD } from '../../lib/theme';

export default function LineChart({ data }: { data: Series[] }) {
  const uid = useId().replace(/[:]/g, '');
  const w = 520, h = 220, pad = { l: 46, r: 14, t: 14, b: 30 };
  const max = Math.max(...data.map(d => d.value)) * 1.12;
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const x = (i: number) => pad.l + (iw * i) / (data.length - 1);
  const y = (v: number) => pad.t + ih * (1 - v / max);
  const pts = data.map((d, i) => `${x(i)},${y(d.value)}`).join(' ');
  const gid = `lg-${uid}`;
  const fmtAxis = (v: number) => (v >= 1000 ? Math.round(v / 1000) + 'k' : String(v));
  const fmtDot = (v: number) => (v >= 1000 ? (v / 1000).toFixed(1) + 'k' : String(v));

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
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={EMERALD} />
          <stop offset="1" stopColor={EMERALD} stopOpacity="0" />
        </linearGradient>
      </defs>
      {grid}
      {data.map((d, i) => (
        <text key={'l' + i} className="axis" x={x(i)} y={h - 8} textAnchor="middle">{d.label.slice(5)}</text>
      ))}
      <polygon points={`${pad.l},${pad.t + ih} ${pts} ${x(data.length - 1)},${pad.t + ih}`} fill={`url(#${gid})`} opacity={0.25} />
      <polyline points={pts} fill="none" stroke={EMERALD} strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
      {data.map((d, i) => (
        <g key={'d' + i}>
          <circle cx={x(i)} cy={y(d.value)} r={3.5} fill={EMERALD} />
          <text className="axis" x={x(i)} y={y(d.value) - 9} textAnchor="middle" fill={EMERALD}>{fmtDot(d.value)}</text>
        </g>
      ))}
    </svg>
  );
}
