package ee.hcapp.javaservice.converter;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HtmlPdfConverter {

    private static final Duration RESOURCE_LOAD_TIMEOUT = Duration.ofSeconds(5);

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

        File tempFile = File.createTempFile("tempHtml", ".html");
        ChromeDriver driver = null;
        try {
            htmlFile.transferTo(tempFile);
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(RESOURCE_LOAD_TIMEOUT);
            // Load resources using the same media rules that PDF printing uses.
            driver.executeCdpCommand("Emulation.setEmulatedMedia", Map.of("media", "print"));
            // Navigation and resource readiness share one five-second budget.
            long loadStarted = System.nanoTime();
            try {
                driver.get(tempFile.toURI().toString());
                long remainingMillis = RESOURCE_LOAD_TIMEOUT.toMillis()
                    - Duration.ofNanos(System.nanoTime() - loadStarted).toMillis();
                if (remainingMillis > 0) {
                    driver.manage().timeouts().scriptTimeout(Duration.ofMillis(remainingMillis));
                    waitForPrintResources(driver, remainingMillis);
                }
            } catch (TimeoutException e) {
                log.warn("HTML resources exceeded the five-second loading budget; printing available content.");
            }
            // Cancel unfinished downloads before printing the available document.
            driver.executeCdpCommand("Page.stopLoading", Map.of());

            // Выполняем команду CDP для печати в PDF
            Map<String, Object> params = new HashMap<>();
            params.put("printBackground", true);
            String command = "Page.printToPDF";
            Map<String, Object> result = driver.executeCdpCommand(command, params);

            byte[] pdfContent = Base64.getDecoder().decode((String) result.get("data"));
            return pdfContent;

        } finally {
            try {
                if (driver != null) {
                    driver.quit();
                }
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        }
    }

    private void waitForPrintResources(ChromeDriver driver, long remainingMillis) {
        driver.executeAsyncScript("""
            const done = arguments[arguments.length - 1];
            let finished = false;
            const finish = () => {
                if (finished) return;
                finished = true;
                clearTimeout(timer);
                done();
            };
            const timer = setTimeout(finish, arguments[0]);
            const images = Array.from(document.images, image => {
                image.loading = 'eager';
                // Failed images must not prevent the rest of the document printing.
                return image.decode().catch(() => {});
            });
            // Trigger print layout so fonts used by print-only elements are requested.
            void document.documentElement.offsetHeight;
            Promise.all([document.fonts.ready, ...images]).then(finish, finish);
            """, remainingMillis);
    }
}
