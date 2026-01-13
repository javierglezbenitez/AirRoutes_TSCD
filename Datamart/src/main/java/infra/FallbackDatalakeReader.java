
package infra;

import java.time.LocalDate;
import java.util.*;

public class FallbackDatalakeReader implements DatalakeReader {
    private final DatalakeReader primary;
    private final DatalakeReader secondary;

    public FallbackDatalakeReader(DatalakeReader primary, DatalakeReader secondary) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.secondary = Objects.requireNonNull(secondary, "secondary");
    }

    @Override
    public List<String> listFilesForDate(LocalDate date) {
        try {
            List<String> keys = primary.listFilesForDate(date);
            if (keys != null && !keys.isEmpty()) {
                System.out.println("   ✅ Usando datalake PRIMARIO (S3). Archivos: " + keys.size());
                return keys;
            }
            System.out.println("   ↪️ Sin archivos en PRIMARIO. Probando SECUNDARIO (LOCAL)...");
        } catch (Exception e) {
            System.out.println("   ⚠️ Error en PRIMARIO (S3): " + e.getMessage() + " — Voy a SECUNDARIO (LOCAL)");
        }
        return secondary.listFilesForDate(date);
    }

    @Override
    public List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception {
        try {
            return primary.readSpecificKeys(keys);
        } catch (Exception e) {
            System.out.println("   ⚠️ Error leyendo PRIMARIO (S3): " + e.getMessage() + " — Leyendo SECUNDARIO (LOCAL)");
            return secondary.readSpecificKeys(keys);
        }
    }
}
