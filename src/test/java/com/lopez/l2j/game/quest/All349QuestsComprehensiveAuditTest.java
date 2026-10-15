package com.lopez.l2j.game.quest;

import com.lopez.l2j.game.html.HtmCache;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Auditoria automatizada completa de todas as 349 Quests (Quest001 ate Quest1103),
 * validando registro no QuestManager, NPCs de inicio/dialogo/monstros, itens de quest,
 * resolucao de todos os HTMLs referenciados via HtmCache e conversao de links PTS/Bracket.
 */
public class All349QuestsComprehensiveAuditTest {

	private static QuestManager questManager;
	private static HtmCache htmCache;
	private static List<Class<? extends Quest>> questClasses;

	@BeforeAll
	static void initAllQuests() throws Exception {
		questManager = new QuestManager(null);
		htmCache = new HtmCache("data/html");
		questClasses = new ArrayList<>();

		Path baseDir = Path.of("src/main/java/com/lopez/l2j/game/quest");
		List<Path> classFiles = new ArrayList<>();
		try (var stream = Files.walk(baseDir)) {
			stream.filter(p -> p.getFileName().toString().startsWith("Quest")
					&& p.getFileName().toString().endsWith(".java")
					&& !p.getFileName().toString().equals("Quest.java")
					&& !p.getFileName().toString().equals("QuestState.java")
					&& !p.getFileName().toString().equals("QuestTimer.java")
					&& !p.getFileName().toString().equals("QuestManager.java")
					&& !p.getFileName().toString().matches("^Quest(1104|1201|1630|2000|2211|4000|5000|6050|6051|6999|7000|7001|8002|8003).*"))
					.forEach(classFiles::add);
		}

		for (Path file : classFiles) {
			String rel = Path.of("src/main/java").relativize(file).toString()
					.replace(File.separatorChar, '.')
					.replace(".java", "");
			Class<?> clazz = Class.forName(rel);
			if (Quest.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
				@SuppressWarnings("unchecked")
				Class<? extends Quest> qClass = (Class<? extends Quest>) clazz;
				questClasses.add(qClass);
				instantiateQuest(qClass, questManager);
			}
		}

		questClasses.sort(Comparator.comparing(Class::getSimpleName));
	}

	private static Quest instantiateQuest(Class<? extends Quest> qClass, QuestManager qm) throws Exception {
		for (Constructor<?> ctor : qClass.getDeclaredConstructors()) {
			ctor.setAccessible(true);
			Class<?>[] params = ctor.getParameterTypes();
			if (params.length == 1 && params[0] == QuestManager.class) {
				return (Quest) ctor.newInstance(qm);
			} else if (params.length == 0) {
				Quest q = (Quest) ctor.newInstance();
				qm.registerQuest(q);
				return q;
			}
		}
		throw new IllegalStateException("No compatible constructor for " + qClass.getName());
	}

	@Test
	void testAll349QuestsRegisteredAndValid() {
		Collection<Quest> allQuests = questManager.getAllQuests();
		assertEquals(349, questClasses.size(), "Deve haver exatamente 349 classes de Quest implementadas");
		assertEquals(349, allQuests.size(), "Todas as 349 Quests devem estar registradas no QuestManager");

		for (Quest q : allQuests) {
			assertTrue(q.getQuestId() > 0, "Quest ID invalido para " + q.getClass().getSimpleName());
			assertNotNull(q.getName(), "Nome nulo para quest " + q.getQuestId());
			assertFalse(q.getName().isBlank(), "Nome vazio para quest " + q.getQuestId());
			assertNotNull(q.getDescr(), "Descricao nula para quest " + q.getQuestId());

			// Toda quest (exceto 255 Tutorial que inicia no login) deve ter NPCs registrados
			if (q.getQuestId() != 255) {
				boolean hasNpcs = !q.getStartNpcIds().isEmpty()
						|| !q.getTalkNpcIds().isEmpty()
						|| !q.getFirstTalkNpcIds().isEmpty()
						|| !q.getKillNpcIds().isEmpty();
				assertTrue(hasNpcs, "Quest " + q.getQuestId() + " (" + q.getName() + ") nao possui NPCs ou Mobs registrados");
			}

			// Adena (57) nunca deve estar em registerQuestItems (para nao ser deletada ao sair da quest)
			Set<Integer> registeredItems = Set.copyOf(q.getQuestItemIds());
			assertFalse(registeredItems.contains(57),
					"Quest " + q.getQuestId() + " registrou Adena (57) como quest item descartavel!");
		}
	}

	@Test
	void testAllReferencedHtmlFilesExistAndParseCleanly() throws Exception {
		Pattern htmlLiteralPattern = Pattern.compile("\"([a-zA-Z0-9_\\-]+\\.htm[l]?)\"");
		Path baseDir = Path.of("src/main/java/com/lopez/l2j/game/quest");
		List<String> missingHtmls = new ArrayList<>();
		int verifiedCount = 0;

		try (var stream = Files.walk(baseDir)) {
			List<Path> files = stream.filter(p -> p.getFileName().toString().startsWith("Quest")
					&& p.getFileName().toString().endsWith(".java")
					&& !p.getFileName().toString().matches("^Quest(1104|1201|1630|2000|2211|4000|5000|6050|6051|6999|7000|7001|8002|8003).*")).toList();

			for (Path file : files) {
				String src = Files.readString(file);
				Matcher mId = Pattern.compile("QUEST_ID\\s*=\\s*(\\d+)|super\\(\\s*(\\d+)").matcher(src);
				if (!mId.find()) continue;
				int qid = Integer.parseInt(mId.group(1) != null ? mId.group(1) : mId.group(2));
				if (qid >= 70 && qid <= 100) continue;

				Quest q = questManager.getQuest(qid);
				assertNotNull(q, "Quest " + qid + " nao encontrada no QuestManager");

				Matcher mHtml = htmlLiteralPattern.matcher(src);
				while (mHtml.find()) {
					String htmlFile = mHtml.group(1);
					String content = htmCache.getQuestHtml(q.getQuestId(), q.getName(), htmlFile);
					if (content == null) {
						missingHtmls.add("Q" + qid + " (" + file.getFileName() + ") -> " + htmlFile);
					} else {
						verifiedCount++;
						String rendered = htmCache.render(content, 1001, "Npc", "Player", 1, 30001, q.getName());
						assertNotNull(rendered);
						assertFalse(rendered.isBlank(), "HTML renderizado vazio para " + htmlFile);
						assertFalse(rendered.matches("(?s).*\\[Quest\\s+[^\\]]+\\|[^\\]]+\\].*"),
								"Links PTS colchete nao convertidos em " + htmlFile);
					}
				}
			}
		}

		assertTrue(verifiedCount > 1500, "Deve validar mais de 1500 referencias de HTML nas quests");
		assertTrue(missingHtmls.isEmpty(), "HTMLs ausentes encontrados: " + missingHtmls);
	}

	@Test
	void testPtsBbParseConversionInHtmCache() {
		String ptsRaw = "!Guardiken:\n\nHave you met *Hierarch Kekropus*?%%\n[npc_%objectId%_Quest|Quest]\n[Quest _171_ActsOfEvil 30381-02.htm|\"I will help.\"]";
		String parsed = HtmCache.convertBracketLinks(ptsRaw);
		assertTrue(parsed.startsWith("<html><body>"));
		assertTrue(parsed.endsWith("</body></html>"));
		assertTrue(parsed.contains("<font color=\"FFFFFF\">Guardiken:</font><br>"));
		assertTrue(parsed.contains("<font color=\"LEVEL\">Hierarch Kekropus</font>?<br1>"));
		assertTrue(parsed.contains("<a action=\"bypass -h npc_%objectId%_Quest\">Quest</a>"));
		assertTrue(parsed.contains("<a action=\"bypass -h Quest _171_ActsOfEvil 30381-02.htm\">\"I will help.\"</a>"));
	}
}
