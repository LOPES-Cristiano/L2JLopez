package com.lopez.l2j;

import com.lopez.l2j.game.instance.InstanceService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.pve.FusionItemService;
import com.lopez.l2j.game.war.TownWarService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.ZoneTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceAndPveWiringTest {

	private GameWorld world;
	private InstanceService instanceService;
	private FusionItemService fusionService;
	private TownWarService townWarService;
	private ZoneTable zoneTable;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		GameWorld.setInstance(world);
		instanceService = new InstanceService(world);
		fusionService = new FusionItemService();
		townWarService = new TownWarService();
		zoneTable = new ZoneTable("data/xml/zone");
		zoneTable.setTownWarService(townWarService);
		townWarService.setZoneTable(zoneTable);
	}

	@Test
	@DisplayName("InstanceService deve isolar espacialmente jogadores e purgar NPCs na destruicao")
	void testInstanceLifecycleAndIsolation() {
		int inst1 = instanceService.createInstance(101, 30);
		int inst2 = instanceService.createInstance(102, 30);
		assertThat(inst1).isNotEqualTo(inst2);

		PlayerCharacter p1 = new PlayerCharacter(1001, "InstPlayer1", 80, 0, 0, 0);
		PlayerCharacter p2 = new PlayerCharacter(1002, "InstPlayer2", 80, 0, 0, 0);
		world.add(new GameWorld.PhantomOnlinePlayer(p1));
		world.add(new GameWorld.PhantomOnlinePlayer(p2));

		instanceService.enterInstance(p1, inst1, 10000, 10000, -100);
		instanceService.enterInstance(p2, inst2, 10000, 10000, -100);

		assertThat(p1.instanceId()).isEqualTo(inst1);
		assertThat(p2.instanceId()).isEqualTo(inst2);

		// NPCs vinculados a instancia 1
		NpcTemplate tpl = NpcTemplate.fallback(20001, "InstMob", "L2Monster");
		NpcInstance npc1 = new NpcInstance(9001, tpl, 10000, 10000, -100, 0);
		npc1.instanceId(inst1);
		world.addNpc(npc1);

		assertThat(instanceService.getNpcsInInstance(inst1)).hasSize(1);
		assertThat(instanceService.getNpcsInInstance(inst2)).isEmpty();

		// Destruir instancia 1 deve purgar NPCs e ejetar jogadores para o mundo 0
		instanceService.destroyInstance(inst1);
		assertThat(p1.instanceId()).isZero();
		assertThat(instanceService.getNpcsInInstance(inst1)).isEmpty();
	}

	@Test
	@DisplayName("FusionItemService deve carregar receitas e executar fusao com heranca de encantamento")
	void testFusionServiceExecution() {
		PlayerCharacter player = new PlayerCharacter(2001, "FusionTester");
		Inventory inv = player.inventory();

		// Verifica se carregou receitas de FusionItems.xml
		assertThat(fusionService.getRecipes()).isNotEmpty();
		var recipe = fusionService.getRecipes().get(0); // Weapons: 6367 (1) + 6393 (30) -> 6590 (1)

		// Sem ingredientes, deve falhar
		var failResult = fusionService.fuseByRecipe(player, recipe);
		assertThat(failResult.success()).isFalse();

		// Adiciona ingredientes necessarios com item base encantado +12
		ItemTemplate baseTpl = ItemTemplate.weapon(6367, 6367, "Infinity Blade", "rhand", "sword", 1300, "s", 280, 150, 379, 8, 0, 0, 0, 100000, true, true, true, true);
		ItemInstance baseItem = new ItemInstance(5001, baseTpl, player.objectId(), 1);
		baseItem.enchant(12);
		inv.add(baseItem);

		ItemTemplate matTpl = ItemTemplate.etc(6393, 6393, "Glittering Medal", "other", "none", 1, "none", 0, true, true, true, true);
		inv.add(new ItemInstance(5002, matTpl, player.objectId(), 30));

		var successResult = fusionService.fuseByRecipe(player, recipe);
		if (successResult.success()) {
			assertThat(successResult.resultingEnchant()).isEqualTo(12);
			assertThat(inv.count(recipe.resultItemId())).isGreaterThanOrEqualTo(1);
		} else {
			// No caso de falha na rolagem de chance (60%), a regra devolve o failItem
			assertThat(inv.count(recipe.failItemId())).isGreaterThanOrEqualTo(1);
		}
	}

	@Test
	@DisplayName("TownWarService deve desativar a Zona de Paz na cidade sob guerra")
	void testTownWarPeaceZoneDeactivation() {
		// Ponto central de Giran (x=83400, y=148000, z=-3400) em condicoes normais e Zona de Paz
		assertThat(zoneTable.isInsidePeace(83400, 148000, -3400)).isTrue();

		// Iniciar Town War em Giran (townId 9)
		townWarService.startTownWar(9, 60);
		assertThat(townWarService.isTownWarActive(9)).isTrue();
		assertThat(townWarService.isTownWarActiveAt(83400, 148000, -3400)).isTrue();

		// Agora a mesma coordenada DEVE retornar false para Zona de Paz, permitindo PvP livre!
		assertThat(zoneTable.isInsidePeace(83400, 148000, -3400)).isFalse();

		// Encerrar a guerra restaura o status de paz
		townWarService.stopTownWar(9);
		assertThat(zoneTable.isInsidePeace(83400, 148000, -3400)).isTrue();
	}
}
