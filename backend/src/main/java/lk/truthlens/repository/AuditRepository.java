package lk.truthlens.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {

    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(String actor, String action, String target) {
        jdbc.update(
            "INSERT INTO admin_audit(id,actor,action,target,created_at) VALUES(?,?,?,?,?)",
            UUID.randomUUID(),
            actor,
            action,
            target,
            java.sql.Timestamp.from(java.time.Instant.now())
        );
    }

    public List<Map<String, Object>> findPage(int page) {
        return jdbc.queryForList(
            "SELECT actor,action,target,created_at FROM admin_audit ORDER BY created_at DESC LIMIT 30 OFFSET ?",
            (long) page * 30
        );
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit", Long.class);
    }
}
