package ch.bbw.m450.tictactoe.players;

import java.util.Arrays;
import java.util.stream.IntStream;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Perfect player using the minimax algorithm: it never loses and wins whenever the opponent makes a mistake.
 * Scores are weighted by search depth, so a quick win is preferred over a later one (and a late loss over an early
 * one). On equal scores the lowest position wins, which keeps the player deterministic.
 */
public class PerfectPlayer implements TicTacToePlayer {

	private static final int WIN_SCORE = 10;

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		var copy = Arrays.copyOf(board, board.length);
		var bestMove = -1;
		var bestScore = Integer.MIN_VALUE;
		for (var position : freePositions(copy)) {
			copy[position] = colorToPlay;
			var score = scoreFor(copy, colorToPlay, 1);
			copy[position] = null;
			if (score > bestScore) {
				bestScore = score;
				bestMove = position;
			}
		}
		if (bestMove < 0) {
			throw new IllegalStateException("cannot play at all");
		}
		return bestMove;
	}

	/**
	 * @return how good the board is for the player who just placed a stone, assuming both sides play perfectly
	 */
	private static int scoreFor(Stone[] board, Stone justPlayed, int depth) {
		if (TicTacToeMain.isWin(board, justPlayed)) {
			return WIN_SCORE - depth;
		}
		var opponent = justPlayed.opponent();
		var bestForOpponent = Integer.MIN_VALUE;
		for (var position : freePositions(board)) {
			board[position] = opponent;
			bestForOpponent = Math.max(bestForOpponent, scoreFor(board, opponent, depth + 1));
			board[position] = null;
		}
		// no free field left and nobody won: draw
		return bestForOpponent == Integer.MIN_VALUE ? 0 : -bestForOpponent;
	}

	private static int[] freePositions(Stone[] board) {
		return IntStream.range(0, board.length)
				.filter(position -> board[position] == null)
				.toArray();
	}
}
