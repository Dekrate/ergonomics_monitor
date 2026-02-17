package pl.dekrate.ergonomicsmonitor.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for Flyway migration files validation.
 * Tests migration SQL syntax and structure without requiring Docker or Spring context.
 *
 * @author dekrate
 * @version 1.0
 */
class FlywayMigrationValidationTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";

    @Test
    void shouldHaveValidInitialMigrationFile() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V1__initial_schema.sql");

        // When & Then
        assertThat(migrationFile).exists();

        String content = Files.readString(migrationFile);
        assertThat(content).isNotBlank();

        // Validate essential SQL elements
        assertThat(content).contains("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        assertThat(content).contains("CREATE TABLE IF NOT EXISTS activity_events");
        assertThat(content).contains("CREATE TABLE IF NOT EXISTS break_recommendations");
        assertThat(content).contains("CREATE TABLE IF NOT EXISTS dashboard_metrics");

        // Validate indexes
        assertThat(content).contains("CREATE INDEX");
        assertThat(content).contains("idx_activity_events_user_id");
        assertThat(content).contains("idx_activity_events_timestamp");

        // Validate constraints
        assertThat(content).contains("PRIMARY KEY");
        assertThat(content).contains("UNIQUE(user_id, metric_date)");

        // Validate PostgreSQL specific features
        assertThat(content).contains("JSONB");
        assertThat(content).contains("UUID");
        assertThat(content).contains("TIMESTAMP WITH TIME ZONE");
    }

    @Test
    void shouldHaveProperMigrationFileNaming() {
        // Given & When
        Path migrationDir = Paths.get(MIGRATION_PATH);

        // Then
        assertThat(migrationDir).exists();
        assertThat(migrationDir).isDirectory();

        // V1__initial_schema.sql should follow Flyway naming convention
        Path v1Migration = migrationDir.resolve("V1__initial_schema.sql");
        assertThat(v1Migration)
                .exists()
                .hasFileName("V1__initial_schema.sql");
    }

    @Test
    void shouldHaveValidSqlSyntaxStructure() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V1__initial_schema.sql");
        String content = Files.readString(migrationFile);

        // When & Then - validate basic SQL structure
        assertThat(content)
                .contains("CREATE TABLE")
                .contains("PRIMARY KEY")
                .contains("NOT NULL")
                .doesNotContain("SYNTAX ERROR")
                .doesNotContain("TODO")
                .doesNotContain("FIXME");

        // Validate proper semicolon usage
        long createTableCount = content.lines()
                .filter(line -> line.trim().startsWith("CREATE TABLE"))
                .count();

        long semicolonCount = content.lines()
                .filter(line -> line.trim().endsWith(";"))
                .count();

        // Should have proper SQL statement termination
        assertThat(semicolonCount).isGreaterThan(0);
    }

    @Test
    void shouldHaveRequiredTablesForApplication() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V1__initial_schema.sql");
        String content = Files.readString(migrationFile);

        // When & Then - validate all required tables exist
        String[] requiredTables = {
                "activity_events",
                "break_recommendations",
                "dashboard_metrics"
        };

        for (String table : requiredTables) {
            assertThat(content)
                    .contains("CREATE TABLE IF NOT EXISTS " + table)
                    .as("Migration should create table: " + table);
        }

        // Validate essential columns for each table
        assertThat(content)
                .contains("user_id UUID NOT NULL")
                .contains("timestamp TIMESTAMP WITH TIME ZONE")
                .contains("metadata JSONB")
                .contains("intensity DOUBLE PRECISION");
    }
}
