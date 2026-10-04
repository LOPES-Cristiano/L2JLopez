package com.lopez.l2j.game.teleport;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Tabela de localizacoes de teleporte carregada de teleports.xml.
 */
@Component
public class TeleportLocationTable {

	private static final Logger log = LoggerFactory.getLogger(TeleportLocationTable.class);

	private final Map<Integer, TeleportLocation> teleports = new HashMap<>();

	public TeleportLocationTable(
			@Value("${l2.datapack.teleports-file:data/xml/world/teleports.xml}") String filePath) {
		load(filePath);
	}

	public void load(String filePath) {
		teleports.clear();
		if (filePath == null || filePath.isBlank()) {
			return;
		}
		Path p = Path.of(filePath);
		if (Files.exists(p)) {
			try (InputStream in = Files.newInputStream(p)) {
				parseXml(in);
				log.info("Carregados {} pontos de teleporte de {}", teleports.size(), p.toAbsolutePath());
				return;
			} catch (Exception e) {
				log.warn("Falha ao carregar teleports de {}: {}", p, e.getMessage());
			}
		}

		// Fallback para classpath apenas se procurava no padrão
		if (filePath.contains("teleports.xml")) {
			try (InputStream in = getClass().getResourceAsStream("/data/xml/world/teleports.xml")) {
				if (in != null) {
					parseXml(in);
					log.info("Carregados {} pontos de teleporte do classpath", teleports.size());
					return;
				}
			} catch (Exception e) {
				log.warn("Falha ao carregar teleports do classpath: {}", e.getMessage());
			}
		}

		log.warn("Nenhum arquivo teleports.xml encontrado (procurado em: {})", filePath);
	}

	private void parseXml(InputStream in) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		Document doc = factory.newDocumentBuilder().parse(in);
		Node root = doc.getFirstChild();
		for (Node n = root; n != null; n = n.getNextSibling()) {
			if ("list".equalsIgnoreCase(n.getNodeName())) {
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
					if ("teleport".equalsIgnoreCase(d.getNodeName())) {
						NamedNodeMap attrs = d.getAttributes();
						int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						int locX = Integer.parseInt(attrs.getNamedItem("loc_x").getNodeValue());
						int locY = Integer.parseInt(attrs.getNamedItem("loc_y").getNodeValue());
						int locZ = Integer.parseInt(attrs.getNamedItem("loc_z").getNodeValue());
						int price = Integer.parseInt(attrs.getNamedItem("price").getNodeValue());
						int forNoble = Integer.parseInt(attrs.getNamedItem("fornoble").getNodeValue());
						teleports.put(id, new TeleportLocation(id, locX, locY, locZ, price, forNoble == 1));
					}
				}
			}
		}
	}

	public Optional<TeleportLocation> get(int id) {
		return Optional.ofNullable(teleports.get(id));
	}

	public int size() {
		return teleports.size();
	}

	public Map<Integer, TeleportLocation> all() {
		return Collections.unmodifiableMap(teleports);
	}
}
