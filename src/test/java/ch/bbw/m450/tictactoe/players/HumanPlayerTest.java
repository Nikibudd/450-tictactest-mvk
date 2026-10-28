package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.board;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIn;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class HumanPlayerTest implements WithAssertions {

	@Test
	@StdIo("4")
	void given_positionOnStdIn_when_playIsCalled_then_returnsThatPosition(StdIn in) {
		var position = new HumanPlayer().play(board("X..", "...", "..."), Stone.CIRCLE);

		assertThat(position).isEqualTo(4);
		assertThat(in.capturedLines()).containsExactly("4");
	}

	@Test
	@StdIo("4")
	void given_board_when_playIsCalled_then_printsBoardAndPromptOnStdOut(StdOut out) {
		new HumanPlayer().play(board("X..", "...", "..."), Stone.CIRCLE);

		assertThat(out.capturedLines())
				.hasSize(4)
				.endsWith("where to to put the next CIRCLE? (0-8): ");
	}

	@Test
	@StdIo({"0", "4", "8"})
	void given_severalLinesOnStdIn_when_playIsCalledRepeatedly_then_readsOneLinePerMove() {
		var player = new HumanPlayer();
		var board = board("...", "...", "...");

		assertThat(new int[]{
				player.play(board, Stone.CROSS),
				player.play(board, Stone.CROSS),
				player.play(board, Stone.CROSS),
		}).containsExactly(0, 4, 8);
	}

	@Test
	@StdIo("not a number")
	void given_nonNumericInput_when_playIsCalled_then_throwsNumberFormatException() {
		var player = new HumanPlayer();
		var emptyBoard = board("...", "...", "...");

		assertThatExceptionOfType(NumberFormatException.class)
				.isThrownBy(() -> player.play(emptyBoard, Stone.CROSS));
	}
}
