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
 * Quest 509 - 509_TheClansPrestige
 */
@Component
public class Quest509TheClansPrestige extends Quest {

	public static final int bJl = 31331;
	public static final int bJm = 25523;
	public static final int bJn = 25514;
	public static final int bJo = 25322;
	public static final int bJp = 25293;
	public static final int bJq = 25290;
	public static final int bJr = 8491;
	public static final int bJs = 8493;
	public static final int bJt = 8492;
	public static final int bJu = 8490;
	public static final int bJv = 8489;

	public Quest509TheClansPrestige(QuestManager questManager) {
	super(509, "509_TheClansPrestige", "509_TheClansPrestige");
		this.addStartNpc(31331);
		this.addKillId(25523, 25514, 25322, 25293, 25290);
		this.addQuestItem(8491, 8493, 8492, 8490, 8489);
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
        int n = qs.getInt("pledge_make_well_known");
        int n2 = getFirstStartNpc();
        if (n2 == 31331) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("pledge_make_well_known", String.valueOf(0), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "grandmagister_valdis_q0509_04.htm";
            } else if (event.equalsIgnoreCase("reply=100")) {
                string2 = "grandmagister_valdis_q0509_06.htm";
            } else if (event.equalsIgnoreCase("reply=101")) {
                qs.unset("pledge_make_well_known");
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "grandmagister_valdis_q0509_07.htm";
            } else if (event.equalsIgnoreCase("reply=102")) {
                qs.set("pledge_make_well_known", String.valueOf(0), true);
                string2 = "grandmagister_valdis_q0509_08.htm";
            } else if (event.equalsIgnoreCase("reply=110")) {
                string2 = "grandmagister_valdis_q0509_01a.htm";
            } else if (event.equalsIgnoreCase("reply=1")) {
                if (n == 0) {
                    qs.set("pledge_make_well_known", String.valueOf(1), true);
                    string2 = "grandmagister_valdis_q0509_09.htm";
                }
            } else if (event.equalsIgnoreCase("reply=2")) {
                if (n == 0) {
                    qs.set("pledge_make_well_known", String.valueOf(2), true);
                    string2 = "grandmagister_valdis_q0509_10.htm";
                }
            } else if (event.equalsIgnoreCase("reply=3")) {
                if (n == 0) {
                    qs.set("pledge_make_well_known", String.valueOf(3), true);
                    string2 = "grandmagister_valdis_q0509_11.htm";
                }
            } else if (event.equalsIgnoreCase("reply=4")) {
                if (n == 0) {
                    qs.set("pledge_make_well_known", String.valueOf(4), true);
                    string2 = "grandmagister_valdis_q0509_12.htm";
                }
            } else if (event.equalsIgnoreCase("reply=5") && n == 0) {
                qs.set("pledge_make_well_known", String.valueOf(5), true);
                string2 = "grandmagister_valdis_q0509_13.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("pledge_make_well_known");
        Clan clan = pc.getClan();
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 31331) break;
                if (clan != null && clan.getLeader().getPlayer() == pc) {
                    if (clan.getLevel() >= 6) {
                        html = "grandmagister_valdis_q0509_01.htm";
                        break;
                    }
                    html = "grandmagister_valdis_q0509_02.htm";
                    qs.exitQuest(true);
                    break;
                }
                html = "grandmagister_valdis_q0509_03.htm";
                qs.exitQuest(true);
                break;
            }
            case 2: {
                if (n2 != 31331) break;
                if (n == 0) {
                    html = "grandmagister_valdis_q0509_05.htm";
                    break;
                }
                if (clan.getLeader().getPlayer() != pc) {
                    qs.unset("pledge_gain_fame");
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.exitQuest(true);
                    html = "grandmagister_valdis_q0509_05a.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount(8489) == 0L) {
                    html = "grandmagister_valdis_q0509_16.htm";
                    break;
                }
                if (n == 1 && qs.getQuestItemsCount(8489) >= 1L) {
                    int n4 = clan.incReputation(200, true, "_509_AClansFame");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8489, 1L);
                    qs.set("pledge_make_well_known", String.valueOf(0), true);
                    html = "grandmagister_valdis_q0509_17.htm";
                    break;
                }
                if (n == 2 && qs.getQuestItemsCount(8490) == 0L) {
                    html = "grandmagister_valdis_q0509_18.htm";
                    break;
                }
                if (n == 2 && qs.getQuestItemsCount(8490) >= 1L) {
                    int n5 = clan.incReputation(438, true, "_509_AClansFame");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8490, 1L);
                    qs.set("pledge_make_well_known", String.valueOf(0), true);
                    html = "grandmagister_valdis_q0509_19.htm";
                    break;
                }
                if (n == 3 && qs.getQuestItemsCount(8491) == 0L) {
                    html = "grandmagister_valdis_q0509_20.htm";
                    break;
                }
                if (n == 3 && qs.getQuestItemsCount(8491) >= 1L) {
                    int n6 = clan.incReputation(400, true, "_509_AClansFame");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8491, 1L);
                    qs.set("pledge_make_well_known", String.valueOf(0), true);
                    html = "grandmagister_valdis_q0509_21.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount(8492) == 0L) {
                    html = "grandmagister_valdis_q0509_22.htm";
                    break;
                }
                if (n == 4 && qs.getQuestItemsCount(8492) >= 1L) {
                    int n7 = clan.incReputation(250, true, "_509_AClansFame");
                    // sendPacket
                    qs.playSound(QuestState.SOUND_FINISH);
                    qs.takeItems(8492, 1L);
                    qs.set("pledge_make_well_known", String.valueOf(0), true);
                    html = "grandmagister_valdis_q0509_23.htm";
                    break;
                }
                if (n == 5 && qs.getQuestItemsCount(8493) == 0L) {
                    html = "grandmagister_valdis_q0509_24.htm";
                    break;
                }
                if (n != 5 || qs.getQuestItemsCount(8493) < 1L) break;
                int n8 = clan.incReputation(150, true, "_509_AClansFame");
                // sendPacket
                qs.playSound(QuestState.SOUND_FINISH);
                qs.takeItems(8493, 1L);
                qs.set("pledge_make_well_known", String.valueOf(0), true);
                html = "grandmagister_valdis_q0509_25.htm";
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
        int n = questState2.getInt("pledge_make_well_known");
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n2 == 25523) {
            if (n == 3 && questState2.getQuestItemsCount(8491) == 0L) {
                questState2.giveItems(8491, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25514) {
            if (n == 5 && questState2.getQuestItemsCount(8493) == 0L) {
                questState2.giveItems(8493, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25322) {
            if (n == 4 && questState2.getQuestItemsCount(8492) == 0L) {
                questState2.giveItems(8492, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25293) {
            if (n == 2 && questState2.getQuestItemsCount(8490) == 0L) {
                questState2.giveItems(8490, 1L);
                questState2.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n2 == 25290 && n == 1 && questState2.getQuestItemsCount(8489) == 0L) {
            questState2.giveItems(8489, 1L);
            questState2.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
