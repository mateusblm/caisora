// Executado por PermanenciaVagasVisualTest com uma API real e PostgreSQL descartável.
// Não recebe URL de banco e não popula uma instância de produção.
import assert from "node:assert/strict";
import http from "node:http";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { resolve, extname } from "node:path";
import { createRequire } from "node:module";
const require = createRequire(
  resolve(
    process.env.CAISORA_BROWSER_TOOLS || "/tmp/caisora-browser-tools",
    "package.json",
  ),
);
const { chromium } = require("playwright");
const backend = new URL(process.argv[2]);
assert.equal(
  backend.hostname,
  "127.0.0.1",
  "A API de demonstração precisa estar no loopback",
);
const dist = resolve("frontend/dist/caisora-web/browser");
const out = resolve(
  process.env.CAISORA_VISUAL_OUTPUT || "docs/evidencias/se06",
);
await mkdir(out, { recursive: true });
const mime = {
  ".js": "application/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".woff2": "font/woff2",
  ".ico": "image/x-icon",
};
const server = http.createServer(async (req, res) => {
  if (req.url.startsWith("/api/")) {
    const headers = { ...req.headers, host: backend.host };
    // Proxy local da build, sem alterar o fluxo de autenticação JWT da aplicação.
    delete headers.origin;
    const upstream = http.request(
      new URL(req.url, backend),
      { method: req.method, headers },
      (incoming) => {
        res.writeHead(incoming.statusCode, incoming.headers);
        incoming.pipe(res);
      },
    );
    upstream.on("error", () => {
      res.writeHead(502);
      res.end();
    });
    req.pipe(upstream);
    return;
  }
  try {
    let file = resolve(
      dist,
      "." + new URL(req.url, "http://localhost").pathname,
    );
    if (!file.startsWith(dist + "/")) file = resolve(dist, "index.html");
    let body;
    try {
      body = await readFile(file);
    } catch {
      file = resolve(dist, "index.html");
      body = await readFile(file);
    }
    res.writeHead(200, {
      "content-type": mime[extname(file)] || "application/octet-stream",
    });
    res.end(body);
  } catch {
    res.writeHead(500);
    res.end();
  }
});
await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const url = `http://127.0.0.1:${server.address().port}`;
const browser = await chromium.launch({
  headless: true,
  args: ["--no-sandbox"],
});
const evidence = {
  browser: browser.version(),
  backend: "Spring Boot + PostgreSQL Testcontainers",
  date: new Date().toISOString(),
  scenarios: [],
  limitations: [
    "Mobile validado por emulação de viewport/touch no Chromium, sem aparelho físico.",
    "Agrupamento usa setor/tipo atuais da vaga, pois não há versionamento desses atributos.",
    "Dados presentes e vazios são respostas reais da API. Loading e erro usam interceptação de teste para reproduzir latência e indisponibilidade.",
  ],
};
try {
  for (const [name, viewport] of [
    ["desktop", { width: 1440, height: 1100 }],
    ["mobile", { width: 390, height: 844 }],
  ]) {
    const context = await browser.newContext({
      viewport,
      isMobile: name === "mobile",
      hasTouch: name === "mobile",
      deviceScaleFactor: 1,
      locale: "pt-BR",
    });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto(url + "/login");
    await page.locator("[formcontrolname=codigoOrganizacao]").fill("se06-a");
    await page.locator("[formcontrolname=email]").fill("demo-se06@example.com");
    await page.locator("[formcontrolname=senha]").fill("Demo-SE06-2026");
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await page.waitForURL("**/dashboard");
    await page.goto(url + "/relatorios");
    await page
      .getByRole("combobox", { name: "Relatório", exact: true })
      .click();
    await page
      .getByRole("option", {
        name: "Permanência histórica nas vagas",
        exact: true,
      })
      .click();
    await page
      .getByLabel("Encerramento inicial (UTC)", { exact: true })
      .fill("2026-10-10");
    await page
      .getByLabel("Encerramento final (UTC)", { exact: true })
      .fill("2026-10-10");
    await page
      .getByLabel("Setor (correspondência exata)", { exact: true })
      .fill("Norte");
    await page
      .getByRole("combobox", { name: "Tipo de vaga", exact: true })
      .click();
    await page.getByRole("option", { name: "seca", exact: true }).click();
    const generate = page.getByRole("button", {
      name: "Gerar relatório",
      exact: true,
    });
    const responsePromise = page.waitForResponse((r) =>
      r.url().includes("/relatorios/permanencia-vagas"),
    );
    await generate.click();
    const response = await responsePromise;
    assert.equal(response.status(), 200);
    const payload = await response.json();
    assert.equal(payload.resumo.quantidadeOcupacoes, 2);
    assert.equal(payload.resumo.permanenciaMediaSegundos, 7200);
    assert.equal(payload.resumo.totalHorasOcupacao, 4);
    assert.equal(payload.resumo.menorPermanenciaSegundos, 3600);
    assert.equal(payload.resumo.maiorPermanenciaSegundos, 10800);
    const card = page.getByTestId("permanencia-resultado");
    await card.waitFor();
    assert.match(await card.innerText(), /7,200\.00/);
    assert.equal(
      await page.getByTestId("quantidade-encerradas").innerText(),
      "2",
    );
    await page.evaluate(() => document.fonts.ready);
    const screenshot = async (state) => {
      if (state !== "tabela-rolada") {
        await page.locator("mat-sidenav-content").evaluate((el) => {
          el.scrollTop = 0;
        });
      } else {
        await page
          .getByRole("region", {
            name: "Permanência por setor e tipo, tabela com rolagem horizontal",
          })
          .scrollIntoViewIfNeeded();
      }
      await page.screenshot({
        path: resolve(out, `${name}-${state}.png`),
        fullPage: true,
      });
      if (state === "resultados" || state === "vazio") {
        // O layout rola dentro de mat-sidenav-content. Para capturar todo o
        // formulário/painel sem recortes, amplia somente a altura do viewport.
        const alturaCompleta =
          Math.ceil(
            (await page.locator("app-relatorios").boundingBox()).height,
          ) + 160;
        await page.setViewportSize({
          width: viewport.width,
          height: alturaCompleta,
        });
        await page.locator("mat-sidenav-content").evaluate((el) => {
          el.scrollTop = 0;
        });
        await page.screenshot({
          path: resolve(out, `${name}-${state}-painel.png`),
          fullPage: true,
        });
        await page.setViewportSize(viewport);
        evidence.scenarios.push({
          viewport: name,
          state: state + "-painel",
          screenshot: `${name}-${state}-painel.png`,
          captureWidth: viewport.width,
          captureHeight: alturaCompleta,
        });
      }
      if (state === "erro") {
        await page
          .getByRole("alert")
          .filter({ hasText: "Indisponibilidade controlada" })
          .screenshot({ path: resolve(out, `${name}-erro-mensagem.png`) });
        evidence.scenarios.push({
          viewport: name,
          state: "erro-mensagem",
          screenshot: `${name}-erro-mensagem.png`,
        });
      }
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth,
      );
      assert.equal(overflow, false, `overflow da página em ${name}/${state}`);
      const contentOverflow = await page
        .locator("mat-sidenav-content")
        .evaluate((el) => el.scrollWidth > el.clientWidth);
      assert.equal(
        contentOverflow,
        false,
        `overflow do conteúdo em ${name}/${state}`,
      );
      evidence.scenarios.push({
        viewport: name,
        state,
        screenshot: `${name}-${state}.png`,
        pageOverflow: overflow,
      });
    };
    await screenshot("resultados");
    // Inspeciona também a rolagem da tabela em viewport estreita.
    if (name === "mobile") {
      const region = page.getByRole("region", {
        name: "Permanência por setor e tipo, tabela com rolagem horizontal",
      });
      assert.equal(
        await region.evaluate((el) => el.scrollWidth > el.clientWidth),
        true,
      );
      await region.evaluate((el) => {
        el.scrollLeft = el.scrollWidth;
      });
      await screenshot("tabela-rolada");
    }
    await page
      .getByLabel("Encerramento inicial (UTC)", { exact: true })
      .fill("2026-11-01");
    await page
      .getByLabel("Encerramento final (UTC)", { exact: true })
      .fill("2026-11-01");
    const emptyPromise = page.waitForResponse((r) =>
      r.url().includes("/relatorios/permanencia-vagas"),
    );
    await generate.click();
    assert.equal((await emptyPromise).status(), 200);
    await page
      .getByText(
        "Nenhuma ocupação encerrada encontrada para os filtros informados.",
        { exact: true },
      )
      .waitFor();
    assert.equal(
      await page.getByTestId("quantidade-encerradas").innerText(),
      "0",
    );
    assert.match(await card.innerText(), /Sem dados/);
    await screenshot("vazio");
    let release;
    const gate = new Promise((resolve) => {
      release = resolve;
    });
    await page.route(
      "**/api/v1/relatorios/permanencia-vagas?**",
      async (route) => {
        await gate;
        await route.continue();
      },
      { times: 1 },
    );
    await generate.click();
    await page
      .getByRole("button", { name: "Calculando…", exact: true })
      .waitFor();
    assert.equal(
      await page
        .getByRole("button", { name: "Calculando…", exact: true })
        .isDisabled(),
      true,
    );
    await screenshot("loading");
    release();
    await generate.waitFor();
    await page.route(
      "**/api/v1/relatorios/permanencia-vagas?**",
      (route) =>
        route.fulfill({
          status: 503,
          contentType: "application/json",
          body: JSON.stringify({
            mensagem: "Indisponibilidade controlada para validação visual",
          }),
        }),
      { times: 1 },
    );
    await generate.click();
    await page
      .getByRole("alert")
      .filter({ hasText: "Indisponibilidade controlada" })
      .waitFor();
    assert.equal(await card.count(), 0);
    await screenshot("erro");
    assert.deepEqual(errors, [], "erros JavaScript no navegador");
    await context.close();
  }
  await writeFile(
    resolve(out, "verificacao.json"),
    JSON.stringify(evidence, null, 2) + "\n",
  );
  console.log(
    `SE06: ${evidence.scenarios.length} capturas validadas em ${out}`,
  );
} finally {
  await browser.close();
  await new Promise((resolve) => server.close(resolve));
}
