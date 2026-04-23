const fs = require('fs');
const path = require('path');

function scanDir(dir) {
  const results = [];
  try {
    const files = fs.readdirSync(dir, { recursive: true, withFileTypes: true });
    for (const f of files) {
      if (!f.isFile()) continue;
      const full = path.join(dir, f.name);
      if (!/\.(tsx?|jsx?)$/.test(full)) continue;
      if (/test|spec|\.test\.|\.spec\./i.test(full)) continue;
      try {
        const content = fs.readFileSync(full, 'utf8');
        const lines = content.split('\n');
        lines.forEach((line, i) => {
          // as any
          if (/\bas\s+any\b/.test(line)) {
            const rel = full.replace(/\\/g, '/').replace('C:/claude/dy05/front/', '');
            results.push({ file: rel, line: i + 1, type: 'as any', text: line.trim().substring(0, 120) });
          }
          // as unknown as
          if (/as\s+unknown\s+as/.test(line)) {
            const rel = full.replace(/\\/g, '/').replace('C:/claude/dy05/front/', '');
            results.push({ file: rel, line: i + 1, type: 'as unknown as', text: line.trim().substring(0, 120) });
          }
          // catch with no body or just empty
          if (/catch\s*\(\s*\)\s*\{/.test(line) || /catch\s*\(\s*\)\s*=>/.test(line)) {
            const rel = full.replace(/\\/g, '/').replace('C:/claude/dy05/front/', '');
            results.push({ file: rel, line: i + 1, type: 'empty catch', text: line.trim().substring(0, 120) });
          }
        });
      } catch (e) {}
    }
  } catch (e) {}
  return results;
}
const r = scanDir('src');
process.stdout.write(JSON.stringify(r, null, 2));
