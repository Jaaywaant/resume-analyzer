package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobDescriptionScraperServiceTest {

    private JobDescriptionScraperService scraperService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getScraper().setMinTextLength(80);
        scraperService = new JobDescriptionScraperService(properties);
    }

    @Test
    void extractJobTextPrefersMainAndStripsChrome() {
        String html = """
                <html><body>
                  <nav>Home Careers Login</nav>
                  <header>Company Logo</header>
                  <main class="job-description">
                    <h1>Senior Java Backend Engineer</h1>
                    <p>We need Spring Boot, Kafka, AWS, and SQL experience for APIs and data pipelines.</p>
                    <p>Preferred: Docker, Kubernetes, and observability tooling.</p>
                  </main>
                  <footer>Copyright 2026</footer>
                  <script>window.track = true;</script>
                </body></html>
                """;
        Document document = Jsoup.parse(html);

        String text = scraperService.extractJobText(document);

        assertTrue(text.contains("Senior Java Backend Engineer"));
        assertTrue(text.contains("Spring Boot"));
        assertFalse(text.contains("Home Careers Login"));
        assertFalse(text.contains("Copyright 2026"));
        assertFalse(text.contains("window.track"));
    }

    @Test
    void cleanTextCollapsesWhitespace() {
        String cleaned = scraperService.cleanText("  Java   Spring\n\n\nBoot  \tKafka  ");
        assertTrue(cleaned.startsWith("Java Spring"));
        assertTrue(cleaned.contains("Boot Kafka"));
        assertFalse(cleaned.contains("\n\n\n"));
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class,
                () -> scraperService.parseAndValidate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> scraperService.parseAndValidate("ftp://example.com/jd"));
    }

    @Test
    void rejectsLocalhost() {
        assertThrows(IllegalArgumentException.class,
                () -> scraperService.parseAndValidate("http://localhost:8080/jobs/1"));
    }

    @Test
    void acceptsPublicHttpsUrl() {
        var uri = scraperService.parseAndValidate("https://example.com/careers/java-engineer");
        assertTrue(uri.getHost().equals("example.com"));
    }
}
