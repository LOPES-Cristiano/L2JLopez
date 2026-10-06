package com.lopez.l2j.game.fishing;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingEnd;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingHpRegen;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingStart;
import com.lopez.l2j.network.game.packet.GameServerPacket.ExFishingStartCombat;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico central de pesca (Fishing / L2Fishing do legado L2JDream).
 * Controla varas, iscas, o minigame de combate de pesca, habilidades Pumping/Reeling
 * e recompensas de peixes e campeonato.
 */
@Service
public class FishingService {

	private static final Logger log = LoggerFactory.getLogger(FishingService.class);

	// Varas de pesca conhecidas
	public static final Set<Integer> FISHING_RODS = Set.of(
			6529, // Baby Duck Rod
			6530, // Albatross Rod
			6531, // Pelican Rod
			6532, // Kingfisher Rod
			6533, // Cygnus Pole
			6534  // Triton Pole
	);

	// Iscas comuns (verdes, roxas, amarelas de varios graus)
	public static final Set<Integer> NORMAL_LURES = Set.of(
			6519, 6520, 6521, 6522, 6523, 6524, 6525, 6526, 6527
	);

	// Iscas noturnas luminosas
	public static final Set<Integer> NIGHT_LURES = Set.of(
			8505, 8506, 8507, 8508, 8509, 8510, 8511, 8512, 8513
	);

	// Iscas de premio
	public static final Set<Integer> PRIZE_LURES = Set.of(
			7610, 7611, 7612, 7613, 7807, 7808, 7809, 8484, 8485, 8486, 8548
	);

	// Fishshots
	public static final Set<Integer> FISHSHOTS = Set.of(
			6535, 6536, 6537, 6538, 6539, 6540
	);

	public static final int PROOF_OF_CATCHING_A_FISH = 7609;

	private final FishTable fishTable;
	private final FishingChampionshipService championship;
	private final InventoryService inventoryService;
	private final ScheduledExecutorService scheduler;

	private final Map<Integer, FishingSession> sessions = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
	private Consumer<GameServerPacket> packetBroadcaster = p -> {};

	@Autowired
	public FishingService(FishTable fishTable,
						  FishingChampionshipService championship,
						  @Autowired(required = false) InventoryService inventoryService) {
		this(fishTable, championship, inventoryService, Executors.newSingleThreadScheduledExecutor());
	}

	public FishingService(FishTable fishTable,
						  FishingChampionshipService championship,
						  InventoryService inventoryService,
						  ScheduledExecutorService scheduler) {
		this.fishTable = fishTable;
		this.championship = championship;
		this.inventoryService = inventoryService;
		this.scheduler = scheduler;
	}

	public void setPacketBroadcaster(Consumer<GameServerPacket> broadcaster) {
		this.packetBroadcaster = broadcaster;
	}

	public static boolean isFishingRod(ItemInstance item) {
		if (item == null || item.template() == null) {
			return false;
		}
		return FISHING_RODS.contains(item.itemId()) || "rod".equalsIgnoreCase(item.template().subType());
	}

	public static boolean isLure(int itemId) {
		return NORMAL_LURES.contains(itemId) || NIGHT_LURES.contains(itemId) || PRIZE_LURES.contains(itemId);
	}

	public static boolean isNightLure(int itemId) {
		return NIGHT_LURES.contains(itemId);
	}

	public boolean isFishing(int playerId) {
		return sessions.containsKey(playerId);
	}

	public Optional<FishingSession> getSession(int playerId) {
		return Optional.ofNullable(sessions.get(playerId));
	}

	/**
	 * Inicia o arremesso da linha de pesca.
	 */
	public boolean startFishing(PlayerCharacter player, int x, int y, int z, Consumer<GameServerPacket> clientSender) {
		if (player == null || player.isDead()) {
			return false;
		}

		// Se ja esta pescando, cancela
		if (isFishing(player.objectId())) {
			stopFishing(player, clientSender);
			return false;
		}

		var inv = player.inventory();
		if (inv == null) {
			return false;
		}

		// Verifica vara de pescar equipada na mao direita
		var weapon = inv.paperdoll(ItemSlots.RHAND);
		if (weapon == null || !isFishingRod(weapon)) {
			clientSender.accept(SystemMessage.id(SystemMessage.FISHING_POLE_NOT_EQUIPPED));
			return false;
		}

		// Verifica isca no slot esquerdo ou inventario
		var lure = inv.paperdoll(ItemSlots.LHAND);
		if (lure == null || !isLure(lure.itemId())) {
			// Procura no inventario se nao estiver equipada na mao esquerda
			lure = inv.items().stream().filter(i -> isLure(i.itemId())).findFirst().orElse(null);
		}

		if (lure == null) {
			clientSender.accept(SystemMessage.id(SystemMessage.BAIT_ON_HOOK_BEFORE_FISHING));
			return false;
		}

		// Consome 1 isca
		if (inventoryService != null) {
			inventoryService.consumeItem(inv, lure.itemId(), 1, "Fishing");
		} else {
			inv.destroyItemByItemId(lure.itemId(), 1);
		}

		boolean nightLure = isNightLure(lure.itemId());
		int fishGroup = ThreadLocalRandom.current().nextInt(100) < 50 ? 1 : (ThreadLocalRandom.current().nextBoolean() ? 0 : 2);
		int fishLevel = Math.max(1, Math.min(27, player.level() / 3));
		int fishType = ThreadLocalRandom.current().nextInt(3); // 0 = fat/pudgy, 1 = nimble, 2 = ugly

		FishData fish = fishTable.getFish(fishLevel, fishType, fishGroup).orElse(null);
		if (fish == null) {
			clientSender.accept(SystemMessage.id(SystemMessage.CANNOT_FISH_HERE));
			return false;
		}

		player.isFishing(true);
		player.setFishCoordinates(x, y, z);

		FishingSession session = new FishingSession(
				player.objectId(), fish, x, y, z, nightLure, fishGroup == 0, fishGroup == 2
		);
		sessions.put(player.objectId(), session);

		// Envia pacotes iniciais de pesca
		var startPacket = new ExFishingStart(player.objectId(), fish.type(), x, y, z, nightLure);
		clientSender.accept(startPacket);
		packetBroadcaster.accept(startPacket);
		clientSender.accept(SystemMessage.id(SystemMessage.CAST_LINE_AND_START_FISHING));

		// Agenda o momento em que o peixe morde a isca (3 a 6 segundos para fluidez)
		int waitSeconds = Math.max(2, Math.min(6, fish.waitTime() / 5000));
		ScheduledFuture<?> biteTask = scheduler.schedule(() -> {
			startCombat(player, session, clientSender);
		}, waitSeconds, TimeUnit.SECONDS);

		tasks.put(player.objectId(), biteTask);
		return true;
	}

	private void startCombat(PlayerCharacter player, FishingSession session, Consumer<GameServerPacket> clientSender) {
		if (!sessions.containsKey(player.objectId()) || player.isDead()) {
			return;
		}

		session.startCombat();
		var combatPacket = new ExFishingStartCombat(
				player.objectId(),
				session.remainingSeconds(),
				session.maxHp(),
				session.combatMode(),
				session.lureType(),
				session.deceptiveMode()
		);
		clientSender.accept(combatPacket);
		packetBroadcaster.accept(combatPacket);
		clientSender.accept(SystemMessage.id(SystemMessage.GOT_A_BITE));

		// Loop de combate a cada 1 segundo
		ScheduledFuture<?> combatTicker = scheduler.scheduleAtFixedRate(() -> {
			try {
				if (!sessions.containsKey(player.objectId()) || player.isDead()) {
					stopFishing(player, clientSender);
					return;
				}

				session.tickAi();

				var regenPacket = new ExFishingHpRegen(
						player.objectId(),
						session.remainingSeconds(),
						session.curHp(),
						session.combatMode(),
						session.goodUse(),
						session.anim(),
						session.penalty(),
						session.deceptiveMode()
				);
				clientSender.accept(regenPacket);

				if (session.isFinished()) {
					if (session.isWon()) {
						winFishing(player, session, clientSender);
					} else {
						loseFishing(player, session, clientSender);
					}
				}
			} catch (Exception e) {
				log.error("Erro no loop de combate da pesca: {}", e.getMessage(), e);
				stopFishing(player, clientSender);
			}
		}, 1, 1, TimeUnit.SECONDS);

		cancelTask(player.objectId());
		tasks.put(player.objectId(), combatTicker);
	}

	/**
	 * Execucao da habilidade Pumping (puxar a vara com forca).
	 */
	public void handlePumping(PlayerCharacter player, int skillLevel, int power, Consumer<GameServerPacket> clientSender) {
		FishingSession session = sessions.get(player.objectId());
		if (session == null || session.state() != FishingSession.State.COMBAT) {
			clientSender.accept(SystemMessage.id(SystemMessage.CAN_USE_PUMPING_ONLY_WHILE_FISHING));
			return;
		}

		int pen = 0;
		int dmg = power;
		session.usePumping(dmg, pen);

		if (session.goodUse() == 1) {
			clientSender.accept(SystemMessage.of(SystemMessage.PUMPING_SUCCESFUL_S1_DAMAGE, new SystemMessage.Number(dmg)));
		} else if (session.goodUse() == 2) {
			clientSender.accept(SystemMessage.of(SystemMessage.FISH_RESISTED_PUMPING_S1_HP_REGAINED, new SystemMessage.Number(dmg)));
		} else {
			clientSender.accept(SystemMessage.id(SystemMessage.FISH_RESISTED_ATTEMPT_TO_BRING_IT_IN));
		}

		var regenPacket = new ExFishingHpRegen(
				player.objectId(), session.remainingSeconds(), session.curHp(),
				session.combatMode(), session.goodUse(), session.anim(), pen, session.deceptiveMode()
		);
		clientSender.accept(regenPacket);

		if (session.isFinished()) {
			if (session.isWon()) {
				winFishing(player, session, clientSender);
			} else {
				loseFishing(player, session, clientSender);
			}
		}
	}

	/**
	 * Execucao da habilidade Reeling (recolher a linha).
	 */
	public void handleReeling(PlayerCharacter player, int skillLevel, int power, Consumer<GameServerPacket> clientSender) {
		FishingSession session = sessions.get(player.objectId());
		if (session == null || session.state() != FishingSession.State.COMBAT) {
			clientSender.accept(SystemMessage.id(SystemMessage.CAN_USE_REELING_ONLY_WHILE_FISHING));
			return;
		}

		int pen = 0;
		int dmg = power;
		session.useReeling(dmg, pen);

		if (session.goodUse() == 1) {
			clientSender.accept(SystemMessage.of(SystemMessage.REELING_SUCCESFUL_S1_DAMAGE, new SystemMessage.Number(dmg)));
		} else if (session.goodUse() == 2) {
			clientSender.accept(SystemMessage.of(SystemMessage.FISH_RESISTED_REELING_S1_HP_REGAINED, new SystemMessage.Number(dmg)));
		} else {
			clientSender.accept(SystemMessage.id(SystemMessage.FISH_RESISTED_ATTEMPT_TO_BRING_IT_IN));
		}

		var regenPacket = new ExFishingHpRegen(
				player.objectId(), session.remainingSeconds(), session.curHp(),
				session.combatMode(), session.goodUse(), session.anim(), pen, session.deceptiveMode()
		);
		clientSender.accept(regenPacket);

		if (session.isFinished()) {
			if (session.isWon()) {
				winFishing(player, session, clientSender);
			} else {
				loseFishing(player, session, clientSender);
			}
		}
	}

	private void winFishing(PlayerCharacter player, FishingSession session, Consumer<GameServerPacket> clientSender) {
		cleanPlayerState(player);
		clientSender.accept(new ExFishingEnd(player.objectId(), true));
		clientSender.accept(SystemMessage.id(SystemMessage.YOU_CAUGHT_SOMETHING));

		// Adiciona o peixe pescado no inventario
		if (inventoryService != null && player.inventory() != null) {
			inventoryService.addItem(player.inventory(), session.fish().id(), 1, "Fishing");
		}

		// Registra no campeonato de pesca
		championship.registerCatch(player.name(), session.fish().level());
	}

	private void loseFishing(PlayerCharacter player, FishingSession session, Consumer<GameServerPacket> clientSender) {
		cleanPlayerState(player);
		clientSender.accept(new ExFishingEnd(player.objectId(), false));

		if (session.curHp() >= session.maxHp() * 2) {
			clientSender.accept(SystemMessage.id(SystemMessage.BAIT_STOLEN_BY_FISH));
		} else {
			clientSender.accept(SystemMessage.id(SystemMessage.FISH_SPIT_THE_HOOK));
		}
	}

	public void stopFishing(PlayerCharacter player, Consumer<GameServerPacket> clientSender) {
		if (player == null) {
			return;
		}
		cleanPlayerState(player);
		if (clientSender != null) {
			clientSender.accept(new ExFishingEnd(player.objectId(), false));
			clientSender.accept(SystemMessage.id(SystemMessage.FISHING_ATTEMPT_CANCELLED));
		}
	}

	private void cleanPlayerState(PlayerCharacter player) {
		cancelTask(player.objectId());
		sessions.remove(player.objectId());
		player.isFishing(false);
	}

	private void cancelTask(int playerId) {
		ScheduledFuture<?> task = tasks.remove(playerId);
		if (task != null) {
			task.cancel(false);
		}
	}
}
