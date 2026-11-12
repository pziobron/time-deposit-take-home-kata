package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDepositApplication;
import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.InterestCalculationInput;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.model.Withdrawal;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.application.service.TimeDepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = TimeDepositApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimeDepositPersistenceIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TimeDepositRepository repository;
    @Autowired
    private TimeDepositService service;
    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void clearDatabase() {
        jdbc.execute("TRUNCATE withdrawals, \"timeDeposits\" RESTART IDENTITY");
    }

    @Test
    void retrievesEachDepositOnceWithDecimalBalancesAndAllItsWithdrawals() {
        insertDeposit(1, "basic", "1200.123", 31);
        insertDeposit(2, "premium", "2000.00", 46);
        jdbc.update("""
            INSERT INTO withdrawals (id, "timeDepositId", amount, date)
            VALUES (7, 1, 50.25, DATE '2026-11-01'), (8, 1, 10.125, DATE '2026-11-02')
            """);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(
            new TimeDepositDetails(1, "basic", new BigDecimal("1200.123"), 31, List.of(
                new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1)),
                new Withdrawal(8, 1, new BigDecimal("10.125"), LocalDate.of(2026, 11, 2)))),
            new TimeDepositDetails(2, "premium", new BigDecimal("2000.00"), 46, List.of()));
    }

    @Test
    void readsOnlyCalculationInputsForBalanceUpdates() {
        insertDeposit(1, "basic", "1200.123", 31);

        assertThat(repository.findAllForBalanceUpdate()).containsExactly(
            new InterestCalculationInput(1, "basic", new BigDecimal("1200.123"), 31));
    }

    @Test
    void savesOnlyTheSpecifiedBalancesWithoutChangingOtherData() {
        insertDeposit(1, "basic", "1200.123", 31);
        insertDeposit(2, "student", "2400.00", 365);
        jdbc.update("INSERT INTO withdrawals (id, \"timeDepositId\", amount, date) VALUES (7, 1, 50.25, DATE '2026-11-01')");
        List<TimeDepositDetails> before = repository.findAll();

        repository.saveBalances(List.of(new BalanceUpdate(1, new BigDecimal("1201.123"))));

        TimeDepositDetails first = before.get(0);
        assertThat(repository.findAll()).containsExactly(
            new TimeDepositDetails(first.id(), first.planType(), new BigDecimal("1201.123"),
                first.days(), first.withdrawals()), before.get(1));
    }

    @Test
    void emptyDatabaseSupportsRetrievalAndUpdatesThroughHttp() {
        assertThat(http.getForEntity("/time-deposits", String.class).getBody()).isEqualTo("[]");
        var response = http.postForEntity("/time-deposits/update-balances", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void httpUpdatesArePersistedAndReturnedWithUnchangedWithdrawalHistory() {
        insertDeposit(1, "basic", "1200.123", 31);
        insertDeposit(2, "student", "1200.0", 365);
        insertDeposit(3, "premium", "1200.0", 46);
        insertDeposit(4, "premium", "1200.0", 45);
        insertDeposit(5, "unknown", "1200.0", 46);
        jdbc.update("INSERT INTO withdrawals (id, \"timeDepositId\", amount, date) VALUES (7, 1, 50.25, DATE '2026-11-01')");

        var update = http.postForEntity("/time-deposits/update-balances", null, String.class);
        var response = http.getForEntity("/time-deposits", TimeDepositDetails[].class);

        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(TimeDepositDetails::balance)
            .containsExactly(new BigDecimal("1201.123"), new BigDecimal("1203.0"),
                new BigDecimal("1205.0"), new BigDecimal("1200.0"), new BigDecimal("1200.0"));
        assertThat(response.getBody()[0].withdrawals()).containsExactly(
            new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1)));
        assertThat(response.getBody()).extracting(TimeDepositDetails::days).containsExactly(31, 365, 46, 45, 46);
    }

    @Test
    void failedBalanceSaveRollsBackTheEntireUpdate() {
        jdbc.update("""
            INSERT INTO "timeDeposits" (id, "planType", balance, days)
            SELECT id, 'basic', 1200.0, 31 FROM generate_series(1, 101) AS id
            """);
        List<TimeDepositDetails> before = repository.findAll();
        // Fail after 100 writes: transactions around individual repository calls
        // would leave earlier deposits updated and fail this assertion.
        jdbc.execute("ALTER TABLE \"timeDeposits\" ADD CONSTRAINT reject_second_update CHECK (id <> 101 OR balance <= 1200)");
        try {
            assertThatThrownBy(service::updateBalances).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(repository.findAll()).containsExactlyElementsOf(before);
        } finally {
            jdbc.execute("ALTER TABLE \"timeDeposits\" DROP CONSTRAINT reject_second_update");
        }
    }

    @Test
    void updateWaitsForLockedRowsAndUsesTheNewlyCommittedBalance() throws Exception {
        insertDeposit(1, "student", "1200.0", 31);
        var executor = Executors.newSingleThreadExecutor();
        try (var connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.executeQuery("SELECT id FROM \"timeDeposits\" WHERE id = 1 FOR UPDATE").close();
                var update = executor.submit(service::updateBalances);

                // Wait for an actual database lock wait, rather than relying on
                // a sleep or thread scheduling to manufacture concurrent reads.
                await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                      AND query LIKE '%ORDER BY id FOR UPDATE%'
                    """, Integer.class) > 0);

                statement.executeUpdate("UPDATE \"timeDeposits\" SET balance = 1203.0 WHERE id = 1");
                connection.commit();
                update.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(repository.findAll()).containsExactly(
            new TimeDepositDetails(1, "student", new BigDecimal("1206.01"), 31, List.of()));
    }

    @Test
    void repeatedRequestsApplyInterestToThePreviouslySavedBalance() {
        insertDeposit(1, "student", "1200.0", 31);

        service.updateBalances();
        service.updateBalances();

        assertThat(repository.findAll()).containsExactly(
            new TimeDepositDetails(1, "student", new BigDecimal("1206.01"), 31, List.of()));
    }

    private void insertDeposit(int id, String plan, String balance, int days) {
        jdbc.update("INSERT INTO \"timeDeposits\" (id, \"planType\", balance, days) VALUES (?, ?, ?, ?)",
            id, plan, new BigDecimal(balance), days);
    }
}
