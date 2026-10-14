package com.lopez.l2j.features;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.util.Rnd;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico completo do Startup System (baseado em StartupSystem.java do L2JDreamV2).
 * Gerencia selecao inicial de classe, armadura (Robe/Light/Heavy), arma (BP_WEAPON_01..31 / WP_01..31_ID),
 * escudo, flechas, buffs iniciais de Fighter/Mage, desativacao do tutorial newbie e titulo inicial.
 */
@Service
public class StartupSystemService {

	private static final Logger log = LoggerFactory.getLogger(StartupSystemService.class);

	public static final String VAR_SELECT_CLASS = "select_class";
	public static final String VAR_SELECT_ARMOR = "select_armor";
	public static final String VAR_SELECT_WEAPON = "select_weapon";
	public static final String VAR_STARTUP_FINISH = "startupfinish";

	private static final Map<String, Integer> THIRD_CLASS_COMMANDS = new LinkedHashMap<>();

	static {
		THIRD_CLASS_COMMANDS.put("Duelist", 88);
		THIRD_CLASS_COMMANDS.put("DreadNought", 89);
		THIRD_CLASS_COMMANDS.put("Phoenix_Knight", 90);
		THIRD_CLASS_COMMANDS.put("Hell_Knight", 91);
		THIRD_CLASS_COMMANDS.put("Sagittarius", 92);
		THIRD_CLASS_COMMANDS.put("Adventurer", 93);
		THIRD_CLASS_COMMANDS.put("Archmage", 94);
		THIRD_CLASS_COMMANDS.put("Soultaker", 95);
		THIRD_CLASS_COMMANDS.put("Arcana_Lord", 96);
		THIRD_CLASS_COMMANDS.put("Cardinal", 97);
		THIRD_CLASS_COMMANDS.put("Hierophant", 98);
		THIRD_CLASS_COMMANDS.put("Eva_Templar", 99);
		THIRD_CLASS_COMMANDS.put("Sword_Muse", 100);
		THIRD_CLASS_COMMANDS.put("Wind_Rider", 101);
		THIRD_CLASS_COMMANDS.put("Moonlight_Sentinel", 102);
		THIRD_CLASS_COMMANDS.put("Mystic_Muse", 103);
		THIRD_CLASS_COMMANDS.put("Elemental_Master", 104);
		THIRD_CLASS_COMMANDS.put("Eva_Saint", 105);
		THIRD_CLASS_COMMANDS.put("Shillien_Templar", 106);
		THIRD_CLASS_COMMANDS.put("Spectral_Dancer", 107);
		THIRD_CLASS_COMMANDS.put("Ghost_Hunter", 108);
		THIRD_CLASS_COMMANDS.put("Ghost_Sentinel", 109);
		THIRD_CLASS_COMMANDS.put("Storm_Screamer", 110);
		THIRD_CLASS_COMMANDS.put("Spectral_Master", 111);
		THIRD_CLASS_COMMANDS.put("Shillien_Saint", 112);
		THIRD_CLASS_COMMANDS.put("Titan", 113);
		THIRD_CLASS_COMMANDS.put("Grand_Khauatari", 114);
		THIRD_CLASS_COMMANDS.put("Dominator", 115);
		THIRD_CLASS_COMMANDS.put("Doomcryer", 116);
		THIRD_CLASS_COMMANDS.put("Fortune_Seeker", 117);
		THIRD_CLASS_COMMANDS.put("Maestro", 118);
	}

	private final CharacterVariablesService variablesService;
	private final InventoryService inventoryService;
	private final Map<Integer, Map<String, String>> localVars = new java.util.concurrent.ConcurrentHashMap<>();

	public StartupSystemService() {
		this(null, null);
	}

	@Autowired
	public StartupSystemService(
			@Autowired(required = false) CharacterVariablesService variablesService,
			@Autowired(required = false) InventoryService inventoryService) {
		this.variablesService = variablesService;
		this.inventoryService = inventoryService;
	}

	public boolean isStartupEnabled() {
		return Config.ENABLE_STARTUP_SYSTEM;
	}

	public boolean isNewbieTutorialDisabled() {
		return Config.DISABLE_NEWBIE_TUTORIAL;
	}

	/**
	 * Aplica titulo inicial customizado e cor do titulo ao criar ou iniciar personagem.
	 */
	public void applyCustomStartTitle(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		if (Config.CUSTOM_START_TITLE && Config.CUSTOM_TITLE_TEXT != null && !Config.CUSTOM_TITLE_TEXT.isBlank()) {
			player.title(Config.CUSTOM_TITLE_TEXT.trim());
		}
		if (Config.TITLE_COLOR != null && !Config.TITLE_COLOR.isBlank()) {
			try {
				player.titleColor(Integer.decode("0x" + Config.TITLE_COLOR.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
	}

	/**
	 * Ponto de entrada no EnterWorld quando ENABLE_STARTUP_SYSTEM esta ativo.
	 * Retorna o HTML da etapa atual do Startup System, ou null se ja concluido/desativado.
	 */
	public String onEnterWorld(PlayerCharacter player) {
		if (player == null) {
			return null;
		}
		if (Config.DISABLE_NEWBIE_TUTORIAL) {
			setVar(player.objectId(), "tutorial_disabled", "1");
		}
		if (!Config.ENABLE_STARTUP_SYSTEM) {
			return null;
		}
		if (getVar(player.objectId(), VAR_STARTUP_FINISH) != null) {
			return null;
		}
		return startSetup(player);
	}

	public String startSetup(PlayerCharacter player) {
		if (player == null) {
			return null;
		}
		if (getVar(player.objectId(), VAR_SELECT_CLASS) == null && Config.STARTUP_SYSTEM_CLASS) {
			return buildSelectClassHtml(player);
		}
		if (getVar(player.objectId(), VAR_SELECT_ARMOR) == null && Config.STARTUP_SYSTEM_ARMOR) {
			return buildSelectArmorHtml(player);
		}
		if (getVar(player.objectId(), VAR_SELECT_WEAPON) == null && Config.STARTUP_SYSTEM_WEAPON) {
			return buildSelectWeaponHtml(player);
		}
		if (getVar(player.objectId(), VAR_STARTUP_FINISH) == null) {
			return buildFinishHtml(player);
		}
		return null;
	}

	/**
	 * Processa comandos de bypass do Startup System (classes, sets de armadura, 31 armas e finalizacao).
	 */
	public String handleCommand(PlayerCharacter player, String command) {
		if (player == null || command == null || command.isBlank()) {
			return null;
		}
		String cmd = command.trim();

		// 1. Selecao de Classe
		if (getVar(player.objectId(), VAR_SELECT_CLASS) == null && Config.STARTUP_SYSTEM_CLASS) {
			for (var entry : THIRD_CLASS_COMMANDS.entrySet()) {
				if (cmd.startsWith(entry.getKey())) {
					if (player.classId() >= 88 && player.classId() <= 118) {
						log.warn("StartupSystem: jogador {} ja possui 3a classe ({})", player.name(), player.classId());
						return null;
					}
					int targetClassId = entry.getValue();
					player.classId(targetClassId);
					player.baseClassId(targetClassId);
					setVar(player.objectId(), VAR_SELECT_CLASS, "1");
					if (getVar(player.objectId(), VAR_SELECT_ARMOR) == null && Config.STARTUP_SYSTEM_ARMOR) {
						return buildSelectArmorHtml(player);
					}
					if (getVar(player.objectId(), VAR_SELECT_WEAPON) == null && Config.STARTUP_SYSTEM_WEAPON) {
						return buildSelectWeaponHtml(player);
					}
					return buildFinishHtml(player);
				}
			}
		}

		// 2. Selecao de Armadura (Heavy, Light, Robe)
		if (Config.HTM_HEAVY != null && !Config.HTM_HEAVY.isBlank() && cmd.startsWith(Config.HTM_HEAVY)) {
			grantArmorSet(player, Config.SET_HEAVY);
			setVar(player.objectId(), VAR_SELECT_ARMOR, "1");
			return Config.STARTUP_SYSTEM_WEAPON ? buildSelectWeaponHtml(player) : buildFinishHtml(player);
		}
		if (Config.HTM_LIGHT != null && !Config.HTM_LIGHT.isBlank() && cmd.startsWith(Config.HTM_LIGHT)) {
			grantArmorSet(player, Config.SET_LIGHT);
			setVar(player.objectId(), VAR_SELECT_ARMOR, "1");
			return Config.STARTUP_SYSTEM_WEAPON ? buildSelectWeaponHtml(player) : buildFinishHtml(player);
		}
		if (Config.HTM_ROBE != null && !Config.HTM_ROBE.isBlank() && cmd.startsWith(Config.HTM_ROBE)) {
			grantArmorSet(player, Config.SET_ROBE);
			setVar(player.objectId(), VAR_SELECT_ARMOR, "1");
			return Config.STARTUP_SYSTEM_WEAPON ? buildSelectWeaponHtml(player) : buildFinishHtml(player);
		}

		// 3. Selecao de Arma (BP_WEAPON_01..31 -> WP_01..31_ID)
		if (getVar(player.objectId(), VAR_SELECT_WEAPON) == null) {
			if (matchesWeaponCommand(cmd, Config.BP_WEAPON_01)) {
				grantWeaponChoice(player, Config.WP_01_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_02)) {
				grantWeaponChoice(player, Config.WP_02_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_03)) {
				grantWeaponChoice(player, Config.WP_03_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_04)) {
				grantWeaponChoice(player, Config.WP_04_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_05)) {
				grantWeaponChoice(player, Config.WP_05_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_06)) {
				grantWeaponChoice(player, Config.WP_06_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_07)) {
				grantWeaponChoice(player, Config.WP_07_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_08)) {
				grantWeaponChoice(player, Config.WP_08_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_09)) {
				grantWeaponChoice(player, Config.WP_09_ID, true, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_10)) {
				grantWeaponChoice(player, Config.WP_10_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_11)) {
				grantWeaponChoice(player, Config.WP_11_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_12)) {
				grantWeaponChoice(player, Config.WP_12_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_13)) {
				grantWeaponChoice(player, Config.WP_13_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_14)) {
				grantWeaponChoice(player, Config.WP_14_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_15)) {
				grantWeaponChoice(player, Config.WP_15_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_16)) {
				grantWeaponChoice(player, Config.WP_16_ID, false, true);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_17)) {
				grantWeaponChoice(player, Config.WP_17_ID, false, true);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_18)) {
				grantWeaponChoice(player, Config.WP_18_ID, false, true);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_19)) {
				grantWeaponChoice(player, Config.WP_19_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_20)) {
				grantWeaponChoice(player, Config.WP_20_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_21)) {
				grantWeaponChoice(player, Config.WP_21_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_22)) {
				grantWeaponChoice(player, Config.WP_22_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_23)) {
				grantWeaponChoice(player, Config.WP_23_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_24)) {
				grantWeaponChoice(player, Config.WP_24_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_25)) {
				grantWeaponChoice(player, Config.WP_25_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_26)) {
				grantWeaponChoice(player, Config.WP_26_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_27)) {
				grantWeaponChoice(player, Config.WP_27_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_28)) {
				grantWeaponChoice(player, Config.WP_28_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_29)) {
				grantWeaponChoice(player, Config.WP_29_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_30)) {
				grantWeaponChoice(player, Config.WP_30_ID, false, false);
				return buildFinishHtml(player);
			} else if (matchesWeaponCommand(cmd, Config.BP_WEAPON_31)) {
				grantWeaponChoice(player, Config.WP_31_ID, false, false);
				return buildFinishHtml(player);
			}
		}

		// 4. Finalizacao do Startup (end_setup)
		if (cmd.startsWith("end_setup")) {
			completeStartup(player);
			return null;
		}

		return null;
	}

	private boolean matchesWeaponCommand(String command, String bypassKey) {
		return bypassKey != null && !bypassKey.isBlank() && !"0".equals(bypassKey) && command.startsWith(bypassKey);
	}

	/**
	 * Retorna o mapa completo de todos os 31 mapeamentos de bypass de armas configurados.
	 */
	public Map<String, Integer> getConfiguredWeaponMappings() {
		Map<String, Integer> map = new LinkedHashMap<>();
		putWeaponMapping(map, Config.BP_WEAPON_01, Config.WP_01_ID);
		putWeaponMapping(map, Config.BP_WEAPON_02, Config.WP_02_ID);
		putWeaponMapping(map, Config.BP_WEAPON_03, Config.WP_03_ID);
		putWeaponMapping(map, Config.BP_WEAPON_04, Config.WP_04_ID);
		putWeaponMapping(map, Config.BP_WEAPON_05, Config.WP_05_ID);
		putWeaponMapping(map, Config.BP_WEAPON_06, Config.WP_06_ID);
		putWeaponMapping(map, Config.BP_WEAPON_07, Config.WP_07_ID);
		putWeaponMapping(map, Config.BP_WEAPON_08, Config.WP_08_ID);
		putWeaponMapping(map, Config.BP_WEAPON_09, Config.WP_09_ID);
		putWeaponMapping(map, Config.BP_WEAPON_10, Config.WP_10_ID);
		putWeaponMapping(map, Config.BP_WEAPON_11, Config.WP_11_ID);
		putWeaponMapping(map, Config.BP_WEAPON_12, Config.WP_12_ID);
		putWeaponMapping(map, Config.BP_WEAPON_13, Config.WP_13_ID);
		putWeaponMapping(map, Config.BP_WEAPON_14, Config.WP_14_ID);
		putWeaponMapping(map, Config.BP_WEAPON_15, Config.WP_15_ID);
		putWeaponMapping(map, Config.BP_WEAPON_16, Config.WP_16_ID);
		putWeaponMapping(map, Config.BP_WEAPON_17, Config.WP_17_ID);
		putWeaponMapping(map, Config.BP_WEAPON_18, Config.WP_18_ID);
		putWeaponMapping(map, Config.BP_WEAPON_19, Config.WP_19_ID);
		putWeaponMapping(map, Config.BP_WEAPON_20, Config.WP_20_ID);
		putWeaponMapping(map, Config.BP_WEAPON_21, Config.WP_21_ID);
		putWeaponMapping(map, Config.BP_WEAPON_22, Config.WP_22_ID);
		putWeaponMapping(map, Config.BP_WEAPON_23, Config.WP_23_ID);
		putWeaponMapping(map, Config.BP_WEAPON_24, Config.WP_24_ID);
		putWeaponMapping(map, Config.BP_WEAPON_25, Config.WP_25_ID);
		putWeaponMapping(map, Config.BP_WEAPON_26, Config.WP_26_ID);
		putWeaponMapping(map, Config.BP_WEAPON_27, Config.WP_27_ID);
		putWeaponMapping(map, Config.BP_WEAPON_28, Config.WP_28_ID);
		putWeaponMapping(map, Config.BP_WEAPON_29, Config.WP_29_ID);
		putWeaponMapping(map, Config.BP_WEAPON_30, Config.WP_30_ID);
		putWeaponMapping(map, Config.BP_WEAPON_31, Config.WP_31_ID);
		return map;
	}

	private void putWeaponMapping(Map<String, Integer> map, String bypass, int itemId) {
		if (bypass != null && !bypass.isBlank() && !"0".equals(bypass) && itemId > 0) {
			map.put(bypass, itemId);
		}
	}

	private void grantArmorSet(PlayerCharacter player, String rawItems) {
		if (player == null || rawItems == null || rawItems.isBlank()) {
			return;
		}
		for (int[] pair : parseItemPairs(rawItems)) {
			addAndEquipItem(player, pair[0], pair[1], "StartupArmor");
		}
	}

	private void grantWeaponChoice(PlayerCharacter player, int weaponId, boolean withShield, boolean withArrows) {
		if (player == null || weaponId <= 0) {
			return;
		}
		addAndEquipItem(player, weaponId, 1, "StartupWeapon");
		if (withShield && Config.SHIELD_ID > 0) {
			addAndEquipItem(player, Config.SHIELD_ID, 1, "StartupShield");
		}
		if (withArrows && Config.ARROW_ID > 0) {
			addItemOnly(player, Config.ARROW_ID, 5000, "StartupArrows");
		}
		setVar(player.objectId(), VAR_SELECT_WEAPON, "1");
	}

	private void addAndEquipItem(PlayerCharacter player, int itemId, int count, String process) {
		if (itemId <= 0 || count <= 0) {
			return;
		}
		if (inventoryService != null) {
			var res = inventoryService.addItem(player.inventory(), itemId, count, process);
			if (res != null && res.item() != null && res.item().template().isEquipable()) {
				player.inventory().equip(res.item());
			}
		} else {
			player.inventory().addItem(itemId, count);
			player.inventory().byItemId(itemId).ifPresent((ItemInstance it) -> {
				if (it.template() != null && it.template().isEquipable()) {
					player.inventory().equip(it);
				}
			});
		}
	}

	private void addItemOnly(PlayerCharacter player, int itemId, int count, String process) {
		if (itemId <= 0 || count <= 0) {
			return;
		}
		if (inventoryService != null) {
			inventoryService.addItem(player.inventory(), itemId, count, process);
		} else {
			player.inventory().addItem(itemId, count);
		}
	}

	/**
	 * Finaliza o Startup System aplicando buffs iniciais (Fighter/Mage), restaurando HP/MP/CP e teleportando.
	 */
	public List<Integer> completeStartup(PlayerCharacter player) {
		List<Integer> appliedBuffs = new ArrayList<>();
		if (player == null) {
			return appliedBuffs;
		}

		if (player.isMageClass()) {
			if (Config.STARTUP_SYSTEM_BUFF_MAGE && Config.MAGE_BUFF_LIST != null) {
				for (Integer skillId : Config.MAGE_BUFF_LIST) {
					if (skillId != null && skillId > 0) {
						appliedBuffs.add(skillId);
					}
				}
			}
		} else {
			if (Config.STARTUP_SYSTEM_BUFF_FIGHT && Config.FIGHTER_BUFF_LIST != null) {
				for (Integer skillId : Config.FIGHTER_BUFF_LIST) {
					if (skillId != null && skillId > 0) {
						appliedBuffs.add(skillId);
					}
				}
			}
		}

		player.currentHp(player.maxHp());
		player.currentMp(player.maxMp());
		player.currentCp(player.maxCp());

		if (getVar(player.objectId(), VAR_STARTUP_FINISH) == null) {
			randomTeleport(player);
		}

		log.info("StartupSystem concluido para {} (buffs aplicados: {})", player.name(), appliedBuffs.size());
		return appliedBuffs;
	}

	public void randomTeleport(PlayerCharacter player) {
		setVar(player.objectId(), VAR_STARTUP_FINISH, "1");
		switch (Rnd.get(5)) {
			case 0 -> player.teleToLocation(82533 + Rnd.get(100), 149122 + Rnd.get(100), -3474);
			case 1 -> player.teleToLocation(82571 + Rnd.get(100), 148060 + Rnd.get(100), -3467);
			case 2 -> player.teleToLocation(81376 + Rnd.get(100), 148042 + Rnd.get(100), -3474);
			case 3 -> player.teleToLocation(81359 + Rnd.get(100), 149218 + Rnd.get(100), -3474);
			default -> player.teleToLocation(82862 + Rnd.get(100), 148606 + Rnd.get(100), -3474);
		}
	}

	public String buildSelectClassHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center><font color=\"LEVEL\">=== Startup Class Selection ===</font><br>");
		sb.append("Bem-vindo, ").append(player.name()).append("! Escolha sua classe:<br>");
		for (String className : THIRD_CLASS_COMMANDS.keySet()) {
			sb.append("<a action=\"bypass -h ").append(className).append("\">").append(className.replace('_', ' ')).append("</a><br1>");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String buildSelectArmorHtml(PlayerCharacter player) {
		return "<html><body><center><font color=\"LEVEL\">=== Startup Armor Selection ===</font><br>"
				+ "Escolha seu conjunto de armadura inicial:<br>"
				+ "<a action=\"bypass -h " + Config.HTM_HEAVY + "\">Heavy Set</a><br>"
				+ "<a action=\"bypass -h " + Config.HTM_LIGHT + "\">Light Set</a><br>"
				+ "<a action=\"bypass -h " + Config.HTM_ROBE + "\">Robe Set</a><br>"
				+ "</center></body></html>";
	}

	public String buildSelectWeaponHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center><font color=\"LEVEL\">=== Startup Weapon Selection ===</font><br>");
		for (var entry : getConfiguredWeaponMappings().entrySet()) {
			sb.append("<a action=\"bypass -h ").append(entry.getKey()).append("\">")
					.append(entry.getKey()).append(" (#").append(entry.getValue()).append(")</a><br1>");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public String buildFinishHtml(PlayerCharacter player) {
		return "<html><body><center><font color=\"LEVEL\">=== Startup Complete ===</font><br>"
				+ "Tudo pronto, " + player.name() + "!<br>"
				+ "<a action=\"bypass -h end_setup\">Entrar no Mundo</a>"
				+ "</center></body></html>";
	}

	public static List<int[]> parseItemPairs(String raw) {
		List<int[]> result = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return result;
		}
		for (String entry : raw.split(";")) {
			if (entry.isBlank()) {
				continue;
			}
			String[] parts = entry.trim().split(",");
			if (parts.length >= 2) {
				try {
					int id = Integer.parseInt(parts[0].trim());
					int count = Integer.parseInt(parts[1].trim());
					if (id > 0 && count > 0) {
						result.add(new int[] { id, count });
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return result;
	}

	public String getVar(int charId, String key) {
		if (variablesService != null) {
			return variablesService.get(charId, key);
		}
		var map = localVars.get(charId);
		return map != null ? map.get(key) : null;
	}

	public void setVar(int charId, String key, String val) {
		if (variablesService != null) {
			variablesService.set(charId, key, val);
		} else {
			localVars.computeIfAbsent(charId, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(key, val);
		}
	}
}
