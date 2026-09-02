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

## 1. `isWin_detectsWinInTopRow`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die oberste Reihe (Felder 0, 1, 2) vollständig mit `CROSS` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CROSS)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine horizontale Dreierreihe vorliegt. |

## 2. `isWin_detectsWinInMiddleColumn`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die mittlere Spalte (Felder 1, 4, 7) vollständig mit `CIRCLE` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CIRCLE)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine vertikale Dreierreihe vorliegt. |

## 3. `isWin_detectsWinInDiagonal`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die Diagonale von oben-links nach unten-rechts (Felder 0, 4, 8) vollständig mit `CROSS` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CROSS)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine diagonale Dreierreihe vorliegt. |

## 4. `isWin_returnsFalseOnEmptyBoard`

| | |
|---|---|
| **GIVEN** | Ein komplett leeres Spielbrett (alle 9 Felder `null`). |
| **WHEN** | `TicTacToeMain.isWin(board, color)` wird für `CROSS` und für `CIRCLE` aufgerufen. |
| **THEN** | Die Methode gibt in beiden Fällen `false` zurück, da keine Dreierreihe existiert. |

## 5. `play_throwsIllegalArgumentException_whenSamePlayerInstancePlaysBothSides`

| | |
|---|---|
| **GIVEN** | Dieselbe `TicTacToePlayer`-Instanz (`GreedyPlayer`) wird sowohl als `xPlayer` als auch als `oPlayer` übergeben. |
| **WHEN** | `TicTacToeMain.play(player, player)` wird aufgerufen. |
| **THEN** | Es wird eine `IllegalArgumentException` mit der Meldung `"players must differ"` geworfen. |
