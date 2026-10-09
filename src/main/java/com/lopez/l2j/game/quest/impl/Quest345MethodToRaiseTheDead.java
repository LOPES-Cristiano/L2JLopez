package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import com.lopez.l2j.network.game.GameSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 345 - 345_MethodToRaiseTheDead
 */
@Component
public class Quest345MethodToRaiseTheDead extends Quest {

	public static final int bnp = 30970;
	public static final int bnq = 30971;
	public static final int bnr = 30912;
	public static final int bns = 30973;
	public static final int bnt = 20789;
	public static final int bnu = 20791;
	public static final int bnv = 4274;
	public static final int bnw = 4275;
	public static final int bnx = 4276;
	public static final int bny = 4277;
	public static final int bnz = 4278;
	public static final int bnA = 4280;
	public static final int bnB = 4281;
	public static final int bnC = 4407;
	public static final int biY = 3456;

	public Quest345MethodToRaiseTheDead(QuestManager questManager) {
	super(345, "345_MethodToRaiseTheDead", "345_MethodToRaiseTheDead");
		this.addStartNpc(30970);
		this.addTalkId(30970, 30912, 30973, 30971);
		this.addQuestItem(4274, 4275, 4276, 4277, 4278, 4281);
		this.addKillId(20789, 20791);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		return onAdvEvent(event, null, qs != null ? qs.getPlayer() : null);
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (qs == null) {
			return null;
		}

		PlayerCharacter pc = qs.playerChar();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "1".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		String html = event;

        String string2 = event;
        int n = qs.getInt("how_to_face_the_dead");
        int n2 = qs.getInt("how_to_face_the_dead_ex");
        int n3 = npc != null ? npc.getNpcId() : 0;
        if (n3 == 30970) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "dorothy_the_locksmith_q0345_03.htm";
            } else if (event.equalsIgnoreCase("reply_1")) {
                string2 = "dorothy_the_locksmith_q0345_04.htm";
                qs.set("how_to_face_the_dead", String.valueOf(1), true);
            } else if (event.equalsIgnoreCase("reply_2") && n == 1 && qs.getQuestItemsCount(4274) >= 1L && qs.getQuestItemsCount(4275) >= 1L && qs.getQuestItemsCount(4276) >= 1L && qs.getQuestItemsCount(4277) >= 1L && qs.getQuestItemsCount(4278) >= 1L) {
                qs.setCond(2);
                qs.set("how_to_face_the_dead", String.valueOf(2), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "dorothy_the_locksmith_q0345_07.htm";
            }
        } else if (n3 == 30912) {
            if (event.equalsIgnoreCase("reply_1")) {
                string2 = "magister_xenovia_q0345_02.htm";
            } else if (event.equalsIgnoreCase("reply_2")) {
                if (n == 2) {
                    if (qs.getQuestItemsCount(57) >= 1000L) {
                        qs.setCond(3);
                        qs.set("how_to_face_the_dead", String.valueOf(3), true);
                        qs.giveItems(4281, 1L);
                        qs.takeItems(57, 1000L);
                        qs.playSound(QuestState.SOUND_MIDDLE);
                        string2 = "magister_xenovia_q0345_03.htm";
                    } else {
                        string2 = "magister_xenovia_q0345_04.htm";
                    }
                }
            } else if (event.equalsIgnoreCase("reply_4")) {
                string2 = "magister_xenovia_q0345_06.htm";
            }
        } else if (n3 == 30973) {
            if (event.equalsIgnoreCase("reply_1")) {
                if (n2 == 1) {
                    string2 = "medium_jar_q0345_03.htm";
                } else if (n2 == 2) {
                    string2 = "medium_jar_q0345_05.htm";
                } else if (n2 == 3) {
                    string2 = "medium_jar_q0345_07.htm";
                }
            }
            if (event.equalsIgnoreCase("reply_2") && n == 7 && n2 == 1) {
                qs.setCond(6);
                qs.set("how_to_face_the_dead", String.valueOf(8), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "medium_jar_q0345_04.htm";
            }
            if (event.equalsIgnoreCase("reply_3") && n == 7 && n2 == 2) {
                qs.setCond(6);
                qs.set("how_to_face_the_dead", String.valueOf(8), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "medium_jar_q0345_06.htm";
            }
            if (event.equalsIgnoreCase("reply_4") && n == 7 && n2 == 3) {
                qs.setCond(7);
                qs.set("how_to_face_the_dead", String.valueOf(8), true);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "medium_jar_q0345_08.htm";
            }
        } else if (n3 == 30971) {
            if (event.equalsIgnoreCase("reply_4")) {
                string2 = "mad_doctor_orpheus_q0345_10.htm";
            } else if (event.equalsIgnoreCase("reply_5") && qs.getQuestItemsCount(4280) > 0L) {
                qs.giveItems(57, qs.getQuestItemsCount(4280) * 104L);
                qs.takeItems(4280, -1L);
                string2 = "mad_doctor_orpheus_q0345_11.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("how_to_face_the_dead");
        int n2 = qs.getInt("how_to_face_the_dead_ex");
        int n3 = npc.getNpcId();
        int n4 = qs.getStateId();
        switch (n4) {
            case 1: {
                if (n3 != 30970) break;
                if (pc.getLevel() < 35 || pc.getLevel() > 42) {
                    html = "dorothy_the_locksmith_q0345_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "dorothy_the_locksmith_q0345_02.htm";
                break;
            }
            case 2: {
                if (n3 == 30970) {
                    if (n == 0) {
                        html = "dorothy_the_locksmith_q0345_04.htm";
                        qs.set("how_to_face_the_dead", String.valueOf(1), true);
                        break;
                    }
                    if (n == 1 && (qs.getQuestItemsCount(4274) == 0L || qs.getQuestItemsCount(4275) == 0L || qs.getQuestItemsCount(4276) == 0L || qs.getQuestItemsCount(4277) == 0L || qs.getQuestItemsCount(4278) == 0L)) {
                        html = "dorothy_the_locksmith_q0345_05.htm";
                        break;
                    }
                    if (n == 1 && qs.getQuestItemsCount(4274) >= 1L && qs.getQuestItemsCount(4275) >= 1L && qs.getQuestItemsCount(4276) >= 1L && qs.getQuestItemsCount(4277) >= 1L && qs.getQuestItemsCount(4278) >= 1L) {
                        html = "dorothy_the_locksmith_q0345_06.htm";
                        break;
                    }
                    if (n == 2) {
                        html = "dorothy_the_locksmith_q0345_08.htm";
                        break;
                    }
                    if (n == 3) {
                        html = "dorothy_the_locksmith_q0345_09.htm";
                        break;
                    }
                    if (n == 7) {
                        html = "dorothy_the_locksmith_q0345_12.htm";
                        break;
                    }
                    if (n == 8 && (n2 == 1 || n2 == 2)) {
                        qs.giveItems(4407, 3L);
                        qs.giveItems(57, 5390L + 70L * qs.getQuestItemsCount(4280));
                        qs.takeItems(4280, -1L);
                        qs.unset("how_to_face_the_dead");
                        qs.unset("how_to_face_the_dead_ex");
                        qs.playSound(QuestState.SOUND_FINISH);
                        qs.exitQuest(true);
                        html = "dorothy_the_locksmith_q0345_13.htm";
                        break;
                    }
                    if (n != 8 || n2 != 3) break;
                    int n5 = ThreadLocalRandom.current().nextInt(100);
                    if (n5 <= 92) {
                        qs.giveItems(4407, 5L);
                    } else {
                        qs.giveItems(3456, 1L);
                    }
                    qs.giveItems(57, 3040L + 70L * qs.getQuestItemsCount(4280));
                    qs.takeItems(4280, -1L);
                    qs.unset("how_to_face_the_dead");
                    qs.unset("how_to_face_the_dead_ex");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    html = "dorothy_the_locksmith_q0345_14.htm";
                    break;
                }
                if (n3 == 30912) {
                    if (n == 2) {
                        html = "magister_xenovia_q0345_01.htm";
                        break;
                    }
                    if (n != 7 && n != 8 && qs.getQuestItemsCount(4281) < 1L) break;
                    html = "magister_xenovia_q0345_07.htm";
                    break;
                }
                if (n3 == 30973) {
                    if (n == 3) {
                        qs.takeItems(4281, -1L);
                        qs.takeItems(4274, -1L);
                        qs.takeItems(4275, -1L);
                        qs.takeItems(4276, -1L);
                        qs.takeItems(4277, -1L);
                        qs.takeItems(4278, -1L);
                        qs.set("how_to_face_the_dead", String.valueOf(7), true);
                        int n6 = ThreadLocalRandom.current().nextInt(100);
                        if (n6 <= 39) {
                            qs.set("how_to_face_the_dead_ex", String.valueOf(1), true);
                        } else if (n6 <= 79) {
                            qs.set("how_to_face_the_dead_ex", String.valueOf(2), true);
                        } else {
                            qs.set("how_to_face_the_dead_ex", String.valueOf(3), true);
                        }
                        html = "medium_jar_q0345_01.htm";
                        break;
                    }
                    if (n == 7 && n2 == 1) {
                        html = "medium_jar_q0345_03t.htm";
                        break;
                    }
                    if (n == 7 && n2 == 2) {
                        html = "medium_jar_q0345_05t.htm";
                        break;
                    }
                    if (n == 7 && n2 == 3) {
                        html = "medium_jar_q0345_07t.htm";
                        break;
                    }
                    if (n != 8) break;
                    html = "medium_jar_q0345_09.htm";
                    break;
                }
                if (n3 != 30971 || qs.getQuestItemsCount(4280) <= 0L) break;
                html = "mad_doctor_orpheus_q0345_08.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = qs.getInt("how_to_face_the_dead");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1 && (n2 == 20789 || n2 == 20791)) {
            int n3 = ThreadLocalRandom.current().nextInt(100);
            if (n3 <= 5) {
                if (qs.getQuestItemsCount(4274) == 0L) {
                    qs.giveItems(4274, 1L);
                } else {
                    qs.giveItems(4280, 1L);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else if (n3 <= 11) {
                if (qs.getQuestItemsCount(4275) == 0L) {
                    qs.giveItems(4275, 1L);
                } else {
                    qs.giveItems(4280, 1L);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else if (n3 <= 17) {
                if (qs.getQuestItemsCount(4276) == 0L) {
                    qs.giveItems(4276, 1L);
                } else {
                    qs.giveItems(4280, 1L);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else if (n3 <= 23) {
                if (qs.getQuestItemsCount(4277) == 0L) {
                    qs.giveItems(4277, 1L);
                } else {
                    qs.giveItems(4280, 1L);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else if (n3 <= 29) {
                if (qs.getQuestItemsCount(4278) == 0L) {
                    qs.giveItems(4278, 1L);
                } else {
                    qs.giveItems(4280, 1L);
                }
                qs.playSound(QuestState.SOUND_ITEMGET);
            } else if (n3 <= 60) {
                qs.giveItems(4280, 1L);
            }
        }
        return null;
    
	}

}
