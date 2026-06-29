alter table organizacoes
    add column slug varchar(80);

update organizacoes organizacao
set slug = 'org-' || left(replace(organizacao.id::text, '-', ''), 12);

alter table organizacoes
    alter column slug set not null;

alter table organizacoes
    add constraint chk_organizacoes_slug_formato
        check (
            slug = lower(slug)
            and slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'
        );

create unique index uk_organizacoes_slug_lower
    on organizacoes (lower(slug));
