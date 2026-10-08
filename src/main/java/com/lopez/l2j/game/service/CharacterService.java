package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.CharacterRepository.NewCharacter;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharCreateFailReason;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Regras de criacao/remocao de personagem (porta de CharacterCreate/CharacterDelete do legado). */
@Service
public class CharacterService {

	private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

	public static final int MAX_CHARACTERS_PER_ACCOUNT = 7;
	/** Equivalente ao CnameTemplate padrao do legado. */
	static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9]{3,16}");

	/** Resultado da criacao: ou o personagem, ou o motivo da falha (para o CharCreateFail). */
	public record CreateResult(PlayerCharacter character, CharCreateFailReason failure) {
		static CreateResult fail(CharCreateFailReason reason) {
			return new CreateResult(null, reason);
		}

		public boolean ok() {
			return character != null;
		}
	}

	public record CreateRequest(String account, String name, int race, int sex, int classId, int hairStyle,
			int hairColor, int face) {
	}

	private final CharacterRepository repository;
	private final CharTemplateTable templates;
	private final InventoryService inventories;
	private final Object createLock = new Object();

	public CharacterService(CharacterRepository repository, CharTemplateTable templates,
			InventoryService inventories) {
		this.repository = repository;
		this.templates = templates;
		this.inventories = inventories;
	}

	public List<PlayerCharacter> list(String account) {
		long now = System.currentTimeMillis();
		var characters = repository.findByAccount(account);
		var result = new java.util.ArrayList<PlayerCharacter>();
		for (var c : characters) {
			if (c.deleteTime() > 0 && c.deleteTime() <= now) {
				repository.delete(c.objectId());
				inventories.deleteAll(c.objectId());
				log.info("Personagem expirado e removido: {} da conta {}", c.name(), c.account());
			} else {
				result.add(c);
			}
		}
		return result;
	}

	public CreateResult create(CreateRequest r) {
		if (r.name() == null || r.name().length() < 3 || r.name().length() > 16) {
			return CreateResult.fail(CharCreateFailReason.ENG_CHARS_16);
		}
		if (!VALID_NAME.matcher(r.name()).matches()) {
			return CreateResult.fail(CharCreateFailReason.INCORRECT_NAME);
		}
		if (Config.FORBIDDEN_NAMES != null) {
			String lower = r.name().toLowerCase(java.util.Locale.ROOT);
			for (String forbidden : Config.FORBIDDEN_NAMES) {
				if (!forbidden.isBlank() && lower.contains(forbidden.toLowerCase(java.util.Locale.ROOT))) {
					return CreateResult.fail(CharCreateFailReason.INCORRECT_NAME);
				}
			}
		}
		if (r.face() < 0 || r.face() > 2 || r.hairColor() < 0 || r.hairColor() > 3 || r.hairStyle() < 0
				|| (r.sex() == 0 && r.hairStyle() > 4) || (r.sex() != 0 && r.hairStyle() > 6)
				|| (r.sex() != 0 && r.sex() != 1)) {
			return CreateResult.fail(CharCreateFailReason.CREATION_FAILED);
		}
		CharTemplate t = templates.get(r.classId()).orElse(null);
		if (t == null || !t.isStartingClass() || t.raceId() != r.race()) {
			return CreateResult.fail(CharCreateFailReason.CREATION_FAILED);
		}
		synchronized (createLock) {
			int maxChars = Config.CHAR_MAX_NUMBER > 0 ? Config.CHAR_MAX_NUMBER : MAX_CHARACTERS_PER_ACCOUNT;
			if (repository.findByAccount(r.account()).size() >= maxChars) {
				return CreateResult.fail(CharCreateFailReason.TOO_MANY_CHARACTERS);
			}
			if (repository.nameExists(r.name())) {
				return CreateResult.fail(CharCreateFailReason.NAME_ALREADY_EXISTS);
			}
			try {
				int spawnX = Config.ALT_SPAWN_NEW_CHAR ? Config.ALT_SPAWN_X : t.spawnX();
				int spawnY = Config.ALT_SPAWN_NEW_CHAR ? Config.ALT_SPAWN_Y : t.spawnY();
				int spawnZ = Config.ALT_SPAWN_NEW_CHAR ? Config.ALT_SPAWN_Z : t.spawnZ();

				var created = repository.create(new NewCharacter(r.account(), r.name(), t.raceId(), t.classId(),
						r.sex() == 1, r.face(), r.hairStyle(), r.hairColor(), (int) t.hpBase(), (int) t.mpBase(),
						(int) t.cpBase(), spawnX, spawnY, spawnZ, t.canCraft()));

				if (Config.ENABLE_STARTUP_LVL && Config.STARTUP_LVL > 1) {
					int targetLvl = Math.min(Config.STARTUP_LVL, Config.PLAYER_MAX_LEVEL);
					long targetExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(targetLvl);
					created.level(targetLvl);
					created.exp(targetExp);
					repository.saveState(created, false);
				}

				var inv = inventories.giveStarterItems(created.objectId(), created.classId());
				log.info("Personagem criado: {} ({}) na conta {} com {} itens", created.name(), t.className(),
						r.account(), inv.size());
				return new CreateResult(created, null);
			} catch (DataIntegrityViolationException e) {
				return CreateResult.fail(CharCreateFailReason.NAME_ALREADY_EXISTS);
			}
		}
	}

	/** Remocao imediata ou agendada (DeleteCharAfterDays no legado). */
	public void delete(PlayerCharacter c) {
		int days = Config.DELETE_CHAR_AFTER_DAYS;
		if (days <= 0) {
			repository.delete(c.objectId());
			inventories.deleteAll(c.objectId());
			log.info("Personagem removido: {} da conta {}", c.name(), c.account());
		} else {
			long deleteTime = System.currentTimeMillis() + (days * 86400000L);
			repository.updateDeleteTime(c.objectId(), deleteTime);
			c.deleteTime(deleteTime);
			log.info("Personagem agendado para remocao: {} da conta {} em {} dias", c.name(), c.account(), days);
		}
	}

	public void restore(PlayerCharacter c) {
		repository.updateDeleteTime(c.objectId(), 0);
		c.deleteTime(0);
	}

	public void save(PlayerCharacter c, boolean online) {
		try {
			repository.saveState(c, online);
		} catch (RuntimeException e) {
			log.error("Falha ao salvar {}", c.name(), e);
		}
	}

	public CharTemplate template(PlayerCharacter c) {
		return templates.get(c.classId())
				.or(() -> templates.get(c.baseClassId()))
				.orElseThrow(() -> new IllegalStateException("classe sem template: " + c.classId()));
	}

	public Optional<CharTemplate> template(int classId) {
		return templates.get(classId);
	}

	public List<CharTemplate> creationTemplates() {
		return templates.creationTemplates();
	}

	public void setAccessLevelByName(String charName, int accessLevel) {
		repository.setAccessLevel(charName, accessLevel);
	}

	public List<PlayerCharacter> listAll(int limit) {
		return repository.listAll(limit);
	}

	public List<PlayerCharacter> searchByName(String query, int limit) {
		return repository.searchByName(query, limit);
	}

	public Optional<PlayerCharacter> findByName(String name) {
		return repository.findByName(name);
	}
}
