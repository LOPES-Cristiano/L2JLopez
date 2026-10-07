package com.lopez.l2j.game.event.pvp;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PvPEventServiceTest {

	private PvPEventService pvpService;

	@BeforeEach
	void setUp() {
		pvpService = new PvPEventService(null);
	}

	private GameSession createSession(int objectId, String name, int level, int karma, String ip,
			int startX, int startY, int startZ) {
		PlayerCharacter player = new PlayerCharacter(objectId, "acc_" + objectId, name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				2000, 1000, 1000, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 2000.0, 1000.0, 1000.0);
		player.inventory(new Inventory(objectId));
		player.karma(karma);
		player.x(startX);
		player.y(startY);
		player.z(startZ);
		player.instanceId(0);

		GameSession session = new GameSession(null, new byte[16], ip, p -> {});
		setField(session, "state", GameClientPacket.State.IN_GAME);
		setField(session, "active", player);
		setField(session, "inWorld", true);
		return session;
	}

	private static void setField(Object obj, String fieldName, Object val) {
		try {
			var f = obj.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(obj, val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("Validação de registro e anti-feed de IP no PvPEventService")
	void testRegistrationAndAntiFeed() {
		pvpService.openRegistration(PvPEventService.EventType.TVT);

		// Nivel < 70 rejeitado
		GameSession lowLvl = createSession(1, "Lowbie", 68, 0, "192.168.1.1", 1000, 1000, -100);
		assertThat(pvpService.register(lowLvl)).isFalse();

		// Karma > 0 rejeitado
		GameSession pk = createSession(2, "PkPlayer", 75, 500, "192.168.1.2", 1000, 1000, -100);
		assertThat(pvpService.register(pk)).isFalse();

		// Valido
		GameSession p1 = createSession(10, "Alpha", 78, 0, "200.1.2.3", 1000, 1000, -100);
		assertThat(pvpService.register(p1)).isTrue();
		assertThat(pvpService.getParticipantCount()).isEqualTo(1);

		// Duplicado rejeitado
		assertThat(pvpService.register(p1)).isFalse();

		// Mesmo IP bloqueado (anti-feed)
		GameSession feed = createSession(11, "BetaFeed", 78, 0, "200.1.2.3", 2000, 2000, -100);
		assertThat(pvpService.register(feed)).isFalse();
	}

	@Test
	@DisplayName("Rotacao automatica de arenas entre eventos sucessivos")
	void testArenaRotation() {
		pvpService.openRegistration(PvPEventService.EventType.TVT);
		assertThat(pvpService.currentArena()).isEqualTo(PvPEventService.Arena.COLISEUM);
		pvpService.cancelEvent();

		pvpService.openRegistration(PvPEventService.EventType.CTF);
		assertThat(pvpService.currentArena()).isEqualTo(PvPEventService.Arena.GLUDIO);
		pvpService.cancelEvent();

		pvpService.openRegistration(PvPEventService.EventType.DEATHMATCH);
		assertThat(pvpService.currentArena()).isEqualTo(PvPEventService.Arena.GIRAN);
		pvpService.cancelEvent();

		pvpService.openRegistration(PvPEventService.EventType.TVT);
		assertThat(pvpService.currentArena()).isEqualTo(PvPEventService.Arena.FANTASY_ISLE);
		pvpService.cancelEvent();

		// Retorna ao comeco do ciclo
		pvpService.openRegistration(PvPEventService.EventType.TVT);
		assertThat(pvpService.currentArena()).isEqualTo(PvPEventService.Arena.COLISEUM);
	}

	@Test
	@DisplayName("Ciclo completo do TvT com isolamento por instância, placar e recompensa em Adena")
	void testTvtEventFlow() {
		pvpService.openRegistration(PvPEventService.EventType.TVT);

		GameSession s1 = createSession(101, "BlueFighter", 78, 0, "10.0.0.1", 82000, 148000, -3460);
		GameSession s2 = createSession(102, "RedFighter", 78, 0, "10.0.0.2", 83000, 149000, -3460);

		// Adiciona buff de teste que deve ser limpo no inicio do evento
		s1.activeChar().effects().addBuff(1204, 2, 60_000);

		pvpService.register(s1);
		pvpService.register(s2);

		// Inicia o evento
		boolean started = pvpService.startEvent();
		assertThat(started).isTrue();
		assertThat(pvpService.state()).isEqualTo(PvPEventService.EventState.RUNNING);

		int instId = pvpService.activeInstanceId();
		assertThat(instId).isGreaterThanOrEqualTo(5000);

		// Jogadores colocados na instancia isolada e com buffs limpos
		assertThat(s1.activeChar().instanceId()).isEqualTo(instId);
		assertThat(s2.activeChar().instanceId()).isEqualTo(instId);
		assertThat(s1.activeChar().effects().active()).isEmpty();

		// Abates registrados: Blue mata Red duas vezes
		pvpService.onKill(101, 102);
		pvpService.onKill(101, 102);
		assertThat(pvpService.blueScore()).isEqualTo(2);
		assertThat(pvpService.redScore()).isEqualTo(0);

		// Finalizacao do evento
		PvPEventService.Team winner = pvpService.finishEvent();
		assertThat(winner).isEqualTo(PvPEventService.Team.BLUE);

		// Vencedor recebe Adena retail (57) no inventario (sem moeda de doacao 9300!)
		assertThat(s1.activeChar().inventory().adena()).isEqualTo(5_000_000);

		// Jogadores restaurados a instancia 0 e coordenadas originais de retorno
		assertThat(s1.activeChar().instanceId()).isEqualTo(0);
		assertThat(s1.activeChar().x()).isEqualTo(82000);
		assertThat(s1.activeChar().y()).isEqualTo(148000);
	}

	@Test
	@DisplayName("Ciclo completo do CTF com captura de bandeira")
	void testCtfEventFlow() {
		pvpService.openRegistration(PvPEventService.EventType.CTF);

		GameSession s1 = createSession(201, "BlueRunner", 78, 0, "10.1.0.1", 1000, 2000, 0);
		GameSession s2 = createSession(202, "RedDefender", 78, 0, "10.1.0.2", 3000, 4000, 0);

		pvpService.register(s1);
		pvpService.register(s2);
		pvpService.startEvent();

		// Blue captura bandeira -> +5 pontos
		pvpService.onFlagCapture(201);
		assertThat(pvpService.blueScore()).isEqualTo(5);
		assertThat(pvpService.redScore()).isEqualTo(0);

		PvPEventService.Team winner = pvpService.finishEvent();
		assertThat(winner).isEqualTo(PvPEventService.Team.BLUE);
	}

	@Test
	@DisplayName("Ciclo do Deathmatch com premiação do maior abatedor individual")
	void testDeathmatchEventFlow() {
		pvpService.openRegistration(PvPEventService.EventType.DEATHMATCH);

		GameSession s1 = createSession(301, "Slayer1", 80, 0, "10.2.0.1", 500, 500, 0);
		GameSession s2 = createSession(302, "Slayer2", 80, 0, "10.2.0.2", 600, 600, 0);

		pvpService.register(s1);
		pvpService.register(s2);
		pvpService.startEvent();

		// Slayer 1 consegue 3 abates sobre Slayer 2
		pvpService.onKill(301, 302);
		pvpService.onKill(301, 302);
		pvpService.onKill(301, 302);

		pvpService.finishEvent();

		// Slayer 1 deve ser recompensado com Adena
		assertThat(s1.activeChar().inventory().adena()).isEqualTo(5_000_000);
	}
}
