package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registro e despachante declarativo de comandos de voz (.online, .stats, .autofarm, etc.).
 */
@Service
public class VoicedCommandHandlerRegistry {

	private static final Logger log = LoggerFactory.getLogger(VoicedCommandHandlerRegistry.class);
	private final Map<String, IVoicedCommandHandler> handlers = new HashMap<>();
	private final List<String> registeredCommands = new ArrayList<>();

	@Autowired
	public VoicedCommandHandlerRegistry(@Autowired(required = false) List<IVoicedCommandHandler> handlerList) {
		if (handlerList != null) {
			for (IVoicedCommandHandler handler : handlerList) {
				register(handler);
			}
		}
	}

	public void register(IVoicedCommandHandler handler) {
		for (String cmd : handler.getVoicedCommandList()) {
			String cleanCmd = cmd.startsWith(".") ? cmd.substring(1) : cmd;
			String lower = cleanCmd.toLowerCase(Locale.ROOT);
			handlers.put(lower, handler);
			if (!registeredCommands.contains("." + lower)) {
				registeredCommands.add("." + lower);
			}
		}
	}

	public boolean execute(String command, GameSession session, String params) {
		if (command == null) {
			return false;
		}
		String cleanCmd = command.startsWith(".") ? command.substring(1) : command;
		IVoicedCommandHandler handler = handlers.get(cleanCmd.toLowerCase(Locale.ROOT));
		if (handler != null) {
			try {
				return handler.useVoicedCommand(cleanCmd, session, params);
			} catch (Exception e) {
				log.warn("Erro ao executar comando de voz '{}': {}", command, e.getMessage(), e);
				return false;
			}
		}
		return false;
	}

	public boolean hasCommand(String command) {
		if (command == null) {
			return false;
		}
		String cleanCmd = command.startsWith(".") ? command.substring(1) : command;
		return handlers.containsKey(cleanCmd.toLowerCase(Locale.ROOT));
	}

	public List<String> getAllCommands() {
		return Collections.unmodifiableList(registeredCommands);
	}
}
