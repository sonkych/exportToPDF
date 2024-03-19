package ee.hcapp.javaservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChromeDriverConfig {

    @Value("${webdriver.chrome.driver:}")
    private String chromeDriverPath;

    public String getChromeDriverPath() {
        return chromeDriverPath;
    }
}