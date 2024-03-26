package ee.hcapp.javaservice.web.rest;

import com.google.common.net.HttpHeaders;
import ee.hcapp.javaservice.converter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/converter")
public class ConverterResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterResource.class);
    private final LibreOfficeService libreOfficeService;
    private final HtmlPdfConverter htmlPdfConverter;
    private final JsonExcelConverter jsonExcelConverter;
    private final JsonCsvConverter jsonCsvConverter;
    private final JsonPdfConverter jsonPdfConverter;

    public ConverterResource(LibreOfficeService libreOfficeService, HtmlPdfConverter htmlPdfConverter, JsonExcelConverter jsonExcelConverter, JsonCsvConverter jsonCsvConverter, JsonPdfConverter jsonPdfConverter) {
        this.libreOfficeService = libreOfficeService;
        this.htmlPdfConverter = htmlPdfConverter;
        this.jsonExcelConverter = jsonExcelConverter;
        this.jsonCsvConverter = jsonCsvConverter;
        this.jsonPdfConverter = jsonPdfConverter;
    }

    @PostMapping("/excel2pdf")
    public ResponseEntity<?> convertExcelToPdf(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to convert Excel file to PDF.");

        if (file.isEmpty()) {
            LOGGER.error("Failed to convert Excel to PDF: file is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            byte[] pdfBytes = libreOfficeService.convertExcel2PdfWithOfficeAsServer(file);
            if (pdfBytes == null) {
                LOGGER.error("Conversion failed with null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Conversion failed.");
            }
            LOGGER.info("Successfully converted Excel file to PDF.");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }

    @PostMapping("/html2pdf")
    public ResponseEntity<?> convertHtmlToPdf(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to convert Html file to PDF.");

        if (file.isEmpty()) {
            LOGGER.error("Failed to convert Html to PDF: file is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            byte[] pdfBytes = htmlPdfConverter.convertHtmlToPdfUsingSelenium(file);
            if (pdfBytes == null) {
                LOGGER.error("Conversion failed with null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Conversion failed.");
            }
            LOGGER.info("Successfully converted Html file to PDF.");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }

    @PostMapping("/json2excel")
    public ResponseEntity<?> convertJsonToExcel(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to convert JSON to Excel.");

        if (file.isEmpty()) {
            LOGGER.error("Failed to convert JSON to Excel: file is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            byte[] excelBytes = jsonExcelConverter.convert(file);
            if (excelBytes == null) {
                LOGGER.error("Conversion failed with null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Conversion failed.");
            }
            LOGGER.info("Successfully converted JSON file to Excel.");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.xlsx\"")
                    .body(excelBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }

    @PostMapping("/json2csv")
    public ResponseEntity<?> convertJsonToCsv(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to convert JSON to CSV.");

        if (file.isEmpty()) {
            LOGGER.error("Failed to convert JSON to CSV: file is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            byte[] csvBytes = jsonCsvConverter.convert(file);
            if (csvBytes == null) {
                LOGGER.error("Conversion failed with null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Conversion failed.");
            }
            LOGGER.info("Successfully converted JSON file to CSV.");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.csv\"")
                    .body(csvBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }

    @PostMapping("/json2pdf")
    public ResponseEntity<?> convertJsonToPdf(@RequestParam("file") MultipartFile file) {
        LOGGER.info("Received request to convert JSON to PDF.");

        if (file.isEmpty()) {
            LOGGER.error("Failed to convert JSON to PDF: file is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            byte[] pdfBytes = jsonPdfConverter.convert(file);
            if (pdfBytes == null) {
                LOGGER.error("Conversion failed with null response.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Conversion failed.");
            }
            LOGGER.info("Successfully converted JSON file to PDF.");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }
}
