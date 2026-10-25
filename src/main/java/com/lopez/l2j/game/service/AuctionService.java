package com.lopez.l2j.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Leilao de Clan Halls (AuctionService / AuctionManager) - #499.
 * Gerencia lances de clãs pelas Clan Halls (tabelas `auction`, `auction_bid`, `auction_watch` - V8).
 * Opera 100% com Adena organica (item 57).
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    public record ClanHallAuction(
            int id,
            int sellerId,
            String sellerName,
            String sellerClanName,
            int itemId,
            String itemName,
            int startingBid,
            int currentBid,
            long endDate
    ) {}

    public record AuctionBid(
            int id,
            int auctionId,
            int bidderId,
            String bidderName,
            String clanName,
            int maxBid,
            long timeBid
    ) {}

    private final JdbcClient jdbc;
    private final Map<Integer, ClanHallAuction> auctions = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, AuctionBid>> bidsByAuction = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> watchersByAuction = new ConcurrentHashMap<>();

    public AuctionService() {
        this(null);
    }

    @Autowired
    public AuctionService(@Autowired(required = false) JdbcClient jdbc) {
        this.jdbc = jdbc;
        initSchema();
        loadAuctions();
    }

    public void initSchema() {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.sql("""
                    CREATE TABLE IF NOT EXISTS auction (
                        id INT NOT NULL DEFAULT 0,
                        sellerId INT NOT NULL DEFAULT 0,
                        sellerName VARCHAR(50) NOT NULL DEFAULT 'NPC',
                        sellerClanName VARCHAR(50) NOT NULL DEFAULT '',
                        itemType VARCHAR(25) NOT NULL DEFAULT 'ClanHall',
                        itemId INT NOT NULL DEFAULT 0,
                        itemObjectId INT NOT NULL DEFAULT 0,
                        itemName VARCHAR(40) NOT NULL DEFAULT '',
                        itemQuantity INT NOT NULL DEFAULT 1,
                        startingBid INT NOT NULL DEFAULT 0,
                        currentBid INT NOT NULL DEFAULT 0,
                        endDate BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id)
                    )
                    """).update();
            jdbc.sql("""
                    CREATE TABLE IF NOT EXISTS auction_bid (
                        id INT NOT NULL DEFAULT 0,
                        auctionId INT NOT NULL DEFAULT 0,
                        bidderId INT NOT NULL DEFAULT 0,
                        bidderName VARCHAR(50) NOT NULL,
                        clan_name VARCHAR(50) NOT NULL,
                        maxBid INT NOT NULL DEFAULT 0,
                        time_bid BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (auctionId, bidderId)
                    )
                    """).update();
            jdbc.sql("""
                    CREATE TABLE IF NOT EXISTS auction_watch (
                        charObjId INT NOT NULL DEFAULT 0,
                        auctionId INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (charObjId, auctionId)
                    )
                    """).update();
        } catch (Exception e) {
            log.debug("AuctionService initSchema: {}", e.getMessage());
        }
    }

    public synchronized void loadAuctions() {
        if (jdbc == null) {
            return;
        }
        try {
            var rows = jdbc.sql("SELECT id, sellerId, sellerName, sellerClanName, itemId, itemName, startingBid, currentBid, endDate FROM auction")
                    .query().listOfRows();
            for (var r : rows) {
                int id = ((Number) r.get("id")).intValue();
                auctions.put(id, new ClanHallAuction(
                        id,
                        ((Number) r.get("sellerId")).intValue(),
                        (String) r.get("sellerName"),
                        (String) r.get("sellerClanName"),
                        ((Number) r.get("itemId")).intValue(),
                        (String) r.get("itemName"),
                        ((Number) r.get("startingBid")).intValue(),
                        ((Number) r.get("currentBid")).intValue(),
                        ((Number) r.get("endDate")).longValue()
                ));
            }
            var bidRows = jdbc.sql("SELECT id, auctionId, bidderId, bidderName, clan_name, maxBid, time_bid FROM auction_bid")
                    .query().listOfRows();
            for (var b : bidRows) {
                int auctionId = ((Number) b.get("auctionId")).intValue();
                int bidderId = ((Number) b.get("bidderId")).intValue();
                AuctionBid bid = new AuctionBid(
                        ((Number) b.get("id")).intValue(),
                        auctionId,
                        bidderId,
                        (String) b.get("bidderName"),
                        (String) b.get("clan_name"),
                        ((Number) b.get("maxBid")).intValue(),
                        ((Number) b.get("time_bid")).longValue()
                );
                bidsByAuction.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>()).put(bidderId, bid);
            }
            var watchRows = jdbc.sql("SELECT charObjId, auctionId FROM auction_watch").query().listOfRows();
            for (var w : watchRows) {
                int charId = ((Number) w.get("charObjId")).intValue();
                int auctionId = ((Number) w.get("auctionId")).intValue();
                watchersByAuction.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(charId);
            }
            log.info("AuctionService: {} leiloes de Clan Hall carregados.", auctions.size());
        } catch (Exception e) {
            log.warn("AuctionService loadAuctions: {}", e.getMessage());
        }
    }

    public synchronized boolean placeBid(int auctionId, int bidderClanId, String bidderLeaderName, String clanName, int bidAmount) {
        ClanHallAuction auction = auctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        int minRequired = Math.max(auction.startingBid(), auction.currentBid() + 1);
        if (bidAmount < minRequired) {
            return false;
        }
        long now = System.currentTimeMillis();
        AuctionBid bid = new AuctionBid(auctionId, auctionId, bidderClanId, bidderLeaderName, clanName, bidAmount, now);
        bidsByAuction.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>()).put(bidderClanId, bid);
        ClanHallAuction updated = new ClanHallAuction(
                auction.id(), auction.sellerId(), auction.sellerName(), auction.sellerClanName(),
                auction.itemId(), auction.itemName(), auction.startingBid(), bidAmount, auction.endDate());
        auctions.put(auctionId, updated);

        if (jdbc != null) {
            try {
                jdbc.sql("DELETE FROM auction_bid WHERE auctionId = :aid AND bidderId = :bid")
                        .param("aid", auctionId)
                        .param("bid", bidderClanId)
                        .update();
                jdbc.sql("""
                        INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, clan_name, maxBid, time_bid)
                        VALUES (:id, :aid, :bid, :bname, :cname, :max, :t)
                        """)
                        .param("id", auctionId)
                        .param("aid", auctionId)
                        .param("bid", bidderClanId)
                        .param("bname", bidderLeaderName)
                        .param("cname", clanName)
                        .param("max", bidAmount)
                        .param("t", now)
                        .update();
                jdbc.sql("UPDATE auction SET currentBid = :cur WHERE id = :id")
                        .param("cur", bidAmount)
                        .param("id", auctionId)
                        .update();
            } catch (Exception e) {
                log.warn("AuctionService placeBid error: {}", e.getMessage());
            }
        }
        return true;
    }

    public synchronized boolean cancelBid(int auctionId, int bidderClanId) {
        Map<Integer, AuctionBid> bids = bidsByAuction.get(auctionId);
        if (bids == null || bids.remove(bidderClanId) == null) {
            return false;
        }
        if (jdbc != null) {
            try {
                jdbc.sql("DELETE FROM auction_bid WHERE auctionId = :aid AND bidderId = :bid")
                        .param("aid", auctionId)
                        .param("bid", bidderClanId)
                        .update();
            } catch (Exception e) {
                log.warn("AuctionService cancelBid error: {}", e.getMessage());
            }
        }
        return true;
    }

    public synchronized void addAuctionWatch(int charObjId, int auctionId) {
        watchersByAuction.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(charObjId);
        if (jdbc != null) {
            try {
                jdbc.sql("INSERT INTO auction_watch (charObjId, auctionId) VALUES (:cid, :aid)")
                        .param("cid", charObjId)
                        .param("aid", auctionId)
                        .update();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void removeAuctionWatch(int charObjId, int auctionId) {
        Set<Integer> set = watchersByAuction.get(auctionId);
        if (set != null) {
            set.remove(charObjId);
        }
        if (jdbc != null) {
            try {
                jdbc.sql("DELETE FROM auction_watch WHERE charObjId = :cid AND auctionId = :aid")
                        .param("cid", charObjId)
                        .param("aid", auctionId)
                        .update();
            } catch (Exception ignored) {}
        }
    }

    public Optional<ClanHallAuction> getAuction(int auctionId) {
        return Optional.ofNullable(auctions.get(auctionId));
    }

    public Collection<ClanHallAuction> getAllAuctions() {
        return Collections.unmodifiableCollection(auctions.values());
    }

    public Collection<AuctionBid> getBids(int auctionId) {
        Map<Integer, AuctionBid> map = bidsByAuction.get(auctionId);
        return map != null ? Collections.unmodifiableCollection(map.values()) : List.of();
    }
}
