package cannon;

import game.Game;
import main.collections.FastArrayList;
import metadata.ai.heuristics.Heuristics;
import other.AI;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

import java.util.ArrayList;

public class AlphaBetaAI extends AI
{
    int time = 600000;
    protected int player = -1;
    Heuristics heuristics;
    TranspositionTableEntry[] transpositionTable = new TranspositionTableEntry[1 << 12];

    public AlphaBetaAI()
    {
        this.friendlyName = "Alpha-Beta AI";
    }

    @Override
    public Move selectAction
            (
                    final Game game,
                    final Context context,
                    final double maxSeconds,
                    final int maxIterations,
                    final int maxDepth

            )
    {
        long start = System.currentTimeMillis();
        Move move = this.NegaMax(game, context, 4, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player).getMove();
        long end = System.currentTimeMillis();
        time -= (end - start);
        System.out.println("Time left : " + time + " for player "+ this.player);
        return move;
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        this.player = playerID;
        this.heuristics = Heuristics.copy(game.metadata().ai().heuristics());
        this.heuristics.init(game);
    }

    public MoveValue NegaMax(Game game, Context state, int depth, double alpha, double beta, int player) {

        // Lookup transposition table
        double oldAlpha = alpha;
        TranspositionTableEntry entry = this.lookUp(state);
        if (entry != null && entry.depth <= depth) {
            if (entry.flag.equals("exact")) return new MoveValue(entry.bestMove, entry.value);
            if (entry.flag.equals("upper")) beta = Math.min(beta, entry.value);
            if (entry.flag.equals("lower")) alpha = Math.max(alpha, entry.value);
            if (alpha >= beta)
                return new MoveValue(entry.bestMove, entry.value);
        }

        // Start alpha-beta
        // If leaf node
        if(depth == 0 || !state.active(player) || !state.active(switchPlayer(player))) {
            double heuristic = (double) (heuristics.computeValue(state, player, Float.valueOf(0.01f)) - heuristics.computeValue(state, this.switchPlayer(player), Float.valueOf(0.01f)));
            if (!state.active(switchPlayer(player)) && state.winners().contains(switchPlayer(player)))
                heuristic -= 10000.f;

            return new MoveValue(null, heuristic);
        }

        // If internal node
        double score = Double.NEGATIVE_INFINITY;
        FastArrayList<Move>  legalMoves = AIUtils.extractMovesForMover(game.moves(state).moves(), player);
        Move bestMove = legalMoves.get(0);

        for (Move move : legalMoves) {
            Context copyState = copyContext(state);
            game.apply(copyState, move);
            double value = -this.NegaMax(game, copyState, depth - 1, -beta, -alpha, this.switchPlayer(player)).getScore();
            if (value > score) {
                score = value;
                bestMove = move;
            }
            if (score > alpha) alpha = score;
            if (alpha >= beta)
                break;
        }

        // Store node in transposition table
        String flag;
        if (score <= oldAlpha) flag = "upper";
        else if (score >= beta) flag = "lower";
        else flag = "exact";

        //if (entry == null)
            this.addToTT(state, new TranspositionTableEntry(score, flag, bestMove, depth, state.state().fullHash()));

        //if (entry != null && entry.depth > depth) {
          //  this.addToTT(state, new TranspositionTableEntry(score, flag, bestMove, depth, state.state().fullHash()));
        //}

        return new MoveValue(bestMove, score);
    }

    private TranspositionTableEntry lookUp(Context context) {

        int i = (int) (context.state().fullHash() >>> (Long.SIZE - 12));
        if (this.transpositionTable[i] != null && this.transpositionTable[i].id == context.state().fullHash())
            return this.transpositionTable[i];
        return null;

    }

    private void addToTT(Context context, TranspositionTableEntry t){

        int i = (int) (context.state().fullHash() >>> (Long.SIZE - 12));
        this.transpositionTable[i] = t;
    }

    private int switchPlayer(int player) {
        if (player == 1)
            return 2;
        return 1;
    }
}
