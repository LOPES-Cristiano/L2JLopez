package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.service.AwayStatusService;
import com.lopez.l2j.game.service.BankingService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para comandos de voz gerais:
 * .online, .stats, .menu, .blockbuff, .bank, .deposit, .withdraw, .away, .back
 */
@Component
public class VoicedGeneralHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"online",
			"stats",
			"menu",
			"blockbuff",
			"bank",
			"deposit",
			"withdraw",
			"away",
			"back"
	);

	private final BankingService bankingService;
	private final AwayStatusService awayStatusService;

	public VoicedGeneralHandler() {
		this(null, null);
	}

	@Autowired(required = false)
	public VoicedGeneralHandler(BankingService bankingService, AwayStatusService awayStatusService) {
		this.bankingService = bankingService != null ? bankingService : new BankingService();
		this.awayStatusService = awayStatusService != null ? awayStatusService : new AwayStatusService();
	}

	@Override
	public boolean useVoicedCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		var ctx = session.context();

		String lower = command.toLowerCase(Locale.ROOT);
		switch (lower) {
			case "online" -> {
				int count = ctx != null && ctx.world() != null ? ctx.world().players().size() : 0;
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + count));
				return true;
			}
			case "stats" -> {
				var t = ctx != null && ctx.characters() != null ? ctx.characters().template(active) : null;
				var stats = PlayerStats.calculate(active, t);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
								active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
								stats.pAtkSpd(), stats.mAtkSpd())));
				return true;
			}
			case "menu" -> {
				if (ctx != null && ctx.preferences() != null) {
					session.send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Menu de preferencias indisponivel."));
				}
				return true;
			}
			case "blockbuff" -> {
				if (ctx != null && ctx.preferences() != null) {
					boolean blocked = ctx.preferences().toggleBlockBuffs(active.objectId());
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
				}
				return true;
			}
			case "bank" -> {
				bankingService.showBankHelp(session);
				return true;
			}
			case "deposit" -> {
				int count = 1;
				if (params != null && !params.isBlank()) {
					try {
						count = Math.max(1, Integer.parseInt(params.trim()));
					} catch (NumberFormatException ignored) {}
				}
				bankingService.deposit(session, count);
				return true;
			}
			case "withdraw" -> {
				int count = 1;
				if (params != null && !params.isBlank()) {
					try {
						count = Math.max(1, Integer.parseInt(params.trim()));
					} catch (NumberFormatException ignored) {}
				}
				bankingService.withdraw(session, count);
				return true;
			}
			case "away" -> {
				awayStatusService.setAway(session, params);
				return true;
			}
			case "back" -> {
				awayStatusService.setBack(session);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	@Override
	public List<String> getVoicedCommandList() {
		return COMMANDS;
	}
}
