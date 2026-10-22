# AIE03–AIE06 — evidências de consulta e interface

As evidências estão separadas em duas categorias:

1. **Consulta real de cada integração pública habilitada:** [`consultas-reais.json`](consultas-reais.json), produzido por `ReferenciasReaisTest` com os clientes Java do Caisora e HTTPS real. Contém endpoint, horário, quantidade e amostra dos campos efetivamente usados.
2. **Inspeção visual reproduzível:** Chromium real abre a build Angular de produção, usa login/JWT e consulta a API Spring Boot com PostgreSQL descartável. Os provedores upstream são servidores HTTP locais controlados. O cenário WorldTides também é exclusivamente controlado: não há conta, chave real ou chamada paga. A interface padrão de marés desabilitada tem suas próprias capturas.

As respostas controladas ficam em `src/test/resources/referencias`; não são carregadas pela aplicação de produção. Os arquivos de evidência desta pasta não são fallback funcional, cache nem tabelas de catálogo. Não houve implantação, contratação ou persistência de cadastros externos em produção.

## Reprodução

Requer Java 21, Node/npm, Docker e bibliotecas de sistema do Chromium. Playwright é instalado temporariamente fora do projeto, sem mudar dependências/package-lock do Caisora.

```bash
npm install --prefix /tmp/caisora-browser-tools --no-save --package-lock=false playwright@1.64.1
PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers /tmp/caisora-browser-tools/node_modules/.bin/playwright install chromium
npm --prefix frontend run build
npm --prefix frontend test -- --watch=false

# Respostas controladas; não consulta serviços públicos nem WorldTides.
CAISORA_AIE_VISUAL=true PLAYWRIGHT_BROWSERS_PATH=/tmp/caisora-browsers \
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  ./mvnw -Dtest=ReferenciasVisualTest,MaresControladasVisualTest test

# Opt-in: três consultas reais pontuais, somente IBGE e BrasilAPI.
CAISORA_AIE_REAL=true JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  ./mvnw -Dtest=ReferenciasReaisTest test
```

Ajustar JAVA_HOME ao JDK 21 instalado. `CAISORA_BROWSER_TOOLS` permite indicar outro diretório Playwright; `CAISORA_AIE_OUTPUT` altera a pasta de capturas. A suíte normal não chama provedores reais: testes real/visual ficam ignorados quando suas variáveis não estão habilitadas. As fixtures visuais são criadas apenas pelo teste em um container PostgreSQL descartável e são removidas com esse ambiente. Não há seed ou endpoint de demonstração em produção.

A conta de teste `demo-aie@example.com`/`Demo-AIE-2026`, marina `aie-a`, existe somente nesse container. Outra marina possui coordenadas diferentes para testar isolamento. Uma embarcação tem bandeira PT; consultar o Brasil preserva esse valor, assim como as quantidades de clientes, embarcações, vagas, ocupações e contratos.

## Consulta real registrada

| Grupo | Consulta | Resultado observado |
|---|---|---|
| AIE03 | IBGE `/api/v1/paises/-` | 193 países; Brasil, M49 76, BR e BRA |
| AIE04 | IBGE `/api/v1/localidades/estados/SC/municipios` | 295 municípios; Florianópolis 4205407, UF 42/SC/Santa Catarina |
| AIE05 | BrasilAPI `/api/feriados/v1/2026?uf=SC` | 15 itens; inclui national e state/SC, preservando campos ausentes |
| AIE06 | WorldTides | **Não executada**: falta de credenciais e autorização; produção permanece desabilitada |

Esses números descrevem a resposta observada na data registrada, não uma garantia de tamanho permanente. Durante tentativas adicionais de inspeção ao vivo, as consultas ao IBGE neste ambiente apresentaram erro tratado de indisponibilidade na API Caisora. A [sondagem posterior](sondagem-real-posterior.json) registra a falha observada, preservando a evidência anterior de sucesso. Por isso as chamadas reais ficam documentadas separadamente e as capturas utilizam respostas controladas para reprodução. Não há fallback para dados simulados na aplicação.

## Capturas desktop e mobile

Todos os grupos foram inspecionados em Chromium headless real. Viewports base: desktop 1440×1100 e mobile 390×844 com touch emulado. Para os painéis completos, somente a altura é ampliada, preservando a largura; os tamanhos constam nos arquivos `verificacao-*.json`. As tabelas mobile têm rolagem horizontal própria.

| Grupo | Resultado completo desktop | Resultado completo mobile | Vazio controlado mobile | Marés padrão |
|---|---|---|---|---|
| Países | [desktop](paises-desktop-resultado-completo.png) | [mobile](paises-mobile-resultado-completo.png) | [vazio](paises-mobile-vazio-controlado.png) | — |
| Municípios | [desktop](municipios-desktop-resultado-completo.png) | [mobile](municipios-mobile-resultado-completo.png) | [vazio](municipios-mobile-vazio-controlado.png) | — |
| Feriados | [desktop](feriados-desktop-resultado-completo.png) | [mobile](feriados-mobile-resultado-completo.png) | [vazio](feriados-mobile-vazio-controlado.png) | — |
| Marés, servidor local controlado | [desktop](mares-desktop-resultado-completo.png) | [mobile](mares-mobile-resultado-completo.png) | [vazio](mares-mobile-vazio-controlado.png) | [desabilitada desktop](mares-desktop-desabilitada.png), [mobile](mares-mobile-desabilitada.png) |

Cada grupo também tem imagens `*-loading.png`, `*-erro-controlado.png`, `*-resultado.png` no viewport base e `*-mobile-tabela-rolada.png` após rolagem. Os roteiros verificam ausência de overflow da página/conteúdo e erros JavaScript, HTTP 401 sem autenticação, identificação/código dos registros, tipos dos feriados, campos de marés e bloqueio do formulário quando não autorizado.

Fixtures usadas nas capturas:

- Países: Brasil e Argentina; busca “Brasil” mostra apenas Brasil/BR/BRA/76.
- Municípios: Florianópolis e Joinville/SC com as duas estruturas de UF; busca “Florianópolis” mostra 4205407/42/SC/Santa Catarina.
- Feriados: 2026/SC, registro national com weekday e registro state/SC sem weekday, exibido sem dia fabricado. A classificação aparece conforme o provedor, acompanhada do aviso sobre calendário oficial/datas comemorativas.
- Marés controladas: High/Low, 1.2 m e −0.3 m, referencial MSL e copyright que diz **RESPOSTA CONTROLADA EXCLUSIVA DE TESTE — WorldTides não consultada**. A unidade m segue o contrato da API v3. Esses valores não descrevem as condições reais de nenhuma marina.

A leitura de respostas para auditoria usa o proxy local de testes, evitando depender da disponibilidade de corpos no protocolo DevTools após navegações. Não modifica o tráfego nem a autenticação da aplicação. Os cenários vazio/loading/erro são induzidos pelo roteiro de navegador; não existem nos arquivos de produção.

## Limitações e termos

- Mobile usa emulação de viewport/touch, não aparelho físico. Não houve inspeção em Safari/Firefox ou leitor de tela.
- Campos date e números preservam a formatação atual do navegador/aplicação; os parâmetros efetivos são ISO e constam nos testes.
- A WorldTides segue sem consulta real; sua validação real depende das credenciais, autorização contratual, créditos e finalidade permitida. O teste jamais usa seu host público. O aviso de segurança e o copyright permanecem visíveis nas capturas.
- Feriados refletem a classificação e a cobertura do provedor, inclusive eventuais datas comemorativas/pontos facultativos rotulados national. Os itens estaduais são os efetivamente retornados; a UF não garante lista local completa.
- Os dois conjuntos Open-Meteo existentes não são duplicados, e estes quatro grupos não são multiplicados por integrante. Este material não atribui PF/complexidade ou resolve automaticamente a contagem acadêmica.
- O build mantém os avisos de orçamento do bundle inicial/estilo do dashboard. As fontes Google já existentes exigem rede no build; nenhuma nova dependência de produção foi adicionada.

## Resultado da validação desta entrega

[Resultado consolidado](resultado-validacao.json): 470 casos backend na execução controlada/visual, **469 aprovados, zero falhas/erros e uma sondagem real opt-in ignorada nessa execução**. As consultas reais anteriores e a falha posterior ao IBGE permanecem nos arquivos próprios; não são mascaradas pelo resultado da suíte controlada. Frontend: **147 testes em 32 arquivos, todos aprovados**. Build de produção aprovado, com avisos de orçamento do bundle inicial (687,11 kB) e do estilo existente do dashboard. Foram aprovadas e inspecionadas **46 capturas**.

A regressão controlada completa foi executada com `CAISORA_VISUAL=true CAISORA_AIE_VISUAL=true CAISORA_AIE_REAL=false`, mantendo a sondagem externa separada. A saída de capturas da SE06 foi direcionada para `/tmp` nessa execução para preservar os artefatos anteriores.
