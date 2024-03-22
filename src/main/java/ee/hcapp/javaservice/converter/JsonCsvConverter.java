package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class JsonCsvConverter {

    public byte[] convert(MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());
        LinkedHashMap<String, Integer> headersMap = createHeadersMap(rootNode);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();

        headersMap.forEach((header, index) -> sb.append(header).append(index < headersMap.size() - 1 ? "," : "\n"));

        for (JsonNode node : rootNode) {
            JsonNode itemsNode = node.get("items");
            List<String> row = new ArrayList<>(headersMap.size());

            for (int i = 0; i < headersMap.size(); i++) {
                row.add("");
            }

            for (JsonNode item : itemsNode) {
                String headerName = item.get("name").asText();
                if (headersMap.containsKey(headerName)) {
                    int columnIndex = headersMap.get(headerName);
                    JsonNode valueNode = item.get("value");
                    String cellValue = extractValue(valueNode);
                    row.set(columnIndex, cellValue);
                }
            }

            sb.append(String.join(",", row)).append("\n");
        }

        baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private LinkedHashMap<String, Integer> createHeadersMap(JsonNode rootNode) {
        LinkedHashMap<String, Integer> headersMap = new LinkedHashMap<>();
        for (JsonNode node : rootNode) {
            JsonNode itemsNode = node.get("items");
            for (JsonNode item : itemsNode) {
                String header = item.get("name").asText();
                headersMap.putIfAbsent(header, headersMap.size());
            }
        }
        return headersMap;
    }

    private String extractValue(JsonNode valueNode) {
        if (valueNode == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (valueNode.isArray()) {
            for (JsonNode arrayElement : valueNode) {
                if (arrayElement.isArray()) {
                    List<String> innerValues = new ArrayList<>();
                    arrayElement.forEach(innerElement -> {
                        if (innerElement.isObject() && innerElement.has("value") && !innerElement.get("value").asText().isEmpty()) {
                            innerValues.add(innerElement.get("value").asText());
                        }
                    });
                    if (!innerValues.isEmpty()) {
                        if (sb.length() > 0) sb.append("\n"); // Использование "\n" для визуального разделения вложенных структур
                        sb.append(String.join(", ", innerValues));
                    }
                } else if (arrayElement.isObject() && arrayElement.has("value")) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(arrayElement.get("value").asText());
                }
            }
        } else if (valueNode.isObject()) {
            if (valueNode.has("values") && valueNode.get("values").isArray()) {
                List<String> objectValues = new ArrayList<>();
                valueNode.get("values").forEach(objValue -> objectValues.add(objValue.asText()));
                sb.append(String.join(", ", objectValues));
            }
        } else if (valueNode.isTextual()) {
            sb.append(valueNode.asText());
        }

        return "\"" + sb.toString().replace("\"", "\"\"") + "\"";
    }
}
