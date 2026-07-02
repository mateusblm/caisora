alter table movimentacoes
    drop constraint if exists ck_movimentacao_retirada;

alter table movimentacoes
    add constraint ck_movimentacao_retirada
        check (
            tipo <> 'RETIRADA'
            or
            (
                tipo_posicao_origem in (
                    'AGUA',
                    'PIER_ESPERA'
                )
                and tipo_posicao_destino = 'VAGA'
            )
        ) not valid;

alter table movimentacoes
    drop constraint if exists ck_movimentacao_deslocamento_interno;

alter table movimentacoes
    add constraint ck_movimentacao_deslocamento_interno
        check (
            tipo <> 'DESLOCAMENTO_INTERNO'
            or tipo_posicao_destino in (
                'AREA_SERVICO',
                'PIER_ESPERA',
                'EXTERNA'
            )
        ) not valid;
