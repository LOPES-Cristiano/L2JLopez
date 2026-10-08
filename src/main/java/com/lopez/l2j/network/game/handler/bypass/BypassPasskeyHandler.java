package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.service.WarehousePasskeyService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler modular para bypasses de senha numerica de armazem:
 * passkey_key, passkey_submit, passkey_prompt
 */
@Component
public class BypassPasskeyHandler implements IBypassHandler {

	private final WarehousePasskeyService passkeyService;

	public BypassPasskeyHandler() {
		this(null);
	}

	@Autowired(required = false)
	public BypassPasskeyHandler(WarehousePasskeyService passkeyService) {
		this.passkeyService = passkeyService != null ? passkeyService : new WarehousePasskeyService();
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) {
			return false;
		}
		return command.startsWith("passkey_key") || command.startsWith("passkey_submit") || command.equals("passkey_prompt");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		int playerId = active.objectId();

		if (command.equals("passkey_prompt")) {
			String mode = passkeyService.hasPasskey(playerId) ? "unlock" : "setup";
			session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, mode, "", "")));
			return true;
		}

		if (command.startsWith("passkey_key ")) {
			String[] parts = command.substring(12).trim().split("\\s+");
			String mode = parts.length > 0 ? parts[0] : "unlock";
			String digits = parts.length > 1 ? parts[1] : "";
			session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, mode, digits, "")));
			return true;
		}

		if (command.startsWith("passkey_submit ")) {
			String[] parts = command.substring(15).trim().split("\\s+");
			String mode = parts.length > 0 ? parts[0] : "unlock";
			String pin = parts.length > 1 ? parts[1] : "";

			if ("setup".equalsIgnoreCase(mode)) {
				boolean ok = passkeyService.setupPasskey(playerId, pin);
				if (ok) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Seguranca", "Senha do armazem definida com sucesso!"));
				} else {
					session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, mode, "", "PIN invalido! Utilize de 4 a 8 digitos numericos.")));
				}
				return true;
			} else {
				var result = passkeyService.unlock(playerId, pin);
				switch (result) {
					case SUCCESS -> {
						session.send(new CreatureSay(0, CreatureSay.ALL, "Seguranca", "Armazem desbloqueado com sucesso!"));
					}
					case LOCKED_OUT -> {
						session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, mode, "", "Muitas tentativas falhas. Armazem bloqueado temporariamente!")));
					}
					case INVALID -> {
						session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, mode, "", "Senha incorreta! Tente novamente.")));
					}
					case NOT_CONFIGURED -> {
						session.send(new NpcHtmlMessage(0, passkeyService.buildPasskeyHtml(playerId, "setup", "", "Defina sua nova senha de armazem:")));
					}
				}
				return true;
			}
		}

		return false;
	}
}
