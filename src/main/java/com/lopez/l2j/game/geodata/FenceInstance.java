package com.lopez.l2j.game.geodata;

/**
 * Instancia de cerca ou barreira dinamica no mundo do jogo (Onda C7 - Repatriacao NEXORA IL).
 */
public class FenceInstance implements GeoObject {

	public static final int STATE_PASSABLE = 0;
	public static final int STATE_BLOCKING = 1;
	public static final int STATE_HIDDEN = 0;
	public static final int STATE_OPEN = 1;
	public static final int STATE_CLOSED = 2;

	private final int objectId;
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
	private volatile int state;

	public FenceInstance(int objectId, String name, int x, int y, int z,
						 int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, int state) {
		this.objectId = objectId;
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
		this.state = state;
	}

	public int objectId() {
		return objectId;
	}

	public String name() {
		return name;
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
	public int xMin() {
		return xMin;
	}

	@Override
	public int xMax() {
		return xMax;
	}

	@Override
	public int yMin() {
		return yMin;
	}

	@Override
	public int yMax() {
		return yMax;
	}

	@Override
	public int zMin() {
		return zMin;
	}

	@Override
	public int zMax() {
		return zMax;
	}

	@Override
	public boolean isBlocking() {
		return state == STATE_BLOCKING || state == STATE_CLOSED;
	}

	public int state() {
		return state;
	}

	public void state(int state) {
		this.state = state;
		broadcastInfo();
	}

	public void open() {
		this.state = STATE_PASSABLE;
		broadcastInfo();
	}

	public void close() {
		this.state = STATE_BLOCKING;
		broadcastInfo();
	}

	public void broadcastInfo() {
		var gw = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (gw != null) {
			int width = Math.max(16, xMax - xMin);
			int length = Math.max(16, yMax - yMin);
			gw.broadcastAround(x, y, com.lopez.l2j.game.world.GameWorld.VISIBILITY_RADIUS,
					new com.lopez.l2j.network.game.packet.GameServerPacket.ExColosseumFenceInfo(
							objectId, state, x, y, z, width, length));
		}
	}
}
