package com.lopez.l2j.game.stats;

import com.lopez.l2j.game.model.PlayerCharacter;

/**
 * Contexto de calculo (Env) contendo o personagem, alvo e valores correntes.
 */
public class Env {

	private PlayerCharacter character;
	private Object target;
	private Object skill;
	public double value;
	public double baseValue;

	public Env() {
	}

	public Env(PlayerCharacter character, double baseValue) {
		this.character = character;
		this.baseValue = baseValue;
		this.value = baseValue;
	}

	public PlayerCharacter getCharacter() {
		return character;
	}

	public void setCharacter(PlayerCharacter character) {
		this.character = character;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Object getSkill() {
		return skill;
	}

	public void setSkill(Object skill) {
		this.skill = skill;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getBaseValue() {
		return baseValue;
	}

	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}
}
