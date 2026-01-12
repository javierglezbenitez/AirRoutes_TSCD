package architecture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.*;

class DatalakeTest {

    @Test
    void saveAirRoutes_shouldDelegateToStorage() {
        Storage storage = Mockito.mock(Storage.class);
        Datalake datalake = new Datalake(storage);

        List<AirRoute> routes = List.of(
                new AirRoute("FL-1","MAD","JFK",100,100.0,"Iberia",System.currentTimeMillis(),"None",3)
        );

        datalake.saveAirRoutes(routes);

        verify(storage, times(1)).saveAirRoutes(routes);
        verifyNoMoreInteractions(storage);
    }
}