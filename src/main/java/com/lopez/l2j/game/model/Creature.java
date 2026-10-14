package com.lopez.l2j.game.model;

/**
 * Abstração unificada para qualquer criatura viva/ativa no mundo do jogo:
 * {@link PlayerCharacter}, {@link com.lopez.l2j.game.npc.NpcInstance} e
 * {@link com.lopez.l2j.game.summon.SummonInstance}.
 *
 * Permite que o motor de combate ({@link com.lopez.l2j.game.combat.CombatService}),
 * Geodata ({@link com.lopez.l2j.game.geodata.GeoEngine}), IA e sistema modular de Skills
 * operem de forma polimórfica sobre qualquer ator.
 */
public interface Creature {

	int objectId();

	String name();

	int level();

	int x();

	void x(int value);

	int y();

	void y(int value);

	int z();

	void z(int value);

	int heading();

	void heading(int value);

	default int instanceId() {
		return 0;
	}

	default void instanceId(int value) {
	}

	double currentHp();

	void currentHp(double value);

	double currentMp();

	void currentMp(double value);

	default double currentCp() {
		return 0.0;
	}

	default void currentCp(double value) {
	}

	double maxHpValue();

	double maxMpValue();

	default double maxCpValue() {
		return 0.0;
	}

	boolean isDead();

	boolean isInCombat();

	boolean invul();

	void invul(boolean value);

	int abnormalEffect();

	void startAbnormalEffect(int mask);

	void stopAbnormalEffect(int mask);

	void disable(long until, boolean sleep);

	void root(long until);

	void mute(long until);

	void physicalMute(long until);

	boolean isDisabled();

	boolean isRooted();

	boolean isMuted();

	boolean isPhysicalMuted();

	void onDamaged();

	void moveTo(int x, int y, int z);

	default boolean isPlayer() {
		return false;
	}

	default boolean isNpc() {
		return false;
	}

	default boolean isMonster() {
		return false;
	}

	default boolean isSummon() {
		return false;
	}

	default double distanceTo(Creature other) {
		if (other == null) {
			return Double.MAX_VALUE;
		}
		return Math.hypot(other.x() - x(), other.y() - y());
	}

	default double distance3DTo(Creature other) {
		if (other == null) {
			return Double.MAX_VALUE;
		}
		double dx = other.x() - x();
		double dy = other.y() - y();
		double dz = other.z() - z();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
