package ch.bbw.m450.tictactoe.players;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Simple human-player taking input from stdin.
 */
@SuppressWarnings("java:S106") // a console game: stdout is the user interface, not logging
public class HumanPlayer implements TicTacToePlayer {

	// one scanner for the whole game: a Scanner buffers ahead, so a new one per move would lose input
	private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		System.out.println(TicTacToeMain.toString(board) + "where to to put the next " + colorToPlay + "? (0-8): ");
		return Integer.parseInt(scanner.nextLine());
	}
}
