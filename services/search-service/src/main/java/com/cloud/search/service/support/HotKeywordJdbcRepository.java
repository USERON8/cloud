package com.cloud.search.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HotKeywordJdbcRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO search_hot_keyword_total (keyword, total_score)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE total_score = VALUES(total_score), updated_at = CURRENT_TIMESTAMP
            """;

    private static final String LOAD_SQL = """
            SELECT keyword, total_score
            FROM search_hot_keyword_total
            ORDER BY total_score DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<HotKeywordRecord> loadTop(int limit) {
        int safeLimit = Math.max(1, limit);
        return jdbcTemplate.query(LOAD_SQL, (rs, rowNum) ->
                new HotKeywordRecord(rs.getString("keyword"), rs.getLong("total_score")), safeLimit);
    }

    public void upsertBatch(Map<String, Long> totals) {
        if (totals == null || totals.isEmpty()) {
            return;
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(totals.entrySet());
        jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<String, Long> entry = entries.get(i);
                ps.setString(1, entry.getKey());
                ps.setLong(2, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });
    }

    public record HotKeywordRecord(String keyword, long totalScore) {
    }
}
