package com.lopez.l2j.events;

/**
 * Evento de dominio publicado quando um jogador mata outro.
 * PvP Rank, Quake, Achievements, War Legend etc. escutam via @EventListener,
 * sem que o Player conheca nenhum deles.
 */
public record PlayerKilledEvent(int killerId, int victimId, boolean pvp) {
}
