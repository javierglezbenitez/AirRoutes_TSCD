
package livedu.api.controller;

import livedu.api.core.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> tickets(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(graphService.tickets(origen, destino, limit));
    }

    @GetMapping("/tickets/baratos")
    public ResponseEntity<?> ticketsBaratos(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(graphService.ticketsBaratos(origen, destino, limit));
    }

    @GetMapping("/tickets/mas-barato")
    public ResponseEntity<?> masBarato(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        // Reutiliza la lógica de 'baratos' con limit=1
        return ResponseEntity.ok(graphService.ticketsBaratos(origen, destino, 1));
    }

    @GetMapping("/tickets/directos")
    public ResponseEntity<?> ticketsDirectos(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(graphService.ticketsDirectos(origen, destino, limit));
    }

    @GetMapping("/ruta/resumen")
    public ResponseEntity<?> resumenRuta(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        return ResponseEntity.ok(graphService.resumenRuta(origen, destino));
    }

    @GetMapping("/ruta/disponibilidad")
    public ResponseEntity<?> disponibilidadRuta(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        return ResponseEntity.ok(graphService.disponibilidadRuta(origen, destino));
    }
}
