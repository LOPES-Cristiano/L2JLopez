package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.effect.PlayerEffects;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.skill.SkillService;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.MagicSkillUse;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowBoard;
import com.lopez.l2j.network.game.packet.GameServerPacket.StatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico unificado de NPC Buffer e Buffer por Esquemas (Scheme Buffer).
 * Suporta:
 * - Estilo L2JDreamV2 / L2Interlude (NPCs 50016 e 50005 VIP, getbuff, restore, cancel, fighter, mage, schemes)
 * - Estilo L2JLucera2 (NPCs 40010 e 40011, scripts_services.Buffer:act / actBBS, buff_templates.xml, gravacao de perfis)
 * - Comando de voz (.buffer / voiced_buffer) e Community Board (_bbsbuff_)
 * - Suporte a Personagem e Mascote/Summon.
 */
@Service
public class SchemeBufferService {

	private static final Logger log = LoggerFactory.getLogger(SchemeBufferService.class);

	public static final int ADENA_ID = 57;
	public static final long SCHEME_COST_ADENA = 25_000L;
	public static final long HEAL_COST_ADENA = 5_000L;
	public static final int MAX_SCHEME_BUFFS = 36;

	public record BuffSkillEntry(int skillId, int skillLevel) {}

	public record BuffTemplate(
			int menuId,
			boolean targetPet,
			int consumeItemId,
			long consumeAmount,
			List<BuffSkillEntry> skills
	) {}

	public static final List<Integer> DEFAULT_FIGHTER_BUFFS = List.of(
			1068, 1040, 1086, 1077, 1242, 1240, 1045, 1204, 1268, // Might, Shield, Haste, Focus, Death Whisper, Guidance, Body, WW, Vampiric
			264, 269, 268, 304, // Song of Earth, Hunter, Wind, Vitality
			271, 274, 275, 310, // Dance of Warrior, Fire, Fury, Vampire
			1363 // Chant of Victory
	);

	public static final List<Integer> DEFAULT_MAGE_BUFFS = List.of(
			1040, 1036, 1085, 1059, 1078, 1303, 1045, 1048, 1204, // Shield, Magic Barrier, Acumen, Empower, Concentration, Wild Magic, Body, Soul, WW
			264, 267, 268, 363, // Song of Earth, Warding, Wind, Meditation
			273, 276, // Dance of Mystic, Concentration
			1413 // Magnus' Chant
	);

	/** Catalogo de buffs disponiveis para edicao de esquemas in-game. */
	public static final Map<Integer, String> AVAILABLE_BUFF_NAMES = new LinkedHashMap<>();
	static {
		AVAILABLE_BUFF_NAMES.put(1204, "Wind Walk");
		AVAILABLE_BUFF_NAMES.put(1068, "Might");
		AVAILABLE_BUFF_NAMES.put(1040, "Shield");
		AVAILABLE_BUFF_NAMES.put(1036, "Magic Barrier");
		AVAILABLE_BUFF_NAMES.put(1086, "Haste");
		AVAILABLE_BUFF_NAMES.put(1077, "Focus");
		AVAILABLE_BUFF_NAMES.put(1242, "Death Whisper");
		AVAILABLE_BUFF_NAMES.put(1240, "Guidance");
		AVAILABLE_BUFF_NAMES.put(1268, "Vampiric Rage");
		AVAILABLE_BUFF_NAMES.put(1045, "Blessed Body");
		AVAILABLE_BUFF_NAMES.put(1048, "Blessed Soul");
		AVAILABLE_BUFF_NAMES.put(1085, "Acumen");
		AVAILABLE_BUFF_NAMES.put(1059, "Empower");
		AVAILABLE_BUFF_NAMES.put(1303, "Wild Magic");
		AVAILABLE_BUFF_NAMES.put(1078, "Concentration");
		AVAILABLE_BUFF_NAMES.put(1062, "Berserker Spirit");
		AVAILABLE_BUFF_NAMES.put(1035, "Mental Shield");
		AVAILABLE_BUFF_NAMES.put(1397, "Clarity");
		AVAILABLE_BUFF_NAMES.put(1087, "Agility");
		AVAILABLE_BUFF_NAMES.put(1243, "Blessed Shield");
		AVAILABLE_BUFF_NAMES.put(264, "Song of Earth");
		AVAILABLE_BUFF_NAMES.put(267, "Song of Warding");
		AVAILABLE_BUFF_NAMES.put(268, "Song of Wind");
		AVAILABLE_BUFF_NAMES.put(269, "Song of Hunter");
		AVAILABLE_BUFF_NAMES.put(304, "Song of Vitality");
		AVAILABLE_BUFF_NAMES.put(349, "Song of Renewal");
		AVAILABLE_BUFF_NAMES.put(363, "Song of Meditation");
		AVAILABLE_BUFF_NAMES.put(364, "Song of Champion");
		AVAILABLE_BUFF_NAMES.put(271, "Dance of Warrior");
		AVAILABLE_BUFF_NAMES.put(273, "Dance of Mystic");
		AVAILABLE_BUFF_NAMES.put(274, "Dance of Fire");
		AVAILABLE_BUFF_NAMES.put(275, "Dance of Fury");
		AVAILABLE_BUFF_NAMES.put(276, "Dance of Concentration");
		AVAILABLE_BUFF_NAMES.put(310, "Dance of Vampire");
		AVAILABLE_BUFF_NAMES.put(365, "Dance of Siren");
		AVAILABLE_BUFF_NAMES.put(1363, "Chant of Victory");
		AVAILABLE_BUFF_NAMES.put(1355, "Prophecy of Water");
		AVAILABLE_BUFF_NAMES.put(1356, "Prophecy of Fire");
		AVAILABLE_BUFF_NAMES.put(1357, "Prophecy of Wind");
		AVAILABLE_BUFF_NAMES.put(1413, "Magnus' Chant");
		AVAILABLE_BUFF_NAMES.put(1388, "Greater Might");
		AVAILABLE_BUFF_NAMES.put(1389, "Greater Shield");
		AVAILABLE_BUFF_NAMES.put(4699, "Blessing of Queen");
		AVAILABLE_BUFF_NAMES.put(4703, "Gift of Seraphim");
	}

	private final CharacterVariablesService variablesService;
	private final SkillService skillService;
	private final org.springframework.jdbc.core.simple.JdbcClient jdbc;
	private final Map<Integer, BuffTemplate> luceraTemplates = new ConcurrentHashMap<>();
	private final Map<Integer, List<Integer>> recordingProfiles = new ConcurrentHashMap<>();

	public SchemeBufferService(
			CharacterVariablesService variablesService,
			SkillService skillService) {
		this(variablesService, skillService, null);
	}

	@Autowired
	public SchemeBufferService(
			@Autowired(required = false) CharacterVariablesService variablesService,
			@Autowired(required = false) SkillService skillService,
			@Autowired(required = false) org.springframework.jdbc.core.simple.JdbcClient jdbc) {
		this.variablesService = variablesService;
		this.skillService = skillService;
		this.jdbc = jdbc;
		loadLuceraBuffTemplates();
	}

	/**
	 * Carrega os templates de buff do arquivo data/buff_templates.xml (padrao Lucera)
	 * e popula fallbacks essenciais para funcionamento sem arquivo externo.
	 */
	public final void loadLuceraBuffTemplates() {
		luceraTemplates.clear();
		registerFallbackLuceraTemplates();

		Path xmlPath = Path.of("data/buff_templates.xml");
		if (!Files.isRegularFile(xmlPath)) {
			Path altPath = Path.of("L2JLopez/data/buff_templates.xml");
			if (Files.isRegularFile(altPath)) {
				xmlPath = altPath;
			}
		}
		if (!Files.isRegularFile(xmlPath)) {
			log.debug("SchemeBufferService: data/buff_templates.xml nao encontrado, usando {} templates internos.", luceraTemplates.size());
			return;
		}

		try (InputStream in = Files.newInputStream(xmlPath)) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
			Document doc = db.parse(in);

			NodeList templates = doc.getElementsByTagName("template");
			int count = 0;
			for (int i = 0; i < templates.getLength(); i++) {
				if (!(templates.item(i) instanceof Element tplEl)) continue;
				String menuIdStr = tplEl.getAttribute("menuId");
				if (menuIdStr == null || menuIdStr.isBlank()) continue;
				int menuId = Integer.parseInt(menuIdStr.trim());
				String targetStr = tplEl.getAttribute("target");
				boolean targetPet = "BUFF_PET".equalsIgnoreCase(targetStr);

				int consumeId = ADENA_ID;
				long consumeAmount = 0L;
				NodeList consumes = tplEl.getElementsByTagName("consume");
				if (consumes.getLength() > 0 && consumes.item(0) instanceof Element consumeEl) {
					NodeList items = consumeEl.getElementsByTagName("item");
					if (items.getLength() > 0 && items.item(0) instanceof Element itemEl) {
						String idAttr = itemEl.getAttribute("id");
						String amtAttr = itemEl.getAttribute("amount");
						if (idAttr != null && !idAttr.isBlank()) consumeId = Integer.parseInt(idAttr.trim());
						if (amtAttr != null && !amtAttr.isBlank()) consumeAmount = Long.parseLong(amtAttr.trim());
					}
				}

				List<BuffSkillEntry> skills = new ArrayList<>();
				NodeList produces = tplEl.getElementsByTagName("produce");
				if (produces.getLength() > 0 && produces.item(0) instanceof Element prodEl) {
					NodeList skillNodes = prodEl.getElementsByTagName("skill");
					for (int s = 0; s < skillNodes.getLength(); s++) {
						if (skillNodes.item(s) instanceof Element skEl) {
							int skId = Integer.parseInt(skEl.getAttribute("id").trim());
							int skLvl = Integer.parseInt(skEl.getAttribute("level").trim());
							skills.add(new BuffSkillEntry(skId, skLvl));
						}
					}
				}

				if (!skills.isEmpty()) {
					luceraTemplates.put(menuId, new BuffTemplate(menuId, targetPet, consumeId, consumeAmount, List.copyOf(skills)));
					count++;
				}
			}
			log.info("SchemeBufferService: {} templates de buff carregados de {}", count, xmlPath);
		} catch (Exception e) {
			log.warn("SchemeBufferService: Falha ao carregar data/buff_templates.xml ({}), usando fallbacks internos.", e.getMessage());
		}
	}

	private void registerFallbackLuceraTemplates() {
		List<BuffSkillEntry> fighterEntries = DEFAULT_FIGHTER_BUFFS.stream()
				.map(id -> new BuffSkillEntry(id, getDefaultSkillLevel(id)))
				.toList();
		List<BuffSkillEntry> mageEntries = DEFAULT_MAGE_BUFFS.stream()
				.map(id -> new BuffSkillEntry(id, getDefaultSkillLevel(id)))
				.toList();

		luceraTemplates.put(124, new BuffTemplate(124, false, ADENA_ID, 0, fighterEntries));
		luceraTemplates.put(41124, new BuffTemplate(41124, false, ADENA_ID, 0, fighterEntries));
		luceraTemplates.put(120, new BuffTemplate(120, false, ADENA_ID, 0, mageEntries));
		luceraTemplates.put(41120, new BuffTemplate(41120, false, ADENA_ID, 0, mageEntries));

		putSingleTemplate(1, 1204, 2);
		putSingleTemplate(2, 1068, 3);
		putSingleTemplate(3, 1040, 3);
		putSingleTemplate(4, 1085, 3);
		putSingleTemplate(5, 1078, 6);
		putSingleTemplate(7, 1204, 2);
		putSingleTemplate(8, 1077, 3);
		putSingleTemplate(10, 1035, 4);
		putSingleTemplate(12, 1062, 2);
		putSingleTemplate(15, 1242, 3);
		putSingleTemplate(16, 1240, 3);
		putSingleTemplate(18, 1045, 6);
		putSingleTemplate(19, 1048, 6);
		putSingleTemplate(20, 1086, 2);
		putSingleTemplate(21, 1036, 2);
		putSingleTemplate(22, 1388, 3);
		putSingleTemplate(23, 1389, 3);
		putSingleTemplate(35, 1397, 3);
		putSingleTemplate(36, 1303, 2);
		putSingleTemplate(40, 1059, 3);
		putSingleTemplate(41, 1268, 4);
		putSingleTemplate(69, 1363, 1);
		putSingleTemplate(70, 1413, 1);
		putSingleTemplate(412, 1068, 3);
		putSingleTemplate(413, 1040, 3);
		putSingleTemplate(414, 1085, 3);
		putSingleTemplate(417, 1204, 2);
		putSingleTemplate(418, 1077, 3);
		putSingleTemplate(4118, 1045, 6);
		putSingleTemplate(4119, 1048, 6);
		putSingleTemplate(4120, 1086, 2);
		putSingleTemplate(4121, 1036, 2);
		putSingleTemplate(4140, 1059, 3);
		putSingleTemplate(4141, 1268, 4);
		putSingleTemplate(4169, 1363, 1);
		putSingleTemplate(4170, 1413, 1);
	}

	private void putSingleTemplate(int menuId, int skillId, int level) {
		luceraTemplates.put(menuId, new BuffTemplate(menuId, false, ADENA_ID, 0, List.of(new BuffSkillEntry(skillId, level))));
	}

	public static int getDefaultSkillLevel(int skillId) {
		return switch (skillId) {
			case 1045, 1048, 1078, 1243, 1311 -> 6;
			case 1035, 1047, 1259, 1268, 1310, 4551, 4552, 4553, 4554 -> 4;
			case 1068, 1040, 1077, 1242, 1240, 1085, 1059, 1388, 1389, 1397, 1087, 1032, 1033, 1044,
					1182, 1189, 1191, 1257, 1304, 1307, 1392, 1393, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
					1249, 1250, 1252, 1253, 1260, 1284, 1308, 1309, 1390, 1391 -> 3;
			case 1086, 1204, 1036, 1303, 1062, 1073, 1251, 1261, 1282 -> 2;
			case 4699, 4700, 4702, 4703 -> 13;
			default -> 1;
		};
	}

	public Optional<BuffTemplate> getLuceraTemplate(int menuId) {
		return Optional.ofNullable(luceraTemplates.get(menuId));
	}

	/**
	 * Retorna a lista de IDs de skills para o esquema solicitado (1, 2, 3 ou 4).
	 */
	public List<Integer> getScheme(int charId, int schemeIdx) {
		String key = "buff_scheme_" + schemeIdx;
		if (variablesService != null) {
			String saved = variablesService.getVariable(charId, key, null);
			if (saved != null && !saved.isBlank()) {
				try {
					String[] parts = saved.split("[,;]");
					List<Integer> list = new ArrayList<>();
					for (String p : parts) {
						if (!p.isBlank()) {
							list.add(Integer.parseInt(p.trim()));
						}
					}
					if (!list.isEmpty()) {
						return list;
					}
				} catch (Exception e) {
					log.warn("Erro ao carregar esquema de buff {}_{}: {}", charId, schemeIdx, e.getMessage());
				}
			}
		}
		if (jdbc != null) {
			try {
				String schemeName = "scheme_" + schemeIdx;
				List<Integer> dbSkills = jdbc.sql("SELECT id FROM mods_buffer_schemes WHERE ownerId = :oId AND scheme = :sch")
						.param("oId", charId)
						.param("sch", schemeName)
						.query((rs, i) -> rs.getInt("id"))
						.list();
				if (!dbSkills.isEmpty()) {
					return dbSkills;
				}
			} catch (Exception ignored) {}
		}
		if (schemeIdx == 1) return new ArrayList<>(DEFAULT_FIGHTER_BUFFS);
		if (schemeIdx == 2) return new ArrayList<>(DEFAULT_MAGE_BUFFS);
		return new ArrayList<>(DEFAULT_FIGHTER_BUFFS);
	}

	public void saveScheme(int charId, int schemeIdx, List<Integer> skillIds) {
		if (skillIds == null || skillIds.isEmpty()) return;
		if (variablesService != null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < skillIds.size(); i++) {
				if (i > 0) sb.append(",");
				sb.append(skillIds.get(i));
			}
			variablesService.setVariable(charId, "buff_scheme_" + schemeIdx, sb.toString());
		}
		if (jdbc != null) {
			try {
				String schemeName = "scheme_" + schemeIdx;
				jdbc.sql("DELETE FROM mods_buffer_schemes WHERE ownerId = :oId AND scheme = :sch")
						.param("oId", charId)
						.param("sch", schemeName)
						.update();
				for (int skId : skillIds) {
					jdbc.sql("INSERT INTO mods_buffer_schemes (ownerId, id, level, scheme) VALUES (:oId, :skId, 1, :sch)")
							.param("oId", charId)
							.param("skId", skId)
							.param("sch", schemeName)
							.update();
				}
			} catch (Exception ex) {
				log.debug("mods_buffer_schemes save: {}", ex.getMessage());
			}
		}
	}

	public boolean addSkillToScheme(int charId, int schemeIdx, int skillId) {
		List<Integer> current = new ArrayList<>(getScheme(charId, schemeIdx));
		if (current.contains(skillId) || current.size() >= MAX_SCHEME_BUFFS) {
			return false;
		}
		current.add(skillId);
		saveScheme(charId, schemeIdx, current);
		return true;
	}

	public boolean removeSkillFromScheme(int charId, int schemeIdx, int skillId) {
		List<Integer> current = new ArrayList<>(getScheme(charId, schemeIdx));
		boolean removed = current.remove(Integer.valueOf(skillId));
		if (removed) {
			if (current.isEmpty()) {
				clearScheme(charId, schemeIdx);
			} else {
				saveScheme(charId, schemeIdx, current);
			}
		}
		return removed;
	}

	public void clearScheme(int charId, int schemeIdx) {
		if (variablesService != null) {
			variablesService.deleteVariable(charId, "buff_scheme_" + schemeIdx);
		}
		if (jdbc != null) {
			try {
				String schemeName = "scheme_" + schemeIdx;
				jdbc.sql("DELETE FROM mods_buffer_schemes WHERE ownerId = :oId AND scheme = :sch")
						.param("oId", charId)
						.param("sch", schemeName)
						.update();
			} catch (Exception ex) {
				log.debug("mods_buffer_schemes clear: {}", ex.getMessage());
			}
		}
	}

	/**
	 * Aplica cura completa de HP, MP e CP (com custo em Adena acima do nivel 40).
	 */
	public boolean heal(PlayerCharacter player, GameSession session) {
		return heal(player, session, null, true);
	}

	/**
	 * Aplica cura completa de HP, MP e CP ao personagem e seu Summon/Pet.
	 */
	public boolean heal(PlayerCharacter player, GameSession session, NpcInstance npc, boolean chargeAdena) {
		if (player == null || player.isDead() || player.isAlikeDead()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode ser curado enquanto estiver morto!"));
			}
			return false;
		}
		if (player.isInCombat()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode utilizar o buffer em combate!"));
			}
			return false;
		}
		if (player.isOlympiadMode()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Buffer desativado nas Olimpiadas!"));
			}
			return false;
		}

		if (chargeAdena && player.level() > 40 && HEAL_COST_ADENA > 0) {
			if (!consumeAdena(player, HEAL_COST_ADENA)) {
				if (session != null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Adena insuficiente para cura completa (" + HEAL_COST_ADENA + " Adena)!"));
				}
				return false;
			}
		}

		player.currentHp(player.maxHp());
		player.currentMp(player.maxMp());
		player.currentCp(player.maxCp());

		SummonInstance summon = player.activeSummon();
		if (summon != null && !summon.isDead()) {
			summon.currentHp(summon.maxHp());
			summon.currentMp(summon.maxMp());
		}

		if (session != null) {
			session.sendVitals();
			session.send(new StatusUpdate(player.objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) player.currentHp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) player.currentMp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) player.currentCp())
			)));
			int npcObjId = npc != null ? npc.objectId() : player.objectId();
			int cx = npc != null ? npc.x() : player.x();
			int cy = npc != null ? npc.y() : player.y();
			int cz = npc != null ? npc.z() : player.z();
			session.send(new MagicSkillUse(npcObjId, player.objectId(), 1012, 1, 0, 0,
					cx, cy, cz, player.x(), player.y(), player.z()));
			session.send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "HP, MP e CP totalmente recuperados!"));
		}
		return true;
	}

	/**
	 * Cancela e remove todos os buffs ativos do personagem.
	 */
	public void cancelBuffs(PlayerCharacter player, GameSession session) {
		cancelBuffs(player, session, null);
	}

	public void cancelBuffs(PlayerCharacter player, GameSession session, NpcInstance npc) {
		if (player == null || player.isDead()) return;
		if (player.isInCombat()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode cancelar buffs em combate!"));
			}
			return;
		}
		player.effects().clear();
		if (session != null) {
			int npcObjId = npc != null ? npc.objectId() : player.objectId();
			int cx = npc != null ? npc.x() : player.x();
			int cy = npc != null ? npc.y() : player.y();
			int cz = npc != null ? npc.z() : player.z();
			session.send(new MagicSkillUse(npcObjId, player.objectId(), 1056, 1, 0, 0,
					cx, cy, cz, player.x(), player.y(), player.z()));
			session.refreshBuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
			session.send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "Todos os seus buffs foram cancelados!"));
		}
	}

	public boolean applySingleBuff(PlayerCharacter player, GameSession session, int skillId, int skillLevel, boolean targetPet) {
		return applySingleBuff(player, session, null, skillId, skillLevel, targetPet);
	}

	/**
	 * Aplica um unico buff ao personagem (e opcionalmente ao mascote/summon), com animacao visual.
	 */
	public boolean applySingleBuff(PlayerCharacter player, GameSession session, NpcInstance npc, int skillId, int skillLevel, boolean targetPet) {
		if (!validateBufferState(player, session)) {
			return false;
		}
		int lvl = skillLevel > 0 ? skillLevel : getDefaultSkillLevel(skillId);
		applyBuffWithLevel(player, session, skillId, lvl, targetPet);

		if (session != null) {
			int npcObjId = npc != null ? npc.objectId() : player.objectId();
			int cx = npc != null ? npc.x() : player.x();
			int cy = npc != null ? npc.y() : player.y();
			int cz = npc != null ? npc.z() : player.z();
			int targetObjId = (targetPet && player.activeSummon() != null) ? player.activeSummon().objectId() : player.objectId();
			var anim = new MagicSkillUse(npcObjId, targetObjId, skillId, lvl, 350, 0,
					cx, cy, cz, player.x(), player.y(), player.z());
			session.send(anim);
			if (session.context() != null && session.context().world() != null) {
				session.context().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, anim, false);
			}
			session.refreshBuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
		}
		return true;
	}

	public boolean isRecordingProfile(int charId) {
		return recordingProfiles.containsKey(charId);
	}

	/**
	 * Aplica o esquema de buff ao personagem ou summon.
	 */
	public boolean applyScheme(PlayerCharacter player, GameSession session, int schemeIdx, boolean targetPet) {
		if (!validateBufferState(player, session)) {
			return false;
		}

		if (player.level() > 40 && SCHEME_COST_ADENA > 0) {
			if (!consumeAdena(player, SCHEME_COST_ADENA)) {
				if (session != null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Adena insuficiente (" + SCHEME_COST_ADENA + " Adena requerida)!"));
				}
				return false;
			}
		}

		List<Integer> buffs = getScheme(player.objectId(), schemeIdx);
		for (int skillId : buffs) {
			applyBuff(player, session, skillId, targetPet);
		}

		if (session != null) {
			session.refreshBuffs();
			session.saveBuffs();
			session.sendMagicEffectIcons();
			String targetLabel = targetPet ? " (Mascote/Summon)" : "";
			session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Esquema " + schemeIdx + targetLabel + " aplicado com sucesso!"));
		}
		return true;
	}

	/**
	 * Processa os comandos do Buffer estilo Lucera:
	 * {@code scripts_services.Buffer:act} e {@code scripts_services.Buffer:actBBS}.
	 */
	public boolean handleLuceraBufferAct(PlayerCharacter player, GameSession session, NpcInstance npc, String rawArgs, boolean isBbs) {
		if (player == null) {
			return false;
		}

		int ask = 0;
		boolean hasAsk = false;
		String reply = isBbs ? "buff" : "0";

		if (rawArgs != null && !rawArgs.isBlank()) {
			String cleaned = rawArgs.trim();
			String[] tokens = cleaned.split("[&\\s]+");
			for (String token : tokens) {
				int eq = token.indexOf('=');
				if (eq > 0) {
					String k = token.substring(0, eq).trim().toLowerCase(Locale.ROOT);
					String v = token.substring(eq + 1).trim();
					if ("ask".equals(k)) {
						try {
							ask = Integer.parseInt(v);
							hasAsk = true;
						} catch (NumberFormatException ignored) {}
					} else if ("reply".equals(k) && !v.isEmpty()) {
						reply = v;
					}
				}
			}
		}

		if (hasAsk) {
			executeLuceraAsk(player, session, npc, ask);
		}

		if (session != null && session.context() != null && session.context().htmls() != null) {
			var htmls = session.context().htmls();
			if (isBbs) {
				String bbsPath = "mods/buffer/community_buffer/" + reply + ".htm";
				String raw = htmls.getHtml(bbsPath);
				if (raw == null) {
					raw = htmls.getHtml("mods/buffer/community_buffer/buff.htm");
				}
				if (raw != null) {
					String rendered = htmls.render(raw, 0, "Community Buffer", player.name());
					session.send(new ShowBoard(rendered));
					return true;
				}
			} else {
				String pagePath;
				if (("0".equals(reply) || reply.isBlank()) && npc != null) {
					String npcSpec = "mods/buffer/npc-" + npc.npcId() + ".htm";
					pagePath = htmls.getHtml(npcSpec) != null ? npcSpec : "mods/buffer/0.htm";
				} else {
					pagePath = "mods/buffer/" + reply + ".htm";
				}
				String raw = htmls.getHtml(pagePath);
				if (raw == null) {
					raw = htmls.getHtml("mods/buffer/0.htm");
				}
				if (raw != null) {
					int npcObjId = npc != null ? npc.objectId() : 0;
					String npcName = npc != null ? npc.name() : "Support Magician";
					String rendered = htmls.render(raw, npcObjId, npcName, player.name());
					session.send(new NpcHtmlMessage(npcObjId, rendered));
					return true;
				}
			}
		}
		return true;
	}

	private void executeLuceraAsk(PlayerCharacter player, GameSession session, NpcInstance npc, int ask) {
		if (!validateBufferState(player, session)) {
			return;
		}
		if (ask == -100) {
			recordingProfiles.put(player.objectId(), new ArrayList<>());
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Gravacao de perfil iniciada! Selecione os buffs desejados e clique em Save Record."));
			}
			return;
		}
		if (ask <= -2000 && ask >= -2009) {
			int profIdx = Math.abs(ask) - 2000; // 0..3
			List<Integer> recorded = recordingProfiles.remove(player.objectId());
			if (recorded != null && !recorded.isEmpty()) {
				String joined = String.join(";", recorded.stream().map(String::valueOf).toList()) + ";";
				if (variablesService != null) {
					variablesService.setVariable(player.objectId(), "BuffProf-" + profIdx, joined);
				}
				// Tambem converte em skillIds e salva em buff_scheme_(profIdx + 1)
				List<Integer> skillIds = new ArrayList<>();
				for (int mId : recorded) {
					BuffTemplate tpl = luceraTemplates.get(mId);
					if (tpl != null) {
						for (BuffSkillEntry se : tpl.skills()) {
							if (!skillIds.contains(se.skillId())) {
								skillIds.add(se.skillId());
							}
						}
					}
				}
				if (!skillIds.isEmpty()) {
					saveScheme(player.objectId(), profIdx + 1, skillIds);
				}
				if (session != null) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Perfil " + (profIdx + 1) + " salvo com sucesso (" + recorded.size() + " buffs)!"));
				}
			} else if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Nenhum buff gravado! Clique em Start Record antes de selecionar os buffs."));
			}
			return;
		}
		if (ask <= -1000 && ask >= -1009) {
			int profIdx = Math.abs(ask) - 1000; // 0..3
			String savedProf = variablesService != null
					? variablesService.getVariable(player.objectId(), "BuffProf-" + profIdx, null)
					: null;
			if (savedProf != null && !savedProf.isBlank()) {
				String[] parts = savedProf.split(";");
				for (String p : parts) {
					if (!p.isBlank()) {
						String token = p.trim();
						try {
							int dash = token.indexOf('-');
							if (dash > 0) {
								int skId = Integer.parseInt(token.substring(0, dash).trim());
								int skLvl = Integer.parseInt(token.substring(dash + 1).trim());
								applyBuffWithLevel(player, session, skId, skLvl, false);
							} else {
								applyLuceraMenuId(player, session, npc, Integer.parseInt(token), false);
							}
						} catch (NumberFormatException ignored) {}
					}
				}
				if (session != null) {
					session.refreshBuffs();
					session.saveBuffs();
					session.sendMagicEffectIcons();
					session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Perfil " + (profIdx + 1) + " aplicado com sucesso!"));
				}
			} else {
				// Fallback para os esquemas padrao caso o perfil ainda nao tenha sido gravado
				int fallbackScheme = (profIdx == 1) ? 2 : (profIdx + 1);
				applyScheme(player, session, fallbackScheme, false);
			}
			return;
		}
		if (ask == -2) {
			cancelBuffs(player, session, npc);
			return;
		}
		if (ask == -3) {
			heal(player, session, npc, false);
			return;
		}
		if (ask == -4) {
			cancelBuffs(player, session, npc);
			return;
		}
		if (ask > 0) {
			List<Integer> rec = recordingProfiles.get(player.objectId());
			if (rec != null && rec.size() < MAX_SCHEME_BUFFS) {
				rec.add(ask);
			}
			applyLuceraMenuId(player, session, npc, ask, true);
		}
	}

	private void applyLuceraMenuId(PlayerCharacter player, GameSession session, NpcInstance npc, int menuId, boolean sendUpdates) {
		BuffTemplate tpl = luceraTemplates.get(menuId);
		if (tpl != null) {
			if (tpl.consumeAmount() > 0 && player.level() > 40) {
				if (player.inventory() == null || !player.inventory().destroyItemByItemId(tpl.consumeItemId(), (int) tpl.consumeAmount())) {
					if (session != null) {
						session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Itens insuficientes para receber este buff!"));
					}
					return;
				}
			}
			int lastSkillId = 0;
			int lastSkillLvl = 1;
			for (BuffSkillEntry entry : tpl.skills()) {
				applyBuffWithLevel(player, session, entry.skillId(), entry.skillLevel(), tpl.targetPet());
				lastSkillId = entry.skillId();
				lastSkillLvl = entry.skillLevel();
			}
			if (sendUpdates && session != null) {
				if (lastSkillId > 0) {
					int npcObjId = npc != null ? npc.objectId() : player.objectId();
					int cx = npc != null ? npc.x() : player.x();
					int cy = npc != null ? npc.y() : player.y();
					int cz = npc != null ? npc.z() : player.z();
					var anim = new MagicSkillUse(npcObjId, player.objectId(), lastSkillId, lastSkillLvl, 300, 0,
							cx, cy, cz, player.x(), player.y(), player.z());
					session.send(anim);
				}
				session.refreshBuffs();
				session.saveBuffs();
				session.sendMagicEffectIcons();
			}
		} else {
			// Fallback caso o menuId seja diretamente um skillId
			applySingleBuff(player, session, npc, menuId, getDefaultSkillLevel(menuId), false);
		}
	}

	private boolean validateBufferState(PlayerCharacter player, GameSession session) {
		if (player == null || player.isDead() || player.isAlikeDead()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Nao e possivel receber buffs neste estado."));
			}
			return false;
		}
		if (player.isInCombat()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Voce nao pode receber buffs em combate!"));
			}
			return false;
		}
		if (player.isOlympiadMode()) {
			if (session != null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Buffer", "Buffer desativado nas Olimpiadas!"));
			}
			return false;
		}
		return true;
	}

	private void applyBuff(PlayerCharacter player, GameSession session, int skillId, boolean targetPet) {
		int maxLvl = (skillService != null && skillService.table() != null)
				? skillService.table().maxLevel(skillId)
				: getDefaultSkillLevel(skillId);
		applyBuffWithLevel(player, session, skillId, maxLvl > 0 ? maxLvl : getDefaultSkillLevel(skillId), targetPet);
	}

	private void applyBuffWithLevel(PlayerCharacter player, GameSession session, int skillId, int skillLevel, boolean targetPet) {
		if (targetPet && player.activeSummon() != null && !player.activeSummon().isDead()) {
			applyPetStatBoost(player.activeSummon(), skillId, skillLevel);
		}
		if (skillService != null) {
			int lvl = skillLevel > 0 ? skillLevel : (skillService.table() != null ? skillService.table().maxLevel(skillId) : 1);
			var opt = skillService.skill(skillId, lvl > 0 ? lvl : 1);
			if (opt.isEmpty() && skillService.table() != null) {
				opt = skillService.table().get(skillId, lvl > 0 ? lvl : 1);
			}
			if (opt.isPresent()) {
				SkillTemplate sk = opt.get();
				if (session != null) {
					session.applySkillEffects(sk, false);
				}
				boolean hasBuff = false;
				for (var b : player.effects().active()) {
					if (b.skillId() == sk.id()) {
						hasBuff = true;
						break;
					}
				}
				if (!hasBuff) {
					long duration = resolveBuffDurationMs(sk.id());
					for (var e : sk.effects()) {
						String stack = e.stackType() == null || e.stackType().equalsIgnoreCase("none")
								? "skill_" + sk.id() + "_" + e.name()
								: e.stackType();
						long end = System.currentTimeMillis() + duration;
						player.effects().put(PlayerEffects.ActiveBuff.ofSkill(sk.id(), sk.level(), stack, end, e.funcs()));
					}
					if (!player.effects().hasSkill(sk.id())) {
						player.effects().addBuff(sk.id(), sk.level(), duration);
					}
				}
				return;
			}
		}
		var consumable = ConsumableTable.get(skillId);
		if (consumable.isPresent() && session != null) {
			session.useConsumable(consumable.get());
		} else {
			player.effects().addBuff(skillId, Math.max(1, skillLevel), resolveBuffDurationMs(skillId));
		}
	}

	private static long resolveBuffDurationMs(int skillId) {
		long duration = 1_200_000L; // 20 minutos padrao
		if (Config.ENABLE_MODIFY_SKILL_DURATION
				&& Config.SKILL_DURATION_LIST != null
				&& Config.SKILL_DURATION_LIST.containsKey(skillId)) {
			duration = Config.SKILL_DURATION_LIST.get(skillId) * 1000L;
		}
		return duration;
	}

	private void applyPetStatBoost(SummonInstance summon, int skillId, int skillLevel) {
		if (skillService != null) {
			int lvl = skillLevel > 0 ? skillLevel : (skillService.table() != null ? skillService.table().maxLevel(skillId) : 1);
			var opt = skillService.skill(skillId, lvl > 0 ? lvl : 1);
			if (opt.isEmpty() && skillService.table() != null) {
				opt = skillService.table().get(skillId, lvl > 0 ? lvl : 1);
			}
			if (opt.isPresent()) {
				for (var eff : opt.get().effects()) {
					for (var f : eff.funcs()) {
						switch (f.stat()) {
							case "pAtk" -> summon.pAtk((int) Math.max(1, Math.round(f.apply(summon.pAtk()))));
							case "pDef" -> summon.pDef((int) Math.max(1, Math.round(f.apply(summon.pDef()))));
							case "mAtk" -> summon.mAtk((int) Math.max(1, Math.round(f.apply(summon.mAtk()))));
							case "mDef" -> summon.mDef((int) Math.max(1, Math.round(f.apply(summon.mDef()))));
							case "pAtkSpd" -> summon.pAtkSpd((int) Math.max(100, Math.round(f.apply(summon.pAtkSpd()))));
							case "mAtkSpd" -> summon.mAtkSpd((int) Math.max(100, Math.round(f.apply(summon.mAtkSpd()))));
							case "runSpd" -> summon.runSpd((int) Math.max(20, Math.round(f.apply(summon.runSpd()))));
							case "maxHp" -> {
								summon.maxHp((int) Math.max(100, Math.round(f.apply(summon.maxHp()))));
								summon.currentHp(summon.maxHp());
							}
							case "maxMp" -> {
								summon.maxMp((int) Math.max(50, Math.round(f.apply(summon.maxMp()))));
								summon.currentMp(summon.maxMp());
							}
							default -> {}
						}
					}
				}
				return;
			}
		}
		switch (skillId) {
			case 1068, 1388, 274 -> summon.pAtk((int) (summon.pAtk() * 1.15));
			case 1040, 1389, 264 -> summon.pDef((int) (summon.pDef() * 1.15));
			case 1036, 269 -> summon.mDef((int) (summon.mDef() * 1.30));
			case 1059, 273 -> summon.mAtk((int) (summon.mAtk() * 1.75));
			case 1086, 275 -> summon.pAtkSpd((int) (summon.pAtkSpd() * 1.33));
			case 1085, 276 -> summon.mAtkSpd((int) (summon.mAtkSpd() * 1.30));
			case 1204, 268 -> summon.runSpd(summon.runSpd() + 33);
			case 1045, 267, 1363 -> {
				summon.maxHp((int) (summon.maxHp() * 1.35));
				summon.currentHp(summon.maxHp());
			}
			case 1048 -> {
				summon.maxMp((int) (summon.maxMp() * 1.35));
				summon.currentMp(summon.maxMp());
			}
		}
	}

	private boolean consumeAdena(PlayerCharacter player, long amount) {
		if (player == null || player.inventory() == null) return false;
		return player.inventory().destroyItemByItemId(ADENA_ID, (int) amount);
	}

	public String renderSchemeEditHtml(PlayerCharacter player, int schemeIdx) {
		return renderSchemeEditHtml(player, 0, schemeIdx);
	}

	/**
	 * Renderiza a janela de edicao de esquemas (Scheme Editor) para o NPC ou Voiced Buffer.
	 */
	public String renderSchemeEditHtml(PlayerCharacter player, int npcObjId, int schemeIdx) {
		int idx = Math.max(1, Math.min(4, schemeIdx));
		List<Integer> activeSkills = getScheme(player != null ? player.objectId() : 0, idx);
		String prefix = npcObjId > 0 ? "npc_" + npcObjId + "_" : "voiced_buffer ";

		StringBuilder sb = new StringBuilder();
		sb.append("<html><title>Gerenciar Esquema #").append(idx).append("</title><body>");
		sb.append("<center><font color=\"LEVEL\"><b>Editor de Esquema #").append(idx).append(" (")
				.append(activeSkills.size()).append("/").append(MAX_SCHEME_BUFFS).append(")</b></font></center><br>");

		sb.append("<table width=270 bgcolor=111111 cellpadding=2 cellspacing=0><tr>");
		for (int s = 1; s <= 3; s++) {
			sb.append("<td align=center><button value=\"Esquema ").append(s).append("\" action=\"bypass -h ")
					.append(prefix).append("editschemes ").append(s)
					.append("\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		}
		sb.append("</tr></table><br>");

		sb.append("<table width=270 cellpadding=2 cellspacing=0>");
		int col = 0;
		for (var entry : AVAILABLE_BUFF_NAMES.entrySet()) {
			int skId = entry.getKey();
			String skName = entry.getValue();
			boolean selected = activeSkills.contains(skId);
			if (col % 2 == 0) sb.append("<tr>");
			String actionCmd = selected
					? prefix + "skillunselect " + idx + " " + skId
					: prefix + "skillselect " + idx + " " + skId;
			String color = selected ? "00FF00" : "AAAAAA";
			String mark = selected ? "[x] " : "[ ] ";
			sb.append("<td width=135><a action=\"bypass -h ").append(actionCmd).append("\"><font color=\"")
					.append(color).append("\">").append(mark).append(skName).append("</font></a></td>");
			if (col % 2 == 1) sb.append("</tr>");
			col++;
		}
		if (col % 2 == 1) sb.append("<td></td></tr>");
		sb.append("</table><br>");

		sb.append("<center>");
		sb.append("<button value=\"Aplicar Esquema #").append(idx).append("\" action=\"bypass -h ")
				.append(prefix).append("support ").append(idx)
				.append("\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"><br>");
		if (npcObjId > 0) {
			sb.append("<button value=\"Voltar\" action=\"bypass -h npc_").append(npcObjId)
					.append("_Chat 0\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\">");
		} else {
			sb.append("<button value=\"Voltar\" action=\"bypass -h voiced_buffer\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\">");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	/**
	 * Renderiza dialogo HTML completo do Buffer (.buffer / voiced_buffer).
	 */
	public String renderHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=260 cellpadding=2 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\"><b>BUFFER DE ESQUEMAS</b></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">Selecione um esquema ou recupere seus status.</font></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td align=center colspan=2><font color=\"8FA888\"><b>Esquemas Salvos (Personagem)</b></font></td></tr>");
		sb.append("<tr><td align=center><button value=\"Esquema 1 (Fighter)\" action=\"bypass -h voiced_buffer 1\" width=114 height=30 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
		sb.append("<td align=center><button value=\"Esquema 2 (Mage)\" action=\"bypass -h voiced_buffer 2\" width=114 height=30 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
		sb.append("<tr><td align=center><button value=\"Esquema 3 (Custom)\" action=\"bypass -h voiced_buffer 3\" width=114 height=30 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td>");
		sb.append("<td align=center><button value=\"Editar Esquemas\" action=\"bypass -h voiced_buffer editschemes 3\" width=114 height=30 back=\"L2UI_CH3.bigbutton2_down\" fore=\"L2UI_CH3.bigbutton2\"></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td align=center colspan=2><font color=\"95B8D1\"><b>Buffar Mascote / Summon</b></font></td></tr>");
		sb.append("<tr><td align=center><button value=\"Pet Fighter\" action=\"bypass -h voiced_buffer pet_1\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("<td align=center><button value=\"Pet Mage\" action=\"bypass -h voiced_buffer pet_2\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td align=center colspan=2><font color=\"C8B588\"><b>Opcoes Utilitarias</b></font></td></tr>");
		sb.append("<tr><td align=center><button value=\"Restaurar HP/MP/CP\" action=\"bypass -h voiced_buffer heal\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
		sb.append("<td align=center><button value=\"Cancelar Buffs\" action=\"bypass -h voiced_buffer cancel\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260>");
		sb.append("<tr><td align=center><font color=\"888888\">Custo por esquema: 25.000 Adena (Gratis ate nv 40)</font></td></tr>");
		sb.append("</table>");
		sb.append("</body></html>");
		return sb.toString();
	}
}
