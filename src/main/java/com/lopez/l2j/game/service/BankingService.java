package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico Bancario do Servidor (.bank, .deposit, .withdraw).
 * Converte Adena (ID 57) em Gold Bars (ID 3470) e vice-versa.
 * Operacao protegida contra overflow de Adena (Integer.MAX_VALUE = 2.147.483.647).
 */
@Service
public class BankingService {

	private static final Logger log = LoggerFactory.getLogger(BankingService.class);

	public static final int ADENA_ID = 57;
	public static final int GOLD_BAR_ID = 3470;

	public boolean isEnabled() {
		return Config.BANKING_SYSTEM_ENABLED;
	}

	public int getAdenaPerBar() {
		return Config.BANKING_SYSTEM_ADENA > 0 ? Config.BANKING_SYSTEM_ADENA : 500_000_000;
	}

	public int getBarsPerTransaction() {
		return Config.BANKING_SYSTEM_GOLDBARS > 0 ? Config.BANKING_SYSTEM_GOLDBARS : 1;
	}

	/**
	 * Converte Adena em Gold Bar.
	 */
	public boolean deposit(GameSession session, int barCount) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		if (!isEnabled()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco", "O sistema bancario esta desativado no momento."));
			return false;
		}
		int count = Math.max(1, barCount);
		var active = session.activeCharacter();
		var ctx = session.context();
		long adenaPerBar = getAdenaPerBar();
		long totalAdenaNeeded = adenaPerBar * count;

		if (totalAdenaNeeded > Integer.MAX_VALUE) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco", "Quantidade solicitada excede o limite por transacao."));
			return false;
		}

		var adenaItem = active.inventory().byItemId(ADENA_ID).orElse(null);
		if (adenaItem == null || adenaItem.count() < totalAdenaNeeded) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco",
					String.format("Voce precisa de %,d adenas para comprar %d Gold Bar(s).", totalAdenaNeeded, count)));
			return false;
		}

		var consumed = ctx.inventories().consumeItem(active.inventory(), ADENA_ID, (int) totalAdenaNeeded, "BankingDeposit");
		var added = ctx.inventories().addItem(active.inventory(), GOLD_BAR_ID, count, "BankingDeposit");
		List<ItemInfo> updates = new ArrayList<>();
		if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
		}
		session.refreshWeightAndPenalties();
		session.send(new CreatureSay(0, CreatureSay.ALL, "Banco",
				String.format("Deposito efetuado: %d Gold Bar(s) adicionado(s) por %,d adenas.", count, totalAdenaNeeded)));
		log.info("Player {} converteu {} adenas em {} Gold Bars", active.name(), totalAdenaNeeded, count);
		return true;
	}

	/**
	 * Converte Gold Bar em Adena, com protecao estrita contra estouro de saldo (Integer.MAX_VALUE).
	 */
	public boolean withdraw(GameSession session, int barCount) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		if (!isEnabled()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco", "O sistema bancario esta desativado no momento."));
			return false;
		}
		int count = Math.max(1, barCount);
		var active = session.activeCharacter();
		var ctx = session.context();

		var barItem = active.inventory().byItemId(GOLD_BAR_ID).orElse(null);
		if (barItem == null || barItem.count() < count) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco",
					String.format("Voce precisa de %d Gold Bar(s) para realizar este saque.", count)));
			return false;
		}

		long adenaPerBar = getAdenaPerBar();
		long totalAdenaGain = adenaPerBar * count;
		long currentAdena = active.inventory().byItemId(ADENA_ID).map(i -> (long) i.count()).orElse(0L);

		// Protecao contra estouro do limite de 2.147.483.647
		if (currentAdena + totalAdenaGain > Integer.MAX_VALUE) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "Banco",
					"Operacao cancelada: o saldo final excederia o limite maximo de adena suportado no inventario."));
			return false;
		}

		var consumed = ctx.inventories().consumeItem(active.inventory(), GOLD_BAR_ID, count, "BankingWithdraw");
		var added = ctx.inventories().addItem(active.inventory(), ADENA_ID, (int) totalAdenaGain, "BankingWithdraw");
		List<ItemInfo> updates = new ArrayList<>();
		if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
		if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
		if (!updates.isEmpty()) {
			session.send(new InventoryUpdate(updates));
		}
		session.refreshWeightAndPenalties();
		session.send(new CreatureSay(0, CreatureSay.ALL, "Banco",
				String.format("Saque efetuado: %,d adenas recebidas por %d Gold Bar(s).", totalAdenaGain, count)));
		log.info("Player {} converteu {} Gold Bars em {} adenas", active.name(), count, totalAdenaGain);
		return true;
	}

	/**
	 * Exibe o painel interativo do banco (.bank).
	 */
	public void showBankHelp(GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return;
		}
		var active = session.activeCharacter();
		long currentAdena = active.inventory().byItemId(ADENA_ID).map(i -> (long) i.count()).orElse(0L);
		int currentBars = active.inventory().byItemId(GOLD_BAR_ID).map(i -> i.count()).orElse(0);
		long adenaRate = getAdenaPerBar();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Sistema Bancario (.bank) ===</font><br>");
		sb.append("Taxa de Cambio: 1 Gold Bar = <font color=\"00FF00\">").append(String.format("%,d", adenaRate)).append("</font> Adenas<br><br>");
		sb.append("Seu Saldo:<br>");
		sb.append("Adena: <font color=\"FFFF00\">").append(String.format("%,d", currentAdena)).append("</font><br>");
		sb.append("Gold Bars: <font color=\"FFA500\">").append(currentBars).append("</font><br><br>");
		sb.append("<table width=240>");
		sb.append("<tr>");
		sb.append("<td><button value=\"Depositar (1 Bar)\" action=\"bypass -h voiced_deposit\" width=115 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("<td><button value=\"Sacar (1 Bar)\" action=\"bypass -h voiced_withdraw\" width=115 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
		sb.append("</tr>");
		sb.append("</table><br>");
		sb.append("<font color=\"LEVEL\">Comandos de Chat:</font><br>");
		sb.append("<font color=\"B09878\">.deposit</font> - Converte ").append(String.format("%,d", adenaRate)).append(" adena em 1 Gold Bar.<br>");
		sb.append("<font color=\"B09878\">.withdraw</font> - Converte 1 Gold Bar em ").append(String.format("%,d", adenaRate)).append(" adena.<br>");
		sb.append("</center></body></html>");

		session.send(new NpcHtmlMessage(0, sb.toString()));
	}
}
