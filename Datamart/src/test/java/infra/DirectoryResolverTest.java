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

        assertEquals(rootDatalake, resolver.resolve(root, today));

        Files.delete(rootDatalake);
        assertEquals(justDate, resolver.resolve(root, today));

        assertEquals(justDate, resolver.resolve(justDate, today));
    }
}