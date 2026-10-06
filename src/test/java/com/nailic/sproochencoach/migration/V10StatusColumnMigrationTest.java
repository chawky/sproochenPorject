package com.nailic.sproochencoach.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class V10StatusColumnMigrationTest {
    @Test
    void preservesExistingStatusesAndAcceptsEvaluating() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v10_status_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""
        ); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE exercise_attempts (
                        id BIGINT PRIMARY KEY,
                        status ENUM('GENERATED', 'COMPLETED', 'EVALUATED') NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO exercise_attempts (id, status) VALUES
                        (1, 'GENERATED'),
                        (2, 'COMPLETED'),
                        (3, 'EVALUATED')
                    """);

            try (var migration = getClass().getResourceAsStream(
                    "/db/migration/V10__change_exercise_attempt_status_to_varchar.sql"
            )) {
                assertThat(migration).isNotNull();
                statement.execute(new String(migration.readAllBytes(), StandardCharsets.UTF_8));
            }

            try (ResultSet column = connection.getMetaData().getColumns(null, null, "EXERCISE_ATTEMPTS", "STATUS")) {
                assertThat(column.next()).isTrue();
                assertThat(column.getInt("DATA_TYPE")).isEqualTo(Types.VARCHAR);
                assertThat(column.getInt("COLUMN_SIZE")).isEqualTo(32);
                assertThat(column.getInt("NULLABLE")).isEqualTo(ResultSetMetaData.columnNoNulls);
            }

            statement.executeUpdate("INSERT INTO exercise_attempts (id, status) VALUES (4, 'EVALUATING')");

            List<String> statuses = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery("SELECT status FROM exercise_attempts ORDER BY id")) {
                while (rows.next()) {
                    statuses.add(rows.getString("status"));
                }
            }
            assertThat(statuses).containsExactly("GENERATED", "COMPLETED", "EVALUATED", "EVALUATING");
        }
    }
}
