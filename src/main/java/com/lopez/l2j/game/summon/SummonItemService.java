package com.lopez.l2j.game.summon;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.npc.SpawnService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.SetupGauge;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de itens invocadores (SummonItemsData / SummonItems do legado L2JDream).
 * Permite invocar Mascotes (Pets como Wolf, Strider, Hatchling, Sin Eater),
 * Montarias (Wyvern) e Objetos Estaticos (Christmas Tree).
 */
@Service
public class SummonItemService {

	private static final Logger log = LoggerFactory.getLogger(SummonItemService.class);

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

	private final Map<Integer, PetInfo> activePets = new ConcurrentHashMap<>();

	@Autowired
	public SummonItemService(SummonItemsTable summonItemsTable,
							 @Autowired(required = false) NpcTemplateTable npcTemplates,
							 @Autowired(required = false) SpawnService spawnService,
							 @Autowired(required = false) InventoryService inventoryService,
							 @Autowired(required = false) ObjectIdFactory idFactory) {
		this.summonItemsTable = summonItemsTable;
		this.npcTemplates = npcTemplates;
		this.spawnService = spawnService;
		this.inventoryService = inventoryService;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x30000000);
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
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode invocar sentado."));
			return false;
		}

		SummonItem sitem = summonItemsTable.get(item.itemId()).orElse(null);
		if (sitem == null) {
			return false;
		}

		if (sitem.isPet() && (player.hasPet() || player.isMounted())) {
			clientSender.accept(SystemMessage.id(SystemMessage.YOU_ALREADY_HAVE_A_PET));
			return false;
		}

		if (sitem.isMount() && player.isMounted()) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja esta montado."));
			return false;
		}

		return true;
	}

	/**
	 * Executa o uso do item invocador.
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
				// Estatico: Objeto/Arvore de natal, etc.
				if (inventoryService != null && player.inventory() != null) {
					inventoryService.consumeItem(player.inventory(), item.itemId(), 1, "SummonItem");
				} else if (player.inventory() != null) {
					player.inventory().destroyItemByItemId(item.itemId(), 1);
				}
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Objeto invocado com sucesso!"));
				return true;
			}
			case 1 -> {
				// Pet: Wolf, Strider, Hatchling, etc.
				clientSender.accept(new SetupGauge(SetupGauge.BLUE, 5000));
				clientSender.accept(SystemMessage.id(SystemMessage.SUMMON_A_PET));

				int petId = idFactory.nextId();
				int petLevel = item.enchant() > 0 ? item.enchant() : 1;
				String petName = player.name() + "'s Pet";
				int maxHp = 100 + petLevel * 20;
				int maxMp = 50 + petLevel * 10;
				var npcTmpl = npcTemplates != null ? npcTemplates.get(sitem.npcId()).orElse(null) : null;
				if (npcTmpl != null) {
					petName = npcTmpl.name();
					maxHp = Math.max(maxHp, (int) npcTmpl.maxHp());
					maxMp = Math.max(maxMp, (int) npcTmpl.maxMp());
				} else {
					npcTmpl = com.lopez.l2j.game.npc.NpcTemplate.fallback(sitem.npcId(), petName, "L2Pet");
				}

				PetInfo pet = new PetInfo(
						petId,
						player.objectId(),
						sitem.npcId(),
						item.objectId(),
						petName,
						petLevel,
						maxHp,
						maxHp,
						maxMp,
						maxMp
				);

				SummonInstance summon = new SummonInstance(
						petId, player, npcTmpl, SummonInstance.TYPE_PET, item.objectId(), 0, petLevel);
				summon.x(player.x() + 40);
				summon.y(player.y() + 40);
				summon.z(player.z());
				summon.heading(player.heading());
				player.activeSummon(summon);
				com.lopez.l2j.game.world.GameWorld.getInstance().addSummon(summon);

				activePets.put(player.objectId(), pet);
				player.petObjectId(petId);
				clientSender.accept(new GameServerPacket.PetStatusShow(SummonInstance.TYPE_PET));
				clientSender.accept(new GameServerPacket.PetInfo(summon, 2));
				var npcInfo = GameServerPacket.NpcInfo.forSummon(summon);
				for (var near : com.lopez.l2j.game.world.GameWorld.getInstance().findPlayersAround(
						summon.x(), summon.y(), com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS, player.instanceId())) {
					near.send(npcInfo);
				}
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pet " + pet.name() + " invocado!"));
				return true;
			}
			case 2 -> {
				// Montaria (Wyvern / Strider)
				player.mountNpcId(sitem.npcId());
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Montaria ativada!"));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * Remove ou recolhe o pet ativo.
	 */
	public boolean unsummonPet(PlayerCharacter player, Consumer<GameServerPacket> clientSender) {
		if (player == null || (!player.hasPet() && !player.hasSummon())) {
			return false;
		}

		PetInfo pet = activePets.remove(player.objectId());
		SummonInstance activeSummon = player.activeSummon();
		int removedObjId = pet != null ? pet.petObjectId() : (activeSummon != null ? activeSummon.objectId() : player.petObjectId());
		int summonType = activeSummon != null ? activeSummon.summonType() : SummonInstance.TYPE_PET;

		player.petObjectId(0);
		player.activeSummon(null);
		if (removedObjId > 0) {
			com.lopez.l2j.game.world.GameWorld.getInstance().removeSummon(removedObjId);
			var del = new GameServerPacket.DeleteObject(removedObjId);
			for (var near : com.lopez.l2j.game.world.GameWorld.getInstance().findPlayersAround(
					player.x(), player.y(), com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS, player.instanceId())) {
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

		player.mountNpcId(0);
		if (clientSender != null) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desmontou."));
		}
		return true;
	}
}
