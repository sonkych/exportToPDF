package ee.hcapp.javaservice.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JsonExcelConverter {

    public byte[] convert(MultipartFile file, String timezone)
        throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file.getInputStream());

        try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Data");

            LinkedHashMap<String, Integer> headersMap = createHeadersMap(
                rootNode
            );
            boolean hasTableFields = createHeadersRow(sheet, rootNode);

            fillData(sheet, rootNode, headersMap, hasTableFields, timezone);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private boolean createHeadersRow(Sheet sheet, JsonNode rootNode) {
        boolean hasTableFields = false;
        int rowNum = 0; // Начинаем с первой строки
        Row headerRow = sheet.createRow(rowNum++); // Первая строка для заголовков

        int cellIndex = 0; // Индекс для ячеек
        Row subHeaderRow = null; // Строка для подзаголовков (будет создана один раз)

        // Получаем первый элемент из rootNode
        JsonNode node = rootNode.get(0);
        JsonNode itemsNode = node.get("items");

        // Перебираем все итемы первого элемента
        for (JsonNode item : itemsNode) {
            String header = item.get("name").asText();
            boolean isTableField = isTableField(item);

            // Если это TableField, то обрабатываем его как особый случай
            if (isTableField) {
                // Получаем количество колонок в таблице
                JsonNode valueNode = item.get("value");
                int columnCount = getTableColumnCount(valueNode);

                if (columnCount > 0) {
                    hasTableFields = true;
                    // Объединяем ячейки для главного заголовка на первой строке
                    sheet.addMergedRegion(
                        new CellRangeAddress(
                            0,
                            0,
                            cellIndex,
                            cellIndex + columnCount - 1
                        )
                    );
                    Cell cell = headerRow.createCell(cellIndex);
                    cell.setCellValue(header); // Заголовок для TableField
                    cellIndex += columnCount; // Пропускаем несколько колонок

                    // Если строка подзаголовков ещё не была создана, создаём её
                    if (subHeaderRow == null) {
                        subHeaderRow = sheet.createRow(1); // Подзаголовки идут во второй строке
                    }

                    // Рисуем подзаголовки для вложенной таблицы в уже существующую строку
                    List<String> subHeaders = getTableHeaders(valueNode); // Получаем заголовки для вложенной таблицы

                    // Пропускаем ячейки, чтобы они выровнялись под главным заголовком
                    int subHeaderCellIndex = cellIndex - columnCount;
                    for (int i = 0; i < subHeaders.size(); i++) {
                        subHeaderRow
                            .createCell(subHeaderCellIndex + i)
                            .setCellValue(subHeaders.get(i));
                    }
                } else {
                    // Нет строк таблицы — рисуем как обычный заголовок
                    Cell cell = headerRow.createCell(cellIndex++);
                    cell.setCellValue(header);
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
        if (
            tableValueNode != null &&
            tableValueNode.isArray() &&
            tableValueNode.size() > 0
        ) {
            JsonNode firstRow = tableValueNode.get(0);
            if (firstRow.has("items") && firstRow.get("items").isArray()) {
                for (JsonNode item : firstRow.get("items")) {
                    subHeaders.add(item.get("name").asText());
                }
            }
        }
        return subHeaders;
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

    private void fillData(
        Sheet sheet,
        JsonNode rootNode,
        LinkedHashMap<String, Integer> headersMap,
        boolean hasTableFields,
        String timezone
    ) {
        int itemFirstRowNum = 1;
        if (hasTableFields) {
            itemFirstRowNum = 2; // Для данных с таблицами начинаем с третьей строки
        }

        int tableLastRow = itemFirstRowNum;

        for (JsonNode node : rootNode) {
            int addCellsFromTables = 0;
            if (tableLastRow > itemFirstRowNum) {
                itemFirstRowNum = tableLastRow;
            }
            Row row = sheet.createRow(itemFirstRowNum); // создаем строку для каждого элемента
            JsonNode itemsNode = node.get("items");

            for (Map.Entry<
                String,
                Integer
            > headerEntry : headersMap.entrySet()) {
                String headerName = headerEntry.getKey();
                int cellNum = headerEntry.getValue() + addCellsFromTables;

                JsonNode valueNode = findValueNodeByName(itemsNode, headerName);

                // Если это таблица, заполняем её
                if (
                    valueNode != null &&
                    valueNode.has("value") &&
                    valueNode.get("value").isArray() &&
                    isTableField(valueNode)
                ) {
                    JsonNode tableValueNode = valueNode.get("value");
                    if (tableValueNode.size() == 0) {
                        continue;
                    }
                    // Передаем строку и индекс для первой строки таблицы
                    int lastRow = fillTable(
                        sheet,
                        row,
                        cellNum,
                        tableValueNode,
                        itemFirstRowNum,
                        timezone
                    );
                    if (lastRow > tableLastRow) {
                        tableLastRow = lastRow;
                    }
                    JsonNode firstRowItems = tableValueNode.get(0).get("items");
                    if (firstRowItems != null && firstRowItems.isArray()) {
                        addCellsFromTables =
                            addCellsFromTables + (firstRowItems.size() - 1);
                    }
                } else {
                    Cell cell = row.createCell(cellNum);
                    writeValueToCell(valueNode, cell, timezone);
                }
            }
            itemFirstRowNum++;
        }

        // Автоматически подстраиваем ширину столбцов
        for (int i = 0; i < headersMap.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int fillTable(
        Sheet sheet,
        Row row,
        int startColumn,
        JsonNode tableValueNode,
        int startRow,
        String timezone
    ) {
        int rowNum = startRow; // Начинаем с текущего индекса строки

        // Проходим по каждой строке в таблице
        for (int i = 0; i < tableValueNode.size(); i++) {
            JsonNode tableRow = tableValueNode.get(i);

            // Если это не первая строка, ищем или создаем строку
            if (i > 0) {
                row = sheet.getRow(rowNum); // Если строка уже существует, не создаем новую
                if (row == null) {
                    row = sheet.createRow(rowNum); // Если строки нет, создаем новую
                }
            }

            int columnNum = startColumn; // Начинаем с переданного индекса столбца
            // Проходим по каждому элементу в текущем ряду
            for (JsonNode item : tableRow.get("items")) {
                // Записываем значение в текущую ячейку
                Cell cell = row.createCell(columnNum);
                writeValueToCell(item, cell, timezone);
                columnNum++; // Переходим к следующему столбцу
            }

            // Увеличиваем rowNum только если это не последняя строка
            if (i < tableValueNode.size() - 1) {
                rowNum++;
            }
        }

        return rowNum;
    }

    private void writeValueToCell(
        JsonNode valueNode,
        Cell cell,
        String timezone
    ) {
        if (valueNode == null) {
            return; // Если значение отсутствует, ничего не делаем
        }

        // Определяем тип поля по form_field_type
        if (valueNode.has("form_field_type")) {
            String fieldType = valueNode.get("form_field_type").asText();

            if ("LinkField".equals(fieldType) || "Link".equals(fieldType)) {
                String url = valueNode.has("value")
                    ? valueNode.get("value").asText()
                    : "";
                if (!url.isEmpty()) {
                    setLinkCell(cell, url);
                }
                return;
            }

            // Обработка типа "NumberField" (число)
            if ("NumberField".equals(fieldType) || "Number".equals(fieldType)) {
                String value = valueNode
                    .get("value")
                    .asText()
                    .replace(",", "."); // Заменяем запятую на точку для чисел
                try {
                    // Преобразуем строку в число и записываем
                    double numericValue = Double.parseDouble(value);
                    cell.setCellValue(numericValue);
                    return;
                } catch (NumberFormatException e) {
                    // Если не удалось преобразовать в число, записываем как строку
                    cell.setCellValue(valueNode.asText());
                    return;
                }
            }

            // Обработка типов "DateField" и "DueDateField" (дата)
            if (
                "DateField".equals(fieldType) ||
                "DueDateField".equals(fieldType) ||
                "Date".equals(fieldType) ||
                "DueDate".equals(fieldType)
            ) {
                try {
                    String dateValue = valueNode.get("value").asText();

                    // Преобразуем строку с датой в формат Date
                    SimpleDateFormat utcFormat = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                    );
                    utcFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Указываем UTC как исходный часовой пояс

                    Date date = utcFormat.parse(dateValue); // Парсим строку в объект Date

                    // Преобразуем в Эстонское время
                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "dd.MM.yyyy"
                    );
                    dateFormat.setTimeZone(TimeZone.getTimeZone(timezone)); // Устанавливаем таймзону  // Применяем формат для даты

                    cell.setCellValue(dateFormat.format(date));
                } catch (ParseException e) {
                    // Если не удалось распарсить как дату, записываем как строку
                    cell.setCellValue(valueNode.asText());
                }
                return;
            }
        }

        // Обработка массивов и объектов с "values"
        if (valueNode.isArray()) {
            List<String> arrayValues = new ArrayList<>();
            for (JsonNode arrayElement : valueNode) {
                // Рекурсивная обработка массива или объекта
                arrayValues.add(extractValue(arrayElement)); // Получаем строковое значение
            }
            if (!arrayValues.isEmpty()) {
                String joinedValues = String.join(", ", arrayValues);
                cell.setCellValue(joinedValues);
                // Устанавливаем стиль для оборачивания текста, если есть новая строка
                if (joinedValues.contains("\n")) {
                    setWrapTextStyle(cell);
                }
            }
        } else if (valueNode.isObject()) {
            if (valueNode.has("values") && valueNode.get("values").isArray()) {
                List<String> objectValues = new ArrayList<>();
                valueNode
                    .get("values")
                    .forEach(objValue -> objectValues.add(objValue.asText()));
                String joinedObjectValues = String.join(", ", objectValues);
                cell.setCellValue(joinedObjectValues);
                // Устанавливаем стиль для оборачивания текста, если есть новая строка
                if (joinedObjectValues.contains("\n")) {
                    setWrapTextStyle(cell);
                }
            } else if (
                valueNode.has("value") && !valueNode.get("value").isArray()
            ) {
                // Если это поле объекта, записываем его "value"
                String value = valueNode.get("value").asText();
                cell.setCellValue(value);
                // Устанавливаем стиль для оборачивания текста, если есть новая строка
                if (value.contains("\n")) {
                    setWrapTextStyle(cell);
                }
            } else if (
                valueNode.has("value") && valueNode.get("value").isArray()
            ) {
                List<String> objectValues = new ArrayList<>();
                valueNode
                    .get("value")
                    .forEach(objValue -> objectValues.add(objValue.asText()));
                String joinedObjectValues = String.join(", ", objectValues);
                cell.setCellValue(joinedObjectValues);

                if (joinedObjectValues.contains("\n")) {
                    setWrapTextStyle(cell);
                }
            }
        } else if (valueNode.isTextual()) {
            // Если значение текстовое, просто записываем как строку
            String value = valueNode.asText();
            cell.setCellValue(value);
            // Устанавливаем стиль для оборачивания текста, если есть новая строка
            if (value.contains("\n")) {
                setWrapTextStyle(cell);
            }
        }
    }

    private void setWrapTextStyle(Cell cell) {
        CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
        cellStyle.setWrapText(true);
        cell.setCellStyle(cellStyle);
    }

    private void setLinkCell(Cell cell, String url) {
        Workbook workbook = cell.getSheet().getWorkbook();
        CreationHelper creationHelper = workbook.getCreationHelper();
        Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(url);
        cell.setHyperlink(hyperlink);
        cell.setCellValue(url);

        CellStyle linkStyle = workbook.createCellStyle();
        Font linkFont = workbook.createFont();
        linkFont.setUnderline(Font.U_SINGLE);
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        linkStyle.setFont(linkFont);
        cell.setCellStyle(linkStyle);
    }

    private String extractValue(JsonNode valueNode) {
        // Этот метод будет использоваться для извлечения значений вложенных объектов
        StringBuilder sb = new StringBuilder();

        if (valueNode != null) {
            // Пример рекурсивной обработки
            if (valueNode.isArray()) {
                List<String> arrayValues = new ArrayList<>();
                for (JsonNode arrayElement : valueNode) {
                    if (arrayElement.isTextual()) {
                        arrayValues.add(arrayElement.asText());
                    } else if (
                        arrayElement.isObject() || arrayElement.isArray()
                    ) {
                        arrayValues.add(extractValue(arrayElement)); // Рекурсивный вызов для вложенных объектов
                    }
                }
                sb.append(String.join(", ", arrayValues));
            } else if (valueNode.isObject()) {
                if (
                    valueNode.has("values") && valueNode.get("values").isArray()
                ) {
                    List<String> objectValues = new ArrayList<>();
                    valueNode
                        .get("values")
                        .forEach(objValue ->
                            objectValues.add(objValue.asText())
                        );
                    sb.append(String.join(", ", objectValues));
                } else if (valueNode.has("value")) {
                    sb.append(valueNode.get("value").asText());
                }
            } else if (valueNode.isTextual()) {
                sb.append(valueNode.asText());
            }
        }

        return sb.toString(); // Возвращаем строку для записи в ячейку
    }

    private int getTableColumnCount(JsonNode tableValueNode) {
        // Предположим, что все ряды таблицы имеют одинаковое количество колонок
        if (
            tableValueNode != null &&
            tableValueNode.isArray() &&
            tableValueNode.size() > 0
        ) {
            JsonNode firstRow = tableValueNode.get(0);
            if (firstRow.has("items") && firstRow.get("items").isArray()) {
                return firstRow.get("items").size();
            }
        }
        return 0; // Если таблица пустая или нет данных
    }

    private JsonNode findValueNodeByName(JsonNode itemsNode, String name) {
        for (JsonNode item : itemsNode) {
            // Проверяем, есть ли нужный name
            if (item.has("name") && item.get("name").asText().equals(name)) {
                // Если это TableField, возвращаем всю структуру
                if (isTableField(item)) {
                    return item; // Возвращаем всю структуру таблицы (вместо только value)
                }
                // Для обычных элементов возвращаем значение
                return item;
            }
        }
        return null;
    }

    private boolean isTableField(JsonNode item) {
        if (item == null || !item.has("form_field_type")) {
            return false;
        }
        String fieldType = item.get("form_field_type").asText();
        return "TableField".equals(fieldType) || "Table".equals(fieldType);
    }
}
