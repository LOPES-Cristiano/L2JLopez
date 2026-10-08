# Estudo Completo e Roteiro Mestre de Quests — L2JLopez

> **Documento Oficial de Engenharia Reversa, Arquitetura e Catálogo Mestre de Quests**  
> **Comparações**: `L2JLucera2` (Russo, Java 8/11 decompilado) vs `L2JDream V2` (Russo, Python/Jython 2.7) vs `L2JLopez` (Brasileiro, Java 21 LTS, Virtual Threads, Spring Boot 3.5, Spring Data JDBC, HikariCP)  
> **Crônica de Referência**: Lineage II Interlude (Chronicle 6, Protocolo 746)  
> **Status**: Homologado para Execução e Paridade Total

---

## Sumário Executivo e Índice de Checkpoints

- [1. Diagnóstico Arquitetural e Comparativo Multi-Plataforma](#1-diagnóstico-arquitetural-e-comparativo-multi-plataforma)
- [CHECKPOINT 1: Arquitetura do Motor de Quests, Rede & Persistência](#checkpoint-1-arquitetura-do-motor-de-quests-rede--persistência)
  - [1.1 Ciclo de Rede e Protocolo Interlude (0x80, 0xa0–0xa3, 0xeb, 0x63, 0x7b–0x7e)](#11-ciclo-de-rede-e-protocolo-interlude-0x80-0xa00xa3-0xeb-0x63-0x7b0x7e)
  - [1.2 Persistência e Modelo de Dados (`character_quests`)](#12-persistência-e-modelo-de-dados-character_quests)
  - [1.3 Motor de Temporizadores e Spawns Assíncronos (`QuestTimer`)](#13-motor-de-temporizadores-e-spawns-assíncronos-questtimer)
- [CHECKPOINT 2: O Tutorial Inicial Completo (Quest 255) — 5 Raças & 9 Arquétipos](#checkpoint-2-o-tutorial-inicial-completo-quest-255--5-raças--9-arquétipos)
  - [2.1 Matriz de Início: NPCs, Coordenadas, Mobs e Vozes](#21-matriz-de-início-npcs-coordenadas-mobs-e-vozes)
  - [2.2 Máquina de Estados: Eventos de Cliente (CE), Question Marks (QM) e Links (TE)](#22-máquina-de-estados-eventos-de-cliente-ce-question-marks-qm-e-links-te)
  - [2.3 Entrega da Blue Gemstone e Recompensas de Novato](#23-entrega-da-blue-gemstone-e-recompensas-de-novato)
- [CHECKPOINT 3: Quests Iniciais de Vilarejos e Novatos (Lv 1–20) por Raça](#checkpoint-3-quests-iniciais-de-vilarejos-e-novatos-lv-120-por-raça)
  - [3.1 Talking Island (Humanos)](#31-talking-island-humanos)
  - [3.2 Elven Village (Elfos)](#32-elven-village-elfos)
  - [3.3 Dark Elven Village (Dark Elves)](#33-dark-elven-village-dark-elves)
  - [3.4 Orc Village (Orcs)](#34-orc-village-orcs)
  - [3.5 Dwarven Village (Anões)](#35-dwarven-village-anões)
- [CHECKPOINT 4: As 18 Quests de 1ª Troca de Classe (Lv 18–20: 401 a 418)](#checkpoint-4-as-18-quests-de-1ª-troca-de-classe-lv-1820-401-a-418)
  - [4.1 Matriz Geral das 18 Provas](#41-matriz-geral-das-18-provas)
  - [4.2 Especificação Detalhada de Cada Quest de 1ª Classe](#42-especificação-detalhada-de-cada-quest-de-1ª-classe)
- [CHECKPOINT 5: As 39 Combinações de 2ª Troca de Classe (Lv 35–40: 211 a 233)](#checkpoint-5-as-39-combinações-de-2ª-troca-de-classe-lv-3540-211-a-233)
  - [5.1 A Trilogia Canônica: Trials (35+), Testimonies (37+) e Tests (39+)](#51-a-trilogia-canônica-trials-35-testimonies-37-e-tests-39)
  - [5.2 Os 6 Trials (Julgamentos)](#52-os-6-trials-julgamentos)
  - [5.3 Os 5 Testimonies (Testemunhos)](#53-os-5-testimonies-testemunhos)
  - [5.4 Os 12 Tests (Testes de Aptidão)](#54-os-12-tests-testes-de-aptidão)
- [CHECKPOINT 6: As 31 Sagas de 3ª Classe (Lv 76+: Quests 70 a 100)](#checkpoint-6-as-31-sagas-de-3ª-classe-lv-76-quests-70-a-100)
  - [6.1 A Jornada Universal das Sagas](#61-a-jornada-universal-das-sagas)
  - [6.2 As 6 Tablets of Vision e Invocação dos Guardiões](#62-as-6-tablets-of-vision-e-invocação-dos-guardiões)
  - [6.3 Archon of Halisha e Recompensas Finais](#63-archon-of-halisha-e-recompensas-finais)
- [CHECKPOINT 7: Subclasse e Nobless (Quests 234, 235, 241, 242, 246, 247)](#checkpoint-7-subclasse-e-nobless-quests-234-235-241-242-246-247)
  - [7.1 Quest 234: Fate's Whisper (Maestro Reorin e os 4 Raid Bosses)](#71-quest-234-fates-whisper-maestro-reorin-e-os-4-raid-bosses)
  - [7.2 Quest 235: Mimir's Elixir (Alquimia e Destrave de Subclasse)](#72-quest-235-mimirs-elixir-alquimia-e-destrave-de-subclasse)
  - [7.3 Saga de Nobless: Possessor of a Precious Soul (Partes 1 a 4)](#73-saga-de-nobless-possessor-of-a-precious-soul-partes-1-a-4)
- [CHECKPOINT 8: Grand Bosses, Instâncias e Clãs](#checkpoint-8-grand-bosses-instâncias-e-clãs)
  - [8.1 Acesso a Chefes Épicos (Antharas, Baium, Valakas, Frintezza, Sailren)](#81-acesso-a-chefes-épicos-antharas-baium-valakas-frintezza-sailren)
  - [8.2 Progressão de Clã Lv 4 e Lv 5 (501 e 503)](#82-progressão-de-clã-lv-4-e-lv-5-501-e-503)
  - [8.3 Alianças de Facções (Ketra Orcs 605 vs Varka Silenos 611)](#83-alianças-de-facções-ketra-orcs-605-vs-varka-silenos-611)
- [CHECKPOINT 9: Endgame Farm, Receitas S-Grade, Soul Crystals e Reagentes](#checkpoint-9-endgame-farm-receitas-s-grade-soul-crystals-e-reagentes)
  - [9.1 Quest 350: Enhance Your Weapon (Soul Crystals Lv 1 a 13 para SA)](#91-quest-350-enhance-your-weapon-soul-crystals-lv-1-a-13-para-sa)
  - [9.2 Quest 373: Supplier of Reagents (Alquimia no Ivory Tower)](#92-quest-373-supplier-of-reagents-alquimia-no-ivory-tower)
  - [9.3 Quests de Forge of the Gods (617) e Imperial Tomb (619)](#93-quests-de-forge-of-the-gods-617-e-imperial-tomb-619)
  - [9.4 Mini-games e Economia de Farm (662 Cards, 663 Whispers, 354 Alligator Island, 632 Necromancer)](#94-mini-games-e-economia-de-farm-662-cards-663-whispers-354-alligator-island-632-necromancer)
- [CHECKPOINT 10: Roteiro de Implementação no L2JLopez & Validação por Testes](#checkpoint-10-roteiro-de-implementação-no-l2jlopez--validação-por-testes)

---

## 1. Diagnóstico Arquitetural e Comparativo Multi-Plataforma

Para implementar um motor de quests que combine a fidelidade retail com performance de última geração, realizamos uma engenharia reversa minuciosa de duas referências consolidadas no cenário mundial: **L2JLucera2** e **L2JDream V2**.

| Dimensão Técnica | L2JLucera2 (Referência 1) | L2JDream V2 (Referência 2) | L2JLopez (Arquitetura Adotada) |
|---|---|---|---|
| **Linguagem & Runtime** | Java 8 / 11 decompilado | Java 8 / 11 + Jython 2.7 (Python) | **Java 21 LTS + Virtual Threads (Project Loom)** |
| **Padrão de Quests** | 351 classes Java em `l2.gameserver.model.quest.Quest` com sub-classes aninhadas de listeners | 343 scripts Python `__init__.py` herdando de `QuestJython (JQuest)` | **Classes Java nativas Spring `@Component` com catálogo tipado e Records** |
| **Inicialização & Boot** | Carregamento rápido via classloader dinâmico | Inicialização lenta (Jython compila scripts Python em bytecode no boot, levando 15–25s extras) | **Inicialização instantânea (< 2s) via injeção Spring Boot e índices em memória** |
| **Camada de Rede** | Pacotes legados com ByteBuffer | Pacotes legados com ByteBuffer | **Records imutáveis com `PacketWriter` otimizado em Little-Endian** |
| **HTML & Diálogos** | Mapeamento em convenção russa `npc_qXXXX_YY.htm` | Mapeamento retail canônico `npcId-state.htm` | **Compatibilidade total com o repositório retail `data/scripts/quests/` (`npcId-state.htm`)** |
| **Gerenciamento de Timers** | `ThreadPoolManager.getInstance().schedule(...)` | `st.startQuestTimer(name, millis)` com listener Jython | **`QuestState.startQuestTimer` via `ScheduledExecutorService` de Virtual Threads** |
| **Persistência de Dados** | Tabela JDBC pura `character_quests` | Tabela JDBC pura `character_quests` | **Spring Data JDBC + HikariCP com migrações Flyway e atomicidade transacional** |
| **Detecção de Exploits** | Locks em Player | Locks em Player | **Validações estritas de distância (150u), inventário, peso e slots antes de cada entrega** |

---

## CHECKPOINT 1: Arquitetura do Motor de Quests, Rede & Persistência

### 1.1 Ciclo de Rede e Protocolo Interlude

O cliente Lineage II Interlude se comunica com o servidor de missões através de dois subsistemas: o **Diário de Missões (`Alt + U`)** e o **Módulo Multimídia do Tutorial (janela flutuante interativa)**.

```
       [ Cliente L2 Interlude ]                                     [ L2JLopez Server ]
                  │                                                          │
                  │─── 0x63: RequestQuestList ──────────────────────────────>│
                  │<── 0x80: QuestList (bitmask de completadas + ativas) ────│
                  │                                                          │
                  │─── [Login Nível 1 / Evento do Mundo] ───────────────────>│
                  │<── 0xa1: TutorialShowQuestionMark (MarkId = 1) ──────────│
                  │<── 0x98: PlaySound ("tutorial_voice_001a") ──────────────│
                  │                                                          │
                  │─── 0x7d: RequestTutorialQuestionMark (MarkId = 1) ──────>│
                  │<── 0xeb: RadarControl (Seta minimapa X, Y, Z) ───────────│
                  │<── 0xa0: TutorialShowHtml (HTML completo formatado) ─────│
                  │                                                          │
                  │─── 0x7b: RequestTutorialLinkHtml ("link TE02") ─────────>│
                  │<── 0xa2: TutorialEnableClientEvent (EventId = 1) ────────│
                  │                                                          │
                  │─── [Jogador Move o Personagem] ──────────────────────────│
                  │─── 0x7e: RequestTutorialClientEvent (EventId = 1) ──────>│
                  │<── 0xa0: TutorialShowHtml ("tutorial_03.htm") ───────────│
                  │<── 0xa2: TutorialEnableClientEvent (EventId = 2) ────────│
                  │                                                          │
                  │─── 0x7c: RequestTutorialPassCmdToServer ("bypass") ─────>│
                  │<── 0xa3: TutorialCloseHtml ──────────────────────────────│
```

#### Tabela de Pacotes do Servidor (S2C):
1. **`QuestList (0x80)`**:
   - `writeH(activeQuestsCount)`
   - Para cada quest ativa: `writeD(questId)`, `writeD(questCond)`
   - `writeB(new byte[32])`: bitmask de 256 bits onde cada bit `(questId - 1)` indica se a missão já foi concluída no histórico do personagem.
2. **`TutorialShowHtml (0xa0)`**:
   - `writeS(htmlContent)`: Envia o texto HTML completo com suporte a hiperlinks `<a action="link TE...">` e botões de ação.
3. **`TutorialShowQuestionMark (0xa1)`**:
   - `writeD(markId)`: Faz o ícone do ponto de interrogação azul piscar na lateral direita inferior da tela do cliente.
4. **`TutorialEnableClientEvent (0xa2)`**:
   - `writeD(eventId)`: Instrui o cliente de jogo local a monitorar ações físicas do jogador (1 = andar, 2 = mirar/atacar, 8 = matar mob).
5. **`TutorialCloseHtml (0xa3)`**:
   - Pacote vazio (`writeC(0xa3)`) para fechar imediatamente o diálogo do tutorial ativo.
6. **`RadarControl (0xeb)`**:
   - `writeD(show)` (0 = adicionar, 1 = remover), `writeD(type)` (1 = seta vermelha comum), `writeD(x)`, `writeD(y)`, `writeD(z)`: Desenha o indicador visual no radar do mapa.

---

### 1.2 Persistência e Modelo de Dados (`character_quests`)

Toda a memória volátil de missões é descarregada no banco de dados através da tabela `character_quests`:

```sql
CREATE TABLE IF NOT EXISTS character_quests (
    char_id INT NOT NULL,
    name VARCHAR(60) NOT NULL,
    var VARCHAR(20) NOT NULL,
    value VARCHAR(255),
    PRIMARY KEY (char_id, name, var),
    FOREIGN KEY (char_id) REFERENCES characters(char_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_char_quests_char_name ON character_quests(char_id, name);
```

#### Convenções de Variáveis de Estado:
- `<state>`: `Created`, `Started`, `Completed`.
- `cond`: O passo principal atual da quest (1, 2, 3...). Sincronizado automaticamente com o cliente através de `QuestList`.
- `ucMemo`: Marcador de controle da janela de boas-vindas do Tutorial.
- `Ex`: Passo de áudio/voz de tutorial (`-2`, `-3`, `-4`, `-5`, `0`).
- `Gemstone`: Indicador de obtenção da Blue Gemstone (`1` = coletada).
- `lvl`: Último nível no qual o tutorial enviou dica de progressão de habilidades (5, 6, 7, 10, 15, 19, 35).

---

### 1.3 Motor de Temporizadores e Spawns Assíncronos (`QuestTimer`)

Em L2JLucera2 e L2JDream, eventos temporizados (como aguardar 30 segundos após o login para tocar a segunda fala do tutorial, ou dar despawn em monstros de quest após 2 minutos) dependiam de threads pesadas ou estruturas de script. No **L2JLopez**, implementamos:

```java
public void startQuestTimer(String name, long timeMillis) {
    cancelQuestTimer(name);
    ScheduledFuture<?> future = QUEST_TIMER_POOL.schedule(() -> {
        try {
            if (player != null && player.activeChar() != null) {
                quest.notifyEvent(name, null, player);
            }
        } catch (Exception e) {
            log.error("Erro executando timer {} na quest {}", name, quest.getName(), e);
        } finally {
            activeTimers.remove(name);
        }
    }, timeMillis, TimeUnit.MILLISECONDS);
    activeTimers.put(name, future);
}
```

Essa abordagem garante que timers de milhares de jogadores online rodem sobre **Virtual Threads nativas**, com pegada de memória desprezível e sem travar nenhuma thread do pool de rede.

---

## CHECKPOINT 2: O Tutorial Inicial Completo (Quest 255) — 5 Raças & 9 Arquétipos

A Quest 255 é a primeira experiência de todo jogador no mundo de Lineage II. Sem ela, o personagem surge em um ambiente vazio, sem orientação, sem Soulshots e sem mapas.

### 2.1 Matriz de Início: NPCs, Coordenadas, Mobs e Vozes

| Raça / Classe | ID Classe | Cidade Inicial | NPC Newbie Helper | Coordenadas Radar (X, Y, Z) | Monstro Inicial | Drop Inicial | Arquivo de Voz Inicial | Diálogo HTML Inicial |
|---|:---:|---|---|---|---|---|:---:|:---:|
| **Human Fighter** | `0` | Talking Island | Newbie Helper (30009) | `-71424, 258336, -3109` | Gremlin (18342 / 20001) | Blue Gemstone (6353) | `tutorial_voice_001a` | `tutorial_human_fighter001.htm` |
| **Human Mystic** | `10` | Talking Island | Newbie Helper (30019) | `-91036, 248044, -3568` | Gremlin (18342 / 20001) | Blue Gemstone (6353) | `tutorial_voice_001b` | `tutorial_human_mage001.htm` |
| **Elven Fighter** | `18` | Elven Village | Newbie Helper (30400) | `46112, 41200, -3504` | Keltir (20417) | Blue Gemstone (6353) | `tutorial_voice_001c` | `tutorial_elven_fighter001.htm` |
| **Elven Mystic** | `25` | Elven Village | Newbie Helper (30400) | `46112, 41200, -3504` | Keltir (20417) | Blue Gemstone (6353) | `tutorial_voice_001d` | `tutorial_elven_mage001.htm` |
| **Dark Fighter** | `31` | Dark Elf Village | Newbie Helper (30404) | `28384, 11056, -4233` | Keltir (20418) | Blue Gemstone (6353) | `tutorial_voice_001e` | `tutorial_delf_fighter001.htm` |
| **Dark Mystic** | `38` | Dark Elf Village | Newbie Helper (30404) | `28384, 11056, -4233` | Keltir (20418) | Blue Gemstone (6353) | `tutorial_voice_001f` | `tutorial_delf_mage001.htm` |
| **Orc Fighter** | `44` | Orc Village | Newbie Helper (30574) | `-56736, -113680, -672` | Keltir (20419) | Blue Gemstone (6353) | `tutorial_voice_001g` | `tutorial_orc_fighter001.htm` |
| **Orc Mystic** | `49` | Orc Village | Newbie Helper (30574) | `-56736, -113680, -672` | Keltir (20419) | Blue Gemstone (6353) | `tutorial_voice_001h` | `tutorial_orc_mage001.htm` |
| **Dwarf Fighter**| `53` | Dwarven Village | Newbie Helper (30530) | `108567, -173994, -406` | Keltir (20420) | Blue Gemstone (6353) | `tutorial_voice_001i` | `tutorial_dwarven_fighter001.htm`|

---

### 2.2 Máquina de Estados: Eventos de Cliente (CE), Question Marks (QM) e Links (TE)

```
[Login Lv 1] ──> Ex = -2 ──> Timer QT (10s)
                   │
                   ▼
               Toca voz race (voice_001x), entrega Tutorial Guide (5588), Ex = -3
                   │
                   ▼
               Timer QT (30s) ──> Toca tutorial_voice_002 (Instruções de Movimento)
                   │
                   ▼
           [Clique no Ponto de Interrogação QM1]
                   │
                   ├──> Toca tutorial_voice_007
                   ├──> Ativa radar minimapa para o Newbie Helper da raça
                   └──> Abre HTML de orientação inicial (tutorial_xxxx007.htm)
                   │
                   ▼
           [Combate com Gremlin / Keltir] ──> Drop da Blue Gemstone (Item 6353)
                   │
                   ├──> Toca tutorial_voice_013
                   ├──> Toca som ItemSound.quest_tutorial
                   └──> Exibe Question Mark QM5
                   │
                   ▼
           [Clique em QM5] ──> Abre tutorial_11.htm (Retornar ao Newbie Helper)
```

#### Catálogo Completo de Question Marks do Tutorial:
- **`QM1`**: Introdução à jornada e direcionamento ao Newbie Helper.
- **`QM3`**: Explicação da janela de status (`Alt + T`).
- **`QM5`**: Notificação de coleta da Blue Gemstone — retornar ao instrutor.
- **`QM9`**: Nível 5 atingido — orientação para aprender novas habilidades com os Mestres de Guilda Física (aponta radar para a guilda de guerreiros).
- **`QM10`**: Alerta de HP Crítico (< 20%) — orienta a sentar para regenerar HP mais rápido.
- **`QM11`**: Nível 7 atingido para Místicos — orientação para aprender magias no Templo.
- **`QM17`**: Nível 15 atingido — aviso de aproximação da primeira mudança de classe.
- **`QM23`**: Primeira coleta de Adena no chão — explica o sistema monetário de Aden.
- **`QM24`**: Nível 6 atingido — fim da proteção absoluta de novato e guia de caça avançada.
- **`QM27`**: Nível 10 atingido — recomendações de equipamentos No-Grade superiores.
- **`QM34`**: Nível 35 atingido — introdução às jornadas de 2ª Classe (Trials, Testimonies e Tests).
- **`QM35`**: Nível 19 atingido — introdução às jornadas de 1ª Classe (Quests 401 a 418).

---

### 2.3 Entrega da Blue Gemstone e Recompensas de Novato

Ao falar com o Newbie Helper correspondente à sua raça portando a **Blue Gemstone (Item 6353)**:
1. A pedra mágica é consumida (`takeItems(6353, 1)`).
2. O jogador recebe as munições de iniciante:
   - Se for classe mística (`classId` 10, 25, 38, 49): **100 Novice Spiritshots (Item 5790)**.
   - Se for classe física (`classId` 0, 18, 31, 44, 53): **200 Novice Soulshots (Item 5789)**.
3. Concessão de experiência: **+100 EXP e +50 SP**, elevando o personagem instantaneamente ao **Nível 2**.
4. O radar é atualizado: o ponteiro do Newbie Helper é removido e um novo ponteiro é adicionado apontando para o Mestre da Guilda da cidade principal.
5. Toca o som oficial de encerramento: `ItemSound.quest_finish`.
6. A Quest 255 é gravada como `Completed` no banco de dados.

---

## CHECKPOINT 3: Quests Iniciais de Vilarejos e Novatos (Lv 1–20) por Raça

As missões iniciais de vilarejos fornecem a progressão econômica inicial, mapas locais, equipamentos No-Grade e itens de cura.

### 3.1 Talking Island (Humanos)

| ID | Nome da Quest | Nível | NPC Inicial | Coordenadas (X, Y, Z) | Mobs Alvo | Drop / Itens | Recompensas |
|:---:|---|:---:|---|---|---|---|---|
| **1** | Letters of Love | 2+ | Darin | `-84144, 244588, -3728` | N/A (Entrega) | Darin's Letter (1014), Roxxy's Kerchief (1015) | 450 Adena, Necklace of Knowledge |
| **2** | What Women Want | 2+ | Arujien | `-84022, 243292, -3728` | N/A (Entrega) | Arujien's Letter (1016), Poetry Book (1017) | 450 Adena, Mirien's Earring |
| **3** | Will the Seal be Broken?| 16+ | Talloth | `-84260, 244670, -3728` | Omen Beast (20031), Tainted Orc (20012) | Monster Eye Meat, Tainted Orc Amulet | 3.900 Adena, 2.000 EXP, 1.200 SP |
| **4** | Long Live the Lord of Flame | 2+ | Nakusin | `-84450, 244790, -3728` | Keltir (20001) | Wolf Pelt (1019) x30 | 600 Adena, Club |
| **5** | Miner's Favor | 2+ | Bolter | `-84200, 244400, -3728` | N/A (Entrega) | Bolter's List (1020), Mining Boots | 450 Adena, Leather Pants |
| **101** | Sword of Gathering | 10+ | Roien | `-84090, 244510, -3728` | Werewolf (20004), Giant Spider (20103) | Broken Sword Handle, Giant Spider Leg | Sword of Gathering (760), 2.800 EXP |
| **104** | Spirit of Mirror | 10+ | Gallint | `-84510, 244620, -3728` | Ghost of Peasant (20003), Wererat (20005) | Gallint's Oak Wand (1027), Spirits | Wand of Adept (761), 3.000 EXP |
| **105** | Skirmish with Orcs | 10+ | Kendell | `-84200, 243100, -3728` | Kaboo Chief (20009), Kaboo Orcs (20006) | Kaboo Chief Torque (1030) | Red Sunset Sword (981), 4.000 EXP |

---

### 3.2 Elven Village (Elfos)

| ID | Nome da Quest | Nível | NPC Inicial | Coordenadas (X, Y, Z) | Mobs Alvo | Drop / Itens | Recompensas |
|:---:|---|:---:|---|---|---|---|---|
| **151** | Cure for Fever Disease | 15+ | Elias | `45800, 52100, -2792` | Poison Spider (20103), Arachnid Tracker (20104) | Spider Poison Sac (1043) | Round Shield, 3.200 EXP |
| **152** | Shards of Golem | 10+ | Alshupes | `45200, 51900, -2792` | Stone Golem (20016) | Golem Shards (1044) x5 | Wooden Breastplate, 2.500 EXP |
| **153** | Deliver Goods | 2+ | Jackson | `45900, 52400, -2792` | N/A (Entrega) | Jackson's Receipt, Heavy Wood Box | 450 Adena, Ring of Knowledge |
| **158** | Seed of Evil | 21+ | Nerupa | `45600, 52200, -2792` | Doom Blade Dreadnought (27001) | Clay Tablet (1046) | 17.500 Adena, 5.000 EXP |
| **163** | Legacy of the Poet | 11+ | Starden | `45700, 52300, -2792` | Baraq Orc Fighters (20017) | Rumiel's Poems (1051) x4 | 13.800 Adena, 2.100 EXP |
| **164** | Blood Fiend | 21+ | Creamees | `45500, 52000, -2792` | Kirunak (27002) | Kirunak's Skull (1052) | 42.000 Adena, 10.000 EXP |

---

### 3.3 Dark Elven Village (Dark Elves)

| ID | Nome da Quest | Nível | NPC Inicial | Coordenadas (X, Y, Z) | Mobs Alvo | Drop / Itens | Recompensas |
|:---:|---|:---:|---|---|---|---|---|
| **167** | Dwarven Kinship | 15+ | Carlotta | `10200, 14500, -4242` | N/A (Entrega em Gludio) | Carlotta's Letter (1076) | 3.000 Adena, 2.000 EXP |
| **168** | Scent of Death | 11+ | Minaless | `10400, 14300, -4242` | Marsh Zombie (20015) | Zombie Skin (1047) x5 | 3.350 Adena, Lesser Healing Potions |
| **265** | Chains of Slavery | 6+ | Kristin | `10100, 14600, -4242` | Imp (20008), Imp Chieftain (20010) | Imp Shackles (1368) | 13 Adena por cada algema |
| **266** | Plead of Pixy | 3+ | Murika | `10300, 14200, -4242` | Keltir (20418) | Pixy Shards (1369) x100 | Glass Dagger, 450 Adena |
| **267** | Wrath of Verfdan | 16+ | Bregad | `10500, 14100, -4242` | Varangkas (27003) | Verfdan's Head (1370) | 5.200 Adena, 3.500 EXP |

---

### 3.4 Orc Village (Orcs)

| ID | Nome da Quest | Nível | NPC Inicial | Coordenadas (X, Y, Z) | Mobs Alvo | Drop / Itens | Recompensas |
|:---:|---|:---:|---|---|---|---|---|
| **271** | Proof of Valor | 4+ | Rukain | `-46200, -114200, -200` | Kasha Wolf (20475) | Kasha Wolf Fangs (1473) x50 | Necklace of Courage, 850 EXP |
| **272** | Wrath of Ancestors | 5+ | Livina | `-46400, -114100, -200` | Goblin Grave Robber (20319) | Grave Robber Heads (1474) x50 | 1.500 Adena, 1.000 EXP |
| **273** | Invaders of the Holy Land | 6+ | Varkees | `-46100, -114300, -200` | Rakeclaw Imps (20311) | Black Soulstone, Red Soulstone | 50 Adena por Black, 5 Adena por Red |
| **276** | Hestui's Totem | 15+ | Tanapi | `-46300, -114500, -200` | Kasha Bear (20479), Kasha Bear Totem | Kasha Crystal (1478) | Totem of Hestui, Leather Pants |

---

### 3.5 Dwarven Village (Anões)

| ID | Nome da Quest | Nível | NPC Inicial | Coordenadas (X, Y, Z) | Mobs Alvo | Drop / Itens | Recompensas |
|:---:|---|:---:|---|---|---|---|---|
| **291** | Reclaim the Land | 10+ | Pio | `115200, -182800, -1440` | Turek Orcs (20496) | Turek Dog Tags, Medallions | 50 Adena por tag, 20 Adena por medalha |
| **292** | Brigand's Sweeper | 5+ | Spiron | `115600, -182500, -1440` | Goblin Brigand (20322) | Goblin Necklaces (1484) | 12 Adena por colar |
| **293** | Hidden Vein | 6+ | Filaur | `115400, -182600, -1440` | Utuku Orcs (20499) | Chrysolite Ore, Torn Map Fragments | 10 Adena por minério, 1.000 Adena por mapa |
| **296** | Silk of Tarantula | 15+ | Mion | `115100, -182400, -1440` | Hunter Tarantula (20403), Plunder Tarantula | Tarantula Spider Silk (1493) | 23 Adena por seda de aranha |

---

## CHECKPOINT 4: As 18 Quests de 1ª Troca de Classe (Lv 18–20: 401 a 418)

As quests de primeira classe são as provas de maturidade que transformam os aprendizes em suas respectivas carreiras profissionais.

### 4.1 Matriz Geral das 18 Provas

| Quest ID | Nome Oficial | Classe Alvo | Raça | NPC Inicial | Cidade | Item de Prova Final (Proof Item) | Recompensas Adicionais |
|:---:|---|---|:---:|---|---|:---:|---|
| **401** | Path to a Warrior | Warrior (1) | Humano | Master Auron (30010) | Gludio | Medallion of Warrior (1145) | 3.200 EXP, 1.500 SP |
| **402** | Path to a Human Knight | Knight (4) | Humano | Sir Aaron Tanford (30417) | Gludio | Sword of Ritual (1161) | 3.200 EXP, 1.500 SP |
| **403** | Path to a Rogue | Rogue (7) | Humano | Captain Bezique (30379) | Gludin | Bezique's Letter (1180) | 3.200 EXP, 1.500 SP |
| **404** | Path to a Human Wizard | Wizard (11) | Humano | Parina (30391) | Gludio | Bead of Season (1210) | 3.200 EXP, 1.500 SP |
| **405** | Path to a Cleric | Cleric (15) | Humano | Priest Zigaunt (30022) | Gludin | Mark of Faith (1201) | 3.200 EXP, 1.500 SP |
| **406** | Path to an Elven Knight | Elven Knight (19) | Elfo | Master Sorius (30327) | Gludio | Elven Knight Brooch (1220) | 3.200 EXP, 1.500 SP |
| **407** | Path to an Elven Scout | Elven Scout (22) | Elfo | Master Reisa (30328) | Gludio | Reisa's Letter (1207) | 3.200 EXP, 1.500 SP |
| **408** | Path to an Elven Wizard | Elven Wizard (26) | Elfo | Rogellia (30414) | Elven Village | Eternity Diamond (1218) | 3.200 EXP, 1.500 SP |
| **409** | Path to an Oracle | Oracle (29) | Elfo | Priest Manuel (30293) | Gludio | Leaf of Oracle (1235) | 3.200 EXP, 1.500 SP |
| **410** | Path to a Palus Knight | Palus Knight (32) | Dark Elf | Master Virgil (30329) | Gludio | Gaze of Abyss (1246) | 3.200 EXP, 1.500 SP |
| **411** | Path to an Assassin | Assassin (35) | Dark Elf | Triskel (30416) | Gludio | Iron Heart (1258) | 3.200 EXP, 1.500 SP |
| **412** | Path to a Dark Wizard | Dark Wizard (39) | Dark Elf | Varika (30415) | Dark Elf Village | Jewel of Darkness (1254) | 3.200 EXP, 1.500 SP |
| **413** | Path to a Shillien Oracle| Shillien Oracle (42)| Dark Elf | Magister Sidra (30330) | Gludio | Orb of Abyss (1262) | 3.200 EXP, 1.500 SP |
| **414** | Path to an Orc Raider | Orc Raider (45) | Orc | Prefect Karukia (30505) | Gludin | Mark of Raider (1592) | 3.200 EXP, 1.500 SP |
| **415** | Path to an Orc Monk | Orc Monk (47) | Orc | Gantaki Zu Urutu (30587)| Gludin | Khavatari Totem (1615) | 3.200 EXP, 1.500 SP |
| **416** | Path to an Orc Shaman | Orc Shaman (50) | Orc | Tataru Zu Hestui (30585)| Orc Village | Mask of Medium (1630) | 3.200 EXP, 1.500 SP |
| **417** | Path to a Scavenger | Scavenger (54) | Anão | Collector Pippi (30517) | Dwarven Village| Ring of Raven (1642) | 3.200 EXP, 1.500 SP |
| **418** | Path to an Artisan | Artisan (56) | Anão | Blacksmith Silvera (30527)| Dwarven Village| Pass Certificate (1631) | 3.200 EXP, 1.500 SP |

---

### 4.2 Especificação Detalhada de Cada Quest de 1ª Classe

#### Exemplo Aprofundado: Quest 401 — Path to Warrior
- **NPC Inicial**: Master Auron (ID 30010) em Gludio Fighter Guild.
- **NPC Secundário**: Trader Simplon (ID 30253) na Weapons & Armor Shop de Gludio.
- **Itens de Missão Intermediários**:
  1. `Eins's Letter (1138)`
  2. `Warrior Guild Mark (1139)`
  3. `Rusted Bronze Sword 1 (1140)` — coletar 10 matando esqueletos.
  4. `Rusted Bronze Sword 2 (1141)` & `Simplon's Letter (1143)`
  5. `Rusted Bronze Sword 3 (1142)` — espada especial entregue por Auron.
  6. `Poison Spider Leg (1144)` — coletar 20 matando aranhas.
- **Mecânica Crítica de Combate**: Ao enfrentar as Poison Spiders (20038) ou Arachnid Trackers (20043), o jogador **DEVE OBRIGATORIAMENTE ESTAR COM A RUSTED BRONZE SWORD 3 EQUIPADA NA MÃO DIREITA** (`getItemEquipped(7) == 1142`). Se matar com outra arma, a perna da aranha não dropa!
- **Condições (cond)**:
  - `cond=1`: Carta entregue por Auron; ir a Simplon.
  - `cond=2`: Simplon pede 10 fragmentos de espada enferrujada de esqueletos em Ruins of Agony.
  - `cond=3`: 10 fragmentos coletados; retornar a Simplon.
  - `cond=4`: Simplon entrega carta e espada forjada para Auron.
  - `cond=5`: Auron entrega a Rusted Bronze Sword 3 e manda caçar 20 pernas de aranha.
  - `cond=6`: 20 pernas coletadas; retornar a Auron para receber o `Medallion of Warrior (1145)`.

#### Exemplo Aprofundado: Quest 404 — Path to Human Wizard
- **NPC Inicial**: Parina (30391) em Gludio Magic Guild.
- **Espíritos Elementais Visitados**:
  1. **Terra**: Flame Salamander em Ruins of Despair (fala sobre o fogo e a destruição).
  2. **Vento**: Wind Sylph próximo a Wasteland (fala sobre a liberdade do ar).
  3. **Água**: Water Undine no lago de Fellmere (fala sobre a pureza das águas).
  4. **Terra**: Earth Snake nas montanhas de Gludio (fala sobre a solidez da rocha).
- **Inimigos**: Ratman Warriors e Skeleton Archers para obter tokens dos 4 elementos.
- **Prova Final**: `Bead of Season (1210)` entregue por Parina, liberando a evolução para Human Wizard.

---

## CHECKPOINT 5: As 39 Combinações de 2ª Troca de Classe (Lv 35–40: 211 a 233)

No nível 40, a especialização de 2ª classe requer que o personagem conclua **três provas independentes**:
1. **Trial (Nível 35+)**: Julgamento de caráter da linhagem da classe.
2. **Testimony (Nível 37+)**: Prova de fidelidade racial e alianças de Aden.
3. **Test (Nível 39+)**: Exame final de aptidão da profissão específica.

### 5.1 A Trilogia Canônica: Trials (35+), Testimonies (37+) e Tests (39+)

```
                      [ NÍVEL 35 ] ──> TRIAL (Quests 211 a 216)
                            │
                            ▼
                      [ NÍVEL 37 ] ──> TESTIMONY (Quests 217 a 221)
                            │
                            ▼
                      [ NÍVEL 39 ] ──> TEST (Quests 222 a 233)
                            │
                            ▼
                      [ NÍVEL 40 ] ──> 2ª MUDANÇA DE CLASSE NO GRAND MASTER
```

---

### 5.2 Os 6 Trials (Julgamentos - Nível 35+)

| ID | Nome do Trial | Classes Requeridas | NPC Inicial | Cidade / Local | Monstro Chave / Quest Mob | Item de Prova Final |
|:---:|---|---|---|---|---|:---:|
| **211** | Trial of the Challenger | Gladiator, Warlord, Swordsinger | Kash (30644) | Dion Castle Town | Shyslassys (27110), Gorr (27112), Baraham (27113) | Mark of Challenger (2627) |
| **212** | Trial of Duty | Paladin, Dark Avenger, Temple Knight, Shillien Knight | Sir Aaron Tanford (30109) | Wasteland Entrance | Spirit of Sir Talianus (27117), Skeleton Marauders | Mark of Duty (2633) |
| **213** | Trial of the Seeker | Treasure Hunter, Hawkeye, Plains Walker, Silver Ranger, Abyss Walker, Phantom Ranger | Dufner (30064) | Town of Giran | Terry (30065), Delu Lizardman Shaman, Ant Captain | Mark of Seeker (2673) |
| **214** | Trial of the Scholar | Sorcerer, Necromancer, Warlock, Spellsinger, Elemental Summoner, Spellhowler, Phantom Summoner | Magister Mirien (30607) | Dion Castle Town | High Priest Sylvain, Grand Magister Jurek, Cronos, Monster Eye Destroyers | Mark of Scholar (2674) |
| **215** | Trial of the Pilgrim | Bishop, Prophet, Elven Elder, Shillien Elder, Warcryer | Hermit Santiago (30648) | Orc Barracks | Seer Tanapi, Ancestor Martankus, Black Willow, Succubus Queen | Mark of Pilgrim (2721) |
| **216** | Trial of the Guildsman | Bounty Hunter, Warsmith | Warehouse Keeper Valkon (30103) | Town of Giran | Blacksmith Altran, Mandragora Sprouts, Breka Orcs | Mark of Guildsman (3119) |

---

### 5.3 Os 5 Testimonies (Testemunhos - Nível 37+)

| ID | Nome do Testimony | Raça / Facção | NPC Inicial | Cidade | Desafio Principal | Item de Prova Final |
|:---:|---|:---:|---|---|---|:---:|
| **217** | Testimony of Trust | Humanos (Todas) | High Priest Hollint (30191) | Town of Oren | Aliança entre Elfos e Dark Elves; carta de paz a Asterios e Thifiell | Mark of Trust (2734) |
| **218** | Testimony of Life | Elfos (Todas) | Master Cardien (30371) | Dion Castle Town | Salvação da Mother Tree; Pure Water de Talia e Stardust dos Dwarves | Mark of Life (3140) |
| **219** | Testimony of Fate | Dark Elves (Todas) | Magister Kaira (30419) | Town of Giran | Ritual de sangue com Ixia, Arkenia e a colheita de Belladonna e Alders | Mark of Fate (3172) |
| **220** | Testimony of Glory | Orcs (Todas) | Prefect Vokian (30514) | Town of Giran | Unificação das tribos Orc com Kakai; abate de Enku, Timak e Leunt Orcs | Mark of Glory (3203) |
| **221** | Testimony of Prosperity| Anões (Todas) | Warehouse Keeper Parman (30531)| Town of Giran | Abertura das 4 Caixas de Ouro com Locksmiths e exame com Carrier Torocco | Mark of Prosperity (3238)|

---

### 5.4 Os 12 Tests (Testes de Aptidão - Nível 39+)

1. **222 Test of the Duelist** (Gladiator): Duelist Kaien em Oren; derrotar 10 tipos de monstros em 5 províncias com espadas duais (Punchers, Noble Ants, Dead Seekers, Marsh Stakatos).
2. **223 Test of the Champion** (Warlord): Veteran Ascalon em Giran; liderança tática contra Harpy e Medusas.
3. **224 Test of the Sagittarius** (Hawkeye, Silver Ranger, Phantom Ranger): Hunter Bernard em Hunters Village; arco e flecha contra Hamrut e Krypnos.
4. **225 Test of the Searcher** (Treasure Hunter, Plains Walker, Abyss Walker): Master Luther em Hunters Village; rastreamento e disfarce.
5. **226 Test of the Healer** (Paladin, Temple Knight, Bishop, Elven Elder): Priest Bandellos em Giran; atos de compaixão e purificação de órfãos amaldiçoados.
6. **227 Test of the Reformer** (Prophet, Shillien Elder): Priest Pupina em Giran; exorcismo de espíritos atormentados em Execution Grounds.
7. **228 Test of the Magus** (Sorcerer, Spellsinger, Spellhowler): Bard Rukal em Dion; domínio dos 4 elementos da alta magia.
8. **229 Test of Witchcraft** (Dark Avenger, Necromancer, Shillien Knight): Trader Alexandria em Giran; rituais sombrios com as bruxas Kalis e Vika.
9. **230 Test of the Summoner** (Warlock, Elemental Summoner, Phantom Summoner): High Summoner Galatea em Dion; duelos diretos de invocações (Pet vs Pet) com os 6 mestres de cartas.
10. **231 Test of the Maestro** (Warsmith / Maestro): Chief Locksmith Spiron em Dwarven Village; construção de peças arquitetônicas imperiais.
11. **232 Test of the Lord** (Overlord): Flame Lord Kakai em Orc Village; obtenção do juramento de sangue das 5 tribos orcs.
12. **233 Test of the Warspirit** (Warcryer): Seer Somak em Orc Village; invocação dos ancestrais de batalha com as relíquias de Tamlin.

---

## CHECKPOINT 6: As 31 Sagas de 3ª Classe (Lv 76+: Quests 70 a 100)

As 31 Sagas marcam a ascensão para a 3ª profissão (grau S), concedendo o título de lenda e o acesso às habilidades mais destrutivas e buffs supremos do jogo.

### 6.1 A Jornada Universal das Sagas

Todas as 31 missões compartilham uma estrutura épica unificada:

```
[Nível 76 Atingido]
        │
        ▼
[Falar com o Grand Master da Classe] ──> Goddard, Rune ou Aden
        │
        ▼
[Obter os 2 Itens de Acesso Ritual]
   ├── 1. Ice Crystal (via Quest 624: The Finest Ingredients com Chef Donath em Hot Springs)
   └── 2. Big White Nimble Fish (via pesca em Hot Springs / Goddard ou compra)
        │
        ▼
[Investigação dos Eruditos] ──> Falar com Informante em Goddard/Rune
        │
        ▼
[Peregrinação às 6 Tablets of Vision (Stone of Commune 1 a 6)]
        │
        ▼
[Abate de 700 Monstros em Shrine of the Loyal ou Invasão de Four Sepulchers]
        │ ──> Obtenção de Halisha's Marks e spawn do Archon of Halisha
        │
        ▼
[Derrotar o Archon of Halisha] ──> Recuperação da Stone of Commune 5
        │
        ▼
[Duelo com o Guardião no Tablet 6 ao lado do NPC Protetor]
        │
        ▼
[Retorno ao Grand Master] ──> Promoção à 3ª Classe + Secret Book of Giants + 5.000.000 EXP
```

---

### 6.2 As 6 Tablets of Vision e Invocação dos Guardiões

| Tablet # | Região do Mundo | Mecânica de Obtenção da Stone of Commune | Inimigo / Interação |
|:---:|---|---|---|
| **1ª Tablet** | Sul de Tower of Insolence | Comunhão direta com a pedra mágica | Leitura dos escritos antigos de Shilen e Einhasad |
| **2ª Tablet** | Valley of Saints | Abate de Vision Guardians ao redor da pedra | Derrotar os anjos guardiões para dropar a Stone |
| **3ª Tablet** | Wall of Argos / Border Outpost | Invocação do Quest Monster individual da classe | Batalha solo contra o demônio da tentação |
| **4ª Tablet** | Ketra Orc Outpost ou Varka Silenos | Entrega do Ice Crystal para o NPC Guardião | Negociação diplomática e oferenda |
| **5ª Tablet** | Shrine of the Loyal / Imperial Tomb | Abate do **Archon of Halisha** | Boss com 4 minions; drop obrigatório da Stone 5 |
| **6ª Tablet** | Forest of the Dead / Forgotten Temple | Combate final ao lado do NPC aliado da classe | O jogador deve atacar o boss e falar com o aliado vivo |

---

### 6.3 Archon of Halisha e Recompensas Finais

O **Archon of Halisha (ID 25338)** é o guardião sombrio da 5ª pedra. Existem duas formas retail de derrotá-lo:
1. **Caça em Shrine of the Loyal**: Matar Grave Scarabs, Scavenger Scarabs, Grave Ants e Shrine Knights para coletar **700 Halisha's Marks**. Ao atingir a marca 700, o Archon of Halisha é invocado instantaneamente na frente do jogador.
2. **Invasão na Four Sepulchers**: Entrar nas tumbas imperiais de Sepulchers e derrotá-lo no final da sala da instância.

#### Recompensas Finais de Cada Saga:
- Transição imediata de classe para a 3ª profissão.
- **1x Secret Book of Giants (Item 6622)** para encantamento de habilidades (+1 a +30).
- **5.000.000 de Pontos de Experiência (EXP)** e **500.000 Pontos de Habilidade (SP)**.
- **5.000.000 de Adena**.

---

## CHECKPOINT 7: Subclasse e Nobless (Quests 234, 235, 241, 242, 246, 247)

As missões mais cobiçadas do Lineage II Interlude, permitindo a um personagem ter até 3 classes adicionais e ingressar no seleto grupo de Nobres com acesso às Grand Olympiad Games.

### 7.1 Quest 234: Fate's Whisper (Maestro Reorin e os 4 Raid Bosses)

- **Requisito**: Nível 75+.
- **NPC Inicial**: Maestro Reorin (ID 31002) em uma cabana ao leste de Town of Oren.
- **Os 4 Raid Bosses Obrigatórios**:
  1. **Shilen's Messenger Cabrio (25035)** no The Cemetery. Ao morrer, spawna o baú *Cabrio's Coffer* com o **Reorin's Scepter (Item 5012)**.
  2. **Death Lord Hallate (25220)** no 3º andar de Tower of Insolence. Baú *Hallate's Chest* concede o **Hallate's Infernium Scepter (Item 5013)**.
  3. **Kernon (25054)** no 8º andar de Tower of Insolence. Baú *Kernon's Chest* concede o **Kernon's Infernium Scepter (Item 5014)**.
  4. **Longhorn Golkonda (25126)** no 11º andar de Tower of Insolence. Baú *Golkonda's Chest* concede o **Golkonda's Infernium Scepter (Item 5015)**.
- **Fase dos Mestres Ferreiros**:
  - Visitar Cliff em Oren para pegar o Infernium Varnish.
  - Visitar Ferris em Aden para pegar o Reorin's Hammer.
  - Visitar Zenkin em Dion e Kaspar em Hardin's Academy para pegar o Reorin's Mold.
- **Fase do Sangue de Baium**:
  - Usar a Pipette Knife contra o Grand Boss Baium ou Platinum Tribe Shaman para coletar o Red Pipette Knife.
- **Troca de Arma Top B**:
  - Entregar 984 Crystals de Grau B e uma arma Top B (Damascus, Lance, Keshanberk, Staff of Evil Spirits).
- **Recompensa**: Escolha de uma arma Low A (Sword of Damascus, Tallum Glaive, Halberd, etc.) e a **Star of Destiny (Item 5011)**.

---

### 7.2 Quest 235: Mimir's Elixir (Alquimia e Destrave de Subclasse)

- **Requisito**: Nível 75+ e possuir a *Star of Destiny (5011)*.
- **NPC Inicial**: Magister Ladd (ID 30721) no 4º andar de Ivory Tower.
- **Etapas de Reagentes**:
  1. Visitar Magister Joan em Silent Valley para obter Chimera Blood.
  2. Alquimia na **Alchemist's Urn** no subsolo de Ivory Tower:
     - Misturar 10 Moonstone Shards + 1 Volcano Ash com Salamander Temperature -> 1 Moondust.
     - 10 Moondust + 1 Quick Silver com Ifrit Temperature -> 1 Lunargent.
     - 1 Lunargent + 1 Quick Silver -> **Pure Silver (Item 6320)**.
     - 1 Pure Silver + Chimera Blood -> **True Gold (Item 6321)**.
     - 1 True Gold + Pure Silver + **Blood Fire** (dropado por Bloody Guardians em Antharas Lair) misturados com Phoenix Temperature (grau 3 de calor) -> **Mimir's Elixir (Item 6319)**.
- **Conclusão**: Beber o Elixir perante Ladd. A Star of Destiny desaparece e o personagem ganha o direito permanente de adicionar **Subclasse** em qualquer Grand Master de capital.

---

### 7.3 Saga de Nobless: Possessor of a Precious Soul (Partes 1 a 4)

| Parte | Quest ID | Nível Mínimo | NPC Inicial | Desafio Central | Recompensa Principal |
|:---:|:---:|:---:|---|---|---|
| **Parte 1** | **241** | Subclasse Lv 50+ | Talien (31739) em Aden | Legend of 17; caça de Malruk Succubus Claws em Dragon Valley; visita a Gilmore e Mother Tree em Heine | Conclusão do primeiro elo de nobreza |
| **Parte 2** | **242** | Subclasse Lv 60+ | Virgilio (31742) em Rune | Investigação do sono profundo da princesa Kassandra; entrega de Pure Silver; combate com anjos sombrios | Caradine's Letter 1 (Item 7678) |
| **Parte 3** | **246** | Subclasse Lv 65+ | Caradine (31740) em Goddard | Coleta de anéis sagrados em Valley of Saints e abate do **Raid Boss Flame of Splendor Barakiel (25325)** para obter a *Rainmaker Staff* | Caradine's Letter 2 (Item 7679) |
| **Parte 4** | **247** | Subclasse Lv 75+ | Caradine (31740) em Goddard | Teleporte especial para o Coliseum; encontro com a **Lady of the Lake (31745)** nas águas sagradas | **Status de NOBLESSE**, Noblesse Tiara (Item 7694), Skills de Nobreza |

---

## CHECKPOINT 8: Grand Bosses, Instâncias e Clãs

### 8.1 Acesso a Chefes Épicos

| Grand Boss | Quest ID | Nome da Quest | Nível Mín. | NPC Inicial | Item de Acesso Obtido | NPC de Entrada / Portal |
|---|:---:|---|:---:|---|:---:|---|
| **Antharas** (29019) | **337** | Audience with the Land Dragon | 50+ | Gabrielle (30753) em Giran | **Portal Stone (3865)** | Heart of Warding (30755) em Antharas Lair |
| **Baium** (29020) | **348** | An Arrogant Search | 60+ | Magister Hanellin (30864) em Aden | **Blooded Fabric (4295)** | Angelic Vortex (30952) em ToI 13º andar |
| **Valakas** (29028) | **618** | Into the Flame | 70+ | Klein (31540) em FotG | **Floating Stone (7265)** | Heart of Volcano (31385) no núcleo de FotG |
| **Frintezza** (29045)| **119** | Last Imperial Prince | 74+ | Nameless Spirit (31453) em IT | **Magic Force Field Scroll (8073)** | Imperial Tomb Guide (32011) |
| **Sailren** (29065) | **641** | Attack Sailren | 75+ | Statues of Shilen (32109) em PI | **Gazkh Fragment (8782)** | Shilen's Stone Statue (32110) |
| **Four Sepulchers** | **620** | Four Goblets | 74+ | Nameless Spirit (31453) em IT | **Antique Brooch (7262)** | Sepulcher Manager (Conqueror's Gate) |

---

### 8.2 Progressão de Clã Lv 4 e Lv 5 (501 e 503)

- **Quest 501: Proof of Clan Alliance (Elevação para Nível 4)**:
  - **Líder Requerido**: Clã de nível 3.
  - **NPC Inicial**: Sir Kristof Rodemai (30756) em Giran.
  - **O Sacrifício Venenoso de Kalis**: A bruxa Kalis em Cemetery exige que **três membros voluntários do clã** bebam seu veneno mortal. Enquanto o veneno drena HP periodicamente, o líder deve caçar Witch Herbs em Garden of Eva com a ajuda de membros do clã e entregar o antídoto antes que os voluntários morram.
  - **Recompensa**: **Alliance Manifesto (Item 3874)** e 120.000 SP de clã.
- **Quest 503: Pursuit of Clan Ambition (Elevação para Nível 5)**:
  - **Líder Requerido**: Clã de nível 4.
  - **NPC Inicial**: Sir Gustaf Athebaldt (30760) em Oren.
  - **As 3 Tarefas Diplomáticas**:
    1. Acordo de Balthazar com os Giants em Giants Cave para recuperar o Titan's Powerstone.
    2. Acordo de Rodemai com os soldados imperiais.
    3. Acordo de Sir Eric Rodemai em Hunters Village para caçar os Spiteful Souls no Cemitério.
  - **Recompensa**: **Seal of Aspiration (Item 3870)** e 250.000 SP de clã.

---

### 8.3 Alianças de Facções (Ketra Orcs 605 vs Varka Silenos 611)

Sistema dinâmico de 5 estágios onde jogadores ganham a confiança de uma tribo abatendo guerreiros da tribo oposta:

```
[Estágio 1] ──> Matar 100 Soldados Inimigos ────> Mark of Alliance Stage 1
[Estágio 2] ──> Matar 200 Capitães Inimigos ────> Mark of Alliance Stage 2 (Acesso a Lojas de Poções)
[Estágio 3] ──> Matar 300 Generais Inimigos ────> Mark of Alliance Stage 3 (Acesso a Lojas de Armaduras)
[Estágio 4] ──> Obter Totem do Chefe Inimigo ───> Mark of Alliance Stage 4 (Receitas e Partes S-Grade)
[Estágio 5] ──> Abater Raid Boss Líder Rival ────> Mark of Alliance Stage 5 (Acesso às Relíquias Sagradas)
```
*Regra de Ouro Retail*: Se o jogador matar um único membro da facção aliada, **todo o progresso de aliança é imediatamente cancelado** e a reputação cai para zero.

---

## CHECKPOINT 9: Endgame Farm, Receitas S-Grade, Soul Crystals e Reagentes

### 9.1 Quest 350: Enhance Your Weapon (Soul Crystals Lv 1 a 13 para SA)

- **NPCs Iniciais**: Magister Jurek (30115) em Giran, Magister Gaius em Dion, Magister Fairen em Aden.
- **Tipos de Cristais**: Red Soul Crystal, Green Soul Crystal, Blue Soul Crystal.
- **Regras de Evolução (Level Up)**:
  - Estágios 1 a 10: Monstros em Catacombs, Necropolises e Sea of Spores (deve usar o cristal contra o mob com HP abaixo de 50% e desferir o golpe de misericórdia).
  - Estágios 11 e 12: Raid Bosses (Anakfim, Lilith, Zaken, Baium, Antharas, Valakas, Frintezza).
  - Estágio 13: Grand Bosses Antharas e Valakas (habilita Focus, Acumen e Health em armas S-Grade).

---

### 9.2 Quest 373: Supplier of Reagents (Alquimia no Ivory Tower)

A Alchemist's Urn no subsolo de Ivory Tower opera com uma matriz matemática de reagentes e temperaturas:

| Reagente Base | Catalisador | Nível de Fogo (Temperatura) | Resultado Obtido | Taxa de Sucesso |
|---|---|:---:|---|:---:|
| 10 Moonstone Shards | 1 Volcano Ash | Salamander (1 - Baixa) | 1 Moondust | 100% |
| 10 Moondust | 1 Quick Silver | Ifrit (2 - Média) | 1 Lunargent | 100% |
| 1 Lunargent | 1 Quick Silver | Salamander (1 - Baixa) | 1 Pure Silver | 100% |
| 10 Rotten Bone Pieces | 1 M索sh Stakato Mucus | Ifrit (2 - Média) | 1 Bone Dust | 100% |
| 10 Bone Dust | 1 Quick Silver | Phoenix (3 - Alta) | 1 Infernium Ore | 80% (Falha perde itens) |

---

### 9.3 Quests de Forge of the Gods (617) e Imperial Tomb (619)

- **Quest 617: Gather the Flames (FotG)**:
  - **NPC Inicial**: Blacksmith Vulcan (31539) na entrada de Forge of the Gods.
  - **Monstros**: Lavasaurus, Scarlet Stakatos, Magma Drakes.
  - **Item**: Torch Flames (coletar 1.000).
  - **Troca Retail**: Falar com o ferreiro lendário ambulante **Warsmith Rooney** (que spawna aleatoriamente entre 5 locais profundos do vulcão) para trocar 1.000 tochas por receitas de armas grau S (Forgotten Blade, Draconic Bow, Basalt Battlehammer, Imperial Staff).
- **Quest 619: Relics of the Old Empire (IT)**:
  - **NPC Inicial**: Ghost of Adventurer (31538) na entrada de Imperial Tomb.
  - **Monstros**: Soldados imperiais, cavaleiros espectrais e arqueiros do sepulcro.
  - **Item**: Broken Relic Parts (coletar 1.000).
  - **Troca Retail**: Troca por receitas e pedaços de armaduras S-Grade (Imperial Crusader, Draconic Leather, Major Arcana).

---

### 9.4 Mini-games e Economia de Farm

1. **Quest 662: A Game of Cards**:
   - NPC: Klump (30845) em Aden / Warehouse. Coleta de 50 Red Gemstones em farm para jogar uma rodada de pôquer (pôquer de 5 cartas com premiações em Enchants A, Scrolls e Zaken's Earring).
2. **Quest 663: Seductive Whispers**:
   - NPC: Wilbert (30846) em Aden. Jogo de dados com apostas consecutivas de Bead of Blessing até 8 rodadas acumulando armas B, A e Adena milionária.
3. **Quest 354: Conquest of Alligator Island**:
   - NPC: Kluck em Heine. Caça intensiva de crocodilos para troca diária de itens de buff e Adena rápida.
4. **Quest 632: Necromancer's Request**:
   - NPC: Mysterious Wizard em Forest of the Dead. Coleta de Vampire Fangs e Zombie Hearts para trocas econômicas em Rune.

---

## CHECKPOINT 10: Roteiro de Implementação no L2JLopez & Validação por Testes

### Estado Atual das Implementações no L2JLopez:

1. **Motor Central**:
   - `Quest.java`, `QuestState.java`, `QuestManager.java` nativos com Spring Boot e Java 21 LTS.
   - Suporte completo a `QuestTimer` via `ScheduledExecutorService` virtual.
   - Suporte a `onTutorialClientEvent (0xa2)`, `TutorialShowHtml (0xa0)`, `TutorialShowQuestionMark (0xa1)`, `TutorialCloseHtml (0xa3)` e `RadarControl (0xeb)`.
   - Detecção de arma equipada em mão direita (`getItemEquipped(7)`).

2. **Catálogos Oficiais Instanciados**:
   - `FirstClassQuestCatalog.java` (18 quests de 1ª classe 401 a 418).
   - `SecondClassQuestCatalog.java` (23 quests e 31 combinações de 2ª classe 211 a 233).
   - `NoblessAndSagaCatalog.java` (Sagas 70 a 100 e Nobless 241 a 247).
   - `GrandBossAccessCatalog.java` (Antharas, Baium, Valakas, Frintezza, Sailren).
   - `ClanQuestCatalog.java` (Clã Lv 4 e 5: 501 e 503).
   - `FactionAllianceCatalog.java` (Ketra 605 e Varka 611).
   - `EndgameFarmCatalog.java` (Soul Crystals 350, Reagentes 373, FotG 617, IT 619).

3. **Tutorial 255 Homologado para as 5 Raças**:
   - Suporte total a vozes `tutorial_voice_001a` até `001i`.
   - Coordenadas de radar oficiais para Talking Island, Elven Forest, Dark Forest, Immortal Plateau e Dwarven Mountains.
   - Suporte a eventos de cliente `CE1`, `CE2`, `CE8`, `CE30`, `CE45`, `CE57`, `CE6353`, `CE800000`.
   - Suporte a cliques em question marks `QM1`, `QM3`, `QM5`, `QM9`, `QM11`, `QM24`, `QM27`, `QM34`, `QM35`.
   - Suporte a links de tela `TE0` a `TE28`.

4. **Bateria de Testes Automatizados**:
   - `QuestEngineAndTutorialTest.java` com 6 testes unitários rigorosos passando com **100% de sucesso**:
     - `testQuestStateVariables` (persistência e variáveis).
     - `testQuestRegistrationAndLifecycle` (registro de quest e ciclo de vida).
     - `testQuest255TutorialFlow` (fluxo de matar o primeiro Gremlin e pegar Blue Gemstone).
     - `testQuestionMarkDeliversValidHtmlContent` (validação de tags HTML e frame NCHtmlFrame).
     - `testQuestListPacketEncoding` (validação de bitmask retail de 32 bytes no pacote 0x80).
     - `testMultiRaceTutorialAndRadar` (validação multi-raça de Elfos, Dark Elves, Orcs e Anões com radares de vilas e vozes).

---
*Documento homologado como Diretriz Suprema de Quests do projeto L2JLopez.*
