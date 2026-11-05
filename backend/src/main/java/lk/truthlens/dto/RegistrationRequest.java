package lk.truthlens.dto;

import jakarta.validation.constraints.*;

public record RegistrationRequest(
    @NotBlank(message = "Please enter your name.")
    @Size(min = 2, max = 100, message = "Use between 2 and 100 characters.")
    String name,
    @NotBlank(message = "Please enter your email address.")
    @Size(max = 254, message = "Use an email address of at most 254 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$",
        message = "Enter a valid email, like name@example.com."
    )
    String email,
    @NotNull(message = "Please enter a password.") String password
) {
    public RegistrationRequest {
        name = name == null ? null : name.strip();
        email = email == null ? null : email.strip();
    }
}
