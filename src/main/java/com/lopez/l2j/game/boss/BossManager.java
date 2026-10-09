package com.lopez.l2j.game.boss;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.CoreAndOrfenService;
import com.lopez.l2j.game.boss.epic.QueenAntService;
import com.lopez.l2j.game.boss.epic.SailrenService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.boss.epic.VanHalterService;
import com.lopez.l2j.game.boss.epic.ZakenService;
import com.lopez.l2j.game.instance.foursepulchers.FourSepulchersService;
import com.lopez.l2j.game.instance.frintezza.FrintezzaService;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplateTable;
import com.lopez.l2j.game.party.Party;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico central de gerenciamento e integracao de todos os Bosses (Raid Bosses e Grand Bosses)
 * com 100% de paridade com L2JDream e L2JLucera2.
 *
 * Gerencia ciclo de vida, mortes, notificacoes, interacoes de dialogo (HTML) com NPCs de entrada
 * e cubos de saida, e aplicacao da Maldicao de Raid (Raid Curse).
 */
@Service
public class BossManager {

	private static final Logger log = LoggerFactory.getLogger(BossManager.class);
	private static volatile BossManager instance;

	// NPC IDs dos portais / estatuas / guardioes de entrada
	public static final int NPC_HEART_OF_WARDING = 13001; // Antharas
	public static final int NPC_ANTHARAS_CUBE = 31859;
	public static final int NPC_KLEIN = 31540;            // Valakas
	public static final int NPC_VALAKAS_CUBE = 31759;
	public static final int NPC_ANGELIC_VORTEX = 31862;   // Baium
	public static final int NPC_BAIUM_STATUE = 29025;
	public static final int NPC_BAIUM_CUBE = 29055;
	public static final int NPC_SHILEN_STATUE = 32109;    // Sailren
	public static final int NPC_SAILREN_CUBE = 32110;

	private final RaidBossSpawnManager raidBossSpawnManager;
	private final GrandBossManager grandBossManager;
	private final AntharasService antharasService;
	private final ValakasService valakasService;
	private final BaiumService baiumService;
	private final SailrenService sailrenService;
	private final QueenAntService queenAntService;
	private final ZakenService zakenService;
	private final CoreAndOrfenService coreAndOrfenService;
	private final FrintezzaService frintezzaService;
	private final VanHalterService vanHalterService;
	private final FourSepulchersService fourSepulchersService;
	private final GameWorld world;
	private final NpcTemplateTable templates;
	private final ObjectIdFactory objectIds;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "SubclassChest-Scheduler");
		t.setDaemon(true);
		return t;
	});

	@Autowired
	public BossManager(
			@Autowired(required = false) RaidBossSpawnManager raidBossSpawnManager,
			@Autowired(required = false) GrandBossManager grandBossManager,
			@Autowired(required = false) AntharasService antharasService,
			@Autowired(required = false) ValakasService valakasService,
			@Autowired(required = false) BaiumService baiumService,
			@Autowired(required = false) SailrenService sailrenService,
			@Autowired(required = false) QueenAntService queenAntService,
			@Autowired(required = false) ZakenService zakenService,
			@Autowired(required = false) CoreAndOrfenService coreAndOrfenService,
			@Autowired(required = false) FrintezzaService frintezzaService,
			@Autowired(required = false) VanHalterService vanHalterService,
			@Autowired(required = false) FourSepulchersService fourSepulchersService,
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) NpcTemplateTable templates,
			@Autowired(required = false) ObjectIdFactory objectIds) {
		this.raidBossSpawnManager = raidBossSpawnManager;
		this.grandBossManager = grandBossManager;
		this.antharasService = antharasService;
		this.valakasService = valakasService;
		this.baiumService = baiumService;
		this.sailrenService = sailrenService;
		this.queenAntService = queenAntService;
		this.zakenService = zakenService;
		this.coreAndOrfenService = coreAndOrfenService;
		this.frintezzaService = frintezzaService;
		this.vanHalterService = vanHalterService;
		this.fourSepulchersService = fourSepulchersService;
		this.world = world;
		this.templates = templates;
		this.objectIds = objectIds;
	}

	public BossManager(
			GrandBossManager grandBossManager,
			RaidBossSpawnManager raidBossSpawnManager,
			AntharasService antharasService,
			ValakasService valakasService,
			BaiumService baiumService,
			SailrenService sailrenService,
			QueenAntService queenAntService,
			ZakenService zakenService,
			CoreAndOrfenService coreAndOrfenService,
			FrintezzaService frintezzaService) {
		this(raidBossSpawnManager, grandBossManager, antharasService, valakasService,
				baiumService, sailrenService, queenAntService, zakenService,
				coreAndOrfenService, frintezzaService, null, null, null, null, null);
	}

	@PostConstruct
	public void init() {
		instance = this;
		log.info("BossManager inicializado com sucesso.");
	}

	public static BossManager getInstance() {
		return instance;
	}

	public RaidBossSpawnManager raidBossSpawnManager() {
		return raidBossSpawnManager;
	}

	public GrandBossManager grandBossManager() {
		return grandBossManager;
	}

	public AntharasService antharasService() {
		return antharasService;
	}

	public ValakasService valakasService() {
		return valakasService;
	}

	public BaiumService baiumService() {
		return baiumService;
	}

	public SailrenService sailrenService() {
		return sailrenService;
	}

	public QueenAntService queenAntService() {
		return queenAntService;
	}

	public ZakenService zakenService() {
		return zakenService;
	}

	public CoreAndOrfenService coreAndOrfenService() {
		return coreAndOrfenService;
	}

	public FrintezzaService frintezzaService() {
		return frintezzaService;
	}

	public VanHalterService vanHalterService() {
		return vanHalterService;
	}

	public FourSepulchersService fourSepulchersService() {
		return fourSepulchersService;
	}

	/**
	 * Processa a morte de qualquer monstro/chefe no servidor.
	 */
	public void onBossKilled(NpcInstance npc, PlayerCharacter killer, Party party) {
		if (npc == null) {
			return;
		}
		int npcId = npc.npcId();

		if (raidBossSpawnManager != null) {
			raidBossSpawnManager.onBossKilled(npc, killer);
		}

		if (antharasService != null && (npcId == 29019 || npcId == 29066 || npcId == 29067 || npcId == 29068)) {
			antharasService.onAntharasKilled(killer);
		}

		if (valakasService != null && npcId == 29028) {
			valakasService.onValakasKilled(killer);
		}

		if (baiumService != null && npcId == 29020) {
			baiumService.onBaiumKilled(killer);
		}

		if (sailrenService != null && (npcId == 29065 || npcId == 22196 || npcId == 22197 || npcId == 22198 || npcId == 22218)) {
			sailrenService.onSailrenKilled(killer);
		}

		if (queenAntService != null && npcId == 29001) {
			queenAntService.onQueenAntKilled(killer);
		}

		if (zakenService != null && npcId == 29022) {
			zakenService.onZakenKilled(killer);
		}

		if (coreAndOrfenService != null) {
			if (npcId == 29006) {
				coreAndOrfenService.onCoreKilled(killer);
			} else if (npcId == 29014) {
				coreAndOrfenService.onOrfenKilled(killer);
			}
		}

		if (vanHalterService != null && npcId == VanHalterService.VAN_HALTER) {
			vanHalterService.onVanHalterKilled(killer);
		}

		if (fourSepulchersService != null) {
			if (npcId == FourSepulchersService.SHADOW_OF_HALISHA_CONQUERORS) {
				fourSepulchersService.defeatShadowOfHalisha(FourSepulchersService.SEPULCHER_CONQUERORS, killer);
			} else if (npcId == FourSepulchersService.SHADOW_OF_HALISHA_EMPERORS) {
				fourSepulchersService.defeatShadowOfHalisha(FourSepulchersService.SEPULCHER_EMPERORS, killer);
			} else if (npcId == FourSepulchersService.SHADOW_OF_HALISHA_SAGES) {
				fourSepulchersService.defeatShadowOfHalisha(FourSepulchersService.SEPULCHER_SAGES, killer);
			} else if (npcId == FourSepulchersService.SHADOW_OF_HALISHA_JUDGES) {
				fourSepulchersService.defeatShadowOfHalisha(FourSepulchersService.SEPULCHER_JUDGES, killer);
			}
		}

		// Fate's Whisper (Quest 234) Subclass Boss Chests: Cabrio (25035), Kernon (25054), Golkonda (25126), Hallate (25220)
		checkSpawnSubclassChest(npc);
	}

	private void checkSpawnSubclassChest(NpcInstance boss) {
		if (world == null || templates == null || objectIds == null || boss == null) {
			return;
		}
		int chestNpcId = switch (boss.npcId()) {
			case 25035 -> 31027; // Shax / Cabrio -> Coffer of the Dead
			case 25054 -> 31028; // Kernon -> Kernon's Chest
			case 25126 -> 31029; // Golkonda -> Golkonda's Chest
			case 25220 -> 31030; // Hallate -> Hallate's Chest
			default -> 0;
		};
		if (chestNpcId == 0) {
			return;
		}

		templates.get(chestNpcId).ifPresent(tpl -> {
			NpcInstance chest = new NpcInstance(objectIds.nextId(), tpl, boss.x(), boss.y(), boss.z(), boss.heading());
			world.addNpc(chest);
			log.info("Spawned quest chest {} ({}) at ({}, {}, {}) after {} death.",
					chestNpcId, tpl.name(), boss.x(), boss.y(), boss.z(), boss.template().name());
			scheduler.schedule(() -> {
				try {
					world.removeNpc(chest);
					log.info("Quest chest {} despawned after 120 seconds.", chestNpcId);
				} catch (Exception e) {
					log.error("Error despawning quest chest {}: {}", chestNpcId, e.getMessage());
				}
			}, 120, TimeUnit.SECONDS);
		});
	}

	/**
	 * Exibe o diálogo HTML personalizado para NPCs de entrada e saida de Grand Bosses.
	 * Retorna true se o NPC foi tratado como NPC especial de Boss.
	 */
	public boolean showBossNpcHtml(PlayerCharacter player, NpcInstance npc,
			Consumer<com.lopez.l2j.network.game.packet.GameServerPacket> packetSender) {
		if (npc == null || player == null) {
			return false;
		}
		int npcId = npc.npcId();

		if (npcId == NPC_HEART_OF_WARDING) {
			String html = """
					<html><body>Heart of Warding:<br>
					The cavern trembles with the dread aura of the Land Dragon Antharas.<br>
					Only warriors holding the <font color="LEVEL">Portal Stone</font> can pass through.<br><br>
					<a action="bypass -h boss_antharas_enter">Enter Antharas' Lair</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_ANTHARAS_CUBE) {
			String html = """
					<html><body>Teleportation Cube:<br>
					The fury of the dragon has calmed. You may leave the lair.<br><br>
					<a action="bypass -h boss_antharas_exit">Teleport to Entrance</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_KLEIN) {
			String html = """
					<html><body>Watcher of Valakas Klein:<br>
					Beyond this barrier of searing flame lies the Hall of Flames where Valakas dwells.<br>
					Only champions holding the <font color="LEVEL">Floating Stone</font> may enter.<br><br>
					<a action="bypass -h boss_valakas_enter">Enter Valakas' Lair</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_VALAKAS_CUBE) {
			String html = """
					<html><body>Teleportation Cube:<br>
					The magma cools down. You may return safely to the volcanic outskirts.<br><br>
					<a action="bypass -h boss_valakas_exit">Teleport Outside</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_ANGELIC_VORTEX) {
			String html = """
					<html><body>Angelic Vortex:<br>
					A divine dimensional rift ascending to the 14th floor of the Tower of Insolence.<br>
					<font color="LEVEL">Requirement:</font> Blooded Fabric.<br><br>
					<a action="bypass -h boss_baium_enter">Ascend to the 14th Floor</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_BAIUM_STATUE) {
			String html = """
					<html><body>Statue of Emperor Baium:<br>
					The ancient monarch stands frozen in eternal stone.<br>
					Will you disturb his thousand-year slumber?<br><br>
					<a action="bypass -h boss_baium_awaken">Touch the statue and awaken Baium</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_BAIUM_CUBE) {
			String html = """
					<html><body>Teleportation Cube:<br>
					The dimensional rift is open. Teleport back to the 13th floor.<br><br>
					<a action="bypass -h boss_baium_exit">Descend to 13th Floor</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_SHILEN_STATUE) {
			String html = """
					<html><body>Shilen's Stone Statue:<br>
					The ancient velociraptors and the great predator Sailren await in the Primeval Nest.<br>
					<font color="LEVEL">Requirement:</font> Gazkh.<br><br>
					<a action="bypass -h boss_sailren_enter">Enter Sailren's Nest</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		if (npcId == NPC_SAILREN_CUBE) {
			String html = """
					<html><body>Teleportation Cube:<br>
					Leave the Primeval Nest and return to the island.<br><br>
					<a action="bypass -h boss_sailren_exit">Exit Nest</a>
					</body></html>
					""";
			packetSender.accept(new NpcHtmlMessage(npc.objectId(), html));
			return true;
		}

		return false;
	}

	/**
	 * Processa bypasses com prefixo boss_ acionados por jogadores.
	 * Retorna true se o comando foi tratado.
	 */
	public boolean handleBypass(GameSession session, String command) {
		if (session == null || command == null || !command.startsWith("boss_")) {
			return false;
		}

		PlayerCharacter player = session.activeChar();
		if (player == null) {
			session.send(new ActionFailed());
			return true;
		}

		switch (command) {
			case "boss_antharas_enter" -> {
				if (antharasService == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Antharas Service indisponivel."));
					return true;
				}
				if (!antharasService.canEnter(player)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Voce nao cumpre os requisitos (Portal Stone) ou o covil esta inacessivel no momento."));
					return true;
				}
				boolean ok = antharasService.enterLair(player);
				if (ok) {
					session.teleportToCoordinates(player.x(), player.y(), player.z());
				}
			}
			case "boss_antharas_exit" -> {
				session.teleportToCoordinates(79800, 151200, -3534);
			}
			case "boss_valakas_enter" -> {
				if (valakasService == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Valakas Service indisponivel."));
					return true;
				}
				if (!valakasService.canEnter(player)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Voce nao cumpre os requisitos (Floating Stone, capacidade maxima ou boss em intervalo)."));
					return true;
				}
				boolean ok = valakasService.enterLair(player);
				if (ok) {
					session.teleportToCoordinates(player.x(), player.y(), player.z());
				}
			}
			case "boss_valakas_exit" -> {
				session.teleportToCoordinates(150037, -57720, -2976);
			}
			case "boss_baium_enter" -> {
				if (baiumService == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Baium Service indisponivel."));
					return true;
				}
				if (!baiumService.canEnter(player)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"A batalha ja comecou ou voce nao possui o Blooded Fabric."));
					return true;
				}
				boolean ok = baiumService.enterFloor(player);
				if (ok) {
					session.teleportToCoordinates(player.x(), player.y(), player.z());
				}
			}
			case "boss_baium_awaken" -> {
				if (baiumService == null) {
					return true;
				}
				boolean ok = baiumService.awakenBaium(player);
				if (!ok) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Nao foi possivel despertar Baium (requisito de Blooded Fabric nao atendido ou ja despertado)."));
				}
			}
			case "boss_baium_exit" -> {
				session.teleportToCoordinates(113998, 16000, 5100);
			}
			case "boss_sailren_enter" -> {
				if (sailrenService == null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sailren Service indisponivel."));
					return true;
				}
				if (!sailrenService.canEnter(player)) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Voce nao possui o item Gazkh ou a area esta em intervalo."));
					return true;
				}
				boolean ok = sailrenService.enterNest(player);
				if (ok) {
					session.teleportToCoordinates(player.x(), player.y(), player.z());
				}
			}
			case "boss_sailren_exit" -> {
				session.teleportToCoordinates(19688, -8478, -1184);
			}
			default -> {
				return false;
			}
		}

		return true;
	}
}
