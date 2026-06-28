create table clientes (
    id uuid primary key,
    organizacao_id uuid not null,
    tipo_pessoa varchar(20) not null,
    nome varchar(150) not null,
    razao_social varchar(200),
    cpf_cnpj varchar(14) not null,
    email varchar(150),
    telefone varchar(20),
    celular varchar(20),
    observacoes varchar(2000),
    ativo boolean not null default true,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,

    constraint fk_cliente_organizacao
        foreign key (organizacao_id)
        references organizacoes(id),

    constraint uk_cliente_organizacao_cpf_cnpj
        unique (organizacao_id, cpf_cnpj),

    constraint ck_cliente_tipo_pessoa
        check (tipo_pessoa in ('FISICA', 'JURIDICA'))
);

create index idx_cliente_organizacao
    on clientes (organizacao_id);

create index idx_cliente_organizacao_nome
    on clientes (organizacao_id, nome);

create index idx_cliente_organizacao_ativo
    on clientes (organizacao_id, ativo);