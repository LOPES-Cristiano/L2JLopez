package com.lopez.l2j.game.diary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico responsavel pelo controle do teto diario de drops de itens raros (NEXORA IL Drops Diary).
 */
@Service
public class DropsDiaryService {

	private static final Logger log = LoggerFactory.getLogger(DropsDiaryService.class);

	public record DiaryLimit(int itemId, int dailyLimit, String name) {}
	public record DailyDropRecord(LocalDate date, int count) {}

	private final Map<Integer, DiaryLimit> limits = new ConcurrentHashMap<>();
	// playerId -> (itemId -> DailyDropRecord)
	private final Map<Integer, Map<Integer, DailyDropRecord>> dropsByPlayer = new ConcurrentHashMap<>();

	public DropsDiaryService() {
		loadLimits();
	}

	public synchronized void loadLimits() {
		limits.clear();
		try {
			File file = new File("data/xml/custom/DropsDiarys.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/DropsDiarys.xml");
			if (is == null) {
				log.warn("DropsDiarys.xml nao encontrado, inicializando limites padrao.");
				registerLimit(new DiaryLimit(6392, 5, "Event Medal"));
				registerLimit(new DiaryLimit(6393, 5, "Glittering Medal"));
				registerLimit(new DiaryLimit(3470, 10, "Gold Bar"));
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList list = doc.getElementsByTagName("drop");

			for (int i = 0; i < list.getLength(); i++) {
				Element elem = (Element) list.item(i);
				int itemId = Integer.parseInt(elem.getAttribute("itemId"));
				int dailyLimit = Integer.parseInt(elem.getAttribute("dailyLimit"));
				String name = elem.hasAttribute("name") ? elem.getAttribute("name") : "Item " + itemId;

				registerLimit(new DiaryLimit(itemId, dailyLimit, name));
			}

			log.info("DropsDiaryService: {} limites diarios carregados com sucesso.", limits.size());
		} catch (Exception e) {
			log.error("Erro ao carregar DropsDiarys.xml: {}", e.getMessage(), e);
		}
	}

	public void registerLimit(DiaryLimit limit) {
		if (limit != null) {
			limits.put(limit.itemId(), limit);
		}
	}

	public boolean canDropItem(int playerId, int itemId) {
		DiaryLimit limit = limits.get(itemId);
		if (limit == null) {
			return true;
		}

		LocalDate today = LocalDate.now();
		Map<Integer, DailyDropRecord> playerMap = dropsByPlayer.get(playerId);
		if (playerMap == null) {
			return true;
		}

		DailyDropRecord record = playerMap.get(itemId);
		if (record == null || !today.equals(record.date())) {
			return true;
		}

		return record.count() < limit.dailyLimit();
	}

	public synchronized void recordDrop(int playerId, int itemId) {
		recordDrop(playerId, itemId, 1);
	}

	public synchronized void recordDrop(int playerId, int itemId, int count) {
		if (count <= 0) return;
		LocalDate today = LocalDate.now();

		dropsByPlayer.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
				.compute(itemId, (k, old) -> {
					if (old == null || !today.equals(old.date())) {
						return new DailyDropRecord(today, count);
					}
					return new DailyDropRecord(today, old.count() + count);
				});
	}

	public int getDailyCount(int playerId, int itemId) {
		LocalDate today = LocalDate.now();
		Map<Integer, DailyDropRecord> map = dropsByPlayer.get(playerId);
		if (map == null) return 0;
		DailyDropRecord record = map.get(itemId);
		if (record == null || !today.equals(record.date())) return 0;
		return record.count();
	}

	public void resetDrops(int playerId) {
		dropsByPlayer.remove(playerId);
	}
}
