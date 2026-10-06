package com.lopez.l2j.game.instance.dimensionalrift;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico responsavel pelo gerenciamento do Dimensional Rift (Item 88).
 * Suporta os 6 tiers de dificuldade, consumo de Dimensional Fragments (7079),
 * progressao de salas, teleporte de party e sala do Boss (Anakazel).
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

    @Autowired(required = false)
    private ItemTemplateTable itemTable;

    @Autowired(required = false)
    private ObjectIdFactory idFactory = ObjectIdFactory.sequential(9300000);

    private final Random random = new Random();

    // Sessoes ativas mapeadas por PartyLeaderId
    private final Map<Integer, RiftSession> activeSessions = new ConcurrentHashMap<>();
    // Mapeamento rapido de PlayerId -> PartyLeaderId
    private final Map<Integer, Integer> playerToLeaderMap = new ConcurrentHashMap<>();

    public DimensionalRiftService() {
    }

    public DimensionalRiftService(ItemTemplateTable itemTable, ObjectIdFactory idFactory) {
        this.itemTable = itemTable;
        this.idFactory = idFactory;
    }

    /**
     * Tenta iniciar a expedicao da party no Dimensional Rift.
     */
    public synchronized boolean enterRift(PlayerCharacter leader, RiftTier tier, List<PlayerCharacter> members) {
        if (leader == null || tier == null || members == null || members.size() < MIN_PARTY_MEMBERS) {
            log.warn("Dimensional Rift: requisitos de party nao atingidos (min {} membros)", MIN_PARTY_MEMBERS);
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

        // 2. Consumo dos fragmentos
        for (PlayerCharacter member : members) {
            consumeItem(member, DIMENSIONAL_FRAGMENT_ID, cost);
        }

        // 3. Criacao da sessao e escolha da sala inicial (1 a 8)
        List<Integer> memberIds = members.stream().map(PlayerCharacter::objectId).toList();
        RiftSession session = new RiftSession(leaderId, tier, memberIds);
        int initialRoom = 1 + random.nextInt(8); // Salas 1 a 8 inicialmente
        session.setCurrentRoom(initialRoom);
        session.getCompletedRooms().add(initialRoom);
        session.incrementJumps();

        activeSessions.put(leaderId, session);
        for (PlayerCharacter member : members) {
            playerToLeaderMap.put(member.objectId(), leaderId);
        }

        // 4. Teleporte da party para a sala inicial
        int[] coords = ROOM_COORDINATES.get(initialRoom);
        for (PlayerCharacter member : members) {
            member.teleport(coords[0], coords[1], coords[2]);
        }

        log.info("Party de {} entrou no Dimensional Rift [{}] - Sala inicial: {}",
                leader.name(), tier.getName(), initialRoom);
        return true;
    }

    /**
     * Avanca a party para a proxima sala (por salto automatico ou manual).
     */
    public synchronized boolean jumpToNextRoom(int leaderId, List<PlayerCharacter> members) {
        RiftSession session = activeSessions.get(leaderId);
        if (session == null || session.isCompleted()) {
            return false;
        }

        // Se alcancou o numero maximo de saltos, conclui a run e devolve a waiting room
        if (session.getJumpsCount() >= MAX_JUMPS) {
            exitRift(leaderId, members);
            return false;
        }

        // Seleciona proxima sala nao visitada
        List<Integer> availableRooms = new ArrayList<>();
        for (int r = 1; r <= 8; r++) {
            if (!session.getCompletedRooms().contains(r)) {
                availableRooms.add(r);
            }
        }

        // No 4º salto (ultimo), existe chance de ir para a Boss Room (sala 9)
        int nextRoom;
        if (session.getJumpsCount() == (MAX_JUMPS - 1) && !session.getCompletedRooms().contains(9)) {
            nextRoom = 9;
            session.setBossRoom(true);
        } else if (!availableRooms.isEmpty()) {
            nextRoom = availableRooms.get(random.nextInt(availableRooms.size()));
        } else {
            nextRoom = 9;
            session.setBossRoom(true);
        }

        session.setCurrentRoom(nextRoom);
        session.getCompletedRooms().add(nextRoom);
        session.incrementJumps();

        int[] coords = ROOM_COORDINATES.get(nextRoom);
        if (members != null) {
            for (PlayerCharacter member : members) {
                member.teleport(coords[0], coords[1], coords[2]);
            }
        }

        log.info("Dimensional Rift: Party de leaderId {} saltou para Sala {} (Salto {}/{})",
                leaderId, nextRoom, session.getJumpsCount(), MAX_JUMPS);
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
        if (members != null) {
            for (PlayerCharacter member : members) {
                deliverItem(member, ANCIENT_ADENA_ID, rewardAmount);
            }
        }

        log.info("Dimensional Rift: Boss {} derrotado! Concedido {} Ancient Adena para cada membro da party",
                bossNpcId, rewardAmount);
        return true;
    }

    /**
     * Encerra a expedicao e teletransporta todos de volta a Waiting Room.
     */
    public synchronized void exitRift(int leaderId, List<PlayerCharacter> members) {
        RiftSession session = activeSessions.remove(leaderId);
        if (session != null) {
            session.setCompleted(true);
            for (int memberId : session.getMemberIds()) {
                playerToLeaderMap.remove(memberId);
            }
        }

        if (members != null) {
            for (PlayerCharacter member : members) {
                member.teleport(WAITING_ROOM_X, WAITING_ROOM_Y, WAITING_ROOM_Z);
            }
        }
        log.info("Dimensional Rift: Expedicao de leaderId {} concluida. Retornados para Waiting Room.", leaderId);
    }

    private void consumeItem(PlayerCharacter player, int itemId, int count) {
        Inventory inv = player.inventory();
        if (inv == null) {
            return;
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
        if (player == null || player.inventory() == null) {
            return;
        }
        Inventory inv = player.inventory();
        var opt = inv.byItemId(itemId);
        if (opt.isPresent() && opt.get().template().stackable()) {
            opt.get().count(opt.get().count() + count);
        } else {
            ItemTemplate tmpl = resolveTemplate(itemId);
            ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
            inv.add(item);
        }
    }

    private ItemTemplate resolveTemplate(int itemId) {
        if (itemTable != null) {
            var opt = itemTable.get(itemId);
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
