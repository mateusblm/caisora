\set ON_ERROR_STOP on
-- Executado somente pelo Compose dedicado, depois das migrations reais.
DO $$ BEGIN
  IF current_database() <> 'caisora_database_first' OR current_user <> 'caisora_demo' THEN
    RAISE EXCEPTION 'Seed permitido apenas no banco exclusivo da demonstracao';
  END IF;
END $$;
CREATE TABLE IF NOT EXISTS database_first_demo_guard (id integer PRIMARY KEY CHECK (id = 1), finalidade text NOT NULL);
INSERT INTO database_first_demo_guard VALUES (1, 'caisora-database-first-local') ON CONFLICT DO NOTHING;
INSERT INTO organizacoes (id, nome, email, ativa, criada_em, atualizada_em, slug)
VALUES ('11111111-1111-4111-8111-111111111111', 'Marina da demonstracao (local)', 'demo@example.invalid', true, now(), now(), 'demo-local')
ON CONFLICT (id) DO NOTHING;
INSERT INTO organizacoes (id, nome, email, ativa, criada_em, atualizada_em, slug)
VALUES ('22222222-2222-4222-8222-222222222222', 'Segunda marina (teste de isolamento)', 'outra@example.invalid', true, now(), now(), 'demo-outra')
ON CONFLICT (id) DO NOTHING;
