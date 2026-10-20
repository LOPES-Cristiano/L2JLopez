package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 7000_HeroItems: Monument of Heroes permitindo escolher e retirar 1 Arma Heroica (6611..6621).
 */
@Component
public class Quest7000HeroItems extends Quest {

	public static final int[] MONUMENTS = { 31690, 31769, 31770, 31771, 31772 };

	public record HeroItemEntry(String icon, String name, String descr, String patkMatk, String type) {}

	public static final Map<Integer, HeroItemEntry> HERO_ITEMS = new LinkedHashMap<>();

	static {
		HERO_ITEMS.put(6611, new HeroItemEntry("weapon_the_sword_of_hero_i00", "Infinity Blade", "During critical attack, decreases P.Def, increases debuff casting, damage shield, Max HP/MP/CP. Enhances PvP damage.", "297/137", "Sword"));
		HERO_ITEMS.put(6612, new HeroItemEntry("weapon_the_two_handed_sword_of_hero_i00", "Infinity Cleaver", "Increases Max HP/CP, critical power and chance. Inflicts extra damage and can reflect skills. Enhances PvP damage.", "361/137", "Two-Handed Sword"));
		HERO_ITEMS.put(6613, new HeroItemEntry("weapon_the_axe_of_hero_i00", "Infinity Axe", "Causes internal conflict on crit. Increases shield defense, Max HP/MP/CP. Enhances PvP damage.", "297/137", "Blunt"));
		HERO_ITEMS.put(6614, new HeroItemEntry("weapon_the_mace_of_hero_i00", "Infinity Rod", "When good magic is cast, increases Max MP/CP, Casting Spd, and recovers 100% HP. Enhances PvP damage.", "238/182", "Blunt"));
		HERO_ITEMS.put(6615, new HeroItemEntry("weapon_the_hammer_of_hero_i00", "Infinity Crusher", "Increases Max HP/CP, Atk Spd. Stuns on crit and reflects skills. Enhances PvP damage.", "361/137", "Blunt"));
		HERO_ITEMS.put(6616, new HeroItemEntry("weapon_the_staff_of_hero_i00", "Infinity Scepter", "Recovers 100% HP at certain rate when casting, increases M.Atk, Magic Critical. Enhances PvP damage.", "290/182", "Blunt"));
		HERO_ITEMS.put(6617, new HeroItemEntry("weapon_the_dagger_of_hero_i00", "Infinity Stinger", "Increases Max MP/CP, Atk Spd, Mortal Blow success. Silences target on crit. Enhances PvP damage.", "260/137", "Dagger"));
		HERO_ITEMS.put(6618, new HeroItemEntry("weapon_the_fist_of_hero_i00", "Infinity Fang", "Increases Max HP/MP/CP, Evasion. Stuns target on crit. Enhances PvP damage.", "361/137", "Dual Fist"));
		HERO_ITEMS.put(6619, new HeroItemEntry("weapon_the_bow_of_hero_i00", "Infinity Bow", "Decreases bow reuse delay. Slows target on crit. Enhances PvP damage.", "614/137", "Bow"));
		HERO_ITEMS.put(6620, new HeroItemEntry("weapon_the_dualsword_of_hero_i00", "Infinity Wing", "Increases Max HP/MP/CP, crit rate. Silences target on crit. Enhances PvP damage.", "361/137", "Dual Sword"));
		HERO_ITEMS.put(6621, new HeroItemEntry("weapon_the_pole_of_hero_i00", "Infinity Spear", "Increases Max HP/CP, Atk Spd, Accuracy. Dispels target on crit. Enhances PvP damage.", "297/137", "Pole"));
	}

	@Autowired
	public Quest7000HeroItems(@Autowired(required = false) QuestManager questManager) {
		super(7000, "7000_HeroItems", "Hero Items");
		for (int npcId : MONUMENTS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	private String renderList() {
		StringBuilder sb = new StringBuilder("<html><body><font color=\"LEVEL\">List of Hero Items:</font><table border=0 width=300>");
		for (var entry : HERO_ITEMS.entrySet()) {
			sb.append("<tr><td width=35 height=45><img src=icon.").append(entry.getValue().icon())
			  .append(" width=32 height=32 align=left></td><td valign=top><a action=\"bypass -h Quest 7000_HeroItems ")
			  .append(entry.getKey()).append("\"><font color=\"FFFFFF\">").append(entry.getValue().name())
			  .append("</font></a></td></tr>");
		}
		sb.append("</table></body></html>");
		return sb.toString();
	}

	private String renderItem(int itemId) {
		HeroItemEntry item = HERO_ITEMS.get(itemId);
		if (item == null) {
			return renderList();
		}
		return "<html><body><table border=0 width=300><tr><td><font color=\"LEVEL\">Item Information</font></td>"
				+ "<td align=right><button value=Back action=\"bypass -h Quest 7000_HeroItems buy\" width=75 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal></td></tr></table>"
				+ "<br><table border=0><tr><td width=35><img src=icon." + item.icon() + " width=32 height=32></td>"
				+ "<td><font color=\"FFFFFF\">" + item.name() + "</font><br><font color=\"B09878\">Type: " + item.type() + " | P.Atk/M.Atk: " + item.patkMatk() + "</font></td></tr></table>"
				+ "<br><font color=\"B09878\">" + item.descr() + "</font><br><br>"
				+ "<button value=Obtain action=\"bypass -h Quest 7000_HeroItems _" + itemId + "\" width=75 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal>"
				+ "</body></html>";
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		PlayerCharacter player = qs != null ? qs.playerChar() : null;
		if (player != null && player.isHero()) {
			return renderList();
		}
		int objId = npc != null ? npc.objectId() : 0;
		if (qs != null) {
			qs.exitQuest(true);
		}
		return "<html><body>Monument of Heroes:<br>You do not meet the requirements. You must become a Hero first!<br><a action=\"bypass -h npc_"
				+ objId + "_Chat 0\">Back</a></body></html>";
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession player) {
		if (event == null || player == null || player.activeChar() == null) {
			return null;
		}
		QuestState qs = checkQuestState(player);
		if (qs == null) {
			qs = newQuestState(player);
		}
		if (!player.activeChar().isHero()) {
			qs.exitQuest(true);
			return "You do not meet the requirements.";
		}

		if ("buy".equalsIgnoreCase(event)) {
			return renderList();
		}
		if (event.chars().allMatch(Character::isDigit)) {
			int itemId = Integer.parseInt(event);
			return renderItem(itemId);
		}
		if (event.startsWith("_")) {
			try {
				int itemId = Integer.parseInt(event.substring(1));
				for (int id = 6611; id <= 6621; id++) {
					if (qs.getQuestItemsCount(id) > 0) {
						qs.exitQuest(true);
						return "You already have an " + (HERO_ITEMS.containsKey(id) ? HERO_ITEMS.get(id).name() : "Hero weapon");
					}
				}
				qs.giveItems(itemId, 1);
				qs.playSound(QuestState.SOUND_FANFARE);
				qs.exitQuest(true);
				return "Enjoy your " + (HERO_ITEMS.containsKey(itemId) ? HERO_ITEMS.get(itemId).name() : "Hero Weapon");
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
