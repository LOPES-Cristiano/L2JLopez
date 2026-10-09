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
 * Quest 347 - 347_GoGetTheCalculator
 */
@Component
public class Quest347GoGetTheCalculator extends Quest {

	public static final int blacksmith_bronp = 30526;
	public static final int blacksmith_silvery = 30527;
	public static final int elder_spiron = 30532;
	public static final int elder_balanki = 30533;
	public static final int gemstone_beast = 20540;
	public static final int q_gemstone = 4286;
	public static final int q_calculator = 4285;
	public static final int calculator = 4393;

	public Quest347GoGetTheCalculator(QuestManager questManager) {
	super(347, "347_GoGetTheCalculator", "347_GoGetTheCalculator");
		this.addStartNpc(30526);
		this.addTalkId(30527, 30532, 30533);
		this.addKillId(20540);
		this.addQuestItem(4286);
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
        int n = qs.getInt("get_calculator");
        if (event.equalsIgnoreCase("quest_accept")) {
            qs.setCond(1);
            qs.set("get_calculator", String.valueOf(100), true);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
            string2 = "blacksmith_bronp_q0347_08.htm";
        } else if (event.equalsIgnoreCase("reply_7") && n == 600 && qs.getQuestItemsCount(4285) >= 1L) {
            qs.unset("get_calculator");
            qs.takeItems(4285, -1L);
            qs.giveItems(4393, 1L);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "blacksmith_bronp_q0347_10.htm";
        } else if (event.equalsIgnoreCase("reply_8") && n == 600 && qs.getQuestItemsCount(4285) >= 1L) {
            qs.unset("get_calculator");
            qs.takeItems(4285, -1L);
            qs.giveItems(57, 1000L, true);
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(true);
            string2 = "blacksmith_bronp_q0347_11.htm";
        } else if (event.equalsIgnoreCase("reply_3")) {
            qs.set("get_calculator", String.valueOf(200 + n), true);
            if (n == 100) {
                qs.setCond(3);
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else if (n == 200) {
                qs.setCond(4);
                qs.playSound(QuestState.SOUND_MIDDLE);
            }
            string2 = "elder_spiron_q0347_02.htm";
        } else if (event.equalsIgnoreCase("reply_2")) {
            if (qs.getQuestItemsCount(57) >= 100L) {
                qs.set("get_calculator", String.valueOf(100 + n), true);
                qs.takeItems(57, 100L);
                if (n == 100) {
                    qs.setCond(2);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                } else if (n == 300) {
                    qs.setCond(4);
                    qs.playSound(QuestState.SOUND_MIDDLE);
                }
                string2 = "elder_balanki_q0347_02.htm";
            } else {
                string2 = "elder_balanki_q0347_03.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("get_calculator");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30526) break;
                if (pc.getLevel() >= 12) {
                    html = "blacksmith_bronp_q0347_01.htm";
                    break;
                }
                html = "blacksmith_bronp_q0347_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 == 30526) {
                    if (qs.getQuestItemsCount(4285) >= 1L) {
                        html = "blacksmith_bronp_q0347_09.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(4285) == 0L && n == 600) {
                        html = "blacksmith_bronp_q0347_12.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(4285) == 0L && n == 100) {
                        html = "blacksmith_bronp_q0347_13.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(4285) == 0L && (n == 200 || n == 300)) {
                        html = "blacksmith_bronp_q0347_14.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(4285) != 0L || n != 400 && n != 500 && n != 600) break;
                    html = "blacksmith_bronp_q0347_15.htm";
                    break;
                }
                if (n2 == 30527) {
                    if (n == 100 || n == 200 || n == 300) {
                        html = "blacksmith_silvery_q0347_01.htm";
                        break;
                    }
                    if (n == 400) {
                        qs.setCond(5);
                        qs.set("get_calculator", String.valueOf(500), true);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "blacksmith_silvery_q0347_02.htm";
                        break;
                    }
                    if (n == 500 && qs.getQuestItemsCount(4286) >= 10L) {
                        qs.setCond(6);
                        qs.set("get_calculator", String.valueOf(600), true);
                        qs.giveItems(4285, 1L);
                        qs.takeItems(4286, -1L);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        html = "blacksmith_silvery_q0347_03.htm";
                        break;
                    }
                    if (n == 500 && qs.getQuestItemsCount(4286) < 10L) {
                        html = "blacksmith_silvery_q0347_04.htm";
                        break;
                    }
                    if (n != 600 && n != 600) break;
                    html = "blacksmith_silvery_q0347_05.htm";
                    break;
                }
                if (n2 == 30532) {
                    if (n == 100 || n == 200) {
                        html = "elder_spiron_q0347_01.htm";
                        break;
                    }
                    if (n != 300 && n != 400 && n != 500 && n != 600) break;
                    html = "elder_spiron_q0347_05.htm";
                    break;
                }
                if (n2 != 30533) break;
                if (n == 100 || n == 300) {
                    html = "elder_balanki_q0347_01.htm";
                    break;
                }
                if (n != 200 && n != 400 && n != 500 && n != 600) break;
                html = "elder_balanki_q0347_04.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        int n2 = qs.getInt("get_calculator");
        if (n == 20540 && n2 == 500 && qs.getQuestItemsCount(4286) < 10L && ThreadLocalRandom.current().nextInt(10) <= 4) {
            qs.giveItems(4286, 1L);
            if (qs.getQuestItemsCount(4286) >= 10L) {
                qs.playSound(QuestState.SOUND_MIDDLE);
            } else {
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        }
        return null;
    
	}

}
