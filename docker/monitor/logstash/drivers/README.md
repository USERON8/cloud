Place the MySQL JDBC driver in this directory before starting Logstash.

Expected file name:
- `mysql-connector-j-9.3.0.jar`

If you use a different version, update `jdbc_driver_library` in:
- `docker/monitor/logstash/pipeline/logstash.conf`
