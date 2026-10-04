package com.lopez.l2j.game.skill;

/**
 * Representa uma habilidade (ativa ou passiva) conhecida pelo personagem.
 *
 * @param id ID da habilidade
 * @param level Nivel da habilidade
 * @param name Nome da habilidade
 * @param passive Se e passiva
 * @param mpConsume Custo de MP para conjurar
 * @param castRange Alcance de conjuracao (em unidades do mapa)
 * @param hitTime Tempo de conjuracao em milissegundos
 * @param reuseDelay Tempo de recarga em milissegundos
 */
public record Skill(
		int id,
		int level,
		String name,
		boolean passive,
		int mpConsume,
		int castRange,
		int hitTime,
		int reuseDelay) {

	public Skill(int id, int level, String name, boolean passive) {
		this(id, level, name, passive, 0, 40, 1500, 1000);
	}
}
