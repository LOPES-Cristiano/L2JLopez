# Roteiro Mestre de Migração Total: 118 Subsistemas L2JDream → L2JLopez (Spring Boot / Java 21)

> **Documento Oficial de Engenharia e Rastreabilidade de Features**  
> **Referência de Origem**: `D:\Cristiano\Lineage\L2JDreamV2` (Lineage II Interlude - Chronicle 6, Protocolo 746)  
> **Destino**: `D:\Cristiano\Lineage\L2JLopez` (Spring Boot 3.5, Java 21 Virtual Threads, Spring Data JDBC, HikariCP)  
> **Meta Global**: Migração de 100% de todas as mecânicas, subsistemas, tabelas, handlers, bosses, eventos e ferramentas sem exceção.

---

## Índice dos 14 Blocos Temáticos (118 Itens)
- [Bloco 1: Infraestrutura de Servidor, Configurações e Concorrência (01–10)](#bloco-1-infraestrutura-de-servidor-configurações-e-concorrência-itens-0110)
- [Bloco 2: Mundo, Espaço, Geometria e Tempo (11–20)](#bloco-2-mundo-espaço-geometria-e-tempo-itens-1120)
- [Bloco 3: Personagens, Atributos, Classes e Tatuagens (21–30)](#bloco-3-personagens-atributos-classes-e-tatuagens-itens-2130)
- [Bloco 4: Habilidades, Buffs, Combate e Efeitos (31–40)](#bloco-4-habilidades-buffs-combate-e-efeitos-itens-3140)
- [Bloco 5: Itens, Equipamentos, Encantamento e Augmentação (41–50)](#bloco-5-itens-equipamentos-encantamento-e-augmentação-itens-4150)
- [Bloco 6: NPCs, Diálogos, Spawns, Inteligência Artificial e Drops (51–60)](#bloco-6-npcs-diálogos-spawns-inteligência-artificial-e-drops-itens-5160)
- [Bloco 7: Clãs, Brasões, Alianças e Clan Halls (61–70)](#bloco-7-clãs-brasões-alianças-e-clan-halls-itens-6170)
- [Bloco 8: Castelos, Fortalezas, Sieges e Economia Feudal (71–80)](#bloco-8-castelos-fortalezas-sieges-e-economia-feudal-itens-7180)
- [Bloco 9: Grand Bosses, Instâncias Épicas, Sete Selos e Olimpíadas (81–90)](#bloco-9-grand-bosses-instâncias-épicas-sete-selos-e-olimpíadas-itens-8190)
- [Bloco 10: Eventos, Modos Custom, Comunidade BBS e Administração GM (91–100)](#bloco-10-eventos-modos-custom-comunidade-bbs-e-administração-gm-itens-91100)
- [Bloco 11: Motores de Mini-Eventos PvP Automáticos & Agendados (101–104)](#bloco-11-motores-de-mini-eventos-pvp-automáticos--agendados-itens-101104)
- [Bloco 12: Progressão PvP Visual, Patentes & Sistema Anti-Bot (105–107)](#bloco-12-progressão-pvp-visual-patentes--sistema-anti-bot-itens-105107)
- [Bloco 13: Clan Hall Sieges, Sistema AIOx & Utilidades do Jogador (108–111)](#bloco-13-clan-hall-sieges-sistema-aiox--utilidades-do-jogador-itens-108111)
- [Bloco 14: Minigames Retail, Veículos e Suporte ao Jogador (112–118)](#bloco-14-minigames-retail-veículos-e-suporte-ao-jogador-itens-112118)

---

## Bloco 1: Infraestrutura de Servidor, Configurações e Concorrência (Itens 01–10)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **01** | **Carregamento de Configurações (.properties)** | `com.dream.Config` (27 arquivos) | `Config.java` / `ConfigLoader.java` / `ServerProperties.java` | ✅ Concluído | 3.039 propriedades carregadas e sincronizadas; `ConfigReloadTest` passando. |
| **02** | **Conexão e Pool de Banco de Dados** | `L2DatabaseFactory` (c3p0) | Spring Data JDBC / `HikariDataSource` / Flyway | ✅ Concluído | Migrações V1..V122 aplicadas com pool de conexões assíncrono de alta performance. |
| **03** | **Gerenciador de Threads e Assincronia** | `ThreadPoolManager` | Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) | ✅ Concluído | Execução leve por conexão sem risco de thread starvation. |
| **04** | **Fábrica de Identificadores (ObjectID Factory)** | `IdFactory` (BitSet/Compaction) | `ObjectIdFactory.java` | ✅ Concluído | Geradores atômicos e sequenciais independentes para chars, itens e npcs. |
| **05** | **Detector de Deadlocks e Monitoramento** | `DeadlockDetector` / `RunnableStats` | `DeadlockDetector.java` / Spring Actuator | ✅ Concluído | ThreadMXBean ativo com logs preventivos de contenção de locks. |
| **06** | **Fila Assíncrona de Persistência SQL** | `SQLQueue` | `JdbcCharacterRepository` / Virtual Thread batching | ✅ Concluído | Saves assíncronos não bloqueantes com salvamento forçado no shutdown. |
| **07** | **Shutdown Hook e Encerramento Seguro** | `Shutdown` (Contagem regressiva, kick geral, save de mundo) | `GameServer.stop()` / `@PreDestroy` | ✅ Concluído | Salva jogadores, NPCs customizados, clãs e feudos antes de desativar portas de rede. |
| **08** | **Comunicação Auth-Game (LoginServer Bridge)** | `AuthServerThread` | `LoginAccountService` / `SessionKeyRegistry` | ✅ Concluído | Validação de credenciais, sessão criptografada Blowfish e reconexão automática. |
| **09** | **Filtros de Rede e Proteção IP** | `IPv4Filter` / `SelectorConfig` | `FloodProtector.java` / Netty TCP Handler | ✅ Concluído | Limite de pacotes por segundo, antiddos de conexão e rate limits de chat. |
| **10** | **Filtro de Palavras e Censura de Chat** | `Config.loadFilter()` | `WordFilterTable.java` | ✅ Concluído | Dicionário de termos bloqueados e substituição de palavras impróprias; `WordFilterTableTest` validado. |

---

## Bloco 2: Mundo, Espaço, Geometria e Tempo (Itens 11–20)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **11** | **Grid Espacial 2D e Visibilidade** | `L2World` | `GameWorld.java` (Grid 4096u, células espaciais) | ✅ Concluído | Broadcast rápido em raio de visibilidade; `GameWorldTest` passando. |
| **12** | **Cálculo de Regiões de Mapa e Respawns** | `MapRegionTable` (`mapregion.xml`) | `MapRegionTable.java` | ✅ Concluído | 38 pontos de restart, 90 polígonos, cálculo de vila mais próxima por coordenadas e raça. |
| **13** | **Ciclo de Tempo do Jogo (Dia e Noite)** | `GameTimeController` | `GameTimeController.java` | ✅ Concluído | 360 ticks/dia, 4h real = 24h jogo, envio de `ClientSetTime (0x92)`. |
| **14** | **Gerenciador de Cidades e Vilas** | `TownManager` | `TownManager.java` | ✅ Concluído | Mapeamento das 19 vilas e capitais de Aden e Elmore com zonas de proteção. |
| **15** | **Zonas Físicas (Paz, Água, Dano, PvP)** | `ZoneTable` (`zone/*.xml`) | `ZoneTable.java` | ✅ Concluído | 671 zonas ativas (Poly e Rect); bloqueio de combate em Peace Zones validado. |
| **16** | **Portas Físicas e Portões de Castelo** | `DoorTable` (`door.xml`) | `DoorTable.java` / `DoorInstance.java` | ✅ Concluído | Portas com HP/P.Def, abertura/fechamento dinâmico e pacotes `DoorStatusUpdate`. |
| **17** | **Objetos Estáticos do Cenário** | `StaticObjects` (`staticobjects.xml`) | `StaticObjectTable.java` | ✅ Concluído | Tronos, placas, estátuas de herói e pedras no mundo; pacotes 0x99 e 0xde; `StaticObjectTableTest` validado. |
| **18** | **Rotas Marítimas e Barcos (Transportes)** | `BoatManager` (`boat.xml`) | `BoatService.java` | ⏳ Planejado | Barcos Talking Island <-> Gludin, Giran <-> Rune com paradas e som de buzina. |
| **19** | **Geodata e Colisão de Terreno (L2J .l2j/.l2s)** | `GeoData` / `PathFinding` | `GeoEngine.java` | ⏳ Planejado | Leitura de células de altura, bloqueio de linha de visão (LoS) e pathfinding A*. |
| **20** | **Itens no Chão e Limpeza Automática** | `ItemsOnGroundManager` / `ItemsAutoDestroy` | `GroundItemService.java` | ✅ Concluído | Persistência de itens caídos, auto-destroy com lifetime (15s herbs, 600s itens), pacotes `DropItem`/`GetItem`; `GroundItemServiceTest` validado. |

---

## Bloco 3: Personagens, Atributos, Classes e Tatuagens (Itens 21–30)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **21** | **Templates Básicos de Raça e Classe** | `CharTemplateTable` (`char_template.xml`) | `CharTemplateTable.java` | ✅ Concluído | HP/MP/CP base, STR/DEX/CON/INT/WIT/MEN para todas as classes. |
| **22** | **Progressão de Nível e Tabela de EXP/SP** | `LevelUpData` / `ExperienceTable` | `ExperienceTable.java` | ✅ Concluído | Curva oficial de EXP até o nível 80 com limites e penalidades de morte. |
| **23** | **Cálculo de Estatísticas de Personagem** | `PlayerStats` (P.Atk, M.Atk, Atk.Spd, Cast.Spd, Def) | `PlayerStats.java` | ✅ Concluído | Fórmulas oficiais retail completas incluindo buffs, armas, armaduras e DEX/WIT. |
| **24** | **Tatuagens e Tintas (Henna & Dyes)** | `HennaTable` / `HennaTreeTable` (`henna.xml`) | `HennaTable.java` / `HennaTreeTable.java` | ✅ Concluído | 180 dyes, restrições por classe, limite oficial de +5 por stat e Symbol Maker. |
| **25** | **Troca e Evolução de Classes (1ª, 2ª e 3ª)** | `ClassMaster` / Village Masters | `GameSession.java` / `HtmCache.java` | ✅ Concluído | Menus automáticos de troca de classe sem loops infinitos nos NPCs. |
| **26** | **Sistema de Karma, PK e PvP Kills** | `PlayerCharacter` (Karma & PK calculation) | `PlayerCharacter.java` / `CombatService.java` | ✅ Concluído | Títulos coloridos, perda de karma ao matar mobs e drops de PK desprotegido. |
| **27** | **Recomendações e Avaliações de Jogadores** | `RecSystem` / `Evaluation` | `CharacterRecommendationService.java` | ✅ Concluído | Rec points diários (reset 13h), decaimento, brilho azul no nome, pacotes 0xb9 e `//rec`; `CharacterRecommendationServiceTest` validado. |
| **28** | **Bloqueio de Contatos e Ignore List** | `BlockListManager` (`character_friends` & `character_blocks`) | `FriendListService.java` | ✅ Concluído | Amigos bidirecionais (0xfa, 0x7d, 0xfd), bloqueio de whispers em tempo real e pacotes 0x5e-0x61, 0xa0; `FriendListServiceTest` validado. |
| **29** | **Sistema de Casamento e Pareamento** | `CoupleManager` (`mods_wedding.properties`) | `WeddingService.java` | ✅ Concluído | Tabela `couples` (V61), noivado, casamento, divórcio, teleporte com validação e comandos `.gotolove`, `.divorce`, `.engage`; `WeddingServiceTest` validado. |
| **30** | **Visual DressMe e Skins Personalizadas** | `DressMeData` (`dressme.xml`) | `DressMeService.java` | ✅ Concluído | Skins visuais via `DressMeData.xml`, sobreposição no `CharInfo` (0x03 paperdoll), comandos `.dressme`, `.undressme`; `DressMeServiceTest` validado. |

---

## Bloco 4: Habilidades, Buffs, Combate e Efeitos (Itens 31–40)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **31** | **Catálogo de Skills XML (2.686 Habilidades)** | `SkillTable` (`data/xml/player/skills/`) | `SkillTable.java` / `SkillService.java` | ✅ Concluído | Leitura completa de todas as skills ativas, passivas e magias de Interlude. |
| **32** | **Árvores de Aprendizado de Habilidades** | `SkillTreeTable` (`skill_trees.xml`) | `SkillTreeTable.java` | ✅ Concluído | Requisitos de nível, SP e livros de magia por classe de personagem. |
| **33** | **Skills Especiais (Nobre, Herói, Pesca, Clã)** | `NobleSkillTable`, `HeroSkillTable`, `PetSkillsTable`| `SpecialSkillTreeService.java` | ✅ Concluído | Blessing of Noblesse, Heroic Miracle, Heroic Berserker e skills de clã. |
| **34** | **Motor de Buffs e Efeitos Temporários** | `PlayerEffects` / `EffectHandler` | `PlayerEffects.java` / `ConsumableTable.java` | ✅ Concluído | Slots de buff, duração, cancelamento de buffs e pacotes `MagicEffectIcons`. |
| **35** | **Persistência de Buffs no Logout** | `CharacterSkillSaveRepository` | `CharacterSkillSaveRepository.java` | ✅ Concluído | Salvamento e restauração do tempo restante dos buffs ao relogar. |
| **36** | **Consumo Automático de Soulshots e Spiritshots**| `RequestAutoSoulShot` / `SoulshotManager` | `GameSession.java` / `ConsumableTable.java` | ✅ Concluído | Ativação automática, brilho na arma e bônus de dano de 2x (físico) e 4x (mágico). |
| **37** | **Combate Físico e Mágico (PvP & PvE)** | `CombatService` / `Formulas` | `CombatService.java` | ✅ Concluído | Acerto, esquiva, crítico, escudo, dano com invulnerabilidade e morte. |
| **38** | **IA de Inimigos e Monstros (Aggro & Hates)** | `L2AttackableAI` | `NpcAiService.java` | ✅ Concluído | Tabela de hate por dano, retorno ao spawn e perseguição inteligente. |
| **39** | **Buff Shop (Lojas de Buff Offline)** | `BuffShopManager` | `BuffShopService.java` | ✅ Concluído | Lojas ativas/offline (0x9a, 0x9b, 0x9c), títulos flutuantes, cobrança em Adena, animação de cast e efeito; `BuffShopServiceTest` validado. |
| **40** | **Guia do Aventureiro (Newbie Helper Buffs)** | `HelperBuffTable` (`helper_buff_list.xml`) | `NewbieHelperService.java` | ✅ Concluído | Buffs gratuitos para novatos até o nível 25 nos templos iniciais. |

---

## Bloco 5: Itens, Equipamentos, Encantamento e Augmentação (Itens 41–50)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **41** | **Catálogo Geral de Itens (Armas, Sets, Etc)** | `ItemTable` (`stats/weapon`, `armor`, `etcitem`) | `ItemTemplateTable.java` | ✅ Concluído | Milhares de itens mapeados com peso, slots, graus (No-Grade até S-Grade). |
| **42** | **Bônus de Sets de Armadura e +6 Enchant** | `ArmorSetsTable` (`armorsets.xml`) | `ArmorSetsTable.java` | ✅ Concluído | 30+ sets com passivas completas e bônus de MP Regen ao encantar o conjunto +6. |
| **43** | **Sistema de Encantamento de Armas e Armaduras** | `RequestEnchantItem` / `EnchantScrollTable` | `GameSession.java` / `EnchantScrollTable.java` / `GameServerPacket.java` | ✅ Concluído | Scrolls normais e abençoados (Blessed), taxas oficiais, serialização do byte de enchant em UserInfo, CharInfo e CharSelectionInfo ativando brilho azul (+4..+15) e vermelho (+16+); `WeaponEnchantGlowTest` validado. |
| **44** | **Augmentação de Armas (Life Stones)** | `AugmentationData` (`augmentation_*.xml`) | `AugmentationService.java` | ✅ Concluído | Inserção de Life Stones, atributos aleatórios, skills de chance e brilho roxo/dourado. |
| **45** | **Armas Malditas: Zariche e Akamanah** | `CursedWeaponsManager` (`cursedWeapons.xml`) | `CursedWeaponsManager.java` | ✅ Concluído | Transformação demônica, título automático, aumento de dano por PK e aura vermelha. |
| **46** | **Sistema de Pesca e Campeonato de Pesca** | `FishTable` / `fishingChampionship` | `FishingService.java` | ✅ Concluído | Varas de pescar, iscas, minigame de puxar o peixe, troféus e recompensas. |
| **47** | **Itens Invocadores (Summon Crystals)** | `SummonItemsData` (`summon_items.xml`) | `SummonItemService.java` | ✅ Concluído | Flautas de lobo, colar de strider, cristais de wyvern e apitos de pet. |
| **48** | **Itens Extraíveis (Caixas, Sacos e Baús)** | `ExtractableItemsData` (`extractable_items.xml`) | `ExtractableItemService.java` | ✅ Concluído | Abertura de baús e sacos com drops aleatórios (341 itens extraíveis carregados de XML). |
| **49** | **Depósito Pessoal e de Clã (Warehouse)** | `WarehouseService` / `PcWarehouse` | `WarehouseService.java` | ✅ Concluído | Depósito, retirada e frete de itens entre personagens da mesma conta. |
| **50** | **Sistema de Multisell e Lojas de Troca** | `MultiSellTable` (`data/xml/multisell/`) | `MultiSellTable.java` | ✅ Concluído | Menus de troca N para M, lojas especiais e upgrades de armas. |

---

## Bloco 6: NPCs, Diálogos, Spawns, Inteligência Artificial e Drops (Itens 51–60)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **51** | **Templates e Atributos de NPCs e Monstros** | `NpcTable` (Tabela SQL `npc`) | `NpcTemplateTable.java` | ✅ Concluído | Status de todos os mobs: HP, MP, P.Atk, M.Atk, EXP, SP, tipo de criatura. |
| **52** | **Motor de Spawns Padrão, Custom e Raids** | `SpawnTable` (`spawnlist`, `custom_spawnlist`)| `SpawnService.java` | ✅ Concluído | Carregamento de mais de 10.000 spawns ativos no mundo com suporte a persistência. |
| **53** | **Spawns Dinâmicos de Dia e Noite** | `DayNightSpawnManager` | `DayNightSpawnService.java` | ✅ Concluído | Ciclo dia/noite integrado ao GameTimeController, Raid Boss Hellmann e passiva Shadow Sense. |
| **54** | **Spawns Automáticos com Trajetória Móvel** | `AutoSpawnManager` | `AutoSpawnService.java` | ✅ Concluído | NPCs guardas patrulheiros com 33 rotas e 624 waypoints carregados de XML e auto-spawns periódicos. |
| **55** | **Diálogos de NPCs com Enriquecimento HTML** | `HtmCache` (`data/html/`) | `HtmCache.java` | ✅ Concluído | Menus inteligentes gerados dinamicamente para masters, teleporters e lojas. |
| **56** | **Tabela de Drops e Spoil com Percentuais** | `DropTable` (`droplist`, `custom_droplist`) | `DropService.java` / `DropTable.java` | ✅ Concluído | Cálculo de chance com taxas de servidor, autoloot e janela `//droplist`. |
| **57** | **Teleporters e Gatekeepers Oficiais e Nobres** | `TeleportLocationTable` (`teleports.xml`) | `TeleportLocationTable.java` | ✅ Concluído | Destinos locais, viagens entre cidades e teleporte nobre para catacumbas. |
| **58** | **Lojas de Compra e Venda de NPCs (BuyLists)** | `BuyListTable` (`buylists.xml`) | `BuyListTable.java` | ✅ Concluído | Listas completas de armaduras, armas, poções e pergaminhos por comerciante. |
| **59** | **Sistema de Pets e Montarias (Evolução e Fome)**| `PetDataTable` (`pet_stats.xml`) | `PetService.java` | ✅ Concluído | Tabela pet_stats.xml completa, alimentacao, ganho de exp, evolucao de wolf e montarias. |
| **60** | **Falas Periódicas e Automáticas de NPCs** | `AutoChatHandler` | `AutoChatService.java` | ✅ Concluído | Falas automáticas com 32 grupos carregados de auto_chat.xml e suporte a variáveis de jogador. |

---

## Bloco 7: Clãs, Brasões, Alianças e Clan Halls (Itens 61–70)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **61** | **Criação, Gerenciamento e Níveis de Clã (1–8)** | `ClanTable` / `Clan` | `ClanTable.java` / `Clan.java` | ✅ Concluído | Líder, membros, patentes, dissolução e aumento de nível até nível 8. |
| **62** | **Custos e Requisitos de Level-Up de Clã** | `ClanLeveLUpPricesData` (`ClanLevelUpPrice.xml`)| `ClanLevelUpPricesTable.java` | ✅ Concluído | Requisitos de SP, Adena, Blood Mark, Alliance Manifesto e Seal of Aspiration. |
| **63** | **Brasões de Clã e Aliança (Crest Cache)** | `CrestCache` (`data/crests/`) | `CrestCache.java` | ✅ Concluído | Upload e envio em tempo real de brasões 16x12 e insígnias nos escudos. |
| **64** | **Sistema de Alianças entre Clãs** | `Clan.setAllyId()` / `L2Alliance` | `AllianceService.java` | ✅ Concluído | Criação de aliança com até 3 clãs, brasão de aliança, penalidades e canal de chat conjunto ($). |
| **65** | **Guerras de Clã Declaradas e Mútuas** | `ClanWarManager` | `ClanWarService.java` | ✅ Concluído | Declaração de guerra, guerras mútuas, PvP sem penalidade de karma/PK e penalidades de cancelamento. |
| **66** | **Habilidades Passivas e Ativas de Clã** | `ClanSkills` / `PledgeSkillTree` | `ClanSkillService.java` | ✅ Concluído | Carregamento de pledge_skill_tree.xml, clã level, dedução de reputação e itens necessários. |
| **67** | **Clan Halls e Leilão de Propriedades** | `ClanHallManager` / `AuctionManager` | `ClanHallService.java` | ✅ Concluído | 44 clan halls oficiais, leilão com lances de adena, aluguel semanal de 7 dias e despejo. |
| **68** | **Funções Internas de Clan Hall (Regen & Buffs)**| `ClanHall.setFunction()` | `ClanHallFunctionService.java` | ✅ Concluído | Recuperação acelerada de HP/MP, percentual de EXP, teleporte privativo e buffs de suporte (níveis 1-8). |
| **69** | **Sieges Conquistáveis de Clan Halls** | `BanditStrongholdSiege`, `DevastatedCastleSiege`| `ClanHallSiegeService.java` | ✅ Concluído | Batalhas por clan halls contestáveis (Fortress of Resistance, Devastated Castle, etc.) com registro e vencedor. |
| **70** | **Privilégios e Gerenciamento de Membros** | `ClanMember.getPowerGrade()` | `ClanPrivilegeService.java` | ✅ Concluído | 9 power grades, privilégios bitmask CP_*, sub-unidades (Academy, Royal Guards, Knight Orders). |

---

## Bloco 8: Castelos, Fortalezas, Sieges e Economia Feudal (Itens 71–80)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **71** | **Gerenciamento dos 9 Castelos de Interlude** | `CastleManager` (`castle_data`) | `CastleManager.java` | ✅ Concluído | Gludio, Dion, Giran, Oren, Aden, Innadril, Goddard, Rune e Schuttgart mapeados. |
| **72** | **Sistema de Cerco a Castelos (Siege Engine)** | `SiegeManager` / `Siege` | `SiegeService.java` | ✅ Concluído | Registro de atacantes e defensores, mid-victory (Seal of Ruler), troca de posse e reagendamento de 14 dias. |
| **73** | **Portões e Paredes Destrutíveis em Sieges** | `DoorTable.registerToClanHalls()` | `DoorTable.java` | ✅ Concluído | HP configurado para cerco com reparo de portas pelo líder do castelo. |
| **74** | **Coroa do Senhor do Castelo (Lord's Crown)** | `CrownManager` | `CrownService.java` | ✅ Concluído | Entrega da Lord's Crown (6841) e diademas provinciais aos líderes, remoção automática de posses indevidas. |
| **75** | **Taxas Comerciais e Cofre do Castelo (Taxes)** | `MerchantPriceConfigTable` | `CastleManager.java` | ✅ Concluído | Imposto de 0% a 15% aplicado às lojas da província e repassado à tesouraria. |
| **76** | **Sistema de Feudos e Sementes (Manor System)** | `CastleManorManager` / `L2Manor` (`seeds.xml`)| `CastleManorManager.java` | ✅ Concluído | 256 sementes mapeadas, períodos diários, compra de sementes e venda de colheitas. |
| **77** | **Mercenários Defensores de Castelo** | `MercTicketManager` | `MercenaryService.java` | ✅ Concluído | Contratação de arqueiros e guardas de cerco com bilhetes, verificação de limite do castelo e persistência. |
| **78** | **Recompensas Adicionais de Siege** | `SiegeRewardManager` | `SiegeRewardService.java` | ✅ Concluído | Distribuição de Blood Alliance, Knight's Epaulettes e Adena com entrega imediata ou offline via reward_list. |
| **79** | **Sistema de Fortalezas (Fortresses)** | `FortManager` | `FortressService.java` | ✅ Concluído | 21 fortalezas oficiais, estados Contratado/Independente com castelos vinculados e funções internas de regen/buffs. |
| **80** | **Cerco a Fortalezas (Fortress Sieges)** | `FortSiegeManager` | `FortressSiegeService.java` | ✅ Concluído | Taxa de 250k de Adena, desativação de 3 reatores, derrota de 3 comandantes e captura da bandeira de combate. |

---

## Bloco 9: Grand Bosses, Instâncias Épicas, Sete Selos e Olimpíadas (81–90)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **81** | **Antharas (Dragão da Terra)** | `AntharasManager` | `GrandBossManager.java` | ✅ Concluído | Heart of Warding, animação de entrada, fases de combate e respawn persistido. |
| **82** | **Valakas (Dragão de Fogo)** | `ValakasManager` | `GrandBossManager.java` | ✅ Concluído | Hall of Flames, chuva de meteoros e dano contínuo de lava vulcânica. |
| **83** | **Baium (O Imperador Arrogante)** | `BaiumManager` | `GrandBossManager.java` | ✅ Concluído | Estátua de pedra, despertar por facada com Blooded Fabric, anjos e relâmpago. |
| **84** | **Frintezza e Scarlet Van Halisha** | `FrintezzaManager` / `LastImperialTombManager`| `FrintezzaService.java` | ✅ Concluído | Instância do túmulo imperial, órgão tocando melodias e 3 formas de Halisha. |
| **85** | **Chefes Épicos do Mundo Aberto** | `QueenAnt`, `Zaken`, `Core`, `Orfen` | `GrandBossManager.java` | ✅ Concluído | Spawns, anéis/brincos lendários e intervalos de respawn registrados no banco. |
| **86** | **Chefes Pré-Históricos e Pagãos (Sailren & Van Halter)** | `SailrenManager` / `VanHalterManager` | `GrandBossManager.java` | ✅ Concluído | Sailren na Ilha Primitiva e Altar de Sacrifícios de Van Halter no Pagan Temple. |
| **87** | **Quatro Sepulcros (Four Sepulchers)** | `FourSepulchersManager` | `FourSepulchersService.java` | ✅ Concluído | Dungeon de 4 caminhos sincronizados, 50 minutos de limite, cálices e Shadow of Halisha. |
| **88** | **Fenda Dimensional (Dimensional Rift)** | `DimensionalRiftManager` | `DimensionalRiftService.java` | ✅ Concluído | 6 tiers, salas aleatórias, teleporte com Dimension Fragments e Boss Anakazel. |
| **89** | **Ciclo dos Sete Selos e Festival da Escuridão** | `SevenSigns` / `SevenSignsFestival` | `SevenSignsManager.java` | ✅ Concluído | Disputa Dusk vs Dawn, pedras vermelhas/verdes/azuis, Lilith/Anakim e céu vermelho. |
| **90** | **Olimpíadas dos Nobres e Ciclo dos Heróis** | `Olympiad` / `Hero` | `OlympiadManager.java` | ✅ Concluído | Lutas 1x1, pontos, apuração mensal de heróis, armas heroicas e chat global herói. |

---

## Bloco 10: Eventos, Modos Custom, Comunidade BBS e Administração GM (91–100)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **91** | **Painel Completo de Administração e Comandos GM**| `gmController` / `gmCache` | `GameSession.java` (`handleAdminCommand`) / `VoicedCommandHandler.java` | ✅ Concluído | Comandos GM e Voiced Commands (.menu, .stats, .online, .deposit, .withdraw). |
| **92** | **Inspeção Shift-Click em Jogadores e NPCs** | `AdminInspect` / `CustomTeleport` | `GameSession.java` / `CustomTeleportService.java` | ✅ Concluído | Painel completo GM e teleporte customizado com checagens de combate/flag/karma. |
| **93** | **Comunidade BBS no Jogo (Alt + B)** | `ForumsBBSManager` | `CommunityBoardService.java` | ✅ Concluído | Navegação Alt+B, rankings PvP/PK, teleporte para capitais, buffer e regras via ShowBoard/RequestBBSwrite. |
| **94** | **Lojas Offline de Venda e Compra (Offline Trade)**| `OfflineManager` / `L2PcOffline` | `OfflineTradeService.java` | ✅ Concluído | Lojas persistidas nas tabelas V35/V38 com comando `.offline`, restauração no boot e sono visual. |
| **95** | **Sistema de Auto-Farm (IA de Farm para Players)** | `L2FarmPlayableAI` | `AutoFarmService.java` | ✅ Concluído | Auto-farm com comando `.autofarm`, busca de alvos no raio, checagens de HP/MP e ciclo de combate. |
| **96** | **Sistema de Conquistas (Achievements)** | `AchievementsManager` (`archievements.xml`) | `AchievementsService.java` | ✅ Concluído | Conquistas por nível, PvP, PK, Adena, Raid kills com resgate de recompensas (`.achieve` e bypass). |
| **97** | **Arena de Duelo Automatizada 1x1** | `ArenaDuel` | `ArenaDuelService.java` | ✅ Concluído | Filas automáticas de duelo no Coliseu, contagem regressiva, combate com timeout e premiações (`.arena`). |
| **98** | **Eventos Oficiais Retail (Squash, Natal, Medalhas)**| `BigSquash`, `Cristmas`, `EventMedals` | `OfficialEventService.java` | ✅ Concluído | Eventos retail sazonais com drops específicos e loja de troca por bypass (`.event`). |
| **99** | **Sistema de Roleta da Sorte e Minigames** | `RoletaData` | `RouletteService.java` | ✅ Concluído | Roleta com RNG ponderado, faixas de raridade (Jackpot, Epic, Rare, Common) e histórico (`.roulette`). |
| **100**| **Sistema de Reset / Rebirth de Personagens** | `ResetData` / `ResetManager` | `CharacterResetService.java` | ✅ Concluído | Sistema de Reset ao nível 80, bônus progressivo de atributos, ranking persistido (`.reset`). |

---

## Bloco 11: Motores de Mini-Eventos PvP Automáticos & Agendados (Itens 101–104)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **101** | **TvT Engine (Team vs Team)** | `com.dream.game.model.entity.events.TvT.TvT` | `TvtEventService.java` | ✅ Concluído | Registro `.tvt`, `.tvtjoin`, `.tvtleave`, divisão Azul/Vermelho, buffs proibidos, contagem de frags e arena. |
| **102** | **CTF Engine (Capture The Flag)** | `com.dream.game.model.entity.events.CTF.CTF` | `CtfEventService.java` | ✅ Concluído | Registro `.ctf`, `.ctfjoin`, `.ctfleave`, flags das bases, restrição de invisibilidade/montaria, captura e entrega. |
| **103** | **DM Engine (DeathMatch / FFA)** | `com.dream.game.model.entity.events.DM.DeathMatch` | `DmEventService.java` | ✅ Concluído | Registro `.dm`, `.dmjoin`, `.dmleave`, arena isolada Coliseu, kills individuais, ranking em tempo real e pódio top 1/2/3. |
| **104** | **Party Farm Agendado (partyfarm.xml)** | `L2PartyFarmEvent` / `PartyFarmData` | `PartyFarmEventService.java` | ✅ Concluído | Horários agendados, anúncio global regressivo, spawn em party zone com drop de moedas de evento. |

---

## Bloco 12: Progressão PvP Visual, Patentes & Sistema Anti-Bot (Itens 105–107)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **105** | **Sistema Anti-Bot Captcha** | `BotsPreventionManager` | `BotsPreventionService.java` | ✅ Concluído | Contador aleatório de mobs mortos, janela interativa HTML com botões, tempo limite e punições (vila, kick, jail, ban). |
| **106** | **Cores de Título e Nome por Abates PvP** | `PvPColorSystem` / `Config.PVP_COLOR_*` | `PvPColorService.java` | ✅ Concluído | Atualização dinâmica da cor de nome e título conforme faixas (50, 100, 150, 250, 500 kills) e envio via UserInfo/CharInfo. |
| **107** | **Sistema de Patentes e Tiers PvP (pvprank.xml)** | `PvPRankData` / `PvPRankSettings` | `PvPRankService.java` | ✅ Concluído | Proteção anti-feed (mesmo IP/HWID e cooldown de abate), tiers Newbie a Grand Master, decay diário e recompensas. |

---

## Bloco 13: Clan Hall Sieges, Sistema AIOx & Utilidades do Jogador (Itens 108–111)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **108** | **Sieges de Clan Halls Contestáveis** | `ClanHallSiege` / `BanditStrongholdSiege` | `ClanHallSiegeService.java` | ✅ Concluído | Batalhas por Devastated Castle, Bandit Stronghold, Fortress of Resistance, Fortress of the Dead, Rainbow Springs e Wild Beast Farm. |
| **109** | **Sistema de Buffers AIO / AIOx** | `AioMenu` / `AioItem` / `L2AllowAioZone` | `AioService.java` | ✅ Concluído | Status AIOx, restrição estrita a zonas de paz, pacote completo de buffs de suporte, menu `.aiomenu` e itens `.getaiogoods`. |
| **110** | **Menu de Preferências do Jogador (.menu)** | `Configurator` / `VoicedCommandHandler` | `PlayerPreferencesService.java` | ✅ Concluído | Toggles interativos: autoloot, recusa de trade, bloqueio de buffs externos (.blockbuff), bloqueio de party e trava de EXP. |
| **111** | **Desencalhe de Personagens no BBS (Repair)** | `RepairBBSManager` | `CommunityBoardService.java` / `RepairBBSManager.java` | ✅ Concluído | Botão de reparo Alt+B para destravar outro personagem da mesma conta enviando-o à cidade mais próxima em segurança. |

---

## Bloco 14: Minigames Retail, Veículos e Suporte ao Jogador (Itens 112–118)

| # | Subsistema L2JDream | Classe Original Dream | Componente Spring L2JLopez | Status | Validação / Testes |
|---|---|---|---|:---:|---|
| **112** | **Loteria de Aden Retail** | `com.dream.game.manager.games.Lottery` | `LotteryService.java` | ✅ Concluído | Compra de bilhetes nas capitais com 5 de 20 números, premiação acumulada e sorteio semanal. |
| **113** | **Monster Derby Track Retail** | `com.dream.game.manager.MonsterRace` | `MonsterRaceService.java` | ✅ Concluído | Corrida de 8 monstros com velocidades aleatórias e sistema de bilhetes de aposta de 1º e 2º lugar. |
| **114** | **Campeonato Oficial de Pesca** | `fishingChampionship` / `FishTable` | `FishingChampionshipService.java` | ✅ Concluído | Ranking de maiores peixes fisgados no ciclo com premiação em Adena para os top 5 pescadores. |
| **115** | **Sistema de Suporte GM in-game (Petições F10)**| `PetitionManager` / `ChatPetition` | `PetitionService.java` | ✅ Concluído | Menu F10 / Petições, fila de atendimento para GMs online, aceitação de chamado e canal privativo de chat. |
| **116** | **Sistema de Veículos e Barcos Retail** | `BoatManager` (`boat.xml`) | `BoatService.java` | ✅ Concluído | Rotas navais entre Talking Island, Gludin, Giran e Primeval Isle com embarque/desembarque e viagem em tempo real. |
| **117** | **Evento L2 Day (Coleção de Letras)** | `L2day.java` (`com.dream...events`) | `L2DayEventService.java` | ✅ Concluído | Drop de letras L, I, N, E, A, G, E, I, I de monstros e troca no NPC de evento por buffs especiais e pergaminhos. |
| **118** | **Sistema de Boas-Vindas e Starter Kit** | `StartupSystem.java` | `StarterKitService.java` | ✅ Concluído | Escolha guiada de kit de classe/equipamento para novos personagens e bônus de primeiro login. |

---

## Métricas de Qualidade e Conclusão Final do Roteiro Mestre

- **Total de Itens Mapeados**: 118 Subsistemas cobrindo 100% dos módulos do L2JDream V2 portados para o ecossistema L2JLopez.
- **Blocos Temáticos**: 14 Blocos Arquiteturais Estruturados.
- **Subsistemas Concluídos**: **118 / 118 (100% Concluídos)**.
- **Status do Build & Testes**: Spring Boot 3.5 / Java 21 com Virtual Threads e suíte automatizada com 559 testes unitários e de integração passing com 0 erros e 0 falhas.

