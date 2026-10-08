package com.lopez.l2j.game.geodata;

import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoEngineTest {

	private static GeoEngine geoEngine;
	private static DoorTable doorTable;

	@BeforeAll
	static void setUp() {
		com.lopez.l2j.config.Config.ENABLE_GEODATA = true;
		doorTable = new DoorTable();
		// Testa se a pasta data/geodata existe no diretorio raiz
		File geodataDir = new File("data/geodata");
		if (geodataDir.exists() && geodataDir.isDirectory()) {
			geoEngine = new GeoEngine("data/geodata", doorTable);
		} else {
			geoEngine = new GeoEngine(null, doorTable);
		}
	}

	@Test
	@DisplayName("GeoEngine deve inicializar com sucesso sem erros")
	void testInitialization() {
		assertNotNull(geoEngine, "GeoEngine deve ser instanciado");
	}

	@Test
	@DisplayName("Verificacao de altura (getHeight) em coordenadas de Talking Island e Giran")
	void testGetHeight() {
		// Talking Island Village: ~ -84000, 244000, -3700
		short zTalking = geoEngine.getHeight(-84000, 244000, -3700);
		assertTrue(zTalking < 0 && zTalking > -6000, "Altura de Talking Island deve ser no range esperado: " + zTalking);

		// Giran Castle Town: ~ 83400, 148000, -3400 (terreno plano exato -3400)
		short zGiran = geoEngine.getHeight(83400, 148000, -3400);
		assertEquals(-3400, zGiran, "Giran deve retornar a altura exata do bloco flat");
	}

	@Test
	@DisplayName("Linha de visao (canSeeTarget) desobstruida em curta distancia")
	void testCanSeeTargetClear() {
		// Em linha reta plana a curta distancia em Giran (-3400), a visao deve ser livre
		boolean see = geoEngine.canSeeTarget(83400, 148000, -3400, 83450, 148000, -3400);
		assertTrue(see, "Deve ver alvo proximo sem obstaculo");
	}

	@Test
	@DisplayName("Linha de visao (canSeeTarget) deve ser bloqueada por porta fechada")
	void testCanSeeTargetBlockedByClosedDoor() {
		// Utiliza a porta de Gludin Clan Hall carregada do door.xml (ID 17220001 em X=-84495, Y=155209, Z=-3150)
		DoorInstance door = doorTable.getDoor(17220001);
		assertNotNull(door, "Porta 17220001 deve existir no DoorTable");

		door.close();
		// Linha de visao atravessando a porta (de X=-84450 para X=-84550 em Y=155225, Z=-3150)
		boolean seeBlocked = geoEngine.canSeeTarget(-84450, 155225, -3150, -84550, 155225, -3150);
		assertFalse(seeBlocked, "Linha de visao deve ser bloqueada por porta fechada");

		// Abre a porta e checa novamente
		door.open();
		boolean seeOpen = geoEngine.canSeeTarget(-84450, 155225, -3150, -84550, 155225, -3150);
		assertTrue(seeOpen, "Linha de visao deve estar liberada quando a porta esta aberta");
	}

	@Test
	@DisplayName("Verificacao de movimento simples (moveCheck)")
	void testMoveCheck() {
		// Movimento em area plana a curta distancia
		var loc = geoEngine.moveCheck(83400, 148000, -3400, 83450, 148000, -3400);
		assertNotNull(loc);
		assertEquals(83450, loc.x());
		assertEquals(148000, loc.y());
	}

	@Test
	@DisplayName("Verificacao de linha de visao em Talking Island Keltir spawn")
	void testTalkingIslandLoS() {
		int mobX = -74684;
		int mobY = 252904;
		int mobZ = -3451;

		short geoMobZ = geoEngine.getHeight(mobX, mobY, mobZ);
		System.out.println("DEBUG: Spawn mob Z: " + mobZ + ", Geo mob Z: " + geoMobZ);

		int[] distances = {30, 50, 100, 150, 160, 200, 250, 300, 400, 500, 600};
		double[] angles = {0, Math.PI / 4, Math.PI / 2, Math.PI, 3 * Math.PI / 2};

		// 1. Quando desativado, LoS deve SEMPRE retornar true
		com.lopez.l2j.config.Config.ENABLE_GEODATA = false;
		assertFalse(geoEngine.isEnabled(), "Deve estar desativado");
		for (int d : distances) {
			for (double a : angles) {
				int px = (int) (mobX + d * Math.cos(a));
				int py = (int) (mobY + d * Math.sin(a));
				assertTrue(geoEngine.canSeeTarget(px, py, -3336, mobX, mobY, mobZ),
						"Com geodata desativado, canSeeTarget deve sempre ser true");
			}
		}

		// 2. Quando ativado, com calibracao de Z no alvo, LoS em terreno aberto deve ser visivel
		com.lopez.l2j.config.Config.ENABLE_GEODATA = true;
		assertTrue(geoEngine.isEnabled(), "Deve estar ativado");
		for (int d : distances) {
			for (double a : angles) {
				int px = (int) (mobX + d * Math.cos(a));
				int py = (int) (mobY + d * Math.sin(a));
				short pz = geoEngine.getHeight(px, py, mobZ);
				assertTrue(geoEngine.canSeeTarget(px, py, pz, mobX, mobY, mobZ),
						"Em terreno aberto plano, canSeeTarget deve ser true para d=" + d);
			}
		}
	}
}
