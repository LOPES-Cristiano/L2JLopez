package com.lopez.l2j.game.event.partyfarm;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Evento Agendado de Party Farm (PartyFarmEvent).
 * Lê os dias da semana ativos (PartyFarmEventByDayOfWeek), horários agendados (StartPartyFarmEvent),
 * mensagem de anúncio prévio (PartyFarmMessageText) e gerencia o ciclo de spawn e despawn
 * dos monstros de Party Farm no mundo real (GameWorld).
 */
@Service
public class PartyFarmEventService {

    private static final Logger log = LoggerFactory.getLogger(PartyFarmEventService.class);

    public enum EventStatus {
        INACTIVE,
        PREPARATION,
        RUNNING
    }

    public static class PartyFarmSpawnConfig {
        private final int npcId;
        private final int count;
        private final int x;
        private final int y;
        private final int z;

        public PartyFarmSpawnConfig(int npcId, int count, int x, int y, int z) {
            this.npcId = npcId;
            this.count = count;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getNpcId() { return npcId; }
        public int getCount() { return count; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
    }

    private boolean enabled = Config.PARTY_FARM_ENABLED;
    private Set<Integer> activeDaysOfWeek = new HashSet<>(Config.PARTY_FARM_DAYS);
    private List<LocalTime> scheduledTimes = parseTimes(Config.PARTY_FARM_TIMES);
    private int durationMinutes = Config.PARTY_FARM_DURATION;
    private int preparationMinutes = Config.PARTY_FARM_PREP_MINUTES;
    private String announcementMessage = Config.PARTY_FARM_MESSAGE;

    private volatile EventStatus status = EventStatus.INACTIVE;
    private LocalDateTime currentStartTime;
    private LocalDateTime currentEndTime;

    private final List<PartyFarmSpawnConfig> spawnConfigs = new CopyOnWriteArrayList<>();
    private final List<Integer> spawnedMonsterObjectIds = new CopyOnWriteArrayList<>();
    private final AtomicInteger idSeq = new AtomicInteger(0x61000000);

    private GameWorld gameWorld;
    private NpcTemplateTable npcTemplates;
    private ScheduledExecutorService scheduler;

    public PartyFarmEventService() {
        loadSpawnConfigs();
    }

    @Autowired
    public PartyFarmEventService(
            @Autowired(required = false) GameWorld gameWorld,
            @Autowired(required = false) NpcTemplateTable npcTemplates) {
        this.gameWorld = gameWorld;
        this.npcTemplates = npcTemplates;
        loadSpawnConfigs();
        if (this.enabled) {
            startScheduler();
        }
    }

    private void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PartyFarm-Scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                checkSchedule(now);
                if (status == EventStatus.PREPARATION && currentStartTime != null && !now.isBefore(currentStartTime)) {
                    startEvent();
                } else if (status == EventStatus.RUNNING && currentEndTime != null && !now.isBefore(currentEndTime)) {
                    endEvent();
                }
            } catch (Exception e) {
                log.warn("PartyFarm scheduler error: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void loadSpawnConfigs() {
        Path xmlPath = Path.of("data", "xml", "events", "partyfarm.xml");
        if (!Files.exists(xmlPath)) {
            xmlPath = Path.of("data", "partyfarm.xml");
        }
        if (Files.exists(xmlPath)) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlPath.toFile());
                NodeList nodes = doc.getElementsByTagName("spawn");
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (nodes.item(i) instanceof Element el) {
                        int npcId = Integer.parseInt(el.getAttribute("npcId"));
                        int count = el.hasAttribute("count") ? Integer.parseInt(el.getAttribute("count")) : 1;
                        int x = Integer.parseInt(el.getAttribute("x"));
                        int y = Integer.parseInt(el.getAttribute("y"));
                        int z = Integer.parseInt(el.getAttribute("z"));
                        spawnConfigs.add(new PartyFarmSpawnConfig(npcId, count, x, y, z));
                    }
                }
            } catch (Exception e) {
                log.warn("PartyFarm: Falha ao ler XML de spawns ({}): {}", xmlPath, e.getMessage());
            }
        }
        if (spawnConfigs.isEmpty()) {
            initDefaultSpawns();
        }
    }

    private void initDefaultSpawns() {
        // Default Party Farm zone spawns (Execution Grounds / Giran Harbor area)
        spawnConfigs.add(new PartyFarmSpawnConfig(22124, 5, 45000, 148000, -3400));
        spawnConfigs.add(new PartyFarmSpawnConfig(22126, 3, 45200, 148200, -3400));
    }

    private static List<LocalTime> parseTimes(List<String> timeStrings) {
        List<LocalTime> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (String s : timeStrings) {
            try {
                result.add(LocalTime.parse(s.trim(), fmt));
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * Verifica se a data/hora informada bate com algum agendamento de Party Farm.
     * Retorna Optional com a mensagem de anúncio se o evento entrou em preparação.
     */
    public synchronized Optional<String> checkSchedule(LocalDateTime now) {
        if (!enabled || status != EventStatus.INACTIVE) {
            return Optional.empty();
        }

        DayOfWeek dow = now.getDayOfWeek();
        int dayNum = dow.getValue(); // 1 = Monday ... 7 = Sunday
        if (!activeDaysOfWeek.contains(dayNum)) {
            return Optional.empty();
        }

        LocalTime currentHM = LocalTime.of(now.getHour(), now.getMinute());
        for (LocalTime target : scheduledTimes) {
            if (currentHM.equals(target)) {
                status = EventStatus.PREPARATION;
                currentStartTime = now.plusMinutes(preparationMinutes);
                currentEndTime = currentStartTime.plusMinutes(durationMinutes);
                log.info("PartyFarm: Entrando em fase de preparacao ({} min). Inicio previsto: {}", preparationMinutes, currentStartTime);
                return Optional.of(announcementMessage);
            }
        }

        return Optional.empty();
    }

    /**
     * Inicia efetivamente o evento de Party Farm (spawna os monstros).
     */
    public synchronized boolean startEvent() {
        if (!enabled) {
            return false;
        }
        status = EventStatus.RUNNING;
        spawnedMonsterObjectIds.clear();

        GameWorld world = resolveWorld();
        int totalSpawned = 0;

        for (PartyFarmSpawnConfig cfg : spawnConfigs) {
            int count = Math.max(1, cfg.getCount());
            for (int i = 0; i < count; i++) {
                int offsetX = (i % 3) * 60;
                int offsetY = (i / 3) * 60;
                int sx = cfg.getX() + offsetX;
                int sy = cfg.getY() + offsetY;
                int sz = cfg.getZ();
                int objId = idSeq.incrementAndGet();

                if (world != null) {
                    NpcTemplate tmpl = (npcTemplates != null)
                            ? npcTemplates.getOrFallback(cfg.getNpcId())
                            : NpcTemplate.fallback(cfg.getNpcId(), "PartyFarmMonster_" + cfg.getNpcId(), "L2Monster");
                    NpcInstance npc = new NpcInstance(objId, tmpl, sx, sy, sz, 0);
                    world.addNpc(npc);
                }
                spawnedMonsterObjectIds.add(objId);
                totalSpawned++;
            }
        }
        log.info("PartyFarm: Evento iniciado! {} monstros de Party Farm spawnados ({} grupos).", totalSpawned, spawnConfigs.size());
        return true;
    }

    /**
     * Encerra o evento de Party Farm e remove os monstros da zona.
     */
    public synchronized void endEvent() {
        GameWorld world = resolveWorld();
        if (world != null) {
            for (int objId : spawnedMonsterObjectIds) {
                world.removeNpc(objId);
            }
        }
        int removed = spawnedMonsterObjectIds.size();
        spawnedMonsterObjectIds.clear();
        status = EventStatus.INACTIVE;
        log.info("PartyFarm: Evento encerrado. {} monstros removidos do mundo.", removed);
    }

    public boolean isPartyFarmMonster(int objectId) {
        return spawnedMonsterObjectIds.contains(objectId);
    }

    public List<Integer> getSpawnedMonsterObjectIds() {
        return Collections.unmodifiableList(spawnedMonsterObjectIds);
    }

    private GameWorld resolveWorld() {
        return gameWorld != null ? gameWorld : GameWorld.getInstance();
    }

    public void addSpawnConfig(PartyFarmSpawnConfig config) {
        spawnConfigs.add(config);
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public EventStatus getStatus() { return status; }
    public Set<Integer> getActiveDaysOfWeek() { return activeDaysOfWeek; }
    public void setActiveDaysOfWeek(Set<Integer> activeDaysOfWeek) { this.activeDaysOfWeek = activeDaysOfWeek; }
    public List<LocalTime> getScheduledTimes() { return scheduledTimes; }
    public void setScheduledTimes(List<LocalTime> scheduledTimes) { this.scheduledTimes = scheduledTimes; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getPreparationMinutes() { return preparationMinutes; }
    public void setPreparationMinutes(int preparationMinutes) { this.preparationMinutes = preparationMinutes; }
    public String getAnnouncementMessage() { return announcementMessage; }
    public void setAnnouncementMessage(String announcementMessage) { this.announcementMessage = announcementMessage; }
    public List<PartyFarmSpawnConfig> getSpawnConfigs() { return Collections.unmodifiableList(spawnConfigs); }
}
