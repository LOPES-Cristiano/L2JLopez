package com.lopez.l2j.game.offlinetrade;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico responsavel pelo gerenciamento de Lojas Privadas em memoria (SellStoreList / BuyStoreList)
 * e Lojas Offline (Offline Trade) - Item 94 / #497.
 * Permite que personagens vendam ou comprem itens reais (online e apos desconexao),
 * persistindo as configuracoes offline nas tabelas character_offline e character_offline_shop
 * e restaurando as lojas diretamente no GameWorld na inicializacao.
 */
@Service
public class OfflineTradeService {

    private static final Logger log = LoggerFactory.getLogger(OfflineTradeService.class);
    private static volatile OfflineTradeService instance = new OfflineTradeService();

    // Tipos de Lojas Privadas
    public static final int STORE_PRIVATE_SELL = 1;
    public static final int STORE_PRIVATE_BUY = 3;
    public static final int STORE_PRIVATE_MANUFACTURE = 5;
    public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

    // Duracao padrao de loja offline: 7 dias em milissegundos
    public static final long DEFAULT_OFFLINE_DURATION = 7L * 24 * 60 * 60 * 1000;

    /**
     * Lista em memoria de itens reais a venda em loja privada de um jogador.
     */
    public record SellStoreList(int playerObjectId, boolean packageSale, String title, List<OfflineShopItem> items) {
        public SellStoreList {
            title = title == null ? "" : title;
            items = items == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(items);
        }
    }

    /**
     * Lista em memoria de pedidos de compra em loja privada de um jogador.
     */
    public record BuyStoreList(int playerObjectId, String title, List<OfflineShopItem> items) {
        public BuyStoreList {
            title = title == null ? "" : title;
            items = items == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(items);
        }
    }

    @Autowired(required = false)
    private JdbcClient jdbc;

    private final AtomicInteger shopIdGen = new AtomicInteger(1000);
    private final Map<Integer, OfflineTrader> activeTraders = new ConcurrentHashMap<>();

    private static final Map<Integer, SellStoreList> sellStoreLists = new ConcurrentHashMap<>();
    private static final Map<Integer, BuyStoreList> buyStoreLists = new ConcurrentHashMap<>();

    public OfflineTradeService() {
        instance = this;
    }

    public OfflineTradeService(JdbcClient jdbc) {
        this.jdbc = jdbc;
        instance = this;
    }

    public static OfflineTradeService getInstance() {
        return instance;
    }

    // ---- Gerenciamento em Memoria de Lojas Privadas (Venda e Compra) ----

    public void setSellStoreList(int playerObjectId, boolean packageSale, String title, List<OfflineShopItem> items) {
        sellStoreLists.put(playerObjectId, new SellStoreList(playerObjectId, packageSale, title, items));
    }

    public SellStoreList getSellStoreList(int playerObjectId) {
        return sellStoreLists.get(playerObjectId);
    }

    public void removeSellStoreList(int playerObjectId) {
        sellStoreLists.remove(playerObjectId);
    }

    public void setBuyStoreList(int playerObjectId, String title, List<OfflineShopItem> items) {
        buyStoreLists.put(playerObjectId, new BuyStoreList(playerObjectId, title, items));
    }

    public BuyStoreList getBuyStoreList(int playerObjectId) {
        return buyStoreLists.get(playerObjectId);
    }

    public void removeBuyStoreList(int playerObjectId) {
        buyStoreLists.remove(playerObjectId);
    }

    public long resolveOfflineDurationMillis(int mode, long requestedDurationMillis) {
        if (requestedDurationMillis != 0L) {
            return requestedDurationMillis;
        }
        int priceHours = (mode == STORE_PRIVATE_MANUFACTURE)
                ? Config.OFFLINE_CRAFT_PRICE_ID_TIME
                : Config.OFFLINE_TRADE_PRICE_ID_TIME;
        if (priceHours > 0) {
            return priceHours * 3_600_000L;
        }
        if (Config.ALLOW_OFFLINE_HOUR > 0) {
            return Config.ALLOW_OFFLINE_HOUR * 3_600_000L;
        }
        return DEFAULT_OFFLINE_DURATION;
    }

    public boolean chargeOfflineFeeIfApplicable(PlayerCharacter player, int mode) {
        if (player == null || player.inventory() == null || player.inventory().items().isEmpty()) {
            return true;
        }
        int feeItemId = (mode == STORE_PRIVATE_MANUFACTURE)
                ? Config.OFFLINE_CRAFT_PRICE_ID
                : Config.OFFLINE_TRADE_PRICE_ID;
        long feeCount = (mode == STORE_PRIVATE_MANUFACTURE)
                ? Config.OFFLINE_CRAFT_PRICE_COUNT
                : Config.OFFLINE_TRADE_PRICE_COUNT;
        if (feeItemId <= 0 || feeCount <= 0) {
            return true;
        }
        var feeItem = player.inventory().byItemId(feeItemId).orElse(null);
        if (feeItem == null || feeItem.count() < feeCount) {
            return false;
        }
        return player.inventory().destroyItemByItemId(feeItemId, (int) Math.min(Integer.MAX_VALUE, feeCount));
    }

    /**
     * Inicia o modo de loja offline para o personagem informado.
     */
    public synchronized boolean startOfflineTrade(PlayerCharacter player, int mode, boolean packaged,
                                                 String title, List<OfflineShopItem> items, long durationMillis) {
        if (player == null || player.isDead() || player.karma() > 0) {
            log.warn("Offline Trade recusado para {}", player != null ? player.name() : "null");
            return false;
        }

        if (player.isBuffShop() && !Config.ALLOW_OFFLINE_BUFF && !Config.ALLOW_OFFLINE_TRADE) {
            log.warn("Offline Buff Shop desativado nas configuracoes para {}", player.name());
            return false;
        }

        if (!Config.ALLOW_OFFLINE_TRADE) {
            log.warn("Offline Trade desativado nas configuracoes");
            return false;
        }

        if (mode == STORE_PRIVATE_MANUFACTURE && !Config.ALLOW_OFFLINE_TRADE_CRAFT) {
            log.warn("Offline Craft desativado nas configuracoes para {}", player.name());
            return false;
        }

        if (items == null || items.isEmpty()) {
            log.warn("Offline Trade recusado para {}: lista de itens vazia", player.name());
            return false;
        }

        if (!chargeOfflineFeeIfApplicable(player, mode)) {
            log.warn("Offline Trade recusado para {}: taxa de item insuficiente", player.name());
            return false;
        }

        if (Config.ALLOW_OFFLINE_TRADE_COLOR_NAME && Config.OFFLINE_TRADE_COLOR_NAME != null) {
            try {
                int color = Integer.decode("0x" + Config.OFFLINE_TRADE_COLOR_NAME.trim());
                player.nameColor(color);
            } catch (Exception ignored) {}
        }

        if (Config.ALLOW_OFFLINE_TRADE_PROTECTION) {
            player.invul(true);
        }

        int charId = player.objectId();
        int shopId = shopIdGen.incrementAndGet();
        long effectiveDuration = resolveOfflineDurationMillis(mode, durationMillis);
        long endTime = System.currentTimeMillis() + effectiveDuration;

        OfflineTrader trader = new OfflineTrader(charId, shopId, mode, packaged, title, endTime, items);
        activeTraders.put(charId, trader);

        // Persistencia no Banco de Dados
        if (jdbc != null) {
            try {
                // Remove qualquer entrada previa deste personagem
                jdbc.sql("DELETE FROM character_offline WHERE charId = :charId")
                        .param("charId", charId)
                        .update();

                jdbc.sql("""
                    INSERT INTO character_offline (charId, shopid, mode, packaged, title, endTime)
                    VALUES (:charId, :shopId, :mode, :packaged, :title, :endTime)
                """)
                        .param("charId", charId)
                        .param("shopId", shopId)
                        .param("mode", mode)
                        .param("packaged", packaged ? 1 : 0)
                        .param("title", title != null ? title : "")
                        .param("endTime", endTime)
                        .update();

                // Insere itens do shop
                for (OfflineShopItem item : items) {
                    jdbc.sql("""
                        INSERT INTO character_offline_shop (shopid, itemid, count, price)
                        VALUES (:shopId, :itemId, :count, :price)
                    """)
                            .param("shopId", shopId)
                            .param("itemId", item.itemId())
                            .param("count", item.count())
                            .param("price", item.price())
                            .update();
                }
            } catch (Exception e) {
                log.error("Erro ao persistir loja offline para charId {}", charId, e);
            }
        }

        log.info("Personagem {} (ID {}) entrou em Offline Trade [ShopId: {}, Itens: {}, Fim: {}]",
                player.name(), charId, shopId, items.size(), endTime);
        return true;
    }

    /**
     * Encerra a loja offline e limpa os registros do banco de dados.
     */
    public synchronized boolean stopOfflineTrade(int charId) {
        OfflineTrader trader = activeTraders.remove(charId);
        sellStoreLists.remove(charId);
        buyStoreLists.remove(charId);
        if (jdbc != null) {
            try {
                if (trader != null) {
                    jdbc.sql("DELETE FROM character_offline_shop WHERE shopid = :shopId")
                            .param("shopId", trader.shopId())
                            .update();
                }
                jdbc.sql("DELETE FROM character_offline WHERE charId = :charId")
                        .param("charId", charId)
                        .update();
            } catch (Exception e) {
                log.error("Erro ao remover loja offline do banco para charId {}", charId, e);
            }
        }

        log.info("Loja offline encerrada para charId {}", charId);
        return true;
    }

    /**
     * Carrega comerciantes offline ativos do banco de dados na inicializacao
     * e reconstrói suas listas em memória e presença no GameWorld.
     */
    public synchronized int loadOfflineTraders() {
        if (!Config.RESTORE_OFFLINE_TRADERS) {
            log.info("Restauracao de comerciantes offline desativada por configuracao.");
            return 0;
        }
        if (jdbc == null) {
            return 0;
        }

        int loaded = 0;
        try {
            var rows = jdbc.sql("SELECT charId, shopid, mode, packaged, title, endTime FROM character_offline")
                    .query().listOfRows();

            long now = System.currentTimeMillis();
            for (var row : rows) {
                int charId = ((Number) row.get("charId")).intValue();
                int shopId = ((Number) row.get("shopid")).intValue();
                int mode = ((Number) row.get("mode")).intValue();
                boolean packaged = ((Number) row.get("packaged")).intValue() == 1;
                String title = (String) row.get("title");
                long endTime = ((Number) row.get("endTime")).longValue();

                if (endTime < now) {
                    // Expirado, remove do banco
                    stopOfflineTrade(charId);
                    continue;
                }

                // Carrega itens da loja
                var itemRows = jdbc.sql("SELECT itemid, count, price FROM character_offline_shop WHERE shopid = :shopId")
                        .param("shopId", shopId)
                        .query().listOfRows();

                List<OfflineShopItem> items = new ArrayList<>();
                for (var iRow : itemRows) {
                    items.add(new OfflineShopItem(
                            ((Number) iRow.get("itemid")).intValue(),
                            ((Number) iRow.get("count")).intValue(),
                            ((Number) iRow.get("price")).intValue()
                    ));
                }

                OfflineTrader trader = new OfflineTrader(charId, shopId, mode, packaged, title, endTime, items);
                activeTraders.put(charId, trader);
                if (mode == STORE_PRIVATE_BUY) {
                    setBuyStoreList(charId, title, items);
                } else {
                    setSellStoreList(charId, packaged, title, items);
                }
                restoreOfflineTraderToWorld(trader);
                loaded++;
            }
        } catch (Exception e) {
            log.error("Erro ao carregar comerciantes offline do banco", e);
        }

        log.info("Offline Trade Service: {} comerciantes offline restaurados do banco", loaded);
        return loaded;
    }

    private void restoreOfflineTraderToWorld(OfflineTrader trader) {
        GameWorld world = GameWorld.getInstance();
        if (world == null || jdbc == null || trader == null) {
            return;
        }
        try {
            var charRows = jdbc.sql("SELECT account_name, char_name, level, race, classid, sex, x, y, z FROM characters WHERE charId = :id")
                    .param("id", trader.charId())
                    .query().listOfRows();
            if (charRows.isEmpty()) {
                return;
            }
            var c = charRows.get(0);
            String acc = (String) c.get("account_name");
            String name = (String) c.get("char_name");
            int lvl = c.get("level") instanceof Number n ? n.intValue() : 1;
            int race = c.get("race") instanceof Number n ? n.intValue() : 0;
            int classId = c.get("classid") instanceof Number n ? n.intValue() : 0;
            boolean sex = c.get("sex") instanceof Number n && n.intValue() == 1;
            int x = c.get("x") instanceof Number n ? n.intValue() : 83400;
            int y = c.get("y") instanceof Number n ? n.intValue() : 147943;
            int z = c.get("z") instanceof Number n ? n.intValue() : -3404;

            PlayerCharacter pc = new PlayerCharacter(trader.charId(), acc != null ? acc : "offline",
                    name != null ? name : ("Trader" + trader.charId()), lvl, 0L, 0, classId, classId, race, sex,
                    0, 0, 0, 1000, 1000, 1000, 0, 0, 0, 0, "", 0, 0L, 0L, x, y, z, 0, 1000.0, 1000.0, 1000.0);
            pc.sitting(true);
            pc.privateStoreType(trader.mode());
            pc.storeTitle(trader.title());
            if (Config.ALLOW_OFFLINE_TRADE_PROTECTION) {
                pc.invul(true);
            }
            world.registerPlayer(new GameWorld.SimpleOnlinePlayer(pc, pkt -> {}));
        } catch (Exception e) {
            log.debug("restoreOfflineTraderToWorld {}: {}", trader.charId(), e.getMessage());
        }
    }

    /**
     * Remove comerciantes offline cujo tempo limite expirou.
     */
    public synchronized int checkExpiredTraders() {
        int expiredCount = 0;
        for (OfflineTrader trader : activeTraders.values()) {
            if (trader.isExpired()) {
                stopOfflineTrade(trader.charId());
                expiredCount++;
            }
        }
        return expiredCount;
    }

    public boolean isOfflineTrader(int charId) {
        return activeTraders.containsKey(charId);
    }

    public OfflineTrader getTrader(int charId) {
        return activeTraders.get(charId);
    }

    public int getActiveTraderCount() {
        return activeTraders.size();
    }
}
