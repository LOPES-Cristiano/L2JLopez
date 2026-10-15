package com.lopez.l2j.game.skill;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillTreeTable.SkillLearn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Regras de skills do jogador: carregar/salvar, passivas, recompensas por nivel (rewardSkills/AutoLearnSkills
 * do L2JDream) e aprendizado com treinador (RequestAquireSkill).
 */
@Service
public class SkillService {

	private static final Logger log = LoggerFactory.getLogger(SkillService.class);

	/** Lucky: sem penalidade de morte ate o nivel 9 (removido no nivel 10, como no L2J). */
	public static final int SKILL_LUCKY = 194;

	private final SkillTable table;
	private final SkillTreeTable trees;
	private final SkillRepository repository;
	private final boolean autoLearn;
	private final int autoLearnMaxLevel;
	private final boolean spBookNeeded;

	@org.springframework.beans.factory.annotation.Autowired
	public SkillService(SkillTable table, SkillTreeTable trees, SkillRepository repository,
			@Value("${l2.skills.auto-learn:true}") boolean autoLearn,
			@Value("${l2.skills.auto-learn-max-level:0}") int autoLearnMaxLevel,
			@Value("${l2.skills.sp-book-needed:true}") boolean spBookNeeded) {
		this.table = table;
		this.trees = trees;
		this.repository = repository;
		this.autoLearn = autoLearn;
		this.autoLearnMaxLevel = autoLearnMaxLevel;
		this.spBookNeeded = spBookNeeded;
	}

	public SkillService(SkillTable table, SkillTreeTable trees, SkillRepository repository, boolean autoLearn) {
		this(table, trees, repository, autoLearn, 0, true);
	}


	public SkillTable table() {
		return table;
	}

	public SkillTreeTable trees() {
		return trees;
	}

	public Optional<SkillTemplate> skill(int skillId, int level) {
		return table != null ? table.get(skillId, level) : Optional.empty();
	}

	public Optional<SkillTemplate> known(PlayerCharacter p, int skillId) {
		int level = p.skillLevel(skillId);
		return level <= 0 ? Optional.empty() : table.get(skillId, level);
	}

	/** Carrega os skills salvos e recalcula as passivas. */
	public void load(PlayerCharacter p) {
		p.skills().clear();
		if (repository != null) {
			for (Skill s : repository.findByCharId(p.objectId(), p.classIndex())) {
				if (s.level() > 0) {
					p.skills().put(s.id(), s.level());
				}
			}
		}
		if (Config.CHECK_SKILLS_ON_ENTER || !Config.ALT_SUBCLASS_SKILLS) {
			cleanInvalidSkills(p);
		}
		refreshPassives(p);
	}

	/** Junta as funcoes de stat de todas as passivas conhecidas (respeitando DISABLE_SKILLS_ON_LEVEL_LOST). */
	public void refreshPassives(PlayerCharacter p) {
		List<StatFunc> funcs = new ArrayList<>();
		for (var e : p.skills().entrySet()) {
			if (isSkillDisabledByLevelLoss(p, e.getKey(), e.getValue())) {
				continue;
			}
			table.get(e.getKey(), e.getValue()).filter(SkillTemplate::isPassive).ifPresent(s -> funcs.addAll(s.funcs()));
		}
		p.passiveFuncs(funcs);
	}

	/**
	 * Verifica se um skill deve ser desativado devido a perda de nivel (DISABLE_SKILLS_ON_LEVEL_LOST).
	 */
	public boolean isSkillDisabledByLevelLoss(PlayerCharacter p, int skillId, int skillLevel) {
		if (!Config.DISABLE_SKILLS_ON_LEVEL_LOST || p == null || trees == null) {
			return false;
		}
		int reqLvl = 0;
		for (SkillLearn sl : trees.allFor(p.classId())) {
			if (sl.id() == skillId && sl.level() == skillLevel) {
				reqLvl = sl.minLevel();
				break;
			}
		}
		return reqLvl > 0 && (reqLvl - p.level()) >= 9;
	}

	/**
	 * Calcula o consumo de MP adicional de Dances/Songs (ALT_DANCE_MP_CONSUME).
	 */
	public static int calculateEffectiveSkillMpConsume(SkillTemplate skill, int activeDanceSongCount) {
		if (skill == null) {
			return 0;
		}
		int baseMp = skill.mpInitialConsume() + skill.mpConsume();
		boolean isDanceOrSong = (skill.id() >= 264 && skill.id() <= 311)
				|| (skill.id() >= 349 && skill.id() <= 367);
		if (isDanceOrSong && !Config.ALT_DANCE_MP_CONSUME) {
			return baseMp;
		}
		if (isDanceOrSong && activeDanceSongCount > 0) {
			return baseMp + (activeDanceSongCount * 30);
		}
		return baseMp;
	}

	/** Lista para o pacote SkillList (ordenada por id), filtrando NOTDONE se SEND_NOT_DONE_SKILLS=false. */
	public List<Skill> skillList(PlayerCharacter p) {
		List<Skill> out = new ArrayList<>();
		for (var e : p.skills().entrySet()) {
			var t = table.get(e.getKey(), e.getValue());
			boolean hideNotDone = Config.SEND_NOT_DONE_SKILLS <= 0
					|| (Config.SEND_NOT_DONE_SKILLS == 1 && p.accessLevel() <= 0);
			if (hideNotDone && t.isPresent() && "NOTDONE".equalsIgnoreCase(t.get().skillType())) {
				continue;
			}
			out.add(new Skill(e.getKey(), e.getValue(), t.map(SkillTemplate::name).orElse(""),
					t.map(SkillTemplate::isPassive).orElse(false)));
		}
		out.sort(Comparator.comparingInt(Skill::id));
		return out;
	}

	/**
	 * Recompensas de nivel: skills iniciais (personagem novo), Lucky, entradas gratuitas (Expertise, Common Craft),
	 * Divine Inspiration (se AUTO_LEARN_DIVINE_INSPIRATION=true) e, com AutoLearnSkills, todos os skills liberados para o nivel.
	 *
	 * @return true se algum skill mudou
	 */
	public boolean rewardSkills(PlayerCharacter p) {
		boolean changed = false;
		if (p.skills().isEmpty()) {
			for (SkillLearn s : trees.available(p.classId(), p.level(), Map.of())) {
				changed |= grant(p, s.id(), s.level());
			}
			if (p.level() < 10) {
				changed |= grant(p, SKILL_LUCKY, 1);
			}
		}
		if (p.level() > 9 && p.skills().containsKey(SKILL_LUCKY)) {
			removeSkill(p, SKILL_LUCKY);
			changed = true;
		}
		if (Config.AUTO_LEARN_DIVINE_INSPIRATION) {
			int diLevel = 0;
			if (p.level() >= 78) diLevel = 4;
			else if (p.level() >= 76) diLevel = 3;
			else if (p.level() >= 61) diLevel = 2;
			else if (p.level() >= 52) diLevel = 1;
			if (diLevel > 0) {
				changed |= grant(p, 1405, diLevel);
			}
		}
		boolean auto = autoLearn && Config.AUTO_LEARN_SKILLS && (autoLearnMaxLevel <= 0 || p.level() <= autoLearnMaxLevel);
		for (var e : trees.unlocked(p.classId(), p.level(), !auto).entrySet()) {
			changed |= grant(p, e.getKey(), e.getValue());
		}
		if (changed) {
			refreshPassives(p);
		}
		return changed;
	}

	/** Skills que o treinador pode ensinar agora (respeitando ALT_GAME_SKILL_LEARN). */
	public List<SkillLearn> available(PlayerCharacter p) {
		return available(p, p.classId());
	}

	public List<SkillLearn> available(PlayerCharacter p, int npcTeachClassId) {
		int targetClassId = (Config.ALT_GAME_SKILL_LEARN && npcTeachClassId >= 0) ? npcTeachClassId : p.classId();
		return trees.available(targetClassId, p.level(), p.skills());
	}

	public int minLevelForNewSkill(PlayerCharacter p) {
		return trees.minLevelForNewSkill(p.classId(), p.level());
	}

	/** Entrada da arvore se o skill/nivel puder ser aprendido agora. */
	public Optional<SkillLearn> learnable(PlayerCharacter p, int id, int level) {
		return available(p).stream().filter(s -> s.id() == id && s.level() == level).findFirst();
	}

	/**
	 * Livro exigido (SpBookNeeded: so para o nivel 1; DivineInspirationSpBookNeeded para skill 1405;
	 * LifeCrystalNeeded para skills de cla), ou -1.
	 */
	public int requiredBook(int id, int level) {
		if (id == 1405) {
			return Config.DIVINE_INSPIRATION_SP_BOOK_NEEDED ? trees.spellbookFor(id) : -1;
		}
		if (id >= 370 && id <= 391) {
			return Config.LIFE_CRYSTAL_NEEDED ? trees.spellbookFor(id) : -1;
		}
		boolean needBook = Config.SP_BOOK_NEEDED && spBookNeeded;
		return needBook && level == 1 ? trees.spellbookFor(id) : -1;
	}

	/** Grava o skill no nivel informado (sem checar custo) e atualiza as passivas. */
	public void addSkill(PlayerCharacter p, int id, int level) {
		if (grant(p, id, level)) {
			refreshPassives(p);
		}
	}

	/**
	 * Define o skill exatamente no nivel informado (usado para enchant e downgrade na falha).
	 */
	public void setSkill(PlayerCharacter p, int id, int level) {
		var t = table.get(id, level);
		if (t.isEmpty()) {
			log.debug("Skill {} nivel {} nao existe no datapack; ignorado", id, level);
			return;
		}
		p.skills().put(id, level);
		if (repository != null) {
			repository.save(p.objectId(), p.classIndex(), new Skill(id, level, t.get().name(), t.get().isPassive()));
		}
		refreshPassives(p);
	}

	public void removeSkill(PlayerCharacter p, int id) {
		p.skills().remove(id);
		if (repository != null) {
			repository.delete(p.objectId(), p.classIndex(), id);
		}
	}

	private boolean grant(PlayerCharacter p, int id, int level) {
		if (p.skillLevel(id) >= level) {
			return false;
		}
		var t = table.get(id, level);
		if (t.isEmpty()) {
			log.debug("Skill {} nivel {} nao existe no datapack; ignorado", id, level);
			return false;
		}
		p.skills().put(id, level);
		if (repository != null) {
			repository.save(p.objectId(), p.classIndex(), new Skill(id, level, t.get().name(), t.get().isPassive()));
		}
		return true;
	}

	/**
	 * Remove habilidades que nao pertencem a arvore da classe atual nem sao habilidades comuns/especiais,
	 * prevenindo o acúmulo indevido de skills entre classes ao trocar/adicionar subclasses.
	 * Respeita CHECK_SKILLS_ON_ENTER, CHECK_ADDITIONAL_SKILLS e ALLOWED_SKILLS.
	 */
	public void cleanInvalidSkills(PlayerCharacter p) {
		if (Config.ALT_SUBCLASS_SKILLS || Config.ALT_GAME_SKILL_LEARN || (p != null && p.isAio())) {
			return;
		}
		Set<Integer> validSkillIds = new HashSet<>();
		for (SkillLearn sl : trees.allFor(p.classId())) {
			validSkillIds.add(sl.id());
		}
		List<Integer> toRemove = new ArrayList<>();
		for (int skillId : p.skills().keySet()) {
			if (validSkillIds.contains(skillId)) {
				continue;
			}
			if (p.armorSetSkillIds().contains(skillId) || p.equippedItemSkills().containsKey(skillId)) {
				continue;
			}
			if (isCommonOrSpecialSkill(skillId) || isAllowedCustomSkill(skillId)) {
				continue;
			}
			if (!Config.CHECK_ADDITIONAL_SKILLS && (skillId == 1405 || (skillId >= 1368 && skillId <= 1373))) {
				continue;
			}
			toRemove.add(skillId);
		}
		for (int skillId : toRemove) {
			p.skills().remove(skillId);
			if (repository != null) {
				repository.delete(p.objectId(), p.classIndex(), skillId);
			}
			log.info("Removido skill incompativel {} do personagem {} (classe {}, slot {})",
					skillId, p.name(), p.classId(), p.classIndex());
		}
		if (!toRemove.isEmpty()) {
			refreshPassives(p);
		}
	}

	public static boolean isAllowedCustomSkill(int skillId) {
		String raw = Config.ALLOWED_SKILLS;
		if (raw == null || raw.isBlank() || "0".equals(raw.trim())) {
			return false;
		}
		for (String part : raw.split(",")) {
			try {
				if (Integer.parseInt(part.trim()) == skillId) {
					return true;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}

	private boolean isCommonOrSpecialSkill(int skillId) {
		// Lucky (194), Expertise (239), Common Craft / Dwarven Craft / Crystallize (1320-1322, 248), Divine Inspiration (1405)
		if (skillId == SKILL_LUCKY || skillId == 239 || skillId == 248 || skillId == 1405 || (skillId >= 1320 && skillId <= 1322)) {
			return true;
		}
		// Seal of Ruler (246, 247)
		if (skillId == 246 || skillId == 247) {
			return true;
		}
		// Fishing & expansion skills (1312-1319, 1368-1373)
		if ((skillId >= 1312 && skillId <= 1319) || (skillId >= 1368 && skillId <= 1373)) {
			return true;
		}
		// Clan skills (370-391)
		if (skillId >= 370 && skillId <= 391) {
			return true;
		}
		// Hero skills (395, 396, 1374-1376)
		if (skillId == 395 || skillId == 396 || (skillId >= 1374 && skillId <= 1376)) {
			return true;
		}
		// Noblesse skills (325-327, 1323)
		if ((skillId >= 325 && skillId <= 327) || skillId == 1323) {
			return true;
		}
		// Item / Augmentation / SA / Armor Set skills (3000-3699)
		if (skillId >= 3000 && skillId <= 3699) {
			return true;
		}
		// GM skills (7029)
		if (skillId == 7029) {
			return true;
		}
		return false;
	}
}

