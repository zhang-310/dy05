const fs = require('fs');
const path = require('path');

function walk(dir) {
  let results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === '.git') continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) results = results.concat(walk(full));
    else if (entry.name.endsWith('.tsx') || entry.name.endsWith('.ts')) results.push(full);
  }
  return results;
}

const files = walk('src');
const casts = [];
for (const file of files) {
  const content = fs.readFileSync(file, 'utf-8');
  const lines = content.split('\n');
  lines.forEach((line, i) => {
    const matches = line.match(/as unknown as|as any/g);
    if (matches && matches.length > 0) {
      const rel = file.replace(/\\/g, '/').replace('src/', '');
      casts.push(rel + ':' + (i + 1) + ' (' + matches.length + '): ' + line.trim().slice(0, 120));
    }
  });
}
casts.forEach(c => console.log(c));
console.log('Total casts:', casts.length);
