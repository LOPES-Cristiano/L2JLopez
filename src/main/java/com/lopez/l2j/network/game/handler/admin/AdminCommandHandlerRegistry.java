package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registro e despachante declarativo de comandos administrativos (//admin).
 */
@Service
public class AdminCommandHandlerRegistry {

	private static final Logger log = LoggerFactory.getLogger(AdminCommandHandlerRegistry.class);
	private final Map<String, IAdminCommandHandler> handlers = new HashMap<>();

	@Autowired
	public AdminCommandHandlerRegistry(@Autowired(required = false) List<IAdminCommandHandler> handlerList) {
		if (handlerList != null) {
			for (IAdminCommandHandler handler : handlerList) {
				register(handler);
			}
		}
	}

	public void register(IAdminCommandHandler handler) {
		for (String cmd : handler.getAdminCommandList()) {
			handlers.put(cmd.toLowerCase(Locale.ROOT), handler);
		}
	}

	public boolean execute(String command, GameSession session, String params) {
		if (command == null) {
			return false;
		}
		IAdminCommandHandler handler = handlers.get(command.toLowerCase(Locale.ROOT));
		if (handler != null) {
			try {
				return handler.useAdminCommand(command, session, params);
			} catch (Exception e) {
				log.warn("Erro ao executar comando admin '{}': {}", command, e.getMessage(), e);
				return false;
			}
		}
		return false;
	}

	public boolean hasCommand(String command) {
		return command != null && handlers.containsKey(command.toLowerCase(Locale.ROOT));
	}
}
