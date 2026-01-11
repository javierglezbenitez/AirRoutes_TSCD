
package service;

import org.junit.jupiter.api.Test;
import repository.GraphRepository;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class DataMartServiceImplTest {

    @Test
    void upsertToday_delegatesToRepo() {
        GraphRepository repo = mock(GraphRepository.class);
        DataMartService svc = new DataMartServiceImpl(repo);

        // 👇 Importante: obligamos a Map<String, Object>
        Map<String, Object> row = Map.<String, Object>of("codigoVuelo", "FL-1");
        var data = List.of(row);

        svc.upsertToday(data);

        verify(repo, times(1)).insertAirRouteBatch(data);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void clearOld_callsRepoClearAll() {
        GraphRepository repo = mock(GraphRepository.class);
        DataMartService svc = new DataMartServiceImpl(repo);

        svc.clearOld();
        verify(repo, times(1)).clearAll();
    }
}
