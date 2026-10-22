# Auditoria visual funcional do Caisora

SHA inspecionado: `247c5ef07c49e2ebe8d119770309dabbc4fb8386`. Branch atual: main. Código de produção preservado; os novos arquivos são exclusivamente documentação e instrumentos de teste. Alterações anteriores em frontend/angular.json e docs/padroes-de-projeto.md não pertencem à auditoria.

## Documentos

- [Inventário CSV](processos-funcionais.csv) e [versão detalhada](processos-funcionais.md).
- [Cobertura de rotas e ações](rotas-cobertura.csv).
- [Grupos de dados candidatos](grupos-de-dados.md).
- [Lacunas e divergências](lacunas-e-divergencias.md).
- [Cálculos observados nas seis saídas candidatas](calculos-observados.md).
- [Mensagens efetivamente vistas, deduplicadas](mensagens-observadas.json).
- [Manifesto do navegador e comparação de banco](execucao-browser.json).
- [Rastreio estático de campos, condições e ações](rastreio-fontes.json).

## Método e reprodução

Aplicação Angular de produção, API Spring Boot real, autenticação JWT real, PostgreSQL descartável Testcontainers. Dados exclusivamente sintéticos criados por código em src/test. Observador HTTP de hashes existe apenas no teste, ligado a 127.0.0.1, encerrado no finally. Nenhum token, senha real ou linha de banco é retornado pelo observador. O manifesto contém somente hashes/quantidades e campos públicos de teste. A fixture de histórico é reaproveitada; data corrente influencia transições realizadas agora.

```bash
npm --prefix frontend run build
CAISORA_APF_VISUAL=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  ./mvnw -Dtest=AuditoriaVisualApfTest test
CAISORA_APF_VISUAL=true CAISORA_APF_COMPLEMENTAR=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -Dtest=AuditoriaVisualApfTest test
# Passagens adicionais: manter as variáveis acima e acrescentar CAISORA_APF_SESSAO=true ou CAISORA_APF_CONTROLES=true.
python3 scripts/gerar-inventario-apf.py
```

Requer Docker, JDK21 e Playwright temporário instalado conforme ../apf-aie/README.md. Roteiro aceita somente host 127.0.0.1. Não consome serviços públicos nem WorldTides. WorldTides permanece desabilitada. Teste JUnit verifica execução do coletor; não declara todos os cenários aprovados. Falhas de seletores ou regras ficam explicitadas no manifesto.

Desktop base 1440×1100; mobile emulado em Chromium, base 390×844. Para contornar scroll interno do layout, altura ampliada preservando largura; dimensão exata por captura no manifesto. Imagens espelhadas durante ações desktop apenas alteram viewport, sem afirmar aparelho físico/touch. A passagem mobile separada usa isMobile e hasTouch.

## Capturas novas realmente obtidas no Chromium

Data UTC da execução: 2026-10-22T15:20:35.179Z; Chromium 156.0.8078.4. Todas abaixo usam dados fictícios de teste; Open-Meteo é resposta controlada upstream local. Não apresentam condições ambientais reais. SHA em todas: `247c5ef07c49e2ebe8d119770309dabbc4fb8386`. Fonte: `AuditoriaVisualApfTest` + `scripts/auditar-apf-browser.mjs`.

| Processo / estado | Perfil | Viewport base → captura | Imagem real do navegador |
|---|---|---|---|
| autenticacao / inicial | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1100 | [desktop](capturas/autenticacao/inicial-desktop.png) |
| autenticacao / credenciais-invalidas | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1100 | [desktop](capturas/autenticacao/credenciais-invalidas-desktop.png) |
| modulos-bloqueados / marina | ADMINISTRADOR_MARINA | 1440×1100 → 1440×2243 | [desktop](capturas/modulos-bloqueados/marina-desktop.png) |
| modulos-bloqueados / acesso-negado | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1396 | [desktop](capturas/modulos-bloqueados/acesso-negado-desktop.png) |
| liberar-modulos / antes | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/liberar-modulos/antes-desktop.png) |
| liberar-modulos / depois | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/liberar-modulos/depois-desktop.png) |
| dashboard / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1396 | [desktop](capturas/dashboard/inicial-desktop.png) |
| painel-tv / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1100 | [desktop](capturas/painel-tv/inicial-desktop.png) |
| clientes-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/inicial-desktop.png) |
| clientes-cadastrar-pf / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1256 | [desktop](capturas/clientes-cadastrar-pf/inicial-desktop.png) |
| clientes-cadastrar-pf / validacao-obrigatorios | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1256 | [desktop](capturas/clientes-cadastrar-pf/validacao-obrigatorios-desktop.png) |
| clientes-alterar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1256 | [desktop](capturas/clientes-alterar/inicial-desktop.png) |
| embarcacoes-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/inicial-desktop.png) |
| embarcacoes-cadastrar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2190 | [desktop](capturas/embarcacoes-cadastrar/inicial-desktop.png) |
| embarcacoes-cadastrar / validacao-obrigatorios | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2190 | [desktop](capturas/embarcacoes-cadastrar/validacao-obrigatorios-desktop.png) |
| embarcacoes-alterar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2190 | [desktop](capturas/embarcacoes-alterar/inicial-desktop.png) |
| vagas-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/inicial-desktop.png) |
| vagas-cadastrar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1425 | [desktop](capturas/vagas-cadastrar/inicial-desktop.png) |
| vagas-cadastrar / validacao-obrigatorios | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1425 | [desktop](capturas/vagas-cadastrar/validacao-obrigatorios-desktop.png) |
| vagas-alterar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1425 | [desktop](capturas/vagas-alterar/inicial-desktop.png) |
| ocupacoes-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-consultar/inicial-desktop.png) |
| ocupacoes-cadastrar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-cadastrar/inicial-desktop.png) |
| movimentacoes-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-consultar/inicial-desktop.png) |
| movimentacoes-cadastrar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1391 | [desktop](capturas/movimentacoes-cadastrar/inicial-desktop.png) |
| movimentacoes-cadastrar / validacao-obrigatorios | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1463 | [desktop](capturas/movimentacoes-cadastrar/validacao-obrigatorios-desktop.png) |
| contratos-consultar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-consultar/inicial-desktop.png) |
| contratos-cadastrar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1375 | [desktop](capturas/contratos-cadastrar/inicial-desktop.png) |
| contratos-cadastrar / validacao-obrigatorios | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1375 | [desktop](capturas/contratos-cadastrar/validacao-obrigatorios-desktop.png) |
| checklist / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/checklist/inicial-desktop.png) |
| localizacao / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/localizacao/inicial-desktop.png) |
| variabilidade / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2234 | [desktop](capturas/variabilidade/inicial-desktop.png) |
| mares-desabilitada / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/mares-desabilitada/inicial-desktop.png) |
| ocupacoes-alterar / inicial | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1288 | [desktop](capturas/ocupacoes-alterar/inicial-desktop.png) |
| clientes-consultar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/entrada-desktop.png) |
| clientes-consultar / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×1585 | [mobile](capturas/clientes-consultar/entrada-mobile.png) |
| clientes-consultar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/saida-desktop.png) |
| clientes-consultar / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1585 | [mobile](capturas/clientes-consultar/saida-mobile.png) |
| clientes-consultar / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/vazio-desktop.png) |
| embarcacoes-consultar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/entrada-desktop.png) |
| embarcacoes-consultar / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-consultar/entrada-mobile.png) |
| embarcacoes-consultar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/saida-desktop.png) |
| embarcacoes-consultar / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-consultar/saida-mobile.png) |
| embarcacoes-consultar / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/vazio-desktop.png) |
| vagas-consultar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/entrada-desktop.png) |
| vagas-consultar / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×1843 | [mobile](capturas/vagas-consultar/entrada-mobile.png) |
| vagas-consultar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/saida-desktop.png) |
| vagas-consultar / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1843 | [mobile](capturas/vagas-consultar/saida-mobile.png) |
| vagas-consultar / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/vazio-desktop.png) |
| relatorio-ocupacao / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-ocupacao/entrada-desktop.png) |
| relatorio-ocupacao / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/relatorio-ocupacao/entrada-mobile.png) |
| relatorio-ocupacao / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-ocupacao/saida-desktop.png) |
| relatorio-ocupacao / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1027 | [mobile](capturas/relatorio-ocupacao/saida-mobile.png) |
| relatorio-ocupacao / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-ocupacao/vazio-desktop.png) |
| relatorio-movimentacoes / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-movimentacoes/entrada-desktop.png) |
| relatorio-movimentacoes / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/relatorio-movimentacoes/entrada-mobile.png) |
| relatorio-movimentacoes / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-movimentacoes/saida-desktop.png) |
| relatorio-movimentacoes / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1389 | [mobile](capturas/relatorio-movimentacoes/saida-mobile.png) |
| relatorio-movimentacoes / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-movimentacoes/vazio-desktop.png) |
| relatorio-contratos / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-contratos/entrada-desktop.png) |
| relatorio-contratos / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/relatorio-contratos/entrada-mobile.png) |
| relatorio-contratos / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1488 | [desktop](capturas/relatorio-contratos/saida-desktop.png) |
| relatorio-contratos / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1840 | [mobile](capturas/relatorio-contratos/saida-mobile.png) |
| relatorio-contratos / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1488 | [desktop](capturas/relatorio-contratos/vazio-desktop.png) |
| relatorio-permanencia-vagas / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-permanencia-vagas/entrada-desktop.png) |
| relatorio-permanencia-vagas / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/relatorio-permanencia-vagas/entrada-mobile.png) |
| relatorio-permanencia-vagas / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1204 | [desktop](capturas/relatorio-permanencia-vagas/saida-desktop.png) |
| relatorio-permanencia-vagas / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1930 | [mobile](capturas/relatorio-permanencia-vagas/saida-mobile.png) |
| relatorio-permanencia-vagas / vazio | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1207 | [desktop](capturas/relatorio-permanencia-vagas/vazio-desktop.png) |
| ambiente-meteorologia / entrada-controlada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-meteorologia/entrada-controlada-desktop.png) |
| ambiente-meteorologia / saida-controlada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-meteorologia/saida-controlada-desktop.png) |
| ambiente-meteorologia / indisponivel-controlado | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-meteorologia/indisponivel-controlado-desktop.png) |
| ambiente-maritimo / entrada-controlada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-maritimo/entrada-controlada-desktop.png) |
| ambiente-maritimo / saida-controlada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-maritimo/saida-controlada-desktop.png) |
| ambiente-maritimo / indisponivel-controlado | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ambiente-maritimo/indisponivel-controlado-desktop.png) |
| checklist-concluir / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/checklist-concluir/entrada-desktop.png) |
| checklist-concluir / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/checklist-concluir/entrada-mobile.png) |
| checklist-concluir / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/checklist-concluir/saida-desktop.png) |
| checklist-concluir / saida | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/checklist-concluir/saida-mobile.png) |
| checklist-concluir / apos-recarregar | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/checklist-concluir/apos-recarregar-desktop.png) |
| clientes-cadastrar-pf / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1256 | [desktop](capturas/clientes-cadastrar-pf/entrada-desktop.png) |
| clientes-cadastrar-pf / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1366 | [mobile](capturas/clientes-cadastrar-pf/entrada-mobile.png) |
| clientes-cadastrar-pf / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-cadastrar-pf/saida-desktop.png) |
| clientes-cadastrar-pf / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1299 | [mobile](capturas/clientes-cadastrar-pf/saida-mobile.png) |
| clientes-cadastrar-pj / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1336 | [desktop](capturas/clientes-cadastrar-pj/entrada-desktop.png) |
| clientes-cadastrar-pj / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1446 | [mobile](capturas/clientes-cadastrar-pj/entrada-mobile.png) |
| clientes-cadastrar-pj / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-cadastrar-pj/saida-desktop.png) |
| clientes-cadastrar-pj / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-cadastrar-pj/saida-mobile.png) |
| clientes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1256 | [desktop](capturas/clientes-alterar/entrada-desktop.png) |
| clientes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1366 | [mobile](capturas/clientes-alterar/entrada-mobile.png) |
| clientes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-alterar/saida-desktop.png) |
| clientes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-alterar/saida-mobile.png) |
| embarcacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2190 | [desktop](capturas/embarcacoes-cadastrar/entrada-desktop.png) |
| embarcacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2765 | [mobile](capturas/embarcacoes-cadastrar/entrada-mobile.png) |
| embarcacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-cadastrar/saida-desktop.png) |
| embarcacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-cadastrar/saida-mobile.png) |
| vagas-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1425 | [desktop](capturas/vagas-cadastrar/entrada-desktop.png) |
| vagas-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1834 | [mobile](capturas/vagas-cadastrar/entrada-mobile.png) |
| vagas-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-cadastrar/saida-desktop.png) |
| vagas-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-cadastrar/saida-mobile.png) |
| localizacao-salvar / entrada | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/localizacao-salvar/entrada-desktop.png) |
| localizacao-salvar / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/localizacao-salvar/entrada-mobile.png) |
| localizacao-salvar / falhou | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/localizacao-salvar/falhou-desktop.png) |
| embarcacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×2190 | [desktop](capturas/embarcacoes-alterar/entrada-desktop.png) |
| embarcacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2744 | [mobile](capturas/embarcacoes-alterar/entrada-mobile.png) |
| embarcacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-alterar/saida-desktop.png) |
| embarcacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-alterar/saida-mobile.png) |
| vagas-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1425 | [desktop](capturas/vagas-alterar/entrada-desktop.png) |
| vagas-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1813 | [mobile](capturas/vagas-alterar/entrada-mobile.png) |
| vagas-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-alterar/saida-desktop.png) |
| vagas-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-alterar/saida-mobile.png) |
| ocupacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1288 | [desktop](capturas/ocupacoes-cadastrar/entrada-desktop.png) |
| ocupacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1692 | [mobile](capturas/ocupacoes-cadastrar/entrada-mobile.png) |
| ocupacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-cadastrar/saida-desktop.png) |
| ocupacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2753 | [mobile](capturas/ocupacoes-cadastrar/saida-mobile.png) |
| ocupacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1288 | [desktop](capturas/ocupacoes-alterar/entrada-desktop.png) |
| ocupacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1671 | [mobile](capturas/ocupacoes-alterar/entrada-mobile.png) |
| ocupacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-alterar/saida-desktop.png) |
| ocupacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2753 | [mobile](capturas/ocupacoes-alterar/saida-mobile.png) |
| ocupacoes-conflito / opcoes-bloqueadas | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-conflito/opcoes-bloqueadas-desktop.png) |
| contratos-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1375 | [desktop](capturas/contratos-cadastrar/entrada-desktop.png) |
| contratos-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1745 | [mobile](capturas/contratos-cadastrar/entrada-mobile.png) |
| contratos-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-cadastrar/saida-desktop.png) |
| contratos-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1721 | [mobile](capturas/contratos-cadastrar/saida-mobile.png) |
| contratos-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1375 | [desktop](capturas/contratos-alterar/entrada-desktop.png) |
| contratos-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1745 | [mobile](capturas/contratos-alterar/entrada-mobile.png) |
| contratos-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-alterar/saida-desktop.png) |
| contratos-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1830 | [mobile](capturas/contratos-alterar/saida-mobile.png) |
| contratos-enviar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-enviar/confirmacao-desktop.png) |
| contratos-enviar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1830 | [mobile](capturas/contratos-enviar/confirmacao-mobile.png) |
| contratos-enviar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1274 | [desktop](capturas/contratos-enviar/saida-desktop.png) |
| contratos-enviar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1936 | [mobile](capturas/contratos-enviar/saida-mobile.png) |
| contratos-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1274 | [desktop](capturas/contratos-ativar/confirmacao-desktop.png) |
| contratos-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1936 | [mobile](capturas/contratos-ativar/confirmacao-mobile.png) |
| contratos-ativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1409 | [desktop](capturas/contratos-ativar/saida-desktop.png) |
| contratos-ativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2213 | [mobile](capturas/contratos-ativar/saida-mobile.png) |
| contratos-vincular / falhou | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1409 | [desktop](capturas/contratos-vincular/falhou-desktop.png) |
| contratos-desvincular / falhou | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1409 | [desktop](capturas/contratos-desvincular/falhou-desktop.png) |
| contratos-suspender / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1409 | [desktop](capturas/contratos-suspender/confirmacao-desktop.png) |
| contratos-suspender / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2213 | [mobile](capturas/contratos-suspender/confirmacao-mobile.png) |
| contratos-suspender / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1501 | [desktop](capturas/contratos-suspender/saida-desktop.png) |
| contratos-suspender / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2171 | [mobile](capturas/contratos-suspender/saida-mobile.png) |
| contratos-reativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1501 | [desktop](capturas/contratos-reativar/confirmacao-desktop.png) |
| contratos-reativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2171 | [mobile](capturas/contratos-reativar/confirmacao-mobile.png) |
| contratos-reativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1636 | [desktop](capturas/contratos-reativar/saida-desktop.png) |
| contratos-reativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2480 | [mobile](capturas/contratos-reativar/saida-mobile.png) |
| contratos-solicitar-encerramento / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1636 | [desktop](capturas/contratos-solicitar-encerramento/confirmacao-desktop.png) |
| contratos-solicitar-encerramento / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2480 | [mobile](capturas/contratos-solicitar-encerramento/confirmacao-mobile.png) |
| contratos-solicitar-encerramento / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1728 | [desktop](capturas/contratos-solicitar-encerramento/saida-desktop.png) |
| contratos-solicitar-encerramento / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2416 | [mobile](capturas/contratos-solicitar-encerramento/saida-mobile.png) |
| contratos-encerrar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1728 | [desktop](capturas/contratos-encerrar/confirmacao-desktop.png) |
| contratos-encerrar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2416 | [mobile](capturas/contratos-encerrar/confirmacao-mobile.png) |
| contratos-encerrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1855 | [desktop](capturas/contratos-encerrar/saida-desktop.png) |
| contratos-encerrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2531 | [mobile](capturas/contratos-encerrar/saida-mobile.png) |
| contratos-cancelar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-cancelar/confirmacao-desktop.png) |
| contratos-cancelar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1721 | [mobile](capturas/contratos-cancelar/confirmacao-mobile.png) |
| contratos-cancelar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-cancelar/saida-desktop.png) |
| contratos-cancelar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1696 | [mobile](capturas/contratos-cancelar/saida-mobile.png) |
| ocupacoes-encerrar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-encerrar/confirmacao-desktop.png) |
| ocupacoes-encerrar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2753 | [mobile](capturas/ocupacoes-encerrar/confirmacao-mobile.png) |
| ocupacoes-encerrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-encerrar/saida-desktop.png) |
| ocupacoes-encerrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2678 | [mobile](capturas/ocupacoes-encerrar/saida-mobile.png) |
| movimentacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1529 | [desktop](capturas/movimentacoes-cadastrar/entrada-desktop.png) |
| movimentacoes-cadastrar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/movimentacoes-cadastrar/entrada-mobile.png) |
| movimentacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-cadastrar/saida-desktop.png) |
| movimentacoes-cadastrar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1403 | [mobile](capturas/movimentacoes-cadastrar/saida-mobile.png) |
| movimentacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1529 | [desktop](capturas/movimentacoes-alterar/entrada-desktop.png) |
| movimentacoes-alterar / entrada | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/movimentacoes-alterar/entrada-mobile.png) |
| movimentacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-alterar/saida-desktop.png) |
| movimentacoes-alterar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1403 | [mobile](capturas/movimentacoes-alterar/saida-mobile.png) |
| movimentacoes-iniciar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-iniciar/confirmacao-desktop.png) |
| movimentacoes-iniciar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1403 | [mobile](capturas/movimentacoes-iniciar/confirmacao-mobile.png) |
| movimentacoes-iniciar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-iniciar/saida-desktop.png) |
| movimentacoes-iniciar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1403 | [mobile](capturas/movimentacoes-iniciar/saida-mobile.png) |
| movimentacoes-concluir / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-concluir/confirmacao-desktop.png) |
| movimentacoes-concluir / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1403 | [mobile](capturas/movimentacoes-concluir/confirmacao-mobile.png) |
| movimentacoes-concluir / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-concluir/saida-desktop.png) |
| movimentacoes-concluir / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1328 | [mobile](capturas/movimentacoes-concluir/saida-mobile.png) |
| movimentacoes-cancelar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-cancelar/confirmacao-desktop.png) |
| movimentacoes-cancelar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1751 | [mobile](capturas/movimentacoes-cancelar/confirmacao-mobile.png) |
| movimentacoes-cancelar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-cancelar/saida-desktop.png) |
| movimentacoes-cancelar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1676 | [mobile](capturas/movimentacoes-cancelar/saida-mobile.png) |
| clientes-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-inativar/confirmacao-desktop.png) |
| clientes-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-inativar/confirmacao-mobile.png) |
| clientes-inativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-inativar/saida-desktop.png) |
| clientes-inativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-inativar/saida-mobile.png) |
| clientes-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-ativar/confirmacao-desktop.png) |
| clientes-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-ativar/confirmacao-mobile.png) |
| clientes-ativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-ativar/saida-desktop.png) |
| clientes-ativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1585 | [mobile](capturas/clientes-ativar/saida-mobile.png) |
| embarcacoes-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-inativar/confirmacao-desktop.png) |
| embarcacoes-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-inativar/confirmacao-mobile.png) |
| embarcacoes-inativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-inativar/saida-desktop.png) |
| embarcacoes-inativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-inativar/saida-mobile.png) |
| embarcacoes-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-ativar/confirmacao-desktop.png) |
| embarcacoes-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-ativar/confirmacao-mobile.png) |
| embarcacoes-ativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-ativar/saida-desktop.png) |
| embarcacoes-ativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-ativar/saida-mobile.png) |
| vagas-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-inativar/confirmacao-desktop.png) |
| vagas-inativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-inativar/confirmacao-mobile.png) |
| vagas-inativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-inativar/saida-desktop.png) |
| vagas-inativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-inativar/saida-mobile.png) |
| vagas-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-ativar/confirmacao-desktop.png) |
| vagas-ativar / confirmacao | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-ativar/confirmacao-mobile.png) |
| vagas-ativar / saida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-ativar/saida-desktop.png) |
| vagas-ativar / saida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1843 | [mobile](capturas/vagas-ativar/saida-mobile.png) |
| relatorio-movimentacoes / entrada-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-movimentacoes/entrada-preenchida-desktop.png) |
| relatorio-movimentacoes / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-movimentacoes/saida-preenchida-desktop.png) |
| relatorio-movimentacoes / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1461 | [mobile](capturas/relatorio-movimentacoes/saida-preenchida-mobile.png) |
| relatorio-contratos / entrada-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/relatorio-contratos/entrada-preenchida-desktop.png) |
| relatorio-contratos / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1488 | [desktop](capturas/relatorio-contratos/saida-preenchida-desktop.png) |
| relatorio-contratos / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×1840 | [mobile](capturas/relatorio-contratos/saida-preenchida-mobile.png) |
| dashboard / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1397 | [desktop](capturas/dashboard/saida-preenchida-desktop.png) |
| dashboard / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×2760 | [mobile](capturas/dashboard/saida-preenchida-mobile.png) |
| painel-tv / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1100 | [desktop](capturas/painel-tv/saida-preenchida-desktop.png) |
| painel-tv / saida-preenchida | ADMINISTRADOR_PLATAFORMA | 390×844 → 390×844 | [mobile](capturas/painel-tv/saida-preenchida-mobile.png) |
| dashboard / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×2760 | [mobile](capturas/dashboard/inicial-mobile.png) |
| painel-tv / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×844 | [mobile](capturas/painel-tv/inicial-mobile.png) |
| clientes-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1585 | [mobile](capturas/clientes-consultar/inicial-mobile.png) |
| clientes-cadastrar-pf / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1366 | [mobile](capturas/clientes-cadastrar-pf/inicial-mobile.png) |
| clientes-cadastrar-pf / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×1366 | [mobile](capturas/clientes-cadastrar-pf/validacao-obrigatorios-mobile.png) |
| clientes-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1366 | [mobile](capturas/clientes-alterar/inicial-mobile.png) |
| embarcacoes-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-consultar/inicial-mobile.png) |
| embarcacoes-cadastrar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×2765 | [mobile](capturas/embarcacoes-cadastrar/inicial-mobile.png) |
| embarcacoes-cadastrar / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×2765 | [mobile](capturas/embarcacoes-cadastrar/validacao-obrigatorios-mobile.png) |
| embarcacoes-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×2744 | [mobile](capturas/embarcacoes-alterar/inicial-mobile.png) |
| vagas-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1843 | [mobile](capturas/vagas-consultar/inicial-mobile.png) |
| vagas-cadastrar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1834 | [mobile](capturas/vagas-cadastrar/inicial-mobile.png) |
| vagas-cadastrar / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×1834 | [mobile](capturas/vagas-cadastrar/validacao-obrigatorios-mobile.png) |
| vagas-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1813 | [mobile](capturas/vagas-alterar/inicial-mobile.png) |
| ocupacoes-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×2678 | [mobile](capturas/ocupacoes-consultar/inicial-mobile.png) |
| ocupacoes-cadastrar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1300 | [mobile](capturas/ocupacoes-cadastrar/inicial-mobile.png) |
| ocupacoes-cadastrar / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×1300 | [mobile](capturas/ocupacoes-cadastrar/validacao-obrigatorios-mobile.png) |
| movimentacoes-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1657 | [mobile](capturas/movimentacoes-consultar/inicial-mobile.png) |
| movimentacoes-cadastrar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1691 | [mobile](capturas/movimentacoes-cadastrar/inicial-mobile.png) |
| movimentacoes-cadastrar / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×1779 | [mobile](capturas/movimentacoes-cadastrar/validacao-obrigatorios-mobile.png) |
| contratos-consultar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1558 | [mobile](capturas/contratos-consultar/inicial-mobile.png) |
| contratos-cadastrar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1745 | [mobile](capturas/contratos-cadastrar/inicial-mobile.png) |
| contratos-cadastrar / validacao-obrigatorios | ADMINISTRADOR_MARINA | 390×844 → 390×1745 | [mobile](capturas/contratos-cadastrar/validacao-obrigatorios-mobile.png) |
| checklist / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/checklist/inicial-mobile.png) |
| localizacao / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/localizacao/inicial-mobile.png) |
| variabilidade / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×4052 | [mobile](capturas/variabilidade/inicial-mobile.png) |
| mares-desabilitada / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×974 | [mobile](capturas/mares-desabilitada/inicial-mobile.png) |
| ocupacoes-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1691 | [mobile](capturas/ocupacoes-alterar/inicial-mobile.png) |
| movimentacoes-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1931 | [mobile](capturas/movimentacoes-alterar/inicial-mobile.png) |
| contratos-detalhe / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1696 | [mobile](capturas/contratos-detalhe/inicial-mobile.png) |
| contratos-alterar / inicial | ADMINISTRADOR_MARINA | 390×844 → 390×1827 | [mobile](capturas/contratos-alterar/inicial-mobile.png) |
| clientes-consultar / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1585 | [mobile](capturas/clientes-consultar/vazio-mobile.png) |
| embarcacoes-consultar / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1454 | [mobile](capturas/embarcacoes-consultar/vazio-mobile.png) |
| vagas-consultar / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1843 | [mobile](capturas/vagas-consultar/vazio-mobile.png) |
| relatorio-ocupacao / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×982 | [mobile](capturas/relatorio-ocupacao/vazio-mobile.png) |
| relatorio-movimentacoes / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1389 | [mobile](capturas/relatorio-movimentacoes/vazio-mobile.png) |
| relatorio-contratos / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1840 | [mobile](capturas/relatorio-contratos/vazio-mobile.png) |
| relatorio-permanencia-vagas / vazio | ADMINISTRADOR_MARINA | 390×844 → 390×1953 | [mobile](capturas/relatorio-permanencia-vagas/vazio-mobile.png) |
| ambiente-meteorologia / entrada-controlada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-meteorologia/entrada-controlada-mobile.png) |
| ambiente-meteorologia / saida-controlada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-meteorologia/saida-controlada-mobile.png) |
| ambiente-meteorologia / indisponivel-controlado | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-meteorologia/indisponivel-controlado-mobile.png) |
| ambiente-maritimo / entrada-controlada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-maritimo/entrada-controlada-mobile.png) |
| ambiente-maritimo / saida-controlada | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-maritimo/saida-controlada-mobile.png) |
| ambiente-maritimo / indisponivel-controlado | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/ambiente-maritimo/indisponivel-controlado-mobile.png) |
| checklist-concluir / apos-recarregar | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/checklist-concluir/apos-recarregar-mobile.png) |
| perfil-administrador_plataforma / menu | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-administrador_plataforma/menu-desktop.png) |
| perfil-administrador_marina / menu | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-administrador_marina/menu-desktop.png) |
| perfil-gerente / menu | GERENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-gerente/menu-desktop.png) |
| perfil-atendente / menu | ATENDENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-atendente/menu-desktop.png) |
| perfil-financeiro / menu | FINANCEIRO | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-financeiro/menu-desktop.png) |
| localizacao-salvar / saida | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/localizacao-salvar/saida-desktop.png) |
| localizacao-salvar / saida | ADMINISTRADOR_MARINA | 390×844 → 390×944 | [mobile](capturas/localizacao-salvar/saida-mobile.png) |
| contratos-vincular / entrada | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1320 | [desktop](capturas/contratos-vincular/entrada-desktop.png) |
| contratos-vincular / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×2104 | [mobile](capturas/contratos-vincular/entrada-mobile.png) |
| contratos-vincular / saida | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1387 | [desktop](capturas/contratos-vincular/saida-desktop.png) |
| contratos-vincular / saida | ADMINISTRADOR_MARINA | 390×844 → 390×2039 | [mobile](capturas/contratos-vincular/saida-mobile.png) |
| contratos-desvincular / confirmacao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1387 | [desktop](capturas/contratos-desvincular/confirmacao-desktop.png) |
| contratos-desvincular / confirmacao | ADMINISTRADOR_MARINA | 390×844 → 390×2039 | [mobile](capturas/contratos-desvincular/confirmacao-mobile.png) |
| contratos-desvincular / motivo-obrigatorio | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1387 | [desktop](capturas/contratos-desvincular/motivo-obrigatorio-desktop.png) |
| contratos-desvincular / motivo-obrigatorio | ADMINISTRADOR_MARINA | 390×844 → 390×2039 | [mobile](capturas/contratos-desvincular/motivo-obrigatorio-mobile.png) |
| contratos-desvincular / saida | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1499 | [desktop](capturas/contratos-desvincular/saida-desktop.png) |
| contratos-desvincular / saida | ADMINISTRADOR_MARINA | 390×844 → 390×2323 | [mobile](capturas/contratos-desvincular/saida-mobile.png) |
| permissoes-salvar / entrada | ADMINISTRADOR_MARINA | 1440×1100 → 1440×2234 | [desktop](capturas/permissoes-salvar/entrada-desktop.png) |
| permissoes-salvar / entrada | ADMINISTRADOR_MARINA | 390×844 → 390×4052 | [mobile](capturas/permissoes-salvar/entrada-mobile.png) |
| permissoes-salvar / saida | ADMINISTRADOR_MARINA | 1440×1100 → 1440×2298 | [desktop](capturas/permissoes-salvar/saida-desktop.png) |
| permissoes-salvar / saida | ADMINISTRADOR_MARINA | 390×844 → 390×4116 | [mobile](capturas/permissoes-salvar/saida-mobile.png) |
| movimentacoes-transferencia / antes | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-transferencia/antes-desktop.png) |
| dashboard / transferencia-agendada | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1545 | [desktop](capturas/dashboard/transferencia-agendada-desktop.png) |
| painel-tv / transferencia-agendada | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1100 | [desktop](capturas/painel-tv/transferencia-agendada-desktop.png) |
| movimentacoes-transferencia / iniciar-confirmacao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-transferencia/iniciar-confirmacao-desktop.png) |
| movimentacoes-transferencia / concluir-confirmacao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-transferencia/concluir-confirmacao-desktop.png) |
| movimentacoes-transferencia / saida | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-transferencia/saida-desktop.png) |
| movimentacoes-transferencia / saida | ADMINISTRADOR_MARINA | 390×844 → 390×1309 | [mobile](capturas/movimentacoes-transferencia/saida-mobile.png) |
| movimentacoes-transferencia / ocupacoes-apos-conclusao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-transferencia/ocupacoes-apos-conclusao-desktop.png) |
| sessao / identidade | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/sessao/identidade-desktop.png) |
| sessao / falhou | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/sessao/falhou-desktop.png) |
| perfil-administrador_plataforma / clientes-acesso | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_plataforma/clientes-acesso-desktop.png) |
| perfil-administrador_plataforma / contratos-acesso | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1499 | [desktop](capturas/perfil-administrador_plataforma/contratos-acesso-desktop.png) |
| perfil-administrador_plataforma / relatorios-acesso | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_plataforma/relatorios-acesso-desktop.png) |
| perfil-administrador_plataforma / referencias-acesso | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_plataforma/referencias-acesso-desktop.png) |
| perfil-administrador_plataforma / configuracoes-acesso | ADMINISTRADOR_PLATAFORMA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_plataforma/configuracoes-acesso-desktop.png) |
| perfil-administrador_marina / clientes-acesso | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_marina/clientes-acesso-desktop.png) |
| perfil-administrador_marina / contratos-acesso | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1499 | [desktop](capturas/perfil-administrador_marina/contratos-acesso-desktop.png) |
| perfil-administrador_marina / relatorios-acesso | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_marina/relatorios-acesso-desktop.png) |
| perfil-administrador_marina / referencias-acesso | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-administrador_marina/referencias-acesso-desktop.png) |
| perfil-administrador_marina / configuracoes-acesso | ADMINISTRADOR_MARINA | 1440×1100 → 1440×2234 | [desktop](capturas/perfil-administrador_marina/configuracoes-acesso-desktop.png) |
| perfil-gerente / clientes-acesso | GERENTE | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-gerente/clientes-acesso-desktop.png) |
| perfil-gerente / contratos-acesso | GERENTE | 1440×1100 → 1440×1499 | [desktop](capturas/perfil-gerente/contratos-acesso-desktop.png) |
| perfil-gerente / relatorios-acesso | GERENTE | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-gerente/relatorios-acesso-desktop.png) |
| perfil-gerente / referencias-acesso | GERENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-gerente/referencias-acesso-desktop.png) |
| perfil-gerente / configuracoes-acesso | GERENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-gerente/configuracoes-acesso-desktop.png) |
| permissao-interna-negada / gerente | GERENTE | 1440×1100 → 1440×1397 | [desktop](capturas/permissao-interna-negada/gerente-desktop.png) |
| perfil-atendente / clientes-acesso | ATENDENTE | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-atendente/clientes-acesso-desktop.png) |
| perfil-atendente / contratos-acesso | ATENDENTE | 1440×1100 → 1440×1499 | [desktop](capturas/perfil-atendente/contratos-acesso-desktop.png) |
| perfil-atendente / relatorios-acesso | ATENDENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-atendente/relatorios-acesso-desktop.png) |
| perfil-atendente / referencias-acesso | ATENDENTE | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-atendente/referencias-acesso-desktop.png) |
| perfil-atendente / configuracoes-acesso | ATENDENTE | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-atendente/configuracoes-acesso-desktop.png) |
| contrato-acao-sem-permissao / erro-backend | ATENDENTE | 1440×1100 → 1440×1499 | [desktop](capturas/contrato-acao-sem-permissao/erro-backend-desktop.png) |
| perfil-financeiro / clientes-acesso | FINANCEIRO | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-financeiro/clientes-acesso-desktop.png) |
| perfil-financeiro / contratos-acesso | FINANCEIRO | 1440×1100 → 1440×1499 | [desktop](capturas/perfil-financeiro/contratos-acesso-desktop.png) |
| perfil-financeiro / relatorios-acesso | FINANCEIRO | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-financeiro/relatorios-acesso-desktop.png) |
| perfil-financeiro / referencias-acesso | FINANCEIRO | 1440×1100 → 1440×1200 | [desktop](capturas/perfil-financeiro/referencias-acesso-desktop.png) |
| perfil-financeiro / configuracoes-acesso | FINANCEIRO | 1440×1100 → 1440×1397 | [desktop](capturas/perfil-financeiro/configuracoes-acesso-desktop.png) |
| sessao / identidade | ADMINISTRADOR_MARINA | 390×844 → 390×2706 | [mobile](capturas/sessao/identidade-mobile.png) |
| sessao / menu-aberto | ADMINISTRADOR_MARINA | 390×844 → 390×2706 | [mobile](capturas/sessao/menu-aberto-mobile.png) |
| sessao / apos-sair | ADMINISTRADOR_MARINA | 390×844 → 390×844 | [mobile](capturas/sessao/apos-sair-mobile.png) |
| clientes-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/filtros-selecao-desktop.png) |
| clientes-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/clientes-consultar/apos-limpar-desktop.png) |
| embarcacoes-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/filtros-selecao-desktop.png) |
| embarcacoes-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/embarcacoes-consultar/apos-limpar-desktop.png) |
| vagas-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/filtros-selecao-desktop.png) |
| vagas-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/vagas-consultar/apos-limpar-desktop.png) |
| ocupacoes-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-consultar/filtros-selecao-desktop.png) |
| ocupacoes-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/ocupacoes-consultar/apos-limpar-desktop.png) |
| movimentacoes-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-consultar/filtros-selecao-desktop.png) |
| movimentacoes-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/movimentacoes-consultar/apos-limpar-desktop.png) |
| contratos-consultar / filtros-selecao | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-consultar/filtros-selecao-desktop.png) |
| contratos-consultar / apos-limpar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1200 | [desktop](capturas/contratos-consultar/apos-limpar-desktop.png) |
| dashboard / apos-atualizar | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1396 | [desktop](capturas/dashboard/apos-atualizar-desktop.png) |
| painel-tv / tela-cheia | ADMINISTRADOR_MARINA | 1440×1100 → 1440×1100 | [desktop](capturas/painel-tv/tela-cheia-desktop.png) |

## Reutilização identificada

61 imagens anteriores: 15 em [SE06](../se06/README.md), 46 em [AIE03–AIE06](../apf-aie/README.md). São artefatos herdados, presentes no SHA inspecionado. As pastas antigas não registram inequivocamente o SHA de captura original; não atribuir o SHA atual como data de criação dessas imagens. A adequação é sustentada pelos scripts/fontes presentes e pela ausência de alterações de produção nesta auditoria. Essa limitação de proveniência fica registrada.

SE06: resultado/vazio com API e banco reais de teste; loading/erro induzidos. AIE: telas com upstream controlado; consulta real IBGE/BrasilAPI em consultas-reais.json, separada dos screenshots; sondagem real posterior indisponível preservada. WorldTides nunca consultada de verdade. Não recriamos essas imagens e não afirmamos consulta real Open-Meteo.

Novas capturas indexadas: 335. Processos/estados candidatos no inventário: 72. Linhas de cobertura (inclui bindings e redirecionamentos, não quantidade de funções APF): 197.

## Testes/build

Ver resultado-validacao.json, produzido a partir dos relatórios efetivos. Ausência de execução não equivale a aprovação. A classificação, agrupamentos e contagem acadêmica dependem da equipe e dos slides não fornecidos.

## Conferência final

31 rotas de tela declaradas possuem captura atual ou evidência anterior reutilizada, conforme [mapa das rotas](rotas-declaradas.json). Isso não equivale à aprovação de cada ação: no CSV de cobertura há 151 linhas cobertas, 20 controles sem execução individual comprovada, 15 não aplicáveis ao cenário e 11 operações sem tela. Os estados e motivos estão explícitos. São 72 objetivos/operações candidatos inventariados, sem contagem APF.

A regressão normal executou 471 casos: **466 aprovados, zero falhas/erros, cinco opt-ins ignorados**. A coleta visual foi executada separadamente em quatro passagens concluídas, com quatro tentativas de coleta inicialmente incompletas depois resolvidas e preservadas no manifesto. Frontend: **147 testes em 32 arquivos aprovados**. Build de produção aprovada, com os avisos preexistentes de orçamento. [Resultado detalhado](resultado-validacao.json), [proveniência dos instrumentos/build](proveniencia.json).

Foram examinadas visualmente amostras de formulários, relatórios, validação de motivo, vínculo/desvínculo, transferência, coordenadas e restrições de perfil. A inspeção identificou corte dos controles de filtro à direita em movimentações desktop e baixo contraste aparente no menu lateral; tabelas dos relatórios móveis requerem rolagem horizontal. As capturas completas permanecem disponíveis para revisão pela equipe. Nenhuma declaração de conformidade WCAG ou cobertura exaustiva de combinações é feita.
