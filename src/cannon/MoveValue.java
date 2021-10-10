package cannon;
import other.move.Move;

public class MoveValue {
    Move move;
    double score;

    public MoveValue(Move move, double score)
    {
        this.move = move;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public Move getMove() {
        return move;
    }
}
