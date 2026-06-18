import { KB } from '../data/mock';
import { KBItem } from '../types';

/** 关键词匹配：返回命中的知识库条目（mock NL→SQL） */
export function matchQuestion(q: string): KBItem | undefined {
  return KB.find(k => k.keys.some(kw => q.includes(kw)));
}

/** SQL 语法高亮（返回 HTML 字符串，需配合 dangerouslySetInnerHTML） */
export function highlightSql(sql: string): string {
  return sql
    .replace(/--.*/g, m => `<span class="cmt">${m}</span>`)
    .replace(/\b(SELECT|FROM|WHERE|GROUP BY|ORDER BY|JOIN|ON|LIMIT|AS|AND|OR|DESC|ASC|SUM|COUNT|AVG)\b/g,
      (m) => (['SUM', 'COUNT', 'AVG'].includes(m) ? `<span class="fn">${m}</span>` : `<span class="kw">${m}</span>`))
    .replace(/'([^']*)'/g, `<span class="str">'$1'</span>`);
}
