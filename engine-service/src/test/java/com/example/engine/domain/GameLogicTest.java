package com.example.engine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class GameLogicTest {

    private final GameLogic gameLogic = new GameLogic();

    @ParameterizedTest
    @MethodSource("winningLines")
    void shouldDetectEveryWinningLine(int[] line) {
        Game game = new Game("game");
        List<Integer> otherMoves = freeCellsExcluding(line);

        gameLogic.applyMove(game, Player.X, line[0]);
        gameLogic.applyMove(game, Player.O, otherMoves.get(0));
        gameLogic.applyMove(game, Player.X, line[1]);
        gameLogic.applyMove(game, Player.O, otherMoves.get(1));
        gameLogic.applyMove(game, Player.X, line[2]);

        assertEquals(Player.X, game.getBoard()[line[0]]);
        assertEquals(Player.X, game.getBoard()[line[1]]);
        assertEquals(Player.X, game.getBoard()[line[2]]);
        assertEquals(GameStatus.X_WON, game.getStatus());
        assertNull(game.getNextPlayer());
    }

    @Test
    void shouldDetectDraw() {
        Game game = new Game("game");

        gameLogic.applyMove(game, Player.X, 0);
        gameLogic.applyMove(game, Player.O, 1);
        gameLogic.applyMove(game, Player.X, 2);
        gameLogic.applyMove(game, Player.O, 4);
        gameLogic.applyMove(game, Player.X, 3);
        gameLogic.applyMove(game, Player.O, 5);
        gameLogic.applyMove(game, Player.X, 7);
        gameLogic.applyMove(game, Player.O, 6);
        gameLogic.applyMove(game, Player.X, 8);

        assertEquals(GameStatus.DRAW, game.getStatus());
        assertNull(game.getNextPlayer());
    }

    @Test
    void shouldRejectMoveIntoOccupiedCell() {
        Game game = new Game("game");
        gameLogic.applyMove(game, Player.X, 0);

        assertThrows(CellOccupiedException.class, () -> gameLogic.applyMove(game, Player.O, 0));
    }

    @Test
    void shouldRejectWrongTurn() {
        Game game = new Game("game");
        gameLogic.applyMove(game, Player.X, 0);

        assertThrows(WrongTurnException.class, () -> gameLogic.applyMove(game, Player.X, 1));
    }

    @Test
    void shouldRejectMoveAfterGameFinished() {
        Game game = new Game("game");
        gameLogic.applyMove(game, Player.X, 0);
        gameLogic.applyMove(game, Player.O, 3);
        gameLogic.applyMove(game, Player.X, 1);
        gameLogic.applyMove(game, Player.O, 4);
        gameLogic.applyMove(game, Player.X, 2);

        assertThrows(GameFinishedException.class, () -> gameLogic.applyMove(game, Player.O, 5));
    }

    @Test
    void shouldRejectFirstMoveByO() {
        Game game = new Game("game");

        assertThrows(WrongTurnException.class, () -> gameLogic.applyMove(game, Player.O, 0));
    }

    private static Stream<int[]> winningLines() {
        return Stream.of(
            new int[]{0, 1, 2},
            new int[]{3, 4, 5},
            new int[]{6, 7, 8},
            new int[]{0, 3, 6},
            new int[]{1, 4, 7},
            new int[]{2, 5, 8},
            new int[]{0, 4, 8},
            new int[]{2, 4, 6}
        );
    }

    private List<Integer> freeCellsExcluding(int[] line) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            boolean excluded = false;
            for (int position : line) {
                if (position == i) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                free.add(i);
            }
        }
        return free;
    }
}
