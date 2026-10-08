package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.service.BankingService;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler modular para bypasses bancarios (.bank):
 * voiced_deposit, voiced_withdraw, voiced_bank
 */
@Component
public class BypassBankingHandler implements IBypassHandler {

	private final BankingService bankingService;

	public BypassBankingHandler() {
		this(null);
	}

	@Autowired(required = false)
	public BypassBankingHandler(BankingService bankingService) {
		this.bankingService = bankingService != null ? bankingService : new BankingService();
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) {
			return false;
		}
		return command.equals("voiced_deposit") || command.startsWith("voiced_deposit ")
				|| command.equals("voiced_withdraw") || command.startsWith("voiced_withdraw ")
				|| command.equals("voiced_bank");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}

		if (command.equals("voiced_bank")) {
			bankingService.showBankHelp(session);
			return true;
		} else if (command.startsWith("voiced_deposit")) {
			int count = 1;
			String[] parts = command.split("\\s+");
			if (parts.length > 1) {
				try {
					count = Math.max(1, Integer.parseInt(parts[1]));
				} catch (NumberFormatException ignored) {}
			}
			bankingService.deposit(session, count);
			return true;
		} else if (command.startsWith("voiced_withdraw")) {
			int count = 1;
			String[] parts = command.split("\\s+");
			if (parts.length > 1) {
				try {
					count = Math.max(1, Integer.parseInt(parts[1]));
				} catch (NumberFormatException ignored) {}
			}
			bankingService.withdraw(session, count);
			return true;
		}

		return false;
	}
}
