package com.lopez.l2j.game.instance.frintezza;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrintezzaServiceTest {

	private FrintezzaService frintezzaService;

	private PlayerCharacter dummyPlayer(int id, String name, int level) {
		PlayerCharacter player = new PlayerCharacter(id, "acc", name, level, 0L, 0, 0, 0, 0, false,
				0, 0, 0, 100, 100, 100, 0, 0, 0, 0, "", 0, 0L, 0L, 0, 0, 0, 0, 0.0, 0.0, 0.0);
		player.inventory(new Inventory(id));
		return player;
	}

	private ItemTemplate scrollTemplate() {
		return ItemTemplate.etc(FrintezzaService.FORCE_FIELD_SCROLL, FrintezzaService.FORCE_FIELD_SCROLL,
				"Force Field Scroll", "scroll", "none", 1, "none", 0, false, false, false, false);
	}

	@BeforeEach
	void setUp() {
		frintezzaService = new FrintezzaService(null, null);
	}

	@Test
	void testCanEnterValidation() {
		PlayerCharacter lowLevelLeader = dummyPlayer(1, "LowLvl", 70);
		assertFalse(frintezzaService.canEnter(lowLevelLeader));

		PlayerCharacter leader = dummyPlayer(2, "EpicLeader", 78);
		// Sem o pergaminho ainda
		assertFalse(frintezzaService.canEnter(leader));

		// Adiciona o pergaminho de remocao do campo de forca
		leader.inventory().add(new ItemInstance(10, scrollTemplate(), leader.objectId(), 1));
		assertTrue(frintezzaService.canEnter(leader));
	}

	@Test
	void testBattleLifecycleAndPhaseTransitions() {
		PlayerCharacter leader = dummyPlayer(100, "Commander", 80);
		PlayerCharacter member = dummyPlayer(101, "Fighter", 79);
		leader.inventory().add(new ItemInstance(10, scrollTemplate(), leader.objectId(), 1));

		// Entrada no Tumulo
		assertTrue(frintezzaService.enterTomb(leader, List.of(leader, member)));
		assertEquals(FrintezzaStatus.ENTRY, frintezzaService.getStatus());
		assertEquals(2, frintezzaService.getPlayersInside().size());
		// Pergaminho consumido
		assertTrue(leader.inventory().byItemId(FrintezzaService.FORCE_FIELD_SCROLL).isEmpty());

		// Inicia Fase 1 (Scarlet Fraca)
		frintezzaService.startFight();
		assertEquals(FrintezzaStatus.PHASE_1_WEAK, frintezzaService.getStatus());

		// Transicao para Fase 2 (Media)
		frintezzaService.transitionToPhase2();
		assertEquals(FrintezzaStatus.PHASE_2_MEDIUM, frintezzaService.getStatus());

		// Transicao para Fase 3 (Demonio Supremo)
		frintezzaService.transitionToPhase3();
		assertEquals(FrintezzaStatus.PHASE_3_STRONG, frintezzaService.getStatus());

		// Vitoria contra Scarlet
		frintezzaService.onScarletDefeated();
		assertEquals(FrintezzaStatus.DEAD, frintezzaService.getStatus());
		assertTrue(frintezzaService.getRespawnTime() > System.currentTimeMillis());
	}
}
