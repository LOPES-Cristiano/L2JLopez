package com.lopez.l2j.game.daily;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico de Recompensas Diarias de Presenca (NEXORA IL Daily Rewards) - ZERO DONATE.
 */
@Service
public class DailyRewardService {

	private static final Logger log = LoggerFactory.getLogger(DailyRewardService.class);

	public record DailyReward(int day, int itemId, long count, String name) {}
	public record ClaimResult(boolean success, int day, int itemId, long count, String message) {
		public static ClaimResult ok(int day, int itemId, long count, String msg) {
			return new ClaimResult(true, day, itemId, count, msg);
		}
		public static ClaimResult fail(String msg) {
			return new ClaimResult(false, 0, 0, 0L, msg);
		}
	}

	public record PlayerDailyStatus(LocalDate lastClaimDate, int claimedDays) {}

	private final Map<Integer, DailyReward> rewards = new ConcurrentHashMap<>();
	private final Map<Integer, PlayerDailyStatus> playerStatus = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(910000);

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	public DailyRewardService() {
		loadRewards();
	}

	public synchronized void loadRewards() {
		rewards.clear();
		try {
			File file = new File("data/xml/custom/DailyRewards.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/DailyRewards.xml");
			if (is == null) {
				log.warn("DailyRewards.xml nao encontrado, inserindo recompensas padrao.");
				rewards.put(1, new DailyReward(1, 57, 1000000L, "1kk Adena"));
				rewards.put(2, new DailyReward(2, 6392, 5L, "5 Event Medals"));
				rewards.put(3, new DailyReward(3, 3470, 1L, "1 Gold Bar"));
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList list = doc.getElementsByTagName("reward");

			for (int i = 0; i < list.getLength(); i++) {
				Element elem = (Element) list.item(i);
				int day = Integer.parseInt(elem.getAttribute("day"));
				int itemId = Integer.parseInt(elem.getAttribute("itemId"));
				long count = Long.parseLong(elem.getAttribute("count"));
				String name = elem.hasAttribute("name") ? elem.getAttribute("name") : "Day " + day + " Reward";

				rewards.put(day, new DailyReward(day, itemId, count, name));
			}

			log.info("DailyRewardService: {} dias de recompensas carregados com sucesso.", rewards.size());
		} catch (Exception e) {
			log.error("Erro ao carregar DailyRewards.xml: {}", e.getMessage(), e);
		}
	}

	public boolean canClaimToday(PlayerCharacter player) {
		if (player == null) return false;
		PlayerDailyStatus status = playerStatus.get(player.objectId());
		if (status == null) return true;
		return !LocalDate.now().equals(status.lastClaimDate());
	}

	public int getCurrentStreakDay(PlayerCharacter player) {
		if (player == null) return 1;
		PlayerDailyStatus status = playerStatus.get(player.objectId());
		if (status == null) return 1;
		int total = Math.max(1, rewards.size());
		return (status.claimedDays() % total) + 1;
	}

	public synchronized ClaimResult claimReward(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return ClaimResult.fail("Personagem ou inventario invalido.");
		}

		if (!canClaimToday(player)) {
			return ClaimResult.fail("Voce ja resgatou a recompensa diaria de hoje! Volte amanha.");
		}

		int nextDay = getCurrentStreakDay(player);
		DailyReward reward = rewards.get(nextDay);
		if (reward == null) {
			return ClaimResult.fail("Nenhuma recompensa configurada para o dia " + nextDay + ".");
		}

		Inventory inv = player.inventory();
		int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemTemplate tmpl = resolveTemplate(reward.itemId(), reward.name());
		ItemInstance item = new ItemInstance(nextId, tmpl, player.objectId(), (int) Math.min(Integer.MAX_VALUE, reward.count()));
		inv.addItem(item);

		PlayerDailyStatus oldStatus = playerStatus.get(player.objectId());
		int newCount = (oldStatus != null) ? oldStatus.claimedDays() + 1 : 1;
		playerStatus.put(player.objectId(), new PlayerDailyStatus(LocalDate.now(), newCount));

		return ClaimResult.ok(nextDay, reward.itemId(), reward.count(),
				"Recompensa do Dia " + nextDay + " resgatada com sucesso: " + reward.count() + "x " + reward.name() + "!");
	}

	public String getStatusHtml(PlayerCharacter player) {
		int currentDay = getCurrentStreakDay(player);
		boolean canClaim = canClaimToday(player);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\" size=4><b>=== RECOMPENSAS DIARIAS (.daily) ===</b></font><br><br>");
		sb.append("<table width=280 border=0 cellpadding=2>");
		sb.append("<tr><td>Status de Hoje:</td><td align=right>").append(canClaim ? "<font color=\"00FF00\">Disponivel</font>" : "<font color=\"FF0000\">Ja Resgatado</font>").append("</td></tr>");
		sb.append("<tr><td>Proximo Dia:</td><td align=right><font color=\"LEVEL\">Dia ").append(currentDay).append("</font></td></tr>");
		sb.append("</table><br>");

		if (canClaim) {
			sb.append("<button value=\"Resgatar Recompensa\" action=\"bypass -h voiced_daily claim\" width=160 height=26 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br>");
		} else {
			sb.append("<font color=\"AAAAAA\">Volte amanha para o proximo resgate!</font><br>");
		}

		sb.append("<br><font color=\"777777\" size=1>Servidor 100% In-Game: Zero Doacoes!</font>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "other", "asset", 100, "none", 0, true, true, true, true);
	}
}
