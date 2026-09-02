# TicTacToe – Test-Dokumentation

Dieses Dokument beschreibt die Unit-Tests für das TicTacToe-Projekt nach dem
**GIVEN / WHEN / THEN**-Pattern.

Getestet wird mit **JUnit 5** (Jupiter) und **AssertJ**
(`org.assertj.core.api.WithAssertions`).

Testklasse: `src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java`

## 0. `dummyTest`

| | |
|---|---|
| **GIVEN** | Kein Vorzustand nötig. |
| **WHEN** | `assertTrue(true)` wird ausgewertet. |
| **THEN** | Der Test besteht immer – dient als Nachweis, dass JUnit 5 korrekt eingerichtet ist. |

## 1. `given_boardWithWinningLineOfCross_when_isWinIsChecked_then_returnsTrue` (parametrisiert)

Ein `@ParameterizedTest` mit `@MethodSource`, der über alle 8 möglichen
Gewinnlinien (3 Reihen, 3 Spalten, 2 Diagonalen) läuft.

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem genau eine Gewinnlinie (z. B. Felder 0, 1, 2 für die oberste Reihe) vollständig mit `CROSS` belegt ist, alle übrigen Felder sind leer. Die Testfälle sind: `{0,1,2}`, `{3,4,5}`, `{6,7,8}` (Reihen), `{0,3,6}`, `{1,4,7}`, `{2,5,8}` (Spalten), `{0,4,8}`, `{2,4,6}` (Diagonalen). |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CROSS)` wird für jede der 8 Gewinnlinien aufgerufen. |
| **THEN** | Die Methode gibt in jedem der 8 Fälle `true` zurück. |

## 2. `given_emptyBoard_when_isWinIsChecked_then_returnsFalse`

| | |
|---|---|
| **GIVEN** | Ein komplett leeres Spielbrett (alle 9 Felder `null`). |
| **WHEN** | `TicTacToeMain.isWin(board, color)` wird für `CROSS` und für `CIRCLE` aufgerufen. |
| **THEN** | Die Methode gibt in beiden Fällen `false` zurück, da keine Dreierreihe existiert. |

## 3. `given_samePlayerInstanceForBothSides_when_playIsCalled_then_throwsIllegalArgumentException`

| | |
|---|---|
| **GIVEN** | Dieselbe `TicTacToePlayer`-Instanz (`GreedyPlayer`) wird sowohl als `xPlayer` als auch als `oPlayer` übergeben. |
| **WHEN** | `TicTacToeMain.play(player, player)` wird aufgerufen. |
| **THEN** | Es wird eine `IllegalArgumentException` mit der Meldung `"players must differ"` geworfen. |
