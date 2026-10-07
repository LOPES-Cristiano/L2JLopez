package com.lopez.l2j.game.olympiad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa uma partida em andamento nas Grandes Olimpiadas.
 */
public class OlympiadMatch {

	public enum MatchState {
		PORTING,
		PREPARATION,
		FIGHTING,
		TERMINATED
	}

	public enum MatchResult {
		TEAM_A_WIN,
		TEAM_B_WIN,
		DRAW
	}

	private final int matchId;
	private final int stadiumId;
	private final OlympiadMode mode;
	private final List<OlympiadParticipant> teamA = new ArrayList<>();
	private final List<OlympiadParticipant> teamB = new ArrayList<>();

	private MatchState state = MatchState.PREPARATION;
	private MatchResult result;
	private long startTime;
	private int transferredPoints = 0;

	public OlympiadMatch(int matchId, int stadiumId, OlympiadMode mode,
			List<OlympiadParticipant> sideA, List<OlympiadParticipant> sideB) {
		this.matchId = matchId;
		this.stadiumId = stadiumId;
		this.mode = mode;
		if (sideA != null) {
			sideA.forEach(p -> p.team(1));
			this.teamA.addAll(sideA);
		}
		if (sideB != null) {
			sideB.forEach(p -> p.team(2));
			this.teamB.addAll(sideB);
		}
		this.startTime = System.currentTimeMillis();
	}

	public int matchId() {
		return matchId;
	}

	public int stadiumId() {
		return stadiumId;
	}

	public OlympiadMode mode() {
		return mode;
	}

	public List<OlympiadParticipant> teamA() {
		return Collections.unmodifiableList(teamA);
	}

	public List<OlympiadParticipant> teamB() {
		return Collections.unmodifiableList(teamB);
	}

	public MatchState state() {
		return state;
	}

	public void state(MatchState state) {
		this.state = state;
	}

	public MatchResult result() {
		return result;
	}

	public int transferredPoints() {
		return transferredPoints;
	}

	public void transferredPoints(int pts) {
		this.transferredPoints = pts;
	}

	public double totalDamageTeamA() {
		return teamA.stream().mapToDouble(OlympiadParticipant::damageDealt).sum();
	}

	public double totalDamageTeamB() {
		return teamB.stream().mapToDouble(OlympiadParticipant::damageDealt).sum();
	}

	public boolean isTeamADefeated() {
		return teamA.stream().allMatch(OlympiadParticipant::isDead);
	}

	public boolean isTeamBDefeated() {
		return teamB.stream().allMatch(OlympiadParticipant::isDead);
	}

	/**
	 * Conclui a partida determinando vencedor por morte ou dano causado.
	 */
	public MatchResult evaluateOutcome() {
		if (isTeamADefeated() && !isTeamBDefeated()) {
			result = MatchResult.TEAM_B_WIN;
		} else if (isTeamBDefeated() && !isTeamADefeated()) {
			result = MatchResult.TEAM_A_WIN;
		} else {
			double dmgA = totalDamageTeamA();
			double dmgB = totalDamageTeamB();
			if (dmgA > dmgB) {
				result = MatchResult.TEAM_A_WIN;
			} else if (dmgB > dmgA) {
				result = MatchResult.TEAM_B_WIN;
			} else {
				result = MatchResult.DRAW;
			}
		}
		this.state = MatchState.TERMINATED;
		return result;
	}
}
