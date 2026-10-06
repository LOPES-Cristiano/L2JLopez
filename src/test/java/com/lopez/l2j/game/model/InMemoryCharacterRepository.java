package com.lopez.l2j.game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio em memoria para testes de sessao/servico sem banco. */
public class InMemoryCharacterRepository implements CharacterRepository {

	public final Map<Integer, PlayerCharacter> rows = new LinkedHashMap<>();
	public final Map<Integer, Boolean> online = new LinkedHashMap<>();
	private int nextId = JdbcCharacterRepository.FIRST_OBJECT_ID;

	@Override
	public synchronized List<PlayerCharacter> findByAccount(String account) {
		List<PlayerCharacter> list = new ArrayList<>();
		for (PlayerCharacter c : rows.values()) {
			if (c.account().equals(account)) {
				list.add(c);
			}
		}
		list.sort(Comparator.comparingInt(PlayerCharacter::objectId));
		return list;
	}

	@Override
	public synchronized boolean nameExists(String name) {
		return rows.values().stream().anyMatch(c -> c.name().equalsIgnoreCase(name));
	}

	@Override
	public synchronized PlayerCharacter create(NewCharacter c) {
		var pc = new PlayerCharacter(nextId++, c.account(), c.name(), 1, 0, 0, c.race(), c.classId(), c.classId(),
				c.female(), c.face(), c.hairStyle(), c.hairColor(), c.maxHp(), c.maxMp(), c.maxCp(), 0, 0, 0, 0, "",
				0, System.currentTimeMillis(), 0, c.x(), c.y(), c.z(), 0, c.maxHp(), c.maxMp(), c.maxCp());
		rows.put(pc.objectId(), pc);
		return pc;
	}

	@Override
	public synchronized void delete(int objectId) {
		rows.remove(objectId);
	}

	@Override
	public synchronized void updateDeleteTime(int objectId, long deleteTime) {
		PlayerCharacter c = rows.get(objectId);
		if (c != null) {
			c.deleteTime(deleteTime);
		}
	}

	@Override
	public synchronized void saveState(PlayerCharacter c, boolean isOnline) {
		rows.put(c.objectId(), c);
		online.put(c.objectId(), isOnline);
	}

	@Override
	public synchronized void setAccessLevel(String charName, int accessLevel) {
		for (PlayerCharacter c : rows.values()) {
			if (c.name().equalsIgnoreCase(charName)) {
				c.accessLevel(accessLevel);
				break;
			}
		}
	}

	@Override
	public synchronized List<PlayerCharacter> listAll(int limit) {
		return rows.values().stream().limit(limit).toList();
	}

	@Override
	public synchronized List<PlayerCharacter> searchByName(String query, int limit) {
		String q = query.toLowerCase(java.util.Locale.ROOT);
		return rows.values().stream()
				.filter(c -> c.name().toLowerCase(java.util.Locale.ROOT).contains(q))
				.limit(limit)
				.toList();
	}

	@Override
	public synchronized Optional<PlayerCharacter> findByName(String name) {
		return rows.values().stream()
				.filter(c -> c.name().equalsIgnoreCase(name))
				.findFirst();
	}

	@Override
	public synchronized List<PlayerCharacter> findTopPvP(int limit) {
		return rows.values().stream()
				.filter(c -> c.pvpKills() > 0)
				.sorted((c1, c2) -> Integer.compare(c2.pvpKills(), c1.pvpKills()))
				.limit(limit)
				.toList();
	}

	@Override
	public synchronized List<PlayerCharacter> findTopPK(int limit) {
		return rows.values().stream()
				.filter(c -> c.pkKills() > 0)
				.sorted((c1, c2) -> Integer.compare(c2.pkKills(), c1.pkKills()))
				.limit(limit)
				.toList();
	}
}
