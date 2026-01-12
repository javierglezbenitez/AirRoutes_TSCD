package infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryResolverTest {

    @TempDir
    Path base;

    @Test
    void resolve_shouldSupportThreeLayouts() throws Exception {
        String today = LocalDate.now().toString();
        Path root = base;
        Path rootDatalake = root.resolve("datalake").resolve(today);
        Files.createDirectories(rootDatalake);
        Path justDate = root.resolve(today);
        Files.createDirectories(justDate);

        DirectoryResolver resolver = new DirectoryResolver();

        // 1) <base>/datalake/YYYY-MM-DD
        assertEquals(rootDatalake, resolver.resolve(root, today));

        // 2) <base>/YYYY-MM-DD (si falta 'datalake' pero existe esta)
        Files.delete(rootDatalake); // fuerza a elegir la segunda
        assertEquals(justDate, resolver.resolve(root, today));

        // 3) Si base ya es la carpeta del día
        assertEquals(justDate, resolver.resolve(justDate, today));
    }
}