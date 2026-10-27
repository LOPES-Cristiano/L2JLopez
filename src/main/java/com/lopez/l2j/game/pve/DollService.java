package com.lopez.l2j.game.pve;

import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.item.ItemTemplateTable;
import com.lopez.l2j.game.model.ObjectIdFactory;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.skill.StatFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico para gerenciamento das Dolls de Chefes (Baium, Antharas, Valakas, Zaken, Queen Ant, Orfen, Core, Frintezza) - Onda C9.
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>Dolls no inventario concedem passivas e atributos reais ao personagem (P.Atk, M.Atk, P.Def, M.Def, MaxHP, CritRate).</li>
 *   <li><b>Nao-cumulativas</b>: Dolls do mesmo chefe nao acumulam (aplica-se apenas a de maior nivel).</li>
 *   <li>Dolls de chefes distintos funcionam em conjunto simultaneamente.</li>
 * </ul>
 */
@Service
public class DollService {

	private static final Logger log = LoggerFactory.getLogger(DollService.class);

	private static volatile DollService instance;

	public record BossDoll(int itemId, String boss, int skillId, int level, double power) {}

	public record DollStatBonus(
			int pAtk,
			int mAtk,
			int pDef,
			int mDef,
			int maxHp,
			int critRate,
			int pAtkSpd,
			int mAtkSpd
	) {
		public static final DollStatBonus NONE = new DollStatBonus(0, 0, 0, 0, 0, 0, 0, 0);

		public boolean isEmpty() {
			return pAtk == 0 && mAtk == 0 && pDef == 0 && mDef == 0
					&& maxHp == 0 && critRate == 0 && pAtkSpd == 0 && mAtkSpd == 0;
		}
	}

	private final Map<Integer, BossDoll> dollsById = new ConcurrentHashMap<>();

	@Autowired(required = false)
	private InventoryService inventoryService;

	@Autowired(required = false)
	private ItemTemplateTable itemTemplateTable;

	@Autowired(required = false)
	private ObjectIdFactory objectIdFactory = ObjectIdFactory.sequential(9400000);

	public DollService() {
		loadDolls();
		instance = this;
	}

	@Autowired
	public DollService(
			@Autowired(required = false) InventoryService inventoryService,
			@Autowired(required = false) ItemTemplateTable itemTemplateTable,
			@Autowired(required = false) ObjectIdFactory objectIdFactory) {
		this.inventoryService = inventoryService;
		this.itemTemplateTable = itemTemplateTable;
		if (objectIdFactory != null) {
			this.objectIdFactory = objectIdFactory;
		}
		loadDolls();
		instance = this;
	}

	public static DollService getInstance() {
		DollService current = instance;
		if (current == null) {
			synchronized (DollService.class) {
				if (instance == null) {
					instance = new DollService();
				}
				current = instance;
			}
		}
		return current;
	}

	public void loadDolls() {
		dollsById.clear();
		try {
			File file = new File("data/xml/custom/Dolls.xml");
			InputStream is = file.exists() ? new java.io.FileInputStream(file) : getClass().getResourceAsStream("/data/xml/custom/Dolls.xml");
			if (is != null) {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
				NodeList dollNodes = doc.getElementsByTagName("Doll");

				for (int i = 0; i < dollNodes.getLength(); i++) {
					Element elem = (Element) dollNodes.item(i);
					int id = Integer.parseInt(elem.getAttribute("Id"));
					String boss = elem.hasAttribute("Boss") ? elem.getAttribute("Boss") : "Boss_" + id;
					int skillId = Integer.parseInt(elem.getAttribute("SkillId"));
					int skillLvl = Integer.parseInt(elem.getAttribute("SkillLvl"));
					double power = elem.hasAttribute("SkillPower") ? Double.parseDouble(elem.getAttribute("SkillPower")) : 2.0;

					dollsById.put(id, new BossDoll(id, boss, skillId, skillLvl, power));
				}
			} else {
				log.warn("Dolls.xml nao encontrado. Usando definicoes padrao.");
			}

			// Garante suporte a Valakas Dolls (20718..20720) caso nao constem no XML
			dollsById.putIfAbsent(20718, new BossDoll(20718, "Valakas", 27508, 1, 3.0));
			dollsById.putIfAbsent(20719, new BossDoll(20719, "Valakas", 27508, 2, 6.0));
			dollsById.putIfAbsent(20720, new BossDoll(20720, "Valakas", 27508, 3, 10.0));

			log.info("DollService: {} dolls de chefes carregadas com sucesso.", dollsById.size());
		} catch (Exception e) {
			log.error("Erro ao carregar Dolls.xml: {}", e.getMessage(), e);
		}
	}

	public boolean isDoll(int itemId) {
		return dollsById.containsKey(itemId);
	}

	public BossDoll getDoll(int itemId) {
		return dollsById.get(itemId);
	}

	/**
	 * Filtra as dolls ativas do jogador aplicando a regra de NAO-CUMULATIVIDADE:
	 * se possuir mais de uma doll do mesmo chefe (ex: Baium Lv 1 e Lv 3), apenas a de MAIOR nivel e considerada.
	 */
	public Map<String, BossDoll> getActiveDolls(PlayerCharacter player) {
		if (player == null || player.inventory() == null) {
			return Collections.emptyMap();
		}

		Map<String, BossDoll> activeBossDolls = new HashMap<>();

		for (ItemInstance item : player.inventory().items()) {
			if (item == null) continue;
			BossDoll doll = dollsById.get(item.itemId());
			if (doll != null) {
				// Verifica se ja temos uma doll desse boss
				BossDoll existing = activeBossDolls.get(doll.boss());
				if (existing == null || doll.level() > existing.level()) {
					activeBossDolls.put(doll.boss(), doll);
				}
			}
		}

		return Collections.unmodifiableMap(activeBossDolls);
	}

	/**
	 * Calcula o somatorio de poder conferido pelas dolls ativas.
	 */
	public double calculateTotalPower(PlayerCharacter player) {
		Map<String, BossDoll> active = getActiveDolls(player);
		double total = 0.0;
		for (BossDoll doll : active.values()) {
			total += doll.power();
		}
		return total;
	}

	/**
	 * Calcula os bonus consolidados de atributos (P.Atk, M.Atk, P.Def, M.Def, MaxHP, CritRate, AtkSpd, CastSpd)
	 * das Boss Dolls ativas na mochila do jogador (respeitando a nao-cumulatividade por chefe).
	 */
	public DollStatBonus calculateStatBonuses(PlayerCharacter player) {
		Map<String, BossDoll> active = getActiveDolls(player);
		if (active.isEmpty()) {
			return DollStatBonus.NONE;
		}

		int pAtk = 0;
		int mAtk = 0;
		int pDef = 0;
		int mDef = 0;
		int maxHp = 0;
		int critRate = 0;
		int pAtkSpd = 0;
		int mAtkSpd = 0;

		for (BossDoll doll : active.values()) {
			double p = doll.power();
			String bossKey = doll.boss() != null ? doll.boss().trim().toLowerCase(Locale.ROOT) : "";
			switch (bossKey) {
				case "baium" -> {
					pAtk += (int) Math.round(p * 10);
					pDef += (int) Math.round(p * 10);
					critRate += (int) Math.round(p * 5);
					pAtkSpd += (int) Math.round(p * 5);
					mAtkSpd += (int) Math.round(p * 5);
				}
				case "queenant", "queen ant" -> {
					pAtk += (int) Math.round(p * 15);
					critRate += (int) Math.round(p * 5);
				}
				case "orfen" -> {
					mAtk += (int) Math.round(p * 15);
					mDef += (int) Math.round(p * 12);
				}
				case "core" -> {
					pDef += (int) Math.round(p * 15);
					mDef += (int) Math.round(p * 10);
				}
				case "zaken" -> {
					pAtk += (int) Math.round(p * 10);
					critRate += (int) Math.round(p * 4);
					maxHp += (int) Math.round(p * 50);
				}
				case "antharas" -> {
					pDef += (int) Math.round(p * 15);
					mDef += (int) Math.round(p * 15);
					maxHp += (int) Math.round(p * 60);
				}
				case "valakas" -> {
					pAtk += (int) Math.round(p * 15);
					mAtk += (int) Math.round(p * 15);
					maxHp += (int) Math.round(p * 50);
					critRate += (int) Math.round(p * 4);
				}
				case "frintezza" -> {
					pAtk += (int) Math.round(p * 12);
					mAtk += (int) Math.round(p * 12);
					pAtkSpd += (int) Math.round(p * 4);
					mAtkSpd += (int) Math.round(p * 4);
					critRate += (int) Math.round(p * 3);
				}
				default -> {
					pAtk += (int) Math.round(p * 10);
					pDef += (int) Math.round(p * 10);
				}
			}
		}

		return new DollStatBonus(pAtk, mAtk, pDef, mDef, maxHp, critRate, pAtkSpd, mAtkSpd);
	}

	public int calculatePAtkBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).pAtk();
	}

	public int calculateMAtkBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).mAtk();
	}

	public int calculatePDefBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).pDef();
	}

	public int calculateMDefBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).mDef();
	}

	public int calculateMaxHpBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).maxHp();
	}

	public int calculateCritRateBonus(PlayerCharacter player) {
		return calculateStatBonuses(player).critRate();
	}

	/**
	 * Converte os bonus das Boss Dolls ativas na mochila em funcoes de atributo (StatFunc)
	 * consumidas diretamente por {@link com.lopez.l2j.game.model.PlayerStats}.
	 */
	public List<StatFunc> getActiveStatFuncs(PlayerCharacter player) {
		DollStatBonus bonus = calculateStatBonuses(player);
		if (bonus.isEmpty()) {
			return List.of();
		}

		List<StatFunc> funcs = new ArrayList<>(8);
		if (bonus.pAtk() > 0) {
			funcs.add(new StatFunc("pAtk", StatFunc.Op.ADD, 0x40, bonus.pAtk()));
		}
		if (bonus.mAtk() > 0) {
			funcs.add(new StatFunc("mAtk", StatFunc.Op.ADD, 0x40, bonus.mAtk()));
		}
		if (bonus.pDef() > 0) {
			funcs.add(new StatFunc("pDef", StatFunc.Op.ADD, 0x40, bonus.pDef()));
		}
		if (bonus.mDef() > 0) {
			funcs.add(new StatFunc("mDef", StatFunc.Op.ADD, 0x40, bonus.mDef()));
		}
		if (bonus.maxHp() > 0) {
			funcs.add(new StatFunc("maxHp", StatFunc.Op.ADD, 0x40, bonus.maxHp()));
		}
		if (bonus.critRate() > 0) {
			funcs.add(new StatFunc("rCrit", StatFunc.Op.ADD, 0x40, bonus.critRate()));
		}
		if (bonus.pAtkSpd() > 0) {
			funcs.add(new StatFunc("pAtkSpd", StatFunc.Op.ADD, 0x40, bonus.pAtkSpd()));
		}
		if (bonus.mAtkSpd() > 0) {
			funcs.add(new StatFunc("mAtkSpd", StatFunc.Op.ADD, 0x40, bonus.mAtkSpd()));
		}
		return funcs;
	}

	/**
	 * Realiza o upgrade de duas Dolls de mesmo chefe e nivel para o nivel seguinte.
	 */
	public synchronized Optional<BossDoll> upgradeDoll(PlayerCharacter player, int dollItemId) {
		if (player == null || player.inventory() == null) {
			return Optional.empty();
		}
		BossDoll current = dollsById.get(dollItemId);
		if (current == null) {
			return Optional.empty();
		}

		BossDoll nextDoll = dollsById.values().stream()
				.filter(d -> d.boss().equalsIgnoreCase(current.boss()) && d.level() == current.level() + 1)
				.findFirst()
				.orElse(null);
		if (nextDoll == null) {
			return Optional.empty();
		}

		Inventory inv = player.inventory();
		int totalOwned = inv.getItemCount(dollItemId);
		if (totalOwned < 2) {
			return Optional.empty();
		}

		if (inventoryService != null) {
			inventoryService.consumeItem(inv, dollItemId, 2, "DollUpgrade");
			var added = inventoryService.addItem(inv, nextDoll.itemId(), 1, "DollUpgrade");
			if (added == null) {
				ItemTemplate tmpl = resolveTemplate(nextDoll.itemId(), nextDoll.boss() + " Doll Lv." + nextDoll.level());
				inv.add(new ItemInstance(objectIdFactory.nextId(), tmpl, player.objectId(), 1));
			}
		} else {
			inv.destroyItemByItemId(dollItemId, 2);
			ItemTemplate tmpl = resolveTemplate(nextDoll.itemId(), nextDoll.boss() + " Doll Lv." + nextDoll.level());
			inv.add(new ItemInstance(objectIdFactory.nextId(), tmpl, player.objectId(), 1));
		}

		var gw = com.lopez.l2j.game.world.GameWorld.getInstance();
		if (gw != null) {
			var online = gw.findPlayer(player.objectId());
			if (online != null) {
				online.sendPacket(new com.lopez.l2j.network.game.packet.GameServerPacket.ItemList(player, false));
			}
		}

		return Optional.of(nextDoll);
	}

	private ItemTemplate resolveTemplate(int itemId, String fallbackName) {
		if (itemTemplateTable != null) {
			var opt = itemTemplateTable.get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		if (inventoryService != null && inventoryService.templates() != null) {
			var opt = inventoryService.templates().get(itemId);
			if (opt.isPresent()) {
				return opt.get();
			}
		}
		return ItemTemplate.etc(itemId, itemId, fallbackName, "other", "normal", 10, "none", 0, true, true, true, true);
	}

	public Map<Integer, BossDoll> allDolls() {
		return Collections.unmodifiableMap(dollsById);
	}
}
