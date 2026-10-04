package com.lopez.l2j.game.npc;

/**
 * Instancia ativa de NPC no mundo do jogo (Monstro, NPC de cidade, Guard...).
 */
public final class NpcInstance {

	private final int objectId;
	private final NpcTemplate template;
	private int x;
	private int y;
	private int z;
	private int heading;
	private double currentHp;
	private double currentMp;
	private boolean dead;
	private boolean inCombat;
	private boolean running;
	private volatile int targetPlayerId;
	private volatile long lastAttackTime;

	private final int spawnX;
	private final int spawnY;
	private final int spawnZ;
	private final int spawnHeading;

	public NpcInstance(int objectId, NpcTemplate template, int x, int y, int z, int heading) {
		this.objectId = objectId;
		this.template = template;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.spawnX = x;
		this.spawnY = y;
		this.spawnZ = z;
		this.spawnHeading = heading;
		this.currentHp = template.maxHp();
		this.currentMp = template.maxMp();
	}

	public int objectId() { return objectId; }
	public NpcTemplate template() { return template; }
	public int npcId() { return template.id(); }
	public String name() { return template.name(); }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
	public int heading() { return heading; }
	public int spawnX() { return spawnX; }
	public int spawnY() { return spawnY; }
	public int spawnZ() { return spawnZ; }
	public int spawnHeading() { return spawnHeading; }
	public double currentHp() { return currentHp; }
	public void currentHp(double value) { this.currentHp = value; }
	public double currentMp() { return currentMp; }
	public void currentMp(double value) { this.currentMp = value; }
	public boolean isDead() { return dead; }
	public void dead(boolean value) { this.dead = value; }
	public boolean isInCombat() { return inCombat; }
	public void inCombat(boolean value) { this.inCombat = value; }
	public boolean isRunning() { return running; }
	public void running(boolean value) { this.running = value; }
	public boolean isAttackable() { return template.isAttackable(); }
	public boolean isMonster() { return template.isMonster(); }
	public int targetPlayerId() { return targetPlayerId; }
	public void targetPlayerId(int value) { this.targetPlayerId = value; }
	public long lastAttackTime() { return lastAttackTime; }
	public void lastAttackTime(long value) { this.lastAttackTime = value; }

	public void moveTo(int x, int y, int z, int heading) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	// ---- controle de debuffs de skills (Stun/Sleep/Paralyze/Root) ----

	private volatile long disabledUntil;
	private volatile boolean sleeping;
	private volatile long rootedUntil;

	/** Stun/Paralyze/Sleep: nao anda nem ataca ate {@code until}. Sleep quebra ao tomar dano. */
	public void disable(long until, boolean sleep) {
		this.disabledUntil = Math.max(disabledUntil, until);
		this.sleeping = sleep;
	}

	public void root(long until) {
		this.rootedUntil = Math.max(rootedUntil, until);
	}

	public boolean isDisabled() {
		return System.currentTimeMillis() < disabledUntil;
	}

	public boolean isRooted() {
		return System.currentTimeMillis() < rootedUntil;
	}

	/** Chamado quando o monstro toma dano: acorda do Sleep. */
	public void onDamaged() {
		if (sleeping) {
			sleeping = false;
			disabledUntil = 0;
		}
	}

	@Override
	public String toString() {
		return template.name() + " (" + template.id() + ") #" + objectId + " [" + x + "," + y + "," + z + "]";
	}
}
