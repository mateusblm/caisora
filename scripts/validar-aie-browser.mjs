// Executado apenas pelos testes opt-in AIE; proxy local da build de produção.
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
const backend = new URL(process.argv[2]),
  mode = process.argv[3];
assert.equal(backend.hostname, "127.0.0.1");
assert.ok(["catalogos-controlados", "mares-controladas"].includes(mode));
const dist = resolve("frontend/dist/caisora-web/browser");
const out = resolve(
  process.env.CAISORA_AIE_OUTPUT || "docs/evidencias/apf-aie",
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
const respostas = new Map();
const server = http.createServer(async (req, res) => {
  if (req.url.startsWith("/api/")) {
    const headers = { ...req.headers, host: backend.host };
    delete headers.origin;
    const upstream = http.request(
      new URL(req.url, backend),
      { method: req.method, headers },
      (incoming) => {
        const partes = [];
        incoming.on("data", (parte) => partes.push(parte));
        incoming.on("end", () => {
          const grupo = new URL(req.url, backend).pathname.split("/").at(-1);
          try {
            respostas.set(grupo, {
              status: incoming.statusCode,
              data: JSON.parse(Buffer.concat(partes).toString("utf8")),
            });
          } catch {
            respostas.set(grupo, { status: incoming.statusCode, data: null });
          }
        });
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
  modo: mode,
  navegador: browser.version(),
  consultadoEm: new Date().toISOString(),
  cenarios: [],
  limites: [
    "Desktop 1440×1100; mobile emulado 390×844 no Chromium real, sem aparelho físico.",
    "Painéis completos preservam a largura e ampliam a altura para capturar a rolagem interna do layout.",
    "Os provedores são servidores HTTP controlados de teste; a API, login e PostgreSQL são reais. As consultas reais externas são registradas separadamente em consultas-reais.json.",
    "WorldTides é exclusivamente um servidor de teste local; nenhuma chamada à API paga é executada.",
  ],
};
let activePage;
let phase = "inicio";
try {
  for (const [viewportName, viewport] of [
    ["desktop", { width: 1440, height: 1100 }],
    ["mobile", { width: 390, height: 844 }],
  ]) {
    const context = await browser.newContext({
      viewport,
      isMobile: viewportName === "mobile",
      hasTouch: viewportName === "mobile",
      locale: "pt-BR",
    });
    const page = await context.newPage();
    activePage = page;
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    assert.equal(
      (await context.request.get(url + "/api/v1/referencias/paises")).status(),
      401,
    );
    await page.goto(url + "/login");
    await page.locator("[formcontrolname=codigoOrganizacao]").fill("aie-a");
    await page.locator("[formcontrolname=email]").fill("demo-aie@example.com");
    await page.locator("[formcontrolname=senha]").fill("Demo-AIE-2026");
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await page.waitForURL("**/dashboard");
    const capture = async (grupo, state, complete = false) => {
      let captureHeight = viewport.height;
      if (complete) {
        for (let i = 0; i < 3; i++) {
          captureHeight =
            Math.ceil(
              (await page.locator("app-referencias").boundingBox()).height,
            ) + 180;
          await page.setViewportSize({
            width: viewport.width,
            height: captureHeight,
          });
        }
      }
      if (state !== "tabela-rolada") {
        await page.locator("mat-sidenav-content").evaluate((el) => {
          el.scrollTop = 0;
        });
      } else {
        await page
          .getByRole("region", {
            name: "Resultados: " + (await page.locator("h1").innerText()),
          })
          .scrollIntoViewIfNeeded();
      }
      await page.evaluate(() => document.fonts.ready);
      const name = `${grupo}-${viewportName}-${state}.png`;
      await page.screenshot({ path: resolve(out, name), fullPage: true });
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth,
        ),
        false,
      );
      assert.equal(
        await page
          .locator("mat-sidenav-content")
          .evaluate((el) => el.scrollWidth > el.clientWidth),
        false,
      );
      evidence.cenarios.push({
        grupo,
        viewport: viewportName,
        estado: state,
        captura: name,
        largura: viewport.width,
        altura: captureHeight,
        overflow: false,
      });
      if (complete) await page.setViewportSize(viewport);
    };
    const grupos =
      mode === "catalogos-controlados"
        ? ["paises", "municipios", "feriados"]
        : ["mares"];
    for (const grupo of grupos) {
      phase = viewportName + "/" + grupo;
      await page.goto(url + "/referencias/" + grupo);
      if (grupo === "paises")
        await page
          .getByLabel("Nome ou código do país", { exact: true })
          .fill("Brasil");
      if (grupo === "municipios") {
        await page
          .getByLabel("Nome ou código do município", { exact: true })
          .fill("Florianópolis");
        await page.getByLabel("UF", { exact: true }).fill("SC");
      }
      if (grupo === "feriados") {
        await page.getByLabel("Ano", { exact: true }).fill("2026");
        await page.getByLabel("UF (opcional)", { exact: true }).fill("SC");
      }
      if (grupo === "mares")
        await page
          .getByText(
            "Consulta consome créditos WorldTides conforme o plano autorizado pela plataforma",
            { exact: true },
          )
          .waitFor();
      const consultar = page.getByRole("button", {
        name: "Consultar",
        exact: true,
      });
      await consultar.click();
      const card = page.getByTestId("referencia-resultado");
      await card.waitFor();
      const resposta = respostas.get(grupo);
      assert.equal(
        resposta?.status,
        200,
        `Resposta da API de teste ausente para ${grupo}/${viewportName}`,
      );
      const data = resposta.data;
      if (grupo === "paises") assert.equal(data.registros[0].codigoIso2, "BR");
      if (grupo === "municipios")
        assert.equal(data.registros[0].codigoIbge, 4205407);
      if (grupo === "feriados") {
        assert.ok(data.registros.some((r) => r.tipo === "national"));
        assert.ok(
          data.registros.some((r) => r.tipo === "state" && r.uf === "SC"),
        );
      }
      if (grupo === "mares") {
        assert.equal(data.registros[0].tipo, "High");
        assert.equal(data.registros[1].tipo, "Low");
        assert.equal(data.registros[0].unidade, "m");
        assert.match(data.copyright, /CONTROLADA/);
      }
      await capture(grupo, "resultado");
      await capture(grupo, "resultado-completo", true);
      if (viewportName === "mobile") {
        const tabela = page.getByRole("region", {
          name: "Resultados: " + (await page.locator("h1").innerText()),
        });
        assert.equal(
          await tabela.evaluate((el) => el.scrollWidth > el.clientWidth),
          true,
        );
        await tabela.evaluate((el) => {
          el.scrollLeft = el.scrollWidth;
        });
        await capture(grupo, "tabela-rolada");
      }
      if (viewportName === "desktop")
        await writeFile(
          resolve(out, `${grupo}-${mode}-resposta.json`),
          JSON.stringify({ modo: mode, grupo, resposta: data }, null, 2) + "\n",
        );
      const endpoint = `**/api/v1/referencias/${grupo}?**`;
      await page.route(
        endpoint,
        (route) =>
          route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({ ...data, registros: [] }),
          }),
        { times: 1 },
      );
      await consultar.click();
      await page
        .getByText("Nenhum registro encontrado para os filtros informados.", {
          exact: true,
        })
        .waitFor();
      await capture(grupo, "vazio-controlado", true);
      let liberar;
      const gate = new Promise((resolve) => {
        liberar = resolve;
      });
      await page.route(
        endpoint,
        async (route) => {
          await gate;
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(data),
          });
        },
        { times: 1 },
      );
      await consultar.click();
      await page
        .getByRole("button", { name: "Consultando…", exact: true })
        .waitFor();
      assert.equal(
        await page
          .getByRole("button", { name: "Consultando…", exact: true })
          .isDisabled(),
        true,
      );
      await capture(grupo, "loading");
      liberar();
      await consultar.waitFor();
      await page.route(
        endpoint,
        (route) =>
          route.fulfill({
            status: 503,
            contentType: "application/json",
            body: JSON.stringify({
              mensagem:
                "Serviço externo indisponível — falha controlada para inspeção",
            }),
          }),
        { times: 1 },
      );
      await consultar.click();
      await page
        .getByRole("alert")
        .filter({ hasText: "falha controlada" })
        .waitFor();
      assert.equal(await card.count(), 0);
      await capture(grupo, "erro-controlado", true);
    }
    if (mode === "catalogos-controlados") {
      await page.goto(url + "/referencias/mares");
      await page
        .getByText("WorldTides desabilitada.", { exact: false })
        .waitFor();
      assert.equal(
        await page
          .getByRole("button", { name: "Consultar", exact: true })
          .isDisabled(),
        true,
      );
      await capture("mares", "desabilitada", true);
    }
    assert.deepEqual(errors, []);
    await context.close();
  }
  await writeFile(
    resolve(out, `verificacao-${mode}.json`),
    JSON.stringify(evidence, null, 2) + "\n",
  );
} catch (error) {
  console.error(
    "DIAGNOSTICO AIE",
    phase,
    activePage?.url(),

    [...respostas].map(([grupo, r]) => ({
      grupo,
      status: r.status,
      mensagem: r.data?.mensagem,
    })),
  );
  if (activePage)
    await activePage.screenshot({
      path: resolve("/tmp", "aie-falha.png"),
      fullPage: true,
    });
  throw error;
} finally {
  await browser.close();
  await new Promise((resolve) => server.close(resolve));
}
