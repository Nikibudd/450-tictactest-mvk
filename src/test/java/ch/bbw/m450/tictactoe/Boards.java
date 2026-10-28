package ch.bbw.m450.tictactoe;

import java.util.Arrays;
import java.util.stream.Collectors;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Test helper to write boards the way they look: {@code board("XO.", ".X.", "..O")}.
 * {@code X} is a cross, {@code O} a circle and {@code .} an empty field.
 */
public final class Boards {

	private static final int ROWS = 3;

	private Boards() {
		// static test helper, not meant to be instantiated
	}

	public static Stone[] emptyBoard() {
		return new Stone[TicTacToeMain.BOARD_SIZE];
	}

	public static Stone[] board(String... rows) {
		if (rows.length != ROWS || Arrays.stream(rows).anyMatch(row -> row.length() != ROWS)) {
			throw new IllegalArgumentException("a board needs 3 rows of 3 fields: " + Arrays.toString(rows));
		}
		var cells = String.join("", rows);
		var board = emptyBoard();
		for (var i = 0; i < board.length; i++) {
			board[i] = toStone(cells.charAt(i));
		}
		return board;
	}

	public static Stone[] boardWithStonesAt(Stone color, int... positions) {
		var board = emptyBoard();
		for (var position : positions) {
			board[position] = color;
		}
		return board;
	}

	/**
	 * Inverse of {@link #board(String...)}, used for readable failure messages.
	 */
	public static String format(Stone[] board) {
		return Arrays.stream(board)
				.map(Boards::toChar)
				.map(String::valueOf)
				.collect(Collectors.joining())
				.replaceAll("(...)(?!$)", "$1\n");
	}

	private static Stone toStone(char field) {
		return switch (field) {
			case 'X' -> Stone.CROSS;
			case 'O' -> Stone.CIRCLE;
			case '.' -> null;
			default -> throw new IllegalArgumentException("unknown field '" + field + "', use X, O or .");
		};
	}

	private static char toChar(Stone stone) {
		if (stone == null) {
			return '.';
		}
		return stone == Stone.CROSS ? 'X' : 'O';
	}
}
