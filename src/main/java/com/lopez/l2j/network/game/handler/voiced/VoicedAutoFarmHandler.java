package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler para os comandos de voz do AutoFarm Premium (.autofarm, .farm, .farmstart, .farmstop, .farmacp, .autosummonfarm).
 */
@Component
public class VoicedAutoFarmHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"autofarm",
			"farm",
			"farmstart",
			"farmstop",
			"farmacp",
			"autosummonfarm"
	);

	private final AutoFarmService autoFarmService;

	@Autowired
	public VoicedAutoFarmHandler(@Autowired(required = false) AutoFarmService autoFarmService) {
		this.autoFarmService = autoFarmService;
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null || autoFarmService == null) {
			return false;
		}
		String fullCommand = (params == null || params.isBlank() || params.equalsIgnoreCase("menu") || params.equalsIgnoreCase("status") || params.equalsIgnoreCase("gui"))
				? "voice_" + command
				: "voice_" + command + " " + params.trim();
		autoFarmService.handleCommand(session, fullCommand);
		return true;
	}
}
