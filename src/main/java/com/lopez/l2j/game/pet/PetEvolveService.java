package com.lopez.l2j.game.pet;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.summon.SummonInstance;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico de Evolucao e Restauracao Oficial de Pets nos Pet Managers (L2JDream / L2JLucera2).
 * - Wolf (lvl 55+) -> Great Wolf (collar 2375 -> necklace 9882)
 * - Great Wolf (lvl 70+) -> Fenrir (9882 / 10163 -> 10426; Snow 10307 -> 10611)
 * - Hatchling (lvl 55+) -> Strider (flute 3500/3501/3502 -> 4422/4423/4424)
 * - Baby Pets (lvl 55+) -> Improved Baby Pets (6648/6649/6650 -> 10311/10312/10313)
 * - Restore / Conversao Especial: Great Wolf (10163/9882) <-> Snow Great Wolf (10307),
 *   Fenrir (10426) <-> Snow Fenrir (10611), Strider (4422..4424) <-> Red Strider (10308..10310).
 */
@Service
public class PetEvolveService {

	private static final Logger log = LoggerFactory.getLogger(PetEvolveService.class);

	public record EvolveRule(int baseItemId, int evolvedItemId, int minLevel, String petName, int targetNpcId) {
		public EvolveRule(int baseItemId, int evolvedItemId, int minLevel, String petName) {
			this(baseItemId, evolvedItemId, minLevel, petName, 0);
		}
	}

	public record EvolveResult(boolean success, int evolvedItemId, String message) {
		public static EvolveResult ok(int id, String msg) {
			return new EvolveResult(true, id, msg);
		}
		public static EvolveResult fail(String msg) {
			return new EvolveResult(false, 0, msg);
		}
	}

	private final Map<Integer, EvolveRule> rules = new ConcurrentHashMap<>();
	private final Map<Integer, EvolveRule> restoreRules = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(920000);

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	@Autowired(required = false)
	private InventoryService inventoryService;

	@Autowired(required = false)
	private PetService petService;

	public PetEvolveService() {
		initRules();
		initRestoreRules();
	}

	private void initRules() {
		// Wolf -> Great Wolf
		rules.put(2375, new EvolveRule(2375, 9882, 55, "Great Wolf", PetDataTable.GREAT_WOLF_ID));
		// Great Wolf -> Fenrir
		rules.put(9882, new EvolveRule(9882, 10426, 70, "Fenrir", PetDataTable.FENRIR_ID));
		rules.put(10163, new EvolveRule(10163, 10426, 70, "Fenrir", PetDataTable.FENRIR_ID));
		rules.put(10307, new EvolveRule(10307, 10611, 70, "Snow Fenrir", PetDataTable.SNOW_FENRIR_ID));
		// Hatchlings -> Striders (Wind, Star, Twilight)
		rules.put(3500, new EvolveRule(3500, 4422, 55, "Wind Strider", PetDataTable.STRIDER_WIND_ID));
		rules.put(3501, new EvolveRule(3501, 4423, 55, "Star Strider", PetDataTable.STRIDER_STAR_ID));
		rules.put(3502, new EvolveRule(3502, 4424, 55, "Twilight Strider", PetDataTable.STRIDER_TWILIGHT_ID));
		// Baby Pets -> Improved Baby Pets
		rules.put(6648, new EvolveRule(6648, 10311, 55, "Improved Baby Buffalo", PetDataTable.IMPROVED_BABY_BUFFALO_ID));
		rules.put(6649, new EvolveRule(6649, 10312, 55, "Improved Baby Cougar", PetDataTable.IMPROVED_BABY_COUGAR_ID));
		rules.put(6650, new EvolveRule(6650, 10313, 55, "Improved Baby Kookaburra", PetDataTable.IMPROVED_BABY_KOOKABURRA_ID));
	}

	private void initRestoreRules() {
		// Great Wolf <-> Snow Great Wolf (#244)
		restoreRules.put(10163, new EvolveRule(10163, 10307, 55, "Snow Great Wolf", PetDataTable.SNOW_GREAT_WOLF_ID));
		restoreRules.put(9882, new EvolveRule(9882, 10307, 55, "Snow Great Wolf", PetDataTable.SNOW_GREAT_WOLF_ID));
		restoreRules.put(10307, new EvolveRule(10307, 10163, 55, "Great Wolf", PetDataTable.GREAT_WOLF_ID));

		// Fenrir <-> Snow Fenrir (#244)
		restoreRules.put(10426, new EvolveRule(10426, 10611, 70, "Snow Fenrir", PetDataTable.SNOW_FENRIR_ID));
		restoreRules.put(10611, new EvolveRule(10611, 10426, 70, "Fenrir", PetDataTable.FENRIR_ID));

		// Strider <-> Red Strider (#244)
		restoreRules.put(4422, new EvolveRule(4422, 10308, 55, "Red Wind Strider", PetDataTable.RED_STRIDER_WIND_ID));
		restoreRules.put(10308, new EvolveRule(10308, 4422, 55, "Wind Strider", PetDataTable.STRIDER_WIND_ID));
		restoreRules.put(4423, new EvolveRule(4423, 10309, 55, "Red Star Strider", PetDataTable.RED_STRIDER_STAR_ID));
		restoreRules.put(10309, new EvolveRule(10309, 4423, 55, "Star Strider", PetDataTable.STRIDER_STAR_ID));
		restoreRules.put(4424, new EvolveRule(4424, 10310, 55, "Red Twilight Strider", PetDataTable.RED_STRIDER_TWILIGHT_ID));
		restoreRules.put(10310, new EvolveRule(10310, 4424, 55, "Twilight Strider", PetDataTable.STRIDER_TWILIGHT_ID));
	}

	/**
	 * Evolui o item de controle do pet para sua forma superior, aceitando tanto o {@code itemId} do template
	 * quanto o {@code objectId} da instancia no inventario (#242, #243).
	 */
	public EvolveResult evolvePet(PlayerCharacter player, int baseControlItemId, int petLevel) {
		return executeItemConversion(player, baseControlItemId, petLevel, rules, "evoluir");
	}

	/**
	 * Evolui um Pet invocado resolvendo a coleira pelo {@code itemId()} real da instancia no inventario (#242, #243).
	 */
	public EvolveResult evolvePet(PlayerCharacter player, SummonInstance pet) {
		if (pet == null) {
			return EvolveResult.fail("Pet invalido.");
		}
		int controlId = resolveControlTemplateId(player, pet);
		return evolvePet(player, controlId, pet.level());
	}

	/**
	 * Restaura / converte entre variantes equivalentes de Pets de elite (#244):
	 * Great Wolf (10163/9882) <-> Snow Great Wolf (10307),
	 * Fenrir (10426) <-> Snow Fenrir (10611),
	 * Strider (4422/4423/4424) <-> Red Strider (10308/10309/10310).
	 */
	public EvolveResult restorePet(PlayerCharacter player, int controlItemId, int petLevel) {
		return executeItemConversion(player, controlItemId, petLevel, restoreRules, "restaurar");
	}

	public EvolveResult restorePet(PlayerCharacter player, int controlItemId) {
		int inferredLevel = 70;
		if (player != null && player.inventory() != null) {
			Optional<ItemInstance> itemOpt = player.inventory().byItemId(controlItemId)
					.or(() -> player.inventory().byObjectId(controlItemId));
			if (itemOpt.isPresent() && itemOpt.get().enchant() > 0) {
				inferredLevel = itemOpt.get().enchant();
			}
		}
		return restorePet(player, controlItemId, inferredLevel);
	}

	public EvolveResult restorePet(PlayerCharacter player, SummonInstance pet) {
		if (pet == null) {
			return EvolveResult.fail("Pet invalido.");
		}
		int controlId = resolveControlTemplateId(player, pet);
		return restorePet(player, controlId, pet.level());
	}

	private int resolveControlTemplateId(PlayerCharacter player, SummonInstance pet) {
		int itemObjId = pet.itemObjectId();
		if (player != null && player.inventory() != null && itemObjId > 0) {
			Optional<ItemInstance> byObj = player.inventory().byObjectId(itemObjId);
			if (byObj.isPresent()) {
				return byObj.get().itemId();
			}
		}
		if (rules.containsKey(itemObjId) || restoreRules.containsKey(itemObjId)) {
			return itemObjId;
		}
		PetDataTable table = PetDataTable.getInstance();
		if (table != null) {
			int mappedCollarId = table.getCollarItemIdByNpcId(pet.npcId());
			if (mappedCollarId > 0) {
				return mappedCollarId;
			}
		}
		return itemObjId;
	}

	private EvolveResult executeItemConversion(
			PlayerCharacter player,
			int controlIdOrObjectId,
			int petLevel,
			Map<Integer, EvolveRule> ruleMap,
			String actionVerb) {
		if (player == null || player.inventory() == null) {
			return EvolveResult.fail("Parametros invalidos.");
		}

		Inventory inv = player.inventory();
		ItemInstance baseItem = null;
		int templateItemId = controlIdOrObjectId;

		// 1. Verifica se controlIdOrObjectId ja e um template itemId conhecido nas regras
		EvolveRule rule = ruleMap.get(templateItemId);
		if (rule != null) {
			baseItem = inv.byItemId(templateItemId).orElse(null);
		}

		// 2. Fallback: se foi passado o objectId da coleira no inventario (#242/#243)
		if (baseItem == null) {
			Optional<ItemInstance> byObj = inv.byObjectId(controlIdOrObjectId);
			if (byObj.isPresent()) {
				baseItem = byObj.get();
				templateItemId = baseItem.itemId();
				rule = ruleMap.get(templateItemId);
			}
		}

		if (rule == null) {
			return EvolveResult.fail("Nenhuma evolucao disponivel para este item de controle de pet.");
		}

		if (petLevel < rule.minLevel()) {
			return EvolveResult.fail("Seu Pet precisa atingir no minimo o nivel " + rule.minLevel() + " para " + actionVerb + ".");
		}

		if (baseItem == null) {
			return EvolveResult.fail("Item de controle do pet nao encontrado no inventario.");
		}

		int oldCollarObjId = baseItem.objectId();

		// Consome item de controle original (persistindo via InventoryService se disponivel)
		if (inventoryService != null) {
			inventoryService.destroyItem(inv, oldCollarObjId, 1, "PetEvolve");
		} else {
			inv.remove(baseItem);
		}

		// Entrega o novo item de controle correspondente
		ItemInstance evolvedItem = null;
		if (inventoryService != null) {
			var addRes = inventoryService.addItem(inv, rule.evolvedItemId(), 1, "PetEvolve");
			if (addRes != null && addRes.item() != null) {
				evolvedItem = addRes.item();
			}
		}
		if (evolvedItem == null) {
			int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
			ItemTemplate tmpl = resolveTemplate(rule.evolvedItemId(), rule.petName() + " Item");
			evolvedItem = new ItemInstance(nextId, tmpl, player.objectId(), 1);
			inv.addItem(evolvedItem);
		}
		evolvedItem.enchant(petLevel);
		if (inventoryService != null) {
			inventoryService.saveItem(evolvedItem);
		}

		// Migra o registro da tabela pets para o novo objectId da coleira evoluida
		PetService activePetService = petService != null ? petService : PetService.getInstance();
		if (activePetService != null) {
			int newCollarObjId = evolvedItem.objectId();
			final EvolveRule appliedRule = rule;
			activePetService.getPet(oldCollarObjId).ifPresentOrElse(oldRec -> {
				activePetService.deletePet(oldCollarObjId);
				PetStatTemplate newStat = activePetService.petDataTable() != null && appliedRule.targetNpcId() > 0
						? activePetService.petDataTable().getStat(appliedRule.targetNpcId(), Math.max(petLevel, oldRec.level())).orElse(null)
						: null;
				int newLv = Math.max(petLevel, oldRec.level());
				int hp = newStat != null ? newStat.hpMax() : oldRec.curHp();
				int mp = newStat != null ? newStat.mpMax() : oldRec.curMp();
				int fed = newStat != null ? newStat.feedMax() : oldRec.fed();
				activePetService.savePet(new PetDataRecord(
						newCollarObjId,
						appliedRule.petName(),
						newLv,
						hp,
						mp,
						oldRec.exp(),
						oldRec.sp(),
						fed,
						oldRec.weapon(),
						oldRec.armor(),
						oldRec.jewel()));
			}, () -> {
				if (appliedRule.targetNpcId() > 0) {
					PetStatTemplate newStat = activePetService.petDataTable() != null
							? activePetService.petDataTable().getStat(appliedRule.targetNpcId(), petLevel).orElse(null)
							: null;
					int hp = newStat != null ? newStat.hpMax() : 1500;
					int mp = newStat != null ? newStat.mpMax() : 500;
					int fed = newStat != null ? newStat.feedMax() : 1000;
					activePetService.savePet(new PetDataRecord(
							newCollarObjId, appliedRule.petName(), petLevel, hp, mp, 0L, 0, fed, 0, 0, 0));
				}
			});
		}

		// Atualiza referencia se o pet estiver invocado
		SummonInstance activeSummon = player.activeSummon();
		if (activeSummon != null && activeSummon.itemObjectId() == oldCollarObjId) {
			activeSummon.name(rule.petName());
			activeSummon.level(petLevel);
		}

		log.info("PetEvolveService: Pet do jogador {} convertido com sucesso para {} (item {})!",
				player.name(), rule.petName(), rule.evolvedItemId());
		return EvolveResult.ok(rule.evolvedItemId(), "Parabens! Seu Pet evoluiu para " + rule.petName() + "!");
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) return opt.get();
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "pet", "normal", 100, "none", 0, true, true, true, true);
	}
}
