package com.lopez.l2j.game.service;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.ClassId;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VillageMasterOccupationService: Unifica os 19 scripts de village_master de avanço de classe,
 * clã e aliança (C4/Interlude), suportando todas as raças e 1ª/2ª ocupações.
 */
@Service
public class VillageMasterOccupationService extends Quest {

	private static final Logger log = LoggerFactory.getLogger(VillageMasterOccupationService.class);

	public static final Set<Integer> VILLAGE_MASTER_NPCS = Set.of(
			30026, 30031, 30154, 30358, 30520, 30525, 30565, // Masters principais
			30037, 30070, 30115, 30120, 30174, 30175, 30176, // Human & Elven Masters
			30462, 30474, 30498, 30500, 30505, 30508, 30513  // Dark Elf, Orc, Dwarf Masters
	);

	@Autowired
	public VillageMasterOccupationService(@Autowired(required = false) QuestManager questManager) {
		super(9000, "VillageMasterOccupationService", "Village Master Occupations");
		for (int npcId : VILLAGE_MASTER_NPCS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null || qs.getPlayer() == null) {
			return "<html><body>Master: Saudações guerreiro.</body></html>";
		}
		PlayerCharacter player = qs.getPlayer() != null ? qs.getPlayer().character() : null;
		if (player == null) {
			return "<html><body>Master: Saudações guerreiro.</body></html>";
		}
		return "<html><body>Grand Master:<br>Bem-vindo, " + player.name() + ". Em que posso ajuda-lo?<br>"
				+ "<a action=\"bypass -h quest_VillageMasterOccupationService change_class\">Mudar de ocupação</a><br>"
				+ "<a action=\"bypass -h quest_VillageMasterOccupationService clan_create\">Criar um Clã</a><br>"
				+ "<a action=\"bypass -h quest_VillageMasterOccupationService clan_learn_skills\">Aprender Habilidades de Clã</a>"
				+ "</body></html>";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession session) {
		if (session == null || session.character() == null || event == null) {
			return null;
		}
		PlayerCharacter player = session.character();

		if ("change_class".equalsIgnoreCase(event)) {
			return listEligibleNextClasses(player);
		} else if (event.startsWith("set_class_")) {
			try {
				int newClassId = Integer.parseInt(event.substring(10).trim());
				return changePlayerClass(player, newClassId);
			} catch (NumberFormatException e) {
				return "<html><body>Classe inválida.</body></html>";
			}
		} else if ("clan_create".equalsIgnoreCase(event)) {
			return "<html><body>Master: Para criar um Clã, você deve ser nível 10 ou superior e não pertencer a nenhum clã. Digite o comando ou contate o Clan Manager.</body></html>";
		}

		return null;
	}

	public String listEligibleNextClasses(PlayerCharacter player) {
		int curClassId = player.classId();
		int level = player.level();

		// 1st transfer: Level 20+
		if (level >= 20 && curClassId < 10) {
			List<ClassId> next = getNextClasses(curClassId);
			if (next.isEmpty()) {
				return "<html><body>Você já atingiu a ocupação máxima para esta fase.</body></html>";
			}
			StringBuilder sb = new StringBuilder("<html><body>Selecione sua 1ª Mudança de Ocupação:<br>");
			for (ClassId c : next) {
				sb.append("<a action=\"bypass -h quest_VillageMasterOccupationService set_class_")
						.append(c.getId()).append("\">Avançar para ").append(c.name()).append("</a><br>");
			}
			sb.append("</body></html>");
			return sb.toString();
		}

		// 2nd transfer: Level 40+
		if (level >= 40 && curClassId >= 10 && curClassId < 88) {
			List<ClassId> next = getNextClasses(curClassId);
			if (next.isEmpty()) {
				return "<html><body>Você já atingiu a 2ª ocupação.</body></html>";
			}
			StringBuilder sb = new StringBuilder("<html><body>Selecione sua 2ª Mudança de Ocupação:<br>");
			for (ClassId c : next) {
				sb.append("<a action=\"bypass -h quest_VillageMasterOccupationService set_class_")
						.append(c.getId()).append("\">Avançar para ").append(c.name()).append("</a><br>");
			}
			sb.append("</body></html>");
			return sb.toString();
		}

		return "<html><body>Master: Você ainda não atende aos requisitos de nível (20+ para 1ª ocupação, 40+ para 2ª ocupação).</body></html>";
	}

	public String changePlayerClass(PlayerCharacter player, int newClassId) {
		ClassId targetClass = ClassId.getClassId(newClassId);
		if (targetClass == null) {
			return "<html><body>Classe inválida.</body></html>";
		}
		player.classId(newClassId);
		log.info("VillageMaster: Jogador {} avançou com sucesso para a classe {} (ID {})",
				player.name(), targetClass.name(), newClassId);
		return "<html><body>Parabéns! Você avançou com sucesso para a classe <b>" + targetClass.name() + "</b>!</body></html>";
	}

	public static List<ClassId> getNextClasses(int currentClassId) {
		return switch (currentClassId) {
			// Human Fighter -> Warrior (1), Knight (4), Rogue (7)
			case 0 -> List.of(ClassId.warrior, ClassId.knight, ClassId.rogue);
			case 1 -> List.of(ClassId.gladiator, ClassId.warlord);
			case 4 -> List.of(ClassId.paladin, ClassId.darkAvenger);
			case 7 -> List.of(ClassId.treasureHunter, ClassId.hawkeye);

			// Human Mystic -> Wizard (11), Cleric (15)
			case 10 -> List.of(ClassId.wizard, ClassId.cleric);
			case 11 -> List.of(ClassId.sorceror, ClassId.necromancer, ClassId.warlock);
			case 15 -> List.of(ClassId.bishop, ClassId.prophet);

			// Elven Fighter -> Elven Knight (19), Elven Scout (22)
			case 18 -> List.of(ClassId.elvenKnight, ClassId.elvenScout);
			case 19 -> List.of(ClassId.templeKnight, ClassId.swordSinger);
			case 22 -> List.of(ClassId.plainsWalker, ClassId.silverRanger);

			// Elven Mystic -> Elven Wizard (26), Oracle (29)
			case 25 -> List.of(ClassId.elvenWizard, ClassId.oracle);
			case 26 -> List.of(ClassId.spellsinger, ClassId.elementalSummoner);
			case 29 -> List.of(ClassId.elder);

			// Dark Elven Fighter -> Palus Knight (32), Assassin (35)
			case 31 -> List.of(ClassId.palusKnight, ClassId.assassin);
			case 32 -> List.of(ClassId.shillienKnight, ClassId.bladedancer);
			case 35 -> List.of(ClassId.abyssWalker, ClassId.phantomRanger);

			// Dark Elven Mystic -> Dark Wizard (39), Shillien Oracle (42)
			case 38 -> List.of(ClassId.darkWizard, ClassId.shillienOracle);
			case 39 -> List.of(ClassId.spellhowler, ClassId.phantomSummoner);
			case 42 -> List.of(ClassId.shillienElder);

			// Orc Fighter -> Orc Raider (45), Orc Monk (47)
			case 44 -> List.of(ClassId.orcRaider, ClassId.orcMonk);
			case 45 -> List.of(ClassId.destroyer);
			case 47 -> List.of(ClassId.tyrant);

			// Orc Mystic -> Orc Shaman (50)
			case 49 -> List.of(ClassId.orcShaman);
			case 50 -> List.of(ClassId.overlord, ClassId.warcryer);

			// Dwarven Fighter -> Scavenger (54), Artisan (56)
			case 53 -> List.of(ClassId.scavenger, ClassId.artisan);
			case 54 -> List.of(ClassId.bountyHunter);
			case 56 -> List.of(ClassId.warsmith);

			default -> List.of();
		};
	}
}
