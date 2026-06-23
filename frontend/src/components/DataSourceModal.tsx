import { useState, FormEvent, useEffect } from 'react';
import { useStore } from '../lib/store';
import { DataSourceConfig, DataSourceInput } from '../api/types';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  mode?: 'create' | 'edit';
  initialData?: DataSourceConfig | null;
}

const EMPTY_FORM: DataSourceInput = {
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  databaseNames: [],
  username: 'root',
  passwordEncrypted: '',
};

export default function DataSourceModal({ isOpen, onClose, mode = 'create', initialData }: Props) {
  const { createDataSource, updateDataSource } = useStore();
  const [form, setForm] = useState<DataSourceInput>(EMPTY_FORM);
  const [dbInput, setDbInput] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && initialData) {
        setForm({
          name: initialData.name,
          type: initialData.type,
          host: initialData.host,
          port: initialData.port,
          databaseNames: initialData.databaseNames || [],
          username: initialData.username || '',
          passwordEncrypted: '',
        });
      } else {
        setForm(EMPTY_FORM);
      }
      setDbInput('');
    }
  }, [isOpen, mode, initialData]);

  if (!isOpen) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.host || !form.databaseNames.length || !form.username) return;
    if (mode === 'create' && !form.passwordEncrypted) return;

    setLoading(true);
    try {
      if (mode === 'edit' && initialData) {
        await updateDataSource(initialData.id, form);
      } else {
        await createDataSource(form);
      }
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const update = <K extends keyof DataSourceInput>(key: K, value: DataSourceInput[K]) =>
    setForm(f => ({ ...f, [key]: value }));

  const addDatabaseName = () => {
    const name = dbInput.trim();
    if (!name) return;
    if (form.databaseNames.includes(name)) return;
    update('databaseNames', [...form.databaseNames, name]);
    setDbInput('');
  };

  const removeDatabaseName = (name: string) => {
    update('databaseNames', form.databaseNames.filter(n => n !== name));
  };

  const handleDbKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addDatabaseName();
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{mode === 'edit' ? '编辑数据源' : '新增数据源'}</h3>
          <button className="modal-close" onClick={onClose} aria-label="关闭">×</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <label>
              <span>名称</span>
              <input type="text" value={form.name} onChange={e => update('name', e.target.value)} placeholder="例如：生产订单库" required />
            </label>
            <label>
              <span>类型</span>
              <select value={form.type} onChange={e => update('type', e.target.value)}>
                <option value="mysql">MySQL</option>
              </select>
            </label>
            <label>
              <span>主机</span>
              <input type="text" value={form.host} onChange={e => update('host', e.target.value)} placeholder="localhost" required />
            </label>
            <label>
              <span>端口</span>
              <input type="number" value={form.port} onChange={e => update('port', Number(e.target.value))} min={1} max={65535} required />
            </label>
            <label>
              <span>数据库名（可多个）</span>
              <div className="tag-input">
                {form.databaseNames.map(name => (
                  <span key={name} className="tag">
                    {name}
                    <button type="button" onClick={() => removeDatabaseName(name)} aria-label="移除">×</button>
                  </span>
                ))}
                <input
                  type="text"
                  value={dbInput}
                  onChange={e => setDbInput(e.target.value)}
                  onKeyDown={handleDbKeyDown}
                  onBlur={addDatabaseName}
                  placeholder="输入库名回车添加"
                />
              </div>
            </label>
            <label>
              <span>用户名</span>
              <input type="text" value={form.username} onChange={e => update('username', e.target.value)} placeholder="root" required />
            </label>
            <label>
              <span>密码{mode === 'edit' && '（留空表示不变）'}</span>
              <input
                type="password"
                value={form.passwordEncrypted}
                onChange={e => update('passwordEncrypted', e.target.value)}
                placeholder={mode === 'edit' ? '留空表示不修改' : ''}
                required={mode === 'create'}
              />
            </label>
          </div>
          <div className="modal-foot">
            <button type="button" className="btn-ghost" onClick={onClose} disabled={loading}>取消</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? '保存中…' : (mode === 'edit' ? '保存' : '保存并扫描')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
