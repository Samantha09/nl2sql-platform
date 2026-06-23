import { useState, FormEvent } from 'react';
import { useStore } from '../lib/store';
import { DataSourceInput } from '../api/types';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

const DEFAULT_FORM: DataSourceInput = {
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  databaseName: '',
  username: 'root',
  passwordEncrypted: '',
};

export default function DataSourceModal({ isOpen, onClose }: Props) {
  const { createDataSource } = useStore();
  const [form, setForm] = useState<DataSourceInput>(DEFAULT_FORM);
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.host || !form.databaseName || !form.passwordEncrypted) return;
    setLoading(true);
    try {
      await createDataSource(form);
      setForm(DEFAULT_FORM);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const update = <K extends keyof DataSourceInput>(key: K, value: DataSourceInput[K]) =>
    setForm(f => ({ ...f, [key]: value }));

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-head">
          <h3>新增数据源</h3>
          <button className="modal-close" onClick={onClose} aria-label="关闭">×</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <label>
              <span>名称</span>
              <input
                type="text"
                value={form.name}
                onChange={e => update('name', e.target.value)}
                placeholder="例如：生产订单库"
                required
              />
            </label>
            <label>
              <span>类型</span>
              <select value={form.type} onChange={e => update('type', e.target.value)}>
                <option value="mysql">MySQL</option>
              </select>
            </label>
            <label>
              <span>主机</span>
              <input
                type="text"
                value={form.host}
                onChange={e => update('host', e.target.value)}
                placeholder="localhost"
                required
              />
            </label>
            <label>
              <span>端口</span>
              <input
                type="number"
                value={form.port}
                onChange={e => update('port', Number(e.target.value))}
                min={1}
                max={65535}
                required
              />
            </label>
            <label>
              <span>数据库</span>
              <input
                type="text"
                value={form.databaseName}
                onChange={e => update('databaseName', e.target.value)}
                placeholder="database_name"
                required
              />
            </label>
            <label>
              <span>用户名</span>
              <input
                type="text"
                value={form.username}
                onChange={e => update('username', e.target.value)}
                placeholder="root"
                required
              />
            </label>
            <label>
              <span>密码</span>
              <input
                type="password"
                value={form.passwordEncrypted}
                onChange={e => update('passwordEncrypted', e.target.value)}
                placeholder=""
                required
              />
            </label>
          </div>
          <div className="modal-foot">
            <button type="button" className="btn-ghost" onClick={onClose} disabled={loading}>取消</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? '保存中…' : '保存并扫描'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
