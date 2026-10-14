package com.lopez.l2j.game.clan.skill;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.world.GameWorld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de habilidades de cla (Clan Skills / Pledge Skill Tree do L2JDream).
 * Gerencia aprendizado, consumo de reputacao/itens necessarios e aplicacao aos membros elegiveis.
 */
@Service
public class ClanSkillService {

	private static final Logger log = LoggerFactory.getLogger(ClanSkillService.class);

	public enum LearnResult {
		SUCCESS,
		NOT_IN_CLAN,
		NOT_LEADER,
		SKILL_NOT_FOUND,
		MAX_LEVEL_REACHED,
		CLAN_LEVEL_TOO_LOW,
		NOT_ENOUGH_REPUTATION,
		MISSING_REQUIRED_ITEM,
		ERROR
	}

	/**
	 * Resumo consolidado dos bonus de atributos concedidos pelas Clan Skills ativas (IDs 370 a 391).
	 */
	public record ClanSkillBonuses(
			double maxHpMul,
			double maxCpMul,
			double maxMpMul,
			double regHpMul,
			double regCpMul,
			double regMpMul,
			double pAtkMul,
			double pDefMul,
			double mAtkMul,
			double mDefMul,
			int accCombatAdd,
			int rEvasAdd,
			double rShldMul,
			double sDefMul,
			int runSpdAdd,
			boolean hasImperium
	) {
		public static final ClanSkillBonuses NONE = new ClanSkillBonuses(
				1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0, 0, 1.0, 1.0, 0, false);
	}

	private final ClanTable clanTable;
	private final ClanSkillTreeTable skillTreeTable;
	private final JdbcClient jdbc;
	private InventoryService inventoryService;
	private SkillService skillService;
	private GameWorld world;

	public ClanSkillService(ClanTable clanTable, ClanSkillTreeTable skillTreeTable, JdbcClient jdbc) {
		this(clanTable, skillTreeTable, jdbc, null, null, null);
	}

	@Autowired
	public ClanSkillService(ClanTable clanTable, ClanSkillTreeTable skillTreeTable,
			@Autowired(required = false) JdbcClient jdbc,
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) SkillService skillService,
			@Autowired(required = false) GameWorld world) {
		this.clanTable = clanTable;
		this.skillTreeTable = skillTreeTable;
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		this.skillService = skillService;
		this.world = world;
	}

	/**
	 * Retorna a lista de habilidades de cla que o cla esta qualificado a aprender no momento.
	 */
	public List<PledgeSkillRecord> getAvailableSkills(Clan clan) {
		if (clan == null) {
			return Collections.emptyList();
		}
		List<PledgeSkillRecord> available = new ArrayList<>();
		for (var candidate : skillTreeTable.allSkills()) {
			int currentLvl = clan.getSkillLevel(candidate.skillId());
			if (candidate.level() == currentLvl + 1 && clan.level() >= candidate.minClanLevel()) {
				available.add(candidate);
			}
		}
		return available;
	}

	/**
	 * Realiza o aprendizado de uma habilidade de cla pelo lider.
	 */
	public synchronized LearnResult learnSkill(PlayerCharacter leader, int skillId) {
		if (leader == null || leader.clanId() <= 0) {
			return LearnResult.NOT_IN_CLAN;
		}
		var clanOpt = clanTable.byClanId(leader.clanId());
		if (clanOpt.isEmpty()) {
			return LearnResult.NOT_IN_CLAN;
		}
		Clan clan = clanOpt.get();

		if (!clan.isLeader(leader.objectId())) {
			return LearnResult.NOT_LEADER;
		}

		int currentLevel = clan.getSkillLevel(skillId);
		int nextLevel = currentLevel + 1;

		var skillOpt = skillTreeTable.getSkill(skillId, nextLevel);
		if (skillOpt.isEmpty()) {
			// Se o nivel 1 sequer existe na arvore
			if (skillTreeTable.getSkillsForSkillId(skillId).isEmpty()) {
				return LearnResult.SKILL_NOT_FOUND;
			}
			return LearnResult.MAX_LEVEL_REACHED;
		}

		PledgeSkillRecord skill = skillOpt.get();

		if (clan.level() < skill.minClanLevel()) {
			return LearnResult.CLAN_LEVEL_TOO_LOW;
		}

		if (clan.reputationScore() < skill.repCost()) {
			return LearnResult.NOT_ENOUGH_REPUTATION;
		}

		// Checa se o lider possui o item necessario no inventario
		if (skill.itemId() > 0) {
			var inv = leader.inventory();
			if (inv == null || inv.getItemCount(skill.itemId()) < 1) {
				return LearnResult.MISSING_REQUIRED_ITEM;
			}
			// Consome o item com persistencia real se disponivel
			if (inventoryService != null) {
				if (inventoryService.consumeItem(inv, skill.itemId(), 1, "ClanSkillLearn") == null) {
					return LearnResult.MISSING_REQUIRED_ITEM;
				}
			} else {
				inv.destroyItemByItemId(skill.itemId(), 1);
			}
		}

		// Consome pontos de reputacao
		if (skill.repCost() > 0) {
			clan.reputationScore(clan.reputationScore() - skill.repCost());
		}

		// Adiciona habilidade ao cla
		clan.addSkill(skillId, nextLevel);

		if (jdbc != null) {
			try {
				jdbc.sql("""
						REPLACE INTO clan_skills (clan_id, skill_id, skill_level, skill_name)
						VALUES (:clanId, :skillId, :lvl, :name)
						""")
						.param("clanId", clan.clanId())
						.param("skillId", skillId)
						.param("lvl", nextLevel)
						.param("name", skill.name())
						.update();

				jdbc.sql("""
						UPDATE clan_data
						SET reputation_score = :rep
						WHERE clan_id = :clanId
						""")
						.param("rep", clan.reputationScore())
						.param("clanId", clan.clanId())
						.update();
			} catch (Exception ex) {
				log.error("Erro ao persistir habilidade de cla {} lvl {} para cla {}",
						skillId, nextLevel, clan.clanId(), ex);
				return LearnResult.ERROR;
			}
		}

		// Aplica imediatamente as Clan Skills ao lider e aos membros online
		applyClanSkillsToPlayer(leader);
		if (world != null) {
			for (ClanMember m : clan.members()) {
				if (m.objectId() != leader.objectId()) {
					PlayerCharacter onlineMember = world.getPlayer(m.objectId());
					if (onlineMember != null) {
						applyClanSkillsToPlayer(onlineMember);
						world.player(m.objectId()).ifPresent(op -> {
							op.sendSkillList();
							op.sendUserInfoAndBroadcastCharInfo();
						});
					}
				}
			}
		}

		log.info("Cla '{}' (ID {}) aprendeu habilidade de cla '{}' Lvl {} (Custo: {} Rep)",
				clan.name(), clan.clanId(), skill.name(), nextLevel, skill.repCost());
		return LearnResult.SUCCESS;
	}

	/**
	 * Aplica todas as Clan Skills aprendidas pelo cla do jogador em seu mapa de skills e funcoes passivas,
	 * retornando os bonus consolidados de atributos.
	 * Se o jogador nao pertencer a um cla ou for membro da Academia, remove quaisquer Clan Skills ativas.
	 */
	public ClanSkillBonuses applyClanSkillsToPlayer(PlayerCharacter player) {
		if (player == null) {
			return ClanSkillBonuses.NONE;
		}

		// Remove quaisquer clan skills antigas (370-391) antes de recalcular
		for (int id = 370; id <= 391; id++) {
			player.skills().remove(id);
		}

		if (player.clanId() <= 0) {
			rebuildPassiveFuncsWithClanSkills(player, Collections.emptyMap());
			return ClanSkillBonuses.NONE;
		}

		var clanOpt = clanTable.byClanId(player.clanId());
		if (clanOpt.isEmpty()) {
			rebuildPassiveFuncsWithClanSkills(player, Collections.emptyMap());
			return ClanSkillBonuses.NONE;
		}

		Clan clan = clanOpt.get();
		ClanMember member = clan.getMember(player.objectId());
		if (member != null && !isMemberEligibleForClanSkills(member)) {
			rebuildPassiveFuncsWithClanSkills(player, Collections.emptyMap());
			return ClanSkillBonuses.NONE;
		}

		Map<Integer, Integer> clanSkills = clan.skills();
		for (var entry : clanSkills.entrySet()) {
			if (entry.getValue() != null && entry.getValue() > 0) {
				player.skills().put(entry.getKey(), entry.getValue());
			}
		}

		rebuildPassiveFuncsWithClanSkills(player, clanSkills);
		return calculateClanSkillBonuses(player);
	}

	/**
	 * Remove todas as Clan Skills (370-391) de um jogador (ex.: ao sair ou ser expulso do cla).
	 */
	public void removeClanSkillsFromPlayer(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		for (int id = 370; id <= 391; id++) {
			player.skills().remove(id);
		}
		rebuildPassiveFuncsWithClanSkills(player, Collections.emptyMap());
	}

	/**
	 * Calcula os bonus de atributos ativos concedidos pelas Clan Skills do cla do jogador.
	 */
	public ClanSkillBonuses calculateClanSkillBonuses(PlayerCharacter player) {
		if (player == null || player.clanId() <= 0) {
			return ClanSkillBonuses.NONE;
		}
		var clanOpt = clanTable.byClanId(player.clanId());
		if (clanOpt.isEmpty()) {
			return ClanSkillBonuses.NONE;
		}
		Clan clan = clanOpt.get();
		ClanMember member = clan.getMember(player.objectId());
		if (member != null && !isMemberEligibleForClanSkills(member)) {
			return ClanSkillBonuses.NONE;
		}

		Map<Integer, Integer> skills = clan.skills();
		if (skills.isEmpty()) {
			return ClanSkillBonuses.NONE;
		}

		double maxHpMul = threeLevelMul(skills.getOrDefault(370, 0), 1.03, 1.05, 1.06);
		double maxCpMul = threeLevelMul(skills.getOrDefault(371, 0), 1.06, 1.10, 1.12);
		double maxMpMul = threeLevelMul(skills.getOrDefault(372, 0), 1.03, 1.05, 1.06);
		double regHpMul = threeLevelMul(skills.getOrDefault(373, 0), 1.03, 1.05, 1.06);
		double regCpMul = threeLevelMul(skills.getOrDefault(374, 0), 1.03, 1.05, 1.06);
		double regMpMul = threeLevelMul(skills.getOrDefault(375, 0), 1.03, 1.05, 1.06);
		double pAtkMul = threeLevelMul(skills.getOrDefault(376, 0), 1.03, 1.05, 1.06);
		double pDefMul = threeLevelMul(skills.getOrDefault(377, 0), 1.03, 1.05, 1.06);
		double mAtkMul = threeLevelMul(skills.getOrDefault(378, 0), 1.03, 1.05, 1.06);
		double mDefMul = threeLevelMul(skills.getOrDefault(379, 0), 1.06, 1.10, 1.12);
		int accCombatAdd = threeLevelAdd(skills.getOrDefault(380, 0), 1, 2, 3);
		int rEvasAdd = threeLevelAdd(skills.getOrDefault(381, 0), 1, 2, 3);
		double rShldMul = threeLevelMul(skills.getOrDefault(382, 0), 1.03, 1.05, 1.06);
		double sDefMul = threeLevelMul(skills.getOrDefault(383, 0), 1.03, 1.05, 1.06);
		int runSpdAdd = threeLevelAdd(skills.getOrDefault(389, 0), 3, 5, 6);
		boolean hasImperium = skills.getOrDefault(391, 0) > 0;

		return new ClanSkillBonuses(
				maxHpMul, maxCpMul, maxMpMul,
				regHpMul, regCpMul, regMpMul,
				pAtkMul, pDefMul, mAtkMul, mDefMul,
				accCombatAdd, rEvasAdd, rShldMul, sDefMul,
				runSpdAdd, hasImperium
		);
	}

	private void rebuildPassiveFuncsWithClanSkills(PlayerCharacter player, Map<Integer, Integer> clanSkills) {
		if (skillService != null) {
			skillService.refreshPassives(player);
			if (!clanSkills.isEmpty()) {
				// Garante que mesmo se a SkillTable em memoria nao tiver carregado os XMLs, as StatFuncs existam
				List<StatFunc> merged = new ArrayList<>(player.passiveFuncs());
				List<StatFunc> fallback = buildClanSkillStatFuncs(clanSkills);
				for (StatFunc sf : fallback) {
					boolean alreadyHasStat = merged.stream().anyMatch(e -> e.stat().equalsIgnoreCase(sf.stat()) && e.op() == sf.op() && Math.abs(e.value() - sf.value()) < 0.0001);
					if (!alreadyHasStat) {
						merged.add(sf);
					}
				}
				player.passiveFuncs(merged);
			}
			return;
		}
		player.passiveFuncs(buildClanSkillStatFuncs(clanSkills));
	}

	private List<StatFunc> buildClanSkillStatFuncs(Map<Integer, Integer> clanSkills) {
		if (clanSkills == null || clanSkills.isEmpty()) {
			return List.of();
		}
		List<StatFunc> funcs = new ArrayList<>();
		for (var entry : clanSkills.entrySet()) {
			int skillId = entry.getKey();
			int lvl = entry.getValue() != null ? entry.getValue() : 0;
			if (lvl <= 0) {
				continue;
			}
			switch (skillId) {
				case 370 -> funcs.add(new StatFunc("maxHp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 371 -> funcs.add(new StatFunc("maxCp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.06, 1.10, 1.12)));
				case 372 -> funcs.add(new StatFunc("maxMp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 373 -> funcs.add(new StatFunc("regHp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 374 -> funcs.add(new StatFunc("regCp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 375 -> funcs.add(new StatFunc("regMp", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 376 -> funcs.add(new StatFunc("pAtk", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 377 -> funcs.add(new StatFunc("pDef", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 378 -> funcs.add(new StatFunc("mAtk", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 379 -> funcs.add(new StatFunc("mDef", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.06, 1.10, 1.12)));
				case 380 -> funcs.add(new StatFunc("accCombat", StatFunc.Op.ADD, 0x40, threeLevelAdd(lvl, 1, 2, 3)));
				case 381 -> funcs.add(new StatFunc("rEvas", StatFunc.Op.ADD, 0x40, threeLevelAdd(lvl, 1, 2, 3)));
				case 382 -> funcs.add(new StatFunc("rShld", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 383 -> funcs.add(new StatFunc("sDef", StatFunc.Op.MUL, 0x30, threeLevelMul(lvl, 1.03, 1.05, 1.06)));
				case 389 -> funcs.add(new StatFunc("runSpd", StatFunc.Op.ADD, 0x40, threeLevelAdd(lvl, 3, 5, 6)));
				default -> {}
			}
		}
		return funcs;
	}

	private static double threeLevelMul(int level, double l1, double l2, double l3) {
		return switch (level) {
			case 1 -> l1;
			case 2 -> l2;
			case 3 -> l3;
			default -> level > 3 ? l3 : 1.0;
		};
	}

	private static int threeLevelAdd(int level, int l1, int l2, int l3) {
		return switch (level) {
			case 1 -> l1;
			case 2 -> l2;
			case 3 -> l3;
			default -> level > 3 ? l3 : 0;
		};
	}

	/**
	 * Retorna o mapa de habilidades de cla aprendidas (skillId -> level).
	 */
	public Map<Integer, Integer> getClanSkills(int clanId) {
		var clanOpt = clanTable.byClanId(clanId);
		return clanOpt.map(Clan::skills).orElse(Collections.emptyMap());
	}

	/**
	 * Checa se um membro de cla e elegivel para receber os efeitos das habilidades de cla.
	 * Na regra oficial do Interlude, membros da Academia nao recebem habilidades de cla.
	 */
	public boolean isMemberEligibleForClanSkills(ClanMember member) {
		if (member == null) {
			return false;
		}
		// Membros da Academia (SUBUNIT_ACADEMY = -1) nao recebem habilidades de cla
		return member.pledgeType() != Clan.SUBUNIT_ACADEMY;
	}
}
