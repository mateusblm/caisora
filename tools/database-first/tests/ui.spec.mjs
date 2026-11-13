import { test, expect } from '@playwright/test';
import { mkdirSync } from 'node:fs';
const evidence = '../../docs/database-first/evidence';
test('CRUD nas duas telas, conflito, confirmacao de exclusao e layout mobile', async ({ page }) => {
  mkdirSync(evidence, { recursive: true });
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  await page.getByLabel('Senha local').fill(process.env.DBF_TOKEN || 'demo-local-123');
  await page.getByRole('button', { name: 'Entrar na demo' }).click();
  await expect(page.locator('#workspace')).toBeVisible();
  for (const table of ['modulos', 'permissoes']) {
    await page.getByLabel('Tabela da demonstração').selectOption(table);
    await expect(page.locator('#count')).not.toHaveText('Consultando PostgreSQL…');
    const label = `UI-${table}-${Date.now()}`;
    const fill = async () => {
      await page.getByLabel('Módulo', { exact: true }).fill(label);
      if (table === 'permissoes') { await page.getByLabel('Perfil', { exact: true }).fill('ATENDENTE'); await page.getByLabel('Ação', { exact: true }).fill('CONSULTAR'); }
    };
    await fill(); await page.getByRole('button', { name: 'Cadastrar', exact: true }).click();
    const row = page.locator('tbody tr').filter({ hasText: label });
    await expect(row).toHaveCount(1); await expect(page.getByRole('status')).toContainText('inserido');
    await page.screenshot({ path: `${evidence}/tela-${table}-cadastro.png`, fullPage: true });
    await fill(); await page.getByRole('button', { name: 'Cadastrar', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Ja existe');
    await row.getByRole('button', { name: `Editar ${label}`, exact: true }).click();
    await page.locator('#enabled').selectOption('false');
    await page.getByRole('button', { name: 'Salvar alteração' }).click();
    await expect(page.getByRole('status')).toContainText('Alteração salva'); await expect(row).toContainText('Não');
    await page.screenshot({ path: `${evidence}/tela-${table}-alteracao.png`, fullPage: true });
    if (table === 'modulos') {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.screenshot({ path: `${evidence}/tela-mobile.png`, fullPage: true });
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
      await page.setViewportSize({ width: 1440, height: 1000 });
    }
    await row.getByRole('button', { name: `Excluir ${label}`, exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible(); await page.getByRole('button', { name: 'Manter registro' }).click();
    await expect(row).toHaveCount(1);
    await row.getByRole('button', { name: `Excluir ${label}`, exact: true }).click();
    await page.screenshot({ path: `${evidence}/tela-${table}-confirmacao.png`, fullPage: true });
    await page.getByRole('button', { name: 'Excluir registro', exact: true }).click();
    await expect(row).toHaveCount(0); await expect(page.getByRole('status')).toContainText('excluído');
    await page.getByRole('button', { name: 'Atualizar listagem' }).click();
    await expect(page.getByRole('status')).toContainText('consultada novamente'); await expect(row).toHaveCount(0);
  }
  await expect(page.locator('#empty')).toBeVisible();
  await page.screenshot({ path: `${evidence}/tela-apos-exclusao.png`, fullPage: true });
  expect(errors).toEqual([]);
});
