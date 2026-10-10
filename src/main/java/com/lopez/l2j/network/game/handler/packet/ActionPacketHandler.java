package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.Action;
import com.lopez.l2j.network.game.packet.GameClientPacket.AttackRequest;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestActionUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestMagicSkillUse;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeMoveType;
import com.lopez.l2j.network.game.packet.GameServerPacket.ChangeWaitType;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.StopMove;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ValidateLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.SendTradeRequest;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
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
		if (!session.inWorld() || active == null || active.sitting() || active.isDisabled() || active.isRooted()) {
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
		if (session.pendingNpcInteractObjectId() != 0 && ctx != null && ctx.world() != null) {
			var pendingNpc = ctx.world().npc(session.pendingNpcInteractObjectId()).orElse(null);
			if (pendingNpc != null) {
				double distToNpc = Math.hypot(p.targetX() - pendingNpc.x(), p.targetY() - pendingNpc.y());
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

		if (p.targetX() == p.originX() && p.targetY() == p.originY() && p.targetZ() == p.originZ()) {
			session.send(new StopMove(active.objectId(), session.x(), session.y(), session.z(), active.heading()));
			return;
		}
		active.moveTo(p.originX(), p.originY(), p.originZ());
		var move = new MoveToLocation(active.objectId(), p.targetX(), p.targetY(), p.targetZ(), p.originX(),
				p.originY(), p.originZ());
		session.send(move);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
		}
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
		double dx = p.x() - realX;
		double dy = p.y() - realY;
		double diffSq = dx * dx + dy * dy;
		// Se a diferenca for muito grande (> 1000 unidades), o pacote esta dessincronizado
		// Nao aceita a posicao defasada do cliente e sincroniza a posicao real do servidor
		if (diffSq > 1_000_000.0) {
			session.send(new ValidateLocation(active.objectId(), realX, realY, active.z(), active.heading()));
			return;
		}
		active.moveTo(p.x(), p.y(), p.z());
		active.heading(p.heading());
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
		if (!session.inWorld() || active == null) {
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
					session.send(new Die(npc.objectId(), false));
				}
				session.showAdminNpcInfo(npc);
				return;
			}
			if (session.targetObjectId() == npc.objectId()) {
				if (npc.isDead()) {
					session.send(StatusUpdate.hp(npc.objectId(), 0, (int) npc.maxHp()));
					session.send(new Die(npc.objectId(), false));
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
					int heading = (int) (Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 32768.0 / Math.PI);
					active.heading(heading);
					int npcHeading = (int) (Math.atan2(active.y() - npc.y(), active.x() - npc.x()) * 32768.0 / Math.PI);
					npc.heading(npcHeading);
					var valLoc = new ValidateLocation(npc.objectId(), npc.x(), npc.y(), npc.z(), npcHeading);
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, valLoc, true);
					session.showNpcHtml(npc, 0);
				}
			} else {
				session.targetObjectId(npc.objectId());
				int levelDiff = active.level() - npc.template().level();
				session.send(new MyTargetSelected(npc.objectId(), levelDiff));
				session.send(StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp()));
				if (npc.isDead()) {
					session.send(new Die(npc.objectId(), false));
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

		if (ctx.doors() != null) {
			var doorOpt = ctx.doors().door(p.objectId());
			if (doorOpt != null) {
				session.targetObjectId(doorOpt.doorId());
				session.send(new MyTargetSelected(doorOpt.doorId(), 0));
				session.send(StatusUpdate.hp(doorOpt.doorId(), (int) doorOpt.currentHp(), doorOpt.maxHp()));
				session.send(new ValidateLocation(doorOpt.doorId(), doorOpt.x(), doorOpt.y(), doorOpt.z(), 0));
				return;
			}
		}

		session.targetObjectId(0);
		session.send(new ActionFailed());
	}

	public void handleAttackRequest(AttackRequest p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled()) {
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
		if (!session.inWorld() || active == null || active.isDead()) {
			session.send(new ActionFailed());
			return;
		}
		var ctx = session.context();
		switch (p.actionId()) {
			case 0 -> { // Sit / Stand
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
						targetSession.send(new SendTradeRequest(active.objectId()));
						return;
					}
				}
				session.send(new ActionFailed());
			}
			case 4 -> { // Target Next
				if (ctx != null && ctx.world() != null) {
					double bestDistSq = 900.0 * 900.0;
					NpcInstance bestNpc = null;
					for (var npc : ctx.world().findNpcsAround(active.x(), active.y(), 900)) {
						if (npc.template().isAttackable() && !npc.isDead()) {
							double dx = active.x() - npc.x();
							double dy = active.y() - npc.y();
							double d2 = dx * dx + dy * dy;
							if (d2 < bestDistSq) {
								bestDistSq = d2;
								bestNpc = npc;
							}
						}
					}
					if (bestNpc != null) {
						session.targetObjectId(bestNpc.objectId());
						session.send(new MyTargetSelected(bestNpc.objectId(), 0));
						session.send(new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(),
								bestNpc.heading()));
						return;
					}
				}
				session.send(new ActionFailed());
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
			case 7 -> session.tradeStoreHandler().handlePrivateStoreManageSell(); // Private Store Buy
			case 8 -> session.tradeStoreHandler().handlePrivateStoreManageSell(); // Private Store Sell
			case 9 -> session.tradeStoreHandler().handlePrivateStoreManageSell(); // Private Store Package Sell
			case 10 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(false)); // Common Craft (Recipe Book)
			case 11 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(true));  // Dwarven Craft (Recipe Book)
			case 12 -> session.sendSocialAction(2); // Greeting
			case 13 -> session.sendSocialAction(3); // Victory
			case 14 -> session.sendSocialAction(4); // Advance
			case 15 -> { // Party Invite via icone
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession
							&& targetSession != session) {
						session.partyClanHandler().handleJoinParty(new GameClientPacket.RequestJoinParty(targetSession.character().name(), 0));
						return;
					}
				}
				session.send(new ActionFailed());
			}
			case 16 -> session.partyClanHandler().handleLeaveParty(); // Party Leave
			case 17 -> { // Party Dismiss / Expel
				var party = session.party();
				if (party != null && party.isLeader(active.objectId()) && session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession) {
						party.oust(targetSession.character().name());
						return;
					}
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
			case 53, 54 -> { // Follow / Move to Target
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession ts) {
						session.startFollow(ts);
						return;
					}
					var npcOpt = ctx.world().npc(session.targetObjectId());
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						active.moveTo(npc.x(), npc.y(), npc.z());
						session.send(new MoveToLocation(active.objectId(), active.x(), active.y(), active.z(), npc.x(), npc.y(), npc.z()));
						return;
					}
				}
				session.send(new ActionFailed());
			}
			default -> session.send(new ActionFailed());
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
