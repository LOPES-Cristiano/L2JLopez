# ROTEIRO MESTRE: 510 MODIFICACOES E MELHORIAS DE PARIDADE REAL (L2JLOPEZ VS L2JDREAM V2 & L2JLUCERA 2)

> Auditoria cirurgica ponta a ponta cobrindo Rede/Protocolo (#001-#100), Combate/Skills/Stats/Formulas (#101-#200), Itens/Pets/Summons/Cubics/Economia (#201-#285), Mundo/GeoEngine/IA/GrandBosses (#286-#370), Clas/Sieges/SevenSigns/Olimpiadas/Quests (#371-#440) e Admin/Voiced/BBS/Eventos/SQL (#441-#510).

---

# Relatório de Auditoria Cirúrgica — Subagente 1: Rede, Protocolo, Opcodes e Pacotes (Interlude 746)

Abaixo estão os **100 itens técnicos concretos e acionáveis (#001 a #100)** resultantes da comparação linha a linha entre `L2JLopez` (`com.lopez.l2j.network.*`), `L2JDreamV2` (`com.dream.game.network.*`) e `L2JLucera2` (`l2.gameserver.network.*`).

---

## EIXO 1: `GameClientPacket.java` vs `L2GamePacketHandler.java` (Dream) / `GamePacketHandler.java` (Lucera) — Opcodes, Sub-opcodes (`0xd0`), Leitura Binária e Estados (#001 a #022)

- **#001 — Vazamento de Estado `AUTHED` para Sub-opcodes `0xd0` (`IN_GAME`):**
  - **L2JLopez:** `GameClientPacket.java:768` (`decode` no `case AUTHED`) roteia incondicionalmente `case 0xd0 -> extended(r)`. Qualquer pacote estendido de mundo (`RequestAutoSoulShot`, `RequestRefine`, `RequestDuelStart`, `RequestProcureCropList`, etc.) é decodificado antes de `EnterWorld`.
  - **Referência Dream / Lucera:** `L2GamePacketHandler.java:81-125` (Dream) e `GamePacketHandler.java:320-335` (Lucera) restringem `0xd0` em `AUTHED` exclusivamente a sub-opcodes de lobby (`0xd0:0x08` `RequestManorList` pós-`CharSelected` ou `0xd0:0x36` `GotoLobby`).

- **#002 — Ausência do Opcode Primário `0xa8` (`NetPing`):**
  - **L2JLopez:** `GameClientPacket.java:771-947` não mapeia o opcode `0xa8`, fazendo com que toda resposta de ping do cliente Interlude caia em `Unknown` e polua logs/métricas.
  - **Referência Lucera:** `GamePacketHandler.java:270, 341, 866` decodifica `0xa8 -> new NetPing()` (lendo `kId`, `ping`, `dta` = 3 `readD`) nos estados `CONNECTED`, `AUTHED` e `IN_GAME`.

- **#003 — Ausência do Opcode Primário `0xa6` (`RequestSkillCoolTime` / `RequestSetCastleSiegeTime`):**
  - **L2JLopez:** `GameClientPacket.java:898` salta de `0xa0` para `0xa7`, omitindo `0xa6`.
  - **Referência Lucera / Dream:** `GamePacketHandler.java:858` (`0xa6 -> new RequestSkillCoolTime()` / `RequestSetCastleSiegeTime`).

- **#004 — Ausência do Opcode Primário `0xc8` (`PetitionVote`):**
  - **L2JLopez:** `GameClientPacket.java:935` salta de `0xc7` para `0xca`, omitindo `0xc8`.
  - **Referência Lucera:** `GamePacketHandler.java:989` (`0xc8 -> new PetitionVote()`, lendo `readD()`, `readD()`, `readS()`).

- **#005 — Ausência do Sub-opcode Estendido `0xd0:0x36` (`GotoLobby`):**
  - **L2JLopez:** `GameClientPacket.java:1085-1197` (`extended(PacketReader r)`) não possui o sub-opcode `0x36`.
  - **Referência Lucera:** `GamePacketHandler.java:321` mapeia `0xd0:0x36 -> new GotoLobby()` para retorno à tela de seleção de personagem.

- **#006 — Ausência dos Sub-opcodes Estendidos `0xd0:0x21` (`RequestKeyMapping`) e `0xd0:0x25` (`RequestPledgeSetMemberPowerGrade` / `RequestExSetPledgeCrestLarge`):**
  - **L2JLopez:** `GameClientPacket.java:1148-1159` pula `0x21` e `0x25` no switch de `extended(PacketReader r)`.
  - **Referência Dream / Lucera:** `L2GamePacketHandler.java:460-520` e `GamePacketHandler.java:1010-1120`.

- **#007 — Gambiarra Heurística nos Opcodes `0x52` (`RequestReplySurrenderPledgeWar`) e `0x53` (`RequestSetPledgeCrest`):**
  - **L2JLopez:** `GameClientPacket.java:829-841` inspeciona `peekLen` no opcode `0x52` para adivinhar se o pacote é `RequestSetPledgeCrest` ou `RequestReplySurrenderPledgeWar`, e no `0x53`, quando `r.remaining() < 4` ou `length == 0`, retorna `new RequestPledgeMemberList()` em vez de `RequestSetPledgeCrest(new byte[0])`, impedindo a remoção de emblema de clã no cliente Interlude (que envia `length = 0` no opcode `0x53`).
  - **Referência Dream / Lucera:** `L2GamePacketHandler.java:244-249` (`0x52 -> RequestReplySurrenderPledgeWar`, `0x53 -> RequestSetPledgeCrest` lendo `length = readD()`; se `length == 0`, remove a crest).

- **#008 — Mapeamento Incorreto do Opcode `0xb7` (`RequestRecipeShopManagePrev` vs `RequestRecipeShopSellList`):**
  - **L2JLopez:** `GameClientPacket.java:919` mapeia `0xb7` sem ler bytes (`new RequestRecipeShopManagePrev()`) e `0xb8` como `RequestRecipeShopManagePrev` (`r.readD()`).
  - **Referência Lucera:** `GamePacketHandler.java:918-925` (`0xb6` = `RequestRecipeShopListSet`, `0xb7` = `RequestRecipeShopSellList(readD())`, `0xb8` = `RequestRecipeShopManageQuit`).

- **#009 — Leitura Incompleta em `0x48` (`ValidatePosition`):**
  - **L2JLopez:** `GameClientPacket.java:780-781` lê apenas 4 inteiros (`x`, `y`, `z`, `heading`) e descarta o 5º `readD()` (`data` / `boatObjectId`).
  - **Referência Dream / Lucera:** `clientpackets/ValidatePosition.java:36-40` lê 5 `readD()` (`_x`, `_y`, `_z`, `_heading`, `_data`).

- **#010 — Leitura Incompleta em `0x14` (`UseItem`):**
  - **L2JLopez:** `GameClientPacket.java:823` lê apenas 1 inteiro (`new UseItem(r.readD())`), ignorando o 2º `readD()` (`ctrlPressed`) enviado pelo cliente Interlude.
  - **Referência Lucera:** `c2s/UseItem.java:23-26` lê `_objectId = readD()` e `_ctrlPressed = readD() == 1`.

- **#011 — Bug de `IllegalArgumentException` / Buffer Underflow em `0x91` (`SetPrivateStoreListBuy`):**
  - **L2JLopez:** `GameClientPacket.java:965-975` (`readSetPrivateStoreListBuy`) valida `r.remaining() < count * 12`, mas dentro do laço lê `readD() + readH() + readH() + readD() + readD()` = **16 bytes por item**. Qualquer pacote com tamanho entre `12 * count` e `16 * count - 1` estoura `IllegalArgumentException: Packet underflow` em `PacketReader`.
  - **Referência Dream / Lucera:** `clientpackets/SetPrivateStoreListBuy.java:43` valida `count * 16` bytes (`_items = new int[count * 4]`).

- **#012 — Leitura Truncada e Divisão Arbitrária de `entryId` em `0xa7` (`MultiSellChoose`):**
  - **L2JLopez:** `GameClientPacket.java:1034-1044` (`readMultiSellChoose`) lê apenas 12 bytes (`listId`, `entryId`, `amount`), ignorando os 26 bytes totais do protocolo Interlude (`enchantment`, `augmentationId`, `manaLeft`), e corrompe `entryId` com `int cleanEntryId = entryId >= 100000 ? entryId / 100000 : entryId`, quebrando listas MultiSell com mais de 999 entradas ou IDs altos.
  - **Referência Dream / Lucera:** `clientpackets/MultiSellChoose.java:48-58` lê `_listId`, `_entryId`, `_amount`, `_enchantment`, `_augmentation`, `_mana`.

- **#013 — Descarte de `manorId`/`itemObjectId` em `0xc3` (`RequestBuyProcure`):**
  - **L2JLopez:** `GameClientPacket.java:1207-1223` (`readBuyProcure`) faz `r.readD(); // service/objectId` descartando o `itemObjectId` de cada entrada e não armazena o `manorId` lido no cabeçalho no record `RequestBuyProcure(List<ProcureItem> items)`.
  - **Referência Dream:** `clientpackets/RequestBuyProcure.java:40-54` preserva `_manorId` e os 3 campos por item.

- **#014 — Sub-opcodes Pós-Interlude Misturados em `extended(PacketReader r)` (`0xee`, `0x45`, `0x46`):**
  - **L2JLopez:** `GameClientPacket.java:1095, 1152-1155` mapeia sub-opcodes inexistentes no cliente 746 (`case 0x04, 0xee -> RequestChangePartyLeader`, `case 0x22, 0x45 -> RequestCursedWeaponList`, `case 0x23, 0x46 -> RequestCursedWeaponLocation`), onde `0x45`/`0x46` são de Kamael/Gracia.
  - **Referência Dream:** `L2GamePacketHandler.java:435-512` usa estritamente a tabela Interlude (`0x01` a `0x30`).

- **#015 — Leitura Incorreta em `0x7d` (`RequestRestartPoint`):**
  - **L2JLopez:** `GameClientPacket.java:793` lê `new RequestRestartPoint(r.remaining() >= 4 ? r.readD() : 0)`, mas em Siege de Fortaleza/ClanHall ou ressurreição específica, pacotes truncados (0 bytes) são aceitos silenciosamente como `0` (To Village), permitindo bypass de restrições de respawn.
  - **Referência Dream:** `clientpackets/RequestRestartPoint.java:37` exige `readD()` obrigatório.

- **#016 — Leitura de `0x22` (`RequestLinkHtml`) Sem Validação de Caracteres de Controle / Nulos:**
  - **L2JLopez:** `GameClientPacket.java:810` lê `r.readS()` cru em `RequestLinkHtml`, enquanto `GameSession.onLinkHtml` (`GameSession.java:3325`) apenas verifica `..`, `/` e `\`.
  - **Referência Dream / Lucera:** `clientpackets/RequestLinkHtml.java` valida formato estrito `.htm` sem parâmetros injetados.

- **#017 — Leitura de `0x6b` (`RequestAcquireSkillInfo`) e `0x6c` (`RequestAcquireSkill`) Sem Validação de `skillType`:**
  - **L2JLopez:** `GameClientPacket.java:856-857` aceita qualquer inteiro para `skillType` (mesmo negativo ou fora de `0..2`).
  - **Referência Dream:** `clientpackets/RequestAcquireSkill.java` valida `_skillType` (normal `0`, fishing `1`, clan `2`).

- **#018 — Leitura de `0x8b` (`RequestGiveNickName`) Sem Ordem Padrão e Validação de Tamanho:**
  - **L2JLopez:** `GameClientPacket.java:884` lê `new RequestGiveNickName(r.readS(), r.readS())` sem limitar tamanho de `target` e `title` na decodificação.
  - **Referência Dream:** `clientpackets/RequestGiveNickName.java:36-38`.

- **#019 — Leitura de `0x9c` (`RequestPackageSendableItemList`) e `0x9f` (`RequestPackageSend`) com Limites Frouxos:**
  - **L2JLopez:** `GameClientPacket.java:1017-1032` (`readPackageSend`) permite `count` até `500` itens, quando o limite de slots de inventário/freight no Interlude é `<= 80` (`Config.MAX_ITEM_IN_PACKET`), abrindo brecha para alocação excessiva por pacote.
  - **Referência Dream:** `clientpackets/RequestPackageSend.java:46` rejeita `count > Config.MAX_ITEM_IN_PACKET`.

- **#020 — Leitura de `0xd0:0x06` (`RequestExAcceptJoinMPCC`) Descartando o 2º `readD()`:**
  - **L2JLopez:** `GameClientPacket.java:1100` lê apenas `r.remaining() >= 4 ? r.readD() : 0` e ignora o 2º `readD()` (`unk`) do pacote.
  - **Referência Dream:** `clientpackets/RequestExAcceptJoinMPCC.java:34-35` lê `_response = readD()` e `_unk = readD()`.

- **#021 — Leitura de `0xd0:0x0d` (`RequestExMPCCAskJoin`) e `0xd0:0x0e` (`RequestExMPCCAcceptJoin`) Duplicados com `0xd0:0x06`/`0x07`:**
  - **L2JLopez:** `GameClientPacket.java:1098-1102` e `1116-1122` mapeia `0xd0:0x0d` como `RequestExMPCCAskJoin` e `0xd0:0x0e` como `RequestExMPCCAcceptJoin`, sendo que no Interlude 746 `0xd0:0x0d` é `RequestManorList` / `RequestProcureCropList` ou `RequestSetSeed` dependendo da tabela, e `0xd0:0x06`/`0x07` já são `RequestExAskJoinMPCC`/`RequestExAcceptJoinMPCC`! Na verdade, em `L2GamePacketHandler.java:443-456` (Dream):
    - `0x08` = `RequestManorList`
    - `0x09` = `RequestProcureCropList`
    - `0x0a` = `RequestSetSeed`
    - `0x0b` = `RequestSetCrop`
    - `0x0c` = `RequestWriteHeroWords`
    - `0x0d` = `RequestExAskJoinMPCC`
    - `0x0e` = `RequestExAcceptJoinMPCC`
    - `0x0f` = `RequestExOustFromMPCC`
    - `0x10` = `RequestPledgeCrestLarge`
    - `0x11` = `RequestSetPledgeCrestLarge`
    Enquanto `GameClientPacket.java:1098-1135` deslocou toda a tabela entre `0x06` e `0x13` (`0x06` = AskJoinMPCC, `0x07` = AcceptJoinMPCC, `0x08` = OustFromMPCC, `0x09` = ShowPartyMembersInfo, `0x0a` = ManorList, `0x0b` = ProcureCropList, `0x0c` = SetSeed, `0x0f` = SetCrop, `0x10` =HeroWords, `0x12` = CrestLarge, `0x13` = SetCrestLarge)! Isso causa colisão/inversão total de sub-opcodes de Manor, MPCC, Hero Words e Large Crest no cliente Interlude 746!
  - **Referência Dream / Lucera:** `L2GamePacketHandler.java:436-466` e `GamePacketHandler.java:1015-1055`.

- **#022 — Desalinhamento de Sub-opcodes Estendidos `0xd0:0x14` a `0xd0:0x20` (PartyMatching, PetUseItem, EnchantSkill, FishRanking, Olympiad):**
  - **L2JLopez:** `GameClientPacket.java:1132-1147` mapeia:
    - `0x14` -> `RequestExEnchantSkillInfo` (no Interlude 746, `0x14` é `RequestExPledgeCrestLarge` / `RequestOlympiadObserverEnd` é `0x12` e `0x06` é `ExEnchantSkillInfo`!)
    - `0x15` -> `RequestExEnchantSkill` (no Interlude 746, `0x07` é `RequestExEnchantSkill`!)
    - `0x16` -> `RequestListPartyMatchingWaitingRoom`
    - `0x17` -> `RequestExitPartyMatchingWaitingRoom`
    - `0x18` -> `RequestGetBossRecord`
  - **Referência Dream / Lucera:** Em `L2GamePacketHandler.java:436-485` (Dream):
    - `0x06` = `RequestExEnchantSkillInfo`
    - `0x07` = `RequestExEnchantSkill`
    - `0x12` = `RequestOlympiadObserverEnd`
    - `0x13` = `RequestOlympiadMatchList`
    - `0x14` = `RequestAskJoinPartyRoom`
    - `0x15` = `AnswerJoinPartyRoom`
    - `0x16` = `RequestListPartyMatchingWaitingRoom`
    - `0x17` = `RequestExitPartyMatchingWaitingRoom`
    - `0x18` = `RequestGetBossRecord`
    - `0x19` = `RequestPledgeSetAcademyMaster`
    - `0x1a` = `RequestPledgePowerGradeList`
    - `0x1b` = `RequestPledgeMemberPowerInfo`
    - `0x1c` = `RequestPledgeSetMemberPowerGrade`
    - `0x1d` = `RequestPledgeMemberInfo`
    - `0x1e` = `RequestPledgeWarList`
    - `0x1f` = `RequestExFishRanking`
    - `0x20` = `RequestPCCafeCouponUse`
    O mapeamento de `0xd0:0x06` até `0xd0:0x20` em `GameClientPacket.java` está completamente trocado em relação ao cliente Interlude 746!

---

## EIXO 2: `GameServerPacket.java` vs `serverpackets` (Dream) / `s2c` (Lucera) — Estruturas Binárias Interlude 746, Campos Hardcoded `0` e Desalinhamento de Bytes (#023 a #055)

- **#023 — `GMViewCharacterInfo` (`0x8f`) Corrompido por Omissão dos 68 Bytes de Augmentação do Paperdoll:**
  - **L2JLopez:** `GameServerPacket.java:2312-2313` chama `writePaperdoll(w, paperdoll)` (17 objectIds + 17 itemIds) e escreve imediatamente `w.writeD(stats.pAtk())`, omitindo os 17 `writeD` (34 `writeH` = 68 bytes) de augmentação do paperdoll exigidos pelo cliente Interlude 746. Todos os campos subsequentes (`pAtk`, `mAtk`, `title`, `clanId`, `maxCp`, `nameColor`) ficam deslocados em 68 bytes, corrompendo a janela Alt+G Character Info.
  - **Referência Dream:** `GMViewCharacterInfo.java:101-134` escreve os 68 bytes de augmentação após os 17 itemIds do paperdoll.

- **#024 — `GMViewItemList` (`0x94`) Corrompido por Omissão de `augmentationId` e `mana` por Item:**
  - **L2JLopez:** `GameServerPacket.java:2405-2417` escreve apenas 28 bytes por item no loop de `GMViewItemList`, omitindo `writeD(augmentationId)` e `writeD(mana)` (8 bytes finais por item). Qualquer jogador com `>= 1` item no inventário gera leitura desalinhada no cliente do GM.
  - **Referência Dream:** `GMViewItemList.java:60-68` escreve `writeD(augmentationId)` e `writeD(temp.getMana())` ao final de cada item.

- **#025 — `GMViewWarehouseWithdrawList` (`0x95`) com Estrutura Binária Inválida para Itens Equipáveis e Não-Equipáveis:**
  - **L2JLopez:** `GameServerPacket.java:2442-2456` escreve incondicionalmente `bodyPart(D)`, `enchant(H)`, `0(H)`, `0(H)`, `objectId(D)` para todos os itens (mesmo consumíveis/materiais não-equipáveis) e omite os 2 `writeD` de augmentação para itens equipáveis.
  - **Referência Dream:** `GMViewWarehouseWithdrawList.java:53-81` só escreve `bodyPart`, `enchant`, `soulShotCount`, `spiritShotCount` e os 2 `writeD` de augmentação quando `item.getItem().isEquipable()` é `true`.

- **#026 — `TradeStart` (`0x1e`), `TradeOwnAdd` (`0x20`) e `TradeOtherAdd` (`0x21`) Desalinhados em 2 Bytes por Item:**
  - **L2JLopez:** `GameServerPacket.java:3270, 3290, 3310` escreve `w.writeD(it.augmentationId())` (4 bytes) no final de cada item, totalizando 30 bytes por item. No protocolo Interlude 746, o campo final é `writeH(0x00)` (2 bytes, totalizando 28 bytes por item). A partir do 2º item na janela de Trade, todos os itens ficam deslocados em `2 * N` bytes!
  - **Referência Dream:** `TradeStart.java:41-51` e `TradeOwnAdd.java:24-33` terminam cada item com `writeH(enchantLevel); writeH(0x00); writeH(0x00);` (28 bytes).

- **#027 — `ExEnchantSkillList` (`0xfe:0x17`) Escrevendo `exp` como `writeD` (4 bytes) em vez de `writeQ` (8 bytes):**
  - **L2JLopez:** `GameServerPacket.java:2048` escreve `w.writeD(s.id()).writeD(s.nextLevel()).writeD(s.sp()).writeD(s.exp())` (16 bytes por skill). O cliente Interlude 746 lê `exp` como `Q` (`int64`, 8 bytes), corrompendo a lista de encantamento de skills quando há mais de 1 habilidade listada.
  - **Referência Dream:** `ExEnchantSkillList.java:65-70` escreve `writeD(s._id); writeD(s._nextLevel); writeD(s._sp); writeQ(s._exp);`.

- **#028 — `SpecialCamera` (`0xc7`) Truncado em 6 `writeD`s em vez de 10 `writeD`s:**
  - **L2JLopez:** `GameServerPacket.java:3489-3500` descarta `turn`, `rise`, `widescreen`, `unk` no construtor de 10 argumentos (`this(id, dist, yaw, pitch, time, duration)`) e escreve apenas 6 `writeD`s em `encode()`, quebrando todas as cutscenes cinemáticas de Bosses (Antharas, Valakas, Baium, Sailren, Frintezza).
  - **Referência Dream:** `SpecialCamera.java:40-53` escreve os 10 `writeD`s (`_id, _dist, _yaw, _pitch, _time, _duration, _turn, _rise, _widescreen, _unk`).

- **#029 — `UserInfo` (`0x04`) com Construtores Secundários que Zeram `clanCrestId`, `allyId` e `allyCrestId`:**
  - **L2JLopez:** `GameServerPacket.java:219-229` define 3 construtores sobrecarregados (`UserInfo(c, t)`, `UserInfo(c, t, paperdoll, currentLoad)`, `UserInfo(c, t, paperdoll, currentLoad, stats)`) que passam `0, 0, 0` para `clanCrestId, allyId, allyCrestId`. Chamadores como `PartyClanPacketHandler.java:251, 286, 322` e `HennaPacketHandler.java:179, 319` apagam visualmente a crest do clã e da aliança do jogador na tela!
  - **Referência Dream:** `UserInfo.java:301-306` busca sempre `clanId`, `clanCrestId`, `allyId`, `allyCrestId` diretamente do `L2PcInstance`.

- **#030 — `UserInfo` (`0x04`) com Campos Hardcoded `0` (`cubics`, `partyMatchRoom`, `flyRunSpd`, `cursedWeaponLevel`, `specialEffects`, `relation`):**
  - **L2JLopez:** `GameServerPacket.java:289-331` escreve `writeD(0).writeD(0)` para `flyRunSpd/flyWalkSpd`, `writeH(0)` para `cubics` (nunca exibe cubics ativos), `writeC(0)` para `partyMatchRoom`, `writeD(0)` para `specialEffects`, `writeD(0)` para `cursedWeaponLevel`, e `relation` só considera `isClanLeader() ? 0x40 : 0` (ignorando estado de Siege `0x180`/`0x80`).
  - **Referência Dream:** `UserInfo.java:34-42, 277-278, 315-365`.

- **#031 — `CharInfo` (`0x03`) com `relation`, `cubics`, `partyMatchRoom`, `cursedWeaponLevel` e `isInvisible` Hardcoded `0`:**
  - **L2JLopez:** `GameServerPacket.java:1616-1653` escreve `writeD(0)` para `relation` (linha 1616), `writeC(0)` para `invisible` (linha 1621), `writeH(0)` para `cubics` (linha 1626), `writeC(0)` para `partyMatchRoom` (linha 1627), `writeD(0x00)` para `cursedWeaponLevel` (linha 1652), e não suporta polimorfismo (`0x16` `NpcInfo` quando o jogador está morphed/decoy).
  - **Referência Dream:** `CharInfo.java:83-165, 320-415`.

- **#032 — `CharSelectionInfo` (`0x13`) com `augmentationId` Hardcoded `0x00`:**
  - **L2JLopez:** `GameServerPacket.java:92` escreve `w.writeD(0x00); // augmentation` fixo em `0`, impedindo que armas augmentadas brilhem na tela de seleção de personagens (Lobby).
  - **Referência Dream:** `CharSelectionInfo.java:371` escreve `writeD(charInfoPackage.getAugmentationId())`.

- **#033 — `NpcInfo` (`0x16`) com Multiplicador de Movimento `1.1` e Crests de Clã/Aliança Hardcoded `0`:**
  - **L2JLopez:** `GameServerPacket.java:1324, 1349-1353` escreve `w.writeF(1.1)` fixo para `moveMultiplier` (causando deslizamento visual de NPCs com buffs/debuffs de velocidade) e `w.writeD(0).writeD(0).writeD(0).writeD(0)` para `clanId, clanCrestId, allyId, allyCrestId` (guardas/NPCs de cidades nunca exibem a bandeira do clã dono do castelo, ignorando `Config.SHOW_NPC_CREST`).
  - **Referência Dream:** `AbstractNpcInfo.java` calcula `moveMultiplier` real e preenche crests da cidade/castelo.

- **#034 — `PetInfo` (`0xb1`) e `PetStatusUpdate` (`0xb5`) com `expThisLevel = 0L` e `expNextLevel = 1000L` Hardcoded:**
  - **L2JLopez:** `GameServerPacket.java:1449-1452, 1464-1469` e `1491-1492` cravam `writeQ(0L)` (`expThisLevel`), `writeQ(1000L)` (`expNextLevel`), `writeD(0)` (`curLoad`), `writeD(0)` (`maxLoad`), `writeH(0)` (`mountable`), `writeD(1)` (`soulshotsPerHit`) e `writeD(1)` (`spiritshotsPerHit`), quebrando a barra de EXP, o peso e a montaria na janela de Pet/Summon.
  - **Referência Dream:** `PetInfo.java:105-138` e `PetStatusUpdate.java:52-63`.

- **#035 — `PledgeShowInfoUpdate` (`0x88`) e `PledgeShowMemberListAll` (`0x53`) Trocando `hideoutId` (ClanHall) por `fortId` e Hardcoding `atWar = 0`:**
  - **L2JLopez:** `GameServerPacket.java:2537, 2543` (`PledgeShowInfoUpdate`) e `2560, 2568` (`PledgeShowMemberListAll`) escrevem `w.writeD(clan.fortId())` no campo que no protocolo Interlude 746 é `hasHideout` (`clan.hideoutId()`), fazendo com que clãs donos de Clan Hall apareçam como "Sem Clan Hall" na janela de clã, além de cravar `writeD(0)` para `atWar`!
  - **Referência Dream:** `PledgeShowInfoUpdate.java:46-54` e `PledgeShowMemberListAll.java:60, 68` escrevem `writeD(_clan.getHasHideout())` e `writeD(_clan.isAtWar() ? 1 : 0)`.

- **#036 — `PledgeShowMemberListAll` (`0x53`) e `PledgeShowMemberListUpdate` (`0x54`) Escrevendo `1` em vez de `objectId` no Campo Online:**
  - **L2JLopez:** `GameServerPacket.java:2575-2578` (`PledgeShowMemberListAll`) e `2595-2597` (`PledgeShowMemberListUpdate`) escrevem `w.writeD(0)` (`sex`), `w.writeD(0)` (`race`), `w.writeD(m.isOnline() ? 1 : 0)` e `w.writeD(0)` (`sponsor`). No cliente Interlude 746, o campo `online` deve conter o **`objectId` real do jogador** (`m.isOnline() ? m.objectId() : 0`), caso contrário o cliente não habilita botões de interação/whisper na janela do clã!
  - **Referência Dream:** `PledgeShowMemberListAll.java:85-95` escreve `sex`, `race`, `m.isOnline() ? m.getObjectId() : 0` e `sponsor`.

- **#037 — `GMViewPledgeInfo` (`0x90`), `GMViewQuestInfo` (`0x93`) e `GMHennaInfo` (`0xea`) Implementados como Stubs Falsos:**
  - **L2JLopez:** `GameServerPacket.java:2347-2363` (`GMViewPledgeInfo`) ignora o `Clan` real e escreve `"Clan " + clanId` com apenas 1 membro falso; `GMViewQuestInfo` (linhas 2388-2393) escreve `writeH(0)` sem a lista de quests e omite o segundo `writeH(0)` de itens de quest; `GMHennaInfo` (linhas 2423-2431) escreve `writeD(0)` fixo sem slots/hennas.
  - **Referência Dream:** `GMViewPledgeInfo.java`, `GMViewQuestInfo.java` e `GMHennaInfo.java`.

- **#038 — `WareHouseDepositList` (`0x41`) e `WareHouseWithdrawalList` (`0x42`) com Augmentação Hardcoded `writeQ(0)`:**
  - **L2JLopez:** `GameServerPacket.java:1917, 1940` escrevem `w.writeQ(0)` fixo ao final de cada item, ocultando as propriedades de augmentação de todos os itens guardados ou depositados no Warehouse.
  - **Referência Dream:** `WareHouseDepositList.java:86-94` e `WareHouseWithdrawalList.java:65-73` escrevem os 2 `writeD` do `augmentationId`.

- **#039 — `ShortCutInit` (`0x45`) e `ShortCutRegister` (`0x44`) Omitindo Dados Reais de Reuse e Augmentação de Itens:**
  - **L2JLopez:** `GameServerPacket.java:694-701` (`ShortCutInit`) e `728-735` (`ShortCutRegister`) escrevem `writeD(-1).writeD(0).writeD(0).writeH(0).writeH(0)` fixos para atalhos de itens (`type == 1`), impedindo que armas augmentadas na barra de atalhos mostrem o ícone/brilho de augmentação e o cooldown compartilhado.
  - **Referência Dream:** `ShortCutInit.java:55-76` e `ShortCutRegister.java:48-66`.

- **#040 — `Attack` (`0x05`) Sem Suporte a Múltiplos Hits (Dual-Wield / Polearm):**
  - **L2JLopez:** `GameServerPacket.java:1740-1760` suporta apenas 1 único hit e escreve `w.writeH(0)` hardcoded na linha 1757, tornando impossível representar o segundo golpe de Dual Swords/Fists ou múltiplos alvos de Polearm em um pacote `Attack` oficial.
  - **Referência Dream:** `Attack.java:32-95` suporta array `Hit[] _hits` com `writeH(_hits.length - 1)`.

- **#041 — `Die` (`0x06`) sem Construtor Baseado em `Castle`/`ClanHall`/`SiegeFlag` do Personagem:**
  - **L2JLopez:** `GameServerPacket.java:537-566` define `Die(int objectId, boolean sweepable)` com `toHideout = false`, `toCastle = false`, `toSiegeHQ = false`, e em `GameSession.onPlayerDeath` os botões de retorno para Clan Hall, Castelo e Bandeira de Siege não verificam o estado real de Siege/ClanHall do clã do jogador.
  - **Referência Dream:** `Die.java:57-97` calcula `_showClanhall`, `_showCastle`, `_showFlag` e `_fixedres`.

- **#042 — `EtcStatusUpdate` (`0xf3`) com Construtor Padrão que Zera `messageRefusal`, `dangerZone`, `charmOfCourage` e `deathPenalty`:**
  - **L2JLopez:** `GameServerPacket.java:1090-1092` (`EtcStatusUpdate(PlayerCharacter c, PlayerStats stats)`) passa `0, 0, 0, 0` fixos para `messageRefusal, dangerZone, charmOfCourage, deathPenalty`, apagando na UI os ícones de bloqueio de chat, zona de perigo, Charm of Courage e Death Penalty sempre que chamado sem os 6 parâmetros!
  - **Referência Dream:** `EtcStatusUpdate.java:37-52` lê todos os 8 estados diretamente do `L2PcInstance`.

- **#043 — `PrivateStoreManageListSell` (`0x9a`) e `PrivateStoreListSell` (`0x9b`) com `type2`, `augment` e `storePrice` Hardcoded `0`:**
  - **L2JLopez:** `GameServerPacket.java:2911-2966` escreve `writeH(0)` para `customType1`, `writeD(0)` para `augment` e `writeD(0)` para `storePrice` (preço de referência da loja) nas listas de venda privada.
  - **Referência Dream:** `PrivateStoreManageListSell.java:56-85` e `PrivateStoreListSell.java:55-74`.

- **#044 — `SiegeInfo` (`0xc9`), `SiegeAttackerList` (`0xca`) e `SiegeDefenderList` (`0xcb`) com Dados de Aliança e Crest Hardcoded `""` / `0`:**
  - **L2JLopez:** `GameServerPacket.java:3413-3475` escreve `writeS("").writeS("").writeD(0)` para nome da aliança, líder da aliança e crest da aliança em todos os pacotes de interface de Siege.
  - **Referência Dream:** `SiegeInfo.java:80-115`, `SiegeAttackerList.java:60-95`, `SiegeDefenderList.java:60-95`.

- **#045 — `ConfirmDlg` (`0xed`) Limitado a 1 Único Parâmetro de Texto (`TYPE_TEXT`):**
  - **L2JLopez:** `GameServerPacket.java:3547-3559` aceita apenas `(int messageId, String requestorName)` e crava `writeD(1).writeD(0).writeS(...)`, impedindo o envio de `ConfirmDlg` com múltiplos parâmetros (como `Ressurreição` com `%` de EXP, `Summon Friend` com nome de zona, ou tempo `time` e `requesterId` reais do protocolo Interlude).
  - **Referência Dream / Lucera:** `ConfirmDlg.java:35-145` suporta lista de parâmetros tipados mais `writeD(_time)` e `writeD(_requesterId)` no final! (Nota: `L2JLopez` omite `writeD(time)` e `writeD(requesterId)` ao final de `ConfirmDlg`, truncando o pacote!).

- **#046 — `MonRaceInfo` (`0xdd`) Implementado como Stub Vazio (`writeD(0)`):**
  - **L2JLopez:** `GameServerPacket.java:4514-4523` escreve apenas `writeD(unknown1).writeD(unknown2).writeD(0)` (0 corredores), omitindo o loop das 8 raias com `npcId`, `loc` e os 20 bytes de velocidades da Monster Derby Track.
  - **Referência Dream:** `MonRaceInfo.java:42-77`.

- **#047 — `SellListProcure` (`0xe9`) e `BuyListSeed` (`0xe8`) com Tipos de Campo Simplificados (`writeH(4)` fixo):**
  - **L2JLopez:** `GameServerPacket.java:4736-4773` crava `writeH(4)` para `itemType1` e `writeH(4)` para `itemType2` em todas as sementes de `BuyListSeed`.
  - **Referência Dream:** `BuyListSeed.java:55-68` e `SellListProcure.java:55-68`.

- **#048 — `ExShowSeedSetting` (`0xfe:0x1f`) e `ExShowCropSetting` (`0xfe:0x20`) Usando Dados Dummy quando Chamados pelo Handler:**
  - **L2JLopez:** Embora a estrutura em `GameServerPacket.java:4832-4906` exista, `ExtendedProtocolPacketHandler.java:963-982` popula `SeedSettingItem` e `CropSettingItem` com recompensas fixas `1864, 1865`, limites `1000` e preços `10, 100` em vez de consultar `ManorService`/`CastleManorManager`.
  - **Referência Dream:** `ExShowSeedSetting.java:31-73` e `ExShowCropSetting.java:31-77`.

- **#049 — `SendMacroList` (`0xe7`) com Comentário Errado (`0xe8`) e `rev`/`count` Fixos:**
  - **L2JLopez:** `GameServerPacket.java:3175-3201` grava `w.writeD(0)` fixo para `_rev` e `w.writeC(0)` para `unknown`, não diferenciando exclusão/adição/listagem inicial de múltiplos macros.
  - **Referência Dream:** `SendMacroList.java:37-85`.

- **#050 — `HennaEquipList` (`0xe2`) e `HennaUnequipList` (`0xe5`) Escrevendo `1` Fixo para `canUse`:**
  - **L2JLopez:** `GameServerPacket.java:3013, 3076` escreve `w.writeD(1)` fixo no final de cada item em vez de verificar se a classe atual do jogador pode usar aquela Henna (`HennaTree`).
  - **Referência Dream:** `HennaEquipList.java:56-68`.

- **#051 — `RelationChanged` (`0xce`) Usado Apenas com Constantes Simplificadas sem Máscara Completa de Guerra/Aliança/Party/Siege:**
  - **L2JLopez:** `GameServerPacket.java:2513-2524` define apenas 5 constantes (`RELATION_PVP_FLAG`, `RELATION_HAS_KARMA`, `RELATION_LEADER`, `RELATION_INSIEGE`, `RELATION_ATTACKER`), faltando `RELATION_PARTY1..4`, `RELATION_CLAN_MEMBER`, `RELATION_ALLY`, `RELATION_MUTUAL_WAR`, `RELATION_1SIDED_WAR`, `RELATION_SUBLEADER`.
  - **Referência Dream / Lucera:** `RelationChanged.java:22-55`.

- **#052 — `ObservationMode` (`0xdf`) e `ObservationReturn` (`0xe0`) com `writeC(0x00)` Faltante em `ObservationMode`:**
  - **L2JLopez:** `GameServerPacket.java:3503-3513` escreve `writeC(0xdf).writeD(x).writeD(y).writeD(z).writeC(0x00).writeC(0xc0).writeC(0x00)` (3 bytes finais), enquanto no cliente Interlude `ObservationMode` exige 4 bytes (`writeC(0x00); writeC(0xc0); writeC(0x00); writeC(0x00);`).
  - **Referência Dream:** `ObservationMode.java:36-44`.

- **#053 — `PackageSendableList` (`0xc3`) Escrevendo `augmentationId` como `writeQ(0)` Fixo:**
  - **L2JLopez:** `GameServerPacket.java:4689-4712` não inclui os 2 `writeD` de augmentação nem `customType1`/`customType2` reais dos itens enviáveis por Freight.
  - **Referência Dream:** `PackageSendableList.java:50-73`.

- **#054 — `ExShowScreenMessage` (`0xfe:0x38`) com Campos Fixos e Sem Suporte a `SystemMessageId`, `effect`, `fade` e `unk`:**
  - **L2JLopez:** `GameServerPacket.java:1049-1071` expõe apenas `(String text, int durationMs)` com `position = 2` (TOP_CENTER) e `effect = 0` fixos, impedindo mensagens de Boss/Olympiad/Quest com efeito de terremoto (`effect = 1`), posição customizada ou `sysMsgId`.
  - **Referência Dream / Lucera:** `ExShowScreenMessage.java:30-115`.

- **#055 — Ausência de Pacotes de Servidor Interlude Usados por Sistemas Oficiais (`ExVariationResult`, `ExVariationCancelResult`, `FriendRecvMsg`, `L2Friend`, `L2FriendStatus`, `PledgeStatusChanged`, `PledgeReceiveWarList`, `PledgeReceivePowerInfo`):**
  - **L2JLopez:** `GameServerPacket.java` não possui `PledgeStatusChanged` (`0xcd`), `L2Friend` (`0xfb`), `FriendRecvMsg` (`0xfd` separado de `L2FriendSay`), e em `ExtendedProtocolPacketHandler` envia pacotes vazios ou `PledgeShowInfoUpdate` no lugar de `PledgeStatusChanged`.
  - **Referência Dream / Lucera:** `serverpackets/PledgeStatusChanged.java`, `L2Friend.java`, `ExVariationResult.java`.

---

## EIXO 3: Roteamento em `GameSession.java` e Implementações Stub/Fake em `com.lopez.l2j.network.game.handler.packet.*` (#056 a #085)

- **#056 — `CharacterPacketHandler`, `MultiSellPacketHandler` e `MovementPacketHandler` São Código Morto (Nunca Chamados em `GameSession`):**
  - **L2JLopez:** `CharacterPacketHandler.java` (`@Component`), `MultiSellPacketHandler.java` (`@Component`) e `MovementPacketHandler.java` foram criados no pacote `handler.packet`, mas **nunca são injetados em `GameSession.Context` nem invocados em `GameSession.java`**! Toda a lógica de transição de `FlagZone` em tempo real (`MovementPacketHandler.checkFlagZoneOnMove`), recomendação (`evaluateRecommendation`), auditoria de itens (`shouldLogItemAction`) e custo de Wear (`processWearPreview`) nunca é executada!
  - **Referência:** `GameSession.java:3185-3630`.

- **#057 — `handleMoveWithDelta` (`0x41`) Permite Teleporte por Teclado sem Geodata ou Checagem de Estado:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:154-166` aplica `active.moveTo(nx, ny, nz)` somando `p.dx(), p.dy(), p.dz()` diretamente sem checar se o jogador está morto (`active.isDead()`), sentado, paralisado, conjurando ou atravessando paredes (Geodata)!
  - **Referência Dream:** `clientpackets/MoveWithDelta.java`.

- **#058 — `handleChangeWaitType` (`0x1c`) e `ActionUse(0)` Permitem Sentar/Levantar Morto, Atacando ou Conjurando:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:109-121` e `ActionPacketHandler.java:527-532` alternam `active.sitting(!sitting)` sem verificar `active.isDead()`, `session.casting()`, `session.isAutoAttacking()`, `active.isStoreOpen()` ou `active.isDisabled()`.
  - **Referência Dream:** `clientpackets/ChangeWaitType2.java` e `RequestActionUse.java`.

- **#059 — `handleMoveToLocationInVehicle` (`0x5c`) Dispara `VehicleDeparture` Falso a Cada Passo no Barco:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:209-219` faz broadcast de `new VehicleDeparture(p.boatId(), 300, 100, p.targetX(), p.targetY(), p.targetZ())` sempre que um passageiro anda dentro do barco, além de `handleGetOnVehicle`/`handleGetOffVehicle` (linhas 199-249) confiarem cegamente nas coordenadas enviadas pelo cliente sem validar a existência/distância do barco!
  - **Referência Dream:** `clientpackets/MoveToLocationInVehicle.java` envia `MoveToLocationInVehicle` (`0x71`), jamais `VehicleDeparture` (`0x5a`).

- **#060 — `handleRecordInfo` (`0xcf`) Spawna um Objeto Estático Fantasma (`ServerObjectInfo`) em Cima do Jogador:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:265-278` envia `new ServerObjectInfo(active.objectId(), 30000, active.name(), false, active.x(), active.y(), active.z(), ...)` para o próprio jogador ao receber `RequestRecordInfo`, criando um objeto estático fantasma com o próprio `objectId` do personagem!
  - **Referência Dream:** `clientpackets/RequestRecordInfo.java` envia `UserInfo` + `CharInfo`/`NpcInfo`/`DoorInfo` dos objetos ao redor.

- **#061 — `handleMagicSkillUseGround` (`0xd0:0x2f`) Teleporta o Jogador com `FlyToLocation` em vez de Conjurar no Chão:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:280-290` envia `new FlyToLocation(active.objectId(), p.x(), p.y(), p.z(), ...)` no próprio jogador e depois chama `session.onMagicSkillUse(new RequestMagicSkillUse(p.skillId(), ...))` perdendo as coordenadas `(x, y, z)` do alvo no solo (Signets/Symbol skills)!
  - **Referência Dream:** `clientpackets/RequestExMagicSkillUseGround.java`.

- **#062 — `handleSiegeInfo` (`0x93`) Retorna Sempre o Castelo 1 ("Gludio") Fixo e um Pacote Pós-Interlude (`ExShowFortressMapInfo`):**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:296-306` ignora o castelo selecionado, envia sempre `new SiegeInfo(1, "Gludio", ...)` com horário falso `now + 3600` e ainda envia `new ExShowFortressMapInfo(101, false, 3)` (`0xfe:0x7d`, pacote de Kamael/Gracia inexistente no Interlude 746)!
  - **Referência Dream:** `clientpackets/RequestSiegeInfo.java`.

- **#063 — `handleReplyStartPledgeWar` (`0x4e`), `handleReplyStopPledgeWar` (`0x50`) e `handleSurrenderPersonally` (`0x54`) São Stubs Visuais:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:321-362` apenas envia `StartPledgeWar`, `StopPledgeWar` ou `RelationChanged` para a própria sessão sem consultar nem atualizar `ClanWarService` ou o clã adversário!
  - **Referência Dream:** `clientpackets/RequestReplyStartPledgeWar.java`, `RequestReplyStopPledgeWar.java`, `RequestSurrenderPersonally.java`.

- **#064 — `handlePartyMatchConfig` (`0x6f`) e `handlePartyMatchDetail` (`0x71`) Criam Salas Infinitas e Entram em Salas Aleatórias:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:569-625`: `handlePartyMatchConfig` cria incondicionalmente uma nova sala toda vez que é chamado, e `handlePartyMatchDetail` faz fallback para `PARTY_ROOMS.values().iterator().next()` quando `roomId` não é encontrado, colocando o jogador em uma sala aleatória!
  - **Referência Dream:** `clientpackets/RequestPartyMatchConfig.java` e `RequestPartyMatchDetail.java`.

- **#065 — `handleListPartyMatchingWaitingRoom` (`0xd0:0x16`) Lista Todos os Jogadores Online do Mundo e `handleExitPartyMatchingWaitingRoom` (`0xd0:0x17`) É um Stub Vazio:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:682-696` lista todos os jogadores online do servidor na faixa de nível (mesmo quem nunca entrou na sala de espera), e `handleExitPartyMatchingWaitingRoom` apenas envia `ActionFailed()`.
  - **Referência Dream:** `clientpackets/RequestListPartyMatchingWaitingRoom.java` e `RequestExitPartyMatchingWaitingRoom.java`.

- **#066 — Sistema de Command Channel (`MPCC`) em `ExtendedProtocolPacketHandler` É Completamente Fake:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:702-745` (`handleExAskJoinMPCC`, `handleExAcceptJoinMPCC`, `handleExOustFromMPCC`, `handleExMPCCShowPartyMembersInfo`) não possui modelo de `CommandChannel` unindo múltiplas parties; `handleExAcceptJoinMPCC` apenas envia `ExOpenMPCC` e `ExMPCCPartyInfoUpdate` contendo a própria party do jogador!
  - **Referência Dream:** `model/L2CommandChannel.java` e `clientpackets/RequestExAcceptJoinMPCC.java`.

- **#067 — `handleRecipeShopMakeItem` (`0xb6`) e `handleRecipeBookDestroy` (`0xac`) Não Consomem Materiais, Não Cobram Adena e Não Criam o Item:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:773-863` usa listas efêmeras na instância do handler (`this.recipeBook`, `this.manufactureList`) em vez de `RecipeService`, e `handleRecipeShopMakeItem` (linhas 844-863) apenas envia `RecipeShopItemInfo(..., 1)` (sucesso visual) sem verificar/consumir ingredientes, sem transferir Adena e sem adicionar o item fabricado ao inventário!
  - **Referência Dream:** `RecipeController.java` e `clientpackets/RequestRecipeShopMakeItem.java`.

- **#068 — `handlePackageSendableItemList` (`0x9c`) Envia Personagem Falso `"AccountChar"` e `handlePackageSend` (`0x9f`) Deposita no Próprio Warehouse Pessoal:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:865-892`: `handlePackageSendableItemList` cria um mapa com `chars.put(p.playerObjectId(), "AccountChar")` em vez de listar os personagens da mesma conta, e `handlePackageSend` chama `session.onWareHouseDepositList(new SendWareHouseDepositList(p.items()))`, guardando os itens no Warehouse pessoal do remetente em vez do Freight do destinatário!
  - **Referência Dream:** `clientpackets/RequestPackageSendableItemList.java` e `RequestPackageSend.java`.

- **#069 — Exploit Crítico de Itens Grátis em `handleBuySeed` (`0xc4`) e Dados Falsos de Manor (`0xd0:0x09`–`0x0b`):**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:909-923` (`handleBuySeed`) executa `ctx.inventories().addItem(active.inventory(), it.seedId(), it.count(), "BuySeed")` entregando qualquer item/semente enviado no pacote **sem cobrar 1 único Adena, sem validar se o `seedId` é realmente uma semente vendida pelo Manor e sem descontar do estoque do castelo**! Qualquer jogador pode enviar um pacote `0xc4` forjado com `seedId = 57` (Adena) ou `6657` (Valakas Necklace) e gerar itens infinitos!
  - **Referência Dream:** `clientpackets/RequestBuySeed.java:65-165` valida o NPC Manor Manager, confere `SeedProduction` no `Castle`, cobra Adena e reduz o estoque.

- **#070 — `handlePreviewItem` (`0xc6`) Equipa Visualmente Itens nos Slots Errados (`0..N`), Sem Cobrar Taxa e Sem Timer de Remoção:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:925-942` coloca os `itemId`s recebidos sequencialmente nos índices `0, 1, 2...` do array `previewPaperdoll` (ignorando o `bodyPart` real do item), não cobra `Config.WEAR_PRICE` e não agenda tarefa para remover a visualização após `Config.WEAR_DELAY`!
  - **Referência Dream:** `clientpackets/RequestPreviewItem.java`.

- **#071 — `handleGiveItemToPet` (`0x8b`), `handleGetItemFromPet` (`0x8c`) e `handlePetUseItem` (`0x8a`) São Fakes Visuais:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:999-1025` apenas envia `PetInventoryUpdate` e `PetItemList(List.of())` falsos sem transferir o item entre o inventário do jogador e o inventário do Pet!
  - **Referência Dream:** `clientpackets/RequestGiveItemToPet.java` e `RequestGetItemFromPet.java`.

- **#072 — `handleOlympiadMatchList` (`0xd0:0x13`), `handleWriteHeroWords` (`0xd0:0x0c`), `handleExFishRanking` (`0xd0:0x1f`) e `handlePCCafeCouponUse` (`0xd0:0x20`) São Stubs Falsos:**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:1121-1153`:
    - `handleOlympiadMatchList` envia `ExOlympiadUserInfoSpectator` do próprio jogador e HTML estático `"<html><body>Olympiad Matches</body></html>"`.
    - `handleWriteHeroWords` não verifica `active.isHero()` e envia `ExHeroList` falso de 1 entrada.
    - `handleExFishRanking` envia `ExAutoFishing(0)` (`0xfe:0x63`, pacote Kamael!) e HTML estático `"<html><body>Fishing Championship Ranking</body></html>"`.
    - `handlePCCafeCouponUse` concede `ExPCCafePointInfo(100, 10, 1, 3600, 0)` sem validar o cupom.
  - **Referência Dream:** `Olympiad.java`, `Hero.java`, `FishingChampionshipManager.java`.

- **#073 — `ActionPacketHandler` (`case 3` Trade Request) Não Registra o Pedido em `TradeService`:**
  - **L2JLopez:** `ActionPacketHandler.java:571-583` envia `new SendTradeRequest(active.objectId())` diretamente para `targetSession` sem chamar `ctx.trades().requestTrade(active.objectId(), targetSession.objectId())` (como faz `TradeStorePacketHandler.java:96`). Quando o alvo clica em Aceitar, `ctx.trades().getPendingRequester(...)` retorna vazio e o trade falha sempre!
  - **Referência:** `TradeStorePacketHandler.java:96-112`.

- **#074 — `ActionPacketHandler` (`case 53, 54` Move Summon to Target) Teleporta o Próprio Jogador para Dentro do NPC:**
  - **L2JLopez:** `ActionPacketHandler.java:744-755` (ações 53 e 54, que são "Move Pets/Servitors to Target") executa `active.moveTo(npc.x(), npc.y(), npc.z())` no **próprio jogador**, teleportando instantaneamente o personagem para cima de qualquer NPC/Boss selecionado a até 2500 de distância, além de enviar `MoveToLocation` com origem e destino invertidos!
  - **Referência Dream:** `clientpackets/RequestActionUse.java` (cases 53 e 54 movem o `L2Summon`/`L2PetInstance`, nunca o `L2PcInstance`).

- **#075 — `ActionPacketHandler` Ataque a Portas (`DoorInstance`) Usa Fórmula Hardcoded `150 + level * 15` e `doorId()` em vez de `objectId()`:**
  - **L2JLopez:** `ActionPacketHandler.java:403, 477-498` calcula o dano em portas como `int pAtk = 150 + active.level() * 15` (ignorando arma, buffs e `PlayerStats`) e envia `MyTargetSelected`, `StatusUpdate`, `ValidateLocation` e `Attack` usando `doorOpt.doorId()` em vez de `doorOpt.objectId()`, fazendo com que o cliente Interlude não mostre a animação de ataque nem a redução de HP da porta!
  - **Referência Dream:** `L2DoorInstance.java` usa `getObjectId()` em todos os pacotes de alvo/combate.

- **#076 — `TradeStorePacketHandler.handlePrivateStoreManageBuy` (`0x90`) Envia Lista de Itens Disponíveis Vazia (`List.of()`):**
  - **L2JLopez:** `TradeStorePacketHandler.java:1058` envia `new PrivateStoreManageListBuy(active.objectId(), adena, List.of(), List.of())` com `inventoryItems = List.of()`, enquanto `handleTryOpenPrivateStoreBuy` (linha 1091) popula `invItems`. Quando o cliente solicita `RequestPrivateStoreManageBuy` (`0x90`), a janela de criação de loja de compra abre completamente vazia!
  - **Referência Dream:** `PrivateStoreManageListBuy.java:40` sempre popula `_itemList = player.getInventory().getUniqueItems(false, true)`.

- **#077 — `AdminPacketHandler.handleGMCommand` (`0x6e`) Envia `GMHennaInfo` Zerado, Limite Fixo `80` e Warehouse Vazio (`List.of()`):**
  - **L2JLopez:** `AdminPacketHandler.java:438-443` envia `new GMViewItemList(targetChar.name(), targetChar.inventory().items(), 80)` (ignorando `targetChar.maxInventorySlots()`), `new GMHennaInfo(0, 0, 0, 0, 0, 0)` (sempre 0 hennas) e `new GMViewWarehouseWithdrawList(..., List.of())` (sempre vazio, sem carregar o warehouse do jogador alvo).
  - **Referência Dream:** `clientpackets/RequestGMCommand.java:50-98`.

- **#078 — Comando `/offline` em `CommandPacketHandler` Desconecta o Jogador Sem Ativar Loja Offline, e `.offline` Cria Item Falso de 1 Adena:**
  - **L2JLopez:** `CommandPacketHandler.java:81-88` (`/offline`) envia `"Modo offline ativado..."` e chama `session.onLogout()`, removendo o personagem do mundo! Já `.offline` (`CommandPacketHandler.java:250-258`) cria `items.add(new OfflineShopItem(57, 1, 1))` (vendendo 1 Adena por 1 Adena) em vez de ler a lista real de venda/compra (`session.privateSellItems()` / `session.privateBuyItems()`)!
  - **Referência:** `TradeStorePacketHandler.java`.

- **#079 — `SocialPacketHandler.handleSendFriendMsg` (`0x66`) Permite Enviar `L2FriendSay` para Qualquer Jogador Sem Ser Amigo:**
  - **L2JLopez:** `SocialPacketHandler.java:68-83` não verifica se `targetPlayer` consta na lista de amigos de `active` (`ctx().friends()`), permitindo enviar mensagens de janela de amigo (`0xfd`) para qualquer jogador online mesmo bloqueado ou desconhecido!
  - **Referência Dream:** `clientpackets/SendFriendMsg.java` valida se o destinatário está na friendlist do remetente.

- **#080 — `SocialPacketHandler` (`ALLBLOCK`) Define `active.setBlockingAll(true)`, mas `ChatPacketHandler` Nunca Verifica `isBlockingAll()`:**
  - **L2JLopez:** `SocialPacketHandler.java:168-171` ativa `active.setBlockingAll(true)` (`/allblock`), mas `ChatPacketHandler.java:235-251` (`case CreatureSay.TELL`) só verifica `isBlocked(target, active)` e ignora completamente `target.get().character().isBlockingAll()`!
  - **Referência Dream:** `clientpackets/Say2.java` verifica `receiver.getMessageRefusal()`.

- **#081 — `GameSession.onManorList` (`0xd0:0x08`) Ignora `CastleManorManager` e Envia Lista Estática:**
  - **L2JLopez:** `GameSession.java:3286` responde `RequestManorList` com `ExSendManorList.CASTLES` hardcoded em vez de consultar os castelos registrados no `CastleManager`.
  - **Referência Dream:** `clientpackets/RequestManorList.java`.

- **#082 — `GameSession.onShowBoard` (`0x57`) Ignora o `boardId` Recebido do Cliente e Abre Sempre `_bbshome`:**
  - **L2JLopez:** `GameSession.java:3306` executa `case RequestShowBoard p -> handleBbsCommand("_bbshome")` ignorando `Config.COMMUNITY_TYPE` e o argumento do pacote.
  - **Referência Dream:** `clientpackets/RequestShowBoard.java`.

- **#083 — `PartyClanPacketHandler.handleSetPledgeCrest` (`0x53`) Não Permite Remover o Emblema do Clã (`data.length == 0`):**
  - **L2JLopez:** `PartyClanPacketHandler.java:295-305` exige `if (p.data() == null || p.data().length == 0 || p.data().length > 256) { send(new ActionFailed()); return; }`, bloqueando a exclusão do emblema do clã quando o líder clica em "Delete Crest" (que envia `length = 0`).
  - **Referência Dream:** `clientpackets/RequestSetPledgeCrest.java:56-65` remove a crest quando `_length == 0`.

- **#084 — `PartyClanPacketHandler.handleSetPledgeCrestLarge` (`0xd0:0x11`) Salva a Insignia Grande (`2176` bytes) Sobrescrevendo a Crest Normal (`crestId`):**
  - **L2JLopez:** `PartyClanPacketHandler.java:307-337` chama `ctx.clans().updateCrest(clan.clanId(), p.data())` (que atualiza `crestId` 16x12 em vez de um campo separado `crestLargeId` 64x64) e envia `UserInfo` com `0` para `clanCrestId` (linha 322)!
  - **Referência Dream:** `clientpackets/RequestExSetPledgeCrestLarge.java` atualiza `setCrestLargeId` separadamente de `setCrestId`.

- **#085 — `NpcBypassPacketHandler` Permite Interação com `npc_<objectId>_...` Sem Validar Distância de Interação (`150` unidades) em Todos os Subcomandos:**
  - **L2JLopez:** `NpcBypassPacketHandler.java` processa diversos bypasses de NPC sem validar uniformemente se o jogador ainda está vivo, próximo ao `npc` (`INTERACTION_DISTANCE = 150`) e na mesma `instanceId`.
  - **Referência Dream:** `L2PcInstance.validateBypass` e `L2Npc.canInteract(player)`.

---

## EIXO 4: Segurança de Rede, `FloodProtectors`, `PacketRateLimiter`, `BypassEncoderService`, Validação de Estado e `SystemMessageId`s Oficiais vs `CreatureSay("SYS")` (#086 a #100)

- **#086 — `BypassEncoderService.decode()` Contém Falha Crítica que Aceita Qualquer Bypass Não-Codificado (`BypassType.SIMPLE_DIRECT`):**
  - **L2JLopez:** `BypassEncoderService.java:208` retorna incondicionalmente `return DecodeResult.valid(BypassType.SIMPLE_DIRECT, trimmed);` ao final do método `decode()` quando o bypass não está na tabela da sessão! Além disso, `BypassEncoderService.encodeHtml()` **nunca é chamado** ao enviar `NpcHtmlMessage` ou `ShowBoard`, tornando toda a criptografia/validação de bypass inoperante na prática!
  - **Referência Lucera:** `ValidBypass.java` / `BypassStorage.java` rejeita qualquer bypass de NPC/BBS que não tenha sido previamente armazenado no envio do HTML.

- **#087 — `GranularFloodProtector` É Código Morto (Nunca Injetado nem Chamado em `GameSession`):**
  - **L2JLopez:** `GranularFloodProtector.java` (que implementa os 16 canais de `FloodProtectorChannel`, punições `kick`/`ban`/`jail` e limites de `flood.properties`) é anotado com `@Service`, mas **não existe em `GameSession.Context` e nunca é chamado em `GameSession.handle(byte[] body)`**! Nenhuma proteção de flood configurada em `flood.properties` está ativa!
  - **Referência Dream:** `util/FloodProtector.java` é invocado diretamente nos handlers de pacotes.

- **#088 — `PacketRateLimiter` Cobre Apenas 8 Pacotes e Deixa `UseItem`, `Action`, `RequestBypassToServer`, `MultiSellChoose` e `Warehouse` Sem Rate Limit:**
  - **L2JLopez:** `PacketRateLimiter.java:36-46` registra regras para apenas 8 pacotes (`AttackRequest`, `RequestEnchantItem`, `RequestDestroyItem`, `RequestItemList`, `RequestMagicSkillUse`, `SendBypassBuildCmd`, `Say2`, `RequestRestart`). Pacotes críticos de exploit/flood como `UseItem` (`0x14`), `Action` (`0x04`), `RequestBypassToServer` (`0x21`), `MultiSellChoose` (`0xa7`), `SendWareHouseDepositList` (`0x31`), `SendWareHouseWithDrawList` (`0x32`), `RequestPrivateStoreBuy` (`0x79`) e `RequestPrivateStoreSell` (`0x96`) passam por `DEFAULT_RULE` (`50ms`, sem proteção semântica por subsistema).
  - **Referência Dream / Lucera:** `FloodProtector.Protected` protege `USEITEM`, `MULTISELL`, `TRANSACTION`, `BYPASS`, `MANUFACTURE`, `MANOR`, `SUBCLASS`, `DROPITEM`.

- **#089 — Pacotes `Unknown` Não Desconectam Clientes em `CONNECTED`/`AUTHED` nem Contabilizam Limite de Pacotes Inválidos em `IN_GAME`:**
  - **L2JLopez:** `GameSession.java:3731` apenas registra `log.debug("Opcode ignorado...")` ao receber `GameClientPacket.Unknown` em qualquer estado (`CONNECTED`, `AUTHED`, `IN_GAME`). Clientes maliciosos podem enviar milhares de opcodes inválidos sem nunca serem desconectados.
  - **Referência Dream / Lucera:** `L2GamePacketHandler.java:21-36` (Dream) desconecta imediatamente em `CONNECTED`/`AUTHED` e incrementa `onUnknownPacket()` em `IN_GAME`; `GamePacketHandler.java:1214` (Lucera) chama `client.onUnknownPacket()`.

- **#090 — Vazamento de Memória em `BypassEncoderService` (`sessionBypasses` Sem Limpeza por Diálogo):**
  - **L2JLopez:** `BypassEncoderService.java:53` acumula cada bypass gerado em um `ConcurrentHashMap<String, String>` por sessão (`MAX_CACHED_BYPASSES_PER_SESSION = 512`), e quando atinge 512 entradas dá `map.clear()` apagando abruptamente todos os bypasses ativos da janela atual, em vez de separar listas por diálogo (`NpcHtmlMessage` vs `ShowBoard`) como no Lucera.
  - **Referência Lucera:** `BypassStorage.java` limpa e recria a lista de bypasses a cada novo `NpcHtmlMessage` ou `ShowBoard`.

- **#091 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial no Sistema de Party (`PartyClanPacketHandler`):**
  - **L2JLopez:** `PartyClanPacketHandler.java:81, 86, 159, 179, 206` envia `CreatureSay(0, CreatureSay.ALL, "SYS", ...)` para alvo offline, alvo ocupado, mudança de líder e recusa de party.
  - **Referência Dream:** Deve enviar `SystemMessageId.THE_PLAYER_IS_NOT_ONLINE`, `WAITING_FOR_ANOTHER_REPLY` (`164`), `S1_HAS_BECOME_A_PARTY_LEADER` (`1384`), `PLAYER_DECLINED` (`305`).

- **#092 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial no Sistema de Clã e Sub-unidades (`PartyClanPacketHandler`):**
  - **L2JLopez:** `PartyClanPacketHandler.java:220-275, 344-505, 535-720` usa dezenas de `CreatureSay(0, CreatureSay.ALL, "SYS", ...)` para criação de clã, dissolução, expulsão, aliança, guerra de clã e subpledges.
  - **Referência Dream:** Deve usar `SystemMessageId.CLAN_CREATED` (`189`), `YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT` (`794`), `CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE` (`264`), `CLAN_MEMBER_S1_EXPELLED` (`191`).

- **#093 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial no Sistema de Amizade e Bloqueio (`SocialPacketHandler`):**
  - **L2JLopez:** `SocialPacketHandler.java:48, 53, 57, 64, 77, 102, 104, 111, 124, 144-175` usa `CreatureSay(0, CreatureSay.ALL, "SYS", ...)` para todas as respostas de `/friendinvite`, `/frienddel` e `/block`.
  - **Referência Dream:** Deve usar `SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST` (`165`), `THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME` (`170`), `S1_ADDED_TO_FRIENDS` (`132`), `S1_JOINED_AS_FRIEND` (`479`), `S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST` (`133`), `S1_WAS_ADDED_TO_YOUR_IGNORE_LIST` (`617`), `S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST` (`618`), `MESSAGE_REFUSAL_MODE` (`176`), `MESSAGE_ACCEPTANCE_MODE` (`177`).

- **#094 — Substituição de `CreatureSay("System", ...)` por `SystemMessageId` Oficial no Sistema de Chat (`ChatPacketHandler`):**
  - **L2JLopez:** `ChatPacketHandler.java:106, 111, 115, 142, 147, 151, 205, 214, 224, 232, 239, 245` envia `CreatureSay` com nome `"System"` ou `"SYS"` quando o jogador tenta falar em Party/Clan/Alliance sem estar em um, ou quando o destinatário de Whisper está offline/bloqueado.
  - **Referência Dream:** Deve enviar `SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME` (`145`), `THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE` (`176`), `YOU_ARE_NOT_IN_A_PARTY` (`200`), `YOU_ARE_NOT_A_CLAN_MEMBER` (`212`).

- **#095 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial em Penalidade de Grade e Receitas (`ItemPacketHandler`):**
  - **L2JLopez:** `ItemPacketHandler.java:209, 223, 734` envia `CreatureSay("SYS", "Esta receita ja esta registrada...")`, `CreatureSay("SYS", "Receita '...' adicionada...")` e `CreatureSay("SYS", "The equipment's grade is too high...")`.
  - **Referência Dream:** Deve enviar `SystemMessageId.RECIPES_CANNOT_BE_REGISTERED_ANYMORE` / `S1_HAS_BEEN_ADDED` (`851`) e atualizar `EtcStatusUpdate` para o ícone oficial de Grade Penalty.

- **#096 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial em Zona de Paz e Consumo de Itens (`MagicSkillPacketHandler`):**
  - **L2JLopez:** `MagicSkillPacketHandler.java:153, 333, 343` envia `CreatureSay("SYS", "Itens insuficientes...")` e `CreatureSay("SYS", "Voce nao pode usar habilidades ofensivas em zona de paz.")`.
  - **Referência Dream:** Deve enviar `SystemMessageId.NOT_ENOUGH_ITEMS` (`351`) e `SystemMessageId.TARGET_IN_PEACEZONE` (`85`) / `A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_PEACE_ZONE` (`84`).

- **#097 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial em Trade e Private Store (`TradeStorePacketHandler`):**
  - **L2JLopez:** `TradeStorePacketHandler.java:74, 80, 91, 98, 129, 1051, 1088, 1110, 1126` envia `CreatureSay("SYS", ...)` para bloqueio de trade/loja em zona proibida, distância ou ocupação.
  - **Referência Dream:** Deve enviar `SystemMessageId.S1_IS_BUSY_TRY_LATER` (`153`), `TARGET_IS_INCORRECT` (`144`), `NO_PRIVATE_STORE_HERE` (`1296`), `NO_PRIVATE_WORKSHOP_HERE` (`1297`).

- **#098 — Substituição de `CreatureSay("SYS", ...)` por `SystemMessageId` Oficial em Duelos, Recomendações e Petições (`ExtendedProtocolPacketHandler`):**
  - **L2JLopez:** `ExtendedProtocolPacketHandler.java:436-567, 1044-1102` envia `CreatureSay("SYS", ...)` para recomendações (`Evaluate`) e convites/início/fim de Duelo.
  - **Referência Dream:** Deve usar `SystemMessageId.YOU_HAVE_RECOMMENDED_S1_YOU_HAVE_S2_RECOMMENDATIONS_LEFT` (`830`), `S1_HAS_RECOMMENDED_YOU` (`831`), `S1_HAS_BEEN_CHALLENGED_TO_A_DUEL` (`1926`), `S1_HAS_CHALLENGED_YOU_TO_A_DUEL` (`1927`).

- **#099 — Validação de `activeChar.isTeleporting()` e `isInOlympiadMode()` Ausente no Despacho Central de `GameSession.handle()`:**
  - **L2JLopez:** `GameSession.java:3185-3630` despacha pacotes de movimentação, ataque, uso de itens, trade e loja sem um guard central para `isTeleporting()` (entre `TeleportToLocation` e `Appearing`), permitindo que o jogador execute ações, drope itens ou troque equipamentos enquanto está em estado transitório de teleporte.
  - **Referência Dream / Lucera:** Verifica `activeChar.isTeleporting()` nos handlers de ação/movimento/item.

- **#100 — Validação de Integridade de `ProtocolVersion` (`0x00`) Aceitando `version == -2` Sem Fechar Imediatamente a Conexão com `SendStatus` / `KeyPacket`:**
  - **L2JLopez:** `GameSession.java:3638-3651` (`onProtocolVersion`) mantém a sessão aberta caso receba pacotes subsequentes antes do close assíncrono em versões inválidas, enquanto no protocolo L2J Interlude (`ProtocolVersion.java` em Dream/Lucera) `version == -2` é um ping de status que deve fechar imediatamente a conexão e qualquer revisão fora de `740..746` deve enviar `KeyPacket(key, 0)` e encerrar a `GameConnection` de forma síncrona.
  - **Referência Dream / Lucera:** `clientpackets/ProtocolVersion.java:40-65`.

---

# Relatório de Auditoria Técnica: Combate, Skills, Efeitos, Condições, Stats e Fórmulas (#101 a #200)

Abaixo estão os **100 itens técnicos concretos e acionáveis (#101 a #200)** resultantes da comparação cirúrgica entre o **`L2JLopez`** e as bases de referência **`L2JDreamV2`** (`com.dream.game.skills.*` / `com.dream.game.handler.skill.*`) e **`L2JLucera2`** (`l2.gameserver.stats.*` / `l2.gameserver.skills.*`).

---

## Bloco 1: Motor de Stats, Calculadoras e Fórmulas Base de Atributos (#101 a #115)

101. **Motor de Calculadoras (`CalculatorEngine` / `Calculator` / `Stat`) Morto e Desconectado**: Em `L2JLopez` (`com.lopez.l2j.game.stats.CalculatorEngine.java`, `Calculator.java`, `Stat.java`, `FuncAdd.java`, `FuncMul.java`), toda a arquitetura orientada a objetos de funções de stats nunca é instanciada ou chamada em nenhum lugar do servidor; `PlayerStats.java` usa uma lista paralela de `com.lopez.l2j.game.skill.StatFunc` avaliada por *strings* a cada cálculo. Ref.: `L2JDreamV2` (`com.dream.game.skills.Calculator.java` e `Formulas.addFuncsToNewCharacter`) / `L2JLucera2` (`l2.gameserver.stats.Calculator.java`).
102. **Enum `Stat.java` Incompleto (Apenas 19 de 100+ Stats)**: Em `L2JLopez` (`com.lopez.l2j.game.stats.Stat.java`, linhas 6–28), o enum define apenas 19 atributos básicos, faltando mais de 80 stats críticos do Interlude (`cAtk`, `cAtkAdd`, `mCritRate`, `rShld`, `sDef`, `shldAngle`, `absorbDam`, `reflectDam`, `reflectSkillPhysic`, `reflectSkillMagic`, `vengeancePdam`, `vengeanceMdam`, `transDam`, `pSkillEvas`, `MagicalMpConsumeRate`, `PhysicalMpConsumeRate`, `DanceMpConsumeRate`, vulnerabilidades elementais, proficiências e vulnerabilidades de status). Ref.: `L2JDreamV2` (`com.dream.game.skills.Stats.java`) / `L2JLucera2` (`l2.gameserver.stats.Stats.java`).
103. **Duplicação e Divergência entre `Formulas.java` e `CombatService.java`**: Em `L2JLopez` (`com.lopez.l2j.game.combat.Formulas.java` vs `CombatService.java`), `Formulas.java` é código quase morto que diverge matematicamente do `CombatService.java` (ex.: `Formulas.calcPhysicalDamage` na linha 60 usa constante `77.0 * pAtk / pDef`, enquanto `CombatService` usa `70.0 * pAtk / pDef`; `Formulas.calcShieldBlock` vs `CombatService.calcShieldDefense`). É necessário unificar as fórmulas retail em `Formulas.java` e consumi-las em `CombatService.java`. Ref.: `L2JDreamV2` (`com.dream.game.skills.Formulas.java`) / `L2JLucera2` (`l2.gameserver.stats.Formulas.java`).
104. **Erro Crítico na Fórmula de `levelMod` em `PlayerStats.java`**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linha 112), o modificador de nível é calculado como `double levelModRatio = (level + 89.0) / 90.0;` (dividindo por `90.0` em vez de `100.0`). No nível 80, isso gera `1.877` em vez do valor retail `1.69` (`(89 + level) / 100.0`), inflando artificialmente P.Atk, M.Atk, P.Def e M.Def. Ref.: `L2JDreamV2` (`L2Character.getLevelMod()`, `(89 + getLevel()) / 100.0`) / `L2JLucera2` (`Creature.getLevelMod()`).
105. **P.Atk da Arma Não Escala pelo Bônus Absoluto de STR nem `levelMod`**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 106–113 e 171), o bônus de STR é calculado como razão relativa ao STR base da classe (`BaseStatsTable.strBonus(effectiveStr) / BaseStatsTable.strBonus(template.str())`) e aplicado apenas sobre o `template.pAtk()`, somando `weaponPAtk` por fora sem multiplicar por `STR_BONUS * levelMod`. No Interlude retail, `(basePAtk + weaponPAtk)` inteiro é multiplicado por `BaseStats.STR.calcBonus(player) * player.getLevelMod()`. Ref.: `L2JDreamV2` (`Formulas.FuncPAtkMod`, linhas 755–774) / `L2JLucera2` (`Formulas.java`).
106. **M.Atk da Arma Não Escala por `INT^2 * levelMod^2`**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 108–113 e 182), `weaponMAtk` é somado de forma plana ao `template.mAtk() * intRatio^2 * levelModRatio^2` sem receber o multiplicador retail `lvlMod^2 * intBonus^2`. Ref.: `L2JDreamV2` (`Formulas.FuncMAtkMod`, linhas 428–449, `env.value *= lvlb * lvlb * (intb * intb)`).
107. **Dedução de P.Def e M.Def por Slot de Armadura/Joia Vazio ou Equipado Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 175–186), `template.pDef()` e `template.mDef()` são somados integralmente aos itens equipados. No Interlude retail, o template base inclui a defesa dos slots nus (Head=12, Chest=31 fighter/15 mage, Legs=18/8, Gloves=8, Feet=7; Ring=5, Earring=9, Neck=13), que devem ser subtraídos quando um item é equipado naquele slot antes de multiplicar por `levelMod` (e `MEN` para M.Def). Ref.: `L2JDreamV2` (`Formulas.FuncPDefMod` linhas 797–842 e `FuncMDefMod` linhas 634–677).
108. **Bônus de Enchant de Armas e Armaduras Hardcoded (`+2` / `+1`) em vez da Tabela por Grade e Over-Enchant (+4+)**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 137–138, 154 e 157), armas recebem `enchant * 2` fixo de P.Atk/M.Atk e armaduras/joias recebem `enchant * 1` fixo de P.Def/M.Def. No Interlude retail, armaduras/joias dão `+1` do +1 ao +3 e `+3` por nível do +4 em diante; armas escalam por Crystal Grade (ex.: S-Grade Bow dá +10 P.Atk até +3 e +20 por nível no +4+; Dual/2H dá +6/+12; 1H dá +5/+10; M.Atk S-Grade dá +4/+8). Ref.: `L2JDreamV2` (`com.dream.game.skills.funcs.FuncEnchant.java`) / `L2JLucera2` (`l2.gameserver.stats.funcs.FuncEnchant.java`).
109. **Fórmula de Accuracy e Evasion Incorreta (`DEX * 6` e Penalidade Nível > 69/77 Ausente)**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 223–226), `accuracy` e `evasion` usam `(int)(Math.sqrt(effectiveDex) * 6.0) + level + weaponAcc` arredondado prematuramente para `int` e sem o bônus adicional de nível acima de 69/77 (`if (level > 69) val += level - 69; if (level > 77) val += level - 77;`). Ref.: `L2JDreamV2` (`Formulas.FuncAtkAccuracy` e `FuncAtkEvasion`, linhas 76–170) / `L2JLucera2` (`Formulas.java`).
110. **Taxa de Crítico Desarmado (Fist sem Arma) Ignora Base 40**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 136 e 227), quando o jogador está sem arma equipada (`weapon == null`), `weaponCrit` é `0` e `baseCrit` torna-se `(int) Math.round(0 * dexBonus) = 0`, impedindo críticos desarmados. No retail, `env.value = 40` quando desarmado. Ref.: `L2JDreamV2` (`Formulas.FuncAtkCritical`, linhas 131–134).
111. **Taxa de Crítico Mágico (`mCritRate`) Hardcoded em `WIT * 0.5` em vez de `BaseWeaponMCrit * WIT_Bonus`**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 700 e 1081), o crítico mágico base é calculado como `stats.wit() * 0.5` (ex.: WIT 20 = 10% base = 100 em escala 1000) em vez do base retail `80 * BaseStats.WIT.calcBonus(player)` (cap de 300/1000 ou `Config.MCRIT_CAP`). Ref.: `L2JDreamV2` (`Formulas.FuncMAtkCritical`, linhas 399–426) / `L2JLucera2` (`Formulas.calcMCrit`, linhas 423–428).
112. **Fórmula de HP/MP/CP Máximo por Nível Ignora `lvlHpAdd` / `lvlHpMod` Quadrático da Classe**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 192–194), o HP/MP/CP máximo usa apenas `template.maxHpForLevel(level) * conRatio` dividindo pelo `BaseStatsTable.conBonus(template.con())`, em vez de aplicar a fórmula quadrática de `lvlHpAdd`/`lvlHpMod` da `CharTemplate` multiplicada diretamente por `BaseStats.CON.calcBonus(player)` (e `MEN` para MP). Ref.: `L2JDreamV2` (`Formulas.FuncMaxHpAdd`, `FuncMaxHpMul`, `FuncMaxMpAdd`, `FuncMaxMpMul`, `FuncMaxCpAdd`, `FuncMaxCpMul`, linhas 472–632).
113. **Regeneração de HP/MP/CP Ignora `CON`/`MEN`, `levelMod`, Castelo/ClanHall/Fortress, Mother Tree e Seven Signs Festival**: Em `L2JLopez` (`com.lopez.l2j.game.combat.Formulas.java`, linhas 188–202), `calcHpRegen` e `calcMpRegen` usam valores lineares simples (`1.5 + level * 0.1`) sem multiplicar por `BaseStats.CON.calcBonus` / `BaseStats.MEN.calcBonus`, sem `levelMod`, sem modificadores de movimento (`1.5x` sentado, `1.1x` parado, `0.7x` correndo) aplicados sobre o base real da classe, e sem bônus de zona (`MotherTree`, `ClanHall`, `Castle`, `SiegeFlag` 1.5x). Ref.: `L2JDreamV2` (`Formulas.calcHpRegen`, `calcMpRegen`, `calcCpRegen`, linhas 1057–1099, 1407–1539, 1911–2027).
114. **Ordem de Aplicação de `StatFunc` sem Isolamento por Atributo e sem Suporte a Lambda `<add order="0x40" stat="pAtk">` Dependente de Env**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`, linhas 276–329), `collectStatFuncs` ordena todas as funções juntas e `applyStat` re-itera toda a lista linearmente para cada atributo; além disso, `StatFunc.Op` não suporta operações `SET`/`OVERRIDE` ou `SUB` condicional baseadas em `target` para vulnerabilidades de NPC. Ref.: `L2JDreamV2` (`com.dream.game.skills.Calculator.java` e `FuncSet.java`, `FuncSub.java`).
115. **Limite de Capacidade de Carga (`maxLoad`) Não Calculado no `PlayerStats`**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerStats.java`), o `CalculatingResult` não calcula `maxLoad` (`baseLoad * BaseStats.CON.calcBonus(player) * maxLoadStat + weightLimit`), impedindo que passivas como `Weight Limit` (skill 150) e `Increase Weight` escalem corretamente com `CON`. Ref.: `L2JDreamV2` (`Formulas.FuncMaxLoad`, linhas 566–585).

---

## Bloco 2: Fórmulas Matemáticas de Combate, Crítico, Blow, Letal, Escudo e Resistências (#116 a #138)

116. **Crítico de Skills Físicas (`PDAM`) Calculado com `DEX` (`stats.critical() / 2`) em vez de `STR` (`skill.getCriticalRate() * STR_Bonus`)**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 644 e 1023), a chance de crítico de skills físicas usa `int skillCritChance = Math.min(500, stats.critical() / 2);`. No Interlude retail, o crítico de skill física depende de **STR** e do atributo `criticalRate` (ou `baseCritRate`) da skill no XML (`skill.getBaseCritRate() * BaseStats.STR.calcBonus(attacker) * 10`). Ref.: `L2JDreamV2` (`Formulas.calcSkillCrit`) / `L2JLucera2` (`Formulas.calcCrit`, linha 404).
117. **Bônus Estático de Dano Crítico (`cAtkAdd` / Vicious Stance) Somado APÓS a Divisão por `pDef` em Ataques e Skills**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 317, 567, 649 e 1028), `cAtkAdd` (de skills como *Vicious Stance* 312, que adiciona +609 no nível 20) é somado diretamente ao `baseDamage` **depois** de dividir por `pDef` (`baseDamage += cAtkAdd`), causando +609 de dano puro ignorando defesa! Na fórmula retail de ataques normais, `cAtkAdd * 70 / pDef` é somado antes da divisão por `pDef`, e em `BLOW` é multiplicado por `6.1` (ou `6.5`) **antes** de `* 70 / defence`. Ref.: `L2JDreamV2` (`Formulas.calcBlowDamage` linha 1013 e `calcPhysDam` linhas 2156–2163) / `L2JLucera2` (`Formulas.calcPhysDam`, linhas 114, 132 e 167).
118. **Taxa de Acerto de `BLOW` Não Escala com `DEX` nem com `blowRate` (`FATALBLOW_RATE`)**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 209–216), `calcBlowSuccess` retorna apenas as constantes fixas `Config.BLOW_BEHIND` (70%), `Config.BLOW_SIDE` (60%) ou `Config.BLOW_FRONT` (50%), ignorando totalmente o bônus de `DEX` (`BaseStats.DEX.calcBonus(attacker)`), o stat `blowRate` (de buffs/boss jewels/passivas como *Critical Chance*) e a diferença de altura `Z`. Ref.: `L2JDreamV2` (`Formulas.calcBlow`, linhas 955–982) / `L2JLucera2` (`Formulas.calcBlow`, linhas 356–397).
119. **Fórmula de Dano de `BLOW` Idêntica a `PDAM` e Multiplicando `skill.power()` por Soulshot Indevidamente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 615–650 e 995–1030), skills `BLOW` (Deadly Blow, Backstab, Lethal Blow, Blinding Blow) usam a mesma fórmula de `PDAM`, dobrando `(pAtk + power)` inteiro com Soulshot e aplicando um crítico secundário de `2.0x`. No Interlude retail (`Formulas.calcBlowDamage`), quando o Blow acerta, ele já é tratado como um acerto crítico especial onde o Soulshot dobra o `pAtk` (e só multiplica `power` se `ssBoost > 0`), somando `((pAtk + power) * cAtk + 6.5 * cAtkAdd) * critVuln * daggerVuln * 70 / pDef`. Ref.: `L2JDreamV2` (`Formulas.calcBlowDamage`, linhas 984–1055) / `L2JLucera2` (`Formulas.calcPhysDam`, linhas 101–123).
120. **Lethal Strike (`lethal1` e `lethal2`) Nunca Executado em Combate nem Parseado do XML**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 222–235), o método `calcLethalSuccess` existe mas **nunca é chamado em nenhum lugar do código**, e `SkillTable.java` / `SkillTemplate.java` não fazem o parse dos atributos `lethal1` e `lethal2` dos XMLs. Skills como *Lethal Blow*, *Backstab*, *Deadly Blow*, *Blinding Blow*, *Lethal Shot*, *Banish Undead* e *Turn Undead* nunca aplicam Half-Kill (CP=1 em players, -50% HP em mobs) nem Lethal Strike (CP=1 e HP=1 em players, HP=1 em mobs). Ref.: `L2JDreamV2` (`Formulas.calcLethal` e `calcLethalHit`, linhas 1541–1630) / `L2JLucera2` (`Formulas.calcPhysDam` linhas 64–96, 202–225 e `calcMagicDam` linhas 258–278).
121. **Bug Crítico em `calcElementalModifier`: Soma Todos os 6 Elementos em Qualquer Ataque ou Skill**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 426–451), `calcElementalModifier` soma os poderes de ataque de **todos os 6 elementos simultaneamente** (`firePower + waterPower + windPower + earthPower + holyPower + darkPower`) e multiplica as vulnerabilidades dos **6 elementos** (`fireVuln * waterVuln * windVuln * earthVuln * holyVuln * darkVuln`) em todo ataque físico e toda skill mágica/física, sem verificar o `element` da skill! Assim, jogar *Surrender to Fire* num alvo aumenta o dano de *Hydro Blast* (água), *Hurricane* (vento) e até de flechadas físicas. Ref.: `L2JDreamV2` (`Formulas.calcSkillVulnerability`, linhas 2781–2801) / `L2JLucera2` (`Formulas.calcDamageResists`, linhas 743–771).
122. **Fórmula de Land Rate de Debuffs (`debuffLands` / `debuffLandsPlayer`) Ignora `MEN`/`CON`, `mAtk`/`mDef` e Spiritshots**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 731–753 e 1221–1247), a chance de entrada de debuffs ignora completamente: (a) o atributo de resistência base do alvo (`saveVs`: `CON` para Stun/Bleed/Poison, `MEN` para Sleep/Root/Silence/Paralyze/Fear/Debuff/Cancel, via `2.0 - Math.sqrt(saveVs.calcBonus(target))`), (b) a relação `(mAtk / mDef)^0.1` do conjurador vs alvo em debuffs mágicos, e (c) o bônus de Spiritshot/Blessed Spiritshot na taxa de acerto do debuff. Ref.: `L2JDreamV2` (`Formulas.calcSkillStatModifier` e `calcSkillSuccess`, linhas 2565–2764) / `L2JLucera2` (`Formulas.calcSkillSuccess`, linhas 510–704).
123. **Proficiências de Status do Atacante (`*Prof`) Ignoradas no Cálculo de Debuffs**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 457–507), `calcDebuffVulnerability` consulta apenas a vulnerabilidade do alvo (`stunVuln`, `sleepVuln`, `rootVuln`, `bleedVuln`, `poisonVuln`, `paralyzeVuln`, `derangementVuln`), ignorando completamente os stats de proficiência do conjurador (`stunProf`, `sleepProf`, `rootProf`, `bleedProf`, `poisonProf`, `paralyzeProf`, `derangementProf`, `debuffProf`, `confusionProf`) concedidos por Joias Boss (Queen Ant, Orfen, Core, Baium, Antharas, Valakas, Zaken, Frintezza) e passivas. Ref.: `L2JDreamV2` (`Formulas.calcSkillProficiency`, linhas 2457–2515) / `L2JLucera2` (`SkillTrait.calcProf`, `Formulas.java` linhas 605–610).
124. **Bloqueio de Escudo (`calcShieldDefense`) em 360 Graus sem Checagem Direcional nem `shldAngle` (`Aegis`)**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, linhas 369–413), o escudo bloqueia ataques vindos das costas e dos lados de qualquer jogador, sem checar se o defensor está de frente para o atacante (`isFacing(attacker, shldAngle)` com ângulo padrão de 60°/120° frontal) nem consultar o stat `shldAngle` (modificado pelas skills *Aegis* 316 e *Aegis Stance* 318 para permitir defesa em 360°). Além disso, não multiplica `shldRate` por `BaseStats.DEX.calcBonus(target)` nem verifica se o item no slot `LHAND` é realmente um escudo (`shieldDef > 0`). Ref.: `L2JDreamV2` (`Formulas.calcShldUse`, linhas 2366–2417) / `L2JLucera2` (`Formulas.calcShldUse`, linhas 498–508).
125. **Evasão de Skills Físicas (`pSkillEvas` - Dodge / Mirage / Talisman) Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`), skills físicas de curto alcance (`PDAM`, `BLOW`, `CHARGEDAM`, `FATALCOUNTER`) nunca verificam o stat `pSkillEvas` (`Stats.P_SKILL_EVASION`) do alvo. Skills como *Dodge* (446) de Dagger/ Archer são completamente inúteis porque o `CombatService` nunca rola a chance de esquivar da skill física e enviar `SystemMessage(42)`/`SystemMessage(43)`. Ref.: `L2JDreamV2` (`Formulas.calcPhysicalSkillEvasion`, linhas 2353–2359) / `L2JLucera2` (`Formulas.calcPhysDam`, linhas 189–193).
126. **Reflexão de Skills Físicas e Mágicas (`reflectSkillPhysic`, `reflectSkillMagic`, `vengeancePdam`, `vengeanceMdam`) Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java` e `MagicSkillPacketHandler.java`), não existe cálculo de `calcSkillReflect`. Skills como *Physical Mirror* (350), *Magical Mirror* (351), *Riposte Stance* (340), *Vengeance* (368), *Counterattack* (447) e *Shield Deflect Magic* (91) nunca refletem debuffs nem devolvem o dano de skills ao conjurador. Ref.: `L2JDreamV2` (`Formulas.calcSkillReflect`, linhas 2518–2563) / `L2JLucera2` (`Skill.java` / `Formulas.java`).
127. **Reflexão de Dano Melee (`reflectDam`) Ausente contra Monstros (PvE) e de Monstros contra Jogadores**: Em `L2JLopez` (`com.lopez.l2j.network.game.GameSession.java`, linhas 5775–5805 vs 6178–6210), `reflectDam` (de *Chant of Revenge* 1284, *Song of Vengeance* 266, *Reflect Damage* 86, *Frintezza's Necklace*) só é calculado no método `onAttackPlayer` (PvP), sendo completamente omitido quando um monstro ataca o jogador em `NpcAiService` ou quando o jogador ataca um monstro com buff de reflect em `onAttackNpc`. Ref.: `L2JDreamV2` (`L2Character.onHitTimer`) / `L2JLucera2` (`Creature.onHitTimer`).
128. **Fórmula de `MANADAM` (Mana Burn / Mana Storm) Incorreta**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linhas 1353–1386), `MANADAM` usa `ctx.combat().magicSkillDamage(...)` (que calcula `91 * sqrt(mAtk) * power / mDef`) diretamente sobre o MP sem considerar o `maxMp` do alvo (`sqrt(mAtk) * power * (targetMaxMp / 97) / mDef` no Dream ou `magicDam / 4` no Lucera), drenando 100% do MP de qualquer alvo em um único cast! Ref.: `L2JDreamV2` (`Formulas.calcManaDam`, linhas 1887–1904) / `L2JLucera2` (`Formulas.calcMagicDam`, linhas 313–315).
129. **Fórmula de `CPDAM` Quebrada em `MagicSkillPacketHandler.java`**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linha 1395), o dano de `CPDAM` é calculado como `int cpDam = (int) Math.round(targetActive.currentCp() * Math.max(0.0, 1.0 - sk.power()));`. Quando `sk.power() >= 1.0` (ou `0.8`), a fórmula inverte o percentual ou resulta em `0` de dano! Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.CpDam.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.CPDam.java`).
130. **Fórmula de `FATALCOUNTER` (Fatal Counter / Heart Shot) Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java` e `MagicSkillPacketHandler.java`), o `skillType="FATALCOUNTER"` não possui o multiplicador de poder baseado no HP perdido do atacante (`power *= 3.5 * (1.0 - attacker.currentHp() / attacker.maxHp())`), ao contrário de `DEATHLINK` que foi implementado para magia. Ref.: `L2JDreamV2` (`Formulas.calcPhysDam`, linhas 2074–2077).
131. **Bônus de Dano baseados na Quantidade de Debuffs do Alvo (`isBasedOnTargetDebuff`) Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`), skills como *Dark Vortex* / *Day of Doom* / *Curse Chaos* que escalam dano pelo número de efeitos ativos no alvo (`1.0 + 0.05 * targetEffectsCount`) não possuem suporte na fórmula de dano mágico. Ref.: `L2JLucera2` (`Formulas.calcMagicDam`, linhas 309–311).
132. **Quebra de Stun ao Receber Dano Crítico/Normal (`calcStunBreak`) Ausente**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`, `NpcInstance.onDamaged`, `PlayerCharacter.java`), apenas o `Sleep` é interrompido ao sofrer dano (`onDamaged()`), enquanto o `Stun` dura 100% do tempo fixo mesmo recebendo múltiplos golpes críticos. No Interlude retail, cada hit recebido por um alvo em Stun tem chance de quebrar o Stun (75% em crítico, 10% em hit normal). Ref.: `L2JLucera2` (`Formulas.calcStunBreak`, linhas 229–231 e 352–354) / `L2JDreamV2` (`L2Character.onHitTimer`).
133. **Interrupção de Cast Mágico por Dano (`calcAtkBreak` / `calcCastBreak`) Incompleta**: Em `L2JLopez` (`com.lopez.l2j.game.combat.Formulas.java`, linhas 267–274), `calcCastBreak` existe mas **nunca é chamado** em `CombatService`, `GameSession` ou `MagicSkillPacketHandler` quando um jogador ou NPC em estado de conjuração (`casting`) recebe dano físico ou mágico, não levando em conta `MEN`, dano causado (`sqrt(13 * dmg)`) nem o stat `cancel` (`Stats.ATTACK_CANCEL` de *Concentration* 1078 e *Chant oflli*). Ref.: `L2JDreamV2` (`Formulas.calcAtkBreak`, linhas 906–943) / `L2JLucera2` (`Formulas.calcCastBreak`, linhas 430–439).
134. **Skill Mastery (`skillMastery` - Skills 330 e 331) Nunca Calculado**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `PlayerEffects.java`), as passivas *Skill Mastery* (330 para Fighters escalando com `STR`, 331 para Mages escalando com `INT`) nunca são checadas para zerar o `reuseDelay` de uma skill ativa ou dobrar a duração de buffs/heals. Ref.: `L2JDreamV2` (`Formulas.calcSkillMastery`, linhas 2435–2455) / `L2JLucera2` (`Formulas.calcSkillMastery`, linhas 465–468 e 715–726).
135. **Fórmula de Cancel (`CANCEL` / *Touch of Death* / *Bane*) Não Usa `cancelVuln`, `cancelProf`, `magicLevel` nem Duração Restante**: Em `L2JLopez` (`com.lopez.l2j.game.effect.PlayerEffects.java`, linhas 237–266, e `MagicSkillPacketHandler.java`, linhas 1429–1466), `Cancel` (1056) remove sempre um número fixo de buffs sem testar a chance individual de cancelamento de cada buff (do mais recente para o mais antigo) com base em `magicLevel`, `cancelVuln` (*Arcane Protection* 1354), `cancelProf` e tempo restante do buff; além disso, na linha 1438 de `MagicSkillPacketHandler.java`, quando `cancelRestore == null`, o código embaralha `targetActive.effects().all()` e pode remover **debuffs e toggles** junto com buffs! Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.Cancel.java` / `EffectCancel.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.Disablers.java`).
136. **Bane Skills (*Warrior Bane* 1350, *Mage Bane* 1351, *Mass Warrior Bane* 1344, *Mass Mage Bane* 1345) Não Removem `stackType` Específicos**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `SkillTable.java`), os atributos XML `negateStats` / `negateAbnormals` (`attack_time_down`, `speed_up`, `casting_time_down`, `ma_up`) não são parseados nem executados, fazendo com que *Warrior Bane* e *Mage Bane* não removam Haste/Wind Walk ou Acumen/Empower do alvo. Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.NegateStats.java` / `L2Skill.getNegateStats()`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.NegateStats.java`).
137. **Fórmula de Resurrect (`RESURRECT`) Não Escala Percentual de EXP por `WIT` do Conjurador**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linhas 1938–1981), a ressurreição não calcula `calculateSkillResurrectRestorePercent(baseRestorePercent, caster)` multiplicado por `BaseStats.WIT.calcBonus(caster)` (com cap de +20% adicional e teto de 90% para skills normais). Ref.: `L2JDreamV2` (`Formulas.calculateSkillResurrectRestorePercent`, linhas 2854–2869).
138. **Vulnerabilidades Raciais de NPC (`PAtkMonsters`, `PAtkUndead`, `PAtkAnimals`, `PAtkPlants`, `PAtkDragons`, `PAtkInsects`, `PAtkGiants`, `PDef*`) Ausentes**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`), o dano físico e mágico contra monstros nunca aplica os multiplicadores raciais concedidos por buffs/passivas (*Holy Blade*, *Detect Monster Weakness*, *Detect Animal Weakness*, *Detect Plant Weakness*, *Detect Dragon Weakness*, *Detect Insect Weakness*, *Valakas Attribute*). Ref.: `L2JDreamV2` (`Formulas.calcPhysDam`, linhas 2190–2261 e `calcValakasAttribute`, linhas 2903–2923).

---

## Bloco 3: Mecânicas de Ataque Físico (Polearm, Duals, Bow, Reuse, Consumo MP) (#139 a #146)

139. **Ataque Normal com Polearm (Lança) Atinge Apenas 1 Alvo em vez de Cone Multi-Target (`doAttackHitByPole`)**: Em `L2JLopez` (`com.lopez.l2j.network.game.GameSession.java`, métodos `onAttackNpc` linha 5627 e `onAttackPlayer` linha 6020), ataques normais com armas do tipo `pole` / `polearm` atingem exclusivamente o alvo principal, ignorando o cone frontal de varredura (ângulo de 120° e alcance da lança) e o stat `atkCountMax` (modificado por *Polearm Mastery* 216, *Provoke* 286, *War Frenzy* 422, *Dread Pool* 440, *Thrill Fight* 420), além de não aplicar a redução progressiva de dano nos alvos secundários. Ref.: `L2JDreamV2` (`L2Character.doAttackHitByPole`) / `L2JLucera2` (`Creature.doAttackHitByPole`).
140. **Ataque Normal com Dual Swords / Dual Fists / Fist Não Divide em 2 Hits (`doAttackHitByDual`)**: Em `L2JLopez` (`com.lopez.l2j.network.game.GameSession.java`, linhas 5739–5748 e 6116–6127), armas `dual` e `dualfist` executam apenas **1 único golpe** no pacote `Attack` e 1 agendamento de dano, em vez de dividir o ataque em **2 hits independentes** (cada um com 50% do dano base, rolagem própria de miss/crit/shield, primeiro hit em `timeAtk / 4` e segundo hit em `timeAtk / 2`). Ref.: `L2JDreamV2` (`L2Character.doAttackHitByDual`) / `L2JLucera2` (`Creature.doAttackHitByDual`).
141. **Ataque com Arco (`BOW`) Sem Reuse Delay (`atkReuse`), Sem Barra Vermelha (`SetupGauge`) e Sem Consumo de MP da Arma**: Em `L2JLopez` (`com.lopez.l2j.network.game.GameSession.java`, linhas 5669–5688 e 6062–6081), o disparo de arco não aplica o delay de recarga entre flechas (`1500 * 333 / pAtkSpd`), não envia o pacote `SetupGauge(SetupGauge.RED, reuse)` para exibir a barra vermelha acima do personagem, e nunca consome o MP por disparo configurado no template de arcos especiais (`mp_consume` de S-grade/A-grade/Hero Bow). Ref.: `L2JDreamV2` (`L2Character.doAttackHitByBow`) / `L2JLucera2` (`Creature.doAttackHitByBow`).
142. **Alcance de Ataque com Arco (`pAtkRange`) Não Escala por Passivas/Buffs (`Long Shot` / `Rapid Fire` / `Snipe`) em `GameSession`**: Em `L2JLopez` (`com.lopez.l2j.network.game.GameSession.java`, linhas 5876 e 5966), `getPhysicalAttackRange(active)` precisa consultar dinamicamente o stat `pAtkRange` via `PlayerStats.applyStat(active, "pAtkRange", baseRange)` para refletir *Long Shot* (113, +200/+400 range), *Snipe* (313, +200 range) e penalidade de *Rapid Fire* (413, -50% range). Ref.: `L2JDreamV2` (`Formulas.FuncBowAtkRange` linha 172 e `CharStat.getPhysicalAttackRange()`).
143. **Multiplicador de Crítico com Arco (`bowCritRate`) Ignorado no Dano Físico**: Em `L2JLopez` (`com.lopez.l2j.game.combat.CombatService.java`), o stat `bowCritRate` não é aplicado sobre o dano crítico quando o atacante utiliza `BOW`. Ref.: `L2JDreamV2` (`Formulas.calcPhysDam`, linhas 2158–2161).
144. **Reuse Delay de Skills Físicas, Mágicas e Músicas Não Escala por `pAtkSpd` / `mAtkSpd` nem por `*ReuseRate`**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linhas 465–485), o cooldown de skills usa o `reuseDelay` bruto do XML sem escalar por `333.0 / pAtkSpd` (skills físicas) ou `333.0 / mAtkSpd` (magias e músicas) e sem aplicar os stats `mReuse`, `pReuse` e `atkReuse` (de *Renewal* 349, *Champion* 364, *Wild Magic*, *Necklace of Frintezza*, *Necklace of Valakas*). Ref.: `L2JDreamV2` (`L2Character.callSkill` / `Formulas.calcAtkSpd`) / `L2JLucera2` (`Formulas.calcSkillReuseDelay`, linhas 457–476).
145. **Consumo de MP de Skills Não Aplica `MagicalMpConsumeRate`, `PhysicalMpConsumeRate` e `DanceMpConsumeRate`**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linhas 492–506), o consumo de MP deduz `sk.mpConsume()` bruto sem multiplicar pelos stats `MagicalMpConsumeRate`, `PhysicalMpConsumeRate` ou `DanceMpConsumeRate` (modificados por *Clarity* 1397, *Song of Meditation* 363, *Arcane Wisdom* 337, *Orfen's Earring*). Ref.: `L2JDreamV2` (`L2Character.getMpConsume(skill)`) / `L2JLucera2` (`Creature.getMpConsume(skill)`).
146. **Divisão de `mpInitialConsume` (20% no Início do Cast) e `mpConsume` (80% no Lançamento) Ausente**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java` e `SkillTable.java`), `mpInitialConsume` não é parseado do XML nem cobrado no início da conjuração (`MagicSkillUse`), cobrando 100% do MP de uma só vez mesmo que o cast seja interrompido (`calcCastBreak` / movimento). Ref.: `L2JDreamV2` (`L2Skill.getMpInitialConsume()` e `L2Character.beginCast`) / `L2JLucera2` (`Skill.getMpConsume1()` e `getMpConsume2()`).

---

## Bloco 4: Tipos de Skills (`skillType`) Ausentes ou Incompletos (#147 a #167)

147. **`SkillHandlerRegistry` com Apenas 5 Handlers Registrados e Monólito em `MagicSkillPacketHandler`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.handler.SkillHandlerRegistry.java`), apenas `SUMMON`, `RECALL`/`TELEPORT`, `TAKECASTLE`, `SIEGEFLAG` e `SUMMON_FRIEND` estão modularizados; dezenas de `skillType` do XML (`data/xml/stats/skills/*.xml`) caem no vazio ou em blocos incompletos de `MagicSkillPacketHandler.java`. Ref.: `L2JDreamV2` (`com.dream.game.handler.SkillHandler.java`, 50+ handlers) / `L2JLucera2` (`l2.gameserver.model.Skill.SkillType`).
148. **`skillType="CREATE_ITEM"` (`L2SkillCreateItem` / `SummonItem`) Não Implementado**: Em `L2JLopez` (`SkillTable.java` e `MagicSkillPacketHandler.java`), os atributos `create_item_id`, `create_item_count` e `random_count` nunca são lidos do XML, e o tipo `CREATE_ITEM` não cria itens no inventário. Skills essenciais como *Summon CP Potion* (1324), *Summon Cursed Bones* (1387), *Summon Treasure Key* (419), *Summon Soulshots/Spiritshots* e *Create Item* falham silenciosamente. Ref.: `L2JDreamV2` (`com.dream.game.skills.l2skills.L2SkillCreateItem.java` e `SummonTreasureKey.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.SummonItem.java`).
149. **`skillType="DWARVEN_CRAFT"` e `"COMMON_CRAFT"` Não Tratados no Handler de Skills**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), conjurar *Create Item* (skill 172) ou *Common Craft* (skill 1322) pela janela de skills não abre a janela de receitas (`RecipeBookItemList`). Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.Craft.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.Craft.java`).
150. **`skillType="SOW"` e `"HARVEST"` Classificados como `OFFENSIVE` e Sem Handler no `MagicSkillPacketHandler`**: Em `L2JLopez` (`SkillTemplate.java` linha 31 e `MagicSkillPacketHandler.java`), `SOW` e `HARVEST` estão listados no `Set<String> OFFENSIVE`, mas não possuem lógica em `applyOffensive`, fazendo com que usar sementes do Manor ou o Harvester pela skill não plante nem colha safras do monstro! Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.Sow.java` e `Harvest.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.Sowing.java` e `Harvesting.java`).
151. **`skillType="SIGNET"` e `"SIGNET_CASTTIME"` (Símbolos/Bolhas Nv. 79-80) Não Implementados**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `SkillTable.java`), as skills supremas de nível 79-80 (*Volcano* 1419, *Cyclone* 1420, *Raging Waves* 1421, *Day of Doom* 1422, *Gehenna* 1423, *Anti-Summoning Field* 1424, *Purification Field* 1425, *Miracle* 1426, *Flames of Invincibility* 1427, *Mass Recharge* 1428, *Symbol of Defense/Noise/Resistance/Honor/Energy/The Sniper/The Assassin*) não spawna o `EffectPoint` (`npcId` do símbolo no chão) nem aplicam pulsos periódicos em área. Ref.: `L2JDreamV2` (`L2SkillSignet.java`, `L2SkillSignetCasttime.java`, `EffectSignet*.java`) / `L2JLucera2` (`EffectSymbol.java`).
152. **`skillType="FUSION"` / `"BUFF_CHARGER"` (*Battle Force* 426 e *Spell Force* 427) Não Implementados**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), canalizar *Battle Force* ou *Spell Force* num membro da party não acumula estágios do efeito `EffectFusion` / `Force` no alvo nem habilita o disparo das skills combinadas de nível 77–80. Ref.: `L2JDreamV2` (`L2SkillChargeEffect.java`, `EffectFusion.java`, `FusionSkill.java`) / `L2JLucera2` (`SkillSeed.java`, `BuffCharger.java`, `EffectForce.java`).
153. **`skillType="STRSIEGEASSAULT"` (*Strider Siege Assault* 325) Ausente**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), a skill de ataque de Strider contra portas e muros de castelo durante Siege (`STRSIEGEASSAULT`) não possui implementação para causar dano às `DoorInstance` / paredes. Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.StrSiegeAssault.java`).
154. **`skillType="ERASE"` (*Erase* 1395) Não Implementado**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), a skill *Erase* (1395) não faz o teste de resistência mágica contra o servitor/summon inimigo nem o envia de volta a outra dimensão (`unSummon()`). Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.Disablers.java`, case `ERASE`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.DestroySummon.java`).
155. **`skillType="BETRAY"` (*Betray* 1380) Não Implementado**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), a skill *Betray* (1380) e `<effect name="Betray">` não forçam o servitor inimigo a atacar seu próprio dono (`owner`). Ref.: `L2JDreamV2` (`com.dream.game.skills.effects.EffectBetray.java`) / `L2JLucera2` (`l2.gameserver.skills.effects.EffectBetray.java`).
156. **`skillType="CONFUSION"` e `"CONFUSE_MOB_ONLY"` (*Confuse* / *Switch* / *Trick* / *Requiem*) Não Trocam o Alvo do Monstro**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1572–1588), `CONFUSE_MOB_ONLY` apenas limpa o alvo, enquanto `CONFUSION` / `<effect name="Confusion">` não faz o monstro escolher aleatoriamente outra criatura próxima para atacar. Ref.: `L2JDreamV2` (`EffectConfusion.java`, `EffectConfuseMob.java`) / `L2JLucera2` (`EffectDiscord.java`).
157. **`skillType="SHIFT_TARGET"` (*Lure* / *Silent Move* / *Transfer Hate*) Não Implementado**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), o tipo `SHIFT_TARGET` não transfere a agressão (`hate` / `targetPlayerId`) dos monstros próximos para o aliado selecionado. Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.ShiftTarget.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.ShiftAggression.java`).
158. **`skillType="DRAIN_SOUL"` (*Soul Crystal* 2099) Não Integrado ao Handler de Skills**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), usar um Soul Crystal (que aciona a skill 2099 `DRAIN_SOUL`) não registra a absorção de alma no monstro alvo (`NpcInstance`) para evoluir o cristal ao matar o mob/raid. Ref.: `L2JDreamV2` (`com.dream.game.skills.l2skills.L2SkillDrain.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.DrainSoul.java`).
159. **`skillType="BEAST_FEED"` (*Golden Spice* / *Crystal Spice*) Não Evolui Monstros de Beast Farm**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), alimentar monstros de Alpen Kookaburra/Buffalo/Cougar com Spice não aciona a progressão de estágio (`FeedableBeast` growth x1/x2/x4/tamed). Ref.: `L2JLucera2` (`l2.gameserver.skills.skillclasses.BeastFeed.java`).
160. **`skillType="DELUXE_KEY_UNLOCK"` e Cálculo de Chance de `UNLOCK` por Nível de Baú/Porta**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 2035–2078), `UNLOCK` abre qualquer porta/baú com chance fixa de 80% sem verificar o nível da porta/baú contra o nível da skill *Unlock* (27) conforme `Formulas.calculateUnlockChance`, e não diferencia baús verdadeiros (`L2ChestInstance`) de mímicos nem trata `DELUXE_KEY_UNLOCK` (skills 2065, 2229). Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.Unlock.java` e `Formulas.calculateUnlockChance`, linhas 2871–2901) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.Unlock.java`).
161. **`skillType="EXTRACTABLE"` / `"EXTRACTABLE_FISH"` Não Implementado no Handler de Skills**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), skills de itens extraíveis (bolsas de pesca, caixas de aventura, *Supply Box*, *Hot Springs Bait*) com `skillType="EXTRACTABLE"` não sorteiam a tabela de produtos extraíveis quando acionadas via skill. Ref.: `L2JLucera2` (`l2.gameserver.skills.skillclasses.Extract.java`).
162. **`skillType="COMBATPOINTHEAL"` e `"CPHOT"` Não Respeitam Limite de `HealLimit` / Invulnerabilidade**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1791–1824), curas de CP/HP/MP podem curar alvos petrificados (`Petrification` / *Celestial Shield* 1418) que no Interlude retail bloqueiam o recebimento de cura externa. Ref.: `L2JDreamV2` (`CombatPointHeal.java`, `Heal.java`, `L2Character.isPetrified()`).
163. **`skillType="BALANCE_LIFE"` (*Balance Life* 1335) Não Ignora Alvos Mortos/Invulneráveis nem Respeita Raio de Alcance da Party**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1859–1896), `BALANCE_LIFE` equaliza o percentual de HP de membros da party sem verificar a distância máxima (`900`/`1000` unidades) entre o conjurador e cada membro da party, equalizando jogadores que estão em outro continente! Ref.: `L2JDreamV2` (`com.dream.game.handler.skill.BalanceLife.java`) / `L2JLucera2` (`l2.gameserver.skills.skillclasses.Balance.java`).
164. **`skillType="CHARGE"` / `"CHARGEDAM"` / `"CHARGE_EFFECT"`: *Sonic Rage* (345) e *Raging Force* (346) Não Concedem Carga ao Atacar**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1126–1136 e 1333–1345), skills `PDAM`/`CHARGEDAM` que possuem `<set name="num_charges" val="1"/>` e aumentam cargas ao atingir o inimigo (*Sonic Rage* 345 e *Raging Force* 346, que causam dano físico à distância **e** incrementam +1 carga até o nível 7) não adicionam carga ao Gladiator/Tyrant. Além disso, `SkillTable.java` não faz parse de `num_charges` nem `charge_skill_id`. Ref.: `L2JDreamV2` (`ChargeDmg.java`, `L2SkillChargeEffect.java`) / `L2JLucera2` (`Charge.java`, `ChargeSoul.java`).
165. **`skillType="AGGDAMAGE"`, `"AGGREDUCE"`, `"AGGREMOVE"` e `"AGGREDUCE_CHAR"` sem Tabela Real de Hate (`AggroList`) em `NpcInstance`**: Em `L2JLopez` (`NpcInstance.java` e `MagicSkillPacketHandler.java`, linhas 1551–1588), `NpcInstance` guarda apenas um único `int targetPlayerId` em vez de uma `AggroList` (`Map<Integer, HateInfo>` com `damage` e `hate`). Por isso, *Aggression* (28), *Aura of Hate* (18), *Peace* (1044), *Requiem* (1048), *Charm* (1061) e *Veil* (1299) não acumulam, reduzem ou zeram o `hate` numérico de forma proporcional ao dano causado pelo grupo. Ref.: `L2JDreamV2` (`L2Attackable.addDamageHate`, `AggroInfo.java`) / `L2JLucera2` (`AggroList.java`, `Aggression.java`).
166. **`skillType="FISHING"`, `"PUMPING"` e `"REELING"` Não Validam Equipamento de Vara/Isca nem Zona de Pesca**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), o acionamento das skills de pesca (1312 *Fishing*, 1313 *Pumping*, 1314 *Reeling*) precisa validar `WeaponType == ROD`, isca (`Lure`) equipada no `LHAND`, `FishingZone` + `WaterZone` e consumir *Fishing Shot* para dobrar o poder de *Pumping*/*Reeling*. Ref.: `L2JDreamV2` (`Fishing.java`, `FishingSkill.java`, `L2Fishing.java`) / `L2JLucera2` (`FishingSkill.java`, `ReelingPumping.java`).
167. **`skillType="SIEGEFLAG"` e `"TAKECASTLE"` Sem Checagem Completa de `HeadquartersZone` / `ArtifactCastZone` e Condições de Líder de Clã Atacante**: Em `L2JLopez` (`SiegeFlagSkillHandler.java` e `TakeCastleSkillHandler.java`), a conjuração de *Build Headquarters* (247), *Build Advanced Headquarters* (326) e *Seal of Ruler* (246) deve usar `ConditionSiegeZone` e validar se o clã já possui uma flag viva, distância mínima de outras flags e interrupção caso o líder saia da zona do artefato. Ref.: `L2JDreamV2` (`SiegeFlag.java`, `TakeCastle.java`) / `L2JLucera2` (`TakeCastle.java`, `SummonSiegeFlag.java`).

---

## Bloco 5: Efeitos (`EffectType` / `<effect name="...">`), Toggles, Cubics, Herbs e SAs (#168 a #187)

168. **Descarte Silencioso de Efeitos sem `<add>`/`<mul>` em `MagicSkillPacketHandler.applySkillEffects`**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.MagicSkillPacketHandler.java`, linha 2355), qualquer `<effect name="...">` cuja lista `e.funcs()` seja vazia e cuja skill não seja estritamente `BUFF`/`DEBUFF`/`CONT` é **ignorado** (`if (e.funcs().isEmpty() && !isBuffOrDebuff) continue;`). Isso descarta dezenas de efeitos especiais em skills híbridas! Ref.: `L2JDreamV2` (`EffectTemplate.getEffect(Env)`) / `L2JLucera2` (`EffectTemplate.getEffect(Env)`).
169. **Efeito `PhoenixBless` / `Salvation` (*Soul of the Phoenix* 438 e *Salvation* 1410) Ausente**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `GameSession.handlePlayerDeath`), o efeito `<effect name="PhoenixBless">` não está implementado: quando um jogador morre com *Salvation* (1410) ou *Soul of the Phoenix* (438), ele não mantém seus buffs nem recebe a janela de confirmação de auto-ressurreição imediata com HP/MP/CP restaurados. Ref.: `L2JDreamV2` (`EffectPhoenixBless.java`) / `L2JLucera2` (`EffectSalvation.java`).
170. **Efeito `NoblesseBless` Verificado Apenas por `hasSkill(1323)` Hardcoded**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linha 1762, e `GameSession.java`), a preservação de buffs na morte verifica apenas `effects().hasSkill(1323)` em vez de checar se há qualquer efeito com `name="NoblesseBless"` (como *Scroll of Blessing of Noblesse*, amuletos ou skills de evento), e não preserva os buffs do `SummonInstance` quando o pet/servitor morre com *Blessing of Noblesse*. Ref.: `L2JDreamV2` (`EffectNoblesseBless.java`) / `L2JLucera2` (`EffectBlessNoblesse.java`).
171. **Efeitos `CharmOfCourage` e `CharmOfLuck` Não Implementados**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `GameSession.java`), `<effect name="CharmOfCourage">` (que permite ressuscitar no mesmo local durante uma Castle Siege) e `<effect name="CharmOfLuck">` (que evita penalidade de morte / queda de itens ao morrer para Raid Boss) não são processados. Ref.: `L2JDreamV2` (`EffectCharmOfCourage.java`, `EffectCharmOfLuck.java`) / `L2JLucera2` (`EffectCharmOfCourage.java`).
172. **Efeitos `SilentMove` (*Silent Move* 221) e `ChameleonRest` (*Chameleon Rest* 296) Não Impedem Aggro de Monstros**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, `PlayerCharacter.java` e `NpcAiService.java`), ativar a toggle *Silent Move* (221) ou *Chameleon Rest* (296) apenas drena MP no tick de `PlayerEffects`, mas nunca define `isSilentMoving = true` no `PlayerCharacter` nem impede que monstros agressivos iniciem combate automático no `NpcAiService`! Ref.: `L2JDreamV2` (`EffectSilentMove.java`, `EffectChameleonRest.java`) / `L2JLucera2` (`EffectSilentMove.java`).
173. **Efeito `FakeDeath` (*Fake Death* 60) Não Finge Morte nem Remove Aggro de Monstros**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), a skill *Fake Death* (60) não envia o pacote `ChangeWaitType(WAIT_DEAD)` (`StartFakeDeath`), não faz os monstros pararem de atacar o jogador (`stopFakeDeath`/`startFakeDeath`), e levantar-se não exige envio de `ChangeWaitType(WAIT_STOP_FAKEDEATH)`. Ref.: `L2JDreamV2` (`EffectFakeDeath.java`) / `L2JLucera2` (`EffectFakeDeath.java`).
174. **Efeito `Relax` (*Relax* 226) Não Senta o Personagem nem Cancela ao Levantar**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `PlayerEffects.java`), ativar a toggle *Relax* (226) não faz o personagem sentar automaticamente (`sitDown()`), não para de consumir MP quando o HP está cheio (`currentHp == maxHp`), e não desliga a toggle automaticamente quando o jogador se levanta. Ref.: `L2JDreamV2` (`EffectRelax.java`) / `L2JLucera2` (`EffectRelax.java`).
175. **Efeitos `ImmobileBuff` (*Vengeance* 368 / *Shield Fortress* 444) e `ImmobileUntilAttacked` (*Invocation* 1430) Não Imobilizam o Conjurador**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), usar *Vengeance* (368) aplica o buff de P.Def/M.Def mas permite que o Tanker corra livremente pelo mapa (quando deveria ficar imobilizado por `ImmobileBuff`), e *Invocation* (1430) regenera MP sem imobilizar o conjurador, sem reduzir a P.Def em 90% e sem cancelar o efeito ao sofrer qualquer ataque (`ImmobileUntilAttacked` / `EffectMeditation`). Ref.: `L2JDreamV2` (`EffectImmobileBuff.java`, `EffectImmobileUntilAttacked.java`) / `L2JLucera2` (`EffectImmobilize.java`, `EffectMeditation.java`).
176. **Efeito `Bluff` (*Bluff* 358) Rotaciona o Alvo para o Lado Errado e Não Envia `StartRotation`/`StopRotation`**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1179–1189 e 1591–1596), *Bluff* define `target.heading(active().heading())` e envia apenas `CharInfo`, sem enviar os pacotes retail `StartRotation` e `StopRotation` para virar o alvo de costas para o Dagger, e sem testar individualmente a chance de virar de costas, remover o target (`TargetCancel`) e aplicar o Stun. Ref.: `L2JDreamV2` (`EffectBluff.java`) / `L2JLucera2` (`EffectBluff.java`).
177. **Efeito `Fear` (*Curse Fear* 1169 / *Mass Fear* 1381 / *Horror*) Paralisa o Alvo no Lugar em vez de Fazê-lo Fugir**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, linhas 1152–1160 e 1687–1694), `FEAR` apenas chama `disable(until, false)` (funcionando exatamente como um Stun!), em vez de mover o jogador/NPC em direção oposta ao conjurador a cada tick de medo (`EffectFear`). Ref.: `L2JDreamV2` (`EffectFear.java`) / `L2JLucera2` (`EffectFear.java`).
178. **Efeitos `BlockBuff` e `BlockDebuff` (*Mystic Immunity* 1411 / *Celestial Shield* 1418 / *Arcane Protection*) Ausentes em `PlayerEffects`**: Em `L2JLopez` (`com.lopez.l2j.game.effect.PlayerEffects.java`), o método `put(...)` nunca verifica se o alvo está sob efeito de `BlockBuff` / `BlockDebuff` (`isPreventedFromReceivingBuffs` / `isDebuffImmunity`), permitindo que alvos com *Mystic Immunity* ou *Celestial Shield* recebam debuffs/buffs normalmente. Ref.: `L2JDreamV2` (`EffectBlockBuff.java`, `EffectBlockDebuff.java`, `EffectPreventBuff.java`) / `L2JLucera2` (`EffectBuffImmunity.java`, `EffectDebuffImmunity.java`).
179. **Efeito `TouchOfDeath` (*Touch of Death* 342) Não Cancela até 5 Buffs nem Reduz 90% do CP**: Em `L2JLopez` (`MagicSkillPacketHandler.java`), a skill *Touch of Death* (342, que no XML tem `skillType="DEBUFF"` ou `<effect name="TouchOfDeath">` / `cancelRate`) aplica apenas os redutores de stats do `<effect>`, sem executar o cancelamento de até 5 buffs (com 25% base rate) nem consumir/reduzir 90% do CP atual do alvo. Ref.: `L2JDreamV2` (`EffectTouchofDeath.java`).
180. **Sistema Completo de Cubics (`L2CubicInstance` / `EffectCubic` / *Cubic Mastery* 143) Ausente**: Em `L2JLopez` (`MagicSkillPacketHandler.java`, `PlayerCharacter.java`, `GameServerPacket.java`), todas as skills de invocação de Cubics (10 *Summon Storm Cubic*, 22 *Summon Vampiric Cubic*, 33 *Summon Phantom Cubic*, 67 *Summon Life Cubic*, 278 *Summon Viper Cubic*, 449 *Summon Attractive Cubic*, 1279–1281 *Binding/Aqua/Spark Cubic*, 1328–1330 *Mass Cubics*) não instanciam cubics no jogador, não enviam a lista de cubics no `UserInfo`/`CharInfo`, não respeitam o limite de 1/2/3 cubics da passiva *Cubic Mastery* (143) e não executam a task periódica de ataque/cura/cleanse do cubic (`calcCubicSkillSuccess` / `calcMagicDam(L2CubicInstance...)`). Ref.: `L2JDreamV2` (`L2CubicInstance.java`, `L2SkillSummon.java`) / `L2JLucera2` (`EffectCubic.java`).
181. **Sistema de `ChanceSkillTrigger` / `TriggerInfo` (*Mirage* 445, *Counterattack* 447, SAs de Armas e Augmentations de Chance) Incompleto**: Em `L2JLopez` (`SkillTable.java`, `GameSession.java`, `MagicSkillPacketHandler.java`), `SkillTable` não faz parse de `triggeredId`, `triggeredLevel`, `chanceType` (`ON_HIT`, `ON_CRIT`, `ON_CAST`, `ON_ATTACKED`, `ON_ATTACKED_HIT`, `ON_ATTACKED_CRIT`, `ON_EVAAK`). Como resultado:
    - *Mirage* (445) nunca dispara o efeito de remover o target do atacante quando o Adventurer é atingido;
    - SAs de armas e Augmentations com `chanceType="ON_ATTACKED"` ou `ON_HIT` nunca disparam em `GameSession.java` (que só implementou `triggerWeaponOnCritSkill` e `triggerWeaponOnCastSkill` para a arma base, ignorando `AugmentationService` de chance!). Ref.: `L2JDreamV2` (`EffectChanceSkillTrigger.java`, `ChanceCondition.java`, `L2Character.onHitTimer`) / `L2JLucera2` (`TriggerInfo.java`, `TriggerType.java`).
182. **Herbs de Buff (Itens 8606 a 8614) Não Aplicam Buffs nem Usam Auto-Loot/Shadow Buffs**: Em `L2JLopez` (`com.lopez.l2j.network.game.handler.packet.ItemPacketHandler.java`, linhas 1661–1680), as Herbs `8606` (*Herb of Power*), `8607` (*Herb of Magic*), `8608` (*Herb of Atk. Spd.*), `8609` (*Herb of Casting Spd.*), `8610` (*Herb of Critical Attack*), `8611` (*Herb of Speed*), `8612` (*Herb of Warrior*), `8613` (*Herb of Mystic*) e `8614` (*Herb of Recovery*) caem todas no `default ->` que apenas cura 10% de HP e 10% de MP sem aplicar nenhuma das skills 2279–2285! Além disso, no Interlude retail, Herbs são consumidas instantaneamente ao pegá-las do chão (`GetItem`) e, em `PlayerEffects.java`, buffs de Herb devem suspender temporariamente o buff normal de mesmo `stackType` por 2 minutos e restaurar o buff normal quando a Herb expira. Ref.: `L2JDreamV2` (`ItemSkills.java`, `L2PcInstance.doAutoLoot`, `L2Character.addEffect`) / `L2JLucera2` (`EffectList.java`).
183. **Transfer Pain (`TransferDam` - Skill 1262) Hardcoded e Sem Atualização de Status do Summon em PvP**: Em `L2JLopez` (`com.lopez.l2j.game.model.PlayerCharacter.java`, linhas 749–765), a transferência de dano ao servitor está hardcoded para `skillLevel(1262) * 0.10` em vez de consultar o stat `transDam` (`Stats.TRANSFER_DAMAGE_PERCENT`), que chega a 50% no nível 5 (e mais com enchant de skill), e quando o dano é redirecionado em `PlayerCharacter.currentHp(double)`, nenhum pacote `PetStatusUpdate` ou `StatusUpdate` do Summon é enviado ao dono! Ref.: `L2JDreamV2` (`EffectTransferDam.java`, `L2PcInstance.reduceCurrentHp`) / `L2JLucera2` (`Player.reduceCurrentHp`).
184. **Efeitos `EnemyCharge` / `InstantJump` (*Rush*, *Warp*, *Blink*) Não Movem o Personagem nem Enviam `FlyToLocation`**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `SkillTable.java`), os atributos `flyType` (`CHARGE`, `DUMMY`, `PUSH_HORIZONTAL`, `THROW_UP`) e `flyRadius` não são lidos do XML nem executam o deslocamento instantâneo de coordenadas com envio do pacote `FlyToLocation` e `ValidateLocation`. Ref.: `L2JDreamV2` (`EffectEnemyCharge.java`, `InstantJump.java`) / `L2JLucera2` (`FlyToLocation.java`).
185. **Penalidades de Grade de Arma e Armadura (`Expertise` D/C/B/A/S - Skill 239) Não Aplicadas em `PlayerStats` e `CombatService`**: Em `L2JLopez` (`PlayerStats.java` e `CombatService.java`), equipar uma arma ou armadura de Crystal Grade superior ao nível de `Expertise` do jogador não aplica a penalidade retail de Grade (`GradePenalty`: -16 a -40 de Accuracy, -10% a -40% de Crit Rate/Crit Dmg, -20% de Atk.Spd/Cast.Spd/Run.Spd e 95% de falha mágica). Ref.: `L2JDreamV2` (`L2PcInstance.refreshExpertisePenalty()`) / `L2JLucera2` (`Player.refreshExpertisePenalty()`, `Formulas.calcMagicDam` linha 320).
186. **Buffs de Dança e Música (`isDance()` / `isMusic()`) Não Cobram MP Adicional por Músicas Ativas nem Têm Slot Separado/Ordenação**: Em `L2JLopez` (`MagicSkillPacketHandler.java` e `PlayerEffects.java`), conjurar Dances (Blade Dancer) e Songs (Swordsinger) não aplica o custo adicional de MP proporcional à quantidade de dances/songs já ativas no alvo (`+30 MP` por música ativa), e `SkillTemplate.java` não parseia `isDance="true"` / `isSong="true"` do XML. Ref.: `L2JDreamV2` (`L2Skill.isDance()`, `L2Character.getMpConsume`) / `L2JLucera2` (`Skill.isMusic()`).
187. **Remoção de Skills de Augmentation e Item Passive/Active ao Desequipar Arma Incompleta**: Em `L2JLopez` (`com.lopez.l2j.game.augmentation.AugmentationService.java`, linhas 354–440, e `ItemPacketHandler.java`), ao desequipar uma arma com Augmentation ou SA, buffs ativos lançados pela augmentation da arma ou skills ativas em cooldown precisam ter o reuse preservado por `itemObjectId` (`TimeStamp`) para impedir o exploit de re-equipar várias armas augmentadas sem cooldown, ou remover efeitos self-buff vinculados à arma. Ref.: `L2JDreamV2` (`AugmentationData.java`, `L2ItemInstance.getAugmentation()`) / `L2JLucera2` (`ItemSkillHandler.java`).

---

## Bloco 6: Condições de Skills (`SkillCondition`), XML Parsing, Skill Trees, Enchant e NPC/Summon Combat (#188 a #200)

188. **Bug Grave em `SkillCondition.parse`: Condições XML Não Reconhecidas Retornam `null` e Liberam o Cast da Skill sem Restrição**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillCondition.java`, linhas 19–334 e `SkillTable.java`, linha 193), quando uma tag `<cond>` contém qualquer condição não implementada no `SkillCondition.parse`, o método retorna `null`. Em `SkillTable.parseSkill`, `castCond == null` significa **nenhuma condição necessária** (`sk.castCondition()` fica nulo), permitindo que o jogador use skills restritas sem cumprir o requisito! Por outro lado, em `SkillTable.parseFunc` (linha 482), se o filho da `<cond>` retorna `null`, ele define `cond = (p, target) -> false`, desativando permanentemente o bônus da passiva! Ref.: `L2JDreamV2` (`ConditionParser.java`, `DocumentBase.java`) / `L2JLucera2` (`DocumentBase.java`).
189. **Condição `<game night="true">` (*Shadow Sense* 294) Ausente em `SkillCondition.java`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillCondition.java`), a tag `<game night="true">` não é suportada. Por causa do bug do item #188, `SkillTable.parseFunc` recebe `null` e transforma a passiva *Shadow Sense* (skill 294 dos Dark Elves, que concede +Accuracy à noite) em `(p, target) -> false`, deixando-a **permanentemente desativada** dia e noite! Ref.: `L2JDreamV2` (`ConditionGameTime.java`) / `L2JLucera2` (`ConditionGameTime.java`).
190. **Condições `<player>` Ausentes em `SkillCondition.java`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillCondition.java`, linhas 120–248), faltam as condições XML de `<player>`: `sex` / `gender`, `race`, `class_id_restriction`, `pledgeClass`, `castle`, `clanHall`, `fort`, `weight`, `invSize`, `active_effect_id`, `active_skill_id`, `olympiad`, `chaotic` / `pkCount`, `attack_stance` e `grade`. Ref.: `L2JDreamV2` (`com.dream.game.skills.conditions.ConditionPlayer*.java`) / `L2JLucera2` (`l2.gameserver.stats.conditions.ConditionPlayer*.java`).
191. **Condições `<target>` e `<using>` Ausentes em `SkillCondition.java`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillCondition.java`, linhas 78–118 e 250–266), `<target>` suporta apenas `undead`, faltando `level`, `class_id_restriction`, `active_effect_id`, `active_skill_id`, `mindistance`, `race_id`/`race`, `npcId`, `npcType`, `mob`, `playable`, `party`, `clan`, `door`; e `<using>` suporta apenas `kind`, faltando `slotitem` (`ConditionSlotItemId`), `skill` (`ConditionUsingSkill`) e `weaponChange`. Ref.: `L2JDreamV2` (`ConditionTarget*.java`, `ConditionSlotItemId.java`, `ConditionUsingSkill.java`).
192. **Condições `<cond>` Dentro de `<effect>` Ignoradas em `SkillTable.parseSkill`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillTable.java`, linhas 227–274), o parser de `<effect>` lê apenas filhos `<add>`, `<sub >`, `<mul>`, `<div >`, `<set>`, ignorando tags `<cond>` aninhadas diretamente dentro de `<effect>` (usadas para aplicar efeitos secundários condicionais). Ref.: `L2JDreamV2` (`DocumentBase.parseEffect`) / `L2JLucera2` (`DocumentBase.parseEffect`).
193. **Atributos XML de Efeitos Secundários (`effectPower`, `effectType`, `effectLvl`, `saveVs`, `lvlDepend`, `ssBoost`) Ignorados em `SkillTable.java`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillTable.java` e `SkillTemplate.java`), skills híbridas como *Shield Slam* (353), *Stun Shot* (101), *Bluff* (358), *Hammer Crush* (260) e *Anchor* (1170) definem `effectType="STUN"` / `effectPower="40"` ou `saveVs="CON"` no XML, mas `SkillTable` nunca lê esses atributos, usando `sk.power()` (o dano físico da skill!) ou valores fixos para calcular a chance do efeito secundário! Ref.: `L2JDreamV2` (`L2Skill.java` linhas 350–420 e `Formulas.calcSkillSuccess` linhas 2635–2639).
194. **Rotas de Enchant de Skills (`enchant3`, `enchant4`, etc.) e Herança de `<cond>` em `SkillTable.parseSkill`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillTable.java`, linhas 157–184 e 282–355), o parser suporta apenas `enchant1` e `enchant2`, ignorando `enchant3+` se presentes nos XMLs; além disso, na linha 319, a restrição de armas (`weaponsAllowed`) só é anexada à skill enchantada se `rCastCond != castCond`, mas quando o `<enchant1>` sobrescreve o `<for>` com uma nova `<cond>`, o `weaponsAllowed` do template base não é recombinado se `rCastCond` substituir `castCond`. Ref.: `L2JDreamV2` (`DocumentSkill.java`) / `L2JLucera2` (`SkillsEngine.java`).
195. **Enchant de Skills (`SkillEnchantService.java`) Não Verifica nem Consome `Secret Book of Giants` (Item 6622) no Nível +1 (101 / 141)**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillEnchantService.java` e `AcquireSkillPacketHandler.java`), embora `EnchantSkillNode` possua o campo `itemId` (6622 - *Secret Book of Giants*, exigido no primeiro encantamento +1 de cada rota: nível 101 ou 141), `SkillEnchantService.enchant(...)` consome apenas `exp` e `sp` (linhas 161–162) e **nunca verifica nem consome o `Secret Book of Giants` do inventário do jogador**! Ref.: `L2JDreamV2` (`RequestExEnchantSkill.java`) / `L2JLucera2` (`RequestExEnchantSkill.java`).
196. **Tabela de Chance de Enchant de Skills por Nível do Jogador (76 a 80) Não Lida do `enchant_skill_trees.xml`**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillEnchantService.java`, linhas 33–36 e 199–205), `parseInt(el, "rate76", 80)` lê apenas um único `rate76` e estima as chances de nível 77–80 somando `+2% por nível acima de 76`, em vez de armazenar e usar os atributos reais `rate76`, `rate77`, `rate78`, `rate79` e `rate80` definidos em cada `<skill>` do `enchant_skill_trees.xml` (onde, por exemplo, no +10 o `rate76` é `1%`, `rate77` é `10%`, `rate78` é `40%`, `rate79` é `70%`!). Ref.: `L2JDreamV2` (`L2EnchantSkillLearn.getEnchantRate(L2PcInstance)`) / `L2JLucera2` (`EnchantSkillLearn.java`).
197. **Auto-Learn de Skills (`SkillTreeTable.java`) Não Concede Skills de Pesca/Common Craft nem Remove Skills Obsoletas Substituídas**: Em `L2JLopez` (`com.lopez.l2j.game.skill.SkillTreeTable.java`), `getAvailableSkills` filtra apenas a árvore da `classId` atual (e classes pai), mas quando `AUTO_LEARN_SKILLS` está ativo ou ao subir de nível, precisa respeitar skills que exigem itens/livros especiais (Forgotten Scrolls de nível 79-80 / Divine Inspiration 1405) caso `AUTO_LEARN_DIVINE_INSPIRATION` esteja desativado. Ref.: `L2JDreamV2` (`SkillTreeTable.java`) / `L2JLucera2` (`SkillAcquireHolder.java`).
198. **Bug Crítico de Acumulação Infinita de Buffs e Debuffs em `NpcInstance.java` e `MagicSkillPacketHandler.applyNpcDebuffMultipliers`**: Em `L2JLopez` (`com.lopez.l2j.game.npc.NpcInstance.java`, linhas 410–506, e `MagicSkillPacketHandler.java`, linhas 2224–2271), `NpcInstance` aplica buffs e debuffs multiplicando diretamente os campos mutáveis `pAtkMul`, `pDefMul`, `mAtkMul`, `mDefMul`, `runSpdMul`, `pAtkSpdMul`, `mAtkSpdMul`:
    - Quando o buff do monstro expira (`hasActiveBuff`), o multiplicador **nunca é revertido**, fazendo o monstro dobrar seus atributos a cada re-buff;
    - Quando jogadores lançam debuffs (*Hex*, *Power Break*, *Curse Gloom*, *Wind Shackle*, *Slow*) em `applyNpcDebuffMultipliers`, o código multiplica `npc.pDefMul(npc.pDefMul() * f.value())` **sem controle de `stackType` e sem agendar reversão na expiração**, permitindo reduzir a defesa de qualquer Raid Boss a quase zero permanentemente! Ref.: `L2JDreamV2` (`L2Character.addEffect` / `Calculator`) / `L2JLucera2` (`EffectList.java`).
199. **`SummonInstance.java` Usa Atributos Fixos de `NpcTemplate` em vez de `PetDataTable` / `SummonStatsTable` e Ignora Buffs de `Accuracy`, `Evasion`, `Critical` e `cAtk`**: Em `L2JLopez` (`com.lopez.l2j.game.summon.SummonInstance.java`, linhas 114–139, 383–393 e 661–691), `accuracy()` retorna fixo `30 + level`, `evasion()` retorna fixo `30 + level` e `critical()` retorna fixo `44`, e o método `recalculateStats()` (linhas 673–684) calcula apenas 9 stats (`pAtk`, `pDef`, `mAtk`, `mDef`, `pAtkSpd`, `mAtkSpd`, `runSpd`, `walkSpd`, `maxHp`, `maxMp`, `absorbDam`), ignorando completamente buffs de *Guidance* (`accCombat`), *Agility* (`rEvas`), *Focus* (`rCrit`), *Death Whisper* (`cAtk`), *Wild Magic* (`mCritRate`) e *Bless Shield* (`rShld`) aplicados no Pet/Servitor! Ref.: `L2JDreamV2` (`L2Summon.java`, `SummonStat.java`, `PetDataTable.java`) / `L2JLucera2` (`Summon.java`, `PetDataHolder.java`).
200. **`SummonInstance.java` Não Possui Limite Máximo de Buffs (`BUFFS_MAX_AMOUNT`), Não Processa Ticks de HoT/DoT e Não Envia `PartySpelled` / `PetInfo` ao Expirar Buffs**: Em `L2JLopez` (`com.lopez.l2j.game.summon.SummonInstance.java`, linhas 608–643), `applySkillBuff` permite acumular um número ilimitado de `stackType` diferentes no Summon sem respeitar `Config.BUFFS_MAX_AMOUNT` (20/24 buffs), ignora efeitos de regeneração/dano periódico (`HealOverTime`, `ManaHealOverTime`, `DamOverTime`, `Poison`, `Bleed`) e não possui tarefa agendada para atualizar a UI do dono (`PetInfo` / `PetStatusUpdate` / `ExPartyPetWindowUpdate`) quando um buff do Summon expira. Ref.: `L2JDreamV2` (`L2Summon.updateEffectIcons()`, `CharEffectList.java`) / `L2JLucera2` (`Summon.sendChanges()`, `EffectList.java`).

---

# Relatório de Auditoria Cirúrgica — Subagente 3 (#201 a #285)
**Escopo**: Itens, Inventário, ItemHandlers, Pets, Summons, Siege Summons, Cubics, Lojas Privadas, Receitas/Craft, Warehouse/Freight, Cursed Weapons, Augmentation, ArmorSets e Manor.
**Comparativo**: `L2JLopez` vs `L2JDreamV2` (`com.dream.game.*`) e `L2JLucera2` (`l2.gameserver.*` / `handler.items.*`).

---

## Parte 1: Itens, Inventário e ItemHandlers (#201 a #230)

- **#201. Ausência de `IItemHandler` / Registry modular de ItemHandlers**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` (`network/game/handler/packet/ItemPacketHandler.java`, linhas 134–252) implementa uma cadeia monolítica de `if/else` por IDs fixos, e `JdbcItemTemplateTable.etc` (`game/item/JdbcItemTemplateTable.java`, linhas 237–256) não carrega a coluna `item_type` / `handler` da tabela `etcitem`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.ItemHandler` / `l2.gameserver.handler.items.ItemHandler` despacham o uso de itens via interface `IItemHandler` associada ao template do item.

- **#202. Soul Crystals (Uso de Item e Skill `2096` ausentes)**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não possui nenhum tratamento para Soul Crystals (`4629–4664`, `5577–5582`, `5908`, `5911`, `5914`, `9570–9572`); clicar duas vezes no cristal retorna `S1_CANNOT_BE_USED` em vez de conjurar a skill `2096` no monstro alvo e registrar o jogador como absorvedor no `NpcInstance`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SoulCrystals` e `handler.items.SoulCrystals`.

- **#203. Absorção, Evolução, Quebra e Ressonância de Soul Crystals na morte de Mobs/RaidBosses**:
  - **L2JLopez**: `PlayerCombatHandler.handleMonsterDeath` (`network/game/handler/packet/PlayerCombatHandler.java`, linhas 43–241) não possui nenhuma lógica de cálculo de absorção de Soul Crystal (`level_up_party`, `level_up`, `stage` mínimo/máximo por NPC, chance de quebra/ressonância).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2Attackable.absorbSoulFromNpc` e `com.dream.game.datatables.xml.SoulCrystalsTable`.

- **#204. Sementes de Manor (`Seed`) sem Handler de Plantio (`Sow` - Skill `2097`)**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não trata itens do tipo semente registrados em `CastleManorManager` (`5016+`). O uso da semente não valida a região/castelo do monstro (`MapRegionTable`), nível do monstro, nem conjura a skill `2097` (`Sow`) para marcar o `NpcInstance` como `seeded`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Seed`, `com.dream.game.handler.skill.Sow` e `handler.items.Seed`.

- **#205. Colheitadeira (`Harvester` - Item `5125`) e Skill `Harvest` (`2098`) inexistentes**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não trata o item `5125` (`Harvester`) para conjurar a skill `2098` (`Harvest`) em monstros mortos que foram semeados (`seeded`), e `SkillHandlerRegistry.java` não possui handler para `SOW` nem `HARVEST`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Harvester`, `com.dream.game.handler.skill.Harvest` e `handler.items.Harvester`.

- **#206. Supressão de Drops Normais em Monstros Semeados (`Seeded`)**:
  - **L2JLopez**: `PlayerCombatHandler.handleMonsterDeath` (linha 94) chama `ctx.drops().rewardMonsterDeath(...)` incondicionalmente, sem verificar se o monstro estava semeado por Manor (em L2 oficial, monstros `seeded` só podem dropar Adena/Seal Stones no abate normal, retendo o restante para o `Harvest`).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2Attackable.doItemDrop`.

- **#207. Mercenary Posting Tickets (`3960–3999`) Incompletos e sem Validação de Cerco/Seven Signs**:
  - **L2JLopez**: `ItemPacketHandler.handleDropItem` (linha 318) só intercepta os IDs `3960–3975` (ignorando `3976–3999`), não verifica se o jogador é Líder do Clã dono do castelo com privilégio `CP_CS_MERCENARIES`, não bloqueia durante o Cerco ativo, não valida o `Seal of Strife` (Dawn/Dusk) do `SevenSignsManager` e não persiste tickets posicionados no banco. Além disso, `handleUseItem` não orienta o jogador ao dar duplo clique no ticket.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.MercTicket` e `com.dream.game.manager.MercTicketManager`.

- **#208. Livros de Ajuda e Diários de Quest (`Book` / `HelpBook`) não abrem HTML**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não possui handler para livros de leitura (`5588` Tutorial Guide, `7063–7066` Lidia's Diary/Contract, `7082–7112` Guild/Quest Books), retornando `S1_CANNOT_BE_USED` em vez de carregar `data/html/help/<itemId>.htm` e enviar `NpcHtmlMessage` + `ShowMiniMap`/`RadarControl`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Book` e `handler.items.HelpBook`.

- **#209. `SpecialXMas` (Item `5555` - Token of Love / XMas Seal) não tratado no Uso de Item**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não trata o item `5555` para enviar `ShowXMasSeal(5555)` (no projeto atual, `ShowXMasSeal(5555)` só é enviado num método de diagnóstico em `ExtendedProtocolPacketHandler.sendProtocolExtendedDiagnostics`, linha 1152).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SpecialXMas`.

- **#210. Sementes de Abóbora (`SquashSeed` - Itens `6389`, `6390`) e Néctar (`Pollen` - Item `6391`) ausentes**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não trata `6389` (Small Squash Seed) nem `6390` (Large Squash Seed) para invocar os NPCs `12774`/`12777` via skill `2003`/`2004`, nem trata o Néctar `6391` para conjurar a skill `2005` nos NPCs de Squash e evoluí-los em King/Emperor Squash.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SquashSeed` e `com.dream.game.handler.item.Pollen`.

- **#211. Marcas de Pagan Temple (`AdvQuestItems` - Itens `8064`, `8065`, `8067`)**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` não possui suporte aos itens `8064` (Visitor's Mark), `8065` (Faded Visitor's Mark) e `8067` (Pagan's Mark) para acionamento de portas/passagens de Pagan Temple e conversão `8064 -> 8065`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.AdvQuestItems`.

- **#212. Árvores de Natal (`5560` Christmas Tree e `5561` Special Christmas Tree) não Spawna NPC no Mundo**:
  - **L2JLopez**: Em `SummonItemService.useSummonItem` (`game/summon/SummonItemService.java`, linhas 125–133, `case 0`), usar o item `5560` ou `5561` apenas destrói o item do inventário e envia a mensagem `"Objeto invocado com sucesso!"`, sem instanciar o `NpcInstance` (`13006` ou `13007`) no `GameWorld`, sem agendar despawn e sem aplicar a aura periódica de regeneração de HP/MP da `Special Christmas Tree` (`13007`).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SummonItems` e `com.dream.game.model.actor.instance.L2SpecialTreeInstance`.

- **#213. `FishShots` (`6535–6540`) sem Validação de Vara/Grade e sem Efeito Real na Pesca**:
  - **L2JLopez**: Em `ItemPacketHandler.handleSpecialMiscellaneousItem` (linhas 1597–1604), usar qualquer `FishShot` (`6535–6540`) destrói 1 item e transmite `MagicSkillUse(2181..2186)` incondicionalmente, sem checar se o jogador está pescando, sem checar se há uma `FishingRod` equipada da mesma `crystalType` (Grade) e sem marcar `fishshotCharged` na `FishingSession` em `FishingService.java` para dobrar o poder de `handlePumping`/`handleReeling`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.FishShots` e `handler.items.FishShots`.

- **#214. Coleta de Herbs (`8600–8614`) vai para o Inventário em vez de Auto-Uso no Pickup**:
  - **L2JLopez**: Em `ActionPacketHandler.tryPickupGroundItem` (`network/game/handler/packet/ActionPacketHandler.java`, linhas 177–233), coletar uma Herb do chão adiciona o item ao `Inventory` e ao banco de dados como um item comum, em vez de executar seu efeito imediatamente ao pegar do chão (e redirecionar para o Summon quando aplicável).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PcInstance.doPickupItem` e `handler.items.Harvester`/`Potions`.

- **#215. Herbs de Buff (`8606–8614`) aplicam apenas Cura de 10% HP/MP em vez dos Buffs de 2 Minutos**:
  - **L2JLopez**: Em `ItemPacketHandler.handleSpecialMiscellaneousItem` (linhas 1662–1680), apenas `8600–8605` possuem tratamento específico; todas as Herbs `8606–8614` (Herb of Power `8606`, Magic `8607`, Atk. Spd. `8608`, Casting Spd. `8609`, Critical `8610`, Speed `8611`, Mystic `8612`, Warrior `8613`, Doubt `8614`) caem no `default ->` que apenas restaura 10% de HP/MP em vez de aplicar as skills `2278–2285` / Big Head.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Potions` (cases `8606` a `8614`).

- **#216. `Scrolls` de Buff, SP e Waking ausentes em `ConsumableTable` e `ItemPacketHandler`**:
  - **L2JLopez**: `ConsumableTable.java` (linhas 160–248) e `ItemPacketHandler.java` não contemplam scrolls clássicos como `3958` (L2Day Blessed Scroll of Escape), `5593–5595` (SP Scrolls que concedem 500 / 5.000 / 100.000 SP via skills `2167`), `6037` (Scroll of Waking) e `9146–9155` (Scrolls of Courage/Charm/etc.).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Scrolls` e `handler.items.Scrolls`.

- **#217. Poções Elementais (`9997–10002`) e Fisherman's Potions (`8193–8201`) ausentes**:
  - **L2JLopez**: `ConsumableTable.java` e `ItemPacketHandler.java` não tratam as poções de resistência elemental (`9997–10002`, skills `2335–2340`), nem as `Fisherman's Potions` (`8193–8201`, skill `2274` níveis 1 a 9 que reduzem temporariamente a perícia de pesca para peixes de nível específico) e `Green Colored Fish Scale` (`8202`).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Potions`.

- **#218. `ScrollOfEscape` sem validação de Destino Específico (Castle, Clan Hall, Town Scrolls, Rift)**:
  - **L2JLopez**: `ItemPacketHandler.handleUseItem` (linha 161) delega a `session.useScrollOfEscape(item)`, que não diferencia os Scrolls de Town específicos (`7117–7135` Talking Island a Rune/Schuttgart, `7554–7559`, `7618` Ketra, `7619` Varka), `1829` (Scroll of Escape: Clan Hall) e `1830` (Scroll of Escape: Castle) com validação de posse de Clan Hall/Castelo e tempo de cast diferenciado (20s vs 200ms nos Blessed SoEs `1538`/`5858`/`5859`).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.ScrollOfEscape` e `handler.items.ScrollOfEscape`.

- **#219. `ScrollOfResurrection` (`737`, `3936`, `3959`, `6387`) sem Tempo de Cast e sem Pet Resurrection**:
  - **L2JLopez**: `ItemPacketHandler.handleScrollOfResurrection` (linhas 1403–1449) aplica a proposta de ressurreição instantaneamente sem conjurar as skills `2014`/`2049`/`2179` com `SetupGauge`, não bloqueia o uso durante Cerco se o clã atacante não possuir Headquarters (`SiegeFlag`) ou se o defensor tiver perdido todos os Life Control Towers, e não permite ressuscitar `SummonInstance` (Pet) morto com `737`/`3936`/`6387` (Pet Blessed Resurrection Scroll).
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.ScrollOfResurrection` e `handler.items.ScrollOfResurrection`.

- **#220. `Enchants` sem Validação de Limite Máximo (`EnchantMaxWeapon`/`Armor`/`Jewelry`) eItens Proibidos**:
  - **L2JLopez**: `ItemPacketHandler.handleEnchantItem` (linhas 546–733) não bloqueia o encantamento de Cursed Weapons (`8190`, `8689`), Hero Weapons (`6611–6621`), Varas de Pesca, Armaduras/Armas de Pets, Shadow Weapons (`mana > 0`), nem verifica limites máximos configuráveis por categoria (`MAX_ENCHANT_LEVEL`).
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestEnchantItem` e `com.dream.game.handler.item.EnchantScrolls`.

- **#221. `PaganKeys` (`8056`, `8273`, `8274`, `8275`) abrem Qualquer Porta sem Checar `doorId` de Pagan Temple**:
  - **L2JLopez**: Em `ItemPacketHandler.handleKeyItem` (linhas 1538–1585), usar `8056` (Key of Darkness), `8273` (Anteroom Key), `8274` (Chapel Key) ou `8275` (Key of Splendor Room) abre qualquer `DoorInstance` selecionada com 100% de chance, em vez de validar os IDs exatos das portas de Pagan Temple (`19160002–19160017`) e chance de abertura.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.PaganKeys` e `handler.items.Keys`.

- **#222. `ExtractableItems` sem Validação de Capacidade de Peso e Slots Livres no Inventário**:
  - **L2JLopez**: Em `ExtractableItemService.extractItem` (`game/extractable/ExtractableItemService.java`, linhas 83–122) e `ItemPacketHandler.handleUseItem` (linhas 177–207), o item cápsula/saco é destruído antes de verificar se o inventário do jogador comporta o peso e os slots dos itens extraídos.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.ExtractableItems` e `handler.items.ExtractableItems`.

- **#223. `EnergyStone` (Item `5589`) carrega Charges para Qualquer Classe e sem Limite do `Sonic Focus`/`Focused Force`**:
  - **L2JLopez**: Em `ItemPacketHandler.handleSpecialMiscellaneousItem` (linhas 1606–1619), usar uma `Energy Stone` (`5589`) adiciona 1 Charge até o nível 2 para qualquer classe do jogo, sem verificar se o personagem é Gladiator/Tyrant/Warlord/Monk e sem conjurar a skill `2165`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.EnergyStone`.

- **#224. `RollingDice` (`4625–4628`) sem Flood Protector / Reuse Delay**:
  - **L2JLopez**: Em `ItemPacketHandler.handleSpecialMiscellaneousItem` (linhas 1621–1635), jogar dados (`4625–4628`) não verifica zona de paz nem aplica `FloodProtector` / cooldown de 4 segundos (`TRY_AGAIN_LATER`), permitindo spam de pacotes `Dice` e `SystemMessage(S1_ROLLED_S2)` em área.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.RollingDice` e `handler.items.RollingDice`.

- **#225. `Firework` (`6403`, `6406`, `6407`) e `CrystalCarol` (`4411–4417`, `5562–5566`, `5583–5587`, `6903`, `7061`, `7062`, `8555`) Incompletos**:
  - **L2JLopez**: Em `ItemPacketHandler.handleSpecialMiscellaneousItem` (linhas 1637–1647), faltam os Cristais de Música `6903`, `7061`, `7062`, `8555`, e não há cooldown/flood protection para evitar spam de `MagicSkillUse` em área.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.Firework` e `com.dream.game.handler.item.CrystalCarol`.

- **#226. `Shadow Weapons` (`duration` na tabela `weapon` e Consumo de Mana Equipada)**:
  - **L2JLopez**: `JdbcItemTemplateTable.weapons` (`game/item/JdbcItemTemplateTable.java`, linhas 90–145) não lê a coluna `duration` do banco de dados; `ItemTemplate` não possui o campo `duration`; `ItemInstance.mana` é inicializado sempre com `-1` e não existe tarefa periódica para decrementar `mana` de Shadow Weapons enquanto equipadas e destruí-las ao chegar a `0`.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2ItemInstance.ScheduleConsumeManaTask`.

- **#227. Métodos Diretos `Inventory.addAdena` e `Inventory.addItem` Geram `objectId` Falso sem Persistência**:
  - **L2JLopez**: Em `Inventory.java` (`game/item/Inventory.java`, linhas 126–157), os métodos `addAdena(int)` e `addItem(int, int)` geram IDs em memória (`0x40000000 + items.size() + 1`) com `ItemTemplate` dummy `"Item#" + itemId` sem persistir no `ItemRepository` (e são chamados em fluxos de fallback de `FishingService` e `TradeStorePacketHandler`).
  - **Ref. Dream/Lucera**: `com.dream.game.model.itemcontainer.PcInventory.addItem`.

- **#228. `SoulShots` e `SpiritShots` sem Validação de `soulshots`/`spiritshots` Consumidos por Ataque da Arma**:
  - **L2JLopez**: `JdbcItemTemplateTable.weapons` (linhas 90–145) não carrega as colunas `soulshots` e `spiritshots` da tabela `weapon` (quantidade de shots consumidos por golpe/cast, que varia de 1 a 10 conforme a arma). `ItemPacketHandler.handleUseSoulShot` e `handleUseSpiritShot` (linhas 926–1059) consomem sempre `1` shot fixo em vez de `weapon.getSoulShotCount()` / `weapon.getSpiritShotCount()`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SoulShots` e `com.dream.game.handler.item.SpiritShot`.

- **#229. `BeastSoulShot` (`6645`, `2033`) e `BeastSpiritShot` (`6646`, `6647`, `2034`, `2035`) não são Consumidos no Combate do Summon**:
  - **L2JLopez**: `ItemPacketHandler.handleBeastShot` (linhas 1451–1506) consome 1 shot no clique e liga a flag `session.beastSoulshotCharged(true)`, mas `PlayerCombatHandler.triggerSummonAssistAttack` (linhas 335–397) e `ActionPacketHandler.orderSummonAttack` (linhas 779–828) nunca verificam `beastSoulshotCharged()`, nunca aplicam o multiplicador 2x no dano físico/mágico do Summon, não descontam `soulShotCount` do Summon e não recarregam o auto-shot do Summon após o ataque.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.BeastSoulShot` e `com.dream.game.handler.item.BeastSpiritShot`.

- **#230. `ChestKeys` (`6665–6672` Deluxe Chest Key e `5197–5204` Key of Thief) sem Distinção entre `L2ChestInstance` e `L2MonsterInstance` (Box)**:
  - **L2JLopez**: `MagicSkillPacketHandler.handleUnlockChest` (linhas 936–1034) usa uma lista estática de IDs `18257–18264` como Mimic em vez de distinguir Treasure Chests de Treasure Boxes e executar a AI oficial de explosão/recompensa de baús.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.ChestKey` e `com.dream.game.model.actor.instance.L2ChestInstance`.

---

## Parte 2: Pets, Summons, Siege Summons, Montarias e Cubics (#231 a #258)

- **#231. Desconexão Total entre `SummonItemService` e `PetService` / `PetDataTable` (Tabela `pets` ignorada)**:
  - **L2JLopez**: Em `SummonItemService.useSummonItem` (`game/summon/SummonItemService.java`, linhas 135–187), usar uma coleira de Pet (`type == 1`) cria um `SummonInstance` calculando `petLevel = item.enchant() > 0 ? item.enchant() : 1` e `maxHp = 100 + petLevel * 20` diretamente na classe `SummonInstance` (linha 128), **nunca** consultando `PetService.getPet(item.objectId())` (tabela `pets`) nem `PetDataTable.getStat(npcId, petLevel)` (`pet_stats.xml`).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetInstance.spawnPet` e `com.dream.game.datatables.xml.PetDataTable`.

- **#232. Invocação Instantânea do Pet em vez de Task de 5 Segundos (`SetupGauge` + `MagicSkillUse 2046`)**:
  - **L2JLopez**: Em `SummonItemService.useSummonItem` (linhas 137–178), o servidor envia `SetupGauge(BLUE, 5000)` na linha 137 e spawna o Pet **no mesmo milissegundo** (linha 167), sem imobilizar o jogador, sem transmitir `MagicSkillUse(2046, 1, 5000, 0)` e sem agendar `PetSummonFinalizer` de 5 segundos cancelável por movimento ou dano.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.SummonItems` (`PetSummonFeedWait` / `PetSummonFinalizer`).

- **#233. Atributos de Combate de Pets e Servitors Calculados com Fórmulas Hardcoded em vez de `PetDataTable` / `NpcTemplate`**:
  - **L2JLopez**: No construtor de `SummonInstance.java` (`game/summon/SummonInstance.java`, linhas 126–146), se `template.maxHp()` for baixo, os status são calculados por fórmulas lineares fixas (`pAtk = 20 + level * 8`, `pDef = 25 + level * 5`, `mAtk = 15 + level * 6`), ignorando completamente os atributos por nível (`petMaxHP`, `petPAtk`, `petPDef`, `petMAtk`, `petMDef`, `petPAtkSpd`, `petMAtkSpd`) já carregados em `PetDataTable.java`!
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetInstance.getStat` e `L2PetData`.

- **#234. Ausência de Task Periódica de Fome (`FeedTask`) e Penalidade/Morte por Inanição do Pet**:
  - **L2JLopez**: Embora `SummonInstance` possua os campos `currentFed` e `maxFed`, não existe nenhuma tarefa agendada (`FeedTask`) para decrementar `currentFed` usando `feedBattle` (em combate) e `feedNormal` (fora de combate). O Pet nunca sente fome, nunca sofre penalidade de 50% de Speed/Atk quando `currentFed == 0`, nunca consome ração automaticamente do próprio inventário quando `currentFed < 55%` e nunca guarda/destrói a coleira por inanição.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetInstance.FeedTask` e `handler.items.PetFood`.

- **#235. Uso de Ração de Pet (`PetFood` - Itens `2515`, `4038`, `5168`, `5169`, `6316`, `7582`) Incompleto**:
  - **L2JLopez**: `ItemPacketHandler.handlePetFood` (linhas 1508–1536) restaura um valor fixo de `500` de `currentFed` para qualquer ração e qualquer Pet, sem validar se a ração corresponde ao `npcId` do Pet (`PetDataTable.getFoodItemId(npcId)` — ex.: Wolf Food `2515` apenas para Wolf, Food for Strider `5168`/`5169` apenas para Strider, Baby Spice `7582` apenas para Baby Pets) e sem conjurar as skills `2048`/`2099`/`2100`/`2101`/`2187`.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.item.PetFood` e `handler.items.PetFood`.

- **#236. Inventário de Pet (`PetItemList`, `RequestGiveItemToPet`, `RequestGetItemFromPet`) Totalmente Stub/Fake**:
  - **L2JLopez**: `SummonInstance` não possui um objeto `Inventory` (nem persistência `loc='PET'` em `ItemRepository`). Em `ExtendedProtocolPacketHandler.java` (linhas 1013–1028), `handleGiveItemToPet` e `handleGetItemFromPet` **não transferem nenhum item** entre o jogador e o Pet: apenas enviam pacotes `PetItemList` e `PetInventoryUpdate` falsos de volta ao cliente!
  - **Ref. Dream/Lucera**: `com.dream.game.model.itemcontainer.PetInventory`, `RequestGiveItemToPet` e `RequestGetItemFromPet`.

- **#237. Equipamento de Armas e Armaduras de Pet (`RequestPetUseItem`) Não Implementado**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handlePetUseItem` (linhas 1002–1011), o método não equipa presas/garras de Lobo, armaduras de Hatchling/Strider ou acessórios de Baby Pet, nem usa ração/poções do inventário do Pet — apenas envia `PetInfo(sum, 1)` sem fazer nada.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestPetUseItem`.

- **#238. Coleta de Item do Chão pelo Pet (`RequestPetGetItem`) é um Stub Vazio**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handlePetGetItem` (linhas 1030–1033), o método valida apenas se existe um `activeSummon()` e retorna sem mover o Pet até o `GroundItem`, sem enviar `GetItem` e sem adicionar o item ao inventário do Pet/Dono.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestPetGetItem` e `L2PetInstance.doPickupItem`.

- **#239. Ganho de EXP/SP do Pet e `expPenalty` em Combate Inexistentes**:
  - **L2JLopez**: `PlayerCombatHandler.handleMonsterDeath` (linhas 86–92) entrega 100% da EXP/SP ao jogador/party sem descontar a fração de `expPenalty` do Pet (`PetStatTemplate.expType()`) ou do Servitor (ex.: 30% em Kat the Cat / Mew / Boxer, 90% em Necromancer summons) e nunca chama `PetService.addPetExp` para evoluir o nível do Pet em combate.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.L2Summon` e `L2PetInstance.addExpAndSp`.

- **#240. Morte do Pet não Agenda Decay de 20 Minutos (Regra Oficial de Perda do Pet)**:
  - **L2JLopez**: Quando um `SummonInstance` do tipo `TYPE_PET` morre em combate (`CombatService` / `SummonInstance.currentHp(0)`), não existe `DecayTask` de 20 minutos (`1.200.000 ms`, ou 24h conforme config) que destrói o Pet/coleira caso não seja ressuscitado a tempo, nem bloqueia `unSummon` enquanto o Pet estiver morto com itens no inventário.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetInstance.doDie`.

- **#241. Renomeação de Pet (`RequestChangePetName`) não Persiste na Tabela `pets` nem Valida Duplicidade**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleChangePetName` (linhas 990–1000), o nome é alterado apenas em memória (`sum.name(name)`), sem validar se o Pet já possui nome definido (`sum.name()`), sem validar regex alfanumérica (3–16 chars), sem checar se o nome já existe na tabela `pets` e sem chamar `PetService` para atualizar o banco de dados e o `enchant`/status da coleira.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestChangePetName` e `PetNameTable`.

- **#242. Bug Crítico em `PetEvolveService.evolvePet(PlayerCharacter, SummonInstance)` (`objectId` passado como `itemId`)**:
  - **L2JLopez**: Em `PetEvolveService.java` (`game/pet/PetEvolveService.java`, linha 105), o método sobrecarregado chama `evolvePet(player, pet.itemObjectId(), pet.level())`, passando o `objectId` da instância da coleira para um parâmetro `baseControlItemId` que faz `rules.get(baseControlItemId)` esperando o `itemId` do template (`2375` Wolf Collar, `3500–3502` Hatchling Flute, `9882` Great Wolf)! Como `objectId != itemId`, a evolução **sempre falha**!
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetManagerInstance.evolve`.

- **#243. `PetEvolveService` Altera Inventário Apenas em Memória sem Persistir no Banco nem na Tabela `pets`**:
  - **L2JLopez**: Em `PetEvolveService.evolvePet` (linhas 86–97), a remoção da coleira antiga (`inv.remove(collarOpt.get().objectId())`) e a criação da coleira evoluída (`inv.addItem(rule.evolvedControlItemId(), 1)`) são feitas diretamente no objeto `Inventory` em memória, sem chamar `InventoryService` (perdendo a alteração no banco `items`), sem atualizar o `item_obj_id`/`npcId` na tabela `pets` e sem enviar `InventoryUpdate`/`ItemList`.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetManagerInstance`.

- **#244. NPCs Pet Manager (`30731`, `30827–30831`, `30869`, `31067`, `31265`, `31309`, `31954`) não Conectados ao `PetEvolveService`**:
  - **L2JLopez**: `NpcBypassPacketHandler.java` não trata os bypasses `evolve` (`evolve 1` a `evolve 5` para Wolf -> Great Wolf, Hatchling -> Strider, Baby Buffalo/Kookaburra/Cougar) nem `restore` nos NPCs Pet Manager.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PetManagerInstance.onBypassFeedback`.

- **#245. Habilidades de Pets e Servitors (`ActionId >= 1000` em `RequestActionUse`) Completamente Ausentes**:
  - **L2JLopez**: `ActionPacketHandler.handleActionUse` (`network/game/handler/packet/ActionPacketHandler.java`, linhas 519–761) trata apenas os Action IDs `15, 16, 17, 19, 21, 22, 23, 52, 53, 54`. Todos os Action IDs de skills de Pets e Servitors (`1000` Siege Hammer, `1001` Ultimate Bombastic Buster, `1003` Wild Stun, `1004` Wild Defense, `1005` Bright Burst, `1006` Bright Heal, `1007–1046` Blessing/Gift of Queen, Cure/Gift of Seraphim, Curse/Mass Curse of Shade, King/Magnus/Spectral Lord skills, Swoop Cannon skills) caem no `default ->` e são ignorados!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestActionUse`.

- **#246. Tabela `pets_skills` (`PetService.getPetSkills`) Nunca Utilizada**:
  - **L2JLopez**: `PetService.getPetSkills(int templateId, int petLevel)` (`game/pet/PetService.java`, linhas 290–308) consulta a tabela `pets_skills`, mas não é chamado em nenhum lugar do código!
  - **Ref. Dream/Lucera**: `com.dream.game.datatables.sql.PetSkillsTable`.

- **#247. Ordem de Ataque do Summon (`orderSummonAttack`) Executa Apenas 1 Golpe Instantâneo com Teleporte**:
  - **L2JLopez**: Em `ActionPacketHandler.orderSummonAttack` (linhas 779–828) e `orderSummonAttackPlayer` (linhas 830–878), ordenar o Pet/Servitor a atacar move instantaneamente as coordenadas do Summon para `npc.x() - 35, npc.y() - 35` e desfere **um único ataque** (`summonAttackNpc`), sem iniciar um loop agendado contínuo de auto-ataque do Summon baseado no `pAtkSpd` dele e sem registrar aggro/hate no `NpcAiService`.
  - **Ref. Dream/Lucera**: `com.dream.game.ai.L2SummonAI`.

- **#248. Follow Task do Summon (`SummonInstance.startFollow`) Não Envia Pacotes de Movimento aos Jogadores ao Redor**:
  - **L2JLopez**: Em `SummonInstance.startFollow` (`game/summon/SummonInstance.java`, linhas 612–646), a task periódica de 500ms altera `this.x = nx; this.y = ny; this.z = nz;` em memória e só notifica o `ownerMoveListener` (que envia apenas para a sessão do dono), sem fazer broadcast de `MoveToPawn` / `MoveToLocation` para os demais jogadores no `GameWorld.VISIBILITY_RADIUS`! Para outros jogadores, o Pet/Servitor fica congelado no ponto de invocação.
  - **Ref. Dream/Lucera**: `com.dream.game.ai.AbstractAI.moveToPawn`.

- **#249. Montaria (`Ride` Packet `0x86`, Action ID `38`, Strider e Wyvern) Não Envia Pacote Visual nem Skills**:
  - **L2JLopez**: `SummonItemService.useSummonItem` (`case 2`, linhas 189–196) e `PetService.mount`/`dismount` (linhas 242–264) apenas alteram `player.mountNpcId(npcId)` e enviam uma mensagem no chat, **sem enviar o pacote `Ride` (`0x86`)** ao jogador e aos jogadores próximos, sem recolher o Pet ativo, sem adicionar a skill `4289` (Wyvern Breath) ao montar Wyvern (`12621`), sem validar restrições (Cursed Weapon, Olympiad, PvP Flag, Siege, transformação/arma incompatível) e sem tratar o botão Mount/Dismount (`actionId == 38` em `ActionPacketHandler.handleActionUse`).
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestActionUse` (case `38`) e `L2PcInstance.mountPlayer`.

- **#250. `SkillHandlerRegistry.resolveServitorNpcId` Ignora o Nível da Skill e Não Lê `npcId` do XML**:
  - **L2JLopez**: Em `SkillHandlerRegistry.resolveServitorNpcId` (`game/skill/handler/SkillHandlerRegistry.java`, linhas 484–505), o `npcId` do Servitor é resolvido por um `switch(skillId)` fixo que ignora `skillLevel`! Assim, conjurar `Summon Mew the Cat` nível 1 ou nível 18 spawna sempre o mesmo `npcId = 14111`, em vez de ler `<set name="npcId" val="..."/>` do `SkillTemplate`.
  - **Ref. Dream/Lucera**: `com.dream.game.skills.l2skills.L2SkillSummon`.

- **#251. Servitors de 3ª Classe e Summons Clássicos Faltando em `SkillHandlerRegistry`**:
  - **L2JLopez**: `SkillHandlerRegistry.resolveServitorNpcId` (linhas 484–505) não inclui `1111` (Summon Kat the Cat), `1228` (Summon Shadow), `1328` (Summon Dark Panther), `1329` (Summon Big Boom), `1406` (Summon Feline King), `1407` (Summon Magnus the Unicorn) e `1408` (Summon Spectral Lord), fazendo todos eles caírem no `default -> 14111` (Mew the Cat)!
  - **Ref. Dream/Lucera**: `com.dream.game.skills.l2skills.L2SkillSummon`.

- **#252. Summons de Necromancer (`1129` Reanimated Man, `1154` Corrupted Man, `1334` Cursed Man) Não Consomem Cadáver**:
  - **L2JLopez**: `SkillHandlerRegistry.handleSummonServitor` (linhas 191–241) permite invocar os mortos-vivos do Necromancer sem exigir um monstro morto (`TARGET_CORPSE_MOB`) como alvo e sem remover/fazer decay do cadáver no mundo.
  - **Ref. Dream/Lucera**: `com.dream.game.handler.skill.Summon`.

- **#253. Consumo Periódico de Cristais e Tempo de Vida (`lifeTime`) de Servitors Inexistentes**:
  - **L2JLopez**: `SkillHandlerRegistry.handleSummonServitor` não agenda a task de tempo de vida (`totalLifeTime`, tipicamente 20 minutos) nem o consumo periódico de Cristais D/C/B/A (`itemConsumeIdOT` / `itemConsumeCountOT` / `itemConsumeSteps`) dos Servitors, fazendo com que durem infinitamente de graça.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2ServitorInstance.SummonLifetime`.

- **#254. Siege Summons (`14737` Siege Golem, `14768` Wild Hog Cannon, `14801` Swoop Cannon) sem Regras de Cerco**:
  - **L2JLopez**: Não existe `SiegeSummonInstance` (ou tratamento específico em `SummonInstance`/`SkillHandlerRegistry`). Os Siege Summons (`skills 13, 299, 448`) podem ser invocados fora de `SiegeZone`, não consomem Gemstones B/C por minuto, e não possuem dano especial contra Portas e Muros de Castelo (`DoorInstance`).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2SiegeSummonInstance`.

- **#255. Sistema de `Cubics` (`L2CubicInstance` / `CubicService`) Totalmente Inexistente**:
  - **L2JLopez**: O projeto não possui nenhuma classe `CubicInstance` ou `CubicService`, nem lista de cubics ativos em `PlayerCharacter`. As skills de invocação de Cubics (Storm Cubic `10`, Vampiric Cubic `22`, Life Cubic `278`, Viper Cubic `279`, Phantom Cubic `33`, Spark Cubic `1279`, Aqua Cubic `1280`, Binding Cubic `1281`, Attractive Cubic `1330` e Mass Cubics `1325–1327`) não invocam Cubics!
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2CubicInstance` e `com.dream.game.skills.l2skills.L2SkillSummon`.

- **#256. Ausência de IA de Ação Periódica dos Cubics (Cura, Ataque, Debuff e Cleanse)**:
  - **L2JLopez**: Como não há `CubicInstance`, os Cubics não executam ciclos periódicos de IA: `Life Cubic` não cura o dono/party com HP baixo, `Storm/Vampiric/Viper/Phantom/Spark/Aqua/Binding/Attractive Cubic` não disparam skills mágicas/debuffs (`4049–4055`, `4164–4166`, `5115–5116`) quando o dono está em combate, e `Smart Cubic` não remove debuffs.
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2CubicInstance.Action` e `Heal`.

- **#257. Limite de `Cubic Mastery` (Skill `143`) e Expiração de Cubics Ausentes**:
  - **L2JLopez**: Não há verificação do nível da passiva `Cubic Mastery` (`skill 143`, permitindo até 2 ou 3 Cubics simultâneos) para substituir o Cubic mais antigo quando o limite é atingido, nem task de expiração de 15 minutos (`900.000 ms`).
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PcInstance.addCubic`.

- **#258. Serialização de Cubics nos Pacotes `UserInfo` e `CharInfo`**:
  - **L2JLopez**: Os pacotes `UserInfo` e `CharInfo` em `GameServerPacket.java` escrevem `writeH(0)` fixo para a contagem de Cubics, impedindo que o cliente renderize os Cubics flutuando ao redor do personagem.
  - **Ref. Dream/Lucera**: `com.dream.game.network.serverpackets.UserInfo` e `CharInfo`.

---

## Parte 3: Economia, Lojas, Craft, Warehouse/Freight, Cursed Weapons, Augmentation, ArmorSets e Manor (#259 a #285)

- **#259. Dwarven Manufacture / Recipe Shop (`ExtendedProtocolPacketHandler`) é um Stub Quebrado**:
  - **L2JLopez**: `ExtendedProtocolPacketHandler.java` (linha 65) declara uma lista vazia local `private final List<Integer> recipeBook = new ArrayList<>()` por sessão e nunca consulta `RecipeService.getInstance().getRecipeBook(...)`!
  - **Ref. Dream/Lucera**: `com.dream.game.model.L2ManufactureList` e `RecipeController`.

- **#260. `RequestRecipeBookDestroy` Não Remove a Receita do `RecipeService` / Banco de Dados**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleRecipeBookDestroy` (linhas 773–781), deletar uma receita do livro apenas faz `recipeBook.remove(Integer.valueOf(p.recipeId()))` na lista local vazia e envia um pacote `RecipeBookItemList` vazio, sem chamar `RecipeService.getInstance().unregisterRecipe(active.objectId(), p.recipeId())`.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestRecipeBookDestroy`.

- **#261. `RequestRecipeShopMakeInfo` Envia o MP do Comprador em vez do Artesão e Preço Zerado**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleRecipeShopMakeInfo` (linhas 835–841), o servidor envia `new RecipeShopItemInfo(p.playerObjectId(), p.recipeId(), (int) active.currentMp(), active.maxMp(), -1)` usando o MP do próprio cliente comprador (`active`) em vez de buscar o jogador artesão (`p.playerObjectId()`) no `GameWorld` e validar sua `manufactureList`.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestRecipeShopMakeInfo`.

- **#262. `RequestRecipeShopMakeItem` Não Consome Ingredientes, Não Cobra Adena, Não Gasta MP e Não Cria o Item**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleRecipeShopMakeItem` (linhas 843–853), clicar em "Create" na loja de craft de um anão **não faz absolutamente nada** além de enviar `RecipeShopItemInfo`: não verifica os ingredientes no inventário do comprador, não transfere a taxa em Adena (`cost`) do comprador para o artesão, não desconta o MP do artesão, não calcula o sucesso (`calculateCraftSuccessRate`) e não adiciona o item criado ao inventário do comprador!
  - **Ref. Dream/Lucera**: `com.dream.game.manager.RecipeController.requestManufactureItem`.

- **#263. Clicar em Jogador com Loja de Craft (`STORE_TYPE_MANUFACTURE = 5`) Não Abre `RecipeShopSellList`**:
  - **L2JLopez**: Em `ActionPacketHandler.handleAction` (`network/game/handler/packet/ActionPacketHandler.java`, linhas 296–308), ao clicar num jogador sentado em loja privada, o código só verifica `STORE_TYPE_SELL (1)`, `8 (Package Sell)` e `STORE_TYPE_BUY (3)`, ignorando `STORE_TYPE_MANUFACTURE (5)`. Assim, é impossível abrir a janela de Manufacture de outro jogador!
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PcInstance.onAction`.

- **#264. Self-Craft (`TradeStorePacketHandler.handleRecipeItemMakeSelf`) Não Valida Receita Conhecida, Nível de Skill nem `DwarfCraftBonus`**:
  - **L2JLopez**: Em `TradeStorePacketHandler.handleRecipeItemMakeSelf` (`network/game/handler/packet/TradeStorePacketHandler.java`, linhas 1368–1440), o jogador pode fabricar qualquer `recipeId` sem tê-la registrada no livro (`recipeService.hasRecipe`), sem validar o nível da skill `172` (`Create Item`) ou `1320` (`Common Craft`) contra `recipe.level()`, sem usar `recipeService.calculateCraftSuccessRate(recipe, active)` (ignorando `DwarfCraftBonus`), sem verificar peso/slots livres antes de consumir os materiais e sem conceder EXP/SP de criação (`ALT_GAME_CREATION`).
  - **Ref. Dream/Lucera**: `com.dream.game.manager.RecipeController`.

- **#265. Loja de Venda em Pacote (`Package Sale` / `STORE_PACKAGE_SELL = 8`) Quebrada na Abertura e na Compra**:
  - **L2JLopez**: Em `TradeStorePacketHandler.handleSetPrivateStoreListSell` (linha 195), mesmo quando `p.packageSale()` é `true`, o servidor define `active.privateStoreType(PlayerCharacter.STORE_TYPE_SELL)` (`1`) em vez de `STORE_PACKAGE_SELL` (`8`) e transmite `PrivateStoreMsgSell` em vez de `ExPrivateStoreSetWholeMsg`. Além disso, em `handlePrivateStoreBuy` (linhas 393–430), o servidor permite comprar itens avulsos de uma loja `packageSale` em vez de exigir a compra integral de todos os lotes.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.SetPrivateStoreListSell` e `RequestPrivateStoreBuy`.

- **#266. Comando `.offline` (`CommandPacketHandler`) Cria Loja Falsa Vendendo 1 Adena por 1 Adena**:
  - **L2JLopez**: Em `CommandPacketHandler.handleDotCommand` (`network/game/handler/packet/CommandPacketHandler.java`, linhas 250–258), digitar `.offline` ignora a `SellStoreList`/`BuyStoreList`/`manufactureList` aberta pelo jogador e cria uma lista hardcoded `items.add(new OfflineShopItem(57, 1, 1))`!
  - **Ref. Dream/Lucera**: `com.dream.game.model.OfflineTradersTable`.

- **#267. Lojas Privadas (`Buy`/`Sell`) e `Trade` Não Validam Peso/Slots Livres e Destroem `Augmentation` na Transferência**:
  - **L2JLopez**: Em `TradeStorePacketHandler.handlePrivateStoreBuy` (linhas 456–476) e `handlePrivateStoreSell` (linhas 1293–1313), não há validação de capacidade de slots (`maxInventorySlots()`) nem de peso máximo do comprador antes da transação. Além disso, `handleSetPrivateStoreListSell` permite colocar itens com `Augmentation` à venda e, ao vendê-los, destrói o item do vendedor e cria um novo para o comprador sem o `Augmentation`.
  - **Ref. Dream/Lucera**: `com.dream.game.model.TradeList.privateStoreBuy`.

- **#268. Sistema de `Freight` / `PackageSendable` (`ExtendedProtocolPacketHandler`) Totalmente Falso**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handlePackageSendableItemList` (linhas 865–883), o servidor não consulta o banco de dados para listar os personagens da mesma conta (`account_name`); em vez disso, envia um mapa dummy `chars.put(p.playerObjectId(), "AccountChar")`. E em `handlePackageSend` (linhas 885–892), chama `session.onWareHouseDeposit(...)`, depositando os itens no **armazém privado do próprio remetente** em vez de enviá-los ao `FREIGHT` do personagem destinatário (`p.playerObjectId()`) cobrando `1000` Adena por item!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestPackageSendableItemList`, `RequestPackageSend` e `PcFreight`.

- **#269. `WarehouseService` Não Possui Suporte a `Clan Warehouse` (`CLANWH`) nem `Freight` (`FREIGHT`)**:
  - **L2JLopez**: `WarehouseService.java` (`game/service/WarehouseService.java`, linhas 1–173) opera exclusivamente sobre um mapa em memória indexado por `ownerId` com `loc="WAREHOUSE"`. Não existe separação de localização `CLANWH` (com verificação de privilégio `CP_CL_VIEW_WAREHOUSE` para retirada pelo Líder/membros autorizados) nem `FREIGHT` em `TradeStorePacketHandler.handleWareHouseDeposit` / `handleWareHouseWithdraw` (linhas 975–1077).
  - **Ref. Dream/Lucera**: `com.dream.game.model.itemcontainer.ClanWarehouse` e `PcFreight`.

- **#270. `WarehouseService` Perde `Augmentation` ao Carregar do Banco e Não Valida Slots/Peso na Retirada**:
  - **L2JLopez**: `WarehouseService.getWarehouseItems` (linhas 36–63) reconstrói `ItemInstance` sem carregar o `AugmentationRecord` da tabela `augmentations`, fazendo armas augmentadas perderem o augment ao reiniciar o servidor ou retirar do armazém. Além disso, `withdrawItem` (linhas 124–166) não verifica limite de slots nem peso do inventário, e `depositItem` permite depositar Cursed Weapons e itens equipados sem desequipá-los.
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.SendWareHouseDepositList` e `SendWareHouseWithDrawList`.

- **#271. Compra de Sementes de Manor (`RequestBuySeed`) Entrega Sementes de Graça sem Integrar com `CastleManorManager`**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleBuySeed` (linhas 908–923), o pacote ignora `CastleManorManager.buySeed`, **não cobra Adena do jogador**, não reduz o estoque de sementes do castelo, não deposita o valor no cofre do castelo (`Castle.addToTreasury`) e envia uma mensagem com preço fixo `100`!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestBuySeed`.

- **#272. Venda de Colheitas de Manor (`RequestProcureCropList`) Não Consome Crops nem Entrega Recompensas Reais**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleProcureCropList` (linhas 943–958), o handler ignora `CastleManorManager.sellCrop`, **não remove as colheitas (`crops`) do inventário do jogador**, não desconta do estoque de compra do castelo e não entrega o material de recompensa (`reward1` ou `reward2` do `SeedTemplate`) ao jogador — apenas envia `ExShowSellCropList` com IDs fixos `1864, 1865`!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestProcureCropList`.

- **#273. Configuração de Manor pelo Lorde (`RequestSetSeed` e `RequestSetCrop`) Não Persiste nem Valida Líder do Castelo**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.handleSetSeed` (linhas 960–969) e `handleSetCrop` (linhas 971–983), o servidor não verifica se o jogador é o Líder do Clã dono do castelo (`manorId`) com privilégio `CP_CS_MANOR_ADMIN`, não valida os limites de preço (60% a 1000% do preço base) e **não chama** `CastleManorManager.setSeedProduction` nem `setCropProcure` (apenas reenvia a janela vazia)!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestSetSeed` e `RequestSetCrop`.

- **#274. `CastleManorManager` sem Agendamento Diário de Refresh (`20:00`) e Approve (`06:00`) + Cofre do Castelo**:
  - **L2JLopez**: Em `CastleManorManager.java` (`game/manor/CastleManorManager.java`, linhas 355–387), os métodos `getNextRefreshTimeMillis()` e `getNextApproveTimeMillis()` existem mas **nunca são agendados** em nenhum `ScheduledExecutorService`, e a transição de período não debita o custo de `CropProcure` do cofre do castelo (`CastleService`).
  - **Ref. Dream/Lucera**: `com.dream.game.manager.CastleManorManager`.

- **#275. `CursedWeaponsManager.checkDrop` e `onPlayerKilled` Nunca São Chamados no Combate**:
  - **L2JLopez**: `PlayerCombatHandler.handleMonsterDeath` (`network/game/handler/packet/PlayerCombatHandler.java`, linhas 43–241) nunca chama `CursedWeaponsManager.checkDrop(npc.npcId(), active)`, e `PlayerCombatHandler.handlePlayerKill` (linhas 272–330) nunca chama `CursedWeaponsManager.onPlayerKilled(attacker, targetPlayer)`. As Cursed Weapons (`Zariche 8190` e `Akamanah 8689`) nunca dropam de monstros nem evoluem/caem no PvP!
  - **Ref. Dream/Lucera**: `com.dream.game.manager.CursedWeaponsManager`.

- **#276. Ativação de Cursed Weapon Não Equipa a Arma, Não Altera Karma/PK e Não Salva Karma Original no Banco**:
  - **L2JLopez**: Em `CursedWeaponsManager.activate` (`game/cursed/CursedWeaponsManager.java`, linhas 157–178), ativar Zariche/Akamanah não força o desequipamento da arma/escudo atual, não equipa a Cursed Weapon no slot `RHAND`/`LRHAND`, não define o Karma do jogador para `9999999`, e `persistDb` (linhas 236–260) não salva nem restaura `playerKarma` e `playerPkKills` na tabela `cursed_weapons`.
  - **Ref. Dream/Lucera**: `com.dream.game.model.CursedWeapon.activate` e `endOfLife`.

- **#277. Cursed Weapons Não Concedem as Skills Passivas (`3603`/`3629`) e Ativas (`3630` Void Flow / `3631` Void Burst)**:
  - **L2JLopez**: `CursedWeaponsManager.activate` e `onPlayerLogin` não adicionam ao jogador a skill passiva da Cursed Weapon (`3603` Demonic Sword Zariche ou `3629` Blood Sword Akamanah no nível correspondente ao `stage`) nem as skills ativas `3630` e `3631`, não expulsam o portador de Party/Olympiad, não desmontam Pet e não transmitem `ExRedSky(10)` + `Earthquake` + anúncio global de localização.
  - **Ref. Dream/Lucera**: `com.dream.game.model.CursedWeapon.giveSkill`.

- **#278. Ausência de Task de Expiração (`endTime`) e Consumo de Duração por Abate nas Cursed Weapons**:
  - **L2JLopez**: `CursedWeaponsManager` não agenda nenhuma task periódica para verificar `cw.isExpired()` enquanto o portador está online e remover a arma ao expirar o `endTime`.
  - **Ref. Dream/Lucera**: `com.dream.game.model.CursedWeapon.DailyTimerTask`.

- **#279. Pacotes de Janela de Augmentation / Refinaria do Ferreiro (`ExConfirmVariationItem`, `RequestRefine`, etc.) Não Fazem Parte do Fluxo de Jogo**:
  - **L2JLopez**: Em `ExtendedProtocolPacketHandler.java` (linhas 683–762), os handlers `handleConfirmTargetItem`, `handleConfirmRefinerItem`, `handleConfirmGemStone`, `handleRefine`, `handleConfirmCancelItem` e `handleRefineCancel` **não validam os itens no inventário, não consomem a Life Stone, não consomem as Gemstones D/C/B, não cobram a Adena de cancelamento e não chamam `AugmentationService.getInstance().generateRandomAugmentation(...)`** — apenas enviam `ExVariationResult(1, 1, 1)` fake!
  - **Ref. Dream/Lucera**: `com.dream.game.network.clientpackets.RequestRefine` e `RequestRefineCancel`.

- **#280. `updateAugmentationBonus` e `updateArmorSetBonus` Não São Chamados no Login (`EnterWorld`)**:
  - **L2JLopez**: Quando o jogador entra no mundo em `GameSession.java`, os métodos `itemHandler.updateAugmentationBonus()` e `itemHandler.updateArmorSetBonus()` não são invocados no carregamento inicial do personagem, deixando o jogador sem os stats/skills de Augmentation e de ArmorSet até que ele desequipe e reequipe manualmente uma peça!
  - **Ref. Dream/Lucera**: `com.dream.game.model.actor.instance.L2PcInstance.restore`.

- **#281. Bônus de Vitals de Augmentation (`maxHp`, `maxMp`, `maxCp`, `regHp`, `regMp`, `regCp`) Ignorados em `recalcMaxVitals`**:
  - **L2JLopez**: `ItemPacketHandler.updateAugmentationBonus` (linhas 869–875) cria `StatFunc`s para `maxHp`, `maxMp`, `maxCp`, `regHp`, `regMp`, `regCp` em `augmentationFuncs`, mas `PlayerStats.calculate` não calcula HP/MP/CP máximos e `GameSession.recalcMaxVitals` não aplica `augmentationFuncs` para `regHp`/`regMp`/`regCp`.
  - **Ref. Dream/Lucera**: `com.dream.game.datatables.xml.AugmentationData`.

- **#282. Skills `chance` de Augmentation (`onCrit`, `onCast`, `onAttacked`) Adicionadas como Skills Normais em vez de Gatilhos**:
  - **L2JLopez**: Em `ItemPacketHandler.updateAugmentationBonus` (linha 892), qualquer skill de Augmentation (`aug.skillId()`) é inserida diretamente em `active.skills().put(aug.skillId(), aug.skillLevel())`, mesmo quando o tipo em `augmentation_skillmap.xml` é `chance` (que deveria ser engatilhado apenas ao acertar crítico, conjurar magia ou receber dano em combate, e não aparecer como skill ativa conjurável a qualquer momento).
  - **Ref. Dream/Lucera**: `com.dream.game.model.L2Augmentation` e `ChanceSkillList`.

- **#283. Desequipar Arma Augmentada Não Remove Buffs Ativos Concedidos pela Augmentation (`CancelAugmentationEffect`)**:
  - **L2JLopez**: `AugmentationService.shouldCancelAugmentationEffectOnUnequip()` (`game/augmentation/AugmentationService.java`, linha 492) nunca é chamado em `ItemPacketHandler.updateAugmentationBonus` (linhas 842–852), permitindo que o jogador use uma arma com buff ativo de Augmentation e troque para outra arma mantendo o buff.
  - **Ref. Dream/Lucera**: `com.dream.game.model.L2Augmentation.removeBonus`.

- **#284. `ArmorSetsTable` Ignora `skill_lvl` do Conjunto e `ItemPacketHandler` Força Nível `1` Fixo**:
  - **L2JLopez**: Em `ArmorSetsTable.java` (`game/item/ArmorSetsTable.java`, linha 154) e `ItemPacketHandler.updateArmorSetBonus` (linha 768), o nível da skill do conjunto (`skill_lvl` ou formato `skillId-skillLvl`) não é lido do XML/SQL, aplicando sempre `active.skills().put(skId, 1)`. Isso quebra conjuntos cuja passiva exige nível > 1 (ex.: `Skill 3006` ou sets específicos por grade).
  - **Ref. Dream/Lucera**: `com.dream.game.datatables.xml.ArmorSetsTable` e `L2ArmorSet`.

- **#285. `ArmorSetsTable` Não Carrega nem Aplica os Bônus de Atributos Base (`STR`, `CON`, `DEX`, `INT`, `WIT`, `MEN`) do Conjunto**:
  - **L2JLopez**: Em `ItemPacketHandler.updateArmorSetBonus` (linha 777), a chamada `active.setArmorSetBonus(set.chest(), List.of())` passa sempre uma lista vazia `List.of()` para `armorSetFuncs`, e `PlayerStats.calculate` (linhas 97–108) calcula `STR/DEX/CON/INT/WIT/MEN` apenas a partir de `t.baseSTR() + player.hennaSTR()`, sem somar modificadores de `StatFunc` de `ArmorSet`!
  - **Ref. Dream/Lucera**: `com.dream.game.model.L2ArmorSet` e `PcStat`.

---

Relatório de Auditoria Cirúrgica concluído para o **Subagente 4 (Mundo, GeoEngine, Pathfinding, Zonas, Portas, Barcos, Spawns, IA e Bosses)**, comparando o `L2JLopez` contra o `L2JDreamV2` e o `L2JLucera2`.

Abaixo estão os **85 itens técnicos concretos e acionáveis (#286 a #370)**:

---

### Área 1: GeoEngine, Pathfinding e Integração de Movimento/LoS (#286 a #306)

- **#286.** [`ActionPacketHandler.java` / `MovementPacketHandler.java` vs `L2JDreamV2: MoveBackwardToLocation.java` / `L2JLucera2: CharMoveToLocation.java`] **Ausência total de validação `GeoEngine.moveCheck` no clique de movimento do jogador**: Em `ActionPacketHandler.handleMove(MoveBackwardToLocation)` (linhas 48–117) e `MovementPacketHandler.handleMove` (linhas 25–30), o servidor nunca consulta `GeoEngine.moveCheck` nem `GeoEngine.getHeight` para truncar o destino antes de transmitir `MoveToLocation(active.objectId(), p.targetX(), p.targetY(), p.targetZ(), ...)`, permitindo que o jogador atravesse paredes, portas fechadas e penhascos clicando do outro lado.
- **#287.** [`ActionPacketHandler.java` / `GameTimeController.java` vs `L2JDreamV2: GameTimeController.java` / `L2Character.java` / `L2JLucera2: GameTimeController.java`] **Inexistência de motor de interpolação de movimento server-side (`MoveData` / `moveObjects`) para jogadores**: `ActionPacketHandler.handleMove` (linha 97) apenas define `active.moveTo(p.originX(), p.originY(), p.originZ())` e depende exclusivamente de pacotes `ValidatePosition` enviados pelo cliente para atualizar X/Y/Z, sem interpolar a posição do jogador no servidor a cada tick (100ms) conforme a `runSpd`/`walkSpd`.
- **#288.** [`ActionPacketHandler.java` vs `L2JDreamV2: ValidatePosition.java` / `L2JLucera2: ValidatePosition.java`] **Falta de validação de Geodata, Z (`GEO_CORRECT_Z`) e colisão dinâmica em `ValidatePosition`**: `ActionPacketHandler.handleValidatePosition` (linhas 119–151) aceita qualquer coordenada enviada pelo cliente cuja distância 2D seja `<= 1000` unidades (`diffSq <= 1_000_000.0`) e sobrescreve `active.moveTo(p.x(), p.y(), p.z())` sem checar `GeoEngine.getHeight`, `GeoEngine.moveCheck`, `isBlockedByDynamicObjects` nem zonas de água (`ZoneTable.isInsideWater`), permitindo wallhack/clipping e adulteração de Z.
- **#289.** [`ActionPacketHandler.java` / `CombatService.java` vs `L2JDreamV2: MoveBackwardToLocation.java` / `L2JLucera2: CharMoveToLocation.java`] **Configuração `ALLOW_KEYBOARD_MOVEMENT` ignorada no handler de movimento**: `ActionPacketHandler.handleMove` (linha 86) apenas registra um log quando `p.moveMovement() == 0` (movimento por setas do teclado) sem bloquear o movimento caso `Config.ALLOW_KEYBOARD_MOVEMENT` seja `false`, enquanto `CombatService.isKeyboardMovementAllowed()` (linha 1456) é código morto nunca chamado.
- **#290.** [`GameSession.java` / `MagicSkillPacketHandler.java` vs `L2JDreamV2: L2PlayerAI.java` / `L2JLucera2: PlayerAI.java`] **Movimento de aproximação (approach) de auto-attack e cast de skills ignora Geodata e Pathfinding**: Em `GameSession.schedulePlayerAutoAttack(NpcInstance)` (linhas 5882–5904), `schedulePlayerAutoAttack(GameSession)` (linhas 5972–5994) e `MagicSkillPacketHandler.castSkill` (linhas 408–420), quando o jogador está fora de alcance, o servidor move o personagem em linha reta euclidiana até o alvo sem chamar `GeoEngine.moveCheck` ou `GeoEngine.findPath`, além de copiar diretamente o Z do alvo (`npc.z()` / `targetPlayer.z()`) sem consultar `GeoEngine.getHeight`.
- **#291.** [`PathFind.java` / `GeoEngine.java` vs `L2JDreamV2: GeoPathFinding.java` / `PathFinding.java`] **Ausência de suporte a arquivos de Pathnode pré-calculados (`data/pathnode/*.pn`)**: Embora `Config.PATH_FINDING_MODE` suporte o modo `"2"` (pathnodes) e `L2JDreamV2` possua `GeoPathFinding.java` para carregar grafos `.pn` em `ByteBuffer`, o `L2JLopez` possui apenas busca em células A* (`PathFind.java`) sem leitor de arquivos `.pn`.
- **#292.** [`PathFind.java` vs `L2JLucera2: PathFindBuffers.java` / `L2JDreamV2: CellNodeMap.java` / `BinaryNodeHeap.java`] **Alocação intensiva de objetos no heap a cada chamada de `PathFind.findPath`**: `PathFind.findPath` (linhas 98–176) instancia novos `PriorityQueue<Node>`, `HashMap<Long, Double>`, `HashSet<Long>` (com autoboxing de `Long`) e dezenas de objetos `Node` a cada busca de caminho de NPC, ao invés de utilizar buffers pré-alocados em pool por thread (`PathFindBuffers` no Lucera / `FastNodeList` + `BinaryNodeHeap` no Dream).
- **#293.** [`PathFind.java` vs `L2JLucera2: GeoMove.java` (`pathClean`) / `L2JDreamV2: PathFinding.java` (`cleanPath`)] **Suavização de waypoints (`reconstructWaypoints`) sem string-pulling por raycast (`moveCheck`)**: `PathFind.reconstructWaypoints` (linhas 203–238) apenas agrupa passos estritamente colineares na grade (`dx != prevDx || dy != prevDy`), sem testar `GeoEngine.moveCheck` entre pontos não-adjacentes para eliminar zigue-zagues (string-pulling) como fazem `GeoMove.pathClean` (Lucera) e `PathFinding.cleanPath` (Dream).
- **#294.** [`PathFind.java` vs `L2JLucera2: PathFind.java` (linhas 164–185)] **Cálculo de custo A* sem penalidade para células adjacentes a paredes ou degraus de altura**: `PathFind.findPath` (linhas 167–175) aplica apenas custo planar (`1.0` ortogonal, `1.414` diagonal) sem somar penalidade de peso quando a célula vizinha possui `nswe != NSWE_ALL` (parede próxima) ou variação de Z (`Math.abs(nextZ - cur.z) > 16`), fazendo NPCs rasparem em quinas e muros.
- **#295.** [`GeoEngine.java` vs `L2JLucera2: GeoEngine.java` (`NgetHeightAndNSWE`) / `L2JDreamV2: GeoEngine.java`] **Tripla varredura linear em blocos `BLOCKTYPE_MULTILAYER` por passo de raycast**: `GeoEngine.getHeight` (linhas 233–262), `getNSWE` (linhas 303–335) e `getCeilingZ` (linhas 399–421) varrem separadamente o bloco multilayer desde o início (`for (int c = 0; c < 64; c++)` a partir de `offset + 1`) e `canSeeTarget`/`moveCheck` invocam os métodos separadamente a cada passo de 16 unidades, ao invés de extrair altura e NSWE na mesma leitura indexada da célula.
- **#296.** [`GeoEngine.java` / `DoorTable.java` / `DoorInstance.java` vs `L2JLucera2: GeoCollision.java` / `L2JDreamV2: DoorTable.java` (`checkIfDoorsBetween`)] **Checagem duplicada e O(N) global de todas as portas do mundo em cada passo do `GeoEngine`**: No construtor de `GeoEngine` (linha 119) e em `DoorInstance.syncWithGeoEngine` (linha 58), todas as portas são adicionadas em `dynamicGeoObjects`, e `GeoEngine.isBlockedByDynamicObjects` (linhas 426–440) itera sobre **todos** os `dynamicGeoObjects` e em seguida novamente sobre **todas** as portas de `doorTable.allDoors()` do mundo inteiro sem filtro por região/célula nem por `instanceId` (enquanto o Lucera aplica máscara NSWE direto nas células via `GeoCollision` e o Dream filtra por região).
- **#297.** [`GeoEngine.java` vs `L2JDreamV2: GeoEngine.java` / `L2JLucera2: GeoEngine.java`] **Cálculo de Line of Sight (`canSeeTarget`) com altura de olho fixa e bloqueio vertical arbitrário em curta distância**: `GeoEngine.canSeeTarget` (linhas 482–502) usa constante fixa `ELEVATION_EYE_OFFSET = 45` (ignorando `collisionHeight` real do `Creature`/`NpcTemplate`) e na linha 500 reprova imediatamente o LoS se `dist2d <= 250.0 && Math.abs(dz) > 150.0`, quebrando ataques/skills em escadarias, rampas e muralhas de castelo.
- **#298.** [`GeoEngine.java` vs `L2JDreamV2: GeoEngine.java` / `L2JLucera2: GeoEngine.java`] **Sobrecarga `GeoEngine.canSeeTarget(PlayerCharacter, PlayerCharacter)` omite calibração de Z no geodata**: Diferente de `canSeeTarget(Creature, Creature)` (linhas 574–586), que calibra `az = getHeight(...)` e `tz = getHeight(...)`, a sobrecarga `canSeeTarget(PlayerCharacter, PlayerCharacter)` (linhas 588–594) passa `actor.z()` e `target.z()` brutos sem calibrar na camada de geodata.
- **#299.** [`GeoEngine.java` vs `L2JLucera2: GeoEngine.java` (`MAX_LAYERS`) / `L2JDreamV2: GeoEngine.java`] **Decodificação incorreta do byte de camadas em blocos `BLOCKTYPE_MULTILAYER`**: Em `GeoEngine.getHeight` (linha 237), `getNSWE` (linha 307) e `getCeilingZ` (linha 403), o número de camadas é lido com `int layers = data[offset] & 0x7F; if (layers <= 0 || layers > 32)`, sendo que no formato `.l2j` o byte de camadas é um inteiro sem sinal simples (`data[offset] & 0xFF`, tipicamente `1..125`); mascarar com `0x7F` e abortar acima de `32` corrompe a leitura da célula e dos offsets subsequentes da coluna caso uma célula tenha `> 32` camadas.
- **#300.** [`FenceInstance.java` / `GeoEngine.java` vs `L2JLucera2: FenceInstance.java` / `GeoCollision.java`] **`FenceInstance` usa distância radial ao ponto central em vez do retângulo (`width` x `length`) para colisão no `GeoEngine`**: `FenceInstance` (linhas 10–67) implementa `GeoObject` apenas com `geoX()`, `geoY()`, `geoZ()`, e `GeoEngine.intersectsGeoObject` (linhas 697–729) trata qualquer `GeoObject` que não seja `DoorInstance` como um ponto com raio fixo `32.0` (`distSq <= 32.0 * 32.0`), ignorando completamente `width` e `length` das cercas de Coliseu/eventos.
- **#301.** [`FenceInstance.java` / `GameSession.java` vs `L2JLucera2: FenceInstance.java` / `ExColosseumFenceInfo.java`] **Cercas dinâmicas (`FenceInstance`) não são registradas nem enviadas em `GameSession.updateKnownObjects`**: `GameSession.updateKnownObjects` (linhas 4650–4770) atualiza jogadores, NPCs, portas, objetos estáticos e itens no chão, mas nunca envia `ExColosseumFenceInfo` para instâncias de `FenceInstance` ao redor do jogador.
- **#302.** [`CombatService.java` / `PlayerCombatHandler.java` / `ActionPacketHandler.java` vs `L2JDreamV2: L2SummonAI.java` / `L2JLucera2: SummonAI.java`] **Summons e Pets atacam e se movem sem checagem de `GeoEngine.canSeeTarget` ou `moveCheck`**: Em `CombatService.summonAttackNpc` (linha 1325), `summonAttackPlayer` (linha 1365), `PlayerCombatHandler.triggerSummonAssistAttack` (linha 335) e `ActionPacketHandler.orderSummonAttack` (linha 779), o summon nunca valida `GeoEngine.canSeeTarget` nem `GeoEngine.moveCheck`, atacando alvos através de paredes e portas fechadas.
- **#303.** [`CombatService.java` vs `L2JDreamV2: ManaDam.java` / `L2Character.java` / `L2JLucera2: Skill.java`] **Skills de Mana Burn (`MDAM` / `MANADAM`) ignoram `GeoEngine.canSeeTarget`**: Ao contrário de `skillMdamNpc` e `skillMdamPlayer` que validam `geo.canSeeTarget`, os métodos `CombatService.skillManaDamNpc` (linha 1093), `skillManaDamPlayer` (linha 1135) e `skillManaDamNpcToPlayer` (linha 1181) nunca verificam linha de visão (`canSeeTarget`).
- **#304.** [`MagicSkillPacketHandler.java` vs `L2JDreamV2: L2Character.java` (`beginCast`) / `L2JLucera2: Creature.java`] **Skills positivas (Heal, Recharge, Buffs, Resurrect) atravessam paredes e portas fechadas**: Em `MagicSkillPacketHandler.castSkill` (linha 428), a validação `ctx().geo().canSeeTarget(...)` está restrita apenas a `if (sk.isOffensive() && ...)`, permitindo curar, buffar e ressuscitar alvos através de paredes sólidas e portas de castelo fechadas.
- **#305.** [`ActionPacketHandler.java` vs `L2JDreamV2: L2DoorInstance.java` / `L2JLucera2: DoorInstance.java`] **Ataque físico a portas (`DoorInstance`) sem checagem de alcance, aproximação, LoS ou P.Atk real**: Em `ActionPacketHandler.handleAction` (linhas 403–421) e `handleAttackRequest` (linhas 474–495), o jogador causa dano imediato a qualquer porta a qualquer distância sem checar alcance físico, sem caminhar até a porta, sem validar `GeoEngine.canSeeTarget`, sem verificar se a porta está em cerco ativo (siege) e usando uma fórmula fixa `int pAtk = 150 + active.level() * 15` em vez de `PlayerStats.pAtk()`.
- **#306.** [`MagicSkillPacketHandler.java` / `CombatService.java` vs `L2JDreamV2: L2Skill.java` (`getTargetList`) / `L2JLucera2: Skill.java`] **Skills AoE (`TARGET_AURA`, `TARGET_AREA`, `TARGET_MULTIFACE`) atingem alvos através de paredes**: Em `MagicSkillPacketHandler` e `CombatService`, os alvos secundários de habilidades em área são filtrados apenas por raio 2D (`findNpcsAround` / `findPlayersAround`), sem validar `GeoEngine.canSeeTarget` entre o centro da explosão/conjurador e cada alvo secundário.

---

### Área 2: Inteligência Artificial de NPCs, Spawns, Minions e Walkers (#307 a #328)

- **#307.** [`NpcInstance.java` / `NpcAiService.java` vs `L2JDreamV2: L2Attackable.java` (`AggroInfo`) / `L2JLucera2: AggroList.java`] **Ausência de `AggroList` (tabela de Hate/Damage) nos monstros e bosses**: `NpcInstance` (linha 22) armazena apenas um único `int targetPlayerId` em vez de um mapa `ConcurrentHashMap<Integer, AggroInfo>` com `hate` e `damage`. Quando o jogador alvo morre ou desconecta, `NpcAiService.stopCombatForPlayer` (linha 271) reseta o monstro imediatamente (`returnToSpawn`) e restaura 100% do HP dele mesmo que o restante da party ainda esteja atacando.
- **#308.** [`NpcInstance.java` / `NpcAiService.java` vs `L2JDreamV2: L2AttackableAI.java` / `L2JLucera2: DefaultAI.java`] **Monstros e Bosses nunca podem focar ou atacar Summons/Pets (`SummonInstance`)**: Como `NpcInstance` só guarda `targetPlayerId` e `NpcAiService` resolve o alvo exclusivamente via `world.player(targetId)`, nenhum monstro ou Raid Boss consegue virar o aggro ou atacar um `SummonInstance` (mesmo quando o summon ataca sozinho ou usa skills de provocação/aggro).
- **#309.** [`GuardAI.java` / `NpcAiService.java` vs `L2JDreamV2: L2GuardAI.java` / `L2JLucera2: Guard.java`] **Guardas de cidade nunca atacam monstros agressivos (`scanForAggressiveMonsters` é código morto)**: `GuardAI` (linhas 112–125) declara o método `scanForAggressiveMonsters()`, mas ele nunca é chamado em lugar nenhum, e guardas não possuem suporte para combater `NpcInstance` (apenas jogadores com `karma > 0`).
- **#310.** [`GuardAI.java` vs `L2JDreamV2: L2GuardAI.java` / `L2JLucera2: Guard.java`] **`GuardAI.moveTowards` ignora completamente `GeoEngine.findPath` e `GeoEngine.moveCheck`**: `GuardAI` (linhas 50–87) sobrescreve `AbstractNpcAI.moveTowards` com cálculo trigonométrico direto (`npc.x() + cos * step`) sem chamar `geo.findPath` nem `geo.moveCheck`, fazendo guardas atravessarem muros, casas e obstáculos ao perseguir PKs.
- **#311.** [`NpcAiFactory.java` / `SpawnService.java` vs `L2JDreamV2: L2SiegeGuardAI.java` / `L2FortSiegeGuardAI.java` / `L2JLucera2: SiegeGuard.java`] **Inexistência de `SiegeGuardAI` para guardas de Castelo e Fortaleza**: Embora `SpawnService` carregue `fort_spawnlist` (linha 261) e guardas de castelo, `NpcAiFactory` não possui `SiegeGuardAI` para atacar membros de clãs atacantes não-registrados/atacantes durante cercos de Castelo e Fortaleza.
- **#312.** [`NpcAiService.java` / `AbstractNpcAI.java` vs `L2JDreamV2: MinionList.java` (`moveMinions`) / `L2JLucera2: DefaultAI.java`] **Minions não acompanham a movimentação do Boss/Líder**: Quando um monstro líder com minions (`npc.minions()`) patrulha (`roamIdleMonsters`) ou se move em combate (`AbstractNpcAI.moveTowards`), os minions permanecem parados nas coordenadas iniciais de spawn em vez de manter formação ao redor do líder.
- **#313.** [`NpcAiService.java` vs `L2JDreamV2: L2AttackableAI.java` (`thinkActive`) / `L2JLucera2: DefaultAI.java`] **Patrulha de monstros ociosos (`roamIdleMonsters`) ignora Geodata e teleporta coordenadas em 0ms**: `NpcAiService.roamIdleMonsters` (linhas 589–607) sorteia `targetX`/`targetY` sem validar `GeoEngine.moveCheck`, fixa o Z sempre em `npc.spawnZ()` sem consultar `GeoEngine.getHeight` e atualiza `npc.moveTo(targetX, targetY, npc.spawnZ())` instantaneamente antes mesmo do cliente animar o `MoveToLocation`.
- **#314.** [`NpcAiService.java` vs `L2JDreamV2: L2AttackableAI.java` (`MAX_DRIFT_RANGE`) / `L2JLucera2: DefaultAI.java` (`MAX_PURSUE_RANGE`)] **Monstros comuns podem ser "kitados" infinitamente pelo mapa sem checar distância do spawn**: `NpcAiService.processCombatNpc` (linhas 681–688) verifica apenas a distância entre o monstro e o jogador (`dist3d > 1500.0`), nunca checando a distância do monstro em relação ao seu ponto de origem (`npc.spawnX(), npc.spawnY()`), permitindo arrastar mobs comuns por regiões inteiras.
- **#315.** [`NpcAiService.java` / `RaidBossAI.java` vs `L2JDreamV2: L2Attackable.java` (`returnHome`) / `L2JLucera2: DefaultAI.java` (`teleportHome`)] **Retorno ao spawn (`returnToSpawn` e `RaidBossAI.returnHome`) causa dessincronização visual no cliente**: `NpcAiService.returnToSpawn` (linhas 717–745) altera `npc.moveTo(spawnX, spawnY, spawnZ)` em 0ms mas envia `MoveToLocation` (caminhada lenta) para distâncias que exigem `TeleportToLocation`; já `RaidBossAI.returnHome` (linhas 81–89) muda `npc.moveTo(spawnX, spawnY, spawnZ)` sem enviar **nenhum** pacote (`TeleportToLocation` ou `DeleteObject` + `NpcInfo`), deixando o Raid Boss invisível/fantasma no cliente.
- **#316.** [`PriestAI.java` vs `L2JDreamV2: L2AttackableAI.java` / `L2JLucera2: Priest.java` / `Mystic.java`] **`PriestAI.findWoundedAlly` cura qualquer NPC ferido (inclusive guardas ou mobs de outra facção) e atravessa paredes**: Em `PriestAI.findWoundedAlly` (linhas 113–120), caso não encontre aliado da mesma `factionId`, o fallback seleciona **qualquer** `NpcInstance` ferido em 600 unidades independente de facção ou tipo, e `healAlly` nunca verifica `GeoEngine.canSeeTarget`.
- **#317.** [`PriestAI.java` / `FighterAI.java` vs `L2JDreamV2: L2Character.java` (`onMagicHitTimer`) / `L2JLucera2: Creature.java`] **Curas de `PriestAI` e self-buffs de `FighterAI` aplicam efeito instantaneamente no início do cast (0ms)**: Em `PriestAI.healAlly` (linhas 144–175) e `FighterAI.castMonsterSkill` (linhas 254–278), o HP da cura e o buff são aplicados imediatamente junto com o pacote `MagicSkillUse` e `npc.casting(false)` é limpo na mesma linha, impedindo que Stun, Silence ou morte interrompam a conjuração do monstro.
- **#318.** [`MysticAI.java` / `RangerAI.java` vs `L2JLucera2: Mystic.java` / `Ranger.java` / `L2JDreamV2: L2AttackableAI.java`] **`MysticAI` nunca faz kiting/recuo e `RangerAI` nunca usa skills de monstro ao atacar de longe**: `MysticAI.processCombat` (linhas 93–137) nunca se afasta quando o alvo cola corpo-a-corpo, enquanto `RangerAI.processCombat` (linhas 40–80) nunca chama `chooseMonsterSkill()` quando está fora do raio de recuo e teleporta a posição do NPC em 0ms em `retreatFromTarget` (linha 103).
- **#319.** [`NpcAiService.java` vs `L2JDreamV2: HotSpringDisease.java` / `ForgeOfTheGods.java` / `L2JLucera2: HotSpringsMob.java`] **Mecanismos de Hot Springs Debuffs (Malaria/Cholera/Flu/Rheumatism) e Clones de Forge of the Gods são métodos no-op**: Em `NpcAiService.processCombatNpc` (linhas 690–691), os retornos booleanos de `tryRollHotSpringDebuff(npc)` e `tryRollFogMobClone(npc)` (linhas 624–644) são descartados sem aplicar as skills `4551..4554` no jogador nem spawnar o monstro clone de FoG.
- **#320.** [`GameSession.java` / `MagicSkillPacketHandler.java` / `NpcInstance.java` / `BossManager.java` vs `L2JDreamV2: L2Character.java` (`checkRaidCurse`) / `L2JLucera2: RaidFighter.java`] **Raid Curse (`4215` / `4515`) sem animação `MagicSkillUse` do Boss, sem punição para healers/buffers e com métodos duplicados mortos**: `NpcInstance.shouldApplyRaidCurse` (linha 540) e `BossManager.shouldApplyRaidFossilization` (linha 582) nunca são chamados; a lógica real em `GameSession.onAttackNpc` (linha 5695) e `MagicSkillPacketHandler` (linha 1043) aplica petrificação/silêncio sem transmitir `MagicSkillUse(boss, player, 4515/4215)` e não pune jogadores de nível `>= bossLevel + 9` que curam ou buffam um membro da party em combate com o Boss.
- **#321.** [`AutoSpawnService.java` / `NpcWalkerRoutesTable.java` vs `L2JDreamV2: NpcWalkerRoutesTable.java` / `AutoSpawnHandler.java` / `L2JLucera2: WalkerRoutesTemplate.java`] **Walkers (`walker_routes.xml`) nunca são spawnados na inicialização e `stepWalkers` teleporta NPCs a cada 1s**: `AutoSpawnService.init()` (linhas 60–63) carrega `NpcWalkerRoutesTable`, mas `spawnAndRegisterWalker` e `registerWalker` nunca são chamados em produção; além disso, `stepWalkers` (linha 201) avança um nó inteiro a cada 1 segundo mudando `npc.moveTo(...)` instantaneamente sem calcular o tempo real de deslocamento pela velocidade (`walkSpd`/`runSpd`).
- **#322.** [`AutoSpawnService.java` vs `L2JDreamV2: AutoSpawnHandler.java` / `L2JLucera2: AutoSpawnManager.java`] **`AutoSpawnService` não carrega a tabela `random_spawn` / `auto_spawn` nem transmite `NpcInfo` / `DeleteObject`**: `AutoSpawnService` nunca lê as entradas de auto-spawn do banco/XML (como Blacksmith of Mammon e Merchant of Mammon), e seus métodos `stepAutoSpawns` (linha 128) e `despawnAutoSpawn` (linha 151) adicionam/removem o NPC no `GameWorld` sem transmitir `NpcInfo` nem `DeleteObject` aos jogadores próximos.
- **#323.** [`DayNightSpawnService.java` vs `L2JDreamV2: DayNightSpawnManager.java` / `L2JLucera2: DayNightSpawnManager.java`] **`DayNightSpawnService` ignora spawns de período Dia/Noite do banco/XML (`spawnlist` `periodOfDay`) e Raid Bosses noturnos (Eilhalder von Hellmann `25328`)**: `DayNightSpawnService.loadDefaultTemplates` (linhas 163–178) possui apenas 6 spawns hardcoded em código (3 diurnos e 3 noturnos) em vez de filtrar todos os spawns com `periodOfDay = 1` (dia) e `periodOfDay = 2` (noite) e gerenciar o Raid Boss noturno Eilhalder von Hellmann (`25328`).
- **#324.** [`SpawnService.java` vs `L2JDreamV2: L2Spawn.java` / `SpawnTable.java` / `L2JLucera2: Spawn.java`] **Spawns em coordenadas fixas sem suporte a territórios poligonais (`spawn_territories` / `locId`) e sem validação `GeoEngine.getHeight`**: `SpawnService.loadFromXml` e `loadFromDatabase` ignoram spawns baseados em `locId`/territórios poligonais e instanciam `NpcInstance` diretamente no Z bruto do XML/SQL sem ajustar a altura via `GeoEngine.getHeight(x, y, z)`.
- **#325.** [`NpcAiService.java` vs `L2JDreamV2: L2Attackable.java` / `L2JLucera2: MonsterInstance.java`] **Ausência de `Over-hit` e `Sweep` (Spoil) / `Seed` (Manor) integrados ao ciclo de morte e despawn do `NpcAiService`**: Em `NpcAiService.scheduleRespawn` (linhas 201–263), o cadáver é removido em tempo fixo (`CORPSE_DECAY_SECONDS = 7`) sem diferenciar o tempo extra de cadáver "spoiled" (`SPOILED_CORPSE_DECAY`) ou limpar estado de `seeded`/`spoiled` no re-spawn caso o mesmo objeto fosse reutilizado.
- **#326.** [`AbstractNpcAI.java` / `FighterAI.java` vs `L2JDreamV2: L2AttackableAI.java` (`callFaction`) / `L2JLucera2: DefaultAI.java` (`notifyFriends`)] **Chamado de facção (`alertClanAllies`) ignora `factionRange` individual do `NpcTemplate` e checagem de Z**: `FighterAI.alertClanAllies` (linhas 197–223) usa constante fixa `CLAN_HELP_RADIUS = 300` em vez do `factionRange` definido no template de cada monstro e não filtra diferença de altura (`Math.abs(ally.z() - npc.z()) > 200`), puxando monstros de outros andares de masmorras/catacumbas.
- **#327.** [`QueenAntNurseAI.java` / `NpcAiService.java` vs `L2JDreamV2: QueenAnt.java` / `L2JLucera2: QueenAntNurse.java`] **`QueenAntNurseAI` nunca é acionada enquanto as Nurse Ants não estiverem em combate direto com um jogador**: O loop de `NpcAiService.tick` (linhas 407–426) só executa `processCombatNpc(npc)` se `npc.inCombat() && npc.targetPlayerId() != 0`. Como ninguém ataca a Nurse Ant diretamente quando bate na Queen Ant ou na Larva, `QueenAntNurseAI.processCombat` nunca roda e as Nurses nunca curam a Queen Ant ou a Larva!
- **#328.** [`NpcAiFactory.java` / `VanHalterAI.java` vs `L2JDreamV2: Vanhalter.java` / `L2JLucera2: AndreasVanHalterInstance.java`] **`VanHalterAI` é código morto devido a mapeamento incorreto em `NpcAiFactory`**: Em `NpcAiFactory.createAI` (linhas 52–54), o ID `29062` (High Priestess van Halter) está incluído na condição que retorna `new RaidBossAI(npc, service, world, geo)` em vez de `new VanHalterAI(npc, service, world, geo)`, tornando `VanHalterAI.java` inteiramente inacessível.

---

### Área 3: Grand Bosses, Raid Bosses e Instâncias (#329 a #352)

- **#329.** [`GrandBossManager.java` vs `L2JDreamV2: AntharasManager.java` / `ValakasManager.java` / `BaiumManager.java` / `FrintezzaManager.java` / `SailrenManager.java`] **`GrandBossManager.scheduleOrSpawnAll()` dá spawn imediato em TODOS os 10 Grand Bosses no boot do servidor e muda o status deles para `ALIVE`**: Nas linhas 213–231 de `GrandBossManager.java`, sempre que um Grand Boss está com `status == NOTSPAWN` ou `ALIVE`, `spawnBoss(info.bossId())` é executado na inicialização e grava `status = ALIVE`. Isso faz com que Antharas, Valakas, Baium, Frintezza e Sailren já nasçam vivos dentro das salas no boot em vez de aguardarem em estado `NOTSPAWN` (`DORMANT`) pelo gatilho de entrada dos jogadores!
- **#330.** [`AntharasService.java` / `ValakasService.java` vs `L2JDreamV2: Antharas.java` / `Valakas.java` / `L2JLucera2: Antharas.java` / `Valakas.java`] **Entrada nos covis de Antharas e Valakas nunca dispara a contagem regressiva (`WAITING`) nem a sequência de câmeras (`SpecialCamera`)**: Como `GrandBossManager.init()` já coloca Antharas e Valakas em `BossStatus.ALIVE` no boot, `AntharasService.enterLair` (linha 151) e `ValakasService.enterLair` (linha 143) veem `status == ALIVE` (não `NOTSPAWN`) e pulam `startWaitingCountdown()`; além disso, quando o boss nasce, há apenas um único pacote `SpecialCamera` genérico em vez da cinemática multi-estágio retail.
- **#331.** [`AntharasService.java` / `ValakasService.java` vs `L2JDreamV2: Antharas.java` / `Valakas.java` / `L2JLucera2: Antharas.java` / `Valakas.java`] **Teleport Cubes de saída (`31859` / `31759`) e minions de Antharas são adicionados ao mundo sem transmitir `NpcInfo`**: Em `AntharasService.spawnMinions` (linha 254), `AntharasService.spawnExitCube` (linha 301) e `ValakasService.spawnExitCube` (linha 253), `world.addNpc(...)` é chamado sem `world.broadcastAround(..., new NpcInfo(...))`, deixando os cubos de saída e os Behemoth Dragons invisíveis para os jogadores dentro do covil; também faltam `Tarasque Dragon (29070)` e `Dragon Bomber (29071..29076)`.
- **#332.** [`AntharasAI.java` / `ValakasAI.java` vs `L2JDreamV2: Antharas.java` / `Valakas.java` / `L2JLucera2: Antharas.java` / `Valakas.java`] **Self-buffs de Antharas (`4241` Antharas Armor) e Valakas (`4680` Valakas Lava Skin) não aplicam status nem transmitem `MagicSkillUse`**: `AntharasAI.applyBuff` (linhas 71–77) e `ValakasAI.applyBuff` (linhas 71–77) apenas somam 5% de HP ao dragão sem aplicar o buff real (`npc.applySkillBuff`) e sem enviar `MagicSkillUse`.
- **#333.** [`BaiumService.java` / `BaiumAI.java` vs `L2JDreamV2: Baium.java` / `L2BaiumAngelAI.java` / `L2JLucera2: Baium.java`] **Despertar de Baium (`awakenBaium`) sem remoção visual da estátua (`29025`), sem morte do despertador, sem cinemática retail e Arcanjos (`29021`) não atacam Baium**: `BaiumService.awakenBaium` (linhas 171–249) não remove a estátua `29025` do mundo com `DeleteObject`, não executa o `SocialAction` + terremoto + sequência de `SpecialCamera` + abate do jogador que o despertou, os 5 Arcanjos não possuem IA (`L2BaiumAngelAI`) para atacar Baium e `putBaiumToSleep` (linha 257) / `clearFloor` (linha 331) removem NPCs sem enviar `DeleteObject`.
- **#334.** [`QueenAntService.java` / `GrandBossManager.java` vs `L2JDreamV2: QueenAnt.java` / `QueenAntManager.java` / `L2JLucera2: QueenAntInstance.java`] **`QueenAntService.spawnQueenAnt()` nunca é chamado pelo `GrandBossManager`, deixando a Larva (`29004`), Nurses (`29002`) e Guards (`29003`) sem spawn**: `GrandBossManager.spawnBoss(29001)` (linha 234) spawna apenas o NPC `29001` via `SpawnService`, nunca invocando `QueenAntService.spawnQueenAnt()` (linha 79); além disso, Nurses e Royal Guards mortos não possuem tarefas individuais de re-spawn (`QUEEN_ANT_RESPAWN_NURSES` / `QUEEN_ANT_RESPAWN_GUARDS`) e jogadores nível `> 48` não são teleportados para fora da zona do formigueiro.
- **#335.** [`CoreAndOrfenService.java` / `OrfenAI.java` vs `L2JDreamV2: Core.java` / `Orfen.java` / `L2JLucera2: Core.java` / `Orfen.java`] **`CoreAndOrfenService.spawnCore()` e `spawnOrfen()` nunca são chamados pelo `GrandBossManager`, e `OrfenAI` está desconectada de `retreatOrfenToNest`**: O `GrandBossManager` nunca invoca `CoreAndOrfenService`, portanto os guardas de Core (`Susceptor` / `Death Knight` / `Doom Wraith`) e de Orfen (`Riba Iren` / `Raikel Leos`) nunca nascem; além disso, `OrfenAI.processCombat` (linha 44) move Orfen para o ninho sem chamar `CoreAndOrfenService.retreatOrfenToNest` e sem transmitir `TeleportToLocation` ou `DeleteObject` + `NpcInfo`.
- **#336.** [`ZakenService.java` / `ZakenAI.java` / `GameTimeController.java` vs `L2JDreamV2: ZakenManager.java` / `ZakenMove.java` / `L2JLucera2: Zaken.java`] **Porta do navio de Zaken (`21240006`) nunca abre à meia-noite e teletransporte de Zaken não envia pacotes nem invoca piratas**: `ZakenService.onGameHourChanged(int)` (linha 139) nunca é chamado pelo `GameTimeController`; além disso, `ZakenAI.processCombat` (linhas 45–69) muda as coordenadas de Zaken (`npc.moveTo`) sem transmitir `MagicSkillUse(4222)` / `TeleportToLocation`, não envia `TeleportToLocation` aos jogadores espalhados com `player.moveTo` (linha 54), não regenera HP à noite e nunca spawna `PIRATE_MINION (29023)`.
- **#337.** [`SailrenService.java` / `NpcBypassPacketHandler.java` vs `L2JDreamV2: Sailren.java` / `SailrenManager.java` / `L2JLucera2: Sailren.java`] **Entrada e waves de Sailren quebradas: validação de party bypassada, waves por timer fixo em vez de kill, e falta de `NpcInfo` / `TeleportToLocation`**: Em `SailrenService.canEnter(PlayerCharacter)` (linha 104), o código passa `Config.SAILREN_ENABLE_SINGLE_PLAYER` no lugar de `hasPartyOrSingleAllowed`, ignorando a exigência de party; `enterNest` (linha 141) e `clearNest` (linha 249) apenas mutam `pc.x/y/z` sem enviar `TeleportToLocation` nem mover a party; `startWaveSequence` (linhas 157–169) agenda Velociraptor, Pterosaur, Tyrannosaurus e Sailren por tempo fixo (30s, 60s, 90s) mesmo que a wave anterior continue viva; e `spawnWaveMob`/`spawnSailren`/`spawnExitCube` nunca enviam `NpcInfo`.
- **#338.** [`VanHalterService.java` vs `L2JDreamV2: Vanhalter.java` / `VanHalterManager.java` / `L2JLucera2: AndreasVanHalterInstance.java`] **`VanHalterService` não possui `@PostConstruct` nem gatilho de ritual no Templo Pagão, e spawns/despawns não enviam `NpcInfo` / `DeleteObject`**: `VanHalterService.triggerVanHalterAppearance()` e `spawnVanHalter()` nunca são chamados no boot nem ao matar os `Triol's Revelation` / guardas do altar; não há controle das portas do Altar of Sacrifice, e `spawnVanHalter` (linha 137), `spawnHelpers` (linha 161) e `clearMinions` (linha 225) não enviam `NpcInfo` nem `DeleteObject`.
- **#339.** [`FrintezzaService.java` / `NpcBypassPacketHandler.java` / `BypassEventsAndPveHandler.java` vs `L2JDreamV2: FrintezzaManager.java` / `L2JLucera2: FrintezzaInstance.java`] **`FrintezzaService.enterTomb` nunca inicia a batalha (`startFight()` só é chamado em comando GM)**: Quando jogadores entram legitimamente pelo NPC `32011` (`NpcBypassPacketHandler.java:316` ou `BypassEventsAndPveHandler.java:267`), `enterTomb` define `status = FrintezzaStatus.ENTRY` e teleporta o grupo, mas nunca agenda nem chama `startFight()`, fazendo com que Frintezza e Scarlet Van Halisha Fase 1 nunca apareçam para jogadores normais!
- **#340.** [`FrintezzaService.java` vs `L2JDreamV2: FrintezzaManager.java` / `L2FrintezzaBossInstance.java` / `L2JLucera2: FrintezzaInstance.java`] **Ausência das 2 salas preliminares (Hall of Alarm / Hall of Dark Choir), dos 4 Retratos Demoníacos (`29050`/`29051`), das 5 melodias do órgão de Frintezza e do `DeleteObject` ao morrer**: `FrintezzaService` teleporta direto para a câmara final sem os portões e salas anteriores, não spawna os 4 Portraits nem os `Evil Spirits (29052/29053)`, Frintezza não toca as músicas/debuffs do órgão (`MagicSkillUse` skills `5007` e `5008`), `onScarletDefeated` (linhas 269–276) remove `activeScarlet` e `frintezzaNpc` sem transmitir `DeleteObject`, e não há task de timeout (`getTimeLimitMillis()`) para expulsar os jogadores.
- **#341.** [`FourSepulchersService.java` vs `L2JDreamV2: FourSepulchersManager.java` / `L2JLucera2: FourSepulchers.java` / `SepulcherActivityRunner.java`] **Inexistência do agendador horário dos Quatro Sepulcros (`ENTRY_TIME`, `WARM_UP`, `ATTACK_TIME`, `COOL_TIME`)**: `FourSepulchersService` inicializa `cycleStatus = SepulcherStatus.ENTRY_TIME` (linha 120), mas não possui nenhum `ScheduledExecutorService` para alternar as fases a cada hora (minuto 55 entrada, minuto 00 aquecimento, minuto 02–50 combate, minuto 50 expulsão via `evictAll()`).
- **#342.** [`FourSepulchersService.java` / `PlayerCombatHandler.java` vs `L2JDreamV2: FourSepulchersMausoleum.java` / `L2SepulcherNpcInstance.java` / `L2JLucera2: SepulcherRoom.java`] **Progressão de salas em `FourSepulchersService` pula caixas (`Mysterious Box` / `Key Box`), portas e o uso da `Chapel Key`**: Quando os 4 monstros da sala morrem, `PlayerCombatHandler` (linha 168) chama `fs.clearRoom(sepId, active)`, que entrega a `CHAPEL_KEY (7260)` e **imediatamente** já avança de estágio e teleporta a party para a próxima sala sem abrir as portas reais da sala e sem usar o método `openNextRoomDoor` (linha 349, que é código morto nunca chamado).
- **#343.** [`FourSepulchersService.java` / `BossManager.java` / `PlayerCombatHandler.java` vs `L2JDreamV2: FourSepulchersMausoleum.java` / `L2JLucera2: SepulcherRaidInstance.java`] **Recompensa do Cálice (`Goblet`) entregue apenas ao líder, ausência de `Victim` / `Halisha's Archon` e dupla chamada em `BossManager` + `PlayerCombatHandler`**: `FourSepulchersService.defeatShadowOfHalisha` (linha 389) entrega apenas 1 Cálice para `leader` (`deliverItem(leader, m.gobletId(), 1)`) em vez de recompensar todos os membros da party presentes no mausoléu, não spawna o `Teleport Cube` de saída e é chamado tanto em `BossManager.onBossKilled` (linha 255) quanto em `PlayerCombatHandler` (linha 162).
- **#344.** [`DimensionalRiftService.java` vs `L2JDreamV2: DimensionalRift.java` / `DimensionalRiftManager.java` / `L2JLucera2: DimensionalRift.java`] **Ausência de agendador de saltos automáticos (`autoAdvanceRoom`) e de isolamento de salas ocupadas no Dimensional Rift**: Embora `DimensionalRiftService` declare `computeAutoJumpDelayMillis` (linha 157) e `autoAdvanceRoom` (linha 242), não há nenhum timer agendado para saltar a party automaticamente a cada `AUTO_JUMPS_DELAY_MIN..MAX`; além disso, `enterRift` e `jumpToNextRoom` não verificam se uma sala (`1..9`) já está ocupada por outra party no mundo `0`, misturando grupos diferentes na mesma sala.
- **#345.** [`DimensionalRiftService.java` / `PlayerCombatHandler.java` / `NpcBypassPacketHandler.java` vs `L2JDreamV2: DimensionalRift.java` / `L2RiftBossInstance.java`] **`DimensionalRiftService.onBossDefeated` nunca é chamado ao matar Anakazel e `manualJump`/`leaveRift` teleportam apenas o líder**: `PlayerCombatHandler` nunca invoca `DimensionalRiftService.onBossDefeated` quando Anakazel (`25333..25338`) morre; além disso, em `NpcBypassPacketHandler.java` (linhas 1700 e 1705), `rift_jump` e `rift_leave` passam `List.of(active())` (apenas quem clicou) em vez dos membros da party na `RiftSession`, deixando o resto do grupo preso na sala anterior!
- **#346.** [`InstanceService.java` / `InstanceWorld.java` vs `L2JDreamV2: InstanceManager.java` / `Instance.java` / `L2JLucera2: ReflectionManager.java` / `Reflection.java`] **`InstanceService` é código morto não utilizado pelas instâncias épicas e não possui task de expiração (`isExpired`)**: Nenhuma classe de produção chama `InstanceService.createInstance`, `enterInstance`, `exitInstance` ou `destroyInstance`, e `InstanceService` não possui agendador periódico que verifique `InstanceWorld.isExpired()` (linha 36 de `InstanceWorld.java`) para destruir instâncias expiradas e expulsar jogadores/NPCs.
- **#347.** [`InstanceService.java` / `GameSession.java` vs `L2JDreamV2: Instance.java` / `L2JLucera2: Reflection.java`] **`InstanceWorld` não suporta portas instanciadas nem recupera o jogador ao relogar/morrer**: `InstanceWorld` armazena apenas IDs de `players` e `npcs`, sem clonar portas (`DoorInstance`) para a instância, e `GameSession` no logout/disconnect ou revive não chama `InstanceService.exitInstance` para restaurar a coordenada de retorno (`playerReturnLocations`) e zerar `instanceId(0)`.
- **#348.** [`RaidBossSpawnManager.java` vs `L2JDreamV2: RaidBossSpawnManager.java` / `L2JLucera2: RaidBossSpawnManager.java`] **Janela de respawn de Raid Bosses comuns ignora configurações por boss e usa tempo fixo de 12h a 24h**: Em `RaidBossSpawnManager.onBossKilled` (linha 184), o tempo de respawn ignora `respawn_min_delay` e `respawn_max_delay` da tabela `raidboss_spawnlist`, aplicando `ThreadLocalRandom.current().nextLong(12 * 3600_000L, 24 * 3600_000L)` hardcoded para todos os Raid Bosses.
- **#349.** [`RaidBossSpawnManager.java` / `GrandBossManager.java` vs `L2JDreamV2: RaidBossSpawnManager.java` / `GrandBossManager.java`] **Persistência no banco (`saveBoss` / `updateStatusInDb`) não salva o HP/MP atual real nem a posição atual dos Bosses vivos no shutdown**: `RaidBossSpawnManager.saveBoss` (linhas 237–258) e `GrandBossManager.updateStatusInDb` (linhas 369–387) gravam sempre `info.maxHp()` / `info.maxMp()` (ou valores constantes) em vez de consultar o `currentHp()` e `currentMp()` da `NpcInstance` viva no `GameWorld` periódica ou preventivamente no `@PreDestroy`.
- **#350.** [`GrandBossManager.java` vs `L2JDreamV2: GrandBossManager.java` / `L2JLucera2: BossRespawnConfig.java`] **Respawn de Grand Bosses em `GrandBossManager.onBossKilled` usa atraso fixo de 24h a 48h ignorando as configs individuais de cada Épico**: `GrandBossManager.onBossKilled` (linha 273) calcula `delay = ThreadLocalRandom.current().nextLong(24 * 3600_000L, 48 * 3600_000L)` para todos os Grand Bosses, ignorando as configurações específicas de `Config.java` para Antharas, Valakas, Baium, Queen Ant, Core, Orfen, Zaken e Sailren.
- **#351.** [`BossManager.java` / `RaidBossSpawnManager.java` / `GrandBossManager.java` vs `L2JDreamV2: RaidBossSpawnManager.java`] **Quando um Raid Boss ou Grand Boss renasce via task agendada (`spawnBoss`), o pacote `NpcInfo` não é transmitido aos jogadores ao redor**: Em `RaidBossSpawnManager.spawnBoss` (linhas 141–169), `GrandBossManager.spawnBoss` (linhas 233–264), `QueenAntService.spawnQueenAnt`, `CoreAndOrfenService.spawnCore/spawnOrfen` e `ZakenService.spawnZaken`, o boss é registrado no `GameWorld`, mas nenhum pacote `NpcInfo` é enviado via `world.broadcastAround`, tornando o boss invisível para jogadores que já estavam aguardando no local até que se afastem e voltem.
- **#352.** [`BossManager.java` vs `L2JDreamV2: L2RaidBossInstance.java` / `L2JLucera2: RaidBossInstance.java`] **Falta de restauração periódica de Minions de Raid Bosses vivos e limpeza de Minions quando o Raid Boss morre**: Em `BossManager` e `NpcAiService`, quando um Raid Boss morre, seus minions (`npc.minions()`) não são despawnados/deletados com `DeleteObject`, e enquanto o Raid Boss está vivo fora de combate, minions mortos não são re-spawnados periodicamente (`maintainMinions` no Dream/Lucera).

---

### Área 4: Zonas, Portas, Barcos, Objetos Estáticos, Tempo e Mundo (#353 a #370)

- **#353.** [`ZoneTable.java` / `GameWorld.java` vs `L2JDreamV2: L2WorldRegion.java` / `ZoneManager.java` / `L2JLucera2: Zone.java`] **Ausência de indexação espacial de zonas por região (`L2WorldRegion`) — varredura O(N) em todas as zonas do servidor a cada consulta**: `ZoneTable.isInsidePeace`, `isInsideArena`, `isInsidePvpCombatZone` e `getZonesAt` (linhas 85–236) percorrem linearmente a lista `allZones` inteira testando `Polygon.contains(x, y)` em todas as zonas do mundo a cada ataque, skill ou movimento, em vez de indexar as zonas nas células/regiões da grade de `GameWorld`.
- **#354.** [`ZoneTable.java` / `ActionPacketHandler.java` vs `L2JDreamV2: L2Zone.java` (`revalidateInZone`) / `L2JLucera2: Zone.java`] **Inexistência de motor de ciclo de vida de zonas (`onEnter` / `onExit` / `revalidateInZone`) durante a movimentação e teleporte**: Ao se mover ou teleportar, o servidor chama apenas `FlagZoneService.updatePlayerZonePosition`, sem revalidar as zonas gerais de `ZoneTable`; por isso, entrar ou sair de zonas de Paz, PvP ou Siege nunca atualiza o radar/bússola (`ExSetCompassZoneCode` / `EtcStatusUpdate`) nem envia as mensagens de sistema `ENTERED_COMBAT_ZONE` / `LEFT_COMBAT_ZONE`.
- **#355.** [`ZoneTable.java` / `Zone.java` vs `L2JDreamV2: L2BossZone.java` / `L2JLucera2: BossZone.java`] **`ZoneType.BOSS` não possui lista de jogadores autorizados (`_playersAllowed`) nem expulsão automática de invasores**: `Zone` (record imutável em `Zone.java`) não rastreia jogadores autorizados pelos NPCs de entrada (Heart of Warding, Klein, Angelic Vortex, Imperial Tomb Guide) nem expulsa jogadores que entram na `BossZone` caminhando, por bug de parede, por Summon Friend ou relogando após o tempo limite (`timeInvade`).
- **#356.** [`ZoneTable.java` / `GameSession.java` vs `L2JDreamV2: L2WaterZone.java` / `L2Character.java` / `L2JLucera2: WaterTask.java`] **`ZoneType.WATER` nunca ativa estado de nado, barra de fôlego (breath gauge) nem dano por afogamento**: Embora `ZoneTable.isInsideWater(x, y, z)` exista (linha 160), ela nunca é chamada na movimentação para alternar a velocidade de nado em `UserInfo`/`CharInfo`, exibir o medidor de oxigênio (`SetupGauge`) ou aplicar dano periódico de afogamento quando o fôlego zera.
- **#357.** [`ZoneTable.java` vs `L2JDreamV2: L2DamageZone.java` / `L2EffectZone.java` / `L2SwampZone.java` / `L2MotherTreeZone.java` / `L2NoLandingZone.java`] **`ZoneType.DAMAGE` é código morto e faltam tipos essenciais de zona retail (`EffectZone`, `SwampZone`, `MotherTreeZone`, `NoLandingZone`, `NoStoreZone`, `NoRestartZone`)**: `ZoneTable.isInsideDamage(x, y, z)` (linha 212) nunca é chamada em nenhum lugar do projeto (zonas de lava/dano ambiental nunca causam dano), e `ZoneTable.parseZoneType` (linha 370) converte zonas `MotherTree`, `Swamp`, `NoLanding` (desmontar Wyvern em cerco), `NoStore` e `NoRestart` em `ZoneType.OTHER` sem efeito algum.
- **#358.** [`ZoneTable.java` / `ZoneShape.java` vs `L2JDreamV2: ZoneCylinder.java` / `ZoneCuboid.java` / `L2JLucera2: Circle.java`] **`ZoneTable.loadXmlFile` descarta zonas cilíndricas (`Cylinder` / `rad`) e `isFishingAllowed` ignora as zonas `ZoneType.FISHING`**: Em `ZoneTable.loadXmlFile` (linhas 324–355), qualquer `<shape>` que não tenha pelo menos 3 `<point>` (como formas `Cylinder` definidas por 1 ponto central e atributo `rad`) é descartada (`poly.npoints >= 3`); além disso, `ZoneTable.isFishingAllowed(int x, int y, int z)` (linha 176) ignora `x, y, z` e retorna apenas `Config.ALLOW_FISHING` sem checar `byType.get(ZoneType.FISHING)`.
- **#359.** [`TimeFarmZoneService.java` / `PlayerCharacter.java` vs `L2JDreamV2: L2TeleporterInstance.java`] **`TimeFarmZoneService` não possui tarefa agendada para `checkExpirations` e usa `player.teleToLocation` que não envia pacote ao cliente**: `TimeFarmZoneService.checkExpirations` (linha 119) nunca é chamado por nenhum scheduler (jogadores nunca são expulsos quando o tempo acaba), e `enterZone`/`exitZone`/`checkExpirations` chamam `player.teleToLocation(x, y, z)` (que em `PlayerCharacter.java` apenas altera `x, y, z` em memória sem chamar `GameWorld.teleportPlayer` para enviar `TeleportToLocation` ao cliente!).
- **#360.** [`DoorInstance.java` / `DoorTable.java` vs `L2JDreamV2: L2DoorInstance.java` / `L2JLucera2: DoorInstance.java`] **`DoorInstance.openDoor()`, `closeDoor()` e `toggleDoor()` não transmitem `DoorStatusUpdate` aos jogadores ao redor**: Quando qualquer serviço chama `door.openDoor()`, `door.closeDoor()` ou `DoorTable.openDoor(id)` / `closeDoor(id)` (como `ZakenService`, ClanHalls, Castelos ou Fortalezas), `DoorInstance` (linhas 102–124) altera o booleano `open` em memória mas nunca envia `DoorStatusUpdate` via `GameWorld.broadcastAround`, mantendo a porta visualmente trancada/aberta no cliente.
- **#361.** [`ActionPacketHandler.java` / `DoorInstance.java` vs `L2JDreamV2: L2DoorInstance.java` / `L2JLucera2: DoorInstance.java`] **Inconsistência entre `door.objectId()` e `door.doorId()` nos pacotes de seleção e combate de portas**: Em `GameSession.updateKnownObjects` (linha 4709), a porta é anunciada ao cliente com `door.objectId()`, mas em `ActionPacketHandler.handleAction` (linhas 403–427) e `handleAttackRequest` (linhas 474–493), `MyTargetSelected`, `StatusUpdate`, `ValidateLocation` e `Attack` são enviados usando `doorOpt.doorId()` no lugar de `doorOpt.objectId()`, quebrando o target visual e a barra de HP da porta no cliente.
- **#362.** [`DoorTable.java` / `DoorInstance.java` vs `L2JDreamV2: L2DoorInstance.java` (`AutoOpenClose`) / `DoorTable.java` / `L2JLucera2: DoorInstance.java`] **Ausência de timer de auto-abertura/auto-fechamento (`autoOpen` / `closeTime`) e de vínculo com `castleId` / `clanHallId` em `DoorInstance`**: `DoorTable.load` (linhas 126–181) não lê tempos de fechamento automático nem associa as portas aos seus respectivos Castelos/ClanHalls/Fortalezas para restringir ataques fora de Siege ou fechar automaticamente portas de ClanHall/masmorras após alguns minutos abertas.
- **#363.** [`ActionPacketHandler.java` / `StaticObjectInstance.java` vs `L2JDreamV2: L2StaticObjectInstance.java` / `L2JLucera2: StaticObjectInstance.java`] **Clique em `StaticObjectInstance` (Mapas de Vila, Placas e Tronos) retorna `ActionFailed` e `canSitOnThrone` é código morto**: `ActionPacketHandler.handleAction` (linhas 380–433) não verifica `ctx.staticObjects().byObjectId(p.objectId())`; clicar em qualquer objeto estático cai direto em `ActionFailed`, nunca enviando `ShowTownMap` para mapas (`TYPE_TOWN_MAP`), `NpcHtmlMessage` para placas (`TYPE_SIGNBOARD`) nem `ChairSit` para tronos de castelo (`StaticObjectInstance.canSitOnThrone` na linha 92 nunca é chamado).
- **#364.** [`GameSession.java` / `StaticObjectInstance.java` vs `L2JDreamV2: StaticObject.java` / `L2JLucera2: StaticObject.java`] **Pacote `StaticObject` (`0x99`) enviado em `GameSession.updateKnownObjects` não diferencia portas estáticas/muros destrutíveis**: `GameSession.updateKnownObjects` (linha 4731) envia apenas `StaticObject(so.staticObjectId(), so.objectId())` para `StaticObjectTable` e `DoorInfo` (`0x4C`) + `DoorStatusUpdate` (`0x4D`) para `DoorTable`, sem suportar a versão completa do pacote `0x99 StaticObject` usada no cliente Interlude para atualizar malhas colidíveis de portas/paredes.
- **#365.** [`BoatService.java` vs `L2JDreamV2: L2BoatInstance.java` / `BoatManager.java` / `L2JLucera2: Boat.java` / `BoatWayEvent.java`] **`BoatService` não possui motor de navegação autônoma por ciclos nem entidades `Boat` visíveis no `GameWorld`**: Os barcos em `BoatService` nunca navegam sozinhos entre os portos em background, nunca aparecem no `GameWorld` (`updateKnownObjects` nunca envia `VehicleInfo` quando um barco se aproxima do porto), nunca anunciam chegadas/partidas pelo `BoatCaptain` e nunca cobram/expulsam clandestinos antes de zarpar.
- **#366.** [`BoatService.java` / `ExtendedProtocolPacketHandler.java` vs `L2JDreamV2: L2BoatInstance.java` / `L2JLucera2: Boat.java`] **`BoatService.boardAndTravel` faz teleporte instantâneo travando no último waypoint, e `handleMoveToLocationInVehicle` move o navio inteiro quando o jogador anda no convés**: `BoatService.advanceWaypoint` (linhas 219–230) incrementa `currentWaypointIndex` sem nunca voltar a `0` após atingir o último ponto (`BoatState.ARRIVED`); além disso, quando um jogador caminha dentro de um barco (`ExtendedProtocolPacketHandler.handleMoveToLocationInVehicle`, linhas 235–249), o handler envia `VehicleDeparture` e `OnVehicleCheckLocation` com as coordenadas locais do jogador, movendo o navio inteiro!
- **#367.** [`GameTimeController.java` vs `L2JDreamV2: GameTimeController.java` / `L2JLucera2: GameTimeController.java`] **`GameTimeController` não transmite os pacotes `SunRise` (`0x1C`) e `SunSet` (`0x1D`) nas transições de dia e noite**: Em `GameTimeController.tick()` (linhas 80–100), quando `currentNight` muda às 00:00 e às 06:00, o controlador transmite apenas um texto `CreatureSay(ANNOUNCEMENT)` mas nunca envia os pacotes `new SunSet()` (ao anoitecer) e `new SunRise()` (ao amanhecer) para mudar a iluminação do céu nos clientes conectados.
- **#368.** [`GameTimeController.java` vs `L2JDreamV2: DayNightSpawnManager.java` (`notifyChangeMode`) / `L2JLucera2: GameTimeController.java`] **Transição Dia/Noite em `GameTimeController` não ativa/desativa o bônus da skill passiva `Shadow Sense` (`294`) dos Dark Elves**: Quando o relógio do servidor alterna entre dia e noite em `GameTimeController.tick()`, os jogadores online que possuem a skill `294` (Shadow Sense) não recebem atualização de precisão noturna (`Accuracy`) nem as mensagens de sistema `SystemMessageId.NIGHT_EFFECT_APPLIES` (`1291`) / `DAY_EFFECT_DISAPPEARS` (`1292`).
- **#369.** [`GameWorld.java` vs `L2JDreamV2: L2WorldRegion.java` / `L2World.java` / `L2JLucera2: World.java` / `WorldRegion.java`] **Jogadores (`playersById`) e Summons (`summonsById`) em `GameWorld` não utilizam a grade espacial 2D (`cellKey`), fazendo varredura linear O(N) em todas as consultas de proximidade**: Enquanto `npcsByCell` usa indexação em células `4096x4096` (`findNpcsAround`), os métodos `findPlayersAround` (linhas 261–275), `broadcastAround` (linhas 293–323) e `findSummonsAround` (linhas 355–369) iteram linearmente sobre **todos** os jogadores e summons conectados no servidor inteiro a cada pacote de movimento, combate, chat ou `updateKnownObjects`.
- **#370.** [`GameWorld.java` / `NpcAiService.java` vs `L2JDreamV2: L2WorldRegion.java` (`setActive`) / `L2JLucera2: WorldRegion.java`] **Ativação/desativação de regiões (`GRID_NEIGHBOR_TURN_ON_TIME` / `TURN_OFF_TIME`) em `GameWorld` é código morto não integrado ao `NpcAiService`**: Os métodos `GameWorld.isGridAlwaysOn()`, `getGridNeighborTurnOnTimeSeconds()`, `getGridNeighborTurnOffTimeSeconds()` e `isCellActive(x, y)` (linhas 472–489) nunca são chamados pelo `NpcAiService` nem possuem controle de estado ativo/inativo por região com atraso de desligamento (`TURN_OFF_TIME`), fazendo o `NpcAiService.tick()` iterar sobre todos os milhares de NPCs de `world.npcs()` a cada 1 segundo mesmo em regiões sem nenhum jogador próximo.

---

# Relatório de Auditoria Técnica — Subagente 5 (Itens #371 a #440)
**Escopo:** Sistema de Clãs, Castelos, Fortalezas, Clan Halls, Seven Signs & Festival of Darkness, Grand Olympiad & Heroes, Motor de Quests e Scripts Retail.
**Comparativo:** `L2JLopez` vs `L2JDreamV2` e `L2JLucera2`.

---

## 1. Sistema de Clãs (#371 a #385)

* **#371 — Omissão de `subpledge`, `power_grade`, `apprentice` e `sponsor` no carregamento de membros do clã no boot (`ClanTable.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanTable.loadClans()` (linha 643), a query SQL executa `SELECT charId, char_name, level, classid, title, clanid FROM characters WHERE clanid > 0` e instancia `new ClanMember(...)` com valores padrão (`pledgeType = 0`, `powerGrade = 5`, `sponsor = 0`, `apprentice = 0`). Todos os membros da Academia (`-1`), Royal Guards (`100/200`) e Orders of Knights (`1001..2002`), bem como seus ranks de privilégio e vínculos de Sponsor/Apprentice, são perdidos a cada reinicialização do servidor.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.L2Clan.restore()` (`L2JDreamV2`) e `l2.gameserver.model.pledge.Clan.restore()` (`L2JLucera2`) carregam `level, char_name, charId, power_grade, subpledge, apprentice, sponsor, sex, race` de `characters` e restauram integralmente `L2ClanMember` / `UnitMember`.
  * **Ação Recomendada:** Incluir `subpledge, power_grade, apprentice, sponsor` no `SELECT` de `ClanTable.loadClans()` e persistir `subpledge` e `power_grade` em `ClanTable.addClanMember()`.

* **#372 — Ausência do pacote e lógica de vinculação/remoção de Sponsor e Apprentice da Academia (`PartyClanPacketHandler.java` / `ClanMember.java`):**
  * **Diagnóstico (`L2JLopez`):** Embora `ClanMember.java` possua os campos `sponsor` e `apprentice`, não existe handler para o pacote `0xD0:0x19` (`RequestSetPledgeAcademyMaster`) em `com.lopez.l2j.network.game.handler.packet.PartyClanPacketHandler` ou `ExtendedProtocolPacketHandler`, nem persistência nas colunas `apprentice` e `sponsor` da tabela `characters`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.network.clientpackets.RequestSetPledgeAcademyMaster` e `L2ClanMember.initApprenticeAndSponsor()` (`L2JDreamV2`) / `l2.gameserver.network.l2.c2s.RequestSetPledgeAcademyMaster` (`L2JLucera2`) validam `CP_CL_APPRENTICE`, vinculam membro principal e acadêmico, persistem no banco e enviam `SystemMessage.S2_HAS_BEEN_DESIGNATED_AS_THE_APPRENTICE_OF_CLAN_MEMBER_S1` e `PledgeShowMemberListUpdate`.
  * **Ação Recomendada:** Implementar o handler de `RequestSetPledgeAcademyMaster` com validação de privilégio `CP_CL_APPRENTICE`, atualização bidirecional em `ClanMember` e persistência SQL em `characters`.

* **#373 — Ausência de graduação automática da Academia ao concluir a 2ª Mudança de Classe (`NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleChangeClass()` (linhas 4845–4955), ao promover o jogador para uma classe de `classTier() == 2` (2ª Job), não há qualquer verificação se o personagem pertence à `Clan.SUBUNIT_ACADEMY (-1)`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.L2Clan.checkAcademy()` invocado em `L2PcInstance.setClassId()` (`L2JDreamV2`) e `l2.gameserver.model.Player.setClassId()` (`L2JLucera2`) verificam se `getPledgeType() == L2Clan.SUBUNIT_ACADEMY` na 2ª troca de classe, concedem de 190 a 400/650 pontos de reputação ao clã (`190 + (40 - lvlJoin) * 10`), entregam o item `Academy Circlet (8181)`, removem o acadêmico sem penalidade de saída e limpam o vínculo do Sponsor.
  * **Ação Recomendada:** Implementar a rotina de graduação da Academia em `handleChangeClass()` (e em `ClassMaster`) ao transicionar para `classTier() == 2`.

* **#374 — Ausência do bônus de EXP de Party entre Sponsor e Apprentice da Academia (`Party.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.party.Party.distributeExpAndSp()`, não há verificação se um membro da Academia e seu respectivo Sponsor estão juntos na mesma party e dentro do raio de alcance para conceder o bônus retail de experiência ao acadêmico.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.L2Party.distributeXpAndSp()` (`L2JDreamV2`) verifica a presença do Sponsor na party e aplica o multiplicador de EXP para o Apprentice.
  * **Ação Recomendada:** Adicionar checagem de `member.sponsor()` presente nos membros em alcance de `Party.distributeExpAndSp()`.

* **#375 — Inelegibilidade de Clan Skills ignorando `minPledgeClass` e cálculo de `pledgeClass` ausente (`ClanSkillService.java` / `ClanMember.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanSkillService.isMemberEligibleForClanSkills()` (linha 409), a única validação feita é `member.pledgeType() != Clan.SUBUNIT_ACADEMY`. Os atributos `minPledgeClass` (Vassal, Heir, Knight, Elder, Baron, Viscount, Count, Marquis, Duke, Grand Duke, Distinguished King) nunca são calculados nem validados por membro (`ClanMember` não possui `calculatePledgeClass()`).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.L2ClanMember.calcPledgeClass(L2PcInstance)` (`L2JDreamV2`) e `l2.gameserver.model.pledge.UnitMember` (`L2JLucera2`) calculam o `pledgeClass` baseado em nível do clã, tipo de sub-unidade, liderança de sub-unidade, liderança do clã e status Noblesse/Hero, aplicando apenas Clan Skills cujo `minPledgeClass <= player.getPledgeClass()`.
  * **Ação Recomendada:** Implementar `calcPledgeClass` em `ClanMember` / `PlayerCharacter`, enviar o valor real em `UserInfo` e validar `minPledgeClass` em `ClanSkillService`.

* **#376 — Clan Skills continuam ativas mesmo quando a reputação do clã é `<= 0` (`ClanSkillService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanSkillService.applyClanSkillsToPlayer()` (linha 247) e `calculateClanSkillBonuses()` (linha 300), os efeitos de todas as Clan Skills aprendidas são aplicados incondicionalmente mesmo quando `clan.reputationScore() <= 0`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.L2Clan.addSkillEffects()` (`L2JDreamV2`) e `l2.gameserver.model.pledge.Clan.broadcastSkillListToOnlineMembers()` (`L2JLucera2`) desativam os efeitos das Clan Skills se `getReputationScore() <= 0` e os reativam assim que a reputação volta a ser positiva.
  * **Ação Recomendada:** Verificar `if (clan.reputationScore() <= 0) return;` em `applyClanSkillsToPlayer()` e `calculateClanSkillBonuses()`, além de recalcular os status dos membros online quando a reputação cruzar o limiar `0`.

* **#377 — Omissão das Clan Skills `384`, `385`, `386`, `387`, `388` e `390` no motor de status (`ClanSkillService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanSkillService.calculateClanSkillBonuses()` (linhas 300–316) e `buildClanSkillStatFuncs()` (linhas 357–374), o `switch (skillId)` contempla apenas as skills `370` a `383`, `389` e `391`, omitindo completamente `384` (Clan Cyclonic Resistance), `385` (Clan Magmatic Resistance), `386` (Clan Fortitude), `387` (Clan Freedom), `388` (Clan Vigilance) e `390` (Clan Luck).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/xml/stats/skills/0300-0399.xml` (`L2JDreamV2`) e `game/data/stats/skills/0300-0399.xml` (`L2JLucera2`) aplicam `windVuln`, `fireVuln`, `stunVuln`, `rootVuln`, `sleepVuln` e redução de perda de EXP em morte (`deathPenalty`) para as skills `384..388` e `390`.
  * **Ação Recomendada:** Incluir os cálculos das skills `384`, `385`, `386`, `387`, `388` e `390` em `ClanSkillService` e delegar para os templates XML de skills.

* **#378 — Contagem indevida de membros da Academia para evolução de Clã aos níveis 6, 7 e 8 (`ClanTable.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanTable.levelUpClan()` (linhas 350–355), a validação de quantidade mínima de membros (`clan.membersCount() < price.minMembers()`) utiliza `clan.membersCount()`, que soma inclusive os membros de `Clan.SUBUNIT_ACADEMY (-1)`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** Em `L2Clan.levelUpClan()` (`L2JDreamV2`) e `VillageMasterPledge` (`L2JLucera2`), membros da Academia não são contabilizados no requisito mínimo de 30, 80 e 120 membros para subir aos níveis 6, 7 e 8.
  * **Ação Recomendada:** Criar método `clan.nonAcademyMembersCount()` filtrando `m.pledgeType() != Clan.SUBUNIT_ACADEMY` e usá-lo em `ClanTable.levelUpClan()`.

* **#379 — Limite de capacidade de membros verificado globalmente em vez de por Sub-Unidade (`ClanTable.java` / `PartyClanPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanTable.addClanMember()` (linha 413), verifica-se apenas o total global `clan.membersCount() >= getMaxMembers(clan.level())` e insere-se o membro sempre com `pledgeType = 0`. Só depois `PartyClanPacketHandler.handleAnswerJoinPledge` altera `member.pledgeType(pledgeType)` em memória, sem respeitar os tetos individuais por sub-unidade (Main Clan: máx 40; Academy: máx 20; Royal Guard 1/2: máx 20 cada; Order of Knights 1..4: máx 10 cada).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2Clan.getMaxNrOfMembers(int pledgeType)` (`L2JDreamV2`) e `Clan.getSubPledgeLimit(int)` (`L2JLucera2`) validam `getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType)` no convite (`RequestJoinPledge`) e na resposta (`RequestAnswerJoinPledge`).
  * **Ação Recomendada:** Implementar `getMaxNrOfMembers(int pledgeType)` e `getSubPledgeMembersCount(int pledgeType)` em `Clan`/`ClanTable` e validar por `pledgeType`.

* **#380 — Criação de Order of Knights sem exigir Royal Guard pai e sem atualizar `subpledge` do Sub-Líder (`ClanPrivilegeService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanPrivilegeService.createSubPledge()` (linhas 188–197), permite-se criar `SUBUNIT_KNIGHT1 (1001)` e `SUBUNIT_KNIGHT2 (1002)` sem que `SUBUNIT_ROYAL1 (100)` tenha sido criada antes (e `1003/1004` sem `SUBUNIT_ROYAL2 (200)`), além de permitir nomear o próprio líder do clã ou líder de outra sub-unidade como capitão.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2Clan.createSubPledge()` (`L2JDreamV2`) e `VillageMasterPledge` (`L2JLucera2`) exigem a existência da Royal Guard correspondente antes de criar suas Orders of Knights e proíbem designar o líder do clã como líder de sub-unidade.
  * **Ação Recomendada:** Validar a hierarquia (`1001/1002` exigem `100`; `2001/2002` exigem `200`) e impedir que o líder do clã ou de outra sub-unidade seja designado em `ClanPrivilegeService.createSubPledge()`.

* **#381 — Dissolução de clã apaga o clã imediatamente em vez de agendar expiração, inutilizando a recuperação (`PartyClanPacketHandler.java` / `NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.PartyClanPacketHandler.dissolveClan()` (linhas 298–326), invoca-se `ctx.clans().dissolveClan(oldClanId)` que deleta o clã imediatamente do banco, em vez de chamar `ClanTable.requestDissolveClan(clan, active)` para agendar `dissolvingExpiryTime` (7 dias). Além disso, em `NpcBypassPacketHandler.handleRecoverClan()` (linha 3289), faz-se `clan.dissolvingExpiryTime(0L)` apenas em memória sem executar `UPDATE clan_data SET dissolving_expiry_time = 0 WHERE clan_id = :cid` e sem cancelar a task agendada.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2VillageMasterInstance.dissolveClan()` e `recoverClan()` (`L2JDreamV2`) / `Clan.setDissolvingExpiryTime()` (`L2JLucera2`) bloqueiam dissolução se o clã possui Castelo/ClanHall, está em Siege ou em Guerra, agendam o prazo em `clan_data.dissolving_expiry_time` e limpam a coluna no banco ao recuperar.
  * **Ação Recomendada:** Alterar o bypass de dissolução para usar `ClanTable.requestDissolveClan()`, validar impedimentos (Castelo, ClanHall, Siege, Clan War) e persistir `dissolving_expiry_time = 0` em `ClanTable.cancelDissolveClan()`.

* **#382 — Transferência diferida de liderança de clã não persiste `new_leader_id` nem agenda execução no servidor (`ClanTable.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanTable.requestLeaderChange()` (linhas 236–265), quando `Config.ALT_CLAN_LEADER_INSTANT_ACTIVATION == false`, define-se `clan.newLeaderId(newLeaderObjectId)` apenas em memória: não há coluna/persistência no banco, não há agendamento de task para `computeNextLeaderChangeExecutionTime()`, e não se valida se o novo líder pertence ao Main Clan (`pledgeType == 0`).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2VillageMasterInstance.changeClanLeader()` (`L2JDreamV2`) exige que o candidato seja membro do Main Clan (`pledgeType == 0`), persiste a solicitação e executa `changeClanLeader()` na manutenção/horário configurado.
  * **Ação Recomendada:** Validar `targetMember.pledgeType() == 0`, persistir `new_leader_id` em `clan_data` e agendar a efetivação em `ClanTable`.

* **#383 — Declaração e encerramento de Clan War não transmitem `RelationChanged`, `UserInfo` nem `SystemMessage` aos membros (`ClanWarService.java` / `PartyClanPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanWarService.declareWar()` (linhas 70–152) e `stopWar()` (linhas 157–215), a guerra é salva em memória/DB, mas os membros online dos dois clãs não recebem `SystemMessage.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP` (`1562`), `SystemMessage.CLAN_S1_DECLARED_WAR` (`1561`), nem pacotes `RelationChanged` / `UserInfo` / `CharInfo` para exibir os ícones de guerra (punhos simples ou cruzados) sobre as cabeças dos jogadores.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `ClanTable.storeclanswars()` e `deleteclanswars()` (`L2JDreamV2`) / `ClanTable.startClanWar()` (`L2JLucera2`) enviam `PledgeShowInfoUpdate`, `SystemMessage` e chamam `broadcastUserInfo()` / `RelationChanged` para todos os membros online de ambos os clãs.
  * **Ação Recomendada:** Ao declarar ou encerrar guerra em `ClanWarService`, transmitir as `SystemMessage` oficiais e atualizar `RelationChanged`/`UserInfo` de todos os membros online de ambos os clãs.

* **#384 — Ganho/perda de reputação em abates de Clan War sem validar reputação positiva da vítima e nível/Academia (`ClanTable.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clan.ClanTable.rewardWarKillReputation()` (linhas 301–311), qualquer abate em guerra transfere reputação mesmo quando `victimClan.reputationScore() <= 0`, quando um dos clãs tem nível `< 5`, ou quando o jogador morto pertence à Academia.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.doDie()` (`L2JDreamV2`) e `Player.onDie()` (`L2JLucera2`) só transferem pontos de reputação em Clan War mútua se ambos os clãs forem nível `>= 5`, o clã da vítima possuir `reputationScore > 0` e a vítima tiver nível `>= 40` fora da Academia.
  * **Ação Recomendada:** Adicionar validação de `attackerClan.level() >= 5 && victimClan.level() >= 5 && victimClan.reputationScore() > 0` e excluir acadêmicos em `rewardWarKillReputation()`.

* **#385 — Impossibilidade de remover Emblemas de Clã/Aliança (`Crest` / `LargeCrest`) enviando pacote vazio (`PartyClanPacketHandler.java` / `CrestCache.java`):**
  * **Diagnóstico (`L2JLopez`):** No protocolo Interlude, quando o líder clica em "Delete Crest", o cliente envia `SetPledgeCrest` ou `ExSetPledgeCrestLarge` com `data.length == 0`. Em `PartyClanPacketHandler.handleSetPledgeCrest()` (linha 217) e `handleExSetPledgeCrestLarge()` (linha 789), `CrestCache.savePledgeCrest()` retorna `0` quando `data.length == 0`, e o handler ignora o retorno `0` (`if (crestId > 0)`), impedindo a remoção do brasão!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.network.clientpackets.SetPledgeCrest` e `ExSetPledgeCrestLarge` (`L2JDreamV2`) verificam `if (length == 0)` para remover o arquivo do `CrestCache`, zerar `crestId = 0` / `crestLargeId = 0` no banco e transmitir `PledgeShowInfoUpdate` e `UserInfo`.
  * **Ação Recomendada:** Tratar `p.data() == null || p.data().length == 0` em `handleSetPledgeCrest`, `handleExSetPledgeCrestLarge` e `handleSetAllianceCrest` removendo o crest no `CrestCache` e atualizando o ID para `0` em `clan_data`.

---

## 2. Castelos, Fortalezas e Clan Halls (#386 a #403)

* **#386 — Confusão de ID entre Siege Headquarters Flag (`35062`) e Holy Artifact em `SiegeService.java` e ausência de Siege Flags:**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.castle.SiegeService` (linha 45), define-se `public static final int HOLY_ARTIFACT_NPC_ID = 35062;`. No cliente Interlude, `35062` é a **Siege Headquarters Flag (`L2SiegeFlagInstance`)**, enquanto os Holy Artifacts reais têm IDs específicos por castelo (`35063`, `35104`, `35146`, `35188`, `35232`, `35278`, `35322`, `35467`, `35514`). Além disso, não há implementação das skills `Build Headquarters (247)` e `Build Advanced Headquarters (326)` para plantar a bandeira do clã atacante, nem respawn dos atacantes na bandeira.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.handler.skill.SiegeFlag`, `L2SiegeFlagInstance` e `Siege.java` (`L2JDreamV2`) / `l2.gameserver.skills.skillclasses.SiegeFlag` (`L2JLucera2`) instanciam a bandeira `35062` vinculada ao `L2SiegeClan` e reduzem o tempo/local de respawn dos atacantes que possuem bandeira viva.
  * **Ação Recomendada:** Corrigir os IDs dos Holy Artifacts por castelo a partir de `siege.properties` e implementar o spawn/gerenciamento de `SiegeFlag (35062)` via skills `247` e `326`.

* **#387 — Skill `Seal of Ruler (246)` desconectada de `SiegeService.midVictory()` e ausência de expulsão dos antigos defensores na troca de turno (`SiegeService.java` / `MagicSkillPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.castle.SiegeService.midVictory()` (linhas 344–398), o método existe mas nunca é acionado ao concluir o conjuramento da skill `Seal of Ruler (246)` sobre um Artefato do Castelo em `MagicSkillPacketHandler`. Além disso, `midVictory()` não teleporta os antigos defensores e atacantes de outros clãs para fora do interior do castelo, nem restaura/fecha os portões para o novo clã defensor.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.handler.skill.TakeCastle` e `com.dream.game.model.entity.siege.Siege.midVictory()` (`L2JDreamV2`) validam se todos os Artefatos do castelo (Aden e Rune exigem 2 artefatos!) foram selados pelo líder atacante, invertem atacantes/defensores, removem bandeiras do novo defensor, fecham portões e teleportam não-aliados para a cidade mais próxima.
  * **Ação Recomendada:** Conectar a finalização de `Skill 246` em `MagicSkillPacketHandler` a `SiegeService.midVictory()`, validar múltiplos artefatos (Aden/Rune) e teleportar não-defensores para fora da zona interna do castelo.

* **#388 — Destruição de Life Control Towers e Flame Control Towers não afeta o respawn dos defensores nem desativa armadilhas de castelo (`SiegeService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.castle.SiegeService.parseControlTower()` e `parseFlameTower()` (linhas 629–684), os dados das torres são lidos de `siege.properties` (inclusive `zoneList` nas Flame Towers, linha 667, que é lida para uma variável local e descartada!), mas quando uma Control Tower ou Flame Tower é destruída em combate, nenhuma lógica é acionada: destruir todas as Life Control Towers não impede a ressurreição nem aumenta o delay de respawn dos defensores, e destruir Flame Towers não desativa as zonas de dano/slow (`CastleTrapZone`).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2ControlTowerInstance.onDeath()` e `L2FlameTowerInstance.onDeath()` (`L2JDreamV2` / `L2JLucera2`) decrementam `_controlTowerCount` em `Siege.java` (bloqueando `RequestRestartPoint` no castelo se `getControlTowerCount() == 0`) e desativam as `L2CastleZone` / `ZoneTemplate` vinculadas à Flame Tower.
  * **Ação Recomendada:** Armazenar `zoneList` em `FlameTowerSpawn`, monitorar a morte das torres em `SiegeService` e bloquear respawn interno/armadilhas quando as respectivas torres forem destruídas.

* **#389 — Upgrades de Portões/Muros (`doors_update`) e Armadilhas (`traps_update`) no Chamberlain são stubs sem efeito (`NpcBypassPacketHandler.java` / `Castle.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleResidenceManagerBypass()` (linha 2485), os comandos `doors_update` e `traps_update` apenas exibem `showNpcHtml(npc, 0)`. `Castle.java` não possui persistência na tabela `castle_doorupgrade` nem restauração de HP reforçado dos portões/muros no início/fim da Siege.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2CastleChamberlainInstance` e `Castle.upgradeDoor(int doorId, int hp, boolean db)` (`L2JDreamV2`) aplicam multiplicadores de HP (200%, 300%, 500%) modulados pelo Selo de Strife (Dawn/Dusk) e persistem em `castle_doorupgrade`.
  * **Ação Recomendada:** Implementar `upgradeDoor` e `upgradeTrap` em `Castle.java` com persistência em `castle_doorupgrade` e tratamento dos bypasses `doors_update` e `traps_update` no Chamberlain.

* **#390 — Taxa de Imposto do Castelo (`tax_set`) ignora os limites do Seal of Strife (Seven Signs) e aplica imediatamente sem aguardar meia-noite (`NpcBypassPacketHandler.java` / `Castle.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleResidenceManagerBypass()` (linhas 3720–3740), `castle.setTaxPercent(tax)` aceita qualquer valor até 15% e o aplica imediatamente.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2CastleChamberlainInstance` e `Castle.setTaxPercent()` (`L2JDreamV2`) limitam a taxa máxima pelo proprietário do Seal of Strife na validação do Seven Signs (`25%` se Dawn controla Strife, `5%` se Dusk controla Strife, `15%` se nenhum controla) e ajustam automaticamente a taxa caso Dusk conquiste Strife.
  * **Ação Recomendada:** Validar o teto de imposto de acordo com `SevenSignsManager.getSealOwner(SEAL_STRIFE)` em `Castle.setTaxPercent()` e reajustar as taxas de todos os castelos quando `strifeOwner` mudar na validação do Seven Signs.

* **#391 — Retirada da Coroa do Lorde (`6841`) permitida durante Siege ativa e não removida do antigo dono ao perder o Castelo (`CastleManager.java` / `CrownService.java` / `NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `NpcBypassPacketHandler.handleGiveCrown()` (linha 3134) e `castle_claim_crown` (linha 1638), o líder do clã pode retirar `Crown of the Lord (6841)` mesmo durante uma Siege em andamento. Além disso, quando o castelo muda de dono em `CastleManager.setOwner()` (linha 229), a coroa `6841` e as tiaras regionais (`6834..6840`, `8182`, `8183`) não são removidas do inventário/banco dos membros do clã que perdeu o castelo.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.manager.CrownManager.checkCrowns()` e `Castle.removeOwner()` (`L2JDreamV2`) bloqueiam a entrega da coroa durante Siege e deletam do inventário e da tabela `items` todas as coroas/circlets inválidas do clã destituído.
  * **Ação Recomendada:** Bloquear `give_crown` se `castle.isSiegeInProgress()` e invocar `CrownService.removeCastleCrownsFromClan(oldOwnerClanId)` em `CastleManager.setOwner()`.

* **#392 — Mapeamento quebrado de Mercenary Tickets para 8 dos 9 Castelos e ausência de validação do Seal of Strife (`MercenaryService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.castle.MercenaryService.resolveNpcIdFromTicket()` (linhas 280–287), apenas os tickets `3960..3975` (Gludio) possuem cálculo `35010 + (ticketItemId - 3960)`, retornando `35010` fixo para todos os tickets de Dion, Giran, Oren, Aden, Innadril, Goddard, Rune e Schuttgart! Também não se verifica o Seal of Strife (Dusk bloqueia contratação de mercenários de elite; Dawn libera mercenários da aliança Dawn).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.manager.MercTicketManager` (`L2JDreamV2`) mapeia a tabela completa de 162 `itemIds` de Mercenary Tickets para seus respectivos `npcIds` (`35010..35560`), limites máximos por tipo e restrições de posicionamento/Seven Signs.
  * **Ação Recomendada:** Substituir `resolveNpcIdFromTicket()` pelo mapeamento completo de tickets dos 9 castelos e validar o estado do Seal of Strife na compra e no drop de tickets.

* **#393 — Wyvern Manager permite montar Wyvern durante Siege ativa ou em estado de combate (`NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleRideWyvern()` (linhas 3832–3896), o líder do clã consegue transformar seu Strider em Wyvern mesmo com o castelo/fortaleza em Siege ativa, ou enquanto o jogador está em combate/flag PvP.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.actor.instance.L2WyvernManagerInstance` (`L2JDreamV2`) bloqueia a montaria se `SiegeManager` indicar Siege ativa, se o jogador estiver em combate, com arma amaldiçoada ou se o Strider estiver abaixo do nível 55.
  * **Ação Recomendada:** Verificar `!castle.isSiegeInProgress()`, `!active.isInCombat()` e `active.pvpFlag() == 0` em `handleRideWyvern()`.

* **#394 — Captura de Fortaleza não atualiza `clan.fortId()` em memória nem a coluna `hasFort` em `clan_data` (`FortressService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.fortress.FortressService.setOwner()` (linhas 119–144), atualiza-se apenas a tabela `fort` (`UPDATE fort SET owner = :owner WHERE id = :id`), mas nunca se atualiza `clan.fortId(fortId)` na instância de `Clan` no `ClanTable`, nem `UPDATE clan_data SET hasFort = :fid` no banco, fazendo com que o clã continue aparecendo com `fortId() == 0` em todo o servidor.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.entity.siege.Fort.setOwner(L2Clan)` (`L2JDreamV2`) limpa `setHasFort(0)` do antigo dono, define `clan.setHasFort(getFortId())` no novo dono, atualiza `clan_data` e envia `PledgeShowInfoUpdate`.
  * **Ação Recomendada:** Sincronizar `Clan.fortId()` e `clan_data.hasFort` em `FortressService.setOwner()` e no boot (`ClanTable.loadClans()`).

* **#395 — Combat Flags de Fortaleza nunca são spawnadas ao derrotar todos os Comandantes e ausência da skill `Raise Flag` (`FortressSiegeService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.fortress.FortressSiegeService.defeatCommander()` (linhas 255–270), matar os Comandantes apenas incrementa um contador em `killedCommandersByFort`, mas `combatFlagSpawnsByFort` (carregado na linha 584) nunca é usado para spawnar as Combat Flags na fortaleza, e não existe mecânica de equipar a Combat Flag no inventário nem conjurar a skill de captura no Flagpole.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.manager.FortSiegeManager`, `FortSiege.commandersAreDead()` e `com.dream.game.handler.skill.TakeFort` (`L2JDreamV2`) spawnam as Combat Flags quando todos os comandantes morrem, equipam a flag reduzindo Speed/AtkSpd e capturam a fortaleza ao finalizar a skill `TakeFort`.
  * **Ação Recomendada:** Invocar o spawn de `combatFlagSpawnsByFort` quando `areAllCommandersDefeated(fortId)` atingir `true` e conectar a captura da fortaleza à skill de hasteamento da bandeira.

* **#396 — Cobrança de aluguel semanal de Clan Hall (`processRentPayment`) nunca é agendada e renova `paidUntil` gratuitamente sem debitar Adena do Clan Warehouse (`ClanHallService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clanhall.ClanHallService.processRentPayment()` (linhas 181–221): (1) não existe nenhum `ScheduledExecutorService` que chame `processRentPayment()` periodicamente; (2) quando chamado, o método **não debita nenhuma Adena do Clan Warehouse** (`ClanWarehouse`) — ele simplesmente estende `paidUntil` por mais 7 dias de graça (`long nextPaidUntil = hall.paidUntil() + RENT_PERIOD_MS`)!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.entity.ClanHall.FeeTask` (`L2JDreamV2`) e `ClanHallRentalFeeTask` (`L2JLucera2`) verificam `clan.getWarehouse().getAdena() >= getLease()`, debitam `getLease()` do Clan Warehouse, ou entram em período de carência de 7 dias e retomam o Clan Hall para leilão (`AuctionManager`) caso o saldo seja insuficiente.
  * **Ação Recomendada:** Agendar uma task horária em `ClanHallService` que verifique `paidUntil <= now`, debite `hall.lease()` do `ClanWarehouse` do clã proprietário e coloque o Clan Hall em leilão após o vencimento da carência.

* **#397 — Custos das funções de Clan Hall (Teleporte, Cortinas, Buffs, Regen HP/MP/EXP) nunca são debitados do Clan Warehouse (`ClanHallFunctionService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clanhall.ClanHallFunctionService.activateFunction()` (linhas 75–123), a função é gravada em `clanhall_functions` com `endTime`, mas nenhuma Adena (`fee`) é debitada na ativação (nem do inventário nem do Clan Warehouse), e não há timer de expiração/renovação para cobrar ou remover funções cujo `endTime` venceu.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `ClanHall.ClanHallFunction.FunctionTask` (`L2JDreamV2`) cobra a taxa inicial na ativação e agenda a renovação automática debitando `getRate()` do Clan Warehouse ou removendo a função da tabela `clanhall_functions` se não houver Adena.
  * **Ação Recomendada:** Debitar `fee` na ativação em `ClanHallFunctionService.activateFunction()` e agendar verificação periódica de `endTime` contra o `ClanWarehouse`.

* **#398 — Leilão de Clan Hall (`bidOnAuction`) transfere a propriedade imediatamente no primeiro lance e debita do inventário pessoal em vez do Clan Warehouse (`ClanHallService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.clanhall.ClanHallService.bidOnAuction()` (linhas 226–288): (1) a Adena do lance é consumida de `leader.inventory()` em vez do `ClanWarehouse`; (2) o lance anterior do líder anterior nunca é estornado ao seu Clan Warehouse; (3) na linha 268, chama-se `setOwner(hallId, clan.clanId())` **imediatamente no momento do lance**, encerrando o leilão na hora em vez de aguardar a data de término do leilão (`endDate`)!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.entity.Auction.setBid()` e `AutoEndTask` (`L2JDreamV2`) debitam a Adena do `ClanWarehouse`, devolvem o lance anterior ao `ClanWarehouse` do antigo maior licitante, gravam em `auction_bid` e só transferem a posse do Clan Hall quando `endDate` expira.
  * **Ação Recomendada:** Reescrever `bidOnAuction()` para persistir lances em `auction_bid`, debitar/estornar via `ClanWarehouse` e transferir a posse apenas no `AuctionEndTask`.

* **#399 — Handler do NPC Auctioneer (`handleAuctionAction`) usa `int hallId = 1` hardcoded para todos os lances (`NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleAuctionAction()` (linha 3905), ao processar `bid`, o código define `int hallId = 1;` fixo, fazendo com que qualquer lance em qualquer cidade seja direcionado sempre para o Clan Hall ID `1`, além de não listar os Clan Halls disponíveis por localidade (`location`).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.actor.instance.L2AuctioneerInstance` (`L2JDreamV2`) extrai o `auctionId` selecionado pelo jogador nos parâmetros do bypass e filtra a lista de leilões pela cidade do Auctioneer.
  * **Ação Recomendada:** Passar e ler o `hallId` real nos bypasses do Auctioneer (`Auction_list`, `Auction_selected`, `Auction_bid`, `Auction_cancelBid`).

* **#400 — Ausência das mecânicas das 6 Conquerable Clan Halls (`Bandit Stronghold`, `Wild Beast Reserve`, `Devastated Castle`, `Fortress of Resistance`, `Rainbow Springs Chateau`, `Fortress of the Dead`) (`ClanHallSiegeService.java`):**
  * **Diagnóstico (`L2JLopez`):** `com.lopez.l2j.game.clanhall.ClanHallSiegeService` implementa apenas uma estrutura genérica de registro (`clanhall_siege_clans`) sem nenhuma das mecânicas específicas das 6 Clan Halls conquistáveis:
    1. **Fortress of Resistance (ID 21):** captura por maior dano acumulado no `Bloody Lord Nurka (35368)`;
    2. **Devastated Castle (ID 34):** captura por maior dano em `Gustav (35410)` após derrotar `Dietrich` e `Mikhail`;
    3. **Bandit Stronghold (ID 35) & Wild Beast Reserve (ID 63):** Quest `504` / `655`, escolha de 1 de 5 NPCs aliados, batalha preliminar em 5 tendas (máx 5 clãs) e final contra o comandante;
    4. **Rainbow Springs Chateau (ID 62):** inscrição por `Rainbow Springs Clan Hall War Decree (8034)` (pesca), minigame de soletrar palavras com `Hot Springs Yeti` e destruição das cabaças (`Gourds`);
    5. **Fortress of the Dead (ID 64):** conquista matando `Lidia von Hellmann (35629)`, `Alfred` e `Giselle`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.manager.clanhallsiege.BanditStrongholdSiege`, `WildBeastFarmSiege`, `DevastatedCastleSiege`, `FortResistSiegeManager`, `RainbowSpringSiege` e `FortressOfDeadSiege` (`L2JDreamV2`).
  * **Ação Recomendada:** Implementar os controladores específicos de combate/pontuação para as Clan Halls `21`, `34`, `35`, `62`, `63` e `64` integrados ao `ClanHallSiegeService`.

* **#401 — Quests `504` (Competition for the Bandit Stronghold) e `655` (A Grand Plan for Taming Wild Beasts) não inscrevem o clã em `ClanHallSiegeService` (`Quest504` / `Quest655`):**
  * **Diagnóstico (`L2JLopez`):** As classes `Quest504CompetitionForTheBanditStronghold.java` e `Quest655AGrandPlanForTamingWildBeasts.java` concedem o certificado/pedra, mas não integram a entrega do item com o registro do clã em `ClanHallSiegeService` para os Clan Halls `35` e `63`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `BanditStrongholdSiege.java` e `WildBeastFarmSiege.java` (`L2JDreamV2`) validam a posse do certificado/item da quest durante o período de registro de 1 hora antes da Siege.
  * **Ação Recomendada:** Integrar os certificados das Quests `504` e `655` na validação de inscrição de `ClanHallSiegeService.registerAttacker()`.

* **#402 — Portões de Clan Hall e Castelo não validam posse do clã no comando `open_doors` / `close_doors` dos Doormen (`NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleBypass()` (linhas 1603–1635), ao processar `open_doors` e `close_doors`, o código abre/fecha portas próximas por distância ou lista de IDs sem verificar se o jogador pertence ao clã proprietário do Castelo/ClanHall/Fortaleza correspondente, se possui o privilégio `CP_CS_OPEN_DOOR` / `CP_CH_OPEN_DOOR`, ou se há uma Siege ativa (durante Siege, Doormen internos não abrem portões externos se não permitido).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2DoormenInstance` e `L2ClanHallDoormenInstance` (`L2JDreamV2`) exigem que `player.getClanId() == residence.getOwnerId()` e validam `L2Clan.CP_CS_OPEN_DOOR` / `L2Clan.CP_CH_OPEN_DOOR`.
  * **Ação Recomendada:** Validar vínculo de clã proprietário e privilégio `CP_CS_OPEN_DOOR` / `CP_CH_OPEN_DOOR` antes de executar `open_doors` e `close_doors`.

* **#403 — Abates dentro de Zonas de Siege e Olympiad aplicam PK Kills e Karma caso a vítima não esteja com `pvpFlag > 0` (`PlayerCombatHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.PlayerCombatHandler.handlePlayerKill()` (linhas 287–316), `karmaFreeEventOrWar` verifica apenas `inTownWar || inFlagZone || inGvE`. Se um jogador matar outro dentro de uma zona de Siege ativa (`SiegeService` / `FortressSiegeService` / `ClanHallSiegeService`), em Clan War mútua ou na Olympiad sem `pvpFlag > 0`, o atacante recebe **+1 PK Kill e +2000 Karma**!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.onKillUpdatePvPKarma()` (`L2JDreamV2`) isenta de PK/Karma abates ocorridos dentro de `L2SiegeZone` ativa (entre participantes), em Clan War mútua e em Olympiad.
  * **Ação Recomendada:** Incluir `attacker.isOlympiadMode()`, participação em Siege ativa e Clan War mútua na flag `karmaFreeEventOrWar` em `PlayerCombatHandler.handlePlayerKill()`.

---

## 3. Seven Signs e Festival of Darkness (#404 a #414)

* **#404 — Cálculo de posse dos Selos (`calcNewSealOwner`) ignora qual cabala venceu a competição do Seven Signs (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.calcNewSealOwner()` (linhas 707–755), determina-se o dono de Avarice, Gnosis e Strife comparando apenas a proporção interna de membros (`dawnPct >= 35` vs `duskPct >= 35`) e a contagem nominal de membros registrados no selo (`dawnScore >= duskScore`), **ignorando completamente qual cabala venceu a competição geral (`getHigherCabal()` / `previousWinner`)**! Assim, a cabala derrotada pode capturar os selos durante o período de validação.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.entity.sevensigns.SevenSigns.calcNewSealOwners()` (`L2JDreamV2`) e `l2.gameserver.model.entity.SevenSigns.calcNewSealOwners()` (`L2JLucera2`) garantem que apenas a cabala vencedora do ciclo (`getCabalHighestScore()`) pode conquistar (`>= 35%`) ou manter (`>= 10%`) o controle de um selo no período de validação.
  * **Ação Recomendada:** Restringir a posse dos selos em `calcNewSealOwner()` exclusivamente à cabala vencedora (`previousWinner`), exigindo `>= 35%` de votos da cabala vencedora para capturar um selo novo (ou `>= 10%` se já era dona do selo no ciclo anterior).

* **#405 — `recalculateSealTotals()` contabiliza apenas 1 ponto por jogador registrado em vez de somar os pontos de contribuição de Seal Stones (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.recalculateSealTotals()` (linhas 1208–1231), `avariceDawnScore`, `gnosisDawnScore` e `strifeDawnScore` são incrementados em `+1` por jogador (`avariceDawnScore++`), desprezando o `contributionScore` de pedras entregues e sobrescrevendo os valores acumulados.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSigns.java` (`L2JDreamV2` / `L2JLucera2`) contabiliza os votos/contribuições para o cálculo proporcional de cada selo dentro da cabala.
  * **Ação Recomendada:** Alinhar o cálculo de pontuação por selo em `SevenSignsManager` com as regras retail e evitar zerar os scores persistidos.

* **#406 — `resetForNewCycle()` não limpa `avariceOwner`, `gnosisOwner` e `strifeOwner` ao iniciar o período de recrutamento/competição (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.resetForNewCycle()` (linhas 1184–1206), ao encerrar `PERIOD_SEAL_VALIDATION` e entrar em `PERIOD_COMP_RECRUITING`, os campos `avariceOwner`, `gnosisOwner` e `strifeOwner` permanecem com os vencedores da semana anterior durante toda a semana de competição (afetando `getSealOwner(seal)` em `Quest2211HuntingGroundsTeleport` e taxas de castelo).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSigns.resetSeals()` (`L2JDreamV2`) salva os antigos donos para referência de retenção (regra dos 10%) e desativa os efeitos ativos de validação durante o período de competição.
  * **Ação Recomendada:** Separar `previousSealOwner` de `activeSealOwner` ou condicionar `getSealOwner()` a retornar `CABAL_NULL` fora de `PERIOD_SEAL_VALIDATION`.

* **#407 — Festival of Darkness (`handleFestivalAction` case 2) apenas teleporta a party para a sala vazia sem instanciar ondas de monstros, baús, arqueiros ou timer de 18 minutos (`NpcBypassPacketHandler.java` / `SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleFestivalAction()` (`case 2`, linhas 4215–4271), o servidor consome as Seal Stones do líder, teleporta a party para `FESTIVAL_DUSK_COORDS` / `FESTIVAL_DAWN_COORDS` e envia `"Boa sorte no Festival of Darkness!"`. Não existe nenhuma classe `SevenSignsFestival` / `L2DarknessFestival`: nenhum monstro (`18009..18108`), arqueiro ou baú do festival nasce na sala, não há drop de `Blood Offering (5901)`, não há progressão de ondas e não há timer de 18 minutos para encerrar e retornar a party ao Oracle.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.entity.sevensigns.SevenSignsFestival` e `L2DarknessFestival` (`L2JDreamV2`) / `l2.gameserver.model.entity.SevenSignsFestival.DarknessFestival` (`L2JLucera2`) gerenciam o ciclo de 18 minutos, spawns de monstros por faixa de nível (`<32, <43, <54, <65, No Limit`), drop de `Blood Offering (5901)` no líder da party, opção de aumentar a dificuldade e teleporte de saída no término.
  * **Ação Recomendada:** Implementar o serviço `SevenSignsFestivalService` gerenciando as instâncias das 10 salas (5 Dawn / 5 Dusk), spawns de monstros/baús, drop de `Blood Offering (5901)` e timer de encerramento.

* **#408 — Entrega de `Blood Offerings` no Festival (`case 4`) registra apenas o nome do líder em vez da party e `rewardHighestRanked()` nunca entrega Ancient Adena (`SevenSignsManager.java` / `NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** (1) Em `NpcBypassPacketHandler.handleFestivalAction()` (`case 4`, linha 4294), chama-se `ss.addFestivalScore(cabal, festivalType, score, List.of(active.name()))`, omitindo todos os demais membros da party; (2) Em `SevenSignsManager.rewardHighestRanked()` (linhas 988–1006), o método apenas faz `log.info(...)` e zera `accumulatedBonuses[i] = 0` sem entregar Ancient Adena a ninguém; (3) No `case 9` de `handleFestivalAction` (linhas 4334–4337), qualquer jogador que clique em "Receive Bonus" recebe **100% do `accumulatedBonus` daquele tier** sem verificar se está em `PERIOD_SEAL_VALIDATION` ou se seu nome consta em `getHighestScoreMembers()`!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSignsFestival.java` (`L2JDreamV2`) grava os nomes de todos os membros da party no `seven_signs_festival` e só permite resgatar a fração individual do bônus durante `PERIOD_SEAL_VALIDATION` para membros listados no recorde vencedor que ainda não sacaram sua parte.
  * **Ação Recomendada:** Registrar todos os membros da party em `addFestivalScore()`, remover o zeramento precoce em `rewardHighestRanked()` e validar `isSealValidationPeriod()`, pertencimento a `getHighestScoreMembers()` e controle de resgate individual no `case 9`.

* **#409 — `Blacksmith of Mammon (31126)` e `Merchant of Mammon (31113)` nunca são spawnados se não existirem no mundo e permanecem ativos fora do período de validação (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.relocateMammons()` (linhas 842–870), o código percorre `world.npcs()` procurando um NPC já existente com `npcId == 31126` ou `31113` para mover suas coordenadas. Se eles não estiverem spawnados no mundo, `relocateMammons()` nunca os cria (`new NpcInstance`), e quando `PERIOD_SEAL_VALIDATION` termina (ou o selo Gnosis/Avarice não foi conquistado), eles nunca são removidos do mundo.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `AutoSpawnHandler` e `SevenSigns.spawnSevenSignsNPC()` (`L2JDreamV2` / `L2JLucera2`) spawnam `Merchant of Mammon` somente durante `PERIOD_SEAL_VALIDATION` quando `avariceOwner == winningCabal`, spawnam `Blacksmith of Mammon` somente quando `gnosisOwner == winningCabal`, anunciam a localização via Priests e fazem despawn ao término da validação.
  * **Ação Recomendada:** Criar/remover dinamicamente as instâncias de `31113` e `31126` em `SevenSignsManager` conforme `isMammonMerchantActive()` e `isMammonBlacksmithActive()`.

* **#410 — Ausência de buffs/debuffs periódicos de `Orator of Revelations (31094)` e `Preacher of Doom (31093)` nas cidades durante a validação do Seal of Gnosis (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Não há qualquer implementação do comportamento retail dos NPCs `Orator of Revelations (31094)` e `Preacher of Doom (31093)` durante `PERIOD_SEAL_VALIDATION`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSigns.spawnSevenSignsNPC()` e a AI de `Orator of Revelations` / `Preacher of Doom` (`L2JDreamV2` / `L2JLucera2`) conjuram periodicamente `Fighter's Will / Magician's Will` (`4364` / `4365`) nos jogadores da cabala vencedora e `Curse of Doom / Curse of Abyss` (`4361` / `4362`) nos jogadores da cabala perdedora próximos nas cidades quando o Seal of Gnosis está ativo.
  * **Ação Recomendada:** Implementar task periódica em `SevenSignsManager` (ou `NpcAiService`) para que `31094` e `31093` apliquem os buffs/debuffs oficiais aos jogadores das cabalas vencedora/perdedora durante a validação do Seal of Gnosis.

* **#411 — Modificador de CP do Seal of Strife (`applyStrifeCpModifiers`) altera `maxCp` diretamente em memória em vez de conceder as skills passivas `5074` / `5075` (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.applyStrifeCpModifiers()` (linhas 1058–1098), multiplica-se `ch.maxCp()` diretamente nos jogadores online no instante da virada de período. Esse valor é sobrescrito no primeiro `recalculateStats()` (ao equipar um item ou receber buff) e não concede as skills oficiais na janela de skills.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSigns.java` e `L2PcInstance.onPlayerEnter()` (`L2JDreamV2`) concedem a skill `The Victor of War (5074)` (+10% Max CP) aos membros da cabala vencedora do Seal of Strife e `The Vanquished of War (5075)` (-10% Max CP) aos membros da cabala perdedora.
  * **Ação Recomendada:** Conceder/remover as skills `5074` e `5075` em `SevenSignsManager` e no login (`GameSession.onEnterWorld`).

* **#412 — Expulsão de Catacumbas e Necrópoles (`teleLosingCabalFromDungeons`) só envia o pacote `TeleportToLocation` sem atualizar `x, y, z` do jogador no servidor e não valida posse do selo (`SevenSignsManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.sevensigns.SevenSignsManager.teleLosingCabalFromDungeons()` (linhas 1144–1159): (1) chama-se apenas `p.send(new GameServerPacket.TeleportToLocation(p.objectId(), 83400, 147943, -3404))` sem atualizar `p.character().x/y/z` nem `world.updatePlayerPosition()`, deixando o jogador fisicamente dentro da dungeon no servidor; (2) não se verifica se a cabala vencedora realmente conquistou o selo exigido (`SEAL_AVARICE` para Necrópoles, `SEAL_GNOSIS` para Catacumbas).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SevenSigns.teleLosingCabalFromDungeons()` (`L2JDreamV2`) verifica `checkIsInSealValidationPeriod()` e posse do respectivo selo, executando `player.teleToLocation(MapRegionTable.TeleportWhereType.Town)` completo no servidor.
  * **Ação Recomendada:** Invocar `session.teleportToLocation(...)` para atualizar a posição no `GameWorld` e verificar se o selo correspondente (`SEAL_AVARICE` / `SEAL_GNOSIS`) foi conquistado pela cabala vencedora.

* **#413 — Quest `1103_OracleTeleport` usa `PlayerCharacter.teleToLocation()` que altera apenas variáveis em memória sem enviar `TeleportToLocation` ao cliente (`Quest1103OracleTeleport.java` / `PlayerCharacter.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest1103OracleTeleport.onTalk()` (linhas 81, 96 e 112), chama-se `player.teleToLocation(aEx)`. Em `PlayerCharacter.java` (linhas 281–291), `teleToLocation()` apenas atribui `this.x = x; this.y = y; this.z = z;` em memória sem enviar o pacote `TeleportToLocation` ao cliente nem atualizar a região no `GameWorld`, fazendo com que a tela do jogador nunca teleporte para o Oracle of Dawn/Dusk!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/scripts/teleports/1103_OracleTeleport/__init__.py` (`L2JDreamV2`) chama `player.teleToLocation()` que atualiza a região do mundo e envia `TeleportToLocation` ao cliente.
  * **Ação Recomendada:** Em `Quest1103OracleTeleport` (e `Quest635InTheDimensionalRift`), usar `qs.getPlayer().teleportToLocation(x, y, z)` para enviar o pacote e atualizar o `GameWorld`.

* **#414 — Quest `2211_HuntingGroundsTeleport` verifica apenas `SEAL_GNOSIS` tanto para Catacumbas quanto para Necrópoles (`Quest2211HuntingGroundsTeleport.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest2211HuntingGroundsTeleport.onTalk()` (linhas 63–71), o desconto/acesso `low_*.htm` verifica exclusivamente `SEAL_GNOSIS` (`sealOwnerGnosis == playerCabal && playerSeal == SEAL_GNOSIS`), ignorando `SEAL_AVARICE` (Necrópoles).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/scripts/teleports/2211_HuntingGroundsTeleport/__init__.py` (`L2JDreamV2`).
  * **Ação Recomendada:** Considerar também `SEAL_AVARICE` na verificação de elegibilidade de teleporte durante `PERIOD_SEAL_VALIDATION`.

---

## 4. Grand Olympiad e Sistema de Heróis (#415 a #427)

* **#415 — Partidas da Olympiad nunca terminam: `OlympiadGameService.finishMatch()` e `OlympiadParticipant.addDamage()` nunca são chamados em lugar algum do código (`OlympiadGameService.java` / `CombatService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadGameService`: (1) `prepareAndStartMatch()` (linha 80) cria a partida em `activeMatches`, mas não agenda nenhum timer para verificar `match.isBattleTimeExpired()` nem chama `finishMatch(match)` (linha 492); (2) `OlympiadParticipant.addDamage(double)` (linha 99) **nunca é chamado** em `CombatService` nem em `MagicSkillPacketHandler`; (3) os competidores ficam presos eternamente dentro do estádio da Olympiad!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.olympiad.OlympiadGame` e `OlympiadGameTask` (`L2JDreamV2`) / `l2.gameserver.model.entity.olympiad.OlympiadGameTask` (`L2JLucera2`) executam uma máquina de estados temporizada (contagem regressiva de teleporte -> 60s de preparação no estádio -> 6 minutos de luta -> avaliação de vencedor -> retorno à cidade) e registram todo dano causado via `addDamage()`.
  * **Ação Recomendada:** Agendar a task de ciclo de vida da partida em `OlympiadGameService.prepareAndStartMatch()`, registrar o dano causado nos participantes em `CombatService`/`MagicSkillPacketHandler` e invocar `finishMatch()` imediatamente quando um lado for derrotado ou o tempo expirar.

* **#416 — Jogador morre de verdade (`Die` packet com botão "To Village") dentro da Olympiad em vez de ter o HP travado em `1` e encerrar a luta (`MagicSkillPacketHandler.java` / `PlayerCombatHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** No Lineage II Interlude retail, um competidor na Olympiad nunca morre de verdade (`doDie`): quando um golpe reduziria seu HP a `<= 0`, o HP é travado em `1` e a partida é encerrada imediatamente dando vitória ao oponente. Em `L2JLopez`, o jogador morre (`currentHp = 0`), recebe o pacote `Die` com opção de clicar em "To Village" e perde buffs de Noblesse.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.actor.status.PcStatus.reduceHp()` (`L2JDreamV2`) e `l2.gameserver.model.Player.onReduceCurrentHp()` (`L2JLucera2`) interceptam dano letal quando `isInOlympiadMode()`, fixam `setCurrentHp(1)`, marcam o participante como derrotado (`olympiadGame.haveWinner()`) e finalizam a partida sem matar o personagem.
  * **Ação Recomendada:** Interceptar dano letal quando `target.isOlympiadMode()` em `CombatService` e `MagicSkillPacketHandler`, mantendo `currentHp(1)` e acionando `OlympiadGameService.onParticipantDefeated()`.

* **#417 — Ausência de contagem regressiva de 120s antes do teleporte e de fase de preparação de 60s com portas fechadas/paz no estádio (`OlympiadGameService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadGameService.prepareAndStartMatch()` (linhas 80–95), a partida muda de `PORTING` diretamente para `FIGHTING` no mesmo milissegundo em que teleporta os jogadores, sem os avisos de `120, 60, 30, 15, 5, 4, 3, 2, 1` segundos na cidade (`SystemMessage.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S`) e sem os 60 segundos de preparação dentro da arena antes do início do combate.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `OlympiadGameTask.teleportCountdown()` e `run()` (`L2JDreamV2` / `L2JLucera2`) executam a contagem de 120s na cidade, teleportam para o estádio em estado de paz (`isOlympiadStart() == false`) por 60s com avisos de `60, 50, 40, 30, 20, 10, 5..1` segundos e só então liberam `THE_GAME_HAS_STARTED`.
  * **Ação Recomendada:** Implementar as fases temporizadas `PORTING` (contagem na cidade) e `PREPARATION` (60s sem permitir ataques no estádio) em `OlympiadGameService`.

* **#418 — `preparePlayerForArena()` não remove buffs externos, não remove Pets, não desequipa Hero Weapons/Cursed Weapons e não envia `ExOlympiadMode` (`OlympiadGameService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadGameService.preparePlayerForArena()` (linhas 254–322): (1) os buffs ativos do jogador (`ch.effects()`) **não são removidos** ao entrar na arena, permitindo entrar full-buffado de NPC Buffer/Prophecy/Dances/Songs/Malaria; (2) Pets (`SummonInstance` tipo Pet) não são recolhidos e buffs de Servitors não são limpos; (3) Clan Skills e Hero Skills não são suspensas; (4) itens proibidos equipados (Hero Weapons `6611..6621`) não são desequipados; (5) os pacotes `ExOlympiadMode(1/2)` e `ExOlympiadUserInfo` não são enviados aos lutadores.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `OlympiadGame.removals()` (`L2JDreamV2`) chama `stopAllEffects()`, remove Clan/Hero skills, recolhe Pets, limpa buffs de Summons, desequipa itens restritos (`checkWeaponRestriction`), remove o jogador de qualquer Party e envia `ExOlympiadMode`.
  * **Ação Recomendada:** Limpar todos os efeitos ativos em `preparePlayerForArena()`, remover da Party, recolher Pets, desequipar itens proibidos em `Config.LIST_OLY_RESTRICTED_ITEMS` e enviar `ExOlympiadMode` e `ExOlympiadUserInfo`.

* **#419 — Desconexão/Logout durante contagem ou luta na Olympiad não aplica derrota por W.O. nem deduz pontos (`OlympiadGameService.java` / `GameSession.java`):**
  * **Diagnóstico (`L2JLopez`):** Não há gancho em `GameSession.onDisconnect()` chamando `OlympiadGameService`: se um jogador fechar o cliente ou deslogar ao ver um adversário desfavorável na contagem ou durante a luta, a partida não aplica a penalidade de abandono (`1/3` ou `1/5` dos pontos) e o oponente fica preso na arena.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `OlympiadGame.handleDisconnect(L2PcInstance)` (`L2JDreamV2`) e `OlympiadGame.logoutPlayer(Player)` (`L2JLucera2`) declaram derrota imediata por desistência (`SystemMessage.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS`), transferem os pontos ao oponente e restauram as coordenadas originais do desconectado.
  * **Ação Recomendada:** Adicionar `OlympiadGameService.onPlayerDisconnect(session)` no encerramento da `GameSession` para remover da fila ou encerrar a partida ativa por W.O.

* **#420 — Fórmula de transferência de pontos em `CLASS_FREE` usa divisor `/ 3` em vez do divisor retail `/ 5` (`OlympiadManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadManager.calculatePointTransfer()` (linhas 410–420), utiliza-se `int transfer = Math.max(1, minPoints / 3);` tanto para `CLASS_BASED` quanto para `CLASS_FREE`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `OlympiadGame.validateWinner()` (`L2JDreamV2`) transfere `min(pointOne, pointTwo) / 3` em lutas Class-Based (`CLASSED`) e `min(pointOne, pointTwo) / 5` em lutas Class-Free (`NON_CLASSED`).
  * **Ação Recomendada:** Usar divisor `5` quando `mode == OlympiadMode.CLASS_FREE` e divisor `3` quando `mode == OlympiadMode.CLASS_BASED` em `OlympiadManager.calculatePointTransfer()`.

* **#421 — Ciclo mensal/semanal da Olympiad e eleição de Heróis (`finishCycleAndElectHeroes`) nunca são agendados e não exigem ao menos 1 vitória (`OlympiadManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadManager`: (1) `startScheduledCycle()` (linha 62) agenda apenas a janela diária das 18h às 00h, mas **nunca agenda o fim do ciclo mensal/período de validação de 24h nem a entrega semanal de +3 pontos**; (2) `finishCycleAndElectHeroes()` (linha 430) verifica apenas `if (n.competitionsDone() < 9 || n.points() <= 0)`, sem exigir ao menos **1 vitória (`n.competitionsWon() >= 1`)**; (3) `finishCycleAndElectHeroes()` não persiste os novos heróis em `HeroService` (`character_herolist` / `heroes`), não remove o status/armas dos heróis do ciclo anterior e não reseta `olympiad_nobles` para o novo ciclo.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.olympiad.Olympiad.setNewOlympiadEnd()`, `addWeeklyPoints()` e `com.dream.game.model.entity.Hero.computeNewHeroes()` (`L2JDreamV2`) exigem `competitions_done >= 9 AND competitions_won >= 1`, removem Hero Weapons/Circlets dos ex-heróis, concedem 1000 pontos de reputação ao clã de cada novo Herói e reiniciam a tabela de nobres para o novo mês.
  * **Ação Recomendada:** Agendar o fim do ciclo mensal e o bônus semanal em `OlympiadManager`, exigir `n.competitionsWon() >= 1` em `finishCycleAndElectHeroes()`, integrar com `HeroService` e limpar armas heroicas do ciclo anterior.

* **#422 — Modo Espectador da Olympiad (`handleOlympiadObserverEnd` e `handleOlympiadMatchList`) deixa o jogador preso invisível no estádio e não lista as arenas (`ExtendedProtocolPacketHandler.java` / `NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Quando um jogador entra como espectador em `NpcBypassPacketHandler.handleOlympiadObserve()` (linha 4606), chama-se `active.enterObserverMode(x, y, z)` (que salva `obsOrigX/Y/Z` em `PlayerCharacter`). Porém, ao clicar em sair (`RequestOlympiadObserverEnd`), `ExtendedProtocolPacketHandler.handleOlympiadObserverEnd()` (linha 1116) lê `this.observing` (que é `false`!), **nunca chama `active.exitObserverMode()`**, não envia `ExOlympiadMode(0)` e não teleporta o jogador de volta para `active.obsOrigX()/Y()/Z()`, deixando-o preso invisível na arena! Além disso, `handleOlympiadMatchList()` (linha 1121) envia os dados do próprio espectador em `ExOlympiadUserInfoSpectator` e um HTML estático `"<html><body>Olympiad Matches</body></html>"`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.leaveOlympiadObserverMode()` e `RequestOlympiadMatchList` (`L2JDreamV2`) chamam `ExOlympiadMode(0)`, restauram a visibilidade e teleportam para `getSavedLocation()`, enquanto `RequestOlympiadMatchList` lista os 22 estádios com status e nomes dos competidores.
  * **Ação Recomendada:** Em `handleOlympiadObserverEnd()`, verificar `active.inObserverMode()`, chamar `active.exitObserverMode()`, enviar `ExOlympiadMode(0)` e teleportar para `obsOrigX/Y/Z`; em `handleOlympiadMatchList()`, listar os estádios de `OlympiadGameService`.

* **#423 — Exploit de Armas Heroicas infinitas via Warehouse em `Quest7000HeroItems.java`:**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest7000HeroItems.onAdvEvent()` (linhas 116–120), para verificar se o Herói já retirou sua arma (`6611..6621`), o código checa apenas o inventário (`qs.getQuestItemsCount(id) > 0`). Se o jogador guardar a arma no Warehouse pessoal (ou se a variável de quest não for salva pois `qs.exitQuest(true)` apaga o estado na linha 124!), ele pode retirar todas as 11 armas `Infinity` repetidamente!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/scripts/custom/7000_HeroItems/__init__.py` (`L2JDreamV2`) e `Hero.java` proíbem depósito de armas heroicas e verificam tanto o inventário quanto o `PCWarehouse` do personagem antes de entregar uma arma `6611..6621` ou a `Wings of Destiny Circlet (6842)`.
  * **Ação Recomendada:** Verificar também `player.context().warehouse().getWarehouseItems(player.activeChar().objectId())` em `Quest7000HeroItems` e `Quest6999HeroCirclet`, além de bloquear depósito de `6611..6621` e `6842` no Warehouse.

* **#424 — Qualquer jogador (mesmo sem ser Herói) pode usar `RequestWriteHeroWords` e a mensagem não é persistida para o Monument of Heroes (`ExtendedProtocolPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.ExtendedProtocolPacketHandler.handleWriteHeroWords()` (linhas 1131–1139), não há verificação `if (!active.isHero()) return;`, a mensagem enviada não é armazenada em `HeroService` / `OlympiadManager`, e o servidor responde enviando um `ExHeroList` falso contendo apenas o próprio jogador que enviou o pacote!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.network.clientpackets.RequestWriteHeroWords` e `Hero.setHeroMessage()` (`L2JDreamV2`) validam `activeChar.isHero()` e persistem a mensagem para exibição no Monument of Heroes (`ExHeroList`).
  * **Ação Recomendada:** Validar `active.isHero()` em `handleWriteHeroWords()`, persistir a mensagem em `HeroService` e enviar a lista real de heróis do ciclo em `ExHeroList`.

* **#425 — Troca de `Noblesse Gate Pass (6651)` na Olympiad Manager usa taxa errada e permite saque de pontos ilimitado (`NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.network.game.handler.packet.NpcBypassPacketHandler.handleOlympiadNoble()` (`case 6`, linhas 4532–4550), qualquer Nobre pode converter seus pontos de Olympiad atuais em `Noblesse Gate Pass (6651)` a qualquer momento durante o ciclo ativo (desde que tenha `>= 50` pontos, sem exigir 9 lutas concluídas e sem estar no período de validação!), zerando `noble.points(0)` e pagando `pts * 20` passes em vez da taxa retail (`1000` Gate Passes por ponto na validação).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `Olympiad.getNoblessePasses()` (`L2JDreamV2`) só permite trocar pontos acumulados do ciclo anterior (`olympiad_nobles_eom`) após o fechamento do ciclo para quem completou ao menos 9 partidas, pagando `Config.ALT_OLY_GP_PER_POINT` (`1000` passes por ponto).
  * **Ação Recomendada:** Restringir o resgate de `Noblesse Gate Pass (6651)` aos pontos fechados do ciclo anterior para nobres com `>= 9` lutas e usar `Config.ALT_OLY_GP_PER_POINT`.

* **#426 — Registro na Olympiad não valida peso de inventário (`>= 80%`), participação em Eventos/Cursed Weapon nem Subclasse ativa (`OlympiadGameService.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.olympiad.OlympiadGameService.register()` (linhas 137–207), não há validação se o jogador está em uma **Subclasse ativa (`player.isSubClassActive()`)**, se está portando uma Cursed Weapon (`player.isCursedWeaponEquipped()`), se está inscrito em TvT/CTF/DM ou se está com `>= 80%` de peso/slots de inventário!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `Olympiad.registerNoble()` (`L2JDreamV2` / `L2JLucera2`) bloqueia imediatamente o registro se `noble.isSubClassActive()` (`SystemMessage.CANT_REGISTER_IN_OLYMPIAD_WITH_SUB_JOB`), se portar Cursed Weapon, se estiver em evento ou com peso `>= 80%`.
  * **Ação Recomendada:** Adicionar validação de `!player.isSubClassActive()`, `!player.isCursedWeaponEquipped()`, ausência de inscrição em eventos e peso `< 80%` em `OlympiadGameService.register()`.

* **#427 — Ausência dos 5 Buffs Oficiais da Olympiad Manager no estádio (`OlympiadGameService.java` / `NpcBypassPacketHandler.java`):**
  * **Diagnóstico (`L2JLopez`):** Na Olympiad Interlude, durante a fase de preparação na arena, o NPC `Olympiad Host (31688)` permite que cada competidor escolha até 5 buffs gratuitos (`Wind Walk Lv2`, `Haste Lv2`, `Acumen Lv3`, `Might Lv3`, `Empower Lv3`, `Shield Lv3`, `Magic Barrier Lv2`, `Guidance Lv3`, `Death Whisper Lv3`, `Focus Lv3`, `Berserker Spirit Lv2`, `Concentration Lv6`). Em `L2JLopez`, o NPC `31688` nunca é spawnado no estádio e o bypass `OlyBuff` não existe.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2OlympiadManagerInstance.onBypassFeedback("OlyBuff")` e `OlympiadGame.spawnBuffer()` (`L2JDreamV2`).
  * **Ação Recomendada:** Spawnar `Olympiad Host (31688)` na fase de preparação da arena e implementar o handler `OlyBuff` com limite de 5 buffs por competidor.

---

## 5. Motor de Quests, Scripts Retail, Sagas, Subclasse e Noblesse (#428 a #440)

* **#428 — `QuestState.addSpawn(...)` é um stub que retorna `null`, quebrando mais de 130 spawns de Quests, Baús de Subclasse e todas as 31 Sagas de 3ª Classe (`QuestState.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.QuestState.addSpawn(int npcId, int x, int y, int z, int despawnDelay)` (linhas 562–564), o método consiste literalmente em `return null;`! Todas as sobrecargas de `addSpawn` em `QuestState` delegam para esse método. Como consequência:
    - Em `Quest234FatesWhisper` (Subclasse), matar `Cabrio`, `Hallate`, `Kernon` e `Golkonda` chama `qs.addSpawn(...)` e **nunca spawna os baús (`31027..31030`)**;
    - Em `SagaMasterQuest` (todas as 31 Sagas de 3ª Classe, Quests `70..100`), interagir com as Tablets of Vision 3 e 6 chama `qs.addSpawn(...)` (linhas 200, 218, 219) e **nunca spawna os Guardiões/Inimigos das Tablets (`mob[0]`, `mob[2]`, `npc[4]`)**, tornando impossível concluir qualquer Saga de 3ª classe;
    - Em `Quest501ProofOfClanAlliance` e `Quest503PursuitOfClanAmbition` (Clã Nível 4 e 5), as caixas de Athrea (`27173..27178`), `Blitz Wyrm (27178)`, `Grave Keymaster (27179)` e `Imperial Coffer (30765)` nunca nascem!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.quest.Quest.addSpawn()` (`L2JDreamV2`) e `l2.gameserver.model.quest.Quest.addSpawn()` (`L2JLucera2`) buscam o `NpcTemplate`, instanciam `NpcInstance`, adicionam ao `GameWorld`, transmitem `NpcInfo` aos jogadores próximos e agendam a remoção automática após `despawnDelay` ms.
  * **Ação Recomendada:** Implementar `QuestState.addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)` criando a `NpcInstance` via `NpcTemplateTable`, registrando-a em `GameWorld` e agendando seu despawn no `QUEST_TIMER_POOL`.

* **#429 — `Quest.addAttackId(...)` é um método vazio e `QuestManager` nunca despacha eventos `onAttack`, quebrando a facada no Baium (`Quest234`), `Abyssal Jewels` (`Quest337`) e `White Fabric` (`Quest348`) (`Quest.java` / `QuestManager.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.Quest.addAttackId(int... npcIds)` (linhas 174–176), o corpo do método está vazio (`// Optional attack hook`), e `QuestManager` não possui índice `questsByAttackNpc` nem método `onNpcAttack()`. Por isso:
    - Na `Quest234FatesWhisper` (linha 300), atacar `Baium (29020)` com a `Pipette Knife (4665)` nunca transforma a faca em `Red Pipette Knife (4673)` via `onAttack`;
    - Na `Quest337AudienceWithLandDragon` (linha 394), atacar as `Abyssal Jewels (27165..27167)` nunca entrega os fragmentos nem spawna os `Jewel Guardians`;
    - Na `Quest348AnArrogantSearch` (linha 440), atacar `Platinum Tribe Shamans/Overlords` nunca tinge a `White Fabric (5232)` em `Blooded Fabric (4295)`.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `com.dream.game.model.quest.Quest.addAttackId()` e `L2Attackable.addDamageHate()` (`L2JDreamV2`) notificam `Quest.notifyAttack(npc, attacker, damage, isPet)` a cada hit físico/mágico.
  * **Ação Recomendada:** Implementar `attackNpcIds` em `Quest.java`, indexar `questsByAttackNpc` em `QuestManager.java` e invocar `questManager().onNpcAttack(npc, session, isPet)` nos handlers de ataque físico e mágico.

* **#430 — `QuestManager.onNpcKill()` notifica apenas o autor do último golpe e `QuestState.getRandomPartyMember()` ignora a Party (`QuestManager.java` / `QuestState.java`):**
  * **Diagnóstico (`L2JLopez`):** (1) Em `QuestState.getRandomPartyMember(int state, double range)` (linha 262), o método ignora a `Party` do jogador e retorna simplesmente `playerChar()`; (2) Em `PlayerCombatHandler.handleMonsterDeath()` (linha 99) e `Quest246PossessorOfAPreciousSoul3.onKill()` (linha 179), matar o Raid Boss `Barakiel (25325)` ou monstros de quest em grupo só chama `onKill` para o único jogador que deu o last hit!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `Quest.getRandomPartyMember()` e `246_PossessorOfAPreciousSoul_3` (`L2JDreamV2` / `L2JLucera2`) sorteiam membros elegíveis da party em quests de drop comum e recompensam todos os membros da party em alcance na morte de Barakiel e Raid Bosses de quest.
  * **Ação Recomendada:** Implementar a busca real nos membros da `Party` dentro de `range` em `QuestState.getRandomPartyMember()` e iterar pelos membros da party elegíveis em `Quest246PossessorOfAPreciousSoul3.onKill()` (Barakiel).

* **#431 — Baús de Subclasse (`Cabrio Coffer`, `Hallate`, `Kernon` e `Golkonda Chests`) em `Quest234FatesWhisper` exigem que o matador do Raid Boss esteja com a Quest 234 ativa (`Quest234FatesWhisper.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest234FatesWhisper.onKill()` (linhas 314–329), o código começa com `if (qs == null || !qs.isStarted()) return null;` antes de verificar `else if (CHEST_SPAWN.containsKey(npcId))`. No Lineage II, quem dá o último golpe nos Raid Bosses `Cabrio (25035)`, `Kernon (25054)`, `Golkonda (25126)` e `Hallate (25220)` quase nunca é o jogador que está fazendo a quest (geralmente é um tanker/DD de nível alto ajudando), o que impede o baú de nascer para os jogadores ao redor!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** Em `L2JDreamV2` / `L2JLucera2`, o spawn dos 4 baús (`31027..31030`) ocorre incondicionalmente na morte de `25035`, `25054`, `25126` e `25220`, independentemente de o autor do último golpe possuir `Quest234FatesWhisper` ativa.
  * **Ação Recomendada:** Mover o `addSpawn(CHEST_SPAWN.get(npcId), ...)` para antes da checagem `if (qs == null || !qs.isStarted())` em `Quest234FatesWhisper.onKill()`.

* **#432 — Quests de Noblesse (`Quest241`, `Quest242`, `Quest246` e `Quest247`) não exigem que o jogador esteja em uma Subclasse ativa (`isSubClassActive()`) (`Quest241..247`):**
  * **Diagnóstico (`L2JLopez`):** Em `Quest241PossessorOfAPreciousSoul1.java`, `Quest242PossessorOfAPreciousSoul2.java`, `Quest246PossessorOfAPreciousSoul3.java` e `Quest247PossessorOfAPreciousSoul4.java`, nenhuma delas verifica `player.isSubClassActive()`. Qualquer personagem na classe base (sem sequer ter feito a quest de Subclasse!) consegue iniciar e concluir as 4 partes da quest de Noblesse! Além disso, `NoblessAndSagaCatalog.java` (linhas 36–38) define `minLevel = 75` para as Partes 1, 2 e 3, quando os níveis oficiais retail são `50` (Parte 1), `60` (Parte 2), `65` (Parte 3) e `75` (Parte 4).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `241_PossessorOfAPreciousSoul_1` a `247_PossessorOfAPreciousSoul_4` (`L2JDreamV2` / `L2JLucera2`) exigem `player.isSubClassActive()` com níveis mínimos `50`, `60`, `65` e `75` respectivamente.
  * **Ação Recomendada:** Exigir `player.isSubClassActive()` no `onTalk` e `onKill` de `Quest241`, `Quest242`, `Quest246` e `Quest247`, e ajustar os níveis mínimos no `NoblessAndSagaCatalog`.

* **#433 — Conclusão da `Quest247PossessorOfAPreciousSoul4` não concede as 8 Skills de Noblesse, não registra o Nobre no `OlympiadManager` e não persiste no banco (`Quest247PossessorOfAPreciousSoul4.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest247PossessorOfAPreciousSoul4.onEvent()` (linhas 62–70), ao concluir a quest na Lady of the Lake (`31745-05.htm`), o código faz apenas `player.setNoble(true);` em memória. Ele não concede as 8 skills oficiais de Noblesse (`325, 326, 327, 1323, 1324, 1325, 1326, 1327`), não registra o personagem em `OlympiadManager.registerNoble()`, não envia `UserInfo` / `SkillList` e não persiste o status `nobless = 1` no banco!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.setNoble(true)` (`L2JDreamV2`) e `Olympiad.addNoble(player)` (`L2JLucera2`) adicionam automaticamente as 8 Noblesse skills (`NobleSkillTable`), registram o nobre na Olympiad, atualizam o banco e enviam `UserInfo`.
  * **Ação Recomendada:** Centralizar a consagração de Noblesse em um método de serviço (ou `GameSession.grantNoblesse()`) que adicione as 8 skills, registre em `OlympiadManager`, salve o personagem e envie `UserInfo`/`SkillList`, invocando-o em `Quest247PossessorOfAPreciousSoul4`.

* **#434 — Conclusão das 31 Sagas de 3ª Classe (`SagaMasterQuest.completeSaga`) altera `classId` apenas em memória sem atualizar skills, banco de dados ou `UserInfo` (`SagaMasterQuest.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.saga.SagaMasterQuest.completeSaga()` (linhas 84–100), ao finalizar a Saga, o método faz apenas `pc.classId(this.classId);` em memória, sem chamar `characters().save()`, sem atualizar `character_subclasses` caso esteja em subclasse, sem conceder as skills iniciais da 3ª classe e sem enviar `UserInfo` e `CharInfo` ao cliente!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `SagasSuperClass.java` (`L2JDreamV2`) e `Saga.java` (`L2JLucera2`) invocam `player.setClassId(classId)`, salvam no banco, concedem as skills da nova classe e transmitem `UserInfo` e `CharInfo`.
  * **Ação Recomendada:** Atualizar `SagaMasterQuest.completeSaga()` para salvar a nova classe em `characters`/`character_subclasses`, chamar `rewardSkills` e enviar `UserInfo`/`CharInfo`.

* **#435 — `Quest350EnhanceYourWeapon` não implementa `onKill()`, impossibilitando a evolução/absorção de Soul Crystals em monstros e Raid Bosses (`Quest350EnhanceYourWeapon.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.impl.Quest350EnhanceYourWeapon` (linhas 1–184), o construtor registra `addKillId(SOUL_ABSORBING_MOBS)`, mas a classe **não sobrescreve `onKill()`**! Não existe lógica de evolução de `Red/Green/Blue Soul Crystal` do estágio 0 ao 13 ao abater monstros/Raid Bosses com a quest ativa.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `350_EnhanceYourWeapon` e `L2Attackable.absorbSoul()` (`L2JDreamV2`) / `SoulCrystalHolder` (`L2JLucera2`) verificam o uso do Soul Crystal na vítima, calculam a chance de subida de estágio ou quebra por nível/tipo de absorção (`LAST_HIT`, `FULL_PARTY`, `PARTY_ONE_RANDOM`) e entregam o cristal evoluído.
  * **Ação Recomendada:** Implementar `onKill()` em `Quest350EnhanceYourWeapon` (e/ou serviço de absorção de Soul Crystal) para evoluir os cristais `4629..4664` dos estágios 0 a 13.

* **#436 — `GrandBossAccessCatalog` mapeia o item de acesso ao Valakas como `7265` (Vacualite Ore) em vez de `7267` (Floating Stone) (`GrandBossAccessCatalog.java`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.quest.GrandBossAccessCatalog` (linha 35), o registro de `VALAKAS` define `accessItemId = 7265` (`Vacualite Ore`, que é o minério intermediário da quest) em vez de `7267` (`Floating Stone`, recompensa final de `Quest618IntoTheFlame`).
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `618_IntoTheFlame` e `Klein (31540)` exigem `Floating Stone (7267)` para atravessar o Hall of Flames rumo ao Valakas.
  * **Ação Recomendada:** Corrigir `accessItemId` de `VALAKAS` em `GrandBossAccessCatalog.java` (linha 35) de `7265` para `7267`.

* **#437 — `Quest4000ShadowWeapons` não possui anotação `@Component`, impedindo a troca de Shadow Weapon Coupons nos Grand Masters (`Quest4000ShadowWeapons.java`):**
  * **Diagnóstico (`L2JLopez`):** `com.lopez.l2j.game.quest.impl.Quest4000ShadowWeapons` (linha 13) é a única classe de quest do pacote `com.lopez.l2j.game.quest.impl` que não possui `@Component` nem `@Autowired` no construtor. Por isso, o Spring nunca a instancia no `QuestManager`, e qualquer bypass `Quest 4000_ShadowWeapons` nos Grand Masters falha silenciosamente.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/scripts/custom/4000_ShadowWeapons/__init__.py` (`L2JDreamV2`).
  * **Ação Recomendada:** Adicionar `@Component` e `@Autowired` em `Quest4000ShadowWeapons.java`.

* **#438 — `Quest605AllianceWithKetraOrcs` e `Quest611AllianceWithVarkaSilenos` não atualizam o nível de aliança em `PlayerCharacter.getAllianceWithVarkaKetra()` (`Quest605` / `Quest611`):**
  * **Diagnóstico (`L2JLopez`):** Em `Quest6050KetraOrcSupport` (linha 72) e `Quest6051VarkaSilenosSupport`, o acesso ao Buffer, Warehouse e Gatekeeper das tribos depende de `player.getAllianceWithVarkaKetra()` retornar `1..5` (ou `-1..-5`). Porém, quando `Quest605AllianceWithKetraOrcs` e `Quest611AllianceWithVarkaSilenos` entregam ou removem os itens `Mark of Ketra's Alliance (7211..7215)` e `Mark of Varka's Alliance (7221..7225)`, `player.getAllianceWithVarkaKetra()` não reflete dinamicamente o estágio da marca no inventário!
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.getAllianceWithVarkaKetra()` (`L2JDreamV2`) verifica a presença dos itens `7211..7215` (`+1` a `+5`) e `7221..7225` (`-1` a `-5`) no inventário ou atualiza `setAllianceWithVarkaKetra()` ao receber/perder a marca.
  * **Ação Recomendada:** Fazer `PlayerCharacter.getAllianceWithVarkaKetra()` inspecionar os itens `7211..7215` e `7221..7225` no inventário do personagem (ou sincronizar o campo em `Quest605` e `Quest611`).

* **#439 — `Quest635InTheDimensionalRift` e `Quest644GraveRobberAnnihilation` dependem de `PlayerCharacter.getLastNpc()` que retorna `null` fixo (`PlayerCharacter.java` / `Quest635` / `Quest644`):**
  * **Diagnóstico (`L2JLopez`):** Em `com.lopez.l2j.game.model.PlayerCharacter.getLastNpc()` (linhas 296–298), o método retorna `null` fixo. Em `Quest635InTheDimensionalRift.java` (linha 63) e `Quest644GraveRobberAnnihilation.java` (linha 58), a lógica verifica `if (pc.getLastNpc() != null ...)`, falhando sempre.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `L2PcInstance.getLastFolkNpc()` (`L2JDreamV2`) retorna o último `NpcInstance` com quem o jogador interagiu.
  * **Ação Recomendada:** Armazenar a referência `lastFolkNpc` em `PlayerCharacter` ao interagir com um NPC em `ActionPacketHandler`/`NpcBypassPacketHandler` e retorná-la em `getLastNpc()`.

* **#440 — Ausência de registro da `Quest344_1000YearsEndofLamentation` por incompatibilidade de nome e de `Quest8000RaidbossInfo` / `Quest8001NpcLocationInfo` (`QuestManager.java` / `com.lopez.l2j.game.quest.impl`):**
  * **Diagnóstico (`L2JLopez`):** Os scripts custom/retail `8000_RaidbossInfo` e `8001_NpcLocationInfo` (presentes em `data/scripts/custom` e referenciados pelos HTMLs das Gatekeepers e Newbie Guides de todas as cidades via `bypass -h Quest 8000_RaidbossInfo` e `bypass -h Quest 8001_NpcLocationInfo`) não possuem classes `@Component` em `com.lopez.l2j.game.quest.impl`, resultando em links quebrados nos NPCs de todas as vilas iniciais e cidades.
  * **Referência (`L2JDreamV2` / `L2JLucera2`):** `game/data/scripts/custom/8000_RaidbossInfo/__init__.py` e `8001_NpcLocationInfo/__init__.py` (`L2JDreamV2`).
  * **Ação Recomendada:** Implementar os componentes `Quest8000RaidbossInfo` e `Quest8001NpcLocationInfo` em `com.lopez.l2j.game.quest.impl` para atender aos bypasses de localização de NPCs (com radar) e informações de Raid Bosses.

---

# Relatório de Auditoria Profunda — Subagente 6 (#441 a #510)
**Escopo:** Admin Commands, Voiced Commands, Community Board (BBS), Events & Custom Mods, e Persistência SQL Ponta a Ponta (`L2JLopez` vs `L2JDreamV2` / `L2JLucera2`).

---

## 1. Admin Commands (Comandos de GM / Admin — `#441` a `#456`)

- **#441. Colisão de Roteamento em `//siege` Impedindo Abertura do Painel de Castelos/ClanHalls:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.admin.AdminGeneralHandler` (linhas 53–54 e 342–376) e `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linha 3437).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminSiege` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminResidence` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `AdminGeneralHandler` registra `"siege"` e `"admin_siege"` no `AdminCommandHandlerRegistry`, que é avaliado na linha 308 de `AdminPacketHandler` antes do `switch (action)`. Quando o GM digita `//siege` sem argumentos (ou clica no botão `"Castles / Siege"` em `main_menu.htm`), `AdminGeneralHandler.handleSiegeCommand()` exige `parts.length >= 2`, envia mensagem de erro de uso e aborta a execução, tornando inacessível o painel HTML `showAdminCastlesHtml()` na linha 3437 de `AdminPacketHandler`. Deve-se delegar `//siege` sem parâmetros para `showAdminCastlesHtml(session, ctx)` e suportar todos os subcomandos (`//add_attacker`, `//add_defender`, `//clear_siege_list`, `//move_defenders`, `//spawn_doors`, `//startsiege`, `//endsiege`, `//setcastle`, `//removecastle`).

- **#442. Comandos Admin de ClanHall e Placeholders `%clanhalls%` / `%freeclanhalls%` Falsos (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 343–348 e 1708–1710).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminSiege` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminClanHall` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Em `showAdminCastlesHtml()`, `%clanhalls%` e `%freeclanhalls%` são substituídos por texto estático `"Giran Clan Hall / Aden Clan Hall"` e `"Nenhum"` em vez de consultar `ctx.clanHalls()`. Além disso, os comandos `//clanhallset`, `//clanhalldel`, `//clanhallopendoors`, `//clanhallclosedoors` e `//clanhallteleportself` (linhas 1708–1710) apenas enviam uma mensagem de chat (`"Acao de ClanHall executada: ..."`) sem invocar `ClanHallService` nem `DoorTable`.

- **#443. Comandos Admin de Cursed Weapons (`//cw_add`, `//cw_remove`, `//cw_reload`) Simulados (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 1643–1651).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminCursedWeapon` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminCursedWeapons` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Enquanto `//cw_info` e `//cw_goto` consultam `CursedWeaponsManager`, os comandos `//cw_add`, `//cw_remove` e `//cw_reload` apenas enviam um `CreatureSay` dizendo `"Comando Cursed Weapon executado: ..."` sem conceder/remover a Zariche/Akamanah no `CursedWeaponsManager` nem recarregar as configurações.

- **#444. Comandos Admin de Olimpíadas (`//endolympiad` e `//fix_noble_name`) Simulados (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 1667–1669 e 3053).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminOlympiad` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminOlympiad` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `//endolympiad` apenas envia `"Periodo da Olimpiada encerrado manualmente."` sem acionar o encerramento do ciclo, cálculo de heróis (`HeroService`) e salvamento no `OlympiadManager`; já `//fix_noble_name` apenas envia `"Nome de nobre corrigido."` sem executar `UPDATE olympiad_nobles SET char_name = ? WHERE char_id = ?`.

- **#445. Comandos Admin de Aura de Time (`//setteam1`, `//setteam2`, `//clearteam`) Simulados (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 1702–1704).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminEffects` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminEffects` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Os comandos `setteam1`, `setteam2`, `clearteam`, `rangesetteam1`, `rangesetteam2` e `rangeclearteam` apenas enviam `"Time de evento atualizado: ..."` sem alterar `target.team(1/2/0)` nem transmitir `UserInfo`/`CharInfo` para os jogadores ao redor.

- **#446. Comando `//clear_skill_reuse` / `//reset_reuse` Simulado e `//remove_all_skills` Não-Persistente:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 1729–1746).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminSkills` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminSkill` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `//clear_skill_reuse` / `//reset_reuse` apenas envia `"Cooldowns resetados para o alvo."` sem limpar os timestamps de cooldown em `session.magicSkillHandler()` nem enviar `SkillCoolTime`. Em `//remove_all_skills`, `target.skills().clear()` limpa apenas a memória sem chamar `ctx.skills().deleteAll(target.objectId(), target.classIndex())` no SQL, e faltam os comandos `//get_skills` e `//reset_skills` (para copiar/restaurar skills de um jogador durante inspeção de GM).

- **#447. Recursão Morta em `//recall_party_menu` e `//recall_clan_menu`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 2665–2670).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminTeleport` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminTeleport` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `case "recall_party_menu"` chama `handleAdminCommand("recallparty " + sub, ...)` e `case "recall_clan_menu"` chama `handleAdminCommand("recallclan " + sub, ...)`, porém nem `recallparty` nem `recallclan` existem no `switch` ou no `AdminCommandHandlerRegistry`, caindo no `default` (`"Comando '//recallparty' nao reconhecido."`) em vez de teleportar todos os membros da party (`target.party().members()`) ou clã (`ctx.world().getPlayers()` filtrados por `clanId()`) até o GM.

- **#448. Comandos `//spawnnight` e `//spawnday` Simulados (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 2830–2831).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminSpawn` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminSpawn` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `//spawnnight` e `//spawnday` apenas enviam mensagens estáticas (`"Spawns noturnos/diurnos ativados."`) em vez de acionar `DayNightSpawnService.spawnNightCreatures()` / `spawnDayCreatures()`.

- **#449. Comandos `//ban_account` e `//unban_account` Alteram `characters` em Vez da Tabela `accounts`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 2444–2476).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminBan` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminBan` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `handleAdminBanAccount()` e `handleAdminUnbanAccount()` chamam `ctx.characters().setAccessLevelByName(accName, -100)` buscando por `char_name` na tabela `characters` em vez de atualizar `accounts.access_level` via `JdbcAccountStore` (usando o login da conta) e desconectar quaisquer personagens online daquela conta.

- **#450. Comando `//find_ip` Não Filtra por IP e `showAdminCharInfo` Hardcoda `%ip%`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 636–641 e 2933–2958).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminEditChar` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminIP` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Em `showAdminCharInfo()`, o placeholder `%ip%` é preenchido literalmente com `"Online"` ou `"127.0.0.1"` em vez de `target.ip()` / `session.remoteAddress()`. Além disso, `handleAdminFindIp()` ignora o endereço IP passado por parâmetro e lista todos os jogadores online do servidor sem comparar `p.ip()`.

- **#451. Comandos `//setcolor`, `//settitlecolor`, `//tradeoff`, `//atmosphere_menu` e `//set_mod` Simulados (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 1698–1701, 2178–2180, 2320–2327 e 2357).
  - **Referência Dream/Lucera:** `AdminEditChar`, `AdminTradeOff`, `AdminEffects` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `//setcolor` e `//settitlecolor` enviam `"Cor do nome/titulo alterada"` sem converter o hex BGR nem atualizar `target.nameColor()` / `target.titleColor()` e `UserInfo`; `//tradeoff` não altera `player.tradeRefusal()`; `//atmosphere_menu` / `//atmosphere` não transmite pacotes `SunRise`, `SunSet` ou `ExRedSky`; e `//set_mod` não altera taxas dinâmicas em `Config`.

- **#452. Comando `//autoannounce` Não Persiste nemLista a Tabela `auto_announcements`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 3071–3074) e `com.lopez.l2j.game.Announcements` (linhas 40–60).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminAnnounce` / `AutoAnnouncementHandler` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** `//list_autoannouncements`, `//add_autoannouncement` e `//del_autoannouncement` apenas respondem `"Gerenciador de auto-anuncios acessado."` sem consultar, inserir ou remover registros da tabela `auto_announcements` (`V9__auto_announcements.sql`) e sem exibir `data/html/admin/autoannounce.htm`.

- **#453. Comando `//reload` Incompleto e `//reload skill` Simulado (Stub):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 2478–2522).
  - **Referência Dream/Lucera:** `com.dream.game.handler.admincommandhandlers.AdminReload` (`L2JDreamV2`) / `ru.catssoftware.gameserver.gmaccess.handlers.AdminReload` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Em `handleAdminReload()`, a opção `skill` / `skills` apenas envia uma mensagem dizendo que as skills foram recarregadas sem chamar `SkillTable.reload()` ou `SkillTreeTable.reload()`. Faltam ainda as opções `reload npc`, `reload item`, `reload droplist`, `reload teleport`, `reload instance` e `reload quest`.

- **#454. Ausência de Handlers Reais para Geodata (`//geo_*`) e Zonas (`//zone_*`):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 3036–3043) e `data/html/admin/zone.htm`.
  - **Referência Dream/Lucera:** `AdminGeodata` e `AdminZone` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `//zone_check` e `//zone_reload` apenas abrem `admin/zone.htm` sem substituir os placeholders de zona atual (`%MAP_X%`, `%MAP_Y%`, flags de `ZoneService`) nem recarregar `ZoneService`. Os comandos `//geo_z`, `//geo_type`, `//geo_nswe`, `//geo_los`, `//geo_position`, `//geo_can_see` e `//geo_move` não existem no `AdminPacketHandler` nem no `AdminCommandHandlerRegistry`.

- **#455. Ausência dos Comandos Admin de Fortress (`//fortsiege`), Manor (`//manor`) e MobGroup (`//mobgroup_*`):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler` (linhas 3045–3051 e 3067–3070).
  - **Referência Dream/Lucera:** `AdminFortress`, `AdminManor`, `AdminMobGroup` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `//fortsiege` e `//manor` apenas abrem `admin/fortsiege.htm` e `admin/manor.htm` sem implementar nenhum dos subcomandos acionados pelos botões dessas páginas (`add_attacker_fort`, `start_fortsiege`, `end_fortsiege`, `manor_info`, `manor_save`, `manor_reset`, `manor_approve`, `manor_disable`). De igual modo, os comandos de `AdminMobGroup` (`//mobgroup_create`, `//mobgroup_spawn`, `//mobgroup_unspawn`, `//mobgroup_attack`, `//mobgroup_follow`, `//mobgroup_idle`) estão completamente ausentes.

- **#456. Ausência de Comandos Admin `//snoop`, `//ride` (por tipo/ID) e `//cache_*`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.AdminPacketHandler`.
  - **Referência Dream/Lucera:** `AdminEditChar` / `AdminRide` / `AdminCache` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Faltam `//snoop <player>` / `//unsnoop` (envio do pacote `Snoop` para monitorar o chat do alvo), `//ride <1|2|3|npcId>` / `//unride` (existem apenas `ride_wyvern` e `ride_strider` nas linhas 1705–1707, faltando `admin_ride` genérico e `admin_unride`) e `//cache_htm_rebuild` / `//cache_htm_reload` / `//cache_crest_rebuild`.

---

## 2. Voiced Commands (Comandos `.comando` — `#457` a `#468`)

- **#457. Classe `VoicedCommandHandler` Duplicada e Desconectada Retornando Stubs Estáticos:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.VoicedCommandHandler` (linhas 1–133) vs `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` e `com.lopez.l2j.network.game.handler.CommandPacketHandler`.
  - **Referência Dream/Lucera:** `com.dream.game.handler.VoicedCommandHandler` (`L2JDreamV2`) / `ru.catssoftware.gameserver.handler.VoicedCommandHandler` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Existem três pontos diferentes lidando com voiced commands: `ChatPacketHandler` delega ao `VoicedCommandHandlerRegistry` (`VoicedGeneralHandler`, `VoicedAcpHandler`, `VoicedAutoFarmHandler`, `VoicedBufferHandler`, `VoicedBuffShopHandler`), enquanto `CommandPacketHandler` mantém um `switch` paralelo (linhas 54–302) e `com.lopez.l2j.network.game.handler.VoicedCommandHandler` mantém stubs falsos que apenas retornam strings estáticas (`"Painel de configuracoes aberto."`, `"AutoFarm alternado."`) sem executar nenhuma lógica. Deve-se unificar tudo no `VoicedCommandHandlerRegistry` e remover os stubs duplicados.

- **#458. Comando `.offline` / `.offlineshop` Ignora a Loja do Jogador e Cria Loja Falsa de 1 Adena:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` (linhas 419–440) e `com.lopez.l2j.network.game.handler.CommandPacketHandler` (linhas 250–259).
  - **Referência Dream/Lucera:** `com.dream.game.handler.voiced.OfflineShop` (`L2JDreamV2`) / `ru.catssoftware.gameserver.handler.voicedcommandhandlers.Offline` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Ao digitar `.offline`, tanto `VoicedGeneralHandler` quanto `CommandPacketHandler` instanciam uma lista com um item fixo hardcoded `items.add(new OfflineTradeService.OfflineShopItem(57, 1, 1))` (vendendo 1 Adena por 1 Adena) e chamam `registerOfflineTrader(player.objectId(), 1, "Offline Shop", items)` sem verificar se o jogador está em `privateStoreType()` (`SELL`, `BUY`, `PACKAGE_SELL` ou `MANUFACTURE`) e sem copiar os itens reais de `OfflineTradeService.getSellStoreList()` / `getBuyStoreList()`, além de não fechar a conexão TCP do cliente após entrar em modo offline.

- **#459. Ausência dos Comandos `.expoff` / `.expon`, `.autoloot`, `.traderefuse` e `.buffshield` por Linha de Comando:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` e `com.lopez.l2j.game.service.PlayerPreferencesService`.
  - **Referência Dream/Lucera:** `Menu` / `Settings` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Embora `.menu` abra o HTML de preferências (`BypassPreferencesHandler`), os jogadores não conseguem alternar diretamente via chat `.expoff` / `.expon` (bloqueio de XP), `.autoloot` (auto-loot individual), `.traderefuse` (recusa de trade) e `.buffshield` / `.antibuff` (proteção contra buffs externos), que são comandos voiced padrão em servidores Interlude baseados em Dream/Lucera.

- **#460. Ausência do Comando `.raidinfo` / `.epic` / `.boss` / `.7rb`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `RaidInfo` / `SevenRb` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Existe o bypass `npc_raidinfo` em `BypassEventsAndPveHandler` (linhas 288–355) e `SevenRaidbossService`, mas os comandos voiced `.raidinfo`, `.epic`, `.boss` e `.7rb` não estão registrados em `VoicedGeneralHandler.getVoicedCommandList()`, impedindo que o jogador consulte o status dos Grand Bosses/Raid Bosses e o progresso da quest dos 7 Raid Bosses pelo chat.

- **#461. Ausência do Comando `.repair` (Reparo de Personagem Preso na Mesma Conta):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` e `com.lopez.l2j.game.community.RepairBBSManager`.
  - **Referência Dream/Lucera:** `com.dream.game.handler.voiced.Repair` (`L2JDreamV2`) / `ru.catssoftware.gameserver.handler.voicedcommandhandlers.Repair` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `RepairBBSManager` existe para o Community Board (`_bbsrepair`), mas falta o comando `.repair <nome>` no `VoicedGeneralHandler` que permite ao jogador listar os personagens da mesma conta (`accounts`/`characters`), teleportar o personagem travado para Floran/Talking Island (`17867, 170259, -3503`), limpar seus atalhos (`character_shortcuts`) e mover itens equipados para o armazém (`LOC_WAREHOUSE`).

- **#462. Ausência do Comando `.changepassword` / `.password` com Persistência em `accounts`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `Password` / `ChangePassword` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Não há comando `.changepassword <senhaAtual> <novaSenha> <confirmacao>` nem janela HTML dedicada que valide o hash da senha atual em `JdbcAccountStore` (`LegacyPasswordHasher`) e execute `UPDATE accounts SET password = ? WHERE login = ?`.

- **#463. Ausência do Comando `.combine` / `.anky` (Combinação de Talismãs / Itens):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `com.dream.game.handler.voiced.Anky` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** O handler `Anky` presente em `L2JDreamV2/java/com/dream/game/handler/voiced/Anky.java` não foi portado para o `VoicedGeneralHandler` do `L2JLopez`.

- **#464. Ausência do Comando `.castlemanager` / `.siege` para Registro em Sieges via Voiced:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `com.dream.game.handler.voiced.CastleManager` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** O comando `.castlemanager` / `.siege` do `L2JDreamV2` (`CastleManager.java`), que abre a interface de visualização de proprietários de castelos, datas de siege e registro de ataque/defesa (`SiegeService`), não foi registrado nem implementado no `VoicedGeneralHandler`.

- **#465. Ausência do Comando `.clan` / `.cwh` (`CWHPrivileges` para Privilégios do Clan Warehouse):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `ru.catssoftware.gameserver.handler.voicedcommandhandlers.CWHPrivileges` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Falta o comando `.clan` / `.cwh` do `L2JLucera2` que permite ao líder do clã conceder/revogar permissão de retirada no Clan Warehouse para membros específicos e gerenciar segurança (`WarehousePasskeyService` / `clan_privs`).

- **#466. Comandos `.engage`, `.gotolove` e `.divorce` Sem Validação de Estado de Combate/Olimpíada/Jail nem Cobrança de Taxas:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` (linhas 163–258) e `com.lopez.l2j.game.service.WeddingService`.
  - **Referência Dream/Lucera:** `com.dream.game.handler.voiced.Wedding` (`L2JDreamV2`) / `ru.catssoftware.gameserver.handler.voicedcommandhandlers. Wedding` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `.gotolove` teleporta o jogador instantaneamente sem delay (`WeddingTeleportDuration`), sem cobrar o custo de teleporte (`WeddingTeleportPrice` — `WeddingService.getTeleportCost()` nunca é cobrado via `InventoryService.consumeItem()`) e sem checar se o jogador ou o cônjuge está em Jail, Cursed Weapon, Dimensional Rift, GrandBoss zone, CTF/TvT/DM ou se o alvo está em instância privada.

- **#467. Comandos `.tvt`, `.ctf` e `.dm` Sem Subcomandos `.tvtjoin` / `.tvtleave` / `.tvtinfo`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` (linhas 40–65 e 260–345).
  - **Referência Dream/Lucera:** `TvTEventCommand`, `CTFEventCommand`, `DMEventCommand` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `VoicedGeneralHandler` registra apenas `"tvt"`, `"ctf"` e `"dm"`, mas no `L2JDreamV2` e `L2JLucera2` os jogadores e HTMLs de anúncio usam diretamente `.tvtjoin`, `.tvtleave`, `.tvtinfo`, `.ctfjoin`, `.ctfleave`, `.ctfinfo`, `.dmjoin`, `.dmleave` e `.dminfo`. Esses aliases devem ser registrados em `getVoicedCommandList()` e roteados para `handleTvt`/`handleCtf`/`handleDm`.

- **#468. Ausência dos Comandos Utilitários `.whoami` / `.stats`, `.ping` e `.help`:**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler`.
  - **Referência Dream/Lucera:** `Help`, `WhoAmI`, `Ping` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Faltam os comandos `.help` (listando todos os comandos voiced disponíveis para o jogador), `.whoami` / `.stats` (exibindo taxas efetivas de XP/SP/Drop/Spoil com bônus VIP, resistências e status ocultos) e `.ping`.

---

## 3. Community Board (`ALT+B` — `#469` a `#479`)

- **#469. Pacote `ShowBoard` Enviado Sem Fragmentação `101`/`102`/`103` (`separateAndSend`):**
  - **L2JLopez:** `com.lopez.l2j.network.game.handler.NpcBypassPacketHandler` (`handleBbsCommand`, linhas 195–218) e `com.lopez.l2j.network.game.serverpacket.ShowBoard`.
  - **Referência Dream/Lucera:** `com.dream.game.communitybbs.Manager.BaseBBSManager.separateAndSend` (`L2JDreamV2`) / `ru.catssoftware.gameserver.communitybbs.Manager.BaseBBSManager` (`L2JLucera2`).
  - **Diagnóstico Acionável:** O protocolo do cliente Interlude exige que o HTML do Community Board seja enviado em 3 pacotes `ShowBoard` consecutivos com prefixos `"101\t"`, `"102\t"` e `"103\t"` (dividindo strings maiores que 4090 caracteres, ou enviando `"102\t"` e `"103\t"` vazios quando menor que 4090). `NpcBypassPacketHandler` envia apenas um único `new ShowBoard(html)` com prefixo `"101"`, o que trunca páginas grandes (como rankings, leilão e lista de buffs) ou impede a renderização correta no cliente Interlude.

- **#470. `CommunityBoardService` Ignora os Templates em `data/html/CommunityBoard/`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (linhas 190–935).
  - **Referência Dream/Lucera:** `TopBBSManager.java` (`L2JDreamV2` / `L2JLucera2`) e `data/html/CommunityBoard/`.
  - **Diagnóstico Acionável:** Todas as telas de `CommunityBoardService` são montadas via `StringBuilder` hardcoded em Java, ignorando completamente os arquivos HTML customizáveis do datapack (`data/html/CommunityBoard/index.htm`, `50.htm`, etc.). Deve-se carregar os templates via `HtmCache` quando disponíveis, mantendo o fallback em memória apenas se o arquivo não existir.

- **#471. Comando `_bbsloc` Roteado Indevidamente para Rankings em Vez de `RegionBBSManager`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (linha 140).
  - **Referência Dream/Lucera:** `com.dream.game.communitybbs.Manager.RegionBBSManager` (`L2JDreamV2`) / `ru.catssoftware.gameserver.communitybbs.Manager.RegionBBSManager` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Na linha 140 de `CommunityBoardService`, `cmd.startsWith("_bbsloc")` chama `getRankingsHtml(player)` em vez de exibir a aba Region (contagem e lista de jogadores online por região/cidade e lojas privadas ativas, como em `RegionBBSManager`).

- **#472. Aba `_bbsclan` (`ClanBBSManager`) Ausente no `CommunityBoardService`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (`handleCommand`, linhas 97–188).
  - **Referência Dream/Lucera:** `com.dream.game.communitybbs.Manager.ClanBBSManager` (`L2JDreamV2`) / `ru.catssoftware.gameserver.communitybbs.Manager.ClanBBSManager` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Quando o jogador clica na aba `"Clan"` do Community Board no cliente Interlude (que envia `_bbsclan`), `CommunityBoardService.handleCommand()` não possui tratador para `_bbsclan` (`clanlist`, `clanhome`, `clanmail`, `clannotice`), caindo no fallback da página inicial (`getHomeHtml`).

- **#473. Aba `_bbsmemo` e Métodos SQL `getForums`/`getTopics`/`getPosts` Mortos (Dead Code) em `CommunityBoardService`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (linhas 97–188 e 1036–1158).
  - **Referência Dream/Lucera:** `TopicBBSManager`, `PostBBSManager` e `ForumsBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `CommunityBoardService` implementa `getForums()`, `getOrCreateForum()`, `getTopics()`, `createTopic()`, `getPosts()` e `createPost()` nas linhas 1036–1158 acessando as tabelas `forums`, `topic` e `posts`, porém **nenhum desses métodos é chamado em lugar algum**! Os comandos `_bbsmemo`, `_bbstopics` e `_bbsposts` não estão mapeados em `handleCommand()`.

- **#474. Abas `_bbsgetfav` (Favoritos), `_bbsfriends` e `_bbsmail` Ausentes:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (`handleCommand`, linhas 97–188).
  - **Referência Dream/Lucera:** `FavoritesBBSManager`, `FriendsBBSManager`, `MailBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** As abas nativas da janela `ALT+B` do cliente Interlude (`_bbsgetfav` / `_bbsdelfav`, `_bbsfriends` e `_bbsmail`) não possuem rota em `CommunityBoardService.handleCommand()`, redirecionando silenciosamente para a Home.

- **#475. Buffer do Community Board (`_bbsbuffer`) é um Stub Falso que Apenas Restaura HP/MP em Memória:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (`getBufferHtml` e `handleBuff`, linhas 688–734).
  - **Referência Dream/Lucera:** `BuffBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `CommunityBoardService` injeta `SchemeBufferService schemeBufferService` na linha 62, mas **nunca o utiliza**! Em `handleBuff()` (linhas 714–734), quando o jogador clica em `"Esquema Guerreiro"`, `"Esquema Mago"` ou qualquer buff individual (`_bbsbuffer;warrior`, `_bbsbuffer;mage`, `_bbsbuffer;1204`), o código apenas faz `player.currentHp(player.maxHp()); player.currentMp(player.maxMp());` sem aplicar nenhum buff (`SchemeBufferService.applyBuff` / `applyScheme` / `applyMacroFighter` / `applyMacroMage`) e sem sequer enviar `StatusUpdate` ou `MagicSkillUse` ao cliente!

- **#476. Alteração de Senha no Community Board (`_bbsservice;passchange`) Salva Apenas em `ConcurrentHashMap` Volátil:**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService` (`handlePasswordChange`, linhas 661–682).
  - **Referência Dream/Lucera:** `ServiceBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `handlePasswordChange()` grava a nova senha apenas em `private final Map<String, String> accountPasswordHashes = new ConcurrentHashMap<>()` em memória! A tabela `accounts` (`JdbcAccountStore`) nunca é atualizada no banco de dados SQL, portanto a troca de senha não tem efeito algum no LoginServer.

- **#477. Leilão do Community Board (`CommunityAuctionService`) 100% em Memória e Perde Pagamento de Vendedor Offline:**
  - **L2JLopez:** `com.lopez.l2j.game.community.CommunityAuctionService` (linhas 19–188).
  - **Referência Dream/Lucera:** `AuctionBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `CommunityAuctionService` guarda os itens anunciados apenas em `Map<Long, AuctionListing> activeListings = new ConcurrentHashMap<>()` sem nenhuma tabela SQL ou persistência via `JdbcItemRepository`:
    1. Se o servidor reiniciar, todos os itens anunciados no leilão desaparecem permanentemente;
    2. Ao criar/cancelar/comprar um anúncio, `player.inventory().destroyItem()` e `addItem()` são chamados diretamente em memória sem persistir no banco (`InventoryService`) e sem enviar `InventoryUpdate`;
    3. Na linha 124 (`buyItem`), se o vendedor estiver offline no momento da compra (`sellerOpt.isEmpty()`), o comentário diz `// Em producao, persistir no banco ou enviar por correio`, e o pagamento em Adena é **totalmente perdido**;
    4. Ao recriar o `ItemInstance` (linhas 119 e 157), `augmentationId`, `mana` e `customType` são perdidos.

- **#478. `RepairBBSManager` Não Valida se o Personagem Alvo Pertence à Mesma Conta nem Se Está Online:**
  - **L2JLopez:** `com.lopez.l2j.game.community.RepairBBSManager` (linhas 24–74) e `com.lopez.l2j.network.game.handler.NpcBypassPacketHandler` (linhas 201–208).
  - **Referência Dream/Lucera:** `RepairBBSManager` / `Repair.java` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `RepairBBSManager.repairCharacter(String charName)` executa `UPDATE characters SET x=17867, y=170259, z=-3503 WHERE char_name = :name` para **qualquer nome de personagem enviado no bypass `_bbsrepair;exec;<nome>`**, sem verificar se `account_name` daquele personagem corresponde ao `session.accountName()` do jogador solicitante e sem checar se o alvo está online ou em prisão (`in_jail`)! Isso é uma falha crítica de segurança (qualquer jogador pode alterar as coordenadas SQL e limpar os atalhos de qualquer outro jogador do servidor).

- **#479. Ausência dos Módulos Customizados de BBS (`_bbsgmshop`, `_bbsdressme`, `_bbsraids`, `_bbsevent`):**
  - **L2JLopez:** `com.lopez.l2j.game.service.CommunityBoardService`.
  - **Referência Dream/Lucera:** `GmShopBBSManager`, `RaidBBSManager`, `EventBBSManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `CommunityBoardService` não integra atalhos e handlers para abrir Multisells de GM Shop (`_bbsgmshop` / `_bbsmultisell`), painel de Skins (`_bbsdressme` -> `DressMeService`), status de Raid Bosses (`_bbsraids` -> `BossStatusService`) e inscrição nos eventos TvT/CTF/DM (`_bbsevent`).

---

## 4. Events & Custom Mods (`#480` a `#498`)

- **#480. `TvtEventService`, `CtfEventService` e `DmEventService` Nunca Anunciam Globalmente nem Spawnam NPC de Registro:**
  - **L2JLopez:** `com.lopez.l2j.game.event.pvp.TvtEventService`, `CtfEventService` e `DmEventService`.
  - **Referência Dream/Lucera:** `TvT`, `CTF`, `DM` (`com.dream.game.model.entity.events.*` em `L2JDreamV2` / `ru.catssoftware.gameserver.model.entity.events.*` em `L2JLucera2`).
  - **Diagnóstico Acionável:** Nenhum dos três serviços injeta ou chama `Announcements.announceToAll()` quando o registro abre (`startRegistration`), durante a contagem regressiva, no início da luta (`startFight`) ou no encerramento (`endEvent`), e nenhum deles faz o spawn/unspawn do NPC Event Manager em Giran (`TVT_EVENT_PARTICIPATION_NPC_ID`, `CTF_EVENT_PARTICIPATION_NPC_ID`, `DM_NPC_ID`).

- **#481. `onKill()` em `TvtEventService` e `DmEventService` Revive a Vítima Instantaneamente Ignorando `ReviveDelay`:**
  - **L2JLopez:** `com.lopez.l2j.game.event.pvp.TvtEventService` (linhas 404–418) e `com.lopez.l2j.game.event.pvp.DmEventService` (linhas 335–345).
  - **Referência Dream/Lucera:** `TvT.java` e `DM.java` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Quando um participante morre no TvT ou DM, `onKill()` chama `victim.revive()`, restaura HP/MP/CP e teleporta a vítima na mesma thread e no mesmo milissegundo da morte, ignorando completamente `getReviveDelaySeconds()` (`Config.TVT_REVIVE_DELAY` / `Config.DM_REVIVE_DELAY`) e sem enviar `StatusUpdate`. Deve-se agendar uma task com `scheduler.schedule(..., getReviveDelaySeconds(), TimeUnit.SECONDS)` para reviver e teleportar após o delay configurado.

- **#482. Fechamento de Portas do Coliseu (`coliseumDoorsClosed`) em `TvtEventService` e `CtfEventService` é Apenas um Booleano Fictício:**
  - **L2JLopez:** `com.lopez.l2j.game.event.pvp.TvtEventService` (linhas 283 e 349) e `com.lopez.l2j.game.event.pvp.CtfEventService` (linhas 326 e 411).
  - **Referência Dream/Lucera:** `TvT.closeColiseumDoors()` / `openColiseumDoors()` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `this.coliseumDoorsClosed = true` e `false` apenas alternam um campo booleano sem nunca consultar `DoorTable` para fechar/abrir de fato as portas do Coliseu (`24190001`, `24190002`, `24190003`, `24190004`), permitindo que jogadores entrem ou saiam da arena durante a luta.

- **#483. Bandeiras do CTF (`CtfEventService`) Desconectadas da Interação com NPC, Combate e Equipamento da Arma de Bandeira (`6718`):**
  - **L2JLopez:** `com.lopez.l2j.game.event.pvp.CtfEventService` (linhas 434–545).
  - **Referência Dream/Lucera:** `com.dream.game.model.entity.events.CTF` (`L2JDreamV2`) / `ru.catssoftware.gameserver.model.entity.events.CTF` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Os métodos `takeEnemyFlag()`, `captureFlag()`, `returnFlagToBase()` e `reviveParticipant()` nunca são chamados em lugar algum do servidor! Quando um jogador clica nas bandeiras spawnadas (`objectId` `0x57000001` e `0x57000002`, template `35062`), `NpcActionPacketHandler` não aciona `CtfEventService`; quando o portador da bandeira morre em `PlayerCombatHandler`, a bandeira não cai nem retorna à base, o jogador morto nunca é revivido no CTF, e pegar a bandeira não equipa o item físico da bandeira (`itemId 6718`) nas mãos do personagem.

- **#484. Ausência de Validação Anti-Dualbox (IP/HWID), Remoção de Party e Checagens de Restrição no TvT/CTF/DM:**
  - **L2JLopez:** `TvtEventService.register()`, `CtfEventService.register()` e `DmEventService.register()`.
  - **Referência Dream/Lucera:** `TvT.checkMaxPlayers` / `addParticipant` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `register()` e `startFight()` não verificam se o jogador já tem outro personagem inscrito com o mesmo IP (`TVT_PROTECT_MULIBOX`), não verificam se está em Olympiad, Duel, Jail, Cursed Weapon, Karma ou Offline Shop, e não removem o jogador de sua `Party` atual ao teleportar para a arena.

- **#485. `PvPEventService` Paralelo Altera `x/y/z` Apenas em Memória Sem Enviar `TeleportToLocation`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.PvPEventService` (linhas 317–331 e 420–429).
  - **Referência Dream/Lucera:** `L2EventEngine` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Além de duplicar `TvtEventService`/`CtfEventService`/`DmEventService`, `PvPEventService.startEvent()` e `endEvent()` alteram diretamente `ch.x(...)`, `ch.y(...)`, `ch.z(...)` e `ch.instanceId(...)` sem chamar `GameWorld.teleportPlayer()` e sem enviar o pacote `TeleportToLocation`, deixando o cliente do jogador dessincronizado com o servidor.

- **#486. `PartyFarmEventService` Não Possui Agendador Ativo nem Faz Parse de `partyfarm.xml`:**
  - **L2JLopez:** `com.lopez.l2j.game.event.PartyFarmEventService` (linhas 59–245).
  - **Referência Dream/Lucera:** `com.dream.game.model.entity.events.partyfarm.PartyFarm` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** O construtor de `PartyFarmEventService` hardcoda dias/horários e 3 spawns fixos sem ler `data/xml/partyfarm.xml`. Além disso, `checkSchedule()` nunca é agendado por nenhum `@Scheduled` ou `ScheduledExecutorService`, e quando `checkSchedule()` coloca o estado em `PREPARATION`, não há nenhuma task agendada para chamar `startEvent()` após `preparationMinutes` nem `endEvent()` após `durationMinutes`!

- **#487. Partidas de `TournamentService` e `ArenaDuelService` Ficam Presas Eternamente em `COUNTDOWN` (`startFight` Nunca é Chamado):**
  - **L2JLopez:** `com.lopez.l2j.game.service.TournamentService` (linhas 404–431) e `com.lopez.l2j.game.service.ArenaDuelService` (linhas 263–285 e 373).
  - **Referência Dream/Lucera:** `ArenaDuel` / `Tournament` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** Tanto `TournamentService.tryMatchTeams()` (linha 404) quanto `ArenaDuelService.tryMatchQueue()` (linha 263) criam a partida no estado `COUNTDOWN`, mas **nenhum dos dois serviços agenda ou chama `startFight(matchId)`**! Como `ArenaDuelService.onPlayerDeath()` (linha 373) exige `if (match.getState() == ArenaDuelState.FIGHTING)`, quando um jogador mata o oponente na arena a morte é ignorada e a partida nunca termina! Além disso:
    1. `TournamentService` usa `m.teleport(...)` em memória sem enviar `TeleportToLocation`, não define aura de time (`m.team(1/2)`), não envia pacote `Revive` em `returnAndHeal()` e não persiste a recompensa no SQL;
    2. `ArenaDuelService.finishMatch()` (linhas 360–363) possui um loop vazio `// Spectator cleanup` que abandona espectadores presos na instância `duelInstanceId`, e `awardWinner()` (linhas 480–509) faz `INSERT INTO items` de um novo registro de Adena (`57`) em vez de somar à pilha existente via `InventoryService.addItem()`.

- **#488. `LotteryService` Opera 100% em Memória Sem Usar a Tabela `games` (`V82__games.sql`) nem Criar Item Físico `4442`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.LotteryService` (linhas 1–263) e `src/main/resources/db/migration/V82__games.sql`.
  - **Referência Dream/Lucera:** `com.dream.game.model.entity.sevensigns.Lottery` (`L2JDreamV2`) / `ru.catssoftware.gameserver.instancemanager.games.Lottery` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `LotteryService` guarda bilhetes e sorteios apenas em mapas em memória, ignorando completamente a tabela `games` (`V82__games.sql`), não possui agendador semanal para encerrar vendas e realizar o sorteio, e não cria o item físico Lottery Ticket (`itemId 4442` com `customType1` e `customType2` codificando a máscara de números escolhidos e o número do concurso) no inventário do jogador.

- **#489. `MonsterRaceService` Opera 100% em Memória Sem Usar `monster_race_bets`/`monster_race_history` e Permite Forçar Corrida via Bypass:**
  - **L2JLopez:** `com.lopez.l2j.game.service.MonsterRaceService` (linhas 1–401) e `com.lopez.l2j.network.game.handler.bypass.BypassEventsAndPveHandler` (linhas 194–223).
  - **Referência Dream/Lucera:** `com.dream.game.idfactory / MonsterRace` (`L2JDreamV2`) / `ru.catssoftware.gameserver.MonsterRace` (`L2JLucera2`).
  - **Diagnóstico Acionável:** `MonsterRaceService` não persiste nada no banco SQL, não cria bilhetes físicos (`itemId 4443`) no inventário, não instancia os 8 NPCs corredores da pista de Monster Derby nem transmite o pacote `MonRaceInfo` com o ciclo oficial de 20 minutos, e expõe o bypass `mr_run` (`runRaceAndSettlePlayer`) que permite a qualquer jogador disparar e liquidar a corrida instantaneamente a qualquer momento.

- **#490. `FishingChampionshipService` Não Salva Peixes no SQL e Apaga o Ranking (`DELETE FROM fishing_championship`) Antes da Coleta de Prêmios:**
  - **L2JLopez:** `com.lopez.l2j.game.service.FishingChampionshipService` (linhas 92–288).
  - **Referência Dream/Lucera:** `com.dream.game.manager.fishingChampionshipManager` (`L2JDreamV2`) / `ru.catssoftware.gameserver.instancemanager.fishingChampionshipManager` (`L2JLucera2`).
  - **Diagnóstico Acionável:** Existem três bugs graves:
    1. `submitFish()` e `registerCatch()` nunca chamam `saveToDb()`, portanto peixes pescados não são gravados em `fishing_championship` (`V73`);
    2. `finishCycle()` (linhas 279–282) executa `rankings.clear()` e `DELETE FROM fishing_championship` assim que o ciclo semanal termina — consequentemente, quando os vencedores vão ao Fisherman (`claimReward`) buscar sua recompensa após o término do campeonato, `getWinnerRank()` retorna `0` e ninguém recebe prêmio algum (no Dream/Lucera, `win_players` é mantido separado de `tmp_players` até a semana seguinte);
    3. `loadFromDb()` lê `finish_date` de `fishing_championship_date` (`V74`), mas ignora o valor lido (`var unused = ...`) em vez de restaurar o timestamp de encerramento do ciclo.

- **#491. `WeddingService.engage()` Força Noivado Sem Confirmação Mútua (`ConfirmDlg`) e Ignora Regras de Cerimônia/Custos/Presentes:**
  - **L2JLopez:** `com.lopez.l2j.game.service.WeddingService` (linhas 99–186 e 288–450) e `com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler` (linhas 163–258).
  - **Referência Dream/Lucera:** `CoupleManager` e `Wedding.java` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Quando um jogador digita `.engage <alvo>`, `VoicedGeneralHandler` chama `weddingService.engage(player.objectId(), target.objectId())` diretamente, forçando o noivado no alvo sem enviar `ConfirmDlg` nem exigir consentimento mútuo! Além disso, os métodos `canEngageWithCeremonyRules()`, `getWeddingPrice()`, `getMarriedNickColor()`, `getCeremonyGiftsForPlayer()` (Cupid's Bow `9140`) e `calculateDivorceCost()` foram escritos nas linhas 288–450 de `WeddingService`, mas **nunca são chamados** em `engage()`, `marry()` ou `divorce()`.

- **#492. `AioService.purchaseAioPackage()` Não Persiste Status AIO nem Skills no Banco e Não Entrega AIO Dual (`9209`):**
  - **L2JLopez:** `com.lopez.l2j.game.service.AioService` (linhas 197–277 e 558–606).
  - **Referência Dream/Lucera:** `L2PcInstance.setAio` / `AioItem` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** Em `purchaseAioPackage()` (linhas 558–606):
    1. O custo em moedas é removido apenas da memória (`inv.destroyItemByItemId`) sem atualizar a tabela `items` via `InventoryService.consumeItem()`;
    2. O status AIO é aplicado apenas em memória (`player.aio(true)`), esquecendo de chamar `saveAio(player.objectId(), true, aioEnd)` para persistir nas colunas `aio` e `aio_end` da tabela `characters`;
    3. As skills de AIO são inseridas apenas em `player.skills()` sem chamar `skillService.addSkill()` e sem enviar `SkillList`;
    4. Nem `setAio()` nem `purchaseAioPackage()` entregam/removem a arma AIO Dual (`Config.ALLOW_AIO_DUAL` / `AIO_DUAL_ID = 9209`).

- **#493. `VipService` Não Transmite `UserInfo`/`CharInfo` ao Ativar/Remover VIP nem Faz Broadcast de `collectLoginAnnouncements()`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.VipService` (linhas 86–154 e 431–442).
  - **Referência Dream/Lucera:** `L2PcInstance.setVip` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** `setVip()` e `removeVip()` alteram `player.nameColor()` e `player.titleColor()` em memória, mas nunca enviam `UserInfo` ao jogador nem fazem broadcast de `CharInfo` para os jogadores ao redor. Além disso, `collectLoginAnnouncements()` apenas escreve no `log.info` sem enviar pacote de anúncio (`CreatureSay`) aos jogadores online.

- **#494. `PcCafeService` Não Persiste `pc_point` na Tabela `characters` nem Envia Pacote `ExPCCafePointInfo` (`FE:31`):**
  - **L2JLopez:** `com.lopez.l2j.game.service.PcCafeService` (linhas 1–273) e `src/main/resources/db/migration/V50__characters.sql` (coluna `pc_point`).
  - **Referência Dream/Lucera:** `PcCafe` / `ExPCCafePointInfo` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** `PcCafeService` mantém os pontos PC Bang apenas em `Map<Integer, Integer> playerPoints = new ConcurrentHashMap<>()` em memória: nunca carrega nem salva a coluna `characters.pc_point` no SQL (`JdbcCharacterRepository`), e quando o jogador ganha ou gasta pontos (`addPoints`, `deductPoints`, `exchangePoints`), nunca envia o pacote `ExPCCafePointInfo` (`0xFE:0x31`) para atualizar o painel de PC Bang Points na tela do cliente Interlude.

- **#495. `SchemeBufferService` Destrói Adena Apenas em Memória e `applyPetStatBoost()` Multiplica Status de Summon Permanentemente:**
  - **L2JLopez:** `com.lopez.l2j.game.service.SchemeBufferService` (linhas 769, 890–943 e 945–957).
  - **Referência Dream/Lucera:** `NpcBuffer` / `SchemeBuffer` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Dois bugs graves:
    1. `consumeAdena()` (linha 949) e `applyLuceraMenuId()` (linha 769) chamam `player.inventory().destroyItemByItemId(57, cost)` apenas em memória sem atualizar o SQL via `InventoryService.consumeItem()` e sem enviar `InventoryUpdate`;
    2. `applyPetStatBoost()` (linhas 890–943) multiplica diretamente os atributos base do `SummonInstance` (`pet.pAtk((int)(pet.pAtk() * 1.15))`, `pet.pDef(...)`, `pet.mAtk(...)`, `pet.maxHp(...)`) a cada buff aplicado sem registrar efeito temporário com expiração — clicar repetidamente no buffer multiplica exponencialmente os atributos do pet até o overflow de `int`!

- **#496. `BotsPreventionService` Sem Task de Expiração, Sem Punição ao Errar Captcha e Sem Geração de Imagem `PledgeCrest`:**
  - **L2JLopez:** `com.lopez.l2j.game.service.BotsPreventionService` (linhas 1–276) e `com.lopez.l2j.network.game.handler.NpcBypassPacketHandler` (linhas 1445–1456).
  - **Referência Dream/Lucera:** `com.dream.game.manager.BotsPreventionManager` (`L2JDreamV2`).
  - **Diagnóstico Acionável:**
    1. Não há task agendada verificando desafios expirados (`isExpired()`), de modo que se o bot simplesmente ignorar a janela HTML, nunca sofre punição;
    2. Em `NpcBypassPacketHandler` (linhas 1445–1456), se o jogador clicar na resposta errada antes do timeout, nenhuma punição é aplicada e ele pode tentar infinitas vezes;
    3. As punições `TOWN`, `JAIL` e `BAN` (linhas 1450–1455 de `NpcBypassPacketHandler`) não estão implementadas (apenas enviam uma mensagem de chat);
    4. `buildCaptchaHtml()` (linha 163) envia os dígitos do código como texto puro `<h1>123</h1>` no HTML (trivialmente lido por qualquer bot) em vez de gerar uma imagem DDS e enviá-la via pacote `PledgeCrest` como faz `BotsPreventionManager` no `L2JDreamV2`.

- **#497. `OfflineTradeService` Não Restaura nem Faz Spawn dos `PlayerCharacter` no `GameWorld` ao Iniciar o Servidor:**
  - **L2JLopez:** `com.lopez.l2j.game.service.OfflineTradeService` (`loadOfflineTraders`, linhas 356–415).
  - **Referência Dream/Lucera:** `OfflineTradersTable` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** No startup do servidor, `loadOfflineTraders()` lê `character_offline` e `character_offline_shop` para o mapa `traders`, mas **nunca carrega o `PlayerCharacter` via `JdbcCharacterRepository` nem o adiciona em `GameWorld`** (`world.registerOfflinePlayer` / spawn no mapa sentado com `privateStoreType` configurado)! Consequentemente, todas as lojas offline salvas no banco ficam invisíveis no mundo após um restart do servidor.

- **#498. `BuffShopService` Não Faz Spawn de Lojas Offline no `GameWorld`, Perde Level das Skills no SQL e Não Atualiza Inventário/MP na Compra:**
  - **L2JLopez:** `com.lopez.l2j.game.service.BuffShopService` (linhas 494–531 e 900–965) e `com.lopez.l2j.network.game.handler.bypass.BypassBuffShopHandler` (linhas 398–433).
  - **Referência Dream/Lucera:** `BuffShopManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:**
    1. Assim como no `OfflineTradeService`, `BuffShopService.loadOfflineShops()` não instancia o `PlayerCharacter` vendedor no `GameWorld` ao reiniciar o servidor;
    2. Na linha 932 (`loadOfflineShops`), todas as skills carregadas de `character_offline_buffshop_skills` recebem `level = 1` e nome `"Skill #" + skillId` hardcoded em vez de preservar o nível real da skill do vendedor e resolver o nome na `SkillTable`;
    3. Em `purchaseBuffs()` / `BypassBuffShopHandler.handleBuy()`, a cobrança de itens não envia `InventoryUpdate` / `ItemList` ao comprador nem ao vendedor online, e nunca consome MP do vendedor (`Config.BUFF_SHOP_MP_CONSUME`).

---

## 5. Persistência SQL Ponta a Ponta (`db/migration/V1..V132` e `db/data/V1005..V1122` — `#499` a `#510`)

- **#499. Sete Tabelas de Buffer (`V10` a `V16`) e `V96__mods_buffer_skills.sql` (`V1096`) Totalmente Desconectadas do `SchemeBufferService`:**
  - **L2JLopez:** `src/main/resources/db/migration/V10__buffer_buff_list.sql`, `V11__buffer_configuration.sql`, `V12__buffer_hall_schemes.sql`, `V13__buffer_scheme_contents.sql`, `V14__buffer_scheme_list.sql`, `V15__buffer_schemes.sql`, `V16__buffer_vip_list.sql`, `V96__mods_buffer_skills.sql` e `db/data/V1096__seed_mods_buffer_skills.sql` vs `com.lopez.l2j.game.service.SchemeBufferService`.
  - **Referência Dream/Lucera:** `BufferSkillsTable` / `CharSchemesTable` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** As 8 tabelas de buffer migradas no Flyway (`buffer_buff_list`, `buffer_configuration`, `buffer_hall_schemes`, `buffer_scheme_contents`, `buffer_scheme_list`, `buffer_schemes`, `buffer_vip_list` e `mods_buffer_skills` com seed em `V1096`) possuem **zero referências SQL no código Java**. `SchemeBufferService` usa um `AVAILABLE_BUFFS` hardcoded em Java e salva esquemas apenas em `mods_buffer_schemes`, ignorando o catálogo de buffs, custos, grupos e restrições VIP populados em `mods_buffer_skills` (`V1096`).

- **#500. Tabelas `V27__char_templates.sql` (`V1027`) e `V60__class_list.sql` (`V1060`) Sem Leitura no Código Java:**
  - **L2JLopez:** `src/main/resources/db/migration/V27__char_templates.sql`, `V60__class_list.sql`, `db/data/V1027__seed_char_templates.sql`, `db/data/V1060__seed_class_list.sql` vs `com.lopez.l2j.game.model.PlayerClassTemplateTable`.
  - **Referência Dream/Lucera:** `CharTemplateTable` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** As tabelas `char_templates` e `class_list` são criadas e populadas pelo Flyway (`V27`/`V1027` e `V60`/`V1060`), mas têm **0 queries no Java**. `PlayerClassTemplateTable` deve carregar ou validar os atributos base (`STR`, `DEX`, `CON`, `INT`, `WIT`, `MEN`, `p_atk`, `p_def`, `m_atk`, `m_def`, `move_spd`, coordenadas de spawn inicial e itens iniciais) diretamente de `char_templates` e `class_list`.

- **#501. Tabelas `V20__castle_doorupgrade.sql` e `V25__castle_trapupgrade.sql` Sem Persistência em `SiegeService` / `CastleService`:**
  - **L2JLopez:** `src/main/resources/db/migration/V20__castle_doorupgrade.sql` e `V25__castle_trapupgrade.sql` vs `com.lopez.l2j.game.service.CastleService` e `SiegeService`.
  - **Referência Dream/Lucera:** `Castle.loadDoorUpgrade()` / `saveDoorUpgrade()` / `loadTrapUpgrade()` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** As tabelas `castle_doorupgrade` e `castle_trapupgrade` possuem **0 referências no código Java**. Quando um líder de clã dono de castelo compra upgrades de portas ou flame towers/traps no Chamberlain, os upgrades não são salvos nem recarregados dessas tabelas ao iniciar o servidor ou iniciar uma Siege.

- **#502. Tabela `V84__global_tasks.sql` (e Seed `V1084__seed_global_tasks.sql`) Completamente Ignorada:**
  - **L2JLopez:** `src/main/resources/db/migration/V84__global_tasks.sql` e `src/main/resources/db/data/V1084__seed_global_tasks.sql`.
  - **Referência Dream/Lucera:** `TaskManager` (`com.dream.game.manager.TaskManager` em `L2JDreamV2` / `ru.catssoftware.gameserver.taskmanager.TaskManager` em `L2JLucera2`).
  - **Diagnóstico Acionável:** A tabela `global_tasks` (populadas com tasks globais como `TaskSevenSignsUpdate`, `TaskOlympiadSave`, `TaskClanLeaderApply`, `TaskRestart`, `TaskJython`) possui **0 referências no Java**. Deve-se implementar o `GlobalTaskService` / `TaskManager` lendo `global_tasks` e atualizando `last_activation` no banco.

- **#503. Tabela `V102__passkey.sql` Ignorada por `WarehousePasskeyService`:**
  - **L2JLopez:** `src/main/resources/db/migration/V102__passkey.sql` vs `com.lopez.l2j.game.service.WarehousePasskeyService`.
  - **Referência Dream/Lucera:** `Passkey` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** A migration `V102__passkey.sql` cria a tabela dedicada `passkey`, mas `WarehousePasskeyService` nunca faz `SELECT`/`INSERT`/`UPDATE` nela (possui 0 queries SQL para `passkey`), gravando hashes duplicados em `character_variables` (`wh_passkey` e `WH_PASSKEY_HASH`).

- **#504. Tabela `V97__mods_voting_reward.sql` Recebe `INSERT`, mas `VoteRewardService` Nunca Faz `SELECT`:**
  - **L2JLopez:** `src/main/resources/db/migration/V97__mods_voting_reward.sql` e `com.lopez.l2j.game.service.VoteRewardService` (linhas 60–103).
  - **Referência Dream/Lucera:** `VoteReward` (`L2JDreamV2`).
  - **Diagnóstico Acionável:** `VoteRewardService.recordVote()` executa `INSERT INTO mods_voting_reward (data, scope, time) VALUES (:data, :scope, :time)` na linha 92, porém **nunca executa `SELECT`** na tabela `mods_voting_reward` (nem no startup nem em `canVote()`). Como `canVote()` consulta apenas o `ConcurrentHashMap` em memória `cooldowns`, qualquer jogador pode votar e resgatar recompensas repetidamente sempre que o servidor reiniciar.

- **#505. Tabela `V101__olympiad_nobles_eom.sql` Sem Persistência no Encerramento de Ciclo do `OlympiadManager`:**
  - **L2JLopez:** `src/main/resources/db/migration/V101__olympiad_nobles_eom.sql` vs `com.lopez.l2j.game.olympiad.OlympiadManager`.
  - **Referência Dream/Lucera:** `Olympiad.java` (`olympiad_nobles_eom` em `L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** A tabela `olympiad_nobles_eom` (End of Month snapshot de pontos e lutas do mês anterior para cálculo e entrega de Noblesse Gate Pass no Monument of Heroes) possui **0 referências no Java**. `OlympiadManager` nunca copia os dados de `olympiad_nobles` para `olympiad_nobles_eom` ao fechar o período da Olimpíada.

- **#506. Tabela `V8__auction_watch.sql` Sem Persistência no `AuctionService`:**
  - **L2JLopez:** `src/main/resources/db/migration/V8__auction_watch.sql` vs `com.lopez.l2j.game.service.AuctionService`.
  - **Referência Dream/Lucera:** `AuctionManager` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Enquanto `auction` (`V6`) e `auction_bid` (`V7`) são consultadas pelo `AuctionService`, a tabela `auction_watch` (`V8`) possui **0 referências no Java**, impedindo que observadores de leilões de ClanHall sejam persistidos e notificados.

- **#507. Tabelas `V2__account_data.sql` e `V29__character_data.sql` Sem Repositório JDBC Conectado:**
  - **L2JLopez:** `src/main/resources/db/migration/V2__account_data.sql` e `V29__character_data.sql`.
  - **Referência Dream/Lucera:** `AccountData` / `CharacterData` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Ambas as tabelas (`account_data` para variáveis em nível de conta como créditos/preferências globais, e `character_data` para dados legados de personagem do Dream) possuem **0 referências no Java**. `AccountDataService` deve ser conectado a `account_data` (ou unificado com `character_variables`).

- **#508. Tabela `V83__gameservers.sql` (e Seed `V1083__seed_gameservers.sql`) Não Utilizada pelo LoginServer:**
  - **L2JLopez:** `src/main/resources/db/migration/V83__gameservers.sql` e `src/main/resources/db/data/V1083__seed_gameservers.sql`.
  - **Referência Dream/Lucera:** `GameServerTable` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** A tabela `gameservers` (`server_id`, `hexid`, `host`) é criada e populada pelo Flyway (`V83`/`V1083`), mas possui **0 referências no código Java**, não sendo validada nem atualizada pelo módulo de Login/GameServer registration.

- **#509. Tabelas `V64__custom_armorsets.sql`, `V92__items_premium.sql` e `V99__obj_restrictions.sql` Sem Leitura no Código Java:**
  - **L2JLopez:** `src/main/resources/db/migration/V64__custom_armorsets.sql`, `V92__items_premium.sql` e `V99__obj_restrictions.sql`.
  - **Referência Dream/Lucera:** `ArmorSetsTable`, `ItemsTable`, `ObjectRestrictions` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:**
    1. `ArmorSetsTable` nunca carrega `custom_armorsets` (`V64`);
    2. `JdbcItemTemplateTable` carrega `custom_armor` (`V63`), `custom_etcitem` (`V66`) e `custom_weapon` (`V70`), mas nunca lê `items_premium` (`V92`);
    3. `obj_restrictions` (`V99` — bloqueios temporários/permanentes de objetos/jogadores do L2JDream/Lucera) possui **0 referências no Java**.

- **#510. Tabelas Legadas Órfãs `V33__character_items.sql`, `V40__character_quests_item.sql` e `V119__temporaryobjecttable.sql` Sem Integração:**
  - **L2JLopez:** `src/main/resources/db/migration/V33__character_items.sql`, `V40__character_quests_item.sql` e `V119__temporaryobjecttable.sql`.
  - **Referência Dream/Lucera:** `CharItems` / `QuestItems` / `TemporaryObjectTable` (`L2JDreamV2` / `L2JLucera2`).
  - **Diagnóstico Acionável:** Essas três tabelas têm **0 referências no código Java** do `L2JLopez`. `character_items` (`V33` — entrega de itens pendentes por fila externa/site/donate para personagens offline) deve ser consumida no login do jogador (`EnterWorldPacketHandler` / `CharacterItemDeliveryService`) movendo os itens para `items` via `InventoryService.addItem()`, enquanto `character_quests_item` e `temporaryobjecttable` devem ser integradas ou deprecadas de forma limpa.

---


