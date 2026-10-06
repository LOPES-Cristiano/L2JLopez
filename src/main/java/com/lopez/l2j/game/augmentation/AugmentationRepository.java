package com.lopez.l2j.game.augmentation;

import java.util.Map;
import java.util.Optional;

/**
 * Repositorio de persistencia de augmentacoes (tabela SQL {@code item_attributes}).
 */
public interface AugmentationRepository {

	void save(int itemId, Augmentation aug);

	void delete(int itemId);

	Optional<Augmentation> findByItemId(int itemId);

	Map<Integer, Augmentation> findByOwnerId(int ownerId);
}
