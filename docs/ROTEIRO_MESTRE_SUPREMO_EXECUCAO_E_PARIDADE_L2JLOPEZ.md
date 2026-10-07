# Roteiro Mestre Supremo de Execução, Paridade e Engenharia — L2JLopez

> **Documento Oficial de Engenharia, Regras de Negócio, Conteúdo Retail e Backlog de Execução**  
> **Versão de Referência**: Lineage II Interlude (Chronicle 6, Protocolo 746) — Base de Comparação: `L2JDream V2`  
> **Destino**: `D:\Cristiano\Lineage\L2JLopez` (Java 21 LTS, Spring Boot 3.5, Virtual Threads, Spring Data JDBC, HikariCP)  
> **Status**: **Roteiro Mestre Supremo Unificado & Plano de Execução Contínua com Checkpoints**

---

## Índice Geral do Roteiro Mestre Supremo

1. [Diagnóstico de Maturidade, Metodologia e Arquitetura Global](#1-diagnóstico-de-maturidade-metodologia-e-arquitetura-global)
2. [EIXO I: Matriz Geral dos 118 Subsistemas Estruturais (Paridade L2JDream V2)](#eixo-i-matriz-geral-dos-118-subsistemas-estruturais-paridade-l2jdream-v2)
   - 2.1 Bloco 1: Infraestrutura de Servidor, Configurações e Concorrência (01–10)
   - 2.2 Bloco 2: Mundo, Espaço, Geometria e Tempo (11–20)
   - 2.3 Bloco 3: Personagens, Atributos, Classes e Tatuagens (21–30)
   - 2.4 Bloco 4: Habilidades, Buffs, Combate e Efeitos (31–40)
   - 2.5 Bloco 5: Itens, Equipamentos, Encantamento e Augmentação (41–50)
   - 2.6 Bloco 6: NPCs, Diálogos, Spawns, IA e Drops (51–60)
   - 2.7 Bloco 7: Clãs, Brasões, Alianças e Clan Halls (61–70)
   - 2.8 Bloco 8: Castelos, Fortalezas, Sieges e Economia Feudal (71–80)
   - 2.9 Bloco 9: Grand Bosses, Instâncias Épicas, Sete Selos e Olimpíadas (81–90)
   - 2.10 Bloco 10: Eventos, Modos Custom, Comunidade BBS e GM (91–100)
   - 2.11 Bloco 11: Motores de Mini-Eventos PvP Automáticos (101–104)
   - 2.12 Bloco 12: Progressão PvP Visual, Patentes & Anti-Bot (105–107)
   - 2.13 Bloco 13: Clan Hall Sieges, Sistema AIOx & Utilidades (108–111)
   - 2.14 Bloco 14: Minigames Retail, Veículos e Suporte (112–118)
3. [EIXO II: Matriz Integral de Validações de Regras de Negócio e Requisitos Retail](#eixo-ii-matriz-integral-de-validações-de-regras-de-negócio-e-requisitos-retail)
   - 3.1 Combate e Engajamento (V.01 a V.10)
   - 3.2 Itens, Inventário e Equipamentos (V.11 a V.17)
   - 3.3 Habilidades, Conjuração e Reagentes (V.18 a V.22)
   - 3.4 Troca Direta, Lojas e Segurança Econômica Anti-Exploit (V.23 a V.27)
   - 3.5 Movimento, Terreno, Afogamento e Zonas Especiais (V.28 a V.30)
   - 3.6 Subclasses, Nobres e Olimpíadas (V.31 a V.33)
   - 3.7 Clãs, Alianças e Guerras (V.34 a V.36)
4. [EIXO III: Plano Diretor de Mecânicas, Penalidades, Bônus e Fórmulas Oficiais](#eixo-iii-plano-diretor-de-mecânicas-penalidades-bônus-e-fórmulas-oficiais)
   - 4.1 Fórmulas Oficiais de Nível (`LevelMod`) e Escala de Atributos (`statBonus.xml`)
   - 4.2 Penalidades de Grau de Equipamento (*Grade Penalty / Expertise*)
   - 4.3 Penalidades de Peso (*Weight Penalty*) nos 4 Graus
   - 4.4 Regras de Armaduras, Escudos e Bônus de Sets (+6 Enchant)
5. [EIXO IV: Arquitetura e Catálogo Completo do Motor de Quests & Tutorial](#eixo-iv-arquitetura-e-catálogo-completo-do-motor-de-quests--tutorial)
   - 5.1 Motor Nativo Java-Spring de Quests & Event Dispatcher
   - 5.2 Tutorial Inicial Completo (Quest 255) para as 5 Raças
   - 5.3 Catálogo das 18 Quests de 1ª Mudança de Classe (Níveis 18–20: 401 a 418)
   - 5.4 Catálogo das 23 Quests de 2ª Mudança de Classe (Níveis 35–40: 211 a 233)
   - 5.5 Catálogo das 31 Quests de 3ª Mudança de Classe (Sagas dos Níveis 76+)
   - 5.6 Quests de Subclasse e Nobless (234, 235, 241, 242, 246, 247)
   - 5.7 Quests de Acesso aos Grand Bosses (337, 348, 618, 119)
   - 5.8 Quests de Clã, Alianças e Reputação (501, 503, 605, 611)
   - 5.9 Quests de Reagentes, Soul Crystals e Farm S-Grade
6. [EIXO V: Inteligência Artificial de NPCs e Monstros (AI Retail Engine)](#eixo-v-inteligência-artificial-de-npcs-e-monstros-ai-retail-engine)
   - 6.1 Sistema de Facções (*Faction / Social Call*)
   - 6.2 Agressividade por Diferença de Nível (*Level Difference Aggro*)
   - 6.3 Conjuração Ativa de Habilidades de Monstros (*Monster Skills*)
   - 6.4 Spoil & Sweeper de Anões
   - 6.5 Minions e Comportamento de Raid Bosses
7. [EIXO VI: Backlog de Execução Sequencial, Checkpoints e Tracking Contínuo](#eixo-vi-backlog-de-execução-sequencial-checkpoints-e-tracking-contínuo)
   - 7.1 Checkpoints da Fase 1: Blindagem de Validações Pendentes (V.20, V.28, V.29)
   - 7.2 Checkpoints da Fase 2: Motor de Quests e Tutorial Inicial (Quest 255)
   - 7.3 Checkpoints da Fase 3: Quests de Mudança de Classe (1ª e 2ª Classe)
   - 7.4 Checkpoints da Fase 4: Sagas de 3ª Classe, Subclasse e Nobless
   - 7.5 Checkpoints da Fase 5: IA Avançada de Raid Bosses e Instâncias Épicas

---

## 1. Diagnóstico de Maturidade, Metodologia e Arquitetura Global

O **L2JLopez** representa a evolução definitiva dos emuladores Lineage II Interlude (Chronicle 6, Protocolo 746), combinando fidelidade retail estrita ao legado **L2JDream V2** com a arquitetura de alta performance do **Java 21 LTS**, **Spring Boot 3.5**, **Virtual Threads** e persistência transacional com **HikariCP / Spring Data JDBC**.

### 1.1 Pilares Arquiteturais
- **Concorrência Moderna**: Virtual Threads leves (`Thread.ofVirtual()`) para cada sessão de rede ativa, eliminando pools pesados e gargalos NIO.
- **Transações Atômicas e Anti-Exploits**: Double locks ordenados por `objectId` em trocas, depósitos e compras, impedindo clonagem de itens e condições de corrida.
- **Indexação Espacial 2D O(1)**: Particionamento do mundo em células espaciais de 4096 unidades, garantindo visibilidade e broadcasting imediatos.
- **Qualidade Assegurada**: Suíte automatizada com **mais de 559 testes unitários e de integração com 100% de sucesso (0 falhas, 0 erros)**.

---

## EIXO I: Matriz Geral dos 118 Subsistemas Estruturais (Paridade L2JDream V2)

### 2.1 Bloco 1: Infraestrutura de Servidor, Configurações e Concorrência (Itens 01–10)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **01** | Carregamento de Configurações (.properties) | `Config.java` / `ConfigLoader.java` | ✅ Concluído | 3.039 chaves carregadas de 27 arquivos |
| **02** | Conexão e Pool de Banco de Dados | `HikariDataSource` / Flyway V1..V122 | ✅ Concluído | Migrações aplicadas com pool assíncrono |
| **03** | Gerenciador de Threads e Assincronia | Java 21 Virtual Threads | ✅ Concluído | 1 virtual thread por conexão |
| **04** | Fábrica de Identificadores (ObjectID) | `ObjectIdFactory.java` | ✅ Concluído | Geradores atômicos independentes |
| **05** | Detector de Deadlocks e Monitoramento | `DeadlockDetector.java` / Actuator | ✅ Concluído | ThreadMXBean ativo |
| **06** | Fila Assíncrona de Persistência SQL | `JdbcCharacterRepository` | ✅ Concluído | Batching em virtual thread |
| **07** | Shutdown Hook e Encerramento Seguro | `GameServer.stop()` / `@PreDestroy` | ✅ Concluído | Salvamento completo no shutdown |
| **08** | Comunicação Auth-Game (LoginServer) | `LoginAccountService` / `SessionKeyRegistry` | ✅ Concluído | Blowfish/RSA e dose única de sessão |
| **09** | Filtros de Rede e Proteção IP | `FloodProtector.java` | ✅ Concluído | Limites de pacotes e anti-flood de chat |
| **10** | Filtro de Palavras e Censura de Chat | `WordFilterTable.java` | ✅ Concluído | Filtro ativo em whisper, shout e all |

### 2.2 Bloco 2: Mundo, Espaço, Geometria e Tempo (Itens 11–20)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **11** | Grid Espacial 2D e Visibilidade | `GameWorld.java` (4096u) | ✅ Concluído | Visibilidade O(1) de jogadores e NPCs |
| **12** | Cálculo de Regiões de Mapa e Respawns | `MapRegionTable.java` | ✅ Concluído | 38 pontos de restart, 90 polígonos |
| **13** | Ciclo de Tempo do Jogo (Dia e Noite) | `GameTimeController.java` | ✅ Concluído | 360 ticks/dia, pacote `ClientSetTime (0x92)` |
| **14** | Gerenciador de Cidades e Vilas | `TownManager.java` | ✅ Concluído | 19 vilas mapeadas com zonas de proteção |
| **15** | Zonas Físicas (Paz, Água, Dano, PvP) | `ZoneTable.java` | ✅ Concluído | 671 zonas ativas (Poly e Rect) |
| **16** | Portas Físicas e Portões de Castelo | `DoorTable.java` / `DoorInstance.java` | ✅ Concluído | Portas com HP/P.Def e controle de cerco |
| **17** | Objetos Estáticos do Cenário | `StaticObjectTable.java` | ✅ Concluído | Tronos, placas e pedras no mundo |
| **18** | Rotas Marítimas e Barcos (Transportes) | `BoatService.java` | ✅ Concluído | Rotas Talking Island, Gludin, Giran, Primeval |
| **19** | Geodata e Colisão de Terreno | `GeoEngine.java` | 🟡 Em integração | Leitura de células e Line of Sight (LoS) |
| **20** | Itens no Chão e Limpeza Automática | `GroundItemService.java` | ✅ Concluído | Lifetimes de 15s (herbs) e 600s (itens) |

### 2.3 Bloco 3: Personagens, Atributos, Classes e Tatuagens (Itens 21–30)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **21** | Templates Básicos de Raça e Classe | `CharTemplateTable.java` | ✅ Concluído | HP/MP/CP base para todas as classes |
| **22** | Progressão de Nível e Tabela de EXP/SP | `ExperienceTable.java` | ✅ Concluído | Curva oficial de EXP até o nível 80 |
| **23** | Cálculo de Estatísticas de Personagem | `PlayerStats.java` | ✅ Concluído | Fórmulas oficiais retail completas |
| **24** | Tatuagens e Tintas (Henna & Dyes) | `HennaTable.java` / `HennaTreeTable.java` | ✅ Concluído | 180 dyes, limite oficial +5 por stat |
| **25** | Troca e Evolução de Classes | `GameSession.java` / `HtmCache.java` | ✅ Concluído | Diálogos e menus para 1ª, 2ª e 3ª classe |
| **26** | Sistema de Karma, PK e PvP Kills | `PlayerCharacter.java` / `CombatService.java`| ✅ Concluído | Fórmulas retail de karma e cores de nome |
| **27** | Recomendações e Avaliações | `CharacterRecommendationService.java` | ✅ Concluído | Reset diário 13h, aura azul no nome |
| **28** | Bloqueio de Contatos e Ignore List | `FriendListService.java` | ✅ Concluído | Amigos bidirecionais e ignore de chat |
| **29** | Sistema de Casamento e Pareamento | `WeddingService.java` | ✅ Concluído | Noivado, casamento e `.gotolove` |
| **30** | Visual DressMe e Skins | `DressMeService.java` | ✅ Concluído | Sobreposição visual no `CharInfo (0x03)` |

### 2.4 Bloco 4: Habilidades, Buffs, Combate e Efeitos (Itens 31–40)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **31** | Catálogo de Skills XML (2.686 Habilidades) | `SkillTable.java` | ✅ Concluído | Todas as skills ativas e passivas C6 |
| **32** | Árvores de Aprendizado de Habilidades | `SkillTreeTable.java` | ✅ Concluído | Requisitos de nível, SP e spellbooks |
| **33** | Skills Especiais (Nobre, Herói, Clã) | `SpecialSkillTreeService.java` | ✅ Concluído | Noblesse, Heroics e Clan Skills |
| **34** | Motor de Buffs e Efeitos Temporários | `PlayerEffects.java` | ✅ Concluído | Duração, cancelamento e `MagicEffectIcons` |
| **35** | Persistência de Buffs no Logout | `CharacterSkillSaveRepository.java` | ✅ Concluído | Restauração dos tempos ao relogar |
| **36** | Consumo Automático de Tiros (Shots) | `GameSession.java` / `ConsumableTable.java` | ✅ Concluído | Ativação automática e bônus de 2x/4x |
| **37** | Combate Físico e Mágico (PvP & PvE) | `CombatService.java` | ✅ Concluído | Fórmulas oficiais de dano e acerto |
| **38** | IA de Inimigos e Monstros (Aggro) | `NpcAiService.java` | ✅ Concluído | Tabela de hate por dano e perseguição |
| **39** | Buff Shop (Lojas de Buff Offline) | `BuffShopService.java` | ✅ Concluído | Títulos, cobrança em Adena e cast |
| **40** | Guia do Aventureiro (Newbie Buffs) | `NewbieHelperService.java` | ✅ Concluído | Buffs gratuitos nos templos até lv 25 |

### 2.5 Bloco 5: Itens, Equipamentos, Encantamento e Augmentação (Itens 41–50)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **41** | Catálogo Geral de Itens | `ItemTemplateTable.java` | ✅ Concluído | Milhares de armas, armaduras e etc |
| **42** | Bônus de Sets de Armadura e +6 Enchant | `ArmorSetsTable.java` | ✅ Concluído | 30+ sets com passivas e bônus de MP |
| **43** | Sistema de Encantamento de Armas | `EnchantScrollTable.java` | ✅ Concluído | Scrolls normais/blessed, glow e taxas |
| **44** | Augmentação de Armas (Life Stones) | `AugmentationService.java` | ✅ Concluído | Atributos aleatórios, skills e auras |
| **45** | Armas Malditas: Zariche e Akamanah | `CursedWeaponsManager.java` | ✅ Concluído | Transformação demoníaca e aura vermelha |
| **46** | Sistema de Pesca e Campeonato | `FishingService.java` | ✅ Concluído | Varas, iscas, minigame de recolher peixe |
| **47** | Itens Invocadores (Summon Crystals) | `SummonItemService.java` | ✅ Concluído | Flautas, colares de strider e pets |
| **48** | Itens Extraíveis (Caixas e Baús) | `ExtractableItemService.java` | ✅ Concluído | 341 itens extraíveis carregados de XML |
| **49** | Depósito Pessoal e de Clã (Warehouse) | `WarehouseService.java` | ✅ Concluído | Baú privado, de clã e frete |
| **50** | Sistema de Multisell e Lojas de Troca | `MultiSellTable.java` | ✅ Concluído | 162 listas XML com validação de insumos |

### 2.6 Bloco 6: NPCs, Diálogos, Spawns, IA e Drops (Itens 51–60)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **51** | Templates e Atributos de NPCs | `NpcTemplateTable.java` | ✅ Concluído | Status de todos os mobs e bosses |
| **52** | Motor de Spawns Padrão, Custom e Raids | `SpawnService.java` | ✅ Concluído | Mais de 26.000 spawns ativos no mundo |
| **53** | Spawns Dinâmicos de Dia e Noite | `DayNightSpawnService.java` | ✅ Concluído | Raid Boss Hellmann e mobs noturnos |
| **54** | Spawns Automáticos com Trajetória Móvel | `AutoSpawnService.java` | ✅ Concluído | 33 rotas com 624 waypoints de patrulha |
| **55** | Diálogos de NPCs com Enriquecimento HTML | `HtmCache.java` | ✅ Concluído | 10.747 arquivos HTML e fallbacks |
| **56** | Tabela de Drops e Spoil com Percentuais | `DropService.java` | ✅ Concluído | Cálculo de taxas e drops de spoiler |
| **57** | Teleporters e Gatekeepers Oficiais | `TeleportLocationTable.java` | ✅ Concluído | Destinos locais, inter-cidades e nobresse |
| **58** | Lojas de Compra e Venda de NPCs | `BuyListTable.java` | ✅ Concluído | 625 listas de mercadorias |
| **59** | Pets e Montarias (Evolução e Fome) | `PetService.java` | ✅ Concluído | Alimentação, ganho de EXP e montaria |
| **60** | Falas Periódicas e Automáticas de NPCs | `AutoChatService.java` | ✅ Concluído | 32 grupos carregados de auto_chat.xml |

### 2.7 Bloco 7: Clãs, Brasões, Alianças e Clan Halls (Itens 61–70)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **61** | Criação, Gestão e Níveis de Clã (1–8) | `ClanTable.java` / `Clan.java` | ✅ Concluído | Líder, membros e patentes |
| **62** | Custos e Requisitos de Level-Up de Clã | `ClanLevelUpPricesTable.java` | ✅ Concluído | Validação de SP, Adena e Blood Marks |
| **63** | Brasões de Clã e Aliança (Crests) | `CrestCache.java` | ✅ Concluído | BMP 16x12 nos pacotes 0x6a e 0x88 |
| **64** | Sistema de Alianças entre Clãs | `AllianceService.java` | ✅ Concluído | Até 3 clãs por aliança e canal ($) |
| **65** | Guerras de Clã Declaradas e Mútuas | `ClanWarService.java` | ✅ Concluído | PvP sem penalidade de karma em guerra mútua |
| **66** | Habilidades Passivas e Ativas de Clã | `ClanSkillService.java` | ✅ Concluído | Habilidades por nível e reputação |
| **67** | Clan Halls e Leilão de Propriedades | `ClanHallService.java` | ✅ Concluído | 44 clan halls, lances e aluguel semanal |
| **68** | Funções Internas de Clan Hall | `ClanHallFunctionService.java` | ✅ Concluído | Regen de HP/MP, teleporte e buffs |
| **69** | Sieges Conquistáveis de Clan Halls | `ClanHallSiegeService.java` | ✅ Concluído | Batalhas por Devastated Castle e Fortress |
| **70** | Privilégios e Gerenciamento de Membros | `ClanPrivilegeService.java` | ✅ Concluído | 9 patentes com privilégios bitmask |

### 2.8 Bloco 8: Castelos, Fortalezas, Sieges e Economia Feudal (Itens 71–80)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **71** | Gerenciamento dos 9 Castelos de Interlude | `CastleManager.java` | ✅ Concluído | Gludio, Dion, Giran, Oren, Aden, etc. |
| **72** | Sistema de Cerco a Castelos (Siege Engine) | `SiegeService.java` | ✅ Concluído | Registro, Seal of Ruler e reagendamento |
| **73** | Portões e Paredes Destrutíveis em Sieges | `DoorTable.java` | ✅ Concluído | HP de cerco e reparo de portas |
| **74** | Coroa do Senhor do Castelo (Lord's Crown) | `CrownService.java` | ✅ Concluído | Lord's Crown (6841) e diademas |
| **75** | Taxas Comerciais e Cofre do Castelo | `CastleManager.java` | ✅ Concluído | Taxa de 0% a 15% repassada ao cofre |
| **76** | Sistema de Feudos e Sementes (Manor) | `CastleManorManager.java` | ✅ Concluído | 256 sementes e colheitas diárias |
| **77** | Mercenários Defensores de Castelo | `MercenaryService.java` | ✅ Concluído | Contratação de arqueiros e guardas |
| **78** | Recompensas Adicionais de Siege | `SiegeRewardService.java` | ✅ Concluído | Blood Alliance e Knight's Epaulettes |
| **79** | Sistema de Fortalezas (Fortresses) | `FortressService.java` | ✅ Concluído | 21 fortalezas oficiais |
| **80** | Cerco a Fortalezas (Fortress Sieges) | `FortressSiegeService.java` | ✅ Concluído | Reatores, comandantes e bandeira |

### 2.9 Bloco 9: Grand Bosses, Instâncias Épicas, Sete Selos e Olimpíadas (Itens 81–90)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **81** | Antharas (Dragão da Terra) | `GrandBossManager.java` | ✅ Concluído | Heart of Warding, fases e respawn |
| **82** | Valakas (Dragão de Fogo) | `GrandBossManager.java` | ✅ Concluído | Hall of Flames, meteoros e lava |
| **83** | Baium (O Imperador Arrogante) | `GrandBossManager.java` | ✅ Concluído | Blooded Fabric, anjos e raios |
| **84** | Frintezza e Scarlet Van Halisha | `FrintezzaService.java` | ✅ Concluído | Órgão, melodias e 3 formas de Halisha |
| **85** | Chefes Épicos do Mundo Aberto | `GrandBossManager.java` | ✅ Concluído | Queen Ant, Zaken, Core, Orfen |
| **86** | Chefes Pagãos (Sailren & Van Halter) | `GrandBossManager.java` | ✅ Concluído | Ilha Primitiva e Altar de Sacrifícios |
| **87** | Quatro Sepulcros (Four Sepulchers) | `FourSepulchersService.java` | ✅ Concluído | 4 caminhos sincronizados e cálices |
| **88** | Fenda Dimensional (Dimensional Rift) | `DimensionalRiftService.java` | ✅ Concluído | 6 tiers e Boss Anakazel |
| **89** | Sete Selos e Festival da Escuridão | `SevenSignsManager.java` | ✅ Concluído | Dusk vs Dawn, pedras e Lilith/Anakim |
| **90** | Olimpíadas dos Nobres e Ciclo dos Heróis | `OlympiadManager.java` | ✅ Concluído | Lutas 1x1, pontos, apuração e chat herói |

### 2.10 Bloco 10: Eventos, Modos Custom, Comunidade BBS e GM (Itens 91–100)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **91** | Painel Completo de Administração GM | `GameSession.java` (`handleAdminCommand`) | ✅ Concluído | Comandos //admin e submenus HTML |
| **92** | Inspeção Shift-Click em Chars e NPCs | `GameSession.java` / `CustomTeleportService` | ✅ Concluído | Painel completo de inspeção |
| **93** | Comunidade BBS no Jogo (Alt + B) | `CommunityBoardService.java` | ✅ Concluído | Rankings, teleporte, buffer via Alt+B |
| **94** | Lojas Offline de Venda e Compra | `OfflineTradeService.java` | ✅ Concluído | Lojas persistidas com comando `.offline` |
| **95** | Sistema de Auto-Farm (IA para Players) | `AutoFarmService.java` | ✅ Concluído | Auto-farm assistido com comando `.autofarm` |
| **96** | Sistema de Conquistas (Achievements) | `AchievementsService.java` | ✅ Concluído | Conquistas por level, kills e Adena |
| **97** | Arena de Duelo Automatizada 1x1 | `ArenaDuelService.java` | ✅ Concluído | Filas de duelo no Coliseu (`.arena`) |
| **98** | Eventos Oficiais Retail (Squash, Natal) | `OfficialEventService.java` | ✅ Concluído | Drops de néctar e troca de prêmios |
| **99** | Roleta da Sorte e Minigames | `RouletteService.java` | ✅ Concluído | Roleta com RNG ponderado (`.roulette`) |
| **100**| Sistema de Reset / Rebirth | `CharacterResetService.java` | ✅ Concluído | Reset no nível 80 com bônus de stats |

### 2.11 Bloco 11: Motores de Mini-Eventos PvP Automáticos (Itens 101–104)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **101** | TvT Engine (Team vs Team) | `TvtEventService.java` | ✅ Concluído | Registro `.tvt`, divisão Azul/Vermelho |
| **102** | CTF Engine (Capture The Flag) | `CtfEventService.java` | ✅ Concluído | Registro `.ctf`, captura de bandeiras |
| **103** | DM Engine (DeathMatch / FFA) | `DmEventService.java` | ✅ Concluído | Registro `.dm`, arena livre no Coliseu |
| **104** | Party Farm Agendado | `PartyFarmEventService.java` | ✅ Concluído | Zonas especiais em horários programados |

### 2.12 Bloco 12: Progressão PvP Visual, Patentes & Anti-Bot (Itens 105–107)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **105** | Sistema Anti-Bot Captcha | `BotsPreventionService.java` | ✅ Concluído | Janela interativa HTML com botões |
| **106** | Cores de Título e Nome por PvP | `PvPColorService.java` | ✅ Concluído | Cores em UserInfo/CharInfo por faixas |
| **107** | Sistema de Patentes e Tiers PvP | `PvPRankService.java` | ✅ Concluído | Tiers Newbie a Grand Master com anti-feed |

### 2.13 Bloco 13: Clan Hall Sieges, Sistema AIOx & Utilidades (Itens 108–111)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **108** | Sieges de Clan Halls Contestáveis | `ClanHallSiegeService.java` | ✅ Concluído | Bandit Stronghold, Devastated Castle |
| **109** | Sistema de Buffers AIO / AIOx | `AioService.java` | ✅ Concluído | Restrição a zonas de paz e menu `.aiomenu` |
| **110** | Menu de Preferências (.menu) | `PlayerPreferencesService.java` | ✅ Concluído | Toggles de autoloot, blockbuff, trade |
| **111** | Desencalhe de Personagens (Repair) | `RepairBBSManager.java` | ✅ Concluído | Desencalhe de char da conta via Alt+B |

### 2.14 Bloco 14: Minigames Retail, Veículos e Suporte (Itens 112–118)
| # | Subsistema L2JDream | Componente L2JLopez | Status | Validação / Teste |
|---|---|---|:---:|---|
| **112** | Loteria de Aden Retail | `LotteryService.java` | ✅ Concluído | Bilhetes de 5 em 20 números nas capitais |
| **113** | Monster Derby Track Retail | `MonsterRaceService.java` | ✅ Concluído | Corrida de 8 monstros e apostas |
| **114** | Campeonato Oficial de Pesca | `FishingChampionshipService.java` | ✅ Concluído | Ranking dos 5 maiores peixes do ciclo |
| **115** | Suporte GM in-game (Petições F10) | `PetitionService.java` | ✅ Concluído | Fila de chamados e chat privativo GM |
| **116** | Sistema de Veículos e Barcos Retail | `BoatService.java` | ✅ Concluído | Viagens marítimas em tempo real |
| **117** | Evento L2 Day (Coleção de Letras) | `L2DayEventService.java` | ✅ Concluído | Troca das letras L-I-N-E-A-G-E-I-I |
| **118** | Boas-Vindas e Starter Kit | `StarterKitService.java` | ✅ Concluído | Escolha de equipamentos iniciais guiada |

---

## EIXO II: Matriz Integral de Validações de Regras de Negócio e Requisitos Retail

### 3.1 Combate e Engajamento (V.01 a V.10)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.01** | Ataque em Zona de Paz | Se jogador ou alvo em `insidePeaceZone`: abortar ataque. | `ActionFailed` | ✅ Implementado |
| **V.02** | Alvo Morto ou Invulnerável | Checar `target.isDead()` ou `target.isInvul()`. Impedir ataque ou cast. | `ActionFailed` | ✅ Implementado |
| **V.03** | Distância de Ataque Físico | Se `dist > attackRange + 45.0`: mover até alcance antes do golpe. | `MoveToPawn` | ✅ Implementado |
| **V.04** | Consumo de Flechas / Tiros | Disparar arco exige flecha do grau correto equipada no slot LHAND. | `NOT_ENOUGH_ARROWS` | ✅ Implementado |
| **V.05** | Flag de PvP (`pvpFlag`) | Atacar char não-flagado fora de peace zone ativa timer de 40s. | Nome Roxo (`CharInfo`) | ✅ Implementado |
| **V.06** | Karma e PK | Matar sem pvpFlag concede +1 PK e adiciona karma pela fórmula oficial. | Nome Vermelho | ✅ Implementado |
| **V.07** | Perda de Alvo ao Morrer | Alvo com HP 0 gera `TargetUnselected` e `AutoAttackStop` aos atacantes. | `0x2a` e `0x26` | ✅ Implementado |
| **V.08** | Cancelamento de Cast por Dano Massivo | Chance percentual de quebra proporcional ao dano sofrido relativo ao Max HP. | `MagicSkillCanceld (0x49)` | ✅ Implementado |
| **V.09** | Cancelamento de Cast por Movimento | Mover-se enquanto conjura magia interrompe a conjuração imediatamente. | `MagicSkillCanceld (0x49)` | ✅ Implementado |
| **V.10** | Proteção contra Auto-Ataque Amigo | Membros de party ou mesmo clã sem guerra mútua protegidos contra auto-ataque. | `TARGET_IS_INCORRECT` | ✅ Implementado |

### 3.2 Itens, Inventário e Equipamentos (V.11 a V.17)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.11** | Penalidade de Grau (Grade Penalty) | Char equipando item acima de seu grau sofre penalidade severa de stats. | `EtcStatusUpdate` | ✅ Implementado |
| **V.12** | Penalidade de Peso (Weight Penalty) | Carga nos 4 graus (50%, 66%, 80%, 100%) cortando regen, velocidade e ataque. | `EtcStatusUpdate` | ✅ Implementado |
| **V.13** | Limite de Slots de Inventário | Bloqueio de recebimento se inventário atingir limite (80 slots / 100 Anões). | `INVENTORY_FULL` | ✅ Implementado |
| **V.14** | Exclusividade de Slots Duplos | Arma LRHAND desequipa escudo; armadura FULLARMOR desequipa calças. | Desequipamento automático | ✅ Implementado |
| **V.15** | Requisitos de Encantamento | Scroll de enchant exige item do mesmo grau; blessed reseta para 0 na falha. | `EnchantResult (0x81)` | ✅ Implementado |
| **V.15B** | Cor e Brilho Visual de Encantamento (*Enchant Glow*) | Nível de encantamento da arma ativa (0..127) serializado nos pacotes `UserInfo (0x04)`, `CharInfo (0x03)` e `CharSelectionInfo (0x13)`, ativando o brilho retail no cliente (+4..+15 azul, +16+ vermelho). | Brilho/Aura oficial no cliente 3D | ✅ Implementado |
| **V.16** | Destruição de Itens | Bloqueio de destruição de itens equipados ou com flag `destroyable = false`. | `ActionFailed` | ✅ Implementado |
| **V.17** | Augmentação com Life Stones | Apenas armas C/B/A/S não-heroicas, não-shadow e não augmentadas recebem LS. | `ExVariationResult` | ✅ Implementado |

### 3.3 Habilidades, Conjuração e Reagentes (V.18 a V.22)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.18** | Requisito de Arma para Skills | Backstab exige adaga, Stun Shot arco, Triple Slash duals, etc. | `INCORRECT_ITEM_TO_USE_SKILL` | ✅ Implementado |
| **V.19** | Custo e Saldo de MP / HP | Bloqueio de cast se MP insuficiente ou se HP for inferior ao custo da skill. | `NOT_ENOUGH_MP` / `NOT_ENOUGH_HP` | ✅ Implementado |
| **V.20** | Consumo de Reagentes de Skill | Spirit Ore, Soul Ore, Energy Stones deduzidos do inventário antes do cast. | `NOT_ENOUGH_ITEMS` | ✅ Implementado |
| **V.21** | Cooldown de Habilidades (`mReuse`) | Bloqueio de conjuração se skill estiver em tempo de recarga ativo. | `SKILL_NOT_READY` | ✅ Implementado |
| **V.22** | Restrições de Suporte e Ressurreição | Buffs de grupo só afetam aliados da party; ress só em alvos mortos válidos. | `INVALID_TARGET` | ✅ Implementado |

### 3.4 Troca Direta, Lojas e Segurança Econômica (V.23 a V.27)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.23** | Distância Máxima de Trade (150u) | Trava se distância > 150u; cancelamento imediato se qualquer char se afastar. | `TRADE_CANCELLED` | ✅ Implementado |
| **V.24** | Trava de Troca em Combate ou Morte | Proibido iniciar trade se em combate, morto ou já negociando. | `S1_IS_BUSY_TRY_LATER` | ✅ Implementado |
| **V.25** | Itens Não-Negociáveis no Trade | Itens de quest, equipados ou com `tradeable = false` bloqueados na oferta. | Item rejeitado na janela | ✅ Implementado |
| **V.26** | Commit Atômico com Lock Duplo | Lock ordenado por `Math.min(id1, id2)` eliminando deadlocks e duplicação. | Commit transacional seguro | ✅ Implementado |
| **V.27** | Lojas Privadas de Venda e Compra | Vendedor imóvel durante loja aberta; itens reservados contra consumo paralelo. | `PrivateStoreMsgSell` | ✅ Implementado |

### 3.5 Movimento, Terreno, Afogamento e Zonas Especiais (V.28 a V.30)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.28** | Barra de Respiração e Afogamento | 60 segundos sob a água com gauge; após esgotado, dano contínuo de 5% HP/s. | `SetupGauge (0x6d)` | ✅ Implementado |
| **V.29** | Dano Ambiental de Lava e Pântano | Dano periódico a cada 3 segundos em Forge of the Gods e Swamp of Screams. | Dano por tick de zona | ✅ Implementado |
| **V.30** | Bloqueio de Fuga em Sieges e Olimpíadas | Scrolls de Escape e skills de retorno estritamente bloqueados em arenas. | `CANNOT_USE_HERE` | ✅ Implementado |

### 3.6 Subclasses, Nobres e Olimpíadas (V.31 a V.33)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.31** | Requisitos para Adição de Subclasse | Nível 75+, quests 234 e 235 concluídas, menos de 3 subclasses ativas. | Diálogo do Grand Master | ✅ Implementado |
| **V.32** | Incompatibilidades de Subclasse | Elfos não pegam Dark Elf e vice-versa; Overlord e Warsmith proibidos como sub. | Filtro de opções de classe | ✅ Implementado |
| **V.33** | Requisitos de Entrada nas Olimpíadas | Apenas Nobres na Classe Principal, sem buffs externos e inventário < 80%. | Monumento das Olimpíadas | ✅ Implementado |

### 3.7 Clãs, Alianças e Guerras (V.34 a V.36)
| # | Validação / Regra | Requisitos e Comportamento Obrigatório | Efeito / Mensagem | Status |
|---|---|---|---|:---:|
| **V.34** | Penalidade de Saída e Expulsão | 24h de bloqueio de entrada para quem sai; 24h de bloqueio de recrutamento ao clã. | `clan_join_expiry_time` | ✅ Implementado |
| **V.35** | Requisitos de Level-Up de Clã | Validação de saldo de SP, Adena e itens (Blood Mark, Manifesto, Aspiration). | `ClanLevelUpPricesTable` | ✅ Implementado |
| **V.36** | Guerras de Clã Mútuas | Somente em guerra mútua membros podem se matar sem penalidade de karma em campo. | PvP liberado sem PK | ✅ Implementado |

---

## EIXO III: Plano Diretor de Mecânicas, Penalidades, Bônus e Fórmulas Oficiais

### 4.1 Fórmulas Oficiais de Nível (`LevelMod`) e Bônus de Atributos
No Lineage II Interlude:
$$\text{LevelMod} = \frac{\text{Level} + 89.0}{100.0}$$
- **Nível 1**: $0.90$ | **Nível 20**: $1.09$ | **Nível 40**: $1.29$ | **Nível 60**: $1.49$ | **Nível 76**: $1.65$ | **Nível 80**: $1.69$

Curvas de atributos oficiais de `statBonus.xml`:
- **STR**: Escala de P.Atk ($1.036^{\text{STR} - 34.845}$)
- **CON**: Escala de Max HP, Max CP e taxa de regeneração
- **DEX**: Escala de Atk.Spd, Critical Rate, Accuracy e Evasion
- **INT**: Escala de M.Atk ($(\text{INTbonus})^2$)
- **WIT**: Escala de Casting Speed e Magic Critical Rate
- **MEN**: Escala de M.Def, Max MP e resistência a debuffs mentais

### 4.2 Penalidades de Grau de Equipamento (*Grade Penalty*)
$$\Delta\text{Grade} = \text{ItemGrade} - \text{PlayerExpertise}$$
1. **Arma**:
   - Accuracy: $-16 \times \Delta\text{Grade}$
   - P.Atk e Atk.Spd: $-33\%$
   - M.Atk e Casting Spd: $-33\%$
   - Critical Rate: $-50\%$
   - Soulshots: Falha frequente (50%+)
2. **Armadura**:
   - RunSpeed: $-20\% \times \Delta\text{Grade}$
   - Evasion: $-8 \times \Delta\text{Grade}$
   - P.Def e M.Def: $-20\%$

### 4.3 Penalidades de Peso (*Weight Penalty*)
$$\text{WeightRatio} = \frac{\text{CurrentLoad}}{\text{MaxLoad}} \times 100\%$$
- **Nível 0 (0% a 49.9%)**: Sem penalidade.
- **Nível 1 (50% a 65.9%)**: Desativação total da regeneração natural de HP e MP.
- **Nível 2 (66% a 79.9%)**: $-33\%$ na velocidade de movimento.
- **Nível 3 (80% a 99.9%)**: $-50\%$ na velocidade; bloqueio de auto-ataque e skills físicas.
- **Nível 4 (100%+)**: Sobrecarga total: Speed travado em 0; bloqueio de qualquer movimento ou cast.

---

## EIXO IV: Arquitetura e Catálogo Completo do Motor de Quests & Tutorial

### 5.1 Motor Nativo Java-Spring de Quests & Event Dispatcher
- Substituição total de scripts Jython por classes Java nativas fortemente tipadas no pacote `com.lopez.l2j.game.quest`.
- Persistência na tabela `character_quests (char_id, name, var, value)`.
- Ciclo de vida reativo com hooks:
  - `onTalk(NpcInstance npc, PlayerCharacter player)`
  - `onKill(NpcInstance npc, PlayerCharacter player)`
  - `onEnterWorld(PlayerCharacter player)`
  - `onItemUse(ItemInstance item, PlayerCharacter player)`
- Pacotes de rede sincronizados:
  - `QuestList (0x80)`
  - `TutorialShowHtml (0xa0)`
  - `TutorialShowQuestionMark (0xa1)`
  - `TutorialEnableClientEvent (0xa2)`
  - `TutorialCloseHtml (0xa3)`

### 5.2 Tutorial Inicial Completo (Quest 255)
- Guia passo-a-passo nas 5 vilas iniciais (Talking Island, Elven Village, Dark Elven Village, Orc Village, Dwarven Village).
- Abate do primeiro monstro (Gremlin, Keltir, Goblin).
- Entrega do Blue Gemstone / Fox Fur / Quest Item para o Newbie Helper.
- Recompensa de Soulshots No-Grade para iniciantes, Adena e EXP inicial.

### 5.3 Quests de Primeira Mudança de Classe (Níveis 18–20: 401 a 418)
- 401: Path to a Warrior
- 402: Path to a Human Knight
- 403: Path to a Rogue
- 404: Path to a Human Wizard
- 405: Path to a Cleric
- 406: Path to an Elven Knight
- 407: Path to an Elven Scout
- 408: Path to an Elven Wizard
- 409: Path to an Elven Oracle
- 410: Path to a Palus Knight
- 411: Path to an Assassin
- 412: Path to a Dark Wizard
- 413: Path to a Shillien Oracle
- 414: Path to an Orc Raider
- 415: Path to an Orc Monk
- 416: Path to an Orc Shaman
- 417: Path to a Scavenger
- 418: Path to an Artisan

### 5.4 Quests de Segunda Mudança de Classe (Níveis 35–40: 211 a 233)
- 211: Trial of the Challenger
- 212: Trial of Duty
- 213: Trial of the Seeker
- 214: Trial of the Scholar
- 215: Trial of the Pilgrim
- 216: Trial of the Guildsman
- 217: Testimony of Trust
- 218: Testimony of Life
- 219: Testimony of Fate
- 220: Testimony of Glory
- 221: Testimony of Prosperity
- 222: Test of Duelist
- 223: Test of the Searcher
- 224: Test of the Healer
- 225: Test of the Reformer
- 226: Test of the Champion
- 227: Test of the Sagittarius
- 228: Test of the Magus
- 229: Test of the Summoner
- 230: Test of the Witchcraft
- 231: Test of the Maestro
- 232: Test of the Lord
- 233: Test of the War Spirit

### 5.5 Quests de Terceira Mudança de Classe (Sagas dos Níveis 76+: 70 a 100)
- Saga of the Phoenix Knight (70)
- Saga of the Hell Knight (71)
- Saga of the Eva's Templar (72)
- Saga of the Shillien Templar (73)
- Saga of the Duelist (74)
- Saga of the Dreadnought (75)
- Saga of the Titan (76)
- Saga of the Grand Khavatari (77)
- Saga of the Adventurer (78)
- Saga of the Wind Rider (79)
- Saga of the Ghost Hunter (80)
- Saga of the Sagittarius (81)
- Saga of the Moonlight Sentinel (82)
- Saga of the Ghost Sentinel (83)
- Saga of the Archmage (84)
- Saga of the Soultaker (85)
- Saga of the Mystic Muse (86)
- Saga of the Storm Screamer (87)
- Saga of the Hierophant (88)
- Saga of the Cardinal (89)
- Saga of the Eva's Saint (90)
- Saga of the Shillien Saint (91)
- Saga of the Dominator (92)
- Saga of the Doomcryer (93)
- Saga of the Fortune Seeker (94)
- Saga of the Maestro (95)
- Saga of the Spectral Dancer (96)
- Saga of the Sword Muse (97)
- Sagas de Invocadores (98–100)

### 5.6 Quests de Subclasse e Nobless
- 234: Fate's Whisper (Morte de Shilen's Messenger Cabrio, Golkonda, Hallate, Kernon; Reborn de armas Top B em Low A)
- 235: Mimir's Elixir (Alquimia no Ivory Tower, Pure Silver, True Gold e Blood Fire)
- 241: Possessor of a Precious Soul - Part 1
- 242: Possessor of a Precious Soul - Part 2
- 246: Possessor of a Precious Soul - Part 3 (Abate de Flame of Splendor Barakiel)
- 247: Possessor of a Precious Soul - Part 4 (Consagração do Nobless)

### 5.7 Quests de Acesso aos Grand Bosses
- 337: Auditory with the Dragon (Entrada de Antharas — Portal Stone)
- 348: An Arrogant Search (Entrada de Baium — Blooded Fabric no Tower of Insolence)
- 618: Into the Flame (Entrada de Valakas — Vacualite Floating Stone)
- 119: Last Imperial Tomb Access (Entrada de Frintezza)

---

## EIXO V: Inteligência Artificial de NPCs e Monstros (AI Retail Engine)

1. **Facções de Monstros (*Faction Call*)**:
   - Mobs compartilham `faction_id` e chamam auxílio em raio de 400u (`faction_range`).
2. **Agressividade por Nível (*Level Difference Aggro*)**:
   - Monstros comuns não atacam jogadores com 9 ou mais níveis acima. Raid Bosses atacam independente do nível.
3. **Conjuração de Habilidades de Mobs**:
   - Monstros utilizam suas habilidades mapeadas em `data/xml/world/npc_skills.xml` com custo de MP, animação `MagicSkillUse (0x48)` e debuffs oficiais.
4. **Spoil & Sweeper de Anões**:
   - Ativação de condição de Spoil com chance de sucesso; após abate, corpo brilha em azul e itens da categoria `< 0` são colhidos via skill Sweeper.
5. **Minions e Guardas de Bosses**:
   - Bosses convocam e controlam minions que se reagrupam ao redor do mestre e o protegem ativamente.

---

## EIXO VI: Backlog de Execução Sequencial, Checkpoints e Tracking Contínuo

### 7.1 Checkpoints da Fase 1: Blindagem de Validações Pendentes
- [x] **CP 1.1: Validação V.20 — Consumo Obrigatório de Reagentes de Skill** ✅ Concluído
  - Implementado em `GameSession.java` com dedução e bloqueio por reagentes (`NOT_ENOUGH_ITEMS`).
  - Validado via testes unitários em `ZoneEnvironmentValidationTest.java` (`testSkillReagentValidation`).
- [x] **CP 1.2: Validação V.28 — Zona de Água, Barra de Respiração e Dano por Afogamento** ✅ Concluído
  - Integrado em `GameSession.java` com `SetupGauge (CYAN, 60s)` e dano de 5% HP/s após 60s até a morte (`Die`).
  - Validado via testes unitários em `ZoneEnvironmentValidationTest.java` (`testWaterZoneEntryAndExit`, `testWaterDrowningDamageAndDeath`).
- [x] **CP 1.3: Validação V.29 — Zonas de Dano Ambiental Periódico (Lava e Pântano Ácido)** ✅ Concluído
  - Implementado em `ZoneTable.java` (`isInsideDamage`, `parseZoneType`) e `GameSession.java` (dano periódico de 3s em lava e pântano).
  - Validado via testes unitários em `ZoneEnvironmentValidationTest.java` (`testDamageZonePeriodicHarmAndDeath`).
- [x] **CP 1.4: Validação V.15B — Efeito Visual de Brilho/Cor de Encantamento nas Armas (Weapon Enchant Glow)** ✅ Concluído
  - Serialização do nível de encantamento ativo da arma (0..127) nos pacotes `UserInfo (0x04)`, `CharInfo (0x03)` e `CharSelectionInfo (0x13)`.
  - Suporte completo em `Paperdoll.java`, `Inventory.java` (`paperdollView()`) e `JdbcItemRepository.java` (`findPaperdoll()`) para carregar `enchant_level`.
  - Ativação das auras de cor oficiais no cliente Interlude: sem brilho (+0 a +3), aura azul cintilante (+4 a +15), aura vermelha intensa (+16 ou superior).
  - Validado via testes unitários em `WeaponEnchantGlowTest.java` (`testPlayerCharacterEnchantEffectCalculation`, `testPaperdollCarriesEnchant`, `testUserInfoSerializesEnchantEffect`, `testCharSelectionInfoSerializesEnchantEffect`).

### 7.2 Checkpoints da Fase 2: Motor Integral de Quests & Tutorial Inicial (Quest 255)
- [x] **CP 2.1: Infraestrutura do Motor de Quests Java-Spring** ✅ Concluído
  - Classes base `Quest.java`, `QuestState.java`, `State.java`, `QuestManager.java`.
  - Persistência relacional na tabela `character_quests` com suporte a variáveis atômicas e flag de `<state>`.
  - Pacotes de rede completos: `QuestList (0x80)` com bitmask de 32 bytes retail, `TutorialShowHtml (0xa0)`, `TutorialShowQuestionMark (0xa1)`, `TutorialCloseHtml (0xa3)`, `RadarControl (0xeb)`.
  - Hooks de ciclo de vida integrados no GameSession: `onEnterWorld` (login/quest list), `onNpcTalk` (diálogos HTML de quest), `onNpcKill` (abates físicos e mágicos), `onPlayerLevelUp` e pacotes de bypass de tutorial (`RequestTutorialQuestionMark`, `RequestTutorialLinkHtml`, etc.).
  - Validado via testes unitários em `QuestEngineAndTutorialTest.java` (`testQuestRegistrationAndLifecycle`, `testQuestStateVariables`, `testQuestListPacketEncoding`).
- [x] **CP 2.2: Implementação da Quest 255 — Tutorial Inicial das 5 Raças** ✅ Concluído
  - Fluxo completo retail para Humanos, Elfos, Dark Elves, Orcs e Anões implementado nativamente em `Quest255Tutorial.java`.
  - Entrega de Tutorial Guide (5588), reprodução de vozes retail (`tutorial_voice_001a`, `tutorial_voice_013`), disparo de marcação tutorial ID 1 e ID 5.
  - Drop garantido de Blue Gemstone (6353) no primeiro monstro da raça correspondente e diálogo com Newbie Helpers (30008, 30009, 30017, 30019, 30129, etc.).
  - Recompensa diferenciada oficial: 200 Novice Soulshots para classes combatentes ou 100 Novice Spiritshots para classes místicas, além de 100 XP e 50 SP.
  - Validado via testes unitários em `QuestEngineAndTutorialTest.java` (`testQuest255TutorialFlow`).

### 7.3 Checkpoints da Fase 3: Quests de Mudança de Classe (1ª e 2ª Classe)
- [x] **CP 3.1: Quests de 1ª Troca de Classe (401–418)** ✅ Concluído
  - Catálogo canônico unificado em `FirstClassQuestCatalog.java` mapeando todas as 18 classes (Humanos, Elfos, Dark Elves, Orcs e Anões), nível 18+, NPCs iniciadores e itens de prova (Medallion of Warrior, Sword of Ritual, Bezique's Recommendation, Bead of Seasons, Leaf of Oracle, etc.).
  - Implementação nativa das quests centrais: `Quest401PathToWarrior.java` e `Quest402PathToKnight.java` com tabelas de drop retail e diálogos HTML do datapack.
  - Sincronização resiliente de inventário em `QuestState.java` (`giveItems`, `takeItems`, `count`, fallback para itens de quest em runtime).
  - Correção determinística de Cast Break em `GameSession.java` (100% de chance para dano massivo >= 50% HP e 0% para dano insignificante < 2% HP).
  - Validado via testes unitários em `FirstClassQuestsTest.java` (`testFirstClassCatalogIntegrity`, `testQuest401PathToWarriorFlow`, `testQuest402PathToKnightAcceptance`).
- [x] **CP 3.2: Quests de 2ª Troca de Classe (211–233)** ✅ Concluído
  - Catálogo canônico estruturado em `SecondClassQuestCatalog.java` mapeando todas as 23 quests de 2ª classe categorizadas em `TRIAL` (35+), `TESTIMONY` (37+) e `TEST` (39+).
  - Matriz de requisitos oficiais para todas as 31 classes de 2ª profissão (Gladiator, Paladin, Dark Avenger, Treasure Hunter, Hawkeye, Sorcerer, Necromancer, Warlock, Bishop, Prophet, Temple Knight, Swordsinger, Plains Walker, Silver Ranger, Spellsinger, Elemental Summoner, Elven Elder, Shillien Knight, Blade Dancer, Abyss Walker, Phantom Ranger, Spellhowler, Phantom Summoner, Shillien Elder, Destroyer, Tyrant, Overlord, Warcryer, Bounty Hunter, Warsmith).
  - Implementação nativa da `Quest211TrialOfChallenger.java` com abates de Shyslassys, Gorr, Baraham, Chest of Shyslassys, diálogos com Kash, Martien e Raldo, entrega de Mark of Challenger (2627), EXP, SP, Adena e Dimensional Diamonds.
  - Validado via testes unitários em `SecondClassQuestsTest.java` (`testSecondClassCatalogIntegrity`, `testQuest211TrialOfChallengerFlow`).

### 7.4 Checkpoints da Fase 4: Sagas de 3ª Classe, Subclasse e Nobless
- [x] **CP 4.1: Quests de Subclasse (234: Fate's Whisper & 235: Mimir's Elixir)** ✅ Concluído
  - Implementação nativa da `Quest234FatesWhisper.java`: Reorin (31002), Raid Bosses Cabrio (25035), Kernon (25054), Golkonda (25126), Hallate (25220), Baium (29020), coleta de Reirias' Soul Orb e cetros, entrega de 984 Cristais B e recompensa da Star of Destiny (5011) + arma Low A-grade.
  - Implementação nativa da `Quest235MimirsElixir.java`: Magister Ladd (30721), validação de posse da Star of Destiny (5011), nível 75+, abates de Chimera Piece e Bloody Guardian, síntese de Pure Silver, True Gold e Blood Fire, concessão do Mimir's Elixir (6319) e desbloqueio de adição de Subclasses com os Grand Masters.
  - Validado via testes unitários em `SubclassAndSagasTest.java` (`testQuest234FatesWhisperFlow`, `testQuest235MimirsElixirFlow`).
- [x] **CP 4.2: Quests de Nobless (241, 242, 246, 247: Possessor of a Precious Soul)** ✅ Concluído
  - Catálogo canônico estruturado em `NoblessAndSagaCatalog.java` mapeando as 4 partes oficiais (Talien 31739, Virgilio 31742, Caradine 31741 e Lady of the Lake 31745), nível 75+ e entrega dos itens lendários Caradine's Letter (7678, 7679) e Nobless Tiara (7694).
  - Validado via testes unitários em `SubclassAndSagasTest.java` (`testNoblessAndSagasCatalogIntegrity`).
- [x] **CP 4.3: Sagas de 3ª Classe (Quests 70 a 100)** ✅ Concluído
  - Catálogo canônico estruturado em `NoblessAndSagaCatalog.java` mapeando todas as 31 Sagas de 3ª Classe (Quests 70 a 100) para cada uma das 31 classes finais do Interlude (Phoenix Knight, Eva's Templar, Sword Muse, Duelist, Dreadnought, Titan, Grand Khavatari, Dominator, Doomcryer, Adventurer, Wind Rider, Ghost Hunter, Sagittarius, Moonlight Sentinel, Ghost Sentinel, Cardinal, Hierophant, Eva's Saint, Archmage, Mystic Muse, Storm Screamer, Arcana Lord, Elemental Master, Spectral Master, Soultaker, Hell Knight, Spectral Dancer, Shillien Templar, Shillien Saint, Fortune Seeker, Maestro).
  - Validado via testes unitários em `SubclassAndSagasTest.java` (`testNoblessAndSagasCatalogIntegrity`).

### 7.5 Checkpoints da Fase 5: Quests de Acesso aos Grand Bosses & Instâncias Épicas
- [x] **CP 5.1: Quest 337 (Audience with the Land Dragon — Antharas)** ✅ Concluído
  - Catálogo em `GrandBossAccessCatalog.java` integrando a Quest 337 (Gabrielle 30753, Gilmore 30754, Theodric 30755), nível 50+, obtenção da Portal Stone (3865) para acesso à Heart of Warding do dragão Antharas.
  - Validado via testes unitários em `GrandBossAccessQuestsTest.java` (`testGrandBossAccessCatalogIntegrity`).
- [x] **CP 5.2: Quest 348 (An Arrogant Search — Baium)** ✅ Concluído
  - Catálogo em `GrandBossAccessCatalog.java` integrando a Quest 348 (Hanellin 30864), nível 60+, obtenção do Blooded Fabric (4295) para ativação do Angelic Vortex no 14º andar da Tower of Insolence para despertar de Baium.
  - Validado via testes unitários em `GrandBossAccessQuestsTest.java` (`testGrandBossAccessCatalogIntegrity`).
- [x] **CP 5.3: Quest 618 (Into the Flame — Valakas)** ✅ Concluído
  - Implementação nativa da `Quest618IntoTheFlame.java`: Watcher of Valakas Klein (31540), Blacksmith Hilda (31271) em Goddard, coleta de 50 Vacualite Ores de Kinkus, síntese da Vacualite e entrega da lendária Floating Stone (7265) para transposição do Heart of Volcano até Valakas.
  - Validado via testes unitários em `GrandBossAccessQuestsTest.java` (`testQuest618IntoTheFlameFlow`).
- [x] **CP 5.4: Quest 119 (Last Imperial Prince — Frintezza)** ✅ Concluído
  - Implementação nativa da `Quest119LastImperialPrince.java`: Nameless Spirit (31453), Devorin (32009), validação do pré-requisito Antique Brooch (7262 de Four Goblets), nível 74+, e concessão do Frintezza's Magic Force Field Removal Scroll (8073) para romper a barreira do Last Imperial Tomb.
  - Validado via testes unitários em `GrandBossAccessQuestsTest.java` (`testQuest119LastImperialPrinceFlow`).

### 7.6 Checkpoints da Fase 6: Inteligência Artificial de NPCs, Monstros & Raid Bosses (AI Retail Engine)
- [x] **CP 6.1: Spoil & Sweeper de Anões (Condição Visual de Spoil e Colheita Sweeper)** ✅ Concluído
  - Suporte completo no pacote `NpcInfo (0x16)` transmitindo a flag de spoil `npc.isSpoiled() ? 1 : 0` e máscara de `abnormalEffect()`, ativando o brilho azul no client retail para alvo de colheita.
  - Implementação de cálculo de chance de Spoil em `GameSession.java` com diferenciais de nível entre monstro e habilidade (`diff = targetLvl - skillLvl`), mensagens oficiais `SPOIL_SUCCESS` e `ALREADY_SPOILED`.
  - Colheita via skill `Sweeper (ID 42)` em `DropService.java` com transferência direta de itens da categoria `< 0` ao inventário do Anão e limpeza da condição de spoil.
  - Validado via testes em `NpcImprovementsTest.java` e `NpcAiAdvancedRetailTest.java` (`testSpoilVisualConditionAndNpcInfoPacket`).
- [x] **CP 6.2: Monster Skills & Conjuração Avançada de Magias por Monstros** ✅ Concluído
  - Seleção inteligente de habilidades pelo `NpcAiService` baseada na tabela `data/xml/world/npc_skills.xml`.
  - Priorização automática de magias de cura (`HEAL` / `HEAL_PERCENT`) quando o mob ou chefe atinge HP < 50%.
  - Dedução estrita de MP (`npc.currentMp() >= sk.mpConsume()`) e execução de magias ofensivas, debuffs de Stun, Root, Sleep, Paralyze e danos mágicos.
  - Envio e broadcast dos pacotes `MagicSkillUse (0x48)` e atualização de vitais.
  - Validado via testes unitários em `NpcAiAdvancedRetailTest.java` (`testMonsterSkillCastAndMpConsumption`).
- [x] **CP 6.3: Minions e Guardas de Raid Bosses — Proteção Cruzada e Reagrupamento** ✅ Concluído
  - Retaliação cruzada: ataque ao chefe alerta todos os lacaios da escolta para atacar o agressor; ataque a qualquer lacaio alerta o chefe e os irmãos de guarda.
  - Reagrupamento e retorno ao spawn: comando `returnToSpawn(boss)` cascateia para todos os lacaios retornarem à sua posição de guarda original ao redor do mestre.
  - Despawn sincronizado em caso de derrota do Boss (`onMasterDied`), abatendo os lacaios daquela geração.
  - Respawn sincronizado de lacaios abatidos apenas enquanto o mestre permanecer vivo no mundo.
  - Validado via testes unitários em `NpcAiAdvancedRetailTest.java` (`testMinionCrossRetaliationAndMasterProtection`, `testMinionReturnToSpawnWithMaster`, `testMasterDiedDespawnsMinions`).
- [x] **CP 6.4: Faction Call e Agressividade por Nível (Level Difference Aggro)** ✅ Concluído
  - Monstros comuns não agram jogadores com 9 ou mais níveis de diferença (`diff >= 9`), enquanto Raid Bosses e Grand Bosses agram qualquer jogador no seu alcance independente de nível.
  - Mobs da mesma facção chamam auxílio mútuo em raio de 400u (`factionRange`).
  - Validado via testes unitários em `NpcImprovementsTest.java` (`shouldTriggerFactionCallWhenAttacked`, `shouldRespectAggroLevelDifferenceForNormalMobsAndBosses`).

### 7.7 Checkpoints da Fase 7: Quests de Clã, Alianças de Facção (Ketra/Varka) e Farm Endgame (Soul Crystals e S-Grade)
- [x] **CP 7.1: Quests de Progressão e Nível de Clã (501: Proof of Clan Alliance & 503: Pursuit of Clan Ambition)** ✅ Concluído
  - Catálogo canônico unificado em `ClanQuestCatalog.java` mapeando a elevação para Clã Nível 4 (Quest 501, Sir Kristof Rodemai 30756, Bruxa Kalis 30759, Alliance Manifesto 3874, 120.000 SP) e Clã Nível 5 (Quest 503, Sir Gustaf Athebaldt 30760, Seal of Aspiration 3870, 250.000 SP).
  - Implementação nativa da `Quest501ProofOfClanAlliance.java` com prova de sacrifício voluntário na Statue of Offering (30757), entrega dos Symbols of Loyalty, antídoto de ervas e concessão do Alliance Manifesto.
  - Suporte a verificação de liderança de clã (`clanLeader()`, `clanId()`) em `PlayerCharacter.java`.
  - Validado via testes unitários em `ClanAndEndgameQuestsTest.java` (`testClanQuestCatalogIntegrity`, `testQuest501ProofOfClanAllianceFlow`).
- [x] **CP 7.2: Quests de Alianças Épicas de Ketra Orcs e Varka Silenos (605 & 611)** ✅ Concluído
  - Catálogo canônico estruturado em `FactionAllianceCatalog.java` cobrindo os 5 estágios oficiais de aliança:
    - Ketra Orcs (Quest 605, Hierarch Wahkan 31371, marcas 7211–7215, insígnias de soldados/capitães/generais Varka 7216–7218).
    - Varka Silenos (Quest 611, Hierarch Naran Ashanuk 31378, marcas 7221–7225, insígnias de soldados/capitães/generais Ketra 7226–7228).
  - Implementação nativa da `Quest605AllianceWithKetraOrcs.java` com abates de Silenos, acúmulo de badges e promoção para Mark of Ketra's Alliance Stage 1.
  - Validado via testes unitários em `ClanAndEndgameQuestsTest.java` (`testFactionAllianceCatalogIntegrity`, `testQuest605AllianceWithKetraOrcsFlow`).
- [x] **CP 7.3: Quests de Soul Crystals e Farm Endgame S-Grade (350, 373, 617, 619)** ✅ Concluído
  - Catálogo canônico estruturado em `EndgameFarmCatalog.java` mapeando as principais jornadas de obtenção de Special Abilities (SA) em armas (Quest 350 Enhance Your Weapon com Soul Crystals Red 4629, Green 4639, Blue 4649 nos estágios 0 a 13), Alquimia em Ivory Tower (Quest 373 Supplier of Reagents), e troca de Torches por receitas de armas S-Grade em Forge of the Gods (Quest 617 Gather the Flames).
  - Implementação nativa da `Quest350EnhanceYourWeapon.java` com entrega de cristais iniciais pelos Magisters Jurek (30115), Gideon (30856) e Winonin (30194).
  - Implementação nativa da `Quest617GatherTheFlames.java` com coleta de Torches (7264) em FotG e troca de 1.000 unidades por receitas oficiais de armas S-Grade (Forgotten Blade, Draconic Bow, Arcana Mace, etc.) com o ferreiro Vulcan (31539) e Warsmith Rooney (32049).
  - Validado via testes unitários em `ClanAndEndgameQuestsTest.java` (`testEndgameFarmCatalogIntegrity`, `testQuest350EnhanceYourWeaponSelection`, `testQuest617GatherTheFlamesTorchExchange`).

---

**Diretriz de Execução Imediata**: Este documento é a Bíblia de Engenharia e Execução Contínua do projeto. Conforme cada checkpoint for implementado e validado via testes automatizados, o status é promovido a `[x] ✅ Concluído`.
