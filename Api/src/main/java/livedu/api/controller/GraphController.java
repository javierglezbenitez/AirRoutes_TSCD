package livedu.api.controller;

import livedu.api.core.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/top-degree")
    public ResponseEntity<?> topDegree(
            @RequestParam(name = "k", defaultValue = "10") int k) {
        try {
            return ResponseEntity.ok(graphService.topDegree(k));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/shortest-path")
    public ResponseEntity<List<String>> shortestPath(
            @RequestParam(name = "source") String source,
            @RequestParam(name = "target") String target) {
        return ResponseEntity.ok(graphService.shortestPath(source, target));
    }

    @GetMapping("/isolated")
    public ResponseEntity<List<String>> isolated() {
        return ResponseEntity.ok(graphService.isolatedNodes());
    }}
