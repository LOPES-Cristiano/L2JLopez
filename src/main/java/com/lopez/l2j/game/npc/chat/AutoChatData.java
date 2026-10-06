package com.lopez.l2j.game.npc.chat;

import java.util.List;

/**
 * Dados de configuracao de chat automatico para NPCs.
 * Porta de AutoChatDefinition de L2JDream.
 */
public record AutoChatData(int groupId, int npcId, long chatDelayMs, List<String> chatTexts) {

	public AutoChatData {
		chatTexts = List.copyOf(chatTexts);
	}

	public AutoChatData(int groupId, int npcId, long chatDelayMs, String... texts) {
		this(groupId, npcId, chatDelayMs, List.of(texts));
	}
}
