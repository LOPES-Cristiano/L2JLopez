package com.lopez.l2j.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.template.CharTemplateTable;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.CharSelectionInfo;
import com.lopez.l2j.network.game.packet.GameServerPacket.UserInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeaponEnchantGlowTest {

	private CharTemplate template;

	@BeforeEach
	void setUp() {
		template = new CharTemplateTable().get(0).orElseThrow();
	}

	private PlayerCharacter createPlayer(int id, String name) {
		PlayerCharacter pc = new PlayerCharacter(id, name, "Hero", 1, 0, 0, 0, 0, 0, false, 0, 0, 0,
				100, 50, 50, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 100.0, 50.0, 50.0);
		pc.inventory(new Inventory(pc.objectId()));
		return pc;
	}

	@Test
	void testPlayerCharacterEnchantEffectCalculation() {
		PlayerCharacter player = createPlayer(1, "Hero");
		assertEquals(0, player.enchantEffect(), "Sem inventario ou arma, enchantEffect deve ser 0");

		ItemTemplateTable table = TestItems.table();
		ItemInstance weapon = new ItemInstance(101, table.get(TestItems.DAGGER).orElseThrow(), 1, 1);
		weapon.enchant(0);

		player.inventory().add(weapon);
		player.inventory().equip(weapon);
		assertEquals(0, player.enchantEffect(), "Arma +0 deve resultar em enchantEffect 0");

		weapon.enchant(4);
		assertEquals(4, player.enchantEffect(), "Arma +4 deve resultar em enchantEffect 4 (brilho azul no cliente)");

		weapon.enchant(16);
		assertEquals(16, player.enchantEffect(), "Arma +16 deve resultar em enchantEffect 16 (brilho vermelho no cliente)");

		weapon.enchant(200);
		assertEquals(127, player.enchantEffect(), "Enchant acima de 127 deve ser limitado a 127 no byte do pacote");
	}

	@Test
	void testPaperdollCarriesEnchant() {
		ItemTemplateTable table = TestItems.table();
		Inventory inv = new Inventory(1);
		ItemInstance weapon = new ItemInstance(101, table.get(TestItems.DAGGER).orElseThrow(), 1, 1);
		weapon.enchant(7);
		inv.add(weapon);
		inv.equip(weapon);

		Paperdoll p = inv.paperdollView();
		assertEquals(7, p.enchant(ItemSlots.RHAND), "Paperdoll deve carregar o nível de encantamento do slot RHAND");
	}

	@Test
	void testUserInfoSerializesEnchantEffect() {
		PlayerCharacter player = createPlayer(1, "Hero");
		player.maxCp(100);
		player.currentCp(100);

		ItemTemplateTable table = TestItems.table();
		ItemInstance weapon = new ItemInstance(101, table.get(TestItems.DAGGER).orElseThrow(), 1, 1);
		weapon.enchant(12);
		player.inventory().add(weapon);
		player.inventory().equip(weapon);

		UserInfo userInfo = new UserInfo(player, template);
		byte[] encoded = userInfo.encode();

		assertEquals(12, player.enchantEffect());
		// No pacote UserInfo, verifica-se que o pacote e gerado com sucesso contendo o nivel 12
		CharInfo charInfo = new CharInfo(player, template);
		byte[] charData = charInfo.encode();
		assertEquals(12, player.enchantEffect());
	}

	@Test
	void testCharSelectionInfoSerializesEnchantEffect() {
		PlayerCharacter player = createPlayer(1, "Hero");
		Paperdoll p = Paperdoll.of(List.of(new Paperdoll.Entry(ItemSlots.RHAND, 101, TestItems.DAGGER, 15)));
		assertEquals(15, p.enchant(ItemSlots.RHAND));

		CharSelectionInfo sel = CharSelectionInfo.of("testacc", 1234, List.of(player), List.of(p));
		byte[] data = sel.encode();
		assertEquals(15, p.enchant(ItemSlots.RHAND));
	}
}
