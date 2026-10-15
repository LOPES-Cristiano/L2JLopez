package com.lopez.l2j.game.pet;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.summon.SummonInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico de Evolucao Oficial de Pets nos Pet Managers (L2JLucera2) - Onda F7.
 * - Wolf (lvl 55+) -> Great Wolf (collar 2375 -> necklace 9882)
 * - Hatchling (lvl 55+) -> Strider (flute 3500/3501/3502 -> 4422/4423/4424)
 * - Baby Pets (lvl 55+) -> Improved Baby Pets (6648/6649/6650 -> 10311/10312/10313)
 */
@Service
public class PetEvolveService {

	private static final Logger log = LoggerFactory.getLogger(PetEvolveService.class);

	public record EvolveRule(int baseItemId, int evolvedItemId, int minLevel, String petName) {}

	public record EvolveResult(boolean success, int evolvedItemId, String message) {
		public static EvolveResult ok(int id, String msg) {
			return new EvolveResult(true, id, msg);
		}
		public static EvolveResult fail(String msg) {
			return new EvolveResult(false, 0, msg);
		}
	}

	private final Map<Integer, EvolveRule> rules = new ConcurrentHashMap<>();
	private final AtomicInteger objIdGen = new AtomicInteger(920000);

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory;

	public PetEvolveService() {
		initRules();
	}

	private void initRules() {
		// Wolf -> Great Wolf
		rules.put(2375, new EvolveRule(2375, 9882, 55, "Great Wolf"));
		// Hatchlings -> Striders (Wind, Star, Twilight)
		rules.put(3500, new EvolveRule(3500, 4422, 55, "Wind Strider"));
		rules.put(3501, new EvolveRule(3501, 4423, 55, "Star Strider"));
		rules.put(3502, new EvolveRule(3502, 4424, 55, "Twilight Strider"));
		// Baby Pets -> Improved Baby Pets
		rules.put(6648, new EvolveRule(6648, 10311, 55, "Improved Baby Buffalo"));
		rules.put(6649, new EvolveRule(6649, 10312, 55, "Improved Baby Cougar"));
		rules.put(6650, new EvolveRule(6650, 10313, 55, "Improved Baby Kookaburra"));
	}

	public EvolveResult evolvePet(PlayerCharacter player, int baseControlItemId, int petLevel) {
		if (player == null || player.inventory() == null) {
			return EvolveResult.fail("Parametros invalidos.");
		}

		EvolveRule rule = rules.get(baseControlItemId);
		if (rule == null) {
			return EvolveResult.fail("Nenhuma evolucao disponivel para este item de controle de pet.");
		}

		if (petLevel < rule.minLevel()) {
			return EvolveResult.fail("Seu Pet precisa atingir no minimo o nivel " + rule.minLevel() + " para evoluir.");
		}

		Inventory inv = player.inventory();
		var baseItemOpt = inv.byItemId(baseControlItemId);
		if (baseItemOpt.isEmpty()) {
			return EvolveResult.fail("Item de controle do pet nao encontrado no inventario.");
		}

		// Consome item de controle original
		inv.remove(baseItemOpt.get());

		// Entrega o novo item de controle correspondente
		int nextId = (objectIdFactory != null) ? objectIdFactory.nextId() : objIdGen.incrementAndGet();
		ItemTemplate tmpl = resolveTemplate(rule.evolvedItemId(), rule.petName() + " Item");
		ItemInstance evolvedItem = new ItemInstance(nextId, tmpl, player.objectId(), 1);
		inv.addItem(evolvedItem);

		log.info("PetEvolveService: Pet do jogador {} evoluiu com sucesso para {}!", player.name(), rule.petName());
		return EvolveResult.ok(rule.evolvedItemId(), "Parabens! Seu Pet evoluiu para " + rule.petName() + "!");
	}

	public EvolveResult evolvePet(PlayerCharacter player, SummonInstance pet) {
		if (pet == null) {
			return EvolveResult.fail("Pet invalido.");
		}
		int controlId = pet.itemObjectId();
		return evolvePet(player, controlId, pet.level());
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) return opt.get();
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "pet", "normal", 100, "none", 0, true, true, true, true);
	}
}
