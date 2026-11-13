# Geração das 15 tabelas de negócio

O comando de geração agora produz os modelos Java e a persistência MyBatis de todas as tabelas de negócio presentes nas migrations V1–V21. A interface continua restrita a módulos e permissões; as demais tabelas não receberam controllers ou telas.

## Executar na apresentação

Na raiz do projeto:

```powershell
node tools/database-first/demo.mjs generate
```

Resultado: **15 modelos, 15 interfaces Mapper e 15 XML SQL**. O gerador consulta o PostgreSQL real; nenhum dos 45 arquivos é escrito manualmente. O mesmo comando que já estava preparado no terminal, inclusive o atalho local `gerar`, passa a gerar tudo.

As saídas ficam em:

- `tools/database-first/generated/java/br/com/caisora/dbfirst/generated/model/`
- `tools/database-first/generated/java/br/com/caisora/dbfirst/generated/mapper/`
- `tools/database-first/generated/resources/br/com/caisora/dbfirst/generated/mapper/`

Para preparar banco existente e pastas vazias:

```powershell
node tools/database-first/demo.mjs prepare
node tools/database-first/demo.mjs clear-generated
```

Para regenerar, compilar e testar:

```powershell
node tools/database-first/demo.mjs verify
```

## Inventário

| Tabela | Modelo | Interface de persistência |
| --- | --- | --- |
| `clientes` | `Cliente` | `ClienteMapper` |
| `contratos` | `Contrato` | `ContratoMapper` |
| `embarcacoes` | `Embarcacao` | `EmbarcacaoMapper` |
| `historicos_contrato` | `HistoricoContrato` | `HistoricoContratoMapper` |
| `historicos_movimentacoes` | `HistoricoMovimentacao` | `HistoricoMovimentacaoMapper` |
| `movimentacoes` | `Movimentacao` | `MovimentacaoMapper` |
| `ocupacoes` | `Ocupacao` | `OcupacaoMapper` |
| `organizacao_modulos` | `OrganizacaoModulo` | `OrganizacaoModuloMapper` |
| `organizacoes` | `Organizacao` | `OrganizacaoMapper` |
| `perfil_permissoes` | `PerfilPermissao` | `PerfilPermissaoMapper` |
| `posicoes_embarcacoes` | `PosicaoEmbarcacao` | `PosicaoEmbarcacaoMapper` |
| `sequencias_contrato` | `SequenciaContrato` | `SequenciaContratoMapper` |
| `usuarios` | `Usuario` | `UsuarioMapper` |
| `vagas` | `Vaga` | `VagaMapper` |
| `vinculos_contrato_ocupacao` | `VinculoContratoOcupacao` | `VinculoContratoOcupacaoMapper` |

Cada interface tem um XML homônimo com SQL parametrizado para `insert`, `selectAll`, `selectByPrimaryKey`, `updateByPrimaryKey` e `deleteByPrimaryKey`. A cobertura é explícita no XML de configuração e conferida contra os metadados reais nos testes; nenhuma tabela de negócio pode faltar silenciosamente.

As tabelas técnicas `flyway_schema_history` e `database_first_demo_guard` são excluídas: são controle de migrations e proteção do laboratório, não entidades do ERP.

## Tipos e regras preservadas

- UUID e chaves estrangeiras usam `java.util.UUID`, com o adaptador JDBC manual existente.
- `timestamptz` usa `OffsetDateTime`; `date` usa `LocalDate`.
- Campos numéricos usam `BigDecimal`, preservando casas decimais de valores, medidas e coordenadas.
- `jsonb` do histórico usa `String` e um adaptador manual `JsonbTypeHandler` que envia o tipo nativo `jsonb`; o SQL continua gerado.
- A chave composta de `sequencias_contrato` gera parâmetros `organizacaoId` e `ano` nos métodos por chave primária.
- O histórico de movimentações permite inserção e consulta. O banco **recusa atualização e exclusão com SQLState 55000**, conforme o trigger original da V16. A existência desses métodos no mapper não remove essa regra.

Os modelos gerados são representações das tabelas. Não geram automaticamente serviços, regras de domínio, enums ou relacionamentos JPA, e não substituem as entidades originais do Caisora. As FKs permanecem como campos escalares e restrições verificadas pelo PostgreSQL.

## Validação executada

O build compilou **35 fontes Java: 30 geradas e cinco de integração**. A suíte terminou com **26 testes, zero falhas, zero erros e zero ignorados**.

Os 16 testes novos incluem um inventário de colunas, tipos, PKs e FKs, além de um caso de persistência para cada uma das 15 tabelas. Usam os modelos e statements MyBatis gerados para inserir, selecionar e, quando permitido, atualizar e excluir. As fixtures respeitam a ordem das FKs. Todos os campos são comparados após leitura, incluindo valores decimais, datas e JSONB. Para o histórico imutável, os testes confirmam a recusa de alterações e exclusões.

Os novos testes usam rollback integral para não deixar usuários, contratos ou históricos de teste no banco da apresentação. Os dez testes anteriores de CRUD com commit, HTTP, restrições e isolamento continuam passando.

Evidências reais:

- [Log de geração de todas as tabelas](evidence/generation-all.log)
- [Compilação e 26 testes](evidence/verify-all.log)
- [Relatório dos testes novos](evidence/br.com.caisora.dbfirst.AllTablesTest.txt)
- [Colunas, tipos, PKs e FKs reais](evidence/all-tables-metadata.txt)
- [Manifesto dos 45 arquivos](evidence/all-tables-manifest.json)

Não houve alteração nas migrations, no banco de produção, nas entidades JPA, na autenticação ou no frontend principal.
