package cannon;
import other.move.Move;
import other.context.Context;
import other.state.owned.Owned;
import other.location.Location;

import java.util.List;

public class TranspositionTableEntry {

    double value;
    String flag;
    Move bestMove;
    int depth;
    long id;

    public TranspositionTableEntry(double value, String flag, Move move, int depth, long id) {
        this.value = value;
        this.flag = flag;
        this.bestMove = move;
        this.depth = depth;
        this.id = id;
    }

    public int computeID(Context context) {
        return context.hashCode();
    }

}
