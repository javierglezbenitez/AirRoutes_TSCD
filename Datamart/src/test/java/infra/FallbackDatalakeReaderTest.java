package infra;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackDatalakeReaderTest {

    static class OkReader implements DatalakeReader {
        final List<String> keys; final List<Map<String,Object>> data;
        OkReader(List<String> keys, List<Map<String,Object>> data) { this.keys = keys; this.data = data; }
        @Override public List<String> listFilesForDate(LocalDate date) { return keys; }
        @Override public List<Map<String, Object>> readSpecificKeys(List<String> keys) { return data; }
    }

    static class ErrReader implements DatalakeReader {
        @Override public List<String> listFilesForDate(LocalDate date) { throw new RuntimeException("boom"); }
        @Override public List<Map<String, Object>> readSpecificKeys(List<String> keys) { throw new RuntimeException("boom"); }
    }

    @Test
    void listFiles_usesPrimaryIfOk_elseSecondary() {
        DatalakeReader primary = new ErrReader();
        DatalakeReader secondary = new OkReader(List.of("k1","k2"), List.of());
        FallbackDatalakeReader fb = new FallbackDatalakeReader(primary, secondary);

        var keys = fb.listFilesForDate(LocalDate.now());
        assertEquals(2, keys.size());
    }

    @Test
    void readFiles_fallsBackOnError() throws Exception {
        DatalakeReader primary = new ErrReader();
        DatalakeReader secondary = new OkReader(List.of(), List.of(Map.of("codigoVuelo","X")));
        FallbackDatalakeReader fb = new FallbackDatalakeReader(primary, secondary);

        var data = fb.readSpecificKeys(List.of("a.json"));
        assertEquals(1, data.size());
    }
}