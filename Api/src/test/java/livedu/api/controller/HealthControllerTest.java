
package livedu.api.controller;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class)
class HealthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    Driver driver;

    @Test
    void health_ok_whenNeo4jResponds() throws Exception {
        Session session = mock(Session.class);
        Result result = mock(Result.class);
        ResultSummary summary = mock(ResultSummary.class);

        when(driver.session()).thenReturn(session);
        when(session.run("RETURN 1")).thenReturn(result);
        when(result.consume()).thenReturn(summary);

        mvc.perform(get("/api/health").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(driver, times(1)).session();
        verify(session, times(1)).run("RETURN 1");
        verify(result, times(1)).consume();
    }

    @Test
    void health_down_whenNeo4jFails() throws Exception {
        when(driver.session()).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/health"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DOWN")));
    }
}
