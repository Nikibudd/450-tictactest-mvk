package ch.bbw.m450.tictactoe;

import java.util.stream.Stream;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TicTacToeMainTest implements WithAssertions {
    @Test
    void dummyTest() {
        assertTrue(true);
    }

    @ParameterizedTest(name = "winning line {0}")
    @MethodSource("allWinningLines")
    void given_boardWithWinningLineOfCross_when_isWinIsChecked_then_returnsTrue(int[] winningLine) {
        var board = boardWithStonesAt(Stone.CROSS, winningLine);

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

    private static Stream<int[]> allWinningLines() {
        return Stream.of(
                new int[]{0, 1, 2}, // top row
                new int[]{3, 4, 5}, // middle row
                new int[]{6, 7, 8}, // bottom row
                new int[]{0, 3, 6}, // left column
                new int[]{1, 4, 7}, // middle column
                new int[]{2, 5, 8}, // right column
                new int[]{0, 4, 8}, // diagonal top-left to bottom-right
                new int[]{2, 4, 6}  // diagonal top-right to bottom-left
        );
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
