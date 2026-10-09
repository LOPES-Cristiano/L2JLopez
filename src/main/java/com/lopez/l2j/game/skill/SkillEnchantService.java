package com.lopez.l2j.game.skill;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillTreeTable.EnchantSkillLearn;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExEnchantSkillInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExEnchantSkillList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servico central de encantamento de habilidades (Skill Enchanting).
 * Controla verificacao de pre-requisitos (Giant's Codex, SP, EXP, 3rd class transfer),
 * taxas oficiais por nivel de jogador (76, 77, 78+), sucesso e downgrade para base_lvl na falha.
 */
@Service
public class SkillEnchantService {

	private static final Logger log = LoggerFactory.getLogger(SkillEnchantService.class);
	public static final int GIANTS_CODEX_ITEM_ID = 6622;

	public enum ResultType {
		SUCCESS,
		FAIL,
		NOT_ENOUGH_SP,
		NOT_ENOUGH_EXP,
		MISSING_ITEM,
		INVALID_CONDITION,
		SKILL_NOT_FOUND
	}

	public record EnchantResult(ResultType type, int skillId, int newLevel, int previousLevel, int rate) {
		public boolean isSuccess() {
			return type == ResultType.SUCCESS;
		}
	}

	public record EnchantInfo(int skillId, int level, int spCost, long expCost, int rate, boolean requiresBook) {
	}

	private final SkillTreeTable skillTreeTable;
	private final SkillService skillService;
	private final InventoryService inventoryService;

	public SkillEnchantService(SkillTreeTable skillTreeTable, SkillService skillService, InventoryService inventoryService) {
		this.skillTreeTable = skillTreeTable;
		this.skillService = skillService;
		this.inventoryService = inventoryService;
	}

	/**
	 * Verifica se o nivel de encantamento corresponde ao inicio de uma rota (+1),
	 * momento no qual a utilizacao do Secret Book of Giants (Giant's Codex) e obrigatoria.
	 */
	public static boolean isFirstEnchantLevel(int level) {
		return level == 101 || level == 141 || level == 181 || level == 221 || level == 261 || (level % 100 == 1);
	}

	/**
	 * Calcula a taxa de sucesso com base no nivel atual do jogador (76, 77, 78+).
	 */
	public int getSuccessRate(EnchantSkillLearn enchant, int playerLevel) {
		if (enchant == null) {
			return 0;
		}
		if (playerLevel <= 76) {
			return enchant.rate76();
		} else if (playerLevel == 77) {
			return enchant.rate77();
		} else {
			return enchant.rate78();
		}
	}

	/**
	 * Retorna a lista de skills passiveis de encantamento para o jogador.
	 */
	public List<ExEnchantSkillList.SkillEntry> getAvailableEnchantSkills(PlayerCharacter player) {
		if (player == null || skillTreeTable == null) {
			return List.of();
		}
		var available = skillTreeTable.availableEnchantSkills(player.skills());
		List<ExEnchantSkillList.SkillEntry> entries = new ArrayList<>(available.size());
		for (var s : available) {
			int sp = Config.ENCH_SKILL_SP_NEEDED ? s.sp() : 0;
			int exp = Config.ENCH_SKILL_XP_NEEDED ? s.exp() : 0;
			entries.add(new ExEnchantSkillList.SkillEntry(s.id(), s.level(), sp, exp));
		}
		return entries;
	}

	/**
	 * Consulta os detalhes de custo e probabilidade para um determinado skill e nivel desejado.
	 */
	public Optional<EnchantInfo> getEnchantInfo(PlayerCharacter player, int skillId, int level) {
		if (player == null || skillTreeTable == null) {
			return Optional.empty();
		}
		var opt = skillTreeTable.getEnchantSkill(skillId, level);
		if (opt.isEmpty()) {
			return Optional.empty();
		}
		var s = opt.get();
		int rate = getSuccessRate(s, player.level());
		int spCost = Config.ENCH_SKILL_SP_NEEDED ? s.sp() : 0;
		long expCost = Config.ENCH_SKILL_XP_NEEDED ? s.exp() : 0;
		boolean needBook = Config.ENCHANT_SKILL_SP_BOOK_NEEDED && isFirstEnchantLevel(s.level());
		return Optional.of(new EnchantInfo(s.id(), s.level(), spCost, expCost, rate, needBook));
	}

	/**
	 * Converte informacoes de encantamento para o pacote oficial do cliente ExEnchantSkillInfo.
	 */
	public Optional<ExEnchantSkillInfo> buildExEnchantSkillInfoPacket(PlayerCharacter player, int skillId, int level) {
		return getEnchantInfo(player, skillId, level).map(info -> {
			List<ExEnchantSkillInfo.Req> reqs = new ArrayList<>();
			if (info.requiresBook()) {
				reqs.add(new ExEnchantSkillInfo.Req(4, GIANTS_CODEX_ITEM_ID, 1, 0));
			}
			return new ExEnchantSkillInfo(info.skillId(), info.level(), info.spCost(), info.expCost(), info.rate(), reqs);
		});
	}

	/**
	 * Executa a operacao de encantamento do skill.
	 */
	public EnchantResult enchantSkill(PlayerCharacter player, int skillId, int level) {
		if (player == null || skillTreeTable == null) {
			return new EnchantResult(ResultType.INVALID_CONDITION, skillId, 0, 0, 0);
		}
		if (player.isDead() || player.level() < 76) {
			return new EnchantResult(ResultType.INVALID_CONDITION, skillId, 0, player.skillLevel(skillId), 0);
		}
		var opt = skillTreeTable.getEnchantSkill(skillId, level);
		if (opt.isEmpty()) {
			return new EnchantResult(ResultType.SKILL_NOT_FOUND, skillId, 0, player.skillLevel(skillId), 0);
		}
		var s = opt.get();
		int currentLvl = player.skillLevel(s.id());
		if (currentLvl != s.minSkillLvl()) {
			log.warn("{} tentou encantar skill {} para nivel {} sem pre-requisito (tem {}, precisa {})",
					player.name(), s.id(), s.level(), currentLvl, s.minSkillLvl());
			return new EnchantResult(ResultType.INVALID_CONDITION, skillId, currentLvl, currentLvl, 0);
		}

		// 1. Checa Giant's Codex (6622)
		boolean needBook = Config.ENCHANT_SKILL_SP_BOOK_NEEDED && isFirstEnchantLevel(s.level());
		ItemInstance bookItem = null;
		if (needBook) {
			Inventory inv = player.inventory();
			if (inv != null) {
				var bookOpt = inv.byItemId(GIANTS_CODEX_ITEM_ID);
				if (bookOpt.isPresent() && bookOpt.get().count() >= 1) {
					bookItem = bookOpt.get();
				}
			}
			if (bookItem == null) {
				return new EnchantResult(ResultType.MISSING_ITEM, skillId, currentLvl, currentLvl, 0);
			}
		}

		// 2. Checa SP
		int spCost = Config.ENCH_SKILL_SP_NEEDED ? s.sp() : 0;
		if (spCost > 0 && player.sp() < spCost) {
			return new EnchantResult(ResultType.NOT_ENOUGH_SP, skillId, currentLvl, currentLvl, 0);
		}

		// 3. Checa EXP
		long expCost = Config.ENCH_SKILL_XP_NEEDED ? s.exp() : 0;
		if (expCost > 0 && player.exp() < expCost) {
			return new EnchantResult(ResultType.NOT_ENOUGH_EXP, skillId, currentLvl, currentLvl, 0);
		}

		// Consumir Livro (Giant's Codex)
		if (needBook && bookItem != null && inventoryService != null) {
			inventoryService.destroyItem(player.inventory(), bookItem.objectId(), 1, "SkillEnchant");
		}

		// Consumir SP e EXP
		if (spCost > 0) {
			player.sp(player.sp() - spCost);
		}
		if (expCost > 0) {
			player.exp(player.exp() - expCost);
		}

		int rate = getSuccessRate(s, player.level());
		boolean success = ThreadLocalRandom.current().nextInt(100) < rate;

		if (success) {
			if (skillService != null) {
				skillService.setSkill(player, s.id(), s.level());
			} else {
				player.skills().put(s.id(), s.level());
			}
			return new EnchantResult(ResultType.SUCCESS, s.id(), s.level(), currentLvl, rate);
		} else {
			// Na falha oficial de Interlude, a habilidade e rebaixada ao seu nivel base
			if (skillService != null) {
				skillService.setSkill(player, s.id(), s.baseLvl());
			} else {
				player.skills().put(s.id(), s.baseLvl());
			}
			return new EnchantResult(ResultType.FAIL, s.id(), s.baseLvl(), currentLvl, rate);
		}
	}
}
