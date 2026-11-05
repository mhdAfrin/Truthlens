package lk.truthlens.exception;

public class RegistrationValidationException extends RuntimeException {

    public final java.util.Map<String, String> errors;

    public RegistrationValidationException(java.util.Map<String, String> errors) {
        this.errors = errors;
    }
}
