
// repository/GraphRepository.java
package repository;
import java.util.List;
import java.util.Map;
public interface GraphRepository {
    void ensureSchema();
    void insertAirRouteBatch(List<Map<String, Object>> routes);
    void clearAll();
}

