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
	public void clearRegisteredClans() { registeredClanIds.clear(); }
}
