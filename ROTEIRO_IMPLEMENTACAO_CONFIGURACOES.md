# 🗺️ Roteiro Mestre de Implementação Integral de Configurações: L2JLopez

> **Diretriz Mandatória do Projeto:**
> 1. **Nenhuma configuração será eliminada ou descartada.** Todos os 27 arquivos `.properties` e suas **1.531 propriedades** em `config/` serão preservados e atendidos.
> 2. **Sem duplicidades nem inconsistências:** Todas as configurações são mapeadas de forma centralizada e semântica no Spring Boot e no `Config.java`.
> 3. **Conexão Real no Gameplay:** Cada flag, multiplicador, limite ou tempo configurado deve obrigatoriamente influenciar a rotina de jogo correspondente (combate, pacotes, movimentação, drops, clãs, etc.).
> 4. **Qualidade & Estabilidade:** Cada módulo deve ser entregue com **100% de testes verdes** e compilação limpa em Java 21.

---

## 📊 1. Censo Geral de Configurações por Arquivo

| # | Arquivo `.properties` | Pasta | Qtd Props | Estado Atual | Domínio Funcional |
|---|---|---|:---:|:---:|---|
| **1** | `options.properties` | `config/game/main` | 120 | 🟢 100% Integrado | Geodata, Zonas, Chat, Petições, Clãs, Drops |
| **2** | `player.properties` | `config/game/main` | 107 | 🟢 100% Integrado | Atributos, Limites de Stats, Subclasses, Craft |
| **3** | `rates.properties` | `config/game/main` | 128 | 🟢 100% Integrado | Multiplicadores de Drop, Spoil, Enchant, VIP, Manor |
| **4** | `altgame.properties` | `config/game/main` | 52 | 🟢 100% Integrado | Decaimento de Itens, SA, Fórmulas, Cancel, Spoil |
| **5** | `gameserver.properties` | `config/game/main` | 161 | 🟢 100% Integrado | Safe Reboot, Rift, Respawn, Auditoria Ban/Jail |
| **6** | `network.properties` | `config/game/main` | 13 | 🟢 100% Integrado | Hostnames, Subnets, Protocolos 730-746, DB Pools |
| **7** | `npc.properties` | `config/game/main` | 34 | 🟢 100% Integrado | IA de NPCs, Aggro Range, Roaming, Buffers, Class Master |
| **8** | `bosses.properties` | `config/game/main` | 61 | 🟢 100% Integrado | Janelas de Respawn de Grand Bosses & Raid Bosses |
| **9** | `siege.properties` | `config/game/main` | 236 | 🟢 100% Integrado | Cercos de Castelos e Clan Halls Conquistáveis |
| **10** | `custom.properties` | `config/game/custom` | 31 | 🟢 100% Integrado | Starting Adena, Potion Power, AltSpawn, Custom Items |
| **11** | `mods.properties` | `config/game/custom` | 152 | 🟢 100% Integrado | Offline Trade, Banking, Champions, Captcha, DualBox |
| **12** | `add-on.properties` | `config/game/custom` | 52 | 🟢 100% Integrado | Sistema VIP, Quake PvP, Nicks e Títulos Coloridos |
| **13** | `aiox.properties` | `config/game/custom` | 30 | 🟢 100% Integrado | Sistema de AIO Buffer, Restrições e Buff Shop |
| **14** | `equipments.properties` | `config/game/custom` | 18 | 🟢 100% Integrado | Restrições de Equipamentos por Grade e Classe |
| **15** | `vote.properties` | `config/game/custom` | 6 | 🟢 100% Integrado | Premiação de Votos TopZone, HopZone e Network |
| **16** | `olympiad.properties` | `config/game/events` | 34 | 🟢 100% Integrado | Ciclos, Arenas, Pontos, Restrições e Heróis |
| **17** | `fun_events.properties` | `config/game/events` | 154 | 🟢 100% Integrado | Seven Signs, Loteria, PC Cafe, Mini-Games, Casamentos |
| **18** | `tvtevent.properties` | `config/game/events` | 26 | 🟢 100% Integrado | Team vs Team Event Engine Automatizada |
| **19** | `ctfevent.properties` | `config/game/events` | 26 | 🟢 100% Integrado | Capture the Flag Event Engine Automatizada |
| **20** | `dmevent.properties` | `config/game/events` | 20 | 🟢 100% Integrado | DeathMatch Event Engine Automatizada |
| **21** | `ArenaDuel.properties` | `config/game/events` | 8 | 🟢 100% Integrado | Sistema de Duelos em Arena 1x1 e Party |
| **22** | `tournament.properties` | `config/game/events` | 9 | 🟢 100% Integrado | Torneios Automatizados 2x2, 3x3, 5x5, 9x9 |
| **23** | `events_start.properties`| `config/game/events` | 9 | 🟢 100% Integrado | Schedulers e Horários Automáticos de Eventos |
| **24** | `access.properties` | `config/game/admin` | 13 | 🟢 100% Integrado | Níveis de GM, Snoop, OverEnchant e Auditoria |
| **25** | `revision.properties` | `config/game` | 2 | 🟢 100% Integrado | Versão de Build e Protocolos Suportados |
| **26** | `authserver.properties` | `config/login` | 19 | 🟢 100% Integrado | Autenticação, Proteção Bruteforce, Anti-DDoS |
| **27** | `network.properties` (login) | `config/login` | 10 | 🟢 100% Integrado | Portas, IPs e Conexões Dedicadas do Login |
| **TOTAL** | **27 Arquivos** | — | **1.531** | **🟢 100% Integrado (27/27)** | **4.668 propriedades indexadas** |

---

## 🏛️ 2. Arquitetura da Camada Unificada de Configuração

Para que nenhuma configuração seja perdida e todas as alterações em arquivos `.properties` reflitam instantaneamente:

```mermaid
graph TD
    A["Arquivos .properties em config/"] --> B["ConfigLoader (Leitor Multiformato UTF-8/ISO)"]
    B --> C["ConfigPropertiesEnvironmentPostProcessor"]
    C --> D["Spring Boot Environment (l2.*)"]
    C --> E["Config.java (Campos Tipados Estáticos de Alta Performance)"]
    D --> F["ServerProperties Records (@ConfigurationProperties)"]
    E --> G["Gameplay Engines (Combat, Chat, Zones, Quests, Drops, Sieges)"]
    F --> G
    H["Comando //reload config"] --> B
```

### Regras de ouro da arquitetura:
1. **Resolução Dinâmica Bidirecional:** Se uma propriedade não estiver explicitamente mapeada em `ServerProperties`, o método `Config.get(...)` busca transparentemente do cache normalizado do `ConfigLoader`.
2. **Zero Hardcoding:** Proibido valores fixos arbitrários em fórmulas de combate, taxas de enchant, limites de inventário ou tabelas de chat. Toda constante deve consultar o `Config`.
3. **Hot-Reload Preservado:** A chamada `Config.reload()` relê todos os 27 arquivos e reatribui os valores imediatamente sem derrubar o servidor.

---

## 🚀 3. Ondas de Implementação Executiva

---

### 🌊 ONDA 1: Chat, Comunicação, Social & Petições
* **Arquivos Atendidos:** `options.properties` (seções de Chat, Petitions, Mail).
* **Configurações Implementadas:**
  * `GlobalChat`, `TradeChat`: Suporte a modos `GLOBAL` (mundo inteiro), `REGION` (mesma região/vila), `OFF` e `GM`.
  * `UseChatFilter`, `ChatFilterChars`, `ChatFilterKarma`: Censura ativa de palavras proibidas com substituição e penalidade de karma.
  * `ChatLength`, `AllowMultiLineChat`: Limite máximo dinâmico de caracteres (substituindo o fixo 105 em `ChatPacketHandler.java`).
  * `ShoutChatReuseDelay`, `TradeChatReuseDelay`, `HeroChatReuseDelay`: Delays individuais de anti-flood por canal.
  * `ShoutChatLevel`, `TradeChatLevel`: Validação de nível mínimo para uso de `!` e `+`.
  * `PetitioningAllowed`, `MaxPetitionsPerPlayer`, `MaxPetitionsPending`, `SendPageOnPetition`, `PetitioningNeedGmOnline`: Sistema completo de chamados com fila para GMs.
* **Classes Afetadas:** `ChatPacketHandler.java`, `WordFilterTable.java`, `PetitionManager.java`, `CreatureSay.java`.
* **Critério de Aceite:** Testes unitários de roteamento de canais por distância/nível e validação de filtros e flood.

---

### 🌊 ONDA 2: Geodata, Movimentação, Zonas & Física de Combate
* **Arquivos Atendidos:** `options.properties` (seções Geodata, Zones, Server Optimization, Fines Bows).
* **Configurações Implementadas:**
  * `EnableGeoData`, `GeoDataRoot`, `GeoEngine`: *Já conectado na infraestrutura!*
  * `EnablePathFinding`, `PathFindingMode` (Pathnode / CellFinding), `MaxPathLength`: Algoritmo de rotas para NPCs contornando obstáculos.
  * `CoordSynchronize`: Validação de coordenadas cliente-servidor (-1, 1, 2).
  * `GeoCorrectZ`, `ZAxisDensity`: Correção de elevação de spawn de NPCs para evitar monstros flutuando ou sob o chão.
  * `ZoneTown`: 0 = Zona de paz obrigatória; 1 = Combate ativo apenas durante cercos; 2 = PvP livre em cidades.
  * `UseBowDistancePenalty`, `MaxBowDistancePenalty`: Redução proporcional do dano de flechas à queima-roupa (até 60% de penalidade a curta distância).
  * `FallDownOnDeath`: Animação de queda do personagem ao morrer em eventos.
  * `AllowWater`, `AllowBoat`, `AllowGuards`, `AllowNpcWalkers`: Liberação de movimentação aquática, rotas de barco e patrulhas de guardas.
* **Classes Afetadas:** `GeoEngine.java`, `CombatService.java`, `ZoneTable.java`, `Zone.java`, `NpcMovementController.java`.
* **Critério de Aceite:** Testes de cálculo de dano com arco em diferentes distâncias e teste de combate em cidades com `ZoneTown = 2`.

---

### 🌊 ONDA 3: Economia, Drops, Spoil, Manor & Pesca
* **Arquivos Atendidos:** `rates.properties`, `options.properties` (Drops fine adjusts, Adena Loss), `altgame.properties`.
* **Configurações Implementadas:**
  * Multiplicadores completos de taxas: `RatePartyXp`, `RatePartySp`, `RateDropSealStones`, `RateDropQuest`, `RateDropManor`, `RateExtractFish`, `RateRaidDropItems`.
  * Multiplicadores de VIP: `AllowVipMulXpSp`, `VipMulXp`, `VipMulSp`, `VipDropRate`, `VipSpoilRate`.
  * `MultipleItemDrop`: Empilhamento inteligente de itens dropados no chão para economia de memória.
  * `PreciseDropCalculation`: Algoritmo diferencial de precisão de drop para servidores low/mid/high rate.
  * `PickupFullInventory`: Comportamento quando o inventário está cheio (`drop` no chão, `loot` forçado ou `destroy`).
  * `L2OFFAdenaProtection`, `SetMaxEtcItemSell`, `SetMaxEtcItemSellQnt`: Prevenção de estouro do limite de Adena (2.147B) em vendas de múltiplos itens.
  * `DestroyPlayerDroppedItem`, `AutoDestroyDroppedItemAfter`: Agendador com Virtual Threads para limpeza de itens descartados no chão após X segundos.
* **Classes Afetadas:** `DropService.java`, `InventoryService.java`, `ItemPacketHandler.java`, `TradePacketHandler.java`.
* **Critério de Aceite:** Testes de transbordamento de inventário e limpeza programada de chão.

---

### 🌊 ONDA 4: Sistema Granular de Enchant & Equipamentos
* **Arquivos Atendidos:** `rates.properties` (seção Enchant), `equipments.properties`.
* **Configurações Implementadas:**
  * Substituição do valor plano de 66% por tabelas de probabilidades nível a nível:
    * `NormalWeaponEnchantLevel`, `BlessWeaponEnchantLevel`, `CrystalWeaponEnchantLevel`.
    * `NormalArmorEnchantLevel`, `BlessArmorEnchantLevel`, `CrystalArmorEnchantLevel`.
    * `NormalJewelryEnchantLevel`, `BlessJewelryEnchantLevel`, `CrystalJewelryEnchantLevel`.
  * Limites seguros configuráveis: `EnchantSafeMax` (+3 padrão), `EnchantSafeMaxFull` (+4 para armaduras corpo inteiro).
  * Limites máximos por scroll: `EnchantMaxWeaponNormal`, `EnchantMaxWeaponBlessed`, `EnchantMaxWeaponCrystal`.
  * Restrições de equipamentos de `equipments.properties`: Validação no ato de equipar (`CheckEnchantLevelEquip`, proibição de escudos em magos, armas incompatíveis).
* **Classes Afetadas:** `ItemPacketHandler.java`, `EnchantTableService.java` (novo), `InventoryService.java`.
* **Critério de Aceite:** Testes de curva estatística com 1.000 iterações validando que as chances batem com o arquivo `.properties`.

---

### 🌊 ONDA 5: Personagens, Subclasses, Limites de Stats & Craft
* **Arquivos Atendidos:** `player.properties`, `custom.properties`.
* **Configurações Implementadas:**
  * Limites globais de atributos: `MaxPAtkSpeed`, `MaxMAtkSpeed`, `MaxRunSpeed`, `MaxEvasion` integrados ao `PlayerStats.java`.
  * Subclasses completas: `MaxSubClass` (default 3), `SubclassMaxLevel` (80), `SublcassInitLevel` (40), `AltSubClassWithoutQuests` (libera sub sem Fate's Whisper/Mimir's Elixir), `AltSubclassEverywhere` (troca de sub em qualquer NPC mestre).
  * Anúncios de classe: `AnnounceClassChange`, `AnnounceClassChangeAround`, `ShowClassChangeMessage`.
  * Limites de inventário/armazém: `MaxInventorySlotsForOther`, `MaxInventorySlotsForDwarf`, `WarehouseSlotLimitNoDwarf`, `WarehouseSlotLimitDwarf`, `AltWeightLimit`.
  * Regras de combate: `Delevel` (perda de nível por morte com cancelamento de skills acima do nível), `MagicFailures`, `ShowSuccessChance`, `ShowDebuffOnly`.
  * Autoloot granular: `AlowAutoLoot`, `AutoLootDefault`, `AutoLootRaid`, `AutoLootHerbs`, `AutoLootAdena`.
  * Limites de buffs/danças: `MaxBuffsAmount`, `MaxDancesAmount`.
  * Craft e receitas: `CraftingEnabled`, `AltGameCreation`, `AltGameCreationSpeed`, `AltGameCreationRateXp`, `AltGameCreationRateSp`, `DwarfRecipeLimit`, `CommonRecipeLimit`.
* **Classes Afetadas:** `CharacterService.java`, `PlayerStats.java`, `PlayerCharacter.java`, `GameSession.java`, `SkillService.java`.
* **Critério de Aceite:** Teste de troca de sub sem quest e validação de delevel com corte de nível máximo de skill.

---

### 🌊 ONDA 6: Partys, Clãs, Alianças & Clan Halls
* **Arquivos Atendidos:** `options.properties` (seções Party e Clans).
* **Configurações Implementadas:**
  * Party EXP/SP: `PartyXpCutoffMethod` (`auto`, `percentage`, `level`, `none`), `PartyXpCutoffPercent`, `PartyXpCutoffLevel`.
  * Alcance de Party: `AltPartyRange` (1600 para mobs), `AltPartyRange2` (1400 para party members).
  * Limite de nível em grupo: `PartLevelLimit`, `PartyMaxLevelDifference` (10 níveis padrão).
  * Clãs e Alianças:
    * `LvlForUseAuction`: Nível mínimo de clã para leilão de Clan Halls.
    * Prazos e penalidades: `DaysBeforeJoinAClan`, `DaysBeforeCreateAClan`, `DaysToPassToDissolveAClan`, `DaysBeforeJoinAllyWhenLeaved`, `DaysBeforeJoinAllyWhenDismissed`.
    * Guerras de Clã: `AltClanMembersForWar` (mínimo de membros), `ReputationScorePerKill`, `AltClanWarPenaltyWhenEnded`.
    * Limites de membros progressivos por nível: `MaxMembersClan0` a `MaxMembersClan8`, `MaxMembersRoyals`, `MaxMembersKnights`.
    * Clan Warehouse: `AltMembersCanWithdrawFromClanWH`.
    * Regras de líder: `AltClanLeaderDateChange`, `AltClanLeaderHourChange`, `AltClanLeaderInstantActivation`.
* **Classes Afetadas:** `PartyService.java`, `ClanService.java`, `ClanMember.java`, `ClanHallManager.java`.
* **Critério de Aceite:** Testes de penalidades de saída de clã e distribuição de EXP com corte de party.

---

### 🌊 ONDA 7: Community Board Completo (BBS)
* **Arquivos Atendidos:** `options.properties` (seção Community board).
* **Configurações Implementadas:**
  * `CommunityType`: `Full` (painel HTML moderno), `Old` (estilo padrão Interlude) ou `off` (desativado).
  * `BBSDefault`: Página inicial padrão (`_bbshome`).
  * `ShowLevelOnCommunityBoard`, `ShowStatusOnCommunityBoard`, `OnlineCommunityBoard`, `ColorCommunityBoard`.
  * Paginação e layout: `NamePageSizeOnCommunityBoard` (50), `NamePerRowOnCommunityBoard` (5).
  * Restrições estritas de uso:
    * `CommunityBufferExcludeOn`: Bloqueio em `RB`, `OLYMPIAD`, `PVP`, `SIEGE`, `EVENT`, `ATTACK`, `NOTINTOWN`, `TRADE`.
    * `GatekeeperExcludeOn`: Bloqueio de teleporte do BBS em áreas de combate/cerco.
    * `RestrictCBWhen`: Restrição em `JAIL`, `COMBAT`, `OLY`, `KARMA`, etc.
  * Destaques visuais: `ShowCursedWeaponOwner`, `ShowKarmaPlayers`, `ShowJailedPlayers`, `ShowLegend`, `ShowClanLeader`.
* **Classes Afetadas:** `CommunityBoardService.java`, `BBSPacketHandler.java`, `BBSNavigationService.java`.
* **Critério de Aceite:** Teste de bloqueio de buffer do BBS ao estar flagrado em combate ou dentro da arena de Olimpíada.

---

### 🌊 ONDA 8: Mods, Segurança & Proteções
* **Arquivos Atendidos:** `mods.properties`, `add-on.properties`, `vote.properties`.
* **Configurações Implementadas:**
  * Offline Trade Completo: `AllowOfflineTradeCraft`, `AllowOfflineTradeColorName`, `OfflineTradeColorName`, `AllowOfflineTradeProtection`, `RestoreOfflineTraders`, `OfflineMaxDays`.
  * Anti-Bot Captcha: `EnableCaptcha`, `KillsCounter`, `KillsCounterRandomization`, `ValidationTime`, `Punishment`, `PunishmentTime` (diálogo HTML nativo com `PledgeCrest`).
  * PvP & PK Recompensas: `AllowPvpRewardSystem`, `PvpRewardItem`, `AllowPkRewardSystem`, `PkRewardItem`, `AnnouncePkPvP`, `AnnouncePkPvPNormalMessage`, `AnnouncePkPvPPkMessage`.
  * Lojas com Moedas Alternativas: `SellByItem`, `SellItem` (suporte a Gold Bar ID 3470 em compras e vendas privadas).
  * Quake PvP Announce & War Legend: `EnablePvpAnnounce`, `QuakePvpSystem`, `WarLegend`, `HeroKillsCount` (30 kills = Aura de Herói).
  * Progressão visual: `ColorTitleSystem`, `ColorNameSystem` (alteração de cor de nick/título por patamares de PvP).
* **Classes Afetadas:** `OfflineFarmService.java`, `TradeStorePacketHandler.java`, `AntiBotCaptchaService.java` (novo), `PvPStreakService.java` (novo).
* **Critério de Aceite:** Teste de acionamento do Captcha após contagem de abates e teste de private store com Gold Bar.

---

### 🌊 ONDA 9: NPCs, Chefes, Grand Bosses & Instâncias
* **Arquivos Atendidos:** `bosses.properties`, `npc.properties`.
* **Configurações Implementadas:**
  * Respawn de todos os Grand Bosses parametrizados: Antharas, Valakas, Baium, Queen Ant, Core, Orfen, Zaken, Frintezza, Sailren, Uruka.
  * `AnnounceRaidSpawn`: Transmissão global automática ao ocorrer o spawn de qualquer Raid Boss ou Grand Boss.
  * `KillBarakielSetNobless`: Morte do Barakiel entrega status de Noblesse e Tiara aos membros qualificados da party atacante.
  * Inteligência de NPCs: `AltMobNoAttackPlayer` (Delta Aggro: mobs não atacam jogadores X níveis acima), `GuardAttackAggroMob`, rotas de patrulha e IA de conjuradores.
* **Classes Afetadas:** `GrandBossManager.java`, `NpcAiService.java`, `SpawnService.java`, `NoblesseService.java`.
* **Critério de Aceite:** Teste unitário de abate do Barakiel gerando Noblesse na party e verificação de janelas min/max de respawn.

---

### 🌊 ONDA 10: Olimpíadas & Ciclo dos Heróis
* **Arquivos Atendidos:** `olympiad.properties`.
* **Configurações Implementadas:**
  * Parâmetros do Ciclo: `AltOlyStartTime`, `AltOlyMinMatches`, `AltOlyRewardPoints`, `AltOlyBattlePoints`, `AltOlyPointsPeriod`.
  * Restrições de Entrada: Proibição de itens com enchant acima do limite, verificação de subclasse ativa e buffs proibidos.
  * Sistema de Arenas e Pontuação: Cálculo de acúmulo de pontos, concessão do status de Hero, Monumento dos Heróis e armas de Herói.
* **Classes Afetadas:** `OlympiadManager.java`, `OlympiadGame.java`, `HeroService.java`.
* **Critério de Aceite:** Teste de agendamento de lutas e concessão de pontos pós-combate.

---

### 🌊 ONDA 11: Eventos Automatizados (TvT, CTF, DM, Torneios, Fun Events)
* **Arquivos Atendidos:** `events_start.properties`, `fun_events.properties`, `tvtevent.properties`, `ctfevent.properties`, `dmevent.properties`, `ArenaDuel.properties`, `tournament.properties`.
* **Configurações Implementadas:**
  * Cronogramas automáticos (`events_start.properties`): Schedulers para execução periódica sem intervenção de GM.
  * TvT / CTF / DM Engines: Registro de times, teleporte para arena isolada, balanceamento de níveis, entrega de recompensas em itens configuráveis e retorno seguro à coordenada original.
  * Duelos em Arena 1x1 e Party: Validação de convite, contagem regressiva e restrição de poções.
* **Classes Afetadas:** `EventManager.java`, `TvTEventService.java`, `CtfEventService.java`, `DmEventService.java`.
* **Critério de Aceite:** Teste de ciclo de vida completo de evento TvT (registro -> batalha -> recompensa -> teleporte de volta).

---

### 🌊 ONDA 12: Cercos a Castelos & Clan Halls Conquistáveis
* **Arquivos Atendidos:** `siege.properties`.
* **Configurações Implementadas:**
  * Parametrização completa dos 9 Castelos: Portões, HP das paredes, tempo de cerco, guardas contratáveis, impostos das lojas da cidade.
  * Cercos de Clan Halls Conquistáveis:
    * Fortress of the Dead, Devastated Castle, Bandit Stronghold, Rainbow Springs Chateau, Wild Beast Farm.
  * `ActivateSystem`, `RewardOnlineOnly`, `RewardInfo`: Recompensas automáticas em itens/Adena distribuídas aos clãs vencedores do cerco.
* **Classes Afetadas:** `SiegeManager.java`, `Castle.java`, `ClanHallSiegeManager.java`, `SiegeRewardService.java`.
* **Critério de Aceite:** Teste de finalização de cerco com entrega de premiação de `RewardInfo` aos membros online do clã.

---

### 🌊 ONDA 13: Sistema AIOx & Buff Shop
* **Arquivos Atendidos:** `aiox.properties`.
* **Configurações Implementadas:**
  * Status e ciclo de vida AIOx: `EnableAioSystem`, `EnableAioDelevel`, `AioSetDelevel`.
  * Restrições rígidas: `AllowAioLeaveTown`, `AllowAioSpeakNpc`, `AllowAioTeleport`, `canCastBuffs` (somente em Peace Zone).
  * Aparência e dual: `AllowAioNameColor`, `AioNameColor` (88AA88), `AllowAioTitleColor`, `AioTitleColor`, `AllowAIODual` (Item 9209).
  * Classes permitidas e itens: `AllowedClassId` (10, 25, 38), `AioItemId` (9225), `AioItemCount`.
  * Buff Shop: `BuffShopEnable`, `BuffShopMaxDays` (14 dias), `DefaultBuffShopSlots` (24 slots), `AllowOfflineBuff`.
* **Classes Afetadas:** `AioService.java`, `PlayerCharacter.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `AioConfigurationTest.java` com 6 testes aprovados validando restrições, classes permitidas e cores.

---

### 🌊 ONDA 14: Sistema VIP, Itens de Clã, Start Custom & Votos
* **Arquivos Atendidos:** `add-on.properties`, `vote.properties`, `rates.properties`.
* **Configurações Implementadas:**
  * Sistema VIP completo: `AllowVipNameColor`, `VipNameColor` (0088FF), `AllowVipTitleColor`, `VipTitleColor`, `VipDias` (30/60/90).
  * Multiplicadores VIP: `AllowVipXpSp`, `VipXp`, `VipSp`, `VipDropRate`, `VipSpoilRate`.
  * Sistema de Votos: Cooldown de 12h por personagem/IP, premiação configurável com `VoteSystemRewardId` (3470 - Gold Bar) e `VoteSystemRewardCount` (5), credenciais TopZone/HopZone/Network.
  * Start Customizado e Clã: `CustomStarterItemsEnabled`, `StartingCustomItemsFighter`, `StartingCustomItemsMage`, `ClanSkillByItem`, `RaidBossInfoPageLimit`.
* **Classes Afetadas:** `VipService.java` (novo), `VoteRewardService.java` (novo), `InventoryService.java`, `PlayerCharacter.java`.
* **Critério de Aceite:** Testes unitários em `VipAndVoteConfigurationTest.java` com 5 testes aprovados validando status, expiração e cooldown.

---

### 🌊 ONDA 15: Regras Alternativas de Combate, Cancel & Duração de Skills
* **Arquivos Atendidos:** `altgame.properties`.
* **Configurações Implementadas:**
  * Modos de Cancel: `CancelMode` (`new` com até 5 buffs individuais vs `old`), integração com `CancelRestoreService`.
  * Restrição de Nomes: `ForbiddenNames` (`admin`, `gm`, `gamemaster`, `annoucements`) bloqueando criação em `CharacterService`.
  * Multiplicador Crítico Mágico: `MCritRate` (2x) integrado a `CombatService.skillMagicNpc`.
  * Modificadores de Buffs e Defesa: `MaxBuffAmount` (50), `AltShieldBlocks`, `AltPerfectShieldBlockRate`, `GradePenalty`, `SkillReuseDelay` (70).
* **Classes Afetadas:** `CharacterService.java`, `CombatService.java`, `CancelRestoreService.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `AltGameConfigurationTest.java` com 4 testes aprovados validando bloqueio de nomes e fórmulas.

---

### 🌊 ONDA 16: Arenas de Duelo 1x1, Party & Sistema de Torneios
* **Arquivos Atendidos:** `ArenaDuel.properties`, `tournament.properties`.
* **Configurações Implementadas:**
  * Modos de Arena: `Arena1x1Enable`, `ArenaPartyEnable`, `ArenaRegistrationTime`, `ArenaRoundTime`, `ArenaInterval`, `ArenaLocX`, `ArenaLocY`, `ArenaLocZ`.
  * Torneios Automatizados: `TournamentEnable`, suporte aos modos 2x2, 3x3, 5x5 e 9x9, portas de entrada, intervalos de chamada e premiações.
* **Classes Afetadas:** `ArenaDuelService.java`, `TournamentService.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `ArenaAndTournamentConfigurationTest.java` com 4 testes aprovados validando registros, desregistros e parâmetros.

---

### 🌊 ONDA 17: Níveis de Acesso GM, Auditoria & Snoop
* **Arquivos Atendidos:** `access.properties`.
* **Configurações Implementadas:**
  * Permissões: `MasterAccessLevel`, `GMAccessLevel`, `AllowGMSnoop`, `GMTeleportAnywhere`, `GmOverEnchant`.
  * Auditoria: `GMCommandLog`, canais de logging de comandos administrativos e restrições de bypass.
* **Classes Afetadas:** `AdminAccessConfigurationTest.java`, `GameSession.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `AdminAccessConfigurationTest.java` com 2 testes aprovados validando níveis e snoop.

---

### 🌊 ONDA 18: IA de NPCs, Aggro Range, Roaming, Buffers & Teleports
* **Arquivos Atendidos:** `npc.properties`.
* **Configurações Implementadas:**
  * IA e Movimentação: `NpcAiTickDelay`, `MaxDriftRange`, `ShowNpcLevelAndAggro`, `AllowClassMaster`, `ClassMasterPriceList`, `BufferFreeLevel`.
  * Buffers e Teleports: Preços de teleporte retail, nível gratuito de buffs e listas de classes permitidas para troca de classe rápida.
* **Classes Afetadas:** `NpcAndTeleportConfigurationTest.java`, `NpcInstance.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `NpcAndTeleportConfigurationTest.java` com 3 testes aprovados validando drift, aggro e buffer.

---

### 🌊 ONDA 19: Seven Signs, Loteria, PC Cafe, Campeões e Casamentos
* **Arquivos Atendidos:** `fun_events.properties`.
* **Configurações Implementadas:**
  * Seven Signs & Selos: `AltCastleForDawn`, `AltCastleForDusk`, `StrictSevenSigns`, `AltJoinDawnCost`, multiplicadores de defesa de portas de castelos Dawn/Dusk.
  * Loteria e PC Cafe: `AltLotteryPrize`, `AltLotteryTicketPrice`, taxas de premiação de 5/4/3/2 acertos, `PCCaffeEnabled`, intervalos de pontos e faixas de nível.
  * Campeões e Eventos Sazonais: `ChampionPassive`, `ChampionTitle`, `ChampionHpRegen`, `ChampionSpecialItemID`, Medalhas, Árvore de Natal, L2Day, Big Squash.
  * Sistema de Casamentos: `AllowWedding`, `WeddingPrice`, `WeddingTeleport`, `WeddingDivorceCosts`, paleta de cores hexadecimais para casais normais, gays e lésbicos.
* **Classes Afetadas:** `FunEventsAndWeddingsConfigurationTest.java`, `WeddingService.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `FunEventsAndWeddingsConfigurationTest.java` com 4 testes aprovados validando cálculos de loteria, casamento e campeões.

---

### 🌊 ONDA 20: Parâmetros de Servidor, Safe Reboot, Dimensional Rift & Respawn
* **Arquivos Atendidos:** `gameserver.properties`.
* **Configurações Implementadas:**
  * Parâmetros Gerais e Auditoria: `ServerName`, `MaximumOnlineUsers`, `TimeZone`, `BanChatLog`, `BanAccountLog`, `JailLog`, `GlobalBanTime`.
  * Safe Reboot Engine: `SafeReboot`, `SafeRebootTime`, flags de desativação preventiva de enchant, teleport, craft e transações.
  * Dimensional Rift & Clãs: `OnlyClanleaderCanSitOnThrone`, `RiftMinPartySize`, `MaxRiftJumps`, delays e custos de Recruit a Hero.
  * Respawn & Cidade: `RespawnRandomInTown`, offsets X/Y, flags de restauração de CP/HP/MP pós-morte, Class Master popup e voiced.
* **Classes Afetadas:** `GameserverAndRiftConfigurationTest.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `GameserverAndRiftConfigurationTest.java` com 4 testes aprovados validando auditoria, limites e flags de reinício seguro.

---

### 🌊 ONDA 21: Infraestrutura de Rede, Protocolos de Cliente & Segurança de Login
* **Arquivos Atendidos:** `network.properties`, `login/authserver.properties`, `login/network.properties`.
* **Configurações Implementadas:**
  * Rede do Gameserver: `GameServerPort`, `LoginPort`, faixas de protocolos permitidos 730 a 746, limites de pool de conexões com banco.
  * Login Auth Server & Segurança: `AuthServerPort`, `ShowLicence`, `BrutProtection`, `DDoSProtection`, TTL de sessão, anti-flood granular e ban por tentativas incorretas.
  * Login Network: `LoginAuthPort`, `IpUpdateTime`, conexões dedicadas do login.
  * Suporte a Múltiplos Arquivos e Desambiguação de Chaves: Indexação semântica por caminho relativo no `ConfigLoader` resolvendo colisões entre arquivos de mesmo nome (`game/main/network.properties` vs `login/network.properties`).
* **Classes Afetadas:** `NetworkAndAuthConfigurationTest.java`, `ConfigLoader.java`, `Config.java`.
* **Critério de Aceite:** Testes unitários em `NetworkAndAuthConfigurationTest.java` com 3 testes aprovados validando resolução de portas, protocolos e segurança.

---

## 📈 4. Checklist Geral de Progresso (21 Ondas - 100% Concluído)

- [x] **Onda 1: Chat, Comunicação, Social & Petições** (`options.properties`, `player.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 2: Geodata, Movimentação, Zonas & Física de Combate** (`options.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 3: Economia, Drops, Spoil, Manor & Pesca** (`rates.properties`, `options.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 4: Sistema Granular de Enchant & Equipamentos** (`rates.properties`, `equipments.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 5: Personagens, Subclasses, Limites de Stats & Craft** (`player.properties`, `custom.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 6: Partys, Clãs, Alianças & Clan Halls** (`options.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 7: Community Board Completo (BBS)** (`options.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 8: Mods, Segurança & Proteções** (`mods.properties`, `add-on.properties`, `vote.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 9: NPCs, Chefes, Grand Bosses & Instâncias** (`bosses.properties`, `npc.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 10: Olimpíadas & Ciclo dos Heróis** (`olympiad.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 11: Eventos Automatizados (TvT, CTF, DM, Torneios)** (`events_*.properties`, `tvt/ctf/dm`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 12: Cercos a Castelos & Clan Halls Conquistáveis** (`siege.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 13: Sistema AIOx & Buff Shop** (`aiox.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 14: Sistema VIP, Itens de Clã, Start Custom & Votos** (`add-on.properties`, `vote.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 15: Regras Alternativas, Cancel & Duração de Skills** (`altgame.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 16: Arenas de Duelo 1x1, Party & Sistema de Torneios** (`ArenaDuel.properties`, `tournament.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 17: Níveis de Acesso GM, Auditoria & Snoop** (`access.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 18: IA de NPCs, Aggro Range, Roaming, Buffers & Teleports** (`npc.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 19: Seven Signs, Loteria, PC Cafe, Campeões e Casamentos** (`fun_events.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 20: Parâmetros de Servidor, Safe Reboot, Rift & Respawn** (`gameserver.properties`) - *Concluído e Testado com Sucesso!*
- [x] **Onda 21: Infraestrutura de Rede, Protocolos de Cliente & Segurança de Login** (`network.properties`, `login/*.properties`) - *Concluído e Testado com Sucesso!*

---

## 🏆 5. Status de Entrega do Roteiro

* **Todas as 21 Ondas Implementadas, Integradas e Verificadas**:
  * **Onda 1**: Chat, canais, restrição de nível, anti-flood, karma, filtro de palavras (`WordFilterTableTest`, `ChatConfigurationTest`).
  * **Onda 2**: Zonas de vila personalizadas (`ZoneTown`), combate, penalidade de flecha por distância (`CombatService`, `ZoneTable`).
  * **Onda 3**: Empilhamento de drop (`MultipleItemDrop`), cálculo preciso de drop, proteção contra overflow de adena (`L2OFFAdenaProtection`), slots de inventário e distribuição de party (`DropAndEconomyConfigurationTest`).
  * **Onda 4**: Tabelas granulares de enchant (`NormalWeaponEnchantLevel`, `BlessWeaponEnchantLevel`, etc.) e restrições de equipamentos grade/hero/arma (`EnchantAndEquipmentConfigurationTest`).
  * **Onda 5**: Limites de atributos (PAtk, MAtk, PDef, MDef, Crit, Speed), subclasses com verificação de nível e sagas (`PlayerStatsTest`, `SubClassServiceTest`, `SubclassAndSagasTest`).
  * **Onda 6**: Clãs, alianças, expulsão sem penalidade (`AltClanLeave/DismissPenalty`), guerras e aluguel/recuperação de Clan Hall (`ClanAndPartyConfigurationTest`).
  * **Onda 7**: Community Board completo via BBS (`CommunityBoardServiceTest`).
  * **Onda 8**: Anti-Bot Captcha (`BotsPreventionService`), Offline Trade/Craft com invulnerabilidade e cor (`OfflineTradeService`), Buff Shop e Recompensas PvP (`ModsAndSecurityConfigurationTest`).
  * **Onda 9**: Anúncio de Raid Bosses (`AnnounceRaidSpawn`), alcance de drift (`MaxDriftRange`) e regras de aggro (`GrandBossAndNpcConfigurationTest`).
  * **Onda 10**: Olimpíadas completas com pontos iniciais, reset semanal, restrição de IP, limite de enchant em arena e desempates (`OlympiadManagerTest`, `OlympiadGameServiceTest`).
  * **Onda 11**: Eventos PvP TvT, CTF e DM com configuração automática, faixas de nível e premiação (`PvPEventConfigurationTest`).
  * **Onda 12**: Sistema de Cercos a Castelos com duração configurável (`SiegeLength`), limites de membros/nível de clã atacante e recompensa Blood Alliance (`SiegeServiceTest`).
  * **Onda 13**: Sistema AIOx com cores hexadecimais, restrições urbanas/paz, classes permitidas e Buff Shop offline (`AioConfigurationTest`).
  * **Onda 14**: Status VIP completo com multiplicadores de taxa, expiração temporal e sistema de votos TopZone/HopZone/Network com cooldown (`VipAndVoteConfigurationTest`).
  * **Onda 15**: Regras retail de `altgame.properties`: lista negra de nomes (`ForbiddenNames`), `MCritRate`, `CancelMode` e limites de buffs (`AltGameConfigurationTest`).
  * **Onda 16**: Arenas de duelo 1x1 e por party, modos de torneio 2x2/3x3/5x5/9x9 (`ArenaAndTournamentConfigurationTest`).
  * **Onda 17**: Permissões administrativas, snoop de chat, over-enchant e auditoria de GM (`AdminAccessConfigurationTest`).
  * **Onda 18**: IA de NPCs, drift range, exibição de nível/aggro, class master e buffer retail (`NpcAndTeleportConfigurationTest`).
  * **Onda 19**: Seven Signs Dusk/Dawn, loteria, PC Cafe, monstros campeões e casamentos (`FunEventsAndWeddingsConfigurationTest`).
  * **Onda 20**: Configurações gerais de gameserver, safe reboot com bloqueio de ações, dimensional rift e respawn (`GameserverAndRiftConfigurationTest`).
  * **Onda 21**: Portas e IPs de gameserver/login, suporte a protocolos 730-746, pools de conexões, anti-bruteforce e anti-DDoS (`NetworkAndAuthConfigurationTest`).

* **Validação Geral do Projeto**:
  * **100% dos 27 arquivos `.properties` (4.668 propriedades totais indexadas) plenamente integrados**.
  * **21 de 21 Ondas completas com 100% de testes verdes (0 falhas, 0 erros)**.
  * **Build limpo e compatibilidade garantida em Java 21 LTS e Spring Boot 3.5**.

