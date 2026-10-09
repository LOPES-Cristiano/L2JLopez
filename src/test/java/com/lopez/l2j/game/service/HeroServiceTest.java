package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeroServiceTest {

	private HeroService heroService;

	private PlayerCharacter createPlayer(int objId, String name) {
		return new PlayerCharacter(objId, "acc", name, 80, 0L, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 1000, 1000, 0, 0, 0, 0, "Title", 0, 0L, 0L, 0, 0, 0, 0, 1000.0, 1000.0, 1000.0);
	}

	@BeforeEach
	void setUp() {
		heroService = new HeroService();
	}

	@Test
	void testSetHeroAndDuration() {
		PlayerCharacter player = createPlayer(1001, "Cristiano");
		assertFalse(player.isHero());
		assertEquals(0L, player.heroExpiration());

		// Concede 10 dias de hero
		long exp1 = heroService.setHero(player, 10);
		assertTrue(player.isHero());
		assertTrue(exp1 > System.currentTimeMillis());
		assertEquals(exp1, player.heroExpiration());

		// Se o admin concede +1 dia para o Cristiano que ja tem 10 dias,
		// o tempo deve SOMAR com o prazo existente (10 + 1 = 11 dias)
		long exp2 = heroService.setHero(player, 1);
		assertTrue(player.isHero());
		long diff = exp2 - exp1;
		// A diferenca deve ser exatamente 1 dia (86_400_000 ms)
		assertEquals(86_400_000L, diff);

		// Remove hero
		heroService.removeHero(player);
		assertFalse(player.isHero());
		assertEquals(0L, player.heroExpiration());
	}

	@Test
	void testHeroExpirationWhenTimePasses() {
		PlayerCharacter player = createPlayer(1002, "HeroExpired");
		heroService.setHero(player, 1);
		assertTrue(player.isHero());

		// Simula que o tempo expirou no passado
		player.heroExpiration(System.currentTimeMillis() - 1000L);
		assertTrue(heroService.isExpired(player));

		heroService.removeHero(player);
		assertFalse(player.isHero());
	}
}
