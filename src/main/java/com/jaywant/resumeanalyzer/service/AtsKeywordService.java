package com.jaywant.resumeanalyzer.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AtsKeywordService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+#./-]{1,}");

    public List<String> extractKeywords(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(jobDescription);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (token.length() >= 3 && !isStopWord(token)) {
                keywords.add(token);
            }
            if (keywords.size() >= 30) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    public List<String> findPresentKeywords(String resumeText, List<String> keywords) {
        String resume = resumeText.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .filter(resume::contains)
                .toList();
    }

    public List<String> findMissingKeywords(String resumeText, List<String> keywords) {
        String resume = resumeText.toLowerCase(Locale.ROOT);
        return keywords.stream()
                .filter(keyword -> !resume.contains(keyword))
                .toList();
    }

    private boolean isStopWord(String token) {
        return switch (token) {
            case "and", "the", "for", "with", "you", "our", "your", "will", "are", "this", "that", "from", "have", "has" -> true;
            default -> false;
        };
    }
}
