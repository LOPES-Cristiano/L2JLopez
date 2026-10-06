package com.lopez.l2j.game.clan;

import com.lopez.l2j.game.model.ObjectIdFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cache e armazenamento de brasoes de cla e alianca (CrestCache do L2JDream).
 * Formato padrao de cla no cliente Interlude: BMP de 16x12 pixels.
 */
@Component
public class CrestCache {

	private static final Logger log = LoggerFactory.getLogger(CrestCache.class);

	private final Path crestDir;
	private final ObjectIdFactory idFactory;
	private final Map<Integer, byte[]> pledgeCrests = new ConcurrentHashMap<>();
	private final Map<Integer, byte[]> allyCrests = new ConcurrentHashMap<>();

	@Autowired
	public CrestCache(@Value("${l2.crests.dir:data/crests}") String dir, ObjectIdFactory idFactory) {
		this(Path.of(dir), idFactory);
	}

	public CrestCache(Path crestDir, ObjectIdFactory idFactory) {
		this.crestDir = crestDir;
		this.idFactory = idFactory != null ? idFactory : ObjectIdFactory.sequential(0x60000000);
		loadAll();
	}

	public byte[] getPledgeCrest(int crestId) {
		return pledgeCrests.get(crestId);
	}

	public byte[] getAllyCrest(int crestId) {
		return allyCrests.get(crestId);
	}

	public int savePledgeCrest(byte[] data) {
		if (data == null || data.length == 0 || data.length > 2176) {
			return 0;
		}
		int crestId = idFactory.nextId();
		pledgeCrests.put(crestId, data);
		saveToFile("crest_" + crestId + ".bmp", data);
		return crestId;
	}

	public int saveAllyCrest(byte[] data) {
		if (data == null || data.length == 0 || data.length > 2176) {
			return 0;
		}
		int crestId = idFactory.nextId();
		allyCrests.put(crestId, data);
		saveToFile("ally_" + crestId + ".bmp", data);
		return crestId;
	}

	public void removePledgeCrest(int crestId) {
		pledgeCrests.remove(crestId);
		deleteFile("crest_" + crestId + ".bmp");
	}

	public void removeAllyCrest(int crestId) {
		allyCrests.remove(crestId);
		deleteFile("ally_" + crestId + ".bmp");
	}

	private void saveToFile(String filename, byte[] data) {
		try {
			Files.createDirectories(crestDir);
			Files.write(crestDir.resolve(filename), data);
		} catch (IOException e) {
			log.warn("Nao foi possivel salvar brasao em {}: {}", filename, e.getMessage());
		}
	}

	private void deleteFile(String filename) {
		try {
			Files.deleteIfExists(crestDir.resolve(filename));
		} catch (IOException ignored) {}
	}

	private void loadAll() {
		if (!Files.isDirectory(crestDir)) {
			return;
		}
		try (var stream = Files.list(crestDir)) {
			for (Path p : stream.filter(Files::isRegularFile).toList()) {
				String fname = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
				if (fname.startsWith("crest_") && fname.endsWith(".bmp")) {
					try {
						int id = Integer.parseInt(fname.substring(6, fname.length() - 4));
						pledgeCrests.put(id, Files.readAllBytes(p));
					} catch (Exception ignored) {}
				} else if (fname.startsWith("ally_") && fname.endsWith(".bmp")) {
					try {
						int id = Integer.parseInt(fname.substring(5, fname.length() - 4));
						allyCrests.put(id, Files.readAllBytes(p));
					} catch (Exception ignored) {}
				}
			}
			log.info("CrestCache: {} brasoes de cla e {} de alianca carregados de {}",
					pledgeCrests.size(), allyCrests.size(), crestDir);
		} catch (Exception ex) {
			log.warn("Falha ao ler diretorio de brasoes: {}", ex.getMessage());
		}
	}
}
