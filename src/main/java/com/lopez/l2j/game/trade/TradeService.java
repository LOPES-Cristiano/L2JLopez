package com.lopez.l2j.game.trade;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.InventoryUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.ItemInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servico de trocas diretas entre jogadores (Trade):
 * - V.23: Distancia maxima de 150 unidades (22500 distSq)
 * - V.24: Trava de troca em combate, morte, loja privada ou olimpiadas
 * - V.25: Bloqueio de itens nao-negociaveis (equipados, quests, shadow, heroicos)
 * - V.26: Commit atomico com lock duplo ordenado por objectId contra duplicacao concorrente
 */
@Service
public class TradeService {

	private static final Logger log = LoggerFactory.getLogger(TradeService.class);
	public static final int MAX_TRADE_DISTANCE = 150;
	public static final double MAX_TRADE_DIST_SQ = 150.0 * 150.0; // 22500

	public record TradeOffer(int itemObjectId, int count) {}

	public static class TradeSession {
		private final PlayerCharacter player1;
		private final PlayerCharacter player2;
		private final List<TradeOffer> offers1 = new ArrayList<>();
		private final List<TradeOffer> offers2 = new ArrayList<>();
		private volatile boolean locked1;
		private volatile boolean locked2;
		private volatile boolean confirmed1;
		private volatile boolean confirmed2;
		private final long startTime;

		public TradeSession(PlayerCharacter player1, PlayerCharacter player2) {
			this.player1 = player1;
			this.player2 = player2;
			this.startTime = System.currentTimeMillis();
		}

		public PlayerCharacter player1() { return player1; }
		public PlayerCharacter player2() { return player2; }
		public List<TradeOffer> offers1() { return offers1; }
		public List<TradeOffer> offers2() { return offers2; }
		public boolean locked1() { return locked1; }
		public boolean locked2() { return locked2; }
		public boolean confirmed1() { return confirmed1; }
		public boolean confirmed2() { return confirmed2; }

		public PlayerCharacter getPartner(PlayerCharacter player) {
			return player.objectId() == player1.objectId() ? player2 : player1;
		}

		public List<TradeOffer> getOffers(PlayerCharacter player) {
			return player.objectId() == player1.objectId() ? offers1 : offers2;
		}

		public void resetConfirmations() {
			this.locked1 = false;
			this.locked2 = false;
			this.confirmed1 = false;
			this.confirmed2 = false;
		}
	}

	private final Map<Integer, Integer> pendingRequests = new ConcurrentHashMap<>();
	private final Map<Integer, TradeSession> activeTrades = new ConcurrentHashMap<>();
	private final InventoryService inventoryService;

	public TradeService() {
		this(null);
	}

	public TradeService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	public boolean isTrading(int playerObjectId) {
		return activeTrades.containsKey(playerObjectId);
	}

	public Optional<TradeSession> getTrade(int playerObjectId) {
		return Optional.ofNullable(activeTrades.get(playerObjectId));
	}

	/**
	 * V.23 & V.24: Valida e inicia pedido de troca de um jogador para outro.
	 */
	public boolean requestTrade(PlayerCharacter requester, PlayerCharacter partner, Consumer<GameServerPacket> packetSender) {
		if (requester == null || partner == null || requester.objectId() == partner.objectId()) {
			if (packetSender != null) packetSender.accept(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return false;
		}

		// V.24: Trava em combate ou morte
		if (requester.isDead() || partner.isDead()) {
			if (packetSender != null) packetSender.accept(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return false;
		}
		if (requester.isInCombat() || partner.isInCombat()) {
			if (packetSender != null) {
				packetSender.accept(SystemMessage.of(SystemMessage.S1_IS_BUSY_TRY_LATER, new SystemMessage.Text(partner.name())));
			}
			return false;
		}
		if (!com.lopez.l2j.config.Config.ALT_KARMA_PLAYER_CAN_TRADE && (requester.karma() > 0 || partner.karma() > 0)) {
			if (packetSender != null) packetSender.accept(SystemMessage.id(SystemMessage.TARGET_IS_INCORRECT));
			return false;
		}

		// V.23: Distancia maxima de 150 unidades
		double dx = requester.x() - partner.x();
		double dy = requester.y() - partner.y();
		if (dx * dx + dy * dy > MAX_TRADE_DIST_SQ) {
			if (packetSender != null) packetSender.accept(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
			return false;
		}

		// Ja em troca ou transacao
		if (isTrading(requester.objectId()) || isTrading(partner.objectId())) {
			if (packetSender != null) packetSender.accept(SystemMessage.id(SystemMessage.ALREADY_TRADING));
			return false;
		}

		pendingRequests.put(requester.objectId(), partner.objectId());
		return true;
	}

	/**
	 * Resposta ao pedido de trade.
	 */
	public Optional<TradeSession> answerTrade(PlayerCharacter partner, PlayerCharacter requester, boolean accept,
			Consumer<GameServerPacket> partnerSender, Consumer<GameServerPacket> requesterSender) {
		if (requester == null || partner == null) {
			return Optional.empty();
		}

		Integer requestedPartnerId = pendingRequests.remove(requester.objectId());
		if (requestedPartnerId == null || requestedPartnerId != partner.objectId()) {
			return Optional.empty();
		}

		if (!accept) {
			if (requesterSender != null) requesterSender.accept(SystemMessage.id(SystemMessage.TRADE_CANCELLED));
			return Optional.empty();
		}

		// Revalida condicoes no aceite (V.23 e V.24)
		if (requester.isDead() || partner.isDead() || requester.isInCombat() || partner.isInCombat()) {
			if (requesterSender != null) requesterSender.accept(SystemMessage.id(SystemMessage.TRADE_CANCELLED));
			if (partnerSender != null) partnerSender.accept(SystemMessage.id(SystemMessage.TRADE_CANCELLED));
			return Optional.empty();
		}

		double dx = requester.x() - partner.x();
		double dy = requester.y() - partner.y();
		if (dx * dx + dy * dy > MAX_TRADE_DIST_SQ) {
			if (requesterSender != null) requesterSender.accept(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
			if (partnerSender != null) partnerSender.accept(SystemMessage.id(SystemMessage.TARGET_TOO_FAR));
			return Optional.empty();
		}

		TradeSession session = new TradeSession(requester, partner);
		activeTrades.put(requester.objectId(), session);
		activeTrades.put(partner.objectId(), session);

		return Optional.of(session);
	}

	/**
	 * V.25: Adiciona um item a oferta de troca apos validar negociabilidade.
	 */
	public boolean addItem(PlayerCharacter player, int itemObjectId, int count, Consumer<GameServerPacket> sender) {
		if (player == null || count <= 0) {
			return false;
		}
		TradeSession session = activeTrades.get(player.objectId());
		if (session == null || session.locked1 || session.locked2) {
			return false;
		}

		Inventory inv = player.inventory();
		if (inv == null) {
			return false;
		}

		var itemOpt = inv.byObjectId(itemObjectId);
		if (itemOpt.isEmpty()) {
			return false;
		}

		ItemInstance item = itemOpt.get();

		// V.25: Validacoes de item negociavel
		if (item.isEquipped()) {
			return false;
		}
		if (!item.template().isTradeable()) {
			return false;
		}
		if (item.count() < count) {
			return false;
		}

		synchronized (session) {
			List<TradeOffer> offers = session.getOffers(player);
			// Verifica se ja esta na lista
			for (int i = 0; i < offers.size(); i++) {
				if (offers.get(i).itemObjectId() == itemObjectId) {
					int newCount = offers.get(i).count() + count;
					if (newCount > item.count()) {
						return false;
					}
					offers.set(i, new TradeOffer(itemObjectId, newCount));
					session.resetConfirmations();
					return true;
				}
			}
			offers.add(new TradeOffer(itemObjectId, count));
			session.resetConfirmations();
		}

		return true;
	}

	/**
	 * Trava a oferta (Lock).
	 */
	public boolean lockTrade(PlayerCharacter player) {
		TradeSession session = activeTrades.get(player.objectId());
		if (session == null) {
			return false;
		}
		synchronized (session) {
			if (player.objectId() == session.player1.objectId()) {
				session.locked1 = true;
			} else {
				session.locked2 = true;
			}
		}
		return true;
	}

	/**
	 * V.26: Confirma a troca (ambos confirmam -> Atomic Trade Commit).
	 */
	public boolean confirmTrade(PlayerCharacter player, Consumer<GameServerPacket> p1Sender, Consumer<GameServerPacket> p2Sender) {
		TradeSession session = activeTrades.get(player.objectId());
		if (session == null) {
			return false;
		}

		synchronized (session) {
			if (!session.locked1 || !session.locked2) {
				return false;
			}
			if (player.objectId() == session.player1.objectId()) {
				session.confirmed1 = true;
			} else {
				session.confirmed2 = true;
			}

			if (session.confirmed1 && session.confirmed2) {
				return executeAtomicCommit(session, p1Sender, p2Sender);
			}
		}

		return true;
	}

	/**
	 * V.26: Execucao Atomica com lock duplo ordenado por objectId.
	 */
	private boolean executeAtomicCommit(TradeSession session, Consumer<GameServerPacket> p1Sender, Consumer<GameServerPacket> p2Sender) {
		PlayerCharacter p1 = session.player1;
		PlayerCharacter p2 = session.player2;

		// Lock duplo ordenado para eliminar deadlocks e race conditions
		PlayerCharacter firstLock = p1.objectId() < p2.objectId() ? p1 : p2;
		PlayerCharacter secondLock = p1.objectId() < p2.objectId() ? p2 : p1;

		synchronized (firstLock) {
			synchronized (secondLock) {
				// Revalidacao final
				if (p1.isDead() || p2.isDead()) {
					cancelTrade(p1, p1Sender, p2Sender);
					return false;
				}

				double dx = p1.x() - p2.x();
				double dy = p1.y() - p2.y();
				if (dx * dx + dy * dy > MAX_TRADE_DIST_SQ) {
					cancelTrade(p1, p1Sender, p2Sender);
					return false;
				}

				Inventory inv1 = p1.inventory();
				Inventory inv2 = p2.inventory();
				if (inv1 == null || inv2 == null) {
					cancelTrade(p1, p1Sender, p2Sender);
					return false;
				}

				// Verifica presenca e saldo de todos os itens de p1
				for (TradeOffer offer : session.offers1) {
					var itemOpt = inv1.byObjectId(offer.itemObjectId());
					if (itemOpt.isEmpty() || itemOpt.get().count() < offer.count()) {
						cancelTrade(p1, p1Sender, p2Sender);
						return false;
					}
				}

				// Verifica presenca e saldo de todos os itens de p2
				for (TradeOffer offer : session.offers2) {
					var itemOpt = inv2.byObjectId(offer.itemObjectId());
					if (itemOpt.isEmpty() || itemOpt.get().count() < offer.count()) {
						cancelTrade(p1, p1Sender, p2Sender);
						return false;
					}
				}

				List<ItemInfo> updates1 = new ArrayList<>();
				List<ItemInfo> updates2 = new ArrayList<>();

				// Transferencia de P1 -> P2
				for (TradeOffer offer : session.offers1) {
					var itemOpt = inv1.byObjectId(offer.itemObjectId());
					if (itemOpt.isPresent()) {
						ItemInstance src = itemOpt.get();
						int itemId = src.itemId();
						var consumed = inventoryService.destroyItem(inv1, offer.itemObjectId(), offer.count(), "Trade");
						if (consumed != null) {
							updates1.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var added = inventoryService.addItem(inv2, itemId, offer.count(), "Trade");
						if (added != null) {
							if (added.allItems() != null && !added.allItems().isEmpty()) {
								for (var it : added.allItems()) {
									updates2.add(ItemInfo.of(it, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
								}
							} else {
								updates2.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							}
						}
					}
				}

				// Transferencia de P2 -> P1
				for (TradeOffer offer : session.offers2) {
					var itemOpt = inv2.byObjectId(offer.itemObjectId());
					if (itemOpt.isPresent()) {
						ItemInstance src = itemOpt.get();
						int itemId = src.itemId();
						var consumed = inventoryService.destroyItem(inv2, offer.itemObjectId(), offer.count(), "Trade");
						if (consumed != null) {
							updates2.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var added = inventoryService.addItem(inv1, itemId, offer.count(), "Trade");
						if (added != null) {
							if (added.allItems() != null && !added.allItems().isEmpty()) {
								for (var it : added.allItems()) {
									updates1.add(ItemInfo.of(it, added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
								}
							} else {
								updates1.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							}
						}
					}
				}

				// Limpeza de sessao ativa
				activeTrades.remove(p1.objectId());
				activeTrades.remove(p2.objectId());

				if (p1Sender != null) {
					if (!updates1.isEmpty()) p1Sender.accept(new InventoryUpdate(updates1));
					p1Sender.accept(SystemMessage.id(SystemMessage.TRADE_SUCCESSFUL));
				}
				if (p2Sender != null) {
					if (!updates2.isEmpty()) p2Sender.accept(new InventoryUpdate(updates2));
					p2Sender.accept(SystemMessage.id(SystemMessage.TRADE_SUCCESSFUL));
				}

				return true;
			}
		}
	}

	/**
	 * Cancela a sessao de troca atual.
	 */
	public void cancelTrade(PlayerCharacter player, Consumer<GameServerPacket> p1Sender, Consumer<GameServerPacket> p2Sender) {
		TradeSession session = activeTrades.remove(player.objectId());
		if (session != null) {
			activeTrades.remove(session.player1.objectId());
			activeTrades.remove(session.player2.objectId());
			if (p1Sender != null) p1Sender.accept(SystemMessage.id(SystemMessage.TRADE_CANCELLED));
			if (p2Sender != null) p2Sender.accept(SystemMessage.id(SystemMessage.TRADE_CANCELLED));
		}
	}

	/**
	 * V.23: Validacao de distancia em movimento (cancela se afastar > 150 unidades).
	 */
	public void checkMovementDistance(PlayerCharacter movingPlayer, Consumer<GameServerPacket> p1Sender, Consumer<GameServerPacket> p2Sender) {
		TradeSession session = activeTrades.get(movingPlayer.objectId());
		if (session != null) {
			PlayerCharacter partner = session.getPartner(movingPlayer);
			double dx = movingPlayer.x() - partner.x();
			double dy = movingPlayer.y() - partner.y();
			if (dx * dx + dy * dy > MAX_TRADE_DIST_SQ) {
				cancelTrade(movingPlayer, p1Sender, p2Sender);
			}
		}
	}
}
