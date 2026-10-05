package com.lopez.l2j.game.model;

import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.skill.StatFunc;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Personagem persistido na tabela legada {@code characters}. Mutavel apenas no que muda durante o jogo
 * (posicao, HP/MP/CP, flags de sessao); o resto e imutavel por enquanto. Acesso sempre pela thread da
 * conexao dona do personagem.
 */
public final class PlayerCharacter {

	private final int objectId;
	private final String account;
	private String name;
	private int level;
	private long exp;
	private int sp;
	private final int race;
	private int classId;
	private final int baseClassId;
	private final boolean female;
	private int face;
	private int hairStyle;
	private int hairColor;
	private int maxHp;
	private int maxMp;
	private int maxCp;
	private int karma;
	private int pvpKills;
	private int pkKills;
	private final int clanId;
	private String title;
	private int accessLevel;
	private final long lastAccess;
	private long deleteTime;
	private int mountType;
	private int gmSpeed;
	private boolean invul;
	private boolean invis;
	private boolean silence;
	private boolean diet;
	private int polyNpcId;

	private int x;
	private int y;
	private int z;
	private int heading;
	private double currentHp;
	private double currentMp;
	private double currentCp;
	private boolean running = true;
	private boolean sitting;
	private Inventory inventory;

	private final Map<Integer, Integer> skills = new ConcurrentHashMap<>();
	private final PlayerEffects effects = new PlayerEffects();
	private List<StatFunc> passiveFuncs = List.of();
	private int abnormalEffect;

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
	public void classId(int value) { this.classId = value; }
	public int baseClassId() { return baseClassId; }
	public boolean female() { return female; }
	public int face() { return face; }
	public void face(int value) { this.face = value; }
	public int hairStyle() { return hairStyle; }
	public void hairStyle(int value) { this.hairStyle = value; }
	public int hairColor() { return hairColor; }
	public void hairColor(int value) { this.hairColor = value; }
	public int maxHp() { return maxHp; }
	public void maxHp(int value) { this.maxHp = value; }
	public int maxMp() { return maxMp; }
	public void maxMp(int value) { this.maxMp = value; }
	public int maxCp() { return maxCp; }
	public void maxCp(int value) { this.maxCp = value; }
	public int karma() { return karma; }
	public int pvpKills() { return pvpKills; }
	public int pkKills() { return pkKills; }
	public int clanId() { return clanId; }
	public String title() { return title; }
	public int accessLevel() { return accessLevel; }
	public void accessLevel(int value) { this.accessLevel = value; }
	public boolean isGm() { return accessLevel > 0; }
	public long lastAccess() { return lastAccess; }
	public long deleteTime() { return deleteTime; }
	public void deleteTime(long value) { this.deleteTime = value; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }
	public int heading() { return heading; }
	public double currentHp() { return currentHp; }
	public void currentHp(double value) { this.currentHp = Math.max(0, Math.min(maxHp, value)); }
	public double currentMp() { return currentMp; }
	public void currentMp(double value) { this.currentMp = Math.max(0, Math.min(maxMp, value)); }
	public double currentCp() { return currentCp; }
	public void currentCp(double value) { this.currentCp = Math.max(0, Math.min(maxCp, value)); }
	public void exp(long value) { this.exp = value; }
	public void sp(int value) { this.sp = value; }
	public void level(int value) { this.level = value; }
	public boolean isDead() { return currentHp <= 0; }
	public boolean running() { return running; }
	public void running(boolean value) { this.running = value; }
	public boolean sitting() { return sitting; }
	public void sitting(boolean value) { this.sitting = value; }

	public Map<Integer, Integer> skills() { return skills; }
	public int skillLevel(int skillId) { return skills.getOrDefault(skillId, 0); }
	public PlayerEffects effects() { return effects; }
	public List<StatFunc> passiveFuncs() { return passiveFuncs; }
	public void passiveFuncs(List<StatFunc> funcs) { this.passiveFuncs = funcs == null ? List.of() : List.copyOf(funcs); }

	public int abnormalEffect() { return abnormalEffect; }
	public void startAbnormalEffect(int mask) { this.abnormalEffect |= mask; }
	public void stopAbnormalEffect(int mask) { this.abnormalEffect &= ~mask; }

	private volatile long disabledUntil;
	private volatile boolean sleeping;
	private volatile long rootedUntil;

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

	public void onDamaged() {
		if (sleeping) {
			sleeping = false;
			disabledUntil = 0;
		}
	}

	/** Inventario carregado ao entrar no jogo (vazio enquanto o personagem esta so na lista). */
	public Inventory inventory() {
		if (inventory == null) {
			inventory = new Inventory(objectId);
		}
		return inventory;
	}

	public void inventory(Inventory value) { this.inventory = value; }

	public void moveTo(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void heading(int value) {
		this.heading = value;
	}

	public void name(String value) { this.name = value; }
	public void classId(int value) { this.classId = value; }
	public void title(String value) { this.title = value; }
	public void karma(int value) { this.karma = value; }
	public void pvpKills(int value) { this.pvpKills = value; }
	public void pkKills(int value) { this.pkKills = value; }
	public int mountType() { return mountType; }
	public void mountType(int value) { this.mountType = value; }
	public int gmSpeed() { return gmSpeed; }
	public void gmSpeed(int value) { this.gmSpeed = value; }
	public boolean invul() { return invul; }
	public void invul(boolean value) { this.invul = value; }
	public boolean invis() { return invis; }
	public void invis(boolean value) { this.invis = value; }
	public boolean silence() { return silence; }
	public void silence(boolean value) { this.silence = value; }
	public boolean diet() { return diet; }
	public void diet(boolean value) { this.diet = value; }
	public int polyNpcId() { return polyNpcId; }
	public void polyNpcId(int value) { this.polyNpcId = value; }
	public void isDisabled(boolean val) { this.disabledUntil = val ? Long.MAX_VALUE : 0; }
}
