package com.lopez.l2j.game.npc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Catalogo de templates de NPC (NpcTable do legado). */
public interface NpcTemplateTable {

	Optional<NpcTemplate> get(int npcId);

	int size();

	default Collection<NpcTemplate> all() {
		return java.util.List.of();
	}

	static NpcTemplateTable of(Collection<NpcTemplate> templates) {
		Map<Integer, NpcTemplate> byId = new HashMap<>();
		templates.forEach(t -> byId.put(t.id(), t));
		return new NpcTemplateTable() {
			@Override
			public Optional<NpcTemplate> get(int npcId) {
				return Optional.ofNullable(byId.get(npcId));
			}

			@Override
			public int size() {
				return byId.size();
			}

			@Override
			public Collection<NpcTemplate> all() {
				return byId.values();
			}
		};
	}
}
