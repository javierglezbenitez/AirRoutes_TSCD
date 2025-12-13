
package storage;

import architecture.AirRoute;
import architecture.Storage;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class LocalStorage implements Storage {

    private final String basePath;
    private final Gson gson;

    public LocalStorage(String basePath) {
        this.basePath = basePath;
        this.gson = new Gson();
    }

    @Override
    public void saveAirRoutes(List<AirRoute> routes) {
        try {
            String dateFolder = LocalDate.now().toString();
            Path folderPath = Path.of(basePath, "datalake", dateFolder);
            Files.createDirectories(folderPath);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmssSSS");
            int counter = 1;

            for (AirRoute ar : routes) {
                String timestamp = LocalTime.now().format(timeFormatter);
                String fileName = "airroute_" + dateFolder + "_" + timestamp + "_" + counter++ + ".json";
                Path filePath = folderPath.resolve(fileName);

                try (FileWriter writer = new FileWriter(filePath.toFile())) {
                    String json = gson.toJson(ar);
                    writer.write(json);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error al guardar AirRoute localmente: " + e.getMessage());
        }
    }
}
