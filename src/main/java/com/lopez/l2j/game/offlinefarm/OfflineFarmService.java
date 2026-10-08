package com.lopez.l2j.game.offlinefarm;

import com.lopez.l2j.game.autofarm.AutoFarmAction;
import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico de Offline Farm (Onda C6 - Repatriacao L2JRadical).
 *
 * <p>Regra de Negocio:</p>
 * <ul>
 *   <li>Permite que o jogador com {@code .autofarm} ativo desconecte a sessao mantendo o personagem
 *       vivo no mundo realizando a rotina de caca automatica.</li>
 *   <li>Executa a rotina de IA de forma leve sob Virtual Threads do Java 21 LTS.</li>
 *   <li>Controle estrito de limite de conexoes offline por IP (evitando sobrecarga).</li>
 *   <li>Zero Doacoes: 100% compativel com economia organica (custo opcional em Adena in-game).</li>
 *   <li>Encerra automaticamente se: personagem morrer, inventario lotar, pocoes acabarem ou comando {@code .offlinestop}.</li>
 * </ul>
 */
@Service
public class OfflineFarmService {

	private static final Logger log = LoggerFactory.getLogger(OfflineFarmService.class);

	public static final int DEFAULT_MAX_ACCOUNTS_PER_IP = 2;
	public static final int MAX_INVENTORY_SLOTS = 80;

	private final int maxPerIp;
	private final long costAdena;
	private final boolean enabled;

	private final AutoFarmService autoFarmService;

	private final Map<Integer, OfflineFarmSession> activeSessions = new ConcurrentHashMap<>();
	private final Map<String, AtomicInteger> ipCounts = new ConcurrentHashMap<>();

	public record OfflineFarmResult(boolean success, String message) {
		public static OfflineFarmResult ok(String msg) {
			return new OfflineFarmResult(true, msg);
		}

		public static OfflineFarmResult fail(String msg) {
			return new OfflineFarmResult(false, msg);
		}
	}

	public record OfflineFarmSession(
			int playerId,
			String playerName,
			String account,
			String clientIp,
			long startTime,
			AtomicBoolean running,
			Thread workerThread
	) {
	}

	public OfflineFarmService() {
		this(DEFAULT_MAX_ACCOUNTS_PER_IP, 0L, true, new AutoFarmService());
	}

	public OfflineFarmService(AutoFarmService autoFarmService) {
		this(DEFAULT_MAX_ACCOUNTS_PER_IP, 0L, true, autoFarmService);
	}

	@Autowired
	public OfflineFarmService(
			@Value("${l2.offlinefarm.max-per-ip:2}") int maxPerIp,
			@Value("${l2.offlinefarm.cost-adena:0}") long costAdena,
			@Value("${l2.offlinefarm.enabled:true}") boolean enabled,
			@Autowired(required = false) AutoFarmService autoFarmService) {
		this.maxPerIp = maxPerIp > 0 ? maxPerIp : DEFAULT_MAX_ACCOUNTS_PER_IP;
		this.costAdena = costAdena;
		this.enabled = enabled;
		this.autoFarmService = autoFarmService != null ? autoFarmService : new AutoFarmService();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getMaxPerIp() {
		return maxPerIp;
	}

	public long getCostAdena() {
		return costAdena;
	}

	public synchronized OfflineFarmResult canStart(PlayerCharacter player, String clientIp) {
		if (!enabled) {
			return OfflineFarmResult.fail("Offline Farm esta desativado no servidor.");
		}
		if (player == null || player.isDead()) {
			return OfflineFarmResult.fail("Personagem invalido ou morto.");
		}
		if (player.karma() > 0) {
			return OfflineFarmResult.fail("Personagens com Karma (PK) nao podem ativar Offline Farm.");
		}
		if (player.isOlympiadMode()) {
			return OfflineFarmResult.fail("Nao e permitido ativar Offline Farm nas Olimpiadas.");
		}

		int playerId = player.objectId();
		if (activeSessions.containsKey(playerId)) {
			return OfflineFarmResult.fail("Seu personagem ja esta em modo Offline Farm.");
		}

		if (!autoFarmService.isAutoFarm(playerId)) {
			return OfflineFarmResult.fail("Voce precisa ativar o AutoFarm primeiro com o comando .autofarm!");
		}

		String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
		int currentIpCount = ipCounts.computeIfAbsent(ip, k -> new AtomicInteger(0)).get();
		if (currentIpCount >= maxPerIp) {
			return OfflineFarmResult.fail("Limite de " + maxPerIp + " sessoes de Offline Farm por IP atingido!");
		}

		if (costAdena > 0 && player.inventory() != null) {
			var adenaItem = player.inventory().byItemId(57).orElse(null);
			if (adenaItem == null || adenaItem.count() < costAdena) {
				return OfflineFarmResult.fail("Adena insuficiente! Custo de ativacao: " + costAdena + " Adena.");
			}
		}

		if (player.inventory() != null && player.inventory().size() >= MAX_INVENTORY_SLOTS) {
			return OfflineFarmResult.fail("Seu inventario esta lotado! Libere espaco antes de ativar o Offline Farm.");
		}

		return OfflineFarmResult.ok("Validacao concluida com sucesso.");
	}

	public synchronized OfflineFarmResult startOfflineFarm(
			PlayerCharacter player,
			String clientIp,
			GameWorld world,
			CombatService combatService,
			InventoryService inventoryService) {

		OfflineFarmResult check = canStart(player, clientIp);
		if (!check.success()) {
			return check;
		}

		int playerId = player.objectId();
		String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

		if (costAdena > 0 && inventoryService != null && player.inventory() != null) {
			inventoryService.consumeItem(player.inventory(), 57, (int) costAdena, "OfflineFarmFee");
		}

		AtomicBoolean running = new AtomicBoolean(true);
		AtomicInteger ipCount = ipCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));
		ipCount.incrementAndGet();

		Thread virtualWorker = Thread.ofVirtual()
				.name("OfflineFarm-" + player.name())
				.start(() -> runFarmLoop(playerId, player, running, world, combatService, inventoryService));

		OfflineFarmSession session = new OfflineFarmSession(
				playerId, player.name(), player.accountName(), ip,
				System.currentTimeMillis(), running, virtualWorker
		);
		activeSessions.put(playerId, session);

		log.info("Offline Farm ativado com sucesso para '{}' (IP: {}, Total Ativos: {})",
				player.name(), ip, activeSessions.size());

		return OfflineFarmResult.ok("Offline Farm ativado com sucesso! Voce pode desconectar em seguranca.");
	}

	private void runFarmLoop(
			int playerId,
			PlayerCharacter player,
			AtomicBoolean running,
			GameWorld world,
			CombatService combatService,
			InventoryService inventoryService) {

		log.debug("Iniciando loop Virtual Thread de Offline Farm para {}", player.name());

		try {
			while (running.get()) {
				if (player.isDead()) {
					log.info("Offline Farm: {} morreu. Encerrando sessao.", player.name());
					break;
				}

				if (player.inventory() != null && player.inventory().size() >= MAX_INVENTORY_SLOTS) {
					log.info("Offline Farm: Inventario de {} lotou. Encerrando sessao.", player.name());
					break;
				}

				List<NpcInstance> nearbyNpcs = List.of();
				if (world != null) {
					nearbyNpcs = world.npcs().stream()
							.filter(n -> !n.isDead() && n.isAttackable())
							.filter(n -> Math.hypot(n.x() - player.x(), n.y() - player.y()) <= 1200.0)
							.toList();
				}

				AutoFarmAction action = autoFarmService.processTick(player, nearbyNpcs);
				if (action != null) {
					switch (action.type()) {
						case STOP -> {
							log.info("Offline Farm: {} recebeu ordem de parada: {}", player.name(), action.message());
							running.set(false);
						}
						case USE_POTION -> {
							int potionId = action.skillId();
							if (player.inventory() != null && player.inventory().byItemId(potionId).isPresent()) {
								if (inventoryService != null) {
									inventoryService.consumeItem(player.inventory(), potionId, 1, "OfflineFarmPotion");
								}
								player.currentHp(Math.min(player.maxHp(), player.currentHp() + 300));
								log.debug("Offline Farm: {} usou pocao {}", player.name(), potionId);
							} else {
								log.debug("Offline Farm: {} sem pocoes de cura!", player.name());
							}
						}
						case ATTACK -> {
							if (world != null && combatService != null) {
								NpcInstance target = world.npc(action.targetId()).orElse(null);
								if (target != null && !target.isDead()) {
									combatService.attackNpc(player, null, target);
								}
							}
						}
						case CAST_SKILL -> {
							if (world != null && combatService != null) {
								NpcInstance target = world.npc(action.targetId()).orElse(null);
								if (target != null && !target.isDead()) {
									combatService.skillPhysicalNpc(player, null, target, 1000.0, true, false);
								}
							}
						}
						case NONE -> {
						}
					}
				}

				Thread.sleep(1200);
			}
		} catch (InterruptedException e) {
			log.debug("Offline Farm: Worker de {} interrompido.", player.name());
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("Offline Farm: Erro inesperado na rotina de {}: {}", player.name(), e.getMessage(), e);
		} finally {
			stopOfflineFarm(playerId);
		}
	}

	public synchronized boolean stopOfflineFarm(int playerId) {
		OfflineFarmSession session = activeSessions.remove(playerId);
		if (session != null) {
			session.running().set(false);
			if (session.workerThread() != null && session.workerThread().isAlive()) {
				session.workerThread().interrupt();
			}

			String ip = session.clientIp();
			AtomicInteger count = ipCounts.get(ip);
			if (count != null) {
				int remaining = count.decrementAndGet();
				if (remaining <= 0) {
					ipCounts.remove(ip);
				}
			}

			log.info("Offline Farm desativado para {} (Ativos restantes: {})", session.playerName(), activeSessions.size());
			return true;
		}
		return false;
	}

	public boolean isOfflineFarming(int playerId) {
		return activeSessions.containsKey(playerId);
	}

	public OfflineFarmSession getSession(int playerId) {
		return activeSessions.get(playerId);
	}

	public int getActiveCount() {
		return activeSessions.size();
	}

	public int getCountByIp(String ip) {
		AtomicInteger count = ipCounts.get(ip);
		return count != null ? count.get() : 0;
	}

	public void clearAll() {
		for (var entry : activeSessions.entrySet()) {
			entry.getValue().running().set(false);
			if (entry.getValue().workerThread() != null) {
				entry.getValue().workerThread().interrupt();
			}
		}
		activeSessions.clear();
		ipCounts.clear();
	}
}
