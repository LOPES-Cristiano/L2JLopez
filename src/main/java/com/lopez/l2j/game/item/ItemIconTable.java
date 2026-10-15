package com.lopez.l2j.game.item;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Tabela e cache em memoria para market_icons (V94 / V1094).
 * Fornece mapeamento de itemId para nome do icone de interface gráfica.
 */
@Component
public class ItemIconTable {

	private static final Logger log = LoggerFactory.getLogger(ItemIconTable.class);

	private final JdbcClient jdbc;
	private final Map<Integer, String> icons = new ConcurrentHashMap<>();

	@Autowired
	public ItemIconTable(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@PostConstruct
	public void init() {
		load();
	}

	public void load() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("SELECT itemId, itemIcon FROM market_icons")
					.query((rs, rowNum) -> {
						int itemId = rs.getInt("itemId");
						String icon = rs.getString("itemIcon");
						if (icon != null && !icon.isBlank()) {
							icons.put(itemId, icon);
						}
						return null;
					})
					.list();
			log.info("ItemIconTable: {} icones carregados da tabela market_icons.", icons.size());
		} catch (Exception e) {
			log.debug("ItemIconTable: market_icons ainda nao populada ou indisponivel: {}", e.getMessage());
		}
	}

	public Optional<String> getIcon(int itemId) {
		return Optional.ofNullable(icons.get(itemId));
	}

	public String getIconOrDefault(int itemId, String defaultIcon) {
		return icons.getOrDefault(itemId, defaultIcon);
	}

	public void register(int itemId, String icon) {
		if (icon != null) {
			icons.put(itemId, icon);
		}
	}

	public int size() {
		return icons.size();
	}

	public Map<Integer, String> allIcons() {
		return Collections.unmodifiableMap(icons);
	}
}
