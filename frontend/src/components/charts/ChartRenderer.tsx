import { ChartType, Series } from '../../types';
import LineChart from './LineChart';
import BarChart from './BarChart';
import DonutChart from './DonutChart';

export default function ChartRenderer({ type, data }: { type: ChartType; data: Series[] }) {
  if (type === 'line') return <LineChart data={data} />;
  if (type === 'donut') return <DonutChart data={data} />;
  return <BarChart data={data} />;
}
