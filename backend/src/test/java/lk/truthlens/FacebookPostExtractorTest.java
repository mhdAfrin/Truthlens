package lk.truthlens;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lk.truthlens.exception.ArticleImportException;
import lk.truthlens.integration.article.ArticleExtractor;
import lk.truthlens.integration.article.ArticleTransport;
import org.junit.jupiter.api.*;

class FacebookPostExtractorTest {

    static final String TEXT =
        "Public post text in Tamil தமிழ் and English. This is a synthetic extraction fixture, not a completed factual assessment. Attached media is outside the extracted text.";

    String fragment(String id) throws Exception {
        return new ObjectMapper().writeValueAsString(
            Map.of(
                "data",
                Map.of(
                    "node_v2",
                    Map.of(
                        "__typename",
                        "Story",
                        "post_id",
                        id,
                        "permalink_url",
                        "https://www.facebook.com/page/posts/" + id,
                        "comet_sections",
                        Map.of(
                            "content",
                            Map.of("story", Map.of("post_id", id, "message", Map.of("text", TEXT)))
                        )
                    )
                )
            )
        );
    }

    ArticleExtractor.Text extract(String html, String url) {
        return new ArticleExtractor().extract(
            new ArticleTransport.Page(
                200,
                "",
                "text/html; charset=UTF-8",
                "",
                html.getBytes(StandardCharsets.UTF_8)
            ),
            url
        );
    }

    String html(String id) throws Exception {
        return (
            "<title>Public Facebook post</title><script type='application/json' data-sjs>" +
            fragment(id) +
            "</script>"
        );
    }

    @Test
    void exactPublicPostCaptionIsReadFromServerDeliveredJson() throws Exception {
        assertEquals(
            TEXT,
            extract(
                html("123"),
                "https://www.facebook.com/story.php?story_fbid=123&id=456"
            ).content()
        );
    }

    @Test
    void relatedPostIdsAreNeverSubstituted() throws Exception {
        assertThrows(ArticleImportException.class, () ->
            extract(html("999"), "https://www.facebook.com/story.php?story_fbid=123")
        );
    }

    @Test
    void ambiguousSharePagesFailInsteadOfChoosingAnotherPost() throws Exception {
        assertThrows(ArticleImportException.class, () ->
            extract(html("123") + html("999"), "https://www.facebook.com/share/p/token/")
        );
    }

    @Test
    void metadataPreviewAloneIsNotAnArticle() {
        assertThrows(ArticleImportException.class, () ->
            extract(
                "<meta property='og:description' content='" + TEXT + "'>",
                "https://www.facebook.com/share/p/token/"
            )
        );
    }

    @Test
    void loginGateAndExecutableScriptsAreNotImported() throws Exception {
        assertThrows(ArticleImportException.class, () ->
            extract(
                html("123") + "<form action='/login'><input name='pass'></form>",
                "https://www.facebook.com/story.php?story_fbid=123"
            )
        );
        assertThrows(ArticleImportException.class, () ->
            extract(
                "<script>" + fragment("123") + "</script>",
                "https://www.facebook.com/story.php?story_fbid=123"
            )
        );
    }

    @Test
    void postRouteMustMatchAndProfileFeedIsNotAnArticle() throws Exception {
        assertEquals(
            TEXT,
            extract(html("123"), "https://www.facebook.com/page/posts/123").content()
        );
        assertThrows(ArticleImportException.class, () ->
            extract(html("123"), "https://www.facebook.com/page/posts/999")
        );
        assertThrows(ArticleImportException.class, () ->
            extract(html("123"), "https://www.facebook.com/page/")
        );
    }
}
