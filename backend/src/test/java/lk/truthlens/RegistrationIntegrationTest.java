package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper json;

    static int address = 1;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    org.springframework.test.web.servlet.ResultActions send(Map<String, Object> fields)
        throws Exception {
        return mvc.perform(
            post("/api/auth/register")
                .with(r -> {
                    r.setRemoteAddr("10.0.0." + address++);
                    return r;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(fields))
        );
    }

    Map<String, Object> valid() {
        return new HashMap<>(
            Map.of(
                "name",
                "  Jane Silva  ",
                "email",
                "Jane@Example.com",
                "password",
                "SafePassword123"
            )
        );
    }

    @Test
    void validRegistrationHashesAndReturnsOnlySafeFields() throws Exception {
        send(valid())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.name").value("Jane Silva"))
            .andExpect(jsonPath("$.user.email").value("jane@example.com"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        UserAccount u = users.findAll().get(0);
        assertNotEquals("SafePassword123", u.getPasswordHash());
        assertTrue(encoder.matches("SafePassword123", u.getPasswordHash()));
        assertTrue(u.getPasswordHash().startsWith("$2a$12$"));
    }

    @Test
    void duplicateIsCaseInsensitiveAndTrimmed() throws Exception {
        send(valid()).andExpect(status().isCreated());
        var second = valid();
        second.put("email", " jane@example.COM ");
        send(second)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errors.email").exists());
        assertEquals(1, users.count());
    }

    @Test
    void requiredFieldsAreRejected() throws Exception {
        send(Map.of())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.password").exists());
        assertEquals(0, users.count());
    }

    @Test
    void malformedEmailAndWhitespaceNameAreRejected() throws Exception {
        var f = valid();
        f.put("email", "wrong@@example.com");
        f.put("name", "   ");
        send(f)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void passwordPolicyRejectsWeakAndOverByteLimit() throws Exception {
        for (String p : List.of("short1", "abcdefghijkl", "123456789012", "A1" + "é".repeat(36))) {
            var f = valid();
            f.put("password", p);
            send(f)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        }
        assertEquals(0, users.count());
    }

    @Test
    void unicodeNamesAndPasswordBoundaryAccepted() throws Exception {
        var f = valid();
        f.put("name", "අෆ්රින් Silva");
        f.put("password", "A1" + "x".repeat(70));
        send(f).andExpect(status().isCreated());
    }

    @Test
    void publicRegistrationCannotAssignAdmin() throws Exception {
        var f = valid();
        f.put("role", "ADMIN");
        send(f).andExpect(status().isBadRequest());
        assertEquals(0, users.count());
    }

    @Test
    void malformedJsonDoesNotLeakInternals() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void otherEndpointsAreProtected() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/register")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateConcurrentRequestsHaveOneWinner() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final int n = i;
            futures.add(
                pool.submit(() -> {
                    start.await();
                    return mvc
                        .perform(
                            post("/api/auth/register")
                                .with(r -> {
                                    r.setRemoteAddr("192.168.1." + n);
                                    return r;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(valid()))
                        )
                        .andReturn()
                        .getResponse()
                        .getStatus();
                })
            );
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        try {
            for (var f : futures) statuses.add(f.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        Collections.sort(statuses);
        assertEquals(List.of(201, 409), statuses);
        assertEquals(1, users.count());
    }

    @Test
    void tooManyAttemptsAreLimited() throws Exception {
        for (int i = 0; i < 20; i++) mvc.perform(
            post("/api/auth/register")
                .with(r -> {
                    r.setRemoteAddr("172.16.0.1");
                    return r;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest());
        mvc.perform(
            post("/api/auth/register")
                .with(r -> {
                    r.setRemoteAddr("172.16.0.1");
                    return r;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "600"));
    }

    @Test
    void oversizedJsonIsRejected() throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("x".repeat(9000))
        )
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.message").exists());
    }
}
