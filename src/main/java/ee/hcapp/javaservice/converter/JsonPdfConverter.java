package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.PdfPCell;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
public class JsonPdfConverter {

    public byte[] convert(MultipartFile file, String timeZone) throws IOException, DocumentException {
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

        fillData(table, rootNode, headersMap, timeZone);

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

    private void fillData(PdfPTable table, JsonNode rootNode, LinkedHashMap<String, Integer> headersMap, String timeZone) throws DocumentException, IOException {
        Font font = getFontForCyrillic();

        for (JsonNode node : rootNode) {
            JsonNode itemsNode = node.get("items");
            LinkedHashMap<String, String> rowData = new LinkedHashMap<>();

            headersMap.forEach((header, index) -> rowData.put(header, ""));

            for (JsonNode item : itemsNode) {
                String headerName = item.get("name").asText();
                JsonNode valueNode = item.get("value");

                String cellValue = extractValue(valueNode);

                if ("DateField".equals(item.get("form_field_type").asText()) ||
                    "DueDateField".equals(item.get("form_field_type").asText()) ||
                    "DueDate".equals(item.get("form_field_type").asText()) ||
                    "Date".equals(item.get("form_field_type").asText())) {

                    cellValue = formatDate(cellValue, timeZone);
                }

                rowData.put(headerName, cellValue);
            }

            headersMap.forEach((header, index) -> {
                PdfPCell cell = new PdfPCell();
                cell.setPhrase(new Phrase(rowData.get(header), font));
                table.addCell(cell);
            });
        }
    }

    private String formatDate(String dateValue, String timeZone) {
        try {
            // Преобразуем строку с датой в формат Date
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Указываем UTC как исходный часовой пояс

            Date date = utcFormat.parse(dateValue); // Парсим строку в объект Date

            // Преобразуем в указанную таймзону
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));  // Устанавливаем таймзону

            return dateFormat.format(date);  // Возвращаем отформатированную дату в нужной таймзоне
        } catch (ParseException e) {
            return dateValue;  // Если не удалось распарсить дату, возвращаем строку как есть
        }
    }

    private String extractValue(JsonNode valueNode) {
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
                    arrayValues.add(extractValue(arrayElement));
                }
            }
            if (!arrayValues.isEmpty()) {
                sb.append(String.join(", ", arrayValues));
            }
        } else if (valueNode.isObject()) {
            if (valueNode.has("values") && valueNode.get("values").isArray()) {
                List<String> objectValues = new ArrayList<>();
                for (JsonNode objValue : valueNode.get("values")) {
                    objectValues.add(objValue.asText());
                }
                sb.append(String.join(", ", objectValues));
            } else if (valueNode.has("value")) {
                sb.append(valueNode.get("value").asText());
            }
        } else if (valueNode.isTextual()) {
            sb.append(valueNode.asText());
        }

        return sb.toString();
    }


    private Font getFontForCyrillic() throws DocumentException, IOException {
        InputStream fontStream = new ClassPathResource("fonts/times.ttf").getInputStream();
        BaseFont bf = BaseFont.createFont("times.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, IOUtils.toByteArray(fontStream), null);
        Font font = new Font(bf, 8);
        return font;
    }
}
