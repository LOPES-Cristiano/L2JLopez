package com.lopez.l2j.game.service;

import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Buffer por Esquemas (Scheme Buffer) - Onda 3 (QoL & Conforto In-Game).
 * Permite aos jogadores salvarem e aplicarem ate 3 perfis customizados de buffs (Fighter, Mage, etc.)
 * para o personagem ou seu Mascote/Summon.
 * 100% In-Game Economy: Custo em Adena, sem moedas customizadas ou doacoes.
 */
@Service
public class SchemeBufferService {

	private static final Logger log = LoggerFactory.getLogger(SchemeBufferService.class);

	public static final int ADENA_ID = 57;
	public static final long SCHEME_COST_ADENA = 25_000L;
	public static final long HEAL_COST_ADENA = 5_000L;

	public static final List<Integer> DEFAULT_FIGHTER_BUFFS = List.of(
			1068, 1040, 1086, 1077, 1242, 1240, 1045, 1204, 1268, // Might, Shield, Haste, Focus, Death Whisper, Guidance, Body, WW, Vampiric
			264, 269, 268, 304, // Song of Earth, Hunter, Wind, Vitality
			271, 274, 275, 310, // Dance of Warrior, Fire, Fury, Vampire
			1363 // Chant of Victory
	);

	public static final List<Integer> DEFAULT_MAGE_BUFFS = List.of(
			1040, 1036, 1085, 1059, 1078, 1303, 1045, 1048, 1204, // Shield, Magic Barrier, Acumen, Empower, Concentration, Wild Magic, Body, Soul, WW
			264, 267, 268, 363, // Song of Earth, Warding, Wind, Meditation
			273, 276, // Dance of Mystic, Concentration
			1413 // Magnus' Chant
	);

	private final CharacterVariablesService variablesService;
	private final SkillService skillService;

	@Autowired
	public SchemeBufferService(
			@Autowired(required = false) CharacterVariablesService variablesService,
			@Autowired(required = false) SkillService skillService) {
		this.variablesService = variablesService;
		this.skillService = skillService;
	}

	/**
	 * Retorna a lista de IDs de skills para o esquema solicitado (1, 2 ou 3).
	 */
	public List<Integer> getScheme(int charId, int schemeIdx) {
		String key = "buff_scheme_" + schemeIdx;
		if (variablesService != null) {
			String saved = variablesService.getVariable(charId, key, null);
			if (saved != null && !saved.isBlank()) {
				try {
					String[] parts = saved.split(",");
					List<Integer> list = new ArrayList<>();
					for (String p : parts) {
						list.add(Integer.parseInt(p.trim()));
					}
					return list;
				} catch (Exception e) {
					log.warn("Erro ao carregar esquema de buff {}_{}: {}", charId, schemeIdx, e.getMessage());
				}
			}
		}
		// Esquemas padrao caso nao haja configuracao personalizada salva
		if (schemeIdx == 1) return DEFAULT_FIGHTER_BUFFS;
		if (schemeIdx == 2) return DEFAULT_MAGE_BUFFS;
		return DEFAULT_FIGHTER_BUFFS;
	}

	public void saveScheme(int charId, int schemeIdx, List<Integer> skillIds) {
		if (variablesService == null || skillIds == null || skillIds.isEmpty()) return;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < skillIds.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(skillIds.get(i));
		}
		variablesService.setVariable(charId, "buff_scheme_" + schemeIdx, sb.toString());
	}

	/**
	 * Aplica cura completa de HP, MP e CP.
	 */
	public boolean heal(PlayerCharacter player, GameSession session) {
		if (player == null || player.isDead() || player.isAlikeDead()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode ser curado enquanto estiver morto!"));
			return false;
		}
		if (player.isInCombat()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode utilizar o buffer em combate!"));
			return false;
		}

		if (player.level() > 40 && HEAL_COST_ADENA > 0) {
			if (!consumeAdena(player, HEAL_COST_ADENA)) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Adena insuficiente para cura completa (" + HEAL_COST_ADENA + " Adena)!"));
				return false;
			}
		}

		player.currentHp(player.maxHp());
		player.currentMp(player.maxMp());
		player.currentCp(player.maxCp());
		session.sendVitals();
		session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "HP, MP e CP totalmente recuperados!"));
		return true;
	}

	/**
	 * Cancela e remove todos os buffs ativos do personagem.
	 */
	public void cancelBuffs(PlayerCharacter player, GameSession session) {
		if (player == null || player.isDead()) return;
		if (player.isInCombat()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode cancelar buffs em combate!"));
			return;
		}
		player.effects().clear();
		if (session != null) {
			session.refreshBuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Todos os seus buffs foram cancelados!"));
		}
	}

	/**
	 * Aplica o esquema de buff ao personagem ou summon.
	 */
	public boolean applyScheme(PlayerCharacter player, GameSession session, int schemeIdx, boolean targetPet) {
		if (player == null || player.isDead()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Nao e possivel receber buffs neste estado."));
			}
			return false;
		}
		if (player.isInCombat()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode receber buffs em combate!"));
			}
			return false;
		}
		if (player.isOlympiadMode()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Buffer desativado nas Olimpiadas!"));
			}
			return false;
		}

		if (player.level() > 40 && SCHEME_COST_ADENA > 0) {
			if (!consumeAdena(player, SCHEME_COST_ADENA)) {
				if (session != null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Adena insuficiente (" + SCHEME_COST_ADENA + " Adena requerida)!"));
				}
				return false;
			}
		}

		List<Integer> buffs = getScheme(player.objectId(), schemeIdx);
		for (int skillId : buffs) {
			applyBuff(player, session, skillId);
		}

		if (session != null) {
			session.refreshBuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Esquema " + schemeIdx + " aplicado com sucesso!"));
		}
		return true;
	}

	private void applyBuff(PlayerCharacter player, GameSession session, int skillId) {
		if (skillService != null) {
			int maxLvl = skillService.table() != null ? skillService.table().maxLevel(skillId) : 1;
			var opt = skillService.skill(skillId, maxLvl > 0 ? maxLvl : 1);
			if (opt.isPresent()) {
				SkillTemplate sk = opt.get();
				if (session != null) {
					session.applySkillEffects(sk, false);
				}
				// Garante que o efeito com funcs e aplicado ao jogador mesmo em sessao mock/nula
				boolean hasBuff = false;
				for (var b : player.effects().active()) {
					if (b.skillId() == sk.id()) {
						hasBuff = true;
						break;
					}
				}
				if (!hasBuff) {
					long duration = 1_200_000L;
					if (com.lopez.l2j.config.Config.ENABLE_MODIFY_SKILL_DURATION
							&& com.lopez.l2j.config.Config.SKILL_DURATION_LIST != null
							&& com.lopez.l2j.config.Config.SKILL_DURATION_LIST.containsKey(sk.id())) {
						duration = com.lopez.l2j.config.Config.SKILL_DURATION_LIST.get(sk.id()) * 1000L;
					}
					for (var e : sk.effects()) {
						String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
								? "skill_" + sk.id() + "_" + e.name()
								: e.stackType();
						long end = System.currentTimeMillis() + duration;
						player.effects().put(com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs()));
					}
					if (!player.effects().hasSkill(sk.id())) {
						player.effects().addBuff(sk.id(), sk.level(), duration);
					}
				}
				return;
			}
		}
		// Fallback para tabela de consumiveis caso skillService nao tenha o skill carregado
		var consumable = ConsumableTable.get(skillId);
		if (consumable.isPresent() && session != null) {
			session.useConsumable(consumable.get());
		} else {
			player.effects().addBuff(skillId, 1, 1_200_000L);
		}
	}

	private boolean consumeAdena(PlayerCharacter player, long amount) {
		if (player == null || player.inventory() == null) return false;
		return player.inventory().destroyItemByItemId(ADENA_ID, (int) amount);
	}

	/**
	 * Renderiza dialogo HTML completo do Buffer.
	 */
	public String renderHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=260 cellpadding=2 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\"><b>BUFFER DE ESQUEMAS</b></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">Selecione um esquema ou recupere seus status.</font></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td align=center colspan=2><font color=\"00FF00\"><b>Esquemas Salvos</b></font></td></tr>");
		sb.append("<tr><td align=center><button value=\"Esquema 1 (Fighter)\" action=\"bypass -h voiced_buffer 1\" width=120 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
		sb.append("<td align=center><button value=\"Esquema 2 (Mage)\" action=\"bypass -h voiced_buffer 2\" width=120 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
		sb.append("<tr><td align=center colspan=2><button value=\"Esquema 3 (Personalizado)\" action=\"bypass -h voiced_buffer 3\" width=140 height=24 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td align=center colspan=2><font color=\"FF9900\"><b>Opcoes Utilitarias</b></font></td></tr>");
		sb.append("<tr><td align=center><button value=\"Restaurar HP/MP/CP\" action=\"bypass -h voiced_buffer heal\" width=120 height=22 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
		sb.append("<td align=center><button value=\"Cancelar Buffs\" action=\"bypass -h voiced_buffer cancel\" width=120 height=22 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260>");
		sb.append("<tr><td align=center><font color=\"888888\">Custo por esquema: 25.000 Adena (Gratis ate nv 40)</font></td></tr>");
		sb.append("</table>");
		sb.append("</body></html>");
		return sb.toString();
	}
}
