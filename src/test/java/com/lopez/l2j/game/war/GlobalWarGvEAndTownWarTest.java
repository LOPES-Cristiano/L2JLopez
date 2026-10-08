package com.lopez.l2j.game.war;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários para a Onda C10:
 * Modo de Facções GvE (Good vs Evil) e Guerras Urbanas Town Wars.
 *
 * <p>Critérios de Aceite oficiais:</p>
 * <ul>
 *   <li>Entrada em zona/facção GvE alterando o relacionamento de alvo (aliado/inimigo).</li>
 *   <li>Início de Town War convertendo a capital pacífica em zona de combate PvP sem karma.</li>
 *   <li>Pontuação por abates e conquista de relicários no GvE.</li>
 *   <li>Recompensas orgânicas estritas em Adena (zero donations).</li>
 * </ul>
 */
class GlobalWarGvEAndTownWarTest {

	private GvEService gveService;
	private TownWarService townWarService;

	@BeforeEach
	void setUp() {
		gveService = new GvEService();
		townWarService = new TownWarService();
	}

	private PlayerCharacter createPlayer(int objId, String name, int x, int y, int z) {
		PlayerCharacter p = new PlayerCharacter(
				objId, "acc_" + objId, name, 78, 100000000L, 5000000, 0,
				88, 88, false, 0, 0, 0, 5000,
				3000, 4000, 0, 10, 0, 0, "", 0,
				System.currentTimeMillis(), 0, x, y, z, 0, 5000.0,
				3000.0, 4000.0
		);
		p.inventory(new Inventory(objId));
		return p;
	}

	private ItemInstance createAdena(int objId, int count) {
		ItemTemplate tmpl = ItemTemplate.etc(57, 57, "Adena", "other", "asset", 0, "none", 1, true, true, true, true);
		return new ItemInstance(objId, tmpl, 0, count);
	}

	@Test
	@DisplayName("Criterio Onda C10: Faccoes GvE alteram cores de nome e definem relacao aliado/inimigo")
	void testGvEFactionsTargetRelationship() {
		PlayerCharacter goodPlayer1 = createPlayer(1, "SirArthur", 0, 0, 0);
		PlayerCharacter goodPlayer2 = createPlayer(2, "LadyGwen", 0, 0, 0);
		PlayerCharacter evilPlayer = createPlayer(3, "LordMalakar", 0, 0, 0);

		// 1. Ingressam nas facções
		gveService.joinFaction(goodPlayer1, FactionType.GOOD);
		gveService.joinFaction(goodPlayer2, FactionType.GOOD);
		gveService.joinFaction(evilPlayer, FactionType.EVIL);

		// Valida cores de nome exclusivas
		assertEquals(FactionType.GOOD.nameColor(), goodPlayer1.nameColor(), "Good deve ter nome Azul (0x3399FF)");
		assertEquals(FactionType.EVIL.nameColor(), evilPlayer.nameColor(), "Evil deve ter nome Vermelho (0xFF3333)");

		// 2. Relacionamento de combate
		// Good vs Evil -> Inimigos diretos
		assertTrue(gveService.isEnemy(goodPlayer1, evilPlayer), "Membros de faccoes opostas DEVEM ser inimigos diretos");
		assertTrue(gveService.isEnemy(evilPlayer, goodPlayer1), "Relacao de inimigo deve ser mutua");
		assertFalse(gveService.isAlly(goodPlayer1, evilPlayer));

		// Good vs Good -> Aliados
		assertTrue(gveService.isAlly(goodPlayer1, goodPlayer2), "Membros da mesma faccao sao aliados");
		assertFalse(gveService.isEnemy(goodPlayer1, goodPlayer2), "Membros da mesma faccao NAO podem ser considerados inimigos");

		// 3. Saida da faccao restaura estado padrao
		gveService.leaveFaction(goodPlayer1);
		assertEquals(FactionType.NONE, goodPlayer1.faction());
		assertEquals(0xFFFFFF, goodPlayer1.nameColor(), "Ao sair da faccao, restaura nome branco padrao");
		assertFalse(gveService.isEnemy(goodPlayer1, evilPlayer), "Jogador neutro nao e considerado alvo automatico de faccao");
	}

	@Test
	@DisplayName("Criterio Onda C10: Abates no GvE computam pontuacao e premiam vencedor em Adena")
	void testGvEScoringAndKillReward() {
		PlayerCharacter killer = createPlayer(10, "PaladinLancelot", 0, 0, 0);
		PlayerCharacter victim = createPlayer(11, "NecroMortis", 0, 0, 0);

		ItemInstance adena = createAdena(100, 1000);
		killer.inventory().add(adena);

		gveService.joinFaction(killer, FactionType.GOOD);
		gveService.joinFaction(victim, FactionType.EVIL);

		assertEquals(0, gveService.getScore(FactionType.GOOD));

		// Processa o abate
		boolean killed = gveService.onKill(killer, victim);
		assertTrue(killed);

		// Placar e recompensa
		assertEquals(1, gveService.getScore(FactionType.GOOD), "Placar da faccao Good deve subir para 1");
		assertEquals(0, gveService.getScore(FactionType.EVIL));
		assertEquals(6000, adena.count(), "Killer deve receber 5000 Adena de recompensa (1000 + 5000 = 6000)");
	}

	@Test
	@DisplayName("Criterio Onda C10: Conquista de Relicários Territoriais no GvE confere 50 pontos à facção")
	void testGvERelicCapture() {
		assertEquals(0, gveService.getScore(FactionType.EVIL));
		assertEquals(FactionType.NONE, gveService.getRelicOwner("Dion Fortress Relic"));

		gveService.captureRelic("Dion Fortress Relic", FactionType.EVIL);

		assertEquals(FactionType.EVIL, gveService.getRelicOwner("Dion Fortress Relic"));
		assertEquals(50, gveService.getScore(FactionType.EVIL), "Conquista do relicario deve conceder 50 pontos a faccao Evil");
	}

	@Test
	@DisplayName("Criterio Onda C10: Início de Town War converte capital em War Zone (PvP livre)")
	void testTownWarActivatesCityPvPZone() {
		// Giran Castle Town (ID: 9) Centro: 83400, 148000, -3400
		int giranX = 83400, giranY = 148000, giranZ = -3400;

		// 1. Estado inicial pacifico
		assertFalse(townWarService.isTownWarActive(9));
		assertFalse(townWarService.isInsideWarZone(giranX, giranY, giranZ), "Sem guerra ativa, a cidade e Peace Zone");
		assertFalse(townWarService.isGatekeeperDisabled(9));

		// 2. Inicia Town War por 30 minutos
		boolean started = townWarService.startTownWar(9, 30);
		assertTrue(started);

		assertTrue(townWarService.isTownWarActive(9));
		assertTrue(townWarService.isInsideWarZone(giranX, giranY, giranZ), "Com Town War ativa, a cidade SE TORNA War Zone!");
		assertTrue(townWarService.isGatekeeperDisabled(9), "Gatekeeper deve ser bloqueado durante o evento urbano");

		// Ponto distante fora de Giran permanece fora da zona de guerra
		assertFalse(townWarService.isInsideWarZone(0, 0, 0));

		// 3. Combate dentro da cidade premia o vencedor em Adena
		PlayerCharacter killer = createPlayer(20, "UrbanWarrior", giranX, giranY, giranZ);
		PlayerCharacter victim = createPlayer(21, "UrbanFallen", giranX, giranY, giranZ);
		ItemInstance adena = createAdena(200, 500);
		killer.inventory().add(adena);

		boolean warKill = townWarService.onWarKill(killer, victim);
		assertTrue(warKill);
		assertEquals(5500, adena.count(), "Deve receber 5000 Adena de recompensa pelo abate na guerra urbana");

		// 4. Encerra a Town War -> Restaura paz
		townWarService.stopTownWar(9);
		assertFalse(townWarService.isTownWarActive(9));
		assertFalse(townWarService.isInsideWarZone(giranX, giranY, giranZ), "Apos o fim do evento, a cidade volta a ser Peace Zone");
	}
}
