package org.ikigaidigital.support;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.InterestCalculationInput;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.port.TimeDepositRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only repository for application and HTTP tests, without database locking
 * or transaction behavior. Immutable snapshots require explicit balance saves.
 */
public class InMemoryTimeDepositRepository implements TimeDepositRepository {
    private final List<TimeDepositDetails> deposits = new ArrayList<>();

    public void add(TimeDepositDetails... deposits) {
        this.deposits.addAll(List.of(deposits));
    }

    public void clear() {
        deposits.clear();
    }

    @Override
    public List<TimeDepositDetails> findAll() {
        return List.copyOf(deposits);
    }

    @Override
    public List<InterestCalculationInput> findAllForBalanceUpdate() {
        return deposits.stream()
            .map(deposit -> new InterestCalculationInput(deposit.id(), deposit.planType(),
                deposit.balance(), deposit.days()))
            .toList();
    }

    @Override
    public void saveBalances(List<BalanceUpdate> updates) {
        for (BalanceUpdate update : updates) {
            deposits.replaceAll(existing -> existing.id() == update.id()
                ? new TimeDepositDetails(existing.id(), existing.planType(), update.balance(),
                    existing.days(), existing.withdrawals())
                : existing);
        }
    }
}
