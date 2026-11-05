package lk.truthlens.repository;

import java.time.*;
import java.util.*;
import lk.truthlens.dto.NewsInput;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NewsRepository {

    private final JdbcTemplate jdbc;

    public NewsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> entries(boolean publicOnly) {
        return jdbc.query(
            "SELECT * FROM news_entries " +
                (publicOnly ? "WHERE visible=TRUE AND published_at<=CURRENT_TIMESTAMP " : "") +
                "ORDER BY published_at DESC LIMIT 100",
            (rs, news) -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("id", rs.getObject("id"));
                m.put("title", rs.getString("title"));
                m.put("summary", rs.getString("summary"));
                m.put("sourceUrl", rs.getString("source_url"));
                m.put("publisher", rs.getString("publisher"));
                m.put("publishedAt", rs.getTimestamp("published_at").toInstant().toString());
                m.put("breaking", rs.getBoolean("breaking"));
                m.put("visible", rs.getBoolean("visible"));
                m.put("origin", "TruthLens editorial selection");
                return m;
            }
        );
    }

    public int insert(UUID newsId, NewsInput news) {
        return jdbc.update(
            "INSERT INTO news_entries(id,title,summary,source_url,publisher,published_at,visible,breaking) VALUES(?,?,?,?,?,?,?,?)",
            newsId,
            news.title().strip(),
            news.summary().strip(),
            news.sourceUrl().strip(),
            news.publisher().strip(),
            java.sql.Timestamp.from(news.publishedAt()),
            Boolean.TRUE.equals(news.visible()),
            Boolean.TRUE.equals(news.breaking())
        );
    }

    public int update(UUID newsId, NewsInput news) {
        return jdbc.update(
            "UPDATE news_entries SET title=?,summary=?,source_url=?,publisher=?,published_at=?,visible=?,breaking=? WHERE id=?",
            news.title().strip(),
            news.summary().strip(),
            news.sourceUrl().strip(),
            news.publisher().strip(),
            java.sql.Timestamp.from(news.publishedAt()),
            Boolean.TRUE.equals(news.visible()),
            Boolean.TRUE.equals(news.breaking()),
            newsId
        );
    }
}
