package sk.testlab.cvicenie02.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.UUID;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** Dôkazy testu. Zlyhanie screenshotu nikdy neprepíše pôvodnú assertion. */
public final class TestArtifacts {
    private final Properties metadata = new Properties();
    public final String runId;
    public final Path directory;

    public TestArtifacts(String testName, TestConfig config) {
        runId = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC)
            .format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        directory = Path.of("target", "artifacts", runId);
        try { Files.createDirectories(directory); }
        catch (IOException e) { throw new UncheckedIOException("Nedá sa vytvoriť priečinok dôkazov.", e); }
        metadata.setProperty("run.id", runId);
        metadata.setProperty("test", testName);
        metadata.setProperty("started.utc", Instant.now().toString());
        metadata.setProperty("base.url", config.baseUrl);
        metadata.setProperty("application.version.declared", config.declaredAppVersion);
        metadata.setProperty("application.version.source", "configuration; not measured from GUI");
        metadata.setProperty("browser.requested", config.browser);
        metadata.setProperty("headless", Boolean.toString(config.headless));
        metadata.setProperty("java.version", System.getProperty("java.version"));
        metadata.setProperty("os.name", System.getProperty("os.name"));
        metadata.setProperty("cleanup.requested", Boolean.toString(config.cleanupOrder));
        put("result", "RUNNING");
        System.out.println("Dôkazy testu: " + directory.toAbsolutePath());
    }

    public void put(String key, String value) {
        metadata.setProperty(key, value == null ? "<null>" : value);
        try (Writer writer = Files.newBufferedWriter(directory.resolve("run.properties"), StandardCharsets.UTF_8)) {
            metadata.store(writer, "TestLab Shop / cvicenie 02 / bez hesiel a tokenov");
        } catch (IOException e) { throw new UncheckedIOException("Zápis metadát testu zlyhal.", e); }
    }

    public void write(String filename, String text) {
        try { Files.writeString(directory.resolve(filename), text, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new UncheckedIOException("Zápis " + filename + " zlyhal.", e); }
    }

    public void recordBrowser(WebDriver driver) {
        if (driver instanceof HasCapabilities browser) {
            put("browser.name", browser.getCapabilities().getBrowserName());
            put("browser.version", browser.getCapabilities().getBrowserVersion());
        }
    }

    public void screenshot(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot camera)) {
            throw new IllegalStateException("Prehliadač neposkytuje screenshot (relácia ešte nevznikla alebo zanikla).");
        }
        try { Files.write(directory.resolve(name), camera.getScreenshotAs(OutputType.BYTES)); }
        catch (IOException e) { throw new UncheckedIOException("Uloženie screenshotu zlyhalo.", e); }
    }

    public void failure(WebDriver driver, String prefix, Throwable error) {
        // Každú operáciu skúšame zvlášť; problém s URL nesmie zabrániť screenshotu.
        try {
            StringWriter stack = new StringWriter(); error.printStackTrace(new PrintWriter(stack));
            write(prefix + ".txt", stack.toString());
        } catch (RuntimeException loggingError) { System.err.println("Nedá sa uložiť výnimka: " + loggingError); }
        if (driver != null) {
            try { put("failure.url", driver.getCurrentUrl()); }
            catch (RuntimeException loggingError) { System.err.println("URL nie je dostupná: " + loggingError.getClass().getSimpleName()); }
        }
        try { screenshot(driver, prefix + ".png"); }
        catch (RuntimeException screenshotError) {
            try { write(prefix + "-screenshot-unavailable.txt", screenshotError.toString()); }
            catch (RuntimeException loggingError) { System.err.println("Screenshot ani záznam chyby nie sú dostupné."); }
        }
    }
}
