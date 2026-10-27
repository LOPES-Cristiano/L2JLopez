package com.lopez.l2j.game.daily;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
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
	private final Map<String, LocalDate> claimedIpDate = new ConcurrentHashMap<>();
	private final Map<String, LocalDate> claimedHwidDate = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(910000);

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	@Autowired(required = false)
	private InventoryService inventoryService;

	@Autowired(required = false)
	private ItemRepository itemRepository;

	@Autowired(required = false)
	private CharacterVariablesService variablesService;

	@Autowired(required = false)
	private JdbcClient jdbc;

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
		PlayerDailyStatus status = resolvePlayerStatus(player.objectId());
		if (status != null && LocalDate.now().equals(status.lastClaimDate())) {
			return false;
		}
		LocalDate today = LocalDate.now();
		String ip = resolveClientIp(player);
		if (ip != null && today.equals(claimedIpDate.get(ip))) {
			return false;
		}
		String hwid = resolveClientHwid(player);
		if (hwid != null && today.equals(claimedHwidDate.get(hwid))) {
			return false;
		}
		return true;
	}

	public int getCurrentStreakDay(PlayerCharacter player) {
		if (player == null) return 1;
		PlayerDailyStatus status = resolvePlayerStatus(player.objectId());
		if (status == null) return 1;
		int total = Math.max(1, rewards.size());
		return (status.claimedDays() % total) + 1;
	}

	private PlayerDailyStatus resolvePlayerStatus(int playerId) {
		PlayerDailyStatus status = playerStatus.get(playerId);
		if (status != null) {
			return status;
		}
		if (variablesService != null) {
			String dateStr = variablesService.getVariable(playerId, "daily_reward_last_date", null);
			int days = variablesService.getInt(playerId, "daily_reward_claimed_days", 0);
			if (dateStr != null && !dateStr.isBlank()) {
				try {
					PlayerDailyStatus loaded = new PlayerDailyStatus(LocalDate.parse(dateStr.trim()), days);
					playerStatus.put(playerId, loaded);
					return loaded;
				} catch (Exception ignored) {
				}
			}
		}
		if (jdbc != null) {
			try {
				var rows = jdbc.sql("SELECT last_claim_date, claimed_days FROM character_daily_rewards WHERE charId = :cid")
						.param("cid", playerId)
						.query().listOfRows();
				if (!rows.isEmpty()) {
					var r = rows.getFirst();
					String dStr = String.valueOf(r.get("last_claim_date"));
					int days = r.get("claimed_days") instanceof Number n ? n.intValue() : 0;
					PlayerDailyStatus loaded = new PlayerDailyStatus(LocalDate.parse(dStr), days);
					playerStatus.put(playerId, loaded);
					return loaded;
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	private void persistPlayerStatus(int playerId, PlayerDailyStatus status, String ip, String hwid) {
		if (variablesService != null) {
			variablesService.setVariable(playerId, "daily_reward_last_date", status.lastClaimDate().toString());
			variablesService.setVariable(playerId, "daily_reward_claimed_days", String.valueOf(status.claimedDays()));
		}
		if (jdbc != null) {
			try {
				jdbc.sql("""
						CREATE TABLE IF NOT EXISTS character_daily_rewards (
						    charId INT NOT NULL PRIMARY KEY,
						    last_claim_date VARCHAR(16) NOT NULL,
						    claimed_days INT NOT NULL DEFAULT 0,
						    last_ip VARCHAR(64) DEFAULT '',
						    last_hwid VARCHAR(128) DEFAULT ''
						)
						""").update();
				jdbc.sql("""
						INSERT INTO character_daily_rewards (charId, last_claim_date, claimed_days, last_ip, last_hwid)
						VALUES (:cid, :dt, :days, :ip, :hwid)
						ON DUPLICATE KEY UPDATE last_claim_date = :dt, claimed_days = :days, last_ip = :ip, last_hwid = :hwid
						""")
						.param("cid", playerId)
						.param("dt", status.lastClaimDate().toString())
						.param("days", status.claimedDays())
						.param("ip", ip != null ? ip : "")
						.param("hwid", hwid != null ? hwid : "")
						.update();
			} catch (Exception ignored) {
			}
		}
	}

	private static String resolveClientIp(PlayerCharacter player) {
		if (player == null) return null;
		try {
			var m = player.getClass().getMethod("lastKnownIp");
			Object v = m.invoke(player);
			if (v instanceof String s && !s.isBlank() && !"127.0.0.1".equals(s.trim()) && !"0.0.0.0".equals(s.trim())) {
				return s.trim();
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static String resolveClientHwid(PlayerCharacter player) {
		if (player == null) return null;
		try {
			var m = player.getClass().getMethod("lastKnownHwid");
			Object v = m.invoke(player);
			if (v instanceof String s && !s.isBlank()) {
				return s.trim();
			}
		} catch (Exception ignored) {
		}
		return null;
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
		int count = (int) Math.min(Integer.MAX_VALUE, reward.count());
		if (inventoryService != null) {
			var addRes = inventoryService.addItem(inv, reward.itemId(), count, "DailyReward");
			if (addRes == null) {
				int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
				ItemTemplate tmpl = resolveTemplate(reward.itemId(), reward.name());
				ItemInstance item = new ItemInstance(nextId, tmpl, player.objectId(), count);
				inv.addItem(item);
				if (itemRepository != null) {
					itemRepository.insert(item, "DailyReward");
				}
				GameWorld gw = GameWorld.getInstance();
				if (gw != null) {
					gw.sendPacket(player.objectId(), new InventoryUpdate(List.of(ItemInfo.of(item, ItemInfo.ADDED))));
				}
			} else {
				GameWorld gw = GameWorld.getInstance();
				if (gw != null && addRes.item() != null) {
					gw.sendPacket(player.objectId(), new InventoryUpdate(List.of(
							ItemInfo.of(addRes.item(), addRes.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
				}
			}
		} else {
			int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
			ItemTemplate tmpl = resolveTemplate(reward.itemId(), reward.name());
			ItemInstance item = new ItemInstance(nextId, tmpl, player.objectId(), count);
			inv.addItem(item);
			if (itemRepository != null) {
				itemRepository.insert(item, "DailyReward");
			}
			GameWorld gw = GameWorld.getInstance();
			if (gw != null) {
				gw.sendPacket(player.objectId(), ItemList.of(inv.items(), false));
			}
		}

		PlayerDailyStatus oldStatus = resolvePlayerStatus(player.objectId());
		int newCount = (oldStatus != null) ? oldStatus.claimedDays() + 1 : 1;
		LocalDate today = LocalDate.now();
		PlayerDailyStatus newStatus = new PlayerDailyStatus(today, newCount);
		playerStatus.put(player.objectId(), newStatus);

		String ip = resolveClientIp(player);
		if (ip != null) {
			claimedIpDate.put(ip, today);
		}
		String hwid = resolveClientHwid(player);
		if (hwid != null) {
			claimedHwidDate.put(hwid, today);
		}
		persistPlayerStatus(player.objectId(), newStatus, ip, hwid);

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
			sb.append("<button value=\"Resgatar Recompensa\" action=\"bypass -h voiced_daily claim\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"><br>");
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
