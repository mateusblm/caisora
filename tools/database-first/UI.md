# Interface de configurações

O módulo independente utiliza a estrutura visual de `frontend/src/app/core/layout/layout`: menu lateral, marca original, marina selecionada, cabeçalho e área de conteúdo. As cores e a tipografia seguem `frontend/src/styles/_tokens.scss` e `frontend/src/styles.scss`. A imagem `brand.png` é uma cópia do símbolo já utilizado pelo produto.

As telas disponíveis são **Módulos da marina** e **Permissões por perfil**. Os controles têm função real: navegação, busca, atualização, inclusão, edição, remoção e saída da sessão. O cadastro e a edição usam diálogo com foco protegido e campos de seleção. A remoção exige confirmação e mantém o foco inicial em Cancelar. Erros aparecem no próprio formulário; resultados são anunciados em região de status. O layout preserva navegação e ações em largura mobile.

Os rótulos de módulos, perfis e ações são definidos manualmente a partir dos enums atuais do Caisora. Essa camada de apresentação não foi gerada a partir dos metadados SQL. Persistência Java/SQL é gerada pelo MyBatis; servidor, validação e interface são manuais. Não há mudança no Angular ou na segurança do produto principal.

As capturas de entrega utilizam a organização fictícia Marina Porto do Sol no PostgreSQL local. Os testes criam dados plausíveis pela API, capturam o fluxo e comprovam inserção, alteração e exclusão pelo `psql`, limpando os registros criados ao final. Os arquivos ficam em `docs/database-first/evidence/`; as legendas e a sequência estão em `docs/database-first/RELATORIO.md`.
