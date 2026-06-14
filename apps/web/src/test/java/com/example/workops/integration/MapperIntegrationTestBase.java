package com.example.workops.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers MySQL上でFlyway適用済みDBとMyBatis Mapperを検証する統合テスト基盤。
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MapperIntegrationTestBase {

    @SuppressWarnings("resource")
    // Testcontainers singleton container pattern keeps this container for the test JVM lifecycle.
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("workops")
            .withUsername("workops")
            .withPassword("workops")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_0900_ai_ci",
                    "--default-time-zone=+09:00");

    static {
        MYSQL.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    protected Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
