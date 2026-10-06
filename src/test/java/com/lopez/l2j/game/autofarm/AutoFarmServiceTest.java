package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoFarmServiceTest {

    private AutoFarmService autoFarmService;

    private PlayerCharacter dummyPlayer(int id, String name, int level) {
        return new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
                0, 0, 0, 1000, 500, 300, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 500.0, 300.0);
    }

    private NpcInstance dummyNpc(int objectId, int x, int y, int z) {
        NpcTemplate tmpl = new NpcTemplate(20001, 20001, "Wolf", false, "", false, 10.0, 15.0, 20, "male",
                "L2Monster", 40, 500, 200, 10, 30, 5, 15, 200, 200, 0, 0, 0, 50, 100, 0, false);
        return new NpcInstance(objectId, tmpl, x, y, z, 0);
    }

    @BeforeEach
    void setUp() {
        autoFarmService = new AutoFarmService();
    }

    @Test
    void testToggleAutoFarm() {
        PlayerCharacter player = dummyPlayer(1, "FarmerOne", 40);

        boolean active = autoFarmService.toggleAutoFarm(player);
        assertTrue(active);
        assertTrue(autoFarmService.isAutoFarm(1));

        boolean disabled = autoFarmService.toggleAutoFarm(player);
        assertFalse(disabled);
        assertFalse(autoFarmService.isAutoFarm(1));

        // Jogador morto não pode ativar
        PlayerCharacter deadPlayer = dummyPlayer(2, "DeadFarmer", 40);
        deadPlayer.currentHp(0);
        assertFalse(autoFarmService.toggleAutoFarm(deadPlayer));
    }

    @Test
    void testProcessTickSelectsClosestMonster() {
        PlayerCharacter player = dummyPlayer(1, "FarmerOne", 40);
        player.teleport(0, 0, 0);
        autoFarmService.toggleAutoFarm(player);

        NpcInstance farNpc = dummyNpc(101, 800, 0, 0);
        NpcInstance closeNpc = dummyNpc(102, 200, 0, 0);

        AutoFarmAction action = autoFarmService.processTick(player, List.of(farNpc, closeNpc));
        assertEquals(AutoFarmAction.ActionType.ATTACK, action.type());
        assertEquals(102, action.targetId());
    }

    @Test
    void testProcessTickAutoPotionWhenLowHp() {
        PlayerCharacter player = dummyPlayer(1, "FarmerOne", 40);
        player.teleport(0, 0, 0);
        player.currentHp(400); // 400 < 600 (60% de 1000)
        autoFarmService.toggleAutoFarm(player);

        NpcInstance npc = dummyNpc(101, 200, 0, 0);
        AutoFarmAction action = autoFarmService.processTick(player, List.of(npc));

        assertEquals(AutoFarmAction.ActionType.USE_POTION, action.type());
        assertEquals(AutoFarmService.GREATER_HEALING_POTION_ID, action.skillId());
    }

    @Test
    void testProcessTickMageModeCastsSkill() {
        PlayerCharacter player = dummyPlayer(1, "MageFarmer", 60);
        player.teleport(0, 0, 0);
        autoFarmService.toggleAutoFarm(player);
        autoFarmService.setMode(1, AutoFarmMode.MAGE);
        autoFarmService.setSelectedSkill(1, 1177); // Hydro Blast

        NpcInstance npc = dummyNpc(101, 500, 0, 0);
        AutoFarmAction action = autoFarmService.processTick(player, List.of(npc));

        assertEquals(AutoFarmAction.ActionType.CAST_SKILL, action.type());
        assertEquals(101, action.targetId());
        assertEquals(1177, action.skillId());
    }
}
