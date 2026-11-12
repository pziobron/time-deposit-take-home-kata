package org.ikigaidigital.application.service;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.model.Withdrawal;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDepositServiceTest {
    private final InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository();
    private final TimeDepositService service = new TimeDepositService(repository, new TimeDepositCalculator());

    @Test
    void retrievesDepositsWithTheirWithdrawalsWithoutChangingStoredValues() {
        Withdrawal withdrawal = new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1));
        TimeDepositDetails deposit = new TimeDepositDetails(
            1, "basic", new BigDecimal("1200.123"), 31, List.of(withdrawal));
        repository.deposits.add(deposit);

        assertThat(service.getTimeDeposits()).containsExactly(deposit);
        assertThat(repository.findAll()).containsExactly(deposit);
    }

    @Test
    void retrievesAnEmptyListWhenNoDepositsExist() {
        assertThat(service.getTimeDeposits()).isEmpty();
    }

    @Test
    void persistsUpdatedBalancesWhilePreservingMetadataAndWithdrawalHistory() {
        Withdrawal withdrawal = new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1));
        repository.deposits.addAll(List.of(
            new TimeDepositDetails(1, "basic", new BigDecimal("1200.123"), 31, List.of(withdrawal)),
            deposit(2, "student", "1200.0", 365),
            deposit(3, "premium", "1200.0", 46),
            deposit(4, "premium", "1200.0", 45),
            deposit(5, "unknown", "1200.0", 46)
        ));

        service.updateBalances();

        assertThat(repository.findAll()).containsExactly(
            new TimeDepositDetails(1, "basic", new BigDecimal("1201.123"), 31, List.of(withdrawal)),
            deposit(2, "student", "1203.0", 365),
            deposit(3, "premium", "1205.0", 46),
            deposit(4, "premium", "1200.0", 45),
            deposit(5, "unknown", "1200.0", 46)
        );
    }

    @Test
    void updatesAnEmptyRepositorySuccessfully() {
        service.updateBalances();

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void successiveUpdatesUseThePreviouslySavedBalances() {
        repository.deposits.add(deposit(1, "student", "1200.0", 31));

        service.updateBalances();
        service.updateBalances();

        assertThat(repository.findAll()).containsExactly(deposit(1, "student", "1206.01", 31));
    }

    private static TimeDepositDetails deposit(int id, String plan, String balance, int days) {
        return new TimeDepositDetails(id, plan, new BigDecimal(balance), days, List.of());
    }

    // Immutable snapshots ensure mutating calculator inputs cannot accidentally
    // persist changes: the service must explicitly save the updated balances.
    private static class InMemoryTimeDepositRepository implements TimeDepositRepository {
        private final List<TimeDepositDetails> deposits = new ArrayList<>();

        @Override
        public List<TimeDepositDetails> findAll() {
            return List.copyOf(deposits);
        }

        @Override
        public void saveBalances(List<BalanceUpdate> updates) {
            for (BalanceUpdate updated : updates) {
                for (int i = 0; i < deposits.size(); i++) {
                    TimeDepositDetails existing = deposits.get(i);
                    if (existing.id() == updated.id()) {
                        deposits.set(i, new TimeDepositDetails(existing.id(), existing.planType(),
                            updated.balance(), existing.days(), existing.withdrawals()));
                        break;
                    }
                }
            }
        }
    }
}
