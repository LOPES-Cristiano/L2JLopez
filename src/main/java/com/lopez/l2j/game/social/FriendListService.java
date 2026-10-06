package com.lopez.l2j.game.social;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket.FriendItem;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Servico de lista de amigos e bloqueio de contatos (Ignore List / Block List) (Item 28 do Roteiro Mestre).
 * Gerencia tabelas `character_friends` e `character_blocks`.
 */
@Service
public class FriendListService {

	private static final Logger log = LoggerFactory.getLogger(FriendListService.class);

	private final JdbcClient jdbc;

	public FriendListService(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Carrega a lista de amigos do jogador do banco de dados.
	 */
	public List<FriendItem> loadFriends(int charId, java.util.function.Function<Integer, Boolean> onlineCheck) {
		try {
			return jdbc.sql("SELECT friendId, friend_name FROM character_friends WHERE charId = :id ORDER BY friend_name ASC")
					.param("id", charId)
					.query((rs, rowNum) -> {
						int fId = rs.getInt("friendId");
						String name = rs.getString("friend_name");
						boolean online = onlineCheck != null && Boolean.TRUE.equals(onlineCheck.apply(fId));
						return new FriendItem(charId, name, online, fId);
					})
					.list();
		} catch (Exception e) {
			log.warn("Erro ao carregar lista de amigos para charId {}: {}", charId, e.getMessage());
			return List.of();
		}
	}

	/**
	 * Adiciona relacao bidirecional de amizade entre dois jogadores.
	 */
	public void addFriendship(int charId1, String name1, int charId2, String name2) {
		try {
			jdbc.sql("""
					INSERT INTO character_friends (charId, friendId, friend_name)
					VALUES (:c1, :f1, :n1), (:c2, :f2, :n2)
					""")
					.param("c1", charId1).param("f1", charId2).param("n1", name2)
					.param("c2", charId2).param("f2", charId1).param("n2", name1)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao salvar amizade entre {} ({}) e {} ({}): {}", name1, charId1, name2, charId2, e.getMessage());
		}
	}

	/**
	 * Remove relacao bidirecional de amizade.
	 */
	public void removeFriendship(int charId1, int charId2) {
		try {
			jdbc.sql("""
					DELETE FROM character_friends
					WHERE (charId = :c1 AND friendId = :c2) OR (charId = :c2 AND friendId = :c1)
					""")
					.param("c1", charId1)
					.param("c2", charId2)
					.update();
		} catch (Exception e) {
			log.warn("Erro ao remover amizade entre {} e {}: {}", charId1, charId2, e.getMessage());
		}
	}

	/**
	 * Carrega a lista de bloqueios (Ignore List) do jogador na memoria.
	 */
	public void loadBlocks(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		try {
			var names = jdbc.sql("SELECT name FROM character_blocks WHERE charId = :id")
					.param("id", player.objectId())
					.query(String.class)
					.list();
			for (String name : names) {
				if (name != null && !name.isBlank()) {
					player.blockList().add(name.toLowerCase(Locale.ROOT));
				}
			}
		} catch (Exception e) {
			log.warn("Erro ao carregar blocks para {}: {}", player.name(), e.getMessage());
		}
	}

	/**
	 * Adiciona um personagem na lista de ignorados.
	 */
	public boolean addBlock(PlayerCharacter actor, String targetName) {
		if (actor == null || targetName == null || targetName.isBlank()) {
			return false;
		}
		String norm = targetName.trim().toLowerCase(Locale.ROOT);
		if (norm.equalsIgnoreCase(actor.name())) {
			return false;
		}
		if (actor.blockList().add(norm)) {
			try {
				jdbc.sql("REPLACE INTO character_blocks (charId, name) VALUES (:id, :name)")
						.param("id", actor.objectId())
						.param("name", targetName.trim())
						.update();
			} catch (Exception e) {
				log.warn("Erro ao salvar block de {} para {}: {}", actor.name(), targetName, e.getMessage());
			}
			return true;
		}
		return false;
	}

	/**
	 * Remove um personagem da lista de ignorados.
	 */
	public boolean removeBlock(PlayerCharacter actor, String targetName) {
		if (actor == null || targetName == null || targetName.isBlank()) {
			return false;
		}
		String norm = targetName.trim().toLowerCase(Locale.ROOT);
		if (actor.blockList().remove(norm)) {
			try {
				jdbc.sql("DELETE FROM character_blocks WHERE charId = :id AND LOWER(name) = :name")
						.param("id", actor.objectId())
						.param("name", norm)
						.update();
			} catch (Exception e) {
				log.warn("Erro ao deletar block de {} para {}: {}", actor.name(), targetName, e.getMessage());
			}
			return true;
		}
		return false;
	}

	/**
	 * Verifica se o destinatario bloqueou o remetente.
	 * GMs e administradores nunca sao bloqueados.
	 */
	public boolean isBlocked(PlayerCharacter receiver, PlayerCharacter sender) {
		if (receiver == null || sender == null) {
			return false;
		}
		if (sender.isGm()) {
			return false;
		}
		if (receiver.isBlockingAll()) {
			return true;
		}
		return receiver.isBlocked(sender.name());
	}
}
