package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.tournament.TournamentService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler modular para bypasses do sistema de torneios (.tournament).
 */
@Component
public class BypassTournamentHandler implements IBypassHandler {

	private final TournamentService tournamentService;

	public BypassTournamentHandler() {
		this(null);
	}

	@Autowired(required = false)
	public BypassTournamentHandler(TournamentService tournamentService) {
		this.tournamentService = tournamentService != null ? tournamentService : new TournamentService();
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null) {
			return false;
		}
		return command.startsWith("tournament_join") || command.equals("tournament_leave")
				|| command.equals("tournament_menu") || command.equals("voiced_tournament");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();

		if (command.equals("tournament_menu") || command.equals("voiced_tournament")) {
			session.send(new NpcHtmlMessage(0, tournamentService.buildTournamentHtml(active)));
			return true;
		}

		if (command.equals("tournament_leave")) {
			boolean ok = tournamentService.unregister(active.objectId());
			if (ok) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Voce cancelou a inscricao no torneio."));
			} else {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Voce nao esta inscrito no torneio."));
			}
			session.send(new NpcHtmlMessage(0, tournamentService.buildTournamentHtml(active)));
			return true;
		}

		if (command.startsWith("tournament_join ")) {
			String modeStr = command.substring(16).trim();
			TournamentService.TournamentMode mode = switch (modeStr.toLowerCase()) {
				case "1x1", "solo" -> TournamentService.TournamentMode.SOLO_1X1;
				case "3x3", "trio" -> TournamentService.TournamentMode.TRIO_3X3;
				case "5x5", "party" -> TournamentService.TournamentMode.PARTY_5X5;
				default -> null;
			};

			if (mode == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Modalidade de torneio invalida."));
				return true;
			}

			if (mode == TournamentService.TournamentMode.SOLO_1X1) {
				boolean ok = tournamentService.register(mode, List.of(active));
				if (ok) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Inscricao realizada no Torneio 1x1! Aguardando oponente..."));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Nao foi possivel realizar a inscricao no momento."));
				}
			} else {
				// Modalidade em equipe: verificar membros da party
				var party = session.party();
				if (party == null || !party.isLeader(active.objectId())) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Apenas o lider de um grupo pode inscrever a equipe em torneios!"));
					return true;
				}
				if (party.members().size() < mode.getRequiredMembers()) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio",
							String.format("Seu grupo precisa de pelo menos %d membros para esta modalidade!", mode.getRequiredMembers())));
					return true;
				}

				List<PlayerCharacter> teamList = new ArrayList<>();
				for (var memberSession : party.members()) {
					if (memberSession.activeCharacter() != null) {
						teamList.add(memberSession.activeCharacter());
					}
					if (teamList.size() == mode.getRequiredMembers()) {
						break;
					}
				}

				boolean ok = tournamentService.register(mode, teamList);
				if (ok) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio",
							String.format("Equipe inscrita com sucesso no Torneio %s!", mode.getLabel())));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Torneio", "Um ou mais membros estao mortos ou ja registrados em outro torneio."));
				}
			}

			session.send(new NpcHtmlMessage(0, tournamentService.buildTournamentHtml(active)));
			return true;
		}

		return false;
	}
}
