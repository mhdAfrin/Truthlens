package lk.truthlens.exception;

public class ArticleImportException extends RuntimeException {

    public final String code;
    public final int status;

    public ArticleImportException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ArticleImportException blocked() {
        return new ArticleImportException(
            400,
            "BLOCKED_DESTINATION",
            "This address cannot be fetched. Use a publicly accessible article URL or paste its text instead."
        );
    }

    public static ArticleImportException invalid() {
        return new ArticleImportException(
            400,
            "INVALID_URL",
            "Enter a complete HTTP or HTTPS article URL without sign-in credentials."
        );
    }
}
