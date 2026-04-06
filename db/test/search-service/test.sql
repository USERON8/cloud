USE search_db;

DELETE FROM search_hot_keyword_total;

-- index optimization
ALTER TABLE search_hot_keyword_total
    ADD INDEX idx_search_hot_keyword_total_score (total_score);

INSERT INTO search_hot_keyword_total (keyword, total_score)
VALUES ('cloud phone', 160),
       ('cloud phone 15', 150),
       ('cloud phone 15 pro', 138),
       ('foldable phone', 124),
       ('cloud fold', 119),
       ('smart watch', 104),
       ('wireless earbuds', 97),
       ('coffee machine', 88),
       ('capsule coffee maker', 80),
       ('electronics', 76);
