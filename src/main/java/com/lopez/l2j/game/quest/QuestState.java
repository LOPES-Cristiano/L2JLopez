package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado individual de uma quest em execucao ou completada para um jogador.
 */
public class QuestState {

	public static final String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static final String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static final String SOUND_FINISH = "ItemSound.quest_finish";
	public static final String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static final String SOUND_FANFARE = "ItemSound.quest_fanfare_2";

	private final Quest quest;
	private GameSession player;
	private State state;
	private final Map<String, String> variables = new ConcurrentHashMap<>();

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

	public void set(String var, int val) {
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

	public int getCond() {
		return getInt("cond");
	}

	public void setCond(int value) {
		set("cond", value);
		playSound(SOUND_MIDDLE);
	}

	public void exitQuest(boolean repeatable) {
		if (repeatable) {
			state = State.CREATED;
			variables.clear();
		} else {
			state = State.COMPLETED;
		}
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

	public void addExpAndSp(long exp, int sp) {
		if (player == null || player.activeChar() == null) {
			return;
		}
		PlayerCharacter c = player.activeChar();
		c.exp(c.exp() + exp);
		c.sp(c.sp() + sp);
		player.send(new StatusUpdate(c.objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.EXP, (int) c.exp()),
				new StatusUpdate.Attribute(StatusUpdate.SP, c.sp()))));
	}

	public void playSound(String sound) {
		if (player != null && sound != null) {
			player.send(new PlaySound(sound));
		}
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
}
