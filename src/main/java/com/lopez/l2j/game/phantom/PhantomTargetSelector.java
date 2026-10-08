package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;

import java.util.Collection;
import java.util.List;

/**
 * Componente modular desacoplado para selecao de alvos dos Phantoms e AutoFarm - Onda C8.
 */
public class PhantomTargetSelector {

	public static final double DEFAULT_HUNT_RADIUS = 1500.0;
	public static final double HEAL_HP_RATIO_TRIGGER = 0.70;

	/**
	 * Encontra o aliado mais ferido (HP < 70%) para acoes de cura/suporte.
	 */
	public PlayerCharacter selectWoundedAlly(PlayerCharacter source, Collection<PlayerCharacter> nearbyPlayers, double radius) {
		if (source == null || nearbyPlayers == null || nearbyPlayers.isEmpty()) {
			return null;
		}

		PlayerCharacter mostWounded = null;
		double lowestHpRatio = 1.0;

		// Checa primeiro se a si mesmo precisa de cura
		double selfHpRatio = source.currentHp() / Math.max(1.0, source.maxHp());
		if (selfHpRatio < HEAL_HP_RATIO_TRIGGER) {
			mostWounded = source;
			lowestHpRatio = selfHpRatio;
		}

		for (PlayerCharacter candidate : nearbyPlayers) {
			if (candidate == null || candidate.isDead() || candidate.objectId() == source.objectId()) {
				continue;
			}
			double dist = Math.hypot(candidate.x() - source.x(), candidate.y() - source.y());
			if (dist > radius) {
				continue;
			}
			double ratio = candidate.currentHp() / Math.max(1.0, candidate.maxHp());
			if (ratio < HEAL_HP_RATIO_TRIGGER && ratio < lowestHpRatio) {
				lowestHpRatio = ratio;
				mostWounded = candidate;
			}
		}

		return mostWounded;
	}

	/**
	 * Encontra o monstro vivo mais proximo dentro do raio especificado.
	 */
	public NpcInstance selectClosestMonster(PlayerCharacter source, Collection<NpcInstance> nearbyNpcs, double radius) {
		if (source == null || nearbyNpcs == null || nearbyNpcs.isEmpty()) {
			return null;
		}

		NpcInstance closest = null;
		double closestDist = Double.MAX_VALUE;

		for (NpcInstance npc : nearbyNpcs) {
			if (npc == null || npc.isDead()) {
				continue;
			}
			double dist = Math.hypot(npc.x() - source.x(), npc.y() - source.y());
			if (dist <= radius && dist < closestDist) {
				closestDist = dist;
				closest = npc;
			}
		}

		return closest;
	}

	/**
	 * Verifica se o alvo ainda e valido (existe, nao esta morto e esta dentro do alcance maximo).
	 */
	public boolean isTargetValid(PlayerCharacter source, NpcInstance target, double maxRadius) {
		if (source == null || target == null || target.isDead()) {
			return false;
		}
		double dist = Math.hypot(target.x() - source.x(), target.y() - source.y());
		return dist <= maxRadius;
	}
}
