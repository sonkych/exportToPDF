package ee.hcapp.javaservice.converter;

import org.apache.commons.io.FilenameUtils;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LibreOfficeService {
    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeService.class);

    @Value("${libreoffice.path}")
    private String libreOfficePath;

    @Autowired
    private OfficeManager officeManager;

    public byte[] convertExcel2Pdf(MultipartFile excelFile) throws IOException, InterruptedException {
        Path sourceFilePath = null;
        Path outputFolderPath = null;
        try {
            sourceFilePath = Files.createTempFile(UUID.randomUUID().toString(), getExtension(excelFile.getOriginalFilename()));
            excelFile.transferTo(sourceFilePath.toFile());

            outputFolderPath = Files.createTempDirectory("pdfOutput");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    libreOfficePath, "--headless", "--convert-to", "pdf:writer_pdf_Export",
                    sourceFilePath.toString(), "--outdir", outputFolderPath.toString());
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("Error during conversion. Exit code: " + exitCode);
                return null;
            }

            File outputFile = findConvertedFile(outputFolderPath, "pdf");
            if (outputFile == null) {
                logger.error("Converted PDF file does not exist.");
                return null;
            }

            return Files.readAllBytes(outputFile.toPath());
        } finally {
            if (sourceFilePath != null) Files.deleteIfExists(sourceFilePath);
            if (outputFolderPath != null) deleteDirectoryRecursively(outputFolderPath.toFile());
        }
    }

    public byte[] convertExcel2PdfWithOfficeAsServer(MultipartFile excelFile) throws IOException, OfficeException {
        File inputFile = Files.createTempFile(UUID.randomUUID().toString(), getExtension(excelFile.getOriginalFilename())).toFile();
        excelFile.transferTo(inputFile);

        File outputFile = new File(inputFile.getParentFile(), FilenameUtils.getBaseName(inputFile.getName()) + ".pdf");

        LocalConverter.builder()
                .officeManager(officeManager)
                .build()
                .convert(inputFile)
                .to(outputFile)
                .execute();

        byte[] pdfBytes = Files.readAllBytes(outputFile.toPath());

        inputFile.delete();
        outputFile.delete();

        return pdfBytes;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : ".tmp";
    }

    private File findConvertedFile(Path directory, String extension) {
        File[] files = directory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith("." + extension));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        file.delete();
    }
}