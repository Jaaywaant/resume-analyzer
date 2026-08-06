package com.jaywant.resumeanalyzer.parser;

public final class TextTruncator {

    private TextTruncator() {
    }

    public static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String normalized = text.strip();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "\n...[truncated]";
    }
}
