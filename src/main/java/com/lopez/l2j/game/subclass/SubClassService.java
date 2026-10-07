package com.lopez.l2j.game.subclass;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela gestão de subclasses no Lineage II Interlude.
 * Implementa validação de elegibilidade, filtros canônicos de raça e classe,
 * além da persistência na tabela character_subclasses.
 */
@Service
public class SubClassService {

	private static final Logger log = LoggerFactory.getLogger(SubClassService.class);

	public static final int MAX_SUBCLASSES = 3;
	public static final int MIN_LEVEL_FOR_SUBCLASS = 75;

	// Raças Lineage II
	public static final int RACE_HUMAN = 0;
	public static final int RACE_LIGHT_ELF = 1;
	public static final int RACE_DARK_ELF = 2;
	public static final int RACE_ORC = 3;
	public static final int RACE_DWARF = 4;

	// Classes de 2nd Job elegíveis para Subclasse
	public static final List<Integer> VALID_SUBCLASSES = List.of(
			// Humano Guerreiro
			2,  // Gladiator
			3,  // Warlord
			5,  // Paladin
			6,  // Dark Avenger
			8,  // Treasure Hunter
			9,  // Hawkeye
			// Humano Mago
			12, // Sorcerer
			13, // Necromancer
			14, // Warlock
			16, // Bishop
			17, // Prophet
			// Elfo da Luz
			20, // Temple Knight
			21, // Swordsinger
			23, // Plains Walker
			24, // Silver Ranger
			27, // Spellsinger
			28, // Elemental Summoner
			30, // Elven Elder
			// Elfo Negro
			33, // Shillien Knight
			34, // Bladedancer
			36, // Abyss Walker
			37, // Phantom Ranger
			40, // Spellhowler
			41, // Phantom Summoner
			43, // Shillien Elder
			// Orc
			46, // Destroyer
			48, // Tyrant
			52, // Warcryer
			// Anão
			55  // Bounty Hunter
	);

	// Nomes das classes para exibição rápida e limpa
	private static final Map<Integer, String> CLASS_NAMES = Map.ofEntries(
			Map.entry(2, "Gladiator"),
			Map.entry(3, "Warlord"),
			Map.entry(5, "Paladin"),
			Map.entry(6, "Dark Avenger"),
			Map.entry(8, "Treasure Hunter"),
			Map.entry(9, "Hawkeye"),
			Map.entry(12, "Sorcerer"),
			Map.entry(13, "Necromancer"),
			Map.entry(14, "Warlock"),
			Map.entry(16, "Bishop"),
			Map.entry(17, "Prophet"),
			Map.entry(20, "Temple Knight"),
			Map.entry(21, "Swordsinger"),
			Map.entry(23, "Plains Walker"),
			Map.entry(24, "Silver Ranger"),
			Map.entry(27, "Spellsinger"),
			Map.entry(28, "Elemental Summoner"),
			Map.entry(30, "Elven Elder"),
			Map.entry(33, "Shillien Knight"),
			Map.entry(34, "Bladedancer"),
			Map.entry(36, "Abyss Walker"),
			Map.entry(37, "Phantom Ranger"),
			Map.entry(40, "Spellhowler"),
			Map.entry(41, "Phantom Summoner"),
			Map.entry(43, "Shillien Elder"),
			Map.entry(46, "Destroyer"),
			Map.entry(48, "Tyrant"),
			Map.entry(52, "Warcryer"),
			Map.entry(55, "Bounty Hunter")
	);

	// Grupos de exclusão mútua (não é permitido ter classes da mesma categoria)
	private static final List<Set<Integer>> EXCLUSION_GROUPS = List.of(
			Set.of(5, 6, 20, 33),   // Tanks: Paladin, Dark Avenger, Temple Knight, Shillien Knight
			Set.of(8, 23, 36),       // Daggers: Treasure Hunter, Plains Walker, Abyss Walker
			Set.of(9, 24, 37),       // Bows: Hawkeye, Silver Ranger, Phantom Ranger
			Set.of(12, 27, 40),      // Nukers: Sorcerer, Spellsinger, Spellhowler
			Set.of(14, 28, 41),      // Summoners: Warlock, Elemental Summoner, Phantom Summoner
			Set.of(16, 30, 43),      // Healers: Bishop, Elven Elder, Shillien Elder
			Set.of(21, 34),          // Enchanters: Swordsinger, Bladedancer
			Set.of(2, 3)             // Fighters: Gladiator, Warlord
	);

	private final JdbcClient jdbc;

	@Autowired
	public SubClassService(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public String getClassName(int classId) {
		return CLASS_NAMES.getOrDefault(classId, "Class " + classId);
	}

	/**
	 * Mapeia qualquer classe (1st, 2nd ou 3rd) para sua respectiva 2nd class base.
	 */
	public int getSecondClassId(int classId) {
		return switch (classId) {
			case 1, 2, 88 -> 2;
			case 3, 89 -> 3;
			case 4, 5, 90 -> 5;
			case 6, 91 -> 6;
			case 7, 8, 93 -> 8;
			case 9, 92 -> 9;
			case 10, 11, 12, 94 -> 12;
			case 13, 95 -> 13;
			case 14, 96 -> 14;
			case 15, 16, 97 -> 16;
			case 17, 98 -> 17;
			case 18, 19, 20, 99 -> 20;
			case 21, 100 -> 21;
			case 22, 23, 101 -> 23;
			case 24, 102 -> 24;
			case 25, 26, 27, 103 -> 27;
			case 28, 104 -> 28;
			case 29, 30, 105 -> 30;
			case 31, 32, 33, 106 -> 33;
			case 34, 107 -> 34;
			case 35, 36, 108 -> 36;
			case 37, 109 -> 37;
			case 38, 39, 40, 110 -> 40;
			case 41, 111 -> 41;
			case 42, 43, 112 -> 43;
			case 44, 45, 46, 113 -> 46;
			case 47, 48, 114 -> 48;
			case 49, 50, 51, 115 -> 51;
			case 52, 116 -> 52;
			case 53, 54, 55, 117 -> 55;
			case 56, 57, 118 -> 57;
			default -> classId;
		};
	}

	/**
	 * Verifica se o jogador pode adicionar uma nova subclasse.
	 */
	public boolean canAddSubClass(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		if (player.isGm()) {
			return player.subClasses().size() < MAX_SUBCLASSES;
		}
		if (player.level() < MIN_LEVEL_FOR_SUBCLASS) {
			return false;
		}
		if (player.subClasses().size() >= MAX_SUBCLASSES) {
			return false;
		}
		// Todas as subclasses já existentes precisam estar nível 75 ou mais
		for (SubClass sub : player.subClasses().values()) {
			if (sub.level() < MIN_LEVEL_FOR_SUBCLASS) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Retorna a lista de 2nd classes que o jogador pode escolher como subclasse.
	 */
	public List<Integer> getAvailableSubClasses(PlayerCharacter player) {
		if (player == null) {
			return Collections.emptyList();
		}
		int baseSecondClass = getSecondClassId(player.baseClassId());
		int playerRace = player.race();

		List<Integer> available = new ArrayList<>();
		for (int candidate : VALID_SUBCLASSES) {
			// Não pode ser a mesma classe base
			if (candidate == baseSecondClass) {
				continue;
			}
			// Não pode ser uma subclasse já existente
			boolean alreadyHas = false;
			for (SubClass sub : player.subClasses().values()) {
				if (getSecondClassId(sub.classId()) == candidate) {
					alreadyHas = true;
					break;
				}
			}
			if (alreadyHas) {
				continue;
			}

			// Regra Canônica de Raça:
			// Elfos da Luz não podem pegar Elfo Negro
			if (playerRace == RACE_LIGHT_ELF && isDarkElfClass(candidate)) {
				continue;
			}
			// Elfos Negros não podem pegar Elfo da Luz
			if (playerRace == RACE_DARK_ELF && isLightElfClass(candidate)) {
				continue;
			}

			// Regra Canônica de Exclusão Mútua:
			if (isInSameExclusionGroup(baseSecondClass, candidate)) {
				continue;
			}
			boolean groupConflict = false;
			for (SubClass sub : player.subClasses().values()) {
				if (isInSameExclusionGroup(getSecondClassId(sub.classId()), candidate)) {
					groupConflict = true;
					break;
				}
			}
			if (groupConflict) {
				continue;
			}

			available.add(candidate);
		}
		return available;
	}

	private boolean isLightElfClass(int classId) {
		return classId >= 18 && classId <= 30;
	}

	private boolean isDarkElfClass(int classId) {
		return classId >= 31 && classId <= 43;
	}

	private boolean isInSameExclusionGroup(int class1, int class2) {
		if (class1 == class2) {
			return true;
		}
		for (Set<Integer> group : EXCLUSION_GROUPS) {
			if (group.contains(class1) && group.contains(class2)) {
				return true;
			}
		}
		return false;
	}

	// ==================== Persistência ====================

	public Map<Integer, SubClass> loadSubClasses(int charId) {
		if (jdbc == null) {
			return new HashMap<>();
		}
		try {
			List<SubClass> list = jdbc.sql("""
					SELECT class_id, exp, sp, level, class_index
					FROM character_subclasses
					WHERE charId = ?
					ORDER BY class_index ASC
					""")
					.param(charId)
					.query((rs, rowNum) -> new SubClass(
							rs.getInt("class_id"),
							rs.getLong("exp"),
							rs.getInt("sp"),
							rs.getInt("level"),
							rs.getInt("class_index")
					))
					.list();

			Map<Integer, SubClass> map = new HashMap<>();
			for (SubClass sc : list) {
				map.put(sc.classIndex(), sc);
			}
			return map;
		} catch (Exception e) {
			log.error("Erro ao carregar subclasses para charId {}: {}", charId, e.getMessage(), e);
			return new HashMap<>();
		}
	}

	public void saveSubClass(int charId, SubClass sc) {
		if (jdbc == null || sc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO character_subclasses (charId, class_id, exp, sp, level, class_index)
					VALUES (?, ?, ?, ?, ?, ?)
					""")
					.params(charId, sc.classId(), sc.exp(), sc.sp(), sc.level(), sc.classIndex())
					.update();
		} catch (Exception e) {
			log.error("Erro ao salvar subclasse index {} para charId {}: {}", sc.classIndex(), charId, e.getMessage(), e);
		}
	}

	public void deleteSubClass(int charId, int classIndex) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM character_subclasses WHERE charId = ? AND class_index = ?")
					.params(charId, classIndex)
					.update();
		} catch (Exception e) {
			log.error("Erro ao deletar subclasse index {} para charId {}: {}", classIndex, charId, e.getMessage(), e);
		}
	}
}
