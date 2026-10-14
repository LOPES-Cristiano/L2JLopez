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

	private final Map<Integer, SiegeRecord> sieges = new ConcurrentHashMap<>();
	private final Map<Integer, List<NpcInstance>> siegeNpcsByCastle = new ConcurrentHashMap<>();
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
		initSieges();
		loadSiegeClans();
	}

	private void initSieges() {
		for (Castle castle : castleManager.allCastles()) {
			long sDate = castle.siegeDate() > 0 ? castle.siegeDate() : System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
			SiegeRecord record = new SiegeRecord(castle.id(), sDate);
			if (castle.hasOwner()) {
				record.defenders().put(castle.ownerClanId(), new SiegeClan(castle.id(), castle.ownerClanId(), SiegeClan.SiegeClanType.OWNER, true));
			}
			sieges.put(castle.id(), record);
		}
	}

	public Optional<SiegeRecord> getSiege(int castleId) {
		return Optional.ofNullable(sieges.get(castleId));
	}

	public Collection<SiegeRecord> allSieges() {
		return Collections.unmodifiableCollection(sieges.values());
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
		siege.controlTowersActive(3);
		siege.flameTowersActive(2);

		spawnSiegeStructures(castleId);

		Castle castle = castleManager.getCastleById(castleId).orElse(null);
		String castleName = castle != null ? castle.name() : ("Castle #" + castleId);
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
					"The siege of " + castleName + " Castle has begun!"), p -> true);
		}

		log.info("Cerco ao Castelo {} iniciado com sucesso!", castleId);
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
		siege.controlTowersActive(3);
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
				}
			}
		}

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			String castleName = castle != null ? castle.name() : ("Castle #" + castleId);
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Siege",
					"The siege of " + castleName + " Castle has ended!"), p -> true);
		}

		// Reagenda cerco para 14 dias no futuro
		long nextSiege = System.currentTimeMillis() + SIEGE_CYCLE_MILLIS;
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
		int[] baseCoords = castleBaseCoords(castleId);
		int bx = baseCoords[0];
		int by = baseCoords[1];
		int bz = baseCoords[2];

		List<NpcInstance> spawned = new CopyOnWriteArrayList<>();
		// 3 Control Towers
		NpcTemplate ctTpl = NpcTemplate.fallback(CONTROL_TOWER_NPC_ID, "Control Tower", "L2ControlTower");
		for (int i = 0; i < 3; i++) {
			NpcInstance ct = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), ctTpl, bx + (i - 1) * 250, by + 200, bz, 0);
			world.addNpc(ct);
			world.broadcast(NpcInfo.of(ct), p -> p.character() != null && Math.hypot(p.character().x() - ct.x(), p.character().y() - ct.y()) <= GameWorld.VISIBILITY_RADIUS);
			spawned.add(ct);
		}

		// 2 Flame Towers
		NpcTemplate ftTpl = NpcTemplate.fallback(FLAME_TOWER_NPC_ID, "Flame Control Tower", "L2FlameTower");
		for (int i = 0; i < 2; i++) {
			NpcInstance ft = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), ftTpl, bx + (i == 0 ? -180 : 180), by - 150, bz, 0);
			world.addNpc(ft);
			world.broadcast(NpcInfo.of(ft), p -> p.character() != null && Math.hypot(p.character().x() - ft.x(), p.character().y() - ft.y()) <= GameWorld.VISIBILITY_RADIUS);
			spawned.add(ft);
		}

		// 1 Holy Artifact
		NpcTemplate artTpl = NpcTemplate.fallback(HOLY_ARTIFACT_NPC_ID, "Holy Artifact", "L2EffectPoint");
		NpcInstance artifact = new NpcInstance(siegeNpcIdSeq.incrementAndGet(), artTpl, bx, by, bz, 0);
		world.addNpc(artifact);
		world.broadcast(NpcInfo.of(artifact), p -> p.character() != null && Math.hypot(p.character().x() - artifact.x(), p.character().y() - artifact.y()) <= GameWorld.VISIBILITY_RADIUS);
		spawned.add(artifact);

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

	private int[] castleBaseCoords(int castleId) {
		return switch (castleId) {
			case 1 -> new int[]{-18100, 109400, -2490}; // Gludio
			case 2 -> new int[]{22070, 160350, -2690};  // Dion
			case 3 -> new int[]{116500, 145100, -2560}; // Giran
			case 4 -> new int[]{82600, 37100, -2280};   // Oren
			case 5 -> new int[]{147450, 4650, -340};    // Aden
			case 6 -> new int[]{116000, 249100, -780};  // Innadril
			case 7 -> new int[]{147400, -48500, -2270}; // Goddard
			case 8 -> new int[]{14300, -49150, -530};   // Rune
			case 9 -> new int[]{77550, -152100, -540};  // Schuttgart
			default -> new int[]{116500, 145100, -2560};
		};
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
