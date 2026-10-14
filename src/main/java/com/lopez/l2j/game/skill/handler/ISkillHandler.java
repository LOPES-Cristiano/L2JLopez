package com.lopez.l2j.game.skill.handler;

import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.GameSession;

import java.util.List;

/**
 * Interface modular para handlers de execução de habilidades (padrão L2JDream / L2JLucera2 {@code ISkillHandler}).
 * Permite registrar e despachar cada {@code skillType} do Lineage II Interlude de forma desacoplada.
 */
public interface ISkillHandler {

	/**
	 * Tipos de skill (ex: "PDAM", "MDAM", "SUMMON", "RECALL", "TAKECASTLE") atendidos por este handler.
	 */
	List<String> skillTypes();

	/**
	 * Executa a habilidade para o conjurador e alvos resolvidos.
	 *
	 * @return {@code true} se o handler processou integralmente o skill; {@code false} para delegar ao pipeline padrão.
	 */
	boolean useSkill(
			GameSession caster,
			SkillTemplate skill,
			NpcInstance mainNpc,
			DoorInstance doorTarget,
			GameSession targetPlayer,
			boolean ss,
			boolean sps,
			boolean bss);
}
