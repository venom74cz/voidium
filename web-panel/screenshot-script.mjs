import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

(async () => {
  console.log('Starting browser...');
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  
  const docsDir = path.resolve('../void-craft.eu--AI-kontext...-/wiki/assets');
  if (!fs.existsSync(docsDir)) {
    fs.mkdirSync(docsDir, { recursive: true });
  }

  const routes = ['', 'players', 'modules', 'discord', 'ranks', 'playtime', 'votes', 'tickets', 'stats', 'feeds', 'config', 'console', 'ai', 'settings'];
  
  for (const r of routes) {
    const url = 'http://localhost:5173/#/' + r;
    console.log('Visiting ' + url);
    await page.goto(url, { waitUntil: 'networkidle' });
    await page.waitForTimeout(600); // UI animations
    
    // Expand config section if on config page
    if (r === 'config') {
      try {
        await page.click('text=General');
        await page.waitForTimeout(300);
      } catch(e) {}
    }

    const name = r === '' ? 'dashboard' : r;
    const dest = path.join(docsDir, `${name}.png`);
    await page.screenshot({ path: dest });
    console.log('Saved ' + dest);
  }
  
  await browser.close();
  console.log('Done.');
})().catch(console.error);
