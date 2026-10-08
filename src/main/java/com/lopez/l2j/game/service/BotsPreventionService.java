package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sistema Anti-Bot Captcha (BotsPreventionManager).
 * Monitora mortes consecutivas de mobs com threshold aleatorizado (KillsCounter + Rnd(KillsCounterRandomization)).
 * Gera janela de verificação visual interativa com prazo de resposta (ValidationTime),
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

        public CaptchaChallenge(int correctNumber, List<Integer> options, long expirationTime) {
            this.correctNumber = correctNumber;
            this.options = options;
            this.expirationTime = expirationTime;
        }

        public int getCorrectNumber() { return correctNumber; }
        public List<Integer> getOptions() { return options; }
        public long getExpirationTime() { return expirationTime; }
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

    public BotsPreventionService() {}

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
        int correct = ThreadLocalRandom.current().nextInt(100, 999);
        Set<Integer> optionSet = new HashSet<>();
        optionSet.add(correct);
        while (optionSet.size() < 4) {
            optionSet.add(ThreadLocalRandom.current().nextInt(100, 999));
        }
        List<Integer> list = new ArrayList<>(optionSet);
        Collections.shuffle(list);
        long expires = System.currentTimeMillis() + (validationTimeoutSeconds * 1000L);
        return new CaptchaChallenge(correct, list, expires);
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
        sb.append("<font color=\"00FF00\"><b><h1>").append(challenge.getCorrectNumber()).append("</h1></b></font><br>");
        sb.append("<table width=200>");
        sb.append("<tr>");
        for (int opt : challenge.getOptions()) {
            sb.append("<td><button value=\"").append(opt)
              .append("\" action=\"bypass -h antibot_validate ").append(opt)
              .append("\" width=60 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
        }
        sb.append("</tr></table><br>");
        sb.append("<font color=\"FF0000\">Voce tem ").append(validationTimeoutSeconds).append(" segundos para responder.</font>");
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
