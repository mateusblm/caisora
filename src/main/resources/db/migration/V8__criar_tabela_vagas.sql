create table vagas (
    id uuid primary key,

    organizacao_id uuid not null,

    codigo varchar(50) not null,

    tipo varchar(30) not null,

    setor varchar(100),

    localizacao varchar(200),

    comprimento_maximo_metros numeric(8, 2) not null,

    boca_maxima_metros numeric(8, 2) not null,

    calado_maximo_metros numeric(8, 2),

    altura_maxima_metros numeric(8, 2),

    peso_maximo_kg numeric(12, 2),

    possui_agua boolean not null default false,

    possui_energia boolean not null default false,

    observacoes varchar(2000),

    ativa boolean not null default true,

    criada_em timestamp with time zone not null,

    atualizada_em timestamp with time zone not null,

    constraint fk_vaga_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint uk_vaga_organizacao_codigo
        unique (organizacao_id, codigo),

    constraint ck_vaga_codigo
        check (btrim(codigo) <> ''),

    constraint ck_vaga_tipo
        check (
            tipo in (
                'MOLHADA',
                'SECA',
                'POITA',
                'OUTRA'
            )
        ),

    constraint ck_vaga_comprimento_positivo
        check (comprimento_maximo_metros > 0),

    constraint ck_vaga_boca_positiva
        check (boca_maxima_metros > 0),

    constraint ck_vaga_calado_positivo
        check (
            calado_maximo_metros is null
            or calado_maximo_metros > 0
        ),

    constraint ck_vaga_altura_positiva
        check (
            altura_maxima_metros is null
            or altura_maxima_metros > 0
        ),

    constraint ck_vaga_peso_positivo
        check (
            peso_maximo_kg is null
            or peso_maximo_kg > 0
        )
);

create index idx_vaga_organizacao
    on vagas (organizacao_id);

create index idx_vaga_codigo
    on vagas (organizacao_id, codigo);

create index idx_vaga_tipo
    on vagas (organizacao_id, tipo);

create index idx_vaga_setor
    on vagas (organizacao_id, setor);

create index idx_vaga_ativa
    on vagas (organizacao_id, ativa);