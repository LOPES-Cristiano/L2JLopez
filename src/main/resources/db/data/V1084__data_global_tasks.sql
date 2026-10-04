-- Dados de tools/sql/global_tasks.sql do L2JDreamV2 (5 linhas). Gerado por GenDataMigrations; nao editar a mao.
-- Valores legados ('' em colunas numericas etc.) exigem modo SQL nao estrito nesta sessao.
SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION';
INSERT INTO `global_tasks` VALUES
('1', 'olympiad_save', 'TYPE_FIXED_SHEDULED', '1662475936774', '900000', '1800000', ''),
('2', 'raid_points_reset', 'TYPE_GLOBAL_TASK', '1662261000742', '1', '00:10:00', ''),
('3', 'sp_recommendations', 'TYPE_GLOBAL_TASK', '1661875200189', '1', '13:00:00', ''),
('4', 'seven_signs_update', 'TYPE_FIXED_SHEDULED', '1278785265718', '1800000', '1800000', ''),
('5', 'clanleaderapply', 'TYPE_GLOBAL_TASK', '1662260400773', '1', '00:00:00', '');
SET SESSION sql_mode = DEFAULT;
