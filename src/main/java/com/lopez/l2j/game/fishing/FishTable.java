package com.lopez.l2j.game.fishing;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tabela de peixes carregada a partir de data/xml/world/fishes.xml.
 */
@Component
public class FishTable {

	private static final Logger log = LoggerFactory.getLogger(FishTable.class);

	private final List<FishData> fishes = new ArrayList<>();

	@Autowired
	public FishTable(@Value("${l2j.data.dir:data}") String dataDir) {
		load(Path.of(dataDir, "xml", "world", "fishes.xml"));
	}

	public FishTable(Path xmlPath) {
		load(xmlPath);
	}

	public FishTable() {
		// Construtor vazio para testes manuais
	}

	public void addFish(FishData fish) {
		fishes.add(fish);
	}

	private void load(Path path) {
		if (!Files.exists(path)) {
			log.warn("Arquivo fishes.xml nao encontrado em {}. Tabela de peixes vazia.", path.toAbsolutePath());
			return;
		}

		try (InputStream in = Files.newInputStream(path)) {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

			var doc = factory.newDocumentBuilder().parse(in);
			NodeList fishNodes = doc.getElementsByTagName("fish");

			for (int i = 0; i < fishNodes.getLength(); i++) {
				if (fishNodes.item(i) instanceof Element el) {
					int id = Integer.parseInt(el.getAttribute("id"));
					int level = Integer.parseInt(el.getAttribute("level"));
					String name = el.getAttribute("name");
					int hp = Integer.parseInt(el.getAttribute("hp"));
					int hpRegen = Integer.parseInt(el.getAttribute("hpregen"));
					int fishType = Integer.parseInt(el.getAttribute("fish_type"));
					int fishGroup = Integer.parseInt(el.getAttribute("fish_group"));
					int fishGuts = Integer.parseInt(el.getAttribute("fish_guts"));
					int gutsCheckTime = Integer.parseInt(el.getAttribute("guts_check_time"));
					int waitTime = Integer.parseInt(el.getAttribute("wait_time"));
					int combatTime = Integer.parseInt(el.getAttribute("combat_time"));

					fishes.add(new FishData(id, level, name, hp, hpRegen, fishType, fishGroup, fishGuts, gutsCheckTime,
							waitTime, combatTime));
				}
			}
			log.info("FishTable: Carregados {} peixes de {}", fishes.size(), path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar fishes.xml de {}: {}", path.toAbsolutePath(), e.getMessage(), e);
		}
	}

	public List<FishData> fishes() {
		return Collections.unmodifiableList(fishes);
	}

	/**
	 * Busca um peixe baseado em nivel, tipo e grupo (como no legado).
	 * Se nao houver combinacao exata, busca por nivel ou retorna o primeiro disponivel.
	 */
	public Optional<FishData> getFish(int level, int type, int group) {
		List<FishData> matched = new ArrayList<>();
		for (FishData f : fishes) {
			if (f.level() == level && f.type() == type && f.group() == group) {
				matched.add(f);
			}
		}
		if (!matched.isEmpty()) {
			return Optional.of(matched.get(ThreadLocalRandom.current().nextInt(matched.size())));
		}

		// Fallback para nivel
		for (FishData f : fishes) {
			if (f.level() == level) {
				matched.add(f);
			}
		}
		if (!matched.isEmpty()) {
			return Optional.of(matched.get(ThreadLocalRandom.current().nextInt(matched.size())));
		}

		// Fallback global
		if (!fishes.isEmpty()) {
			return Optional.of(fishes.get(ThreadLocalRandom.current().nextInt(fishes.size())));
		}

		// Fallback sintetico para ambientes sem XML
		return Optional.of(new FishData(6411, 1, "Small Green Nimble Fish", 100, 4, 1, 1, 500, 5000, 15000, 24000));
	}
}
