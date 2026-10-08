package com.lopez.l2j.game.service;

import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de gerenciamento de status ausente (.away / .back).
 * Inspiração L2JDream V2 / L2jFrozen:
 * - Coloca o jogador em modo AFK alterando seu título para [AFK] ou [AFK - motivo].
 * - Bloqueia solicitações de Party, Trade e Duelos enquanto estiver ausente.
 * - Desativa automaticamente ao se mover, atacar ou digitar .back.
 */
@Service
public class AwayStatusService {

	private static final Logger log = LoggerFactory.getLogger(AwayStatusService.class);

	public record AwayData(String originalTitle, long startTime, String reason) {
	}

	private final Map<Integer, AwayData> awayMap = new ConcurrentHashMap<>();

	public boolean isAway(int playerObjectId) {
		return awayMap.containsKey(playerObjectId);
	}

	public AwayData getAwayData(int playerObjectId) {
		return awayMap.get(playerObjectId);
	}

	/**
	 * Coloca o jogador no modo ausente (.away [motivo]).
	 */
	public boolean setAway(GameSession session, String reason) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();

		if (isAway(active.objectId())) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce ja esta no modo ausente. Digite .back para retornar."));
			return false;
		}

		if (active.isDead()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao pode ficar ausente estando morto!"));
			return false;
		}

		if (active.isInCombat()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao pode ficar ausente enquanto estiver em combate!"));
			return false;
		}

		if (active.pvpFlag() > 0 || active.karma() > 0) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao pode ficar ausente com flag ou karma ativo!"));
			return false;
		}

		if (active.isOlympiadMode()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao pode ficar ausente nas Olimpiadas!"));
			return false;
		}

		if (active.isFishing()) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao pode ficar ausente enquanto estiver pescando!"));
			return false;
		}

		String origTitle = active.title() != null ? active.title() : "";
		String cleanReason = (reason != null && !reason.isBlank()) ? reason.trim() : "";
		String afkTitle = cleanReason.isEmpty() ? "[AFK]" : "[AFK - " + cleanReason + "]";

		// Limita tamanho do titulo do cliente L2 (max 16 caracteres para caber sem truncar)
		if (afkTitle.length() > 20) {
			afkTitle = afkTitle.substring(0, 20);
		}

		awayMap.put(active.objectId(), new AwayData(origTitle, System.currentTimeMillis(), cleanReason));
		active.title(afkTitle);
		session.broadcastAppearance();

		session.send(new CreatureSay(0, CreatureSay.ALL, "AFK",
				"Modo ausente ATIVADO. Digite .back ou mova-se para retornar."));
		log.info("Player {} entrou em modo AFK (motivo: '{}')", active.name(), cleanReason);
		return true;
	}

	/**
	 * Remove o jogador do modo ausente (.back).
	 */
	public boolean setBack(GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		var active = session.activeCharacter();
		AwayData data = awayMap.remove(active.objectId());
		if (data == null) {
			session.send(new CreatureSay(0, CreatureSay.ALL, "AFK", "Voce nao esta no modo ausente."));
			return false;
		}

		active.title(data.originalTitle());
		session.broadcastAppearance();

		long durationSec = (System.currentTimeMillis() - data.startTime()) / 1000;
		session.send(new CreatureSay(0, CreatureSay.ALL, "AFK",
				String.format("Voce retornou do modo ausente apos %d segundo(s). Bem-vindo de volta!", durationSec)));
		log.info("Player {} saiu do modo AFK apos {}s", active.name(), durationSec);
		return true;
	}

	/**
	 * Verifica e desativa o modo AFK automaticamente ao realizar acao de jogo (mover, atacar, skill).
	 */
	public boolean checkAndRemoveAway(GameSession session) {
		if (session == null || session.activeCharacter() == null) {
			return false;
		}
		if (isAway(session.activeCharacter().objectId())) {
			return setBack(session);
		}
		return false;
	}
}
