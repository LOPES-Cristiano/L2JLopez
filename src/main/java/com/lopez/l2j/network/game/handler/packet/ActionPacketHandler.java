package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.trade.TradeService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameEngineContext;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.Action;
import com.lopez.l2j.network.game.packet.GameClientPacket.AttackRequest;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestActionUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMagicSkillUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChairSit;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeMoveType;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.DoorStatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ManufactureRecipeEntry;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.RecipeShopSellList;
import com.lopez.l2j.network.game.packet.GameServerPacket.StaticObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.StopMove;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ValidateLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeRequest;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler modular para acoes do cliente, movimentacao, selecao de alvo e uso de habilidades.
 */
public class ActionPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(ActionPacketHandler.class);

	private final GameSession session;

	public ActionPacketHandler(GameSession session) {
		this.session = session;
	}

	public void handleMove(MoveBackwardToLocation p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled() || active.isRooted()) {
			session.send(new ActionFailed());
			return;
		}
		if (p.moveMovement() == 0 && !Config.ALLOW_KEYBOARD_MOVEMENT) {
			session.send(new ActionFailed());
			return;
		}
		if (session.casting()) {
			SkillTemplate currentSk = session.castingSkill();
			// Ao conjurar qualquer habilidade (ataque, cura, buff), o personagem para ("da uma paradinha")
			// e executa a habilidade ate o fim, sem cancelar o cast por cliques de movimentacao no chao.
			if (currentSk != null) {
				session.send(new ActionFailed());
				return;
			}
			// Apenas comandos especiais sem SkillTemplate (como /unstuck e Scroll of Escape) cancelam ao andar
			session.cancelCast();
		}
		if (session.teleporting()) {
			session.onAppearing();
		}
		var ctx = session.context();
		if (ctx != null && ctx.away() != null) {
			ctx.away().checkAndRemoveAway(session);
		}
		if (active.isFishing() && ctx != null && ctx.fishing() != null) {
			ctx.fishing().stopFishing(active, session::send);
		}

		int targetX = p.targetX();
		int targetY = p.targetY();
		int targetZ = p.targetZ();
		GeoEngine geo = GeoEngine.getInstance();
		if (geo != null) {
			if (geo.isBlockedByDynamicObjects(p.originX(), p.originY(), p.originZ(), targetX, targetY, targetZ)) {
				active.stopMove();
				session.send(new StopMove(active.objectId(), session.x(), session.y(), session.z(), active.heading()));
				session.send(new ActionFailed());
				return;
			}
			if (geo.isEnabled()) {
				GeoEngine.Location checked = geo.moveCheck(
						p.originX(), p.originY(), p.originZ(),
						targetX, targetY, targetZ);
				if (checked != null) {
					targetX = checked.x();
					targetY = checked.y();
					targetZ = checked.z();
				}
			}
		}

		if (session.pendingNpcInteractObjectId() != 0 && ctx != null && ctx.world() != null) {
			var pendingNpc = ctx.world().npc(session.pendingNpcInteractObjectId()).orElse(null);
			if (pendingNpc != null) {
				double distToNpc = Math.hypot(targetX - pendingNpc.x(), targetY - pendingNpc.y());
				if (distToNpc > 300) {
					session.pendingNpcInteractObjectId(0);
				}
			} else {
				session.pendingNpcInteractObjectId(0);
			}
		}
		if (p.moveMovement() != 0) {
			session.stopAutoAttack();
			session.stopFollow();
			session.pendingGroundItemPickupObjectId(0);
		}

		if (targetX == p.originX() && targetY == p.originY() && targetZ == p.originZ()) {
			active.stopMove();
			session.send(new StopMove(active.objectId(), session.x(), session.y(), session.z(), active.heading()));
			return;
		}
		active.moveTo(p.originX(), p.originY(), p.originZ());
		var move = new MoveToLocation(active.objectId(), targetX, targetY, targetZ, p.originX(),
				p.originY(), p.originZ());
		session.send(move);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
		}
		var summon = active.activeSummon();
		if (summon != null && !summon.isDead() && summon.following()) {
			int sx = summon.x(), sy = summon.y(), sz = summon.z();
			int tx = targetX - 40, ty = targetY - 40, tz = targetZ;
			summon.moveTo(tx, ty, tz);
			var sMove = new MoveToLocation(summon.objectId(), tx, ty, tz, sx, sy, sz);
			session.send(sMove);
			if (ctx != null && ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, sMove, false);
			}
		}
		MovementPacketHandler.checkFlagZoneOnMove(session);
		session.updateKnownObjects();
	}

	public void handleValidatePosition(ValidatePosition p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || (p.x() == 0 && p.y() == 0)) {
			return;
		}
		// Personagem morto ou em processo de teleporte nao tem posicao sobrescrita por pacotes residuais
		if (active.isDead() || session.teleporting()) {
			return;
		}
		int realX = active.x();
		int realY = active.y();
		int realZ = active.z();
		double dx = p.x() - realX;
		double dy = p.y() - realY;
		double diffSq = dx * dx + dy * dy;

		GeoEngine geo = GeoEngine.getInstance();
		if (geo != null && geo.isBlockedByDynamicObjects(realX, realY, realZ, p.x(), p.y(), p.z())) {
			session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
			return;
		}

		// Se a diferenca for muito grande (> 500 unidades em modo estrito 2 ou > 1000 unidades), o pacote esta dessincronizado
		double maxDriftSq = (Config.COORD_SYNCHRONIZE == 2) ? 250_000.0 : 1_000_000.0;
		if (diffSq > maxDriftSq) {
			session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
			return;
		}

		int resolvedZ = p.z();
		if (geo != null && geo.isEnabled()) {
			short geoZ = geo.getHeight(p.x(), p.y(), p.z());
			if (Math.abs(p.z() - geoZ) > 200) {
				if (Math.abs(realZ - geoZ) <= 200) {
					resolvedZ = geoZ;
				} else {
					session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
					return;
				}
			} else {
				resolvedZ = geoZ;
			}
		}

		active.moveTo(p.x(), p.y(), resolvedZ);
		active.heading(p.heading());
		MovementPacketHandler.checkFlagZoneOnMove(session);
		session.updateKnownObjects();
		session.checkPendingNpcInteract();
		if (session.pendingGroundItemPickupObjectId() != 0) {
			if (tryPickupGroundItem(session.pendingGroundItemPickupObjectId())) {
				session.pendingGroundItemPickupObjectId(0);
			}
		}
		session.checkAutoAttackRangeOnMove();
		session.checkZoneEnvironment();
	}

	public boolean tryPickupGroundItem(int groundItemObjectId) {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (active == null || ctx == null || ctx.groundItems() == null) {
			return false;
		}
		var giOpt = ctx.groundItems().byObjectId(groundItemObjectId);
		if (giOpt.isEmpty()) {
			return false;
		}
		var gi = giOpt.get();
		double dx = active.x() - gi.x();
		double dy = active.y() - gi.y();
		if (dx * dx + dy * dy <= 150 * 150) {
			if (active.inventory() != null && ctx.inventories() != null) {
				boolean alreadyOwned = active.inventory().byItemId(gi.itemId()).isPresent();
				var tpl = ctx.inventories().templates().get(gi.itemId()).orElse(null);
				boolean stackable = tpl != null && tpl.stackable();
				if ((!stackable || !alreadyOwned) && active.inventory().size() >= active.maxInventorySlots()) {
					session.send(SystemMessage.id(SystemMessage.SLOTS_FULL));
					session.send(new ActionFailed());
					return false;
				}
			}
			var picked = ctx.groundItems().pickupItem(active, gi.objectId());
			if (picked.isPresent() && ctx.inventories() != null) {
				var added = ctx.inventories().addItem(active.inventory(), gi.itemId(), gi.count(), "GroundPickup");
				if (added != null) {
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(java.util.List.of(
							com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(), added.created() ? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED : com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
					session.refreshWeightAndPenalties();
					if (added.item().template() != null) {
						session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay(0, com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay.ALL, "SYS",
								"Voce pegou " + added.item().template().name() + " x" + gi.count()));
					}
				}
			}
			return true;
		}
		return false;
	}

	public void handleAction(Action p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || session.teleporting()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		if (ctx == null || ctx.world() == null) {
			session.send(new ActionFailed());
			return;
		}
		if (p.objectId() == active.objectId()) {
			session.targetObjectId(active.objectId());
			session.send(new MyTargetSelected(active.objectId(), 0));
			session.send(StatusUpdate.hp(active.objectId(), (int) active.currentHp(), active.maxHp()));
			return;
		}
		var npcOpt = ctx.world().npc(p.objectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (p.shift() != 0 && active.isGm()) {
				session.targetObjectId(npc.objectId());
				int levelDiff = active.level() - npc.template().level();
				session.send(new MyTargetSelected(npc.objectId(), levelDiff));
				session.send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp()));
				if (npc.isDead()) {
					session.send(new Die(npc.objectId(), false, false, false, npc.isSweepable()));
				}
				session.showAdminNpcInfo(npc);
				return;
			}
			if (session.targetObjectId() == npc.objectId()) {
				if (npc.isDead()) {
					session.send(StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp()));
					session.send(new Die(npc.objectId(), false, false, false, npc.isSweepable()));
					session.send(new ActionFailed());
					return;
				}
				if (npc.template().isAttackable()) {
					if (session.isAttackingNow()) {
						session.send(new ActionFailed());
						return;
					}
					session.startAutoAttack(npc);
				} else {
					double dx = active.x() - npc.x();
					double dy = active.y() - npc.y();
					double distSq = dx * dx + dy * dy;
					double interactDist = 220.0;
					if (distSq > interactDist * interactDist) {
						session.pendingNpcInteractObjectId(npc.objectId());
						var movePawn = new MoveToPawn(active.objectId(), npc.objectId(), 80, active.x(), active.y(),
								active.z());
						session.send(movePawn);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
						return;
					}
					session.pendingNpcInteractObjectId(0);
					int heading = (int) (Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 32768.0 / Math.PI) & 0xFFFF;
					active.heading(heading);
					int npcHeading = (int) (Math.atan2(active.y() - npc.y(), active.x() - npc.x()) * 32768.0 / Math.PI) & 0xFFFF;
					npc.heading(npcHeading);
					session.showNpcHtml(npc, 0);
				}
			} else {
				session.targetObjectId(npc.objectId());
				int levelDiff = active.level() - npc.template().level();
				session.send(new MyTargetSelected(npc.objectId(), levelDiff));
				session.send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp()));
				if (npc.isDead()) {
					session.send(new Die(npc.objectId(), false, false, false, npc.isSweepable()));
				}
				session.send(new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npc.heading()));
			}
			return;
		}
		var playerOpt = ctx.world().player(p.objectId());
		if (playerOpt.isPresent()) {
			var other = playerOpt.get();
			if (other.character() != null && (other.character().isBuffShop() || (ctx.buffShop() != null && ctx.buffShop().isBuffShop(other.objectId())))) {
				session.targetObjectId(other.objectId());
				session.send(new MyTargetSelected(other.objectId(), 0));
				if (other.character() != null) {
					session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(), other.character().maxHp()));
				}
				session.send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));

				double dx = active.x() - other.x();
				double dy = active.y() - other.y();
				if (dx * dx + dy * dy <= 250.0 * 250.0) {
					var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.buffShop().renderBuyerShopHtml(active, other.objectId(), skillTable, 1)));
				} else {
					var movePawn = new MoveToPawn(active.objectId(), other.objectId(), 200, active.x(), active.y(), active.z());
					session.send(movePawn);
					if (ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
					}
				}
				return;
			}
			if (other.character() != null && (other.character().privateStoreType() == PlayerCharacter.STORE_TYPE_SELL
					|| other.character().privateStoreType() == 8
					|| other.character().privateStoreType() == PlayerCharacter.STORE_TYPE_BUY
					|| other.character().privateStoreType() == PlayerCharacter.STORE_MANUFACTURE)) {
				session.targetObjectId(other.objectId());
				session.send(new MyTargetSelected(other.objectId(), 0));
				session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(), other.character().maxHp()));
				session.send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));

				double dx = active.x() - other.x();
				double dy = active.y() - other.y();
				if (dx * dx + dy * dy <= 250.0 * 250.0) {
					int storeType = other.character().privateStoreType();
					if (storeType == PlayerCharacter.STORE_TYPE_BUY) {
						session.tradeStoreHandler().showPrivateStoreBuyWindow(other.character());
					} else if (storeType == PlayerCharacter.STORE_MANUFACTURE) {
						showManufactureShopWindow(active, other);
					} else {
						session.tradeStoreHandler().showPrivateStoreSellWindow(other.character());
					}
				} else {
					var movePawn = new MoveToPawn(active.objectId(), other.objectId(), 200, active.x(), active.y(), active.z());
					session.send(movePawn);
					if (ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
					}
				}
				return;
			}
			if (session.targetObjectId() == other.objectId() && other instanceof GameSession targetSession && targetSession != session) {
				if (session.isAttackingNow()) {
					session.send(new ActionFailed());
					return;
				}
				if (session.canAutoAttackPlayerWithoutCtrl(targetSession)) {
					session.startAutoAttack(targetSession, false);
				} else {
					session.startFollow(targetSession);
				}
				return;
			}
			session.targetObjectId(other.objectId());
			session.send(new MyTargetSelected(other.objectId(), 0));
			if (other.character() != null) {
				session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(), other.character().maxHp()));
			}
			session.send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));
			if (p.shift() != 0 && active.isGm() && other.character() != null) {
				session.showAdminCharInfo(other.character().name());
			}
			return;
		}

		if (ctx.staticObjects() != null) {
			var staticObjOpt = ctx.staticObjects().byObjectId(p.objectId());
			if (staticObjOpt.isPresent()) {
				var obj = staticObjOpt.get();
				session.targetObjectId(obj.objectId());
				session.send(new MyTargetSelected(obj.objectId(), 0));
				session.send(new StaticObject(obj.staticObjectId(), obj.objectId()));
				session.send(new ValidateLocation(obj.objectId(), obj.x(), obj.y(), obj.z(), 0));

				if (p.shift() != 0 && active.isGm()) {
					String htm = "<html><body><table border=0>"
							+ "<tr><td>Static Object Info:</td></tr>"
							+ "<tr><td>X: " + obj.x() + "</td></tr>"
							+ "<tr><td>Y: " + obj.y() + "</td></tr>"
							+ "<tr><td>Z: " + obj.z() + "</td></tr>"
							+ "<tr><td>Object ID: " + obj.objectId() + "</td></tr>"
							+ "<tr><td>Static ID: " + obj.staticObjectId() + "</td></tr>"
							+ "<tr><td>Type: " + obj.type() + "</td></tr>"
							+ "<tr><td>Texture: " + obj.texture() + "</td></tr>"
							+ "</table></body></html>";
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(obj.objectId(), htm));
					return;
				}

				if (obj.isTownMap()) {
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.ShowTownMap(obj.texture(), obj.mapX(), obj.mapY()));
					session.send(new ActionFailed());
				} else if (obj.isSignboard()) {
					String content = ctx.htmls() != null ? ctx.htmls().getHtml("signboard.htm") : null;
					if (content == null) {
						content = "<html><body>Signboard:<br>Welcome to the realm.</body></html>";
					}
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(obj.objectId(), content));
					session.send(new ActionFailed());
				} else if (obj.isThroneOrStatue()) {
					double dx = active.x() - obj.x();
					double dy = active.y() - obj.y();
					if (dx * dx + dy * dy > 150.0 * 150.0) {
						var movePawn = new MoveToPawn(active.objectId(), obj.objectId(), 60, active.x(), active.y(), active.z());
						session.send(movePawn);
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
					} else {
						Clan clan = (active.clanId() > 0 && ctx.clans() != null)
								? ctx.clans().byClanId(active.clanId()).orElse(null)
								: null;
						int castleOwnerClanId = clan != null && clan.castleId() > 0 ? clan.clanId() : 0;
						if (active.isGm() || obj.canSitOnThrone(active, clan, castleOwnerClanId)) {
							active.sitting(true);
							active.moveTo(obj.x(), obj.y(), obj.z());
							ChairSit sit = new ChairSit(active.objectId(), obj.staticObjectId());
							session.send(sit);
							ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, sit, false);
						} else {
							session.send(new ActionFailed());
						}
					}
				}
				return;
			}
		}

		if (ctx.groundItems() != null) {
			var groundItemOpt = ctx.groundItems().byObjectId(p.objectId());
			if (groundItemOpt.isPresent()) {
				var gi = groundItemOpt.get();
				if (!tryPickupGroundItem(gi.objectId())) {
					session.pendingGroundItemPickupObjectId(gi.objectId());
					var move = new MoveToLocation(active.objectId(), gi.x(), gi.y(), gi.z(), active.x(), active.y(),
							active.z());
					session.send(move);
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
				} else {
					session.pendingGroundItemPickupObjectId(0);
				}
				return;
			}
		}

		DoorTable doorTable = ctx.doors() != null ? ctx.doors() : DoorTable.getInstance();
		if (doorTable != null) {
			DoorInstance doorOpt = doorTable.door(p.objectId());
			if (doorOpt != null) {
				int doorObjId = doorOpt.objectId();
				if (p.shift() != 0 && active.isGm()) {
					session.targetObjectId(doorObjId);
					session.send(new MyTargetSelected(doorObjId, 0));
					session.send(new StaticObject(doorOpt.doorId(), doorObjId));
					session.send(new DoorStatusUpdate(doorOpt));
					session.send(StatusUpdate.hp(doorObjId, doorOpt.currentHp(), doorOpt.maxHp()));
					String htm = "<html><body><table border=0>"
							+ "<tr><td>Door Info:</td></tr>"
							+ "<tr><td>Door ID: " + doorOpt.doorId() + "</td></tr>"
							+ "<tr><td>Object ID: " + doorObjId + "</td></tr>"
							+ "<tr><td>HP: " + doorOpt.currentHp() + " / " + doorOpt.maxHp() + "</td></tr>"
							+ "<tr><td>Open: " + doorOpt.isOpen() + "</td></tr>"
							+ "</table></body></html>";
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(doorObjId, htm));
					return;
				}
				if ((session.targetObjectId() == doorObjId || session.targetObjectId() == doorOpt.doorId())
						&& !doorOpt.isOpen() && doorOpt.currentHp() > 0) {
					attackDoor(active, ctx, doorOpt);
					return;
				}
				session.targetObjectId(doorObjId);
				session.send(new MyTargetSelected(doorObjId, 0));
				session.send(new StaticObject(doorOpt.doorId(), doorObjId));
				session.send(new DoorStatusUpdate(doorOpt));
				session.send(StatusUpdate.hp(doorObjId, doorOpt.currentHp(), doorOpt.maxHp()));
				session.send(new ValidateLocation(doorObjId, doorOpt.x(), doorOpt.y(), doorOpt.z(), 0));
				return;
			}
		}

		session.targetObjectId(0);
		session.send(new ActionFailed());
	}

	private void showManufactureShopWindow(PlayerCharacter buyer, GameWorld.OnlinePlayer crafterOnline) {
		if (buyer == null || crafterOnline == null || crafterOnline.character() == null) {
			return;
		}
		PlayerCharacter crafter = crafterOnline.character();
		List<ManufactureRecipeEntry> recipes = new ArrayList<>();
		if (crafterOnline instanceof GameSession crafterSession && crafterSession.extendedProtocolHandler() != null) {
			try {
				var field = ExtendedProtocolPacketHandler.class.getDeclaredField("manufactureList");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				List<ManufactureRecipeEntry> list = (List<ManufactureRecipeEntry>) field.get(crafterSession.extendedProtocolHandler());
				if (list != null) {
					recipes.addAll(list);
				}
			} catch (Exception ignored) {
			}
		}
		if (recipes.isEmpty() && OfflineTradeService.getInstance() != null) {
			OfflineTradeService.SellStoreList sellList = OfflineTradeService.getInstance().getSellStoreList(crafter.objectId());
			if (sellList != null && sellList.items() != null) {
				for (OfflineShopItem item : sellList.items()) {
					recipes.add(new ManufactureRecipeEntry(item.itemId(), item.price()));
				}
			}
		}
		session.send(new RecipeShopSellList(
				crafter.objectId(),
				(int) crafter.currentMp(),
				crafter.maxMp(),
				(int) (buyer.inventory() != null ? buyer.inventory().adena() : 0L),
				recipes));
	}

	private void attackDoor(PlayerCharacter active, GameSession.Context ctx, DoorInstance door) {
		int doorObjId = door.objectId();
		session.targetObjectId(doorObjId);

		int attackRange = session.getPhysicalAttackRange(active) + 100;
		double dx = active.x() - door.x();
		double dy = active.y() - door.y();
		if ((door.x() != 0 || door.y() != 0) && (dx * dx + dy * dy > (double) attackRange * attackRange)) {
			var movePawn = new MoveToPawn(active.objectId(), doorObjId, Math.max(60, attackRange - 40), active.x(), active.y(), active.z());
			session.send(movePawn);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
			return;
		}

		GeoEngine geo = GeoEngine.getInstance();
		if (geo != null && geo.isEnabled() && (door.x() != 0 || door.y() != 0)) {
			if (!geo.canSeeTarget(active.x(), active.y(), active.z(), door.x(), door.y(), door.z())) {
				session.send(SystemMessage.id(SystemMessage.CANT_SEE_TARGET));
				session.send(new ActionFailed());
				return;
			}
		}

		int pAtk = 150 + active.level() * 15;
		if (ctx.characters() != null) {
			var tpl = ctx.characters().template(active);
			if (tpl != null) {
				var stats = PlayerStats.calculate(active, tpl);
				if (stats != null && stats.pAtk() > 0) {
					pAtk = stats.pAtk();
				}
			}
		}
		int pDef = Math.max(1, door.pDef());
		int dmg = Math.max(1, (int) Math.round((70.0 * pAtk) / pDef));
		door.currentHp(door.currentHp() - dmg);
		var atk = new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(
				active.objectId(), doorObjId, dmg, 0, active.x(), active.y(), active.z());
		session.send(atk);
		ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, atk, false);
		var su = StatusUpdate.hp(doorObjId, door.currentHp(), door.maxHp());
		session.send(su);
		ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
		var dsu = new DoorStatusUpdate(door);
		session.send(dsu);
		ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, dsu, false);
	}

	public void handleAttackRequest(AttackRequest p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled() || session.teleporting()) {
			session.send(new ActionFailed());
			return;
		}
		if (session.isAttackingNow()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		if (ctx == null || ctx.world() == null) {
			session.send(new ActionFailed());
			return;
		}
		var npcOpt = ctx.world().npc(p.objectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (npc.isDead()) {
				session.send(StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp()));
				session.send(new Die(npc.objectId(), false));
				session.send(new ActionFailed());
				return;
			}
			if (npc.template().isAttackable()) {
				session.targetObjectId(npc.objectId());
				session.startAutoAttack(npc);
				return;
			}
		}
		var playerOpt = ctx.world().player(p.objectId());
		if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession && targetSession != session) {
			if (targetSession.activeChar() != null && !targetSession.activeChar().isDead()) {
				session.targetObjectId(targetSession.character().objectId());
				session.startAutoAttack(targetSession, true);
				return;
			}
		}
		DoorTable doorTable = ctx.doors() != null ? ctx.doors() : DoorTable.getInstance();
		if (doorTable != null) {
			var doorOpt = doorTable.door(p.objectId());
			if (doorOpt != null && !doorOpt.isOpen() && doorOpt.currentHp() > 0) {
				attackDoor(active, ctx, doorOpt);
				return;
			}
		}
		session.send(new ActionFailed());
	}

	public void handleCancelTarget() {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null) {
			return;
		}
		if (session.casting()) {
			session.cancelCast();
			return;
		}
		session.stopAutoAttack();
		session.stopFollow();
		session.clearTarget();
	}

	public void handleActionUse(RequestActionUse p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || session.teleporting()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		switch (p.actionId()) {
			case 0 -> { // Sit / Stand
				if (session.casting() || session.isAttackingNow() || active.isDisabled()
						|| (active.effects() != null && active.effects().hasSkill(60))
						|| active.privateStoreType() != PlayerCharacter.STORE_TYPE_NONE) {
					session.send(new ActionFailed());
					return;
				}
				active.sitting(!active.sitting());
				var wait = new ChangeWaitType(active.objectId(), active.sitting() ? 0 : 1, session.x(), session.y(), session.z());
				session.send(wait);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, wait, false);
				}
			}
			case 1 -> { // Walk / Run
				active.running(!active.running());
				var move = new ChangeMoveType(active.objectId(), active.running());
				session.send(move);
				if (ctx != null && ctx.world() != null) {
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
				}
			}
			case 2 -> { // Attack
				if (session.isAttackingNow()) {
					session.send(new ActionFailed());
					return;
				}
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var npcOpt = ctx.world().npc(session.targetObjectId());
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (npc.template().isAttackable() && !npc.isDead()) {
							session.startAutoAttack(npc);
							return;
						}
					}
					var playerOpt = ctx.world().player(session.targetObjectId());
					if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession && targetSession != session) {
						if (targetSession.activeChar() != null && !targetSession.activeChar().isDead()) {
							if (p.ctrl() || session.canAutoAttackPlayerWithoutCtrl(targetSession)) {
								session.startAutoAttack(targetSession, p.ctrl());
							} else {
								session.startFollow(targetSession);
							}
							return;
						}
					}
				}
				session.send(new ActionFailed());
			}
			case 3 -> { // Trade Request
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession
							&& targetSession != session && targetSession.activeChar() != null) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
								"Solicitando troca com " + targetSession.character().name() + "..."));
						TradeService tradeSvc = ctx.getBean(TradeService.class);
						if (tradeSvc == null) {
							tradeSvc = GameEngineContext.getBean(TradeService.class);
						}
						if (tradeSvc != null && session.tradeStoreHandler() != null) {
							session.tradeStoreHandler().handleTradeRequest(new GameClientPacket.TradeRequest(targetSession.objectId()));
						} else {
							targetSession.send(new SendTradeRequest(active.objectId()));
						}
						return;
					}
				}
				session.send(new ActionFailed());
			}
			case 4 -> { // Target Next
				handleTargetNearestHostile();
			}
			case 5 -> { // Pickup
				if (ctx != null && ctx.groundItems() != null) {
					var items = ctx.groundItems().findAround(active.x(), active.y(), 250);
					if (!items.isEmpty()) {
						var closest = items.stream().min(Comparator.comparingDouble(i -> {
							double dx = active.x() - i.x();
							double dy = active.y() - i.y();
							return dx * dx + dy * dy;
						})).orElse(null);
						if (closest != null && tryPickupGroundItem(closest.objectId())) {
							return;
						}
					}
				}
				session.send(new ActionFailed());
			}
			case 6 -> { // Assist
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession) {
						if (targetSession.targetObjectId() != 0) {
							session.targetObjectId(targetSession.targetObjectId());
							session.send(new MyTargetSelected(session.targetObjectId(), 0));
							return;
						}
					}
				}
				session.send(new ActionFailed());
			}
			case 7 -> session.tradeStoreHandler().handlePrivateStoreManageBuy(); // Private Store Buy
			case 8 -> session.tradeStoreHandler().handlePrivateStoreManageSell(); // Private Store Sell
			case 9 -> session.tradeStoreHandler().handlePrivateStoreManageSell(); // Private Store Package Sell
			case 10 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(false)); // Common Craft (Recipe Book)
			case 11 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(true));  // Dwarven Craft (Recipe Book)
			case 12 -> session.sendSocialAction(2); // Greeting
			case 13 -> session.sendSocialAction(3); // Victory
			case 14 -> session.sendSocialAction(4); // Advance
			case 15 -> { // Party Invite via icone (ou Pet Follow se não houver jogador alvo)
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession
							&& targetSession != session) {
						session.partyClanHandler().handleJoinParty(new GameClientPacket.RequestJoinParty(targetSession.character().name(), 0));
						return;
					}
				}
				if (active.activeSummon() != null) {
					toggleSummonFollow(active, ctx);
					return;
				}
				session.send(new ActionFailed());
			}
			case 16 -> { // Party Leave ou Pet Attack
				if (session.party() == null && active.activeSummon() != null) {
					orderSummonAttack(active, ctx);
					return;
				}
				session.partyClanHandler().handleLeaveParty();
			}
			case 17 -> { // Party Dismiss / Expel ou Pet Stop
				var party = session.party();
				if (party != null && party.isLeader(active.objectId()) && session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession) {
						party.oust(targetSession.character().name());
						return;
					}
				}
				if (active.activeSummon() != null) {
					stopSummon(active, ctx);
					return;
				}
				session.send(new ActionFailed());
			}
			case 18 -> { // Change Party Leader
				var party = session.party();
				if (party != null && party.isLeader(active.objectId()) && session.targetObjectId() != 0) {
					party.changeLeader(session.targetObjectId());
					return;
				}
				session.send(new ActionFailed());
			}
			case 19, 52 -> { // Unsummon Pet / Unsummon Servitor
				if (ctx != null && ctx.summonItems() != null && (active.hasPet() || active.hasSummon())) {
					ctx.summonItems().unsummonPet(active, session::send);
					return;
				}
				var sum = active.activeSummon();
				if (sum != null) {
					active.activeSummon(null);
					active.petObjectId(0);
					if (ctx != null && ctx.world() != null) {
						ctx.world().removeSummon(sum.objectId());
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
								new com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject(sum.objectId()), true);
					}
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetDelete(sum.summonType(), sum.objectId()));
					return;
				}
				session.send(new ActionFailed());
			}
			case 21 -> { // Summon Follow / Stay
				if (active.activeSummon() != null) {
					toggleSummonFollow(active, ctx);
					return;
				}
				session.send(new ActionFailed());
			}
			case 22 -> { // Summon Attack
				if (active.activeSummon() != null) {
					orderSummonAttack(active, ctx);
					return;
				}
				session.send(new ActionFailed());
			}
			case 23 -> { // Summon Stop
				if (active.activeSummon() != null) {
					stopSummon(active, ctx);
					return;
				}
				session.send(new ActionFailed());
			}
			case 24 -> session.sendSocialAction(6); // Yes
			case 25 -> session.sendSocialAction(5); // No
			case 26 -> session.sendSocialAction(7); // Bow
			case 27 -> session.sendSocialAction(8); // Unaware
			case 28 -> session.sendSocialAction(9); // Social Waiting
			case 29 -> session.sendSocialAction(10); // Laugh
			case 30 -> session.sendSocialAction(11); // Applaud / Clapping
			case 31 -> session.sendSocialAction(12); // Dance
			case 32 -> session.sendSocialAction(13); // Sorrow
			case 33 -> session.sendSocialAction(14); // Charm
			case 34 -> session.sendSocialAction(15); // Shyness
			case 53, 54 -> { // Move Servitor/Pet to Target (ou Follow se sem summon)
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var sum = active.activeSummon();
					if (sum != null && !sum.isDead()) {
						int tx = 0, ty = 0, tz = 0;
						boolean found = false;
						var npcOpt = ctx.world().npc(session.targetObjectId());
						if (npcOpt.isPresent()) {
							var npc = npcOpt.get();
							tx = npc.x();
							ty = npc.y();
							tz = npc.z();
							found = true;
						} else {
							var tpOpt = ctx.world().player(session.targetObjectId());
							if (tpOpt.isPresent()) {
								var tp = tpOpt.get();
								tx = tp.x();
								ty = tp.y();
								tz = tp.z();
								found = true;
							}
						}
						if (found) {
							sum.following(false);
							int ox = sum.x(), oy = sum.y(), oz = sum.z();
							sum.moveTo(tx, ty, tz);
							var mv = new MoveToLocation(sum.objectId(), tx, ty, tz, ox, oy, oz);
							session.send(mv);
							ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, mv, false);
							return;
						}
					}
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession ts) {
						session.startFollow(ts);
						return;
					}
					var npcOpt = ctx.world().npc(session.targetObjectId());
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						int ox = active.x(), oy = active.y(), oz = active.z();
						active.moveTo(npc.x(), npc.y(), npc.z());
						session.send(new MoveToLocation(active.objectId(), npc.x(), npc.y(), npc.z(), ox, oy, oz));
						return;
					}
				}
				session.send(new ActionFailed());
			}
			default -> session.send(new ActionFailed());
		}
	}

	/**
	 * Seleciona o inimigo atacavel mais proximo dentro de 1200 unidades (NextTarget / Action 4) (#099).
	 */
	public void handleTargetNearestHostile() {
		PlayerCharacter active = session.activeChar();
		var ctx = session.context();
		if (!session.inWorld() || active == null || ctx == null || ctx.world() == null) {
			session.send(new ActionFailed());
			return;
		}
		int currentTargetId = session.targetObjectId();
		double bestDistSq = 1200.0 * 1200.0;
		NpcInstance bestNpc = null;
		double fallbackDistSq = 1200.0 * 1200.0;
		NpcInstance fallbackNpc = null;
		GeoEngine geo = GeoEngine.getInstance();

		for (var npc : ctx.world().findNpcsAround(active.x(), active.y(), 1200)) {
			if (npc.template() != null && npc.template().isAttackable() && !npc.isDead()) {
				if (geo != null && geo.isEnabled()
						&& !geo.canSeeTarget(active.x(), active.y(), active.z(), npc.x(), npc.y(), npc.z())) {
					continue;
				}
				double dx = active.x() - npc.x();
				double dy = active.y() - npc.y();
				double d2 = dx * dx + dy * dy;
				if (npc.objectId() != currentTargetId && d2 < bestDistSq) {
					bestDistSq = d2;
					bestNpc = npc;
				}
				if (d2 < fallbackDistSq) {
					fallbackDistSq = d2;
					fallbackNpc = npc;
				}
			}
		}
		if (bestNpc == null) {
			bestNpc = fallbackNpc;
		}
		if (bestNpc != null) {
			session.targetObjectId(bestNpc.objectId());
			int levelDiff = active.level() - (bestNpc.template() != null ? bestNpc.template().level() : active.level());
			session.send(new MyTargetSelected(bestNpc.objectId(), levelDiff));
			session.send(StatusUpdate.hp(bestNpc.objectId(), (int) bestNpc.currentHp(), (int) bestNpc.maxHp()));
			session.send(new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(), bestNpc.heading()));
			return;
		}

		// Se nao houver monstro hostil, verifica jogador hostil/PvP proximo
		GameSession bestPlayer = null;
		double bestPlayerDistSq = 1200.0 * 1200.0;
		for (var pl : ctx.world().players()) {
			if (pl instanceof GameSession gs && gs != session && gs.activeChar() != null && !gs.activeChar().isDead()) {
				if (!session.canAutoAttackPlayerWithoutCtrl(gs)) {
					continue;
				}
				double dx = active.x() - gs.x();
				double dy = active.y() - gs.y();
				double d2 = dx * dx + dy * dy;
				if (d2 < bestPlayerDistSq) {
					bestPlayerDistSq = d2;
					bestPlayer = gs;
				}
			}
		}
		if (bestPlayer != null && bestPlayer.activeChar() != null) {
			session.targetObjectId(bestPlayer.objectId());
			session.send(new MyTargetSelected(bestPlayer.objectId(), 0));
			session.send(StatusUpdate.hp(bestPlayer.objectId(), (int) bestPlayer.activeChar().currentHp(), bestPlayer.activeChar().maxHp()));
			session.send(new ValidateLocation(bestPlayer.objectId(), bestPlayer.x(), bestPlayer.y(), bestPlayer.z(), bestPlayer.activeChar().heading()));
			return;
		}

		session.send(new ActionFailed());
	}

	private void toggleSummonFollow(PlayerCharacter active, GameSession.Context ctx) {
		var sum = active.activeSummon();
		if (sum == null || sum.isDead()) return;
		sum.following(!sum.following());
		if (sum.following()) {
			int ox = sum.x(), oy = sum.y(), oz = sum.z();
			sum.moveTo(active.x() + 40, active.y() + 40, active.z());
			var mv = new MoveToLocation(sum.objectId(), sum.x(), sum.y(), sum.z(), ox, oy, oz);
			session.send(mv);
			if (ctx != null && ctx.world() != null) {
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, mv, false);
			}
		}
		session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(sum));
	}

	private void orderSummonAttack(PlayerCharacter active, GameSession.Context ctx) {
		var sum = active.activeSummon();
		if (sum == null || sum.isDead() || session.targetObjectId() == 0 || ctx == null || ctx.world() == null || ctx.combat() == null) {
			session.send(new ActionFailed());
			return;
		}
		var npcOpt = ctx.world().npc(session.targetObjectId());
		if (npcOpt.isPresent()) {
			var npc = npcOpt.get();
			if (!npc.isDead() && npc.template().isAttackable()) {
				sum.targetObjectId(npc.objectId());
				sum.moveTo(npc.x() - 35, npc.y() - 35, npc.z());
				var hit = ctx.combat().summonAttackNpc(sum, npc);
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(sum.objectId(), npc.objectId(), hit.damage(), hit.flags(), sum.x(), sum.y(), sum.z()), true);
				if (hit.damage() > 0 && !sum.isDead() && sum.absorbDam() > 0.0 && sum.currentHp() < sum.maxHp()) {
					double vampHeal = Math.min(hit.damage(), Math.max(1.0, hit.damage() * (sum.absorbDam() / 100.0)));
					sum.currentHp(Math.min(sum.maxHp(), sum.currentHp() + vampHeal));
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(sum));
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
							StatusUpdate.hp(sum.objectId(), (int) sum.currentHp(), sum.maxHp()), true);
				}
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp()), true);
				if (hit.isDead()) {
					var tpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null) : null;
					PlayerCombatHandler.handleMonsterDeath(session, ctx, active, tpl, npc, session.party(), hit.expReward(), hit.spReward());
				}
				return;
			}
		}
		var playerOpt = ctx.world().player(session.targetObjectId());
		if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession && targetSession.activeChar() != null && !targetSession.activeChar().isDead()) {
			var targetChar = targetSession.activeChar();
			var charTpl = ctx.characters() != null ? ctx.characters().template(targetChar.classId()).orElse(null) : null;
			sum.targetObjectId(targetChar.objectId());
			sum.moveTo(targetChar.x() - 35, targetChar.y() - 35, targetChar.z());
			var hit = ctx.combat().summonAttackPlayer(sum, targetChar, charTpl);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(sum.objectId(), targetChar.objectId(), hit.damage(), 0, sum.x(), sum.y(), sum.z()), true);
			if (hit.damage() > 0 && !sum.isDead() && sum.absorbDam() > 0.0 && sum.currentHp() < sum.maxHp()) {
				double vampHeal = Math.min(hit.damage(), Math.max(1.0, hit.damage() * (sum.absorbDam() / 100.0)));
				sum.currentHp(Math.min(sum.maxHp(), sum.currentHp() + vampHeal));
				session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate(sum));
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						StatusUpdate.hp(sum.objectId(), (int) sum.currentHp(), sum.maxHp()), true);
			}
			targetSession.send(StatusUpdate.forPlayer(targetChar));
		}
	}

	private void stopSummon(PlayerCharacter active, GameSession.Context ctx) {
		var sum = active.activeSummon();
		if (sum == null || sum.isDead()) return;
		sum.targetObjectId(0);
		var stop = new StopMove(sum.objectId(), sum.x(), sum.y(), sum.z(), sum.heading());
		session.send(stop);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stop, false);
		}
	}

	public void handleMagicSkillUse(RequestMagicSkillUse p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.isDisabled()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		if (p.magicId() == 1312 && active.isFishing()) {
			if (ctx != null && ctx.fishing() != null) {
				ctx.fishing().stopFishing(active, session::send);
			}
			session.send(new ActionFailed());
			return;
		}
		var svc = ctx != null ? ctx.skillService() : null;
		var sk = svc == null ? null : svc.known(active, p.magicId()).orElse(null);
		if (sk == null || sk.isPassive()) {
			session.send(new ActionFailed());
			return;
		}
		if (sk.isToggle()) {
			session.toggleSkill(sk);
			session.send(new ActionFailed());
			return;
		}
		if (sk.magic() && active.isMuted()) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			session.send(new ActionFailed());
			return;
		}
		if (!sk.magic() && active.isPhysicalMuted()) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(sk.id(), sk.level())));
			session.send(new ActionFailed());
			return;
		}
		session.castSkill(sk, true);
	}
}
