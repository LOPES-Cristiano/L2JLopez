package com.lopez.l2j.network.game;

import static org.junit.jupiter.api.Assertions.*;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.State;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import com.lopez.l2j.network.login.packet.PacketReader;
import com.lopez.l2j.network.login.packet.PacketWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Suite completa de validacao do protocolo Interlude (Revision 746):
 * 1) Correcao dos 4 opcodes criticos (PledgeCrest, SendTradeRequest,
 * RecipeBookItemList, RecipeItemMakeInfo)
 * 2) Decodificacao de todos os 77 novos GameClientPackets (1-byte e 0xD0:xx)
 * 3) Codificacao binaria de todos os novos GameServerPackets (1-byte e 0xFE:xx)
 * 4) Integracao ponta a ponta via GameSession e ExtendedProtocolPacketHandler
 */
class InterludeProtocolFullCoverageTest {

	@Test
	void testFourCriticalOpcodeFixes() {
		// 1. PledgeCrest deve usar 0x6C (Interlude) e nao 0x6A (C4)
		byte[] crest = new PledgeCrest(123, new byte[] { 1, 2, 3, 4 }).encode();
		assertEquals(0x6c, crest[0] & 0xFF);

		// 2. SendTradeRequest deve usar 0x5E (Interlude) e nao 0x70 (OpenUrl no C6)
		byte[] tradeReq = new SendTradeRequest(999).encode();
		assertEquals(0x5e, tradeReq[0] & 0xFF);

		// 3. RecipeBookItemList deve usar 0xD6 (Interlude) e nao 0xDC (ShowCalc no C6)
		byte[] recipeBook = new RecipeBookItemList(true, 500, List.of(10, 20)).encode();
		assertEquals(0xd6, recipeBook[0] & 0xFF);

		// 4. RecipeItemMakeInfo deve usar 0xD7 (Interlude) e nao 0xDD (MonRaceInfo no
		// C6)
		byte[] recipeMake = new RecipeItemMakeInfo(10, true, 250, 500, 1).encode();
		assertEquals(0xd7, recipeMake[0] & 0xFF);
	}

	@Test
	void testAllNewClientPacketsDecodeCorrectly() {
		// GameGuardReply (0xCA) nos 3 estados
		byte[] gg = new PacketWriter().writeC(0xca).writeD(1).writeD(2).writeD(3).writeD(4).toByteArray();
		assertInstanceOf(GameClientPacket.GameGuardReply.class,
				GameClientPacket.decode(State.CONNECTED, gg).orElseThrow());
		assertInstanceOf(GameClientPacket.GameGuardReply.class,
				GameClientPacket.decode(State.AUTHED, gg).orElseThrow());
		assertInstanceOf(GameClientPacket.GameGuardReply.class,
				GameClientPacket.decode(State.IN_GAME, gg).orElseThrow());

		// Movimentacao, Rotacao, Barcos, HTML
		assertInstanceOf(GameClientPacket.RequestChangeMoveType.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x1c).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestChangeWaitType.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x1d).writeD(0).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestLinkHtml.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x20).writeS("help.htm").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.CannotMoveAnymore.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0x36).writeD(10).writeD(20).writeD(30).writeD(40).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.MoveWithDelta.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x41).writeD(5).writeD(6).writeD(7).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestGetOnVehicle.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0x42).writeD(1).writeD(10).writeD(20).writeD(30).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestGetOffVehicle.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0x43).writeD(1).writeD(10).writeD(20).writeD(30).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSiegeInfo.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x47).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.StartRotating.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0x4a).writeD(180).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.FinishRotating.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0x4b).writeD(180).writeD(0).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestReplyStartPledgeWar.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x4e).writeS("WarClan").writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestReplyStopPledgeWar.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x50).writeS("WarClan").writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSurrenderPledgeWar.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x51).writeS("WarClan").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestMoveToLocationInVehicle.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x5c).writeD(1).writeD(10).writeD(20)
						.writeD(30).writeD(1).writeD(2).writeD(3).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.CannotMoveAnymoreInVehicle.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x5d).writeD(1).writeD(10).writeD(20)
						.writeD(30).writeD(40).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPledgeExtendedInfo.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x67).writeS("MyClan").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSurrenderPersonally.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x69).writeS("WarClan").toByteArray())
						.orElseThrow());

		// Party Match, Peticoes, Alianca, Pets, Freight, Manufacture, Manor
		assertInstanceOf(GameClientPacket.RequestPartyMatchConfig.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x6f).writeD(1).writeD(0).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPartyMatchList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x70).writeD(1).writeD(8).writeD(1)
						.writeD(80).writeD(0).writeS("Room").toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPartyMatchDetail.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x71).writeD(1).writeD(0).writeD(0).writeD(50).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPrivateStoreManageCancel.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x75).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPetition.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0x7f).writeS("Help").writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPetitionCancel.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x80).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestGmList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x81).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.AllyLeave.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x84).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.AllyDismiss.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x85).writeS("SubClan").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestDismissAlly.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x86).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSetAllyCrest.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x87).writeD(2).writeB(new byte[] { 9, 8 }).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestAllyCrest.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x88).writeD(77).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestChangePetName.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x89).writeS("Wolfie").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPetUseItem.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x8a).writeD(500).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestGiveItemToPet.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0x8b).writeD(500).writeD(10).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestGetItemFromPet.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0x8c).writeD(500).writeD(5).writeD(0).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestAllyInfo.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x8e).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPetGetItem.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x8f).writeD(600).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPackageSendableItemList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0x9e).writeD(123).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPackageSend.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0x9f).writeD(123).writeD(1).writeD(555).writeD(2).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.SnoopQuit.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xab).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.SnoopQuit.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xb4).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeBookDestroy.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xad).writeD(44).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopManageList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xb0).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopMessageSet.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xb1).writeS("Craft Shop").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopListSet.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xb2).writeD(1).writeD(10).writeD(5000).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopManageQuit.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xb3).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopMakeInfo.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xb5).writeD(100).writeD(10).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopMakeItem.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xb6).writeD(100).writeD(10).writeD(5000).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecipeShopManagePrev.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xb7).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.ObserverReturn.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xb8).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestBuyProcure.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xc3).writeD(1).writeD(1).writeD(100)
						.writeD(5000).writeD(2).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestBuySeed.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0xc4).writeD(1).writeD(1).writeD(5033).writeD(5).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPreviewItem.class,
				GameClientPacket.decode(State.IN_GAME,
						new PacketWriter().writeC(0xc6).writeD(0).writeD(10).writeD(1).writeD(6379).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSendFriendMsg.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xcc).writeS("Hello").writeS("Friend").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestRecordInfo.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xcf).toByteArray()).orElseThrow());

		// 0xD0 Extended ClientPackets
		assertInstanceOf(GameClientPacket.RequestOustFromPartyRoom.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x01).writeD(200).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestDismissPartyRoom.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x02).writeD(1).writeD(0).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestWithdrawPartyRoom.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x03).writeD(1).writeD(0).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestChangePartyLeader.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x04).writeS("NewLead").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestChangePartyLeader.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0xee).writeS("NewLead").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestManorList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x08).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestProcureCropList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x09).writeD(1)
						.writeD(101).writeD(5000).writeD(1).writeD(5).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSetSeed.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x0a).writeD(1).writeD(1)
						.writeD(5033).writeD(100).writeD(20).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestSetCrop.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x0b).writeD(1).writeD(1)
						.writeD(5000).writeD(100).writeD(50).writeC(1).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestWriteHeroWords.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x0c).writeS("For Aden!").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExAskJoinMPCC.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x0d).writeS("Party2Lead").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExAcceptJoinMPCC.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x0e).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExOustFromMPCC.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x0f).writeS("Party2Lead").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestOlympiadObserverEnd.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x12).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestOlympiadMatchList.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x13).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestAskJoinPartyRoom.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x14).writeS("Candidate").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.AnswerJoinPartyRoom.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x15).writeD(1).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestListPartyMatchingWaitingRoom.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x16).writeD(1).writeD(1)
						.writeD(85).writeD(1).toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExitPartyMatchingWaitingRoom.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x17).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPledgeSetAcademyMaster.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x19).writeD(1)
						.writeS("Master").writeS("Apprentice").toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExFishRanking.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x1f).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPCCafeCouponUse.class,
				GameClientPacket
						.decode(State.IN_GAME,
								new PacketWriter().writeC(0xd0).writeH(0x20).writeS("COUPON-123").toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestPledgeReorganizeMember.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x24).writeD(1)
						.writeS("Knight").writeD(100).writeS("Other").toByteArray()).orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExMPCCShowPartyMembersInfo.class,
				GameClientPacket
						.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x26).writeD(100).toByteArray())
						.orElseThrow());
		assertInstanceOf(GameClientPacket.RequestExMagicSkillUseGround.class,
				GameClientPacket.decode(State.IN_GAME, new PacketWriter().writeC(0xd0).writeH(0x2f).writeD(10)
						.writeD(20).writeD(30).writeD(1417).writeD(1).writeC(0).toByteArray()).orElseThrow());
	}

	@Test
	void testAllNewServerPacketsEncodeCorrectOpcodes() {
		assertEquals(0x02, new NpcSay(1, 0, 30001, "Hi").encode()[0] & 0xFF);
		assertEquals(0x0b, new SpawnItem(1, 57, 0, 0, 0, true, 100).encode()[0] & 0xFF);
		assertEquals(0x14, new AuthLoginFail(1).encode()[0] & 0xFF);
		assertEquals(0x1c, new SunRise().encode()[0] & 0xFF);
		assertEquals(0x1d, new SunSet().encode()[0] & 0xFF);
		assertEquals(0x24, new CharDeleteFail(1).encode()[0] & 0xFF);
		assertEquals(0x29, new TargetSelected(1, 2, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x4b, new EquipUpdate(1, 2, 3).encode()[0] & 0xFF);
		assertEquals(0x55, new PledgeShowMemberListAdd("Mem", 70, 88, 0, 0, 1, 0).encode()[0] & 0xFF);
		assertEquals(0x56, new PledgeShowMemberListDelete("Mem").encode()[0] & 0xFF);
		assertEquals(0x59, new VehicleInfo(1, 0, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x5a, new VehicleDeparture(1, 300, 100, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x5b, new OnVehicleCheckLocation(1, 0, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x5c, new GetOnVehicle(1, 2, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x5d, new GetOffVehicle(1, 2, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x62, new StartRotation(1, 90, 1, 0).encode()[0] & 0xFF);
		assertEquals(0x63, new StopRotation(1, 90, 0).encode()[0] & 0xFF);
		assertEquals(0x65, new StartPledgeWar("A", "B").encode()[0] & 0xFF);
		assertEquals(0x67, new StopPledgeWar("A", "B").encode()[0] & 0xFF);
		assertEquals(0x69, new SurrenderPledgeWar("A", "B").encode()[0] & 0xFF);
		assertEquals(0x70, new OpenUrl("https://l2.com").encode()[0] & 0xFF);
		assertEquals(0x71, new MoveToLocationInVehicle(1, 2, 0, 0, 0, 1, 1, 1).encode()[0] & 0xFF);
		assertEquals(0x72, new StopMoveInVehicle(1, 2, 0, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x73, new ValidateLocationInVehicle(1, 2, 0, 0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0x74, new TradeItemUpdate(List.of()).encode()[0] & 0xFF);
		assertEquals(0x7b, new FriendStatus(true, "Buddy", 10).encode()[0] & 0xFF);
		assertEquals(0x7d, new FriendAddRequest("Buddy").encode()[0] & 0xFF);
		assertEquals(0x7f, new AbnormalStatusUpdate(List.of()).encode()[0] & 0xFF);
		assertEquals(0x82, new PledgeShowMemberListDeleteAll().encode()[0] & 0xFF);
		assertEquals(0x83, new PledgeInfo(1, "Clan", "Ally").encode()[0] & 0xFF);
		assertEquals(0x8c,
				new ServerObjectInfo(1, 30000, "Chair", false, 0, 0, 0, 0, 10, 20, 100, 100).encode()[0] & 0xFF);
		assertEquals(0x96, new PartyMatchList(List.of()).encode()[0] & 0xFF);
		assertEquals(0x97, new PartyMatchDetail(1, 8, 1, 85, 0, 1, "Room").encode()[0] & 0xFF);
		assertEquals(0xa7, new PartyMemberPosition(List.of()).encode()[0] & 0xFF);
		assertEquals(0xa8, new AskJoinAlly(1, "Leader").encode()[0] & 0xFF);
		assertEquals(0xae, new AllyCrest(1, new byte[] { 1, 2 }).encode()[0] & 0xFF);
		assertEquals(0xb2, new PetItemList(List.of()).encode()[0] & 0xFF);
		assertEquals(0xb3, new PetInventoryUpdate(List.of()).encode()[0] & 0xFF);
		assertEquals(0xb4, new AllyInfo("Ally", 10, 5, "Clan", "Leader", List.of()).encode()[0] & 0xFF);
		assertEquals(0xba, new VehicleStarted(1, 1).encode()[0] & 0xFF);
		assertEquals(0xc2, new PackageToList(Map.of(1, "Alt")).encode()[0] & 0xFF);
		assertEquals(0xc3, new PackageSendableList(1, 1000, List.of()).encode()[0] & 0xFF);
		assertEquals(0xc5, new FlyToLocation(1, 0, 0, 0, 1, 1, 1, 0).encode()[0] & 0xFF);
		assertEquals(0xc8, new NormalCamera().encode()[0] & 0xFF);
		assertEquals(0xcc, new TitleUpdate(1, "Hero").encode()[0] & 0xFF);
		assertEquals(0xcd, new PledgeStatusChanged(1, 2, 3, 4, 5).encode()[0] & 0xFF);
		assertEquals(0xce, new RelationChanged(1, 0, false, 0, 0).encode()[0] & 0xFF);
		assertEquals(0xd1, new SetSummonRemainTime(100, 50).encode()[0] & 0xFF);
		assertEquals(0xd5, new Snoop(1, "Player", 0, "Player", "Hi").encode()[0] & 0xFF);
		assertEquals(0xd8, new RecipeShopManageList(1, 1000, true, List.of(), List.of()).encode()[0] & 0xFF);
		assertEquals(0xd9, new RecipeShopSellList(1, 100, 100, 1000, List.of()).encode()[0] & 0xFF);
		assertEquals(0xda, new RecipeShopItemInfo(1, 10, 100, 100, 1).encode()[0] & 0xFF);
		assertEquals(0xdb, new RecipeShopMsg(1, "Shop").encode()[0] & 0xFF);
		assertEquals(0xdd, new MonRaceInfo(0, 0).encode()[0] & 0xFF);
		assertEquals(0xdf, new ObservationMode(0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0xe0, new ObservationReturn(0, 0, 0).encode()[0] & 0xFF);
		assertEquals(0xe1, new ChairSit(1, 2).encode()[0] & 0xFF);
		assertEquals(0xe8, new BuyListSeed(100, 1, List.of()).encode()[0] & 0xFF);
		assertEquals(0xe9, new SellListProcure(100, List.of()).encode()[0] & 0xFF);
		assertEquals(0xed, new ConfirmDlg(1510, "Cleric", 10000, 1).encode()[0] & 0xFF);
		assertEquals(0xee, new PartySpelled(0, 1, List.of()).encode()[0] & 0xFF);
		assertEquals(0xef, new ShopPreviewList(1, 100, List.of()).encode()[0] & 0xFF);
		assertEquals(0xf0, new ShopPreviewInfo(new int[17]).encode()[0] & 0xFF);
		assertEquals(0xf1, new CameraMode(0).encode()[0] & 0xFF);
		assertEquals(0xf2, new ShowXMasSeal(5555).encode()[0] & 0xFF);
		assertEquals(0xf4, new ShortBuffStatusUpdate(1, 1, 15).encode()[0] & 0xFF);
		assertEquals(0xf7, new ClanHallDecoration(21, 1, 1, 1, 1, 1, 1, 1, 1).encode()[0] & 0xFF);
		assertEquals(0xf9, new GameGuardQuery().encode()[0] & 0xFF);
		assertEquals(0xfd, new L2FriendSay("A", "B", "Hello").encode()[0] & 0xFF);

		// 0xFE Extended ServerPackets
		assertFeSubOpcode(0x01, new ExRegMax(1, 10, 1.0));
		assertFeSubOpcode(0x09, new ExColosseumFenceInfo(1, 2, 0, 0, 0, 100, 100));
		assertFeSubOpcode(0x0e, new ExPartyRoomMember(1, List.of()));
		assertFeSubOpcode(0x0f, new ExClosePartyRoom());
		assertFeSubOpcode(0x10, new ExManagePartyRoomMember(0, new PartyRoomMemberEntry(1, "A", 0, 1, 1, 1)));
		assertFeSubOpcode(0x19, new ExQuestInfo());
		assertFeSubOpcode(0x1c, new ExShowSeedInfo(1, List.of()));
		assertFeSubOpcode(0x1d, new ExShowCropInfo(1, List.of()));
		assertFeSubOpcode(0x1e, new ExShowManorDefaultInfo(List.of()));
		assertFeSubOpcode(0x1f, new ExShowSeedSetting(1, List.of()));
		assertFeSubOpcode(0x20, new ExShowCropSetting(1, List.of()));
		assertFeSubOpcode(0x21, new ExShowSellCropList(1, List.of()));
		assertFeSubOpcode(0x22, new ExShowProcureCropDetail(1, List.of()));
		assertFeSubOpcode(0x23, new ExHeroList(List.of()));
		assertFeSubOpcode(0x24, new ExServerPrimitive("Dbg", 0, 0, 0));
		assertFeSubOpcode(0x25, new ExOpenMPCC());
		assertFeSubOpcode(0x26, new ExCloseMPCC());
		assertFeSubOpcode(0x27, new ExAskJoinMPCC("Lead"));
		assertFeSubOpcode(0x2a, new ExOlympiadSpelledInfo(1, List.of()));
		assertFeSubOpcode(0x2c, new ExOlympiadMatchEnd());
		assertFeSubOpcode(0x2d, new ExMailArrived());
		assertFeSubOpcode(0x30, new ExMultiPartyCommandChannelInfo("Lead", 0, 5, List.of()));
		assertFeSubOpcode(0x31, new ExPCCafePointInfo(10, 5, 1, 60, 0));
		assertFeSubOpcode(0x32, new ExSetCompassZoneCode(11));
		assertFeSubOpcode(0x34, new ExAskJoinPartyRoom("Lead"));
		assertFeSubOpcode(0x35, new ExListPartyMatchingWaitingRoom(0, List.of()));
		assertFeSubOpcode(0x37, new ExShowAdventurerGuideBook());
		assertFeSubOpcode(0x39, new PledgeSkillList(List.of()));
		assertFeSubOpcode(0x3a, new PledgeSkillListAdd(370, 1));
		assertFeSubOpcode(0x3f, new PledgeReceiveSubPledgeCreated(100, "Royal", "Lead"));
		assertFeSubOpcode(0x40, new ExRedSky(10));
		assertFeSubOpcode(0x43, new ShowPCCafeCouponShowUI());
		assertFeSubOpcode(0x44, new ExOrcMove());
		assertFeSubOpcode(0x47, new ExRestartClient());
		assertFeSubOpcode(0x48, new ExRequestHackShield());
		assertFeSubOpcode(0x49, new ExUseSharedGroupItem(1539, 1, 5, 10));
		assertFeSubOpcode(0x4a, new ExMPCCShowPartyMemberInfo(List.of()));
		assertFeSubOpcode(0x4f, new ExDuelUpdateUserInfo("A", 1, 88, 80, 1000, 1000, 500, 500, 800, 800));
		assertFeSubOpcode(0x5b, new ExShowSlideshowKamael());
		assertFeSubOpcode(0x5c, new ExPlayScene());
		assertFeSubOpcode(0x5d, new ExSpawnEmitter(1, 2));
		assertFeSubOpcode(0x5e, new ExEnchantSkillInfoDetail(6622, 1));
		assertFeSubOpcode(0x63, new ExAutoFishing(1));
		assertFeSubOpcode(0x67, new ExShowTrace(List.of()));
		assertFeSubOpcode(0x7a, new ExOlympiadUserInfoSpectator(1, 1, "Hero", 88, 1000, 1000, 500, 500));
		assertFeSubOpcode(0x7d, new ExShowFortressMapInfo(101, true, 3));
		assertFeSubOpcode(0x80, new ExPrivateStoreSetWholeMsg(1, "Package"));
		assertFeSubOpcode(0x81, new RequestEnchant(1));
		assertFeSubOpcode(0x8a, new ExShowCubTimer(30));
		assertFeSubOpcode(0x8b, new ExCubTimerStop());
	}

	@Test
	void testGameSessionIntegrationForNewPackets() {
		List<GameServerPacket> sent = new ArrayList<>();
		GameWorld world = new GameWorld();
		var ctx = new GameSession.Context(
				740, 750, null, null, null, world, null, null, null, null, null, null, null, null, null, null, null,
				null, null, List.of(), null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, "TestServer");
		GameSession session = new GameSession(ctx, new byte[16], "127.0.0.1", sent::add);
		PlayerCharacter pc = new PlayerCharacter(1001, "DwarfCrafter", 80, 83400, 147900, -3400);
		session.setState(State.IN_GAME);
		try {
			var activeField = GameSession.class.getDeclaredField("active");
			activeField.setAccessible(true);
			activeField.set(session, pc);
			var inWorldField = GameSession.class.getDeclaredField("inWorld");
			inWorldField.setAccessible(true);
			inWorldField.set(session, true);
		} catch (Exception e) {
			fail(e);
		}
		world.add(session);

		// 1. Dwarven Manufacture
		assertTrue(session.handle(new PacketWriter().writeC(0xb0).toByteArray()));
		assertTrue(sent.stream().anyMatch(RecipeShopManageList.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0xb1).writeS("Best Crafts").toByteArray()));
		assertTrue(sent.stream().anyMatch(RecipeShopMsg.class::isInstance));
		sent.clear();

		// Action 37 (Dwarven Manufacture) - valida skill e abre RecipeShopManageList
		pc.classId(57); // Warsmith (Dwarf)
		assertTrue(session.handle(new PacketWriter().writeC(0x45).writeD(37).writeD(0).writeC(0).toByteArray()));
		assertTrue(sent.stream().anyMatch(RecipeShopManageList.class::isInstance));
		sent.clear();

		// Registra loja de Dwarven Manufacture via 0xB2 (RequestRecipeShopListSet) e
		// valida sincronizacao com RecipeService
		assertTrue(session.handle(new PacketWriter().writeC(0xb2).writeD(1).writeD(20).writeD(500).toByteArray()));
		assertEquals(PlayerCharacter.STORE_TYPE_MANUFACTURE, pc.privateStoreType());
		assertEquals(500,
				com.lopez.l2j.game.recipe.RecipeService.getInstance().getManufactureCost(pc.objectId(), 20).orElse(-1));
		sent.clear();

		// Fecha loja via 0xB3 (RequestRecipeShopManageQuit)
		assertTrue(session.handle(new PacketWriter().writeC(0xb3).toByteArray()));
		assertEquals(PlayerCharacter.STORE_TYPE_NONE, pc.privateStoreType());
		sent.clear();

		// 2. Party Matching Room
		assertTrue(session.handle(new PacketWriter().writeC(0x6f).writeD(1).writeD(1).writeD(1).toByteArray()));
		assertTrue(sent.stream().anyMatch(PartyMatchDetail.class::isInstance));
		assertTrue(sent.stream().anyMatch(ExPartyRoomMember.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0x70).writeD(0).writeD(8).writeD(1).writeD(85).writeD(0)
				.writeS("All").toByteArray()));
		assertTrue(sent.stream().anyMatch(PartyMatchList.class::isInstance));
		sent.clear();

		// 3. Vehicles / Boats
		assertTrue(session
				.handle(new PacketWriter().writeC(0x42).writeD(1).writeD(100).writeD(200).writeD(-3600).toByteArray()));
		assertTrue(sent.stream().anyMatch(VehicleInfo.class::isInstance));
		assertTrue(sent.stream().anyMatch(GetOnVehicle.class::isInstance));
		sent.clear();

		assertTrue(session
				.handle(new PacketWriter().writeC(0x43).writeD(1).writeD(150).writeD(250).writeD(-3500).toByteArray()));
		assertTrue(sent.stream().anyMatch(GetOffVehicle.class::isInstance));
		sent.clear();

		// 4. Manor Seed/Crop Settings & Shop Preview
		assertTrue(session.handle(new PacketWriter().writeC(0xd0).writeH(0x0a).writeD(1).writeD(1).writeD(5033)
				.writeD(100).writeD(20).toByteArray()));
		assertTrue(sent.stream().anyMatch(ExShowSeedSetting.class::isInstance));
		assertTrue(sent.stream().anyMatch(ExShowSeedInfo.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0xd0).writeH(0x0b).writeD(1).writeD(1).writeD(5000)
				.writeD(100).writeD(50).writeC(1).toByteArray()));
		assertTrue(sent.stream().anyMatch(ExShowCropSetting.class::isInstance));
		assertTrue(sent.stream().anyMatch(ExShowCropInfo.class::isInstance));
		assertTrue(sent.stream().anyMatch(ExShowManorDefaultInfo.class::isInstance));
		sent.clear();

		assertTrue(session
				.handle(new PacketWriter().writeC(0xc6).writeD(0).writeD(1).writeD(1).writeD(6379).toByteArray()));
		assertTrue(sent.stream().anyMatch(ShopPreviewList.class::isInstance));
		assertTrue(sent.stream().anyMatch(ShopPreviewInfo.class::isInstance));
		sent.clear();

		// 5. Observation Mode, Hero Words & PC Cafe
		session.extendedProtocolHandler().enterObservationMode(1000, 2000, -3000);
		assertTrue(sent.stream().anyMatch(ObservationMode.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0xb8).toByteArray()));
		assertTrue(sent.stream().anyMatch(ObservationReturn.class::isInstance));
		assertTrue(sent.stream().anyMatch(NormalCamera.class::isInstance));
		assertTrue(sent.stream().anyMatch(CameraMode.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0xd0).writeH(0x0c).writeS("Hero Msg").toByteArray()));
		assertTrue(sent.stream().anyMatch(ExHeroList.class::isInstance));
		sent.clear();

		assertTrue(session.handle(new PacketWriter().writeC(0xd0).writeH(0x20).writeS("CAFE-777").toByteArray()));
		assertTrue(sent.stream().anyMatch(ShowPCCafeCouponShowUI.class::isInstance));
		assertTrue(sent.stream().anyMatch(ExPCCafePointInfo.class::isInstance));
		sent.clear();

		// 6. Diagnostics helper
		session.extendedProtocolHandler().sendProtocolExtendedDiagnostics(null, null);
		assertFalse(sent.isEmpty());
	}

	private static void assertFeSubOpcode(int expectedSubOpcode, GameServerPacket pkt) {
		byte[] bytes = pkt.encode();
		PacketReader r = new PacketReader(bytes);
		assertEquals(0xfe, r.readC());
		assertEquals(expectedSubOpcode, r.readH());
	}
}
