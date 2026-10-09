package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.buffshop.BuffShopService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ServerClose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para comandos de voz do sistema Buff Shop:
 * .buffshop, .buffstore, .buff_shop, .buff_store, .buybuff
 */
@Component
public class VoicedBuffShopHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"buffshop",
			"buffstore",
			"buff_shop",
			"buff_store",
			"buybuff"
	);

	private final BuffShopService buffShopService;

	@Autowired
	public VoicedBuffShopHandler(BuffShopService buffShopService) {
		this.buffShopService = buffShopService;
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null || buffShopService == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();
		var skillTable = ctx != null && ctx.skillService() != null ? ctx.skillService().table() : null;

		String lower = command.toLowerCase(Locale.ROOT);
		if (lower.equals("buybuff")) {
			int targetId = session.targetObjectId();
			if (targetId == 0 || !buffShopService.isBuffShop(targetId)) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione um vendedor de Buff Shop como alvo."));
				return true;
			}
			session.send(new NpcHtmlMessage(0, buffShopService.renderBuyerShopHtml(active, targetId, skillTable, 1)));
			return true;
		}

		if (params != null && !params.isBlank()) {
			String[] parts = params.split("\\s+", 2);
			String sub = parts[0].toLowerCase(Locale.ROOT);
			switch (sub) {
				case "start" -> {
					var draft = buffShopService.getDraftOrActiveItems(active, skillTable);
					if (draft.isEmpty()) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Selecione ao menos um buff antes de iniciar a loja."));
						session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
						return true;
					}
					String title = buffShopService.getDraftTitle(active);
					boolean ok = buffShopService.startShop(active, title, List.copyOf(draft.values()));
					if (ok) {
						session.sendUserInfoAndBroadcastCharInfo();
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff Shop iniciada com sucesso! Titulo: " + title));
					}
					session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
					return true;
				}
				case "stop" -> {
					buffShopService.stopShop(active);
					session.sendUserInfoAndBroadcastCharInfo();
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Buff Shop encerrada."));
					session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
					return true;
				}
				case "offline" -> {
					if (!buffShopService.isBuffShop(active.objectId())) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa estar com a Buff Shop aberta para entrar em modo offline."));
						session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
						return true;
					}
					var shopOpt = buffShopService.getShop(active.objectId());
					if (shopOpt.isPresent()) {
						buffShopService.saveOfflineShop(shopOpt.get());
						if (ctx != null && ctx.offlineTrade() != null) {
							ctx.offlineTrade().startOfflineTrade(active, 1, false, active.storeTitle(),
									List.of(new com.lopez.l2j.game.offlinetrade.OfflineShopItem(57, 1, 1)),
									com.lopez.l2j.game.offlinetrade.OfflineTradeService.DEFAULT_OFFLINE_DURATION);
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Modo offline ativado! Sua loja continuara online. Desconectando..."));
						session.close(new ServerClose());
					}
					return true;
				}
				case "title" -> {
					if (parts.length > 1 && !parts[1].isBlank()) {
						buffShopService.setDraftTitle(active, parts[1].trim());
						if (buffShopService.isBuffShop(active.objectId())) {
							active.storeTitle(parts[1].trim());
							session.sendUserInfoAndBroadcastCharInfo();
						}
						session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Titulo da loja alterado para: " + parts[1].trim()));
					}
					session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
					return true;
				}
			}
		}

		session.send(new NpcHtmlMessage(0, buffShopService.renderSellerManageHtml(active, skillTable, 1)));
		return true;
	}
}
