package com.lopez.l2j.game.instance;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa um mundo/dimensao instanciada (Reflection) isolada espacialmente por instanceId.
 */
public class InstanceWorld {

	private final int id;
	private final int templateId;
	private final long startTime;
	private final long durationMillis;
	private final Set<Integer> players = ConcurrentHashMap.newKeySet();
	private final Set<Integer> npcs = ConcurrentHashMap.newKeySet();
	private volatile int exitX;
	private volatile int exitY;
	private volatile int exitZ;

	public InstanceWorld(int id, int templateId, int durationMinutes, int exitX, int exitY, int exitZ) {
		this.id = id;
		this.templateId = templateId;
		this.startTime = System.currentTimeMillis();
		this.durationMillis = (long) durationMinutes * 60_000L;
		this.exitX = exitX;
		this.exitY = exitY;
		this.exitZ = exitZ;
	}

	public int id() { return id; }
	public int templateId() { return templateId; }
	public long startTime() { return startTime; }
	public long durationMillis() { return durationMillis; }
	public boolean isExpired() { return durationMillis > 0 && (System.currentTimeMillis() - startTime) > durationMillis; }

	public Set<Integer> players() { return Collections.unmodifiableSet(players); }
	public Set<Integer> npcs() { return Collections.unmodifiableSet(npcs); }

	public void addPlayer(int playerObjectId) { players.add(playerObjectId); }
	public void removePlayer(int playerObjectId) { players.remove(playerObjectId); }
	public void addNpc(int npcObjectId) { npcs.add(npcObjectId); }
	public void removeNpc(int npcObjectId) { npcs.remove(npcObjectId); }

	public int exitX() { return exitX; }
	public int exitY() { return exitY; }
	public int exitZ() { return exitZ; }
	public void setExitLoc(int x, int y, int z) { this.exitX = x; this.exitY = y; this.exitZ = z; }
}
