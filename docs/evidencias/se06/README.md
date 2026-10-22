# SE06 — demonstração reproduzível e evidências visuais

O relatório usa a build de produção Angular e a API Spring Boot real, autenticada por login/JWT. O PostgreSQL é criado e destruído pelo Testcontainers. A única fixture está em `src/test/java/br/com/caisora/relatorio/suporte/PermanenciaFixture.java`; não há seed, migration de dados ou perfil de produção que carregue esses registros.

## Reprodução

Requer Java 21, Node/npm, Docker e bibliotecas de sistema para Chromium. Playwright é uma ferramenta temporária de validação, instalada fora do repositório; não é dependência da aplicação nem altera package.json/package-lock.json do frontend.

```bash
npm install --prefix /tmp/caisora-browser-tools --no-save --package-lock=false playwright@1.64.1
PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers /tmp/caisora-browser-tools/node_modules/.bin/playwright install chromium
npm --prefix frontend run build
CAISORA_VISUAL=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  ./mvnw -Dtest=PermanenciaVagasVisualTest test
```

`JAVA_HOME` deve apontar ao JDK 21 disponível na máquina. `CAISORA_BROWSER_TOOLS` permite trocar o diretório do Playwright; `CAISORA_VISUAL_OUTPUT` permite trocar o diretório de capturas. Sem `CAISORA_VISUAL=true`, o teste de demonstração é ignorado na suíte comum. Seu processo tem timeout e encerra o navegador/servidor local ao finalizar. O script recusa uma API fora de `127.0.0.1` e é iniciado pelo teste com a porta aleatória do servidor isolado.

## Dados de demonstração e resultados esperados

A fixture cria marinas A/B, cliente/embarcação e vagas Norte/SECA e Sul/MOLHADA. A conta `demo-se06@example.com` e senha `Demo-SE06-2026` existem apenas no container descartável de demonstração e não concedem acesso a nenhuma marina real. O módulo RELATORIOS é liberado apenas nessa organização de testes.

Filtro em todas as capturas de resultado: **10/10/2026 a 10/10/2026, setor Norte, tipo SECA**.

| Ocupação selecionada | Início UTC | Encerramento UTC | Duração |
|---|---|---|---|
| Norte/SECA 1 | 09/10/2026 23:00 | 10/10/2026 00:00 | 3600 s |
| Norte/SECA 2 | 10/10/2026 20:59:59.999999 | 10/10/2026 23:59:59.999999 | 10800 s |

Resultado: **2 encerradas, média 7200 s, mínimo 3600 s, máximo 10800 s, soma 4 horas**. Sem setor/tipo: 3 encerradas, soma 6 horas. A marina B tem uma ocupação de 10 horas que não participa do resultado da A. A fixture inclui ocupação ativa, previsões distintas do encerramento, registros antes/depois do período e vaga hoje inativa, para demonstrar a seleção correta.

Estado vazio: **01/11/2026 a 01/11/2026, Norte/SECA**, quantidade 0, horas 0, média/mínimo/máximo “Sem dados” e nenhum grupo. Esses resultados são calculados pelo backend; não são respostas estáticas no script.

## Capturas

| Estado | Desktop | Mobile |
|---|---|---|
| Filtros preenchidos e consulta concluída | [desktop](desktop-resultados.png) | [mobile](mobile-resultados.png) |
| Painel completo de indicadores/resultados | [desktop](desktop-resultados-painel.png) | [mobile](mobile-resultados-painel.png) |
| Filtros para período vazio | [desktop](desktop-vazio.png) | [mobile](mobile-vazio.png) |
| Painel completo de estado vazio | [desktop](desktop-vazio-painel.png) | [mobile](mobile-vazio-painel.png) |
| Carregamento | [desktop](desktop-loading.png) | [mobile](mobile-loading.png) |
| Erro induzido | [desktop](desktop-erro.png) | [mobile](mobile-erro.png) |
| Mensagem de erro completa | [desktop](desktop-erro-mensagem.png) | [mobile](mobile-erro-mensagem.png) |
| Tabela após rolagem horizontal | — | [mobile](mobile-tabela-rolada.png) |

[`verificacao.json`](verificacao.json) registra a versão do navegador, data, viewports e os cenários concluídos. O script verifica os cinco indicadores numéricos da API, texto renderizado, quantidade exibida, ausência de erros JavaScript, ausência de overflow da página/conteúdo e presença de rolagem na tabela mobile. As imagens foram inspecionadas visualmente para revisar legibilidade e disposição dos elementos. Foram aprovadas 15 capturas. Na execução final completa (`CAISORA_VISUAL=true ./mvnw test` com as demais variáveis acima), os 441 testes do backend passaram, sem ignorados; os 136 testes frontend e a build de produção também passaram.

## Limitações

- Mobile validado por viewport/touch em Chromium headless real (390×844), não por aparelho físico. Desktop: 1440×1100. Não houve cobertura de Safari, Firefox ou leitor de tela.
- No mobile, os filtros e o painel excedem a altura de uma tela. São fornecidas capturas do formulário e do painel completo (para estas, amplia-se somente a altura do viewport, preservando a largura mobile de 390 px), além da tabela rolada no viewport 390×844; a página permanece com rolagem vertical normal e a tabela tem rolagem horizontal própria.
- Loading e erro são reproduzidos com interceptação de teste no navegador; resultados e estados vazios usam API e banco reais. Esses controles não fazem parte da aplicação de produção.
- As capturas preservam a formatação numérica atual do Caisora (`7,200.00` representa 7200 segundos). O controle de data nativo do Chromium pode apresentar mês/dia/ano; as consultas do cenário vazio enviam explicitamente `inicio=2026-11-01&fim=2026-11-01`, independente da apresentação local.
- O agrupamento usa setor/tipo atuais da vaga, pois não há snapshots desses atributos. Horas totais somam permanências por ocupação, não horas de calendário da marina.
- O build depende do download das fontes Google já usadas pelo projeto e mantém os avisos de orçamento do bundle/estilo do dashboard. Nenhuma nova integração ambiental ou AIE foi adicionada.
