package com.example.engine.domain;

public class GameLogic {

    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    public void applyMove(Game game, Player player, int position) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameFinishedException(game.getStatus());
        }
        if (game.getNextPlayer() != player) {
            throw new WrongTurnException(game.getNextPlayer(), player);
        }
        if (game.getBoard()[position] != null) {
            throw new CellOccupiedException(position, game.getBoard()[position]);
        }

        game.getBoard()[position] = player;

        if (hasWinningLine(game.getBoard(), player)) {
            game.setStatus(player == Player.X ? GameStatus.X_WON : GameStatus.O_WON);
            game.setNextPlayer(null);
            return;
        }
        if (isBoardFull(game.getBoard())) {
            game.setStatus(GameStatus.DRAW);
            game.setNextPlayer(null);
            return;
        }

        game.setNextPlayer(player.opposite());
    }

    private boolean hasWinningLine(Player[] board, Player player) {
        for (int[] line : LINES) {
            if (board[line[0]] == player && board[line[1]] == player && board[line[2]] == player) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoardFull(Player[] board) {
        for (Player cell : board) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }
}
