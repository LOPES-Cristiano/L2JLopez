package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.world.GameWorld;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servico unificado de busca flexivel (case-insensitive, multi-termo, sem erros de sintaxe)
 * para NPCs, Itens e Skills, gerando interfaces HTML ricas e paginadas para GMs e ALT+G.
 */
@Service
public class AdminSearchService {

	public static final int PAGE_SIZE = 7;

	private final NpcTemplateTable npcTemplates;
	private final ItemTemplateTable itemTemplates;
	private final SkillTable skillTable;
	private final GameWorld world;

	@Autowired
	public AdminSearchService(
			@Autowired(required = false) NpcTemplateTable npcTemplates,
			@Autowired(required = false) ItemTemplateTable itemTemplates,
			@Autowired(required = false) SkillTable skillTable,
			@Autowired(required = false) GameWorld world) {
		this.npcTemplates = npcTemplates;
		this.itemTemplates = itemTemplates;
		this.skillTable = skillTable;
		this.world = world;
	}

	// =========================================================================
	// RECORDS DE RESULTADO
	// =========================================================================

	public record NpcSearchResult(
			int id,
			String name,
			String title,
			int level,
			String type,
			int hp,
			int mp,
			boolean isSpawnedInWorld,
			int spawnedCount,
			int spawnX,
			int spawnY,
			int spawnZ
	) {}

	public record ItemSearchResult(
			int id,
			String name,
			String grade,
			String type,
			long price,
			boolean isStackable
	) {}

	public record SkillSearchResult(
			int id,
			String name,
			int maxLevel,
			String operateType,
			String targetType,
			boolean isMagic
	) {}

	// =========================================================================
	// METODOS DE CONSULTA
	// =========================================================================

	public List<NpcSearchResult> searchNpcs(String query) {
		if (npcTemplates == null || query == null || query.isBlank()) {
			return List.of();
		}
		String clean = query.trim();
		String lower = clean.toLowerCase(Locale.ROOT);
		String[] tokens = lower.split("\\s+");

		// Tentativa por ID direto
		Integer targetId = parseInteger(clean);
		if (targetId != null) {
			var opt = npcTemplates.get(targetId);
			if (opt.isPresent()) {
				return List.of(mapNpcResult(opt.get()));
			}
		}

		Collection<NpcTemplate> all = npcTemplates.all();
		if (all.isEmpty() && targetId != null) {
			return List.of();
		}

		List<NpcSearchResult> matches = new ArrayList<>();
		for (NpcTemplate tpl : all) {
			String name = tpl.name() != null ? tpl.name().toLowerCase(Locale.ROOT) : "";
			String title = tpl.title() != null ? tpl.title().toLowerCase(Locale.ROOT) : "";
			String type = tpl.type() != null ? tpl.type().toLowerCase(Locale.ROOT) : "";

			boolean match = true;
			for (String token : tokens) {
				if (!name.contains(token) && !title.contains(token) && !type.contains(token)
						&& !String.valueOf(tpl.id()).contains(token)) {
					match = false;
					break;
				}
			}
			if (match) {
				matches.add(mapNpcResult(tpl));
			}
		}

		// Ordenacao inteligente: quem comeca com o termo primeiro, depois por level decrescente
		matches.sort((a, b) -> {
			boolean aExact = a.name().equalsIgnoreCase(clean);
			boolean bExact = b.name().equalsIgnoreCase(clean);
			if (aExact != bExact) return aExact ? -1 : 1;

			boolean aStarts = a.name().toLowerCase(Locale.ROOT).startsWith(lower);
			boolean bStarts = b.name().toLowerCase(Locale.ROOT).startsWith(lower);
			if (aStarts != bStarts) return aStarts ? -1 : 1;

			return Integer.compare(b.level(), a.level());
		});

		return matches;
	}

	public List<ItemSearchResult> searchItems(String query) {
		if (itemTemplates == null || query == null || query.isBlank()) {
			return List.of();
		}
		String clean = query.trim();
		String lower = clean.toLowerCase(Locale.ROOT);
		String[] tokens = lower.split("\\s+");

		// Tentativa por ID direto
		Integer targetId = parseInteger(clean);
		if (targetId != null) {
			var opt = itemTemplates.get(targetId);
			if (opt.isPresent()) {
				return List.of(mapItemResult(opt.get()));
			}
		}

		Collection<ItemTemplate> all = itemTemplates.all();
		List<ItemSearchResult> matches = new ArrayList<>();
		for (ItemTemplate tpl : all) {
			String name = tpl.name() != null ? tpl.name().toLowerCase(Locale.ROOT) : "";
			String grade = tpl.crystalType() != null ? tpl.crystalType().toLowerCase(Locale.ROOT) : "";
			String itemType = tpl.subType() != null ? tpl.subType().toLowerCase(Locale.ROOT) : "";

			boolean match = true;
			for (String token : tokens) {
				if (!name.contains(token) && !grade.contains(token) && !itemType.contains(token)
						&& !String.valueOf(tpl.id()).contains(token)) {
					match = false;
					break;
				}
			}
			if (match) {
				matches.add(mapItemResult(tpl));
			}
		}

		matches.sort((a, b) -> {
			boolean aExact = a.name().equalsIgnoreCase(clean);
			boolean bExact = b.name().equalsIgnoreCase(clean);
			if (aExact != bExact) return aExact ? -1 : 1;

			boolean aStarts = a.name().toLowerCase(Locale.ROOT).startsWith(lower);
			boolean bStarts = b.name().toLowerCase(Locale.ROOT).startsWith(lower);
			if (aStarts != bStarts) return aStarts ? -1 : 1;

			return a.name().compareToIgnoreCase(b.name());
		});

		return matches;
	}

	public List<SkillSearchResult> searchSkills(String query) {
		if (skillTable == null || query == null || query.isBlank()) {
			return List.of();
		}
		String clean = query.trim();
		String lower = clean.toLowerCase(Locale.ROOT);
		String[] tokens = lower.split("\\s+");

		Integer targetId = parseInteger(clean);
		if (targetId != null) {
			int maxLvl = skillTable.maxLevel(targetId);
			if (maxLvl > 0) {
				var opt = skillTable.get(targetId, maxLvl);
				if (opt.isPresent()) {
					return List.of(mapSkillResult(opt.get(), maxLvl));
				}
			}
		}

		Collection<SkillTemplate> all = skillTable.allSkillsMaxLevel();
		List<SkillSearchResult> matches = new ArrayList<>();
		for (SkillTemplate sk : all) {
			String name = sk.name() != null ? sk.name().toLowerCase(Locale.ROOT) : "";
			String sType = sk.skillType() != null ? sk.skillType().toLowerCase(Locale.ROOT) : "";

			boolean match = true;
			for (String token : tokens) {
				if (!name.contains(token) && !sType.contains(token)
						&& !String.valueOf(sk.id()).contains(token)) {
					match = false;
					break;
				}
			}
			if (match) {
				matches.add(mapSkillResult(sk, skillTable.maxLevel(sk.id())));
			}
		}

		matches.sort((a, b) -> {
			boolean aExact = a.name().equalsIgnoreCase(clean);
			boolean bExact = b.name().equalsIgnoreCase(clean);
			if (aExact != bExact) return aExact ? -1 : 1;

			boolean aStarts = a.name().toLowerCase(Locale.ROOT).startsWith(lower);
			boolean bStarts = b.name().toLowerCase(Locale.ROOT).startsWith(lower);
			if (aStarts != bStarts) return aStarts ? -1 : 1;

			return a.name().compareToIgnoreCase(b.name());
		});

		return matches;
	}

	// =========================================================================
	// RENDERIZACAO DE TELAS HTML MODERNAS
	// =========================================================================

	public String renderNpcSearchHtml(String query, int page) {
		List<NpcSearchResult> list = searchNpcs(query);
		int total = list.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
		int currentPage = Math.max(1, Math.min(page, totalPages));

		int startIdx = (currentPage - 1) * PAGE_SIZE;
		int endIdx = Math.min(startIdx + PAGE_SIZE, total);
		List<NpcSearchResult> slice = total > 0 ? list.subList(startIdx, endIdx) : List.of();

		StringBuilder sb = new StringBuilder(4096);
		sb.append("<html><title>NPC Search Engine</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\"><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180 align=center><font color=\"LEVEL\">NPC Search & Tool</font></td>");
		sb.append("<td width=45 align=right><a action=\"bypass -h admin_spawn_menu\"><font color=\"FFD306\">Spawn</font></a></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		// Formulario de busca rapida
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><edit var=\"npc_q\" width=175 height=15></td>");
		sb.append("<td width=100 align=right><button value=\"Search NPC\" action=\"bypass -h admin_find_npc $npc_q\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("</tr></table><br>");

		String safeQuery = query != null ? query.replace('"', ' ').trim() : "";
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><font color=\"B09979\">Query: </font><font color=\"FFFFFF\">").append(safeQuery.isEmpty() ? "All" : safeQuery).append("</font></td>");
		sb.append("<td width=100 align=right><font color=\"00FF7F\">Total: ").append(total).append("</font></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		if (slice.isEmpty()) {
			sb.append("<br><br><font color=\"FF4444\">Nenhum NPC encontrado para o termo pesquisado.</font><br>");
			sb.append("<font color=\"AAAAAA\">Tente digitar parte do nome (ex: Antharas, Guard, Merchant, Wolf)</font><br><br>");
		} else {
			for (NpcSearchResult npc : slice) {
				sb.append("<table width=285 bgcolor=\"181818\">");
				sb.append("<tr>");
				sb.append("<td width=185><font color=\"FFD700\"><b>").append(npc.name()).append("</b></font>");
				if (npc.title() != null && !npc.title().isBlank()) {
					sb.append(" <font color=\"AAAAAA\">&lt;").append(npc.title()).append("&gt;</font>");
				}
				sb.append("<br><font color=\"808080\">ID: </font><font color=\"FFFFFF\">").append(npc.id())
						.append("</font> <font color=\"808080\">Lv: </font><font color=\"FFFFFF\">").append(npc.level())
						.append("</font> <font color=\"808080\">Tipo: </font><font color=\"FFFFFF\">").append(npc.type()).append("</font>");
				if (npc.isSpawnedInWorld()) {
					sb.append("<br><font color=\"00FF00\">Ativo no Mundo: </font><font color=\"FFFF00\">").append(npc.spawnedCount()).append("x</font>");
				}
				sb.append("</td>");

				sb.append("<td width=95 align=right>");
				sb.append("<button value=\"Spawn\" action=\"bypass -h admin_spawn ").append(npc.id()).append(" 1\" width=60 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"><br>");
				if (npc.isSpawnedInWorld()) {
					sb.append("<button value=\"Teleport\" action=\"bypass -h admin_move_to ").append(npc.spawnX()).append(" ").append(npc.spawnY()).append(" ").append(npc.spawnZ()).append("\" width=60 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"><br>");
				}
				sb.append("<button value=\"DropList\" action=\"bypass -h admin_show_droplist ").append(npc.id()).append("\" width=60 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
				sb.append("</td>");
				sb.append("</tr>");
				sb.append("</table>");
				sb.append("<img src=\"L2UI.SquareBlank\" width=290 height=2>");
			}
		}

		// Barra de navegacao/paginacao
		sb.append("<br><table width=280><tr>");
		if (currentPage > 1) {
			sb.append("<td width=70 align=left><button value=\"&lt;&lt; Prev\" action=\"bypass -h admin_find_npc ").append(safeQuery).append(" ").append(currentPage - 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("<td width=140 align=center><font color=\"A0A0A0\">Page ").append(currentPage).append(" of ").append(totalPages).append("</font></td>");
		if (currentPage < totalPages) {
			sb.append("<td width=70 align=right><button value=\"Next &gt;&gt;\" action=\"bypass -h admin_find_npc ").append(safeQuery).append(" ").append(currentPage + 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("</tr></table>");

		sb.append("<br><img src=\"L2UI.SquareGray\" width=290 height=1><br>");
		sb.append("<font color=\"666666\">Lopez L2J Admin Suite</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String renderItemSearchHtml(String query, int page) {
		List<ItemSearchResult> list = searchItems(query);
		int total = list.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
		int currentPage = Math.max(1, Math.min(page, totalPages));

		int startIdx = (currentPage - 1) * PAGE_SIZE;
		int endIdx = Math.min(startIdx + PAGE_SIZE, total);
		List<ItemSearchResult> slice = total > 0 ? list.subList(startIdx, endIdx) : List.of();

		StringBuilder sb = new StringBuilder(4096);
		sb.append("<html><title>Item Search Engine</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\"><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180 align=center><font color=\"LEVEL\">Item Search & Creator</font></td>");
		sb.append("<td width=45 align=right><a action=\"bypass -h admin_itemcreation\"><font color=\"FFD306\">Create</font></a></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		// Formulario de busca
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><edit var=\"item_q\" width=175 height=15></td>");
		sb.append("<td width=100 align=right><button value=\"Search Item\" action=\"bypass -h admin_find_item $item_q\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("</tr></table><br>");

		String safeQuery = query != null ? query.replace('"', ' ').trim() : "";
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><font color=\"B09979\">Query: </font><font color=\"FFFFFF\">").append(safeQuery.isEmpty() ? "All" : safeQuery).append("</font></td>");
		sb.append("<td width=100 align=right><font color=\"00FF7F\">Total: ").append(total).append("</font></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		if (slice.isEmpty()) {
			sb.append("<br><br><font color=\"FF4444\">Nenhum item encontrado para o termo pesquisado.</font><br>");
			sb.append("<font color=\"AAAAAA\">Tente digitar parte do nome (ex: Draconic, Tateossian, Soulshot, Blessed)</font><br><br>");
		} else {
			for (ItemSearchResult item : slice) {
				sb.append("<table width=285 bgcolor=\"181818\">");
				sb.append("<tr>");
				sb.append("<td width=190><font color=\"FFD700\"><b>").append(item.name()).append("</b></font>");
				sb.append("<br><font color=\"808080\">ID: </font><font color=\"FFFFFF\">").append(item.id())
						.append("</font> <font color=\"808080\">Grade: </font><font color=\"FFFF77\">").append(item.grade().toUpperCase(Locale.ROOT))
						.append("</font> <font color=\"808080\">Tipo: </font><font color=\"AAAAAA\">").append(item.type()).append("</font>");
				sb.append("</td>");

				sb.append("<td width=90 align=right>");
				sb.append("<button value=\"Give 1\" action=\"bypass -h admin_create_item ").append(item.id()).append(" 1\" width=55 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"> ");
				sb.append("<button value=\"Give Target\" action=\"bypass -h admin_give_item_target ").append(item.id()).append(" 1\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
				sb.append("</td>");
				sb.append("</tr>");
				sb.append("</table>");
				sb.append("<img src=\"L2UI.SquareBlank\" width=290 height=2>");
			}
		}

		// Barra de navegacao/paginacao
		sb.append("<br><table width=280><tr>");
		if (currentPage > 1) {
			sb.append("<td width=70 align=left><button value=\"&lt;&lt; Prev\" action=\"bypass -h admin_find_item ").append(safeQuery).append(" ").append(currentPage - 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("<td width=140 align=center><font color=\"A0A0A0\">Page ").append(currentPage).append(" of ").append(totalPages).append("</font></td>");
		if (currentPage < totalPages) {
			sb.append("<td width=70 align=right><button value=\"Next &gt;&gt;\" action=\"bypass -h admin_find_item ").append(safeQuery).append(" ").append(currentPage + 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("</tr></table>");

		sb.append("<br><img src=\"L2UI.SquareGray\" width=290 height=1><br>");
		sb.append("<font color=\"666666\">Lopez L2J Admin Suite</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String renderSkillSearchHtml(String query, int page) {
		List<SkillSearchResult> list = searchSkills(query);
		int total = list.size();
		int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
		int currentPage = Math.max(1, Math.min(page, totalPages));

		int startIdx = (currentPage - 1) * PAGE_SIZE;
		int endIdx = Math.min(startIdx + PAGE_SIZE, total);
		List<SkillSearchResult> slice = total > 0 ? list.subList(startIdx, endIdx) : List.of();

		StringBuilder sb = new StringBuilder(4096);
		sb.append("<html><title>Skill Search Engine</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\"><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180 align=center><font color=\"LEVEL\">Skill Search & Add</font></td>");
		sb.append("<td width=45 align=right><a action=\"bypass -h admin_show_skills\"><font color=\"FFD306\">Skills</font></a></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		// Formulario de busca
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><edit var=\"skill_q\" width=175 height=15></td>");
		sb.append("<td width=100 align=right><button value=\"Search Skill\" action=\"bypass -h admin_find_skill $skill_q\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("</tr></table><br>");

		String safeQuery = query != null ? query.replace('"', ' ').trim() : "";
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><font color=\"B09979\">Query: </font><font color=\"FFFFFF\">").append(safeQuery.isEmpty() ? "All" : safeQuery).append("</font></td>");
		sb.append("<td width=100 align=right><font color=\"00FF7F\">Total: ").append(total).append("</font></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");

		if (slice.isEmpty()) {
			sb.append("<br><br><font color=\"FF4444\">Nenhuma skill encontrada para o termo pesquisado.</font><br>");
			sb.append("<font color=\"AAAAAA\">Tente digitar parte do nome (ex: Haste, Might, Wind Walk, Shield, Heal)</font><br><br>");
		} else {
			for (SkillSearchResult sk : slice) {
				sb.append("<table width=285 bgcolor=\"181818\">");
				sb.append("<tr>");
				sb.append("<td width=185><font color=\"FFD700\"><b>").append(sk.name()).append("</b></font>");
				sb.append("<br><font color=\"808080\">ID: </font><font color=\"FFFFFF\">").append(sk.id())
						.append("</font> <font color=\"808080\">Max Lv: </font><font color=\"00FFFF\">").append(sk.maxLevel())
						.append("</font> <font color=\"808080\">Tipo: </font><font color=\"AAAAAA\">").append(sk.operateType()).append("</font>");
				sb.append("</td>");

				sb.append("<td width=95 align=right>");
				sb.append("<button value=\"Add Max\" action=\"bypass -h admin_skill ").append(sk.id()).append(" ").append(sk.maxLevel()).append("\" width=60 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"><br>");
				sb.append("<button value=\"Remove\" action=\"bypass -h admin_removeskill ").append(sk.id()).append("\" width=60 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
				sb.append("</td>");
				sb.append("</tr>");
				sb.append("</table>");
				sb.append("<img src=\"L2UI.SquareBlank\" width=290 height=2>");
			}
		}

		// Barra de navegacao/paginacao
		sb.append("<br><table width=280><tr>");
		if (currentPage > 1) {
			sb.append("<td width=70 align=left><button value=\"&lt;&lt; Prev\" action=\"bypass -h admin_find_skill ").append(safeQuery).append(" ").append(currentPage - 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("<td width=140 align=center><font color=\"A0A0A0\">Page ").append(currentPage).append(" of ").append(totalPages).append("</font></td>");
		if (currentPage < totalPages) {
			sb.append("<td width=70 align=right><button value=\"Next &gt;&gt;\" action=\"bypass -h admin_find_skill ").append(safeQuery).append(" ").append(currentPage + 1).append("\" width=65 height=19 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td>");
		} else {
			sb.append("<td width=70></td>");
		}
		sb.append("</tr></table>");

		sb.append("<br><img src=\"L2UI.SquareGray\" width=290 height=1><br>");
		sb.append("<font color=\"666666\">Lopez L2J Admin Suite</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String renderGlobalSearchHtml(String query) {
		String safeQuery = query != null ? query.trim() : "";
		int npcsFound = searchNpcs(safeQuery).size();
		int itemsFound = searchItems(safeQuery).size();
		int skillsFound = searchSkills(safeQuery).size();

		StringBuilder sb = new StringBuilder(2048);
		sb.append("<html><title>Global Game Search</title><body><center>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=45><a action=\"bypass -h admin_admin\"><font color=\"FFD306\">Main</font></a></td>");
		sb.append("<td width=180 align=center><font color=\"LEVEL\">Global Search Hub</font></td>");
		sb.append("<td width=45 align=right><a action=\"bypass -h admin_admin\"><font color=\"FFD306\">Back</font></a></td>");
		sb.append("</tr></table>");

		sb.append("<img src=\"L2UI.SquareGray\" width=290 height=1><br>");
		sb.append("<table width=280><tr>");
		sb.append("<td width=180><edit var=\"g_q\" width=175 height=15></td>");
		sb.append("<td width=100 align=right><button value=\"Search All\" action=\"bypass -h admin_search $g_q\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("</tr></table><br>");

		sb.append("<table width=280 bgcolor=\"181818\">");
		sb.append("<tr><td colspan=2><font color=\"LEVEL\">Resultados para: </font><font color=\"FFFFFF\">").append(safeQuery.isEmpty() ? "(Todos)" : safeQuery).append("</font></td></tr>");
		sb.append("<tr><td width=170><font color=\"00FF7F\">NPCs Encontrados:</font> ").append(npcsFound).append("</td>");
		sb.append("<td width=110 align=right><button value=\"Ver NPCs\" action=\"bypass -h admin_find_npc ").append(safeQuery).append("\" width=85 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

		sb.append("<tr><td width=170><font color=\"00FFFF\">Itens Encontrados:</font> ").append(itemsFound).append("</td>");
		sb.append("<td width=110 align=right><button value=\"Ver Itens\" action=\"bypass -h admin_find_item ").append(safeQuery).append("\" width=85 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

		sb.append("<tr><td width=170><font color=\"FFD700\">Skills Encontradas:</font> ").append(skillsFound).append("</td>");
		sb.append("<td width=110 align=right><button value=\"Ver Skills\" action=\"bypass -h admin_find_skill ").append(safeQuery).append("\" width=85 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");
		sb.append("</table>");

		sb.append("<br><img src=\"L2UI.SquareGray\" width=290 height=1><br>");
		sb.append("<font color=\"666666\">Lopez L2J Admin Suite</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	// =========================================================================
	// AUXILIARES
	// =========================================================================

	private NpcSearchResult mapNpcResult(NpcTemplate tpl) {
		boolean spawned = false;
		int count = 0;
		int sx = 0, sy = 0, sz = 0;
		if (world != null) {
			for (NpcInstance inst : world.npcs()) {
				if (inst.npcId() == tpl.id()) {
					spawned = true;
					count++;
					if (count == 1) {
						sx = inst.x();
						sy = inst.y();
						sz = inst.z();
					}
				}
			}
		}
		return new NpcSearchResult(
				tpl.id(),
				tpl.name() != null ? tpl.name() : "Npc " + tpl.id(),
				tpl.title() != null ? tpl.title() : "",
				tpl.level(),
				tpl.type() != null ? tpl.type() : "L2Npc",
				tpl.maxHp(),
				tpl.maxMp(),
				spawned,
				count,
				sx, sy, sz
		);
	}

	private static ItemSearchResult mapItemResult(ItemTemplate tpl) {
		String grade = tpl.crystalType() != null && !tpl.crystalType().isBlank() ? tpl.crystalType() : "none";
		String type = tpl.subType() != null ? tpl.subType() : (tpl.kind() != null ? tpl.kind().name() : "Item");
		return new ItemSearchResult(
				tpl.id(),
				tpl.name() != null ? tpl.name() : "Item " + tpl.id(),
				grade,
				type,
				tpl.price(),
				tpl.stackable()
		);
	}

	private static SkillSearchResult mapSkillResult(SkillTemplate sk, int maxLevel) {
		return new SkillSearchResult(
				sk.id(),
				sk.name() != null ? sk.name() : "Skill " + sk.id(),
				maxLevel,
				sk.operateType() != null ? sk.operateType().name() : "ACTIVE",
				sk.target() != null ? sk.target() : "TARGET_ONE",
				sk.magic()
		);
	}

	private static Integer parseInteger(String val) {
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
			return null;
		}
	}
}
