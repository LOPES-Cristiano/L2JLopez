package com.lopez.l2j.game.castle.siege;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Instancia de estado de um cerco para um castelo especifico.
 */
public class SiegeRecord {

	private final int castleId;
	private SiegeStatus status;
	private long siegeDate;
	private long siegeEndDate;
	private boolean registrationOver;

	// Participantes
	private final Map<Integer, SiegeClan> attackers = new ConcurrentHashMap<>();
	private final Map<Integer, SiegeClan> defenders = new ConcurrentHashMap<>();
	private final Map<Integer, SiegeClan> waitingDefenders = new ConcurrentHashMap<>();

	// Torres de controle ativas
	private int controlTowersActive = 3;
	private int flameTowersActive = 2;

	public SiegeRecord(int castleId, long siegeDate) {
		this.castleId = castleId;
		this.status = SiegeStatus.REGISTRATION;
		this.siegeDate = siegeDate;
		this.siegeEndDate = siegeDate + (2 * 60 * 60 * 1000L); // 2 horas padrao
		this.registrationOver = false;
	}

	public int castleId() {
		return castleId;
	}

	public SiegeStatus status() {
		return status;
	}

	public void status(SiegeStatus status) {
		this.status = status;
	}

	public long siegeDate() {
		return siegeDate;
	}

	public void siegeDate(long siegeDate) {
		this.siegeDate = siegeDate;
		this.siegeEndDate = siegeDate + (2 * 60 * 60 * 1000L);
	}

	public long siegeEndDate() {
		return siegeEndDate;
	}

	public void siegeEndDate(long siegeEndDate) {
		this.siegeEndDate = siegeEndDate;
	}

	public boolean isRegistrationOver() {
		return registrationOver;
	}

	public void registrationOver(boolean registrationOver) {
		this.registrationOver = registrationOver;
	}

	public Map<Integer, SiegeClan> attackers() {
		return attackers;
	}

	public Map<Integer, SiegeClan> defenders() {
		return defenders;
	}

	public Map<Integer, SiegeClan> waitingDefenders() {
		return waitingDefenders;
	}

	public int controlTowersActive() {
		return controlTowersActive;
	}

	public void controlTowersActive(int controlTowersActive) {
		this.controlTowersActive = Math.max(0, controlTowersActive);
	}

	public int flameTowersActive() {
		return flameTowersActive;
	}

	public void flameTowersActive(int flameTowersActive) {
		this.flameTowersActive = Math.max(0, flameTowersActive);
	}
}
