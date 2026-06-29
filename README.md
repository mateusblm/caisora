# Caisora

Backend do Caisora, um ERP web SaaS para gestao de marinas. Esta primeira entrega cria a fundacao tecnica do backend para desenvolvimento incremental do MVP.

## Tecnologias

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

## Arquitetura

O backend sera um monolito modular organizado por dominio. O pacote `compartilhado` concentra apenas recursos transversais usados por mais de um modulo, como configuracao, auditoria, excecoes, seguranca, contexto de organizacao e utilidades pequenas.

Estrutura inicial:

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
|-- contrato
`-- financeiro
```

## Banco local

Suba o PostgreSQL:

```bash
docker compose up -d
```

As variaveis locais podem ser copiadas a partir de `.env.example`.

## Execucao

```bash
./mvnw spring-boot:run
```

No Windows:

```bash
./mvnw.cmd spring-boot:run
```

## Testes

```bash
./mvnw test
```

```bash
./mvnw clean verify
```

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

- Modulos de cliente, embarcacao, vagas, ocupacao, contrato e financeiro ainda nao possuem endpoints.
- OpenAPI sera configurado junto com os primeiros controllers.
- O usuario inicial local sera criado em uma entrega futura.
