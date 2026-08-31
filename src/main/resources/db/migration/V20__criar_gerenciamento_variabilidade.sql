create table organizacao_modulos (
    id uuid primary key,
    organizacao_id uuid not null,
    modulo varchar(50) not null,
    ativo boolean not null,
    constraint fk_organizacao_modulos_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint uk_organizacao_modulo
        unique (organizacao_id, modulo)
);

create table perfil_permissoes (
    id uuid primary key,
    organizacao_id uuid not null,
    perfil varchar(50) not null,
    modulo varchar(50) not null,
    acao varchar(50) not null,
    permitido boolean not null,
    constraint fk_perfil_permissoes_organizacao
        foreign key (organizacao_id) references organizacoes(id),
    constraint uk_perfil_permissao
        unique (organizacao_id, perfil, modulo, acao)
);

create index idx_organizacao_modulos_organizacao
    on organizacao_modulos (organizacao_id);

create index idx_perfil_permissoes_consulta
    on perfil_permissoes (organizacao_id, perfil, modulo, acao);
