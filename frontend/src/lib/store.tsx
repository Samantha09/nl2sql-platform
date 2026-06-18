import { createContext, useContext, useState, ReactNode } from 'react';
import { ViewKey, SchemaMode } from '../types';

interface TreeState {
  openDb: boolean;
  openSchema: boolean;
  openTables: Record<string, boolean>;
}

interface StoreCtx {
  view: ViewKey;
  go: (v: ViewKey, table?: string) => void;
  curTable: string;
  curMode: SchemaMode;
  selectTable: (n: string) => void;
  selectMode: (n: string, m: SchemaMode) => void;
  tree: TreeState;
  toggleNode: (k: 'db' | 'schema') => void;
}

const Ctx = createContext<StoreCtx>(null as unknown as StoreCtx);

export function StoreProvider({ children }: { children: ReactNode }) {
  const [view, setView] = useState<ViewKey>('overview');
  const [curTable, setCurTable] = useState('orders');
  const [curMode, setCurMode] = useState<SchemaMode>('struct');
  const [tree, setTree] = useState<TreeState>({
    openDb: true,
    openSchema: true,
    openTables: { orders: true },
  });

  const go = (v: ViewKey, table?: string) => {
    setView(v);
    if (v === 'schema' && table) {
      setCurTable(table);
      setCurMode('struct');
      setTree(t => ({ ...t, openTables: { ...t.openTables, [table]: true } }));
    }
  };

  const selectTable = (n: string) => {
    setView('schema');
    if (n === curTable) {
      // 再次点击当前表：收起/展开子节点
      setTree(t => ({ ...t, openTables: { ...t.openTables, [n]: !t.openTables[n] } }));
    } else {
      setCurTable(n);
      setCurMode('struct');
      setTree(t => ({ ...t, openTables: { ...t.openTables, [n]: true } }));
    }
  };

  const selectMode = (n: string, m: SchemaMode) => {
    setCurTable(n);
    setCurMode(m);
    setTree(t => ({ ...t, openTables: { ...t.openTables, [n]: true } }));
    setView('schema');
  };

  const toggleNode = (k: 'db' | 'schema') =>
    setTree(t => (k === 'db' ? { ...t, openDb: !t.openDb } : { ...t, openSchema: !t.openSchema }));

  return (
    <Ctx.Provider value={{ view, go, curTable, curMode, selectTable, selectMode, tree, toggleNode }}>
      {children}
    </Ctx.Provider>
  );
}

export const useStore = () => useContext(Ctx);
