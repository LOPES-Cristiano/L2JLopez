package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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
}
