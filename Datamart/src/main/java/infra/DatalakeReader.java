
// infra/DatalakeReader.java
package infra;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public interface DatalakeReader {
    List<String> listFilesForDate(LocalDate date);
    List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception;
}
