package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lk.truthlens.exception.InvalidRequestException;
import lk.truthlens.model.Submission;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.service.SubmissionPolicy;
import lk.truthlens.verification.VerificationOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TextSubmissionIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubmissionRepository records;

    @Autowired
    ObjectMapper json;

    @Autowired
    VerificationOrchestrator workflow;

    @Autowired
    SubmissionPolicy policy;

    static AtomicInteger ips = new AtomicInteger();
    String ip;

    record Guest(MockHttpSession session, String header, String token) {}

    @BeforeEach
    void setup() {
        records.deleteAll();
        ip = "203.0.113." + ips.incrementAndGet();
    }

    @AfterEach
    void cleanup() {
        records.deleteAll();
    }

    Guest guest() throws Exception {
        var r = mvc.perform(get("/api/auth/csrf")).andReturn();
        var j = json.readTree(r.getResponse().getContentAsString());
        return new Guest(
            (MockHttpSession) r.getRequest().getSession(false),
            j.get("headerName").asText(),
            j.get("token").asText()
        );
    }

    ResultActions send(Guest guest, Object body) throws Exception {
        return mvc.perform(
            post("/api/verifications")
                .session(guest.session())
                .header(guest.header(), guest.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                })
        );
    }

    JsonNode accepted(Guest guest, String text) throws Exception {
        return json.readTree(
            send(guest, Map.of("content", text))
                .andExpect(status().isAccepted())
                .andExpect(
                    jsonPath("$.message").value("Submission received. Processing has started.")
                )
                .andReturn()
                .getResponse()
                .getContentAsString()
        );
    }

    @Test
    void configurationPublishesSrsLimitAndCountingUnit() throws Exception {
        mvc.perform(get("/api/config/submission"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maxCodePoints").value(20000))
            .andExpect(jsonPath("$.unit").value("Unicode code points"))
            .andExpect(jsonPath("$.format").value("text-v1"));
    }

    @Test
    void emptyAndUnicodeWhitespaceOnlyAreRejected() throws Exception {
        var g = guest();
        for (String text : List.of("", " ", "\t\n\r", "\u00a0\u2003"))
            send(g, Map.of("content", text))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        assertEquals(0, records.count());
    }

    @Test
    void nullMissingAndWrongTypeAreRejected() throws Exception {
        var g = guest();
        send(g, Collections.singletonMap("content", null)).andExpect(status().isBadRequest());
        send(g, Map.of()).andExpect(status().isBadRequest());
        send(g, Map.of("content", List.of("article"))).andExpect(status().isBadRequest());
        send(g, Map.of("content", 123)).andExpect(status().isBadRequest());
        send(g, Map.of("content", true)).andExpect(status().isBadRequest());
    }

    @Test
    void exactlyLimitUnicodeCodePointsAcceptedAndOneOverRejected() throws Exception {
        var g = guest();
        String text = "😀".repeat(policy.maxCodePoints());
        var reply = accepted(g, text);
        var row = records.findById(UUID.fromString(reply.get("id").asText())).orElseThrow();
        assertEquals(text, row.getContent());
        send(g, Map.of("content", text + "x")).andExpect(status().isBadRequest());
    }

    @Test
    void whitespaceCountsTowardLimitAndOriginalContentIsPreserved() throws Exception {
        var g = guest();
        String text = "  An article.\nSecond paragraph.  ";
        var reply = accepted(g, text);
        assertEquals(
            text,
            records
                .findById(UUID.fromString(reply.get("id").asText()))
                .orElseThrow()
                .getContent()
        );
        send(g, Map.of("content", "a" + " ".repeat(20000))).andExpect(status().isBadRequest());
    }

    @Test
    void validGuestSubmissionHasUniqueIdStoredContentAndProcessingStatus() throws Exception {
        var g = guest();
        var first = accepted(g, "A copied article for factual review.");
        var second = accepted(g, "A copied article for factual review.");
        assertNotEquals(first.get("id"), second.get("id"));
        assertEquals(first.get("id"), first.get("requestId"));
        assertEquals("QUEUED", first.get("status").asText());
        var row = records.findById(UUID.fromString(first.get("id").asText())).orElseThrow();
        assertNull(row.getOwnerId());
        assertNotNull(row.getGuestScope());
        assertEquals(Submission.Status.QUEUED, row.getStatus());
        assertEquals(2, records.count());
        mvc.perform(get("/api/verifications/" + first.get("id").asText()).session(g.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("RECEIVED"))
            .andExpect(jsonPath("$.textMetrics").doesNotExist());
    }

    @Test
    void otherGuestAndAnonymousDirectRequestsCannotOpenGuestReport() throws Exception {
        var a = guest();
        var b = guest();
        var reply = accepted(a, "Private to the submitting browser.");
        String path = "/api/verifications/" + reply.get("id").asText();
        mvc.perform(get(path).session(a.session())).andExpect(status().isOk());
        mvc.perform(get(path).session(b.session())).andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void preparationWorkflowProducesRealMetricsWithoutFabricatingVerdict() throws Exception {
        var g = guest();
        var reply = accepted(g, "One article.\nTwo facts.");
        String path = "/api/verifications/" + reply.get("id").asText();
        workflow.preparePending();
        mvc.perform(get(path).session(g.session()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.stage").value("EXTRACTING_CLAIMS"))
            .andExpect(jsonPath("$.textMetrics.words").value(4))
            .andExpect(jsonPath("$.textMetrics.paragraphs").value(2))
                .andExpect(jsonPath("$.reportAvailable").value(false))
            .andExpect(jsonPath("$.claims[0].finding").value("Not assessed"));
    }

    @Test
    void persistedPendingWorkIsRecoverableAndPreparationIsIdempotent() throws Exception {
        var g = guest();
        var reply = accepted(g, "Durable intake record.");
        var id = UUID.fromString(reply.get("id").asText());

        // A new submission starts in the queue.
        assertEquals(
                Submission.Status.QUEUED,
                records.findById(id).orElseThrow().getStatus()
        );

        // First run moves the submission into processing.
        workflow.preparePending();

        assertEquals(
                Submission.Status.PROCESSING,
                records.findById(id).orElseThrow().getStatus()
        );

        // Second run performs verification.
        workflow.preparePending();

        var completed = records.findById(id).orElseThrow();

        assertEquals(Submission.Status.COMPLETED, completed.getStatus());

        String snapshot = completed.getReportJson();

        // Running again must not change a completed report.
        workflow.preparePending();

        assertEquals(
                snapshot,
                records.findById(id).orElseThrow().getReportJson()
        );
    }

    @Test
    void guestCreationRequiresCsrfAndDoesNotExposeAccountHistory() throws Exception {
        mvc.perform(
            post("/api/verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Article\"}")
        ).andExpect(status().isForbidden());
        var g = guest();
        accepted(g, "Guest article.");
        mvc.perform(get("/api/verifications").session(g.session())).andExpect(
            status().isUnauthorized()
        );
    }

    @Test
    void guestRequestThrottlingAndBodyBoundAreEnforced() throws Exception {
        var g = guest();
        for (int i = 0; i < 20; i++) send(g, Map.of("content", "")).andExpect(
            status().isBadRequest()
        );
        send(g, Map.of("content", "Valid text")).andExpect(status().isTooManyRequests());
        ip = "203.0.114." + ips.incrementAndGet();
        send(g, Map.of("content", "x".repeat(270000))).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void clientCannotInjectOwnerStatusOrVerdicts() throws Exception {
        var g = guest();
        send(g, Map.of("content", "A claim.", "ownerId", UUID.randomUUID())).andExpect(
            status().isBadRequest()
        );
        send(g, Map.of("content", "A claim.", "status", "COMPLETED")).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void configuredLimitMayBeLoweredButCannotExceedSrsMaximum() {
        var lower = new SubmissionPolicy(10);
        lower.validate("😀".repeat(10));
        assertThrows(InvalidRequestException.class, () -> lower.validate("😀".repeat(11)));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionPolicy(20001));
        assertThrows(IllegalArgumentException.class, () -> new SubmissionPolicy(0));
        assertThrows(InvalidRequestException.class, () -> lower.validate("a\u0000b"));
    }

    @Test
    void expiredAccountIntentDoesNotSilentlyCreateGuestRecord() throws Exception {
        var g = guest();
        mvc.perform(
            post("/api/verifications")
                .session(g.session())
                .header(g.header(), g.token())
                .header("X-TruthLens-Account", "required")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Article text\"}")
        ).andExpect(status().isUnauthorized());
        assertEquals(0, records.count());
    }

    @Test
    void endingGuestSessionRevokesAccessWithoutDeletingOtherRecords() throws Exception {
        var g = guest();
        var reply = accepted(g, "Guest report.");
        String path = "/api/verifications/" + reply.get("id").asText();
        mvc.perform(
            post("/api/auth/logout").session(g.session()).header(g.header(), g.token())
        ).andExpect(status().isOk());
        assertTrue(g.session().isInvalid());
        mvc.perform(get(path)).andExpect(status().isNotFound());
        assertEquals(1, records.count());
    }
}
