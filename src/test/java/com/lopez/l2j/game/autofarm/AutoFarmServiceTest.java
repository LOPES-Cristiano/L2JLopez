package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.shortcut.ShortCut;
import com.lopez.l2j.game.shortcut.ShortCutRepository;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AutoFarmServiceTest {

    private PlayerCharacter createPlayer(int objectId, String name) {
        return new PlayerCharacter(objectId, "Account", name, 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
                200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 200.0, 100.0, 100.0);
    }

    @Test
    void testToggleAndGetOrCreateState() {
        AutoFarmService service = new AutoFarmService();
        PlayerCharacter player = createPlayer(1001, "Hero");

        assertFalse(service.isAutoFarm(player.objectId()));
        boolean turnedOn = service.toggleAutoFarm(player);
        assertTrue(turnedOn);
        assertTrue(service.isAutoFarm(player.objectId()));

        var state = service.getOrCreateState(1001);
        assertNotNull(state);
        assertTrue(state.isEnabled());

        service.setFarmRadius(player.objectId(), 1400);
        assertEquals(1400, state.getFarmRadius());

        service.setAutoPotionHpThreshold(player.objectId(), 60.0);
        assertEquals(60.0, state.getAutoPotionHpThreshold());

        boolean turnedOff = service.toggleAutoFarm(player);
        assertFalse(turnedOff);
        assertFalse(service.isAutoFarm(player.objectId()));
    }

    @Test
    void testRenderHtmlDoesNotThrowAndContainsConfig() {
        AutoFarmService service = new AutoFarmService();
        PlayerCharacter player = createPlayer(2002, "MageHero");
        player.skills().put(1001, 1);

        SkillService skillService = mock(SkillService.class);
        SkillTemplate mockSkill = new SkillTemplate(
                1001, 1, "Wind Strike", SkillTemplate.OperateType.ACTIVE, "MDAM", "TARGET_ONE",
                true, 10, 2, 0, 20.0, 600, 0, 1500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        when(skillService.known(any(), anyInt())).thenReturn(Optional.of(mockSkill));

        var state = service.getOrCreateState(2002);
        state.setMode(AutoFarmMode.MAGE);

        String html = service.renderHtml(player, skillService);
        assertNotNull(html);
        assertTrue(html.contains("Wind Strike"));
        assertTrue(html.contains("voiced_autofarm"));
        assertTrue(html.contains("Mage"));
    }

    @Test
    void testPremiumCommandsArchetypesTabsAndTacticalCheckboxes() {
        AutoFarmService service = new AutoFarmService();
        PlayerCharacter player = createPlayer(3003, "PremiumTester");
        GameSession session = mock(GameSession.class);
        when(session.activeCharacter()).thenReturn(player);

        // 1. Verifica modo gratuito por padrao
        assertTrue(Config.AUTOFARM_IS_FREE);

        // 2. Testa troca de arquetipos via voice_autofarm <Archetype>
        service.handleCommand(session, "voice_autofarm Archer");
        AutoFarmState state = service.getOrCreateState(player.objectId());
        assertEquals(1, state.getFarmType());
        assertEquals(AutoFarmMode.ARCHER, state.getMode());

        service.handleCommand(session, "voice_autofarm Mage");
        assertEquals(2, state.getFarmType());
        assertEquals(AutoFarmMode.MAGE, state.getMode());

        service.handleCommand(session, "voice_autofarm Healer");
        assertEquals(3, state.getFarmType());
        assertEquals(AutoFarmMode.HEALER, state.getMode());

        service.handleCommand(session, "voice_autofarm Summon");
        assertEquals(4, state.getFarmType());
        assertEquals(AutoFarmMode.SUMMON, state.getMode());

        // 3. Testa checkboxes taticas via voice_editFarmOption
        assertFalse(state.isLeaderAssist());
        service.handleCommand(session, "voice_editFarmOption attack assist");
        assertTrue(state.isLeaderAssist());

        assertFalse(state.isKeepLocation());
        service.handleCommand(session, "voice_editFarmOption attack keepLocation");
        assertTrue(state.isKeepLocation());
        assertFalse(state.isLeaderAssist()); // Mutuamente exclusivo com keepLocation

        assertFalse(state.isExtraDelaySkill());
        service.handleCommand(session, "voice_editFarmOption attack exDelaySkill");
        assertTrue(state.isExtraDelaySkill());

        // 4. Testa configuracao de chance e limite de MP/HP das abas de skills + distancia
        service.handleCommand(session, "voice_editFarmOption attack attackChance 85");
        assertEquals(85, state.getAttackChance());

        service.handleCommand(session, "voice_editFarmOption self selfPercent 25");
        assertEquals(25, state.getSelfPercent());

        service.handleCommand(session, "voice_editFarmOption attack distance 1600");
        assertEquals(1600, state.getFarmRadius());

        // 5. Testa ACP (Auto Combat Potions HP/MP/CP)
        service.handleCommand(session, "voice_farmacp set_hp 75");
        assertEquals(75, state.getAcpHpPercent());

        service.handleCommand(session, "voice_farmacp set_mp 50");
        assertEquals(50, state.getAcpMpPercent());

        service.handleCommand(session, "voice_farmacp off_hp");
        assertEquals(0, state.getAcpHpPercent());
    }

    @Test
    void testShortcutSkillSyncAndCategorization() {
        SkillTable skillTable = mock(SkillTable.class);
        ShortCutRepository shortCutRepository = mock(ShortCutRepository.class);

        SkillTemplate attackSkill = new SkillTemplate(
                1177, 1, "Wind Strike", SkillTemplate.OperateType.ACTIVE, "MDAM", "TARGET_ONE",
                true, 10, 2, 0, 20.0, 600, 0, 1500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate spoilSkill = new SkillTemplate(
                254, 1, "Spoil", SkillTemplate.OperateType.ACTIVE, "SPOIL", "TARGET_ONE",
                false, 10, 0, 0, 0.0, 40, 0, 1500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate selfBuffSkill = new SkillTemplate(
                1040, 1, "Shield", SkillTemplate.OperateType.ACTIVE, "BUFF", "TARGET_SELF",
                true, 10, 2, 0, 0.0, 0, 0, 1500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, false, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate healSkill = new SkillTemplate(
                1015, 1, "Battle Heal", SkillTemplate.OperateType.ACTIVE, "HEAL", "TARGET_ONE",
                true, 15, 2, 0, 100.0, 600, 0, 1500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, false, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );

        when(skillTable.get(1177, 1)).thenReturn(Optional.of(attackSkill));
        when(skillTable.get(254, 1)).thenReturn(Optional.of(spoilSkill));
        when(skillTable.get(1040, 1)).thenReturn(Optional.of(selfBuffSkill));
        when(skillTable.get(1015, 1)).thenReturn(Optional.of(healSkill));

        AutoFarmService service = new AutoFarmService();
        ReflectionTestUtils.setField(service, "skillTable", skillTable);
        ReflectionTestUtils.setField(service, "shortCutRepository", shortCutRepository);

        PlayerCharacter player = createPlayer(4004, "ShortcutTester");
        player.skills().put(1177, 1);
        player.skills().put(254, 1);
        player.skills().put(1040, 1);
        player.skills().put(1015, 1);

        // Registra atalhos na barra F10 (page 9 = 10 - 1, slots 0..3)
        List<ShortCut> shortcuts = List.of(
                new ShortCut(0, 9, ShortCut.TYPE_SKILL, 1177, 1, 1),
                new ShortCut(1, 9, ShortCut.TYPE_SKILL, 254, 1, 1),
                new ShortCut(2, 9, ShortCut.TYPE_SKILL, 1040, 1, 1),
                new ShortCut(3, 9, ShortCut.TYPE_SKILL, 1015, 1, 1)
        );
        when(shortCutRepository.findByCharId(eq(4004), anyInt())).thenReturn(shortcuts);

        GameSession session = mock(GameSession.class);
        when(session.activeCharacter()).thenReturn(player);

        service.handleCommand(session, "voice_refreshSkills attack 10");
        AutoFarmState state = service.getOrCreateState(player.objectId());

        assertTrue(state.getAttackSpells().contains(1177));
        assertTrue(state.getChanceSpells().contains(254));
        assertTrue(state.getSelfSpells().contains(1040));
        assertTrue(state.getLowLifeSpells().contains(1015));

        // Testa processTick usando as skills sincronizadas
        NpcInstance monster = mock(NpcInstance.class);
        when(monster.objectId()).thenReturn(90001);
        when(monster.isDead()).thenReturn(false);
        when(monster.x()).thenReturn(100);
        when(monster.y()).thenReturn(100);

        state.setEnabled(true);
        state.setAttackChance(100);
        state.setChanceChance(0);
        state.setSelfChance(0);
        AutoFarmAction action = service.processTick(player, List.of(monster));
        assertEquals(AutoFarmAction.ActionType.CAST_SKILL, action.type());
        assertEquals(1177, action.skillId());
    }

    @Test
    void testSpoilRotationAndAutoSweepWhenSpoilAndSweeperPlacedInAttackTab() {
        SkillTable skillTable = mock(SkillTable.class);

        SkillTemplate spoil254 = new SkillTemplate(
                254, 1, "Spoil", SkillTemplate.OperateType.ACTIVE, "SPOIL", "TARGET_ONE",
                false, 10, 0, 0, 0.0, 40, 0, 1500, 0, 3000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate sweeper42 = new SkillTemplate(
                42, 1, "Sweeper", SkillTemplate.OperateType.ACTIVE, "SWEEP", "TARGET_CORPSE_MOB",
                false, 0, 0, 0, 0.0, 40, 0, 500, 0, 1000, 20, 0.0, false,
                0, 0, 0, 0, 0, false, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate spoilFestival302 = new SkillTemplate(
                302, 1, "Spoil Festival", SkillTemplate.OperateType.ACTIVE, "SPOIL", "TARGET_AREA",
                false, 20, 0, 0, 0.0, 40, 200, 1500, 0, 3000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );
        SkillTemplate spoilCrush348 = new SkillTemplate(
                348, 1, "Spoil Crush", SkillTemplate.OperateType.ACTIVE, "PDAM", "TARGET_ONE",
                false, 15, 0, 0, 50.0, 40, 0, 1500, 0, 3000, 20, 0.0, false,
                0, 0, 0, 0, 0, true, false,
                Collections.emptyList(), Collections.emptyList(), null, null
        );

        when(skillTable.get(254, 1)).thenReturn(Optional.of(spoil254));
        when(skillTable.get(42, 1)).thenReturn(Optional.of(sweeper42));
        when(skillTable.get(302, 1)).thenReturn(Optional.of(spoilFestival302));
        when(skillTable.get(348, 1)).thenReturn(Optional.of(spoilCrush348));

        AutoFarmService service = new AutoFarmService();
        ReflectionTestUtils.setField(service, "skillTable", skillTable);

        PlayerCharacter player = createPlayer(5005, "FortuneSeeker");
        player.skills().put(254, 1);
        player.skills().put(42, 1);
        player.skills().put(302, 1);
        player.skills().put(348, 1);

        AutoFarmState state = service.getOrCreateState(player.objectId());
        state.setEnabled(true);
        state.setRndAttackSkills(false);
        state.setAttackChance(100);
        // Simula configuracao da screenshot: Spoil (254), Sweeper (42), Spoil Festival (302), Spoil Crush (348) na aba ATTACK
        state.getAttackSpells().clear();
        state.getAttackSpells().addAll(List.of(254, 42, 302, 348));

        NpcInstance chest = mock(NpcInstance.class);
        when(chest.objectId()).thenReturn(95001);
        when(chest.isAttackable()).thenReturn(true);
        when(chest.isDead()).thenReturn(false);
        when(chest.isSpoiled()).thenReturn(false);
        when(chest.x()).thenReturn(20);
        when(chest.y()).thenReturn(20);

        // 1. Monstro nao spoilado: seleciona Spoil (254)
        AutoFarmAction action1 = service.processTick(player, List.of(chest));
        assertEquals(AutoFarmAction.ActionType.CAST_SKILL, action1.type());
        assertEquals(254, action1.skillId());

        // 2. Proximo tick (round-robin) e monstro ainda nao foi spoilado -> deve pular Sweeper (42) e usar Spoil Festival (302)
        AutoFarmAction action2 = service.processTick(player, List.of(chest));
        assertEquals(AutoFarmAction.ActionType.CAST_SKILL, action2.type());
        assertEquals(302, action2.skillId());

        // 3. Monstro agora esta spoilado -> deve ignorar Spoil (254), Sweeper (42) e Spoil Festival (302) e usar Spoil Crush (348)
        when(chest.isSpoiled()).thenReturn(true);
        AutoFarmAction action3 = service.processTick(player, List.of(chest));
        assertEquals(AutoFarmAction.ActionType.CAST_SKILL, action3.type());
        assertEquals(348, action3.skillId());
    }
}
