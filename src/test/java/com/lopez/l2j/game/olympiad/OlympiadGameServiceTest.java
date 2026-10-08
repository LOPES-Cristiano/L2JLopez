package com.lopez.l2j.game.olympiad;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OlympiadGameServiceTest {

	private OlympiadManager olympiadManager;
	private OlympiadGameService gameService;

	@BeforeEach
	void setUp() {
		olympiadManager = new OlympiadManager(null);
		gameService = new OlympiadGameService(olympiadManager);
	}

	private GameSession createSession(int objectId, String name, int classId, int level, int karma, String ip) {
		PlayerCharacter player = new PlayerCharacter(objectId, "acc_" + objectId, name, level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 1000.0, 500.0, 500.0);
		player.inventory(new Inventory(objectId));
		player.classId(classId);
		player.karma(karma);

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
	@DisplayName("Validação estrita de registro nas Olimpíadas (Nobreza, Nível, Karma, Anti-Feed)")
	void testRegistrationValidations() {
		GameSession nonNoble = createSession(101, "NormalHero", 88, 76, 0, "192.168.1.10");
		assertThat(gameService.register(nonNoble, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.NOT_NOBLE);

		// Registra nobreza no OlympiadManager
		olympiadManager.registerNoble(101, "NormalHero", 88);
		assertThat(gameService.register(nonNoble, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.SUCCESS);

		// Dupla inscricao rejeitada
		assertThat(gameService.register(nonNoble, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.ALREADY_REGISTERED);

		// Nivel insuficiente (< 75)
		GameSession lowLevel = createSession(102, "LowLvlHero", 88, 70, 0, "192.168.1.11");
		olympiadManager.registerNoble(102, "LowLvlHero", 88);
		assertThat(gameService.register(lowLevel, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.LEVEL_TOO_LOW);

		// Karma > 0
		GameSession pkHero = createSession(103, "PkHero", 88, 78, 500, "192.168.1.12");
		olympiadManager.registerNoble(103, "PkHero", 88);
		assertThat(gameService.register(pkHero, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.HAS_KARMA);
	}

	@Test
	@DisplayName("Bloqueio Anti-Feed por mesmo IP address em participantes concorrentes")
	void testAntiFeedSameIpBlocking() {
		String publicIp = "200.150.10.5";

		GameSession s1 = createSession(201, "PlayerAlpha", 88, 78, 0, publicIp);
		GameSession s2 = createSession(202, "PlayerBetaFeed", 88, 78, 0, publicIp);

		olympiadManager.registerNoble(201, "PlayerAlpha", 88);
		olympiadManager.registerNoble(202, "PlayerBetaFeed", 88);

		assertThat(gameService.register(s1, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.SUCCESS);

		// Oponente com o mesmo IP tenta registrar na mesma fila -> BLOQUEADO
		assertThat(gameService.register(s2, OlympiadMode.CLASS_FREE))
				.isEqualTo(OlympiadGameService.RegisterResult.SAME_IP_BLOCKED);
	}

	@Test
	@DisplayName("Ciclo completo de partida Class-Free 1v1 com preparo, combate e transferência de pontos")
	void testClassFreeMatchLifecycle() {
		List<GameSession> sessions = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			int id = 300 + i;
			var s = createSession(id, "CF_Fighter_" + i, 88 + (i % 2), 78, 0, "10.0.0." + i);
			olympiadManager.registerNoble(id, "CF_Fighter_" + i, 88 + (i % 2));
			// Adiciona um buff externo de teste para checar remocao
			s.activeChar().effects().addBuff(1204, 2, 120_000);
			sessions.add(s);
			gameService.register(s, OlympiadMode.CLASS_FREE);
		}

		assertThat(gameService.getQueueCount(OlympiadMode.CLASS_FREE)).isEqualTo(4);

		// Dispara matchmaking
		OlympiadMatch match = gameService.createMatchIfReady(OlympiadMode.CLASS_FREE, 1);
		assertThat(match).isNotNull();
		assertThat(match.teamA()).hasSize(1);
		assertThat(match.teamB()).hasSize(1);

		// Valida preparo dos combatentes (buffs externos removidos e full heal)
		PlayerCharacter p1 = match.teamA().get(0).session().activeChar();
		PlayerCharacter p2 = match.teamB().get(0).session().activeChar();
		assertThat(p1.effects().active()).isEmpty();
		assertThat(p1.currentHp()).isEqualTo(p1.maxHp());
		assertThat(p1.currentCp()).isEqualTo(p1.maxCp());

		// Simula dano em combate: Team A causa 5.000 de dano, Team B causa 1.500
		match.teamA().get(0).addDamage(5000);
		match.teamB().get(0).addDamage(1500);

		// Conclusao da partida
		var outcome = gameService.concludeMatch(match.matchId());
		assertThat(outcome).isEqualTo(OlympiadMatch.MatchResult.TEAM_A_WIN);

		// Valida transferencia de pontos no OlympiadManager
		var nobleA = olympiadManager.getNoble(p1.objectId()).orElseThrow();
		var nobleB = olympiadManager.getNoble(p2.objectId()).orElseThrow();
		assertThat(nobleA.points()).isGreaterThan(OlympiadNoble.DEFAULT_POINTS);
		assertThat(nobleB.points()).isLessThan(OlympiadNoble.DEFAULT_POINTS);
	}

	@Test
	@DisplayName("Ciclo de partida Class-Based pareando combatentes da mesma classe")
	void testClassBasedMatchLifecycle() {
		// Registra 2 Duelistas (88) e 2 Archmages (94)
		GameSession d1 = createSession(401, "Duelist_1", 88, 78, 0, "192.168.2.1");
		GameSession d2 = createSession(402, "Duelist_2", 88, 78, 0, "192.168.2.2");
		GameSession m1 = createSession(403, "Archmage_1", 94, 78, 0, "192.168.2.3");
		GameSession m2 = createSession(404, "Archmage_2", 94, 78, 0, "192.168.2.4");

		olympiadManager.registerNoble(401, "Duelist_1", 88);
		olympiadManager.registerNoble(402, "Duelist_2", 88);
		olympiadManager.registerNoble(403, "Archmage_1", 94);
		olympiadManager.registerNoble(404, "Archmage_2", 94);

		gameService.register(d1, OlympiadMode.CLASS_BASED);
		gameService.register(d2, OlympiadMode.CLASS_BASED);
		gameService.register(m1, OlympiadMode.CLASS_BASED);
		gameService.register(m2, OlympiadMode.CLASS_BASED);

		OlympiadMatch match = gameService.createMatchIfReady(OlympiadMode.CLASS_BASED, 2);
		assertThat(match).isNotNull();
		// Ambos os combatentes devem pertencer a mesma classe!
		assertThat(match.teamA().get(0).classId()).isEqualTo(match.teamB().get(0).classId());
	}

	@Test
	@DisplayName("Ciclo de partida Team-Based 3v3 com 6 participantes")
	void testTeamBasedMatchLifecycle() {
		for (int i = 1; i <= 6; i++) {
			int id = 500 + i;
			var s = createSession(id, "TeamHero_" + i, 88, 78, 0, "172.16.0." + i);
			olympiadManager.registerNoble(id, "TeamHero_" + i, 88);
			gameService.register(s, OlympiadMode.TEAM_BASED);
		}

		assertThat(gameService.getQueueCount(OlympiadMode.TEAM_BASED)).isEqualTo(6);

		OlympiadMatch match = gameService.createMatchIfReady(OlympiadMode.TEAM_BASED, 3);
		assertThat(match).isNotNull();
		assertThat(match.teamA()).hasSize(3);
		assertThat(match.teamB()).hasSize(3);

		// Abate todos de Team B
		match.teamB().forEach(p -> p.setDead(true));
		var outcome = gameService.concludeMatch(match.matchId());
		assertThat(outcome).isEqualTo(OlympiadMatch.MatchResult.TEAM_A_WIN);
	}

	@Test
	@DisplayName("Validação das restrições de configuração da Olimpíada (OlympiadEnabled e AltOlyEnchantLimit)")
	void testOlympiadConfigurableRestrictions() {
		boolean origEnabled = Config.OLYMPIAD_ENABLED;
		int origLimit = Config.ALT_OLY_ENCHANT_LIMIT;

		try {
			var session = createSession(901, "RestrictedHero", 88, 78, 0, "192.168.1.99");
			olympiadManager.registerNoble(901, "RestrictedHero", 88);

			// 1. OlympiadEnabled = false bloqueia registro
			Config.OLYMPIAD_ENABLED = false;
			assertThat(gameService.register(session, OlympiadMode.CLASS_FREE))
					.isEqualTo(OlympiadGameService.RegisterResult.DISABLED);

			Config.OLYMPIAD_ENABLED = true;

			// 2. AltOlyEnchantLimit = 6
			Config.ALT_OLY_ENCHANT_LIMIT = 6;
			ItemTemplate swordTpl = ItemTemplate.weapon(1, 1, "Excalibur", "rhand", "sword", 1000, "s", 300, 150, 379, 10, 0, 1000000, true, true, true, true);
			ItemInstance overEnchanted = new ItemInstance(9901, swordTpl, 901, 1);
			overEnchanted.enchant(10); // +10 > +6
			overEnchanted.location(ItemInstance.Location.PAPERDOLL, 0); // equipped
			session.activeChar().inventory().add(overEnchanted);

			assertThat(gameService.register(session, OlympiadMode.CLASS_FREE))
					.isEqualTo(OlympiadGameService.RegisterResult.ENCHANT_LIMIT_EXCEEDED);

			// Reduz o enchant para +6 (dentro do limite)
			overEnchanted.enchant(6);
			assertThat(gameService.register(session, OlympiadMode.CLASS_FREE))
					.isEqualTo(OlympiadGameService.RegisterResult.SUCCESS);

		} finally {
			Config.OLYMPIAD_ENABLED = origEnabled;
			Config.ALT_OLY_ENCHANT_LIMIT = origLimit;
		}
	}
}
