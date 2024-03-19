package ee.hcapp.javaservice.converter;

import ee.hcapp.javaservice.config.ChromeDriverConfig;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Component
public class HtmlPdfConverter {
    private static final Logger logger = LoggerFactory.getLogger(HtmlPdfConverter.class);
    private final ChromeDriverConfig chromeDriverConfig;

    @Autowired
    public HtmlPdfConverter(ChromeDriverConfig chromeDriverConfig) {
        this.chromeDriverConfig = chromeDriverConfig;
    }

    public byte[] convertHtmlToPdfUsingSelenium(MultipartFile htmlFile) throws IOException {
        File tempFile = File.createTempFile("tempHtml", ".html");
        try {
            htmlFile.transferTo(tempFile);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--run-all-compositor-stages-before-draw");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            if(!chromeDriverConfig.getChromeDriverPath().isEmpty()) {
                String chromeDriverPath = chromeDriverConfig.getChromeDriverPath();
                System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            }

            ChromeDriver chromeDriver = new ChromeDriver(options);

            chromeDriver.get(tempFile.toURI().toString());
            Map<String, Object> params = new HashMap<>();
            String command = "Page.printToPDF";
            Map<String, Object> output = chromeDriver.executeCdpCommand(command, params);

            chromeDriver.quit();

            byte[] pdfContent = java.util.Base64.getDecoder().decode((String) output.get("data"));
            return pdfContent;

        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }
}