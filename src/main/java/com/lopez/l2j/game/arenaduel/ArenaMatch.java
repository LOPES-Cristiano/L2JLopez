package com.lopez.l2j.game.arenaduel;

import com.lopez.l2j.game.model.PlayerCharacter;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaMatch {

	private final int matchId;
	private final ArenaDefinition arena;
	private final PlayerCharacter player1;
	private final PlayerCharacter player2;
	private final ArenaLoc originalLoc1;
	private final ArenaLoc originalLoc2;
	private final Instant startTime;
	private ArenaDuelState state;
	private int winnerId;
	private final Set<Integer> spectators = ConcurrentHashMap.newKeySet();

	public ArenaMatch(int matchId, ArenaDefinition arena, PlayerCharacter player1, PlayerCharacter player2,
					  ArenaLoc originalLoc1, ArenaLoc originalLoc2) {
		this.matchId = matchId;
		this.arena = arena;
		this.player1 = player1;
		this.player2 = player2;
		this.originalLoc1 = originalLoc1;
		this.originalLoc2 = originalLoc2;
		this.startTime = Instant.now();
		this.state = ArenaDuelState.COUNTDOWN;
		this.winnerId = 0;
	}

	public int getMatchId() {
		return matchId;
	}

	public ArenaDefinition getArena() {
		return arena;
	}

	public PlayerCharacter getPlayer1() {
		return player1;
	}

	public PlayerCharacter getPlayer2() {
		return player2;
	}

	public ArenaLoc getOriginalLoc1() {
		return originalLoc1;
	}

	public ArenaLoc getOriginalLoc2() {
		return originalLoc2;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public ArenaDuelState getState() {
		return state;
	}

	public void setState(ArenaDuelState state) {
		this.state = state;
	}

	public int getWinnerId() {
		return winnerId;
	}

	public void setWinnerId(int winnerId) {
		this.winnerId = winnerId;
	}

	public void addSpectator(int playerId) {
		spectators.add(playerId);
	}

	public void removeSpectator(int playerId) {
		spectators.remove(playerId);
	}

	public Set<Integer> getSpectators() {
		return Collections.unmodifiableSet(spectators);
	}

	public boolean containsPlayer(int playerId) {
		return player1.objectId() == playerId || player2.objectId() == playerId;
	}

	public PlayerCharacter getOpponent(int playerId) {
		if (player1.objectId() == playerId) return player2;
		if (player2.objectId() == playerId) return player1;
		return null;
	}
}
