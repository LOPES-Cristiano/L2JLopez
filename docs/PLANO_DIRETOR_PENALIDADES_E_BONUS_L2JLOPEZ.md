# Plano Diretor de Mecânicas: Penalidades, Bônus de Armaduras, Skills, Classes e Atributos

Este plano estabelece a implementação exaustiva e retail de todas as penalidades, escalas de nível, bônus de atributos, proficiência de grau (*Expertise*) e regras de armaduras no **L2JLopez**, espelhando o comportamento de referência do **L2JDreamV2**.

---

## 1. Escala Oficial de Atributos Básicos (*BaseStats*) e Nível (*LevelMod*)

### 1.1 Fórmulas de LevelMod
No Lineage II Interlude oficial:
$$\text{LevelMod} = \frac{\text{Level} + 89.0}{100.0}$$
- **Nível 1:** $0.90$
- **Nível 20:** $1.09$
- **Nível 40:** $1.29$
- **Nível 60:** $1.49$
- **Nível 76:** $1.65$
- **Nível 80:** $1.69$

### 1.2 Tabelas de Bônus de Atributos (*statBonus.xml*)
Os bônus são indexados de 1 a 100 conforme o arquivo oficial:
- **STR:** Escala de P.Atk ($1.036^{\text{STR} - 34.845}$)
- **CON:** Escala de Max HP, Max CP e taxa de regeneração
- **DEX:** Escala de Atk.Spd, Critical Rate, Accuracy e Evasion
- **INT:** Escala de M.Atk ($(\text{INTbonus})^2$)
- **WIT:** Escala de Casting Speed e Magic Critical Rate
- **MEN:** Escala de M.Def, Max MP e resistência a debuffs mentais

---

## 2. Sistema de Penalidades de Grau (*Grade Penalty / Expertise*)

### 2.1 Faixas de Grau por Nível
| Nível | Grau do Equipamento | Nível da Skill Expertise (Id 239) |
|---|---|---|
| 1 a 19 | No-Grade (0) | - |
| 20 a 39 | D-Grade (1) | Lv 1 |
| 40 a 51 | C-Grade (2) | Lv 2 |
| 52 a 60 | B-Grade (3) | Lv 3 |
| 61 a 75 | A-Grade (4) | Lv 4 |
| 76 a 80 | S-Grade (5) | Lv 5 |

### 2.2 Efeito das Penalidades de Grau Excedido
Se um personagem equipar uma arma ou armadura com grau superior ao seu nível de maestria:
$$\Delta\text{Grade} = \text{ItemGrade} - \text{PlayerExpertise}$$

1. **Arma:**
   - **Precisão (Accuracy):** $-16 \times \Delta\text{Grade}$
   - **P.Atk e Atk.Spd:** $-33\%$
   - **M.Atk e Casting Spd:** $-33\%$
   - **Critical Rate:** $-50\%$
   - **Soulshots:** Chance de falha de 50%+
2. **Armadura:**
   - **Velocidade (RunSpeed):** $-20\% \times \Delta\text{Grade}$
   - **Evasão (Evasion):** $-8 \times \Delta\text{Grade}$
   - **P.Def / M.Def:** $-20\%$
3. **Interface e Pacotes:**
   - Envio do pacote `EtcStatusUpdate` com `expertisePenalty = \Delta\text{Grade}`.
   - Mensagem de sistema: *The equipment's grade is too high. A penalty is applied.*

---

## 3. Sistema de Penalidades de Peso (*Weight Penalty*)

Cálculo percentual:
$$\text{WeightRatio} = \frac{\text{CurrentLoad}}{\text{MaxLoad}} \times 100\%$$

| Faixa de Carga | Nível da Penalidade | Efeitos Práticos |
|---|---|---|
| $0\%$ a $49.9\%$ | Nível 0 | Sem penalidade (regeneração normal) |
| $50\%$ a $65.9\%$ | Nível 1 | **Desativação total** da regeneração natural de HP e MP |
| $66\%$ a $79.9\%$ | Nível 2 | $-33\%$ na velocidade de movimento (*RunSpeed*) |
| $80\%$ a $99.9\%$ | Nível 3 | $-50\%$ na velocidade de movimento; **bloqueio** de auto-ataque e skills físicas |
| $\ge 100\%$ | Nível 4 | **Sobrecarga total**: Speed travado em 0; bloqueio de movimento, ataque e magias |

- Sincronização via `EtcStatusUpdate` com o índice de penalidade ($0$ a $4$) ativando a balança no cliente.

---

## 4. Tipos de Armadura, Escudos e Bônus de Sets

1. **Penalidade de Escudo:**
   - Redução permanente de $-8$ de evasão enquanto equipado.
   - Bloqueio com escudo absorve dano físico até o `shield_def` da peça.
2. **Sets de Armadura (*Armor Sets*):**
   - Ativação de bônus cumulativos de conjunto ao equipar as peças completas (ex: Dark Crystal Robe, Tallum Heavy, Majestic Light, Draconic Leather, Imperial Crusader).

---

## 5. Status da Implementação e Validação

- [x] **BaseStatsTable**: Tabela oficial completa com curvas de bônus (`statBonus.xml`) portada do `L2JDreamV2` e inicializada no static block.
- [x] **LevelMod & Escala Proporcional**: Fórmulas oficiais aplicadas para escalonamento monótono de P.Atk, P.Def, M.Atk, M.Def, Accuracy, Evasion e Critical.
- [x] **Grade Penalty (Expertise)**: Detecção automática do maior grau equipado versus nível do personagem; reduções severas de precisão (-16 por grau), velocidade, ataque e defesa.
- [x] **Weight Penalty**: Detecção de 5 níveis de carga (0 a 4) baseados na capacidade oficial da classe (`template.maxLoad()`) e CON; corte de regeneração a partir de 50%, cortes de velocidade em 66% e 80%, travamento total em 100%, e bloqueio de ataque físico em sobrecarga.
- [x] **Penalidade de Escudo**: Redução de -8 pontos de evasão ao equipar escudo na mão esquerda.
- [x] **Sincronização de Rede**: Pacote `EtcStatusUpdate` (0xF3) sincronizado de forma reativa a cada mudança de penalidade de peso ou grau.
- [x] **Suíte de Testes Automatizada**: 100% dos 476 testes do repositório (`PlayerPenaltiesTest`, `PlayerStatsTest`, `GameServerEndToEndTest`, etc.) validados e passando com `BUILD SUCCESS`.
