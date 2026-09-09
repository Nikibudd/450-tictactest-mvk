# TicTacToe Test Concept

This document describes the test concept for the TicTacToe project. It covers what is tested, how it is tested, the tools used and which parts of the code are currently covered by automated tests.

## 1. Purpose and Scope

The goal of the test concept is to make sure the core game logic in `TicTacToeMain` behaves correctly, independent of any concrete player implementation or console input/output. The tests focus on pure, deterministic logic that can be verified without user interaction.

In scope:

- Win detection logic (`isWin`), including all possible winning lines
- Input validation of the game loop (`play`)

Out of scope (not covered by automated tests):

- Console output formatting (`toString`)
- `HumanPlayer` (depends on console input)
- `GreedyPlayer` (concrete move selection strategy)
- Full game flow of `play` (a complete game from start to finish)

## 2. Test Object

Package under test: `ch.bbw.m450.tictactoe`

Main classes:

- `TicTacToeMain` contains the game logic (`isWin`, `play`, `toString`) and the `main` entry point.
- `TicTacToePlayer` is the interface every player implementation has to fulfill (`HumanPlayer`, `GreedyPlayer`).

## 3. Tools and Framework

- Test runner: JUnit 5 (Jupiter), including the parameterized tests extension (`junit-jupiter-params`)
- Assertions: AssertJ (`org.assertj.core.api.WithAssertions`)
- Build tool: Gradle (task `test`, configured with `useJUnitPlatform()`)
- Test class: `src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java`

Tests are executed with:

```
./gradlew test
```

## 4. Test Strategy

Tests are written against the static methods of `TicTacToeMain`. The naming pattern follows GIVEN / WHEN / THEN, for example:

```
given_boardWithWinningLineOfCross_when_isWinIsChecked_then_returnsTrue
```

- GIVEN describes the starting state (usually a board array).
- WHEN describes the method call under test.
- THEN describes the expected result.

Where the same check has to be repeated for multiple inputs, a `@ParameterizedTest` with a `@MethodSource` is used instead of writing one test method per case. This applies to the win detection test, which is run once for every one of the 8 possible winning lines on the board.

Two private helper methods keep the test data setup short and readable:

- `emptyBoard()` returns a fresh board with all 9 fields set to `null`.
- `boardWithStonesAt(color, positions...)` returns a board where the given positions are filled with the given color, all other fields stay empty.

No mocking framework is used. Where a `TicTacToePlayer` instance is needed, the existing `GreedyPlayer` implementation is reused directly instead of creating a test double, since only reference identity matters for the test in question.

## 5. Existing Test Cases

| # | Test method | Given | When | Then |
|---|---|---|---|---|
| 0 | `dummyTest` | no precondition | `assertTrue(true)` is evaluated | test always passes, confirms JUnit 5 is set up correctly |
| 1 | `given_boardWithWinningLineOfCross_when_isWinIsChecked_then_returnsTrue` (parameterized, 8 cases) | a board where one of the 8 possible winning lines (3 rows, 3 columns, 2 diagonals) is fully occupied by CROSS, all other fields empty | `isWin(board, Stone.CROSS)` is called | returns `true` for every one of the 8 lines |
| 2 | `given_emptyBoard_when_isWinIsChecked_then_returnsFalse` | completely empty board (all 9 fields `null`) | `isWin(board, color)` is called for CROSS and for CIRCLE | returns `false` in both cases, no line exists |
| 3 | `given_samePlayerInstanceForBothSides_when_playIsCalled_then_throwsIllegalArgumentException` | the same `TicTacToePlayer` instance (`GreedyPlayer`) is passed as `xPlayer` and as `oPlayer` | `play(player, player)` is called | throws `IllegalArgumentException` with message `"players must differ"` |

The 8 winning line cases provided by `allWinningLines()` are:

| Line | Positions | Description |
|---|---|---|
| 1 | 0, 1, 2 | top row |
| 2 | 3, 4, 5 | middle row |
| 3 | 6, 7, 8 | bottom row |
| 4 | 0, 3, 6 | left column |
| 5 | 1, 4, 7 | middle column |
| 6 | 2, 5, 8 | right column |
| 7 | 0, 4, 8 | diagonal top left to bottom right |
| 8 | 2, 4, 6 | diagonal top right to bottom left |

## 6. Coverage Assessment

`isWin` is fully covered for all 8 winning lines that exist on the board, plus the negative case of no line at all (empty board, checked for both colors). This is a complete coverage of every branch inside `isWin`.

`play` is only covered for its input validation (rejecting identical player instances). The actual game loop, the alternating turns, the win detection during play and the draw case are not covered by automated tests yet.

## 7. Open Points / Suggested Additions

These are gaps identified while reviewing the current test suite, not yet implemented:

- A test for `play` that results in a draw (no winner after 9 rounds).
- A test for `play` that results in a win, using two deterministic stub players.
- A test for `play` where a player returns an out of range or already occupied position, expecting an `IllegalStateException`.
- A test verifying `Stone.opponent()` returns the correct opposite value for both `CROSS` and `CIRCLE`.
- A test verifying `isWin` returns `false` on a board that is full but has no winning line (draw situation).
