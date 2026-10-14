package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de pacotes AIOx Seller, classes permitidas, moedas de compra,
 * anuncio de login AIOx e permissao de loja de buffs offline (aiox.properties / add-on.properties).
 */
@Service
public class AioSystemService {

	private static final Logger log = LoggerFactory.getLogger(AioSystemService.class);

	public record AioPackageOption(int tier, int days, int itemId, int itemCount, String coinText) {}

	public List<AioPackageOption> getAvailablePackages() {
		String coin = getCoinText();
		int itemId = Config.AIO_ITEM_ID;
		List<AioPackageOption> packages = new ArrayList<>(3);
		if (Config.AIO_DIAS > 0) {
			packages.add(new AioPackageOption(1, Config.AIO_DIAS, itemId, Config.AIO_ITEM_COUNT, coin));
		}
		if (Config.AIO_DIAS_2 > 0) {
			packages.add(new AioPackageOption(2, Config.AIO_DIAS_2, itemId, Config.AIO_ITEM_COUNT_2, coin));
		}
		if (Config.AIO_DIAS_3 > 0) {
			packages.add(new AioPackageOption(3, Config.AIO_DIAS_3, itemId, Config.AIO_ITEM_COUNT_3, coin));
		}
		return packages;
	}

	public int getPackageDays(int tier) {
		return switch (tier) {
			case 1 -> Config.AIO_DIAS;
			case 2 -> Config.AIO_DIAS_2;
			case 3 -> Config.AIO_DIAS_3;
			default -> 0;
		};
	}

	public int getPackageCost(int tier) {
		return switch (tier) {
			case 1 -> Config.AIO_ITEM_COUNT;
			case 2 -> Config.AIO_ITEM_COUNT_2;
			case 3 -> Config.AIO_ITEM_COUNT_3;
			default -> 0;
		};
	}

	public int getRequiredItemId() {
		return Config.AIO_ITEM_ID;
	}

	public String getCoinText() {
		return (Config.AIO_COIN_TEXT != null && !Config.AIO_COIN_TEXT.isBlank())
				? Config.AIO_COIN_TEXT.trim()
				: "Coin";
	}

	public List<String> getAllowedClassNames() {
		String raw = Config.AIO_CLASSES_NAME;
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (String part : raw.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				result.add(trimmed);
			}
		}
		return result;
	}

	public boolean isClassAllowedForAio(String className) {
		List<String> allowed = getAllowedClassNames();
		if (allowed.isEmpty()) {
			return true;
		}
		if (className == null || className.isBlank()) {
			return false;
		}
		for (String c : allowed) {
			if (c.equalsIgnoreCase(className.trim())) {
				return true;
			}
		}
		return false;
	}

	public boolean isOfflineBuffAllowed() {
		return Config.ALLOW_OFFLINE_BUFF;
	}

	public boolean purchaseAioPackage(PlayerCharacter player, int tier, String className) {
		if (player == null || player.isDead() || player.isOlympiadMode()) {
			return false;
		}
		int days = getPackageDays(tier);
		int cost = getPackageCost(tier);
		int itemId = Config.AIO_ITEM_ID;
		if (days <= 0 || cost < 0 || itemId <= 0) {
			return false;
		}
		if (className != null && !className.isBlank() && !isClassAllowedForAio(className)) {
			log.info("AIOx Seller: classe '{}' nao permitida para {}", className, player.name());
			return false;
		}
		if (cost > 0) {
			var inv = player.inventory();
			if (inv == null) {
				return false;
			}
			var currency = inv.byItemId(itemId).orElse(null);
			if (currency == null || currency.count() < cost) {
				return false;
			}
			if (!inv.destroyItemByItemId(itemId, cost)) {
				return false;
			}
		}

		long now = System.currentTimeMillis();
		long baseTime = (player.isAio() && player.aioExpiration() > now) ? player.aioExpiration() : now;
		long newExpiration = baseTime + (days * 86_400_000L);
		player.setAio(true);
		player.aioExpiration(newExpiration);
		player.rewardAioSkills();

		if (Config.ALLOW_AIO_NCOLOR && Config.AIO_NCOLOR != 0) {
			player.nameColor(Config.AIO_NCOLOR);
		}
		if (Config.ALLOW_AIO_TCOLOR && Config.AIO_TCOLOR != 0) {
			player.titleColor(Config.AIO_TCOLOR);
		}
		log.info("Jogador {} adquiriu pacote AIOx tier {} ({} dias por {} {})",
				player.name(), tier, days, cost, getCoinText());
		return true;
	}

	public String buildLoginAnnouncement(PlayerCharacter player) {
		if (player == null || !player.isAio() || !Config.ANNOUNCE_AIOX_LOGIN) {
			return null;
		}
		return "O jogador AIOx " + player.name() + " acabou de entrar no mundo!";
	}

	public String renderAioSellerHtml(PlayerCharacter player) {
		String coin = getCoinText();
		List<String> classes = getAllowedClassNames();
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== AIOx Seller ===</font><br>");
		if (!classes.isEmpty()) {
			sb.append("<font color=\"AAAAAA\">Classes elegiveis: ")
					.append(String.join(", ", classes))
					.append("</font><br><br>");
		}
		for (AioPackageOption pkg : getAvailablePackages()) {
			sb.append(String.format(Locale.ROOT,
					"<button value=\"%d Dias (%d %s)\" action=\"bypass -h aio_buy %d\" width=200 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br1>",
					pkg.days(), pkg.itemCount(), coin, pkg.tier()));
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
