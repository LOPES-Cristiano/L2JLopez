package com.lopez.l2j.game.model;

import java.util.List;
import java.util.Optional;

/** Persistencia de personagens (tabela legada {@code characters}). */
public interface CharacterRepository {

	/** Dados de criacao validados pelo servico. */
	record NewCharacter(String account, String name, int race, int classId, boolean female, int face,
			int hairStyle, int hairColor, int maxHp, int maxMp, int maxCp, int x, int y, int z, boolean canCraft) {
	}

	/** Personagens da conta em ordem estavel (a posicao na lista e o "slot" que o cliente envia). */
	List<PlayerCharacter> findByAccount(String account);

	boolean nameExists(String name);

	PlayerCharacter create(NewCharacter c);

	void delete(int objectId);

	void updateDeleteTime(int objectId, long deleteTime);

	/** Grava posicao, HP/MP/CP e marca online/offline com lastAccess = agora. */
	void saveState(PlayerCharacter c, boolean online);

	void setAccessLevel(String charName, int accessLevel);

	List<PlayerCharacter> listAll(int limit);

	List<PlayerCharacter> searchByName(String query, int limit);

	Optional<PlayerCharacter> findByName(String name);

	List<PlayerCharacter> findTopPvP(int limit);

	List<PlayerCharacter> findTopPK(int limit);
}
