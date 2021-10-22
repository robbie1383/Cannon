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
    int depth = 4;
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
        double turn = 0;
        long start = System.currentTimeMillis();
        MoveValue move = this.AlphaBeta(game, context, this.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, maxSeconds*1000);
        long end = System.currentTimeMillis();
        turn += (end - start);

        while (turn < (maxSeconds*1000 - 2000)) {
            this.depth++;
            start = System.currentTimeMillis();
            move = this.AlphaBeta(game, context, this.depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, maxSeconds*1000-turn);
            end = System.currentTimeMillis();
            turn += (end - start);

        }
        time -= turn;
        System.out.println("Depth "+depth);
        System.out.println("Turn took : "+ (turn/1000) + " seconds. Time left : " + (time/1000) + " seconds for player "+ this.player);
        System.out.println(("Score :" + move.getScore()));
        this.depth = 4;
        return move.getMove();
    }

    @Override
    public void initAI(final Game game, final int playerID)
    {
        this.player = playerID;
        this.heuristics = Heuristics.copy(game.metadata().ai().heuristics());
        this.heuristics.init(game);
    }

    public MoveValue AlphaBeta(Game game, Context state, int depth, double alpha, double beta, int player, double timeLeft) {

        // Start move timer
        long start = System.currentTimeMillis();

        // Lookup transposition table
        double oldAlpha = alpha;
        TranspositionTableEntry entry = this.lookUp(state);
        if (entry != null && entry.depth >= depth) {
            //System.out.println("Used Transposition table.");
            if (entry.flag.equals("exact")) return new MoveValue(entry.bestMove, entry.value);
            if (entry.flag.equals("upper")) beta = Math.min(beta, entry.value);
            if (entry.flag.equals("lower")) alpha = Math.max(alpha, entry.value);
            if (alpha >= beta)
                return new MoveValue(entry.bestMove, entry.value);
        }

        // If leaf node
        if(depth == 0 || !state.active(player) || !state.active(switchPlayer(player))) {
            double heuristic = heuristics.computeValue(state, this.player, Float.valueOf(0.01f)) - heuristics.computeValue(state, this.switchPlayer(this.player), Float.valueOf(0.01f));
            if (state.winners().contains(switchPlayer(this.player))) {
                heuristic -= 10000.f;
                heuristic -= (this.depth - depth)*10;
            }

            if (state.winners().contains(this.player)) {
                heuristic += 10000.f;
                heuristic -= (this.depth - depth) * 10;
            }
            return new MoveValue(null, heuristic);
        }

        // If no time left
        if(timeLeft <= 10)
            return new MoveValue(null, heuristics.computeValue(state, this.player, Float.valueOf(0.01f)) - heuristics.computeValue(state, this.switchPlayer(this.player), Float.valueOf(0.01f)));

        // Move ordering
        FastArrayList<Move> legalMoves = AIUtils.extractMovesForMover(game.moves(state).moves(), player);
        this.order(legalMoves, state, game, 0, legalMoves.size()-1);
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

    private void order(FastArrayList<Move> list, Context state, Game game, int low, int high) {
        //Quicksort descending on heuristic values
        if (low < high) {
            double pivot = this.getHeuristic(list.get(low), state, game);
            int i = low;
            for(int j = low + 1; j <= high; j++) {
                if (this.getHeuristic(list.get(j), state, game) > pivot) {
                    i = i+1;
                    Move aux = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, aux);
                }
            }
            Move aux = list.get(i);
            list.set(i, list.get(low));
            list.set(low, aux);

            this.order(list, state, game, low, i);
            this.order(list, state, game, i+1, high);
        }

    }

    private double getHeuristic(Move move, Context state, Game game){
        Context copyState = copyContext(state);
        game.apply(copyState, move);
        return heuristics.computeValue(copyState, this.player, Float.valueOf(0.01f)) - heuristics.computeValue(copyState, this.switchPlayer(this.player), Float.valueOf(0.01f));
    }
    private int switchPlayer(int player) {
        if (player == 1)
            return 2;
        return 1;
    }
}
