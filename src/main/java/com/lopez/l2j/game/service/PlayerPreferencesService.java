package com.lopez.l2j.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menu de Preferências do Jogador (.menu / Configurator).
 * Toggles interativos no HTML para:
 * - ativar/desativar autoloot
 * - permitir/recusar pedidos de troca (trade)
 * - bloquear buffs externos de terceiros (blockbuff)
 * - bloquear convites de grupo (party)
 * - alternar/travar ganho de experiência (EXP lock)
 * Persiste preferências através do CharacterVariablesService.
 */
@Service
public class PlayerPreferencesService {

    public static class Preferences {
        private boolean autoLoot = true;
        private boolean tradeRefusal = false;
        private boolean blockBuffs = false;
        private boolean blockParty = false;
        private boolean blockExp = false;
        private boolean blockPm = false;

        public boolean isAutoLoot() { return autoLoot; }
        public void setAutoLoot(boolean autoLoot) { this.autoLoot = autoLoot; }
        public boolean isTradeRefusal() { return tradeRefusal; }
        public void setTradeRefusal(boolean tradeRefusal) { this.tradeRefusal = tradeRefusal; }
        public boolean isBlockTrade() { return tradeRefusal; }
        public void setBlockTrade(boolean blockTrade) { this.tradeRefusal = blockTrade; }
        public boolean isBlockPm() { return blockPm; }
        public void setBlockPm(boolean blockPm) { this.blockPm = blockPm; }
        public boolean isBlockBuffs() { return blockBuffs; }
        public void setBlockBuffs(boolean blockBuffs) { this.blockBuffs = blockBuffs; }
        public boolean isBlockParty() { return blockParty; }
        public void setBlockParty(boolean blockParty) { this.blockParty = blockParty; }
        public boolean isBlockExp() { return blockExp; }
        public void setBlockExp(boolean blockExp) { this.blockExp = blockExp; }
    }

    public void savePreferences(Preferences prefs) {
        // Preferences are persisted in memory and via characterVariables on toggle
    }

    private final CharacterVariablesService characterVariables;
    private final Map<Integer, Preferences> preferencesMap = new ConcurrentHashMap<>();

    public PlayerPreferencesService() {
        this(null);
    }

    @Autowired(required = false)
    public PlayerPreferencesService(CharacterVariablesService characterVariables) {
        this.characterVariables = characterVariables;
    }

    public Preferences getPreferences(int playerId) {
        return preferencesMap.computeIfAbsent(playerId, k -> {
            Preferences p = new Preferences();
            if (characterVariables != null) {
                p.setAutoLoot(characterVariables.getBoolean(playerId, "pref_autoloot", true));
                p.setTradeRefusal(characterVariables.getBoolean(playerId, "pref_traderefusal", false));
                p.setBlockBuffs(characterVariables.getBoolean(playerId, "pref_blockbuff", false));
                p.setBlockParty(characterVariables.getBoolean(playerId, "pref_blockparty", false));
                p.setBlockExp(characterVariables.getBoolean(playerId, "pref_blockexp", false));
            }
            return p;
        });
    }

    public boolean toggleAutoLoot(int playerId) {
        Preferences p = getPreferences(playerId);
        p.setAutoLoot(!p.isAutoLoot());
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_autoloot", p.isAutoLoot());
        }
        return p.isAutoLoot();
    }

    public boolean toggleTradeRefusal(int playerId) {
        Preferences p = getPreferences(playerId);
        p.setTradeRefusal(!p.isTradeRefusal());
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_traderefusal", p.isTradeRefusal());
        }
        return p.isTradeRefusal();
    }

    public boolean toggleBlockBuffs(int playerId) {
        Preferences p = getPreferences(playerId);
        p.setBlockBuffs(!p.isBlockBuffs());
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockbuff", p.isBlockBuffs());
        }
        return p.isBlockBuffs();
    }

    public boolean toggleBlockParty(int playerId) {
        Preferences p = getPreferences(playerId);
        p.setBlockParty(!p.isBlockParty());
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockparty", p.isBlockParty());
        }
        return p.isBlockParty();
    }

    public boolean toggleBlockExp(int playerId) {
        Preferences p = getPreferences(playerId);
        p.setBlockExp(!p.isBlockExp());
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockexp", p.isBlockExp());
        }
        return p.isBlockExp();
    }

    public void setAutoLoot(int playerId, boolean val) {
        Preferences p = getPreferences(playerId);
        p.setAutoLoot(val);
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_autoloot", val);
        }
    }

    public void setTradeRefusal(int playerId, boolean val) {
        Preferences p = getPreferences(playerId);
        p.setTradeRefusal(val);
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_traderefusal", val);
        }
    }

    public void setBlockBuffs(int playerId, boolean val) {
        Preferences p = getPreferences(playerId);
        p.setBlockBuffs(val);
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockbuff", val);
        }
    }

    public void setBlockParty(int playerId, boolean val) {
        Preferences p = getPreferences(playerId);
        p.setBlockParty(val);
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockparty", val);
        }
    }

    public void setBlockExp(int playerId, boolean val) {
        Preferences p = getPreferences(playerId);
        p.setBlockExp(val);
        if (characterVariables != null) {
            characterVariables.set(playerId, "pref_blockexp", val);
        }
    }

    public String buildMenuHtml(int playerId, String playerName) {
        Preferences p = getPreferences(playerId);
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">=== Configuracoes Pessoais (.menu) ===</font><br>");
        sb.append("Jogador: <font color=\"8FA8BE\">").append(playerName).append("</font><br><br>");

        sb.append("<table width=260>");

        // AutoLoot
        sb.append("<tr><td>Auto Loot:</td><td>");
        sb.append(p.isAutoLoot() ? "<font color=\"8FA888\">ATIVADO</font>" : "<font color=\"B86B6B\">DESATIVADO</font>");
        sb.append("</td><td><button value=\"Alternar\" action=\"bypass -h voiced_menutoggle autoloot\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

        // Trade Refusal
        sb.append("<tr><td>Recusar Trade:</td><td>");
        sb.append(p.isTradeRefusal() ? "<font color=\"B86B6B\">RECUSANDO</font>" : "<font color=\"8FA888\">ACEITANDO</font>");
        sb.append("</td><td><button value=\"Alternar\" action=\"bypass -h voiced_menutoggle trade\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

        // Block Buffs
        sb.append("<tr><td>Bloquear Buffs:</td><td>");
        sb.append(p.isBlockBuffs() ? "<font color=\"B86B6B\">BLOQUEADO</font>" : "<font color=\"8FA888\">PERMITIDO</font>");
        sb.append("</td><td><button value=\"Alternar\" action=\"bypass -h voiced_menutoggle blockbuff\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

        // Block Party
        sb.append("<tr><td>Recusar Party:</td><td>");
        sb.append(p.isBlockParty() ? "<font color=\"B86B6B\">RECUSANDO</font>" : "<font color=\"8FA888\">ACEITANDO</font>");
        sb.append("</td><td><button value=\"Alternar\" action=\"bypass -h voiced_menutoggle party\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

        // Block Exp
        sb.append("<tr><td>Trava de EXP:</td><td>");
        sb.append(p.isBlockExp() ? "<font color=\"B86B6B\">TRAVADO</font>" : "<font color=\"8FA888\">NORMAL</font>");
        sb.append("</td><td><button value=\"Alternar\" action=\"bypass -h voiced_menutoggle exp\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");

        sb.append("</table>");
        sb.append("</center></body></html>");
        return sb.toString();
    }
}
