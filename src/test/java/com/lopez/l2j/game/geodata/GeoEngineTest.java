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
}
