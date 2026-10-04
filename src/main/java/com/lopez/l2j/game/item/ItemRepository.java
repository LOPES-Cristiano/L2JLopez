package com.lopez.l2j.game.item;

import java.util.List;

/** Persistencia da tabela legada {@code items}. */
public interface ItemRepository {

	record StoredItem(int objectId, int itemId, int count, int enchant, String location, int locationData,
			int customType1, int customType2, int mana) {
	}

	/** Itens no inventario ou equipados (loc INVENTORY/PAPERDOLL), como Inventory.restore. */
	List<StoredItem> findInventory(int ownerId);

	/** So os equipados, para a tela de selecao de personagem. */
	List<Paperdoll.Entry> findPaperdoll(int ownerId);

	void insert(ItemInstance item, String process);

	/** Grava dono, quantidade, encantamento e localizacao. */
	void update(ItemInstance item);

	void delete(int objectId);

	void deleteByOwner(int ownerId);
}
