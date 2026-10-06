package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public record KitItem(int itemId, int count, String name) {}

    private final Set<Integer> charactersClaimed = ConcurrentHashMap.newKeySet();

    // Kits por arquétipo
    private final List<KitItem> fighterKit = new ArrayList<>();
    private final List<KitItem> mageKit = new ArrayList<>();
    private final List<KitItem> commonBonus = new ArrayList<>();

    public StarterKitService() {
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
        fighterKit.add(new KitItem(2, 1, "Short Sword"));
        fighterKit.add(new KitItem(23, 1, "Wooden Breastplate"));
        fighterKit.add(new KitItem(2386, 1, "Wooden Gaiters"));
        fighterKit.add(new KitItem(43, 1, "Wooden Helmet"));
        fighterKit.add(new KitItem(1835, 3000, "Soulshot: No-Grade"));

        // Kit Mage (No-Grade Apprentice Robe + Apprentice Staff + Spiritshots)
        mageKit.add(new KitItem(3, 1, "Apprentice's Staff"));
        mageKit.add(new KitItem(425, 1, "Apprentice's Robe"));
        mageKit.add(new KitItem(463, 1, "Apprentice's Stockings"));
        mageKit.add(new KitItem(2509, 2000, "Spiritshot: No-Grade"));
    }

    /**
     * Resgata o kit inicial de boas-vindas do jogador. Retorna a lista de itens concedidos.
     */
    public synchronized List<KitItem> claimStarterKit(PlayerCharacter player) {
        if (player == null || charactersClaimed.contains(player.getObjectId())) {
            return Collections.emptyList();
        }

        List<KitItem> rewards = new ArrayList<>(commonBonus);
        if (player.isMage()) {
            rewards.addAll(mageKit);
        } else {
            rewards.addAll(fighterKit);
        }

        charactersClaimed.add(player.getObjectId());
        log.info("StarterKit: Jogador {} [{}] resgatou seu Kit de Boas-Vindas ({} itens).",
                player.getName(), player.getObjectId(), rewards.size());
        return rewards;
    }

    public boolean hasClaimed(int charId) {
        return charactersClaimed.contains(charId);
    }

    public List<KitItem> getFighterKit() { return Collections.unmodifiableList(fighterKit); }
    public List<KitItem> getMageKit() { return Collections.unmodifiableList(mageKit); }
    public List<KitItem> getCommonBonus() { return Collections.unmodifiableList(commonBonus); }
}
