package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.util.*;
import lk.truthlens.integration.article.ArticleAddressResolver;
import lk.truthlens.integration.article.ArticleTransport;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.verification.VerificationOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlSubmissionIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubmissionRepository records;

    @Autowired
    ObjectMapper json;

    @Autowired
    VerificationOrchestrator workflow;

    @MockitoBean
    ArticleTransport transport;

    @MockitoBean
    ArticleAddressResolver dns;

    MockHttpSession session;
    String header, token;

    @BeforeEach
    void setup() throws Exception {
        records.deleteAll();
        when(dns.resolve(anyString())).thenReturn(new InetAddress[] {
            InetAddress.getByName("93.184.215.14"),
        });
        when(transport.get(any(), anyLong())).thenReturn(
            new ArticleTransport.Page(
                200,
                "",
                "text/html",
                "",
                ArticleImportTest.HTML.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            )
        );
        var r = mvc.perform(get("/api/auth/csrf")).andReturn();
        session = (MockHttpSession) r.getRequest().getSession(false);
        var j = json.readTree(r.getResponse().getContentAsString());
        header = j.get("headerName").asText();
        token = j.get("token").asText();
    }

    @AfterEach
    void cleanup() {
        records.deleteAll();
    }

    ResultActions send(Object body) throws Exception {
        return mvc.perform(
            post("/api/verifications/url")
                .session(session)
                .header(header, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body))
        );
    }

    @Test
    void storesSourceContentAndRunsExistingWorkflow() throws Exception {
        var response = send(Map.of("url", "https://news.org/article"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.originalUrl").value("https://news.org/article"))
            .andReturn();
        var id = UUID.fromString(
            json.readTree(response.getResponse().getContentAsString()).get("id").asText()
        );
        var row = records.findById(id).orElseThrow();
        assertEquals("URL", row.getInputType());
        assertTrue(row.getContent().contains("Researchers"));
        assertEquals("Research update", row.getTitle());
        workflow.preparePending();
        mvc.perform(get("/api/verifications/" + id).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalUrl").value("https://news.org/article"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        mvc.perform(get("/api/verifications/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void invalidAndMissingUrlNeverFetched() throws Exception {
        for (Object body : List.of(
            Map.of(),
            Map.of("url", true),
            Map.of("url", "ftp://news.org/a")
        )) {
            send(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fallback").value("TEXT"));
        }
        verifyNoInteractions(transport);
        assertEquals(0, records.count());
    }

    @Test
    void blockedDestinationNeverFetched() throws Exception {
        when(dns.resolve(anyString())).thenReturn(new InetAddress[] {
            InetAddress.getByName("127.0.0.1"),
        });
        send(Map.of("url", "https://news.org/private"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BLOCKED_DESTINATION"));
        verifyNoInteractions(transport);
        assertEquals(0, records.count());
    }

    @Test
    void inaccessiblePageOffersTextAndCreatesNoRecord() throws Exception {
        when(transport.get(any(), anyLong())).thenReturn(
            new ArticleTransport.Page(403, "", "text/html", "", new byte[0])
        );
        send(Map.of("url", "https://news.org/article"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fallback").value("TEXT"));
        assertEquals(0, records.count());
    }

    @Test
    void csrfAndExpiredAccountIntentEnforced() throws Exception {
        mvc.perform(
            post("/api/verifications/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://news.org/a\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            post("/api/verifications/url")
                .session(session)
                .header(header, token)
                .header("X-TruthLens-Account", "required")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://news.org/a\"}")
        ).andExpect(status().isUnauthorized());
        verifyNoInteractions(transport);
    }
}
