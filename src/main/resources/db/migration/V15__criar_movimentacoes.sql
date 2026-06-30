create table if not exists movimentacoes (
    id uuid primary key,

    organizacao_id uuid not null,
    embarcacao_id uuid not null,

    tipo varchar(30) not null,
    status varchar(20) not null,
    prioridade varchar(20) not null,

    tipo_posicao_origem varchar(30) not null,
    vaga_origem_id uuid,
    descricao_origem varchar(255),

    tipo_posicao_destino varchar(30) not null,
    vaga_destino_id uuid,
    descricao_destino varchar(255),

    agendada_para timestamp with time zone not null,

    iniciada_em timestamp with time zone,
    concluida_em timestamp with time zone,
    cancelada_em timestamp with time zone,

    solicitada_por_id uuid not null,
    operador_responsavel_id uuid,

    observacoes varchar(2000),
    motivo_cancelamento varchar(1000),

    versao bigint not null default 0,

    criada_em timestamp with time zone not null,
    atualizada_em timestamp with time zone not null,

    constraint fk_movimentacao_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint fk_movimentacao_embarcacao
        foreign key (embarcacao_id)
        references embarcacoes(id),

    constraint fk_movimentacao_vaga_origem
        foreign key (vaga_origem_id)
        references vagas(id),

    constraint fk_movimentacao_vaga_destino
        foreign key (vaga_destino_id)
        references vagas(id),

    constraint fk_movimentacao_solicitante
        foreign key (solicitada_por_id)
        references usuarios(id),

    constraint fk_movimentacao_operador
        foreign key (operador_responsavel_id)
        references usuarios(id),

    constraint ck_movimentacao_tipo
        check (
            tipo in (
                'LANCAMENTO',
                'RETIRADA',
                'TRANSFERENCIA',
                'DESLOCAMENTO_INTERNO'
            )
        ),

    constraint ck_movimentacao_status
        check (
            status in (
                'AGENDADA',
                'EM_EXECUCAO',
                'CONCLUIDA',
                'CANCELADA'
            )
        ),

    constraint ck_movimentacao_prioridade
        check (
            prioridade in (
                'NORMAL',
                'ALTA',
                'URGENTE'
            )
        ),

    constraint ck_movimentacao_posicao_origem
        check (
            tipo_posicao_origem in (
                'VAGA',
                'AGUA',
                'PIER_ESPERA',
                'AREA_SERVICO',
                'EXTERNA',
                'DESCONHECIDA'
            )
        ),

    constraint ck_movimentacao_posicao_destino
        check (
            tipo_posicao_destino in (
                'VAGA',
                'AGUA',
                'PIER_ESPERA',
                'AREA_SERVICO',
                'EXTERNA',
                'DESCONHECIDA'
            )
        ),

    constraint ck_movimentacao_origem_vaga
        check (
            (
                tipo_posicao_origem = 'VAGA'
                and vaga_origem_id is not null
            )
            or
            (
                tipo_posicao_origem <> 'VAGA'
                and vaga_origem_id is null
            )
        ),

    constraint ck_movimentacao_destino_vaga
        check (
            (
                tipo_posicao_destino = 'VAGA'
                and vaga_destino_id is not null
            )
            or
            (
                tipo_posicao_destino <> 'VAGA'
                and vaga_destino_id is null
            )
        ),

    constraint ck_movimentacao_transferencia
        check (
            tipo <> 'TRANSFERENCIA'
            or
            (
                tipo_posicao_origem = 'VAGA'
                and tipo_posicao_destino = 'VAGA'
                and vaga_origem_id is not null
                and vaga_destino_id is not null
                and vaga_origem_id <> vaga_destino_id
            )
        ),

    constraint ck_movimentacao_lancamento
        check (
            tipo <> 'LANCAMENTO'
            or
            (
                tipo_posicao_origem = 'VAGA'
                and tipo_posicao_destino in (
                    'AGUA',
                    'PIER_ESPERA'
                )
            )
        ),

    constraint ck_movimentacao_retirada
        check (
            tipo <> 'RETIRADA'
            or
            (
                tipo_posicao_origem in (
                    'AGUA',
                    'PIER_ESPERA',
                    'EXTERNA'
                )
                and tipo_posicao_destino = 'VAGA'
            )
        ),

    constraint ck_movimentacao_status_datas
        check (
            (
                status = 'AGENDADA'
                and iniciada_em is null
                and concluida_em is null
                and cancelada_em is null
                and motivo_cancelamento is null
            )
            or
            (
                status = 'EM_EXECUCAO'
                and iniciada_em is not null
                and concluida_em is null
                and cancelada_em is null
                and motivo_cancelamento is null
            )
            or
            (
                status = 'CONCLUIDA'
                and iniciada_em is not null
                and concluida_em is not null
                and concluida_em >= iniciada_em
                and cancelada_em is null
                and motivo_cancelamento is null
            )
            or
            (
                status = 'CANCELADA'
                and iniciada_em is null
                and concluida_em is null
                and cancelada_em is not null
                and motivo_cancelamento is not null
                and length(trim(motivo_cancelamento)) > 0
            )
        ),

    constraint ck_movimentacao_versao
        check (versao >= 0)
);

/*
 * Uma embarcação não pode possuir duas ordens
 * operacionais simultaneamente abertas.
 */
create unique index if not exists
    uk_movimentacao_embarcacao_aberta
on movimentacoes (
    organizacao_id,
    embarcacao_id
)
where status in (
    'AGENDADA',
    'EM_EXECUCAO'
);

/*
 * Uma vaga de destino não pode ser reservada
 * por duas movimentações abertas.
 */
create unique index if not exists
    uk_movimentacao_vaga_destino_aberta
on movimentacoes (
    organizacao_id,
    vaga_destino_id
)
where
    vaga_destino_id is not null
    and status in (
        'AGENDADA',
        'EM_EXECUCAO'
    );

create index if not exists
    idx_movimentacao_fila_operacional
on movimentacoes (
    organizacao_id,
    status,
    prioridade,
    agendada_para
);

create index if not exists
    idx_movimentacao_embarcacao
on movimentacoes (
    organizacao_id,
    embarcacao_id,
    agendada_para
);

create index if not exists
    idx_movimentacao_vaga_origem
on movimentacoes (
    organizacao_id,
    vaga_origem_id
)
where vaga_origem_id is not null;

create index if not exists
    idx_movimentacao_vaga_destino
on movimentacoes (
    organizacao_id,
    vaga_destino_id
)
where vaga_destino_id is not null;

create index if not exists
    idx_movimentacao_solicitante
on movimentacoes (
    organizacao_id,
    solicitada_por_id
);

create index if not exists
    idx_movimentacao_operador
on movimentacoes (
    organizacao_id,
    operador_responsavel_id
)
where operador_responsavel_id is not null;

alter table posicoes_embarcacoes
    add column if not exists movimentacao_origem_id uuid;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_posicao_movimentacao_origem'
            and conrelid = 'posicoes_embarcacoes'::regclass
    ) then
        alter table posicoes_embarcacoes
            add constraint fk_posicao_movimentacao_origem
                foreign key (movimentacao_origem_id)
                references movimentacoes(id);
    end if;
end $$;

create index if not exists idx_posicao_movimentacao_origem
    on posicoes_embarcacoes (
        movimentacao_origem_id
    )
    where movimentacao_origem_id
        is not null;
