package lk.truthlens.service;

import lk.truthlens.exception.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SRS input requirement: at most 20,000 Unicode code points, including whitespace. */
@Component
public class SubmissionPolicy {

    private final int maxCodePoints;

    public SubmissionPolicy(@Value("${truthlens.verification.max-code-points:20000}") int value) {
        if (value < 1 || value > 20000) throw new IllegalArgumentException(
            "Input limit must be between 1 and the SRS maximum of 20,000 code points."
        );
        maxCodePoints = value;
    }

    public int maxCodePoints() {
        return maxCodePoints;
    }

    public void validate(String content) {
        if (
            content == null ||
            content
                .codePoints()
                .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))
        ) throw new InvalidRequestException(
            "Paste an article or copied text before starting verification."
        );
        if (content.indexOf(0) >= 0) throw new InvalidRequestException(
            "Article text contains an unsupported null character. Remove it and try again."
        );
        if (
            content.codePointCount(0, content.length()) > maxCodePoints
        ) throw new InvalidRequestException(
            "Keep the article to " +
                String.format(java.util.Locale.US, "%,d", maxCodePoints) +
                " Unicode characters."
        );
    }
}
