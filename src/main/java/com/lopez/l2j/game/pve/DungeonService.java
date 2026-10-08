package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico para gerenciamento de Dungeons Instanciadas por Estagios Cronometrados - Onda C9.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Estagios escalonados com monstros e chefes de raid.</li>
 *   <li>Cobranca de entrada estritamente em Adena (zero doacoes).</li>
 *   <li>Premiações organicas em Adena e Festival Adena / Event Tokens (ID 4037).</li>
 *   <li>Cooldowns por jogador e instanciamento isolado.</li>
 * </ul>
 */
@Service
public class DungeonService {

	private static final Logger log = LoggerFactory.getLogger(DungeonService.class);

	public record DungeonDrop(int itemId, int count, int chance) {}
	public record DungeonSpawn(int npcId, String title, int count, int x, int y, int z, List<DungeonDrop> drops) {}
	public record DungeonStage(int order, int x, int y, int z, boolean teleport, int minutes, List<DungeonSpawn> spawns) {}
	public record DungeonTemplate(
			int id,
			String name,
			String type,
			int entryFeeItemId,
			long entryFeeCount,
			int cooldownMinutes,
			List<DungeonStage> stages
	) {}

	public static class DungeonSession {
		private final int instanceId;
		private final DungeonTemplate template;
		private final PlayerCharacter leader;
		private final long startTime;
		private int currentStageIndex;
		private int remainingMonstersInStage;
		private boolean completed;
		private boolean failed;

		public DungeonSession(int instanceId, DungeonTemplate template, PlayerCharacter leader) {
			this.instanceId = instanceId;
			this.template = template;
			this.leader = leader;
			this.startTime = System.currentTimeMillis();
			this.currentStageIndex = 0;
			initStage();
		}

		private void initStage() {
			if (currentStageIndex < template.stages().size()) {
				DungeonStage stage = template.stages().get(currentStageIndex);
				int total = 0;
				for (DungeonSpawn spawn : stage.spawns()) {
					total += spawn.count();
				}
				this.remainingMonstersInStage = Math.max(1, total);
			}
		}

		public int instanceId() { return instanceId; }
		public DungeonTemplate template() { return template; }
		public PlayerCharacter leader() { return leader; }
		public int currentStageIndex() { return currentStageIndex; }
		public int currentStageNumber() { return currentStageIndex + 1; }
		public int remainingMonstersInStage() { return remainingMonstersInStage; }
		public boolean isCompleted() { return completed; }
		public boolean isFailed() { return failed; }
	}

	public record DungeonResult(boolean success, String message, DungeonSession session) {
		public static DungeonResult ok(String msg, DungeonSession s) { return new DungeonResult(true, msg, s); }
		public static DungeonResult fail(String msg) { return new DungeonResult(false, msg, null); }
	}

	private final Map<Integer, DungeonTemplate> dungeons = new ConcurrentHashMap<>();
	private final Map<Integer, DungeonSession> activeSessions = new ConcurrentHashMap<>();
	private final Map<Integer, Long> playerCooldowns = new ConcurrentHashMap<>();
	private final AtomicInteger instanceCounter = new AtomicInteger(1000);

	public DungeonService() {
		loadDungeons();
	}

	public void loadDungeons() {
		dungeons.clear();
		try {
			File file = new File("data/xml/custom/dungeon_event.xml");
			InputStream is;
			if (file.exists()) {
				is = new java.io.FileInputStream(file);
			} else {
				is = getClass().getResourceAsStream("/data/xml/custom/dungeon_event.xml");
			}

			if (is == null) {
				log.warn("Arquivo dungeon_event.xml nao encontrado.");
				return;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			NodeList dungeonNodes = doc.getElementsByTagName("dungeon");

			for (int i = 0; i < dungeonNodes.getLength(); i++) {
				Element dElem = (Element) dungeonNodes.item(i);
				int id = Integer.parseInt(dElem.getAttribute("id"));
				String name = dElem.getAttribute("name");
				String type = dElem.getAttribute("type");

				int feeItem = 57;
				long feeCount = 0;
				NodeList feeNodes = dElem.getElementsByTagName("entryFee");
				if (feeNodes.getLength() > 0) {
					Element feeElem = (Element) feeNodes.item(0);
					feeItem = Integer.parseInt(feeElem.getAttribute("itemId"));
					feeCount = Long.parseLong(feeElem.getAttribute("count"));
				}

				int cooldown = 30;
				NodeList cdNodes = dElem.getElementsByTagName("cooldown");
				if (cdNodes.getLength() > 0) {
					Element cdElem = (Element) cdNodes.item(0);
					if (cdElem.hasAttribute("minutes")) {
						cooldown = Integer.parseInt(cdElem.getAttribute("minutes"));
					} else if (cdElem.hasAttribute("hours")) {
						cooldown = Integer.parseInt(cdElem.getAttribute("hours")) * 60;
					}
				}

				List<DungeonStage> stages = new ArrayList<>();
				NodeList stageNodes = dElem.getElementsByTagName("stage");
				for (int s = 0; s < stageNodes.getLength(); s++) {
					Element sElem = (Element) stageNodes.item(s);
					int order = Integer.parseInt(sElem.getAttribute("order"));
					int minutes = sElem.hasAttribute("minutes") ? Integer.parseInt(sElem.getAttribute("minutes")) : 10;
					boolean teleport = Boolean.parseBoolean(sElem.getAttribute("teleport"));

					String[] loc = sElem.getAttribute("loc").split(",");
					int x = Integer.parseInt(loc[0].trim());
					int y = Integer.parseInt(loc[1].trim());
					int z = Integer.parseInt(loc[2].trim());

					List<DungeonSpawn> spawns = new ArrayList<>();
					NodeList spawnNodes = sElem.getElementsByTagName("spawn");
					for (int p = 0; p < spawnNodes.getLength(); p++) {
						Element spElem = (Element) spawnNodes.item(p);
						int npcId = Integer.parseInt(spElem.getAttribute("npcId"));
						String title = spElem.hasAttribute("title") ? spElem.getAttribute("title") : "";
						int count = spElem.hasAttribute("count") ? Integer.parseInt(spElem.getAttribute("count")) : 1;

						int sx = x, sy = y, sz = z;
						if (spElem.hasAttribute("loc")) {
							String[] sloc = spElem.getAttribute("loc").split(",");
							sx = Integer.parseInt(sloc[0].trim());
							sy = Integer.parseInt(sloc[1].trim());
							sz = Integer.parseInt(sloc[2].trim());
						}

						List<DungeonDrop> drops = new ArrayList<>();
						if (spElem.hasAttribute("drops")) {
							String[] dEntries = spElem.getAttribute("drops").split(";");
							for (String entry : dEntries) {
								if (entry.isBlank()) continue;
								String[] parts = entry.split("-");
								if (parts.length >= 3) {
									drops.add(new DungeonDrop(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
								}
							}
						}
						spawns.add(new DungeonSpawn(npcId, title, count, sx, sy, sz, drops));
					}
					stages.add(new DungeonStage(order, x, y, z, teleport, minutes, spawns));
				}

				dungeons.put(id, new DungeonTemplate(id, name, type, feeItem, feeCount, cooldown, stages));
			}
			log.info("DungeonService: {} dungeons instanciadas carregadas com sucesso.", dungeons.size());
		} catch (Exception e) {
			log.error("Erro ao carregar dungeon_event.xml: {}", e.getMessage(), e);
		}
	}

	/**
	 * Tenta ingressar o jogador em uma Dungeon instanciada.
	 */
	public DungeonResult enterDungeon(PlayerCharacter player, int dungeonId) {
		if (player == null || player.isDead()) {
			return DungeonResult.fail("Jogador invalido ou morto.");
		}

		DungeonTemplate template = dungeons.get(dungeonId);
		if (template == null) {
			return DungeonResult.fail("Dungeon nao encontrada: " + dungeonId);
		}

		// Validacao de cooldown
		Long cdEnd = playerCooldowns.get(player.objectId());
		if (cdEnd != null && System.currentTimeMillis() < cdEnd) {
			long remMin = (cdEnd - System.currentTimeMillis()) / (1000 * 60);
			return DungeonResult.fail("Dungeon em cooldown. Aguarde " + remMin + " minuto(s).");
		}

		// Cobranca de entrada
		if (template.entryFeeCount() > 0) {
			if (player.inventory() != null) {
				var opt = player.inventory().byItemId(template.entryFeeItemId());
				if (opt.isEmpty() || opt.get().count() < template.entryFeeCount()) {
					return DungeonResult.fail("Adena insuficiente para entrada na Dungeon. Necessario: " + template.entryFeeCount());
				}
				opt.get().count((int) (opt.get().count() - template.entryFeeCount()));
			}
		}

		int instanceId = instanceCounter.incrementAndGet();
		player.instanceId(instanceId);

		// Teleporta para o primeiro estagio
		if (!template.stages().isEmpty()) {
			DungeonStage stage1 = template.stages().get(0);
			player.x(stage1.x());
			player.y(stage1.y());
			player.z(stage1.z());
		}

		DungeonSession session = new DungeonSession(instanceId, template, player);
		activeSessions.put(instanceId, session);

		// Aplica cooldown
		playerCooldowns.put(player.objectId(), System.currentTimeMillis() + (template.cooldownMinutes() * 60L * 1000L));

		return DungeonResult.ok("Dungeon '" + template.name() + "' iniciada com sucesso!", session);
	}

	/**
	 * Chamado quando um monstro da dungeon e abatido.
	 * Se todos os monstros do estagio atual forem derrotados, avanca de estagio ou finaliza com sucesso.
	 */
	public boolean onMonsterKilled(int instanceId) {
		DungeonSession session = activeSessions.get(instanceId);
		if (session == null || session.isCompleted() || session.isFailed()) {
			return false;
		}

		session.remainingMonstersInStage--;

		if (session.remainingMonstersInStage <= 0) {
			session.currentStageIndex++;
			if (session.currentStageIndex >= session.template.stages().size()) {
				// Conclusão total da Dungeon!
				session.completed = true;
				deliverCompletionRewards(session);
				return true;
			} else {
				// Avanca para o proximo estagio
				session.initStage();
				DungeonStage nextStage = session.template.stages().get(session.currentStageIndex);
				if (nextStage.teleport()) {
					session.leader.x(nextStage.x());
					session.leader.y(nextStage.y());
					session.leader.z(nextStage.z());
				}
				return true;
			}
		}

		return false;
	}

	private void deliverCompletionRewards(DungeonSession session) {
		PlayerCharacter leader = session.leader();
		if (leader != null && leader.inventory() != null) {
			// Premiação da conclusão: 500,000 Adena e 5 Event Tokens (4037)
			var adenaOpt = leader.inventory().byItemId(57);
			adenaOpt.ifPresent(adena -> adena.count(adena.count() + 500000));

			var tokenOpt = leader.inventory().byItemId(4037);
			if (tokenOpt.isPresent()) {
				tokenOpt.get().count(tokenOpt.get().count() + 5);
			}
		}
	}

	public DungeonTemplate getDungeon(int id) {
		return dungeons.get(id);
	}

	public DungeonSession getSession(int instanceId) {
		return activeSessions.get(instanceId);
	}

	public Map<Integer, DungeonTemplate> allDungeons() {
		return Collections.unmodifiableMap(dungeons);
	}
}
