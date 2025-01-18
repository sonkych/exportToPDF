package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DashboardJsonExcelConverter {

    public byte[] convertDashboard(MultipartFile file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dashboard");

            // Создаем заголовки
            String[] headers = {"", "Open Count", "Open Sum", "Overdue Count", "Overdue Sum", "Total Count", "Total Sum"};
            createHeadersRow(sheet, headers, workbook);

            // Заполняем данными
            JsonNode rowsNode = rootNode.get("rows");
            int rowNum = 1;
            for (JsonNode rowNode : rowsNode) {
                Row row = sheet.createRow(rowNum++);
                createDataRow(row, rowNode);
            }

            // Устанавливаем ширину столбцов
            setColumnWidths(sheet);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createHeadersRow(Sheet sheet, String[] headers, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setBorderBottom(BorderStyle.THIN);  // Нижняя граница для заголовков

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRow(Row row, JsonNode rowNode) {
        // Заполняем данные для каждой строки
        String name = rowNode.get("name").asText();
        row.createCell(0).setCellValue(name);

        // Open count и sum
        JsonNode openNode = rowNode.get("open");
        row.createCell(1).setCellValue(openNode.get("count").asInt());
        row.createCell(2).setCellValue(openNode.get("sum").asDouble());

        // Overdue count и sum
        JsonNode overdueNode = rowNode.get("overdue");
        row.createCell(3).setCellValue(overdueNode.get("count").asInt());
        row.createCell(4).setCellValue(overdueNode.get("sum").asDouble());

        // Total count и sum
        JsonNode totalNode = rowNode.get("total");
        row.createCell(5).setCellValue(totalNode.get("count").asInt());
        row.createCell(6).setCellValue(totalNode.get("sum").asDouble());
    }

    private void setColumnWidths(Sheet sheet) {
        // Устанавливаем авторазмер для первой колонки
        sheet.autoSizeColumn(0);

        // Устанавливаем ширину в два раза больше стандартной для остальных колонок
        int defaultWidth = 256 * 10; // стандартная ширина
        sheet.setColumnWidth(1, defaultWidth * 2);  // Колонка "Open Count"
        sheet.setColumnWidth(2, defaultWidth * 2);  // Колонка "Open Sum"
        sheet.setColumnWidth(3, defaultWidth * 2);  // Колонка "Overdue Count"
        sheet.setColumnWidth(4, defaultWidth * 2);  // Колонка "Overdue Sum"
        sheet.setColumnWidth(5, defaultWidth * 2);  // Колонка "Total Count"
        sheet.setColumnWidth(6, defaultWidth * 2);  // Колонка "Total Sum"
    }
}
