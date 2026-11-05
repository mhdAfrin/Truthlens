package lk.truthlens.config;

import static lk.truthlens.security.SecurityResponses.reply;

import java.util.Locale;
import lk.truthlens.repository.UserRepository;
import lk.truthlens.security.ActiveAccountFilter;
import lk.truthlens.security.LoginRateLimitFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var account = users
                .findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole())
                .disabled(!account.isActive())
                .build();
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, UserRepository users) throws Exception {
        return http
            .csrf(c -> c.ignoringRequestMatchers("/api/auth/register"))
            .sessionManagement(s ->
                s
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(f -> f.changeSessionId())
            )
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a ->
                a
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/auth/csrf",
                        "/api/config/submission",
                        "/api/news",
                        "/api/verifications/*"
                    )
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/verifications",
                        "/api/verifications/url"
                    )
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/api/auth/me",
                        "/api/account/**",
                        "/api/verifications",
                        "/api/verifications/**"
                    )
                    .hasAnyRole("USER", "ADMIN")
                    .anyRequest()
                    .denyAll()
            )
            .formLogin(f ->
                f
                    .loginProcessingUrl("/api/auth/login")
                    .usernameParameter("email")
                    .successHandler((req, res, auth) -> reply(res, 200, "Signed in successfully."))
                    .failureHandler((req, res, ex) ->
                        reply(res, 401, "Unable to sign in with those credentials.")
                    )
            )
            .logout(l ->
                l
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((req, res, auth) ->
                        reply(res, 200, "Signed out successfully.")
                    )
            )
            .exceptionHandling(e ->
                e
                    .authenticationEntryPoint((req, res, ex) ->
                        reply(res, 401, "Please sign in to continue.")
                    )
                    .accessDeniedHandler((req, res, ex) ->
                        reply(
                            res,
                            403,
                            "This request is not permitted. Refresh the page or check your account access."
                        )
                    )
            )
            .addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new ActiveAccountFilter(users), AuthorizationFilter.class)
            .build();
    }
}
