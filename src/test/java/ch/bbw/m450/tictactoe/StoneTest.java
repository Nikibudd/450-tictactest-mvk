package ch.bbw.m450.tictactoe;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class StoneTest implements WithAssertions {

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			"CROSS, CIRCLE",
			"CIRCLE, CROSS",
	})
	void given_stone_when_opponentIsRequested_then_returnsTheOtherColor(Stone stone, Stone expectedOpponent) {
		assertThat(stone.opponent()).isEqualTo(expectedOpponent);
	}
}
