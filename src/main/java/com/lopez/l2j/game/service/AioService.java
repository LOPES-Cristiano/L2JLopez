package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema Completo de Buffers AIO / AIOx (AioService).
 * Suporta atribuição de status AIOx, restrição estrita a zonas de paz (Peace Zones),
 * catálogo completo de buffs de suporte, menu próprio (.aiomenu) e entrega de itens (.getaiogoods).
 */
@Service
public class AioService {

    private static final Logger log = LoggerFactory.getLogger(AioService.class);

    public record AioBuff(int skillId, int level, String name) {}

    private final Set<Integer> aioPlayers = ConcurrentHashMap.newKeySet();
    private final List<AioBuff> aioBuffs = new ArrayList<>();

    // Consumíveis entregues pelo comando .getaiogoods
    public record ConsumableReward(int itemId, int count, String name) {}
    private final List<ConsumableReward> aioGoods = new ArrayList<>();

    public AioService() {
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

    public void setAioStatus(PlayerCharacter player, boolean active) {
        if (active) {
            aioPlayers.add(player.getObjectId());
            player.setAio(true);
            log.info("AIO: Status AIOx concedido ao jogador {} [{}]", player.getName(), player.getObjectId());
        } else {
            aioPlayers.remove(player.getObjectId());
            player.setAio(false);
            log.info("AIO: Status AIOx removido do jogador {} [{}]", player.getName(), player.getObjectId());
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
        return Collections.unmodifiableList(aioBuffs);
    }

    public String buildAioMenuHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Painel de Buffer AIOx ===</font><br>");
        sb.append("Ola, <font color=\"00FFFF\">").append(player.getName()).append("</font>!<br>");
        sb.append("Utilize o menu abaixo para aplicar buffs ou retirar itens.<br><br>");
        sb.append("<button value=\"Retirar Consumiveis (.getaiogoods)\" action=\"bypass -h voiced_getaiogoods\" width=190 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><br>");
        sb.append("<table width=240>");
        for (AioBuff buff : aioBuffs) {
            sb.append("<tr>");
            sb.append("<td>").append(buff.name()).append("</td>");
            sb.append("<td><button value=\"Cast\" action=\"bypass -h voiced_aiobuff ").append(buff.skillId())
              .append("\" width=45 height=18 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        sb.append("</center></body></html>");
        return sb.toString();
    }
}
