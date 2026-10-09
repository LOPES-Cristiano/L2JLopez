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
 * Quest 384 - 384_WarehouseKeepersPastime
 */
@Component
public class Quest384WarehouseKeepersPastime extends Quest {

	public static final int byn = 30182;
	public static final int byo = 30685;
	public static final int byp = 5964;
	public static final Map<Integer, Integer> byq = new HashMap<Integer, Integer>();
	public static final Map<Integer, Bingo> byr = new HashMap<Integer, Bingo>();
	public static final int[][] bys = new int[][]{{16, 1888, 1}, {32, 1887, 1}, {50, 1894, 1}, {80, 952, 1}, {89, 1890, 1}, {98, 1893, 1}, {100, 951, 1}};
	public static final int[][] byt = new int[][]{{50, 883, 1}, {80, 951, 1}, {98, 852, 1}, {100, 401, 1}};
	public static final int[][] byu = new int[][]{{50, 4041, 1}, {80, 952, 1}, {98, 1892, 1}, {100, 917, 1}};
	public static final int[][] byv = new int[][]{{50, 951, 1}, {80, 500, 1}, {98, 2437, 2}, {100, 135, 1}};

	public Quest384WarehouseKeepersPastime(QuestManager questManager) {
	super(384, "384_WarehouseKeepersPastime", "384_WarehouseKeepersPastime");
		this.addStartNpc(30182);
		this.addTalkId(30685);
		byq.put(20948, 18);
		byq.put(20945, 12);
		byq.put(20946, 15);
		byq.put(20947, 16);
		byq.put(20635, 15);
		byq.put(20773, 61);
		byq.put(20774, 60);
		byq.put(20760, 24);
		byq.put(20758, 24);
		byq.put(20759, 23);
		byq.put(20242, 22);
		byq.put(20281, 22);
		byq.put(20556, 14);
		byq.put(20668, 21);
		byq.put(20241, 22);
		byq.put(20286, 22);
		byq.put(20950, 20);
		byq.put(20949, 19);
		byq.put(20942, 9);
		byq.put(20943, 12);
		byq.put(20944, 11);
		byq.put(20559, 14);
		byq.put(20243, 21);
		byq.put(20282, 21);
		byq.put(20677, 34);
		byq.put(20605, 15);
		for (int n : byq.keySet()) {
		this.addKillId(n);
		}
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

        int n = qs.getStateId();
        long l = qs.getQuestItemsCount(5964);
        if (event.equalsIgnoreCase("30182-05.htm") && n == 1) {
            qs.setCond(1);
            qs.setState(State.STARTED);
            qs.playSound(QuestState.SOUND_ACCEPT);
        } else if ((event.equalsIgnoreCase("30182-08.htm") || event.equalsIgnoreCase("30685-08.htm")) && n == 2) {
            qs.playSound(QuestState.SOUND_FINISH);
            qs.exitQuest(false);
        } else {
            if (event.contains("-game") && n == 2) {
                int n2;
                boolean bl = event.contains("-big");
                int n3 = n2 = bl ? 100 : 10;
                if (l < (long)n2) {
                    return event.replaceFirst("-big", "").replaceFirst("game", "09.htm");
                }
                qs.takeItems(5964, n2);
                int n4 = pc.objectId();
                if (byr.containsKey(n4)) {
                    byr.remove(n4);
                }
                Bingo bingo = new Bingo(bl, qs);
                byr.put(n4, bingo);
                return bingo.getDialog("");
            }
            if (event.contains("choice-") && n == 2) {
                int n5 = pc.objectId();
                if (!byr.containsKey(n5)) {
                    return null;
                }
                Bingo bingo = byr.get(n5);
                return bingo.Select(event.replaceFirst("choice-", ""));
            }
        }
        return html;
    
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

        int n = qs.getStateId();
        int n2 = npc != null ? npc.getNpcId() : 0;
        if (n == 1) {
            if (n2 != 30182) {
                return "noquest";
            }
            if (pc.getLevel() < 40) {
                qs.exitQuest(true);
                return "30182-04.htm";
            }
            qs.setCond(0);
            return "30182-01.htm";
        }
        if (n != 2) {
            return "noquest";
        }
        long l = qs.getQuestItemsCount(5964);
        if (l >= 100L) {
            return String.valueOf(n2) + "-06.htm";
        }
        if (l >= 10L) {
            return String.valueOf(n2) + "-06a.htm";
        }
        return String.valueOf(n2) + "-06b.htm";
    
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

        if (qs.getState() != State.STARTED) {
            return null;
        }
        Integer n = byq.get(npc.getNpcId());
        if (n != null && (ThreadLocalRandom.current().nextDouble(100.0) < ((double)n.intValue() * 1.0))) {
            qs.giveItems(5964, 1L);
            qs.playSound(qs.getQuestItemsCount(5964) == 10L ? QuestState.SOUND_MIDDLE : QuestState.SOUND_ITEMGET);
        }
        return null;
    
	}

public static class Bingo
    extends com.lopez.l2j.game.quest.Bingo {
        protected static final String msg_begin = "I've arranged 9 numbers on the panel. Don't peek! Ha ha ha!<br>Now give me your 10 medals. Some players run away when they realize that they don't stand a good chance of winning. Therefore, I prefer to hold the medals before the game starts. If you quit during game play, you'll forfeit your bet. Is that satisfactory?<br>Now, select your %choicenum% number.";
        protected static final String msg_0lines = "You are spectacularly unlucky! The red-colored numbers on the panel below are the ones you chose. As you can see, they didn't create even a single line. Did you know that it is harder not to create a single line than creating all 3 lines?<br>Usually, I don't give a reward when you don't create a single line, but since I'm feeling sorry for you, I'll be generous this time. Wait here.<br>.<br>.<br>.<br><br><br>Here, take this. I hope it will bring you better luck in the future.";
        protected static final String msg_3lines = "You've created 3 lines! The red colored numbers on the bingo panel below are the numbers you chose. Congratulations! As I promised, I'll give you an unclaimed item from my warehouse. Wait here.<br>.<br>.<br>.<br><br><br>Puff puff... it's very dusty. Here it is. Do you like it?";
        private static final String byw = "<a action=\"bypass -h Quest _384_WarehouseKeepersPastime choice-%n%\">%n%</a>&nbsp;&nbsp;&nbsp;&nbsp;  ";
        private final boolean byx;
        private final QuestState Zj;

        public Bingo(boolean bl, QuestState qs) {
            super(byw);
            this.byx = bl;
            this.Zj = qs;
        }

        @Override
        protected String getFinal() {
            String html = super.getFinal();
            if (this.lines == 3) {
                this.a(this.byx ? byt : bys);
            } else if (this.lines == 0) {
                this.a(this.byx ? byv : byu);
            }
            byr.remove(this.Zj.playerChar().objectId());
            return html;
        }

        private void a(int[][] nArray) {
            int n = ThreadLocalRandom.current().nextInt(100);
            for (int[] nArray2 : nArray) {
                if (n >= nArray2[0]) continue;
                this.Zj.giveItems(nArray2[1], nArray2[2], true);
                return;
            }
        }
    }

}
