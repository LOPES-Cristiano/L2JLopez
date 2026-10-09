package com.lopez.l2j.game.service;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Tarefa agendada periodica para inspecionar jogadores online e remover
 * automaticamente status Hero, VIP e AIOx cuja duracao temporal tenha expirado.
 */
@Service
public class PlayerStatusExpirationTask {

	private static final Logger log = LoggerFactory.getLogger(PlayerStatusExpirationTask.class);

	private final GameWorld world;
	private final HeroService heroService;
	private final VipService vipService;
	private final AioService aioService;

	public PlayerStatusExpirationTask(
			@Autowired(required = false) GameWorld world,
			@Autowired(required = false) HeroService heroService,
			@Autowired(required = false) VipService vipService,
			@Autowired(required = false) AioService aioService) {
		this.world = world;
		this.heroService = heroService;
		this.vipService = vipService;
		this.aioService = aioService;
	}

	@Scheduled(fixedDelay = 60_000)
	public void checkExpirations() {
		if (world == null) {
			return;
		}
		long now = System.currentTimeMillis();
		for (GameWorld.OnlinePlayer op : world.players()) {
			PlayerCharacter player = op.character();
			if (player == null) {
				continue;
			}
			boolean changed = false;

			// Checagem de expiracao de Hero
			if (heroService != null && player.isHero() && player.heroExpiration() > 0 && player.heroExpiration() <= now) {
				heroService.removeHero(player);
				op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", "Seu periodo de Heroi expirou!"));
				changed = true;
				log.info("Status Hero expirado para o jogador {} [{}]", player.getName(), player.objectId());
			}

			// Checagem de expiracao de VIP
			if (vipService != null && player.isVip() && player.vipExpiration() > 0 && player.vipExpiration() <= now) {
				vipService.removeVip(player);
				op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", "Seu periodo de VIP expirou!"));
				changed = true;
				log.info("Status VIP expirado para o jogador {} [{}]", player.getName(), player.objectId());
			}

			// Checagem de expiracao de AIOx
			if (aioService != null && player.isAio() && player.aioExpiration() > 0 && player.aioExpiration() <= now) {
				aioService.removeAio(player);
				op.send(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", "Seu periodo de Buffer AIO expirou!"));
				changed = true;
				log.info("Status AIOx expirado para o jogador {} [{}]", player.getName(), player.objectId());
			}

			if (changed && op instanceof GameSession gs && gs.context() != null) {
				var t = gs.context().characters().template(player);
				op.send(new UserInfo(player, t));
				world.broadcastAround(op, GameWorld.VISIBILITY_RADIUS, new CharInfo(player, t), false);
			}
		}
	}
}
