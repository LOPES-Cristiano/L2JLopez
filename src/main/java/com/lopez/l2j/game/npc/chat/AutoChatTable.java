package com.lopez.l2j.game.npc.chat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Tabela de chats automaticos para NPCs carregada de data/xml/world/auto_chat.xml.
 * Porta de AutoChatHandler XML loader de L2JDream.
 */
@Component
public class AutoChatTable {

	private static final Logger log = LoggerFactory.getLogger(AutoChatTable.class);

	private final Map<Integer, AutoChatData> byNpcId = new HashMap<>();
	private final Map<Integer, AutoChatData> byGroupId = new HashMap<>();

	@Autowired
	public AutoChatTable(@Value("${l2j.data.dir:data}") String dataDir) {
		load(Path.of(dataDir, "xml", "world", "auto_chat.xml"));
	}

	public AutoChatTable(Path xmlPath) {
		load(xmlPath);
	}

	public AutoChatTable() {
		// Construtor vazio para testes
	}

	public void load() {
		load(Path.of("data", "xml", "world", "auto_chat.xml"));
	}

	public void load(Path path) {
		byNpcId.clear();
		byGroupId.clear();

		if (!Files.exists(path)) {
			log.warn("Arquivo auto_chat.xml nao encontrado em {}. Tabela vazia.", path.toAbsolutePath());
			return;
		}

		try (InputStream in = Files.newInputStream(path)) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(in);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
				if ("list".equalsIgnoreCase(n.getNodeName())) {
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
						if ("autochat".equalsIgnoreCase(d.getNodeName())) {
							NamedNodeMap attrs = d.getAttributes();
							int groupId = Integer.parseInt(attrs.getNamedItem("groupId").getNodeValue());
							int npcId = Integer.parseInt(attrs.getNamedItem("npcId").getNodeValue());
							long chatDelay = Long.parseLong(attrs.getNamedItem("chatDelay").getNodeValue());

							List<String> texts = new ArrayList<>();
							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
								if ("chatText".equalsIgnoreCase(c.getNodeName())) {
									String txt = c.getTextContent();
									if (txt != null && !txt.trim().isEmpty()) {
										texts.add(txt.trim());
									}
								}
							}

							AutoChatData data = new AutoChatData(groupId, npcId, chatDelay, texts);
							add(data);
						}
					}
				}
			}
			log.info("AutoChatTable: {} configuracoes de auto-chat carregadas de {}", byNpcId.size(), path.toAbsolutePath());
		} catch (Exception e) {
			log.error("Erro ao carregar auto_chat.xml de {}", path, e);
		}
	}

	public void add(AutoChatData data) {
		if (data == null) {
			return;
		}
		byNpcId.put(data.npcId(), data);
		byGroupId.put(data.groupId(), data);
	}

	public Optional<AutoChatData> getByNpcId(int npcId) {
		return Optional.ofNullable(byNpcId.get(npcId));
	}

	public Optional<AutoChatData> getByGroupId(int groupId) {
		return Optional.ofNullable(byGroupId.get(groupId));
	}

	public Map<Integer, AutoChatData> all() {
		return Collections.unmodifiableMap(byNpcId);
	}

	public int size() {
		return byNpcId.size();
	}
}
