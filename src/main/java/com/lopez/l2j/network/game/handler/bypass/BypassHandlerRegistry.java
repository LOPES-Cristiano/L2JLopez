package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registro e despachante declarativo de handlers de bypass de HTML.
 */
@Service
public class BypassHandlerRegistry {

	private static final Logger log = LoggerFactory.getLogger(BypassHandlerRegistry.class);
	private final List<IBypassHandler> handlers;

	@Autowired
	public BypassHandlerRegistry(@Autowired(required = false) List<IBypassHandler> handlers) {
		this.handlers = handlers != null ? handlers : List.of();
	}

	public boolean execute(String command, GameSession session) {
		if (command == null || session == null) {
			return false;
		}
		for (IBypassHandler handler : handlers) {
			if (handler.canHandle(command)) {
				try {
					return handler.handleBypass(command, session);
				} catch (Exception e) {
					log.warn("Erro ao processar bypass '{}' com handler {}: {}", command, handler.getClass().getSimpleName(), e.getMessage(), e);
					return false;
				}
			}
		}
		return false;
	}
}
