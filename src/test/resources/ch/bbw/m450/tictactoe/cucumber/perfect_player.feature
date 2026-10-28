Feature: Perfect player
  The perfect player never loses a game and takes every chance to win.

  Scenario Outline: Chooses the best move on a given board
    Given the board
      | <top>    |
      | <middle> |
      | <bottom> |
    When the perfect player places a <color>
    Then it plays position <position>

    Examples:
      | top | middle | bottom | color  | position | why                             |
      | XX. | OO.    | ...    | CROSS  | 2        | takes the winning field         |
      | XX. | OO.    | ...    | CIRCLE | 5        | wins instead of blocking        |
      | OO. | X..    | ..X    | CROSS  | 2        | blocks the opponent             |
      | X.. | .O.    | ..X    | CIRCLE | 1        | prevents a fork of the opponent |

  Scenario Outline: Beats the greedy player
    When the perfect player as <color> plays against the greedy player
    Then the winner is <color>

    Examples:
      | color  |
      | CROSS  |
      | CIRCLE |

  Scenario: Two perfect players always draw
    When two perfect players play against each other
    Then the game ends in a draw
