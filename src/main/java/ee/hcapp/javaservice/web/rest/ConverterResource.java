package ee.hcapp.javaservice.web.rest;


import ee.hcapp.javaservice.converter.HtmlPdfConverter;
import ee.hcapp.javaservice.converter.LibreOfficeService;
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

    public ConverterResource(LibreOfficeService libreOfficeService, HtmlPdfConverter htmlPdfConverter) {
        this.libreOfficeService = libreOfficeService;
        this.htmlPdfConverter = htmlPdfConverter;
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
                    .body(pdfBytes);
        } catch (Exception e) {
            LOGGER.error("Error processing file for conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Test";
    }
}
