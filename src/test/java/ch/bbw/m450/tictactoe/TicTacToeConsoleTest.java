package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.Boards.board;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

/**
 * End-to-end tests of the console game: moves of the human player come from stdin, the game is checked on stdout.
 * The computer opponent in {@link TicTacToeMain#main(String[])} is the {@code PerfectPlayer}: it never loses.
 */
class TicTacToeConsoleTest implements WithAssertions {

	private static final String[] NO_ARGS = {};

	@Test
	@StdIo({"4", "2", "3", "7", "8"})
	void given_humanPlaysPerfectly_when_mainIsRun_then_endsInDraw(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedLines())
				.endsWith("it's a draw!")
				.filteredOn(line -> line.startsWith("where to"))
				.hasSize(5);
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
	void given_invalidMove_when_playIsCalled_then_printsBoardBeforeFailing(StdOut out) {
		var xPlayer = ScriptedPlayer.playing(0);
		var oPlayer = ScriptedPlayer.playing(0);

		assertThatIllegalStateException().isThrownBy(() -> TicTacToeMain.play(xPlayer, oPlayer));
		assertThat(out.capturedString()).startsWith(TicTacToeMain.toString(board("X..", "...", "...")));
	}

	@Test
	@StdIo
	void given_gameEndingInDraw_when_playIsCalled_then_printsDraw(StdOut out) {
		TicTacToeMain.play(ScriptedPlayer.playing(0, 1, 5, 6, 8), ScriptedPlayer.playing(2, 3, 4, 7));

		assertThat(out.capturedLines()).containsExactly("it's a draw!");
	}
}
