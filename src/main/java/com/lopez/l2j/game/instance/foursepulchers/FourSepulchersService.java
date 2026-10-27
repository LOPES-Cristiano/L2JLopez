package com.lopez.l2j.game.instance.foursepulchers;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemRepository;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico dos Quatro Sepulcros (Four Sepulchers de Lineage II Interlude).
 * Gerencia os 4 mausoleus (Conquerors, Emperors, Sages, Judges), chaves de capela,
 * waves de monstros, combate com a Sombra de Halisha e a troca dos 4 cálices pelo passe do Túmulo Imperial.
 */
@Service
public class FourSepulchersService {

	private static final Logger log = LoggerFactory.getLogger(FourSepulchersService.class);

	public static final int SEPULCHER_CONQUERORS = 1;
	public static final int SEPULCHER_EMPERORS = 2;
	public static final int SEPULCHER_SAGES = 3;
	public static final int SEPULCHER_JUDGES = 4;

	// Itens
	public static final int PASS_OF_DARKNESS = 7075;
	public static final int CHAPEL_KEY = 7260;
	public static final int GOBLET_CONQUERORS = 7256; // Goblet of Aethelred
	public static final int GOBLET_EMPERORS = 7257;   // Goblet of Wigglie
	public static final int GOBLET_SAGES = 7258;       // Goblet of Konrad
	public static final int GOBLET_JUDGES = 7259;      // Goblet of Rahman
	public static final int IMPERIAL_TOMB_PASS = 8073; // Antique Brooch / Imperial Tomb Pass

	// NPCs Chefes (Shadow of Halisha)
	public static final int SHADOW_OF_HALISHA_CONQUERORS = 25339;
	public static final int SHADOW_OF_HALISHA_EMPERORS = 25340;
	public static final int SHADOW_OF_HALISHA_SAGES = 25341;
	public static final int SHADOW_OF_HALISHA_JUDGES = 25342;

	// Monstros das waves das salas 1 a 4
	private static final int[] WAVE_MONSTER_IDS = { 18120, 18121, 18122, 18123 };

	// Coordenadas do Lobby de Four Sepulchers (Pilgrim's Temple)
	public static final int LOBBY_X = 178298;
	public static final int LOBBY_Y = -85574;
	public static final int LOBBY_Z = -7216;

	// Coordenadas base dos 4 Mausoléus (salas progridem em +800 no eixo X por stage)
	private static final Map<Integer, int[]> MAUSOLEUM_BASE_COORDS = Map.of(
			SEPULCHER_CONQUERORS, new int[]{181632, -85584, -7216},
			SEPULCHER_EMPERORS, new int[]{179568, -88960, -7216},
			SEPULCHER_SAGES, new int[]{173216, -86144, -7216},
			SEPULCHER_JUDGES, new int[]{175616, -82800, -7216}
	);

	public static final int MIN_LEVEL = 78;
	public static final int MIN_PARTY_MEMBERS = 4;

	public static class SepulcherMausoleum {
		private final int id;
		private final String name;
		private final int bossId;
		private final int gobletId;
		private volatile int stage = 0; // 0=livre, 1..5=salas
		private volatile int lastClearedStage = 0;
		private volatile int leaderId = 0;
		private final List<Integer> players = new CopyOnWriteArrayList<>();
		private final List<PlayerCharacter> partyMembers = new CopyOnWriteArrayList<>();
		private final List<NpcInstance> spawnedMonsters = new CopyOnWriteArrayList<>();

		public SepulcherMausoleum(int id, String name, int bossId, int gobletId) {
			this.id = id;
			this.name = name;
			this.bossId = bossId;
			this.gobletId = gobletId;
		}

		public int id() { return id; }
		public String name() { return name; }
		public int bossId() { return bossId; }
		public int gobletId() { return gobletId; }
		public int stage() { return stage; }
		public void stage(int stage) { this.stage = stage; }
		public int lastClearedStage() { return lastClearedStage; }
		public void lastClearedStage(int lastClearedStage) { this.lastClearedStage = lastClearedStage; }
		public int leaderId() { return leaderId; }
		public void leaderId(int leaderId) { this.leaderId = leaderId; }
		public List<Integer> players() { return players; }
		public List<PlayerCharacter> partyMembers() { return partyMembers; }
		public List<NpcInstance> spawnedMonsters() { return spawnedMonsters; }
		public boolean isInUse() { return stage > 0; }
		public void reset() {
			stage = 0;
			lastClearedStage = 0;
			leaderId = 0;
			players.clear();
			partyMembers.clear();
			spawnedMonsters.clear();
		}
	}

	private final Map<Integer, SepulcherMausoleum> mausoleums = new ConcurrentHashMap<>();
	private volatile SepulcherStatus cycleStatus = SepulcherStatus.ENTRY_TIME;
	private ScheduledExecutorService cycleScheduler;

	@Autowired(required = false)
	private ItemTemplateTable itemTable;

	@Autowired(required = false)
	private ObjectIdFactory idFactory = ObjectIdFactory.sequential(0x60000000);

	@Autowired(required = false)
	private InventoryService inventoryService;

	@Autowired(required = false)
	private GameWorld gameWorld;

	@Autowired(required = false)
	private NpcTemplateTable npcTemplateData;

	@Autowired(required = false)
	private ItemRepository itemRepository;

	public FourSepulchersService() {
		initMausoleums();
	}

	public FourSepulchersService(ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
		this.itemTable = itemTable;
		if (idFactory != null) {
			this.idFactory = idFactory;
		}
		initMausoleums();
	}

	public FourSepulchersService(InventoryService inventoryService, GameWorld gameWorld, NpcTemplateTable npcTemplateData) {
		this.inventoryService = inventoryService;
		this.gameWorld = gameWorld;
		this.npcTemplateData = npcTemplateData;
		initMausoleums();
	}

	@Autowired
	public FourSepulchersService(
			@Autowired(required = false) ItemTemplateTable itemTable,
			@Autowired(required = false) ObjectIdFactory idFactory,
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) GameWorld gameWorld,
			@Autowired(required = false) NpcTemplateTable npcTemplateData,
			@Autowired(required = false) ItemRepository itemRepository) {
		this.itemTable = itemTable;
		if (idFactory != null) {
			this.idFactory = idFactory;
		}
		this.inventoryService = inventoryService;
		this.gameWorld = gameWorld;
		this.npcTemplateData = npcTemplateData;
		this.itemRepository = itemRepository;
		initMausoleums();
	}

	@PostConstruct
	public void init() {
		if (cycleScheduler == null) {
			cycleScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "FourSepulchers-CycleTimer");
				t.setDaemon(true);
				return t;
			});
			cycleScheduler.scheduleAtFixedRate(this::updateCycleFromClock, 60L, 60L, TimeUnit.SECONDS);
		}
	}

	@PreDestroy
	public void shutdown() {
		if (cycleScheduler != null) {
			cycleScheduler.shutdownNow();
			cycleScheduler = null;
		}
	}

	/**
	 * Atualiza o estado do ciclo horario oficial dos Quatro Sepulcros:
	 * Minutos 55..59 = ENTRY_TIME, 00..49 = ATTACK_TIME, 50..54 = COOL_TIME.
	 */
	public void updateCycleFromClock() {
		int minute = LocalTime.now().getMinute();
		SepulcherStatus target;
		if (minute >= 55) {
			target = SepulcherStatus.ENTRY_TIME;
		} else if (minute >= 50) {
			target = SepulcherStatus.COOL_TIME;
		} else {
			target = SepulcherStatus.ATTACK_TIME;
		}
		if (target != this.cycleStatus) {
			setCycleStatus(target);
		}
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public void setGameWorld(GameWorld gameWorld) {
		this.gameWorld = gameWorld;
	}

	public void setNpcTemplateData(NpcTemplateTable npcTemplateData) {
		this.npcTemplateData = npcTemplateData;
	}

	public void setItemRepository(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	private void initMausoleums() {
		mausoleums.put(SEPULCHER_CONQUERORS, new SepulcherMausoleum(SEPULCHER_CONQUERORS, "Conquerors", SHADOW_OF_HALISHA_CONQUERORS, GOBLET_CONQUERORS));
		mausoleums.put(SEPULCHER_EMPERORS, new SepulcherMausoleum(SEPULCHER_EMPERORS, "Emperors", SHADOW_OF_HALISHA_EMPERORS, GOBLET_EMPERORS));
		mausoleums.put(SEPULCHER_SAGES, new SepulcherMausoleum(SEPULCHER_SAGES, "Sages", SHADOW_OF_HALISHA_SAGES, GOBLET_SAGES));
		mausoleums.put(SEPULCHER_JUDGES, new SepulcherMausoleum(SEPULCHER_JUDGES, "Judges", SHADOW_OF_HALISHA_JUDGES, GOBLET_JUDGES));
	}

	public static int getRegistrationMode() {
		return Config.REGISTRATION_MODE;
	}

	public static int getRegistrationTimeMinutes() {
		return Math.max(1, Config.REGISTRATION_TIME);
	}

	public static int getMinPartyCount() {
		return Math.max(1, Config.MIN_PARTY_COUNT);
	}

	public static int getMaxPartyCount() {
		return Math.max(getMinPartyCount(), Config.MAX_PARTY_COUNT);
	}

	public static int getMinPlayerCount() {
		return Math.max(1, Config.MIN_PLAYER_COUNT);
	}

	public static int getMaxPlayerCount() {
		return Math.max(getMinPlayerCount(), Config.MAX_PLAYER_COUNT);
	}

	public static int getTimeLimitMinutes() {
		return Math.max(1, Config.TIME_LIMIT);
	}

	public static long getTimeLimitMillis() {
		return getTimeLimitMinutes() * 60_000L;
	}

	public SepulcherStatus getCycleStatus() {
		return cycleStatus;
	}

	public void setCycleStatus(SepulcherStatus cycleStatus) {
		this.cycleStatus = cycleStatus;
		if (cycleStatus == SepulcherStatus.COOL_TIME) {
			evictAll();
		}
		log.info("Ciclo dos Quatro Sepulcros alterado para: {}", cycleStatus);
	}

	public SepulcherMausoleum getMausoleum(int id) {
		return mausoleums.get(id);
	}

	public boolean canEnter(PlayerCharacter leader, List<PlayerCharacter> party) {
		if (leader == null || leader.level() < MIN_LEVEL) {
			return false;
		}

		if (cycleStatus != SepulcherStatus.ENTRY_TIME) {
			return false;
		}

		int minRequired = Math.max(MIN_PARTY_MEMBERS, Config.MIN_PLAYER_COUNT);
		int maxAllowed = getMaxPlayerCount();
		if (party != null && (party.size() < minRequired || party.size() > maxAllowed)) {
			return false;
		}

		Inventory inv = leader.inventory();
		return inv != null && inv.byItemId(PASS_OF_DARKNESS).isPresent();
	}

	/**
	 * Tenta entrar em um dos 4 mausoleus com o grupo.
	 */
	public synchronized boolean tryEnter(int sepulcherId, PlayerCharacter leader, List<PlayerCharacter> party) {
		return enterSepulcher(sepulcherId, leader, party);
	}

	/**
	 * Entra em um dos 4 mausoleus com o grupo.
	 */
	public synchronized boolean enterSepulcher(int sepulcherId, PlayerCharacter leader, List<PlayerCharacter> party) {
		if (!canEnter(leader, party)) {
			return false;
		}

		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || m.isInUse()) {
			return false;
		}

		// Consome o Pass of Darkness do lider e dos membros da party que o possuirem (persistindo via InventoryService se disponivel)
		consumeItem(leader, PASS_OF_DARKNESS, 1);
		if (party != null) {
			for (PlayerCharacter pc : party) {
				if (pc != null && pc != leader && pc.objectId() != leader.objectId()
						&& pc.inventory() != null && pc.inventory().byItemId(PASS_OF_DARKNESS).isPresent()) {
					consumeItem(pc, PASS_OF_DARKNESS, 1);
				}
			}
		}

		despawnMausoleumMonsters(m);
		m.reset();
		m.leaderId(leader.objectId());
		m.stage(1); // Primeira sala

		List<PlayerCharacter> membersToTeleport = (party != null && !party.isEmpty()) ? party : List.of(leader);
		for (PlayerCharacter pc : membersToTeleport) {
			m.players().add(pc.objectId());
			m.partyMembers().add(pc);
		}

		int[] coords = roomCoordinates(sepulcherId, 1);
		for (PlayerCharacter pc : membersToTeleport) {
			teleportMember(pc, coords[0], coords[1], coords[2]);
		}

		spawnMausoleumStageMonsters(m, 1);

		log.info("{} e grupo entraram no Sepulcro {} ({})", leader.name(), sepulcherId, m.name());
		return true;
	}

	/**
	 * Avanca a wave da sala atual, concedendo a Chapel Key e preparando a proxima sala/boss.
	 */
	public synchronized boolean advanceWave(int sepulcherId, PlayerCharacter leader) {
		return clearRoom(sepulcherId, leader);
	}

	/**
	 * Limpa a sala atual: concede a Chave da Capela (Chapel Key), avanca para a proxima sala,
	 * teleporta o grupo e gera os monstros da nova wave (ou Sombra de Halisha na 5ª sala).
	 */
	public synchronized boolean clearRoom(int sepulcherId, PlayerCharacter leader) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || !m.isInUse() || m.stage() >= 5) {
			return false;
		}

		// Concede a Chapel Key ao lider para abrir o portao
		deliverItem(leader, CHAPEL_KEY, 1);

		int nextStage = m.stage() + 1;
		m.stage(nextStage);
		m.lastClearedStage(nextStage);

		int[] coords = roomCoordinates(sepulcherId, nextStage);
		teleportMausoleumParty(m, leader, coords[0], coords[1], coords[2]);
		spawnMausoleumStageMonsters(m, nextStage);

		log.info("Sepulcro {} ({}) avancou para a Sala {}/5. Chave da Capela concedida a {}",
				sepulcherId, m.name(), m.stage(), leader != null ? leader.name() : "Leader");
		return true;
	}

	/**
	 * Usa a Chapel Key (7260) para abrir a porta da proxima sala, teleportando o grupo e dando spawn na wave/boss.
	 */
	public synchronized boolean openNextRoomDoor(int sepulcherId, PlayerCharacter leader) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || !m.isInUse() || leader == null || leader.inventory() == null) {
			return false;
		}

		if (leader.inventory().byItemId(CHAPEL_KEY).isEmpty()) {
			return false;
		}

		// Consome a Chapel Key
		consumeItem(leader, CHAPEL_KEY, 1);

		// Se clearRoom ainda nao tinha avancado o estagio, avanca agora
		if (m.lastClearedStage() < m.stage() && m.stage() < 5) {
			int nextStage = m.stage() + 1;
			m.stage(nextStage);
			m.lastClearedStage(nextStage);
			spawnMausoleumStageMonsters(m, nextStage);
		} else if (m.spawnedMonsters().isEmpty()) {
			spawnMausoleumStageMonsters(m, m.stage());
		}

		int[] coords = roomCoordinates(sepulcherId, m.stage());
		teleportMausoleumParty(m, leader, coords[0], coords[1], coords[2]);

		log.info("Sepulcro {} ({}): Porta da Sala {} aberta por {} usando Chapel Key.",
				sepulcherId, m.name(), m.stage(), leader.name());
		return true;
	}

	/**
	 * Derrota a Sombra de Halisha na 5ª sala e entrega o Cálice correspondente para toda a party.
	 */
	public synchronized boolean defeatShadowOfHalisha(int sepulcherId, PlayerCharacter leader) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || m.stage() < 5) {
			return false;
		}

		boolean leaderDelivered = false;
		for (PlayerCharacter member : m.partyMembers()) {
			if (member != null) {
				deliverItem(member, m.gobletId(), 1);
				if (leader != null && member.objectId() == leader.objectId()) {
					leaderDelivered = true;
				}
			}
		}
		if (!leaderDelivered && leader != null) {
			deliverItem(leader, m.gobletId(), 1);
		}

		despawnMausoleumMonsters(m);
		m.reset(); // Sepulcro concluido com sucesso
		log.info("Sombra de Halisha derrotada no Sepulcro {}! Calice {} entregue a party de {}",
				sepulcherId, m.gobletId(), leader != null ? leader.name() : "Leader");
		return true;
	}

	/**
	 * Expulsa todos os grupos de todos os mausoléus de volta para o lobby e limpa todos os monstros.
	 */
	public synchronized void evictAll() {
		for (SepulcherMausoleum m : mausoleums.values()) {
			despawnMausoleumMonsters(m);
			if (m.isInUse() || !m.partyMembers().isEmpty() || !m.players().isEmpty()) {
				teleportMausoleumParty(m, null, LOBBY_X, LOBBY_Y, LOBBY_Z);
			}
			m.reset();
		}
		log.info("Four Sepulchers: Todos os jogadores foram teleportados para o lobby e os mausoleus foram resetados.");
	}

	/**
	 * Troca os 4 Cálices conquistados pelo Passe do Túmulo Imperial (Frintezza).
	 */
	public synchronized boolean combineFourGoblets(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return false;
		}

		Inventory inv = player.inventory();
		boolean hasAll = inv.byItemId(GOBLET_CONQUERORS).isPresent()
				&& inv.byItemId(GOBLET_EMPERORS).isPresent()
				&& inv.byItemId(GOBLET_SAGES).isPresent()
				&& inv.byItemId(GOBLET_JUDGES).isPresent();

		if (!hasAll) {
			return false;
		}

		// Consome os 4 calices (persistindo via InventoryService se disponivel)
		consumeItem(player, GOBLET_CONQUERORS, 1);
		consumeItem(player, GOBLET_EMPERORS, 1);
		consumeItem(player, GOBLET_SAGES, 1);
		consumeItem(player, GOBLET_JUDGES, 1);

		// Concede o Passe do Tumulo Imperial
		deliverItem(player, IMPERIAL_TOMB_PASS, 1);
		log.info("{} combinou os 4 Calices e obteve o Passe do Tumulo Imperial (Item {})!",
				player.name(), IMPERIAL_TOMB_PASS);
		return true;
	}

	public boolean consumeItem(PlayerCharacter player, int itemId, int count) {
		if (player == null || player.inventory() == null || count <= 0) {
			return false;
		}
		Inventory inv = player.inventory();
		var opt = inv.byItemId(itemId);
		if (opt.isEmpty() || opt.get().count() < count) {
			return false;
		}
		if (inventoryService != null) {
			var consumed = inventoryService.consumeItem(inv, itemId, count, "FourSepulchers");
			if (consumed != null) {
				if (gameWorld != null) {
					gameWorld.sendPacket(player.objectId(),
							new GameServerPacket.InventoryUpdate(List.of(
									GameServerPacket.ItemInfo.of(consumed.item(),
											consumed.removed() ? GameServerPacket.ItemInfo.REMOVED : GameServerPacket.ItemInfo.MODIFIED))));
				}
				return true;
			}
		}
		ItemInstance item = opt.get();
		if (item.count() <= count) {
			inv.remove(item);
			if (itemRepository != null) {
				itemRepository.delete(item.objectId());
			}
		} else {
			item.count(item.count() - count);
			if (itemRepository != null) {
				itemRepository.update(item);
			}
		}
		return true;
	}

	public void deliverItem(PlayerCharacter player, int itemId, int count) {
		if (player == null || player.inventory() == null || count <= 0) {
			return;
		}
		Inventory inv = player.inventory();
		if (inventoryService != null) {
			var added = inventoryService.addItem(inv, itemId, count, "FourSepulchers");
			if (added != null && added.item() != null) {
				if (gameWorld != null) {
					gameWorld.sendPacket(player.objectId(),
							new GameServerPacket.InventoryUpdate(List.of(
									GameServerPacket.ItemInfo.of(added.item(),
											added.created() ? GameServerPacket.ItemInfo.ADDED : GameServerPacket.ItemInfo.MODIFIED))));
				}
				return;
			}
		}
		var opt = inv.byItemId(itemId);
		if (opt.isPresent() && opt.get().template().stackable()) {
			ItemInstance existing = opt.get();
			existing.count(existing.count() + count);
			if (itemRepository != null) {
				itemRepository.update(existing);
			} else if (inventoryService != null) {
				inventoryService.saveItem(existing);
			}
		} else {
			ItemTemplate tmpl = resolveTemplate(itemId);
			ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
			inv.add(item);
			if (itemRepository != null) {
				itemRepository.insert(item, "FourSepulchers");
			} else if (inventoryService != null) {
				inventoryService.saveItem(item);
			}
		}
	}

	private int[] roomCoordinates(int sepulcherId, int stage) {
		int[] base = MAUSOLEUM_BASE_COORDS.getOrDefault(sepulcherId, new int[]{181632, -85584, -7216});
		int stageOffset = Math.max(0, stage - 1) * 800;
		return new int[]{base[0] + stageOffset, base[1], base[2]};
	}

	private void teleportMausoleumParty(SepulcherMausoleum m, PlayerCharacter leader, int x, int y, int z) {
		if (!m.partyMembers().isEmpty()) {
			for (PlayerCharacter pc : m.partyMembers()) {
				teleportMember(pc, x, y, z);
			}
		} else if (gameWorld != null && !m.players().isEmpty()) {
			for (int playerId : m.players()) {
				PlayerCharacter pc = gameWorld.getPlayer(playerId);
				if (pc != null) {
					teleportMember(pc, x, y, z);
				} else {
					gameWorld.teleportPlayer(playerId, x, y, z, 0);
				}
			}
		} else if (leader != null) {
			teleportMember(leader, x, y, z);
		}
	}

	private void teleportMember(PlayerCharacter member, int x, int y, int z) {
		if (member == null) {
			return;
		}
		member.teleport(x, y, z);
		if (gameWorld != null) {
			gameWorld.teleportPlayer(member, x, y, z, member.instanceId());
			var sessionOpt = gameWorld.player(member.objectId());
			if (sessionOpt.isPresent() && !(sessionOpt.get() instanceof com.lopez.l2j.network.game.GameSession)) {
				sessionOpt.get().send(new GameServerPacket.TeleportToLocation(member.objectId(), x, y, z, member.heading()));
			}
		}
	}

	private void spawnMausoleumStageMonsters(SepulcherMausoleum m, int stage) {
		if (m == null || gameWorld == null || npcTemplateData == null) {
			return;
		}
		despawnMausoleumMonsters(m);

		int[] coords = roomCoordinates(m.id(), stage);
		int centerX = coords[0];
		int centerY = coords[1];
		int centerZ = coords[2];

		if (stage >= 5) {
			// 5ª sala: Boss Sombra de Halisha
			int bossId = m.bossId();
			NpcTemplate tpl = npcTemplateData.get(bossId)
					.orElseGet(() -> NpcTemplate.fallback(bossId, "Shadow of Halisha", "L2RaidBoss"));
			NpcInstance boss = new NpcInstance(idFactory.nextId(), tpl, centerX, centerY, centerZ, 0);
			gameWorld.addNpc(boss);
			gameWorld.broadcastAround(centerX, centerY, GameWorld.VISIBILITY_RADIUS, boss.instanceId(),
					new GameServerPacket.NpcInfo(boss));
			m.spawnedMonsters().add(boss);
		} else {
			// Salas 1 a 4: Wave de monstros do Sepulcro
			int[][] offsets = {{-100, -100}, {100, -100}, {-100, 100}, {100, 100}};
			for (int i = 0; i < WAVE_MONSTER_IDS.length; i++) {
				int mobId = WAVE_MONSTER_IDS[i];
				NpcTemplate tpl = npcTemplateData.get(mobId)
						.orElseGet(() -> NpcTemplate.fallback(mobId, "Sepulcher Guardian", "L2Monster"));
				int sx = centerX + offsets[i % offsets.length][0];
				int sy = centerY + offsets[i % offsets.length][1];
				NpcInstance mob = new NpcInstance(idFactory.nextId(), tpl, sx, sy, centerZ, 0);
				gameWorld.addNpc(mob);
				gameWorld.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, mob.instanceId(),
						new GameServerPacket.NpcInfo(mob));
				m.spawnedMonsters().add(mob);
			}
		}
	}

	private void despawnMausoleumMonsters(SepulcherMausoleum m) {
		if (m == null || m.spawnedMonsters().isEmpty()) {
			return;
		}
		for (NpcInstance npc : m.spawnedMonsters()) {
			npc.deleteMe();
			if (gameWorld != null) {
				gameWorld.removeNpc(npc);
				gameWorld.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npc.instanceId(),
						new GameServerPacket.DeleteObject(npc.objectId()));
			}
		}
		m.spawnedMonsters().clear();
	}

	public List<NpcInstance> getSpawnedMonsters(int sepulcherId) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		return m != null ? Collections.unmodifiableList(m.spawnedMonsters()) : List.of();
	}

	/**
	 * Notifica a morte de um monstro dentro de um dos mausoleus dos Quatro Sepulcros.
	 * Se for a Sombra de Halisha (5ª sala), entrega o Calice correspondente;
	 * se for o ultimo monstro vivo da wave atual (salas 1 a 4), concede a Chapel Key ao lider.
	 */
	public synchronized boolean onMonsterKilled(NpcInstance npc, PlayerCharacter killer) {
		if (npc == null) {
			return false;
		}
		for (SepulcherMausoleum m : mausoleums.values()) {
			if (!m.isInUse() || m.spawnedMonsters().isEmpty()) {
				continue;
			}
			boolean found = false;
			for (NpcInstance spawned : m.spawnedMonsters()) {
				if (spawned.objectId() == npc.objectId()) {
					found = true;
					break;
				}
			}
			if (!found) {
				continue;
			}
			PlayerCharacter leader = resolveMausoleumLeader(m, killer);
			if (m.stage() >= 5 || npc.npcId() == m.bossId()) {
				return defeatShadowOfHalisha(m.id(), leader);
			}
			boolean allDead = true;
			for (NpcInstance spawned : m.spawnedMonsters()) {
				if (spawned.objectId() != npc.objectId() && !spawned.isDead() && !spawned.isDeleted()) {
					allDead = false;
					break;
				}
			}
			if (allDead && leader != null) {
				deliverItem(leader, CHAPEL_KEY, 1);
				m.lastClearedStage(m.stage());
				log.info("Sepulcro {} ({}): Wave da Sala {} concluida! Chapel Key entregue a {}.",
						m.id(), m.name(), m.stage(), leader.name());
				return true;
			}
			return false;
		}
		return false;
	}

	private PlayerCharacter resolveMausoleumLeader(SepulcherMausoleum m, PlayerCharacter fallback) {
		if (m == null) {
			return fallback;
		}
		for (PlayerCharacter pc : m.partyMembers()) {
			if (pc != null && pc.objectId() == m.leaderId()) {
				return pc;
			}
		}
		if (gameWorld != null && m.leaderId() > 0) {
			PlayerCharacter onlineLeader = gameWorld.getPlayer(m.leaderId());
			if (onlineLeader != null) {
				return onlineLeader;
			}
		}
		if (!m.partyMembers().isEmpty()) {
			return m.partyMembers().get(0);
		}
		return fallback;
	}

	private ItemTemplate resolveTemplate(int itemId) {
		if (itemTable != null) {
			var opt = itemTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		if (inventoryService != null && inventoryService.templates() != null) {
			var opt = inventoryService.templates().get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, "Sepulcher Quest Item", "quest", "none", 1, "none", 0, false, false, false, false);
	}
}
