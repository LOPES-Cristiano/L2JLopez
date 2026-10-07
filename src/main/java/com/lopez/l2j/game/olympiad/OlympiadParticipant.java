package com.lopez.l2j.game.olympiad;

import com.lopez.l2j.network.game.GameSession;

/**
 * Representa um participante ativo na fila ou arena das Grandes Olimpiadas.
 * Armazena metadados de seguranca (IP anti-feed) e coordenadas de retorno.
 */
public class OlympiadParticipant {

	private final int charId;
	private final String charName;
	private final int classId;
	private final String ipAddress;
	private final GameSession session;
	private final int origX;
	private final int origY;
	private final int origZ;

	private int team; // 1 = Blue/Side A, 2 = Red/Side B
	private double damageDealt = 0;
	private boolean dead = false;

	public OlympiadParticipant(GameSession session, int origX, int origY, int origZ) {
		this.session = session;
		var active = session != null ? session.activeChar() : null;
		this.charId = active != null ? active.objectId() : 0;
		this.charName = active != null ? active.name() : "Unknown";
		this.classId = active != null ? active.classId() : 0;
		this.ipAddress = session != null && session.clientIp() != null ? session.clientIp() : "127.0.0.1";
		this.origX = origX;
		this.origY = origY;
		this.origZ = origZ;
	}

	public OlympiadParticipant(int charId, String charName, int classId, String ipAddress,
			int origX, int origY, int origZ) {
		this.session = null;
		this.charId = charId;
		this.charName = charName;
		this.classId = classId;
		this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
		this.origX = origX;
		this.origY = origY;
		this.origZ = origZ;
	}

	public int charId() {
		return charId;
	}

	public String charName() {
		return charName;
	}

	public int classId() {
		return classId;
	}

	public String ipAddress() {
		return ipAddress;
	}

	public GameSession session() {
		return session;
	}

	public int origX() {
		return origX;
	}

	public int origY() {
		return origY;
	}

	public int origZ() {
		return origZ;
	}

	public int team() {
		return team;
	}

	public void team(int team) {
		this.team = team;
	}

	public double damageDealt() {
		return damageDealt;
	}

	public void addDamage(double dmg) {
		this.damageDealt += dmg;
	}

	public boolean isDead() {
		return dead;
	}

	public void setDead(boolean dead) {
		this.dead = dead;
	}
}
