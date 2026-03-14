USE search_db;

DELETE FROM search_hot_keyword_total;

INSERT INTO search_hot_keyword_total (keyword, total_score)
VALUES ('iphone', 120),
       ('xiaomi', 80),
       ('watch', 60);
