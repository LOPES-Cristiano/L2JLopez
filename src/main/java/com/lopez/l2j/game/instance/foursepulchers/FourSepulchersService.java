package com.lopez.l2j.game.instance.foursepulchers;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico dos Quatro Sepulcros (Four Sepulchers de Lineage II Interlude).
 * Gerencia os 4 mausoleus (Conquerors, Emperors, Sages, Judges), chaves de capela,
 * combate com a Sombra de Halisha e a troca dos 4 cálices pelo passe do Túmulo Imperial.
 */
@Service
public class FourSepulchersService {

	private static final Logger log = LoggerFactory.getLogger(FourSepulchersService.class);

	public static final int SEPULCHER_CONQUERORS = 1;
	public static final int SEPULCHER_EMPERORS = 2;
	public static final int SEPULCHER_SAGES = 3;
	public static final int SEPULCHER_JUDGES = 4;

	// Itens
	public static final int PASS_OF_DARKNESS = 7075;
	public static final int CHAPEL_KEY = 7260;
	public static final int GOBLET_CONQUERORS = 7256; // Goblet of Aethelred
	public static final int GOBLET_EMPERORS = 7257;   // Goblet of Wigglie
	public static final int GOBLET_SAGES = 7258;       // Goblet of Konrad
	public static final int GOBLET_JUDGES = 7259;      // Goblet of Rahman
	public static final int IMPERIAL_TOMB_PASS = 8073;

	// NPCs Chefes
	public static final int SHADOW_OF_HALISHA_CONQUERORS = 25339;
	public static final int SHADOW_OF_HALISHA_EMPERORS = 25340;
	public static final int SHADOW_OF_HALISHA_SAGES = 25341;
	public static final int SHADOW_OF_HALISHA_JUDGES = 25342;

	public static final int MIN_LEVEL = 78;
	public static final int MIN_PARTY_MEMBERS = 4;

	public static class SepulcherMausoleum {
		private final int id;
		private final String name;
		private final int bossId;
		private final int gobletId;
		private volatile int stage = 0; // 0=livre, 1..5=salas
		private volatile int leaderId = 0;
		private final List<Integer> players = new CopyOnWriteArrayList<>();

		public SepulcherMausoleum(int id, String name, int bossId, int gobletId) {
			this.id = id;
			this.name = name;
			this.bossId = bossId;
			this.gobletId = gobletId;
		}

		public int id() { return id; }
		public String name() { return name; }
		public int bossId() { return bossId; }
		public int gobletId() { return gobletId; }
		public int stage() { return stage; }
		public void stage(int stage) { this.stage = stage; }
		public int leaderId() { return leaderId; }
		public void leaderId(int leaderId) { this.leaderId = leaderId; }
		public List<Integer> players() { return players; }
		public boolean isInUse() { return stage > 0; }
		public void reset() {
			stage = 0;
			leaderId = 0;
			players.clear();
		}
	}

	private final Map<Integer, SepulcherMausoleum> mausoleums = new ConcurrentHashMap<>();
	private volatile SepulcherStatus cycleStatus = SepulcherStatus.ENTRY_TIME;

	private final ItemTemplateTable itemTable;
	private final ObjectIdFactory idFactory;

	@Autowired
	public FourSepulchersService(@Autowired(required = false) ItemTemplateTable itemTable,
								 @Autowired(required = false) ObjectIdFactory idFactory) {
		this.itemTable = itemTable;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x60000000);
		initMausoleums();
	}

	private void initMausoleums() {
		mausoleums.put(SEPULCHER_CONQUERORS, new SepulcherMausoleum(SEPULCHER_CONQUERORS, "Conquerors", SHADOW_OF_HALISHA_CONQUERORS, GOBLET_CONQUERORS));
		mausoleums.put(SEPULCHER_EMPERORS, new SepulcherMausoleum(SEPULCHER_EMPERORS, "Emperors", SHADOW_OF_HALISHA_EMPERORS, GOBLET_EMPERORS));
		mausoleums.put(SEPULCHER_SAGES, new SepulcherMausoleum(SEPULCHER_SAGES, "Sages", SHADOW_OF_HALISHA_SAGES, GOBLET_SAGES));
		mausoleums.put(SEPULCHER_JUDGES, new SepulcherMausoleum(SEPULCHER_JUDGES, "Judges", SHADOW_OF_HALISHA_JUDGES, GOBLET_JUDGES));
	}

	public SepulcherStatus getCycleStatus() {
		return cycleStatus;
	}

	public void setCycleStatus(SepulcherStatus cycleStatus) {
		this.cycleStatus = cycleStatus;
		if (cycleStatus == SepulcherStatus.COOL_TIME) {
			mausoleums.values().forEach(SepulcherMausoleum::reset);
		}
		log.info("Ciclo dos Quatro Sepulcros alterado para: {}", cycleStatus);
	}

	public SepulcherMausoleum getMausoleum(int id) {
		return mausoleums.get(id);
	}

	public boolean canEnter(PlayerCharacter leader, List<PlayerCharacter> party) {
		if (leader == null || leader.level() < MIN_LEVEL) {
			return false;
		}

		if (cycleStatus != SepulcherStatus.ENTRY_TIME) {
			return false;
		}

		if (party != null && party.size() < MIN_PARTY_MEMBERS) {
			return false;
		}

		Inventory inv = leader.inventory();
		return inv != null && inv.byItemId(PASS_OF_DARKNESS).isPresent();
	}

	/**
	 * Entra em um dos 4 mausoleus com o grupo.
	 */
	public boolean enterSepulcher(int sepulcherId, PlayerCharacter leader, List<PlayerCharacter> party) {
		if (!canEnter(leader, party)) {
			return false;
		}

		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || m.isInUse()) {
			return false;
		}

		// Consome o Pass of Darkness do lider
		leader.inventory().byItemId(PASS_OF_DARKNESS).ifPresent(leader.inventory()::remove);

		m.reset();
		m.leaderId(leader.objectId());
		m.stage(1); // Primeira sala

		if (party != null) {
			for (PlayerCharacter pc : party) {
				m.players().add(pc.objectId());
			}
		} else {
			m.players().add(leader.objectId());
		}

		log.info("{} e grupo entraram no Sepulcro {} ({})", leader.name(), sepulcherId, m.name());
		return true;
	}

	/**
	 * Limpa a sala atual: concede a Chave da Capela (Chapel Key) e avanca para a proxima sala.
	 */
	public boolean clearRoom(int sepulcherId, PlayerCharacter leader) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || !m.isInUse() || m.stage() >= 5) {
			return false;
		}

		// Concede a Chapel Key ao lider para abrir o portao
		deliverItem(leader, CHAPEL_KEY, 1);
		m.stage(m.stage() + 1);
		log.info("Sepulcro {} ({}) avancou para a Sala {}/5. Chave da Capela concedida a {}",
				sepulcherId, m.name(), m.stage(), leader.name());
		return true;
	}

	/**
	 * Derrota a Sombra de Halisha na 5ª sala e entrega o Cálice correspondente.
	 */
	public boolean defeatShadowOfHalisha(int sepulcherId, PlayerCharacter leader) {
		SepulcherMausoleum m = mausoleums.get(sepulcherId);
		if (m == null || m.stage() < 5) {
			return false;
		}

		deliverItem(leader, m.gobletId(), 1);
		m.reset(); // Sepulcro concluido com sucesso
		log.info("Sombra de Halisha derrotada no Sepulcro {}! Calice {} entregue a {}",
				sepulcherId, m.gobletId(), leader.name());
		return true;
	}

	/**
	 * Troca os 4 Cálices conquistados pelo Passe do Túmulo Imperial (Frintezza).
	 */
	public boolean combineFourGoblets(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return false;
		}

		Inventory inv = player.inventory();
		boolean hasAll = inv.byItemId(GOBLET_CONQUERORS).isPresent()
				&& inv.byItemId(GOBLET_EMPERORS).isPresent()
				&& inv.byItemId(GOBLET_SAGES).isPresent()
				&& inv.byItemId(GOBLET_JUDGES).isPresent();

		if (!hasAll) {
			return false;
		}

		// Consome os 4 calices
		inv.byItemId(GOBLET_CONQUERORS).ifPresent(inv::remove);
		inv.byItemId(GOBLET_EMPERORS).ifPresent(inv::remove);
		inv.byItemId(GOBLET_SAGES).ifPresent(inv::remove);
		inv.byItemId(GOBLET_JUDGES).ifPresent(inv::remove);

		// Concede o Passe do Tumulo Imperial
		deliverItem(player, IMPERIAL_TOMB_PASS, 1);
		log.info("{} combinou os 4 Calices e obteve o Passe do Tumulo Imperial (Item {})!",
				player.name(), IMPERIAL_TOMB_PASS);
		return true;
	}

	private void deliverItem(PlayerCharacter player, int itemId, int count) {
		if (player == null || player.inventory() == null) {
			return;
		}
		Inventory inv = player.inventory();
		var opt = inv.byItemId(itemId);
		if (opt.isPresent() && opt.get().template().stackable()) {
			opt.get().count(opt.get().count() + count);
		} else {
			ItemTemplate tmpl = resolveTemplate(itemId);
			ItemInstance item = new ItemInstance(idFactory.nextId(), tmpl, player.objectId(), count);
			inv.add(item);
		}
	}

	private ItemTemplate resolveTemplate(int itemId) {
		if (itemTable != null) {
			var opt = itemTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, "Sepulcher Quest Item", "quest", "none", 1, "none", 0, false, false, false, false);
	}
}
