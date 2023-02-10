package de.linus.deepltranslator;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * API for the DeepL Translator
 */
class DeepLTranslatorBase {

    /**
     * For asynchronous translating.
     *
     * @see DeepLTranslator#translateAsync(String, SourceLanguage, TargetLanguage)
     */
    final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * All executors used for asynchronous translating.
     */
    static final List<ExecutorService> EXECUTOR_LIST = new ArrayList<>();

    /**
     * For cleaning up the input field on the DeepL site.
     *
     * @see DeepLTranslator#translate(String, SourceLanguage, TargetLanguage)
     */
    static final ExecutorService CLEANUP_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * All browser instances created.
     */
    static final List<WebDriver> GLOBAL_INSTANCES = new ArrayList<>();

    /**
     * Available browser instances for this configuration.
     */
    private static final LinkedBlockingQueue<WebDriver> AVAILABLE_INSTANCES = new LinkedBlockingQueue<>();

    /**
     * User-Agent for WebDriver.
     */
    private static final String USER_AGENT;

    /**
     * Script to disable animations on a website.
     * <p>
     * Source: https://github.com/dcts/remove-CSS-animations
     */
    private static final String DISABLE_ANIMATIONS_SCRIPT = "document.querySelector('html > head').insertAdjacentHTML(\"beforeend\", \""
            +
            "<style>\\n" +
            "* {\\n" +
            "  -o-transition-property: none !important;\\n" +
            "  -moz-transition-property: none !important;\\n" +
            "  -ms-transition-property: none !important;\\n" +
            "  -webkit-transition-property: none !important;\\n" +
            "  transition-property: none !important;\\n" +
            "}\\n" +
            "* {\\n" +
            "  -o-transform: none !important;\\n" +
            "  -moz-transform: none !important;\\n" +
            "  -ms-transform: none !important;\\n" +
            "  -webkit-transform: none !important;\\n" +
            "  transform: none !important;\\n" +
            "}\\n" +
            "* {\\n" +
            "  -webkit-animation: none !important;\\n" +
            "  -moz-animation: none !important;\\n" +
            "  -o-animation: none !important;\\n" +
            "  -ms-animation: none !important;\\n" +
            "  animation: none !important;\\n" +
            "}\\n" +
            "</style>\\n" +
            "\");";

    /**
     * For debugging purposes.
     */
    public static boolean HEADLESS = true;

    static {
        // Set default user agent
        WebDriver dummyDriver = newWebDriver();
        String userAgent = (String) ((ChromeDriver) dummyDriver).executeScript("return navigator.userAgent");
        USER_AGENT = userAgent.replace("HeadlessChrome", "Chrome");
        dummyDriver.close();
    }

    /**
     * All settings.
     */
    private final DeepLConfiguration configuration;

    /**
     * With default settings.
     */
    DeepLTranslatorBase() {
        this.configuration = new DeepLConfiguration.Builder().build();
        EXECUTOR_LIST.add(executor);
    }

    /**
     * With custom settings.
     */
    DeepLTranslatorBase(DeepLConfiguration configuration) {
        this.configuration = configuration;
        EXECUTOR_LIST.add(executor);
    }

    /**
     * Checks if all arguments are valid, if not, an exception is thrown.
     */
    void isValid(String text, SourceLanguage from, TargetLanguage to) throws IllegalStateException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalStateException("Text is null or empty");
        } else if (from == null || to == null) {
            throw new IllegalStateException("Language is null");
        } else if (text.length() > 5000) {
            throw new IllegalStateException("Text length is limited to 5000 characters");
        }
    }

    /**
     * Generates a request with all settings like timeout etc.
     * and returns the translation if succeeded.
     */
    String getTranslation(String text, SourceLanguage from, TargetLanguage to) throws TimeoutException {
        long timeoutMillisEnd = System.currentTimeMillis() + configuration.getTimeout().toMillis();
        WebDriver driver = AVAILABLE_INSTANCES.poll();

        try {
            if (driver == null) {
                if (configuration.getRemoteWebDriverUrl() != null) {
                    driver = newRemoteWebDriver(configuration.getRemoteWebDriverUrl());
                } else {
                    driver = newWebDriver();
                }
                driver.manage().timeouts()
                        .pageLoadTimeout(Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));

                GLOBAL_INSTANCES.add(driver);
                driver.get("https://www.deepl.com/translator");
                ((RemoteWebDriver) driver).executeScript(DISABLE_ANIMATIONS_SCRIPT);
            }
        } catch (

        TimeoutException e) {
            GLOBAL_INSTANCES.remove(driver);
            driver.close();
            throw e;
        }

        try {
            // close Chrome extension install dialog
            By node = By.xpath("//*[name()='button'][@aria-label='Close']");
            WebDriverWait waitNode = new WebDriverWait(driver,
                    Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));
            try {
                waitNode.until(ExpectedConditions.visibilityOfElementLocated(node));
                driver.findElement(node).click();
            } catch (Exception e) {
                // TODO: handle exception
            }

            // Source language button
            driver.findElements(By.className("lmt__language_select__active")).get(0).click();
            By srcButtonBy = By.xpath("//button[@dl-test='" + from.getAttributeValue() + "']");
            WebDriverWait waitSource = new WebDriverWait(driver,
                    Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));
            waitSource.until(ExpectedConditions.visibilityOfElementLocated(srcButtonBy));
            driver.findElement(srcButtonBy).click();

            // Target language button
            driver.findElements(By.className("lmt__language_select__active")).get(1).click();
            By targetButtonBy = By.xpath("//button[@dl-test='" + to.getAttributeValue() + "']");
            WebDriverWait waitTarget = new WebDriverWait(driver,
                    Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));
            waitTarget.until(ExpectedConditions.visibilityOfElementLocated(targetButtonBy));
            driver.findElement(targetButtonBy).click();
        } catch (TimeoutException e) {
            AVAILABLE_INSTANCES.offer(driver);
            throw e;
        }

        String result = null;
        TimeoutException timeoutException = null;
        By targetTextBy = By.id("target-dummydiv");

        try {
            // Source text
            driver.findElement(By.className("lmt__source_textarea")).sendKeys(text);

            // Target text
            WebDriverWait waitText = new WebDriverWait(driver,
                    Duration.ofMillis(timeoutMillisEnd - System.currentTimeMillis()));
            waitText.pollingEvery(Duration.ofMillis(100));
            ExpectedCondition<Boolean> textCondition;

            if (text.contains("[...]")) {
                textCondition = ExpectedConditions.and(
                        DriverWaitUtils.attributeNotBlank(targetTextBy, "innerHTML"),
                        DriverWaitUtils.attributeNotChanged(targetTextBy, "innerHTML", Duration.ofMillis(1000)));
            } else {
                textCondition = ExpectedConditions.and(
                        DriverWaitUtils.attributeNotBlank(targetTextBy, "innerHTML"),
                        DriverWaitUtils.attributeNotContains(targetTextBy, "innerHTML", "[...]"),
                        DriverWaitUtils.attributeNotChanged(targetTextBy, "innerHTML", Duration.ofMillis(1000)));
            }

            waitText.until(textCondition);
            result = driver.findElement(targetTextBy).getAttribute("innerHTML");
        } catch (TimeoutException e) {
            timeoutException = e;
        }

        WebDriver finalDriver = driver;

        CLEANUP_EXECUTOR.submit(() -> {
            By buttonClearBy = By.className("lmt__clear_text_button");
            By sourceText = By.id("source-dummydiv");

            try {
                finalDriver.findElement(buttonClearBy).click();
            } catch (NoSuchElementException ignored) {
            }

            WebDriverWait waitCleared = new WebDriverWait(finalDriver, Duration.ofSeconds(10));

            try {
                waitCleared.until(ExpectedConditions.and(
                        DriverWaitUtils.attributeBlank(sourceText, "innerHTML"),
                        DriverWaitUtils.attributeBlank(targetTextBy, "innerHTML")));
                AVAILABLE_INSTANCES.offer(finalDriver);
            } catch (TimeoutException e) {
                GLOBAL_INSTANCES.remove(finalDriver);
                finalDriver.close();
            }
        });

        if (timeoutException != null)
            throw timeoutException;

        // Post-processing
        if (result != null && configuration.isPostProcessingEnabled()) {
            result = result
                    .trim()
                    .replaceAll("\\s{2,}", " ");
        }

        return result;
    }

    /**
     * The settings.
     */
    public DeepLConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Create new WebDriver instance.
     */
    public static WebDriver newWebDriver() {
        ChromeOptions chromeOptions = new ChromeOptions();

        if (HEADLESS) {
            chromeOptions.addArguments("--headless");
        }

        chromeOptions.addArguments("--disable-gpu", "--window-size=1920,1080");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        if (USER_AGENT != null) {
            chromeOptions.addArguments("--user-agent=" + USER_AGENT);
        }

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        // Pass the permissions test
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("permissions", Arrays.asList("notifications"));
        driver.executeCdpCommand("Browser.grantPermissions", params);

        // Pass rtt test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'maxTouchPoints', {get: () => 1}); Object.defineProperty(navigator.connection, 'rtt', {get: () => 100});"));

        // Pass plugins prototype test
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'plugins', { " +
                                "get: () => { " +
                                "       var ChromiumPDFPlugin = {};" +
                                "       ChromiumPDFPlugin.__proto__ = Plugin.prototype;" +
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

        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "window.chrome = {runtime: {},};"));

        setScreen(driver);
        return driver;
    }

    /**
     * Create new RemoteWebDriver instance.
     */
    private static WebDriver newRemoteWebDriver(String remoteWebDriverUrl) {
        ChromeOptions chromeOptions = new ChromeOptions();
        RemoteWebDriver driver = null;
        try {
            driver = new RemoteWebDriver(new URL(remoteWebDriverUrl), chromeOptions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid chrome remote url " + remoteWebDriverUrl);
        }
        setScreen(driver);
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

    private static void setScreen(RemoteWebDriver driver) {
        (driver).executeScript(
                "Object.defineProperty(screen, 'height', {value: 1080, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'width', {value: 1920, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'availWidth', {value: 1920, configurable: true, writeable: true});");
        (driver).executeScript(
                "Object.defineProperty(screen, 'availHeight', {value: 1080, configurable: true, writeable: true});");
    }

}
