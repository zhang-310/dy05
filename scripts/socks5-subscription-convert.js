#!/usr/bin/env node
/**
 * 将 socks5://host:port:user:pass 批量转为标准 socks5://user:pass@host:port，
 * 并生成明文订阅（多行）与 Base64 订阅串。
 *
 * 用法:
 *   node scripts/socks5-subscription-convert.js                    # 从 stdin 读
 *   node scripts/socks5-subscription-convert.js proxies.txt
 *   node scripts/socks5-subscription-convert.js --json proxies.txt
 *
 * 在代码里:
 *   const { convertLines, toPlainSubscription, toBase64Subscription } = require('./socks5-subscription-convert.js');
 */

const fs = require('fs');

/**
 * @param {string} line
 * @returns {string|null} 标准 URI，跳过空行/注释返回 null，无法解析抛错由调用方处理
 */
function convertLine(line) {
  const trimmed = line.trim();
  if (!trimmed || trimmed.startsWith('#')) return null;

  // 已是 socks5://user:pass@host:port
  if (/^socks5:\/\/.+@.+:\d+$/.test(trimmed)) {
    return trimmed;
  }

  const m = trimmed.match(/^socks5:\/\/(.+?):(\d+):([^:]+):([\s\S]+)$/);
  if (!m) {
    throw new Error(`无法解析行: ${trimmed.slice(0, 80)}${trimmed.length > 80 ? '…' : ''}`);
  }

  const [, host, port, user, pass] = m;
  const encUser = encodeURIComponent(user);
  const encPass = encodeURIComponent(pass);
  return `socks5://${encUser}:${encPass}@${host}:${port}`;
}

/**
 * @param {string|string[]} input 多行文本或行数组
 * @returns {string[]}
 */
function convertLines(input) {
  const lines = Array.isArray(input)
    ? input
    : input.split(/\r?\n/);
  const out = [];
  for (const line of lines) {
    const uri = convertLine(line);
    if (uri) out.push(uri);
  }
  return out;
}

/**
 * @param {string[]} uris
 * @returns {string} 每行一个，末尾无换行也可（订阅多数接受最后有换行）
 */
function toPlainSubscription(uris) {
  return uris.join('\n');
}

/**
 * @param {string[]} uris
 * @returns {string} UTF-8 明文再 Base64（多行一起编码）
 */
function toBase64Subscription(uris) {
  const plain = toPlainSubscription(uris);
  return Buffer.from(plain, 'utf8').toString('base64');
}

function main() {
  const args = process.argv.slice(2);
  const jsonOut = args.includes('--json');
  const files = args.filter((a) => a !== '--json');

  const readStdin = () =>
    new Promise((resolve, reject) => {
      let data = '';
      process.stdin.setEncoding('utf8');
      process.stdin.on('data', (chunk) => {
        data += chunk;
      });
      process.stdin.on('end', () => resolve(data));
      process.stdin.on('error', reject);
    });

  (async () => {
    let text = '';
    if (files.length === 0) {
      if (process.stdin.isTTY) {
        process.stderr.write(
          '用法: node scripts/socks5-subscription-convert.js [--json] [文件.txt]\n' +
            '或: cat proxies.txt | node scripts/socks5-subscription-convert.js\n',
        );
        process.exit(1);
      }
      text = await readStdin();
    } else {
      text = fs.readFileSync(files[0], 'utf8');
    }

    let uris;
    try {
      uris = convertLines(text);
    } catch (e) {
      process.stderr.write(String(e.message) + '\n');
      process.exit(1);
    }

    const plain = toPlainSubscription(uris);
    const b64 = toBase64Subscription(uris);

    if (jsonOut) {
      process.stdout.write(
        JSON.stringify(
          { count: uris.length, plain, base64: b64, lines: uris },
          null,
          2,
        ) + '\n',
      );
    } else {
      process.stdout.write('--- 明文订阅（每行一个）---\n');
      process.stdout.write(plain + '\n');
      process.stdout.write('\n--- Base64 订阅 ---\n');
      process.stdout.write(b64 + '\n');
    }
  })();
}

if (require.main === module) {
  main();
}

module.exports = {
  convertLine,
  convertLines,
  toPlainSubscription,
  toBase64Subscription,
};
