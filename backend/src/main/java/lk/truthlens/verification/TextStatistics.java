package lk.truthlens.verification;

import java.text.BreakIterator;
import java.util.*;

public final class TextStatistics {

    public static Map<String, Integer> measure(String content) {
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(content);
        int words = 0,
            start = iterator.first();
        for (
            int end = iterator.next();
            end != BreakIterator.DONE;
            start = end, end = iterator.next()
        ) {
            if (
                content.substring(start, end).codePoints().anyMatch(Character::isLetterOrDigit)
            ) words++;
        }
        int paragraphs = (int) content
            .lines()
            .filter(line -> !line.isBlank())
            .count();
        return Map.of(
            "characters",
            content.codePointCount(0, content.length()),
            "words",
            words,
            "paragraphs",
            paragraphs
        );
    }

    private TextStatistics() {}
}
