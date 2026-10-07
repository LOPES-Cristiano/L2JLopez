package com.lopez.l2j.network.game.handler.voiced;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemList;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Handler modular para comandos de voz gerais:
 * .online, .stats, .menu, .blockbuff, .deposit, .withdraw
 */
@Component
public class VoicedGeneralHandler implements IVoicedCommandHandler {

	private static final List<String> COMMANDS = List.of(
			"online",
			"stats",
			"menu",
			"blockbuff",
			"deposit",
			"withdraw"
	);

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
				int count = ctx.world() != null ? ctx.world().players().size() : 0;
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Jogadores online: " + count));
				return true;
			}
			case "stats" -> {
				var t = ctx.characters() != null ? ctx.characters().template(active) : null;
				var stats = PlayerStats.calculate(active, t);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						String.format("%s (Nv %d): P.Atk %d, M.Atk %d, P.Def %d, M.Def %d, AtkSpd %d, CastSpd %d",
								active.name(), active.level(), stats.pAtk(), stats.mAtk(), stats.pDef(), stats.mDef(),
								stats.pAtkSpd(), stats.mAtkSpd())));
				return true;
			}
			case "menu" -> {
				if (ctx.preferences() != null) {
					session.send(new NpcHtmlMessage(0, ctx.preferences().buildMenuHtml(active.objectId(), active.name())));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Menu de preferencias indisponivel."));
				}
				return true;
			}
			case "blockbuff" -> {
				if (ctx.preferences() != null) {
					boolean blocked = ctx.preferences().toggleBlockBuffs(active.objectId());
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bloqueio de buffs: " + (blocked ? "ATIVADO" : "DESATIVADO")));
				}
				return true;
			}
			case "deposit" -> {
				if (!Config.BANKING_SYSTEM_ENABLED) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
					return true;
				}
				int reqAdena = Config.BANKING_SYSTEM_ADENA;
				var adenaItem = active.inventory().byItemId(57).orElse(null);
				if (adenaItem == null || adenaItem.count() < reqAdena) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS",
							"Voce precisa de " + reqAdena + " adena para comprar um Gold Bar."));
					return true;
				}
				ctx.inventories().consumeItem(active.inventory(), 57, reqAdena, "BankingDeposit");
				ctx.inventories().addItem(active.inventory(), 3470, Config.BANKING_SYSTEM_GOLDBARS, "BankingDeposit");
				session.send(ItemList.of(active.inventory().items(), false));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Deposito realizado: Gold Bar adicionado ao seu inventario."));
				return true;
			}
			case "withdraw" -> {
				if (!Config.BANKING_SYSTEM_ENABLED) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Sistema bancario desativado."));
					return true;
				}
				int reqBars = Config.BANKING_SYSTEM_GOLDBARS;
				var barItem = active.inventory().byItemId(3470).orElse(null);
				if (barItem == null || barItem.count() < reqBars) {
					session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa de " + reqBars + " Gold Bar para sacar."));
					return true;
				}
				ctx.inventories().consumeItem(active.inventory(), 3470, reqBars, "BankingWithdraw");
				ctx.inventories().addItem(active.inventory(), 57, Config.BANKING_SYSTEM_ADENA, "BankingWithdraw");
				session.send(ItemList.of(active.inventory().items(), false));
				session.send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Saque realizado: Adena adicionada ao seu inventario."));
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
