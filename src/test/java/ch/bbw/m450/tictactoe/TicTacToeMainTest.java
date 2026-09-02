package ch.bbw.m450.tictactoe;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TicTacToeMainTest implements WithAssertions {
    @Test
    void dummyTest() {
        assertTrue(true);
    }

    // GIVEN a board where the top row is fully occupied by CROSS
    // WHEN isWin is checked for CROSS
    // THEN it returns true
    @Test
    void isWin_detectsWinInTopRow() {
        var board = new Stone[]{
                Stone.CROSS, Stone.CROSS, Stone.CROSS,
                null, null, null,
                null, null, null
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isFalse();
    }

    // GIVEN a board where the middle column is fully occupied by CIRCLE
    // WHEN isWin is checked for CIRCLE
    // THEN it returns true
    @Test
    void isWin_detectsWinInMiddleColumn() {
        var board = new Stone[]{
                null, Stone.CIRCLE, null,
                null, Stone.CIRCLE, null,
                null, Stone.CIRCLE, null
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isTrue();
    }

    // GIVEN a board where the diagonal from top-left to bottom-right is fully occupied by CROSS
    // WHEN isWin is checked for CROSS
    // THEN it returns true
    @Test
    void isWin_detectsWinInDiagonal() {
        var board = new Stone[]{
                Stone.CROSS, null, null,
                null, Stone.CROSS, null,
                null, null, Stone.CROSS
        };

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isTrue();
    }

    // GIVEN an empty board
    // WHEN isWin is checked for either color
    // THEN it returns false
    @Test
    void isWin_returnsFalseOnEmptyBoard() {
        var board = new Stone[TicTacToeMain.BOARD_SIZE];

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isFalse();
        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isFalse();
    }

    // GIVEN the same player instance passed as both xPlayer and oPlayer
    // WHEN play is called
    // THEN an IllegalArgumentException is thrown
    @Test
    void play_throwsIllegalArgumentException_whenSamePlayerInstancePlaysBothSides() {
        var player = new GreedyPlayer();

        assertThatThrownBy(() -> TicTacToeMain.play(player, player))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("players must differ");
    }
}
