-- Each fight gets a client-generated UUID. The unique index makes a replayed
-- (re-sent) submission fail instead of counting twice. Old rows stay NULL;
-- NULLs do not collide in a unique index.
ALTER TABLE scores ADD COLUMN fight_id VARCHAR(36) NULL;
CREATE UNIQUE INDEX uk_scores_fight_id ON scores (fight_id);
