ALTER TABLE organizacoes ADD COLUMN latitude NUMERIC(9,6), ADD COLUMN longitude NUMERIC(10,6);
ALTER TABLE organizacoes ADD CONSTRAINT ck_coordenadas CHECK (
 (latitude IS NULL AND longitude IS NULL) OR
 (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));
