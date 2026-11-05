package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper json;

    static AtomicInteger address = new AtomicInteger(1);
    String ip;
    UserAccount account;

    record Token(MockHttpSession session, String value, String header) {}

    @BeforeEach
    void setup() {
        users.deleteAll();
        ip = "192.0.2." + address.getAndIncrement();
        account = users.save(
            new UserAccount("Login Tester", "login@example.com", encoder.encode("SafePassword123"))
        );
    }

    Token token(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Token(
            (MockHttpSession) result.getRequest().getSession(false),
            body.get("token").asText(),
            body.get("headerName").asText()
        );
    }

    ResultActions login(Token t, String email, String password) throws Exception {
        return mvc.perform(
            post("/api/auth/login")
                .session(t.session())
                .header(t.header(), t.value())
                .param("email", email)
                .param("password", password)
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                })
        );
    }

    MockHttpSession signedIn() throws Exception {
        var t = token(null);
        login(t, "login@example.com", "SafePassword123").andExpect(status().isOk());
        return t.session();
    }

    @Test
    void activeLoginRotatesSessionAndPersistsAuthentication() throws Exception {
        var t = token(null);
        String old = t.session().getId();
        login(t, " LOGIN@EXAMPLE.COM ", "SafePassword123").andExpect(status().isOk());
        assertNotEquals(old, t.session().getId());
        mvc.perform(get("/api/account/profile").session(t.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("login@example.com"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unknownEmailAndWrongPasswordHaveIdenticalErrors() throws Exception {
        var t = token(null);
        var a = login(t, "unknown@example.com", "SafePassword123")
            .andExpect(status().isUnauthorized())
            .andReturn();
        var b = login(t, "login@example.com", "WrongPassword123")
            .andExpect(status().isUnauthorized())
            .andReturn();
        assertEquals(a.getResponse().getContentAsString(), b.getResponse().getContentAsString());
    }

    @Test
    void inactiveAccountCannotLoginAndGetsSameError() throws Exception {
        account.setActive(false);
        users.save(account);
        var t = token(null);
        var a = login(t, "login@example.com", "SafePassword123")
            .andExpect(status().isUnauthorized())
            .andReturn();
        var b = login(t, "unknown@example.com", "SafePassword123")
            .andExpect(status().isUnauthorized())
            .andReturn();
        assertEquals(a.getResponse().getContentAsString(), b.getResponse().getContentAsString());
    }

    @Test
    void deactivationRevokesExistingSession() throws Exception {
        var session = signedIn();
        account.setActive(false);
        users.save(account);
        mvc.perform(get("/api/account/profile").session(session)).andExpect(
            status().isUnauthorized()
        );
        assertTrue(session.isInvalid());
    }

    @Test
    void anonymousPersonalAndAdminRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void ordinaryUserCannotAccessAdmin() throws Exception {
        mvc.perform(get("/api/admin/session").session(signedIn())).andExpect(
            status().isForbidden()
        );
    }

    @Test
    void adminAllowedAndRoleDowngradeAppliesImmediately() throws Exception {
        account.setRole("ADMIN");
        users.save(account);
        var session = signedIn();
        mvc.perform(get("/api/admin/session").session(session)).andExpect(status().isOk());
        account.setRole("USER");
        users.save(account);
        mvc.perform(get("/api/admin/session").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesSessionAndOldCookieCannotAccessProfile() throws Exception {
        var session = signedIn();
        String id = session.getId();
        var t = token(session);
        mvc.perform(post("/api/auth/logout").session(session).header(t.header(), t.value()))
            .andExpect(status().isOk())
            .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/account/profile").cookie(new Cookie("JSESSIONID", id))).andExpect(
            status().isUnauthorized()
        );
    }

    @Test
    void logoutOnlyInvalidatesCurrentSession() throws Exception {
        var first = signedIn();
        var second = signedIn();
        var t = token(first);
        mvc.perform(
            post("/api/auth/logout").session(first).header(t.header(), t.value())
        ).andExpect(status().isOk());
        mvc.perform(get("/api/account/profile").session(second)).andExpect(status().isOk());
    }

    @Test
    void loginAndLogoutRequireCsrf() throws Exception {
        mvc.perform(
            post("/api/auth/login")
                .param("email", "login@example.com")
                .param("password", "SafePassword123")
        ).andExpect(status().isForbidden());
        var session = signedIn();
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/account/profile").session(session)).andExpect(status().isOk());
    }

    @Test
    void oldCsrfTokenIsRotatedOnLogin() throws Exception {
        var t = token(null);
        login(t, "login@example.com", "SafePassword123").andExpect(status().isOk());
        mvc.perform(
            post("/api/auth/logout").session(t.session()).header(t.header(), t.value())
        ).andExpect(status().isForbidden());
    }

    @Test
    void oversizedPasswordAndMissingCredentialsAreGeneric() throws Exception {
        var t = token(null);
        login(t, "login@example.com", "x".repeat(73))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Unable to sign in with those credentials."));
        login(t, "", "").andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedAttemptsAreThrottled() throws Exception {
        var t = token(null);
        for (int i = 0; i < 20; i++) login(t, "", "").andExpect(status().isUnauthorized());
        login(t, "login@example.com", "SafePassword123")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }
}
