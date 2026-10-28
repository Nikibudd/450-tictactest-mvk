package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.BoardAssert.assertThatBoard;
import static ch.bbw.m450.tictactoe.Boards.board;
import static ch.bbw.m450.tictactoe.Boards.emptyBoard;

import java.util.Arrays;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class PerfectPlayerTest implements WithAssertions {

	private final PerfectPlayer player = new PerfectPlayer();

	@ParameterizedTest(name = "{0} | {1} | {2}, {3} to play -> {4} ({5})")
	@CsvSource({
			"XX., OO., ..., CROSS, 2, takes the winning field",
			"XX., OO., ..., CIRCLE, 5, wins instead of blocking",
			"OO., X.., ..X, CROSS, 2, blocks the opponent",
			"X.., .O., ..X, CIRCLE, 1, prevents a fork of the opponent",
			"XOX, XOO, OX., CROSS, 8, plays the last free field",
	})
	void given_board_when_playIsCalled_then_choosesBestMove(String top, String middle, String bottom,
			Stone colorToPlay, int expectedPosition, String reason) {
		var position = player.play(board(top, middle, bottom), colorToPlay);

		assertThat(position).as(reason).isEqualTo(expectedPosition);
	}

	@Test
	void given_board_when_playIsCalled_then_boardIsNotModified() {
		var board = board("X..", ".O.", "...");
		var original = board.clone();

		player.play(board, Stone.CROSS);

		assertThat(board).containsExactly(original);
	}

	@Test
	void given_fullBoard_when_playIsCalled_then_throwsIllegalStateException() {
		var fullBoard = board("XOX", "XOO", "OXX");

		assertThatIllegalStateException()
				.isThrownBy(() -> player.play(fullBoard, Stone.CROSS))
				.withMessage("cannot play at all");
	}

	@Test
	void given_twoPerfectPlayers_when_theyPlay_then_itIsADraw() {
		assertThat(TicTacToeMain.play(new PerfectPlayer(), new PerfectPlayer())).isNull();
	}

	@ParameterizedTest(name = "perfect player as {0}")
	@EnumSource(Stone.class)
	void given_greedyOpponent_when_theyPlay_then_perfectPlayerWins(Stone perfectColor) {
		var perfect = new PerfectPlayer();
		var greedy = new GreedyPlayer();

		var winner = perfectColor == Stone.CROSS
				? TicTacToeMain.play(perfect, greedy)
				: TicTacToeMain.play(greedy, perfect);

		assertThat(winner).isEqualTo(perfectColor);
	}

	/**
	 * Proof by brute force: walks through every possible game against every possible opponent.
	 */
	@ParameterizedTest(name = "perfect player as {0}")
	@EnumSource(Stone.class)
	void given_everyPossibleOpponent_when_theyPlay_then_perfectPlayerNeverLoses(Stone perfectColor) {
		var gamesPlayed = playAllGames(emptyBoard(), Stone.CROSS, perfectColor);

		// the actual check is inside playAllGames, this only makes sure the walk branched over opponent moves
		assertThat(gamesPlayed).isGreaterThan(1);
	}

	private int playAllGames(Stone[] board, Stone toPlay, Stone perfectColor) {
		var opponent = perfectColor.opponent();
		if (TicTacToeMain.isWin(board, perfectColor) || Arrays.stream(board).noneMatch(field -> field == null)) {
			assertThatBoard(board).isNotWonBy(opponent);
			return 1;
		}
		if (toPlay == perfectColor) {
			var next = board.clone();
			next[player.play(board, perfectColor)] = perfectColor;
			return playAllGames(next, opponent, perfectColor);
		}
		var games = 0;
		for (var position = 0; position < board.length; position++) {
			if (board[position] == null) {
				var next = board.clone();
				next[position] = opponent;
				assertThatBoard(next).isNotWonBy(opponent);
				games += playAllGames(next, perfectColor, perfectColor);
			}
		}
		return games;
	}
}
