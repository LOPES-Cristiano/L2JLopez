package com.lopez.l2j.game.ai;

import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.AutoAttackStop;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToPawn;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de IA para monstros em combate: perseguicao do jogador (MoveToPawn) e ataques continuos
 * baseados na velocidade de ataque do monstro.
 */
@Service
public class NpcAiService {

	private static final Logger log = LoggerFactory.getLogger(NpcAiService.class);

	private final GameWorld world;
	private final CombatService combatService;
	private final CharTemplateTable charTemplates;

	private final Set<NpcInstance> activeCombatNpcs = ConcurrentHashMap.newKeySet();
	private ScheduledExecutorService scheduler;

	public NpcAiService(GameWorld world, CombatService combatService, CharTemplateTable charTemplates) {
		this.world = world;
		this.combatService = combatService;
		this.charTemplates = charTemplates;
	}

	@PostConstruct
	public void start() {
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "NpcAiThread");
			t.setDaemon(true);
			return t;
		});
		scheduler.scheduleAtFixedRate(this::tick, 500, 500, TimeUnit.MILLISECONDS);
		log.info("NpcAiService iniciado (tick a cada 500ms)");
	}

	@PreDestroy
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Registra ou atualiza um monstro em estado de combate perseguindo/atacando um jogador.
	 */
	public void startCombat(NpcInstance npc, int targetPlayerId) {
		if (npc == null || npc.isDead()) {
			return;
		}
		npc.targetPlayerId(targetPlayerId);
		npc.inCombat(true);

		int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
		long cooldownMs = 500_000L / pAtkSpd;
		// Atraso de reacao inicial para o player ver o proprio golpe primeiro (~700ms)
		npc.lastAttackTime(System.currentTimeMillis() - cooldownMs + 700);

		activeCombatNpcs.add(npc);
	}

	/**
	 * Remove um monstro do combate (por morte ou perda de aggro).
	 */
	public void stopCombat(NpcInstance npc) {
		if (npc != null) {
			npc.inCombat(false);
			npc.targetPlayerId(0);
			activeCombatNpcs.remove(npc);
		}
	}

	/**
	 * Executa um ciclo da IA para todos os monstros atualmente em combate.
	 */
	public void tick() {
		for (NpcInstance npc : activeCombatNpcs) {
			try {
				processNpc(npc);
			} catch (Exception e) {
				log.warn("Erro ao processar IA do monstro {}: {}", npc.name(), e.getMessage());
			}
		}
	}

	private void processNpc(NpcInstance npc) {
		if (npc.isDead() || npc.targetPlayerId() == 0) {
			stopCombat(npc);
			return;
		}

		var playerOpt = world.player(npc.targetPlayerId());
		if (playerOpt.isEmpty()) {
			stopCombat(npc);
			return;
		}

		var player = playerOpt.get();
		var character = player.character();
		if (character == null || character.isDead()) {
			stopCombat(npc);
			return;
		}

		double dx = player.x() - npc.x();
		double dy = player.y() - npc.y();
		double dist = Math.hypot(dx, dy);

		// Perda de aggro se o jogador fugir muito longe (> 1500)
		if (dist > 1500.0) {
			stopCombat(npc);
			var stopAtk = new AutoAttackStop(npc.objectId());
			player.send(stopAtk);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, stopAtk, false);
			return;
		}

		int attackRange = Math.max(40, npc.template().attackRange());
		int reach = attackRange + 30; // margem de contato físico

		// Stun/Sleep/Paralyze de skills: monstro parado sem agir
		if (npc.isDisabled()) {
			return;
		}

		if (dist > reach && npc.isRooted()) {
			return; // Root: nao persegue, mas ataca se o jogador encostar
		}

		if (dist > reach) {
			// Monstro corre atras do jogador (MoveToPawn)
			npc.running(true);
			double runSpeed = Math.max(60, npc.template().runSpd()); // unidades/s (ex: 110)
			double step = Math.min(dist - attackRange, runSpeed * 0.5); // passo para 500ms
			int newX = (int) Math.round(npc.x() + (dx / dist) * step);
			int newY = (int) Math.round(npc.y() + (dy / dist) * step);
			int heading = (int) Math.round(Math.atan2(dy, dx) * 10430.378);
			npc.moveTo(newX, newY, player.z(), heading);

			var movePawn = new MoveToPawn(npc.objectId(), player.objectId(), attackRange, npc.x(), npc.y(), npc.z());
			player.send(movePawn);
			world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, movePawn, false);
		} else {
			// Monstro esta no alcance de ataque!
			int pAtkSpd = Math.max(100, npc.template().pAtkSpd());
			long cooldownMs = 500_000L / pAtkSpd; // ex: 333 atk spd -> 1500ms
			long now = System.currentTimeMillis();

			if (now - npc.lastAttackTime() >= cooldownMs) {
				npc.lastAttackTime(now);
				var template = charTemplates.get(character.classId()).orElse(null);
				if (template != null) {
					var counter = combatService.attackPlayer(npc, character, template);
					if (counter != null) {
						var npcAtk = new Attack(npc.objectId(), character.objectId(), counter.damage(), counter.flags(),
								npc.x(), npc.y(), npc.z());
						player.send(npcAtk);
						world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, npcAtk, false);

						if (counter.damage() > 0) {
							player.send(SystemMessage.of(SystemMessage.S1_GAVE_YOU_S2_DMG,
									new SystemMessage.NpcName(npc.npcId()),
									new SystemMessage.Number(counter.damage())));
						}

						player.send(StatusUpdate.hp(character.objectId(), (int) character.currentHp(), character.maxHp()));
						player.send(new UserInfo(character, template));

						if (character.isDead()) {
							stopCombat(npc);
							var die = new Die(character.objectId(), true);
							player.send(die);
							world.broadcastAround(player, GameWorld.VISIBILITY_RADIUS, die, false);
						}
					}
				}
			}
		}
	}
}
