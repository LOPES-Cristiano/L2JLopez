package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.CharacterRepository.NewCharacter;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharCreateFailReason;
import java.util.List;
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
	private final Object createLock = new Object();

	public CharacterService(CharacterRepository repository, CharTemplateTable templates) {
		this.repository = repository;
		this.templates = templates;
	}

	public List<PlayerCharacter> list(String account) {
		return repository.findByAccount(account);
	}

	public CreateResult create(CreateRequest r) {
		if (r.name() == null || r.name().length() < 3 || r.name().length() > 16) {
			return CreateResult.fail(CharCreateFailReason.ENG_CHARS_16);
		}
		if (!VALID_NAME.matcher(r.name()).matches()) {
			return CreateResult.fail(CharCreateFailReason.INCORRECT_NAME);
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
			if (repository.findByAccount(r.account()).size() >= MAX_CHARACTERS_PER_ACCOUNT) {
				return CreateResult.fail(CharCreateFailReason.TOO_MANY_CHARACTERS);
			}
			if (repository.nameExists(r.name())) {
				return CreateResult.fail(CharCreateFailReason.NAME_ALREADY_EXISTS);
			}
			try {
				var created = repository.create(new NewCharacter(r.account(), r.name(), t.raceId(), t.classId(),
						r.sex() == 1, r.face(), r.hairStyle(), r.hairColor(), (int) t.hpBase(), (int) t.mpBase(),
						(int) t.cpBase(), t.spawnX(), t.spawnY(), t.spawnZ(), t.canCraft()));
				log.info("Personagem criado: {} ({}) na conta {}", created.name(), t.className(), r.account());
				return new CreateResult(created, null);
			} catch (DataIntegrityViolationException e) {
				return CreateResult.fail(CharCreateFailReason.NAME_ALREADY_EXISTS);
			}
		}
	}

	/** Remocao imediata (DeleteCharAfterDays = 0 no legado). */
	public void delete(PlayerCharacter c) {
		repository.delete(c.objectId());
		log.info("Personagem removido: {} da conta {}", c.name(), c.account());
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

	public List<CharTemplate> creationTemplates() {
		return templates.creationTemplates();
	}
}
