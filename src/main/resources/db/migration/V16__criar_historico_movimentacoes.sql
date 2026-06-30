create table historicos_movimentacoes (
    id uuid primary key,

    organizacao_id uuid not null,
    movimentacao_id uuid not null,

    tipo_evento varchar(30) not null,

    status_anterior varchar(20),
    status_novo varchar(20) not null,

    agendada_para_anterior
        timestamp with time zone,

    agendada_para_nova
        timestamp with time zone,

    usuario_id uuid not null,

    observacao varchar(2000),

    dados_anteriores jsonb,
    dados_novos jsonb,

    ocorrido_em
        timestamp with time zone not null,

    constraint fk_historico_movimentacao_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint fk_historico_movimentacao_movimentacao
        foreign key (movimentacao_id)
        references movimentacoes(id),

    constraint fk_historico_movimentacao_usuario
        foreign key (usuario_id)
        references usuarios(id),

    constraint ck_historico_movimentacao_tipo_evento
        check (
            tipo_evento in (
                'CRIADA',
                'ATUALIZADA',
                'REAGENDADA',
                'INICIADA',
                'CONCLUIDA',
                'CANCELADA'
            )
        ),

    constraint ck_historico_movimentacao_status_anterior
        check (
            status_anterior is null
            or status_anterior in (
                'AGENDADA',
                'EM_EXECUCAO',
                'CONCLUIDA',
                'CANCELADA'
            )
        ),

    constraint ck_historico_movimentacao_status_novo
        check (
            status_novo in (
                'AGENDADA',
                'EM_EXECUCAO',
                'CONCLUIDA',
                'CANCELADA'
            )
        ),

    constraint ck_historico_movimentacao_dados_anteriores
        check (
            dados_anteriores is null
            or jsonb_typeof(
                dados_anteriores
            ) = 'object'
        ),

    constraint ck_historico_movimentacao_dados_novos
        check (
            dados_novos is null
            or jsonb_typeof(
                dados_novos
            ) = 'object'
        ),

    constraint ck_historico_movimentacao_evento
        check (
            (
                tipo_evento = 'CRIADA'
                and status_anterior is null
                and status_novo = 'AGENDADA'
                and agendada_para_anterior
                    is null
                and agendada_para_nova
                    is not null
            )
            or
            (
                tipo_evento = 'ATUALIZADA'
                and status_anterior = 'AGENDADA'
                and status_novo = 'AGENDADA'
                and dados_anteriores
                    is not null
                and dados_novos
                    is not null
            )
            or
            (
                tipo_evento = 'REAGENDADA'
                and status_anterior = 'AGENDADA'
                and status_novo = 'AGENDADA'
                and agendada_para_anterior
                    is not null
                and agendada_para_nova
                    is not null
                and agendada_para_anterior
                    <> agendada_para_nova
            )
            or
            (
                tipo_evento = 'INICIADA'
                and status_anterior = 'AGENDADA'
                and status_novo = 'EM_EXECUCAO'
            )
            or
            (
                tipo_evento = 'CONCLUIDA'
                and status_anterior
                    = 'EM_EXECUCAO'
                and status_novo
                    = 'CONCLUIDA'
            )
            or
            (
                tipo_evento = 'CANCELADA'
                and status_anterior = 'AGENDADA'
                and status_novo = 'CANCELADA'
                and observacao is not null
                and length(
                    trim(observacao)
                ) > 0
            )
        )
);

create index idx_historico_movimentacao
    on historicos_movimentacoes (
        organizacao_id,
        movimentacao_id,
        ocorrido_em desc
    );

create index idx_historico_movimentacao_usuario
    on historicos_movimentacoes (
        organizacao_id,
        usuario_id,
        ocorrido_em desc
    );

create index idx_historico_movimentacao_evento
    on historicos_movimentacoes (
        organizacao_id,
        tipo_evento,
        ocorrido_em desc
    );

create index idx_historico_movimentacao_data
    on historicos_movimentacoes (
        organizacao_id,
        ocorrido_em desc
    );

/*
 * Histórico operacional é append-only:
 * depois de registrado, não pode ser
 * atualizado nem removido.
 */
create function bloquear_alteracao_historico_movimentacoes()
returns trigger
language plpgsql
as $$
begin
    raise exception
        'Historico de movimentacao e imutavel'
        using errcode = '55000';

    return old;
end;
$$;

create trigger trg_bloquear_alteracao_historico_movimentacoes
before update or delete
on historicos_movimentacoes
for each row
execute function
    bloquear_alteracao_historico_movimentacoes();