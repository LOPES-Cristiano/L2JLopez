package com.lopez.l2j.game.event;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servico da Loteria Oficial de Aden (LotteryManager de L2JDreamV2 / L2JLucera2).
 * Conecta AltLotteryPrize, AltLotteryTicketPrice, AltLottery5NumberRate,
 * AltLottery4NumberRate, AltLottery3NumberRate e AltLottery2and1NumberPrize de fun_events.properties.
 */
@Service
public class LotteryService {

	private static final Logger log = LoggerFactory.getLogger(LotteryService.class);
	public static final int ADENA_ITEM_ID = 57;
	public static final int LOTTERY_TICKET_ITEM_ID = 4442;
	public static final int NUMBERS_PER_TICKET = 5;
	public static final int MAX_LOTTERY_NUMBER = 20;

	public record LotteryTicket(int roundNumber, int ownerId, Set<Integer> chosenNumbers) {}
	public record DrawSummary(int roundNumber, Set<Integer> winningNumbers, long totalPrizePool,
	                          long tier5Prize, long tier4Prize, long tier3Prize, long tier2And1Prize) {}

	private final InventoryService inventoryService;
	private final AtomicInteger roundNumber = new AtomicInteger(1);
	private final AtomicLong currentPrizePool = new AtomicLong(getInitialLotteryPrize());
	private final List<LotteryTicket> activeTickets = new CopyOnWriteArrayList<>();
	private volatile Set<Integer> lastWinningNumbers = Set.of();

	public LotteryService() {
		this(null);
	}

	@Autowired
	public LotteryService(@Autowired(required = false) InventoryService inventoryService) {
		this.inventoryService = inventoryService;
		this.currentPrizePool.set(getInitialLotteryPrize());
	}

	public int getInitialLotteryPrize() {
		return Config.ALT_LOTTERY_PRIZE > 0 ? Config.ALT_LOTTERY_PRIZE : 50_000;
	}

	public int getTicketPrice() {
		return Config.ALT_LOTTERY_TICKET_PRICE > 0 ? Config.ALT_LOTTERY_TICKET_PRICE : 2_000;
	}

	public double getFiveNumberRate() {
		return Config.ALT_LOTTERY_5_NUMBER_RATE > 0 ? Config.ALT_LOTTERY_5_NUMBER_RATE : 0.6;
	}

	public double getFourNumberRate() {
		return Config.ALT_LOTTERY_4_NUMBER_RATE > 0 ? Config.ALT_LOTTERY_4_NUMBER_RATE : 0.2;
	}

	public double getThreeNumberRate() {
		return Config.ALT_LOTTERY_3_NUMBER_RATE > 0 ? Config.ALT_LOTTERY_3_NUMBER_RATE : 0.2;
	}

	public int getTwoAndOneNumberPrize() {
		return Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE >= 0 ? Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE : 200;
	}

	public int getCurrentRound() {
		return roundNumber.get();
	}

	public long getCurrentPrizePool() {
		return currentPrizePool.get();
	}

	public List<LotteryTicket> getActiveTickets() {
		return Collections.unmodifiableList(activeTickets);
	}

	public Set<Integer> getLastWinningNumbers() {
		return lastWinningNumbers;
	}

	public synchronized boolean buyTicket(PlayerCharacter player, Set<Integer> chosenNumbers) {
		if (player == null || chosenNumbers == null || chosenNumbers.size() != NUMBERS_PER_TICKET) {
			return false;
		}
		for (int n : chosenNumbers) {
			if (n < 1 || n > MAX_LOTTERY_NUMBER) {
				return false;
			}
		}
		int price = getTicketPrice();
		if (player.inventory() != null) {
			if (player.inventory().getAdena() < price) {
				return false;
			}
			if (inventoryService != null) {
				inventoryService.consumeItem(player.inventory(), ADENA_ITEM_ID, price, "LotteryTicketBuy");
			} else {
				player.inventory().reduceAdena(price);
			}
		}

		activeTickets.add(new LotteryTicket(roundNumber.get(), player.objectId(), Set.copyOf(chosenNumbers)));
		currentPrizePool.addAndGet(price);
		log.info("LotteryService: Jogador {} comprou bilhete na rodada #{} ({}). Premio acumulado: {}",
				player.name(), roundNumber.get(), chosenNumbers, currentPrizePool.get());
		return true;
	}

	public long calculatePrizeForMatches(int matchedNumbers, long totalPrizePool) {
		return switch (matchedNumbers) {
			case 5 -> Math.round(totalPrizePool * getFiveNumberRate());
			case 4 -> Math.round(totalPrizePool * getFourNumberRate());
			case 3 -> Math.round(totalPrizePool * getThreeNumberRate());
			case 2, 1 -> getTwoAndOneNumberPrize();
			default -> 0L;
		};
	}

	public int countMatchingNumbers(Set<Integer> ticketNumbers, Set<Integer> winningNumbers) {
		if (ticketNumbers == null || winningNumbers == null) {
			return 0;
		}
		int matches = 0;
		for (int n : ticketNumbers) {
			if (winningNumbers.contains(n)) {
				matches++;
			}
		}
		return matches;
	}

	public synchronized DrawSummary finishRoundAndDraw() {
		Set<Integer> winning = new HashSet<>();
		while (winning.size() < NUMBERS_PER_TICKET) {
			winning.add(ThreadLocalRandom.current().nextInt(1, MAX_LOTTERY_NUMBER + 1));
		}
		this.lastWinningNumbers = Collections.unmodifiableSet(winning);
		long pool = currentPrizePool.get();
		int finishedRound = roundNumber.getAndIncrement();

		DrawSummary summary = new DrawSummary(
				finishedRound,
				this.lastWinningNumbers,
				pool,
				calculatePrizeForMatches(5, pool),
				calculatePrizeForMatches(4, pool),
				calculatePrizeForMatches(3, pool),
				calculatePrizeForMatches(2, pool));

		activeTickets.clear();
		currentPrizePool.set(getInitialLotteryPrize());
		log.info("LotteryService: Rodada #{} sorteada! Numeros: {}, Pool: {}", finishedRound, winning, pool);
		return summary;
	}
}
