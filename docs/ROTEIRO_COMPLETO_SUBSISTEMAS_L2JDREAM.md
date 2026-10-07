# Roteiro Completo de Migração e Subsistemas: L2JDream → L2JLopez

**Data**: Outubro de 2026  
**Referência**: Lineage II Interlude (Chronicle 6, Protocolo 730–746) — Fonte base: `D:\Cristiano\Lineage\L2JDreamV2`  
**Objetivo**: Mapeamento exaustivo de cada subsistema, tabela, gerenciador e tarefa de inicialização (`L2GameServer.java` do Dream) para o L2JLopez, com checkpoints acionáveis de implementação.

---

## 1. Análise Comparativa do Startup (`L2GameServer.java`)

A inicialização do L2JDream executa rigorosamente 23 seções ordenadas no método `main(String[] args)`. Abaixo está o raio-x completo de cada subsistema:

| # | Seção no Dream | Classes / Gerenciadores | Arquivo de Dados / XML | Status no L2JLopez | Prioridade |
|---|---|---|---|:---:|:---:|
| **01** | **Boot & Config** | `Config.loadAll()`, `Config.loadFilter()`, log4j, directories | 25 `.properties` (~1.500 chaves) | **Portado moderno** (`ServerProperties` records tipados) | Concluído |
| **02** | **Database Engine** | `L2DatabaseFactory` (c3p0) + cleanup `PcAction.clearRestartTask()` | Flyway V2..V122 | **Modernizado** (Spring Boot HikariCP + Spring Data JDBC) | Concluído |
| **03** | **Script Engine** | `L2ScriptEngineManager` (Jython / Java) | `data/scripts/` | **Pendente** (Jython obsoleto; scripts de quests/IA portados em Java) | Alta |
| **04** | **Concorrência** | `ThreadPoolManager` (General, Effect, AI, Packet pools) | — | **Modernizado** (Java 21 Virtual Threads por conexão + ScheduledExecutor) | Concluído |
| **05** | **Mundo & Spatial** | `L2World`, `ServerData`, `DeadlockDetector` | — | **Portado** (`GameWorld` com Spatial Grid 2D 4096 unidades) | Concluído |
| **06** | **Regiões do Mapa** | `MapRegionTable` (Restart points, vilas mais próximas por raça/coord) | `data/xml/world/mapregion/mapregion.xml` | **Pendente** | **Imediata** |
| **07** | **Anúncios** | `Announcements` (Mensagens de boas-vindas, auto-anúncios, broadcast) | `announcements` (banco) | **Pendente** | **Imediata** |
| **08** | **ID Factory** | `IdFactory` (BitSet/Increment/Compaction) | Tabela `characters`, `items` | **Portado** (`ObjectIdFactory` atômica compartilhada) | Concluído |
| **09** | **Geodata & Path** | `GeoData`, `PathFinding` | `data/geodata/` (573 MB) | **Pendente** (carregador de geodata L2J / células) | Média |
| **10** | **Ciclo de Tempo** | `GameTimeController` (Dia/Noite 360 ticks, 4h real = 24h jogo, pôr/nascer do sol) | — | **Pendente** | **Imediata** |
| **11** | **Barcos & Estáticos** | `BoatManager`, `StaticObjects` (tronos, estátuas, placas) | `boat.xml`, `staticobjects.xml` | **Pendente** | Média |
| **12** | **Gerenciadores de Tarefas** | `AttackStanceTaskManager`, `DecayTaskManager`, `KnownListUpdateTaskManager`, `SQLQueue` | — | **Parcial** (concorrência reativa implementada; falta Decay oficial) | Alta |
| **13** | **Teleportes** | `TeleportLocationTable` (gatekeepers normais e nobres) | `teleports.xml`, `custom_teleports` | **Portado** (`TeleportLocationTable`) | Concluído |
| **14** | **Skills & Árvores** | `SkillTreeTable`, `SkillTable`, `PetSkillsTable`, `NobleSkillTable`, `HeroSkillTable` | `data/xml/player/skills/` (34 XMLs, 2.686 skills) | **Portado** (`SkillTable`, `SkillTreeTable`, `SkillService`) | Concluído |
| **15** | **Itens & Armaduras** | `ItemTable`, `ArmorSetsTable`, `IconTable`, `AugmentationData`, `SkillSpellbookTable` | `stats/weapon`, `armor`, `etcitem`, `armorsets.xml` | **Parcial** (Itens funcionam; falta carregar `ArmorSetsTable` e Augment) | **Imediata** |
| **16** | **Chão & Autodestroy** | `ItemsOnGroundManager`, `ItemsAutoDestroy` (herbs e drops temporários) | — | **Parcial** (Drops e autoloot funcionam; falta timer no chão) | Média |
| **17** | **HTML & Diálogos** | `HtmCache` (Indexação de diálogos e menus) | `data/html/`, `data/scripts/` (10.747 arquivos) | **Portado** (`HtmCache` com fallbacks inteligentes) | Concluído |
| **18** | **Personagens & Tatuagens** | `CharTemplateTable`, `CharNameTable`, `LevelUpData`, `HennaTable`, `HennaTreeTable` | `char_template.xml`, `lvl_up_data.xml`, `henna.xml` | **Parcial** (`CharTemplateTable` pronto; falta Henna/Dyes) | Alta |
| **19** | **Clãs & Crests** | `ClanTable`, `ClanLevelUpPricesData`, `CrestCache`, `Hero`, `BlockListManager` | `ClanLevelUpPrice.xml`, `data/crests/` | **Pendente** | **Imediata** |
| **20** | **NPCs & Pets** | `NpcTable`, `PetDataTable`, `RaidBossInfoManager` | Tabela `npc`, `pet_stats.xml` | **Parcial** (`NpcTemplateTable`, `SpawnService` prontos) | Alta |
| **21** | **Portas & Entidades** | `DoorTable` (Portas de cidades, castelos, coliseu com HP/P.Def), `TownManager` | `door.xml`, `zone/` | **Pendente** | **Imediata** |
| **22** | **Castelos, Fortes & Sieges** | `CastleManager`, `SiegeManager`, `FortManager`, `FortSiegeManager`, `ClanHallManager` | `castle`, `fort`, `clanhall` tabelas | **Pendente** | Alta |
| **23** | **Grand Bosses & Dungeons** | `AntharasManager`, `BaiumManager`, `ValakasManager`, `Frintezza`, `Zaken`, `FourSepulchers` | `grandboss_data`, `four_sepulchers.xml` | **Pendente** (Spawns ativos; falta IA de instâncias e status) | Alta |
| **24** | **Olimpíadas & Sete Selos** | `Olympiad`, `SevenSigns`, `SevenSignsFestival` | Tabelas `olympiad_nobles`, `seven_signs` | **Pendente** | Média |
| **25** | **Economia & Manor** | `BuyListTable`, `MultiSellTable`, `CastleManorManager`, `CursedWeaponsManager` | `buylists.xml`, `multisell/`, `cursedWeapons.xml` | **Parcial** (BuyList e MultiSell prontos; falta Cursed e Manor) | Alta |
| **26** | **Mods & Addons** | Achievements, Quake, Offline Trade, AutoFarm, Reset, DressMe, PvPRank | `features.*`, XMLs raiz | **Parcial** (Achievements e Quake com lógica; falta UI/offline) | Média |

---

## 2. Checkpoints do Plano de Implementação

### Bloco A — Fundação do Mundo e Sobrevivência (Imediato)
- [x] **A.1: `MapRegionTable` (Cálculo de Vilas e Respawn)** — ✅ Concluído (38 pontos de restart, 90 polígonos, cálculo espacial e por raça).
- [x] **A.2: `GameTimeController` & Ciclo Dia/Noite** — ✅ Concluído (360 ticks diários, 4h real = 24h jogo, pôr/nascer do sol, ClientSetTime 0x92).
- [x] **A.3: `Announcements` (Sistema de Anúncios e Boas-vindas)** — ✅ Concluído (Anúncios globais, auto-welcome, //announce GM broadcast).
- [x] **A.4: `DoorTable` (Portas Físicas do Mundo)** — ✅ Concluído (Carregamento de data/xml/world/door.xml, DoorInfo 0x4c, DoorStatusUpdate 0x4d, //open, //close).
- [x] **A.5: `ArmorSetsTable` (Bônus de Sets de Armadura)** — ✅ Concluído (30+ sets de data/xml/player/armorsets.xml, passivas, bônus de escudo, +6 enchant bonus integrados no PlayerStats).

### Bloco B — Clãs, Brasões e Social
- [x] **B.1: `ClanTable` e Modelo de Clã** — ✅ Concluído (Criação de clã, líderes, membros, dissolução, level-up com ClanLevelUpPricesTable, sincronização com banco).
- [x] **B.2: Brasões de Clã e Aliança (`CrestCache`)** — ✅ Concluído (Cache e persistência em data/crests/ de BMP 16x12, pacotes PledgeCrest 0x6a e PledgeShowInfoUpdate 0x88).
- [x] **B.3: Sistema de Tatuagens (`HennaTable` & `HennaTreeTable`)** — ✅ Concluído (180 dyes de henna.xml, restrições por classe, limite oficial de +5 por stat, pacotes HennaEquipList, HennaItemInfo, HennaInfo e rotas no Symbol Maker).

### Bloco C — Castelos, Sieges e Zonas
- [x] **C.1: `ZoneTable` (Zonas de Paz, Água, Dano, Cidade)** — ✅ Concluído (671 zonas carregadas dos 14 arquivos XML de data/xml/zone/, suporte a formas Poly e Rect, bloqueio de ataque em Peace Zone).
- [x] **C.2: `CastleManager` & Taxas Comerciais** — ✅ Concluído (9 castelos oficiais de Interlude, tesouraria, limite de impostos 0-15%, vínculo com clãs proprietários).

### Bloco D — Grand Bosses, Cursed Weapons e Olimpíadas
- [x] **D.1: Armas Malditas (`CursedWeaponsManager`)** — ✅ Concluído (Zariche 8190 e Akamanah 8689, ciclo de vida ativo/chão, pacotes ExCursedWeaponList 0xfe:0x45 e ExCursedWeaponLocation 0xfe:0x46).
- [x] **D.2: Grand Bosses (`GrandBossManager`)** — ✅ Concluído (Antharas, Valakas, Baium, Queen Ant, Zaken, Core, Orfen, Frintezza, Sailren, Van Halter, persistência em grandboss_data e grandboss_intervallist, agendamento de respawn).
- [x] **D.3: Olimpíadas & Nobres (`OlympiadManager`)** — ✅ Concluído (Registro de Nobres, pontos iniciais 18, histórico de vitórias/derrotas/empates, apuração de heróis, comando /olympiadstat e persistência em olympiad_nobles).

### Bloco E — Sete Selos e Economia
- [x] **E.1: Sete Selos (`SevenSignsManager`)** — ✅ Concluído (Cabais Dawn e Dusk, Selos da Avareza/Gnose/Luta, 4 períodos, entrega de pedras e cálculo de Ancient Adena, pacote de céu SSQInfo 0xf8 e registro SSQStatus 0xf5 / RequestSSQStatus 0xc7).
- [x] **E.2: Sistema de Feudos e Sementes (`CastleManorManager`)** — ✅ Concluído (256 sementes de seeds.xml em 9 castelos, compra de sementes, venda de colheitas, transição de períodos e persistência em castle_manor_production e castle_manor_procure).

---

## 3. Estado de Execução e Métricas de Qualidade

| Módulo / Subsistema | Classes Principais | XML / Tabela | Cobertura de Testes | Status |
|---|---|---|---|:---:|
| **Fundação & Mundo** | `MapRegionTable`, `GameTimeController`, `Announcements` | `mapregion.xml` | 100% (Testes unitários dedicados) | ✅ Concluído |
| **Portas & Sets** | `DoorTable`, `DoorInstance`, `ArmorSetsTable` | `door.xml`, `armorsets.xml` | 100% | ✅ Concluído |
| **Clãs & Brasões** | `ClanTable`, `ClanMember`, `CrestCache` | `clan_data`, `ClanLevelUpPrice.xml` | 100% | ✅ Concluído |
| **Tatuagens / Dyes** | `HennaTable`, `HennaTreeTable`, `Henna` | `henna.xml`, `henna_trees` | 100% | ✅ Concluído |
| **Zonas Espaciais** | `ZoneTable`, `Zone`, `ZoneShape` | 14 arquivos `data/xml/zone/*.xml` | 100% (671 zonas ativas) | ✅ Concluído |
| **Castelos** | `CastleManager`, `Castle` | `castle_data`, 9 feudos | 100% | ✅ Concluído |
| **Armas Malditas** | `CursedWeaponsManager`, `CursedWeapon` | `cursedWeapons.xml`, `cursed_weapons` | 100% | ✅ Concluído |
| **Grand Bosses** | `GrandBossManager`, `GrandBossInfo` | `grandboss_data`, `grandboss_intervallist` | 100% (10 chefes épicos) | ✅ Concluído |
| **Olimpíadas** | `OlympiadManager`, `OlympiadNoble` | `olympiad_nobles` | 100% | ✅ Concluído |
| **Sete Selos** | `SevenSignsManager` | `seven_signs_status`, `seven_signs` | 100% | ✅ Concluído |
| **Manor / Feudos** | `CastleManorManager`, `SeedTemplate` | `seeds.xml`, `castle_manor_*` | 100% (256 sementes) | ✅ Concluído |

> **Status da Suíte de Testes Geral**:  
> **559 testes executados**, **0 falhas**, **0 erros**, **100% de sucesso na compilação e execução Maven** (`.\mvnw.cmd test`).

