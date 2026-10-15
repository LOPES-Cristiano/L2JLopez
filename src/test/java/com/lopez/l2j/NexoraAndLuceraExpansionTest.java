package com.lopez.l2j;

import com.lopez.l2j.game.community.CommunityAuctionService;
import com.lopez.l2j.game.daily.DailyRewardService;
import com.lopez.l2j.game.diary.DropsDiaryService;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.pet.PetEvolveService;
import com.lopez.l2j.game.phantom.PhantomProfile;
import com.lopez.l2j.game.phantom.PhantomService;
import com.lopez.l2j.game.upgrade.UpgradeItemService;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.TimeFarmZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NexoraAndLuceraExpansionTest {

	private UpgradeItemService upgradeItemService;
	private DropsDiaryService dropsDiaryService;
	private DailyRewardService dailyRewardService;
	private TimeFarmZoneService timeFarmZoneService;
	private PetEvolveService petEvolveService;
	private CommunityAuctionService auctionService;
	private PhantomService phantomService;

	private PlayerCharacter createPlayer(int objectId, String name) {
		PlayerCharacter player = new PlayerCharacter(
				objectId,
				"acc_" + objectId,
				name,
				80,
				100000000L,
				1000000,
				0,
				88,
				88,
				false,
				0,
				0,
				0,
				5000,
				3000,
				2000,
				0,
				0,
				0,
				0,
				"Tester",
				0,
				System.currentTimeMillis(),
				0,
				83400,
				147900,
				-3400,
				0,
				5000.0,
				3000.0,
				2000.0
		);
		player.setInventory(new Inventory(objectId));
		return player;
	}

	@BeforeEach
	void setUp() {
		if (GameWorld.getInstance() == null) {
			new GameWorld();
		}
		upgradeItemService = new UpgradeItemService();
		dropsDiaryService = new DropsDiaryService();
		dailyRewardService = new DailyRewardService();
		timeFarmZoneService = new TimeFarmZoneService();
		petEvolveService = new PetEvolveService();
		auctionService = new CommunityAuctionService();
		phantomService = new PhantomService(false, null);
	}

	@Test
	@DisplayName("UpgradeItemService deve evoluir item preservando o nivel de encantamento e consumindo custo")
	void testUpgradeItemPreservingEnchant() {
		PlayerCharacter player = createPlayer(5001, "UpgradeTester");
		// Dá item base 6658 +7
		ItemInstance weapon = new ItemInstance(1001, ItemTemplate.etc(6658, 6658, "Sword of Ipos", "sword", "normal", 100, "none", 0, true, true, true, true), player.objectId(), 1);
		weapon.enchant(7);
		player.inventory().addItem(weapon);

		// Dá 10kk Adena para a taxa de 5kk
		ItemInstance adena = new ItemInstance(1002, ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true), player.objectId(), 10_000_000);
		player.inventory().addItem(adena);

		var result = upgradeItemService.upgradeItem(player, 6658, 25325);
		assertThat(result.success()).isTrue();
		assertThat(result.resultItemId()).isEqualTo(7577);
		assertThat(result.resultEnchant()).isEqualTo(7);

		// Item antigo consumido e novo item presente com enchant +7
		assertThat(player.inventory().byItemId(6658)).isEmpty();
		assertThat(player.inventory().byItemId(7577)).isPresent();
		assertThat(player.inventory().byItemId(7577).get().enchant()).isEqualTo(7);
		assertThat(player.inventory().count(57)).isEqualTo(5_000_000);
	}

	@Test
	@DisplayName("DropsDiaryService deve permitir drops ate o limite diario e bloquear drops excedentes")
	void testDropsDiaryDailyLimitEnforcement() {
		int playerId = 6001;
		int eventMedal = 6392; // Limite diario: 5

		assertThat(dropsDiaryService.canDropItem(playerId, eventMedal)).isTrue();

		// Registra 4 drops
		dropsDiaryService.recordDrop(playerId, eventMedal, 4);
		assertThat(dropsDiaryService.canDropItem(playerId, eventMedal)).isTrue();

		// Registra o 5º drop (atinge o limite)
		dropsDiaryService.recordDrop(playerId, eventMedal, 1);
		assertThat(dropsDiaryService.canDropItem(playerId, eventMedal)).isFalse();
	}

	@Test
	@DisplayName("DailyRewardService deve entregar recompensas de presenca consecutivas e bloquear resgate duplicado")
	void testDailyRewardClaimSequence() {
		PlayerCharacter player = createPlayer(7001, "DailyTester");

		assertThat(dailyRewardService.canClaimToday(player)).isTrue();
		var claim1 = dailyRewardService.claimReward(player);
		assertThat(claim1.success()).isTrue();
		assertThat(claim1.day()).isEqualTo(1);
		assertThat(player.inventory().count(57)).isEqualTo(1_000_000);

		// Tentativa no mesmo dia deve falhar
		assertThat(dailyRewardService.canClaimToday(player)).isFalse();
		var claimDuplicate = dailyRewardService.claimReward(player);
		assertThat(claimDuplicate.success()).isFalse();
	}

	@Test
	@DisplayName("TimeFarmZoneService deve permitir entrada, cobrar Adena e controlar tempo de permanencia")
	void testTimeFarmZoneCycle() {
		PlayerCharacter player = createPlayer(8001, "TimeFarmer");
		ItemInstance adena = new ItemInstance(2001, ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true), player.objectId(), 1_000_000);
		player.inventory().addItem(adena);

		boolean entered = timeFarmZoneService.enterZone(player, 1); // Forbidden Cemetery (500k Adena)
		assertThat(entered).isTrue();
		assertThat(player.inventory().count(57)).isEqualTo(500_000);
		assertThat(timeFarmZoneService.isInsideTimeZone(player.objectId())).isTrue();
		assertThat(timeFarmZoneService.getRemainingTimeSeconds(player.objectId())).isGreaterThan(3500);

		timeFarmZoneService.exitZone(player);
		assertThat(timeFarmZoneService.isInsideTimeZone(player.objectId())).isFalse();
	}

	@Test
	@DisplayName("PetEvolveService deve validar requisitos de nivel e evoluir Wolf para Great Wolf")
	void testPetEvolveRequirementsAndEvolution() {
		PlayerCharacter player = createPlayer(9001, "PetTrainer");
		ItemInstance collar = new ItemInstance(3001, ItemTemplate.etc(2375, 2375, "Wolf Collar", "pet", "normal", 100, "none", 0, true, true, true, true), player.objectId(), 1);
		player.inventory().addItem(collar);

		// Tentativa com pet abaixo do level 55 deve falhar
		var failResult = petEvolveService.evolvePet(player, 2375, 54);
		assertThat(failResult.success()).isFalse();

		// Evolução com level 55
		var successResult = petEvolveService.evolvePet(player, 2375, 55);
		assertThat(successResult.success()).isTrue();
		assertThat(successResult.evolvedItemId()).isEqualTo(9882); // Great Wolf Necklace
		assertThat(player.inventory().byItemId(2375)).isEmpty();
		assertThat(player.inventory().byItemId(9882)).isPresent();
	}

	@Test
	@DisplayName("CommunityAuctionService deve listar e vender item entre jogadores via Adena (ZERO DONATE)")
	void testCommunityAuctionListingAndPurchase() {
		PlayerCharacter seller = createPlayer(10001, "SellerChar");
		PlayerCharacter buyer = createPlayer(10002, "BuyerChar");

		ItemInstance sword = new ItemInstance(4001, ItemTemplate.etc(135, 135, "Samurai Longsword", "sword", "normal", 100, "none", 0, true, true, true, true), seller.objectId(), 1);
		sword.enchant(4);
		seller.inventory().addItem(sword);

		ItemInstance buyerAdena = new ItemInstance(4002, ItemTemplate.etc(57, 57, "Adena", "none", "asset", 0, "none", 0, true, true, true, true), buyer.objectId(), 5_000_000);
		buyer.inventory().addItem(buyerAdena);

		var listing = auctionService.listItem(seller, sword.objectId(), 1, 3_000_000L, 57);
		assertThat(listing).isNotNull();
		assertThat(seller.inventory().byItemId(135)).isEmpty();

		boolean bought = auctionService.buyItem(buyer, listing.auctionId());
		assertThat(bought).isTrue();
		assertThat(buyer.inventory().count(57)).isEqualTo(2_000_000);
		assertThat(buyer.inventory().byItemId(135)).isPresent();
		assertThat(buyer.inventory().byItemId(135).get().enchant()).isEqualTo(4);
	}

	@Test
	@DisplayName("PhantomService deve instanciar phantom no GameWorld e registrar no ecossistema")
	void testPhantomMaterializationInWorld() {
		PhantomProfile profile = new PhantomProfile("PhantomWarrior", 88, 80, null, 83400, 147900, -3400, null, null);
		PlayerCharacter spawned = phantomService.spawnPhantom(profile);

		assertThat(spawned).isNotNull();
		assertThat(spawned.isPhantom()).isTrue();
		assertThat(spawned.name()).isEqualTo("PhantomWarrior");
		assertThat(GameWorld.getInstance().findPlayer(spawned.objectId())).isNotNull();
	}
}
