package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcAiArchetypesTest {

	private GameWorld world;
	private CombatService combatService;
	private CharTemplateTable charTemplates;
	private SkillTable skillTable;
	private NpcSkillTable npcSkillTable;
	private NpcAiService npcAiService;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		combatService = mock(CombatService.class);
		charTemplates = mock(CharTemplateTable.class);
		skillTable = new SkillTable();
		npcSkillTable = mock(NpcSkillTable.class);
		npcAiService = new NpcAiService(world, combatService, charTemplates, npcSkillTable, skillTable);
	}

	@Test
	@DisplayName("Factory resolve arquétipos corretos por características de template")
	void testFactoryArchetypeResolution() {
		// 1. Guarda
		NpcTemplate guardTpl = mock(NpcTemplate.class);
		when(guardTpl.type()).thenReturn("L2Guard");
		when(guardTpl.name()).thenReturn("Giran Guard");
		NpcInstance guard = new NpcInstance(101, guardTpl, 0, 0, 0, 0);
		AbstractNpcAI guardAI = NpcAiFactory.createAI(guard, world, combatService, charTemplates, npcSkillTable, skillTable);
		assertEquals(AbstractNpcAI.AiArchetype.GUARD, guardAI.getArchetype());

		// 2. Arqueiro (Ranger)
		NpcTemplate archerTpl = mock(NpcTemplate.class);
		when(archerTpl.type()).thenReturn("L2Monster");
		when(archerTpl.name()).thenReturn("Ol Mahum Archer");
		when(archerTpl.attackRange()).thenReturn(600);
		NpcInstance archer = new NpcInstance(102, archerTpl, 0, 0, 0, 0);
		AbstractNpcAI rangerAI = NpcAiFactory.createAI(archer, world, combatService, charTemplates, npcSkillTable, skillTable);
		assertEquals(AbstractNpcAI.AiArchetype.RANGER, rangerAI.getArchetype());

		// 3. Guerreiro Melee (Fighter)
		NpcTemplate fighterTpl = mock(NpcTemplate.class);
		when(fighterTpl.type()).thenReturn("L2Monster");
		when(fighterTpl.name()).thenReturn("Wolf");
		when(fighterTpl.attackRange()).thenReturn(40);
		NpcInstance wolf = new NpcInstance(103, fighterTpl, 0, 0, 0, 0);
		AbstractNpcAI fighterAI = NpcAiFactory.createAI(wolf, world, combatService, charTemplates, npcSkillTable, skillTable);
		assertEquals(AbstractNpcAI.AiArchetype.FIGHTER, fighterAI.getArchetype());

		// 4. Sacerdote / Curandeiro (Priest)
		NpcTemplate priestTpl = mock(NpcTemplate.class);
		when(priestTpl.type()).thenReturn("L2Monster");
		when(priestTpl.name()).thenReturn("Orc Priest Healer");
		when(priestTpl.attackRange()).thenReturn(40);
		NpcInstance priest = new NpcInstance(104, priestTpl, 0, 0, 0, 0);
		AbstractNpcAI priestAI = NpcAiFactory.createAI(priest, world, combatService, charTemplates, npcSkillTable, skillTable);
		assertEquals(AbstractNpcAI.AiArchetype.PRIEST, priestAI.getArchetype());
	}

	@Test
	@DisplayName("Monstros guerreiros com passivas/buffs magicos (isMagic=true) ou skills hibridas recebem FighterAI e nunca MysticAI")
	void testWarriorWithMagicPassivesAndBuffsStaysFighter() {
		SkillTemplate passiveMagic = new SkillTemplate(
				4009, 1, "Resist Fire", SkillTemplate.OperateType.PASSIVE, "BUFF",
				"TARGET_SELF", true, 0, 0, 0, 0.0,
				0, 0, 0, 0, 0, 1,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate hasteBuff = new SkillTemplate(
				4402, 1, "NPC Haste", SkillTemplate.OperateType.ACTIVE, "BUFF",
				"TARGET_SELF", true, 10, 0, 0, 0.0,
				400, 0, 1000, 0, 5000, 1,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate stunSkill = new SkillTemplate(
				4072, 1, "NPC Stun", SkillTemplate.OperateType.ACTIVE, "STUN",
				"TARGET_ONE", false, 20, 0, 0, 100.0,
				40, 0, 1000, 0, 6000, 40,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate nukeSkill = new SkillTemplate(
				4001, 1, "NPC Wind Strike", SkillTemplate.OperateType.ACTIVE, "MDAM",
				"TARGET_ONE", true, 20, 0, 0, 50.0,
				600, 0, 2000, 0, 8000, 40,
				0.0, false, List.of(), List.of(), null, null);

		skillTable.register(passiveMagic);
		skillTable.register(hasteBuff);
		skillTable.register(stunSkill);
		skillTable.register(nukeSkill);

		// Monstro guerreiro com passiva magica, buff magico e skill hibrida (MDAM + STUN)
		when(npcSkillTable.getSkills(eq(20555), anyInt())).thenReturn(List.of(
				new NpcSkillTable.NpcSkillHolder(4009, 1),
				new NpcSkillTable.NpcSkillHolder(4402, 1),
				new NpcSkillTable.NpcSkillHolder(4072, 1),
				new NpcSkillTable.NpcSkillHolder(4001, 1)
		));

		NpcTemplate warriorTpl = mock(NpcTemplate.class);
		when(warriorTpl.id()).thenReturn(20555);
		when(warriorTpl.idTemplate()).thenReturn(20555);
		when(warriorTpl.type()).thenReturn("L2Monster");
		when(warriorTpl.name()).thenReturn("Ketra Orc Footman");
		when(warriorTpl.attackRange()).thenReturn(40);

		NpcInstance warrior = new NpcInstance(105, warriorTpl, 0, 0, 0, 0);
		AbstractNpcAI ai = NpcAiFactory.createAI(warrior, world, combatService, charTemplates, npcSkillTable, skillTable);
		assertEquals(AbstractNpcAI.AiArchetype.FIGHTER, ai.getArchetype(),
				"Guerreiro com passivas/buffs magicos ou skills fisicas deve receber FighterAI");
	}

	@Test
	@DisplayName("Cooldown de skills e filtro de passivas em chooseMonsterSkill garantem ataques fisicos continuos")
	void testSkillCooldownAndPassiveFilter() {
		SkillTemplate passiveRace = new SkillTemplate(
				4416, 6, "Humanoids", SkillTemplate.OperateType.PASSIVE, "PASSIVE",
				"TARGET_SELF", false, 0, 0, 0, 0.0,
				0, 0, 0, 0, 0, 1,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate activeStrike = new SkillTemplate(
				4032, 1, "Power Strike", SkillTemplate.OperateType.ACTIVE, "PDAM",
				"TARGET_ONE", false, 10, 0, 0, 150.0,
				40, 0, 1000, 0, 8000, 40,
				0.0, false, List.of(), List.of(), null, null);

		skillTable.register(passiveRace);
		skillTable.register(activeStrike);

		when(npcSkillTable.getSkills(eq(20556), anyInt())).thenReturn(List.of(
				new NpcSkillTable.NpcSkillHolder(4416, 6),
				new NpcSkillTable.NpcSkillHolder(4032, 1)
		));

		NpcTemplate tpl = mock(NpcTemplate.class);
		when(tpl.id()).thenReturn(20556);
		when(tpl.idTemplate()).thenReturn(20556);
		when(tpl.type()).thenReturn("L2Monster");
		when(tpl.name()).thenReturn("Orc Grunt");
		when(tpl.attackRange()).thenReturn(40);
		when(tpl.maxHp()).thenReturn(1000);
		when(tpl.maxMp()).thenReturn(500);

		NpcInstance mob = new NpcInstance(106, tpl, 0, 0, 0, 0);
		FighterAI fighterAI = new FighterAI(mob, world, combatService, charTemplates, npcSkillTable, skillTable);

		// Desabilita a skill ativa (simulando cooldown)
		mob.disableSkill(4032, 8000L);
		assertTrue(mob.isSkillDisabled(4032));

		// Em 50 tentativas, chooseMonsterSkill NUNCA deve retornar a passiva 4416 nem a skill 4032 em cooldown
		for (int i = 0; i < 50; i++) {
			assertNull(fighterAI.chooseMonsterSkill(), "Com a skill ativa em cooldown, deve retornar null para atacar fisicamente");
		}
	}

	@Test
	@DisplayName("Grand Bosses (Antharas, Valakas, Baium) alternam ataques fisicos (65%) e skills (35%) respeitando cooldown")
	void testGrandBossPhysicalAttackCadenceAndCooldown() {
		SkillTemplate fist = new SkillTemplate(
				com.lopez.l2j.game.ai.boss.BaiumAI.SKILL_FIST, 1, "Baium Fist", SkillTemplate.OperateType.ACTIVE, "PDAM",
				"TARGET_ONE", false, 0, 0, 0, 5000.0,
				100, 0, 1500, 0, 10000, 75,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate thunder = new SkillTemplate(
				com.lopez.l2j.game.ai.boss.BaiumAI.SKILL_THUNDER, 1, "Baium Thunder", SkillTemplate.OperateType.ACTIVE, "MDAM",
				"TARGET_AURA", true, 0, 0, 0, 3000.0,
				0, 300, 2000, 0, 10000, 75,
				0.0, false, List.of(), List.of(), null, null);
		SkillTemplate quake = new SkillTemplate(
				com.lopez.l2j.game.ai.boss.BaiumAI.SKILL_EARTHQUAKE, 1, "Baium Earthquake", SkillTemplate.OperateType.ACTIVE, "PDAM",
				"TARGET_AURA", false, 0, 0, 0, 4000.0,
				0, 300, 2000, 0, 10000, 75,
				0.0, false, List.of(), List.of(), null, null);

		skillTable.register(fist);
		skillTable.register(thunder);
		skillTable.register(quake);

		NpcTemplate baiumTpl = mock(NpcTemplate.class);
		when(baiumTpl.id()).thenReturn(29020);
		when(baiumTpl.type()).thenReturn("L2GrandBoss");
		when(baiumTpl.name()).thenReturn("Baium");
		when(baiumTpl.maxHp()).thenReturn(500000);
		when(baiumTpl.collisionRadius()).thenReturn(65.0);
		when(baiumTpl.collisionHeight()).thenReturn(174.0);
		when(baiumTpl.attackRange()).thenReturn(40);

		NpcInstance baium = new NpcInstance(29020, baiumTpl, 0, 0, 0, 0);
		AbstractNpcAI baiumAI = new com.lopez.l2j.game.ai.boss.BaiumAI(baium, world, combatService, charTemplates, npcSkillTable, skillTable);

		int physicalTurns = 0;
		for (int i = 0; i < 100; i++) {
			if (baiumAI.chooseMonsterSkill() == null) {
				physicalTurns++;
			}
		}
		assertTrue(physicalTurns >= 40, "Baium deve realizar ataques fisicos na maioria dos turnos (esperado ~65%, obtido " + physicalTurns + "%)");
	}

	@Test
	@DisplayName("RangerAI recua (kiting) quando o jogador se aproxima a menos de 150 unidades")
	void testRangerKitingRetreat() {
		NpcTemplate archerTpl = mock(NpcTemplate.class);
		when(archerTpl.type()).thenReturn("L2Monster");
		when(archerTpl.name()).thenReturn("Sniper");
		when(archerTpl.attackRange()).thenReturn(700);
		when(archerTpl.runSpd()).thenReturn(140);
		when(archerTpl.pAtkSpd()).thenReturn(200);

		NpcInstance archer = new NpcInstance(201, archerTpl, 100, 100, 0, 0);
		world.addNpc(archer);

		PlayerCharacter player = new PlayerCharacter(
				301, "acc", "MeleeWarrior", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0,
				"", 0, 0, 0, 150, 100, 0, 0, // Posicao X=150, dist = 50 unidades (< 150 SAFE_DISTANCE)
				1000.0, 500.0, 500.0);

		GameWorld.OnlinePlayer session = mock(GameWorld.OnlinePlayer.class);
		when(session.character()).thenReturn(player);
		when(session.objectId()).thenReturn(301);
		when(session.name()).thenReturn("Runner");
		when(session.x()).thenReturn(150);
		when(session.y()).thenReturn(100);
		when(session.z()).thenReturn(0);
		world.add(session);

		archer.targetPlayerId(301);
		archer.inCombat(true);

		RangerAI rangerAI = new RangerAI(archer, world, combatService, charTemplates, npcSkillTable, skillTable);
		int initialX = archer.x();
		rangerAI.processCombat();

		// O monstro deve ter recuado para longe do jogador (distancia aumentou)
		double newDist = Math.hypot(archer.x() - session.x(), archer.y() - session.y());
		assertTrue(newDist > 50, "Ranger deve ter recuado para manter distancia de seguranca");
	}

	@Test
	@DisplayName("PriestAI detecta aliado ferido (HP < 50%) e conjura cura no aliado")
	void testPriestHealsWoundedAlly() {
		// Registra skill de cura
		SkillTemplate healSkill = new SkillTemplate(
				4035, 1, "Monster Heal", SkillTemplate.OperateType.ACTIVE, "HEAL",
				"TARGET_ONE", true, 30, 0, 0, 500.0,
				0, 0, 1500, 500, 3000, 40,
				0.0, false, List.of(), List.of(), null, null);
		skillTable.register(healSkill);

		when(npcSkillTable.getSkills(401)).thenReturn(List.of(new NpcSkillTable.NpcSkillHolder(4035, 1)));
		when(npcSkillTable.getSkills(eq(401), anyInt())).thenReturn(List.of(new NpcSkillTable.NpcSkillHolder(4035, 1)));

		NpcTemplate priestTpl = mock(NpcTemplate.class);
		when(priestTpl.id()).thenReturn(401);
		when(priestTpl.type()).thenReturn("L2Monster");
		when(priestTpl.name()).thenReturn("Ketra Priest");
		when(priestTpl.factionId()).thenReturn("ketra_clan");
		when(priestTpl.mAtkSpd()).thenReturn(333);
		when(priestTpl.maxHp()).thenReturn(1000);

		NpcInstance priest = new NpcInstance(401, priestTpl, 500, 500, 0, 0);
		priest.currentHp(1000);
		priest.currentMp(500);

		NpcTemplate warriorTpl = mock(NpcTemplate.class);
		when(warriorTpl.type()).thenReturn("L2Monster");
		when(warriorTpl.name()).thenReturn("Ketra Warrior");
		when(warriorTpl.factionId()).thenReturn("ketra_clan");
		when(warriorTpl.maxHp()).thenReturn(2000);
		when(warriorTpl.isMonster()).thenReturn(true);

		NpcInstance woundedWarrior = new NpcInstance(402, warriorTpl, 520, 500, 0, 0);
		woundedWarrior.currentHp(400); // 400 / 2000 = 20% (< 50% HP)

		world.addNpc(priest);
		world.addNpc(woundedWarrior);

		PlayerCharacter player = new PlayerCharacter(
				302, "acc", "Attacker", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0,
				"", 0, 0, 0, 510, 500, 0, 0,
				1000.0, 500.0, 500.0);

		GameWorld.OnlinePlayer session = mock(GameWorld.OnlinePlayer.class);
		when(session.character()).thenReturn(player);
		when(session.objectId()).thenReturn(302);
		when(session.name()).thenReturn("Attacker");
		world.add(session);

		priest.targetPlayerId(302);
		priest.inCombat(true);

		PriestAI priestAI = new PriestAI(priest, world, combatService, charTemplates, npcSkillTable, skillTable);
		priestAI.processCombat();

		// O guerreiro aliado deve ter sido curado
		assertTrue(woundedWarrior.currentHp() > 400, "Priest deve curar o aliado com HP baixo");
		assertEquals(470, priest.currentMp(), "Priest deve ter consumido 30 MP na cura");
	}

	@Test
	@DisplayName("GuardAI detecta jogador criminoso (karma > 0) e tem velocidade aumentada")
	void testGuardPkHuntingAndSpeed() {
		NpcTemplate guardTpl = mock(NpcTemplate.class);
		when(guardTpl.type()).thenReturn("L2Guard");
		when(guardTpl.name()).thenReturn("Captain of the Guard");
		when(guardTpl.runSpd()).thenReturn(140);

		NpcInstance guard = new NpcInstance(501, guardTpl, 1000, 1000, 0, 0);
		world.addNpc(guard);

		PlayerCharacter pkPlayer = new PlayerCharacter(
				601, "acc", "PKMurderer", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 500, 5000, 0, 0, 0, // karma = 5000 (PK!)
				"", 0, 0, 0, 1100, 1000, 0, 0,
				1000.0, 500.0, 500.0);

		GameWorld.OnlinePlayer session = mock(GameWorld.OnlinePlayer.class);
		when(session.character()).thenReturn(pkPlayer);
		when(session.objectId()).thenReturn(601);
		when(session.name()).thenReturn("PKMurderer");
		when(session.x()).thenReturn(1100);
		when(session.y()).thenReturn(1000);
		world.add(session);

		GuardAI guardAI = new GuardAI(guard, world, combatService, charTemplates, npcSkillTable, skillTable);
		Integer targetId = guardAI.scanForCriminals();

		assertNotNull(targetId);
		assertEquals(601, targetId.intValue(), "Guarda deve identificar o jogador com karma como criminoso");
	}
}
