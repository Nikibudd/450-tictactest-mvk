package ch.bbw.m450.tictactoe.cucumber;

import java.util.List;

import org.assertj.core.api.WithAssertions;

import ch.bbw.m450.tictactoe.Boards;
import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.PerfectPlayer;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for {@code perfect_player.feature}. Cucumber creates a new instance per scenario.
 */
public class PerfectPlayerSteps implements WithAssertions {

	private Stone[] board;
	private int playedPosition;
	private Stone winner;

	@ParameterType("CROSS|CIRCLE")
	public Stone color(String name) {
		return Stone.valueOf(name);
	}

	@Given("the board")
	public void theBoard(List<String> rows) {
		board = Boards.board(rows.toArray(String[]::new));
	}

	@When("the perfect player places a {color}")
	public void thePerfectPlayerPlaces(Stone color) {
		playedPosition = new PerfectPlayer().play(board, color);
	}

	@When("the perfect player as {color} plays against the greedy player")
	public void thePerfectPlayerPlaysAgainstTheGreedyPlayer(Stone color) {
		var perfect = new PerfectPlayer();
		var greedy = new GreedyPlayer();
		winner = color == Stone.CROSS ? TicTacToeMain.play(perfect, greedy) : TicTacToeMain.play(greedy, perfect);
	}

	@When("two perfect players play against each other")
	public void twoPerfectPlayersPlayAgainstEachOther() {
		winner = TicTacToeMain.play(new PerfectPlayer(), new PerfectPlayer());
	}

	@Then("it plays position {int}")
	public void itPlaysPosition(int expectedPosition) {
		assertThat(playedPosition).isEqualTo(expectedPosition);
	}

	@Then("the winner is {color}")
	public void theWinnerIs(Stone expectedWinner) {
		assertThat(winner).isEqualTo(expectedWinner);
	}

	@Then("the game ends in a draw")
	public void theGameEndsInADraw() {
		assertThat(winner).isNull();
	}
}
