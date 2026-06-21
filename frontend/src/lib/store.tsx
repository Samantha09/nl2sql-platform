import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from 'react';
import { ViewKey, SchemaMode, TableMeta } from '../types';
import { schemaApi } from '../api';
import { fromTableDetail } from './adapt';
import { SCHEMA } from '../data/mock';
import { toast } from './toast';

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
  railOpen: boolean;
  toggleRail: () => void;
  // —— schema 数据（真实扫描，失败回退 mock）——
  /** 数据来源：real=后端真实扫描，mock=后端不可用时的本地兜底 */
  schemaSource: 'real' | 'mock';
  /** 当前数据源展示名 */
  dsName: string;
  /** 当前数据源的库名 */
  dbName: string;
  /** 表名列表 */
  tableNames: string[];
  /** 表名 → 结构详情 */
  details: Record<string, TableMeta>;
  /** schema 加载/扫描中 */
  schemaLoading: boolean;
  /** 触发后端真实重扫描并刷新 */
  rescan: () => void;
}

const Ctx = createContext<StoreCtx>(null as unknown as StoreCtx);

export function StoreProvider({ children }: { children: ReactNode }) {
  const [view, setView] = useState<ViewKey>('overview');
  const [curTable, setCurTable] = useState('orders');
  const [curMode, setCurMode] = useState<SchemaMode>('struct');
  const [railOpen, setRailOpen] = useState(true);
  const [tree, setTree] = useState<TreeState>({
    openDb: true,
    openSchema: true,
    openTables: { orders: true },
  });

  const [schemaSource, setSchemaSource] = useState<'real' | 'mock'>('real');
  const [dsId, setDsId] = useState<number | null>(null);
  const [dsName, setDsName] = useState('');
  const [dbName, setDbName] = useState('');
  const [tableNames, setTableNames] = useState<string[]>([]);
  const [details, setDetails] = useState<Record<string, TableMeta>>({});
  const [schemaLoading, setSchemaLoading] = useState(false);

  /** 拉取某数据源全部表的结构详情，组装成 name→TableMeta */
  const loadDetails = async (id: number, names: string[]) => {
    const entries = await Promise.all(
      names.map(async n => [n, fromTableDetail(await schemaApi.getTableDetail(id, n))] as const));
    return Object.fromEntries(entries) as Record<string, TableMeta>;
  };

  /** 回退到本地 mock，保证后端不可用时演示链路仍可用 */
  const fallbackToMock = useCallback(() => {
    setDsName('demo.db');
    setDbName('main');
    setTableNames(Object.keys(SCHEMA));
    setDetails(SCHEMA);
    setSchemaSource('mock');
    setCurTable(c => (SCHEMA[c] ? c : Object.keys(SCHEMA)[0]));
  }, []);

  /** 首屏加载：取第一个数据源，读其已扫描的表与结构 */
  const loadSchema = useCallback(async () => {
    setSchemaLoading(true);
    try {
      const dss = await schemaApi.listDataSources();
      if (!dss.length) throw new Error('无数据源');
      const ds = dss[0];
      setDsId(ds.id);
      setDsName(ds.name);
      setDbName(ds.databaseName);
      const names = await schemaApi.listTables(ds.id);
      setTableNames(names);
      setDetails(await loadDetails(ds.id, names));
      setSchemaSource('real');
      if (names.length) setCurTable(c => (names.includes(c) ? c : names[0]));
    } catch {
      fallbackToMock();
    } finally {
      setSchemaLoading(false);
    }
  }, [fallbackToMock]);

  useEffect(() => {
    loadSchema();
  }, [loadSchema]);

  /** 重新扫描当前数据源 */
  const rescan = useCallback(async () => {
    if (schemaSource === 'mock' || dsId == null) {
      toast('演示模式：未连接真实数据源');
      return;
    }
    setSchemaLoading(true);
    try {
      const names = await schemaApi.scanTables(dsId);
      setTableNames(names);
      setDetails(await loadDetails(dsId, names));
      if (names.length) setCurTable(c => (names.includes(c) ? c : names[0]));
      toast(`已扫描 ${names.length} 张表`);
    } catch (e) {
      toast('扫描失败：' + ((e as Error).message || '后端异常'));
    } finally {
      setSchemaLoading(false);
    }
  }, [schemaSource, dsId]);

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

  const toggleRail = () => setRailOpen(o => !o);

  return (
    <Ctx.Provider value={{
      view, go, curTable, curMode, selectTable, selectMode, tree, toggleNode, railOpen, toggleRail,
      schemaSource, dsName, dbName, tableNames, details, schemaLoading, rescan,
    }}>
      {children}
    </Ctx.Provider>
  );
}

export const useStore = () => useContext(Ctx);
