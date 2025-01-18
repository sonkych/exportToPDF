package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;

@Service
public class DashboardJsonPdfConverter {

    public byte[] convertDashboard(MultipartFile file) throws IOException, DocumentException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        // Создание таблицы и заголовков
        PdfPTable table = createTable();

        // Добавляем заголовки
        addTableHeaders(table);

        // Заполняем таблицу данными
        fillData(table, rootNode);

        // Добавляем таблицу в PDF
        document.add(table);
        document.close();

        return out.toByteArray();
    }

    private PdfPTable createTable() {
        // Таблица будет состоять из 7 колонок
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        return table;
    }

    private void addTableHeaders(PdfPTable table) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

        // Цвет фона для заголовков
        BaseColor headerColor = new BaseColor(230, 230, 230);

        // Заголовки таблицы
        String[] headers = {"Name", "Open Count", "Open Sum", "Overdue Count", "Overdue Sum", "Total Count", "Total Sum"};

        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
            headerCell.setBackgroundColor(headerColor);
            table.addCell(headerCell);
        }
    }

    private void fillData(PdfPTable table, JsonNode rootNode) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10);

        for (JsonNode rowNode : rootNode.get("rows")) {
            // Добавляем имя
            String name = rowNode.get("name").asText();
            table.addCell(new PdfPCell(new Phrase(name, font)));

            // Open count и sum
            JsonNode openNode = rowNode.get("open");
            table.addCell(new PdfPCell(new Phrase(String.valueOf(openNode.get("count").asInt()), font)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(openNode.get("sum").asDouble()), font)));

            // Overdue count и sum
            JsonNode overdueNode = rowNode.get("overdue");
            table.addCell(new PdfPCell(new Phrase(String.valueOf(overdueNode.get("count").asInt()), font)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(overdueNode.get("sum").asDouble()), font)));

            // Total count и sum
            JsonNode totalNode = rowNode.get("total");
            table.addCell(new PdfPCell(new Phrase(String.valueOf(totalNode.get("count").asInt()), font)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(totalNode.get("sum").asDouble()), font)));
        }
    }
}
