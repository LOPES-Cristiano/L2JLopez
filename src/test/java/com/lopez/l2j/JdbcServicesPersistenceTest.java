package com.lopez.l2j;

import com.lopez.l2j.game.announcements.Announcements;
import com.lopez.l2j.game.castle.CastleManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.clan.clanhall.ClanHallService;
import com.lopez.l2j.game.community.CommunityBoardService;
import com.lopez.l2j.game.fishing.FishingChampionshipService;
import com.lopez.l2j.game.item.GroundItemService;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.recipe.RecipeService;
import com.lopez.l2j.game.service.CharacterVariablesService;
import com.lopez.l2j.game.service.SchemeBufferService;
import com.lopez.l2j.game.service.VoteRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JdbcServicesPersistenceTest {

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private RecipeService recipeService;

	@Autowired
	private CommunityBoardService cbService;

	@Autowired
	private ClanTable clanTable;

	@Autowired
	private SchemeBufferService schemeBufferService;

	@Autowired
	private VoteRewardService voteRewardService;

	@Autowired
	private GroundItemService groundItemService;

	@Autowired
	private Announcements announcements;

	@Autowired
	private FishingChampionshipService fishingService;

	@Autowired
	private ClanHallService clanHallService;

	@Autowired
	private CastleManager castleManager;

	@Autowired
	private CharacterVariablesService charVarsService;

	private PlayerCharacter testPlayer;

	@BeforeEach
	void setUp() {
		// Garante criacao idempotente das tabelas testadas no H2 em memoria
		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS character_recipebook (
				  charId INT NOT NULL DEFAULT 0,
				  id DECIMAL(11,0) NOT NULL DEFAULT 0,
				  type INT NOT NULL DEFAULT 0,
				  PRIMARY KEY (id, charId)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS character_teleports (
				  charId INT NOT NULL,
				  locId TINYINT NOT NULL DEFAULT 1,
				  locName VARCHAR(50) NOT NULL,
				  acronym VARCHAR(4) NOT NULL,
				  iconId TINYINT NOT NULL,
				  xCoord MEDIUMINT NOT NULL,
				  yCoord MEDIUMINT NOT NULL,
				  zCoord MEDIUMINT NOT NULL,
				  PRIMARY KEY (charId, locId)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS clan_notices (
				  clanID INT NOT NULL DEFAULT 0,
				  notice TEXT NOT NULL,
				  enabled VARCHAR(5) NOT NULL DEFAULT '',
				  PRIMARY KEY (clanID)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS mods_buffer_schemes (
				  ownerId INT NOT NULL DEFAULT 0,
				  id INT NOT NULL DEFAULT 0,
				  level INT NOT NULL DEFAULT 0,
				  scheme VARCHAR(20) NOT NULL DEFAULT 'default'
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS mods_voting_reward (
				  id INT AUTO_INCREMENT PRIMARY KEY,
				  data VARCHAR(255) NOT NULL,
				  scope VARCHAR(255) NOT NULL,
				  time BIGINT NOT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS items_on_ground (
				  object_id INT NOT NULL DEFAULT 0,
				  item_id INT DEFAULT NULL,
				  count INT DEFAULT NULL,
				  enchant_level INT DEFAULT NULL,
				  x INT NOT NULL DEFAULT 0,
				  y INT NOT NULL DEFAULT 0,
				  z INT NOT NULL DEFAULT 0,
				  drop_time DECIMAL(20,0) NOT NULL DEFAULT 0,
				  equipable INT DEFAULT 0,
				  PRIMARY KEY (object_id)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS auto_announcements (
				  id INT NOT NULL PRIMARY KEY,
				  initial BIGINT NOT NULL,
				  delay BIGINT NOT NULL,
				  cycle INT NOT NULL,
				  memo TEXT DEFAULT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS fishing_championship (
				  PlayerName VARCHAR(35) NOT NULL,
				  fishLength FLOAT NOT NULL,
				  rewarded INT NOT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS fishing_championship_date (
				  finish_date DECIMAL(20,0) NOT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS auction_bid (
				  id INT NOT NULL DEFAULT 0,
				  auctionId INT NOT NULL DEFAULT 0,
				  bidderId INT NOT NULL DEFAULT 0,
				  bidderName VARCHAR(50) NOT NULL,
				  clan_name VARCHAR(50) NOT NULL,
				  maxBid INT NOT NULL DEFAULT 0,
				  time_bid DECIMAL(20,0) NOT NULL DEFAULT 0,
				  PRIMARY KEY (auctionId, bidderId)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS castle_functions (
				  castle_id INT NOT NULL DEFAULT 0,
				  type INT NOT NULL DEFAULT 0,
				  lvl INT NOT NULL DEFAULT 0,
				  lease INT NOT NULL DEFAULT 0,
				  rate DECIMAL(20,0) NOT NULL DEFAULT 0,
				  endTime DECIMAL(20,0) NOT NULL DEFAULT 0,
				  PRIMARY KEY (castle_id, type)
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS forums (
				  forum_id INT NOT NULL PRIMARY KEY,
				  forum_name VARCHAR(255) NOT NULL DEFAULT '',
				  forum_parent INT NOT NULL DEFAULT 0,
				  forum_post INT NOT NULL DEFAULT 0,
				  forum_type INT NOT NULL DEFAULT 0,
				  forum_perm INT NOT NULL DEFAULT 0,
				  forum_owner_id INT NOT NULL DEFAULT 0
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS topic (
				  topic_id INT NOT NULL PRIMARY KEY,
				  topic_forum_id INT NOT NULL DEFAULT 0,
				  topic_name VARCHAR(255) NOT NULL DEFAULT '',
				  topic_date DECIMAL(20,0) NOT NULL DEFAULT 0,
				  topic_ownername VARCHAR(255) NOT NULL DEFAULT '0',
				  topic_ownerid INT NOT NULL DEFAULT 0,
				  topic_type INT NOT NULL DEFAULT 0,
				  topic_reply INT NOT NULL DEFAULT 0
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS posts (
				  post_id INT NOT NULL PRIMARY KEY,
				  post_owner_name VARCHAR(255) NOT NULL DEFAULT '',
				  post_ownerid INT NOT NULL DEFAULT 0,
				  post_date DECIMAL(20,0) NOT NULL DEFAULT 0,
				  post_topic_id INT NOT NULL DEFAULT 0,
				  post_forum_id INT NOT NULL DEFAULT 0,
				  post_txt TEXT NOT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS server_data (
				  valueName VARCHAR(64) NOT NULL PRIMARY KEY,
				  valueData VARCHAR(200) NOT NULL
				);
				""").update();

		jdbc.sql("""
				CREATE TABLE IF NOT EXISTS quest_global_data (
				  quest_name VARCHAR(40) NOT NULL DEFAULT '',
				  var VARCHAR(20) NOT NULL DEFAULT '',
				  `value` VARCHAR(255) DEFAULT NULL,
				  PRIMARY KEY (quest_name, var)
				);
				""").update();

		testPlayer = new PlayerCharacter(98765, "TestPersistenceHero");
		testPlayer.inventory().addAdena(100_000_000);
	}

	@Test
	@DisplayName("1. RecipeService: persiste e carrega character_recipebook (V42)")
	void testRecipeBookPersistence() {
		int charId = testPlayer.objectId();
		recipeService.registerRecipe(charId, 1);
		recipeService.registerRecipe(charId, 2);

		List<Integer> book = recipeService.getRecipeBook(charId, true);
		assertThat(book).contains(1, 2);

		int count = jdbc.sql("SELECT count(*) FROM character_recipebook WHERE charId = :id")
				.param("id", charId)
				.query(Integer.class).single();
		assertThat(count).isEqualTo(2);

		recipeService.unregisterRecipe(charId, 1);
		count = jdbc.sql("SELECT count(*) FROM character_recipebook WHERE charId = :id")
				.param("id", charId)
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("2. CommunityBoardService: persiste e carrega character_teleports (V48)")
	void testCharacterTeleportsPersistence() {
		cbService.handleCommand(testPlayer, "_bbsteleport_save 1");
		String fav = cbService.getFavorite(testPlayer, 1);
		assertThat(fav).isNotNull();

		int count = jdbc.sql("SELECT count(*) FROM character_teleports WHERE charId = :id AND locId = 1")
				.param("id", testPlayer.objectId())
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);

		cbService.handleCommand(testPlayer, "_bbsteleport_del 1");
		count = jdbc.sql("SELECT count(*) FROM character_teleports WHERE charId = :id AND locId = 1")
				.param("id", testPlayer.objectId())
				.query(Integer.class).single();
		assertThat(count).isEqualTo(0);
	}

	@Test
	@DisplayName("3. ClanTable: salva e carrega clan_notices (V52)")
	void testClanNoticePersistence() {
		Clan clan = new Clan(9001, "PersistenceClan", testPlayer.objectId(), testPlayer.name(), 1);
		clanTable.registerClan(clan);

		clanTable.saveNotice(clan.clanId(), "Mensagem de aviso importante!", true);
		assertThat(clan.notice()).isEqualTo("Mensagem de aviso importante!");
		assertThat(clan.isNoticeEnabled()).isTrue();

		int count = jdbc.sql("SELECT count(*) FROM clan_notices WHERE clanID = 9001")
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("4. SchemeBufferService: persiste e carrega mods_buffer_schemes (V95)")
	void testBufferSchemesPersistence() {
		int charId = testPlayer.objectId();
		schemeBufferService.saveScheme(charId, 1, List.of(1068, 1086, 1204));

		List<Integer> scheme = schemeBufferService.getScheme(charId, 1);
		assertThat(scheme).contains(1068, 1086, 1204);

		int count = jdbc.sql("SELECT count(*) FROM mods_buffer_schemes WHERE ownerId = :id")
				.param("id", charId)
				.query(Integer.class).single();
		assertThat(count).isEqualTo(3);
	}

	@Test
	@DisplayName("5. VoteRewardService: persiste mods_voting_reward (V97)")
	void testVoteRewardPersistence() {
		boolean rewarded = voteRewardService.claimVoteReward(testPlayer, "HopZone");
		assertThat(rewarded).isTrue();

		int count = jdbc.sql("SELECT count(*) FROM mods_voting_reward WHERE scope = 'HopZone'")
				.query(Integer.class).single();
		assertThat(count).isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("6. GroundItemService: salva e carrega items_on_ground (V91)")
	void testItemsOnGroundPersistence() {
		var itemTpl = ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 1, List.of(), true, true, false, true);
		var itemInst = new ItemInstance(888123, itemTpl, 0, 5000);
		groundItemService.dropItem(0, itemInst, 83400, 147900, -3400);

		groundItemService.saveToDb();

		int count = jdbc.sql("SELECT count(*) FROM items_on_ground WHERE object_id = 888123")
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("7. Announcements: carrega auto_announcements (V9)")
	void testAutoAnnouncementsPersistence() {
		jdbc.sql("INSERT INTO auto_announcements (id, initial, delay, cycle, memo) VALUES (999, 10, 60, -1, 'Evento Automático!')")
				.update();

		announcements.loadAutoAnnouncements();
		assertThat(announcements.listAutoAnnouncements()).contains("Evento Automático!");
	}

	@Test
	@DisplayName("8. FishingChampionshipService: salva e carrega fishing_championship (V73, V74)")
	void testFishingChampionshipPersistence() {
		fishingService.submitFish(testPlayer.objectId(), testPlayer.name(), 85.50);
		fishingService.saveToDb();

		int count = jdbc.sql("SELECT count(*) FROM fishing_championship WHERE PlayerName = :n")
				.param("n", testPlayer.name())
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);

		int dateCount = jdbc.sql("SELECT count(*) FROM fishing_championship_date")
				.query(Integer.class).single();
		assertThat(dateCount).isEqualTo(1);
	}

	@Test
	@DisplayName("9. ClanHallService: persiste auction_bid (V7)")
	void testAuctionBidPersistence() {
		Clan clan = new Clan(9002, "BidderClan", testPlayer.objectId(), testPlayer.name(), 2);
		clanTable.registerClan(clan);
		testPlayer.clanId(clan.clanId());

		var res = clanHallService.bidOnAuction(testPlayer, 22, 3500000);
		assertThat(res).isEqualTo(ClanHallService.BidResult.SUCCESS);

		var bids = clanHallService.getBidsForAuction(22);
		assertThat(bids).isNotEmpty();
	}

	@Test
	@DisplayName("10. CastleManager: persiste castle_functions (V21)")
	void testCastleFunctionsPersistence() {
		var rec = castleManager.setCastleFunction(1, CastleManager.FUNC_TELEPORT, 1);
		assertThat(rec).isNotNull();

		int count = jdbc.sql("SELECT count(*) FROM castle_functions WHERE castle_id = 1 AND type = 1")
				.query(Integer.class).single();
		assertThat(count).isEqualTo(1);

		var loaded = castleManager.getCastleFunction(1, CastleManager.FUNC_TELEPORT);
		assertThat(loaded).isPresent();
		assertThat(loaded.get().level()).isEqualTo(1);
	}

	@Test
	@DisplayName("11. CommunityBoardService: persiste forums, topic e posts (V81, V120, V106)")
	void testForumPersistence() {
		cbService.createForum(101, "Geral", 0, 1, 1, 0);
		var forums = cbService.getForums();
		assertThat(forums.stream().anyMatch(f -> f.forumId() == 101)).isTrue();

		cbService.createTopic(201, 101, "Boas Vindas", testPlayer);
		var topics = cbService.getTopics(101);
		assertThat(topics.stream().anyMatch(t -> t.topicId() == 201)).isTrue();

		cbService.addPost(301, 201, 101, "Olá a todos os aventureiros!", testPlayer);
		var posts = cbService.getPosts(201);
		assertThat(posts.stream().anyMatch(p -> p.postId() == 301)).isTrue();
	}

	@Test
	@DisplayName("12. CharacterVariablesService: persiste server_data (V113) e quest_global_data (V107)")
	void testServerAndQuestGlobalDataPersistence() {
		charVarsService.setServerVariable("SERVER_STAGE", "INTERLUDE_EPILOGUE");
		String stage = charVarsService.getServerVariable("SERVER_STAGE", "DEFAULT");
		assertThat(stage).isEqualTo("INTERLUDE_EPILOGUE");

		charVarsService.setQuestGlobalVariable("QuestMeetBaium", "BaiumAwakened", "1");
		String baiumState = charVarsService.getQuestGlobalVariable("QuestMeetBaium", "BaiumAwakened", "0");
		assertThat(baiumState).isEqualTo("1");
	}
}
