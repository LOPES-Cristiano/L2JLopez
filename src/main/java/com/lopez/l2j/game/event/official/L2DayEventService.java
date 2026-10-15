package com.lopez.l2j.game.event.official;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Evento L2 Day (Coleção de Letras L2day).
 * Drop de letras (L, I, N, E, A, G, E, I, I) de monstros e troca com o NPC de evento
 * por buffs especiais, pergaminhos de encantamento e acessórios.
 */
@Service
public class L2DayEventService {

    private static final Logger log = LoggerFactory.getLogger(L2DayEventService.class);

    // Mapeamento das letras e seus IDs de item oficiais
    public static final int LETTER_A = 3875;
    public static final int LETTER_C = 3876;
    public static final int LETTER_E = 3877;
    public static final int LETTER_F = 3878;
    public static final int LETTER_G = 3879;
    public static final int LETTER_H = 3880;
    public static final int LETTER_I = 3881;
    public static final int LETTER_L = 3882;
    public static final int LETTER_N = 3883;
    public static final int LETTER_O = 3884;
    public static final int LETTER_R = 3885;
    public static final int LETTER_S = 3886;
    public static final int LETTER_T = 3887;
    public static final int LETTER_II = 3888; // Numeral romano II

    public static final int[] ALL_LETTERS = {
        LETTER_A, LETTER_C, LETTER_E, LETTER_F, LETTER_G, LETTER_H, LETTER_I,
        LETTER_L, LETTER_N, LETTER_O, LETTER_R, LETTER_S, LETTER_T, LETTER_II
    };

    private static final double DEFAULT_LETTER_DROP_CHANCE = 0.08;

    public record WordRecipe(String word, Map<Integer, Integer> requiredLetters, int rewardItemId, int rewardCount, String rewardName) {}

    public record WordReward(boolean success, String word, int rewardItemId, int rewardCount, String rewardName, String message) {
        public boolean ok() { return success; }
        public int itemId() { return rewardItemId; }
        public int count() { return rewardCount; }
    }

    private boolean active = true;
    private double letterDropChance = Config.L2_DAY_DROP_CHANCE > 0 ? (Config.L2_DAY_DROP_CHANCE / 100.0) : DEFAULT_LETTER_DROP_CHANCE;
    private final Map<String, WordRecipe> recipes = new HashMap<>();

    private final InventoryService inventoryService;
    private final ItemRepository itemRepository;
    private final ItemTemplateTable itemTable;
    private final ObjectIdFactory idFactory;

    public L2DayEventService() {
        this(null, null, null, null);
    }

    public L2DayEventService(InventoryService inventoryService) {
        this(inventoryService, null, inventoryService != null ? inventoryService.templates() : null, null);
    }

    @Autowired
    public L2DayEventService(
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) ItemRepository itemRepository,
            @Autowired(required = false) ItemTemplateTable itemTable,
            @Autowired(required = false) ObjectIdFactory idFactory) {
        this.inventoryService = inventoryService;
        this.itemRepository = itemRepository;
        this.itemTable = itemTable != null ? itemTable : (inventoryService != null ? inventoryService.templates() : null);
        this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x5B000000);
        if (Config.L2_DAY_DROP_CHANCE > 0) {
            this.letterDropChance = Config.L2_DAY_DROP_CHANCE / 100.0;
        }
        initRecipes();
    }

    private void initRecipes() {
        // Palavra 1: LINEAGEII -> Scroll of Enchant Armor (A-Grade) [948]
        Map<Integer, Integer> lineageLetters = new HashMap<>();
        lineageLetters.put(LETTER_L, 1);
        lineageLetters.put(LETTER_I, 1);
        lineageLetters.put(LETTER_N, 1);
        lineageLetters.put(LETTER_E, 2);
        lineageLetters.put(LETTER_A, 1);
        lineageLetters.put(LETTER_G, 1);
        lineageLetters.put(LETTER_II, 1);
        recipes.put("LINEAGEII", new WordRecipe("LINEAGEII", lineageLetters, 948, 1, "Scroll: Enchant Armor (A-Grade)"));

        // Palavra 2: NCSOFT -> Blessed Scroll of Escape [1538]
        Map<Integer, Integer> ncsoftLetters = new HashMap<>();
        ncsoftLetters.put(LETTER_N, 1);
        ncsoftLetters.put(LETTER_C, 1);
        ncsoftLetters.put(LETTER_S, 1);
        ncsoftLetters.put(LETTER_O, 1);
        ncsoftLetters.put(LETTER_F, 1);
        ncsoftLetters.put(LETTER_T, 1);
        recipes.put("NCSOFT", new WordRecipe("NCSOFT", ncsoftLetters, 1538, 2, "Blessed Scroll of Escape"));

        // Palavra 3: CHRONICLE -> recompensa configurada em L2DayEventRewards/Scrolls/Accessoris
        Map<Integer, Integer> chronicleLetters = new HashMap<>();
        chronicleLetters.put(LETTER_C, 2);
        chronicleLetters.put(LETTER_H, 1);
        chronicleLetters.put(LETTER_R, 1);
        chronicleLetters.put(LETTER_O, 1);
        chronicleLetters.put(LETTER_N, 1);
        chronicleLetters.put(LETTER_I, 1);
        chronicleLetters.put(LETTER_L, 1);
        chronicleLetters.put(LETTER_E, 1);
        List<Integer> configuredRewards = getConfiguredRewardIds();
        int chronRewardId = !configuredRewards.isEmpty() ? configuredRewards.get(0) : 3926;
        recipes.put("CHRONICLE", new WordRecipe("CHRONICLE", chronicleLetters, chronRewardId, 1, "L2Day Chronicle Special Reward"));
    }

    public boolean isConfigDropEnabled() {
        return Config.L2_DAY_ADD_DROP;
    }

    public boolean isSpawnManagersEnabled() {
        return Config.L2_DAY_SPAWN_MANAGER;
    }

    public int getScrollChance() {
        return Math.max(0, Config.L2_DAY_SCROLL_CHS);
    }

    public int getEnchantScrollChance() {
        return Math.max(0, Config.L2_DAY_ENCH_SCROLL_CHS);
    }

    public int getAccessoryChance() {
        return Math.max(0, Config.L2_DAY_ACC_SCROLL_CHS);
    }

    public List<Integer> getConfiguredRewardIds() {
        return parseIdList(Config.L2_DAY_REWARDS);
    }

    public List<Integer> getConfiguredAccessoryIds() {
        return parseIdList(Config.L2_DAY_REWARDS_ACCESSORIE);
    }

    public List<Integer> getConfiguredScrollIds() {
        return parseIdList(Config.L2_DAY_REWARDS_SCROLL);
    }

    private List<Integer> parseIdList(String raw) {
        List<Integer> ids = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String token : raw.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        int id = Integer.parseInt(trimmed);
                        if (id > 0) ids.add(id);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return ids;
    }

    public int rollConfiguredPoolReward() {
        int roll = ThreadLocalRandom.current().nextInt(1000);
        if (roll < getAccessoryChance() && !getConfiguredAccessoryIds().isEmpty()) {
            var list = getConfiguredAccessoryIds();
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        if (roll < getEnchantScrollChance() && !getConfiguredRewardIds().isEmpty()) {
            var list = getConfiguredRewardIds();
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        if (roll < getScrollChance() && !getConfiguredScrollIds().isEmpty()) {
            var list = getConfiguredScrollIds();
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        return 3926; // L2Day Scroll of Guidance fallback
    }

    /**
     * Valida se a palavra é suportada pelo evento L2 Day.
     */
    public Optional<WordRecipe> getRecipe(String word) {
        if (word == null) return Optional.empty();
        return Optional.ofNullable(recipes.get(word.toUpperCase().trim()));
    }

    /**
     * Checa se o conjunto de itens fornecido possui todas as letras da palavra solicitada.
     */
    public boolean hasLetters(String word, Map<Integer, Integer> playerInventoryLetters) {
        if (word == null || playerInventoryLetters == null) return false;
        WordRecipe recipe = recipes.get(word.toUpperCase().trim());
        if (recipe == null) return false;

        for (Map.Entry<Integer, Integer> entry : recipe.requiredLetters().entrySet()) {
            int currentCount = playerInventoryLetters.getOrDefault(entry.getKey(), 0);
            if (currentCount < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasLetters(PlayerCharacter player, String word) {
        if (player == null || player.inventory() == null || word == null) return false;
        WordRecipe recipe = recipes.get(word.toUpperCase().trim());
        if (recipe == null) return false;
        for (Map.Entry<Integer, Integer> entry : recipe.requiredLetters().entrySet()) {
            int owned = player.inventory().byItemId(entry.getKey()).map(ItemInstance::count).orElse(0);
            if (owned < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida e consome as letras do inventário real do jogador, entregando a recompensa no inventário e no DB.
     */
    public synchronized WordReward exchangeWord(PlayerCharacter player, String word) {
        if (!active) {
            return new WordReward(false, word, 0, 0, "", "L2Day event is not currently active.");
        }
        if (player == null || player.isDead() || player.inventory() == null) {
            return new WordReward(false, word, 0, 0, "", "Cannot exchange word right now.");
        }
        if (word == null || word.isBlank()) {
            return new WordReward(false, "", 0, 0, "", "Invalid word.");
        }
        String normalized = word.toUpperCase().trim();
        WordRecipe recipe = recipes.get(normalized);
        if (recipe == null) {
            return new WordReward(false, normalized, 0, 0, "", "Unsupported word: " + normalized);
        }

        // 1. Verificar se o jogador possui todas as letras necessárias
        for (Map.Entry<Integer, Integer> req : recipe.requiredLetters().entrySet()) {
            int owned = player.inventory().byItemId(req.getKey()).map(ItemInstance::count).orElse(0);
            if (owned < req.getValue()) {
                return new WordReward(false, normalized, 0, 0, "", "You do not have all required letters for " + normalized + ".");
            }
        }

        // 2. Consumir letras do inventário e persistir no DB
        for (Map.Entry<Integer, Integer> req : recipe.requiredLetters().entrySet()) {
            consumeFromInventoryAndDb(player, req.getKey(), req.getValue(), "L2DayConsume");
        }

        // 3. Entregar recompensa no inventário e persistir no DB
        grantToInventoryAndDb(player, recipe.rewardItemId(), recipe.rewardCount(), "L2DayReward");

        log.info("Player {} (ID: {}) exchanged L2Day word '{}' for {} x{}",
                player.name(), player.objectId(), normalized, recipe.rewardName(), recipe.rewardCount());

        return new WordReward(true, normalized, recipe.rewardItemId(), recipe.rewardCount(), recipe.rewardName(),
                "Congratulations! You received " + recipe.rewardName() + " x" + recipe.rewardCount() + ".");
    }

    private void consumeFromInventoryAndDb(PlayerCharacter player, int itemId, int count, String process) {
        if (count <= 0 || player == null || player.inventory() == null) return;
        if (inventoryService != null) {
            inventoryService.consumeItem(player.inventory(), itemId, count, process);
            return;
        }
        ItemInstance existing = player.inventory().byItemId(itemId).orElse(null);
        if (existing != null) {
            if (existing.count() > count) {
                existing.count(existing.count() - count);
                if (itemRepository != null) {
                    itemRepository.update(existing);
                }
            } else {
                player.inventory().remove(existing);
                existing.count(0);
                if (itemRepository != null) {
                    itemRepository.delete(existing.objectId());
                }
            }
        }
    }

    private void grantToInventoryAndDb(PlayerCharacter player, int itemId, int count, String process) {
        if (count <= 0 || player == null) return;
        if (inventoryService != null && player.inventory() != null) {
            try {
                var res = inventoryService.addItem(player.inventory(), itemId, count, process);
                if (res != null && res.item() != null) {
                    return;
                }
            } catch (Exception e) {
                log.debug("InventoryService.addItem fallback for L2Day item {}: {}", itemId, e.getMessage());
            }
        }
        ItemTemplateTable templates = this.itemTable != null ? this.itemTable
                : (inventoryService != null ? inventoryService.templates() : null);
        if (templates != null) {
            var tmplOpt = templates.get(itemId);
            if (tmplOpt.isPresent()) {
                var tmpl = tmplOpt.get();
                if (player.inventory() != null && tmpl.isStackable()) {
                    ItemInstance existing = player.inventory().byItemId(itemId).orElse(null);
                    if (existing != null) {
                        existing.count((int) Math.min(Integer.MAX_VALUE, (long) existing.count() + count));
                        if (itemRepository != null) {
                            itemRepository.update(existing);
                        }
                        return;
                    }
                }
                ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
                if (player.inventory() != null) {
                    player.inventory().add(item);
                }
                if (itemRepository != null) {
                    itemRepository.insert(item, process);
                }
                return;
            }
        }
        if (player.inventory() != null) {
            player.inventory().addItem(itemId, count);
        }
    }

    /**
     * Sorteia uma letra do evento L2Day ao derrotar um monstro elegível.
     * Retorna o itemId da letra sorteada ou 0 caso não ocorra drop.
     */
    public int rollLetterDrop(int monsterLevel) {
        if (!active || monsterLevel < 5) {
            return 0;
        }
        if (ThreadLocalRandom.current().nextDouble() < letterDropChance) {
            return ALL_LETTERS[ThreadLocalRandom.current().nextInt(ALL_LETTERS.length)];
        }
        return 0;
    }

    public int rollLetterDrop() {
        return rollLetterDrop(20);
    }

    public String generateHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><title>L2Day Letter Collector</title><body><center>");
        sb.append("<font color=\"LEVEL\">=== L2Day Letter Event ===</font><br>");
        sb.append("Collect letters from monsters and exchange complete words for rewards!<br><br>");
        sb.append("<table width=280 bgcolor=000000>");
        for (WordRecipe recipe : recipes.values()) {
            boolean ready = player != null && hasLetters(player, recipe.word());
            sb.append("<tr><td><font color=\"LEVEL\">").append(recipe.word()).append("</font><br1>");
            sb.append("<font color=\"8FA888\">").append(recipe.rewardName()).append(" x").append(recipe.rewardCount()).append("</font></td>");
            sb.append("<td align=right>");
            if (ready) {
                sb.append("<button value=\"Exchange\" action=\"bypass -h npc_%objectId%_l2day_exchange ").append(recipe.word())
                        .append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
            } else {
                sb.append("<button value=\"Collect\" action=\"bypass -h npc_%objectId%_l2day_exchange ").append(recipe.word())
                        .append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
            }
            sb.append("</td></tr>");
        }
        sb.append("</table></center></body></html>");
        return sb.toString();
    }

    public boolean isActive() { return active; }
    public boolean isEventActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getLetterDropChance() { return letterDropChance; }
    public void setLetterDropChance(double letterDropChance) { this.letterDropChance = Math.max(0.0, Math.min(1.0, letterDropChance)); }
    public Map<String, WordRecipe> getRecipes() { return Collections.unmodifiableMap(recipes); }
}
