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
 * Quest 372 - 372_LegacyOfInsolence
 */
@Component
public class Quest372LegacyOfInsolence extends Quest {

	public static final int btr = 30844;
	public static final int bts = 30839;
	public static final int btt = 30855;
	public static final int bqT = 30929;
	public static final int bnM = 31001;
	public static final int btu = 20817;
	public static final int btv = 20821;
	public static final int btw = 20825;
	public static final int bnS = 20829;
	public static final int btx = 21069;
	public static final int bty = 21063;
	public static final int btz = 5989;
	public static final int btA = 5990;
	public static final int btB = 5991;
	public static final int btC = 5992;
	public static final int btD = 5993;
	public static final int btE = 5994;
	public static final int btF = 5995;
	public static final int btG = 5996;
	public static final int btH = 5997;
	public static final int btI = 5998;
	public static final int btJ = 5999;
	public static final int btK = 6000;
	public static final int btL = 6001;
	public static final int btM = 5496;
	public static final int btN = 5508;
	public static final int btO = 5525;
	public static final int btP = 5368;
	public static final int btQ = 5392;
	public static final int btR = 5426;
	public static final int btS = 5497;
	public static final int btT = 5509;
	public static final int btU = 5526;
	public static final int btV = 5370;
	public static final int btW = 5394;
	public static final int btX = 5428;
	public static final int btY = 5502;
	public static final int btZ = 5514;
	public static final int bua = 5527;
	public static final int bub = 5380;
	public static final int buc = 5404;
	public static final int bud = 5430;
	public static final int bue = 5503;
	public static final int buf = 5515;
	public static final int bug = 5528;
	public static final int buh = 5382;
	public static final int bui = 5406;
	public static final int buj = 5432;
	public static final int buk = 5984;
	public static final int bul = 5985;
	public static final int bum = 5986;
	public static final int bun = 5987;
	public static final int buo = 5988;
	public static final int bup = 5972;
	public static final int buq = 5973;
	public static final int bur = 5974;
	public static final int bus = 5975;
	public static final int but = 5976;
	public static final int buu = 5977;
	public static final int buv = 5978;
	public static final int buw = 5979;
	public static final int bux = 5980;
	public static final int buy = 5981;
	public static final int buz = 5982;
	public static final int buA = 5983;
	public static final int buB = 5966;
	public static final int buC = 5967;
	public static final int buD = 5968;
	public static final int buE = 5969;

	public Quest372LegacyOfInsolence(QuestManager questManager) {
	super(372, "372_LegacyOfInsolence", "372_LegacyOfInsolence");
		this.addStartNpc(30844);
		this.addTalkId(30839, 30855, 30929, 31001);
		this.addKillId(20817, 20821, 20825, 20829, 21069, 21063);
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
        if (n == 30844) {
            if (event.equalsIgnoreCase("quest_accept")) {
                qs.setCond(1);
                qs.set("legacy_of_insolence", String.valueOf(1), true);
                qs.setState(State.STARTED);
                qs.playSound(QuestState.SOUND_ACCEPT);
                string2 = "whouse_keeper_walderal_q0372_04.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=1")) {
                string2 = "whouse_keeper_walderal_q0372_03.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=3")) {
                if (qs.getQuestItemsCount(5989) < 1L || qs.getQuestItemsCount(5990) < 1L || qs.getQuestItemsCount(5991) < 1L || qs.getQuestItemsCount(5992) < 1L || qs.getQuestItemsCount(5993) < 1L || qs.getQuestItemsCount(5994) < 1L || qs.getQuestItemsCount(5995) < 1L || qs.getQuestItemsCount(5996) < 1L || qs.getQuestItemsCount(5997) < 1L || qs.getQuestItemsCount(5998) < 1L || qs.getQuestItemsCount(5999) < 1L || qs.getQuestItemsCount(6000) < 1L || qs.getQuestItemsCount(6001) < 1L) {
                    string2 = "whouse_keeper_walderal_q0372_06.htm";
                } else if (qs.getQuestItemsCount(5989) >= 1L && qs.getQuestItemsCount(5990) >= 1L && qs.getQuestItemsCount(5991) >= 1L && qs.getQuestItemsCount(5992) >= 1L && qs.getQuestItemsCount(5993) >= 1L && qs.getQuestItemsCount(5994) >= 1L && qs.getQuestItemsCount(5995) >= 1L && qs.getQuestItemsCount(5996) >= 1L && qs.getQuestItemsCount(5997) >= 1L && qs.getQuestItemsCount(5998) >= 1L && qs.getQuestItemsCount(5999) >= 1L && qs.getQuestItemsCount(6000) >= 1L && qs.getQuestItemsCount(6001) >= 1L) {
                    string2 = "whouse_keeper_walderal_q0372_07.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=4")) {
                string2 = "whouse_keeper_walderal_q0372_08.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=5")) {
                qs.playSound(QuestState.SOUND_FINISH);
                qs.exitQuest(true);
                string2 = "whouse_keeper_walderal_q0372_09.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=6")) {
                string2 = "whouse_keeper_walderal_q0372_11.htm";
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=7")) {
                if (qs.getQuestItemsCount(5989) < 1L || qs.getQuestItemsCount(5990) < 1L || qs.getQuestItemsCount(5991) < 1L || qs.getQuestItemsCount(5992) < 1L || qs.getQuestItemsCount(5993) < 1L || qs.getQuestItemsCount(5994) < 1L || qs.getQuestItemsCount(5995) < 1L || qs.getQuestItemsCount(5996) < 1L || qs.getQuestItemsCount(5997) < 1L || qs.getQuestItemsCount(5998) < 1L || qs.getQuestItemsCount(5999) < 1L || qs.getQuestItemsCount(6000) < 1L || qs.getQuestItemsCount(6001) < 1L) {
                    string2 = "whouse_keeper_walderal_q0372_07e.htm";
                } else if (qs.getQuestItemsCount(5989) >= 1L && qs.getQuestItemsCount(5990) >= 1L && qs.getQuestItemsCount(5991) >= 1L && qs.getQuestItemsCount(5992) >= 1L && qs.getQuestItemsCount(5993) >= 1L && qs.getQuestItemsCount(5994) >= 1L && qs.getQuestItemsCount(5995) >= 1L && qs.getQuestItemsCount(5996) >= 1L && qs.getQuestItemsCount(5997) >= 1L && qs.getQuestItemsCount(5998) >= 1L && qs.getQuestItemsCount(5999) >= 1L && qs.getQuestItemsCount(6000) >= 1L && qs.getQuestItemsCount(6001) >= 1L) {
                    qs.takeItems(5989, 1L);
                    qs.takeItems(5990, 1L);
                    qs.takeItems(5991, 1L);
                    qs.takeItems(5992, 1L);
                    qs.takeItems(5993, 1L);
                    qs.takeItems(5994, 1L);
                    qs.takeItems(5995, 1L);
                    qs.takeItems(5996, 1L);
                    qs.takeItems(5997, 1L);
                    qs.takeItems(5998, 1L);
                    qs.takeItems(5999, 1L);
                    qs.takeItems(6000, 1L);
                    qs.takeItems(6001, 1L);
                    int n2 = ThreadLocalRandom.current().nextInt(100);
                    if (n2 < 10) {
                        qs.giveItems(5496, 1L);
                    } else if (n2 < 20) {
                        qs.giveItems(5508, 1L);
                    } else if (n2 < 30) {
                        qs.giveItems(5525, 1L);
                    } else if (n2 < 40) {
                        qs.giveItems(5496, 1L);
                        qs.giveItems(5508, 1L);
                        qs.giveItems(5525, 1L);
                    } else if (n2 < 51) {
                        qs.giveItems(5368, 1L);
                    } else if (n2 < 62) {
                        qs.giveItems(5392, 1L);
                    } else if (n2 < 79) {
                        qs.giveItems(5426, 1L);
                    } else if (n2 < 100) {
                        qs.giveItems(5368, 1L);
                        qs.giveItems(5392, 1L);
                        qs.giveItems(5426, 1L);
                    }
                    string2 = "whouse_keeper_walderal_q0372_07a.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=8")) {
                if (qs.getQuestItemsCount(5989) < 1L || qs.getQuestItemsCount(5990) < 1L || qs.getQuestItemsCount(5991) < 1L || qs.getQuestItemsCount(5992) < 1L || qs.getQuestItemsCount(5993) < 1L || qs.getQuestItemsCount(5994) < 1L || qs.getQuestItemsCount(5995) < 1L || qs.getQuestItemsCount(5996) < 1L || qs.getQuestItemsCount(5997) < 1L || qs.getQuestItemsCount(5998) < 1L || qs.getQuestItemsCount(5999) < 1L || qs.getQuestItemsCount(6000) < 1L || qs.getQuestItemsCount(6001) < 1L) {
                    string2 = "whouse_keeper_walderal_q0372_07e.htm";
                } else if (qs.getQuestItemsCount(5989) >= 1L && qs.getQuestItemsCount(5990) >= 1L && qs.getQuestItemsCount(5991) >= 1L && qs.getQuestItemsCount(5992) >= 1L && qs.getQuestItemsCount(5993) >= 1L && qs.getQuestItemsCount(5994) >= 1L && qs.getQuestItemsCount(5995) >= 1L && qs.getQuestItemsCount(5996) >= 1L && qs.getQuestItemsCount(5997) >= 1L && qs.getQuestItemsCount(5998) >= 1L && qs.getQuestItemsCount(5999) >= 1L && qs.getQuestItemsCount(6000) >= 1L && qs.getQuestItemsCount(6001) >= 1L) {
                    qs.takeItems(5989, 1L);
                    qs.takeItems(5990, 1L);
                    qs.takeItems(5991, 1L);
                    qs.takeItems(5992, 1L);
                    qs.takeItems(5993, 1L);
                    qs.takeItems(5994, 1L);
                    qs.takeItems(5995, 1L);
                    qs.takeItems(5996, 1L);
                    qs.takeItems(5997, 1L);
                    qs.takeItems(5998, 1L);
                    qs.takeItems(5999, 1L);
                    qs.takeItems(6000, 1L);
                    qs.takeItems(6001, 1L);
                    int n3 = ThreadLocalRandom.current().nextInt(100);
                    if (n3 < 10) {
                        qs.giveItems(5497, 1L);
                    } else if (n3 < 20) {
                        qs.giveItems(5509, 1L);
                    } else if (n3 < 30) {
                        qs.giveItems(5526, 1L);
                    } else if (n3 < 40) {
                        qs.giveItems(5497, 1L);
                        qs.giveItems(5509, 1L);
                        qs.giveItems(5526, 1L);
                    } else if (n3 < 51) {
                        qs.giveItems(5370, 1L);
                    } else if (n3 < 62) {
                        qs.giveItems(5394, 1L);
                    } else if (n3 < 79) {
                        qs.giveItems(5428, 1L);
                    } else if (n3 < 100) {
                        qs.giveItems(5370, 1L);
                        qs.giveItems(5394, 1L);
                        qs.giveItems(5428, 1L);
                    }
                    string2 = "whouse_keeper_walderal_q0372_07b.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=9")) {
                if (qs.getQuestItemsCount(5989) < 1L || qs.getQuestItemsCount(5990) < 1L || qs.getQuestItemsCount(5991) < 1L || qs.getQuestItemsCount(5992) < 1L || qs.getQuestItemsCount(5993) < 1L || qs.getQuestItemsCount(5994) < 1L || qs.getQuestItemsCount(5995) < 1L || qs.getQuestItemsCount(5996) < 1L || qs.getQuestItemsCount(5997) < 1L || qs.getQuestItemsCount(5998) < 1L || qs.getQuestItemsCount(5999) < 1L || qs.getQuestItemsCount(6000) < 1L || qs.getQuestItemsCount(6001) < 1L) {
                    string2 = "whouse_keeper_walderal_q0372_07e.htm";
                } else if (qs.getQuestItemsCount(5989) >= 1L && qs.getQuestItemsCount(5990) >= 1L && qs.getQuestItemsCount(5991) >= 1L && qs.getQuestItemsCount(5992) >= 1L && qs.getQuestItemsCount(5993) >= 1L && qs.getQuestItemsCount(5994) >= 1L && qs.getQuestItemsCount(5995) >= 1L && qs.getQuestItemsCount(5996) >= 1L && qs.getQuestItemsCount(5997) >= 1L && qs.getQuestItemsCount(5998) >= 1L && qs.getQuestItemsCount(5999) >= 1L && qs.getQuestItemsCount(6000) >= 1L && qs.getQuestItemsCount(6001) >= 1L) {
                    qs.takeItems(5989, 1L);
                    qs.takeItems(5990, 1L);
                    qs.takeItems(5991, 1L);
                    qs.takeItems(5992, 1L);
                    qs.takeItems(5993, 1L);
                    qs.takeItems(5994, 1L);
                    qs.takeItems(5995, 1L);
                    qs.takeItems(5996, 1L);
                    qs.takeItems(5997, 1L);
                    qs.takeItems(5998, 1L);
                    qs.takeItems(5999, 1L);
                    qs.takeItems(6000, 1L);
                    qs.takeItems(6001, 1L);
                    int n4 = ThreadLocalRandom.current().nextInt(100);
                    if (n4 < 17) {
                        qs.giveItems(5502, 1L);
                    } else if (n4 < 34) {
                        qs.giveItems(5514, 1L);
                    } else if (n4 < 49) {
                        qs.giveItems(5527, 1L);
                    } else if (n4 < 58) {
                        qs.giveItems(5502, 1L);
                        qs.giveItems(5514, 1L);
                        qs.giveItems(5527, 1L);
                    } else if (n4 < 70) {
                        qs.giveItems(5380, 1L);
                    } else if (n4 < 82) {
                        qs.giveItems(5404, 1L);
                    } else if (n4 < 92) {
                        qs.giveItems(5430, 1L);
                    } else if (n4 < 100) {
                        qs.giveItems(5380, 1L);
                        qs.giveItems(5404, 1L);
                        qs.giveItems(5430, 1L);
                    }
                    string2 = "whouse_keeper_walderal_q0372_07c.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=10")) {
                if (qs.getQuestItemsCount(5989) < 1L || qs.getQuestItemsCount(5990) < 1L || qs.getQuestItemsCount(5991) < 1L || qs.getQuestItemsCount(5992) < 1L || qs.getQuestItemsCount(5993) < 1L || qs.getQuestItemsCount(5994) < 1L || qs.getQuestItemsCount(5995) < 1L || qs.getQuestItemsCount(5996) < 1L || qs.getQuestItemsCount(5997) < 1L || qs.getQuestItemsCount(5998) < 1L || qs.getQuestItemsCount(5999) < 1L || qs.getQuestItemsCount(6000) < 1L || qs.getQuestItemsCount(6001) < 1L) {
                    string2 = "whouse_keeper_walderal_q0372_07e.htm";
                } else if (qs.getQuestItemsCount(5989) >= 1L && qs.getQuestItemsCount(5990) >= 1L && qs.getQuestItemsCount(5991) >= 1L && qs.getQuestItemsCount(5992) >= 1L && qs.getQuestItemsCount(5993) >= 1L && qs.getQuestItemsCount(5994) >= 1L && qs.getQuestItemsCount(5995) >= 1L && qs.getQuestItemsCount(5996) >= 1L && qs.getQuestItemsCount(5997) >= 1L && qs.getQuestItemsCount(5998) >= 1L && qs.getQuestItemsCount(5999) >= 1L && qs.getQuestItemsCount(6000) >= 1L && qs.getQuestItemsCount(6001) >= 1L) {
                    qs.takeItems(5989, 1L);
                    qs.takeItems(5990, 1L);
                    qs.takeItems(5991, 1L);
                    qs.takeItems(5992, 1L);
                    qs.takeItems(5993, 1L);
                    qs.takeItems(5994, 1L);
                    qs.takeItems(5995, 1L);
                    qs.takeItems(5996, 1L);
                    qs.takeItems(5997, 1L);
                    qs.takeItems(5998, 1L);
                    qs.takeItems(5999, 1L);
                    qs.takeItems(6000, 1L);
                    qs.takeItems(6001, 1L);
                    int n5 = ThreadLocalRandom.current().nextInt(100);
                    if (n5 < 17) {
                        qs.giveItems(5503, 1L);
                    } else if (n5 < 34) {
                        qs.giveItems(5515, 1L);
                    } else if (n5 < 49) {
                        qs.giveItems(5528, 1L);
                    } else if (n5 < 58) {
                        qs.giveItems(5503, 1L);
                        qs.giveItems(5515, 1L);
                        qs.giveItems(5528, 1L);
                    } else if (n5 < 70) {
                        qs.giveItems(5382, 1L);
                    } else if (n5 < 82) {
                        qs.giveItems(5406, 1L);
                    } else if (n5 < 92) {
                        qs.giveItems(5432, 1L);
                    } else if (n5 < 100) {
                        qs.giveItems(5382, 1L);
                        qs.giveItems(5406, 1L);
                        qs.giveItems(5432, 1L);
                    }
                    string2 = "whouse_keeper_walderal_q0372_07d.htm";
                }
            } else if (event.equalsIgnoreCase("menu_select?ask=372&reply=99")) {
                qs.setCond(2);
                qs.playSound(QuestState.SOUND_MIDDLE);
                string2 = "whouse_keeper_walderal_q0372_05b.htm";
            }
        }
        return string2;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        String html = "noquest";
        int n = qs.getInt("legacy_of_insolence");
        int n2 = npc != null ? npc.getNpcId() : 0;
        int n3 = qs.getStateId();
        switch (n3) {
            case 1: {
                if (n2 != 30844) break;
                if (pc.getLevel() < 59) {
                    html = "whouse_keeper_walderal_q0372_01.htm";
                    qs.exitQuest(true);
                    break;
                }
                if (pc.getLevel() < 59) break;
                html = "whouse_keeper_walderal_q0372_02.htm";
                break;
            }
            case 2: {
                if (n2 == 30844) {
                    if (n != 1) break;
                    html = "whouse_keeper_walderal_q0372_05.htm";
                    break;
                }
                if (n2 == 30839) {
                    if (qs.getQuestItemsCount(5984) < 1L || qs.getQuestItemsCount(5985) < 1L || qs.getQuestItemsCount(5986) < 1L || qs.getQuestItemsCount(5987) < 1L || qs.getQuestItemsCount(5988) < 1L) {
                        html = "trader_holly_q0372_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(5984) < 1L || qs.getQuestItemsCount(5985) < 1L || qs.getQuestItemsCount(5986) < 1L || qs.getQuestItemsCount(5987) < 1L || qs.getQuestItemsCount(5988) < 1L) break;
                    qs.takeItems(5984, 1L);
                    qs.takeItems(5985, 1L);
                    qs.takeItems(5986, 1L);
                    qs.takeItems(5987, 1L);
                    qs.takeItems(5988, 1L);
                    int n4 = ThreadLocalRandom.current().nextInt(100);
                    if (n4 < 30) {
                        qs.giveItems(5496, 1L);
                    } else if (n4 < 60) {
                        qs.giveItems(5508, 1L);
                    } else if (n4 < 80) {
                        qs.giveItems(5525, 1L);
                    } else if (n4 < 90) {
                        qs.giveItems(5496, 1L);
                        qs.giveItems(5508, 1L);
                        qs.giveItems(5525, 1L);
                    } else if (n4 < 100) {
                        qs.giveItems(57, 4000L);
                    }
                    html = "trader_holly_q0372_02.htm";
                    break;
                }
                if (n2 == 30855) {
                    if (qs.getQuestItemsCount(5972) < 1L || qs.getQuestItemsCount(5973) < 1L || qs.getQuestItemsCount(5974) < 1L || qs.getQuestItemsCount(5975) < 1L || qs.getQuestItemsCount(5976) < 1L || qs.getQuestItemsCount(5977) < 1L || qs.getQuestItemsCount(5978) < 1L) {
                        html = "magister_desmond_q0372_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(5972) < 1L || qs.getQuestItemsCount(5973) < 1L || qs.getQuestItemsCount(5974) < 1L || qs.getQuestItemsCount(5975) < 1L || qs.getQuestItemsCount(5976) < 1L || qs.getQuestItemsCount(5977) < 1L || qs.getQuestItemsCount(5978) < 1L) break;
                    qs.takeItems(5972, 1L);
                    qs.takeItems(5973, 1L);
                    qs.takeItems(5974, 1L);
                    qs.takeItems(5975, 1L);
                    qs.takeItems(5976, 1L);
                    qs.takeItems(5977, 1L);
                    qs.takeItems(5978, 1L);
                    int n5 = ThreadLocalRandom.current().nextInt(100);
                    if (n5 < 31) {
                        qs.giveItems(5503, 1L);
                    } else if (n5 < 62) {
                        qs.giveItems(5515, 1L);
                    } else if (n5 < 75) {
                        qs.giveItems(5528, 1L);
                    } else if (n5 < 83) {
                        qs.giveItems(5503, 1L);
                        qs.giveItems(5515, 1L);
                        qs.giveItems(5528, 1L);
                    } else if (n5 < 100) {
                        qs.giveItems(57, 4000L);
                    }
                    html = "magister_desmond_q0372_02.htm";
                    break;
                }
                if (n2 == 30929) {
                    if (qs.getQuestItemsCount(5979) < 1L || qs.getQuestItemsCount(5980) < 1L || qs.getQuestItemsCount(5981) < 1L || qs.getQuestItemsCount(5982) < 1L || qs.getQuestItemsCount(5983) < 1L) {
                        html = "patrin_q0372_01.htm";
                        break;
                    }
                    if (qs.getQuestItemsCount(5979) < 1L || qs.getQuestItemsCount(5980) < 1L || qs.getQuestItemsCount(5981) < 1L || qs.getQuestItemsCount(5982) < 1L || qs.getQuestItemsCount(5983) < 1L) break;
                    qs.takeItems(5979, 1L);
                    qs.takeItems(5980, 1L);
                    qs.takeItems(5981, 1L);
                    qs.takeItems(5982, 1L);
                    qs.takeItems(5983, 1L);
                    int n6 = ThreadLocalRandom.current().nextInt(100);
                    if (n6 < 30) {
                        qs.giveItems(5497, 1L);
                    } else if (n6 < 60) {
                        qs.giveItems(5509, 1L);
                    } else if (n6 < 80) {
                        qs.giveItems(5526, 1L);
                    } else if (n6 < 90) {
                        qs.giveItems(5497, 1L);
                        qs.giveItems(5509, 1L);
                        qs.giveItems(5526, 1L);
                    } else if (n6 < 100) {
                        qs.giveItems(57, 4000L);
                    }
                    html = "patrin_q0372_02.htm";
                    break;
                }
                if (n2 != 31001) break;
                if (qs.getQuestItemsCount(5972) < 1L || qs.getQuestItemsCount(5973) < 1L || qs.getQuestItemsCount(5974) < 1L || qs.getQuestItemsCount(5975) < 1L || qs.getQuestItemsCount(5976) < 1L || qs.getQuestItemsCount(5977) < 1L || qs.getQuestItemsCount(5978) < 1L) {
                    html = "claudia_a_q0372_01.htm";
                    break;
                }
                if (qs.getQuestItemsCount(5972) < 1L || qs.getQuestItemsCount(5973) < 1L || qs.getQuestItemsCount(5974) < 1L || qs.getQuestItemsCount(5975) < 1L || qs.getQuestItemsCount(5976) < 1L || qs.getQuestItemsCount(5977) < 1L || qs.getQuestItemsCount(5978) < 1L) break;
                qs.takeItems(5972, 1L);
                qs.takeItems(5973, 1L);
                qs.takeItems(5974, 1L);
                qs.takeItems(5975, 1L);
                qs.takeItems(5976, 1L);
                qs.takeItems(5977, 1L);
                qs.takeItems(5978, 1L);
                int n7 = ThreadLocalRandom.current().nextInt(100);
                if (n7 < 31) {
                    qs.giveItems(5502, 1L);
                } else if (n7 < 62) {
                    qs.giveItems(5514, 1L);
                } else if (n7 < 75) {
                    qs.giveItems(5527, 1L);
                } else if (n7 < 83) {
                    qs.giveItems(5502, 1L);
                    qs.giveItems(5514, 1L);
                    qs.giveItems(5527, 1L);
                } else if (n7 < 100) {
                    qs.giveItems(57, 4000L);
                }
                html = "claudia_a_q0372_02.htm";
            }
        }
        return html;
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        int n = npc != null ? npc.getNpcId() : 0;
        if (n == 20817) {
            if (ThreadLocalRandom.current().nextInt(1000) < 302) {
                qs.rollAndGive(5966, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20821) {
            if (ThreadLocalRandom.current().nextInt(100) < 41) {
                qs.rollAndGive(5966, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20825) {
            if (ThreadLocalRandom.current().nextInt(1000) < 447) {
                qs.rollAndGive(5966, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 20829) {
            if (ThreadLocalRandom.current().nextInt(1000) < 451) {
                qs.rollAndGive(5967, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 21069) {
            if (ThreadLocalRandom.current().nextInt(100) < 28) {
                qs.rollAndGive(5968, 1, 100.0);
                qs.playSound(QuestState.SOUND_ITEMGET);
            }
        } else if (n == 21063 && ThreadLocalRandom.current().nextInt(100) < 29) {
            qs.rollAndGive(5969, 1, 100.0);
            qs.playSound(QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

}
