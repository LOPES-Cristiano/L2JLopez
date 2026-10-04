package com.lopez.l2j.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Substitui o antigo Config.java (191 KB) por records imutaveis e validados.
 * Cada dominio ganha seu proprio record conforme os modulos forem migrados.
 */
@ConfigurationProperties(prefix = "l2")
public record ServerProperties(
		@NotBlank String serverName,
		@Valid @NotNull Rates rates,
		@Valid @NotNull Network network,
		@Valid @NotNull Features features) {

	public record Rates(
			@DecimalMin("0.0") double xp,
			@DecimalMin("0.0") double sp,
			@DecimalMin("0.0") double adena,
			@DecimalMin("0.0") double drop,
			@DecimalMin("0.0") double spoil) {
	}

	public record Network(
			@Min(1) int loginPort,
			@Min(1) int gamePort,
			@Min(1) int loginInternalPort,
			@Min(1) int protocolMin,
			@Min(1) int protocolMax) {
	}

	public record Features(
			@Valid Toggle autofarm,
			@Valid Toggle dressme,
			@Valid Toggle pvpRank,
			@Valid Toggle reset,
			@Valid Toggle roulette,
			@Valid Toggle aio,
			@Valid Toggle vip,
			@Valid Toggle achievements,
			@Valid Toggle vote,
			@Valid Toggle quake) {
	}

	public record Toggle(boolean enabled) {
	}
}
