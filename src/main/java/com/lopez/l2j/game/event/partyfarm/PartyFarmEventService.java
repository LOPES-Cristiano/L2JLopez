package com.lopez.l2j.game.event.partyfarm;

import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Motor de Party Farm Agendado (partyfarm.xml).
 * Lê dias da semana e horários de pico, emite aviso global com contagem regressiva
 * e spawna mobs de party em coordenadas configuradas com drop especial de moedas de evento.
 */
@Service
public class PartyFarmEventService {

    private static final Logger log = LoggerFactory.getLogger(PartyFarmEventService.class);

    public record MobSpawnConfig(int npcId, int count, int x, int y, int z) {}

    public enum EventStatus {
        INACTIVE,
        PREPARATION,
        RUNNING
    }

    private GameWorld gameWorld;
    private NpcTemplateTable npcTemplates;
    private ObjectIdFactory idFactory;

    private boolean enabled = true;
    private int durationMinutes = 30;
    private int preparationMinutes = 5;
    private final Set<Integer> activeDays = new HashSet<>(Arrays.asList(1, 3, 5)); // Seg, Qua, Sex
    private final List<String> scheduleTimes = new ArrayList<>(Arrays.asList("10:30", "15:00", "17:20", "21:15"));
    private final List<MobSpawnConfig> spawnConfigs = new CopyOnWriteArrayList<>();
    private final List<Integer> spawnedMonsterObjectIds = new CopyOnWriteArrayList<>();

    private volatile EventStatus status = EventStatus.INACTIVE;
    private String lastTriggeredTime = "";
    private int eventCoinItemId = 6393; // Festival Adena

    public PartyFarmEventService() {
        this(null, null, null);
    }

    public PartyFarmEventService(GameWorld gameWorld, NpcTemplateTable npcTemplates) {
        this(gameWorld, npcTemplates, null);
    }

    @Autowired
    public PartyFarmEventService(
            @Autowired(required = false) GameWorld gameWorld,
            @Autowired(required = false) NpcTemplateTable npcTemplates,
            @Autowired(required = false) ObjectIdFactory idFactory) {
        this.gameWorld = gameWorld;
        this.npcTemplates = npcTemplates;
        this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x57000000);
        // Spawns padrão baseados no partyfarm.xml do Interlude (ex: Primeval Isle / Ketra / Varka)
        spawnConfigs.add(new MobSpawnConfig(1186, 5, -82408, 246784, -3644));
        spawnConfigs.add(new MobSpawnConfig(1186, 3, -82762, 247279, -3573));
    }

    /**
     * Checagem periódica executada pelo agendador de tarefas.
     * Retorna a mensagem de aviso caso um horário tenha sido disparado.
     */
    public synchronized Optional<String> checkSchedule(LocalDateTime now) {
        if (!enabled) {
            return Optional.empty();
        }
        int day = now.getDayOfWeek().getValue() % 7; // 0 = Domingo
        if (!activeDays.contains(day)) {
            return Optional.empty();
        }

        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (scheduleTimes.contains(currentTime) && !currentTime.equals(lastTriggeredTime) && status == EventStatus.INACTIVE) {
            lastTriggeredTime = currentTime;
            status = EventStatus.PREPARATION;
            String msg = "O Party Farm iniciará em " + preparationMinutes + " minutos!";
            log.info("PartyFarm: Horario {} atingido! Preparacao iniciada.", currentTime);
            return Optional.of(msg);
        }
        return Optional.empty();
    }

    public synchronized void startEvent() {
        status = EventStatus.RUNNING;
        spawnedMonsterObjectIds.clear();

        GameWorld world = this.gameWorld != null ? this.gameWorld : GameWorld.getInstance();
        if (world != null) {
            for (MobSpawnConfig cfg : spawnConfigs) {
                NpcTemplate tmpl = npcTemplates != null ? npcTemplates.get(cfg.npcId()).orElse(null) : null;
                if (tmpl == null) {
                    tmpl = NpcTemplate.fallback(cfg.npcId(), "PartyFarm Guardian", "L2Monster");
                }
                int count = Math.max(1, cfg.count());
                for (int i = 0; i < count; i++) {
                    int sx = cfg.x();
                    int sy = cfg.y();
                    if (i > 0) {
                        double angle = (2.0 * Math.PI * i) / count;
                        int dist = 45 + (i * 20);
                        sx += (int) Math.round(Math.cos(angle) * dist);
                        sy += (int) Math.round(Math.sin(angle) * dist);
                    }
                    int objId = idFactory.nextId();
                    NpcInstance monster = new NpcInstance(objId, tmpl, sx, sy, cfg.z(), 0);
                    world.addNpc(monster);
                    spawnedMonsterObjectIds.add(objId);

                    NpcInfo info = new NpcInfo(monster);
                    world.broadcastAround(sx, sy, GameWorld.VISIBILITY_RADIUS, info);
                }
            }
            world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "PartyFarm",
                    "O evento Party Farm começou! Monstros especiais foram invocados no mundo!"));
        }

        log.info("PartyFarm: Evento iniciado! {} grupos de mobs de party ativos ({} monstros invocados no mundo).",
                spawnConfigs.size(), spawnedMonsterObjectIds.size());
    }

    public synchronized void endEvent() {
        status = EventStatus.INACTIVE;

        GameWorld world = this.gameWorld != null ? this.gameWorld : GameWorld.getInstance();
        int removedCount = 0;
        if (world != null && !spawnedMonsterObjectIds.isEmpty()) {
            for (int objId : spawnedMonsterObjectIds) {
                Optional<NpcInstance> npcOpt = world.npc(objId);
                if (npcOpt.isPresent()) {
                    NpcInstance npc = npcOpt.get();
                    world.removeNpc(npc);
                    world.broadcastAround(npc.x(), npc.y(), GameWorld.VISIBILITY_RADIUS, new DeleteObject(objId));
                    removedCount++;
                }
            }
            world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "PartyFarm",
                    "O evento Party Farm foi encerrado! Os monstros especiais desapareceram."));
        }
        spawnedMonsterObjectIds.clear();

        log.info("PartyFarm: Evento finalizado. {} mobs removidos do mundo.", removedCount);
    }

    public boolean isPartyFarmMonster(int npcObjectId) {
        return spawnedMonsterObjectIds.contains(npcObjectId);
    }

    public List<Integer> getSpawnedMonsterObjectIds() {
        return Collections.unmodifiableList(spawnedMonsterObjectIds);
    }

    // Gerenciamento de spawns e configurações
    public void addSpawn(int npcId, int count, int x, int y, int z) {
        spawnConfigs.add(new MobSpawnConfig(npcId, count, x, y, z));
    }

    public void clearSpawns() {
        spawnConfigs.clear();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public EventStatus getStatus() { return status; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getPreparationMinutes() { return preparationMinutes; }
    public void setPreparationMinutes(int preparationMinutes) { this.preparationMinutes = preparationMinutes; }
    public Set<Integer> getActiveDays() { return Collections.unmodifiableSet(activeDays); }
    public List<String> getScheduleTimes() { return Collections.unmodifiableList(scheduleTimes); }
    public List<MobSpawnConfig> getSpawnConfigs() { return Collections.unmodifiableList(spawnConfigs); }
    public int getEventCoinItemId() { return eventCoinItemId; }
    public void setEventCoinItemId(int eventCoinItemId) { this.eventCoinItemId = eventCoinItemId; }
    public void setGameWorld(GameWorld gameWorld) { this.gameWorld = gameWorld; }
    public void setNpcTemplates(NpcTemplateTable npcTemplates) { this.npcTemplates = npcTemplates; }
}
