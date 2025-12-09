
package architecture;

import java.util.List;

public class Datalake {

    private final Storage storage;

    public Datalake(Storage storage) {
        this.storage = storage;
    }

    public void saveAirRoutes(List<AirRoute> routes) {
        storage.saveAirRoutes(routes);
    }
}
