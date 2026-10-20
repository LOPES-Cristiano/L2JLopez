package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.effect.ConsumableTable.Consumable;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.shortcut.ShortCut;
import com.lopez.l2j.game.shortcut.ShortCutRepository;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTable;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.world.GameWorld.OnlinePlayer;
import com.lopez.l2j.game.zone.Zone;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Servico completo do Sistema AutoFarm Premium integrado ao L2JLopez.
 *
 * Funcionalidades:
 * - 5 Modos de Combate: Fighter, Archer, Mage, Healer e Summon
 * - 4 Abas de Skills do Jogador (Attack, Chance/Debuff, Self Buff, Low Life/Heal) - ate 8 slots cada
 * - 3 Abas de Skills de Summon/Pet (Attack, Self Buff, Heal) - ate 8 slots cada
 * - Configuracao individual por aba: Chance (1-100%), Min MP/HP% (1-100%) e Random Skills
 * - Sincronizacao com barra de atalhos (Shortcut Bar 1..10)
 * - Opcoes tÃ¡ticas: Assist Party Leader, Assist Mobs, Keep Start Farm Loc, Use Skills Extra Delay,
 *   Run Target Close Up, Use Summon Skills, Restore Target MP, Search Distance
 * - Painel ACP (Auto Combat Potions) integrado para HP, MP e CP
 * - Suporte a Modo Gratuito (padrao) ou Modo Pago / VIP / Horas Gratis Diarias / Free Trial
 * - Controle de limite de contas ativas por IP/HWID com expansao compravel
 * - Efeitos visuais (Red Ring e AbnormalEffect) e bloqueio em zonas proibidas / Karma / Olimpiadas
 */
@Service
public class AutoFarmService {

    private static final Logger log = LoggerFactory.getLogger(AutoFarmService.class);

    public static final int GREATER_HEALING_POTION_ID = 1061;
    private static final String[] SKILL_TYPE_KEYS = {
            "farmAttackSkills", "farmChanceSkills", "farmSelfSkills", "farmHealSkills",
            "farmAttackSummonSkills", "farmSelfSummonSkills", "farmHealSummonSkills"
    };

    private final Map<Integer, AutoFarmState> activeFarmStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> activeIpLimits = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> endSubscriptionTasks = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private HtmCache htmCache;

    @Autowired(required = false)
    private CharacterVariablesService characterVariablesService;

    @Autowired(required = false)
    private SkillService skillService;

    @Autowired(required = false)
    private SkillTable skillTable;

    @Autowired(required = false)
    private ShortCutRepository shortCutRepository;

    @Autowired(required = false)
    private ItemTemplateTable itemTemplateTable;

    @Autowired(required = false)
    private ZoneTable zoneTable;

    @Autowired(required = false)
    private GameWorld gameWorld;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> mainLoopFuture;
    private ScheduledFuture<?> dailyRefreshFuture;

    public AutoFarmService() {
    }

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "AutoFarm-Scheduler");
            t.setDaemon(true);
            return t;
        });
        int tickMs = Math.max(200, Config.AUTOFARM_TASK_TICK_MS);
        mainLoopFuture = scheduler.scheduleAtFixedRate(this::runGlobalFarmLoop, 1000L, tickMs, TimeUnit.MILLISECONDS);
        scheduleDailyRefreshIfNeeded();
        log.info("AutoFarmService Premium inicializado (Free={}, Tick={}ms)", Config.AUTOFARM_IS_FREE, tickMs);
    }

    @PreDestroy
    public void shutdown() {
        if (mainLoopFuture != null) {
            mainLoopFuture.cancel(false);
        }
        if (dailyRefreshFuture != null) {
            dailyRefreshFuture.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void scheduleDailyRefreshIfNeeded() {
        if (!Config.AUTOFARM_REFRESH_TIME || scheduler == null) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) > 6 || (cal.get(Calendar.HOUR_OF_DAY) == 6 && cal.get(Calendar.MINUTE) >= 30)) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 30);
        cal.set(Calendar.SECOND, 0);
        long delayMs = Math.max(60_000L, cal.getTimeInMillis() - System.currentTimeMillis());
        dailyRefreshFuture = scheduler.scheduleAtFixedRate(this::refreshDailyOnlineTime, delayMs, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    private void refreshDailyOnlineTime() {
        try {
            for (AutoFarmState state : activeFarmStates.values()) {
                state.clearFarmOnlineTime();
                saveVar(state.getPlayerId(), "activeAutofarmOnlineTime", "0");
            }
            log.info("AutoFarm: Tempo gratuito diario resetado para todos os jogadores.");
        } catch (Exception e) {
            log.warn("Erro ao resetar tempo diario do AutoFarm: {}", e.getMessage());
        }
    }

    // =========================================================================
    // GERENCIAMENTO DE ESTADO E PERSISTENCIA (CharacterVariablesService)
    // =========================================================================

    public boolean isAutoFarm(int playerId) {
        AutoFarmState state = activeFarmStates.get(playerId);
        return state != null && state.isEnabled();
    }

    public AutoFarmState getState(int playerId) {
        return activeFarmStates.get(playerId);
    }

    public AutoFarmState getOrCreateState(int playerId) {
        AutoFarmState state = activeFarmStates.computeIfAbsent(playerId, id -> new AutoFarmState(id, false));
        if (!state.isLoadedFromDb() && characterVariablesService != null) {
            loadStateFromDb(state);
        }
        return state;
    }

    public AutoFarmState getOrCreateState(PlayerCharacter player) {
        AutoFarmState state = getOrCreateState(player.objectId());
        if (!state.isLoadedFromDb()) {
            if (characterVariablesService == null || characterVariablesService.get(player.objectId(), "farmType") == null) {
                detectDefaultFarmType(player, state);
            }
            state.setLoadedFromDb(true);
        }
        return state;
    }

    private void detectDefaultFarmType(PlayerCharacter player, AutoFarmState state) {
        if (player == null) return;
        int classId = player.classId();
        // Archers: Hawkeye(9), SilverRanger(24), PhantomRanger(37), Sagittarius(92), MoonlightSentinel(102), GhostSentinel(109)
        if (classId == 9 || classId == 24 || classId == 37 || classId == 92 || classId == 102 || classId == 109) {
            state.setFarmType(1);
        } else if (player.activeSummon() != null) {
            state.setFarmType(4);
        } else if (player.isMage()) {
            state.setFarmType(2);
        } else {
            state.setFarmType(0);
        }
    }

    private void loadStateFromDb(AutoFarmState state) {
        state.setLoadedFromDb(true);
        if (characterVariablesService == null) {
            return;
        }
        int playerId = state.getPlayerId();
        try {
            state.setFarmType(characterVariablesService.getInt(playerId, "farmType", state.getFarmType()));
            state.setShortcutPage(characterVariablesService.getInt(playerId, "shortcutPage", 10));
            state.setFarmRadius(characterVariablesService.getInt(playerId, "farmDistance", 1200));

            state.setAttackChance(characterVariablesService.getInt(playerId, "farmAttackChance", Config.AUTOFARM_ATTACK_SKILLS_DEFAULT_CHANCE));
            state.setChanceChance(characterVariablesService.getInt(playerId, "farmChanceChance", Config.AUTOFARM_CHANCE_SKILLS_DEFAULT_CHANCE));
            state.setSelfChance(characterVariablesService.getInt(playerId, "farmSelfChance", Config.AUTOFARM_SELF_SKILLS_DEFAULT_CHANCE));
            state.setLifeChance(characterVariablesService.getInt(playerId, "farmHealChance", Config.AUTOFARM_LOW_LIFE_SKILLS_DEFAULT_CHANCE));

            state.setAttackPercent(characterVariablesService.getInt(playerId, "farmAttackPercent", Config.AUTOFARM_ATTACK_SKILLS_MIN_MP_PERCENT));
            state.setChancePercent(characterVariablesService.getInt(playerId, "farmChancePercent", Config.AUTOFARM_CHANCE_SKILLS_MIN_MP_PERCENT));
            state.setSelfPercent(characterVariablesService.getInt(playerId, "farmSelfPercent", Config.AUTOFARM_SELF_SKILLS_MIN_MP_PERCENT));
            state.setLifePercent(characterVariablesService.getInt(playerId, "farmHealPercent", Config.AUTOFARM_LOW_LIFE_SKILLS_MIN_HP_PERCENT));

            state.setSummonAttackChance(characterVariablesService.getInt(playerId, "farmAttackSummonChance", Config.AUTOFARM_SUMMON_ATTACK_DEFAULT_CHANCE));
            state.setSummonSelfChance(characterVariablesService.getInt(playerId, "farmSelfSummonChance", Config.AUTOFARM_SUMMON_SELF_DEFAULT_CHANCE));
            state.setSummonLifeChance(characterVariablesService.getInt(playerId, "farmHealSummonChance", Config.AUTOFARM_SUMMON_HEAL_DEFAULT_CHANCE));

            state.setSummonAttackPercent(characterVariablesService.getInt(playerId, "farmAttackSummonPercent", Config.AUTOFARM_SUMMON_ATTACK_MIN_MP_PERCENT));
            state.setSummonSelfPercent(characterVariablesService.getInt(playerId, "farmSelfSummonPercent", Config.AUTOFARM_SUMMON_SELF_MIN_MP_PERCENT));
            state.setSummonLifePercent(characterVariablesService.getInt(playerId, "farmHealSummonPercent", Config.AUTOFARM_SUMMON_HEAL_MIN_HP_PERCENT));

            state.setLeaderAssist(characterVariablesService.getBoolean(playerId, "farmLeaderAssist", false));
            state.setKeepLocation(characterVariablesService.getBoolean(playerId, "farmKeepLocation", false));
            state.setExtraDelaySkill(characterVariablesService.getBoolean(playerId, "farmExDelaySkill", false));
            state.setExtraSummonDelaySkill(characterVariablesService.getBoolean(playerId, "farmExSumDelaySkill", false));
            state.setRunTargetCloseUp(characterVariablesService.getBoolean(playerId, "farmRunCloseUp", false));
            state.setUseSummonSkills(characterVariablesService.getBoolean(playerId, "farmUseSummonSkills", false));
            state.setAssistMonsterAttack(characterVariablesService.getBoolean(playerId, "farmAssistMonster", false));
            state.setTargetRestoreMp(characterVariablesService.getBoolean(playerId, "farmRestoreTargetMp", false));

            state.setRndAttackSkills(characterVariablesService.getBoolean(playerId, "farmRndAttackSkills", false));
            state.setRndChanceSkills(characterVariablesService.getBoolean(playerId, "farmRndChanceSkills", false));
            state.setRndSelfSkills(characterVariablesService.getBoolean(playerId, "farmRndSelfSkills", false));
            state.setRndLifeSkills(characterVariablesService.getBoolean(playerId, "farmRndLifeSkills", false));

            state.setRndSummonAttackSkills(characterVariablesService.getBoolean(playerId, "farmRndAttackSummonSkills", false));
            state.setRndSummonSelfSkills(characterVariablesService.getBoolean(playerId, "farmRndSelfSummonSkills", false));
            state.setRndSummonLifeSkills(characterVariablesService.getBoolean(playerId, "farmRndLifeSummonSkills", false));

            state.setAcpHpPercent(characterVariablesService.getInt(playerId, "farmAcpHpPercent", 0));
            state.setAcpMpPercent(characterVariablesService.getInt(playerId, "farmAcpMpPercent", 0));
            state.setAcpCpPercent(characterVariablesService.getInt(playerId, "farmAcpCpPercent", 0));

            state.setAutoFarmEnd(getVarLong(playerId, "activeAutofarm", 0L));
            state.setFarmOnlineTime(getVarLong(playerId, "activeAutofarmOnlineTime", 0L));
            state.setFreeTrialUsed(characterVariablesService.getBoolean(playerId, "useFreeTime", false));

            for (String key : SKILL_TYPE_KEYS) {
                String raw = characterVariablesService.get(playerId, key);
                if (raw != null && !raw.isBlank()) {
                    List<Integer> list = state.getSkillsForType(key);
                    list.clear();
                    for (String part : raw.split(";")) {
                        if (!part.isBlank()) {
                            try {
                                list.add(Integer.parseInt(part.trim()));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao carregar variaveis de AutoFarm do player {}: {}", playerId, e.getMessage());
        }
    }

    private long getVarLong(int playerId, String name, long defaultValue) {
        if (characterVariablesService == null) {
            return defaultValue;
        }
        String raw = characterVariablesService.get(playerId, name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void saveSkills(AutoFarmState state, String key) {
        if (state == null || key == null) return;
        String dbKey = normalizeSkillStorageKey(key, false);
        List<Integer> skills = state.getSkillsForType(dbKey);
        StringBuilder sb = new StringBuilder();
        for (int skillId : skills) {
            sb.append(skillId).append(";");
        }
        saveVar(state.getPlayerId(), dbKey, sb.toString());
    }

    public void saveSummonSkills(AutoFarmState state, String key) {
        if (state == null || key == null) return;
        String dbKey = normalizeSkillStorageKey(key, true);
        List<Integer> skills = state.getSummonSkillsForType(dbKey);
        StringBuilder sb = new StringBuilder();
        for (int skillId : skills) {
            sb.append(skillId).append(";");
        }
        saveVar(state.getPlayerId(), dbKey, sb.toString());
    }

    private String normalizeSkillStorageKey(String key, boolean summon) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (summon) {
            return switch (lower) {
                case "self", "farmselfsummonskills" -> "farmSelfSummonSkills";
                case "heal", "farmhealsummonskills" -> "farmHealSummonSkills";
                default -> "farmAttackSummonSkills";
            };
        }
        return switch (lower) {
            case "chance", "farmchanceskills" -> "farmChanceSkills";
            case "self", "farmselfskills" -> "farmSelfSkills";
            case "heal", "farmhealskills" -> "farmHealSkills";
            case "farmattacksummonskills" -> "farmAttackSummonSkills";
            case "farmselfsummonskills" -> "farmSelfSummonSkills";
            case "farmhealsummonskills" -> "farmHealSummonSkills";
            default -> "farmAttackSkills";
        };
    }

    private void saveVar(int playerId, String name, String value) {
        if (characterVariablesService != null) {
            characterVariablesService.set(playerId, name, value);
        }
    }

    // =========================================================================
    // CONTROLE DE ATIVACAO / DESATIVACAO E LIMITES DE IP/HWID
    // =========================================================================

    public synchronized boolean toggleAutoFarm(PlayerCharacter player) {
        if (player == null) {
            return false;
        }
        if (isAutoFarm(player.objectId())) {
            stopFarm(player, null);
            return false;
        } else {
            return startFarm(player, null);
        }
    }

    public synchronized boolean startFarm(PlayerCharacter player, GameSession session) {
        if (player == null) {
            return false;
        }
        if (!Config.ALLOW_AUTO_FARM) {
            sendMessage(session, "O sistema de AutoFarm esta desativado.");
            return false;
        }
        if (player.isDead()) {
            sendMessage(session, "AutoFarm nao pode ser ativado enquanto morto.");
            return false;
        }
        if (player.karma() > 0 && (Config.AUTOFARM_STOP_IF_KARMA || session == null)) {
            sendMessage(session, "AutoFarm nao pode ser ativado com Karma.");
            return false;
        }
        if (player.isOlympiadMode()) {
            sendMessage(session, "AutoFarm nao permitido nas Olimpiadas.");
            return false;
        }
        if (isInProhibitedZone(player)) {
            sendMessage(session, "AutoFarm nao permitido nesta zona.");
            return false;
        }

        AutoFarmState state = getOrCreateState(player);
        if (session != null && session.ip() != null) {
            state.setClientIp(session.ip());
        }

        if (state.isEnabled()) {
            sendMessage(session, "AutoFarm ja esta ativado.");
            return true;
        }

        // Verifica restricao VIP
        boolean isVip = player.vip() && (player.vipExpiration() == 0L || player.vipExpiration() > System.currentTimeMillis());
        if (Config.AUTOFARM_ONLY_FOR_PREMIUM && !isVip) {
            sendMessage(session, "AutoFarm disponivel apenas para jogadores VIP!");
            return false;
        }

        // Verifica se precisa de tempo pago / online gratuito
        boolean isFreeForPlayer = Config.AUTOFARM_IS_FREE || (Config.AUTOFARM_FREE_FOR_PREMIUM && isVip);
        boolean useOnlineFreeTime = false;
        if (!isFreeForPlayer && !state.isActiveAutofarmAllowed()) {
            if (Config.AUTOFARM_ONLINE_TYPE && Config.AUTOFARM_FREE_TIME > 0) {
                long maxFreeMs = TimeUnit.MINUTES.toMillis(Config.AUTOFARM_FREE_TIME);
                if (state.getFarmOnlineTime() >= maxFreeMs) {
                    sendMessage(session, "Seu tempo gratuito diario de AutoFarm acabou!");
                    return false;
                }
                useOnlineFreeTime = true;
            } else {
                sendMessage(session, "Voce precisa adquirir tempo de AutoFarm na aba Buy.");
                return false;
            }
        }

        // Verifica limite de IP/HWID
        if (Config.AUTOFARM_MAXIMUM_ACTIVATED_LIMITS > 0 && session != null) {
            int remainingSlots = getRemainingIpSlots(player, state.getClientIp());
            if (remainingSlots <= 0) {
                sendMessage(session, "Limite maximo de personagens em AutoFarm atingido para seu IP!");
                return false;
            }
        }

        if (useOnlineFreeTime) {
            state.setActiveFarmOnlineTime();
        }

        if (state.isKeepLocation()) {
            state.setKeepLocation(true, player.x(), player.y(), player.z());
        }

        state.setEnabled(true);

        applyVisualEffects(player, session, true);
        sendMessage(session, "AutoFarm ativado!");
        log.info("Auto-Farm ativado para {}", player.name());
        return true;
    }

    public synchronized void stopFarm(PlayerCharacter player, GameSession session) {
        if (player == null) {
            return;
        }
        AutoFarmState state = activeFarmStates.get(player.objectId());
        if (state == null || !state.isEnabled()) {
            return;
        }
        state.setEnabled(false);
        if (state.isActiveFarmOnlineTime()) {
            state.stopFarmOnlineTime();
            saveVar(player.objectId(), "activeAutofarmOnlineTime", String.valueOf(state.getFarmOnlineTime()));
        }
        applyVisualEffects(player, session, false);
        if (session != null) {
            session.stopAutoAttack();
            sendMessage(session, "AutoFarm desativado.");
        }
        log.info("Auto-Farm desativado para {}", player.name());
    }

    private void applyVisualEffects(PlayerCharacter player, GameSession session, boolean active) {
        boolean changed = false;
        if (Config.AUTOFARM_SET_RED_RING) {
            player.team(active ? 2 : 0);
            changed = true;
        }
        int abnormalMask = resolveAbnormalEffectMask(Config.AUTOFARM_ABNORMAL_EFFECT_NAME);
        if (abnormalMask != 0) {
            if (active) {
                player.startAbnormalEffect(abnormalMask);
            } else {
                player.stopAbnormalEffect(abnormalMask);
            }
            changed = true;
        }
        if (changed && session != null) {
            session.sendUserInfoAndBroadcastCharInfo();
        }
    }

    private int resolveAbnormalEffectMask(String name) {
        if (name == null || name.isBlank() || "NONE".equalsIgnoreCase(name.trim()) || "NULL".equalsIgnoreCase(name.trim())) {
            return 0;
        }
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "BLEEDING" -> 0x000001;
            case "POISON" -> 0x000002;
            case "REDCIRCLE" -> 0x000004;
            case "ICE" -> 0x000008;
            case "WIND" -> 0x000010;
            case "FEAR" -> 0x000020;
            case "STUN" -> 0x000040;
            case "SLEEP" -> 0x000080;
            case "MUTED" -> 0x000100;
            case "ROOT" -> 0x000200;
            case "HOLD_1" -> 0x000400;
            case "HOLD_2" -> 0x000800;
            case "BIG_HEAD" -> 0x002000;
            case "FLAME" -> 0x004000;
            case "GROW" -> 0x010000;
            case "FLOATING_ROOT" -> 0x020000;
            case "DANCE_STUNNED" -> 0x040000;
            case "FIREROOT_STUN" -> 0x080000;
            case "STEALTH" -> 0x100000;
            case "IMPRISONING_1" -> 0x200000;
            case "IMPRISONING_2" -> 0x400000;
            case "MAGIC_CIRCLE" -> 0x800000;
            case "VITALITY" -> 0x1000000;
            default -> 0;
        };
    }

    private boolean isInProhibitedZone(PlayerCharacter player) {
        if (zoneTable == null || Config.AUTOFARM_PROHIBITED_ZONES == null || Config.AUTOFARM_PROHIBITED_ZONES.isEmpty()) {
            return false;
        }
        List<Zone> zones = zoneTable.getZonesAt(player.x(), player.y(), player.z());
        for (Zone zone : zones) {
            if (Config.AUTOFARM_PROHIBITED_ZONES.contains(zone.id())) {
                return true;
            }
        }
        return false;
    }

    public int getRemainingIpSlots(PlayerCharacter player, String ip) {
        if (Config.AUTOFARM_MAXIMUM_ACTIVATED_LIMITS <= 0) {
            return 999;
        }
        String checkIp = (ip == null || ip.isBlank()) ? "127.0.0.1" : ip;
        int extraSlots = activeIpLimits.getOrDefault(checkIp, 0);
        if (extraSlots == 0 && characterVariablesService != null && player != null) {
            String saved = characterVariablesService.get(player.objectId(), "autoFarmLimits");
            if (saved != null && saved.contains(";")) {
                String[] parts = saved.split(";");
                if (parts.length == 2) {
                    try {
                        extraSlots = Integer.parseInt(parts[1]);
                        activeIpLimits.put(checkIp, extraSlots);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        int activeOnSameIp = 0;
        for (AutoFarmState st : activeFarmStates.values()) {
            if (st.isEnabled() && checkIp.equals(st.getClientIp())) {
                activeOnSameIp++;
            }
        }
        return (Config.AUTOFARM_MAXIMUM_ACTIVATED_LIMITS + extraSlots) - activeOnSameIp;
    }

    public void setMode(int playerId, AutoFarmMode mode) {
        AutoFarmState state = getOrCreateState(playerId);
        state.setMode(mode);
        saveVar(playerId, "farmType", String.valueOf(state.getFarmType()));
    }

    public void setSelectedSkill(int playerId, int skillId) {
        AutoFarmState state = getOrCreateState(playerId);
        state.setSelectedSkillId(skillId);
        saveSkills(state, "attack");
    }

    public void setFarmRadius(int playerId, int radius) {
        AutoFarmState state = getOrCreateState(playerId);
        state.setFarmRadius(radius);
        saveVar(playerId, "farmDistance", String.valueOf(state.getFarmRadius()));
    }

    public void setAutoPotionHpThreshold(int playerId, double threshold) {
        AutoFarmState state = getOrCreateState(playerId);
        state.setAutoPotionHpThreshold(threshold);
        saveVar(playerId, "farmAcpHpPercent", String.valueOf(state.getAcpHpPercent()));
    }

    // =========================================================================
    // PROCESSAMENTO DE COMANDOS (VOICED / BYPASS)
    // =========================================================================

    public void handleCommand(GameSession session, String fullCommand) {
        if (session == null || session.activeCharacter() == null || fullCommand == null || fullCommand.isBlank()) {
            return;
        }
        PlayerCharacter player = session.activeCharacter();
        AutoFarmState state = getOrCreateState(player);
        if (session.ip() != null) {
            state.setClientIp(session.ip());
        }

        String trimmed = fullCommand.trim();
        StringTokenizer st = new StringTokenizer(trimmed);
        String cmd = st.hasMoreTokens() ? st.nextToken() : "voice_autofarm";

        // Normaliza prefixo
        String normalizedCmd = cmd;
        if (normalizedCmd.startsWith("voice_")) {
            normalizedCmd = normalizedCmd.substring("voice_".length());
        } else if (normalizedCmd.startsWith("user_")) {
            normalizedCmd = normalizedCmd.substring("user_".length());
        } else if (normalizedCmd.startsWith("voiced_")) {
            normalizedCmd = normalizedCmd.substring("voiced_".length());
        }

        // Suporte aos subcomandos legados (.autofarm toggle / mode / radius / hp / skill)
        if ("autofarm".equalsIgnoreCase(normalizedCmd) || "farm".equalsIgnoreCase(normalizedCmd)) {
            if (st.hasMoreTokens()) {
                String token = st.nextToken();
                if ("toggle".equalsIgnoreCase(token)) {
                    toggleAutoFarmWithSession(player, session);
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("on".equalsIgnoreCase(token) || "start".equalsIgnoreCase(token)) {
                    startFarm(player, session);
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("off".equalsIgnoreCase(token) || "stop".equalsIgnoreCase(token)) {
                    stopFarm(player, session);
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("mode".equalsIgnoreCase(token) && st.hasMoreTokens()) {
                    String modeStr = st.nextToken();
                    applyFarmTypeString(player, state, modeStr);
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("radius".equalsIgnoreCase(token) && st.hasMoreTokens()) {
                    try {
                        setFarmRadius(player.objectId(), Integer.parseInt(st.nextToken()));
                    } catch (NumberFormatException ignored) {
                    }
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("hp".equalsIgnoreCase(token) && st.hasMoreTokens()) {
                    try {
                        int pct = Integer.parseInt(st.nextToken());
                        setAutoPotionHpThreshold(player.objectId(), pct / 100.0);
                    } catch (NumberFormatException ignored) {
                    }
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else if ("skill".equalsIgnoreCase(token) && st.hasMoreTokens()) {
                    try {
                        setSelectedSkill(player.objectId(), Integer.parseInt(st.nextToken()));
                    } catch (NumberFormatException ignored) {
                    }
                    showIndexWindow(session, player, state.getActiveSkillTab());
                    return;
                } else {
                    // Ex: voice_autofarm Mage
                    applyFarmTypeString(player, state, token);
                }
            }
            showIndexWindow(session, player, state.getActiveSkillTab());
            return;
        }

        switch (normalizedCmd.toLowerCase(Locale.ROOT)) {
            case "farmstart" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                startFarm(player, session);
                showIndexWindow(session, player, skillType);
            }
            case "farmstop" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                stopFarm(player, session);
                showIndexWindow(session, player, skillType);
            }
            case "farmacp" -> {
                if (st.hasMoreTokens()) {
                    String action = st.nextToken();
                    if ("set_hp".equalsIgnoreCase(action) && st.hasMoreTokens()) {
                        state.setAcpHpPercent(parseIntSafe(st.nextToken(), 0));
                        saveVar(player.objectId(), "farmAcpHpPercent", String.valueOf(state.getAcpHpPercent()));
                    } else if ("set_mp".equalsIgnoreCase(action) && st.hasMoreTokens()) {
                        state.setAcpMpPercent(parseIntSafe(st.nextToken(), 0));
                        saveVar(player.objectId(), "farmAcpMpPercent", String.valueOf(state.getAcpMpPercent()));
                    } else if ("set_cp".equalsIgnoreCase(action) && st.hasMoreTokens()) {
                        state.setAcpCpPercent(parseIntSafe(st.nextToken(), 0));
                        saveVar(player.objectId(), "farmAcpCpPercent", String.valueOf(state.getAcpCpPercent()));
                    } else if ("off_hp".equalsIgnoreCase(action)) {
                        state.setAcpHpPercent(0);
                        saveVar(player.objectId(), "farmAcpHpPercent", "0");
                    } else if ("off_mp".equalsIgnoreCase(action)) {
                        state.setAcpMpPercent(0);
                        saveVar(player.objectId(), "farmAcpMpPercent", "0");
                    } else if ("off_cp".equalsIgnoreCase(action)) {
                        state.setAcpCpPercent(0);
                        saveVar(player.objectId(), "farmAcpCpPercent", "0");
                    }
                }
                showAcpWindow(session, player, state);
            }
            case "changeskilltype" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : "attack";
                state.setActiveSkillTab(skillType);
                showIndexWindow(session, player, skillType);
            }
            case "refreshskills" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                int page = st.hasMoreTokens() ? parseIntSafe(st.nextToken(), state.getShortcutPage()) : state.getShortcutPage();
                state.setShortcutPage(page);
                saveVar(player.objectId(), "shortcutPage", String.valueOf(state.getShortcutPage()));
                refreshSkillsFromShortcutBar(player, state);
                showIndexWindow(session, player, skillType);
            }
            case "removeskill" -> {
                if (st.countTokens() >= 3) {
                    String skillType = st.nextToken();
                    int skillId = parseIntSafe(st.nextToken(), 0);
                    int slot = parseIntSafe(st.nextToken(), -1);
                    List<Integer> list = state.getSkillsForType(skillType);
                    if (slot >= 0 && slot < list.size() && list.get(slot) == skillId) {
                        list.remove(slot);
                        saveSkills(state, skillType);
                    }
                    showIndexWindow(session, player, skillType);
                }
            }
            case "addskill" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                int page = st.hasMoreTokens() ? parseIntSafe(st.nextToken(), 1) : 1;
                showAvailablePlayerSkillsWindow(session, player, state, skillType, page);
            }
            case "addnewskill" -> {
                if (st.countTokens() >= 2) {
                    int skillId = parseIntSafe(st.nextToken(), 0);
                    String skillType = st.nextToken();
                    addPlayerSkillToSlot(session, player, state, skillId, skillType);
                    showIndexWindow(session, player, skillType);
                }
            }
            case "editfarmoption" -> {
                handleEditFarmOption(session, player, state, st);
            }
            case "editsummonskills", "autosummonfarm" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSummonSkillTab();
                state.setActiveSummonSkillTab(skillType);
                showSummonSkillsWindow(session, player, state, skillType);
            }
            case "removesummonskill" -> {
                if (st.countTokens() >= 3) {
                    String skillType = st.nextToken();
                    int skillId = parseIntSafe(st.nextToken(), 0);
                    int slot = parseIntSafe(st.nextToken(), -1);
                    List<Integer> list = state.getSummonSkillsForType(skillType);
                    if (slot >= 0 && slot < list.size() && list.get(slot) == skillId) {
                        list.remove(slot);
                        saveSummonSkills(state, skillType);
                    }
                    showSummonSkillsWindow(session, player, state, skillType);
                }
            }
            case "addsummonskill" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSummonSkillTab();
                int page = st.hasMoreTokens() ? parseIntSafe(st.nextToken(), 1) : 1;
                showAvailableSummonSkillsWindow(session, player, state, skillType, page);
            }
            case "addnewsummonskill" -> {
                if (st.countTokens() >= 2) {
                    int skillId = parseIntSafe(st.nextToken(), 0);
                    String skillType = st.nextToken();
                    addSummonSkillToSlot(session, player, state, skillId, skillType);
                    showSummonSkillsWindow(session, player, state, skillType);
                }
            }
            case "editsummonfarmoption" -> {
                handleEditSummonFarmOption(session, player, state, st);
            }
            case "buyfarm" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                showBuyFarmWindow(session, player, state, skillType);
            }
            case "buyfarmtime" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                if (!st.hasMoreTokens()) {
                    sendMessage(session, "Selecione uma opcao de tempo valida.");
                    showBuyFarmWindow(session, player, state, skillType);
                    return;
                }
                int hours = parseIntSafe(st.nextToken(), 0);
                buyFarmTime(session, player, state, hours);
                showIndexWindow(session, player, skillType);
            }
            case "tryfreetime" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                activateFreeTrialTime(session, player, state);
                showIndexWindow(session, player, skillType);
            }
            case "expendlimit" -> {
                String skillType = st.hasMoreTokens() ? st.nextToken() : state.getActiveSkillTab();
                expandIpLimit(session, player, state);
                showIndexWindow(session, player, skillType);
            }
            default -> showIndexWindow(session, player, state.getActiveSkillTab());
        }
    }

    private void toggleAutoFarmWithSession(PlayerCharacter player, GameSession session) {
        if (isAutoFarm(player.objectId())) {
            stopFarm(player, session);
        } else {
            startFarm(player, session);
        }
    }

    private void applyFarmTypeString(PlayerCharacter player, AutoFarmState state, String typeStr) {
        if (typeStr == null) return;
        switch (typeStr.trim().toLowerCase(Locale.ROOT)) {
            case "fighter", "0", "balanced" -> state.setFarmType(0);
            case "archer", "1" -> state.setFarmType(1);
            case "mage", "2" -> state.setFarmType(2);
            case "healer", "3" -> state.setFarmType(3);
            case "summon", "4" -> state.setFarmType(4);
        }
        saveVar(player.objectId(), "farmType", String.valueOf(state.getFarmType()));
    }

    private void handleEditFarmOption(GameSession session, PlayerCharacter player, AutoFarmState state, StringTokenizer st) {
        if (st.countTokens() < 2) {
            showIndexWindow(session, player, state.getActiveSkillTab());
            return;
        }
        String skillType = st.nextToken();
        String option = st.nextToken();

        if (!st.hasMoreTokens()) {
            switch (option) {
                case "assist" -> {
                    state.setLeaderAssist(!state.isLeaderAssist());
                    saveVar(player.objectId(), "farmLeaderAssist", String.valueOf(state.isLeaderAssist()));
                    saveVar(player.objectId(), "farmAssistMonster", String.valueOf(state.isAssistMonsterAttack()));
                    saveVar(player.objectId(), "farmKeepLocation", String.valueOf(state.isKeepLocation()));
                }
                case "assistMonster" -> {
                    state.setAssistMonsterAttack(!state.isAssistMonsterAttack());
                    saveVar(player.objectId(), "farmAssistMonster", String.valueOf(state.isAssistMonsterAttack()));
                    saveVar(player.objectId(), "farmLeaderAssist", String.valueOf(state.isLeaderAssist()));
                }
                case "keepLocation" -> {
                    state.setKeepLocation(!state.isKeepLocation(), player.x(), player.y(), player.z());
                    saveVar(player.objectId(), "farmKeepLocation", String.valueOf(state.isKeepLocation()));
                    saveVar(player.objectId(), "farmLeaderAssist", String.valueOf(state.isLeaderAssist()));
                }
                case "exDelaySkill" -> {
                    state.setExtraDelaySkill(!state.isExtraDelaySkill());
                    saveVar(player.objectId(), "farmExDelaySkill", String.valueOf(state.isExtraDelaySkill()));
                }
                case "runCloseUp" -> {
                    state.setRunTargetCloseUp(!state.isRunTargetCloseUp());
                    saveVar(player.objectId(), "farmRunCloseUp", String.valueOf(state.isRunTargetCloseUp()));
                }
                case "useSummonSkills" -> {
                    state.setUseSummonSkills(!state.isUseSummonSkills());
                    saveVar(player.objectId(), "farmUseSummonSkills", String.valueOf(state.isUseSummonSkills()));
                }
                case "restoreMp" -> {
                    state.setTargetRestoreMp(!state.isTargetRestoreMp());
                    saveVar(player.objectId(), "farmRestoreTargetMp", String.valueOf(state.isTargetRestoreMp()));
                }
                case "rndAttackSkills" -> {
                    state.setRndAttackSkills(!state.isRndAttackSkills());
                    saveVar(player.objectId(), "farmRndAttackSkills", String.valueOf(state.isRndAttackSkills()));
                }
                case "rndChanceSkills" -> {
                    state.setRndChanceSkills(!state.isRndChanceSkills());
                    saveVar(player.objectId(), "farmRndChanceSkills", String.valueOf(state.isRndChanceSkills()));
                }
                case "rndSelfSkills" -> {
                    state.setRndSelfSkills(!state.isRndSelfSkills());
                    saveVar(player.objectId(), "farmRndSelfSkills", String.valueOf(state.isRndSelfSkills()));
                }
                case "rndLifeSkills" -> {
                    state.setRndLifeSkills(!state.isRndLifeSkills());
                    saveVar(player.objectId(), "farmRndLifeSkills", String.valueOf(state.isRndLifeSkills()));
                }
            }
            showIndexWindow(session, player, skillType);
            return;
        }

        int val = parseIntSafe(st.nextToken(), -1);
        if (val > 0) {
            if ("distance".equalsIgnoreCase(option)) {
                state.setFarmRadius(Math.max(100, Math.min(2500, val)));
                saveVar(player.objectId(), "farmDistance", String.valueOf(state.getFarmRadius()));
            } else {
                int pct = Math.max(1, Math.min(100, val));
                switch (option) {
                    case "attackChance" -> {
                        state.setAttackChance(pct);
                        saveVar(player.objectId(), "farmAttackChance", String.valueOf(pct));
                    }
                    case "chanceChance" -> {
                        state.setChanceChance(pct);
                        saveVar(player.objectId(), "farmChanceChance", String.valueOf(pct));
                    }
                    case "selfChance" -> {
                        state.setSelfChance(pct);
                        saveVar(player.objectId(), "farmSelfChance", String.valueOf(pct));
                    }
                    case "healChance" -> {
                        state.setLifeChance(pct);
                        saveVar(player.objectId(), "farmHealChance", String.valueOf(pct));
                    }
                    case "attackPercent" -> {
                        state.setAttackPercent(pct);
                        saveVar(player.objectId(), "farmAttackPercent", String.valueOf(pct));
                    }
                    case "chancePercent" -> {
                        state.setChancePercent(pct);
                        saveVar(player.objectId(), "farmChancePercent", String.valueOf(pct));
                    }
                    case "selfPercent" -> {
                        state.setSelfPercent(pct);
                        saveVar(player.objectId(), "farmSelfPercent", String.valueOf(pct));
                    }
                    case "healPercent" -> {
                        state.setLifePercent(pct);
                        saveVar(player.objectId(), "farmHealPercent", String.valueOf(pct));
                    }
                }
            }
        }
        showIndexWindow(session, player, skillType);
    }

    private void handleEditSummonFarmOption(GameSession session, PlayerCharacter player, AutoFarmState state, StringTokenizer st) {
        if (st.countTokens() < 2) {
            showSummonSkillsWindow(session, player, state, state.getActiveSummonSkillTab());
            return;
        }
        String skillType = st.nextToken();
        String option = st.nextToken();

        if (!st.hasMoreTokens()) {
            switch (option) {
                case "useSummonSkills" -> {
                    state.setUseSummonSkills(!state.isUseSummonSkills());
                    saveVar(player.objectId(), "farmUseSummonSkills", String.valueOf(state.isUseSummonSkills()));
                }
                case "exDelaySkill" -> {
                    state.setExtraSummonDelaySkill(!state.isExtraSummonDelaySkill());
                    saveVar(player.objectId(), "farmExSumDelaySkill", String.valueOf(state.isExtraSummonDelaySkill()));
                }
                case "rndAttackSkills" -> {
                    state.setRndSummonAttackSkills(!state.isRndSummonAttackSkills());
                    saveVar(player.objectId(), "farmRndAttackSummonSkills", String.valueOf(state.isRndSummonAttackSkills()));
                }
                case "rndSelfSkills" -> {
                    state.setRndSummonSelfSkills(!state.isRndSummonSelfSkills());
                    saveVar(player.objectId(), "farmRndSelfSummonSkills", String.valueOf(state.isRndSummonSelfSkills()));
                }
                case "rndLifeSkills" -> {
                    state.setRndSummonLifeSkills(!state.isRndSummonLifeSkills());
                    saveVar(player.objectId(), "farmRndLifeSummonSkills", String.valueOf(state.isRndSummonLifeSkills()));
                }
            }
            showSummonSkillsWindow(session, player, state, skillType);
            return;
        }

        int val = parseIntSafe(st.nextToken(), -1);
        if (val > 0) {
            int pct = Math.max(1, Math.min(100, val));
            switch (option) {
                case "attackChance" -> {
                    state.setSummonAttackChance(pct);
                    saveVar(player.objectId(), "farmAttackSummonChance", String.valueOf(pct));
                }
                case "selfChance" -> {
                    state.setSummonSelfChance(pct);
                    saveVar(player.objectId(), "farmSelfSummonChance", String.valueOf(pct));
                }
                case "healChance" -> {
                    state.setSummonLifeChance(pct);
                    saveVar(player.objectId(), "farmHealSummonChance", String.valueOf(pct));
                }
                case "attackPercent" -> {
                    state.setSummonAttackPercent(pct);
                    saveVar(player.objectId(), "farmAttackSummonPercent", String.valueOf(pct));
                }
                case "selfPercent" -> {
                    state.setSummonSelfPercent(pct);
                    saveVar(player.objectId(), "farmSelfSummonPercent", String.valueOf(pct));
                }
                case "healPercent" -> {
                    state.setSummonLifePercent(pct);
                    saveVar(player.objectId(), "farmHealSummonPercent", String.valueOf(pct));
                }
            }
        }
        showSummonSkillsWindow(session, player, state, skillType);
    }

    // =========================================================================
    // MONETIZACAO / TEMPO DE ASSINATURA / FREE TRIAL / EXPANSAO DE IP
    // =========================================================================

    private void buyFarmTime(GameSession session, PlayerCharacter player, AutoFarmState state, int hours) {
        if (hours <= 0 || Config.AUTOFARM_PRICES == null || !Config.AUTOFARM_PRICES.containsKey(hours)) {
            sendMessage(session, "Opcao de tempo nao disponivel.");
            return;
        }
        int[] price = Config.AUTOFARM_PRICES.get(hours);
        int itemId = price[0];
        int count = price[1];
        if (!session.consumeItem(itemId, count)) {
            sendMessage(session, "Voce nao possui itens suficientes para comprar o AutoFarm.");
            return;
        }
        long addMs = TimeUnit.HOURS.toMillis(hours);
        long baseTime = Math.max(System.currentTimeMillis(), state.getAutoFarmEnd());
        long newEnd = baseTime + addMs;
        state.setAutoFarmEnd(newEnd);
        saveVar(player.objectId(), "activeAutofarm", String.valueOf(newEnd));
        scheduleEndSubscriptionTask(player.objectId(), newEnd);
        sendMessage(session, "AutoFarm adquirido por " + hours + "h com sucesso!");
    }

    private void activateFreeTrialTime(GameSession session, PlayerCharacter player, AutoFarmState state) {
        if (Config.AUTOFARM_GAIN_FREE_TIME <= 0) {
            return;
        }
        if (state.isFreeTrialUsed()) {
            sendMessage(session, "Voce ja utilizou seu periodo de teste gratuito.");
            return;
        }
        state.setFreeTrialUsed(true);
        saveVar(player.objectId(), "useFreeTime", "true");
        long addMs = TimeUnit.HOURS.toMillis(Config.AUTOFARM_GAIN_FREE_TIME);
        long baseTime = Math.max(System.currentTimeMillis(), state.getAutoFarmEnd());
        long newEnd = baseTime + addMs;
        state.setAutoFarmEnd(newEnd);
        saveVar(player.objectId(), "activeAutofarm", String.valueOf(newEnd));
        scheduleEndSubscriptionTask(player.objectId(), newEnd);
        sendMessage(session, "Periodo gratuito de " + Config.AUTOFARM_GAIN_FREE_TIME + "h ativado!");
    }

    private void expandIpLimit(GameSession session, PlayerCharacter player, AutoFarmState state) {
        if (!Config.AUTOFARM_ALLOW_EXTEND_LIMITS || Config.AUTOFARM_MAXIMUM_ACTIVATED_LIMITS <= 0) {
            return;
        }
        int[] price = Config.AUTOFARM_VISIBLE_LIMITS_PRICE;
        if (price == null || price.length < 2) {
            return;
        }
        if (!session.consumeItem(price[0], price[1])) {
            sendMessage(session, "Itens insuficientes para expandir o limite de AutoFarm.");
            return;
        }
        String ip = state.getClientIp();
        int currentExtra = activeIpLimits.getOrDefault(ip, 0) + 1;
        activeIpLimits.put(ip, currentExtra);
        saveVar(player.objectId(), "autoFarmLimits", ip + ";" + currentExtra);
        sendMessage(session, "Limite de contas em AutoFarm expandido em +1!");
    }

    private void scheduleEndSubscriptionTask(int playerId, long endTimeMs) {
        if (scheduler == null) return;
        ScheduledFuture<?> prev = endSubscriptionTasks.remove(playerId);
        if (prev != null) {
            prev.cancel(false);
        }
        long delay = endTimeMs - System.currentTimeMillis();
        if (delay > 500L) {
            ScheduledFuture<?> fut = scheduler.schedule(() -> {
                AutoFarmState st = activeFarmStates.get(playerId);
                if (st != null) {
                    st.setAutoFarmEnd(0L);
                    saveVar(playerId, "activeAutofarm", "0");
                    if (!Config.AUTOFARM_IS_FREE && !Config.AUTOFARM_ONLINE_TYPE && st.isEnabled()) {
                        GameWorld world = GameWorld.getInstance();
                        OnlinePlayer op = world != null ? world.player(playerId).orElse(null) : null;
                        if (op instanceof GameSession gs && gs.activeCharacter() != null) {
                            stopFarm(gs.activeCharacter(), gs);
                            showIndexWindow(gs, gs.activeCharacter(), st.getActiveSkillTab());
                        } else {
                            st.setEnabled(false);
                        }
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            endSubscriptionTasks.put(playerId, fut);
        }
    }

    // =========================================================================
    // GERENCIAMENTO DE SKILLS DO JOGADOR E SUMMON
    // =========================================================================

    private void refreshSkillsFromShortcutBar(PlayerCharacter player, AutoFarmState state) {
        if (shortCutRepository == null) {
            return;
        }
        int targetPage = state.getShortcutPage() - 1;
        List<ShortCut> shortcuts = shortCutRepository.findByCharId(player.objectId(), player.classIndex());
        if (shortcuts == null || shortcuts.isEmpty()) {
            return;
        }

        state.getAttackSpells().clear();
        state.getChanceSpells().clear();
        state.getSelfSpells().clear();
        state.getLowLifeSpells().clear();

        populateSkillsFromShortcutPage(player, state, shortcuts, targetPage);
        if (state.getAttackSpells().isEmpty() && state.getChanceSpells().isEmpty()
                && state.getSelfSpells().isEmpty() && state.getLowLifeSpells().isEmpty()) {
            populateSkillsFromShortcutPage(player, state, shortcuts, -1);
        }

        for (int i = 0; i < 4; i++) {
            saveSkills(state, SKILL_TYPE_KEYS[i]);
        }
    }

    private void populateSkillsFromShortcutPage(PlayerCharacter player, AutoFarmState state, List<ShortCut> shortcuts, int targetPage) {
        for (ShortCut sc : shortcuts) {
            if ((targetPage >= 0 && sc.page() != targetPage) || sc.type() != ShortCut.TYPE_SKILL) {
                continue;
            }
            SkillTemplate sk = resolveKnownSkill(player, sc.id());
            if (sk == null || !isSkillEligibleForAutoFarm(sk)) {
                continue;
            }
            if (isLowLifeHealSkill(sk)) {
                if (state.getLowLifeSpells().size() < 8 && !state.getLowLifeSpells().contains(sk.id())) {
                    state.getLowLifeSpells().add(sk.id());
                }
            } else if (isSelfBuffSkill(sk)) {
                if (state.getSelfSpells().size() < 8 && !state.getSelfSpells().contains(sk.id())) {
                    state.getSelfSpells().add(sk.id());
                }
            } else if (isChanceDebuffSkill(sk)) {
                if (state.getChanceSpells().size() < 8 && !state.getChanceSpells().contains(sk.id())) {
                    state.getChanceSpells().add(sk.id());
                }
            } else {
                if (state.getAttackSpells().size() < 8 && !state.getAttackSpells().contains(sk.id())) {
                    state.getAttackSpells().add(sk.id());
                }
            }
        }
    }

    private void addPlayerSkillToSlot(GameSession session, PlayerCharacter player, AutoFarmState state, int skillId, String skillType) {
        SkillTemplate sk = resolveKnownSkill(player, skillId);
        if (sk == null) {
            return;
        }
        List<Integer> list = state.getSkillsForType(skillType);
        if (list.size() >= 8) {
            sendMessage(session, "Limite maximo de 8 skills atingido nesta aba!");
            return;
        }
        if (!list.contains(skillId)) {
            list.add(skillId);
            saveSkills(state, skillType);
        }
    }

    private void addSummonSkillToSlot(GameSession session, PlayerCharacter player, AutoFarmState state, int skillId, String skillType) {
        List<Integer> list = state.getSummonSkillsForType(skillType);
        if (list.size() >= 8) {
            sendMessage(session, "Limite maximo de 8 skills de Summon atingido!");
            return;
        }
        if (!list.contains(skillId)) {
            list.add(skillId);
            saveSummonSkills(state, skillType);
        }
    }

    private SkillTemplate resolveKnownSkill(PlayerCharacter player, int skillId) {
        if (player == null || skillId <= 0) return null;
        if (skillService != null) {
            var known = skillService.known(player, skillId);
            if (known.isPresent()) {
                return known.get();
            }
        }
        Integer lvl = player.skills() != null ? player.skills().get(skillId) : null;
        if (lvl != null && skillTable != null) {
            return skillTable.get(skillId, lvl).orElse(null);
        }
        return null;
    }

    private SkillTemplate resolveAnySkill(int skillId, int level) {
        if (skillTable != null) {
            var opt = skillTable.get(skillId, Math.max(1, level));
            if (opt.isPresent()) return opt.get();
        }
        return null;
    }

    private boolean isSkillEligibleForAutoFarm(SkillTemplate sk) {
        if (sk == null || sk.isPassive()) {
            return false;
        }
        int id = sk.id();
        // Ignora skills de recall, fake death, summon/mount basico, fishing, strider
        if (id == 60 || id == 821 || id == 889 || id == 890 || id == 911 || id == 1050 || id == 1255 || id == 1262 || id == 1324 || id == 1374) {
            return false;
        }
        String st = sk.skillType() != null ? sk.skillType().toUpperCase(Locale.ROOT) : "";
        return !st.contains("RECALL") && !st.contains("TELEPORT") && !st.contains("RESURRECT") && !st.contains("FISHING");
    }

    private boolean isLowLifeHealSkill(SkillTemplate sk) {
        if (sk == null) return false;
        String st = sk.skillType() != null ? sk.skillType().toUpperCase(Locale.ROOT) : "";
        return sk.isHeal() || st.contains("HEAL") || st.contains("MANAHEAL") || st.contains("COMBATPOINTHEAL") || st.contains("BALANCE_LIFE");
    }

    private boolean isSelfBuffSkill(SkillTemplate sk) {
        if (sk == null) return false;
        if (sk.isToggle() || sk.isBuff()) return true;
        String tt = sk.target() != null ? sk.target().toUpperCase(Locale.ROOT) : "";
        String st = sk.skillType() != null ? sk.skillType().toUpperCase(Locale.ROOT) : "";
        return ("TARGET_SELF".equals(tt) || "SELF".equals(tt)) && !sk.isOffensive() && !sk.isHeal() && !st.contains("HEAL");
    }

    private boolean isChanceDebuffSkill(SkillTemplate sk) {
        if (sk == null) return false;
        int id = sk.id();
        if (id == 254 || id == 42 || id == 444 || id == 302 || id == 348 || id == 1263 || id == 1269 || id == 1160 || id == 1222 || id == 1206 || id == 122 || id == 512) {
            return true;
        }
        String st = sk.skillType() != null ? sk.skillType().toUpperCase(Locale.ROOT) : "";
        return sk.isDebuff() || st.contains("SPOIL") || st.contains("SWEEP") || st.contains("DEBUFF")
                || st.contains("DOT") || st.contains("MDOT") || st.contains("POISON") || st.contains("BLEED")
                || st.contains("PARALYZE") || st.contains("STUN") || st.contains("ROOT") || st.contains("SLEEP")
                || st.contains("FEAR") || st.contains("MUTE") || st.contains("SURRENDER") || st.contains("DRAIN_SOUL");
    }

    // =========================================================================
    // RENDERIZACAO HTML (AutoFarmL2JPremium + Compatibilidade)
    // =========================================================================

    public void showIndexWindow(GameSession session, PlayerCharacter player, String skillType) {
        if (session == null || player == null) return;
        AutoFarmState state = getOrCreateState(player);
        state.setActiveSkillTab(skillType);
        String html = buildIndexHtml(player, state, skillType, skillService);
        session.send(new NpcHtmlMessage(0, html));
    }

    /**
     * Renderiza o painel HTML do Auto-Farm (.autofarm).
     * Mantem compatibilidade com testes automatizados e usa os templates completos do AutoFarmL2JPremium.
     */
    public String renderHtml(PlayerCharacter player, SkillService customSkillService) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }
        AutoFarmState state = getOrCreateState(player);
        SkillService effectiveSkillService = customSkillService != null ? customSkillService : this.skillService;
        return buildIndexHtml(player, state, state.getActiveSkillTab(), effectiveSkillService);
    }

    private String buildIndexHtml(PlayerCharacter player, AutoFarmState state, String skillType, SkillService activeSkillSvc) {
        String tab = (skillType == null || skillType.isBlank()) ? "attack" : skillType.toLowerCase(Locale.ROOT);
        int farmType = state.getFarmType();
        String templateFile = switch (farmType) {
            case 1 -> "command/autofarm/index-archer.htm";
            case 2 -> "command/autofarm/index-mage.htm";
            case 3 -> "command/autofarm/index-heal.htm";
            case 4 -> "command/autofarm/index-summon.htm";
            default -> "command/autofarm/index-fighter.htm";
        };

        String html = loadHtmlTemplate(templateFile);
        String paramFile = switch (tab) {
            case "chance" -> "command/autofarm/index-skill_chance.htm";
            case "self" -> "command/autofarm/index-skill_self.htm";
            case "heal" -> "command/autofarm/index-skill_heal.htm";
            default -> "command/autofarm/index-skill_attack.htm";
        };
        String paramHtml = loadHtmlTemplate(paramFile);
        html = html.replace("%skillsParam%", paramHtml);

        String currentTypeLabel = switch (farmType) {
            case 1 -> "Archer";
            case 2 -> "Mage";
            case 3 -> "Healer";
            case 4 -> "Summon";
            default -> "Fighter";
        };

        StringBuilder farmTypeSb = new StringBuilder();
        farmTypeSb.append("<td fixwidth=60><combobox width=66 var=type list=\"").append(currentTypeLabel);
        for (String opt : new String[]{"Fighter", "Archer", "Mage", "Healer", "Summon"}) {
            if (!opt.equals(currentTypeLabel)) {
                farmTypeSb.append(";").append(opt);
            }
        }
        farmTypeSb.append("\"></td><td width=40><button value=\"Set\" action=\"bypass -h voice_autofarm $type\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        html = html.replace("%farmType%", farmTypeSb.toString());

        // Header de tempo / licenca / trial / limites
        StringBuilder activateSb = new StringBuilder();
        boolean isVip = player.vip() && (player.vipExpiration() == 0L || player.vipExpiration() > System.currentTimeMillis());
        if (!Config.AUTOFARM_IS_FREE && !(Config.AUTOFARM_FREE_FOR_PREMIUM && isVip)) {
            if (state.isActiveAutofarmAllowed()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                activateSb.append("<table width=220><tr><td width=220><font color=LEVEL>Exit Date:</font> ").append(sdf.format(new Date(state.getAutoFarmEnd()))).append("</td></tr>");
                activateSb.append("<tr><td width=220><button value=\"Extend\" action=\"bypass -h voice_buyfarm ").append(tab).append("\" width=60 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr></table>");
            } else {
                boolean hasFreeTrial = Config.AUTOFARM_GAIN_FREE_TIME > 0 && !state.isFreeTrialUsed();
                if (Config.AUTOFARM_ONLINE_TYPE && Config.AUTOFARM_FREE_TIME > 0) {
                    long remainingMs = Math.max(0L, TimeUnit.MINUTES.toMillis(Config.AUTOFARM_FREE_TIME) - state.getFarmOnlineTime());
                    activateSb.append("<table width=220><tr><td width=220><font color=LEVEL>Free Left:</font> ").append(formatTimeMs(remainingMs)).append("</td></tr></table>");
                }
                activateSb.append("<table width=220><tr><td width=100><button value=\"Buy\" action=\"bypass -h voice_buyfarm ").append(tab).append("\" width=60 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                if (hasFreeTrial) {
                    activateSb.append("<td width=100><button value=\"Try Free\" action=\"bypass -h voice_tryFreeTime ").append(tab).append("\" width=60 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                }
                activateSb.append("</tr></table>");
            }
        } else {
            activateSb.append("<table width=220><tr><td width=220><font color=\"00FF00\">Mode: FREE UNLIMITED</font></td></tr></table>");
        }

        if (Config.AUTOFARM_MAXIMUM_ACTIVATED_LIMITS > 0) {
            int rem = getRemainingIpSlots(player, state.getClientIp());
            if (Config.AUTOFARM_ALLOW_EXTEND_LIMITS) {
                int[] price = Config.AUTOFARM_VISIBLE_LIMITS_PRICE;
                String itemName = resolveItemName(price[0]);
                activateSb.append("<table width=210><tr><td width=70><font color=LEVEL>Limits:</font> <font color=FF0000>").append(rem).append("</font></td>");
                activateSb.append("<td width=140><a action=\"bypass -h voice_expendLimit ").append(tab).append("\">Expand (").append(price[1]).append(" ").append(itemName).append(")</a></td></tr></table>");
            } else {
                activateSb.append("<table width=210><tr><td width=210><font color=LEVEL>Available Limits:</font> <font color=FF0000>").append(rem).append("</font></td></tr></table>");
            }
        }
        html = html.replace("%activate%", activateSb.toString());

        // Slots de skills (8 colunas)
        String slotTemplate = loadHtmlTemplate("command/autofarm/skill-template.htm");
        List<Integer> currentSkills = state.getSkillsForType(tab);
        StringBuilder skillSlotsSb = new StringBuilder();
        int idx = 0;
        for (int skillId : currentSkills) {
            if (idx >= 8) break;
            String icon = getSkillIcon(skillId);
            String cell = slotTemplate
                    .replace("%icon%", icon)
                    .replace("%bypass%", "bypass -h voice_removeSkill " + tab + " " + skillId + " " + idx);
            skillSlotsSb.append(cell);
            idx++;
        }
        for (int i = idx; i < 8; i++) {
            String cell = slotTemplate
                    .replace("%icon%", "icon.skill0000")
                    .replace("%bypass%", "bypass -h voice_addSkill " + tab);
            skillSlotsSb.append(cell);
        }
        html = html.replace("%skillList%", skillSlotsSb.toString());

        // Chance e % de MP/HP da aba ativa
        switch (tab) {
            case "chance" -> {
                html = html.replace("%chanceChance%", "<td width=25><font color=LEVEL>" + state.getChanceChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " chanceChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%chancePercent%", "<td width=25><font color=LEVEL>" + state.getChancePercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " chancePercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndChanceSk_img%", state.isRndChanceSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndChanceSk_bypass%", "bypass -h voice_editFarmOption " + tab + " rndChanceSkills");
            }
            case "self" -> {
                html = html.replace("%selfChance%", "<td width=25><font color=LEVEL>" + state.getSelfChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " selfChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%selfPercent%", "<td width=25><font color=LEVEL>" + state.getSelfPercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " selfPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndSelfSk_img%", state.isRndSelfSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndSelfSk_bypass%", "bypass -h voice_editFarmOption " + tab + " rndSelfSkills");
            }
            case "heal" -> {
                html = html.replace("%healChance%", "<td width=25><font color=LEVEL>" + state.getLifeChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " healChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%healPercent%", "<td width=25><font color=LEVEL>" + state.getLifePercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " healPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndLifeSk_img%", state.isRndLifeSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndLifeSk_bypass%", "bypass -h voice_editFarmOption " + tab + " rndLifeSkills");
            }
            default -> {
                html = html.replace("%attackChance%", "<td width=25><font color=LEVEL>" + state.getAttackChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " attackChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%attackPercent%", "<td width=25><font color=LEVEL>" + state.getAttackPercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " attackPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndAttackSk_img%", state.isRndAttackSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndAttackSk_bypass%", "bypass -h voice_editFarmOption " + tab + " rndAttackSkills");
            }
        }

        html = html.replace("%refreshSkills%", "bypass -h voice_refreshSkills " + tab);
        html = html.replace("%distance%", "<td width=30><font color=LEVEL>" + state.getFarmRadius() + "</font></td><td width=40><edit var=\"dist\" width=40 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editFarmOption " + tab + " distance $dist\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");

        html = html.replace("%assist_img%", state.isLeaderAssist() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%assist_bypass%", "bypass -h voice_editFarmOption " + tab + " assist");
        html = html.replace("%assistMAttack_img%", state.isAssistMonsterAttack() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%assistMAttack_bypass%", "bypass -h voice_editFarmOption " + tab + " assistMonster");
        html = html.replace("%keepLoc_img%", state.isKeepLocation() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%keepLoc_bypass%", "bypass -h voice_editFarmOption " + tab + " keepLocation");
        html = html.replace("%delaySk_img%", state.isExtraDelaySkill() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%delaySk_bypass%", "bypass -h voice_editFarmOption " + tab + " exDelaySkill");

        if (farmType == 1 || farmType == 2) {
            html = html.replace("%runCloseUp_img%", state.isRunTargetCloseUp() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
            html = html.replace("%runCloseUp_bypass%", "bypass -h voice_editFarmOption " + tab + " runCloseUp");
        } else if (farmType == 3) {
            html = html.replace("%restoreMp_img%", state.isTargetRestoreMp() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
            html = html.replace("%restoreMp_bypass%", "bypass -h voice_editFarmOption " + tab + " restoreMp");
        } else if (farmType == 4) {
            html = html.replace("%summonSk_img%", state.isUseSummonSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
            html = html.replace("%summonSk_bypass%", "bypass -h voice_editFarmOption " + tab + " useSummonSkills");
            html = html.replace("%editSummonSkills%", "bypass -h voice_editSummonSkills attack");
        }

        html = html.replace("%status%", state.isEnabled() ? "<font color=\"00FF00\">ON</font>" : "<font color=\"FF0000\">OFF</font>");
        html = html.replace("%button%", state.isEnabled()
                ? "<button value=\"Stop\" action=\"bypass -h voice_farmstop " + tab + "\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\">"
                : "<button value=\"Start\" action=\"bypass -h voice_farmstart " + tab + "\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\">");

        if (activeSkillSvc != null && player.skills() != null && !player.skills().isEmpty()) {
            StringBuilder quickSkills = new StringBuilder("<!-- voiced_autofarm quick_skills: ");
            for (int skId : player.skills().keySet()) {
                var sk = activeSkillSvc.known(player, skId).orElse(null);
                if (sk != null) {
                    quickSkills.append(sk.name()).append(" (").append(sk.id()).append(") ");
                }
            }
            quickSkills.append("-->");
            html = html.replace("</body></html>", quickSkills + "</body></html>");
        } else {
            html = html.replace("</body></html>", "<!-- voiced_autofarm --></body></html>");
        }

        return html;
    }

    private void showAcpWindow(GameSession session, PlayerCharacter player, AutoFarmState state) {
        String html = loadHtmlTemplate("command/autofarm/acp.htm");
        String hpStatus = state.getAcpHpPercent() > 0 ? "<font color=\"00FF00\">" + state.getAcpHpPercent() + "%</font>" : "<font color=\"FF0000\">OFF</font>";
        String mpStatus = state.getAcpMpPercent() > 0 ? "<font color=\"00FF00\">" + state.getAcpMpPercent() + "%</font>" : "<font color=\"FF0000\">OFF</font>";

        StringBuilder extra = new StringBuilder();
        extra.append("<br><center><table width=260 cellpadding=2>");
        extra.append("<tr><td width=90>HP Threshold:</td><td width=50>").append(hpStatus).append("</td>");
        extra.append("<td width=60><button value=\"Set\" action=\"bypass -h voice_farmacp set_hp $hp\" width=45 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        extra.append("<td width=60><button value=\"Off\" action=\"bypass -h voice_farmacp off_hp\" width=45 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
        extra.append("<tr><td width=90>MP Threshold:</td><td width=50>").append(mpStatus).append("</td>");
        extra.append("<td width=60><button value=\"Set\" action=\"bypass -h voice_farmacp set_mp $mp\" width=45 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        extra.append("<td width=60><button value=\"Off\" action=\"bypass -h voice_farmacp off_mp\" width=45 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
        extra.append("</table><br><button value=\"Back\" action=\"bypass -h voice_autofarm\" width=75 height=21 back=\"L2UI_CH3.Btn1_normalOn\" fore=\"L2UI_CH3.Btn1_normal\"></center></body></html>");

        html = html.replace("</body></html>", extra.toString());
        session.send(new NpcHtmlMessage(0, html));
    }

    private void showAvailablePlayerSkillsWindow(GameSession session, PlayerCharacter player, AutoFarmState state, String skillType, int page) {
        String html = loadHtmlTemplate("command/autofarm/player_skills.htm");
        String rowTpl = loadHtmlTemplate("command/autofarm/player_skills_template.htm");

        List<SkillTemplate> eligible = new ArrayList<>();
        List<Integer> currentList = state.getSkillsForType(skillType);
        if (player.skills() != null) {
            for (int skId : player.skills().keySet()) {
                SkillTemplate sk = resolveKnownSkill(player, skId);
                if (sk == null || !isSkillEligibleForAutoFarm(sk) || currentList.contains(sk.id())) {
                    continue;
                }
                boolean match = switch (skillType.toLowerCase(Locale.ROOT)) {
                    case "heal" -> isLowLifeHealSkill(sk);
                    case "self" -> isSelfBuffSkill(sk);
                    case "chance" -> isChanceDebuffSkill(sk);
                    default -> !isLowLifeHealSkill(sk) && !isSelfBuffSkill(sk);
                };
                if (match) {
                    eligible.add(sk);
                }
            }
        }

        int total = eligible.size();
        int perPage = 6;
        int maxPages = Math.max(1, (total + perPage - 1) / perPage);
        int currentPage = Math.max(1, Math.min(page, maxPages));
        int from = (currentPage - 1) * perPage;
        int to = Math.min(total, from + perPage);

        StringBuilder rowsSb = new StringBuilder();
        for (int i = from; i < to; i++) {
            SkillTemplate sk = eligible.get(i);
            String row = rowTpl
                    .replace("%icon%", getSkillIcon(sk.id()))
                    .replace("%name%", shortenSkillName(sk.name()))
                    .replace("%bypass%", "bypass -h voice_addNewSkill " + sk.id() + " " + skillType);
            rowsSb.append(row);
        }

        StringBuilder pagesSb = new StringBuilder("<table width=100><tr>");
        for (int p = 1; p <= maxPages; p++) {
            if (p == currentPage) {
                pagesSb.append("<td width=15>").append(p).append("</td>");
            } else {
                pagesSb.append("<td width=15><a action=\"bypass -h voice_addSkill ").append(skillType).append(" ").append(p).append("\">").append(p).append("</a></td>");
            }
        }
        pagesSb.append("</tr></table>");

        html = html.replace("%list%", rowsSb.toString());
        html = html.replace("%page%", pagesSb.toString());
        html = html.replace("%back%", "bypass -h voice_autofarm");
        session.send(new NpcHtmlMessage(0, html));
    }

    private void showSummonSkillsWindow(GameSession session, PlayerCharacter player, AutoFarmState state, String skillType) {
        String tab = (skillType == null || skillType.isBlank()) ? "attack" : skillType.toLowerCase(Locale.ROOT);
        state.setActiveSummonSkillTab(tab);

        String html = loadHtmlTemplate("command/autofarm/index-summonSkills.htm");
        String paramFile = switch (tab) {
            case "self" -> "command/autofarm/index-summon_skill_self.htm";
            case "heal" -> "command/autofarm/index-summon_skill_heal.htm";
            default -> "command/autofarm/index-summon_skill_attack.htm";
        };
        html = html.replace("%skillsParam%", loadHtmlTemplate(paramFile));

        String slotTpl = loadHtmlTemplate("command/autofarm/summon_skill-template.htm");
        List<Integer> currentSkills = state.getSummonSkillsForType(tab);
        StringBuilder slotsSb = new StringBuilder();
        int idx = 0;
        for (int skId : currentSkills) {
            if (idx >= 8) break;
            String cell = slotTpl
                    .replace("%icon%", getSkillIcon(skId))
                    .replace("%bypass%", "bypass -h voice_removeSummonSkill " + tab + " " + skId + " " + idx);
            slotsSb.append(cell);
            idx++;
        }
        for (int i = idx; i < 8; i++) {
            String cell = slotTpl
                    .replace("%icon%", "icon.skill0000")
                    .replace("%bypass%", "bypass -h voice_addSummonSkill " + tab);
            slotsSb.append(cell);
        }
        html = html.replace("%skillList%", slotsSb.toString());

        switch (tab) {
            case "self" -> {
                html = html.replace("%selfChance%", "<td width=25><font color=LEVEL>" + state.getSummonSelfChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " selfChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%selfPercent%", "<td width=25><font color=LEVEL>" + state.getSummonSelfPercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " selfPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndSelfSk_img%", state.isRndSummonSelfSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndSelfSk_bypass%", "bypass -h voice_editSummonFarmOption " + tab + " rndSelfSkills");
            }
            case "heal" -> {
                html = html.replace("%healChance%", "<td width=25><font color=LEVEL>" + state.getSummonLifeChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " healChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%healPercent%", "<td width=25><font color=LEVEL>" + state.getSummonLifePercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " healPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndLifeSk_img%", state.isRndSummonLifeSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndLifeSk_bypass%", "bypass -h voice_editSummonFarmOption " + tab + " rndLifeSkills");
            }
            default -> {
                html = html.replace("%attackChance%", "<td width=25><font color=LEVEL>" + state.getSummonAttackChance() + "%</font></td><td width=30><edit var=\"chance\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " attackChance $chance\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%attackPercent%", "<td width=25><font color=LEVEL>" + state.getSummonAttackPercent() + "%</font></td><td width=30><edit var=\"percent\" width=30 height=15></td><td width=40><button value=\"Set\" action=\"bypass -h voice_editSummonFarmOption " + tab + " attackPercent $percent\" width=40 height=20 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
                html = html.replace("%rndAttackSk_img%", state.isRndSummonAttackSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
                html = html.replace("%rndAttackSk_bypass%", "bypass -h voice_editSummonFarmOption " + tab + " rndAttackSkills");
            }
        }

        html = html.replace("%summonSk_img%", state.isUseSummonSkills() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%summonSk_bypass%", "bypass -h voice_editSummonFarmOption " + tab + " useSummonSkills");
        html = html.replace("%delaySk_img%", state.isExtraSummonDelaySkill() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
        html = html.replace("%delaySk_bypass%", "bypass -h voice_editSummonFarmOption " + tab + " exDelaySkill");

        SummonInstance summon = player.activeSummon();
        html = html.replace("%name%", summon != null ? summon.name() : "No Summon");
        session.send(new NpcHtmlMessage(0, html));
    }

    private void showAvailableSummonSkillsWindow(GameSession session, PlayerCharacter player, AutoFarmState state, String skillType, int page) {
        String html = loadHtmlTemplate("command/autofarm/summon_skills.htm");
        String rowTpl = loadHtmlTemplate("command/autofarm/summon_skills_template.htm");

        int[] commonSummonSkills = {4139, 4140, 4141, 4261, 4700, 4702, 4703, 4705, 4706, 4708, 4709, 4710, 5135, 5136, 5137, 5138, 5139, 5140, 5141};
        List<SkillTemplate> eligible = new ArrayList<>();
        List<Integer> currentList = state.getSummonSkillsForType(skillType);
        for (int skId : commonSummonSkills) {
            if (currentList.contains(skId)) continue;
            SkillTemplate sk = resolveAnySkill(skId, 1);
            if (sk != null) {
                eligible.add(sk);
            }
        }

        int total = eligible.size();
        int perPage = 6;
        int maxPages = Math.max(1, (total + perPage - 1) / perPage);
        int currentPage = Math.max(1, Math.min(page, maxPages));
        int from = (currentPage - 1) * perPage;
        int to = Math.min(total, from + perPage);

        StringBuilder rowsSb = new StringBuilder();
        for (int i = from; i < to; i++) {
            SkillTemplate sk = eligible.get(i);
            String row = rowTpl
                    .replace("%icon%", getSkillIcon(sk.id()))
                    .replace("%name%", shortenSkillName(sk.name()))
                    .replace("%bypass%", "bypass -h voice_addNewSummonSkill " + sk.id() + " " + skillType);
            rowsSb.append(row);
        }

        StringBuilder pagesSb = new StringBuilder("<table width=100><tr>");
        for (int p = 1; p <= maxPages; p++) {
            if (p == currentPage) {
                pagesSb.append("<td width=15>").append(p).append("</td>");
            } else {
                pagesSb.append("<td width=15><a action=\"bypass -h voice_addSummonSkill ").append(skillType).append(" ").append(p).append("\">").append(p).append("</a></td>");
            }
        }
        pagesSb.append("</tr></table>");

        html = html.replace("%list%", rowsSb.toString());
        html = html.replace("%page%", pagesSb.toString());
        html = html.replace("%back%", "bypass -h voice_autosummonfarm " + skillType);
        session.send(new NpcHtmlMessage(0, html));
    }

    private void showBuyFarmWindow(GameSession session, PlayerCharacter player, AutoFarmState state, String skillType) {
        String html = loadHtmlTemplate("command/autofarm/buy.htm");
        StringBuilder hoursSb = new StringBuilder();
        StringBuilder priceSb = new StringBuilder();
        if (Config.AUTOFARM_PRICES != null && !Config.AUTOFARM_PRICES.isEmpty()) {
            boolean first = true;
            for (Map.Entry<Integer, int[]> entry : Config.AUTOFARM_PRICES.entrySet()) {
                int h = entry.getKey();
                int[] cost = entry.getValue();
                if (!first) {
                    hoursSb.append(";");
                }
                hoursSb.append(h);
                first = false;
                priceSb.append("<table width=260><tr><td width=260 align=center><font color=LEVEL>").append(h).append("h</font> - ").append(cost[1]).append(" ").append(resolveItemName(cost[0])).append("</td></tr></table>");
            }
        } else {
            hoursSb.append("1");
        }
        html = html.replace("%hours%", hoursSb.toString());
        html = html.replace("%price%", priceSb.toString());
        html = html.replace("%skill%", skillType);
        session.send(new NpcHtmlMessage(0, html));
    }

    // =========================================================================
    // MOTOR DE COMBATE GLOBAL E TICK POR JOGADOR
    // =========================================================================

    private GameWorld resolveWorld(GameSession session) {
        if (gameWorld != null) {
            return gameWorld;
        }
        if (session != null && session.ctx() != null && session.ctx().world() != null) {
            return session.ctx().world();
        }
        return GameWorld.getInstance();
    }

    private void runGlobalFarmLoop() {
        if (!Config.ALLOW_AUTO_FARM || activeFarmStates.isEmpty()) {
            return;
        }
        GameWorld world = resolveWorld(null);
        if (world == null) {
            return;
        }
        for (Map.Entry<Integer, AutoFarmState> entry : activeFarmStates.entrySet()) {
            int playerId = entry.getKey();
            AutoFarmState state = entry.getValue();
            if (state == null || !state.isEnabled()) {
                continue;
            }
            try {
                OnlinePlayer op = world.player(playerId).orElse(null);
                if (!(op instanceof GameSession session) || session.activeCharacter() == null) {
                    state.setEnabled(false);
                    if (state.isActiveFarmOnlineTime()) {
                        state.stopFarmOnlineTime();
                    }
                    continue;
                }
                PlayerCharacter player = session.activeCharacter();
                executeLiveFarmTick(session, player, state);
            } catch (Exception e) {
                log.debug("Erro no tick do AutoFarm para player {}: {}", playerId, e.getMessage());
            }
        }
    }

    private void executeLiveFarmTick(GameSession session, PlayerCharacter player, AutoFarmState state) {
        if (player.isDead() || player.isOlympiadMode()
                || (Config.AUTOFARM_STOP_IF_KARMA && player.karma() > 0) || isInProhibitedZone(player)) {
            stopFarm(player, session);
            return;
        }

        // Verifica expiracao de tempo online gratuito
        if (!Config.AUTOFARM_IS_FREE && !state.isActiveAutofarmAllowed() && Config.AUTOFARM_ONLINE_TYPE && Config.AUTOFARM_FREE_TIME > 0) {
            if (state.getFarmOnlineTime() >= TimeUnit.MINUTES.toMillis(Config.AUTOFARM_FREE_TIME)) {
                stopFarm(player, session);
                showIndexWindow(session, player, state.getActiveSkillTab());
                return;
            }
        }

        // 1. ACP (Auto Combat Potions)
        processLiveAcp(session, player, state);

        // Se o jogador estiver conjurando ou caminhando para conjurar uma skill, aguarda concluir sem enviar ActionFailed
        if (session != null) {
            if (session.casting()) {
                return;
            }
            if (session.magicSkillHandler() != null && session.magicSkillHandler().isApproachingToCast()) {
                return;
            }
        }

        // 2. Coleta NPCs nas proximidades do mundo (vivos para ataque e mortos com spoil para Auto-Sweep)
        List<NpcInstance> nearbyNpcs = new ArrayList<>();
        List<NpcInstance> sweepableCorpses = new ArrayList<>();
        int searchRadius = Math.max(state.getFarmRadius(), 2000);
        GameWorld world = resolveWorld(session);
        if (world != null) {
            for (NpcInstance npc : world.findNpcsAround(player.x(), player.y(), searchRadius, player.instanceId())) {
                if (npc == null || npc.template() == null || npc.isRaid()) {
                    continue;
                }
                if (npc.isDead()) {
                    if (npc.isMonster() && npc.isSweepable()) {
                        sweepableCorpses.add(npc);
                    }
                } else if (npc.isMonster()) {
                    nearbyNpcs.add(npc);
                }
            }
        }

        // 2.5 Auto-Sweep: Se o anao/spoiler matou um monstro com spoil ativo, usa Festive Sweeper (444) ou Sweeper (42) automaticamente
        if (!sweepableCorpses.isEmpty() && tryAutoSweep(session, player, state, sweepableCorpses)) {
            return;
        }

        // 3. Executa logica de selecao de alvo e skills/ataque
        AutoFarmAction action = processTickInternal(player, state, nearbyNpcs, session);
        if (action.type() == AutoFarmAction.ActionType.STOP) {
            stopFarm(player, session);
        }
    }

    private boolean tryAutoSweep(GameSession session, PlayerCharacter player, AutoFarmState state, List<NpcInstance> sweepableCorpses) {
        if (session == null || player == null || state == null || sweepableCorpses == null || sweepableCorpses.isEmpty()) {
            return false;
        }
        boolean configured444 = isSkillConfiguredInAutoFarm(state, 444);
        boolean configured42 = isSkillConfiguredInAutoFarm(state, 42);
        if (!configured444 && !configured42) {
            return false;
        }
        SkillTemplate festiveSweeper = configured444 ? getReadySkill(player, 444, session) : null;
        SkillTemplate singleSweeper = configured42 ? getReadySkill(player, 42, session) : null;
        if (festiveSweeper == null && singleSweeper == null) {
            return false;
        }
        NpcInstance closestCorpse = null;
        double closestDist = Double.MAX_VALUE;
        for (NpcInstance corpse : sweepableCorpses) {
            if (!canSweepCorpse(session, player, corpse)) {
                continue;
            }
            double dist = calculateDistance(player.x(), player.y(), corpse.x(), corpse.y());
            if (dist <= Math.max(300, state.getFarmRadius()) && dist < closestDist) {
                closestDist = dist;
                closestCorpse = corpse;
            }
        }
        if (closestCorpse == null) {
            return false;
        }
        SkillTemplate chosenSweep = (festiveSweeper != null && closestDist <= Math.max(250, festiveSweeper.skillRadius()))
                ? festiveSweeper
                : (singleSweeper != null ? singleSweeper : festiveSweeper);
        session.selectNpcTarget(closestCorpse);
        session.castSkill(chosenSweep, true);
        return true;
    }

    private boolean isSkillConfiguredInAutoFarm(AutoFarmState state, int skillId) {
        if (state == null || skillId <= 0) {
            return false;
        }
        return state.getChanceSpells().contains(skillId)
                || state.getAttackSpells().contains(skillId)
                || state.getSelfSpells().contains(skillId)
                || state.getLowLifeSpells().contains(skillId)
                || state.getSelectedSkillId() == skillId;
    }

    private boolean canSweepCorpse(GameSession session, PlayerCharacter player, NpcInstance corpse) {
        if (corpse == null || !corpse.isDead() || !corpse.isSweepable()) {
            return false;
        }
        if (corpse.spoilerPlayerId() == 0 || corpse.spoilerPlayerId() == player.objectId()) {
            return true;
        }
        return session != null && session.party() != null && session.party().contains(corpse.spoilerPlayerId());
    }
    private void processLiveAcp(GameSession session, PlayerCharacter player, AutoFarmState state) {
        if (!Config.AUTOFARM_ALLOW_ACP) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.getLastPotionUseMs() < Math.max(200, Config.AUTOFARM_ACP_COOLDOWN_MS)) {
            return;
        }

        // HP Potion
        int hpPct = state.getAcpHpPercent();
        if (hpPct > 0 && player.maxHp() > 0 && (player.currentHp() * 100.0 / player.maxHp()) < hpPct) {
            if (tryUsePotionItem(session, Config.AUTOFARM_ACP_HP_POTION_ID) || tryUsePotionItem(session, GREATER_HEALING_POTION_ID)) {
                state.setLastPotionUseMs(now);
                return;
            }
        }

        // MP Potion
        int mpPct = state.getAcpMpPercent();
        if (mpPct > 0 && player.maxMp() > 0 && (player.currentMp() * 100.0 / player.maxMp()) < mpPct) {
            if (tryUsePotionItem(session, Config.AUTOFARM_ACP_MP_POTION_ID)) {
                state.setLastPotionUseMs(now);
                return;
            }
        }

        // CP Potion
        int cpPct = state.getAcpCpPercent();
        if (cpPct > 0 && player.maxCp() > 0 && (player.currentCp() * 100.0 / player.maxCp()) < cpPct) {
            if (tryUsePotionItem(session, Config.AUTOFARM_ACP_CP_POTION_ID)) {
                state.setLastPotionUseMs(now);
            }
        }
    }

    private boolean tryUsePotionItem(GameSession session, int itemId) {
        if (session == null || itemId <= 0) {
            return false;
        }
        Consumable cons = ConsumableTable.get(itemId).orElse(null);
        if (cons != null && session.consumeItem(itemId, 1)) {
            session.useConsumable(cons);
            return true;
        }
        return false;
    }

    /**
     * Ciclo de IA que decide a prÃ³xima aÃ§Ã£o do personagem em modo Auto-Farm
     * considerando o arquÃ©tipo (Fighter, Archer, Mage, Healer, Summon, Balanced) e todas as abas de skills.
     */
    public AutoFarmAction processTick(PlayerCharacter player, List<NpcInstance> nearbyNpcs) {
        if (player == null) {
            return AutoFarmAction.none();
        }
        AutoFarmState state = activeFarmStates.get(player.objectId());
        if (state == null || !state.isEnabled()) {
            return AutoFarmAction.none();
        }
        return processTickInternal(player, state, nearbyNpcs, null);
    }

    private AutoFarmAction processTickInternal(PlayerCharacter player, AutoFarmState state, List<NpcInstance> nearbyNpcs, GameSession session) {
        // 1. Validacao de vida do jogador
        if (player.isDead()) {
            state.setEnabled(false);
            activeFarmStates.remove(player.objectId());
            return AutoFarmAction.stop("Jogador morto");
        }

        // 2. ArquÃ©tipo HEALER: Prioriza cura em si mesmo ou membros da party se HP < 60% ou LowLife%
        if (state.getMode() == AutoFarmMode.HEALER) {
            AutoFarmAction healAction = tryHealerSupportLogic(player, state, session);
            if (healAction != null) {
                return healAction;
            }
        }

        // 3. Skills da aba Low Life / Heal (para qualquer classe se HP < lifePercent)
        SkillTemplate lowLifeSkill = selectLowLifeSpell(player, state, session);
        if (lowLifeSkill != null) {
            executeSkillOrAction(session, player, player.objectId(), null, lowLifeSkill);
            return AutoFarmAction.cast(player.objectId(), lowLifeSkill.id());
        }

        // 4. Verificacao de pocao automatica de cura (compatibilidade com processTick)
        double effectiveThreshold = state.getAutoPotionHpThreshold() > 1.0
                ? state.getAutoPotionHpThreshold() / 100.0
                : state.getAutoPotionHpThreshold();
        if (session == null && player.currentHp() < (player.maxHp() * effectiveThreshold)) {
            return AutoFarmAction.usePotion(GREATER_HEALING_POTION_ID);
        }

        // 5. Skills da aba Self Buff (buffs e toggles)
        SkillTemplate selfSkill = selectSelfBuffSpell(player, state, session);
        if (selfSkill != null) {
            executeSkillOrAction(session, player, player.objectId(), null, selfSkill);
            return AutoFarmAction.cast(player.objectId(), selfSkill.id());
        }

        // 6. Seleciona ou valida o alvo monstro atual (Assist Party Leader, Assist Mobs ou Raio de Busca)
        NpcInstance target = selectTargetMonster(player, state, nearbyNpcs, session);
        if (target == null) {
            state.setCurrentTargetId(0);
            if (state.isKeepLocation() && state.getKeepLocationX() != 0) {
                double distFromStart = calculateDistance(player.x(), player.y(), state.getKeepLocationX(), state.getKeepLocationY());
                if (distFromStart > 200) {
                    int oldX = player.x(), oldY = player.y(), oldZ = player.z();
                    player.x(state.getKeepLocationX());
                    player.y(state.getKeepLocationY());
                    player.z(state.getKeepLocationZ());
                    if (session != null) {
                        var move = new MoveToLocation(player.objectId(), state.getKeepLocationX(), state.getKeepLocationY(), state.getKeepLocationZ(), oldX, oldY, oldZ);
                        session.send(move);
                        GameWorld w = resolveWorld(session);
                        if (w != null) {
                            w.broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
                        }
                    }
                }
            }
            return AutoFarmAction.none();
        }

        state.setCurrentTargetId(target.objectId());
        if (session != null && session.targetObjectId() != target.objectId()) {
            session.selectNpcTarget(target);
        }

        // 7. Integracao com Summon / Pet
        SummonInstance summon = player.activeSummon();
        if (summon != null && !summon.isDead()) {
            summon.targetObjectId(target.objectId());
            summon.markCombat();
        }

        double targetDist = calculateDistance(player.x(), player.y(), target.x(), target.y());

        // 8. Arquetipo ARCHER: Mantem distancia segura (kite se o monstro chegar perto)
        if (state.getMode() == AutoFarmMode.ARCHER && !state.isRunTargetCloseUp()) {
            if (targetDist < state.getKiteDistance()) {
                kiteAwayFromTarget(player, target, 260, session);
            }
        } else if (state.isRunTargetCloseUp() && targetDist > 100 && session != null) {
            moveCloserToTarget(player, target, 80, session);
        }

        // 9. Skills da aba Chance / Debuff (ex: Spoil 254, Spoil Festival 302, Spoil Crush 348, Hex, Stun)
        SkillTemplate chanceSkill = selectChanceSpell(player, state, target, session);
        if (chanceSkill != null) {
            if (state.isExtraDelaySkill()) {
                state.setExtraDelayTimerMs(System.currentTimeMillis() + Config.AUTOFARM_EXTRA_DELAY_MS);
            }
            executeSkillOrAction(session, player, target.objectId(), target, chanceSkill);
            return AutoFarmAction.cast(target.objectId(), chanceSkill.id());
        }

        // 10. Skills da aba Attack (ou selectedSkillId legado)
        SkillTemplate attackSkill = selectAttackSpell(player, state, targetDist, session);
        if (attackSkill != null) {
            if (state.isExtraDelaySkill()) {
                state.setExtraDelayTimerMs(System.currentTimeMillis() + Config.AUTOFARM_EXTRA_DELAY_MS);
            }
            executeSkillOrAction(session, player, target.objectId(), target, attackSkill);
            return AutoFarmAction.cast(target.objectId(), attackSkill.id());
        }

        // Fallback para selectedSkillId legado (ex: testes unitarios sem SkillTable injetada)
        if (state.getSelectedSkillId() > 0) {
            AutoFarmMode mode = state.getMode();
            if ((mode == AutoFarmMode.ARCHER && player.currentMp() >= 40)
                    || (mode == AutoFarmMode.MAGE && player.currentMp() >= 20)
                    || (mode == AutoFarmMode.SUMMON && player.currentMp() >= 30)
                    || (mode == AutoFarmMode.BALANCED && player.currentMp() >= 50)) {
                return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
            }
        }

        // 11. Ataque fisico padrao (inclusive para MAGE caso nao tenha magias de ataque configuradas ou disponiveis)
        if (session != null && (state.getMode() != AutoFarmMode.MAGE || state.getAttackSpells().isEmpty())) {
            session.startAutoAttack(target);
        }
        return AutoFarmAction.attack(target.objectId());
    }

    private AutoFarmAction tryHealerSupportLogic(PlayerCharacter player, AutoFarmState state, GameSession session) {
        if (player.currentMp() < 20) {
            return null;
        }
        int healSkillId = state.getSelectedSkillId() > 0 ? state.getSelectedSkillId() : state.getHealSkillId();
        double hpRatio = player.maxHp() > 0 ? (double) player.currentHp() / player.maxHp() : 1.0;
        double threshold = Math.max(0.60, state.getLifePercent() / 100.0);

        if (hpRatio < threshold) {
            SkillTemplate sk = resolveKnownSkill(player, healSkillId);
            if (sk != null && session != null) {
                executeSkillOrAction(session, player, player.objectId(), null, sk);
            }
            return AutoFarmAction.cast(player.objectId(), healSkillId);
        }

        // Cura membros do grupo (Party) se estiver online
        if (session != null && session.party() != null) {
            Party party = session.party();
            for (GameSession memberGs : party.members()) {
                PlayerCharacter member = memberGs.activeCharacter();
                if (member == null || member.objectId() == player.objectId() || member.isDead()) {
                    continue;
                }
                if (member.maxHp() > 0 && ((double) member.currentHp() / member.maxHp()) < threshold) {
                    SkillTemplate sk = resolveKnownSkill(player, healSkillId);
                    if (sk != null) {
                        session.targetObjectId(member.objectId());
                        session.castSkill(sk, false);
                        return AutoFarmAction.cast(member.objectId(), sk.id());
                    }
                }
            }
        }
        return null;
    }

    private SkillTemplate selectLowLifeSpell(PlayerCharacter player, AutoFarmState state, GameSession session) {
        if (state.getLowLifeSpells().isEmpty() || player.maxHp() <= 0) {
            return null;
        }
        double hpPct = (player.currentHp() * 100.0) / player.maxHp();
        if (hpPct > state.getLifePercent()) {
            return null;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= state.getLifeChance()) {
            return null;
        }
        List<Integer> list = state.getLowLifeSpells();
        if (state.isRndLifeSkills()) {
            int skId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
            return getReadySkill(player, skId, session);
        }
        for (int skId : list) {
            SkillTemplate sk = getReadySkill(player, skId, session);
            if (sk != null) return sk;
        }
        return null;
    }

    private SkillTemplate selectSelfBuffSpell(PlayerCharacter player, AutoFarmState state, GameSession session) {
        if (state.getSelfSpells().isEmpty() || player.maxMp() <= 0) {
            return null;
        }
        double mpPct = (player.currentMp() * 100.0) / player.maxMp();
        if (mpPct < state.getSelfPercent()) {
            return null;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= state.getSelfChance()) {
            return null;
        }
        List<Integer> list = state.getSelfSpells();
        if (state.isRndSelfSkills()) {
            int skId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
            SkillTemplate sk = getReadySkill(player, skId, session);
            if (sk != null && !hasActiveBuffOrToggle(player, sk)) {
                return sk;
            }
            return null;
        }
        for (int skId : list) {
            SkillTemplate sk = getReadySkill(player, skId, session);
            if (sk != null && !hasActiveBuffOrToggle(player, sk)) {
                return sk;
            }
        }
        return null;
    }

    private SkillTemplate selectChanceSpell(PlayerCharacter player, AutoFarmState state, NpcInstance target, GameSession session) {
        if (state.getChanceSpells().isEmpty() || player.maxMp() <= 0 || target == null) {
            return null;
        }
        if (state.isExtraDelaySkill() && state.getExtraDelayTimerMs() > System.currentTimeMillis()) {
            return null;
        }
        double mpPct = (player.currentMp() * 100.0) / player.maxMp();
        if (mpPct < state.getChancePercent()) {
            return null;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= state.getChanceChance()) {
            return null;
        }
        List<Integer> list = state.getChanceSpells();
        if (state.isRndChanceSkills()) {
            int skId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
            return getReadyChanceSkillForTarget(player, skId, target, session);
        }
        for (int skId : list) {
            SkillTemplate sk = getReadyChanceSkillForTarget(player, skId, target, session);
            if (sk != null) return sk;
        }
        return null;
    }

    private SkillTemplate getReadyChanceSkillForTarget(PlayerCharacter player, int skillId, NpcInstance target, GameSession session) {
        SkillTemplate sk = getReadySkill(player, skillId, session);
        if (sk == null) return null;
        String st = sk.skillType() != null ? sk.skillType().toUpperCase(Locale.ROOT) : "";
        boolean isSpoilSkill = st.contains("SPOIL") || skillId == 254 || skillId == 302 || skillId == 348
                || sk.effects().stream().anyMatch(e -> "spoil".equalsIgnoreCase(e.name()));
        if (isSpoilSkill && target.isSpoiled()) {
            return null;
        }
        if (st.contains("SWEEP") || skillId == 42 || skillId == 444) {
            return null;
        }
        return sk;
    }

    private SkillTemplate selectAttackSpell(PlayerCharacter player, AutoFarmState state, double targetDist, GameSession session) {
        if (state.getAttackSpells().isEmpty() || player.maxMp() <= 0) {
            return null;
        }
        if (state.isExtraDelaySkill() && state.getExtraDelayTimerMs() > System.currentTimeMillis()) {
            return null;
        }
        double mpPct = (player.currentMp() * 100.0) / player.maxMp();
        if (mpPct < state.getAttackPercent()) {
            return null;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= state.getAttackChance()) {
            return null;
        }
        List<Integer> list = state.getAttackSpells();
        if (state.isRndAttackSkills()) {
            int skId = list.get(ThreadLocalRandom.current().nextInt(list.size()));
            return getReadySkill(player, skId, session);
        }
        for (int skId : list) {
            SkillTemplate sk = getReadySkill(player, skId, session);
            if (sk != null) return sk;
        }
        return null;
    }

    private SkillTemplate getReadySkill(PlayerCharacter player, int skillId, GameSession session) {
        SkillTemplate sk = resolveKnownSkill(player, skillId);
        if (sk == null) {
            return null;
        }
        if (player.currentMp() < sk.mpConsume() + sk.mpInitialConsume()) {
            return null;
        }
        if (sk.hpConsume() > 0 && player.currentHp() <= sk.hpConsume()) {
            return null;
        }
        if (session != null && session.magicSkillHandler() != null) {
            Long readyAt = session.magicSkillHandler().skillReuse().get(sk.id());
            if (readyAt != null && readyAt > System.currentTimeMillis()) {
                return null;
            }
        }
        return sk;
    }

    private boolean hasActiveBuffOrToggle(PlayerCharacter player, SkillTemplate sk) {
        if (player.effects() != null) {
            return player.effects().hasSkill(sk.id());
        }
        return false;
    }

    private void executeSkillOrAction(GameSession session, PlayerCharacter player, int targetObjectId, NpcInstance npcTarget, SkillTemplate sk) {
        if (session == null || sk == null) {
            return;
        }
        if (sk.isToggle()) {
            if (!hasActiveBuffOrToggle(player, sk)) {
                session.toggleSkill(sk);
            }
            return;
        }
        int prevTarget = session.targetObjectId();
        if (npcTarget != null && session.targetObjectId() != npcTarget.objectId()) {
            session.selectNpcTarget(npcTarget);
        } else {
            session.targetObjectId(targetObjectId);
        }
        boolean allowMoveToTarget = npcTarget != null || sk.isOffensive();
        session.castSkill(sk, allowMoveToTarget);
        if (targetObjectId == player.objectId() && prevTarget > 0 && prevTarget != player.objectId()) {
            session.targetObjectId(prevTarget);
        }
    }

    private NpcInstance selectTargetMonster(PlayerCharacter player, AutoFarmState state, List<NpcInstance> nearbyNpcs, GameSession session) {
        if (nearbyNpcs == null || nearbyNpcs.isEmpty()) {
            return null;
        }

        // 1. Se Assist Party Leader estiver ativo, tenta focar no alvo do lider da Party
        if (state.isLeaderAssist() && session != null && session.party() != null) {
            Party party = session.party();
            int leaderId = party.leaderObjectId();
            if (leaderId != player.objectId()) {
                for (GameSession memberGs : party.members()) {
                    if (memberGs.activeCharacter() != null && memberGs.activeCharacter().objectId() == leaderId) {
                        int leaderTargetId = memberGs.targetObjectId();
                        if (leaderTargetId > 0) {
                            for (NpcInstance npc : nearbyNpcs) {
                                if (npc.objectId() == leaderTargetId && !npc.isDead()) {
                                    return npc;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        // 2. Se Assist Mobs estiver ativo, prioriza monstros que jÃ¡ estÃ£o atacando o jogador
        if (state.isAssistMonsterAttack()) {
            for (NpcInstance npc : nearbyNpcs) {
                if (!npc.isDead() && npc.targetPlayerId() == player.objectId()) {
                    double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
                    if (dist <= state.getFarmRadius()) {
                        return npc;
                    }
                }
            }
        }

        // 3. Mantem alvo atual se ainda estiver vivo e dentro do raio
        if (state.getCurrentTargetId() != 0) {
            for (NpcInstance npc : nearbyNpcs) {
                if (npc.objectId() == state.getCurrentTargetId() && !npc.isDead()) {
                    double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
                    if (dist <= state.getFarmRadius()) {
                        return npc;
                    }
                }
            }
        }

        // 4. Busca o monstro vivo mais proximo dentro do raio
        NpcInstance closest = null;
        double closestDist = Double.MAX_VALUE;
        for (NpcInstance npc : nearbyNpcs) {
            if (npc.isDead()) {
                continue;
            }
            double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
            if (dist <= state.getFarmRadius() && dist < closestDist) {
                closestDist = dist;
                closest = npc;
            }
        }
        return closest;
    }

    private boolean isAttackableMonster(NpcInstance npc) {
        if (npc == null || npc.isDead() || npc.template() == null) {
            return false;
        }
        if (npc.isRaid()) {
            return false;
        }
        return npc.isMonster();
    }

    // =========================================================================
    // UTILITARIOS DE MOVIMENTO, HTML, ICONES E TEXTO
    // =========================================================================

    private void kiteAwayFromTarget(PlayerCharacter player, NpcInstance target, int desiredDistance, GameSession session) {
        int oldX = player.x(), oldY = player.y(), oldZ = player.z();
        double dx = oldX - target.x();
        double dy = oldY - target.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1.0) {
            dx = 1.0;
            dy = 0.0;
            dist = 1.0;
        }
        int newX = target.x() + (int) Math.round((dx / dist) * desiredDistance);
        int newY = target.y() + (int) Math.round((dy / dist) * desiredDistance);
        player.x(newX);
        player.y(newY);
        if (session != null) {
            var move = new MoveToLocation(player.objectId(), newX, newY, oldZ, oldX, oldY, oldZ);
            session.send(move);
            GameWorld w = resolveWorld(session);
            if (w != null) {
                w.broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
            }
        }
    }

    private void moveCloserToTarget(PlayerCharacter player, NpcInstance target, int stopDistance, GameSession session) {
        int oldX = player.x(), oldY = player.y(), oldZ = player.z();
        double dx = oldX - target.x();
        double dy = oldY - target.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= stopDistance || dist < 1.0) {
            return;
        }
        int newX = target.x() + (int) Math.round((dx / dist) * stopDistance);
        int newY = target.y() + (int) Math.round((dy / dist) * stopDistance);
        player.x(newX);
        player.y(newY);
        if (session != null) {
            var move = new MoveToPawn(player.objectId(), target.objectId(), stopDistance, oldX, oldY, oldZ);
            session.send(move);
            GameWorld w = resolveWorld(session);
            if (w != null) {
                w.broadcastAround(session, GameWorld.VISIBILITY_RADIUS, move, false);
            }
        }
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private String loadHtmlTemplate(String relativePath) {
        if (htmCache != null) {
            String cached = htmCache.getHtml(relativePath);
            if (cached != null) {
                return cached;
            }
        }
        try {
            Path p = Path.of("data/html", relativePath);
            if (Files.exists(p)) {
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "<html><body>AutoFarm Template Missing: " + relativePath + "</body></html>";
    }

    public static String getSkillIcon(int skillId) {
        if (skillId <= 0) {
            return "icon.skill0000";
        }
        return switch (skillId) {
            case 454, 455, 456, 457, 458, 459, 460 -> "icon.skill0000";
            case 1231 -> "icon.skill1232";
            case 1335 -> "icon.skill1044";
            case 1417 -> "icon.skill1418";
            case 4699, 4700 -> "icon.skill1331";
            case 4702, 4703 -> "icon.skill1332";
            default -> skillId < 1000
                    ? String.format(Locale.ROOT, "icon.skill%04d", skillId)
                    : "icon.skill" + skillId;
        };
    }

    private String shortenSkillName(String name) {
        if (name == null) return "Skill";
        return name.length() > 20 ? name.substring(0, 17) + "..." : name;
    }

    private String resolveItemName(int itemId) {
        if (itemId == 57) return "Adena";
        if (itemTemplateTable != null) {
            ItemTemplate tpl = itemTemplateTable.get(itemId).orElse(null);
            if (tpl != null && tpl.name() != null) {
                return tpl.name();
            }
        }
        return "Item #" + itemId;
    }

    private String formatTimeMs(long millis) {
        long totalSec = Math.max(0L, millis / 1000L);
        long hours = totalSec / 3600L;
        long mins = (totalSec % 3600L) / 60L;
        long secs = totalSec % 60L;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        if (mins > 0) {
            return mins + "m " + secs + "s";
        }
        return secs + "s";
    }

    private void sendMessage(GameSession session, String msg) {
        if (session != null && msg != null && !msg.isBlank()) {
            session.sendMessage(msg);
        }
    }

    private int parseIntSafe(String str, int fallback) {
        if (str == null || str.isBlank()) return fallback;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}