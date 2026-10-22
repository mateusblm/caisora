# Inventário técnico para análise de Pontos de Função

Revisão: 22/10/2026. Este documento descreve comportamento implementado, dados de entrada/saída e evidências. Não atribui classificação nem quantidade de Pontos de Função. A classificação e a contagem dependem da fronteira da aplicação, dos processos elementares e dos slides do professor Tiago Navarro, a serem analisados posteriormente.

## Arquitetura e segurança

O backend mantém módulos por domínio com camadas `api`, `aplicacao`, `dominio` e `infraestrutura`. O módulo `ambiente` tem o cliente do provedor em `infraestrutura/openmeteo`; `relatorio` utiliza a porta `DadosRelatorio` e sua implementação JPA. Relatórios usam transações de leitura e não persistem indicadores. Não há novas dependências de produção.

O JWT validado fornece a organização. Os endpoints de consultas ambientais, relatórios e coordenadas da própria marina não recebem um identificador de organização. Todos os filtros JPA incluem `organizacao.id`. O gerenciamento global de módulos aceita uma organização explícita e exige o perfil de administrador da plataforma. A autorização é aplicada no backend; menu e guardas Angular utilizam as permissões resolvidas pelo mesmo mecanismo de variabilidade.

`AMBIENTE` e `RELATORIOS` começam desabilitados e exigem liberação pela plataforma. Módulos existentes preservam seus padrões e configurações. Após liberação, administradores da marina e gerentes possuem acesso padrão a ambos; atendentes possuem acesso ambiental; financeiro possui acesso a relatórios comerciais. As permissões podem ser personalizadas. Cada relatório exige, adicionalmente, acesso de leitura aos seus módulos de negócio: VAGAS + OCUPACOES, MOVIMENTACOES ou CONTRATOS. Assim a permissão de relatórios não contorna bloqueios comerciais ou internos.

## Consultas sem produção de dados derivados

### Referência externa: meteorologia — criada

- Finalidade: consultar previsões meteorológicas horárias para a localização da marina.
- Angular: `/ambiente`, opção **Meteorologia**. Componente `features/ambiente/ambiente.component.ts` e serviço `ambiente.service.ts`.
- Endpoint: `GET /api/v1/ambiente/meteorologia?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`.
- Entrada: `inicio`, `fim`, obrigatórios; datas inclusivas entre hoje UTC e hoje + 15 dias (16 datas), ordem cronológica. As coordenadas vêm da organização autenticada.
- Saída: `origem`, `fonte`, `consultadoEm`, `fusoHorario`, `atualizacaoProvedor`, `incompleta`, `unidades`, `horas[].horario`, `horas[].valores`.
- Valores: `temperature_2m` (temperatura), `wind_speed_10m` (velocidade do vento), `wind_direction_10m` (direção), `wind_gusts_10m` (rajadas), `precipitation_probability` (probabilidade de precipitação). Unidades são as retornadas pelo provedor, apresentadas nas colunas.
- Dados/entidades: conjunto Weather Forecast mantido pela Open-Meteo; `Organizacao`/`organizacoes` apenas fornece latitude e longitude. Nenhuma tabela de previsões, cópia de referência ou dado fictício é criada.
- Processamento: validação do período, leitura das coordenadas, requisição HTTP e adaptação de formato. Não produz indicadores derivados do negócio.
- Evidências: `AmbienteServiceTest`, `OpenMeteoClientTest`, `NovosEndpointsTest`, `NovasFuncionalidadesSegurancaTest`, `ambiente.service.spec.ts`, `ambiente.component.spec.ts`.

### Referência externa: condições marítimas — criada

- Finalidade: consultar ondas e ondulação separadamente da meteorologia.
- Angular: `/ambiente`, opção **Condições marítimas**; endpoint: `GET /api/v1/ambiente/maritimo?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`.
- Entrada, envelope de saída e intervalo: os mesmos da consulta meteorológica.
- Campos próprios: `wave_height` (altura das ondas), `wave_direction` (direção das ondas), `wave_period` (período das ondas), `swell_wave_height` (altura da ondulação), `swell_wave_direction` (direção da ondulação).
- Dados/entidades: conjunto Marine Forecast mantido pela Open-Meteo, separado de Weather Forecast; `Organizacao`/`organizacoes` para coordenadas. Não há persistência dos dados externos.
- Processamento: consulta e adaptação de dados de referência externos. A disponibilidade efetiva de cada variável depende do modelo/localidade; dados ausentes permanecem `null`, nunca zero fabricado. O frontend mostra “—” e aviso de resposta incompleta.
- Evidências: os mesmos testes ambientais, com cenários específicos de resposta marítima, arrays menores, ausência de campos e período vazio.

### Regras comuns das integrações

As duas consultas usam hosts distintos e campos próprios, no cliente `OpenMeteoClient`. Timeout de conexão: 3 segundos; de leitura: 8 segundos, configuráveis pelo backend. Erros HTTP, timeout, JSON inválido e estrutura sem horários geram HTTP 503 com código `PROVEDOR_INDISPONIVEL`, sem expor URL/chave. Campos ausentes ou arrays menores geram resposta parcial identificada. Erros de datas, coordenadas não configuradas e integração desabilitada geram HTTP 400; acesso negado gera 403; ausência de autenticação é bloqueada pela cadeia de segurança.

`consultadoEm` representa a obtenção da resposta, e não a atualização do modelo. A API de previsão não fornece nesse contrato o instante de atualização do modelo; a tela informa essa ausência explicitamente. Todas as horas são apresentadas em UTC. A consulta pode ser repetida pelo usuário para obter uma nova resposta. Não há promessa de cobertura completa de todos os modelos por 16 dias.

As telas informam que previsões são estimativas e não garantem segurança para navegação, e exibem a fonte e a atribuição CC BY 4.0. Os dados não substituem informações náuticas oficiais.

### Consultas de negócio existentes — verificadas, sem reimplementação

Todas recebem parâmetros, executam pesquisa restrita à organização e retornam resultados sem alterar os dados de negócio. Os métodos dos serviços são `@Transactional(readOnly = true)`. Listagens usam paginação `page`, `size` e `sort`; consultas individuais recebem `id`. Os quatro serviços e controllers existentes foram mantidos.

| Consulta e finalidade | Rota Angular | Endpoint GET | Filtros e processamento | Entidades/tabelas | Evidências |
|---|---|---|---|---|---|
| Clientes: localizar clientes | `/clientes` | `/api/v1/clientes`, `/{id}` | `nome` parcial sem distinguir caixa; senão `ativo`; paginação | Cliente / clientes; organização | ClienteControllerTest, ClienteServiceTest, ClienteRepositoryJpaTest |
| Embarcações: localizar embarcações | `/embarcacoes` | `/api/v1/embarcacoes`, `/{id}` | Prioridade: `nome`, `proprietarioId`, `tipo`, `ativa`; paginação | Embarcacao / embarcacoes; Cliente / clientes; organização | EmbarcacaoControllerTest, EmbarcacaoServiceTest, EmbarcacaoRepositoryJpaTest |
| Vagas: localizar vagas | `/vagas` | `/api/v1/vagas`, `/{id}` | Prioridade: `codigo`, `setor`, `tipo`, `ativa`; paginação | Vaga / vagas; organização | VagaControllerTest, VagaServiceTest, VagaRepositoryJpaTest, vaga-listagem.component.spec.ts |
| Contratos: localizar contratos | `/contratos`, `/contratos/:id` | `/api/v1/contratos`, `/{id}` | Combina `status`, `clienteId`, `embarcacaoId`; paginação | Contrato / contratos; Cliente / clientes; Embarcacao / embarcacoes | ContratoServiceTest; contrato-listagem.component.spec.ts; contrato.service.spec.ts |

Clientes, embarcações e vagas usam o primeiro filtro aplicável na ordem acima; os filtros não são cumulativos nessas consultas existentes. O frontend de vagas/embarcações respeita essa semântica. Os relatórios novos combinam seus próprios filtros e não alteram essa API.

Campos retornados pelas consultas (DTOs existentes; as telas exibem as colunas principais e detalhes/formulários dão acesso aos demais campos):

- **ClienteResponse**: `id`, `tipoPessoa`, `nome`, `razaoSocial`, `cpfCnpj`, `email`, `telefone`, `celular`, `observacoes`, `ativo`, `organizacaoId`, `criadoEm`, `atualizadoEm`. Fonte: [`ClienteResponse.java`](../src/main/java/br/com/caisora/cliente/api/ClienteResponse.java).
- **EmbarcacaoResponse**: `id`, `proprietarioId`, `proprietarioNome`, `nome`, `tipo`, `fabricante`, `modelo`, `anoFabricacao`, `numeroInscricao`, `numeroCasco`, `portoInscricao`, `codigoPaisBandeira`, `comprimentoTotalMetros`, `bocaMetros`, `caladoMetros`, `pontalMetros`, `alturaTotalMetros`, `pesoKg`, `capacidadePessoas`, `tipoPropulsao`, `corPredominante`, `observacoes`, `ativa`, `organizacaoId`, `criadaEm`, `atualizadaEm`. Fonte: [`EmbarcacaoResponse.java`](../src/main/java/br/com/caisora/embarcacao/api/EmbarcacaoResponse.java).
- **VagaResponse**: `id`, `codigo`, `tipo`, `setor`, `localizacao`, `comprimentoMaximoMetros`, `bocaMaximaMetros`, `caladoMaximoMetros`, `alturaMaximaMetros`, `pesoMaximoKg`, `possuiAgua`, `possuiEnergia`, `observacoes`, `ativa`, `organizacaoId`, `criadaEm`, `atualizadaEm`. Fonte: [`VagaResponse.java`](../src/main/java/br/com/caisora/vaga/api/VagaResponse.java).
- **ContratoResponse**: `id`, `numero`, `status`, `clienteId`, `clienteNome`, `embarcacaoId`, `embarcacaoNome`, `tipoVagaContratada`, `periodicidade`, `dataInicio`, `dataFim`, `dataAssinatura`, `dataAtivacao`, `dataEncerramento`, `renovacaoAutomatica`, `diasAvisoPrevio`, `valorBase`, `diaVencimento`, `observacoes`, `criadoPorId`, `criadoPorNome`, `organizacaoId`, `criadoEm`, `atualizadoEm`, `versao`. Fonte: [`ContratoResponse.java`](../src/main/java/br/com/caisora/contrato/api/ContratoResponse.java).

Nenhuma dessas consultas produz os indicadores dos relatórios abaixo; paginação, filtragem e formatação não são tratadas aqui como novos relatórios.

## Relatórios com produção de dados derivados — criados

Rota Angular comum: `/relatorios`, com seleção de objetivo funcional e formulário próprio. `RelatoriosComponent` exibe somente relatórios para cujos módulos de negócio o usuário possui permissão. `RelatoriosService` chama os endpoints; todos os cálculos são feitos no backend em `RelatorioService`, com consultas JPA em `DadosRelatorioJpa`. O componente usa Angular Material, filtros que quebram em linhas em telas estreitas e tabelas com rolagem horizontal. As respostas vazias possuem mensagens e indicadores definidos, sem dados simulados.

### Ocupação de vagas

- Objetivo: analisar a utilização atual das vagas ativas, por setor e tipo.
- Endpoint: `GET /api/v1/relatorios/ocupacao`.
- Entradas opcionais cumulativas: `setor` (até 100 caracteres, igualdade sem distinguir caixa, espaços externos removidos); `tipo` (`MOLHADA`, `SECA`, `POITA`, `OUTRA`). Setor em branco equivale à ausência do filtro. Tipos inválidos retornam 400.
- Entidades/tabelas: Vaga / `vagas`; Ocupacao / `ocupacoes`. Organização obrigatória em ambas as partes da consulta.
- Regras: inclui somente vagas ativas; uma vaga é ocupada quando existe ocupação com `status=ATIVA` vinculada a ela. Uso de `exists` evita contar mais de uma vez uma vaga. Ocupações encerradas não ocupam vagas. Posição física da embarcação não substitui a ocupação contratada/registrada.
- Saída: `totalVagasAtivas`, `ocupadas`, `disponiveis`, `taxaOcupacao`, `grupos[]` contendo `setor`, `tipo` e os mesmos quatro indicadores. Grupos ordenados por setor/tipo; setor nulo aparece como “Sem setor”.
- Derivação: disponíveis = ativas − ocupadas; taxa = ocupadas / ativas × 100, arredondada a duas casas com HALF_UP. Sem vagas ativas: totais e taxa zero, grupos vazios.
- Evidências: RelatorioServiceTest (66,67%, zero e grupos), DadosRelatorioJpaTest (ativas, ocupação, filtros e duas organizações), NovasFuncionalidadesSegurancaTest, NovosEndpointsTest e relatorios.component.spec.ts.

### Desempenho das movimentações

- Objetivo: analisar volume, duração e pontualidade operacional.
- Endpoint: `GET /api/v1/relatorios/movimentacoes`.
- Entradas: `inicio`, `fim` obrigatórios, datas ISO em ordem cronológica; `tipo` opcional (`LANCAMENTO`, `RETIRADA`, `RETORNO_PARA_VAGA`, `TRANSFERENCIA`, `DESLOCAMENTO_INTERNO`).
- Período: seleciona pelo **agendamento**, não pela criação/conclusão, entre 00:00 UTC da data inicial (inclusivo) e 00:00 UTC do dia posterior à data final (exclusivo).
- Entidades/tabelas: Movimentacao / `movimentacoes`. Os instantes de início e conclusão já existem na entidade; não foi necessário duplicar o histórico de movimentações.
- Saída: `total`, `porSituacao`, `concluidas`, `canceladas`, `tempoMedioExecucaoSegundos`, `amostrasDuracao`, `concluidasNoPrazo`, `percentualNoPrazo`, `regraPontualidade`.
- Derivação da duração: somente movimentações `CONCLUIDA` com início/conclusão presentes e conclusão >= início; média em segundos, duas casas HALF_UP. Zero amostras: média `null` e texto “Sem dados suficientes”; duração válida zero participa da média.
- Pontualidade: conclusão efetiva **menor ou igual** ao `agendadaPara`, sem tolerância. Aqui o único horário agendado existente é usado como limite de conclusão; não foi inventado um campo de prazo. Todas as concluídas filtradas compõem o denominador; registros sem conclusão suficiente não contam como pontuais. A regra aparece na resposta e na tela. Percentual = pontuais / concluídas × 100. Nenhuma concluída: 0%.
- Situações sem registros aparecem com quantidade zero. Falta de dados para duração não remove o registro dos totais.
- Evidências: RelatorioServiceTest (limite exato, atraso, duração ausente/invertida e vazio), DadosRelatorioJpaTest (limites UTC e isolamento), NovasFuncionalidadesSegurancaTest e relatorios.component.spec.ts.

### Carteira de contratos

- Objetivo: analisar composição comercial e valores-base contratados por periodicidade.
- Endpoint: `GET /api/v1/relatorios/contratos`.
- Entradas opcionais cumulativas: `status`, `periodicidade`, `inicio`, `fim`. Datas filtram `dataInicio`, com limites inclusivos; aceita só um limite. Se os dois existirem, fim >= início.
- Situações: `RASCUNHO`, `PENDENTE_ASSINATURA`, `ATIVO`, `SUSPENSO`, `EM_ENCERRAMENTO`, `ENCERRADO`, `CANCELADO`. Periodicidades: `DIARIA`, `MENSAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`, `PERSONALIZADA`. Enum inválido retorna 400.
- Entidades/tabelas: Contrato / `contratos`. Não foi necessário acessar clientes, pois não há filtro de cliente solicitado para esse relatório.
- Saída: `total`, `porSituacao`, `percentualPorSituacao`, `porPeriodicidade`, `valoresBasePorPeriodicidade`.
- Derivações: contagens por situação/periodicidade; percentual por situação em relação ao total filtrado (duas casas HALF_UP); soma monetária decimal de `valorBase` para cada periodicidade, em reais. Sem dados: contagens, percentuais e somas zero. Arredondamento pode fazer a soma dos percentuais diferir de 100%.
- Não há conversão entre periodicidades, soma financeira geral ou projeção de receitas. Mesmo os valores de periodicidade personalizada são apenas uma soma da categoria, não um fluxo de caixa. Contratos cancelados/encerrados entram se os filtros os incluírem. As somas não representam pagamento recebido nem faturamento realizado.
- Evidências: RelatorioServiceTest (mensal e anual separados, percentuais e vazio), DadosRelatorioJpaTest (filtros combinados e isolamento), NovasFuncionalidadesSegurancaTest e relatorios.component.spec.ts.

### SE06 — Permanência histórica nas vagas — criada

**Objetivo funcional próprio:** analisar quanto tempo duraram as ocupações efetivamente encerradas no período, apoiando análise histórica de uso. O relatório de ocupação já existente continua apresentando vagas ativas ocupadas/disponíveis no instante da consulta. SE06 seleciona outro conjunto de registros e produz médias, extremos e horas históricas; não deriva seus resultados da fotografia atual nem chama seu endpoint.

A identificação **SE06** é a referência informada pela equipe acadêmica. Não atribui Pontos de Função nem certifica automaticamente classificação/contagem global ou distribuição por integrante. A conferência segue os slides do professor e a avaliação da equipe.

- **Rota/tela:** `/relatorios`, opção **Permanência histórica nas vagas** em `RelatoriosComponent` (`relatorios.component.ts`, `.html`, `.scss`).
- **Endpoint:** `GET /api/v1/relatorios/permanencia-vagas`.
- **DTO:** `PermanenciaVagasResponse`, com records internos `Indicadores` e `Grupo`, em `relatorio/api`.
- **Porta e implementação:** `DadosRelatorio.permanencias(...)` retorna projeções `PermanenciaOcupacao`; `DadosRelatorioJpa` consulta apenas campos de Ocupacao e Vaga; `RelatorioService.permanenciaVagas(...)` calcula todos os indicadores dentro de transação de leitura.
- **Autorização:** exige simultaneamente `RELATORIOS:VISUALIZAR`, `VAGAS:VISUALIZAR` e `OCUPACOES:VISUALIZAR`, pelos controles existentes de variabilidade. Módulo comercial desligado ou permissão negada impede acesso, inclusive diretamente ao endpoint. A organização é obtida do JWT; não há parâmetro para trocar o tenant. A consulta verifica `o.organizacao.id` **e** `v.organizacao.id`.
- **Persistência:** nenhuma tabela, coluna ou migration nova necessária. Nenhum indicador ou dado de demonstração é persistido em produção.

**ENTRADA**

| Campo | Tipo/obrigatoriedade | Regra |
|---|---|---|
| `inicio` | data ISO `AAAA-MM-DD`, obrigatório | Primeiro dia de encerramento efetivo, inclusivo, 00:00 UTC |
| `fim` | data ISO `AAAA-MM-DD`, obrigatório | Último dia de encerramento efetivo, inclusivo; consulta usa limite exclusivo às 00:00 UTC do dia seguinte |
| `tipo` | enum opcional | MOLHADA, SECA, POITA ou OUTRA; igualdade no cadastro da vaga |
| `setor` | texto opcional, até 100 caracteres | Correspondência exata sem distinguir caixa, espaços externos removidos; texto em branco equivale a filtro ausente |

As datas precisam estar em ordem e entre os anos 1 e 9999. Data ausente, formato inválido, enum inválido, cronologia invertida e setor excessivo retornam HTTP 400. Filtros opcionais são cumulativos. Exemplo: `/api/v1/relatorios/permanencia-vagas?inicio=2026-10-10&fim=2026-10-10&tipo=SECA&setor=Norte`.

**ALIs realmente referenciados para esta análise**

| Arquivo lógico de negócio | Entidade/tabela | Campos efetivamente usados |
|---|---|---|
| Ocupações | `Ocupacao` / `ocupacoes` | `id`, `organizacao_id`, `vaga_id`, `status`, `inicio_em`, `encerrada_em` |
| Vagas | `Vaga` / `vagas` | `id`, `organizacao_id`, `setor`, `tipo` |

A projeção não carrega embarcação, cliente, contrato ou histórico de movimentações. As chaves de organização restringem o tenant; os dados cadastrais de organizações não são consultados para este cálculo. Os dois ALIs de negócio acima são os referenciados nesta evidência, sem atribuição automática de complexidade ou quantidade de PF.

**Seleção e cálculos derivados**

1. Considera somente `status=ENCERRADA` com `encerradaEm` no intervalo solicitado. Ocupações ATIVAS ficam fora, mesmo se a previsão de saída estiver no período.
2. Para cada ocupação, `duracaoSegundos = encerradaEm − inicioEm`, considerando a duração completa, inclusive a parte anterior à data inicial. `fimPrevistoEm` nunca é usado como encerramento. As frações de segundo são preservadas antes de agregar.
3. Quantidade = número de ocupações selecionadas; média = soma das durações / quantidade; menor/maior = extremos das durações selecionadas; total de horas = soma das durações / 3600.
4. Repete os mesmos cinco indicadores para cada combinação de setor/tipo. A média geral é ponderada pela quantidade de ocupações, não uma média simples das médias dos grupos.
5. Média, mínimo e máximo são apresentados em segundos com duas casas decimais; horas com quatro casas; arredondamento HALF_UP somente na saída. Duração zero válida é incluída.
6. Sem registros: quantidade 0, total de horas 0, média/mínimo/máximo `null`, grupos `[]`; a tela mostra “Sem dados” e mensagem específica, sem divisão por zero.
7. Datas ausentes/invertidas ou tipo ausente em projeções inconsistentes geram erro explícito `DADOS_HISTORICOS_INVALIDOS` (HTTP 400); não viram duração zero nem são removidas silenciosamente. As constraints existentes do PostgreSQL já impedem essas inconsistências pelos fluxos normais.

**SAÍDA**

| Campo JSON | Conteúdo |
|---|---|
| `resumo.quantidadeOcupacoes` | Quantidade de ocupações encerradas selecionadas |
| `resumo.permanenciaMediaSegundos` | Média de duração efetiva, ou null sem amostras |
| `resumo.menorPermanenciaSegundos` | Menor duração efetiva, ou null sem amostras |
| `resumo.maiorPermanenciaSegundos` | Maior duração efetiva, ou null sem amostras |
| `resumo.totalHorasOcupacao` | Soma de durações efetivas convertida em horas |
| `grupos[].setor`, `grupos[].tipo` | Dimensões do agrupamento; setor nulo aparece como “Sem setor” |
| `grupos[].indicadores` | Os mesmos cinco campos do resumo para o grupo |

**Limitações semânticas:** inclui vagas hoje inativas. Setor e tipo são os valores **atuais** da vaga, pois não existe versionamento desses atributos; não se afirma reconstruir a configuração física antiga. Horas são somadas por ocupação, sem deduplicar simultaneidade entre vagas; não são horas de calendário da marina, taxa de ocupação temporal, receita ou pagamento. Não há corte da duração ao intervalo, pois o período seleciona pelo encerramento.

**Evidências de implementação e teste**

- `PermanenciaVagasServiceTest`: média geral/grupos, ponderação, mínimos/máximos, horas, zero válido, frações de segundo, filtros encaminhados à porta, datas/limites inválidos, dados inconsistentes e conjunto vazio.
- `PermanenciaVagasJpaTest`: PostgreSQL com todas as migrations; apenas encerradas; encerramento exatamente no início e no último microssegundo do dia; exclusão de registros imediatamente antes/depois; previsão de saída ignorada; duração que começa antes do filtro; vaga inativa; filtros cumulativos por tipo/setor; dois tenants e associação inconsistente entre tenants; vazio e ausência de alteração de registros.
- `NovasFuncionalidadesSegurancaTest`: nega o novo endpoint com cada um dos três módulos desligado; verifica perfil, tenant do JWT e chamada autorizada.
- `NovosEndpointsTest`: valida parâmetros obrigatórios, formato das datas e enum HTTP.
- `relatorios.component.spec.ts` e `relatorios.service.spec.ts`: filtros, requisição GET independente, opção conforme permissões, loading, erro, vazio, médias/extremos/horas recebidos do backend e validação de datas no formulário.
- `PermanenciaVagasVisualTest` + [`scripts/validar-se06-browser.mjs`](../scripts/validar-se06-browser.mjs): Chromium real com build de produção, login real, JWT, Spring Boot e PostgreSQL descartável. Capturas desktop/mobile de filtros preenchidos, resultados, estado vazio, loading/erro e tabela rolada. Resultados e vazios vêm da API real; apenas latência/falha são induzidas pelo navegador para reprodução controlada.
- Evidências visuais e instruções: [demonstração SE06](evidencias/se06/README.md), com capturas e `verificacao.json`. Mobile usa emulação de viewport/touch, sem aparelho físico. Não houve validação em Safari/Firefox nem leitor de tela.

**AIEs e contagem:** esta implementação não adiciona AIE nem integração externa. Os dois conjuntos externos já documentados permanecem os mesmos. A eventual reutilização por integrante não deve ser apresentada como quatro novos conjuntos ou somada em duplicidade no total do projeto; a interpretação do requisito “2 por membro” permanece pendente de confirmação com o professor.

## Manutenção e configurações

### Localização da marina — criada

- Finalidade: administrar latitude/longitude usadas nas consultas externas.
- Angular: `/configuracoes/localizacao`, acessível na navegação de configurações.
- Endpoints: `GET /api/v1/minha-organizacao/coordenadas` para consulta autenticada; `PUT /api/v1/minha-organizacao/coordenadas` somente para administradores da marina/plataforma da organização do token.
- Entrada PUT: `latitude` obrigatória entre −90 e 90; `longitude` obrigatória entre −180 e 180; até seis casas decimais. Não recebe organização no corpo/URL. Zero é coordenada válida, não marcador de ausência.
- Saída GET/PUT: `latitude`, `longitude`. Organizações antigas começam com ambas nulas. A tela inicia vazia quando não configuradas.
- Entidade/tabela: Organizacao / `organizacoes`, campos novos `latitude NUMERIC(9,6)`, `longitude NUMERIC(10,6)`. Migration `V21__coordenadas_organizacao.sql`; constraint valida o par e os intervalos. Auditoria de atualização da organização preservada.
- Derivação: nenhuma; PUT mantém configuração interna, GET é uma consulta separada.
- Evidências: CoordenadasTest, DadosRelatorioJpaTest, NovosEndpointsTest, NovasFuncionalidadesSegurancaTest e localizacao.component.spec.ts.

### Disponibilidade de módulos — modificada

- Finalidade: permitir à plataforma definir o que cada organização pode usar.
- Angular: `/configuracoes/variabilidade`. A plataforma consulta a organização pelo UUID e altera seus módulos. O UUID carregado fica vinculado às alterações mesmo se o campo for editado depois. A marina vê a disponibilidade em modo de leitura.
- Endpoints globais criados: `GET /api/v1/organizacoes/{organizacaoId}/modulos`; `PUT /api/v1/organizacoes/{organizacaoId}/modulos/{modulo}`. Exigem administrador da plataforma e organização existente.
- Endpoint existente modificado: `PUT /api/v1/variabilidade/modulos/{modulo}` agora exige administrador da plataforma (mantido por compatibilidade, atua na organização do token). O serviço `atualizarModulo` também exige esse perfil.
- Entradas: UUID da organização (endpoint global), enum do módulo, corpo `{ "ativo": true|false }` obrigatório. DASHBOARD/CONFIGURACOES são obrigatórios e não podem ser desabilitados.
- Saída: `modulos[]` (`modulo`, `nome`, `obrigatorio`, `ativo`, `acoes`) e `perfis[]` (`perfil`, `permissoes`).
- Entidades/tabelas: ModuloOrganizacao / `organizacao_modulos`; PermissaoPerfilOrganizacao / `perfil_permissoes`; Organizacao / `organizacoes` para validação da existência.
- Sem cálculo de negócio. As configurações resolvidas seguem os padrões/sobrescritas existentes. Defaults comerciais antigos são preservados; os novos módulos começam desligados.
- Evidências: NovasFuncionalidadesSegurancaTest prova negação à marina nos dois endpoints e no serviço; variabilidade.component.spec prova bloqueio visual e alvo carregado; VariabilidadeServiceTest preserva as regras anteriores.

### Permissões internas da marina — existente, preservada

- Finalidade: definir ações por perfil dentro dos módulos liberados pela plataforma.
- Angular: `/configuracoes/variabilidade`, seção “Permissões por perfil” para a marina.
- Endpoints: `GET /api/v1/variabilidade/configuracao`; `PUT /api/v1/variabilidade/perfis/{perfil}/permissoes`; `GET /api/v1/variabilidade/minha-configuracao` para o próprio acesso autenticado.
- Entrada PUT: perfil e conjunto completo `permissoes` de chaves `MODULO:ACAO`. Organização sempre pelo token.
- Saída: configuração de módulos/perfis; a consulta do próprio acesso retorna `modulosAtivos`, `permissoes`.
- Entidades/tabelas: `perfil_permissoes` e `organizacao_modulos`. Permissões não habilitam módulos. Perfil da plataforma não é configurável pela marina; administrador da marina mantém CONFIGURACOES:CONFIGURAR.
- Evidências: VariabilidadeServiceTest, PoliticaPadraoVariabilidadeTest e NovasFuncionalidadesSegurancaTest (atribuir RELATORIOS:VISUALIZAR não libera módulo desligado).

### Operações de manutenção existentes — verificadas e preservadas

Os cadastros e alterações abaixo usam organização autenticada, validação dos DTOs e regras de domínio. Todos continuam nas mesmas rotas. A saída é o DTO da entidade, enumerado na seção de consultas; ocupações e vínculos têm campos próprios abaixo.

| Operação/finalidade | Situação | Rota Angular | Endpoint | Entidades/tabelas | Evidências |
|---|---|---|---|---|---|
| Incluir/alterar cliente | Existente | `/clientes/novo`, `/clientes/:id/editar` | POST `/api/v1/clientes`, PUT `/api/v1/clientes/{id}` | Cliente / clientes | ClienteControllerTest, ClienteServiceTest |
| Incluir/alterar embarcação | Existente | `/embarcacoes/nova`, `/embarcacoes/:id/editar` | POST `/api/v1/embarcacoes`, PUT `/api/v1/embarcacoes/{id}` | Embarcacao / embarcacoes, Cliente / clientes | EmbarcacaoControllerTest, EmbarcacaoServiceTest |
| Incluir/alterar vaga | Existente | `/vagas/nova`, `/vagas/:id/editar` | POST `/api/v1/vagas`, PUT `/api/v1/vagas/{id}` | Vaga / vagas | VagaControllerTest, VagaServiceTest, vaga-formulario.component.spec.ts |
| Registrar/alterar ocupação | Existente | `/ocupacoes/nova`, `/ocupacoes/:id/editar` | POST `/api/v1/ocupacoes`, PUT `/api/v1/ocupacoes/{id}` | Ocupacao / ocupacoes, Vaga / vagas, Embarcacao / embarcacoes | OcupacaoControllerTest, OcupacaoServiceTest, OcupacaoRepositoryJpaTest, ocupacao-formulario.component.spec.ts |
| Registrar/alterar contrato | Existente | `/contratos/novo`, `/contratos/:id/editar` | POST `/api/v1/contratos`, PUT `/api/v1/contratos/{id}` | Contrato / contratos; Cliente / clientes; Embarcacao / embarcacoes; HistoricoContrato / historicos_contrato; sequencias_contrato | ContratoServiceTest, ContratoTest, contrato-formulario.component.spec.ts |
| Desvincular ocupação de contrato | Existente | `/contratos/:id` | DELETE `/api/v1/contratos/{id}/ocupacoes/{vinculoId}` | VinculoContratoOcupacao / vinculos_contrato_ocupacao; Contrato / contratos; HistoricoContrato / historicos_contrato | ContratoServiceTest, VinculoContratoOcupacaoTest, contrato-detalhe.component.spec.ts, contrato.service.spec.ts |

Campos de entrada/saída das manutenções, extraídos dos DTOs existentes:

#### Cliente

- `CriarClienteRequest.java`: `tipoPessoa`, `nome`, `razaoSocial`, `cpfCnpj`, `email`, `telefone`, `celular`, `observacoes`.
- `AtualizarClienteRequest.java`: `tipoPessoa`, `nome`, `razaoSocial`, `cpfCnpj`, `email`, `telefone`, `celular`, `observacoes`.

#### Embarcacao

- `CriarEmbarcacaoRequest.java`: `proprietarioId`, `nome`, `tipo`, `fabricante`, `modelo`, `anoFabricacao`, `numeroInscricao`, `numeroCasco`, `portoInscricao`, `codigoPaisBandeira`, `comprimentoTotalMetros`, `bocaMetros`, `caladoMetros`, `pontalMetros`, `alturaTotalMetros`, `pesoKg`, `capacidadePessoas`, `tipoPropulsao`, `corPredominante`, `observacoes`.
- `AtualizarEmbarcacaoRequest.java`: `proprietarioId`, `nome`, `tipo`, `fabricante`, `modelo`, `anoFabricacao`, `numeroInscricao`, `numeroCasco`, `portoInscricao`, `codigoPaisBandeira`, `comprimentoTotalMetros`, `bocaMetros`, `caladoMetros`, `pontalMetros`, `alturaTotalMetros`, `pesoKg`, `capacidadePessoas`, `tipoPropulsao`, `corPredominante`, `observacoes`.

#### Vaga

- `CriarVagaRequest.java`: `codigo`, `tipo`, `setor`, `localizacao`, `comprimentoMaximoMetros`, `bocaMaximaMetros`, `caladoMaximoMetros`, `alturaMaximaMetros`, `pesoMaximoKg`, `possuiAgua`, `possuiEnergia`, `observacoes`.
- `AtualizarVagaRequest.java`: `codigo`, `tipo`, `setor`, `localizacao`, `comprimentoMaximoMetros`, `bocaMaximaMetros`, `caladoMaximoMetros`, `alturaMaximaMetros`, `pesoMaximoKg`, `possuiAgua`, `possuiEnergia`, `observacoes`.

#### Ocupacao

- `CriarOcupacaoRequest.java`: `embarcacaoId`, `vagaId`, `inicioEm`, `fimPrevistoEm`, `observacoes`.
- `AtualizarOcupacaoRequest.java`: `fimPrevistoEm`, `observacoes`.
- Saída `OcupacaoResponse`: `id`, `embarcacaoId`, `embarcacaoNome`, `embarcacaoModelo`, `proprietarioNome`, `vagaId`, `vagaCodigo`, `vagaTipo`, `vagaSetor`, `vagaLocalizacao`, `status`, `inicioEm`, `fimPrevistoEm`, `encerradaEm`, `observacoes`, `organizacaoId`, `criadaEm`, `atualizadaEm`.

#### Contrato

- `CriarContratoRequest.java`: `clienteId`, `embarcacaoId`, `tipoVagaContratada`, `periodicidade`, `dataInicio`, `dataFim`, `renovacaoAutomatica`, `diasAvisoPrevio`, `valorBase`, `diaVencimento`, `observacoes`.
- `AtualizarContratoRequest.java`: `clienteId`, `embarcacaoId`, `tipoVagaContratada`, `periodicidade`, `dataInicio`, `dataFim`, `renovacaoAutomatica`, `diasAvisoPrevio`, `valorBase`, `diaVencimento`, `observacoes`.

#### Regras e exclusão segura

- Clientes: documento normalizado e único na organização; alteração de situação mantém o registro (`PATCH /clientes/{id}/status`, corpo `ativo`).
- Embarcações: proprietário pertence à mesma organização, medidas validadas; situação mantida por `PATCH /embarcacoes/{id}/status`, corpo `ativa`.
- Vagas: código único por organização, dimensões validadas; `PATCH /vagas/{id}/status`, corpo `ativa`, mantém histórico.
- Ocupações: impedem duas ocupações ativas para a mesma vaga/embarcação; edição mantém identificadores, altera previsão/observações. Encerramento por `PATCH /ocupacoes/{id}/encerramento` mantém o registro e a data de encerramento.
- Contratos: máquina de estados limita edição/transições, valida vigência e dados comerciais e mantém histórico. Operações específicas ativam, suspendem, reativam, encerram ou cancelam contratos.
- Desvinculação: entrada `id`, `vinculoId` e `motivo` opcional (até 500 caracteres; padrão “Ocupacao desvinculada”). Valida contrato e vínculo da organização. Finaliza `fimEm` e `motivoFim`, salva vínculo e evento `OCUPACAO_DESVINCULADA` no histórico. Não apaga ocupação, contrato ou vínculo; vínculo já finalizado gera conflito. Esta operação DELETE é uma candidata segura para demonstrar exclusão lógica de uma associação. Sua classificação APF será analisada posteriormente.
- A inativação de organizações também já existe por DELETE `/api/v1/organizacoes/{id}`, exclusiva da plataforma, e altera apenas `ativa` (sem tela Angular específica).

Nenhuma nova exclusão destrutiva foi criada para atender contagem acadêmica.

Saída da desvinculação, `VinculoContratoOcupacaoResponse`: `id`, `contratoId`, `ocupacaoId`, `vagaId`, `vagaCodigo`, `inicioEm`, `fimEm`, `motivoFim`, `aberto`.

## Habilitação e termos do provedor

A integração começa desabilitada (`OPEN_METEO_ENABLED=false`). Para demonstração acadêmica não comercial, revisar os termos, configurar `OPEN_METEO_ENABLED=true`, liberar o módulo AMBIENTE pela plataforma e salvar coordenadas reais da marina. RELATORIOS também deve ser liberado pela plataforma. Permissões resolvidas são atualizadas na sessão pelo fluxo existente; usuários de outra sessão podem atualizar o próprio usuário ou entrar novamente para atualizar os menus. O backend aplica a configuração a cada requisição.

Configurações do backend:

| Variável | Padrão | Finalidade |
|---|---|---|
| OPEN_METEO_ENABLED | false | Habilitar chamadas após revisão de uso/licença |
| OPEN_METEO_WEATHER_URL | https://api.open-meteo.com | Host meteorológico |
| OPEN_METEO_MARINE_URL | https://marine-api.open-meteo.com | Host marítimo |
| OPEN_METEO_API_KEY | vazio | Chave comercial, apenas no backend |

Timeouts: `caisora.open-meteo.connect-timeout-ms` e `read-timeout-ms`. Configurar hosts comerciais e chave conforme contrato do provedor para uso SaaS comercial; nenhuma chave é enviada ao Angular. Nenhum plano comercial foi contratado ou habilitado automaticamente.

Revisão documental em 22/10/2026: [Weather API](https://open-meteo.com/en/docs), [Marine API](https://open-meteo.com/en/docs/marine-weather-api), [termos de uso](https://open-meteo.com/en/terms). Os termos distinguem acesso gratuito não comercial (incluindo educação, sujeito a limites de chamadas) do serviço comercial por assinatura. A licença dos dados CC BY 4.0 exige atribuição; ela não substitui os termos de acesso da API gratuita. A configuração técnica permite usar os hosts contratados, mas a validação contratual e dos limites cabe à plataforma antes da habilitação comercial.

## Verificação e evidências reproduzíveis

Requer Java 21, Node compatível com o projeto e Docker disponível para PostgreSQL/Testcontainers. Os testes ambientais abrem um servidor HTTP local descartável e não consultam a Open-Meteo. Mockito é carregado como agente explícito pelo Surefire para funcionar no Java 21 sem autoanexação à JVM.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
npm --prefix frontend run build
npm --prefix frontend test -- --watch=false
```

Evidências detalhadas: relatórios JUnit em `target/surefire-reports`, saída Vitest e build Angular. Testes com PostgreSQL executam todas as migrations, incluindo V21, e verificam coordenadas persistidas, filtros combinados, isolamento de organizações e consulta sem mudança de auditoria. Testes de segurança criam proxies reais de method security e JWTs controlados para confirmar a negação à marina, permissão da plataforma, permissões internas e bloqueio dos relatórios por módulo.

Os testes Angular usam DOM em jsdom: verificam renderização de resultados/vazios/erros, filtros, atribuição e bloqueios visuais. A responsividade é implementada por flex-wrap, campos a 100% abaixo de 600 px e tabelas roláveis. Esses testes não substituem inspeção visual em navegador real em diferentes dispositivos; a entrega inicial não executou uma suíte de navegador real. A extensão SE06 acrescenta inspeção no Chromium com API real, documentada acima.

### Resultado da entrega inicial (anterior à SE06)

- Backend: **422 testes**, zero falhas, zero erros, zero ignorados, incluindo PostgreSQL/Testcontainers, migration V21 e HTTP controlado.
- Frontend: **129 testes** em 29 arquivos, incluindo 14 testes adicionados para as novas telas/serviços e configuração de módulos.
- Compilação Angular de desenvolvimento aprovada. Build de produção aprovado, com avisos de orçamento do bundle inicial (686,18 kB frente a 500 kB de aviso) e do estilo existente do dashboard (11,09 kB frente a 8 kB de aviso). Nenhum limite de erro foi excedido. O build incorpora as fontes Google já presentes no projeto e precisa de acesso à rede nessa etapa.
- Nenhuma chamada ao provedor real foi usada como evidência de teste; não houve contratação comercial, publicação ou alteração de dados de produção.

### Resultado da extensão SE06

- Backend: **441 testes**, zero falhas, zero erros e zero ignorados na execução final com `CAISORA_VISUAL=true`, incluindo a demonstração visual opt-in. Sem essa variável, os 440 testes funcionais são executados e apenas a demonstração visual é ignorada.
- Frontend: 136 testes em 30 arquivos, sem falhas.
- Build de produção aprovado. Permanecem os avisos de orçamento do bundle inicial e do estilo do dashboard já registrados na entrega inicial.
- Chromium real: desktop 1440×1100 e mobile 390×844, inspeção de filtros e painéis completos, inclusive vazio e rolagem horizontal contida na tabela. Quinze capturas e relatório de verificação em `docs/evidencias/se06/`; as capturas de painel completo ampliam apenas a altura do viewport, preservando sua largura.
