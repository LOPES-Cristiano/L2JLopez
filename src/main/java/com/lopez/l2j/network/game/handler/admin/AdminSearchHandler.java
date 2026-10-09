package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.AdminSearchService;
import com.lopez.l2j.game.skill.Skill;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para todos os comandos de busca e acoes rapidas de administracao:
 * //find_npc, //search_npc, //find_item, //search_item, //find_skill, //search_skill, //search
 */
@Component
public class AdminSearchHandler implements IAdminCommandHandler {

	private static final List<String> COMMANDS = List.of(
			// Busca de NPC
			"find_npc",
			"findnpc",
			"search_npc",
			"searchnpc",
			"npc_find",
			"find_monster",
			"search_monster",

			// Busca de Item
			"find_item",
			"finditem",
			"search_item",
			"searchitem",
			"item_find",

			// Busca de Skill
			"find_skill",
			"findskill",
			"search_skill",
			"searchskill",
			"skill_find",

			// Busca Global
			"search",
			"find",

			// Acoes rapidas disparadas pelas listas de busca
			"spawn_id",
			"find_npc_goto",
			"create_item_id",
			"give_target_item_id",
			"add_skill_id",
			"remove_skill_id"
	);

	private final AdminSearchService searchService;

	@Autowired
	public AdminSearchHandler(AdminSearchService searchService) {
		this.searchService = searchService;
	}

	@Override
	public boolean useAdminCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();

		String lower = command.toLowerCase(Locale.ROOT);
		String trimmed = params != null ? params.trim() : "";

		switch (lower) {
			case "find_npc", "findnpc", "search_npc", "searchnpc", "npc_find", "find_monster", "search_monster" -> {
				ParsedQuery pq = parseQueryAndPage(trimmed);
				String html = searchService.renderNpcSearchHtml(pq.query, pq.page);
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}

			case "find_item", "finditem", "search_item", "searchitem", "item_find" -> {
				ParsedQuery pq = parseQueryAndPage(trimmed);
				String html = searchService.renderItemSearchHtml(pq.query, pq.page);
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}

			case "find_skill", "findskill", "search_skill", "searchskill", "skill_find" -> {
				ParsedQuery pq = parseQueryAndPage(trimmed);
				String html = searchService.renderSkillSearchHtml(pq.query, pq.page);
				session.send(new NpcHtmlMessage(0, html));
				return true;
			}

			case "search", "find" -> {
				if (trimmed.isEmpty()) {
					String html = searchService.renderGlobalSearchHtml("");
					session.send(new NpcHtmlMessage(0, html));
					return true;
				}
				String[] parts = trimmed.split("\\s+", 2);
				String first = parts[0].toLowerCase(Locale.ROOT);
				if (first.equals("npc") || first.equals("mob") || first.equals("monster")) {
					ParsedQuery pq = parseQueryAndPage(parts.length > 1 ? parts[1] : "");
					session.send(new NpcHtmlMessage(0, searchService.renderNpcSearchHtml(pq.query, pq.page)));
					return true;
				} else if (first.equals("item") || first.equals("equip") || first.equals("weapon") || first.equals("armor")) {
					ParsedQuery pq = parseQueryAndPage(parts.length > 1 ? parts[1] : "");
					session.send(new NpcHtmlMessage(0, searchService.renderItemSearchHtml(pq.query, pq.page)));
					return true;
				} else if (first.equals("skill") || first.equals("buff") || first.equals("magic")) {
					ParsedQuery pq = parseQueryAndPage(parts.length > 1 ? parts[1] : "");
					session.send(new NpcHtmlMessage(0, searchService.renderSkillSearchHtml(pq.query, pq.page)));
					return true;
				} else {
					// Busca global unificada
					session.send(new NpcHtmlMessage(0, searchService.renderGlobalSearchHtml(trimmed)));
					return true;
				}
			}

			case "spawn_id" -> {
				String[] parts = trimmed.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					try {
						int npcId = Integer.parseInt(parts[0]);
						int count = parts.length >= 2 ? Math.min(50, Math.max(1, Integer.parseInt(parts[1]))) : 1;
						boolean store = parts.length >= 3 && "1".equals(parts[2]);
						if (ctx.spawns() != null) {
							for (int i = 0; i < count; i++) {
								ctx.spawns().spawn(npcId, active.x(), active.y(), active.z(), active.heading(), store);
							}
							session.updateKnownObjects();
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Spawned " + count + "x NPC id " + npcId + (store ? " (salvo no banco de dados)" : "")));
							return true;
						}
					} catch (Exception e) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //spawn_id <npcId> [count] [store:0|1]"));
						return true;
					}
				}
			}

			case "find_npc_goto" -> {
				if (!trimmed.isEmpty()) {
					try {
						int npcId = Integer.parseInt(trimmed.split("\\s+")[0]);
						var opt = ctx.world().npcs().stream().filter(n -> n.npcId() == npcId).findFirst();
						if (opt.isPresent()) {
							var npc = opt.get();
							session.teleportToLocation(npc.x(), npc.y(), npc.z());
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Teleportado para " + npc.name() + " (" + npc.x() + ", " + npc.y() + ", " + npc.z() + ")"));
						} else {
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"NPC id " + npcId + " nao possui instancias ativas no mundo para teleportar."));
						}
						return true;
					} catch (Exception ignored) {}
				}
			}

			case "create_item_id" -> {
				String[] parts = trimmed.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					try {
						int itemId = Integer.parseInt(parts[0]);
						int count = parts.length >= 2 ? Math.max(1, Integer.parseInt(parts[1])) : 1;
						var added = ctx.inventories().addItem(active.inventory(), itemId, count, "AdminSearchCreate");
						if (added != null) {
							session.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
									added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
							session.refreshWeightAndPenalties();
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Item " + itemId + " x" + count + " entregue a seu inventario."));
						}
						return true;
					} catch (Exception e) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //create_item_id <itemId> [count]"));
						return true;
					}
				}
			}

			case "give_target_item_id" -> {
				String[] parts = trimmed.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					try {
						int itemId = Integer.parseInt(parts[0]);
						int count = parts.length >= 2 ? Math.max(1, Integer.parseInt(parts[1])) : 1;
						int targetObjId = session.targetObjectId();
						var targetPlayer = targetObjId != 0 ? ctx.world().player(targetObjId).orElse(null) : null;
						var destChar = targetPlayer != null && targetPlayer.character() != null ? targetPlayer.character() : active;
						var destSession = targetPlayer != null ? targetPlayer : session;

						var added = ctx.inventories().addItem(destChar.inventory(), itemId, count, "AdminSearchTarget");
						if (added != null) {
							destSession.send(new InventoryUpdate(List.of(ItemInfo.of(added.item(),
									added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
							if (destSession instanceof GameSession gs) {
								gs.refreshWeightAndPenalties();
							} else {
								destSession.send(new UserInfo(destChar, ctx.characters().template(destChar)));
							}
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
									"Item " + itemId + " x" + count + " entregue a " + destChar.name()));
						}
						return true;
					} catch (Exception e) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //give_target_item_id <itemId> [count]"));
						return true;
					}
				}
			}

			case "add_skill_id" -> {
				String[] parts = trimmed.split("\\s+");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					try {
						int skillId = Integer.parseInt(parts[0]);
						int level = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
						int targetObjId = session.targetObjectId();
						var targetPlayer = targetObjId != 0 ? ctx.world().player(targetObjId).orElse(null) : null;
						var destChar = targetPlayer != null && targetPlayer.character() != null ? targetPlayer.character() : active;
						var destSession = targetPlayer != null ? targetPlayer : session;

						destChar.skills().put(skillId, level);
						if (ctx.skillService() != null) {
							ctx.skillService().refreshPassives(destChar);
						}
						if (ctx.skills() != null) {
							ctx.skills().save(destChar.objectId(), 0, new Skill(skillId, level, "Skill " + skillId, false));
						}
						if (destSession instanceof GameSession gs) {
							gs.sendSkillList();
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Skill " + skillId + " nv " + level + " concedida a " + destChar.name()));
						return true;
					} catch (Exception e) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //add_skill_id <skillId> [level]"));
						return true;
					}
				}
			}

			case "remove_skill_id" -> {
				if (!trimmed.isEmpty()) {
					try {
						int skillId = Integer.parseInt(trimmed.split("\\s+")[0]);
						int targetObjId = session.targetObjectId();
						var targetPlayer = targetObjId != 0 ? ctx.world().player(targetObjId).orElse(null) : null;
						var destChar = targetPlayer != null && targetPlayer.character() != null ? targetPlayer.character() : active;
						var destSession = targetPlayer != null ? targetPlayer : session;

						destChar.skills().remove(skillId);
						if (ctx.skillService() != null) {
							ctx.skillService().removeSkill(destChar, skillId);
						} else if (ctx.skills() != null) {
							ctx.skills().delete(destChar.objectId(), 0, skillId);
						}
						if (destSession instanceof GameSession gs) {
							gs.sendSkillList();
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Skill " + skillId + " removida de " + destChar.name()));
						return true;
					} catch (Exception e) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //remove_skill_id <skillId>"));
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public List<String> getAdminCommandList() {
		return COMMANDS;
	}

	private record ParsedQuery(String query, int page) {}

	private static ParsedQuery parseQueryAndPage(String input) {
		if (input == null || input.isBlank()) {
			return new ParsedQuery("", 1);
		}
		String[] parts = input.trim().split("\\s+");
		if (parts.length >= 2) {
			try {
				int last = Integer.parseInt(parts[parts.length - 1]);
				if (last > 0) {
					StringBuilder q = new StringBuilder();
					for (int i = 0; i < parts.length - 1; i++) {
						if (i > 0) q.append(" ");
						q.append(parts[i]);
					}
					return new ParsedQuery(q.toString().trim(), last);
				}
			} catch (NumberFormatException ignored) {}
		}
		return new ParsedQuery(input.trim(), 1);
	}
}
