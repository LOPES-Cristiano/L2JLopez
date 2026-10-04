# L2JLopez

Servidor Lineage 2 Interlude (C6, protocolo 730-746) reescrito em **Java 21 + Spring Boot + Maven**, baseado no estudo do L2JDream V2.

## Requisitos
- JDK 21
- MySQL 8 (validado) ou MariaDB 10.6+ (apenas para rodar; os testes usam H2). Crie o schema: `CREATE DATABASE l2jlopez CHARACTER SET utf8mb4;`

## Comandos
```
./mvnw test                 # compila e roda os testes
./mvnw spring-boot:run      # sobe a aplicacao (precisa do MariaDB)
```
Variaveis de ambiente: `L2_DB_URL`, `L2_DB_USER`, `L2_DB_PASSWORD`. Nunca versione credenciais.

API de teste: `GET http://localhost:8080/api/status` e `/actuator/health`.

## Estrutura (`com.lopez.l2j`)
| Pacote | Papel |
|---|---|
| `config` | Records `@ConfigurationProperties` (fim do `Config.java`) |
| `persistence` | Entities e repositories (Spring Data), migracoes em `db/migration` (Flyway) |
| `network` | Login/Game sockets e pacotes (mmocore ou Netty) |
| `domain` | Modelo do jogo (personagens, itens, skills, clans), sem I/O |
| `features` | Mods: autofarm, dressme, pvprank, reset, roulette, aio, vip, achievements, vote, quake |
| `events` | Eventos de dominio (`PlayerKilledEvent`...) e eventos TvT/CTF/DM |
| `scripting` | NPCs e quests portados do Jython para Java |
| `api` | REST e webhooks |

## Regras de arquitetura
- Servicos, repositorios, listeners e agendadores sao beans do Spring.
- Monstros, drops no chao e efeitos temporarios sao POJOs (nao beans).
- Mods se comunicam por eventos, nunca chamando uns aos outros.
- Cada feature pode ser desligada em `l2.features.*.enabled`.

## Banco de dados (Flyway)
- `V1` baseline; `V2`..`V122`: estrutura das 121 tabelas (somente DDL, MyISAM -> InnoDB, `CREATE TABLE IF NOT EXISTS`).
- `V123`: `player_achievement` (normalizada; substitui a tabela legada `achievements`).
- `V1005`..`V1122`: dados estaticos do jogo (108.740 linhas portadas para `db/data`): npcs, droplist, spawnlist, armor, weapons, etc.

## Modulos e Sistemas Portados (100% Autonomo)
- **Datapack e Configs Locais**: diretório local `data/` com mais de 15.500 arquivos (HTMLs de NPCs, XMLs de teleporte, buylists, skills, stats, scripts) e `config/` com todas as propriedades de taxas, bosses, sieges e regras de jogo.
- **Login Server** (`network.login`): criptografia Blowfish L2, RSA 1024, handshake completo, autocriacao de contas.
- **Game Server** (`network.game`): `GameCrypt`, virtual threads (uma por conexao), pacotes de handshake e sessao do jogador.
- **Inventario e Paperdoll** (`game.item`, `game.service`): 22 slots de paperdoll, equip/unequip, empilhamento, itens iniciais de criacao (`char_creation_items`) e pacote `ItemList` (0x1b).
- **Mundo, NPCs e Spatial Grid** (`game.npc`, `game.world`): 26.622 spawns e 7.074 templates de NPCs, Spatial Grid 2D (4096 un) para visibilidade O(1), KnownList e pacotes `NpcInfo`, `CharInfo`, `DeleteObject`.
- **Dialogos e Menus HTML** (`game.html`): `HtmCache` com mais de 13.000 dialogos, substituicao de variaveis dinâmicas, `NpcHtmlMessage` (0x0f) e `RequestBypassToServer` (0x21).
- **Teleportes e Lojas** (`game.teleport`, `game.trade`): 561 pontos de teleporte (`TeleportToLocation` 0x28) com validacao de adena/noblesse pass; 625 listas de compras de comerciantes (`BuyList` 0x11, `RequestBuyItem` 0x1f).
- **Combate Fisico** (`game.combat`): calculos de acerto, evasao, chance de critico e formulas de dano base `(pAtk * 70.0) / pDef`, pacotes `Attack` (0x05), `StatusUpdate` (0x0e), `Die` (0x06), `Revive` (0x07) e recompensa de XP/SP ao abater monstros.
- **Drops, Spoil e Autoloot** (`game.drop`): 28.055 regras de drop da tabela `droplist`, suporte a taxas de adena/drop e autoloot direto no inventario com mensagens `SystemMessage` (53, 29).
- **Atalhos e Skills** (`game.shortcut`, `game.skill`): persistencia completa de atalhos da barra rapida (`character_shortcuts`, 0x33, 0x35, 0x44, 0x45) e conjuracao de habilidades (`character_skills`, `SkillList` 0x58, `RequestMagicSkillUse` 0x2f, `MagicSkillUse` 0x48).

## Testando com o cliente Interlude real
1. `.\scripts\run-dev.ps1` (ou use variáveis `L2_DB_USER`/`L2_DB_PASSWORD`).
2. Abra `system\l2.exe` no cliente `Lineage II - Chronicle Interlude` (ja configurado para `127.0.0.1`).
3. Entre com qualquer usuario e senha (criacao automatica ativa em dev).
4. Crie o personagem, teleporte com Gatekeepers, compre itens em mercadores, monte sua barra de atalhos e cace monstros com drops e XP!
