# AUDITORIA PROFUNDA: GAPS REAIS ENTRE L2JLOPEZ VS L2JDREAM V2 & L2JLUCERA 2

> **Diagnóstico Honesto ("Por que o L2JLopez parece incompleto dentro do jogo mesmo tendo 1.089 testes unitários?")**
>
> A varredura completa do código-fonte de [`L2JLopez`](file:///d:/Cristiano/Lineage/L2JLopez) frente ao [`L2JDreamV2`](file:///d:/Cristiano/Lineage/L2JDreamV2) e ao [`L2JLucera2`](file:///d:/Cristiano/Lineage/L2JLucera2) revelou o verdadeiro motivo: **existe um abismo entre "existir a classe Java testada em teste unitário" e "estar conectado ponta a ponta nos pacotes, opcodes, bypasses de NPCs e XMLs quando um jogador real clica dentro do cliente Interlude 746"**.
>
> Muitos subsistemas foram criados em `com.lopez.l2j.game.*` (com testes unitários chamando seus métodos diretamente), mas **não foram plugados** em [`GameClientPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameClientPacket.java), [`GameServerPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameServerPacket.java), [`GameSession.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/GameSession.java) ou [`NpcBypassPacketHandler.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/handler/packet/NpcBypassPacketHandler.java) — ou pior, os handlers de NPC/Admin enviam apenas mensagens de chat falsas (`CreatureSay`) sem chamar o serviço real!

---

## 1. Bugs Críticos de Protocolo, Opcodes Invertidos e Pacotes Sem Roteamento

### 1.1. `RequestAcquireSkill (0x6c)` Decodificado, Mas Nunca Chamado no `switch(pkt)`!
- **Onde está o bug**: Em [`GameClientPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameClientPacket.java#L504), o opcode `0x6c` é decodificado como `RequestAcquireSkill`. Em [`GameSession.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/GameSession.java#L9001), existe o método `onAcquireSkill(RequestAcquireSkill p)`. **Porém, no `switch (pkt)` de `GameSession.handle()` (linhas 3517–3789), esqueceram de colocar `case GameClientPacket.RequestAcquireSkill p -> onAcquireSkill(p);`!**
- **Impacto Real In-Game**: Quando o jogador abre a janela de aprender skills em qualquer NPC Trainer e clica em **"Learn Skill"**, o pacote cai no `default -> log.debug("Pacote {} recebido e nao tratado")` e **nada acontece**!
- ** agravante**: `onAcquireSkillInfo` (linha 8971) e `onAcquireSkill` (linha 9005) têm `if (... || p.skillType() != 0) return;`, bloqueando o aprendizado nativo de **Fishing Skills (`skillType == 1`)** e **Clan Skills (`skillType == 2`)**, além de `SkillTreeTable` nunca carregar `data/xml/player/skills/fishing_skill_tree.xml` nem `pledge_skill_tree.xml`!

### 1.2. Opcodes de Clã (`0x3c`, `0x52`, `0x53`) Invertidos em `GameClientPacket.java`
- **Onde está o bug**: Em [`GameClientPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameClientPacket.java#L495-L496):
  - No protocolo Interlude 746:
    - `0x3c` = `RequestPledgeMemberList`
    - `0x52` = `RequestReplySurrenderPledgeWar`
    - `0x53` = `RequestSetPledgeCrest`
  - No `L2JLopez`:
    - `0x3c` não existe (cai em `Unknown`).
    - `0x52` foi mapeado errado como `readSetPledgeCrest(r)`.
    - `0x53` foi mapeado errado como `new RequestPledgeMemberList()`.
- **Impacto Real In-Game**: Abrir a janela de clã (`0x3c`) não carrega a lista de membros, e tentar enviar uma bandeira/crest de clã (`0x53`) descarta os bytes da imagem!

### 1.3. Sub-Opcodes de Cursed Weapons (`0xd0:0x22` e `0xd0:0x23`) Mapeados com IDs de Servidor (`0x45` e `0x46`)
- **Onde está o bug**: Em [`GameClientPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameClientPacket.java#L713-L714), o cliente envia `0xd0:0x22` (`RequestCursedWeaponList`) e `0xd0:0x23` (`RequestCursedWeaponLocation`), mas o `L2JLopez` colocou `case 0x45` e `case 0x46` (que são os sub-opcodes de resposta `0xfe:0x45` e `0xfe:0x46` do servidor!).
- **Impacto Real In-Game**: O botão de Zariche/Akamanah no mini-mapa nunca funciona.

### 1.4. `ChatPacketHandler` Não Verifica `active.isHero()` no Hero Chat (`%`)
- **Onde está o bug**: Em [`ChatPacketHandler.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/handler/packet/ChatPacketHandler.java#L175-L187), o canal `CreatureSay.HERO` (`17`, `%`) verifica apenas o cooldown, **sem checar `if (!active.isHero() && !active.isGm())`**!
- **Impacto Real In-Game**: Qualquer jogador nível 1 recém-criado consegue falar em azul global para o servidor inteiro usando `%`!

---

## 2. Bytes Hardcoded como `0` em `GameServerPacket.java` (UI Quebrada no Cliente)

1. **`UserInfo (0x04)` & `CharInfo (0x03)` ([`GameServerPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameServerPacket.java#L292-L322))**:
   - `clanPrivileges` está hardcoded como `w.writeD(0)`! Isso faz o cliente Interlude **desabilitar todos os botões da janela de clã** (`Alt+N`) para líderes e membros (convidar, expulsar, editar título, crest, guerra, privilégios)!
   - `mountType` e `mountNpcId` estão hardcoded como `0` (montar em Strider/Wyvern não aparece visualmente).
   - `largeClanCrestId`, `pledgeClass`, `cursedWeaponLevel`, `relation` estão hardcoded como `0`.
   - Em `CharInfo`, `isHero`, `isNoble` e `isDead` estavam hardcoded como `0` (outros jogadores não veem a aura de Hero de quem passa por perto!).
2. **`Die (0x06)` & `onRestartPoint (0x6d)` ([`GameServerPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameServerPacket.java#L1084-L1122), [`GameSession.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/GameSession.java#L7285))**:
   - Quando o jogador morre, `Die` envia `hasHideaway = 0`, `hasCastle = 0`, `toSiegeHQ = 0` hardcoded, nunca mostrando os botões **"To Clan Hall"**, **"To Castle"** ou **"To Siege HQ"**. E `onRestartPoint` ignora `p.pointType()`, teleportando sempre para a vila mais próxima!
3. **`HennaInfo (0xe4)` ([`GameServerPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameServerPacket.java#L2447-L2475))**:
   - Os 6 modificadores de atributos (`INT`, `STR`, `CON`, `MEN`, `DEX`, `WIT`) na janela de Dyes/Hennas estão hardcoded como `w.writeC(0)` em vez de ler `active.hennaINT()`, etc.
4. **`WareHouseDepositList (0x41)` & `WareHouseWithdrawalList (0x42)` ([`GameServerPacket.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/packet/GameServerPacket.java#L1999-L2071))**:
   - Hardcodeiam `w.writeD(0)` para Augmentation e Mana de Shadow Item nos itens do Warehouse!
5. **`GMViewCharacterInfo (0x8f)`, `GMViewSkillInfo (0x91)`, `GMViewQuestInfo (0x93)`, `GMViewWarehouseWithdrawList (0x95)`**:
   - São stubs que escrevem atributos fixos (`10`, `100`, `300`) e `0` skills, `0` quests e `0` itens de warehouse quando o GM inspeciona um jogador!
6. **Pacotes de Manor (`ExShowSeedInfo`, `ExShowCropInfo`, `ExShowManorDefaultInfo`, `ExShowProcureCropDetail`)**:
   - São stubs vazios que sempre escrevem `0` entradas em vez de ler `CastleManorManager`!

---

## 3. Serviços Que Existem no Código, Mas Têm Bypasses/Comandos "Fake" (Teatro!)

Esta é a maior causa da sensação de "sistema oco": **a lógica foi programada nos Services, mas os Handlers de NPC/Admin mandam apenas texto falso no chat!**

1. **Grand Olympiad (`handleOlympiadNoble` em [`NpcBypassPacketHandler.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/network/game/handler/packet/NpcBypassPacketHandler.java#L2878-L2898) e `AdminPacketHandler.java`)**:
   - [`OlympiadManager.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/game/olympiad/OlympiadManager.java) e `OlympiadGameService.java` existem e têm fila classed/non-classed, cálculo de pontos, arenas e entrega de Hero.
   - **PORÉM**, quando o jogador clica no Olympiad Manager (`31688`), `handleOlympiadNoble` faz apenas:
     ```java
     case 4 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Voce foi registrado nas batalhas sem restricao de classe."));
     ```
     **Sem nunca chamar `ctx().olympiad().registerForQueue(...)`!** E `OlympiadManager` não possui task periódica chamando `processMatchmaking()` / `OlympiadGameService`!
   - Da mesma forma, `//oly_start`, `//oly_stop`, `//add_oly_points` em `AdminPacketHandler.java` apenas enviam `CreatureSay` sem chamar `ctx.olympiad()`!
2. **100 Arquivos XML de Status de Armas/Armaduras (`data/xml/stats/armor/*.xml`, `weapon/*.xml`, `etcitem/*.xml`) NUNCA Carregados!**:
   - [`JdbcItemTemplateTable.java`](file:///d:/Cristiano/Lineage/L2JLopez/src/main/java/com/lopez/l2j/game/item/JdbcItemTemplateTable.java) lê apenas as colunas básicas das tabelas SQL (`armor`, `weapon`, `etcitem`), **ignorando completamente os 100 arquivos XML em `data/xml/stats/`**!
   - **Impacto**: Nenhum modificador `<for>` (`<add>`, `<mul>`, `<enchant>`) de itens, bônus de MP de robes, reuse delay de arcos, shield def rate ou Special Abilities (Acumen, Empower, Haste, Focus, Health) definidos nos XMLs de itens estava sendo aplicado aos equipamentos!
3. **Pets (`PetService` & `PetEvolveService` 100% Desconectados do Jogo)**:
   - `PetService.java` (367 linhas) e `PetEvolveService.java` (104 linhas) têm persistência SQL na tabela `pets`, evolução de Wolf/Strider/Baby Pet, fome e experiência, **mas têm ZERO chamadas em `src/main/java`**! E os bypasses `evolve` e `exchange` nos Pet Managers (`30731`, etc.) não funcionam (`evolve` dá `ActionFailed` e `exchange` abre a Multisell #3 do Mammon!).
4. **Grand Bosses Spawnados Duplicados no Boot (`GrandBossManager` vs Lair Services)**:
   - `GrandBossManager.init()` dá spawn direto em todos os 10 Grand Bosses no boot via `SpawnService`, ignorando as salas adormecidas! Enquanto isso, `QueenAntService.spawnQueenAnt()` (com Nurses/Larva/Royal Guards), `CoreAndOrfenService.spawnCore/Orfen()`, `ZakenService.spawnZaken()`, `VanHalterService` e `FrintezzaService.startFight()` nunca eram acionados automaticamente no fluxo real!
   - E em `ZakenAI` e `OrfenAI`, o teleporte mutava apenas `player.x/y/z` na memória sem enviar o pacote `TeleportToLocation` para o cliente 3D!
5. **Four Sepulchers & Dimensional Rift Incompletos no Fluxo Real**:
   - Em `FourSepulchersService`, matar os monstros da Sala 1 nunca chama `clearRoom()` / `openNextRoomDoor()`, prendendo a party na primeira sala.
   - Em `NpcBypassPacketHandler`, `changeriftroom` e `exitrift` enviavam apenas texto em vez de chamar `ctx().dimensionalRift().manualJump()` / `leaveRift()`.
6. **Cerco de Castelo (`SiegeService`) — Portas e Torres Não Atacáveis**:
   - `ActionPacketHandler.handleAttackRequest` e `MagicSkillPacketHandler` só buscavam alvos em `world.npc()` e `world.player()`, ignorando `DoorInstance`! Jogadores não conseguiam atacar portões de castelo/fortaleza! E `NpcTemplate.isAttackable()` não incluía `L2ControlTower` nem `L2FlameTower`!
7. **Festival of Darkness (`SevenSignsManager`) Sem Monstros**:
   - O Festival Guide cobrava as Seal Stones e teleportava o jogador para a arena do Festival, mas **nenhum monstro do Festival era spawnado** na sala!
8. **46 Bypasses de NPCs Nativos em `data/html/**/*.htm` Caindo em `ActionFailed`**:
   - **Adventurer Guildsman**: `raidInfo` (informações de Raid Bosses por nível).
   - **Pet Manager**: `evolve`, `exchange`, `restore`.
   - **Wyvern Manager**: `RideWyvern`.
   - **Castle Chamberlain**: `manage_functions`, `functions`, `manage`, `manage_vault`, `tax_setup`, `tax_set`, `siege_change`, `doors_update`, `traps_update`, `products`, `list_siege_clans`, `manor`, `receive_report` (com dados reais do castelo), `banish_foreigner` (teleportando invasores de verdade).
   - **Clan Hall Manager**: `functions`, `manage`, `manage_vault`, `door`, `list_back`, `edit_other`, `edit_recovery`, `deco`.
   - **Fortress Manager**: `functions`, `manage`, `manage_functions`, `manage_vault`, `rewards`, `blood`, `supply`, `treport`, `register`.
   - **Village Master (`9000_clan` / `9001_alliance`)**: `learn_clan_skills`, `create_academy`, `create_royal`, `create_knight`, `assign_subpl_leader`, `rename_pledge`, `create_ally`, `dissolve_ally`, `change_clan_leader`, `recover_clan`.
   - **Mercenary Manager**: `hire`.
   - **Fisherman**: `fishingChampionship`, `fishingReward`.
   - **Monster Race Manager (`30995`)**: `BuyTicket`, `ShowOdds`, `CalculateWin`, `viewHistory`.
   - **Doormen**: `open_near_doors`, `close_near_doors`, `instant_teleport`.
   - **Warehouse**: `WithdrawSortedP`, `WithdrawSortedC`, `WithdrawSortedF`.

---

## 4. Pacotes Client/Server, UserCommands, Items e Skills Faltantes

### 4.1. UserCommands (`RequestUserCommand 0xaa` em `GameSession.onUserCommand`)
Dos 15 comandos nativos do cliente Interlude, apenas 4 (`0` `/loc`, `52` `/unstuck`, `77` `/time`, `109` `/olympiadstat`) estavam implementados. Faltavam **11**:
- `61` (`/mount`), `62` (`/dismount`), `81` (`/partyinfo`), `88` (`/attacklist`), `89` (`/underattacklist`), `90` (`/warlist`), `92` (`/channelcreate`), `93` (`/channeldelete`), `96` (`/channelleave`), `97` (`/channelinfo`), `99`/`100` (`/siegestatus` / `/clanpenalty`), `114` (`/instancezone` / `/buffshop`).

### 4.2. Pacotes Essenciais de Clã, Party, Duel, ConfirmDlg, Quest, Crystallize e Recipe Shop
- **Confirmação (`ConfirmDlg 0xed` & `DlgAnswer 0xc5`)**: Necessário para Ressurreição (`1510`), Summon Friend (`1842`) e pedidos de casamento/porta.
- **Clã (`Alt+N`)**: `0x24` `RequestJoinPledge` + `AskJoinPledge (0x32)`, `0x25` `RequestAnswerJoinPledge`, `0x26` `RequestWithdrawalPledge`, `0x27` `RequestOustPledgeMember`, `0x3c` `RequestPledgeMemberList`, `0x55` `RequestGiveNickName`, `0xd0:0x10` `RequestExPledgeCrestLarge` + `ExPledgeCrestLarge (0xfe:0x28)`, `0xd0:0x11` `RequestExSetPledgeCrestLarge`, `0xd0:0x1a` `RequestPledgePowerGradeList` + `PledgePowerGradeList (0xfe:0x3b)`, `0xd0:0x1b` `RequestPledgeMemberPowerInfo` + `PledgeReceivePowerInfo (0xfe:0x3c)`, `0xd0:0x1c` `RequestPledgeSetMemberPowerGrade`, `0xd0:0x1d` `RequestPledgeMemberInfo` + `PledgeReceiveMemberInfo (0xfe:0x3d)`, `0xd0:0x1e` `RequestPledgeWarList` + `PledgeReceiveWarList (0xfe:0x3e)`.
- **Quests, Crystallize, SkillCoolTime, Duelos e Combate**:
  - `0x64` `RequestQuestAbort` (abortar quest na janela `Alt+U`).
  - `0x72` `RequestCrystallizeItem` (cristalizar itens Grade D..S com Dwarven Crystallize).
  - `0x9d` `RequestSkillCoolTime` + `SkillCoolTime (0xc1)`.
  - `MagicSkillLaunched (0x76)`, `RelationChanged (0xce)`, `Ride (0x86)`, `ExShowScreenMessage (0xfe:0x38)`.
  - Duelos Retail (`0xd0:0x27` `RequestDuelStart`, `0xd0:0x28` `RequestDuelAnswerStart`, `0xd0:0x30` `RequestDuelSurrender`, `ExDuelAskStart`, `ExDuelReady`, `ExDuelStart`, `ExDuelEnd`, `ExDuelUpdateUserInfo`).

### 4.3. Item Handlers & Skill/AI Mechanics Faltantes
- **Itens**: Beast SoulShot/SpiritShot (`6645`, `6646`, `6647`), FishShots (`6535`–`6540`), PetFood (`2515`, `4038`, `5168`, `5169`, `7582`), Calculator (`4393`), RollingDice (`4625`–`4628`), WorldMap (`1665`, `1863`), SevenSignsRecord (`5707`), Pagan/Door Keys (`8056`, `8273`–`8275`), Firework (`6403`, `6406`, `6407`), CrystalCarol (`4410`–`4417`, `5562`–`5566`), MysteryPotion (`5234`), Herbs (`8600`–`8614`), Hot Springs Remedies (`8030`–`8033`).
- **Skills & AI**:
  - `SILENT_MOVE (221)` não verificado em `NpcAiService.checkAggroAroundPlayers()` (monstros agressivos atacavam jogadores com Silent Move ligado!).
  - `FAKE_DEATH (60)` não verificado em `NpcAiService` nem em `RequestActionUse (2/3)`.
  - Skills de Pesca (`1312` Fishing, `1313` Pumping, `1314` Reeling) não roteadas em `MagicSkillPacketHandler` para `FishingService`.
  - `RESURRECT` / Blessed Scroll of Resurrection (`3936`) não restauravam XP perdido nem enviavam `ConfirmDlg`.

---

## 5. Plano de Execução Imediata (Ondas de Implementação Real no `L2JLopez`)

Estamos executando agora mesmo as correções estruturais para eliminar esses gaps no código do `L2JLopez`:

1. **Onda 1 — Protocolo, Opcodes, `GameServerPacket` e Aprendizado de Skills (Normal, Pesca e Clã)**:
   - Conectar `RequestAcquireSkill` no `switch (pkt)` de `GameSession.handle()`.
   - Carregar `fishing_skill_tree.xml` e `pledge_skill_tree.xml` em `SkillTreeTable` e suportar `skillType == 0, 1, 2` em `showSkillList`, `showFishingSkillList`, `showClanSkillList`, `onAcquireSkillInfo` e `onAcquireSkill`.
   - Corrigir os opcodes `0x3c` (`RequestPledgeMemberList`), `0x52` (`RequestReplySurrenderPledgeWar`), `0x53` (`RequestSetPledgeCrest`), `0xd0:0x22` (`RequestCursedWeaponList`) e `0xd0:0x23` (`RequestCursedWeaponLocation`).
   - Corrigir `UserInfo`, `CharInfo`, `Die`, `HennaInfo`, `WareHouseDepositList`, `WareHouseWithdrawalList`, `GMView*` e `onRestartPoint` (respawn em Clan Hall, Castelo, Siege HQ e Fixed).
   - Corrigir a validação de `active.isHero()` no Hero Chat (`%`) e adicionar os canais de chat de Party Room (`14`) e Command Channel (`15`, `16`).
2. **Onda 2 — Parser de XML de Status de Itens (`data/xml/stats/{armor,weapon,etcitem}/*.xml`)**:
   - Fazer `JdbcItemTemplateTable` carregar e mesclar os 100 arquivos XML de `data/xml/stats/{armor,weapon,etcitem}/`, aplicando todos os `<for>` (`StatFunc`) e `<skill>` aos templates de armas e armaduras!
3. **Onda 3 — Conexão Real dos Bypasses de NPCs e Comandos Admin aos Serviços**:
   - Conectar `handleOlympiadNoble` e `//oly_*` ao `OlympiadManager` real e iniciar o matchmaking periódico.
   - Conectar todos os 46 bypasses de NPCs faltantes (`raidInfo`, `evolve`, `exchange` de Pet, `RideWyvern`, `Chamberlain`, `ClanHallManager`, `FortressManager`, `VillageMaster` `learn_clan_skills`/`create_ally`/etc., `MercManager` `hire`, `Fisherman` `fishingChampionship`/`fishingReward`, `RaceManager` `BuyTicket`/`ShowOdds`, `Doormen`, `Manor`).
   - Permitir ataque a `DoorInstance` (portões de cerco) e `L2ControlTower` / `L2FlameTower`, conectar `FourSepulchers` (`clearRoom`/`openNextRoomDoor`), `DimensionalRift` (`changeriftroom`/`exitrift`), `Festival of Darkness` (spawn real de monstros e drop de Blood Offerings), e corrigir o teleporte de `ZakenAI` / `OrfenAI` e os spawns de `GrandBossManager`.
4. **Onda 4 — Pacotes Faltantes, 11 UserCommands, Item Handlers e Mecânicas de Skills/AI**:
   - Implementar os 11 `UserCommands` faltantes (`/mount`, `/dismount`, `/partyinfo`, `/attacklist`, `/underattacklist`, `/warlist`, `/channelcreate`, `/channeldelete`, `/channelleave`, `/channelinfo`, `/siegestatus`, `/clanpenalty`).
   - Implementar `RequestQuestAbort (0x64)`, `RequestCrystallizeItem (0x72)`, `ConfirmDlg (0xed)` + `DlgAnswer (0xc5)`, pacotes de Clã (`0x24..0x27`, `0x55`, `0xd0:0x10..0x1e`), `Ride (0x86)` e `SkillCoolTime (0xc1)`.
   - Implementar os Item Handlers (Beast Shots, FishShots, PetFood, Calculator, RollingDice, WorldMap, SevenSignsRecord, Keys, Firework, Carols, Herbs) e mecânicas de `SILENT_MOVE`, `FAKE_DEATH`, `Fishing (1312-1314)` e `RESURRECT`.
