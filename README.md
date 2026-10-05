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
- **Login Server** (`network.login`): criptografia Blowfish L2, RSA 1024, handshake completo, autocriacao de contas e single-use sessions.
- **Game Server** (`network.game`): `GameCrypt`, virtual threads (uma por conexao), pacotes de handshake e sessao do jogador.
- **Inventario, Paperdoll & Economia** (`game.item`, `game.service`): 22 slots de paperdoll (com slots duplos), equip/unequip, empilhamento, itens iniciais de criacao (`char_creation_items`), compra em mercadores (`BuyList` 0x11), venda com retorno de adena (`SellList` 0x10, `RequestSellItem` 0x1e), 162 listas de multisell (`MultiSellList` 0xd0) e armazém privado/clã/freight (`WarehouseService`).
- **Mundo, Spawns & Spatial Grid** (`game.npc`, `game.world`): Carregamento de mais de 26.000 spawns (`spawnlist`, `custom_spawnlist`, `raidboss_spawnlist`, `vanhalter_spawnlist`, `lastimperialtomb_spawnlist`) com `fallbackTemplate`, Spatial Grid 2D (4096 un) para visibilidade O(1), KnownList e pacotes `NpcInfo`, `CharInfo`, `DeleteObject`.
- **Dialogos e Menus HTML** (`game.html`): `HtmCache` com mais de 10.700 dialogos indexados em `data/html`, `data/scripts` e `data/`, substituicao de variaveis dinâmicas, fallbacks funcionais inteligentes para Teleporters, Merchants, Blacksmiths, Guild Trainers e Warehouses.
- **Teleportes e Bypasses** (`game.teleport`): 561 pontos de teleporte (`TeleportToLocation` 0x28) com validacao de adena/noblesse pass, teleporte do Monster Derby Track (`1101_teleport_to_race_track`) e janelas de augmentação Life Stone (`ExShowVariationMakeWindow`/`ExShowVariationCancelWindow`).
- **Combate Fisico & Magico** (`game.combat`): calculos de acerto, evasao, chance de critico, dano de CP pré-HP em PvP, debuffs de controle (Sleep com wake-up ao receber dano, Stun, Root, Paralysis, Silence) e formulas de dano base Interlude.
- **Atalhos, Skills & Buffs** (`game.shortcut`, `game.skill`, `game.effect`): 2.686 templates de skills, 89 classes em `SkillTreeTable`, validacao estrita de tipo de arma equipada (arco consome flechas, adagas, espadas, blunts, etc.), persistencia de buffs no relogin (`character_skills_save`) e ativacao automatica de Soulshots/Spiritshots.
- **Sistema de Party** (`game.party`): convites, janela de membros (`PartySmallWindow`), buffs e curas em grupo, divisao justa de EXP/SP com multiplicador de bônus por quantidade de jogadores e proximidade.
- **Painel Administrativo & GM Commands**: menu interativo `//admin` com navegacao HTML completa e comandos `//spawn`, `//item`, `//heal`, `//setlevel`, `//setew`, `//setec`, `//para`, `//unpara`, `//invis`, `//vis`, `//speed`, `//skill`, `//removeskill`, `//goname`, `//recall`, `//announce`, `//setadmin`. Cores de nome e titulo GM ativas.
- **Mods & Features**: Sistema de Conquistas (`features.achievements`), Quake Kill Announcer (`Killing Spree`, `Rampage`), REST API (`/api/status`, `/actuator/health`).

## Documentação Detalhada
Para uma visão aprofundada de todos os sistemas, arquitetura e estado atual, consulte [docs/ESTADO_DO_PROJETO.md](file:///docs/ESTADO_DO_PROJETO.md).

## Testando com o cliente Interlude real
1. `.\scripts\run-dev.ps1` (ou use variáveis `L2_DB_USER`/`L2_DB_PASSWORD`).
2. Abra `system\l2.exe` no cliente `Lineage II - Chronicle Interlude` (ja configurado para `127.0.0.1`).
3. Entre com qualquer usuario e senha (criacao automatica ativa em dev).
4. Crie o personagem, teleporte com Gatekeepers, compre/venda itens em mercadores, encante equipamentos, jogue em party e explore o mundo com mais de 26.000 monstros e bosses!

