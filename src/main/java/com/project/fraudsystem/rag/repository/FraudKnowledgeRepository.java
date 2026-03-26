package com.project.fraudsystem.rag.repository;

import com.project.fraudsystem.rag.model.FraudKnowledgeChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FraudKnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;

    public FraudKnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FraudKnowledgeChunk> findAll()
    {
        String sql = "SELECT id, title, content, category, risk_level FROM fraud_knowledge";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new FraudKnowledgeChunk(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("category"),
                        rs.getString("risk_level")
                )
        );
    }


    public List<FraudKnowledgeChunk> searchByKeyword(String keyword) {
        String sql = """
                SELECT id, title, content, category, risk_level
                FROM fraud_knowledge
                WHERE LOWER(title) LIKE LOWER(?)
                   OR LOWER(content) LIKE LOWER(?)
                   OR LOWER(category) LIKE LOWER(?)
                """;

        String searchPattern = "%" + keyword + "%";

        return jdbcTemplate.query(
                sql,
                new Object[]{searchPattern, searchPattern, searchPattern},
                (rs, rowNum) ->
                        new FraudKnowledgeChunk(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("content"),
                                rs.getString("category"),
                                rs.getString("risk_level")
                        )
        );
    }

    public void updateEmbedding(int id, List<Double> embedding) {

        String sql = "UPDATE fraud_knowledge SET embedding = CAST(? AS vector) WHERE id = ?";

        String vectorString = embedding.toString().replace(" ", "");

        jdbcTemplate.update(sql, vectorString, id);
    }

    public List<FraudKnowledgeChunk> searchByVector(String queryVector) {

        String sql = """
            SELECT id, title, content, category, risk_level
            FROM fraud_knowledge
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT 3
            """;

        return jdbcTemplate.query(
                sql,
                new Object[]{queryVector},
                (rs, rowNum) ->
                        new FraudKnowledgeChunk(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("content"),
                                rs.getString("category"),
                                rs.getString("risk_level")
                        )
        );
    }
}