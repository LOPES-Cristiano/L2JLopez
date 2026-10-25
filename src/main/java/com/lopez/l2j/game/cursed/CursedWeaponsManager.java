package com.lopez.l2j.game.cursed;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Gerenciador das Armas Amaldicoadas (Zariche e Akamanah) de Lineage II Interlude.
 */
@Component
public class CursedWeaponsManager {

	private static final Logger log = LoggerFactory.getLogger(CursedWeaponsManager.class);
	private static volatile CursedWeaponsManager instance;

	public static final int ZARICHE = 8190;
	public static final int AKAMANAH = 8689;
	public static final int ZARICHE_ID = ZARICHE;
	public static final int AKAMANAH_ID = AKAMANAH;

	public static final int SKILL_ZARICHE = 3603;
	public static final int SKILL_AKAMANAH = 3629;
	public static final int SKILL_VOID_FLOW = 3630;
	public static final int SKILL_VOID_BURST = 3631;
	public static final int CURSED_KARMA = 9_999_999;

	private final Map<Integer, CursedWeapon> weapons = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> savedKarma = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> savedPkKills = new ConcurrentHashMap<>();
	private final Map<Integer, PlayerCharacter> activeHolders = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;
	private final ScheduledExecutorService expirationScheduler;

	public static CursedWeaponsManager getInstance() {
		return instance;
	}

	@Autowired
	public CursedWeaponsManager(@Autowired(required = false) JdbcClient jdbc) {
		this("data/xml/world/cursedWeapons.xml", jdbc);
	}

	public CursedWeaponsManager(String xmlPath, JdbcClient jdbc) {
		this.jdbc = jdbc;
		instance = this;
		initSchema();
		loadXml(xmlPath);
		loadFromDb();
		this.expirationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "CursedWeapons-ExpirationTask");
			t.setDaemon(true);
			return t;
		});
		this.expirationScheduler.scheduleAtFixedRate(this::checkExpirations, 60L, 60L, TimeUnit.SECONDS);
	}

	public void initSchema() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					CREATE TABLE IF NOT EXISTS cursed_weapons (
					  itemId INT NOT NULL DEFAULT 0,
					  charId INT DEFAULT 0,
					  playerKarma INT DEFAULT 0,
					  playerPkKills INT DEFAULT 0,
					  nbKills INT DEFAULT 0,
					  endTime DECIMAL(20,0) DEFAULT 0,
					  PRIMARY KEY (itemId)
					)
					""").update();
		} catch (Exception ex) {
			log.debug("cursed_weapons schema init: {}", ex.getMessage());
		}
	}

	public boolean isCursedWeapon(int itemId) {
		return weapons.containsKey(itemId);
	}

	public Optional<CursedWeapon> getCursedWeapon(int itemId) {
		return Optional.ofNullable(weapons.get(itemId));
	}

	public Optional<CursedWeapon> getActiveWeaponForPlayer(int playerId) {
		if (playerId <= 0) {
			return Optional.empty();
		}
		return weapons.values().stream()
				.filter(w -> w.isActive() && w.playerId() == playerId)
				.findFirst();
	}

	public int getCursedWeaponLevel(int playerId) {
		return getActiveWeaponForPlayer(playerId).map(CursedWeapon::stage).orElse(0);
	}

	public int getSavedKarma(int itemId) {
		return savedKarma.getOrDefault(itemId, 0);
	}

	public int getSavedPkKills(int itemId) {
		return savedPkKills.getOrDefault(itemId, 0);
	}

	public Collection<CursedWeapon> allWeapons() {
		return Collections.unmodifiableCollection(weapons.values());
	}

	public List<CursedWeapon> activeOrDroppedWeapons() {
		return weapons.values().stream()
				.filter(w -> w.isActive() || w.isDropped())
				.toList();
	}

	public synchronized void checkExpirations() {
		long now = System.currentTimeMillis();
		for (CursedWeapon cw : weapons.values()) {
			if ((cw.isActive() || cw.isDropped()) && cw.endTime() > 0 && now >= cw.endTime()) {
				endOfLife(cw.itemId());
			}
		}
	}

	/**
	 * Verifica chance de queda ao matar monstros no mundo.
	 *
	 * @return A arma dropada ou null se nenhuma dropou
	 */
	public synchronized CursedWeapon checkMonsterDrop(int monsterId, int x, int y, int z) {
		if (!com.lopez.l2j.config.Config.ALLOW_CURSED_WEAPONS) {
			return null;
		}
		for (CursedWeapon cw : weapons.values()) {
			if (cw.isActive() || cw.isDropped()) {
				continue;
			}
			int roll = ThreadLocalRandom.current().nextInt(1_000_000);
			if (roll < cw.dropRate()) {
				cw.dropOnGround(x, y, z);
				cw.endTime(System.currentTimeMillis() + ((long) cw.durationMinutes() * 60_000L));
				persistDb(cw);
				log.info("Arma amaldicoada {} (ID {}) dropou no mundo em ({}, {}, {}) pelo monstro {}",
						cw.name(), cw.itemId(), x, y, z, monsterId);
				return cw;
			}
		}
		return null;
	}

	public synchronized CursedWeapon checkDrop(int monsterId, PlayerCharacter killer) {
		if (killer == null) {
			return null;
		}
		return checkMonsterDrop(monsterId, killer.x(), killer.y(), killer.z());
	}

	public synchronized CursedWeapon checkDrop(int monsterId, PlayerCharacter killer, int x, int y, int z) {
		return checkMonsterDrop(monsterId, x, y, z);
	}

	/**
	 * Forca o drop de uma arma amaldicoada no chao (usado por comando GM ou evento).
	 */
	public synchronized boolean forceDrop(int itemId, int x, int y, int z) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null && !cw.isActive()) {
			cw.dropOnGround(x, y, z);
			cw.endTime(System.currentTimeMillis() + ((long) cw.durationMinutes() * 60_000L));
			persistDb(cw);
			log.info("Drop forcado da arma amaldicoada {} em ({}, {}, {})", cw.name(), x, y, z);
			return true;
		}
		return false;
	}

	/**
	 * Ativa a arma amaldicoada nas maos do jogador ao pegá-la do chão.
	 */
	public synchronized boolean activate(int itemId, int playerId, String playerName) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw == null) {
			return false;
		}
		cw.activate(playerId, playerName);
		persistDb(cw);
		log.info("Arma amaldicoada {} ativada pelo jogador {} (ID {})", cw.name(), playerName, playerId);
		return true;
	}

	/**
	 * Ativa a arma amaldicoada aplicando transformacao/skills (3603/3629 + 3630/3631), karma 9999999 e equipamento.
	 */
	public synchronized boolean activate(int itemId, PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		CursedWeapon cw = weapons.get(itemId);
		if (cw == null) {
			return false;
		}
		savedKarma.put(itemId, player.karma());
		savedPkKills.put(itemId, player.pkKills());
		activeHolders.put(itemId, player);

		cw.activate(player.objectId(), player.name());
		applyCursedStateToPlayer(cw, player);
		persistDb(cw);
		log.info("Arma amaldicoada {} ativada pelo jogador {} (ID {}) no estagio {}",
				cw.name(), player.name(), player.objectId(), cw.stage());
		return true;
	}

	public synchronized void onPlayerLogin(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		for (CursedWeapon cw : weapons.values()) {
			if (cw.isActive() && cw.playerId() == player.objectId()) {
				if (cw.endTime() > 0 && System.currentTimeMillis() >= cw.endTime()) {
					endOfLife(cw.itemId(), player);
				} else {
					activeHolders.put(cw.itemId(), player);
					applyCursedStateToPlayer(cw, player);
				}
			}
		}
	}

	private void applyCursedStateToPlayer(CursedWeapon cw, PlayerCharacter player) {
		player.karma(CURSED_KARMA);
		if (player.pkKills() <= 0) {
			player.pkKills(1);
		}
		if (player.party() != null) {
			player.party().removeMember(player);
			player.party(null);
		}
		if (player.isMounted()) {
			player.mountNpcId(0);
		}

		// Concede skill passiva da arma (3603 Zariche / 3629 Akamanah) no nivel = stage e ativas 3630/3631
		int stageLvl = Math.max(1, Math.min(10, cw.stage()));
		player.skills().put(cw.skillId(), stageLvl);
		player.skills().put(SKILL_VOID_FLOW, 1);
		player.skills().put(SKILL_VOID_BURST, 1);

		// Garante que o item exista e esteja equipado no slot LRHAND/RHAND
		var inv = player.inventory();
		if (inv != null) {
			ItemInstance cwItem = inv.byItemId(cw.itemId()).orElse(null);
			if (cwItem == null) {
				ItemTemplate tpl = ItemTemplate.weapon(cw.itemId(), cw.itemId(), cw.name(), "lrhand", "dualfist",
						1300, "s", 361, 169, 325, 80, 0, 0, false, false, false, false);
				cwItem = new ItemInstance(0x40000000 + cw.itemId(), tpl, player.objectId(), 1);
				inv.add(cwItem);
			}
			ItemInstance rhand = inv.paperdoll(ItemSlots.RHAND);
			if (rhand != null && rhand.itemId() != cw.itemId()) {
				inv.unequip(rhand);
			}
			ItemInstance lhand = inv.paperdoll(ItemSlots.LHAND);
			if (lhand != null && lhand.itemId() != cw.itemId()) {
				inv.unequip(lhand);
			}
			if (!cwItem.isEquipped()) {
				inv.equip(cwItem);
			}
		}
	}

	/**
	 * Incrementa os abates PvP do portador da arma amaldicoada.
	 */
	public synchronized void onKill(int itemId) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null && cw.isActive()) {
			cw.increaseKills();
			PlayerCharacter holder = activeHolders.get(itemId);
			if (holder != null) {
				holder.skills().put(cw.skillId(), cw.stage());
				holder.karma(CURSED_KARMA);
				holder.pkKills(holder.pkKills() + 1);
			}
			persistDb(cw);
		}
	}

	public synchronized void onKill(int itemId, PlayerCharacter killer) {
		if (killer != null) {
			activeHolders.put(itemId, killer);
		}
		onKill(itemId);
	}

	/**
	 * Processa morte em PvP envolvendo portadores de Cursed Weapon (#275-#277).
	 */
	public synchronized void onPlayerKilled(PlayerCharacter killer, PlayerCharacter victim) {
		if (victim != null) {
			CursedWeapon victimCw = getActiveWeaponForPlayer(victim.objectId()).orElse(null);
			if (victimCw != null) {
				restorePlayerState(victimCw, victim);
				// 50% de chance de cair no chao ou desaparecer
				if (ThreadLocalRandom.current().nextBoolean()) {
					victimCw.dropOnGround(victim.x(), victim.y(), victim.z());
					persistDb(victimCw);
				} else {
					endOfLife(victimCw.itemId(), victim);
				}
			}
		}
		if (killer != null) {
			CursedWeapon killerCw = getActiveWeaponForPlayer(killer.objectId()).orElse(null);
			if (killerCw != null) {
				onKill(killerCw.itemId(), killer);
			}
		}
	}

	/**
	 * Remove a arma amaldicoada quando expira ou quando o portador morre.
	 */
	public synchronized void endOfLife(int itemId) {
		PlayerCharacter holder = activeHolders.remove(itemId);
		endOfLife(itemId, holder);
	}

	public synchronized void endOfLife(int itemId, PlayerCharacter holder) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null) {
			if (holder != null) {
				restorePlayerState(cw, holder);
			}
			cw.endOfLife();
			savedKarma.remove(itemId);
			savedPkKills.remove(itemId);
			activeHolders.remove(itemId);
			removeFromDb(itemId);
			log.info("Arma amaldicoada {} expirada e removida do mundo", cw.name());
		}
	}

	private void restorePlayerState(CursedWeapon cw, PlayerCharacter holder) {
		int origKarma = savedKarma.getOrDefault(cw.itemId(), 0);
		int origPk = savedPkKills.getOrDefault(cw.itemId(), holder.pkKills());
		holder.karma(origKarma);
		holder.pkKills(origPk);
		holder.skills().remove(cw.skillId());
		holder.skills().remove(SKILL_VOID_FLOW);
		holder.skills().remove(SKILL_VOID_BURST);
		var inv = holder.inventory();
		if (inv != null) {
			inv.byItemId(cw.itemId()).ifPresent(inv::remove);
		}
	}

	public List<Integer> getCursedWeaponIds() {
		return new java.util.ArrayList<>(weapons.keySet());
	}

	public List<CursedWeapon> getActiveCursedWeapons() {
		return activeOrDroppedWeapons();
	}

	public void loadXml(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo cursedWeapons.xml nao encontrado: {}", file.getAbsolutePath());
			weapons.put(ZARICHE, new CursedWeapon(ZARICHE, SKILL_ZARICHE, "Demonic Sword Zariche", 1, 300, 10));
			weapons.put(AKAMANAH, new CursedWeapon(AKAMANAH, SKILL_AKAMANAH, "Blood Sword Akamanah", 1, 300, 10));
			return;
		}

		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(file);
			NodeList itemNodes = doc.getElementsByTagName("item");

			for (int i = 0; i < itemNodes.getLength(); i++) {
				Node node = itemNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element elem = (Element) node;

				int id = Integer.parseInt(elem.getAttribute("id"));
				int skillId = Integer.parseInt(elem.getAttribute("skillId"));
				String name = elem.getAttribute("name");
				int dropRate = 1;
				int duration = 300;
				int stageKills = 10;

				NodeList children = elem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					Node child = children.item(j);
					if (child.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					Element ce = (Element) child;
					String tag = ce.getTagName().toLowerCase();
					switch (tag) {
						case "droprate" -> dropRate = Integer.parseInt(ce.getAttribute("val"));
						case "duration" -> duration = Integer.parseInt(ce.getAttribute("val"));
						case "stagekills" -> stageKills = Integer.parseInt(ce.getAttribute("val"));
					}
				}

				weapons.put(id, new CursedWeapon(id, skillId, name, dropRate, duration, stageKills));
			}

			log.info("CursedWeaponsManager carregou {} armas amaldicoadas de {}", weapons.size(), filePath);
		} catch (Exception ex) {
			log.error("Erro ao carregar cursedWeapons.xml de {}", filePath, ex);
		}
	}

	public void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT itemId, charId, playerKarma, playerPkKills, nbKills, endTime
					FROM cursed_weapons
					""").query().listOfRows();

			for (var row : rows) {
				int itemId = ((Number) row.get("itemId")).intValue();
				int charId = row.get("charId") != null ? ((Number) row.get("charId")).intValue() : 0;
				int pKarma = row.get("playerKarma") != null ? ((Number) row.get("playerKarma")).intValue() : 0;
				int pPkKills = row.get("playerPkKills") != null ? ((Number) row.get("playerPkKills")).intValue() : 0;
				int nbKills = row.get("nbKills") != null ? ((Number) row.get("nbKills")).intValue() : 0;
				long endTime = row.get("endTime") != null ? ((Number) row.get("endTime")).longValue() : 0L;

				CursedWeapon cw = weapons.get(itemId);
				if (cw != null) {
					if (endTime > 0 && System.currentTimeMillis() >= endTime) {
						removeFromDb(itemId);
					} else {
						cw.playerId(charId);
						cw.kills(nbKills);
						cw.endTime(endTime);
						cw.setActive(charId > 0);
						cw.setDropped(charId == 0);
						savedKarma.put(itemId, pKarma);
						savedPkKills.put(itemId, pPkKills);
					}
				}
			}
		} catch (Exception ex) {
			log.warn("Nao foi possivel restaurar cursed_weapons do banco: {}", ex.getMessage());
		}
	}

	public void saveAllToDb() {
		if (jdbc == null) {
			return;
		}
		initSchema();
		for (CursedWeapon cw : weapons.values()) {
			if (cw.isActive() || cw.isDropped()) {
				persistDb(cw);
			}
		}
	}

	public void saveToDb() {
		saveAllToDb();
	}

	private void persistDb(CursedWeapon cw) {
		if (jdbc == null) {
			return;
		}
		int pKarma = savedKarma.getOrDefault(cw.itemId(), 0);
		int pPk = savedPkKills.getOrDefault(cw.itemId(), 0);
		try {
			jdbc.sql("DELETE FROM cursed_weapons WHERE itemId = :itemId")
					.param("itemId", cw.itemId())
					.update();
			jdbc.sql("""
					INSERT INTO cursed_weapons (itemId, charId, playerKarma, playerPkKills, nbKills, endTime)
					VALUES (:itemId, :charId, :playerKarma, :playerPkKills, :nbKills, :endTime)
					""")
					.param("itemId", cw.itemId())
					.param("charId", cw.playerId())
					.param("playerKarma", pKarma)
					.param("playerPkKills", pPk)
					.param("nbKills", cw.kills())
					.param("endTime", cw.endTime())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir cursed weapon {}", cw.itemId(), ex);
		}
	}

	private void removeFromDb(int itemId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM cursed_weapons WHERE itemId = :id")
					.param("id", itemId)
					.update();
		} catch (Exception ex) {
			log.error("Erro ao remover cursed weapon {} do banco", itemId, ex);
		}
	}
}
