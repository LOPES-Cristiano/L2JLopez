package com.lopez.l2j.features.achievements;

import com.lopez.l2j.events.AchievementCompletedEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulo Achievements: avalia as conquistas de um jogador a partir de um {@link PlayerSnapshot},
 * grava o progresso e publica {@link AchievementCompletedEvent} para o mundo entregar a recompensa.
 * So existe quando {@code l2.features.achievements.enabled=true}.
 */
@Service
@ConditionalOnProperty(prefix = "l2.features.achievements", name = "enabled", havingValue = "true")
public class AchievementService {

	private static final Logger log = LoggerFactory.getLogger(AchievementService.class);
	static final String CATALOG_RESOURCE = "features/achievements.xml";

	private final Map<Integer, Achievement> catalog;
	private final AchievementProgressStore store;
	private final ApplicationEventPublisher publisher;

	@Autowired
	public AchievementService(AchievementProgressStore store, ApplicationEventPublisher publisher) {
		this(loadCatalog(), store, publisher);
	}

	AchievementService(Map<Integer, Achievement> catalog, AchievementProgressStore store,
			ApplicationEventPublisher publisher) {
		this.catalog = Map.copyOf(catalog);
		this.store = store;
		this.publisher = publisher;
		log.info("[Achievements] {} conquistas carregadas", this.catalog.size());
	}

	private static Map<Integer, Achievement> loadCatalog() {
		try (InputStream in = new ClassPathResource(CATALOG_RESOURCE).getInputStream()) {
			return AchievementXmlParser.parse(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Nao foi possivel abrir " + CATALOG_RESOURCE, e);
		}
	}

	public Collection<Achievement> all() {
		return catalog.values();
	}

	/**
	 * Avalia todas as conquistas pendentes do jogador. Conquistas completadas na mesma chamada
	 * contam para condicoes como CompleteAchievements (em ordem de id).
	 *
	 * @return as conquistas completadas agora
	 */
	@Transactional
	public List<Achievement> evaluate(PlayerSnapshot player) {
		Set<Integer> completed = new HashSet<>(store.completedIds(player.ownerId()));
		List<Achievement> newlyCompleted = new ArrayList<>();

		for (Achievement achievement : catalog.values().stream()
				.sorted(java.util.Comparator.comparingInt(Achievement::id)).toList()) {
			if (completed.contains(achievement.id()) && !achievement.repeatable()) {
				continue;
			}
			if (!achievement.isMetBy(player, new AchievementCondition.Context(completed))) {
				continue;
			}
			store.recordCompletion(player.ownerId(), achievement.id());
			completed.add(achievement.id());
			newlyCompleted.add(achievement);
			publisher.publishEvent(new AchievementCompletedEvent(
					player.ownerId(), achievement.id(), achievement.name(), achievement.rewards()));
		}
		return newlyCompleted;
	}
}
