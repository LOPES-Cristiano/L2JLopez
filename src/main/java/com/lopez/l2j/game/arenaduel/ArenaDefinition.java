package com.lopez.l2j.game.arenaduel;

public record ArenaDefinition(
		int id,
		String name,
		ArenaLoc spawn1,
		ArenaLoc spawn2,
		ArenaLoc spectatorLoc
) {}
