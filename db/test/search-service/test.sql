USE search_db;

DELETE FROM search_hot_keyword_total;

-- 索引优化
ALTER TABLE search_hot_keyword_total
    ADD INDEX idx_search_hot_keyword_total_score (total_score);

INSERT INTO search_hot_keyword_total (keyword, total_score)
VALUES ('iphone', 120),
       ('xiaomi', 80),
       ('watch', 60);
