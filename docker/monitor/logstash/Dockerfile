FROM logstash:9.1.2

USER root

RUN set -eux; \
    mkdir -p /usr/share/logstash/drivers; \
    curl -fsSL \
      https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.3.0/mysql-connector-j-9.3.0.jar \
      -o /usr/share/logstash/drivers/mysql-connector-j-9.3.0.jar; \
    chown -R logstash:root /usr/share/logstash/drivers

USER logstash
