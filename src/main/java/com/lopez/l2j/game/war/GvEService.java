package com.lopez.l2j.game.war;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico para gerenciamento do modo Good vs Evil (GvE) - Onda C10.
 *
 * <p>Recursos:</p>
 * <ul>
 *   <li>Duas facções rivais: Good (Azul) e Evil (Vermelho).</li>
 *   <li>Relacionamento automatico: membros da mesma facção sao aliados, membros opostos sao inimigos diretos (Karma-free).</li>
 *   <li>Pontuacao por abates e conquista de relicarios territoriais.</li>
 *   <li>Recompensas organicas em Adena por vitoria em combate.</li>
 * </ul>
 */
@Service
public class GvEService {

	private static final Logger log = LoggerFactory.getLogger(GvEService.class);

	public static final int REWARD_PER_KILL_ADENA = 5000;
	public static final int KILL_REWARD_ADENA = REWARD_PER_KILL_ADENA;
	public static final int RELIC_CAPTURE_POINTS = 50;
	public static final String VAR_GVE_FACTION = "GVE_FACTION";

	public record GvEZone(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		public boolean contains(int x, int y, int z) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
		}
	}

	private InventoryService inventoryService;
	private CharacterVariablesService characterVariables;

	private volatile boolean active = true;
	private final AtomicInteger goodScore = new AtomicInteger(0);
	private final AtomicInteger evilScore = new AtomicInteger(0);
	private final List<GvEZone> gveZones = new CopyOnWriteArrayList<>();
	private final Map<String, FactionType> relicOwners = new ConcurrentHashMap<>();

	public GvEService() {
		this(null, null);
	}

	@Autowired
	public GvEService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) CharacterVariablesService characterVariables) {
		this.inventoryService = inventoryService;
		this.characterVariables = characterVariables;
		// Registra zona padrao de fronteira GvE (ex: Gludio Borderlands)
		registerGvEZone("Gludio Borderlands", -20000, 20000, 100000, 140000, -5000, 5000);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void registerGvEZone(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		gveZones.add(new GvEZone(name, minX, maxX, minY, maxY, minZ, maxZ));
	}

	public boolean isInsideGvEZone(int x, int y, int z) {
		for (GvEZone zone : gveZones) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Associa o jogador a uma facção, atualiza sua cor de nome e persiste em CharacterVariablesService.
	 */
	public void joinFaction(PlayerCharacter player, FactionType faction) {
		if (player == null || faction == null) {
			return;
		}
		player.faction(faction);
		player.nameColor(faction.nameColor());
		if (characterVariables != null) {
			if (faction == FactionType.NONE) {
				characterVariables.delete(player.objectId(), VAR_GVE_FACTION);
			} else {
				characterVariables.set(player.objectId(), VAR_GVE_FACTION, faction.name());
			}
		}
		log.info("Jogador {} ingressou na faccao {} (Cor: 0x{})",
				player.name(), faction.displayName(), Integer.toHexString(faction.nameColor()));
	}

	/**
	 * Desassocia o jogador da facção, restaura a cor de nome padrao e remove a variavel persistida.
	 */
	public void leaveFaction(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		player.faction(FactionType.NONE);
		player.nameColor(0xFFFFFF);
		if (characterVariables != null) {
			characterVariables.delete(player.objectId(), VAR_GVE_FACTION);
		}
		log.info("Jogador {} deixou sua faccao GvE.", player.name());
	}

	/**
	 * Carrega a facção salva do jogador no login via CharacterVariablesService.
	 */
	public FactionType loadPlayerFaction(PlayerCharacter player) {
		if (player == null) {
			return FactionType.NONE;
		}
		if (characterVariables != null) {
			String saved = characterVariables.get(player.objectId(), VAR_GVE_FACTION);
			if (saved != null && !saved.isBlank()) {
				try {
					FactionType ft = FactionType.valueOf(saved.trim().toUpperCase(Locale.ROOT));
					player.faction(ft);
					if (ft != FactionType.NONE) {
						player.nameColor(ft.nameColor());
					}
					return ft;
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
		return player.faction();
	}

	/**
	 * Determina se o alvo e considerado um inimigo sob as regras de facção GvE.
	 */
	public boolean isEnemy(PlayerCharacter attacker, PlayerCharacter target) {
		if (!active || attacker == null || target == null) {
			return false;
		}
		if (attacker.faction() == FactionType.NONE || target.faction() == FactionType.NONE) {
			return false;
		}
		return attacker.faction().isOpponent(target.faction());
	}

	/**
	 * Determina se dois jogadores sao aliados da mesma facção.
	 */
	public boolean isAlly(PlayerCharacter p1, PlayerCharacter p2) {
		if (!active || p1 == null || p2 == null) {
			return false;
		}
		if (p1.faction() == FactionType.NONE || p2.faction() == FactionType.NONE) {
			return false;
		}
		return p1.faction() == p2.faction();
	}

	/**
	 * Processa o abate de um inimigo no GvE conferindo pontos e recompensa em Adena.
	 */
	public boolean onKill(PlayerCharacter killer, PlayerCharacter victim) {
		if (!active || killer == null || victim == null) {
			return false;
		}
		if (killer.objectId() == victim.objectId()) {
			return false;
		}
		if (!isEnemy(killer, victim)) {
			return false;
		}

		if (killer.faction() == FactionType.GOOD) {
			goodScore.incrementAndGet();
		} else if (killer.faction() == FactionType.EVIL) {
			evilScore.incrementAndGet();
		}

		// Recompensa em Adena com persistencia real via InventoryService e fallback em memoria
		if (killer.inventory() != null) {
			boolean persisted = false;
			if (inventoryService != null) {
				var added = inventoryService.addItem(killer.inventory(), 57, KILL_REWARD_ADENA, "GvEKill");
				persisted = (added != null);
			}
			if (!persisted) {
				var opt = killer.inventory().byItemId(57);
				opt.ifPresent(adena -> adena.count(adena.count() + KILL_REWARD_ADENA));
			}
		}

		log.info("GvE Kill: {} [{}] eliminou {} [{}]. Placar Good: {} vs Evil: {}",
				killer.name(), killer.faction(), victim.name(), victim.faction(),
				goodScore.get(), evilScore.get());
		return true;
	}

	/**
	 * Processa a conquista de um relicário territorial por uma facção.
	 */
	public void captureRelic(String relicName, FactionType faction) {
		if (faction == null || faction == FactionType.NONE) {
			return;
		}
		relicOwners.put(relicName, faction);
		if (faction == FactionType.GOOD) {
			goodScore.addAndGet(RELIC_CAPTURE_POINTS);
		} else if (faction == FactionType.EVIL) {
			evilScore.addAndGet(RELIC_CAPTURE_POINTS);
		}
		log.info("GvE Relic: A faccao {} conquistou o relicario '{}'! (+{} pontos)",
				faction.displayName(), relicName, RELIC_CAPTURE_POINTS);
	}

	public int getScore(FactionType faction) {
		if (faction == FactionType.GOOD) return goodScore.get();
		if (faction == FactionType.EVIL) return evilScore.get();
		return 0;
	}

	public FactionType getRelicOwner(String relicName) {
		return relicOwners.getOrDefault(relicName, FactionType.NONE);
	}

	public void setInventoryService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public void startEvent() {
		this.active = true;
		this.goodScore.set(0);
		this.evilScore.set(0);
		log.info("GvEService: Evento Good vs Evil INICIADO!");
	}

	public void stopEvent() {
		this.active = false;
		log.info("GvEService: Evento Good vs Evil ENCERRADO! Placar final: Good {} x Evil {}",
				goodScore.get(), evilScore.get());
	}

	public String getStatusHtml(PlayerCharacter player) {
		FactionType myFac = (player != null && player.faction() != null) ? player.faction() : FactionType.NONE;
		return "<html><body><title>Good vs Evil</title><center>"
				+ "<font color=\"LEVEL\">== Good vs Evil Faction ==</font><br><br>"
				+ "Status: " + (active ? "<font color=\"00FF00\">ATIVO</font>" : "<font color=\"FF0000\">INATIVO</font>") + "<br>"
				+ "Sua Faccao: <font color=\"AAAAAA\">" + myFac.displayName() + "</font><br><br>"
				+ "Placar Geral:<br>"
				+ "<font color=\"3366FF\">Good: " + goodScore.get() + "</font> | "
				+ "<font color=\"FF3300\">Evil: " + evilScore.get() + "</font><br><br>"
				+ "<button value=\"Entrar GOOD\" action=\"bypass voiced_gve_join good\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>"
				+ "<button value=\"Entrar EVIL\" action=\"bypass voiced_gve_join evil\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\">"
				+ "</center></body></html>";
	}

	public void setCharacterVariablesService(CharacterVariablesService characterVariables) {
		this.characterVariables = characterVariables;
	}
}
