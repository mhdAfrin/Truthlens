package lk.truthlens.verification;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

record FoundingStatement(String subject, String event, String year, boolean yearOnly) {
    private static final Pattern STATEMENT = Pattern.compile(
        "(?i)^(?<subject>.+?)\\s+(?:was|were)\\s+"
            + "(?<event>founded|established|formed|created)\\s+"
            + "(?:in\\s+|(?<day>on\\s+(?:\\d{1,2}\\s+[a-z]+\\s+|[a-z]+\\s+\\d{1,2},?\\s+)))?"
            + "(?<year>(?:18|19|20)\\d{2})"
            + "(?<extra>, and (?:formally )?began (?:its )?(?:work|operations) (?:on|in) [^;!?]+)?[.!]?$"
    );

    static Optional<FoundingStatement> parse(String text) {
        var match = STATEMENT.matcher(text.strip());
        if (!match.matches()) return Optional.empty();
        String event = match.group("event").toLowerCase(Locale.ROOT);
        if (event.equals("founded")) event = "established";
        return Optional.of(new FoundingStatement(
            match.group("subject"), event, match.group("year"),
            match.group("day") == null && match.group("extra") == null
        ));
    }

    static String name(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceFirst("^the\\s+", "").replaceAll("\\s+", " ");
    }

    boolean matchesSubject(String passageSubject, String pageTitle) {
        if (name(subject).equals(name(passageSubject))) return true;
        // An initialism is meaningful only on the named entity's own page.
        if (!name(subject).equals(name(pageTitle))) return false;
        StringBuilder initials = new StringBuilder();
        for (String word : name(subject).split(" ")) initials.append(word.charAt(0));
        return initials.length() >= 2 && initials.toString().equals(name(passageSubject));
    }
}
