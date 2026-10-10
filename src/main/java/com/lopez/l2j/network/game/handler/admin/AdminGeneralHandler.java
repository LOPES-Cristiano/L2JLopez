package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para comandos administrativos essenciais:
 * //heal, //kill, //delete, //del, //unspawn
 */
@Component
public class AdminGeneralHandler implements IAdminCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"heal",
			"kill",
			"kill_menu",
			"delete",
			"del",
			"unspawn"
	);

	@Override
	public boolean useAdminCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();
		int targetObjectId = session.targetObjectId();

		String lower = command.toLowerCase(Locale.ROOT);
		switch (lower) {
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
							session.send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), npc.template().maxHp()));
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " totalmente curado."));
							return true;
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
						new StatusUpdate.Attribute(StatusUpdate.MAX_MP, targetChar.maxMp()),
						new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) targetChar.currentCp()),
						new StatusUpdate.Attribute(StatusUpdate.MAX_CP, targetChar.maxCp()))));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetChar.name() + " totalmente curado."));
				return true;
			}
			case "kill", "kill_menu" -> {
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
						session.send(su);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
						var die = new Die(npc.objectId(), false);
						session.send(die);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, die, false);
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", npc.name() + " foi morto."));
						return true;
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
								pOnline.send(new Die(pOnline.objectId(), true));
							}
							session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", pOnline.name() + " foi morto."));
						}
						return true;
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um alvo valido para matar."));
				return true;
			}
			case "delete", "del", "unspawn" -> {
				if (targetObjectId != 0) {
					var npcOpt = ctx.world().npc(targetObjectId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (ctx.spawns() != null) {
							ctx.spawns().deleteSpawn(npc, true);
						} else {
							ctx.world().removeNpc(npc);
						}
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, new DeleteObject(npc.objectId()), false);
						session.send(new DeleteObject(npc.objectId()));
						session.targetObjectId(0);
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"NPC " + npc.name() + " (id " + npc.npcId() + ") removido do mundo e do banco."));
						return true;
					}
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um NPC valido para deletar."));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	@Override
	public List<String> getAdminCommandList() {
		return COMMANDS;
	}
}
