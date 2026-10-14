package com.lopez.l2j.game.autofarm;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico responsavel pelo Sistema de Auto-Farm (IA de Farm para Players) - Item 95.
 * Permite que o jogador ative a automação de caça para atacar monstros próximos,
 * utilizar poções de cura quando o HP estiver baixo e conjurar magias configuradas.
 */
@Service
public class AutoFarmService {

    private static final Logger log = LoggerFactory.getLogger(AutoFarmService.class);

    public static final int GREATER_HEALING_POTION_ID = 1061;

    private final Map<Integer, AutoFarmState> activeFarmStates = new ConcurrentHashMap<>();

    /**
     * Alterna o estado do Auto-Farm para o jogador (liga/desliga).
     */
    public synchronized boolean toggleAutoFarm(PlayerCharacter player) {
        if (player == null || player.isDead() || player.karma() > 0) {
            log.warn("Auto-Farm nao pode ser ativado para {}", player != null ? player.name() : "null");
            return false;
        }

        int playerId = player.objectId();
        AutoFarmState state = activeFarmStates.get(playerId);
        if (state != null && state.isEnabled()) {
            state.setEnabled(false);
            activeFarmStates.remove(playerId);
            log.info("Auto-Farm desativado para {}", player.name());
            return false;
        } else {
            AutoFarmState newState = new AutoFarmState(playerId, true);
            activeFarmStates.put(playerId, newState);
            log.info("Auto-Farm ativado para {}", player.name());
            return true;
        }
    }

    public boolean isAutoFarm(int playerId) {
        AutoFarmState state = activeFarmStates.get(playerId);
        return state != null && state.isEnabled();
    }

    public AutoFarmState getState(int playerId) {
        return activeFarmStates.get(playerId);
    }

    public AutoFarmState getOrCreateState(int playerId) {
        return activeFarmStates.computeIfAbsent(playerId, id -> new AutoFarmState(id, false));
    }

    public void setMode(int playerId, AutoFarmMode mode) {
        getOrCreateState(playerId).setMode(mode);
    }

    public void setSelectedSkill(int playerId, int skillId) {
        getOrCreateState(playerId).setSelectedSkillId(skillId);
    }

    public void setFarmRadius(int playerId, int radius) {
        getOrCreateState(playerId).setFarmRadius(radius);
    }

    public void setAutoPotionHpThreshold(int playerId, double threshold) {
        getOrCreateState(playerId).setAutoPotionHpThreshold(threshold);
    }

    /**
     * Renderiza o painel grafico interativo HTML do Auto-Farm (.autofarm)
     */
    public String renderHtml(PlayerCharacter player, com.lopez.l2j.game.skill.SkillService skillService) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }
        AutoFarmState state = getOrCreateState(player.objectId());
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<table width=270 cellpadding=2 cellspacing=0>");
        sb.append("<tr><td align=center><font color=\"LEVEL\"><b>SISTEMA DE AUTO-FARM</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Automacao de caca e assistencia de combate</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=270 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
        sb.append("<tr><td><b>Status:</b></td><td align=center>");
        if (state.isEnabled()) {
            sb.append("<font color=\"8FA888\"><b>ATIVO</b></font></td><td align=center>");
            sb.append("<button value=\"Desativar\" action=\"bypass -h voiced_autofarm toggle\" width=70 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
        } else {
            sb.append("<font color=\"B86B6B\"><b>DESLIGADO</b></font></td><td align=center>");
            sb.append("<button value=\"Ativar\" action=\"bypass -h voiced_autofarm toggle\" width=70 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
        }
        sb.append("</td></tr>");

        // Modo de combate (Arquetipos)
        sb.append("<tr><td><b>Modo:</b></td><td colspan=2 align=center>");
        sb.append(state.getMode() == AutoFarmMode.FIGHTER ? "<font color=\"8FA888\">[Fighter]</font> " : "<a action=\"bypass -h voiced_autofarm mode FIGHTER\">[Fighter]</a> ");
        sb.append(state.getMode() == AutoFarmMode.ARCHER ? "<font color=\"8FA888\">[Archer]</font> " : "<a action=\"bypass -h voiced_autofarm mode ARCHER\">[Archer]</a> ");
        sb.append(state.getMode() == AutoFarmMode.MAGE ? "<font color=\"8FA888\">[Mage]</font> " : "<a action=\"bypass -h voiced_autofarm mode MAGE\">[Mage]</a> ");
        sb.append("<br>");
        sb.append(state.getMode() == AutoFarmMode.HEALER ? "<font color=\"8FA888\">[Healer]</font> " : "<a action=\"bypass -h voiced_autofarm mode HEALER\">[Healer]</a> ");
        sb.append(state.getMode() == AutoFarmMode.SUMMON ? "<font color=\"8FA888\">[Summon]</font> " : "<a action=\"bypass -h voiced_autofarm mode SUMMON\">[Summon]</a> ");
        sb.append(state.getMode() == AutoFarmMode.BALANCED ? "<font color=\"8FA888\">[Balanced]</font>" : "<a action=\"bypass -h voiced_autofarm mode BALANCED\">[Balanced]</a>");
        sb.append("</td></tr>");

        // Raio
        sb.append("<tr><td><b>Raio:</b> <font color=\"LEVEL\">").append(state.getFarmRadius()).append("</font></td><td colspan=2 align=center>");
        sb.append("<a action=\"bypass -h voiced_autofarm radius 600\">[600]</a> ");
        sb.append("<a action=\"bypass -h voiced_autofarm radius 1000\">[1000]</a> ");
        sb.append("<a action=\"bypass -h voiced_autofarm radius 1400\">[1400]</a> ");
        sb.append("<a action=\"bypass -h voiced_autofarm radius 1800\">[1800]</a>");
        sb.append("</td></tr>");

        // Auto Potion HP
        int hpPct = (int) Math.round(state.getAutoPotionHpThreshold() * 100);
        sb.append("<tr><td><b>Pocao HP:</b> <font color=\"B86B6B\">").append(hpPct).append("%</font></td><td colspan=2 align=center>");
        sb.append("<a action=\"bypass -h voiced_autofarm hp 40\">[40%]</a> ");
        sb.append("<a action=\"bypass -h voiced_autofarm hp 60\">[60%]</a> ");
        sb.append("<a action=\"bypass -h voiced_autofarm hp 80\">[80%]</a>");
        sb.append("</td></tr>");

        // Skill selecionada para Mage/Healer/Summon/Balanced
        if (state.getMode() != AutoFarmMode.FIGHTER && state.getMode() != AutoFarmMode.ARCHER) {
            String skillName = "Nenhuma";
            if (state.getSelectedSkillId() > 0 && skillService != null) {
                var sk = skillService.known(player, state.getSelectedSkillId()).orElse(null);
                if (sk != null) {
                    skillName = sk.name();
                } else {
                    skillName = "Skill #" + state.getSelectedSkillId();
                }
            }
            sb.append("<tr><td colspan=3><b>Magia Ativa:</b> <font color=\"8FA8BE\">").append(skillName).append("</font></td></tr>");
            if (skillService != null && player.skills() != null && !player.skills().isEmpty()) {
                sb.append("<tr><td colspan=3><font color=\"AAAAAA\">Selecione uma habilidade:</font><br>");
                int count = 0;
                for (int skillId : player.skills().keySet()) {
                    var sk = skillService.known(player, skillId).orElse(null);
                    if (sk != null && (sk.magic() || sk.isOffensive()) && !sk.isPassive() && !sk.isToggle()) {
                        sb.append("<a action=\"bypass -h voiced_autofarm skill ").append(skillId).append("\">")
                          .append("[").append(sk.name()).append("]</a> ");
                        count++;
                        if (count % 2 == 0) sb.append("<br>");
                        if (count >= 8) break;
                    }
                }
                sb.append("</td></tr>");
            }
        }
        sb.append("</table><br>");

        sb.append("<table width=270>");
        sb.append("<tr><td align=center><font color=\"888888\">Comandos no chat: .autofarm | .farm</font></td></tr>");
        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Ciclo de IA que decide a próxima ação do personagem em modo Auto-Farm
     * considerando o arquetipo: FIGHTER, ARCHER, MAGE, HEALER, SUMMON, BALANCED.
     */
    public AutoFarmAction processTick(PlayerCharacter player, List<NpcInstance> nearbyNpcs) {
        if (player == null) {
            return AutoFarmAction.none();
        }

        int playerId = player.objectId();
        AutoFarmState state = activeFarmStates.get(playerId);
        if (state == null || !state.isEnabled()) {
            return AutoFarmAction.none();
        }

        // 1. Validacao de vida e postura do jogador
        if (player.isDead()) {
            state.setEnabled(false);
            activeFarmStates.remove(playerId);
            return AutoFarmAction.stop("Jogador morto");
        }

        // 2. Arquetipo HEALER: Prioriza cura se o proprio jogador estiver com HP < 60%
        if (state.getMode() == AutoFarmMode.HEALER && player.currentMp() >= 20) {
            int healSkill = state.getSelectedSkillId() > 0 ? state.getSelectedSkillId() : state.getHealSkillId();
            if (player.currentHp() < (player.maxHp() * 0.60)) {
                return AutoFarmAction.cast(player.objectId(), healSkill);
            }
        }

        // 3. Verificacao de pocao automatica de cura (se HP < threshold)
        if (player.currentHp() < (player.maxHp() * state.getAutoPotionHpThreshold())) {
            return AutoFarmAction.usePotion(GREATER_HEALING_POTION_ID);
        }

        // 4. Procura ou valida o alvo atual
        NpcInstance target = null;
        double targetDist = Double.MAX_VALUE;
        if (state.getCurrentTargetId() != 0 && nearbyNpcs != null) {
            for (NpcInstance npc : nearbyNpcs) {
                if (npc.objectId() == state.getCurrentTargetId() && !npc.isDead()) {
                    double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
                    if (dist <= state.getFarmRadius()) {
                        target = npc;
                        targetDist = dist;
                        break;
                    }
                }
            }
        }

        // Se nao tem alvo valido, busca o monstro vivo mais proximo dentro do raio
        if (target == null && nearbyNpcs != null) {
            double closestDist = Double.MAX_VALUE;
            for (NpcInstance npc : nearbyNpcs) {
                if (npc.isDead()) {
                    continue;
                }
                double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
                if (dist <= state.getFarmRadius() && dist < closestDist) {
                    closestDist = dist;
                    target = npc;
                    targetDist = dist;
                }
            }
        }

        if (target == null) {
            state.setCurrentTargetId(0);
            return AutoFarmAction.none();
        }

        state.setCurrentTargetId(target.objectId());

        // 5. Integracao com Summon / Pet: se o jogador possui Summon ativo, ordena ataque ao alvo
        if (player.activeSummon() != null && !player.activeSummon().isDead()) {
            player.activeSummon().targetObjectId(target.objectId());
            player.activeSummon().markCombat();
        }

        // 6. Arquetipo ARCHER: Mantem distancia segura (kite se o monstro chegar a menos de 120 unidades)
        if (state.getMode() == AutoFarmMode.ARCHER) {
            if (targetDist < state.getKiteDistance()) {
                kiteAwayFromTarget(player, target, 260);
            }
            if (state.getSelectedSkillId() > 0 && player.currentMp() >= 40) {
                return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
            }
            return AutoFarmAction.attack(target.objectId());
        }

        // 7. Decisao de ataque ou skill baseado no modo (MAGE, SUMMON, BALANCED, FIGHTER, HEALER)
        if (state.getMode() == AutoFarmMode.MAGE && state.getSelectedSkillId() > 0 && player.currentMp() >= 20) {
            return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
        } else if (state.getMode() == AutoFarmMode.SUMMON && state.getSelectedSkillId() > 0 && player.currentMp() >= 30) {
            return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
        } else if (state.getMode() == AutoFarmMode.BALANCED && state.getSelectedSkillId() > 0 && player.currentMp() >= 50) {
            return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
        } else {
            return AutoFarmAction.attack(target.objectId());
        }
    }

    private void kiteAwayFromTarget(PlayerCharacter player, NpcInstance target, int desiredDistance) {
        double dx = player.x() - target.x();
        double dy = player.y() - target.y();
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
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
