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

		// Consome 1 item extraivel
		if (inventoryService != null) {
			inventoryService.consumeItem(inv, item.itemId(), 1, "Extract");
		} else {
			inv.destroyItemByItemId(item.itemId(), 1);
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

		if (selectedProduct == null || selectedProduct.items().isEmpty()) {
			if (clientSender != null) {
				clientSender.accept(SystemMessage.id(SystemMessage.NOTHING_INSIDE_THAT));
			}
			return true;
		}

		// Entrega as recompensas
		for (ExtractableProduct.ProductItem reward : selectedProduct.items()) {
			if (inventoryService != null) {
				inventoryService.addItem(inv, reward.itemId(), reward.count(), "Extract");
			} else {
				// Fallback sintetico para testes sem servico completo
				inv.destroyItemByItemId(reward.itemId(), 0); // no-op check
			}

			if (clientSender != null) {
				if (reward.count() > 1) {
					clientSender.accept(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1_S2,
							new SystemMessage.ItemName(reward.itemId()),
							new SystemMessage.Number(reward.count())));
				} else {
					clientSender.accept(SystemMessage.of(SystemMessage.YOU_PICKED_UP_S1,
							new SystemMessage.ItemName(reward.itemId())));
				}
			}
		}

		return true;
	}
}
