package com.lopez.l2j.game.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameTimeControllerTest {

	@Test
	void calculatesDayAndNightCycles() {
		GameTimeController controller = new GameTimeController(null);
		assertNotNull(controller);

		// Ciclo de 360 minutos (00:00 a 06:00) e noite
		assertTrue(GameTimeController.isNightTime(0));
		assertTrue(GameTimeController.isNightTime(180));
		assertTrue(GameTimeController.isNightTime(359));

		// Dia: 360 minutos em diante (06:00 as 24:00)
		assertFalse(GameTimeController.isNightTime(360));
		assertFalse(GameTimeController.isNightTime(720));
		assertFalse(GameTimeController.isNightTime(1439));

		// Horas e minutos calculados
		int hour = controller.getGameHour();
		int min = controller.getGameMinute();
		assertTrue(hour >= 0 && hour < 24);
		assertTrue(min >= 0 && min < 60);
	}
}
