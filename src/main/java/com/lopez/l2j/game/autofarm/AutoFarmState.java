package com.lopez.l2j.game.autofarm;

/**
 * Estado e preferencias de Auto-Farm de um jogador ativo.
 */
public class AutoFarmState {
    private final int playerId;
    private volatile boolean enabled;
    private volatile AutoFarmMode mode = AutoFarmMode.FIGHTER;
    private volatile int farmRadius = 1200;
    private volatile double autoPotionHpThreshold = 0.60; // 60%
    private volatile int selectedSkillId = 0;
    private volatile int currentTargetId = 0;

    public AutoFarmState(int playerId, boolean enabled) {
        this.playerId = playerId;
        this.enabled = enabled;
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AutoFarmMode getMode() {
        return mode;
    }

    public void setMode(AutoFarmMode mode) {
        this.mode = mode;
    }

    public int getFarmRadius() {
        return farmRadius;
    }

    public void setFarmRadius(int farmRadius) {
        this.farmRadius = farmRadius;
    }

    public double getAutoPotionHpThreshold() {
        return autoPotionHpThreshold;
    }

    public void setAutoPotionHpThreshold(double autoPotionHpThreshold) {
        this.autoPotionHpThreshold = autoPotionHpThreshold;
    }

    public int getSelectedSkillId() {
        return selectedSkillId;
    }

    public void setSelectedSkillId(int selectedSkillId) {
        this.selectedSkillId = selectedSkillId;
    }

    public int getCurrentTargetId() {
        return currentTargetId;
    }

    public void setCurrentTargetId(int currentTargetId) {
        this.currentTargetId = currentTargetId;
    }
}
