package com.lopez.l2j.game.olympiad;

/**
 * Dados de um Nobre competidor nas Grandes Olimpiadas.
 */
public class OlympiadNoble {

	public static final int DEFAULT_POINTS = 18;

	private final int charId;
	private final int classId;
	private String charName;
	private int points;
	private int competitionsDone;
	private int competitionsWon;
	private int competitionsLost;
	private int competitionsDrawn;

	public OlympiadNoble(int charId, String charName, int classId, int points,
			int competitionsDone, int competitionsWon, int competitionsLost, int competitionsDrawn) {
		this.charId = charId;
		this.charName = charName != null ? charName : "";
		this.classId = classId;
		this.points = points;
		this.competitionsDone = competitionsDone;
		this.competitionsWon = competitionsWon;
		this.competitionsLost = competitionsLost;
		this.competitionsDrawn = competitionsDrawn;
	}

	public OlympiadNoble(int charId, String charName, int classId) {
		this(charId, charName, classId, DEFAULT_POINTS, 0, 0, 0, 0);
	}

	public int charId() {
		return charId;
	}

	public int classId() {
		return classId;
	}

	public String charName() {
		return charName;
	}

	public void charName(String charName) {
		this.charName = charName != null ? charName : "";
	}

	public int points() {
		return points;
	}

	public void points(int points) {
		this.points = Math.max(0, points);
	}

	public int competitionsDone() {
		return competitionsDone;
	}

	public int competitionsWon() {
		return competitionsWon;
	}

	public int competitionsLost() {
		return competitionsLost;
	}

	public int competitionsDrawn() {
		return competitionsDrawn;
	}

	public void recordWin(int pointTransfer) {
		this.competitionsDone++;
		this.competitionsWon++;
		this.points += pointTransfer;
	}

	public void recordLoss(int pointTransfer) {
		this.competitionsDone++;
		this.competitionsLost++;
		this.points = Math.max(0, this.points - pointTransfer);
	}

	public void recordDraw() {
		this.competitionsDone++;
		this.competitionsDrawn++;
	}
}
