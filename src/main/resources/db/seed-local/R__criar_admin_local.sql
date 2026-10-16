-- Seed apenas para desenvolvimento local (profile local).
-- Login: codigoOrganizacao "marina", email "admin@marina.com", senha "teste123".
-- senha_hash e o BCrypt de "teste123".

insert into organizacoes (id, nome, email, ativa, criada_em, atualizada_em, slug)
select
    '00000000-0000-0000-0000-000000000001',
    'Marina Local',
    'admin@marina.com',
    true,
    now(),
    now(),
    'marina'
where not exists (
    select 1 from organizacoes where lower(slug) = 'marina'
);

insert into usuarios (id, organizacao_id, nome, email, senha_hash, perfil, ativo, criado_em, atualizado_em)
select
    '00000000-0000-0000-0000-000000000002',
    organizacao.id,
    'Administrador Local',
    'admin@marina.com',
    '$2b$10$t2wce1E22LoFdQf5iBg.o.Nf6B/8EFXqvFQl8lVEru7rUxDWHOP/2',
    'ADMINISTRADOR_MARINA',
    true,
    now(),
    now()
from organizacoes organizacao
where lower(organizacao.slug) = 'marina'
  and not exists (
    select 1 from usuarios usuario
    where usuario.organizacao_id = organizacao.id
      and usuario.email = 'admin@marina.com'
);
