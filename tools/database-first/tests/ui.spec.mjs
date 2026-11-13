import { test, expect } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const evidence = path.resolve(root, '../../docs/database-first/evidence');
const authorization = 'Basic ' + Buffer.from('demo:' + (process.env.DBF_TOKEN || 'demo-local-123')).toString('base64');
const baseline = {
  modulos: [
    { modulo: 'CLIENTES', ativo: true }, { modulo: 'EMBARCACOES', ativo: true },
    { modulo: 'VAGAS', ativo: true }, { modulo: 'OCUPACOES', ativo: true },
    { modulo: 'MOVIMENTACOES', ativo: true }, { modulo: 'CONTRATOS', ativo: false }
  ],
  permissoes: [
    { modulo: 'CLIENTES', perfil: 'ATENDENTE', acao: 'VISUALIZAR', permitido: true },
    { modulo: 'VAGAS', perfil: 'ATENDENTE', acao: 'VISUALIZAR', permitido: true },
    { modulo: 'MOVIMENTACOES', perfil: 'GERENTE', acao: 'INICIAR', permitido: true },
    { modulo: 'CONTRATOS', perfil: 'FINANCEIRO', acao: 'VISUALIZAR', permitido: true }
  ]
};
function persisted(table, id) {
  expect(id).toMatch(/^[0-9a-f-]{36}$/);
  const name = table === 'modulos' ? 'organizacao_modulos' : 'perfil_permissoes';
  const result = spawnSync('docker', ['compose', '--project-directory', root, '-f', path.join(root, 'compose.yaml'), 'exec', '-T', 'postgres', 'psql', '-U', 'caisora_demo', '-d', 'caisora_database_first', '-At', '-c', "SELECT row_to_json(t) FROM " + name + " t WHERE id = '" + id + "'"], { encoding: 'utf8' });
  expect(result.status, result.stderr).toBe(0);
  return result.stdout.trim() ? JSON.parse(result.stdout) : null;
}
test('Configuração de módulos e permissões: cadastro, edição, conflito e remoção', async ({ page, request }) => {
  mkdirSync(evidence, { recursive: true });
  const errors = [], proof = [];
  page.on('pageerror', error => errors.push(error.message));
  const api = (table, method = 'GET', data, id = '') => request.fetch('/api/' + table + (id ? '/' + id : ''), { method, headers: { Authorization: authorization }, data });
  const createdBaseline = [], targets = [];
  try {
    for (const table of ['modulos', 'permissoes']) {
      const current = await (await api(table)).json();
      for (const data of baseline[table]) {
        if (current.some(row => row.modulo === data.modulo && (table === 'modulos' || (row.perfil === data.perfil && row.acao === data.acao)))) continue;
        const response = await api(table, 'POST', data);
        expect(response.status()).toBe(201);
        createdBaseline.push([table, (await response.json()).id]);
      }
    }
    await page.goto('/');
    await page.getByLabel('Senha', { exact: true }).fill(process.env.DBF_TOKEN || 'demo-local-123');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await expect(page.locator('#workspace')).toBeVisible();
    await page.screenshot({ path: path.join(evidence, 'tela-modulos-listagem.png') });

    for (const table of ['modulos', 'permissoes']) {
      const permission = table === 'permissoes';
      await page.locator('#nav-' + table).click();
      await expect(page.locator('#count')).not.toHaveText('Carregando…');
      if (permission) await page.screenshot({ path: path.join(evidence, 'tela-permissoes-listagem.png') });
      const addLabel = permission ? 'Adicionar permissão' : 'Adicionar módulo';
      const targetCode = permission ? 'RELATORIOS' : 'CHECKLIST_SAIDA';
      const targetName = permission ? 'Relatórios' : 'Checklist de saída';
      const before = await (await api(table)).json();
      expect(before.some(row => row.modulo === targetCode), 'Use um banco limpo para o cenário de entrega.').toBe(false);
      const fill = async () => {
        await page.locator('#new-record').click();
        await page.getByLabel('Módulo', { exact: true }).selectOption(targetCode);
        if (permission) {
          await page.getByLabel('Perfil', { exact: true }).selectOption('GERENTE');
          await page.getByLabel('Ação', { exact: true }).selectOption('VISUALIZAR');
        }
      };
      await fill();
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-formulario.png') });
      const createdResponse = page.waitForResponse(response => response.url().endsWith('/api/' + table) && response.request().method() === 'POST');
      await page.locator('#save').click();
      const created = await (await createdResponse).json();
      targets.push([table, created.id]);
      const row = page.locator('tbody tr').filter({ hasText: targetName });
      await expect(row).toHaveCount(1);
      await expect(page.getByRole('status')).toContainText('com sucesso');
      expect(persisted(table, created.id)[permission ? 'permitido' : 'ativo']).toBe(true);
      proof.push({ table, step: 'insert', http: created, postgres: persisted(table, created.id) });
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-cadastro.png') });

      await fill(); await page.locator('#save').click();
      await expect(page.locator('#form-error')).toContainText('Já existe');
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-conflito.png') });
      await page.locator('#cancel').click();
      await row.getByRole('button', { name: 'Editar ' + targetName, exact: true }).click();
      await page.locator('#enabled').selectOption('false');
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-edicao.png') });
      await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click();
      await expect(page.getByRole('status')).toContainText('Alterações salvas');
      await expect(row).toContainText(permission ? 'Bloqueado' : 'Desativado');
      expect(persisted(table, created.id)[permission ? 'permitido' : 'ativo']).toBe(false);
      proof.push({ table, step: 'update', postgres: persisted(table, created.id) });
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-alteracao.png') });
      if (!permission) {
        await page.setViewportSize({ width: 390, height: 844 });
        await page.screenshot({ path: path.join(evidence, 'tela-mobile.png'), fullPage: true });
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
        await page.setViewportSize({ width: 1440, height: 1000 });
      }
      await row.getByRole('button', { name: 'Remover ' + targetName, exact: true }).click();
      await expect(page.locator('#delete-dialog')).toBeVisible();
      await page.locator('#cancel-delete').click(); await expect(row).toHaveCount(1);
      await row.getByRole('button', { name: 'Remover ' + targetName, exact: true }).click();
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-confirmacao.png') });
      await page.locator('#confirm-delete').click();
      await expect(row).toHaveCount(0); await expect(page.getByRole('status')).toContainText('removid');
      expect(persisted(table, created.id)).toBe(null);
      expect((await api(table, 'GET', undefined, created.id)).status()).toBe(404);
      proof.push({ table, step: 'delete', postgres: null, getStatus: 404 });
      await page.locator('#refresh').click(); await expect(page.getByRole('status')).toContainText('atualizada');
      await expect(row).toHaveCount(0);
      await page.screenshot({ path: path.join(evidence, 'tela-' + table + '-apos-exclusao.png') });
      if (permission) await page.screenshot({ path: path.join(evidence, 'tela-apos-exclusao.png') });

      await page.getByLabel('Buscar registros').fill('sem-resultados');
      await expect(page.locator('#empty')).toBeVisible();
      await page.getByLabel('Buscar registros').clear();
      await expect(page.locator('#empty')).toBeHidden();
    }
    expect(errors).toEqual([]);
    writeFileSync(path.join(evidence, 'capturas-persistencia.json'), JSON.stringify(proof, null, 2) + '\n');
  } finally {
    for (const [table, id] of [...targets, ...createdBaseline]) await api(table, 'DELETE', undefined, id);
  }
});
