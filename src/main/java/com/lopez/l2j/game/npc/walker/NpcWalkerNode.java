package com.lopez.l2j.game.npc.walker;

/**
 * Representa um ponto de rota (waypoint) para NPCs patrulheiros/andantes.
 * Porta de L2NpcWalkerNode do legado L2JDream.
 */
public record NpcWalkerNode(
		int routeId,
		int npcId,
		int movePoint,
		String chatText,
		int moveX,
		int moveY,
		int moveZ,
		int delay,
		boolean running) {

	public NpcWalkerNode(int routeId, int npcId, int movePoint, int moveX, int moveY, int moveZ) {
		this(routeId, npcId, movePoint, "", moveX, moveY, moveZ, 0, false);
	}
}
