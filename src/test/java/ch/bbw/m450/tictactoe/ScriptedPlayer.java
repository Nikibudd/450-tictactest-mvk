package ch.bbw.m450.tictactoe;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Deterministic test stub: plays the given positions in order, independent of the board.
 */
public final class ScriptedPlayer implements TicTacToePlayer {

	private final Queue<Integer> moves;

	private ScriptedPlayer(int... moves) {
		this.moves = new ArrayDeque<>(Arrays.stream(moves).boxed().toList());
	}

	public static ScriptedPlayer playing(int... moves) {
		return new ScriptedPlayer(moves);
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		var next = moves.poll();
		if (next == null) {
			throw new IllegalStateException("script exhausted, the game should already be over");
		}
		return next;
	}
}
