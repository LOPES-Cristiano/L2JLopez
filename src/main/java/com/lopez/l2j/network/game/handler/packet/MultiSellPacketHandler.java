package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler modular para operacoes de MultiSell, Blacksmith of Mammon, Wear (Try-On),
 * Pet Tickets, Clan Hall Auction Currency e GoldBar Exchange (custom.properties / options.properties).
 */
@Component
public class MultiSellPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(MultiSellPacketHandler.class);
	private static final int ADENA_ITEM_ID = 57;

	public boolean doesBlacksmithRequireRecipes() {
		return Config.ALT_BLACKSMITH_USE_RECIPES;
	}

	public int getWearDelaySeconds() {
		return Math.max(1, Config.WEAR_DELAY);
	}

	public long getWearDelayMillis() {
		return getWearDelaySeconds() * 1000L;
	}

	public int getWearPriceAdena() {
		return Math.max(0, Config.WEAR_PRICE);
	}

	public boolean processWearPreview(PlayerCharacter player, int itemId) {
		if (player == null || player.isDead() || itemId <= 0) {
			return false;
		}
		int price = getWearPriceAdena();
		if (price > 0 && player.inventory() != null && !player.inventory().items().isEmpty()) {
			if (player.inventory().adena() < price) {
				return false;
			}
			if (!player.inventory().destroyItemByItemId(ADENA_ITEM_ID, price)) {
				return false;
			}
		}
		if (Config.LOG_ITEMS && !CharacterPacketHandler.getIgnoredLogItemIds().contains(itemId)) {
			log.info("[WearPreview] player={} itemId={} durationSec={} price={}",
					player.name(), itemId, getWearDelaySeconds(), price);
		}
		return true;
	}

	public boolean isValidPetTicket(int itemId, boolean specialPet) {
		if (specialPet) {
			return itemId > 0 && itemId == Config.SPECIAL_PET_TICKET_ID;
		}
		return itemId > 0 && (itemId == Config.PET_TICKET_ID || itemId == Config.SPECIAL_PET_TICKET_ID);
	}

	public int getAuctionBidCurrencyItemId() {
		return Config.AUCTION_BID_ITEM_ID > 0 ? Config.AUCTION_BID_ITEM_ID : ADENA_ITEM_ID;
	}

	public boolean isGoldBarExchangeEnabled() {
		return Config.GOLDBAR_ENABLED && Config.GOLD_BAR_ID > 0 && Config.GOLD_BAR_PRICE > 0;
	}

	public boolean areCustomTablesEnabled() {
		return Config.LOAD_CUSTOM_TELEPORTS
				|| Config.LOAD_CUSTOM_ITEM_TABLES
				|| Config.LOAD_CUSTOM_NPC_TABLE
				|| Config.LOAD_CUSTOM_DROPLIST_TABLE
				|| Config.LOAD_CUSTOM_ARMOR_SET_TABLE
				|| Config.LOAD_CUSTOM_SPAWNLIST_TABLE;
	}
}
