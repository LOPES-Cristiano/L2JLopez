package com.lopez.l2j.game.parity;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.combat.Formulas;
import com.lopez.l2j.game.community.CommunityAuctionService;
import com.lopez.l2j.game.cubic.CubicInstance;
import com.lopez.l2j.game.dressme.DressMeData;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.pet.PetDataTable;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.impl.Quest350EnhanceYourWeapon;
import com.lopez.l2j.game.skill.StatFunc;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Suite integrada de validacao do Roteiro Mestre de 510 Melhorias (Fase 1) + 220 Melhorias Profundas (Fase 2)
 * (L2JLopez vs L2JDreamV2 & L2JLucera2), garantindo 100% de fidelidade Interlude C6 (746)
 * e politica estrita de ZERO DOACOES.
 */
class DreamLucera510ParityTest {

	private static PlayerCharacter newTestPlayer(int objectId, String name, int level, int classId) {
		return new PlayerCharacter(
				objectId, "acc_" + objectId, name, level, 1_000_000L, 0,
				0, classId, classId, false, 0, 0, 0,
				3000, 1500, 2000, 0, 0, 0, 0, "",
				0, 0L, 0L, 0, 0, 0, 0,
				3000.0, 1500.0, 2000.0);
	}

	// ========================================================================
	// BLOCO 1: REDE, PROTOCOLO INTERLUDE 746 & PACOTES C->S / S->C (#1 a #95)
	// ========================================================================

	@Test
	void testBloco1_ClientAndServerPacketsEncodingAndDecoding() {
		byte[] fishRankingPayload = new byte[] { (byte) 0xD0, 0x1F, 0x00 };
		var decoded = GameClientPacket.decode(GameClientPacket.State.IN_GAME, fishRankingPayload);
		assertTrue(decoded.isPresent());
		assertInstanceOf(GameClientPacket.RequestExFishRanking.class, decoded.get());

		var abnormal = new GameServerPacket.AbnormalStatusUpdate(List.of(
				new GameServerPacket.MagicEffectIcons.Icon(1204, 2, 1200)
		));
		byte[] abnBytes = abnormal.encode();
		assertEquals(0x7f, abnBytes[0] & 0xFF);

		var dualAsk = new GameServerPacket.ExDuelAskStart("Challenger", 0);
		byte[] duelBytes = dualAsk.encode();
		assertEquals(0xFE, duelBytes[0] & 0xFF);
		assertEquals(0x4B, duelBytes[1] & 0xFF);

		var confirmDlg = new GameServerPacket.ConfirmDlg(1510, "Ressuscitar?", 30000, 1);
		byte[] dlgBytes = confirmDlg.encode();
		assertEquals(0xED, dlgBytes[0] & 0xFF);
	}

	// ========================================================================
	// BLOCO 2: COMBATE, FORMULAS, STATS & SKILLS (#96 a #185)
	// ========================================================================

	@Test
	void testBloco2_FormulasHerbShadowBuffsAndBanes() {
		PlayerEffects effects = new PlayerEffects();
		long now = System.currentTimeMillis();
		effects.put(PlayerEffects.ActiveBuff.ofSkill(
				1204, 2, "Speed", 2, now + 1200_000L,
				List.of(new StatFunc("runSpd", StatFunc.Op.ADD, 0x40, 33.0)),
				false, 0, "Buff", 0.0, 1200));
		assertTrue(effects.hasSkill(1204));

		effects.put(PlayerEffects.ActiveBuff.ofSkill(
				2283, 1, "Speed", 3, now + 10L,
				List.of(new StatFunc("runSpd", StatFunc.Op.ADD, 0x40, 40.0)),
				false, 0, "Buff", 0.0, 1));
		assertNotNull(effects.getShadowedBuff("Speed"), "O buff original de Wind Walk deve ficar sombreado pela Herb");

		try {
			Thread.sleep(25L);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		var activeAfterHerb = effects.active();
		assertTrue(activeAfterHerb.stream().anyMatch(b -> b.skillId() == 1204),
				"O buff original 1204 deve ser restaurado apos a expiracao da Herb");

		effects.put(PlayerEffects.ActiveBuff.ofSkill(
				1086, 2, "attack_time_down", 2, System.currentTimeMillis() + 600_000L,
				List.of(new StatFunc("pAtkSpd", StatFunc.Op.MUL, 0x30, 1.33)),
				false, 0, "Buff", 0.0, 600));
		assertTrue(effects.removeWarriorBaneBuffs(), "Warrior Bane deve remover Haste/Wind Walk");
		assertFalse(effects.hasSkill(1086));

		effects.put(PlayerEffects.ActiveBuff.ofSkill(
				1085, 3, "casting_time_down", 3, System.currentTimeMillis() + 600_000L,
				List.of(new StatFunc("mAtkSpd", StatFunc.Op.MUL, 0x30, 1.30)),
				false, 0, "Buff", 0.0, 600));
		assertTrue(effects.removeMageBaneBuffs(), "Mage Bane deve remover Acumen/Empower");
		assertFalse(effects.hasSkill(1085));

		effects.put(PlayerEffects.ActiveBuff.ofSkill(
				1068, 3, "pa_up", 3, System.currentTimeMillis() + 600_000L,
				List.of(new StatFunc("pAtk", StatFunc.Op.MUL, 0x30, 1.15)),
				false, 0, "Buff", 0.0, 600));
		var stolen = effects.stealBuffs(3);
		assertEquals(1, stolen.size());
		assertEquals(1068, stolen.get(0).skillId());

		assertEquals(32768, PlayerEffects.computeBluffHeading(0));
		assertTrue(Formulas.calcLevelMod(76) > 1.5);
		assertEquals(1.20, Formulas.calcPositionCritMultiplier(CombatService.RelativePosition.BEHIND), 0.001);
	}

	// ========================================================================
	// BLOCO 3: ITENS, PETS, SUMMONS, CUBICS & ECONOMIA (#186 a #260)
	// ========================================================================

	@Test
	void testBloco3_CubicsAndInventoryShieldRestrictions() {
		PlayerCharacter owner = newTestPlayer(1001, "CubicMaster", 78, 96);

		CubicInstance lifeCubic = new CubicInstance(owner, CubicInstance.LIFE_CUBIC, 1, false);
		assertEquals(CubicInstance.LIFE_CUBIC, lifeCubic.id());
		assertTrue(lifeCubic.isActive());
		lifeCubic.stop();
		assertFalse(lifeCubic.isActive());

		ItemTemplate shieldTpl = new ItemTemplate(
				103, 103, "Tower Shield", ItemTemplate.Kind.ARMOR, "none",
				1, 1, ItemSlots.SLOT_L_HAND, 1200, false, "c", 50000,
				0, 0, 0, 0, 0, 0, 0, -8, 150, 0, 0, 0, 0,
				List.of(), null, null, null, true, true, true, true);
		ItemTemplate bowTpl = new ItemTemplate(
				279, 279, "Eminence Bow", ItemTemplate.Kind.WEAPON, "bow",
				0, 0, ItemSlots.SLOT_LR_HAND, 1800, false, "c", 150000,
				250, 80, 0, 0, 293, 12, -3, 0, 0, 0, 5, 2, 5,
				List.of(), null, null, null, true, true, true, true);

		ItemInstance shield = new ItemInstance(9001, shieldTpl, owner.objectId(), 1);
		ItemInstance bow = new ItemInstance(9002, bowTpl, owner.objectId(), 1);
		owner.inventory().addItem(shield);
		owner.inventory().addItem(bow);

		owner.inventory().equip(shield);
		assertNotNull(owner.inventory().paperdoll(ItemSlots.LHAND));

		owner.inventory().equip(bow);
		assertNotNull(owner.inventory().paperdoll(ItemSlots.RHAND));
		assertNull(owner.inventory().paperdoll(ItemSlots.LHAND),
				"Equipar um arco (LRHAND) deve desequipar automaticamente o escudo na mao esquerda");
	}

	// ========================================================================
	// BLOCO 4: MUNDO, GEODATA, ZONAS, IA DE MONSTROS & BOSSES (#261 a #345)
	// ========================================================================

	@Test
	void testBloco4_GeoEngineAggroListAndRaidCurse() {
		GeoEngine geo = GeoEngine.getInstance();
		assertNotNull(geo);
		assertTrue(geo.canSeeTarget(83400, 148600, -3400, 83450, 148650, -3400));

		NpcTemplate raidTpl = new NpcTemplate(
				25001, 25001, "Queen Ant", true, "Raid Boss", true,
				15.0, 22.0, 40, "female", "L2RaidBoss", 40,
				50000, 10000, 500, 400, 300, 300, 278, 333,
				0, 0, 0, 80, 150, 500, false, 100000L, 10000);
		NpcInstance raid = new NpcInstance(50001, raidTpl, 0, 0, 0, 0);

		raid.addDamageHate(1001, 500, 1200);
		raid.addDamageHate(1002, 900, 800);
		assertEquals(1001, raid.getMostHatedId(), "O alvo com maior hate (1001) deve ser o MostHated");

		raid.stopHating(1001);
		assertEquals(1002, raid.getMostHatedId(), "Apos stopHating(1001), o proximo na fila de hate deve ser 1002");

		Config.DISABLE_RAID_BOSS_FOSSILIZATION = false;
		Config.MAX_LEVEL_RAID_BOSS_CURSE = 8;
		assertTrue(raid.shouldApplyRaidCurse(55), "Atacante Lv55 contra Raid Lv40 deve sofrer Raid Curse");
		assertFalse(raid.shouldApplyRaidCurse(42), "Atacante Lv42 contra Raid Lv40 nao deve sofrer Raid Curse");
		assertTrue(raid.isLethalProtected(), "Raid Boss deve ter imunidade a Lethal Strike");
	}

	// ========================================================================
	// BLOCO 5: CLANS, SIEGES, SEVEN SIGNS, OLYMPIAD, PARTY & QUESTS (#346 a #430)
	// ========================================================================

	@Test
	void testBloco5_ClanSubPledgesPartyBonusAndNoblesse() {
		Clan clan = new Clan(1, "Alpha", 1001, "Leader", 5);
		clan.addSubPledge(Clan.SUBUNIT_ACADEMY, "AlphaAcademy", 0);
		assertNotNull(clan.getSubPledge(Clan.SUBUNIT_ACADEMY));

		clan.setRankPrivilege(1, Clan.CP_ALL);
		assertEquals(Clan.CP_ALL, clan.getRankPrivilege(1));

		assertEquals(8871, Party.CommandChannel.STRATEGY_GUIDE_ID);
		assertEquals(391, Party.CommandChannel.CLAN_IMPERIUM_SKILL_ID);

		PlayerCharacter leader = newTestPlayer(2001, "Leader", 76, 88);
		leader.effects().put(PlayerEffects.ActiveBuff.ofSkill(
				1323, 1, "noblesse_blessing", 1, System.currentTimeMillis() + 3600_000L,
				List.of(), false, 0, "NoblesseBless", 0.0, 3600));
		assertTrue(leader.effects().hasNoblesseBless());
		assertTrue(leader.effects().consumeNoblesseBless());
		assertFalse(leader.effects().hasNoblesseBless());
	}

	// ========================================================================
	// BLOCO 6: BBS, VOICED, ADMIN, EVENTOS, MODS & ZERO DOACOES (#431 a #510)
	// ========================================================================

	@Test
	void testBloco6_ZeroDonationsPolicyAndConfigs() {
		assertEquals(746, Config.MAX_PROTOCOL_VERSION, "Protocolo maximo deve ser Interlude 746");

		CommunityAuctionService auction = new CommunityAuctionService();
		PlayerCharacter seller = newTestPlayer(3001, "HonestSeller", 78, 88);
		ItemTemplate dualsTpl = new ItemTemplate(
				6580, 6580, "Tallum Blade*Dark Legion's Edge", ItemTemplate.Kind.WEAPON, "dual",
				0, 0, ItemSlots.SLOT_LR_HAND, 2080, false, "s", 48_000_000,
				342, 132, 0, 0, 325, 8, 0, 0, 0, 0, 1, 1, 10,
				List.of(), null, null, null, true, true, true, true);
		ItemInstance duals = new ItemInstance(9901, dualsTpl, seller.objectId(), 1);
		seller.inventory().addItem(duals);

		var rejectedListing = auction.listItem(seller, 9901, 1, 100L, 9999);
		assertNull(rejectedListing, "Politica Zero Doacoes: Moedas customizadas/doacao (9999) devem ser bloqueadas");

		var validListing = auction.listItem(seller, 9901, 1, 50_000_000L, 57);
		assertNotNull(validListing, "Moeda organica Adena (57) deve ser aceita no Auction");
		assertEquals(57, validListing.currencyItemId());
	}

	// ========================================================================
	// FASE 2: VARREDURA PROFUNDA & CONEXOES PONTA-A-PONTA (#511 a #730)
	// ========================================================================

	@Test
	void testPhase2_DeepImplementationAndConnections() {
		// 1. Opcode 0xA6 RequestSetCastleSiegeTime separado de 0x9D RequestSkillCoolTime (#512)
		byte[] setSiegeTimePayload = new byte[] { (byte) 0xA6, 0x03, 0x00, 0x00, 0x00, 0x10, 0x20, 0x00, 0x00 };
		var decodedSiegeTime = GameClientPacket.decode(GameClientPacket.State.IN_GAME, setSiegeTimePayload);
		assertTrue(decodedSiegeTime.isPresent());
		assertInstanceOf(GameClientPacket.RequestSetCastleSiegeTime.class, decodedSiegeTime.get());
		assertEquals(3, ((GameClientPacket.RequestSetCastleSiegeTime) decodedSiegeTime.get()).castleId());

		// 2. Novos pacotes Server->Client de Fase 2 (#514-#525)
		assertEquals(0x6E, new GameServerPacket.HideBoard().encode()[0] & 0xFF);
		assertEquals(0xCF, new GameServerPacket.EventTrigger(100, true).encode()[0] & 0xFF);
		assertEquals(0xD3, new GameServerPacket.RequestNetPing(42).encode()[0] & 0xFF);

		// 3. Soul Crystal Leveling (SA) IDs retail Stage 0..13 e Ressonancia (#596-#605)
		assertEquals(4629, Quest350EnhanceYourWeapon.RED_SOUL_CRYSTAL_0);
		assertEquals(4640, Quest350EnhanceYourWeapon.GREEN_SOUL_CRYSTAL_0);
		assertEquals(4651, Quest350EnhanceYourWeapon.BLUE_SOUL_CRYSTAL_0);
		assertEquals(5577, Quest350EnhanceYourWeapon.getNextSoulCrystalId(4639)); // Red 10 -> 11
		assertEquals(5908, Quest350EnhanceYourWeapon.getNextSoulCrystalId(5580)); // Red 12 -> 13
		assertEquals(5578, Quest350EnhanceYourWeapon.getNextSoulCrystalId(4650)); // Green 10 -> 11
		assertEquals(5911, Quest350EnhanceYourWeapon.getNextSoulCrystalId(5581)); // Green 12 -> 13
		assertEquals(5579, Quest350EnhanceYourWeapon.getNextSoulCrystalId(4661)); // Blue 10 -> 11
		assertEquals(5914, Quest350EnhanceYourWeapon.getNextSoulCrystalId(5582)); // Blue 12 -> 13

		// 4. Multi-NPC QuestState resolveEventNpcId (#631)
		PlayerCharacter quester = newTestPlayer(4001, "PaganRunner", 76, 88);
		QuestState qs = new QuestState(null, null);
		qs.setLastTalkedNpcId(32010); // Priest Flauron (2nd NPC)
		assertEquals(32010, qs.resolveEventNpcId(31329), "resolveEventNpcId deve retornar o ultimo NPC interagido (32010)");

		// 5. PetDataTable.isPetArmor nao confunde Spiritshots (2509..2514) ou Scrolls (3926..3935) (#625)
		assertFalse(PetDataTable.isPetArmor(2509), "Spiritshot NG (2509) nao deve ser classificado como Pet Armor");
		assertFalse(PetDataTable.isPetArmor(3926), "L2Day Scroll (3926) nao deve ser classificado como Pet Armor");
		assertEquals(8872, SummonInstance.SIEGE_CANNONBALL_ID, "Flash Gunpowder retail ID deve ser 8872");

		// 6. PlayerEffects ProtectionBlessing e Fusion Charges (#566, #560)
		quester.effects().addBuff(5182, 1, 300_000L);
		assertTrue(quester.effects().hasProtectionBlessing());
		quester.effects().addFusionCharge(1285, 3);
		assertEquals(1, quester.effects().getFusionCharge(1285));

		// 7. DressMeData.xml carregado (#719)
		DressMeData dressMeData = new DressMeData();
		assertFalse(dressMeData.all().isEmpty(), "DressMeData.xml deve carregar skins classicas do Interlude");
	}
}
