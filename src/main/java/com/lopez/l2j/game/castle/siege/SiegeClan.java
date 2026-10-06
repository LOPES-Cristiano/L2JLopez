package com.lopez.l2j.game.castle.siege;

/**
 * Representa a inscricao de um cla no cerco a determinado castelo.
 */
public class SiegeClan {

	public enum SiegeClanType {
		DEFENDER(0),
		ATTACKER(1),
		DEFENDER_NOT_APPROVED(2),
		OWNER(3);

		private final int id;

		SiegeClanType(int id) {
			this.id = id;
		}

		public int id() {
			return id;
		}

		public static SiegeClanType fromId(int id) {
			for (SiegeClanType type : values()) {
				if (type.id == id) {
					return type;
				}
			}
			return ATTACKER;
		}
	}

	private final int castleId;
	private final int clanId;
	private SiegeClanType type;
	private boolean isCastleOwner;

	public SiegeClan(int castleId, int clanId, SiegeClanType type, boolean isCastleOwner) {
		this.castleId = castleId;
		this.clanId = clanId;
		this.type = type;
		this.isCastleOwner = isCastleOwner;
	}

	public int castleId() {
		return castleId;
	}

	public int clanId() {
		return clanId;
	}

	public SiegeClanType type() {
		return type;
	}

	public void type(SiegeClanType type) {
		this.type = type;
	}

	public boolean isCastleOwner() {
		return isCastleOwner;
	}

	public void isCastleOwner(boolean castleOwner) {
		isCastleOwner = castleOwner;
	}
}
