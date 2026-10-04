-- Achievements normalizado: uma linha por (jogador, conquista), no lugar da tabela legada
-- `achievements` (uma coluna aN por conquista, alterada via ALTER TABLE em tempo de execucao).
-- A tabela legada continua criada (migracao achievements) para permitir migrar os dados antigos.
CREATE TABLE IF NOT EXISTS `player_achievement` (
  `owner_id` int(11) NOT NULL,
  `achievement_id` int(11) NOT NULL,
  `times_completed` int(11) NOT NULL DEFAULT 1,
  `last_completed_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`owner_id`,`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
