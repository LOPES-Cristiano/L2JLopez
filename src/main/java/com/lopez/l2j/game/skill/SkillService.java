package com.lopez.l2j.game.skill;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.skill.SkillTreeTable.SkillLearn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	@Autowired
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

	public SkillTable table() {
		return table;
	}

	public SkillTreeTable trees() {
		return trees;
	}

	public Optional<SkillTemplate> known(PlayerCharacter p, int skillId) {
		int level = p.skillLevel(skillId);
		return level <= 0 ? Optional.empty() : table.get(skillId, level);
	}

	/** Carrega os skills salvos e recalcula as passivas. */
	public void load(PlayerCharacter p) {
		p.skills().clear();
		if (repository != null) {
			for (Skill s : repository.findByCharId(p.objectId(), 0)) {
				if (s.level() > 0) {
					p.skills().put(s.id(), s.level());
				}
			}
		}
		refreshPassives(p);
	}

	/** Junta as funcoes de stat de todas as passivas conhecidas. */
	public void refreshPassives(PlayerCharacter p) {
		List<StatFunc> funcs = new ArrayList<>();
		for (var e : p.skills().entrySet()) {
			table.get(e.getKey(), e.getValue()).filter(SkillTemplate::isPassive).ifPresent(s -> funcs.addAll(s.funcs()));
		}
		p.passiveFuncs(funcs);
	}

	/** Lista para o pacote SkillList (ordenada por id). */
	public List<Skill> skillList(PlayerCharacter p) {
		List<Skill> out = new ArrayList<>();
		for (var e : p.skills().entrySet()) {
			var t = table.get(e.getKey(), e.getValue());
			out.add(new Skill(e.getKey(), e.getValue(), t.map(SkillTemplate::name).orElse(""),
					t.map(SkillTemplate::isPassive).orElse(false)));
		}
		out.sort(Comparator.comparingInt(Skill::id));
		return out;
	}

	/**
	 * Recompensas de nivel: skills iniciais (personagem novo), Lucky, entradas gratuitas (Expertise, Common Craft)
	 * e, com AutoLearnSkills, todos os skills liberados para o nivel.
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
		boolean auto = autoLearn && (autoLearnMaxLevel <= 0 || p.level() <= autoLearnMaxLevel);
		for (var e : trees.unlocked(p.classId(), p.level(), !auto).entrySet()) {
			changed |= grant(p, e.getKey(), e.getValue());
		}
		if (changed) {
			refreshPassives(p);
		}
		return changed;
	}

	/** Skills que o treinador pode ensinar agora. */
	public List<SkillLearn> available(PlayerCharacter p) {
		return trees.available(p.classId(), p.level(), p.skills());
	}

	public int minLevelForNewSkill(PlayerCharacter p) {
		return trees.minLevelForNewSkill(p.classId(), p.level());
	}

	/** Entrada da arvore se o skill/nivel puder ser aprendido agora. */
	public Optional<SkillLearn> learnable(PlayerCharacter p, int id, int level) {
		return available(p).stream().filter(s -> s.id() == id && s.level() == level).findFirst();
	}

	/** Livro exigido (SpBookNeeded: so para o nivel 1), ou -1. */
	public int requiredBook(int id, int level) {
		return spBookNeeded && level == 1 ? trees.spellbookFor(id) : -1;
	}

	/** Grava o skill no nivel informado (sem checar custo) e atualiza as passivas. */
	public void addSkill(PlayerCharacter p, int id, int level) {
		if (grant(p, id, level)) {
			refreshPassives(p);
		}
	}

	public void removeSkill(PlayerCharacter p, int id) {
		p.skills().remove(id);
		if (repository != null) {
			repository.delete(p.objectId(), 0, id);
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
			repository.save(p.objectId(), 0, new Skill(id, level, t.get().name(), t.get().isPassive()));
		}
		return true;
	}
}
