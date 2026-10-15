package com.lopez.l2j.game.combat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico central de balanceamento dinamico via XML (class_balance.xml, matchup_balance.xml, skill_balance.xml).
 * Permite ajustes finos de P.Atk, M.Atk, P.Def, M.Def e multiplicadores de confronto entre classes (matchup)
 * com reload a quente via //reload balance.
 */
@Service
public class BalanceProfileService {

	private static final Logger log = LoggerFactory.getLogger(BalanceProfileService.class);

	public record ClassBalance(double pAtk, double mAtk, double pDef, double mDef) {}

	private final Map<Integer, ClassBalance> classMultipliers = new ConcurrentHashMap<>();
	private final Map<Long, Double> matchupMultipliers = new ConcurrentHashMap<>();
	private final Map<Integer, Double> skillMultipliers = new ConcurrentHashMap<>();

	public BalanceProfileService() {
		reload();
	}

	public synchronized void reload() {
		classMultipliers.clear();
		matchupMultipliers.clear();
		skillMultipliers.clear();
		loadClassBalance();
		loadMatchupBalance();
		loadSkillBalance();
		log.info("BalanceProfileService: Carregados {} classes, {} matchups e {} skills balanceados.",
				classMultipliers.size(), matchupMultipliers.size(), skillMultipliers.size());
	}

	private void loadClassBalance() {
		try {
			File f = new File("data/xml/custom/class_balance.xml");
			if (f.exists()) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				NodeList nodes = doc.getElementsByTagName("class");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element el = (Element) nodes.item(i);
					int classId = Integer.parseInt(el.getAttribute("id"));
					double pAtk = el.hasAttribute("pAtk") ? Double.parseDouble(el.getAttribute("pAtk")) : 1.0;
					double mAtk = el.hasAttribute("mAtk") ? Double.parseDouble(el.getAttribute("mAtk")) : 1.0;
					double pDef = el.hasAttribute("pDef") ? Double.parseDouble(el.getAttribute("pDef")) : 1.0;
					double mDef = el.hasAttribute("mDef") ? Double.parseDouble(el.getAttribute("mDef")) : 1.0;
					classMultipliers.put(classId, new ClassBalance(pAtk, mAtk, pDef, mDef));
				}
			}
		} catch (Exception e) {
			log.warn("BalanceProfileService: Erro ao ler class_balance.xml: {}", e.getMessage());
		}
	}

	private void loadMatchupBalance() {
		try {
			File f = new File("data/xml/custom/matchup_balance.xml");
			if (f.exists()) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				NodeList nodes = doc.getElementsByTagName("matchup");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element el = (Element) nodes.item(i);
					int att = Integer.parseInt(el.getAttribute("attacker"));
					int def = Integer.parseInt(el.getAttribute("target"));
					double mult = Double.parseDouble(el.getAttribute("multiplier"));
					matchupMultipliers.put(matchupKey(att, def), mult);
				}
			}
		} catch (Exception e) {
			log.warn("BalanceProfileService: Erro ao ler matchup_balance.xml: {}", e.getMessage());
		}
	}

	private void loadSkillBalance() {
		try {
			File f = new File("data/xml/custom/skill_balance.xml");
			if (f.exists()) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				NodeList nodes = doc.getElementsByTagName("skill");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element el = (Element) nodes.item(i);
					int id = Integer.parseInt(el.getAttribute("id"));
					double mult = Double.parseDouble(el.getAttribute("multiplier"));
					skillMultipliers.put(id, mult);
				}
			}
		} catch (Exception e) {
			log.warn("BalanceProfileService: Erro ao ler skill_balance.xml: {}", e.getMessage());
		}
	}

	private static long matchupKey(int attackerClassId, int targetClassId) {
		return (((long) attackerClassId) << 32) | (targetClassId & 0xFFFFFFFFL);
	}

	public double getPAtkMultiplier(int classId) {
		ClassBalance b = classMultipliers.get(classId);
		return b != null ? b.pAtk() : 1.0;
	}

	public double getMAttackMultiplier(int classId) {
		ClassBalance b = classMultipliers.get(classId);
		return b != null ? b.mAtk() : 1.0;
	}

	public double getPDefMultiplier(int classId) {
		ClassBalance b = classMultipliers.get(classId);
		return b != null ? b.pDef() : 1.0;
	}

	public double getMDefMultiplier(int classId) {
		ClassBalance b = classMultipliers.get(classId);
		return b != null ? b.mDef() : 1.0;
	}

	public double getMatchupMultiplier(int attackerClassId, int targetClassId) {
		Double mult = matchupMultipliers.get(matchupKey(attackerClassId, targetClassId));
		return mult != null ? mult : 1.0;
	}

	public double getSkillMultiplier(int skillId) {
		Double mult = skillMultipliers.get(skillId);
		return mult != null ? mult : 1.0;
	}

	public void setClassMultiplier(int classId, double pAtk, double mAtk, double pDef, double mDef) {
		classMultipliers.put(classId, new ClassBalance(pAtk, mAtk, pDef, mDef));
	}

	public void setMatchupMultiplier(int attackerClassId, int targetClassId, double multiplier) {
		matchupMultipliers.put(matchupKey(attackerClassId, targetClassId), multiplier);
	}

	public void setSkillMultiplier(int skillId, double multiplier) {
		skillMultipliers.put(skillId, multiplier);
	}
}
