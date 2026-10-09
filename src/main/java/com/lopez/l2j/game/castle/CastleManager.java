package com.lopez.l2j.game.castle;

import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.game.zone.ZoneType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Gerenciador dos 9 castelos de Lineage II Interlude (CastleManager do L2JDream).
 * Sincroniza estado com a tabela castle do banco de dados.
 */
@Component
public class CastleManager {

	private static final Logger log = LoggerFactory.getLogger(CastleManager.class);

	public static final int GLUDIO = 1;
	public static final int DION = 2;
	public static final int GIRAN = 3;
	public static final int OREN = 4;
	public static final int ADEN = 5;
	public static final int INNADRIL = 6;
	public static final int GODDARD = 7;
	public static final int RUNE = 8;
	public static final int SCHUTTGART = 9;

	private static final Map<Integer, String> DEFAULT_CASTLES = Map.of(
			GLUDIO, "Gludio",
			DION, "Dion",
			GIRAN, "Giran",
			OREN, "Oren",
			ADEN, "Aden",
			INNADRIL, "Innadril",
			GODDARD, "Goddard",
			RUNE, "Rune",
			SCHUTTGART, "Schuttgart"
	);

	private final Map<Integer, Castle> byId = new ConcurrentHashMap<>();
	private final Map<String, Castle> byName = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;

	@Autowired
	public CastleManager(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
		initDefaults();
		loadFromDb();
	}

	public int size() {
		return byId.size();
	}

	public Collection<Castle> allCastles() {
		return Collections.unmodifiableCollection(byId.values());
	}

	public Collection<Castle> all() {
		return allCastles();
	}

	public Optional<Castle> getCastleById(int castleId) {
		return Optional.ofNullable(byId.get(castleId));
	}

	public Optional<Castle> getCastleByName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
	}

	public Optional<Castle> byName(String name) {
		return getCastleByName(name);
	}

	public Optional<Castle> getCastleByTownId(int townId) {
		// Mapeamento padrao de vilas para seus respectivos castelos
		int castleId = switch (townId) {
			case 1, 2, 5, 6 -> GLUDIO;       // Talking Island, Gludin, Gludio, Neutral Zone
			case 7, 8, 9 -> DION;            // Dion, Floran, Monster Derby Track
			case 10, 11 -> GIRAN;            // Giran, Giran Harbor
			case 12, 13 -> OREN;             // Oren, Hunters Village
			case 14, 15 -> ADEN;             // Aden, Coliseum
			case 16 -> INNADRIL;             // Heine
			case 17 -> GODDARD;              // Goddard
			case 18, 19, 20 -> RUNE;         // Rune, Primeval Isle
			case 21, 22 -> SCHUTTGART;       // Schuttgart
			default -> 0;
		};
		return getCastleById(castleId);
	}

	/**
	 * Localiza o castelo responsavel pela coordenada especificada consultando as zonas.
	 */
	public Optional<Castle> getCastleAt(int x, int y, int z, ZoneTable zoneTable) {
		if (zoneTable != null) {
			var zones = zoneTable.getZonesAt(x, y, z);
			for (var zEntry : zones) {
				if (zEntry.castleId() > 0) {
					return getCastleById(zEntry.castleId());
				}
			}
		}
		return Optional.empty();
	}

	public void setTaxPercent(int castleId, int newTax) {
		Castle c = byId.get(castleId);
		if (c != null) {
			c.taxPercent(newTax);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE castle SET taxPercent = :tax WHERE id = :id")
							.param("tax", c.taxPercent())
							.param("id", castleId)
							.update();
				} catch (Exception ex) {
					log.error("Erro ao salvar imposto do castelo {}", castleId, ex);
				}
			}
		}
	}

	public void setOwner(int castleId, int clanId) {
		Castle c = byId.get(castleId);
		if (c != null) {
			c.ownerClanId(clanId);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE clan_data SET hasCastle = :cid WHERE clan_id = :clanId")
							.param("cid", castleId)
							.param("clanId", clanId)
							.update();
				} catch (Exception ignored) {}
			}
		}
	}

	public void addToTreasury(int castleId, long amount) {
		Castle c = byId.get(castleId);
		if (c != null && amount > 0) {
			c.addToTreasury(amount);
			if (jdbc != null) {
				try {
					jdbc.sql("UPDATE castle SET treasury = :tr WHERE id = :id")
							.param("tr", c.treasury())
							.param("id", castleId)
							.update();
				} catch (Exception ex) {
					log.error("Erro ao atualizar tesouro do castelo {}", castleId, ex);
				}
			}
		}
	}

	private void initDefaults() {
		DEFAULT_CASTLES.forEach((id, name) -> {
			Castle c = new Castle(id, name);
			byId.put(id, c);
			byName.put(name.toLowerCase(Locale.ROOT), c);
		});
	}

	private void loadFromDb() {
		if (jdbc == null) {
			log.info("CastleManager inicializado com os 9 castelos padrao (sem banco)");
			return;
		}

		try {
			List<Map<String, Object>> rows = jdbc.sql("""
					SELECT id, name, taxPercent, treasury, siegeDate
					FROM castle
					""").query().listOfRows();

			for (var row : rows) {
				int id = ((Number) row.get("id")).intValue();
				String name = (String) row.get("name");
				int tax = row.get("taxPercent") != null ? ((Number) row.get("taxPercent")).intValue() : 0;
				long treasury = row.get("treasury") != null ? ((Number) row.get("treasury")).longValue() : 0L;
				long siegeDate = row.get("siegeDate") != null ? ((Number) row.get("siegeDate")).longValue() : 0L;

				Castle c = byId.computeIfAbsent(id, k -> new Castle(id, name));
				c.taxPercent(tax);
				c.treasury(treasury);
				c.siegeDate(siegeDate);
				byName.put(name.toLowerCase(Locale.ROOT), c);
			}

			// Carrega os donos a partir de clan_data
			List<Map<String, Object>> clanCastles = jdbc.sql("SELECT clan_id, hasCastle FROM clan_data WHERE hasCastle > 0")
					.query().listOfRows();
			for (var row : clanCastles) {
				int clanId = ((Number) row.get("clan_id")).intValue();
				int castleId = ((Number) row.get("hasCastle")).intValue();
				Castle c = byId.get(castleId);
				if (c != null) {
					c.ownerClanId(clanId);
				}
			}

			log.info("CastleManager: {} castelos carregados e sincronizados com o banco de dados", byId.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar dados de castelos do banco: {}. Mantendo defaults.", ex.getMessage());
		}
	}
}
