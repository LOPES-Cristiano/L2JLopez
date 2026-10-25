package com.lopez.l2j.game.instance.dimensionalrift;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
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
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servico responsavel pelo gerenciamento do Dimensional Rift (Item 88).
 * Suporta os 6 tiers de dificuldade, consumo de Dimensional Fragments (7079),
 * progressao de salas, teleporte de party, spawn/remocao real de monstros no GameWorld
 * e sala do Boss (Anakazel).
 */
@Service
public class DimensionalRiftService {

    private static final Logger log = LoggerFactory.getLogger(DimensionalRiftService.class);

    public static final int DIMENSIONAL_FRAGMENT_ID = 7079;
    public static final int ANCIENT_ADENA_ID = 5575;
    public static final int MAX_JUMPS = 4;
    public static final int MIN_PARTY_MEMBERS = 2;

    // Coordenadas da Sala de Espera do Dimensional Rift
    public static final int WAITING_ROOM_X = -114790;
    public static final int WAITING_ROOM_Y = -180576;
    public static final int WAITING_ROOM_Z = -6781;

    // Coordenadas das 9 salas por ID (1 a 9)
    private static final Map<Integer, int[]> ROOM_COORDINATES = Map.of(
            1, new int[]{-114944, -184400, -6720},
            2, new int[]{-115200, -182600, -6720},
            3, new int[]{-117200, -182600, -6720},
            4, new int[]{-119200, -182600, -6720},
            5, new int[]{-117200, -184400, -6720},
            6, new int[]{-119200, -184400, -6720},
            7, new int[]{-121200, -182600, -6720},
            8, new int[]{-121200, -184400, -6720},
            9, new int[]{-121200, -186400, -6720} // Boss Room (Anakazel)
    );

    // Monstros invasores (Rift Invaders) por Tier (1 a 6)
    private static final Map<RiftTier, int[]> TIER_MONSTER_IDS = Map.of(
            RiftTier.RECRUIT, new int[]{21654, 21655, 21656, 21657},
            RiftTier.SOLDIER, new int[]{21678, 21679, 21680, 21681},
            RiftTier.OFFICER, new int[]{21702, 21703, 21704, 21705},
            RiftTier.CAPTAIN, new int[]{21726, 21727, 21728, 21729},
            RiftTier.COMMANDER, new int[]{21750, 21751, 21752, 21753},
            RiftTier.HERO, new int[]{21774, 21775, 21776, 21777}
    );

    @Autowired(required = false)
    private ItemTemplateTable itemTable;

    @Autowired(required = false)
    private ObjectIdFactory idFactory = ObjectIdFactory.sequential(9300000);

    @Autowired(required = false)
    private InventoryService inventoryService;

    @Autowired(required = false)
    private GameWorld gameWorld;

    @Autowired(required = false)
    private NpcTemplateTable npcTemplateData;

    private final Random random = new Random();

    // Sessoes ativas mapeadas por PartyLeaderId
    private final Map<Integer, RiftSession> activeSessions = new ConcurrentHashMap<>();
    // Mapeamento rapido de PlayerId -> PartyLeaderId
    private final Map<Integer, Integer> playerToLeaderMap = new ConcurrentHashMap<>();
    // Monstros spawnados por sessao (PartyLeaderId -> lista de NpcInstance)
    private final Map<Integer, List<NpcInstance>> spawnedMonstersByLeader = new ConcurrentHashMap<>();
    // Tarefas de salto automatico por sessao (PartyLeaderId -> ScheduledFuture)
    private final Map<Integer, ScheduledFuture<?>> autoJumpTasks = new ConcurrentHashMap<>();
    // Cache das referencias de PlayerCharacter da party para saltos automaticos
    private final Map<Integer, List<PlayerCharacter>> partyMembersByLeader = new ConcurrentHashMap<>();

    private final ScheduledExecutorService autoJumpScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DimensionalRift-AutoJump");
        t.setDaemon(true);
        return t;
    });

    public DimensionalRiftService() {
    }

    public DimensionalRiftService(ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
        this.itemTable = itemTable;
        if (idFactory != null) {
            this.idFactory = idFactory;
        }
    }

    public DimensionalRiftService(InventoryService inventoryService, GameWorld gameWorld, NpcTemplateTable npcTemplateData) {
        this.inventoryService = inventoryService;
        this.gameWorld = gameWorld;
        this.npcTemplateData = npcTemplateData;
    }

    @Autowired
    public DimensionalRiftService(
            @Autowired(required = false) ItemTemplateTable itemTable,
            @Autowired(required = false) ObjectIdFactory idFactory,
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) GameWorld gameWorld,
            @Autowired(required = false) NpcTemplateTable npcTemplateData) {
        this.itemTable = itemTable;
        if (idFactory != null) {
            this.idFactory = idFactory;
        }
        this.inventoryService = inventoryService;
        this.gameWorld = gameWorld;
        this.npcTemplateData = npcTemplateData;
    }

    @PreDestroy
    public void shutdown() {
        for (ScheduledFuture<?> f : autoJumpTasks.values()) {
            f.cancel(false);
        }
        autoJumpTasks.clear();
        autoJumpScheduler.shutdownNow();
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

    public static int getMinPartyMembers() {
        return Config.RIFT_MIN_PARTY_SIZE > 0 ? Config.RIFT_MIN_PARTY_SIZE : MIN_PARTY_MEMBERS;
    }

    public static int getMaxJumps() {
        return Config.MAX_RIFT_JUMPS > 0 ? Config.MAX_RIFT_JUMPS : MAX_JUMPS;
    }

    public static long getSpawnDelayMillis() {
        return Math.max(0L, Config.RIFT_SPAWN_DELAY);
    }

    /**
     * Calcula o tempo ate o proximo salto automatico de sala (em ms), aplicando
     * Config.AUTO_JUMPS_DELAY_MIN, Config.AUTO_JUMPS_DELAY_MAX e Config.BOSS_ROOM_TIME_MULTIPLY.
     */
    public long computeAutoJumpDelayMillis(boolean isBossRoom) {
        int minDelay = Math.max(1, Config.AUTO_JUMPS_DELAY_MIN);
        int maxDelay = Math.max(minDelay, Config.AUTO_JUMPS_DELAY_MAX);
        long delayMillis = minDelay == maxDelay
                ? minDelay * 1000L
                : (minDelay + random.nextInt(maxDelay - minDelay + 1)) * 1000L;
        if (isBossRoom && Config.BOSS_ROOM_TIME_MULTIPLY > 0f) {
            delayMillis = Math.round(delayMillis * Config.BOSS_ROOM_TIME_MULTIPLY);
        }
        return delayMillis;
    }

    /**
     * Retorna o conjunto de salas atualmente ocupadas por sessoes ativas (para evitar colisao de parties).
     */
    public Set<Integer> getOccupiedRooms() {
        Set<Integer> occupied = new HashSet<>();
        for (RiftSession s : activeSessions.values()) {
            if (!s.isCompleted() && s.getCurrentRoom() > 0) {
                occupied.add(s.getCurrentRoom());
            }
        }
        return occupied;
    }

    private int chooseInitialRoom() {
        Set<Integer> occupied = getOccupiedRooms();
        List<Integer> freeRooms = new ArrayList<>();
        for (int r = 1; r <= 8; r++) {
            if (!occupied.contains(r)) {
                freeRooms.add(r);
            }
        }
        if (!freeRooms.isEmpty()) {
            return freeRooms.get(random.nextInt(freeRooms.size()));
        }
        return 1 + random.nextInt(8);
    }

    private void scheduleAutoJump(int leaderId, boolean isBossRoom) {
        cancelAutoJump(leaderId);
        long delayMs = computeAutoJumpDelayMillis(isBossRoom);
        ScheduledFuture<?> future = autoJumpScheduler.schedule(() -> {
            List<PlayerCharacter> cachedMembers = partyMembersByLeader.get(leaderId);
            autoAdvanceRoom(leaderId, cachedMembers);
        }, delayMs, TimeUnit.MILLISECONDS);
        autoJumpTasks.put(leaderId, future);
    }

    private void cancelAutoJump(int leaderId) {
        ScheduledFuture<?> prev = autoJumpTasks.remove(leaderId);
        if (prev != null) {
            prev.cancel(false);
        }
    }

    /**
     * Tenta iniciar a expedicao da party no Dimensional Rift.
     */
    public synchronized boolean enterRift(PlayerCharacter leader, RiftTier tier, List<PlayerCharacter> members) {
        int minPartySize = getMinPartyMembers();
        if (leader == null || tier == null || members == null || members.size() < minPartySize) {
            log.warn("Dimensional Rift: requisitos de party nao atingidos (min {} membros)", minPartySize);
            return false;
        }

        int leaderId = leader.objectId();

        // Valida se party ou lider ja estao em rift ativo
        if (activeSessions.containsKey(leaderId) || playerToLeaderMap.containsKey(leaderId)) {
            log.warn("Dimensional Rift: lider {} ja esta em uma sessao ativa", leader.name());
            return false;
        }

        int cost = tier.getFragmentCost();

        // 1. Validacao de nivel e posse de fragmentos de todos os membros
        for (PlayerCharacter member : members) {
            if (member.level() < tier.getMinLevel()) {
                log.warn("Dimensional Rift: membro {} nao possui nivel minimo ({}) para tier {}",
                        member.name(), tier.getMinLevel(), tier.getName());
                return false;
            }

            Inventory inv = member.inventory();
            if (inv == null) {
                return false;
            }
            var fragmentOpt = inv.byItemId(DIMENSIONAL_FRAGMENT_ID);
            if (fragmentOpt.isEmpty() || fragmentOpt.get().count() < cost) {
                log.warn("Dimensional Rift: membro {} nao possui fragmentos suficientes (precisa {})",
                        member.name(), cost);
                return false;
            }
        }

        // 2. Consumo dos fragmentos (persistindo via InventoryService se disponivel)
        for (PlayerCharacter member : members) {
            consumeFragments(member, cost);
        }

        // 3. Criacao da sessao e escolha da sala inicial desocupada (1 a 8)
        List<Integer> memberIds = members.stream().map(PlayerCharacter::objectId).toList();
        RiftSession session = new RiftSession(leaderId, tier, memberIds);
        int initialRoom = chooseInitialRoom();
        session.setCurrentRoom(initialRoom);
        session.getCompletedRooms().add(initialRoom);
        session.incrementJumps();

        activeSessions.put(leaderId, session);
        partyMembersByLeader.put(leaderId, new CopyOnWriteArrayList<>(members));
        for (PlayerCharacter member : members) {
            playerToLeaderMap.put(member.objectId(), leaderId);
        }

        // 4. Teleporte da party para a sala inicial e spawn real dos monstros no GameWorld
        int[] coords = ROOM_COORDINATES.get(initialRoom);
        for (PlayerCharacter member : members) {
            teleportMember(member, coords[0], coords[1], coords[2]);
        }
        spawnRoomMonsters(session, coords[0], coords[1], coords[2]);
        scheduleAutoJump(leaderId, session.isBossRoom());

        log.info("Party de {} entrou no Dimensional Rift [{}] - Sala inicial: {}",
                leader.name(), tier.getName(), initialRoom);
        return true;
    }

    /**
     * Avanca automaticamente a party para a proxima sala.
     */
    public synchronized boolean autoAdvanceRoom(int leaderId, List<PlayerCharacter> members) {
        return jumpToNextRoom(leaderId, members);
    }

    /**
     * Avanca a party para a proxima sala (por salto automatico ou manual).
     */
    public synchronized boolean jumpToNextRoom(int leaderId, List<PlayerCharacter> members) {
        RiftSession session = activeSessions.get(leaderId);
        if (session == null || session.isCompleted()) {
            return false;
        }

        int maxJumps = getMaxJumps();

        // Se alcancou o numero maximo de saltos, conclui a run e devolve a waiting room
        if (session.getJumpsCount() >= maxJumps) {
            exitRift(leaderId, members);
            return false;
        }

        // Remove os monstros restantes da sala anterior antes de trocar de sala
        despawnRoomMonsters(leaderId);

        // Seleciona proxima sala nao visitada (preferindo salas nao ocupadas por outras parties)
        Set<Integer> occupied = getOccupiedRooms();
        List<Integer> availableUnoccupied = new ArrayList<>();
        List<Integer> availableRooms = new ArrayList<>();
        for (int r = 1; r <= 8; r++) {
            if (!session.getCompletedRooms().contains(r)) {
                availableRooms.add(r);
                if (!occupied.contains(r)) {
                    availableUnoccupied.add(r);
                }
            }
        }

        // No ultimo salto, existe chance de ir para a Boss Room (sala 9)
        int nextRoom;
        if (session.getJumpsCount() == (maxJumps - 1) && !session.getCompletedRooms().contains(9)) {
            nextRoom = 9;
            session.setBossRoom(true);
        } else if (!availableUnoccupied.isEmpty()) {
            nextRoom = availableUnoccupied.get(random.nextInt(availableUnoccupied.size()));
            session.setBossRoom(false);
        } else if (!availableRooms.isEmpty()) {
            nextRoom = availableRooms.get(random.nextInt(availableRooms.size()));
            session.setBossRoom(false);
        } else {
            nextRoom = 9;
            session.setBossRoom(true);
        }

        session.setCurrentRoom(nextRoom);
        session.getCompletedRooms().add(nextRoom);
        session.incrementJumps();

        int[] coords = ROOM_COORDINATES.get(nextRoom);
        List<PlayerCharacter> effectiveMembers = members != null ? members : partyMembersByLeader.get(leaderId);
        if (effectiveMembers != null) {
            for (PlayerCharacter member : effectiveMembers) {
                teleportMember(member, coords[0], coords[1], coords[2]);
            }
        } else if (gameWorld != null) {
            for (int memberId : session.getMemberIds()) {
                PlayerCharacter member = gameWorld.getPlayer(memberId);
                if (member != null) {
                    teleportMember(member, coords[0], coords[1], coords[2]);
                }
            }
        }

        spawnRoomMonsters(session, coords[0], coords[1], coords[2]);
        scheduleAutoJump(leaderId, session.isBossRoom());

        log.info("Dimensional Rift: Party de leaderId {} saltou para Sala {} (Salto {}/{})",
                leaderId, nextRoom, session.getJumpsCount(), maxJumps);
        return true;
    }

    /**
     * Salto manual solicitado pelo lider via NPC Rift Post (permitido uma vez por sessao).
     */
    public synchronized boolean manualJump(PlayerCharacter leader, List<PlayerCharacter> members) {
        if (leader == null) {
            return false;
        }
        int leaderId = leader.objectId();
        RiftSession session = activeSessions.get(leaderId);
        if (session == null || session.isManualJumpUsed() || session.isCompleted()) {
            return false;
        }

        session.setManualJumpUsed(true);
        return jumpToNextRoom(leaderId, members);
    }

    /**
     * Notificacao de derrota do Boss Anakazel na sala do boss.
     */
    public synchronized boolean onBossDefeated(int leaderId, int bossNpcId, List<PlayerCharacter> members) {
        RiftSession session = activeSessions.get(leaderId);
        if (session == null || !session.isBossRoom()) {
            return false;
        }

        if (session.getTier().getBossNpcId() != bossNpcId) {
            return false;
        }

        // Recompensa a party com Ancient Adena proporcional ao Tier
        int rewardAmount = session.getTier().getId() * 25000;
        List<PlayerCharacter> effectiveMembers = members != null ? members : partyMembersByLeader.get(leaderId);
        if (effectiveMembers != null) {
            for (PlayerCharacter member : effectiveMembers) {
                deliverItem(member, ANCIENT_ADENA_ID, rewardAmount);
            }
        } else if (gameWorld != null) {
            for (int memberId : session.getMemberIds()) {
                PlayerCharacter member = gameWorld.getPlayer(memberId);
                if (member != null) {
                    deliverItem(member, ANCIENT_ADENA_ID, rewardAmount);
                }
            }
        }

        log.info("Dimensional Rift: Boss {} derrotado! Concedido {} Ancient Adena para cada membro da party",
                bossNpcId, rewardAmount);
        return true;
    }

    /**
     * Abandona o Dimensional Rift e retorna o grupo para a Waiting Room.
     */
    public synchronized void leaveRift(int leaderId, List<PlayerCharacter> members) {
        exitRift(leaderId, members);
    }

    /**
     * Abandona o Dimensional Rift a partir de qualquer membro do grupo.
     */
    public synchronized void leaveRift(PlayerCharacter player, List<PlayerCharacter> members) {
        if (player == null) {
            return;
        }
        int leaderId = playerToLeaderMap.getOrDefault(player.objectId(), player.objectId());
        exitRift(leaderId, members);
    }

    /**
     * Encerra a expedicao, remove monstros restantes e teletransporta todos de volta a Waiting Room.
     */
    public synchronized void exitRift(int leaderId, List<PlayerCharacter> members) {
        cancelAutoJump(leaderId);
        despawnRoomMonsters(leaderId);
        List<PlayerCharacter> cachedMembers = partyMembersByLeader.remove(leaderId);

        RiftSession session = activeSessions.remove(leaderId);
        if (session != null) {
            session.setCompleted(true);
            for (int memberId : session.getMemberIds()) {
                playerToLeaderMap.remove(memberId);
            }
        }

        List<PlayerCharacter> effectiveMembers = members != null ? members : cachedMembers;
        if (effectiveMembers != null) {
            for (PlayerCharacter member : effectiveMembers) {
                teleportMember(member, WAITING_ROOM_X, WAITING_ROOM_Y, WAITING_ROOM_Z);
            }
        } else if (session != null && gameWorld != null) {
            for (int memberId : session.getMemberIds()) {
                PlayerCharacter member = gameWorld.getPlayer(memberId);
                if (member != null) {
                    teleportMember(member, WAITING_ROOM_X, WAITING_ROOM_Y, WAITING_ROOM_Z);
                }
            }
        }
        log.info("Dimensional Rift: Expedicao de leaderId {} concluida. Retornados para Waiting Room.", leaderId);
    }

    /**
     * Consome os Dimensional Fragments (7079) de um membro usando InventoryService quando disponivel.
     */
    public void consumeFragments(PlayerCharacter player, int count) {
        consumeItem(player, DIMENSIONAL_FRAGMENT_ID, count);
    }

    private void consumeItem(PlayerCharacter player, int itemId, int count) {
        if (player == null || player.inventory() == null || count <= 0) {
            return;
        }
        Inventory inv = player.inventory();
        if (inventoryService != null) {
            var consumed = inventoryService.consumeItem(inv, itemId, count, "DimensionalRift");
            if (consumed != null) {
                if (gameWorld != null) {
                    gameWorld.sendPacket(player.objectId(),
                            new GameServerPacket.InventoryUpdate(List.of(
                                    GameServerPacket.ItemInfo.of(consumed.item(),
                                            consumed.removed() ? GameServerPacket.ItemInfo.REMOVED : GameServerPacket.ItemInfo.MODIFIED))));
                }
                return;
            }
        }
        var opt = inv.byItemId(itemId);
        if (opt.isPresent()) {
            ItemInstance item = opt.get();
            if (item.count() <= count) {
                inv.remove(item);
            } else {
                item.count(item.count() - count);
            }
        }
    }

    private void deliverItem(PlayerCharacter player, int itemId, int count) {
        if (player == null || player.inventory() == null || count <= 0) {
            return;
        }
        Inventory inv = player.inventory();
        if (inventoryService != null) {
            var added = inventoryService.addItem(inv, itemId, count, "DimensionalRiftReward");
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
            opt.get().count(opt.get().count() + count);
        } else {
            ItemTemplate tmpl = resolveTemplate(itemId);
            ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
            inv.add(item);
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

    private void spawnRoomMonsters(RiftSession session, int centerX, int centerY, int centerZ) {
        if (session == null || gameWorld == null || npcTemplateData == null) {
            return;
        }
        int leaderId = session.getPartyLeaderId();
        List<NpcInstance> roomMonsters = new CopyOnWriteArrayList<>();

        if (session.isBossRoom() || session.getCurrentRoom() == 9) {
            int bossId = session.getTier().getBossNpcId();
            NpcTemplate tpl = npcTemplateData.get(bossId)
                    .orElseGet(() -> NpcTemplate.fallback(bossId, "Anakazel", "L2RaidBoss"));
            NpcInstance boss = new NpcInstance(idFactory.nextId(), tpl, centerX, centerY, centerZ, 0);
            gameWorld.addNpc(boss);
            gameWorld.broadcastAround(centerX, centerY, GameWorld.VISIBILITY_RADIUS, boss.instanceId(),
                    new GameServerPacket.NpcInfo(boss));
            roomMonsters.add(boss);
        } else {
            int[] mobIds = TIER_MONSTER_IDS.getOrDefault(session.getTier(), TIER_MONSTER_IDS.get(RiftTier.RECRUIT));
            int[][] offsets = {{-120, -120}, {120, -120}, {-120, 120}, {120, 120}};
            for (int i = 0; i < mobIds.length; i++) {
                int mobId = mobIds[i];
                NpcTemplate tpl = npcTemplateData.get(mobId)
                        .orElseGet(() -> NpcTemplate.fallback(mobId, "Rift Invader", "L2Monster"));
                int sx = centerX + offsets[i % offsets.length][0];
                int sy = centerY + offsets[i % offsets.length][1];
                NpcInstance mob = new NpcInstance(idFactory.nextId(), tpl, sx, sy, centerZ, 0);
                gameWorld.addNpc(mob);
                gameWorld.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, mob.instanceId(),
                        new GameServerPacket.NpcInfo(mob));
                roomMonsters.add(mob);
            }
        }

        spawnedMonstersByLeader.put(leaderId, roomMonsters);
    }

    private void despawnRoomMonsters(int leaderId) {
        List<NpcInstance> monsters = spawnedMonstersByLeader.remove(leaderId);
        if (monsters == null || monsters.isEmpty()) {
            return;
        }
        for (NpcInstance npc : monsters) {
            npc.deleteMe();
            if (gameWorld != null) {
                gameWorld.removeNpc(npc);
                gameWorld.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, npc.instanceId(),
                        new GameServerPacket.DeleteObject(npc.objectId()));
            }
        }
    }

    public List<NpcInstance> getSpawnedMonsters(int leaderId) {
        List<NpcInstance> list = spawnedMonstersByLeader.get(leaderId);
        return list != null ? Collections.unmodifiableList(list) : List.of();
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
        return ItemTemplate.etc(itemId, itemId, "Rift Reward Item", "quest", "none", 1, "none", 0, false, false, false, false);
    }

    public boolean isInRift(int playerId) {
        return playerToLeaderMap.containsKey(playerId);
    }

    public RiftSession getSessionByLeader(int leaderId) {
        return activeSessions.get(leaderId);
    }

    public RiftSession getSessionByPlayer(int playerId) {
        Integer leaderId = playerToLeaderMap.get(playerId);
        return leaderId != null ? activeSessions.get(leaderId) : null;
    }
}
