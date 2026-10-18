package com.nailic.sproochencoach.migration;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class V11RetryParentMigrationTest {
    @Test
    void existingAttemptsRemainUnlinkedAndRetriesCanReferenceThem() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:v11_retry;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE exercise_attempts (id BIGINT PRIMARY KEY, status VARCHAR(32))");
            statement.execute("INSERT INTO exercise_attempts VALUES (1, 'EVALUATED')");
            try (var migration = getClass().getResourceAsStream(
                    "/db/migration/V11__add_exercise_attempt_retry_parent.sql")) {
                assertThat(migration).isNotNull();
                statement.execute(new String(migration.readAllBytes(), StandardCharsets.UTF_8));
            }
            statement.execute("INSERT INTO exercise_attempts VALUES (2, 'GENERATED', 1)");
            try (var rows = statement.executeQuery("SELECT * FROM exercise_attempts ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("status")).isEqualTo("EVALUATED");
                assertThat(rows.getObject("retry_of_attempt_id")).isNull();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("status")).isEqualTo("GENERATED");
                assertThat(rows.getLong("retry_of_attempt_id")).isEqualTo(1);
            }
        }
    }
}
