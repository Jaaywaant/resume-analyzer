package com.jaywant.resumeanalyzer.service;

import com.jaywant.resumeanalyzer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic JD fetch + HTML → text cleaning (hybrid AI: code before the LLM).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDescriptionScraperService {

    private static final Pattern MULTI_BLANK = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");

    private final AppProperties appProperties;

    public String scrape(String jobUrl) {
        URI uri = parseAndValidate(jobUrl);
        AppProperties.Scraper scraper = appProperties.getScraper();
        try {
            Document document = Jsoup.connect(uri.toString())
                    .userAgent(scraper.getUserAgent())
                    .timeout(scraper.getTimeoutMs())
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();
            String text = extractJobText(document);
            if (text.length() < scraper.getMinTextLength()) {
                throw new JobDescriptionScrapeException(
                        "Job page did not contain enough readable text (got "
                                + text.length() + " chars). The posting may be behind a login or rendered by JavaScript.");
            }
            log.info("Scraped {} characters from {}", text.length(), uri.getHost());
            return text;
        }
        catch (JobDescriptionScrapeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new JobDescriptionScrapeException(
                    "Failed to fetch job description from URL: " + ex.getMessage(), ex);
        }
    }

    /**
     * Package-visible for unit tests — cleans HTML without hitting the network.
     */
    String extractJobText(Document document) {
        Document copy = document.clone();
        copy.select("script, style, noscript, svg, iframe, canvas, template").remove();
        copy.select("nav, header, footer, aside").remove();
        copy.select("[role=navigation], [role=banner], [role=contentinfo], [aria-hidden=true]").remove();

        String raw = preferMainContent(copy);
        return cleanText(raw);
    }

    private String preferMainContent(Document document) {
        Elements candidates = document.select(
                "main, article, [role=main], .job-description, .jobDescription, "
                        + "#job-description, .description, .posting, .job-details");
        for (Element candidate : candidates) {
            String text = candidate.text();
            if (text != null && text.strip().length() >= appProperties.getScraper().getMinTextLength()) {
                return text;
            }
        }
        Element body = document.body();
        return body == null ? document.text() : body.text();
    }

    String cleanText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.replace('\u00a0', ' ');
        normalized = MULTI_BLANK.matcher(normalized).replaceAll(" ");
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        normalized = MULTI_NEWLINE.matcher(normalized).replaceAll("\n\n");
        return normalized.strip();
    }

    URI parseAndValidate(String jobUrl) {
        if (jobUrl == null || jobUrl.isBlank()) {
            throw new IllegalArgumentException("Job URL is required");
        }
        String trimmed = jobUrl.strip();
        URI uri;
        try {
            uri = new URI(trimmed);
        }
        catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid job URL: " + trimmed, ex);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Job URL must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Job URL must include a host");
        }

        rejectPrivateOrLocalHost(uri.getHost());
        return uri.normalize();
    }

    private void rejectPrivateOrLocalHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost") || h.endsWith(".localhost") || h.equals("metadata.google.internal")) {
            throw new IllegalArgumentException("Job URL host is not allowed: " + host);
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Job URL must not target a private or local network address");
            }
        }
        catch (UnknownHostException ex) {
            throw new JobDescriptionScrapeException("Could not resolve job URL host: " + host, ex);
        }
    }
}
