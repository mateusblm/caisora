# Demonstração — Parte III: Database First (5 minutos)

Use dois terminais na **raiz do repositório** e um navegador. Todos os comandos abaixo funcionam em PowerShell ou shell Unix com Node 22+ e Docker Compose. Não é necessário iniciar o Spring Boot nem o Angular.

## Preparação antes da apresentação

Baixe dependências e valide fora dos cinco minutos:

```sh
node tools/database-first/demo.mjs verify
```

Confirme `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`. Prepare o cenário com tabelas existentes e código gerado ausente:

```sh
node tools/database-first/demo.mjs prepare
node tools/database-first/demo.mjs clear-generated
```

Deixe o editor aberto em `tools/database-first/generated/` (pastas vazias) e `generatorConfig.xml`. Se houver registros de ensaios anteriores, `reproduce` recria somente o banco da demo; rode antes de `clear-generated`. Não faça reset do banco do ERP.

## 0:00–0:50 — Banco de dados já existente

No primeiro terminal:

```sh
node tools/database-first/demo.mjs inspect
```

**Resultado esperado:** definição de `organizacao_modulos` e `perfil_permissoes`, PK UUID, FKs para organizações, índices únicos e tabelas inicialmente vazias em banco recém-criado.

**Fala:** “Estas são tabelas reais da V20 do Caisora. Aplicamos as migrations originais em um PostgreSQL exclusivo de demonstração. O banco está pronto, mas estas pastas ainda não têm modelos ou mappers gerados. Nenhum banco de produção é usado.”

## 0:50–1:40 — Geração a partir do PostgreSQL

```sh
node tools/database-first/demo.mjs generate
```

**Resultado esperado:** `Connecting to the Database`, `Introspecting table public.organizacao_modulos`, `Introspecting table public.perfil_permissoes`, `Saving file ...` e `BUILD SUCCESS`. Surgem quatro Java e dois XML.

**Fala:** “O comando executa MyBatis Generator de verdade. A ferramenta se conecta pelo JDBC e consulta os metadados das tabelas. Não está lendo nossas entidades JPA e não estamos copiando classes à mão.”

## 1:40–2:20 — Código obtido do banco

Abra no editor:

```text
tools/database-first/generated/java/br/com/caisora/dbfirst/generated/model/OrganizacaoModulo.java
tools/database-first/generated/java/br/com/caisora/dbfirst/generated/mapper/OrganizacaoModuloMapper.java
tools/database-first/generated/resources/br/com/caisora/dbfirst/generated/mapper/OrganizacaoModuloMapper.xml
```

Mostre `id`, `organizacaoId`, `modulo`, `ativo`, comentários `@mbg.generated`, métodos CRUD e XML com `insert`, `select`, `update`, `delete` e `where id = ...`.

**Fala:** “UUID, texto e booleano correspondem às colunas do PostgreSQL; UUID tem uma configuração explícita de tipo para o driver. A chave primária determina o SQL por ID. As FKs continuam sendo garantidas pelo banco; o gerador não inventa um relacionamento de objetos. Modelos, interfaces e SQL são automáticos. Integração HTTP, validação, autenticação e tela foram feitas manualmente.”

## 2:20–4:20 — Compilação e CRUD pela tela

```sh
node tools/database-first/demo.mjs start
```

**Resultado esperado:** `Demo pronta em http://localhost:8095`. `start` compila antes de executar; falhas de compilação impedem a aplicação de responder. Se a porta foi configurada no `.env` da demo, use essa porta.

Abra <http://localhost:8095>, entre com usuário implícito `demo` e senha `demo-local-123` (ou o `DBF_TOKEN` configurado).

1. Selecione `organizacao_modulos` e cadastre módulo `CLIENTES`, ativo `Sim`.
2. Veja a linha e a mensagem de inserção. No **segundo terminal** execute `node tools/database-first/demo.mjs inspect`: aparece `CLIENTES | t` no PostgreSQL.
3. Clique em **Editar**, altere ativo para `Não` e salve. Repita `inspect`: o mesmo UUID aparece com `CLIENTES | f`.
4. Clique em **Excluir**, mostre a confirmação e confirme. Atualize a listagem.
5. Se houver tempo, selecione `perfil_permissoes`: módulo `CLIENTES`, perfil `ATENDENTE`, ação `CONSULTAR`, permitido `Sim`. O fluxo é o mesmo. Exclua ao fim.

**Fala:** “A tela usa os mappers e o SQL que acabamos de gerar. Cada gravação confirma uma transação real e consulta novamente o banco. Podemos também tentar cadastrar duas vezes a mesma combinação: a restrição única do PostgreSQL impede e recebemos uma mensagem de conflito.”

## 4:20–5:00 — Conferência e conclusão da técnica

```sh
node tools/database-first/demo.mjs inspect
```

**Resultado esperado:** os registros que foram excluídos não estão no PostgreSQL. Em ambiente recém-preparado e após limpar os registros da apresentação, ambas as tabelas ficam com `(0 rows)`.

**Fala:** “As mudanças foram efetivamente persistidas e a exclusão foi confirmada por outra conexão ao PostgreSQL. O laboratório tem testes de CRUD, chaves, tipos inválidos e isolamento. No Caisora principal as migrations Flyway e entidades JPA são mantidas manualmente, com Hibernate em validação; aqui demonstramos a geração automática de código a partir do banco. Não substituímos entidades nem alteramos a segurança do ERP.”

## Alternativa e perguntas do professor

Se o navegador não estiver disponível, execute com a demo iniciada:

```sh
node tools/database-first/smoke.mjs
```

O script imprime `POST 201`, consultas `GET 200`, duplicidade `409`, `PUT 200`, `DELETE 204`, consulta posterior `404` e leituras independentes pelo `psql` após cada alteração, para ambas as tabelas.

Para responder “e se o banco mudar?”, fora dos cinco minutos:

```sh
node tools/database-first/demo.mjs prove-schema
```

Esse comando para a demo, adiciona uma coluna temporária ao banco descartável, regenera o campo Java, compila, remove a coluna e regenera a versão original com testes. Não modifica migrations.

Para mostrar a reprodução integral em banco limpo:

```sh
node tools/database-first/demo.mjs reproduce
```

Esse comando apaga apenas o volume de dados da demo e faz migrations, geração, compilação e testes novamente. Logs e capturas reais: [docs/database-first/evidence](docs/database-first/evidence). Não apresente como executado um teste que apenas consta neste roteiro; use os registros de [RESULTADOS.md](docs/database-first/RESULTADOS.md).

Encerramento:

```sh
node tools/database-first/demo.mjs stop
```
