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
import lk.truthlens.model.Submission;
import lk.truthlens.model.UserAccount;
import lk.truthlens.repository.SubmissionRepository;
import lk.truthlens.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    SubmissionRepository records;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate db;

    @MockitoBean
    ArticleTransport transport;

    @MockitoBean
    ArticleAddressResolver dns;

    UserAccount admin, member;
    static String hash;
    static int counter;

    record Session(MockHttpSession session, String header, String token) {}

    @BeforeEach
    void setup() throws Exception {
        records.deleteAll();
        users.deleteAll();
        db.update("DELETE FROM admin_audit");
        db.update("DELETE FROM news_entries");
        if (hash == null) hash = encoder.encode("TestSecret2026!");
        admin = new UserAccount("Operator", "operator@example.com", hash);
        admin.setRole("ADMIN");
        users.save(admin);
        member = users.save(new UserAccount("Reader", "reader@example.com", hash));
        when(dns.resolve(anyString())).thenReturn(new InetAddress[] {
            InetAddress.getByName("93.184.215.14"),
        });
        when(transport.get(any(), anyLong())).thenReturn(
            new ArticleTransport.Page(
                200,
                "",
                "application/rss+xml",
                "",
                "<rss><channel/></rss>".getBytes()
            )
        );
    }

    @AfterEach
    void cleanup() {
        records.deleteAll();
        db.update("DELETE FROM admin_audit");
        db.update("DELETE FROM news_entries");
    }

    Session token(MockHttpSession session) throws Exception {
        var req = get("/api/auth/csrf");
        if (session != null) req.session(session);
        var r = mvc.perform(req).andReturn();
        var j = json.readTree(r.getResponse().getContentAsString());
        return new Session(
            (MockHttpSession) r.getRequest().getSession(false),
            j.get("headerName").asText(),
            j.get("token").asText()
        );
    }

    Session login(String email) throws Exception {
        var s = token(null);
        mvc.perform(
            post("/api/auth/login")
                .session(s.session())
                .header(s.header(), s.token())
                .param("email", email)
                .param("password", "TestSecret2026!")
                .with(r -> {
                    r.setRemoteAddr("198.19.1." + ++counter);
                    return r;
                })
        ).andExpect(status().isOk());
        return token(s.session());
    }

    @Test
    void everyAdminCollectionRejectsAnonymousAndOrdinaryUsers() throws Exception {
        var s = login(member.getEmail());
        for (String endpoint : List.of(
            "overview",
            "users",
            "submissions",
            "news",
            "audit",
            "system"
        )) {
            mvc.perform(get("/api/admin/" + endpoint)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/admin/" + endpoint).session(s.session())).andExpect(
                status().isForbidden()
            );
        }
    }

    @Test
    void overviewAndSystemReadRealCountsAndMigrations() throws Exception {
        var s = login(admin.getEmail());
        mvc.perform(get("/api/admin/overview").session(s.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.users").value(2));
        mvc.perform(get("/api/admin/system").session(s.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.databaseStatus").value("Connected"))
            .andExpect(jsonPath("$.migrations.length()").value(7));
    }

    @Test
    void userDetailsNeverExposePasswordHashes() throws Exception {
        var s = login(admin.getEmail());
        var result = mvc
            .perform(get("/api/admin/users").session(s.session()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertFalse(result.contains("password"));
        assertFalse(result.contains("$2a$"));
    }

    @Test
    void deactivationRevokesExistingSessionAndIsAudited() throws Exception {
        var reader = login(member.getEmail());
        var a = login(admin.getEmail());
        mvc.perform(
            patch("/api/admin/users/" + member.getId())
                .session(a.session())
                .header(a.header(), a.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")
        ).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(reader.session())).andExpect(
            status().isUnauthorized()
        );
        assertEquals(
            1,
            db.queryForObject(
                "SELECT COUNT(*) FROM admin_audit WHERE action='ACCOUNT_DEACTIVATED'",
                Integer.class
            )
        );
    }

    @Test
    void adminAccountAndCsrfRemainProtected() throws Exception {
        var a = login(admin.getEmail());
        mvc.perform(
            patch("/api/admin/users/" + member.getId())
                .session(a.session())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            patch("/api/admin/users/" + admin.getId())
                .session(a.session())
                .header(a.header(), a.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")
        ).andExpect(status().isBadRequest());
        assertTrue(users.findById(admin.getId()).orElseThrow().isActive());
    }

    @Test
    void privateAdminInspectionIsAuditedButPersonalRoutesRemainOwnerScoped() throws Exception {
        var r = new Submission();
        r.setId(UUID.randomUUID());
        r.setOwnerId(member.getId());
        r.setContent("Private article");
        r.setTitle("Private title");
        r.setCreatedAt(java.time.Instant.now());
        r.setStatus(Submission.Status.PROCESSING);
        r.setReportJson("{}");
        records.save(r);
        var a = login(admin.getEmail());
        mvc.perform(get("/api/verifications/" + r.getId()).session(a.session())).andExpect(
            status().isNotFound()
        );
        mvc.perform(get("/api/admin/submissions/" + r.getId()).session(a.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Private article"));
        assertEquals(
            1,
            db.queryForObject(
                "SELECT COUNT(*) FROM admin_audit WHERE action='PRIVATE_SUBMISSION_VIEWED'",
                Integer.class
            )
        );
    }

    @Test
    void draftsStayPrivateAndPublishedNewsAppearsPublicly() throws Exception {
        var a = login(admin.getEmail());
        var story = new HashMap<String, Object>(
            Map.of(
                "title",
                "A sourced update",
                "summary",
                "Editorial context",
                "publisher",
                "Publisher",
                "sourceUrl",
                "https://news.org/article",
                "publishedAt",
                java.time.Instant.now().minusSeconds(60).toString(),
                "visible",
                false,
                "breaking",
                false
            )
        );
        var response = mvc
            .perform(
                post("/api/admin/news")
                    .session(a.session())
                    .header(a.header(), a.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(story))
            )
            .andExpect(status().isOk())
            .andReturn();
        String id = json.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
        story.put("visible", true);
        mvc.perform(
            put("/api/admin/news/" + id)
                .session(a.session())
                .header(a.header(), a.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(story))
        ).andExpect(status().isOk());
        mvc.perform(get("/api/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].title").value("A sourced update"));
    }

    @Test
    void ordinaryUserCannotPublishAndInvalidSourceIsRejected() throws Exception {
        var reader = login(member.getEmail());
        mvc.perform(
            post("/api/admin/news")
                .session(reader.session())
                .header(reader.header(), reader.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isForbidden());
        var a = login(admin.getEmail());
        mvc.perform(
            post("/api/admin/news")
                .session(a.session())
                .header(a.header(), a.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"x\",\"summary\":\"x\",\"publisher\":\"x\",\"sourceUrl\":\"javascript:alert(1)\",\"publishedAt\":\"2026-01-01T00:00:00Z\"}"
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registrationAcceptsEightAndRejectsSevenCharacters() throws Exception {
        var s = token(null);
        for (String password : List.of("Abcd123", "Abcd1234")) {
            mvc.perform(
                post("/api/auth/register")
                    .session(s.session())
                    .header(s.header(), s.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsBytes(
                            Map.of(
                                "name",
                                "Eight Character",
                                "email",
                                "eight@example.com",
                                "password",
                                password
                            )
                        )
                    )
                    .with(r -> {
                        r.setRemoteAddr("198.19.2." + ++counter);
                        return r;
                    })
            ).andExpect(password.length() == 8 ? status().isCreated() : status().isBadRequest());
        }
        assertTrue(
            encoder.matches(
                "Abcd1234",
                users.findByEmail("eight@example.com").orElseThrow().getPasswordHash()
            )
        );
    }
}
