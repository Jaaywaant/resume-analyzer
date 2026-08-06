package com.jaywant.resumeanalyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        try {
            String text = tika.parseToString(file.getInputStream());
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Could not extract text from the uploaded file");
            }
            log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
            return text.strip();
        }
        catch (IOException | TikaException ex) {
            throw new IllegalArgumentException("Failed to read resume file", ex);
        }
    }
}
