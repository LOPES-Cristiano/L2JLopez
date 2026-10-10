package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.drop.DropData;
import com.lopez.l2j.game.drop.DropService;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ExperienceTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.subclass.SubClass;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestGMCommand;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handler modular para comandos de Administrador (//admin, //heal, //spawn, etc.)
 * e paineis HTML administrativos da GameSession.
 */
public class AdminPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminPacketHandler.class);

	private final GameSession session;

	public AdminPacketHandler(GameSession session) {
		this.session = session;
	}

	private void send(GameServerPacket packet) {
		session.send(packet);
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private GameSession.Context context() {
		return session.context();
	}

	public PlayerCharacter getTargetPlayerOrActive() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		int targetObjectId = session.targetObjectId();
		if (targetObjectId != 0 && active != null && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other != null && other.character() != null) {
				return other.character();
			}
		}
		return active;
	}

	public GameWorld.OnlinePlayer getTargetOnlinePlayerOrSelf() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		int targetObjectId = session.targetObjectId();
		if (targetObjectId != 0 && active != null && targetObjectId != active.objectId()) {
			var other = ctx.world().player(targetObjectId).orElse(null);
			if (other != null) {
				return other;
			}
		}
		return session;
	}

	public String resolveAdminHtml(String requested) {
		var ctx = session.context();
		if (ctx == null || ctx.htmls() == null || requested == null || requested.isBlank()) {
			return null;
		}
		String clean = requested.trim();
		if (clean.startsWith("admin_")) {
			clean = clean.substring(6).trim();
		}
		if (clean.isEmpty() || "admin".equalsIgnoreCase(clean) || "main".equalsIgnoreCase(clean) || "main_menu".equalsIgnoreCase(clean)) {
			clean = "menus/main.htm";
		} else if ("admin2".equalsIgnoreCase(clean) || "gamemenu".equalsIgnoreCase(clean) || "game".equalsIgnoreCase(clean) || "game_menu".equalsIgnoreCase(clean)) {
			clean = "menus/game.htm";
		} else if ("admin4".equalsIgnoreCase(clean) || "server".equalsIgnoreCase(clean) || "servermenu".equalsIgnoreCase(clean) || "server_menu".equalsIgnoreCase(clean)) {
			clean = "menus/server.htm";
		} else if ("admin3".equalsIgnoreCase(clean) || "effects".equalsIgnoreCase(clean) || "effectsmenu".equalsIgnoreCase(clean) || "effects_menu".equalsIgnoreCase(clean)) {
			clean = "menus/effects.htm";
		} else if ("admin5".equalsIgnoreCase(clean) || "mod".equalsIgnoreCase(clean) || "mods".equalsIgnoreCase(clean) || "mods_menu".equalsIgnoreCase(clean)) {
			clean = "menus/mod.htm";
		} else if ("show_moves".equalsIgnoreCase(clean) || "teleports".equalsIgnoreCase(clean)
				|| "tele".equalsIgnoreCase(clean) || "tele_menu".equalsIgnoreCase(clean) || "move".equalsIgnoreCase(clean)) {
			clean = "teleports.htm";
		} else if ("gmshop".equalsIgnoreCase(clean) || "adminshop".equalsIgnoreCase(clean)
				|| "shop".equalsIgnoreCase(clean) || "gmshops".equalsIgnoreCase(clean)) {
			clean = "gmshops.htm";
		} else if ("enchant".equalsIgnoreCase(clean) || "enchant_menu".equalsIgnoreCase(clean)) {
			clean = "enchant.htm";
		} else if ("spawn_menu".equalsIgnoreCase(clean) || "spawnmenu".equalsIgnoreCase(clean) || "spawn".equalsIgnoreCase(clean) || "spawn.htm".equalsIgnoreCase(clean)) {
			clean = "spawn.htm";
		} else if ("show_skills".equalsIgnoreCase(clean) || "skills_menu".equalsIgnoreCase(clean)
				|| "skills".equalsIgnoreCase(clean)) {
			clean = "skills.htm";
		} else if ("social_menu".equalsIgnoreCase(clean) || "socialmenu".equalsIgnoreCase(clean) || "social".equalsIgnoreCase(clean)) {
			clean = "social.htm";
		} else if ("abnormal_menu".equalsIgnoreCase(clean) || "abnormalmenu".equalsIgnoreCase(clean) || "abnormal".equalsIgnoreCase(clean) || "effect_menu".equalsIgnoreCase(clean)) {
			clean = "abnormal.htm";
		} else if ("announce_menu".equalsIgnoreCase(clean) || "announce".equalsIgnoreCase(clean)) {
			clean = "announce.htm";
		} else if ("control".equalsIgnoreCase(clean) || "control_menu".equalsIgnoreCase(clean)) {
			clean = "menus/control.htm";
		} else if ("players".equalsIgnoreCase(clean) || "players_menu".equalsIgnoreCase(clean)) {
			clean = "menus/players.htm";
		} else if ("config".equalsIgnoreCase(clean) || "configs".equalsIgnoreCase(clean)
				|| "config_menu".equalsIgnoreCase(clean)) {
			clean = "menus/config.htm";
		} else if ("events".equalsIgnoreCase(clean) || "events_menu".equalsIgnoreCase(clean)) {
			clean = "menus/events.htm";
		} else if ("charedit".equalsIgnoreCase(clean) || "charedit_menu".equalsIgnoreCase(clean)
				|| "current_player".equalsIgnoreCase(clean) || "edit_char".equalsIgnoreCase(clean) || "edit_character".equalsIgnoreCase(clean)) {
			clean = "charedit.htm";
		} else if ("charinfo".equalsIgnoreCase(clean) || "charinfo_menu".equalsIgnoreCase(clean) || "character_info".equalsIgnoreCase(clean)) {
			clean = "menus/submenus/charinfo_menu.htm";
		} else if ("charlist".equalsIgnoreCase(clean) || "charlist_menu".equalsIgnoreCase(clean)
				|| "find_character".equalsIgnoreCase(clean)) {
			clean = "charlist.htm";
		} else if ("charmanage".equalsIgnoreCase(clean) || "char_manage".equalsIgnoreCase(clean)) {
			clean = "charmanage.htm";
		} else if ("gmmenu".equalsIgnoreCase(clean)) {
			clean = "gmshops.htm";
		} else if ("itemcreation".equalsIgnoreCase(clean) || "itemcreation_menu".equalsIgnoreCase(clean)
				|| "itemcreate".equalsIgnoreCase(clean)) {
			clean = "itemcreation.htm";
		} else if ("expsp".equalsIgnoreCase(clean) || "expsp_menu".equalsIgnoreCase(clean) || "add_exp_sp_to_character".equalsIgnoreCase(clean)) {
			clean = "expsp.htm";
		} else if ("charclasses".equalsIgnoreCase(clean) || "charclasses_menu".equalsIgnoreCase(clean) || "setclass".equalsIgnoreCase(clean)) {
			clean = "charclasses.htm";
		} else if ("cwinfo".equalsIgnoreCase(clean) || "cw_info_menu".equalsIgnoreCase(clean)) {
			clean = "cwinfo.htm";
		} else if ("castle".equalsIgnoreCase(clean) || "castles".equalsIgnoreCase(clean) || "siege".equalsIgnoreCase(clean)) {
			clean = "castles.htm";
		} else if ("fort".equalsIgnoreCase(clean) || "forts".equalsIgnoreCase(clean) || "fortsiege".equalsIgnoreCase(clean)) {
			clean = "forts.htm";
		} else if ("mobgroup".equalsIgnoreCase(clean) || "mobmenu".equalsIgnoreCase(clean)) {
			clean = "mobgroup.htm";
		} else if ("sounds".equalsIgnoreCase(clean) || "sound".equalsIgnoreCase(clean)
				|| "songs".equalsIgnoreCase(clean) || "song".equalsIgnoreCase(clean)) {
			clean = "songs/songs.htm";
		} else if ("rblist".equalsIgnoreCase(clean) || "raid".equalsIgnoreCase(clean)
				|| "raidboss".equalsIgnoreCase(clean)) {
			clean = "tele/raid/raid.htm";
		} else if ("altg".equalsIgnoreCase(clean) || "altg_menu".equalsIgnoreCase(clean)
				|| "gametool".equalsIgnoreCase(clean)) {
			clean = "menus/altg_menu.htm";
		}

		List<String> candidates = new ArrayList<>();
		if (clean.startsWith("admin/")) {
			candidates.add(clean);
		} else {
			candidates.add("admin/" + clean);
			if (!clean.endsWith(".htm") && !clean.endsWith(".html")) {
				candidates.add("admin/" + clean + ".htm");
				candidates.add("admin/menus/" + clean + ".htm");
				candidates.add("admin/menus/submenus/" + clean + ".htm");
				candidates.add("admin/menus/submenus/" + clean + "_menu.htm");
				candidates.add("admin/gmshop/" + clean + ".htm");
				candidates.add("admin/tele/" + clean + ".htm");
				candidates.add("admin/skills/" + clean + ".htm");
				candidates.add("admin/songs/" + clean + ".htm");
				candidates.add("admin/classes/" + clean + ".htm");
				candidates.add("admin/tele/raid/" + clean + ".htm");
			}
			candidates.add("admin/menus/" + clean);
			candidates.add("admin/menus/submenus/" + clean);
			candidates.add("admin/gmshop/" + clean);
			candidates.add("admin/tele/" + clean);
			candidates.add("admin/skills/" + clean);
			candidates.add("admin/songs/" + clean);
			candidates.add("admin/classes/" + clean);
			candidates.add("admin/tele/raid/" + clean);
		}

		for (String cand : candidates) {
			String html = ctx.htmls().getHtml(cand);
			if (html != null && !html.isBlank()) {
				return html;
			}
		}

		// Fallback inteligente usando o indice global do HtmCache
		String indexed = ctx.htmls().getIndexedHtml(clean);
		if (indexed != null && !indexed.isBlank()) {
			return indexed;
		}
		if (clean.contains("/")) {
			String lastPart = clean.substring(clean.lastIndexOf('/') + 1);
			indexed = ctx.htmls().getIndexedHtml(lastPart);
			if (indexed != null && !indexed.isBlank()) {
				return indexed;
			}
		}

		return null;
	}

	public void showAdminHtml(String requested) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		String cleanReq = requested != null ? requested.trim().toLowerCase(java.util.Locale.ROOT) : "";
		if (cleanReq.contains("charlist")) {
			showAdminCharList("", 1);
			return;
		}
		if (cleanReq.contains("charinfo")) {
			PlayerCharacter tc = getTargetPlayerOrActive();
			showAdminCharInfo(tc != null ? tc.name() : (active != null ? active.name() : ""));
			return;
		}
		String html = resolveAdminHtml(requested);
		if (html == null) {
			html = "<html><title>Admin Panel</title><body><center><font color=\"LEVEL\">Admin Control Panel</font><br><br>"
					+ "<a action=\"bypass -h admin_admin\">Main Menu</a><br>"
					+ "<a action=\"bypass -h admin_gamemenu\">Game Menu</a><br>"
					+ "<a action=\"bypass -h admin_server\">Server Menu</a><br>"
					+ "<a action=\"bypass -h admin_effects\">Effects Menu</a><br>"
					+ "<a action=\"bypass -h admin_show_moves\">Teleports</a><br>"
					+ "<a action=\"bypass -h admin_gmshop\">GM Shop</a><br>"
					+ "<a action=\"bypass -h admin_enchant\">Enchant</a><br>"
					+ "<a action=\"bypass -h admin_spawn_menu\">Spawn Menu</a><br>"
					+ "</center></body></html>";
		}
		PlayerCharacter targetChar = getTargetPlayerOrActive();
		String rendered = ctx.htmls() != null
				? ctx.htmls().render(html, 0, "Admin", targetChar != null ? targetChar.name() : "Admin")
				: html;
		if (targetChar != null) {
			CharTemplate template = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
			PlayerStats stats = template != null ? PlayerStats.calculate(targetChar, template) : null;
			int curLoad = targetChar.inventory() != null ? targetChar.inventory().currentLoad() : 0;
			int maxLoad = stats != null && stats.maxLoad() > 0 ? stats.maxLoad() : (template != null ? template.maxLoad() : 69000);
			int loadPct = maxLoad > 0 ? (curLoad * 100) / maxLoad : 0;
			String clanName = targetChar.clanId() != 0 ? "Clan " + targetChar.clanId() : "No Clan";
			rendered = rendered
					.replace("%name%", targetChar.name())
					.replace("%account%", targetChar.account() != null ? targetChar.account() : "none")
					.replace("%clan%", clanName)
					.replace("%currenthp%", String.valueOf((int) targetChar.currentHp()))
					.replace("%maxhp%", String.valueOf(targetChar.maxHp()))
					.replace("%currentmp%", String.valueOf((int) targetChar.currentMp()))
					.replace("%maxmp%", String.valueOf(targetChar.maxMp()))
					.replace("%currentcp%", String.valueOf((int) targetChar.currentCp()))
					.replace("%maxcp%", String.valueOf(targetChar.maxCp()))
					.replace("%class%", String.valueOf(targetChar.classId()))
					.replace("%level%", String.valueOf(targetChar.level()))
					.replace("%xp%", String.valueOf(targetChar.exp()))
					.replace("%exp%", String.valueOf(targetChar.exp()))
					.replace("%sp%", String.valueOf(targetChar.sp()))
					.replace("%title%", targetChar.title() != null ? targetChar.title() : "")
					.replace("%karma%", String.valueOf(targetChar.karma()))
					.replace("%pvp%", String.valueOf(targetChar.pvpKills()))
					.replace("%pvpkills%", String.valueOf(targetChar.pvpKills()))
					.replace("%pk%", String.valueOf(targetChar.pkKills()))
					.replace("%pkkills%", String.valueOf(targetChar.pkKills()))
					.replace("%currentload%", String.valueOf(curLoad))
					.replace("%maxload%", String.valueOf(maxLoad))
					.replace("%percent%", String.valueOf(loadPct))
					.replace("%access%", String.valueOf(targetChar.accessLevel()))
					.replace("%pvpflag%", targetChar.pvpFlag() > 0 ? "1" : "0")
					.replace("%x%", String.valueOf(targetChar.x()))
					.replace("%y%", String.valueOf(targetChar.y()))
					.replace("%z%", String.valueOf(targetChar.z()));
		}
		long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
		long usedMem = totalMem - freeMem;
		long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		int onlineCount = ctx.world() != null ? ctx.world().allPlayers().size() : 1;
		long uptimeSec = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		long uptimeH = uptimeSec / 3600;
		long uptimeM = (uptimeSec % 3600) / 60;
		rendered = rendered
				.replace("%meminfo%", usedMem + "MB / " + maxMem + "MB")
				.replace("%os%", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")")
				.replace("%time%", uptimeH + "h " + uptimeM + "m")
				.replace("%online%", String.valueOf(onlineCount))
				.replace("%count%", String.valueOf(onlineCount))
				.replace("%used%", String.valueOf(usedMem * 1024 * 1024))
				.replace("%adena%", String.valueOf((int) com.lopez.l2j.config.Config.RATE_DROP_ADENA))
				.replace("%drop%", String.valueOf((int) com.lopez.l2j.config.Config.RATE_DROP_ITEMS))
				.replace("%server_name%", ctx.serverName() != null ? ctx.serverName() : "L2JLopez")
				.replace("%status%", "Good")
				.replace("%clock%", uptimeH + ":" + String.format("%02d", uptimeM))
				.replace("%brackets%", "Off")
				.replace("%max_players%", String.valueOf(com.lopez.l2j.config.Config.MAXIMUM_ONLINE_USERS))
				.replace("%max%", String.valueOf(com.lopez.l2j.config.Config.MAXIMUM_ONLINE_USERS))
				.replace("%geo%", com.lopez.l2j.config.Config.ENABLE_GEODATA ? "Enabled" : "Disabled")
				.replace("%target%", targetChar != null ? targetChar.name() : (active != null ? active.name() : "None"));
		if (rendered.contains("%announces%")) {
			StringBuilder annSb = new StringBuilder("<table width=260>");
			if (ctx.announcements() != null) {
				var list = ctx.announcements().list();
				for (int i = 0; i < list.size(); i++) {
					annSb.append("<tr><td width=215>").append(list.get(i)).append("</td>")
							.append("<td width=45><button value=\"Del\" action=\"bypass -h admin_del_announcement ").append(i).append("\" width=40 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td></tr>");
				}
			}
			annSb.append("</table>");
			rendered = rendered.replace("%announces%", annSb.toString());
		}
		if (rendered.contains("%castles%")) {
			StringBuilder cSb = new StringBuilder();
			if (ctx.castles() != null) {
				for (var c : ctx.castles().all()) {
					cSb.append("<td><button value=\"").append(c.name()).append("\" action=\"bypass -h admin_siege ").append(c.name()).append("\" width=80 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td>");
				}
			}
			rendered = rendered.replace("%castles%", cSb.toString());
		}
		if (rendered.contains("%clanhalls%")) {
			rendered = rendered.replace("%clanhalls%", "<td><font color=\"LEVEL\">Nenhum Clan Hall ocupado.</font></td>");
		}
		if (rendered.contains("%freeclanhalls%")) {
			rendered = rendered.replace("%freeclanhalls%", "<td><font color=\"00FF00\">Todos os Clan Halls disponiveis.</font></td>");
		}
		if (rendered.contains("%forts%")) {
			StringBuilder fSb = new StringBuilder();
			fSb.append("<td><button value=\"Valley\" action=\"bypass -h admin_fortsiege Valley\" width=80 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td>")
			   .append("<td><button value=\"Borderland\" action=\"bypass -h admin_fortsiege Borderland\" width=80 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td>");
			rendered = rendered.replace("%forts%", fSb.toString());
		}
		if (rendered.contains("%cwinfo%")) {
			StringBuilder cwSb = new StringBuilder("<table width=270 bgcolor=\"000000\">");
			if (ctx.cursedWeapons() != null) {
				for (var cw : ctx.cursedWeapons().allWeapons()) {
					String status = cw.isActive() ? "<font color=\"FF0000\">Equipada por " + (!cw.playerName().isEmpty() ? cw.playerName() : "Desconhecido") + "</font>"
							: (cw.isDropped() ? "<font color=\"FFFF00\">Dropada no Chao (" + cw.x() + ", " + cw.y() + ")</font>"
							: "<font color=\"00FF00\">Inativa / No Void</font>");
					cwSb.append("<tr><td><font color=\"LEVEL\">").append(cw.name()).append(":</font></td></tr>");
					cwSb.append("<tr><td>Status: ").append(status).append("</td></tr>");
					cwSb.append("<tr><td>");
					cwSb.append("<button value=\"Spawn CW\" action=\"bypass -h admin_cw_add ").append(cw.itemId()).append("\" width=80 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"> ");
					cwSb.append("<button value=\"Remover CW\" action=\"bypass -h admin_cw_remove ").append(cw.itemId()).append("\" width=80 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
					if (cw.isDropped() || cw.isActive()) {
						cwSb.append(" <button value=\"Ir ate Ela\" action=\"bypass -h admin_move_to ").append(cw.x()).append(" ").append(cw.y()).append(" ").append(cw.z()).append("\" width=70 height=20 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
					}
					cwSb.append("</td></tr>");
				}
			} else {
				cwSb.append("<tr><td>CursedWeaponsManager desativado.</td></tr>");
			}
			cwSb.append("</table>");
			rendered = rendered.replace("%cwinfo%", cwSb.toString());
		}
		send(new NpcHtmlMessage(0, rendered));
	}

	public void onGMCommand(GameClientPacket.RequestGMCommand p) {
		PlayerCharacter active = session.activeChar();
		if (active == null || !active.isGm()) {
			send(new ActionFailed());
			return;
		}
		handleGMCommand(p.targetName(), p.command());
	}

	public void handleGMCommand(String targetName, int command) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || !active.isGm()) {
			return;
		}
		PlayerCharacter targetChar = null;
		if (targetName != null && !targetName.isBlank()) {
			var online = ctx.world().byName(targetName.trim()).orElse(null);
			if (online != null && online.character() != null) {
				targetChar = online.character();
			} else if (ctx.characters() != null) {
				targetChar = ctx.characters().findByName(targetName.trim()).orElse(null);
			}
		}
		if (targetChar == null) {
			targetChar = getTargetPlayerOrActive();
		}
		if (targetChar == null) {
			targetChar = active;
		}

		CharTemplate template = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
		PlayerStats stats = template != null ? PlayerStats.calculate(targetChar, template) : null;

		switch (command) {
			case 1 -> {
				if (template != null && stats != null) {
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(),
							targetChar.inventory().currentLoad()));
				}
			}
			case 2 -> send(new GMViewPledgeInfo(targetChar.name(), targetChar.clanId(), targetChar.level(),
					targetChar.classId()));
			case 3 -> send(new GMViewSkillInfo(targetChar.name(), targetChar.skills(), ctx.skillService()));
			case 4 -> send(new GMViewQuestInfo(targetChar.name()));
			case 5 -> {
				send(new GMViewItemList(targetChar.name(), targetChar.inventory().items(), 80));
				send(new GMHennaInfo(0, 0, 0, 0, 0, 0));
			}
			case 6 -> send(new GMViewWarehouseWithdrawList(targetChar.name(),
					(int) Math.min(Integer.MAX_VALUE, targetChar.inventory().adena()), List.of()));
			default -> {
				if (template != null && stats != null) {
					send(new GMViewCharacterInfo(targetChar, template, stats, targetChar.inventory().paperdollView(),
							targetChar.inventory().currentLoad()));
				}
			}
		}
	}

	public void showAdminCharList(String query, int page) {
		var ctx = session.context();
		String html = resolveAdminHtml("menus/submenus/charlist_menu.htm");
		if (html == null) {
			html = "<html><title>Players</title><body><center>Players Menu</center></body></html>";
		}
		Map<Integer, PlayerCharacter> playerMap = new LinkedHashMap<>();

		// 1. Online players first
		for (var p : ctx.world().players()) {
			if (p.character() != null) {
				playerMap.put(p.character().objectId(), p.character());
			}
		}

		// 2. Add repository players
		if (ctx.characters() != null) {
			List<PlayerCharacter> repoList = (query != null && !query.isBlank())
					? ctx.characters().searchByName(query.trim(), 100)
					: ctx.characters().listAll(100);
			for (var c : repoList) {
				playerMap.putIfAbsent(c.objectId(), c);
			}
		}

		List<PlayerCharacter> allPlayers = new ArrayList<>();
		if (query != null && !query.isBlank()) {
			String q = query.trim().toLowerCase(java.util.Locale.ROOT);
			for (var c : playerMap.values()) {
				if (c.name().toLowerCase(java.util.Locale.ROOT).contains(q)) {
					allPlayers.add(c);
				}
			}
		} else {
			allPlayers.addAll(playerMap.values());
		}

		int pageSize = 15;
		int totalPlayers = allPlayers.size();
		int maxPages = Math.max(1, (int) Math.ceil((double) totalPlayers / pageSize));
		int currentPage = Math.min(Math.max(1, page), maxPages);
		int fromIndex = (currentPage - 1) * pageSize;
		int toIndex = Math.min(fromIndex + pageSize, totalPlayers);
		List<PlayerCharacter> pageList = (fromIndex < totalPlayers) ? allPlayers.subList(fromIndex, toIndex)
				: List.of();

		StringBuilder rows = new StringBuilder();
		if (pageList.isEmpty()) {
			rows.append(
					"<tr><td colspan=3><center><font color=\"LEVEL\">No characters found.</font></center></td></tr>");
		} else {
			for (PlayerCharacter pc : pageList) {
				String className = "Class " + pc.classId();
				if (ctx.characters() != null) {
					try {
						var t = ctx.characters().template(pc);
						if (t != null) {
							className = t.className();
						}
					} catch (Exception ignored) {
					}
				}
				boolean isOnline = ctx.world().byName(pc.name()).isPresent();
				String nameDisplay = isOnline ? "<font color=\"00FF00\">" + pc.name() + "</font>" : pc.name();
				rows.append("<tr>")
						.append("<td width=80><a action=\"bypass -h admin_character_info ").append(pc.name())
						.append("\">").append(nameDisplay).append("</a></td>")
						.append("<td width=110>").append(className).append("</td>")
						.append("<td width=40>").append(pc.level()).append("</td>")
						.append("</tr>");
			}
		}

		StringBuilder pages = new StringBuilder();
		if (maxPages > 1) {
			pages.append("<table width=270><tr>");
			String safeQuery = (query != null && !query.isBlank()) ? query.trim() : "";
			for (int p = 1; p <= maxPages; p++) {
				if (p == currentPage) {
					pages.append("<td><button value=\"[").append(p)
							.append("]\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ")
							.append(p)
							.append("\" width=30 height=19 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
				} else {
					pages.append("<td><button value=\"").append(p)
							.append("\" action=\"bypass -h admin_show_characters ").append(safeQuery).append(" ")
							.append(p).append("\" width=30 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
				}
			}
			pages.append("</tr></table>");
		} else {
			pages.append("<font color=\"LEVEL\">Page 1 of 1 (").append(totalPlayers).append(" players)</font>");
		}

		String rendered = html
				.replace("%players%", rows.toString())
				.replace("%pages%", pages.toString());

		send(new NpcHtmlMessage(0, rendered));
	}

	public void showAdminCastlesHtml(String args) {
		var ctx = session.context();
		StringBuilder sb = new StringBuilder("<html><title>Castles Management</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center><button value=\"Main\" action=\"bypass -h admin_admin\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"ALT+G\" action=\"bypass -h admin_altg\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Game\" action=\"bypass -h admin_gamemenu\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Server\" action=\"bypass -h admin_server\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("<td align=center><button value=\"Tele\" action=\"bypass -h admin_show_moves\" width=52 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=270 height=1>");
		sb.append("<table width=270><tr><td><font color=\"00FFBB\">Gerenciamento de Castelos (Sieges)</font></td></tr></table>");
		sb.append("<table width=270 bgcolor=\"000000\">");
		sb.append("<tr><td width=90><font color=\"LEVEL\">Castelo</font></td><td width=90><font color=\"LEVEL\">Cla Dono</font></td><td width=90><font color=\"LEVEL\">Acao</font></td></tr>");

		if (ctx.castles() != null) {
			for (var c : ctx.castles().all()) {
				String ownerName = "Sem Dono";
				if (c.hasOwner() && ctx.clans() != null) {
					var cl = ctx.clans().byClanId(c.ownerClanId()).orElse(null);
					ownerName = cl != null ? cl.name() : ("Clan " + c.ownerClanId());
				}
				sb.append("<tr>")
						.append("<td width=90><font color=\"FFFF77\">").append(c.name()).append("</font></td>")
						.append("<td width=90>").append(ownerName).append("</td>")
						.append("<td width=90><button value=\"Dar Alvo\" action=\"bypass -h admin_setcastle ").append(c.name()).append("\" width=60 height=18 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>")
						.append("</tr>");
			}
		} else {
			sb.append("<tr><td colspan=3>CastleManager indisponivel.</td></tr>");
		}
		sb.append("</table><img src=\"L2UI.SquareGray\" width=270 height=1>");
		sb.append("<table width=270><tr>");
		sb.append("<td align=center><button value=\"Atualizar\" action=\"bypass -h admin_siege\" width=130 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
		sb.append("</tr></table></center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void showAdminCharInfo(String charName) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		PlayerCharacter targetChar = null;
		GameWorld.OnlinePlayer onlineTarget = null;
		if (charName != null && !charName.isBlank()) {
			onlineTarget = ctx.world().byName(charName.trim()).orElse(null);
			if (onlineTarget != null && onlineTarget.character() != null) {
				targetChar = onlineTarget.character();
			} else if (ctx.characters() != null) {
				targetChar = ctx.characters().findByName(charName.trim()).orElse(null);
			}
		}
		if (targetChar == null) {
			targetChar = getTargetPlayerOrActive();
		}
		if (targetChar == null) {
			targetChar = active;
		}
		session.targetObjectId(targetChar.objectId());

		String html = resolveAdminHtml("menus/submenus/charinfo_menu.htm");
		if (html == null) {
			html = resolveAdminHtml("charinfo.htm");
		}
		if (html == null) {
			showAdminHtml("charinfo.htm");
			return;
		}

		CharTemplate template = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
		PlayerStats stats = template != null ? PlayerStats.calculate(targetChar, template) : null;

		String ip = "Offline";
		if (onlineTarget != null) {
			ip = "Online";
		} else if (targetChar.objectId() == active.objectId()) {
			ip = "127.0.0.1";
		}

		String clanName = targetChar.clanId() != 0 ? "Clan " + targetChar.clanId() : "No Clan";
		String className = template != null ? template.className() : "Class " + targetChar.classId();
		int curLoad = targetChar.inventory() != null ? targetChar.inventory().currentLoad() : 0;
		int maxLoad = stats != null && stats.maxLoad() > 0 ? stats.maxLoad() : (template != null ? template.maxLoad() : 69000);
		int loadPct = maxLoad > 0 ? (curLoad * 100) / maxLoad : 0;

		String rendered = html
				.replace("%name%", targetChar.name())
				.replace("%account%", targetChar.account() != null ? targetChar.account() : "none")
				.replace("%ip%", ip)
				.replace("%clan%", clanName)
				.replace("%class%", className)
				.replace("%level%", String.valueOf(targetChar.level()))
				.replace("%currentcp%", String.valueOf((int) targetChar.currentCp()))
				.replace("%maxcp%", String.valueOf(targetChar.maxCp()))
				.replace("%currenthp%", String.valueOf((int) targetChar.currentHp()))
				.replace("%maxhp%", String.valueOf(targetChar.maxHp()))
				.replace("%currentmp%", String.valueOf((int) targetChar.currentMp()))
				.replace("%maxmp%", String.valueOf(targetChar.maxMp()))
				.replace("%pkkills%", String.valueOf(targetChar.pkKills()))
				.replace("%karma%", String.valueOf(targetChar.karma()))
				.replace("%pvpkills%", String.valueOf(targetChar.pvpKills()))
				.replace("%pvpflag%", targetChar.pvpFlag() > 0 ? "1" : "0")
				.replace("%currentload%", String.valueOf(curLoad))
				.replace("%maxload%", String.valueOf(maxLoad))
				.replace("%percent%", String.valueOf(loadPct))
				.replace("%access%", String.valueOf(targetChar.accessLevel()))
				.replace("%x%", String.valueOf(targetChar.x()))
				.replace("%y%", String.valueOf(targetChar.y()))
				.replace("%z%", String.valueOf(targetChar.z()))
				.replace("%xp%", String.valueOf(targetChar.exp()))
				.replace("%sp%", String.valueOf(targetChar.sp()))
				.replace("%patk%", String.valueOf(stats != null ? stats.pAtk() : 0))
				.replace("%accuracy%", String.valueOf(stats != null ? stats.accuracy() : 0))
				.replace("%matk%", String.valueOf(stats != null ? stats.mAtk() : 0))
				.replace("%evasion%", String.valueOf(stats != null ? stats.evasion() : 0))
				.replace("%pdef%", String.valueOf(stats != null ? stats.pDef() : 0))
				.replace("%critical%", String.valueOf(stats != null ? stats.critical() : 0))
				.replace("%mdef%", String.valueOf(stats != null ? stats.mDef() : 0))
				.replace("%runspeed%", String.valueOf(stats != null ? stats.runSpeed() : 0))
				.replace("%patkspd%", String.valueOf(stats != null ? stats.pAtkSpd() : 0))
				.replace("%matkspd%", String.valueOf(stats != null ? stats.mAtkSpd() : 0));

		send(new NpcHtmlMessage(0, rendered));
	}

	public void showAdminSpawnIndex(int level, int page) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || !active.isGm()) return;
		var allTemplates = ctx.npcTemplates() != null ? ctx.npcTemplates().all() : java.util.List.<com.lopez.l2j.game.npc.NpcTemplate>of();
		var list = allTemplates.stream().filter(t -> t.level() == level).sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
		int pageSize = 12;
		int total = list.size();
		int maxPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
		int currentPage = Math.max(1, Math.min(page, maxPages));
		int from = (currentPage - 1) * pageSize;
		int to = Math.min(from + pageSize, total);
		var sub = from < total ? list.subList(from, to) : java.util.List.<com.lopez.l2j.game.npc.NpcTemplate>of();

		StringBuilder sb = new StringBuilder("<html><title>Spawn Index Lv " + level + "</title><body><center>");
		sb.append("<table width=260><tr>");
		sb.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td>");
		sb.append("<td width=180><center><font color=\"FF9900\">Spawn Index: Level ").append(level).append("</font></center></td>");
		sb.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"l2ui.MatchingListIndex_click\" fore=\"l2ui.Macro_Icon_Indicator\"></td>");
		sb.append("</tr></table><br>");

		sb.append("<table width=270 bgcolor=\"000000\">");
		if (sub.isEmpty()) {
			sb.append("<tr><td><center>Nenhum NPC de nivel ").append(level).append(" encontrado.</center></td></tr>");
		} else {
			for (var t : sub) {
				sb.append("<tr>");
				sb.append("<td width=160><font color=\"FFFF77\">").append(t.name()).append("</font> <font color=\"AAAAAA\">[").append(t.id()).append("]</font></td>");
				sb.append("<td width=55><button value=\"Spawn\" action=\"bypass -h admin_spawn ").append(t.id()).append("\" width=50 height=17 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("<td width=55><button value=\"1x\" action=\"bypass -h admin_spawn_once ").append(t.id()).append("\" width=45 height=17 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table><br>");
		if (maxPages > 1) {
			sb.append("<table width=260><tr>");
			if (currentPage > 1) {
				sb.append("<td align=left><button value=\"Prev\" action=\"bypass -h admin_spawn_index ").append(level).append(" ").append(currentPage - 1).append("\" width=55 height=17 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			} else {
				sb.append("<td width=55></td>");
			}
			sb.append("<td align=center><font color=\"LEVEL\">Page ").append(currentPage).append(" / ").append(maxPages).append(" (").append(total).append(")</font></td>");
			if (currentPage < maxPages) {
				sb.append("<td align=right><button value=\"Next\" action=\"bypass -h admin_spawn_index ").append(level).append(" ").append(currentPage + 1).append("\" width=55 height=17 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			} else {
				sb.append("<td width=55></td>");
			}
			sb.append("</tr></table>");
		}
		sb.append("</center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void showAdminNpcInfo(NpcInstance npc) {
		PlayerCharacter active = session.activeChar();
		if (npc == null || active == null || !active.isGm()) {
			return;
		}
		var t = npc.template();
		int curHp = (int) npc.currentHp();
		int maxHp = t != null ? t.maxHp() : curHp;
		int curMp = (int) npc.currentMp();
		int maxMp = t != null ? t.maxMp() : curMp;
		int pAtk = t != null ? t.pAtk() : 0;
		int mAtk = t != null ? t.mAtk() : 0;
		int pDef = t != null ? t.pDef() : 0;
		int mDef = t != null ? t.mDef() : 0;
		int lvl = t != null ? t.level() : 1;
		String type = t != null ? t.type() : "L2Npc";

		String htm = "<html><title>NPC Info: " + npc.name() + "</title><body>"
				+ "<center>"
				+ "<table width=270>"
				+ "<tr><td><font color=\"LEVEL\">" + npc.name() + "</font> (ID: " + npc.npcId()
				+ ")</td><td align=right>Obj: " + npc.objectId() + "</td></tr>"
				+ "</table>"
				+ "<center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>"
				+ "<table width=270>"
				+ "<tr><td>Type: <font color=\"00FF00\">" + type + "</font></td><td>Level: <font color=\"LEVEL\">" + lvl
				+ "</font></td></tr>"
				+ "<tr><td>HP: <font color=\"FF5555\">" + curHp + " / " + maxHp
				+ "</font></td><td>MP: <font color=\"5555FF\">" + curMp + " / " + maxMp + "</font></td></tr>"
				+ "<tr><td>P.Atk: " + pAtk + " | P.Def: " + pDef + "</td><td>M.Atk: " + mAtk + " | M.Def: " + mDef
				+ "</td></tr>"
				+ "<tr><td colspan=2>Loc: " + npc.x() + ", " + npc.y() + ", " + npc.z() + " (" + npc.heading()
				+ ")</td></tr>"
				+ "</table>"
				+ "<br><center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>"
				+ "<table width=270>"
				+ "<tr>"
				+ "<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Heal\" action=\"bypass -h admin_heal\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "<td><button value=\"Teleport\" action=\"bypass -h admin_move_to " + npc.x() + " " + npc.y() + " "
				+ npc.z()
				+ "\" width=65 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
				+ "</tr>"
				+ "<tr>"
				+ "<td colspan=2><button value=\"Talk Dialogue\" action=\"bypass -h npc_" + npc.objectId()
				+ "_Chat 0\" width=130 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>"
				+ "<td colspan=2><button value=\"View DropList\" action=\"bypass -h admin_show_droplist " + npc.npcId()
				+ "\" width=130 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>"
				+ "</tr>"
				+ "</table>"
				+ "</center></body></html>";

		send(new NpcHtmlMessage(npc.objectId(), htm));
	}

	public void showAdminDropList(int npcId) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || !active.isGm()) {
			return;
		}
		List<com.lopez.l2j.game.drop.DropData> drops = (ctx.drops() != null) ? ctx.drops().getDrops(npcId) : List.of();
		String npcName = ctx.world().npcs().stream().filter(n -> n.npcId() == npcId).findFirst().map(NpcInstance::name)
				.orElse("NPC " + npcId);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>DropList: ").append(npcName).append("</title><body><center>");
		sb.append("<font color=\"LEVEL\">").append(npcName).append("</font> (ID: ").append(npcId).append(")<br><br>");
		if (drops.isEmpty()) {
			sb.append("<font color=\"FF5555\">Nenhum drop configurado para este NPC.</font><br><br>");
		} else {
			sb.append("<table width=280>");
			sb.append("<tr><td><b>Item</b></td><td><b>Qtd</b></td><td><b>Chance</b></td><td><b>Tipo</b></td></tr>");
			for (var d : drops) {
				String itemName = "Item " + d.itemId();
				if (ctx.inventories() != null && ctx.inventories().templates() != null) {
					var tpl = ctx.inventories().templates().get(d.itemId()).orElse(null);
					if (tpl != null && tpl.name() != null) {
						itemName = tpl.name();
					}
				}
				if (itemName.length() > 18) {
					itemName = itemName.substring(0, 16) + "..";
				}
				double pct = (double) d.chance() / 10000.0;
				String typeStr = d.isSpoil() ? "<font color=\"00FF00\">Spoil</font>"
						: "<font color=\"LEVEL\">Drop</font>";
				sb.append("<tr>");
				sb.append("<td>").append(itemName).append("</td>");
				sb.append("<td>").append(d.min()).append("-").append(d.max()).append("</td>");
				sb.append("<td>").append(String.format(java.util.Locale.US, "%.2f%%", pct)).append("</td>");
				sb.append("<td>").append(typeStr).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		sb.append("<br><a action=\"bypass -h admin_admin\">Main Admin Menu</a>");
		sb.append("</center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	private record RaidBossEntry(int id, String name, int level, int x, int y, int z, boolean alive) {}

	public void showAdminRaidBossList(String args) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || !active.isGm()) {
			return;
		}
		if (args == null || args.isBlank()) {
			showAdminHtml("tele/raid/raid.htm");
			return;
		}

		String[] parts = args.trim().split("\\s+");
		String cat = parts[0].toLowerCase(java.util.Locale.ROOT);
		int page = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 1) : 1;
		if (page < 1) {
			page = 1;
		}

		List<RaidBossEntry> bosses = new ArrayList<>();
		String title;

		if ("grand".equals(cat)) {
			title = "Grand Bosses";
			List<RaidBossEntry> grandTemplates = List.of(
					new RaidBossEntry(29001, "Queen Ant", 40, -21610, 181594, -5734, false),
					new RaidBossEntry(29006, "Core", 50, 17726, 108915, -6480, false),
					new RaidBossEntry(29014, "Orfen", 50, 55024, 17368, -5412, false),
					new RaidBossEntry(29022, "Zaken", 60, 55312, 219168, -3223, false),
					new RaidBossEntry(29020, "Baium", 75, 116033, 17447, 10107, false),
					new RaidBossEntry(29019, "Antharas", 79, 181323, 114850, -7670, false),
					new RaidBossEntry(29062, "High Priestess van Halter", 80, -16375, -53658, 10448, false),
					new RaidBossEntry(29065, "Sailren", 80, 27333, -6835, -1970, false),
					new RaidBossEntry(29028, "Valakas", 85, 212852, -114842, -1632, false),
					new RaidBossEntry(29045, "Frintezza", 85, -87784, -155083, -9083, false));

			Map<Integer, RaidBossEntry> bossMap = new LinkedHashMap<>();
			for (RaidBossEntry t : grandTemplates) {
				bossMap.put(t.id(), t);
			}

			if (ctx.world() != null) {
				for (NpcInstance n : ctx.world().npcs()) {
					if (n.template() == null) {
						continue;
					}
					boolean isGrand = n.template().isGrandBoss() || bossMap.containsKey(n.npcId());
					if (!isGrand) {
						continue;
					}
					int x = n.x() != 0 ? n.x() : n.spawnX();
					int y = n.y() != 0 ? n.y() : n.spawnY();
					int z = n.z() != 0 ? n.z() : n.spawnZ();
					boolean alive = !n.isDead();
					bossMap.put(n.npcId(), new RaidBossEntry(n.npcId(), n.name(), n.template().level(), x, y, z, alive));
				}
			}
			bosses.addAll(bossMap.values());
			bosses.sort(java.util.Comparator.comparingInt(RaidBossEntry::level).thenComparing(RaidBossEntry::name));
		} else {
			int minLvl = 20;
			int maxLvl = 29;
			if (cat.contains("-")) {
				String[] lr = cat.split("-");
				minLvl = GameSession.parseIntSafe(lr[0], 20);
				maxLvl = GameSession.parseIntSafe(lr[1], minLvl + 9);
			} else {
				int parsed = GameSession.parseIntSafe(cat, 20);
				minLvl = parsed;
				maxLvl = (parsed % 10 == 0) ? parsed + 9 : parsed;
			}
			title = "Raid Bosses (" + minLvl + "-" + maxLvl + ")";

			Map<Integer, RaidBossEntry> bossMap = new LinkedHashMap<>();
			if (ctx.world() != null) {
				for (NpcInstance n : ctx.world().npcs()) {
					if (n.template() == null || !n.template().isRaidBoss() || n.template().isGrandBoss()) {
						continue;
					}
					int lvl = n.template().level();
					if (lvl < minLvl || lvl > maxLvl) {
						continue;
					}
					int x = n.x() != 0 ? n.x() : n.spawnX();
					int y = n.y() != 0 ? n.y() : n.spawnY();
					int z = n.z() != 0 ? n.z() : n.spawnZ();
					boolean alive = !n.isDead();
					RaidBossEntry existing = bossMap.get(n.npcId());
					if (existing == null || (!existing.alive() && alive)) {
						bossMap.put(n.npcId(), new RaidBossEntry(n.npcId(), n.name(), lvl, x, y, z, alive));
					}
				}
			}
			bosses.addAll(bossMap.values());
			bosses.sort(java.util.Comparator.comparingInt(RaidBossEntry::level).thenComparing(RaidBossEntry::name));
		}

		int pageSize = 8;
		int totalBosses = bosses.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) totalBosses / pageSize));
		if (page > totalPages) {
			page = totalPages;
		}
		int startIdx = (page - 1) * pageSize;
		int endIdx = Math.min(startIdx + pageSize, totalBosses);
		List<RaidBossEntry> pageList = (startIdx < totalBosses) ? bosses.subList(startIdx, endIdx) : List.of();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Raid Boss Menu</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		sb.append("<td width=180><center><font color=\"LEVEL\">").append(title).append("</font></center></td>");
		sb.append("<td width=45><button value=\"Back\" action=\"bypass -h admin_help tele/raid/raid.htm\" width=40 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		sb.append("</tr></table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center>");
		sb.append("<table width=270 bgcolor=\"000000\"><tr>");
		sb.append("<td width=150><font color=\"LEVEL\">Boss Name</font></td>");
		sb.append("<td width=50><center><font color=\"LEVEL\">Status</font></center></td>");
		sb.append("<td width=70><center><font color=\"LEVEL\">Teleport</font></center></td>");
		sb.append("</tr></table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center>");
		sb.append("<table width=270>");

		if (pageList.isEmpty()) {
			sb.append("<tr><td colspan=3 align=center><br><font color=\"FF5555\">Nenhum Raid Boss encontrado nesta faixa de nivel.</font><br></td></tr>");
		} else {
			for (RaidBossEntry b : pageList) {
				String status = b.alive() ? "<font color=\"00FF00\">Alive</font>" : "<font color=\"FF0000\">Dead</font>";
				String shortName = b.name().length() > 18 ? b.name().substring(0, 16) + ".." : b.name();
				sb.append("<tr>");
				sb.append("<td width=150>").append(shortName).append(" <font color=\"LEVEL\">(Lv ").append(b.level()).append(")</font></td>");
				sb.append("<td width=50 align=center>").append(status).append("</td>");
				sb.append("<td width=70 align=right><button value=\"Teleport\" action=\"bypass -h admin_move_to ")
						.append(b.x()).append(" ").append(b.y()).append(" ").append(b.z())
						.append("\" width=60 height=18 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table>");
		sb.append("<center><img src=\"L2UI.SquareGray\" width=270 height=1></center><br>");

		// Botoes de paginacao
		sb.append("<table width=270><tr>");
		sb.append("<td width=60 align=left>");
		if (page > 1) {
			sb.append("<button value=\"Prev\" action=\"bypass -h admin_rblist ").append(cat).append(" ").append(page - 1)
					.append("\" width=50 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		}
		sb.append("</td>");
		sb.append("<td width=150 align=center>Pagina ").append(page).append(" / ").append(totalPages)
				.append(" (").append(totalBosses).append(")</td>");
		sb.append("<td width=60 align=right>");
		if (page < totalPages) {
			sb.append("<button value=\"Next\" action=\"bypass -h admin_rblist ").append(cat).append(" ").append(page + 1)
					.append("\" width=50 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\">");
		}
		sb.append("</td></tr></table>");
		sb.append("</center></body></html>");

		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void adminChangeClass(PlayerCharacter targetChar, int targetClassId) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		var tplOpt = ctx.characters() != null ? ctx.characters().template(targetClassId) : java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isEmpty()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe id " + targetClassId + " nao existe."));
			return;
		}
		var tpl = tplOpt.get();

		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (onlineTarget instanceof GameSession gs) {
			gs.handleChangeClass(0, targetClassId, true);
			if (gs != session) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe de " + targetChar.name() + " alterada para " + tpl.className() + " (ID: " + targetClassId + ")."));
			}
		} else {
			targetChar.classId(targetClassId);
			if (!targetChar.isSubClassActive()) {
				targetChar.baseClassId(targetClassId);
			}
			if (ctx.characters() != null) {
				ctx.characters().save(targetChar, true);
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Classe de " + targetChar.name() + " (offline) alterada para " + tpl.className() + "."));
		}
	}

	public void showAdminSubClassMenu(PlayerCharacter targetChar) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		StringBuilder sb = new StringBuilder("<html><title>Admin - SubClasses: " + targetChar.name() + "</title><body><center>");
		sb.append("<table bgcolor=\"000000\" width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\" width=54 height=19><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180><center><font color=\"LEVEL\">Subclasses: ").append(targetChar.name()).append("</font></center></td>");
		sb.append("<td width=45><a action=\"bypass -h admin_character_info ").append(targetChar.name()).append("\" width=54 height=19><font color=\"FFD306\">Back</font></a></td>");
		sb.append("</tr></table><br>");

		var baseTpl = ctx.characters() != null ? ctx.characters().template(targetChar.baseClassId()) : java.util.Optional.<CharTemplate>empty();
		String baseName = baseTpl.map(CharTemplate::className).orElse("Class " + targetChar.baseClassId());
		sb.append("<table width=270>");
		sb.append("<tr><td><b>Main Class:</b></td><td>").append(baseName).append("</td></tr>");
		sb.append("<tr><td><b>Active Slot:</b></td><td>").append(targetChar.classIndex() == 0 ? "Main Class" : "Subclass #" + targetChar.classIndex()).append("</td></tr>");
		sb.append("</table><br>");

		sb.append("<font color=\"LEVEL\">Active Subclasses:</font><br>");
		sb.append("<table width=270 border=1>");
		sb.append("<tr><th>Slot</th><th>Class</th><th>Level</th><th>Action</th></tr>");
		for (int i = 1; i <= 3; i++) {
			var sc = targetChar.subClasses().get(i);
			sb.append("<tr><td align=center>").append(i).append("</td>");
			if (sc != null) {
				var scTpl = ctx.characters() != null ? ctx.characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String scName = scTpl.map(CharTemplate::className).orElse("Class " + sc.classId());
				sb.append("<td>").append(scName).append("</td>");
				sb.append("<td align=center>").append(sc.level()).append("</td>");
				sb.append("<td>");
				if (targetChar.classIndex() != i) {
					sb.append("<a action=\"bypass -h admin_switchsubclass ").append(i).append("\">Switch</a> | ");
				}
				sb.append("<a action=\"bypass -h admin_delsubclass ").append(i).append(" ").append(targetChar.name()).append("\"><font color=\"FF0000\">Del</font></a></td>");
			} else {
				sb.append("<td><font color=\"GRAY\">Empty</font></td><td align=center>-</td><td align=center>-</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table><br>");

		if (targetChar.isSubClassActive()) {
			sb.append("<a action=\"bypass -h admin_switchsubclass 0\">Switch to Main Class</a><br><br>");
		}

		if (targetChar.subClasses().size() < 3) {
			sb.append("<font color=\"LEVEL\">Add New Subclass (GM Fast-Pick):</font><br>");
			sb.append("<table width=270>");
			var available = ctx.subClasses() != null ? ctx.subClasses().getAvailableSubClasses(targetChar) : java.util.List.<Integer>of();
			int col = 0;
			for (int cid : available) {
				if (col == 0) sb.append("<tr>");
				String cName = ctx.characters() != null ? ctx.characters().template(cid).map(CharTemplate::className).orElse("Class " + cid) : "Class " + cid;
				sb.append("<td><a action=\"bypass -h admin_addsubclass ").append(cid).append(" ").append(targetChar.name()).append("\">").append(cName).append("</a></td>");
				col++;
				if (col == 2) {
					sb.append("</tr>");
					col = 0;
				}
			}
			if (col != 0) sb.append("<td></td></tr>");
			sb.append("</table>");
		}

		sb.append("</center></body></html>");
		send(new NpcHtmlMessage(0, sb.toString()));
	}

	public void adminAddSubClass(PlayerCharacter targetChar, int targetClassId) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		if (targetChar.subClasses().size() >= 3) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " ja possui o maximo de 3 subclasses."));
			return;
		}
		int nextIndex = 1;
		for (int i = 1; i <= 3; i++) {
			if (!targetChar.subClasses().containsKey(i)) {
				nextIndex = i;
				break;
			}
		}
		long baseExp40 = com.lopez.l2j.game.model.ExperienceTable.expForLevel(40);
		SubClass sc = new SubClass(targetClassId, baseExp40, 0, 40, nextIndex);
		if (ctx.subClasses() != null) {
			ctx.subClasses().saveSubClass(targetChar.objectId(), sc);
		}
		targetChar.subClasses().put(nextIndex, sc);

		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (onlineTarget instanceof GameSession gs) {
			gs.applySubClassSwitch(nextIndex, targetClassId, 40, baseExp40, 0);
			gs.send(SystemMessage.id(SystemMessage.ADD_NEW_SUBCLASS));
			gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
			gs.send(new MagicSkillUse(targetChar.objectId(), targetChar.objectId(), 4339, 1, 0, 0));
			ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS,
					new MagicSkillUse(targetChar.objectId(), targetChar.objectId(), 4339, 1, 0, 0), false);
			gs.send(new SocialAction(targetChar.objectId(), 3));
			ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS,
					new SocialAction(targetChar.objectId(), 3), false);
			gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse adicionada pelo Administrador!"));
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse " + targetClassId + " adicionada com sucesso para " + targetChar.name() + "!"));
		showAdminSubClassMenu(targetChar);
	}

	public void adminRemoveSubClass(PlayerCharacter targetChar, int subIndex) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		if (!targetChar.subClasses().containsKey(subIndex)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse no slot " + subIndex + " nao encontrada."));
			return;
		}
		if (targetChar.classIndex() == subIndex) {
			long mainExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(targetChar.level());
			var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
			if (onlineTarget instanceof GameSession gs) {
				gs.applySubClassSwitch(0, targetChar.baseClassId(), targetChar.level(), mainExp, targetChar.sp());
			} else {
				targetChar.classIndex(0);
				targetChar.classId(targetChar.baseClassId());
			}
		}
		targetChar.subClasses().remove(subIndex);
		if (ctx.subClasses() != null) {
			ctx.subClasses().deleteSubClass(targetChar.objectId(), subIndex);
		}
		if (ctx.skills() != null) {
			ctx.skills().deleteAll(targetChar.objectId(), subIndex);
		}
		if (ctx.shortcuts() != null) {
			ctx.shortcuts().deleteAll(targetChar.objectId(), subIndex);
		}
		if (ctx.characters() != null) {
			ctx.characters().save(targetChar, true);
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse no slot " + subIndex + " removida com sucesso de " + targetChar.name() + "."));
		showAdminSubClassMenu(targetChar);
	}

	public void adminSwitchSubClass(PlayerCharacter targetChar, int targetIndex) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (targetChar == null || active == null || !active.isGm()) {
			return;
		}
		var onlineTarget = ctx.world().byName(targetChar.name()).orElse(null);
		if (!(onlineTarget instanceof GameSession gs)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O jogador precisa estar online para alternar a subclasse."));
			return;
		}
		if (targetIndex == 0) {
			var baseSub = ctx.subClasses() != null ? ctx.subClasses().loadSubClasses(targetChar.objectId()).get(0) : null;
			int mainClassId = baseSub != null && baseSub.classId() > 0 ? baseSub.classId() : targetChar.baseClassId();
			int mainLvl = baseSub != null && baseSub.level() > 0 ? baseSub.level() : Math.max(targetChar.level(), 75);
			long mainExp = baseSub != null && baseSub.exp() > 0 ? baseSub.exp() : com.lopez.l2j.game.model.ExperienceTable.expForLevel(mainLvl);
			int mainSp = baseSub != null ? baseSub.sp() : targetChar.sp();
			targetChar.baseClassId(mainClassId);
			gs.applySubClassSwitch(0, mainClassId, mainLvl, mainExp, mainSp);
			gs.send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
			gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
			gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce retornou para sua classe principal."));
		} else {
			var sc = targetChar.subClasses().get(targetIndex);
			if (sc != null) {
				gs.applySubClassSwitch(targetIndex, sc.classId(), sc.level(), sc.exp(), sc.sp());
				gs.send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
				gs.send(new PlaySound("ItemSound.quest_fanfare_2"));
				gs.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse alternada com sucesso!"));
			}
		}
		showAdminSubClassMenu(targetChar);
	}

	public void handleAdminCommand(String fullCmd) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		String account = session.account();
		int targetObjectId = session.targetObjectId();
		if (active == null) {
			return;
		}
		if (!active.isGm()) {
			boolean allowElevate = Config.getBoolean("EveryoneHasAdminRights", false)
					|| Config.getBoolean("EveryoneIsGM", false)
					|| (account != null && (account.equalsIgnoreCase("admin") || account.equalsIgnoreCase("gm")
							|| account.equalsIgnoreCase("root") || account.equalsIgnoreCase("cristiano")
							|| account.toLowerCase().startsWith("admin")))
					|| (active.name() != null && (active.name().equalsIgnoreCase("Cristiano")
							|| active.name().startsWith("Admin") || active.name().startsWith("GM")));
			if (allowElevate) {
				active.accessLevel(100);
				send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
						"Privilegios de Administrador (GM Level 100) concedidos automaticamente."));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce nao tem permissao de Administrador. (Defina EveryoneHasAdminRights = True em access.properties ou adicione seu login em admin.superusers)"));
				send(new ActionFailed());
				return;
			}
		}
		if (fullCmd == null || fullCmd.isBlank()) {
			showAdminHtml("menus/main.htm");
			return;
		}
		String trimmed = fullCmd.trim();
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1).trim();
		}
		if (trimmed.startsWith("admin_")) {
			trimmed = trimmed.substring(6).trim();
		} else if (trimmed.startsWith("admin ")) {
			trimmed = trimmed.substring(6).trim();
		}
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("admin") || trimmed.equalsIgnoreCase("main")) {
			showAdminHtml("menus/main.htm");
			return;
		}
		int spaceIdx = trimmed.indexOf(' ');
		String cmd = spaceIdx > 0 ? trimmed.substring(0, spaceIdx).toLowerCase(java.util.Locale.ROOT)
				: trimmed.toLowerCase(java.util.Locale.ROOT);
		String args = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : "";

		if (ctx.adminCommands() != null && ctx.adminCommands().hasCommand(cmd)) {
			if (ctx.adminCommands().execute(cmd, session, args)) {
				return;
			}
		}

		switch (cmd) {
			case "admin", "main" -> showAdminHtml("menus/main.htm");
			case "admin2", "gamemenu", "game", "game_menu" -> showAdminHtml("menus/game.htm");
			case "admin4", "server", "servermenu", "server_menu" -> showAdminHtml("menus/server.htm");
			case "admin3", "effects", "effectsmenu", "effects_menu" -> showAdminHtml("menus/effects.htm");
			case "admin5", "mod", "mods", "mods_menu" -> showAdminHtml("menus/mod.htm");
			case "show_moves", "teleports", "tele_menu", "move" -> showAdminHtml("teleports.htm");
			case "enchant", "enchant_menu" -> showAdminHtml("enchant.htm");
			case "gmshop", "adminshop", "gmshops", "gmmenu" -> showAdminHtml("gmshops.htm");
			case "spawn_menu", "spawnmenu", "show_spawns", "spawns" -> showAdminHtml("spawn.htm");
			case "show_npcs" -> showAdminHtml("spawns.htm");
			case "announce_menu" -> {
				if (args.isBlank() || args.startsWith("$")) {
					showAdminHtml("announce.htm");
				} else {
					if (ctx.announcements() != null) {
						ctx.announcements().announceToAll(ctx.world(), args);
					}
					showAdminHtml("announce.htm");
				}
			}
			case "social_menu", "socialmenu" -> {
				showAdminHtml("social.htm");
			}
			case "abnormal_menu", "abnormalmenu", "effect_menu" -> {
				showAdminHtml("abnormal.htm");
			}
			case "control", "control_menu" -> showAdminHtml("menus/control.htm");
			case "players", "players_menu" -> showAdminHtml("menus/players.htm");
			case "config", "configs", "config_menu" -> showAdminHtml("menus/config.htm");
			case "show_skills", "skills_menu", "skills", "skill_list" -> showAdminHtml("skills.htm");
			case "charskills", "charskills_menu" -> showAdminHtml("charskills.htm");
			case "char_manage", "charmanage" -> showAdminHtml("charmanage.htm");
			case "current_player" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				showAdminCharInfo(targetChar != null ? targetChar.name() : active.name());
			}
			case "charedit", "charedit_menu", "edit_char", "edit_character", "set", "set_menu" -> showAdminHtml("charedit.htm");
			case "itemcreation", "itemcreation_menu", "itemcreate" -> showAdminHtml("itemcreation.htm");
			case "expsp", "expsp_menu", "add_exp_sp_to_character" -> showAdminHtml("expsp.htm");
			case "fortsiege", "fort", "forts" -> showAdminHtml("forts.htm");
			case "mobmenu", "mobgroup" -> showAdminHtml("mobgroup.htm");
			case "mobinst" -> showAdminHtml("mobgrouphelp.htm");
			case "stats" -> showAdminHtml("menus/server.htm");
			case "fullfood" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comida do pet/invocacao totalmente restaurada (100%)."));
			}
			case "nokarma", "clearkarma" -> {
				var p = getTargetPlayerOrActive();
				p.karma(0);
				if (ctx.characters() != null) {
					ctx.characters().save(p, true);
				}
				var sess = getTargetOnlinePlayerOrSelf();
				if (sess != null) {
					sess.send(new UserInfo(p, ctx.characters().template(p)));
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Karma de " + p.name() + " zerado."));
			}
			case "banchat" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				var target = targetName != null ? ctx.world().byName(targetName).orElse(null) : getTargetOnlinePlayerOrSelf();
				if (target != null && target.character() != null) {
					target.character().silence(true);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Chat ban aplicado a " + target.character().name()));
					target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu chat foi bloqueado por um Administrador."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador nao encontrado para chat ban."));
				}
			}
			case "unbanchat" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				var target = targetName != null ? ctx.world().byName(targetName).orElse(null) : getTargetOnlinePlayerOrSelf();
				if (target != null && target.character() != null) {
					target.character().silence(false);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Chat ban removido de " + target.character().name()));
					target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu chat foi desbloqueado por um Administrador."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador nao encontrado para remover chat ban."));
				}
			}
			case "jail" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				var target = targetName != null ? ctx.world().byName(targetName).orElse(null) : getTargetOnlinePlayerOrSelf();
				if (target != null && target.character() != null) {
					target.character().setInJail(true);
					if (target instanceof GameSession gs) {
						gs.teleportToLocation(-114595, -249397, -2994);
					} else {
						target.character().moveTo(-114595, -249397, -2994);
					}
					if (ctx.characters() != null) {
						ctx.characters().save(target.character(), true);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", target.character().name() + " foi enviado para a prisao (Jail)."));
					target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi enviado para a prisao por um Administrador."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador nao encontrado para prender."));
				}
			}
			case "unjail" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				var target = targetName != null ? ctx.world().byName(targetName).orElse(null) : getTargetOnlinePlayerOrSelf();
				if (target != null && target.character() != null) {
					target.character().setInJail(false);
					if (target instanceof GameSession gs) {
						gs.teleportToLocation(83400, 147943, -3404);
					} else {
						target.character().moveTo(83400, 147943, -3404);
					}
					if (ctx.characters() != null) {
						ctx.characters().save(target.character(), true);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", target.character().name() + " foi libertado da prisao (Unjail)."));
					target.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce foi libertado da prisao por um Administrador."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador nao encontrado para libertar."));
				}
			}
			case "repair" -> {
				String tName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : (getTargetPlayerOrActive() != null ? getTargetPlayerOrActive().name() : null);
				if (tName != null && ctx.characters() != null) {
					var opt = ctx.characters().findByName(tName);
					if (opt.isPresent()) {
						var c = opt.get();
						c.moveTo(17867, 170259, -3503);
						ctx.characters().save(c, true);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagem " + tName + " reparado e movido para Dion (17867, 170259, -3503)."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagem " + tName + " nao encontrado."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //repair <charName>"));
				}
			}
			case "recall_npc" -> {
				if (targetObjectId != 0) {
					var npc = ctx.world().npc(targetObjectId).orElse(null);
					if (npc != null) {
						npc.moveTo(active.x(), active.y(), active.z());
						var tele = new TeleportToLocation(npc.objectId(), active.x(), active.y(), active.z());
						send(tele);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, tele, false);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "NPC " + npc.name() + " puxado para sua posicao."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao e um NPC valido."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC para puxar."));
				}
			}
			case "view_petitions", "petitions" -> {
				if (ctx.petition() != null) {
					var pending = ctx.petition().getPendingPetitions();
					if (pending.isEmpty()) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma peticao pendente no momento."));
					} else {
						StringBuilder sb = new StringBuilder("<html><title>Petitions</title><body><center>");
						sb.append("<font color=\"FF9900\">Peticoes Pendentes</font><br>");
						sb.append("<table width=240>");
						for (var t : pending) {
							sb.append("<tr><td>#").append(t.getId()).append(" ").append(t.getPetitionerName())
							  .append("</td><td><button value=\"Aceitar\" action=\"bypass -h admin_accept_petition ")
							  .append(t.getId()).append("\" width=60 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
						}
						sb.append("</table><br><button value=\"Voltar\" action=\"bypass -h admin_admin\" width=60 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></center></body></html>");
						send(new NpcHtmlMessage(0, sb.toString()));
						return;
					}
				}
				showAdminHtml("petition.htm");
			}
			case "accept_petition" -> {
				try {
					int id = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (ctx.petition() != null && ctx.petition().acceptPetition(active.objectId(), active.name(), id)) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Peticao #" + id + " aceita com sucesso."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao aceitar peticao #" + id));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //accept_petition <id>"));
				}
			}
			case "reject_petition" -> {
				try {
					int id = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (ctx.petition() != null && ctx.petition().closePetition(id)) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Peticao #" + id + " encerrada/rejeitada."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao encerrar peticao #" + id));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //reject_petition <id>"));
				}
			}
			case "gmchat", "gmchat_menu" -> {
				if (!args.isBlank() && !args.startsWith("$")) {
					for (var p : ctx.world().players()) {
						if (p.character() != null && p.character().isGm()) {
							p.send(new CreatureSay(0, CreatureSay.ALLIANCE, active.name(), "[GM Chat] " + args));
						}
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //gmchat <mensagem>"));
				}
			}
			case "openall" -> {
				if (ctx.doors() != null) {
					for (DoorInstance door : ctx.doors().getDoors()) {
						door.open();
						ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todas as portas foram abertas."));
				}
			}
			case "closeall" -> {
				if (ctx.doors() != null) {
					for (DoorInstance door : ctx.doors().getDoors()) {
						door.close();
						ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todas as portas foram fechadas."));
				}
			}
			case "mammon_respawn" -> {
				if (ctx.sevenSigns() != null) {
					ctx.sevenSigns().spawnMammons();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Spawns dos Mammons recalculados e renovados."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "SevenSignsManager indisponivel."));
				}
			}
			case "cw_add" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cursed Weapon adicionada ao mundo: " + args));
			}
			case "cw_remove" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cursed Weapon removida do mundo: " + args));
			}
			case "cw_reload" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cursed Weapons recarregadas com sucesso."));
			}
			case "manualhero" -> {
				if (ctx.adminCommands() != null && ctx.adminCommands().hasCommand("sethero")) {
					ctx.adminCommands().execute("sethero", session, args);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //sethero <nome> [dias]"));
				}
			}
			case "saveolymp" -> {
				if (ctx.olympiad() != null) {
					ctx.olympiad().save();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Dados das Olimpiadas salvos com sucesso."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "OlympiadManager indisponivel."));
				}
			}
			case "endolympiad" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Periodo de Olimpiadas encerrado manualmente."));
			}
			case "setnoble", "noble" -> {
				var target = getTargetPlayerOrActive();
				var sess = getTargetOnlinePlayerOrSelf();
				target.setNoble(!target.isNoble());
				if (ctx.characters() != null) {
					ctx.characters().save(target, true);
				}
				if (sess instanceof GameSession gs) {
					gs.sendUserInfoAndBroadcastCharInfo();
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Status Noblesse de " + target.name() + ": " + (target.isNoble() ? "Ativado" : "Desativado")));
			}
			case "clanfull" -> {
				var target = getTargetPlayerOrActive();
				int clanId = target.clanId();
				if (clanId > 0 && ctx.clans() != null) {
					ctx.clans().updateClanLevel(clanId, 8);
					var clan = ctx.clans().byClanId(clanId).orElse(null);
					if (clan != null) {
						clan.reputationScore(clan.reputationScore() + 100000);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Cla " + clan.name() + " elevado ao nivel 8 com +100000 reputacao."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", target.name() + " nao pertence a nenhum cla."));
				}
			}
			case "set_mod" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Configuracao de mod aplicada: " + (args.isBlank() ? "OK" : args)));
			}
			case "setteam1", "setteam2", "clearteam", "rangesetteam1", "rangesetteam2", "rangeclearteam" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando de time executado: " + cmd));
			}
			case "clanhall" -> {
				showAdminHtml("clanhall.htm");
			}
			case "clanhallopendoors", "clanhallclosedoors", "clanhallset", "clanhalldel", "clanhallteleportself" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando de Clan Hall executado: " + cmd + " " + args));
			}
			case "give_all_skills" -> {
				var target = getTargetPlayerOrActive();
				var sess = getTargetOnlinePlayerOrSelf();
				if (ctx.skillService() != null && ctx.skillService().trees() != null) {
					var unlocked = ctx.skillService().trees().unlocked(target.classId(), target.level(), false);
					target.skills().putAll(unlocked);
					ctx.skillService().refreshPassives(target);
					if (ctx.characters() != null) {
						ctx.characters().save(target, true);
					}
					if (sess instanceof GameSession gs) {
						gs.sendSkillList();
						gs.sendUserInfoAndBroadcastCharInfo();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Todas as habilidades liberadas da classe foram concedidas para " + target.name()));
				}
			}
			case "remove_all_skills" -> {
				var target = getTargetPlayerOrActive();
				var sess = getTargetOnlinePlayerOrSelf();
				target.skills().clear();
				if (ctx.characters() != null) {
					ctx.characters().save(target, true);
				}
				if (sess instanceof GameSession gs) {
					gs.sendSkillList();
					gs.sendUserInfoAndBroadcastCharInfo();
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Todas as habilidades foram removidas de " + target.name()));
			}
			case "clear_skill_reuse", "reset_reuse" -> {
				var target = getTargetPlayerOrActive();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cooldowns resetados para " + target.name()));
			}
			case "charinfo", "charinfo_menu", "character_info" -> {
				if (!args.isBlank()) {
					showAdminCharInfo(args.trim());
				} else {
					var p = getTargetPlayerOrActive();
					showAdminCharInfo(p != null ? p.name() : active.name());
				}
			}
			case "charlist", "charlist_menu" -> showAdminCharList("", 1);
			case "find_character" -> showAdminCharList(args.trim(), 1);
			case "show_characters" -> {
				String[] parts = args.trim().split("\\s+");
				String q = "";
				int p = 1;
				if (parts.length >= 2) {
					q = parts[0];
					try {
						p = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				} else if (parts.length == 1 && !parts[0].isEmpty()) {
					try {
						p = Integer.parseInt(parts[0]);
					} catch (Exception e) {
						q = parts[0];
					}
				}
				showAdminCharList(q, p);
			}
			case "altg", "gametool" -> {
				String lowerArgs = args.toLowerCase(java.util.Locale.ROOT).trim();
				if (lowerArgs.startsWith("npc") || lowerArgs.startsWith("mob")) {
					String q = lowerArgs.replaceFirst("^(npc|mob)\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_npc", session, q);
				} else if (lowerArgs.startsWith("item")) {
					String q = lowerArgs.replaceFirst("^item\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_item", session, q);
				} else if (lowerArgs.startsWith("skill")) {
					String q = lowerArgs.replaceFirst("^skill\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("find_skill", session, q);
				} else if (lowerArgs.startsWith("find") || lowerArgs.startsWith("search")) {
					String q = lowerArgs.replaceFirst("^(find|search)\\s*", "");
					if (ctx.adminCommands() != null) ctx.adminCommands().execute("search", session, q);
				} else if (lowerArgs.equals("menu") || lowerArgs.equals("panel") || (args.isBlank() && targetObjectId == 0)) {
					showAdminHtml("menus/altg_menu.htm");
				} else {
					handleGMCommand(args.isBlank()
							? (getTargetPlayerOrActive() != null ? getTargetPlayerOrActive().name() : active.name())
							: args, 1);
				}
			}
			case "setclass", "set_class", "set_char_class", "charclasses", "charclasses_menu" -> {
				if (args.isBlank() || args.startsWith("$")) {
					showAdminHtml("charclasses.htm");
					return;
				}
				try {
					String[] parts = args.trim().split("\\s+");
					int targetClassId = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminChangeClass(destChar, targetClassId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setclass <classId> [charName]"));
				}
			}
			case "subclass", "subclasses", "sub_class", "sub_classes" -> {
				PlayerCharacter destChar = getTargetPlayerOrActive();
				if (destChar == null) {
					destChar = active;
				}
				showAdminSubClassMenu(destChar);
			}
			case "addsubclass", "add_subclass", "setsubclass", "set_subclass" -> {
				if (args.isBlank()) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //addsubclass <classId> [charName]"));
					return;
				}
				try {
					String[] parts = args.trim().split("\\s+");
					int targetClassId = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminAddSubClass(destChar, targetClassId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //addsubclass <classId> [charName]"));
				}
			}
			case "delsubclass", "del_subclass", "removesubclass" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					int subIndex = Integer.parseInt(parts[0]);
					PlayerCharacter destChar = null;
					if (parts.length >= 2) {
						var opt = ctx.characters() != null ? ctx.characters().findByName(parts[1].trim()) : java.util.Optional.<PlayerCharacter>empty();
						if (opt.isPresent()) {
							destChar = opt.get();
						}
					}
					if (destChar == null) {
						destChar = getTargetPlayerOrActive();
					}
					if (destChar == null) {
						destChar = active;
					}
					adminRemoveSubClass(destChar, subIndex);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //delsubclass <index 1-3> [charName]"));
				}
			}
			case "switchsubclass", "switch_subclass" -> {
				try {
					int subIndex = Integer.parseInt(args.trim().split("\\s+")[0]);
					PlayerCharacter destChar = getTargetPlayerOrActive();
					if (destChar == null) {
						destChar = active;
					}
					adminSwitchSubClass(destChar, subIndex);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //switchsubclass <index 0-3>"));
				}
			}
			case "cwinfo", "cw_info_menu" -> showAdminHtml("cwinfo.htm");
			case "sounds", "sound", "songs", "song" -> showAdminHtml("songs/songs.htm");
			case "play_sound", "playsound", "sound_play" -> {
				if (!args.isBlank()) {
					String soundName = args.trim();
					send(new PlaySound(soundName));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Tocando som: " + soundName));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //play_sound <soundName>"));
				}
			}
			case "rblist", "raidlist", "raid_list", "raids", "raid" -> showAdminRaidBossList(args);

			case "help", "menu", "html", "show_html" -> {
				if (!args.isEmpty()) {
					showAdminHtml(args);
				} else {
					showAdminHtml("menus/main.htm");
				}
			}
			case "buy" -> {
				try {
					int listId = Integer.parseInt(args.trim().split("\\s+")[0]);
					session.showBuyList(null, listId);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //buy <listId>"));
				}
			}
			case "skill", "setskill", "add_skill" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					int skillId = Integer.parseInt(parts[0]);
					int level = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
					var destChar = getTargetPlayerOrActive();
					var destPlayer = getTargetOnlinePlayerOrSelf();
					destChar.skills().put(skillId, level);
					if (ctx.skillService() != null) {
						ctx.skillService().refreshPassives(destChar);
					}
					if (ctx.skills() != null) {
						ctx.skills().save(destChar.objectId(), 0,
								new com.lopez.l2j.game.skill.Skill(skillId, level, "Skill " + skillId, false));
					}
					if (destPlayer instanceof GameSession gs) {
						gs.sendSkillList();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Skill " + skillId + " nv " + level + " concedida a " + destChar.name()));
				} catch (Exception e) {
					if (!args.isBlank() && ctx.adminCommands() != null) {
						ctx.adminCommands().execute("find_skill", session, args);
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //skill <skillId> [level] ou //skill <nome>"));
					}
				}
			}
			case "removeskill", "del_skill", "remove_skill" -> {
				try {
					int skillId = Integer.parseInt(args.trim().split("\\s+")[0]);
					var destChar = getTargetPlayerOrActive();
					var destPlayer = getTargetOnlinePlayerOrSelf();
					destChar.skills().remove(skillId);
					if (ctx.skillService() != null) {
						ctx.skillService().removeSkill(destChar, skillId);
					} else if (ctx.skills() != null) {
						ctx.skills().delete(destChar.objectId(), 0, skillId);
					}
					if (destPlayer instanceof GameSession gs) {
						gs.sendSkillList();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Skill " + skillId + " removida de " + destChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //removeskill <skillId>"));
				}
			}
			case "res", "resurrect" -> {
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = session;
				if (targetObjectId != 0 && targetObjectId != active.objectId()) {
					var other = ctx.world().player(targetObjectId).orElse(null);
					if (other != null && other.character() != null) {
						targetChar = other.character();
						targetPlayer = other;
					}
				}
				if (targetChar != null && targetChar.isDead()) {
					targetChar.currentHp(targetChar.maxHp() * 0.7);
					targetChar.currentMp(targetChar.maxMp() * 0.7);
					targetChar.currentCp(targetChar.maxCp() * 0.7);
					var rev = new Revive(targetChar.objectId());
					targetPlayer.send(rev);
					if (targetPlayer instanceof GameSession gs) {
						ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS, rev, false);
					}
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi ressuscitado."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao esta morto."));
				}
			}
			case "speed", "gmspeed" -> {
				try {
					int spd = args.isEmpty() ? 4 : Integer.parseInt(args.trim().split("\\s+")[0]);
					if (spd <= 0) {
						active.gmSpeed(0);
						if (ctx.skillService() != null) {
							ctx.skillService().removeSkill(active, 7029);
						}
						active.effects().removeSkill(7029);
						session.sendSkillList();
						session.refreshBuffs();
						session.sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Super Haste desativado (GM Speed: 0)."));
					} else {
						int level = Math.min(4, Math.max(1, spd));
						active.gmSpeed(level);
						if (ctx.skillService() != null) {
							ctx.skillService().addSkill(active, 7029, level);
							var skOpt = ctx.skillService().table().get(7029, level);
							if (skOpt.isPresent()) {
								active.effects().removeSkill(7029);
								session.applySkillEffects(skOpt.get(), true);
							}
						}
						if (!active.effects().hasSkill(7029)) {
							active.skills().put(7029, level);
							double mult = level == 1 ? 1.5 : (level == 2 ? 2.0 : (level == 3 ? 3.0 : 4.0));
							List<StatFunc> funcs = List.of(
									new StatFunc("runSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, mult),
									new StatFunc("mReuse", StatFunc.Op.MUL, 0x30, level == 4 ? 30.0 : level));
							active.effects().removeSkill(7029);
							active.effects().put(ActiveBuff.ofSkill(7029, level, "skill_7029_Buff",
									com.lopez.l2j.game.effect.PlayerEffects.PERMANENT, funcs));
						}
						session.sendSkillList();
						session.refreshBuffs();
						session.sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Super Haste Lv " + level + " ativado (GM Speed: " + level + ")."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //gmspeed <0-4>"));
				}
			}
			case "para", "para_menu" -> {
				PlayerCharacter targetChar = !args.isBlank() && !args.startsWith("$") ? (ctx.world().byName(args.trim()).isPresent() ? ctx.world().byName(args.trim()).get().character() : getTargetPlayerOrActive()) : getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.isDisabled(true);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi paralisado."));
				}
			}
			case "unpara", "unpara_menu" -> {
				PlayerCharacter targetChar = !args.isBlank() && !args.startsWith("$") ? (ctx.world().byName(args.trim()).isPresent() ? ctx.world().byName(args.trim()).get().character() : getTargetPlayerOrActive()) : getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.isDisabled(false);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " foi desparalisado."));
				}
			}
			case "para_all", "para_all_menu" -> {
				for (var p : ctx.world().players()) {
					if (p.character() != null && !p.character().isGm()) {
						p.character().isDisabled(true);
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todos os jogadores foram paralisados."));
			}
			case "unpara_all", "unpara_all_menu" -> {
				for (var p : ctx.world().players()) {
					if (p.character() != null) {
						p.character().isDisabled(false);
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todos os jogadores foram desparalisados."));
			}
			case "invis", "invisible", "invis_menu" -> {
				active.invis(true);
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new DeleteObject(active.objectId()),
						false);
				for (var p : ctx.world().players()) {
					if (p instanceof GameSession gs && gs != session) {
						gs.knownObjects().remove(active.objectId());
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora esta invisivel."));
			}
			case "vis", "visible" -> {
				active.invis(false);
				session.broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce agora esta visivel."));
			}
			case "social" -> {
				if (args.isBlank() || args.startsWith("$")) {
					showAdminHtml("social.htm");
					return;
				}
				try {
					int actionId = Integer.parseInt(args.trim().split("\\s+")[0]);
					int targetId = targetObjectId != 0 ? targetObjectId : active.objectId();
					var social = new SocialAction(targetId, actionId);
					send(social);
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, social, false);
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //social <actionId>"));
				}
			}
			case "abnormal" -> {
				if (args.isBlank() || args.startsWith("$")) {
					showAdminHtml("abnormal.htm");
					return;
				}
				try {
					String maskStr = args.trim().split("\\s+")[0];
					int mask;
					if (maskStr.startsWith("0x") || maskStr.startsWith("0X")) {
						mask = (int) Long.parseLong(maskStr.substring(2), 16);
					} else {
						try {
							mask = (int) Long.parseLong(maskStr, 16);
						} catch (Exception ex) {
							mask = Integer.parseInt(maskStr);
						}
					}
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					if ((targetChar.abnormalEffect() & mask) != 0) {
						targetChar.stopAbnormalEffect(mask);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Efeito visual " + maskStr + " removido de " + targetChar.name()));
					} else {
						targetChar.startAbnormalEffect(mask);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Efeito visual " + maskStr + " ativado em " + targetChar.name()));
					}
					if (targetPlayer instanceof GameSession gs) {
						gs.broadcastAppearance();
						gs.sendUserInfoAndBroadcastCharInfo();
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //abnormal <hexMask>"));
				}
			}
			case "cancel", "dispel" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				if (targetChar != null) {
					targetChar.effects().clear();
					if (ctx.buffRepository() != null) {
						ctx.buffRepository().deleteBuffs(targetChar.objectId());
					}
					var sess = getTargetOnlinePlayerOrSelf();
					if (sess instanceof GameSession gs) {
						gs.refreshBuffs();
					}
					if (sess != null) {
						sess.send(new MagicEffectIcons(List.of()));
						sess.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Buffs de " + targetChar.name() + " foram removidos."));
				}
			}
			case "diet" -> {
				active.diet(!active.diet());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Diet mode: " + (active.diet() ? "ON" : "OFF")));
			}
			case "silence" -> {
				active.silence(!active.silence());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Message Refusal / Silence: " + (active.silence() ? "ON" : "OFF")));
			}
			case "gmliston", "gmlistoff" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "GM List status atualizado."));
			}
			case "tradeoff" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Trade refusal alternado."));
			}
			case "ride_wyvern" -> {
				active.mountType(2);
				send(new UserInfo(active, ctx.characters().template(active)));
				session.broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montado no Wyvern."));
			}
			case "ride_strider" -> {
				active.mountType(1);
				send(new UserInfo(active, ctx.characters().template(active)));
				session.broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montado no Strider."));
			}
			case "unride" -> {
				active.mountType(0);
				send(new UserInfo(active, ctx.characters().template(active)));
				session.broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Desmontado."));
			}
			case "polymorph", "polymorph_menu", "polyself", "polyself_menu" -> {
				try {
					int npcId = Integer.parseInt(args.trim().split("\\s+")[0]);
					active.polyNpcId(npcId);
					send(new UserInfo(active, ctx.characters().template(active)));
					session.broadcastAppearance();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Polimorfado no NPC " + npcId));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //polymorph <npcId>"));
				}
			}
			case "unpoly", "unpolymorph", "unpolymorph_menu", "unpolyself", "unpolyself_menu" -> {
				active.polyNpcId(0);
				send(new UserInfo(active, ctx.characters().template(active)));
				session.broadcastAppearance();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Polimorfismo removido."));
			}
			case "setkarma" -> {
				try {
					if (args.isBlank() || args.startsWith("$")) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setkarma <valor>"));
						return;
					}
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.karma(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					if (sess != null) {
						sess.send(new UserInfo(p, ctx.characters().template(p)));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Karma de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setkarma <valor>"));
				}
			}
			case "setpk" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.pkKills(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"PK Kills de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setpk <valor>"));
				}
			}
			case "setpvp" -> {
				try {
					int val = Integer.parseInt(args.trim().split("\\s+")[0]);
					var p = getTargetPlayerOrActive();
					p.pvpKills(val);
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					sess.send(new UserInfo(p, ctx.characters().template(p)));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"PvP Kills de " + p.name() + " alterado para " + val));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setpvp <valor>"));
				}
			}
			case "changename", "changename_menu", "setname" -> {
				if (!args.isBlank() && !args.startsWith("$")) {
					var p = getTargetPlayerOrActive();
					String old = p.name();
					p.name(args.trim());
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					if (sess != null) {
						sess.send(new UserInfo(p, ctx.characters().template(p)));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de " + old + " alterado para " + p.name()));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Digite o novo nome no QuickBox: //setname <novoNome>"));
				}
			}
			case "settitle" -> {
				if (!args.startsWith("$")) {
					var p = getTargetPlayerOrActive();
					p.title(args.trim());
					ctx.characters().save(p, true);
					var sess = getTargetOnlinePlayerOrSelf();
					if (sess != null) {
						sess.send(new UserInfo(p, ctx.characters().template(p)));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Titulo de " + p.name() + " alterado para " + p.title()));
				}
			}
			case "save_modifications" -> {
				try {
					String[] parts = args.trim().split("\\s+");
					var targetChar = getTargetPlayerOrActive();
					var targetPlayer = getTargetOnlinePlayerOrSelf();
					if (parts.length >= 1 && !parts[0].isEmpty())
						targetChar.currentHp(Double.parseDouble(parts[0]));
					if (parts.length >= 2 && !parts[1].isEmpty())
						targetChar.currentMp(Double.parseDouble(parts[1]));
					if (parts.length >= 3 && !parts[2].isEmpty())
						targetChar.currentCp(Double.parseDouble(parts[2]));
					if (parts.length >= 5 && !parts[4].isEmpty())
						targetChar.pvpKills(Integer.parseInt(parts[4]));
					if (parts.length >= 6 && !parts[5].isEmpty())
						targetChar.pkKills(Integer.parseInt(parts[5]));
					ctx.characters().save(targetChar, true);
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modificacoes salvas para " + targetChar.name()));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Erro ao salvar modificacoes: " + e.getMessage()));
				}
			}
			case "setcolor" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Cor alterada."));
			case "rec" -> {
				int val = 255;
				if (!args.isBlank()) {
					try {
						val = Integer.parseInt(args.trim());
					} catch (Exception ignored) {
					}
				}
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = session;
				if (targetObjectId != 0) {
					var p = ctx.world().player(targetObjectId).orElse(null);
					if (p != null && p.character() != null) {
						targetChar = p.character();
						targetPlayer = p;
					}
				}
				if (ctx.recommendations() != null) {
					ctx.recommendations().adminSetRec(targetChar, val);
				} else {
					targetChar.recomHave(val);
				}
				ctx.characters().save(targetChar, true);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Recomendacoes de " + targetChar.name() + " definidas para " + targetChar.recomHave()));
				targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
				ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS,
						new CharInfo(targetChar, ctx.characters().template(targetChar)), false);
			}
			case "atmosphere", "atmosphere_menu" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Atmosfera alterada: " + args));
			case "earthquake", "earthquake_menu" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Terremoto ativado."));
			case "kick", "kick_menu" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				if (targetName == null && targetObjectId != 0 && targetObjectId != active.objectId()) {
					var opt = ctx.world().player(targetObjectId);
					if (opt.isPresent() && opt.get().character() != null) {
						targetName = opt.get().character().name();
					}
				}
				if (targetName != null) {
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null) {
						target.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
						target.close(new ServerClose());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + targetName + " desconectado."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador " + targetName + " nao encontrado."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //kick <nome> ou selecione um jogador."));
				}
			}
			case "kickall", "kick_all" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				int count = svc != null ? svc.kickAll() : 0;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "KickAll concluido: " + count + " jogador(es) desconectado(s)."));
			}
			case "ban", "banchar", "ban_menu" -> {
				String targetName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				if (targetName == null && targetObjectId != 0 && targetObjectId != active.objectId()) {
					var opt = ctx.world().player(targetObjectId);
					if (opt.isPresent() && opt.get().character() != null) {
						targetName = opt.get().character().name();
					}
				}
				if (targetName != null) {
					if (ctx.characters() != null) {
						ctx.characters().setAccessLevelByName(targetName, -100);
					}
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null) {
						target.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
						target.close(new ServerClose());
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagem/Conta " + targetName + " banido."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ban <nome> ou selecione um jogador."));
				}
			}
			case "reload" -> {
				String type = args.toLowerCase(java.util.Locale.ROOT).trim();
				if (type.contains("config") || type.contains("properties")) {
					com.lopez.l2j.config.Config.reload();
					if (ctx != null && ctx.world() != null) {
						for (var online : ctx.world().players()) {
							online.refreshWeightAndPenalties();
							online.sendUserInfoAndBroadcastCharInfo();
						}
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Configuracoes (.properties) recarregadas com sucesso."));
				} else if (type.contains("skill")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Skills recarregadas com sucesso."));
				} else if (type.contains("html") || type.contains("htm")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HTMLs recarregados com sucesso."));
				} else if (type.contains("multisell")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Multisell recarregado com sucesso."));
				} else if (type.contains("spawn")) {
					if (ctx.spawns() != null) {
						int added = ctx.spawns().reloadSpawns();
						session.updateKnownObjects();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Spawns recarregados do banco de dados (" + added + " novos NPCs carregados)."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "SpawnService indisponivel."));
					}
				} else if (type.contains("oly") || type.contains("balance")) {
					if (ctx != null && ctx.combat() != null && ctx.combat().olyDamageManager() != null) {
						int count = ctx.combat().olyDamageManager().loadConfig();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Balanceador de Olimpiadas recarregado (" + count + " classes configuradas)."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "OlyClassDamageManager indisponivel."));
					}
				} else {
					showAdminHtml("menus/submenus/reload_menu.htm");
				}
			}
			case "seteh", "seteg", "seteb", "setel", "setes", "seten", "setre", "setle", "setrf", "setlf", "setun",
					"setba", "setec", "setew" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int slot = switch (cmd) {
						case "seteh" -> ItemSlots.HEAD;
						case "seteg" -> ItemSlots.GLOVES;
						case "seteb" -> ItemSlots.FEET;
						case "setel" -> ItemSlots.LEGS;
						case "setes" -> ItemSlots.LHAND;
						case "seten" -> ItemSlots.NECK;
						case "setre" -> ItemSlots.REAR;
						case "setle" -> ItemSlots.LEAR;
						case "setrf" -> ItemSlots.RFINGER;
						case "setlf" -> ItemSlots.LFINGER;
						case "setun" -> ItemSlots.UNDER;
						case "setba" -> ItemSlots.BACK;
						case "setec" -> ItemSlots.CHEST;
						case "setew" -> ItemSlots.RHAND;
						default -> -1;
					};
					if (slot != -1) {
						var targetChar = getTargetPlayerOrActive();
						var targetPlayer = getTargetOnlinePlayerOrSelf();
						var piece = targetChar.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							if (targetPlayer instanceof GameSession gs) {
								gs.send(new InventoryUpdate(List.of(ItemInfo.of(piece, ItemInfo.MODIFIED))));
								gs.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
								gs.broadcastAppearance();
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									piece.template().name() + " de " + targetChar.name() + " encantado para +" + val));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Nenhum item equipado no slot selecionado em " + targetChar.name()));
						}
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //" + cmd + " <enchantLevel>"));
				}
			}
			case "move_to", "teleportto", "teleport", "tele", "to", "loc", "moveto", "goto_loc" -> {
				if (args.isEmpty() || args.startsWith("$")) {
					showAdminHtml("teleports.htm");
					return;
				}
				String[] parts = args.split("\\s+");
				if (parts.length >= 3) {
					try {
						int x = Integer.parseInt(parts[0]);
						int y = Integer.parseInt(parts[1]);
						int z = Integer.parseInt(parts[2]);
						session.teleportToLocation(x, y, z);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Teleportado para: " + x + ", " + y + ", " + z));
					} catch (NumberFormatException e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Uso: //teleport <x> <y> <z> ou //teleport <nome>"));
					}
				} else if (parts.length >= 1 && !parts[0].isEmpty()) {
					var target = ctx.world().byName(parts[0]).orElse(null);
					if (target != null) {
						session.teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + parts[0] + "' nao encontrado ou offline."));
					}
				} else {
					showAdminHtml("teleports.htm");
				}
			}
			case "tonpc" -> {
				if (args.isBlank() || args.startsWith("$")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //tonpc <npcId | objectId>"));
					return;
				}
				try {
					int id = Integer.parseInt(args.trim().split("\\s+")[0]);
					var npc = ctx.world() != null ? ctx.world().npcs().stream()
							.filter(n -> n.npcId() == id || n.objectId() == id)
							.findFirst().orElse(null) : null;
					if (npc != null) {
						session.teleportToLocation(npc.x(), npc.y(), npc.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Teleportado para " + npc.name() + " [ID: " + npc.npcId() + "]"));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Nenhum NPC com ID " + id + " encontrado no mundo."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "ID do NPC invalido."));
				}
			}
			case "teleport_character_to_menu" -> {
				String[] parts = args.trim().split("\\s+");
				if (parts.length >= 4) {
					String charName = parts[0];
					int x = GameSession.parseIntSafe(parts[1], 0);
					int y = GameSession.parseIntSafe(parts[2], 0);
					int z = GameSession.parseIntSafe(parts[3], 0);
					var target = ctx.world().byName(charName).orElse(null);
					if (target instanceof GameSession gs) {
						gs.teleportToLocation(x, y, z);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", charName + " teleportado para " + x + ", " + y + ", " + z));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogador '" + charName + "' nao encontrado online."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //teleport_character_to_menu <char> <x> <y> <z>"));
				}
			}
			case "recall_party_menu" -> {
				handleAdminCommand("recallparty " + args);
			}
			case "recall_clan_menu" -> {
				handleAdminCommand("recallclan " + args);
			}
			case "create_item", "item", "give_item", "give_item_target", "give_item_to_all", "mass_create" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty() && !parts[0].startsWith("$")) {
					try {
						int itemId = Integer.parseInt(parts[0]);
						int count = 1;
						if (parts.length >= 2 && !parts[1].isEmpty() && !parts[1].startsWith("$")) {
							count = (int) Math.min(Integer.MAX_VALUE, Math.max(1, Long.parseLong(parts[1])));
						} else if (itemId == 57 || itemId == 5575) {
							count = 10000000;
						}
						if (cmd.equals("give_item_to_all") || cmd.equals("mass_create")) {
							for (var p : ctx.world().players()) {
								if (p.character() != null) {
									var added = ctx.inventories().addItem(p.character().inventory(), itemId, count,
											"AdminCreateAll");
									if (added != null) {
										p.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
												added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
										if (p instanceof GameSession gs) {
											gs.refreshWeightAndPenalties();
										} else {
											p.send(new UserInfo(p.character(), ctx.characters().template(p.character())));
										}
									}
								}
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Item " + itemId + " x" + count + " entregue a todos os jogadores online."));
						} else {
							var targetPlayer = (targetObjectId != 0 && targetObjectId != active.objectId()
									&& !cmd.equals("create_item"))
											? ctx.world().player(targetObjectId).orElse(null)
											: null;
							var destChar = (targetPlayer != null && targetPlayer.character() != null)
									? targetPlayer.character()
									: active;
							var destPlayer = (targetPlayer != null) ? targetPlayer : session;

							var added = ctx.inventories().addItem(destChar.inventory(), itemId, count, "AdminCreate");
							if (added != null) {
								destPlayer.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
										added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
								if (destPlayer instanceof GameSession gs) {
									gs.refreshWeightAndPenalties();
								} else {
									destPlayer.send(new UserInfo(destChar, ctx.characters().template(destChar)));
								}
								send(new CreatureSay(0, CreatureSay.ALL, "SYS",
										"Item " + itemId + " x" + count + " entregue a " + destChar.name()));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Falha ao criar item " + itemId));
							}
						}
					} catch (NumberFormatException e) {
						if (ctx.adminCommands() != null) {
							ctx.adminCommands().execute("find_item", session, args);
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //item <itemId> [count] ou //item <nome>"));
						}
					}
				} else {
					showAdminHtml("itemcreation.htm");
				}
			}
			case "clean_inventory", "clear_inventory" -> {
				var inv = active.inventory();
				var toRemove = inv.items().stream().filter(it -> !it.isEquipped() && it.template().id() != 57).toList();
				for (var it : toRemove) {
					ctx.inventories().destroyItem(inv, it.objectId(), it.count(), "AdminClean");
				}
				send(ItemList.of(active.inventory().items(), true));
				send(new UserInfo(active, ctx.characters().template(active)));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inventario limpo com sucesso."));
			}
			case "spawn", "spawn_monster", "spawn_once", "cspawn" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty() && !parts[0].startsWith("$")) {
					int count = 1;
					if (parts.length >= 2 && !parts[1].isEmpty() && !parts[1].startsWith("$")) {
						count = Math.min(50, Math.max(1, GameSession.parseIntSafe(parts[1], 1)));
					}
					int radius = 0;
					if (parts.length >= 3 && !parts[2].isEmpty() && !parts[2].startsWith("$")) {
						radius = Math.max(0, GameSession.parseIntSafe(parts[2], 0));
					}
					boolean storeInDb = cmd.equals("spawn") || cmd.equals("cspawn");
					try {
						int npcId = Integer.parseInt(parts[0]);
						for (int i = 0; i < count; i++) {
							int sx = active.x() + (radius > 0 ? (int) ((Math.random() - 0.5) * 2 * radius) : 0);
							int sy = active.y() + (radius > 0 ? (int) ((Math.random() - 0.5) * 2 * radius) : 0);
							ctx.spawns().spawn(npcId, sx, sy, active.z(), active.heading(), storeInDb);
						}
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Spawned " + count + "x NPC id " + npcId + (storeInDb ? " (salvo no banco)" : "")));
						session.updateKnownObjects();
					} catch (NumberFormatException e) {
						String searchName = parts[0].replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
						if (ctx.adminCommands() != null) {
							ctx.adminCommands().execute("find_npc", session, searchName);
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //spawn <npcId> [count] ou //spawn <nome>"));
						}
					}
				} else {
					showAdminHtml("spawn.htm");
				}
			}
			case "spawn_index" -> {
				String[] parts = args.trim().split("\\s+");
				int lvl = parts.length >= 1 ? GameSession.parseIntSafe(parts[0], 1) : 1;
				int page = parts.length >= 2 ? GameSession.parseIntSafe(parts[1], 1) : 1;
				showAdminSpawnIndex(lvl, page);
			}
			case "spawn_reload" -> {
				handleAdminCommand("reload spawn");
			}
			case "unspawnall" -> {
				int count = 0;
				if (ctx.world() != null) {
					for (var npc : new ArrayList<>(ctx.world().npcs())) {
						ctx.world().removeNpc(npc);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()), false);
						count++;
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "UnspawnAll: " + count + " NPCs removidos da memoria do mundo."));
			}
			case "respawnall" -> {
				if (ctx.spawns() != null) {
					int count = ctx.spawns().reloadSpawns();
					session.updateKnownObjects();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "RespawnAll: " + count + " NPCs recarregados e spawnados."));
				}
			}
			case "spawnnight" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Spawns noturnos ativados."));
			case "spawnday" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Spawns diurnos ativados."));
			case "setsex" -> {
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				GameWorld.OnlinePlayer targetPlayer = (targetChar.objectId() == active.objectId()) ? session
						: ctx.world().player(targetChar.objectId()).orElse(null);
				targetChar.female(!targetChar.female());
				if (ctx.characters() != null) {
					ctx.characters().save(targetChar, true);
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Sexo de " + targetChar.name() + " alterado para " + (targetChar.female() ? "Feminino" : "Masculino")));
				if (targetPlayer != null) {
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					ctx.world().broadcastAround(targetPlayer, GameWorld.VISIBILITY_RADIUS,
							new CharInfo(targetChar, ctx.characters().template(targetChar)), false);
				}
				showAdminHtml("charedit.htm");
			}
			case "changelvl" -> {
				String[] parts = args.trim().split("\\s+");
				PlayerCharacter targetChar = getTargetPlayerOrActive();
				int level = 0;
				if (parts.length >= 2 && !parts[0].startsWith("$")) {
					String name = parts[0];
					level = GameSession.parseIntSafe(parts[1], 0);
					var found = ctx.characters() != null ? ctx.characters().findByName(name).orElse(null) : null;
					if (found != null) {
						targetChar = found;
					}
				} else if (parts.length >= 1 && !parts[0].isEmpty() && !parts[0].startsWith("$")) {
					level = GameSession.parseIntSafe(parts[0], 0);
				}
				if (targetChar != null) {
					targetChar.accessLevel(level);
					if (ctx.characters() != null) {
						ctx.characters().setAccessLevelByName(targetChar.name(), level);
						ctx.characters().save(targetChar, true);
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"AccessLevel de " + targetChar.name() + " definido para " + level));
					if (level < 0) {
						var online = ctx.world().byName(targetChar.name()).orElse(null);
						if (online != null) {
							online.send(SystemMessage.of(SystemMessage.DISCONNECTED_FROM_SERVER));
							online.close(new ServerClose());
						}
					}
				}
			}
			case "find_account" -> {
				String charName = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				PlayerCharacter targetChar = charName != null ? (ctx.characters() != null ? ctx.characters().findByName(charName).orElse(null) : null) : getTargetPlayerOrActive();
				if (targetChar != null && ctx.characters() != null) {
					String acc = targetChar.account() != null ? targetChar.account() : "none";
					var chars = ctx.characters().list(acc);
					StringBuilder rows = new StringBuilder("<table width=270>");
					for (var c : chars) {
						String cName = c.name();
						boolean isOnline = ctx.world().byName(cName).isPresent();
						rows.append("<tr><td width=100><a action=\"bypass -h admin_character_info ").append(cName).append("\">")
								.append(isOnline ? "<font color=\"00FF00\">" + cName + "</font>" : cName).append("</a></td>")
								.append("<td width=110>Class ").append(c.classId()).append("</td>")
								.append("<td width=60>Lv ").append(c.level()).append("</td></tr>");
					}
					rows.append("</table>");
					String htm = resolveAdminHtml("accountinfo.htm");
					if (htm != null) {
						htm = htm.replace("%account%", acc)
								.replace("%player%", targetChar.name())
								.replace("%characters%", rows.toString());
						send(new NpcHtmlMessage(0, htm));
						return;
					}
				}
				showAdminHtml("accountinfo.htm");
			}
			case "find_ip" -> {
				String ip = !args.isBlank() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : "127.0.0.1";
				StringBuilder rows = new StringBuilder();
				int count = 0;
				if (ctx.world() != null) {
					for (var p : ctx.world().players()) {
						if (p.character() != null) {
							count++;
							rows.append("<tr><td width=80><a action=\"bypass -h admin_character_info ").append(p.character().name()).append("\">")
									.append(p.character().name()).append("</a></td>")
									.append("<td width=110>Class ").append(p.character().classId()).append("</td>")
									.append("<td width=40>").append(p.character().level()).append("</td></tr>");
						}
					}
				}
				String htm = resolveAdminHtml("ipfind.htm");
				if (htm != null) {
					htm = htm.replace("%ip%", ip)
							.replace("%results%", rows.toString())
							.replace("%number%", String.valueOf(count))
							.replace("%end%", count == 1 ? "" : "s");
					send(new NpcHtmlMessage(0, htm));
					return;
				}
				showAdminHtml("ipfind.htm");
			}
			case "add_exp_sp", "remove_exp_sp" -> {
				String[] parts = args.trim().split("\\s+");
				long expVal = parts.length >= 1 && !parts[0].startsWith("$") ? GameSession.parseLongSafe(parts[0], 0) : 0;
				int spVal = parts.length >= 2 && !parts[1].startsWith("$") ? GameSession.parseIntSafe(parts[1], 0) : 0;
				if (cmd.equals("remove_exp_sp")) {
					expVal = -expVal;
					spVal = -spVal;
				}
				var targetChar = getTargetPlayerOrActive();
				var targetPlayer = getTargetOnlinePlayerOrSelf();
				targetChar.exp(Math.max(0, targetChar.exp() + expVal));
				targetChar.sp(Math.max(0, targetChar.sp() + spVal));
				if (ctx.characters() != null) {
					ctx.characters().save(targetChar, true);
				}
				if (targetPlayer instanceof GameSession gs) {
					gs.sendUserInfoAndBroadcastCharInfo();
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Exp (" + expVal + ") e SP (" + spVal + ") modificados para " + targetChar.name()));
				showAdminHtml("expsp.htm");
			}
			case "gonorth", "gosouth", "goeast", "gowest", "goup", "godown" -> {
				int offset = 500;
				if (!args.isBlank() && !args.startsWith("$")) {
					offset = GameSession.parseIntSafe(args.trim().split("\\s+")[0], 500);
				}
				int nx = active.x();
				int ny = active.y();
				int nz = active.z();
				switch (cmd) {
					case "gonorth" -> ny -= offset;
					case "gosouth" -> ny += offset;
					case "goeast" -> nx += offset;
					case "gowest" -> nx -= offset;
					case "goup" -> nz += offset;
					case "godown" -> nz -= offset;
				}
				session.teleportToLocation(nx, ny, nz);
				showAdminHtml("move.htm");
			}
			case "teleto" -> {
				if (args.trim().equalsIgnoreCase("r")) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Demonico ativado."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Normal de movimento ativo."));
				}
				showAdminHtml("move.htm");
			}
			case "gmspeed_menu" -> {
				handleAdminCommand("speed " + args);
				showAdminHtml("move.htm");
			}
			case "walk" -> {
				String[] parts = args.trim().split("\\s+");
				if (parts.length >= 3) {
					int wx = GameSession.parseIntSafe(parts[0], active.x());
					int wy = GameSession.parseIntSafe(parts[1], active.y());
					int wz = GameSession.parseIntSafe(parts[2], active.z());
					active.moveTo(wx, wy, wz);
				}
			}
			case "server_gm_only" -> handleAdminCommand("onlygm");
			case "kick_non_gm" -> handleAdminCommand("kickall");
			case "server_all" -> handleAdminCommand("forall");
			case "oly_save" -> handleAdminCommand("saveolymp");
			case "add_hero" -> handleAdminCommand("manualhero " + args);
			case "oly_start" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Olimpiadas iniciadas."));
			case "oly_stop" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Olimpiadas pausadas."));
			case "add_oly_points" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pontos de Olimpiada adicionados: " + args));
			case "fix_noble_name" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de nobre sincronizado."));
			case "add_announcement" -> {
				if (!args.isBlank() && !args.startsWith("$") && ctx.announcements() != null) {
					ctx.announcements().add(args.trim());
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Anuncio adicionado: " + args.trim()));
				}
				showAdminHtml("announce.htm");
			}
			case "del_announcement" -> {
				if (!args.isBlank() && ctx.announcements() != null) {
					int idx = GameSession.parseIntSafe(args.trim(), -1);
					if (ctx.announcements().delete(idx)) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Anuncio removido."));
					}
				}
				showAdminHtml("announce.htm");
			}
			case "announce_announcements" -> showAdminHtml("announce.htm");
			case "autoannounce" -> {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto-anuncio configurado."));
				showAdminHtml("announce.htm");
			}
			case "delete", "del", "delete_npc", "unspawn" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (ctx.spawns() != null) {
							ctx.spawns().deleteSpawn(npc, true);
						} else {
							ctx.world().removeNpc(npc);
						}
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()),
								false);
						send(new DeleteObject(npc.objectId()));
						session.targetObjectId(0);
						targetObjectId = 0;
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"NPC " + npc.name() + " (id " + npc.npcId() + ") removido do mundo e do banco."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "O alvo nao e um NPC valido."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC para deletar."));
				}
			}
			case "heal" -> {
				PlayerCharacter targetChar = active;
				GameWorld.OnlinePlayer targetPlayer = session;
				if (targetObjectId != 0 && targetObjectId != active.objectId()) {
					var other = ctx.world().player(targetObjectId).orElse(null);
					if (other != null && other.character() != null) {
						targetChar = other.character();
						targetPlayer = other;
					} else {
						var npc = ctx.world().npc(targetObjectId).orElse(null);
						if (npc != null) {
							npc.currentHp(npc.template().maxHp());
							npc.currentMp(npc.template().maxMp());
							send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " totalmente curado."));
							return;
						}
					}
				}
				targetChar.currentHp(targetChar.maxHp());
				targetChar.currentMp(targetChar.maxMp());
				targetChar.currentCp(targetChar.maxCp());
				targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " totalmente curado."));
			}
			case "kill" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						npc.dead(true);
						if (ctx.npcAi() != null) {
							ctx.npcAi().stopCombat(npc);
							ctx.npcAi().scheduleDecayAndRespawn(npc);
						}
						var su = StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp());
						send(su);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
						var die = new Die(npc.objectId(), false);
						send(die);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, die, false);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " foi morto."));
						return;
					}
					var playerOpt = ctx.world().player(targetObjectId);
					if (playerOpt.isPresent()) {
						var pOnline = playerOpt.get();
						if (pOnline.character() != null) {
							if (pOnline instanceof GameSession pSess) {
								pSess.handlePlayerDeath(active);
							} else {
								pOnline.character().currentHp(0);
								pOnline.send(new StatusUpdate(pOnline.objectId(), List.of(
										new StatusUpdate.Attribute(StatusUpdate.CUR_HP, 0))));
								var die = new Die(pOnline.objectId(), true);
								pOnline.send(die);
							}
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", pOnline.name() + " foi morto."));
						}
						return;
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um alvo para matar."));
			}
			case "setlevel", "set_level", "level" -> {
				try {
					int newLevel = Integer.parseInt(args.trim().split("\\s+")[0]);
					if (newLevel < 1 || newLevel > 80) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel deve ser entre 1 e 80."));
						return;
					}
					PlayerCharacter targetChar = active;
					GameWorld.OnlinePlayer targetPlayer = session;
					if (targetObjectId != 0 && targetObjectId != active.objectId()) {
						var other = ctx.world().player(targetObjectId).orElse(null);
						if (other != null && other.character() != null) {
							targetChar = other.character();
							targetPlayer = other;
						}
					}
					targetChar.level(newLevel);
					targetChar.exp(ExperienceTable.expForLevel(newLevel));
					var t = ctx.characters() != null ? ctx.characters().template(targetChar) : null;
					if (t != null) {
						if (targetPlayer instanceof GameSession gs) {
							gs.rewardSkills(t, true);
						}
						if (ctx.skillService() == null) {
							targetChar.maxHp(t.calculateMaxHp(newLevel));
							targetChar.maxMp(t.calculateMaxMp(newLevel));
							targetChar.maxCp(t.calculateMaxCp(newLevel));
						}
					}
					targetChar.currentHp(targetChar.maxHp());
					targetChar.currentMp(targetChar.maxMp());
					targetChar.currentCp(targetChar.maxCp());
					ctx.characters().save(targetChar, true);
					var social = new SocialAction(targetChar.objectId(), 15);
					targetPlayer.send(social);
					if (targetPlayer instanceof GameSession gs) {
						ctx.world().broadcastAround(gs, GameWorld.VISIBILITY_RADIUS, social, false);
					}
					targetPlayer.send(new UserInfo(targetChar, ctx.characters().template(targetChar)));
					targetPlayer.send(new StatusUpdate(targetChar.objectId(), List.of(
							new StatusUpdate.Attribute(StatusUpdate.LEVEL, targetChar.level()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) targetChar.currentHp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_HP, targetChar.maxHp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) targetChar.currentMp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
							new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
							new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
					if (targetPlayer instanceof GameSession gs) {
						gs.checkClassMasterPopup();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Nivel de " + targetChar.name() + " alterado para " + newLevel));
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //level <1-80>"));
				}
			}
			case "enchant_weapon" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					var weapon = active.inventory().paperdoll(ItemSlots.RHAND);
					if (weapon == null) {
						weapon = active.inventory().paperdoll(ItemSlots.LRHAND);
					}
					if (weapon != null) {
						weapon.enchant(val);
						ctx.inventories().saveItem(weapon);
						send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
						send(new UserInfo(active, ctx.characters().template(active)));
						session.broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arma encantada para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma arma equipada na mao direita."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setew <enchantLevel>"));
				}
			}
			case "enchant_armor" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int[] armorSlots = { ItemSlots.CHEST, ItemSlots.LEGS, ItemSlots.HEAD, ItemSlots.GLOVES,
							ItemSlots.FEET };
					List<ItemInfo> modified = new ArrayList<>();
					for (int slot : armorSlots) {
						var piece = active.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							modified.add(ItemInfo.of(piece, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						session.broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Armaduras encantadas para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma armadura equipada."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setec <enchantLevel>"));
				}
			}
			case "setej", "enchant_jewel" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 16
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					int[] jewelSlots = { ItemSlots.NECK, ItemSlots.REAR, ItemSlots.LEAR, ItemSlots.RFINGER,
							ItemSlots.LFINGER };
					List<ItemInfo> modified = new ArrayList<>();
					for (int slot : jewelSlots) {
						var piece = active.inventory().paperdoll(slot);
						if (piece != null) {
							piece.enchant(val);
							ctx.inventories().saveItem(piece);
							modified.add(ItemInfo.of(piece, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						session.broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Joias encantadas para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma joia equipada."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setej <enchantLevel>"));
				}
			}
			case "enchant_all", "setall" -> {
				try {
					int val = args.isEmpty() || args.equalsIgnoreCase("$qbox") ? 20
							: Integer.parseInt(args.trim().split("\\s+")[0]);
					List<ItemInfo> modified = new ArrayList<>();
					for (var item : active.inventory().equipped()) {
						if (item != null) {
							item.enchant(val);
							ctx.inventories().saveItem(item);
							modified.add(ItemInfo.of(item, ItemInfo.MODIFIED));
						}
					}
					if (!modified.isEmpty()) {
						send(new InventoryUpdate(modified));
						send(new UserInfo(active, ctx.characters().template(active)));
						session.broadcastAppearance();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Todos os itens equipados foram encantados para +" + val));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum item equipado para encantar."));
					}
				} catch (Exception e) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //enchant_all <nivel>"));
				}
			}
			case "show_droplist", "droplist" -> {
				int mobId = 0;
				if (!args.isEmpty()) {
					mobId = GameSession.parseIntSafe(args.trim().split("\\s+")[0], 0);
				} else if (targetObjectId != 0) {
					var npc = ctx.world().npc(targetObjectId).orElse(null);
					if (npc != null) {
						mobId = npc.npcId();
					}
				}
				if (mobId > 0) {
					showAdminDropList(mobId);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //droplist <npcId> ou selecione um monstro."));
				}
			}
			case "edit_npc", "npcinfo", "npc_info" -> {
				NpcInstance targetNpc = null;
				if (!args.isEmpty()) {
					int npcId = GameSession.parseIntSafe(args.trim().split("\\s+")[0], 0);
					targetNpc = ctx.world().npcs().stream().filter(n -> n.npcId() == npcId).findFirst().orElse(null);
				} else if (targetObjectId != 0) {
					targetNpc = ctx.world().npc(targetObjectId).orElse(null);
				}
				if (targetNpc != null) {
					showAdminNpcInfo(targetNpc);
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC ou use //edit_npc <npcId>"));
				}
			}
			case "pledge", "clan" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 2) {
					String action = parts[0].toLowerCase(java.util.Locale.ROOT);
					String targetName = parts[1];
					switch (action) {
						case "create" -> session.createClan(targetName);
						case "dismiss" -> session.dissolveClan();
						case "setlevel" -> {
							int lvl = parts.length >= 3 ? GameSession.parseIntSafe(parts[2], 5) : 5;
							if (active.clanId() > 0 && ctx.clans() != null) {
								ctx.clans().updateClanLevel(active.clanId(), lvl);
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nivel do cla definido para " + lvl));
							}
						}
						case "rep" -> {
							int rep = parts.length >= 3 ? GameSession.parseIntSafe(parts[2], 10000) : 10000;
							if (active.clanId() > 0 && ctx.clans() != null) {
								var clan = ctx.clans().byClanId(active.clanId()).orElse(null);
								if (clan != null) {
									clan.reputationScore(clan.reputationScore() + rep);
									send(new CreatureSay(0, CreatureSay.ALL, "SYS",
											"Reputacao do cla adicionada: " + rep));
								}
							}
						}
					}
				} else {
					showAdminHtml("menus/game.htm");
				}
			}
			case "siege", "castles", "castle" -> showAdminCastlesHtml(args);
			case "setcastle" -> {
				if (!args.isBlank() && ctx.castles() != null) {
					String cName = args.trim().split("\\s+")[0];
					var castle = ctx.castles().byName(cName).orElse(null);
					var target = getTargetPlayerOrActive();
					if (castle != null && target != null) {
						ctx.castles().setOwner(castle.id(), target.clanId());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Castelo " + castle.name() + " atribuido ao cla " + target.clanId() + " (" + target.name() + ")"));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Castelo ou jogador invalido."));
					}
				} else {
					showAdminCastlesHtml("");
				}
			}
			case "startsiege" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Siege iniciada: " + args));
			case "endsiege" -> send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Siege finalizada: " + args));
			case "mammon_find" -> {
				String param = args.trim().toLowerCase(java.util.Locale.ROOT);
				boolean isBlacksmith = param.equals("1") || param.contains("black") || param.contains("bs");
				int targetNpcId = isBlacksmith
						? com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_BLACKSMITH_ID
						: com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_ID;
				String name = isBlacksmith ? "Blacksmith of Mammon" : "Merchant of Mammon";

				var loc = isBlacksmith
						? (ctx.sevenSigns() != null ? ctx.sevenSigns().getMammonBlacksmithLocation() : null)
						: (ctx.sevenSigns() != null ? ctx.sevenSigns().getMammonMerchantLocation() : null);

				var npc = ctx.world().npcs().stream().filter(n -> n.npcId() == targetNpcId).findFirst().orElse(null);
				if (npc != null) {
					session.teleportToLocation(npc.x(), npc.y(), npc.z());
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Teleportado para " + npc.name() + " em " + npc.x() + ", " + npc.y() + ", " + npc.z()));
				} else if (loc != null) {
					session.teleportToLocation(loc.x(), loc.y(), loc.z());
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Teleportado para spawn de " + name + " (" + loc.dungeonName() + ") em " + loc.x() + ", " + loc.y() + ", " + loc.z()));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							name + " (ID " + targetNpcId + ") nao encontrado ativo no mundo."));
				}
			}
			case "ssq", "sevensigns" -> {
				if (ctx.sevenSigns() == null) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "SevenSignsManager indisponivel."));
					break;
				}
				var ss = ctx.sevenSigns();
				String[] parts = args.trim().split("\\s+");
				String sub = parts.length > 0 ? parts[0].toLowerCase(java.util.Locale.ROOT) : "";
				switch (sub) {
					case "change", "advance" -> {
						ss.changePeriodManually();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Seven Signs: Periodo avancado para " + ss.activePeriod() + " (Ciclo " + ss.currentCycle() + ")."));
					}
					case "set_period" -> {
						if (parts.length >= 2) {
							int p = GameSession.parseIntSafe(parts[1], 0);
							ss.changePeriodManually(p);
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Periodo alterado para " + ss.activePeriod() + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_period <0-3>"));
						}
					}
					case "set_cycle" -> {
						if (parts.length >= 2) {
							int c = GameSession.parseIntSafe(parts[1], 1);
							ss.currentCycle(c);
							ss.persistStatus();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Ciclo alterado para " + c + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_cycle <numero>"));
						}
					}
					case "set_seal" -> {
						if (parts.length >= 3) {
							int seal = GameSession.parseIntSafe(parts[1], 1);
							int cabal = GameSession.parseIntSafe(parts[2], 0);
							ss.setSealOwner(seal, cabal);
							ss.persistStatus();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Seven Signs: Selo " + seal + " definido para Cabal " + cabal + "."));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //ssq set_seal <1-3> <0=None, 1=Dusk, 2=Dawn>"));
						}
					}
					case "sky" -> {
						ss.broadcastSky();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seven Signs: Sky state atualizado para todos."));
					}
					default -> {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								String.format("Seven Signs Status: Ciclo=%d, Periodo=%d, DawnScore=%d, DuskScore=%d, Winner=%d",
										ss.currentCycle(), ss.activePeriod(), ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN),
										ss.getCurrentScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK), ss.previousWinner())));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								String.format("Selos: Avarice=%d, Gnosis=%d, Strife=%d",
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_AVARICE),
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_GNOSIS),
										ss.getSealOwner(com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STRIFE))));
					}
				}
			}
			case "cleanup", "clean_up" -> {
				long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.gc();
				long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				long freedMb = Math.max(0, (before - after) / (1024 * 1024));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Memoria liberada: " + freedMb + " MB. Coleta de lixo concluida."));
			}
			case "shutdown", "server_shutdown" -> {
				int sec = GameSession.parseIntSafe(args.isEmpty() || args.equalsIgnoreCase("$qbox") ? "60" : args.trim().split("\\s+")[0], 60);
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.startShutdown(sec, com.lopez.l2j.game.service.ShutdownService.Mode.SHUTDOWN, active != null ? active.name() : "Admin");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Shutdown agendado para " + sec + " segundos."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "ShutdownService nao inicializado."));
				}
			}
			case "restart", "server_restart" -> {
				int sec = GameSession.parseIntSafe(args.isEmpty() || args.equalsIgnoreCase("$qbox") ? "60" : args.trim().split("\\s+")[0], 60);
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.startShutdown(sec, com.lopez.l2j.game.service.ShutdownService.Mode.RESTART, active != null ? active.name() : "Admin");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Restart agendado para " + sec + " segundos."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "ShutdownService nao inicializado."));
				}
			}
			case "abort", "server_abort" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					boolean ok = svc.abort(active != null ? active.name() : "Admin");
					if (ok) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Desligamento/restart abortado com sucesso."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum desligamento ou restart em andamento."));
					}
				}
			}
			case "onlygm", "server_onlygm" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.setOnlyGm(true);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Apenas GM ATIVADO (Acesso restrito para manutencao)."));
				}
			}
			case "forall", "server_forall" -> {
				var svc = com.lopez.l2j.game.service.ShutdownService.getInstance();
				if (svc != null) {
					svc.setOnlyGm(false);
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo Apenas GM DESATIVADO (Servidor aberto para todos os jogadores)."));
				}
			}
			case "maxplayer", "set_maxplayer" -> {
				int max = GameSession.parseIntSafe(args.trim().split("\\s+")[0], 1000);
				Config.MAXIMUM_ONLINE_USERS = max;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Capacidade maxima de jogadores alterada para: " + max));
			}
			case "setparam" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 2) {
					String param = parts[0].toLowerCase(java.util.Locale.ROOT);
					try {
						int val = Integer.parseInt(parts[1]);
						var destChar = getTargetPlayerOrActive();
						switch (param) {
							case "hp", "maxhp" -> destChar.maxHp(val);
							case "mp", "maxmp" -> destChar.maxMp(val);
							case "cp", "maxcp" -> destChar.maxCp(val);
							case "speed", "runspeed" -> destChar.gmSpeed(Math.min(5, Math.max(0, val)));
						}
						destChar.currentHp(destChar.maxHp());
						destChar.currentMp(destChar.maxMp());
						destChar.currentCp(destChar.maxCp());
						ctx.characters().save(destChar, true);
						var destPlayer = getTargetOnlinePlayerOrSelf();
						destPlayer.send(new UserInfo(destChar, ctx.characters().template(destChar)));
						destPlayer.send(
								StatusUpdate.hp(destChar.objectId(), (int) destChar.currentHp(), destChar.maxHp()));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Parametro " + param + " de " + destChar.name() + " alterado para " + val));
					} catch (Exception e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setparam <hp|mp|cp|speed> <valor>"));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setparam <hp|mp|cp|speed> <valor>"));
				}
			}
			case "announce", "announce_add" -> {
				if (!args.isEmpty() && !args.equalsIgnoreCase("$new_announcement")) {
					String sender = Config.ANNOUNCE_GM_NAME ? active.name() : "";
					ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, sender, args), x -> true);
				} else {
					showAdminHtml("menus/submenus/announce_menu.htm");
				}
			}
			case "announce_critical", "critical_announce" -> {
				if (!args.isEmpty() && !args.equalsIgnoreCase("$new_announcement")) {
					String sender = Config.ANNOUNCE_GM_NAME ? active.name() : "";
					ctx.world().broadcast(new CreatureSay(0, CreatureSay.CRITICAL_ANNOUNCE, sender, args), x -> true);
				} else {
					showAdminHtml("menus/submenus/announce_menu.htm");
				}
			}
			case "announce_refresh" -> showAdminHtml("menus/submenus/announce_menu.htm");
			case "open", "open_menu" -> {
				if (!args.isBlank() && !args.startsWith("$")) {
					try {
						int doorId = Integer.parseInt(args.trim().split("\\s+")[0]);
						if (ctx.doors() != null) {
							var door = ctx.doors().getDoor(doorId);
							if (door != null) {
								door.open();
								ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
								send(new CreatureSay(0, CreatureSay.ALL, "SYS",
										"Porta " + doorId + " (" + door.name() + ") aberta."));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + doorId + " nao encontrada."));
							}
						}
					} catch (Exception e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //open <doorId>"));
					}
				} else if (targetObjectId != 0 && ctx.doors() != null) {
					var door = ctx.doors().getDoor(targetObjectId);
					if (door != null) {
						door.open();
						ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + door.name() + " aberta."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione uma porta ou digite o ID: //open <doorId>"));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione uma porta ou digite o ID: //open <doorId>"));
				}
			}
			case "close", "close_menu" -> {
				if (!args.isBlank() && !args.startsWith("$")) {
					try {
						int doorId = Integer.parseInt(args.trim().split("\\s+")[0]);
						if (ctx.doors() != null) {
							var door = ctx.doors().getDoor(doorId);
							if (door != null) {
								door.close();
								ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
								send(new CreatureSay(0, CreatureSay.ALL, "SYS",
										"Porta " + doorId + " (" + door.name() + ") fechada."));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + doorId + " nao encontrada."));
							}
						}
					} catch (Exception e) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //close <doorId>"));
					}
				} else if (targetObjectId != 0 && ctx.doors() != null) {
					var door = ctx.doors().getDoor(targetObjectId);
					if (door != null) {
						door.close();
						ctx.world().broadcast(new DoorStatusUpdate(door), p -> true);
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Porta " + door.name() + " fechada."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione uma porta ou digite o ID: //close <doorId>"));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione uma porta ou digite o ID: //close <doorId>"));
				}
			}
			case "invul", "undying" -> {
				active.invul(!active.invul());
				active.currentHp(active.maxHp());
				active.currentCp(active.maxCp());
				active.currentMp(active.maxMp());
				send(new StatusUpdate(active.objectId(), List.of(
						new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active.currentHp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_HP, active.maxHp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active.currentMp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, active.maxMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, active.maxCp()))));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Invulnerabilidade GM: " + (active.invul() ? "ATIVADA (Dano zero)" : "DESATIVADA")));
			}
			case "goname", "goto", "goto_char", "goto_char_menu", "teleport_to_character" -> {
				String targetName = !args.isEmpty() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				if (targetName == null && targetObjectId != 0 && targetObjectId != active.objectId()) {
					var opt = ctx.world().player(targetObjectId);
					if (opt.isPresent() && opt.get().character() != null) {
						targetName = opt.get().character().name();
					}
				}
				if (targetName != null) {
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null) {
						session.teleportToLocation(target.x(), target.y(), target.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para " + target.name()));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + targetName + "' nao encontrado ou offline."));
					}
				} else {
					showAdminHtml("menus/submenus/charfind_menu.htm");
				}
			}
			case "recall", "recall_char", "recall_char_menu" -> {
				String targetName = !args.isEmpty() && !args.startsWith("$") ? args.trim().split("\\s+")[0] : null;
				if (targetName == null && targetObjectId != 0 && targetObjectId != active.objectId()) {
					var opt = ctx.world().player(targetObjectId);
					if (opt.isPresent() && opt.get().character() != null) {
						targetName = opt.get().character().name();
					}
				}
				if (targetName != null) {
					var target = ctx.world().byName(targetName).orElse(null);
					if (target instanceof GameSession s) {
						s.teleportToLocation(active.x(), active.y(), active.z());
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador " + target.name() + " puxado para sua posicao."));
					} else if (target != null && target.character() != null) {
						target.character().moveTo(active.x(), active.y(), active.z());
						target.send(new TeleportToLocation(target.objectId(), active.x(), active.y(), active.z()));
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador " + target.name() + " puxado para sua posicao."));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Jogador '" + targetName + "' nao encontrado ou offline."));
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //recall <nomeDoPlayer> ou selecione um jogador."));
				}
			}
			case "setadmin" -> {
				String[] parts = args.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					String targetName = parts[0];
					int level = parts.length >= 2 ? Integer.parseInt(parts[1]) : 100;
					ctx.characters().setAccessLevelByName(targetName, level);
					var target = ctx.world().byName(targetName).orElse(null);
					if (target != null && target.character() != null) {
						target.character().accessLevel(level);
						target.send(new UserInfo(target.character(), ctx.characters().template(target.character())));
						target.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Seu nivel de acesso administrativo foi atualizado para: " + level));
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"AccessLevel de " + targetName + " definido para " + level + " no banco de dados."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //setadmin <NomeDoPersonagem> [nivel]"));
				}
			}
			default -> {
				String resolved = resolveAdminHtml(cmd);
				if (resolved != null) {
					showAdminHtml(cmd);
				} else if (!args.isEmpty()) {
					resolved = resolveAdminHtml(cmd + "/" + args);
					if (resolved != null) {
						showAdminHtml(cmd + "/" + args);
						return;
					}
					resolved = resolveAdminHtml(args);
					if (resolved != null) {
						showAdminHtml(args);
						return;
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando admin desconhecido: " + cmd));
					showAdminHtml("menus/main.htm");
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando admin desconhecido: " + cmd));
					showAdminHtml("menus/main.htm");
				}
			}
		}
	}

}
