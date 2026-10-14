package com.lopez.l2j.game.geodata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Motor de Pathfinding A* sobre a grade 16x16 da Geodata L2J Interlude.
 * Considera alturas Z, flags direcionais NSWE e objetos dinamicos (portas e cercas).
 */
public final class PathFind {

	public static final int DEFAULT_MAX_ITERATIONS = 512;
	private static final int CELL_SIZE = GeoEngine.RAY_STEP_SIZE; // 16 unidades de mundo
	private static final int MAX_STEP_HEIGHT = 64;

	private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
	private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};

	private PathFind() {
	}

	private static final class Node implements Comparable<Node> {
		final int gx;
		final int gy;
		final int z;
		final double g;
		final double f;
		final Node parent;

		Node(int gx, int gy, int z, double g, double f, Node parent) {
			this.gx = gx;
			this.gy = gy;
			this.z = z;
			this.g = g;
			this.f = f;
			this.parent = parent;
		}

		long key() {
			return (((long) gx) << 32) | (gy & 0xFFFFFFFFL);
		}

		@Override
		public int compareTo(Node other) {
			return Double.compare(this.f, other.f);
		}
	}

	/**
	 * Executa busca A* entre a origem e o destino, retornando a lista de waypoints [x, y, z].
	 */
	public static List<int[]> findPath(GeoEngine engine, int startX, int startY, int startZ,
			int targetX, int targetY, int targetZ, int instanceId, int maxIterations) {
		if (engine == null || !engine.isEnabled()) {
			return List.of(new int[]{targetX, targetY, targetZ});
		}

		short calibratedStartZ = engine.getHeight(startX, startY, startZ);
		if (Math.abs(calibratedStartZ - startZ) < 500) {
			startZ = calibratedStartZ;
		}
		short calibratedTargetZ = engine.getHeight(targetX, targetY, targetZ);
		if (Math.abs(calibratedTargetZ - targetZ) < 500) {
			targetZ = calibratedTargetZ;
		}

		// Se o movimento em linha reta estiver completamente livre, retorna o destino diretamente
		if (engine.canMoveToTarget(startX, startY, startZ, targetX, targetY, targetZ)) {
			return List.of(new int[]{targetX, targetY, targetZ});
		}

		int startGx = startX >> 4;
		int startGy = startY >> 4;
		int targetGx = targetX >> 4;
		int targetGy = targetY >> 4;

		if (startGx == targetGx && startGy == targetGy) {
			return List.of(new int[]{targetX, targetY, targetZ});
		}

		int limit = maxIterations > 0 ? maxIterations : DEFAULT_MAX_ITERATIONS;
		PriorityQueue<Node> openSet = new PriorityQueue<>();
		Map<Long, Double> bestG = new HashMap<>();
		Set<Long> closedSet = new HashSet<>();

		Node startNode = new Node(startGx, startGy, startZ, 0.0, heuristic(startGx, startGy, targetGx, targetGy), null);
		openSet.add(startNode);
		bestG.put(startNode.key(), 0.0);

		Node reached = null;
		int iterations = 0;

		while (!openSet.isEmpty() && iterations < limit) {
			iterations++;
			Node current = openSet.poll();
			long curKey = current.key();

			if (closedSet.contains(curKey)) {
				continue;
			}
			closedSet.add(curKey);

			if (current.gx == targetGx && current.gy == targetGy) {
				reached = current;
				break;
			}

			// Se chegou na celula adjacente ao alvo e o passo final e valido
			if (Math.abs(current.gx - targetGx) <= 1 && Math.abs(current.gy - targetGy) <= 1) {
				int curWorldX = (current.gx << 4) + 8;
				int curWorldY = (current.gy << 4) + 8;
				if (canStep(engine, curWorldX, curWorldY, current.z, targetX, targetY, targetZ)) {
					reached = new Node(targetGx, targetGy, targetZ, current.g + 1.0, current.g + 1.0, current);
					break;
				}
			}

			int curWorldX = (current.gx << 4) + 8;
			int curWorldY = (current.gy << 4) + 8;

			for (int dir = 0; dir < 8; dir++) {
				int nx = current.gx + DX[dir];
				int ny = current.gy + DY[dir];
				long nKey = (((long) nx) << 32) | (ny & 0xFFFFFFFFL);
				if (closedSet.contains(nKey)) {
					continue;
				}

				int nextWorldX = (nx << 4) + 8;
				int nextWorldY = (ny << 4) + 8;
				short nextZ = engine.getHeight(nextWorldX, nextWorldY, current.z);

				if (!canStep(engine, curWorldX, curWorldY, current.z, nextWorldX, nextWorldY, nextZ)) {
					continue;
				}

				// Para movimento diagonal, evita cortar quinas de paredes/portas
				if (dir >= 4) {
					int side1X = ((current.gx + DX[dir]) << 4) + 8;
					int side1Y = (current.gy << 4) + 8;
					short side1Z = engine.getHeight(side1X, side1Y, current.z);
					int side2X = (current.gx << 4) + 8;
					int side2Y = ((current.gy + DY[dir]) << 4) + 8;
					short side2Z = engine.getHeight(side2X, side2Y, current.z);
					if (!canStep(engine, curWorldX, curWorldY, current.z, side1X, side1Y, side1Z)
							|| !canStep(engine, curWorldX, curWorldY, current.z, side2X, side2Y, side2Z)) {
						continue;
					}
				}

				double stepCost = (dir >= 4) ? 1.41421356 : 1.0;
				double tentativeG = current.g + stepCost;
				Double existingG = bestG.get(nKey);
				if (existingG == null || tentativeG < existingG) {
					bestG.put(nKey, tentativeG);
					double f = tentativeG + heuristic(nx, ny, targetGx, targetGy);
					openSet.add(new Node(nx, ny, nextZ, tentativeG, f, current));
				}
			}
		}

		if (reached == null) {
			return Collections.emptyList();
		}

		return reconstructWaypoints(reached, targetX, targetY, targetZ);
	}

	private static boolean canStep(GeoEngine engine, int x1, int y1, int z1, int x2, int y2, int z2) {
		if (Math.abs(z2 - z1) > MAX_STEP_HEIGHT) {
			return false;
		}
		if (!engine.checkNSWE(x1, y1, z1, x2, y2)) {
			return false;
		}
		return !engine.isBlockedByDynamicObjects(x1, y1, z1, x2, y2, z2);
	}

	private static double heuristic(int gx1, int gy1, int gx2, int gy2) {
		int dx = Math.abs(gx1 - gx2);
		int dy = Math.abs(gy1 - gy2);
		return (dx + dy) + (1.41421356 - 2.0) * Math.min(dx, dy);
	}

	private static List<int[]> reconstructWaypoints(Node endNode, int targetX, int targetY, int targetZ) {
		List<Node> nodes = new ArrayList<>();
		Node cur = endNode;
		while (cur != null && cur.parent != null) { // exclui o no inicial
			nodes.add(cur);
			cur = cur.parent;
		}
		Collections.reverse(nodes);

		if (nodes.isEmpty()) {
			return List.of(new int[]{targetX, targetY, targetZ});
		}

		List<int[]> waypoints = new ArrayList<>();
		int prevDx = Integer.MIN_VALUE;
		int prevDy = Integer.MIN_VALUE;

		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			boolean isLast = (i == nodes.size() - 1);
			if (isLast) {
				waypoints.add(new int[]{targetX, targetY, targetZ});
			} else {
				Node next = nodes.get(i + 1);
				int dx = Integer.compare(next.gx, n.gx);
				int dy = Integer.compare(next.gy, n.gy);
				if (dx != prevDx || dy != prevDy) {
					waypoints.add(new int[]{(n.gx << 4) + 8, (n.gy << 4) + 8, n.z});
					prevDx = dx;
					prevDy = dy;
				}
			}
		}

		return waypoints;
	}
}
