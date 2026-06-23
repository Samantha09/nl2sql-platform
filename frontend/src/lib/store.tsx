import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from 'react';
import { ViewKey, SchemaMode, TableMeta } from '../types';
import { schemaApi } from '../api';
import { DataSourceConfig, DataSourceInput } from '../api/types';
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
  // —— 多数据源管理 ——
  /** 当前选中的数据源 ID */
  dsId: number | null;
  /** 已注册的数据源列表 */
  dataSources: DataSourceConfig[];
  /** 切换当前数据源 */
  selectDataSource: (id: number) => Promise<void>;
  /** 新增数据源并自动选中扫描 */
  createDataSource: (form: DataSourceInput) => Promise<void>;
  /** 删除数据源 */
  removeDataSource: (id: number) => Promise<void>;
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
  const [dataSources, setDataSources] = useState<DataSourceConfig[]>([]);

  /** 拉取某数据源全部表的结构详情，组装成 name→TableMeta */
  const loadDetails = async (id: number, names: string[]) => {
    const entries = await Promise.all(
      names.map(async n => [n, fromTableDetail(await schemaApi.getTableDetail(id, n))] as const));
    return Object.fromEntries(entries) as Record<string, TableMeta>;
  };

  /** 将指定数据源加载为当前视图 */
  const loadDataSourceById = async (ds: DataSourceConfig) => {
    setDsId(ds.id);
    setDsName(ds.name);
    setDbName(ds.databaseName);
    const names = await schemaApi.listTables(ds.id);
    setTableNames(names);
    setDetails(await loadDetails(ds.id, names));
    setSchemaSource('real');
    if (names.length) setCurTable(c => (names.includes(c) ? c : names[0]));
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

  /** 刷新数据源列表，并按 targetId（未指定则取第一个）选中 */
  const refreshDataSources = useCallback(async (targetId?: number) => {
    const dss = await schemaApi.listDataSources();
    setDataSources(dss);
    if (dss.length) {
      const ds = (targetId != null ? dss.find(d => d.id === targetId) : undefined) ?? dss[0];
      await loadDataSourceById(ds);
    } else {
      setDsId(null);
      setDsName('');
      setDbName('');
      setTableNames([]);
      setDetails({});
      setSchemaSource('real');
    }
  }, []);

  /** 首屏加载：取数据源列表并默认选中第一个 */
  const loadSchema = useCallback(async () => {
    setSchemaLoading(true);
    try {
      await refreshDataSources();
      if (!dataSources.length && !tableNames.length) {
        // 列表为空且无真实表，回退 mock
        fallbackToMock();
      }
    } catch {
      fallbackToMock();
    } finally {
      setSchemaLoading(false);
    }
  }, [fallbackToMock, refreshDataSources, dataSources.length, tableNames.length]);

  useEffect(() => {
    loadSchema();
  }, [loadSchema]);

  /** 切换当前数据源 */
  const selectDataSource = useCallback(async (id: number) => {
    const ds = dataSources.find(d => d.id === id);
    if (!ds) return;
    setSchemaLoading(true);
    try {
      await loadDataSourceById(ds);
    } catch (e) {
      toast('切换数据源失败：' + ((e as Error).message || '后端异常'));
    } finally {
      setSchemaLoading(false);
    }
  }, [dataSources]);

  /** 新增数据源：创建后刷新列表、选中新源并自动扫描 */
  const createDataSource = useCallback(async (form: DataSourceInput) => {
    setSchemaLoading(true);
    try {
      const ds = await schemaApi.addDataSource(form);
      toast(`数据源 ${ds.name} 已创建`);
      await refreshDataSources(ds.id);
    } catch (e) {
      toast('创建失败：' + ((e as Error).message || '后端异常'));
      throw e;
    } finally {
      setSchemaLoading(false);
    }
  }, [refreshDataSources]);

  /** 删除数据源：刷新列表，若删的是当前源则切到第一个 */
  const removeDataSource = useCallback(async (id: number) => {
    setSchemaLoading(true);
    try {
      await schemaApi.deleteDataSource(id);
      toast('数据源已删除');
      await refreshDataSources(dsId === id ? undefined : dsId ?? undefined);
    } catch (e) {
      toast('删除失败：' + ((e as Error).message || '后端异常'));
    } finally {
      setSchemaLoading(false);
    }
  }, [dsId, refreshDataSources]);

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
      dataSources, dsId, selectDataSource, createDataSource, removeDataSource,
    }}>
      {children}
    </Ctx.Provider>
  );
}

export const useStore = () => useContext(Ctx);
