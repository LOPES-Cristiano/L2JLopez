package com.lopez.l2j.game.champion;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.drop.DropReward;
import com.lopez.l2j.game.npc.NpcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servico central do Sistema de Mobs Champion (Onda C12).
 * <p>
 * Gerencia a transformacao de monstros em Champions, aplicando multiplicadores
 * de HP, ataque, XP/SP, Adena, drops e recompensa especial em Event Medals.
 */
@Service
public class ChampionService {

	private static final Logger log = LoggerFactory.getLogger(ChampionService.class);

	public static final int DEFAULT_EVENT_MEDAL_ID = 6392; // Event Medal
	public static final int DEFAULT_GLITTERING_MEDAL_ID = 6393; // Glittering Medal

	private volatile boolean enabled = true;
	private volatile int frequency = 10; // 10% chance padrao
	private volatile int minLevel = 20;
	private volatile int maxLevel = 80;
	private volatile int hpMultiplier = 8;
	private volatile double pAtkMultiplier = 1.25;
	private volatile double mAtkMultiplier = 1.25;
	private volatile double expSpMultiplier = 8.0;
	private volatile double dropMultiplier = 8.0;
	private volatile double adenaMultiplier = 8.0;
	private volatile int specialItemId = DEFAULT_EVENT_MEDAL_ID;
	private volatile int specialItemAmount = 1;
	private volatile int specialItemChance = 50; // 50% de chance de medalha
	private volatile int specialItemLevelDiff = 9;
	private volatile String title = "Champion";

	public ChampionService() {
		loadFromConfig();
	}

	public ChampionService(
			boolean enabled,
			int frequency,
			int minLevel,
			int maxLevel,
			int hpMultiplier,
			double expSpMultiplier,
			double dropMultiplier,
			int specialItemChance,
			int specialItemId,
			int specialItemAmount,
			int specialItemLevelDiff) {
		this.enabled = enabled;
		this.frequency = frequency;
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		this.hpMultiplier = hpMultiplier;
		this.expSpMultiplier = expSpMultiplier;
		this.dropMultiplier = dropMultiplier;
		this.adenaMultiplier = dropMultiplier;
		this.specialItemChance = specialItemChance;
		this.specialItemId = specialItemId;
		this.specialItemAmount = specialItemAmount;
		this.specialItemLevelDiff = specialItemLevelDiff;
	}

	public void loadFromConfig() {
		try {
			if (Config.CHAMPION_ENABLE) {
				this.enabled = true;
			}
			if (Config.CHAMPION_FREQUENCY > 0) {
				this.frequency = Config.CHAMPION_FREQUENCY;
			}
			if (Config.CHAMPION_MIN_LVL > 0) {
				this.minLevel = Config.CHAMPION_MIN_LVL;
			}
			if (Config.CHAMPION_MAX_LVL > 0) {
				this.maxLevel = Config.CHAMPION_MAX_LVL;
			}
			if (Config.CHAMPION_HP > 0) {
				this.hpMultiplier = Config.CHAMPION_HP;
			}
			if (Config.CHAMPION_REWARDS > 0) {
				this.expSpMultiplier = Config.CHAMPION_REWARDS;
				this.dropMultiplier = Config.CHAMPION_REWARDS;
				this.adenaMultiplier = Config.CHAMPION_REWARDS;
			}
		} catch (Exception e) {
			log.warn("Nao foi possivel carregar parametros de Champion de Config: {}", e.getMessage());
		}
	}

	/**
	 * Verifica se o NPC e elegivel para se tornar um Champion.
	 */
	public boolean canBeChampion(NpcInstance npc) {
		if (!enabled || npc == null || npc.template() == null) {
			return false;
		}
		// Apenas monstros comuns atacaveis (nao pode ser Raid Boss, Minion ou NPC de cidade)
		if (!npc.isMonster() || !npc.isAttackable() || npc.template().isRaidBoss() || npc.isMinion()) {
			return false;
		}
		// Validacao por faixa de nivel
		int lvl = npc.template().level();
		if (lvl < minLevel || lvl > maxLevel) {
			return false;
		}
		// Nao pode ser monstro de quest exclusivo ou de tutorial
		int id = npc.npcId();
		if (id >= 18000 && id <= 18650) {
			return false;
		}
		return true;
	}

	/**
	 * Sorteia e aplica o status de Champion ao monstro, se elegivel.
	 *
	 * @param npc instancia do NPC
	 * @return true se foi transformado em Champion, false caso contrario
	 */
	public boolean tryRollChampion(NpcInstance npc) {
		if (!canBeChampion(npc)) {
			resetChampion(npc);
			return false;
		}
		int roll = ThreadLocalRandom.current().nextInt(100);
		if (roll < frequency) {
			makeChampion(npc);
			return true;
		} else {
			resetChampion(npc);
			return false;
		}
	}

	/**
	 * Transforma o monstro em Champion aplicando multiplicadores de atributos.
	 */
	public void makeChampion(NpcInstance npc) {
		if (npc == null || npc.template() == null) {
			return;
		}
		npc.champion(true);
		npc.championTitle(title);
		npc.maxHpMul(hpMultiplier);
		npc.pAtkMul(pAtkMultiplier);
		npc.mDefMul(1.1);
		npc.pDefMul(1.1);
		npc.currentHp(npc.maxHp());
		npc.currentMp(npc.template().maxMp());
		log.debug("Monstro {} #{} transformado em Champion [HP: {}]", npc.name(), npc.objectId(), npc.maxHp());
	}

	/**
	 * Restaura o monstro para os atributos de monstro normal.
	 */
	public void resetChampion(NpcInstance npc) {
		if (npc == null || npc.template() == null) {
			return;
		}
		npc.champion(false);
		npc.championTitle(null);
		npc.maxHpMul(1.0);
		npc.pAtkMul(1.0);
		npc.mDefMul(1.0);
		npc.pDefMul(1.0);
		npc.currentHp(npc.template().maxHp());
		npc.currentMp(npc.template().maxMp());
	}

	/**
	 * Calcula o ganho de EXP com multiplicador de Champion.
	 */
	public long calculateExp(NpcInstance npc, long baseExp) {
		if (npc != null && npc.isChampion()) {
			return (long) Math.round(baseExp * expSpMultiplier);
		}
		return baseExp;
	}

	/**
	 * Calcula o ganho de SP com multiplicador de Champion.
	 */
	public int calculateSp(NpcInstance npc, int baseSp) {
		if (npc != null && npc.isChampion()) {
			return (int) Math.round(baseSp * expSpMultiplier);
		}
		return baseSp;
	}

	/**
	 * Aplica os multiplicadores de drop e adiciona a recompensa especial de Medalhas de Evento.
	 */
	public List<DropReward> applyDropMultipliers(NpcInstance npc, List<DropReward> originalRewards, int playerLevel) {
		if (npc == null || !npc.isChampion()) {
			return originalRewards;
		}
		List<DropReward> multiplied = new ArrayList<>();
		for (DropReward reward : originalRewards) {
			if (reward == null) {
				continue;
			}
			int count;
			if (reward.isAdena()) {
				count = (int) Math.min(2_000_000_000L, Math.round(reward.count() * adenaMultiplier));
			} else {
				count = (int) Math.min(2_000_000_000L, Math.round(reward.count() * dropMultiplier));
			}
			multiplied.add(new DropReward(reward.itemId(), Math.max(1, count), reward.isAdena()));
		}

		// Drop especial de Medalha de Evento
		if (specialItemChance > 0 && specialItemId > 0) {
			int mobLevel = npc.template() != null ? npc.template().level() : 0;
			int levelDiff = Math.abs(mobLevel - playerLevel);
			if (playerLevel <= 0 || levelDiff <= specialItemLevelDiff) {
				int roll = ThreadLocalRandom.current().nextInt(100);
				if (roll < specialItemChance) {
					multiplied.add(new DropReward(specialItemId, specialItemAmount, false));
				}
			}
		}

		return multiplied;
	}

	// ---- Getters e Setters para administracao e testes ----

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public int getFrequency() { return frequency; }
	public void setFrequency(int frequency) { this.frequency = frequency; }

	public int getMinLevel() { return minLevel; }
	public void setMinLevel(int minLevel) { this.minLevel = minLevel; }

	public int getMaxLevel() { return maxLevel; }
	public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }

	public int getHpMultiplier() { return hpMultiplier; }
	public void setHpMultiplier(int hpMultiplier) { this.hpMultiplier = hpMultiplier; }

	public double getPAtkMultiplier() { return pAtkMultiplier; }
	public void setPAtkMultiplier(double pAtkMultiplier) { this.pAtkMultiplier = pAtkMultiplier; }

	public double getMAtkMultiplier() { return mAtkMultiplier; }
	public void setMAtkMultiplier(double mAtkMultiplier) { this.mAtkMultiplier = mAtkMultiplier; }

	public double getExpSpMultiplier() { return expSpMultiplier; }
	public void setExpSpMultiplier(double expSpMultiplier) { this.expSpMultiplier = expSpMultiplier; }

	public double getDropMultiplier() { return dropMultiplier; }
	public void setDropMultiplier(double dropMultiplier) { this.dropMultiplier = dropMultiplier; }

	public double getAdenaMultiplier() { return adenaMultiplier; }
	public void setAdenaMultiplier(double adenaMultiplier) { this.adenaMultiplier = adenaMultiplier; }

	public int getSpecialItemId() { return specialItemId; }
	public void setSpecialItemId(int specialItemId) { this.specialItemId = specialItemId; }

	public int getSpecialItemAmount() { return specialItemAmount; }
	public void setSpecialItemAmount(int specialItemAmount) { this.specialItemAmount = specialItemAmount; }

	public int getSpecialItemChance() { return specialItemChance; }
	public void setSpecialItemChance(int specialItemChance) { this.specialItemChance = specialItemChance; }

	public int getSpecialItemLevelDiff() { return specialItemLevelDiff; }
	public void setSpecialItemLevelDiff(int specialItemLevelDiff) { this.specialItemLevelDiff = specialItemLevelDiff; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
}
