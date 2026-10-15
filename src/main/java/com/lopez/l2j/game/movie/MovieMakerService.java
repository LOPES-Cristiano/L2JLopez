package com.lopez.l2j.game.movie;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.SpecialCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Sequencias Cinematograficas (Movie Maker) - Onda F6 / Source L2JRadical.
 * Suporta emissao do pacote SpecialCamera (0xc7) para sequencias e trailers in-game.
 */
@Service
public class MovieMakerService {

	private static final Logger log = LoggerFactory.getLogger(MovieMakerService.class);

	public record CameraScene(
			int targetObjectId,
			int distance,
			int yaw,
			int pitch,
			int time,
			int duration,
			int turn,
			int rise,
			int widescreen,
			int unk
	) {
		public SpecialCamera toPacket() {
			return new SpecialCamera(targetObjectId, distance, yaw, pitch, time, duration, turn, rise, widescreen, unk);
		}
	}

	public record MovieSequence(String name, List<CameraScene> scenes) {}

	private final Map<String, MovieSequence> sequences = new ConcurrentHashMap<>();
	private final GameWorld gameWorld;

	public MovieMakerService() {
		this(null);
	}

	@Autowired
	public MovieMakerService(@Autowired(required = false) GameWorld gameWorld) {
		this.gameWorld = gameWorld != null ? gameWorld : GameWorld.getInstance();
		registerDefaultSequences();
	}

	private void registerDefaultSequences() {
		List<CameraScene> antharasIntro = List.of(
				new CameraScene(0, 1500, 180, -15, 0, 3000, 0, 0, 1, 0),
				new CameraScene(0, 800, 90, 0, 3000, 5000, 0, 0, 1, 0)
		);
		registerSequence("antharas_intro", antharasIntro);

		List<CameraScene> valakasIntro = List.of(
				new CameraScene(0, 2000, 0, -25, 0, 4000, 0, 0, 1, 0),
				new CameraScene(0, 1000, 120, -5, 4000, 6000, 0, 0, 1, 0)
		);
		registerSequence("valakas_intro", valakasIntro);
	}

	public void registerSequence(String name, List<CameraScene> scenes) {
		sequences.put(name.toLowerCase(), new MovieSequence(name.toLowerCase(), new ArrayList<>(scenes)));
	}

	public MovieSequence getSequence(String name) {
		return sequences.get(name.toLowerCase());
	}

	public Map<String, MovieSequence> getSequences() {
		return Collections.unmodifiableMap(sequences);
	}

	public void playScene(PlayerCharacter player, CameraScene scene) {
		if (player == null || scene == null) return;
		SpecialCamera packet = scene.toPacket();
		GameWorld world = (gameWorld != null) ? gameWorld : GameWorld.getInstance();
		if (world != null) {
			world.sendPacket(player.objectId(), packet);
		}
	}

	public void playSequence(PlayerCharacter player, String sequenceName) {
		MovieSequence seq = getSequence(sequenceName);
		if (seq != null && !seq.scenes().isEmpty()) {
			playScene(player, seq.scenes().get(0));
			log.info("MovieMaker: Iniciando sequencia cinematografica '{}' para jogador {}", sequenceName, player.name());
		}
	}
}
