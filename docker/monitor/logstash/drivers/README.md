Version: 1.1.0
The MySQL JDBC driver is now baked into the Logstash image during
`docker compose up --build`.

This directory is kept only as a placeholder. If you change the driver
version, update both:
- `docker/monitor/logstash/Dockerfile`
- `docker/monitor/logstash/pipeline/logstash.conf`
