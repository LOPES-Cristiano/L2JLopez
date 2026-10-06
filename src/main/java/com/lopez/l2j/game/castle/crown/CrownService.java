package com.lopez.l2j.game.castle.crown;

import com.lopez.l2j.game.castle.Castle;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico responsavel pelo gerenciamento da Coroa do Senhor do Castelo (Lord's Crown)
 * e dos Diademas de cada castelo (CrownManager do L2JDream).
 * Valida elegibilidade ao logar ou mudar de cla/castelo, remove itens indevidos e premia os lordes.
 */
@Service
public class CrownService {

	private static final Logger log = LoggerFactory.getLogger(CrownService.class);

	private final CastleManager castleManager;
	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	@Autowired
	public CrownService(CastleManager castleManager,
						@Autowired(required = false) ItemTemplateTable itemTable,
						@Autowired(required = false) ObjectIdFactory idFactory) {
		this.castleManager = castleManager;
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x50000000);
	}

	/**
	 * Verifica e normaliza a posse de coroas e diademas no inventario do jogador.
	 * Portado diretamente de CrownManager.checkCrowns(activeChar).
	 */
	public void checkCrowns(PlayerCharacter player, Clan clan) {
		if (player == null || player.inventory() == null) {
			return;
		}

		Inventory inv = player.inventory();
		int validCircletId = 0;
		boolean isLeader = false;

		if (clan != null) {
			int castleId = clan.castleId();
			if (castleId > 0) {
				validCircletId = CrownTable.getCircletId(castleId);
			}
			isLeader = clan.isLeader(player.objectId());
		}

		boolean alreadyFoundCirclet = false;
		boolean alreadyFoundCrown = false;

		List<ItemInstance> toRemove = new ArrayList<>();

		for (ItemInstance item : inv.items()) {
			if (CrownTable.isCrownOrCirclet(item.itemId())) {
				if (validCircletId > 0 && item.itemId() == validCircletId) {
					if (!alreadyFoundCirclet) {
						alreadyFoundCirclet = true;
						continue;
					}
				} else if (item.itemId() == CrownTable.LORDS_CROWN && isLeader) {
					if (!alreadyFoundCrown) {
						alreadyFoundCrown = true;
						continue;
					}
				}

				// Item invalido ou duplicado
				if (item.itemId() == CrownTable.LORDS_CROWN) {
					toRemove.add(item);
					log.info("Removendo Coroa do Lorde de {}, pois nao e lorde de castelo", player.name());
				} else if (item.isEquipped()) {
					inv.unequip(item);
					log.info("Desequipando diadema invalido {} de {}", item.itemId(), player.name());
				}
			}
		}

		for (ItemInstance item : toRemove) {
			inv.remove(item);
		}
	}

	/**
	 * Concede a Coroa do Senhor (Lord's Crown 6841) e o Diadema do Castelo ao lider vitorioso.
	 */
	public void rewardLordCrown(PlayerCharacter leader, int castleId) {
		if (leader == null || leader.inventory() == null) {
			return;
		}

		Inventory inv = leader.inventory();

		// Concede Coroa do Lorde se nao possuir
		if (inv.byItemId(CrownTable.LORDS_CROWN).isEmpty()) {
			ItemTemplate tmpl = resolveTemplate(CrownTable.LORDS_CROWN, "Lord's Crown");
			ItemInstance crown = new ItemInstance(idFactory.nextId(), tmpl, leader.objectId(), 1);
			inv.add(crown);
			log.info("Coroa do Senhor do Castelo entregue a {}", leader.name());
		}

		// Concede Diadema da provincia se nao possuir
		int circletId = CrownTable.getCircletId(castleId);
		if (circletId > 0 && inv.byItemId(circletId).isEmpty()) {
			ItemTemplate tmpl = resolveTemplate(circletId, "Castle Circlet");
			ItemInstance circlet = new ItemInstance(idFactory.nextId(), tmpl, leader.objectId(), 1);
			inv.add(circlet);
			log.info("Diadema do Castelo {} entregue a {}", castleId, leader.name());
		}
	}

	/**
	 * Remove a Coroa do Senhor do jogador quando seu cla perde o castelo.
	 */
	public void removeLordCrown(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return;
		}

		Inventory inv = player.inventory();
		inv.byItemId(CrownTable.LORDS_CROWN).ifPresent(crown -> {
			inv.remove(crown);
			log.info("Coroa do Senhor removida de {}", player.name());
		});
	}

	private ItemTemplate resolveTemplate(int itemId, String defaultName) {
		if (itemTable != null) {
			var opt = itemTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.armor(itemId, itemId, defaultName, "hair", "none", 10, "none", 0, 0, 0, false, false, false, false);
	}
}
