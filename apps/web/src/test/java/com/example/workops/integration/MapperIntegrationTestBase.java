package com.example.workops.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Testcontainers MySQL上でroot SQL適用済みDBとMyBatis Mapperを検証する統合テスト基盤。 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
abstract class MapperIntegrationTestBase {

  private static final Object DATABASE_INIT_LOCK = new Object();
  private static final List<String> APPLIED_SQL_SCRIPT_NAMES = new ArrayList<>();
  private static boolean databaseInitialized;

  // Testcontainers singleton container pattern keeps this container for the test JVM lifecycle.
  private static final MySQLContainer MYSQL = createMysqlContainer();

  @Autowired private DataSource dataSource;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
    MYSQL.start();
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @BeforeAll
  void applyRootSql() throws SQLException, IOException {
    synchronized (DATABASE_INIT_LOCK) {
      if (databaseInitialized) {
        return;
      }
      List<Path> scripts = sqlScripts();
      try (Connection connection = dataSource.getConnection()) {
        for (Path script : scripts) {
          ScriptUtils.executeSqlScript(
              connection,
              new EncodedResource(new FileSystemResource(script), StandardCharsets.UTF_8));
          APPLIED_SQL_SCRIPT_NAMES.add(script.getFileName().toString());
        }
      }
      databaseInitialized = true;
    }
  }

  protected Long countRows(String tableName) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
  }

  protected List<String> appliedSqlScriptNames() {
    return List.copyOf(APPLIED_SQL_SCRIPT_NAMES);
  }

  private List<Path> sqlScripts() throws IOException {
    Path dbRoot = Path.of("..", "..", "db").toAbsolutePath().normalize();
    List<Path> scripts = new ArrayList<>();
    addSqlFiles(scripts, dbRoot.resolve("migration"));
    addSqlFiles(scripts, dbRoot.resolve("seed").resolve("common"));
    addSqlFiles(scripts, dbRoot.resolve("seed").resolve("local"));
    scripts.sort(Comparator.comparing((path) -> path.getFileName().toString()));
    return scripts;
  }

  private void addSqlFiles(List<Path> scripts, Path directory) throws IOException {
    try (var paths = Files.list(directory)) {
      scripts.addAll(
          paths.filter((path) -> path.getFileName().toString().endsWith(".sql")).toList());
    }
  }

  @SuppressWarnings("resource")
  private static MySQLContainer createMysqlContainer() {
    configureDockerClientStrategy();
    return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
        .withDatabaseName("workops")
        .withUsername("workops")
        .withPassword("workops")
        .withCommand(
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_0900_ai_ci",
            "--default-time-zone=+09:00");
  }

  private static void configureDockerClientStrategy() {
    // Windows Docker Desktop exposes Docker through a named pipe, so tests avoid PATH-based
    // docker-machine probing.
    if (isWindows()
        && System.getenv("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY") == null
        && System.getProperty("docker.client.strategy") == null) {
      System.setProperty(
          "docker.client.strategy",
          "org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy");
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
  }
}
