# Laboratório Database First — Parte III

Projeto Java 21 independente do backend e do frontend do Caisora. PostgreSQL exclusivo via Compose, migrations reais V1–V21, MyBatis Generator 1.4.2, CRUD HTTP e tela manual. Não é um módulo do ERP nem uma configuração de produção.

A geração abrange as **15 tabelas de negócio**: são **15 modelos Java, 15 interfaces Mapper e 15 XML SQL**. A tela continua limitada a módulos e permissões. Inventário, tipos especiais e testes: [geração completa](../../docs/database-first/GERACAO_COMPLETA.md).

## Executar

Requisitos: Git, Docker com engine Linux/Compose v2 em execução e Node.js 22 ou superior. Java 21 e Maven são fornecidos pela imagem do runner; não precisam estar no PATH do host. A primeira execução baixa imagens/dependências e precisa de internet. Portas locais padrão: PostgreSQL `55432`, HTTP `8095`.

Na raiz do repositório:

```sh
node tools/database-first/demo.mjs verify
node tools/database-first/demo.mjs start
```

Abra <http://localhost:8095>. A conta local é `demo`, com senha padrão `demo-local-123`. Na tela, informe apenas a senha. Use o menu **Módulos da marina** ou **Permissões por perfil** para cadastrar, buscar, editar e remover configurações. `verify` prepara o banco, remove as saídas geradas, executa o MBG e faz `clean verify` com testes reais no PostgreSQL. Ele para o servidor da demo antes do build.

Para mudar senha/portas, copie `.env.example` para `.env` **nesta pasta**, antes de preparar o banco. Os scripts carregam apenas esse arquivo; variáveis já exportadas têm prioridade. `DBF_PASSWORD` é a senha do PostgreSQL; `DBF_TOKEN` é a senha HTTP. Mudar a senha PostgreSQL de um volume já criado exige recriar **o banco da demo** com `reproduce`. `DBF_PG_PORT` e `DBF_HTTP_PORT` mudam apenas portas publicadas no host. O host interno, usuário e nome do banco no Compose são fixos para evitar conexão acidental ao produto.

## Mostrar geração ao vivo

```sh
node tools/database-first/demo.mjs prepare
node tools/database-first/demo.mjs clear-generated
node tools/database-first/demo.mjs inspect
node tools/database-first/demo.mjs generate
```

O banco existe antes dos modelos Java. O último comando recria `generated/` e executa no container:

```sh
mvn -B -ntp mybatis-generator:generate
```

Para executar diretamente, com o banco preparado e as pastas de saída existentes:

```sh
cd tools/database-first
docker compose run --rm -T runner mvn -B -ntp mybatis-generator:generate
docker compose run --rm -T runner mvn -B -ntp clean verify
```

`generated/java` e `generated/resources` contêm **somente saídas MBG**; não edite esses arquivos. `generated/manifest.json` é um inventário escrito pelo script, não uma saída do MBG. Os arquivos gerados estão versionados para revisão, mas não são necessários para iniciar: `verify` sempre os apaga e gera de novo.

## Outros comandos

| Comando após `node tools/database-first/demo.mjs` | Efeito |
| --- | --- |
| `prepare` | Sobe PostgreSQL; aplica/valida migrations e seed local idempotente |
| `clear-generated` | Remove apenas `tools/database-first/generated` e recria suas pastas vazias |
| `generate` | Remove saídas antigas e executa MBG; exige banco preparado |
| `verify` | Para o servidor; prepara, gera, compila e testa |
| `reproduce` | Para a demo, remove seu volume de dados, recria banco e gerados, compila e testa |
| `prove-schema` | Prova coluna SQL → campo Java com alteração temporária, restauração e testes |
| `start` | Sobe servidor e espera HTTP responder |
| `inspect` | Mostra definição das duas tabelas e registros via `psql` |
| `stop` | Para/remove containers e rede da demo, preservando volumes |

`reproduce` é intencionalmente destrutivo **apenas** para o volume literal `caisora-database-first_database-first-data`. Execute primeiro `verify` quando esse volume ainda não existe. Cache Maven é preservado. `prove-schema` altera somente o esquema descartável e restaura a coluna em `finally`; não altera migrations. Não execute os comandos de geração/build simultaneamente.

## Testes HTTP, tela e evidências

Com a demo iniciada:

```sh
node tools/database-first/smoke.mjs
cd tools/database-first
npm ci
npx playwright install chromium
npm run test:ui
```

O smoke usa os endpoints com mappers gerados e consulta o PostgreSQL por outra conexão `psql` após inserir, atualizar e excluir. O teste de tela percorre as duas tabelas e grava screenshots reais em `docs/database-first/evidence/`. A massa de apresentação usa Clientes, Embarcações, Vagas, Ocupações, Movimentações e Contratos; os registros são inseridos pela API local e removidos ao final. O teste preserva configurações preexistentes que reutilizar. `capturas-persistencia.json` contém leituras independentes por `psql` dos registros mostrados nas capturas.

Para repetir e capturar todas as evidências da demo, depois de instalar Playwright:

```sh
node tools/database-first/evidence.mjs
```

Esse comando inclui `reproduce`: recria **o volume da demo**. Grava stdout/stderr reais, relatórios e hashes dos gerados, e deixa o servidor iniciado. Não executa testes do produto principal; os resultados dessa validação adicional estão documentados separadamente.

## Arquivos e responsabilidades

| Caminho | Origem |
| --- | --- |
| `generated/java/.../model/{OrganizacaoModulo,PerfilPermissao}.java` | MBG: atributos, tipos, getters/setters |
| `generated/java/.../mapper/*Mapper.java` | MBG: contratos CRUD |
| `generated/resources/.../mapper/*Mapper.xml` | MBG: mapeamento e SQL executável CRUD parametrizado |
| `generatorConfig.xml`, `pom.xml` | Configuração manual da geração/build |
| `src/main/java/.../{Database,UuidTypeHandler,JsonbTypeHandler,CrudService,DemoServer}.java` | Integração manual, tipos PostgreSQL, transações, validação, autenticação e HTTP |
| `src/main/resources/web/` | Tela HTML/CSS/JS manual |
| `src/test/`, `tests/`, `smoke.mjs` | Testes manuais automatizados |
| `db/seed.sql` | Somente duas organizações locais e marcador de segurança |
| `demo.mjs`, `env.mjs`, `evidence.mjs`, `compose.yaml` | Orquestração manual |

Detalhes técnicos, análise de tabelas e resultados: [docs/database-first](../../docs/database-first/README.md). Apresentação: [DEMONSTRACAO.md](../../DEMONSTRACAO.md).
