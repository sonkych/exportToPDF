package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.PdfPCell;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class JsonPdfConverter {

    public byte[] convert(MultipartFile file) throws IOException, DocumentException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        LinkedHashMap<String, Integer> headersMap = createHeadersMap(rootNode);
        PdfPTable table = new PdfPTable(headersMap.size());
        table.setWidthPercentage(100);

        addTableHeaders(table, headersMap);

        fillData(table, rootNode, headersMap);

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private void addTableHeaders(PdfPTable table, LinkedHashMap<String, Integer> headersMap) throws DocumentException, IOException {
        Font headerFont = getFontForCyrillic();

        BaseColor headerColor = new BaseColor(230, 230, 230);

        headersMap.forEach((header, index) -> {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
            headerCell.setBackgroundColor(headerColor);
            table.addCell(headerCell);
        });
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

    private void fillData(PdfPTable table, JsonNode rootNode, LinkedHashMap<String, Integer> headersMap) throws DocumentException, IOException {
        Font font = getFontForCyrillic();

        for (JsonNode node : rootNode) {
            JsonNode itemsNode = node.get("items");
            LinkedHashMap<String, String> rowData = new LinkedHashMap<>();

            headersMap.forEach((header, index) -> rowData.put(header, ""));

            for (JsonNode item : itemsNode) {
                String headerName = item.get("name").asText();
                JsonNode valueNode = item.get("value");
                String cellValue = extractValue(valueNode);
                rowData.put(headerName, cellValue);
            }

            headersMap.forEach((header, index) -> {
                PdfPCell cell = new PdfPCell();
                cell.setPhrase(new Phrase(rowData.get(header), font));
                table.addCell(cell);
            });
        }
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
                    for (JsonNode innerElement : arrayElement) {
                        if (innerElement.isObject() && innerElement.has("value") && !innerElement.get("value").asText().isEmpty()) {
                            innerValues.add(innerElement.get("value").asText());
                        }
                    }
                    if (!innerValues.isEmpty()) {
                        if (sb.length() > 0) sb.append("\n");
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
                for (JsonNode objValue : valueNode.get("values")) {
                    objectValues.add(objValue.asText());
                }
                sb.append(String.join(", ", objectValues));
            }
        } else if (valueNode.isTextual()) {
            sb.append(valueNode.asText());
        }

        return sb.toString();
    }

    private Font getFontForCyrillic() throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont("classpath:fonts/times.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(bf, 8);
        return font;
    }
}
