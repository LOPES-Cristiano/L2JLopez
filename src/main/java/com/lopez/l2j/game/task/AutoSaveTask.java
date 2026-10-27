package com.lopez.l2j.game.task;

import com.lopez.l2j.game.community.CommunityBoardService;
import com.lopez.l2j.game.cursed.CursedWeaponsManager;
import com.lopez.l2j.game.fishing.FishingChampionshipService;
import com.lopez.l2j.game.item.GroundItemService;
import com.lopez.l2j.game.manor.CastleManorManager;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.olympiad.OlympiadManager;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.sevensigns.SevenSignsManager;
import com.lopez.l2j.game.world.GameWorld;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Task Global de Auto-Save Periodico e Inicializacao Idempotente de Schemas SQL (#500 a #510).
 * Garante a criacao das tabelas relacionais e sincroniza periodicamente o estado em memoria
 * de RecipeBook, Community Mail, Manor, Cursed Weapons, Fishing Championship e fila de entrega
 * de itens (`character_items` - V33).
 */
@Service
public class AutoSaveTask {

    private static final Logger log = LoggerFactory.getLogger(AutoSaveTask.class);

    private final JdbcClient jdbc;
    private final RecipeService recipeService;
    private final CommunityBoardService communityBoardService;
    private final CastleManorManager castleManorManager;
    private final CursedWeaponsManager cursedWeaponsManager;
    private final FishingChampionshipService fishingChampionshipService;
    private final InventoryService inventoryService;
    @Autowired(required = false)
    private CharacterService characterService;
    @Autowired(required = false)
    private OlympiadManager olympiadManager;
    @Autowired(required = false)
    private SevenSignsManager sevenSignsManager;
    @Autowired(required = false)
    private GroundItemService groundItemService;
    private ScheduledExecutorService scheduler;

    public AutoSaveTask() {
        this(null, null, null, null, null, null, null);
    }

    public AutoSaveTask(
            JdbcClient jdbc,
            RecipeService recipeService,
            CommunityBoardService communityBoardService,
            CastleManorManager castleManorManager,
            CursedWeaponsManager cursedWeaponsManager,
            FishingChampionshipService fishingChampionshipService,
            InventoryService inventoryService) {
        this(jdbc, recipeService, communityBoardService, castleManorManager, cursedWeaponsManager,
                fishingChampionshipService, inventoryService, null, null, null, null);
    }

    @Autowired
    public AutoSaveTask(
            @Autowired(required = false) JdbcClient jdbc,
            @Autowired(required = false) RecipeService recipeService,
            @Autowired(required = false) CommunityBoardService communityBoardService,
            @Autowired(required = false) CastleManorManager castleManorManager,
            @Autowired(required = false) CursedWeaponsManager cursedWeaponsManager,
            @Autowired(required = false) FishingChampionshipService fishingChampionshipService,
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) CharacterService characterService,
            @Autowired(required = false) OlympiadManager olympiadManager,
            @Autowired(required = false) SevenSignsManager sevenSignsManager,
            @Autowired(required = false) GroundItemService groundItemService) {
        this.jdbc = jdbc;
        this.recipeService = recipeService;
        this.communityBoardService = communityBoardService;
        this.castleManorManager = castleManorManager;
        this.cursedWeaponsManager = cursedWeaponsManager;
        this.fishingChampionshipService = fishingChampionshipService;
        this.inventoryService = inventoryService;
        this.characterService = characterService;
        this.olympiadManager = olympiadManager;
        this.sevenSignsManager = sevenSignsManager;
        this.groundItemService = groundItemService;
    }

    @PostConstruct
    public void start() {
        initSchemas();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Global-AutoSaveTask");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runAutoSave, 5, 5, TimeUnit.MINUTES);
        log.info("AutoSaveTask: Agendador periodico de persistencia SQL iniciado (intervalo: 5 min).");
    }

    @PreDestroy
    public void shutdown() {
        runAutoSave();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public void initSchemas() {
        if (jdbc == null) {
            return;
        }
        String[] ddlStatements = {
            """
            CREATE TABLE IF NOT EXISTS character_hennas (
                charId INT UNSIGNED NOT NULL DEFAULT 0,
                symbol_id INT,
                slot INT NOT NULL DEFAULT 0,
                class_index INT(1) NOT NULL DEFAULT 0,
                PRIMARY KEY (charId, slot, class_index)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS character_recipebook (
                charId INT UNSIGNED NOT NULL DEFAULT 0,
                id DECIMAL(11) NOT NULL DEFAULT 0,
                classIndex TINYINT NOT NULL DEFAULT 0,
                type INT NOT NULL DEFAULT 0,
                PRIMARY KEY (id, charId, classIndex)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS character_friends (
                charId INT UNSIGNED NOT NULL DEFAULT 0,
                friendId INT UNSIGNED NOT NULL DEFAULT 0,
                friend_name VARCHAR(35) NOT NULL DEFAULT '',
                memo VARCHAR(255) DEFAULT NULL,
                PRIMARY KEY (charId, friendId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS character_blocklist (
                charId INT UNSIGNED NOT NULL DEFAULT 0,
                targetId INT UNSIGNED NOT NULL DEFAULT 0,
                target_name VARCHAR(35) NOT NULL DEFAULT '',
                PRIMARY KEY (charId, targetId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS bbs_mail (
                letter_id INT NOT NULL PRIMARY KEY,
                sender_id INT NOT NULL,
                sender_name VARCHAR(35) NOT NULL,
                receiver_id INT NOT NULL,
                receiver_name VARCHAR(35) NOT NULL,
                subject VARCHAR(128) NOT NULL,
                content TEXT NOT NULL,
                sent_date BIGINT NOT NULL,
                unread TINYINT(1) NOT NULL DEFAULT 1,
                deleted TINYINT(1) NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS seven_signs_festival (
                festivalId INT(1) NOT NULL DEFAULT 0,
                cabal VARCHAR(4) NOT NULL DEFAULT '',
                cycle INT(4) NOT NULL DEFAULT 0,
                date BIGINT(50) DEFAULT 0,
                score INT(5) NOT NULL DEFAULT 0,
                members VARCHAR(255) NOT NULL DEFAULT '',
                PRIMARY KEY (festivalId, cabal, cycle)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS castle_manor_production (
                castle_id INT NOT NULL DEFAULT 0,
                seed_id INT NOT NULL DEFAULT 0,
                can_produce INT NOT NULL DEFAULT 0,
                start_produce INT NOT NULL DEFAULT 0,
                seed_price INT NOT NULL DEFAULT 0,
                period INT NOT NULL DEFAULT 1,
                PRIMARY KEY (castle_id, seed_id, period)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS castle_manor_procure (
                castle_id INT NOT NULL DEFAULT 0,
                crop_id INT NOT NULL DEFAULT 0,
                can_buy INT NOT NULL DEFAULT 0,
                start_buy INT NOT NULL DEFAULT 0,
                price INT NOT NULL DEFAULT 0,
                reward_type INT NOT NULL DEFAULT 0,
                period INT NOT NULL DEFAULT 1,
                PRIMARY KEY (castle_id, crop_id, period)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS cursed_weapons (
                itemId INT NOT NULL,
                charId INT NOT NULL DEFAULT 0,
                playerKarma INT DEFAULT 0,
                playerPkKills INT DEFAULT 0,
                nbKills INT DEFAULT 0,
                endTime BIGINT DEFAULT 0,
                PRIMARY KEY (itemId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS clan_wars (
                clan1 INT NOT NULL DEFAULT 0,
                clan2 INT NOT NULL DEFAULT 0,
                expiry_time BIGINT NOT NULL DEFAULT 0,
                wantspeace1 DECIMAL(1,0) NOT NULL DEFAULT 0,
                wantspeace2 DECIMAL(1,0) NOT NULL DEFAULT 0,
                PRIMARY KEY (clan1, clan2)
            )
            """
        };
        for (String ddl : ddlStatements) {
            try {
                jdbc.sql(ddl).update();
            } catch (Exception e) {
                log.debug("AutoSaveTask DDL check: {}", e.getMessage());
            }
        }
    }

    public synchronized void runAutoSave() {
        try {
            GameWorld world = GameWorld.getInstance();
            if (characterService != null && world != null) {
                for (var online : world.onlinePlayers()) {
                    PlayerCharacter pc = online.character();
                    if (pc != null) {
                        characterService.save(pc, true);
                    }
                }
            }
            if (olympiadManager != null) {
                olympiadManager.save();
            }
            if (sevenSignsManager != null) {
                sevenSignsManager.persistStatus();
            }
            if (groundItemService != null) {
                groundItemService.saveToDb();
            }
            if (recipeService != null) {
                recipeService.saveAllToDb();
            }
            if (communityBoardService != null) {
                communityBoardService.saveAllMailsToDb();
            }
            if (castleManorManager != null) {
                castleManorManager.saveAllToDb();
            }
            if (cursedWeaponsManager != null) {
                cursedWeaponsManager.saveToDb();
            }
            if (fishingChampionshipService != null) {
                fishingChampionshipService.saveToDb();
            }
            processPendingCharacterItemsQueue();
        } catch (Exception e) {
            log.warn("AutoSaveTask error during periodic persistence: {}", e.getMessage());
        }
    }

    /**
     * Consome a fila de entrega externa `character_items` (V33 - #510) para jogadores online.
     */
    public void processPendingCharacterItemsQueue() {
        GameWorld world = GameWorld.getInstance();
        if (jdbc == null || world == null) {
            return;
        }
        for (var online : world.onlinePlayers()) {
            PlayerCharacter pc = online.character();
            if (pc == null || pc.inventory() == null) {
                continue;
            }
            try {
                var rows = jdbc.sql("SELECT item_id, count, enchant_level FROM character_items WHERE owner_id = :oid")
                        .param("oid", pc.objectId())
                        .query().listOfRows();
                if (!rows.isEmpty()) {
                    jdbc.sql("DELETE FROM character_items WHERE owner_id = :oid")
                            .param("oid", pc.objectId())
                            .update();
                    for (var r : rows) {
                        int itemId = ((Number) r.get("item_id")).intValue();
                        int count = ((Number) r.get("count")).intValue();
                        int enchant = r.get("enchant_level") instanceof Number n ? n.intValue() : 0;
                        if (inventoryService != null) {
                            var added = inventoryService.addItem(pc.inventory(), itemId, count, "CharacterItemsDelivery");
                            if (added != null && added.item() != null && enchant > 0) {
                                added.item().enchantLevel(enchant);
                            }
                        } else {
                            var item = pc.inventory().addItem(itemId, count);
                            if (item != null && enchant > 0) {
                                item.enchantLevel(enchant);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
