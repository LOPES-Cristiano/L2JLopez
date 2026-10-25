package com.lopez.l2j.game.extractable;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico para abertura e extracao de itens (caixas, sacos, peixes, baus de tesouro).
 * Porta de ExtractableItems / Extractable do legado L2JDream.
 */
@Service
public class ExtractableItemService {

	private static final Logger log = LoggerFactory.getLogger(ExtractableItemService.class);

	private final ExtractableItemsTable extractableItemsTable;
	private final InventoryService inventoryService;

	@Autowired
	public ExtractableItemService(ExtractableItemsTable extractableItemsTable,
								  @Autowired(required = false) InventoryService inventoryService) {
		this.extractableItemsTable = extractableItemsTable;
		this.inventoryService = inventoryService;
	}

	public boolean isExtractable(int itemId) {
		return extractableItemsTable.isExtractable(itemId);
	}

	public Optional<ExtractableItem> getExtractableItem(int itemId) {
		return extractableItemsTable.get(itemId);
	}

	/**
	 * Abre o item extraivel e concede as recompensas correspondentes com base na probabilidade configurada.
	 */
	public boolean extract(PlayerCharacter player, ItemInstance item, Consumer<GameServerPacket> clientSender) {
		if (player == null || item == null || player.isDead()) {
			return false;
		}

		ExtractableItem exItem = extractableItemsTable.get(item.itemId()).orElse(null);
		if (exItem == null) {
			return false;
		}

		Inventory inv = player.inventory();
		if (inv == null) {
			return false;
		}

		int roll = ThreadLocalRandom.current().nextInt(100);
		int cumulativeChance = 0;
		ExtractableProduct selectedProduct = null;

		for (ExtractableProduct product : exItem.products()) {
			cumulativeChance += product.chance();
			if (roll < cumulativeChance) {
				selectedProduct = product;
				break;
			}
		}

		// Valida capacidade de slots e peso antes de destruir a capsula (#222)
		int fishRate = isFishItem(item) ? Math.max(1, com.lopez.l2j.config.Config.RATE_EXTR_FISH) : 1;
		if (selectedProduct != null && !selectedProduct.items().isEmpty()) {
			int slotsNeeded = 0;
			long weightNeeded = 0;
			for (ExtractableProduct.ProductItem reward : selectedProduct.items()) {
				int finalCount = Math.max(1, reward.count() * fishRate);
				var tmpl = inventoryService != null && inventoryService.templates() != null
						? inventoryService.templates().get(reward.itemId()).orElse(null)
						: null;
				boolean stackable = tmpl == null || tmpl.stackable();
				if (!stackable) {
					slotsNeeded += finalCount;
				} else if (inv.byItemId(reward.itemId()).isEmpty()) {
					slotsNeeded += 1;
				}
				if (tmpl != null && reward.itemId() != com.lopez.l2j.game.item.ItemTemplate.ADENA_ID && !tmpl.isQuestItem()) {
					weightNeeded += (long) tmpl.weight() * finalCount;
				}
			}
			if (item.count() == 1) {
				slotsNeeded = Math.max(0, slotsNeeded - 1);
			}
			if (item.template() != null) {
				weightNeeded -= item.template().weight();
			}
			int maxSlots = player.maxInventorySlots() > 0 ? player.maxInventorySlots() : 80;
			if (inv.size() + slotsNeeded > maxSlots) {
				if (clientSender != null) {
					clientSender.accept(SystemMessage.id(SystemMessage.SLOTS_FULL));
				}
				return false;
			}
			if (weightNeeded > 0 && inv.currentLoad() + weightNeeded > 69_000 * Math.max(1.0, com.lopez.l2j.config.Config.ALT_WEIGHT_LIMIT)) {
				if (clientSender != null) {
					clientSender.accept(SystemMessage.id(SystemMessage.WEIGHT_LIMIT_EXCEEDED));
				}
				return false;
			}
		} else if (inv.size() > (player.maxInventorySlots() > 0 ? player.maxInventorySlots() : 80)) {
			if (clientSender != null) {
				clientSender.accept(SystemMessage.id(SystemMessage.SLOTS_FULL));
			}
			return false;
		}

		// Consome 1 item extraivel
		if (inventoryService != null) {
			inventoryService.consumeItem(inv, item.itemId(), 1, "Extract");
		} else {
			inv.destroyItemByItemId(item.itemId(), 1);
		}

		if (selectedProduct == null || selectedProduct.items().isEmpty()) {
			if (clientSender != null) {
				clientSender.accept(SystemMessage.id(SystemMessage.NOTHING_INSIDE_THAT));
			}
			return true;
		}

		// Entrega as recompensas (aplicando RATE_EXTR_FISH quando o item extraido for um peixe)
		for (ExtractableProduct.ProductItem reward : selectedProduct.items()) {
			int finalCount = Math.max(1, reward.count() * fishRate);
			if (inventoryService != null) {
				inventoryService.addItem(inv, reward.itemId(), finalCount, "Extract");
			} else {
				// Fallback sintetico para testes sem servico completo
				inv.addItem(reward.itemId(), finalCount);
			}

			if (clientSender != null) {
				if (finalCount > 1) {
					clientSender.accept(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
							new SystemMessage.ItemName(reward.itemId()),
							new SystemMessage.Number(finalCount)));
				} else {
					clientSender.accept(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1,
							new SystemMessage.ItemName(reward.itemId())));
				}
			}
		}

		return true;
	}

	public static boolean isFishItem(ItemInstance item) {
		if (item == null) {
			return false;
		}
		int id = item.itemId();
		if (id >= 6411 && id <= 6518) {
			return true;
		}
		return item.template() != null && item.template().name() != null
				&& item.template().name().toLowerCase(java.util.Locale.ROOT).contains("fish");
	}
}
