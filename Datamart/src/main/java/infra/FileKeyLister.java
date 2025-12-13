
package infra;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

class FileKeyLister {
    List<String> listJsonKeys(Path dir, Path basePath) {
        List<String> keys = new ArrayList<>();
        if (dir == null) return keys;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : stream) {
                keys.add(basePath.relativize(p).toString().replace('\\', '/'));
            }
        } catch (IOException e) {
            System.err.println("   ❌ Error listando LOCAL: " + e.getMessage());
        }
        return keys;
    }
}