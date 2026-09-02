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

    @Test
    void given_topRowFullOfCross_when_isWinIsChecked_then_returnsTrue() {
        var board = boardWithStonesAt(Stone.CROSS, 0, 1, 2);

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isTrue();
    }

    @Test
    void given_middleColumnFullOfCircle_when_isWinIsChecked_then_returnsTrue() {
        var board = boardWithStonesAt(Stone.CIRCLE, 1, 4, 7);

        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isTrue();
    }

    @Test
    void given_diagonalFullOfCross_when_isWinIsChecked_then_returnsTrue() {
        var board = boardWithStonesAt(Stone.CROSS, 0, 4, 8);

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isTrue();
    }

    @Test
    void given_emptyBoard_when_isWinIsChecked_then_returnsFalse() {
        var board = emptyBoard();

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isFalse();
        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isFalse();
    }

    @Test
    void given_samePlayerInstanceForBothSides_when_playIsCalled_then_throwsIllegalArgumentException() {
        var player = new GreedyPlayer();

        assertThatThrownBy(() -> TicTacToeMain.play(player, player))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("players must differ");
    }

    private static Stone[] emptyBoard() {
        return new Stone[TicTacToeMain.BOARD_SIZE];
    }

    private static Stone[] boardWithStonesAt(Stone color, int... positions) {
        var board = emptyBoard();
        for (var position : positions) {
            board[position] = color;
        }
        return board;
    }
}
