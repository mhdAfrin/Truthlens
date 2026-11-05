package lk.truthlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
)
public class TruthLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(TruthLensApplication.class, args);
    }
}
