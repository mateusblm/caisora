create table ocupacoes (
    id uuid primary key,

    organizacao_id uuid not null,

    embarcacao_id uuid not null,

    vaga_id uuid not null,

    status varchar(20) not null,

    inicio_em timestamp with time zone not null,

    fim_previsto_em timestamp with time zone,

    encerrada_em timestamp with time zone,

    observacoes varchar(2000),

    criada_em timestamp with time zone not null,

    atualizada_em timestamp with time zone not null,

    constraint fk_ocupacao_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint fk_ocupacao_embarcacao
        foreign key (embarcacao_id)
        references embarcacoes(id),

    constraint fk_ocupacao_vaga
        foreign key (vaga_id)
        references vagas(id),

    constraint ck_ocupacao_status
        check (
            status in (
                'ATIVA',
                'ENCERRADA'
            )
        ),

    constraint ck_ocupacao_fim_previsto
        check (
            fim_previsto_em is null
            or fim_previsto_em > inicio_em
        ),

    constraint ck_ocupacao_encerramento
        check (
            encerrada_em is null
            or encerrada_em >= inicio_em
        ),

    constraint ck_ocupacao_status_encerramento
        check (
            (
                status = 'ATIVA'
                and encerrada_em is null
            )
            or
            (
                status = 'ENCERRADA'
                and encerrada_em is not null
            )
        )
);

create unique index uk_ocupacao_vaga_ativa
    on ocupacoes (
        organizacao_id,
        vaga_id
    )
    where status = 'ATIVA';

create unique index uk_ocupacao_embarcacao_ativa
    on ocupacoes (
        organizacao_id,
        embarcacao_id
    )
    where status = 'ATIVA';

create index idx_ocupacao_organizacao_status
    on ocupacoes (
        organizacao_id,
        status
    );

create index idx_ocupacao_organizacao_inicio
    on ocupacoes (
        organizacao_id,
        inicio_em
    );

create index idx_ocupacao_embarcacao
    on ocupacoes (
        organizacao_id,
        embarcacao_id
    );

create index idx_ocupacao_vaga
    on ocupacoes (
        organizacao_id,
        vaga_id
    );