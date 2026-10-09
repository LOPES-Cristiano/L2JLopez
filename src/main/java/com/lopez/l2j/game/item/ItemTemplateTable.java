package com.lopez.l2j.game.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo de templates de item + itens iniciais por classe (ItemTable + char_creation_items do legado). */
public interface ItemTemplateTable {

	/** Linha de char_creation_items: classId -1 vale para todas as classes. */
	record CreationItem(int classId, int itemId, int amount, boolean equipped) {
	}

	Optional<ItemTemplate> get(int itemId);

	int size();

	default Collection<ItemTemplate> all() {
		return java.util.List.of();
	}

	/** Itens dados na criacao de um personagem da classe (os de classId -1 primeiro, como no legado). */
	List<CreationItem> creationItems(int classId);

	/** Tabela fixa em memoria (testes e ferramentas). */
	static ItemTemplateTable of(Collection<ItemTemplate> templates, Collection<CreationItem> creation) {
		Map<Integer, ItemTemplate> byId = new HashMap<>();
		templates.forEach(t -> byId.put(t.id(), t));
		List<CreationItem> rows = List.copyOf(creation);
		return new ItemTemplateTable() {
			@Override
			public Optional<ItemTemplate> get(int itemId) {
				return Optional.ofNullable(byId.get(itemId));
			}

			@Override
			public int size() {
				return byId.size();
			}

			@Override
			public Collection<ItemTemplate> all() {
				return byId.values();
			}

			@Override
			public List<CreationItem> creationItems(int classId) {
				return filterCreation(rows, classId, byId::containsKey);
			}
		};
	}

	static List<CreationItem> filterCreation(List<CreationItem> rows, int classId,
			java.util.function.IntPredicate known) {
		var all = rows.stream().filter(r -> r.classId() == -1 && known.test(r.itemId()));
		var own = rows.stream().filter(r -> r.classId() == classId && known.test(r.itemId()));
		return java.util.stream.Stream.concat(all, own).toList();
	}
}
