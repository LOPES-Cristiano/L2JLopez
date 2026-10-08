package com.lopez.l2j.game.geodata;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitarios para a Onda C7:
 * Portas e Cercas Dinamicas na Geodata (GeoObject, FenceInstance, DoorInstance).
 *
 * <p>Criterio de Aceite:</p>
 * <ul>
 *   <li>canSeeTarget() e canMoveToTarget() retornando false com a porta do castelo fechada.</li>
 *   <li>canSeeTarget() e canMoveToTarget() retornando true assim que a porta recebe o status de aberta.</li>
 *   <li>Cercas dinamicas bloqueando passagem quando ativas e liberando quando abertas.</li>
 * </ul>
 */
class DynamicGeoObjectTest {

	private GeoEngine geoEngine;
	private CombatService combatService;

	@BeforeEach
	void setUp() {
		com.lopez.l2j.config.Config.ENABLE_GEODATA = true;
		geoEngine = new GeoEngine("target/empty_geodata", true, null);
		combatService = new CombatService(1.0, 1.0, geoEngine, null);
	}

	private PlayerCharacter createPlayer(int objId, String name, int x, int y, int z) {
		return new PlayerCharacter(
				objId, "acc_" + objId, name, 78, 100000000L, 5000000, 0,
				88, 88, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, x, y, z, 0, 5000.0,
				3000.0, 4000.0
		);
	}

	@Test
	@DisplayName("Criterio Onda C7: Porta fechada bloqueia visao (LoS) e movimento, porta aberta libera ambos")
	void testCastleDoorDynamicBlockingAndOpening() {
		// Porta de castelo posicionada entre X: 1000..1050, Y: 1000..1200
		DoorInstance castleDoor = new DoorInstance(
				101, 1, "Castle Main Gate",
				1025, 1100, 0,
				1000, 1050, 1000, 1200, -100, 200,
				100000, 1000, 1000, false, false // fechada
		);

		geoEngine.addGeoObject(castleDoor);

		// Player 1 fora da porta (X=900, Y=1100)
		PlayerCharacter attacker = createPlayer(1, "Attacker", 900, 1100, 0);
		// Player 2 dentro do castelo (X=1200, Y=1100)
		PlayerCharacter defender = createPlayer(2, "Defender", 1200, 1100, 0);

		// 1. Porta FECHADA -> Bloqueia Visao e Movimento
		assertFalse(castleDoor.isOpen());
		assertTrue(castleDoor.isBlocking());
		assertFalse(geoEngine.canSeeTarget(attacker.x(), attacker.y(), attacker.z(), defender.x(), defender.y(), defender.z()),
				"Com a porta fechada, a linha de visao 3D LoS DEVE ser bloqueada");
		assertFalse(geoEngine.canMoveToTarget(attacker.x(), attacker.y(), attacker.z(), defender.x(), defender.y(), defender.z()),
				"Com a porta fechada, o caminho fisico DEVE ser bloqueado");
		assertFalse(combatService.canSeeTarget(attacker, defender),
				"CombatService deve reportar LoS bloqueado");
		assertFalse(combatService.canMoveToTarget(attacker, defender),
				"CombatService deve reportar caminho bloqueado");

		// 2. Abre a porta -> Libera Visao e Movimento
		castleDoor.openDoor();
		assertTrue(castleDoor.isOpen());
		assertFalse(castleDoor.isBlocking());

		assertTrue(geoEngine.canSeeTarget(attacker.x(), attacker.y(), attacker.z(), defender.x(), defender.y(), defender.z()),
				"Com a porta aberta, a linha de visao DEVE ser liberada instantaneamente");
		assertTrue(geoEngine.canMoveToTarget(attacker.x(), attacker.y(), attacker.z(), defender.x(), defender.y(), defender.z()),
				"Com a porta aberta, a passagem fisica DEVE ser liberada instantaneamente");
		assertTrue(combatService.canSeeTarget(attacker, defender),
				"CombatService deve reportar LoS desobstruido com porta aberta");
		assertTrue(combatService.canMoveToTarget(attacker, defender),
				"CombatService deve reportar caminho liberado com porta aberta");
	}

	@Test
	@DisplayName("Cerca Dinamica (FenceInstance): Bloqueia quando ativa e libera quando removida/aberta")
	void testDynamicFenceObstacle() {
		// Cerca dinamica bloqueando travessia
		FenceInstance fence = new FenceInstance(
				201, "Colosseum Fence", 500, 500, 0,
				480, 520, 400, 600, -50, 150, FenceInstance.STATE_BLOCKING
		);

		geoEngine.addGeoObject(fence);

		int x1 = 400, y1 = 500, z1 = 0;
		int x2 = 600, y2 = 500, z2 = 0;

		// 1. Cerca ativa -> Bloqueado
		assertTrue(fence.isBlocking());
		assertFalse(geoEngine.canSeeTarget(x1, y1, z1, x2, y2, z2));
		assertFalse(geoEngine.canMoveToTarget(x1, y1, z1, x2, y2, z2));

		// 2. Cerca desativada / aberta -> Passavel
		fence.open();
		assertFalse(fence.isBlocking());
		assertTrue(geoEngine.canSeeTarget(x1, y1, z1, x2, y2, z2));
		assertTrue(geoEngine.canMoveToTarget(x1, y1, z1, x2, y2, z2));

		// 3. Remocao do registro de GeoObject
		geoEngine.removeGeoObject(fence);
		assertFalse(geoEngine.dynamicGeoObjects().contains(fence));
	}

	@Test
	@DisplayName("Porta Destruida (HP <= 0): Considerada nao-bloqueante na Geodata")
	void testDestroyedDoorBecomesPassable() {
		DoorInstance door = new DoorInstance(
				301, 2, "Siege Wall Door",
				2000, 2000, 0,
				1950, 2050, 1950, 2050, -50, 100,
				50000, 500, 500, false, false // fechada
		);

		geoEngine.addGeoObject(door);
		assertTrue(door.isBlocking());

		// Porta e atacada e destruida
		door.currentHp(0);
		assertFalse(door.isBlocking(), "Porta com HP zerado (destruida em siege) nao bloqueia mais");

		assertTrue(geoEngine.canSeeTarget(1900, 2000, 0, 2100, 2000, 0));
		assertTrue(geoEngine.canMoveToTarget(1900, 2000, 0, 2100, 2000, 0));
	}
}
