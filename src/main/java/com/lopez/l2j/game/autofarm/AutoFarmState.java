package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Estado e configuracoes completas de Auto-Farm de um jogador (Baseado no AutoFarmContext L2JPremium + extensoes).
 * Suporta:
 * - 5 arquÃ©tipos (0=Fighter, 1=Archer, 2=Mage, 3=Healer, 4=Summon) + compatibilidade com AutoFarmMode
 * - 4 categorias de skills do personagem (Attack, Chance/Debuff, Self Buff, Low Life/Heal) com atÃ© 8 slots cada
 * - Chances (1-100%), limites minimos de MP/HP (1-100%) e modo Random por categoria
 * - 3 categorias de skills de Summon/Pet (Attack, Self, Heal) com atÃ© 8 slots cada, chances, limites e modo Random
 * - OpÃ§Ãµes tÃ¡ticas: Assist Party Leader, Assist Mobs, Keep Start Farm Loc, Use Skills Extra Delay,
 *   Run Target Close Up, Use Summon Skills, Restore Target MP, Search Distance (Raio), Shortcut Page (1-10)
 * - Auto Combat Potions (ACP) integrado (HP%, MP%, CP%)
 * - Controle de tempo de assinatura, horas grÃ¡tis diÃ¡rias, trial e IP/HWID
 */
public class AutoFarmState {
    private final int playerId;
    private volatile boolean enabled;

    // 0 = Fighter, 1 = Archer, 2 = Mage, 3 = Healer, 4 = Summon
    private volatile int farmType = 0;
    private volatile AutoFarmMode mode = AutoFarmMode.FIGHTER;

    // UI current tabs
    private volatile String activeSkillTab = "attack"; // attack, chance, self, heal
    private volatile String activeSummonSkillTab = "attack"; // attack, self, heal

    // Distance & Tactical options
    private volatile int farmRadius = 1200;
    private volatile int shortcutPage = 1; // 1..10
    private volatile int lastAttackSkillIndex = -1;
    private volatile int lastChanceSkillIndex = -1;
    private volatile int lastSelfSkillIndex = -1;
    private volatile int lastLowLifeSkillIndex = -1;
    private volatile boolean leaderAssist = false;
    private volatile boolean assistMonsterAttack = false;
    private volatile boolean keepLocation = false;
    private volatile int keepLocationX = 0;
    private volatile int keepLocationY = 0;
    private volatile int keepLocationZ = 0;
    private volatile boolean extraDelaySkill = false;
    private volatile boolean extraSummonDelaySkill = false;
    private volatile boolean runTargetCloseUp = false;
    private volatile boolean useSummonSkills = false;
    private volatile boolean targetRestoreMp = false;

    // Player skill lists (max 8 slots each)
    private final List<Integer> attackSpells = new CopyOnWriteArrayList<>();
    private final List<Integer> chanceSpells = new CopyOnWriteArrayList<>();
    private final List<Integer> selfSpells = new CopyOnWriteArrayList<>();
    private final List<Integer> lowLifeSpells = new CopyOnWriteArrayList<>();

    // Player skill chances (1..100)
    private volatile int attackChance = Config.AUTOFARM_ATTACK_SKILLS_DEFAULT_CHANCE;
    private volatile int chanceChance = Config.AUTOFARM_CHANCE_SKILLS_DEFAULT_CHANCE;
    private volatile int selfChance = Config.AUTOFARM_SELF_SKILLS_DEFAULT_CHANCE;
    private volatile int lifeChance = Config.AUTOFARM_LOW_LIFE_SKILLS_DEFAULT_CHANCE;

    // Player skill min MP/HP percent thresholds (1..100)
    private volatile int attackPercent = Config.AUTOFARM_ATTACK_SKILLS_MIN_MP_PERCENT;
    private volatile int chancePercent = Config.AUTOFARM_CHANCE_SKILLS_MIN_MP_PERCENT;
    private volatile int selfPercent = Config.AUTOFARM_SELF_SKILLS_MIN_MP_PERCENT;
    private volatile int lifePercent = Config.AUTOFARM_LOW_LIFE_SKILLS_MIN_HP_PERCENT;

    // Player random skill selection flags
    private volatile boolean rndAttackSkills = false;
    private volatile boolean rndChanceSkills = false;
    private volatile boolean rndSelfSkills = false;
    private volatile boolean rndLifeSkills = false;

    // Summon skill lists (max 8 slots each)
    private final List<Integer> summonAttackSpells = new CopyOnWriteArrayList<>();
    private final List<Integer> summonSelfSpells = new CopyOnWriteArrayList<>();
    private final List<Integer> summonHealSpells = new CopyOnWriteArrayList<>();

    // Summon skill chances (1..100)
    private volatile int summonAttackChance = Config.AUTOFARM_SUMMON_ATTACK_DEFAULT_CHANCE;
    private volatile int summonSelfChance = Config.AUTOFARM_SUMMON_SELF_DEFAULT_CHANCE;
    private volatile int summonLifeChance = Config.AUTOFARM_SUMMON_HEAL_DEFAULT_CHANCE;

    // Summon skill min MP/HP percent thresholds (1..100)
    private volatile int summonAttackPercent = Config.AUTOFARM_SUMMON_ATTACK_MIN_MP_PERCENT;
    private volatile int summonSelfPercent = Config.AUTOFARM_SUMMON_SELF_MIN_MP_PERCENT;
    private volatile int summonLifePercent = Config.AUTOFARM_SUMMON_HEAL_MIN_HP_PERCENT;

    // Summon random skill selection flags
    private volatile boolean rndSummonAttackSkills = false;
    private volatile boolean rndSummonSelfSkills = false;
    private volatile boolean rndSummonLifeSkills = false;

    // ACP (Auto Combat Potions)
    private volatile double autoPotionHpThreshold = 0.60; // 60%
    private volatile int acpHpPercent = 0; // 0 = off, or 10..95
    private volatile int acpMpPercent = 0; // 0 = off, or 10..95
    private volatile int acpCpPercent = 0; // 0 = off, or 10..95
    private volatile long lastPotionUseMs = 0L;

    // Legacy & Combat runtime fields
    private volatile int selectedSkillId = 0;
    private volatile int healSkillId = 1015; // Battle Heal default
    private volatile int kiteDistance = 120;
    private volatile int currentTargetId = 0;
    private volatile long extraDelayTimerMs = 0L;
    private volatile long extraSummonDelayTimerMs = 0L;
    private volatile long movePauseUntilMs = 0L;

    // Subscription / Time / HWID / IP fields
    private volatile long autoFarmEnd = 0L; // Timestamp ms when paid/trial farm expires
    private volatile long farmOnlineTime = 0L; // Accumulated online farm time (ms)
    private volatile long lastFarmOnlineTime = 0L; // Timestamp when online farm started
    private volatile boolean activeFarmOnlineTime = false;
    private volatile boolean freeTrialUsed = false;
    private volatile String clientIp = "127.0.0.1";
    private volatile boolean loadedFromDb = false;

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

    public int getFarmType() {
        return farmType;
    }

    public void setFarmType(int farmType) {
        int clamped = Math.max(0, Math.min(4, farmType));
        this.farmType = clamped;
        this.mode = switch (clamped) {
            case 0 -> AutoFarmMode.FIGHTER;
            case 1 -> AutoFarmMode.ARCHER;
            case 2 -> AutoFarmMode.MAGE;
            case 3 -> AutoFarmMode.HEALER;
            case 4 -> AutoFarmMode.SUMMON;
            default -> AutoFarmMode.FIGHTER;
        };
    }

    public AutoFarmMode getMode() {
        return mode;
    }

    public void setMode(AutoFarmMode mode) {
        if (mode == null) {
            mode = AutoFarmMode.FIGHTER;
        }
        this.mode = mode;
        this.farmType = switch (mode) {
            case FIGHTER, BALANCED -> 0;
            case ARCHER -> 1;
            case MAGE -> 2;
            case HEALER -> 3;
            case SUMMON -> 4;
        };
    }

    public String getActiveSkillTab() {
        return activeSkillTab;
    }

    public void setActiveSkillTab(String activeSkillTab) {
        if (activeSkillTab != null && !activeSkillTab.isBlank()) {
            this.activeSkillTab = activeSkillTab.toLowerCase();
        }
    }

    public String getActiveSummonSkillTab() {
        return activeSummonSkillTab;
    }

    public void setActiveSummonSkillTab(String activeSummonSkillTab) {
        if (activeSummonSkillTab != null && !activeSummonSkillTab.isBlank()) {
            this.activeSummonSkillTab = activeSummonSkillTab.toLowerCase();
        }
    }

    public int getFarmRadius() {
        return farmRadius;
    }

    public void setFarmRadius(int farmRadius) {
        this.farmRadius = Math.max(100, Math.min(3000, farmRadius));
    }

    public int getShortcutPage() {
        return shortcutPage;
    }

    public void setShortcutPage(int shortcutPage) {
        this.shortcutPage = Math.max(1, Math.min(10, shortcutPage));
    }

    public boolean isLeaderAssist() {
        return leaderAssist;
    }

    public void setLeaderAssist(boolean leaderAssist) {
        this.leaderAssist = leaderAssist;
        if (leaderAssist) {
            this.assistMonsterAttack = false;
            this.keepLocation = false;
        }
    }

    public boolean isAssistMonsterAttack() {
        return assistMonsterAttack;
    }

    public void setAssistMonsterAttack(boolean assistMonsterAttack) {
        this.assistMonsterAttack = assistMonsterAttack;
        if (assistMonsterAttack) {
            this.leaderAssist = false;
        }
    }

    public boolean isKeepLocation() {
        return keepLocation;
    }

    public void setKeepLocation(boolean keepLocation, int x, int y, int z) {
        this.keepLocation = keepLocation;
        if (keepLocation) {
            this.keepLocationX = x;
            this.keepLocationY = y;
            this.keepLocationZ = z;
            this.leaderAssist = false;
        }
    }

    public void setKeepLocation(boolean keepLocation) {
        this.keepLocation = keepLocation;
        if (keepLocation) {
            this.leaderAssist = false;
        }
    }

    public int getKeepLocationX() {
        return keepLocationX;
    }

    public int getKeepLocationY() {
        return keepLocationY;
    }

    public int getKeepLocationZ() {
        return keepLocationZ;
    }

    public boolean isExtraDelaySkill() {
        return extraDelaySkill;
    }

    public void setExtraDelaySkill(boolean extraDelaySkill) {
        this.extraDelaySkill = extraDelaySkill;
    }

    public boolean isExtraSummonDelaySkill() {
        return extraSummonDelaySkill;
    }

    public void setExtraSummonDelaySkill(boolean extraSummonDelaySkill) {
        this.extraSummonDelaySkill = extraSummonDelaySkill;
    }

    public boolean isRunTargetCloseUp() {
        return runTargetCloseUp;
    }

    public void setRunTargetCloseUp(boolean runTargetCloseUp) {
        this.runTargetCloseUp = runTargetCloseUp;
    }

    public boolean isUseSummonSkills() {
        return useSummonSkills;
    }

    public void setUseSummonSkills(boolean useSummonSkills) {
        this.useSummonSkills = useSummonSkills;
    }

    public boolean isTargetRestoreMp() {
        return targetRestoreMp;
    }

    public void setTargetRestoreMp(boolean targetRestoreMp) {
        this.targetRestoreMp = targetRestoreMp;
    }

    public List<Integer> getAttackSpells() {
        return attackSpells;
    }

    public List<Integer> getChanceSpells() {
        return chanceSpells;
    }

    public List<Integer> getSelfSpells() {
        return selfSpells;
    }

    public List<Integer> getLowLifeSpells() {
        return lowLifeSpells;
    }

    public List<Integer> getSummonAttackSpells() {
        return summonAttackSpells;
    }

    public List<Integer> getSummonSelfSpells() {
        return summonSelfSpells;
    }

    public List<Integer> getSummonHealSpells() {
        return summonHealSpells;
    }

    public int getAttackChance() {
        return attackChance;
    }

    public void setAttackChance(int attackChance) {
        this.attackChance = clampPercent(attackChance);
    }

    public int getChanceChance() {
        return chanceChance;
    }

    public void setChanceChance(int chanceChance) {
        this.chanceChance = clampPercent(chanceChance);
    }

    public int getSelfChance() {
        return selfChance;
    }

    public void setSelfChance(int selfChance) {
        this.selfChance = clampPercent(selfChance);
    }

    public int getLifeChance() {
        return lifeChance;
    }

    public void setLifeChance(int lifeChance) {
        this.lifeChance = clampPercent(lifeChance);
    }

    public int getAttackPercent() {
        return attackPercent;
    }

    public void setAttackPercent(int attackPercent) {
        this.attackPercent = clampPercent(attackPercent);
    }

    public int getChancePercent() {
        return chancePercent;
    }

    public void setChancePercent(int chancePercent) {
        this.chancePercent = clampPercent(chancePercent);
    }

    public int getSelfPercent() {
        return selfPercent;
    }

    public void setSelfPercent(int selfPercent) {
        this.selfPercent = clampPercent(selfPercent);
    }

    public int getLifePercent() {
        return lifePercent;
    }

    public void setLifePercent(int lifePercent) {
        this.lifePercent = clampPercent(lifePercent);
    }

    public boolean isRndAttackSkills() {
        return rndAttackSkills;
    }

    public void setRndAttackSkills(boolean rndAttackSkills) {
        this.rndAttackSkills = rndAttackSkills;
    }

    public boolean isRndChanceSkills() {
        return rndChanceSkills;
    }

    public void setRndChanceSkills(boolean rndChanceSkills) {
        this.rndChanceSkills = rndChanceSkills;
    }

    public boolean isRndSelfSkills() {
        return rndSelfSkills;
    }

    public void setRndSelfSkills(boolean rndSelfSkills) {
        this.rndSelfSkills = rndSelfSkills;
    }

    public boolean isRndLifeSkills() {
        return rndLifeSkills;
    }

    public void setRndLifeSkills(boolean rndLifeSkills) {
        this.rndLifeSkills = rndLifeSkills;
    }

    public int getSummonAttackChance() {
        return summonAttackChance;
    }

    public void setSummonAttackChance(int summonAttackChance) {
        this.summonAttackChance = clampPercent(summonAttackChance);
    }

    public int getSummonSelfChance() {
        return summonSelfChance;
    }

    public void setSummonSelfChance(int summonSelfChance) {
        this.summonSelfChance = clampPercent(summonSelfChance);
    }

    public int getSummonLifeChance() {
        return summonLifeChance;
    }

    public void setSummonLifeChance(int summonLifeChance) {
        this.summonLifeChance = clampPercent(summonLifeChance);
    }

    public int getSummonAttackPercent() {
        return summonAttackPercent;
    }

    public void setSummonAttackPercent(int summonAttackPercent) {
        this.summonAttackPercent = clampPercent(summonAttackPercent);
    }

    public int getSummonSelfPercent() {
        return summonSelfPercent;
    }

    public void setSummonSelfPercent(int summonSelfPercent) {
        this.summonSelfPercent = clampPercent(summonSelfPercent);
    }

    public int getSummonLifePercent() {
        return summonLifePercent;
    }

    public void setSummonLifePercent(int summonLifePercent) {
        this.summonLifePercent = clampPercent(summonLifePercent);
    }

    public boolean isRndSummonAttackSkills() {
        return rndSummonAttackSkills;
    }

    public void setRndSummonAttackSkills(boolean rndSummonAttackSkills) {
        this.rndSummonAttackSkills = rndSummonAttackSkills;
    }

    public boolean isRndSummonSelfSkills() {
        return rndSummonSelfSkills;
    }

    public void setRndSummonSelfSkills(boolean rndSummonSelfSkills) {
        this.rndSummonSelfSkills = rndSummonSelfSkills;
    }

    public boolean isRndSummonLifeSkills() {
        return rndSummonLifeSkills;
    }

    public void setRndSummonLifeSkills(boolean rndSummonLifeSkills) {
        this.rndSummonLifeSkills = rndSummonLifeSkills;
    }

    public double getAutoPotionHpThreshold() {
        return autoPotionHpThreshold;
    }

    public void setAutoPotionHpThreshold(double autoPotionHpThreshold) {
        this.autoPotionHpThreshold = autoPotionHpThreshold;
        if (autoPotionHpThreshold > 1.0) {
            this.acpHpPercent = (int) Math.round(autoPotionHpThreshold);
        } else {
            this.acpHpPercent = (int) Math.round(autoPotionHpThreshold * 100.0);
        }
    }

    public int getAcpHpPercent() {
        return acpHpPercent;
    }

    public void setAcpHpPercent(int acpHpPercent) {
        this.acpHpPercent = Math.max(0, Math.min(100, acpHpPercent));
        if (this.acpHpPercent > 0) {
            this.autoPotionHpThreshold = this.acpHpPercent / 100.0;
        }
    }

    public int getAcpMpPercent() {
        return acpMpPercent;
    }

    public void setAcpMpPercent(int acpMpPercent) {
        this.acpMpPercent = Math.max(0, Math.min(100, acpMpPercent));
    }

    public int getAcpCpPercent() {
        return acpCpPercent;
    }

    public void setAcpCpPercent(int acpCpPercent) {
        this.acpCpPercent = Math.max(0, Math.min(100, acpCpPercent));
    }

    public long getLastPotionUseMs() {
        return lastPotionUseMs;
    }

    public void setLastPotionUseMs(long lastPotionUseMs) {
        this.lastPotionUseMs = lastPotionUseMs;
    }

    public int getSelectedSkillId() {
        if (selectedSkillId > 0) {
            return selectedSkillId;
        }
        if (!attackSpells.isEmpty()) {
            return attackSpells.get(0);
        }
        return 0;
    }

    public void setSelectedSkillId(int selectedSkillId) {
        this.selectedSkillId = selectedSkillId;
        if (selectedSkillId > 0 && !attackSpells.contains(selectedSkillId) && attackSpells.size() < 8) {
            attackSpells.add(selectedSkillId);
        }
    }

    public int getHealSkillId() {
        if (!lowLifeSpells.isEmpty()) {
            return lowLifeSpells.get(0);
        }
        return healSkillId;
    }

    public void setHealSkillId(int healSkillId) {
        this.healSkillId = healSkillId;
        if (healSkillId > 0 && !lowLifeSpells.contains(healSkillId) && lowLifeSpells.size() < 8) {
            lowLifeSpells.add(healSkillId);
        }
    }

    public int getKiteDistance() {
        return kiteDistance;
    }

    public void setKiteDistance(int kiteDistance) {
        this.kiteDistance = kiteDistance;
    }

    public int getCurrentTargetId() {
        return currentTargetId;
    }

    public void setCurrentTargetId(int currentTargetId) {
        this.currentTargetId = currentTargetId;
    }

    public int getLastAttackSkillIndex() {
        return lastAttackSkillIndex;
    }

    public void setLastAttackSkillIndex(int lastAttackSkillIndex) {
        this.lastAttackSkillIndex = lastAttackSkillIndex;
    }

    public int getLastChanceSkillIndex() {
        return lastChanceSkillIndex;
    }

    public void setLastChanceSkillIndex(int lastChanceSkillIndex) {
        this.lastChanceSkillIndex = lastChanceSkillIndex;
    }

    public int getLastSelfSkillIndex() {
        return lastSelfSkillIndex;
    }

    public void setLastSelfSkillIndex(int lastSelfSkillIndex) {
        this.lastSelfSkillIndex = lastSelfSkillIndex;
    }

    public int getLastLowLifeSkillIndex() {
        return lastLowLifeSkillIndex;
    }

    public void setLastLowLifeSkillIndex(int lastLowLifeSkillIndex) {
        this.lastLowLifeSkillIndex = lastLowLifeSkillIndex;
    }

    public long getExtraDelayTimerMs() {
        return extraDelayTimerMs;
    }

    public void setExtraDelayTimerMs(long extraDelayTimerMs) {
        this.extraDelayTimerMs = extraDelayTimerMs;
    }

    public long getExtraSummonDelayTimerMs() {
        return extraSummonDelayTimerMs;
    }

    public void setExtraSummonDelayTimerMs(long extraSummonDelayTimerMs) {
        this.extraSummonDelayTimerMs = extraSummonDelayTimerMs;
    }

    public long getMovePauseUntilMs() {
        return movePauseUntilMs;
    }

    public void setMovePauseUntilMs(long movePauseUntilMs) {
        this.movePauseUntilMs = movePauseUntilMs;
    }

    public long getAutoFarmEnd() {
        return autoFarmEnd;
    }

    public void setAutoFarmEnd(long autoFarmEnd) {
        this.autoFarmEnd = autoFarmEnd;
    }

    public boolean isActiveAutofarmAllowed() {
        if (Config.AUTOFARM_IS_FREE) {
            return true;
        }
        return autoFarmEnd > System.currentTimeMillis();
    }

    public long getFarmOnlineTime() {
        if (activeFarmOnlineTime && lastFarmOnlineTime > 0) {
            return farmOnlineTime + (System.currentTimeMillis() - lastFarmOnlineTime);
        }
        return farmOnlineTime;
    }

    public void setFarmOnlineTime(long farmOnlineTime) {
        this.farmOnlineTime = farmOnlineTime;
    }

    public boolean isActiveFarmOnlineTime() {
        return activeFarmOnlineTime;
    }

    public void setActiveFarmOnlineTime() {
        this.lastFarmOnlineTime = System.currentTimeMillis();
        this.activeFarmOnlineTime = true;
    }

    public void stopFarmOnlineTime() {
        if (activeFarmOnlineTime && lastFarmOnlineTime > 0) {
            this.farmOnlineTime += (System.currentTimeMillis() - lastFarmOnlineTime);
        }
        this.lastFarmOnlineTime = 0L;
        this.activeFarmOnlineTime = false;
    }

    public void clearFarmOnlineTime() {
        this.lastFarmOnlineTime = 0L;
        this.farmOnlineTime = 0L;
        this.activeFarmOnlineTime = false;
    }

    public boolean isFreeTrialUsed() {
        return freeTrialUsed;
    }

    public void setFreeTrialUsed(boolean freeTrialUsed) {
        this.freeTrialUsed = freeTrialUsed;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        if (clientIp != null && !clientIp.isBlank()) {
            this.clientIp = clientIp;
        }
    }

    public boolean isLoadedFromDb() {
        return loadedFromDb;
    }

    public void setLoadedFromDb(boolean loadedFromDb) {
        this.loadedFromDb = loadedFromDb;
    }

    public List<Integer> getSkillsForType(String type) {
        if (type == null) return attackSpells;
        return switch (type.toLowerCase()) {
            case "chance", "farmchanceskills" -> chanceSpells;
            case "self", "farmselfskills" -> selfSpells;
            case "heal", "farmhealskills" -> lowLifeSpells;
            case "farmattacksummonskills" -> summonAttackSpells;
            case "farmselfsummonskills" -> summonSelfSpells;
            case "farmhealsummonskills" -> summonHealSpells;
            default -> attackSpells;
        };
    }

    public List<Integer> getSummonSkillsForType(String type) {
        if (type == null) return summonAttackSpells;
        return switch (type.toLowerCase()) {
            case "self", "farmselfsummonskills" -> summonSelfSpells;
            case "heal", "farmhealsummonskills" -> summonHealSpells;
            default -> summonAttackSpells;
        };
    }

    private static int clampPercent(int val) {
        return Math.max(1, Math.min(100, val));
    }
}
