package de.linus.deepltranslator;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DeepLTranslatorTest {

    static String REMOTE_WEBDRIVER_URL;

    @BeforeAll
    static void initializeTesting() {
        REMOTE_WEBDRIVER_URL = "http://localhost:4444";
    }

    @Test
    public void testHeadlessDetection() throws IOException, InterruptedException {
        WebDriverBuilder.USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";
        WebDriver driver = WebDriverBuilder.builder().headless(true).build();

        driver.get("https://infosimples.github.io/detect-headless/");

        Alert alt = driver.switchTo().alert();
        alt.accept();

        List<WebElement> elements = driver.findElements(By.xpath("//tr[@class='headless']"));
        elements.forEach(element -> {
            System.out.print("\033[0;31m" + element.getText() + "\033[0m \n");
        });
        Assertions.assertTrue(elements.size() == 0);

        elements = driver.findElements(By.xpath("//tr[@class='undefined']"));
        elements.forEach(element -> {
            System.out.print("\033[0;33m" + element.getText() + "\033[0m \n");
        });

        elements = driver.findElements(By.xpath("//tr[@class='headful']"));
        elements.forEach(element -> {
            System.out.print("\033[0;32m" + element.getText() + "\033[0m \n");
        });

        // Thread.sleep(5000);

        driver.close();
        driver.quit();
    }

    @Test
    public void testTranslate() {
        DeepLTranslator.HEADLESS = false;
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .build();
        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);
        String translation = deepLTranslator.translate("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN);
        translation = translation.trim();
        Assertions.assertEquals(translation, "Hallo Welt");
        DeepLTranslator.shutdown();
    }

    @Test
    public void testTranslateHeadless() {
        DeepLTranslator.HEADLESS = true;
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .build();

        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);
        String translation = deepLTranslator.translate("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN);
        translation = translation.trim();
        Assertions.assertEquals(translation, "Hallo Welt");
        DeepLTranslator.shutdown();
    }

    @Test
    public void testTranslateAsyncHeadless() {
        DeepLTranslator.HEADLESS = true;
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .build();
        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);

        deepLTranslator.translateAsync("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN)
                .whenComplete((translation, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                    } else {
                        Assertions.assertEquals(translation, "Hallo Welt");
                    }

                });

        try {
            deepLTranslator.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DeepLTranslator.shutdown();
    }

    @Test
    public void testTranslateRemote() {
        DeepLTranslator.HEADLESS = false;
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .remoteWebDriverUrl(REMOTE_WEBDRIVER_URL)
                .build();

        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);
        String translation = deepLTranslator.translate("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN);
        translation = translation.trim();
        Assertions.assertEquals(translation, "Hallo Welt");
        DeepLTranslator.shutdown();
    }

    @AfterAll
    static void terminateTesting() throws IOException {
        Runtime.getRuntime().exec("killall chromedriver");
        Runtime.getRuntime().exec("killall chrome");
    }
}
