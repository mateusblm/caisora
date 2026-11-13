// Evidencia HTTP + leitura independente pelo psql. Nunca escreve SQL de CRUD.
import './env.mjs';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.dirname(fileURLToPath(import.meta.url));
const base = `http://127.0.0.1:${process.env.DBF_HTTP_PORT || 8095}`;
async function api(table, method, body, id = '') {
  const response = await fetch(`${base}/api/${table}${id ? '/' + id : ''}`, {
    method, headers: { Authorization: 'Basic ' + Buffer.from('demo:' + (process.env.DBF_TOKEN || 'demo-local-123')).toString('base64'), 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined
  });
  const value = response.status === 204 ? null : await response.json();
  console.log(`HTTP ${method} /api/${table}${id ? '/' + id : ''} -> ${response.status}`, JSON.stringify(value));
  return { status: response.status, value };
}
function database(table, id) {
  assert.match(id, /^[0-9a-f-]{36}$/);
  const name = table === 'modulos' ? 'organizacao_modulos' : 'perfil_permissoes';
  const result = spawnSync('docker', ['compose', '--project-directory', root, '-f', path.join(root, 'compose.yaml'), 'exec', '-T', 'postgres', 'psql', '-U', 'caisora_demo', '-d', 'caisora_database_first', '-At', '-c', `SELECT row_to_json(t) FROM ${name} t WHERE id = '${id}'`], { encoding: 'utf8' });
  if (result.status !== 0) throw new Error(result.stderr);
  console.log('PostgreSQL (conexao psql independente):', result.stdout.trim() || '(0 registros)');
  return result.stdout.trim() ? JSON.parse(result.stdout) : null;
}
for (const table of ['modulos', 'permissoes']) {
  let id;
  const data = table === 'modulos' ? { modulo: 'DEMO-' + Date.now(), ativo: true } : { modulo: 'DEMO-' + Date.now(), perfil: 'ATENDENTE', acao: 'CONSULTAR', permitido: true };
  const flag = table === 'modulos' ? 'ativo' : 'permitido';
  try {
    const created = await api(table, 'POST', data); assert.equal(created.status, 201); id = created.value.id;
    assert.equal(database(table, id)[flag], true);
    assert((await api(table, 'GET')).value.some(row => row.id === id));
    assert.equal((await api(table, 'POST', data)).status, 409);
    assert.equal((await api(table, 'PUT', { ...data, [flag]: false }, id)).status, 200);
    assert.equal(database(table, id)[flag], false);
    assert.equal((await api(table, 'DELETE', undefined, id)).status, 204);
    assert.equal(database(table, id), null);
    assert.equal((await api(table, 'GET', undefined, id)).status, 404);
  } finally { if (id) await api(table, 'DELETE', undefined, id); }
}
console.log('PASS: CRUD nas duas tabelas confirmado por HTTP e PostgreSQL independente.');
