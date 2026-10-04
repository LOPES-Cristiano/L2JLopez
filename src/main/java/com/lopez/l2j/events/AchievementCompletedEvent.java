package com.lopez.l2j.events;

import java.util.Map;

/**
 * Publicado quando um jogador completa uma conquista. O mundo do jogo escuta e entrega
 * as recompensas (itemId -> quantidade); as conquistas nao conhecem inventario.
 */
public record AchievementCompletedEvent(int ownerId, int achievementId, String name, Map<Integer, Long> rewards) {
}
