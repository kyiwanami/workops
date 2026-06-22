# The migration image owns Flyway CLI execution independently from the Spring Boot web app.
FROM flyway/flyway:12.9.0-alpine

ARG MYSQL_CONNECTOR_J_VERSION=9.7.0

ADD --chmod=0644 https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${MYSQL_CONNECTOR_J_VERSION}/mysql-connector-j-${MYSQL_CONNECTOR_J_VERSION}.jar /flyway/drivers/mysql-connector-j.jar

COPY db/migration/ /flyway/sql/migration/
COPY db/seed/common/ /flyway/sql/seed/common/
COPY db/seed/local/ /flyway/sql/seed/local/
COPY db/seed/aws-dev/ /flyway/sql/seed/aws-dev/
COPY infra/docker/migration/entrypoint.sh /flyway/workops-entrypoint.sh

RUN chmod 0555 /flyway/workops-entrypoint.sh

ENTRYPOINT ["/flyway/workops-entrypoint.sh"]
