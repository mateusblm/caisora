alter table usuarios
    add constraint uk_usuarios_organizacao_email unique (organizacao_id, email);

create index idx_usuarios_organizacao_id on usuarios (organizacao_id);
create index idx_usuarios_email on usuarios (email);
