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

    public void setMode(int playerId, AutoFarmMode mode) {
        AutoFarmState state = activeFarmStates.get(playerId);
        if (state != null) {
            state.setMode(mode);
        }
    }

    public void setSelectedSkill(int playerId, int skillId) {
        AutoFarmState state = activeFarmStates.get(playerId);
        if (state != null) {
            state.setSelectedSkillId(skillId);
        }
    }

    /**
     * Ciclo de IA que decide a próxima ação do personagem em modo Auto-Farm.
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

        // 2. Verificacao de pocao automatica de cura (se HP < threshold)
        if (player.currentHp() < (player.maxHp() * state.getAutoPotionHpThreshold())) {
            return AutoFarmAction.usePotion(GREATER_HEALING_POTION_ID);
        }

        // 3. Procura ou valida o alvo atual
        NpcInstance target = null;
        if (state.getCurrentTargetId() != 0 && nearbyNpcs != null) {
            for (NpcInstance npc : nearbyNpcs) {
                if (npc.objectId() == state.getCurrentTargetId() && !npc.isDead()) {
                    double dist = calculateDistance(player.x(), player.y(), npc.x(), npc.y());
                    if (dist <= state.getFarmRadius()) {
                        target = npc;
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
                }
            }
        }

        if (target == null) {
            state.setCurrentTargetId(0);
            return AutoFarmAction.none();
        }

        state.setCurrentTargetId(target.objectId());

        // 4. Decisao de ataque ou skill baseado no modo
        if (state.getMode() == AutoFarmMode.MAGE && state.getSelectedSkillId() > 0 && player.currentMp() >= 20) {
            return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
        } else if (state.getMode() == AutoFarmMode.BALANCED && state.getSelectedSkillId() > 0 && player.currentMp() >= 50) {
            return AutoFarmAction.cast(target.objectId(), state.getSelectedSkillId());
        } else {
            return AutoFarmAction.attack(target.objectId());
        }
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        long dx = (long) x1 - x2;
        long dy = (long) y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
