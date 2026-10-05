# Estado Atual do Projeto — L2JLopez

**Data de Atualização**: Outubro de 2026  
**Versão**: Lineage II Interlude (Chronicle 6, Protocolo 730–746)  
**Stack**: Java 21 LTS, Spring Boot, Maven, MariaDB/MySQL (H2 nos testes automatizados), Flyway  
**Suíte de Testes**: **212 testes automatizados passando com 100% de sucesso (0 falhas, 0 erros)**

---

## 1. Visão Geral da Arquitetura

O **L2JLopez** é uma reescrita moderna do servidor Lineage 2 Interlude, projetada sobre os princípios de concorrência moderna do Java 21 (Virtual Threads), eliminando a complexidade legada de seletores NIO manuais (`mmocore`) e threads pools pesadas.

- **Concorrência**: Virtual Threads leves (`Thread.ofVirtual()`) para cada conexão de Login Server e Game Server.
- **Configuração**: Record classes fortemente tipadas mapeadas via `@ConfigurationProperties` em substituição aos arquivos de `.properties` soltos.
- **Persistência**: Spring Data JDBC / JPA + Flyway migrations (123 migrações estruturais + migrações de dados estáticos).
- **Mundo & Spatial Index**: Grid Espacial 2D particionado em células de 4096 unidades, garantindo buscas de visibilidade e broadcasting `O(1)`.

---

## 2. Status dos Módulos e Sistemas

| Módulo / Sistema | Status | Descrição do Estado Atual |
|---|:---:|---|
| **Login Server** | **Completo** | Handshake Blowfish/RSA 1024, auto-criação de contas em dev, validação de sessão em dose única (`SessionKeyRegistry`), virtual threads por conexão. |
| **Game Server Network** | **Funcional** | Decodificação e codificação de mais de 50 pacotes do protocolo C6 Interlude com encriptação `GameCrypt` ativa. |
| **Mundo & Spatial Grid** | **Funcional** | `GameWorld` com spatial grid para jogadores e NPCs; KnownList com ciclo de detecção, atualização de coordenadas e broadcast de movimento/ações. |
| **Spawns de NPCs e Bosses** | **Funcional** | Carregamento de mais de 26.000 spawns via `spawnlist`, `custom_spawnlist`, `raidboss_spawnlist`, `vanhalter_spawnlist` e `lastimperialtomb_spawnlist`. Sistema de `fallbackTemplate` para evitar que NPCs sem template explícito sejam descartados. |
| **Diálogos & Menus HTML** | **Funcional** | `HtmCache` indexa mais de 10.700 arquivos HTML em `data/html`, `data/scripts` e `data/`. Fallbacks interativos inteligentes para Teleporters, Merchants, Blacksmiths, Guild Trainers e Warehouses para que nenhum NPC caia em "I have nothing to say to you". |
| **Bypasses & Interações** | **Funcional** | Suporte a `Chat`, `Link` (com e sem prefixo `npc_`), `goto` (teleportes normais e nobres), `Quest` (incluindo Monster Derby Track 1101), `Buy`, `Sell`, `multisell` / `exc_multisell`, `Wear`, `Augment`, `DepositP/C/F` e `WithdrawP/C/F`. |
| **Comércio & Economia** | **Funcional** | `BuyList` (625 listas), `SellList` (0x10) e `RequestSellItem` (0x1e) com cálculo de adena e atualização de inventário; `MultiSellTable` (162 listas XML) com troca e validação de ingredientes. |
| **Armazém (Warehouse)** | **Funcional** | Baú privado (`DepositP`/`WithdrawP`), baú de clã (`DepositC`/`WithdrawC`) e freight com persistência no banco de dados. |
| **Inventário & Equipamentos** | **Funcional** | 22 slots de paperdoll, suporte a slots duplos (`LRHAND` e `FULLARMOR`), itens iniciais por classe (`char_creation_items`), restrições de uso e destruição de itens. |
| **Encantamento (Enchant)** | **Funcional** | `RequestEnchantItem`, cálculo de taxas de sucesso por scroll (normal, blessed, crystal), bônus de P.Atk/P.Def/M.Atk e glow visual da arma. |
| **Consumíveis & Tiros** | **Funcional** | Poções de HP/MP/CP com regeneração ao longo do tempo (HoT), scrolls de escape e ressurreição, Soulshots e Spiritshots com ativação automática em combate. |
| **Combate Físico & Mágico** | **Funcional** | Fórmulas de dano físico e mágico Interlude, chance de acerto/evasão, taxa de crítico, dano de CP pré-HP em PvP, perda de alvo, cancelamento de cast ao receber dano massivo. |
| **Restrições de Armas em Skills** | **Funcional** | Validação de arma equipada antes de conjurar habilidades (ex: arco exige e consome flechas; skills de adaga exigem adaga; skills de blunt/dual/polearm exigem os respectivos tipos). |
| **Efeitos, Buffs e Debuffs** | **Funcional** | Efeitos de Sleep (com despertar ao sofrer dano), Stun, Root, Paralysis, Silence. Persistência de buffs ativos no logout/relogin via tabela `character_skills_save`. |
| **Árvore de Skills & Classes** | **Funcional** | 2.686 templates de skills carregados de `data/xml/stats/skills`, 89 classes e 216 treinadores mapeados em `SkillTreeTable`. |
| **Sistema de Party** | **Funcional** | Convites (`RequestJoinParty`, `RequestAnswerJoinParty`), interface de membros (`PartySmallWindowAll/Add/Delete`), buffs e curas em grupo, divisão de EXP/SP com bônus de party por proximidade. |
| **Comandos de Administrador (GM)** | **Funcional** | Menu visual `//admin` e submenus HTML; comandos de chat e bypass `//move_to`, `//item`, `//spawn`, `//heal`, `//setlevel`, `//setew`, `//setec`, `//para`, `//unpara`, `//invis`, `//vis`, `//speed`, `//skill`, `//removeskill`, `//goname`, `//recall`, `//announce`, `//setadmin`. Cores de nome e título GM personalizadas. |
| **Mods & Customizações** | **Funcional** | Sistema de Conquistas (`features.achievements`), Quake Kill Announcer (`Killing Spree`, `Rampage`), REST API (`/api/status`, `/actuator/health`). |

---

## 3. Estrutura de Código Atual

```text
com.lopez.l2j
├── config/              # ServerProperties e records de configuracao
├── features/            # Mods: achievements, quake announce
│   └── achievements/    # Sistema completo de conquistas
├── game/
│   ├── ai/              # Servico de IA basico de NPCs e mobs
│   ├── combat/          # CombatService (formulas, formulas magicas, retaliacao)
│   ├── drop/            # DropService e JdbcDropTable (autoloot, droplist)
│   ├── effect/          # ConsumableTable, PlayerEffects, Buff persistence
│   ├── html/            # HtmCache (10.747 HTMLs indexados, fallbacks funcionais)
│   ├── item/            # Inventory, ItemInstance, ItemSlots, EnchantScrollTable
│   ├── model/           # PlayerCharacter, PlayerStats, ExperienceTable
│   ├── multisell/       # MultiSellTable (listas XML e trocas)
│   ├── npc/             # NpcInstance, NpcTemplate, SpawnService
│   ├── party/           # Party (distribuicao de EXP/SP, gestao de membros)
│   ├── service/         # CharacterService, InventoryService, WarehouseService
│   ├── shortcut/        # Atalhos de barra rapida e persistencia
│   ├── skill/           # SkillTable, SkillTreeTable, SkillCondition
│   ├── teleport/        # TeleportLocationTable (pontos normais e nobresse)
│   ├── template/        # CharTemplateTable e status base de criacao
│   ├── trade/           # BuyListTable (comerciantes e lojas)
│   └── world/           # GameWorld e GameWorldSpatialGrid
└── network/
    ├── game/            # GameSession, net.GameServer, crypt.GameCrypt
    │   └── packet/      # GameClientPacket (0x01..0x6e) e GameServerPacket
    └── login/           # LoginConnection, net.LoginServer, service.LoginAccountService
```

---

## 4. Testes e Qualidade

- **Execução**: `./mvnw test`
- **Total de Testes**: **212 testes**
- **Falhas / Erros**: **0**
- **Cobertura Principal**:
  - `GameSessionFeaturesTest`: Combate, penalidades, restrições de arco/armas, bypasses de NPCs, multisell, sell list, teleporte, comandos de admin e persistência de buffs.
  - `SkillSystemTest`: Custo de MP/HP, restrições de armas por skill, alcance e condições de alvo.
  - `HtmCacheTest`: Indexação de arquivos, substituição de variáveis e geração de fallbacks funcionais.
  - `SpawnServiceTest`: Carregamento de tabelas de spawn e templates de contingência.
  - `PartyTest`: Formação de grupo, limites, distribuição justa de EXP/SP e cálculo de bônus.
  - `CombatServiceTest`: Fórmulas de dano, chance de crítico, acerto e evasão.
  - `GameServerEndToEndTest`: Fluxo de rede ponta a ponta desde conexão até ações no mundo.

---

## 5. Como Iniciar o Servidor

1. **Configurar Variáveis ou application.yml**:
   Certifique-se de que o MariaDB/MySQL está ativo e o schema criado:
   ```sql
   CREATE DATABASE l2jlopez CHARACTER SET utf8mb4;
   ```
2. **Executar a Aplicação**:
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Conectar com Cliente Interlude**:
   - Abrir o cliente Lineage II Interlude apontando para `127.0.0.1`.
   - Criar conta automaticamente ao digitar usuário e senha.
   - Criar personagem e interagir com o mundo, NPCs, combate e comandos de admin (`//admin`).
