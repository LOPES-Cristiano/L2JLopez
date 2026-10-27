package com.lopez.l2j.game.skill.handler;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.siege.SiegeRecord;
import com.lopez.l2j.game.castle.siege.SiegeStatus;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanMember;
import com.lopez.l2j.game.cubic.CubicService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.model.Creature;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.skill.AbnormalEffect;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.Zone;
import com.lopez.l2j.game.zone.ZoneType;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetDelete;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetStatusShow;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Registro centralizado de {@link ISkillHandler} por {@code skillType} do Lineage II Interlude.
 * Desacopla a execução de habilidades especiais (Summons, Siege Summons, Cubics, Recall/Teleport,
 * Seal of Ruler, Siege Flag, Summon Friend, Create Item, Erase, Betray, Strider Siege Assault,
 * Curas, Buffs e Ofensivas) e oferece pontos de entrada unificados ({@link #execute} e {@link #dispatchSpecialSkill}).
 */
@Service
public class SkillHandlerRegistry {

	private static volatile SkillHandlerRegistry instance;

	private static final Set<String> SPECIAL_DISPATCH_TYPES = Set.of(
			"SUMMON",
			"CUBIC",
			"RECALL",
			"TELEPORT",
			"TAKECASTLE",
			"SIEGEFLAG",
			"SUMMON_FRIEND",
			"GATE_CHANT",
			"CLAN_GATE",
			"CREATE_ITEM",
			"SUMMON_TREASURE_KEY",
			"ERASE",
			"BETRAY",
			"STRSIEGEASSAULT",
			"FEED_PET"
	);

	// Tabelas oficiais de NPC IDs por nivel para Servitors multi-nivel (data/xml/stats/skills/*.xml)
	private static final int[] KAT_THE_CAT_NPCS = {
			14111, 14116, 14112, 14117, 14113, 14118, 14114, 14119, 14120,
			14115, 14121, 14122, 14123, 14124, 14125, 14126, 14127, 14128
	};
	private static final int[] MEW_THE_CAT_NPCS = {
			14163, 14159, 14164, 14160, 14165, 14161, 14166, 14162, 14167,
			14168, 14169, 14170, 14171, 14172, 14173, 14174, 14175, 14176
	};
	private static final int[] BOXER_THE_UNICORN_NPCS = {
			14295, 14300, 14296, 14301, 14297, 14302, 14298, 14303, 14304,
			14299, 14305, 14306, 14307, 14308, 14309, 14310, 14311, 14312
	};
	private static final int[] MIRAGE_THE_UNICORN_NPCS = {
			14347, 14343, 14348, 14344, 14349, 14345, 14350, 14346, 14351,
			14352, 14353, 14354, 14355, 14356, 14357, 14358, 14359, 14360
	};
	private static final int[] SHADOW_NPCS = {
			14479, 14484, 14480, 14485, 14481, 14486, 14482, 14487, 14488,
			14483, 14489, 14490, 14491, 14492, 14493, 14494, 14495, 14496
	};
	private static final int[] SILHOUETTE_NPCS = {
			14531, 14527, 14532, 14528, 14533, 14529, 14534, 14530, 14535,
			14536, 14537, 14538, 14539, 14540, 14541, 14542, 14543, 14544
	};

	private final Map<String, ISkillHandler> handlersByType = new ConcurrentHashMap<>();
	private final ObjectIdFactory idFactory;

	@Autowired
	public SkillHandlerRegistry(@Autowired(required = false) ObjectIdFactory idFactory) {
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x38000000);
		registerDefaultHandlers();
		instance = this;
	}

	public SkillHandlerRegistry() {
		this(null);
	}

	public static SkillHandlerRegistry getInstance() {
		if (instance == null) {
			instance = new SkillHandlerRegistry();
		}
		return instance;
	}

	public void register(ISkillHandler handler) {
		if (handler == null || handler.skillTypes() == null) {
			return;
		}
		for (String type : handler.skillTypes()) {
			if (type != null && !type.isBlank()) {
				handlersByType.put(type.toUpperCase(Locale.ROOT), handler);
			}
		}
	}

	public void register(String skillType, ISkillHandler handler) {
		if (skillType != null && !skillType.isBlank() && handler != null) {
			handlersByType.put(skillType.toUpperCase(Locale.ROOT), handler);
		}
	}

	public ISkillHandler getHandler(String skillType) {
		if (skillType == null) {
			return null;
		}
		return handlersByType.get(skillType.toUpperCase(Locale.ROOT));
	}

	public boolean hasHandler(String skillType) {
		return getHandler(skillType) != null;
	}

	public Set<String> registeredTypes() {
		return Collections.unmodifiableSet(handlersByType.keySet());
	}

	public int size() {
		return handlersByType.size();
	}

	/**
	 * Ponto de entrada unificado de execucao por {@code skillType} e sessao (#147, #199, #200).
	 */
	public boolean execute(
			String skillType,
			GameSession caster,
			SkillTemplate skill,
			NpcInstance mainNpc,
			DoorInstance doorTarget,
			GameSession targetPlayer,
			boolean ss,
			boolean sps,
			boolean bss) {
		if (skill != null && CubicService.isCubicSummonSkill(skill.id())) {
			return handleSummonCubic(caster, skill);
		}
		String resolvedType = skillType != null ? skillType : (skill != null ? skill.skillType() : null);
		ISkillHandler handler = getHandler(resolvedType);
		if (handler != null) {
			return handler.useSkill(caster, skill, mainNpc, doorTarget, targetPlayer, ss, sps, bss);
		}
		return false;
	}

	public boolean execute(
			SkillTemplate skill,
			GameSession caster,
			NpcInstance mainNpc,
			DoorInstance doorTarget,
			GameSession targetPlayer,
			boolean ss,
			boolean sps,
			boolean bss) {
		if (skill == null) {
			return false;
		}
		return execute(skill.skillType(), caster, skill, mainNpc, doorTarget, targetPlayer, ss, sps, bss);
	}

	/**
	 * Ponto de entrada unificado para execucao direta entre {@link Creature}s (ex.: Summons, Cubics, testes unitarios).
	 */
	public boolean execute(String skillType, Creature caster, Creature target, SkillTemplate skill) {
		if (caster == null || skill == null) {
			return false;
		}
		String type = (skillType != null ? skillType : skill.skillType()).toUpperCase(Locale.ROOT);
		if (caster instanceof PlayerCharacter pc && (type.equals("CUBIC") || CubicService.isCubicSummonSkill(skill.id()))) {
			return !CubicService.getInstance().summonCubicBySkill(pc, skill.id(), skill.level(), null).isEmpty();
		}
		if (target instanceof SummonInstance sum && (skill.isBuff() || "BUFF".equals(type) || "HOT".equals(type))) {
			return sum.applySkillBuff(skill) > 0;
		}
		if (target instanceof SummonInstance sum && skill.isHeal()) {
			return sum.applyHeal(skill.power()) > 0;
		}
		if (target != null && (skill.isPhysicalDamage() || skill.isMagicDamage())) {
			int dmg = Math.max(1, (int) Math.round(skill.power()));
			if (target instanceof SummonInstance sum) {
				sum.applyDamage(dmg);
				return true;
			}
			if (target instanceof NpcInstance npc) {
				npc.onDamage(caster.objectId(), dmg);
				return true;
			}
			target.currentHp(Math.max(0.0, target.currentHp() - dmg));
			return true;
		}
		return hasHandler(type);
	}

	public boolean execute(SkillTemplate skill, Creature caster, Creature target) {
		if (skill == null) {
			return false;
		}
		return execute(skill.skillType(), caster, target, skill);
	}

	/**
	 * Despacha skills especiais a partir do {@code MagicSkillPacketHandler}.
	 * Retorna {@code true} quando o skill foi integralmente processado por um handler especial,
	 * ou {@code false} para delegar ao pipeline padrao de combate/efeitos do {@code MagicSkillPacketHandler}.
	 */
	public boolean dispatchSpecialSkill(
			GameSession caster,
			SkillTemplate skill,
			NpcInstance mainNpc,
			DoorInstance doorTarget,
			GameSession targetPlayer,
			boolean ss,
			boolean sps,
			boolean bss) {
		if (caster == null || skill == null) {
			return false;
		}
		// 1. Invocacao de Cubics (#180, #255, #256, #257)
		if (CubicService.isCubicSummonSkill(skill.id()) || "CUBIC".equalsIgnoreCase(skill.skillType())) {
			return handleSummonCubic(caster, skill);
		}
		// 2. Skills especificos por ID
		if (skill.id() == 246) {
			return handleTakeCastle(caster, skill, mainNpc);
		}
		if (skill.id() == 247 || skill.id() == 326) {
			return handleSiegeFlag(caster, skill);
		}
		if (skill.id() == 1050 || skill.id() == 1255) {
			return handleRecall(caster, skill, targetPlayer);
		}
		if (skill.id() == 1403 || skill.id() == 1429 || skill.id() == 3632) {
			return handleSummonFriendOrGate(caster, skill, targetPlayer);
		}
		if (skill.id() == 1395) {
			return handleErase(caster, skill, targetPlayer);
		}
		if (skill.id() == 1380) {
			return handleBetray(caster, skill, targetPlayer);
		}
		if (skill.id() == 325) {
			return handleStriderSiegeAssault(caster, skill, doorTarget);
		}
		if (skill.id() == 419 || skill.id() == 1324 || skill.id() == 1387) {
			return handleCreateItem(caster, skill);
		}

		String typeUpper = skill.skillType() != null ? skill.skillType().toUpperCase(Locale.ROOT) : "";
		if (!SPECIAL_DISPATCH_TYPES.contains(typeUpper)) {
			return false;
		}
		ISkillHandler handler = getHandler(typeUpper);
		if (handler != null) {
			return handler.useSkill(caster, skill, mainNpc, doorTarget, targetPlayer, ss, sps, bss);
		}
		return false;
	}

	private void registerDefaultHandlers() {
		// 1. SUMMON (Servitors, 3ª Classe, Necromancer Corpse Summons, Siege Summons)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SUMMON");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSummonServitor(caster, skill, mainNpc);
			}
		});

		// 2. CUBIC (Todos os 9 Cubics Interlude + Mass Cubics)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("CUBIC");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSummonCubic(caster, skill);
			}
		});

		// 3. RECALL / TELEPORT (Return, Party Recall, Mass Teleport)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("RECALL", "TELEPORT");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleRecall(caster, skill, targetPlayer);
			}
		});

		// 4. TAKECASTLE (Seal of Ruler - Skill 246)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("TAKECASTLE");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleTakeCastle(caster, skill, mainNpc);
			}
		});

		// 5. SIEGEFLAG (Build Headquarters - Skills 247, 326)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SIEGEFLAG");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSiegeFlag(caster, skill);
			}
		});

		// 6. SUMMON_FRIEND / GATE_CHANT / CLAN_GATE (Skills 1403, 1429, 3632)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("SUMMON_FRIEND", "GATE_CHANT", "CLAN_GATE");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleSummonFriendOrGate(caster, skill, targetPlayer);
			}
		});

		// 7. CREATE_ITEM / SUMMON_TREASURE_KEY (#148)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("CREATE_ITEM", "SUMMON_TREASURE_KEY");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleCreateItem(caster, skill);
			}
		});

		// 8. ERASE (#154)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("ERASE");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleErase(caster, skill, targetPlayer);
			}
		});

		// 9. BETRAY (#155)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("BETRAY");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleBetray(caster, skill, targetPlayer);
			}
		});

		// 10. STRSIEGEASSAULT (#153)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("STRSIEGEASSAULT");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return handleStriderSiegeAssault(caster, skill, doorTarget);
			}
		});

		// 11. FEED_PET (#235)
		register(new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of("FEED_PET", "BEAST_FEED");
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				if (caster != null && caster.active() != null && caster.active().activeSummon() != null) {
					SummonInstance sum = caster.active().activeSummon();
					sum.currentFed(Math.min(sum.maxFed(), sum.currentFed() + Math.max(50, sum.maxFed() / 4)));
					return true;
				}
				return false;
			}
		});

		// 12. Handlers padrao para todos os demais tipos do catalogo Interlude (#147)
		ISkillHandler standardPipelineHandler = new ISkillHandler() {
			@Override
			public List<String> skillTypes() {
				return List.of(
						"PDAM", "MDAM", "BLOW", "CHARGEDAM", "FATALCOUNTER", "DRAIN", "DEATHLINK",
						"MANADAM", "CPDAM", "DOT", "MDOT", "POISON", "BLEED", "DEBUFF", "STUN",
						"SLEEP", "ROOT", "PARALYZE", "MUTE", "FEAR", "CONFUSION", "CONFUSE_MOB_ONLY",
						"WEAKNESS", "AGGDEBUFF", "AGGDAMAGE", "AGGREDUCE", "AGGREDUCE_CHAR", "AGGREMOVE",
						"CANCEL", "MAGE_BANE", "WARRIOR_BANE", "HEAL", "HEAL_PERCENT", "HOT",
						"COMBATPOINTHEAL", "CPHOT", "MANAHEAL", "MANAHEAL_PERCENT", "RECHARGE",
						"BALANCE_LIFE", "RESURRECT", "BUFF", "CONT", "SPOIL", "SWEEP", "SOW", "HARVEST",
						"DRAIN_SOUL", "UNLOCK", "DELUXE_KEY_UNLOCK", "DWARVEN_CRAFT", "COMMON_CRAFT",
						"FISHING", "PUMPING", "REELING", "SIGNET", "SIGNET_CASTTIME", "FUSION",
						"BUFF_CHARGER", "SHIFT_TARGET", "EXTRACTABLE", "EXTRACTABLE_FISH",
						"CHARGE", "CHARGE_EFFECT",
						"INSTANT_JUMP", "GET_PLAYER", "ZAKEN_MOVE", "CANCEL_DEBUFF",
						"GIVE_SP", "FAKE_DEATH", "DRESSME", "FATAL", "TAKEFORT"
				);
			}

			@Override
			public boolean useSkill(GameSession caster, SkillTemplate skill, NpcInstance mainNpc,
					DoorInstance doorTarget, GameSession targetPlayer, boolean ss, boolean sps, boolean bss) {
				return false;
			}
		};
		register(standardPipelineHandler);
	}

	private boolean handleSummonCubic(GameSession caster, SkillTemplate skill) {
		if (caster == null || caster.active() == null || skill == null) {
			return false;
		}
		PlayerCharacter active = caster.active();
		List<PlayerCharacter> partyMembers = null;
		if (caster.party() != null) {
			partyMembers = new ArrayList<>();
			for (GameSession memberSession : caster.party().members()) {
				if (memberSession != null && memberSession.active() != null) {
					double dist = Math.hypot(memberSession.active().x() - active.x(), memberSession.active().y() - active.y());
					if (dist <= Party.getPartyRange()) {
						partyMembers.add(memberSession.active());
					}
				}
			}
		}
		var spawned = CubicService.getInstance().summonCubicBySkill(active, skill.id(), skill.level(), partyMembers);
		if (!spawned.isEmpty()) {
			caster.sendUserInfoAndBroadcastCharInfo();
			return true;
		}
		return false;
	}

	private boolean handleSummonServitor(GameSession caster, SkillTemplate skill, NpcInstance mainNpc) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null || skill == null) {
			return false;
		}

		// 1. Validacao de Cadaver para Necromancer Summons (1129 Reanimated Man, 1154 Corrupted Man, 1334 Cursed Man) (#252)
		if (isNecromancerCorpseSummon(skill.id()) || "TARGET_CORPSE_MOB".equalsIgnoreCase(skill.target())) {
			if (mainNpc == null || !mainNpc.isDead()) {
				caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
				return true;
			}
			if (ctx.world() != null) {
				ctx.world().removeNpc(mainNpc);
				ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, new DeleteObject(mainNpc.objectId()), true);
			}
		}

		// 2. Validacao de Zona de Cerco para Siege Summons (13 Siege Golem, 299 Wild Hog Cannon, 448 Swoop Cannon) (#254)
		if (isSiegeSummonSkill(skill.id())) {
			if (!isInsideActiveSiegeZone(caster)) {
				caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Siege Summons so podem ser invocados dentro de uma Zona de Cerco ativa."));
				return true;
			}
		}

		// Se ja possuir um summon ativo, remove o anterior
		SummonInstance prev = active.activeSummon();
		if (prev != null) {
			prev.stopAllTasks();
			active.activeSummon(null);
			caster.send(new PetDelete(prev.summonType(), prev.objectId()));
			if (ctx.world() != null) {
				ctx.world().removeSummon(prev);
				ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, new DeleteObject(prev.objectId()), true);
			}
		}

		int npcId = resolveServitorNpcId(skill.id(), skill.level());
		NpcTemplate tpl = null;
		if (ctx.npcTemplates() != null) {
			tpl = ctx.npcTemplates().get(npcId).orElse(null);
		}
		if (tpl == null) {
			tpl = NpcTemplate.fallback(npcId, skill.name() != null && !skill.name().isBlank() ? skill.name() : "Servitor", "L2Servitor");
		}

		int summonObjId = idFactory.nextId();
		int servitorLvl = Math.max(active.level(), tpl.level());
		SummonInstance servitor = new SummonInstance(
				summonObjId,
				active,
				tpl,
				SummonInstance.TYPE_SERVITOR,
				0,
				skill.id(),
				servitorLvl);

		// Inicia tasks de tempo de vida/consumo de cristais (#253) e FollowTask (#248)
		servitor.startLifeTimeTask();
		servitor.startFollowTask();

		active.activeSummon(servitor);
		if (ctx.world() != null) {
			ctx.world().addSummon(servitor);
		}

		caster.send(new PetStatusShow(SummonInstance.TYPE_SERVITOR));
		caster.send(new PetInfo(servitor, 2));
		caster.send(NpcInfo.forSummon(servitor));
		if (ctx.world() != null) {
			ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, NpcInfo.forSummon(servitor), false);
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Servitor " + servitor.name() + " invocado!"));
		return true;
	}

	public static boolean isNecromancerCorpseSummon(int skillId) {
		return skillId == 1129 || skillId == 1154 || skillId == 1334;
	}

	public static boolean isSiegeSummonSkill(int skillId) {
		return skillId == 13 || skillId == 299 || skillId == 448;
	}

	private boolean isInsideActiveSiegeZone(GameSession session) {
		if (session == null || session.active() == null) {
			return false;
		}
		PlayerCharacter p = session.active();
		if (p.isInSiege()) {
			return true;
		}
		GameSession.Context ctx = session.ctx();
		if (ctx != null && ctx.zones() != null) {
			for (Zone z : ctx.zones().getZonesAt(p.x(), p.y(), p.z())) {
				if (z.type() == ZoneType.SIEGE) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean handleCreateItem(GameSession caster, SkillTemplate skill) {
		if (caster == null || caster.active() == null || caster.active().inventory() == null || skill == null) {
			return false;
		}
		int itemId;
		int count;
		switch (skill.id()) {
			case 1324 -> { // Summon CP Potion
				itemId = 5592; // Greater CP Potion
				count = 20;
			}
			case 1387 -> { // Summon Cursed Bones
				itemId = 2508; // Cursed Bone
				count = 20 + skill.level() * 20;
			}
			case 419 -> { // Summon Treasure Key
				int gradeOffset = Math.clamp(skill.level(), 1, 4);
				itemId = 6665 + gradeOffset + ThreadLocalRandom.current().nextInt(2);
				count = 2 + ThreadLocalRandom.current().nextInt(2);
			}
			default -> {
				return false;
			}
		}
		if (caster.ctx() != null && caster.ctx().inventories() != null) {
			caster.ctx().inventories().addItem(caster.active().inventory(), itemId, count, "CreateItemSkill");
		} else {
			caster.active().inventory().addItem(itemId, count);
		}
		return true;
	}

	private boolean handleErase(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		if (caster == null || targetPlayer == null || targetPlayer.active() == null) {
			return false;
		}
		SummonInstance targetSummon = targetPlayer.active().activeSummon();
		if (targetSummon == null || targetSummon.isPet() || targetSummon.isSiegeSummon()) {
			return true;
		}
		if (ThreadLocalRandom.current().nextInt(100) < 60) {
			targetSummon.unsummon();
		}
		return true;
	}

	private boolean handleBetray(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		if (caster == null || targetPlayer == null || targetPlayer.active() == null) {
			return false;
		}
		SummonInstance targetSummon = targetPlayer.active().activeSummon();
		if (targetSummon != null && targetSummon.isServitor()) {
			targetSummon.betray(System.currentTimeMillis() + 30_000L);
			targetSummon.targetObjectId(targetPlayer.active().objectId());
		}
		return true;
	}

	private boolean handleStriderSiegeAssault(GameSession caster, SkillTemplate skill, DoorInstance doorTarget) {
		if (caster == null || caster.active() == null || doorTarget == null) {
			return false;
		}
		if (!caster.active().isMounted()) {
			return true;
		}
		int dmg = Math.max(500, (int) Math.round(skill.power() > 0 ? skill.power() : 1500));
		doorTarget.reduceHp(dmg);
		return true;
	}

	private boolean canTeleportPlayer(GameSession session, boolean isRecallTarget) {
		if (session == null || session.active() == null || session.active().isDead()) {
			return false;
		}
		PlayerCharacter p = session.active();
		if (p.isOlympiadMode() || p.isInJail() || p.isDisabled() || p.isRooted() || p.isAlikeDead() || p.instanceId() > 0) {
			return false;
		}
		if (isRecallTarget && (p.pvpFlag() > 0 || p.karma() > 0)) {
			return false;
		}
		if (!isRecallTarget && !Config.ALT_KARMA_PLAYER_CAN_TELEPORT && p.karma() > 0) {
			return false;
		}
		GameSession.Context ctx = session.ctx();
		if (ctx != null) {
			if (ctx.tvt() != null && ctx.tvt().isParticipant(p.objectId()) && ctx.tvt().getState() == TvtEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.ctf() != null && ctx.ctf().isParticipant(p.objectId()) && ctx.ctf().getState() == CtfEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.dm() != null && ctx.dm().isParticipant(p.objectId()) && ctx.dm().getState() == DmEventService.EventState.RUNNING) {
				return false;
			}
			if (ctx.dimensionalRift() != null && ctx.dimensionalRift().isInRift(p.objectId())) {
				return false;
			}
		}
		return true;
	}

	private boolean isInRestrictedSummonZone(GameSession session) {
		if (session == null || session.active() == null || session.ctx() == null || session.ctx().zones() == null) {
			return false;
		}
		PlayerCharacter p = session.active();
		List<Zone> zones = session.ctx().zones().getZonesAt(p.x(), p.y(), p.z());
		for (Zone z : zones) {
			if (z.type() == ZoneType.BOSS || z.type() == ZoneType.SIEGE || z.type() == ZoneType.ARENA || z.isArena()) {
				return true;
			}
		}
		return false;
	}

	private boolean handleRecall(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null) {
			return false;
		}

		int destX = 82698, destY = 148638, destZ = -3473; // Default Giran
		if (ctx.mapRegions() != null) {
			int[] coords = ctx.mapRegions().getRestartCoordinates(
					active.x(), active.y(), active.z(), active.race(), active.karma() > 0);
			if (coords != null && coords.length >= 3 && (coords[0] != 0 || coords[1] != 0)) {
				destX = coords[0];
				destY = coords[1];
				destZ = coords[2];
			}
		}

		boolean isPartyRecall = "TARGET_PARTY".equalsIgnoreCase(skill.target()) || skill.id() == 1255;
		if (isPartyRecall && caster.party() != null) {
			for (GameSession member : caster.party().members()) {
				if (member != null && canTeleportPlayer(member, member != caster)) {
					double dist = Math.hypot(member.active().x() - active.x(), member.active().y() - active.y());
					if (dist <= Party.getPartyRange()) {
						member.teleportToLocation(destX, destY, destZ, 0);
					}
				}
			}
		} else if (targetPlayer != null && targetPlayer != caster) {
			if (!canTeleportPlayer(targetPlayer, true)) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			targetPlayer.teleportToLocation(destX, destY, destZ, 0);
		} else {
			if (!canTeleportPlayer(caster, false)) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			caster.teleportToLocation(destX, destY, destZ, 0);
		}
		return true;
	}

	private boolean handleSummonFriendOrGate(GameSession caster, SkillTemplate skill, GameSession targetPlayer) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null) {
			return false;
		}
		if (!canTeleportPlayer(caster, false) || isInRestrictedSummonZone(caster) || active.isInSiege()) {
			caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
			return true;
		}

		if (skill.id() == 1429 || "GATE_CHANT".equalsIgnoreCase(skill.skillType())) {
			// Gate Chant (Doomcryer): teleports all eligible party members without consuming crystals
			if (caster.party() == null) {
				caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Gate Chant requer estar em uma Party."));
				return true;
			}
			for (GameSession member : caster.party().members()) {
				if (member != null && member != caster && canTeleportPlayer(member, true) && !isInRestrictedSummonZone(member)) {
					member.teleportToLocation(active.x(), active.y(), active.z(), 0);
				}
			}
			return true;
		}

		if (skill.id() == 3632 || "CLAN_GATE".equalsIgnoreCase(skill.skillType())) {
			// Clan Gate: Clan Leader opens a gate and teleports eligible online clan members
			if (active.clanId() <= 0 || !active.clanLeader()) {
				caster.send(SystemMessage.of(SystemMessage.S1_CANNOT_BE_USED, new SystemMessage.SkillName(skill.id(), skill.level())));
				return true;
			}
			Clan clan = ctx.clans() != null ? ctx.clans().byClanId(active.clanId()).orElse(null) : active.clan();
			if (clan == null || ctx.world() == null) {
				return true;
			}
			active.startAbnormalEffect(AbnormalEffect.MAGIC_CIRCLE);
			caster.sendUserInfoAndBroadcastCharInfo();
			CreatureSay gateNotice = new CreatureSay(0, CreatureSay.CLAN, active.name(),
					"Clan Gate aberto! Membros elegiveis do cla estao sendo invocados.");
			clan.broadcastToOnlineMembers(ctx.world(), gateNotice);
			for (ClanMember cm : clan.members()) {
				if (cm.objectId() == active.objectId()) {
					continue;
				}
				ctx.world().player(cm.objectId()).ifPresent(online -> {
					if (online instanceof GameSession memberSession) {
						if (canTeleportPlayer(memberSession, true)
								&& !memberSession.active().isInCombat()
								&& !isInRestrictedSummonZone(memberSession)) {
							memberSession.teleportToLocation(active.x(), active.y(), active.z(), 0);
						}
					}
				});
			}
			GameSession.autoAttackScheduler().schedule(() -> {
				if (caster.inWorld() && caster.active() == active) {
					active.stopAbnormalEffect(AbnormalEffect.MAGIC_CIRCLE);
					caster.sendUserInfoAndBroadcastCharInfo();
				}
			}, 120_000L, TimeUnit.MILLISECONDS);
			return true;
		}

		// 1403 (Summon Friend)
		if (targetPlayer == null || targetPlayer == caster || targetPlayer.active() == null) {
			caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return true;
		}
		if (caster.party() == null || !caster.party().members().contains(targetPlayer)) {
			caster.send(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return true;
		}
		if (!canTeleportPlayer(targetPlayer, true) || isInRestrictedSummonZone(targetPlayer) || targetPlayer.active().isInCombat()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS", targetPlayer.active().name() + " nao pode ser invocado no momento."));
			return true;
		}
		boolean consumed = targetPlayer.consumeItem(8615, 1) || caster.consumeItem(8615, 1);
		if (!consumed && !Config.getBoolean("FreeSummonFriend", false)) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					targetPlayer.active().name() + " precisa de um Summoning Crystal (8615) para ser invocado."));
			targetPlayer.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					active.name() + " tentou invocar voce, mas voce precisa de um Summoning Crystal (8615)."));
			return true;
		}
		targetPlayer.teleportToLocation(active.x(), active.y(), active.z(), 0);
		return true;
	}

	private boolean handleTakeCastle(GameSession caster, SkillTemplate skill, NpcInstance mainNpc) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null || ctx.sieges() == null) {
			return false;
		}
		if (active.clanId() <= 0 || !active.clanLeader()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Apenas o lider de um cla atacante pode gravar o selo!"));
			return true;
		}
		for (SiegeRecord siege : ctx.sieges().allSieges()) {
			if (siege.status() == SiegeStatus.IN_PROGRESS && ctx.sieges().isAttacker(siege.castleId(), active.clanId())) {
				ctx.sieges().engraveArtifact(siege.castleId(), active.clanId(), 0);
				boolean ok = ctx.sieges().midVictory(siege.castleId(), active.clanId());
				if (ok) {
					caster.send(new CreatureSay(0, CreatureSay.HERO_VOICE, "Siege",
							"O selo foi gravado com sucesso! Seu cla assumiu o controle do Castelo!"));
					if (ctx.world() != null) {
						ctx.world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
								"O cla de " + active.name() + " conquistou o Castelo " + siege.castleId() + " (Mid-Victory)!"));
					}
				}
				return true;
			}
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Seu cla nao esta registrado como atacante em um cerco ativo."));
		return true;
	}

	private boolean handleSiegeFlag(GameSession caster, SkillTemplate skill) {
		PlayerCharacter active = caster.active();
		GameSession.Context ctx = caster.ctx();
		if (active == null || ctx == null || ctx.sieges() == null) {
			return false;
		}
		if (active.clanId() <= 0 || !active.clanLeader()) {
			caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Apenas o lider do cla pode construir um Quartel-General (Headquarters)."));
			return true;
		}
		for (SiegeRecord siege : ctx.sieges().allSieges()) {
			if (siege.status() == SiegeStatus.IN_PROGRESS && ctx.sieges().isAttacker(siege.castleId(), active.clanId())) {
				int flagNpcId = skill.id() == 326 ? 35063 : 35062;
				NpcTemplate flagTpl = ctx.npcTemplates() != null
						? ctx.npcTemplates().get(flagNpcId).orElse(NpcTemplate.fallback(flagNpcId, active.name() + "'s Headquarters", "L2SiegeFlag"))
						: NpcTemplate.fallback(flagNpcId, active.name() + "'s Headquarters", "L2SiegeFlag");
				NpcInstance flag = new NpcInstance(
						idFactory.nextId(),
						flagTpl,
						active.x() + 30,
						active.y() + 30,
						active.z(),
						active.heading());
				flag.instanceId(active.instanceId());
				if (ctx.world() != null) {
					ctx.world().addNpc(flag);
					ctx.world().broadcastAround(caster, GameWorld.VISIBILITY_RADIUS, NpcInfo.of(flag), true);
				}
				caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Quartel-General (Siege Headquarters) construido com sucesso!"));
				return true;
			}
		}
		caster.send(new CreatureSay(0, CreatureSay.ALL, "Siege", "Quartel-General so pode ser construido durante um cerco ativo."));
		return true;
	}

	/**
	 * Resolve o {@code npcId} oficial do Servitor considerando o {@code skillId} e o {@code skillLevel}
	 * (incluindo niveis de Enchant 101..130 e Servitors de 3ª classe) (#250, #251).
	 */
	public static int resolveServitorNpcId(int skillId, int skillLevel) {
		int lvl = Math.max(1, skillLevel);
		boolean isEnchant = lvl > 100;
		int enchIdx = isEnchant ? Math.clamp((lvl % 100) - 1, 0, 29) : 0;

		return switch (skillId) {
			case 13 -> isEnchant ? 14738 + enchIdx : 14737;   // Summon Siege Golem
			case 25 -> isEnchant ? 14672 + enchIdx : 14663 + Math.clamp(lvl - 1, 0, 8);  // Summon Mechanic Golem
			case 283 -> isEnchant ? 14806 + enchIdx : 14799 + Math.clamp(lvl - 1, 0, 6); // Summon Dark Panther
			case 299 -> isEnchant ? 14769 + enchIdx : 14768;  // Summon Wild Hog Cannon
			case 301 -> isEnchant ? 14707 + enchIdx : 14702 + Math.clamp(lvl - 1, 0, 4); // Summon Big Boom
			case 448 -> isEnchant ? 14840 + enchIdx : 14839;  // Summon Swoop Cannon
			case 1111 -> isEnchant ? 14129 + enchIdx : KAT_THE_CAT_NPCS[Math.clamp(lvl - 1, 0, KAT_THE_CAT_NPCS.length - 1)]; // Summon Kat the Cat
			case 1128 -> isEnchant ? 14497 + enchIdx : SHADOW_NPCS[Math.clamp(lvl - 1, 0, SHADOW_NPCS.length - 1)];           // Summon Shadow
			case 1129 -> isEnchant ? 14008 + enchIdx : 14001 + Math.clamp(lvl - 1, 0, 6); // Summon Reanimated Man
			case 1154 -> isEnchant ? 14044 + enchIdx : 14038 + Math.clamp(lvl - 1, 0, 5); // Summon Corrupted Man
			case 1225 -> isEnchant ? 14177 + enchIdx : MEW_THE_CAT_NPCS[Math.clamp(lvl - 1, 0, MEW_THE_CAT_NPCS.length - 1)]; // Summon Mew the Cat
			case 1226 -> isEnchant ? 14313 + enchIdx : BOXER_THE_UNICORN_NPCS[Math.clamp(lvl - 1, 0, BOXER_THE_UNICORN_NPCS.length - 1)]; // Summon Unicorn Boxer
			case 1227 -> isEnchant ? 14361 + enchIdx : MIRAGE_THE_UNICORN_NPCS[Math.clamp(lvl - 1, 0, MIRAGE_THE_UNICORN_NPCS.length - 1)]; // Summon Mirage the Unicorn
			case 1228 -> isEnchant ? 14545 + enchIdx : SILHOUETTE_NPCS[Math.clamp(lvl - 1, 0, SILHOUETTE_NPCS.length - 1)];   // Summon Silhouette
			case 1276 -> isEnchant ? 14221 + enchIdx : 14207 + Math.clamp(lvl - 1, 0, 13); // Summon Kai the Cat
			case 1277 -> isEnchant ? 14405 + enchIdx : 14391 + Math.clamp(lvl - 1, 0, 13); // Summon Merrow the Unicorn
			case 1278 -> isEnchant ? 14589 + enchIdx : 14575 + Math.clamp(lvl - 1, 0, 13); // Summon Soulless
			case 1331 -> isEnchant ? 14265 + enchIdx : 14251 + Math.clamp(lvl - 1, 0, 13); // Summon Feline Queen
			case 1332 -> isEnchant ? 14449 + enchIdx : 14435 + Math.clamp(lvl - 1, 0, 13); // Summon Unicorn Seraphim
			case 1333 -> isEnchant ? 14633 + enchIdx : 14619 + Math.clamp(lvl - 1, 0, 13); // Summon Nightshade
			case 1334 -> isEnchant ? 14081 + enchIdx : 14074 + Math.clamp(lvl - 1, 0, 6);  // Summon Cursed Man
			case 1406 -> 14836; // Summon Feline King
			case 1407 -> 14837; // Summon Magnus the Unicorn
			case 1408 -> 14838; // Summon Spectral Lord
			default -> 14111;
		};
	}
}
