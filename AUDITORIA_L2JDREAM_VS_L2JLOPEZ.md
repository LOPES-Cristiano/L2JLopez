# 🔍 Auditoria Comparativa Completa: L2JDream V2 vs L2JLopez

Este documento consolida a pesquisa aprofundada de **todas as funcionalidades, sistemas, mecânicas e configurações** presentes no **L2JDream V2** (`D:\Cristiano\Lineage\L2JDreamV2`) que estão **ausentes, incompletas ou não integradas** no **L2JLopez** (`D:\Cristiano\Lineage\L2JLopez`).

---

## 📊 1. Matriz de Comparação Executiva

| Categoria | Funcionalidade no L2JDream | Origem (Classes/Configs) | Estado no L2JLopez | Impacto & Valor |
|---|---|---|---|---|
| **PvE** | **Mobs Champion System** | `L2Spawn`, `L2Attackable`, `fun_events.properties` | 🔴 **Ausente** (flags declaradas em `Config.java` mas sem código) | ⭐️⭐️⭐️⭐️⭐️ (Altíssimo) |
| **QoL / PvE** | **Live In-Game AutoFarm** | `L2FarmPlayableAI.java`, `AutoFarm.java` | 🔴 **Ausente** (possui apenas `OfflineFarmService`) | ⭐️⭐️⭐️⭐️⭐️ (Altíssimo) |
| **Combate** | **Custom Cancel Return (5s)** | `Disablers.java`, `CustomCancelTask` | 🔴 **Ausente** (cancelamento no Lopez é permanente) | ⭐️⭐️⭐️⭐️⭐️ (Altíssimo no PvP) |
| **Economia** | **Tabela de Enchant Granular por Nível** | `rates.properties`, `RequestEnchantItem` | 🔴 **Incompleto** (Lopez tem taxa fixa em 66%) | ⭐️⭐️⭐️⭐️⭐️ (Altíssimo) |
| **PvP** | **Quake PvP Announce & Killstreaks** | `L2PcInstance.java`, `add-on.properties` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Muito Alto) |
| **PvP** | **War Legend (Hero Aura por Kills)** | `L2PcInstance.java`, `add-on.properties` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Muito Alto) |
| **PvP** | **Progressão de Cor de Nick/Título por PvP** | `mods.properties`, `L2PcInstance.java` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Alto) |
| **Segurança** | **Anti-Bot Captcha Verification** | `BotsPreventionManager.java`, `mods.properties` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Muito Alto) |
| **Social** | **Sistema de Casamento (Wedding & Couple)** | `CoupleManager.java`, `Wedding.java` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Alto) |
| **Economia** | **Lojas Privadas com Moedas Alternativas** | `TradeStorePacketHandler`, `mods.properties` | 🔴 **Ausente** (apenas Adena 57 suportada) | ⭐️⭐️⭐️⭐️ (Alto) |
| **Endgame** | **Cercos de Clan Halls Conquistáveis** | `com.dream.game.manager.clanhallsiege.*` | 🔴 **Ausente** (apenas Castelos têm cerco) | ⭐️⭐️⭐️⭐️ (Alto) |
| **Endgame** | **Dungeon Four Sepulchers (4S)** | `FourSepulchersManager.java` | 🔴 **Ausente** | ⭐️⭐️⭐️⭐️ (Alto) |
| **Recompensas** | **Automated Siege Rewards & Offline Queue** | `SiegeRewardManager.java` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio/Alto) |
| **QoL** | **Barakiel Direct Noblesse Reward** | `L2Boss.java`, `KillBarakielSetNobless` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio/Alto) |
| **Minigame** | **Lucky Roulette / Cassino In-Game** | `RoletaData.java`, `Roulette.java`, `roulette.xml` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio) |
| **PvE** | **Deep Blue Drop Rules & Delta Aggro** | `options.properties`, `AltMobNoAttack...` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio) |
| **Visual** | **DressMe / Visual Skins System** | `DressMeData.java`, `applySkins.java` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio) |
| **Evolução** | **Character Reset / Rebirth System** | `ResetData.java`, `resetData.xml` | 🔴 **Ausente** | ⭐️⭐️⭐️ (Médio) |

---

## 🔬 2. Detalhamento Técnico das Funcionalidades Identificadas

### 2.1. Mobs Champion System (PvE Dinâmico)
- **Como funciona no L2JDream:**
  - No `L2Spawn.java`, todo mob comum (não-boss, não-minion, não-quest) com nível entre `ChampionMinLevel` (20) e `ChampionMaxLevel` (80) rola uma chance percentual de `ChampionFrequency` (ex: 5% a 20%).
  - Ao ser sorteado, recebe `mob.setChampion(true)`:
    - **HP Máximo:** Multiplicado por `ChampionHp` (7x ou 8x).
    - **P.Atk / M.Atk / SpdAtk:** Multiplicados conforme configuração.
    - **Visual:** Título alterado para "Champion" com coloração diferenciada (vermelho/azul) e aura visual de círculo no chão.
    - **Recompensas ao Morrer:** EXP e SP multiplicados por `ChampionExpSp` (8x-10x); Drop geral multiplicado por `ChampionRewards`; Drop de Adena multiplicado por `ChampionAdenasRewards`.
    - **Drop Especial:** Sorteio adicional de Medalhas de Evento (ID 6392/6393) ou Festival Adena se a diferença de nível com o jogador for menor ou igual a `ChampionSpecialItemLevelDiff`.
- **Por que falta no L2JLopez:** As variáveis estáticas existem em `Config.java`, mas o `SpawnService` e a rotina de combate/drops nunca as consultam ou aplicam.

---

### 2.2. In-Game Live AutoFarm System (.autofarm)
- **Como funciona no L2JDream:**
  - `L2FarmPlayableAI.java` roda como uma tarefa periódica de baixa latência (500ms) para jogadores online com a flag `isAutoFarm = true` (ativada via `.autofarm`).
  - **Varredura e Alvos:** Localiza monstros vivos e atacáveis em um raio de até 1200 unidades, validando linha de visão (`GeoEngine.canSeeTarget`).
  - **Mapeamento da Barra de Atalhos (Shortcuts):**
    - Slots F1 a F4: Ciclo e rotação de habilidades de ataque disponíveis com MP suficiente.
    - Slot F8: Manutenção automática de Toggle Skills ativas (War Cry, Focus Mind, etc.).
    - Slots F11 e F12: Acionamento de cura de emergência / poções quando o HP do personagem desce abaixo de 50%.
    - Slot de Ataque Físico: Executa aproximação e auto-ataque com soulshot automático.
  - **Interrupção de Segurança:** Cancela instantaneamente o autofarm se o jogador mover o personagem manualmente (`MoveBackwardToLocation`), sentar, morrer, entrar em área de paz ou combate PvP.
- **Situação no L2JLopez:** Possui o `OfflineFarmService` (jogadores desconectados com clones phantom), mas jogadores ativos conectados não têm ferramenta de autofarm in-game.

---

### 2.3. Custom Cancel Return / Recovery System (Equilíbrio de PvP)
- **Como funciona no L2JDream:**
  - No `Disablers.java`, quando as skills `Cancel` (ID 1056) ou `Touch of Death` removem até 5 buffs do alvo:
    - Os buffs removidos são temporariamente capturados em uma lista `cancelledBuffs`.
    - Uma tarefa agendada `CustomCancelTask` é disparada para executar após `Config.CUSTOM_CANCEL_SECONDS` (default: 5 segundos).
    - Ao disparar, todos os buffs cancelados são automaticamente restaurados no jogador com sua duração restante preservada.
- **Importância:** Elimina a frustração de ter barras de 24+ buffs totalmente zeradas em massa no PvP aberto, ao mesmo tempo em que preserva a janela tática de 5 segundos de burst para derrotar o alvo desprotegido.

---

### 2.4. Tabela de Enchant Granular por Nível e Tipo de Scroll
- **Como funciona no L2JDream:**
  - Em `rates.properties`, as chances não são um número único, mas uma sequência de pares `nível,chance%`:
    - `NormalWeaponEnchantLevel = 1,100;2,100;3,100;4,70;5,65;6,60;7,55;8,50;9,45;10,40;...`
    - `BlessWeaponEnchantLevel = 1,100;2,100;3,100;4,80;5,75;6,70;7,65;8,60;9,55;...`
    - `CrystalWeaponEnchantLevel = 1,100;2,100;3,100;4,90;5,85;6,80;7,75;...`
    - Tabelas separadas para Armaduras e Joias.
    - Limites máximos por scroll: `EnchantMaxWeaponNormal`, `EnchantMaxWeaponBlessed`, `EnchantMaxWeaponCrystal`.
    - Verificação de segurança no momento do equipamento (`CheckEnchantLevelEquip`).
- **Situação no L2JLopez:** Em `ItemPacketHandler.java`, a chance após o limite seguro (+3 ou +4 em peito inteiro) é hardcoded em `ThreadLocalRandom.current().nextInt(100) < 66` (66% plano para qualquer nível e tipo).

---

### 2.5. Quake PvP Announce & Killstreaks
- **Como funciona no L2JDream:**
  - No `L2PcInstance.java`, a cada abate de jogador em PvP válido (não-farm, não-mesmo IP):
    - Incrementa o contador `spreeKills`.
    - Ao atingir marcos (1 = First Blood, 2 = Dominating, 4 = Rampage, 8 = Killing Spree, 16 = Monster Kill, 24 = Unstoppable, 32 = Ultra Kill, 48 = God Like, 64 = Wicked Sick, 96 = Ludicrous, 128 = Holy Shit, 132 = Flawless Victory), transmite anúncio global e pacote de som no canal de sistema/mercado.
    - Morte do jogador reseta o streak para 0.
- **Situação no L2JLopez:** Registra apenas a contagem bruta de `pvpKills` no banco de dados.

---

### 2.6. War Legend System (Hero Aura Temporária no PvP)
- **Como funciona no L2JDream:**
  - Se um jogador alcançar 30 abates consecutivos sem morrer (`heroConsecutiveKillCount >= 30`):
    - Um broadcast global anuncia o novo "War Legend".
    - O jogador ganha a flag `isPVPHero = true`.
    - Em `UserInfo` e `CharInfo`, o cliente renderiza a Aura Dourada de Herói da Olimpíada ao redor do personagem.
    - A aura dura até o jogador ser derrotado por outro jogador ou deslogar.
- **Situação no L2JLopez:** Apenas heróis oficiais do ciclo de Olimpíada possuem a aura.

---

### 2.7. Anti-Bot Captcha Verification (Segurança sem Client Patch)
- **Como funciona no L2JDream:**
  - `BotsPreventionManager.java` contabiliza monstros abatidos por cada jogador.
  - A cada `KillsCounter` + random (60 a 110 kills), abre um diálogo HTML forçado na tela do jogador.
  - O diálogo exibe uma imagem gerada ou selecionada que utiliza o ID de `PledgeCrest` (nativo do protocolo do Lineage II Interlude, sem necessitar de patches no cliente).
  - O jogador tem 60 segundos (`ValidationTime`) para selecionar o botão correspondente à imagem correta.
  - Se falhar ou expirar: teletransporta para a vila mais próxima ou coloca em jail (`Punishment`).
- **Situação no L2JLopez:** Não possui proteção anti-bot ativa in-game.

---

### 2.8. Sistema de Casamento (Wedding & Couple System)
- **Como funciona no L2JDream:**
  - Gerenciado por `CoupleManager.java` e `Wedding.java` com tabela no banco `mods_wedding`.
  - Permite pedido de casamento via NPC cerimonial (50014) ou comando `.engage`.
  - Exige traje formal (Formal Wear) e taxa em Adena.
  - Entrega o item cosmético *Cupid's Bow* aos noivos.
  - Permite o teleporte conjugal via `.gotolove`:
    - Possui tempo de conjuração de 15s com barra de gauge.
    - Validação de segurança estrita: impede teleporte se o parceiro estiver em Olimpíada, Zona de Cerco, Dimensional Rift, Boss Lair, Festival ou Prisão.
  - Suporta divórcio via `.divorce` com penalidade percentual de Adena.
- **Situação no L2JLopez:** Inexistente.

---

### 2.9. Lojas Privadas com Moedas Alternativas (Gold Bars / Medals)
- **Como funciona no L2JDream:**
  - `SellByItem = True`, `SellItem = 3470` (Gold Bar) ou `6392` (Event Medal).
  - Permite que a Private Store (venda e compra) negocie itens utilizando barras de ouro em vez de Adena, permitindo negociações de alto valor acima de 2.147.483.647 Adenas.
- **Situação no L2JLopez:** Apenas moeda 57 (Adena) é suportada nas operações de compra/venda de jogadores.

---

### 2.10. Cercos de Clan Halls Conquistáveis (Conquerable CH Sieges)
- **Como funciona no L2JDream:**
  - Pacote `com.dream.game.manager.clanhallsiege.*`:
    - **Fortress of the Dead:** Cerco contra NPCs zumbis e a raid boss Lidia von Hellmann.
    - **Devastated Castle:** Cerco contra as forças do lorde Gustav.
    - **Rainbow Springs Chateau:** Competição temática com pesca e mini-game de Hot Springs.
    - **Bandit Stronghold:** Cerco com corrida de bestas e combate de facção.
    - **Wild Beast Farm:** Cerco com dominação de animais domesticados.
- **Situação no L2JLopez:** Possui apenas cerco aos 9 Castelos convencionais. Os Clan Halls conquistáveis não possuem scripts de cerco e NPCs defensores.

---

### 2.11. Dungeon Four Sepulchers (4S em Imperial Tomb)
- **Como funciona no L2JDream:**
  - `FourSepulchersManager.java`: 4 alas subterrâneas no Imperial Tomb para grupos com líderes portando itens de acesso.
  - Portões que se fecham, cronômetro de 50 minutos, caixas secretas misteriosas, spawns sucessivos de guardas sepulcrais e confronto final com o *Shadow of Halisha*.
- **Situação no L2JLopez:** As salas existem geograficamente, mas não possuem o controlador cronometrado da dungeon.

---

## 🚀 3. Plano de Implementação Recomendado para o L2JLopez

Para manter a consistência da arquitetura **Spring Boot**, a política estrita de **Zero Doações** (economia orientada exclusivamente a Adena, Gold Bars e drops) e a regra de **1 commit por arquivo com 100% de testes verdes**, estruturamos as novas fases recomendadas em ondas claras:

### 🌟 Fase 1: PvE & QoL de Combate de Alto Impacto
1. **Onda C12 - Mobs Champion System: [CONCLUÍDO ✅]**
   - [x] Criado `ChampionService.java` com injeção Spring, suporte a multiplicadores de HP (x8), P.Atk/M.Atk (x1.25), P.Def/M.Def (x1.1), EXP/SP (x8), Drops/Adena (x8) e roll de Medalha de Evento (ID 6392).
   - [x] Conectado no `SpawnService` e `NpcAiService` para sortear o estado Champion ao nascer ou respawnar.
   - [x] Conectado no `GameServerPacket` (`NpcInfo`) com título `Champion` e aura vermelha circular de time (`team = 2`).
   - [x] Conectado no `CombatService`, `DropService` e `GameSession` para multiplicar dano/HP, EXP/SP e drops.
   - [x] Testes unitários com 100% de aprovação em `ChampionServiceTest.java`.

2. **Onda C13 - Custom Cancel Return (5s):**
   - Implementar `CancelRecoveryService.java` para capturar efeitos cancelados e reagendá-los com Virtual Threads.
   - Integrar nas rotinas de cálculo de efeito de desativação (`ActionPacketHandler` / `SkillEffects`).
   - Teste unitário de verificação temporal e integridade de slots em `CancelRecoveryServiceTest.java`.

3. **Onda C14 - Tabela Granular de Enchant por Nível:**
   - Criar `EnchantTableService.java` gerenciando probabilidades nível a nível para armas/armaduras/joias (Normal, Blessed, Crystal) com limites máximos.
   - Conectar em `ItemPacketHandler.java` substituindo o valor fixo de 66%.
   - Testes unitários de curva de probabilidade e segurança em `EnchantTableServiceTest.java`.

---

### ⚔️ Fase 2: PvP & Competitivo
4. **Onda C15 - Quake PvP & War Legend Engine:**
   - Implementar `PvPStreakService.java` gerenciando abates consecutivos, mensagens globais Quake, e concessão da Hero Aura aos 30 kills.
   - Conectar com `PlayerCharacter` e pacotes `CreatureSay` / `UserInfo`.
   - Testes unitários em `PvPStreakServiceTest.java`.

5. **Onda C16 - Lojas Privadas com Moedas Alternativas:**
   - Expandir `TradeStorePacketHandler.java` para permitir a seleção de moeda entre Adena (57) e Gold Bar (3470).
   - Testes unitários de balanço e transação em `AlternativeStoreCurrencyTest.java`.

---

### 🛡️ Fase 3: Segurança, Social & Endgame
6. **Onda C17 - Anti-Bot Captcha Service:**
   - Criar `AntiBotCaptchaService.java` com contadores por jogador, gerador de códigos visuais em `PledgeCrest` e diálogos HTML responsivos.
   - Testes unitários de validação e timeout em `AntiBotCaptchaServiceTest.java`.

7. **Onda C18 - Wedding & Couple Service:**
   - Criar `WeddingService.java` com repositório de casais, NPC cerimonial e comandos de voz `.engage`, `.gotolove` e `.divorce` com validação de zonas.
   - Testes unitários em `WeddingServiceTest.java`.

8. **Onda C19 - In-Game Live AutoFarm (.autofarm):**
   - Criar `LiveAutoFarmService.java` executando varredura e rotação de shortcuts dos jogadores online ativos com failsafes em threads virtuais.
   - Testes unitários em `LiveAutoFarmServiceTest.java`.
