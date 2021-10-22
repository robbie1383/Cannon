package cannon;

import game.Game;
import main.collections.FastArrayList;
import metadata.ai.heuristics.Heuristics;
import other.AI;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

public class AlphaBetaAI extends AI
{
    int time = 600000;
    protected int player = -1;
    Heuristics heuristics;
    TranspositionTableEntry[] transpositionTable = new TranspositionTableEntry[1 << 12];
    int depth = 2;
    public AlphaBetaAI()
    {
        this.friendlyName = "Robbie's AI";
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
        // Time management
        double turn = 0;
        long start = System.currentTimeMillis();

        // Iterative deepening - start with depth 2
        MoveValue move = this.AlphaBeta(game, context, this.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, maxSeconds*1000);

        // Time management
        long end = System.currentTimeMillis();
        turn += (end - start);

        // Iterative deepening - if there is still time (2 seconds), try searching deeper
        while (turn < (maxSeconds*1000 - 2000)) {
            this.depth++;
            start = System.currentTimeMillis();
            move = this.AlphaBeta(game, context, this.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, maxSeconds*1000-turn);
            end = System.currentTimeMillis();
            turn += (end - start);

        }

        // Print some information and time management
        time -= turn;
        System.out.println("Depth " + depth);
        System.out.println("Turn took : "+ (turn/1000) + " seconds. Time left : " + (time/1000) + " seconds for player "+ this.player);
        System.out.println(("Score :" + move.getScore()));
        this.depth = 2;

        // Return best move
        return move.getMove();
    }

    @Override
    public void initAI(final Game game, final int playerID) {

        this.player = playerID;
        this.heuristics = Heuristics.copy(game.metadata().ai().heuristics());
        this.heuristics.init(game);
    }

    public MoveValue AlphaBeta(Game game, Context state, int depth, double alpha, double beta, int player, double timeLeft) {

        // Start move timer
        long start = System.currentTimeMillis();

        // Lookup current state in transposition table
        double oldAlpha = alpha;
        TranspositionTableEntry entry = this.lookUp(state);
        if (entry != null && entry.depth >= depth) {
            // Check flag and follow rules
            if (entry.flag.equals("exact")) return new MoveValue(entry.bestMove, entry.value);
            if (entry.flag.equals("upper")) beta = Math.min(beta, entry.value);
            if (entry.flag.equals("lower")) alpha = Math.max(alpha, entry.value);
            if (alpha >= beta)
                return new MoveValue(entry.bestMove, entry.value);
        }

        // Alpha-beta - if it's a terminal state or depth = 0
        if(depth == 0 || !state.active(player) || !state.active(switchPlayer(player)))
            return new MoveValue(null, this.getHeuristic(state, depth));

        // If there is no time left
        if(timeLeft <= 10)
            return new MoveValue(null, this.getHeuristic(state, depth));

        // Move ordering
        FastArrayList<Move> legalMoves = AIUtils.extractMovesForMover(game.moves(state).moves(), player);
        this.order(legalMoves, state, game, 0, legalMoves.size()-1, depth);

        // Alpha-beta - initializations
        Move bestMove = legalMoves.get(0);
        double score;

        // Maximizing player
        if (player == this.player) {
            score = Double.NEGATIVE_INFINITY;
            for (Move move : legalMoves) {
                Context copyState = copyContext(state);
                game.apply(copyState, move);
                double newScore = this.AlphaBeta(game, copyState, depth - 1, alpha, beta, this.switchPlayer(player), timeLeft - (System.currentTimeMillis() - start)).getScore();
                if (newScore > score) {
                    score = newScore;
                    bestMove = move;
                }
                if (score >= beta)
                    break;
                alpha = Math.max(alpha, score);
            }
        }

        // Minimizing player
        else {
            score = Double.POSITIVE_INFINITY;
            for (Move move : legalMoves) {
                Context copyState = copyContext(state);
                game.apply(copyState, move);
                double newScore = this.AlphaBeta(game, copyState, depth - 1, alpha, beta, this.switchPlayer(player), timeLeft - (System.currentTimeMillis() - start)).getScore();
                if (newScore < score) {
                    score = newScore;
                    bestMove = move;
                }
                if (score <= alpha)
                    break;
                beta = Math.min(beta, score);
            }
        }

        // Store node in transposition table
        String flag;
        if (score <= oldAlpha) flag = "upper";
        else if (score >= beta) flag = "lower";
        else flag = "exact";
        this.addToTT(state, new TranspositionTableEntry(score, flag, bestMove, depth, state.state().fullHash()));

        // Return best move and score
        return new MoveValue(bestMove, score);
    }

    private TranspositionTableEntry lookUp(Context context) {

        // Compute hash of current state and look up the primary in the transposition table
        int i = (int) (context.state().fullHash() >>> (Long.SIZE - 12));
        if (this.transpositionTable[i] != null && this.transpositionTable[i].id == context.state().fullHash())
            return this.transpositionTable[i];
        return null;
    }

    private void addToTT(Context context, TranspositionTableEntry t){
        // Compute hash of current state and add it to the primary hash index in transposition table
        int i = (int) (context.state().fullHash() >>> (Long.SIZE - 12));
        this.transpositionTable[i] = t;
    }

    private void order(FastArrayList<Move> list, Context state, Game game, int low, int high, int depth) {
        //Quicksort to sort descending on heuristic values
        if (low < high) {
            double pivot = this.getHeuristicOfMove(list.get(low), state, game, depth);
            int i = low;
            for(int j = low + 1; j <= high; j++) {
                if (this.getHeuristicOfMove(list.get(j), state, game, depth) > pivot) {
                    i = i+1;
                    Move aux = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, aux);
                }
            }
            Move aux = list.get(i);
            list.set(i, list.get(low));
            list.set(low, aux);

            this.order(list, state, game, low, i, depth);
            this.order(list, state, game, i+1, high, depth);
        }

    }
    private double getHeuristic(Context state, int depth) {

        // Compute the heuristic for a given state and depth
        double heuristic = heuristics.computeValue(state, this.player, Float.valueOf(0.01f)) - heuristics.computeValue(state, this.switchPlayer(this.player), Float.valueOf(0.01f));

        // If the other player wins
        if (state.winners().contains(switchPlayer(this.player))) {
            heuristic -= 10000.f;
            heuristic -= (this.depth - depth)*10;
        }

        // If the root player wins
        if (state.winners().contains(this.player)) {
            heuristic += 10000.f;
            heuristic -= (this.depth - depth) * 10;
        }

        return heuristic;

    }
    private double getHeuristicOfMove(Move move, Context state, Game game, int depth){
        // Compute the simple heuristic value of making a move in a given state
        Context copyState = copyContext(state);
        game.apply(copyState, move);
        return this.getHeuristic(copyState, depth);

    }

    private int switchPlayer(int player) {
        // Get the opponent player ID
        if (player == 1)
            return 2;
        return 1;
    }

}
