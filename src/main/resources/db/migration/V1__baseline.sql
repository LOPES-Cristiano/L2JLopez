-- Baseline do L2JLopez. As tabelas do L2JDream (tools/sql, 121 arquivos)
-- serao migradas aqui em versoes V2, V3... conforme cada modulo for portado.
CREATE TABLE schema_info (
    id INT PRIMARY KEY,
    description VARCHAR(100) NOT NULL
);

INSERT INTO schema_info (id, description) VALUES (1, 'L2JLopez baseline');
