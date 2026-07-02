create table contratos (
    id uuid primary key,
    organizacao_id uuid not null,
    numero varchar(30) not null,
    cliente_id uuid not null,
    embarcacao_id uuid not null,
    status varchar(30) not null,
    tipo_vaga_contratada varchar(30) not null,
    periodicidade varchar(30) not null,
    data_inicio date not null,
    data_fim date,
    data_assinatura date,
    data_ativacao date,
    data_encerramento date,
    renovacao_automatica boolean not null default false,
    dias_aviso_previo integer,
    valor_base numeric(12, 2) not null,
    dia_vencimento integer not null,
    observacoes varchar(2000),
    criado_por_id uuid not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    versao bigint not null default 0,

    constraint fk_contrato_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint fk_contrato_cliente
        foreign key (cliente_id) references clientes(id),
    constraint fk_contrato_embarcacao
        foreign key (embarcacao_id) references embarcacoes(id),
    constraint fk_contrato_criado_por
        foreign key (criado_por_id) references usuarios(id),
    constraint uk_contrato_organizacao_numero
        unique (organizacao_id, numero),
    constraint ck_contrato_status check (
        status in (
            'RASCUNHO',
            'PENDENTE_ASSINATURA',
            'ATIVO',
            'SUSPENSO',
            'EM_ENCERRAMENTO',
            'ENCERRADO',
            'CANCELADO'
        )
    ),
    constraint ck_contrato_tipo_vaga check (
        tipo_vaga_contratada in (
            'MOLHADA',
            'SECA',
            'POITA',
            'OUTRA'
        )
    ),
    constraint ck_contrato_periodicidade check (
        periodicidade in (
            'DIARIA',
            'MENSAL',
            'TRIMESTRAL',
            'SEMESTRAL',
            'ANUAL',
            'PERSONALIZADA'
        )
    ),
    constraint ck_contrato_periodo check (
        data_fim is null or data_fim >= data_inicio
    ),
    constraint ck_contrato_periodicidade_personalizada check (
        periodicidade <> 'PERSONALIZADA' or data_fim is not null
    ),
    constraint ck_contrato_aviso_previo check (
        dias_aviso_previo is null or dias_aviso_previo >= 0
    ),
    constraint ck_contrato_valor_base check (valor_base >= 0),
    constraint ck_contrato_dia_vencimento check (
        dia_vencimento between 1 and 31
    ),
    constraint ck_contrato_encerramento check (
        data_encerramento is null
        or (
            data_encerramento >= data_inicio
            and (
                data_ativacao is null
                or data_encerramento >= data_ativacao
            )
        )
    )
);

create table sequencias_contrato (
    organizacao_id uuid not null,
    ano integer not null,
    ultimo_numero bigint not null,
    atualizado_em timestamp with time zone not null,

    primary key (organizacao_id, ano),
    constraint fk_sequencia_contrato_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint ck_sequencia_contrato_numero check (ultimo_numero > 0)
);

create table vinculos_contrato_ocupacao (
    id uuid primary key,
    organizacao_id uuid not null,
    contrato_id uuid not null,
    ocupacao_id uuid not null,
    inicio_em timestamp with time zone not null,
    fim_em timestamp with time zone,
    motivo_fim varchar(500),
    criado_em timestamp with time zone not null,

    constraint fk_vinculo_contrato_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint fk_vinculo_contrato
        foreign key (contrato_id) references contratos(id),
    constraint fk_vinculo_contrato_ocupacao
        foreign key (ocupacao_id) references ocupacoes(id),
    constraint ck_vinculo_contrato_periodo check (
        fim_em is null or fim_em >= inicio_em
    )
);

create table historicos_contrato (
    id uuid primary key,
    organizacao_id uuid not null,
    contrato_id uuid not null,
    tipo_evento varchar(50) not null,
    status_anterior varchar(30),
    status_novo varchar(30),
    descricao varchar(1000),
    realizado_por_id uuid not null,
    realizado_em timestamp with time zone not null,

    constraint fk_historico_contrato_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint fk_historico_contrato
        foreign key (contrato_id) references contratos(id),
    constraint fk_historico_contrato_usuario
        foreign key (realizado_por_id) references usuarios(id),
    constraint ck_historico_contrato_tipo_evento check (
        tipo_evento in (
            'CRIADO',
            'EDITADO',
            'ENVIADO_PARA_ASSINATURA',
            'ATIVADO',
            'SUSPENSO',
            'REATIVADO',
            'ENCERRAMENTO_SOLICITADO',
            'ENCERRADO',
            'CANCELADO',
            'OCUPACAO_VINCULADA',
            'OCUPACAO_DESVINCULADA'
        )
    ),
    constraint ck_historico_contrato_status_anterior check (
        status_anterior is null or status_anterior in (
            'RASCUNHO',
            'PENDENTE_ASSINATURA',
            'ATIVO',
            'SUSPENSO',
            'EM_ENCERRAMENTO',
            'ENCERRADO',
            'CANCELADO'
        )
    ),
    constraint ck_historico_contrato_status_novo check (
        status_novo is null or status_novo in (
            'RASCUNHO',
            'PENDENTE_ASSINATURA',
            'ATIVO',
            'SUSPENSO',
            'EM_ENCERRAMENTO',
            'ENCERRADO',
            'CANCELADO'
        )
    )
);

create index idx_contrato_organizacao_status
    on contratos (organizacao_id, status);
create index idx_contrato_organizacao_cliente
    on contratos (organizacao_id, cliente_id);
create index idx_contrato_organizacao_embarcacao
    on contratos (organizacao_id, embarcacao_id);
create index idx_contrato_data_inicio
    on contratos (organizacao_id, data_inicio);

create unique index uk_contrato_embarcacao_comercial_ativo
    on contratos (organizacao_id, embarcacao_id)
    where status in ('ATIVO', 'SUSPENSO', 'EM_ENCERRAMENTO');

create index idx_vinculo_contrato
    on vinculos_contrato_ocupacao (organizacao_id, contrato_id, inicio_em desc);
create index idx_vinculo_ocupacao
    on vinculos_contrato_ocupacao (organizacao_id, ocupacao_id);
create unique index uk_vinculo_contrato_aberto
    on vinculos_contrato_ocupacao (contrato_id)
    where fim_em is null;
create unique index uk_vinculo_ocupacao_aberto
    on vinculos_contrato_ocupacao (ocupacao_id)
    where fim_em is null;

create index idx_historico_contrato
    on historicos_contrato (organizacao_id, contrato_id, realizado_em desc);
