package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lk.truthlens.model.Submission;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import lk.truthlens.verification.VerificationOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalHistoryIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    SubmissionRepository records;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper json;

    @Autowired
    VerificationOrchestrator workflow;

    UserAccount alice, bob;
    static String hash;
    static AtomicInteger ips = new AtomicInteger(1);

    record Token(MockHttpSession session, String value, String header) {}

    @BeforeEach
    void setup() {
        records.deleteAll();
        users.deleteAll();
        if (hash == null) hash = encoder.encode("SafePassword123");
        alice = users.save(new UserAccount("Alice", "alice@example.com", hash));
        bob = users.save(new UserAccount("Bob", "bob@example.com", hash));
    }

    @AfterEach
    void cleanup() {
        records.deleteAll();
    }

    Token token(MockHttpSession session) throws Exception {
        var req = get("/api/auth/csrf");
        if (session != null) req.session(session);
        var result = mvc.perform(req).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Token(
            (MockHttpSession) result.getRequest().getSession(false),
            body.get("token").asText(),
            body.get("headerName").asText()
        );
    }

    MockHttpSession login(String email) throws Exception {
        var t = token(null);
        mvc.perform(
            post("/api/auth/login")
                .session(t.session())
                .header(t.header(), t.value())
                .param("email", email)
                .param("password", "SafePassword123")
                .with(req -> {
                    req.setRemoteAddr("198.51.100." + ips.getAndIncrement());
                    return req;
                })
        ).andExpect(status().isOk());
        return t.session();
    }

    ResultActions submit(MockHttpSession session, Map<String, Object> body) throws Exception {
        var t = token(session);
        return mvc.perform(
            post("/api/verifications")
                .session(session)
                .header(t.header(), t.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))
        );
    }

    String create(MockHttpSession session, String content) throws Exception {
        String id = json
            .readTree(
                submit(session, Map.of("content", content))
                    .andExpect(status().isAccepted())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
            )
            .get("id")
            .asText();
        workflow.preparePending();
        return id;
    }

    @Test
    void submissionIsOwnedByAuthenticatedUserAndReportIsStored() throws Exception {
        var session = login(alice.getEmail());
        String id = create(session, "An article submitted for verification.");
        var row = records.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(alice.getId(), row.getOwnerId());
        assertEquals(Submission.Status.PROCESSING, row.getStatus());
        assertTrue(row.getReportJson().contains("Not assessed"));
        mvc.perform(get("/api/verifications/" + id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stored").value(true))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.content").value(row.getContent()))
            .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void privateHistoryAndDirectReportsAreIsolatedIncludingForAdmin() throws Exception {
        var a = login(alice.getEmail());
        String id = create(a, "Alice private report.");
        bob.setRole("ADMIN");
        users.save(bob);
        var b = login(bob.getEmail());
        mvc.perform(get("/api/verifications").session(b).param("ownerId", alice.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        var forbidden = mvc
            .perform(get("/api/verifications/" + id).session(b))
            .andExpect(status().isNotFound())
            .andReturn();
        var missing = mvc
            .perform(get("/api/verifications/" + UUID.randomUUID()).session(b))
            .andExpect(status().isNotFound())
            .andReturn();
        assertEquals(
            missing.getResponse().getContentAsString(),
            forbidden.getResponse().getContentAsString()
        );
    }

    @Test
    void ownerAndStatusCannotBeInjectedIntoSubmission() throws Exception {
        var a = login(alice.getEmail());
        submit(a, Map.of("content", "Private article.", "ownerId", bob.getId())).andExpect(
            status().isBadRequest()
        );
        submit(a, Map.of("content", "Private article.", "status", "COMPLETED")).andExpect(
            status().isBadRequest()
        );
        assertEquals(0, records.count());
    }

    @Test
    void historySurvivesNewLoginAndContainsTitlesDatesAndStatus() throws Exception {
        create(login(alice.getEmail()), "My saved article about transport.");
        var session = login(alice.getEmail());
        mvc.perform(get("/api/verifications").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].title").value("My saved article about transport."))
            .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$.items[0].reportAvailable").value(false));
    }

    @Test
    void combinedDateStatusAndSearchFiltersRemainOwnerScoped() throws Exception {
        var a = login(alice.getEmail());
        String first = create(a, "Transport announcement");
        String second = create(a, "Health announcement");
        create(login(bob.getEmail()), "Transport private to Bob");
        var row = records.findById(UUID.fromString(first)).orElseThrow();
        row.setCreatedAt(Instant.parse("2026-09-12T12:00:00Z"));
        row.setStatus(Submission.Status.COMPLETED);
        records.saveAndFlush(row);
        row = records.findById(UUID.fromString(second)).orElseThrow();
        row.setCreatedAt(Instant.parse("2026-09-13T12:00:00Z"));
        row.setStatus(Submission.Status.FAILED);
        records.saveAndFlush(row);
        mvc.perform(
            get("/api/verifications")
                .session(a)
                .param("from", "2026-09-12")
                .param("to", "2026-09-12")
                .param("status", "COMPLETED")
                .param("q", "transport")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(first));
    }

    @Test
    void inclusiveUtcDateFilterHandlesBothDayBoundaries() throws Exception {
        var a = login(alice.getEmail());
        String id = create(a, "Midnight article");
        var row = records.findById(UUID.fromString(id)).orElseThrow();
        row.setCreatedAt(Instant.parse("2026-09-12T23:59:59.999999Z"));
        records.saveAndFlush(row);
        mvc.perform(
            get("/api/verifications")
                .session(a)
                .param("to", "2026-09-12")
                .param("from", "2026-09-12")
        ).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/verifications").session(a).param("from", "2026-09-13")).andExpect(
            jsonPath("$.totalElements").value(0)
        );
    }

    @Test
    void paginationIsStableAndDoesNotLeakOtherUsers() throws Exception {
        var a = login(alice.getEmail());
        for (int i = 0; i < 3; i++) create(a, "Article number " + i);
        create(login(bob.getEmail()), "Bob unrelated");
        var first = mvc
            .perform(get("/api/verifications").session(a).param("size", "2"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andReturn();
        var next = mvc
            .perform(get("/api/verifications").session(a).param("size", "2").param("page", "1"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andReturn();
        assertNotEquals(
            json.readTree(first.getResponse().getContentAsString()).at("/items/0/id"),
            json.readTree(next.getResponse().getContentAsString()).at("/items/0/id")
        );
    }

    @Test
    void invalidDatesStatusesPagesAndIdsReturnUnderstandableErrors() throws Exception {
        var a = login(alice.getEmail());
        for (String query : List.of(
            "?from=wrong",
            "?from=2026-10-02&to=2026-10-01",
            "?status=UNKNOWN",
            "?page=-1",
            "?size=51",
            "/invalid-id"
        ))
            mvc.perform(get("/api/verifications" + query).session(a))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void anonymousRequestsAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/api/verifications")).andExpect(status().isUnauthorized());
        var t = token(null);
        mvc.perform(
            post("/api/verifications")
                .session(t.session())
                .header(t.header(), t.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Article\"}")
        ).andExpect(status().isAccepted());
        var a = login(alice.getEmail());
        mvc.perform(
            post("/api/verifications")
                .session(a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Article\"}")
        ).andExpect(status().isForbidden());
    }

    @Test
    void blankAndOversizedUnicodeTextAreValidatedAndValidUnicodeIsPreserved() throws Exception {
        var a = login(alice.getEmail());
        submit(a, Map.of("content", "  ")).andExpect(status().isBadRequest());
        String text = "😀".repeat(20000);
        String id = create(a, text);
        assertEquals(text, records.findById(UUID.fromString(id)).orElseThrow().getContent());
        submit(a, Map.of("content", text + "x")).andExpect(status().isBadRequest());
    }

    @Test
    void deactivationAndLogoutBlockHistoryAndDirectReportAccess() throws Exception {
        var a = login(alice.getEmail());
        String id = create(a, "Private report");
        alice.setActive(false);
        users.save(alice);
        mvc.perform(get("/api/verifications/" + id).session(a)).andExpect(
            status().isUnauthorized()
        );
        assertTrue(a.isInvalid());
        mvc.perform(get("/api/verifications/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void literalSearchWildcardsDoNotExpandMatches() throws Exception {
        var a = login(alice.getEmail());
        create(a, "100% accurate headline");
        create(a, "Different headline");
        mvc.perform(get("/api/verifications").session(a).param("q", "%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
