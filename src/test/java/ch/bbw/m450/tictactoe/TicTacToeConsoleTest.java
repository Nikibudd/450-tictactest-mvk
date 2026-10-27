package ch.bbw.m450.tictactoe;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

/**
 * End-to-end tests of the console game: moves of the human player come from stdin, the game is checked on stdout.
 * The computer opponent in {@link TicTacToeMain#main(String[])} is the {@code GreedyPlayer} (first free field).
 */
class TicTacToeConsoleTest implements WithAssertions {

	private static final String[] NO_ARGS = {};

	@Test
	@StdIo({"0", "3", "6"})
	void given_humanCompletesLeftColumn_when_mainIsRun_then_announcesCrossAsWinner(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedLines())
				.endsWith("...and the winner is: CROSS")
				.filteredOn(line -> line.startsWith("where to"))
				.hasSize(3);
	}

	@Test
	@StdIo({"8", "7", "5"})
	void given_humanIgnoresTopRow_when_mainIsRun_then_announcesCircleAsWinner(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedLines()).endsWith("...and the winner is: CIRCLE");
	}

	@Test
	@StdIo("9")
	void given_humanEntersPositionOutsideBoard_when_mainIsRun_then_printsBoardAndFails(StdOut out) {
		assertThatIllegalStateException()
				.isThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.withMessage("cannot play to position 9");
		assertThat(out.capturedString()).contains("where to to put the next CROSS? (0-8): ");
	}

	@Test
	@StdIo
	void given_gameEndingInDraw_when_playIsCalled_then_printsDraw(StdOut out) {
		TicTacToeMain.play(ScriptedPlayer.playing(0, 1, 5, 6, 8), ScriptedPlayer.playing(2, 3, 4, 7));

		assertThat(out.capturedLines()).containsExactly("it's a draw!");
	}
}
