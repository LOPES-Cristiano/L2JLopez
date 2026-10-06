package com.lopez.l2j.game.npc.minion;

/**
 * Representa uma regra de minion vinculada a um monstro ou chefe principal.
 */
public record MinionEntry(int bossId, int minionId, int amountMin, int amountMax) {
}
