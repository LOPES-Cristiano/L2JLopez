package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Servico central de seguranca, controle de acesso hierarquico (RBAC) e auditoria (GMAudit)
 * para comandos e paineis administrativos.
 */
@Service
public class AdminSecurityService {

	private static final Logger log = LoggerFactory.getLogger(AdminSecurityService.class);

	public static final int LEVEL_HELPER = 1;
	public static final int LEVEL_EVENT_GM = 50;
	public static final int LEVEL_ADMIN = 80;
	public static final int LEVEL_MASTER = 100;

	public static final int MAX_AUDIT_ENTRIES = 200;
	public static final int MAX_ENCHANT_LEVEL = 65535;
	public static final int MAX_STACKABLE_ITEM_COUNT = 1_000_000_000;
	public static final int MAX_NON_STACKABLE_ITEM_COUNT = 20;

	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private static volatile AdminSecurityService instance;

	public static AdminSecurityService getInstance() {
		AdminSecurityService result = instance;
		if (result == null) {
			synchronized (AdminSecurityService.class) {
				result = instance;
				if (result == null) {
					result = new AdminSecurityService();
					instance = result;
				}
			}
		}
		return result;
	}

	public record AuditEntry(
			long timestamp,
			String gmName,
			String account,
			String ip,
			int accessLevel,
			String command,
			String args,
			String targetName
	) {}

	private static final Set<String> MASTER_COMMANDS = Set.of(
			"setadmin", "changelvl",
			"shutdown", "server_shutdown",
			"restart", "server_restart",
			"abort", "server_abort",
			"reload", "spawn_reload", "cw_reload",
			"setparam",
			"onlygm", "server_onlygm", "server_gm_only",
			"forall", "server_forall", "server_all",
			"maxplayer", "set_maxplayer",
			"cleanup", "clean_up",
			"set_mod"
	);

	private static final Set<String> ADMIN_COMMANDS = Set.of(
			"setlevel", "set_level", "level",
			"setclass", "set_class", "set_char_class", "charclasses", "charclasses_menu",
			"subclass", "subclasses", "sub_class", "sub_classes",
			"addsubclass", "add_subclass", "setsubclass", "set_subclass",
			"delsubclass", "del_subclass", "removesubclass",
			"switchsubclass", "switch_subclass",
			"skill", "setskill", "add_skill", "add_skill_id",
			"removeskill", "del_skill", "remove_skill", "remove_skill_id",
			"give_all_skills", "remove_all_skills",
			"enchant", "enchant_menu", "enchant_weapon", "enchant_armor", "enchant_jewel",
			"enchant_all", "setall",
			"seteh", "seteg", "seteb", "setel", "setes", "seten", "setre", "setle",
			"setrf", "setlf", "setun", "setba", "setec", "setew", "setej",
			"itemcreate", "itemcreation", "itemcreation_menu",
			"create_item", "create_item_id", "item", "give_item", "give_item_target",
			"give_target_item_id", "give_item_to_all", "mass_create",
			"clean_inventory", "clear_inventory",
			"ban", "banchar", "ban_menu",
			"kickall", "kick_all", "kick_non_gm",
			"siege", "admin_siege", "castles", "castle", "setcastle", "startsiege", "endsiege",
			"fortsiege", "fort", "forts",
			"clanhall", "clanhallopendoors", "clanhallclosedoors", "clanhallset", "clanhalldel", "clanhallteleportself",
			"ssq", "sevensigns", "mammon_respawn",
			"cw_add", "cw_remove", "cwinfo", "cw_info_menu",
			"saveolymp", "oly_save", "endolympiad", "oly_start", "oly_stop", "add_oly_points",
			"setnoble", "noble", "fix_noble_name",
			"clanfull", "pledge", "clan",
			"add_exp_sp", "remove_exp_sp", "expsp", "expsp_menu", "add_exp_sp_to_character",
			"setkarma", "setpk", "setpvp", "changename", "changename_menu", "setname",
			"settitle", "setsex", "setcolor", "rec", "save_modifications",
			"charedit", "charedit_menu", "edit_char", "edit_character", "set", "set_menu",
			"sethero", "set_hero", "removehero", "remove_hero", "delhero", "del_hero",
			"set_masshero", "setmasshero", "masshero", "mass_hero", "manualhero", "add_hero",
			"setvip", "set_vip", "removevip", "remove_vip", "delvip", "del_vip",
			"set_massvip", "setmassvip", "massvip", "mass_vip",
			"setaio", "set_aio", "removeaio", "remove_aio", "delaio", "del_aio",
			"set_massaio", "setmassaio", "massaio", "mass_aio",
			"epic", "admin_epic", "phantom", "admin_phantom",
			"unspawnall", "respawnall",
			"buy", "gmshop", "adminshop", "gmshops", "gmmenu"
	);

	private static final Set<String> EVENT_GM_COMMANDS = Set.of(
			"heal", "kill", "kill_menu", "res", "resurrect",
			"tvt", "admin_tvt", "ctf", "admin_ctf", "dm", "admin_dm", "oly", "admin_oly",
			"spawn", "spawn_monster", "spawn_once", "cspawn", "spawn_id",
			"spawn_menu", "spawnmenu", "show_spawns", "spawns", "show_npcs", "spawn_index",
			"spawnnight", "spawnday",
			"delete", "del", "delete_npc", "unspawn",
			"open", "open_menu", "close", "close_menu", "openall", "closeall",
			"announce", "announce_add", "announce_critical", "critical_announce",
			"announce_menu", "announce_refresh", "add_announcement", "del_announcement",
			"announce_announcements", "autoannounce",
			"kick", "kick_menu", "jail", "unjail", "banchat", "unbanchat",
			"para", "para_menu", "unpara", "unpara_menu", "para_all", "para_all_menu", "unpara_all", "unpara_all_menu",
			"cancel", "dispel", "nokarma", "clearkarma",
			"abnormal", "abnormal_menu", "abnormalmenu", "effect_menu",
			"social", "social_menu", "socialmenu",
			"polymorph", "polymorph_menu", "polyself", "polyself_menu",
			"unpoly", "unpolymorph", "unpolymorph_menu", "unpolyself", "unpolyself_menu",
			"atmosphere", "atmosphere_menu", "earthquake", "earthquake_menu",
			"setteam1", "setteam2", "clearteam", "rangesetteam1", "rangesetteam2", "rangeclearteam",
			"recall_npc", "recall_party_menu", "recall_clan_menu", "recallparty", "recallclan",
			"teleport_character_to_menu"
	);

	private final ConcurrentLinkedDeque<AuditEntry> auditBuffer = new ConcurrentLinkedDeque<>();

	public AdminSecurityService() {
		instance = this;
	}

	/**
	 * Normaliza o nome do comando administrativo, removendo barras, prefixo "admin_" e argumentos.
	 */
	public static String normalizeCommand(String rawCmd) {
		if (rawCmd == null) {
			return "admin";
		}
		String clean = rawCmd.trim();
		while (clean.startsWith("/")) {
			clean = clean.substring(1).trim();
		}
		if (clean.toLowerCase(Locale.ROOT).startsWith("admin_")) {
			clean = clean.substring(6).trim();
		} else if (clean.toLowerCase(Locale.ROOT).startsWith("admin ")) {
			clean = clean.substring(6).trim();
		}
		int spaceIdx = clean.indexOf(' ');
		if (spaceIdx > 0) {
			clean = clean.substring(0, spaceIdx).trim();
		}
		return clean.isEmpty() ? "admin" : clean.toLowerCase(Locale.ROOT);
	}

	/**
	 * Retorna o nivel minimo de AccessLevel exigido para executar o comando.
	 */
	public int requiredLevelForCommand(String cmd) {
		String norm = normalizeCommand(cmd);
		if (MASTER_COMMANDS.contains(norm)) {
			return LEVEL_MASTER;
		}
		if (ADMIN_COMMANDS.contains(norm)) {
			return LEVEL_ADMIN;
		}
		if (EVENT_GM_COMMANDS.contains(norm)) {
			return LEVEL_EVENT_GM;
		}
		return LEVEL_HELPER;
	}

	/**
	 * Verifica se a sessao possui permissao para executar o comando informado.
	 * NUNCA concede GM apenas porque a conta/nome comeca com "admin" ou "GM".
	 */
	public boolean canExecute(GameSession session, String cmd) {
		if (session == null || session.activeChar() == null) {
			return false;
		}
		PlayerCharacter active = session.activeChar();
		if (active.accessLevel() <= 0) {
			boolean everyoneAdmin = Config.getBoolean("EveryoneHasAdminRights", false)
					|| Config.getBoolean("EveryoneIsGM", false);
			boolean explicitSuperuser = isConfiguredSuperuser(session, active);
			if (everyoneAdmin || explicitSuperuser) {
				active.accessLevel(LEVEL_MASTER);
				log.info("[GM-SECURITY] Elevacao explicita autorizada para char='{}' (acc='{}', ip='{}') -> Level {}",
						active.name(), session.account(), session.ip(), LEVEL_MASTER);
			} else {
				log.warn("[GM-SECURITY] Tentativa bloqueada de comando admin sem privilegio: char='{}' (acc='{}', ip='{}', lvl={}) cmd='{}'",
						active.name(), session.account(), session.ip(), active.accessLevel(), cmd);
				return false;
			}
		}
		int required = requiredLevelForCommand(cmd);
		if (active.accessLevel() < required) {
			log.warn("[GM-SECURITY] AccessLevel insuficiente: char='{}' (acc='{}', ip='{}', lvl={}) tentou executar cmd='{}' (req={})",
					active.name(), session.account(), session.ip(), active.accessLevel(), cmd, required);
			return false;
		}
		return true;
	}

	private boolean isConfiguredSuperuser(GameSession session, PlayerCharacter active) {
		var ctx = session.context();
		if (ctx == null || ctx.adminSuperusers() == null || ctx.adminSuperusers().isEmpty()) {
			return false;
		}
		String account = session.account();
		String charName = active.name();
		for (String entry : ctx.adminSuperusers()) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			String trimmed = entry.trim();
			if ((account != null && trimmed.equalsIgnoreCase(account))
					|| (charName != null && trimmed.equalsIgnoreCase(charName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Impede que um GM de nivel inferior puna ou altere status de um GM de nivel superior.
	 */
	public boolean canTargetPlayer(PlayerCharacter gm, PlayerCharacter target) {
		if (gm == null || !gm.isGm()) {
			return false;
		}
		if (target == null || gm.objectId() == target.objectId()) {
			return true;
		}
		return gm.accessLevel() >= target.accessLevel();
	}

	/**
	 * Registra uma acao administrativa no log e no buffer circular em memoria (max 200 entradas).
	 */
	public void recordAudit(GameSession session, String cmd, String args) {
		String targetName = resolveTargetName(session);
		recordAudit(session, cmd, args, targetName);
	}

	public void recordAudit(GameSession session, String cmd, String args, String targetName) {
		if (session == null || session.activeChar() == null) {
			return;
		}
		PlayerCharacter gm = session.activeChar();
		String gmName = gm.name() != null ? gm.name() : "Unknown";
		String acc = session.account() != null ? session.account() : (gm.account() != null ? gm.account() : "none");
		String ip = session.ip() != null ? session.ip() : "unknown";
		int lvl = gm.accessLevel();
		String safeCmd = normalizeCommand(cmd);
		String safeArgs = args != null ? args.trim() : "";
		String safeTarget = (targetName != null && !targetName.isBlank()) ? targetName.trim() : resolveTargetName(session);

		log.info("[GM-AUDIT] GM={} (acc={}, ip={}, lvl={}) cmd='{}' args='{}' target='{}'",
				gmName, acc, ip, lvl, safeCmd, safeArgs, safeTarget);

		AuditEntry entry = new AuditEntry(
				System.currentTimeMillis(),
				gmName,
				acc,
				ip,
				lvl,
				safeCmd,
				safeArgs,
				safeTarget
		);
		auditBuffer.addFirst(entry);
		while (auditBuffer.size() > MAX_AUDIT_ENTRIES) {
			auditBuffer.pollLast();
		}
	}

	private String resolveTargetName(GameSession session) {
		if (session == null) {
			return "None";
		}
		PlayerCharacter active = session.activeChar();
		int targetObjId = session.targetObjectId();
		if (targetObjId == 0) {
			return active != null ? active.name() : "None";
		}
		if (active != null && targetObjId == active.objectId()) {
			return active.name();
		}
		var ctx = session.context();
		if (ctx != null && ctx.world() != null) {
			var p = ctx.world().player(targetObjId).orElse(null);
			if (p != null && p.character() != null) {
				return p.character().name();
			}
			var npc = ctx.world().npc(targetObjId).orElse(null);
			if (npc != null) {
				return "NPC:" + npc.name() + "(" + npc.npcId() + ")";
			}
		}
		return "Obj:" + targetObjId;
	}

	public List<AuditEntry> getAuditEntries() {
		return List.copyOf(auditBuffer);
	}

	/**
	 * Renderiza o painel HTML de auditoria (sem filtro).
	 */
	public String renderAuditHtml(int page) {
		return renderAuditHtml("", page);
	}

	/**
	 * Renderiza o painel HTML de auditoria com filtro opcional (por GM, comando ou alvo) e paginacao.
	 */
	public String renderAuditHtml(String filter, int page) {
		String safeFilter = (filter == null || filter.startsWith("$")) ? "" : filter.trim();
		String lowerFilter = safeFilter.toLowerCase(Locale.ROOT);

		List<AuditEntry> filtered = new ArrayList<>();
		for (AuditEntry e : auditBuffer) {
			if (lowerFilter.isEmpty()
					|| e.gmName().toLowerCase(Locale.ROOT).contains(lowerFilter)
					|| e.command().toLowerCase(Locale.ROOT).contains(lowerFilter)
					|| e.args().toLowerCase(Locale.ROOT).contains(lowerFilter)
					|| e.targetName().toLowerCase(Locale.ROOT).contains(lowerFilter)) {
				filtered.add(e);
			}
		}

		int pageSize = 10;
		int total = filtered.size();
		int maxPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
		int currentPage = Math.max(1, Math.min(page, maxPages));
		int fromIdx = (currentPage - 1) * pageSize;
		int toIdx = Math.min(fromIdx + pageSize, total);
		List<AuditEntry> pageList = fromIdx < total ? filtered.subList(fromIdx, toIdx) : List.of();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>GM Security Audit</title><body><center>");
		sb.append("<table width=270 bgcolor=\"000000\"><tr>");
		sb.append("<td width=50><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=19 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		sb.append("<td width=170 align=center><font color=\"LEVEL\">GM Security & Audit</font></td>");
		sb.append("<td width=50 align=right><button value=\"Refresh\" action=\"bypass -h admin_gmaudit ")
				.append(safeFilter.isEmpty() ? String.valueOf(currentPage) : safeFilter + " " + currentPage)
				.append("\" width=50 height=21 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
		sb.append("</tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=270 height=1><br>");

		sb.append("<table width=270><tr>");
		sb.append("<td width=45>Filtro:</td>");
		sb.append("<td width=125><edit var=\"qfilter\" width=120 height=15></td>");
		sb.append("<td width=50><button value=\"Filtrar\" action=\"bypass -h admin_gmaudit $qfilter 1\" width=50 height=21 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
		sb.append("<td width=50><button value=\"Limpar\" action=\"bypass -h admin_gmaudit 1\" width=50 height=21 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\"></td>");
		sb.append("</tr></table>");
		if (!safeFilter.isEmpty()) {
			sb.append("<font color=\"8FA888\">Filtro ativo: ").append(escapeHtml(safeFilter)).append("</font><br>");
		}

		sb.append("<table width=270 bgcolor=\"000000\">");
		sb.append("<tr><td width=48><font color=\"LEVEL\">Hora</font></td>")
				.append("<td width=62><font color=\"LEVEL\">GM(Lv)</font></td>")
				.append("<td width=95><font color=\"LEVEL\">Comando</font></td>")
				.append("<td width=65><font color=\"LEVEL\">Alvo</font></td></tr>");

		if (pageList.isEmpty()) {
			sb.append("<tr><td colspan=4 align=center><font color=\"AAAAAA\">Nenhum registro encontrado.</font></td></tr>");
		} else {
			for (AuditEntry e : pageList) {
				String timeStr = TIME_FMT.format(Instant.ofEpochMilli(e.timestamp()));
				String cmdFull = e.command() + (e.args().isEmpty() ? "" : " " + e.args());
				if (cmdFull.length() > 16) {
					cmdFull = cmdFull.substring(0, 14) + "..";
				}
				String tgt = e.targetName();
				if (tgt.length() > 10) {
					tgt = tgt.substring(0, 9) + ".";
				}
				sb.append("<tr>")
						.append("<td><font color=\"AAAAAA\">").append(timeStr).append("</font></td>")
						.append("<td><font color=\"8FA8BE\">").append(escapeHtml(e.gmName())).append("</font>(").append(e.accessLevel()).append(")</td>")
						.append("<td><font color=\"D4C49A\">//").append(escapeHtml(cmdFull)).append("</font></td>")
						.append("<td>").append(escapeHtml(tgt)).append("</td>")
						.append("</tr>");
			}
		}
		sb.append("</table><br>");

		sb.append("<table width=270><tr>");
		sb.append("<td width=60 align=left>");
		if (currentPage > 1) {
			String prevArg = safeFilter.isEmpty() ? String.valueOf(currentPage - 1) : safeFilter + " " + (currentPage - 1);
			sb.append("<button value=\"Prev\" action=\"bypass -h admin_gmaudit ").append(prevArg)
					.append("\" width=50 height=21 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
		}
		sb.append("</td>");
		sb.append("<td width=150 align=center><font color=\"LEVEL\">Pag ").append(currentPage)
				.append("/").append(maxPages).append(" (").append(total).append(")</font></td>");
		sb.append("<td width=60 align=right>");
		if (currentPage < maxPages) {
			String nextArg = safeFilter.isEmpty() ? String.valueOf(currentPage + 1) : safeFilter + " " + (currentPage + 1);
			sb.append("<button value=\"Next\" action=\"bypass -h admin_gmaudit ").append(nextArg)
					.append("\" width=50 height=21 back=\"L2UI_CH3.smallbutton1_over\" fore=\"L2UI_CH3.smallbutton1\">");
		}
		sb.append("</td></tr></table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	private static String escapeHtml(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
