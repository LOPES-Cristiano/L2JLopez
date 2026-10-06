package com.lopez.l2j.game.cursed;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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

	public static final int ZARICHE = 8190;
	public static final int AKAMANAH = 8689;

	private final Map<Integer, CursedWeapon> weapons = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;

	@Autowired
	public CursedWeaponsManager(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadXml("data/xml/world/cursedWeapons.xml");
		loadFromDb();
	}

	public CursedWeaponsManager(String xmlPath, JdbcClient jdbc) {
		this.jdbc = jdbc;
		loadXml(xmlPath);
		loadFromDb();
	}

	public boolean isCursedWeapon(int itemId) {
		return weapons.containsKey(itemId);
	}

	public Optional<CursedWeapon> getCursedWeapon(int itemId) {
		return Optional.ofNullable(weapons.get(itemId));
	}

	public Collection<CursedWeapon> allWeapons() {
		return Collections.unmodifiableCollection(weapons.values());
	}

	public List<CursedWeapon> activeOrDroppedWeapons() {
		return weapons.values().stream()
				.filter(w -> w.isActive() || w.isDropped())
				.toList();
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
				persistDb(cw);
				log.info("Arma amaldicoada {} (ID {}) dropou no mundo em ({}, {}, {}) pelo monstro {}",
						cw.name(), cw.itemId(), x, y, z, monsterId);
				return cw;
			}
		}
		return null;
	}

	/**
	 * Forca o drop de uma arma amaldicoada no chao (usado por comando GM ou evento).
	 */
	public synchronized boolean forceDrop(int itemId, int x, int y, int z) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null && !cw.isActive()) {
			cw.dropOnGround(x, y, z);
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
	 * Incrementa os abates PvP do portador da arma amaldicoada.
	 */
	public void onKill(int itemId) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null && cw.isActive()) {
			cw.increaseKills();
			persistDb(cw);
		}
	}

	/**
	 * Remove a arma amaldicoada quando expira ou quando o portador morre.
	 */
	public synchronized void endOfLife(int itemId) {
		CursedWeapon cw = weapons.get(itemId);
		if (cw != null) {
			cw.endOfLife();
			removeFromDb(itemId);
			log.info("Arma amaldicoada {} expirada e removida do mundo", cw.name());
		}
	}

	private void loadXml(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("Arquivo cursedWeapons.xml nao encontrado: {}", file.getAbsolutePath());
			// Fallback defaults se arquivo nao existir
			weapons.put(ZARICHE, new CursedWeapon(ZARICHE, 3603, "Demonic Sword Zariche", 1, 300, 10));
			weapons.put(AKAMANAH, new CursedWeapon(AKAMANAH, 3629, "Blood Sword Akamanah", 1, 300, 10));
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

	private void loadFromDb() {
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
				int charId = ((Number) row.get("charId")).intValue();
				int nbKills = ((Number) row.get("nbKills")).intValue();
				long endTime = ((Number) row.get("endTime")).longValue();

				CursedWeapon cw = weapons.get(itemId);
				if (cw != null) {
					if (System.currentTimeMillis() >= endTime) {
						removeFromDb(itemId);
					} else {
						cw.playerId(charId);
						cw.kills(nbKills);
						cw.endTime(endTime);
						cw.setActive(charId > 0);
						cw.setDropped(charId == 0);
					}
				}
			}
		} catch (Exception ex) {
			log.warn("Nao foi possivel restaurar cursed_weapons do banco: {}", ex.getMessage());
		}
	}

	private void persistDb(CursedWeapon cw) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO cursed_weapons (itemId, charId, nbKills, endTime)
					VALUES (:itemId, :charId, :nbKills, :endTime)
					ON DUPLICATE KEY UPDATE
						charId = :charId,
						nbKills = :nbKills,
						endTime = :endTime
					""")
					.param("itemId", cw.itemId())
					.param("charId", cw.playerId())
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
