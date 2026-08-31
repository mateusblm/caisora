# Variabilidade do Caisora

O Caisora utiliza uma única base de código para atender marinas com necessidades diferentes. A variabilidade é configurada por organização em dois níveis: módulos disponíveis para a marina e ações permitidas para cada perfil.

## Pontos de variação

- módulos habilitados por organização;
- permissões por perfil e ação;
- função opcional `CHECKLIST_SAIDA`, desabilitada por padrão e ativável apenas para clientes que contratarem ou precisarem desse processo;
- menu e rotas do frontend derivados das permissões resolvidas para o usuário;
- autorização no backend, que impede acesso direto à API mesmo quando uma tela ou rota é chamada manualmente.

## Módulos

`DASHBOARD`, `CLIENTES`, `EMBARCACOES`, `VAGAS`, `OCUPACOES`, `MOVIMENTACOES`, `CONTRATOS`, `PAINEL_TV`, `USUARIOS`, `CHECKLIST_SAIDA` e `CONFIGURACOES`.

`DASHBOARD` e `CONFIGURACOES` são obrigatórios. `CHECKLIST_SAIDA` é a variante específica de cliente usada na demonstração e começa desabilitada.

## Ações

As ações reutilizadas pelos módulos são `VISUALIZAR`, `CRIAR`, `EDITAR`, `ALTERAR_STATUS`, `INICIAR`, `CONCLUIR`, `CANCELAR` e `CONFIGURAR`. Cada módulo declara apenas as ações que suporta.

Uma permissão é representada por uma chave no formato `MODULO:ACAO`, por exemplo `MOVIMENTACOES:CONCLUIR`.

## Resolução da configuração

1. O sistema verifica se o módulo está ativo para a organização.
2. Verifica se existe uma sobrescrita de permissão para o perfil naquela organização.
3. Quando não há sobrescrita, utiliza a política padrão.
4. O backend aplica a decisão com `@PreAuthorize` e o bean `ControleAcesso`.
5. Login e `/api/v1/autenticacao/me` retornam módulos e permissões resolvidos para o frontend.
6. O Angular filtra o menu e bloqueia rotas conforme a mesma configuração.

As tabelas `organizacao_modulos` e `perfil_permissoes` armazenam somente diferenças em relação à política padrão. Isso evita duplicar configurações para todas as marinas e facilita criar novas instâncias a partir do mesmo produto-base.

## Perfis padrão

- `ADMINISTRADOR_MARINA`: acesso a todas as funções ativas da organização e sempre mantém acesso às configurações.
- `GERENTE`: funções operacionais, sem administração de usuários/configurações.
- `ATENDENTE`: cadastros e criação/edição de ordens, sem concluir operações.
- `FINANCEIRO`: contratos e consultas necessárias para montar e consultar contratos.
- `ADMINISTRADOR_PLATAFORMA`: perfil global, não configurável por uma marina.

As permissões dos perfis de cada organização podem ser alteradas na tela **Configurações > Variabilidade**, sem criar fork ou nova versão do Caisora.

## Variante específica: Checklist de saída

A função `CHECKLIST_SAIDA` representa um requisito específico de uma marina que exige conferência formal antes da entrega de uma embarcação. Uma organização pode habilitar o módulo e autorizar somente os perfis que precisam utilizá-lo. Outras marinas continuam usando o mesmo software sem visualizar essa função.

## Testes em Docker

O arquivo `docker-compose.test.yml` cria ambientes descartáveis para testes do backend e do frontend. O backend monta o socket Docker porque a suíte existente usa Testcontainers/PostgreSQL.

```bash
./scripts/testar-com-docker.sh
```

Também é possível executar apenas uma parte:

```bash
docker compose -f docker-compose.test.yml run --rm backend-tests
docker compose -f docker-compose.test.yml run --rm frontend-tests
```
