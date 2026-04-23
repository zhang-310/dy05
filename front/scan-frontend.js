const fs = require('fs');
const path = require('path');

const dirs = ['src/pages', 'src/components', 'src/utils', 'src/hooks'];
const results = { asAny: [], asUnknownAs: [], emptyCatch: [] };
const SKIP_DIRS = ['node_modules', '.next', 'dist', '__tests__'];

function scanDir(dir) {
  if (!fs.existsSync(dir)) return;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.some(d => entry.name.includes(d))) scanDir(full);
    } else if (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx')) {
      if (entry.name.includes('.test.') || entry.name.includes('.spec.')) continue;
      const content = fs.readFileSync(full, 'utf8');
      const lines = content.split('\n');
      const rel = path.relative('C:/claude/dy05/front', full).replace(/\\/g, '/');

      lines.forEach((line, i) => {
        // as any - any standalone usage of `: any` or `as any` (not in comments)
        const trimmed = line.trim();
        if (trimmed.startsWith('//') || trimmed.startsWith('*')) return;
        if (/\b: any\b/.test(line) || /\bas any\b/.test(line)) {
          results.asAny.push({ file: rel, line: i + 1, text: line.trim() });
        }
        // as unknown as
        if (/as unknown as/.test(line)) {
          results.asUnknownAs.push({ file: rel, line: i + 1, text: line.trim() });
        }
        // empty catch - not clipboard
        if (/\.catch\s*\(\s*\(\s*\)\s*=>\s*\{\s*\}\s*\)/.test(line)) {
          if (!line.includes('clipboard') && !line.includes('ClipboardItem')) {
            results.emptyCatch.push({ file: rel, line: i + 1, text: line.trim() });
          }
        }
      });
    }
  }
}

for (const d of dirs) scanDir(d);

console.log('=== as any: ' + results.asAny.length + ' ===');
results.asAny.slice(0, 50).forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 120)));
console.log('\n=== as unknown as: ' + results.asUnknownAs.length + ' ===');
results.asUnknownAs.slice(0, 50).forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 120)));
console.log('\n=== empty catch: ' + results.emptyCatch.length + ' ===');
results.emptyCatch.slice(0, 30).forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 120)));