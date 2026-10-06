package com.lopez.l2j.game.offlinetrade;

import java.util.List;

/**
 * Representa um vendedor ou comprador em modo Offline Trade (Item 94).
 */
public record OfflineTrader(
        int charId,
        int shopId,
        int mode,
        boolean packaged,
        String title,
        long endTime,
        List<OfflineShopItem> items
) {
    public boolean isExpired() {
        return endTime > 0 && System.currentTimeMillis() > endTime;
    }
}
