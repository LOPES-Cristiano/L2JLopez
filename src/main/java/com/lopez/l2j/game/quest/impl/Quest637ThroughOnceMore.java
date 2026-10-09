package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.Race;
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
 * Quest 637 - 637_ThroughOnceMore
 */
@Component
public class Quest637ThroughOnceMore extends Quest {

	public static final int bQC = 32010;
	public static final int aAQ = 21565;
	public static final int aAR = 21566;
	public static final int aAS = 21567;
	public static final int bQG = 8066;
	public static final int bQE = 8064;
	public static final int bQF = 8065;
	public static final int bQD = 8067;
	public static final int bQH = 8273;

	public Quest637ThroughOnceMore(QuestManager questManager) {
	super(637, "637_ThroughOnceMore", "637_ThroughOnceMore");
		this.addStartNpc(32010);
		this.addKillId(21565, 21566, 21567);
		this.addQuestItem(8066);
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
        if (n == 32010) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("beyond_the_door_agai", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "falsepriest_flauron_q0637_11.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "falsepriest_flauron_q0637_06.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "falsepriest_flauron_q0637_07.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                string2 = "falsepriest_flauron_q0637_08.htm";
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = "falsepriest_flauron_q0637_09.htm";
            } else if (event.equalsIgnoreCase("reply_5")) {
                string2 = "falsepriest_flauron_q0637_10.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("beyond_the_door_agai");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 32010) break;
                if (pc.getLevel() >= 73 && qs.getQuestItemsCount(8065) >= 1L && qs.getQuestItemsCount(8067) == 0L) {
                    html = "falsepriest_flauron_q0637_01.htm";
                    break;
                }
                html = "falsepriest_flauron_q0637_02.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 32010) break;
                if (n == 1 && qs.getQuestItemsCount(8064) >= 1L && qs.getQuestItemsCount(8065) == 0L && qs.getQuestItemsCount(8067) == 0L) {
                    html = "falsepriest_flauron_q0637_03.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount(8064) == 0L && qs.getQuestItemsCount(8065) == 0L && qs.getQuestItemsCount(8067) == 0L) {
                    html = "falsepriest_flauron_q0637_04.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount(8067) >= 1L) {
                    html = "falsepriest_flauron_q0637_05.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount(8066) < 10L) {
                    html = "falsepriest_flauron_q0637_12.htm";
                    break;
                }
                if (n != 1 || qs.getQuestItemsCount(8066) < 10L) break;
                qs.giveItems(8067, 1L);
                qs.giveItems(8273, 10L);
                qs.takeItems(8064, -1L);
                qs.takeItems(8065, -1L);
                qs.takeItems(8066, -1L);
                qs.unset("beyond_the_door_agai");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                html = "falsepriest_flauron_q0637_13.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("beyond_the_door_agai");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1 && qs.getQuestItemsCount(8066) < 10L) {
            if (n2 == 21565) {
                if (ThreadLocalRandom.current().nextInt(100) < 84) {
                    qs.giveItems(8066, 1L);
                    if (qs.getQuestItemsCount(8066) >= 9L) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    } else {
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 21566) {
                if (ThreadLocalRandom.current().nextInt(100) < 92) {
                    qs.giveItems(8066, 1L);
                    if (qs.getQuestItemsCount(8066) > 9L) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    } else {
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            } else if (n2 == 21567) {
                if (ThreadLocalRandom.current().nextInt(100) < 10) {
                    qs.giveItems(8066, 1L);
                    if (qs.getQuestItemsCount(8066) >= 9L) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    } else {
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                } else {
                    qs.giveItems(8066, 1L);
                    if (qs.getQuestItemsCount(8066) >= 9L) {
                        qs.setCond(2);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                    } else {
                        qs.playSound(QuestState.SOUND_ITEMGET);
                    }
                }
            }
        }
        return null;
    
	}

}
