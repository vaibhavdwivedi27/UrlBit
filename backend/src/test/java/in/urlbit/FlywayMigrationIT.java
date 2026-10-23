package in.urlbit;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired private DataSource dataSource;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsRunSuccessfully() {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load();
        var info = flyway.info();
        assertThat(info.applied()).isNotEmpty();
        assertThat(info.pending()).isEmpty();
    }

    @Test
    void usersTableExists() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables"
                                + " WHERE table_name = 'users'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shortUrlsTableExists() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables"
                                + " WHERE table_name = 'short_urls'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void guestQuotasTableExists() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables"
                                + " WHERE table_name = 'guest_quotas'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void refreshTokensTableExists() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables"
                                + " WHERE table_name = 'refresh_tokens'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void urlClickDailyTableExists() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables"
                                + " WHERE table_name = 'url_click_daily'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shortUrlsCodeHasUniqueConstraint() {
        var count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.table_constraints"
                                + " WHERE table_name = 'short_urls'"
                                + " AND constraint_type = 'UNIQUE'"
                                + " AND constraint_name = 'uq_short_urls_code'",
                        Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
