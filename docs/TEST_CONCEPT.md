# TicTacToe Test Concept

This document describes the test concept for the TicTacToe project. It covers what is tested, how it is tested, the tools used and which parts of the code are covered by automated tests.

## 1. Purpose and Scope

The goal of the test concept is to make sure the game behaves according to the rules of tic-tac-toe (see §2.1), for the game logic in `TicTacToeMain` as well as for every player implementation and the console interaction.

In scope:

- Win detection (`isWin`) for both colors, all 8 lines, nearly complete and blocked lines
- The game loop (`play`): alternating turns, win, draw, invalid moves
- Console rendering (`toString`) and console interaction (`main`, `HumanPlayer` via stdin/stdout)
- All player implementations: `GreedyPlayer`, `HumanPlayer`, `PerfectPlayer`
- `Stone.opponent()`

Out of scope: nothing of the production code. Non-numeric input of a human player is tested, but only to document the current behavior (a `NumberFormatException` ends the game).

## 2. Test Object

Package under test: `ch.bbw.m450.tictactoe`

- `TicTacToeMain` contains the game logic (`isWin`, `play`, `toString`) and the `main` entry point (human vs. greedy player).
- `TicTacToePlayer` is the interface every player implements. `Stone` (`CROSS`, `CIRCLE`) is a field of the board, `null` is an empty field.
- `GreedyPlayer` plays the first free field, `HumanPlayer` reads the position from stdin, `PerfectPlayer` plays perfectly using minimax.
- The board is a `Stone[9]`, indexed row by row:

```
 0 | 1 | 2
---+---+---
 3 | 4 | 5
---+---+---
 6 | 7 | 8
```

### 2.1 Test Basis (Requirements)

| ID | Requirement |
|---|---|
| R1 | A color wins with three own stones in a row, column or diagonal. Stones of the other color or empty fields never count. |
| R2 | X starts, the players alternate. The game ends at the first win or after 9 moves as a draw (`null`). |
| R3 | A move outside 0–8 or onto an occupied field ends the game with an `IllegalStateException`, the board is printed before. |
| R4 | The same player instance must not play both colors (`IllegalArgumentException`). |
| R5 | The console shows stones bold, free fields grey with their index; the human player is prompted and enters one position per line. |
| R6 | The perfect player never loses, takes an immediate win and blocks the opponent. |

## 3. Tools and Framework

| Purpose | Tool |
|---|---|
| Test runner | JUnit 6 (Jupiter) incl. parameterized tests |
| Assertions | AssertJ (`WithAssertions`) plus a custom `BoardAssert` |
| StdIn/StdOut | JUnit Pioneer (`@StdIo`, `StdIn`, `StdOut`) |
| BDD (additional framework) | Cucumber 8 via the JUnit Platform Suite engine |
| Coverage | JaCoCo report + coverage verification (branch coverage ≥ 90 %) |
| Mutation testing | PITest (mutation score ≥ 90 %) |
| Build | Gradle |

Test helpers in `src/test/java/ch/bbw/m450/tictactoe/`:

- `Boards`: `board("XO.", ".X.", "..O")` builds a board the way it looks, `boardWithStonesAt(color, positions...)`, `emptyBoard()`
- `BoardAssert`: `assertThatBoard(board).isWonBy(CROSS).isNotWonBy(CIRCLE)`, `hasNoWinner()` with a readable failure message showing the board
- `ScriptedPlayer`: `ScriptedPlayer.playing(0, 4, 8)`, a deterministic stub that plays fixed positions

## 4. Test Strategy

### 4.1 Test levels

- **Unit tests**: `isWin`, `toString`, `Stone`, each player in isolation
- **Component tests**: `play` with `ScriptedPlayer` stubs (no concrete player involved)
- **System tests**: `TicTacToeConsoleTest` runs `main` with stdin/stdout replaced by Pioneer, Cucumber scenarios describe the perfect player in business language

### 4.2 Test design techniques

- **Equivalence classes**: full line (win) / nearly complete line / blocked line / no line, for both colors
- **Boundary values**: positions `-1` and `9` (just outside the board), a line with exactly one field missing, the win with the 9th (last) move
- **Decision table**: `isWin` is a disjunction of 8 lines × 3 conditions; every condition is made true and false at least once (full line, gap at each of the 3 positions)
- **Exhaustive testing**: the perfect player is played against every possible opponent (all game trees)

### 4.3 Conventions

- Test names follow `given_<state>_when_<action>_then_<result>`.
- Whenever only the data of a test differs, a `@ParameterizedTest` is used (`@MethodSource`, `@CsvSource`, `@EnumSource`) instead of copied test methods. Boards in `@CsvSource` are written as three row strings.
- Exceptions are checked with `assertThatIllegalStateException()` / `assertThatIllegalArgumentException()` including the message, because the message contains the invalid position and is shown to the user.
- Only fluent AssertJ assertions, no `assertTrue`.

## 5. Test Cases

Detailed list in [`TESTS.md`](TESTS.md). Overview (160 tests):

| Test class | Tests | Covers |
|---|---|---|
| `TicTacToeMainTest.IsWin` | 115 | R1: 2 colors × 8 lines, 96 nearly complete/blocked lines, 3 boards without line |
| `TicTacToeMainTest.Play` | 8 | R2–R4: X wins, O wins, win with 9th move, draw, 3 invalid moves, same instance |
| `TicTacToeMainTest.ToString` | 2 | R5: content and ANSI formatting |
| `TicTacToeConsoleTest` | 5 | R2, R3, R5 via stdin/stdout: human wins, human loses, invalid input, board printed on invalid move, draw message |
| `StoneTest` | 2 | `opponent()` for both colors |
| `GreedyPlayerTest` | 5 | first free field, full board |
| `HumanPlayerTest` | 4 | R5: reads position, prints prompt, one line per move, non-numeric input |
| `PerfectPlayerTest` | 12 | R6: best moves, board unchanged, full board, draw vs. itself, beats greedy, never loses (exhaustive) |
| `perfect_player.feature` (Cucumber) | 7 | R6 as BDD scenarios |

## 6. Execution, Entry and Exit Criteria

| | |
|---|---|
| **Locally** | `./gradlew check` (tests, coverage report and verification), `./gradlew pitest` (mutation testing) |
| **CI** | On every push (all branches) and on pull requests, see [`COVERAGE.md`](COVERAGE.md). All reports are uploaded as build artifacts. |
| **Environment** | Devcontainer image (Alpine, Azul Zulu JDK 25), same image locally and in CI |
| **Entry** | The code compiles (`build` job). |
| **Exit** | All tests green, branch coverage ≥ 90 % (JaCoCo verification), line coverage ≥ 90 % and not lower than on `main` (PR check), mutation score ≥ 90 % (PIT). |
| **Failing tests** | A red build blocks the merge (required checks). The HTML reports in the artifacts show the cause, they are uploaded even when tests fail. |

Measured on 2026-10-27: line coverage 100 %, branch coverage 100 %, mutation score 100 % (72/72 mutants killed).

## 7. Open Points

- `HumanPlayer` ends the game with a `NumberFormatException` on non-numeric input. Asking again would be friendlier; the behavior is documented by a test so a change is a conscious decision.
- `main` always plays human vs. greedy player. Choosing the `PerfectPlayer` as opponent would need a small menu (and new console tests).

## 8. Audit (2026-09-16)

Auditor: Philipp Noel Derks
Gegenstand: dieses Dokument. Der Code (`TicTacToeMain.java`, `TicTacToeMainTest.java`) wurde für dieses Audit nicht eingesehen; Befunde, die von der Implementierung abhängen, sind entsprechend markiert.
Ergebnis: 2 kritisch, 4 hoch, 5 mittel, 4 niedrig. Das Konzept war in diesem Zustand nicht abgabereif. Die Probleme lagen weniger im Stil als in unbelegten Aussagen und einer fehlenden Testbasis.

### Befunde

| ID | Schwere | Ort | Befund | Massnahme |
|---|---|---|---|---|
| K1 | Kritisch | §6 | "Complete coverage of every branch inside isWin" wurde ohne Messung behauptet. Es gab kein Coverage-Tool (§3). | JaCoCo einbinden und den gemessenen Wert eintragen, oder die Aussage streichen. |
| K2 | Kritisch | §5, §6 | Kein Test prüft, ob die Farbe berücksichtigt wird. Eine Implementierung, die "3 gleiche nicht-null Steine" prüft statt "3× color", besteht alle Tests. | Testfall: CROSS-Linie, `isWin(board, CIRCLE)` → `false`. |
| H1 | Hoch | §6 | (abhängig von der Implementierung) Bei einer typischen Prüfung `b[x]==c && b[y]==c && b[z]==c` wird der Fall "erste und zweite Bedingung wahr, dritte falsch" wahrscheinlich nie erreicht. Das leere Brett scheitert immer an der ersten Bedingung. Die 8 Gewinnlinien scheitern in vorher geprüften Linien höchstens an der zweiten, z.B. prüft Linie 0,3,6 zuerst 0,1,2 mit b0=T, b1=F. Damit wäre die Branch Coverage unvollständig. | Beinahe-Linien testen: 2 von 3 besetzt, sowie 2× CROSS + 1× CIRCLE. |
| H2 | Hoch | gesamt | Es fehlt eine Testbasis: keine Anforderungen und keine Spezifikation, gegen die getestet wird. Die Soll-Werte sind offensichtlich aus dem Code abgeleitet. Dadurch bestätigen die Tests die Implementierung, statt sie zu prüfen. | Anforderungen formulieren (Spielregeln, Verhalten bei ungültigem Zug, Unentschieden) und die Testfälle darauf referenzieren. |
| H3 | Hoch | §1, §6 | Die Risikoverteilung ist invertiert. `isWin` ist trivial und am besten getestet. `play` enthält Zugwechsel, Zugvalidierung, Gewinn- und Remis-Erkennung und hat nur einen Guard-Test. | `play`-Tests mit deterministischen Stubs als Priorität 1. |
| H4 | Hoch | §4 | Es sind keine Testentwurfsverfahren genannt, also weder Äquivalenzklassen noch Grenzwerte noch Entscheidungstabellen. Die Testfallauswahl ist dadurch nicht begründet. | Verfahren benennen und pro Testfall zuordnen. Beinahe-Linien sind z.B. der Grenzwert. |
| M1 | Mittel | §1 | Die Scope-Begründung widerspricht sich. "Pure, deterministic logic" wird als Kriterium genannt, trotzdem sind `GreedyPlayer` und `toString` ausgeschlossen, obwohl beide deterministisch und trivial testbar sind. | Jeden Out-of-Scope-Punkt einzeln begründen oder in den Scope aufnehmen. |
| M2 | Mittel | §4, §5 | Test 3 hängt von `GreedyPlayer` ab. Das widerspricht "independent of any concrete player implementation" in §1. | Lambda oder anonymen Stub verwenden. Der wird für die `play`-Tests ohnehin gebraucht. |
| M3 | Mittel | §5 | Es gibt keine CIRCLE-Gewinnfälle. Die parametrisierten Tests laufen nur für eine Farbe. | `@MethodSource` auf das Kreuzprodukt Farbe × Linie erweitern (16 Fälle). |
| M4 | Mittel | Struktur | Wesentliche Konzeptteile fehlen: Testebenen, Ein- und Ausstiegskriterien, Testumgebung, Ausführungszeitpunkt (lokal/CI) und der Umgang mit fehlschlagenden Tests. | Abschnitte ergänzen. |
| M5 | Mittel | §7 | Die offenen Punkte waren nicht priorisiert, und die zentralen Lücken K2 und H1 fehlten dort ganz. | Liste nach Risiko sortieren und ergänzen. |
| N1 | Niedrig | §3, §5 | Es gibt eine Inkonsistenz: Als Assertion-Library ist AssertJ angegeben, `dummyTest` nutzt aber `assertTrue`, und das stammt aus JUnit. `WithAssertions` hat kein `assertTrue`. | Einheitlich AssertJ verwenden, oder die Angabe korrigieren. |
| N2 | Niedrig | §5 | `dummyTest` kann nie fehlschlagen und hat keinen Aussagewert. | Löschen. |
| N3 | Niedrig | §2 | `Stone` fehlt bei den Testobjekten, obwohl es verwendet wird und `opponent()` in §7 vorkommt. Auch die Board-Repräsentation (Typ, Index-Layout 0–8 zeilenweise) ist nicht dokumentiert. | Ergänzen. |
| N4 | Niedrig | §5 | Test 2 prüft zwei Farben in einer Methode, und Test 3 vergleicht die exakte Message. Das ist beides nicht falsch, aber nicht begründet. | Parametrisieren. Beim Message-Vergleich bewusst entscheiden und die Entscheidung dokumentieren. |

### Massnahmen nach Reihenfolge

1. K2, H1 und M3: `isWin`-Tests erweitern (Farbwechsel, Beinahe-Linien, CIRCLE)
2. K1: JaCoCo einbinden und den echten Wert eintragen
3. H3 und M2: `play`-Tests mit Stubs schreiben (Gewinn, Remis, ungültiger Zug → `IllegalStateException`)
4. H2, H4 und M4: Testbasis, Verfahren und fehlende Konzeptabschnitte ergänzen
5. Rest: redaktionelle Korrekturen

## 9. Umsetzung der Audit-Massnahmen (2026-10-27)

| ID | Status | Umsetzung |
|---|---|---|
| K1 | erledigt | JaCoCo misst 100 % Line/Branch Coverage, `jacocoTestCoverageVerification` erzwingt ≥ 90 % Branch Coverage (§6). |
| K2 | erledigt | Jeder Gewinnlinien-Test prüft zusätzlich `isNotWonBy(color.opponent())`. |
| H1 | erledigt | 96 Beinahe-Linien (je Linie jede der 3 Positionen leer oder vom Gegner besetzt), PIT bestätigt: alle Mutanten in `isWin` werden getötet. |
| H2 | erledigt | Anforderungen R1–R6 (§2.1), Testklassen darauf referenziert (§5). |
| H3 | erledigt | `play` mit `ScriptedPlayer`-Stubs: Gewinn X/O, Gewinn im 9. Zug, Remis, ungültige Züge. |
| H4 | erledigt | Testentwurfsverfahren in §4.2. |
| M1 | erledigt | `toString`, `GreedyPlayer`, `HumanPlayer` und `main` sind jetzt im Scope. |
| M2 | erledigt | `ScriptedPlayer` statt `GreedyPlayer` im Guard-Test. |
| M3 | erledigt | Kreuzprodukt Farbe × Linie (16 Fälle). |
| M4 | erledigt | Testebenen (§4.1), Ein-/Ausstiegskriterien, Umgebung, Ausführung (§6). |
| M5 | erledigt | §7 neu, enthält nur noch bewusst offene Punkte. |
| N1, N2 | erledigt | `dummyTest` und `assertTrue` entfernt, nur noch AssertJ. |
| N3 | erledigt | `Stone` und Brett-Layout in §2. |
| N4 | erledigt | Leeres Brett ist Teil eines parametrisierten Tests; Message-Vergleich begründet in §4.3. |
