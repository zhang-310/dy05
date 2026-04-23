const fs = require('fs');
const path = require('path');

function scanJavaDir(dir) {
  const results = [];
  try {
    const files = fs.readdirSync(dir, { recursive: true, withFileTypes: true });
    for (const f of files) {
      if (!f.isFile()) continue;
      const full = path.join(dir, f.name);
      if (!/\.java$/.test(full)) continue;
      // Skip test directories
      if (/\/test\//.test(full) || /\\test\\/.test(full)) continue;

      try {
        const content = fs.readFileSync(full, 'utf8');
        const lines = content.split('\n');
        const rel = full.replace(/\\/g, '/');

        lines.forEach((line, i) => {
          // printStackTrace
          if (/\.printStackTrace\s*\(/ .test(line)) {
            results.push({ file: rel, line: i + 1, type: 'printStackTrace', text: line.trim().substring(0, 120) });
          }
          // System.out
          if (/System\.out\./.test(line) && !line.includes('//')) {
            results.push({ file: rel, line: i + 1, type: 'System.out', text: line.trim().substring(0, 120) });
          }
          // System.err
          if (/System\.err\./.test(line) && !line.includes('//')) {
            results.push({ file: rel, line: i + 1, type: 'System.err', text: line.trim().substring(0, 120) });
          }
        });

        // Check for empty catch blocks (catch without meaningful content)
        const emptyCatchRegex = /catch\s*\([^)]+\)\s*\{([^}]{0,5})\}/g;
        let match;
        while ((match = emptyCatchRegex.exec(content)) !== null) {
          const beforeMatch = content.substring(0, match.index);
          const lineNum = (beforeMatch.match(/\n/g) || []).length + 1;
          results.push({ file: rel, line: lineNum, type: 'empty catch', text: content.substring(match.index, match.index + 80) });
        }

        // Check for @Resource with problematic patterns
        const resourceRegex = /@Resource\s*\([^)]*required\s*=\s*false[^)]*\)/g;
        while ((match = resourceRegex.exec(content)) !== null) {
          const beforeMatch = content.substring(0, match.index);
          const lineNum = (beforeMatch.match(/\n/g) || []).length + 1;
          results.push({ file: rel, line: lineNum, type: '@Resource(required=false)', text: content.substring(match.index, match.index + 80) });
        }

      } catch (e) {}
    }
  } catch (e) {}
  return results;
}

const modules = [
  'C:/claude/dy05/douyin-operations-common/src/main/java',
  'C:/claude/dy05/douyin-operations-app/src/main/java',
  'C:/claude/dy05/douyin-operations-platform/src/main/java',
  'C:/claude/dy05/douyin-operations-integration/src/main/java',
  'C:/claude/dy05/douyin-operations-asset/src/main/java',
  'C:/claude/dy05/douyin-operations-content/src/main/java',
  'C:/claude/dy05/douyin-operations-intelligence/src/main/java',
  'C:/claude/dy05/douyin-operations-douyin/src/main/java',
  'C:/claude/dy05/douyin-operations-payment/src/main/java',
  'C:/claude/dy05/douyin-operations-live/src/main/java',
  'C:/claude/dy05/douyin-operations-shortvideo/src/main/java',
];

const allResults = [];
for (const m of modules) {
  if (fs.existsSync(m)) {
    allResults.push(...scanJavaDir(m));
  }
}

process.stdout.write(JSON.stringify(allResults, null, 2));
