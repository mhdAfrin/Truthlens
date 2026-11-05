package lk.truthlens.exception;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<Failure> historyInput(InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new Failure(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    ResponseEntity<Failure> missingReport() {
        return ResponseEntity.status(404).body(new Failure("Report not found.", Map.of()));
    }

    @ExceptionHandler(
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    )
    ResponseEntity<Failure> badParameter() {
        return ResponseEntity.badRequest().body(
            new Failure("Check the report ID, dates and filter values.", Map.of())
        );
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Failure> badContentType() {
        return ResponseEntity.status(415).body(
            new Failure("Send article text as a JSON form.", Map.of())
        );
    }

    @ExceptionHandler(AccountSessionRequiredException.class)
    ResponseEntity<Failure> submissionSession() {
        return ResponseEntity.status(401).body(
            new Failure(
                "Your session has expired. Sign in again to save this submission to your account.",
                Map.of()
            )
        );
    }

    @ExceptionHandler(ArticleImportException.class)
    ResponseEntity<?> articleImport(ArticleImportException ex) {
        return ResponseEntity.status(ex.status).body(
            Map.of("message", ex.getMessage(), "code", ex.code, "fallback", "TEXT")
        );
    }

    record Failure(String message, Map<String, String> errors) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Failure> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
            .getFieldErrors()
            .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
            new Failure("Please check the highlighted fields.", errors)
        );
    }

    @ExceptionHandler(RegistrationValidationException.class)
    ResponseEntity<Failure> invalid(RegistrationValidationException ex) {
        return ResponseEntity.badRequest().body(
            new Failure("Please check the highlighted fields.", ex.errors)
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<Failure> duplicate() {
        return ResponseEntity.status(409).body(
            new Failure(
                "An account with this email already exists.",
                Map.of("email", "This email is already registered. Please use a different address.")
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Failure> integrity(DataIntegrityViolationException ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        if (
            t instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())
        ) return duplicate();
        return ResponseEntity.status(500).body(
            new Failure("We couldn’t save your account. Please try again.", Map.of())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Failure> malformed() {
        return ResponseEntity.badRequest().body(new Failure("Send a valid form.", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Failure> unavailable() {
        return ResponseEntity.status(503).body(
            new Failure(
                "This service is temporarily unavailable. Please try again shortly.",
                Map.of()
            )
        );
    }
}
