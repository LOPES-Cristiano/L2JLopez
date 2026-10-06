package com.lopez.l2j.game.autofarm;

/**
 * Acao recomendada pelo motor de IA do Auto-Farm para execucao.
 */
public record AutoFarmAction(
        ActionType type,
        int targetId,
        int skillId,
        String message
) {
    public enum ActionType {
        NONE,
        ATTACK,
        CAST_SKILL,
        USE_POTION,
        STOP
    }

    public static AutoFarmAction none() {
        return new AutoFarmAction(ActionType.NONE, 0, 0, "");
    }

    public static AutoFarmAction stop(String reason) {
        return new AutoFarmAction(ActionType.STOP, 0, 0, reason);
    }

    public static AutoFarmAction attack(int targetId) {
        return new AutoFarmAction(ActionType.ATTACK, targetId, 0, "Attacking target " + targetId);
    }

    public static AutoFarmAction cast(int targetId, int skillId) {
        return new AutoFarmAction(ActionType.CAST_SKILL, targetId, skillId, "Casting skill " + skillId + " on target " + targetId);
    }

    public static AutoFarmAction usePotion(int itemId) {
        return new AutoFarmAction(ActionType.USE_POTION, 0, itemId, "Using potion " + itemId);
    }
}
