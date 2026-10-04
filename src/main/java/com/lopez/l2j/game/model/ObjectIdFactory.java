package com.lopez.l2j.game.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerador de object ids unicos no mundo (personagens, itens, NPCs...). Substitui o IdFactory do legado.
 */
@FunctionalInterface
public interface ObjectIdFactory {

	/** Faixa usada pelo IdFactory legado. */
	int FIRST_OBJECT_ID = 0x10000000;

	int nextId();

	/** Sequencial em memoria (testes e objetos nao persistidos). */
	static ObjectIdFactory sequential(int first) {
		AtomicInteger next = new AtomicInteger(first);
		return next::getAndIncrement;
	}
}
