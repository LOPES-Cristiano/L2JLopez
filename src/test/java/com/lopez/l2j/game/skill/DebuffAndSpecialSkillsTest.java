package com.lopez.l2j.game.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.effect.PlayerEffects.ActiveBuff;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DebuffAndSpecialSkillsTest {

	private static SkillTable skillTable;
	private static CombatService combatService;
	private static CharTemplate humanFighterTemplate;

	@BeforeAll
	static void setUp() {
		skillTable = new SkillTable(Path.of("data", "xml", "stats", "skills"));
		combatService = new CombatService();
		humanFighterTemplate = new CharTemplateTable().get(0).orElseThrow();
	}

	private static PlayerCharacter createPlayer(int objId, String name, int level) {
		return new PlayerCharacter(objId, name, "Title", level, 0, 0, 0, 0, 0, false, 0, 0, 0,
				1000, 500, 500, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 1000.0, 500.0, 500.0);
	}

	private static NpcInstance createMonster(int objId, int npcId, int level, double maxHp, double maxMp) {
		var tpl = new NpcTemplate(npcId, npcId, "TestMonster", false, "Monster", false,
				9.0, 24.0, level, "male", "Monster", 40, (int) maxHp, (int) maxMp,
				200, 150, 100, 120, 253, 333, 0, 0, 0, 50, 120, 0, false, 1000L, 100);
		return new NpcInstance(objId, tpl, 1000, 1000, -100, 0);
	}

	@Test
	void testSkillClassification() {
		// Mana Burn (1398) deve ser MANADAM e nao causar dano fisico/magico
		var manaBurn = skillTable.get(1398, 1).orElseThrow();
		assertTrue(manaBurn.isManaBurn());
		assertTrue(manaBurn.isOffensive());
		assertFalse(manaBurn.isPhysicalDamage());
		assertFalse(manaBurn.isMagicDamage());

		// Shield Stun (92) e puro debuff fisico, nao e PDAM
		var shieldStun = skillTable.get(92, 1).orElseThrow();
		assertEquals("STUN", shieldStun.skillType());
		assertTrue(shieldStun.isOffensive());
		assertFalse(shieldStun.isPhysicalDamage());
		assertFalse(shieldStun.isMagicDamage());

		// Hex (122) e puro debuff magico, nao e MDAM
		var hex = skillTable.get(122, 1).orElseThrow();
		assertEquals("DEBUFF", hex.skillType());
		assertTrue(hex.isOffensive());
		assertFalse(hex.isPhysicalDamage());
		assertFalse(hex.isMagicDamage());

		// Sleep (1069) e puro debuff de sono
		var sleep = skillTable.get(1069, 1).orElseThrow();
		assertEquals("SLEEP", sleep.skillType());
		assertTrue(sleep.isOffensive());
		assertFalse(sleep.isPhysicalDamage());
		assertFalse(sleep.isMagicDamage());
	}

	@Test
	void testManaBurnDoesNotDealHpDamage() {
		var player1 = createPlayer(1001, "Caster", 76);
		var player2 = createPlayer(1002, "Target", 76);
		var mob = createMonster(2001, 20001, 70, 5000.0, 2000.0);

		// Mana Burn no monstro
		int mpDamNpc = combatService.skillManaDamNpc(player1, humanFighterTemplate, mob, 162.0, 74, false, false);
		assertTrue(mpDamNpc > 0, "Mana Burn deve reduzir MP");
		assertEquals(5000.0, mob.currentHp(), "HP do monstro nao pode sofrer dano por Mana Burn");

		// Mana Burn no jogador
		int mpDamPlayer = combatService.skillManaDamPlayer(player1, humanFighterTemplate, player2, humanFighterTemplate,
				162.0, 74, false, false);
		assertTrue(mpDamPlayer > 0, "Mana Burn em PvP deve reduzir MP");
		assertEquals(1000.0, player2.currentHp(), "HP do jogador alvo nao pode sofrer dano por Mana Burn");
		assertEquals(500.0, player2.currentCp(), "CP do jogador alvo nao pode sofrer dano por Mana Burn");
	}

	@Test
	void testMonsterManaBurnDoesNotDealHpDamageToPlayer() {
		var mob = createMonster(2002, 20002, 75, 10000.0, 5000.0);
		var player = createPlayer(1003, "TargetPlayer", 75);

		int mpDam = combatService.skillManaDamNpcToPlayer(mob, player, humanFighterTemplate, 150.0, 75);
		assertTrue(mpDam > 0, "Dreno de mana do monstro deve reduzir MP");
		assertEquals(1000.0, player.currentHp(), "HP nao deve ser alterado por dreno de mana de monstro");
	}

	@Test
	void testZeroPowerDebuffDealsZeroDamage() {
		var mob = createMonster(2003, 20003, 70, 5000.0, 1000.0);
		var player = createPlayer(1004, "TargetPlayer", 70);

		// skillAttackPlayer com power 0 (pure debuff)
		var res = combatService.skillAttackPlayer(mob, player, humanFighterTemplate, 0, false, 70);
		assertEquals(0, res.damage(), "Skill sem power nao deve causar dano");
		assertEquals(1000.0, player.currentHp());
	}

	@Test
	void testPetrificationInvulnerability() {
		var mob = createMonster(2004, 20004, 70, 5000.0, 1000.0);
		mob.invul(true);
		assertTrue(mob.invul());

		var resNpc = combatService.applyDamage(mob, 500, 0);
		assertEquals(0, resNpc.damage(), "Alvo petrificado/invulneravel nao deve tomar dano");
		assertEquals(5000, resNpc.remainingHp());

		var player = createPlayer(1005, "PetrifiedPlayer", 70);
		player.invul(true);
		var resPlayer = combatService.applyDamagePlayer(player, 500);
		assertEquals(0, resPlayer.damage(), "Jogador petrificado/invulneravel nao deve tomar dano");
		assertEquals(1000.0, player.currentHp());
	}

	@Test
	void testSleepWakesUpOnDamage() {
		var player = createPlayer(1006, "SleepingPlayer", 70);
		player.disable(System.currentTimeMillis() + 10000, true);
		player.startAbnormalEffect(0x0080);
		assertTrue(player.isDisabled());
		assertEquals(0x0080, player.abnormalEffect() & 0x0080);

		// Ao sofrer dano (onDamaged), o sono e interrompido
		player.onDamaged();
		assertFalse(player.isDisabled());
		assertEquals(0, player.abnormalEffect() & 0x0080, "Mascara anormal de sono deve ser removida");

		// No monstro
		var mob = createMonster(2005, 20005, 70, 1000.0, 100.0);
		mob.disable(System.currentTimeMillis() + 10000, true);
		mob.startAbnormalEffect(0x0080);
		assertTrue(mob.isDisabled());
		assertEquals(0x0080, mob.abnormalEffect() & 0x0080);

		mob.onDamaged();
		assertFalse(mob.isDisabled());
		assertEquals(0, mob.abnormalEffect() & 0x0080);
	}

	@Test
	void testPlayerEffectsSeparatesDebuffsFromMaxBuffLimit() {
		var effects = new PlayerEffects();
		long now = System.currentTimeMillis();

		// Adiciona 24 buffs normais
		for (int i = 1; i <= 24; i++) {
			effects.put(new ActiveBuff(1000 + i, 1, "buff_" + i, now + 60000, 0, 1.0, 1.0, 0, List.of(), false), 24);
		}
		assertEquals(24, effects.active().size());

		// Adiciona 2 debuffs (ex: Hex e Curse: Weakness)
		effects.put(ActiveBuff.ofSkill(122, 1, "pDefDown", now + 15000, List.of(), true), 24);
		effects.put(ActiveBuff.ofSkill(1164, 1, "pAtkDown", now + 15000, List.of(), true), 24);

		// Os debuffs nao devem remover os buffs positivos
		assertEquals(26, effects.active().size());
		long buffCount = effects.active().stream().filter(b -> !b.isDebuff()).count();
		long debuffCount = effects.active().stream().filter(ActiveBuff::isDebuff).count();
		assertEquals(24, buffCount, "Buffs positivos devem ser preservados");
		assertEquals(2, debuffCount, "Debuffs devem estar presentes em slots dedicados");
	}

	@Test
	void testPhysicalMuteBlock() {
		var player = createPlayer(1007, "MutedPlayer", 70);
		assertFalse(player.isPhysicalMuted());

		player.physicalMute(System.currentTimeMillis() + 5000);
		assertTrue(player.isPhysicalMuted());

		var mob = createMonster(2006, 20006, 70, 1000.0, 100.0);
		assertFalse(mob.isPhysicalMuted());
		mob.physicalMute(System.currentTimeMillis() + 5000);
		assertTrue(mob.isPhysicalMuted());
	}

	@Test
	void testCpDamageWrath() {
		var wrath = skillTable.get(320, 10).orElseThrow();
		assertTrue(wrath.isCpDamage(), "Wrath deve ser identificado como CPDAM");
		assertTrue(wrath.isOffensive());
		assertFalse(wrath.isPhysicalDamage());
		assertFalse(wrath.isMagicDamage());

		var player = createPlayer(1008, "WrathTarget", 75);
		assertEquals(500.0, player.currentCp());
		assertEquals(1000.0, player.currentHp());

		// Wrath nível 10 tem power 0.7 (reduz 30% do CP atual)
		int cpDam = (int) Math.round(player.currentCp() * Math.max(0.0, 1.0 - wrath.power()));
		assertEquals(150, cpDam);
		player.currentCp(player.currentCp() - cpDam);

		assertEquals(350.0, player.currentCp(), "CP deve ter sido reduzido em 30%");
		assertEquals(1000.0, player.currentHp(), "HP nao deve ser afetado por CPDAM");
	}

	@Test
	void testNegateAndCleanse() {
		var player = createPlayer(1009, "CleanseTarget", 75);
		long until = System.currentTimeMillis() + 10000;

		player.root(until);
		player.mute(until);
		player.disable(until, false);
		player.startAbnormalEffect(0x0040 | 0x0020 | 0x0010);

		assertTrue(player.isRooted());
		assertTrue(player.isMuted());
		assertTrue(player.isDisabled());

		// Cleanse limpa todos os controles e anormais
		player.clearControls();
		assertFalse(player.isRooted());
		assertFalse(player.isMuted());
		assertFalse(player.isDisabled());
		assertEquals(0, player.abnormalEffect());

		// Limpeza de debuffs em PlayerEffects
		var effects = player.effects();
		effects.put(ActiveBuff.ofSkill(1164, 1, "curse_weakness", until, List.of(), true));
		effects.put(ActiveBuff.ofSkill(1086, 1, "haste", until, List.of(), false));
		assertEquals(2, effects.active().size());

		effects.clearDebuffs();
		assertEquals(1, effects.active().size());
		assertFalse(effects.active().get(0).isDebuff(), "Buff positivo deve permanecer apos cleanse");
	}

	@Test
	void testCurseDeathLinkScaling() {
		var cdl = skillTable.get(1159, 1).orElseThrow();
		assertEquals("DEATHLINK", cdl.skillType());
		assertTrue(cdl.isMagicDamage());

		double fullHpRatio = 1.0;
		double multFullHp = Math.max(0.2, Math.pow(1.7165 - fullHpRatio, 2) * 0.577);

		double lowHpRatio = 0.1;
		double multLowHp = Math.max(0.2, Math.pow(1.7165 - lowHpRatio, 2) * 0.577);

		assertTrue(multLowHp > multFullHp * 4, "Curse Death Link deve causar muito mais dano com HP baixo");
	}

	@Test
	void testLifeBalanceCalculation() {
		var player1 = createPlayer(1010, "Healer", 75);
		var player2 = createPlayer(1011, "Tanker", 75);

		player1.currentHp(200.0); // 20%
		player2.currentHp(1000.0); // 100%

		double totalCurHp = player1.currentHp() + player2.currentHp(); // 1200
		double totalMaxHp = player1.maxHp() + player2.maxHp(); // 2000
		double ratio = totalCurHp / totalMaxHp; // 0.60 (60%)

		player1.currentHp((int) (player1.maxHp() * ratio));
		player2.currentHp((int) (player2.maxHp() * ratio));

		assertEquals(600.0, player1.currentHp());
		assertEquals(600.0, player2.currentHp());
	}
}
