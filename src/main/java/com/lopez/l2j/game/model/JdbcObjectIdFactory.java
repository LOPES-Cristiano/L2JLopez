package com.lopez.l2j.game.model;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Continua do maior object id ja persistido em characters/items (a partir de 0x10000000). Ids de objetos nao
 * persistidos (NPCs) tambem saem daqui, entao nunca colidem com os persistidos durante a execucao.
 */
@Component
class JdbcObjectIdFactory implements ObjectIdFactory {

	private final JdbcClient jdbc;
	private volatile AtomicInteger next;

	JdbcObjectIdFactory(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public int nextId() {
		AtomicInteger n = next;
		if (n == null) {
			synchronized (this) {
				n = next;
				if (n == null) {
					int max = Math.max(maxOf("SELECT MAX(charId) FROM characters"),
							maxOf("SELECT MAX(object_id) FROM items"));
					next = n = new AtomicInteger(Math.max(FIRST_OBJECT_ID, max + 1));
				}
			}
		}
		return n.getAndIncrement();
	}

	private int maxOf(String sql) {
		try {
			Integer v = jdbc.sql(sql).query(Integer.class).optional().orElse(null);
			return v == null ? 0 : v;
		} catch (RuntimeException e) {
			return 0; // tabela ausente (ex.: testes com schema reduzido)
		}
	}
}
