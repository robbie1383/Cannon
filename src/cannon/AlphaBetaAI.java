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
        double turn = 0;
        long start = System.currentTimeMillis();
        MoveValue move = this.NegaMax(game, context, 2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player);
        long end = System.currentTimeMillis();
        turn += (end - start);
        int count = 3;
        while (turn < (maxSeconds*1000 - 2000)) {

            start = System.currentTimeMillis();
            move = this.NegaMax(game, context, count, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player);
            end = System.currentTimeMillis();
            turn += (end - start);
            count++;
        }
        time -= turn;
        System.out.println("Depth "+count);
        System.out.println("Turn took : "+ (turn/1000) + " secodn. Time left : " + (time/1000) + " seconds for player "+ this.player);
        System.out.println(("Score :" + move.getScore()));
        return move.getMove();
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
        if (entry != null && entry.depth >= depth) {
            if (entry.flag.equals("exact")) return new MoveValue(entry.bestMove, entry.value);
            if (entry.flag.equals("upper")) beta = Math.min(beta, entry.value);
            if (entry.flag.equals("lower")) alpha = Math.max(alpha, entry.value);
            if (alpha >= beta)
                return new MoveValue(entry.bestMove, entry.value);
        }

        // Start alpha-beta
        // If leaf node
        if(depth == 0 || !state.active(player) || !state.active(switchPlayer(player))) {
            double heuristic = heuristics.computeValue(state, player, Float.valueOf(0.01f)) - heuristics.computeValue(state, this.switchPlayer(player), Float.valueOf(0.01f));
            if (!state.active(switchPlayer(player)) && state.winners().contains(switchPlayer(player)))
                heuristic -= 10000.f;
            if (!state.active(player) && state.winners().contains(player))
                heuristic += 10000.f;
            return new MoveValue(null, heuristic);
        }

        // If internal node
        double score = Double.NEGATIVE_INFINITY;
        FastArrayList<Move> legalMoves = AIUtils.extractMovesForMover(game.moves(state).moves(), player);
        this.order(legalMoves, state, player, game, 0, legalMoves.size()-1);
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

    private void order(FastArrayList<Move> list, Context state, int player, Game game, int low, int high) {

        // Quicksort on the heuristic values.
        int i = low, j = high;
        double pivot = this.getHeuristic(list.get(i + (j-i)/2), state, player, game);
        while (i <= j) {
            while (this.getHeuristic(list.get(i), state, player, game)< pivot) {
                i++;
            }
            while (this.getHeuristic(list.get(j), state, player, game) > pivot) {
                j--;
            }
            if (i <= j) {
                Move aux = list.get(i);
                list.set(i, list.get(j));
                list.set(j, aux);
                i++;
                j--;
            }
        }
        if (low < j)
            this.order(list, state, player, game, low, j);
        if (i < high)
            this.order(list, state, player, game, i, high);

    }

    private double getHeuristic(Move move, Context state, int player, Game game){
        Context copyState = copyContext(state);
        game.apply(copyState, move);
        return heuristics.computeValue(copyState, player, Float.valueOf(0.01f)) - heuristics.computeValue(copyState, this.switchPlayer(player), Float.valueOf(0.01f));
    }
    private int switchPlayer(int player) {
        if (player == 1)
            return 2;
        return 1;
    }
}
