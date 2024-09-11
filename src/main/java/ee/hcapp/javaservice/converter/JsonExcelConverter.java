package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonExcelConverter {

    public byte[] convert(MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");

            LinkedHashMap<String, Integer> headersMap = createHeadersMap(rootNode);
            createHeadersRow(sheet, headersMap);
            fillData(sheet, rootNode, headersMap);

            workbook.write(out);
            return out.toByteArray();
        }
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

    private void createHeadersRow(Sheet sheet, LinkedHashMap<String, Integer> headersMap) {
        Row headerRow = sheet.createRow(0);
        headersMap.forEach((header, index) -> headerRow.createCell(index).setCellValue(header));
    }

    private void fillData(Sheet sheet, JsonNode rootNode, LinkedHashMap<String, Integer> headersMap) {
        int rowNum = 1;
        for (JsonNode node : rootNode) {
            Row row = sheet.createRow(rowNum++);
            JsonNode itemsNode = node.get("items");

            for (Map.Entry<String, Integer> headerEntry : headersMap.entrySet()) {
                String headerName = headerEntry.getKey();
                int cellNum = headerEntry.getValue();
                Cell cell = row.createCell(cellNum);

                JsonNode valueNode = findValueNodeByName(itemsNode, headerName);
                String cellValue = extractValue(valueNode, cell);
                cell.setCellValue(cellValue);
            }
        }

        for (int i = 0; i < headersMap.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String extractValue(JsonNode valueNode, Cell cell) {
        if (valueNode == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        if (valueNode.isArray()) {
            List<String> arrayValues = new ArrayList<>();
            for (JsonNode arrayElement : valueNode) {
                if (arrayElement.isTextual()) {
                    arrayValues.add(arrayElement.asText());
                } else if (arrayElement.isArray() || arrayElement.isObject()) {
                    arrayValues.add(extractValue(arrayElement, cell));
                }
            }
            if (!arrayValues.isEmpty()) {
                sb.append(String.join(", ", arrayValues));
            }
        } else if (valueNode.isObject()) {
            if (valueNode.has("values") && valueNode.get("values").isArray()) {
                List<String> objectValues = new ArrayList<>();
                valueNode.get("values").forEach(objValue -> objectValues.add(objValue.asText()));
                sb.append(String.join(", ", objectValues));
            } else if (valueNode.has("value")) {
                sb.append(valueNode.get("value").asText());
            }
        } else if (valueNode.isTextual()) {
            sb.append(valueNode.asText());
        }

        if (sb.indexOf("\n") >= 0) {
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            cellStyle.setWrapText(true);
            cell.setCellStyle(cellStyle);
        }

        return sb.toString();
    }


    private JsonNode findValueNodeByName(JsonNode itemsNode, String name) {
        for (JsonNode item : itemsNode) {
            if (item.has("name") && item.get("name").asText().equals(name)) {
                return item.get("value");
            }
        }
        return null;
    }
}
