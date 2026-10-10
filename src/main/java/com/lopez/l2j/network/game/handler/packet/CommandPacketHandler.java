package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.augmentation.AugmentationService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationCancelWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExShowVariationMakeWindow;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.MyTargetSelected;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ValidateLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handler modular para comandos de barra (slash /...) e comandos de voz (dot ....).
 */
public class CommandPacketHandler {

	private final GameSession session;

	public CommandPacketHandler(GameSession session) {
		this.session = session;
	}

	private GameSession.Context ctx() {
		return session.context();
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private void send(GameServerPacket p) {
		session.send(p);
	}

	public void handleSlashCommand(String cmd) {
		PlayerCharacter active = active();
		if (active == null) {
			return;
		}
		String lower = cmd.toLowerCase(Locale.ROOT).trim();
		if (lower.equals("/loc")) {
			session.onUserCommand(0);
		} else if (lower.equals("/unstuck")) {
			session.onUserCommand(52);
		} else if (lower.equals("/time")) {
			session.onUserCommand(77);
		} else if (lower.equals("/olympiadstat")) {
			session.onUserCommand(109);
		} else if (lower.startsWith("/target ")) {
			String name = cmd.substring(8).trim().toLowerCase(Locale.ROOT);
			if (!name.isEmpty()) {
				targetByName(name);
			}
		} else if (lower.equals("/sit") || lower.equals("/stand")) {
			session.onActionUse(new GameClientPacket.RequestActionUse(0, false, false));
		} else if (lower.equals("/attack")) {
			session.onActionUse(new GameClientPacket.RequestActionUse(2, false, false));
		} else if (lower.equals("/leave") || lower.equals("/partyleave")) {
			session.onLeaveParty();
		} else if (lower.equals("/offline")) {
			if (!com.lopez.l2j.config.Config.ALLOW_OFFLINE_TRADE) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline desativado pelo servidor."));
				return;
			}
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado. Desconectando sessao..."));
			session.onLogout();
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Comando: " + cmd));
		}
	}

	public void targetByName(String query) {
		PlayerCharacter active = active();
		var ctx = ctx();
		if (active == null || ctx == null || ctx.world() == null) {
			send(new ActionFailed());
			return;
		}
		NpcInstance bestNpc = null;
		double bestNpcDistSq = Double.MAX_VALUE;
		for (var npc : ctx.world().findNpcsAround(active.x(), active.y(), GameWorld.VISIBILITY_RADIUS)) {
			if (npc.name().toLowerCase(Locale.ROOT).startsWith(query)) {
				double dx = active.x() - npc.x();
				double dy = active.y() - npc.y();
				double distSq = dx * dx + dy * dy;
				if (distSq < bestNpcDistSq) {
					bestNpcDistSq = distSq;
					bestNpc = npc;
				}
			}
		}
		if (bestNpc != null) {
			session.targetObjectId(bestNpc.objectId());
			int levelDiff = active.level() - bestNpc.template().level();
			send(new MyTargetSelected(bestNpc.objectId(), levelDiff));
			send(StatusUpdate.hp(bestNpc.objectId(), (int) bestNpc.currentHp(), bestNpc.template().maxHp()));
			send(new ValidateLocation(bestNpc.objectId(), bestNpc.x(), bestNpc.y(), bestNpc.z(), bestNpc.heading()));
			return;
		}

		for (var other : ctx.world().players()) {
			if (other.objectId() != active.objectId()
					&& other.name().toLowerCase(Locale.ROOT).startsWith(query)) {
				session.targetObjectId(other.objectId());
				send(new MyTargetSelected(other.objectId(), 0));
				if (other.character() != null) {
					send(StatusUpdate.hp(other.objectId(), (int) other.character().currentHp(),
							other.character().maxHp()));
				}
				send(new ValidateLocation(other.objectId(), other.x(), other.y(), other.z(), 0));
				return;
			}
		}
		send(new ActionFailed());
	}

	public void handleDotCommand(String cmd) {
		PlayerCharacter active = active();
		var ctx = ctx();
		if (active == null || ctx == null) {
			return;
		}
		String trimmed = cmd.trim();
		String rawCmd = trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
		int space = rawCmd.indexOf(' ');
		String commandName = space > 0 ? rawCmd.substring(0, space).trim() : rawCmd;
		String params = space > 0 ? rawCmd.substring(space + 1).trim() : "";
		if (ctx.voicedCommands() != null && ctx.voicedCommands().hasCommand(commandName)) {
			if (ctx.voicedCommands().execute(commandName, session, params)) {
				return;
			}
		}
		String lower = cmd.toLowerCase(Locale.ROOT).trim();
		if (lower.equals(".online")) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + ctx.world().players().size()));
		} else if (lower.equals(".stats")) {
			var t = ctx.characters() != null ? ctx.characters().template(active) : null;
			var stats = PlayerStats.calculate(active, t);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
							active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
							stats.pAtkSpd(), stats.mAtkSpd())));
		} else if (lower.equals(".menu")) {
			if (ctx.preferences() != null) {
				send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Menu de preferencias indisponivel."));
			}
		} else if (lower.equals(".blockbuff")) {
			if (ctx.preferences() != null) {
				boolean blocked = ctx.preferences().toggleBlockBuffs(active.objectId());
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
			}
		} else if (lower.equals(".tvt")) {
			if (ctx.tvt() != null) {
				send(new NpcHtmlMessage(0, ctx.tvt().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento TvT desativado."));
			}
		} else if (lower.equals(".tvtjoin")) {
			if (ctx.tvt() != null) {
				var res = ctx.tvt().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "TvT: " + res.name()));
			}
		} else if (lower.equals(".tvtleave")) {
			if (ctx.tvt() != null) {
				boolean ok = ctx.tvt().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do TvT cancelada." : "Voce nao esta inscrito no TvT."));
			}
		} else if (lower.equals(".ctf")) {
			if (ctx.ctf() != null) {
				send(new NpcHtmlMessage(0, ctx.ctf().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento CTF desativado."));
			}
		} else if (lower.equals(".ctfjoin")) {
			if (ctx.ctf() != null) {
				var res = ctx.ctf().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "CTF: " + res.name()));
			}
		} else if (lower.equals(".ctfleave")) {
			if (ctx.ctf() != null) {
				boolean ok = ctx.ctf().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do CTF cancelada." : "Voce nao esta inscrito no CTF."));
			}
		} else if (lower.equals(".dm")) {
			if (ctx.dm() != null) {
				send(new NpcHtmlMessage(0, ctx.dm().buildStatusHtml()));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Evento DM desativado."));
			}
		} else if (lower.equals(".dmjoin")) {
			if (ctx.dm() != null) {
				var res = ctx.dm().register(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DM: " + res.name()));
			}
		} else if (lower.equals(".dmleave")) {
			if (ctx.dm() != null) {
				boolean ok = ctx.dm().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao do DM cancelada." : "Voce nao esta inscrito no DM."));
			}
		} else if (lower.equals(".aiomenu")) {
			if (ctx.aio() != null && active.isAio()) {
				send(new NpcHtmlMessage(0, ctx.aio().buildAioMenuHtml(active)));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem acessar este menu."));
			}
		} else if (lower.equals(".getaiogoods")) {
			if (ctx.aio() != null && active.isAio()) {
				for (var item : ctx.aio().getAioGoods()) {
					ctx.inventories().addItem(active.inventory(), item.itemId(), item.count(), "AioGoods");
				}
				send(ItemList.of(active.inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens com status AIOx podem receber consumiveis."));
			}
		} else if (lower.equals(".classmaster") || lower.equals(".class")) {
			session.showClassMasterMenu(0, 0);
		} else if (lower.equals(".offline")) {
			if (ctx.offlineTrade() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de loja offline desativado."));
				return;
			}
			if (active.privateStoreType() == 0 && !active.isBuffShop()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa estar com uma loja pessoal aberta para ativar o modo offline."));
				return;
			}
			List<OfflineShopItem> items = new ArrayList<>();
			items.add(new OfflineShopItem(57, 1, 1));
			boolean ok = ctx.offlineTrade().startOfflineTrade(
					active,
					active.privateStoreType() != 0 ? active.privateStoreType() : OfflineTradeService.STORE_PRIVATE_SELL,
					false,
					active.storeTitle(),
					items,
					OfflineTradeService.DEFAULT_OFFLINE_DURATION
			);
			if (ok) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado com sucesso! Desconectando sessao..."));
				active.sitting(true);
				session.close(new ServerClose());
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel ativar o modo offline."));
			}
		} else if (lower.equals(".deposit")) {
			if (!com.lopez.l2j.config.Config.BANKING_SYSTEM_ENABLED) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
				return;
			}
			int reqAdena = com.lopez.l2j.config.Config.BANKING_SYSTEM_ADENA;
			var adenaItem = active.inventory().byItemId(57).orElse(null);
			if (adenaItem == null || adenaItem.count() < reqAdena) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce precisa de " + reqAdena + " adena para comprar um Gold Bar."));
				return;
			}
			var consumed = ctx.inventories().consumeItem(active.inventory(), 57, reqAdena, "BankingDeposit");
			var added = ctx.inventories().addItem(active.inventory(), 3470, com.lopez.l2j.config.Config.BANKING_SYSTEM_GOLDBARS,
					"BankingDeposit");
			List<ItemInfo> updates = new java.util.ArrayList<>();
			if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
			if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			if (!updates.isEmpty()) {
				send(new InventoryUpdate(updates));
			}
			session.refreshWeightAndPenalties();
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Deposito realizado: Gold Bar adicionado ao seu inventario."));
		} else if (lower.equals(".withdraw")) {
			if (!com.lopez.l2j.config.Config.BANKING_SYSTEM_ENABLED) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
				return;
			}
			int reqBars = com.lopez.l2j.config.Config.BANKING_SYSTEM_GOLDBARS;
			var barItem = active.inventory().byItemId(3470).orElse(null);
			if (barItem == null || barItem.count() < reqBars) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Voce precisa de " + reqBars + " Gold Bar para sacar."));
				return;
			}
			var consumed = ctx.inventories().consumeItem(active.inventory(), 3470, reqBars, "BankingWithdraw");
			var added = ctx.inventories().addItem(active.inventory(), 57, com.lopez.l2j.config.Config.BANKING_SYSTEM_ADENA,
					"BankingWithdraw");
			List<ItemInfo> updates = new java.util.ArrayList<>();
			if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
			if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
			if (!updates.isEmpty()) {
				send(new InventoryUpdate(updates));
			}
			session.refreshWeightAndPenalties();
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Saque realizado: Adena adicionada ao seu inventario."));
		} else if (lower.equals(".gotolove")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			int partnerId = ctx.weddings().getPartnerId(active.objectId());
			if (partnerId == 0 || !ctx.weddings().isMarried(active.objectId())) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao e casado."));
				return;
			}
			var partnerOpt = ctx.world().player(partnerId);
			if (partnerOpt.isEmpty() || partnerOpt.get().character() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu parceiro(a) nao esta online."));
				return;
			}
			var partner = partnerOpt.get().character();
			if (!ctx.weddings().canTeleportToPartner(active, partner)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Nao e possivel teleportar para seu parceiro(a) no momento (combate/morte/karma)."));
				return;
			}
			session.teleportToLocation(partner.x(), partner.y(), partner.z());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Teleportado para seu parceiro(a)!"));
		} else if (lower.equals(".divorce")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			var coupleOpt = ctx.weddings().getCoupleForPlayer(active.objectId());
			if (coupleOpt.isEmpty()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui relacionamento ativo."));
				return;
			}
			int partnerId = ctx.weddings().getPartnerId(active.objectId());
			ctx.weddings().divorce(coupleOpt.get().id());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce esta oficialmente divorciado(a)."));
			ctx.world().player(partnerId).ifPresent(p -> {
				p.send(new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " se divorciou de voce."));
			});
		} else if (lower.equals(".engage")) {
			if (ctx.weddings() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de casamento desativado."));
				return;
			}
			if (session.targetObjectId() == 0 || session.targetObjectId() == active.objectId()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione seu pretendente antes de usar o comando."));
				return;
			}
			var targetOpt = ctx.world().player(session.targetObjectId());
			if (targetOpt.isEmpty() || targetOpt.get().character() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Alvo invalido para noivado."));
				return;
			}
			var targetChar = targetOpt.get().character();
			if (ctx.weddings().getCoupleForPlayer(active.objectId()).isPresent()
					|| ctx.weddings().getCoupleForPlayer(targetChar.objectId()).isPresent()) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Um de voces ja esta noivo ou casado."));
				return;
			}
			ctx.weddings().engage(active, targetChar);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce e " + targetChar.name() + " agora estao noivos!"));
			targetOpt.get().send(
					new CreatureSay(0, CreatureSay.ALL, "SYS", active.name() + " oficializou o noivado com voce!"));
		} else if (lower.equals(".autofarm") || lower.startsWith(".autofarm ") || lower.equals(".farm") || lower.startsWith(".farm ")) {
			if (ctx.autoFarm() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de Auto-Farm desativado."));
				return;
			}
			var skillService = ctx.skillService();
			String rawParam = lower.contains(" ") ? lower.substring(lower.indexOf(' ')).trim() : "";
			if (rawParam.isEmpty() || rawParam.equals("menu") || rawParam.equals("gui")) {
				send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.autoFarm().renderHtml(active, skillService)));
			} else if (rawParam.equals("on")) {
				if (!ctx.autoFarm().isAutoFarm(active.objectId())) {
					ctx.autoFarm().toggleAutoFarm(active);
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto-Farm ATIVADO."));
				send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.autoFarm().renderHtml(active, skillService)));
			} else if (rawParam.equals("off")) {
				if (ctx.autoFarm().isAutoFarm(active.objectId())) {
					ctx.autoFarm().toggleAutoFarm(active);
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto-Farm DESATIVADO."));
				send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.autoFarm().renderHtml(active, skillService)));
			} else {
				boolean activeState = ctx.autoFarm().toggleAutoFarm(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Auto-Farm " + (activeState ? "ATIVADO" : "DESATIVADO") + "."));
				send(new com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage(0, ctx.autoFarm().renderHtml(active, skillService)));
			}
		} else if (lower.equals(".dressme")) {
			if (ctx.dressMe() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema DressMe indisponivel."));
				return;
			}
			boolean state = ctx.dressMe().toggle(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DressMe " + (state ? "ativado" : "desativado") + "."));
			session.sendUserInfoAndBroadcastCharInfo();
		} else if (lower.equals(".undressme")) {
			if (ctx.dressMe() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema DressMe indisponivel."));
				return;
			}
			ctx.dressMe().removeArmorSkin(active);
			ctx.dressMe().removeWeaponSkin(active);
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Todas as skins DressMe foram removidas."));
			session.sendUserInfoAndBroadcastCharInfo();
		} else if (lower.equals(".buffshop") || lower.equals(".buffstore") || lower.equals(".buff_shop") || lower.equals(".buff_store")) {
			if (ctx.buffShop() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema Buff Shop desativado."));
				return;
			}
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			send(new NpcHtmlMessage(0, ctx.buffShop().renderSellerManageHtml(active, skillTable, 1)));
		} else if (lower.equals(".buybuff")) {
			if (ctx.buffShop() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema Buff Shop desativado."));
				return;
			}
			if (session.targetObjectId() == 0 || !ctx.buffShop().isBuffShop(session.targetObjectId())) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um vendedor de buffs ativo como alvo."));
				return;
			}
			var skillTable = ctx.skillService() != null ? ctx.skillService().table() : null;
			send(new NpcHtmlMessage(0, ctx.buffShop().renderBuyerShopHtml(active, session.targetObjectId(), skillTable, 1)));
		} else if (lower.startsWith(".augment")) {
			if (ctx.augmentation() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de augmentacao desativado."));
				return;
			}
			String[] parts = lower.split("\\s+");
			var weapon = session.activeWeapon();
			if (parts.length > 1 && weapon != null && ctx.augmentation().isAugmentable(weapon)) {
				try {
					int stoneId = Integer.parseInt(parts[1]);
					if (AugmentationService.isLifeStone(stoneId)) {
						ctx.augmentation().applyAugmentation(weapon, stoneId);
						session.updateAugmentationBonus();
						send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
						session.sendUserInfoAndBroadcastCharInfo();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arma equipada augmentada com Life Stone " + stoneId + "!"));
						return;
					}
				} catch (NumberFormatException ignored) {}
			}
			send(ExShowVariationMakeWindow.STATIC_PACKET);
		} else if (lower.equals(".unaugment")) {
			if (ctx.augmentation() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de augmentacao desativado."));
				return;
			}
			var weapon = session.activeWeapon();
			if (weapon != null && weapon.isAugmented()) {
				ctx.augmentation().removeAugmentation(weapon);
				session.updateAugmentationBonus();
				send(new InventoryUpdate(List.of(ItemInfo.of(weapon, ItemInfo.MODIFIED))));
				session.sendUserInfoAndBroadcastCharInfo();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Augmentacao da arma equipada removida!"));
				return;
			}
			send(ExShowVariationCancelWindow.STATIC_PACKET);
		} else if (lower.equals(".achieve") || lower.equals(".achievements")) {
			if (ctx.achievements() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de conquistas desativado."));
				return;
			}
			int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
			String html = ctx.achievements().generateHtml(active, adena, 0, 0, 0);
			send(new NpcHtmlMessage(0, html));
		} else if (lower.equals(".arena") || lower.equals(".duel")) {
			if (ctx.arenaDuel() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Arena 1x1 desativada."));
				return;
			}
			if (ctx.arenaDuel().isRegistered(active.objectId())) {
				ctx.arenaDuel().unregister(active);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscricao cancelada na Arena 1x1."));
			} else {
				boolean ok = ctx.arenaDuel().register(active);
				if (ok) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscrito na Arena 1x1! Aguardando oponente..."));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel se inscrever na Arena 1x1."));
				}
			}
		} else if (lower.equals(".event") || lower.equals(".events")) {
			if (ctx.officialEvent() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum evento oficial ativo no momento."));
				return;
			}
			send(new NpcHtmlMessage(0, ctx.officialEvent().generateHtml(active)));
		} else if (lower.equals(".roulette") || lower.startsWith(".roulette ")) {
			if (ctx.roulette() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de roleta desativado."));
				return;
			}
			String html = ctx.roulette().generateMainHtml(active);
			send(new NpcHtmlMessage(0, html));
		} else if (lower.equals(".reset") || lower.equals(".rebirth")) {
			if (ctx.characterReset() == null) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema de reset desativado."));
				return;
			}
			int adena = active.inventory().byItemId(57).map(ItemInstance::count).orElse(0);
			Map<Integer, Long> invMap = new HashMap<>();
			invMap.put(57, (long) adena);
			String html = ctx.characterReset().generateHtml(active, invMap);
			send(new NpcHtmlMessage(0, html));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Comandos de voz: .online, .stats, .classmaster, .offline, .deposit, .withdraw, .gotolove, .divorce, .engage, .dressme, .undressme, .buffshop, .buffstore, .buybuff, .augment, .unaugment, .autofarm, .achieve, .arena, .event, .roulette, .reset"));
		}
	}
}
