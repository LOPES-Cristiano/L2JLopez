package com.lopez.l2j.game.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.combat.CombatService.HitPlan;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcSkillTable;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillCanceld;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RangedAttackAndPeaceZoneCombatTest {

	private GameWorld world;
	private CombatService combatService;
	private CharTemplateTable charTemplates;
	private SkillTable skillTable;
	private NpcSkillTable npcSkillTable;
	private ZoneTable zones;
	private ScheduledExecutorService mockScheduler;
	private NpcAiService npcAiService;

	@BeforeEach
	void setUp() {
		world = new GameWorld();
		zones = mock(ZoneTable.class);
		combatService = new CombatService(1.0, 1.0, null, null, null, zones);
		charTemplates = mock(CharTemplateTable.class);
		skillTable = new SkillTable();
		npcSkillTable = mock(NpcSkillTable.class);
		mockScheduler = mock(ScheduledExecutorService.class);
		npcAiService = new NpcAiService(world, combatService, charTemplates, npcSkillTable, skillTable, null, zones);
	}

	@Test
	@DisplayName("Ataque à distância (arco) envia Attack a t=0 e agenda o dano no momento do impacto (timeToHit)")
	void testRangedPhysicalAttackSchedulesImpact() {
		NpcTemplate archerTpl = mock(NpcTemplate.class);
		when(archerTpl.id()).thenReturn(20050);
		when(archerTpl.name()).thenReturn("Orc Archer");
		when(archerTpl.attackRange()).thenReturn(600);
		when(archerTpl.pAtkSpd()).thenReturn(250);
		when(archerTpl.pAtk()).thenReturn(100);
		when(archerTpl.isMonster()).thenReturn(true);
		when(archerTpl.isAttackable()).thenReturn(true);

		NpcInstance archer = new NpcInstance(1001, archerTpl, 100, 100, 0, 0);
		world.addNpc(archer);

		PlayerCharacter player = new PlayerCharacter(
				5001, "acc", "TargetHero", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 0, 0, 0, 0, 0,
				"", 0, 0, 0, 400, 100, 0, 0,
				1000.0, 500.0, 0.0);

		CharTemplate charTpl = mock(CharTemplate.class);
		when(charTpl.pDef()).thenReturn(100);
		when(charTpl.mDef()).thenReturn(100);
		when(charTemplates.get(anyInt())).thenReturn(Optional.of(charTpl));

		TestOnlinePlayer session = new TestOnlinePlayer(player);
		world.add(session);

		archer.targetPlayerId(player.objectId());
		archer.inCombat(true);

		@SuppressWarnings("unchecked")
		ScheduledFuture<Void> scheduledFuture = mock(ScheduledFuture.class);
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);

		doReturn(scheduledFuture).when(mockScheduler).schedule(runnableCaptor.capture(), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));

		// Fornece um spy para garantir que o ataque nao falhe por roll aleatorio de miss
		CombatService spyCombatService = spy(combatService);
		doReturn(new HitPlan(65, 0, false, false)).when(spyCombatService).planAttackPlayerByNpc(eq(archer), eq(player), any());

		RangerAI rangerAI = new RangerAI(archer, world, spyCombatService, charTemplates, npcSkillTable, skillTable, mockScheduler, zones);

		// Executa ciclo de combate
		rangerAI.processCombat();

		// O pacote Attack foi enviado imediatamente para animar o disparo
		assertThat(session.sentPackets).anyMatch(p -> p instanceof Attack);

		// Mas o dano AINDA NAO foi aplicado no momento t=0!
		assertThat(player.currentHp()).isEqualTo(1000.0);

		// Um callback foi agendado para o impacto com timeToHit correspondendo a 70% do tempo de ataque
		long expectedTimeToHit = (long) ((500_000L / 250) * 0.70);
		assertThat(delayCaptor.getValue()).isEqualTo(expectedTimeToHit);
		assertThat(archer.currentAttackTask() != null).isTrue();

		// Agora simulamos a chegada do projetil (execucao do Runnable agendado)
		runnableCaptor.getValue().run();

		// Apos o impacto, o HP do jogador foi reduzido
		assertThat(player.currentHp()).isEqualTo(935.0);
	}

	@Test
	@DisplayName("Magias ofensivas de monstros: animacao transmitida a t=0, dano aplicado no fim do cast, e aborto no cancelamento")
	void testMonsterSpellCastTimingAndAbortOnDeathOrTeleport() {
		SkillTemplate nukeSkill = new SkillTemplate(
				4032, 1, "Monster Blaze", SkillTemplate.OperateType.ACTIVE, "MDAM",
				"TARGET_ONE", true, 25, 0, 0, 100.0,
				600, 0, 2000, 500, 3000, 40,
				0.0, false, List.of(), List.of(), null, null);
		skillTable.register(nukeSkill);

		when(npcSkillTable.getSkills(20060)).thenReturn(List.of(new NpcSkillTable.NpcSkillHolder(4032, 1)));

		NpcTemplate mageTpl = mock(NpcTemplate.class);
		when(mageTpl.id()).thenReturn(20060);
		when(mageTpl.name()).thenReturn("Orc Shaman");
		when(mageTpl.attackRange()).thenReturn(40);
		when(mageTpl.mAtkSpd()).thenReturn(333);
		when(mageTpl.mAtk()).thenReturn(200);
		when(mageTpl.maxMp()).thenReturn(1000);
		when(mageTpl.maxHp()).thenReturn(1000);
		when(mageTpl.isMonster()).thenReturn(true);
		when(mageTpl.isAttackable()).thenReturn(true);

		NpcInstance mage = new NpcInstance(1002, mageTpl, 100, 100, 0, 0);
		mage.currentMp(1000);
		mage.currentHp(1000);
		world.addNpc(mage);

		PlayerCharacter player = new PlayerCharacter(
				5002, "acc", "MageTarget", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0,
				"", 0, 0, 0, 200, 100, 0, 0,
				1000.0, 500.0, 500.0);

		CharTemplate charTpl = mock(CharTemplate.class);
		when(charTpl.pDef()).thenReturn(100);
		when(charTpl.mDef()).thenReturn(100);
		when(charTemplates.get(anyInt())).thenReturn(Optional.of(charTpl));

		TestOnlinePlayer session = new TestOnlinePlayer(player);
		world.add(session);

		mage.targetPlayerId(player.objectId());
		mage.inCombat(true);

		@SuppressWarnings("unchecked")
		ScheduledFuture<Void> scheduledFuture = mock(ScheduledFuture.class);
		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);

		doReturn(scheduledFuture).when(mockScheduler).schedule(runnableCaptor.capture(), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));

		MysticAI mysticAI = new MysticAI(mage, world, combatService, charTemplates, npcSkillTable, skillTable, mockScheduler, zones);

		// Executa ataque magico
		mysticAI.castMonsterSkill(nukeSkill, session, player, charTpl);

		// MagicSkillUse transmitido a t=0 com hitTime calculado
		assertThat(session.sentPackets).anyMatch(p -> p instanceof MagicSkillUse);
		assertThat(mage.isCasting()).isTrue();
		assertThat(mage.currentCastTask() != null).isTrue();

		// Dano AINDA NAO aplicado a t=0
		assertThat(player.currentHp()).isEqualTo(1000.0);

		// Tempo agendado deve respeitar o effHitTime (~2000ms com 333 mAtkSpd)
		assertThat(delayCaptor.getValue()).isGreaterThanOrEqualTo(1500L);

		// Cenário: Jogador morre ou vai para cidade (stopCombatForPlayer acionado)
		npcAiService.stopCombatForPlayer(player.objectId());

		// A conjuração do monstro deve ter sido cancelada e pacote MagicSkillCanceld enviado
		assertThat(mage.isCasting()).isFalse();
		assertThat(session.sentPackets).anyMatch(p -> p instanceof MagicSkillCanceld);
		verify(scheduledFuture, atLeastOnce()).cancel(false);

		// Se o runnable ainda assim for disparado tardiamente, nenhuma alteração de vida deve ocorrer
		player.currentHp(700.0); // vida revivida em cidade
		runnableCaptor.getValue().run();
		assertThat(player.currentHp()).isEqualTo(700.0); // dano rejeitado!
	}

	@Test
	@DisplayName("Monstros e habilidades rejeitam dano contra jogadores em Zona de Paz (Peace Zone / Cidade)")
	void testNoDamageInPeaceZone() {
		NpcTemplate tpl = mock(NpcTemplate.class);
		when(tpl.id()).thenReturn(20070);
		when(tpl.name()).thenReturn("Aggressive Mob");
		when(tpl.attackRange()).thenReturn(40);
		when(tpl.pAtkSpd()).thenReturn(200);
		when(tpl.pAtk()).thenReturn(300);

		NpcInstance mob = new NpcInstance(1003, tpl, 100, 100, 0, 0);

		PlayerCharacter player = new PlayerCharacter(
				5003, "acc", "PeacePlayer", 40, 0, 0, 0,
				0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0,
				"", 0, 0, 0, 110, 100, 0, 0,
				1000.0, 500.0, 500.0);

		CharTemplate charTpl = mock(CharTemplate.class);
		when(charTpl.pDef()).thenReturn(100);
		when(charTemplates.get(anyInt())).thenReturn(Optional.of(charTpl));

		// Jogador encontra-se dentro de uma zona de paz
		when(zones.isInsidePeace(player.x(), player.y(), player.z())).thenReturn(true);

		var hit = combatService.attackPlayer(mob, player, charTpl);
		assertThat(hit.damage()).isEqualTo(0);
		assertThat(player.currentHp()).isEqualTo(1000.0);

		var skillHit = combatService.skillAttackPlayer(mob, player, charTpl, 100.0, true, 40);
		assertThat(skillHit.damage()).isEqualTo(0);
		assertThat(player.currentHp()).isEqualTo(1000.0);
	}

	private static class TestOnlinePlayer implements GameWorld.OnlinePlayer {
		private final PlayerCharacter character;
		private final List<GameServerPacket> sentPackets = new ArrayList<>();

		TestOnlinePlayer(PlayerCharacter character) {
			this.character = character;
		}

		@Override public int objectId() { return character.objectId(); }
		@Override public String name() { return character.name(); }
		@Override public int x() { return character.x(); }
		@Override public int y() { return character.y(); }
		@Override public int z() { return character.z(); }
		@Override public PlayerCharacter character() { return character; }
		@Override public void send(GameServerPacket packet) { sentPackets.add(packet); }
	}
}
