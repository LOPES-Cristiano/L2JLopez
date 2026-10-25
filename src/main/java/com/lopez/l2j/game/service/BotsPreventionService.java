package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Sistema Anti-Bot Captcha (BotsPreventionManager) - #496.
 * Monitora mortes consecutivas de mobs com threshold aleatorizado (KillsCounter + Rnd(KillsCounterRandomization)).
 * Gera janela de verificação visual/aritmética interativa com prazo de resposta (ValidationTime),
 * opções de clique e punições automáticas (0 = vila, 1 = kick, 2 = jail, 3 = ban).
 */
@Service
public class BotsPreventionService {

    private static final Logger log = LoggerFactory.getLogger(BotsPreventionService.class);

    public enum PunishmentType {
        TOWN(0),
        KICK(1),
        JAIL(2),
        BAN(3);

        private final int code;
        PunishmentType(int code) { this.code = code; }
        public int getCode() { return code; }

        public static PunishmentType fromCode(int code) {
            for (PunishmentType p : values()) {
                if (p.code == code) return p;
            }
            return TOWN;
        }
    }

    public static class CaptchaChallenge {
        private final int correctNumber;
        private final List<Integer> options;
        private final long expirationTime;
        private final String mathPrompt;

        public CaptchaChallenge(int correctNumber, List<Integer> options, long expirationTime) {
            this(correctNumber, options, expirationTime, null);
        }

        public CaptchaChallenge(int correctNumber, List<Integer> options, long expirationTime, String mathPrompt) {
            this.correctNumber = correctNumber;
            this.options = options;
            this.expirationTime = expirationTime;
            this.mathPrompt = mathPrompt;
        }

        public int getCorrectNumber() { return correctNumber; }
        public List<Integer> getOptions() { return options; }
        public long getExpirationTime() { return expirationTime; }
        public String getMathPrompt() { return mathPrompt; }
        public boolean isExpired() { return System.currentTimeMillis() > expirationTime; }
    }

    private boolean enabled = Config.ENABLE_CAPTCHA;
    private int baseKillsCounter = Config.CAPTCHA_KILLS_COUNTER;
    private int killsRandomization = Config.CAPTCHA_KILLS_COUNTER_RANDOMIZATION;
    private int validationTimeoutSeconds = Config.CAPTCHA_VALIDATION_TIME;
    private PunishmentType punishment = PunishmentType.fromCode(Config.CAPTCHA_PUNISHMENT);

    // Player objectId -> current mob kills count
    private final Map<Integer, Integer> playerKillCounters = new ConcurrentHashMap<>();
    // Player objectId -> next kill threshold
    private final Map<Integer, Integer> playerNextThreshold = new ConcurrentHashMap<>();
    // Player objectId -> pending challenge
    private final Map<Integer, CaptchaChallenge> pendingChallenges = new ConcurrentHashMap<>();
    private ScheduledExecutorService timeoutMonitor;

    public BotsPreventionService() {
        if (enabled) {
            startTimeoutMonitor();
        }
    }

    public synchronized void startTimeoutMonitor() {
        if (timeoutMonitor != null && !timeoutMonitor.isShutdown()) {
            return;
        }
        timeoutMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AntiBot-TimeoutMonitor");
            t.setDaemon(true);
            return t;
        });
        timeoutMonitor.scheduleAtFixedRate(this::sweepExpiredChallenges, 5, 5, TimeUnit.SECONDS);
    }

    public void sweepExpiredChallenges() {
        for (Integer playerId : new ArrayList<>(pendingChallenges.keySet())) {
            checkPunishment(playerId).ifPresent(p -> applyPunishmentToWorldPlayer(playerId, p));
        }
    }

    public void applyPunishmentToWorldPlayer(int playerId, PunishmentType type) {
        GameWorld world = GameWorld.getInstance();
        if (world == null) {
            return;
        }
        world.player(playerId).ifPresent(online -> {
            PlayerCharacter pc = online.character();
            if (pc == null) {
                return;
            }
            switch (type) {
                case TOWN -> world.teleportPlayer(pc, 83400, 147943, -3404, 0);
                case JAIL -> {
                    pc.setInJail(true);
                    world.teleportPlayer(pc, -114356, -249645, -2984, 0);
                }
                case KICK, BAN -> world.unregisterPlayer(playerId);
            }
        });
    }

    /**
     * Chamado toda vez que um jogador derrota um monstro.
     * Retorna Optional com o CaptchaChallenge gerado, se o limite for atingido.
     */
    public synchronized Optional<CaptchaChallenge> onMobKill(int playerId) {
        if (!enabled) {
            return Optional.empty();
        }
        // Se já possui desafio pendente, não incrementa
        if (pendingChallenges.containsKey(playerId)) {
            return Optional.of(pendingChallenges.get(playerId));
        }

        int target = playerNextThreshold.computeIfAbsent(playerId,
                id -> baseKillsCounter + ThreadLocalRandom.current().nextInt(killsRandomization + 1));

        int kills = playerKillCounters.merge(playerId, 1, Integer::sum);

        if (kills >= target) {
            // Gera desafio
            CaptchaChallenge challenge = generateChallenge();
            pendingChallenges.put(playerId, challenge);
            log.info("AntiBot: Limite de {} kills atingido para player {}. Desafio gerado (numero {}).",
                    target, playerId, challenge.getCorrectNumber());
            return Optional.of(challenge);
        }
        return Optional.empty();
    }

    private CaptchaChallenge generateChallenge() {
        int partA = ThreadLocalRandom.current().nextInt(50, 500);
        int partB = ThreadLocalRandom.current().nextInt(50, 499);
        int correct = partA + partB;
        String mathPrompt = partA + " + " + partB + " = " + correct;
        Set<Integer> optionSet = new HashSet<>();
        optionSet.add(correct);
        while (optionSet.size() < 4) {
            optionSet.add(ThreadLocalRandom.current().nextInt(100, 999));
        }
        List<Integer> list = new ArrayList<>(optionSet);
        Collections.shuffle(list);
        long expires = System.currentTimeMillis() + (validationTimeoutSeconds * 1000L);
        return new CaptchaChallenge(correct, list, expires, mathPrompt);
    }

    /**
     * Resposta do jogador via botão de captcha.
     * Retorna true se a validação foi bem-sucedida, false se falhou.
     */
    public synchronized boolean validateAnswer(int playerId, int selectedNumber) {
        CaptchaChallenge challenge = pendingChallenges.get(playerId);
        if (challenge == null) {
            return true;
        }

        if (challenge.isExpired()) {
            log.warn("AntiBot: Desafio do player {} expirou!", playerId);
            return false;
        }

        if (challenge.getCorrectNumber() == selectedNumber) {
            // Sucesso
            pendingChallenges.remove(playerId);
            playerKillCounters.put(playerId, 0);
            playerNextThreshold.put(playerId, baseKillsCounter + ThreadLocalRandom.current().nextInt(killsRandomization + 1));
            log.info("AntiBot: Player {} validou o captcha com sucesso!", playerId);
            return true;
        }

        log.warn("AntiBot: Player {} respondeu incorretamente (esperado: {}, recebido: {}).",
                playerId, challenge.getCorrectNumber(), selectedNumber);
        return false;
    }

    /**
     * Verifica e aplica punição a jogadores com desafios expirados.
     */
    public synchronized Optional<PunishmentType> checkPunishment(int playerId) {
        CaptchaChallenge challenge = pendingChallenges.get(playerId);
        if (challenge != null && challenge.isExpired()) {
            pendingChallenges.remove(playerId);
            playerKillCounters.put(playerId, 0);
            log.warn("AntiBot: Aplicando punicao {} ao player {}", punishment, playerId);
            return Optional.of(punishment);
        }
        return Optional.empty();
    }

    public String buildCaptchaHtml(CaptchaChallenge challenge) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Verificacao de Seguranca Anti-Bot ===</font><br><br>");
        sb.append("Para continuar caçando, selecione o seguinte numero na tela:<br><br>");
        if (challenge.getMathPrompt() != null) {
            sb.append("<font color=\"B09B79\">").append(challenge.getMathPrompt()).append("</font><br1>");
        }
        sb.append("<font color=\"8FA888\"><b><h1>").append(challenge.getCorrectNumber()).append("</h1></b></font><br>");
        sb.append("<table width=200>");
        sb.append("<tr>");
        for (int opt : challenge.getOptions()) {
            sb.append("<td><button value=\"").append(opt)
              .append("\" action=\"bypass -h antibot_validate ").append(opt)
              .append("\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
        }
        sb.append("</tr></table><br>");
        sb.append("<font color=\"B86B6B\">Voce tem ").append(validationTimeoutSeconds).append(" segundos para responder.</font>");
        sb.append("</center></body></html>");
        return sb.toString();
    }

    public boolean hasPendingChallenge(int playerId) { return pendingChallenges.containsKey(playerId); }
    public CaptchaChallenge getPendingChallenge(int playerId) { return pendingChallenges.get(playerId); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getBaseKillsCounter() { return baseKillsCounter; }
    public void setBaseKillsCounter(int baseKillsCounter) { this.baseKillsCounter = baseKillsCounter; }
    public int getKillsRandomization() { return killsRandomization; }
    public void setKillsRandomization(int killsRandomization) { this.killsRandomization = killsRandomization; }
    public int getValidationTimeoutSeconds() { return validationTimeoutSeconds; }
    public void setValidationTimeoutSeconds(int validationTimeoutSeconds) { this.validationTimeoutSeconds = validationTimeoutSeconds; }
    public PunishmentType getPunishment() { return punishment; }
    public void setPunishment(PunishmentType punishment) { this.punishment = punishment; }
}
