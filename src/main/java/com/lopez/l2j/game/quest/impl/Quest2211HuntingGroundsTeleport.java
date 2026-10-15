package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.sevensigns.SevenSignsManager;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 2211_HuntingGroundsTeleport: Teleporte dos Priests of Dawn e Dusk para Catacumbas e Necropoles
 * cobrando Ancient Adena (5575) com base no registro dos Sete Selos.
 */
@Component
public class Quest2211HuntingGroundsTeleport extends Quest {

	private final SevenSignsManager sevenSigns;

	public static final Set<Integer> DAWN_NPCS = Set.of(
			31078, 31079, 31080, 31081, 31082, 31083, 31084,
			31168, 31692, 31694, 31997
	);

	@Autowired
	public Quest2211HuntingGroundsTeleport(@Autowired(required = false) QuestManager questManager,
			@Autowired(required = false) SevenSignsManager sevenSigns) {
		super(2211, "2211_HuntingGroundsTeleport", "Teleports");
		this.sevenSigns = sevenSigns;

		for (int i = 31078; i <= 31091; i++) {
			addStartNpc(i);
			addTalkId(i);
		}
		for (int i = 31168; i <= 31169; i++) {
			addStartNpc(i);
			addTalkId(i);
		}
		for (int i = 31692; i <= 31695; i++) {
			addStartNpc(i);
			addTalkId(i);
		}
		for (int i = 31997; i <= 31998; i++) {
			addStartNpc(i);
			addTalkId(i);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (npc == null || qs == null) {
			return null;
		}
		int npcId = npc.npcId();
		int charId = qs.getCharId();

		int playerCabal = sevenSigns != null ? sevenSigns.getPlayerCabal(charId) : SevenSignsManager.CABAL_NULL;
		int playerSeal = sevenSigns != null ? sevenSigns.getPlayerSeal(charId) : SevenSignsManager.SEAL_NULL;
		int sealOwnerGnosis = sevenSigns != null ? sevenSigns.getSealOwner(SevenSignsManager.SEAL_GNOSIS) : SevenSignsManager.CABAL_NULL;
		boolean periodValidate = sevenSigns != null && sevenSigns.isSealValidationPeriod();

		String htmltext;
		if (playerCabal == SevenSignsManager.CABAL_NULL) {
			htmltext = DAWN_NPCS.contains(npcId) ? "dawn_tele-no.htm" : "dusk_tele-no.htm";
		} else {
			boolean gnosisOwnerMatch = (sealOwnerGnosis != SevenSignsManager.CABAL_NULL && sealOwnerGnosis == playerCabal);
			boolean isLow = periodValidate && gnosisOwnerMatch && playerSeal == SevenSignsManager.SEAL_GNOSIS;

			htmltext = switch (npcId) {
				case 31078, 31085 -> isLow ? "low_gludin.htm" : "hg_gludin.htm";
				case 31079, 31086 -> isLow ? "low_gludio.htm" : "hg_gludio.htm";
				case 31080, 31087 -> isLow ? "low_dion.htm" : "hg_dion.htm";
				case 31081, 31088 -> isLow ? "low_giran.htm" : "hg_giran.htm";
				case 31082, 31089 -> isLow ? "low_heine.htm" : "hg_heine.htm";
				case 31083, 31090 -> isLow ? "low_oren.htm" : "hg_oren.htm";
				case 31084, 31091 -> isLow ? "low_aden.htm" : "hg_aden.htm";
				case 31168, 31169 -> isLow ? "low_hw.htm" : "hg_hw.htm";
				case 31692, 31693 -> isLow ? "low_goddard.htm" : "hg_goddard.htm";
				case 31694, 31695 -> isLow ? "low_rune.htm" : "hg_rune.htm";
				case 31997, 31998 -> isLow ? "low_schuttgart.htm" : "hg_schuttgart.htm";
				default -> "hg_wrong.htm";
			};
		}

		qs.exitQuest(true);
		return htmltext;
	}
}
