package com.lopez.l2j.game.geodata;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor nativo de Geodata & Line of Sight (LoS) 3D do Lineage II Interlude.
 * Carrega arquivos binarios .l2j e executa raycasting tridimensional de visibilidade e colisao.
 */
@Service
public class GeoEngine {

	private static final Logger log = LoggerFactory.getLogger(GeoEngine.class);

	public static final byte BLOCK_TYPE_FLAT = 0;
	public static final byte BLOCK_TYPE_COMPLEX = 1;
	public static final byte BLOCK_TYPE_MULTILAYER = 2;

	public static final byte EAST = 1;
	public static final byte WEST = 2;
	public static final byte SOUTH = 4;
	public static final byte NORTH = 8;
	public static final byte ALL_DIRECTIONS = 15;

	public static final int ELEVATION_EYE_OFFSET = 45; // Altura dos olhos do atacante em relacao aos pes
	public static final int RAY_STEP_SIZE = 16;        // Tamanho de 1 celula de geodata (16 unidades de mundo)

	private final String geodataDir;
	private final boolean enabled;
	private final DoorTable doorTable;
	private final Map<Integer, RegionData> regions = new ConcurrentHashMap<>();
	private final java.util.Set<GeoObject> dynamicGeoObjects = ConcurrentHashMap.newKeySet();

	private static final class RegionData {
		final MappedByteBuffer buffer;
		final int[] blockOffsets;

		RegionData(MappedByteBuffer buffer, int[] blockOffsets) {
			this.buffer = buffer;
			this.blockOffsets = blockOffsets;
		}
	}

	@Autowired
	public GeoEngine(
			@Value("${l2.geodata.dir:data/geodata}") String geodataDir,
			@Value("${l2.geodata.enabled:true}") boolean enabled,
			@Autowired(required = false) DoorTable doorTable) {
		this.geodataDir = geodataDir;
		this.enabled = enabled;
		this.doorTable = doorTable;

		if (this.enabled) {
			File dir = new File(geodataDir);
			if (dir.isDirectory()) {
				File[] files = dir.listFiles((d, name) -> name.endsWith(".l2j"));
				int count = files != null ? files.length : 0;
				log.info("GeoEngine: Inicializado com {} arquivos de geodata disponiveis em {}", count, dir.getAbsolutePath());
			} else {
				log.warn("GeoEngine: Diretorio de geodata nao encontrado: {}", dir.getAbsolutePath());
			}
		} else {
			log.info("GeoEngine: Geodata desativado por configuracao.");
		}

		if (this.doorTable != null) {
			this.doorTable.allDoors().forEach(this::addGeoObject);
		}
	}

	public void addGeoObject(GeoObject object) {
		if (object != null) {
			dynamicGeoObjects.add(object);
		}
	}

	public void removeGeoObject(GeoObject object) {
		if (object != null) {
			dynamicGeoObjects.remove(object);
		}
	}

	public java.util.Set<GeoObject> dynamicGeoObjects() {
		return java.util.Collections.unmodifiableSet(dynamicGeoObjects);
	}

	public GeoEngine(String geodataDir, DoorTable doorTable) {
		this(geodataDir, true, doorTable);
	}

	public GeoEngine(DoorTable doorTable) {
		this("data/geodata", true, doorTable);
	}

	public int loadedRegionsCount() {
		return regions.size();
	}

	public boolean isEnabled() {
		return Config.ENABLE_GEODATA;
	}

	public static int getRegionX(int x) {
		return (x >> 15) + 20;
	}

	public static int getRegionY(int y) {
		return (y >> 15) + 18;
	}

	public static int getRegionKey(int regionX, int regionY) {
		return (regionX << 16) | (regionY & 0xFFFF);
	}

	/**
	 * Obtem a altura exata Z do terreno para as coordenadas mundiais (X, Y, Z).
	 */
	public short getHeight(int x, int y, int z) {
		if (!isEnabled()) {
			return (short) z;
		}

		int regX = getRegionX(x);
		int regY = getRegionY(y);
		RegionData region = getRegion(regX, regY);
		if (region == null) {
			return (short) z;
		}

		int localX = x - ((regX - 20) << 15);
		int localY = y - ((regY - 18) << 15);
		int blockX = (localX >> 7) & 0xFF;
		int blockY = (localY >> 7) & 0xFF;
		int blockIdx = (blockX << 8) | blockY;
		int offset = region.blockOffsets[blockIdx];
		if (offset < 0 || offset >= region.buffer.capacity()) {
			return (short) z;
		}

		MappedByteBuffer buf = region.buffer;
		byte type = buf.get(offset);

		if (type == BLOCK_TYPE_FLAT) {
			return buf.getShort(offset + 1);
		} else if (type == BLOCK_TYPE_COMPLEX) {
			int cellX = (localX >> 4) & 0x07;
			int cellY = (localY >> 4) & 0x07;
			int cellIdx = (cellX << 3) | cellY;
			short raw = buf.getShort(offset + 1 + (cellIdx << 1));
			return (short) ((short) (raw & 0xFFF0) >> 1);
		} else if (type == BLOCK_TYPE_MULTILAYER) {
			int cellX = (localX >> 4) & 0x07;
			int cellY = (localY >> 4) & 0x07;
			int targetCell = (cellX << 3) | cellY;

			int curOffset = offset + 1;
			for (int c = 0; c < 64; c++) {
				byte layers = buf.get(curOffset);
				curOffset++;
				if (c == targetCell) {
					short bestZ = Short.MIN_VALUE;
					for (int l = 0; l < layers; l++) {
						short raw = buf.getShort(curOffset + (l << 1));
						short layerZ = (short) ((short) (raw & 0xFFF0) >> 1);
						if (layerZ <= z + 64 && layerZ > bestZ) {
							bestZ = layerZ;
						}
					}
					return bestZ != Short.MIN_VALUE ? bestZ : (short) z;
				}
				curOffset += (layers << 1);
			}
		}

		return (short) z;
	}

	/**
	 * Verifica linha de visao (Line of Sight - LoS) 3D entre duas posicoes no mundo.
	 * Utiliza raycasting com amostragem de celulas e checagem de portas fechadas.
	 */
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz) {
		if (!isEnabled()) {
			return true;
		}

		// Checagem de portas e barreiras dinamicas (GeoObject) no trajeto
		for (GeoObject obj : dynamicGeoObjects) {
			if (obj.isBlocking() && intersectsGeoObject(x, y, z, tx, ty, tz, obj)) {
				return false;
			}
		}

		if (doorTable != null) {
			for (DoorInstance door : doorTable.allDoors()) {
				if (!door.isOpen() && intersectsDoor(x, y, z, tx, ty, tz, door)) {
					return false;
				}
			}
		}

		// Ajusta altura do alvo caso haja pequena discrepancia de spawn z com a superficie
		short geoTz = getHeight(tx, ty, tz);
		if (Math.abs(geoTz - tz) < 500) {
			tz = Math.max(tz, geoTz);
		}

		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		double dist2d = Math.sqrt(dx * dx + dy * dy);

		// Em curta/media distancia (<= 250 unidades, combate melee / proximo), visao e garantida
		// a menos que haja um desnivel vertical abrupto (> 150 unidades, ex: topo de muralha)
		if (dist2d <= 250.0) {
			return Math.abs(dz) <= 150.0;
		}

		// Raycasting em passos de RAY_STEP_SIZE para alvos a longa distancia
		int steps = Math.max(1, (int) Math.round(dist2d / RAY_STEP_SIZE));
		double stepX = dx / steps;
		double stepY = dy / steps;
		double stepZ = dz / steps;

		double curX = x;
		double curY = y;
		double curZ = z + ELEVATION_EYE_OFFSET;

		for (int i = 1; i < steps; i++) {
			curX += stepX;
			curY += stepY;
			curZ += stepZ;

			short groundZ = getHeight((int) Math.round(curX), (int) Math.round(curY), (int) Math.round(curZ));

			// O terreno so obstrui a visao se ultrapassar significativamente a linha de visao
			// dos olhos (tolerancia retail de 64 unidades para ignorar colinas suaves)
			if (groundZ > curZ + 64) {
				return false;
			}
		}

		return true;
	}

	public boolean canSeeTarget(PlayerCharacter player, NpcInstance npc) {
		if (player == null || npc == null) {
			return false;
		}
		if (!isEnabled()) {
			return true;
		}
		int pz = player.z();
		int nz = npc.z();
		short geoNz = getHeight(npc.x(), npc.y(), nz);
		if (Math.abs(geoNz - nz) < 500) {
			nz = geoNz;
		}
		short geoPz = getHeight(player.x(), player.y(), pz);
		if (Math.abs(geoPz - pz) < 500) {
			pz = geoPz;
		}
		return canSeeTarget(player.x(), player.y(), pz, npc.x(), npc.y(), nz);
	}

	public boolean canSeeTarget(NpcInstance npc, PlayerCharacter player) {
		if (npc == null || player == null) {
			return false;
		}
		if (!isEnabled()) {
			return true;
		}
		int nz = npc.z();
		int pz = player.z();
		short geoNz = getHeight(npc.x(), npc.y(), nz);
		if (Math.abs(geoNz - nz) < 500) {
			nz = geoNz;
		}
		short geoPz = getHeight(player.x(), player.y(), pz);
		if (Math.abs(geoPz - pz) < 500) {
			pz = geoPz;
		}
		return canSeeTarget(npc.x(), npc.y(), nz, player.x(), player.y(), pz);
	}

	public boolean canSeeTarget(PlayerCharacter player, PlayerCharacter target) {
		if (player == null || target == null) {
			return false;
		}
		if (!isEnabled()) {
			return true;
		}
		return canSeeTarget(player.x(), player.y(), player.z(), target.x(), target.y(), target.z());
	}

	private boolean intersectsGeoObject(int x1, int y1, int z1, int x2, int y2, int z2, GeoObject obj) {
		int dMinZ = obj.zMin() - 32;
		int dMaxZ = obj.zMax() + 32;
		int rayMinZ = Math.min(z1, z2);
		int rayMaxZ = Math.max(z1, z2);

		if (rayMaxZ < dMinZ || rayMinZ > dMaxZ) {
			return false;
		}

		return lineIntersectsBox(x1, y1, x2, y2, obj.xMin(), obj.yMin(), obj.xMax(), obj.yMax());
	}

	private boolean intersectsDoor(int x1, int y1, int z1, int x2, int y2, int z2, DoorInstance door) {
		int dMinZ = door.zMin() - 32;
		int dMaxZ = door.zMax() + 32;
		int rayMinZ = Math.min(z1, z2);
		int rayMaxZ = Math.max(z1, z2);

		if (rayMaxZ < dMinZ || rayMinZ > dMaxZ) {
			return false;
		}

		return lineIntersectsBox(x1, y1, x2, y2, door.xMin(), door.yMin(), door.xMax(), door.yMax());
	}

	private boolean lineIntersectsBox(double x1, double y1, double x2, double y2,
			double minX, double minY, double maxX, double maxY) {
		if (x1 >= minX && x1 <= maxX && y1 >= minY && y1 <= maxY) {
			return true;
		}
		if (x2 >= minX && x2 <= maxX && y2 >= minY && y2 <= maxY) {
			return true;
		}
		return linesIntersect(x1, y1, x2, y2, minX, minY, maxX, minY)
				|| linesIntersect(x1, y1, x2, y2, maxX, minY, maxX, maxY)
				|| linesIntersect(x1, y1, x2, y2, maxX, maxY, minX, maxY)
				|| linesIntersect(x1, y1, x2, y2, minX, maxY, minX, minY);
	}

	private boolean linesIntersect(double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		if (Math.abs(d) < 1e-6) {
			return false;
		}
		double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / d;
		double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / d;
		return t >= 0.0 && t <= 1.0 && u >= 0.0 && u <= 1.0;
	}

	private RegionData getRegion(int regionX, int regionY) {
		int key = getRegionKey(regionX, regionY);
		return regions.computeIfAbsent(key, k -> loadRegion(regionX, regionY));
	}

	private RegionData loadRegion(int regionX, int regionY) {
		String filename = regionX + "_" + regionY + ".l2j";
		File file = new File(geodataDir, filename);
		if (!file.isFile()) {
			return null;
		}

		try (RandomAccessFile raf = new RandomAccessFile(file, "r");
			 FileChannel channel = raf.getChannel()) {
			long size = channel.size();
			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			int[] blockOffsets = new int[65536];
			int curOffset = 0;

			for (int b = 0; b < 65536; b++) {
				if (curOffset >= size) {
					break;
				}
				blockOffsets[b] = curOffset;
				byte type = buffer.get(curOffset);

				if (type == BLOCK_TYPE_FLAT) {
					curOffset += 3;
				} else if (type == BLOCK_TYPE_COMPLEX) {
					curOffset += (1 + (64 << 1));
				} else if (type == BLOCK_TYPE_MULTILAYER) {
					curOffset += 1;
					for (int c = 0; c < 64; c++) {
						if (curOffset >= size) {
							break;
						}
						byte layers = buffer.get(curOffset);
						curOffset += (1 + (layers << 1));
					}
				} else {
					curOffset += 3; // fallback de seguranca
				}
			}

			return new RegionData(buffer, blockOffsets);
		} catch (Exception e) {
			log.warn("Erro ao carregar geodata para regiao {}_{}: {}", regionX, regionY, e.getMessage());
			return null;
		}
	}

	public record Location(int x, int y, int z) {}

	/**
	 * Verifica movimentacao do ponto (x,y,z) ate (tx,ty,tz).
	 * Se bloqueado por porta fechada ou desnivel intransponivel, retorna a ultima posicao valida.
	 */
	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz) {
		if (!isEnabled()) {
			return new Location(tx, ty, tz);
		}

		for (GeoObject obj : dynamicGeoObjects) {
			if (obj.isBlocking() && intersectsGeoObject(x, y, z, tx, ty, tz, obj)) {
				return new Location(x, y, z);
			}
		}

		if (doorTable != null) {
			for (DoorInstance door : doorTable.allDoors()) {
				if (!door.isOpen() && intersectsDoor(x, y, z, tx, ty, tz, door)) {
					return new Location(x, y, z);
				}
			}
		}

		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		double dist = Math.hypot(dx, dy);
		int steps = Math.max(1, (int) Math.ceil(dist / RAY_STEP_SIZE));
		double stepX = dx / steps;
		double stepY = dy / steps;
		double stepZ = dz / steps;

		int lastX = x;
		int lastY = y;
		int lastZ = z;

		for (int i = 1; i <= steps; i++) {
			int curX = (int) Math.round(x + i * stepX);
			int curY = (int) Math.round(y + i * stepY);
			int curZ = (int) Math.round(z + i * stepZ);

			short geoZ = getHeight(curX, curY, curZ);
			if (Math.abs(geoZ - curZ) > 64) {
				return new Location(lastX, lastY, lastZ);
			}
			lastX = curX;
			lastY = curY;
			lastZ = curZ;
		}

		return new Location(tx, ty, tz);
	}

	public boolean canMoveToTarget(int x, int y, int z, int tx, int ty, int tz) {
		if (!isEnabled()) {
			return true;
		}
		for (GeoObject obj : dynamicGeoObjects) {
			if (obj.isBlocking() && intersectsGeoObject(x, y, z, tx, ty, tz, obj)) {
				return false;
			}
		}
		if (doorTable != null) {
			for (DoorInstance door : doorTable.allDoors()) {
				if (!door.isOpen() && intersectsDoor(x, y, z, tx, ty, tz, door)) {
					return false;
				}
			}
		}
		Location loc = moveCheck(x, y, z, tx, ty, tz);
		double distToEnd = Math.hypot(loc.x() - tx, loc.y() - ty);
		return distToEnd <= 32.0;
	}

	public boolean canMoveToTarget(PlayerCharacter player, PlayerCharacter target) {
		if (player == null || target == null) return false;
		return canMoveToTarget(player.x(), player.y(), player.z(), target.x(), target.y(), target.z());
	}

	public boolean canMoveToTarget(PlayerCharacter player, NpcInstance npc) {
		if (player == null || npc == null) return false;
		return canMoveToTarget(player.x(), player.y(), player.z(), npc.x(), npc.y(), npc.z());
	}

	public boolean canMoveToTarget(NpcInstance npc, PlayerCharacter player) {
		if (npc == null || player == null) return false;
		return canMoveToTarget(npc.x(), npc.y(), npc.z(), player.x(), player.y(), player.z());
	}
}
