package infra;

import java.nio.file.Files;
import java.nio.file.Path;

class DirectoryResolver {

    Path resolve(Path basePath, String dateStr) {
        if (basePath == null || dateStr == null || dateStr.isBlank()) {
            return null;
        }

        // <base>/datalake/YYYY-MM-DD
        Path rootPlusDatalake = basePath.resolve("datalake").resolve(dateStr);
        // <base>/YYYY-MM-DD
        Path datalakePlusDate = basePath.resolve(dateStr);

        if (Files.isDirectory(rootPlusDatalake)) {
            return rootPlusDatalake;
        }
        if (Files.isDirectory(datalakePlusDate)) {
            return datalakePlusDate;
        }

        // <base> (si ya es la carpeta del día)
        Path fileName = basePath.getFileName();
        if (fileName != null
                && fileName.toString().equals(dateStr)
                && Files.isDirectory(basePath)) {
            return basePath;
        }

        return null;
    }
}
