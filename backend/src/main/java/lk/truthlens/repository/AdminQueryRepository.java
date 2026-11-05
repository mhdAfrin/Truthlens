package lk.truthlens.repository;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminQueryRepository {

    private final JdbcTemplate jdbc;
    private final DataSource source;

    public AdminQueryRepository(JdbcTemplate jdbc, DataSource source) {
        this.jdbc = jdbc;
        this.source = source;
    }

    public long activeUsers() {
        return count("SELECT COUNT(*) FROM user_accounts WHERE active=TRUE");
    }

    public long processingSubmissions() {
        return count("SELECT COUNT(*) FROM verification_records WHERE status='PROCESSING'");
    }

    public long awaitingSubmissions() {
        return count(
            "SELECT COUNT(*) FROM verification_records WHERE status='AWAITING_VERIFICATION'"
        );
    }

    public long publishedNews() {
        return count("SELECT COUNT(*) FROM news_entries WHERE visible=TRUE");
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    public Map<String, Object> system(int textLimit, long users, long submissions)
        throws Exception {
        try (var connection = source.getConnection()) {
            var result = new LinkedHashMap<String, Object>();
            result.put("database", connection.getMetaData().getDatabaseProductName());
            result.put("databaseStatus", connection.isValid(2) ? "Connected" : "Unavailable");
            result.put(
                "migrations",
                jdbc.queryForList(
                    "SELECT \"version\",\"description\",\"success\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\""
                )
            );
            result.put("textLimit", textLimit);
            result.put("sessions", "Server-side · 30-minute idle timeout");
            result.put("verificationEngine", "Evidence engine not connected");
            result.put("passwordStorage", "BCrypt hashes; never exposed");
            result.put(
                "tables",
                List.of(
                    Map.of("name", "user_accounts", "rows", users),
                    Map.of("name", "verification_records", "rows", submissions),
                    Map.of(
                        "name",
                        "news_entries",
                        "rows",
                        count("SELECT COUNT(*) FROM news_entries")
                    ),
                    Map.of("name", "admin_audit", "rows", count("SELECT COUNT(*) FROM admin_audit"))
                )
            );
            return result;
        }
    }
}
