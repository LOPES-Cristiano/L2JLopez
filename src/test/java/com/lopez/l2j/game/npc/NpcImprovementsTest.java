package com.lopez.l2j.game.npc;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.ai.NpcAiService;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.drop.DropData;
import com.lopez.l2j.game.drop.DropReward;
import com.lopez.l2j.game.drop.DropService;
import com.lopez.l2j.game.drop.DropTable;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NpcImprovementsTest {

	private GameWorld world;
	private CombatService combatService;
	private CharTemplateTable charTemplates;
	private NpcAiService npcAiService;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		combatService = new CombatService();
		charTemplates = new CharTemplateTable();
		npcAiService = new NpcAiService(world, combatService, charTemplates);
	}

	@Test
	void shouldTriggerFactionCallWhenAttacked() {
		// Mob 1: Lider/Aliado atacado (faccao "orcs", alcance 500)
		NpcTemplate tplOrc = new NpcTemplate(
				20010, 20010, "Orc Fighter", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				500, 100, 50, 40, 30, 10, 10, 250, 333,
				0, 0, 0, 50, 100, 0, false, "orcs", 500);

		// Mob 2: Outra faccao ("goblins")
		NpcTemplate tplGoblin = new NpcTemplate(
				20020, 20020, "Goblin", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				500, 100, 50, 40, 30, 10, 10, 250, 333,
				0, 0, 0, 50, 100, 0, false, "goblins", 500);

		NpcInstance mob1 = new NpcInstance(1001, tplOrc, 100, 100, 0, 0);
		NpcInstance mob2NearOrc = new NpcInstance(1002, tplOrc, 200, 100, 0, 0); // mesma faccao, raio 100 <= 500
		NpcInstance mob3NearGoblin = new NpcInstance(1003, tplGoblin, 200, 100, 0, 0); // faccao diferente
		NpcInstance mob4FarOrc = new NpcInstance(1004, tplOrc, 1000, 1000, 0, 0); // mesma faccao, raio > 500

		world.addNpc(mob1);
		world.addNpc(mob2NearOrc);
		world.addNpc(mob3NearGoblin);
		world.addNpc(mob4FarOrc);

		// Jogador ataca o mob1
		int targetPlayerId = 555;
		npcAiService.startCombat(mob1, targetPlayerId);

		// 1. O mob atacado entra em combate
		assertThat(mob1.inCombat()).isTrue();
		assertThat(mob1.targetPlayerId()).isEqualTo(targetPlayerId);

		// 2. O mob aliado da mesma faccao no raio entra em combate em socorro
		assertThat(mob2NearOrc.inCombat()).isTrue();
		assertThat(mob2NearOrc.targetPlayerId()).isEqualTo(targetPlayerId);

		// 3. O mob de outra faccao NAO entra em combate
		assertThat(mob3NearGoblin.inCombat()).isFalse();

		// 4. O mob da mesma faccao fora do raio NAO entra em combate
		assertThat(mob4FarOrc.inCombat()).isFalse();
	}

	@Test
	void shouldRespectAggroLevelDifferenceForNormalMobsAndBosses() {
		NpcTemplate tplNormalMob = new NpcTemplate(
				20030, 20030, "Wolf", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				40, 500, 100, 50, 40, 30, 10, 250, 333,
				0, 0, 0, 50, 100, 300, false, "", 0);

		NpcTemplate tplRaidBoss = new NpcTemplate(
				25001, 25001, "Queen Shye", false, "", false,
				20.0, 40.0, 20, "female", "L2RaidBoss",
				40, 10000, 5000, 500, 400, 300, 100, 250, 333,
				0, 0, 0, 50, 100, 500, false, "", 0);

		NpcInstance normalMob = new NpcInstance(2001, tplNormalMob, 100, 100, 0, 0);
		NpcInstance raidBoss = new NpcInstance(2002, tplRaidBoss, 100, 100, 0, 0);

		world.addNpc(normalMob);
		world.addNpc(raidBoss);

		// Jogador High Level (Lvl 80 vs Mob Lvl 20 -> diferenca = 60 >= 9)
		PlayerCharacter highLvlPlayer = new PlayerCharacter(3001, "acc", "HighLvlHero", 80, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 1000.0, 500.0, 500.0);

		// Simula sessao no GameWorld
		TestGameSession highSession = new TestGameSession(highLvlPlayer);
		world.add(highSession);

		// Check de aggro por proximidade (tickCount % 2 == 0)
		npcAiService.tick();
		npcAiService.tick();

		// Monstro normal ignora jogador de nivel muito alto (Retail Lineage 2)
		assertThat(normalMob.inCombat()).isFalse();

		// Raid Boss NAO ignora: ataca mesmo jogadores de nivel alto
		assertThat(raidBoss.inCombat()).isTrue();
		assertThat(raidBoss.targetPlayerId()).isEqualTo(highLvlPlayer.objectId());

		// Agora jogador Low Level (Lvl 20 vs Mob Lvl 20 -> diferenca = 0 < 9)
		PlayerCharacter lowLvlPlayer = new PlayerCharacter(3002, "acc", "Novice", 20, 0, 0, 0, 0, 0, false, 0, 0, 0,
				200, 100, 100, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 200.0, 100.0, 100.0);

		world.remove(highSession);
		world.removeNpc(raidBoss);
		TestGameSession lowSession = new TestGameSession(lowLvlPlayer);
		world.add(lowSession);

		npcAiService.tick();
		npcAiService.tick();

		// Monstro normal agora agra o jogador de nivel correspondente
		assertThat(normalMob.inCombat()).isTrue();
		assertThat(normalMob.targetPlayerId()).isEqualTo(lowLvlPlayer.objectId());
	}

	@Test
	void shouldRollAndSweepSpoilDrops() {
		// Mock drop table com drop comum e spoil
		int mobId = 20100;
		int spoilItemId = com.lopez.l2j.game.item.TestItems.DAGGER;
		DropData normalDrop = new DropData(mobId, 57, 100, 200, 0, 1_000_000); // 100% Adena
		DropData spoilDrop = new DropData(mobId, spoilItemId, 1, 1, -1, 1_000_000); // 100% Spoil

		DropTable dropTable = new DropTable() {
			@Override
			public List<DropData> getDrops(int id) {
				if (id == mobId) {
					return List.of(normalDrop, spoilDrop);
				}
				return List.of();
			}

			@Override
			public int size() {
				return 1;
			}
		};

		DropService dropService = new DropService(dropTable, 1.0, 1.0, 1.0, true);

		// 1. rollDrops normal ignora o spoil
		var normalRewards = dropService.rollDrops(mobId, 20, 20);
		assertThat(normalRewards).hasSize(1);
		assertThat(normalRewards.get(0).itemId()).isEqualTo(57);

		// 2. rollSpoil retorna somente o spoil
		var spoilRewards = dropService.rollSpoil(mobId, 20, 20);
		assertThat(spoilRewards).hasSize(1);
		assertThat(spoilRewards.get(0).itemId()).isEqualTo(spoilItemId);

		// 3. Execucao de Sweeper no monstro morto e spoiled
		NpcTemplate tpl = new NpcTemplate(
				mobId, mobId, "SpoilTarget", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				40, 500, 100, 50, 40, 30, 10, 250, 333,
				0, 0, 0, 50, 100, 0, false, "", 0);

		NpcInstance mob = new NpcInstance(4001, tpl, 0, 0, 0, 0);
		mob.dead(true);
		mob.spoiled(true);
		mob.spoilRewards(spoilRewards);

		PlayerCharacter scavenger = new PlayerCharacter(5001, "acc", "DwarfScavenger", 25, 0, 0, 0, 53, 53, false, 0, 0, 0,
				500, 200, 200, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 500.0, 200.0, 200.0);
		scavenger.inventory(new Inventory(scavenger.objectId()));

		var itemRepo = new com.lopez.l2j.game.item.InMemoryItemRepository();
		InventoryService inventoryService = new InventoryService(
				com.lopez.l2j.game.item.TestItems.table(),
				itemRepo,
				ObjectIdFactory.sequential(100000),
				0);

		boolean sweepSuccess = dropService.sweep(scavenger, mob, inventoryService, null);
		assertThat(sweepSuccess).isTrue();

		// Verifica que o item do spoil entrou no inventario do anao
		var sweptItem = scavenger.inventory().byItemId(spoilItemId);
		assertThat(sweptItem).isPresent();
		assertThat(sweptItem.get().count()).isGreaterThanOrEqualTo(1);

		// Verifica que o spoil do monstro foi consumido
		assertThat(mob.isSpoiled()).isFalse();
		assertThat(mob.spoilRewards()).isNull();

		// Tentar sweep novamente nao colhe mais nada
		boolean sweepAgain = dropService.sweep(scavenger, mob, inventoryService, null);
		assertThat(sweepAgain).isFalse();
	}

	@Test
	void shouldLoadAndRetrieveNpcSkillsFromXml() {
		NpcSkillTable table = new NpcSkillTable();
		table.load();

		// Verifica que carregou a base do datapack oficial
		assertThat(table.size()).isGreaterThan(100);

		// Testa um mob classico conhecido (ex: Ant 20079 ou orc)
		var skills = table.getSkills(20079);
		// Deve ter habilidades registradas
		assertThat(skills).isNotEmpty();
	}

	/**
	 * Stub simples de GameSession para simular jogador no GameWorld.
	 */
	private static class TestGameSession implements com.lopez.l2j.game.world.GameWorld.OnlinePlayer {
		private final PlayerCharacter character;

		TestGameSession(PlayerCharacter character) {
			this.character = character;
		}

		@Override
		public int objectId() {
			return character.objectId();
		}

		@Override
		public String name() {
			return character.name();
		}

		@Override
		public int x() {
			return character.x();
		}

		@Override
		public int y() {
			return character.y();
		}

		@Override
		public int z() {
			return character.z();
		}

		@Override
		public PlayerCharacter character() {
			return character;
		}

		@Override
		public void send(com.lopez.l2j.network.game.packet.GameServerPacket packet) {
		}

		@Override
		public void onAttacked(int attackerObjectId) {
		}
	}
}
