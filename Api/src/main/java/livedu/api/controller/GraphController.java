
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

    // 🎯 ENDPOINTS ORIENTADOS AL USUARIO

    /**
     * Lista de tickets disponibles entre origen y destino.
     * Incluye precio, duración, aerolínea y si hay escala.
     */
    @GetMapping("/tickets")
    public ResponseEntity<?> tickets(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(graphService.tickets(origen, destino, limit));
    }

    /**
     * Top N tickets más baratos para una ruta (origen-destino).
     */
    @GetMapping("/tickets/baratos")
    public ResponseEntity<?> ticketsBaratos(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(graphService.ticketsBaratos(origen, destino, limit));
    }

    /**
     * El ticket más barato absoluto para una ruta.
     */
    @GetMapping("/tickets/mas-barato")
    public ResponseEntity<?> masBarato(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        // Reutiliza la lógica de 'baratos' con limit=1
        return ResponseEntity.ok(graphService.ticketsBaratos(origen, destino, 1));
    }

    /**
     * Solo tickets directos (sin escala) entre origen y destino.
     */
    @GetMapping("/tickets/directos")
    public ResponseEntity<?> ticketsDirectos(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(graphService.ticketsDirectos(origen, destino, limit));
    }

    /**
     * Resumen por aerolínea en la ruta (min/avg/max de precio y duración media).
     */
    @GetMapping("/ruta/resumen")
    public ResponseEntity<?> resumenRuta(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        return ResponseEntity.ok(graphService.resumenRuta(origen, destino));
    }

    /**
     * Disponibilidad de la ruta: total, directos y con escala.
     */
    @GetMapping("/ruta/disponibilidad")
    public ResponseEntity<?> disponibilidadRuta(
            @RequestParam(name = "origen") String origen,
            @RequestParam(name = "destino") String destino) {
        return ResponseEntity.ok(graphService.disponibilidadRuta(origen, destino));
    }
}
