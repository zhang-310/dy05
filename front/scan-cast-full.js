const fs = require('fs');
const path = require('path');

// Patterns to detect
const CAST_PATTERNS = [
  /as unknown as \w+/g,
  /as any\b/g,
  /: any\b/g,  // type annotation like ": any"
];

// Files to skip
const SKIP_DIRS = ['node_modules', 'dist', '.git', '__pycache__'];
const SKIP_PATTERNS = [/__tests?__/, /\.test\./, /\.spec\./, /test\.ts$/];

function shouldSkip(file) {
  return SKIP_PATTERNS.some(p => p.test(file));
}

function scanFile(file) {
  const content = fs.readFileSync(file, 'utf8');
  const issues = [];

  // Line-by-line analysis
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Skip comments
    const codePart = line.replace(/\/\/.*$/, '').replace(/\/\*[\s\S]*?\*\//g, '');

    // "as unknown as" pattern
    const asUnknownAs = codePart.match(/as unknown as (\w+)/);
    if (asUnknownAs) {
      issues.push({ line: i + 1, type: 'as unknown as', text: line.trim() });
    }

    // "as any" pattern (but not "as any[]" which is different)
    const asAnyMatch = codePart.match(/\bas any\b(?!\[)/);
    if (asAnyMatch) {
      issues.push({ line: i + 1, type: 'as any', text: line.trim() });
    }

    // ": any" in type annotation (but skip function params with "any" as default value)
    // Look for patterns like "param?: any)", "param: any,", ": any[]", "key: any;"
    const typeAnyMatch = codePart.match(/:(\s*)any(\s*)[,\)\[\];{}<>=]/);
    if (typeAnyMatch && !line.includes('mockReturnValue')) {
      issues.push({ line: i + 1, type: ': any', text: line.trim() });
    }
  }

  return issues;
}

function scanDir(dir, results = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.includes(entry.name) && !entry.name.startsWith('.')) {
        scanDir(full, results);
      }
    } else if (entry.name.match(/\.(ts|tsx)$/)) {
      if (shouldSkip(full)) continue;

      const issues = scanFile(full);
      if (issues.length > 0) {
        results.push({ file: full.replace(/\\/g, '/').replace(/^src\//, ''), issues });
      }
    }
  }
  return results;
}

const results = scanDir('src');

// Sort by total issues descending
results.sort((a, b) => b.issues.length - a.issues.length);

console.log('=== Type Safety Scan Results ===');
console.log(`Total files with issues: ${results.length}`);
console.log(`Total issues: ${results.reduce((s, r) => s + r.issues.length, 0)}\n`);

results.forEach((r, i) => {
  console.log(`${i + 1}. ${r.file} (${r.issues.length} issues)`);
  r.issues.slice(0, 5).forEach(iss => {
    console.log(`   L${iss.line}: [${iss.type}] ${iss.text.substring(0, 100)}`);
  });
  if (r.issues.length > 5) {
    console.log(`   ... and ${r.issues.length - 5} more`);
  }
  console.log('');
});

console.log('\n=== Top 20 Priority Queue ===');
const top20 = results.slice(0, 20).map((r, i) => ({
  rank: i + 1,
  file: r.file,
  issues: r.issues.length,
  types: [...new Set(r.issues.map(iss => iss.type))].join('+')
}));
top20.forEach(t => {
  console.log(`${t.rank}. ${t.file} - ${t.issues} casts [${t.types}]`);
});
