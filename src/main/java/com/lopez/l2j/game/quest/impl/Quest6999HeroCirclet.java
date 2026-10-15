package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 6999_HeroCirclet: Monument of Heroes entregando a Wings of Destiny Circlet (6842)
 * para Heróis ativos.
 */
@Component
public class Quest6999HeroCirclet extends Quest {

	public static final int CIRCLET = 6842;
	public static final int[] MONUMENTS = { 31690, 31769, 31770, 31771, 31772 };

	@Autowired
	public Quest6999HeroCirclet(@Autowired(required = false) QuestManager questManager) {
		super(6999, "6999_HeroCirclet", "Hero Items");
		for (int npcId : MONUMENTS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null) {
			return null;
		}
		PlayerCharacter player = qs.playerChar();
		String htmltext;
		if (player != null && player.isHero()) {
			if (qs.getQuestItemsCount(CIRCLET) > 0) {
				htmltext = "<html><body>Monument of Heroes:<br>You can't have more than one circlet.</body></html>";
			} else {
				qs.giveItems(CIRCLET, 1);
				htmltext = "<html><body>Monument of Heroes:<br>Enjoy your Wings of Destiny Circlet.</body></html>";
			}
		} else {
			int objId = npc != null ? npc.objectId() : 0;
			htmltext = "<html><body>Monument of Heroes:<br>You're not a Hero and aren't eligible to receive the Wings of Destiny Circlet. Better luck next time.<br><a action=\"bypass -h npc_"
					+ objId + "_Chat 0\">Return</a></body></html>";
		}
		qs.exitQuest(true);
		return htmltext;
	}
}
