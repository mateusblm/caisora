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

## Multi-tenant

O modelo planejado usa banco unico, schema unico e isolamento por coluna `organizacao_id`. Nesta entrega foi criado o `ContextoOrganizacao`, que sera populado a partir do usuario autenticado quando a autenticacao JWT for implementada.

## Limitacoes atuais

- Modulos de negocio ainda nao possuem endpoints.
- Autenticacao JWT ainda nao foi implementada.
- OpenAPI sera configurado junto com os primeiros controllers.
- O usuario inicial local sera criado em uma entrega futura, depois das entidades `Organizacao` e `Usuario`.
