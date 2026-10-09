package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estado individual de uma quest em execucao ou completada para um jogador.
 */
public class QuestState {

	private static final Logger log = LoggerFactory.getLogger(QuestState.class);
	private static final ScheduledExecutorService QUEST_TIMER_POOL =
			Executors.newScheduledThreadPool(4, Thread.ofVirtual().name("QuestTimer-", 0).factory());

	public static final String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static final String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static final String SOUND_FINISH = "ItemSound.quest_finish";
	public static final String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static final String SOUND_FANFARE = "ItemSound.quest_fanfare_2";
	public static final String SOUND_JACKPOT = "ItemSound.quest_jackpot";
	public static final String SOUND_HORROR2 = "SkillSound5.horror_02";
	public static final String SOUND_BEFORE_BATTLE = "Itemsound.quest_before_battle";

	private final Quest quest;
	private GameSession player;
	private State state;
	private final Map<String, String> variables = new ConcurrentHashMap<>();
	private final Map<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

	public QuestState(Quest quest, GameSession player, State state) {
		this.quest = quest;
		this.player = player;
		this.state = state;
	}

	public QuestState(Quest quest, GameSession player) {
		this(quest, player, State.CREATED);
	}

	public Quest getQuest() {
		return quest;
	}

	public String getQuestName() {
		return quest.getName();
	}

	public int getQuestId() {
		return quest.getQuestId();
	}

	public GameSession getPlayer() {
		return player;
	}

	public void setPlayer(GameSession player) {
		this.player = player;
	}

	public int getCharId() {
		return (player != null && player.activeChar() != null) ? player.activeChar().objectId() : 0;
	}

	public State getState() {
		return state;
	}

	public int getStateId() {
		if (state == State.COMPLETED) {
			return 3;
		} else if (state == State.STARTED) {
			return 2;
		}
		return 1;
	}

	public void setState(State state) {
		this.state = state;
	}

	public boolean isCreated() {
		return state == State.CREATED;
	}

	public boolean isStarted() {
		return state == State.STARTED;
	}

	public boolean isCompleted() {
		return state == State.COMPLETED;
	}

	public String get(String var) {
		return variables.get(var);
	}

	public int getInt(String var) {
		String val = variables.get(var);
		if (val == null) {
			return 0;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public void set(String var, String val) {
		if (val == null) {
			variables.remove(var);
		} else {
			variables.put(var, val);
		}
	}

	public void set(String var, String val, boolean store) {
		set(var, val);
	}

	public void set(String var, int val) {
		set(var, String.valueOf(val));
	}

	public void set(String var, int val, boolean store) {
		set(var, String.valueOf(val));
	}

	public void setInternal(String var, String val) {
		variables.put(var, val);
	}

	public void unset(String var) {
		variables.remove(var);
	}

	public Map<String, String> getVariables() {
		return Collections.unmodifiableMap(variables);
	}

	public Map<String, String> getAllVars() {
		return Collections.unmodifiableMap(variables);
	}

	public int calculateLevelDiffForDrop(int mobLevel, int playerLevel) {
		int diff = playerLevel - mobLevel;
		return Math.max(0, diff);
	}

	public double getRateQuestsAdenaReward() {
		return 1.0;
	}

	public double getRateQuestsReward() {
		return 1.0;
	}

	public double getRateQuestsDrop() {
		return 1.0;
	}

	public void dropItemDelay(com.lopez.l2j.game.npc.NpcInstance npc, int itemId, int count) {
		giveItems(itemId, count);
	}

	public QuestState getQuestState(String questName) {
		return player != null ? player.getQuestState(questName) : null;
	}

	public QuestState getQuestState(Class<?> clazz) {
		if (clazz != null) {
			String cname = clazz.getSimpleName();
			if (cname.startsWith("_")) {
				cname = cname.substring(1);
			}
			return getQuestState(cname);
		}
		return null;
	}

	public PlayerCharacter getRandomPartyMember(int state, double range) {
		return playerChar();
	}

	public int getCond() {
		return getInt("cond");
	}

	public void setCond(int value) {
		set("cond", value);
		playSound(SOUND_MIDDLE);
	}

	public QuestState exitQuest(boolean repeatable) {
		if (repeatable) {
			state = State.CREATED;
			variables.clear();
		} else {
			state = State.COMPLETED;
		}
		cancelQuestTimers();
		return this;
	}

	public QuestState exitCurrentQuest(boolean repeatable) {
		return exitQuest(repeatable);
	}

	public long getQuestItemsCount(int itemId) {
		if (player == null || player.activeChar() == null) {
			return 0;
		}
		var inv = player.activeChar().inventory();
		if (inv == null) {
			return 0;
		}
		return inv.byItemId(itemId).map(i -> (long) i.count()).orElse(0L);
	}

	public boolean hasQuestItems(int itemId) {
		return getQuestItemsCount(itemId) > 0;
	}

	public long count(int itemId) {
		return getQuestItemsCount(itemId);
	}

	public void giveItems(int itemId, int count) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		var invSvc = player.context() != null ? player.context().inventories() : null;
		var inv = player.activeChar().inventory();
		if (inv != null) {
			if (invSvc != null) {
				invSvc.addItem(inv, itemId, count, "Quest");
			}
			var opt = inv.byItemId(itemId);
			if (opt.isPresent()) {
				opt.get().count(opt.get().count() + count);
			} else {
				ItemTemplate dummy = ItemTemplate.etc(itemId, itemId, "QuestItem_" + itemId, "quest", "asset", 1, "none", 0, false, false, false, false);
				inv.add(new ItemInstance(0x40000000 + (itemId & 0xFFFF), dummy, player.activeChar().objectId(), count));
			}
			player.send(ItemList.of(inv.items(), false));
			if (count > 1) {
				player.send(SystemMessage.of(SystemMessage.EARNED_S2_S1_S, new SystemMessage.Number(count), new SystemMessage.ItemName(itemId)));
			} else {
				player.send(SystemMessage.of(SystemMessage.EARNED_S1, new SystemMessage.ItemName(itemId)));
			}
		}
	}

	public void giveItems(int itemId, long count) {
		giveItems(itemId, (int) Math.min(Integer.MAX_VALUE, count));
	}

	public void rewardItems(int itemId, int count) {
		giveItems(itemId, count);
	}

	public PlayerCharacter getPlayerCharacter() {
		return player != null ? player.activeChar() : null;
	}

	public PlayerCharacter playerChar() {
		return player != null ? player.activeChar() : null;
	}

	public int playerClassId() {
		return playerChar() != null ? playerChar().classId() : -1;
	}

	public int playerLevel() {
		return playerChar() != null ? playerChar().level() : 0;
	}

	public void takeItems(int itemId, int count) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		var invSvc = player.context() != null ? player.context().inventories() : null;
		var inv = player.activeChar().inventory();
		if (inv != null) {
			if (invSvc != null) {
				if (count < 0) {
					long current = getQuestItemsCount(itemId);
					if (current > 0) {
						invSvc.consumeItem(inv, itemId, (int) current, "Quest");
					}
				} else {
					invSvc.consumeItem(inv, itemId, count, "Quest");
				}
			}
			var opt = inv.byItemId(itemId);
			if (opt.isPresent()) {
				var item = opt.get();
				if (count < 0 || count >= item.count()) {
					inv.remove(item);
				} else {
					item.count(item.count() - count);
				}
			}
			player.send(ItemList.of(inv.items(), false));
		}
	}

	public void takeItems(int itemId, long count) {
		takeItems(itemId, (int) Math.min(Integer.MAX_VALUE, count));
	}

	public void takeAllItems(int... itemIds) {
		if (itemIds != null) {
			for (int id : itemIds) {
				takeItems(id, -1);
			}
		}
	}

	public boolean rollAndGive(int itemId, int count, double chance) {
		if (chance <= 0) return false;
		if (chance >= 100.0 || java.util.concurrent.ThreadLocalRandom.current().nextDouble(100.0) < chance) {
			giveItems(itemId, count);
			playSound(SOUND_ITEMGET);
			return true;
		}
		return false;
	}

	public boolean rollAndGive(int itemId, int count, int countMax, int limit, double chance) {
		if (chance <= 0) return false;
		long current = getQuestItemsCount(itemId);
		if (limit > 0 && current >= limit) return false;
		if (chance >= 100.0 || java.util.concurrent.ThreadLocalRandom.current().nextDouble(100.0) < chance) {
			int add = countMax > count ? java.util.concurrent.ThreadLocalRandom.current().nextInt(count, countMax + 1) : count;
			if (limit > 0 && current + add > limit) {
				add = (int) (limit - current);
			}
			if (add > 0) {
				giveItems(itemId, add);
				playSound(SOUND_ITEMGET);
				return true;
			}
		}
		return false;
	}

	public void addExpAndSp(long exp, int sp) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		PlayerCharacter c = player.activeChar();
		c.addExpAndSp(exp, sp);
		player.send(new StatusUpdate(c.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.EXP, (int) c.exp()),
				new StatusUpdate.Attribute(StatusUpdate.SP, c.sp()))));
	}

	public void addExpAndSp(long exp, long sp) {
		addExpAndSp(exp, (int) sp);
	}

	public void playSound(String sound) {
		if (player != null && sound != null) {
			player.send(new PlaySound(sound));
		}
	}

	public NpcInstance addSpawn(int npcId) {
		if (player != null && player.activeChar() != null) {
			return addSpawn(npcId, player.activeChar().x(), player.activeChar().y(), player.activeChar().z(), 0);
		}
		return null;
	}

	public NpcInstance addSpawn(int npcId, int x, int y, int z, int despawnDelay) {
		return null;
	}

	public NpcInstance addSpawn(int npcId, int x, int y, int z) {
		return addSpawn(npcId, x, y, z, 0);
	}

	public void playTutorialVoice(String voice) {
		playSound(voice);
	}

	public void showQuestionMark(int number) {
		if (player != null) {
			player.send(new TutorialShowQuestionMark(number));
		}
	}

	public String loadTutorialHtml(String path) {
		if (player != null && player.context().htmls() != null && path != null) {
			String raw = player.context().htmls().getHtml(path);
			if (raw == null || raw.isBlank()) {
				raw = player.context().htmls().getIndexedHtml(path);
			}
			if (raw == null || raw.isBlank()) {
				raw = player.context().htmls().getHtml("quests/255_Tutorial/" + path);
			}
			if (raw == null || raw.isBlank()) {
				raw = player.context().htmls().getHtml("data/scripts/quests/255_Tutorial/" + path);
			}
			return raw;
		}
		return null;
	}

	public void showTutorialHtml(String html) {
		if (player == null || html == null) {
			return;
		}
		String text = html;
		if (html.endsWith(".htm") || html.endsWith(".html") || !html.trim().startsWith("<")) {
			String loaded = loadTutorialHtml(html);
			if (loaded != null && !loaded.isBlank()) {
				text = loaded;
			} else if (!text.trim().startsWith("<")) {
				text = "<html><body>" + text + "</body></html>";
			}
		}
		player.send(new TutorialShowHtml(text));
	}

	public void closeTutorialHtml() {
		if (player != null) {
			player.send(new TutorialCloseHtml());
		}
	}

	public void addRadar(int x, int y, int z) {
		if (player != null) {
			player.send(new RadarControl(0, 1, x, y, z));
		}
	}

	public void removeRadar(int x, int y, int z) {
		if (player != null) {
			player.send(new RadarControl(1, 1, x, y, z));
		}
	}

	public void onTutorialClientEvent(int eventId) {
		if (player != null) {
			player.send(new TutorialEnableClientEvent(eventId));
		}
	}

	public void startQuestTimer(String name, long timeMillis) {
		cancelQuestTimer(name);
		ScheduledFuture<?> future = QUEST_TIMER_POOL.schedule(() -> {
			try {
				if (player != null && player.activeChar() != null) {
					quest.notifyEvent(name, null, player);
				}
			} catch (Exception e) {
				log.error("Erro executando quest timer {} na quest {}", name, quest.getName(), e);
			} finally {
				activeTimers.remove(name);
			}
		}, timeMillis, TimeUnit.MILLISECONDS);
		activeTimers.put(name, future);
	}

	public void startQuestTimer(String name, long timeMillis, NpcInstance npc) {
		cancelQuestTimer(name);
		ScheduledFuture<?> future = QUEST_TIMER_POOL.schedule(() -> {
			try {
				if (player != null && player.activeChar() != null) {
					quest.notifyEvent(name, npc, player);
				}
			} catch (Exception e) {
				log.error("Erro executando quest timer {} na quest {}", name, quest.getName(), e);
			} finally {
				activeTimers.remove(name);
			}
		}, timeMillis, TimeUnit.MILLISECONDS);
		activeTimers.put(name, future);
	}

	public boolean isRunningQuestTimer(String name) {
		return activeTimers.containsKey(name);
	}

	public NpcInstance addSpawn(int npcId, int count) {
		return addSpawn(npcId);
	}

	public void cancelQuestTimer(String name) {
		ScheduledFuture<?> future = activeTimers.remove(name);
		if (future != null) {
			future.cancel(false);
		}
	}

	public void cancelQuestTimers() {
		for (ScheduledFuture<?> future : activeTimers.values()) {
			future.cancel(false);
		}
		activeTimers.clear();
	}

	public int getItemEquipped(int slot) {
		if (player == null || player.activeChar() == null) {
			return 0;
		}
		var inv = player.activeChar().inventory();
		if (inv == null) {
			return 0;
		}
		for (var item : inv.items()) {
			if (item.isEquipped()) {
				int bodyPart = item.template() != null ? item.template().bodyPart() : 0;
				if (slot == 7 || slot == ItemSlots.RHAND || slot == ItemSlots.LRHAND) {
					if (bodyPart == ItemSlots.SLOT_R_HAND || bodyPart == ItemSlots.SLOT_LR_HAND) {
						return item.itemId();
					}
				}
			}
		}
		return 0;
	}

	public void giveItems(int itemId, long count, boolean notify) {
		giveItems(itemId, count);
	}

	public long getQuestItemsCount(int... itemIds) {
		if (itemIds == null) return 0;
		long total = 0;
		for (int id : itemIds) {
			total += getQuestItemsCount(id);
		}
		return total;
	}

	public NpcInstance addSpawn(int npcId, int x, int y, int z, int heading, int randomOffset, int despawnDelay) {
		return addSpawn(npcId, x, y, z, despawnDelay);
	}
}
