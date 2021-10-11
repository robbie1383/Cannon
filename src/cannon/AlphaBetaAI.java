package cannon;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;
import main.collections.FastArrayList;
import metadata.ai.heuristics.Heuristics;
import other.AI;
import other.action.Action;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

public class AlphaBetaAI extends AI
{

    protected int player = -1;
    Heuristics heuristics;

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

        return this.NegaMax(game, context, 5, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player).getMove();
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        this.player = playerID;
        this.heuristics = Heuristics.copy(game.metadata().ai().heuristics());
        this.heuristics.init(game);
    }

    public MoveValue NegaMax(Game game, Context state, int depth, double alpha, double beta, int player) {
        System.out.println("depth : " + depth);
        Move bestMove = null;
        if(depth == 0 || !state.active(player) || !state.active(switchPlayer(player))) {
            System.out.println("heuristic : " + (double) heuristics.computeValue(state, player, Float.valueOf(0.001f)));
            return new MoveValue(null, (double) heuristics.computeValue(state, player, Float.valueOf(0.001f)));
        }
        double score = Double.NEGATIVE_INFINITY;
        FastArrayList<Move>  legalMoves = AIUtils.extractMovesForMover(game.moves(state).moves(), player);
        System.out.println("possible moves :" + legalMoves.size());
        for (Move move : legalMoves) {
            Context copyState = copyContext(state);
            game.apply(copyState, move);
            double value = -this.NegaMax(game, copyState, depth - 1, -beta, -alpha, this.switchPlayer(player)).getScore();
            if (value > score) {
                score = value;
                bestMove = move;
            }
            System.out.println("score : " + score);
            if (score > alpha) alpha = score;
            if (score >= beta)
                break;
        }
        System.out.println("best score : " + score);
        return new MoveValue(bestMove, score);
    }

    private int switchPlayer(int player) {
        if (player == 1)
            return 2;
        return 1;
    }
}
