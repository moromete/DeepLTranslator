package de.linus.deepltranslator;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DeepLTranslatorTest {
    @Test
    public void testHeadlessDetection() throws IOException, InterruptedException {
        // DeepLTranslator.HEADLESS = false;
        WebDriver driver = DeepLTranslator.newWebDriver();

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
    public void testTranslateRemote() {
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

    // @Test
    // public void testTranslateAsync() {
    //     DeepLConfiguration deepLConfiguration = new DeepLConfiguration.Builder()
    //             .setRepetitions(0)
    //             .build();

    //     DeepLTranslator.HEADLESS = false;
    //     DeepLTranslator deepLTranslator = new DeepLTranslator(deepLConfiguration);

    //     deepLTranslator.translateAsync("Hello world", SourceLanguage.ENGLISH, TargetLanguage.GERMAN)
    //             .whenComplete((translation, ex) -> {
    //                 if (ex != null) {
    //                     ex.printStackTrace();
    //                 } else {
    //                     System.out.print(translation);
    //                     Assertions.assertEquals(translation, "Hallo Welt");
    //                 }

    //             });
    //     // DeepLTranslator.shutdown();

    // }

}
