package com.lopez.l2j.game.castle.siege;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de gerenciamento de cercos aos castelos (Siege Engine de Lineage II Interlude).
 * Gerencia inscricoes de atacantes e defensores, artefatos, mid-victory e troca de posse.
 */
@Service
public class SiegeService {

	private static final Logger log = LoggerFactory.getLogger(SiegeService.class);

	public static final int MIN_CLAN_LEVEL = 4;
	public static final int MIN_CLAN_MEMBERS = 15;
	public static final long SIEGE_DURATION_MILLIS = 2 * 60 * 60 * 1000L; // 2 horas
	public static final long SIEGE_CYCLE_MILLIS = 14 * 24 * 60 * 60 * 1000L; // 14 dias

	public static final int CONTROL_TOWER_NPC_ID = 13002;
	public static final int FLAME_TOWER_NPC_ID = 13004;
	public static final int HOLY_ARTIFACT_NPC_ID = 35062;

	public record SiegeSpawn(int castleId, int x, int y, int z, int heading, int npcId, int hp, List<Integer> zoneList) {}

	private final Map<Integer, SiegeRecord> sieges = new ConcurrentHashMap<>();
	private final Map<Integer, List<NpcInstance>> siegeNpcsByCastle = new ConcurrentHashMap<>();
	private final Map<Integer, List<SiegeSpawn>> controlTowerSpawns = new ConcurrentHashMap<>();
	private final Map<Integer, List<SiegeSpawn>> artefactSpawns = new ConcurrentHashMap<>();
	private final Map<Integer, List<SiegeSpawn>> flameTowerSpawns = new ConcurrentHashMap<>();
	private final AtomicInteger siegeNpcIdSeq = new AtomicInteger(1_970_000_000);
	private final CastleManager castleManager;
	private final ClanTable clanTable;
	private final JdbcClient jdbc;

	@Autowired
	public SiegeService(CastleManager castleManager,
						@Autowired(required = false) ClanTable clanTable,
						@Autowired(required = false) JdbcClient jdbc) {
		this.castleManager = castleManager;
		this.clanTable = clanTable;
		this.jdbc = jdbc;
		loadTowerAndArtefactSpawns();
		initSieges();
		loadSiegeClans();
	}

	private void initSieges() {
		for (Castle castle : castleManager.allCastles()) {
			long sDate = castle.siegeDate() > 0 ? castle.siegeDate() : computeNextSiegeDate(System.currentTimeMillis());
			SiegeRecord record = new SiegeRecord(castle.id(), sDate);
			List<SiegeSpawn> cts = controlTowerSpawns.get(castle.id());
			if (cts != null && !cts.isEmpty()) {
				record.controlTowersActive(cts.size());
			}
			List<SiegeSpawn> fts = flameTowerSpawns.get(castle.id());
			if (fts != null && !fts.isEmpty()) {
				record.flameTowersActive(fts.size());
			}
			if (castle.hasOwner()) {
				record.defenders().put(castle.ownerClanId(), new SiegeClan(castle.id(), castle.ownerClanId(), SiegeClan.SiegeClanType.OWNER, true));
			}
			sieges.put(castle.id(), record);
		}
	}

	public long computeNextSiegeDate(long baseMillis) {
		long days = Config.DAY_TO_SIEGE > 0 ? Config.DAY_TO_SIEGE : 14L;
		long next = baseMillis + (days * 24L * 60L * 60L * 1000L);
		if (Config.HOUR_TO_SIEGE >= 0 && Config.HOUR_TO_SIEGE <= 23) {
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTimeInMillis(next);
			cal.set(java.util.Calendar.HOUR_OF_DAY, Config.HOUR_TO_SIEGE);
			cal.set(java.util.Calendar.MINUTE, 0);
			cal.set(java.util.Calendar.SECOND, 0);
			if (Config.CORRECT_DATE_BY_7S && cal.getTimeInMillis() <= baseMillis) {
				cal.add(java.util.Calendar.DAY_OF_MONTH, 7);
			}
			return cal.getTimeInMillis();
		}
		return next;
	}

	public int getCountdownLengthMinutes() {
		return Math.max(0, Config.COUNTDOWN_LENGTH);
	}

	public int getMaxFlagsPerClan() {
		return Math.max(1, Config.MAX_FLAGS);
	}

	public boolean canPlaceFlag(int castleId, int clanId, int currentFlags) {
		if (!isAttacker(castleId, clanId)) {
			return false;
		}
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS) {
			return false;
		}
		return currentFlags < getMaxFlagsPerClan();
	}

	public long getAttackerRespawnDelayMillis() {
		return Math.max(0L, (long) Config.ATTACKER_RESPAWN);
	}

	public boolean isSpawnSiegeGuardEnabled() {
		return Config.SPAWN_SIEGE_GUARD;
	}

	public int getMaxGuardCount() {
		return Math.max(0, Config.MAX_GUARD_COUNT);
	}

	public boolean isAllowedInSiegeZone(int castleId, int clanId) {
		if (!Config.ONLY_REGISTERED) {
			return true;
		}
		return isParticipant(castleId, clanId);
	}

	public boolean isFlyingWyvernAllowedInSiege(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			return Config.ALT_FLYING_WYVERN_IN_SIEGE;
		}
		return true;
	}

	public boolean canControlGatesDuringSiege(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege != null && siege.status() == SiegeStatus.IN_PROGRESS) {
			return Config.ALLOW_GATE_CONTROL;
		}
		return true;
	}

	public List<Integer> getAllowedSiegeHours() {
		List<Integer> hours = new ArrayList<>();
		if (Config.SIEGE_HOUR_LIST != null && !Config.SIEGE_HOUR_LIST.isBlank()) {
			for (String part : Config.SIEGE_HOUR_LIST.split(",")) {
				try {
					hours.add(Integer.parseInt(part.trim()));
				} catch (NumberFormatException ignored) {}
			}
		}
		return hours;
	}

	public boolean changeSiegeTime(int castleId, int requestingClanId, int requestedHour) {
		if (Config.DISABLE_CHANGE_SIEGE_TIME) {
			return false;
		}
		if (Config.CL_SET_SIEGE_TIME_LIST == null || !Config.CL_SET_SIEGE_TIME_LIST.toLowerCase().contains("hour")) {
			return false;
		}
		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle == null || castle.ownerClanId() != requestingClanId) {
			return false;
		}
		List<Integer> allowedHours = getAllowedSiegeHours();
		if (!allowedHours.isEmpty() && !allowedHours.contains(requestedHour)) {
			return false;
		}
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTimeInMillis(siege.siegeDate());
		cal.set(java.util.Calendar.HOUR_OF_DAY, requestedHour);
		cal.set(java.util.Calendar.MINUTE, 0);
		cal.set(java.util.Calendar.SECOND, 0);
		if (Config.CORRECT_DATE_BY_7S && cal.getTimeInMillis() < System.currentTimeMillis()) {
			cal.add(java.util.Calendar.DAY_OF_MONTH, 7);
		}
		siege.siegeDate(cal.getTimeInMillis());
		castle.siegeDate(cal.getTimeInMillis());
		return true;
	}

	public Optional<SiegeRecord> getSiege(int castleId) {
		return Optional.ofNullable(sieges.get(castleId));
	}

	public Collection<SiegeRecord> allSieges() {
		return Collections.unmodifiableCollection(sieges.values());
	}

	public List<SiegeSpawn> getControlTowerSpawns(int castleId) {
		return controlTowerSpawns.getOrDefault(castleId, Collections.emptyList());
	}

	public List<SiegeSpawn> getArtefactSpawns(int castleId) {
		return artefactSpawns.getOrDefault(castleId, Collections.emptyList());
	}

	public List<SiegeSpawn> getFlameTowerSpawns(int castleId) {
		return flameTowerSpawns.getOrDefault(castleId, Collections.emptyList());
	}

	/**
	 * Inscreve um cla como atacante no cerco do castelo.
	 */
	public boolean registerAttacker(int castleId, Clan clan) {
		if (clan == null) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.isRegistrationOver() || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		int minLvl = Config.SIEGE_CLAN_MIN_LEVEL > 0 ? Config.SIEGE_CLAN_MIN_LEVEL : MIN_CLAN_LEVEL;
		int minMembers = Config.SIEGE_CLAN_MIN_MEMBERS_COUNT > 0 ? Config.SIEGE_CLAN_MIN_MEMBERS_COUNT : MIN_CLAN_MEMBERS;
		if (clan.level() < minLvl || clan.membersCount() < minMembers) {
			return false;
		}

		if (Config.ATTACKER_MAX_CLANS > 0 && siege.attackers().size() >= Config.ATTACKER_MAX_CLANS) {
			log.warn("Cerco ao Castelo {}: limite maximo de atacantes ({}) atingido", castleId, Config.ATTACKER_MAX_CLANS);
			return false;
		}

		// Nao pode possuir outro castelo
		if (clan.castleId() > 0) {
			return false;
		}

		// Nao pode atacar o proprio castelo ou da mesma alianca
		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle != null && castle.hasOwner()) {
			Clan ownerClan = clanTable != null ? clanTable.byClanId(castle.ownerClanId()).orElse(null) : null;
			if (ownerClan != null && ownerClan.allyId() > 0 && ownerClan.allyId() == clan.allyId()) {
				return false;
			}
		}

		SiegeClan sc = new SiegeClan(castleId, clan.clanId(), SiegeClan.SiegeClanType.ATTACKER, false);
		siege.attackers().put(clan.clanId(), sc);
		saveSiegeClan(sc);
		log.info("Cla {} ({}) registrado como atacante no Castelo {}", clan.name(), clan.clanId(), castleId);
		return true;
	}

	/**
	 * Inscreve um cla como defensor pendente de aprovacao pelo lorde do castelo.
	 */
	public boolean registerDefender(int castleId, Clan clan) {
		if (clan == null) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.isRegistrationOver() || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle == null || !castle.hasOwner()) {
			return false; // Sem lorde, nao ha quem defenda alem de mercenarios/NPCs
		}

		int minLvl = Config.SIEGE_CLAN_MIN_LEVEL > 0 ? Config.SIEGE_CLAN_MIN_LEVEL : MIN_CLAN_LEVEL;
		if (clan.level() < minLvl) {
			return false;
		}

		if (Config.DEFENDER_MAX_CLANS > 0 && (siege.defenders().size() + siege.waitingDefenders().size()) >= Config.DEFENDER_MAX_CLANS) {
			log.warn("Cerco ao Castelo {}: limite maximo de defensores ({}) atingido", castleId, Config.DEFENDER_MAX_CLANS);
			return false;
		}

		SiegeClan sc = new SiegeClan(castleId, clan.clanId(), SiegeClan.SiegeClanType.DEFENDER_NOT_APPROVED, false);
		siege.waitingDefenders().put(clan.clanId(), sc);
		saveSiegeClan(sc);
		log.info("Cla {} ({}) solicitou inscricao na defesa do Castelo {}", clan.name(), clan.clanId(), castleId);
		return true;
	}

	/**
	 * O lorde do castelo aprova a participacao de um cla defensor.
	 */
	public boolean approveDefender(int castleId, int clanId, int requestingClanId) {
		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle == null || castle.ownerClanId() != requestingClanId) {
			return false;
		}

		SiegeRecord siege = sieges.get(castleId);
		if (siege == null) {
			return false;
		}

		SiegeClan waiting = siege.waitingDefenders().remove(clanId);
		if (waiting != null) {
			waiting.type(SiegeClan.SiegeClanType.DEFENDER);
			siege.defenders().put(clanId, waiting);
			saveSiegeClan(waiting);
			log.info("Lorde aprovou o cla {} como defensor do Castelo {}", clanId, castleId);
			return true;
		}
		return false;
	}

	/**
	 * Inicia a batalha de cerco, materializando Control Towers, Flame Towers e Holy Artifact no GameWorld.
	 */
	public boolean startSiege(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() == SiegeStatus.IN_PROGRESS) {
			return false;
		}

		siege.status(SiegeStatus.IN_PROGRESS);
		siege.registrationOver(true);
		List<SiegeSpawn> cts = controlTowerSpawns.get(castleId);
		siege.controlTowersActive(cts != null && !cts.isEmpty() ? cts.size() : 3);
		List<SiegeSpawn> fts = flameTowerSpawns.get(castleId);
		siege.flameTowersActive(fts != null && !fts.isEmpty() ? fts.size() : 2);

		spawnSiegeStructures(castleId);

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		String castleName = castle != null ? castle.name() : ("Castle #" + castleId);
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
					"The siege of " + castleName + " Castle has begun!"), p -> true);
		}

		log.info("Cerco ao Castelo {} iniciado com sucesso (countdown: {} min)!", castleId, getCountdownLengthMinutes());
		return true;
	}

	/**
	 * Processa a gravacao do artefato / Mid-Victory (Seal of Ruler).
	 * A posse do castelo e transferida temporariamente durante o cerco.
	 */
	public boolean midVictory(int castleId, int newOwnerClanId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null || siege.status() != SiegeStatus.IN_PROGRESS) {
			return false;
		}

		SiegeClan newOwnerSc = siege.attackers().remove(newOwnerClanId);
		if (newOwnerSc == null) {
			return false;
		}

		// Antigos defensores viram atacantes
		for (SiegeClan def : siege.defenders().values()) {
			def.type(SiegeClan.SiegeClanType.ATTACKER);
			def.isCastleOwner(false);
			siege.attackers().put(def.clanId(), def);
			saveSiegeClan(def);
		}
		siege.defenders().clear();

		// Novo cla vira dono e defensor
		newOwnerSc.type(SiegeClan.SiegeClanType.OWNER);
		newOwnerSc.isCastleOwner(true);
		siege.defenders().put(newOwnerClanId, newOwnerSc);
		saveSiegeClan(newOwnerSc);

		castleManager.setOwner(castleId, newOwnerClanId);

		// Restaura as Control Towers para o novo defensor
		List<SiegeSpawn> cts = controlTowerSpawns.get(castleId);
		siege.controlTowersActive(cts != null && !cts.isEmpty() ? cts.size() : 3);
		despawnSiegeStructures(castleId);
		spawnSiegeStructures(castleId);

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			Castle castle = castleManager.getCastleById(castleId).orElse(null);
			String castleName = castle != null ? castle.name() : ("Castle #" + castleId);
			Clan newClan = clanTable != null ? clanTable.byClanId(newOwnerClanId).orElse(null) : null;
			String clanName = newClan != null ? newClan.name() : ("Clan #" + newOwnerClanId);
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
					"Clan " + clanName + " has engraved the Holy Artifact of " + castleName + " Castle (Mid-Victory)!"), p -> true);
		}

		// Se nao sobrou nenhum outro atacante, cerco encerra imediatamente
		if (siege.attackers().isEmpty()) {
			endSiege(castleId, newOwnerClanId);
		}

		log.info("Mid-Victory no Castelo {}: Nova posse temporaria pertence ao Cla {}", castleId, newOwnerClanId);
		return true;
	}

	/**
	 * Conclui o cerco e define o vencedor final.
	 */
	public boolean endSiege(int castleId, int winnerClanId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege == null) {
			return false;
		}

		siege.status(SiegeStatus.FINISHED);
		siege.registrationOver(false);
		despawnSiegeStructures(castleId);

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		if (castle != null && winnerClanId > 0) {
			boolean defended = castle.hasOwner() && castle.ownerClanId() == winnerClanId;
			castleManager.setOwner(castleId, winnerClanId);

			if (clanTable != null) {
				Clan winnerClan = clanTable.byClanId(winnerClanId).orElse(null);
				if (winnerClan != null) {
					clanTable.updateClanReputation(winnerClanId, winnerClan.reputationScore() + 1000);
					if (defended && Config.BLOOD_ALLIANCE_REWARD > 0) {
						log.info("Cerco ao Castelo {}: Defesa bem-sucedida! Cla {} premiado com {} Blood Alliance",
								castleId, winnerClanId, Config.BLOOD_ALLIANCE_REWARD);
					}
					if (Config.REWARD_ID > 0 && Config.REWARD_COUNT > 0) {
						log.info("Cerco ao Castelo {}: Recompensa configurada (Item {} x{}) atribuida ao Cla {}",
								castleId, Config.REWARD_ID, Config.REWARD_COUNT, winnerClanId);
					}
				}
			}
		}

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			String castleName = castle != null ? castle.name() : ("Castle #" + castleId);
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
					"The siege of " + castleName + " Castle has ended!"), p -> true);
		}

		// Reagenda cerco conforme Config.DAY_TO_SIEGE / Config.HOUR_TO_SIEGE
		long nextSiege = computeNextSiegeDate(System.currentTimeMillis());
		siege.siegeDate(nextSiege);
		if (castle != null) {
			castle.siegeDate(nextSiege);
		}

		clearSiegeClans(castleId);
		log.info("Cerco ao Castelo {} finalizado! Vencedor: Cla {}", castleId, winnerClanId);
		return true;
	}

	private void spawnSiegeStructures(int castleId) {
		GameWorld world = GameWorld.getInstance();
		if (world == null) {
			return;
		}

		List<NpcInstance> spawned = new CopyOnWriteArrayList<>();
		List<SiegeSpawn> cts = controlTowerSpawns.getOrDefault(castleId, Collections.emptyList());
		for (SiegeSpawn sp : cts) {
			NpcTemplate ctTpl = NpcTemplate.fallback(sp.npcId(), "Control Tower", "L2ControlTower");
			NpcInstance ct = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), ctTpl, sp.x(), sp.y(), sp.z(), sp.heading());
			world.addNpc(ct);
			world.broadcast(NpcInfo.of(ct), p -> p.character() != null && Math.hypot(p.character().x() - ct.x(), p.character().y() - ct.y()) <= GameWorld.VISIBILITY_RADIUS);
			spawned.add(ct);
		}

		List<SiegeSpawn> fts = flameTowerSpawns.getOrDefault(castleId, Collections.emptyList());
		for (SiegeSpawn sp : fts) {
			NpcTemplate ftTpl = NpcTemplate.fallback(sp.npcId(), "Flame Control Tower", "L2FlameTower");
			NpcInstance ft = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), ftTpl, sp.x(), sp.y(), sp.z(), sp.heading());
			world.addNpc(ft);
			world.broadcast(NpcInfo.of(ft), p -> p.character() != null && Math.hypot(p.character().x() - ft.x(), p.character().y() - ft.y()) <= GameWorld.VISIBILITY_RADIUS);
			spawned.add(ft);
		}

		List<SiegeSpawn> arts = artefactSpawns.getOrDefault(castleId, Collections.emptyList());
		for (SiegeSpawn sp : arts) {
			NpcTemplate artTpl = NpcTemplate.fallback(sp.npcId(), "Holy Artifact", "L2EffectPoint");
			NpcInstance artifact = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), artTpl, sp.x(), sp.y(), sp.z(), sp.heading());
			world.addNpc(artifact);
			world.broadcast(NpcInfo.of(artifact), p -> p.character() != null && Math.hypot(p.character().x() - artifact.x(), p.character().y() - artifact.y()) <= GameWorld.VISIBILITY_RADIUS);
			spawned.add(artifact);
		}

		siegeNpcsByCastle.put(castleId, spawned);
	}

	private void despawnSiegeStructures(int castleId) {
		List<NpcInstance> list = siegeNpcsByCastle.remove(castleId);
		if (list == null || list.isEmpty()) {
			return;
		}
		GameWorld world = GameWorld.getInstance();
		for (NpcInstance npc : list) {
			if (world != null) {
				world.removeNpc(npc);
				world.broadcast(new DeleteObject(npc.objectId()), p -> true);
			}
		}
	}

	public final void loadTowerAndArtefactSpawns() {
		controlTowerSpawns.clear();
		artefactSpawns.clear();
		flameTowerSpawns.clear();

		// 1. Gludio
		registerCastleSpawns(CastleManager.GLUDIO,
				List.of(Config.GLUDIO_CONTROL_TOWER_1, Config.GLUDIO_CONTROL_TOWER_2, Config.GLUDIO_CONTROL_TOWER_3, Config.GLUDIO_CONTROL_TOWER_4),
				List.of(Config.GLUDIO_ARTEFACT_1),
				List.of(Config.GLUDIO_FLAME_TOWER_1, Config.GLUDIO_FLAME_TOWER_2));

		// 2. Dion
		registerCastleSpawns(CastleManager.DION,
				List.of(Config.DION_CONTROL_TOWER_1, Config.DION_CONTROL_TOWER_2, Config.DION_CONTROL_TOWER_3, Config.DION_CONTROL_TOWER_4),
				List.of(Config.DION_ARTEFACT_1),
				List.of(Config.DION_FLAME_TOWER_1, Config.DION_FLAME_TOWER_2));

		// 3. Giran
		registerCastleSpawns(CastleManager.GIRAN,
				List.of(Config.GIRAN_CONTROL_TOWER_1, Config.GIRAN_CONTROL_TOWER_2, Config.GIRAN_CONTROL_TOWER_3, Config.GIRAN_CONTROL_TOWER_4),
				List.of(Config.GIRAN_ARTEFACT_1),
				List.of(Config.GIRAN_FLAME_TOWER_1, Config.GIRAN_FLAME_TOWER_2));

		// 4. Oren
		registerCastleSpawns(CastleManager.OREN,
				List.of(Config.OREN_CONTROL_TOWER_1, Config.OREN_CONTROL_TOWER_2, Config.OREN_CONTROL_TOWER_3, Config.OREN_CONTROL_TOWER_4),
				List.of(Config.OREN_ARTEFACT_1),
				List.of(Config.OREN_FLAME_TOWER_1, Config.OREN_FLAME_TOWER_2));

		// 5. Aden
		registerCastleSpawns(CastleManager.ADEN,
				List.of(Config.ADEN_CONTROL_TOWER_1, Config.ADEN_CONTROL_TOWER_2, Config.ADEN_CONTROL_TOWER_3, Config.ADEN_CONTROL_TOWER_4,
						Config.ADEN_CONTROL_TOWER_5, Config.ADEN_CONTROL_TOWER_6, Config.ADEN_CONTROL_TOWER_7, Config.ADEN_CONTROL_TOWER_8),
				List.of(Config.ADEN_ARTEFACT_1),
				List.of(Config.ADEN_FLAME_TOWER_1, Config.ADEN_FLAME_TOWER_2));

		// 6. Innadril
		registerCastleSpawns(CastleManager.INNADRIL,
				List.of(Config.INNADRIL_CONTROL_TOWER_1, Config.INNADRIL_CONTROL_TOWER_2, Config.INNADRIL_CONTROL_TOWER_3, Config.INNADRIL_CONTROL_TOWER_4),
				List.of(Config.INNADRIL_ARTEFACT_1),
				List.of(Config.INNADRIL_FLAME_TOWER_1, Config.INNADRIL_FLAME_TOWER_2));

		// 7. Goddard
		registerCastleSpawns(CastleManager.GODDARD,
				List.of(Config.GODDARD_CONTROL_TOWER_1, Config.GODDARD_CONTROL_TOWER_2, Config.GODDARD_CONTROL_TOWER_3, Config.GODDARD_CONTROL_TOWER_4),
				List.of(Config.GODDARD_ARTEFACT_1, Config.GODDARD_ARTEFACT_2),
				List.of(Config.GODDARD_FLAME_TOWER_1, Config.GODDARD_FLAME_TOWER_2));

		// 8. Rune
		registerCastleSpawns(CastleManager.RUNE,
				List.of(Config.RUNE_CONTROL_TOWER_1, Config.RUNE_CONTROL_TOWER_2, Config.RUNE_CONTROL_TOWER_3,
						Config.RUNE_CONTROL_TOWER_4, Config.RUNE_CONTROL_TOWER_5, Config.RUNE_CONTROL_TOWER_6),
				List.of(Config.RUNE_ARTEFACT_1),
				List.of(Config.RUNE_FLAME_TOWER_1, Config.RUNE_FLAME_TOWER_2));

		// 9. Schuttgart
		registerCastleSpawns(CastleManager.SCHUTTGART,
				List.of(Config.SCHUTTGART_CONTROL_TOWER_1, Config.SCHUTTGART_CONTROL_TOWER_2, Config.SCHUTTGART_CONTROL_TOWER_3, Config.SCHUTTGART_CONTROL_TOWER_4),
				List.of(Config.SCHUTTGART_ARTEFACT_1, Config.SCHUTTGART_ARTEFACT_2),
				List.of(Config.SCHUTTGART_FLAME_TOWER_1, Config.SCHUTTGART_FLAME_TOWER_2));
	}

	private void registerCastleSpawns(int castleId, List<String> ctConfigs, List<String> artConfigs, List<String> ftConfigs) {
		List<SiegeSpawn> cts = new ArrayList<>();
		for (String raw : ctConfigs) {
			SiegeSpawn sp = parseControlTower(castleId, raw);
			if (sp != null) {
				cts.add(sp);
			}
		}
		controlTowerSpawns.put(castleId, cts);

		List<SiegeSpawn> arts = new ArrayList<>();
		for (String raw : artConfigs) {
			SiegeSpawn sp = parseArtefact(castleId, raw);
			if (sp != null) {
				arts.add(sp);
			}
		}
		artefactSpawns.put(castleId, arts);

		List<SiegeSpawn> fts = new ArrayList<>();
		for (String raw : ftConfigs) {
			SiegeSpawn sp = parseFlameTower(castleId, raw);
			if (sp != null) {
				fts.add(sp);
			}
		}
		flameTowerSpawns.put(castleId, fts);
	}

	private SiegeSpawn parseControlTower(int castleId, String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			String[] parts = raw.trim().split(",");
			if (parts.length < 5) {
				return null;
			}
			int x = Integer.parseInt(parts[0].trim());
			int y = Integer.parseInt(parts[1].trim());
			int z = Integer.parseInt(parts[2].trim());
			int npcId = Integer.parseInt(parts[3].trim());
			int hp = Integer.parseInt(parts[4].trim());
			return new SiegeSpawn(castleId, x, y, z, 0, npcId, hp, Collections.emptyList());
		} catch (Exception e) {
			log.warn("Erro ao parsear ControlTower do castelo {}: {}", castleId, raw);
			return null;
		}
	}

	private SiegeSpawn parseArtefact(int castleId, String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			String[] parts = raw.trim().split(",");
			if (parts.length < 5) {
				return null;
			}
			int x = Integer.parseInt(parts[0].trim());
			int y = Integer.parseInt(parts[1].trim());
			int z = Integer.parseInt(parts[2].trim());
			int heading = Integer.parseInt(parts[3].trim());
			int npcId = Integer.parseInt(parts[4].trim());
			return new SiegeSpawn(castleId, x, y, z, heading, npcId, 0, Collections.emptyList());
		} catch (Exception e) {
			log.warn("Erro ao parsear Artefact do castelo {}: {}", castleId, raw);
			return null;
		}
	}

	private SiegeSpawn parseFlameTower(int castleId, String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			String[] parts = raw.trim().split(",");
			if (parts.length < 4) {
				return null;
			}
			int x = Integer.parseInt(parts[0].trim());
			int y = Integer.parseInt(parts[1].trim());
			int z = Integer.parseInt(parts[2].trim());
			int npcId = Integer.parseInt(parts[3].trim());
			List<Integer> zones = new ArrayList<>();
			for (int i = 4; i < parts.length; i++) {
				zones.add(Integer.parseInt(parts[i].trim()));
			}
			return new SiegeSpawn(castleId, x, y, z, 0, npcId, 0, Collections.unmodifiableList(zones));
		} catch (Exception e) {
			log.warn("Erro ao parsear FlameTower do castelo {}: {}", castleId, raw);
			return null;
		}
	}

	public boolean isAttacker(int castleId, int clanId) {
		SiegeRecord s = sieges.get(castleId);
		return s != null && s.attackers().containsKey(clanId);
	}

	public boolean isDefender(int castleId, int clanId) {
		SiegeRecord s = sieges.get(castleId);
		return s != null && s.defenders().containsKey(clanId);
	}

	public boolean isParticipant(int castleId, int clanId) {
		return isAttacker(castleId, clanId) || isDefender(castleId, clanId);
	}

	private void saveSiegeClan(SiegeClan sc) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					REPLACE INTO siege_clans (castle_id, clan_id, type, castle_owner)
					VALUES (:castleId, :clanId, :type, :owner)
					""")
					.param("castleId", sc.castleId())
					.param("clanId", sc.clanId())
					.param("type", sc.type().id())
					.param("owner", sc.isCastleOwner() ? 1 : 0)
					.update();
		} catch (Exception ex) {
			log.warn("Erro ao salvar participante de cerco na tabela siege_clans: {}", ex.getMessage());
		}
	}

	private void clearSiegeClans(int castleId) {
		SiegeRecord siege = sieges.get(castleId);
		if (siege != null) {
			siege.attackers().clear();
			siege.waitingDefenders().clear();
			// Mantem apenas o dono atual na lista de defensores
			Castle castle = castleManager.getCastleById(castleId).orElse(null);
			siege.defenders().clear();
			if (castle != null && castle.hasOwner()) {
				SiegeClan owner = new SiegeClan(castleId, castle.ownerClanId(), SiegeClan.SiegeClanType.OWNER, true);
				siege.defenders().put(castle.ownerClanId(), owner);
				saveSiegeClan(owner);
			}
		}

		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM siege_clans WHERE castle_id = :cid AND castle_owner = 0")
						.param("cid", castleId)
						.update();
			} catch (Exception ex) {
				log.warn("Erro ao limpar siege_clans do castelo {}: {}", castleId, ex.getMessage());
			}
		}
	}

	private void loadSiegeClans() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT castle_id, clan_id, type, castle_owner
					FROM siege_clans
					""").query().listOfRows();

			for (var row : rows) {
				int castleId = ((Number) row.get("castle_id")).intValue();
				int clanId = ((Number) row.get("clan_id")).intValue();
				int typeId = ((Number) row.get("type")).intValue();
				boolean isOwner = row.get("castle_owner") != null && ((Number) row.get("castle_owner")).intValue() == 1;

				SiegeRecord siege = sieges.get(castleId);
				if (siege != null) {
					SiegeClan sc = new SiegeClan(castleId, clanId, SiegeClan.SiegeClanType.fromId(typeId), isOwner);
					switch (sc.type()) {
						case ATTACKER -> siege.attackers().put(clanId, sc);
						case DEFENDER, OWNER -> siege.defenders().put(clanId, sc);
						case DEFENDER_NOT_APPROVED -> siege.waitingDefenders().put(clanId, sc);
					}
				}
			}
			log.info("SiegeService: participantes de cercos carregados do banco");
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar siege_clans: {}. Mantendo defaults.", ex.getMessage());
		}
	}
}
