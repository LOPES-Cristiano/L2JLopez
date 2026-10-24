package com.lopez.l2j.game.skill;

import java.util.Locale;

/**
 * Interlude client (Protocol 746) AbnormalEffect visual bitmask constants and XML parser.
 * Matches L2JDreamV2 L2Character / AbnormalEffect retail bitmasks.
 */
public final class AbnormalEffect {

    public static final int NULL = 0x00000000;
    public static final int BLEEDING = 0x00000001;
    public static final int POISON = 0x00000002;
    public static final int REDCIRCLE = 0x00000004;
    public static final int ICE = 0x00000008;
    public static final int WIND = 0x00000010;
    public static final int FEAR = 0x00000020;
    public static final int STUN = 0x00000040;
    public static final int SLEEP = 0x00000080;
    public static final int MUTED = 0x00000100;
    public static final int ROOT = 0x00000200;
    public static final int HOLD_1 = 0x00000400;
    public static final int HOLD_2 = 0x00000800;
    public static final int UNKNOWN_13 = 0x00001000;
    public static final int BIG_HEAD = 0x00002000;
    public static final int FLAME = 0x00004000;
    public static final int UNKNOWN_16 = 0x00008000;
    public static final int GROW = 0x00010000;
    public static final int FLOATING_ROOT = 0x00020000;
    public static final int DANCE_STUNNED = 0x00040000;
    public static final int FIREROOT_STUN = 0x00080000;
    public static final int STEALTH = 0x00100000;
    public static final int IMPRISIONING_1 = 0x00200000;
    public static final int IMPRISIONING_2 = 0x00400000;
    public static final int MAGIC_CIRCLE = 0x00800000;
    public static final int ICE2 = 0x01000000;
    public static final int EARTHQUAKE = 0x02000000;
    public static final int UNKNOWN_27 = 0x04000000;
    public static final int INVULNERABLE = 0x08000000;
    public static final int VITALITY = 0x10000000;
    public static final int REAL_TARGET = 0x20000000;
    public static final int DEATH_MARK = 0x40000000;
    public static final int SKULL_FEAR = 0x80000000;

    private AbnormalEffect() {
    }

    /**
     * Resolves the Interlude AbnormalEffect bitmask from the XML {@code abnormal} attribute
     * and/or the effect name.
     */
    public static int parseMask(String abnormalAttr, String effectName) {
        int mask = 0;
        if (abnormalAttr != null && !abnormalAttr.isBlank()) {
            String key = abnormalAttr.trim().toLowerCase(Locale.ROOT);
            mask |= switch (key) {
                case "bleed", "bleeding" -> BLEEDING;
                case "poison" -> POISON;
                case "redcircle" -> REDCIRCLE;
                case "ice" -> ICE;
                case "wind", "afflicted" -> WIND;
                case "fear" -> FEAR;
                case "stun" -> STUN;
                case "sleep" -> SLEEP;
                case "mute", "muted" -> MUTED;
                case "root" -> ROOT;
                case "hold_1", "hold1" -> HOLD_1;
                case "hold_2", "hold2" -> HOLD_2;
                case "bighead", "big_head" -> BIG_HEAD;
                case "flame" -> FLAME;
                case "grow" -> GROW;
                case "floatroot", "floating_root", "floatingroot" -> FLOATING_ROOT;
                case "dancestun", "dance_stunned", "dancestunned" -> DANCE_STUNNED;
                case "firerootstun", "fireroot_stun" -> FIREROOT_STUN;
                case "stealth", "shadow" -> STEALTH;
                case "imprison1", "imprisioning_1", "imprisoning_1" -> IMPRISIONING_1;
                case "imprison2", "imprisioning_2", "imprisoning_2" -> IMPRISIONING_2;
                case "magiccircle", "magic_circle" -> MAGIC_CIRCLE;
                case "ice2" -> ICE2;
                case "earthquake" -> EARTHQUAKE;
                case "invul", "invulnerable", "invincible" -> INVULNERABLE;
                case "vitality" -> VITALITY;
                case "realtarget", "real_target" -> REAL_TARGET;
                case "deathmark", "death_mark" -> DEATH_MARK;
                case "skull_fear", "skullfear" -> SKULL_FEAR;
                default -> {
                    if (key.startsWith("0x")) {
                        try {
                            yield Integer.parseUnsignedInt(key.substring(2), 16);
                        } catch (NumberFormatException ignored) {
                            yield 0;
                        }
                    }
                    yield 0;
                }
            };
        }

        if (effectName != null && !effectName.isBlank()) {
            mask |= switch (effectName.trim()) {
                case "Stun", "StunSelf" -> STUN;
                case "Sleep" -> SLEEP;
                case "Root" -> ROOT;
                case "Mute", "PhysicalMute", "SilenceMagicPhysical" -> MUTED;
                case "Paralyze", "Petrification" -> HOLD_1;
                case "Fear" -> FEAR;
                case "BigHead" -> BIG_HEAD;
                case "SilentMove" -> STEALTH;
                case "Invincible" -> INVULNERABLE;
                case "ClanGate" -> MAGIC_CIRCLE;
                case "ImmobileBuff", "ImmobilePetBuff" -> FLOATING_ROOT;
                default -> 0;
            };
        }
        return mask;
    }
}
