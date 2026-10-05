# Estudo do L2JDreamV2 -> L2JLopez

Inventario completo do que existe em `D:\Cristiano\Lineage\L2JDreamV2` (Interlude C6, protocolo 730-746) e o plano
de migracao para o L2JLopez. Numeros medidos direto nos arquivos (outubro/2026).

## 1. Visao geral

| Pasta | Arquivos | Tamanho | Conteudo |
|---|---:|---:|---|
| `java/com/dream` | 1.775 `.java` | 8,3 MB | Codigo-fonte (login, game, mmocore, utilitarios) |
| `game/data` | 15.928 | 719 MB | Datapack: html, xml, scripts Jython, geodata (573 MB), pathnodes (122 MB) |
| `game/config` | 32 | 222 KB | 25 `.properties` (~1.500 chaves) + access levels + log4j |
| `tools/sql` | 121 `.sql` | 13,5 MB | Schema + dados estaticos (npc, spawn, itens, drops...) |
| `libs` | 3 | 11 MB | Jars de terceiros (c3p0, jython, mysql etc.) |
| `login` | 9 | 13 KB | Config/scripts do login server |

## 2. Codigo-fonte (`java/com/dream`)

| Pacote | Arquivos | KB | Papel | Status no L2JLopez |
|---|---:|---:|---|---|
| `auth` | 52 | 137 | Login server (Blowfish/RSA, contas, registro de GS) | **Portado** (`network.login`), autenticacao e sessao completa |
| `mmocore` | 13 | 38 | NIO selector proprio | **Substituido** por virtual threads (1 thread por conexao) |
| `config` | 19 | 26 | Leitura dos `.properties` | **Substituido** por `ServerProperties` (records validados) |
| `game.network` | 530 | 1.349 | 208 pacotes do cliente, 297 do servidor, crypt, handler | **Avancado**: crypt + ~55 pacotes (combate, trade, sell, multisell, party, dialogos) |
| `game.model` | 462 | 3.439 | L2Object/L2Character/L2PcInstance, 108 tipos de instancia, zonas, olimpiada, sieges, eventos | **Avancado**: `PlayerCharacter`, `PlayerStats`, `GameWorld` com Spatial Grid, `Party` |
| `game.handler` | 181 | 727 | admin (38), item (45), skill (41), chat (13), user (15), voiced (10) | **Portado parcialmente**: admin (menus HTML, spawn, heal, level, enchant...), item (consumíveis, shots, enchant, destroy), bypasses de NPCs |
| `game.manager` | 66 | 673 | Castle/Fort/ClanHall/Siege, grandbosses (11), raid, boat, manor, olimpiada, offline etc. | **Parcial**: Spawns de RaidBoss e Dungeons ativos em `SpawnService` |
| `game.skills` | 149 | 359 | Condicoes (49), efeitos (68), funcs, formulas | **Portado**: `SkillTable` (2.686 skills), `SkillCondition`, restrições de armas, buffs/debuffs e persistência |
| `game.datatables` | 47 | 297 | Carregadores: `sql/` (11) e `xml/` (30) | **Portado**: `CharTemplateTable`, `TeleportLocationTable`, `BuyListTable`, `MultiSellTable`, `SkillTreeTable`, `HtmCache` (10.747 HTMLs) |
| `game.ai` | 16 | 153 | IA de NPC/monstro/jogador | **Funcional**: `NpcAiService` básico |
| `game.communitybbs` | 18 | 138 | Community Board (Alt+B) | Pendente |
| `game.geodata` | 13 | 71 | Geodata + pathfinding | Pendente |
| `game.taskmanager` | 24 | 44 | Decay, attack stance, knownlist, auto-announce, reset | **Portado**: `ScheduledExecutorService`, VitalsRegenTask, Spatial Grid broadcasts |
| `game.templates` | 17 | 73 | Templates de NPC/item/char | **Portado**: `NpcTemplate`, `ItemTemplate`, `CharTemplate` com fallbacks |
| `game.idfactory` | 5 | 27 | Alocacao de objectId | **Portado**: `ObjectIdFactory` sequencial thread-safe |
| `tools`, `util`, `thread`, `lang`, `jdklog` | ~90 | ~280 | Utilitarios, crypt, pools, i18n, log | Substituidos por JDK 21/Spring/SLF4J |

### 2.1 Instancias de NPC (108 classes em `model.actor.instance`)
Comerciais/servico: Merchant, Warehouse, Teleporter, Trainer, VillageMaster, ClassMaster, Buffer (+Vip/Event),
Blacksmith, Fisherman, ManorManager, Auctioneer, SymbolMaker, Adventurer, NewbieHelper, Wedding, Observation,
Olympiad, WyvernManager, Doormen, Warden. Castelo/forte: CastleChamberlain/Blacksmith/Magician/Teleporter/Warehouse,
Fort* (Commander, Envoy, Manager, SiegeGuard...). Combate: Monster, RaidBoss, GrandBoss, Minion, Guard, Chest,
Sepulcher*, Rift*, Festival*, Feedable/TamedBeast. Customizados do Dream: **Achievements, AioSeller,
AIOTeleporter, ArenaDuel, BufferEvent, RaidBossInfo, Report, Status, Vote, Event, Fast**.

### 2.2 Handlers
- **Admin (38)**: Admin, Announce, Ban, BanChat, BuffShop, ClanFull, Commands, Control, CursedWeapon, Delete,
  Developer, DoorControl, EditChar, EditNpc, Effects, Enchant, Event, ExpSp, Fortress, Jail, Level, Manor, Methods,
  MobGroup, Olympiad, Petition, Pledge, Polymorph, Quest, Reload, Reset, Server, SevenSigns, Shop, Siege, Skills,
  Spawn, Teleport.
- **Item (45)**: pocoes, soulshots/spiritshots (+beast, fish), scrolls (escape, ressurreicao, enchant), receitas,
  sementes/manor, chaves, fogos, livros, mapas, cristais de alma, itens AIO/VIP/Nobless/ClanFull/ClanSkill.
- **Voiced (10)**: `.aio`, `.autofarm`, `.bank`, `.classmaster`, `.cfg` (Configurator), `.help`, `.offline`,
  `.reset`, `.roulette`, `.wedding`.
- **User (15)**: /loc, /time, /escape, /mount, /dismount, /partyinfo, /siegestatus, /olympiadstat, canais de comando,
  clan penalty/wars, BuffShop.

### 2.3 Managers e sistemas grandes
Castle, CastleManor, Fort (+Siege/Spawn/Guards), ClanHall (+Siege, Auction), Siege (+Guard, Reward), SevenSigns,
Olympiad, Hero, Duel, Couple (casamento), Boat, DimensionalRift, FourSepulchers, MonsterRace, Lottery, Fishing
Championship, CursedWeapons, RaidBossSpawn/Points/Info, BossSpawn, AutoSpawn, DayNightSpawn, ItemsOnGround/AutoDestroy,
Offline (trade/craft/buff shop), BuffShop, BotsPrevention (captcha), Petition, PartyRoom, Town, Crown, MercTicket.
Grand bosses: Antharas, Baium, Core, Frintezza, Orfen, Queen Ant, Sailren, Valakas, Van Halter, Zaken (+ Last
Imperial Tomb).

### 2.4 Mods/customizacoes do Dream (alvo de `features.*`)
| Mod | Onde no legado | Config | Status |
|---|---|---|---|
| Achievements | `model.entity.events.archievements` (31) + `L2AchievementsInstance` | `custom/achievements.xml` | **Logica portada** (falta NPC/UI) |
| Quake (killing spree) | add-on | `AllowQuakeSystem` | **Listener portado** |
| AIO (buffer char) | `aiox.properties`, `AioMenu`, itens AIO | 30 chaves | Pendente |
| VIP | `add-on` Vip*, itens VIP | | Pendente |
| AutoFarm | `L2FarmPlayableAI`, voiced `.autofarm` | | Pendente |
| DressMe / Skins | `DressMeData.xml`, `DressMeEffectManager`, `SkinsItemsData` | | Pendente |
| PvP Rank / cores PvP | `pvprank.xml`, `PvPRankData`, `PvPColorSystem` | `mods` | Pendente |
| Reset | `resetData.xml`, `ResetManager`, `.reset` | | Pendente |
| Roleta | `roulette.xml`, `RoletaData`, `.roulette` | | Pendente |
| Party Farm | `partyfarm.xml`, `L2PartyFarmEvent` | | Pendente |
| Eventos TvT/CTF/DM/ArenaDuel/Tournament | `model.entity.events.*` | `events/*.properties` (~250 chaves) | Pendente |
| Fun events (L2Day, Natal, Squash, Medals, Starlight) | `model.entity.events` | `fun_events` (154) | Pendente |
| Offline trade/craft/buff shop | `OfflineManager`, `BuffShopManager` | `mods`, `aiox` | Pendente |
| Captcha anti-bot | `BotsPreventionManager` | `EnableCaptcha` | Pendente |
| Startup system (classe/equipamento inicial guiado) | `StartupSystem` | `mods` | Pendente |
| Recompensas (online, PvP/PK, primeiro login) | varios | `mods`, `add-on` | Pendente |
| Vote reward | `L2VoteInstance` | `vote.properties` | Pendente |
| Banco (`.bank`, gold bar) | voiced Bank | `custom` | Pendente |
| Restricoes de equipamento por classe | | `equipments.properties` | Pendente |
| Greeting/auto-announce | `greeting.xml`, `GreetingManager` | | Pendente |

## 3. Datapack (`game/data`)

### 3.1 XML (357 arquivos, 12,4 MB)
| Grupo | Arquivos | Destaques |
|---|---|---|
| `player/` | 9 + `skills/` 6 | char_template (**portado**), lvl_up_data, statBonus, armorsets, henna, pet_stats, extractable_items, summon_items, helper_buff_list; skill_tree (1,2 MB), enchant_skill_tree (1,5 MB), skill_learn, spellbooks, fishing/pledge trees |
| `stats/skills` | 34 | 2.686 skills (efeitos, condicoes, formulas) |
| `stats/weapon` / `armor` / `etcitem` | 56 / 39 / 7 | Stats e funcoes de 1.315 armas, 1.074 armaduras, 46 etc |
| `stats/augmentation` | 5 | Augments |
| `world/` | 17 | teleports, buylists (681 KB), recipes, npc_skills (1,5 MB), minion, doors, boats, seeds, fishes, walkers, auto_chat, four_sepulchers, dimensional rift, cursed weapons, static objects, mapregion |
| `zone/` | 14 | peace, castle, clanhall, forts, boss, water, arena, fishing, misc, mothertree, stadia... |
| `multisell/` | 162 | Lojas multisell |
| raiz | 7 | DressMeData, extraicons, greeting, partyfarm, pvprank, resetData, roulette |

### 3.2 HTML (4.363 arquivos, 4 MB) em 36 pastas
Maiores: default (790), doormen (505), merchant (431), admin (402), guard (335), teleporter (266), trainer (236),
seven_signs (162), warehouse (139), adventurer_guildsman (135), help (126), fortress (122), villagemaster (105),
chamberlain (101), mods (100).

### 3.3 Scripts Jython (10.868 arquivos)
| Pasta | `.py` | `.htm` |
|---|---:|---:|
| quests (343 quests) | 334 | 9.170 |
| village_master | 19 | 815 |
| custom | 26 | 444 |
| teleports | 5 | 55 |
Jython esta morto (sem Python 3); quests devem ser reescritas em Java (`scripting`), reaproveitando os `.htm`.

### 3.4 Geodata / pathnode
171 + 167 arquivos (695 MB). Nao versionar; carregar de um diretorio configuravel.

## 4. SQL (`tools/sql`)
121 tabelas; **schema portado** (Flyway V2..V122). Tabelas com dados estaticos (108.740 linhas, agora em
`db/data`, V1xxx):

| Tabela | Linhas | Tabela | Linhas |
|---|---:|---|---:|
| droplist | 28.055 | spawnlist | 26.622 |
| market_icons | 13.311 | etcitem | 11.590 |
| henna_trees | 7.267 | npc | 7.074 |
| castle_siege_guards | 4.373 | fort_siege_guards | 3.428 |
| pets_skills | 1.594 | weapon | 1.313 |
| armor | 1.014 | buffer_scheme_contents | 968 |
| fort_spawnlist | 342 | fort_staticobjects | 229 |
| custom_teleports | 196 | raidboss_spawnlist | 193 |
| outras 24 tabelas | < 160 cada | | |

## 5. Configuracoes (`game/config`, ~1.500 chaves)
| Arquivo | Chaves | Arquivo | Chaves |
|---|---:|---|---:|
| siege | 236 | gameserver | 161 |
| fun_events | 154 | mods | 152 |
| rates | 128 | options | 120 |
| player | 104 | bosses | 61 |
| add-on | 52 | altgame | 52 |
| npc | 34 | olympiad | 34 |
| custom | 31 | aiox | 30 |
| ctf/tvt | 26 cada | dm | 20 |
| equipments | 18 | network | 13 |
Estrategia: um record por dominio em `ServerProperties` (ja existem `rates`, `network`, `login`, `features`),
migrando chave a chave junto com o sistema que a usa (sem portar chave morta).

## 6. Plano de migracao (ordem e status)
1. **Dados estaticos no banco** (**CONCLUIDO**): 40 tabelas via Flyway `db/data` (108k linhas).
2. **Itens e Inventario** (**CONCLUIDO**):
   - `ItemTemplate`, `ItemTemplateTable`, `JdbcItemTemplateTable` (weapon, armor, etcitem).
   - Inventario com 22 slots de paperdoll, regras de equip/unequip portadas do legado.
   - Itens iniciais da criacao (`char_creation_items`), `ItemList` (0x1b), `InventoryUpdate` (0x27), `SystemMessage` (0x64), `UseItem` (0x14), `RequestUnEquipItem` (0x11).
   - `ObjectIdFactory` compartilhada entre personagens, itens e NPCs (`0x10000000+`).
3. **NPCs no mundo e Visibilidade** (**CONCLUIDO**):
   - `NpcTemplate`, `NpcTemplateTable`, `JdbcNpcTemplateTable` (carrega 7.074 NPCs em ~100ms).
   - `NpcInstance` e `SpawnService` (carrega 26.622 spawns de `spawnlist` e `custom_spawnlist` em ~300ms).
   - Spatial Grid 2D em `GameWorld` (celulas de 4096 unidades, lookup O(1) com raio euclidiano de 3500).
   - KnownList na `GameSession`: `NpcInfo` (0x16), `CharInfo` (0x03), `DeleteObject` (0x12), broadcast de movimento `MoveToLocation` e selecao de alvo `MyTargetSelected` + `ValidateLocation`.
4. **HTML + dialogos**: `HtmCache` de `data/html`, `Action` em NPC -> `NpcHtmlMessage` (0x0f / 0x19 / `NpcHtmlMessage`), bypass basico.
5. **Teleporte e lojas**: teleports.xml/`custom_teleports`, buylists.xml, multisell.
6. **Combate base**: stats (statBonus, lvl_up_data), ataque fisico, morte/respawn, drops (`droplist`), XP.
7. **Skills**: XML de skills + skill_tree, efeitos/condicoes mais usados, buffs.
8. **Mods do Dream** como `features.*` (AIO, VIP, autofarm, dressme, pvp rank, reset, roleta, eventos).
9. **Sistemas grandes**: clans, party, trade, warehouse, quests (Java), olimpiada, sieges, grand bosses.

Cada etapa sai testavel no cliente real e com testes automatizados.
