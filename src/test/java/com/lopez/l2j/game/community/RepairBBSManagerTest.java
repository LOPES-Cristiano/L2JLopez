package com.lopez.l2j.game.community;

import com.lopez.l2j.game.model.InMemoryCharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepairBBSManagerTest {

    private InMemoryCharacterRepository repo;
    private RepairBBSManager repairManager;

    @BeforeEach
    void setUp() {
        repo = new InMemoryCharacterRepository();
        repairManager = new RepairBBSManager(repo);
    }

    private PlayerCharacter create(int objectId, String account, String name, int x, int y, int z) {
        PlayerCharacter pc = new PlayerCharacter(objectId, account, name, 70, 0, 0, 0, 1, 1, false,
                0, 0, 0, 1000, 500, 500, 0, 0, 0, 0, "Title", 0, 0, 0,
                x, y, z, 0, 1000.0, 500.0, 500.0);
        repo.saveState(pc, false);
        return pc;
    }

    @Test
    void testRepairStuckCharacterOnSameAccount() {
        PlayerCharacter activeChar = create(1, "myAccount", "ActiveHero", 1000, 2000, -3000);
        PlayerCharacter stuckChar = create(2, "myAccount", "StuckAlt", 999999, 999999, -999999);

        // Repara o char alternativo da mesma conta
        var result = repairManager.repairCharacter(activeChar, "StuckAlt");
        assertEquals(RepairBBSManager.RepairResult.SUCCESS, result);

        // Verifica que as coordenadas foram corrigidas para Dion (vila segura)
        PlayerCharacter repaired = repo.findByName("StuckAlt").orElseThrow();
        assertEquals(RepairBBSManager.REPAIR_X, repaired.getX());
        assertEquals(RepairBBSManager.REPAIR_Y, repaired.getY());
        assertEquals(RepairBBSManager.REPAIR_Z, repaired.getZ());
    }

    @Test
    void testCannotRepairCurrentCharOrOtherAccount() {
        PlayerCharacter activeChar = create(1, "myAccount", "ActiveHero", 1000, 2000, -3000);
        PlayerCharacter otherChar = create(3, "otherAccount", "OtherPlayer", 5000, 5000, -5000);

        // Nao pode reparar o proprio personagem online
        assertEquals(RepairBBSManager.RepairResult.CANNOT_REPAIR_CURRENT_CHAR,
                repairManager.repairCharacter(activeChar, "ActiveHero"));

        // Nao pode reparar personagem de outra conta
        assertEquals(RepairBBSManager.RepairResult.NOT_SAME_ACCOUNT,
                repairManager.repairCharacter(activeChar, "OtherPlayer"));
    }

    @Test
    void testBuildRepairHtml() {
        PlayerCharacter activeChar = create(1, "myAccount", "ActiveHero", 1000, 2000, -3000);
        create(2, "myAccount", "StuckAlt", 999999, 999999, -999999);

        String html = repairManager.buildRepairHtml(activeChar);
        assertTrue(html.contains("DESENCALHE"));
        assertTrue(html.contains("StuckAlt"));
        assertFalse(html.contains("ActiveHero</font></td>")); // O personagem atual nao deve aparecer para reparo
    }
}
