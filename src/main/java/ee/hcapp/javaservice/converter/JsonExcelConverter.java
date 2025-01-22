package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
            boolean hasTableFields = createHeadersRow(sheet, rootNode);

            fillData(sheet, rootNode, headersMap, hasTableFields);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private boolean createHeadersRow(Sheet sheet, JsonNode rootNode) {
        boolean hasTableFields = false;
        int rowNum = 0; // Начинаем с первой строки
        Row headerRow = sheet.createRow(rowNum++); // Первая строка для заголовков

        int cellIndex = 0; // Индекс для ячеек

        // Получаем первый элемент из rootNode
        JsonNode node = rootNode.get(0);
        JsonNode itemsNode = node.get("items");

        // Перебираем все итемы первого элемента
        for (JsonNode item : itemsNode) {
            String header = item.get("name").asText();
            boolean isTableField = item.has("form_field_type") && "TableField".equals(item.get("form_field_type").asText());

            // Если это TableField, то обрабатываем его как особый случай
            if (isTableField) {
                hasTableFields = true;
                // Получаем количество колонок в таблице
                JsonNode valueNode = item.get("value");
                int columnCount = getTableColumnCount(valueNode);

                // Объединяем ячейки для главного заголовка на первой строке
                sheet.addMergedRegion(new CellRangeAddress(0, 0, cellIndex, cellIndex + columnCount - 1));
                Cell cell = headerRow.createCell(cellIndex);
                cell.setCellValue(header); // Заголовок для TableField
                cellIndex += columnCount; // Пропускаем несколько колонок

                // Рисуем подзаголовки для вложенной таблицы на следующей строке
                Row subHeaderRow = sheet.createRow(rowNum++);
                List<String> subHeaders = getTableHeaders(valueNode); // Получаем заголовки для вложенной таблицы

                // Пропускаем ячейки, чтобы они выровнялись под главным заголовком
                int subHeaderCellIndex = cellIndex - columnCount;
                for (int i = 0; i < subHeaders.size(); i++) {
                    subHeaderRow.createCell(subHeaderCellIndex + i).setCellValue(subHeaders.get(i));
                }
            } else {
                // Для обычных элементов просто рисуем заголовок
                Cell cell = headerRow.createCell(cellIndex++);
                cell.setCellValue(header);
            }
        }

        return hasTableFields;
    }



    private List<String> getTableHeaders(JsonNode tableValueNode) {
        List<String> subHeaders = new ArrayList<>();
        if (tableValueNode != null && tableValueNode.isArray() && tableValueNode.size() > 0) {
            JsonNode firstRow = tableValueNode.get(0);
            for (JsonNode item : firstRow.get("items")) {
                subHeaders.add(item.get("name").asText());
            }
        }
        return subHeaders;
    }








    // ********************************************************************************************************************





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

    private void fillData(Sheet sheet, JsonNode rootNode, LinkedHashMap<String, Integer> headersMap, boolean hasTableFields) {
        int rowNum = 1;
        if (hasTableFields) {
            rowNum = 2; // Для данных с таблицами начинаем с третьей строки
        }

        int nextRowNum = rowNum;

        for (JsonNode node : rootNode) {
            Row row = sheet.createRow(nextRowNum); // создаем строку для каждого элемента
            JsonNode itemsNode = node.get("items");

            for (Map.Entry<String, Integer> headerEntry : headersMap.entrySet()) {
                String headerName = headerEntry.getKey();
                int cellNum = headerEntry.getValue();

                JsonNode valueNode = findValueNodeByName(itemsNode, headerName);

                // Если это таблица, заполняем её
                if (valueNode != null && valueNode.has("value") && valueNode.get("value").isArray()) {
                    JsonNode tableValueNode = valueNode.get("value");
                    // Передаем строку и индекс для первой строки таблицы
                    int tableLastRow = fillTable(sheet, row, cellNum, tableValueNode, nextRowNum);
                    if (tableLastRow > nextRowNum) {
                        nextRowNum = tableLastRow;
                    }
                } else {
                    Cell cell = row.createCell(cellNum);
                    String cellValue = extractValue(valueNode, cell);
                    cell.setCellValue(cellValue);
                }
            }

            nextRowNum++;
        }

        // Автоматически подстраиваем ширину столбцов
        for (int i = 0; i < headersMap.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }



    private int fillTable(Sheet sheet, Row row, int startColumn, JsonNode tableValueNode, int startRow) {
        int rowNum = startRow; // Начинаем с текущего индекса строки

        // Проходим по каждой строке в таблице
        for (int i = 0; i < tableValueNode.size(); i++) {
            JsonNode tableRow = tableValueNode.get(i);

            // Если это не первая строка, создаем новую строку
            if (i > 0) {
                row = sheet.createRow(rowNum); // создаем новую строку для текущей записи
            }

            int columnNum = startColumn; // Начинаем с переданного индекса столбца
            // Проходим по каждому элементу в текущем ряду
            for (JsonNode item : tableRow.get("items")) {
                // Записываем значение в текущую ячейку
                row.createCell(columnNum).setCellValue(item.get("value").asText());
                columnNum++; // Переходим к следующему столбцу
            }

            // Увеличиваем rowNum только если это не последняя строка
            if (i < tableValueNode.size() - 1) {
                rowNum++;
            }
        }

        return rowNum;
    }

    private int getTableColumnCount(JsonNode tableValueNode) {
        // Предположим, что все ряды таблицы имеют одинаковое количество колонок
        if (tableValueNode.isArray() && tableValueNode.size() > 0) {
            JsonNode firstRow = tableValueNode.get(0);
            return firstRow.get("items").size();
        }
        return 0; // Если таблица пустая или нет данных
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
            // Проверяем, есть ли нужный name
            if (item.has("name") && item.get("name").asText().equals(name)) {
                // Если это TableField, возвращаем всю структуру
                if (item.has("form_field_type") && "TableField".equals(item.get("form_field_type").asText())) {
                    return item; // Возвращаем всю структуру таблицы (вместо только value)
                }
                // Для обычных элементов возвращаем значение
                return item.get("value");
            }
        }
        return null;
    }
}
