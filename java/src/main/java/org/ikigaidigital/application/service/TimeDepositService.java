package org.ikigaidigital.application.service;

import lombok.RequiredArgsConstructor;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Coordinates deposit retrieval and interest updates independently of HTTP and storage technology. */
@RequiredArgsConstructor
public class TimeDepositService {
    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    @Transactional(readOnly = true)
    public List<TimeDepositDetails> getTimeDeposits() {
        return repository.findAll();
    }

    /**
     * Applies and saves one interest update for every deposit.
     * The transaction covers the entire read-calculate-save operation so locks
     * are held until all balances are saved, with rollback on failure.
     */
    @Transactional
    public void updateBalances() {
        // Keep decimal balances for retrieval, converting only at the boundary
        // of the shared calculator, whose Double contract must remain unchanged.
        List<TimeDeposit> deposits = repository.findAllForBalanceUpdate().stream()
            .map(deposit -> new TimeDeposit(deposit.id(), deposit.planType(),
                deposit.balance().doubleValue(), deposit.days()))
            .toList();
        calculator.updateBalance(deposits);
        List<BalanceUpdate> updates = deposits.stream()
            .map(deposit -> new BalanceUpdate(deposit.getId(), BigDecimal.valueOf(deposit.getBalance())))
            .toList();
        repository.saveBalances(updates);
    }
}
