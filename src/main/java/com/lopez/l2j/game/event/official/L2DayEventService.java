package com.lopez.l2j.game.event.official;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

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

    public record WordRecipe(String word, Map<Integer, Integer> requiredLetters, int rewardItemId, int rewardCount, String rewardName) {}

    private boolean active = true;
    private final Map<String, WordRecipe> recipes = new HashMap<>();

    public L2DayEventService() {
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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Map<String, WordRecipe> getRecipes() { return Collections.unmodifiableMap(recipes); }
}
