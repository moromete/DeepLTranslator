package de.linus.deepltranslator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
    static ExecutorService CLEANUP_EXECUTOR;

    /**
     * All browser instances created.
     */
    static final List<WebDriver> GLOBAL_INSTANCES = new ArrayList<>();

    /**
     * Available browser instances for this configuration.
     */
    private static final LinkedBlockingQueue<WebDriver> AVAILABLE_INSTANCES = new LinkedBlockingQueue<>();

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
    // public boolean headless;

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
        CLEANUP_EXECUTOR = Executors.newCachedThreadPool();
    }

    /**
     * With custom settings.
     */
    DeepLTranslatorBase(DeepLConfiguration configuration) {
        this.configuration = configuration;
        EXECUTOR_LIST.add(executor);
        CLEANUP_EXECUTOR = Executors.newCachedThreadPool();
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
                WebDriverBuilder.REMOTE_WEBDRIVER_URL = configuration.getRemoteWebDriverUrl();
                WebDriverBuilder.USER_AGENT = configuration.getUserAgent();
                WebDriverBuilder.TIMEOUT = configuration.getTimeout();

                driver = WebDriverBuilder.builder().headless(HEADLESS).build();

                GLOBAL_INSTANCES.add(driver);
                driver.get("https://www.deepl.com/translator");
                ((RemoteWebDriver) driver).executeScript(DISABLE_ANIMATIONS_SCRIPT);

            }
        } catch (

        TimeoutException e) {
            GLOBAL_INSTANCES.remove(driver);
            if (driver != null) {
                driver.close();
            }
            throw e;
        }

        try {
            closeCromeExtensionInstallDialog(timeoutMillisEnd, driver);

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
            By buttonClearBy = By.className("lmt__clear_text_button_wrapper");
            By sourceText = By.id("source-dummydiv");
            finalDriver.findElement(buttonClearBy).click();

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

    private void closeCromeExtensionInstallDialog(long timeoutMillisEnd, WebDriver driver) {
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
    }

    /**
     * The settings.
     */
    public DeepLConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Tries to quit all browsers and all active threads, which were started for
     * asynchronous translating.
     * This method does not wait for the running tasks to finish.
     */
    public static void shutdown() {
        GLOBAL_INSTANCES.forEach(WebDriver::quit);
        EXECUTOR_LIST.forEach(ExecutorService::shutdownNow);
        CLEANUP_EXECUTOR.shutdownNow();
        AVAILABLE_INSTANCES.clear();
        GLOBAL_INSTANCES.clear();
        EXECUTOR_LIST.clear();
    }
}
