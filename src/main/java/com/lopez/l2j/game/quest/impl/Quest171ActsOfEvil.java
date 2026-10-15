package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 171 - Acts Of Evil
 */
@Component
public class Quest171ActsOfEvil extends Quest {

	public Quest171ActsOfEvil(QuestManager questManager) {
		super(171, "171_ActsOfEvil", "Acts Of Evil");
		addStartNpc(30381);
		addTalkId(30381, 30207, 30420, 30437, 30425, 30617);
		addKillId(20062, 20064, 20438, 27190, 20496, 20497, 20498, 20499, 20066);
		registerQuestItems(4239, 4240, 4241, 4242, 4243, 4244, 4245, 4246, 4247, 4248, 4249);
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onEvent(String event, QuestState qs) {
		int cond = qs.getCond();
		if ("quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) || "30381-02.htm".equalsIgnoreCase(event) || "30381-02a.htm".equalsIgnoreCase(event)) {
			qs.setCond(1);
			qs.setState(State.STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
			return "quest_accept".equalsIgnoreCase(event) || "accept".equalsIgnoreCase(event) ? "guard_alvah_q0171_03.htm" : event;
		} else if ("30207-02.htm".equalsIgnoreCase(event)) {
			qs.setCond(2);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return "trader_arodin_q0171_01.htm";
		} else if ("30381-04.htm".equalsIgnoreCase(event)) {
			qs.setCond(5);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30381-07.htm".equalsIgnoreCase(event)) {
			qs.setCond(7);
			qs.takeItems(4245, -1);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30437-03.htm".equalsIgnoreCase(event)) {
			qs.giveItems(4247, 1);
			qs.giveItems(4248, 1);
			qs.takeItems(4245, -1);
			qs.setCond(9);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("30617-04.htm".equalsIgnoreCase(event)) {
			qs.takeItems(4246, -1);
			qs.takeItems(4247, -1);
			qs.takeItems(4248, -1);
			qs.setCond(10);
			qs.playSound(QuestState.SOUND_MIDDLE);
			return event;
		} else if ("reply_1".equalsIgnoreCase(event)) {
			if (cond == 4) {
				qs.setCond(5);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "guard_alvah_q0171_07.htm";
			} else if (cond == 8) {
				return "trader_rolento_q0171_02.htm";
			} else if (cond == 9) {
				return "turek_chief_burai_q0171_03.htm";
			}
		} else if ("reply_2".equalsIgnoreCase(event)) {
			if (cond == 6) {
				qs.setCond(7);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "guard_alvah_q0171_12.htm";
			} else if (cond == 8) {
				return "trader_rolento_q0171_03.htm";
			} else if (cond == 9) {
				return "turek_chief_burai_q0171_04.htm";
			}
		} else if ("reply_3".equalsIgnoreCase(event)) {
			if (cond == 8) {
				qs.giveItems(4247, 1);
				qs.giveItems(4248, 1);
				qs.takeItems(4245, -1);
				qs.setCond(9);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "trader_rolento_q0171_04.htm";
			} else if (cond == 9) {
				qs.takeItems(4246, -1);
				qs.takeItems(4247, -1);
				qs.takeItems(4248, -1);
				qs.setCond(10);
				qs.playSound(QuestState.SOUND_MIDDLE);
				return "turek_chief_burai_q0171_05.htm";
			}
		}
		return event;
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return "noquest";

		String html = "no-quest";
		int n = npc.getNpcId();
		int n2 = qs.getStateId();
		int n3 = qs.getCond();
		switch (n2) {
			case 1: {
				if (n != 30381) break;
				if (pc.level() < 27) {
					html = "guard_alvah_q0171_01.htm";
					qs.exitQuest(true);
					break;
				}
				html = "guard_alvah_q0171_02.htm";
				break;
			}
			case 2: {
				if (n == 30381) {
					if (n3 == 1) {
						html = "guard_alvah_q0171_04.htm";
						break;
					}
					if (n3 == 2 || n3 == 3) {
						html = "guard_alvah_q0171_05.htm";
						break;
					}
					if (n3 == 4) {
						html = "guard_alvah_q0171_06.htm";
						break;
					}
					if (n3 == 5 && (qs.getQuestItemsCount(4241) == 0 || qs.getQuestItemsCount(4242) == 0 || qs.getQuestItemsCount(4243) == 0 || qs.getQuestItemsCount(4244) == 0)) {
						html = "guard_alvah_q0171_08.htm";
						break;
					}
					if (n3 == 5 && qs.getQuestItemsCount(4241) >= 1 && qs.getQuestItemsCount(4242) >= 1 && qs.getQuestItemsCount(4243) >= 1 && qs.getQuestItemsCount(4244) >= 1) {
						qs.takeItems(4241, -1);
						qs.takeItems(4242, -1);
						qs.takeItems(4243, -1);
						qs.takeItems(4244, -1);
						qs.setCond(6);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "guard_alvah_q0171_09.htm";
						break;
					}
					if (n3 == 6 && (qs.getQuestItemsCount(4245) == 0 || qs.getQuestItemsCount(4246) == 0)) {
						html = "guard_alvah_q0171_10.htm";
						break;
					}
					if (n3 == 6 && qs.getQuestItemsCount(4245) >= 1 && qs.getQuestItemsCount(4246) >= 1) {
						html = "guard_alvah_q0171_11.htm";
						break;
					}
					if (n3 == 7) {
						html = "guard_alvah_q0171_13.htm";
						break;
					}
					if (n3 == 8) {
						html = "guard_alvah_q0171_14.htm";
						break;
					}
					if (n3 == 9) {
						html = "guard_alvah_q0171_15.htm";
						break;
					}
					if (n3 == 10) {
						html = "guard_alvah_q0171_16.htm";
						break;
					}
					if (n3 != 11) break;
					qs.giveItems(57, 95000);
					qs.playSound(QuestState.SOUND_FINISH);
					html = "guard_alvah_q0171_17.htm";
					qs.exitQuest(false);
					break;
				}
				if (n == 30420) {
					if (n3 == 2 && qs.getQuestItemsCount(4239) < 20) {
						html = "tweety_q0171_01.htm";
						break;
					}
					if (n3 == 2 && qs.getQuestItemsCount(4239) >= 20) {
						qs.giveItems(4240, 1);
						qs.takeItems(4239, -1);
						qs.setCond(3);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "tweety_q0171_02.htm";
						break;
					}
					if (n3 == 3) {
						html = "tweety_q0171_03.htm";
						break;
					}
					if (n3 < 4) break;
					html = "tweety_q0171_04.htm";
					break;
				}
				if (n == 30207) {
					if (n3 == 1) {
						qs.setCond(2);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "trader_arodin_q0171_01.htm";
						break;
					}
					if (n3 == 2 && qs.getQuestItemsCount(4239) < 20) {
						html = "trader_arodin_q0171_02.htm";
						break;
					}
					if (n3 == 2 && qs.getQuestItemsCount(4239) >= 20) {
						html = "trader_arodin_q0171_03.htm";
						break;
					}
					if (n3 == 3) {
						qs.takeItems(4240, -1);
						qs.setCond(4);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "trader_arodin_q0171_04.htm";
						break;
					}
					if (n3 < 4) break;
					html = "trader_arodin_q0171_05.htm";
					break;
				}
				if (n == 30437) {
					if (n3 == 8) {
						html = "trader_rolento_q0171_02.htm";
						break;
					}
					if (n3 == 9) {
						html = "trader_rolento_q0171_05.htm";
						break;
					}
					if (n3 < 10) break;
					html = "trader_rolento_q0171_06.htm";
					break;
				}
				if (n == 30425) {
					if (n3 == 7) {
						qs.setCond(8);
						qs.playSound(QuestState.SOUND_MIDDLE);
						html = "neti_q0171_01.htm";
						break;
					}
					if (n3 == 8) {
						html = "neti_q0171_02.htm";
						break;
					}
					if (n3 < 9) break;
					html = "neti_q0171_03.htm";
					break;
				}
				if (n != 30617) break;
				if (n3 < 9) {
					html = "turek_chief_burai_q0171_01.htm";
					break;
				}
				if (n3 == 9) {
					html = "turek_chief_burai_q0171_02.htm";
					break;
				}
				if (n3 == 10 && qs.getQuestItemsCount(4249) < 30) {
					html = "turek_chief_burai_q0171_06.htm";
					break;
				}
				if (n3 == 10 && qs.getQuestItemsCount(4249) >= 30) {
					qs.giveItems(57, 8000);
					qs.takeItems(4249, -1);
					qs.setCond(11);
					qs.playSound(QuestState.SOUND_MIDDLE);
					html = "turek_chief_burai_q0171_07.htm";
					break;
				}
				if (n3 != 11) break;
				html = "turek_chief_burai_q0171_08.htm";
			}
		}
		return html;
	}

	@Override
	public String onKill(NpcInstance npc, QuestState qs) {
		PlayerCharacter pc = qs.playerChar();
		if (pc == null) return null;

		int n = npc.getNpcId();
		int n2 = qs.getCond();
		if ((n == 20496 || n == 20497 || n == 20498 || n == 20499) && n2 == 2 && qs.getQuestItemsCount(4239) < 20) {
			int chance = (n == 20496) ? 53 : (n == 20497) ? 55 : (n == 20498) ? 51 : 50;
			if (ThreadLocalRandom.current().nextInt(100) < chance) {
				qs.rollAndGive(4239, 1, 100.0);
				if (qs.getQuestItemsCount(4239) >= 20) {
					qs.playSound(QuestState.SOUND_MIDDLE);
				}
			}
			if (qs.getQuestItemsCount(4239) == 5 || (qs.getQuestItemsCount(4239) >= 10 && ThreadLocalRandom.current().nextInt(100) <= 24)) {
				qs.addSpawn(27190, pc.getX(), pc.getY(), pc.getZ(), 200000);
			}
		} else if ((n == 20062 || n == 20064) && n2 == 5) {
			if (qs.getQuestItemsCount(4241) == 0) {
				qs.rollAndGive(4241, 1, 100.0);
			} else if (qs.getQuestItemsCount(4242) == 0 && ThreadLocalRandom.current().nextInt(100) <= 19) {
				qs.rollAndGive(4242, 1, 100.0);
			} else if (qs.getQuestItemsCount(4243) == 0 && ThreadLocalRandom.current().nextInt(100) <= 19) {
				qs.rollAndGive(4243, 1, 100.0);
			} else if (qs.getQuestItemsCount(4244) == 0 && ThreadLocalRandom.current().nextInt(100) <= 19) {
				qs.rollAndGive(4244, 1, 100.0);
			}
		} else if (n == 20438 && n2 == 6) {
			if (ThreadLocalRandom.current().nextInt(100) <= 9) {
				if (qs.getQuestItemsCount(4245) == 0) {
					qs.rollAndGive(4245, 1, 100.0);
				}
				if (qs.getQuestItemsCount(4246) == 0) {
					qs.rollAndGive(4246, 1, 100.0);
				}
			}
		} else if (n == 20066 && n2 == 10 && qs.getQuestItemsCount(4249) < 30 && ThreadLocalRandom.current().nextInt(100) <= 49) {
			qs.rollAndGive(4249, 1, 100.0);
			if (qs.getQuestItemsCount(4249) >= 30) {
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}
