package com.lopez.l2j.game.summon;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.pet.PetDataRecord;
import com.lopez.l2j.game.pet.PetDataTable;
import com.lopez.l2j.game.pet.PetService;
import com.lopez.l2j.game.pet.PetStatTemplate;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de itens invocadores (SummonItemsData / SummonItems do legado L2JDream / L2JLucera2).
 * Permite invocar Mascotes (Pets como Wolf, Strider, Hatchling, Baby Pet, Sin Eater),
 * Montarias (Wyvern, Strider) e Objetos Estaticos (Christmas Tree 5560 / Special Christmas Tree 5561).
 */
@Service
public class SummonItemService {

	private static final Logger log = LoggerFactory.getLogger(SummonItemService.class);

	public static final int CHRISTMAS_TREE_ITEM_ID = 5560;
	public static final int SPECIAL_CHRISTMAS_TREE_ITEM_ID = 5561;
	public static final int CHRISTMAS_TREE_NPC_ID = 13006;
	public static final int SPECIAL_CHRISTMAS_TREE_NPC_ID = 13007;
	public static final int SUMMON_PET_SKILL_ID = 2046;

	private static final ScheduledExecutorService TREE_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "static-summon-scheduler");
		t.setDaemon(true);
		return t;
	});

	public record PetInfo(
			int petObjectId,
			int ownerId,
			int npcId,
			int itemObjectId,
			String name,
			int level,
			int curHp,
			int maxHp,
			int curMp,
			int maxMp
	) {
	}

	private final SummonItemsTable summonItemsTable;
	private final NpcTemplateTable npcTemplates;
	private final SpawnService spawnService;
	private final InventoryService inventoryService;
	private final ObjectIdFactory idFactory;
	private final PetService petService;
	private final PetDataTable petDataTable;

	private final Map<Integer, PetInfo> activePets = new ConcurrentHashMap<>();

	public SummonItemService(SummonItemsTable summonItemsTable,
							 NpcTemplateTable npcTemplates,
							 SpawnService spawnService,
							 InventoryService inventoryService,
							 ObjectIdFactory idFactory) {
		this(summonItemsTable, npcTemplates, spawnService, inventoryService, idFactory, null, null);
	}

	@Autowired
	public SummonItemService(SummonItemsTable summonItemsTable,
							 @Autowired(required = false) NpcTemplateTable npcTemplates,
							 @Autowired(required = false) SpawnService spawnService,
							 @Autowired(required = false) InventoryService inventoryService,
							 @Autowired(required = false) ObjectIdFactory idFactory,
							 @Autowired(required = false) PetService petService,
							 @Autowired(required = false) PetDataTable petDataTable) {
		this.summonItemsTable = summonItemsTable != null ? summonItemsTable : new SummonItemsTable();
		this.npcTemplates = npcTemplates;
		this.spawnService = spawnService;
		this.inventoryService = inventoryService;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x30000000);
		this.petService = petService;
		this.petDataTable = petDataTable;
	}

	private PetService resolvePetService() {
		return petService != null ? petService : PetService.getInstance();
	}

	private PetDataTable resolvePetDataTable() {
		return petDataTable != null ? petDataTable : PetDataTable.getInstance();
	}

	public boolean isSummonItem(int itemId) {
		return summonItemsTable.isSummonItem(itemId);
	}

	public Optional<SummonItem> getSummonItem(int itemId) {
		return summonItemsTable.get(itemId);
	}

	public Optional<PetInfo> getPet(int playerId) {
		return Optional.ofNullable(activePets.get(playerId));
	}

	/**
	 * Valida condicoes para invocacao do item.
	 */
	public boolean canSummon(PlayerCharacter player, ItemInstance item, Consumer<GameServerPacket> clientSender) {
		if (player == null || item == null || player.isDead()) {
			return false;
		}

		if (player.sitting()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode invocar sentado."));
			}
			return false;
		}

		if (player.isInCombat() || player.isOlympiadMode()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode invocar em combate ou nas Olimpiadas."));
			}
			return false;
		}

		SummonItem sitem = summonItemsTable.get(item.itemId()).orElse(null);
		if (sitem == null) {
			return false;
		}

		if (sitem.isPet() && (player.hasPet() || player.hasSummon() || player.isMounted())) {
			if (clientSender != null) {
				clientSender.accept(SystemMessage.id(SystemMessage.YOU_ALREADY_HAVE_A_PET));
			}
			return false;
		}

		if (sitem.isMount() && player.isMounted()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja esta montado."));
			}
			return false;
		}

		return true;
	}

	/**
	 * Executa o uso do item invocador (#212, #231..#235).
	 */
	public boolean useSummonItem(PlayerCharacter player, ItemInstance item, Consumer<GameServerPacket> clientSender) {
		if (!canSummon(player, item, clientSender)) {
			return false;
		}

		SummonItem sitem = summonItemsTable.get(item.itemId()).orElse(null);
		if (sitem == null) {
			return false;
		}

		switch (sitem.summonType()) {
			case 0 -> {
				// Estatico: Objeto / Arvore de Natal (5560 -> 13006, 5561 -> 13007) (#212)
				if (inventoryService != null && player.inventory() != null) {
					inventoryService.consumeItem(player.inventory(), item.itemId(), 1, "SummonItem");
				} else if (player.inventory() != null) {
					player.inventory().destroyItemByItemId(item.itemId(), 1);
				}

				spawnStaticSummonObject(player, sitem.npcId());
				if (clientSender != null) {
					clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Objeto invocado com sucesso!"));
				}
				return true;
			}
			case 1 -> {
				// Pet: Wolf, Strider, Hatchling, Baby Pet, Sin Eater (#231, #232, #233)
				if (clientSender != null) {
					clientSender.accept(new GameServerPacket.MagicSkillUse(
							player.objectId(), player.objectId(), SUMMON_PET_SKILL_ID, 1, 5000, 0,
							player.x(), player.y(), player.z()));
					clientSender.accept(new SetupGauge(SetupGauge.BLUE, 5000));
					clientSender.accept(SystemMessage.id(SystemMessage.SUMMON_A_PET));
				}

				int petId = idFactory.nextId();
				String defaultName = player.name() + "'s Pet";
				NpcTemplate npcTmpl = npcTemplates != null ? npcTemplates.get(sitem.npcId()).orElse(null) : null;
				if (npcTmpl != null && npcTmpl.name() != null && !npcTmpl.name().isBlank()) {
					defaultName = npcTmpl.name();
				} else {
					npcTmpl = NpcTemplate.fallback(sitem.npcId(), defaultName, "L2Pet");
				}

				PetService pService = resolvePetService();
				PetDataTable pTable = resolvePetDataTable();

				// Busca ou cria registro persistido na tabela pets (#231)
				PetDataRecord persisted = pService != null ? pService.getPet(item.objectId()).orElse(null) : null;
				int petLevel;
				if (persisted != null) {
					petLevel = Math.max(1, Math.max(persisted.level(), item.enchant()));
				} else {
					petLevel = item.enchant() > 0 ? item.enchant() : Math.max(1, npcTmpl.level());
				}

				PetStatTemplate stat = pTable != null ? pTable.getStat(sitem.npcId(), petLevel).orElse(null) : null;
				int maxHp = stat != null ? stat.hpMax() : Math.max(100 + petLevel * 20, npcTmpl.maxHp());
				int maxMp = stat != null ? stat.mpMax() : Math.max(50 + petLevel * 10, npcTmpl.maxMp());
				int maxFed = stat != null ? stat.feedMax() : 1200;

				if (persisted == null && pService != null) {
					persisted = new PetDataRecord(item.objectId(), defaultName, petLevel, maxHp, maxMp, 0L, 0, maxFed, 0, 0, 0);
					pService.savePet(persisted);
				} else if (persisted != null && persisted.level() != petLevel && pService != null) {
					persisted = persisted.withExp(persisted.exp(), petLevel, maxHp, maxMp);
					pService.savePet(persisted);
				}

				item.enchant(petLevel);

				SummonInstance summon = new SummonInstance(
						petId, player, npcTmpl, SummonInstance.TYPE_PET, item.objectId(), 0, petLevel);
				if (pService != null) {
					pService.restoreAndApplyPetStats(summon, item.objectId(), sitem.npcId(), defaultName);
				} else if (stat != null) {
					summon.applyPetStatTemplate(stat);
				}

				summon.x(player.x() + 40);
				summon.y(player.y() + 40);
				summon.z(player.z());
				summon.heading(player.heading());
				summon.instanceId(player.instanceId());

				// Inicia tasks de fome (#234) e follow (#248)
				summon.startFeedTask(pService);
				summon.startFollowTask();

				player.activeSummon(summon);
				player.petObjectId(petId);

				PetInfo pet = new PetInfo(
						petId,
						player.objectId(),
						sitem.npcId(),
						item.objectId(),
						summon.name(),
						summon.level(),
						(int) Math.round(summon.currentHp()),
						summon.maxHp(),
						(int) Math.round(summon.currentMp()),
						summon.maxMp()
				);
				activePets.put(player.objectId(), pet);

				GameWorld world = GameWorld.getInstance();
				if (world != null) {
					world.addSummon(summon);
					var npcInfo = GameServerPacket.NpcInfo.forSummon(summon);
					for (var near : world.findPlayersAround(
							summon.x(), summon.y(), GameWorld.VISIBILITY_RADIUS, player.instanceId())) {
						near.send(npcInfo);
					}
				}

				if (clientSender != null) {
					clientSender.accept(new GameServerPacket.PetStatusShow(SummonInstance.TYPE_PET));
					clientSender.accept(new GameServerPacket.PetInfo(summon, 2));
					if (pService != null) {
						clientSender.accept(pService.buildPetItemListPacket(summon));
					}
					clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pet " + summon.name() + " invocado!"));
				}

				if (summon.isSinEater()) {
					summon.triggerSinEaterCurse();
				}
				return true;
			}
			case 2 -> {
				// Montaria (Wyvern / Strider) (#240..#242, #249)
				PetService pService = resolvePetService();
				if (pService != null) {
					boolean mounted = pService.mount(player, sitem.npcId(), clientSender);
					if (mounted) {
						return true;
					}
				}
				player.mountNpcId(sitem.npcId());
				if (clientSender != null) {
					clientSender.accept(new GameServerPacket.Ride(
							player.objectId(), 1, sitem.npcId() == PetDataTable.WYVERN_ID ? 2 : 1, sitem.npcId()));
					clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montaria ativada!"));
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * Spawna o NPC de objeto estatico (ex.: Christmas Tree 13006 ou Special Christmas Tree 13007) no mundo
	 * e agenda despawn e aura de regeneracao (#212).
	 */
	private NpcInstance spawnStaticSummonObject(PlayerCharacter player, int npcId) {
		String treeName = npcId == SPECIAL_CHRISTMAS_TREE_NPC_ID ? "Special Christmas Tree" : "Christmas Tree";
		NpcTemplate tpl = npcTemplates != null
				? npcTemplates.get(npcId).orElse(NpcTemplate.fallback(npcId, treeName, "L2SpecialTree"))
				: NpcTemplate.fallback(npcId, treeName, "L2SpecialTree");
		NpcInstance treeNpc = new NpcInstance(
				idFactory.nextId(),
				tpl,
				player.x() + 30,
				player.y() + 30,
				player.z(),
				player.heading());
		treeNpc.instanceId(player.instanceId());

		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.addNpc(treeNpc);
			world.broadcastAround(treeNpc.x(), treeNpc.y(), GameWorld.VISIBILITY_RADIUS, treeNpc.instanceId(),
					GameServerPacket.NpcInfo.of(treeNpc));
		}

		// Se for Special Christmas Tree (13007), aplica aura de regeneracao de HP/MP a cada 5s
		final ScheduledFuture<?> auraTask;
		if (npcId == SPECIAL_CHRISTMAS_TREE_NPC_ID && world != null) {
			auraTask = TREE_SCHEDULER.scheduleAtFixedRate(() -> {
				for (var near : world.findPlayersAround(treeNpc.x(), treeNpc.y(), 200, treeNpc.instanceId())) {
					PlayerCharacter ch = near.character();
					if (ch != null && !ch.isDead()) {
						ch.currentHp(Math.min(ch.maxHp(), ch.currentHp() + 35.0));
						ch.currentMp(Math.min(ch.maxMp(), ch.currentMp() + 18.0));
					}
				}
			}, 5_000L, 5_000L, TimeUnit.MILLISECONDS);
		} else {
			auraTask = null;
		}

		TREE_SCHEDULER.schedule(() -> {
			if (auraTask != null) {
				auraTask.cancel(false);
			}
			if (world != null) {
				world.removeNpc(treeNpc);
				world.broadcastAround(treeNpc.x(), treeNpc.y(), GameWorld.VISIBILITY_RADIUS, treeNpc.instanceId(),
						new GameServerPacket.DeleteObject(treeNpc.objectId()));
			}
		}, 1_200_000L, TimeUnit.MILLISECONDS);

		return treeNpc;
	}

	/**
	 * Remove ou recolhe o pet ativo, persistindo seu estado no {@link PetService}.
	 */
	public boolean unsummonPet(PlayerCharacter player, Consumer<GameServerPacket> clientSender) {
		if (player == null || (!player.hasPet() && !player.hasSummon())) {
			return false;
		}

		PetInfo pet = activePets.remove(player.objectId());
		SummonInstance activeSummon = player.activeSummon();
		int removedObjId = pet != null ? pet.petObjectId() : (activeSummon != null ? activeSummon.objectId() : player.petObjectId());
		int summonType = activeSummon != null ? activeSummon.summonType() : SummonInstance.TYPE_PET;

		if (activeSummon != null) {
			activeSummon.stopAllTasks();
			PetService pService = resolvePetService();
			if (pService != null && activeSummon.isPet()) {
				pService.storePetStatus(activeSummon);
			}
		}

		player.petObjectId(0);
		player.activeSummon(null);
		GameWorld world = GameWorld.getInstance();
		if (removedObjId > 0 && world != null) {
			world.removeSummon(removedObjId);
			var del = new GameServerPacket.DeleteObject(removedObjId);
			for (var near : world.findPlayersAround(
					player.x(), player.y(), GameWorld.VISIBILITY_RADIUS, player.instanceId())) {
				near.send(del);
			}
		}
		if (clientSender != null) {
			if (removedObjId > 0) {
				clientSender.accept(new GameServerPacket.PetDelete(summonType, removedObjId));
			}
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pet recolhido."));
		}
		return pet != null || activeSummon != null;
	}

	/**
	 * Desmonta da montaria ativa.
	 */
	public boolean dismount(PlayerCharacter player, Consumer<GameServerPacket> clientSender) {
		if (player == null || !player.isMounted()) {
			return false;
		}

		PetService pService = resolvePetService();
		if (pService != null) {
			return pService.dismount(player, clientSender);
		}

		player.mountNpcId(0);
		player.mountType(0);
		player.flying(false);
		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.Ride(player.objectId(), 0, 0, 0));
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desmontou."));
		}
		return true;
	}
}
