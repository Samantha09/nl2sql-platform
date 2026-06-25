import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { KB } from '../data/mock';
import { highlightSql } from '../lib/nlsql';
import { useStore } from '../lib/store';
import { QueryHistory } from '../api/types';
import { queryApi, ApiError } from '../api';

const STORAGE_KEY = 'nl2sql-conversation-id';

function formatTime(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export default function QueryView() {
  const { dsId, dbName } = useStore();
  const [input, setInput] = useState('');
  const [history, setHistory] = useState<QueryHistory[]>([]);
  const [thinkingText, setThinkingText] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const [conversationId] = useState<string>(() => {
    let id = localStorage.getItem(STORAGE_KEY);
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem(STORAGE_KEY, id);
    }
    return id;
  });
  const bottomRef = useRef<HTMLDivElement>(null);

  const loadHistory = useCallback(async () => {
    try {
      const list = await queryApi.getHistory(conversationId);
      setHistory(list);
    } catch (e) {
      const reason = e instanceof ApiError ? e.message : '后端异常';
      setNotice(`加载聊天记录失败（${reason}）`);
      console.error('加载历史失败', e);
    }
  }, [conversationId]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history, thinkingText]);

  const run = async (q?: string) => {
    const text = (q ?? input).trim();
    if (!text) return;
    if (dsId == null) {
      setNotice('请先选择一个数据源');
      return;
    }
    setInput('');
    setThinkingText(text);
    setNotice('');
    try {
      await queryApi.nlQuery({
        dataSourceId: dsId,
        databaseName: dbName || undefined,
        naturalLanguage: text,
        conversationId,
      });
      await loadHistory();
    } catch (e) {
      const reason = e instanceof ApiError ? e.message : '后端异常';
      setNotice(`查询失败（${reason}）`);
    } finally {
      setThinkingText(null);
    }
  };

  const rerun = (h: QueryHistory) => {
    setInput(h.naturalLanguage);
    run(h.naturalLanguage);
  };

  return (
    <div className="query-chat">
      <div className="chat-messages">
        {history.length === 0 && !thinkingText && (
          <Empty msg="输入问题或点击示例，AI 会生成 SQL、执行并保存到聊天记录" />
        )}

        {/* 后端历史按 createdAt desc，这里从早到晚展示 */}
        {[...history].reverse().map(h => (
          <div key={h.id} className="chat-turn">
            <div className="chat-msg chat-msg-user">
              <div className="chat-meta">{formatTime(h.createdAt)}</div>
              <div className="chat-bubble chat-bubble-user">{h.naturalLanguage}</div>
            </div>
            <div className="chat-msg chat-msg-ai">
              <div className="chat-avatar">✦</div>
              <div className="chat-bubble chat-bubble-ai">
                <AiReply history={h} onRerun={() => rerun(h)} />
              </div>
            </div>
          </div>
        ))}

        {thinkingText && (
          <div className="chat-turn">
            <div className="chat-msg chat-msg-user">
              <div className="chat-bubble chat-bubble-user">{thinkingText}</div>
            </div>
            <div className="chat-msg chat-msg-ai">
              <div className="chat-avatar">✦</div>
              <div className="chat-bubble chat-bubble-ai thinking">
                <span className="spinner" /> AI 正在分析问题…
              </div>
            </div>
          </div>
        )}

        {notice && <div className="notice-fallback">{notice}</div>}
        <div ref={bottomRef} />
      </div>

      <div className="chat-input-area card">
        <div className="askbar">
          <input
            value={input}
            placeholder="例如：每个月的销售额趋势"
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') run(); }}
          />
          <button className="btn-primary" disabled={thinkingText != null} onClick={() => run()}>
            {thinkingText != null ? <><span className="spinner" /> 思考中</> : <>▶ 提问</>}
          </button>
        </div>
        <div className="chips">
          {KB.map((k, i) => <button key={i} className="chip-q" onClick={() => run(k.label)}>{k.label}</button>)}
        </div>
      </div>
    </div>
  );
}

function AiReply({ history: h, onRerun }: { history: QueryHistory; onRerun: () => void }) {
  const copySql = (sql: string, btn: HTMLButtonElement) => {
    navigator.clipboard?.writeText(sql);
    btn.textContent = '已复制';
    window.setTimeout(() => (btn.textContent = '复制'), 1500);
  };

  if (h.status === 'failed') {
    return <div className="chat-error">执行失败：{h.errorMessage || '未知错误'}</div>;
  }

  if (h.status === 'chat' || h.status === 'clarification') {
    return (
      <div className="chat-text">
        {h.generatedSql}
        <div className="chat-stats" style={{ marginTop: 10 }}>
          <span>{h.status === 'chat' ? '对话' : '需要澄清'}</span>
          <button className="chat-rerun" onClick={onRerun}>↻ 重新执行</button>
        </div>
      </div>
    );
  }

  // sql 或其他：展示 SQL 与执行统计
  return (
    <>
      <div className="sql-block" style={{ marginBottom: 10 }}>
        <button className="copybtn" onClick={e => copySql(h.generatedSql, e.currentTarget)}>复制</button>
        <pre dangerouslySetInnerHTML={{ __html: highlightSql(h.generatedSql) }} />
      </div>
      <ResultTable resultJson={h.resultJson} />
      <div className="chat-stats">
        <span>{h.resultCount} 行</span>
        <span>{h.executeTimeMs} ms</span>
        <button className="chat-rerun" onClick={onRerun}>↻ 重新执行</button>
      </div>
    </>
  );
}

function ResultTable({ resultJson }: { resultJson?: string }) {
  const rows = useMemo(() => {
    if (!resultJson) return [];
    try {
      const parsed = JSON.parse(resultJson);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }, [resultJson]);

  if (rows.length === 0) return null;
  const columns = Object.keys(rows[0]);

  return (
    <div className="result-table-wrap">
      <table className="data">
        <thead>
          <tr>
            {columns.map(col => <th key={col}>{col}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              {columns.map(col => (
                <td key={col}>{formatCell(row[col])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatCell(value: unknown) {
  if (value === null || value === undefined) return <span style={{ color: 'var(--ink-3)' }}>NULL</span>;
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  return String(value);
}

function Empty({ msg }: { msg: string }) {
  return (
    <div className="empty">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
      <div>{msg}</div>
    </div>
  );
}
