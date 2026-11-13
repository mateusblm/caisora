# Interface da demonstração Database First

## Propósito e limites

Interface acadêmica de CRUD para `organizacao_modulos` e `perfil_permissoes`, em banco local exclusivo e organização fixa. É uma extensão da identidade existente do Caisora; não estabelece uma nova identidade nem configura o produto em produção.

## Herança visual

As referências são [`frontend/src/styles/_tokens.scss`](../../frontend/src/styles/_tokens.scss) e [`frontend/src/styles.scss`](../../frontend/src/styles.scss). O CSS independente da demo reproduz o marinho do cabeçalho (`#07354a`), oceano das ações (`#075d78`), fundo claro (`#f3f8fa`), superfície branca, texto (`#17313d`) e bordas (`#d7e3e6`). A pilha tipográfica permanece `Inter, Roboto, Arial, sans-serif`.

Os painéis e o diálogo usam o raio principal herdado de 14px. Controles e avisos usam 6px como ajuste local de densidade; o token de raio pequeno do produto continua em 9px. Texto secundário, foco e cores de estado recebem tratamentos locais de legibilidade sem alterar os tokens do produto. A documentação não redefine a fonte nem a paleta principal.

## Organização e interação

No desktop, listagem e formulário ficam lado a lado, com maior espaço para a tabela (proporção 1,7:1 e intervalo de 24px). Até 760px, o conteúdo passa a uma coluna: controles de tabela, listagem, formulário e explicação acadêmica. A tabela tem rolagem horizontal quando necessária; identificadores podem quebrar linha.

Campos possuem labels associados, obrigatoriedade e limite de texto de 50 caracteres conforme o banco. Botões, inputs e selects apresentam foco visível turquesa de 3px. Ao editar, o formulário identifica o registro e leva o foco ao campo Módulo. Mensagens de resultado usam região `role="status"` com anúncio `polite`; erros acrescentam texto e tratamento vermelho, sem depender apenas da cor.

A exclusão pede confirmação em diálogo modal nativo, identifica o registro e distingue “Excluir registro” de “Manter registro”. O foco inicial fica em “Manter registro”. Ações de persistência apresentam estado desabilitado enquanto aguardam resposta; a listagem é consultada novamente após alterações.

## Tela manual e persistência gerada

HTML, CSS, JavaScript, HTTP, validação, autenticação local e integração UUID são manuais. O MyBatis Generator produz modelos Java, interfaces Mapper e SQL de inserção, consulta, atualização e exclusão a partir das tabelas da migration V20. A tela comunica explicitamente essa separação.

## Evidências comparadas

- [`tela-permissoes-alteracao.png`](../../docs/database-first/evidence/tela-permissoes-alteracao.png): confirma cabeçalho marinho, superfícies claras, listagem e formulário em duas colunas, campos de permissão e mensagem após alteração.
- [`tela-mobile.png`](../../docs/database-first/evidence/tela-mobile.png): confirma a mesma identidade em fluxo vertical, controles reorganizados e listagem antes do formulário.

As capturas foram comparadas ao código em `src/main/resources/web` e às referências visuais do frontend. Foco, erro e diálogo foram documentados a partir do código, pois não aparecem nessas capturas. A revisão já registrou aprovação sem achados materiais; os avisos sobre Inter herdado foram aceitos. Esta etapa documental não executou navegador, testes ou detector.
