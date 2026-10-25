package com.lopez.l2j.game.pet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.GroundItemService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetInventoryItem;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetInventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.PetItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico para gerenciamento de mascotes (Pets) e montarias:
 * ciclo de alimentacao (fome), experiencia, evolucao de nivel, inventario do pet e montaria.
 * Porta de L2PetInstance e PetDataTable de L2JDream / L2JLucera2.
 */
@Service
public class PetService {

	private static final Logger log = LoggerFactory.getLogger(PetService.class);

	public static final int WYVERN_BREATH_SKILL_ID = 4289;

	private static volatile PetService instance;

	private final PetDataTable petDataTable;
	private final JdbcClient jdbc;
	private final InventoryService inventoryService;

	private final Map<Integer, PetDataRecord> activePets = new ConcurrentHashMap<>();
	private final Map<Integer, List<ItemInstance>> petInventoriesByCollar = new ConcurrentHashMap<>();
	private final AtomicInteger fallbackItemObjIdGen = new AtomicInteger(0x49000000);

	@Autowired
	public PetService(PetDataTable petDataTable,
					  @Autowired(required = false) JdbcClient jdbc,
					  @Autowired(required = false) InventoryService inventoryService) {
		this.petDataTable = petDataTable != null ? petDataTable : PetDataTable.getInstance();
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
		instance = this;
	}

	public static PetService getInstance() {
		if (instance == null) {
			instance = new PetService(PetDataTable.getInstance(), null, null);
		}
		return instance;
	}

	public PetDataTable petDataTable() {
		return petDataTable != null ? petDataTable : PetDataTable.getInstance();
	}

	public Optional<PetDataRecord> getPet(int itemObjectId) {
		PetDataRecord mem = activePets.get(itemObjectId);
		if (mem != null) {
			return Optional.of(mem);
		}

		if (jdbc != null) {
			try {
				var rec = jdbc.sql("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed, weapon, armor, jewel FROM pets WHERE item_obj_id = ?")
						.param(itemObjectId)
						.query((rs, i) -> new PetDataRecord(
								rs.getInt("item_obj_id"),
								rs.getString("name"),
								rs.getInt("level"),
								rs.getInt("curHp"),
								rs.getInt("curMp"),
								rs.getLong("exp"),
								rs.getInt("sp"),
								rs.getInt("fed"),
								rs.getInt("weapon"),
								rs.getInt("armor"),
								rs.getInt("jewel")
						))
						.optional();

				if (rec.isPresent()) {
					activePets.put(itemObjectId, rec.get());
					return rec;
				}
			} catch (Exception e) {
				log.warn("Erro ao buscar pet item_obj_id {} no banco: {}", itemObjectId, e.getMessage());
			}
		}

		return Optional.empty();
	}

	public PetDataRecord createOrGetDefault(int itemObjectId, int npcId, String defaultName) {
		return getPet(itemObjectId).orElseGet(() -> {
			PetStatTemplate stat = petDataTable != null ? petDataTable.getStat(npcId, 1).orElse(null) : null;
			int hp = stat != null ? stat.hpMax() : 100;
			int mp = stat != null ? stat.mpMax() : 100;
			int fed = stat != null ? stat.feedMax() : 100;

			PetDataRecord created = new PetDataRecord(itemObjectId, defaultName != null ? defaultName : "Pet", 1, hp, mp, 0L, 0, fed, 0, 0, 0);
			savePet(created);
			return created;
		});
	}

	public void savePet(PetDataRecord pet) {
		if (pet == null) {
			return;
		}
		activePets.put(pet.itemObjectId(), pet);

		if (jdbc != null) {
			try {
				jdbc.sql("INSERT INTO pets (item_obj_id, name, level, curHp, curMp, exp, sp, fed, weapon, armor, jewel) " +
								"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
								"ON DUPLICATE KEY UPDATE name = VALUES(name), level = VALUES(level), curHp = VALUES(curHp), " +
								"curMp = VALUES(curMp), exp = VALUES(exp), sp = VALUES(sp), fed = VALUES(fed), " +
								"weapon = VALUES(weapon), armor = VALUES(armor), jewel = VALUES(jewel)")
						.params(pet.itemObjectId(), pet.name(), pet.level(), pet.curHp(), pet.curMp(),
								pet.exp(), pet.sp(), pet.fed(), pet.weapon(), pet.armor(), pet.jewel())
						.update();
			} catch (Exception e) {
				log.debug("Aviso ao persistir pet no banco (pode ser mock de teste): {}", e.getMessage());
			}
		}
	}

	public void deletePet(int itemObjectId) {
		activePets.remove(itemObjectId);
		petInventoriesByCollar.remove(itemObjectId);
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM pets WHERE item_obj_id = ?").param(itemObjectId).update();
			} catch (Exception e) {
				log.warn("Erro ao remover pet {}: {}", itemObjectId, e.getMessage());
			}
		}
	}

	/**
	 * Restaura os dados persistidos na tabela {@code pets} e aplica os atributos do {@link PetDataTable}
	 * na instancia viva {@link SummonInstance} (#231, #233).
	 */
	public PetDataRecord restoreAndApplyPetStats(SummonInstance summon, int itemObjectId, int npcId, String defaultName) {
		PetDataRecord record = createOrGetDefault(itemObjectId, npcId, defaultName);
		if (summon == null) {
			return record;
		}

		int level = Math.max(1, record.level());
		PetStatTemplate stat = petDataTable != null ? petDataTable.getStat(npcId, level).orElse(null) : null;
		if (stat != null) {
			summon.applyPetStatTemplate(stat);
		} else {
			summon.level(level);
		}

		if (record.name() != null && !record.name().isBlank()) {
			summon.name(record.name());
		}
		summon.exp(record.exp());
		summon.sp(record.sp());
		if (stat != null) {
			summon.maxFed(Math.max(1, stat.feedMax()));
		}
		summon.currentFed(record.fed() > 0 ? Math.min(summon.maxFed(), record.fed()) : summon.maxFed());
		summon.currentHp(record.curHp() > 0 ? Math.min(summon.maxHp(), record.curHp()) : summon.maxHp());
		summon.currentMp(record.curMp() > 0 ? Math.min(summon.maxMp(), record.curMp()) : summon.maxMp());
		summon.equippedWeaponId(record.weapon());
		summon.equippedArmorId(record.armor());
		summon.equippedJewelId(record.jewel());

		loadPetInventory(summon);
		return record;
	}

	/**
	 * Salva o estado atual da instancia viva {@link SummonInstance} na tabela {@code pets} e seu inventario.
	 */
	public void storePetStatus(SummonInstance summon) {
		if (summon == null || !summon.isPet() || summon.itemObjectId() <= 0) {
			return;
		}
		PetDataRecord updated = new PetDataRecord(
				summon.itemObjectId(),
				summon.name(),
				summon.level(),
				(int) Math.round(summon.currentHp()),
				(int) Math.round(summon.currentMp()),
				summon.exp(),
				summon.sp(),
				summon.currentFed(),
				summon.equippedWeaponId(),
				summon.equippedArmorId(),
				summon.equippedJewelId()
		);
		savePet(updated);
		savePetInventory(summon);
	}

	/**
	 * Alimenta o pet consumindo a racao correspondente (#234, #235).
	 */
	public boolean feed(PlayerCharacter player, int itemObjectId, int npcId, int foodItemId, Consumer<GameServerPacket> clientSender) {
		if (player == null) {
			return false;
		}

		if (petDataTable != null && !petDataTable.isPetFood(npcId, foodItemId)) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu mascote recusa essa comida."));
			}
			return false;
		}

		SummonInstance activePet = player.activeSummon();
		boolean consumedFromPetInv = false;
		if (activePet != null && activePet.isPet() && activePet.itemObjectId() == itemObjectId) {
			if (activePet.petInventory().getItemCount(foodItemId) > 0) {
				consumedFromPetInv = activePet.petInventory().destroyItemByItemId(foodItemId, 1);
			}
		}

		if (!consumedFromPetInv) {
			Inventory inv = player.inventory();
			if (inv == null || inv.getItemCount(foodItemId) <= 0) {
				if (clientSender != null) {
					clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui o alimento necessario."));
				}
				return false;
			}
			if (inventoryService != null) {
				inventoryService.consumeItem(inv, foodItemId, 1, "PetFeed");
			} else {
				inv.destroyItemByItemId(foodItemId, 1);
			}
		}

		PetDataRecord pet = createOrGetDefault(itemObjectId, npcId, "Pet");
		PetStatTemplate stat = petDataTable != null ? petDataTable.getStat(npcId, pet.level()).orElse(null) : null;
		int maxFed = stat != null ? stat.feedMax() : (activePet != null ? activePet.maxFed() : 100);
		int foodRate = Math.max(1, Math.round(Config.PET_FOOD_RATE));
		int fedGain = Math.max(10, (maxFed * 20 / 100) * foodRate); // Recupera 20% * PET_FOOD_RATE
		int baseFed = (activePet != null && activePet.itemObjectId() == itemObjectId) ? activePet.currentFed() : pet.fed();
		int newFed = Math.min(maxFed, baseFed + fedGain);

		PetDataRecord updated = pet.withFed(newFed);
		savePet(updated);

		if (activePet != null && activePet.itemObjectId() == itemObjectId) {
			activePet.maxFed(maxFed);
			activePet.currentFed(newFed);
			if (clientSender != null) {
				int foodSkillId = PetDataTable.getFoodSkillId(foodItemId);
				clientSender.accept(new GameServerPacket.MagicSkillUse(
						activePet.objectId(), activePet.objectId(), foodSkillId, 1, 0, 0,
						activePet.x(), activePet.y(), activePet.z()));
				clientSender.accept(new GameServerPacket.PetStatusUpdate(activePet));
			}
		}

		if (clientSender != null) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "Pet", pet.name() + " comeu com satisfacao! Fome: " + newFed + "/" + maxFed));
		}
		return true;
	}

	/**
	 * Concede experiencia ao mascote (aplicando PET_XP_RATE ou SIN_EATER_XP_RATE para npcId 12564) e processa level-up se necessario (#239).
	 */
	public boolean addExp(int itemObjectId, int npcId, long expAmount, Consumer<GameServerPacket> clientSender) {
		if (expAmount <= 0) {
			return false;
		}

		double xpMult = (npcId == PetDataTable.SIN_EATER_ID)
				? Math.max(0.1, Config.SIN_EATER_XP_RATE)
				: Math.max(0.1, Config.PET_XP_RATE);
		long scaledExp = xpMult == 1.0 ? expAmount : Math.max(1L, Math.round(expAmount * xpMult));

		PetDataRecord pet = createOrGetDefault(itemObjectId, npcId, "Pet");
		long totalExp = pet.exp() + scaledExp;
		int curLevel = pet.level();

		// Verifica se sobe de nivel
		while (curLevel < 85 && petDataTable != null) {
			Optional<PetStatTemplate> nextStat = petDataTable.getStat(npcId, curLevel + 1);
			if (nextStat.isPresent() && totalExp >= nextStat.get().expMax()) {
				curLevel++;
			} else {
				break;
			}
		}

		PetStatTemplate currentStat = petDataTable != null ? petDataTable.getStat(npcId, curLevel).orElse(null) : null;
		int newHp = currentStat != null ? currentStat.hpMax() : pet.curHp();
		int newMp = currentStat != null ? currentStat.mpMax() : pet.curMp();

		boolean leveledUp = curLevel > pet.level();
		PetDataRecord updated = pet.withExp(totalExp, curLevel, newHp, newMp);
		savePet(updated);

		if (leveledUp && clientSender != null) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "Pet", pet.name() + " alcancou o nivel " + curLevel + "!"));
		}
		return leveledUp;
	}

	/**
	 * Concede EXP e SP diretamente a um {@link SummonInstance} ativo, sincronizando nivel, atributos e coleira.
	 */
	public boolean addPetExpAndSp(SummonInstance summon, long expAmount, int spAmount, Consumer<GameServerPacket> clientSender) {
		if (summon == null || !summon.isPet()) {
			return false;
		}
		if (spAmount > 0) {
			summon.sp(summon.sp() + spAmount);
		}
		boolean leveledUp = addExp(summon.itemObjectId(), summon.npcId(), expAmount, clientSender);
		getPet(summon.itemObjectId()).ifPresent(rec -> {
			summon.exp(rec.exp());
			summon.level(rec.level());
			if (leveledUp) {
				if (petDataTable != null) {
					petDataTable.getStat(summon.npcId(), rec.level()).ifPresent(summon::applyPetStatTemplate);
				}
				summon.currentHp(summon.maxHp());
				summon.currentMp(summon.maxMp());
				if (summon.owner() != null && summon.owner().inventory() != null) {
					summon.owner().inventory().byObjectId(summon.itemObjectId())
							.ifPresent(collar -> collar.enchant(rec.level()));
				}
			}
			if (summon.isSinEater() && leveledUp) {
				summon.triggerSinEaterCurse();
			}
		});
		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.PetStatusUpdate(summon));
			if (leveledUp) {
				clientSender.accept(new GameServerPacket.PetInfo(summon, 1));
			}
		}
		return leveledUp;
	}

	// ---- PET INVENTORY MANAGEMENT (#236, #237, #238) ----

	public void loadPetInventory(SummonInstance pet) {
		if (pet == null) {
			return;
		}
		List<ItemInstance> cached = petInventoriesByCollar.get(pet.itemObjectId());
		if (cached != null) {
			for (ItemInstance it : cached) {
				pet.petInventory().addItem(it);
			}
		}
	}

	public void savePetInventory(SummonInstance pet) {
		if (pet == null || pet.itemObjectId() <= 0) {
			return;
		}
		petInventoriesByCollar.put(pet.itemObjectId(), new ArrayList<>(pet.petInventory().items()));
	}

	/**
	 * Transfere um item do inventario do jogador para o inventario do Pet (#071, #236).
	 */
	public boolean transferItemToPet(
			PlayerCharacter player,
			SummonInstance pet,
			int itemObjectId,
			int count,
			Consumer<GameServerPacket> clientSender) {
		if (player == null || pet == null || !pet.isPet() || pet.isDead() || count <= 0) {
			return false;
		}
		if (isCollarLocked(player, itemObjectId)) {
			return false;
		}
		Inventory playerInv = player.inventory();
		if (playerInv == null) {
			return false;
		}
		ItemInstance item = playerInv.byObjectId(itemObjectId).orElse(null);
		if (item == null || item.count() < count || item.isEquipped() || item.isAugmented()) {
			return false;
		}
		if (item.template() != null && (!item.template().dropable() || !item.template().tradeable() || item.template().isQuestItem())) {
			return false;
		}

		Inventory petInv = pet.petInventory();
		boolean stackable = item.template() != null && item.template().isStackable();
		Optional<ItemInstance> existingInPet = stackable ? petInv.byItemId(item.itemId()) : Optional.empty();
		if (existingInPet.isEmpty() && petInv.size() >= getMaximumSlotsForPet()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "O inventario do seu mascote esta cheio."));
			}
			return false;
		}

		// Remove do jogador
		boolean playerRemovedAll = item.count() == count;
		if (playerRemovedAll) {
			playerInv.remove(item);
		} else {
			item.count(item.count() - count);
		}

		// Adiciona ao Pet
		ItemInstance petItem;
		int petChange;
		if (existingInPet.isPresent()) {
			petItem = existingInPet.get();
			petItem.count(petItem.count() + count);
			petChange = 2; // MODIFIED
		} else {
			if (playerRemovedAll) {
				petItem = item;
				petItem.ownerId(pet.objectId());
				petItem.location(ItemInstance.Location.PET, 0);
			} else {
				int newObjId = fallbackItemObjIdGen.incrementAndGet();
				petItem = new ItemInstance(newObjId, item.template(), pet.objectId(), count);
				petItem.enchant(item.enchant());
				petItem.location(ItemInstance.Location.PET, 0);
			}
			petInv.addItem(petItem);
			petChange = 1; // ADDED
		}

		savePetInventory(pet);

		if (clientSender != null) {
			clientSender.accept(new InventoryUpdate(List.of(
					ItemInfo.of(item, playerRemovedAll ? ItemInfo.REMOVED : ItemInfo.MODIFIED))));
			clientSender.accept(new PetInventoryUpdate(List.of(toPetInventoryItem(petItem, petChange))));
			clientSender.accept(buildPetItemListPacket(pet));
			clientSender.accept(new GameServerPacket.PetStatusUpdate(pet));
		}
		return true;
	}

	/**
	 * Transfere um item do inventario do Pet de volta para o inventario do jogador (#071, #236).
	 */
	public boolean transferItemFromPet(
			PlayerCharacter player,
			SummonInstance pet,
			int itemObjectId,
			int count,
			Consumer<GameServerPacket> clientSender) {
		if (player == null || pet == null || !pet.isPet() || count <= 0) {
			return false;
		}
		Inventory petInv = pet.petInventory();
		Inventory playerInv = player.inventory();
		if (petInv == null || playerInv == null) {
			return false;
		}
		ItemInstance petItem = petInv.byObjectId(itemObjectId).orElse(null);
		if (petItem == null || petItem.count() < count) {
			return false;
		}

		if (petItem.isEquipped()) {
			unequipPetItem(pet, petItem);
		}

		boolean petRemovedAll = petItem.count() == count;
		if (petRemovedAll) {
			petInv.remove(petItem);
		} else {
			petItem.count(petItem.count() - count);
		}

		boolean stackable = petItem.template() != null && petItem.template().isStackable();
		Optional<ItemInstance> existingInPlayer = stackable ? playerInv.byItemId(petItem.itemId()) : Optional.empty();
		ItemInstance playerItem;
		int playerChange;
		if (existingInPlayer.isPresent()) {
			playerItem = existingInPlayer.get();
			playerItem.count(playerItem.count() + count);
			playerChange = ItemInfo.MODIFIED;
		} else {
			if (petRemovedAll) {
				playerItem = petItem;
				playerItem.ownerId(player.objectId());
				playerItem.location(ItemInstance.Location.INVENTORY, 0);
			} else {
				int newObjId = fallbackItemObjIdGen.incrementAndGet();
				playerItem = new ItemInstance(newObjId, petItem.template(), player.objectId(), count);
				playerItem.enchant(petItem.enchant());
				playerItem.location(ItemInstance.Location.INVENTORY, 0);
			}
			playerInv.addItem(playerItem);
			playerChange = ItemInfo.ADDED;
		}

		savePetInventory(pet);

		if (clientSender != null) {
			clientSender.accept(new PetInventoryUpdate(List.of(
					toPetInventoryItem(petItem, petRemovedAll ? 3 : 2))));
			clientSender.accept(buildPetItemListPacket(pet));
			clientSender.accept(new InventoryUpdate(List.of(ItemInfo.of(playerItem, playerChange))));
			clientSender.accept(new GameServerPacket.PetStatusUpdate(pet));
		}
		return true;
	}

	/**
	 * Usa um item de dentro do inventario do Pet: equipa/desequipa Arma ou Armadura de Pet, ou consome Racao (#071, #237).
	 */
	public boolean usePetItem(
			PlayerCharacter player,
			SummonInstance pet,
			int itemObjectId,
			Consumer<GameServerPacket> clientSender) {
		if (player == null || pet == null || !pet.isPet() || pet.isDead()) {
			return false;
		}
		ItemInstance item = pet.petInventory().byObjectId(itemObjectId).orElse(null);
		if (item == null) {
			return false;
		}

		int itemId = item.itemId();
		// 1. Verifica se e racao de Pet
		if ((petDataTable != null && petDataTable.isPetFood(pet.npcId(), itemId)) || PetDataTable.isAnyPetFood(itemId)) {
			return feed(player, pet.itemObjectId(), pet.npcId(), itemId, clientSender);
		}

		// 2. Equipamento de Arma / Armadura de Pet
		ItemTemplate tpl = item.template();
		boolean isWeapon = PetDataTable.isPetWeapon(itemId) || (tpl != null && tpl.isWeapon());
		boolean isArmor = PetDataTable.isPetArmor(itemId) || (tpl != null && tpl.isArmor());
		if (!isWeapon && !isArmor) {
			return false;
		}

		if (item.location() == ItemInstance.Location.PET_EQUIP) {
			unequipPetItem(pet, item);
		} else {
			// Desequipa outro item do mesmo tipo se houver
			for (ItemInstance other : pet.petInventory().items()) {
				if (other != item && other.location() == ItemInstance.Location.PET_EQUIP) {
					boolean otherWeapon = PetDataTable.isPetWeapon(other.itemId())
							|| (other.template() != null && other.template().isWeapon());
					if (otherWeapon == isWeapon) {
						unequipPetItem(pet, other);
					}
				}
			}
			item.location(ItemInstance.Location.PET_EQUIP, isWeapon ? 7 : 10);
			if (isWeapon) {
				pet.equippedWeaponId(itemId);
				int wPAtk = tpl != null && tpl.pAtk() > 0 ? tpl.pAtk() : 25;
				int wMAtk = tpl != null && tpl.mAtk() > 0 ? tpl.mAtk() : 15;
				int curArmorPDef = pet.equippedArmorId() > 0 ? 20 : 0;
				pet.setEquipmentBonuses(wPAtk, wMAtk, curArmorPDef, 0);
			} else {
				pet.equippedArmorId(itemId);
				int aPDef = tpl != null && tpl.pDef() > 0 ? tpl.pDef() : 25;
				int aMDef = tpl != null && tpl.mDef() > 0 ? tpl.mDef() : 15;
				int curWpnPAtk = pet.equippedWeaponId() > 0 ? 25 : 0;
				int curWpnMAtk = pet.equippedWeaponId() > 0 ? 15 : 0;
				pet.setEquipmentBonuses(curWpnPAtk, curWpnMAtk, aPDef, aMDef);
			}
		}

		storePetStatus(pet);

		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.PetInfo(pet, 1));
			clientSender.accept(buildPetItemListPacket(pet));
			clientSender.accept(new GameServerPacket.PetStatusUpdate(pet));
		}
		return true;
	}

	private void unequipPetItem(SummonInstance pet, ItemInstance item) {
		item.location(ItemInstance.Location.PET, 0);
		if (pet.equippedWeaponId() == item.itemId()) {
			pet.equippedWeaponId(0);
		}
		if (pet.equippedArmorId() == item.itemId()) {
			pet.equippedArmorId(0);
		}
		if (pet.equippedJewelId() == item.itemId()) {
			pet.equippedJewelId(0);
		}
		pet.recalculateStats();
	}

	/**
	 * Coleta um item do chao usando o Pet e adiciona ao inventario do Pet (#238).
	 */
	public boolean petPickUpItem(
			PlayerCharacter player,
			SummonInstance pet,
			GroundItemService.GroundItem groundItem,
			GroundItemService groundItemService,
			Consumer<GameServerPacket> clientSender) {
		if (pet == null || !pet.isPet() || pet.isDead() || groundItem == null) {
			return false;
		}
		if (pet.petInventory().size() >= getMaximumSlotsForPet()) {
			return false;
		}
		if (groundItemService != null) {
			groundItemService.pickupItem(player, groundItem.objectId());
		}
		pet.moveTo(groundItem.x(), groundItem.y(), groundItem.z());
		ItemInstance inst = groundItem.itemInstance();
		if (inst == null) {
			ItemTemplate dummy = ItemTemplate.etc(groundItem.itemId(), groundItem.itemId(),
					"Item_" + groundItem.itemId(), "etcitem", "none", 0, "none", 0, true, true, true, true);
			inst = new ItemInstance(groundItem.objectId(), dummy, pet.objectId(), groundItem.count());
		}
		return petPickUpItem(pet, inst, clientSender);
	}

	public boolean petPickUpItem(SummonInstance pet, ItemInstance item, Consumer<GameServerPacket> clientSender) {
		if (pet == null || !pet.isPet() || pet.isDead() || item == null) {
			return false;
		}
		Inventory petInv = pet.petInventory();
		boolean stackable = item.template() != null && item.template().isStackable();
		Optional<ItemInstance> existing = stackable ? petInv.byItemId(item.itemId()) : Optional.empty();
		if (existing.isEmpty() && petInv.size() >= getMaximumSlotsForPet()) {
			return false;
		}

		ItemInstance finalItem;
		int change;
		if (existing.isPresent()) {
			finalItem = existing.get();
			finalItem.count(finalItem.count() + item.count());
			change = 2;
		} else {
			item.ownerId(pet.objectId());
			item.location(ItemInstance.Location.PET, 0);
			petInv.addItem(item);
			finalItem = item;
			change = 1;
		}

		savePetInventory(pet);

		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.GetItem(pet.objectId(), item.objectId(), pet.x(), pet.y(), pet.z()));
			clientSender.accept(new PetInventoryUpdate(List.of(toPetInventoryItem(finalItem, change))));
			clientSender.accept(buildPetItemListPacket(pet));
		}
		return true;
	}

	public PetItemList buildPetItemListPacket(SummonInstance pet) {
		if (pet == null) {
			return new PetItemList(List.of());
		}
		List<PetInventoryItem> list = new ArrayList<>();
		for (ItemInstance it : pet.petInventory().items()) {
			list.add(toPetInventoryItem(it, 0));
		}
		return new PetItemList(list);
	}

	private static PetInventoryItem toPetInventoryItem(ItemInstance it, int change) {
		ItemTemplate tpl = it.template();
		int type1 = tpl != null ? tpl.type1() : 0;
		int type2 = tpl != null ? tpl.type2() : 0;
		int bodyPart = tpl != null ? tpl.bodyPart() : 0;
		boolean equipped = it.location() == ItemInstance.Location.PET_EQUIP;
		return new PetInventoryItem(
				change,
				type1,
				it.objectId(),
				it.itemId(),
				it.count(),
				type2,
				it.customType1(),
				equipped,
				bodyPart,
				it.enchant(),
				it.customType2());
	}

	// ---- PET RENAME & COLLAR LOCK (#241, #242) ----

	public boolean isCollarLocked(PlayerCharacter player, int collarObjectId) {
		if (player == null || collarObjectId <= 0) {
			return false;
		}
		SummonInstance active = player.activeSummon();
		return active != null && active.isPet() && active.itemObjectId() == collarObjectId;
	}

	public boolean isPetNameTaken(String name) {
		if (name == null || name.isBlank()) {
			return true;
		}
		for (PetDataRecord rec : activePets.values()) {
			if (rec.name() != null && rec.name().equalsIgnoreCase(name)) {
				return true;
			}
		}
		if (jdbc != null) {
			try {
				Integer count = jdbc.sql("SELECT COUNT(*) FROM pets WHERE LOWER(name) = LOWER(?)")
						.param(name)
						.query(Integer.class)
						.single();
				return count != null && count > 0;
			} catch (Exception ignored) {
			}
		}
		return false;
	}

	public boolean changePetName(PlayerCharacter player, SummonInstance pet, String newName, Consumer<GameServerPacket> clientSender) {
		if (player == null || pet == null || !pet.isPet() || newName == null) {
			return false;
		}
		String trimmed = newName.trim();
		if (trimmed.length() < 3 || trimmed.length() > 16 || !trimmed.matches("^[A-Za-z0-9]+$")) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nome de mascote invalido (use 3 a 16 caracteres alfanumericos)."));
			}
			return false;
		}
		if (isPetNameTaken(trimmed)) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Este nome de mascote ja esta em uso."));
			}
			return false;
		}
		pet.name(trimmed);
		storePetStatus(pet);
		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.PetInfo(pet, 1));
		}
		return true;
	}

	// ---- PET DEATH & RESURRECTION (#219, #240) ----

	public void onPetDeath(SummonInstance pet, Consumer<GameServerPacket> clientSender) {
		if (pet == null) {
			return;
		}
		pet.doDie();
		storePetStatus(pet);
		if (clientSender != null) {
			clientSender.accept(new GameServerPacket.PetStatusUpdate(pet));
		}
	}

	public boolean resurrectPet(PlayerCharacter player, SummonInstance pet, double expRestorePercent, Consumer<GameServerPacket> clientSender) {
		if (pet == null || !pet.isDead()) {
			return false;
		}
		boolean ok = pet.resurrect(expRestorePercent);
		if (ok) {
			storePetStatus(pet);
			if (clientSender != null) {
				clientSender.accept(new GameServerPacket.PetInfo(pet, 1));
				clientSender.accept(new GameServerPacket.PetStatusUpdate(pet));
			}
		}
		return ok;
	}

	public int getMaximumSlotsForPet() {
		return Math.max(1, Config.MAXIMUM_SLOTS_FOR_PET);
	}

	public boolean canRentPetAtNpc(int npcId) {
		return Config.LIST_PET_RENT_NPC != null
				&& Config.LIST_PET_RENT_NPC.contains(npcId);
	}

	// ---- MOUNT / DISMOUNT & SPEED HELPERS (#240..#242, #249) ----

	public static int getMountSpeedForNpc(int mountNpcId) {
		if (mountNpcId <= 0) {
			return 0;
		}
		if (mountNpcId == PetDataTable.WYVERN_ID) {
			return 200;
		}
		if (PetDataTable.isStrider(mountNpcId)) {
			return 180;
		}
		if (PetDataTable.isGreatWolf(mountNpcId)) {
			return 170;
		}
		return 160;
	}

	public int getMountSpeed(PlayerCharacter player) {
		if (player == null || !player.isMounted()) {
			return 0;
		}
		return getMountSpeedForNpc(player.mountNpcId());
	}

	/**
	 * Monta o jogador no pet especificado (se montavel), enviando o pacote {@code Ride} e aplicando skills (#240, #249).
	 */
	public boolean mount(PlayerCharacter player, int petNpcId, Consumer<GameServerPacket> clientSender) {
		if (player == null || player.isDead() || player.sitting() || player.isOlympiadMode() || player.isInCombat()) {
			return false;
		}
		if (player.isMounted()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja esta montado."));
			}
			return false;
		}
		if (petDataTable != null && !petDataTable.isMountable(petNpcId)) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Esta criatura nao serve como montaria."));
			}
			return false;
		}

		// Se o pet estiver invocado no mundo, recolhe antes de montar
		SummonInstance activeSummon = player.activeSummon();
		if (activeSummon != null) {
			activeSummon.unsummon();
		}

		int rideType = (petNpcId == PetDataTable.WYVERN_ID) ? 2 : 1;
		player.mountNpcId(petNpcId);
		player.mountType(rideType);
		player.flying(petNpcId == PetDataTable.WYVERN_ID);
		if (petNpcId == PetDataTable.WYVERN_ID) {
			player.skills().put(WYVERN_BREATH_SKILL_ID, 1);
		}

		GameServerPacket.Ride ridePkt = new GameServerPacket.Ride(player.objectId(), 1, rideType, petNpcId);
		if (clientSender != null) {
			clientSender.accept(ridePkt);
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce montou com sucesso!"));
		}
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.broadcastAround(player.x(), player.y(), GameWorld.VISIBILITY_RADIUS, player.instanceId(), ridePkt);
		}
		return true;
	}

	/**
	 * Desmonta o jogador, removendo Wyvern Breath e transmitindo {@code Ride(0)}.
	 */
	public boolean dismount(PlayerCharacter player, Consumer<GameServerPacket> clientSender) {
		if (player == null || !player.isMounted()) {
			return false;
		}
		int prevNpcId = player.mountNpcId();
		player.mountNpcId(0);
		player.mountType(0);
		player.flying(false);
		if (prevNpcId == PetDataTable.WYVERN_ID) {
			player.skills().remove(WYVERN_BREATH_SKILL_ID);
		}

		GameServerPacket.Ride ridePkt = new GameServerPacket.Ride(player.objectId(), 0, 0, 0);
		if (clientSender != null) {
			clientSender.accept(ridePkt);
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce desmontou."));
		}
		GameWorld world = GameWorld.getInstance();
		if (world != null) {
			world.broadcastAround(player.x(), player.y(), GameWorld.VISIBILITY_RADIUS, player.instanceId(), ridePkt);
		}
		return true;
	}

	/**
	 * Verifica condicoes de auto-desmontaria (Olympiad, zona No-Landing de Wyvern ou fome zerada).
	 */
	public boolean checkAutoDismount(
			PlayerCharacter player,
			boolean inNoLandingZone,
			int currentMountFed,
			Consumer<GameServerPacket> clientSender) {
		if (player == null || !player.isMounted()) {
			return false;
		}
		if (player.isOlympiadMode() || inNoLandingZone || currentMountFed <= 0) {
			return dismount(player, clientSender);
		}
		return false;
	}

	/**
	 * Evolui um mascote que atingiu nivel minimo (ex: Wolf Lv 55 -> Great Wolf).
	 */
	public Optional<PetDataRecord> evolve(PlayerCharacter player, int collarObjectId, int oldNpcId, int newNpcId, String newName) {
		PetDataRecord pet = getPet(collarObjectId).orElse(null);
		if (pet == null || pet.level() < 55) {
			return Optional.empty();
		}

		PetStatTemplate newStat = petDataTable != null ? petDataTable.getStat(newNpcId, pet.level()).orElse(null) : null;
		int hp = newStat != null ? newStat.hpMax() : pet.curHp();
		int mp = newStat != null ? newStat.mpMax() : pet.curMp();
		int fed = newStat != null ? newStat.feedMax() : pet.fed();

		PetDataRecord evolved = new PetDataRecord(collarObjectId, newName, pet.level(), hp, mp, pet.exp(), pet.sp(), fed,
				pet.weapon(), pet.armor(), pet.jewel());
		savePet(evolved);
		log.info("Pet {} evoluiu com sucesso de NPC {} para NPC {}", pet.name(), oldNpcId, newNpcId);
		return Optional.of(evolved);
	}

	public record PetSkillLearn(int templateId, int minLvl, int skillId, int skillLvl) {}

	/**
	 * Retorna as habilidades disponíveis para a mascote a partir da tabela pets_skills (V105 / V1105).
	 */
	public List<PetSkillLearn> getPetSkills(int templateId, int petLevel) {
		if (jdbc == null) {
			return List.of();
		}
		try {
			return jdbc.sql("SELECT templateId, minLvl, skillId, skillLvl FROM pets_skills WHERE templateId = ? AND minLvl <= ?")
					.param(templateId)
					.param(petLevel)
					.query((rs, i) -> new PetSkillLearn(
							rs.getInt("templateId"),
							rs.getInt("minLvl"),
							rs.getInt("skillId"),
							rs.getInt("skillLvl")
					)).list();
		} catch (Exception e) {
			log.debug("pets_skills indisponivel: {}", e.getMessage());
			return List.of();
		}
	}
}
