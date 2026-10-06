package com.lopez.l2j.game.npc.walker;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Tabela de rotas de movimentacao de NPCs andantes/guardas carregada de
 * data/xml/world/walkers_routes.xml.
 * Porta de NpcWalkerRoutesTable do legado L2JDream.
 */
@Component
public class NpcWalkerRoutesTable {

	private static final Logger log = LoggerFactory.getLogger(NpcWalkerRoutesTable.class);

	private final Map<Integer, List<NpcWalkerNode>> routesByRouteId = new HashMap<>();
	private final Map<Integer, List<NpcWalkerNode>> routesByNpcId = new HashMap<>();
	private int totalNodes = 0;

	@Autowired
	public NpcWalkerRoutesTable(@Value("${l2j.data.dir:data}") String dataDir) {
		load(Path.of(dataDir, "xml", "world", "walkers_routes.xml"));
	}

	public NpcWalkerRoutesTable(Path xmlPath) {
		load(xmlPath);
	}

	public NpcWalkerRoutesTable() {
		// Construtor vazio para testes
	}

	public void load() {
		load(Path.of("data", "xml", "world", "walkers_routes.xml"));
	}

	public void load(Path path) {
		routesByRouteId.clear();
		routesByNpcId.clear();
		totalNodes = 0;

		if (!Files.exists(path)) {
			log.warn("Arquivo walkers_routes.xml nao encontrado em {}. Tabela vazia.", path.toAbsolutePath());
			return;
		}

		try (InputStream in = Files.newInputStream(path)) {
			parseXml(in);
			// Ordena os waypoints de cada rota por movePoint ascendente
			routesByRouteId.values().forEach(list -> list.sort(Comparator.comparingInt(NpcWalkerNode::movePoint)));
			routesByNpcId.values().forEach(list -> list.sort(Comparator.comparingInt(NpcWalkerNode::movePoint)));
			log.info("NpcWalkerRoutesTable: {} rotas e {} waypoints carregados de {}",
					routesByRouteId.size(), totalNodes, path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar walkers_routes.xml de {}", path, e);
		}
	}

	public void add(NpcWalkerNode node) {
		if (node == null) {
			return;
		}
		routesByRouteId.computeIfAbsent(node.routeId(), k -> new ArrayList<>()).add(node);
		routesByNpcId.computeIfAbsent(node.npcId(), k -> new ArrayList<>()).add(node);
		totalNodes++;
	}

	private void parseXml(InputStream in) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		Document doc = factory.newDocumentBuilder().parse(in);

		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
			if ("list".equalsIgnoreCase(n.getNodeName())) {
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
					if ("walker_route".equalsIgnoreCase(d.getNodeName())) {
						NamedNodeMap attrs = d.getAttributes();
						int routeId = Integer.parseInt(attrs.getNamedItem("route_id").getNodeValue());
						int npcId = Integer.parseInt(attrs.getNamedItem("npc_id").getNodeValue());
						int movePoint = Integer.parseInt(attrs.getNamedItem("move_point").getNodeValue());
						String chatText = attrs.getNamedItem("chatText") != null
								? attrs.getNamedItem("chatText").getNodeValue() : "";
						int moveX = Integer.parseInt(attrs.getNamedItem("move_x").getNodeValue());
						int moveY = Integer.parseInt(attrs.getNamedItem("move_y").getNodeValue());
						int moveZ = Integer.parseInt(attrs.getNamedItem("move_z").getNodeValue());
						int delay = attrs.getNamedItem("delay") != null
								? Integer.parseInt(attrs.getNamedItem("delay").getNodeValue()) : 0;
						boolean running = attrs.getNamedItem("running") != null
								&& Boolean.parseBoolean(attrs.getNamedItem("running").getNodeValue());

						NpcWalkerNode node = new NpcWalkerNode(routeId, npcId, movePoint, chatText, moveX, moveY, moveZ, delay, running);
						add(node);
					}
				}
			}
		}
	}

	public List<NpcWalkerNode> getRoute(int routeId) {
		return routesByRouteId.getOrDefault(routeId, Collections.emptyList());
	}

	public List<NpcWalkerNode> getRouteForNpc(int npcId) {
		return routesByNpcId.getOrDefault(npcId, Collections.emptyList());
	}

	public Map<Integer, List<NpcWalkerNode>> getAllRoutes() {
		return Collections.unmodifiableMap(routesByRouteId);
	}

	public int totalRoutes() {
		return routesByRouteId.size();
	}

	public int totalNodes() {
		return totalNodes;
	}
}
