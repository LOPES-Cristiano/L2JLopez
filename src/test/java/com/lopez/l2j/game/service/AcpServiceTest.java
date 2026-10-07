package com.lopez.l2j.game.service;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcpServiceTest {

	private AcpService acpService;
	private CharacterVariablesService variablesService;
	private PlayerCharacter player;
	private GameSession session;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		variablesService = mock(CharacterVariablesService.class);
		acpService = new AcpService(variablesService);

		player = new PlayerCharacter(
				1001, "acc", "TestHero", 1, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 800, 500, 0, 0, 0, 0,
				"", 0, 0, 0, 0, 0, 0, 0,
				1000.0, 800.0, 500.0);
		inventory = mock(Inventory.class);
		player.inventory(inventory);

		session = mock(GameSession.class);
		when(session.character()).thenReturn(player);
		when(session.activeCharacter()).thenReturn(player);
	}

	@Test
	@DisplayName("Configuracao padrao deve ser desativada com limiares equilibrados")
	void testDefaultConfig() {
		var cfg = acpService.getConfig(player);
		assertFalse(cfg.enabled(), "ACP deve iniciar desativado por padrao");
		assertEquals(70, cfg.hpPercent());
		assertEquals(80, cfg.cpPercent());
		assertEquals(60, cfg.mpPercent());
	}

	@Test
	@DisplayName("Ativar e desativar ACP deve refletir no cache e variaveis")
	void testToggleAcp() {
		acpService.setEnabled(player, true);
		assertTrue(acpService.getConfig(player).enabled());
		verify(variablesService).setVariable(eq(1001), eq("acp_enabled"), eq("true"));

		acpService.setEnabled(player, false);
		assertFalse(acpService.getConfig(player).enabled());
		verify(variablesService).setVariable(eq(1001), eq("acp_enabled"), eq("false"));
	}

	@Test
	@DisplayName("Configurar percentuais com valores invalidos deve fazer clamp de 0 a 100")
	void testClampThresholds() {
		acpService.setThresholds(player, 150, -20, 50);
		var cfg = acpService.getConfig(player);
		assertEquals(100, cfg.hpPercent(), "HP deve ser travado no maximo 100%");
		assertEquals(0, cfg.cpPercent(), "CP deve ser travado no minimo 0%");
		assertEquals(50, cfg.mpPercent());
	}

	@Test
	@DisplayName("Nao consome pocoes quando HP/CP/MP estao cheios")
	void testDoNotConsumeWhenFull() {
		acpService.setEnabled(player, true);
		acpService.checkAndConsume(session);
		verify(session, never()).useConsumable(any());
	}

	@Test
	@DisplayName("Consome Greater CP Potion quando CP cai abaixo do limiar")
	void testConsumeCpPotion() {
		acpService.setEnabled(player, true);
		player.currentCp(200); // 200/500 = 40% <= 80%

		ItemInstance cpPot = mock(ItemInstance.class);
		when(cpPot.count()).thenReturn(10);
		when(inventory.byItemId(AcpService.ITEM_GREATER_CP)).thenReturn(Optional.of(cpPot));

		acpService.checkAndConsume(session);
		verify(session).useConsumable(argThat(c -> c.itemId() == AcpService.ITEM_GREATER_CP));
	}

	@Test
	@DisplayName("Nao consome pocoes se o personagem estiver morto")
	void testDoNotConsumeWhenDead() {
		acpService.setEnabled(player, true);
		player.currentHp(0);

		acpService.checkAndConsume(session);
		verify(session, never()).useConsumable(any());
	}

	@Test
	@DisplayName("Renderiza HTML com tabela de status e botoes de acao")
	void testRenderHtml() {
		String html = acpService.renderHtml(player);
		assertNotNull(html);
		assertTrue(html.contains("SISTEMA AUTO-POTION (.ACP)"));
		assertTrue(html.contains("voiced_acp on") || html.contains("voiced_acp off"));
		assertTrue(html.contains("CP Minimo"));
	}
}
