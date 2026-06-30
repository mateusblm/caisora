create table posicoes_embarcacoes (
    id uuid primary key,

    organizacao_id uuid not null,
    embarcacao_id uuid not null,

    tipo varchar(30) not null,

    vaga_id uuid,
    descricao_local varchar(255),

    versao bigint not null default 0,

    criada_em timestamp with time zone not null,
    atualizada_em timestamp with time zone not null,

    constraint fk_posicao_embarcacao_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint fk_posicao_embarcacao_embarcacao
        foreign key (embarcacao_id)
        references embarcacoes(id),

    constraint fk_posicao_embarcacao_vaga
        foreign key (vaga_id)
        references vagas(id),

    constraint uk_posicao_embarcacao
        unique (embarcacao_id),

    constraint ck_posicao_embarcacao_tipo
        check (
            tipo in (
                'VAGA',
                'AGUA',
                'PIER_ESPERA',
                'AREA_SERVICO',
                'EXTERNA',
                'DESCONHECIDA'
            )
        ),

    constraint ck_posicao_embarcacao_vaga
        check (
            (
                tipo = 'VAGA'
                and vaga_id is not null
            )
            or
            (
                tipo <> 'VAGA'
                and vaga_id is null
            )
        )
);

create index idx_posicao_embarcacao_organizacao
    on posicoes_embarcacoes (
        organizacao_id
    );

create index idx_posicao_embarcacao_organizacao_tipo
    on posicoes_embarcacoes (
        organizacao_id,
        tipo
    );

create index idx_posicao_embarcacao_vaga
    on posicoes_embarcacoes (
        organizacao_id,
        vaga_id
    )
    where vaga_id is not null;

/*
 * Inicializa a posição de todas as embarcações existentes.
 *
 * Embarcação com ocupação ativa:
 *     posição = VAGA
 *
 * Embarcação sem ocupação ativa:
 *     posição = DESCONHECIDA
 *
 * O UUID da embarcação é usado como UUID inicial da posição.
 * Isso é seguro porque são tabelas diferentes e evita depender
 * de extensões do PostgreSQL para gerar UUID durante o backfill.
 */
insert into posicoes_embarcacoes (
    id,
    organizacao_id,
    embarcacao_id,
    tipo,
    vaga_id,
    descricao_local,
    versao,
    criada_em,
    atualizada_em
)
select
    embarcacao.id,
    embarcacao.organizacao_id,
    embarcacao.id,

    case
        when ocupacao.id is not null
            then 'VAGA'
        else 'DESCONHECIDA'
    end,

    ocupacao.vaga_id,
    null,
    0,
    current_timestamp,
    current_timestamp

from embarcacoes embarcacao

left join ocupacoes ocupacao
    on ocupacao.organizacao_id
        = embarcacao.organizacao_id
    and ocupacao.embarcacao_id
        = embarcacao.id
    and ocupacao.status = 'ATIVA';