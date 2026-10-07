package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de gerenciamento de variaveis dinamicas de personagens (character_variables).
 * Suporta persistencia relacional com expiração temporal (expire_time).
 */
@Service
public class CharacterVariablesService {

	private static final Logger log = LoggerFactory.getLogger(CharacterVariablesService.class);
	private static final String DEFAULT_TYPE = "user-var";

	private final JdbcTemplate jdbcTemplate;
	private final Map<String, VariableEntry> cache = new ConcurrentHashMap<>();

	public record VariableEntry(int charId, String type, String name, String value, long expireTime) {
		public boolean isExpired(long now) {
			return expireTime > 0 && expireTime <= now;
		}
	}

	public CharacterVariablesService(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private String cacheKey(int charId, String type, String name) {
		return charId + ":" + type + ":" + name.toLowerCase();
	}

	public void set(int charId, String type, String name, String value, long expireTimeMillis) {
		if (name == null || value == null) {
			return;
		}
		String key = cacheKey(charId, type, name);
		VariableEntry entry = new VariableEntry(charId, type, name, value, expireTimeMillis);
		cache.put(key, entry);

		try {
			String sql = "INSERT INTO character_variables (obj_id, type, name, `value`, expire_time) "
					+ "VALUES (?, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), expire_time = VALUES(expire_time)";
			jdbcTemplate.update(sql, charId, type, name, value, expireTimeMillis);
		} catch (Exception e) {
			// Fallback para dialetos que usam MERGE ou sintaxe padrao (ex: H2)
			try {
				jdbcTemplate.update("DELETE FROM character_variables WHERE obj_id = ? AND type = ? AND name = ?", charId, type, name);
				jdbcTemplate.update("INSERT INTO character_variables (obj_id, type, name, `value`, expire_time) VALUES (?, ?, ?, ?, ?)",
						charId, type, name, value, expireTimeMillis);
			} catch (Exception ex) {
				log.warn("Erro ao persistir character_variable para charId={}: {}", charId, ex.getMessage());
			}
		}
	}

	public void set(int charId, String name, String value) {
		set(charId, DEFAULT_TYPE, name, value, 0L);
	}

	public void set(int charId, String name, int value) {
		set(charId, DEFAULT_TYPE, name, String.valueOf(value), 0L);
	}

	public void set(int charId, String name, boolean value) {
		set(charId, DEFAULT_TYPE, name, String.valueOf(value), 0L);
	}

	public String get(int charId, String type, String name) {
		if (name == null) {
			return null;
		}
		long now = System.currentTimeMillis();
		String key = cacheKey(charId, type, name);
		VariableEntry cached = cache.get(key);
		if (cached != null) {
			if (cached.isExpired(now)) {
				delete(charId, type, name);
				return null;
			}
			return cached.value();
		}

		try {
			String sql = "SELECT `value`, expire_time FROM character_variables WHERE obj_id = ? AND type = ? AND name = ?";
			return jdbcTemplate.query(sql, rs -> {
				if (rs.next()) {
					String val = rs.getString("value");
					long expire = rs.getLong("expire_time");
					if (expire > 0 && expire <= now) {
						delete(charId, type, name);
						return null;
					}
					cache.put(key, new VariableEntry(charId, type, name, val, expire));
					return val;
				}
				return null;
			}, charId, type, name);
		} catch (Exception e) {
			log.warn("Erro ao consultar character_variable para charId={}: {}", charId, e.getMessage());
			return null;
		}
	}

	public String get(int charId, String name) {
		return get(charId, DEFAULT_TYPE, name);
	}

	public String getVariable(int charId, String name, String defaultValue) {
		String val = get(charId, name);
		return val != null ? val : defaultValue;
	}

	public void setVariable(int charId, String name, String value) {
		set(charId, name, value);
	}

	public void deleteVariable(int charId, String name) {
		delete(charId, name);
	}

	public int getInt(int charId, String name, int defaultValue) {
		String val = get(charId, name);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getBoolean(int charId, String name, boolean defaultValue) {
		String val = get(charId, name);
		if (val == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(val) || "1".equals(val);
	}

	public boolean has(int charId, String name) {
		return get(charId, name) != null;
	}

	public void delete(int charId, String type, String name) {
		if (name == null) {
			return;
		}
		cache.remove(cacheKey(charId, type, name));
		try {
			jdbcTemplate.update("DELETE FROM character_variables WHERE obj_id = ? AND type = ? AND name = ?", charId, type, name);
		} catch (Exception e) {
			log.warn("Erro ao remover character_variable para charId={}: {}", charId, e.getMessage());
		}
	}

	public void delete(int charId, String name) {
		delete(charId, DEFAULT_TYPE, name);
	}

	public int cleanupExpired(long now) {
		cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
		try {
			return jdbcTemplate.update("DELETE FROM character_variables WHERE expire_time > 0 AND expire_time <= ?", now);
		} catch (Exception e) {
			log.warn("Erro ao limpar character_variables expiradas: {}", e.getMessage());
			return 0;
		}
	}
}
