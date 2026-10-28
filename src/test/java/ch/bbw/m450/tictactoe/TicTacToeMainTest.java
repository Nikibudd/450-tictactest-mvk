package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.BoardAssert.assertThatBoard;
import static ch.bbw.m450.tictactoe.Boards.board;
import static ch.bbw.m450.tictactoe.Boards.boardWithStonesAt;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeMainTest implements WithAssertions {

	/**
	 * All 8 lines that win a game, named for readable test output.
	 */
	enum Line {
		TOP_ROW(0, 1, 2),
		MIDDLE_ROW(3, 4, 5),
		BOTTOM_ROW(6, 7, 8),
		LEFT_COLUMN(0, 3, 6),
		MIDDLE_COLUMN(1, 4, 7),
		RIGHT_COLUMN(2, 5, 8),
		DIAGONAL(0, 4, 8),
		ANTI_DIAGONAL(2, 4, 6);

		final int[] positions;

		Line(int... positions) {
			this.positions = positions;
		}
	}

	@Nested
	class IsWin {

		@ParameterizedTest(name = "{0} on {1}")
		@MethodSource("ch.bbw.m450.tictactoe.TicTacToeMainTest#everyColorOnEveryLine")
		void given_fullLineOfOneColor_when_isWinIsChecked_then_onlyThatColorWins(Stone color, Line line) {
			var board = boardWithStonesAt(color, line.positions);

			assertThatBoard(board)
					.isWonBy(color)
					.isNotWonBy(color.opponent());
		}

		@ParameterizedTest(name = "{0} on {1}, position {2} is {3}")
		@MethodSource("ch.bbw.m450.tictactoe.TicTacToeMainTest#everyNearlyCompleteLine")
		void given_lineWithOneFieldEmptyOrBlocked_when_isWinIsChecked_then_noOneWins(Stone color, Line line,
				int gap, Stone gapStone) {
			var board = boardWithStonesAt(color, line.positions);
			board[gap] = gapStone;

			assertThatBoard(board).hasNoWinner();
		}

		@ParameterizedTest(name = "{0} | {1} | {2}")
		@CsvSource({
				"..., ..., ...", // empty
				"XOX, XOO, OXX", // full, draw
				"XO., OX., ..O", // mixed diagonal
		})
		void given_boardWithoutCompleteLine_when_isWinIsChecked_then_noOneWins(String top, String middle,
				String bottom) {
			assertThatBoard(board(top, middle, bottom)).hasNoWinner();
		}
	}

	@Nested
	class ToString {

		@Test
		void given_partiallyPlayedBoard_when_toString_then_showsStonesAndIndicesOfFreeFields() {
			var rendered = TicTacToeMain.toString(board("X..", "...", "..O"));

			assertThat(withoutAnsiCodes(rendered)).isEqualTo("""
					X  1  2 \s
					3  4  5 \s
					6  7  O \s
					""");
		}

		@Test
		void given_partiallyPlayedBoard_when_toString_then_stonesAreBoldAndFreeFieldsGrey() {
			var rendered = TicTacToeMain.toString(board("X..", "...", "..O"));

			assertThat(rendered).contains("\033[1mX\033[0m", "\033[1mO\033[0m", "\033[37m1\033[0m");
		}

		private static String withoutAnsiCodes(String text) {
			return text.replaceAll("\033\\[\\d+m", "");
		}
	}

	@Nested
	class Play {

		@ParameterizedTest(name = "X plays {0}, O plays {1} -> {3}")
		@MethodSource("ch.bbw.m450.tictactoe.TicTacToeMainTest#finishedGames")
		void given_scriptedPlayers_when_playIsCalled_then_returnsWinnerOrNullOnDraw(int[] xMoves, int[] oMoves,
				Stone expectedWinner, String description) {
			var winner = TicTacToeMain.play(ScriptedPlayer.playing(xMoves), ScriptedPlayer.playing(oMoves));

			assertThat(winner).as(description).isEqualTo(expectedWinner);
		}

		@ParameterizedTest(name = "X plays {0}, O plays {1}")
		@MethodSource("ch.bbw.m450.tictactoe.TicTacToeMainTest#invalidMoves")
		void given_playerChoosesInvalidPosition_when_playIsCalled_then_throwsIllegalStateException(int[] xMoves,
				int[] oMoves, int invalidPosition) {
			var xPlayer = ScriptedPlayer.playing(xMoves);
			var oPlayer = ScriptedPlayer.playing(oMoves);

			assertThatIllegalStateException()
					.isThrownBy(() -> TicTacToeMain.play(xPlayer, oPlayer))
					.withMessage("cannot play to position %d", invalidPosition);
		}

		@Test
		void given_samePlayerInstanceForBothSides_when_playIsCalled_then_throwsIllegalArgumentException() {
			var player = ScriptedPlayer.playing();

			assertThatIllegalArgumentException()
					.isThrownBy(() -> TicTacToeMain.play(player, player))
					.withMessage("players must differ");
		}
	}

	static Stream<Arguments> everyColorOnEveryLine() {
		return Arrays.stream(Stone.values())
				.flatMap(color -> Arrays.stream(Line.values()).map(line -> arguments(color, line)));
	}

	static Stream<Arguments> everyNearlyCompleteLine() {
		return Arrays.stream(Stone.values()).flatMap(color -> Arrays.stream(Line.values()).flatMap(line ->
				IntStream.of(line.positions).boxed().flatMap(gap -> Stream.of(
						arguments(color, line, gap, null),
						arguments(color, line, gap, color.opponent())))));
	}

	static Stream<Arguments> finishedGames() {
		return Stream.of(
				arguments(new int[]{0, 1, 2}, new int[]{3, 4}, Stone.CROSS, "X wins with the top row"),
				arguments(new int[]{0, 1, 8}, new int[]{3, 4, 5}, Stone.CIRCLE, "O wins with the middle row"),
				arguments(new int[]{0, 2, 4, 7, 8}, new int[]{1, 3, 5, 6}, Stone.CROSS, "X wins with the 9th move"),
				arguments(new int[]{0, 1, 5, 6, 8}, new int[]{2, 3, 4, 7}, null, "draw"));
	}

	static Stream<Arguments> invalidMoves() {
		return Stream.of(
				arguments(new int[]{-1}, new int[]{}, -1),
				arguments(new int[]{9}, new int[]{}, 9),
				arguments(new int[]{4}, new int[]{4}, 4));
	}
}
