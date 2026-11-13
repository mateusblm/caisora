# Resultados reais e matriz de validação

Execução em Windows, Docker Desktop Linux, Node 24.18.0, Chromium 141 via Playwright 1.56.1. O runner usa Maven 3.9.11/Temurin 21 e PostgreSQL **16.14**, obtido pela imagem `postgres:16-alpine`. Logs registram os horários UTC de 13/11/2026. Não foram usados H2, mocks de banco ou produção para validar o CRUD.

## Demo — critérios obrigatórios

| Critério | Verificação executada | Evidência |
| --- | --- | --- |
| Conexão ao PostgreSQL | JDBC e `psql`, nome/usuário/marcador exclusivos | [metadados JDBC](evidence/schema-metadata.txt), [psql](evidence/postgres-schema.log) |
| Esquema real | Flyway aplica/valida V1–V21 originais em volume vazio | [reproduce.log](evidence/reproduce.log) |
| Geração automática real | MBG introspecta ambas as tabelas após apagar os gerados; quatro Java e dois XML | [generation.log](evidence/generation.log), [hashes](evidence/generated-sha256.json) |
| Compilação | `javac release 21`, oito fontes: quatro manuais e quatro geradas; JAR construído | [verify.log](evidence/verify.log) |
| Inserção | Mapper gerado, commit, leitura por conexão independente; HTTP 201 | [CRUD HTTP/psql](evidence/crud-http-postgres.log) |
| Consulta | `selectAll`/`selectByPrimaryKey`, GET 200, verificação de ID e campos | [testes](evidence/br.com.caisora.dbfirst.DatabaseFirstTest.txt), [HTTP](evidence/crud-http-postgres.log) |
| Atualização | `true → false`, mesmo UUID, SQL gerado e releitura independente | [CRUD HTTP/psql](evidence/crud-http-postgres.log) |
| Exclusão | `DELETE 204`, GET posterior 404 e consulta PostgreSQL sem linha | [CRUD HTTP/psql](evidence/crud-http-postgres.log) |
| Dados inválidos | JSON malformado/nulo, campos ausentes/desconhecidos, boolean com tipo errado, texto vazio/maior que 50, UUID inválido, corpo maior que 8 KiB | [testes HTTP](evidence/br.com.caisora.dbfirst.HttpCrudTest.txt) |
| Restrições reais | PK/UNIQUE `23505`, FK `23503`, NOT NULL `23502`, comprimento `22001`, pai com filhos não pode ser excluído | [testes PostgreSQL](evidence/br.com.caisora.dbfirst.DatabaseFirstTest.txt) |
| Autenticação/tenant | HTTP sem credencial recebe 401; cliente não define organização; leitura/edição/exclusão de outro tenant recusadas e listagem filtrada | [testes PostgreSQL](evidence/br.com.caisora.dbfirst.DatabaseFirstTest.txt), [HTTP](evidence/br.com.caisora.dbfirst.HttpCrudTest.txt) |
| Reprodução limpa | Remove volume exclusivo e gerados; reaplica migrations, gera, compila e testa | [reproduce.log](evidence/reproduce.log) |
| Banco determina o código | Coluna temporária SQL gera campo Java, compila; coluna removida e originais regenerados/testados | [schema-change.log](evidence/schema-change.log) |
| Interface funcional | Chromium percorre CRUD e conflito nas duas tabelas, cancela/confirma exclusão, consulta novamente e valida ausência de erro JS/overflow global mobile | [ui-tests.log](evidence/ui-tests.log), [relatório](evidence/playwright-results.json) |

**Resultado da demo:** 10 testes JUnit, **0 falhas, 0 erros, 0 ignorados**, tanto em `verify` quanto em `reproduce` e depois da prova de alteração de esquema. O teste Playwright passou (um cenário integrado cobrindo ambas as tabelas). O smoke HTTP/psql terminou com `PASS: CRUD nas duas tabelas confirmado por HTTP e PostgreSQL independente.`

Os relatórios TXT são saídas do Surefire; os detalhes das asserções estão no código dos testes. Não se interpreta apenas `BUILD SUCCESS` como evidência de CRUD: há asserções sobre persistência real após commits e logs independentes por `psql`.

## Capturas reais

| Situação | Captura |
| --- | --- |
| Módulo inserido | [cadastro](evidence/tela-modulos-cadastro.png) |
| Módulo alterado para “Não” | [alteração](evidence/tela-modulos-alteracao.png) |
| Confirmação de exclusão do módulo | [confirmação](evidence/tela-modulos-confirmacao.png) |
| Permissão inserida | [cadastro](evidence/tela-permissoes-cadastro.png) |
| Permissão alterada para “Não” | [alteração](evidence/tela-permissoes-alteracao.png) |
| Confirmação de exclusão da permissão | [confirmação](evidence/tela-permissoes-confirmacao.png) |
| Após excluir e consultar novamente | [estado vazio](evidence/tela-apos-exclusao.png) |
| Layout mobile, viewport 390×844 | [mobile](evidence/tela-mobile.png) |

Capturas completas da página, feitas durante operações reais. UUIDs e nomes de teste são dinâmicos, não valores a copiar para a apresentação. A largura desktop foi 1440px. Revisão adicional da interface: disposição `ship`, sem achados materiais nas capturas/código; o detector apontou somente a tipografia Inter herdada, mantida para preservar a identidade existente. Tela e persistência gerada estão identificadas separadamente na própria interface.

## Preservação do produto principal

Validações adicionais executadas **sem alterar arquivos do produto**:

| Comando | Resultado real |
| --- | --- |
| `./mvnw.cmd test`, com `JAVA_HOME` apontando ao JDK 21 instalado | **471 contabilizados, 0 falhas, 0 erros, 5 ignorados**, `BUILD SUCCESS`; inclui Testcontainers em bancos descartáveis. [Log](evidence/product-backend-tests.log) |
| `cd frontend` e `npm test -- --watch=false` | **32 arquivos de teste e 147 testes passaram**. [Log](evidence/product-frontend-tests.log) |
| `cd frontend` e `npm run build` | Build concluído. Avisos de orçamento: bundle inicial 687,11 kB para limite 500 kB; SCSS do dashboard 11,09 kB para limite 8 kB. São fontes inalteradas nesta atividade. [Log](evidence/product-frontend-build.log) |
| `git diff --exit-code -- src frontend pom.xml docker-compose.yml .gitignore` | Sem diferenças nos arquivos principais já versionados |

Os cinco testes opcionais do backend **não foram executados**, por condições já existentes de variáveis de ambiente ausentes:

- `AuditoriaVisualApfTest`: `CAISORA_APF_VISUAL`.
- `ReferenciasReaisTest`: `CAISORA_AIE_REAL`.
- `MaresControladasVisualTest` e `ReferenciasVisualTest`: `CAISORA_AIE_VISUAL`.
- `PermanenciaVagasVisualTest`: `CAISORA_VISUAL`.

Esses cenários adicionais do produto não foram habilitados; isso não representa dezena de testes da demo ignorada. Todos os testes específicos de Database First foram executados. Não se habilitou consumo de API paga nem se conectou a serviços de produção.

## Como repetir os resultados

Da raiz do repositório, com Docker em execução:

```sh
node tools/database-first/demo.mjs verify
node tools/database-first/demo.mjs reproduce
node tools/database-first/demo.mjs prove-schema
node tools/database-first/demo.mjs start
node tools/database-first/smoke.mjs
cd tools/database-first
npm ci
npx playwright install chromium
npm run test:ui
```

Após instalar Playwright, `node tools/database-first/evidence.mjs` captura novamente todos os logs/relatórios/capturas da demo. **Ele recria somente seu banco descartável** e deixa a aplicação iniciada. Não sobrescreve os logs `product-*`: estes registram a validação adicional do ERP. Consulte [README](README.md) e o [roteiro](../../DEMONSTRACAO.md).

## Limites restantes

Telas foram implementadas manualmente; MBG gerou apenas persistência Java/SQL. FK foi identificada nos metadados e exercitada pelo PostgreSQL, sem geração de objetos relacionados/joins. A integração tem tenant fixo e listagem pequena, sem paginação. Não houve teste em aparelho físico, outros navegadores, implantação ou banco de produção. Nenhum desses itens é necessário para executar esta demonstração acadêmica local. Os arquivos gerados não devem ser promovidos a substitutos das entidades JPA do ERP.
