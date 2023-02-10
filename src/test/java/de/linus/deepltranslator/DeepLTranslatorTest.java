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
    void testHeadlessDetection() throws IOException {
        WebDriver driver = DeepLTranslator.newWebDriver();
        driver.get("https://infosimples.github.io/detect-headless/");

        Alert alt = driver.switchTo().alert();
        alt.accept();

        List<WebElement> elements = driver.findElements(By.xpath("//tr[@class='headless']"));
        elements.forEach(element -> {
            System.out.print("\033[0;31m" + element.getText() + "\033[0m \n");
        });

        Assertions.assertTrue(elements.size() == 0);

        driver.close();
        driver.quit();
    }
}
