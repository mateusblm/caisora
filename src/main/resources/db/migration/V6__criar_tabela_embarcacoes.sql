create table embarcacoes (
    id uuid primary key,
    organizacao_id uuid not null,
    proprietario_id uuid not null,

    nome varchar(150),
    tipo varchar(30) not null,

    fabricante varchar(100),
    modelo varchar(100),
    ano_fabricacao integer,

    numero_inscricao varchar(50),
    numero_casco varchar(100),
    porto_inscricao varchar(150),
    codigo_pais_bandeira varchar(2) not null default 'BR',

    comprimento_total_metros numeric(8, 2) not null,
    boca_metros numeric(8, 2) not null,
    calado_metros numeric(8, 2),
    pontal_metros numeric(8, 2),
    altura_total_metros numeric(8, 2),
    peso_kg numeric(12, 2),

    capacidade_pessoas integer,
    tipo_propulsao varchar(30) not null,

    cor_predominante varchar(50),
    observacoes varchar(2000),

    ativa boolean not null default true,
    criada_em timestamp with time zone not null,
    atualizada_em timestamp with time zone not null,

    constraint fk_embarcacao_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint fk_embarcacao_proprietario
        foreign key (proprietario_id)
        references clientes(id),

    constraint uk_embarcacao_organizacao_numero_inscricao
        unique (organizacao_id, numero_inscricao),

    constraint uk_embarcacao_organizacao_numero_casco
        unique (organizacao_id, numero_casco),

    constraint ck_embarcacao_tipo
        check (
            tipo in (
                'LANCHA',
                'VELEIRO',
                'CATAMARA',
                'IATE',
                'MOTO_AQUATICA',
                'BOTE',
                'CANOA',
                'ESCUNA',
                'TRAINEIRA',
                'PESQUEIRO',
                'FLUTUANTE',
                'OUTRA'
            )
        ),

    constraint ck_embarcacao_tipo_propulsao
        check (
            tipo_propulsao in (
                'MOTOR',
                'VELA',
                'VELA_E_MOTOR',
                'REMO',
                'SEM_PROPULSAO',
                'OUTRA'
            )
        ),

    constraint ck_embarcacao_comprimento_positivo
        check (comprimento_total_metros > 0),

    constraint ck_embarcacao_boca_positiva
        check (boca_metros > 0),

    constraint ck_embarcacao_calado_positivo
        check (calado_metros is null or calado_metros > 0),

    constraint ck_embarcacao_pontal_positivo
        check (pontal_metros is null or pontal_metros > 0),

    constraint ck_embarcacao_altura_positiva
        check (altura_total_metros is null or altura_total_metros > 0),

    constraint ck_embarcacao_peso_positivo
        check (peso_kg is null or peso_kg > 0),

    constraint ck_embarcacao_capacidade_positiva
        check (
            capacidade_pessoas is null
            or capacidade_pessoas > 0
        ),

    constraint ck_embarcacao_ano_fabricacao
        check (
            ano_fabricacao is null
            or ano_fabricacao >= 1800
        ),

    constraint ck_embarcacao_pais_bandeira
        check (char_length(codigo_pais_bandeira) = 2)
);

create index idx_embarcacao_organizacao
    on embarcacoes (organizacao_id);

create index idx_embarcacao_proprietario
    on embarcacoes (organizacao_id, proprietario_id);

create index idx_embarcacao_nome
    on embarcacoes (organizacao_id, nome);

create index idx_embarcacao_tipo
    on embarcacoes (organizacao_id, tipo);

create index idx_embarcacao_ativa
    on embarcacoes (organizacao_id, ativa);