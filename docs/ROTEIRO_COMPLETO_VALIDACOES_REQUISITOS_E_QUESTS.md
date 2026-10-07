# Roteiro Completo: Validações, Requisitos e Motor Integral de Quests — L2JLopez

> **Documento Oficial de Engenharia, Regras de Negócio e Conteúdo de Quests**  
> **Referência**: Lineage II Interlude (Chronicle 6, Protocolo 746) — Base de Comparação: `L2JDream V2`  
> **Destino**: `D:\Cristiano\Lineage\L2JLopez` (Java 21 LTS, Spring Boot 3.5, Spring Data JDBC, HikariCP)  
> **Status**: Roteiro Mestre de Especificação, Validações e Implementação

---

## Índice Geral
1. [Diagnóstico de Maturidade e Visão Estratégica](#1-diagnóstico-de-maturidade-e-visão-estratégica)
2. [PARTE I: Matriz Geral de Validações e Requisitos de Regras de Negócio](#parte-i-matriz-geral-de-validações-e-requisitos-de-regras-de-negócio)
   - [1.1 Validações de Combate e Engajamento](#11-validações-de-combate-e-engajamento)
   - [1.2 Validações de Itens, Inventário e Equipamentos](#12-validações-de-itens-inventário-e-equipamentos)
   - [1.3 Validações de Habilidades e Conjuração (Skills & Casting)](#13-validações-de-habilidades-e-conjuração-skills--casting)
   - [1.4 Validações de Troca, Lojas e Segurança Econômica (Anti-Exploits)](#14-validações-de-troca-lojas-e-segurança-econômica-anti-exploits)
   - [1.5 Validações de Movimento, Terreno e Zonas Especiais](#15-validações-de-movimento-terreno-e-zonas-especiais)
   - [1.6 Validações de Subclasses, Nobres e Olimpíadas](#16-validações-de-subclasses-nobres-e-olimpíadas)
   - [1.7 Validações de Clãs, Alianças e Guerras](#17-validações-de-clãs-alianças-e-guerras)
3. [PARTE II: Arquitetura do Motor de Quests & Tutorial](#parte-ii-arquitetura-do-motor-de-quests--tutorial)
   - [2.1 Persistência e Modelo de Dados (`character_quests`)](#21-persistência-e-modelo-de-dados-character_quests)
   - [2.2 Ciclo de Vida e Hooks de Eventos (Java-Native Event Dispatcher)](#22-ciclo-de-vida-e-hooks-de-eventos-java-native-event-dispatcher)
   - [2.3 Pacotes de Rede de Quests e Tutorial (0x80 e 0xa0–0xa3)](#23-pacotes-de-rede-de-quests-e-tutorial-0x80-e-0xa00xa3)
4. [PARTE III: Roteiro e Catálogo Completo das Quests Retail Interlude](#parte-iii-roteiro-e-catálogo-completo-das-quests-retail-interlude)
   - [3.1 O Tutorial Inicial Completo (Quest 255) para as 5 Raças](#31-o-tutorial-inicial-completo-quest-255-para-as-5-raças)
   - [3.2 Quests de Primeira Troca de Classe (Níveis 18–20: 401 a 418)](#32-quests-de-primeira-troca-de-classe-níveis-1820-401-a-418)
   - [3.3 Quests de Segunda Troca de Classe (Níveis 35–40: 211 a 233)](#33-quests-de-segunda-troca-de-classe-níveis-3540-211-a-233)
   - [3.4 Quests de Terceira Troca de Classe (Nível 76+: Sagas 70 a 100)](#34-quests-de-terceira-troca-de-classe-nível-76-sagas-70-a-100)
   - [3.5 Quests de Subclasse e Nobless (234, 235, 241, 242, 246, 247)](#35-quests-de-subclasse-e-nobless-234-235-241-242-246-247)
   - [3.6 Quests de Acesso aos Grand Bosses (337, 348, 618, 119)](#36-quests-de-acesso-aos-grand-bosses-337-348-618-119)
   - [3.7 Quests de Clã, Alianças e Reputação (501, 503, 605, 611)](#37-quests-de-clã-alianças-e-reputação-501-503-605-611)
   - [3.8 Quests de Reagentes, Soul Crystals e Farm S-Grade](#38-quests-de-reagentes-soul-crystals-e-farm-s-grade)
5. [PARTE IV: Fases de Implementação, Checklists e Testes Automatizados](#parte-iv-fases-de-implementação-checklists-e-testes-automatizados)

---

## 1. Diagnóstico de Maturidade e Visão Estratégica

O **L2JLopez** atingiu paridade arquitetural de infraestrutura em relação à **L2JDream V2**, operando com Java 21 Virtual Threads, Spring Boot 3.5, persistência em HikariCP/Spring Data JDBC e cobertura de mais de 260 testes automatizados. Todos os 118 subsistemas estruturais (Spawns, Castelos, Grand Bosses, Sieges, Manor, Cursed Weapons, Eventos PvP) estão instanciados no servidor.

No entanto, para transformar o projeto em um ambiente **100% retail-ready**, indestrutível contra exploits e com progressão autêntica de jogador, os dois grandes pilares pendentes são:
1. **Validações Exaustivas de Regras de Negócio e Requisitos**: Blindagem de cada ação do cliente com checagens de peso, slots, graus de equipamento, compatibilidade de armas, zonas de combate, alcance e atômicos anti-duplicação.
2. **Subsistema Integral de Quests e Tutorial**: Implementação do motor nativo de Quests com persistência, pacotes `QuestList` e `TutorialShowHtml`, cobrindo desde o primeiro passo do personagem nível 1 até as Sagas de 3ª classe e o status de Nobless.

---

## PARTE I: Matriz Geral de Validações e Requisitos de Regras de Negócio

### 1.1 Validações de Combate e Engajamento

O combate é o coração do Lineage II. Qualquer brecha em validações de alvos ou alcance compromete o equilíbrio competitivo.

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.01** | **Ataque em Zona de Paz** | Se `active.insidePeaceZone()` ou `target.insidePeaceZone()` e não estiver em duelo/arena: abortar ataque. | `TARGET_IN_PEACEZONE` | ✅ Implementado |
| **V.02** | **Alvo Morto ou Invulnerável** | Checar se `target.isDead()` ou `target.isInvul()`. Impedir novo ataque físico ou início de cast. | `INVALID_TARGET` | ✅ Implementado |
| **V.03** | **Distância de Ataque Físico** | Validar distância em relação ao `physicalAttackRange` da arma equipada (40u corpo-a-corpo, 500–900u arco). Se fora do alcance, mover até a distância mínima antes do primeiro golpe. | Movimento automático até alcance | 🟡 Parcial (Ajustar suavidade) |
| **V.04** | **Consumo de Flechas / Projéteis** | Ao disparar arco, checar flecha do grau correto no inventário (`ItemSlots.SLOT_LHAND`). Se sem flechas: cancelar ataque. | `NOT_ENOUGH_ARROWS` | ✅ Implementado |
| **V.05** | **Flag de PvP (`pvpFlag`)** | Atacar jogador não-flagado fora de zona de paz ativa `pvpFlag = 1` por 40 segundos. Dano ou debuff renova o timer para 40s. | `CharInfo` envia nome roxo | ✅ Implementado |
| **V.06** | **Sistema de Karma e PK** | Matar jogador sem `pvpFlag` ativo concede +1 PK e adiciona Karma calculado pela fórmula `(level * 1000) / (pkCount + 1)`. Jogador com karma > 0 tem nome vermelho. | `UserInfo` / `CharInfo` nome vermelho | ✅ Implementado |
| **V.07** | **Perda de Alvo ao Morrer** | Quando o alvo tem HP zerado, todos os atacantes devem receber `TargetUnselected` e `AutoAttackStop`. | `TargetUnselected (0x2a)` | ✅ Implementado |
| **V.07A** | **Sistema de Facções de NPCs (*Faction / Social Call*)** | Ao entrar em combate, monstros com `factionId` chamam aliados da mesma facção em um raio configurado (`factionRange`, padrão 400u). | Aliados entram em combate e socorrem | ✅ Implementado |
| **V.07B** | **Agressividade por Nível (*Level Difference Aggro*)** | Monstros comuns não agram jogadores com 9 ou mais níveis acima (Retail Interlude / L2JDream). Raid Bosses e Grand Bosses agram qualquer nível. | Checagem de aggro retail no tick da IA | ✅ Implementado |
| **V.07C** | **Conjuração de Habilidades por Monstros (*Monster Skills*)** | Mobs usam suas habilidades reais de `data/xml/world/npc_skills.xml` (26.200+ mapeamentos) com animação `MagicSkillUse`, dano e debuffs. | `MagicSkillUse (0x48)` e cálculo de dano | ✅ Implementado |
| **V.07D** | **Sistema de Spoil & Sweeper (Anões)** | Habilidade Spoil (254) ativa condição no mob vivo (`SPOIL_SUCCESS`). Na morte, drops de categoria `< 0` são sorteados e colhidos via Sweeper (42). | Entrega no inventário e remoção do spoil | ✅ Implementado |
| **V.08** | **Cancelamento de Cast por Dano Massivo** | Dano sofrido durante a conjuração de magia possui chance percentual de quebrar o cast: `Chance = (Dano / MaxHP) * 100 * fatorSkill`. | `MagicSkillCanceld (0x49)` | ✅ Implementado |
| **V.09** | **Cancelamento de Cast por Movimento** | Qualquer clique de movimentação (`MoveBackwardToLocation`) enquanto `casting == true` interrompe a conjuração imediatamente. | `MagicSkillCanceld (0x49)` | ✅ Implementado |
| **V.10** | **Proteção contra Auto-Ataque Amigo** | Membros da mesma party ou clã sem guerra mútua não podem se auto-atacar com ataque simples (somente com Force Attack segurando `Ctrl`). | `INVALID_TARGET` | ✅ Implementado |

---

### 1.2 Validações de Itens, Inventário e Equipamentos

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.11** | **Penalidade de Grau de Equipamento (Grade Penalty)** | Checar nível do char vs grau do item: D (20+), C (40+), B (52+), A (61+), S (76+). Se equipado item de grau superior: aplicar Grade Penalty (Redução drástica de P.Atk, Atk.Spd, M.Atk, Cast.Spd e Accuracy). | Skill passiva oficial / `EtcStatusUpdate` | ✅ Implementado |
| **V.12** | **Limite de Peso (Weight Penalty)** | Carga máxima calculada por `CON` e passivas de peso: 50% (sem penalidade), 66% (sem regeneração de HP/MP), 80% (velocidade -50%), 100% (impossibilitado de atacar ou conjurar). | `EtcStatusUpdate` ícone de peso nos 4 graus | ✅ Implementado |
| **V.13** | **Limite de Slots de Inventário** | Limite padrão: 80 slots (raças normais), 100 slots (Anões). Ao receber item por drop, compra ou troca, validar se `inventory.size() >= limit`. Impedir recebimento se cheio. | `INVENTORY_FULL` | ✅ Implementado |
| **V.14** | **Exclusividade de Slots Duplos** | Equipar arma de duas mãos (`LRHAND`) remove automaticamente o escudo/sigil (`LHAND`). Equipar armadura de corpo inteiro (`FULLARMOR`) remove automaticamente calças (`LEGS`). | Itens anteriores desequipados no pacote | ✅ Implementado |
| **V.15** | **Requisitos de Encantamento** | Scroll D/C/B/A/S só pode encantar item do mesmo grau correspondente. Arma/Armadura deve ser compatível com `isEquipable()`. Scroll blessed não destrói o item em caso de falha (reseta para 0). | `EnchantResult (0x81)` | ✅ Implementado |
| **V.15B** | **Cor e Brilho Visual de Encantamento (*Enchant Glow*)** | Nível de encantamento da arma ativa (0..127) serializado nos pacotes `UserInfo (0x04)`, `CharInfo (0x03)` e `CharSelectionInfo (0x13)`, ativando o brilho retail no cliente (+4..+15 azul, +16+ vermelho). | Brilho/Aura oficial no cliente 3D | ✅ Implementado |
| **V.16** | **Destruição de Itens (`RequestDestroyItem`)** | Não pode destruir item equipado. Não pode destruir itens protegidos (`isDestroyable() == false`). Não pode destruir quantidade maior do que a existente no inventário. | `ActionFailed` se inválido | ✅ Implementado |
| **V.17** | **Compatibilidade de Life Stone (Augment)** | Life Stone só pode ser aplicada em armas de grau C, B, A ou S que não sejam armas heroicas, shadow ou já augmentadas. Gemstones D/C requeridas conforme grau. | `ExVariationResult` | ✅ Implementado |

---

### 1.3 Validações de Habilidades e Conjuração (Skills & Casting)

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.18** | **Requisito de Arma para Skills Específicas** | Mortal Blow/Backstab exigem Adaga (`DAGGER`); Stunning Shot/Double Shot exigem Arco (`BOW`); Hammer Crush/Stun Attack exigem Blunt (`BLUNT`); Triple Slash exige Espada Dual (`DUALFIST`/`DUAL`). | `INCORRECT_ITEM_TO_USE_SKILL` | ✅ Implementado |
| **V.19** | **Custo e Saldo de MP / HP** | Checar se `currentMp >= skill.mpConsume()` e `currentHp > skill.hpConsume()`. A skill não pode matar o próprio conjurador por falta de HP a menos que seja Suicide skill. | `NOT_ENOUGH_MP` / `NOT_ENOUGH_HP` | ✅ Implementado |
| **V.20** | **Consumo de Itens Reagentes de Skill** | Skills de alto nível consomem itens: Spirit Ore, Soul Ore, Energy Stones (Gladiador/Tyrant). Se o inventário não tiver a quantidade exata, o cast é bloqueado antes de iniciar. | `NOT_ENOUGH_ITEMS` | ✅ Implementado |
| **V.21** | **Cooldown / Reuso de Habilidade (`mReuse`)** | Nenhuma skill pode ser conjurada enquanto seu cooldown individual não estiver zerado no mapa `skillReuseTime`. Proteção contra bypass de pacotes de client. | `SKILL_NOT_READY` | ✅ Implementado |
| **V.22** | **Restrições de Alvos para Skills de Suporte** | Buffs de grupo só afetam membros da party. Curas direcionadas não afetam inimigos em combate. Ressurreição só pode ser conjurada em alvos mortos da mesma party/clã ou sem flag de PvP ativo. | `INVALID_TARGET` | ✅ Implementado |

---

### 1.4 Validações de Troca, Lojas e Segurança Econômica (Anti-Exploits)

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.23** | **Distância Máxima de Trade Direto** | Jogadores não podem trocar se distância > 150 unidades. Se um dos jogadores se afastar durante o trade: cancelamento automático imediato. | `TRADE_CANCELLED` | ✅ Implementado |
| **V.24** | **Trava de Troca em Combate ou Morte** | Não é permitido abrir trade se `active.isInCombat()` ou `target.isInCombat()` ou se qualquer um estiver morto. | `CANNOT_TRADE_DISCONNECTED_OR_DEAD` | ✅ Implementado |
| **V.25** | **Itens Não-Negociáveis (`isTradeable`)** | Armas heroicas, itens de quest, itens alugados (Shadow) e itens de novato não podem ser colocados na janela de Trade, Lojas Privadas ou Correio. | `CANNOT_TRADE_THIS_ITEM` | ✅ Implementado |
| **V.26** | **Prevenção de Duplicação Concorrente (Atomic Trade Commit)** | O fechamento de troca entre dois jogadores deve rodar em bloco sincronizado ou lock duplo ordenado por `objectId` para eliminar race conditions de duplicação. | Transação atômica em memória | ✅ Implementado |
| **V.27** | **Lojas Privadas (Private Store Sell/Buy)** | O vendedor não pode se mover nem conjurar enquanto a loja estiver aberta. Os itens vendidos devem permanecer reservados para não serem destruídos ou encantados em paralelo. | `PrivateStoreMsgSell` | ✅ Implementado |

---

### 1.5 Validações de Movimento, Terreno e Zonas Especiais

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.28** | **Zona de Água e Barra de Respiração (Drowning)** | Ao entrar em zona de água (`WaterZone`), ativar natação. Tempo limite de respiração de 60 segundos; após esgotado, perda contínua de 5% de HP/segundo por afogamento. | Envio de `SetupGauge (0x6d)` | 🟡 Pendente afogamento |
| **V.29** | **Zonas de Dano Ambiental (Lava / Pântano Ácido)** | Zonas vulcânicas (Forge of the Gods) e pântanos tóxicos (Swamp of Screams) aplicam dano contínuo periódico de HP a cada 3 segundos a personagens sem proteção. | Dano periódico por tick | 🟡 Pendente tick de zona |
| **V.30** | **Proibição de Fuga em Zonas de Cerco e Olimpíadas** | Scrolls de Escape e skills como `Return` ou `Gatekeeper Token` são estritamente bloqueados dentro de coliseus de olimpíadas e arenas de cerco a castelo. | `CANNOT_USE_HERE` | ✅ Implementado |

---

### 1.6 Validações de Subclasses, Nobres e Olimpíadas

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.31** | **Requisitos para Adição de Subclasse** | Personagem deve ter nível 75+, ter concluído as quests *Fate's Whisper (234)* e *Mimir's Elixir (235)*, ter menos de 3 subclasses ativas e não estar com itens de transformação equipados. | Diálogo do Grand Master | ✅ Implementado |
| **V.32** | **Incompatibilidades de Raça e Classe em Subclasse** | Elfos não podem pegar Dark Elf e vice-versa. Warsmith (Maestro) e Overlord (Dominator) não podem ser escolhidos como subclasse por nenhuma outra classe. | Bloqueio na lista de opções | ✅ Implementado |
| **V.33** | **Requisitos de Entrada nas Olimpíadas** | Apenas personagens Nobres (`isNoble == true`), na sua **Classe Principal** (não pode em subclasse), sem buffs externos, sem itens proibidos e com inventário com menos de 80% de carga. | Validação no registro do Monumento | ✅ Implementado |

---

### 1.7 Validações de Clãs, Alianças e Guerras

| # | Regra / Ação | Requisitos e Validações Obrigatórias | Mensagem de Sistema / Efeito | Status Atual |
|---|---|---|---|:---:|
| **V.34** | **Penalidade de Saída de Clã** | Jogador que abandona clã recebe penalidade de 24 horas sem poder ingressar em novo clã. Líder que expulsa jogador fica 24 horas sem poder recrutar. | Tabela `characters` / `clan_data` | ✅ Implementado |
| **V.35** | **Requisitos de Level-Up de Clã** | Validar saldo exato de SP, Adena e itens especiais (Blood Mark para lv 3, Alliance Manifesto para lv 4, Seal of Aspiration para lv 5) antes de promover o clã. | `ClanLevelUpPricesTable` | ✅ Implementado |
| **V.36** | **Guerras de Clã Mútuas** | Uma guerra só é considerada mútua quando ambos os líderes declaram formalmente. Somente em guerra mútua os membros podem se matar sem acumular PK em qualquer local fora de Peace Zone. | Sem penalidade de karma | ✅ Implementado |

---

## PARTE II: Arquitetura do Motor de Quests & Tutorial

### 2.1 Persistência e Modelo de Dados (`character_quests`)

Toda a progressão de quests do jogador deve ser persistida de forma relacional para suportar desconexões, relogs e reinicializações de servidor sem qualquer perda de estado.

A tabela oficial `character_quests` já criada nas migrações Flyway opera com a seguinte estrutura:

```sql
CREATE TABLE IF NOT EXISTS character_quests (
    char_id INT NOT NULL,
    name VARCHAR(60) NOT NULL,
    var VARCHAR(20) NOT NULL,
    value VARCHAR(255),
    PRIMARY KEY (char_id, name, var),
    FOREIGN KEY (char_id) REFERENCES characters(char_id) ON DELETE CASCADE
);
```

#### Variáveis Padrão de Estado:
- `<state>`: Estado atual da quest (`Created`, `Started`, `Completed`).
- `cond`: Passo numérico atual da missão (ex: `cond=1` falar com NPC A, `cond=2` matar 10 monstros, `cond=3` entregar itens).
- `<var_custom>`: Contadores temporários de monstros abatidos ou opções de diálogo selecionadas pelo jogador.

---

### 2.2 Ciclo de Vida e Hooks de Eventos (Java-Native Event Dispatcher)

Para eliminar a dependência frágil de scripts legados em Python/Jython (que causam lentidão de inicialização e erros em runtime no Java 21), o motor do L2JLopez adota um **`QuestManager` Spring nativo fortemente tipado**, onde cada Quest é uma classe Java que herda de `Quest` e implementa os hooks:

```text
               ┌────────────────────────────────────────────────────────┐
               │                     Ação do Jogador                    │
               └───────┬───────────────────┬───────────────────┬────────┘
                       │                   │                   │
                       ▼                   ▼                   ▼
                [Interação NPC]     [Abate de Mob]      [Entrada no Mundo]
                 (RequestBypass)     (CombatService)     (EnterWorld 0x03)
                       │                   │                   │
                       ▼                   ▼                   ▼
                  onTalk(...)         onKill(...)        onEnterWorld(...)
                       │                   │                   │
                       └───────────────┬───┴───────────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │          QuestState           │
                       │  - set("cond", 2)             │
                       │  - giveItems(itemId, count)   │
                       │  - playSound("ItemSound...")  │
                       │  - addRadar(x, y, z)          │
                       └───────────────┬───────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │          Persistência         │
                       │    (character_quests JDBC)    │
                       └───────────────────────────────┘
```

#### Métodos de Eventos da Classe Base `Quest`:
1. `onAdvEvent(String event, NpcInstance npc, PlayerCharacter player)`: Disparado ao clicar em opções de diálogo HTML que executam `bypass -h Quest <QuestName> <event>`.
2. `onTalk(NpcInstance npc, PlayerCharacter player)`: Disparado ao falar com o NPC. Retorna a página HTML correspondente ao estado atual do jogador.
3. `onKill(NpcInstance npc, PlayerCharacter player, boolean isPet)`: Disparado quando um monstro é abatido. Realiza sorteio de drop de itens de missão (`dropItem(itemId, chance, maxCount)`).
4. `onAttack(NpcInstance npc, PlayerCharacter player, int damage, boolean isPet)`: Disparado ao desferir golpes contra monstros com falas ou transições de fase de combate.
5. `onEnterWorld(PlayerCharacter player)`: Disparado no login do personagem para restaurar radares de quest, timers pendentes ou iniciar o tutorial inicial de nível 1.

---

### 2.3 Pacotes de Rede de Quests e Tutorial (0x80 e 0xa0–0xa3)

O cliente Lineage II Interlude possui uma interface gráfica dedicada para o gerenciamento de missões (atalho `Alt + U`) e uma janela flutuante multimídia para o Tutorial.

#### Pacotes a Implementar em `GameServerPacket.java`:

| Opcode | Nome do Pacote | Direção | Finalidade |
|:---:|---|:---:|---|
| **`0x80`** | **`QuestList`** | Servidor → Cliente | Envia a lista completa de missões em andamento e missões concluídas para preencher a janela `Alt + U`. |
| **`0xa0`** | **`TutorialShowHtml`** | Servidor → Cliente | Abre a janela visual de tutorial com botões, textos e ilustrações formatadas em HTML. |
| **`0xa1`** | **`TutorialShowQuestionMark`** | Servidor → Cliente | Exibe o ícone pulsante do Ponto de Interrogação azul piscando sobre a cabeça do jogador. |
| **`0xa2`** | **`TutorialEnableClientEvent`** | Servidor → Cliente | Habilita gatilhos de eventos locais do cliente (ex: matar 1 monstro, atingir nível 2, abrir inventário). |
| **`0xa3`** | **`TutorialCloseHtml`** | Servidor → Cliente | Fecha a janela de tutorial ativa no cliente. |

#### Pacotes de Entrada do Cliente em `GameClientPacket.java`:

| Opcode | Nome do Pacote | Direção | Finalidade |
|:---:|---|:---:|---|
| **`0x63`** | **`RequestQuestList`** | Cliente → Servidor | Disparado ao abrir a janela `Alt + U` para solicitar os dados de missões. |
| **`0x7e`** | **`RequestTutorialLinkHtml`** | Cliente → Servidor | Disparado ao clicar em links dentro da janela de tutorial. |
| **`0x7f`** | **`RequestTutorialPassCmdToServer`**| Cliente → Servidor | Envia comandos executados pelos botões de ação do tutorial. |
| **`0x80`** | **`RequestTutorialQuestionMark`** | Cliente → Servidor | Disparado quando o jogador clica no ponto de interrogação azul piscando. |
| **`0x81`** | **`RequestTutorialClientEvent`** | Cliente → Servidor | Notifica o servidor quando um evento de cliente monitorado ocorreu com sucesso. |

---

## PARTE III: Roteiro e Catálogo Completo das Quests Retail Interlude

### 3.1 O Tutorial Inicial Completo (Quest 255) para as 5 Raças

Todo novo personagem criado no Lineage II surge em um templo de partida com a **Quest 255 (Tutorial)** engatilhada automaticamente. Sem este fluxo, novos jogadores ficam sem Soulshots de novato, sem orientações de interface e sem mapas de suas vilas.

#### Fluxo Detalhado por Raça e Classe:

```text
[Criação do Personagem Lv 1]
           │
           ▼
[Disparo da Quest 255 (Tutorial)] ──> Toca tutorial_voice_001 e abre HTML introdutório
           │
           ▼
[Objetivo 1: Abate do Primeiro Monstro]
   - Humanos: Gremlin (ID 18342) na Talking Island
   - Elfos: Keltir (ID 18343) em Elven Forest
   - Dark Elves: Keltir (ID 18344) em Dark Elven Forest
   - Orcs: Keltir (ID 18345) no Immortal Plateau
   - Anões: Keltir (ID 18346) nas Dwarven Mountains
           │
           ▼
[Drop Garantido da Blue Gemstone (Item 6353)]
           │
           ▼
[Ponto de Interrogação Azul Pisca na Tela] ──> Jogador clica e recebe coordenadas com seta radar
           │
           ▼
[Falar com o Newbie Helper / Instrutor Inicial]
   - Entrega da Blue Gemstone
   - Recompensa 1: 200 Soulshots No-Grade para Iniciantes (Item 5789) ou 100 Spiritshots (Item 5790)
   - Recompensa 2: EXP e SP suficientes para subir imediatamente ao nível 2
           │
           ▼
[Orientação para a Capital da Província]
   - Ativação do radar para o Guardião / Mestre da Guilda da respectiva cidade
   - Conclusão do Tutorial básico e liberação da Quest de Armas de Iniciante
```

---

### 3.2 Quests de Primeira Troca de Classe (Níveis 18–20: 401 a 418)

As 18 profissões de 1ª classe exigem suas respectivas provas de honra e itens de admissão perante os Mestres de Guilda:

| ID | Nome da Quest | Classe Destino | Raça | NPC Inicial | Cidade | Item de Recompensa |
|:---:|---|---|:---:|---|---|---|
| **401** | **Path to a Warrior** | Warrior | Humano | Master Auron | Gludio | Medallion of Warrior (1145) |
| **402** | **Path to a Human Knight** | Knight | Humano | Sir Aaron Tanford | Gludio | Sword of Ritual (1161) |
| **403** | **Path to a Rogue** | Rogue | Humano | Captain Bezique | Gludin | Bezique's Letter (1180) |
| **404** | **Path to a Human Wizard** | Wizard | Humano | Parina | Gludio | Bead of Season (1210) |
| **405** | **Path to a Cleric** | Cleric | Humano | Priest Zigaunt | Gludin | Mark of Faith (1201) |
| **406** | **Path to an Elven Knight** | Elven Knight | Elfo | Master Sorius | Gludio | Elven Knight Brooch (1240) |
| **407** | **Path to an Elven Scout** | Elven Scout | Elfo | Master Reisa | Gludio | Reisa's Letter (1207) |
| **408** | **Path to an Elven Wizard** | Elven Wizard | Elfo | Rogellia | Elven Village | Eternity Diamond (1231) |
| **409** | **Path to an Oracle** | Elven Oracle | Elfo | Priest Manuel | Gludio | Leaf of Oracle (1235) |
| **410** | **Path to a Palus Knight** | Palus Knight | Dark Elf | Master Virgil | Gludio | Gaze of Abyss (1244) |
| **411** | **Path to an Assassin** | Assassin | Dark Elf | Triskel | Gludio | Iron Heart (1248) |
| **412** | **Path to a Dark Wizard** | Dark Wizard | Dark Elf | Varika | Dark Elf Village | Jewel of Darkness (1262) |
| **413** | **Path to a Shillien Oracle** | Shillien Oracle | Dark Elf | Magister Sidra | Gludio | Orb of Abyss (1270) |
| **414** | **Path to an Orc Raider** | Orc Raider | Orc | Prefect Karukia | Gludin | Mark of Raider (1592) |
| **415** | **Path to an Orc Monk** | Orc Monk | Orc | Gantaki Zu Urutu | Gludin | Khavatari Totem (1615) |
| **416** | **Path to an Orc Shaman** | Orc Shaman | Orc | Hestui | Orc Village | Mask of Medium (1631) |
| **417** | **Path to a Scavenger** | Scavenger | Anão | Collector Pippi | Dwarven Village | Ring of Raven (1642) |
| **418** | **Path to an Artisan** | Artisan | Anão | Blacksmith Silvera | Dwarven Village | Pass Certificate (1635) |

---

### 3.3 Quests de Segunda Troca de Classe (Níveis 35–40: 211 a 233)

Para atingir a 2ª classe no nível 40, cada personagem deve completar três missões independentes (Trial, Testimony e Test):

#### Provas de Julgamento (Trials - Nível 35+):
- **211_TrialOfChallenger**: Prova dos Gladiadores e Warlords (Kash e Martankus).
- **212_TrialOfDuty**: Prova dos Paladinos e Dark Avengers (Sir Aaron e Dust).
- **213_TrialOfSeeker**: Prova dos Arqueiros e Caçadores (Dufner e Terry).
- **214_TrialOfScholar**: Prova dos Magos Elementais e Necromancers (Mirien e Cronos).
- **215_TrialOfPilgrim**: Prova dos Curandeiros e Oráculos (Hermit Santiago).
- **216_TrialOfGuildsman**: Prova dos Anões Artesãos e Spoilers (Valcon e Blacksmith Altran).

#### Provas de Testemunho (Testimonies - Nível 37+):
- **217_TestimonyOfTrust**: Testemunho da Confiança (Aliança entre Humanos, Elfos e Dark Elves).
- **218_TestimonyOfLife**: Testemunho da Vida (Proteção da Mãe Árvore pelos Elfos).
- **219_TestimonyOfFate**: Testemunho do Destino (Rituais sombrios dos Dark Elves com Kaira).
- **220_TestimonyOfGlory**: Testemunho da Glória (Honra das tribos Orc com Kakai).
- **221_TestimonyOfProsperity**: Testemunho da Prosperidade (Riqueza das guildas de ferro dos Anões).

#### Provas de Aptidão (Tests - Nível 39+):
- **222 a 233**: Test of Duelist, Champion, Sagittarius, Searcher, Healer, Reformer, Magus, Witchcraft, Summoner, Maestro, Lord e Warspirit.

---

### 3.4 Quests de Terceira Troca de Classe (Nível 76+: Sagas 70 a 100)

A ascensão lendária de 3ª classe envolve a jornada pelas **Resonances of Tablet** nos confins de Aden e Elmore:

| Intervalo | Faixa de Quests | Classes Envolvidas |
|:---:|---|---|
| **70 a 79** | Phoenix Knight, Eva's Templar, Sword Muse, Duelist, Dreadnought, Titan, Grand Khavatari, Dominator, Doomcryer, Adventurer. | Classes de combate físico, tanques e orcs de batalha. |
| **80 a 89** | Wind Rider, Ghost Hunter, Sagittarius, Moonlight Sentinel, Ghost Sentinel, Cardinal, Hierophant, Eva's Saint, Archmage, Mystic Muse. | Caçadores, assassinos, curandeiros sagrados e arquimagos. |
| **90 a 100**| Storm Screamer, Arcana Lord, Elemental Master, Spectral Master, Soultaker, Hell Knight, Spectral Dancer, Shillien Templar, Shillien Saint, Fortune Seeker, Maestro. | Invocadores, dançarinos espectrais, necromantes e anões mestres. |

---

### 3.5 Quests de Subclasse e Nobless (234, 235, 241, 242, 246, 247)

As missões mais cobiçadas e disputadas do Lineage II Interlude:

```text
[Nível 75 Atingido]
        │
        ▼
[Quest 234: Fate's Whisper]
   - Caçar os 4 Raid Bosses: Shilen's Messenger Cabrio, Golkonda, Hallate e Kernon
   - Entregar as 4 Infernium Scepters ao Maestro Reorin
   - Obter Mold e entregar arma Top B para receber a Star of Destiny (Item 5011)
        │
        ▼
[Quest 235: Mimir's Elixir]
   - Mistura alquímica na Urna de Ivory Tower: Pure Silver + True Gold
   - Adicionar Blood Fire e obter o Elixir de Mimir
   - Bebida do elixir concede a liberação de Subclasse no Grand Master
        │
        ▼
[Subclasse no Nível 75]
        │
        ▼
[Saga de Nobless: Possessor of a Precious Soul (241, 242, 246, 247)]
   - Parte 1 (241): Legend of Seventeen, Malruk Succubus e Talisman com Talien
   - Parte 2 (242): Recuperação da princesa com Virgil e Pure Silver
   - Parte 3 (246): Abate do Raid Boss Barakiel em Valley of Saints para pegar Staff
   - Parte 4 (247): Consagração com Lady of the Lake em Coliseu -> Status de NOBLESSE
```

---

### 3.6 Quests de Acesso aos Grand Bosses (337, 348, 618, 119)

Sem estas missões, jogadores não conseguem transpor as barreiras mágicas das tocas dos chefes mais poderosos de Aden:

1. **Quest 337: Audience with the Land Dragon (Antharas)**:
   - NPC Inicial: Gabrielle em Giran.
   - Visitar 4 guardiões (Kendra, Chakiris, Gilmore, Theodric) e coletar fragmentos de joias de dragão para obter a **Portal Stone (Item 3865)**.
2. **Quest 348: An Arrogant Search (Baium)**:
   - NPC Inicial: Magister Hanellin em Aden.
   - Banhar os tecidos brancos em sangue de monstros de Platinum Tribe e Shaman em Tower of Insolence para gerar o cobiçado **Blooded Fabric (Item 4295)**.
3. **Quest 618: Into the Flame (Valakas)**:
   - NPC Inicial: Klein na entrada de Forge of the Gods.
   - Coletar Vacualite Ore para forjar a pedra mágica de calor extremo **Floating Stone (Item 7265)**.
4. **Quest 119: Last Imperial Prince (Frintezza)**:
   - NPC Inicial: Nameless Spirit no Imperial Tomb.
   - Investigar a história da família real e destravar o acesso com a Antique Brooch para abrir os portões da sala do órgão de Frintezza.

---

### 3.7 Quests de Clã, Alianças e Reputação (501, 503, 605, 611)

- **Quest 501: Proof of Clan Alliance**: Missão de Nível 4 de clã com Sir Kristof Rodemai e a bruxa Kalis. Exige o sacrifício e envenenamento voluntário de 3 membros do clã.
- **Quest 503: Pursuit of Clan Ambition**: Missão de Nível 5 de clã com Sir Gustaf Athebaldt. Envolve acordos com Balthazar, Rodemai e Eric para forjar o Scepter of Judgment.
- **Quest 605 / 611: Alianças com Ketra Orcs e Varka Silenos**: Sistema de reputação de 5 níveis onde matar membros da tribo rival aumenta o selo de aliança e libera lojas de receitas S-Grade exclusivas.

---

### 3.8 Quests de Reagentes, Soul Crystals e Farm S-Grade

1. **Quest 350: Enhance Your Weapon (Soul Crystals)**:
   - Aquisição dos Cristais Vermelho, Azul e Verde (Red, Blue, Green Soul Crystals) níveis 1 a 13 para aplicar Special Abilities (SA) como Focus, Acumen, Health e Guidance nas armas.
2. **Quest 373: Supplier of Reagents**:
   - Coleta de reagentes em Blazing Swamp para misturar na Alchemist's Urn de Ivory Tower (Moonstone Shards, Magma Dust, Pure Silver).
3. **Quest 617: Gather the Flames (Forge of the Gods)**:
   - Coleta de 1.000 Torch Flames para trocar por receitas de armas S-Grade com o ferreiro Rooney.
4. **Quest 619: Relics of the Old Empire (Imperial Tomb)**:
   - Coleta de relíquias imperiais para troca por receitas e pedaços de armaduras S-Grade (Imperial Crusader, Draconic Leather, Major Arcana).

---

## PARTE IV: Fases de Implementação, Checklists e Testes Automatizados

### Cronograma de Execução em 4 Fases:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ FASE 1: Motor Central de Quests & Pacotes de Rede (Imediato)           │
│  - Criação de QuestManager, Quest, QuestState e QuestTimer             │
│  - Implementação dos pacotes QuestList (0x80) e Tutorial (0xa0-0xa3)   │
│  - Conexão de eventos com GameSession e CombatService                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ FASE 2: Tutorial Inicial (Quest 255) & Quests de 1ª Classe (401-418)   │
│  - Fluxo completo das 5 raças, Blue Gemstone e Newbie Helper           │
│  - As 18 quests de 1ª classe com entrega de itens de admissão          │
│  - Validação de recompensas de EXP/SP e Soulshots de iniciante         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ FASE 3: Quests de Progressão Maior (2ª Classe, Sagas, Sub e Nobless)   │
│  - 2ª Classe (Trials, Testimonies, Tests: 211 a 233)                   │
│  - Sagas de 3ª Classe (70 a 100)                                       │
│  - Fate's Whisper (234), Mimir's Elixir (235) e Nobless (241-247)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ FASE 4: Quests de Chefes Épicos, Clãs e Blindagem de Validações        │
│  - Antharas (337), Baium (348), Valakas (618) e Frintezza (119)        │
│  - Quests de Clã lv 4 e 5 (501 e 503) e Ketra/Varka (605/611)          │
│  - Auditoria completa das validações de regras de combate e economia   │
└────────────────────────────────────────────────────────────────────────┘
```

### Checklist de Qualidade e Critérios de Conclusão:
- [ ] O comando de rede `RequestQuestList` (0x63) retorna todas as quests em andamento com suas respectivas variáveis `cond` sem erros no cliente.
- [ ] Novos personagens criados recebem o gatilho da janela do Tutorial e sobem de nível com o abate do primeiro monstro e entrega da gema azul.
- [ ] Todas as 18 classes iniciais conseguem realizar a 1ª troca de classe sem interferência de comandos de GM.
- [ ] Todas as ações de inventário, combate e troca operam com validação estrita de pré-requisitos, impedindo ações ilegais ou inconsistências no banco de dados.
- [ ] A suíte de testes automatizados passa com 100% de sucesso em `./mvnw test`.

---
*Documento homologado para execução imediata no projeto L2JLopez.*
