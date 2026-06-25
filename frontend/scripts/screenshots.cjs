const { chromium } = require('playwright');

const BASE = 'http://localhost:5173';
const CONV_ID = 'screenshot-demo-0001-0000-000000000001';
const OUTDIR = './docs/images';

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function waitForSpinner(page) {
  // 等待所有 thinking 状态消失
  await page.waitForFunction(() => !document.querySelector('.thinking'), { timeout: 30000 });
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PW_CHROME || '/usr/bin/google-chrome-stable',
  });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // 1. 概览页
  await page.goto(BASE, { waitUntil: 'networkidle' });
  await sleep(800);
  await page.screenshot({ path: `${OUTDIR}/overview.png` });

  // 2. 表结构页
  await page.click('button[data-tip="表结构"]');
  await page.waitForSelector('.objtree, .schema-panel', { timeout: 10000 });
  await sleep(800);
  await page.screenshot({ path: `${OUTDIR}/schema.png` });

  // 3. 智能查询页（先写入 conversationId 再进入，保证有历史记录）
  await page.evaluate((id) => localStorage.setItem('nl2sql-conversation-id', id), CONV_ID);
  await page.click('button[data-tip="智能查询"]');
  await page.waitForSelector('.chat-turn', { timeout: 15000 });
  await waitForSpinner(page);
  await sleep(500);
  await page.screenshot({ path: `${OUTDIR}/query.png` });

  // 4. 数据可视化页
  await page.click('button[data-tip="数据可视化"]');
  await page.waitForSelector('.charts-grid, .chart-card', { timeout: 10000 });
  await sleep(800);
  await page.screenshot({ path: `${OUTDIR}/charts.png` });

  await browser.close();
  console.log('截图已生成到', OUTDIR);
})();
