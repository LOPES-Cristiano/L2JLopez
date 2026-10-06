package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StarterKitServiceTest {

    private StarterKitService kitService;

    @BeforeEach
    void setUp() {
        kitService = new StarterKitService();
    }

    private PlayerCharacter createFighter(int objectId) {
        return new PlayerCharacter(objectId, "acc1", "FighterBoy", 1, 0, 0, 0, 0, 0, false, // race 0, class 0 (Human Fighter)
                0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0,
                0, 0, 0, 0, 100.0, 50.0, 50.0);
    }

    private PlayerCharacter createMage(int objectId) {
        return new PlayerCharacter(objectId, "acc1", "MageGirl", 1, 0, 0, 0, 10, 10, true, // class 10 (Human Mystic)
                0, 0, 0, 100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0,
                0, 0, 0, 0, 100.0, 50.0, 50.0);
    }

    @Test
    void testClaimFighterKit() {
        PlayerCharacter fighter = createFighter(101);
        assertFalse(kitService.hasClaimed(101));

        List<StarterKitService.KitItem> rewards = kitService.claimStarterKit(fighter);
        assertFalse(rewards.isEmpty());
        assertTrue(kitService.hasClaimed(101));

        // Contem arma de fighter e soulshots
        assertTrue(rewards.stream().anyMatch(i -> i.name().contains("Short Sword")));
        assertTrue(rewards.stream().anyMatch(i -> i.name().contains("Soulshot")));

        // Tentativa de segundo resgate: lista vazia
        List<StarterKitService.KitItem> second = kitService.claimStarterKit(fighter);
        assertTrue(second.isEmpty());
    }

    @Test
    void testClaimMageKit() {
        PlayerCharacter mage = createMage(202);
        List<StarterKitService.KitItem> rewards = kitService.claimStarterKit(mage);

        // Contem staff e spiritshots
        assertTrue(rewards.stream().anyMatch(i -> i.name().contains("Staff")));
        assertTrue(rewards.stream().anyMatch(i -> i.name().contains("Spiritshot")));
    }
}
