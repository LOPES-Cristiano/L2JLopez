package com.lopez.l2j.game.model;

/**
 * Personagem persistido na tabela legada {@code characters}. Mutavel apenas no que muda durante o jogo
 * (posicao, HP/MP/CP, flags de sessao); o resto e imutavel por enquanto. Acesso sempre pela thread da
 * conexao dona do personagem.
 */
public final class PlayerCharacter {

	private final int objectId;
	private final String account;
	private final String name;
	private final int level;
	private final long exp;
	private final int sp;
	private final int race;
	private final int classId;
	private final int baseClassId;
	private final boolean female;
	private final int face;
	private final int hairStyle;
	private final int hairColor;
	private final int maxHp;
	private final int maxMp;
	private final int maxCp;
	private final int karma;
	private final int pvpKills;
	private final int pkKills;
	private final int clanId;
	private final String title;
	private final int accessLevel;
	private final long lastAccess;
	private long deleteTime;

	private int x;
	private int y;
	private int z;
	private int heading;
	private double currentHp;
	private double currentMp;
	private double currentCp;
	private boolean running = true;
	private boolean sitting;

	public PlayerCharacter(int objectId, String account, String name, int level, long exp, int sp, int race,
			int classId, int baseClassId, boolean female, int face, int hairStyle, int hairColor, int maxHp,
			int maxMp, int maxCp, int karma, int pvpKills, int pkKills, int clanId, String title, int accessLevel,
			long lastAccess, long deleteTime, int x, int y, int z, int heading, double currentHp,
			double currentMp, double currentCp) {
		this.objectId = objectId;
		this.account = account;
		this.name = name;
		this.level = level;
		this.exp = exp;
		this.sp = sp;
		this.race = race;
		this.classId = classId;
		this.baseClassId = baseClassId;
		this.female = female;
		this.face = face;
		this.hairStyle = hairStyle;
		this.hairColor = hairColor;
		this.maxHp = maxHp;
		this.maxMp = maxMp;
		this.maxCp = maxCp;
		this.karma = karma;
		this.pvpKills = pvpKills;
		this.pkKills = pkKills;
		this.clanId = clanId;
		this.title = title == null ? "" : title;
		this.accessLevel = accessLevel;
		this.lastAccess = lastAccess;
		this.deleteTime = deleteTime;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.currentHp = currentHp;
		this.currentMp = currentMp;
		this.currentCp = currentCp;
	}

	public int objectId() { return objectId; }
	public String account() { return account; }
	public String name() { return name; }
	public int level() { return level; }
	public long exp() { return exp; }
	public int sp() { return sp; }
	public int race() { return race; }
	public int classId() { return classId; }
	public int baseClassId() { return baseClassId; }
	public boolean female() { return female; }
	public int face() { return face; }
	public int hairStyle() { return hairStyle; }
	public int hairColor() { return hairColor; }
	public int maxHp() { return maxHp; }
	public int maxMp() { return maxMp; }
	public int maxCp() { return maxCp; }
	public int karma() { return karma; }
	public int pvpKills() { return pvpKills; }
	public int pkKills() { return pkKills; }
	public int clanId() { return clanId; }
	public String title() { return title; }
	public int accessLevel() { return accessLevel; }
	public boolean isGm() { return accessLevel > 0; }
	public long lastAccess() { return lastAccess; }
	public long deleteTime() { return deleteTime; }
	public void deleteTime(long value) { this.deleteTime = value; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
	public int heading() { return heading; }
	public double currentHp() { return currentHp; }
	public double currentMp() { return currentMp; }
	public double currentCp() { return currentCp; }
	public boolean running() { return running; }
	public void running(boolean value) { this.running = value; }
	public boolean sitting() { return sitting; }
	public void sitting(boolean value) { this.sitting = value; }

	public void moveTo(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void heading(int value) {
		this.heading = value;
	}
}
