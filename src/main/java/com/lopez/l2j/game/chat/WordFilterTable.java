package com.lopez.l2j.game.chat;

import com.lopez.l2j.config.Config;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Filtro de palavras proibidas e censura de chat (SayFilter do L2JDream).
 * Lê as expressões de config/game/admin/sayfilter.txt e substitui termos impróprios.
 */
@Component
public class WordFilterTable {

	private static final Logger log = LoggerFactory.getLogger(WordFilterTable.class);
	private static final String DEFAULT_FILTER_FILE = "config/game/admin/sayfilter.txt";

	private final List<Pattern> patterns = new CopyOnWriteArrayList<>();
	private final String filePath;

	public WordFilterTable() {
		this(DEFAULT_FILTER_FILE);
	}

	public WordFilterTable(String filePath) {
		this.filePath = filePath;
	}

	@PostConstruct
	public void init() {
		load();
	}

	public synchronized void load() {
		patterns.clear();
		File f = new File(filePath);
		if (!f.exists()) {
			log.info("Chat Filter: arquivo {} nao encontrado. Filtro inativo.", filePath);
			return;
		}

		int count = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				try {
					patterns.add(Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE));
					count++;
				} catch (Exception ex) {
					log.warn("Chat Filter: padrao regex invalido '{}': {}", trimmed, ex.getMessage());
				}
			}
			log.info("Chat Filter: {} palavras/padroes carregados de {}", count, filePath);
		} catch (Exception e) {
			log.warn("Chat Filter: erro ao ler {}: {}", filePath, e.getMessage());
		}
	}

	public int size() {
		return patterns.size();
	}

	public void addPattern(String regex) {
		if (regex != null && !regex.isBlank()) {
			patterns.add(Pattern.compile(regex.trim(), Pattern.CASE_INSENSITIVE));
		}
	}

	public void clear() {
		patterns.clear();
	}

	/**
	 * Filtra o texto informado substituindo ocorrencias proibidas pelos caracteres configurados.
	 */
	public String filter(String text) {
		if (text == null || text.isEmpty() || patterns.isEmpty()) {
			return text;
		}

		String replacement = Config.CHAT_FILTER_CHARS != null ? Config.CHAT_FILTER_CHARS : "...";
		String result = text;
		for (Pattern p : patterns) {
			result = p.matcher(result).replaceAll(replacement);
		}
		return result;
	}
}
