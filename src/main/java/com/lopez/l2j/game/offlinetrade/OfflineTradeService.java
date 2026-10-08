package com.lopez.l2j.game.offlinetrade;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico responsavel pelo gerenciamento de Lojas Offline (Offline Trade) - Item 94.
 * Permite que personagens continuem vendendo ou comprando itens apos desconexao,
 * persistindo as configuracoes nas tabelas character_offline e character_offline_shop.
 */
@Service
public class OfflineTradeService {

    private static final Logger log = LoggerFactory.getLogger(OfflineTradeService.class);

    // Tipos de Lojas Privadas
    public static final int STORE_PRIVATE_SELL = 1;
    public static final int STORE_PRIVATE_BUY = 3;
    public static final int STORE_PRIVATE_MANUFACTURE = 5;
    public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

    // Duracao padrao de loja offline: 7 dias em milissegundos
    public static final long DEFAULT_OFFLINE_DURATION = 7L * 24 * 60 * 60 * 1000;

    @Autowired(required = false)
    private JdbcClient jdbc;

    private final AtomicInteger shopIdGen = new AtomicInteger(1000);
    private final Map<Integer, OfflineTrader> activeTraders = new ConcurrentHashMap<>();

    public OfflineTradeService() {
    }

    public OfflineTradeService(JdbcClient jdbc) {
        this.jdbc = jdbc;
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
        long endTime = durationMillis != 0 ? System.currentTimeMillis() + durationMillis : System.currentTimeMillis() + DEFAULT_OFFLINE_DURATION;

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
     * Carrega comerciantes offline ativos do banco de dados na inicializacao.
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

                activeTraders.put(charId, new OfflineTrader(charId, shopId, mode, packaged, title, endTime, items));
                loaded++;
            }
        } catch (Exception e) {
            log.error("Erro ao carregar comerciantes offline do banco", e);
        }

        log.info("Offline Trade Service: {} comerciantes offline restaurados do banco", loaded);
        return loaded;
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
