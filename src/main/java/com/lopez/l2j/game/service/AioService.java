package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema Completo de Buffers AIO / AIOx (AioService).
 * Suporta atribuição de status AIOx com duração temporal, persistência relacional
 * nas colunas `aio` e `aio_end` da tabela `characters`, restrição estrita a zonas
 * de paz (Peace Zones), catálogo completo de buffs de suporte, menu próprio (.aiomenu)
 * e entrega de itens (.getaiogoods).
 */
@Service
public class AioService {

    private static final Logger log = LoggerFactory.getLogger(AioService.class);

    public record AioBuff(int skillId, int level, String name) {}

    private final JdbcTemplate jdbc;
    private final com.lopez.l2j.game.skill.SkillService skillService;
    private final com.lopez.l2j.game.skill.SkillTable skillTable;
    private final Set<Integer> aioPlayers = ConcurrentHashMap.newKeySet();
    private final List<AioBuff> aioBuffs = new ArrayList<>();

    // Consumíveis entregues pelo comando .getaiogoods
    public record ConsumableReward(int itemId, int count, String name) {}
    private final List<ConsumableReward> aioGoods = new ArrayList<>();

    public AioService() {
        this(null, null, null);
    }

    public AioService(@Autowired(required = false) DataSource dataSource) {
        this(dataSource, null, null);
    }

    public AioService(@Autowired(required = false) DataSource dataSource,
                      @Autowired(required = false) com.lopez.l2j.game.skill.SkillService skillService,
                      @Autowired(required = false) com.lopez.l2j.game.skill.SkillTable skillTable) {
        this.jdbc = dataSource != null ? new JdbcTemplate(dataSource) : null;
        this.skillService = skillService;
        this.skillTable = skillTable;
        initDefaultBuffs();
        initDefaultGoods();
    }

    private void initDefaultBuffs() {
        // Buffs de Prophet
        aioBuffs.add(new AioBuff(1068, 3, "Might"));
        aioBuffs.add(new AioBuff(1040, 3, "Shield"));
        aioBuffs.add(new AioBuff(1086, 2, "Haste"));
        aioBuffs.add(new AioBuff(1204, 2, "Wind Walk"));
        aioBuffs.add(new AioBuff(1059, 3, "Empower"));
        aioBuffs.add(new AioBuff(1085, 3, "Acumen"));
        aioBuffs.add(new AioBuff(1077, 3, "Focus"));
        aioBuffs.add(new AioBuff(1242, 3, "Death Whisper"));
        aioBuffs.add(new AioBuff(1035, 4, "Mental Shield"));
        aioBuffs.add(new AioBuff(1036, 2, "Magic Barrier"));
        aioBuffs.add(new AioBuff(1045, 6, "Blessed Body"));
        aioBuffs.add(new AioBuff(1048, 6, "Blessed Soul"));

        // Dances de Bladedancer
        aioBuffs.add(new AioBuff(271, 1, "Dance of the Warrior"));
        aioBuffs.add(new AioBuff(272, 1, "Dance of Inspiration"));
        aioBuffs.add(new AioBuff(274, 1, "Dance of Fire"));
        aioBuffs.add(new AioBuff(275, 1, "Dance of Fury"));
        aioBuffs.add(new AioBuff(273, 1, "Dance of the Mystic"));
        aioBuffs.add(new AioBuff(276, 1, "Dance of Concentration"));
        aioBuffs.add(new AioBuff(365, 1, "Dance of Siren"));

        // Songs de Swordsinger
        aioBuffs.add(new AioBuff(264, 1, "Song of Earth"));
        aioBuffs.add(new AioBuff(265, 1, "Song of Life"));
        aioBuffs.add(new AioBuff(267, 1, "Song of Warding"));
        aioBuffs.add(new AioBuff(268, 1, "Song of Wind"));
        aioBuffs.add(new AioBuff(269, 1, "Song of Hunter"));
        aioBuffs.add(new AioBuff(349, 1, "Song of Renewal"));
        aioBuffs.add(new AioBuff(364, 1, "Song of Champion"));

        // Chants de Warcryer / Overlord
        aioBuffs.add(new AioBuff(1388, 3, "Greater Might"));
        aioBuffs.add(new AioBuff(1389, 3, "Greater Shield"));
        aioBuffs.add(new AioBuff(1356, 1, "Prophecy of Fire"));
        aioBuffs.add(new AioBuff(1355, 1, "Prophecy of Water"));
        aioBuffs.add(new AioBuff(1357, 1, "Prophecy of Wind"));
        aioBuffs.add(new AioBuff(1363, 1, "Chant of Victory"));
    }

    private void initDefaultGoods() {
        aioGoods.add(new ConsumableReward(3031, 1000, "Spirit Ore"));
        aioGoods.add(new ConsumableReward(1785, 1000, "Soul Ore"));
        aioGoods.add(new ConsumableReward(736, 10, "Scroll of Escape"));
    }

    public boolean isAioEnabled() {
        return Config.ENABLE_AIO_SYSTEM;
    }

    public boolean isClassAllowed(int classId) {
        return Config.AIO_ALLOWED_CLASS_IDS.isEmpty() || Config.AIO_ALLOWED_CLASS_IDS.contains(classId);
    }

    public boolean canLeaveTown(PlayerCharacter player) {
        if (!player.isAio()) {
            return true;
        }
        return Config.ALLOW_AIO_LEAVE_TOWN;
    }

    public boolean canSpeakNpc(PlayerCharacter player) {
        if (!player.isAio()) {
            return true;
        }
        return Config.ALLOW_AIO_SPEAK_NPC;
    }

    public boolean canTeleport(PlayerCharacter player) {
        if (!player.isAio()) {
            return true;
        }
        return Config.ALLOW_AIO_TELEPORT;
    }

    public boolean canEquipWeapon(PlayerCharacter player, int itemId) {
        if (itemId == 9209) { // AIO Dual
            return Config.ALLOW_AIO_DUAL;
        }
        return true;
    }

    public boolean isBuffShopEnabled() {
        return Config.BUFF_SHOP_ENABLE;
    }

    public int getBuffShopMaxDays() {
        return Config.BUFF_SHOP_MAX_DAYS;
    }

    public int getDefaultBuffShopSlots() {
        return Config.DEFAULT_BUFF_SHOP_SLOTS;
    }

    /**
     * Retorna o catálogo de habilidades AIO configurado via Config.AIO_SKILLS ou o padrão.
     */
    public Map<Integer, Integer> getAioSkillMap() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        if (Config.AIO_SKILLS != null && !Config.AIO_SKILLS.isBlank()) {
            String[] pairs = Config.AIO_SKILLS.split(";");
            for (String pair : pairs) {
                String clean = pair.trim();
                if (clean.isEmpty()) {
                    continue;
                }
                String[] parts = clean.split(",");
                if (parts.length >= 2) {
                    try {
                        int skillId = Integer.parseInt(parts[0].trim());
                        int level = Integer.parseInt(parts[1].trim());
                        if (skillId > 0 && level > 0) {
                            map.put(skillId, level);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (map.isEmpty()) {
            for (AioBuff buff : aioBuffs) {
                map.put(buff.skillId(), buff.level());
            }
        }
        return map;
    }

    public boolean isAioSkill(int skillId) {
        return getAioSkillMap().containsKey(skillId);
    }

    /**
     * Concede ao jogador todas as habilidades de buff do catálogo AIO.
     * Ajusta o nível caso a configuração exceda o nível máximo válido no servidor.
     */
    public void rewardAioSkills(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        Map<Integer, Integer> skills = getAioSkillMap();
        for (Map.Entry<Integer, Integer> entry : skills.entrySet()) {
            int skillId = entry.getKey();
            int desiredLevel = entry.getValue();
            int maxLevel = skillTable != null ? skillTable.maxLevel(skillId) : desiredLevel;
            int actualLevel = desiredLevel;
            if (maxLevel > 0 && desiredLevel > maxLevel && desiredLevel < 100) {
                actualLevel = maxLevel;
            }
            if (skillService != null) {
                skillService.addSkill(player, skillId, actualLevel);
            } else {
                player.skills().put(skillId, actualLevel);
            }
        }
        if (skillService != null) {
            skillService.refreshPassives(player);
        }
        log.info("AIO: {} habilidades concedidas ao jogador {} [{}]",
                skills.size(), player.getName(), player.getObjectId());
    }

    /**
     * Remove do jogador todas as habilidades exclusivas de AIO.
     */
    public void removeAioSkills(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        Map<Integer, Integer> skills = getAioSkillMap();
        for (int skillId : skills.keySet()) {
            if (skillService != null) {
                skillService.removeSkill(player, skillId);
            } else {
                player.skills().remove(skillId);
            }
        }
        if (skillService != null) {
            skillService.cleanInvalidSkills(player);
            skillService.refreshPassives(player);
        }
        log.info("AIO: Habilidades de AIO removidas do jogador {} [{}]",
                player.getName(), player.getObjectId());
    }

    /**
     * Concede status AIOx a um jogador online por uma quantidade de dias.
     * Se o jogador já possuir tempo de AIO ativo, os novos dias são somados.
     *
     * @param player jogador alvo
     * @param days   dias adicionais (se <= 0, permanente)
     * @return timestamp de expiração em milissegundos
     */
    public long setAio(PlayerCharacter player, int days) {
        if (player == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long expiration;
        if (days > 0) {
            long current = player.aioExpiration();
            long base = (player.isAio() && current > now) ? current : now;
            expiration = base + (days * 86_400_000L);
        } else {
            expiration = 0L;
        }

        aioPlayers.add(player.getObjectId());
        player.setAio(true);
        player.aioExpiration(expiration);
        applyAioColors(player);
        rewardAioSkills(player);
        saveAio(player.objectId(), 1, expiration);

        log.info("AIO: Status AIOx concedido ao jogador {} [{}] por {} dias (expira em {})",
                player.getName(), player.getObjectId(), days, expiration);
        return expiration;
    }

    /**
     * Concede status AIOx a um personagem offline no banco de dados.
     */
    public long setAio(int charId, int days) {
        long now = System.currentTimeMillis();
        long current = getAioExpiration(charId);
        long expiration;
        if (days > 0) {
            long base = (current > now) ? current : now;
            expiration = base + (days * 86_400_000L);
        } else {
            expiration = 0L;
        }

        saveAio(charId, 1, expiration);
        log.info("AIO: Status AIOx offline concedido ao charId={} por {} dias (expira em {})",
                charId, days, expiration);
        return expiration;
    }

    /**
     * Remove status AIOx de um jogador online.
     */
    public void removeAio(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        aioPlayers.remove(player.getObjectId());
        player.setAio(false);
        player.aioExpiration(0L);
        player.nameColor(0xFFFFFF);
        player.titleColor(0xFFFF77);
        removeAioSkills(player);
        if (Config.ENABLE_AIO_DELEVEL && Config.AIO_SET_DELEVEL > 0) {
            player.level(Config.AIO_SET_DELEVEL);
        }
        saveAio(player.objectId(), 0, 0L);
        log.info("AIO: Status AIOx removido do jogador {} [{}]", player.getName(), player.getObjectId());
    }

    /**
     * Remove status AIOx de um personagem offline no banco.
     */
    public void removeAio(int charId) {
        aioPlayers.remove(charId);
        if (jdbc != null) {
            Map<Integer, Integer> skills = getAioSkillMap();
            for (int skillId : skills.keySet()) {
                try {
                    jdbc.update("DELETE FROM character_skills WHERE charId = ? AND skill_id = ?", charId, skillId);
                } catch (Exception ignored) {}
            }
        }
        saveAio(charId, 0, 0L);
        log.info("AIO: Status AIOx offline removido do charId={}", charId);
    }

    /**
     * Persiste nas colunas `aio` e `aio_end` da tabela characters.
     */
    public void saveAio(int charId, int aio, long expiration) {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.update("UPDATE characters SET aio = ?, aio_end = ? WHERE charId = ?", aio, expiration, charId);
        } catch (Exception e) {
            log.warn("AIO: Erro ao persistir AIO para charId={}: {}", charId, e.getMessage());
        }
    }

    /**
     * Consulta aio_end no banco de dados.
     */
    public long getAioExpiration(int charId) {
        if (jdbc == null) {
            return 0L;
        }
        try {
            List<Long> results = jdbc.query("SELECT aio_end FROM characters WHERE charId = ? AND aio = 1",
                    (rs, rowNum) -> rs.getLong("aio_end"), charId);
            return results.isEmpty() ? 0L : results.get(0);
        } catch (Exception e) {
            log.warn("AIO: Erro ao buscar aio_end para charId={}: {}", charId, e.getMessage());
            return 0L;
        }
    }

    /**
     * Carrega e valida o status AIO ao entrar no mundo.
     */
    public void loadAio(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        if (player.isAio()) {
            long exp = player.aioExpiration();
            if (exp > 0 && exp <= System.currentTimeMillis()) {
                removeAio(player);
            } else {
                aioPlayers.add(player.getObjectId());
                applyAioColors(player);
                rewardAioSkills(player);
            }
        }
    }

    public void applyAioColors(PlayerCharacter player) {
        if (player == null) {
            return;
        }
        if (Config.ALLOW_AIO_NAME_COLOR && Config.AIO_NAME_COLOR != null && !Config.AIO_NAME_COLOR.isBlank()) {
            try {
                player.nameColor(Integer.decode("0x" + Config.AIO_NAME_COLOR));
            } catch (Exception ignored) {}
        }
        if (Config.ALLOW_AIO_TITLE_COLOR && Config.AIO_TITLE_COLOR != null && !Config.AIO_TITLE_COLOR.isBlank()) {
            try {
                player.titleColor(Integer.decode("0x" + Config.AIO_TITLE_COLOR));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Método retrocompatível com código e testes existentes.
     */
    public void setAioStatus(PlayerCharacter player, boolean active) {
        if (active) {
            setAio(player, 0);
        } else {
            removeAio(player);
        }
    }

    public boolean isAio(int objectId) {
        return aioPlayers.contains(objectId);
    }

    /**
     * Valida restrição territorial: AIOx só pode usar skills em zonas de paz.
     */
    public boolean canCastBuffs(PlayerCharacter player, boolean isInsidePeaceZone) {
        if (!player.isAio()) {
            return true;
        }
        return isInsidePeaceZone;
    }

    public List<ConsumableReward> getAioGoods() {
        return Collections.unmodifiableList(aioGoods);
    }

    public List<AioBuff> getAioBuffs() {
        Map<Integer, Integer> map = getAioSkillMap();
        List<AioBuff> list = new ArrayList<>();
        for (var entry : map.entrySet()) {
            int skillId = entry.getKey();
            int level = entry.getValue();
            String name = resolveSkillName(skillId, level);
            list.add(new AioBuff(skillId, level, name));
        }
        return Collections.unmodifiableList(list);
    }

    private String resolveSkillName(int skillId, int level) {
        if (skillTable != null) {
            int maxLvl = skillTable.maxLevel(skillId);
            int lookupLvl = maxLvl > 0 ? Math.min(level, maxLvl) : level;
            var opt = skillTable.get(skillId, lookupLvl);
            if (opt.isPresent() && opt.get().name() != null && !opt.get().name().isBlank()) {
                return opt.get().name();
            }
        }
        for (AioBuff b : aioBuffs) {
            if (b.skillId() == skillId) {
                return b.name();
            }
        }
        return "Skill " + skillId;
    }

    public String buildAioMenuHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Painel de Buffer AIOx ===</font><br>");
        sb.append("Ola, <font color=\"8FA8BE\">").append(player.getName()).append("</font>!<br>");
        sb.append("Utilize o menu abaixo para aplicar buffs ou retirar itens.<br><br>");
        sb.append("<button value=\"Retirar Consumiveis (.getaiogoods)\" action=\"bypass -h voiced_getaiogoods\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br><br>");
        sb.append("<table width=240>");
        for (AioBuff buff : getAioBuffs()) {
            sb.append("<tr>");
            sb.append("<td>").append(buff.name()).append("</td>");
            sb.append("<td><button value=\"Cast\" action=\"bypass -h voiced_aiobuff ").append(buff.skillId())
              .append("\" width=45 height=18 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        sb.append("</center></body></html>");
        return sb.toString();
    }

    public record AioPackageOption(int tier, int days, int itemId, int itemCount, String coinText) {}

    public List<AioPackageOption> getAvailablePackages() {
        String coin = getCoinText();
        int itemId = Config.AIO_ITEM_ID;
        List<AioPackageOption> packages = new ArrayList<>(3);
        if (Config.AIO_DIAS > 0) {
            packages.add(new AioPackageOption(1, Config.AIO_DIAS, itemId, Config.AIO_ITEM_COUNT, coin));
        }
        if (Config.AIO_DIAS_2 > 0) {
            packages.add(new AioPackageOption(2, Config.AIO_DIAS_2, itemId, Config.AIO_ITEM_COUNT_2, coin));
        }
        if (Config.AIO_DIAS_3 > 0) {
            packages.add(new AioPackageOption(3, Config.AIO_DIAS_3, itemId, Config.AIO_ITEM_COUNT_3, coin));
        }
        return packages;
    }

    public int getPackageDays(int tier) {
        return switch (tier) {
            case 1 -> Config.AIO_DIAS;
            case 2 -> Config.AIO_DIAS_2;
            case 3 -> Config.AIO_DIAS_3;
            default -> 0;
        };
    }

    public int getPackageCost(int tier) {
        return switch (tier) {
            case 1 -> Config.AIO_ITEM_COUNT;
            case 2 -> Config.AIO_ITEM_COUNT_2;
            case 3 -> Config.AIO_ITEM_COUNT_3;
            default -> 0;
        };
    }

    public int getRequiredItemId() {
        return Config.AIO_ITEM_ID;
    }

    public String getCoinText() {
        return (Config.AIO_COIN_TEXT != null && !Config.AIO_COIN_TEXT.isBlank())
                ? Config.AIO_COIN_TEXT.trim()
                : "Coin";
    }

    public List<String> getAllowedClassNames() {
        String raw = Config.AIO_CLASSES_NAME;
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public boolean isClassAllowedForAio(String className) {
        List<String> allowed = getAllowedClassNames();
        if (allowed.isEmpty()) {
            return true;
        }
        if (className == null || className.isBlank()) {
            return false;
        }
        for (String c : allowed) {
            if (c.equalsIgnoreCase(className.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean isOfflineBuffAllowed() {
        return Config.ALLOW_OFFLINE_BUFF;
    }

    public boolean purchaseAioPackage(PlayerCharacter player, int tier, String className) {
        if (player == null || player.isDead() || player.isOlympiadMode()) {
            return false;
        }
        int days = getPackageDays(tier);
        int cost = getPackageCost(tier);
        int itemId = Config.AIO_ITEM_ID;
        if (days <= 0 || cost < 0 || itemId <= 0) {
            return false;
        }
        if (className != null && !className.isBlank() && !isClassAllowedForAio(className)) {
            log.info("AIOx Seller: classe '{}' nao permitida para {}", className, player.name());
            return false;
        }
        if (cost > 0) {
            var inv = player.inventory();
            if (inv == null) {
                return false;
            }
            var currency = inv.byItemId(itemId).orElse(null);
            if (currency == null || currency.count() < cost) {
                return false;
            }
            if (!inv.destroyItemByItemId(itemId, cost)) {
                return false;
            }
        }

        long now = System.currentTimeMillis();
        long baseTime = (player.isAio() && player.aioExpiration() > now) ? player.aioExpiration() : now;
        long newExpiration = baseTime + (days * 86_400_000L);
        player.setAio(true);
        player.aioExpiration(newExpiration);
        player.rewardAioSkills();

        if (Config.ALLOW_AIO_NAME_COLOR && Config.AIO_NAME_COLOR != null && !Config.AIO_NAME_COLOR.isBlank()) {
            try {
                player.nameColor(Integer.decode("0x" + Config.AIO_NAME_COLOR.trim()));
            } catch (NumberFormatException ignored) {}
        }
        if (Config.ALLOW_AIO_TITLE_COLOR && Config.AIO_TITLE_COLOR != null && !Config.AIO_TITLE_COLOR.isBlank()) {
            try {
                player.titleColor(Integer.decode("0x" + Config.AIO_TITLE_COLOR.trim()));
            } catch (NumberFormatException ignored) {}
        }
        log.info("Jogador {} adquiriu pacote AIOx tier {} ({} dias por {} {})",
                player.name(), tier, days, cost, getCoinText());
        return true;
    }

    public String buildLoginAnnouncement(PlayerCharacter player) {
        if (player == null || !player.isAio() || !Config.ANNOUNCE_AIOX_LOGIN) {
            return null;
        }
        return "O jogador AIOx " + player.name() + " acabou de entrar no mundo!";
    }

    public String renderAioSellerHtml(PlayerCharacter player) {
        String coin = getCoinText();
        List<String> classes = getAllowedClassNames();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== AIOx Seller ===</font><br>");
        if (!classes.isEmpty()) {
            sb.append("<font color=\"AAAAAA\">Classes elegiveis: ")
                    .append(String.join(", ", classes))
                    .append("</font><br><br>");
        }
        for (AioPackageOption pkg : getAvailablePackages()) {
            sb.append(String.format(java.util.Locale.ROOT,
                    "<button value=\"%d Dias (%d %s)\" action=\"bypass -h aio_buy %d\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br1>",
                    pkg.days(), pkg.itemCount(), coin, pkg.tier()));
        }
        sb.append("</center></body></html>");
        return sb.toString();
    }
}
