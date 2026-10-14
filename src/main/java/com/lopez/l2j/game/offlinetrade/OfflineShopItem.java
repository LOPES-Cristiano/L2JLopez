package com.lopez.l2j.game.offlinetrade;

/**
 * Item a venda ou compra em uma loja privada ou offline.
 */
public record OfflineShopItem(int objectId, int itemId, int count, int price, int enchant, int bodyPart) {

    public OfflineShopItem(int itemId, int count, int price) {
        this(0, itemId, count, price, 0, 0);
    }

    public OfflineShopItem(int objectId, int itemId, int count, int price, int enchant) {
        this(objectId, itemId, count, price, enchant, 0);
    }
}
