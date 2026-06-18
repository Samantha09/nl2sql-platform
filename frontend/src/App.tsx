import { useStore } from './lib/store';
import Rail from './components/Rail';
import ObjectTree from './components/ObjectTree';
import Topbar from './components/Topbar';
import Overview from './views/Overview';
import SchemaView from './views/SchemaView';
import QueryView from './views/QueryView';
import ChartsView from './views/ChartsView';

export default function App() {
  const { view } = useStore();
  return (
    <div className="app">
      <Rail />
      {view === 'schema' && <ObjectTree />}
      <main className="main">
        <Topbar />
        <section className="view">
          {view === 'overview' && <Overview />}
          {view === 'schema' && <SchemaView />}
          {view === 'query' && <QueryView />}
          {view === 'charts' && <ChartsView />}
        </section>
      </main>
    </div>
  );
}
