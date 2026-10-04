package com.lopez.l2j.game.teleport;

/**
 * Ponto de teleporte carregado do datapack teleports.xml.
 */
public record TeleportLocation(
		int id,
		int locX,
		int locY,
		int locZ,
		int price,
		boolean forNoble
) {}
