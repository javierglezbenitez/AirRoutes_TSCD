
package livedu.api.controller;

import livedu.api.core.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GraphController.class)
class GraphControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    GraphService graphService;

    @Test
    void tickets_returnsList() throws Exception {
        List<Map<String,Object>> fake =
                List.of(Map.of("codigoVuelo", "FL-1", "precio", 100.0));

        when(graphService.tickets(eq("MAD"), eq("JFK"), eq(50)))
                .thenReturn(fake);

        mvc.perform(get("/api/graph/tickets")
                        .param("origen", "MAD")
                        .param("destino", "JFK")
                        .param("limit", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoVuelo").value("FL-1"))
                .andExpect(jsonPath("$[0].precio").value(100.0));
    }

    @Test
    void ticketsBaratos_topN() throws Exception {
        List<Map<String,Object>> fake =
                List.of(Map.of("codigoVuelo", "CHEAP-1"));

        when(graphService.ticketsBaratos(eq("MAD"), eq("JFK"), eq(5)))
                .thenReturn(fake);

        mvc.perform(get("/api/graph/tickets/baratos")
                        .param("origen", "MAD")
                        .param("destino", "JFK")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoVuelo").value("CHEAP-1"));
    }

    @Test
    void masBarato_usesBaratosLimit1() throws Exception {
        List<Map<String,Object>> fake =
                List.of(Map.of("codigoVuelo", "MIN-1"));

        when(graphService.ticketsBaratos(eq("MAD"), eq("JFK"), eq(1)))
                .thenReturn(fake);

        mvc.perform(get("/api/graph/tickets/mas-barato")
                        .param("origen", "MAD")
                        .param("destino", "JFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoVuelo").value("MIN-1"));
    }

    @Test
    void ticketsDirectos() throws Exception {
        List<Map<String,Object>> fake =
                List.of(Map.of("codigoVuelo", "DIR-1"));

        when(graphService.ticketsDirectos(eq("MAD"), eq("JFK"), eq(50)))
                .thenReturn(fake);

        mvc.perform(get("/api/graph/tickets/directos")
                        .param("origen", "MAD")
                        .param("destino", "JFK")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoVuelo").value("DIR-1"));
    }

    @Test
    void resumenRuta_returnsAggregatesList() throws Exception {
        List<Map<String,Object>> resumen = List.of(
                Map.of("aerolinea", "Iberia",
                        "vuelos", 3,
                        "minPrecio", 50.0,
                        "maxPrecio", 200.0,
                        "avgPrecio", 120.0,
                        "avgDuracionMin", 100)
        );

        when(graphService.resumenRuta(eq("MAD"), eq("JFK")))
                .thenReturn(resumen);

        mvc.perform(get("/api/graph/ruta/resumen")
                        .param("origen", "MAD")
                        .param("destino", "JFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aerolinea").value("Iberia"))
                .andExpect(jsonPath("$[0].minPrecio").value(50.0))
                .andExpect(jsonPath("$[0].maxPrecio").value(200.0))
                .andExpect(jsonPath("$[0].avgPrecio").value(120.0))
                .andExpect(jsonPath("$[0].avgDuracionMin").value(100));
    }

    @Test
    void disponibilidadRuta_returnsAggregatesList() throws Exception {
        List<Map<String,Object>> disp = List.of(
                Map.of("total", 10, "directos", 6, "conEscala", 4)
        );

        when(graphService.disponibilidadRuta(eq("MAD"), eq("JFK")))
                .thenReturn(disp);

        mvc.perform(get("/api/graph/ruta/disponibilidad")
                        .param("origen", "MAD")
                        .param("destino", "JFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(10))
                .andExpect(jsonPath("$[0].directos").value(6))
                .andExpect(jsonPath("$[0].conEscala").value(4));
    }
}
