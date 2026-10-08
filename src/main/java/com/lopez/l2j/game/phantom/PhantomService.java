package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico central de Simulacao de Populacao (Phantoms / Fake Players) - Onda C8.
 *
 * <p>Recursos principais:</p>
 * <ul>
 *   <li>7 Kits de Combate Oficiais compartilhados diretamente com o AutoFarm.</li>
 *   <li>Comportamento modular: selecao inteligente de alvos, gestao de consumiveis e vida social/lojas em vilas.</li>
 *   <li>Execucao assincrona sob Virtual Threads (Project Loom / Java 21+).</li>
 *   <li>Isolamento de seguranca: Phantoms nao afetam rankings publicos nem economia de doacoes (zero donations).</li>
 *   <li>Controle operacional via flag {@code phantom.enabled} e comandos GM //phantom.</li>
 * </ul>
 */
@Service
public class PhantomService {

	private static final Logger log = LoggerFactory.getLogger(PhantomService.class);

	private final boolean enabled;
	private final PhantomTargetSelector targetSelector;
	private final PhantomConsumableManager consumableManager;
	private final PhantomVillageBehavior villageBehavior;
	private final AutoFarmService autoFarmService;

	private final Map<Integer, PhantomRunner> activePhantoms = new ConcurrentHashMap<>();
	private final AtomicInteger idCounter = new AtomicInteger(950000);

	@Autowired
	public PhantomService(
			@Value("${phantom.enabled:false}") boolean enabled,
			@Autowired(required = false) AutoFarmService autoFarmService) {
		this.enabled = enabled;
		this.targetSelector = new PhantomTargetSelector();
		this.consumableManager = new PhantomConsumableManager();
		this.villageBehavior = new PhantomVillageBehavior();
		this.autoFarmService = autoFarmService;

		if (this.enabled) {
			log.info("PhantomService ativado. Motor de populacao artificial pronto.");
		} else {
			log.info("PhantomService carregado em modo dormente (phantom.enabled=false).");
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int count() {
		return activePhantoms.size();
	}

	public Collection<PhantomRunner> allPhantoms() {
		return Collections.unmodifiableCollection(activePhantoms.values());
	}

	public PhantomRunner getPhantom(int objectId) {
		return activePhantoms.get(objectId);
	}

	/**
	 * Instancia e registra um novo Phantom no mundo.
	 */
	public PlayerCharacter spawnPhantom(PhantomProfile profile) {
		if (profile == null) {
			return null;
		}

		int objId = idCounter.incrementAndGet();
		PlayerCharacter player = new PlayerCharacter(
				objId,
				"acc_phantom_" + objId,
				profile.name(),
				profile.level(),
				100000000L,
				1000000,
				0,
				profile.classId(),
				profile.classId(),
				false,
				0,
				0,
				0,
				6000,
				4000,
				3000,
				0,
				10,
				0,
				0,
				"Phantom",
				0,
				System.currentTimeMillis(),
				0,
				profile.spawnX(),
				profile.spawnY(),
				profile.spawnZ(),
				0,
				6000.0,
				4000.0,
				3000.0
		);

		// Marcador estrito de isolamento de seguranca
		player.setPhantom(true);

		// Concede as skills do kit
		if (profile.kit() != null) {
			for (int skillId : profile.kit().skills()) {
				player.skills().put(skillId, 1);
			}
		}

		PhantomRunner runner = new PhantomRunner(player, profile.kit(), profile.mode(), profile.spawnX(), profile.spawnY(), profile.spawnZ(), profile.storeTitle());
		activePhantoms.put(objId, runner);

		if (enabled) {
			runner.start();
		}

		log.info("Phantom {} (ID: {}, Kit: {}, Modo: {}) registrado com sucesso.",
				player.name(), objId, profile.kit(), profile.mode());
		return player;
	}

	/**
	 * Remove um Phantom ativo e encerra sua Virtual Thread.
	 */
	public boolean despawnPhantom(int objectId) {
		PhantomRunner runner = activePhantoms.remove(objectId);
		if (runner != null) {
			runner.stop();
			log.info("Phantom {} (ID: {}) removido do mundo.", runner.player.name(), objectId);
			return true;
		}
		return false;
	}

	/**
	 * Desconecta e limpa todos os Phantoms ativos.
	 */
	public void despawnAll() {
		for (PhantomRunner runner : activePhantoms.values()) {
			runner.stop();
		}
		activePhantoms.clear();
		log.info("Todos os Phantoms foram desativados e removidos.");
	}

	/**
	 * Avalia e decide a proxima acao do Phantom no ciclo de IA.
	 * Compartilhavel diretamente com o AutoFarm.
	 */
	public PhantomAction processTick(
			PlayerCharacter phantom,
			PhantomCombatKit kit,
			PhantomBehaviorMode mode,
			Collection<NpcInstance> nearbyNpcs,
			Collection<PlayerCharacter> nearbyPlayers) {

		if (phantom == null || phantom.isDead()) {
			return PhantomAction.idle();
		}

		// 1. Modos Sociais e de Vila
		if (mode == PhantomBehaviorMode.TOWN_STORE) {
			return villageBehavior.processTownStore(phantom, phantom.storeTitle());
		} else if (mode == PhantomBehaviorMode.TOWN_ROAM) {
			return villageBehavior.processTownRoam(phantom, phantom.x(), phantom.y(), phantom.z(), 600);
		}

		// 2. Modo de Cacada / Combate (HUNTING)
		// A. Avalia consumiveis de sobrevivencia (Pocoes de HP e MP)
		PhantomAction potAction = consumableManager.checkPotions(phantom);
		if (potAction != null) {
			return potAction;
		}

		// B. Logica de Suporte e Cura (HEALER_SUPPORT)
		if (kit != null && kit.isHealer()) {
			PlayerCharacter woundedAlly = targetSelector.selectWoundedAlly(phantom, nearbyPlayers, kit.maxDistance());
			if (woundedAlly != null) {
				// Escolhe primeira skill de cura disponivel com MP suficiente
				int healSkill = kit.skills().isEmpty() ? 1218 : kit.skills().get(0); // 1218: Greater Battle Heal
				if (phantom.currentMp() >= 30) {
					return PhantomAction.cast(woundedAlly.objectId(), healSkill);
				}
			}
		}

		// C. Selecao do Alvo de Ataque (Monstro vivo mais proximo)
		NpcInstance monster = targetSelector.selectClosestMonster(phantom, nearbyNpcs, PhantomTargetSelector.DEFAULT_HUNT_RADIUS);
		if (monster == null) {
			return PhantomAction.idle();
		}

		double distance = Math.hypot(monster.x() - phantom.x(), monster.y() - phantom.y());

		// D. Logica Tatica de Kiting (ex: ARCHER_KITE recua se o monstro estiver colado)
		if (kit != null && kit.isKiting() && distance < kit.minDistance() && distance > 0) {
			int retreatX = (int) Math.round(phantom.x() + ((phantom.x() - monster.x()) / distance) * 200);
			int retreatY = (int) Math.round(phantom.y() + ((phantom.y() - monster.y()) / distance) * 200);
			return PhantomAction.kite(monster.objectId(), retreatX, retreatY, phantom.z());
		}

		// E. Uso de Habilidades Ofensivas do Kit
		if (kit != null && !kit.skills().isEmpty() && phantom.currentMp() >= 25) {
			// Seleciona uma skill ofensiva do kit
			int skillId = kit.skills().get(0);
			return PhantomAction.cast(monster.objectId(), skillId);
		}

		// F. Ataque Fisico Padrao com Soulshot
		return PhantomAction.attack(monster.objectId());
	}

	/**
	 * Permite que um jogador real com AutoFarm execute uma rotina baseada nos kits dos Phantoms.
	 */
	public PhantomAction planAutoFarmKitAction(
			PlayerCharacter player,
			PhantomCombatKit kit,
			Collection<NpcInstance> nearbyNpcs,
			Collection<PlayerCharacter> nearbyPlayers) {
		return processTick(player, kit, PhantomBehaviorMode.HUNTING, nearbyNpcs, nearbyPlayers);
	}

	public PhantomTargetSelector targetSelector() {
		return targetSelector;
	}

	public PhantomConsumableManager consumableManager() {
		return consumableManager;
	}

	public PhantomVillageBehavior villageBehavior() {
		return villageBehavior;
	}

	/**
	 * Encapsula o ciclo de vida e a Virtual Thread de um Phantom individual.
	 */
	public class PhantomRunner {
		private final PlayerCharacter player;
		private final PhantomCombatKit kit;
		private final PhantomBehaviorMode mode;
		private final int anchorX;
		private final int anchorY;
		private final int anchorZ;
		private final String storeTitle;
		private volatile boolean running;
		private Thread workerThread;

		public PhantomRunner(PlayerCharacter player, PhantomCombatKit kit, PhantomBehaviorMode mode, int anchorX, int anchorY, int anchorZ, String storeTitle) {
			this.player = player;
			this.kit = kit;
			this.mode = mode;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
			this.anchorZ = anchorZ;
			this.storeTitle = storeTitle;
		}

		public synchronized void start() {
			if (running) {
				return;
			}
			running = true;
			workerThread = Thread.ofVirtual().name("PhantomWorker-" + player.objectId()).start(this::runLoop);
		}

		public synchronized void stop() {
			running = false;
			if (workerThread != null) {
				workerThread.interrupt();
				workerThread = null;
			}
		}

		private void runLoop() {
			while (running && !player.isDead()) {
				try {
					processTick(player, kit, mode, List.of(), List.of());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					log.warn("Erro no ciclo de execucao do Phantom {}: {}", player.name(), e.getMessage());
				}
			}
		}

		public PlayerCharacter player() { return player; }
		public PhantomCombatKit kit() { return kit; }
		public PhantomBehaviorMode mode() { return mode; }
		public boolean isRunning() { return running; }
	}
}
