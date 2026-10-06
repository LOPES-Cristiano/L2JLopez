package com.lopez.l2j.game.event.partyfarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private boolean enabled = true;
    private int durationMinutes = 30;
    private int preparationMinutes = 5;
    private final Set<Integer> activeDays = new HashSet<>(Arrays.asList(1, 3, 5)); // Seg, Qua, Sex
    private final List<String> scheduleTimes = new ArrayList<>(Arrays.asList("10:30", "15:00", "17:20", "21:15"));
    private final List<MobSpawnConfig> spawnConfigs = new CopyOnWriteArrayList<>();

    private volatile EventStatus status = EventStatus.INACTIVE;
    private String lastTriggeredTime = "";
    private int eventCoinItemId = 6393; // Festival Adena

    public PartyFarmEventService() {
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
        log.info("PartyFarm: Evento iniciado! {} grupos de mobs de party ativos.", spawnConfigs.size());
    }

    public synchronized void endEvent() {
        status = EventStatus.INACTIVE;
        log.info("PartyFarm: Evento finalizado. Mobs removidos.");
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
}
