package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.summon.SummonInstance;
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
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillLaunched;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.ManufactureRecipeEntry;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreManageListSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.RecipeShopManageList;
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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler modular para acoes do cliente, movimentacao, selecao de alvo e uso de
 * habilidades.
 */
public class ActionPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(ActionPacketHandler.class);

	/**
	 * Mapeamento oficial de Action ID -> Skill ID para habilidades de Pets e
	 * Servitors Interlude (#252).
	 */
	private static final Map<Integer, Integer> SUMMON_ACTION_SKILLS = Map.ofEntries(
			Map.entry(36, 4259), // Toxic Smoke (Soulless)
			Map.entry(39, 4138), // Parasite Burst (Soulless)
			Map.entry(41, 4230), // Wild Cannon (Wild Hog Cannon)
			Map.entry(42, 4378), // Anti-Summoning Field (Mew the Cat)
			Map.entry(43, 4137), // Hydro Screw (Boxer the Unicorn)
			Map.entry(44, 4139), // Boom Attack (Big Boom)
			Map.entry(45, 4025), // Master Recharge (Merrow the Unicorn / Boxer)
			Map.entry(46, 4261), // Mega Storm Strike (Mirage the Unicorn)
			Map.entry(47, 4260), // Steal Blood (Silhouette)
			Map.entry(48, 4068), // Mech. Cannon (Mechanic Golem)
			Map.entry(1000, 4079), // Siege Hammer (Siege Golem)
			Map.entry(1001, 4163), // Ultimate Bombastic Buster (Sin Eater)
			Map.entry(1003, 4710), // Wild Stun (Wind Hatchling/Strider)
			Map.entry(1004, 4711), // Wild Defense (Wind Hatchling/Strider)
			Map.entry(1005, 4712), // Bright Burst (Star Hatchling/Strider)
			Map.entry(1006, 4713), // Bright Heal (Star Hatchling/Strider)
			Map.entry(1007, 4699), // Blessing of Queen (Cat Queen)
			Map.entry(1008, 4700), // Gift of Queen (Cat Queen)
			Map.entry(1009, 4701), // Cure of Queen (Cat Queen)
			Map.entry(1010, 4702), // Blessing of Seraphim (Unicorn Seraphim)
			Map.entry(1011, 4703), // Gift of Seraphim (Unicorn Seraphim)
			Map.entry(1012, 4704), // Cure of Seraphim (Unicorn Seraphim)
			Map.entry(1013, 4705), // Curse of Shade (Nightshade)
			Map.entry(1014, 4706), // Mass Curse of Shade (Nightshade)
			Map.entry(1015, 4707), // Shade Sacrifice (Nightshade)
			Map.entry(1016, 4709), // Greater Impulse (Cursed Man)
			Map.entry(1017, 4708), // Stunning Blow (Cursed Man)
			Map.entry(1031, 5135), // Slash (Feline King)
			Map.entry(1032, 5136), // Spinning Slash (Feline King)
			Map.entry(1033, 5137), // Hold of King (Feline King)
			Map.entry(1034, 5138), // Whiplash (Magnus the Unicorn)
			Map.entry(1035, 5139), // Tidal Wave (Magnus the Unicorn)
			Map.entry(1036, 5142), // Corpse Kaboom (Spectral Lord)
			Map.entry(1037, 5141), // Dicing Death (Spectral Lord)
			Map.entry(1038, 5140), // Force Curse (Spectral Lord)
			Map.entry(1039, 5110), // Swoop Cannon (Swoop Cannon)
			Map.entry(1040, 5111) // Canopy (Swoop Cannon)
	);

	private final GameSession session;

	public ActionPacketHandler(GameSession session) {
		this.session = session;
	}

	public static int resolveSummonSkillId(int actionId) {
		return SUMMON_ACTION_SKILLS.getOrDefault(actionId, 0);
	}

	public void handleMove(MoveBackwardToLocation p) {
		PlayerCharacter active = session.activeChar();
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled()
				|| active.isRooted()) {
			session.send(new ActionFailed());
			return;
		}
		if (p.moveMovement() == 0 && !Config.ALLOW_KEYBOARD_MOVEMENT) {
			session.send(new ActionFailed());
			return;
		}
		if (session.casting()) {
			SkillTemplate currentSk = session.castingSkill();
			// Ao conjurar qualquer habilidade (ataque, cura, buff), o personagem para ("da
			// uma paradinha")
			// e executa a habilidade ate o fim, sem cancelar o cast por cliques de
			// movimentacao no chao.
			if (currentSk != null) {
				session.send(new ActionFailed());
				return;
			}
			// Apenas comandos especiais sem SkillTemplate (como /unstuck e Scroll of
			// Escape) cancelam ao andar
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
		// Personagem morto ou em processo de teleporte nao tem posicao sobrescrita por
		// pacotes residuais
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

		// Se a diferenca for muito grande (> 500 unidades em modo estrito 2 ou > 1000
		// unidades), o pacote esta dessincronizado
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
					session.send(
							new com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate(java.util.List.of(
									com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.of(added.item(),
											added.created()
													? com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.ADDED
													: com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo.MODIFIED))));
					session.refreshWeightAndPenalties();
					if (added.item().template() != null) {
						session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay(0,
								com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay.ALL, "SYS",
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
					int heading = (int) (Math.atan2(npc.y() - active.y(), npc.x() - active.x()) * 32768.0 / Math.PI)
							& 0xFFFF;
					active.heading(heading);
					int npcHeading = (int) (Math.atan2(active.y() - npc.y(), active.x() - npc.x()) * 32768.0 / Math.PI)
							& 0xFFFF;
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
			if (other.character() != null && (other.character().isBuffShop()
					|| (ctx.buffShop() != null && ctx.buffShop().isBuffShop(other.objectId())))) {
				session.targetObjectId(other.objectId());
				session.send(new MyTargetSelected(other.objectId(), 0));
				if (other.character() != null) {
					session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(),
							other.character().maxHp()));
				}
				session.send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));

				double dx = active.x() - other.x();
				double dy = active.y() - other.y();
				if (dx * dx + dy * dy <= 250.0 * 250.0) {
					var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0,
							ctx.buffShop().renderBuyerShopHtml(active, other.objectId(), skillTable, 1)));
				} else {
					var movePawn = new MoveToPawn(active.objectId(), other.objectId(), 200, active.x(), active.y(),
							active.z());
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
				session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(),
						other.character().maxHp()));
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
					var movePawn = new MoveToPawn(active.objectId(), other.objectId(), 200, active.x(), active.y(),
							active.z());
					session.send(movePawn);
					if (ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, movePawn, false);
					}
				}
				return;
			}
			if (session.targetObjectId() == other.objectId() && other instanceof GameSession targetSession
					&& targetSession != session) {
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
				session.send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(),
						other.character().maxHp()));
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
					session.send(
							new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(obj.objectId(), htm));
					return;
				}

				if (obj.isTownMap()) {
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.ShowTownMap(obj.texture(),
							obj.mapX(), obj.mapY()));
					session.send(new ActionFailed());
				} else if (obj.isSignboard()) {
					String content = ctx.htmls() != null ? ctx.htmls().getHtml("signboard.htm") : null;
					if (content == null) {
						content = "<html><body>Signboard:<br>Welcome to the realm.</body></html>";
					}
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(obj.objectId(),
							content));
					session.send(new ActionFailed());
				} else if (obj.isThroneOrStatue()) {
					double dx = active.x() - obj.x();
					double dy = active.y() - obj.y();
					if (dx * dx + dy * dy > 150.0 * 150.0) {
						var movePawn = new MoveToPawn(active.objectId(), obj.objectId(), 60, active.x(), active.y(),
								active.z());
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
		var ctx = session.context();
		RecipeService rs = (ctx != null && ctx.recipes() != null)
				? ctx.recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		if (rs != null) {
			for (var item : rs.getManufactureList(crafter.objectId())) {
				recipes.add(new ManufactureRecipeEntry(item.recipeId(), item.cost()));
			}
		}
		if (recipes.isEmpty() && crafterOnline instanceof GameSession crafterSession
				&& crafterSession.extendedProtocolHandler() != null) {
			recipes.addAll(crafterSession.extendedProtocolHandler().getManufactureList());
		}
		if (recipes.isEmpty() && OfflineTradeService.getInstance() != null) {
			OfflineTradeService.SellStoreList sellList = OfflineTradeService.getInstance()
					.getSellStoreList(crafter.objectId());
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
			var movePawn = new MoveToPawn(active.objectId(), doorObjId, Math.max(60, attackRange - 40), active.x(),
					active.y(), active.z());
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
		if (!session.inWorld() || active == null || active.isDead() || active.sitting() || active.isDisabled()
				|| session.teleporting()) {
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
			case 0 -> { // Sit / Stand (ou cancelar Fake Death)
				if (active.effects() != null && (active.effects().isFakeDeath() || active.effects().hasSkill(60))) {
					active.effects().removeSkill(60);
					active.sitting(false);
					var stopFd = new ChangeWaitType(active.objectId(), 3, session.x(), session.y(), session.z());
					session.send(stopFd);
					if (ctx != null && ctx.world() != null) {
						ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stopFd, false);
					}
					session.refreshBuffs();
					session.sendMagicEffectIcons();
					return;
				}
				if (session.casting() || session.isAttackingNow() || active.isDisabled()
						|| active.privateStoreType() != PlayerCharacter.STORE_TYPE_NONE) {
					session.send(new ActionFailed());
					return;
				}
				active.sitting(!active.sitting());
				var wait = new ChangeWaitType(active.objectId(), active.sitting() ? 0 : 1, session.x(), session.y(),
						session.z());
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
					if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession
							&& targetSession != session) {
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
							session.tradeStoreHandler()
									.handleTradeRequest(new GameClientPacket.TradeRequest(targetSession.objectId()));
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
			case 9, 61 -> openPrivateStorePackageManageList(active, ctx); // Private Store Package Sell
			case 10 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(false)); // Common Craft
																									// (Recipe Book)
			case 11 -> session.onRecipeBookOpen(new GameClientPacket.RequestRecipeBookOpen(true)); // Dwarven Craft
																									// (Recipe Book)
			case 12 -> session.sendSocialAction(2); // Greeting
			case 13 -> session.sendSocialAction(3); // Victory
			case 14 -> session.sendSocialAction(4); // Advance
			case 15 -> { // Party Invite via icone (ou Pet Follow se não houver jogador alvo)
				if (session.targetObjectId() != 0 && ctx != null && ctx.world() != null) {
					var targetPlayerOpt = ctx.world().player(session.targetObjectId());
					if (targetPlayerOpt.isPresent() && targetPlayerOpt.get() instanceof GameSession targetSession
							&& targetSession != session) {
						session.partyClanHandler().handleJoinParty(
								new GameClientPacket.RequestJoinParty(targetSession.character().name(), 0));
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
				if (party != null && party.isLeader(active.objectId()) && session.targetObjectId() != 0 && ctx != null
						&& ctx.world() != null) {
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
								new com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject(sum.objectId()),
								true);
					}
					session.send(new com.lopez.l2j.network.game.packet.GameServerPacket.PetDelete(sum.summonType(),
							sum.objectId()));
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
			case 32 -> { // Wild Hog Cannon Switch Mode / Social Sorrow
				SummonInstance sum = active.activeSummon();
				if (sum != null && !sum.isDead() && sum.isWildHogCannon()) {
					boolean siegeMode = sum.toggleWildHogCannonMode();
					session.send(new PetStatusUpdate(sum));
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							siegeMode ? "Wild Hog Cannon entrou em modo de disparo (Siege Mode)."
									: "Wild Hog Cannon voltou ao modo de movimento."));
					return;
				}
				session.sendSocialAction(13);
			}
			case 33 -> session.sendSocialAction(14); // Charm
			case 34 -> session.sendSocialAction(15); // Shyness
			case 37 -> openManufactureManageList(active, ctx, true); // Dwarven Manufacture
			case 38 -> handleMountDismountAction(active, ctx); // Mount / Dismount
			case 51 -> openManufactureManageList(active, ctx, false); // General Manufacture
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
			default -> {
				int summonSkillId = resolveSummonSkillId(p.actionId());
				if (summonSkillId > 0) {
					useSummonSkill(active, ctx, summonSkillId, p.ctrl());
					return;
				}
				session.send(new ActionFailed());
			}
		}
	}

	private void handleMountDismountAction(PlayerCharacter active, GameSession.Context ctx) {
		PetService petService = (ctx != null && ctx.getBean(PetService.class) != null)
				? ctx.getBean(PetService.class)
				: PetService.getInstance();
		if (petService == null) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isMounted()) {
			if (petService.dismount(session, active)) {
				session.sendUserInfoAndBroadcastCharInfo();
				return;
			}
			session.send(new ActionFailed());
			return;
		}
		SummonInstance sum = active.activeSummon();
		if (sum != null && !sum.isDead() && sum.isPet()) {
			int npcId = sum.idTemplate();
			if (petService.mount(session, active, npcId)) {
				if (ctx != null && ctx.summonItems() != null) {
					ctx.summonItems().unsummonPet(active, session::send);
				} else {
					active.activeSummon(null);
					active.petObjectId(0);
				}
				session.sendUserInfoAndBroadcastCharInfo();
				return;
			}
		}
		session.send(new ActionFailed());
	}

	private void openManufactureManageList(PlayerCharacter active, GameSession.Context ctx, boolean isDwarven) {
		if (active == null || active.isDead() || active.isOlympiadMode()) {
			session.send(new ActionFailed());
			return;
		}
		if (active.isInCombat()) {
			session.send(SystemMessage.id(SystemMessage.CANT_CRAFT_DURING_COMBAT));
			session.send(new ActionFailed());
			return;
		}
		RecipeService rs = (ctx != null && ctx.recipes() != null)
				? ctx.recipes()
				: (GameEngineContext.getBean(RecipeService.class) != null
						? GameEngineContext.getBean(RecipeService.class)
						: RecipeService.getInstance());
		if (rs != null && !rs.isCraftingEnabled()) {
			session.send(SystemMessage.id(SystemMessage.CANT_CRAFT_DURING_COMBAT));
			session.send(new ActionFailed());
			return;
		}
		if (!active.isGm()) {
			if (isDwarven) {
				boolean canDwarvenCraft = active.skillLevel(172) > 0 || active.skillLevel(1321) > 0
						|| active.canCraft() || com.lopez.l2j.game.craft.DwarfCraftBonus.isDwarf(active);
				if (!canDwarvenCraft) {
					session.send(SystemMessage.id(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
					session.send(new ActionFailed());
					return;
				}
			} else {
				boolean canCommonCraft = active.skillLevel(1320) > 0 || active.skillLevel(1322) > 0;
				if (!canCommonCraft) {
					session.send(SystemMessage.id(SystemMessage.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
					session.send(new ActionFailed());
					return;
				}
			}
		}
		if (active.privateStoreType() != PlayerCharacter.STORE_TYPE_NONE) {
			active.privateStoreType(PlayerCharacter.STORE_TYPE_NONE);
			active.sitting(false);
			session.sendUserInfoAndBroadcastCharInfo();
		}
		List<Integer> book = new ArrayList<>();
		List<ManufactureRecipeEntry> currentList = new ArrayList<>();
		if (rs != null) {
			book.addAll(rs.getRecipeBook(active.objectId(), isDwarven));
			for (var item : rs.getManufactureList(active.objectId())) {
				if (book.contains(item.recipeId())) {
					currentList.add(new ManufactureRecipeEntry(item.recipeId(), item.cost()));
				}
			}
		}
		if (session.extendedProtocolHandler() != null) {
			session.extendedProtocolHandler().syncManufactureList(currentList);
		}
		long adena = active.inventory() != null ? active.inventory().adena() : 0L;
		session.send(new RecipeShopManageList(active.objectId(), (int) adena, isDwarven, book, currentList));
	}

	private void openPrivateStorePackageManageList(PlayerCharacter active, GameSession.Context ctx) {
		List<PrivateStoreItem> availableItems = new ArrayList<>();
		if (active.inventory() != null) {
			for (ItemInstance it : active.inventory().items()) {
				if (!it.isEquipped() && it.template() != null && it.template().type2() != ItemTemplate.TYPE2_QUEST
						&& it.itemId() != ItemTemplate.ADENA_ID
						&& (it.template().isTradeable() || it.template().price() > 0)) {
					availableItems.add(new PrivateStoreItem(it.objectId(), it.itemId(), it.count(),
							it.template().price(), it.template().bodyPart(), it.enchant()));
				}
			}
		}
		List<PrivateStoreItem> currentItems = new ArrayList<>();
		OfflineTradeService ots = ctx != null && ctx.offlineTrade() != null ? ctx.offlineTrade()
				: OfflineTradeService.getInstance();
		if (ots != null) {
			OfflineTradeService.SellStoreList existing = ots.getSellStoreList(active.objectId());
			if (existing != null && existing.items() != null) {
				for (OfflineShopItem si : existing.items()) {
					currentItems.add(new PrivateStoreItem(si.objectId(), si.itemId(), si.count(), si.price(),
							si.bodyPart(), si.enchant()));
				}
			}
		}
		int currencyId = Config.SELL_BY_ITEM ? Config.SELL_ITEM : ItemTemplate.ADENA_ID;
		int coinCount = active.inventory() != null
				? active.inventory().byItemId(currencyId).map(ItemInstance::count).orElse(0)
				: 0;
		session.send(new PrivateStoreManageListSell(active.objectId(), true, coinCount, availableItems, currentItems));
	}

	private void useSummonSkill(PlayerCharacter active, GameSession.Context ctx, int skillId, boolean ctrlPressed) {
		SummonInstance sum = active.activeSummon();
		if (sum == null || sum.isDead() || sum.isDisabled() || sum.isMuted()) {
			session.send(new ActionFailed());
			return;
		}
		// Wild Cannon (4230) exige modo de cerco no Wild Hog Cannon e consome Siege
		// Cannonball; Swoop Cannon (5110) tambem consome Cannonball
		if (skillId == 4230) {
			if (sum.isWildHogCannon() && !sum.isSiegeMode()) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Wild Hog Cannon precisa estar em Siege Mode para disparar."));
				session.send(new ActionFailed());
				return;
			}
			if (!sum.consumeSiegeCannonball()) {
				session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_ITEMS));
				session.send(new ActionFailed());
				return;
			}
		} else if (skillId == 5110) {
			if (!sum.consumeSiegeCannonball()) {
				session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_ITEMS));
				session.send(new ActionFailed());
				return;
			}
		}

		SkillTemplate sk = null;
		if (ctx != null && ctx.skillService() != null && ctx.skillService().table() != null) {
			int maxLvl = ctx.skillService().table().maxLevel(skillId);
			sk = ctx.skillService().table().get(skillId, Math.max(1, maxLvl)).orElse(null);
		}
		int skillLvl = sk != null ? sk.level() : 1;
		int mpConsume = sk != null ? Math.max(0, sk.mpConsume()) : 10;
		if (sum.currentMp() < mpConsume) {
			session.send(SystemMessage.id(SystemMessage.NOT_ENOUGH_MP));
			session.send(new ActionFailed());
			return;
		}
		sum.currentMp(Math.max(0.0, sum.currentMp() - mpConsume));
		double spsMult = sum.consumeSpiritShotAndRecharge();

		int targetObjId = session.targetObjectId() != 0 ? session.targetObjectId() : active.objectId();
		int hitTime = sk != null && sk.hitTime() > 0 ? sk.hitTime() : 1000;
		int reuseDelay = sk != null && sk.reuseDelay() > 0 ? sk.reuseDelay() : 3000;

		MagicSkillUse msu = new MagicSkillUse(sum.objectId(), targetObjId, skillId, skillLvl, hitTime, reuseDelay,
				sum.x(), sum.y(), sum.z());
		MagicSkillLaunched msl = new MagicSkillLaunched(sum.objectId(), skillId, skillLvl, new int[] { targetObjId });
		session.send(msu);
		session.send(msl);
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msu, false);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msl, false);
		}

		// Aplica efeito da skill (cura, recarga de MP, buff em mestre/party ou dano no
		// alvo)
		if (skillId == 4025) { // Master Recharge
			double mpGain = (40.0 + sum.level() * 1.5) * Math.sqrt(spsMult);
			active.currentMp(Math.min(active.maxMp(), active.currentMp() + mpGain));
			session.sendVitals();
		} else if (skillId == 4713 || skillId == 4707) { // Bright Heal / Shade Sacrifice
			double hpGain = (80.0 + sum.level() * 3.0) * Math.sqrt(spsMult);
			active.currentHp(Math.min(active.maxHp(), active.currentHp() + hpGain));
			session.sendVitals();
		} else if (skillId == 4699 || skillId == 4700 || skillId == 4702 || skillId == 4703 || skillId == 4711
				|| skillId == 5111) {
			if (sk != null && active.effects() != null) {
				active.effects().applySkill(sk, 120_000L);
				session.refreshBuffs();
				session.sendMagicEffectIcons();
			}
		} else if (skillId == 4701 || skillId == 4704) { // Cure of Queen / Cure of Seraphim
			if (active.effects() != null) {
				active.effects().cleanseDebuffs();
				session.refreshBuffs();
				session.sendMagicEffectIcons();
			}
		} else if (ctx != null && ctx.world() != null && session.targetObjectId() != 0) {
			var npcOpt = ctx.world().npc(session.targetObjectId());
			if (npcOpt.isPresent()) {
				NpcInstance npc = npcOpt.get();
				if (!npc.isDead() && npc.template().isAttackable()) {
					double power = sk != null && sk.power() > 0 ? sk.power() : (60.0 + sum.level() * 2.5);
					int dmg = Math.max(1,
							(int) Math.round((sum.mAtk() * spsMult + power) * 45.0 / Math.max(1.0, npc.mDef())));
					boolean killed = npc.applyDamage(active.objectId(), dmg);
					if (ctx.questManager() != null) {
						ctx.questManager().onNpcAttack(npc, session, true);
					}
					var su = StatusUpdate.hp(npc.objectId(), (int) npc.currentHp(), (int) npc.maxHp());
					session.send(su);
					ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, su, false);
					if (killed) {
						var tpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null)
								: null;
						PlayerCombatHandler.handleMonsterDeath(session, ctx, active, tpl, npc, session.party(),
								npc.template().exp(), npc.template().sp());
					}
				}
			}
		}
		session.send(new PetStatusUpdate(sum));
	}

	/**
	 * Seleciona o inimigo atacavel mais proximo dentro de 1200 unidades (NextTarget
	 * / Action 4) (#099).
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
			session.send(
					new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(), bestNpc.heading()));
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
			session.send(StatusUpdate.hp(bestPlayer.objectId(), (int) bestPlayer.activeChar().currentHp(),
					bestPlayer.activeChar().maxHp()));
			session.send(new ValidateLocation(bestPlayer.objectId(), bestPlayer.x(), bestPlayer.y(), bestPlayer.z(),
					bestPlayer.activeChar().heading()));
			return;
		}

		session.send(new ActionFailed());
	}

	private void toggleSummonFollow(PlayerCharacter active, GameSession.Context ctx) {
		var sum = active.activeSummon();
		if (sum == null || sum.isDead())
			return;
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
		if (sum == null || sum.isDead() || session.targetObjectId() == 0 || ctx == null || ctx.world() == null
				|| ctx.combat() == null) {
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
				if (ctx.questManager() != null) {
					ctx.questManager().onNpcAttack(npc, session, true);
				}
				ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(sum.objectId(), npc.objectId(),
								hit.damage(), hit.flags(), sum.x(), sum.y(), sum.z()),
						true);
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
					var tpl = ctx.characters() != null ? ctx.characters().template(active.classId()).orElse(null)
							: null;
					PlayerCombatHandler.handleMonsterDeath(session, ctx, active, tpl, npc, session.party(),
							hit.expReward(), hit.spReward());
				}
				return;
			}
		}
		var playerOpt = ctx.world().player(session.targetObjectId());
		if (playerOpt.isPresent() && playerOpt.get() instanceof GameSession targetSession
				&& targetSession.activeChar() != null && !targetSession.activeChar().isDead()) {
			var targetChar = targetSession.activeChar();
			var charTpl = ctx.characters() != null ? ctx.characters().template(targetChar.classId()).orElse(null)
					: null;
			sum.targetObjectId(targetChar.objectId());
			sum.moveTo(targetChar.x() - 35, targetChar.y() - 35, targetChar.z());
			var hit = ctx.combat().summonAttackPlayer(sum, targetChar, charTpl);
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new com.lopez.l2j.network.game.packet.GameServerPacket.Attack(sum.objectId(), targetChar.objectId(),
							hit.damage(), 0, sum.x(), sum.y(), sum.z()),
					true);
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
		if (sum == null || sum.isDead())
			return;
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
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			session.send(new ActionFailed());
			return;
		}
		if (!sk.magic() && active.isPhysicalMuted()) {
			session.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED,
					new SystemMessage.SkillName(sk.id(), sk.level())));
			session.send(new ActionFailed());
			return;
		}
		session.castSkill(sk, true);
	}
}
