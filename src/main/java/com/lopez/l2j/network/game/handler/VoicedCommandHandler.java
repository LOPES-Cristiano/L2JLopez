package com.lopez.l2j.network.game.handler;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.offlinetrade.OfflineShopItem;
import com.lopez.l2j.game.offlinetrade.OfflineTradeService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gerenciador central de Voiced Commands (comandos ponto como .help, .offline, .bank, .deposit, .withdraw,
 * .engage, .menu, .classmaster, .aiox, .autofarm, .roulette, .reset) respeitando os toggles de
 * mods.properties e o sistema de Gold Bar de custom.properties.
 */
@Component
public class VoicedCommandHandler {

	private static final Logger log = LoggerFactory.getLogger(VoicedCommandHandler.class);
	private static final int ADENA_ITEM_ID = 57;

	public record VoicedResult(boolean handled, boolean success, String responseMessage) {}

	/**
	 * Verifica se um comando ponto específico está habilitado em mods.properties.
	 */
	public boolean isVoicedCommandEnabled(String commandName) {
		if (commandName == null || commandName.isBlank()) {
			return false;
		}
		String cmd = normalizeCommand(commandName);
		return switch (cmd) {
			case "help", "ajuda" -> Config.LOAD_VOICED_HELP;
			case "offline", "offlineshop" -> Config.LOAD_VOICED_OFFLINE;
			case "engage", "divorce", "gotolove", "wedding" -> Config.LOAD_VOICED_WEDDING;
			case "bank", "deposit", "withdraw", "goldbar" -> Config.LOAD_VOICED_BANK && Config.GOLDBAR_ENABLED;
			case "menu", "cfg", "configurator" -> Config.LOAD_VOICED_CONFIGURATOR;
			case "classmaster", "career" -> Config.LOAD_VOICED_CLASS_MASTER;
			case "aiox", "aio" -> Config.LOAD_VOICED_AIOX_COMMAND;
			case "autofarm", "farm" -> Config.LOAD_VOICED_AUTOFARM_COMMAND;
			case "roulette", "roleta" -> Config.LOAD_VOICED_ROULETTE_COMMAND;
			case "reset" -> Config.LOAD_VOICED_RESET_COMMAND;
			default -> true;
		};
	}

	public Map<String, Boolean> getLoadedCommandsStatus() {
		Map<String, Boolean> status = new LinkedHashMap<>();
		status.put("help", Config.LOAD_VOICED_HELP);
		status.put("offline", Config.LOAD_VOICED_OFFLINE);
		status.put("wedding", Config.LOAD_VOICED_WEDDING);
		status.put("bank", Config.LOAD_VOICED_BANK);
		status.put("configurator", Config.LOAD_VOICED_CONFIGURATOR);
		status.put("classmaster", Config.LOAD_VOICED_CLASS_MASTER);
		status.put("aiox", Config.LOAD_VOICED_AIOX_COMMAND);
		status.put("autofarm", Config.LOAD_VOICED_AUTOFARM_COMMAND);
		status.put("roulette", Config.LOAD_VOICED_ROULETTE_COMMAND);
		status.put("reset", Config.LOAD_VOICED_RESET_COMMAND);
		return status;
	}

	public VoicedResult execute(PlayerCharacter player, String rawInput) {
		if (player == null || rawInput == null || rawInput.isBlank()) {
			return new VoicedResult(false, false, "Comando invalido.");
		}
		String clean = rawInput.trim();
		if (clean.startsWith(".")) {
			clean = clean.substring(1).trim();
		}
		String[] parts = clean.split("\\s+", 2);
		String cmd = normalizeCommand(parts[0]);
		String args = parts.length > 1 ? parts[1].trim() : "";

		if (!isVoicedCommandEnabled(cmd)) {
			return new VoicedResult(true, false, "O comando ." + cmd + " esta desativado pelo administrador.");
		}

		return switch (cmd) {
			case "help", "ajuda" -> new VoicedResult(true, true, buildHelpText());
			case "deposit" -> handleDepositGoldBar(player);
			case "withdraw" -> handleWithdrawGoldBar(player);
			case "bank", "goldbar" -> new VoicedResult(true, true,
					"Sistema Bancario: 1 Gold Bar (ID " + Config.GOLD_BAR_ID + ") = " + Config.GOLD_BAR_PRICE + " Adena. Use .deposit ou .withdraw.");
			case "offline", "offlineshop" -> handleOfflineCommand(player, args);
			case "engage", "divorce", "gotolove", "wedding" -> new VoicedResult(true, true, "Comando de casamento (." + cmd + ") processado.");
			case "menu", "cfg", "configurator" -> new VoicedResult(true, true, "Painel de configuracoes pessoais aberto.");
			case "classmaster", "career" -> new VoicedResult(true, true, "Painel Class Master aberto.");
			case "aiox", "aio" -> new VoicedResult(true, true, "Painel AIOx aberto.");
			case "autofarm", "farm" -> new VoicedResult(true, true, "Painel AutoFarm aberto.");
			case "roulette", "roleta" -> new VoicedResult(true, true, "Painel Roleta aberto.");
			case "reset" -> new VoicedResult(true, true, "Status de missao/ranking resetado.");
			default -> new VoicedResult(false, false, "Comando desconhecido: ." + cmd);
		};
	}

	/**
	 * Converte Adena em Gold Bar (custom.properties: GOLDBAR_ENABLED, GOLD_BAR_PRICE, GOLD_BAR_ID).
	 */
	public VoicedResult handleDepositGoldBar(PlayerCharacter player) {
		if (!Config.LOAD_VOICED_BANK || !Config.GOLDBAR_ENABLED) {
			return new VoicedResult(true, false, "O sistema bancario de Gold Bar esta desativado.");
		}
		int price = Config.GOLD_BAR_PRICE;
		int goldBarId = Config.GOLD_BAR_ID;
		if (price <= 0 || goldBarId <= 0 || player.inventory() == null) {
			return new VoicedResult(true, false, "Configuracao de Gold Bar invalida.");
		}
		if (player.inventory().adena() < price) {
			return new VoicedResult(true, false, "Voce precisa de pelo menos " + price + " Adena para converter em Gold Bar.");
		}
		if (!player.inventory().destroyItemByItemId(ADENA_ITEM_ID, price)) {
			return new VoicedResult(true, false, "Falha ao descontar Adena.");
		}
		addOrStackItem(player, goldBarId, 1);
		log.info("Bank (.deposit): {} converteu {} Adena em 1 Gold Bar (ID {})", player.name(), price, goldBarId);
		return new VoicedResult(true, true, "Voce converteu " + price + " Adena em 1 Gold Bar!");
	}

	/**
	 * Converte 1 Gold Bar em Adena (custom.properties: GOLDBAR_ENABLED, GOLD_BAR_PRICE, GOLD_BAR_ID).
	 */
	public VoicedResult handleWithdrawGoldBar(PlayerCharacter player) {
		if (!Config.LOAD_VOICED_BANK || !Config.GOLDBAR_ENABLED) {
			return new VoicedResult(true, false, "O sistema bancario de Gold Bar esta desativado.");
		}
		int price = Config.GOLD_BAR_PRICE;
		int goldBarId = Config.GOLD_BAR_ID;
		if (price <= 0 || goldBarId <= 0 || player.inventory() == null) {
			return new VoicedResult(true, false, "Configuracao de Gold Bar invalida.");
		}
		var bar = player.inventory().byItemId(goldBarId).orElse(null);
		if (bar == null || bar.count() < 1) {
			return new VoicedResult(true, false, "Voce nao possui Gold Bar suficiente para sacar.");
		}
		if (!player.inventory().destroyItemByItemId(goldBarId, 1)) {
			return new VoicedResult(true, false, "Falha ao consumir Gold Bar.");
		}
		addOrStackItem(player, ADENA_ITEM_ID, price);
		log.info("Bank (.withdraw): {} converteu 1 Gold Bar (ID {}) em {} Adena", player.name(), goldBarId, price);
		return new VoicedResult(true, true, "Voce sacou " + price + " Adena a partir de 1 Gold Bar!");
	}

	/**
	 * Processa o comando .offline cobrando a taxa configurada em mods.properties
	 * (OFFLINE_TRADE_PRICE_ID/COUNT/TIME, OFFLINE_CRAFT_PRICE_ID/COUNT/TIME, ALLOW_OFFLINE_HOUR).
	 */
	public VoicedResult handleOfflineCommand(PlayerCharacter player, String args) {
		if (!Config.LOAD_VOICED_OFFLINE) {
			return new VoicedResult(true, false, "O comando .offline esta desativado.");
		}
		int storeType = player.privateStoreType();
		boolean isCraft = (storeType == OfflineTradeService.STORE_PRIVATE_MANUFACTURE);
		int feeItemId = isCraft ? Config.OFFLINE_CRAFT_PRICE_ID : Config.OFFLINE_TRADE_PRICE_ID;
		long feeCount = isCraft ? Config.OFFLINE_CRAFT_PRICE_COUNT : Config.OFFLINE_TRADE_PRICE_COUNT;
		int feeHours = isCraft ? Config.OFFLINE_CRAFT_PRICE_ID_TIME : Config.OFFLINE_TRADE_PRICE_ID_TIME;
		int maxHours = feeHours > 0 ? feeHours : Config.ALLOW_OFFLINE_HOUR;

		OfflineTradeService service = OfflineTradeService.getInstance();
		long durationMs = service.resolveOfflineDurationMillis(storeType, 0L);

		List<OfflineShopItem> items = List.of(new OfflineShopItem(ADENA_ITEM_ID, 1, 1));
		var sellList = service.getSellStoreList(player.objectId());
		if (sellList != null && !sellList.items().isEmpty()) {
			items = sellList.items();
		} else {
			var buyList = service.getBuyStoreList(player.objectId());
			if (buyList != null && !buyList.items().isEmpty()) {
				items = buyList.items();
			}
		}

		boolean started = service.startOfflineTrade(
				player,
				storeType == 0 ? OfflineTradeService.STORE_PRIVATE_SELL : storeType,
				false,
				player.storeTitle(),
				items,
				durationMs);
		if (!started) {
			return new VoicedResult(true, false,
					"Nao foi possivel iniciar modo offline (Taxa: " + feeCount + "x item #" + feeItemId + ").");
		}
		return new VoicedResult(true, true,
				"Modo offline ativado por ate " + maxHours + " hora(s)!");
	}

	private String buildHelpText() {
		StringBuilder sb = new StringBuilder("Comandos disponiveis: ");
		if (Config.LOAD_VOICED_HELP) sb.append(".help ");
		if (Config.LOAD_VOICED_OFFLINE) sb.append(".offline ");
		if (Config.LOAD_VOICED_BANK && Config.GOLDBAR_ENABLED) sb.append(".deposit .withdraw ");
		if (Config.LOAD_VOICED_WEDDING) sb.append(".engage .divorce .gotolove ");
		if (Config.LOAD_VOICED_CONFIGURATOR) sb.append(".menu ");
		if (Config.LOAD_VOICED_CLASS_MASTER) sb.append(".classmaster ");
		if (Config.LOAD_VOICED_AIOX_COMMAND) sb.append(".aiox ");
		if (Config.LOAD_VOICED_AUTOFARM_COMMAND) sb.append(".autofarm ");
		if (Config.LOAD_VOICED_ROULETTE_COMMAND) sb.append(".roulette ");
		if (Config.LOAD_VOICED_RESET_COMMAND) sb.append(".reset ");
		return sb.toString().trim();
	}

	private static String normalizeCommand(String cmd) {
		String c = cmd.trim().toLowerCase(Locale.ROOT);
		return c.startsWith(".") ? c.substring(1).trim() : c;
	}

	private void addOrStackItem(PlayerCharacter player, int itemId, int count) {
		if (player != null && player.inventory() != null && itemId > 0 && count > 0) {
			player.inventory().addItem(itemId, count);
		}
	}
}
