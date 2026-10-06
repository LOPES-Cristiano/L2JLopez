package com.lopez.l2j.game.instance.dimensionalrift;

/**
 * Representa os 6 tiers/niveis do Dimensional Rift:
 * 1. Recruit (20-30)
 * 2. Soldier (30-40)
 * 3. Officer (40-50)
 * 4. Captain (50-60)
 * 5. Commander (60-70)
 * 6. Hero (70-80)
 */
public enum RiftTier {
    RECRUIT(1, "Recruit", 20, 30, 18, 25333),
    SOLDIER(2, "Soldier", 30, 40, 21, 25334),
    OFFICER(3, "Officer", 40, 50, 24, 25335),
    CAPTAIN(4, "Captain", 50, 60, 27, 25336),
    COMMANDER(5, "Commander", 60, 70, 30, 25337),
    HERO(6, "Hero", 70, 80, 33, 25338);

    private final int id;
    private final String name;
    private final int minLevel;
    private final int maxLevel;
    private final int fragmentCost;
    private final int bossNpcId;

    RiftTier(int id, String name, int minLevel, int maxLevel, int fragmentCost, int bossNpcId) {
        this.id = id;
        this.name = name;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.fragmentCost = fragmentCost;
        this.bossNpcId = bossNpcId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getFragmentCost() {
        return fragmentCost;
    }

    public int getBossNpcId() {
        return bossNpcId;
    }

    public static RiftTier fromId(int id) {
        for (RiftTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }
        return RECRUIT;
    }
}
