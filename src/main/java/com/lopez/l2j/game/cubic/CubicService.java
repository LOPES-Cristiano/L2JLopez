package com.lopez.l2j.game.cubic;

import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Servico gerenciador de Cubics dos personagens (#180, #255, #256, #257).
 * Controla invocacao por skill (individual e em massa para Party), limite de {@code Cubic Mastery} (Skill 143:
 * 1, 2 ou 3 cubics simultaneos com substituicao FIFO do mais antigo), expiracao de 15 minutos e acoes de IA.
 */
@Service
public class CubicService {

	public static final int CUBIC_MASTERY_SKILL_ID = 143;

	private static volatile CubicService instance;

	private final Map<Integer, Map<Integer, CubicInstance>> cubicsByPlayerId = new ConcurrentHashMap<>();

	public CubicService() {
		instance = this;
	}

	public static CubicService getInstance() {
		if (instance == null) {
			instance = new CubicService();
		}
		return instance;
	}

	/**
	 * Calcula o limite maximo de Cubics simultaneos permitido pela passiva {@code Cubic Mastery} (Skill 143):
	 * - Nivel 0 (sem a passiva): 1 Cubic
	 * - Nivel 1: 2 Cubics
	 * - Nivel 2+: 3 Cubics
	 */
	public int getMaxConcurrentCubics(PlayerCharacter player) {
		if (player == null) {
			return 1;
		}
		int masteryLevel = player.skillLevel(CUBIC_MASTERY_SKILL_ID);
		if (masteryLevel >= 2) {
			return 3;
		}
		if (masteryLevel == 1) {
			return 2;
		}
		return 1;
	}

	/**
	 * Invoca ou substitui um Cubic no jogador respeitando o limite de {@code Cubic Mastery}.
	 */
	public synchronized CubicInstance spawnCubic(
			PlayerCharacter owner,
			int cubicId,
			int skillLevel,
			boolean givenByOther) {
		if (owner == null || owner.isDead() || cubicId < 1 || cubicId > 9) {
			return null;
		}

		Map<Integer, CubicInstance> playerCubics = cubicsByPlayerId.computeIfAbsent(
				owner.objectId(), k -> Collections.synchronizedMap(new LinkedHashMap<>()));

		// Limpa cubics ja expirados
		playerCubics.values().removeIf(c -> !c.isActive());

		// Se ja possuir um Cubic do mesmo ID, encerra o anterior
		CubicInstance existingSame = playerCubics.remove(cubicId);
		if (existingSame != null) {
			existingSame.stop();
		} else {
			int maxAllowed = getMaxConcurrentCubics(owner);
			while (playerCubics.size() >= maxAllowed && !playerCubics.isEmpty()) {
				Integer oldestId = playerCubics.keySet().iterator().next();
				CubicInstance evicted = playerCubics.remove(oldestId);
				if (evicted != null) {
					evicted.stop();
				}
			}
		}

		CubicInstance cubic = new CubicInstance(owner, cubicId, skillLevel, givenByOther);
		cubic.onExpireCallback(() -> playerCubics.remove(cubicId, cubic));
		playerCubics.put(cubicId, cubic);
		cubic.start();
		return cubic;
	}

	/**
	 * Invoca Cubic(s) a partir do ID da skill de invocacao (incluindo Mass Summon Cubics de 3ª classe).
	 */
	public List<CubicInstance> summonCubicBySkill(
			PlayerCharacter caster,
			int skillId,
			int skillLevel,
			Collection<PlayerCharacter> partyMembers) {
		if (caster == null) {
			return List.of();
		}
		int cubicId = resolveCubicIdBySkill(skillId);
		if (cubicId <= 0) {
			return List.of();
		}

		boolean isMass = isMassCubicSkill(skillId);
		List<CubicInstance> spawned = new ArrayList<>();
		if (isMass && partyMembers != null && !partyMembers.isEmpty()) {
			for (PlayerCharacter member : partyMembers) {
				if (member != null && !member.isDead()) {
					CubicInstance c = spawnCubic(member, cubicId, skillLevel, member != caster);
					if (c != null) {
						spawned.add(c);
					}
				}
			}
		} else {
			CubicInstance c = spawnCubic(caster, cubicId, skillLevel, false);
			if (c != null) {
				spawned.add(c);
			}
		}
		return spawned;
	}

	/**
	 * Mapeia o ID da skill de invocacao de Cubic para o {@code cubicId} oficial (1..9).
	 */
	public static int resolveCubicIdBySkill(int skillId) {
		return switch (skillId) {
			case 10, 1325, 1328 -> CubicInstance.STORM_CUBIC;       // Summon Storm Cubic / Mass Summon Storm Cubic
			case 22 -> CubicInstance.VAMPIRIC_CUBIC;                // Summon Vampiric Cubic
			case 67 -> CubicInstance.LIFE_CUBIC;                    // Summon Life Cubic
			case 278, 279 -> CubicInstance.VIPER_CUBIC;             // Summon Viper Cubic
			case 33, 1326, 1330 -> CubicInstance.POLTERGEIST_CUBIC; // Summon Phantom Cubic / Mass Summon Phantom Cubic
			case 1279 -> CubicInstance.BINDING_CUBIC;               // Summon Binding Cubic
			case 1280, 1327, 1329 -> CubicInstance.AQUA_CUBIC;      // Summon Aqua Cubic / Mass Summon Aqua Cubic
			case 1281 -> CubicInstance.SPARK_CUBIC;                 // Summon Spark Cubic
			case 449 -> CubicInstance.ATTRACTIVE_CUBIC;             // Summon Attractive Cubic
			default -> 0;
		};
	}

	public static boolean isCubicSummonSkill(int skillId) {
		return resolveCubicIdBySkill(skillId) > 0;
	}

	public static boolean isMassCubicSkill(int skillId) {
		return skillId == 1325 || skillId == 1326 || skillId == 1327
				|| skillId == 1328 || skillId == 1329 || skillId == 1330;
	}

	public List<CubicInstance> getActiveCubics(PlayerCharacter player) {
		return player != null ? getActiveCubics(player.objectId()) : List.of();
	}

	public List<CubicInstance> getActiveCubics(int playerObjectId) {
		Map<Integer, CubicInstance> map = cubicsByPlayerId.get(playerObjectId);
		if (map == null || map.isEmpty()) {
			return List.of();
		}
		synchronized (map) {
			map.values().removeIf(c -> !c.isActive());
			return List.copyOf(map.values());
		}
	}

	public List<Integer> getCubicIds(int playerObjectId) {
		List<CubicInstance> active = getActiveCubics(playerObjectId);
		if (active.isEmpty()) {
			return List.of();
		}
		List<Integer> ids = new ArrayList<>(active.size());
		for (CubicInstance c : active) {
			ids.add(c.id());
		}
		return ids;
	}

	public Optional<CubicInstance> getCubic(int playerObjectId, int cubicId) {
		Map<Integer, CubicInstance> map = cubicsByPlayerId.get(playerObjectId);
		if (map == null) {
			return Optional.empty();
		}
		CubicInstance c = map.get(cubicId);
		if (c != null && !c.isActive()) {
			map.remove(cubicId, c);
			return Optional.empty();
		}
		return Optional.ofNullable(c);
	}

	public boolean removeCubic(int playerObjectId, int cubicId) {
		Map<Integer, CubicInstance> map = cubicsByPlayerId.get(playerObjectId);
		if (map == null) {
			return false;
		}
		CubicInstance removed = map.remove(cubicId);
		if (removed != null) {
			removed.stop();
			return true;
		}
		return false;
	}

	public void clearCubics(int playerObjectId) {
		Map<Integer, CubicInstance> map = cubicsByPlayerId.remove(playerObjectId);
		if (map != null) {
			synchronized (map) {
				for (CubicInstance c : new ArrayList<>(map.values())) {
					c.stop();
				}
				map.clear();
			}
		}
	}

	/**
	 * Atualiza o alvo de combate de todos os Cubics ativos do jogador e dispara um ciclo de acao.
	 */
	public int triggerCombatAction(
			PlayerCharacter owner,
			Creature combatTarget,
			Collection<PlayerCharacter> partyMembers) {
		if (owner == null) {
			return 0;
		}
		List<CubicInstance> cubics = getActiveCubics(owner.objectId());
		int triggered = 0;
		for (CubicInstance c : cubics) {
			c.currentTarget(combatTarget);
			if (c.doAction(combatTarget, partyMembers)) {
				triggered++;
			}
		}
		return triggered;
	}

	/**
	 * Gatilho chamado quando o dono ataca um alvo (PvE/PvP), atualizando o alvo dos Cubics ativos
	 * e disparando sua avaliacao de acao (#551-#595).
	 */
	public int onOwnerAttack(PlayerCharacter owner, Creature combatTarget) {
		return triggerCombatAction(owner, combatTarget, owner != null ? List.of(owner) : List.of());
	}

	public int onOwnerAttack(
			PlayerCharacter owner,
			Creature combatTarget,
			Collection<PlayerCharacter> partyMembers) {
		return triggerCombatAction(owner, combatTarget, partyMembers);
	}
}
