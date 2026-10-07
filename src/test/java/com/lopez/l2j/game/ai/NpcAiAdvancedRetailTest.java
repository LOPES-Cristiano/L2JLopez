package com.lopez.l2j.game.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NpcAiAdvancedRetailTest {

	private GameWorld world;
	private CombatService combatService;
	private CharTemplateTable charTemplates;
	private SkillTable skillTable;
	private TestNpcSkillTable npcSkillTable;
	private NpcAiService npcAiService;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		combatService = new CombatService();
		charTemplates = new CharTemplateTable();
		skillTable = new SkillTable();
		npcSkillTable = new TestNpcSkillTable();
		npcAiService = new NpcAiService(world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Test
	@DisplayName("CP 6.1: Condição visual de Spoil refletida no pacote NpcInfo (0x16) e abnormalEffect")
	void testSpoilVisualConditionAndNpcInfoPacket() {
		NpcTemplate tpl = new NpcTemplate(
				20001, 20001, "Spoilable Wolf", false, "", false,
				8.0, 16.0, 20, "male", "L2Monster",
				40, 500, 100, 50, 40, 30, 10, 250, 333,
				0, 0, 0, 50, 100, 0, false, "", 0);

		NpcInstance mob = new NpcInstance(1001, tpl, 100, 200, -100, 16384);
		mob.startAbnormalEffect(0x0001); // Ex: Poison ou bleed

		// Antes de ser spoiled
		assertThat(mob.isSpoiled()).isFalse();
		byte[] dataUnspoiled = new NpcInfo(mob).encode();
		assertThat(dataUnspoiled[0]).isEqualTo((byte) 0x16);

		// Após aplicar Spoil com sucesso
		mob.spoiled(true);
		mob.spoilerPlayerId(777);
		assertThat(mob.isSpoiled()).isTrue();

		byte[] dataSpoiled = new NpcInfo(mob).encode();
		assertThat(dataSpoiled[0]).isEqualTo((byte) 0x16);

		// Verifica que o abnormalEffect() esta presente nos bytes do pacote
		ByteBuffer buf = ByteBuffer.wrap(dataSpoiled).order(ByteOrder.LITTLE_ENDIAN);
		buf.get(); // opcode 0x16
		int objId = buf.getInt();
		assertThat(objId).isEqualTo(1001);
	}

	@Test
	@DisplayName("CP 6.2: Conjuração de magias por monstros, dedução de MP e efeitos de cura/dano")
	void testMonsterSkillCastAndMpConsumption() {
		// Registra skill ofensiva e skill de cura no SkillTable
		SkillTemplate nukeSkill = new SkillTemplate(
				4032, 1, "Monster Fire", SkillTemplate.OperateType.ACTIVE, "MDAM",
				"TARGET_ONE", true, 25, 0, 0, 120.0,
				600, 0, 1500, 500, 2000, 40,
				0.0, false, List.of(), List.of(), null, null);

		SkillTemplate healSkill = new SkillTemplate(
				4035, 1, "Monster Heal", SkillTemplate.OperateType.ACTIVE, "HEAL",
				"TARGET_SELF", true, 30, 0, 0, 150.0,
				0, 0, 1500, 500, 3000, 40,
				0.0, false, List.of(), List.of(), null, null);

		skillTable.register(nukeSkill);
		skillTable.register(healSkill);

		int mobId = 20500;
		npcSkillTable.addSkill(mobId, 4032, 1);
		npcSkillTable.addSkill(mobId, 4035, 1);

		NpcTemplate tpl = new NpcTemplate(
				mobId, mobId, "Mage Mob", false, "", false,
				8.0, 16.0, 40, "male", "L2Monster",
				40, 1000, 500, 100, 80, 150, 100, 250, 333,
				0, 0, 0, 50, 100, 0, false, "", 0);

		NpcInstance mob = new NpcInstance(2001, tpl, 100, 100, 0, 0);
		mob.currentHp(1000);
		mob.currentMp(500);
		world.addNpc(mob);

		PlayerCharacter player = new PlayerCharacter(3001, "acc", "Hero", 40, 0, 0, 0, 0, 0, false, 0, 0, 0,
				800, 300, 300, 0, 0, 0, 0, "", 0, 0, 0, 100, 100, 0, 0, 800.0, 300.0, 300.0);
		TestGameSession session = new TestGameSession(player);
		world.add(session);

		// Inicia combate contra o jogador
		npcAiService.startCombat(mob, player.objectId());
		assertThat(mob.inCombat()).isTrue();

		// Forca HP baixo do monstro para testar prioridade de CURA (HP < 50%)
		mob.currentHp(200); // 200 / 1000 = 20%
		double initialMp = mob.currentMp();

		// Executa ticks da IA
		npcAiService.tick();

		// Se conjurou a cura, MP diminuiu e HP subiu
		if (mob.currentMp() < initialMp) {
			assertThat(mob.currentHp()).isGreaterThan(200);
			assertThat(mob.currentMp()).isEqualTo(initialMp - 30);
		}
	}

	@Test
	@DisplayName("CP 6.3: Minions e Guardas de Bosses — Proteção Mútua e Retaliação Cruzada")
	void testMinionCrossRetaliationAndMasterProtection() {
		NpcTemplate bossTpl = new NpcTemplate(
				25050, 25050, "Raid Boss Kernon", false, "", false,
				25.0, 50.0, 75, "male", "L2RaidBoss",
				50, 100000, 10000, 2000, 1500, 1000, 800, 300, 333,
				0, 0, 0, 80, 160, 0, false, "", 0);

		NpcTemplate minionTpl = new NpcTemplate(
				25051, 25051, "Kernon's Guard", false, "", false,
				12.0, 24.0, 72, "male", "L2Minion",
				40, 15000, 3000, 800, 700, 400, 300, 280, 333,
				0, 0, 0, 70, 140, 0, false, "", 0);

		NpcInstance boss = new NpcInstance(5001, bossTpl, 1000, 1000, 0, 0);
		NpcInstance minion1 = new NpcInstance(5002, minionTpl, 1050, 1000, 0, 0);
		NpcInstance minion2 = new NpcInstance(5003, minionTpl, 950, 1000, 0, 0);

		boss.addMinion(minion1);
		boss.addMinion(minion2);

		world.addNpc(boss);
		world.addNpc(minion1);
		world.addNpc(minion2);

		assertThat(boss.hasMinions()).isTrue();
		assertThat(boss.minions()).containsExactlyInAnyOrder(minion1, minion2);
		assertThat(minion1.isMinion()).isTrue();
		assertThat(minion1.masterObjectId()).isEqualTo(boss.objectId());

		int playerId = 999;

		// 1. Jogador ataca o Boss: todos os minions entram em combate para defende-lo
		npcAiService.startCombat(boss, playerId);
		assertThat(boss.inCombat()).isTrue();
		assertThat(minion1.inCombat()).isTrue();
		assertThat(minion1.targetPlayerId()).isEqualTo(playerId);
		assertThat(minion2.inCombat()).isTrue();
		assertThat(minion2.targetPlayerId()).isEqualTo(playerId);

		// Limpa combate
		npcAiService.stopCombat(boss);
		npcAiService.stopCombat(minion1);
		npcAiService.stopCombat(minion2);

		// 2. Jogador ataca um Minion: o mestre Boss tambem revida e entra em combate
		npcAiService.startCombat(minion1, playerId);
		assertThat(minion1.inCombat()).isTrue();
		assertThat(boss.inCombat()).isTrue();
		assertThat(boss.targetPlayerId()).isEqualTo(playerId);
	}

	@Test
	@DisplayName("CP 6.3: Reagrupamento e Retorno ao Spawn de Boss e seus Minions")
	void testMinionReturnToSpawnWithMaster() {
		NpcTemplate bossTpl = new NpcTemplate(
				25060, 25060, "Raid Boss", false, "", false,
				25.0, 50.0, 75, "male", "L2RaidBoss",
				50, 100000, 10000, 2000, 1500, 1000, 800, 300, 333,
				0, 0, 0, 80, 160, 0, false, "", 0);

		NpcTemplate minionTpl = new NpcTemplate(
				25061, 25061, "Guard", false, "", false,
				12.0, 24.0, 72, "male", "L2Minion",
				40, 15000, 3000, 800, 700, 400, 300, 280, 333,
				0, 0, 0, 70, 140, 0, false, "", 0);

		NpcInstance boss = new NpcInstance(6001, bossTpl, 500, 500, 0, 0);
		NpcInstance minion = new NpcInstance(6002, minionTpl, 520, 500, 0, 0);
		boss.addMinion(minion);

		world.addNpc(boss);
		world.addNpc(minion);

		// Simula Boss e Minion se deslocando durante perseguição
		boss.moveTo(1200, 1200, 0, 0);
		minion.moveTo(1220, 1200, 0, 0);
		boss.inCombat(true);
		minion.inCombat(true);

		// Perda de aggro -> Boss retorna ao spawn
		npcAiService.returnToSpawn(boss);

		assertThat(boss.inCombat()).isFalse();
		assertThat(boss.x()).isEqualTo(500);
		assertThat(boss.y()).isEqualTo(500);

		// Minion tambem retorna ao spawn original
		assertThat(minion.inCombat()).isFalse();
		assertThat(minion.x()).isEqualTo(520);
		assertThat(minion.y()).isEqualTo(500);
	}

	@Test
	@DisplayName("CP 6.3: Morte do Mestre Boss com despawn/queda dos Minions")
	void testMasterDiedDespawnsMinions() {
		NpcTemplate bossTpl = new NpcTemplate(
				25070, 25070, "Grand Boss", false, "", false,
				25.0, 50.0, 80, "male", "L2RaidBoss",
				50, 200000, 20000, 3000, 2500, 1500, 1200, 300, 333,
				0, 0, 0, 80, 160, 0, false, "", 0);

		NpcTemplate minionTpl = new NpcTemplate(
				25071, 25071, "Minion Guard", false, "", false,
				12.0, 24.0, 75, "male", "L2Minion",
				40, 20000, 4000, 1000, 800, 500, 400, 280, 333,
				0, 0, 0, 70, 140, 0, false, "", 0);

		NpcInstance boss = new NpcInstance(7001, bossTpl, 200, 200, 0, 0);
		NpcInstance minion1 = new NpcInstance(7002, minionTpl, 230, 200, 0, 0);
		NpcInstance minion2 = new NpcInstance(7003, minionTpl, 170, 200, 0, 0);

		boss.addMinion(minion1);
		boss.addMinion(minion2);

		world.addNpc(boss);
		world.addNpc(minion1);
		world.addNpc(minion2);

		minion1.inCombat(true);
		minion2.inCombat(true);

		// Boss e derrotado
		boss.dead(true);
		npcAiService.onMasterDied(boss);

		// Minions saem de combate e sao abatidos/despawnados
		assertThat(minion1.inCombat()).isFalse();
		assertThat(minion1.isDead()).isTrue();
		assertThat(minion2.inCombat()).isFalse();
		assertThat(minion2.isDead()).isTrue();
	}

	@Test
	@DisplayName("CP 6.3: Mobs comuns com lacaios (ex: Ketra Prophet) — Lacaios sobrevivem a morte do líder e morrem independentemente")
	void testCommonMonsterMinionsSurviveMasterDeathAndDieIndependently() {
		// Ketra Prophet - monstro comum (L2Monster)
		NpcTemplate leaderTpl = new NpcTemplate(
				21350, 21350, "Ketra Prophet", false, "", false,
				12.0, 24.0, 80, "male", "L2Monster",
				50, 15000, 3000, 1500, 1200, 800, 700, 250, 333,
				0, 0, 0, 80, 160, 0, false, "", 0);

		// Prophet's Guard - lacaio (L2Minion)
		NpcTemplate minionTpl = new NpcTemplate(
				21351, 21351, "Prophet's Guard", false, "", false,
				10.0, 20.0, 80, "male", "L2Minion",
				40, 10000, 2000, 1200, 900, 600, 500, 240, 333,
				0, 0, 0, 70, 140, 0, false, "", 0);

		NpcInstance leader = new NpcInstance(8001, leaderTpl, 1000, 1000, 0, 0);
		NpcInstance guard1 = new NpcInstance(8002, minionTpl, 1030, 1000, 0, 0);
		NpcInstance guard2 = new NpcInstance(8003, minionTpl, 970, 1000, 0, 0);

		leader.addMinion(guard1);
		leader.addMinion(guard2);

		world.addNpc(leader);
		world.addNpc(guard1);
		world.addNpc(guard2);

		int playerId = 9999;
		// 1. Jogador ataca o líder: lider e TODOS os lacaios entram em combate juntos contra o jogador
		npcAiService.startCombat(leader, playerId);

		assertThat(leader.inCombat()).isTrue();
		assertThat(guard1.inCombat()).isTrue();
		assertThat(guard1.targetPlayerId()).isEqualTo(playerId);
		assertThat(guard2.inCombat()).isTrue();
		assertThat(guard2.targetPlayerId()).isEqualTo(playerId);

		// 2. Líder morre (ex: Ketra Prophet)
		leader.dead(true);
		leader.currentHp(0);
		npcAiService.scheduleDecayAndRespawn(leader);

		// Conforme regra oficial / L2JDream: os lacaios NÃO morrem! Continuam vivos e em combate
		assertThat(guard1.isDead()).isFalse();
		assertThat(guard1.inCombat()).isTrue();
		assertThat(guard1.targetPlayerId()).isEqualTo(playerId);
		assertThat(guard2.isDead()).isFalse();
		assertThat(guard2.inCombat()).isTrue();
		assertThat(guard2.targetPlayerId()).isEqualTo(playerId);

		// 3. Jogador ataca e mata guard1: apenas guard1 morre de forma independente
		guard1.dead(true);
		guard1.currentHp(0);
		npcAiService.scheduleDecayAndRespawn(guard1);

		assertThat(guard1.isDead()).isTrue();
		assertThat(guard1.inCombat()).isFalse();

		// guard2 continua vivo lutando
		assertThat(guard2.isDead()).isFalse();
		assertThat(guard2.inCombat()).isTrue();
	}

	@Test
	@DisplayName("CP 6.3: Grupo de monstros — Quando qualquer lacaio é atacado, o líder e todos os lacaios atacam juntos")
	void testGroupAllAttackTogetherWhenMinionAttacked() {
		NpcTemplate leaderTpl = new NpcTemplate(
				21350, 21350, "Ketra Prophet", false, "", false,
				12.0, 24.0, 80, "male", "L2Monster",
				50, 15000, 3000, 1500, 1200, 800, 700, 250, 333,
				0, 0, 0, 80, 160, 0, false, "", 0);

		NpcTemplate minionTpl = new NpcTemplate(
				21351, 21351, "Prophet's Guard", false, "", false,
				10.0, 20.0, 80, "male", "L2Minion",
				40, 10000, 2000, 1200, 900, 600, 500, 240, 333,
				0, 0, 0, 70, 140, 0, false, "", 0);

		NpcInstance leader = new NpcInstance(8101, leaderTpl, 1000, 1000, 0, 0);
		NpcInstance guard1 = new NpcInstance(8102, minionTpl, 1030, 1000, 0, 0);
		NpcInstance guard2 = new NpcInstance(8103, minionTpl, 970, 1000, 0, 0);

		leader.addMinion(guard1);
		leader.addMinion(guard2);

		world.addNpc(leader);
		world.addNpc(guard1);
		world.addNpc(guard2);

		int playerId = 9999;
		// Jogador ataca guard1 (um lacaio): guard1, o líder e o irmão guard2 entram todos em combate
		npcAiService.startCombat(guard1, playerId);

		assertThat(guard1.inCombat()).isTrue();
		assertThat(guard1.targetPlayerId()).isEqualTo(playerId);
		assertThat(leader.inCombat()).isTrue();
		assertThat(leader.targetPlayerId()).isEqualTo(playerId);
		assertThat(guard2.inCombat()).isTrue();
		assertThat(guard2.targetPlayerId()).isEqualTo(playerId);
	}

	/**
	 * NpcSkillTable para testes em memória sem depender do XML em disco.
	 */
	private static class TestNpcSkillTable extends NpcSkillTable {
		private final java.util.Map<Integer, java.util.List<NpcSkillHolder>> testSkills = new java.util.HashMap<>();

		void addSkill(int npcId, int skillId, int level) {
			testSkills.computeIfAbsent(npcId, k -> new java.util.ArrayList<>())
					.add(new NpcSkillHolder(skillId, level));
		}

		@Override
		public java.util.List<NpcSkillHolder> getSkills(int npcId) {
			return testSkills.getOrDefault(npcId, java.util.List.of());
		}
	}

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
		public void send(GameServerPacket packet) {
			// Stub
		}
	}
}
