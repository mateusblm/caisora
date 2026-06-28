create table organizacoes (
    id uuid primary key,
    nome varchar(150) not null,
    razao_social varchar(200),
    documento varchar(30),
    email varchar(150) not null,
    telefone varchar(30),
    ativa boolean not null,
    criada_em timestamp with time zone not null,
    atualizada_em timestamp with time zone not null
);
