package com.lopez.l2j.game.clan.clanhall.siege;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa o estado de uma siege de Clan Hall contestavel (clanhall_siege table do L2JDream).
 */
public class ClanHallSiegeRecord {

	public enum SiegeState {
		REGISTRATION,
		IN_PROGRESS,
		FINISHED
	}

	private final int hallId;
	private final String name;
	private volatile long siegeDate;
	private volatile SiegeState state;
	private final Set<Integer> registeredClanIds = ConcurrentHashMap.newKeySet();
	// Dano acumulado por cla nos Bosses de Clan Hall (Nurka 35368, Gustav 35410, Lidia 35629) (#399)
	private final java.util.Map<Integer, Long> damageByClanId = new ConcurrentHashMap<>();
	// Bandeira / NPC Aliado escolhido por cla em Bandit Stronghold (35) e Beast Farm (63) (#400)
	private final java.util.Map<Integer, Integer> clanFlagNpcIds = new ConcurrentHashMap<>();
	private final java.util.Map<Integer, Integer> clanAllyNpcIds = new ConcurrentHashMap<>();
	// Decretos de guerra e HP do Gourd em Rainbow Springs Chateau (62) (#401)
	private final java.util.Map<Integer, Integer> warDecreesByClanId = new ConcurrentHashMap<>();
	private final java.util.Map<Integer, Integer> gourdHpByClanId = new ConcurrentHashMap<>();

	public ClanHallSiegeRecord(int hallId, String name, long siegeDate, SiegeState state) {
		this.hallId = hallId;
		this.name = name != null ? name : "";
		this.siegeDate = siegeDate;
		this.state = state != null ? state : SiegeState.REGISTRATION;
	}

	public int hallId() { return hallId; }
	public String name() { return name; }
	public long siegeDate() { return siegeDate; }
	public void siegeDate(long siegeDate) { this.siegeDate = siegeDate; }
	public SiegeState state() { return state; }
	public void state(SiegeState state) { this.state = state; }

	public Set<Integer> registeredClanIds() { return Collections.unmodifiableSet(registeredClanIds); }
	public boolean registerClan(int clanId) { return registeredClanIds.add(clanId); }
	public boolean unregisterClan(int clanId) { return registeredClanIds.remove(clanId); }
	public boolean isRegistered(int clanId) { return registeredClanIds.contains(clanId); }
	public void clearRegisteredClans() {
		registeredClanIds.clear();
		damageByClanId.clear();
		clanFlagNpcIds.clear();
		clanAllyNpcIds.clear();
		warDecreesByClanId.clear();
		gourdHpByClanId.clear();
	}

	public java.util.Map<Integer, Long> damageByClanId() { return damageByClanId; }
	public void addClanDamage(int clanId, long damage) {
		if (clanId > 0 && damage > 0) {
			damageByClanId.merge(clanId, damage, Long::sum);
		}
	}

	public java.util.Map<Integer, Integer> clanFlagNpcIds() { return clanFlagNpcIds; }
	public java.util.Map<Integer, Integer> clanAllyNpcIds() { return clanAllyNpcIds; }
	public java.util.Map<Integer, Integer> warDecreesByClanId() { return warDecreesByClanId; }
	public java.util.Map<Integer, Integer> gourdHpByClanId() { return gourdHpByClanId; }
}
