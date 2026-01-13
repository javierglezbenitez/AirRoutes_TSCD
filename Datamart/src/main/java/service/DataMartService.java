
package service;
import java.util.List;
import java.util.Map;
public interface DataMartService {
    void upsertToday(List<Map<String, Object>> routes);
    void clearOld();
}
