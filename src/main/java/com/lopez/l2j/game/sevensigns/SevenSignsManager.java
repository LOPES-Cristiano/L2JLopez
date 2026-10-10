package com.lopez.l2j.game.sevensigns;

import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Gerenciador dos Sete Selos (Seven Signs: Cabals of Dawn and Dusk,
 * Selos da Avareza, Gnose e Luta, Festival of Darkness, pedras de selo e Ancient Adena).
 * Implementado com paridade absoluta ao Lineage II Interlude (Dream / Lucera).
 */
@Service
public class SevenSignsManager implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(SevenSignsManager.class);

	// Cabais
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;

	// Selos
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;

	// Periodos do Ciclo
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;

	// Constantes de Horario e Duracao
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 0;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	public static final long PERIOD_MINOR_LENGTH = 900_000L; // 15 minutos (preparacao e apuracao)
	public static final long PERIOD_MAJOR_LENGTH = 604_800_000L - PERIOD_MINOR_LENGTH; // ~7 dias

	// Itens de Pedras de Selo e Recompensas
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	public static final int ANCIENT_ADENA_ID = 5575;
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int CERTIFICATE_OF_APPROVAL_ID = 6388;
	public static final int BLOOD_OFFERING_ID = 5901;

	// Conversao de Pedras em Ancient Adena e Pontuacao
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;

	// NPCs Especiais dos 7 Selos
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int SPIRIT_IN_ID = 31111;
	public static final int SPIRIT_OUT_ID = 31112;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;

	// Festival of Darkness
	public static final int FESTIVAL_COUNT = 5;
	public static final int[] FESTIVAL_LEVEL_SCORES = { 60, 70, 100, 120, 150 };
	public static final int[] FESTIVAL_MAX_LEVELS = { 31, 42, 53, 64, 80 };
	public static final int[][] FESTIVAL_FEE_STONES = {
			{ 900, 540, 270 },
			{ 1500, 900, 450 },
			{ 3000, 1800, 900 },
			{ 4500, 2700, 1350 },
			{ 6000, 3600, 1800 }
	};
	public static final int[][] FESTIVAL_ARENA_LOCS = {
			{ -76158, 111585, -4901 },
			{ -76158, 86874, -5157 },
			{ -74158, 111585, -4901 },
			{ -74158, 86874, -5157 },
			{ -72158, 111585, -4901 }
	};

	// Skills de Strife
	public static final int SKILL_STRIFE_DAWN_BUFF = 5074; // +10% CP
	public static final int SKILL_STRIFE_DUSK_CURSE = 5075; // -10% CP

	public record PlayerData(int charId, int cabal, String oldCabal, int seal,
			int redStones, int greenStones, int blueStones, int ancientAdena, int contributionScore) {
		public PlayerData(int charId, int cabal, int seal, int redStones, int greenStones, int blueStones, int ancientAdena) {
			this(charId, cabal, "", seal, redStones, greenStones, blueStones, ancientAdena, 0);
		}
	}

	public record FestivalRecord(int festivalId, int cabal, int cycle, long date, int score, String members, String names) {
	}

	public record MammonLocation(String dungeonName, int x, int y, int z) {
	}

	public enum DungeonAccess {
		ALLOWED,
		NOT_REGISTERED,
		NOT_IN_VALIDATION_WINNER,
		SEAL_NOT_OWNED
	}

	// Locais de Mammon Blacksmith (Catacumbas)
	public static final List<MammonLocation> MAMMON_BLACKSMITH_LOCS = List.of(
			new MammonLocation("Catacomb of Dark Omens", -53131, -250502, -7909),
			new MammonLocation("Catacomb of the Forbidden Path", -20485, -251008, -8165),
			new MammonLocation("Catacomb of the Apostate", 46303, 170091, -4981),
			new MammonLocation("Catacomb of the Witch", 140519, 79464, -5429),
			new MammonLocation("Catacomb of the Heretic", -19360, 13278, -4901),
			new MammonLocation("Catacomb of the Branded", 12669, -248698, -9581)
	);

	// Locais de Mammon Merchant (Necropoles)
	public static final List<MammonLocation> MAMMON_MERCHANT_LOCS = List.of(
			new MammonLocation("Patriot's Necropolis", -21657, 77164, -5173),
			new MammonLocation("Saint's Necropolis", 83175, 208998, -5439),
			new MammonLocation("Disciple's Necropolis", 172373, -17833, -4901),
			new MammonLocation("Worshipers Necropolis", 111337, 173804, -5439),
			new MammonLocation("Martyr's Necropolis", 118343, 132578, -4831),
			new MammonLocation("Heretics Necropolis", 45029, 123802, -5413),
			new MammonLocation("Sacrifice Necropolis", -41350, 209876, -5087),
			new MammonLocation("Pilgrim's Necropolis", -52172, 78884, -4741)
	);

	public static final int SANCTUM_BOSS_X = 184464;
	public static final int SANCTUM_BOSS_Y = -13104;
	public static final int SANCTUM_BOSS_Z = -4900;
	public static final int SANCTUM_BOSS_HEADING = 0;

	private final Map<Integer, PlayerData> players = new ConcurrentHashMap<>();
	private final Map<String, FestivalRecord> festivalRecords = new ConcurrentHashMap<>();
	private final JdbcClient jdbc;
	private final GameWorld world;
	private final com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates;

	private int currentCycle = 1;
	private int festivalCycle = 1;
	private int activePeriod = PERIOD_COMPETITION;
	private int previousWinner = CABAL_NULL;
	private int date = 1;

	private long dawnStoneScore = 0;
	private long duskStoneScore = 0;
	private int dawnFestivalScore = 0;
	private int duskFestivalScore = 0;

	private final int[] dawnFestivalIdScore = new int[FESTIVAL_COUNT];
	private final int[] duskFestivalIdScore = new int[FESTIVAL_COUNT];
	private final int[] accumulatedBonuses = new int[FESTIVAL_COUNT];

	private int avariceOwner = CABAL_NULL;
	private int gnosisOwner = CABAL_NULL;
	private int strifeOwner = CABAL_NULL;

	private int avariceDawnScore = 0;
	private int gnosisDawnScore = 0;
	private int strifeDawnScore = 0;
	private int avariceDuskScore = 0;
	private int duskGnosisScore = 0;
	private int duskStrifeScore = 0;

	private volatile int currentBlacksmithLocIndex = 0;
	private volatile int currentMerchantLocIndex = 0;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "SevenSigns-Scheduler");
		t.setDaemon(true);
		return t;
	});
	private ScheduledFuture<?> periodTimer;
	private ScheduledFuture<?> mammonTimer;

	@Autowired
	public SevenSignsManager(@Autowired(required = false) JdbcClient jdbc,
	                         @Autowired(required = false) GameWorld world,
	                         @Autowired(required = false) com.lopez.l2j.game.npc.NpcTemplateTable npcTemplates) {
		this.jdbc = jdbc;
		this.world = world;
		this.npcTemplates = npcTemplates;
	}

	public SevenSignsManager(JdbcClient jdbc, GameWorld world) {
		this(jdbc, world, null);
	}

	public SevenSignsManager(JdbcClient jdbc) {
		this(jdbc, null, null);
	}

	@PostConstruct
	public void init() {
		loadFromDb();
		recalculateSealTotals();
		if (activePeriod == PERIOD_SEAL_VALIDATION) {
			handleBossSpawnsOnValidation();
		}
		scheduleNextPeriodChange();
		scheduleMammonRelocation();
	}

	@PreDestroy
	@Override
	public void close() {
		if (periodTimer != null) {
			periodTimer.cancel(false);
		}
		if (mammonTimer != null) {
			mammonTimer.cancel(false);
		}
		scheduler.shutdownNow();
		persistStatus();
	}

	public int currentCycle() {
		return currentCycle;
	}

	public void currentCycle(int currentCycle) {
		this.currentCycle = currentCycle;
	}

	public int festivalCycle() {
		return festivalCycle;
	}

	public void festivalCycle(int festivalCycle) {
		this.festivalCycle = festivalCycle;
	}

	public int activePeriod() {
		return activePeriod;
	}

	public void activePeriod(int activePeriod) {
		this.activePeriod = activePeriod;
	}

	public int previousWinner() {
		return previousWinner;
	}

	public void previousWinner(int previousWinner) {
		this.previousWinner = previousWinner;
	}

	public long dawnStoneScore() {
		return dawnStoneScore;
	}

	public long duskStoneScore() {
		return duskStoneScore;
	}

	public int dawnFestivalScore() {
		return dawnFestivalScore;
	}

	public int duskFestivalScore() {
		return duskFestivalScore;
	}

	public int avariceOwner() {
		return avariceOwner;
	}

	public int gnosisOwner() {
		return gnosisOwner;
	}

	public int strifeOwner() {
		return strifeOwner;
	}

	public boolean isSealValidationPeriod() {
		return activePeriod == PERIOD_SEAL_VALIDATION;
	}

	public boolean isCompResultsPeriod() {
		return activePeriod == PERIOD_COMP_RESULTS;
	}

	public boolean isCompetitionPeriod() {
		return activePeriod == PERIOD_COMPETITION;
	}

	public boolean isRecruitingPeriod() {
		return activePeriod == PERIOD_COMP_RECRUITING;
	}

	public int getCurrentPeriod() {
		return activePeriod;
	}

	public int getCurrentCycle() {
		return currentCycle;
	}

	public int getSealOwner(int seal) {
		return switch (seal) {
			case SEAL_AVARICE -> avariceOwner;
			case SEAL_GNOSIS -> gnosisOwner;
			case SEAL_STRIFE -> strifeOwner;
			default -> CABAL_NULL;
		};
	}

	public void setSealOwner(int seal, int cabal) {
		switch (seal) {
			case SEAL_AVARICE -> avariceOwner = cabal;
			case SEAL_GNOSIS -> gnosisOwner = cabal;
			case SEAL_STRIFE -> strifeOwner = cabal;
		}
	}

	public int getSealProportion(int seal, int cabal) {
		if (cabal == CABAL_DAWN) {
			return switch (seal) {
				case SEAL_AVARICE -> avariceDawnScore;
				case SEAL_GNOSIS -> gnosisDawnScore;
				case SEAL_STRIFE -> strifeDawnScore;
				default -> 0;
			};
		} else if (cabal == CABAL_DUSK) {
			return switch (seal) {
				case SEAL_AVARICE -> avariceDuskScore;
				case SEAL_GNOSIS -> duskGnosisScore;
				case SEAL_STRIFE -> duskStrifeScore;
				default -> 0;
			};
		}
		return 0;
	}

	public int getTotalMembers(int cabal) {
		int count = 0;
		for (PlayerData p : players.values()) {
			if (p.cabal() == cabal) {
				count++;
			}
		}
		return count;
	}

	public Optional<PlayerData> getPlayerData(int charId) {
		return Optional.ofNullable(players.get(charId));
	}

	public int getPlayerCabal(int charId) {
		PlayerData data = players.get(charId);
		return data != null ? data.cabal() : CABAL_NULL;
	}

	public int getPlayerSeal(int charId) {
		PlayerData data = players.get(charId);
		return data != null ? data.seal() : SEAL_NULL;
	}

	public int getStoneScoreProp(int cabal) {
		double totalStoneScore = dawnStoneScore + duskStoneScore;
		if (totalStoneScore == 0) {
			return 0;
		}
		if (cabal == CABAL_DAWN) {
			return (int) Math.round(((double) dawnStoneScore / totalStoneScore) * 500);
		} else if (cabal == CABAL_DUSK) {
			return (int) Math.round(((double) duskStoneScore / totalStoneScore) * 500);
		}
		return 0;
	}

	public int getCurrentScore(int cabal) {
		if (cabal == CABAL_DAWN) {
			return getStoneScoreProp(CABAL_DAWN) + dawnFestivalScore;
		} else if (cabal == CABAL_DUSK) {
			return getStoneScoreProp(CABAL_DUSK) + duskFestivalScore;
		}
		return 0;
	}

	public int getCurrentFestivalScore(int cabal) {
		if (cabal == CABAL_DAWN) {
			return dawnFestivalScore;
		} else if (cabal == CABAL_DUSK) {
			return duskFestivalScore;
		}
		return 0;
	}

	public long getCurrentStoneScore(int cabal) {
		if (cabal == CABAL_DAWN) {
			return dawnStoneScore;
		} else if (cabal == CABAL_DUSK) {
			return duskStoneScore;
		}
		return 0;
	}

	public int getCabalHighestScore() {
		int dawnTotal = getCurrentScore(CABAL_DAWN);
		int duskTotal = getCurrentScore(CABAL_DUSK);
		if (dawnTotal > duskTotal) {
			return CABAL_DAWN;
		} else if (duskTotal > dawnTotal) {
			return CABAL_DUSK;
		}
		return CABAL_NULL;
	}

	public int getWinningCabal() {
		return getCabalHighestScore();
	}

	public int getSkyState() {
		if (activePeriod != PERIOD_SEAL_VALIDATION) {
			return 256; // Neutro
		}
		return switch (previousWinner) {
			case CABAL_DUSK -> 257; // Blood Red sky
			case CABAL_DAWN -> 258; // Blue Dawn sky
			default -> 256;
		};
	}

	public static String getCabalShortName(int cabal) {
		return switch (cabal) {
			case CABAL_DAWN -> "dawn";
			case CABAL_DUSK -> "dusk";
			default -> "";
		};
	}

	public static int getCabalNumber(String cabal) {
		if (cabal == null) {
			return CABAL_NULL;
		}
		String lower = cabal.trim().toLowerCase(java.util.Locale.ROOT);
		if ("dawn".equals(lower)) {
			return CABAL_DAWN;
		} else if ("dusk".equals(lower)) {
			return CABAL_DUSK;
		}
		return CABAL_NULL;
	}

	public synchronized boolean registerPlayer(int charId, int cabal, int seal) {
		if (cabal != CABAL_DAWN && cabal != CABAL_DUSK) {
			return false;
		}
		if (seal < SEAL_AVARICE || seal > SEAL_STRIFE) {
			seal = SEAL_AVARICE;
		}
		PlayerData existing = players.get(charId);

		// Incrementa estatisticas de voto nos selos
		if (cabal == CABAL_DAWN) {
			switch (seal) {
				case SEAL_AVARICE -> avariceDawnScore++;
				case SEAL_GNOSIS -> gnosisDawnScore++;
				case SEAL_STRIFE -> strifeDawnScore++;
			}
		} else {
			switch (seal) {
				case SEAL_AVARICE -> avariceDuskScore++;
				case SEAL_GNOSIS -> duskGnosisScore++;
				case SEAL_STRIFE -> duskStrifeScore++;
			}
		}

		PlayerData updated = new PlayerData(charId, cabal,
				existing != null ? existing.oldCabal() : "",
				seal,
				existing != null ? existing.redStones() : 0,
				existing != null ? existing.greenStones() : 0,
				existing != null ? existing.blueStones() : 0,
				existing != null ? existing.ancientAdena() : 0,
				existing != null ? existing.contributionScore() : 0);
		players.put(charId, updated);
		persistPlayerData(updated);
		persistStatus();
		return true;
	}

	public synchronized int contributeStones(int charId, int blue, int green, int red) {
		PlayerData existing = players.get(charId);
		if (existing == null || existing.cabal() == CABAL_NULL) {
			return 0;
		}

		int earnedAA = (blue * SEAL_STONE_BLUE_VALUE) + (green * SEAL_STONE_GREEN_VALUE) + (red * SEAL_STONE_RED_VALUE);
		long scoreGain = (blue * BLUE_CONTRIB_POINTS) + (green * GREEN_CONTRIB_POINTS) + (red * RED_CONTRIB_POINTS);

		if (existing.cabal() == CABAL_DAWN) {
			dawnStoneScore += scoreGain;
		} else if (existing.cabal() == CABAL_DUSK) {
			duskStoneScore += scoreGain;
		}

		PlayerData updated = new PlayerData(charId, existing.cabal(), existing.oldCabal(), existing.seal(),
				existing.redStones() + red,
				existing.greenStones() + green,
				existing.blueStones() + blue,
				existing.ancientAdena() + earnedAA,
				existing.contributionScore() + (int) scoreGain);
		players.put(charId, updated);

		persistPlayerData(updated);
		persistStatus();
		return earnedAA;
	}

	public synchronized int claimAncientAdena(int charId) {
		PlayerData existing = players.get(charId);
		if (existing == null || existing.ancientAdena() <= 0) {
			return 0;
		}
		int reward = existing.ancientAdena();
		PlayerData updated = new PlayerData(charId, existing.cabal(), existing.oldCabal(), existing.seal(),
				existing.redStones(), existing.greenStones(), existing.blueStones(), 0, existing.contributionScore());
		players.put(charId, updated);
		persistPlayerData(updated);
		return reward;
	}

	public synchronized void calcNewSealOwners() {
		int totalDawn = getTotalMembers(CABAL_DAWN);
		int totalDusk = getTotalMembers(CABAL_DUSK);
		int winningCabal = getCabalHighestScore();

		int[] seals = { SEAL_AVARICE, SEAL_GNOSIS, SEAL_STRIFE };
		for (int seal : seals) {
			int prevOwner = getSealOwner(seal);
			int newOwner = CABAL_NULL;

			int dawnProp = getSealProportion(seal, CABAL_DAWN);
			int duskProp = getSealProportion(seal, CABAL_DUSK);

			int dawnPercent = totalDawn > 0 ? Math.round((float) dawnProp / totalDawn * 100) : 0;
			int duskPercent = totalDusk > 0 ? Math.round((float) duskProp / totalDusk * 100) : 0;

			switch (prevOwner) {
				case CABAL_NULL -> {
					if (winningCabal == CABAL_DAWN && dawnPercent >= 35) {
						newOwner = CABAL_DAWN;
					} else if (winningCabal == CABAL_DUSK && duskPercent >= 35) {
						newOwner = CABAL_DUSK;
					}
				}
				case CABAL_DAWN -> {
					if (winningCabal == CABAL_DAWN) {
						if (dawnPercent >= 10) newOwner = CABAL_DAWN;
					} else if (winningCabal == CABAL_DUSK) {
						if (duskPercent >= 35) newOwner = CABAL_DUSK;
						else if (dawnPercent >= 10) newOwner = CABAL_DAWN;
					} else { // tie
						if (dawnPercent >= 10) newOwner = CABAL_DAWN;
					}
				}
				case CABAL_DUSK -> {
					if (winningCabal == CABAL_DUSK) {
						if (duskPercent >= 10) newOwner = CABAL_DUSK;
					} else if (winningCabal == CABAL_DAWN) {
						if (dawnPercent >= 35) newOwner = CABAL_DAWN;
						else if (duskPercent >= 10) newOwner = CABAL_DUSK;
					} else { // tie
						if (duskPercent >= 10) newOwner = CABAL_DUSK;
					}
				}
			}
			setSealOwner(seal, newOwner);
		}
	}

	public synchronized void advancePeriod() {
		final int periodEnded = activePeriod;
		activePeriod = (activePeriod + 1) % 4;

		log.info("SevenSignsManager: Transicao de periodo: {} -> {}", periodEnded, activePeriod);

		switch (periodEnded) {
			case PERIOD_COMP_RECRUITING -> {
				broadcastSystemMessage(1176); // COMPETITION_PERIOD_BEGUN
				broadcastAnnouncement("Seven Signs: The Competition period has begun!");
			}
			case PERIOD_COMPETITION -> {
				broadcastSystemMessage(1184); // RESULTS_PERIOD_BEGUN
				broadcastAnnouncement("Seven Signs: The Results calculation period has begun!");
				calcNewSealOwners();
			}
			case PERIOD_COMP_RESULTS -> {
				int compWinner = getCabalHighestScore();
				previousWinner = compWinner;
				if (compWinner == CABAL_DAWN) {
					broadcastSystemMessage(1264); // DAWN_WON
					broadcastAnnouncement("Seven Signs: The Lords of Dawn have won the Seven Signs!");
				} else if (compWinner == CABAL_DUSK) {
					broadcastSystemMessage(1265); // DUSK_WON
					broadcastAnnouncement("Seven Signs: The Revolutionaries of Dusk have won the Seven Signs!");
				} else {
					broadcastAnnouncement("Seven Signs: The Seven Signs competition ended in a tie!");
				}
				broadcastSystemMessage(1185); // VALIDATION_PERIOD_BEGUN

				applyStrifeCpModifiers();
				teleLosingCabalFromDungeons();
				relocateMammons();
				handleBossSpawnsOnValidation();
				broadcastSky();
			}
			case PERIOD_SEAL_VALIDATION -> {
				rewardHighestRanked();
				removeStrifeCpModifiers();
				despawnSanctumBosses();
				resetForNewCycle();
				currentCycle++;
				broadcastSystemMessage(1183); // PREPARATIONS_PERIOD_BEGUN
				broadcastAnnouncement("Seven Signs: Preparations for Cycle " + currentCycle + " have begun!");
				broadcastSky();
			}
		}

		persistStatus();
		scheduleNextPeriodChange();
	}

	public synchronized void changePeriodManually() {
		if (periodTimer != null) {
			periodTimer.cancel(false);
		}
		advancePeriod();
	}

	public synchronized void changePeriodManually(int period) {
		if (periodTimer != null) {
			periodTimer.cancel(false);
		}
		activePeriod = (period - 1 + 4) % 4;
		advancePeriod();
	}

	public int getDaysToPeriodChange() {
		Calendar cal = Calendar.getInstance();
		int numDays = cal.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		if (numDays < 0) {
			return -numDays;
		}
		return 7 - numDays;
	}

	public long getMilliToPeriodChange() {
		Calendar cal = Calendar.getInstance();
		long currTimeMillis = cal.getTimeInMillis();
		switch (activePeriod) {
			case PERIOD_COMP_RECRUITING, PERIOD_COMP_RESULTS -> {
				return PERIOD_MINOR_LENGTH;
			}
			case PERIOD_COMPETITION, PERIOD_SEAL_VALIDATION -> {
				int daysToChange = getDaysToPeriodChange();
				if (daysToChange == 7) {
					if (cal.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR
							|| (cal.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR && cal.get(Calendar.MINUTE) < PERIOD_START_MINS)) {
						daysToChange = 0;
					}
				}
				Calendar nextChange = Calendar.getInstance();
				if (daysToChange > 0) {
					nextChange.add(Calendar.DATE, daysToChange);
				}
				nextChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				nextChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				nextChange.set(Calendar.SECOND, 0);
				nextChange.set(Calendar.MILLISECOND, 0);
				long diff = nextChange.getTimeInMillis() - currTimeMillis;
				return diff > 0 ? diff : PERIOD_MAJOR_LENGTH;
			}
		}
		return PERIOD_MINOR_LENGTH;
	}

	private void scheduleNextPeriodChange() {
		if (periodTimer != null) {
			periodTimer.cancel(false);
		}
		long milli = getMilliToPeriodChange();
		log.info("SevenSignsManager: Proxima mudanca de periodo em {} minutos.", milli / 60000L);
		periodTimer = scheduler.schedule(this::advancePeriod, milli, TimeUnit.MILLISECONDS);
	}

	private void scheduleMammonRelocation() {
		if (mammonTimer != null) {
			mammonTimer.cancel(false);
		}
		// Realoca Mammons a cada 30 minutos
		mammonTimer = scheduler.scheduleAtFixedRate(this::relocateMammons, 30, 30, TimeUnit.MINUTES);
	}

	public synchronized void spawnMammons() {
		relocateMammons();
	}

	public synchronized void relocateMammons() {
		currentBlacksmithLocIndex = (currentBlacksmithLocIndex + 1) % MAMMON_BLACKSMITH_LOCS.size();
		currentMerchantLocIndex = (currentMerchantLocIndex + 1) % MAMMON_MERCHANT_LOCS.size();
		var bLoc = getMammonBlacksmithLocation();
		var mLoc = getMammonMerchantLocation();
		log.info("SevenSignsManager: Mammons realocados. Blacksmith: {}, Merchant: {}",
				bLoc.dungeonName(), mLoc.dungeonName());
		if (world != null) {
			for (var npc : world.npcs()) {
				if (npc.npcId() == MAMMON_BLACKSMITH_ID) {
					int oldX = npc.x();
					int oldY = npc.y();
					npc.x(bLoc.x());
					npc.y(bLoc.y());
					npc.z(bLoc.z());
					world.updateNpcPosition(npc, oldX, oldY);
				} else if (npc.npcId() == MAMMON_MERCHANT_ID) {
					int oldX = npc.x();
					int oldY = npc.y();
					npc.x(mLoc.x());
					npc.y(mLoc.y());
					npc.z(mLoc.z());
					world.updateNpcPosition(npc, oldX, oldY);
				}
			}
		}
	}

	public MammonLocation getMammonBlacksmithLocation() {
		return MAMMON_BLACKSMITH_LOCS.get(currentBlacksmithLocIndex % MAMMON_BLACKSMITH_LOCS.size());
	}

	public MammonLocation getMammonMerchantLocation() {
		return MAMMON_MERCHANT_LOCS.get(currentMerchantLocIndex % MAMMON_MERCHANT_LOCS.size());
	}

	public boolean shouldSpawnLilith() {
		return activePeriod == PERIOD_SEAL_VALIDATION
				&& previousWinner == CABAL_DUSK
				&& avariceOwner == CABAL_DUSK;
	}

	public boolean shouldSpawnAnakim() {
		return activePeriod == PERIOD_SEAL_VALIDATION
				&& previousWinner == CABAL_DAWN
				&& avariceOwner == CABAL_DAWN;
	}

	public boolean isMammonBlacksmithActive() {
		return activePeriod == PERIOD_SEAL_VALIDATION
				&& previousWinner != CABAL_NULL
				&& gnosisOwner == previousWinner;
	}

	public boolean isMammonMerchantActive() {
		return activePeriod == PERIOD_SEAL_VALIDATION
				&& previousWinner != CABAL_NULL
				&& avariceOwner == previousWinner;
	}

	public DungeonAccess checkDungeonEntry(int charId, boolean isNecropolis) {
		int cabal = getPlayerCabal(charId);
		if (activePeriod == PERIOD_COMPETITION) {
			if (cabal == CABAL_NULL) {
				return DungeonAccess.NOT_REGISTERED;
			}
			return DungeonAccess.ALLOWED;
		}
		if (activePeriod == PERIOD_SEAL_VALIDATION) {
			int winner = previousWinner;
			if (winner == CABAL_NULL || cabal != winner) {
				return DungeonAccess.NOT_IN_VALIDATION_WINNER;
			}
			int requiredSeal = isNecropolis ? SEAL_AVARICE : SEAL_GNOSIS;
			if (getSealOwner(requiredSeal) != winner) {
				return DungeonAccess.SEAL_NOT_OWNED;
			}
			return DungeonAccess.ALLOWED;
		}
		return cabal != CABAL_NULL ? DungeonAccess.ALLOWED : DungeonAccess.NOT_REGISTERED;
	}

	public synchronized void addFestivalScore(int cabal, int festivalId, int score, List<String> members) {
		if (festivalId < 0 || festivalId >= FESTIVAL_COUNT) {
			return;
		}
		String key = currentCycle + "_" + festivalId + "_" + cabal;
		FestivalRecord current = festivalRecords.get(key);
		if (current == null || score > current.score()) {
			String memStr = String.join(",", members);
			FestivalRecord rec = new FestivalRecord(festivalId, cabal, currentCycle, System.currentTimeMillis(), score, memStr, memStr);
			festivalRecords.put(key, rec);
			persistFestivalRecord(rec);

			// Recalcula pontuacao do festival para os cabais
			int highestDusk = getHighestScore(CABAL_DUSK, festivalId);
			int highestDawn = getHighestScore(CABAL_DAWN, festivalId);

			if (highestDusk > highestDawn) {
				duskFestivalIdScore[festivalId] = 100;
				dawnFestivalIdScore[festivalId] = 0;
			} else if (highestDawn > highestDusk) {
				dawnFestivalIdScore[festivalId] = 100;
				duskFestivalIdScore[festivalId] = 0;
			} else {
				dawnFestivalIdScore[festivalId] = 0;
				duskFestivalIdScore[festivalId] = 0;
			}

			dawnFestivalScore = Arrays.stream(dawnFestivalIdScore).sum();
			duskFestivalScore = Arrays.stream(duskFestivalIdScore).sum();
			persistStatus();
		}
	}

	public int getHighestScore(int cabal, int festivalId) {
		String key = currentCycle + "_" + festivalId + "_" + cabal;
		FestivalRecord rec = festivalRecords.get(key);
		return rec != null ? rec.score() : 0;
	}

	public List<String> getHighestScoreMembers(int cabal, int festivalId) {
		String key = currentCycle + "_" + festivalId + "_" + cabal;
		FestivalRecord rec = festivalRecords.get(key);
		if (rec != null && rec.members() != null && !rec.members().isBlank()) {
			return Arrays.asList(rec.members().split(","));
		}
		return List.of();
	}

	public int getAccumulatedBonus(int festivalId) {
		return (festivalId >= 0 && festivalId < FESTIVAL_COUNT) ? accumulatedBonuses[festivalId] : 0;
	}

	public void addAccumulatedBonus(int festivalId, int amount) {
		if (festivalId >= 0 && festivalId < FESTIVAL_COUNT && amount > 0) {
			accumulatedBonuses[festivalId] += amount;
			persistStatus();
		}
	}

	public synchronized void rewardHighestRanked() {
		for (int i = 0; i < FESTIVAL_COUNT; i++) {
			int duskScore = getHighestScore(CABAL_DUSK, i);
			int dawnScore = getHighestScore(CABAL_DAWN, i);
			int winningCabal = CABAL_NULL;
			if (dawnScore > duskScore) winningCabal = CABAL_DAWN;
			else if (duskScore > dawnScore) winningCabal = CABAL_DUSK;

			if (winningCabal != CABAL_NULL && accumulatedBonuses[i] > 0) {
				var members = getHighestScoreMembers(winningCabal, i);
				if (!members.isEmpty()) {
					int share = accumulatedBonuses[i] / members.size();
					log.info("SevenSignsManager: Distribuindo premio do Festival tier {}: {} AA para {} membros",
							i, share, members.size());
					accumulatedBonuses[i] = 0;
				}
			}
		}
	}

	private void broadcastSystemMessage(int sysMsgId) {
		if (world != null) {
			for (var p : world.allPlayers()) {
				p.send(new GameServerPacket.SystemMessage(sysMsgId, List.of()));
			}
		}
	}

	private void broadcastAnnouncement(String text) {
		if (world != null && text != null) {
			for (var p : world.allPlayers()) {
				p.send(new GameServerPacket.CreatureSay(0, GameServerPacket.CreatureSay.ALL, "SevenSigns", text));
			}
		}
	}

	public void broadcastSky() {
		if (world != null) {
			var ssq = new GameServerPacket.SsqInfo(getSkyState());
			for (var p : world.allPlayers()) {
				p.send(ssq);
			}
		}
	}

	public void sendCurrentPeriodMsg(GameWorld.OnlinePlayer player) {
		if (player == null) return;
		int msgId = switch (activePeriod) {
			case PERIOD_COMP_RECRUITING -> 1183; // PREPARATIONS_PERIOD_BEGUN
			case PERIOD_COMPETITION -> 1176;     // COMPETITION_PERIOD_BEGUN
			case PERIOD_COMP_RESULTS -> 1184;    // RESULTS_PERIOD_BEGUN
			case PERIOD_SEAL_VALIDATION -> 1185; // VALIDATION_PERIOD_BEGUN
			default -> 1176;
		};
		player.send(new GameServerPacket.SystemMessage(msgId, List.of()));
	}

	public double getStrifeCpMultiplier(int charId) {
		if (activePeriod != PERIOD_SEAL_VALIDATION || strifeOwner == CABAL_NULL) {
			return 1.0;
		}
		int cabal = getPlayerCabal(charId);
		if (cabal == strifeOwner) {
			return 1.10;
		} else if (cabal != CABAL_NULL) {
			return 0.90;
		}
		return 1.0;
	}

	private void applyStrifeCpModifiers() {
		log.info("SevenSignsManager: Aplicando modificadores de CP para Strife (Dono: {})", strifeOwner);
		if (world != null && strifeOwner != CABAL_NULL) {
			for (var p : world.allPlayers()) {
				var ch = p.character();
				if (ch != null) {
					int cabal = getPlayerCabal(p.objectId());
					if (cabal == strifeOwner) {
						int newCp = (int) Math.round(ch.maxCp() * 1.10);
						ch.maxCp(newCp);
						ch.currentCp(Math.min(ch.currentCp(), newCp));
					} else if (cabal != CABAL_NULL) {
						int newCp = (int) Math.round(ch.maxCp() * 0.90);
						ch.maxCp(newCp);
						ch.currentCp(Math.min(ch.currentCp(), newCp));
					}
				}
			}
		}
	}

	private void removeStrifeCpModifiers() {
		log.info("SevenSignsManager: Removendo modificadores de CP de Strife");
		if (world != null && strifeOwner != CABAL_NULL) {
			for (var p : world.allPlayers()) {
				var ch = p.character();
				if (ch != null) {
					int cabal = getPlayerCabal(p.objectId());
					if (cabal == strifeOwner) {
						int restored = (int) Math.round(ch.maxCp() / 1.10);
						ch.maxCp(restored);
						ch.currentCp(Math.min(ch.currentCp(), restored));
					} else if (cabal != CABAL_NULL) {
						int restored = (int) Math.round(ch.maxCp() / 0.90);
						ch.maxCp(restored);
						ch.currentCp(Math.min(ch.currentCp(), restored));
					}
				}
			}
		}
	}

	public void handleBossSpawnsOnValidation() {
		if (world == null) {
			return;
		}
		despawnSanctumBosses();
		if (shouldSpawnLilith()) {
			spawnSanctumBoss(LILITH_NPC_ID, "Lilith");
		} else if (shouldSpawnAnakim()) {
			spawnSanctumBoss(ANAKIM_NPC_ID, "Anakim");
		}
	}

	public void despawnSanctumBosses() {
		if (world == null) {
			return;
		}
		for (var npc : world.npcs()) {
			if (npc.npcId() == LILITH_NPC_ID || npc.npcId() == ANAKIM_NPC_ID) {
				world.removeNpc(npc);
			}
		}
	}

	private void spawnSanctumBoss(int npcId, String bossName) {
		if (world == null) {
			return;
		}
		for (var npc : world.npcs()) {
			if (npc.npcId() == npcId) {
				return;
			}
		}
		var template = (npcTemplates != null ? npcTemplates.get(npcId) : Optional.<com.lopez.l2j.game.npc.NpcTemplate>empty())
				.orElseGet(() -> new com.lopez.l2j.game.npc.NpcTemplate(
						npcId, npcId, bossName, false, "Seven Signs Raid Boss", false,
						30.0, 60.0, 80, "female", "L2RaidBoss",
						40, 5000000, 200000, 3000, 2500, 2800, 2400,
						300, 333, 0, 0, 0, 80, 150, 0, false, 1000000L, 50000, "RaidBoss", 0
				));
		var boss = new com.lopez.l2j.game.npc.NpcInstance(npcId, template, SANCTUM_BOSS_X, SANCTUM_BOSS_Y, SANCTUM_BOSS_Z, SANCTUM_BOSS_HEADING);
		world.addNpc(boss);
		log.info("SevenSignsManager: {} spawnada no Disciple's Necropolis Inner Sanctum!", bossName);
	}

	public void teleLosingCabalFromDungeons() {
		if (world == null || activePeriod != PERIOD_SEAL_VALIDATION) {
			return;
		}
		int winner = previousWinner;
		for (var p : world.allPlayers()) {
			int pCabal = getPlayerCabal(p.objectId());
			if (pCabal != winner && isInside7sDungeon(p.x(), p.y(), p.z())) {
				log.info("SevenSignsManager: Ejetando jogador {} de dungeon 7s para cidade", p.name());
				p.send(new GameServerPacket.CreatureSay(0, GameServerPacket.CreatureSay.ALL, "SYS",
						"Voce foi movido para a cidade pois nao pertence a cabala vitoriosa."));
				// Teleporta para a cidade mais proxima (ex: Giran 83400, 147943, -3404)
				p.send(new GameServerPacket.TeleportToLocation(p.objectId(), 83400, 147943, -3404));
			}
		}
	}

	public boolean isInside7sDungeon(int x, int y, int z) {
		if (z > -4000) {
			return false;
		}
		for (var loc : MAMMON_BLACKSMITH_LOCS) {
			double distSq = Math.pow(x - loc.x(), 2) + Math.pow(y - loc.y(), 2);
			if (distSq <= 16_000_000) { // raio ~4000
				return true;
			}
		}
		for (var loc : MAMMON_MERCHANT_LOCS) {
			double distSq = Math.pow(x - loc.x(), 2) + Math.pow(y - loc.y(), 2);
			if (distSq <= 16_000_000) { // raio ~4000
				return true;
			}
		}
		double distSanctumSq = Math.pow(x - 184464, 2) + Math.pow(y - (-13104), 2);
		if (distSanctumSq <= 16_000_000) {
			return true;
		}
		return false;
	}

	private synchronized void resetForNewCycle() {
		for (var entry : players.entrySet()) {
			var p = entry.getValue();
			var updated = new PlayerData(p.charId(), CABAL_NULL, getCabalShortName(p.cabal()),
					SEAL_NULL, 0, 0, 0, p.ancientAdena(), 0);
			entry.setValue(updated);
			persistPlayerData(updated);
		}
		dawnStoneScore = 0;
		duskStoneScore = 0;
		dawnFestivalScore = 0;
		duskFestivalScore = 0;
		for (int i = 0; i < FESTIVAL_COUNT; i++) {
			dawnFestivalIdScore[i] = 0;
			duskFestivalIdScore[i] = 0;
		}
		avariceDawnScore = 0;
		gnosisDawnScore = 0;
		strifeDawnScore = 0;
		avariceDuskScore = 0;
		duskGnosisScore = 0;
		duskStrifeScore = 0;
	}

	private void recalculateSealTotals() {
		avariceDawnScore = 0;
		gnosisDawnScore = 0;
		strifeDawnScore = 0;
		avariceDuskScore = 0;
		duskGnosisScore = 0;
		duskStrifeScore = 0;

		for (PlayerData p : players.values()) {
			if (p.cabal() == CABAL_DAWN) {
				switch (p.seal()) {
					case SEAL_AVARICE -> avariceDawnScore++;
					case SEAL_GNOSIS -> gnosisDawnScore++;
					case SEAL_STRIFE -> strifeDawnScore++;
				}
			} else if (p.cabal() == CABAL_DUSK) {
				switch (p.seal()) {
					case SEAL_AVARICE -> avariceDuskScore++;
					case SEAL_GNOSIS -> duskGnosisScore++;
					case SEAL_STRIFE -> duskStrifeScore++;
				}
			}
		}
	}

	private void loadFromDb() {
		if (jdbc == null) {
			return;
		}
		try {
			var statusRows = jdbc.sql("""
					SELECT current_cycle, festival_cycle, active_period, date, previous_winner,
					       dawn_stone_score, dusk_stone_score,
					       dawn_festival_score, dusk_festival_score,
					       dawn_festival_score1, dawn_festival_score2, dawn_festival_score3, dawn_festival_score4, dawn_festival_score5,
					       dusk_festival_score1, dusk_festival_score2, dusk_festival_score3, dusk_festival_score4, dusk_festival_score5,
					       avarice_owner, gnosis_owner, strife_owner,
					       avarice_dawn_score, gnosis_dawn_score, strife_dawn_score,
					       avarice_dusk_score, gnosis_dusk_score, strife_dusk_score,
					       accumulated_bonus0, accumulated_bonus1, accumulated_bonus2, accumulated_bonus3, accumulated_bonus4
					FROM seven_signs_status WHERE id = 0
					""").query().listOfRows();

			if (!statusRows.isEmpty()) {
				var row = statusRows.getFirst();
				currentCycle = ((Number) row.get("current_cycle")).intValue();
				festivalCycle = row.get("festival_cycle") != null ? ((Number) row.get("festival_cycle")).intValue() : 1;
				activePeriod = ((Number) row.get("active_period")).intValue();
				date = row.get("date") != null ? ((Number) row.get("date")).intValue() : 1;
				previousWinner = ((Number) row.get("previous_winner")).intValue();
				dawnStoneScore = ((Number) row.get("dawn_stone_score")).longValue();
				duskStoneScore = ((Number) row.get("dusk_stone_score")).longValue();
				dawnFestivalScore = ((Number) row.get("dawn_festival_score")).intValue();
				duskFestivalScore = ((Number) row.get("dusk_festival_score")).intValue();

				for (int i = 0; i < FESTIVAL_COUNT; i++) {
					var dawnF = row.get("dawn_festival_score" + (i + 1));
					if (dawnF != null) dawnFestivalIdScore[i] = ((Number) dawnF).intValue();
					var duskF = row.get("dusk_festival_score" + (i + 1));
					if (duskF != null) duskFestivalIdScore[i] = ((Number) duskF).intValue();
					var b = row.get("accumulated_bonus" + i);
					if (b != null) accumulatedBonuses[i] = ((Number) b).intValue();
				}

				avariceOwner = ((Number) row.get("avarice_owner")).intValue();
				gnosisOwner = ((Number) row.get("gnosis_owner")).intValue();
				strifeOwner = ((Number) row.get("strife_owner")).intValue();

				if (row.get("avarice_dawn_score") != null) avariceDawnScore = ((Number) row.get("avarice_dawn_score")).intValue();
				if (row.get("gnosis_dawn_score") != null) gnosisDawnScore = ((Number) row.get("gnosis_dawn_score")).intValue();
				if (row.get("strife_dawn_score") != null) strifeDawnScore = ((Number) row.get("strife_dawn_score")).intValue();
				if (row.get("avarice_dusk_score") != null) avariceDuskScore = ((Number) row.get("avarice_dusk_score")).intValue();
				if (row.get("gnosis_dusk_score") != null) duskGnosisScore = ((Number) row.get("gnosis_dusk_score")).intValue();
				if (row.get("strife_dusk_score") != null) duskStrifeScore = ((Number) row.get("strife_dusk_score")).intValue();

				log.info("SevenSignsManager: Status carregado do banco. Ciclo={}, Periodo={}, Vencedor={}",
						currentCycle, activePeriod, previousWinner);
			}

			var playerRows = jdbc.sql("""
					SELECT charId, cabal, old_cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score
					FROM seven_signs
					""").query().listOfRows();

			for (var r : playerRows) {
				int cid = ((Number) r.get("charId")).intValue();
				Object cabalObj = r.get("cabal");
				int cabal = cabalObj instanceof Number n ? n.intValue() : getCabalNumber(String.valueOf(cabalObj));
				String oldCabal = r.get("old_cabal") != null ? String.valueOf(r.get("old_cabal")) : "";
				int seal = ((Number) r.get("seal")).intValue();
				int red = ((Number) r.get("red_stones")).intValue();
				int green = ((Number) r.get("green_stones")).intValue();
				int blue = ((Number) r.get("blue_stones")).intValue();
				int aa = ((Number) r.get("ancient_adena_amount")).intValue();
				int contrib = r.get("contribution_score") != null ? ((Number) r.get("contribution_score")).intValue() : 0;
				players.put(cid, new PlayerData(cid, cabal, oldCabal, seal, red, green, blue, aa, contrib));
			}
			log.info("SevenSignsManager: {} registros de jogadores carregados", playerRows.size());

			var festRows = jdbc.sql("""
					SELECT festivalId, cabal, cycle, date, score, members
					FROM seven_signs_festival
					""").query().listOfRows();

			for (var fr : festRows) {
				int fid = ((Number) fr.get("festivalId")).intValue();
				String cabalStr = String.valueOf(fr.get("cabal"));
				int cabal = getCabalNumber(cabalStr);
				int cyc = ((Number) fr.get("cycle")).intValue();
				long d = ((Number) fr.get("date")).longValue();
				int sc = ((Number) fr.get("score")).intValue();
				String mems = fr.get("members") != null ? String.valueOf(fr.get("members")) : "";
				String key = cyc + "_" + fid + "_" + cabal;
				festivalRecords.put(key, new FestivalRecord(fid, cabal, cyc, d, sc, mems, mems));
			}
			log.info("SevenSignsManager: {} registros de festival carregados", festRows.size());
		} catch (Exception ex) {
			log.warn("Nao foi possivel carregar dados de seven_signs do banco: {}", ex.getMessage());
		}
	}

	public void persistStatus() {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO seven_signs_status (id, current_cycle, festival_cycle, active_period, date, previous_winner,
					                                dawn_stone_score, dusk_stone_score,
					                                dawn_festival_score, dusk_festival_score,
					                                dawn_festival_score1, dawn_festival_score2, dawn_festival_score3, dawn_festival_score4, dawn_festival_score5,
					                                dusk_festival_score1, dusk_festival_score2, dusk_festival_score3, dusk_festival_score4, dusk_festival_score5,
					                                avarice_owner, gnosis_owner, strife_owner,
					                                avarice_dawn_score, gnosis_dawn_score, strife_dawn_score,
					                                avarice_dusk_score, gnosis_dusk_score, strife_dusk_score,
					                                accumulated_bonus0, accumulated_bonus1, accumulated_bonus2, accumulated_bonus3, accumulated_bonus4)
					VALUES (0, :current_cycle, :festival_cycle, :active_period, :date, :previous_winner,
					        :dawn_stone_score, :dusk_stone_score,
					        :dawn_festival_score, :dusk_festival_score,
					        :dawn_festival_score1, :dawn_festival_score2, :dawn_festival_score3, :dawn_festival_score4, :dawn_festival_score5,
					        :dusk_festival_score1, :dusk_festival_score2, :dusk_festival_score3, :dusk_festival_score4, :dusk_festival_score5,
					        :avarice_owner, :gnosis_owner, :strife_owner,
					        :avarice_dawn_score, :gnosis_dawn_score, :strife_dawn_score,
					        :avarice_dusk_score, :gnosis_dusk_score, :strife_dusk_score,
					        :accumulated_bonus0, :accumulated_bonus1, :accumulated_bonus2, :accumulated_bonus3, :accumulated_bonus4)
					ON DUPLICATE KEY UPDATE
						current_cycle = :current_cycle,
						festival_cycle = :festival_cycle,
						active_period = :active_period,
						date = :date,
						previous_winner = :previous_winner,
						dawn_stone_score = :dawn_stone_score,
						dusk_stone_score = :dusk_stone_score,
						dawn_festival_score = :dawn_festival_score,
						dusk_festival_score = :dusk_festival_score,
						dawn_festival_score1 = :dawn_festival_score1,
						dawn_festival_score2 = :dawn_festival_score2,
						dawn_festival_score3 = :dawn_festival_score3,
						dawn_festival_score4 = :dawn_festival_score4,
						dawn_festival_score5 = :dawn_festival_score5,
						dusk_festival_score1 = :dusk_festival_score1,
						dusk_festival_score2 = :dusk_festival_score2,
						dusk_festival_score3 = :dusk_festival_score3,
						dusk_festival_score4 = :dusk_festival_score4,
						dusk_festival_score5 = :dusk_festival_score5,
						avarice_owner = :avarice_owner,
						gnosis_owner = :gnosis_owner,
						strife_owner = :strife_owner,
						avarice_dawn_score = :avarice_dawn_score,
						gnosis_dawn_score = :gnosis_dawn_score,
						strife_dawn_score = :strife_dawn_score,
						avarice_dusk_score = :avarice_dusk_score,
						gnosis_dusk_score = :gnosis_dusk_score,
						strife_dusk_score = :strife_dusk_score,
						accumulated_bonus0 = :accumulated_bonus0,
						accumulated_bonus1 = :accumulated_bonus1,
						accumulated_bonus2 = :accumulated_bonus2,
						accumulated_bonus3 = :accumulated_bonus3,
						accumulated_bonus4 = :accumulated_bonus4
					""")
					.param("current_cycle", currentCycle)
					.param("festival_cycle", festivalCycle)
					.param("active_period", activePeriod)
					.param("date", Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
					.param("previous_winner", previousWinner)
					.param("dawn_stone_score", dawnStoneScore)
					.param("dusk_stone_score", duskStoneScore)
					.param("dawn_festival_score", dawnFestivalScore)
					.param("dusk_festival_score", duskFestivalScore)
					.param("dawn_festival_score1", dawnFestivalIdScore[0])
					.param("dawn_festival_score2", dawnFestivalIdScore[1])
					.param("dawn_festival_score3", dawnFestivalIdScore[2])
					.param("dawn_festival_score4", dawnFestivalIdScore[3])
					.param("dawn_festival_score5", dawnFestivalIdScore[4])
					.param("dusk_festival_score1", duskFestivalIdScore[0])
					.param("dusk_festival_score2", duskFestivalIdScore[1])
					.param("dusk_festival_score3", duskFestivalIdScore[2])
					.param("dusk_festival_score4", duskFestivalIdScore[3])
					.param("dusk_festival_score5", duskFestivalIdScore[4])
					.param("avarice_owner", avariceOwner)
					.param("gnosis_owner", gnosisOwner)
					.param("strife_owner", strifeOwner)
					.param("avarice_dawn_score", avariceDawnScore)
					.param("gnosis_dawn_score", gnosisDawnScore)
					.param("strife_dawn_score", strifeDawnScore)
					.param("avarice_dusk_score", avariceDuskScore)
					.param("gnosis_dusk_score", duskGnosisScore)
					.param("strife_dusk_score", duskStrifeScore)
					.param("accumulated_bonus0", accumulatedBonuses[0])
					.param("accumulated_bonus1", accumulatedBonuses[1])
					.param("accumulated_bonus2", accumulatedBonuses[2])
					.param("accumulated_bonus3", accumulatedBonuses[3])
					.param("accumulated_bonus4", accumulatedBonuses[4])
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir seven_signs_status", ex);
		}
	}

	private void persistPlayerData(PlayerData p) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO seven_signs (charId, cabal, old_cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score)
					VALUES (:charId, :cabal, :old_cabal, :seal, :red_stones, :green_stones, :blue_stones, :ancient_adena_amount, :contribution_score)
					ON DUPLICATE KEY UPDATE
						cabal = :cabal,
						old_cabal = :old_cabal,
						seal = :seal,
						red_stones = :red_stones,
						green_stones = :green_stones,
						blue_stones = :blue_stones,
						ancient_adena_amount = :ancient_adena_amount,
						contribution_score = :contribution_score
					""")
					.param("charId", p.charId())
					.param("cabal", getCabalShortName(p.cabal()))
					.param("old_cabal", p.oldCabal())
					.param("seal", p.seal())
					.param("red_stones", p.redStones())
					.param("green_stones", p.greenStones())
					.param("blue_stones", p.blueStones())
					.param("ancient_adena_amount", p.ancientAdena())
					.param("contribution_score", p.contributionScore())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir seven_signs player {}", p.charId(), ex);
		}
	}

	private void persistFestivalRecord(FestivalRecord fr) {
		if (jdbc == null) {
			return;
		}
		try {
			jdbc.sql("""
					INSERT INTO seven_signs_festival (festivalId, cabal, cycle, date, score, members)
					VALUES (:festivalId, :cabal, :cycle, :date, :score, :members)
					ON DUPLICATE KEY UPDATE
						date = :date,
						score = :score,
						members = :members
					""")
					.param("festivalId", fr.festivalId())
					.param("cabal", getCabalShortName(fr.cabal()))
					.param("cycle", fr.cycle())
					.param("date", fr.date())
					.param("score", fr.score())
					.param("members", fr.members())
					.update();
		} catch (Exception ex) {
			log.error("Erro ao persistir seven_signs_festival record", ex);
		}
	}
}
