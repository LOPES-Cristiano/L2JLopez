package com.lopez.l2j.game.pet;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico para gerenciamento de mascotes (Pets) e montarias:
 * ciclo de alimentacao (fome), experiencia, evolucao de nivel e montaria.
 * Porta de L2PetInstance e PetDataTable de L2JDream.
 */
@Service
public class PetService {

	private static final Logger log = LoggerFactory.getLogger(PetService.class);

	private final PetDataTable petDataTable;
	private final JdbcClient jdbc;
	private final InventoryService inventoryService;

	private final Map<Integer, PetDataRecord> activePets = new ConcurrentHashMap<>();

	@Autowired
	public PetService(PetDataTable petDataTable,
					  @Autowired(required = false) JdbcClient jdbc,
					  @Autowired(required = false) InventoryService inventoryService) {
		this.petDataTable = petDataTable;
		this.jdbc = jdbc;
		this.inventoryService = inventoryService;
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
			PetStatTemplate stat = petDataTable.getStat(npcId, 1).orElse(null);
			int hp = stat != null ? stat.hpMax() : 100;
			int mp = stat != null ? stat.mpMax() : 100;
			int fed = stat != null ? stat.feedMax() : 100;

			PetDataRecord created = new PetDataRecord(itemObjectId, defaultName, 1, hp, mp, 0L, 0, fed, 0, 0, 0);
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
		if (jdbc != null) {
			try {
				jdbc.sql("DELETE FROM pets WHERE item_obj_id = ?").param(itemObjectId).update();
			} catch (Exception e) {
				log.warn("Erro ao remover pet {}: {}", itemObjectId, e.getMessage());
			}
		}
	}

	/**
	 * Alimenta o pet consumindo a racao correspondente.
	 */
	public boolean feed(PlayerCharacter player, int itemObjectId, int npcId, int foodItemId, Consumer<GameServerPacket> clientSender) {
		if (player == null) {
			return false;
		}

		if (!petDataTable.isPetFood(npcId, foodItemId)) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu mascote recusa essa comida."));
			}
			return false;
		}

		Inventory inv = player.inventory();
		if (inv == null || inv.getItemCount(foodItemId) <= 0) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui o alimento necessario."));
			}
			return false;
		}

		// Consome 1 alimento
		if (inventoryService != null) {
			inventoryService.consumeItem(inv, foodItemId, 1, "PetFeed");
		} else {
			inv.destroyItemByItemId(foodItemId, 1);
		}

		PetDataRecord pet = createOrGetDefault(itemObjectId, npcId, "Pet");
		PetStatTemplate stat = petDataTable.getStat(npcId, pet.level()).orElse(null);
		int maxFed = stat != null ? stat.feedMax() : 100;
		int fedGain = Math.max(10, maxFed * 20 / 100); // Recupera 20%
		int newFed = Math.min(maxFed, pet.fed() + fedGain);

		PetDataRecord updated = pet.withFed(newFed);
		savePet(updated);

		if (clientSender != null) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "Pet", pet.name() + " comeu com satisfacao! Fome: " + newFed + "/" + maxFed));
		}
		return true;
	}

	/**
	 * Concede experiencia ao mascote e processa level-up se necessario.
	 */
	public boolean addExp(int itemObjectId, int npcId, long expAmount, Consumer<GameServerPacket> clientSender) {
		if (expAmount <= 0) {
			return false;
		}

		PetDataRecord pet = createOrGetDefault(itemObjectId, npcId, "Pet");
		long totalExp = pet.exp() + expAmount;
		int curLevel = pet.level();

		// Verifica se sobe de nivel
		while (curLevel < 85) {
			Optional<PetStatTemplate> nextStat = petDataTable.getStat(npcId, curLevel + 1);
			if (nextStat.isPresent() && totalExp >= nextStat.get().expMax()) {
				curLevel++;
			} else {
				break;
			}
		}

		PetStatTemplate currentStat = petDataTable.getStat(npcId, curLevel).orElse(null);
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
	 * Monta o jogador no pet especificado (se montavel).
	 */
	public boolean mount(PlayerCharacter player, int petNpcId, Consumer<GameServerPacket> clientSender) {
		if (player == null) {
			return false;
		}
		if (player.isMounted()) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce ja esta montado."));
			}
			return false;
		}
		if (!petDataTable.isMountable(petNpcId)) {
			if (clientSender != null) {
				clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Esta criatura nao serve como montaria."));
			}
			return false;
		}

		player.mountNpcId(petNpcId);
		if (clientSender != null) {
			clientSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce montou com sucesso!"));
		}
		return true;
	}

	/**
	 * Desmonta o jogador.
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

	/**
	 * Evolui um mascote que atingiu nivel minimo (ex: Wolf Lv 55 -> Great Wolf).
	 */
	public Optional<PetDataRecord> evolve(PlayerCharacter player, int collarObjectId, int oldNpcId, int newNpcId, String newName) {
		PetDataRecord pet = getPet(collarObjectId).orElse(null);
		if (pet == null || pet.level() < 55) {
			return Optional.empty();
		}

		PetStatTemplate newStat = petDataTable.getStat(newNpcId, pet.level()).orElse(null);
		int hp = newStat != null ? newStat.hpMax() : pet.curHp();
		int mp = newStat != null ? newStat.mpMax() : pet.curMp();
		int fed = newStat != null ? newStat.feedMax() : pet.fed();

		PetDataRecord evolved = new PetDataRecord(collarObjectId, newName, pet.level(), hp, mp, pet.exp(), pet.sp(), fed,
				pet.weapon(), pet.armor(), pet.jewel());
		savePet(evolved);
		log.info("Pet {} evoluiu com sucesso de NPC {} para NPC {}", pet.name(), oldNpcId, newNpcId);
		return Optional.of(evolved);
	}
}
