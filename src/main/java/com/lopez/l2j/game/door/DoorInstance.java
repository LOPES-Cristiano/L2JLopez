package com.lopez.l2j.game.door;

import com.lopez.l2j.game.geodata.GeoObject;

/**
 * Representa uma porta no mundo (castelos, clanhalls, templos, fortes).
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
	}

	public int objectId() { return objectId; }
	public int doorId() { return doorId; }
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
	public void currentHp(int hp) { this.currentHp = Math.max(0, Math.min(hp, maxHp)); }
	public int pDef() { return pDef; }
	public int mDef() { return mDef; }
	public boolean unlockable() { return unlockable; }
	public boolean isOpen() { return open; }
	public void open(boolean open) { this.open = open; }
	public void open() { openDoor(); }
	public void close() { closeDoor(); }

	public boolean openDoor() {
		if (!open) {
			open = true;
			return true;
		}
		return false;
	}

	public boolean closeDoor() {
		if (open) {
			open = false;
			return true;
		}
		return false;
	}

	public boolean toggleDoor() {
		open = !open;
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
