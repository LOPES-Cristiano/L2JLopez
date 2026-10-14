package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.springframework.stereotype.Service;

/**
 * Cores de Título e Nome por Abates PvP (PvPColorSystem).
 * Atualiza dinamicamente a cor do nome e título do jogador conforme as faixas de abates:
 * 50 kills, 100 kills, 150 kills, 250 kills e 500 kills.
 */
@Service
public class PvPColorService {

    public static final int DEFAULT_NAME_COLOR = 0xFFFFFF;
    public static final int DEFAULT_TITLE_COLOR = 0xFFFF77;

    private boolean enabled = true;
    private boolean affectName = true;
    private boolean affectTitle = true;

    // Faixas oficiais de PvP do Interlude
    private int amount1 = 50;
    private int amount2 = 100;
    private int amount3 = 150;
    private int amount4 = 250;
    private int amount5 = 500;

    // Cores BGR/RGB em formato hex int
    private int nameColor1 = 0x00FF00;  // Verde
    private int nameColor2 = 0x00FFFF;  // Amarelo
    private int nameColor3 = 0xFF00FF;  // Roxo
    private int nameColor4 = 0xFFA500;  // Laranja
    private int nameColor5 = 0xFF0000;  // Vermelho

    private int titleColor1 = 0x00FF00;
    private int titleColor2 = 0x00FFFF;
    private int titleColor3 = 0xFF00FF;
    private int titleColor4 = 0xFFA500;
    private int titleColor5 = 0xFF0000;

    public PvPColorService() {}

    /**
     * Calcula a cor do nome baseada na contagem de abates PvP usando a cor padrão (0xFFFFFF).
     */
    public int getNameColor(int pvpKills) {
        return getNameColor(pvpKills, DEFAULT_NAME_COLOR);
    }

    /**
     * Calcula a cor do nome baseada na contagem de abates PvP.
     */
    public int getNameColor(int pvpKills, int defaultColor) {
        if (!enabled || !affectName) {
            return defaultColor;
        }
        if (pvpKills >= amount5) {
            return nameColor5;
        } else if (pvpKills >= amount4) {
            return nameColor4;
        } else if (pvpKills >= amount3) {
            return nameColor3;
        } else if (pvpKills >= amount2) {
            return nameColor2;
        } else if (pvpKills >= amount1) {
            return nameColor1;
        }
        return defaultColor;
    }

    /**
     * Calcula a cor do título baseada na contagem de abates usando a cor padrão (0xFFFF77).
     */
    public int getTitleColor(int kills) {
        return getTitleColor(kills, DEFAULT_TITLE_COLOR);
    }

    /**
     * Calcula a cor do título baseada na contagem de abates PvP/PK.
     */
    public int getTitleColor(int pvpKills, int defaultColor) {
        if (!enabled || !affectTitle) {
            return defaultColor;
        }
        if (pvpKills >= amount5) {
            return titleColor5;
        } else if (pvpKills >= amount4) {
            return titleColor4;
        } else if (pvpKills >= amount3) {
            return titleColor3;
        } else if (pvpKills >= amount2) {
            return titleColor2;
        } else if (pvpKills >= amount1) {
            return titleColor1;
        }
        return defaultColor;
    }

    /**
     * Calcula e aplica dinamicamente as cores de nome (baseada em pvpKills)
     * e de título (baseada em pkKills, com fallback para pvpKills caso pkKills não atinja a faixa) no jogador.
     */
    public void applyColors(PlayerCharacter player) {
        if (player == null || !enabled) {
            return;
        }
        if (affectName) {
            player.nameColor(getNameColor(player.pvpKills(), player.nameColor()));
        }
        if (affectTitle) {
            int killsForTitle = player.pkKills() >= amount1 ? player.pkKills()
                    : (player.pvpKills() >= amount1 ? player.pvpKills() : player.pkKills());
            player.titleColor(getTitleColor(killsForTitle, player.titleColor()));
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAffectName() { return affectName; }
    public void setAffectName(boolean affectName) { this.affectName = affectName; }
    public boolean isAffectTitle() { return affectTitle; }
    public void setAffectTitle(boolean affectTitle) { this.affectTitle = affectTitle; }
    public int getAmount1() { return amount1; }
    public int getAmount2() { return amount2; }
    public int getAmount3() { return amount3; }
    public int getAmount4() { return amount4; }
    public int getAmount5() { return amount5; }
    public int getNameColor1() { return nameColor1; }
    public int getNameColor2() { return nameColor2; }
    public int getNameColor3() { return nameColor3; }
    public int getNameColor4() { return nameColor4; }
    public int getNameColor5() { return nameColor5; }
    public int getTitleColor1() { return titleColor1; }
    public int getTitleColor2() { return titleColor2; }
    public int getTitleColor3() { return titleColor3; }
    public int getTitleColor4() { return titleColor4; }
    public int getTitleColor5() { return titleColor5; }
}
