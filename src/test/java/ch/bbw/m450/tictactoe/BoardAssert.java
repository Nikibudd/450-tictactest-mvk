package ch.bbw.m450.tictactoe;

import org.assertj.core.api.AbstractAssert;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Custom AssertJ assertion for boards: {@code assertThatBoard(board).isWonBy(CROSS).isNotWonBy(CIRCLE)}.
 */
public class BoardAssert extends AbstractAssert<BoardAssert, Stone[]> {

	private BoardAssert(Stone[] actual) {
		super(actual, BoardAssert.class);
	}

	public static BoardAssert assertThatBoard(Stone[] actual) {
		return new BoardAssert(actual);
	}

	public BoardAssert isWonBy(Stone color) {
		isNotNull();
		if (!TicTacToeMain.isWin(actual, color)) {
			failWithMessage("Expected board%n%s%nto be won by <%s> but it was not", Boards.format(actual), color);
		}
		return this;
	}

	public BoardAssert isNotWonBy(Stone color) {
		isNotNull();
		if (TicTacToeMain.isWin(actual, color)) {
			failWithMessage("Expected board%n%s%nnot to be won by <%s> but it was", Boards.format(actual), color);
		}
		return this;
	}

	public BoardAssert hasNoWinner() {
		return isNotWonBy(Stone.CROSS).isNotWonBy(Stone.CIRCLE);
	}
}
