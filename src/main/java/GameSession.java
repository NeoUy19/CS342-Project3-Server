import Checkers.Board;
import Checkers.Rules;

public class GameSession {
    Board board;
    Rules rules;

    Players playerOne;
    Players playerTwo;

    public GameSession(Board board,Players playerOne,Players playerTwo) {
        this.board = board;
        rules = new Rules(board);
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
    }
}
