# Caisora

ERP web SaaS para gestao de marinas. O repositorio tem o backend (Spring Boot, na raiz) e o frontend (Angular, em `frontend/`).

## Documentação do projeto

- [Fluxo atual do sistema](docs/fluxo-caisora.md)
- [Variabilidade (modulos e permissoes por perfil)](docs/variabilidade-lps.md)

## Estrutura do repositorio

```text
caisora
|-- src/            backend Spring Boot
|-- frontend/       frontend Angular
|-- scripts/        scripts do Flyway (PowerShell)
|-- docs/           documentacao
`-- docker-compose.yml  PostgreSQL local
```

## Tecnologias

### Backend

- Java 21
- Spring Boot
- Maven
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Lombok
- Spring Boot Actuator
- JUnit 5
- Testcontainers

### Frontend

- Angular 22 (componentes standalone)
- Angular Material
- RxJS
- TypeScript
- Vitest

## Arquitetura do backend

O backend e um monolito modular organizado por dominio. O pacote `compartilhado` concentra apenas recursos transversais usados por mais de um modulo, como configuracao, auditoria, excecoes, seguranca, contexto de organizacao e utilidades pequenas.

Estrutura:

```text
src/main/java/br/com/caisora
|-- CaisoraApplication.java
|-- compartilhado
|   |-- auditoria
|   |-- configuracao
|   |-- excecao
|   |-- locatario
|   |-- seguranca
|   `-- util
|-- autenticacao
|-- organizacao
|-- usuario
|-- cliente
|-- embarcacao
|-- marina
|-- vaga
|-- ocupacao
|-- movimentacao
|-- contrato
|-- variabilidade
`-- financeiro
```

Os pacotes `marina` e `financeiro` ainda estao vazios, reservados para entregas futuras.

## Arquitetura do frontend

```text
frontend/src/app
|-- core
|   |-- autenticacao   login, sessao, interceptor do token e guards de rota
|   `-- layout         casca da aplicacao com menu lateral
|-- features           uma pasta por modulo (pages, services, models, componentes)
|   |-- autenticacao
|   |-- dashboard
|   |-- clientes
|   |-- embarcacoes
|   |-- vagas
|   |-- ocupacoes
|   |-- movimentacoes
|   |-- contratos
|   |-- painel-tv
|   |-- checklist-saida
|   `-- variabilidade
`-- shared             componentes e modelos reaproveitados
```

O menu e as rotas sao filtrados pelos modulos ativos e pelas permissoes que o backend devolve no login e no `/me`. Isso so melhora a experiencia: quem bloqueia de fato e o `@PreAuthorize` do backend.

## Banco local

Suba o PostgreSQL:

```bash
docker compose up -d
```

As variaveis locais podem ser copiadas a partir de `.env.example`.

No profile `local`, a migration repetivel `db/seed-local/R__criar_admin_local.sql` cria um acesso de desenvolvimento:

- codigo da marina: `marina`
- e-mail: `admin@marina.com`
- senha: `teste123`
- perfil: `ADMINISTRADOR_MARINA`

## Fluxo seguro de migrations

O profile `local` e o padrao quando nenhum profile e informado, entao a aplicacao sobe sem configuracao extra apos o clone. Em producao, `SPRING_PROFILES_ACTIVE` deve estar definido (ex.: `prod`) para nao usar o segredo JWT local. O profile `local` mantem o Flyway ativo, mas exclui `db/migration` do restart automatico do DevTools para evitar que uma migration seja aplicada enquanto ainda esta sendo escrita.

Fluxo recomendado:

1. Pare a aplicacao ou rode com o profile `local`.
2. Crie a migration primeiro com extensao temporaria, por exemplo `V12__criar_movimentacoes.sql.tmp`.
3. Escreva e revise todo o SQL.
4. Renomeie para `.sql` somente quando a migration estiver completa.
5. Execute `.\scripts\flyway-info.ps1`.
6. Confirme que a migration aparece como `Pending`.
7. Execute `.\scripts\flyway-validate.ps1`. O script aceita migrations `Pending` para validar o historico antes do `migrate`.
8. Faca backup do banco antes de aplicar alteracoes relevantes.
9. Execute `.\scripts\flyway-migrate.ps1`.
10. Execute `.\scripts\flyway-info.ps1` novamente e confirme `Success`.
11. Nunca edite novamente uma migration ja aplicada.
12. Para qualquer correcao posterior, crie uma nova versao.

Os scripts usam `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Se `DB_URL` ou `DB_USERNAME` nao forem informados, os valores locais padrao sao usados. A senha deve vir de `DB_PASSWORD` ou do parametro `-Password`.

`flyway repair` nao executa novamente uma migration, nao recria tabela e nao corrige estrutura ausente. Ele apenas ajusta metadados do historico, como checksums. Checksum mismatch nao deve ser resolvido cegamente com `repair`: primeiro confirme qual arquivo mudou, qual checksum sera alterado, se o banco esta compativel com o conteudo correto e se existe backup.

## Execucao

### Backend

```bash
./mvnw spring-boot:run
```

No Windows:

```bash
./mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. O Maven Wrapper usa o Java do `JAVA_HOME`, que precisa ser 21.

### Frontend

Requer Node.js e npm.

```bash
cd frontend
npm install
npm start
```

A aplicacao abre em `http://localhost:4200`. O `npm start` usa o `proxy.conf.json`, que encaminha `/api` para o backend em `localhost:8080`, entao os dois precisam estar rodando.

Build de producao:

```bash
cd frontend
npm run build
```

Os arquivos saem em `frontend/dist/caisora-web/browser`.

## Testes

Backend:

```bash
./mvnw test
```

```bash
./mvnw clean verify
```

Frontend:

```bash
cd frontend
npm test
```

## Deploy do frontend

O frontend esta configurado para a Vercel (`frontend/vercel.json`). No build, o script `frontend/scripts/gerar-environment.mjs` gera o `environment.ts` a partir da variavel `API_BASE_URL`, que deve ter a URL publica do backend com HTTPS. O sufixo `/api/v1` e adicionado se faltar. O backend precisa liberar o dominio do frontend no CORS pela propriedade `caisora.cors.origens-permitidas` (separada por virgula; o padrao e `http://localhost:4200`).

## Health

Com a aplicacao em execucao:

```text
GET /actuator/health
```

## Autenticacao JWT

O Caisora usa JWT para autenticar chamadas da API. No login, o backend valida a senha com BCrypt, verifica se o usuario esta ativo, verifica se a organizacao esta ativa e entao gera um token assinado.

Login no MVP:

```http
POST /api/v1/autenticacao/login
```

```json
{
  "codigoOrganizacao": "marina-exemplo",
  "email": "admin@marinaexemplo.com",
  "senha": "senha"
}
```

O campo `codigoOrganizacao` e o codigo amigavel e estavel da marina. O backend normaliza esse codigo, localiza a organizacao pelo slug e usa o UUID internamente para buscar o usuario, isolar os dados multi-tenant e gerar o JWT.

Claims geradas no token:

- `sub`: id do usuario
- `nome`: nome do usuario
- `email`: e-mail normalizado
- `perfil`: perfil de autorizacao
- `organizacaoId`: organizacao do usuario
- `organizacaoNome`: nome da organizacao
- `iat`: data de emissao
- `exp`: data de expiracao

Uso do token:

```http
Authorization: Bearer <token>
```

Endpoints autenticados, como `/api/v1/usuarios`, usam a organizacao presente no JWT. O frontend nao deve enviar codigo da marina nem UUID de organizacao nesses endpoints.

Endpoints de `/api/v1/organizacoes` exigem usuario autenticado com perfil `ADMINISTRADOR_PLATAFORMA`.

Usuario atual:

```http
GET /api/v1/autenticacao/me
Authorization: Bearer <token>
```

A chave JWT deve vir da variavel `JWT_SECRET`. No profile `local`, ha um segredo local apenas para desenvolvimento.

## Multi-tenant

O modelo planejado usa banco unico, schema unico e isolamento por coluna `organizacao_id`. Os endpoints de usuario ja usam a organizacao presente no JWT para evitar acesso cruzado entre tenants.

## Limitacoes atuais

- O modulo financeiro ainda nao possui endpoints.
- OpenAPI ainda nao foi configurado.
- O perfil do usuario vai dentro do JWT, entao mudar o perfil ou desativar um usuario so vale depois que o token atual expirar.

## Funcionalidades para análise de Pontos de Função

O [inventário técnico](docs/inventario-apf.md) descreve consultas ambientais Open-Meteo, configuração de coordenadas, relatórios de ocupação/movimentações/contratos/permanência histórica, revisão das consultas e manutenções existentes, autorização dos módulos e evidências de testes. A integração externa começa desabilitada; os módulos AMBIENTE/RELATORIOS exigem liberação pela plataforma. O documento inclui configuração acadêmica/comercial e regras dos indicadores. Não há contagem automática de Pontos de Função.
