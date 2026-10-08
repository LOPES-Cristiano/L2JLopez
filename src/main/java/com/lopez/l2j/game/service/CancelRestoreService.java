package com.lopez.l2j.game.service;

import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico de Cancel com Restauracao Temporal (CustomCancelTaskManager).
 * Repatriado da inovacao tecnica do Source L2JRadical.
 * Quando o jogador e atingido por habilidades de Cancel (ex: Cancellation 1056, Touch of Death 343),
 * os buffs expurgados sao salvos em cache temporal. Apos 15 segundos (fora das Olimpiadas),
 * se o jogador permanecer vivo e online, todos os efeitos cancelados sao automaticamente restaurados.
 */
@Service
public class CancelRestoreService {

	private static final Logger log = LoggerFactory.getLogger(CancelRestoreService.class);
	public static final int DEFAULT_RESTORE_DELAY_SECONDS = 15;
	public static final int NOBLESSE_BLESSING_SKILL_ID = 1323;

	public record CancelRestoreTask(int taskId, int playerId, List<PlayerEffects.ActiveBuff> canceledBuffs,
									long scheduledTime, long executionTime) {
	}

	private int restoreDelaySeconds = DEFAULT_RESTORE_DELAY_SECONDS;
	private final Map<Integer, CancelRestoreTask> pendingTasks = new ConcurrentHashMap<>();
	private final AtomicInteger taskIdGenerator = new AtomicInteger(1);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
			Thread.ofVirtual().name("CancelRestore-", 0).factory()
	);

	public CancelRestoreService() {}

	public int getRestoreDelaySeconds() { return restoreDelaySeconds; }
	public void setRestoreDelaySeconds(int seconds) { this.restoreDelaySeconds = Math.max(1, seconds); }

	/**
	 * Aplica a mecanica de Cancel com agendamento de restauracao no alvo.
	 *
	 * @param session sessao do jogador alvo
	 * @param maxBuffs quantidade maxima de buffs a expurgar (ex: 5)
	 * @return lista de buffs que foram cancelados
	 */
	public List<PlayerEffects.ActiveBuff> applyCancelWithRestore(GameSession session, int maxBuffsToCancel) {
		if (session == null || session.activeCharacter() == null) {
			return List.of();
		}
		var player = session.activeCharacter();
		return applyCancelWithRestore(player, session, maxBuffsToCancel);
	}

	/**
	 * Executa o cancelamento e agenda a restauracao via Virtual Thread.
	 */
	public List<PlayerEffects.ActiveBuff> applyCancelWithRestore(PlayerCharacter player, GameSession session, int maxBuffsToCancel) {
		if (player == null || player.isDead()) {
			return List.of();
		}

		List<PlayerEffects.ActiveBuff> activeBuffs = new ArrayList<>(player.effects().active());
		if (activeBuffs.isEmpty()) {
			return List.of();
		}

		// Filtra buffs protegidos (ex: Noblesse Blessing ID 1323) e permanentes
		List<PlayerEffects.ActiveBuff> eligible = new ArrayList<>();
		for (var buff : activeBuffs) {
			if (buff.skillId() != NOBLESSE_BLESSING_SKILL_ID && !buff.isPermanent()) {
				eligible.add(buff);
			}
		}

		if (eligible.isEmpty()) {
			return List.of();
		}

		// Seleciona ate maxBuffs aleatorios para cancelar
		Collections.shuffle(eligible);
		int toCancelCount = Math.min(maxBuffsToCancel, eligible.size());
		List<PlayerEffects.ActiveBuff> toCancel = new ArrayList<>(eligible.subList(0, toCancelCount));

		// Remove da lista ativa de efeitos
		for (var buff : toCancel) {
			player.effects().remove(buff);
		}

		// Atualiza interface do cliente
		if (session != null) {
			session.sendMagicEffectIcons();
			session.send(new CreatureSay(0, CreatureSay.ALL, "Combate",
					String.format("%d efeito(s) foram cancelados e serao restaurados em %d segundos.",
							toCancel.size(), restoreDelaySeconds)));
		}

		// Agenda a restauracao em 15 segundos
		int taskId = taskIdGenerator.getAndIncrement();
		long now = System.currentTimeMillis();
		long restoreAt = now + (restoreDelaySeconds * 1000L);
		CancelRestoreTask task = new CancelRestoreTask(taskId, player.objectId(), toCancel, now, restoreAt);
		pendingTasks.put(taskId, task);

		scheduler.schedule(() -> restoreTaskExecution(taskId, player, session), restoreDelaySeconds, TimeUnit.SECONDS);

		log.info("CancelRestore: Agendada restauracao de {} buffs para player {} (Task #{}) em {}s",
				toCancel.size(), player.name(), taskId, restoreDelaySeconds);

		return toCancel;
	}

	/**
	 * Execucao do callback de restauracao dos buffs.
	 */
	public void restoreTaskExecution(int taskId, PlayerCharacter player, GameSession session) {
		CancelRestoreTask task = pendingTasks.remove(taskId);
		if (task == null) {
			return;
		}

		// Regra de Negocio Radical: Se o jogador estiver morto ou nas Olimpiadas, o cancel torna-se definitivo
		if (player.isDead()) {
			log.info("CancelRestore: Player {} morreu. Restauracao de buffs descartada.", player.name());
			return;
		}

		if (player.isOlympiadMode()) {
			log.info("CancelRestore: Player {} esta em combate nas Olimpiadas. Restauracao anulada conforme regra retail.", player.name());
			return;
		}

		long now = System.currentTimeMillis();
		int restoredCount = 0;

		for (var buff : task.canceledBuffs()) {
			// Calcula tempo restante original descontando os 15s decorridos
			long originalRemaining = buff.endTimeMillis() - task.scheduledTime();
			long newEnd = now + originalRemaining;

			PlayerEffects.ActiveBuff restoredBuff = new PlayerEffects.ActiveBuff(
					buff.skillId(),
					buff.level(),
					buff.stackType(),
					newEnd,
					buff.runSpdAdd(),
					buff.pAtkSpdMul(),
					buff.mAtkSpdMul(),
					buff.accuracyAdd(),
					buff.funcs()
			);

			player.effects().put(restoredBuff);
			restoredCount++;
		}

		if (session != null) {
			session.sendMagicEffectIcons();
			session.send(new CreatureSay(0, CreatureSay.ALL, "Combate",
					String.format("Seus %d efeito(s) cancelados retornaram!", restoredCount)));
		}

		log.info("CancelRestore: Sucesso! {} buffs restaurados para player {} (Task #{})",
				restoredCount, player.name(), taskId);
	}

	public Map<Integer, CancelRestoreTask> getPendingTasks() {
		return Collections.unmodifiableMap(pendingTasks);
	}
}
