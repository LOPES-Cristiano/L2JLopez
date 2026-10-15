package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.service.SchemeBufferService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Handler modular para bypasses de buffer interativo (HTML, NPC Lucera e BBS).
 */
@Component
public class BypassBufferHandler implements IBypassHandler {

	private final SchemeBufferService schemeBufferService;

	@Autowired
	public BypassBufferHandler(SchemeBufferService schemeBufferService) {
		this.schemeBufferService = schemeBufferService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) return false;
		return command.startsWith("voiced_buffer")
				|| command.startsWith("_bbsbuff_")
				|| command.startsWith("scripts_services.Buffer:act");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || schemeBufferService == null) {
			return false;
		}
		var active = session.activeCharacter();

		if (command.startsWith("scripts_services.Buffer:actBBS")) {
			String args = command.length() > 30 ? command.substring(30).trim() : "";
			return schemeBufferService.handleLuceraBufferAct(active, session, resolveTargetNpc(session), args, true);
		}
		if (command.startsWith("scripts_services.Buffer:act")) {
			String args = command.length() > 27 ? command.substring(27).trim() : "";
			return schemeBufferService.handleLuceraBufferAct(active, session, resolveTargetNpc(session), args, false);
		}

		String param;
		if (command.startsWith("voiced_buffer")) {
			param = command.length() > 13 ? command.substring(13).trim() : "";
		} else {
			param = command.substring(9).trim();
		}

		if (param.isEmpty()) {
			session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			return true;
		}

		String[] parts = param.split("\\s+");
		String sub = parts[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "heal", "restore" -> {
				schemeBufferService.heal(active, session);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "cancel", "cleanup" -> {
				schemeBufferService.cancelBuffs(active, session);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "1", "fighter" -> {
				schemeBufferService.applyScheme(active, session, 1, false);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "2", "mage" -> {
				schemeBufferService.applyScheme(active, session, 2, false);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "3" -> {
				schemeBufferService.applyScheme(active, session, 3, false);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "pet_1", "pet_fighter" -> {
				schemeBufferService.applyScheme(active, session, 1, true);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "pet_2", "pet_mage" -> {
				schemeBufferService.applyScheme(active, session, 2, true);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "support" -> {
				int idx = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 1) : 1;
				boolean pet = parts.length > 2 && "pet".equalsIgnoreCase(parts[2]);
				schemeBufferService.applyScheme(active, session, idx, pet);
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
			}
			case "editschemes" -> {
				int idx = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 3) : 3;
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderSchemeEditHtml(active, 0, idx)));
			}
			case "skillselect" -> {
				int idx = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 3) : 3;
				int skId = parts.length > 2 ? GameSession.parseIntSafe(parts[2], 0) : 0;
				if (skId > 0) {
					schemeBufferService.addSkillToScheme(active.objectId(), idx, skId);
				}
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderSchemeEditHtml(active, 0, idx)));
			}
			case "skillunselect" -> {
				int idx = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 3) : 3;
				int skId = parts.length > 2 ? GameSession.parseIntSafe(parts[2], 0) : 0;
				if (skId > 0) {
					schemeBufferService.removeSkillFromScheme(active.objectId(), idx, skId);
				}
				session.send(new NpcHtmlMessage(0, schemeBufferService.renderSchemeEditHtml(active, 0, idx)));
			}
			default -> session.send(new NpcHtmlMessage(0, schemeBufferService.renderHtml(active)));
		}
		return true;
	}

	private NpcInstance resolveTargetNpc(GameSession session) {
		if (session.context() == null || session.context().world() == null) {
			return null;
		}
		int targetId = session.targetObjectId();
		if (targetId > 0) {
			var npcOpt = session.context().world().npc(targetId);
			if (npcOpt.isPresent()) {
				return npcOpt.get();
			}
		}
		return null;
	}
}
