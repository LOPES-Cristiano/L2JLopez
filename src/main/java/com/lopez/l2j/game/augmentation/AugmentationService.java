package com.lopez.l2j.game.augmentation;

import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Servico de Augmentacao de Armas via Life Stones (L2JDream AugmentationData e L2Augmentation).
 * Mapeia 10 niveis de Life Stones (46-76) e 4 graus (No-Grade, Mid-Grade, High-Grade, Top-Grade).
 */
@Service
public class AugmentationService {

	private static final Logger log = LoggerFactory.getLogger(AugmentationService.class);

	public static final int STAT_START = 1;
	public static final int STAT_END = 14560;
	public static final int STAT_BLOCKSIZE = 3640;
	public static final int STAT_SUBBLOCKSIZE = 91;
	public static final int SKILLS_BLOCKSIZE = 178;

	public static final int BLUE_START = 0;
	public static final int BLUE_END = 16;
	public static final int PURPLE_START = 17;
	public static final int PURPLE_END = 123;
	public static final int RED_START = 124;
	public static final int RED_END = 177;

	public static final int BASESTAT_STR = 16341;
	public static final int BASESTAT_CON = 16342;
	public static final int BASESTAT_INT = 16343;
	public static final int BASESTAT_MEN = 16344;

	public static final int GEMSTONE_D = 2130;
	public static final int GEMSTONE_C = 2131;
	public static final int GEMSTONE_B = 2132;

	public record AugStat(String stat, double value) {}

	public static class AugmentationSkill {
		private final int skillId;
		private final int maxSkillLevel;
		private final int augmentationSkillId;

		public AugmentationSkill(int skillId, int maxSkillLevel, int augmentationSkillId) {
			this.skillId = skillId;
			this.maxSkillLevel = maxSkillLevel;
			this.augmentationSkillId = augmentationSkillId;
		}

		public int skillId() { return skillId; }
		public int maxSkillLevel() { return maxSkillLevel; }
		public int augmentationSkillId() { return augmentationSkillId; }
	}

	public static class AugmentationStatDef {
		private final String stat;
		private final int singleSize;
		private final int combinedSize;
		private final float[] singleValues;
		private final float[] combinedValues;

		public AugmentationStatDef(String stat, float[] sValues, float[] cValues) {
			this.stat = stat;
			this.singleValues = sValues != null ? sValues : new float[0];
			this.singleSize = this.singleValues.length;
			this.combinedValues = cValues != null ? cValues : new float[0];
			this.combinedSize = this.combinedValues.length;
		}

		public String stat() { return stat; }

		public float getSingleStatValue(int i) {
			if (singleSize == 0) return 0f;
			if (i >= singleSize || i < 0) return singleValues[singleSize - 1];
			return singleValues[i];
		}

		public float getCombinedStatValue(int i) {
			if (combinedSize == 0) return 0f;
			if (i >= combinedSize || i < 0) return combinedValues[combinedSize - 1];
			return combinedValues[i];
		}
	}

	private final AugmentationRepository repository;
	private final List<AugmentationStatDef>[] augmentationStats = new List[4];
	private AugmentationSkill[] augmentationSkills = new AugmentationSkill[0];
	private final Map<Integer, AugmentationSkill> skillMapByAugId = new HashMap<>();

	public AugmentationService(AugmentationRepository repository) {
		this.repository = repository;
		for (int i = 0; i < 4; i++) {
			augmentationStats[i] = new ArrayList<>();
		}
		loadXmlData();
	}

	public AugmentationRepository repository() {
		return repository;
	}

	private void loadXmlData() {
		File dataDir = findDataDirectory();
		if (dataDir == null) {
			log.info("Diretorio de dados de augmentacao nao encontrado. Usando configuracoes default.");
			loadFallbackData();
			return;
		}

		try {
			loadSkillMap(new File(dataDir, "augmentation_skillmap.xml"));
			for (int i = 1; i <= 4; i++) {
				loadStatsXml(new File(dataDir, "augmentation_stats" + i + ".xml"), i - 1);
			}
			log.info("AugmentationService inicializado: {} skills e {} stats carregados.",
					augmentationSkills.length, augmentationStats[0].size() * 4);
		} catch (Exception e) {
			log.warn("Erro ao carregar dados XML de augmentacao: {}. Usando fallbacks.", e.getMessage());
			loadFallbackData();
		}
	}

	private File findDataDirectory() {
		Path[] candidates = new Path[] {
				Path.of("data", "xml", "stats", "augmentation"),
				Path.of("..", "data", "xml", "stats", "augmentation"),
				Path.of("L2JLopez", "data", "xml", "stats", "augmentation")
		};
		for (Path p : candidates) {
			File f = p.toFile();
			if (f.exists() && f.isDirectory()) {
				return f;
			}
		}
		return null;
	}

	private void loadSkillMap(File file) {
		if (!file.exists()) return;
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(file);

			List<AugmentationSkill> list = new ArrayList<>();
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
				if ("list".equalsIgnoreCase(n.getNodeName())) {
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
						if ("augmentation".equalsIgnoreCase(d.getNodeName())) {
							NamedNodeMap attrs = d.getAttributes();
							int augId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int skillId = 0, skillLevel = 0;

							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
								if ("skillId".equalsIgnoreCase(cd.getNodeName())) {
									skillId = Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue());
								} else if ("skillLevel".equalsIgnoreCase(cd.getNodeName())) {
									skillLevel = Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue());
								}
							}
							var augSkill = new AugmentationSkill(skillId, skillLevel, augId);
							list.add(augSkill);
							skillMapByAugId.put(augId, augSkill);
						}
					}
				}
			}
			augmentationSkills = list.toArray(new AugmentationSkill[0]);
		} catch (Exception e) {
			log.warn("Erro lendo {}: {}", file.getName(), e.getMessage());
		}
	}

	private void loadStatsXml(File file, int blockIdx) {
		if (!file.exists()) return;
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(file);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
				if ("list".equalsIgnoreCase(n.getNodeName())) {
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
						if ("stat".equalsIgnoreCase(d.getNodeName())) {
							NamedNodeMap attrs = d.getAttributes();
							String statName = attrs.getNamedItem("name").getNodeValue();
							float[] solo = null;
							float[] combined = null;

							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
								if ("table".equalsIgnoreCase(cd.getNodeName())) {
									String tableName = cd.getAttributes().getNamedItem("name").getNodeValue();
									StringTokenizer tok = new StringTokenizer(cd.getFirstChild().getNodeValue());
									List<Float> vals = new ArrayList<>();
									while (tok.hasMoreTokens()) {
										vals.add(Float.parseFloat(tok.nextToken()));
									}
									float[] arr = new float[vals.size()];
									for (int v = 0; v < vals.size(); v++) arr[v] = vals.get(v);

									if ("#soloValues".equalsIgnoreCase(tableName)) {
										solo = arr;
									} else if ("#combinedValues".equalsIgnoreCase(tableName)) {
										combined = arr;
									}
								}
							}
							augmentationStats[blockIdx].add(new AugmentationStatDef(statName, solo, combined));
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("Erro lendo {}: {}", file.getName(), e.getMessage());
		}
	}

	private void loadFallbackData() {
		String[] statNames = new String[] { "pDef", "mDef", "maxHp", "maxMp", "maxCp", "pAtk", "mAtk", "regHp", "regMp", "regCp", "rEvas", "accCombat", "rCrit" };
		for (int b = 0; b < 4; b++) {
			for (String st : statNames) {
				float[] solo = new float[40];
				float[] comb = new float[80];
				for (int i = 0; i < solo.length; i++) solo[i] = (i + 1) * 1.5f;
				for (int i = 0; i < comb.length; i++) comb[i] = (i + 1) * 0.8f;
				augmentationStats[b].add(new AugmentationStatDef(st, solo, comb));
			}
		}
		List<AugmentationSkill> fallbackSkills = new ArrayList<>();
		for (int i = 14561; i <= 14561 + 2000; i++) {
			var sk = new AugmentationSkill(3080 + (i % 50), 1, i);
			fallbackSkills.add(sk);
			skillMapByAugId.put(i, sk);
		}
		augmentationSkills = fallbackSkills.toArray(new AugmentationSkill[0]);
	}

	public static int getLifeStoneGrade(int itemId) {
		int diff = itemId - 8723;
		if (diff < 10) return 0; // No-Grade
		if (diff < 20) return 1; // Mid-Grade
		if (diff < 30) return 2; // High-Grade
		return 3;                // Top-Grade
	}

	public static int getLifeStoneLevel(int itemId) {
		int grade = getLifeStoneGrade(itemId);
		int diff = itemId - 8722 - (10 * grade);
		return Math.max(1, Math.min(10, diff));
	}

	public static boolean isLifeStone(int itemId) {
		return itemId >= 8723 && itemId <= 8762;
	}

	public static int getMinPlayerLevel(int lifeStoneLevel) {
		return switch (lifeStoneLevel) {
			case 1 -> 46;
			case 2 -> 49;
			case 3 -> 52;
			case 4 -> 55;
			case 5 -> 58;
			case 6 -> 61;
			case 7 -> 64;
			case 8 -> 67;
			case 9 -> 70;
			default -> 76;
		};
	}

	public static int getGemstoneItemId(ItemInstance weapon) {
		String grade = weapon.template().crystalType();
		if (grade == null) return GEMSTONE_D;
		return switch (grade.toUpperCase()) {
			case "A", "S" -> GEMSTONE_C;
			default -> GEMSTONE_D;
		};
	}

	public static int getGemstoneCount(ItemInstance weapon) {
		String grade = weapon.template().crystalType();
		if (grade == null) return 20;
		return switch (grade.toUpperCase()) {
			case "C" -> 20;
			case "B" -> 30;
			case "A" -> 20;
			case "S" -> 25;
			default -> 20;
		};
	}

	public static int getCancelPrice(ItemInstance weapon) {
		String grade = weapon.template().crystalType();
		if (grade == null) return 95000;
		return switch (grade.toUpperCase()) {
			case "C" -> 210000;
			case "B" -> 270000;
			case "A" -> 420000;
			case "S" -> 480000;
			default -> 100000;
		};
	}

	public boolean isAugmentable(ItemInstance item) {
		if (item == null || item.isAugmented()) return false;
		var t = item.template();
		if (t.kind() != ItemTemplate.Kind.WEAPON) return false;
		String grade = t.crystalType();
		if (grade == null) return false;
		String g = grade.toUpperCase();
		if (!g.equals("C") && !g.equals("B") && !g.equals("A") && !g.equals("S")) return false;
		return t.destroyable();
	}

	public Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade) {
		int level = Math.max(1, Math.min(10, lifeStoneLevel));
		int grade = Math.max(0, Math.min(3, lifeStoneGrade));

		var rnd = ThreadLocalRandom.current();
		int skillChance = switch (grade) {
			case 0 -> 15;
			case 1 -> 30;
			case 2 -> 45;
			default -> 60;
		};
		int glowChance = switch (grade) {
			case 0 -> 0;
			case 1 -> 40;
			case 2 -> 70;
			default -> 100;
		};

		boolean generateSkill = rnd.nextInt(100) < skillChance;
		boolean generateGlow = rnd.nextInt(100) < glowChance;

		int stat34 = 0;
		if (!generateSkill && rnd.nextInt(100) < 1) { // 1% chance de atributo base
			stat34 = rnd.nextInt(BASESTAT_STR, BASESTAT_MEN + 1);
		}

		int resultColor = 0;
		if (stat34 == 0 && !generateSkill) {
			int rc = rnd.nextInt(100);
			resultColor = (rc <= 15 * grade + 40) ? 1 : 0;
		} else {
			int rc = rnd.nextInt(100);
			if (rc <= 10 * grade + 5 || stat34 != 0) {
				resultColor = 3;
			} else if (rc <= 10 * grade + 10) {
				resultColor = 1;
			} else {
				resultColor = 2;
			}
		}

		int stat12;
		if (stat34 == 0 && !generateSkill) {
			int temp = rnd.nextInt(2, 4);
			int colorOffset = resultColor * 10 * STAT_SUBBLOCKSIZE + temp * STAT_BLOCKSIZE + 1;
			int offset = (level - 1) * STAT_SUBBLOCKSIZE + colorOffset;
			stat34 = rnd.nextInt(offset, offset + STAT_SUBBLOCKSIZE);

			if (generateGlow && grade >= 2) {
				offset = (level - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + grade * 10 * STAT_SUBBLOCKSIZE + 1;
			} else {
				offset = (level - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + rnd.nextInt(0, 2) * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = rnd.nextInt(offset, offset + STAT_SUBBLOCKSIZE);
		} else {
			int offset;
			if (!generateGlow) {
				offset = (level - 1) * STAT_SUBBLOCKSIZE + rnd.nextInt(0, 2) * STAT_BLOCKSIZE + 1;
			} else {
				offset = (level - 1) * STAT_SUBBLOCKSIZE + rnd.nextInt(0, 2) * STAT_BLOCKSIZE + (grade + resultColor) / 2 * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = rnd.nextInt(offset, offset + STAT_SUBBLOCKSIZE);
		}

		int skillId = 0;
		int skillLevel = 0;
		if (generateSkill && augmentationSkills.length > 0) {
			int skillOffset = (level - 1) * SKILLS_BLOCKSIZE;
			int start = switch (resultColor) {
				case 1 -> BLUE_START;
				case 2 -> PURPLE_START;
				default -> RED_START;
			};
			int end = switch (resultColor) {
				case 1 -> BLUE_END;
				case 2 -> PURPLE_END;
				default -> RED_END;
			};
			int idx = Math.min(augmentationSkills.length - 1, Math.max(0, skillOffset + rnd.nextInt(start, end + 1)));
			AugmentationSkill sk = augmentationSkills[idx];
			stat34 = sk.augmentationSkillId();
			skillId = sk.skillId();
			skillLevel = sk.maxSkillLevel();
		}

		int attributes = (stat34 << 16) | (stat12 & 0xFFFF);
		return new Augmentation(attributes, skillId, skillLevel);
	}

	public List<AugStat> getAugStatsById(int augmentationId) {
		List<AugStat> temp = new ArrayList<>();
		int[] stats = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = augmentationId >> 16;

		for (int i = 0; i < 2; i++) {
			int val = stats[i];
			if (val >= STAT_START && val <= STAT_END) {
				int block = 0;
				while (val > STAT_BLOCKSIZE) {
					val -= STAT_BLOCKSIZE;
					block++;
				}
				int subblock = 0;
				while (val > STAT_SUBBLOCKSIZE) {
					val -= STAT_SUBBLOCKSIZE;
					subblock++;
				}
				block = Math.min(3, block);
				List<AugmentationStatDef> blkList = augmentationStats[block];
				if (blkList.isEmpty()) continue;

				if (val < 14 && val - 1 < blkList.size()) {
					AugmentationStatDef as = blkList.get(val - 1);
					temp.add(new AugStat(as.stat(), as.getSingleStatValue(subblock)));
				} else {
					val -= 13;
					int x = 12;
					int rescales = 0;
					while (val > x) {
						val -= x;
						x--;
						rescales++;
					}
					if (rescales < blkList.size()) {
						AugmentationStatDef as = blkList.get(rescales);
						temp.add(new AugStat(as.stat(), rescales == 0
								? as.getCombinedStatValue(subblock)
								: as.getCombinedStatValue(subblock * 2 + 1)));
					}
					if (rescales + val < blkList.size()) {
						AugmentationStatDef as = blkList.get(rescales + val);
						temp.add(new AugStat(as.stat(), "rCrit".equalsIgnoreCase(as.stat())
								? as.getCombinedStatValue(subblock)
								: as.getCombinedStatValue(subblock * 2)));
					}
				}
			} else if (val >= BASESTAT_STR && val <= BASESTAT_MEN) {
				switch (val) {
					case BASESTAT_STR -> temp.add(new AugStat("STR", 1.0));
					case BASESTAT_CON -> temp.add(new AugStat("CON", 1.0));
					case BASESTAT_INT -> temp.add(new AugStat("INT", 1.0));
					case BASESTAT_MEN -> temp.add(new AugStat("MEN", 1.0));
				}
			}
		}
		return temp;
	}

	public Augmentation applyAugmentation(ItemInstance item, int lifeStoneItemId) {
		int grade = getLifeStoneGrade(lifeStoneItemId);
		int level = getLifeStoneLevel(lifeStoneItemId);
		Augmentation aug = generateRandomAugmentation(level, grade);
		item.augmentation(aug);
		repository.save(item.objectId(), aug);
		return aug;
	}

	public void removeAugmentation(ItemInstance item) {
		item.augmentation(null);
		repository.delete(item.objectId());
	}
}
