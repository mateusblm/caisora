alter table movimentacoes
    drop constraint if exists ck_movimentacao_tipo;

alter table movimentacoes
    add constraint ck_movimentacao_tipo
        check (
            tipo in (
                'LANCAMENTO',
                'RETIRADA',
                'RETORNO_PARA_VAGA',
                'TRANSFERENCIA',
                'DESLOCAMENTO_INTERNO'
            )
        );

alter table movimentacoes
    drop constraint if exists ck_movimentacao_retorno_para_vaga;

alter table movimentacoes
    add constraint ck_movimentacao_retorno_para_vaga
        check (
            tipo <> 'RETORNO_PARA_VAGA'
            or
            (
                tipo_posicao_origem in (
                    'AREA_SERVICO',
                    'EXTERNA'
                )
                and tipo_posicao_destino = 'VAGA'
                and vaga_origem_id is null
                and vaga_destino_id is not null
            )
        );
