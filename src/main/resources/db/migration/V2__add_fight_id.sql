ALTER TABLE scores ADD COLUMN fight_id VARCHAR(36) NULL;
CREATE UNIQUE INDEX uk_scores_fight_id ON scores (fight_id);
