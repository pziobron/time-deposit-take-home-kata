package org.ikigaidigital.application.service;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.TimeDepositRepository;

import java.math.BigDecimal;
import java.util.List;

/** Coordinates deposit retrieval and interest updates independently of HTTP and storage technology. */
public class TimeDepositService {
    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    public List<TimeDepositDetails> getTimeDeposits() {
        return repository.findAll();
    }

    /**
     * Applies and saves one interest update for every deposit.
     * The infrastructure integration must wrap the entire read-calculate-save
     * operation in a transaction, not just the save.
     */
    public void updateBalances() {
        // Keep decimal balances for retrieval, converting only at the boundary
        // of the shared calculator, whose Double contract must remain unchanged.
        List<TimeDeposit> deposits = repository.findAll().stream()
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
