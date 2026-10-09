package com.lopez.l2j.game.henna;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Persistencia das tatuagens / simbolos (hennas) equipados pelos personagens na tabela {@code character_hennas}.
 * Suporta isolamento por charId, slot (1..3) e class_index (suporte nativo e independente para subclasses).
 */
@Repository
public class CharacterHennaRepository {

	private static final Logger log = LoggerFactory.getLogger(CharacterHennaRepository.class);

	private final JdbcClient jdbc;

	@Autowired
	public CharacterHennaRepository(@Autowired(required = false) JdbcClient jdbc) {
		this.jdbc = jdbc;
		com.lopez.l2j.network.game.GameSession.Context.setGlobalCharacterHennaRepository(this);
	}

	/**
	 * Carrega os simbolos equipados por slot (1..3) para o charId e classIndex fornecidos.
	 */
	public Map<Integer, Integer> restoreHennas(int charId, int classIndex) {
		if (jdbc == null) {
			return Map.of();
		}
		try {
			Map<Integer, Integer> map = new HashMap<>();
			jdbc.sql("SELECT slot, symbol_id FROM character_hennas WHERE charId = :charId AND class_index = :classIndex")
					.param("charId", charId)
					.param("classIndex", classIndex)
					.query((rs, rowNum) -> {
						int slot = rs.getInt("slot");
						int symbolId = rs.getInt("symbol_id");
						if (slot >= 1 && slot <= 3 && symbolId > 0) {
							map.put(slot, symbolId);
						}
						return null;
					})
					.list();
			return map;
		} catch (Exception e) {
			log.error("Erro ao carregar hennas do charId {} (classIndex {}): {}", charId, classIndex, e.getMessage(), e);
			return Map.of();
		}
	}

	/**
	 * Salva ou atualiza uma henna em um slot especifico para o personagem e subclass.
	 */
	public void saveHenna(int charId, int classIndex, int slot, int symbolId) {
		if (jdbc == null) {
			return;
		}
		try {
			deleteHenna(charId, classIndex, slot);

			jdbc.sql("INSERT INTO character_hennas (charId, symbol_id, slot, class_index) VALUES (:charId, :symbolId, :slot, :classIndex)")
					.param("charId", charId)
					.param("symbolId", symbolId)
					.param("slot", slot)
					.param("classIndex", classIndex)
					.update();
			log.debug("Henna {} salva no slot {} para charId {} (classIndex {})", symbolId, slot, charId, classIndex);
		} catch (Exception e) {
			log.error("Erro ao salvar henna no slot {} para charId {}: {}", slot, charId, e.getMessage(), e);
		}
	}

	/**
	 * Remove a henna de um slot especifico para o personagem e subclass.
	 */
	public void deleteHenna(int charId, int classIndex, int slot) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM character_hennas WHERE charId = :charId AND slot = :slot AND class_index = :classIndex")
					.param("charId", charId)
					.param("slot", slot)
					.param("classIndex", classIndex)
					.update();
			log.debug("Henna removida do slot {} para charId {} (classIndex {})", slot, charId, classIndex);
		} catch (Exception e) {
			log.error("Erro ao deletar henna do slot {} para charId {}: {}", slot, charId, e.getMessage(), e);
		}
	}

	/**
	 * Remove todas as hennas de uma subclass ou de todas as classes do personagem ao deletar.
	 */
	public void deleteAllHennas(int charId, int classIndex) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM character_hennas WHERE charId = :charId AND class_index = :classIndex")
					.param("charId", charId)
					.param("classIndex", classIndex)
					.update();
		} catch (Exception e) {
			log.error("Erro ao deletar hennas para charId {} (classIndex {}): {}", charId, classIndex, e.getMessage(), e);
		}
	}

	public void deleteAllHennas(int charId) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("DELETE FROM character_hennas WHERE charId = :charId")
					.param("charId", charId)
					.update();
		} catch (Exception e) {
			log.error("Erro ao deletar todas hennas para charId {}: {}", charId, e.getMessage(), e);
		}
	}
}
