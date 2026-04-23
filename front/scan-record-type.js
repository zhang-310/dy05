const fs = require('fs');
const path = require('path');

function scanDir(dir) {
  let count = 0;
  let files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!entry.name.startsWith('.') && entry.name !== 'node_modules' && entry.name !== 'dist') {
        const sub = scanDir(full);
        count += sub.count;
        files.push(...sub.files);
      }
    } else if (entry.name.match(/\.(ts|tsx)$/)) {
      // Skip test files
      if (full.includes('__tests__') || full.includes('.test.') || full.includes('.spec.')) continue;
      const content = fs.readFileSync(full, 'utf8');
      const matches = [...content.matchAll(/Record<string, unknown>/g)];
      if (matches.length > 0) {
        count += matches.length;
        const pos = content.indexOf('Record<string, unknown>');
        const sample = content.substring(Math.max(0, pos - 30), pos + 50);
        files.push({ file: full.replace(/\\/g, '/'), count: matches.length, sample });
      }
    }
  }
  return { count, files };
}

const result = scanDir('src');
console.log('Record<string, unknown> in non-test files:', result.count);
result.files.forEach(f => console.log(f.file + ': ' + f.count + ' -- ' + f.sample.trim()));
if (result.count === 0) console.log('\nCLEAN - no Record<string, unknown> in production source files');
