package ee.hcapp.javaservice.converter;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HtmlPdfConverter {

    public byte[] convertHtmlToPdfUsingSelenium(MultipartFile htmlFile) throws IOException {
        // Проверяем, работаем ли мы в контейнере
        boolean isDocker = System.getenv("IS_DOCKER") != null;

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        if (isDocker) {
            log.info(" ****** Running in Docker ****** ");
            // Используем статически установленный ChromeDriver
            System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        } else {
            // Локальная разработка - используем WebDriverManager
            WebDriverManager.chromedriver().setup();
        }

        // Инициализируем ChromeDriver
        ChromeDriver driver = new ChromeDriver(options);

        // Логика работы с ChromeDriver остается прежней
        File tempFile = File.createTempFile("tempHtml", ".html");
        try {
            htmlFile.transferTo(tempFile);
            driver.get(tempFile.toURI().toString());

            // Выполняем команду CDP для печати в PDF
            Map<String, Object> params = new HashMap<>();
            String command = "Page.printToPDF";
            Map<String, Object> result = driver.executeCdpCommand(command, params);

            driver.quit();

            byte[] pdfContent = Base64.getDecoder().decode((String) result.get("data"));
            return pdfContent;

        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }
}
