package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de Boas-Vindas e Starter Kit (StartupSystemService).
 * Escolha guiada de kit de classe/equipamento para novos personagens e bônus de primeiro login.
 */
@Service
public class StarterKitService {

    private static final Logger log = LoggerFactory.getLogger(StarterKitService.class);
    public static final String VAR_STARTER_KIT_GRANTED = "STARTER_KIT_GRANTED";

    public record KitItem(int itemId, int count, String name) {}

    private final Set<Integer> charactersClaimed = ConcurrentHashMap.newKeySet();

    // Kits por arquétipo
    private final List<KitItem> fighterKit = new ArrayList<>();
    private final List<KitItem> mageKit = new ArrayList<>();
    private final List<KitItem> commonBonus = new ArrayList<>();

    private final InventoryService inventoryService;
    private final CharacterVariablesService characterVariablesService;
    private final ItemRepository itemRepository;
    private final ItemTemplateTable itemTable;
    private final ObjectIdFactory idFactory;

    public StarterKitService() {
        this(null, null, null, null, null);
    }

    public StarterKitService(InventoryService inventoryService, CharacterVariablesService characterVariablesService) {
        this(inventoryService, characterVariablesService, null, inventoryService != null ? inventoryService.templates() : null, null);
    }

    @Autowired
    public StarterKitService(
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) CharacterVariablesService characterVariablesService,
            @Autowired(required = false) ItemRepository itemRepository,
            @Autowired(required = false) ItemTemplateTable itemTable,
            @Autowired(required = false) ObjectIdFactory idFactory) {
        this.inventoryService = inventoryService;
        this.characterVariablesService = characterVariablesService;
        this.itemRepository = itemRepository;
        this.itemTable = itemTable != null ? itemTable : (inventoryService != null ? inventoryService.templates() : null);
        this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x66000000);
        initDefaultKits();
    }

    private void initDefaultKits() {
        // Kit Comum de Boas-Vindas
        commonBonus.add(new KitItem(57, 50000, "Adena"));
        commonBonus.add(new KitItem(1060, 50, "Lesser Healing Potion"));
        commonBonus.add(new KitItem(736, 5, "Scroll of Escape"));
        commonBonus.add(new KitItem(734, 10, "Potion of Haste"));
        commonBonus.add(new KitItem(735, 10, "Potion of Wind Walk"));

        // Kit Fighter (No-Grade Wooden Set + Short Sword + Soulshots)
        fighterKit.add(new KitItem(1, 1, "Short Sword"));
        fighterKit.add(new KitItem(23, 1, "Wooden Breastplate"));
        fighterKit.add(new KitItem(2386, 1, "Wooden Gaiters"));
        fighterKit.add(new KitItem(43, 1, "Wooden Helmet"));
        fighterKit.add(new KitItem(1835, 3000, "Soulshot: No-Grade"));

        // Kit Mage (No-Grade Apprentice Robe + Apprentice's Rod + Spiritshots)
        mageKit.add(new KitItem(7, 1, "Apprentice's Rod"));
        mageKit.add(new KitItem(425, 1, "Apprentice's Tunic"));
        mageKit.add(new KitItem(461, 1, "Apprentice's Stockings"));
        mageKit.add(new KitItem(2509, 2000, "Spiritshot: No-Grade"));
    }

    /**
     * Resgata e concede o kit inicial de boas-vindas do jogador no inventário e no DB.
     */
    public synchronized List<KitItem> grantStarterKit(PlayerCharacter player) {
        return claimStarterKit(player);
    }

    /**
     * Resgata o kit inicial de boas-vindas do jogador. Retorna a lista de itens concedidos.
     */
    public synchronized List<KitItem> claimStarterKit(PlayerCharacter player) {
        if (player == null || hasClaimed(player.getObjectId())) {
            return Collections.emptyList();
        }

        List<KitItem> rewards = new ArrayList<>(commonBonus);
        if (player.isMage()) {
            rewards.addAll(mageKit);
        } else {
            rewards.addAll(fighterKit);
        }

        for (KitItem kitItem : rewards) {
            persistKitItem(player, kitItem);
        }

        charactersClaimed.add(player.getObjectId());
        if (characterVariablesService != null) {
            characterVariablesService.set(player.getObjectId(), VAR_STARTER_KIT_GRANTED, "1");
        }
        log.info("StarterKit: Jogador {} [{}] resgatou seu Kit de Boas-Vindas ({} itens).",
                player.getName(), player.getObjectId(), rewards.size());
        return rewards;
    }

    private void persistKitItem(PlayerCharacter player, KitItem kitItem) {
        if (player == null || kitItem == null || kitItem.count() <= 0) {
            return;
        }
        if (inventoryService != null && player.inventory() != null) {
            try {
                var res = inventoryService.addItem(player.inventory(), kitItem.itemId(), kitItem.count(), "StarterKit");
                if (res != null && res.item() != null) {
                    return;
                }
            } catch (Exception e) {
                log.debug("InventoryService.addItem fallback for StarterKit item {}: {}", kitItem.itemId(), e.getMessage());
            }
        }
        ItemTemplateTable templates = this.itemTable != null ? this.itemTable
                : (inventoryService != null ? inventoryService.templates() : null);
        if (templates != null) {
            var tmplOpt = templates.get(kitItem.itemId());
            if (tmplOpt.isPresent()) {
                var tmpl = tmplOpt.get();
                if (player.inventory() != null && tmpl.isStackable()) {
                    ItemInstance existing = player.inventory().byItemId(kitItem.itemId()).orElse(null);
                    if (existing != null) {
                        existing.count((int) Math.min(Integer.MAX_VALUE, (long) existing.count() + kitItem.count()));
                        if (itemRepository != null) {
                            itemRepository.update(existing);
                        }
                        return;
                    }
                }
                ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.getObjectId(), kitItem.count());
                if (player.inventory() != null) {
                    player.inventory().add(item);
                }
                if (itemRepository != null) {
                    itemRepository.insert(item, "StarterKit");
                }
                return;
            }
        }
        if (player.inventory() != null) {
            player.inventory().addItem(kitItem.itemId(), kitItem.count());
        }
    }

    public boolean hasClaimed(int charId) {
        if (charactersClaimed.contains(charId)) {
            return true;
        }
        if (characterVariablesService != null) {
            String granted = characterVariablesService.get(charId, VAR_STARTER_KIT_GRANTED);
            if ("1".equals(granted) || "true".equalsIgnoreCase(granted)) {
                charactersClaimed.add(charId);
                return true;
            }
        }
        return false;
    }

    public List<KitItem> getFighterKit() { return Collections.unmodifiableList(fighterKit); }
    public List<KitItem> getMageKit() { return Collections.unmodifiableList(mageKit); }
    public List<KitItem> getCommonBonus() { return Collections.unmodifiableList(commonBonus); }
}
