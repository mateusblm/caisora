create table usuarios (
    id uuid primary key,
    organizacao_id uuid not null,
    nome varchar(150) not null,
    email varchar(150) not null,
    senha_hash varchar(255) not null,
    perfil varchar(50) not null,
    ativo boolean not null,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    constraint fk_usuario_organizacao
        foreign key (organizacao_id)
        references organizacoes(id)
);
