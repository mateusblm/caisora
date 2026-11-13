// Orquestracao manual. A geracao Java/SQL e feita exclusivamente pelo MBG.
import './env.mjs';
import { spawnSync } from 'node:child_process';
import { mkdirSync, readdirSync, rmSync, existsSync, writeFileSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.dirname(fileURLToPath(import.meta.url));
const compose = ['compose', '--project-directory', root, '-f', path.join(root, 'compose.yaml')];
function docker(...args) {
  const result = spawnSync('docker', [...compose, ...args], { cwd: root, stdio: 'inherit' });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`docker ${args.join(' ')} falhou: ${result.status}`);
}
function maven(...goals) { docker('run', '--rm', '-T', 'runner', 'mvn', '-B', '-ntp', ...goals); }
function prepare() {
  docker('up', '-d', '--wait', 'postgres');
  maven('flyway:migrate', 'flyway:validate');
  docker('exec', '-T', 'postgres', 'psql', '-U', 'caisora_demo', '-d', 'caisora_database_first', '-f', '/demo/seed.sql');
}
function clearGenerated() {
  // Alvo literal relativo a este script; nunca remove src/ ou migrations.
  const target = path.resolve(root, 'generated');
  if (path.basename(root) !== 'database-first' || path.dirname(target) !== root) throw new Error('Diretorio de geracao inesperado.');
  rmSync(target, { recursive: true, force: true });
  mkdirSync(path.join(root, 'generated/java'), { recursive: true });
  mkdirSync(path.join(root, 'generated/resources'), { recursive: true });
}
function generate() {
  clearGenerated();
  maven('mybatis-generator:generate');
  const files = readdirSync(path.join(root, 'generated'), { recursive: true }).filter(f => /\.(java|xml)$/.test(f)).map(f => f.split(path.sep).join('/')).sort();
  if (files.length !== 6) throw new Error(`Esperados 6 arquivos MBG, encontrados ${files.length}`);
  writeFileSync(path.join(root, 'generated/manifest.json'), JSON.stringify({ tool: 'MyBatis Generator 1.4.2', database: 'caisora_database_first', files }, null, 2) + '\n');
  console.log('Gerados pelo MBG:\n' + files.join('\n'));
}
const action = process.argv[2] || 'help';
switch (action) {
  case 'prepare': prepare(); break;
  case 'clear-generated': clearGenerated(); break;
  case 'generate': generate(); break;
  case 'verify':
    docker('stop', 'runner'); prepare(); generate(); maven('clean', 'verify'); break;
  case 'prove-schema': {
    docker('stop', 'runner');
    const sql = statement => docker('exec', '-T', 'postgres', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'caisora_demo', '-d', 'caisora_database_first', '-c', statement);
    try {
      sql('ALTER TABLE organizacao_modulos ADD COLUMN demo_observacao varchar(80)');
      generate();
      const model = readFileSync(path.join(root, 'generated/java/br/com/caisora/dbfirst/generated/model/OrganizacaoModulo.java'), 'utf8');
      if (!model.includes('String demoObservacao')) throw new Error('A coluna adicionada ao banco nao apareceu no modelo gerado.');
      console.log('PROVA: coluna demo_observacao no PostgreSQL -> campo String demoObservacao gerado pelo MBG.');
      maven('clean', 'compile');
    } finally {
      sql('ALTER TABLE organizacao_modulos DROP COLUMN IF EXISTS demo_observacao');
      generate();
    }
    maven('clean', 'verify'); break;
  }
  case 'reproduce':
    // Apaga somente o volume do PostgreSQL deste projeto. Cache Maven e preservado.
    docker('stop', 'runner'); docker('rm', '-f', 'runner');
    docker('down');
    { const result = spawnSync('docker', ['volume', 'rm', 'caisora-database-first_database-first-data'], { stdio: 'inherit' });
      if (result.status !== 0) throw new Error('Nao foi possivel remover o volume exclusivo da demo; execute verify se ainda nao existe.'); }
    prepare(); generate(); maven('clean', 'verify'); break;
  case 'start':
    if (!existsSync(path.join(root, 'generated/manifest.json'))) throw new Error('Execute node demo.mjs verify primeiro.');
    docker('up', '-d', 'runner');
    { let ready = false;
      for (let attempt = 0; attempt < 60; attempt++) {
        try { if ((await fetch(`http://127.0.0.1:${process.env.DBF_HTTP_PORT || 8095}/`, { signal: AbortSignal.timeout(1000) })).ok) { ready = true; break; } } catch {}
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      if (!ready) throw new Error('Servidor nao iniciou. Consulte docker compose -f tools/database-first/compose.yaml logs runner.');
      console.log(`Demo pronta em http://localhost:${process.env.DBF_HTTP_PORT || 8095}`);
    } break;
  case 'stop': docker('down'); break;
  case 'inspect':
    docker('exec', '-T', 'postgres', 'psql', '-U', 'caisora_demo', '-d', 'caisora_database_first', '-c', '\\d organizacao_modulos', '-c', '\\d perfil_permissoes', '-c', 'TABLE organizacao_modulos', '-c', 'TABLE perfil_permissoes'); break;
  default: console.log('Uso: node demo.mjs prepare|clear-generated|generate|verify|reproduce|prove-schema|start|stop|inspect');
}
