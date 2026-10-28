package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.board;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class GreedyPlayerTest implements WithAssertions {

	private final GreedyPlayer player = new GreedyPlayer();

	@ParameterizedTest(name = "{0} | {1} | {2} -> {3}")
	@CsvSource({
			"..., ..., ..., 0",
			"XO., ..., ..., 2",
			"XOX, OXO, .XO, 6",
			"XOX, OXO, XO., 8",
	})
	void given_board_when_playIsCalled_then_takesFirstFreeField(String top, String middle, String bottom,
			int expectedPosition) {
		assertThat(player.play(board(top, middle, bottom), Stone.CROSS)).isEqualTo(expectedPosition);
	}

	@Test
	void given_fullBoard_when_playIsCalled_then_throwsIllegalStateException() {
		var fullBoard = board("XOX", "XOO", "OXX");

		assertThatIllegalStateException()
				.isThrownBy(() -> player.play(fullBoard, Stone.CROSS))
				.withMessage("cannot play at all");
	}
}
