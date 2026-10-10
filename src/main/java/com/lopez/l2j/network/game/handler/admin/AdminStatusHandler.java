package com.lopez.l2j.network.game.handler.admin;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.AioService;
import com.lopez.l2j.game.service.HeroService;
import com.lopez.l2j.game.service.VipService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular de administracao para gestao de status AIO, Hero e VIP.
 * Comandos suportados:
 * //setaio <nome> <dias>
 * //removeaio <nome>
 * //sethero <nome> <dias>
 * //removehero <nome>
 * //setvip <nome> <dias>
 * //removevip <nome>
 * //set_masshero <dias> (ou //masshero)
 * //set_massvip <dias> (ou //massvip)
 * //set_massaio <dias> (ou //massaio)
 */
@Component
public class AdminStatusHandler implements IAdminCommandHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminStatusHandler.class);

	private static final List<String> COMMANDS = List.of(
			// AIO
			"setaio",
			"set_aio",
			"removeaio",
			"remove_aio",
			"delaio",
			"del_aio",
			"set_massaio",
			"setmassaio",
			"massaio",
			"mass_aio",

			// Hero
			"sethero",
			"set_hero",
			"removehero",
			"remove_hero",
			"delhero",
			"del_hero",
			"set_masshero",
			"setmasshero",
			"masshero",
			"mass_hero",

			// VIP
			"setvip",
			"set_vip",
			"removevip",
			"remove_vip",
			"delvip",
			"del_vip",
			"set_massvip",
			"setmassvip",
			"massvip",
			"mass_vip"
	);

	private final HeroService heroService;
	private final VipService vipService;
	private final AioService aioService;
	private final CharacterRepository characterRepository;

	public AdminStatusHandler(
			@Autowired(required = false) HeroService heroService,
			@Autowired(required = false) VipService vipService,
			@Autowired(required = false) AioService aioService,
			@Autowired(required = false) CharacterRepository characterRepository) {
		this.heroService = heroService;
		this.vipService = vipService;
		this.aioService = aioService;
		this.characterRepository = characterRepository;
	}

	@Override
	public boolean useAdminCommand(String command, GameSession session, String params) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}

		String lower = command.toLowerCase(Locale.ROOT);
		return switch (lower) {
			case "setaio", "set_aio" -> handleSetAio(session, params);
			case "removeaio", "remove_aio", "delaio", "del_aio" -> handleRemoveAio(session, params);
			case "set_massaio", "setmassaio", "massaio", "mass_aio" -> handleMassAio(session, params);

			case "sethero", "set_hero" -> handleSetHero(session, params);
			case "removehero", "remove_hero", "delhero", "del_hero" -> handleRemoveHero(session, params);
			case "set_masshero", "setmasshero", "masshero", "mass_hero" -> handleMassHero(session, params);

			case "setvip", "set_vip" -> handleSetVip(session, params);
			case "removevip", "remove_vip", "delvip", "del_vip" -> handleRemoveVip(session, params);
			case "set_massvip", "setmassvip", "massvip", "mass_vip" -> handleMassVip(session, params);

			default -> false;
		};
	}

	@Override
	public List<String> getAdminCommandList() {
		return COMMANDS;
	}

	// =========================================================================
	// HERO COMMANDS
	// =========================================================================

	private boolean handleSetHero(GameSession session, String params) {
		if (heroService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HeroService nao disponivel no servidor."));
			return true;
		}

		TargetAndDays parsed = parseTargetAndDays(session, params, "sethero");
		if (parsed == null) {
			return true;
		}

		if (parsed.days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A quantidade de dias deve ser maior que 0."));
			return true;
		}

		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			long exp = heroService.setHero(targetChar, parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);

			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status Hero concedido a " + targetChar.getName() + " por " + parsed.days
							+ " dia(s). Total restante: " + daysRemaining + " dia(s)."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu status Heroico por " + parsed.days + " dia(s) do Administrador! Total restante: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			long exp = heroService.setHero(offline.objectId(), parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status Hero concedido ao personagem offline " + offline.getName() + " por " + parsed.days
							+ " dia(s). Total no banco: " + daysRemaining + " dia(s)."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //sethero <nome> <dias> (ou selecione um jogador e digite //sethero <dias>)"));
		return true;
	}

	private boolean handleRemoveHero(GameSession session, String params) {
		if (heroService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HeroService nao disponivel no servidor."));
			return true;
		}

		TargetOnly parsed = parseTargetOnly(session, params);
		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			heroService.removeHero(targetChar);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status Hero removido de " + targetChar.getName() + "."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Seu status Heroico foi removido pelo Administrador."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			heroService.removeHero(offline.objectId());
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status Hero removido do personagem offline " + offline.getName() + " no banco."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //removehero <nome> (ou selecione um jogador e digite //removehero)"));
		return true;
	}

	private boolean handleMassHero(GameSession session, String params) {
		if (heroService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "HeroService nao disponivel no servidor."));
			return true;
		}

		int days = parsePositiveDays(params);
		if (days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //set_masshero <dias> (dias > 0)"));
			return true;
		}

		var world = session.context().world();
		if (world == null) {
			return true;
		}

		int count = 0;
		for (var op : world.players()) {
			PlayerCharacter c = op.character();
			if (c == null) {
				continue;
			}
			long exp = heroService.setHero(c, days);
			long daysRemaining = calculateDaysRemaining(exp);
			op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu +" + days + " dia(s) de status Heroico do Administrador! Total: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(op, c, session.context());
			count++;
		}

		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "ANNOUNCEMENT",
				"[Admin] Concedeu +" + days + " dia(s) de Hero para todos os jogadores online!"));
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Mass Hero aplicado com sucesso: " + count + " jogador(es) online receberam +" + days + " dia(s) de Hero."));
		return true;
	}

	// =========================================================================
	// VIP COMMANDS
	// =========================================================================

	private boolean handleSetVip(GameSession session, String params) {
		if (vipService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "VipService nao disponivel no servidor."));
			return true;
		}

		TargetAndDays parsed = parseTargetAndDays(session, params, "setvip");
		if (parsed == null) {
			return true;
		}

		if (parsed.days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A quantidade de dias deve ser maior que 0."));
			return true;
		}

		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			long exp = vipService.setVip(targetChar, parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);

			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status VIP concedido a " + targetChar.getName() + " por " + parsed.days
							+ " dia(s). Total restante: " + daysRemaining + " dia(s)."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu status VIP por " + parsed.days + " dia(s) do Administrador! Total restante: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			long exp = vipService.setVip(offline.objectId(), parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status VIP concedido ao personagem offline " + offline.getName() + " por " + parsed.days
							+ " dia(s). Total no banco: " + daysRemaining + " dia(s)."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //setvip <nome> <dias> (ou selecione um jogador e digite //setvip <dias>)"));
		return true;
	}

	private boolean handleRemoveVip(GameSession session, String params) {
		if (vipService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "VipService nao disponivel no servidor."));
			return true;
		}

		TargetOnly parsed = parseTargetOnly(session, params);
		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			vipService.removeVip(targetChar);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status VIP removido de " + targetChar.getName() + "."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Seu status VIP foi removido pelo Administrador."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			vipService.removeVip(offline.objectId());
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status VIP removido do personagem offline " + offline.getName() + " no banco."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //removevip <nome> (ou selecione um jogador e digite //removevip)"));
		return true;
	}

	private boolean handleMassVip(GameSession session, String params) {
		if (vipService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "VipService nao disponivel no servidor."));
			return true;
		}

		int days = parsePositiveDays(params);
		if (days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //set_massvip <dias> (dias > 0)"));
			return true;
		}

		var world = session.context().world();
		if (world == null) {
			return true;
		}

		int count = 0;
		for (var op : world.players()) {
			PlayerCharacter c = op.character();
			if (c == null) {
				continue;
			}
			long exp = vipService.setVip(c, days);
			long daysRemaining = calculateDaysRemaining(exp);
			op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu +" + days + " dia(s) de status VIP do Administrador! Total: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(op, c, session.context());
			count++;
		}

		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "ANNOUNCEMENT",
				"[Admin] Concedeu +" + days + " dia(s) de VIP para todos os jogadores online!"));
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Mass VIP aplicado com sucesso: " + count + " jogador(es) online receberam +" + days + " dia(s) de VIP."));
		return true;
	}

	// =========================================================================
	// AIO COMMANDS
	// =========================================================================

	private boolean handleSetAio(GameSession session, String params) {
		if (aioService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "AioService nao disponivel no servidor."));
			return true;
		}

		TargetAndDays parsed = parseTargetAndDays(session, params, "setaio");
		if (parsed == null) {
			return true;
		}

		if (parsed.days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "A quantidade de dias deve ser maior que 0."));
			return true;
		}

		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			long exp = aioService.setAio(targetChar, parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);

			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status AIOx concedido a " + targetChar.getName() + " por " + parsed.days
							+ " dia(s). Total restante: " + daysRemaining + " dia(s)."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu status de Buffer AIO por " + parsed.days + " dia(s) do Administrador! Total restante: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			long exp = aioService.setAio(offline.objectId(), parsed.days);
			long daysRemaining = calculateDaysRemaining(exp);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status AIOx concedido ao personagem offline " + offline.getName() + " por " + parsed.days
							+ " dia(s). Total no banco: " + daysRemaining + " dia(s)."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //setaio <nome> <dias> (ou selecione um jogador e digite //setaio <dias>)"));
		return true;
	}

	private boolean handleRemoveAio(GameSession session, String params) {
		if (aioService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "AioService nao disponivel no servidor."));
			return true;
		}

		TargetOnly parsed = parseTargetOnly(session, params);
		if (parsed.onlinePlayer != null) {
			PlayerCharacter targetChar = parsed.onlinePlayer.character();
			aioService.removeAio(targetChar);
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status AIO removido de " + targetChar.getName() + "."));
			parsed.onlinePlayer.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Seu status AIO foi removido pelo Administrador."));
			refreshAppearance(parsed.onlinePlayer, targetChar, session.context());
			return true;
		}

		if (parsed.charName != null) {
			PlayerCharacter offline = findCharacter(parsed.charName);
			if (offline == null) {
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Personagem '" + parsed.charName + "' nao encontrado no mundo nem no banco."));
				return true;
			}
			aioService.removeAio(offline.objectId());
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Status AIO removido do personagem offline " + offline.getName() + " no banco."));
			return true;
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //removeaio <nome> (ou selecione um jogador e digite //removeaio)"));
		return true;
	}

	private boolean handleMassAio(GameSession session, String params) {
		if (aioService == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "AioService nao disponivel no servidor."));
			return true;
		}

		int days = parsePositiveDays(params);
		if (days <= 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Uso: //set_massaio <dias> (dias > 0)"));
			return true;
		}

		var world = session.context().world();
		if (world == null) {
			return true;
		}

		int count = 0;
		for (var op : world.players()) {
			PlayerCharacter c = op.character();
			if (c == null) {
				continue;
			}
			long exp = aioService.setAio(c, days);
			long daysRemaining = calculateDaysRemaining(exp);
			op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS",
					"Voce recebeu +" + days + " dia(s) de status AIO do Administrador! Total: "
							+ daysRemaining + " dia(s)."));
			refreshAppearance(op, c, session.context());
			count++;
		}

		world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "ANNOUNCEMENT",
				"[Admin] Concedeu +" + days + " dia(s) de AIO para todos os jogadores online!"));
		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Mass AIO aplicado com sucesso: " + count + " jogador(es) online receberam +" + days + " dia(s) de AIO."));
		return true;
	}

	// =========================================================================
	// HELPER METHODS
	// =========================================================================

	private record TargetAndDays(GameWorld.OnlinePlayer onlinePlayer, String charName, int days) {}
	private record TargetOnly(GameWorld.OnlinePlayer onlinePlayer, String charName) {}

	private TargetAndDays parseTargetAndDays(GameSession session, String params, String cmdName) {
		String[] parts = (params == null || params.isBlank()) ? new String[0] : params.trim().split("\\s+");

		// Caso 1: Dois argumentos passados (ex: //sethero Cristiano 10 ou //sethero 10 Cristiano)
		if (parts.length >= 2) {
			String nameCandidate = parts[0];
			String daysCandidate = parts[1];
			int days;
			try {
				days = Integer.parseInt(daysCandidate);
			} catch (NumberFormatException e) {
				try {
					days = Integer.parseInt(nameCandidate);
					nameCandidate = daysCandidate;
				} catch (NumberFormatException ex) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Uso: //" + cmdName + " <nome> <dias> (os dias devem ser numericos)."));
					return null;
				}
			}

			GameWorld.OnlinePlayer online = findOnlinePlayer(session, nameCandidate);
			return new TargetAndDays(online, nameCandidate, days);
		}

		// Caso 2: Um argumento numerico e jogador selecionado no alvo
		if (parts.length == 1) {
			try {
				int days = Integer.parseInt(parts[0]);
				GameWorld.OnlinePlayer targetPlayer = getSelectedTargetPlayer(session);
				if (targetPlayer != null) {
					return new TargetAndDays(targetPlayer, targetPlayer.name(), days);
				}
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Nenhum alvo selecionado. Uso: //" + cmdName + " <nome> <dias>"));
				return null;
			} catch (NumberFormatException e) {
				// Nome informado sem dias; padrao 1 dia
				String name = parts[0];
				GameWorld.OnlinePlayer online = findOnlinePlayer(session, name);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Dias nao informados; aplicando 1 dia padrao. (Uso: //" + cmdName + " <nome> <dias>)"));
				return new TargetAndDays(online, name, 1);
			}
		}

		// Caso 3: Nenhum argumento, checa se tem alvo selecionado
		GameWorld.OnlinePlayer targetPlayer = getSelectedTargetPlayer(session);
		if (targetPlayer != null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Dias nao informados; aplicando 1 dia padrao. (Uso: //" + cmdName + " <dias>)"));
			return new TargetAndDays(targetPlayer, targetPlayer.name(), 1);
		}

		session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
				"Uso: //" + cmdName + " <nome> <dias> (ou selecione o alvo e digite //" + cmdName + " <dias>)"));
		return null;
	}

	private TargetOnly parseTargetOnly(GameSession session, String params) {
		String[] parts = (params == null || params.isBlank()) ? new String[0] : params.trim().split("\\s+");
		if (parts.length >= 1) {
			String name = parts[0];
			GameWorld.OnlinePlayer online = findOnlinePlayer(session, name);
			return new TargetOnly(online, name);
		}

		GameWorld.OnlinePlayer targetPlayer = getSelectedTargetPlayer(session);
		if (targetPlayer != null) {
			return new TargetOnly(targetPlayer, targetPlayer.name());
		}

		return new TargetOnly(null, null);
	}

	private GameWorld.OnlinePlayer findOnlinePlayer(GameSession session, String name) {
		if (session == null || session.context() == null || session.context().world() == null || name == null) {
			return null;
		}
		return session.context().world().byName(name).orElse(null);
	}

	private GameWorld.OnlinePlayer getSelectedTargetPlayer(GameSession session) {
		if (session == null || session.context() == null || session.context().world() == null) {
			return null;
		}
		int targetId = session.targetObjectId();
		if (targetId != 0 && targetId != session.activeCharacter().objectId()) {
			var opt = session.context().world().player(targetId);
			if (opt.isPresent() && opt.get().character() != null) {
				return opt.get();
			}
		}
		return null;
	}

	private PlayerCharacter findCharacter(String charName) {
		if (characterRepository == null || charName == null) {
			return null;
		}
		return characterRepository.findByName(charName).orElse(null);
	}

	private int parsePositiveDays(String params) {
		if (params == null || params.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(params.trim().split("\\s+")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	private long calculateDaysRemaining(long expirationMillis) {
		if (expirationMillis <= 0) {
			return 0;
		}
		long diff = expirationMillis - System.currentTimeMillis();
		if (diff <= 0) {
			return 0;
		}
		return Math.max(1, (diff + 86_399_999L) / 86_400_000L);
	}

	private void refreshAppearance(GameWorld.OnlinePlayer player, PlayerCharacter character, GameSession.Context ctx) {
		if (player == null || character == null || ctx == null || ctx.characters() == null) {
			return;
		}
		var t = ctx.characters().template(character);
		player.send(new UserInfo(character, t));
		player.sendSkillList();
		if (ctx.world() != null) {
			ctx.world().broadcastAround(player, GameWorld.VISIBILITY_RADIUS,
					new CharInfo(character, t), false);
		}
	}
}
