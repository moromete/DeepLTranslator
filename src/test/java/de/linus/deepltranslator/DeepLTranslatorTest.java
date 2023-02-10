package de.linus.deepltranslator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

public class DeepLTranslatorTest {
    @Test
    void testHeadlessDetection() throws IOException, InterruptedException {
        // DeepLTranslator.HEADLESS = false;
        WebDriver driver = DeepLTranslator.newWebDriver();

        ((ChromeDriver) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                Map.of("source",
                        "Object.defineProperty(navigator, 'plugins', {get: function() {return [1, 2, 3, 4, 5];},});"));

        driver.get("https://infosimples.github.io/detect-headless/");

        Alert alt = driver.switchTo().alert();
        alt.accept();

        // WebElement hoverable = driver.findElement(By.className("headless"));
        // new Actions(driver).moveToElement(hoverable).perform();
        // Thread.sleep(1000);
        // hoverable = driver.findElement(By.className("undefined"));
        // new Actions(driver).moveToElement(hoverable).perform();
        // WebElement tracker = driver.findElement(By.tagName("body"));
        // new Actions(driver)
        // .moveToElement(tracker, 8, 0)
        // .perform();

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

        Thread.sleep(5000);

        driver.close();
        driver.quit();
    }

    @Test
    void testTranslate() {
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .build();

        DeepLTranslator.HEADLESS = false;
        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);
        String translation = deepLTranslator.translate("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN);
        translation = translation.trim();
        Assertions.assertEquals(translation, "Hallo Welt");

        DeepLTranslator.shutdown();
    }

    @Test
    void testTranslateHeadless() {
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
    void testTranslateRemote() {
        DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
                .setRepetitions(0)
                .remoteWebDriverUrl("http://localhost:4444")
                .build();

        DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);
        String translation = deepLTranslator.translate("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN);
        translation = translation.trim();
        Assertions.assertEquals(translation, "Hallo Welt");

        DeepLTranslator.shutdown();
    }

}
