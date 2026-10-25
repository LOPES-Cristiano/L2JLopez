package com.lopez.l2j.game.skills.handlers;

import com.lopez.l2j.game.model.ObjectIdFactory;

/**
 * Bridge de compatibilidade para {@link com.lopez.l2j.game.skill.handler.SkillHandlerRegistry}.
 */
public class SkillHandlerRegistry extends com.lopez.l2j.game.skill.handler.SkillHandlerRegistry {

	public SkillHandlerRegistry(ObjectIdFactory idFactory) {
		super(idFactory);
	}

	public SkillHandlerRegistry() {
		super();
	}
}
