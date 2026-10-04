# L2JLopez

Servidor Lineage 2 Interlude (C6, protocolo 730-746) reescrito em **Java 21 + Spring Boot + Maven**, baseado no estudo do L2JDream V2.

## Requisitos
- JDK 21
- MariaDB 10.6+ (apenas para rodar; os testes usam H2)

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
- `V1` baseline; `V2`..`V122`: estrutura das 121 tabelas de `tools/sql` do L2JDream (somente DDL, MyISAM -> InnoDB,
  `CREATE TABLE IF NOT EXISTS`). **Os dados estaticos** (npc, droplist, spawnlist, armor... ~14 MB) continuam em
  `tools/sql` do L2JDreamV2 e devem ser importados a parte.
- `V123`: `player_achievement` (normalizada; substitui a tabela legada `achievements` com uma coluna por conquista).
- Nos testes o Flyway fica desligado (H2 nao entende todo o DDL do MariaDB); `MigrationFilesTest` valida os arquivos
  estaticamente. **Falta rodar as migracoes contra um MariaDB real** (`./mvnw spring-boot:run` com as variaveis acima).

## Modulos portados
- **Achievements** (`features.achievements`): le `features/achievements.xml` (mesmo formato do L2JDream),
  avalia a partir de um `PlayerSnapshot` e publica `AchievementCompletedEvent`. Diferencas: atributo desconhecido
  agora falha no start (antes era ignorado) e `mustBeX="false"` nao exige mais X.
- **Login / criptografia** (`network.login.crypt`): Blowfish L2 (JCE), checksum/XOR pass, `LoginCrypt`, RSA 1024 com
  modulo embaralhado, decodificacao de credenciais e verificacao do hash legado (SHA-1 Base64, fraco: trocar por hash
  com sal no rehash do primeiro login). Ainda **nao ha socket nem pacotes** (Init, RequestAuthLogin...).

## Roadmap
Ver `REVISAO_BACKLOG_MELHORIAS.md` no repositorio L2JDreamV2.
