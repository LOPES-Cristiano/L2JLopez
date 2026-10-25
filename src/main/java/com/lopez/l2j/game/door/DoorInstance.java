package com.lopez.l2j.game.door;

import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.geodata.GeoObject;

/**
 * Representa uma porta no mundo (castelos, clanhalls, templos, fortes).
 * Implementa GeoObject e sincroniza com o GeoEngine para bloqueio/liberacao dinamica em tempo real.
 */
public class DoorInstance implements GeoObject {

	private final int objectId;
	private final int doorId;
	private final String name;
	private final int x;
	private final int y;
	private final int z;
	private final int xMin;
	private final int xMax;
	private final int yMin;
	private final int yMax;
	private final int zMin;
	private final int zMax;
	private final int maxHp;
	private final int pDef;
	private final int mDef;
	private final boolean unlockable;
	private volatile boolean open;
	private volatile int currentHp;
	private volatile int castleId;
	private volatile int clanHallId;
	private volatile int fortId;
	private volatile long autoActionDelayMs;
	private volatile java.util.concurrent.ScheduledFuture<?> autoActionTask;

	public DoorInstance(int objectId, int doorId, String name, int x, int y, int z,
			int xMin, int xMax, int yMin, int yMax, int zMin, int zMax,
			int maxHp, int pDef, int mDef, boolean unlockable, boolean open) {
		this.objectId = objectId;
		this.doorId = doorId;
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.zMin = zMin;
		this.zMax = zMax;
		this.maxHp = maxHp;
		this.currentHp = maxHp;
		this.pDef = pDef;
		this.mDef = mDef;
		this.unlockable = unlockable;
		this.open = open;
		syncWithGeoEngine();
	}

	private void syncWithGeoEngine() {
		GeoEngine geo = GeoEngine.peekInstance();
		if (geo != null) {
			geo.addGeoObject(this);
		}
	}

	public void broadcastStatusUpdate() {
		var gw = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (gw != null) {
			gw.broadcastAround(x, y, com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS,
					new com.lopez.l2j.network.game.packet.GameServerPacket.DoorStatusUpdate(this));
			gw.broadcastAround(x, y, com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS,
					new com.lopez.l2j.network.game.packet.GameServerPacket.StaticObject(doorId, objectId));
		}
	}

	public int objectId() { return objectId; }
	public int getObjectId() { return objectId; }
	public int doorId() { return doorId; }
	public int getDoorId() { return doorId; }
	public String name() { return name; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
	public int xMin() { return xMin; }
	public int xMax() { return xMax; }
	public int yMin() { return yMin; }
	public int yMax() { return yMax; }
	public int zMin() { return zMin; }
	public int zMax() { return zMax; }
	public int maxHp() { return maxHp; }
	public int currentHp() { return currentHp; }
	public void currentHp(int hp) {
		int nextHp = Math.max(0, Math.min(hp, maxHp));
		boolean changed = (this.currentHp != nextHp);
		this.currentHp = nextHp;
		syncWithGeoEngine();
		if (changed) {
			broadcastStatusUpdate();
		}
	}
	public void reduceHp(int dmg) {
		if (dmg > 0) {
			currentHp(this.currentHp - dmg);
		}
	}
	public int pDef() { return pDef; }
	public int mDef() { return mDef; }
	public boolean unlockable() { return unlockable; }
	public int castleId() { return castleId; }
	public void castleId(int castleId) { this.castleId = castleId; }
	public int clanHallId() { return clanHallId; }
	public void clanHallId(int clanHallId) { this.clanHallId = clanHallId; }
	public int fortId() { return fortId; }
	public void fortId(int fortId) { this.fortId = fortId; }
	public long autoActionDelayMs() { return autoActionDelayMs; }
	public void autoActionDelayMs(long autoActionDelayMs) { this.autoActionDelayMs = Math.max(0L, autoActionDelayMs); }

	public void scheduleAutoAction(java.util.concurrent.ScheduledExecutorService scheduler) {
		if (scheduler == null || autoActionDelayMs <= 0) {
			return;
		}
		var prev = this.autoActionTask;
		if (prev != null) {
			prev.cancel(false);
		}
		this.autoActionTask = scheduler.scheduleAtFixedRate(
				this::toggleDoor,
				autoActionDelayMs,
				autoActionDelayMs,
				java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	public boolean canBeOpenedBy(com.lopez.l2j.game.model.PlayerCharacter player, int ownerClanId, boolean siegeInProgress) {
		if (player == null || player.isDead() || siegeInProgress) {
			return false;
		}
		if (player.accessLevel() > 0) {
			return true;
		}
		return ownerClanId > 0 && player.clanId() == ownerClanId;
	}

	public boolean isAttackableInSiege(boolean siegeInProgress) {
		return siegeInProgress && (castleId > 0 || fortId > 0 || clanHallId > 0) && currentHp > 0;
	}

	public boolean isOpen() { return open; }
	public boolean isClosed() { return !open; }
	public void open(boolean open) {
		boolean changed = (this.open != open);
		this.open = open;
		syncWithGeoEngine();
		if (changed) {
			broadcastStatusUpdate();
		}
	}
	public void open() { openDoor(); }
	public void close() { closeDoor(); }

	public boolean openMe() {
		return openDoor();
	}

	public boolean closeMe() {
		return closeDoor();
	}

	public boolean openDoor() {
		syncWithGeoEngine();
		if (!open) {
			open = true;
			broadcastStatusUpdate();
			return true;
		}
		return false;
	}

	public boolean closeDoor() {
		syncWithGeoEngine();
		if (open) {
			open = false;
			broadcastStatusUpdate();
			return true;
		}
		return false;
	}

	public boolean toggleDoor() {
		open = !open;
		syncWithGeoEngine();
		broadcastStatusUpdate();
		return true;
	}

	@Override
	public int geoX() {
		return x >> 4;
	}

	@Override
	public int geoY() {
		return y >> 4;
	}

	@Override
	public int geoZ() {
		return z;
	}

	@Override
	public int height() {
		return zMax - zMin;
	}

	@Override
	public boolean isBlocking() {
		return !open && (currentHp > 0);
	}
}
