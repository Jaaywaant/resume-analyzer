package com.jaywant.resumeanalyzer.web;

import com.jaywant.resumeanalyzer.domain.AnalysisResult;
import com.jaywant.resumeanalyzer.service.AnalysisService;
import com.jaywant.resumeanalyzer.service.DocumentService;
import com.jaywant.resumeanalyzer.service.JobDescriptionScraperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UiControllerTest {

    @Mock
    private DocumentService documentService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private JobDescriptionScraperService jobDescriptionScraperService;

    @InjectMocks
    private UiController uiController;

    @Test
    void indexShowsSavedResumeFromSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UiController.SESSION_RESUME_TEXT, "Java");
        session.setAttribute(UiController.SESSION_RESUME_NAME, "cv.pdf");

        Model model = new ConcurrentModel();
        assertEquals("index", uiController.index(model, session));
        assertEquals(true, model.getAttribute("hasSavedResume"));
        assertEquals("cv.pdf", model.getAttribute("resumeFileName"));
    }

    @Test
    void analyzeWithPastJdShowsResultAndSavesResume() {
        when(documentService.extractText(org.mockito.ArgumentMatchers.any()))
                .thenReturn("Java Spring Boot");
        when(analysisService.analyze(anyString(), anyString()))
                .thenReturn(new AnalysisResult(
                        80,
                        List.of("Java"),
                        List.of("Kafka"),
                        "Solid backend fit.",
                        List.of("Add metrics"),
                        List.of("java"),
                        List.of("kafka"),
                        List.of()));

        Model model = new ConcurrentModel();
        MockHttpSession session = new MockHttpSession();
        MockMultipartFile resume = new MockMultipartFile(
                "resume", "resume.txt", "text/plain", "Java Spring Boot".getBytes());

        String view = uiController.analyze(resume, "Need Java and Spring Boot", null, model, session);

        assertEquals("index", view);
        assertNotNull(model.getAttribute("result"));
        assertEquals("resume.txt", session.getAttribute(UiController.SESSION_RESUME_NAME));
        assertEquals(true, model.getAttribute("hasSavedResume"));
        verify(analysisService).analyze(eq("Java Spring Boot"), eq("Need Java and Spring Boot"));
    }

    @Test
    void analyzeReusesSessionResumeWithoutNewUpload() {
        when(analysisService.analyze(anyString(), anyString()))
                .thenReturn(new AnalysisResult(70, List.of(), List.of(), "ok", List.of(), List.of(), List.of(), List.of()));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UiController.SESSION_RESUME_TEXT, "Saved Java resume");
        session.setAttribute(UiController.SESSION_RESUME_NAME, "jaywant.pdf");

        Model model = new ConcurrentModel();
        uiController.analyze(null, "Need Java", null, model, session);

        verifyNoInteractions(documentService);
        verify(analysisService).analyze(eq("Saved Java resume"), eq("Need Java"));
        assertEquals("jaywant.pdf", model.getAttribute("resumeFileName"));
    }

    @Test
    void analyzeRequiresJdOrUrl() {
        Model model = new ConcurrentModel();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UiController.SESSION_RESUME_TEXT, "Java");
        session.setAttribute(UiController.SESSION_RESUME_NAME, "r.txt");

        uiController.analyze(null, "  ", "  ", model, session);

        assertTrue(String.valueOf(model.getAttribute("error")).contains("job description"));
    }

    @Test
    void analyzeFromUrlUsesScraper() {
        when(jobDescriptionScraperService.scrape("https://example.com/job"))
                .thenReturn("Need Java engineer");
        when(documentService.extractText(org.mockito.ArgumentMatchers.any()))
                .thenReturn("Java");
        when(analysisService.analyze(anyString(), anyString()))
                .thenReturn(new AnalysisResult(70, List.of(), List.of(), "ok", List.of(), List.of(), List.of(), List.of()));

        Model model = new ConcurrentModel();
        MockHttpSession session = new MockHttpSession();
        MockMultipartFile resume = new MockMultipartFile(
                "resume", "resume.txt", "text/plain", "Java".getBytes());

        uiController.analyze(resume, null, "https://example.com/job", model, session);

        verify(jobDescriptionScraperService).scrape("https://example.com/job");
        verify(analysisService).analyze(eq("Java"), eq("Need Java engineer"));
    }
}
