package ee.hcapp.javaservice.converter;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardJsonCsvConverter {

    public byte[] convertDashboard(MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();

        // Добавляем заголовки
        sb.append("Name,Open Count,Open Sum,Overdue Count,Overdue Sum,Total Count,Total Sum\n");

        // Заполняем данными
        for (JsonNode rowNode : rootNode.get("rows")) {
            List<String> row = new ArrayList<>();

            // Добавляем имя
            String name = rowNode.get("name").asText();
            row.add(name);

            // Open count и sum
            JsonNode openNode = rowNode.get("open");
            row.add(String.valueOf(openNode.get("count").asInt()));
            row.add(String.valueOf(openNode.get("sum").asDouble()));

            // Overdue count и sum
            JsonNode overdueNode = rowNode.get("overdue");
            row.add(String.valueOf(overdueNode.get("count").asInt()));
            row.add(String.valueOf(overdueNode.get("sum").asDouble()));

            // Total count и sum
            JsonNode totalNode = rowNode.get("total");
            row.add(String.valueOf(totalNode.get("count").asInt()));
            row.add(String.valueOf(totalNode.get("sum").asDouble()));

            // Добавляем строку в CSV
            sb.append(String.join(",", row)).append("\n");
        }

        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }
}
