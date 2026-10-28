# TicTacToe – Test-Dokumentation

Dieses Dokument beschreibt die Tests für das TicTacToe-Projekt nach dem **GIVEN / WHEN / THEN**-Pattern.
Konzept, Werkzeuge und Anforderungen (R1–R6): siehe [`TEST_CONCEPT.md`](TEST_CONCEPT.md).

Getestet wird mit **JUnit 6**, **AssertJ**, **JUnit Pioneer** (StdIn/StdOut) und **Cucumber** (BDD).
Brett-Notation in den Tabellen: drei Zeilen à drei Zeichen, `X` = CROSS, `O` = CIRCLE, `.` = leer.

## Hilfsstrukturen

| Klasse | Zweck | Beispiel |
|---|---|---|
| `Boards` | Brett so schreiben, wie es aussieht | `board("XO.", ".X.", "..O")` |
| `BoardAssert` | Custom AssertJ Assertion mit lesbarer Fehlermeldung | `assertThatBoard(b).isWonBy(CROSS).isNotWonBy(CIRCLE)` |
| `ScriptedPlayer` | Deterministischer Stub-Spieler | `ScriptedPlayer.playing(0, 4, 8)` |

## `TicTacToeMainTest.IsWin`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `given_fullLineOfOneColor_..._then_onlyThatColorWins` (16 Fälle: 2 Farben × 8 Linien) | Linie komplett mit einer Farbe belegt | `isWin` für beide Farben | nur diese Farbe gewinnt |
| `given_lineWithOneFieldEmptyOrBlocked_..._then_noOneWins` (96 Fälle) | Linie mit 2 von 3 Steinen, 3. Feld leer oder Gegner | `isWin` | niemand gewinnt |
| `given_boardWithoutCompleteLine_..._then_noOneWins` (3 Fälle) | leeres Brett, volles Remis-Brett, gemischte Diagonale | `isWin` | niemand gewinnt |

## `TicTacToeMainTest.Play`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `given_scriptedPlayers_..._then_returnsWinnerOrNullOnDraw` (4 Fälle) | Stub-Spieler mit festen Zügen: X gewinnt, O gewinnt, X gewinnt im 9. Zug, Remis | `play` | `CROSS` / `CIRCLE` / `CROSS` / `null` |
| `given_playerChoosesInvalidPosition_..._then_throwsIllegalStateException` (3 Fälle) | Zug auf `-1`, `9` oder besetztes Feld `4` | `play` | `IllegalStateException` "cannot play to position N" |
| `given_samePlayerInstanceForBothSides_..._then_throwsIllegalArgumentException` | dieselbe Instanz für X und O | `play` | `IllegalArgumentException` "players must differ" |

## `TicTacToeMainTest.ToString`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `..._then_showsStonesAndIndicesOfFreeFields` | `X..` / `...` / `..O` | `toString` (ohne ANSI-Codes) | `X  1  2`, `3  4  5`, `6  7  O` |
| `..._then_stonesAreBoldAndFreeFieldsGrey` | dasselbe Brett | `toString` | Steine fett (`\033[1m`), freie Felder grau (`\033[37m`) |

## `TicTacToeConsoleTest` (Pioneer `@StdIo`)

| Test | GIVEN (stdin) | WHEN | THEN (stdout) |
|---|---|---|---|
| `given_humanCompletesLeftColumn_..._announcesCrossAsWinner` | `0`, `3`, `6` | `main` | 3 Prompts, letzte Zeile "...and the winner is: CROSS" |
| `given_humanIgnoresTopRow_..._announcesCircleAsWinner` | `8`, `7`, `5` | `main` | letzte Zeile "...and the winner is: CIRCLE" |
| `given_humanEntersPositionOutsideBoard_..._printsBoardAndFails` | `9` | `main` | Prompt ausgegeben, `IllegalStateException` |
| `given_invalidMove_..._printsBoardBeforeFailing` | Stubs spielen beide `0` | `play` | Ausgabe beginnt mit dem Brett `X..` |
| `given_gameEndingInDraw_..._printsDraw` | Stubs spielen Remis | `play` | einzige Zeile "it's a draw!" |

## `StoneTest`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `given_stone_..._then_returnsTheOtherColor` (2 Fälle) | `CROSS` / `CIRCLE` | `opponent()` | `CIRCLE` / `CROSS` |

## `GreedyPlayerTest`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `given_board_..._then_takesFirstFreeField` (4 Fälle) | leeres / teilweise belegtes Brett | `play` | erstes freies Feld (0, 2, 6, 8) |
| `given_fullBoard_..._then_throwsIllegalStateException` | volles Brett | `play` | `IllegalStateException` "cannot play at all" |

## `HumanPlayerTest` (Pioneer `@StdIo`)

| Test | GIVEN (stdin) | WHEN | THEN |
|---|---|---|---|
| `given_positionOnStdIn_..._then_returnsThatPosition` | `4` | `play` | `4`, Eingabe wurde gelesen |
| `given_board_..._then_printsBoardAndPromptOnStdOut` | `4` | `play` | 3 Brett-Zeilen + Prompt "where to to put the next CIRCLE? (0-8): " |
| `given_severalLinesOnStdIn_..._then_readsOneLinePerMove` | `0`, `4`, `8` | 3× `play` | `0`, `4`, `8` (Regressionstest: früher ging Eingabe verloren) |
| `given_nonNumericInput_..._then_throwsNumberFormatException` | `not a number` | `play` | `NumberFormatException` |

## `PerfectPlayerTest`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `given_board_..._then_choosesBestMove` (5 Fälle) | Gewinn möglich / Gewinn statt Blocken / Blocken nötig / Gabel droht / letztes Feld | `play` | 2 / 5 / 2 / 1 / 8 |
| `given_board_..._then_boardIsNotModified` | Brett `X..` / `.O.` / `...` | `play` | Brett unverändert |
| `given_fullBoard_..._then_throwsIllegalStateException` | volles Brett | `play` | `IllegalStateException` |
| `given_twoPerfectPlayers_..._then_itIsADraw` | 2 perfekte Spieler | `TicTacToeMain.play` | Remis |
| `given_greedyOpponent_..._then_perfectPlayerWins` (2 Fälle) | perfekt als X / als O gegen Greedy | `TicTacToeMain.play` | perfekter Spieler gewinnt |
| `given_everyPossibleOpponent_..._then_perfectPlayerNeverLoses` (2 Fälle) | jeder mögliche Spielverlauf des Gegners | perfekter Spieler antwortet | Gegner gewinnt nie |

## Cucumber: `perfect_player.feature`

Feature-Datei: `src/test/resources/ch/bbw/m450/tictactoe/cucumber/perfect_player.feature`,
Steps: `PerfectPlayerSteps`, Runner: `CucumberSuite` (läuft automatisch mit `./gradlew test`).

| Szenario | Fälle |
|---|---|
| Chooses the best move on a given board | 4 (gewinnen, gewinnen statt blocken, blocken, Gabel verhindern) |
| Beats the greedy player | 2 (als CROSS, als CIRCLE) |
| Two perfect players always draw | 1 |
