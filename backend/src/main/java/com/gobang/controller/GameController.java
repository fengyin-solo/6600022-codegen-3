package com.gobang.controller;

import com.gobang.model.GameState;
import com.gobang.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private AiService aiService;

    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> spectators = new ConcurrentHashMap<>();

    @PostMapping("/new")
    public ResponseEntity<GameState> newGame() {
        String id = UUID.randomUUID().toString();
        GameState game = new GameState(id);
        games.put(id, game);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameState> getGame(@PathVariable String id) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<?> makeMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int row = body.getOrDefault("row", -1);
        int col = body.getOrDefault("col", -1);

        if (!game.placeStone(row, col)) {
            return ResponseEntity.badRequest().body("Invalid move");
        }

        broadcastUpdate(id, game);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/ai-move")
    public ResponseEntity<?> aiMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int aiPlayer = body.getOrDefault("player", GameState.WHITE);
        int depth = body.getOrDefault("depth", 3);

        int[][] boardCopy = new int[GameState.BOARD_SIZE][GameState.BOARD_SIZE];
        for (int r = 0; r < GameState.BOARD_SIZE; r++) {
            System.arraycopy(game.getBoard()[r], 0, boardCopy[r], 0, GameState.BOARD_SIZE);
        }

        int[] move = aiService.getBestMove(boardCopy, aiPlayer, depth);
        if (move == null) return ResponseEntity.badRequest().body("No valid move");

        game.placeStone(move[0], move[1]);

        broadcastUpdate(id, game);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<GameState>> getOngoingGames() {
        List<GameState> ongoing = games.values().stream()
                .filter(g -> g.getWinner() == null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(ongoing);
    }

    @GetMapping("/records")
    public ResponseEntity<List<GameState>> getRecords() {
        List<GameState> finished = games.values().stream()
                .filter(g -> g.getWinner() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(finished);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        games.remove(id);
        cleanupSpectators(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/spectate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter spectate(@PathVariable String id) {
        GameState game = games.get(id);
        if (game == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("Game not found"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(300_000L);
        spectators.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("state")
                    .data(game, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> removeSpectator(id, emitter));
        emitter.onTimeout(() -> removeSpectator(id, emitter));
        emitter.onError(e -> removeSpectator(id, emitter));

        return emitter;
    }

    private void broadcastUpdate(String gameId, GameState game) {
        CopyOnWriteArrayList<SseEmitter> emitters = spectators.get(gameId);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("state")
                        .data(game, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private void removeSpectator(String gameId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = spectators.get(gameId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    private void cleanupSpectators(String gameId) {
        CopyOnWriteArrayList<SseEmitter> emitters = spectators.remove(gameId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
        }
    }
}
