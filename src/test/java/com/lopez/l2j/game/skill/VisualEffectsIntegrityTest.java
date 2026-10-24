package com.lopez.l2j.game.skill;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.network.game.GameSession;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Validates that skill visual IDs, AbnormalEffect bitmasks, Scroll of Escape animations,
 * Spiritshot/Blessed Spiritshot grades, and XML attributes (displayId, staticHitTime,
 * staticReuse, abnormal) match Interlude Protocol 746 and L2JDreamV2 retail specifications.
 */
class VisualEffectsIntegrityTest {

    @Test
    void scrollOfEscapeUsesCorrectInterludeSkillIdsInsteadOfResurrection() {
        // 736: Scroll of Escape -> 2013 lvl 1 (20000ms), NOT 2014 (Scroll of Resurrection)
        assertArrayEquals(new int[] { 2013, 1, 20000 }, GameSession.resolveScrollOfEscapeSkill(736));
        // 1538 & 3958: Blessed Scroll of Escape -> 2036 lvl 1 (200ms)
        assertArrayEquals(new int[] { 2036, 1, 200 }, GameSession.resolveScrollOfEscapeSkill(1538));
        assertArrayEquals(new int[] { 2036, 1, 200 }, GameSession.resolveScrollOfEscapeSkill(3958));
        // 9156: Blessed Scroll of Escape Event -> 2036 lvl 2 (200ms)
        assertArrayEquals(new int[] { 2036, 2, 200 }, GameSession.resolveScrollOfEscapeSkill(9156));
        // 1829 & 5858: Clan Hall Escape & Blessed Clan Hall Escape
        assertArrayEquals(new int[] { 2040, 1, 20000 }, GameSession.resolveScrollOfEscapeSkill(1829));
        assertArrayEquals(new int[] { 2177, 1, 200 }, GameSession.resolveScrollOfEscapeSkill(5858));
        // 1830 & 5859: Castle Escape & Blessed Castle Escape
        assertArrayEquals(new int[] { 2041, 1, 20000 }, GameSession.resolveScrollOfEscapeSkill(1830));
        assertArrayEquals(new int[] { 2178, 1, 200 }, GameSession.resolveScrollOfEscapeSkill(5859));
        // 7117..7135: Town Scrolls -> 2213 lvl 1..19
        assertArrayEquals(new int[] { 2213, 1, 20000 }, GameSession.resolveScrollOfEscapeSkill(7117));
        assertArrayEquals(new int[] { 2213, 19, 20000 }, GameSession.resolveScrollOfEscapeSkill(7135));
        // 7554..7559: Quest Town Scrolls -> 2214 lvl 1..6
        assertArrayEquals(new int[] { 2214, 1, 20000 }, GameSession.resolveScrollOfEscapeSkill(7554));
        assertArrayEquals(new int[] { 2214, 6, 20000 }, GameSession.resolveScrollOfEscapeSkill(7559));
        // 7618 & 7619: Ketra / Varka Village -> 2213 lvl 20 / 21
        assertArrayEquals(new int[] { 2213, 20, 20000 }, GameSession.resolveScrollOfEscapeSkill(7618));
        assertArrayEquals(new int[] { 2213, 21, 20000 }, GameSession.resolveScrollOfEscapeSkill(7619));
    }

    @Test
    void spiritshotsAndConsumablesHaveExactGradeSkillIds() {
        // Spiritshots: No-grade=2047, D=2155, C=2156, B=2157, A=2158, S=2159
        assertEquals(2047, ConsumableTable.get(2509).orElseThrow().skillId());
        assertEquals(2155, ConsumableTable.get(2510).orElseThrow().skillId());
        assertEquals(2156, ConsumableTable.get(2511).orElseThrow().skillId());
        assertEquals(2157, ConsumableTable.get(2512).orElseThrow().skillId());
        assertEquals(2158, ConsumableTable.get(2513).orElseThrow().skillId());
        assertEquals(2159, ConsumableTable.get(2514).orElseThrow().skillId());

        // Blessed Spiritshots: No-grade=2061, D=2160, C=2161, B=2162, A=2163, S=2164
        assertEquals(2061, ConsumableTable.get(3947).orElseThrow().skillId());
        assertEquals(2160, ConsumableTable.get(3948).orElseThrow().skillId());
        assertEquals(2161, ConsumableTable.get(3949).orElseThrow().skillId());
        assertEquals(2162, ConsumableTable.get(3950).orElseThrow().skillId());
        assertEquals(2163, ConsumableTable.get(3951).orElseThrow().skillId());
        assertEquals(2164, ConsumableTable.get(3952).orElseThrow().skillId());

        // Appearance Potions (5235..5248 -> 2122..2135)
        for (int itemId = 5235; itemId <= 5248; itemId++) {
            assertEquals(2122 + (itemId - 5235), ConsumableTable.get(itemId).orElseThrow().skillId());
        }

        // Remedies: Bandage (1833 -> 2044), Potion of Recovery (3889 -> 2060)
        assertEquals(2044, ConsumableTable.get(1833).orElseThrow().skillId());
        assertEquals(2060, ConsumableTable.get(3889).orElseThrow().skillId());
    }

    @Test
    void skillTableParsesDisplayIdStaticTimingsAndAbnormalEffectsFromXml() {
        SkillTable table = new SkillTable(Path.of("data", "xml", "stats", "skills"));
        assertTrue(table.size() > 0, "SkillTable should load XML skills");

        // Skill 221 (Silent Move): STEALTH abnormal (0x00100000)
        var silentMove = table.get(221, 1).orElse(null);
        assertNotNull(silentMove);
        assertTrue(silentMove.effects().stream().anyMatch(e -> (e.abnormalMask() & AbnormalEffect.STEALTH) != 0));

        // Skill 1418 (Celestial Shield): INVULNERABLE abnormal (0x08000000)
        var celestial = table.get(1418, 1).orElse(null);
        assertNotNull(celestial);
        assertTrue(celestial.effects().stream().anyMatch(e -> (e.abnormalMask() & AbnormalEffect.INVULNERABLE) != 0));

        // Skill 2013 (Scroll of Escape): staticHitTime="true"
        var soe = table.get(2013, 1).orElse(null);
        assertNotNull(soe);
        assertTrue(soe.staticHitTime());

        // AbnormalEffect bitmask constants
        assertEquals(0x00800000, AbnormalEffect.MAGIC_CIRCLE);
        assertEquals(0x08000000, AbnormalEffect.INVULNERABLE);
        assertEquals(0x00004000, AbnormalEffect.parseMask("flame", "Buff"));
        assertEquals(0x00000008, AbnormalEffect.parseMask("ice", "Debuff"));
        assertEquals(0x00000004, AbnormalEffect.parseMask("redcircle", "Debuff"));
    }
}
