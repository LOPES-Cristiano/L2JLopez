package com.lopez.l2j.game.fortress.siege;

import com.lopez.l2j.game.castle.siege.SiegeStatus;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado e progresso da batalha de cerco a uma fortaleza (FortSiege).
 */
public class FortressSiegeRecord {

	private final int fortId;
	private SiegeStatus status;
	private long siegeDate;
	private long siegeEndDate;

	// Participantes atacantes inscritos na tabela fortsiege_clans
	private final Set<Integer> attackerClans = ConcurrentHashMap.newKeySet();

	// Mecanicas de cerco de fortaleza
	public static final int TOTAL_REACTORS = 3;
	public static final int TOTAL_COMMANDERS = 3;

	private int reactorsDisabled = 0;
	private int commandersDefeated = 0;
	private boolean flagRaised = false;
	private int victoriousClanId = 0;

	public FortressSiegeRecord(int fortId, long siegeDate) {
		this.fortId = fortId;
		this.status = SiegeStatus.REGISTRATION;
		this.siegeDate = siegeDate;
		this.siegeEndDate = siegeDate + (60 * 60 * 1000L); // 1 hora de batalha
	}

	public int fortId() { return fortId; }
	public SiegeStatus status() { return status; }
	public void status(SiegeStatus status) { this.status = status; }
	public long siegeDate() { return siegeDate; }
	public void siegeDate(long siegeDate) { this.siegeDate = siegeDate; }
	public long siegeEndDate() { return siegeEndDate; }
	public void siegeEndDate(long siegeEndDate) { this.siegeEndDate = siegeEndDate; }
	public Set<Integer> attackerClans() { return attackerClans; }
	public int reactorsDisabled() { return reactorsDisabled; }
	public void reactorsDisabled(int count) { this.reactorsDisabled = Math.min(TOTAL_REACTORS, count); }
	public int commandersDefeated() { return commandersDefeated; }
	public void commandersDefeated(int count) { this.commandersDefeated = Math.min(TOTAL_COMMANDERS, count); }
	public boolean flagRaised() { return flagRaised; }
	public void flagRaised(boolean flagRaised) { this.flagRaised = flagRaised; }
	public int victoriousClanId() { return victoriousClanId; }
	public void victoriousClanId(int victoriousClanId) { this.victoriousClanId = victoriousClanId; }

	public boolean canRaiseFlag() {
		return reactorsDisabled >= TOTAL_REACTORS && commandersDefeated >= TOTAL_COMMANDERS;
	}
}
