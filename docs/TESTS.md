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

## 1. `given_topRowFullOfCross_when_isWinIsChecked_then_returnsTrue`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die oberste Reihe (Felder 0, 1, 2) vollständig mit `CROSS` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CROSS)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine horizontale Dreierreihe vorliegt. |

## 2. `given_middleColumnFullOfCircle_when_isWinIsChecked_then_returnsTrue`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die mittlere Spalte (Felder 1, 4, 7) vollständig mit `CIRCLE` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CIRCLE)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine vertikale Dreierreihe vorliegt. |

## 3. `given_diagonalFullOfCross_when_isWinIsChecked_then_returnsTrue`

| | |
|---|---|
| **GIVEN** | Ein Spielbrett, auf dem die Diagonale von oben-links nach unten-rechts (Felder 0, 4, 8) vollständig mit `CROSS` belegt ist, alle übrigen Felder sind leer. |
| **WHEN** | `TicTacToeMain.isWin(board, Stone.CROSS)` wird aufgerufen. |
| **THEN** | Die Methode gibt `true` zurück, da eine diagonale Dreierreihe vorliegt. |

## 4. `given_emptyBoard_when_isWinIsChecked_then_returnsFalse`

| | |
|---|---|
| **GIVEN** | Ein komplett leeres Spielbrett (alle 9 Felder `null`). |
| **WHEN** | `TicTacToeMain.isWin(board, color)` wird für `CROSS` und für `CIRCLE` aufgerufen. |
| **THEN** | Die Methode gibt in beiden Fällen `false` zurück, da keine Dreierreihe existiert. |

## 5. `given_samePlayerInstanceForBothSides_when_playIsCalled_then_throwsIllegalArgumentException`

| | |
|---|---|
| **GIVEN** | Dieselbe `TicTacToePlayer`-Instanz (`GreedyPlayer`) wird sowohl als `xPlayer` als auch als `oPlayer` übergeben. |
| **WHEN** | `TicTacToeMain.play(player, player)` wird aufgerufen. |
| **THEN** | Es wird eine `IllegalArgumentException` mit der Meldung `"players must differ"` geworfen. |
