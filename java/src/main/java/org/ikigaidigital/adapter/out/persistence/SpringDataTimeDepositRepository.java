package org.ikigaidigital.adapter.out.persistence;

import lombok.RequiredArgsConstructor;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.InterestCalculationInput;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.model.Withdrawal;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Implements the application port through Spring Data JDBC repositories. */
@Repository
@RequiredArgsConstructor
public class SpringDataTimeDepositRepository implements TimeDepositRepository {
    private final TimeDepositJdbcRepository deposits;
    private final WithdrawalJdbcRepository withdrawals;

    @Override
    public List<TimeDepositDetails> findAll() {
        // Two bulk reads avoid issuing a withdrawal query for every deposit.
        var storedDeposits = deposits.findAllByOrderByIdAsc();
        Map<Integer, List<Withdrawal>> history = withdrawals.findAllByOrderByIdAsc().stream()
            .map(withdrawal -> new Withdrawal(withdrawal.id(), withdrawal.timeDepositId(),
                withdrawal.amount(), withdrawal.date()))
            .collect(Collectors.groupingBy(Withdrawal::timeDepositId));
        return storedDeposits.stream()
            .map(deposit -> new TimeDepositDetails(deposit.id(), deposit.planType(), deposit.balance(),
                deposit.days(), history.getOrDefault(deposit.id(), List.of())))
            .toList();
    }

    @Override
    public List<InterestCalculationInput> findAllForBalanceUpdate() {
        return deposits.findAllForBalanceUpdate().stream()
            .map(deposit -> new InterestCalculationInput(deposit.id(), deposit.planType(),
                deposit.balance(), deposit.days()))
            .toList();
    }

    @Override
    @Transactional
    public void saveBalances(List<BalanceUpdate> updates) {
        // Explicit updates leave plan metadata and withdrawal history untouched.
        for (BalanceUpdate update : updates) {
            deposits.updateBalance(update.id(), update.balance());
        }
    }
}
