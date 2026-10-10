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
 * Quest 508 - 508_TheClansReputation
 */
@Component
public class Quest508TheClansReputation extends Quest {

	public static final int bIY = 30868;
	public static final int bIZ = 25051;
	public static final int bJa = 25245;
	public static final int bJb = 25252;
	public static final int bJc = 25255;
	public static final int bJd = 25140;
	public static final int bJe = 25524;
	public static final int bJf = 8494;
	public static final int bJg = 8280;
	public static final int bJh = 8277;
	public static final int bJi = 8279;
	public static final int bJj = 8281;
	public static final int bJk = 8282;

	public Quest508TheClansReputation(QuestManager questManager) {
	super(508, "508_TheClansReputation", "508_TheClansReputation");
		this.addStartNpc(30868);
		this.addKillId(25051, 25245, 25252, 25255, 25140, 25524);
		this.addQuestItem(8494, 8280, 8277, 8281, 8282, 8279);
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
        int n = qs.getInt("pledge_gain_fame");
        int n2 = getFirstStartNpc();
        if (n2 == 30868) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("pledge_gain_fame", String.valueOf(0), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "sir_eric_rodemai_q0508_04.htm";
            } else if (event.equalsIgnoreCase("reply=100")) {
                string2 = "sir_eric_rodemai_q0508_06.htm";
            } else if (event.equalsIgnoreCase("reply=101")) {
                qs.unset("pledge_gain_fame");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "sir_eric_rodemai_q0508_07.htm";
            } else if (event.equalsIgnoreCase("reply=102")) {
                qs.set("pledge_gain_fame", String.valueOf(0), true);
                string2 = "sir_eric_rodemai_q0508_08.htm";
            } else if (event.equalsIgnoreCase("reply=110")) {
                string2 = "sir_eric_rodemai_q0508_01a.htm";
            } else if (event.equalsIgnoreCase("reply=2")) {
                if (n == 0) {
                    qs.set("pledge_gain_fame", String.valueOf(2), true);
                    string2 = "sir_eric_rodemai_q0508_10.htm";
                }
            } else if (event.equalsIgnoreCase("reply=4")) {
                if (n == 0) {
                    qs.set("pledge_gain_fame", String.valueOf(4), true);
                    string2 = "sir_eric_rodemai_q0508_12.htm";
                }
            } else if (event.equalsIgnoreCase("reply=5")) {
                if (n == 0) {
                    qs.set("pledge_gain_fame", String.valueOf(5), true);
                    string2 = "sir_eric_rodemai_q0508_13.htm";
                }
            } else if (event.equalsIgnoreCase("reply=6")) {
                if (n == 0) {
                    qs.set("pledge_gain_fame", String.valueOf(6), true);
                    string2 = "sir_eric_rodemai_q0508_14.htm";
                }
            } else if (event.equalsIgnoreCase("reply=7")) {
                if (n == 0) {
                    qs.set("pledge_gain_fame", String.valueOf(7), true);
                    string2 = "sir_eric_rodemai_q0508_15.htm";
                }
            } else if (event.equalsIgnoreCase("reply=8") && n == 0) {
                qs.set("pledge_gain_fame", String.valueOf(8), true);
                string2 = "sir_eric_rodemai_q0508_15a.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("pledge_gain_fame");
        Clan clan = pc.getClan();
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30868) break;
                if (clan != null && clan.getLeader().getPlayer() == pc) {
                    if (clan.getLevel() >= 5) {
                        html = "sir_eric_rodemai_q0508_01.htm";
                        break;
                    }
                    html = "sir_eric_rodemai_q0508_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "sir_eric_rodemai_q0508_03.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 30868) break;
                if (n == 0) {
                    html = "sir_eric_rodemai_q0508_05.htm";
                    break;
                }
                if (clan.getLeader().getPlayer() != pc) {
                    qs.unset("pledge_gain_fame");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    html = "sir_eric_rodemai_q0508_05a.htm";
                    break;
                }
                if (n == 2 && qs.getQuestItemsCount(8277) == 0L) {
                    html = "sir_eric_rodemai_q0508_18.htm";
                    break;
                }
                if (n == 2 && qs.getQuestItemsCount(8277) >= 1L) {
                    int n4 = clan.incReputation(85, true, "_508_TheClansReputation");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8277, 1L);
                    qs.set("pledge_gain_fame", String.valueOf(0), true);
                    html = "sir_eric_rodemai_q0508_19.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount(8279) == 0L) {
                    html = "sir_eric_rodemai_q0508_22.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount(8279) >= 1L) {
                    int n5 = clan.incReputation(65, true, "_508_TheClansReputation");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8279, 1L);
                    qs.set("pledge_gain_fame", String.valueOf(0), true);
                    html = "sir_eric_rodemai_q0508_23.htm";
                    break;
                }
                if (n == 5 && qs.getQuestItemsCount(8280) == 0L) {
                    html = "sir_eric_rodemai_q0508_24.htm";
                    break;
                }
                if (n == 5 && qs.getQuestItemsCount(8280) >= 1L) {
                    int n6 = clan.incReputation(50, true, "_508_TheClansReputation");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8280, 1L);
                    qs.set("pledge_gain_fame", String.valueOf(0), true);
                    html = "sir_eric_rodemai_q0508_25.htm";
                    break;
                }
                if (n == 6 && qs.getQuestItemsCount(8281) == 0L) {
                    html = "sir_eric_rodemai_q0508_26.htm";
                    break;
                }
                if (n == 6 && qs.getQuestItemsCount(8281) >= 1L) {
                    int n7 = clan.incReputation(125, true, "_508_TheClansReputation");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8281, 1L);
                    qs.set("pledge_gain_fame", String.valueOf(0), true);
                    html = "sir_eric_rodemai_q0508_27.htm";
                    break;
                }
                if (n == 7 && qs.getQuestItemsCount(8282) == 0L) {
                    html = "sir_eric_rodemai_q0508_28.htm";
                    break;
                }
                if (n == 7 && qs.getQuestItemsCount(8282) >= 1L) {
                    int n8 = clan.incReputation(71, true, "_508_TheClansReputation");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8282, 1L);
                    qs.set("pledge_gain_fame", String.valueOf(0), true);
                    html = "sir_eric_rodemai_q0508_29.htm";
                    break;
                }
                if (n == 8 && qs.getQuestItemsCount(8494) == 0L) {
                    html = "sir_eric_rodemai_q0508_30.htm";
                    break;
                }
                if (n != 8 || qs.getQuestItemsCount(8494) < 1L) break;
                int n9 = clan.incReputation(80, true, "_508_TheClansReputation");
                // sendPacket
                qs.playSound(QuestState.SOUND_FINISH);
                qs.takeItems(8494, 1L);
                qs.set("pledge_gain_fame", String.valueOf(0), true);
                html = "sir_eric_rodemai_q0508_31.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        PlayerCharacter player = pc;
        try {
            player = pc.getClan().getLeader().getPlayer();
        }
        catch (Exception exception) {
            return null;
        }
        if (player == null) {
            return null;
        }
        if (!pc.equals(player) && Math.hypot(player.x() - npc.x(), player.y() - npc.y()) > com.lopez.l2j.config.Config.ALT_PARTY_RANGE) {
            return null;
        }
        var leaderSessionOpt = com.lopez.l2j.game.world.GameWorld.getInstance().player(player.objectId());
        if (leaderSessionOpt.isEmpty() || !(leaderSessionOpt.get() instanceof com.lopez.l2j.network.game.GameSession leaderSession)) {
            return null;
        }
        QuestState questState2 = leaderSession.getQuestState(this.getName());
        if (questState2 == null || !questState2.isStarted() || questState2.getCond() != 1) {
            return null;
        }
        int n = questState2.getInt("pledge_gain_fame");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 25051) {
            if (n == 7 && questState2.getQuestItemsCount(8282) == 0L) {
                questState2.giveItems(8282, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25245) {
            if (n == 6 && questState2.getQuestItemsCount(8281) == 0L) {
                questState2.giveItems(8281, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25252) {
            if (n == 2 && questState2.getQuestItemsCount(8277) == 0L) {
                questState2.giveItems(8277, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25255) {
            if (n == 5 && questState2.getQuestItemsCount(8280) == 0L) {
                questState2.giveItems(8280, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25140) {
            if (n == 4 && questState2.getQuestItemsCount(8279) == 0L) {
                questState2.giveItems(8279, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25524 && n == 8 && questState2.getQuestItemsCount(8494) == 0L) {
            questState2.giveItems(8494, 1L);
            questState2.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
