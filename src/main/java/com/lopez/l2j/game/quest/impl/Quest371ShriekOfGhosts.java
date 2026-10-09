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
 * Quest 371 - 371_ShriekOfGhosts
 */
@Component
public class Quest371ShriekOfGhosts extends Quest {

	public static final int bth = 30867;
	public static final int bqT = 30929;
	public static final int bti = 20818;
	public static final int btj = 20820;
	public static final int btk = 20824;
	public static final int btl = 6002;
	public static final int btm = 6003;
	public static final int btn = 6004;
	public static final int bto = 6005;
	public static final int btp = 6006;
	public static final int btq = 5903;

	public Quest371ShriekOfGhosts(QuestManager questManager) {
	super(371, "371_ShriekOfGhosts", "371_ShriekOfGhosts");
		this.addStartNpc(bth);
		this.addTalkId(bqT);
		this.addKillId(bti, btj, btk);
		this.addQuestItem(btq);
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
        if (n == bth) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("spirits_cry_secrets", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "seer_reva_q0371_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                if (qs.getQuestItemsCount(btq) < 1L) {
                    string2 = "seer_reva_q0371_06.htm";
                } else if (qs.getQuestItemsCount(btq) >= 1L && qs.getQuestItemsCount(btq) < 100L) {
                    qs.giveItems(57, qs.getQuestItemsCount(btq) * 1000L + 15000L);
                    qs.takeItems(btq, -1L);
                    string2 = "seer_reva_q0371_07.htm";
                } else if (qs.getQuestItemsCount(btq) >= 100L) {
                    qs.giveItems(57, qs.getQuestItemsCount(btq) * 1000L + 37700L);
                    qs.takeItems(btq, -1L);
                    string2 = "seer_reva_q0371_08.htm";
                }
            } else if (event.equalsIgnoreCase("reply_2")) {
                string2 = "seer_reva_q0371_09.htm";
            } else if (event.equalsIgnoreCase("reply_3")) {
                if (qs.getQuestItemsCount(btq) > 0L) {
                    qs.giveItems(57, qs.getQuestItemsCount(btq) * 1000L);
                }
                qs.takeItems(btq, -1L);
                qs.unset("spirits_cry_secrets");
                qs.exitQuest(true);
                string2 = "seer_reva_q0371_10.htm";
            }
        } else if (n == bqT && event.equalsIgnoreCase("reply_1")) {
            if (qs.getQuestItemsCount(btl) < 1L) {
                string2 = "patrin_q0371_02.htm";
            } else if (qs.getQuestItemsCount(btl) >= 1L) {
                int n2 = ThreadLocalRandom.current().nextInt(100);
                if (n2 < 2) {
                    qs.giveItems(btm, 1L);
                    qs.takeItems(btl, 1L);
                    string2 = "patrin_q0371_03.htm";
                } else if (n2 < 32) {
                    qs.giveItems(btn, 1L);
                    qs.takeItems(btl, 1L);
                    string2 = "patrin_q0371_04.htm";
                } else if (n2 < 62) {
                    qs.giveItems(bto, 1L);
                    qs.takeItems(btl, 1L);
                    string2 = "patrin_q0371_05.htm";
                } else if (n2 < 77) {
                    qs.giveItems(btp, 1L);
                    qs.takeItems(btl, 1L);
                    string2 = "patrin_q0371_06.htm";
                } else {
                    qs.giveItems(btl, 1L);
                    string2 = "patrin_q0371_07.htm";
                }
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("spirits_cry_secrets");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != bth) break;
                if (pc.getLevel() < 59) {
                    html = "seer_reva_q0371_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() < 59) break;
                html = "seer_reva_q0371_02.htm";
                break;
            }
            case 2: {
                if (n2 == bth) {
                    if (n == 1 && qs.getQuestItemsCount(btl) < 1L) {
                        html = "seer_reva_q0371_04.htm";
                        break;
                    }
                    if (n != 1 || qs.getQuestItemsCount(btl) < 1L) break;
                    html = "seer_reva_q0371_05.htm";
                    break;
                }
                if (n2 != bqT || n != 1) break;
                html = "patrin_q0371_01.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("spirits_cry_secrets");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 == bti) {
                int n3 = ThreadLocalRandom.current().nextInt(1000);
                if (n3 < 350) {
                    qs.giveItems(btq, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n3 < 400) {
                    qs.giveItems(btl, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == btj) {
                int n4 = ThreadLocalRandom.current().nextInt(1000);
                if (n4 < 583) {
                    qs.giveItems(btq, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n4 < 673) {
                    qs.giveItems(btl, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            } else if (n2 == btk) {
                int n5 = ThreadLocalRandom.current().nextInt(1000);
                if (n5 < 458) {
                    qs.giveItems(btq, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                } else if (n5 < 538) {
                    qs.giveItems(btl, 1L);
                    qs.playSound(QuestState.SOUND_ITEMGET);
                }
            }
        }
        return null;
    
	}

}
