package de.linus.deepltranslator;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UselessFileDetector;

public class WebDriverBuilder {

    static String REMOTE_WEBDRIVER_URL;
    // static boolean HEADLESS;
    private boolean headless;
    static String USER_AGENT;
    static Duration TIMEOUT;

    static {
        // HEADLESS = true;
        TIMEOUT = Duration.ofSeconds(10);
        USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";
        // setUserAgent();
    }

    // private static void setUserAgent() {
    //     WebDriver dummyDriver = WebDriverBuilder.builder().headless(true).build();
    //     String userAgent = (String) ((ChromeDriver) dummyDriver).executeScript("return navigator.userAgent");
    //     USER_AGENT = userAgent.replace("HeadlessChrome", "Chrome");
    //     dummyDriver.close();
    // }

    static WebDriverBuilder builder() {
        return new WebDriverBuilder();
    }

    public WebDriver build() {
        if (REMOTE_WEBDRIVER_URL == null) {
            return newWebDriver();
        } else {
            return newRemoteWebDriver();
        }
    }

    public WebDriver newWebDriver() {
        ChromeOptions chromeOptions = getChromeOptions();
        ChromeDriver driver = new ChromeDriver(chromeOptions);

        permissionsTest(driver);
        rttTest(driver);
        pluginsTest(driver);
        mimetest(driver);
        chromeObjectTest(driver);

        setScreen(driver);

        long timeoutMillisEnd = System.currentTimeMillis() + TIMEOUT.toMillis();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));

        return driver;
    }

    private void chromeObjectTest(ChromeDriver driver) {
        // Pass Chrome object test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "window.chrome = {runtime: {},};"));
    }

    private void mimetest(ChromeDriver driver) {
        // Pass mime test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'mimeTypes', { " +
                                "get: () => { " +
                                "       var mime = {};" +
                                "       mime.__proto__ = MimeType.prototype;" +
                                "       var mimes = {" +
                                "           0: mime," +
                                "           length: 1," +
                                "           __proto__: MimeTypeArray.prototype," +
                                "       };" +
                                "       return mimes;" +
                                "   }, " +
                                "});"));
    }

    private void pluginsTest(ChromeDriver driver) {
        // Pass plugins prototype test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'plugins', { " +
                                "get: () => { " +
                                "       var ChromiumPDFPlugin = {};" +
                                "       ChromiumPDFPlugin.__proto__ = Plugin.prototype;"
                                +
                                "       var plugins = {" +
                                "           0: ChromiumPDFPlugin," +
                                "           description: 'Portable Document Format'," +
                                "           filename: 'internal-pdf-viewer'," +
                                "           length: 1," +
                                "           name: 'Chromium PDF Plugin'," +
                                "           __proto__: PluginArray.prototype," +
                                "       };" +
                                "       return plugins;" +
                                "   }, " +
                                "});"));
    }

    private void rttTest(ChromeDriver driver) {
        // Pass rtt test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'maxTouchPoints', {get: () => 1}); " +
                                "Object.defineProperty(navigator.connection, 'rtt', {get: () => 100});"));
    }

    private void permissionsTest(ChromeDriver driver) {
        // Pass the permissions test
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("permissions", Arrays.asList("notifications"));
        driver.executeCdpCommand("Browser.grantPermissions", params);
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();

        if (headless) {
            chromeOptions.addArguments("--headless");
        }

        chromeOptions.addArguments("--disable-gpu", "--window-size=1920,1080");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        if (USER_AGENT != null) {
            chromeOptions.addArguments("--user-agent=" + USER_AGENT);
        }
        return chromeOptions;
    }

    /**
     * Create new RemoteWebDriver instance.
     */
    private WebDriver newRemoteWebDriver() {
        ChromeOptions chromeOptions = new ChromeOptions();
        RemoteWebDriver driver = null;
        try {
            driver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_URL), chromeOptions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid chrome remote url " + REMOTE_WEBDRIVER_URL);
        }
        return driver;
    }

    private static void setScreen(ChromeDriver driver) {
        (driver).executeScript(
                "Object.defineProperty(screen, 'height', {value: 1080, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'width', {value: 1920, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'availWidth', {value: 1920, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'availHeight', {value: 1080, configurable: true, writeable: true});");
    }

    public WebDriverBuilder headless(boolean headless) {
        this.headless = headless;
        return this;
    }
}
