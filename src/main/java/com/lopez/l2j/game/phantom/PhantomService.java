package com.lopez.l2j.game.phantom;

import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.Attack;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.DeleteObject;
import com.lopez.l2j.network.game.packet.GameServerPacket.Die;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.MoveToLocation;
import com.lopez.l2j.network.game.packet.GameServerPacket.PrivateStoreMsgSell;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
	 * Instancia e registra um novo Phantom no mundo 3D (GameWorld), transmitindo CharInfo aos jogadores próximos.
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
		if (profile.storeTitle() != null && !profile.storeTitle().isBlank()) {
			player.storeTitle(profile.storeTitle());
		}

		// Concede as skills do kit
		if (profile.kit() != null) {
			for (int skillId : profile.kit().skills()) {
				player.skills().put(skillId, 1);
			}
		}

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.add(new GameWorld.PhantomOnlinePlayer(player));
			world.broadcast(new CharInfo(player), op ->
					op.character() != null && op.objectId() != player.objectId()
							&& op.instanceId() == player.instanceId()
							&& Math.hypot(op.x() - player.x(), op.y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
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
	 * Remove um Phantom ativo, encerra sua Virtual Thread e remove seu avatar do GameWorld.
	 */
	public boolean despawnPhantom(int objectId) {
		PhantomRunner runner = activePhantoms.remove(objectId);
		if (runner != null) {
			runner.stop();
			GameWorld world = GameWorld.getInstance();
			if (world != null) {
				world.removeById(objectId);
				world.broadcast(new DeleteObject(objectId), op -> true);
			}
			log.info("Phantom {} (ID: {}) removido do mundo.", runner.player.name(), objectId);
			return true;
		}
		return false;
	}

	/**
	 * Desconecta e limpa todos os Phantoms ativos.
	 */
	public void despawnAll() {
		GameWorld world = GameWorld.getInstance();
		for (PhantomRunner runner : activePhantoms.values()) {
			runner.stop();
			if (world != null) {
				int oid = runner.player.objectId();
				world.removeById(oid);
				world.broadcast(new DeleteObject(oid), op -> true);
			}
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
					GameWorld world = GameWorld.getInstance();
					Collection<NpcInstance> npcs = List.of();
					List<PlayerCharacter> players = new ArrayList<>();
					if (world != null) {
						npcs = world.findNpcsAround(player.x(), player.y(), (int) PhantomTargetSelector.DEFAULT_HUNT_RADIUS, player.instanceId());
						for (GameWorld.OnlinePlayer op : world.players()) {
							if (op.character() != null && op.character().objectId() != player.objectId()
									&& op.character().instanceId() == player.instanceId()
									&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= PhantomTargetSelector.DEFAULT_HUNT_RADIUS) {
								players.add(op.character());
							}
						}
					}

					PhantomAction action = processTick(player, kit, mode, npcs, players);
					executeWorldAction(action, world);
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					log.warn("Erro no ciclo de execucao do Phantom {}: {}", player.name(), e.getMessage());
				}
			}
		}

		private void executeWorldAction(PhantomAction action, GameWorld world) {
			if (action == null || world == null) {
				return;
			}
			switch (action.type()) {
				case MOVE_TO, KITE_RETREAT -> {
					int ox = player.x();
					int oy = player.y();
					int oz = player.z();
					player.x(action.x());
					player.y(action.y());
					player.z(action.z());
					MoveToLocation mv = new MoveToLocation(player.objectId(), action.x(), action.y(), action.z(), ox, oy, oz);
					world.broadcast(mv, op -> op.character() != null
							&& op.character().instanceId() == player.instanceId()
							&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
				}
				case ATTACK -> {
					world.npc(action.targetId()).ifPresent(npc -> {
						if (!npc.isDead()) {
							int dmg = Math.max(25, player.level() * 5);
							npc.applyDamage(dmg);
							Attack atk = new Attack(player.objectId(), npc.objectId(), dmg, Attack.HITFLAG_USESS,
									player.x(), player.y(), player.z());
							StatusUpdate su = StatusUpdate.hp(npc.objectId(), Math.max(0, (int) npc.currentHp()), (int) npc.maxHp());
							world.broadcast(atk, op -> op.character() != null
									&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
							world.broadcast(su, op -> op.character() != null
									&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
							if (npc.isDead()) {
								world.broadcast(new Die(npc.objectId(), false), op -> op.character() != null
										&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
							}
						}
					});
				}
				case CAST_SKILL -> {
					player.currentMp(Math.max(0, player.currentMp() - 20));
					var tgtNpc = world.npc(action.targetId()).orElse(null);
					int tx = tgtNpc != null ? tgtNpc.x() : player.x();
					int ty = tgtNpc != null ? tgtNpc.y() : player.y();
					int tz = tgtNpc != null ? tgtNpc.z() : player.z();
					MagicSkillUse msu = new MagicSkillUse(player.objectId(), action.targetId(), action.skillId(), 1, 1000, 1500,
							player.x(), player.y(), player.z(), tx, ty, tz);
					world.broadcast(msu, op -> op.character() != null
							&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
					world.npc(action.targetId()).ifPresent(npc -> {
						if (!npc.isDead()) {
							int dmg = Math.max(40, player.level() * 8);
							npc.applyDamage(dmg);
							if (npc.isDead()) {
								world.broadcast(new Die(npc.objectId(), false), op -> op.character() != null
										&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
							}
						}
					});
				}
				case SIT_STORE -> {
					if (player.privateStoreType() != PlayerCharacter.STORE_SELL) {
						player.privateStoreType(PlayerCharacter.STORE_SELL);
						String msg = action.message() != null ? action.message() : "Phantom Store";
						player.storeTitle(msg);
						world.broadcast(new PrivateStoreMsgSell(player.objectId(), msg), op -> op.character() != null
								&& Math.hypot(op.character().x() - player.x(), op.character().y() - player.y()) <= GameWorld.VISIBILITY_RADIUS);
					}
				}
				default -> {}
			}
		}

		public PlayerCharacter player() { return player; }
		public PhantomCombatKit kit() { return kit; }
		public PhantomBehaviorMode mode() { return mode; }
		public boolean isRunning() { return running; }
	}
}
