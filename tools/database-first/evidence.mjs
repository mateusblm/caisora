// Captura stdout/stderr reais, sem sintetizar resultados de execucao.
import './env.mjs';
import { spawnSync } from 'node:child_process';
import { openSync, closeSync, mkdirSync, copyFileSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.dirname(fileURLToPath(import.meta.url));
const evidence = path.resolve(root, '../../docs/database-first/evidence');
mkdirSync(evidence, { recursive: true });
function record(file, executable, args, options = {}) {
  console.log(`Executando ${file}: ${executable} ${args.join(' ')}`);
  const descriptor = openSync(path.join(evidence, file), 'w');
  let result;
  try { result = spawnSync(executable, args, { cwd: root, stdio: ['ignore', descriptor, descriptor], ...options }); }
  finally { closeSync(descriptor); }
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${file} falhou (${result.status}). Consulte o log real.`);
}
for (const [action, file] of [['verify', 'verify.log'], ['reproduce', 'reproduce.log'], ['prove-schema', 'schema-change.log'], ['generate', 'generation.log'], ['start', 'start.log']]) {
  record(file, process.execPath, ['demo.mjs', action]);
}
record('crud-http-postgres.log', process.execPath, ['smoke.mjs']);
record('postgres-schema.log', process.execPath, ['demo.mjs', 'inspect']);
record('ui-tests.log', process.execPath, ['node_modules/@playwright/test/cli.js', 'test']);
for (const file of readdirSync(path.join(root, 'target/surefire-reports')).filter(file => file.endsWith('.txt'))) {
  copyFileSync(path.join(root, 'target/surefire-reports', file), path.join(evidence, file));
}
copyFileSync(path.join(root, 'target/schema-metadata.txt'), path.join(evidence, 'schema-metadata.txt'));
copyFileSync(path.join(root, 'target/all-tables-metadata.txt'), path.join(evidence, 'all-tables-metadata.txt'));
copyFileSync(path.join(root, 'target/playwright-results.json'), path.join(evidence, 'playwright-results.json'));
const files = readdirSync(path.join(root, 'generated'), { recursive: true }).filter(file => /\.(java|xml)$/.test(file)).sort();
const hashes = Object.fromEntries(files.map(file => [file.split(path.sep).join('/'), createHash('sha256').update(readFileSync(path.join(root, 'generated', file))).digest('hex')]));
writeFileSync(path.join(evidence, 'generated-sha256.json'), JSON.stringify(hashes, null, 2) + '\n');
console.log('Evidencias reais salvas em ' + evidence);
