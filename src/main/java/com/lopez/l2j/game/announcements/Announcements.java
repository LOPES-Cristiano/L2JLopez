package com.lopez.l2j.game.announcements;

import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gerenciador de anuncios do servidor (Announcements do L2JDream).
 * Envia mensagens no canal ANNOUNCEMENT (0x0a) e exibe boas-vindas no login.
 */
@Component
public class Announcements {

	private static final Logger log = LoggerFactory.getLogger(Announcements.class);

	private final List<String> announcements = new CopyOnWriteArrayList<>();

	public Announcements() {
		// Anuncios padrao de boas-vindas
		announcements.add("Bem-vindo ao L2JLopez Interlude Server!");
		announcements.add("Digite .menu ou .stats para visualizar seus atributos e .classmaster para avancar de classe.");
	}

	public List<String> list() {
		return Collections.unmodifiableList(announcements);
	}

	public void add(String text) {
		if (text != null && !text.isBlank()) {
			announcements.add(text.trim());
			log.info("Novo anuncio registrado: {}", text);
		}
	}

	public boolean delete(int index) {
		if (index >= 0 && index < announcements.size()) {
			String removed = announcements.remove(index);
			log.info("Anuncio removido [{}]: {}", index, removed);
			return true;
		}
		return false;
	}

	/**
	 * Envia todos os anuncios ativos para o jogador que acabou de entrar no mundo.
	 */
	public void showToPlayer(java.util.function.Consumer<com.lopez.l2j.network.game.packet.GameServerPacket> packetSender) {
		for (String msg : announcements) {
			packetSender.accept(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "SYS", msg));
		}
	}

	public void showToPlayer(GameWorld.OnlinePlayer player) {
		if (player != null) {
			showToPlayer(player::send);
		}
	}

	/**
	 * Envia um anuncio em broadcast para todos os jogadores online.
	 */
	public void announceToAll(GameWorld world, String text) {
		if (world != null && text != null && !text.isBlank()) {
			world.broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "Announce", text.trim()), p -> true);
		}
	}
}
