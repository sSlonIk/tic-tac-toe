package com.example.engine.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.engine.domain.GameConflictException;
import com.example.engine.domain.GameLogic;
import com.example.engine.domain.Player;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameStoreConcurrencyTest {

    @Test
    void concurrentIdenticalMovesShouldApplyExactlyOnce() throws InterruptedException {
        GameStore store = new GameStore();
        GameLogic logic = new GameLogic();
        store.create("race");

        int threads = 32;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        store.withGame("race", game -> {
                            logic.applyMove(game, Player.X, 4);
                            return game;
                        });
                        successes.incrementAndGet();
                    } catch (GameConflictException expected) {
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
        }

        assertEquals(1, successes.get());
        store.withGame("race", game -> {
            assertEquals(Player.X, game.getBoard()[4]);
            assertEquals(Player.O, game.getNextPlayer());
            return game;
        });
    }
}
