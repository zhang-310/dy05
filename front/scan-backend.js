const fs = require('fs');
const path = require('path');

const BASE = 'C:/claude/dy05';
const SKIP_DIRS = ['node_modules', '.next', 'dist', 'target', '__tests__', '.git'];
const results = { emptyCatch: [], ignoredCatch: [], noLogCatch: [], systemOut: [], resourceRequired: [] };

function scanJava(dir) {
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!SKIP_DIRS.some(d => entry.name.includes(d))) scanJava(full);
      } else if (entry.name.endsWith('.java') && !entry.name.includes('test') && !entry.name.includes('.test.')) {
        const content = fs.readFileSync(full, 'utf8');
        const lines = content.split('\n');
        const rel = path.relative(BASE, full).replace(/\\/g, '/');

        // Find catch blocks with no logging
        for (let i = 0; i < lines.length; i++) {
          const l = lines[i].trim();
          const prev = i > 0 ? lines[i-1].trim() : '';
          const prev2 = i > 1 ? lines[i-2].trim() : '';

          // System.out/err
          if (/\bSystem\.out\b|\bSystem\.err\b/.test(l)) {
            results.systemOut.push({ file: rel, line: i+1, text: l.substring(0, 100) });
          }

          // @Resource with required=false (Jakarta doesn't support it)
          if (/@Resource.*required\s*=\s*false/.test(l) || /@Autowired.*required\s*=\s*false/.test(l)) {
            results.resourceRequired.push({ file: rel, line: i+1, text: l.trim() });
          }

          // Empty catch block: } catch (...) {}
          const emptyCatchMatch = l.match(/^\s*\}\s*catch\s*\(\s*(\w+Exception|\w+)\s+\w+\s*\)\s*\{\s*\}\s*$/);
          if (emptyCatchMatch) {
            results.emptyCatch.push({ file: rel, line: i+1, text: lines[i].trim() });
          }

          // Catch with "ignored" but no log call
          const ignoredCatch = l.match(/\}\s*catch\s*\([^)]+\s+\w+\s*\)\s*\{[^}]*ignored[^}]*$/);
          if (ignoredCatch) {
            results.ignoredCatch.push({ file: rel, line: i+1, text: l.trim() });
          }
        }

        // Check catch blocks spanning multiple lines — use proper brace-matching
        // Find all catch blocks via line-by-line scan (handles multi-line headers correctly)
        for (let i = 0; i < lines.length; i++) {
          const l = lines[i].trim();
          // Detect catch block start: } catch (...)  OR  } catch (...) { ... on same line
          // Also detect: catch (...) { on its own line
          const catchHeaderMatch = l.match(/^\}\s*catch\s*\(\s*[^)]+\s*\)\s*$/);
          const catchInlineMatch = l.match(/^\}\s*catch\s*\(\s*[^)]+\s*\)\s*\{.*$/);
          if (!catchHeaderMatch && !catchInlineMatch) continue;

          // Determine the line of the opening { of the catch
          let openLine = i;
          if (catchHeaderMatch) {
            // opening { is on next line(s) — find it
            let j = i + 1;
            while (j < lines.length && !lines[j].trim().startsWith('{')) j++;
            openLine = j;
          }

          // Find matching } for the catch block
          let depth = 0;
          let closeLine = -1;
          for (let j = openLine; j < lines.length; j++) {
            const lineContent = lines[j];
            for (const ch of lineContent) {
              if (ch === '{') depth++;
              else if (ch === '}') { depth--; if (depth === 0) { closeLine = j; break; } }
            }
            if (closeLine >= 0) break;
          }
          if (closeLine < 0) continue;

          // Extract catch body lines
          const bodyLines = [];
          for (let j = openLine + 1; j < closeLine; j++) {
            const tl = lines[j].trim();
            if (tl.startsWith('//')) continue; // strip line comments
            bodyLines.push(tl);
          }
          const bodyStr = bodyLines.join(' ');
          if (bodyStr.length === 0) continue; // empty (caught by single-line check)
          if (bodyStr === '// fallthrough') continue;

          // Check for logging
          const hasLog = /\blog\.\w+/.test(bodyStr) || /\blog\s*\(/.test(bodyStr);
          if (!hasLog) {
            results.noLogCatch.push({ file: rel, line: i + 1, text: bodyStr.substring(0, 80) });
          }
        }
      }
    }
  } catch(e) {}
}

scanJava(path.join(BASE, 'douyin-operations-app'));
scanJava(path.join(BASE, 'douyin-operations-common'));
scanJava(path.join(BASE, 'douyin-operations-intelligence'));
scanJava(path.join(BASE, 'douyin-operations-live'));
scanJava(path.join(BASE, 'douyin-operations-asset'));
scanJava(path.join(BASE, 'douyin-operations-douyin'));
scanJava(path.join(BASE, 'douyin-operations-integration'));
scanJava(path.join(BASE, 'douyin-operations-content'));
scanJava(path.join(BASE, 'douyin-operations-shortvideo'));
scanJava(path.join(BASE, 'douyin-operations-platform'));
scanJava(path.join(BASE, 'douyin-operations-payment'));

console.log('=== empty catch (no body):', results.emptyCatch.length, '===');
results.emptyCatch.forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 100)));
console.log('\n=== ignored catch (no log):', results.ignoredCatch.length, '===');
results.ignoredCatch.forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 100)));
console.log('\n=== catch with body but no log:', results.noLogCatch.length, '===');
results.noLogCatch.forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 100)));
console.log('\n=== System.out/err:', results.systemOut.length, '===');
results.systemOut.forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 100)));
console.log('\n=== @Autowired(required=false):', results.resourceRequired.length, '===');
results.resourceRequired.forEach(r => console.log(r.file + ':' + r.line + ' | ' + r.text.substring(0, 100)));
