package com.lopez.l2j.game.offlinefarm;

import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.zone.FlagZoneService;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitarios para a Onda C6:
 * Offline Farm (.offlinefarm / .offlinestop) e Zonas de Flag Automatica (FlagZoneService).
 */
class OfflineFarmAndFlagZoneTest {

	private AutoFarmService autoFarmService;
	private OfflineFarmService offlineFarmService;
	private FlagZoneService flagZoneService;

	@BeforeEach
	void setUp() {
		autoFarmService = new AutoFarmService();
		offlineFarmService = new OfflineFarmService(2, 0L, true, autoFarmService);
		flagZoneService = new FlagZoneService(new ZoneTable());
		flagZoneService.clearCustomZones();
		offlineFarmService.clearAll();
	}

	private PlayerCharacter createPlayer(int objId, String name, int classId, int level) {
		return new PlayerCharacter(
				objId, "acc_" + objId, name, level, 100000000L, 5000000, 0,
				classId, classId, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, 0, 0, 0, 0, 5000.0,
				3000.0, 4000.0
		);
	}

	@Test
	@DisplayName("FlagZone: Entrada na zona atualiza pvpFlag para 1 sem expiracao temporal")
	void testFlagZoneEnterAppliesPvpFlag() {
		// Registra zona nobre (ex: Primeval Isle)
		flagZoneService.registerFlagZone("Primeval Isle Farm", 10000, 20000, 10000, 20000, -5000, 5000);

		PlayerCharacter player = createPlayer(1, "HeroPvp", 88, 78);
		player.setX(15000);
		player.setY(15000);
		player.setZ(0);

		assertTrue(flagZoneService.isInsideFlagZone(player));
		assertEquals(0, player.pvpFlag());

		List<GameServerPacket> sentPackets = new ArrayList<>();
		flagZoneService.onEnter(player, sentPackets::add);

		assertEquals(1, player.pvpFlag(), "Ao entrar na Flag Zone o jogador DEVE ficar com flag roxo");
		assertEquals(Long.MAX_VALUE, player.pvpFlagEndTime(), "Na Flag Zone o flag nao expira por inatividade");
		assertFalse(sentPackets.isEmpty(), "Deve enviar notificacao de entrada na Flag Zone");
	}

	@Test
	@DisplayName("FlagZone: Saida da zona inicia contagem regressiva de 20s para decaimento")
	void testFlagZoneExitStarts20sCountdown() {
		flagZoneService.registerFlagZone("Imperial Tomb Farm", 30000, 40000, 30000, 40000, -5000, 5000);

		PlayerCharacter player = createPlayer(2, "FlagExitPlayer", 88, 78);
		player.setX(35000);
		player.setY(35000);
		flagZoneService.onEnter(player, p -> {});

		assertEquals(1, player.pvpFlag());

		// Sai da zona
		player.setX(50000);
		player.setY(50000);
		assertFalse(flagZoneService.isInsideFlagZone(player));

		long beforeExit = System.currentTimeMillis();
		flagZoneService.onExit(player, p -> {});

		assertTrue(player.pvpFlagEndTime() >= beforeExit + 19_000L,
				"Ao sair da Flag Zone deve iniciar o cooldown padrao de 20s de flag");
	}

	@Test
	@DisplayName("FlagZone: Combate e abates dentro da zona sao isentos de Karma (PvP livre)")
	void testFlagZoneKarmaFree() {
		flagZoneService.registerFlagZone("Goddard Noble Farm", 50000, 60000, 50000, 60000, -5000, 5000);

		PlayerCharacter attacker = createPlayer(3, "Attacker", 88, 78);
		PlayerCharacter defender = createPlayer(4, "Defender", 95, 78);

		// Ambos fora
		attacker.setX(0);
		attacker.setY(0);
		defender.setX(0);
		defender.setY(0);
		assertFalse(flagZoneService.isKarmaFree(attacker, defender), "Fora da zona, PK comum gera karma");

		// Alvo dentro da Flag Zone
		defender.setX(55000);
		defender.setY(55000);
		assertTrue(flagZoneService.isKarmaFree(attacker, defender), "Dentro da Flag Zone nao pode gerar karma");
	}

	@Test
	@DisplayName("OfflineFarm: Exige que .autofarm esteja previamente ativo")
	void testOfflineFarmRequiresAutoFarmFirst() {
		PlayerCharacter player = createPlayer(5, "FarmerNoAuto", 88, 78);

		var result = offlineFarmService.canStart(player, "192.168.1.10");
		assertFalse(result.success());
		assertTrue(result.message().contains(".autofarm"), "Mensagem deve exigir ativacao previa do .autofarm");
	}

	@Test
	@DisplayName("OfflineFarm: Ativacao com sucesso e ciclo de vida ativo")
	void testOfflineFarmSuccessLifecycle() {
		PlayerCharacter player = createPlayer(6, "ActiveFarmer", 88, 78);

		// 1. Ativa AutoFarm
		boolean autoFarmEnabled = autoFarmService.toggleAutoFarm(player);
		assertTrue(autoFarmEnabled);

		// 2. Inicia Offline Farm
		var startResult = offlineFarmService.startOfflineFarm(player, "192.168.1.50", null, null, null);
		assertTrue(startResult.success(), "Deve iniciar com sucesso quando autofarm esta ativo");
		assertTrue(offlineFarmService.isOfflineFarming(player.objectId()));
		assertEquals(1, offlineFarmService.getActiveCount());
		assertEquals(1, offlineFarmService.getCountByIp("192.168.1.50"));

		// 3. Cancela com .offlinestop
		boolean stopped = offlineFarmService.stopOfflineFarm(player.objectId());
		assertTrue(stopped);
		assertFalse(offlineFarmService.isOfflineFarming(player.objectId()));
		assertEquals(0, offlineFarmService.getActiveCount());
		assertEquals(0, offlineFarmService.getCountByIp("192.168.1.50"));
	}

	@Test
	@DisplayName("OfflineFarm: Limite estrito de conexoes por IP (max 2)")
	void testOfflineFarmIpLimitation() {
		String clientIp = "10.0.0.5";
		PlayerCharacter p1 = createPlayer(10, "Farmer1", 88, 78);
		PlayerCharacter p2 = createPlayer(11, "Farmer2", 88, 78);
		PlayerCharacter p3 = createPlayer(12, "Farmer3", 88, 78);

		autoFarmService.toggleAutoFarm(p1);
		autoFarmService.toggleAutoFarm(p2);
		autoFarmService.toggleAutoFarm(p3);

		// Conta 1: OK
		assertTrue(offlineFarmService.startOfflineFarm(p1, clientIp, null, null, null).success());
		// Conta 2: OK
		assertTrue(offlineFarmService.startOfflineFarm(p2, clientIp, null, null, null).success());

		// Conta 3: Deve ser rejeitada por limite de IP
		var failResult = offlineFarmService.startOfflineFarm(p3, clientIp, null, null, null);
		assertFalse(failResult.success());
		assertTrue(failResult.message().contains("Limite"), "Deve informar limite de IP atingido");
		assertEquals(2, offlineFarmService.getActiveCount());
	}

	@Test
	@DisplayName("OfflineFarm: Personagem morto nao pode iniciar")
	void testDeadPlayerCannotStartOfflineFarm() {
		PlayerCharacter dead = createPlayer(13, "DeadFarmer", 88, 78);
		dead.currentHp(0);
		assertTrue(dead.isDead());

		autoFarmService.toggleAutoFarm(dead);
		var result = offlineFarmService.canStart(dead, "127.0.0.1");
		assertFalse(result.success());
		assertTrue(result.message().contains("morto"));
	}
}
