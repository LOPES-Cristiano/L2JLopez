package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 352 - 352_HelpRoodRaiseANewPet
 */
@Component
public class Quest352HelpRoodRaiseANewPet extends Quest {

	public static final int bqv = 31067;
	public static final int bqw = 20786;
	public static final int bqx = 20787;
	public static final int bqy = 5860;
	public static final int bqz = 5861;

	public Quest352HelpRoodRaiseANewPet(QuestManager questManager) {
	super(352, "352_HelpRoodRaiseANewPet", "352_HelpRoodRaiseANewPet");
		this.addStartNpc(bqv);
		this.addKillId(bqw, bqx);
		this.addQuestItem(bqy, bqz);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		String html = event;

        String string2 = event;
        int n = getFirstStartNpc();
        if (n == bqv) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("how_about_new_pet", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "pet_manager_rood_q0352_05.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "pet_manager_rood_q0352_09.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "pet_manager_rood_q0352_10.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                qs.unset("how_about_new_pet");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "pet_manager_rood_q0352_11.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = "pet_manager_rood_q0352_04.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("how_about_new_pet");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != bqv) break;
                if (pc.getLevel() >= 39) {
                    html = "pet_manager_rood_q0352_02.htm";
                    break;
                }
                qs.exitQuest(true);
                html = "pet_manager_rood_q0352_01.htm";
                break;
            }
            case 2: {
                if (n2 != bqv || n != 1) break;
                if (qs.getQuestItemsCount(bqy) < 1L && qs.getQuestItemsCount(bqz) < 1L) {
                    html = "pet_manager_rood_q0352_06.htm";
                    break;
                }
                if (qs.getQuestItemsCount(bqy) >= 1L && qs.getQuestItemsCount(bqz) < 1L) {
                    if (qs.getQuestItemsCount(bqy) >= 10L) {
                        qs.giveItems(57, qs.getQuestItemsCount(bqy) * 34L + 4000L);
                    } else {
                        qs.giveItems(57, qs.getQuestItemsCount(bqy) * 34L + 2000L);
                    }
                    qs.takeItems(bqy, -1L);
                    html = "pet_manager_rood_q0352_07.htm";
                    break;
                }
                if (qs.getQuestItemsCount(bqz) < 1L) break;
                qs.giveItems(57, 4000L + (qs.getQuestItemsCount(bqy) * 34L + qs.getQuestItemsCount(bqz) * 1025L));
                qs.takeItems(bqz, -1L);
                qs.takeItems(bqy, -1L);
                html = "pet_manager_rood_q0352_08.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("how_about_new_pet");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 == bqw) {
                int n3 = ThreadLocalRandom.current().nextInt(100);
                if (n3 < 46) {
                    qs.giveItems(bqy, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n3 < 48) {
                    qs.giveItems(bqz, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == bqx) {
                int n4 = ThreadLocalRandom.current().nextInt(100);
                if (n4 < 69) {
                    qs.giveItems(bqy, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n4 < 71) {
                    qs.giveItems(bqz, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
